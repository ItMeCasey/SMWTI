package com.itmecasey.smwti.script;

import com.itmecasey.smwti.api.BlockView;
import com.itmecasey.smwti.api.EntityView;
import com.itmecasey.smwti.api.Line;
import com.itmecasey.smwti.api.SmwtiApi;
import com.itmecasey.smwti.api.Tooltip;
import com.itmecasey.smwti.client.LookTracker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The {@code SMWTI} global of ModJS client scripts. Positions are {@code "badge"}, {@code "head"}, {@code "body"} and {@code "tail"}.
 * Line functions get the block or entity and answer the text of one line, or null to add none.
 */
public final class ScriptApi {
    /** Adds a line to the overlay for one kind of block: {@code SMWTI.addBlockLine("minecraft:furnace", "body", b => "Lit: " + b.properties().get("lit"))}. */
    public void addBlockLine(String blockId, String position, ScriptRules.BlockLine line) {
        ScriptRules.addBlockLine(blockId, position, line);
    }

    /** Adds a line to the overlay for one kind of entity. */
    public void addEntityLine(String entityId, String position, ScriptRules.EntityLine line) {
        ScriptRules.addEntityLine(entityId, position, line);
    }

    /** Shows another name for a block in the overlay. */
    public void renameBlock(String blockId, String name) {
        ScriptRules.renameBlock(blockId, name);
    }

    /** Shows another name for an entity type in the overlay. */
    public void renameEntity(String entityId, String name) {
        ScriptRules.renameEntity(entityId, name);
    }

    /** The overlay never shows this block. */
    public void hideBlock(String blockId) {
        ScriptRules.hideBlock(blockId);
    }

    /** The overlay never shows this kind of entity. */
    public void hideEntity(String entityId) {
        ScriptRules.hideEntity(entityId);
    }

    /** The text of every line the overlay would show for what the crosshair is on (empty when it is on nothing). */
    public List<String> lookedAtLines() {
        List<String> out = new ArrayList<>();
        try {
            Minecraft game = Minecraft.getInstance();
            HitResult hit = game.hitResult;
            Tooltip tooltip = new Tooltip();
            if (game.level != null && hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                BlockView view = new BlockView(game.level, block.getBlockPos(), game.level.getBlockState(block.getBlockPos()),
                        game.level.getBlockEntity(block.getBlockPos()), false, LookTracker.serverLines());
                SmwtiApi.blockProviders().forEach(provider -> provider.append(tooltip, view));
            } else if (hit instanceof EntityHitResult entity) {
                EntityView view = new EntityView(entity.getEntity(), false, LookTracker.serverLines());
                SmwtiApi.entityProviders().forEach(provider -> provider.append(tooltip, view));
            }
            for (Line line : tooltip.lines()) {
                out.add(line.text().getString());
            }
        } catch (RuntimeException | LinkageError e) {
            // A provider that fails leaves the lines it had.
        }
        return out;
    }
}
