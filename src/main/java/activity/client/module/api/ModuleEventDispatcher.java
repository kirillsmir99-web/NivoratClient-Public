package activity.client.module.api;

import activity.client.module.impl.combat.ParticlePhysicsModule;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.impl.combat.VectorStreamModule;
import activity.client.module.impl.combat.MatrixTransformModule;
import activity.client.module.impl.utility.AudioWaveModule;
import activity.client.module.impl.utility.ModelMeshModule;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ModuleEventDispatcher {

    private static volatile IModule[] activeTickModules = new IModule[0];
    private static volatile IModule[] activeAttackModules = new IModule[0];
    private static volatile IModule[] activeHudModules = new IModule[0];

    private static final ClassValue<java.util.Map<String, Boolean>> overrides = new ClassValue<>() {
        @Override protected java.util.Map<String, Boolean> computeValue(Class<?> type) {
            return java.util.Map.of(
                "onTick", resolveOverride(type, "onTick", MinecraftClient.class),
                "onClientTick", resolveOverride(type, "onClientTick", MinecraftClient.class),
                "onAttackEntity", resolveOverride(type, "onAttackEntity", PlayerEntity.class, World.class, Hand.class, Entity.class, EntityHitResult.class),
                "onRenderHud", resolveOverride(type, "onRenderHud", DrawContext.class, RenderTickCounter.class));
        }
    };

    private static boolean eventsRegistered = false;
    private static long clientTickCounter = 0L;
    private static net.minecraft.client.world.ClientWorld sessionWorld;
    private static net.minecraft.client.network.ClientPlayerEntity sessionPlayer;
    private static boolean suspended = true;

    private ModuleEventDispatcher() {}

    public static synchronized void init() {
        if (eventsRegistered) return;
        eventsRegistered = true;

        KeybindManager.markDispatcherManaged();
        TickBoundScheduler.markDispatcherManaged();
        CombatRaytraceGuard.markDispatcherManaged();

        ClientTickEvents.START_CLIENT_TICK.register(ModuleEventDispatcher::onClientTick);

        AttackEntityCallback.EVENT.register(ModuleEventDispatcher::onAttackEntity);

        HudElementRegistry.addLast(Identifier.of("activity", "modules_hud"), ModuleEventDispatcher::onRenderHud);

        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.START_MAIN.register(context -> {
            if (activity.client.capitulation.CapitulationManager.isCapitulated()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                dev.raycast.RaycastPredictorController.getInstance().onRender(client);
                dev.nivorat.arc.AutoCartController.onRender(client);
            }
        });

        updateActiveModules();
    }

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
            if (module == null) continue;
            if (!module.isEnabled()) continue;

            if (supportsTick(module)) {
                tickList.add(module);
            }
            if (supportsAttack(module) && module.isEnabled()) {
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

    public static void onClientTick(MinecraftClient client) {
        boolean sessionChanged = client == null || client.world != sessionWorld || client.player != sessionPlayer;
        boolean blocked = client == null || client.player == null || client.world == null
                || !client.player.isAlive() || client.player.isSpectator() || client.currentScreen != null
                || activity.client.capitulation.CapitulationManager.isCapitulated();
        if (sessionChanged || (blocked && !suspended)) {
            for (IModule module : ModuleRegistry.getAll()) {
                if (!sessionChanged && module.isEnabled() && module.canTickWhileScreenOpen(client)) continue;
                try { module.onSuspend(client); }
                catch (Throwable error) { ModuleDiagnostics.report(module.getId(), "suspend", error); }
            }
            TickBoundScheduler.clear();
            boolean managedScreen = !sessionChanged && ModuleRegistry.getAll().stream()
                    .anyMatch(module -> module.isEnabled() && module.canTickWhileScreenOpen(client));
            if (!managedScreen) net.fabricmc.pack.api.CombatLockManager.reset();
            net.fabricmc.pack.api.SafeSlotManager.reset();
            net.fabricmc.pack.api.SlotArbiter.reset();
            PlayerStateService.reset();
            TargetCacheService.reset();
            InventoryScanService.invalidate();
            CombatRaytraceGuard.clearCache();
            sessionWorld = client != null ? client.world : null;
            sessionPlayer = client != null ? client.player : null;
        }
        suspended = blocked;
        for (IModule module : ModuleRegistry.getAll()) {
            try { module.onCleanupTick(client); }
            catch (Throwable error) { ModuleDiagnostics.report(module.getId(), "cleanup", error); }
        }
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

        if (client.currentScreen != null) {
            KeybindManager.handleTick(client);
            for (IModule module : activeTickModules) {
                if (module.canTickWhileScreenOpen(client)) {
                    try { module.onClientTick(client); }
                    catch (Throwable error) { ModuleDiagnostics.report(module.getId(), "screen-tick", error); }
                }
            }
            return;
        }
        if (blocked) return;

        long tick = ++clientTickCounter;

        PlayerStateService.onTick(client, tick);
        net.fabricmc.pack.api.SlotArbiter.onClientTick(client);
        TargetCacheService.onTick(client, tick);
        InventoryScanService.onTick(client, tick);
        CombatRaytraceGuard.onTick(tick);

        TickBoundScheduler.onTick(client);

        KeybindManager.handleTick(client);

        if (client.player != null && client.player.isUsingItem() && activity.client.module.service.CooldownTrackerService.isTridentItem(client.player.getActiveItem().getItem())) {
            activity.client.module.service.CooldownTrackerService.recordTridentUsed();
        }

        IModule[] modules = activeTickModules;
        for (int i = 0; i < modules.length; i++) {
            if (!modules[i].isEnabled()) continue;
            if (client.currentScreen != null && !modules[i].canTickWhileScreenOpen(client)) continue;
            try {
                modules[i].onClientTick(client);
            } catch (Throwable error) { ModuleDiagnostics.report(modules[i].getId(), "tick", error); }
        }
    }

    public static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        try {
            if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
                return ActionResult.PASS;
            }

            try {
                if (player != null) {
                    ItemStack main = player.getMainHandStack();
                    ItemStack off = player.getOffHandStack();
                    if ((main != null && !main.isEmpty() && activity.client.module.service.CooldownTrackerService.isTridentItem(main.getItem()))
                            || (off != null && !off.isEmpty() && activity.client.module.service.CooldownTrackerService.isTridentItem(off.getItem()))) {
                        activity.client.module.service.CooldownTrackerService.recordTridentUsed();
                    }
                }
            } catch (Throwable ignored) {}

            IModule[] modules = activeAttackModules;
            if (modules == null || modules.length == 0) return ActionResult.PASS;

            for (int i = 0; i < modules.length; i++) {
                try {
                    if (modules[i] != null) {
                        ActionResult result = modules[i].onAttackEntity(player, world, hand, entity, hitResult);
                        if (result != null && result != ActionResult.PASS) {
                            return result;
                        }
                    }
                } catch (Throwable error) { ModuleDiagnostics.report(modules[i].getId(), "attack", error); }
            }
        } catch (Throwable ignored) {
            return ActionResult.PASS;
        }
        return ActionResult.PASS;
    }

    public static void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }

        try {
            activity.client.gui.hud.ActivityHudOverlay.render(context, tickCounter);
        } catch (Throwable ignored) {}

        IModule[] modules = activeHudModules;
        if (modules.length == 0) return;

        for (int i = 0; i < modules.length; i++) {
            try {
                modules[i].onRenderHud(context, tickCounter);
            } catch (Throwable error) { ModuleDiagnostics.report(modules[i].getId(), "hud", error); }
        }
    }

    private static boolean isMethodOverridden(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return overrides.get(clazz).getOrDefault(methodName, false);
    }

    private static boolean resolveOverride(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Method method = clazz.getMethod(methodName, parameterTypes);
            Class<?> declaring = method.getDeclaringClass();
            return declaring != IModule.class
                    && declaring != AbstractModule.class
                    && declaring != CooldownModule.class
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
        return module instanceof ParticlePhysicsModule
                || module instanceof VectorStreamModule
                || module instanceof ShaderPassModule
                || module instanceof MatrixTransformModule
                || module instanceof HPReaperModule
                || module instanceof ModelMeshModule
                || module instanceof AudioWaveModule
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
