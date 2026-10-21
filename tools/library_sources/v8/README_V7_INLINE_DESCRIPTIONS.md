# GameReel v7 — inline tag descriptions

Descriptions are now embedded in the primary indexes used by the app.

- `ALL_TAGS_BY_REEL_v5.json`: existing `reels` remains unchanged; new `reel_items` contains `{label, description, description_ru, source}`.
- `MASTER_TAG_DICTIONARY_v5.json`: existing `categories` remains unchanged; new `category_items` and `tag_descriptions` provide the same metadata.
- Existing curated JSON files still contain descriptions.

Example: `disassembly work` now has its description directly available from the primary index.
