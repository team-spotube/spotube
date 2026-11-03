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
import VLCKit

/// Registers the VLCKit engine with the shared Kotlin `AudioPlayer`. Call before the first
/// `ComposeView` is created.
func registerNativePlayerEngine() {
    NativePlayerRegistry.shared.factory = VlcPlayerEngineFactory()
}

final class VlcPlayerEngineFactory: NSObject, NativePlayerEngineFactory {
    func create() -> NativePlayerEngine {
        VlcPlayerEngine()
    }
}

/// Plays one URL at a time with VLCKit. The Kotlin `AudioPlayer` owns the playlist; see
/// `NativePlayerEngine.kt` for the contract.
///
/// Every VLCKit call is made on the main queue, in the order the Kotlin side issued it.
final class VlcPlayerEngine: NSObject, NativePlayerEngine, VLCMediaPlayerDelegate {
    private let player = VLCMediaPlayer()
    private var listener: NativePlayerListener?

    // VLCKit 4 has no "ended" state: reaching the end of a media goes Stopping -> Stopped, the
    // same as stop(). `stopRequested` marks the stops this class asked for, so any other stop of
    // a media that had started playing is a natural end.
    private var stopRequested = false
    private var hasStarted = false
    private var finished = false
    private var released = false

    private var rate: Float = 1
    private var volumePercent: Int32 = 100

    override init() {
        super.init()
        player.delegate = self
        player.timeChangeUpdateInterval = 0.25
        player.minimalTimePeriod = 250_000
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
    }

    // MARK: NativePlayerEngine

    func setListener(listener: NativePlayerListener?) {
        onMain { self.listener = listener }
    }

    func load(url: String, autoPlay: Bool, startPositionMs: Int64) {
        onMain {
            guard !self.released else { return }
            guard let mediaUrl = URL(string: url), let media = VLCMedia(url: mediaUrl) else {
                self.listener?.onError(message: "Invalid URL: \(url)")
                return
            }

            self.stopCurrentMedia()
            self.hasStarted = false
            self.finished = false
            if startPositionMs > 0 {
                media.addOption(":start-time=\(Double(startPositionMs) / 1000.0)")
            }
            self.player.media = media
            self.listener?.onState(state: .opening)
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
            self.stopCurrentMedia()
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
            self.listener = nil
            self.player.delegate = nil
            self.player.stop()
            self.player.media = nil
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }
    }

    // MARK: VLCMediaPlayerDelegate

    func mediaPlayerStateChanged(_ newState: VLCMediaPlayerState) {
        onMain {
            guard !self.released else { return }
            switch newState {
            case .opening:
                self.listener?.onState(state: .opening)
            case .playing:
                self.hasStarted = true
                self.applyPlaybackSettings()
                self.reportDuration()
                self.listener?.onState(state: .playing)
            case .paused:
                self.listener?.onState(state: .paused)
            case .stopping, .stopped:
                self.handleStop(isFinalStop: newState == .stopped)
            case .error:
                self.finished = true
                self.listener?.onError(message: "VLC could not play this media")
                self.listener?.onState(state: .idle)
            default:
                break
            }
        }
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        onMain {
            guard !self.released, let ms = self.player.time.value?.int64Value, ms >= 0 else { return }
            self.listener?.onTime(positionMs: ms)
        }
    }

    func mediaPlayerLengthChanged(_ length: Int64) {
        onMain {
            guard !self.released else { return }
            self.listener?.onDuration(durationMs: length)
        }
    }

    func mediaPlayerBufferingChanged(_ progress: Float) {
        onMain {
            guard !self.released else { return }
            self.listener?.onBuffered(percent: progress * 100)
            // VLC always reports 0 and 1 before the first frame; only mid-playback stalls matter.
            guard self.hasStarted else { return }
            if progress < 1 {
                self.listener?.onState(state: .buffering)
            } else if self.player.isPlaying {
                self.listener?.onState(state: .playing)
            }
        }
    }

    // MARK: Internals

    private func startPlayback() {
        try? AVAudioSession.sharedInstance().setActive(true)
        let wasPaused = player.state == .paused
        player.play()
        if wasPaused { kickTimeReporting() }
    }

    /// After a pause, VLCKit 4 withholds time updates until libvlc reports a fresh time point, and
    /// on iOS that report doesn't come after a resume: audio plays but the position stays frozen.
    /// A seek goes through a different callback that does reset VLCKit's time tracking, so seek
    /// to where we already are.
    private func kickTimeReporting() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            guard let self, !self.released, self.player.isPlaying, self.player.isSeekable else { return }
            self.player.time = self.player.time
        }
    }

    /// Stops the current media and marks the resulting stop events as ours.
    private func stopCurrentMedia() {
        switch player.state {
        case .opening, .playing, .paused, .stopping:
            stopRequested = true
            player.stop()
        default:
            break
        }
    }

    private func handleStop(isFinalStop: Bool) {
        if stopRequested {
            if isFinalStop {
                stopRequested = false
                listener?.onState(state: .idle)
            }
            return
        }
        if hasStarted && !finished {
            finished = true
            listener?.onState(state: .ended)
        } else if isFinalStop && !finished {
            listener?.onState(state: .idle)
        }
    }

    private func applyPlaybackSettings() {
        player.audio?.volume = volumePercent
        if player.rate != rate { player.rate = rate }
    }

    private func reportDuration() {
        if let ms = player.media?.length.value?.int64Value, ms > 0 {
            listener?.onDuration(durationMs: ms)
        }
    }

    private func onMain(_ work: @escaping () -> Void) {
        DispatchQueue.main.async(execute: work)
    }
}
