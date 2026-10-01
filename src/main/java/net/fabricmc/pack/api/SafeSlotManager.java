package net.fabricmc.pack.api;

import activity.client.mixin.pipeline.PipelineInteractionManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public final class SafeSlotManager {
    private static long lastChangeTick = -1L;
    private static int lastSelectedSlot = -1;

    private SafeSlotManager() {}

    public static boolean setLastSelectedSlot(ClientPlayerInteractionManager manager, int slot) {
        if (manager instanceof PipelineInteractionManagerAccessor accessor && slot >= 0 && slot < 9) {
            accessor.activity$setLastSelectedSlot(slot);
            return true;
        }
        return false;
    }

    public static int getLastSelectedSlot(ClientPlayerInteractionManager manager) {
        if (manager instanceof PipelineInteractionManagerAccessor accessor) {
            return accessor.activity$getLastSelectedSlot();
        }
        return -1;
    }

    public static boolean selectSlot(MinecraftClient client, int slot, long currentTick) {
        return changeSlot(client, slot, currentTick, false);
    }

    public static boolean restoreSlot(MinecraftClient client, int slot) {
        return changeSlot(client, slot, -1L, true);
    }

    private static boolean changeSlot(MinecraftClient client, int slot, long currentTick, boolean restoring) {
        try {
            if (client == null || client.player == null || client.interactionManager == null
                    || !client.player.isAlive() || client.player.isSpectator()
                    || (!restoring && client.currentScreen != null) || slot < 0 || slot >= 9) {
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
            if (client.interactionManager instanceof PipelineInteractionManagerAccessor accessor) {
                accessor.invokeSyncSelectedSlot();
            } else {
                setLastSelectedSlot(client.interactionManager, slot);
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                }
            }
            lastChangeTick = currentTick;
            lastSelectedSlot = slot;
            activity.client.module.service.PlayerStateService.updateSelectedSlot(slot);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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
