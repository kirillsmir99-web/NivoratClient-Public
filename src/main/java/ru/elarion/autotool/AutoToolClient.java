package ru.elarion.autotool;

import net.fabricmc.api.ClientModInitializer;

/**
 * Configuration and state holder for the AutoTool engine.
 */
public final class AutoToolClient implements ClientModInitializer {
    public static final AutoToolConfig CONFIG = AutoToolConfig.load();

    public AutoToolClient() {}

    @Override
    public void onInitializeClient() {
        // Compatibility no-op entrypoint
    }
}
