/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.StringResource
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.library.local.encodeLocalCoverThumbnail
import org.spotube.spotube.modules.library.local_tracks.media.*
import org.spotube.spotube.modules.settings.SettingsRepository
import spotube.composeapp.generated.resources.*
import uniffi.compose_app.AudioTagException
import kotlin.io.encoding.Base64

internal data class BrowserFolder(val id: String, val name: String, val label: StringResource? = null)
internal data class BrowserTrack(val id: String, val name: String, val subtitle: String?, val cover: ByteArray?)
internal enum class TrackAction { Copy, Move, Delete, Edit, Details }
internal data class TrackTransfer(val count: Int, val move: Boolean)
internal data class DeleteTracks(val count: Int, val title: String?)
internal data class FileBatchReport(val done: Int, val conflicts: Int, val errors: Int)

private data class MetadataSave(val file: MusicFile, val tags: LocalTrackTags)

internal data class FileBatchResult<T>(val succeeded: List<T>, val conflicts: List<T>, val errors: List<T>) {
    val report get() = FileBatchReport(succeeded.size, conflicts.size, errors.size)
}

/** One bad destination or permission error shouldn't prevent processing the other tracks. */
internal suspend fun <T> runFileBatch(files: List<T>, operation: suspend (T) -> Unit): FileBatchResult<T> {
    val succeeded = mutableListOf<T>()
    val conflicts = mutableListOf<T>()
    val errors = mutableListOf<T>()
    for (file in files) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        try {
            operation(file)
            succeeded += file
        } catch (e: CancellationException) {
            throw e
        } catch (_: MusicDestinationExists) {
            conflicts += file
        } catch (_: Exception) {
            errors += file
        }
    }
    return FileBatchResult(succeeded, conflicts, errors)
}

internal class TrackEditor(tags: LocalTrackTags) {
    val title = TextFieldState(tags.title)
    val artists = TextFieldState(tags.artists)
    val album = TextFieldState(tags.album)
    val track = TextFieldState(tags.track?.toString().orEmpty())
    val disc = TextFieldState(tags.disc?.toString().orEmpty())
}

internal data class MusicBrowserState(
    val breadcrumb: List<BrowserFolder> = emptyList(),
    val folders: List<BrowserFolder> = emptyList(),
    val tracks: List<BrowserTrack> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val canAccessMedia: Boolean = true,
    val canPaste: Boolean = false,
    val canPlay: Boolean = false,
    val isSelecting: Boolean = false,
    val selected: Set<String> = emptySet(),
    val transfer: TrackTransfer? = null,
    val deleteRequest: DeleteTracks? = null,
    val newFolder: TextFieldState? = null,
    val details: MusicFileDetails? = null,
    val editor: TrackEditor? = null,
    val editorCover: ByteArray? = null,
    val isLoadingCover: Boolean = false,
    val awaitingWriteAccess: Boolean = false,
    val message: StringResource? = null,
    val report: FileBatchReport? = null
) {
    val hasFileActions: Boolean get() = transfer != null || isSelecting
    val hasDialog: Boolean get() = editor != null || deleteRequest != null || newFolder != null || details != null
    val canNavigateBack: Boolean
        get() = (breadcrumb.isNotEmpty() || isSelecting) && !busy && !hasDialog
}

