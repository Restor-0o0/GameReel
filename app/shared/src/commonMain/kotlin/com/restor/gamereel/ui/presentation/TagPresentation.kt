package com.restor.gamereel.ui.presentation

import androidx.compose.runtime.Composable
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

val Category.symbol: String
    get() =
        when (this) {
            Category.CORE -> "🎮"
            Category.SECONDARY_MECHANIC -> "⚙️"
            Category.STRUCTURE -> "🗺️"
            Category.PRESSURE -> "⏳"
            Category.CAMERA -> "🎥"
            Category.VISUAL -> "🎨"
            Category.SETTING -> "🌍"
            Category.TWIST -> "⚡"
            Category.PROGRESSION -> "📈"
            Category.MULTIPLAYER -> "👥"
            Category.INTERACTION -> "🤝"
            Category.SCOPE -> "📐"
            Category.MONETIZATION -> "💎"
            Category.PLATFORM_TRAIT -> "📱"
            Category.OTHER -> "✨"
            Category.THEME -> "🎭"
            Category.WORLD -> "🌐"
            Category.LOCATION -> "📍"
            Category.ENVIRONMENT -> "🏞️"
        }

private val fixtureIcons =
    mapOf(
        "destruction" to "💥",
        "farming" to "🌱",
        "building" to "🏗️",
        "deckbuilding" to "🃏",
        "stealth" to "🥷",
        "inventory" to "🎒",
        "crafting" to "🔨",
        "logistics" to "📦",
        "repair" to "🔧",
        "dialogue" to "💬",
        "runs" to "🔁",
        "sandbox" to "🏖️",
        "contracts" to "📋",
        "campaign" to "📖",
        "days" to "🌅",
        "timer" to "⏱️",
        "debt" to "💸",
        "hunger" to "🍎",
        "pursuit" to "🚨",
        "decay" to "🥀",
        "first" to "👁️",
        "third" to "🧍",
        "top" to "🛰️",
        "iso" to "🔷",
        "side" to "↔️",
        "ps1" to "👾",
        "pixel" to "🟪",
        "voxel" to "🧊",
        "painted" to "🖌️",
        "mono" to "◐",
        "factory" to "🏭",
        "ocean" to "🌊",
        "space" to "🚀",
        "village" to "🌲",
        "train" to "🚂",
        "break" to "💢",
        "enemy" to "♟️",
        "night" to "🌙",
        "memory" to "🗝️",
        "cost" to "⚖️",
    )

private val aliases =
    mapOf(
        "destruction" to "destruction",
        "building" to "building",
        "base building" to "building",
        "farming" to "farming",
        "deckbuilding" to "deckbuilding",
        "stealth" to "stealth",
        "inventory management" to "inventory",
        "crafting" to "crafting",
        "resource management" to "logistics",
        "repair" to "repair",
        "choice-driven narrative" to "dialogue",
        "sandbox" to "sandbox",
        "run-based progression" to "runs",
        "first-person" to "first",
        "third-person" to "third",
        "top-down" to "top",
        "isometric" to "iso",
        "side-view" to "side",
        "pixel art 2d" to "pixel",
        "voxel 3d" to "voxel",
        "hand-drawn 2d" to "painted",
    )
