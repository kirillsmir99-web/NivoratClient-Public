package ru.elarion.autotool;

/**
 * Configuration and state holder for the AutoTool engine.
 */
public final class AutoToolClient {
    public static final AutoToolConfig CONFIG = AutoToolConfig.load();

    private AutoToolClient() {}
}
