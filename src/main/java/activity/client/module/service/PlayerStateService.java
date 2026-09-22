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
        selectedSlot = player.getInventory() != null ? player.getInventory().getSelectedSlot() : 0;
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

    public static void reset() {
        lastTickId = -1L;
        airTicks = 0;
        lastOnGroundTimeMs = System.currentTimeMillis();
        playerBusy = false;
        playerHealth = 20.0f;
        playerAbsorption = 0.0f;
        selectedSlot = 0;
        onGround = true;
    }
}
