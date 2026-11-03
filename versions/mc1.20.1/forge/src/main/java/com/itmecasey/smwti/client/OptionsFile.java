package com.itmecasey.smwti.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.SettingValues;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Keeps the player's choices for the settings mods add (see {@link com.itmecasey.smwti.api.Setting})
 * in {@code config/smwti-options.json}, a flat object from setting id to true or false, a whole
 * number, or a text. They are not part of the TOML config because their number and names are not
 * known until every mod has loaded.
 */
public final class OptionsFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OptionsFile() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("smwti-options.json");
    }

    /** Reads the saved choices; a missing or broken file leaves every setting at its default. */
    public static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null || !entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
                if (value.isBoolean()) {
                    SettingValues.set(id, value.getAsBoolean());
                } else if (value.isNumber()) {
                    SettingValues.set(id, value.getAsInt());
                } else if (value.isString()) {
                    SettingValues.set(id, value.getAsString());
                }
            }
        } catch (IOException | RuntimeException e) {
            Smwti.LOGGER.warn("Could not read {}; the settings stay at their defaults", file, e);
        }
    }

    /** Writes every recorded choice. */
    public static void save() {
        JsonObject json = new JsonObject();
        SettingValues.all().forEach((id, value) -> {
            if (value instanceof Boolean flag) {
                json.addProperty(id.toString(), flag);
            } else if (value instanceof Number number) {
                json.addProperty(id.toString(), number.intValue());
            } else if (value instanceof String text) {
                json.addProperty(id.toString(), text);
            }
        });
        try (Writer writer = Files.newBufferedWriter(file())) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            Smwti.LOGGER.warn("Could not write {}", file(), e);
        }
    }
}
