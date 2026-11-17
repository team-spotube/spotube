# Spotube themes

A theme is a **package** (a `.zip`), like a plugin: it has metadata (author, repository, license, logo), it is installed from **Settings → Look & feel → Themes**, shows up as a card next to the built-in looks (the Millennium palettes and the bundled Modern theme, which is itself written in this format), and can be selected, updated or removed. Build one by zipping a folder that has `theme.json` at its root (for example `cd my-theme && zip -r ../my-theme.zip .`). Any `composeApp/themes/<name>/` folder you add here is packed, loaded and rendered (light and dark) by `ThemeGalleryTest`, which fails on warnings or missing metadata.

## Package layout
```
my-theme.zip
├── theme.json        required, at the root
├── logo.png          optional, referenced by "logo" (max 512 x 512)
├── LICENSE           optional, shown on the theme's card
├── fonts/*.ttf|otf   optional
└── icons/*.png       optional
```

### Metadata (top of `theme.json`)
| Field | Required | Notes |
|---|---|---|
| `id` | yes | `A-Z a-z 0-9 . _ -`, up to 48 chars; installing the same id again updates the theme |
| `name`, `version`, `author` | yes | |
| `description` | no | up to 500 chars |
| `repository`, `bugs`, `homepage` | no | `http(s)` URLs only; "Report an issue" opens `bugs` |
| `contact` | no | an email address or an `http(s)` URL |
| `license` | no | SPDX id or a short name; ship the text as `LICENSE` |
| `logo` | no | path of a `.png` inside the zip |
| `schema` | no | currently `1` |

## How a theme styles the app
Two modes in one file, both optional:

* **Global**: `tokens.interaction` and `tokens.surface` (see below), `tokens.color` (replaces any of the 15 palette roles: `background surface raised inset text muted border highlight accent accentTop accentBottom onAccent success danger scrim`), `tokens.shape` (`small medium large`), `tokens.type` (`family`, `scale`, `styles.<name>`), and `defaults` (props applied to every component that has them, e.g. `{ "shape": 12 }`).
* **Per component**: `components.<name>` with `base`, `states`, `variants`, `rules`. Component names are Millennium's component themes without the `Theme` suffix: `button iconButton segmentedButton toggleSwitch checkbox triStateCheckbox radioGroup slider progress loading textField autocomplete card listTile avatar tooltip scrollbar separator tabGroup disclosure dialog drawer bottomSheet modalBottomSheet dropdownMenu sidebar bottomNavigationBar topAppBar scaffold snackbar pullToRefresh icon text`.

Cascade, later wins: `defaults` → `base` → `states.<state>` → `variants.<v>.base` → `variants.<v>.states.<state>` → matching `rules`. States: `default hovered focused pressed disabled selected error`. Components with several slots use `variants` for them: `button`/`iconButton` → `primary secondary quiet danger`; `sidebar` → `root header content footer group item badge trigger`; `bottomNavigationBar` → `main item`. **A `base` applies to every slot of a component**, so for these always put slot properties under `variants` (a border set in `bottomNavigationBar.base` would also box every dock item).

The dock (`bottomNavigationBar`) is fully themeable: `main` takes the bar's `background`, `shape`, `borderColor/Width`, `shadowElevation`, `overlayBrush`, `contentPadding`, `itemSpacing`, `minimumHeight` and, for a floating inset dock, `margin` plus `insetsInsideSurface: false`; `item` takes per-state `background`, `foreground`, `shape`, `labelStyle`, borders, scales and effects (`states.selected` is the active tab).

**Property names are Millennium's own** (`background`, `shape`, `borderWidth`, `shadowElevation`, `innerGlowColor`, `trackBackground`, `thumbBackground`, ...). Every property a Millennium component theme declares is available; the exhaustive list with docs is the KDoc of the `Millennium*ThemeProperties` classes, and the generated mapping is `core/ui/theming/engine/GeneratedThemeProps.kt`. Properties a component doesn't have are reported as `unknown property` warnings on the theme's card, so typos don't fail silently. The bundled Modern theme (`BuiltInThemes.kt`) is a complete worked example.

