package com.itmecasey.smwti.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itmecasey.smwti.Smwti;
import com.itmecasey.smwti.api.Option;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Keeps the player's choices for the options mods add (see {@link Option}) in
 * {@code config/smwti-options.json}, a flat object from option id to true or false. Options are
 * not part of the TOML config because their number and names are not known until every mod has
 * loaded.
 */
public final class OptionsFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OptionsFile() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("smwti-options.json");
    }

    /** Reads the saved choices; a missing or broken file leaves every option at its default. */
    public static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (id != null && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
                    Option.choose(id, entry.getValue().getAsBoolean());
                }
            }
        } catch (IOException | RuntimeException e) {
            Smwti.LOGGER.warn("Could not read {}; the options stay at their defaults", file, e);
        }
    }

    /** Writes every recorded choice. */
    public static void save() {
        JsonObject json = new JsonObject();
        Option.choices().forEach((id, value) -> json.addProperty(id.toString(), value));
        try (Writer writer = Files.newBufferedWriter(file())) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            Smwti.LOGGER.warn("Could not write {}", file(), e);
        }
    }
}
