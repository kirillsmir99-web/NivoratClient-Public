package ru.elarion.autotool;

import net.fabricmc.api.ClientModInitializer;

public final class AutoToolClient implements ClientModInitializer {
    public static final AutoToolConfig CONFIG = AutoToolConfig.load();

    public AutoToolClient() {}

    @Override
    public void onInitializeClient() {

    }
}
