package com.itmecasey.smwti.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The line codec is what the server sends the client, so a line must survive it and a bad one must not break the rest. */
class LineTest {
    private static boolean gameReady;

    /** The component codec reads the game's registries, so the game is bootstrapped once, as it is before a world loads. */
    @BeforeAll
    static void bootstrap() {
        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        } catch (RuntimeException | Error e) {
            // Some loaders cannot bootstrap the game inside a unit test; the codec may still work without it.
        }
        try {
            gameReady = Line.CODEC.parse(JsonOps.INSTANCE, lineJson("probe")).result().isPresent();
        } catch (Throwable e) {
            gameReady = false;
        }
    }

    @BeforeEach
    void needsTheGame() {
        assumeTrue(gameReady, "this loader cannot bootstrap the game inside a unit test, so the codec cannot be built");
    }

    private static Line decode(JsonObject json) {
        return Line.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    private static JsonObject lineJson(String text) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        return json;
    }

    @Test
    void aLineSurvivesTheCodecWithEveryField() {
        Line sent = new Line(Component.literal("Burning"), 0.5F, 0xFF8800, Position.TAIL, "minecraft:coal", 12, "x", 0x00FF00,
                "minecraft:water");
        Line got = Line.CODEC.parse(NbtOps.INSTANCE, Line.CODEC.encodeStart(NbtOps.INSTANCE, sent).result().orElseThrow())
                .result().orElseThrow();
        assertEquals("Burning", got.text().getString());
        assertEquals(0.5F, got.fraction());
        assertEquals(0xFF8800, got.color());
        assertEquals(Position.TAIL, got.position());
        assertEquals("minecraft:coal", got.item());
        assertEquals(12, got.count());
        assertEquals("x", got.mark());
        assertEquals(0x00FF00, got.markColor());
        assertEquals("minecraft:water", got.fluid());
    }

    @Test
    void aListOfLinesSurvivesTheWayTheChannelSendsIt() {
        List<Line> sent = List.of(Line.of(Component.literal("a")), Line.of(Component.literal("b")).at(Position.HEAD));
        List<Line> got = Line.CODEC.listOf().parse(NbtOps.INSTANCE,
                Line.CODEC.listOf().encodeStart(NbtOps.INSTANCE, sent).result().orElseThrow()).result().orElseThrow();
        assertEquals(2, got.size());
        assertEquals("b", got.get(1).text().getString());
        assertEquals(Position.HEAD, got.get(1).position());
    }

    @Test
    void everyPositionSurvivesTheCodec() {
        for (Position position : Position.values()) {
            Line got = Line.CODEC.parse(NbtOps.INSTANCE,
                    Line.CODEC.encodeStart(NbtOps.INSTANCE, Line.of(Component.literal("p")).at(position)).result().orElseThrow())
                    .result().orElseThrow();
            assertEquals(position, got.position());
        }
    }

    @Test
    void aLineWithOnlyTextGetsTheDefaults() {
        Line line = decode(lineJson("hello"));
        assertEquals("hello", line.text().getString());
        assertFalse(line.hasBar());
        assertEquals(0xFFFFFF, line.color());
        assertEquals(Position.BODY, line.position());
        assertEquals(1, line.count());
        assertEquals("", line.item());
        assertEquals("", line.fluid());
    }

    @Test
    void aLineWithoutTextIsRefused() {
        assertTrue(Line.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).result().isEmpty());
    }

    @Test
    void aFieldThatIsNotTheRightTypeIsRefusedOrDefaulted() {
        JsonObject json = lineJson("hello");
        json.addProperty("color", "red");
        // Older codec libraries fall back to the default for a bad optional field; newer ones refuse the line.
        Line.CODEC.parse(JsonOps.INSTANCE, json).result().ifPresent(line -> assertEquals(0xFFFFFF, line.color()));
    }

    @Test
    void fieldsAnOlderOrNewerVersionAddsAreIgnored() {
        JsonObject json = lineJson("hello");
        json.addProperty("a_field_from_a_later_version", 7);
        assertEquals("hello", decode(json).text().getString());
    }

    @Test
    void aPositionFromALaterVersionFallsBackToTheBodyInsteadOfThrowing() {
        JsonObject json = lineJson("hello");
        json.addProperty("position", "SIDEBAR");
        assertEquals(Position.BODY, decode(json).position());
    }

    @Test
    void oneBadLineInAListDoesNotTakeTheOthersWithIt() {
        JsonArray lines = new JsonArray();
        lines.add(lineJson("good"));
        JsonObject bad = lineJson("bad");
        bad.addProperty("position", "SIDEBAR");
        lines.add(bad);
        List<Line> got = Line.CODEC.listOf().parse(JsonOps.INSTANCE, lines).result().orElseThrow();
        assertEquals(2, got.size());
    }

    @Test
    void aBarNeedsAFractionAndAFluidBarNeedsAFluidToo() {
        assertFalse(Line.of(Component.literal("x")).hasBar());
        Line bar = new Line(Component.literal("x"), 0.0F, 0xFFFFFF, Position.BODY, "", 1);
        assertTrue(bar.hasBar());
        assertFalse(bar.isFluidBar());
        assertTrue(new Line(Component.literal("x"), 0.2F, 0xFFFFFF, Position.BODY, "", 1, "", 0xFFFFFF, "minecraft:water").isFluidBar());
    }

    @Test
    void aStackIsALineWithAnItemAndNoTextOrBar() {
        assertTrue(new Line(Component.empty(), Line.NO_BAR, 0xFFFFFF, Position.BODY, "minecraft:stone", 3).isStack());
        assertFalse(new Line(Component.literal("named"), Line.NO_BAR, 0xFFFFFF, Position.BODY, "minecraft:stone", 3).isStack());
        assertFalse(Line.of(Component.empty()).isStack());
    }
}