### Surfaces (shadows and glow)
Millennium surfaces (including ones the host app builds itself, like the settings cards) draw drop shadows and inner glow/rims. `tokens.surface` switches them for every surface at once: `{ "shadows": false, "glow": false }` makes everything flat. Flat themes should set it: besides matching the design, a shadow layer per element is the most expensive part of drawing Millennium, so it is also faster.

### Hover and press lighting
Controls that have no lighting property of their own (sidebar, dropdown and bottom-navigation rows, tabs, the dialog buttons) draw Millennium's glossy Aqua hover/press lighting. `tokens.interaction` changes it for all of them at once: `{ "lighting": false }` removes it (a flat design lets the per-state `background`s carry hover/selection), or give your own washes: `{ "hover": <brush>, "press": <brush> }`. Where a component has its own `interactionOverlay` property, that wins for that component.

Millennium draws hover/press lighting itself; a theme replaces it with the component's `interactionOverlay` / `interactionRim` / `interactionTint` properties, and a transparent value switches it off. Put such an overlay only in the states that should show it (a value in `base` is painted in every state).

## Values
| Kind | JSON |
|---|---|
| number / dp | `12` |
| color | `"#RGB"`, `"#RRGGBB"`, `"#RRGGBBAA"` |
| brush | a color, or a gradient (below) |
| shape | `12`, `"pill"`, `"rect"`, `[topStart, topEnd, bottomEnd, bottomStart]` |
| padding | `12`, `[horizontal, vertical]`, `[start, top, end, bottom]`, `{horizontal, vertical, start, top, end, bottom}` |
| text style | `{ "fontSize", "lineHeight", "fontWeight", "letterSpacing", "fontStyle", "fontFamily", "color", "textDecoration", "shadow": {"color","offsetX","offsetY","blurRadius"} }`, merged onto the default |
| size | `[width, height]` |
| motion (buttons) | `{ "stateChangeMillis", "hoverScale", "pressedScale", "scaleDampingRatio", ... }` |
| clear a nullable prop | `null` |

### Brushes
Millennium paints buttons, tracks, thumbs, fills and panels with `Brush`es, so a theme can restyle them completely. Stops are a color or `[color, position]` (position 0..1; missing positions are spread evenly).

* `["linear", angle, stop, stop, ...]`: CSS angle (0 = up, 90 = right, 180 = down), corners get the end colors exactly.
* `["gradient", c1, c2, ...]`: a vertical linear gradient (top to bottom).
* `["radial", cx, cy, radius, stop, ...]`: centre and radius as fractions of the box (radius of the larger side).
* `["sweep", cx, cy, stop, ...]`: a conic gradient.
* `["gel", top, bottom]`: Millennium's convex Aqua gel.

Coordinates are fractions of the painted box, so one theme fits a 40dp icon button and a full-width card.

