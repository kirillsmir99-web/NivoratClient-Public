package activity.client.module.api;

import activity.client.module.impl.combat.AutoMaceModule;
import activity.client.module.impl.combat.AutoShieldbreakerModule;
import activity.client.module.impl.combat.AutoSpearModule;
import activity.client.module.impl.combat.AutoStunSlamModule;
import activity.client.module.impl.utility.AutoGGModule;
import activity.client.module.impl.utility.AutoToolModule;
import activity.client.module.impl.utility.CartHudModule;
import activity.client.module.impl.utility.HPReaperModule;
import activity.client.module.keybind.KeybindManager;
import activity.client.module.service.InventoryScanService;
import activity.client.module.service.PlayerStateService;
import activity.client.module.service.TargetCacheService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.TickBoundScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated high-performance event dispatcher for NivoratClient.
 *
 * <p>Key Guarantees:
 * <ul>
 *   <li>Single Fabric ClientTick hook, single AttackEntity callback, and single HUD render element.</li>
 *   <li>Fast-path routing: calls <b>only</b> enabled modules that actually require the event.</li>
 *   <li>Zero overhead for disabled modules (0 ns cost; no tick/attack/render iteration).</li>
 *   <li>Zero per-frame / per-tick object allocations in event dispatch loops.</li>
 *   <li>Integrated per-tick lifecycle coordination for shared services (player, inventory, target caches).</li>
 * </ul>
 */
public final class ModuleEventDispatcher {

    private static volatile IModule[] activeTickModules = new IModule[0];
    private static volatile IModule[] activeAttackModules = new IModule[0];
    private static volatile IModule[] activeHudModules = new IModule[0];

    private static boolean eventsRegistered = false;
    private static long clientTickCounter = 0L;

    private ModuleEventDispatcher() {}

    /**
     * Initializes global Fabric client event hooks. Safe to call multiple times.
     */
    public static synchronized void init() {
        if (eventsRegistered) return;
        eventsRegistered = true;

        KeybindManager.markDispatcherManaged();
        TickBoundScheduler.markDispatcherManaged();
        CombatRaytraceGuard.markDispatcherManaged();

        // 1. Single Consolidated Client Tick & Keybind dispatch
        ClientTickEvents.START_CLIENT_TICK.register(ModuleEventDispatcher::onClientTick);

        // 2. Single Consolidated Attack Entity Callback dispatch
        AttackEntityCallback.EVENT.register(ModuleEventDispatcher::onAttackEntity);

        // 3. Single Consolidated HUD Render dispatch
        HudElementRegistry.addLast(Identifier.of("activity", "modules_hud"), ModuleEventDispatcher::onRenderHud);

        updateActiveModules();
    }

    /**
     * Rebuilds fast-path arrays of enabled modules categorized by capability.
     */
    public static synchronized void updateActiveModules() {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            activeTickModules = new IModule[0];
            activeAttackModules = new IModule[0];
            activeHudModules = new IModule[0];
            KeybindManager.clearAllForCapitulation();
            return;
        }

        List<IModule> all = ModuleRegistry.getAll();
        List<IModule> tickList = new ArrayList<>();
        List<IModule> attackList = new ArrayList<>();
        List<IModule> hudList = new ArrayList<>();

        for (IModule module : all) {
            if (module == null || !module.isEnabled()) continue;

            if (supportsTick(module)) {
                tickList.add(module);
            }
            if (supportsAttack(module)) {
                attackList.add(module);
            }
            if (supportsHud(module)) {
                hudList.add(module);
            }
        }

        activeTickModules = tickList.toArray(new IModule[0]);
        activeAttackModules = attackList.toArray(new IModule[0]);
        activeHudModules = hudList.toArray(new IModule[0]);

