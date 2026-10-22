package com.restor.gamereel.data.importer

import com.restor.gamereel.model.*
import com.restor.gamereel.text.fail
import gamereel.app.shared.generated.resources.*
import kotlinx.serialization.json.*

interface LibraryImporter {
    fun parse(text: String): ImportResult
}

class JsonLibraryImporter : LibraryImporter {
    companion object {
        const val MAX_MEGABYTES = 30
        const val MAX_CHARS = MAX_MEGABYTES * 1_000_000
        const val MAX_GAMES = ContentLimits.MAX_GAMES
    }

    override fun parse(text: String): ImportResult {
        if (text.length > MAX_CHARS) fail(Res.string.import_too_large, MAX_MEGABYTES)

        val root =
            try {
                Json.parseToJsonElement(text.removePrefix("\uFEFF")).jsonObject
            } catch (_: Exception) {
                fail(Res.string.import_bad_json)
            }
        val canonical = root["library"] as? JsonObject
        val rows = root["games"] as? JsonArray
            ?: if (canonical != null) JsonArray(emptyList()) else fail(Res.string.import_no_games)
        if (rows.size > MAX_GAMES) fail(Res.string.import_too_many_games, MAX_GAMES)

        val first = rows.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
        val legacyId =
            first.string("library").ifBlank {
                if (first.string("app_id").isNotBlank()) LibraryIds.GOOGLE_PLAY
                else LibraryIds.STEAM_BROAD
            }
        val id = canonical?.string("id") ?: legacyId
        if (!id.matches(Regex("[A-Za-z0-9_.-]{1,100}")) || id == LibraryIds.FIXTURE)
            fail(Res.string.import_bad_id)
        val name = canonical?.string("name") ?: root.string("dataset").ifBlank { "Steam Broad" }
        if (name.isBlank()) fail(Res.string.import_no_name)
        val version =
            canonical?.get("version")?.jsonPrimitive?.intOrNull
                ?: root["annotation_version"]?.jsonPrimitive?.intOrNull
                ?: 1
        if (version <= 0) fail(Res.string.import_bad_version)

        val slots = parseSlots(id, canonical)
        val reels = (canonical?.get("reels") as? JsonArray).orEmpty().map { element ->
            val local = (element as? JsonPrimitive)?.contentOrNull.orEmpty()
            slots.firstOrNull { it.id == "$id:slot:$local" }?.id ?: fail(Res.string.import_unknown_slot, local)
        }
        if (reels.size > ContentLimits.MAX_REELS) fail(Res.string.error_reel_limit)

        val slotTags = mutableMapOf<String, Tag>()
        val slotRequires = mutableMapOf<String, MutableSet<String>>()
        val standaloneTags =
            (canonical?.get("standaloneTags") as? JsonArray)
                .orEmpty()
                .mapNotNull { atom ->
                    val tag = atom as? JsonObject ?: fail(Res.string.import_bad_tag)
                    val label = tag.string("name").trim()
                    if (label.isBlank()) fail(Res.string.import_empty_tag_name)
                    val slotName = tag.string("slot")
                    if (slotName.isNotBlank()) {
                        val slot = "$id:slot:$slotName"
                        val declared = slots.firstOrNull { it.id == slot } ?: fail(Res.string.import_unknown_slot, slotName)
                        val normalized = label.lowercase().replace(Regex("\\s+"), " ")
                        val tagId = "$slot:${identity(normalized)}"
                        slotRequires.getOrPut(tagId) { mutableSetOf() } += tag.strings("requires")
                        if (tagId !in slotTags) slotTags[tagId] = Tag(tagId, label, Category.OTHER, id,
                            description = tag.string("description"),
                            icon = tag.string("icon").ifBlank { declared.icon }, slot = slot)
                        return@mapNotNull null
                    }
                    val category =
                        Category.entries.find { it.name == tag.string("category") }
                            ?: fail(Res.string.import_unknown_tag_category)
                    val normalized = label.lowercase().replace(Regex("\\s+"), " ")
                    Tag(
                        "$id:standalone:${category.name}:${identity(normalized)}",
                        label,
                        category,
                        id,
                        description = tag.string("description"),
                        icon = tag.string("icon"),
                    )
                }
                .distinctBy { it.id } + resolveRequires(slots, slotTags, slotRequires)
        if (rows.isEmpty() && standaloneTags.isEmpty()) fail(Res.string.import_no_content)
        if (standaloneTags.size > ContentLimits.MAX_TAGS)
            fail(Res.string.import_too_many_tags, ContentLimits.MAX_TAGS)

        val warnings = mutableSetOf<ImportWarning>()
        val seenIds = mutableSetOf<String>()
        val games = rows.mapIndexed { index, element ->
            val row = element as? JsonObject ?: fail(Res.string.import_game_not_object, index + 1)
            val externalId =
                row.string("externalId")
                    .ifBlank { row.string("appid") }
                    .ifBlank { row.string("app_id") }
                    .ifBlank { row.string("id") }
            if (externalId.isBlank()) fail(Res.string.import_game_no_id, index + 1)
            if (!seenIds.add(externalId)) fail(Res.string.import_duplicate_id, externalId)
            val title = row.string("title")
            if (title.isBlank()) fail(Res.string.import_game_no_title, externalId)

            // id from source ids only, never rank or title
            val gameId = "$id:${identity(externalId)}"
            val tags = mutableListOf<Tag>()

            fun add(
                label: String,
                category: Category,
                description: String = "",
                icon: String = "",
            ) {
                val clean = label.trim()
                if (
                    clean.isBlank() ||
                        clean.contains("infer from", true) ||
                        clean.startsWith("unspecified", true)
                )
                    return
                val normalized = clean.lowercase().replace(Regex("\\s+"), " ")
                val tagId = "$gameId:${category.name}:${identity(normalized)}"
                if (tags.none { it.id == tagId })
                    tags += Tag(tagId, clean, category, id, gameId, description, icon)
            }

            if (canonical != null) {
                val atoms = row["tags"] as? JsonArray ?: fail(Res.string.import_game_no_tags, title)
                atoms.forEach { atom ->
                    val tag = atom as? JsonObject ?: fail(Res.string.import_game_bad_tag, title)
                    val categoryName = tag.string("category")
                    val category =
                        Category.entries.find { it.name == categoryName }
                            ?: fail(Res.string.import_game_unknown_category, title, categoryName)
                    if (tag.string("name").isBlank()) fail(Res.string.import_game_empty_tag, title)
                    add(tag.string("name"), category, tag.string("description"), tag.string("icon"))
                }
            } else {
                row.strings("core_mechanics").forEach {
                    add(it, if (version >= 4) Category.CORE else legacyCategory(it, Category.CORE))
                }
                row.strings("extra_mechanics").forEach {
                    add(
                        it,
                        if (version >= 4) Category.SECONDARY_MECHANIC
                        else legacyCategory(it, Category.SECONDARY_MECHANIC),
                    )
                }
                enrichedFields.forEach { (field, category) ->
                    row.strings(field).forEach { add(it, category) }
                }
                add(row.string("visual_style"), Category.VISUAL)
                add(row.string("camera"), Category.CAMERA)
                add(row.string("player_mode"), Category.MULTIPLAYER)
                add(row.string("production_scope_estimate"), Category.SCOPE)
                row.strings("mobile_business_tags").forEach { add(it, Category.MONETIZATION) }
                row.strings(if ("other_tags" in row) "other_tags" else "steam_tags").forEach {
                    add(it, Category.OTHER)
                }
            }

            if (tags.isEmpty()) warnings += ImportWarning.UNTAGGED_GAMES

            val image =
                row.string("header_image_url")
                    .ifBlank { row.string("image_url") }
                    .ifBlank { row.string("image") }
                    .ifBlank { row.string("local_image") }
                    .ifBlank { null }
            if (image != null && !image.startsWith("https://"))
                warnings += ImportWarning.LOCAL_IMAGES

            if (tags.size > ContentLimits.MAX_TAGS)
                fail(Res.string.import_too_many_tags, ContentLimits.MAX_TAGS)
            GameReference(
                gameId,
                externalId,
                id,
                title,
                tags,
                image = image?.takeIf { it.startsWith("https://") },
                storeUrl =
                    row.string("store_url")
                        .ifBlank { row.string("steam_url") }
                        .takeIf { it.startsWith("https://") },
            )
        }

        return ImportResult(
            LibraryPack(
                Library(
                    id,
                    name,
                    version,
                    root.string("source_url").ifBlank { root.string("source") },
                    standaloneTags,
                    slots,
                    reels,
                ),
                games,
            ),
            warnings.toList(),
        )
    }

