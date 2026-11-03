package com.itmecasey.smwti.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.itmecasey.smwti.Smwti;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The blocks and entity types the player hid from the overlay in the game (the hide key, or the
 * settings screen), kept in {@code config/smwti-hidden.json} as a list of registry ids. They add to
 * what resource packs hide (see {@link PackRules}); the player can show any of them again from the
 * settings.
 */
public final class UserHidden {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TreeSet<String> HIDDEN = new TreeSet<>();

    private UserHidden() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("smwti-hidden.json");
    }

    /** Reads the saved list; a missing or broken file leaves it empty. */
    public static synchronized void load() {
        HIDDEN.clear();
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                HIDDEN.add(element.getAsString());
            }
        } catch (IOException | RuntimeException e) {
            Smwti.LOGGER.warn("Could not read {}; nothing is hidden by the player", file, e);
            HIDDEN.clear();
        }
    }

    private static void save() {
        JsonArray array = new JsonArray();
        HIDDEN.forEach(array::add);
        try (Writer writer = Files.newBufferedWriter(file())) {
            GSON.toJson(array, writer);
        } catch (IOException e) {
            Smwti.LOGGER.warn("Could not write {}", file(), e);
        }
    }

    /** Whether the player hid the thing with this registry id. */
    public static synchronized boolean contains(String id) {
        return HIDDEN.contains(id);
    }

    /** Hides a block or entity type. */
    public static synchronized void add(String id) {
        if (HIDDEN.add(id)) {
            save();
        }
    }

    /** Shows a hidden block or entity type again. */
    public static synchronized void remove(String id) {
        if (HIDDEN.remove(id)) {
            save();
        }
    }

    /** The hidden ids in alphabetical order. */
    public static synchronized List<String> all() {
        return Collections.unmodifiableList(new ArrayList<>(HIDDEN));
    }
}
