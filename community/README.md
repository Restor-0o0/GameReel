# Каталог сообщества

Пресеты барабанов, наборы тегов и библиотеки игр, которые можно импортировать в GameReel.
Приложение работает офлайн и ничего отсюда не скачивает само: файл нужно скачать
и открыть через импорт.

| Папка | Что внутри | Где импортировать |
| --- | --- | --- |
| `presets/` | Набор категорий барабанов | Генератор → Пресеты → «+ Импорт JSON» |
| `tag-sets/` | Свой пул тегов, барабаны крутятся только по нему | «Наборы» → наборы тегов → «+ Импорт JSON» |
| `libraries/` | Игры с тегами и отдельные теги | «Наборы» → библиотеки → «+ Импорт JSON» |

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

---

# Community catalog

Reel presets, tag sets and game libraries for GameReel. Download a file and import it
in the app (the app stays offline). To contribute, add a `kebab-case.json` file next to the
examples, run `python tools/validate_community.py` and open a pull request.