## References, functions, conditions
* `"{tokens.color.accent}"`: the theme's **effective** value (what `tokens` resolved to). `"{palette.accent}"` is the **built-in** Millennium value, before your overrides, useful as a base (`["lighten", "{palette.surface}", 0.04]`). `"{vars.x}"` is a variable (below). `"{ctx.env.dark}"`, `"{ctx.state}"` etc. are live context.
* `{ "light": x, "dark": y }` picks by appearance anywhere a value is allowed. Use it for every hardcoded brush color, otherwise one appearance is unreadable.
* Functions: `["if", cond, a, b]`, `["match", v, {"a": 1, "default": 2}]`, `alpha lighten darken mix add sub mul div min max clamp`.
* Conditions (a rule's `when`, or an `if` test): `all`, `any`, `not`, `state`, `variant`, and `{ "var": "ctx.x", "eq"|"ne"|"in"|"gt"|"gte"|"lt"|"lte": ... }`; several keys in one object are ANDed.
* Live context: `ctx.env.dark`, `ctx.env.compact`, `ctx.player.{isPlaying,isPaused,isBuffering,hasTrack,shuffle,loop}`, and `ctx.item.{current,playing}` on track rows.

### Variables
`vars` is the theme author's own token store: `{ "type": "color|number|bool|string|enum", "default": ..., "min", "max", "options" }`, or `"value": <expression>` for a derived one. Declare colors, radii and the like once, reference them as `{vars.name}` everywhere, change them in one place. Types are validated; cycles are rejected. A component can declare its own `vars`, which shadow global ones. Variables are never shown to users.

## Fonts and icons
```jsonc
"assets": {
  "fonts": { "Comfortaa": [ { "src": "fonts/Comfortaa-Regular.otf", "weight": 400 },
                            { "src": "fonts/Comfortaa-Bold.otf",    "weight": 700 } ] },
  "icons": {
    "IconsaxHome": { "glyph": "U+E900", "font": "MyIcons" },     // a glyph of an icon font declared above
    "IconsaxPlay": { "image": "icons/play.png", "tint": true }    // or a PNG, tinted like the stock icon
  }
},
"tokens": { "type": { "family": "Comfortaa" } }
```
`tokens.type.family` applies the font everywhere; any `textStyle` can override with `fontFamily`. Built-in family names need no assets: `default sans-serif serif monospace cursive`. Icon keys are the app's icon vector names (see `composeApp/src/commonMain/kotlin/org/spotube/spotube/resources/iconsax`). Icons drawn by Millennium components (sidebar, icon buttons, checkbox ticks, chevrons) are covered too, but only by built-in themes: swapping them needs real `ImageVector`s, which a package (pure data) can't carry. The bundled Modern theme replaces the whole Iconsax set with Lucide this way (`ModernLucideIcons.kt`). Fonts: `.ttf`/`.otf` only. Images: `.png` only, max 512 x 512. No SVG.

## Security model
A theme is untrusted input and is sandboxed by construction:
* **Never extracted to disk.** The zip is read in memory; entry names are never used as paths ("zip slip" has no target). Android needs font *files*, which go to the cache under a content-hash name we choose.
* **Strict allowlist.** Only `theme.json` (root), `.ttf`/`.otf`, `.png` and inert `.txt`/`.md`/`LICENSE` are accepted; any other entry (executables, scripts, `.svg`, nested archives, a second `.json`) rejects the whole archive. Absolute paths, `..`, backslashes, drive letters, control characters, hidden files, long or deep names and case-insensitive duplicates are rejected; macOS/Windows metadata (`__MACOSX/`, `.DS_Store`, `Thumbs.db`) is skipped.
* **Hard size limits, counted while reading** (a zip bomb stops at the cap): archive 16 MB, 128 entries, 32 MB total, `theme.json` 512 KB, font 8 MB, PNG 256 KB, at most 16 fonts and 128 icon images.
* **Structural validation before any native decoder runs:** fonts need a sane table directory (in-range offsets, required tables, valid `head`/`maxp`/`loca`, no SVG table); PNGs need valid chunk framing and CRCs and at most 512 px.
* **Bounded evaluation:** JSON nesting and expression depth are capped, and a global step budget stops exponential `{ref}` graphs (the theme is then ignored with a warning, never a hang).
* **Links are validated:** repository, bugs and homepage must be `http(s)` URLs; `contact` an email or `http(s)` URL. Never `file:`, `javascript:` or similar.
* **No capabilities:** pure data. No scripting, no network, no file access. A font or icon `src` is only looked up inside the package.
* **Consent and integrity:** an import is validated first and only installed after the user confirms a summary (author, links, license, contents, size). Installed packages are re-validated with the same reader on every launch; a corrupt file, or one stored under another theme's id, is skipped.

Residual risk, stated plainly: fonts and PNGs are ultimately decoded by native libraries (Skia/FreeType, the OS). Validation removes malformed-structure cases but cannot make a native parser bug impossible, which is why installing requires consent and the dialog says to only install themes from people you trust.
