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

package org.spotube.spotube.core.ui.theming.engine

/**
 * Themes that ship inside the app. They are ordinary theme documents (same engine, same format a
 * theme author uses) with no assets, so they need no package and can't be removed.
 */
object BuiltInThemes {
    /** The plain Millennium look; its accent (Blue/Orange/Lime) is `UserSettings.palette`. Older `builtin:<palette>` ids mean this too. */
    const val MILLENNIUM_ID = "builtin:millennium"

    /** Settings value of `UserSettings.selectedThemeId` for the Modern theme. */
    const val MODERN_ID = "builtin:modern"

    /** Whether [id] names a theme provided by this object. */
    fun isBuiltIn(id: String?): Boolean = id == MODERN_ID

    private val modern: ThemePackage by lazy { ThemePackage(MODERN_JSON, emptyMap()) }

    /** Parses the Modern theme; the id inside its JSON is internal, the selectable id is [MODERN_ID]. */
    fun loadModern(cacheDir: String): LoadedTheme {
        val base = requireNotNull(loadTheme(modern, cacheDir, LinkedHashSet())) { "built-in Modern theme is invalid" }
        // Modern swaps the Iconsax set for Lucide. Vectors can't be expressed in JSON (a package can't ship
        // code), so only built-in themes carry them; MillenniumIcon applies them to every icon it draws.
        val icons = lucideIconReplacements.mapValues { ThemeIcon.Vector(it.value) }
        return LoadedTheme(
            doc = base.doc, assets = ThemeAssets(icons = icons), problems = base.problems,
            usesItemContext = base.usesItemContext, usesLiveContext = base.usesLiveContext, fontFileCount = 0, iconCount = 0, packageBytes = base.packageBytes,
        )
    }
}

/**
 * "Modern": a neutral, flat look in the spirit of shadcn/ui's zinc palette. Solid surfaces, 1px hairline
 * borders, small radii, no gloss, no hover scaling; the primary action is the foreground colour. Icons are Lucide.
 */
