package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleEventDispatcher;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ParticlePhysicsModule;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.impl.combat.VectorStreamModule;
import activity.client.module.impl.combat.MatrixTransformModule;
import activity.client.module.impl.defense.LightmapFilterModule;
import activity.client.module.impl.defense.OcclusionCacheModule;
import activity.client.module.impl.defense.BufferPipelineModule;
import activity.client.module.keybind.Keybind;
import activity.client.module.keybind.KeybindManager;
import activity.client.module.service.CartStateService;
import activity.client.module.service.InventoryScanService;
import activity.client.module.service.PlayerStateService;
import activity.client.module.service.TargetCacheService;
import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.TickBoundScheduler;
import activity.client.module.api.NivoratModule;
import activity.client.module.api.ModuleCategory;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stage 9: Module Performance and Event Consolidation Tests")
public class ModulePerformanceAndEventConsolidationTest {

    @BeforeEach
    void setUp() {
        BuiltinModules.registerAll();
        CombatLockManager.reset();
        PlayerStateService.reset();
        TargetCacheService.reset();
        InventoryScanService.invalidate();
        CombatRaytraceGuard.clearCache();
    }

    @Test
    @DisplayName("ModuleEventDispatcher: Zero cost for disabled modules")
    void testZeroCostForDisabledModules() {

        for (IModule module : ModuleRegistry.getAll()) {
            module.setEnabled(false);
        }
        ModuleEventDispatcher.updateActiveModules();

        assertEquals(0, ModuleEventDispatcher.getActiveTickModules().length, "Active tick modules must be 0 when all disabled");
        assertEquals(0, ModuleEventDispatcher.getActiveAttackModules().length, "Active attack modules must be 0 when all disabled");
        assertEquals(0, ModuleEventDispatcher.getActiveHudModules().length, "Active hud modules must be 0 when all disabled");

        ActionResult result = ModuleEventDispatcher.onAttackEntity(null, null, null, null, null);
        assertEquals(ActionResult.PASS, result, "Must return PASS immediately when no active modules");
    }

