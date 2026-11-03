# SMWTI branding

Logo, banners and store listing copy for Show Me What This Is.

- `icon/`: the logo, 32x32 pixel art scaled up without smoothing. `icon_<size>.png` for every size, `icon.svg` for vector use, `smwti_logo.png` for the jar.
- `banners/`: description banner, gallery features card, themes comparison, social preview and section headers.
- `LISTING.md`: CurseForge and Modrinth fields, summaries, description text and a setup checklist.
- `source/`: the scripts that draw all of it. Everything is original, including the pixel font and sprites; no Minecraft textures are used.

Regenerate from the repository root:

```
python branding/source/make_icon.py
python branding/source/render_banners.py
```

The second script needs Python, Pillow and Google Chrome.

## Palette

| Name | Hex | Use |
|---|---|---|
| Night | `#0f0e1a` to `#1a1830` | Backgrounds |
| Edge | `#2e2a46` | Borders, tile edge |
| Gold | `#f2b33d` (light `#ffe28c`, dark `#b27420`) | Wordmark, lens frame, highlights |
| Mod blue | `#7aa8ff` | Mod names in panels |
| Text | `#f4f2ff`, muted `#a9a4c8` | Copy |
