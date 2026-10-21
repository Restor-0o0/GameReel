package com.restor.gamereel.data.storage

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.storage.*
import com.restor.gamereel.model.*
import kotlin.test.*
import kotlinx.serialization.encodeToString

class BrowserPersistenceTest {
    @Test
    fun legacyDataMigratesTogetherAndFailedWritesLeaveItUntouched() {
        val before = BuiltinCatalog.initialWorkspace()
        val original = LibraryPack(Library("mine", "Original"), emptyList())
        val other = LibraryPack(Library("other", "Other"), emptyList())
        var storage = workspaceJson.encodeToString(before)
        val legacyLibraries = workspaceJson.encodeToString(listOf(original, other))
        var fail = true
        var writes = 0
        fun open() = BrowserWorkspaceRepository({ storage }, {
            check(!fail) { "Quota exceeded" }
            writes++
            storage = it
        }, { legacyLibraries })
        val repository = open()
        val after = before.copy(nextId = 20)
        val replacement = original.copy(library = original.library.copy(name = "Replacement"))
        assertFails { repository.saveWithLibraries(after, listOf(replacement)) }
        assertEquals(before, open().load())
        assertEquals(listOf(original, other), open().loadLibraries())
        fail = false
        repository.saveWithLibraries(after, listOf(replacement))
        assertEquals(1, writes)
        assertEquals(after, open().load())
        assertEquals(setOf(replacement, other), open().loadLibraries().toSet())
    }

    @Test
    fun reservedUserImportIsRejectedBeforeChangingStorageOrBundledContent() {
        var storage: String? = null
        val repository = BrowserWorkspaceRepository({ storage }, { storage = it })
        val bundled = LibraryPack(Library(LibraryIds.CURATED, "Bundled"), emptyList())
        repository.importBundledLibrary(bundled)
        assertFails { repository.importLibrary(bundled.copy(library = bundled.library.copy(name = "User"))) }
        assertEquals(listOf(bundled), repository.loadLibraries())
        assertNull(storage)
    }

    @Test
    fun browserSnapshotSurvivesRepositoryRecreation() {
        var storage: String? = null
        val first = BrowserWorkspaceRepository({ storage }, { storage = it })
        assertNull(first.load())
        first.save(BuiltinCatalog.initialWorkspace())
        val reopened = BrowserWorkspaceRepository({ storage }, { storage = it })
        assertEquals(BuiltinCatalog.initialWorkspace(), reopened.load())
    }

    @Test
    fun corruptDataIsReportedWithoutOverwritingIt() {
        var storage = "broken"
        val repository = BrowserWorkspaceRepository({ storage }, { storage = it })
        assertFails { repository.load() }
        assertEquals("broken", storage)
    }

    @Test
    fun bundledLibrariesStayInMemoryAndImportsArePersisted() {
        var storage: String? = null
        val repository = BrowserWorkspaceRepository({ storage }, { storage = it })
        repository.importBundledLibrary(LibraryPack(Library(LibraryIds.CURATED, "curated"), emptyList()))
        repository.importLibrary(LibraryPack(Library("mine", "mine"), emptyList()))
        assertEquals(
            setOf(LibraryIds.CURATED, "mine"),
            repository.loadLibraries().map { it.library.id }.toSet(),
        )
        val reopened = BrowserWorkspaceRepository({ storage }, { storage = it })
        assertEquals(listOf("mine"), reopened.loadLibraries().map { it.library.id })
    }
}
