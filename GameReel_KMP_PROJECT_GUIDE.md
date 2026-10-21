# GameReel — KMP Game Idea Generator

> Рабочее название проекта: **GameReel**

## 0. Что это за приложение

GameReel — local-first KMP-приложение для генерации идей игр из большой библиотеки референсов, механик, визуальных стилей, камер, структур, сеттингов и других атомов.

Основная метафора интерфейса — **однорукий бандит / набор барабанов**.

Пользователь может:

- крутить все барабаны сразу;
- перекручивать один конкретный барабан;
- замораживать понравившиеся результаты;
- безусловно закреплять нужные механики/теги;
- искать игру-референс;
- брать из конкретной игры только нужный аспект;
- добавлять собственные механики, графику, сеттинги и картинки;
- сохранять удачные комбинации как идеи проектов;
- использовать разные библиотеки:
  - Steam broad top 1000;
  - Steam all-time top 1000;
  - Google Play top 1000;
  - пользовательские библиотеки;
  - режим ALL.

Приложение не должно пытаться автоматически придумать полностью готовую игру.
Его задача — быстро выдавать комбинации, которые пользователь интерпретирует самостоятельно.

---

# 1. Название

Основное рабочее название:

## GameReel

Почему подходит:

- `Game` — сразу понятно, что приложение про игры;
- `Reel` — барабан слот-машины;
- название подходит и для desktop/mobile;
- не ограничивает продукт только Steam;
- не привязано к AI, поэтому приложение не устаревает концептуально.

Запасные варианты:

- IdeaReel
- GameMixer
- MechanicReel
- Spinforge
- GameForge
- IdeaForge
- GameRoulette
- MechanicMixer

Если название GameReel позже окажется занятым или неудобным для публикации, не менять внутреннюю архитектуру — имя продукта должно быть отделено от package/module naming.

---

# 2. Главная цель MVP

MVP должен позволять открыть приложение и за 10–20 секунд получить новую комбинацию идеи игры.

Пример:

- CORE: Physics destruction
- SECONDARY: Inventory management
- STRUCTURE: Short repeatable runs
- PRESSURE: Timer
- CAMERA: First person
- VISUAL: PS1 low-poly
- SETTING: Abandoned industrial complex
- MULTIPLAYER: Co-op 2–4
- TWIST: Every machine can physically break

Пользователь должен иметь возможность заморозить:

- Physics destruction
- First person

и перекрутить всё остальное.

---

# 3. Платформы

Использовать Kotlin Multiplatform.

Минимально:

- Android
- iOS

Архитектура должна позволять позже добавить:

- Desktop JVM
- Web/Wasm, если это станет практично

Не завязывать shared-код на Android SDK.

---

# 4. Предпочтительный стек

Использовать актуальные стабильные версии библиотек на момент реализации.

Предпочтительно:

- Kotlin Multiplatform
- Compose Multiplatform
- Coroutines
- Flow / StateFlow
- kotlinx.serialization
- Koin или ручной DI

Для локальной БД:

- SQLDelight

или:

- Room KMP, если его KMP-поддержка на момент реализации стабильна и удобнее.

Предпочтение отдавать решению с:

- нормальной миграцией схем;
- Android + iOS;
- простым bulk import тысяч записей;
- нормальной поддержкой offline-first.

Для изображений:

- Coil 3 / Compose Multiplatform image loading, если подходит;
- собственный abstraction layer для image source.

---

# 5. Главный принцип архитектуры

## Local-first

Сервер не требуется для основной работы приложения.

Встроенные библиотеки и импортированные данные хранятся локально.

Пользовательские данные должны храниться отдельно от импортируемых библиотек.

Это критично.

Обновление Steam/Google Play библиотеки НЕ должно удалять:

- пользовательские теги;
- пользовательские изображения;
- закреплённые элементы;
- сохранённые идеи;
- кастомные барабаны;
- пользовательские настройки.

---

# 6. Предлагаемая структура модулей

Пример:

```text
composeApp/
shared/
    core/
    data/
    database/
    domain/
    generator/
    libraries/
    import/
    settings/
    feature-home/
    feature-generator/
    feature-library/
    feature-reference/
    feature-saved/
    feature-settings/
```

