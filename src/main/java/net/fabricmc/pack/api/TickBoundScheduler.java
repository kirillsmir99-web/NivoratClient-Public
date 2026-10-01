package net.fabricmc.pack.api;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TickBoundScheduler {
    private static final List<ScheduledTask> TASKS = new CopyOnWriteArrayList<>();
    private static volatile boolean dispatcherManaged = false;
    private static boolean standaloneRegistered = false;
    private static long tickCounter = 0L;

    private TickBoundScheduler() {}

    public static synchronized void init() {
        if (dispatcherManaged || standaloneRegistered) return;
        standaloneRegistered = true;
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!dispatcherManaged) {
                onTick(client);
            }
        });
    }

    public static synchronized void markDispatcherManaged() {
        dispatcherManaged = true;
    }

    public static long getTickCount() {
        return tickCounter;
    }

    public static void runAfterTicks(int delayTicks, Runnable action) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            return;
        }
        init();
        if (delayTicks <= 0) {
            action.run();
            return;
        }
        TASKS.add(new ScheduledTask(delayTicks, action));
    }

    public static void runAfterMs(long delayMs, Runnable action) {
        int ticks = GaussianTimingEngine.toActionTicks(delayMs);
        runAfterTicks(ticks, action);
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void onTick(MinecraftClient client) {
        if (activity.client.capitulation.CapitulationManager.isCapitulated()) {
            TASKS.clear();
            return;
        }
        tickCounter++;
        if (TASKS.isEmpty()) return;
        Iterator<ScheduledTask> it = TASKS.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                TASKS.remove(task);
                try {
                    task.action.run();
                } catch (Throwable error) {
                    activity.client.module.api.ModuleDiagnostics.report("scheduler", "action", error);
                }
            }
        }
    }

    private static final class ScheduledTask {
        private int remainingTicks;
        private final Runnable action;

        private ScheduledTask(int remainingTicks, Runnable action) {
            this.remainingTicks = remainingTicks;
            this.action = action;
        }
    }
}
