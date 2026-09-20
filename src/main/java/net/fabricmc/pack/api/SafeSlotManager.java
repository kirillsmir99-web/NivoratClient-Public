package net.fabricmc.pack.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.lang.reflect.Field;
import java.util.Locale;

public final class SafeSlotManager {
    private static Field lastSelectedSlotField;
    private static long lastChangeTick = -1L;
    private static int lastSelectedSlot = -1;

    static {
        try {
            for (Field f : ClientPlayerInteractionManager.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if ("lastselectedslot".equals(name) || "field_3721".equals(name) || name.contains("lastselectedslot")) {
                        f.setAccessible(true);
                        lastSelectedSlotField = f;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private SafeSlotManager() {}

    public static boolean setLastSelectedSlot(ClientPlayerInteractionManager manager, int slot) {
        if (manager == null || lastSelectedSlotField == null || slot < 0 || slot >= 9) {
            return false;
        }
        try {
            lastSelectedSlotField.setInt(manager, slot);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getLastSelectedSlot(ClientPlayerInteractionManager manager) {
        if (manager == null || lastSelectedSlotField == null) {
            return -1;
        }
        try {
            return lastSelectedSlotField.getInt(manager);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static boolean selectSlot(MinecraftClient client, int slot, long currentTick) {
        if (client == null || client.player == null || client.interactionManager == null || slot < 0 || slot >= 9) {
            return false;
        }
        if (currentTick < 0) {
            long schedTick = TickBoundScheduler.getTickCount();
            if (schedTick > 0) {
                currentTick = schedTick;
            } else if (client.world != null) {
                currentTick = client.world.getTime();
            }
        }
        int cur = client.player.getInventory().getSelectedSlot();
        if (cur == slot && lastSelectedSlot == slot) {
            return false;
        }
        if (currentTick >= 0 && currentTick == lastChangeTick && cur == slot) {
            return false;
        }
        client.player.getInventory().setSelectedSlot(slot);
        setLastSelectedSlot(client.interactionManager, slot);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        lastChangeTick = currentTick;
        lastSelectedSlot = slot;
        activity.client.module.service.PlayerStateService.updateSelectedSlot(slot);
        return true;
    }

    public static boolean selectSlot(MinecraftClient client, int slot) {
        return selectSlot(client, slot, -1L);
    }

    public static void reset() {
        lastChangeTick = -1L;
        lastSelectedSlot = -1;
        activity.client.module.service.PlayerStateService.updateSelectedSlot(0);
        activity.client.module.service.InventoryScanService.invalidate();
    }
}