Если проект небольшой, не дробить физически на десятки Gradle-модулей сразу.

Можно начать с feature/package архитектуры внутри shared и разделить на модули позже.

Главное — соблюдать границы ответственности.

---

# 7. Основные сущности

## GameReference

```kotlin
data class GameReference(
    val id: String,
    val externalId: String?,
    val source: LibrarySource,
    val title: String,
    val image: ImageRef?,
    val tags: List<TagRef>,
    val metadata: GameMetadata
)
```

---

## LibrarySource

```kotlin
enum class LibrarySource {
    STEAM_BROAD,
    STEAM_ALL_TIME,
    GOOGLE_PLAY,
    USER
}
```

В будущем источник не должен быть enum-only.

Лучше поддержать `sourceId: String`, чтобы пользователь мог импортировать новые библиотеки без обновления приложения.

---

## Tag

Тег — главный атом генератора.

```kotlin
data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val category: TagCategory,
    val image: ImageRef?,
    val sourceGameId: String?,
    val sourceLibraryId: String?,
    val isUserCreated: Boolean
)
```

---

# 8. Категории тегов / барабанов

Минимальный набор:

```text
CORE
SECONDARY_MECHANIC
STRUCTURE
PROGRESSION
PRESSURE
CAMERA
VISUAL
SETTING
MULTIPLAYER
INTERACTION
TWIST
SCOPE
MONETIZATION
PLATFORM_TRAIT
OTHER
```

Примеры:

### CORE

- shooting
- farming
- building
- physics manipulation
- driving
- deckbuilding
- cooking
- trading
- stealth
- survival

### SECONDARY_MECHANIC

- inventory management
- crafting
- resource logistics
- procedural generation
- physics
- climbing
- dialogue choices
- repair
- automation

### VISUAL

- low-poly
- PS1
- pixel art
- voxel
- hand-painted
- realistic
- stylized 3D
- minimalistic
- monochrome
- retro UI

### CAMERA

- first person
- third person
- top-down
- isometric
- side view
- fixed camera

### STRUCTURE

- endless
- run-based
- session-based
- campaign
- sandbox
- extraction
- day cycle
- job/contract based
- open world

### PRESSURE

- timer
- debt
- hunger
- limited inventory
- environmental danger
- increasing difficulty
- pursuit
- resource decay

### MULTIPLAYER

- solo
- co-op 2
- co-op 2–4
- co-op 4+
- PvP
- asynchronous
- party

---

# 9. Reel

Каждая строка генератора — Reel.

```kotlin
data class Reel(
    val id: String,
    val category: TagCategory,
    val title: String,
    val selectedTagId: String?,
    val state: ReelState,
    val sourceFilter: Set<String>,
    val customPool: Set<String>
)
```

Состояния:

```kotlin
enum class ReelState {
    RANDOM,
    FROZEN,
    PINNED
}
```

---

# 10. Важная разница Freeze и Pin

## Freeze

Пользователь получил случайный результат и решил его пока сохранить.

Например:

```text
🎰 CAMERA
First person

[Freeze]
```

После этого общий Spin его не меняет.

Но пользователь может легко снять Freeze.

---

## Pin

Pin — безусловное закрепление.

Это сознательно выбранная пользователем часть идеи.

Пример:

Пользователь открыл Teardown.

Увидел:

```text
Physics destruction
Voxel
First person
Sandbox
```

Нажал:

```text
+ Pin Physics destruction
```

После этого слева появляется:

```text
PINNED

🔒 Physics destruction
   Teardown
```

Pin:

- не изменяется обычным Spin;
- не снимается через обычный Unlock all;
- требует отдельного Unpin;
- визуально должен отличаться от Freeze;
- всегда располагается перед случайными барабанами или в отдельной pinned-зоне.

---

# 11. Безусловное добавление тегов из игры

Это обязательный UX.

Экран игры должен показывать теги по категориям.

Например:

