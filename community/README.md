# Каталог сообщества

Пресеты барабанов, наборы тегов и библиотеки игр, которые можно импортировать в GameReel.
Приложение работает офлайн и ничего отсюда не скачивает само: файл нужно скачать
и открыть через импорт.

| Папка | Что внутри | Где импортировать |
| --- | --- | --- |
| `presets/` | Набор категорий барабанов | Генератор → Пресеты → «+ Импорт JSON» |
| `tag-sets/` | Свой пул тегов, барабаны крутятся только по нему | «Наборы» → наборы тегов → «+ Импорт JSON» |
| `libraries/` | Игры с тегами и отдельные теги | «Наборы» → библиотеки → «+ Импорт JSON» |
| `packs/` | Игровые пакеты-рандомайзеры: герой, предметы, таланты | «Наборы» → «+ Импорт JSON» или просто открыть файл на телефоне |

Список всех файлов лежит в [index.json](index.json).

## Как добавить свой файл

1. Экспортируйте пресет, набор тегов или библиотеку из приложения в JSON
   или напишите файл по образцу из соответствующей папки.
2. Назовите файл в `kebab-case`, например `space-western.json`.
3. Запустите проверку. Она обновит `index.json`:
   ```
   python tools/validate_community.py
   ```
4. Откройте pull request с файлом и обновлённым `index.json`.

Правила:

- Категории берутся из списка: `CORE`, `SECONDARY_MECHANIC`, `STRUCTURE`, `PRESSURE`, `CAMERA`,
  `VISUAL`, `SETTING`, `TWIST`, `PROGRESSION`, `MULTIPLAYER`, `INTERACTION`, `SCOPE`,
  `MONETIZATION`, `PLATFORM_TRAIT`, `OTHER`, `THEME`, `WORLD`, `LOCATION`, `ENVIRONMENT`.
- id библиотеки состоит из `A-Z a-z 0-9 _ . -` и не совпадает со встроенными. Рекомендуемый
  префикс: `community.`.
- Без обложек, описаний из магазинов и другого чужого текста. Названия игр и ссылки на их
  страницы допустимы.
- Размер файла до 5 МБ.

## Игровые пакеты

Пакет крутит не идеи, а выбор для настоящей игры: персонажа, предметы, таланты. После импорта
библиотека включается, появляется пресет с именем пакета, и барабаны сразу перестраиваются под
него. Вернуть прежние барабаны можно кнопкой ↶ или любым другим пресетом.

```json
{"library": {
  "id": "dota2", "name": "Dota 2", "version": 1,
  "slots": [
    {"id": "hero", "name": "Hero", "icon": "🦸"},
    {"id": "item", "name": "Item", "icon": "🗡️"},
    {"id": "talent10", "name": "Talent 10", "parent": "hero"}
  ],
  "reels": ["hero", "item", "item", "talent10"],
  "standaloneTags": [
    {"name": "Anti-Mage", "slot": "hero"},
    {"name": "Black King Bar", "slot": "item"},
    {"name": "+20 Agility", "slot": "talent10", "requires": ["Anti-Mage"]}
  ]
}}
```

- `slots`: до 50 видов барабанов. `id` состоит из `A-Z a-z 0-9 _ . -`, `icon` это эмодзи.
- `unique` (по умолчанию `true`): значения на барабанах одного слота не повторяются.
- `parent`: барабан зависит от другого слота. Тег с `requires` выпадает, только если на
  родительском барабане стоит одно из перечисленных имён. Если родителя поменять, зависимые
  барабаны перекрутятся. Тег с одинаковым именем в одном слоте объединяется, его `requires`
  складываются.
- `reels`: какие барабаны поставить при установке, по порядку.
- Только текст. Картинки и иконки из игр не добавляйте.

Пакет Dota 2 собирается из [OpenDota dotaconstants](https://github.com/odota/dotaconstants)
(MIT): `python tools/build_dota_pack.py <папка с heroes.json, items.json, hero_abilities.json, abilities.json>`.
Dota 2 является товарным знаком Valve. Пакет неофициальный и не связан с Valve.

---

# Community catalog

Reel presets, tag sets and game libraries for GameReel. Download a file and import it
in the app (the app stays offline). To contribute, add a `kebab-case.json` file next to the
examples, run `python tools/validate_community.py` and open a pull request.

Game packs (`packs/`) turn the reels into a randomizer for a real game: a hero, items, talents.
A slot can depend on a parent slot through `parent`, and a tag lists the parent names it fits
in `requires`. See the Russian section above for the format.
