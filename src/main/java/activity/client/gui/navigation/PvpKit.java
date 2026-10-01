package activity.client.gui.navigation;

import activity.client.module.api.IModule;
import java.util.Locale;

public enum PvpKit {
    ALL("Все", "All", ""),
    NETHERITE_POT("НПОТ", "Netherite Pot", "totem pearl reaper shield tool gg cooldown"),
    CRYSTAL("КПВП", "Crystal", "anchor cart totem pearl reaper gg cooldown"),
    UHC("УХК", "UHC", "shield tool totem reaper gg cooldown"),
    SMP("СМП", "SMP", "mace spear stun pearl totem cart anchor shield tool reaper gg cooldown water"),
    MACE("МЕЙСЫ", "Mace", "mace stun spear pearl totem shield reaper gg cooldown water"),
    BEAST("БИСТЫ", "Beast", "reaper gg tool"),
    SWORD("OP", "OP", "tool reaper totem gg cooldown"),
    AXE("ТОПОРЫ", "Axe", "shield tool totem reaper gg cooldown"),
    DIAMOND_POT("ДПОТ", "Diamond Pot", "pearl totem reaper tool gg cooldown");

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
        if (normalized.equals("click_pearl")) return true;
        for (String key : keywords) if (normalized.contains(key)) return true;
        return false;
    }
}
