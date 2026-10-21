package com.restor.gamereel.data.personal

import com.restor.gamereel.model.*
import kotlin.random.Random

data class GameTagDraft(val name: String, val category: Category)

object PersonalGames {
    fun empty(): LibraryPack = LibraryPack(Library(LibraryIds.PERSONAL, "My games"), emptyList())

    fun save(
        current: LibraryPack?,
        editingId: String?,
        title: String,
        drafts: List<GameTagDraft>,
    ): LibraryPack {
        val pack = current ?: empty()
        require(pack.library.id == LibraryIds.PERSONAL)
        require(editingId != null || pack.games.size < 10_000)
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty() && cleanTitle.length <= 160)
        val normalizedDrafts = drafts.map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotEmpty() }
            .distinctBy { "${it.category}:${it.name.lowercase()}" }
        require(normalizedDrafts.isNotEmpty() && normalizedDrafts.size <= 100)
        require(normalizedDrafts.all { it.name.length <= 120 })

        val previous = editingId?.let { id -> pack.games.firstOrNull { it.id == id } }
        require(editingId == null || previous != null)
        val externalId = previous?.externalId ?: generateSequence(::randomId)
            .first { candidate -> pack.games.none { it.externalId == candidate } }
        val gameId = previous?.id ?: "${LibraryIds.PERSONAL}:${identity(externalId)}"
        val tags = normalizedDrafts.map { draft ->
            val key = draft.name.lowercase().replace(Regex("\\s+"), " ")
            Tag(
                id = "$gameId:${draft.category.name}:${identity(key)}",
                name = draft.name,
                category = draft.category,
                sourceLibraryId = LibraryIds.PERSONAL,
                sourceGameId = gameId,
            )
        }
        val game = GameReference(gameId, externalId, LibraryIds.PERSONAL, cleanTitle, tags)
        return pack.copy(games = if (previous == null) pack.games + game else
            pack.games.map { if (it.id == editingId) game else it })
    }

    fun delete(current: LibraryPack?, id: String): LibraryPack {
        val pack = current ?: empty()
        require(pack.library.id == LibraryIds.PERSONAL)
        return pack.copy(games = pack.games.filterNot { it.id == id })
    }

    fun merge(current: LibraryPack?, incoming: LibraryPack): LibraryPack {
        require(incoming.library.id == LibraryIds.PERSONAL)
        val pack = current ?: empty()
        require(pack.library.id == LibraryIds.PERSONAL)
        val games = (pack.games + incoming.games).associateBy { it.externalId }.values.toList()
        require(games.size <= 10_000)
        return pack.copy(games = games)
    }

    private fun randomId(): String = List(4) {
        Random.Default.nextInt().toUInt().toString(16).padStart(8, '0')
    }.joinToString("")

    private fun identity(value: String): String = value.encodeToByteArray().joinToString("") {
        (it.toInt() and 255).toString(16).padStart(2, '0')
    }
}
