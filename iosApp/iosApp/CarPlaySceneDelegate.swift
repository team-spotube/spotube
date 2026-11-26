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

import CarPlay
import UIKit
import ComposeApp

/// The CarPlay scene: a Library list (playlists, albums, saved tracks), with Now Playing pushed once
/// something starts. (CPTabBarTemplate only accepts list and grid templates, so Now Playing can't be a tab.)
///
/// CarPlay is UIKit only, so the templates live here, while what they show and play, including every
/// user-facing string, comes from `CarPlayBridge` in the shared Kotlin code. The playback itself is the
/// app's normal player, so Now Playing, the steering wheel buttons and Siri come from the media
/// session the app already publishes (`IosMediaSession`).
///
/// CarPlay may start the app with no window on the phone, so this boots what playback needs itself.
@objc(CarPlaySceneDelegate)
@MainActor
final class CarPlaySceneDelegate: UIResponder, @preconcurrency CPTemplateApplicationSceneDelegate {
    private var interfaceController: CPInterfaceController?

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController
    ) {
        self.interfaceController = interfaceController
        Task { await showRoot(on: interfaceController) }
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnectInterfaceController interfaceController: CPInterfaceController
    ) {
        self.interfaceController = nil
    }

    private func showRoot(on controller: CPInterfaceController) async {
        do {
            try await CarPlayBridge.shared.start()
            let libraryTitle = try await CarPlayBridge.shared.libraryTitle()
            let root = try await CarPlayBridge.shared.root()

            let library = CPListTemplate(
                title: libraryTitle,
                sections: [CPListSection(items: root.map(row(for:)))])
            try await controller.setRootTemplate(library, animated: false)
        } catch {
            NSLog("CarPlay: could not show the root template: \(error)")
        }
    }

    private func row(for item: CarPlayItem) -> CPListItem {
        let row = CPListItem(text: item.title, detailText: item.subtitle)
        row.accessoryType = item.opensList ? .disclosureIndicator : .none
        row.handler = { [weak self] _, completion in
            Task { @MainActor in
                await self?.open(item)
                completion()
            }
        }
        if let urlString = item.imageUrl {
            loadImage(urlString, into: row)
        } else if item.kind == CarPlayItemKind.savedtracks {
            // The row that starts your music wears the player's play button.
            row.setImage(PlayOrbImage.make(size: CPListItem.maximumImageSize))
        }
        return row
    }

    private func open(_ item: CarPlayItem) async {
        if item.opensList {
            let children = (try? await CarPlayBridge.shared.items(kind: item.kind)) ?? []
            let shown = children.prefix(CPListTemplate.maximumItemCount)
            let template = CPListTemplate(
                title: item.title,
                sections: [CPListSection(items: shown.map(row(for:)))])
            _ = try? await interfaceController?.pushTemplate(template, animated: true)
        } else {
            _ = try? await CarPlayBridge.shared.play(item: item)
            await showNowPlaying()
        }
    }

    /// Opens Now Playing, or comes back to it if it is already on the stack (it can only be there once).
    private func showNowPlaying() async {
        guard let controller = interfaceController else { return }
        let nowPlaying = CPNowPlayingTemplate.shared
        if controller.templates.contains(where: { $0 === nowPlaying }) {
            _ = try? await controller.pop(to: nowPlaying, animated: true)
        } else {
            _ = try? await controller.pushTemplate(nowPlaying, animated: true)
        }
    }

    /// Covers load after the row appears so a slow network never delays the list.
    private func loadImage(_ urlString: String, into row: CPListItem) {
        guard let url = URL(string: urlString) else { return }
        let size = CPListItem.maximumImageSize
        Task.detached {
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  let image = UIImage(data: data) else { return }
            let fitted = UIGraphicsImageRenderer(size: size).image { _ in
                image.draw(in: CGRect(origin: .zero, size: size))
            }
            await MainActor.run { row.setImage(fitted) }
        }
    }
}
