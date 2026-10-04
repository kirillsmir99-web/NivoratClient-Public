package net.fabricmc.pack.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class SlotArbiter {
    public enum Resource {
        HOTBAR_SELECT,
        OFFHAND,
        INVENTORY_CLICKS,
        USE_ITEM
    }

    public enum Priority {
        LOW(10),
        UTILITY(40),
        COMBAT_NORMAL(60),
        COMBAT_HIGH(80),
        EMERGENCY(100);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public static final class Lease implements AutoCloseable {
        private final String owner;
        private final Priority priority;
        private final Set<Resource> resources;
        private final int ttlTicks;
        private final int returnSlot;
        private final boolean atomic;
        private final long startTick;
        private volatile boolean active = true;

        private Lease(String owner, Priority priority, Set<Resource> resources, int ttlTicks, int returnSlot, boolean atomic, long startTick) {
            this.owner = owner;
            this.priority = priority;
            this.resources = Collections.unmodifiableSet(EnumSet.copyOf(resources));
            this.ttlTicks = ttlTicks;
            this.returnSlot = returnSlot;
            this.atomic = atomic;
            this.startTick = startTick;
        }

        public String getOwner() {
            return owner;
        }

        public Priority getPriority() {
            return priority;
        }

        public Set<Resource> getResources() {
            return resources;
        }

        public int getTtlTicks() {
            return ttlTicks;
        }

        public int getReturnSlot() {
            return returnSlot;
        }

        public boolean isAtomic() {
            return atomic;
        }

        public long getStartTick() {
            return startTick;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public void close() {
            SlotArbiter.release(this);
        }
    }

    private static final AtomicReference<Lease> ACTIVE_LEASE = new AtomicReference<>(null);
    private static volatile int lastUserSelectedSlot = 0;
    private static volatile int lastAssignedSlot = -1;
    private static volatile boolean managerSwitchActive = false;
    private static volatile long currentTickCounter = 0L;

    private SlotArbiter() {}

    public static void onClientTick(MinecraftClient client) {
        currentTickCounter++;
        if (client == null || client.player == null || client.world == null) {
            forceRelease();
            return;
        }

        ClientPlayerEntity player = client.player;
        if (!player.isAlive()) {
            forceRelease();
            return;
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        if (!managerSwitchActive && currentSlot >= 0 && currentSlot < 9) {
            if (currentSlot != lastAssignedSlot) {
                lastUserSelectedSlot = currentSlot;
                lastAssignedSlot = currentSlot;
            }
        }

        Lease current = ACTIVE_LEASE.get();
        if (current != null && current.active) {
            long age = currentTickCounter - current.startTick;
            if (current.ttlTicks > 0 && age >= current.ttlTicks) {
                release(current, true);
            }
        }
    }

    public static Lease acquire(String owner, Priority priority, Set<Resource> resources, int ttlTicks, boolean atomic) {
        if (owner == null || priority == null || resources == null || resources.isEmpty()) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int snapshotReturnSlot = lastUserSelectedSlot;
        if (client != null && client.player != null) {
            int cur = client.player.getInventory().getSelectedSlot();
            if (!managerSwitchActive && cur >= 0 && cur < 9) {
                snapshotReturnSlot = cur;
                lastUserSelectedSlot = cur;
            }
        }

        long tick = currentTickCounter;
        Lease newLease = new Lease(owner, priority, resources, ttlTicks, snapshotReturnSlot, atomic, tick);

        while (true) {
            Lease current = ACTIVE_LEASE.get();
            if (current == null || !current.active) {
                if (ACTIVE_LEASE.compareAndSet(current, newLease)) {
                    return newLease;
                }
                continue;
            }

            if (current.owner.equals(owner)) {
                if (ACTIVE_LEASE.compareAndSet(current, newLease)) {
                    current.active = false;
                    return newLease;
                }
                continue;
            }

            boolean sharesResource = false;
            for (Resource r : resources) {
                if (current.resources.contains(r)) {
                    sharesResource = true;
                    break;
                }
            }

            if (!sharesResource) {
                return newLease;
            }

            if (priority.getLevel() > current.priority.getLevel()) {
                if (current.atomic && priority != Priority.EMERGENCY) {
                    return null;
                }
                if (ACTIVE_LEASE.compareAndSet(current, newLease)) {
                    current.active = false;
                    return newLease;
                }
                continue;
            }

            return null;
        }
    }

    public static boolean selectSlot(MinecraftClient client, Lease lease, int slot) {
        if (lease == null || !lease.active) {
            return false;
        }
        if (!lease.resources.contains(Resource.HOTBAR_SELECT)) {
            return false;
        }
        if (slot < 0 || slot >= 9) {
            return false;
        }

        managerSwitchActive = true;
        try {
            boolean success = SafeSlotManager.selectSlot(client, slot);
            lastAssignedSlot = slot;
            return success;
        } finally {
            managerSwitchActive = false;
        }
    }

    public static void release(Lease lease) {
        release(lease, true);
    }

    public static void release(Lease lease, boolean restoreSlot) {
        if (lease == null) return;
        lease.active = false;
        if (ACTIVE_LEASE.compareAndSet(lease, null)) {
            if (restoreSlot && lease.resources.contains(Resource.HOTBAR_SELECT)) {
                int ret = lease.returnSlot;
                if (ret >= 0 && ret < 9) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null && client.player.isAlive()) {
                        managerSwitchActive = true;
                        try {
                            SafeSlotManager.restoreSlot(client, ret);
                            lastAssignedSlot = ret;
                        } finally {
                            managerSwitchActive = false;
                        }
                    }
                }
            }
        }
    }

    public static void forceRelease() {
        Lease current = ACTIVE_LEASE.getAndSet(null);
        if (current != null) {
            current.active = false;
        }
    }

    public static boolean isResourceLocked(Resource resource, String inquirer) {
        Lease current = ACTIVE_LEASE.get();
        if (current == null || !current.active) {
            return false;
        }
        if (inquirer != null && current.owner.equals(inquirer)) {
            return false;
        }
        return current.resources.contains(resource);
    }

    public static boolean isLockedByHigherPriority(Resource resource, Priority priority) {
        Lease current = ACTIVE_LEASE.get();
        if (current == null || !current.active) {
            return false;
        }
        if (!current.resources.contains(resource)) {
            return false;
        }
        return current.priority.getLevel() >= priority.getLevel();
    }

    public static Lease getActiveLease() {
        Lease current = ACTIVE_LEASE.get();
        return (current != null && current.active) ? current : null;
    }

    public static int getLastUserSelectedSlot() {
        return lastUserSelectedSlot;
    }

    public static void notifyManagerSlotChange(int slot) {
        lastAssignedSlot = slot;
    }

    public static void notifyUserSlotChange(int slot) {
        if (!managerSwitchActive && slot >= 0 && slot < 9) {
            lastUserSelectedSlot = slot;
            lastAssignedSlot = slot;
        }
    }

    public static void reset() {
        forceRelease();
        lastUserSelectedSlot = 0;
        lastAssignedSlot = -1;
        managerSwitchActive = false;
        currentTickCounter = 0L;
    }
}
