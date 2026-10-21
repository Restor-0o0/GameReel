# Подготовленные источники библиотек

Скрипты `steam_broad/scripts/build_top1000.py`, `steam_alltime/scripts/build_steam_alltime.py` и `google_play/scripts/build_google_play_top1000.py` скопированы без изменений из ZIP пользователя в корне проекта.

Запуск из корня проекта:

```powershell
python tools/library_sources/steam_broad/scripts/build_top1000.py
python tools/library_sources/steam_alltime/scripts/build_steam_alltime.py
python tools/library_sources/google_play/scripts/download_source.py
python tools/library_sources/google_play/scripts/build_google_play_top1000.py
```

После подготовки скопировать `data/games_1000.json` каждого набора в `app/shared/src/commonMain/composeResources/files/libraries/steam_broad.json` и `steam_alltime.json` соответственно.

Google Play: скопировать `google_play/data/games_1000.json` в `app/shared/src/commonMain/composeResources/files/libraries/google_play.json`. `download_source.py` скачивает публичный архив Kaggle напрямую без установки Python-зависимостей и извлекает только `Google-Playstore.csv` в фиксированный путь. Исходный датасет большой; в приложение включается только JSON отобранных игр.

Google Play использует исторический срез `gauthamp10/google-playstore-apps` за июнь 2021: игровые категории, дедупликация по App Id, сортировка по Maximum Installs / Rating Count / Rating. Это не текущий официальный рейтинг Google Play. Механики выведены генератором из категорий; визуальные признаки не выдумываются.

Снимок подготовлен 25 сентября 2026. Steam Broad: 1000 записей, 986 с исходными механиками, источник `https://raw.githubusercontent.com/preethichelli/GameSense/main/steam_games_2026.csv`. Steam All-Time: 1000 записей из `https://steamspy.com/api.php?request=all&page=0`; ответ не содержит тегов. Эти записи доступны для поиска, но не дают автоматически придуманных механик.

Ранжирование и качество данных наследуются от источников; это не проверенный актуальный рейтинг продаж. В приложении пропускаются значения `unspecified` и `infer from ...`. Изображения не скачивались.
