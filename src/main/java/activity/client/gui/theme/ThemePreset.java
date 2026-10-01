package activity.client.gui.theme;

import java.util.Locale;

public enum ThemePreset {
    CLIENT("Client Red", "CORE", "DEEP", new int[]{0xD51F36, 0xFF5364, 0x8D1028}),
    NIVORA("Nivora", "CORE", "BALANCED", new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8}),
    OBSIDIAN("Obsidian", "CORE", "DEEP", new int[]{0x1A1C23, 0x2E3440, 0x434C5E, 0x3B4252}),
    GRAPHITE("Graphite", "CORE", "MONO", new int[]{0x2B2D42, 0x4A4E69, 0x6C757D, 0x8D99AE}),
    PEARL("Pearl", "CORE", "CRYSTAL", new int[]{0xE8ECEF, 0xC5D3E8, 0xA3B8CC, 0xDFE7F2}),
    AETHER("Aether", "CHROMA", "CRYSTAL", new int[]{0x4A90E2, 0x50E3C2, 0xB8E986, 0x7ED321}),
    PRISM("Prism", "CHROMA", "VIVID", new int[]{0xFF4B4B, 0xFF851B, 0x0074D9, 0xB10DC9}),
    ARC("Arc", "CHROMA", "VIVID", new int[]{0x00D2FF, 0x3A7BD5, 0x00F2FE, 0x4FACFE}),
    BLOOM("Bloom", "CHROMA", "SOFT", new int[]{0xFF758C, 0xFF7EB3, 0xFA709A, 0xFEE140}),
    BOREAL("Boreal", "NATURE", "CRYSTAL", new int[]{0x0BA360, 0x3CBA92, 0x30DD8A, 0x2BB673}),
    VERDANT("Verdant", "NATURE", "BALANCED", new int[]{0x134E5E, 0x71B280, 0x2E7D32, 0x81C784}),
    TIDAL("Tidal", "NATURE", "DEEP", new int[]{0x0F2027, 0x203A43, 0x2C5364, 0x1A365D}),
    DUNE("Dune", "NATURE", "WARM", new int[]{0xD4A373, 0xCCD5AE, 0xE9EDC9, 0xFAEDCD}),
    EMBER("Ember", "WARM", "WARM", new int[]{0xFF4E50, 0xF9D423, 0xE65100, 0xFF8A65}),
    SOLARIS("Solaris", "WARM", "VIVID", new int[]{0xF12711, 0xF5AF19, 0xFFB300, 0xFF6F00}),
    SCARLET("Scarlet", "WARM", "DEEP", new int[]{0x8E0E00, 0x1F1C18, 0xB71C1C, 0x4A148C}),
    ROSEVEIL("Roseveil", "WARM", "SOFT", new int[]{0xF8B195, 0xF67280, 0xC06C84, 0x6C5B7B}),
    NEBULA("Nebula", "DARK", "DEEP", new int[]{0x654EA3, 0xEAAFC8, 0x3F2B96, 0xA8C0FF}),
    NOCTURNE("Nocturne", "DARK", "DEEP", new int[]{0x090A0F, 0x182026, 0x243B55, 0x141E30}),
    VELVET("Velvet", "DARK", "SOFT", new int[]{0x360033, 0x0B8793, 0x4A00E0, 0x8E2DE2});
    private final String displayName;
    private final String group;
    private final String profile;
    private final int[] palette;
    ThemePreset(String name, String group, String profile, int[] palette) {
        this.displayName = name; this.group = group; this.profile = profile; this.palette = palette;
    }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String displayName() { return displayName; }
    public String group() { return group; }
    public String profile() { return profile; }
    public int accent() { return 0xFF000000 | palette[0]; }
    public int colorCount() { return palette.length; }
    public int color(int index) { return 0xFF000000 | palette[index]; }
    public static ThemePreset fromId(String id) {
        if (id != null) for (ThemePreset p : values()) if (p.id().equalsIgnoreCase(id)) return p;
        return CLIENT;
    }
}
