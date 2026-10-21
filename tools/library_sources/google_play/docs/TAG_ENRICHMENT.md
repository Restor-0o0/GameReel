# Tag enrichment v4

Эта версия исправляет две проблемы старых библиотек GameReel:

1. У многих игр было слишком мало размеченных атомов: `core_mechanics` часто строился только из 1–2 жанровых тегов, а `structure / progression / pressure / interaction / setting / theme` вообще не существовали.
2. В `reels_from_1000.json` популярные атомы имели вес до 20, поэтому `combat / action / multiplayer` выпадали намного чаще редких вариантов.

## Что изменилось

Каждая игра теперь может иметь независимые группы:

- `core_mechanics`
- `extra_mechanics`
- `structure_tags`
- `progression_tags`
- `pressure_tags`
- `interaction_tags`
- `setting_tags`
- `theme_tags`
- `visual_tags`
- `camera_tags`
- `player_tags`
- `other_tags`

`other_tags` сохраняет полезные исходные теги, которые не удалось надёжно отнести к конкретному барабану. Они остаются доступными для поиска и `Pin`, но не загрязняют КОР.

## Неизвестные значения

Старые значения вроде:

- `unspecified`
- `infer from reference image`
- `unspecified / infer from game`

больше не должны попадать в барабаны как обычные варианты. Если камера или графика неизвестна, список для этой игры остаётся пустым.

## Вес барабанов

По умолчанию каждый уникальный атом получает:

```json
{"label":"physics interaction","weight":1,"frequency":37}
```

`frequency` показывает, сколько игр содержат этот атом, но не делает его автоматически более вероятным.

Это специально сделано для режима генерации идей: редкая механика должна иметь шанс выпасть, а не проигрывать популярному `combat` в 20 раз.

## Coverage report

После сборки создаётся:

```text
data/tag_coverage_report.json
```

В нём видно:

- сколько игр имеют каждый тип разметки;
- сколько уникальных атомов найдено;
- какие игры всё ещё `sparse`.

Это позволяет дальше дорабатывать правила не вслепую.

## Steam: обновить уже собранную библиотеку без повторного скачивания рейтинга

Если `data/games_1000.json` уже существует:

```powershell
python .\scripts\enrich_existing.py
```

Скрипт использует уже сохранённые `steam_tags` и жанр, создаёт backup и перестраивает барабаны.

## Google Play

У Google Play исходный Kaggle snapshot содержит в основном категорию и метаданные магазина, поэтому одной категории недостаточно для механик.

В v4 есть два прохода:

1. offline fallback по `category + title`;
2. online enrichment по `summary + description + genre` из Google Play через `google-play-scraper`.

Полный проход:

```powershell
python -m pip install google-play-scraper
python .\scripts\enrich_existing.py --online
```

Метаданные кэшируются в:

```text
data/google_play_metadata_cache.json
```

Поэтому прерванный запуск можно продолжить и не запрашивать уже обработанные игры снова.

## Важное ограничение

Разметка остаётся эвристической. Скрипт намеренно использует консервативные правила и предпочитает оставить поле пустым, а не уверенно приписать игре неверную механику. Исходные Steam tags и Google Play metadata сохраняются, поэтому вручную `Pin` всегда доступен.
