package activity.client.module.api;

import activity.client.gui.icon.ActivityIcon;
import activity.client.module.keybind.Keybind;
import net.minecraft.text.Text;

import java.util.Objects;

/**
 * Unified metadata model for Activity modules and integration stubs.
 *
 * <p>Contains descriptive identity, versioning, authorship, categorization,
 * interactive keybind reference, and social links.
 */
public class ModuleMetadata {

    public static final String DEFAULT_AUTHOR = "Nivorat";
    public static final String DEFAULT_TELEGRAM_URL = "https://t.me/virionDEV";
    public static final String DEFAULT_VERSION = "1.0.0";
    public static final String DEFAULT_LAST_UPDATED = "2026-09-16";

    private final String id;
    private final Text displayName;
    private final Text description;
    private final String author;
    private final String version;
    private final String lastUpdated;
    private final ModuleCategory category;
    private final String telegramUrl;
    private final ActivityIcon icon;
    private final Keybind keybind;
    private final java.util.List<String> aliases;

    public ModuleMetadata(
        String id,
        Text displayName,
        Text description,
        String author,
        String version,
        String lastUpdated,
        ModuleCategory category,
        String telegramUrl,
        ActivityIcon icon,
        Keybind keybind
    ) {
        this(id, displayName, description, author, version, lastUpdated, category, telegramUrl, icon, keybind, java.util.List.of());
    }

    public ModuleMetadata(
        String id,
        Text displayName,
        Text description,
        String author,
        String version,
        String lastUpdated,
        ModuleCategory category,
        String telegramUrl,
        ActivityIcon icon,
        Keybind keybind,
        java.util.List<String> aliases
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = displayName != null ? displayName : Text.literal(id);
        this.description = description != null ? description : Text.empty();
        this.author = author != null ? author : DEFAULT_AUTHOR;
        this.version = version != null ? version : DEFAULT_VERSION;
        this.lastUpdated = lastUpdated != null ? lastUpdated : DEFAULT_LAST_UPDATED;
        this.category = category != null ? category : ModuleCategory.COMBAT;
        this.telegramUrl = telegramUrl != null ? telegramUrl : DEFAULT_TELEGRAM_URL;
        this.icon = icon != null ? icon : ActivityIcon.INFO;
        this.keybind = keybind;
        this.aliases = aliases != null ? java.util.List.copyOf(aliases) : java.util.List.of();
    }

    public String getId() {
        return id;
    }

    public Text getDisplayName() {
        return displayName;
    }

    public Text getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public String getTelegramUrl() {
        return telegramUrl;
    }

    public String getTelegram() {
        return telegramUrl;
    }

    public ActivityIcon getIcon() {
        return icon;
    }

    public Keybind getKeybind() {
        return keybind;
    }

    public boolean hasKeybind() {
        return keybind != null && !keybind.isUnbound();
    }

    public String getKeybindDisplay() {
        if (keybind == null || keybind.isUnbound()) {
            return "[-]";
        }
        return "[" + keybind.getDisplayString() + "]";
    }

    public java.util.List<String> getAliases() {
        return aliases != null ? aliases : java.util.List.of();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private Text displayName;
        private Text description;
        private String author = DEFAULT_AUTHOR;
        private String version = DEFAULT_VERSION;
        private String lastUpdated = DEFAULT_LAST_UPDATED;
        private ModuleCategory category = ModuleCategory.COMBAT;
        private String telegramUrl = DEFAULT_TELEGRAM_URL;
        private ActivityIcon icon = ActivityIcon.INFO;
        private Keybind keybind;
        private java.util.List<String> aliases = new java.util.ArrayList<>();

        public Builder(String id) {
            this.id = id;
        }

        public Builder aliases(String... aliases) {
            if (aliases != null) {
                this.aliases = java.util.List.of(aliases);
            }
            return this;
        }

        public Builder aliases(java.util.List<String> aliases) {
            if (aliases != null) {
                this.aliases = new java.util.ArrayList<>(aliases);
            }
            return this;
        }

        public Builder displayName(Text displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(Text description) {
            this.description = description;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder lastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder category(ModuleCategory category) {
            this.category = category;
            return this;
        }

        public Builder telegramUrl(String telegramUrl) {
            this.telegramUrl = telegramUrl;
            return this;
        }

        public Builder telegram(String telegram) {
            return telegramUrl(telegram);
        }

        public Builder icon(ActivityIcon icon) {
            this.icon = icon;
            return this;
        }

        public Builder keybind(Keybind keybind) {
            this.keybind = keybind;
            return this;
        }

        public ModuleMetadata build() {
            return new ModuleMetadata(
                id,
                displayName,
                description,
                author,
                version,
                lastUpdated,
                category,
                telegramUrl,
                icon,
                keybind,
                aliases
            );
        }
    }
}
