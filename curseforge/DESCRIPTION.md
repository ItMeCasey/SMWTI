# Show Me What This Is

Point at a block or a creature and see what it is, which mod it comes from and what state it is in, in a small panel at the top of your screen.

It is for Forge 26.1.2, where Jade and The One Probe have no Forge build and WTHIT was the only choice. SMWTI shares no code with them.

## What you see

- The block's icon and name, the mod it comes from, and a small tool icon beside the name: a green check when you hold the right tool, a red cross when the block would drop nothing.
- Crop growth, redstone power, how full a composter, cauldron or hive is, furnace and brewing progress, a spawner's countdown, enchanting power around an enchanting table, what is written on a sign, what is in a chest or on a shelf (as icons with counts), tanks, stored energy.
- For creatures: health, armor, active effects with the time left, what they wear, riders, a horse's speed and jump, a villager's job, a pet's owner, how long a young animal takes to grow up, the next egg a chicken lays.
- Sneak for more: the registry id, the block's state, the tool tier it needs, its hardness, and longer item lists.

## Make it yours

Press **Control+F10** for the settings. Pick a theme (dark, light, vanilla, clear or modern), set the size and backdrop, **drag the panel anywhere** (it snaps to centers and corners and stays off your hotbar), switch on compact mode or hold-to-show, or hide anything you do not want shown. **F10** switches the overlay on and off.

Resource packs can add themes and hide, rename or add lines. Servers can cut how far away lines are sent, keep chests private and list things that are never shown.

## For mod authors

SMWTI has a small API and a plugin system: mark a class `@SmwtiPlugin` and it is found automatically, with no hard dependency. See the README on GitHub.

Source and issues: https://github.com/ItMeCasey/SMWTI