private const val MODERN_JSON = """
{
  "schema": 1,
  "id": "modern",
  "name": "Modern",
  "version": "1.0.0",
  "author": "Spotube Team",
  "description": "Neutral, flat and minimal.",

  "parameters": {
    "accent": {
      "type": "enum", "label": "Accent color", "default": "zinc",
      "options": [
        { "value": "zinc",   "label": "Zinc",   "color": "#18181b" },
        { "value": "blue",   "label": "Blue",   "color": "#2563eb" },
        { "value": "green",  "label": "Green",  "color": "#16a34a" },
        { "value": "orange", "label": "Orange", "color": "#ea580c" },
        { "value": "rose",   "label": "Rose",   "color": "#e11d48" },
        { "value": "violet", "label": "Violet", "color": "#7c3aed" }
      ]
    },
    "radius": { "type": "number", "label": "Corner radius", "default": 6, "min": 0, "max": 16, "step": 1 },
    "density": {
      "type": "enum", "label": "Density", "default": "comfortable",
      "options": [
        { "value": "compact", "label": "Compact" },
        { "value": "comfortable", "label": "Comfortable" },
        { "value": "spacious", "label": "Spacious" }
      ]
    }
  },

  "vars": {
    "none":      { "type": "color", "default": "#00000000" },
    "r":     { "type": "number", "value": "{params.radius}" },
    "rSm":   { "type": "number", "value": ["max", 0, ["sub", "{params.radius}", 2]] },
    "rLg":   { "type": "number", "value": ["add", "{params.radius}", 2] },
    "rXl":   { "type": "number", "value": ["add", "{params.radius}", 6] },
    "padH":  { "type": "number", "value": ["match", "{params.density}", { "compact": 12, "comfortable": 16, "spacious": 20 }] },
    "padV":  { "type": "number", "value": ["match", "{params.density}", { "compact": 6, "comfortable": 8, "spacious": 12 }] },
    "ctl":   { "type": "number", "value": ["match", "{params.density}", { "compact": 34, "comfortable": 40, "spacious": 48 }] },
    "rowH":  { "type": "number", "value": ["match", "{params.density}", { "compact": 44, "comfortable": 56, "spacious": 68 }] },
    "rowPadH": { "type": "number", "value": ["match", "{params.density}", { "compact": 10, "comfortable": 14, "spacious": 18 }] },
    "rowPadV": { "type": "number", "value": ["match", "{params.density}", { "compact": 6, "comfortable": 10, "spacious": 14 }] },
    "bg":        { "type": "color", "value": { "light": "#ffffff", "dark": "#09090b" } },
    "fg":        { "type": "color", "value": { "light": "#09090b", "dark": "#fafafa" } },
    "muted":     { "type": "color", "value": { "light": "#f4f4f5", "dark": "#27272a" } },
    "mutedFg":   { "type": "color", "value": { "light": "#71717a", "dark": "#a1a1aa" } },
    "border":    { "type": "color", "value": { "light": "#e4e4e7", "dark": "#27272a" } },
    "primary":   { "type": "color", "value": ["match", "{params.accent}", {
      "zinc":   { "light": "#18181b", "dark": "#fafafa" },
      "blue":   { "light": "#2563eb", "dark": "#3b82f6" },
      "green":  { "light": "#16a34a", "dark": "#22c55e" },
      "orange": { "light": "#ea580c", "dark": "#f97316" },
      "rose":   { "light": "#e11d48", "dark": "#f43f5e" },
      "violet": { "light": "#7c3aed", "dark": "#8b5cf6" }
    }] },
    "primaryFg": { "type": "color", "value": ["match", "{params.accent}", {
      "zinc": { "light": "#fafafa", "dark": "#18181b" },
      "default": "#ffffff"
    }] },
    "primaryHover": { "type": "color", "value": ["alpha", "{vars.primary}", 0.9] },
    "destructive":   { "type": "color", "value": { "light": "#ef4444", "dark": "#7f1d1d" } },
    "destructiveFg": { "type": "color", "default": "#fafafa" },
    "destructiveHover": { "type": "color", "value": ["alpha", "{vars.destructive}", 0.9] },
    "ring":      { "type": "color", "value": { "light": "#a1a1aa", "dark": "#d4d4d8" } },
    "wash":      { "type": "color", "value": { "light": "#0000000a", "dark": "#ffffff12" } },
    "shadow":    { "type": "color", "value": { "light": "#0000001a", "dark": "#00000080" } }
  },

  "tokens": {
    "color": {
      "background": "{vars.bg}", "surface": "{vars.bg}", "inset": "{vars.muted}",
      "raised": { "light": "#ffffff", "dark": "#18181b" },
      "text": "{vars.fg}", "muted": "{vars.mutedFg}", "border": "{vars.border}", "highlight": "{vars.none}",
      "accent": "{vars.primary}", "accentTop": "{vars.primary}", "accentBottom": "{vars.primary}", "onAccent": "{vars.primaryFg}",
      "success": { "light": "#16a34a", "dark": "#22c55e" }, "danger": { "light": "#dc2626", "dark": "#ef4444" },
      "scrim": "#00000080"
    },
    "shape": { "small": "{vars.rSm}", "medium": "{vars.r}", "large": "{vars.rLg}" },
    "interaction": { "lighting": false },
    "surface": { "shadows": false, "glow": false }
  },

  "defaults": { "shape": "{vars.r}", "innerGlowColor": "{vars.none}", "bottomRimColor": null },

  "components": {
    "button": {
      "base": {
        "borderWidth": 0, "shadows": ["{vars.none}"], "shadowElevation": 0, "minSize": [40, "{vars.ctl}"], "padding": ["{vars.padH}", "{vars.padV}"],
        "interactionOverlay": "{vars.none}", "interactionRim": "{vars.none}", "labelShadow": false,
        "motion": { "hoverScale": 1, "pressedScale": 1 },
        "textStyle": { "fontSize": 14, "lineHeight": 20, "fontWeight": 500 }
      },
      "states": {
        "focused":  { "borderWidth": 2 },
        "disabled": { "alpha": 0.5 }
      },
      "variants": {
        "primary": {
          "base": { "background": "{vars.primary}", "foreground": "{vars.primaryFg}", "borderColor": "{vars.none}" },
          "states": { "hovered": { "background": "{vars.primaryHover}" }, "pressed": { "background": "{vars.primaryHover}" }, "focused": { "borderColor": "{vars.ring}" } }
        },
        "secondary": {
          "base": { "background": "{vars.bg}", "foreground": "{vars.fg}", "borderWidth": 1, "borderColor": "{vars.border}" },
          "states": { "hovered": { "background": "{vars.muted}" }, "pressed": { "background": "{vars.muted}" }, "focused": { "borderColor": "{vars.ring}", "borderWidth": 2 } }
        },
        "quiet": {
          "base": { "background": "{vars.none}", "foreground": "{vars.fg}", "borderColor": "{vars.none}" },
          "states": { "hovered": { "background": "{vars.muted}", "borderWidth": 0 }, "pressed": { "background": "{vars.muted}", "borderWidth": 0 }, "focused": { "borderColor": "{vars.ring}" } }
        },
        "danger": {
          "base": { "background": "{vars.destructive}", "foreground": "{vars.destructiveFg}", "borderColor": "{vars.none}" },
          "states": { "hovered": { "background": "{vars.destructiveHover}" }, "pressed": { "background": "{vars.destructiveHover}" }, "focused": { "borderColor": "{vars.ring}" } }
        }
      }
    },

    "iconButton": {
      "base": {
        "orb": false, "borderWidth": 0, "shadows": ["{vars.none}"], "shadowElevation": 0, "iconSize": 16,
        "interactionOverlay": "{vars.none}", "interactionRim": "{vars.none}", "motion": { "hoverScale": 1, "pressedScale": 1 }
      },
      "states": { "focused": { "borderWidth": 2, "borderColor": "{vars.ring}" }, "disabled": { "alpha": 0.5 } },
      "variants": {
        "primary":   { "base": { "background": "{vars.primary}", "foreground": "{vars.primaryFg}" }, "states": { "hovered": { "background": "{vars.primaryHover}" } } },
        "secondary": { "base": { "background": "{vars.bg}", "foreground": "{vars.fg}", "borderWidth": 1, "borderColor": "{vars.border}" }, "states": { "hovered": { "background": "{vars.muted}" } } },
        "quiet":     { "base": { "background": "{vars.none}", "foreground": "{vars.fg}" }, "states": { "hovered": { "background": "{vars.muted}" } } },
        "danger":    { "base": { "background": "{vars.destructive}", "foreground": "{vars.destructiveFg}" }, "states": { "hovered": { "background": "{vars.destructiveHover}" } } }
      }
    },

    "toggleSwitch": {
      "base": {
        "trackSize": [44, 24], "trackShape": "pill", "thumbShape": "pill",
        "trackBackground": "{vars.border}", "selectedTrackBackground": "{vars.primary}",
        "trackBorderColor": "{vars.none}", "trackGlowColor": "{vars.none}", "trackBottomRimColor": null,
        "thumbBackground": "{vars.bg}", "thumbBorderColor": "{vars.none}", "thumbGlowColor": "{vars.none}", "thumbBottomRimColor": null,
        "thumbElevation": 0, "trackInteractionOverlay": "{vars.none}", "thumbInteractionOverlay": "{vars.none}",
        "thumbHoverScaleX": 1, "thumbHoverScaleY": 1, "thumbPressedScaleX": 1, "thumbPressedScaleY": 1
      },
      "states": { "disabled": { "alpha": 0.5 }, "focused": { "trackBorderColor": "{vars.ring}" } }
    },

    "checkbox": {
      "base": {
        "faceShape": "{vars.rSm}", "faceSize": [16, 16], "faceBackground": "{vars.bg}", "faceBorderColor": "{vars.primary}", "faceBorderWidth": 1,
        "selectedFaceBackground": "{vars.primary}", "selectedFaceBorderColor": "{vars.primary}", "selectedFaceForeground": "{vars.primaryFg}",
        "faceGlowColor": "{vars.none}", "selectedFaceGlowColor": "{vars.none}", "faceElevation": 0, "faceBottomRimColor": null,
        "faceInteractionOverlay": "{vars.none}", "faceHoverScale": 1, "facePressedScale": 1
      },
      "states": { "disabled": { "faceAlpha": 0.5 }, "focused": { "faceBorderColor": "{vars.ring}", "faceBorderWidth": 2 } }
    },

    "slider": {
      "base": {
        "trackBackground": "{vars.muted}", "trackBorderColor": "{vars.none}", "trackInsetShadeColor": "{vars.none}", "trackShape": "pill",
        "trackInnerGlowColor": "{vars.none}", "trackBottomRimColor": null,
        "activeTrackBackground": "{vars.primary}", "activeTrackShape": "pill", "activeTrackGlowColor": "{vars.none}",
        "thumbBackground": "{vars.bg}", "thumbBorderColor": "{vars.primary}", "thumbBorderWidth": 2, "thumbShape": "pill",
        "thumbGlowColor": "{vars.none}", "thumbBottomRimColor": null, "thumbElevation": 0,
        "trackInteractionOverlay": "{vars.none}", "thumbInteractionOverlay": "{vars.none}"
      },
      "states": { "disabled": { "alpha": 0.5 } }
    },

    "progress": {
      "base": {
        "shape": "pill", "height": 8, "trackBackground": "{vars.muted}", "borderColor": "{vars.none}", "borderWidth": 0,
        "progressBackground": "{vars.primary}", "progressShape": "pill", "progressInnerGlowColor": "{vars.none}", "progressBottomRimColor": null,
        "trackBottomRimColor": null
      }
    },

    "textField": {
      "base": {
        "background": "{vars.bg}", "borderColor": "{vars.border}", "borderWidth": 1, "shape": "{vars.r}", "minimumHeight": "{vars.ctl}",
        "insetShadeColor": "{vars.none}", "cursorColor": "{vars.fg}", "interactionOverlay": "{vars.none}",
        "placeholderColor": "{vars.mutedFg}", "labelColor": "{vars.fg}"
      },
      "states": { "focused": { "borderColor": "{vars.ring}", "borderWidth": 2 }, "disabled": { "alpha": 0.5 } }
    },

    "card": {
      "base": {
        "shape": "{vars.rXl}", "background": "{vars.bg}", "borderColor": "{vars.border}", "borderWidth": 1,
        "shadowElevation": 1, "shadowColor": "{vars.shadow}", "insetShadeColor": "{vars.none}", "interactionOverlay": "{vars.none}"
      }
    },

    "listTile": {
      "base": {
        "shape": "{vars.r}", "contentPadding": ["{vars.rowPadH}", "{vars.rowPadV}"], "minimumHeight": "{vars.rowH}", "background": "{vars.none}", "selectedBackground": "{vars.muted}", "borderWidth": 0, "shadowElevation": 0,
        "titleForeground": "{vars.fg}", "subtitleForeground": "{vars.mutedFg}",
        "selectedTitleForeground": "{vars.fg}", "selectedSubtitleForeground": "{vars.mutedFg}",
        "interactionOverlay": "{vars.none}"
      },
      "states": {
        "hovered": { "interactionOverlay": ["gradient", "{vars.wash}", "{vars.wash}"] },
        "pressed": { "interactionOverlay": ["gradient", "{vars.wash}", "{vars.wash}"] },
        "focused": { "interactionOverlay": ["gradient", "{vars.wash}", "{vars.wash}"] }
      }
    },

    "icon": { "base": { "size": 16 } },

    "separator": { "base": { "color": "{vars.border}", "highlightColor": "{vars.none}", "thickness": 1 } },

    "dialog": {
      "base": {
        "shape": "{vars.rLg}", "panelBackground": "{vars.bg}", "panelBorderColor": "{vars.border}", "panelBorderWidth": 1,
        "panelGlowColor": "{vars.none}", "panelBottomRimColor": null, "panelShadowElevation": 18, "panelShadowColor": "{vars.shadow}",
        "titleBarBackground": "{vars.bg}", "titleBarForeground": "{vars.fg}", "titleBarBorderColor": "{vars.none}", "titleBarDividerThickness": 0,
        "footerBackground": "{vars.bg}", "footerBorderColor": "{vars.none}", "footerDividerThickness": 0,
        "scrimColor": "#00000080", "descriptionForeground": "{vars.mutedFg}",
        "closeCapBackground": "{vars.none}", "closeCapForeground": "{vars.mutedFg}", "closeCapBorderColor": "{vars.none}", "closeCapBorderWidth": 0,
        "closeCapGlowColor": "{vars.none}", "closeCapShape": "{vars.rSm}", "closeCapElevation": 0, "closeCapRaisedElevation": 0, "closeCapPressedElevation": 0,
        "closeCapTint": "{vars.none}", "closeCapFocusBorderColor": "{vars.ring}", "closeScaleHover": 1, "closeScalePressed": 1
      },
      "states": {
        "hovered": { "closeCapBackground": "{vars.muted}", "closeCapForeground": "{vars.fg}" },
        "pressed": { "closeCapBackground": "{vars.muted}", "closeCapForeground": "{vars.fg}" },
        "focused": { "closeCapForeground": "{vars.fg}" }
      }
    },

    "dropdownMenu": {
      "base": {
        "panelBackground": "{vars.bg}", "panelShape": "{vars.r}", "panelBorderColor": "{vars.border}", "panelBorderWidth": 1, "panelGlowColor": "{vars.none}",
        "panelShadowElevation": 6, "panelShadowColor": "{vars.shadow}", "iconSize": 16, "itemShape": "{vars.rSm}", "itemForeground": "{vars.fg}",
        "itemActiveBackground": "{vars.muted}", "itemSelectedBackground": "{vars.muted}", "itemActiveForeground": "{vars.fg}", "itemActiveElevation": 0,
        "itemHoverScale": 1, "itemPressedScale": 1, "itemMutedForeground": "{vars.mutedFg}", "groupLabelForeground": "{vars.mutedFg}",
        "separatorBrush": "{vars.border}", "separatorThickness": 1,
        "itemBorderColor": "{vars.none}", "itemBorderWidth": 0, "itemGlowColor": "{vars.none}", "itemActiveBorderColor": "{vars.none}",
        "itemOrbColor": null, "itemBottomRimColor": null, "itemShadowColor": "{vars.none}", "itemOverlayBrush": "{vars.none}",
        "itemIconTint": "{vars.mutedFg}", "itemDestructiveBackground": "{vars.muted}", "itemDestructiveForeground": "{vars.destructive}"
      },
      "states": {
        "hovered":  { "itemIconTint": "{vars.fg}", "itemBackground": "{vars.muted}" },
        "focused":  { "itemIconTint": "{vars.fg}", "itemBackground": "{vars.muted}" },
        "pressed":  { "itemIconTint": "{vars.fg}", "itemBackground": "{vars.muted}" },
        "selected": { "itemIconTint": "{vars.fg}", "itemBackground": "{vars.muted}" }
      }
    },

    "tooltip": {
      "base": { "background": "{vars.primary}", "foreground": "{vars.primaryFg}", "shape": "{vars.r}", "borderWidth": 0, "shadowElevation": 0 }
    },

    "tabGroup": {
      "base": {
        "listBackground": "{vars.muted}", "listShape": "{vars.r}", "listBorderColor": "{vars.none}", "listBorderWidth": 0, "listShadowElevation": 0,
        "tabShape": "{vars.rSm}", "selectedTabBackground": "{vars.bg}", "selectedTabForeground": "{vars.fg}", "unselectedTabForeground": "{vars.mutedFg}",
        "selectedTabBorderColor": "{vars.none}", "tabGlowColor": "{vars.none}", "tabElevation": 1, "tabHoverScale": 1, "tabPressedScale": 1
      }
    },

    "segmentedButton": {
      "base": {
        "background": "{vars.bg}", "foreground": "{vars.fg}", "borderColor": "{vars.border}", "borderWidth": 1, "shadowElevation": 0,
        "interactionOverlay": "{vars.none}", "interactionRim": "{vars.none}", "motion": { "hoverScale": 1, "pressedScale": 1 }
      },
      "states": { "hovered": { "background": "{vars.muted}" }, "selected": { "background": "{vars.muted}" } }
    },

    "scrollbar": {
      "base": { "trackBackground": "{vars.none}", "thumbBackground": "{vars.border}", "thumbBorderColor": "{vars.none}", "thumbInnerGlowColor": "{vars.none}", "thumbBottomRimColor": null, "trackInnerGlowColor": "{vars.none}", "trackBottomRimColor": null, "thumbInteractionOverlay": "{vars.none}" },
      "states": { "hovered": { "thumbBackground": "{vars.mutedFg}" }, "pressed": { "thumbBackground": "{vars.mutedFg}" } }
    },

    "snackbar": {
      "base": { "background": "{vars.bg}", "foreground": "{vars.fg}", "borderColor": "{vars.border}", "borderWidth": 1, "shape": "{vars.r}", "shadowElevation": 8, "shadowColor": "{vars.shadow}" }
    },

    "topAppBar": { "base": { "background": "{vars.bg}", "separatorColor": "{vars.border}" } },

    "bottomNavigationBar": {
      "variants": {
        "main": {
          "base": {
            "background": "{vars.bg}", "foreground": "{vars.fg}", "shape": "rect", "borderWidth": 0, "shadowElevation": 0,
            "minimumHeight": 64, "contentPadding": [8, 6], "itemSpacing": 4,
            "overlayBrush": ["linear", 180, ["{vars.border}", 0], ["{vars.border}", 0.02], ["#00000000", 0.02], ["#00000000", 1]]
          }
        },
        "item": {
          "base": {
            "background": "{vars.none}", "foreground": "{vars.mutedFg}", "shape": "{vars.r}", "minimumHeight": 52, "iconSize": 20, "borderWidth": 0, "borderColor": "{vars.none}",
            "shadowElevation": 0, "innerGlowColor": "{vars.none}", "bottomRimColor": null, "orbColor": null, "overlayBrush": "{vars.none}",
            "hoverScale": 1, "pressedScale": 1, "labelStyle": { "fontSize": 12, "fontWeight": 500 }
          },
          "states": {
            "hovered":  { "background": "{vars.muted}", "foreground": "{vars.fg}" },
            "pressed":  { "background": "{vars.muted}", "foreground": "{vars.fg}" },
            "focused":  { "background": "{vars.muted}", "foreground": "{vars.fg}", "borderWidth": 2, "borderColor": "{vars.ring}" },
            "selected": { "background": "{vars.muted}", "foreground": "{vars.fg}", "labelStyle": { "fontWeight": 700 } },
            "disabled": { "alpha": 0.5 }
          }
        }
      }
    },

    "sidebar": {
      "variants": {
        "root": {
          "base": {
            "background": "{vars.bg}", "foreground": "{vars.fg}", "shape": "rect", "borderWidth": 0, "shadowElevation": 0,
            "overlayBrush": ["linear", 90, ["#00000000", 0], ["#00000000", 0.995], ["{vars.border}", 0.995], ["{vars.border}", 1]]
          }
        },
        "header": { "base": { "background": "{vars.none}", "dividerColor": "{vars.none}", "dividerWidth": 0, "dividerHighlightColor": "{vars.none}" } },
        "footer": { "base": { "background": "{vars.none}", "dividerColor": "{vars.border}", "dividerWidth": 1, "dividerHighlightColor": "{vars.none}" } },
        "group": {
          "base": {
            "labelForeground": "{vars.mutedFg}", "labelHoveredBackground": "{vars.muted}", "labelFocusedBackground": "{vars.muted}", "labelPressedBackground": "{vars.muted}",
            "labelBorderColor": "{vars.none}", "labelBorderWidth": 0, "labelInnerGlowColor": "{vars.none}", "labelShape": "{vars.r}", "labelOverlayBrush": "{vars.none}",
            "separatorBrush": "{vars.border}"
          }
        },
        "item": {
          "base": {
            "background": "{vars.none}", "foreground": "{vars.fg}", "iconTint": "{vars.mutedFg}", "iconSize": 16, "shape": "{vars.r}", "borderWidth": 0, "borderColor": "{vars.none}",
            "shadowElevation": 0, "innerGlowColor": "{vars.none}", "bottomRimColor": null, "orbColor": null, "overlayBrush": "{vars.none}",
            "hoverScale": 1, "pressedScale": 1, "hoverBackground": "{vars.muted}", "focusBackground": "{vars.muted}", "pressedBackground": "{vars.muted}",
            "selectedBackground": "{vars.muted}", "selectedIndicatorColor": "{vars.none}", "selectedIndicatorBrush": "{vars.none}", "selectedIndicatorWidth": 0
          },
          "states": {
            "hovered":  { "background": "{vars.muted}", "iconTint": "{vars.fg}" },
            "focused":  { "background": "{vars.muted}", "iconTint": "{vars.fg}", "borderWidth": 2, "borderColor": "{vars.ring}" },
            "pressed":  { "background": "{vars.muted}", "iconTint": "{vars.fg}" },
            "selected": { "background": "{vars.muted}", "iconTint": "{vars.fg}", "labelStyle": { "fontWeight": 600 } }
          }
        }
      }
    }
  }
}
"""
