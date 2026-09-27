package dev.hpreaper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class HealthHudOverlay {
    private static final int COLOR_ABSORPTION = 0xFFFFF014;
    private static final int COLOR_NORMAL = 0xFFFF2A2A;
    private static final int COLOR_TARGET = 0xFFFF7A18;
    private static final int COLOR_MARKER = 0xFF9B9B9B;
    private static final int COLOR_DIFFERENCE_AHEAD = 0xFF55FF55;
    private static final int COLOR_DIFFERENCE_BEHIND = 0xFFFF5555;
    private static final long TARGET_DISPLAY_DURATION_NANOS = 20_000_000_000L;
    private static final long CROSSHAIR_RETENTION_NANOS = 800_000_000L;
    private static final double RAYCAST_MAX_DISTANCE = 24.0;
    private static final int HUD_Y_OFFSET = 52;
    private static final String SEPARATOR = " • ";

    private static final String[] CACHED_HP = new String[1001];
    private static final String[] CACHED_DIFF_POS = new String[1001];
    private static final String[] CACHED_DIFF_NEG = new String[1001];

    static {
        for (int i = 0; i <= 1000; i++) {
            int whole = i / 10;
            int frac = i % 10;
            CACHED_HP[i] = whole + "." + frac;
            CACHED_DIFF_POS[i] = "+" + whole + "." + frac;
            CACHED_DIFF_NEG[i] = "-" + whole + "." + frac;
        }
    }

    private static float displayHp = -1.0F;
    private static float displayTargetHp = -1.0F;
    private static long lastTimeNanos = 0L;
    private static LivingEntity lastCombatTarget = null;
    private static long lastCombatHitNanos = 0L;
    private static LivingEntity cachedCrosshairTarget = null;
    private static long lastCrosshairSeenNanos = 0L;
    private static LivingEntity lastActiveTarget = null;

    private HealthHudOverlay() {}

    public static boolean isValidTarget(LivingEntity player, LivingEntity entity) {
        if (player == null
                || entity == null
                || entity == player
                || (entity instanceof ArmorStandEntity)
                || !entity.isAlive()
                || entity.isRemoved()
                || entity.isSpectator()) {
            return false;
        }
        VitalityConfig.TargetFilter filter = VitalityConfig.targetFilter;
        if (filter == VitalityConfig.TargetFilter.PLAYERS_ONLY) {
            return entity instanceof net.minecraft.entity.player.PlayerEntity;
        }
        if (filter == VitalityConfig.TargetFilter.HOSTILE_AND_PLAYERS) {
            return entity instanceof net.minecraft.entity.player.PlayerEntity || entity instanceof net.minecraft.entity.mob.Monster;
        }
        return true;
    }

    public static float extractEntityHealth(LivingEntity entity) {
        if (entity == null) {
            return 0.0F;
        }
        float vanillaHealth = 0.0F;
        float maxHealth = 20.0F;
        float absorption = 0.0F;
        try {
            vanillaHealth = entity.getHealth();
            maxHealth = entity.getMaxHealth();
            absorption = entity.getAbsorptionAmount();
        } catch (Throwable ignored) {}

        try {
            if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.world != null) {
                    net.minecraft.scoreboard.Scoreboard scoreboard = client.world.getScoreboard();
                    if (scoreboard != null) {
                        try {
                            net.minecraft.scoreboard.ScoreboardObjective belowName = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME);
                            if (belowName != null) {
                                net.minecraft.scoreboard.ReadableScoreboardScore score = scoreboard.getScore(player, belowName);
                                if (score != null && score.getScore() > 0) {
                                    return (float) score.getScore();
                                }
                            }
                        } catch (Throwable ignored) {}

                        try {
                            net.minecraft.scoreboard.ScoreboardObjective listObj = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.LIST);
                            if (listObj != null) {
                                net.minecraft.scoreboard.ReadableScoreboardScore score = scoreboard.getScore(player, listObj);
                                if (score != null && score.getScore() > 0) {
                                    return (float) score.getScore();
                                }
                            }
                        } catch (Throwable ignored) {}

                        String holderName = null;
                        try {
                            holderName = player.getNameForScoreboard();
                        } catch (Throwable ignored) {}
                        if (holderName == null) {
                            try {
                                holderName = player.getName().getString();
                            } catch (Throwable ignored) {}
                        }
                        if (holderName != null && !holderName.isBlank()) {
                            try {
                                net.minecraft.scoreboard.ScoreHolder holder = net.minecraft.scoreboard.ScoreHolder.fromName(holderName);
                                net.minecraft.scoreboard.ScoreboardObjective belowName = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME);
                                if (belowName != null) {
                                    net.minecraft.scoreboard.ReadableScoreboardScore score = scoreboard.getScore(holder, belowName);
                                    if (score != null && score.getScore() > 0) {
                                        return (float) score.getScore();
                                    }
                                }
                                net.minecraft.scoreboard.ScoreboardObjective listObj = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.LIST);
                                if (listObj != null) {
                                    net.minecraft.scoreboard.ReadableScoreboardScore score = scoreboard.getScore(holder, listObj);
                                    if (score != null && score.getScore() > 0) {
                                        return (float) score.getScore();
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (Float.isNaN(vanillaHealth) || Float.isInfinite(vanillaHealth)) vanillaHealth = 0.0F;
        if (Float.isNaN(maxHealth) || Float.isInfinite(maxHealth) || maxHealth <= 0.0F) maxHealth = 20.0F;
        if (Float.isNaN(absorption) || Float.isInfinite(absorption) || absorption < 0.0F) absorption = 0.0F;

        float safeHp = Math.max(0.0F, Math.min(maxHealth, vanillaHealth));
        if (Math.abs(safeHp - maxHealth) < 0.05F) {
            safeHp = maxHealth;
        }
        return safeHp + Math.max(0.0F, absorption);
    }

    public static LivingEntity resolveLivingEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof EnderDragonPart part) {
            return part.owner;
        }
        return null;
    }

    public static void updateTick(MinecraftClient client) {
        try {
            if (client == null || client.world == null || client.player == null) {
                cachedCrosshairTarget = null;
                return;
            }

            LivingEntity found = null;
            LivingEntity crosshairLiving = activity.client.module.service.TargetCacheService.getCrosshairLivingTarget(client);
            if (crosshairLiving != null && isValidTarget(client.player, crosshairLiving)) {
                found = crosshairLiving;
            } else if (client.crosshairTarget instanceof EntityHitResult ehr) {
                LivingEntity resolved = resolveLivingEntity(ehr.getEntity());
                if (isValidTarget(client.player, resolved)) {
                    found = resolved;
                }
            }

            if (found == null) {
                Entity camera = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
                if (camera != null) {
                    Vec3d cameraPos = camera.getCameraPosVec(1.0F);
                    Vec3d rot = camera.getRotationVec(1.0F);
                    Vec3d end = cameraPos.add(rot.x * RAYCAST_MAX_DISTANCE, rot.y * RAYCAST_MAX_DISTANCE, rot.z * RAYCAST_MAX_DISTANCE);
                    HitResult blockHit = camera.raycast(RAYCAST_MAX_DISTANCE, 1.0F, false);
                    double maxDistSq = RAYCAST_MAX_DISTANCE * RAYCAST_MAX_DISTANCE;
                    if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                        maxDistSq = blockHit.getPos().squaredDistanceTo(cameraPos);
                        end = blockHit.getPos();
                    }
                    Box bb = camera.getBoundingBox();
                    if (bb != null) {
                        Box box = bb.stretch(rot.multiply(RAYCAST_MAX_DISTANCE)).expand(1.0);
                        EntityHitResult hit = ProjectileUtil.raycast(camera, cameraPos, end, box, e -> {
                            LivingEntity living = resolveLivingEntity(e);
                            return isValidTarget(client.player, living);
                        }, maxDistSq);

                        if (hit != null) {
                            found = resolveLivingEntity(hit.getEntity());
                        }
                    }
                }
            }

            long now = System.nanoTime();
            if (found != null) {
                cachedCrosshairTarget = found;
                lastCrosshairSeenNanos = now;
            } else if (cachedCrosshairTarget != null) {
                if (now - lastCrosshairSeenNanos > CROSSHAIR_RETENTION_NANOS || !isValidTarget(client.player, cachedCrosshairTarget)) {
                    cachedCrosshairTarget = null;
                }
            }
        } catch (Throwable ignored) {
            cachedCrosshairTarget = null;
        }
    }

    public static void trackCombatTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        if (lastCombatTarget != target) {
            displayTargetHp = -1.0F;
        }
        lastCombatTarget = target;
        lastCombatHitNanos = System.nanoTime();
    }

    public static void trackTarget(PlayerEntity target) {
        trackCombatTarget(target);
    }

    public static void trackTarget(LivingEntity target) {
        trackCombatTarget(target);
    }

    public static void trackTarget(Entity target) {
        if (target instanceof LivingEntity living) {
            trackCombatTarget(living);
        }
    }

    public static LivingEntity getActiveTarget(ClientPlayerEntity player, long now) {
        if (isValidTarget(player, cachedCrosshairTarget)) {
            return cachedCrosshairTarget;
        }
        if (isValidTarget(player, lastCombatTarget) && now - lastCombatHitNanos <= TARGET_DISPLAY_DURATION_NANOS) {
            return lastCombatTarget;
        }
        return null;
    }

    public static boolean isEnabled() {
        return VitalityConfig.displayMode != DisplayMode.DISABLED;
    }

    public static void toggle() {
        if (VitalityConfig.displayMode == DisplayMode.DISABLED) {
            VitalityConfig.displayMode = DisplayMode.OWN_HEALTH;
        } else {
            VitalityConfig.displayMode = DisplayMode.DISABLED;
        }
    }

    public static DisplayMode getDisplayMode() {
        return VitalityConfig.displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) {
        if (mode != null) {
            VitalityConfig.displayMode = mode;
        }
    }

    public static DisplayMode cycleDisplayMode() {
        VitalityConfig.displayMode = switch (VitalityConfig.displayMode) {
            case OWN_HEALTH -> DisplayMode.CROSSHAIR_AND_TARGET;
            case CROSSHAIR_AND_TARGET -> DisplayMode.TARGET_HEALTH;
            case TARGET_HEALTH -> DisplayMode.OWN_TARGET_AND_DIFFERENCE;
            case OWN_TARGET_AND_DIFFERENCE -> DisplayMode.DISABLED;
            case DISABLED -> DisplayMode.OWN_HEALTH;
        };
        return VitalityConfig.displayMode;
    }

    public static int getDefaultX(int screenWidth, int elementWidth) {
        return screenWidth / 2 + 82 - elementWidth;
    }

    public static int getDefaultY(int screenHeight, int elementHeight) {
        return screenHeight - HUD_Y_OFFSET;
    }

    public static int getDefaultY(int screenHeight) {
        return screenHeight - HUD_Y_OFFSET;
    }

    public static int getEffectiveX(DisplayMode mode, int screenWidth, int elementWidth) {
        int customX = VitalityConfig.getModeX(mode);
        if (customX >= 0) {
            return Math.max(2, Math.min(screenWidth - elementWidth - 2, customX));
        }
        return getDefaultX(screenWidth, elementWidth);
    }

    public static int getEffectiveY(DisplayMode mode, int screenHeight, int elementHeight) {
        int customY = VitalityConfig.getModeY(mode);
        if (customY >= 0) {
            return Math.max(4, Math.min(screenHeight - elementHeight - 2, customY));
        }
        return getDefaultY(screenHeight);
    }

    public static int getEffectiveX(int screenWidth, int elementWidth) {
        return getEffectiveX(VitalityConfig.displayMode, screenWidth, elementWidth);
    }

    public static int getEffectiveY(int screenHeight, int elementHeight) {
        return getEffectiveY(VitalityConfig.displayMode, screenHeight, elementHeight);
    }

    public static int getPreviewWidth(TextRenderer textRenderer, DisplayMode mode) {
        if (textRenderer == null) {
            return 24;
        }
        return switch (mode) {
            case OWN_HEALTH -> textRenderer.getWidth("20.0");
            case CROSSHAIR_AND_TARGET, TARGET_HEALTH -> textRenderer.getWidth("18.5");
            case OWN_TARGET_AND_DIFFERENCE, DISABLED ->
                textRenderer.getWidth("20.0") + textRenderer.getWidth(SEPARATOR) * 2 + textRenderer.getWidth("18.5") + textRenderer.getWidth("+1.5");
        };
    }

    public static int getPreviewHeight(DisplayMode mode) {
        return 10;
    }

    public static void renderPreview(DrawContext context, MinecraftClient client, int x, int y, DisplayMode mode) {
        if (context == null || client == null || client.textRenderer == null) {
            return;
        }
        renderElement(context, client, x, y, "20.0", COLOR_NORMAL, "18.5", COLOR_TARGET, "+1.5", COLOR_DIFFERENCE_AHEAD, mode);
    }

    public static void renderElement(DrawContext context, MinecraftClient client, int x, int y,
                                     String ownText, int ownColor,
                                     String targetText, int targetColor,
                                     String diffText, int diffColor,
                                     DisplayMode mode) {
        TextRenderer tr = client.textRenderer;
        if (tr == null) return;

        switch (mode) {
            case OWN_HEALTH -> context.drawTextWithShadow(tr, ownText, x, y, ownColor);
            case CROSSHAIR_AND_TARGET -> {
                if (targetText != null) {
                    context.drawTextWithShadow(tr, targetText, x, y, targetColor);
                } else {
                    context.drawTextWithShadow(tr, ownText, x, y, ownColor);
                }
            }
            case TARGET_HEALTH -> {
                if (targetText != null) {
                    context.drawTextWithShadow(tr, targetText, x, y, targetColor);
                }
            }
            case OWN_TARGET_AND_DIFFERENCE -> {
                if (targetText != null && diffText != null) {
                    int curX = x;
                    context.drawTextWithShadow(tr, ownText, curX, y, ownColor);
                    curX += tr.getWidth(ownText);
                    context.drawTextWithShadow(tr, SEPARATOR, curX, y, COLOR_MARKER);
                    curX += tr.getWidth(SEPARATOR);
                    context.drawTextWithShadow(tr, targetText, curX, y, targetColor);
                    curX += tr.getWidth(targetText);
                    context.drawTextWithShadow(tr, SEPARATOR, curX, y, COLOR_MARKER);
                    curX += tr.getWidth(SEPARATOR);
                    context.drawTextWithShadow(tr, diffText, curX, y, diffColor);
                } else {
                    context.drawTextWithShadow(tr, ownText, x, y, ownColor);
                }
            }
            case DISABLED -> {}
        }
    }

    public static void render(DrawContext context, float tickDelta) {
        render(context, (RenderTickCounter) null);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            if (context == null || VitalityConfig.displayMode == DisplayMode.DISABLED) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null || client.options.hudHidden || client.world == null) {
                return;
            }
            if (client.currentScreen instanceof HpHudEditorScreen || (client.currentScreen != null && client.currentScreen.getClass().getSimpleName().contains("EditorScreen"))) {
                return;
            }

            ClientPlayerEntity player = client.player;
            if (player == null || !player.isAlive() || player.isSpectator()) {
                displayHp = -1.0F;
                displayTargetHp = -1.0F;
                lastTimeNanos = 0L;
                return;
            }

            float maxHealth = player.getMaxHealth();
            float health = Math.max(0.0F, Math.min(maxHealth, player.getHealth()));
            float absorption = Math.max(0.0F, player.getAbsorptionAmount());
            if (Math.abs(health - maxHealth) < 0.05F) {
                health = maxHealth;
            }
            float targetOwnHp = health + absorption;

            long now = System.nanoTime();
            float animationFactor = 1.0F;
            if (displayHp < 0.0F || lastTimeNanos == 0L) {
                displayHp = targetOwnHp;
            } else {
                float dt = (now - lastTimeNanos) / 1_000_000_000.0F;
                if (dt > 0.1F) {
                    dt = 0.1F;
                }
                animationFactor = Math.min(1.0F, dt * 24.0F);
                displayHp = animateHp(displayHp, targetOwnHp, animationFactor);
            }
            lastTimeNanos = now;

            String ownText = formatHp(displayHp);
            int ownColor = absorption > 0.0F ? COLOR_ABSORPTION : COLOR_NORMAL;

            LivingEntity activeTarget = getActiveTarget(player, now);
            if (activeTarget != lastActiveTarget) {
                lastActiveTarget = activeTarget;
                displayTargetHp = -1.0F;
            }

            String targetText = null;
            String diffText = null;
            int diffColor = COLOR_DIFFERENCE_AHEAD;

            if (activeTarget != null) {
                try {
                    float actualTargetHp = extractEntityHealth(activeTarget);
                    if (displayTargetHp < 0.0F) {
                        displayTargetHp = actualTargetHp;
                    } else {
                        displayTargetHp = animateHp(displayTargetHp, actualTargetHp, animationFactor);
                    }
                    targetText = formatHp(displayTargetHp);
                    diffText = formatDifference(displayHp - displayTargetHp);
                    diffColor = displayHp >= displayTargetHp ? COLOR_DIFFERENCE_AHEAD : COLOR_DIFFERENCE_BEHIND;
                } catch (Throwable ignored) {
                    targetText = null;
                }
            } else {
                displayTargetHp = -1.0F;
            }

            if (VitalityConfig.displayMode == DisplayMode.TARGET_HEALTH && targetText == null) {
                return;
            }

            TextRenderer tr = client.textRenderer;
            int elementWidth = getPreviewWidth(tr, VitalityConfig.displayMode);
            int elementHeight = getPreviewHeight(VitalityConfig.displayMode);

            if (elementWidth <= 0 || VitalityConfig.displayMode == DisplayMode.DISABLED) {
                return;
            }

            int sw = context.getScaledWindowWidth();
            int sh = context.getScaledWindowHeight();
            int x = getEffectiveX(VitalityConfig.displayMode, sw, elementWidth);
            int y = getEffectiveY(VitalityConfig.displayMode, sh, elementHeight);

            try {
                renderElement(context, client, x, y, ownText, ownColor, targetText, COLOR_TARGET, diffText, diffColor, VitalityConfig.displayMode);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static float animateHp(float displayedHp, float actualHp, float factor) {
        if (Float.isNaN(displayedHp) || Float.isInfinite(displayedHp)) displayedHp = -1.0F;
        if (Float.isNaN(actualHp) || Float.isInfinite(actualHp)) actualHp = 20.0F;
        if (displayedHp < 0.0F || Math.abs(actualHp - displayedHp) < 0.05F) {
            return actualHp;
        }
        return displayedHp + (actualHp - displayedHp) * factor;
    }

    public static String formatHp(float hp) {
        if (Float.isNaN(hp) || Float.isInfinite(hp) || hp <= 0.0F) {
            return "0.0";
        }
        int index = Math.round(hp * 10.0F);
        if (index >= 0 && index <= 1000) {
            return CACHED_HP[index];
        }
        int whole = index / 10;
        int frac = index % 10;
        return whole + "." + frac;
    }

    public static String formatDifference(float difference) {
        if (Float.isNaN(difference) || Float.isInfinite(difference) || Math.abs(difference) < 0.05F) {
            return "0.0";
        }
        int index = Math.round(Math.abs(difference) * 10.0F);
        if (index <= 1000) {
            return difference > 0.0F ? CACHED_DIFF_POS[index] : CACHED_DIFF_NEG[index];
        }
        int whole = index / 10;
        int frac = index % 10;
        return (difference > 0.0F ? "+" : "-") + whole + "." + frac;
    }

    public enum DisplayMode {
        OWN_HEALTH("message.hpreaper.mode.own_health", Formatting.YELLOW, "§eСвоё HP"),
        CROSSHAIR_AND_TARGET("message.hpreaper.mode.crosshair_and_target", Formatting.AQUA, "§bВезде"),
        TARGET_HEALTH("message.hpreaper.mode.target_health", Formatting.GOLD, "§6Только цель"),
        OWN_TARGET_AND_DIFFERENCE("message.hpreaper.mode.own_target_and_difference", Formatting.LIGHT_PURPLE, "§dСвоё+Цель+Разница"),
        DISABLED("message.hpreaper.mode.disabled", Formatting.RED, "§cВыкл");

        private final String translationKey;
        private final Formatting messageColor;
        private final String shortName;

        DisplayMode(String translationKey, Formatting messageColor, String shortName) {
            this.translationKey = translationKey;
            this.messageColor = messageColor;
            this.shortName = shortName;
        }

        public String translationKey() {
            return translationKey;
        }

        public Formatting messageColor() {
            return messageColor;
        }

        public String getShortName() {
            return shortName;
        }
    }
}