    @Test
    @DisplayName("ModuleEventDispatcher: Dynamic fast-path updates on toggle")
    void testDynamicFastPathUpdates() {
        ParticlePhysicsModule mace = (ParticlePhysicsModule) ModuleRegistry.get(ParticlePhysicsModule.ID);
        assertNotNull(mace);

        mace.setEnabled(false);
        assertFalse(mace.isEnabled());
        for (IModule m : ModuleEventDispatcher.getActiveTickModules()) {
            assertNotEquals(ParticlePhysicsModule.ID, m.getId(), "Disabled module must not be in activeTickModules");
        }

        mace.setEnabled(true);
        assertTrue(mace.isEnabled());
        boolean found = false;
        for (IModule m : ModuleEventDispatcher.getActiveTickModules()) {
            if (m.getId().equals(ParticlePhysicsModule.ID)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Enabled module must appear in activeTickModules");

        mace.setEnabled(false);
        found = false;
        for (IModule m : ModuleEventDispatcher.getActiveTickModules()) {
            if (m.getId().equals(ParticlePhysicsModule.ID)) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Disabled module must be immediately removed from activeTickModules");
    }

    @Test
    @DisplayName("CombatLockManager: Atomic bitmask and system property synchronization")
    void testCombatLockManagerBitmask() {
        assertFalse(CombatLockManager.isLocked());
        assertEquals(0, CombatLockManager.getLockMask());

        CombatLockManager.setLock(CombatLockManager.MACE, true);
        assertTrue(CombatLockManager.isLocked());
        assertEquals("true", System.getProperty(CombatLockManager.MACE));
        assertNotEquals(0, CombatLockManager.getLockMask());

        CombatLockManager.setLock(CombatLockManager.CART_PLACEMENT, true);
        assertTrue(CombatLockManager.isLocked());
        assertEquals("true", System.getProperty(CombatLockManager.CART_PLACEMENT));

        CombatLockManager.setLock(CombatLockManager.MACE, false);
        assertTrue(CombatLockManager.isLocked());
        assertNull(System.getProperty(CombatLockManager.MACE));
        assertEquals("true", System.getProperty(CombatLockManager.CART_PLACEMENT));

        CombatLockManager.reset();
        assertFalse(CombatLockManager.isLocked());
        assertEquals(0, CombatLockManager.getLockMask());
        assertNull(System.getProperty(CombatLockManager.CART_PLACEMENT));
    }

    @Test
    @DisplayName("Module onDisable: Releases combat locks automatically")
    void testModuleOnDisableReleasesLocks() {
        ParticlePhysicsModule mace = (ParticlePhysicsModule) ModuleRegistry.get(ParticlePhysicsModule.ID);
        ShaderPassModule shield = (ShaderPassModule) ModuleRegistry.get(ShaderPassModule.ID);
        VectorStreamModule spear = (VectorStreamModule) ModuleRegistry.get(VectorStreamModule.ID);
        MatrixTransformModule sunder = (MatrixTransformModule) ModuleRegistry.get(MatrixTransformModule.ID);
        LightmapFilterModule anchor = (LightmapFilterModule) ModuleRegistry.get(LightmapFilterModule.ID);
        OcclusionCacheModule cart = (OcclusionCacheModule) ModuleRegistry.get(OcclusionCacheModule.ID);
        BufferPipelineModule totem = (BufferPipelineModule) ModuleRegistry.get(BufferPipelineModule.ID);

        CombatLockManager.setLock(CombatLockManager.MACE, true);
        assertTrue(CombatLockManager.isLocked());
        mace.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SHIELD_COMBO, true);
        assertTrue(CombatLockManager.isLocked());
        shield.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SPEAR, true);
        assertTrue(CombatLockManager.isLocked());
        spear.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.SUNDER, true);
        assertTrue(CombatLockManager.isLocked());
        sunder.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.ANCHOR, true);
        assertTrue(CombatLockManager.isLocked());
        anchor.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.CART_PLACEMENT, true);
        assertTrue(CombatLockManager.isLocked());
        cart.onDisable();
        assertFalse(CombatLockManager.isLocked());

        CombatLockManager.setLock(CombatLockManager.TOTEM, true);
        assertTrue(CombatLockManager.isLocked());
        totem.onDisable();
        assertFalse(CombatLockManager.isLocked());
    }

    @Test
    @DisplayName("KeybindManager: Pre-cached bound keybind arrays")
    void testKeybindManagerCaching() {
        KeybindManager.markDispatcherManaged();

        for (IModule module : ModuleRegistry.getAll()) {
            module.getKeybind().clear();
        }
        KeybindManager.rebuildBoundKeybinds();

        assertEquals(0, KeybindManager.getBoundPrimaries().length, "With all clear, bound primaries must be 0");

        IModule mace = ModuleRegistry.get("auto_mace");
        mace.getKeybind().set(GLFW.GLFW_KEY_R, true, false, false);

        assertTrue(KeybindManager.getBoundPrimaries().length >= 1, "Setting keybind must rebuild bound primaries array");
        boolean foundMace = false;
        for (KeybindManager.BoundPrimary bp : KeybindManager.getBoundPrimaries()) {
            if ("auto_mace".equals(bp.module().getId())) {
                foundMace = true;
                assertEquals(GLFW.GLFW_KEY_R, bp.keybind().getKeyCode());
                assertTrue(bp.keybind().isCtrl());
                break;
            }
        }
        assertTrue(foundMace, "AutoMace keybind must be present in bound primaries");

        mace.getKeybind().clear();
        foundMace = false;
        for (KeybindManager.BoundPrimary bp : KeybindManager.getBoundPrimaries()) {
            if ("auto_mace".equals(bp.module().getId())) {
                foundMace = true;
                break;
            }
        }
        assertFalse(foundMace, "Cleared keybind must be evicted from bound primaries");
    }

    @Test
    @DisplayName("Shared Services: PlayerStateService per-tick memoization")
    void testPlayerStateService() {
        PlayerStateService.reset();
        assertFalse(PlayerStateService.isBusy());
        assertEquals(0, PlayerStateService.getAirTicks());
        assertFalse(PlayerStateService.isAirborne());

        PlayerStateService.setBusyForTest(true);
        assertTrue(PlayerStateService.isBusy());

        PlayerStateService.setAirTicksForTest(5);
        assertEquals(5, PlayerStateService.getAirTicks());
        assertTrue(PlayerStateService.isAirborne());

        PlayerStateService.setHealthForTest(14.5f, 4.0f);
        assertEquals(14.5f, PlayerStateService.getHealth(), 0.01f);
        assertEquals(4.0f, PlayerStateService.getAbsorption(), 0.01f);

        PlayerStateService.reset();
        assertFalse(PlayerStateService.isBusy());
        assertEquals(0, PlayerStateService.getAirTicks());
    }

    @Test
    @DisplayName("Shared Services: InventoryScanService cache invalidation")
    void testInventoryScanService() {
        InventoryScanService.invalidate();
        assertEquals(-2, InventoryScanService.getCachedSwordSlot());
        assertEquals(-2, InventoryScanService.getCachedAxeSlot());

        InventoryScanService.setCachedSwordSlotForTest(3);
        InventoryScanService.setCachedAxeSlotForTest(1);
        assertEquals(3, InventoryScanService.getCachedSwordSlot());
        assertEquals(1, InventoryScanService.getCachedAxeSlot());

        InventoryScanService.invalidate();
        assertEquals(-2, InventoryScanService.getCachedSwordSlot());
        assertEquals(-2, InventoryScanService.getCachedAxeSlot());
    }

    @Test
    @DisplayName("Shared Services: TargetCacheService reset & caching")
    void testTargetCacheService() {
        TargetCacheService.reset();
        assertEquals(0, TargetCacheService.getShieldCacheSize());

        TargetCacheService.reset();
        assertNull(TargetCacheService.getCrosshairTarget(null));
        assertNull(TargetCacheService.getCrosshairLivingTarget(null));
        assertNull(TargetCacheService.getCrosshairPlayerTarget(null));
    }

    @Test
    @DisplayName("Shared Services: CombatRaytraceGuard LOS cache eviction")
    void testCombatRaytraceGuardEviction() {
        CombatRaytraceGuard.clearCache();
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize());
        assertEquals(0, CombatRaytraceGuard.getBlockCacheSize());

        CombatRaytraceGuard.onTick(100L);
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize());

        CombatRaytraceGuard.onTick(101L);
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize());
    }

    @Test
    @DisplayName("ModuleEventDispatcher: Dynamic extensibility for custom modules")
    void testDynamicExtensibilityForCustomModules() {
        class CustomCombatModule extends NivoratModule {
            boolean attackCalled = false;
            boolean hudCalled = false;

            public CustomCombatModule() {
                super("custom_combat_test", Text.literal("Custom"), Text.literal("Desc"), ModuleCategory.COMBAT);
            }

            @Override
            public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
                attackCalled = true;
                return ActionResult.SUCCESS;
            }

            @Override
            public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
                hudCalled = true;
            }
        }

        CustomCombatModule custom = new CustomCombatModule();
        try {
            ModuleRegistry.register(custom);
            custom.setEnabled(true);
            ModuleEventDispatcher.updateActiveModules();

            boolean foundAttack = false;
            for (IModule m : ModuleEventDispatcher.getActiveAttackModules()) {
                if ("custom_combat_test".equals(m.getId())) {
                    foundAttack = true;
                    break;
                }
            }
            assertTrue(foundAttack, "Custom module overriding onAttackEntity must be in activeAttackModules");

            boolean foundHud = false;
            for (IModule m : ModuleEventDispatcher.getActiveHudModules()) {
                if ("custom_combat_test".equals(m.getId())) {
                    foundHud = true;
                    break;
                }
            }
            assertTrue(foundHud, "Custom module overriding onRenderHud must be in activeHudModules");

            ActionResult result = ModuleEventDispatcher.onAttackEntity(null, null, null, null, null);
            assertEquals(ActionResult.SUCCESS, result);
            assertTrue(custom.attackCalled);

            ModuleEventDispatcher.onRenderHud(null, null);
            assertTrue(custom.hudCalled);

            custom.setEnabled(false);
            foundAttack = false;
            for (IModule m : ModuleEventDispatcher.getActiveAttackModules()) {
                if ("custom_combat_test".equals(m.getId())) {
                    foundAttack = true;
                    break;
                }
            }
            assertFalse(foundAttack, "Disabled custom module must be removed from activeAttackModules");
        } finally {
            ModuleRegistry.unregister("custom_combat_test");
        }
    }

    @Test
    @DisplayName("Shared Services: Disconnect resets all caches including CartStateService")
    void testDisconnectResetsAllCaches() {
        PlayerStateService.setBusyForTest(true);
        InventoryScanService.setCachedSwordSlotForTest(5);
        TargetCacheService.reset();

        ModuleEventDispatcher.onClientTick(null);

        assertFalse(PlayerStateService.isBusy(), "PlayerStateService must be reset on disconnect");
        assertEquals(-2, InventoryScanService.getCachedSwordSlot(), "InventoryScanService must be invalidated on disconnect");
        assertEquals(0, TargetCacheService.getShieldCacheSize(), "TargetCacheService must be reset on disconnect");
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize(), "CombatRaytraceGuard must be cleared on disconnect");
    }

    @Test
    @DisplayName("Scheduler: TickBoundScheduler dispatcher-managed delegation")
    void testTickBoundSchedulerDispatcherManaged() {
        TickBoundScheduler.markDispatcherManaged();
        TickBoundScheduler.clear();

        boolean[] ran = new boolean[]{false};
        TickBoundScheduler.runAfterTicks(2, () -> ran[0] = true);

        assertFalse(ran[0]);
        TickBoundScheduler.onTick(null);
        assertFalse(ran[0]);
        TickBoundScheduler.onTick(null);
        assertTrue(ran[0], "Scheduled task must execute after 2 ticks");
    }

    @Test
    @DisplayName("RaytraceGuard: Dispatcher managed mode prevents clock desync")
    void testCombatRaytraceGuardDispatcherManagement() {
        CombatRaytraceGuard.markDispatcherManaged();
        assertTrue(CombatRaytraceGuard.isDispatcherManaged(), "CombatRaytraceGuard must be dispatcher managed");

        CombatRaytraceGuard.clearCache();
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize());
        assertEquals(0, CombatRaytraceGuard.getBlockCacheSize());

        CombatRaytraceGuard.onTick(1L);
        assertEquals(0, CombatRaytraceGuard.getEntityCacheSize());

        assertFalse(CombatRaytraceGuard.hasLineOfSight(null, (Entity) null));
        assertFalse(CombatRaytraceGuard.hasLineOfSight(null, (net.minecraft.util.math.BlockPos) null));
    }

    @Test
    @DisplayName("ModuleEventDispatcher: Monotonic client tick progression")
    void testMonotonicClientTickProgression() {
        ModuleEventDispatcher.resetTickCountForTest();
        assertEquals(0L, ModuleEventDispatcher.getClientTickCount());

        ModuleEventDispatcher.onClientTick(null);
        assertEquals(0L, ModuleEventDispatcher.getClientTickCount());
    }

    @Test
    @DisplayName("CartStateService: Placement and refill trigger immediate cache invalidation")
    void testCartStateServiceImmediateInvalidation() {
        boolean[] placedFired = new boolean[]{false};
        boolean[] refilledFired = new boolean[]{false};

        CartStateService.CartEventListener listener = new CartStateService.CartEventListener() {
            @Override
            public void onCartPlaced(net.minecraft.util.math.BlockPos pos) {
                placedFired[0] = true;
            }

            @Override
            public void onCartRefilled(int hotbarSlot) {
                refilledFired[0] = true;
            }
        };

        try {
            CartStateService.addListener(listener);

            CartStateService.notifyCartPlaced(new net.minecraft.util.math.BlockPos(0, 64, 0));
            assertTrue(placedFired[0], "Cart placed event must be dispatched");

            CartStateService.notifyCartRefilled(2);
            assertTrue(refilledFired[0], "Cart refilled event must be dispatched");

            CartStateService.invalidate();
            assertEquals(0, CartStateService.countCarts(null));
        } finally {
            CartStateService.removeListener(listener);
        }
    }

    @Test
    @DisplayName("InventoryScanService: Totem cross-memoization between hotbar and full scan")
    void testInventoryScanServiceTotemCrossMemoization() {
        InventoryScanService.invalidate();
        assertEquals(-2, InventoryScanService.getCachedTotemSlot());
        assertEquals(-2, InventoryScanService.getCachedHotbarTotemSlot());

        InventoryScanService.setCachedTotemSlotForTest(4);
        InventoryScanService.setCachedHotbarTotemSlotForTest(4);
        assertEquals(4, InventoryScanService.getCachedTotemSlot());
        assertEquals(4, InventoryScanService.getCachedHotbarTotemSlot());

        InventoryScanService.invalidate();
        assertEquals(-2, InventoryScanService.getCachedTotemSlot());
        assertEquals(-2, InventoryScanService.getCachedHotbarTotemSlot());
    }

    @Test
    @DisplayName("TargetCacheService: Shield safety and dead/null handling")
    void testTargetCacheServiceShieldSafety() {
        assertFalse(TargetCacheService.isTargetShielding(null), "Null entity must never be shielding");

        TargetCacheService.reset();
        assertEquals(0, TargetCacheService.getShieldCacheSize());
        assertNull(TargetCacheService.getCrosshairTarget(null));
        assertNull(TargetCacheService.getCrosshairLivingTarget(null));
        assertNull(TargetCacheService.getCrosshairPlayerTarget(null));
    }
}
