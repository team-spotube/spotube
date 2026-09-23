import 'package:spotube/provider/metadata_plugin/metadata_plugin_provider.dart';
import 'package:spotube/models/metadata/metadata.dart';
import 'package:spotube/services/logger/logger.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:spotube/provider/audio_player/audio_player.dart';
import 'package:spotube/provider/user_preferences/user_preferences_provider.dart';
import 'package:spotube/services/audio_player/audio_player.dart';

void useEndlessPlayback(WidgetRef ref) {
  final playback = ref.watch(audioPlayerProvider.notifier);
  final audioPlayerState = ref.watch(audioPlayerProvider);
  final endlessPlayback = ref.watch(
    userPreferencesProvider.select((s) => s.endlessPlayback),
  );
  final metadataPlugin = ref.watch(metadataPluginProvider.future);

  useEffect(
    () {
      if (!endlessPlayback) return null;

      void listener(int index) async {
        try {
          final playlist = ref.read(audioPlayerProvider);
          if (index != playlist.tracks.length - 1) return;

          final track = playlist.tracks.last;
          final plugin = await metadataPlugin;
          if (plugin == null) return;

          final tracks = <SpotubeFullTrackObject>[];
          final existingIds = playlist.tracks.map((track) => track.id).toSet();

          Future<void> addFirstAvailable(
            Future<Iterable<SpotubeFullTrackObject>> Function() load,
          ) async {
            if (tracks.isNotEmpty) return;

            try {
              final candidates = await load();
              for (final candidate in candidates) {
                if (existingIds.add(candidate.id)) {
                  tracks.add(candidate);
                }
              }
            } catch (error, stack) {
              // A provider may not implement every discovery endpoint. Keep
              // trying the progressively broader fallbacks below.
              AppLogger.reportError(error, stack);
            }
          }

          // Prefer the provider's recommendations. Some providers currently
          // return an empty list or throw here, leaving a searched song as a
          // one-item queue even when Endless Playback is enabled.
          await addFirstAvailable(() => plugin.track.radio(track.id));

          // Keep playback moving with the rest of the album when radio is not
          // available. Singles naturally fall through to the artist fallback.
          await addFirstAvailable(
            () async =>
                (await plugin.album.tracks(track.album.id, limit: 50)).items,
          );

          if (track.artists.isNotEmpty) {
            await addFirstAvailable(
              () async => (await plugin.artist.topTracks(
                track.artists.first.id,
                limit: 50,
              )).items,
            );
          }

          if (tracks.isEmpty) return;
          await playback.addTracks(tracks);
        } catch (e, stack) {
          AppLogger.reportError(e, stack);
        }
      }

      // Sometimes user can change settings for which the currentIndexChanged
      // might not be called. So we need to check if the current track is the
      // last track and if it is then we need to call the listener manually.
      if (audioPlayerState.currentIndex == audioPlayerState.tracks.length - 1 &&
          audioPlayer.isPlaying) {
        listener(audioPlayerState.currentIndex);
      }

      final subscription = audioPlayer.currentIndexChangedStream.listen(
        listener,
      );

      return subscription.cancel;
    },
    [
      metadataPlugin,
      playback,
      audioPlayerState.tracks,
      audioPlayerState.currentIndex,
      endlessPlayback,
    ],
  );
}
