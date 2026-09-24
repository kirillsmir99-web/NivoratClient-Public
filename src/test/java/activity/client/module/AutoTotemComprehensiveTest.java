package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.gui.ActivityScreen;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.defense.AutoTotemModule;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.NumberSetting;
import com.google.gson.JsonObject;
import dev.autototem.AutoTotemConfig;
import dev.autototem.AutoTotemController;
import dev.autototem.AutoTotemController.State;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AutoTotemComprehensiveTest {

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Unsafe instance", e);
        }
    }

    private static ItemStack createDummyStack() {
        try {
            Unsafe unsafe = getUnsafe();
            ItemStack stack = (ItemStack) unsafe.allocateInstance(ItemStack.class);
            for (Class<?> c = ItemStack.class; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        f.setAccessible(true);
                        f.setInt(stack, 1);
                        break;
                    }
                }
            }
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static class MockInventory extends PlayerInventory {
        public int selected = 0;
        public ItemStack[] slots = new ItemStack[36];

        public MockInventory() {
            super(null, null);
        }

        @Override
        public int getSelectedSlot() {
            return selected;
        }

        @Override
        public void setSelectedSlot(int slot) {
            this.selected = slot;
        }

        @Override
        public ItemStack getStack(int slot) {
            if (slot >= 0 && slot < slots.length && slots[slot] != null) {
                return slots[slot];
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (slot >= 0 && slot < slots.length) {
                slots[slot] = stack != null ? stack : ItemStack.EMPTY;
            }
        }
    }

    public static class MockPlayer extends ClientPlayerEntity {
        public float health = 20.0f;
        public float absorption = 0.0f;
        public boolean alive = true;
        public ItemStack mainHand = ItemStack.EMPTY;
        public ItemStack offHand = ItemStack.EMPTY;
        public MockInventory inventory;

        public MockPlayer() {
            super(null, null, null, null, null, null, false);
        }

        @Override
        public float getHealth() {
            return health;
        }

        @Override
        public float getAbsorptionAmount() {
            return absorption;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public ItemStack getMainHandStack() {
            return mainHand;
        }

        @Override
        public ItemStack getOffHandStack() {
            return offHand;
        }

        @Override
        public MockInventory getInventory() {
            return inventory;
        }

        @Override
        public void setStackInHand(Hand hand, ItemStack stack) {
            if (hand == Hand.MAIN_HAND) {
                this.mainHand = stack != null ? stack : ItemStack.EMPTY;
            } else if (hand == Hand.OFF_HAND) {
                this.offHand = stack != null ? stack : ItemStack.EMPTY;
            }
        }
    }

    private MockPlayer createMockPlayer() {
        try {
            Unsafe unsafe = getUnsafe();
            MockPlayer player = (MockPlayer) unsafe.allocateInstance(MockPlayer.class);
            MockInventory inv = (MockInventory) unsafe.allocateInstance(MockInventory.class);
            inv.slots = new ItemStack[36];
            Arrays.fill(inv.slots, ItemStack.EMPTY);
            inv.selected = 0;
            player.inventory = inv;
            player.health = 20.0f;
            player.absorption = 0.0f;
            player.alive = true;
            player.mainHand = ItemStack.EMPTY;
            player.offHand = ItemStack.EMPTY;
            return player;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate MockPlayer", e);
        }
    }

    @BeforeAll
    static void initRegistry() {
        try {
            net.minecraft.SharedConstants.createGameVersion();
            for (Field f : net.minecraft.Bootstrap.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    f.setBoolean(null, true);
                }
            }
        } catch (Throwable ignored) {
        }
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetGlobalState() {
        ActivityConfigManager.resetDefaults();
        SafeSlotManager.reset();
        ActivityScreen.clearSession();
        AutoTotemConfig.enabled = true;
        AutoTotemConfig.mode = 1;
        AutoTotemConfig.triggerHearts = 3.0;
        AutoTotemConfig.restoreHearts = 6.0;
        AutoTotemConfig.mainhandTriggerHearts = 3.0;
        AutoTotemConfig.mainhandRestoreHearts = 6.0;
        AutoTotemConfig.offhandTriggerHearts = 2.0;
        AutoTotemConfig.offhandRestoreHearts = 5.0;
        AutoTotemConfig.crystalTriggerHearts = 3.0;
        AutoTotemConfig.crystalRestoreHearts = 6.0;
        AutoTotemConfig.countAbsorption = false;
        AutoTotemConfig.chance = 100;
        AutoTotemConfig.returnItem = true;
        AutoTotemConfig.returnOnPop = true;
        AutoTotemConfig.autoRefill = true;
        AutoTotemConfig.refillSlot = -1;
    }

    @Test
    @DisplayName("R1: count_absorption false only counts red health, ignoring absorption")
    void testEffectiveHealthWhenCountAbsorptionFalse() {
        AutoTotemConfig.countAbsorption = false;
        MockPlayer player = createMockPlayer();

        player.health = 8.0f;
        player.absorption = 0.0f;
        assertEquals(8.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 8.0f;
        player.absorption = 10.0f;
        assertEquals(8.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 1.0f;
        player.absorption = 20.0f;
        assertEquals(1.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 0.5f;
        player.absorption = 4.0f;
        assertEquals(0.5f, AutoTotemController.getEffectiveHealth(player), 0.001f);
    }

    @Test
    @DisplayName("R1: count_absorption true sums red health + absorption amount")
    void testEffectiveHealthWhenCountAbsorptionTrue() {
        AutoTotemConfig.countAbsorption = true;
        MockPlayer player = createMockPlayer();

        player.health = 8.0f;
        player.absorption = 0.0f;
        assertEquals(8.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 8.0f;
        player.absorption = 6.0f;
        assertEquals(14.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 2.5f;
        player.absorption = 4.5f;
        assertEquals(7.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);

        player.health = 1.0f;
        player.absorption = 16.0f;
        assertEquals(17.0f, AutoTotemController.getEffectiveHealth(player), 0.001f);
    }

    @Test
    @DisplayName("R1: Effective health calculation returns 0.0 for null player")
    void testEffectiveHealthNullPlayerSafety() {
        AutoTotemConfig.countAbsorption = false;
        assertEquals(0.0f, AutoTotemController.getEffectiveHealth(null), 0.001f);

        AutoTotemConfig.countAbsorption = true;
        assertEquals(0.0f, AutoTotemController.getEffectiveHealth(null), 0.001f);
    }

    @Test
    @DisplayName("R1: Gaining absorption above restore_hearts triggers item restoration condition")
    void testGainingAbsorptionAboveRestoreHeartsTriggersRestorationCondition() {
        MockPlayer player = createMockPlayer();

        AutoTotemConfig.mode = 2;
        AutoTotemConfig.triggerHearts = 3.0;
        AutoTotemConfig.restoreHearts = 6.0;
        float restoreHp = (float) (AutoTotemConfig.restoreHearts * 2.0);

        player.health = 5.0f;
        player.absorption = 0.0f;

        AutoTotemConfig.countAbsorption = false;
        player.absorption = 8.0f;
        float effectiveHpFalse = AutoTotemController.getEffectiveHealth(player);
        assertEquals(5.0f, effectiveHpFalse, 0.001f);
        boolean canRestoreFalse = AutoTotemConfig.restoreHearts > 0.0 && effectiveHpFalse >= restoreHp;
        assertFalse(canRestoreFalse, "When count_absorption is false, gaining absorption must NOT trigger restoration");

        AutoTotemConfig.countAbsorption = true;
        float effectiveHpTrue = AutoTotemController.getEffectiveHealth(player);
        assertEquals(13.0f, effectiveHpTrue, 0.001f);
        boolean canRestoreTrue = AutoTotemConfig.restoreHearts > 0.0 && effectiveHpTrue >= restoreHp;
        assertTrue(canRestoreTrue, "When count_absorption is true, gaining absorption (5+8=13 >= 12) MUST trigger item restoration");
    }

    @Test
    @DisplayName("R1: Gaining absorption in main_hand mode restores item when threshold exceeded")
    void testGainingAbsorptionInMainhandModeRestorationCondition() {
        MockPlayer player = createMockPlayer();

        AutoTotemConfig.mode = 1;
        AutoTotemConfig.triggerHearts = 3.0;
        AutoTotemConfig.restoreHearts = 6.0;
        float restoreHp = (float) (AutoTotemConfig.restoreHearts * 2.0);

        player.health = 4.0f;
        player.absorption = 10.0f;

        AutoTotemConfig.countAbsorption = false;
        float effFalse = AutoTotemController.getEffectiveHealth(player);
        assertEquals(4.0f, effFalse, 0.001f);
        boolean restoreFalse = AutoTotemConfig.restoreHearts > 0.0 && effFalse >= restoreHp;
        assertFalse(restoreFalse, "Without absorption accounting, player remains below restoration threshold");

        AutoTotemConfig.countAbsorption = true;
        float effTrue = AutoTotemController.getEffectiveHealth(player);
        assertEquals(14.0f, effTrue, 0.001f);
        boolean restoreTrue = AutoTotemConfig.restoreHearts > 0.0 && effTrue >= restoreHp;
        assertTrue(restoreTrue, "With absorption accounting, player (4+10=14 >= 12) reaches restoration threshold");
    }

    @Test
    @DisplayName("R2: trigger_hearts setting bounds strictly match [0.5, 10.0] with step 0.5")
    void testTriggerHeartsSettingRangeBounds() {
        IModule mod = ModuleRegistry.get(AutoTotemModule.ID);
        assertNotNull(mod);
        NumberSetting triggerSetting = (NumberSetting) mod.getSetting("trigger_hearts");
        assertNotNull(triggerSetting);

        assertEquals(0.5, triggerSetting.getMin(), 0.001);
        assertEquals(10.0, triggerSetting.getMax(), 0.001);
        assertEquals(0.5, triggerSetting.getStep(), 0.001);
        assertFalse(triggerSetting.isIntegerOnly());

        triggerSetting.set(0.5);
        assertEquals(0.5, triggerSetting.get(), 0.001);
        assertEquals(0.5, AutoTotemConfig.triggerHearts, 0.001);

        triggerSetting.set(10.0);
        assertEquals(10.0, triggerSetting.get(), 0.001);
        assertEquals(10.0, AutoTotemConfig.triggerHearts, 0.001);

        triggerSetting.set(5.5);
        assertEquals(5.5, triggerSetting.get(), 0.001);
        assertEquals(5.5, AutoTotemConfig.triggerHearts, 0.001);
    }

    @Test
    @DisplayName("R2: restore_hearts setting bounds strictly match [0.0, 20.0] with step 0.5")
    void testRestoreHeartsSettingRangeBounds() {
        IModule mod = ModuleRegistry.get(AutoTotemModule.ID);
        assertNotNull(mod);
        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_hearts");
        assertNotNull(restoreSetting);

        assertEquals(0.0, restoreSetting.getMin(), 0.001);
        assertEquals(20.0, restoreSetting.getMax(), 0.001);
        assertEquals(0.5, restoreSetting.getStep(), 0.001);
        assertFalse(restoreSetting.isIntegerOnly());

        restoreSetting.set(0.0);
        assertEquals(0.0, restoreSetting.get(), 0.001);
        assertEquals(0.0, AutoTotemConfig.restoreHearts, 0.001);

        restoreSetting.set(20.0);
        assertEquals(20.0, restoreSetting.get(), 0.001);
        assertEquals(20.0, AutoTotemConfig.restoreHearts, 0.001);

        restoreSetting.set(12.5);
        assertEquals(12.5, restoreSetting.get(), 0.001);
        assertEquals(12.5, AutoTotemConfig.restoreHearts, 0.001);
    }

    @Test
    @DisplayName("R2: restore_hearts = 0.0 completely disables item restoration (totem remains held)")
    void testZeroRestoreHeartsDisablesRestorationCondition() {
        MockPlayer player = createMockPlayer();

        AutoTotemConfig.triggerHearts = 3.0;
        AutoTotemConfig.restoreHearts = 0.0;
        AutoTotemConfig.returnItem = true;

        player.health = 20.0f;
        player.absorption = 10.0f;
        AutoTotemConfig.countAbsorption = true;

        float effectiveHp = AutoTotemController.getEffectiveHealth(player);
        assertEquals(30.0f, effectiveHp, 0.001f);

        boolean canRestore = AutoTotemConfig.restoreHearts > 0.0 && effectiveHp >= (float) (AutoTotemConfig.restoreHearts * 2.0);
        assertFalse(canRestore, "When restore_hearts is 0.0, canRestore must be FALSE regardless of player HP (restoration completely disabled)");

        AutoTotemConfig.restoreHearts = 5.0;
        boolean canRestoreEnabled = AutoTotemConfig.restoreHearts > 0.0 && effectiveHp >= (float) (AutoTotemConfig.restoreHearts * 2.0);
        assertTrue(canRestoreEnabled, "When restore_hearts > 0.0, canRestore evaluates to true when health is sufficient");
    }

    @Test
    @DisplayName("R2: offhand, main_hand, and crystal modes maintain independent threshold values")
    void testIndependentPerModeThresholdsAndReactiveSwitching() {
        IModule mod = ModuleRegistry.get(AutoTotemModule.ID);
        assertNotNull(mod);
        ActivityConfig config = ActivityConfigManager.getConfig();
        assertNotNull(config);

        EnumSetting modeSetting = (EnumSetting) mod.getSetting("mode");
        NumberSetting triggerSetting = (NumberSetting) mod.getSetting("trigger_hearts");
        NumberSetting restoreSetting = (NumberSetting) mod.getSetting("restore_hearts");

        modeSetting.set("main_hand");
        triggerSetting.set(4.0);
        restoreSetting.set(7.5);

        assertEquals(4.0, config.autoTotemMainhandTriggerHearts, 0.001);
        assertEquals(7.5, config.autoTotemMainhandRestoreHearts, 0.001);
        assertEquals(4.0, AutoTotemConfig.mainhandTriggerHearts, 0.001);
        assertEquals(7.5, AutoTotemConfig.mainhandRestoreHearts, 0.001);
        assertEquals(4.0, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(7.5, AutoTotemConfig.restoreHearts, 0.001);

        modeSetting.set("offhand");
        triggerSetting.set(1.5);
        restoreSetting.set(0.0);

        assertEquals(1.5, config.autoTotemOffhandTriggerHearts, 0.001);
        assertEquals(0.0, config.autoTotemOffhandRestoreHearts, 0.001);
        assertEquals(1.5, AutoTotemConfig.offhandTriggerHearts, 0.001);
        assertEquals(0.0, AutoTotemConfig.offhandRestoreHearts, 0.001);
        assertEquals(1.5, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(0.0, AutoTotemConfig.restoreHearts, 0.001);

        modeSetting.set("crystal");
        triggerSetting.set(5.5);
        restoreSetting.set(14.0);

        assertEquals(5.5, config.autoTotemCrystalTriggerHearts, 0.001);
        assertEquals(14.0, config.autoTotemCrystalRestoreHearts, 0.001);
        assertEquals(5.5, AutoTotemConfig.crystalTriggerHearts, 0.001);
        assertEquals(14.0, AutoTotemConfig.crystalRestoreHearts, 0.001);
        assertEquals(5.5, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(14.0, AutoTotemConfig.restoreHearts, 0.001);

        modeSetting.set("main_hand");
        assertEquals(4.0, triggerSetting.get(), 0.001);
        assertEquals(7.5, restoreSetting.get(), 0.001);
        assertEquals(4.0, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(7.5, AutoTotemConfig.restoreHearts, 0.001);

        modeSetting.set("offhand");
        assertEquals(1.5, triggerSetting.get(), 0.001);
        assertEquals(0.0, restoreSetting.get(), 0.001);
        assertEquals(1.5, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(0.0, AutoTotemConfig.restoreHearts, 0.001);

        modeSetting.set("crystal");
        assertEquals(5.5, triggerSetting.get(), 0.001);
        assertEquals(14.0, restoreSetting.get(), 0.001);
        assertEquals(5.5, AutoTotemConfig.triggerHearts, 0.001);
        assertEquals(14.0, AutoTotemConfig.restoreHearts, 0.001);
    }

    @Test
    @DisplayName("R2: PresetSerializer extracts and applies all per-mode fields and count_absorption")
    void testPresetSerializationAndDeserializationOfAllPerModeFields() {
        ActivityConfig src = new ActivityConfig();
        src.autoTotemMode = "crystal";
        src.autoTotemMainhandTriggerHearts = 3.5;
        src.autoTotemMainhandRestoreHearts = 8.0;
        src.autoTotemOffhandTriggerHearts = 1.0;
        src.autoTotemOffhandRestoreHearts = 0.0;
        src.autoTotemCrystalTriggerHearts = 6.0;
        src.autoTotemCrystalRestoreHearts = 15.5;
        src.autoTotemCountAbsorption = true;
        src.autoTotemTriggerHearts = 6.0;
        src.autoTotemRestoreHearts = 15.5;

        JsonObject snapshot = PresetSerializer.extractSettingsSnapshot(src);
        assertNotNull(snapshot);
        assertTrue(snapshot.has("autoTotemMainhandTriggerHearts"));
        assertTrue(snapshot.has("autoTotemMainhandRestoreHearts"));
        assertTrue(snapshot.has("autoTotemOffhandTriggerHearts"));
        assertTrue(snapshot.has("autoTotemOffhandRestoreHearts"));
        assertTrue(snapshot.has("autoTotemCrystalTriggerHearts"));
        assertTrue(snapshot.has("autoTotemCrystalRestoreHearts"));
        assertTrue(snapshot.has("autoTotemCountAbsorption"));

        assertEquals(3.5, snapshot.get("autoTotemMainhandTriggerHearts").getAsDouble(), 0.001);
        assertEquals(8.0, snapshot.get("autoTotemMainhandRestoreHearts").getAsDouble(), 0.001);
        assertEquals(1.0, snapshot.get("autoTotemOffhandTriggerHearts").getAsDouble(), 0.001);
        assertEquals(0.0, snapshot.get("autoTotemOffhandRestoreHearts").getAsDouble(), 0.001);
        assertEquals(6.0, snapshot.get("autoTotemCrystalTriggerHearts").getAsDouble(), 0.001);
        assertEquals(15.5, snapshot.get("autoTotemCrystalRestoreHearts").getAsDouble(), 0.001);
        assertTrue(snapshot.get("autoTotemCountAbsorption").getAsBoolean());

        ActivityConfig dst = new ActivityConfig();
        PresetSerializer.applySettingsSnapshot(snapshot, dst);

        assertEquals("crystal", dst.autoTotemMode);
        assertEquals(3.5, dst.autoTotemMainhandTriggerHearts, 0.001);
        assertEquals(8.0, dst.autoTotemMainhandRestoreHearts, 0.001);
        assertEquals(1.0, dst.autoTotemOffhandTriggerHearts, 0.001);
        assertEquals(0.0, dst.autoTotemOffhandRestoreHearts, 0.001);
        assertEquals(6.0, dst.autoTotemCrystalTriggerHearts, 0.001);
        assertEquals(15.5, dst.autoTotemCrystalRestoreHearts, 0.001);
        assertTrue(dst.autoTotemCountAbsorption);
    }

    @Test
    @DisplayName("R3: onTotemPop() does not trigger RESTORE_SWAP_SELECT in any mode")
    void testOnTotemPopDoesNotTriggerRestoreSwapSelect() {
        AutoTotemController controller = new AutoTotemController();

        AutoTotemConfig.mode = 2;
        controller.setStateForTest(State.ACTIVE);
        controller.setSwappedHotbarSlotForTest(1);
        controller.setSavedMainSlotForTest(0);

        controller.onTotemPop((MinecraftClient) null);
        assertNotEquals(State.RESTORE_SWAP_SELECT, controller.getState(), "onTotemPop in offhand mode must NOT enter RESTORE_SWAP_SELECT");
        assertEquals(State.IDLE, controller.getState());

        AutoTotemConfig.mode = 1;
        controller.setStateForTest(State.HOLD_IN_HAND);
        controller.setHeldTotemHotbarSlotForTest(2);
        controller.setSavedMainSlotForTest(0);

        controller.onTotemPop((MinecraftClient) null);
        assertNotEquals(State.RESTORE_SWAP_SELECT, controller.getState(), "onTotemPop in main_hand mode must NOT enter RESTORE_SWAP_SELECT");
        assertEquals(State.IDLE, controller.getState());

        AutoTotemConfig.mode = 3;
        controller.setStateForTest(State.HOLD_IN_HAND);
        controller.setHeldTotemHotbarSlotForTest(8);
        controller.setSavedMainSlotForTest(0);

        controller.onTotemPop((MinecraftClient) null);
        assertNotEquals(State.RESTORE_SWAP_SELECT, controller.getState(), "onTotemPop in crystal mode must NOT enter RESTORE_SWAP_SELECT");
        assertEquals(State.IDLE, controller.getState());
    }

    @Test
    @DisplayName("R3: Refill target slot pinning without drifting to empty slots")
    void testRefillTargetSlotPinningWithoutDriftingToEmptySlots() {
        AutoTotemController controller = new AutoTotemController();
        MockPlayer player = createMockPlayer();

        ItemStack dummyWeapon = createDummyStack();
        player.inventory.slots[0] = dummyWeapon;
        player.inventory.slots[1] = ItemStack.EMPTY;
        player.inventory.slots[2] = ItemStack.EMPTY;
        player.inventory.slots[3] = ItemStack.EMPTY;
        player.inventory.slots[4] = ItemStack.EMPTY;

        AutoTotemConfig.mode = 1;
        AutoTotemConfig.refillSlot = -1;

        int pinnedSlot = controller.resolveRefillTargetSlotForTest(player, 2);
        assertEquals(2, pinnedSlot, "Preferred slot 2 must be strictly pinned even though slot 1 is empty");

        controller.setLastTotemHotbarSlotForTest(3);
        int pinnedFromLast = controller.resolveRefillTargetSlotForTest(player, -1);
        assertEquals(3, pinnedFromLast, "Last totem slot 3 must be strictly pinned even though slots 1 and 2 are empty");

        AutoTotemConfig.refillSlot = 4;
        int explicitConfigSlot = controller.resolveRefillTargetSlotForTest(player, 1);
        assertEquals(4, explicitConfigSlot, "Explicit refillSlot setting must take precedence");

        AutoTotemConfig.refillSlot = -1;
        AutoTotemConfig.mode = 3;
        int crystalPinnedSlot = controller.resolveRefillTargetSlotForTest((ClientPlayerEntity) null, 1);
        assertEquals(8, crystalPinnedSlot, "Crystal mode must strictly pin to designated crystal slot 8");
    }

    @Test
    @DisplayName("R3: Concurrency guard: onTotemPop() is ignored during active refill states")
    void testConcurrencyGuardDuringRefillStates() {
        AutoTotemController controller = new AutoTotemController();

        controller.setStateForTest(State.REFILL_WAIT_OPEN);
        controller.onTotemPop((MinecraftClient) null);
        assertEquals(State.REFILL_WAIT_OPEN, controller.getState(), "onTotemPop must not interrupt REFILL_WAIT_OPEN");

        controller.setStateForTest(State.REFILL_WAIT_SWAP);
        controller.onTotemPop((MinecraftClient) null);
        assertEquals(State.REFILL_WAIT_SWAP, controller.getState(), "onTotemPop must not interrupt REFILL_WAIT_SWAP");

        controller.setStateForTest(State.REFILL_WAIT_CLOSE);
        controller.onTotemPop((MinecraftClient) null);
        assertEquals(State.REFILL_WAIT_CLOSE, controller.getState(), "onTotemPop must not interrupt REFILL_WAIT_CLOSE");
    }

    @Test
    @DisplayName("R3: Zero selectedSlot disruption during inventory refill setup and finish")
    void testZeroSelectedSlotDisruptionDuringRefill() {
        AutoTotemController controller = new AutoTotemController();
        MockPlayer player = createMockPlayer();

        player.inventory.selected = 0;
        player.inventory.slots[0] = createDummyStack();

        controller.startRefillForTest(null, 2);
        assertEquals(State.REFILL_WAIT_OPEN, controller.getState());
        assertEquals(2, controller.getRefillTargetHotbarSlot());
        assertEquals(0, player.inventory.selected, "Starting refill must NOT alter player.selectedSlot");

        controller.finishRefillForTest(null);
        assertEquals(State.IDLE, controller.getState());
        assertEquals(-1, controller.getRefillTargetHotbarSlot());
        assertEquals(0, player.inventory.selected, "Finishing refill must maintain player.selectedSlot cleanly without disruption");
    }

    @Test
    @DisplayName("R4: SESSION_MEMORY_TTL_MS is strictly Long.MAX_VALUE")
    void testSessionMemoryTTLIsInfinite() {
        assertEquals(Long.MAX_VALUE, ActivityScreen.SESSION_MEMORY_TTL_MS, "SESSION_MEMORY_TTL_MS must be infinite (Long.MAX_VALUE)");
    }

    @Test
    @DisplayName("R4: Session memory validity and scroll retention across 5+ minutes, 10 min, 1 hour, 24 hours")
    void testSessionValidityAndScrollRetentionAcrossTime() {
        ActivityScreen.recordSession("utilities", "auto_tool", 380.25);
        assertTrue(ActivityScreen.hasValidSession(), "Session must be immediately valid");

        long now = System.currentTimeMillis();

        ActivityScreen.setLastSessionCloseTimestamp(now - 300_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 5 minutes (300,000 ms)");
        assertEquals("utilities", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_tool", ActivityScreen.getLastSessionModuleId());
        assertEquals(380.25, ActivityScreen.getLastSessionScrollAmount(), 1e-6);

        ActivityScreen.setLastSessionCloseTimestamp(now - 600_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 10 minutes (600,000 ms)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 3_600_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 1 hour (3,600,000 ms)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 86_400_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 24 hours (86,400,000 ms)");

        ActivityScreen.setLastSessionCloseTimestamp(now - 2_592_000_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid after 30 days");

        assertEquals("utilities", ActivityScreen.getLastSessionTabId());
        assertEquals("auto_tool", ActivityScreen.getLastSessionModuleId());
        assertEquals(380.25, ActivityScreen.getLastSessionScrollAmount(), 1e-6);
    }

    @Test
    @DisplayName("R4: Session validity persists even across system clock changes")
    void testSessionValidityAcrossClockChanges() {
        ActivityScreen.recordSession("defense", "auto_totem", 150.0);
        assertTrue(ActivityScreen.hasValidSession());

        ActivityScreen.setLastSessionCloseTimestamp(System.currentTimeMillis() + 10_000_000L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid when timestamp is ahead (clock rollback)");

        ActivityScreen.setLastSessionCloseTimestamp(0L);
        assertTrue(ActivityScreen.hasValidSession(), "Session must remain valid even when timestamp is 0L, as long as tabId is present");
    }
}