        KeybindManager.rebuildBoundKeybinds();
    }

    /**
     * Internal tick dispatch called on every Minecraft client tick.
     */
    public static void onClientTick(MinecraftClient client) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }

        if (client == null || client.player == null) {
            PlayerStateService.reset();
            TargetCacheService.reset();
            InventoryScanService.invalidate();
            CombatRaytraceGuard.clearCache();
            activity.client.module.service.CartStateService.reset();
            return;
        }

        long tick = ++clientTickCounter;

        // Advance shared per-tick state caches once
        PlayerStateService.onTick(client, tick);
        TargetCacheService.onTick(client, tick);
        InventoryScanService.onTick(client, tick);
        CombatRaytraceGuard.onTick(tick);

        // Tick scheduler
        TickBoundScheduler.onTick(client);

        // Guaranteed Inventory Screen Opener
        if (client.currentScreen == null && client.options != null && client.options.inventoryKey != null) {
            while (client.options.inventoryKey.wasPressed()) {
                if (client.interactionManager != null && client.interactionManager.hasRidingInventory()) {
                    client.player.openRidingInventory();
                } else {
                    client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
                    break;
                }
            }
        }

        // Keybind evaluation
        KeybindManager.handleTick(client);

        // Dispatch to only enabled modules with tick logic
        IModule[] modules = activeTickModules;
        for (int i = 0; i < modules.length; i++) {
            try {
                modules[i].onTick(client);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Internal attack entity dispatch called on player entity attacks.
     */
    public static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return ActionResult.PASS;
        }

        IModule[] modules = activeAttackModules;
        if (modules.length == 0) return ActionResult.PASS;

        for (int i = 0; i < modules.length; i++) {
            try {
                ActionResult result = modules[i].onAttackEntity(player, world, hand, entity, hitResult);
                if (result != ActionResult.PASS) {
                    return result;
                }
            } catch (Throwable ignored) {}
        }
        return ActionResult.PASS;
    }

    /**
     * Internal HUD render dispatch called on each render frame.
     */
    public static void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }

        IModule[] modules = activeHudModules;
        if (modules.length == 0) return;

        for (int i = 0; i < modules.length; i++) {
            try {
                modules[i].onRenderHud(context, tickCounter);
            } catch (Throwable ignored) {}
        }
    }

    private static boolean isMethodOverridden(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Method method = clazz.getMethod(methodName, parameterTypes);
            Class<?> declaring = method.getDeclaringClass();
            return declaring != IModule.class
                    && declaring != AbstractModule.class
                    && declaring != NivoratModule.class
                    && declaring != activity.client.module.stub.AbstractModuleStub.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean supportsTick(IModule module) {
        if (!module.hasTickLogic()) return false;
        return isMethodOverridden(module.getClass(), "onTick", MinecraftClient.class)
                || isMethodOverridden(module.getClass(), "onClientTick", MinecraftClient.class);
    }

    private static boolean supportsAttack(IModule module) {
        return module instanceof AutoMaceModule
                || module instanceof AutoSpearModule
                || module instanceof AutoShieldbreakerModule
                || module instanceof AutoStunSlamModule
                || module instanceof HPReaperModule
                || module instanceof AutoToolModule
                || module instanceof AutoGGModule
                || isMethodOverridden(module.getClass(), "onAttackEntity", PlayerEntity.class, World.class, Hand.class, Entity.class, EntityHitResult.class);
    }

    private static boolean supportsHud(IModule module) {
        return module instanceof CartHudModule
                || module instanceof HPReaperModule
                || isMethodOverridden(module.getClass(), "onRenderHud", DrawContext.class, RenderTickCounter.class);
    }

    public static IModule[] getActiveTickModules() {
        return activeTickModules;
    }

    public static IModule[] getActiveAttackModules() {
        return activeAttackModules;
    }

    public static IModule[] getActiveHudModules() {
        return activeHudModules;
    }

    public static long getClientTickCount() {
        return clientTickCounter;
    }

    public static void resetTickCountForTest() {
        clientTickCounter = 0L;
    }
}