    private fun parseSlots(id: String, canonical: JsonObject?): List<ReelSlot> {
        val rows = (canonical?.get("slots") as? JsonArray).orEmpty()
        if (rows.size > 50) fail(Res.string.import_bad_slot)
        val slots = rows.map { element ->
            val row = element as? JsonObject ?: fail(Res.string.import_bad_slot)
            val local = row.string("id")
            val name = row.string("name").trim()
            if (!local.matches(Regex("[A-Za-z0-9_.-]{1,60}")) || name.isBlank()) fail(Res.string.import_bad_slot)
            ReelSlot(
                "$id:slot:$local",
                name.take(60),
                row.string("icon").take(16),
                row.string("parent").ifBlank { null }?.let { "$id:slot:$it" },
                (row["unique"] as? JsonPrimitive)?.booleanOrNull ?: true,
            )
        }
        val ids = slots.map { it.id }
        if (ids.size != ids.toSet().size) fail(Res.string.import_bad_slot)
        slots.forEach { slot ->
            var parent = slot.parent
            var steps = 0
            while (parent != null) {
                if (parent !in ids || ++steps > slots.size) fail(Res.string.import_bad_slot)
                parent = slots.first { it.id == parent }.parent
            }
        }
        return slots
    }

    // requires lists parent tag names; a tag whose parents are all unknown is dropped
    private fun resolveRequires(
        slots: List<ReelSlot>,
        tags: Map<String, Tag>,
        requires: Map<String, Set<String>>,
    ): List<Tag> {
        val idsByName = tags.values.groupBy { it.slot }.mapValues { (_, group) ->
            group.associate { it.name.trim().lowercase() to it.id }
        }
        return tags.values.mapNotNull { tag ->
            val names = requires[tag.id].orEmpty()
            val parent = slots.first { it.id == tag.slot }.parent
            if (names.isEmpty() || parent == null) return@mapNotNull tag
            val ids = names.mapNotNull { idsByName[parent]?.get(it.trim().lowercase()) }
            if (ids.isEmpty()) null else tag.copy(requires = ids.distinct())
        }
    }

