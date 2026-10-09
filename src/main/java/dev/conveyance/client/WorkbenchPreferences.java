package dev.conveyance.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Local UI choices, independent of the player's items and world research. */
final class WorkbenchPreferences {
    private static boolean loaded;
    private static int tab;
    private static String category = "production";
    private static Path path() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/conveyance-ui.json"); }
    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(path())) return;
            JsonObject data = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            if (data.has("tab")) tab = Math.max(0, Math.min(2, data.get("tab").getAsInt()));
            if (data.has("category")) category = data.get("category").getAsString();
        } catch (java.io.IOException | RuntimeException ignored) { tab = 0; category = "production"; }
    }
    static int tab() { load(); return tab; }
    static String category() { load(); return category; }
    static void remember(int selectedTab, String selectedCategory) {
        load(); tab = selectedTab; category = selectedCategory;
        try {
            Files.createDirectories(path().getParent());
            JsonObject data = new JsonObject(); data.addProperty("tab", tab); data.addProperty("category", category);
            Files.writeString(path(), data.toString());
        } catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not save Conveyance UI preferences", failure); }
    }
    private WorkbenchPreferences() {}
}
