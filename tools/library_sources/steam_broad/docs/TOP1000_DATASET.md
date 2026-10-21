# Top-1000 Steam dataset для генератора идей

## Источник

Публичный CSV `steam_games_2026.csv`, содержащий ровно 1000 игр + строку заголовка. В полях есть AppID, название, дата релиза, основной жанр, Steam-теги, цена, процент положительных отзывов, число отзывов, оценка владельцев и пиковый онлайн.

## Как база превращается в генератор

Steam-теги не используются как финальная игровая концепция. Они нормализуются в более удобные атомы:

- `FPS`, `Shooter`, `Third-Person Shooter` -> `shooting`;
- `Crafting` -> `crafting`;
- `Building`, `Base Building` -> `building / base building`;
- `Physics` -> `physics`;
- `Destruction` -> `destruction`;
- `Roguelike`, `Roguelite` -> `run-based progression`;
- `Deckbuilding` -> `deckbuilding`;
- `Management` -> `management`;
- `Farming Sim` -> `farming`;
- `Creature Collector` -> `creature collection`;
- `Investigation` -> `investigation`;
- `Tower Defense` -> `tower defense`;
- `Colony Sim` -> `colony management`;
- и т.д.

Первые 3 нормализованных атома становятся `core_mechanics`, следующие — `extra_mechanics`.

## Графика

Тип графики выводится из тегов как производственный класс, а не как художественная экспертиза:

- Pixel Graphics -> pixel art 2D;
- Voxel -> voxel 3D;
- Hand-drawn -> hand-drawn 2D;
- Anime -> anime stylized;
- Cartoony/Cartoon -> cartoon stylized;
- Realistic -> realistic 3D;
- 2.5D -> 2.5D;
- 2D + 3D -> mixed 2D/3D.

Если визуальный тег отсутствует, поле помечается `unspecified / infer from reference image`. В UI в таком случае удобнее показывать Steam header и позволять пользователю самому интерпретировать визуал.

## Барабаны

Помимо барабанов из основной спецификации стоит добавить:

- **ИГРА-РЕФЕРЕНС** — случайная игра из 1000, с картинкой;
- **ЖАНРОВАЯ ОСНОВА** — Action / Strategy / Simulation / Indie / Adventure и т.д.;
- **МЕХАНИКА ИЗ РЕФЕРЕНСА** — один случайный нормализованный атом именно выбранной игры;
- **ГРАФИКА ИЗ РЕФЕРЕНСА** — визуальный класс выбранной игры;
- **ТЕГ-ХАОС** — любой оригинальный Steam-тег без нормализации, полезен для странных комбинаций.

## Рекомендуемый режим генерации

Для твоего кейса я бы сделал два отдельных способа Spin:

**Atomic Spin** — каждый барабан независимо выбирает механику/графику/камеру/структуру.

**Reference Spin** — сначала выпадают 2–3 игры, затем генератор вытягивает из каждой по одному аспекту. Например:

`Teardown [physics/destruction] + Stardew Valley [daily progression] + DREDGE [fishing/inventory pressure]`

После этого можно заморозить удачный аспект и перекручивать остальные.