private val mechanicDetails =
    mapOf(
        "shooting" to
            ("🎯" to
                "Прицеливание, стрельба и выбор позиции образуют основной цикл. Подумай, как окружение и ресурсы меняют тактику."),
        "exploration" to
            ("🧭" to
                "Игрок исследует неизвестное пространство. Открытия, маршруты и ориентиры поддерживают любопытство."),
        "narrative" to
            ("📜" to
                "Продвижение раскрывает историю. Свяжи информацию о мире с действиями, а не только с текстом."),
        "survival" to
            ("🔥" to
                "Игрок поддерживает жизнь в опасной среде. Добыча ресурсов и подготовка конкурируют с исследованием."),
        "physics" to
            ("⚛️" to
                "Масса, импульс и столкновения позволяют решать задачи экспериментами с объектами."),
        "puzzles" to
            ("🧩" to
                "Игрок раскрывает правила и находит решение. Давай понятную обратную связь на эксперименты."),
        "management" to
            ("📊" to
                "Игрок распределяет ограниченные ресурсы и принимает решения, влияющие на работу системы."),
        "economy" to
            ("🪙" to
                "Производство, обмен и стоимость ресурсов образуют связанную систему. Меняя её, игрок достигает целей."),
        "melee combat" to
            ("⚔️" to "Ближний бой строится на дистанции, таймингах и чтении действий противника."),
        "combat" to
            ("🛡️" to
                "Противостояние требует выбора атаки, защиты и позиции. Сделай последствия действий читаемыми."),
        "turn-based combat" to
            ("♟️" to
                "Участники действуют по очереди. Планирование и порядок действий важнее скорости реакции."),
        "turn-based strategy" to
            ("♟️" to
                "Каждый ход меняет положение на карте и доступные ресурсы. Решения могут иметь отложенный эффект."),
        "city building" to
            ("🏙️" to
                "Игрок развивает город, связывая размещение зданий, потребности жителей и потоки ресурсов."),
        "driving" to
            ("🚗" to
                "Управление транспортом — часть испытания: маршрут, инерция, груз и дорожные условия."),
        "racing" to
            ("🏁" to
                "Игрок соревнуется в скорости прохождения маршрута. Риск, траектория и контроль транспорта определяют результат."),
        "life simulation" to
            ("🏠" to
                "Повседневные действия и отношения образуют взаимосвязанные потребности и долгосрочные цели."),
        "tower defense" to
            ("🏰" to
                "Игрок размещает защиту против волн угроз. Маршруты, ресурсы и сочетания обороны важнее прямого управления."),
        "dungeon crawling" to
            ("🗝️" to
                "Исследование опасных комнат чередуется с боями, находками и решением, когда вернуться."),
        "cooking" to
            ("🍳" to
                "Подготовка ингредиентов и последовательность операций создают игровой процесс приготовления еды."),
        "fishing" to
            ("🎣" to
                "Выбор места, приманки и момента ловли соединяет исследование с управлением риском."),
        "automation" to
            ("🤖" to
                "Игрок проектирует системы, которые выполняют работу сами. Узкие места становятся следующей задачей."),
        "platforming" to
            ("🦘" to
                "Движение и прыжки через препятствия задают испытание. Читаемые расстояния помогают оценивать риск."),
        "co-op" to
            ("🤝" to
                "Игроки помогают друг другу. Раздели роли или информацию так, чтобы сотрудничество меняло способы решения задач."),
        "singleplayer" to
            ("🧑" to
                "Сценарий рассчитан на одного игрока. Все ключевые решения и действия должны быть доступны самостоятельно."),
        "multiplayer" to
            ("👥" to
                "Несколько игроков действуют в одном игровом процессе. Определи, как их действия влияют друг на друга."),
        "pvp" to
            ("⚔️" to
                "Игроки противостоят друг другу. Читаемые правила и возможности ответного действия особенно важны."),
        "pve" to
            ("🐉" to
                "Главные угрозы создаёт игровой мир. Игрок преодолевает поведение противников и условия окружения."),
    )
private val Tag.fixtureKey: String?
    get() = if (sourceLibraryId == LibraryIds.FIXTURE) id.substringAfterLast(':') else null

private val Tag.aliasKey: String?
    get() = fixtureKey ?: aliases[name.trim().lowercase()]

val Tag.displayIcon: String
    get() = icon.ifBlank {
        aliasKey?.let(fixtureIcons::get)
            ?: mechanicDetails[name.lowercase()]?.first
            ?: category.symbol
    }

// data-only text, fixture tags get theirs from resources
val Tag.explanation: String
    get() = description.ifBlank {
        if (fixtureKey != null) ""
        else
            tagDescriptions[name.trim().lowercase()]
                ?: mechanicDetails[name.lowercase()]?.second
                ?: curatedDescriptions["${category.name}:${name.trim().lowercase()}"].orEmpty()
    }

val Tag.referenceGames: List<String>
    get() =
        if (fixtureKey != null) emptyList()
        else tagReferences["${category.name}:${name.trim().lowercase()}"]?.split('\n').orEmpty()

internal fun fixtureName(key: String) = Res.allStringResources["fixture_$key"]