    private fun JsonObject.string(key: String) =
        (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private val enrichedFields =
        mapOf(
            "structure_tags" to Category.STRUCTURE,
            "progression_tags" to Category.PROGRESSION,
            "pressure_tags" to Category.PRESSURE,
            "interaction_tags" to Category.INTERACTION,
            "setting_tags" to Category.SETTING,
            "theme_tags" to Category.THEME,
            "visual_tags" to Category.VISUAL,
            "camera_tags" to Category.CAMERA,
            "player_tags" to Category.MULTIPLAYER,
            "world_tags" to Category.WORLD,
            "location_tags" to Category.LOCATION,
            "environment_tags" to Category.ENVIRONMENT,
            "twist_tags" to Category.TWIST,
        )

    private fun JsonObject.strings(key: String) =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    private fun identity(value: String) =
        value.encodeToByteArray().joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }

    private fun legacyCategory(label: String, fallback: Category): Category =
        when (label.trim().lowercase()) {
            "multiplayer",
            "singleplayer",
            "co-op",
            "pvp",
            "pve",
            "massively multiplayer" -> Category.MULTIPLAYER
            "sandbox",
            "run-based progression",
            "extraction" -> Category.STRUCTURE
            "character customization",
            "rpg progression",
            "character growth",
            "idle progression",
            "incremental progression" -> Category.PROGRESSION
            else -> fallback
        }
}
