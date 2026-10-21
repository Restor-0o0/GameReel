# Steam All-Time Top 1000 — Idea Generator Library

Отдельная библиотека для генератора игровых идей.

**Источник рейтинга:** SteamSpy `all?page=0` — 1000 игр, отсортированных по числу владельцев. Это all-time popularity срез, не текущий топ продаж.

## Быстрый запуск (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build_all.ps1
```

После запуска появятся готовые `games_1000.json/csv`, барабаны и 1000 Steam header-картинок.

## Совместимость с генератором

Формат совпадает с предыдущей Steam-библиотекой. В приложении достаточно добавить поле `library=steam_alltime_owners` и переключатель библиотеки.

Поддерживаются Reference Spin, Atomic Spin, Pin tag / Pin mechanic / Pin graphics и замороженные барабаны слева — см. `docs/IDEA_GENERATOR_SPEC.md`.

## Tag enrichment v4

Добавлена расширенная атомарная разметка и равномерные веса барабанов. Пустые camera/visual больше не превращаются в повторяющийся `unspecified`.

Для уже собранного `data/games_1000.json`:

```powershell
python .\scripts\enrich_existing.py
```

Подробности: `docs/TAG_ENRICHMENT.md`.
