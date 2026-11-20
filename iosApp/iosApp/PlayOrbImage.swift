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

import UIKit

/// The glass Play orb, drawn for CarPlay. A port of the Android widget's `PlayOrb`, itself a port of
/// Millennium's Primary icon button (the player's play button), in the dark Blue palette. CarPlay draws its
/// own transport buttons and can't restyle them, but a list row's icon is just an image.
enum PlayOrbImage {
    private static let top = UIColor(red: 0x19 / 255, green: 0x76 / 255, blue: 0xB9 / 255, alpha: 1) // accentTop
    private static let bottom = UIColor(red: 0, green: 0x2D / 255, blue: 0x74 / 255, alpha: 1) // accentBottom
    private static let strength: CGFloat = 0.48 // MillenniumIconButton's default orbStrength

    static func make(size: CGSize) -> UIImage {
        UIGraphicsImageRenderer(size: size).image { renderer in
            draw(renderer.cgContext, side: min(size.width, size.height))
        }
    }

    private static func draw(_ ctx: CGContext, side s: CGFloat) {
        let half = s / 2
        let bounds = CGRect(x: 0, y: 0, width: s, height: s)
        let space = CGColorSpaceCreateDeviceRGB()

        ctx.saveGState()
        ctx.addEllipse(in: bounds)
        ctx.clip()

        // Body: dark jewel glass, brightest only at the lower lip.
        let body = blend(bottom, .black, 0.52)
        gradient(ctx, space, [blend(bottom, body, 0.65), body, body, blend(body, top, 0.40), top],
                 [0, 0.28, 0.60, 0.82, 1], from: CGPoint(x: 0, y: 0), to: CGPoint(x: 0, y: s))

        // A light pool under the sphere.
        let glow = blend(top, UIColor(red: 0x65 / 255, green: 0xE8 / 255, blue: 0xFF / 255, alpha: 1), 0.65)
        radial(ctx, space,
               [glow.withAlphaComponent(strength * 0.95), glow.withAlphaComponent(strength * 0.65),
                glow.withAlphaComponent(strength * 0.10), glow.withAlphaComponent(0)],
               [0, 0.40, 0.78, 1], center: CGPoint(x: half, y: s * 1.12), radius: s * 0.66)

        // The curved glass dome on the upper half.
        let dome = CGMutablePath()
        dome.move(to: CGPoint(x: s * 0.08, y: s * 0.37))
        dome.addCurve(to: CGPoint(x: s * 0.92, y: s * 0.37),
                      control1: CGPoint(x: s * 0.12, y: -s * 0.04), control2: CGPoint(x: s * 0.88, y: -s * 0.04))
        dome.addCurve(to: CGPoint(x: s * 0.70, y: s * 0.38),
                      control1: CGPoint(x: s * 0.94, y: s * 0.49), control2: CGPoint(x: s * 0.81, y: s * 0.39))
        dome.addCurve(to: CGPoint(x: s * 0.12, y: s * 0.44),
                      control1: CGPoint(x: s * 0.48, y: s * 0.32), control2: CGPoint(x: s * 0.28, y: s * 0.43))
        dome.addCurve(to: CGPoint(x: s * 0.08, y: s * 0.37),
                      control1: CGPoint(x: s * 0.08, y: s * 0.45), control2: CGPoint(x: s * 0.07, y: s * 0.41))
        dome.closeSubpath()
        ctx.saveGState()
        ctx.addPath(dome)
        ctx.clip()
        radial(ctx, space,
               [UIColor.white.withAlphaComponent(strength), UIColor.white.withAlphaComponent(strength * 0.42),
                UIColor.white.withAlphaComponent(strength * 0.03)],
               [0, 0.50, 1], center: CGPoint(x: s * 0.46, y: s * 0.04), radius: s * 0.63)
        ctx.restoreGState()

        // Metallic rim, then the thin inner rim lit from above and below.
        let rim = max(s / 46, s * 0.025)
        stroke(ctx, width: rim * 2, in: bounds) {
            gradient(ctx, space, [hex(0x26364F), hex(0x66748A), hex(0xC6D4DA), hex(0x89949D)],
                     [0, 0.28, 0.62, 1], from: .zero, to: CGPoint(x: s * 0.65, y: s))
        }
        let inset = rim * 1.7
        stroke(ctx, width: s * 0.018, in: bounds.insetBy(dx: inset, dy: inset)) {
            gradient(ctx, space,
                     [UIColor.white.withAlphaComponent(strength * 0.70), UIColor.white.withAlphaComponent(0),
                      UIColor.white.withAlphaComponent(0), glow.withAlphaComponent(strength * 0.75)],
                     [0, 0.35, 0.75, 1], from: CGPoint(x: 0, y: inset), to: CGPoint(x: 0, y: s - inset))
        }
        ctx.restoreGState()

        // The play glyph, at the player's 30-in-46 proportion (a 24 unit box).
        let scale = s * (30.0 / 46.0) / 24
        let offset = (s - 24 * scale) / 2
        ctx.translateBy(x: offset, y: offset)
        ctx.scaleBy(x: scale, y: scale)
        ctx.move(to: CGPoint(x: 8, y: 5))
        ctx.addLine(to: CGPoint(x: 8, y: 19))
        ctx.addLine(to: CGPoint(x: 19, y: 12))
        ctx.closePath()
        ctx.setFillColor(UIColor.white.cgColor)
        ctx.fillPath()
    }

    private static func gradient(
        _ ctx: CGContext, _ space: CGColorSpace, _ colors: [UIColor], _ locations: [CGFloat],
        from start: CGPoint, to end: CGPoint
    ) {
        guard let gradient = CGGradient(colorsSpace: space, colors: colors.map(\.cgColor) as CFArray, locations: locations)
        else { return }
        ctx.drawLinearGradient(gradient, start: start, end: end, options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
    }

    private static func radial(
        _ ctx: CGContext, _ space: CGColorSpace, _ colors: [UIColor], _ locations: [CGFloat],
        center: CGPoint, radius: CGFloat
    ) {
        guard let gradient = CGGradient(colorsSpace: space, colors: colors.map(\.cgColor) as CFArray, locations: locations)
        else { return }
        ctx.drawRadialGradient(gradient, startCenter: center, startRadius: 0, endCenter: center, endRadius: radius,
                               options: [.drawsAfterEndLocation])
    }

    /// Paints `fill` through the outline of an ellipse's stroke.
    private static func stroke(_ ctx: CGContext, width: CGFloat, in rect: CGRect, fill: () -> Void) {
        ctx.saveGState()
        ctx.setLineWidth(width)
        ctx.addEllipse(in: rect)
        ctx.replacePathWithStrokedPath()
        ctx.clip()
        fill()
        ctx.restoreGState()
    }

    private static func hex(_ value: Int) -> UIColor {
        UIColor(red: CGFloat((value >> 16) & 0xFF) / 255, green: CGFloat((value >> 8) & 0xFF) / 255,
                blue: CGFloat(value & 0xFF) / 255, alpha: 1)
    }

    private static func blend(_ a: UIColor, _ b: UIColor, _ t: CGFloat) -> UIColor {
        var (ar, ag, ab, aa) = (CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0))
        var (br, bg, bb, ba) = (CGFloat(0), CGFloat(0), CGFloat(0), CGFloat(0))
        a.getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
        b.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        return UIColor(red: ar + (br - ar) * t, green: ag + (bg - ag) * t, blue: ab + (bb - ab) * t, alpha: 1)
    }
}
