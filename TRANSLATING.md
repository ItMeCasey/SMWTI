# Translating SMWTI

All text is in one file per language: `src/main/resources/assets/smwti/lang/<locale>.json`, where the locale is the game's code (`de_de`, `fr_fr`, `es_es`, `pt_br`, `ja_jp`, `zh_cn` and so on). English, `en_us.json`, is the source.

1. Copy `en_us.json` to `<locale>.json` and translate the values. Keep the keys, and keep every `%s` in place: each one is filled in with a number or a name, in the same order as the English line.
2. Anything you leave out falls back to English, so a partial translation is fine.
3. Names of mods and blocks are not in this file; the game translates those itself.
4. Send the file as a pull request.

Settings added by other mods have their own keys (`smwti.option.<mod>.<name>`) in that mod's language files, not here.