internal fun fixtureAbout(key: String) = Res.allStringResources["fixture_${key}_about"]

val Tag.shortName: String
    get() =
        tagShortNames[name.trim().lowercase()] ?: name.trim().replaceFirstChar { it.uppercase() }

@Composable
fun Tag.label(): String {
    return fixtureKey?.let(::fixtureName)?.let { stringResource(it) }
        ?: localizedShortName(stringResource(Res.string.language_code))
}

suspend fun Tag.loadLabel(environment: ResourceEnvironment): String =
    fixtureKey?.let(::fixtureName)?.let { getString(environment, it) }
        ?: localizedShortName(getString(environment, Res.string.language_code))

private fun Tag.localizedShortName(language: String): String {
    if (language != "ru") return shortName
    val russian =
        explanation
            .substringBefore(':')
            .substringBefore('.')
            .trim()
            .takeIf { candidate ->
                candidate.length in 2..120 &&
                    candidate.any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
            }
    return russian ?: shortName
}

// raw name when the label hides it, null otherwise
val Tag.fullName: String?
    get() = name.trim().takeIf { fixtureKey == null && !it.equals(shortName, ignoreCase = true) }

@Composable
fun Tag.about(): String {
    val text = explanation
    val about = if (text.isBlank()) aliasKey?.let(::fixtureAbout) else null
    return about?.let { stringResource(it) } ?: text
}

@Composable
fun referenceLabel(games: List<String>): String? =
    if (games.isEmpty()) null
    else pluralStringResource(Res.plurals.references, games.size, games.joinToString(", "))

fun gameTitle(id: String?, gameNames: Map<String, String>): String? = id?.let {
    gameNames[it] ?: BuiltinCatalog.gameNames[it]
}

val Category.title: StringResource
    get() =
        when (this) {
            Category.CORE -> Res.string.category_core
            Category.SECONDARY_MECHANIC -> Res.string.category_secondary_mechanic
            Category.STRUCTURE -> Res.string.category_structure
            Category.PRESSURE -> Res.string.category_pressure
            Category.CAMERA -> Res.string.category_camera
            Category.VISUAL -> Res.string.category_visual
            Category.SETTING -> Res.string.category_setting
            Category.TWIST -> Res.string.category_twist
            Category.PROGRESSION -> Res.string.category_progression
            Category.MULTIPLAYER -> Res.string.category_multiplayer
            Category.INTERACTION -> Res.string.category_interaction
            Category.SCOPE -> Res.string.category_scope
            Category.MONETIZATION -> Res.string.category_monetization
            Category.PLATFORM_TRAIT -> Res.string.category_platform_trait
            Category.OTHER -> Res.string.category_other
            Category.THEME -> Res.string.category_theme
            Category.WORLD -> Res.string.category_world
            Category.LOCATION -> Res.string.category_location
            Category.ENVIRONMENT -> Res.string.category_environment
        }

val AnimationSpeed.title: StringResource
    get() =
        when (this) {
            AnimationSpeed.OFF -> Res.string.speed_off
            AnimationSpeed.FAST -> Res.string.speed_fast
            AnimationSpeed.NORMAL -> Res.string.speed_normal
        }

val IdeaStage.title: StringResource
    get() = when (this) {
        IdeaStage.IDEA -> Res.string.stage_idea
        IdeaStage.PROTOTYPE -> Res.string.stage_prototype
        IdeaStage.JAM -> Res.string.stage_jam
        IdeaStage.RELEASED -> Res.string.stage_released
        IdeaStage.SHELVED -> Res.string.stage_shelved
    }

val GeneratorView.title: StringResource
    get() =
        when (this) {
            GeneratorView.CARDS -> Res.string.view_cards
            GeneratorView.MACHINE -> Res.string.view_machine
        }

@Composable
fun LibraryPack.displayName(): String =
    when (library.id) {
        LibraryIds.CURATED -> stringResource(Res.string.library_curated_name)
        LibraryIds.GOOGLE_PLAY -> stringResource(Res.string.library_google_name)
        LibraryIds.PERSONAL -> stringResource(Res.string.personal_games_title)
        else -> library.name
    }
