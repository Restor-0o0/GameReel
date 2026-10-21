# Google Play Top 1000 — методика

У Google Play нет единого официального публичного глобального рейтинга «1000 лучших игр за всё время». Текущие чарты зависят от страны, категории и типа чарта.

Поэтому для генератора используется воспроизводимый **all-time-like popularity snapshot**:

1. большой публичный датасет `gauthamp10/google-playstore-apps` (2.3M+ приложений, сбор June 2021);
2. оставляются только игровые категории Google Play;
3. игры дедуплицируются по `App Id`;
4. сортировка: `Maximum Installs` ↓, затем `Rating Count` ↓, затем `Rating` ↓;
5. берутся первые 1000.

Это не «официальный Google Top 1000», а исторический срез самых массовых игр по накопленным установкам.

## Игровые категории

Action, Adventure, Arcade, Board, Card, Casino, Casual, Educational, Music, Puzzle, Racing, Role Playing, Simulation, Sports, Strategy, Trivia, Word.

## Дополнительные барабаны

В мобильной библиотеке полезны отдельные атомы:

- free-to-play / premium;
- ads;
- in-app purchases;
- Editors Choice;
- mobile genre.

Графику лучше брать непосредственно с reference image: Google Play не даёт настолько богатых визуальных community-tags, как Steam.
