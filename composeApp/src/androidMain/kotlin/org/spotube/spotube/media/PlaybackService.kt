/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.media

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.spotube.spotube.MainActivity
import org.spotube.spotube.R
import org.spotube.spotube.core.audioplayer.AudioPlayer
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.notification_playback_channel_description
import spotube.composeapp.generated.resources.notification_playback_channel_name

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService(), KoinComponent {
    private var taskRemoved = false


    private val audioPlayer = inject<AudioPlayerInterface>().value as AudioPlayer
    private val audioPlayerQueue: AudioPlayerQueue by inject()
    private val mediaBrowseHelper: MediaBrowseHelper by inject()
    private lateinit var librarySession: MediaLibrarySession

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")

        createNotificationChannel()

        // Media3 posts the real playback notification; point it at our channel,
        // ID and monochrome icon so it replaces the placeholder below in place.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_spotube) }
        )

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Short-lived placeholder until Media3 posts the playback notification. No localized text:
        // loading it here would block the main thread (see createNotificationChannel).
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spotube")
            .setSmallIcon(R.drawable.ic_stat_spotube)
            .setContentIntent(sessionActivity)
            .setOngoing(true)
            .build()

        // On Android 15+ a service created from the background (e.g. by a jam
        // sync or queue restoration) hits this in startForeground instead of at
        // the startForegroundService call site. Don't crash: playback keeps
        // running in-process, just without the notification until the app is
        // foregrounded and the service can start properly.
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "startForeground not allowed; continuing without notification", e)
        }

        librarySession =
            MediaLibrarySession.Builder(this, audioPlayer.player, LibrarySessionCallback())
                .setSessionActivity(sessionActivity)
                .build()

        addSession(librarySession)

        Log.i(TAG, "Library session created")
    }

    /**
     * The channel must exist before startForeground, so it's created right away under the app name
     * and renamed once the localized strings load.
     *
     * Never `runBlocking { getString(...) }` here: onCreate runs on the main thread, and Compose
     * resources share one in-flight load per strings file. If App()'s stringResource started that
     * load on the main dispatcher first, blocking main to await it deadlocks the app on its
     * splash screen.
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        fun channel(name: String, description: String? = null) =
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                this.description = description
                setShowBadge(false)
            }

        manager.createNotificationChannel(channel("Spotube"))
        serviceScope.launch {
            // Re-creating a channel with the same ID updates its name and description.
            manager.createNotificationChannel(
                channel(
                    name = getString(Res.string.notification_playback_channel_name),
                    description = getString(Res.string.notification_playback_channel_description)))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        librarySession

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        removeSession(librarySession)
        librarySession.release()
        super.onDestroy()
        // Swiping the app out of recents should fully close it: Android otherwise keeps this
        // process cached with the proxy server, jam client and every singleton still alive.
        if (taskRemoved) Process.killProcess(Process.myPid())
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        taskRemoved = true
        pauseAllPlayersAndStopSelf()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder()
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE)
                .build()

            val playerCommands = Player.Commands.Builder()
                .addAllCommands()
                .build()

            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                playerCommands
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Media3MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(mediaBrowseHelper.buildRootItem(), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<Media3MediaItem>> {
            val future = SettableFuture.create<LibraryResult<Media3MediaItem>>()
            serviceScope.launch {
                try {
                    val items = mediaBrowseHelper.getChildren(mediaId)
                    if (items.isEmpty()) {
                        future.set(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))
                    } else {
                        future.set(LibraryResult.ofItem(items.first(), null))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onGetItem failed for $mediaId", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<Media3MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<Media3MediaItem>>>()
            serviceScope.launch {
                try {
                    Log.d(TAG, "onGetChildren: parentId=$parentId, page=$page, pageSize=$pageSize")
                    val children = mediaBrowseHelper.getChildren(parentId)
                    val paged = if (pageSize in 1 until children.size) {
                        val start = page * pageSize
                        val end = minOf(start + pageSize, children.size)
                        if (start < children.size) children.subList(start, end) else emptyList()
                    } else {
                        children
                    }
                    Log.d(TAG, "onGetChildren: returning ${paged.size} items for $parentId")
                    future.set(LibraryResult.ofItemList(paged, params))
                } catch (e: Exception) {
                    Log.e(TAG, "onGetChildren failed for $parentId", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onUnsubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid(null))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val future = SettableFuture.create<LibraryResult<Void>>()
            serviceScope.launch {
                try {
                    val results = mediaBrowseHelper.search(query)
                    Log.d(TAG, "onSearch: query=$query, got ${results.size} results")
                    session.notifySearchResultChanged(browser, query, results.size, params)
                    future.set(LibraryResult.ofVoid(params))
                } catch (e: Exception) {
                    Log.e(TAG, "onSearch failed for $query", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<Media3MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<Media3MediaItem>>>()
            serviceScope.launch {
                try {
                    val results = mediaBrowseHelper.search(query)
                    val paged = if (pageSize in 1 until results.size) {
                        val start = page * pageSize
                        val end = minOf(start + pageSize, results.size)
                        if (start < results.size) results.subList(start, end) else emptyList()
                    } else {
                        results
                    }
                    Log.d(
                        TAG,
                        "onGetSearchResult: query=$query, page=$page, returning ${paged.size} items"
                    )
                    future.set(LibraryResult.ofItemList(paged, params))
                } catch (e: Exception) {
                    Log.e(TAG, "onGetSearchResult failed for $query", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<Media3MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    handlePlaybackRequest(mediaItems, startIndex, startPositionMs)
                    val currentItems = buildCurrentMediaItemList()
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            currentItems,
                            startIndex,
                            startPositionMs
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "onSetMediaItems failed", e)
                    future.setException(e)
                }
            }
            return future
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<Media3MediaItem>
        ): ListenableFuture<List<Media3MediaItem>> {
            val future = SettableFuture.create<List<Media3MediaItem>>()
            serviceScope.launch {
                try {
                    val entries = mutableListOf<QueueEntry>()
                    for (item in mediaItems) {
                        val track = mediaBrowseHelper.resolveTrackById(
                            item.mediaId.removePrefix("${MediaBrowseHelper.MEDIA_ID_TRACK}:")
                        )
                        if (track != null) {
                            entries.add(QueueEntry.StreamingTrack(track = track, url = ""))
                        }
                    }
                    if (entries.isNotEmpty()) {
                        audioPlayerQueue.addAllToQueue(entries)
                    }
                    future.set(buildCurrentMediaItemList())
                } catch (e: Exception) {
                    Log.e(TAG, "onAddMediaItems failed", e)
                    future.setException(e)
                }
            }
            return future
        }
    }

    private suspend fun handlePlaybackRequest(
        mediaItems: List<Media3MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        val firstItem = mediaItems.firstOrNull()
        if (firstItem != null) {
            val mediaId = firstItem.mediaId
            when {
                mediaId.startsWith("${MediaBrowseHelper.MEDIA_ID_PLAYLIST}:") -> {
                    val playlistId = mediaId.removePrefix("${MediaBrowseHelper.MEDIA_ID_PLAYLIST}:")
                    val helper: org.spotube.spotube.core.playback.CollectionPlaybackHelper by inject()
                    helper.playPlaylist(playlistId)
                    return
                }

                mediaId.startsWith("${MediaBrowseHelper.MEDIA_ID_ALBUM}:") -> {
                    val albumId = mediaId.removePrefix("${MediaBrowseHelper.MEDIA_ID_ALBUM}:")
                    val helper: org.spotube.spotube.core.playback.CollectionPlaybackHelper by inject()
                    helper.playAlbum(albumId)
                    return
                }

                mediaId == MediaBrowseHelper.MEDIA_ID_SAVED_TRACKS -> {
                    val helper: org.spotube.spotube.core.playback.CollectionPlaybackHelper by inject()
                    helper.playSavedTracks()
                    return
                }

                mediaId.startsWith("${MediaBrowseHelper.MEDIA_ID_ARTIST_TRACKS}:") -> {
                    mediaBrowseHelper.resolveAndPlayFromMediaId(mediaId)
                    return
                }
            }
        }

        val entries = mutableListOf<QueueEntry>()
        for (item in mediaItems) {
            val track = mediaBrowseHelper.resolveTrackById(
                item.mediaId.removePrefix("${MediaBrowseHelper.MEDIA_ID_TRACK}:")
            )
            if (track != null) {
                entries.add(QueueEntry.StreamingTrack(track = track, url = ""))
            }
        }
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(entries, autoPlay = true, startPosition = startIndex)
        }
    }

    private suspend fun buildCurrentMediaItemList(): List<Media3MediaItem> =
        withContext(Dispatchers.Main) {
            val count = audioPlayer.player.mediaItemCount
            val items = mutableListOf<Media3MediaItem>()
            for (i in 0 until count) {
                val item = audioPlayer.player.getMediaItemAt(i)
                items.add(item)
            }
            items
        }

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "spotube_playback"
        private const val NOTIFICATION_ID = 1
    }
}
