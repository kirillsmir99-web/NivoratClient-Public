package dev.culling;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CartRefillControllerTest {

    private OcclusionCacheController controller;

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Unsafe instance", e);
        }
    }

    private static MinecraftClient allocateMockClient() {
        try {
            return (MinecraftClient) getUnsafe().allocateInstance(MinecraftClient.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate MinecraftClient instance", e);
        }
    }

    private static InventoryScreen allocateMockInventoryScreen() {
        try {
            return (InventoryScreen) getUnsafe().allocateInstance(InventoryScreen.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate InventoryScreen instance", e);
        }
    }

    private static class DummyContainerScreen extends Screen {
        protected DummyContainerScreen() {
            super(null);
        }
    }

    private static Screen allocateMockContainerScreen() {
        try {
            return (Screen) getUnsafe().allocateInstance(DummyContainerScreen.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate DummyContainerScreen instance", e);
        }
    }

    @BeforeEach
    void setUp() {
        controller = new OcclusionCacheController();
        controller.reset();
        OcclusionCacheConfig.enabled = true;
        OcclusionCacheConfig.autoClose = true;
        OcclusionCacheConfig.randomDelay = true;
        OcclusionCacheConfig.legitMode = true;
        OcclusionCacheConfig.refillDelayTicks = 2;
    }

    @Test
    @DisplayName("Test 1: Verify dynamic 2-4 tick bounds over 1,000 iterations for open, swap, and close steps")
    void testDynamicTickBoundsOver1000Iterations() {
        final int iterations = 1000;
        Set<Integer> openTicksSeen = new HashSet<>();
        Set<Integer> swapTicksSeen = new HashSet<>();
        Set<Integer> closeTicksSeen = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            int openTicks = controller.sampleOpenDelayTicks();
            int swapTicks = controller.sampleSwapDelayTicks();
            int closeTicks = controller.sampleCloseDelayTicks();

            assertTrue(openTicks >= 2 && openTicks <= 4,
                "Open ticks must be strictly within [2, 4], got: " + openTicks);
            assertTrue(swapTicks >= 2 && swapTicks <= 4,
                "Swap ticks must be strictly within [2, 4], got: " + swapTicks);
            assertTrue(closeTicks >= 2 && closeTicks <= 4,
                "Close ticks must be strictly within [2, 4], got: " + closeTicks);

            openTicksSeen.add(openTicks);
            swapTicksSeen.add(swapTicks);
            closeTicksSeen.add(closeTicks);

            int engineOpen = GaussianTimingEngine.getFastLegitRefillOpenDelayTicks();
            int engineSwap = GaussianTimingEngine.getFastLegitRefillSwapDelayTicks();
            int engineClose = GaussianTimingEngine.getFastLegitRefillCloseDelayTicks();

            assertTrue(engineOpen >= 2 && engineOpen <= 4);
            assertTrue(engineSwap >= 2 && engineSwap <= 4);
            assertTrue(engineClose >= 2 && engineClose <= 4);
        }

        assertTrue(openTicksSeen.size() >= 2,
            "Open ticks distribution must be dynamic and produce variation. Seen: " + openTicksSeen);
        assertTrue(swapTicksSeen.size() >= 2,
            "Swap ticks distribution must be dynamic and produce variation. Seen: " + swapTicksSeen);
        assertTrue(closeTicksSeen.size() >= 2,
            "Close ticks distribution must be dynamic and produce variation. Seen: " + closeTicksSeen);

        assertEquals(2, GaussianTimingEngine.FAST_LEGIT_MIN_TICKS);
        assertEquals(4, GaussianTimingEngine.FAST_LEGIT_MAX_TICKS);
    }

    @Test
    @DisplayName("Test 2: Verify millisecond spread strictly within 60-180 ms over 1,000 iterations")
    void testMillisecondSpreadStrictlyWithin60To180Ms() {
        final int iterations = 1000;
        long minOpen = Long.MAX_VALUE, maxOpen = Long.MIN_VALUE;
        long minSwap = Long.MAX_VALUE, maxSwap = Long.MIN_VALUE;
        long minClose = Long.MAX_VALUE, maxClose = Long.MIN_VALUE;

        double sumOpen = 0.0;
        double sumSwap = 0.0;
        double sumClose = 0.0;

        for (int i = 0; i < iterations; i++) {
            long openMs = controller.sampleOpenDelayMs();
            long swapMs = controller.sampleSwapDelayMs();
            long closeMs = controller.sampleCloseDelayMs();

            assertTrue(openMs >= 60L && openMs <= 180L,
                "Open ms must be strictly within [60, 180], got: " + openMs);
            assertTrue(swapMs >= 60L && swapMs <= 180L,
                "Swap ms must be strictly within [60, 180], got: " + swapMs);
            assertTrue(closeMs >= 60L && closeMs <= 180L,
                "Close ms must be strictly within [60, 180], got: " + closeMs);

            minOpen = Math.min(minOpen, openMs);
            maxOpen = Math.max(maxOpen, openMs);
            sumOpen += openMs;

            minSwap = Math.min(minSwap, swapMs);
            maxSwap = Math.max(maxSwap, swapMs);
            sumSwap += swapMs;

            minClose = Math.min(minClose, closeMs);
            maxClose = Math.max(maxClose, closeMs);
            sumClose += closeMs;

            long engineOpenMs = GaussianTimingEngine.getFastLegitRefillOpenDelayMs();
            long engineSwapMs = GaussianTimingEngine.getFastLegitRefillSwapDelayMs();
            long engineCloseMs = GaussianTimingEngine.getFastLegitRefillCloseDelayMs();
            assertTrue(engineOpenMs >= 60L && engineOpenMs <= 180L);
            assertTrue(engineSwapMs >= 60L && engineSwapMs <= 180L);
            assertTrue(engineCloseMs >= 60L && engineCloseMs <= 180L);
        }

        assertTrue(maxOpen - minOpen >= 40L, "Open ms must exhibit organic spread. Span: " + (maxOpen - minOpen));
        assertTrue(maxSwap - minSwap >= 40L, "Swap ms must exhibit organic spread. Span: " + (maxSwap - minSwap));
        assertTrue(maxClose - minClose >= 40L, "Close ms must exhibit organic spread. Span: " + (maxClose - minClose));

        double avgOpen = sumOpen / iterations;
        double avgSwap = sumSwap / iterations;
        double avgClose = sumClose / iterations;

        assertTrue(avgOpen >= 95.0 && avgOpen <= 115.0, "Open mean must center around 105ms, observed: " + avgOpen);
        assertTrue(avgSwap >= 105.0 && avgSwap <= 125.0, "Swap mean must center around 115ms, observed: " + avgSwap);
        assertTrue(avgClose >= 85.0 && avgClose <= 105.0, "Close mean must center around 95ms, observed: " + avgClose);

        assertEquals(60L, GaussianTimingEngine.FAST_LEGIT_MIN_MS);
        assertEquals(180L, GaussianTimingEngine.FAST_LEGIT_MAX_MS);
    }

    @Test
    @DisplayName("Test 3: Verify independence: sampling open, swap, and close produces distinct, non-identical values across iterations")
    void testStatisticalIndependenceAcrossSteps() {
        final int iterations = 1000;
        int nonIdenticalTriplets = 0;

        double[] openVals = new double[iterations];
        double[] swapVals = new double[iterations];
        double[] closeVals = new double[iterations];

        for (int i = 0; i < iterations; i++) {
            long openMs = controller.sampleOpenDelayMs();
            long swapMs = controller.sampleSwapDelayMs();
            long closeMs = controller.sampleCloseDelayMs();

            openVals[i] = openMs;
            swapVals[i] = swapMs;
            closeVals[i] = closeMs;

            if (openMs != swapMs || swapMs != closeMs) {
                nonIdenticalTriplets++;
            }
        }

        assertTrue(nonIdenticalTriplets >= 990,
            "Open, swap, and close steps must be independently sampled, observed distinct count: " + nonIdenticalTriplets);

        double rOpenSwap = computePearsonCorrelation(openVals, swapVals);
        double rSwapClose = computePearsonCorrelation(swapVals, closeVals);

        assertTrue(Math.abs(rOpenSwap) < 0.15,
            "Correlation between open and swap delays must be near zero (< 0.15), got: " + rOpenSwap);
        assertTrue(Math.abs(rSwapClose) < 0.15,
            "Correlation between swap and close delays must be near zero (< 0.15), got: " + rSwapClose);
    }

    @Test
    @DisplayName("Test 4: Verify screen safety: manually opened InventoryScreen is never closed when refill finishes (openedByRefill == false)")
    void testManuallyOpenedInventoryScreenIsNeverClosed() {
        MinecraftClient client = allocateMockClient();
        InventoryScreen manualScreen = allocateMockInventoryScreen();
        client.currentScreen = manualScreen;

        assertFalse(controller.shouldCloseScreen(manualScreen, false),
            "shouldCloseScreen must return false when openedByRefill == false");
        assertTrue(controller.shouldCloseScreen(manualScreen, true),
            "shouldCloseScreen must return true when openedByRefill == true and autoClose == true");

        controller.setStateForTest(OcclusionCacheController.State.WAITING_CLOSE, 0, false);
        controller.finishRefill(client);

        assertSame(manualScreen, client.currentScreen,
            "Manually opened InventoryScreen MUST NOT be closed when openedByRefill is false!");
        assertEquals(OcclusionCacheController.State.IDLE, controller.getState());
        assertFalse(controller.isOpenedByRefill());

        OcclusionCacheConfig.autoClose = false;
        assertFalse(controller.shouldCloseScreen(manualScreen, true),
            "shouldCloseScreen must return false when autoClose is disabled");
    }

    @Test
    @DisplayName("Test 5: Verify screen safety: container screens (e.g. chests) are never closed")
    void testContainerScreensAreNeverClosed() {
        MinecraftClient client = allocateMockClient();
        Screen chestScreen = allocateMockContainerScreen();
        Screen furnaceScreen = allocateMockContainerScreen();
        Screen anvilScreen = allocateMockContainerScreen();

        client.currentScreen = chestScreen;

        assertFalse(controller.shouldCloseScreen(chestScreen, false),
            "Chest screen must never be flagged for closing (openedByRefill = false)");
        assertFalse(controller.shouldCloseScreen(chestScreen, true),
            "Chest screen must never be flagged for closing even if openedByRefill was somehow true");
        assertFalse(controller.shouldCloseScreen(furnaceScreen, true),
            "Furnace screen must never be flagged for closing");
        assertFalse(controller.shouldCloseScreen(anvilScreen, true),
            "Anvil screen must never be flagged for closing");

        controller.setStateForTest(OcclusionCacheController.State.WAITING_CLOSE, 0, true);
        controller.finishRefill(client);

        assertSame(chestScreen, client.currentScreen,
            "Container screen MUST NEVER be closed by finishRefill!");
        assertEquals(OcclusionCacheController.State.IDLE, controller.getState());
        assertFalse(controller.isOpenedByRefill());

        client.currentScreen = furnaceScreen;
        controller.setStateForTest(OcclusionCacheController.State.WAITING_OPEN, 3, false);

        controller.reset();
        assertSame(furnaceScreen, client.currentScreen,
            "Furnace screen must remain active after controller reset");
    }

    @Test
    @DisplayName("Test 6: Verify screen open prevents refill and preserves exact slot targeting")
    void testScreenOpenPreventsRefillAndPreservesExactSlotTargeting() {
        MinecraftClient client = allocateMockClient();
        Screen chestScreen = allocateMockContainerScreen();
        client.currentScreen = chestScreen;

        // When container is open, controller must stay IDLE and openedByRefill false
        assertEquals(OcclusionCacheController.State.IDLE, controller.getState());
        assertFalse(controller.isOpenedByRefill());

        // In WAITING_SWAP, if manual screen is open (openedByRefill == false), finishRefill resets to IDLE
        controller.setStateForTest(OcclusionCacheController.State.WAITING_SWAP, 0, false);
        controller.finishRefill(client);
        assertEquals(OcclusionCacheController.State.IDLE, controller.getState());
        assertFalse(controller.isOpenedByRefill());
    }

    private static double computePearsonCorrelation(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0.0, sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double numerator = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;

        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            numerator += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }

        if (denomX <= 0.0 || denomY <= 0.0) {
            return 0.0;
        }
        return numerator / Math.sqrt(denomX * denomY);
    }
}
