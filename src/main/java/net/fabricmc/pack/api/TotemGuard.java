package net.fabricmc.pack.api;

import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.defense.BufferPipelineModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class TotemGuard {
    private TotemGuard() {}

    public static boolean isArmed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;
        if (client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        var mod = ModuleRegistry.get(BufferPipelineModule.ID);
        if (mod instanceof BufferPipelineModule bpm && bpm.isEnabled()) {
            return bpm.getController().isArmed();
        }
        return false;
    }
}
