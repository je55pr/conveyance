package dev.conveyance.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Local UI choices, independent of the player's items and world research. */
final class WorkbenchPreferences {
    private static boolean loaded;
    private static String category = "production";
    private static boolean pollution;
    private static Path path() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/conveyance-ui.json"); }
    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(path())) return;
            JsonObject data = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            if (data.has("category")) category = data.get("category").getAsString();
            if (data.has("pollution")) pollution = data.get("pollution").getAsBoolean();
        } catch (java.io.IOException | RuntimeException ignored) { category = "production"; }
    }
    static String category() { load(); return category; }
    static boolean pollution() { load(); return pollution; }
    static void pollution(boolean enabled) { load(); pollution = enabled; save(); }
    static void remember(String selectedCategory) {
        load(); category = selectedCategory;
        save();
    }
    private static void save() {
        try {
            Files.createDirectories(path().getParent());
            JsonObject data = new JsonObject(); data.addProperty("category", category);
            data.addProperty("pollution", pollution);
            Files.writeString(path(), data.toString());
        } catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not save Conveyance UI preferences", failure); }
    }
    private WorkbenchPreferences() {}
}
