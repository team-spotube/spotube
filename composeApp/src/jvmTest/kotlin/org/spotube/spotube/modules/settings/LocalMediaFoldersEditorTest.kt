/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.use
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.settings.components.LocalMediaFoldersEditorContent
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.theme.MillenniumTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_local_media_folders_title
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocalMediaFoldersEditorTest {
    private val models = mutableListOf<LocalMediaFoldersViewModel>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanup() {
        models.forEach { it.viewModelScope.cancel() }
        models.clear()
        Dispatchers.resetMain()
    }

    private fun model(
        source: MutableStateFlow<UserSettings>,
        persist: suspend (List<String>) -> Unit = {}
    ) =
        LocalMediaFoldersViewModel(source, persist).also { models.add(it) }

    @Test
    fun addingFoldersDeduplicatesPathsAndCancelDiscardsTheDraft() = runTest {
        val source = MutableStateFlow(UserSettings(localMediaFolders = listOf("/home/user/Music")))
        var writes = 0
        val editor = model(source) { writes++ }
        editor.open()
        editor.addFolder(" /home/user/Music/// ")
        editor.addFolder("  ")
        assertFalse(editor.uiState.value.canSave)
        editor.addFolder("/home/user/Documents/Music Production/")
        runCurrent()
        assertEquals(
            listOf("Music", "Music Production"),
            editor.uiState.value.folders.map { it.name })
        assertTrue(editor.uiState.value.canSave)
        editor.dismiss()
        editor.addFolder("/late-picker-result")
        editor.open()
        runCurrent()
        assertEquals(listOf("/home/user/Music"), editor.uiState.value.folders.map { it.path })
        assertEquals(0, writes)
    }

    @Test
    fun settingsUpdatesDoNotResetAnOpenDraft() = runTest {
        val source = MutableStateFlow(UserSettings(localMediaFolders = listOf("/music")))
        val editor = model(source)
        editor.open()
        editor.addFolder("/new-music")
        source.value = source.value.copy(overloadedDownloadFolder = "/downloads")
        runCurrent()
        assertEquals(listOf("/music", "/new-music"), editor.uiState.value.folders.map { it.path })
        assertTrue(editor.uiState.value.canSave)
    }

    @Test
    fun saveLocksTheDraftUntilPersistenceCompletes() = runTest {
        val source = MutableStateFlow(UserSettings(localMediaFolders = listOf("/music")))
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val editor = model(source) { paths ->
            writes++
            gate.await()
            source.value = source.value.copy(localMediaFolders = paths)
        }
        editor.open()
        editor.addFolder("/new-music")
        editor.save()
        editor.save()
        editor.addFolder("/not-allowed-during-save")
        editor.dismiss()
        runCurrent()
        assertTrue(editor.uiState.value.isSaving)
        assertTrue(editor.uiState.value.isOpen)
        assertFalse(editor.uiState.value.canSave)
        assertEquals(2, editor.uiState.value.folders.size)
        gate.complete(Unit)
        runCurrent()
        assertFalse(editor.uiState.value.isOpen)
        assertEquals(listOf("/music", "/new-music"), source.value.localMediaFolders)
        assertEquals(1, writes)
    }

    @Test
    fun failedSaveKeepsChangesForRetry() = runTest {
        val source = MutableStateFlow(UserSettings())
        val editor = model(source) { throw IllegalStateException("Test storage failure") }
        editor.open()
        editor.addFolder("/music")
        editor.save()
        runCurrent()
        assertTrue(editor.uiState.value.isOpen)
        assertTrue(editor.uiState.value.saveFailed)
        assertTrue(editor.uiState.value.canSave)
        assertEquals(listOf("/music"), editor.uiState.value.folders.map { it.path })
    }

    @Test
    fun rootPathsStayValid() {
        assertEquals("/", normalizeMediaFolderPath(" / "))
        assertEquals("C:\\", normalizeMediaFolderPath("C:\\"))
        assertEquals("C:/", normalizeMediaFolderPath("C:///"))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun renderDialogAtCompactAndWideSizes() {
        val output = File("build/reports/local-media-folders").apply { mkdirs() }
        val source =
            MutableStateFlow(UserSettings(localMediaFolders = listOf("/home/krtirtho/Documents/Music Production")))
        val editor = model(source)
        editor.open()
        editor.addFolder("/home/krtirtho/Music")
        val oneFolder = editor.uiState.value.copy(folders = editor.uiState.value.folders.take(1))
        val cases = mapOf(
            "empty" to oneFolder.copy(folders = emptyList()),
            "one" to oneFolder,
            "many" to oneFolder.copy(folders = (1..8).map {
                LocalMediaFolderRowUi(
                    "/home/user/Music/Collection-$it",
                    "Collection-$it"
                )
            })
        )
        for (theme in listOf(Theme.DARK, Theme.LIGHT)) {
            for (width in listOf(360, 640)) {
                for ((name, state) in cases) {
                    var added = 0
                    ImageComposeScene(width, 660) {
                        SpotubeTheme(UserSettings(theme = theme)) {
                            Box(
                                Modifier.fillMaxSize().background(MillenniumTheme.colors.background)
                            ) {
                                MillenniumDialog(
                                    visible = true,
                                    onDismissRequest = {},
                                    title = stringResource(Res.string.settings_local_media_folders_title)
                                ) {
                                    LocalMediaFoldersEditorContent(state, { added++ }, {}, {}, {})
                                }
                            }
                        }
                    }.use { scene ->
                        repeat(24) { scene.render(it * 16_000_000L).close() }
                        fun flatten(node: SemanticsNode): List<SemanticsNode> =
                            listOf(node) + node.children.flatMap(::flatten)

                        val nodes = scene.semanticsOwners.flatMap { flatten(it.rootSemanticsNode) }
                        val add = nodes.first {
                            it.config.getOrNull(SemanticsActions.OnClick) != null &&
                                    it.config.getOrNull(SemanticsProperties.Text)
                                        ?.any { text -> text.text == "Add folder" } == true
                        }
                        add.config[SemanticsActions.OnClick].action?.invoke()
                        assertEquals(1, added)
                        assertTrue(
                            add.boundsInRoot.right <= width,
                            "Actions must fit inside compact dialogs"
                        )
                        scene.render(400_000_000).use { image ->
                            image.encodeToData()!!.use {
                                File(
                                    output,
                                    "$name-$theme-$width.png"
                                ).writeBytes(it.bytes)
                            }
                        }
                    }
                }
            }
        }
    }
}
