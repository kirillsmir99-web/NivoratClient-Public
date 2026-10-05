package activity.client.integration;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ModMenuStealth {
    private ModMenuStealth() {}

    public static void init() {
        hideClientSpoofer();
        suppressSpooferToast();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            hideClientSpoofer();
            suppressSpooferToast();
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen != null && screen.getClass().getName().contains("modmenu")) {
                hideClientSpoofer();
            }
        });
    }

    public static void hideClientSpoofer() {
        try {
            Class<?> modMenuClass = Class.forName("com.terraformersmc.modmenu.ModMenu");

            Field modsField = modMenuClass.getField("MODS");
            Object modsObj = modsField.get(null);
            if (modsObj instanceof Map<?, ?> mods) {
                mods.remove("clientspoofer");
            }

            Field rootModsField = modMenuClass.getField("ROOT_MODS");
            Object rootModsObj = rootModsField.get(null);
            if (rootModsObj instanceof Map<?, ?> rootMods) {
                rootMods.remove("clientspoofer");
            }

            Field parentMapField = modMenuClass.getField("PARENT_MAP");
            Object parentMapObj = parentMapField.get(null);
            if (parentMapObj instanceof com.google.common.collect.Multimap<?, ?> multimap) {
                multimap.entries().removeIf(entry -> isSpoofer(entry.getKey()) || isSpoofer(entry.getValue()));
            }

            Class<?> configClass = Class.forName("com.terraformersmc.modmenu.config.ModMenuConfig");
            Field hiddenModsField = configClass.getField("HIDDEN_MODS");
            Object hiddenModsOption = hiddenModsField.get(null);
            if (hiddenModsOption != null) {
                Method getValue = hiddenModsOption.getClass().getMethod("getValue");
                Object val = getValue.invoke(hiddenModsOption);
                if (val instanceof Set set) {
                    set.add("clientspoofer");
                }
            }

            Method clearCache = modMenuClass.getMethod("clearModCountCache");
            clearCache.invoke(null);
        } catch (Throwable ignored) {}
    }

    private static boolean isSpoofer(Object modObj) {
        if (modObj == null) return false;
        try {
            Method getId = modObj.getClass().getMethod("getId");
            Object id = getId.invoke(modObj);
            return "clientspoofer".equals(id);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void suppressSpooferToast() {
        try {
            Class<?> toastUtils = Class.forName("de.fabiexe.clientspoofer.util.ToastUtils");
            Field field = toastUtils.getDeclaredField("serversAttemptedReadingMods");
            field.setAccessible(true);
            field.set(null, new AbstractSet<String>() {
                @Override
                public boolean contains(Object o) {
                    return true;
                }

                @Override
                public boolean add(String s) {
                    return true;
                }

                @Override
                public Iterator<String> iterator() {
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 1;
                }
            });
        } catch (Throwable ignored) {}
    }
}
