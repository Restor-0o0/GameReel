package com.restor.gamereel.feature.library

import com.restor.gamereel.model.ContentLimits
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.ui.presentation.isRemoteImage
import gamereel.app.shared.generated.resources.*
import kotlinx.serialization.json.*
import org.jetbrains.compose.resources.StringResource
import kotlin.random.Random

internal data class SlotDraft(
    val key: Int,
    val name: String,
    val icon: String = "",
    val parent: Int? = null,
    val count: Int = 1,
    val unique: Boolean = true,
    val id: String = "",
)

internal data class TagDraft(
    val key: Int,
    val slot: Int,
    val name: String,
    val image: String = "",
    val requires: Set<String> = emptySet(),
    val description: String = "",
)

internal data class PackDraft(
    val name: String = "",
    val slots: List<SlotDraft> = listOf(SlotDraft(0, "")),
    val tags: List<TagDraft> = emptyList(),
    val id: String = "",
    val version: Int = 1,
    val source: String = "",
    val seed: String = Random.nextInt().toUInt().toString(16).padStart(8, '0').take(6),
) {
    val nextKey get() = (slots.map { it.key } + tags.map { it.key }).maxOrNull()?.plus(1) ?: 0
    val reelCount get() = slots.sumOf { it.count }

    fun tagsOf(slot: Int) = tags.filter { it.slot == slot }

    fun withTags(slot: Int, names: List<String>): PackDraft {
        val known = tagsOf(slot).map { it.name.lowercase() }.toMutableSet()
        var key = nextKey
        val added = names.map { it.trim().take(120) }.filter { it.isNotEmpty() && known.add(it.lowercase()) }
            .map { TagDraft(key++, slot, it) }
        return copy(tags = (tags + added).take(ContentLimits.MAX_TAGS))
    }

    fun withoutSlot(key: Int) = copy(
        slots = slots.filterNot { it.key == key }.map { if (it.parent == key) it.copy(parent = null) else it },
        tags = tags.filterNot { it.slot == key },
    )

    fun problem(): Pair<StringResource, String>? {
        if (name.isBlank()) return Res.string.pack_editor_need_name to ""
        if (slots.any { it.name.isBlank() }) return Res.string.pack_editor_need_slot_name to ""
        if (reelCount == 0) return Res.string.pack_editor_need_reel to ""
        slots.firstOrNull { it.count > 0 && tagsOf(it.key).isEmpty() }
            ?.let { return Res.string.pack_editor_empty_slot to it.name.trim() }
        tags.firstOrNull { it.image.isNotBlank() && !isRemoteImage(it.image.trim()) }
            ?.let { return Res.string.pack_editor_bad_image to it.name }
        return null
    }

    fun libraryId() = id.ifBlank { (slug(name).take(40).ifBlank { "pack" }) + "-" + seed }

    fun toJson(): String {
        val ids = mutableMapOf<Int, String>()
        slots.sortedBy { it.id.isBlank() }.forEach { slot ->
            val wanted = slot.id.ifBlank { slug(slot.name).take(40) }
            ids[slot.key] = if (wanted.isEmpty() || wanted in ids.values) "slot-${slot.key}" else wanted
        }
        val names = tags.groupBy { it.slot }.mapValues { (_, group) -> group.map { it.name.lowercase() }.toSet() }
        val library = buildJsonObject {
            put("id", libraryId())
            put("name", name.trim().take(80))
            put("version", version)
            putJsonArray("slots") {
                slots.forEach { slot ->
                    addJsonObject {
                        put("id", ids.getValue(slot.key))
                        put("name", slot.name.trim().take(60))
                        if (slot.icon.isNotBlank()) put("icon", slot.icon.trim().take(16))
                        slot.parent?.let { put("parent", ids.getValue(it)) }
                        if (!slot.unique) put("unique", false)
                    }
                }
            }
            putJsonArray("reels") { slots.forEach { slot -> repeat(slot.count) { add(ids.getValue(slot.key)) } } }
            putJsonArray("standaloneTags") {
                tags.forEach { tag ->
                    val parent = slots.first { it.key == tag.slot }.parent
                    addJsonObject {
                        put("name", tag.name)
                        put("slot", ids.getValue(tag.slot))
                        val requires = tag.requires.filter { parent != null && it.lowercase() in names[parent].orEmpty() }
                        if (requires.isNotEmpty()) putJsonArray("requires") { requires.forEach { add(it) } }
                        if (tag.description.isNotBlank()) put("description", tag.description)
                        tag.image.trim().takeIf(::isRemoteImage)?.let { put("image", it) }
                    }
                }
            }
        }
        return Json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("library", library)
            if (source.startsWith("https://")) put("source_url", source)
        })
    }

    companion object {
        fun from(pack: LibraryPack): PackDraft {
            val library = pack.library
            val prefix = "${library.id}:slot:"
            val keys = library.slots.mapIndexed { index, slot -> slot.id to index }.toMap()
            val slots = library.slots.mapIndexed { index, slot ->
                SlotDraft(index, slot.name, slot.icon, slot.parent?.let(keys::get),
                    library.reels.count { it == slot.id }, slot.unique, slot.id.removePrefix(prefix))
            }
            val byId = library.standaloneTags.associateBy { it.id }
            var key = slots.size
            val tags = library.standaloneTags.mapNotNull { tag ->
                val slot = keys[tag.slot ?: return@mapNotNull null] ?: return@mapNotNull null
                TagDraft(key++, slot, tag.name, tag.image.orEmpty(),
                    tag.requires.mapNotNull { byId[it]?.name }.toSet(), tag.description)
            }
            return PackDraft(library.name, slots, tags, library.id, library.version + 1, library.source)
        }

        private fun slug(text: String) =
            text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }
}