```text
TEARDOWN

CORE
[Physics destruction] [+]

CAMERA
[First person] [+]

VISUAL
[Voxel] [+]

STRUCTURE
[Sandbox] [+]

OTHER TAGS
[Physics] [+]
[Destruction] [+]
[Heist] [+]
```

Нажатие `[+]`:

1. создаёт новый Reel;
2. ставит нужный Tag;
3. помечает Reel как PINNED;
4. добавляет его в левую pinned-часть генератора;
5. сохраняет source game.

Можно выбрать несколько тегов и нажать:

```text
Pin selected
```

---

# 12. Поведение дубликатов

Если пользователь пытается Pin тег, который уже закреплён:

не создавать дубль автоматически.

Показать:

```text
Physics destruction is already pinned.
```

Можно разрешить:

```text
Add duplicate anyway
```

только через дополнительное действие.

---

# 13. Главный экран Generator

Условно:

```text
┌──────────────────────────────────────┐
│ GameReel                             │
│ Library: ALL ▼                       │
├──────────────────────────────────────┤
│ PINNED                               │
│ 🔒 Physics destruction  [Teardown]   │
│ 🔒 PS1 visual           [Custom]     │
├──────────────────────────────────────┤
│ CORE                                 │
│ 🎰 Logistics                         │
│ [reroll] [freeze]                    │
├──────────────────────────────────────┤
│ STRUCTURE                            │
│ 🎰 Short runs                        │
│ [reroll] [freeze]                    │
├──────────────────────────────────────┤
│ SETTING                              │
│ 🎰 Underwater laboratory             │
│ [reroll] [freeze]                    │
├──────────────────────────────────────┤
│ TWIST                                │
│ 🎰 Player controls the enemy economy │
│ [reroll] [freeze]                    │
│                                      │
│        [ SPIN ]                      │
└──────────────────────────────────────┘
```

---

# 14. Управление барабанами

Каждый Reel должен поддерживать:

- reroll;
- freeze;
- unfreeze;
- remove;
- move up;
- move down;
- pin;
- unpin;
- change category;
- manually select value.

Общий экран:

- Spin all;
- Reroll unlocked;
- Unlock all frozen;
- Clear random;
- Clear all;
- Save idea.

Pinned элементы нельзя снимать через `Unlock all frozen`.

---

# 15. Добавление и удаление барабанов

Пользователь может создать сколько угодно Reel.

Пример:

```text
+ Add reel

CORE
CAMERA
VISUAL
SETTING
PRESSURE
CUSTOM
```

Можно иметь:

```text
CORE #1
CORE #2
```

если пользователь специально хочет два core-компонента.

Не запрещать повтор категорий.

---

# 16. Custom Reel

Пользователь может создать собственную категорию:

```text
NAME:
"Что ломается"

VALUES:
- машина
- оружие
- карта
- экономика
- управление
```

Custom Reel должен работать так же, как стандартный.

---

# 17. Режимы генерации

## Atomic Spin

Обычный режим.

Каждый барабан независимо берёт один Tag из своей категории.

---

## Reference Spin

Генератор выбирает 2–3 игры.

Пример:

```text
Teardown
Take: CORE

DREDGE
Take: STRUCTURE

PEAK
Take: VISUAL
```

У каждой карточки два независимых параметра:

1. Game
2. Aspect

Можно отдельно Freeze:

```text
Game = Teardown
Aspect = random
```

или:

```text
Game = random
Aspect = VISUAL
```

---

# 18. Extract from Reference

После Reference Spin пользователь может нажать:

```text
Extract
```

Например:

```text
Teardown / CORE
```

превращается в:

```text
Physics destruction
```

и добавляется обычным Reel.

Далее пользователь может:

- Freeze;
- Pin;
- удалить;
- заменить.

---

# 19. Hybrid Spin

Hybrid режим:

1. пользователь сначала получает несколько референсов;
2. часть элементов закрепляет;
3. остальное приложение достраивает Atomic reels.

Пример:

```text
PIN:
Physics destruction

PIN:
PS1 graphics

GENERATE:
core loop
pressure
setting
progression
multiplayer
```

---

# 20. Anti-copy rule

Приложение не должно специально собирать копию одной игры.

