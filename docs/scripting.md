# ModJS scripting

SMWTI adds a global `SMWTI` to ModJS **client scripts** when ModJS is installed. Without ModJS nothing is loaded. A script that fails is logged as one line and skipped; it never stops the overlay.

Ids are registry ids (`minecraft:furnace`, `minecraft:cow`). Positions are `"badge"` (beside the name), `"head"` (under the name), `"body"` and `"tail"`; anything else means the body.

| Call | What it does |
|---|---|
| `SMWTI.addBlockLine(blockId, position, fn)` | Adds a line for a kind of block. `fn(block)` answers the text, or `null` for no line. |
| `SMWTI.addEntityLine(entityId, position, fn)` | The same for a kind of entity. |
| `SMWTI.renameBlock(blockId, name)` | Another name for the block in the overlay. |
| `SMWTI.renameEntity(entityId, name)` | Another name for the entity type. |
| `SMWTI.hideBlock(blockId)` | The overlay never shows this block. |
| `SMWTI.hideEntity(entityId)` | The overlay never shows this kind of entity. |
| `SMWTI.lookedAtLines()` | The text of every line the overlay would show for what the crosshair is on. |

`block` has `id()`, `x()`, `y()`, `z()`, `properties()` (block state values as text) and `detailed()` (the long form is on). `entity` has `id()`, `name()`, `x()`, `y()`, `z()` and `detailed()`.

Scripts run again on `/modjs reload`; what a script registered before is dropped first.

```js
// client_scripts/smwti.js
SMWTI.addBlockLine("minecraft:furnace", "body", b => b.properties().get("lit") == "true" ? "Burning" : null);
SMWTI.addEntityLine("minecraft:cow", "badge", e => e.detailed() ? e.id() : null);
SMWTI.renameBlock("minecraft:stone", "Rock");
SMWTI.hideEntity("minecraft:bat");
```
