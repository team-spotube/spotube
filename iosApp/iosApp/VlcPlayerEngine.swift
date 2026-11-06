// Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

import AVFoundation
import ComposeApp
import Foundation
import MobileVLCKit

/// Registers the MobileVLCKit engine with the shared Kotlin `AudioPlayer`. Call before the first
/// `ComposeView` is created.
func registerNativePlayerEngine() {
    NativePlayerRegistry.shared.factory = VlcPlayerEngineFactory()
}

final class VlcPlayerEngineFactory: NSObject, NativePlayerEngineFactory {
    func create() -> NativePlayerEngine {
        VlcPlayerEngine()
    }
}

/// Plays one URL at a time with MobileVLCKit. The Kotlin `AudioPlayer` owns the playlist; see
/// `NativePlayerEngine.kt` for the contract.
///
/// Every VLC call is made on the main queue, in the order the Kotlin side issued it.
final class VlcPlayerEngine: NSObject, NativePlayerEngine, VLCMediaPlayerDelegate {
    private let player = VLCMediaPlayer()
    private let audioSession = AVAudioSession.sharedInstance()
    private var listener: NativePlayerListener?
    private var released = false

    private var rate: Float = 1
    private var volumePercent: Int32 = 100
    private var lastReportedDurationMs: Int64 = 0
    private var lastReportedState: NativePlayerState?

    override init() {
        super.init()
        player.delegate = self
    }

    // MARK: NativePlayerEngine

    func setListener(listener: NativePlayerListener?) {
        onMain { self.listener = listener }
    }

    func load(url: String, autoPlay: Bool, startPositionMs: Int64) {
        onMain {
            guard !self.released else { return }
            guard let mediaUrl = URL(string: url) else {
                self.listener?.onError(message: "Invalid URL: \(url)")
                return
            }

            let media = VLCMedia(url: mediaUrl)
            if startPositionMs > 0 {
                media.addOption(":start-time=\(Double(startPositionMs) / 1000.0)")
            }
            self.lastReportedDurationMs = 0
            self.player.media = media
            self.applyPlaybackSettings()
            self.report(.opening)
            if autoPlay { self.startPlayback() }
        }
    }

    func play() {
        onMain {
            guard !self.released else { return }
            self.startPlayback()
        }
    }

    func pause() {
        onMain {
            guard !self.released, self.player.isPlaying else { return }
            self.player.pause()
        }
    }

    func stop() {
        onMain {
            guard !self.released else { return }
            self.player.stop()
        }
    }

    func seekTo(positionMs: Int64) {
        onMain {
            guard !self.released, self.player.isSeekable else { return }
            self.player.time = VLCTime(number: NSNumber(value: positionMs))
        }
    }

    func setVolume(volume: Float) {
        onMain {
            self.volumePercent = Int32((min(max(volume, 0), 1) * 100).rounded())
            self.player.audio?.volume = self.volumePercent
        }
    }

    func setRate(rate: Float) {
        onMain {
            self.rate = rate
            self.player.rate = rate
        }
    }

    func destroy() {
        onMain {
            guard !self.released else { return }
            self.released = true
            self.player.delegate = nil
            self.player.stop()
            self.player.media = nil
            do {
                try self.audioSession.setActive(false, options: .notifyOthersOnDeactivation)
            } catch {
                self.listener?.onError(message: "Could not deactivate audio session: \(error.localizedDescription)")
            }
            self.listener = nil
        }
    }

    // MARK: VLCMediaPlayerDelegate

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        onMain {
            guard !self.released else { return }
            switch self.player.state {
            case .opening:
                self.report(.opening)
            case .buffering, .esAdded:
                // MobileVLCKit keeps sending these while it plays; only a stall counts as buffering.
                self.report(self.player.isPlaying ? .playing : .buffering)
            case .playing:
                self.report(.playing)
            case .paused:
                self.report(.paused)
            case .ended:
                self.report(.ended)
            case .stopped:
                self.report(.idle)
            case .error:
                self.listener?.onError(message: "VLC could not play this media")
                self.report(.idle)
            default:
                break
            }
        }
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        onMain {
            guard !self.released else { return }
            // Time only advances while playing, whatever the last state notification said.
            if self.player.isPlaying { self.report(.playing) }
            self.reportDuration()
            if let ms = self.player.time.value?.int64Value, ms >= 0 {
                self.listener?.onTime(positionMs: ms)
            }
        }
    }

    // MARK: Internals

    private func startPlayback() {
        do {
            try audioSession.setCategory(.playback, mode: .default, options: [])
        } catch {
            listener?.onError(message: "Could not configure audio session: \(error.localizedDescription)")
            return
        }
        do {
            try audioSession.setActive(true)
        } catch {
            listener?.onError(message: "Could not activate audio session: \(error.localizedDescription)")
            return
        }
        applyPlaybackSettings()
        player.play()
    }

    /// Sends [state] to Kotlin unless it was the last one sent.
    private func report(_ state: NativePlayerState) {
        guard state != lastReportedState else { return }
        lastReportedState = state
        if state == .playing {
            applyPlaybackSettings()
            reportDuration()
        }
        listener?.onState(state: state)
    }

    private func applyPlaybackSettings() {
        player.audio?.volume = volumePercent
        if player.rate != rate { player.rate = rate }
    }

    /// MobileVLCKit has no length callback, so the length is read from the media as it plays.
    private func reportDuration() {
        guard let ms = player.media?.length.value?.int64Value, ms > 0, ms != lastReportedDurationMs else { return }
        lastReportedDurationMs = ms
        listener?.onDuration(durationMs: ms)
    }

    private func onMain(_ work: @escaping () -> Void) {
        DispatchQueue.main.async(execute: work)
    }
}
