package com.itmecasey.smwti.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.smwti.api.Position;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ScriptRulesTest {
    private static final ScriptRules.BlockInfo FURNACE = new ScriptRules.BlockInfo("minecraft:furnace", 1, 2, 3, Map.of("lit", "true"), false);

    @AfterEach
    void reset() {
        ScriptRules.clear();
    }

    @Test
    void addsLinesOnlyForTheMatchingBlock() {
        ScriptRules.addBlockLine("minecraft:furnace", "head", b -> "Lit: " + b.properties().get("lit"));
        ScriptRules.addBlockLine("minecraft:chest", "body", b -> "never");
        assertEquals(List.of(new ScriptRules.Placed(Position.HEAD, "Lit: true")), ScriptRules.blockLines(FURNACE));
    }

    @Test
    void aFailingOrEmptyFunctionAddsNothingAndTheRestStillRun() {
        ScriptRules.addBlockLine("minecraft:furnace", "body", b -> {
            throw new IllegalStateException("boom");
        });
        ScriptRules.addBlockLine("minecraft:furnace", "body", b -> null);
        ScriptRules.addBlockLine("minecraft:furnace", "tail", b -> "ok");
        assertEquals(List.of(new ScriptRules.Placed(Position.TAIL, "ok")), ScriptRules.blockLines(FURNACE));
    }

    @Test
    void entityLinesGetTheEntity() {
        ScriptRules.addEntityLine("minecraft:cow", "badge", e -> e.name() + "!");
        assertEquals(List.of(new ScriptRules.Placed(Position.BADGE, "Cow!")),
                ScriptRules.entityLines(new ScriptRules.EntityInfo("minecraft:cow", "Cow", 0, 0, 0, true)));
    }

    @Test
    void positionsAreForgivingAndDefaultToTheBody() {
        assertEquals(Position.TAIL, ScriptRules.position(" Tail "));
        assertEquals(Position.BODY, ScriptRules.position("somewhere"));
        assertEquals(Position.BODY, ScriptRules.position(null));
    }

    @Test
    void renamesAndHidesAreByIdAndClearForgetsThem() {
        ScriptRules.renameBlock("minecraft:stone", "Rock");
        ScriptRules.hideEntity("minecraft:bat");
        assertEquals("Rock", ScriptRules.blockName("minecraft:stone"));
        assertNull(ScriptRules.entityName("minecraft:stone"));
        assertTrue(ScriptRules.entityHidden("minecraft:bat"));
        assertFalse(ScriptRules.blockHidden("minecraft:bat"));
        ScriptRules.clear();
        assertNull(ScriptRules.blockName("minecraft:stone"));
        assertFalse(ScriptRules.entityHidden("minecraft:bat"));
    }

    @Test
    void withoutModJsTheBridgeDoesNothing() {
        assertFalse(ModJsBridge.install());
    }
}