При генерации желательно ограничить количество автоматически выбранных признаков из одного reference game.

Например:

```text
maxAutoAtomsFromSameGame = 2
```

Pinned элементы этим правилом не ограничиваются.

Если пользователь сознательно Pin 5 элементов из одной игры — это разрешено.

---

# 21. Production scope

Добавить Reel:

```text
SCOPE
```

Пример значений:

- weekend prototype
- 1 month
- 3–4 months indie
- 6–12 months small team
- large project
- unrestricted

Это не должно запрещать механики.

Можно использовать как дополнительный фильтр веса.

---

# 22. Режимы веса

## Wild

Максимально случайные комбинации.

Совместимость почти не учитывается.

---

## Balanced

Чуть меньше бессмысленных сочетаний.

---

## Production Safe

Снижает вес:

- MMO;
- huge open worlds;
- photorealistic content heavy;
- massive PvP;
- сложного сетевого симулятора;
- контентно огромных жанров.

Важно:

НЕ удалять их полностью.

Пользователь может специально выбрать или Pin любой элемент.

---

# 23. Compatibility rules

Не делать hard-coded giant if/else.

Использовать:

```kotlin
data class CompatibilityRule(
    val tagA: String,
    val tagB: String,
    val weightMultiplier: Float
)
```

Пример:

```text
First person + pixel art
0.4

Physics destruction + voxel
1.5
```

Wild mode может игнорировать compatibility.

---

# 24. Libraries

В приложении есть Library Manager.

Пример:

```text
Libraries

☑ Steam Broad Top 1000
☑ Steam All-Time Top 1000
☑ Google Play Top 1000
☑ My Library
```

Для генерации можно включить несколько одновременно.

---

# 25. Формат импортируемой библиотеки

Поддержать JSON.

Минимальный рекомендуемый формат:

```json
{
  "library": {
    "id": "steam_all_time",
    "name": "Steam All-Time Top 1000",
    "version": 1
  },
  "games": [
    {
      "id": "steam_123",
      "externalId": "123",
      "title": "Example Game",
      "image": "images/123.jpg",
      "tags": [
        {
          "name": "First person",
          "category": "CAMERA"
        }
      ]
    }
  ]
}
```

Не связывать приложение жёстко с текущим конкретным JSON из архивов.

Сделать importer/adaptor.

---

# 26. Старые архивы

У пользователя уже есть наборы:

```text
Steam broad
Steam all-time
Google Play
```

Они должны использоваться как seed data.

Не переписывать данные вручную.

Сделать import pipeline.

Пример:

```text
Raw archive
      ↓
ArchiveParser
      ↓
Normalization
      ↓
ImportedLibrary
      ↓
Local DB
```

---

# 27. Импорт изображений

ImageRef:

```kotlin
sealed interface ImageRef {

    data class Local(
        val path: String
    ) : ImageRef

    data class Remote(
        val url: String
    ) : ImageRef

    data class Resource(
        val key: String
    ) : ImageRef
}
```

После импорта архив может содержать:

```text
images/
123.jpg
456.jpg
...
```

Сохранять изображения локально.

Не хранить Bitmap внутри SQL БД.

---

# 28. Добавление своих данных

Пользователь может создать Tag вручную:

```text
Name
Category
Image
Notes
```

Например:

```text
Name:
"Машины постоянно перегреваются"

Category:
PRESSURE

Image:
custom.png
```

---

# 29. Свои картинки

Позволить:

- выбрать фото/изображение;
- привязать к Tag;
- привязать к Reel;
- привязать к сохранённой идее.

На iOS/Android использовать platform-specific picker через expect/actual или интерфейс сервиса.

---

# 30. Поиск

Поиск должен работать:

### По игре

```text
Teardown
RimWorld
Balatro
```

### По тегу

```text
physics
building
low poly
deckbuilding
```

### По категории

```text
VISUAL
CAMERA
CORE
```

---

# 31. Search result

Если пользователь ищет:

```text
physics
```

показать:

```text
PHYSICS DESTRUCTION
Teardown
[+ Pin]

PHYSICS OBJECTS
Half-Life 2
[+ Pin]

PHYSICS CO-OP
Human Fall Flat
[+ Pin]
```

