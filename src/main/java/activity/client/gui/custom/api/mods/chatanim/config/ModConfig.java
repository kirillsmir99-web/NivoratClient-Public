package activity.client.gui.custom.api.mods.chatanim.config;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public final class ModConfig {
    private static ModConfig instance;
    private static final Gson GSON;
    private transient File configFile;
    @SerializedName("enableMessageAnimation")
    public boolean enableMessageAnimation = true;
    @SerializedName("enableTextFieldAnimation")
    public boolean enableTextFieldAnimation = true;
    @SerializedName("removeMessageIndicator")
    public boolean removeMessageIndicator = true;
    @SerializedName("fadeTimeMessage")
    public int fadeTimeMessage = 150;
    @SerializedName("fadeTimeTextField")
    public int fadeTimeTextField = 170;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load(File file) {
        this.configFile = file;
        if (!file.exists()) {
            this.save();
            return;
        }
        try (FileReader fileReader = new FileReader(file);){
            ModConfig modConfig = (ModConfig)GSON.fromJson((Reader)fileReader, ModConfig.class);
            if (modConfig != null) {
                instance = modConfig;
                ModConfig.instance.configFile = file;
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void save() {
        if (this.configFile == null) {
            return;
        }
        try (FileWriter fileWriter = new FileWriter(this.configFile);){
            GSON.toJson((Object)this, (Appendable)fileWriter);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public static ModConfig getConfig() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }
}
