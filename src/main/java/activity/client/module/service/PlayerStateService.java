package activity.client.module.service;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class PlayerStateService {

    private static long lastTickId = -1L;
    private static int airTicks = 0;
    private static long lastOnGroundTimeMs = 0L;
    private static boolean playerBusy = false;
    private static float playerHealth = 20.0f;
    private static float playerAbsorption = 0.0f;
    private static int selectedSlot = 0;
    private static boolean onGround = true;
    private static int lastNonTotemSlot = -1;
    private static net.minecraft.item.Item lastNonTotemItem = null;

    private PlayerStateService() {}

    public static void onTick(MinecraftClient client, long tickId) {
        if (client == null || client.player == null) {
            reset();
            return;
        }

        if (tickId == lastTickId && tickId >= 0) {
            return;
        }
        lastTickId = tickId;

        ClientPlayerEntity player = client.player;
        onGround = player.isOnGround() || player.verticalCollision || player.isTouchingWater() || player.isClimbing() || player.hasVehicle();
        if (onGround) {
            airTicks = 0;
            lastOnGroundTimeMs = System.currentTimeMillis();
        } else {
            airTicks++;
        }

        playerBusy = !player.isAlive()
                || player.isSpectator()
                || player.isSleeping()
                || player.hasVehicle()
                || player.isGliding()
                || player.isUsingItem()
                || player.isBlocking();

        playerHealth = player.getHealth();
        playerAbsorption = player.getAbsorptionAmount();
        if (player.getInventory() != null) {
            int cur = player.getInventory().getSelectedSlot();
            selectedSlot = cur;
            net.minecraft.item.ItemStack stack = player.getInventory().getStack(cur);
            if (!stack.isEmpty() && !stack.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) {
                lastNonTotemSlot = cur;
                lastNonTotemItem = stack.getItem();
            }
        } else {
            selectedSlot = 0;
        }
    }

    public static boolean isBusy() {
        return playerBusy;
    }

    public static boolean isBusy(ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) return true;
        if (playerBusy) return true;
        return player.isSpectator()
                || player.isSleeping()
                || player.hasVehicle()
                || player.isGliding()
                || player.isUsingItem()
                || player.isBlocking();
    }

    public static int getAirTicks() {
        return airTicks;
    }

    public static boolean isAirborne() {
        return !onGround && airTicks > 0;
    }

    public static long getAirDurationMs() {
        return (!onGround) ? Math.max(0L, System.currentTimeMillis() - lastOnGroundTimeMs) : 0L;
    }

    public static float getHealth() {
        return playerHealth;
    }

    public static float getAbsorption() {
        return playerAbsorption;
    }

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    public static void updateSelectedSlot(int slot) {
        selectedSlot = slot;
    }

    public static void setAirTicksForTest(int ticks) {
        airTicks = ticks;
        onGround = (ticks == 0);
    }

    public static void setBusyForTest(boolean busy) {
        playerBusy = busy;
    }

    public static void setHealthForTest(float health, float absorption) {
        playerHealth = health;
        playerAbsorption = absorption;
    }

    public static int getLastNonTotemSlot() {
        return lastNonTotemSlot;
    }

    public static net.minecraft.item.Item getLastNonTotemItem() {
        return lastNonTotemItem;
    }

    public static void setLastNonTotemSlotForTest(int slot, net.minecraft.item.Item item) {
        lastNonTotemSlot = slot;
        lastNonTotemItem = item;
    }

    public static void reset() {
        lastTickId = -1L;
        airTicks = 0;
        lastOnGroundTimeMs = System.currentTimeMillis();
        playerBusy = false;
        playerHealth = 20.0f;
        playerAbsorption = 0.0f;
        selectedSlot = 0;
        onGround = true;
        lastNonTotemSlot = -1;
        lastNonTotemItem = null;
    }
}