Это очень важный сценарий.

---

# 32. Game Details

Карточка игры:

```text
[IMAGE]

TEARDOWN

Steam All-Time

CORE
Physics destruction      [+]

SECONDARY
Heist                    [+]
Sandbox                  [+]

CAMERA
First person             [+]

VISUAL
Voxel                    [+]

[Pin selected]

[Use as Reference]
```

---

# 33. Saved Ideas

Пользователь может сохранить текущую комбинацию.

```kotlin
data class SavedIdea(
    val id: String,
    val title: String?,
    val createdAt: Instant,
    val reels: List<SavedReel>,
    val notes: String,
    val favorite: Boolean
)
```

---

# 34. Saved Idea screen

Пример:

```text
Factory horror #4

Physics destruction
Factory management
Co-op 2–4
First person
Low-poly
Night shift
Debt

Notes:
"Игроки работают ремонтной бригадой..."
```

Поддержать:

- Favorite;
- Rename;
- Notes;
- Duplicate;
- Load into generator;
- Delete;
- Export JSON;
- Share text.

---

# 35. History

Хранить последние Spin результаты.

Например последние:

```text
100
```

или настройка пользователя.

Позволить:

```text
Undo Spin
```

Это важно, потому что пользователь может случайно перекрутить понравившуюся идею.

---

# 36. Random engine

Random должен быть отдельным компонентом.

```kotlin
interface RandomProvider {
    fun nextInt(bound: Int): Int
    fun nextFloat(): Float
}
```

GeneratorEngine не должен напрямую использовать platform random.

Это позволит нормально тестировать.

---

# 37. GeneratorEngine

Пример API:

```kotlin
interface GeneratorEngine {

    suspend fun spin(
        state: GeneratorState,
        config: GeneratorConfig
    ): GeneratorState

    suspend fun reroll(
        reelId: String,
        state: GeneratorState,
        config: GeneratorConfig
    ): GeneratorState
}
```

---

# 38. Weighted random

Каждый Tag может иметь вес.

```kotlin
data class WeightedTag(
    val tag: Tag,
    val weight: Float
)
```

Учитывать:

- режим Wild/Balanced;
- compatibility;
- filters;
- scope;
- source weights;
- blacklist.

---

# 39. Не использовать AI в MVP

MVP должен нормально работать полностью без LLM.

Не делать генерацию зависимой от API.

В будущем AI можно добавить как optional feature:

```text
"Explain this idea"
"Turn into pitch"
"Generate 5 interpretations"
"Suggest missing mechanic"
```

Но ядро должно быть обычным deterministic/random генератором.

---

# 40. Home

Домашний экран:

```text
GameReel

[New Spin]

Continue
Factory horror #4

Libraries
Steam Broad
Steam All-Time
Google Play

Recent ideas
...
```

---

# 41. Navigation

Минимальные разделы:

```text
Generator
Search
Libraries
Saved
Settings
```

На телефоне:

Bottom Navigation.

На планшете/Desktop:

Navigation Rail.

---

# 42. Adaptive UI

Compose UI должен нормально работать:

- Android phone;
- Android tablet;
- iPhone;
- iPad.

Generator на широком экране:

```text
Pinned | Reels | Reference/Search
```

На телефоне:

вертикальный список.

---

# 43. Анимация Spin

Не нужна физическая сложная слот-машина для MVP.

Первая версия:

- короткая vertical scrolling animation;
- blur/opacity;
- финальный snap.

Важно:

анимация не должна задерживать результат надолго.

Настройка:

```text
Animations:
Off
Fast
Normal
```

---

# 44. Не блокировать UX анимацией

Если пользователь нажал Spin несколько раз:

- либо отменить предыдущую animation;
- либо сразу переключить результат.

Не строить queue на 5 секунд.

---

# 45. Filters

Поддержать фильтры:

```text
Sources
Steam
Google Play
Custom

Players
Solo
Co-op
PvP

Visual
2D
3D
Pixel
Low-poly

Scope
Small
Medium
Any
```

Фильтры не должны влиять на pinned элементы.