class LibraryLocalTracksViewModel(
    private val coordinator: LocalMediaLibraryCoordinator,
    private val libraryState: LibraryState,
    private val settings: SettingsRepository,
    private val paths: Paths,
    private val queue: AudioPlayerQueue
) : ViewModel() {
    private val model = MutableStateFlow(MusicBrowserState())
    private var trail = emptyList<Pair<MusicLocation, BrowserFolder>>()
    private var children = emptyMap<String, MusicLocation>()
    private var tracks = emptyList<MusicFile>()
    private var pendingTrack: MusicFile? = null
    private var transferSources = emptyList<MusicFile>()
    private var pendingDeletes = emptyList<MusicFile>()
    private var listing: Job? = null
    private var browserIndex = MusicBrowserIndex()
    private var pendingMetadataSave: MetadataSave? = null
    private val writeAccessRequests = Channel<MusicWriteRequest>(Channel.BUFFERED)
    internal val writePermissionRequests = writeAccessRequests.receiveAsFlow()

    internal val state = combine(model, libraryState.queryFlow(LibraryTab.LocalTracks)) { ui, query ->
        val term = query.trim()
        val visibleTracks = ui.tracks.filter { it.name.contains(term, true) || it.subtitle?.contains(term, true) == true }
        ui.copy(
            folders = ui.folders.filter { it.label != null || it.name.contains(term, true) },
            tracks = visibleTracks,
            canPlay = visibleTracks.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, model.value)

    init {
        viewModelScope.launch { libraryState.localRefreshRequests.collect { refresh() } }
        viewModelScope.launch {
            val locations = settings.userSettings.distinctUntilChangedBy {
                Triple(it.localMediaFolders, it.localMediaFolderBookmarks, it.downloadFolderLocation to it.overloadedDownloadFolder)
            }.map { prefs ->
                musicRoots(paths, prefs).mapNotNull { root ->
                    runCatching {
                        val bookmark = prefs.localMediaFolderBookmarks[root.path]
                        val file = if (bookmark == null) PlatformFile(root.path) else PlatformFile.fromBookmarkData(Base64.decode(bookmark))
                        BrowserFolder(root.path, root.displayName ?: file.name.ifBlank { root.path }, root.label) to MusicLocation(file)
                    }.getOrNull()
                }
            }.flowOn(Dispatchers.IO)
            combine(coordinator.cacheState, locations) { cache, roots ->
                val shared = if (getPlatform().type == PlatformType.Android) roots.firstOrNull()?.first?.id?.toPath() else null
                buildMusicBrowserIndex(cache, roots, shared)
            }.conflate().flowOn(Dispatchers.Default).collect { index ->
                browserIndex = index
                if (model.value.transfer == null) showIndexedFolder()
            }
        }
    }

    fun onPermissionChanged(granted: Boolean) {
        if (model.value.canAccessMedia == granted) return
        model.update { it.copy(canAccessMedia = granted) }
        refresh()
    }

    internal fun openFolder(id: String) {
        if (model.value.busy) return
        val location = children[id] ?: return
        val row = model.value.folders.firstOrNull { it.id == id } ?: return
        trail = trail + (location to row)
        navigate()
    }

    internal fun up() = goTo(trail.lastIndex - 1)

    internal fun back() {
        if (model.value.isSelecting) clearSelection() else up()
    }

    internal fun goTo(index: Int) {
        if (model.value.busy || index >= trail.lastIndex) return
        trail = trail.take((index + 1).coerceAtLeast(0))
        navigate()
    }

    private fun navigate() {
        model.update { it.copy(breadcrumb = trail.map { it.second }, isSelecting = false, selected = emptySet(), message = null, report = null) }
        loadListing()
    }

    private fun refresh() = coordinator.scheduleRefresh("music_browser_refresh")

    private fun loadListing() {
        if (model.value.busy) return
        listing?.cancel()
        if (model.value.transfer == null) {
            showIndexedFolder()
            return
        }
        val location = trail.lastOrNull()?.first
        val rootLabel = trail.firstOrNull()?.second?.label
        if (location == null) {
            publishListing(BrowserListing(browserIndex.locations))
            return
        }
        val indexed = browserIndex.listings[location] ?: BrowserListing()
        publishListing(indexed, loading = true)
        listing = viewModelScope.launch {
            try {
                val destinations = withContext(Dispatchers.IO) {
                    if (location.root == location.folder && !location.folder.exists() &&
                        (rootLabel == Res.string.library_browser_private_data || rootLabel == Res.string.library_browser_private_cache)) {
                        location.folder.createDirectories()
                    }
                    location.listDestinationFolders().map { BrowserFolder(it.path, it.name) to location.copy(folder = it) }
                }
                publishListing(indexed.copy(folders = destinations), canPaste = true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                model.update { it.copy(message = Res.string.library_browser_open_failed, canPaste = false) }
            } finally {
                if (listing == coroutineContext[Job]) {
                    model.update { it.copy(loading = false) }
                }
            }
        }
    }

    private fun showIndexedFolder() = publishListing(browserIndex.listings[trail.lastOrNull()?.first] ?: BrowserListing())

    private fun publishListing(result: BrowserListing, canPaste: Boolean = false, loading: Boolean = false) {
        children = result.folders.associate { (row, loc) -> row.id to loc }
        tracks = result.tracks
        model.update { ui -> ui.copy(
            folders = result.folders.map { it.first }, tracks = result.trackRows,
            selected = if (ui.selected.isEmpty()) ui.selected else ui.selected.intersect(result.tracks.mapTo(hashSetOf()) { it.path }),
            canPaste = canPaste, loading = loading
        ) }
    }

    internal fun addFolder(folder: PlatformFile) = operate(Res.string.library_browser_add_failed) {
        val path = folder.path
        val prefs = settings.getCurrentSettings()
        if (musicRoots(paths, prefs).any { it.path == path }) return@operate Res.string.library_browser_already_available
        val bookmark = Base64.encode(folder.bookmarkData().bytes)
        settings.updateSettings(prefs.copy(
            localMediaFolders = prefs.localMediaFolders + path,
            localMediaFolderBookmarks = prefs.localMediaFolderBookmarks + (path to bookmark)
        ))
        Res.string.library_browser_folder_added
    }

    internal fun play(path: String? = null) {
        val visible = state.value.tracks.map { it.id }.toSet()
        val playable = tracks.filter { it.path in visible }
        val start = if (path == null) 0 else playable.indexOfFirst { it.path == path }
        if (playable.isEmpty() || start < 0) return
        viewModelScope.launch {
            queue.load(playable.map { file -> file.tags.toTrack(file.path).let {
                QueueEntry.LocalTrack(name = it.name, artists = it.artists, duration = it.durationMs,
                    album = it.album, coverBytes = it.coverBytes, url = file.playbackPath)
            } }, autoPlay = true, startPosition = start, collectionEntry = null)
        }
    }

    internal fun clickTrack(path: String) {
        if (model.value.busy || model.value.transfer != null) return
        if (model.value.isSelecting) toggleSelection(path) else play(path)
    }

    internal fun startSelection() {
        if (!model.value.busy && model.value.transfer == null) model.update { it.copy(isSelecting = true) }
    }

    internal fun toggleSelection(path: String) {
        model.update { ui ->
            if (ui.busy || ui.transfer != null) ui
            else ui.copy(isSelecting = true, selected = if (path in ui.selected) ui.selected - path else ui.selected + path)
        }
    }

    internal fun selectAll() {
        if (!model.value.busy && model.value.transfer == null) {
            model.update { it.copy(selected = it.selected + state.value.tracks.map { track -> track.id }) }
        }
    }

    internal fun clearSelection() {
        if (!model.value.busy) model.update { it.copy(isSelecting = false, selected = emptySet()) }
    }

    internal fun selectionAction(action: TrackAction) {
        if (model.value.busy || model.value.transfer != null) return
        val selected = tracks.filter { it.path in model.value.selected }
        if (selected.isNotEmpty()) batchAction(selected, action)
    }

    private fun batchAction(files: List<MusicFile>, action: TrackAction) {
        when (action) {
            TrackAction.Copy, TrackAction.Move -> {
                transferSources = files
                model.update { it.copy(transfer = TrackTransfer(files.size, action == TrackAction.Move),
                    isSelecting = false, selected = emptySet(), canPaste = false, message = null, report = null) }
                loadListing()
            }
            TrackAction.Delete -> {
                pendingDeletes = files
                model.update { it.copy(deleteRequest = DeleteTracks(files.size, files.singleOrNull()?.tags?.title), message = null, report = null) }
            }
            else -> Unit
        }
    }

    internal fun action(path: String, action: TrackAction) {
        if (model.value.busy || model.value.transfer != null) return
        val file = tracks.firstOrNull { it.path == path } ?: return
        when (action) {
            TrackAction.Copy, TrackAction.Move, TrackAction.Delete -> batchAction(listOf(file), action)
            TrackAction.Edit -> {
                operate(Res.string.library_browser_open_failed) {
                    val latest = file.location.root.withBrowserAccess { readLocalTrackTags(file.location.folder, file.file) }
                    val tags = latest?.copy(title = latest.title.ifBlank { file.tags.title }) ?: file.tags
                    pendingTrack = file.copy(tags = tags)
                    model.update { it.copy(editor = TrackEditor(tags), editorCover = tags.cover, message = null) }
                    null
                }
            }
            TrackAction.Details -> {
                operate(Res.string.library_browser_details_failed) {
                    val details = file.details()
                    model.update { it.copy(details = details) }
                    null
                }
            }
        }
    }

    internal fun cancelTransfer() {
        if (model.value.busy) return
        transferSources = emptyList()
        model.update { it.copy(transfer = null, canPaste = false) }
        loadListing()
    }

    internal fun paste() {
        if (!model.value.canPaste) return
        val sources = transferSources.takeIf { it.isNotEmpty() } ?: return
        val location = trail.lastOrNull()?.first ?: return
        val move = model.value.transfer?.move ?: return
        operate(Res.string.library_browser_transfer_failed, changesMedia = true) {
            val result = runFileBatch(sources) { it.copyTo(location, move) }
            transferSources = result.conflicts + result.errors
            model.update { it.copy(
                transfer = transferSources.takeIf { it.isNotEmpty() }?.let { remaining -> TrackTransfer(remaining.size, move) },
                report = result.report.takeIf { sources.size > 1 }
            ) }
            when {
                sources.size > 1 -> null
                result.conflicts.isNotEmpty() -> Res.string.library_browser_file_exists
                result.errors.isNotEmpty() -> Res.string.library_browser_transfer_failed
                move -> Res.string.library_browser_moved
                else -> Res.string.library_browser_copied
            }
        }
    }

    internal fun dismissAction() {
        if (model.value.busy || model.value.awaitingWriteAccess) return
        closeAction()
    }

    private fun closeAction() {
        pendingTrack = null
        pendingDeletes = emptyList()
        model.update { it.copy(editor = null, deleteRequest = null, details = null, newFolder = null, editorCover = null, isLoadingCover = false) }
    }

    internal fun delete() {
        val files = pendingDeletes.takeIf { it.isNotEmpty() } ?: return
        operate(Res.string.library_browser_delete_failed, changesMedia = true) {
            val result = runFileBatch(files) { it.delete() }
            closeAction()
            model.update { it.copy(isSelecting = result.errors.isNotEmpty(), selected = result.errors.map { file -> file.path }.toSet(),
                report = result.report.takeIf { files.size > 1 }) }
            when {
                files.size > 1 -> null
                result.errors.isNotEmpty() -> Res.string.library_browser_delete_failed
                else -> Res.string.library_browser_deleted
            }
        }
    }

    internal fun showNewFolder() {
        if (trail.isNotEmpty() && !model.value.busy) model.update { it.copy(newFolder = TextFieldState(), message = null, report = null) }
    }

    internal fun createFolder() {
        val location = trail.lastOrNull()?.first ?: return
        val name = model.value.newFolder?.text?.toString()?.trim() ?: return
        if (!isValidMusicFolderName(name)) {
            model.update { it.copy(message = Res.string.library_browser_folder_name_invalid) }
            return
        }
        operate(Res.string.library_browser_create_folder_failed) {
            val file = try { location.createFolder(name) } catch (_: MusicDestinationExists) {
                return@operate Res.string.library_browser_folder_exists
            }
            closeAction()
            if (model.value.transfer != null) {
                trail = trail + (location.copy(folder = file) to BrowserFolder(file.path, name))
                model.update { it.copy(breadcrumb = trail.map { step -> step.second }) }
                Res.string.library_browser_folder_created
            } else Res.string.library_browser_folder_created_hidden
        }
    }

    internal fun pickCover(file: PlatformFile) {
        val editor = model.value.editor ?: return
        if (model.value.busy || model.value.isLoadingCover) return
        model.update { it.copy(isLoadingCover = true, message = null) }
        viewModelScope.launch {
            try {
                val cover = withContext(Dispatchers.IO) {
                    file.withBrowserAccess {
                        file.source().buffered().use { source ->
                            // Only buffer a bounded amount, including for document providers with unknown sizes.
                            require(!source.request(20L * 1024 * 1024 + 1))
                            requireNotNull(encodeLocalCoverThumbnail(source.readByteArray()))
                        }
                    }
                }
                model.update { if (it.editor === editor) it.copy(editorCover = cover) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                model.update { if (it.editor === editor) it.copy(message = Res.string.library_browser_cover_failed) else it }
            } finally {
                model.update { if (it.editor === editor) it.copy(isLoadingCover = false) else it }
            }
        }
    }

    internal fun saveTags() {
        if (model.value.isLoadingCover || model.value.awaitingWriteAccess || model.value.busy) return
        val draft = model.value.editor ?: return
        val track = pendingTrack ?: return
        val title = draft.title.text.toString().trim()
        val number = draft.track.text.toString().trim()
        val disc = draft.disc.text.toString().trim()
        val trackNumber = number.toIntOrNull()?.takeIf { it > 0 }
        val discNumber = disc.toIntOrNull()?.takeIf { it > 0 }
        if (title.isBlank() || (number.isNotEmpty() && trackNumber == null) || (disc.isNotEmpty() && discNumber == null)) {
            model.update { it.copy(message = Res.string.library_browser_invalid_tags) }
            return
        }
        val tags = track.tags.copy(title = title, artists = draft.artists.text.toString().trim(),
            album = draft.album.text.toString().trim(), track = trackNumber, disc = discNumber,
            // A text-only edit must leave all existing artwork (including alternate covers) alone.
            cover = model.value.editorCover?.takeUnless { it === track.tags.cover })
        saveMetadata(MetadataSave(track, tags))
    }

    internal fun onWritePermissionResult(decision: MusicWriteDecision) {
        val save = pendingMetadataSave ?: return
        pendingMetadataSave = null
        model.update { it.copy(awaitingWriteAccess = false) }
        if (decision.granted) saveMetadata(save, decision)
        else model.update { it.copy(message = Res.string.library_browser_write_access_not_granted) }
    }

    private fun saveMetadata(save: MetadataSave, decision: MusicWriteDecision? = null) {
        model.update { it.copy(busy = true, message = null, report = null) }
        viewModelScope.launch {
            var launchRequest: MusicWriteRequest? = null
            try {
                val request = withContext(Dispatchers.IO) { saveMusicFileTags(save.file, save.tags, decision?.documentUri) }
                when {
                    request == null -> {
                        closeAction()
                        updateSavedTrack(save.file, save.tags.copy(cover = save.tags.cover ?: save.file.tags.cover))
                        model.update { it.copy(message = Res.string.library_browser_tags_saved) }
                        coordinator.scheduleRefresh("track_metadata_saved")
                    }
                    decision == null -> {
                        pendingMetadataSave = save
                        launchRequest = request
                        model.update { it.copy(awaitingWriteAccess = true) }
                    }
                    else -> model.update { it.copy(message = Res.string.library_browser_write_access_not_granted) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: WrongMusicDocument) {
                model.update { it.copy(message = Res.string.library_browser_wrong_write_file) }
            } catch (_: AudioTagException.UnsupportedFormat) {
                model.update { it.copy(message = Res.string.library_browser_unsupported_embedded_tags) }
            } catch (_: AudioTagException.AccessDenied) {
                model.update { it.copy(message = Res.string.library_browser_write_access_not_granted) }
            } catch (_: Exception) {
                model.update { it.copy(message = Res.string.library_browser_save_failed) }
            } finally {
                model.update { it.copy(busy = false) }
            }
            launchRequest?.let { writeAccessRequests.send(it) }
        }
    }

    private fun updateSavedTrack(file: MusicFile, tags: LocalTrackTags) {
        val location = trail.lastOrNull()?.first ?: return
        val listing = browserIndex.listings[location] ?: return
        val updatedTracks = listing.tracks.map { if (it.path == file.path) it.copy(tags = tags) else it }
            .sortedBy { it.tags.title.lowercase() }
        val updated = BrowserListing(listing.folders, updatedTracks)
        browserIndex = browserIndex.copy(listings = browserIndex.listings + (location to updated))
        showIndexedFolder()
    }

    private fun operate(error: StringResource, changesMedia: Boolean = false, block: suspend () -> StringResource?) {
        if (model.value.busy) return
        model.update { it.copy(busy = true, message = null, report = null) }
        viewModelScope.launch {
            try {
                val message = withContext(Dispatchers.IO) { block() }
                model.update { it.copy(message = message) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                model.update { it.copy(message = error) }
            } finally {
                model.update { it.copy(busy = false) }
                if (changesMedia) coordinator.scheduleRefresh("file_browser_operation")
                loadListing()
            }
        }
    }
}
