package com.itmecasey.smwti.script;

import com.itmecasey.smwti.api.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * What ModJS scripts asked the overlay to do: extra lines, other names and hidden targets, keyed by block or entity id. This is plain
 * Java, so tests can use it directly; {@link ModJsBridge} fills it from script functions and {@link ScriptProviders} applies it.
 */
public final class ScriptRules {
    private static final Logger LOG = Logger.getLogger("smwti");

    /** A block as a script sees it: its id, position, block state properties and whether the long form is on. */
    public record BlockInfo(String id, int x, int y, int z, Map<String, String> properties, boolean detailed) {
    }

    /** An entity as a script sees it: its type id, name, position and whether the long form is on. */
    public record EntityInfo(String id, String name, double x, double y, double z, boolean detailed) {
    }

    /** A script function that answers the text of one overlay line, or null for no line. */
    @FunctionalInterface
    public interface BlockLine {
        String line(BlockInfo block);
    }

    /** A script function that answers the text of one overlay line, or null for no line. */
    @FunctionalInterface
    public interface EntityLine {
        String line(EntityInfo entity);
    }

    /** A line a script function produced, and where it goes. */
    public record Placed(Position position, String text) {
    }

    private record BlockRule(String id, Position position, BlockLine function) {
    }

    private record EntityRule(String id, Position position, EntityLine function) {
    }

    private static final List<BlockRule> BLOCK_LINES = new CopyOnWriteArrayList<>();
    private static final List<EntityRule> ENTITY_LINES = new CopyOnWriteArrayList<>();
    private static final Map<String, String> BLOCK_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> ENTITY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> HIDDEN_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> HIDDEN_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> REPORTED = new ConcurrentHashMap<>();

    private ScriptRules() {
    }

    /** Forgets everything scripts registered; ModJS runs the scripts again on a reload. */
    public static void clear() {
        BLOCK_LINES.clear();
        ENTITY_LINES.clear();
        BLOCK_NAMES.clear();
        ENTITY_NAMES.clear();
        HIDDEN_BLOCKS.clear();
        HIDDEN_ENTITIES.clear();
        REPORTED.clear();
    }

    /** Reads a part name such as "head" or "TAIL"; anything unknown is the body. */
    public static Position position(String name) {
        if (name != null) {
            try {
                return Position.valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // fall through to the body
            }
        }
        return Position.BODY;
    }

    public static void addBlockLine(String id, String position, BlockLine function) {
        BLOCK_LINES.add(new BlockRule(id, position(position), function));
    }

    public static void addEntityLine(String id, String position, EntityLine function) {
        ENTITY_LINES.add(new EntityRule(id, position(position), function));
    }

    public static void renameBlock(String id, String name) {
        BLOCK_NAMES.put(id, name);
    }

    public static void renameEntity(String id, String name) {
        ENTITY_NAMES.put(id, name);
    }

    public static void hideBlock(String id) {
        HIDDEN_BLOCKS.put(id, Boolean.TRUE);
    }

    public static void hideEntity(String id) {
        HIDDEN_ENTITIES.put(id, Boolean.TRUE);
    }

    public static boolean blockHidden(String id) {
        return HIDDEN_BLOCKS.containsKey(id);
    }

    public static boolean entityHidden(String id) {
        return HIDDEN_ENTITIES.containsKey(id);
    }

    /** The scripted name of a block, or null. */
    public static String blockName(String id) {
        return BLOCK_NAMES.get(id);
    }

    /** The scripted name of an entity type, or null. */
    public static String entityName(String id) {
        return ENTITY_NAMES.get(id);
    }

    /** The lines the scripts add for this block. A function that throws is logged once and skipped. */
    public static List<Placed> blockLines(BlockInfo block) {
        List<Placed> out = new ArrayList<>();
        for (BlockRule rule : BLOCK_LINES) {
            if (rule.id().equals(block.id())) {
                String text = call(() -> rule.function().line(block), "block line for " + rule.id());
                if (text != null && !text.isEmpty()) {
                    out.add(new Placed(rule.position(), text));
                }
            }
        }
        return out;
    }

    /** The lines the scripts add for this entity. A function that throws is logged once and skipped. */
    public static List<Placed> entityLines(EntityInfo entity) {
        List<Placed> out = new ArrayList<>();
        for (EntityRule rule : ENTITY_LINES) {
            if (rule.id().equals(entity.id())) {
                String text = call(() -> rule.function().line(entity), "entity line for " + rule.id());
                if (text != null && !text.isEmpty()) {
                    out.add(new Placed(rule.position(), text));
                }
            }
        }
        return out;
    }

    private static String call(Supplier<String> function, String what) {
        try {
            Object result = function.get();
            return result == null ? null : result.toString();
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED.putIfAbsent(what, Boolean.TRUE) == null) {
                LOG.warning("SMWTI: a script's " + what + " failed: " + e);
            }
            return null;
        }
    }
}