---

# 46. Blacklist

Пользователь может сказать:

```text
Never show:
MMO
Battle Royale
Realistic military
```

Сделать blacklist тегов.

Pinned вручную blacklist игнорирует.

---

# 47. Favorites

Пользователь может Favorite:

- Tag;
- Game;
- Idea.

Favorite Tag может иметь повышенный random weight при включённой настройке.

---

# 48. Notes

Tag может иметь пользовательские notes.

Например:

```text
Physics destruction

Note:
"Можно интерпретировать как разбор фабрики,
а не обязательно разрушение зданий."
```

Это важно для того, чтобы пользователь сам задавал интерпретации.

---

# 49. Data separation

Обязательно разделить:

```text
Imported Data
User Data
Generator State
```

Нельзя добавлять user modifications прямо в imported raw records.

Использовать overlays.

Пример:

```text
Imported Tag
   +
User Override
   =
Visible Tag
```

---

# 50. Обновление библиотеки

Новая версия Steam library может заменить:

```text
Steam All-Time v1
→
Steam All-Time v2
```

Но IDs должны позволять сохранить:

- favorite;
- notes;
- pinned history.

Сопоставлять прежде всего по external id.

---

# 51. Database tables

Минимально:

```text
libraries
games
tags
game_tags

user_tags
user_tag_overrides

saved_ideas
saved_idea_reels

favorites
blacklist

generator_presets
settings
```

---

# 52. Presets

Пользователь может сохранить расположение барабанов.

Например:

## Quick Indie

```text
CORE
SECONDARY
STRUCTURE
VISUAL
SETTING
TWIST
```

## Co-op Prototype

```text
CORE
INTERACTION
PRESSURE
MULTIPLAYER
CAMERA
VISUAL
TWIST
```

---

# 53. Default preset

При первом запуске:

```text
CORE
SECONDARY
STRUCTURE
PRESSURE
CAMERA
VISUAL
SETTING
TWIST
```

---

# 54. Drag & Drop

Позволить менять порядок Reel.

На touch:

long press + drag.

На Desktop позже:

mouse drag.

---

# 55. Export

Поддержать экспорт идеи как текст:

```text
Game idea

Core:
Physics destruction

Secondary:
Logistics

Structure:
Short runs

Visual:
PS1

Setting:
Factory
```

и JSON.

---

# 56. Import user pack

Позволить импортировать пользовательский pack:

```text
my_pack.zip
```

с:

```text
library.json
images/
```

Это позволит пользователю создавать тематические наборы:

- horror;
- cozy;
- factory;
- co-op;
- mobile;
- mechanics from old games.

---

# 57. Ошибки импорта

Importer должен выдавать понятные ошибки:

```text
Invalid JSON
Missing library.json
Unknown category
Missing image
Duplicate external ID
```

Не падать всем приложением.

---

# 58. Performance

База порядка:

```text
3000+ games
десятки тысяч tags
тысячи изображений
```

не является большой.

Не переоптимизировать.

Но:

- использовать DB индексы;
- lazy image loading;
- lazy lists;
- не загружать весь JSON в UI memory постоянно.

---

# 59. Тесты

Обязательные unit tests:

- frozen reel не меняется;
- pinned reel не меняется;
- unlock all не снимает pin;
- reroll меняет только выбранный reel;
- blacklist работает;
- filters работают;
- duplicates при Pin обрабатываются;
- deterministic random с fake RandomProvider;
- import сохраняет IDs;
- library update не удаляет user overlay.

---

# 60. MVP — этап 1

Сделать:

- KMP project;
- Android + iOS;
- local DB;
- встроенный маленький тестовый pack;
- Generator;
- Spin;
- Reroll;
- Freeze;
- Pin;
- Add Reel;
- Remove Reel;
- Saved Ideas.

Пока без красивой animation.

---

# 61. MVP — этап 2

Добавить:

- импорт настоящих архивов;
- Steam broad;
- Steam all-time;
- Google Play;
- изображения;
- Search;
- Game Details;
- Pin from Game.

---

# 62. Этап 3

Добавить:

