# GameReel tag descriptions patch (v6)

This patch adds descriptions to all curated/author tags.

## Changes

- All 1,439 entries in every `data/curated_reels_v5.json` are now objects with:
  - `label`
  - `description` (English)
  - `description_ru` (Russian)
- `scripts/curated_reels.py` now preserves curated metadata when merging with source-derived tags.
- If a curated tag also exists in the source dataset, its real `frequency` is preserved while descriptions are attached.
- Added root `TAG_DESCRIPTIONS_v6.json` as a direct lookup dictionary for UI code.
- Existing tag labels, reel IDs, weights and source-frequency semantics are unchanged.

Example:

```json
{
  "label": "factory automation",
  "description": "Core loop focused on factory automation: the player builds or configures systems that perform work with less direct input.",
  "description_ru": "Основной цикл вокруг «factory automation»: игрок строит или настраивает процессы, которые затем выполняют работу автоматически."
}
```
