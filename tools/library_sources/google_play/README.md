# Google Play Top 1000 — Idea Generator Library

Отдельная мобильная библиотека для генератора игровых идей.

Это **не официальный Google all-time chart** — такого единого публичного топа нет. Набор строится из 2.3M+ Google Play приложений и выбирает 1000 игр с наибольшим `Maximum Installs` (далее Rating Count и Rating).

## Быстрый запуск (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build_all.ps1
```

Скрипт:

1. скачает публичный Kaggle dataset;
2. отберёт top-1000 игр;
3. создаст JSON/CSV и барабаны;
4. попробует скачать reference image для каждой игры через Google Play scraper.

Если Kaggle не скачивается автоматически, вручную положи `Google-Playstore.csv` в `data/raw/` и запусти `build_all.ps1` снова.

Поддерживаются Reference Spin и безусловный `Pin` в левую замороженную область — см. `docs/IDEA_GENERATOR_SPEC.md`.

## Tag enrichment v4

Google Play теперь размечается не только по категории. `build_all.ps1` после базовой сборки делает второй проход через Google Play metadata (`summary / description / genre`) и извлекает mechanics, structure, progression, pressure, visual, camera, setting и multiplayer-теги.

Для уже собранной библиотеки:

```powershell
python -m pip install google-play-scraper
python .\scripts\enrich_existing.py --online
```

Результаты запросов кэшируются, запуск можно продолжать после остановки. Подробности: `docs/TAG_ENRICHMENT.md`.