- Reference Spin;
- Extract;
- Hybrid Spin;
- compatibility;
- weighted random;
- modes Wild/Balanced/Production Safe;
- custom packs.

---

# 63. Этап 4

Polish:

- animations;
- adaptive tablet UI;
- history;
- undo;
- presets;
- sharing;
- backup/export.

---

# 64. Что НЕ делать на старте

Не делать сразу:

- backend;
- account system;
- cloud sync;
- social network;
- генеративный AI;
- сложную авторизацию;
- магазин;
- подписки;
- online multiplayer;
- огромную design system.

Сначала генератор должен быть полезным локально.

---

# 65. Definition of Done MVP

MVP считается готовым, если пользователь может:

1. установить приложение;
2. открыть Generator;
3. нажать Spin;
4. получить 8 независимых компонентов;
5. Freeze часть из них;
6. перекрутить остальные;
7. открыть Search;
8. найти Teardown;
9. Pin `Physics destruction`;
10. вернуться в Generator;
11. увидеть его слева как постоянный locked Reel;
12. снова Spin;
13. сохранить получившуюся идею;
14. закрыть приложение;
15. открыть снова;
16. найти сохранённую идею на месте.

---

# 66. Инструкция агенту

Ты работаешь над KMP-приложением GameReel.

Не переписывай проект целиком без необходимости.

Перед крупным изменением:

1. изучи существующую структуру;
2. найди текущую реализацию;
3. определи минимальный набор файлов для изменения;
4. реализуй;
5. собери проект;
6. исправь ошибки;
7. запусти доступные тесты.

Не создавай заглушки вместо реальной реализации, если задача позволяет реализовать функционал нормально.

Не добавляй backend, Firebase, авторизацию или AI без отдельного запроса.

Shared business logic должна быть platform-independent.

Android/iOS-specific код держать за интерфейсами или expect/actual там, где это действительно нужно.

При реализации UI:

- учитывать phone/tablet;
- избегать Android-only API в shared UI;
- состояние хранить вне Composable;
- UI должен быть максимально stateless;
- state подаётся сверху;
- события уходят вверх.

---

# 67. Первая задача агенту

Начни проект с минимального вертикального среза.

Нужно реализовать:

```text
GameReel
↓
Generator screen
↓
8 default reels
↓
Spin
↓
Reroll one
↓
Freeze
↓
Pin
↓
Save idea
↓
Persistence
```

Создай небольшой встроенный fixture dataset на 30–50 Tag.

Пока НЕ импортируй 1000 игр.

Когда вертикальный срез заработает на Android и iOS, переходи к import pipeline для существующих архивов.

---

# 68. Вторая задача агенту

После MVP генератора реализуй библиотечную модель.

Нужно:

- Library entity;
- GameReference;
- Tag;
- GameTag;
- importer interface;
- JSON importer;
- image path resolver;
- Library Manager;
- Search.

После этого подключить первый существующий архив.

---

# 69. Третья задача агенту

Реализуй сценарий:

```text
Search
→ game
→ game details
→ tag
→ + Pin
→ Generator
```

Это один из ключевых сценариев приложения.

Считать его приоритетнее Reference Spin.

---

# 70. Четвёртая задача агенту

После Pin flow реализовать:

```text
Reference Spin
Hybrid Spin
Extract to Reel
```

---

# 71. UX-принцип

Приложение должно ощущаться как инструмент для быстрого брейншторма.

Каждое действие должно занимать 1–2 нажатия.

Не заставлять пользователя заполнять формы там, где это необязательно.

Главная ценность:

```text
Spin
→ увидел что-то интересное
→ Freeze/Pin
→ Spin
→ сохранил
```

---

# 72. Финальный принцип

GameReel не придумывает игру вместо пользователя.

Он создаёт неожиданные ограничения и сочетания, из которых пользователь сам строит концепт.

Поэтому:

- случайность важнее идеальной совместимости;
- пользовательский Pin важнее алгоритма;
- любые автоматически применяемые правила должны быть отключаемыми;
- пользователь всегда может вручную выбрать любой Tag;
- генератор не должен говорить, что комбинация «неправильная».
