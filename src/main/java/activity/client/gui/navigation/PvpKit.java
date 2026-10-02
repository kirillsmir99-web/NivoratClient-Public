package activity.client.gui.navigation;

import activity.client.module.api.IModule;
import java.util.Locale;

public enum PvpKit {
    ALL("Все", "All", ""),
    NETHERITE_POT("НПОТ", "Netherite Pot", "auto_totem hp_reaper auto_gg cooldown_hud"),
    CRYSTAL("КПВП", "Crystal", "click_pearl auto_totem auto_anchor hp_reaper cooldown_hud auto_gg"),
    UHC("УХК", "UHC", "auto_shieldbreaker auto_gg cooldown_hud auto_tool"),
    SMP("СМП", "SMP", "auto_shieldbreaker click_pearl auto_totem hp_reaper auto_tool auto_gg cooldown_hud"),
    MACE("МЕЙСЫ", "Mace", "auto_mace auto_spear auto_stun_slam auto_pearl_catch click_pearl auto_totem hp_reaper auto_gg cooldown_hud"),
    BEAST("БИСТЫ", "Beast", "auto_gg hp_reaper"),
    SWORD("OP", "OP", "click_pearl hp_reaper cooldown_hud auto_gg"),
    AXE("ТОПОРЫ", "Axe", "auto_shieldbreaker auto_tool auto_gg hp_reaper cooldown_hud"),
    DIAMOND_POT("ДПОТ", "Diamond Pot", "hp_reaper auto_gg cooldown_hud");

    public static final int FIRST_TAB_INDEX = 6;
    private final String russianName;
    private final String englishName;
    private final String[] keywords;
    PvpKit(String russianName, String englishName, String keywords) {
        this.russianName = russianName;
        this.englishName = englishName;
        this.keywords = keywords.split(" ");
    }
    public String id() { return "kit_" + name().toLowerCase(Locale.ROOT); }
    public String russianName() { return russianName; }
    public String englishName() { return englishName; }
    public int tabIndex() { return FIRST_TAB_INDEX + ordinal(); }
    public boolean matches(IModule module) { return module != null && matchesId(module.getId()); }
    public boolean matchesId(String id) {
        if (id == null) return false;
        if (this == ALL) return true;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (String key : keywords) if (normalized.equals(key)) return true;
        return false;
    }
}
