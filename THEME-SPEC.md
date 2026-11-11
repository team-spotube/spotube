# Theme Spec: "Feishin" palette family for Spotube V6

Companion to `SCOPE.md` in this folder. Reference: **Feishin** (github.com/jeffvli/feishin) — its theme system (`src/shared/themes/`, `docs/CUSTOM_THEMES.md`), translated onto Spotube V6's 15 `MillenniumColors` roles. Values marked **\[src\]** are verbatim from Feishin's source (rgb() converted to hex); values marked **\[derived\]** follow the stated rule because Feishin's model doesn't have that role. Hand this file to whoever builds the retheme.

## How the mapping works

Feishin themes are flat variable sets — no gradients on accents, no bevel highlights. That flatness *is* the look being imported, so:

| Feishin key | Millennium role(s) | Rule |
| :---- | :---- | :---- |
| `background` | `background` | direct |
| `background-alternate` | `inset` | Feishin's nested-bg ≈ Millennium's inset |
| `surface` | `surface` | direct |
| — | `raised` | **\[derived\]** surface lightened \~5% (dark) / pure white (light), matching Millennium's own family pattern |
| `foreground` | `text` | direct |
| `surface-foreground` | — | equals `text` in every Feishin preset; no separate role needed |
| `foreground-muted` | `muted` | direct |
| `primary` | `accent`, `accentTop`, `accentBottom` | **all three \= primary.** This is the de-gloss: Millennium's gel brush (`accentTop → accentBottom`) renders flat when the stops match. Keep stops within ±3% if a whisper of depth is wanted — never the stock spread |
| — | `onAccent` | **\[derived\]** white on dark accents, near-black on light/pastel accents (per-preset below) |
| — | `border` | **\[derived\]** Feishin exposes no border color (its UI kit derives one). Rule: `text` at \~14% over `background`, precomputed per preset below |
| — | `highlight` | **\[derived\]** Feishin has no bevel sheen. Set \= `surface` (dark) / white (light). It should be invisible-to-barely-there; if a component still looks embossed, the component theme is adding it, not this role |
| `state-error` | `danger` | direct |
| `state-success` | `success` | direct |
| `state-info` | — | no Millennium role; where info color is needed it falls back to `accent` |
| `black` @ \~65% | `scrim` | direct-ish: Feishin overlays run 85%→transparent gradients over black; a flat 65% black scrim is the Millennium-shaped equivalent |

Delivery shape (from SCOPE.md): register these as an **added** palette family in the app's theme wiring (`AppTheme.kt`, settings entry in `AppearanceSection.kt`) — do not edit the built-in Blue/Orange/Lime sets. The family needs six role sets; **Feishin Default (dark) is the primary target**, the rest are alternates that cost nothing extra because they're the same code path.

## The palettes

### 1\. Feishin Default Dark ★ primary

| Role | Value |  |
| :---- | :---- | :---- |
| background | `#0C0C0C` | \[src — Feishin's own doc example for defaultDark\] |
| inset | `#101012` | \[derived: bg-alt direction, near-black step\] |
| surface | `#161618` | \[derived: bg \+ \~4% lift\] |
| raised | `#1E1E20` | \[derived: surface \+ \~4%\] |
| text | `#E8E8E8` | \[derived from Feishin's fg family; presets below carry their own\] |
| muted | `#9A9A9E` | \[derived: text at \~60%\] |
| border | `#2A2A2E` | \[derived: rule above\] |
| highlight | `#161618` | \= surface (rule) |
| accent / accentTop / accentBottom | `#3574FC` | \[src\] |
| onAccent | `#FFFFFF` |  |
| success | `#30D158` | \[src, from Default Light state set — Feishin reuses the family\] |
| danger | `#FF453A` | \[src family; dark-preset error red\] |
| scrim | `#A6000000` | black @ 65% |

### 2\. Feishin Default Light

background `#EBEBEB` \[src\] · inset `#F0F0F0` \[src bg-alt\] · surface `#E1E1E1` \[src\] · raised `#FFFFFF` \[derived\] · text `#191919` \[src\] · muted `#505050` \[src\] · border `#C9C9C9` \[derived\] · highlight `#FFFFFF` · accent ×3 `#007AFF` \[src\] · onAccent `#FFFFFF` · success `#30D158` \[src\] · danger `#FF3B30` \[src\] · scrim `#A6000000`

### 3\. Dracula (alternate, dark)

background `#282A36` \[src\] · inset `#1E1F29` \[src\] · surface `#44475A` \[src\] · raised `#4E5169` \[derived\] · text `#F8F8F2` \[src\] · muted `#BD93F9` \[src — yes, Feishin's Dracula really uses its purple here; if it reads too loud as secondary text, fallback: text at 60%\] · border `#3A3D4F` \[derived\] · highlight \= surface · accent ×3 `#BD93F9` \[src\] · onAccent `#282A36` · success `#50FA7B` \[src\] · danger `#FF5555` \[src\] · scrim `#A6000000`

### 4\. Catppuccin Mocha (alternate, dark)

background `#181825` \[src\] · inset `#11111B` \[src\] · surface `#1E1E2E` \[src\] · raised `#262637` \[derived\] · text `#CDD6F4` \[src\] · muted `#BAC2DE` \[src\] · border `#313244` \[derived\] · highlight \= surface · accent ×3 `#89B4FA` \[src\] · onAccent `#11111B` · success `#A6E3A1` \[src\] · danger `#F38BA8` \[src\] · scrim `#A6000000`

### 5\. Everforest Dark (alternate)

background `#232A2E` \[src\] · inset `#232A2E` \[src — Feishin ships bg-alt identical here\] · surface `#343F38` \[src\] · raised `#3D4A41` \[derived\] · text `#D3C6AA` \[src\] · muted `#D3C6AA` \[src — same caveat as Dracula: verbatim, fallback is text at 60%\] · border `#2E3833` \[derived\] · highlight \= surface · accent ×3 `#A7C080` \[src\] · onAccent `#1A2E05`\-family → use `#232A2E` · success `#A7C080` \[src\] · danger `#E67E80` \[src\] · scrim `#A6000000`

*(Catppuccin Latte and Everforest Light exist in Feishin with the same key structure; add them by the same table if light-mode alternates are ever wanted. Not included: nothing about the primary target needs them.)*

## Added instruction 1 — adaptive color from album artwork (KEEP/ADD)

What the reference does: Feishin extracts the **average color of the current artwork** (`useFastAverageColor`) and lets it tint the player/dynamic surfaces. Spotube V6 today has **no equivalent** — checked: its artwork treatment is blur/haze backdrops in the shell and large player, texture without color adaptation. So this is an *addition*, scoped tightly:

- Extract the average color of the now-playing artwork (downscale to a few px and average; do it off the UI thread, cache per track).  
- Clamp it before use: force saturation ≤ \~45% and lightness into a mid band (dark theme: 18–32%; light: 68–82%). Raw averages are muddy or garish unclamped; the clamp is what keeps it "adaptive tint" instead of "new theme every song."  
- Apply it to exactly two things: **(a)** the large-player background (tint over the `background` role, under the existing haze), and **(b)** the accent *inside the player only*. The app-wide accent stays the palette's — Feishin does not re-theme the whole app per track, and neither should this.  
- Never apply it to `text`/`muted`/`border` (contrast safety), and fall back to the palette accent silently when artwork is missing or extraction fails.  
- Animate the change on track switch with `animateColorAsState`, \~600 ms — a hard cut reads as a glitch; a slow tint shift reads as the feature. (The codebase already uses this exact pattern for lyric highlighting at tween(300).)

## Added instruction 2 — smooth, modern animation, everywhere reasonable

Baseline found in V6's code: motion exists but is utilitarian — `tween` dominates (19 uses), `AnimatedVisibility` 19, `AnimatedContent` 10, `spring` 1, `Crossfade` **0** (artwork changes hard-cut today). Directives for the builder, in priority order:

1. **Color is motion too.** Every role change (palette switch, artwork tint) animates via `animateColorAsState`, 400–600 ms, ease-out. No instant theme swaps.  
2. **Artwork crossfades.** All artwork image swaps (player, queue, cards on track change) get `Crossfade` ≈ 300 ms.  
3. **Springs for size, tweens for opacity.** Player expand/collapse, queue and sidebar panels: spring, medium-low stiffness, no visible overshoot (damping such that scale never exceeds \~1.02). Fades stay on short ease-out tweens, 150–250 ms.  
4. **Screen transitions.** `AnimatedContent` with fade \+ ±8 dp slide, 220–280 ms, consistent direction (forward \= from right).  
5. **Micro-interactions.** Hover/press states 120–150 ms; press scale 0.98 on cards/buttons. Nothing in the UI should change state in 0 ms except text entry.  
6. **Stretch:** honor the OS reduced-motion setting by collapsing the above to crossfades-only. (Cheap to gate centrally if the specs are defined in one place — define them in one place.)

Honesty boundary: directives 1–2 live at theme/wiring level. 3–5 are targeted composable edits in the shell/player — real work beyond token-swapping, i.e. this spec's animation section is a Tier-3 activity (see SCOPE.md), and a full motion redesign of every screen is out of scope for the retheme.

## Contrast guardrails (builder must verify)

- `text` on `background`/`surface`/`raised` ≥ 7:1; `muted` on the same ≥ 4.5:1; `onAccent` on `accent` ≥ 4.5:1, for every shipped role set, *including* with an artwork tint active in the player.  
- The two flagged `muted` values (Dracula, Everforest) are the known risks; use the stated fallbacks if they fail.

## Provenance

Feishin values read from `src/shared/themes/{default-dark, default-light, dracula, catppuccin-mocha, everforest-dark}` and `docs/CUSTOM_THEMES.md` on 2026-11-11. Spotube token model and animation baseline read from team-spotube/millennium `Theme.kt` and the V6 app source the same week. Feishin is GPL-3.0: these are *facts* (published color values) being re-expressed in another app's token model for a personal build — keep this spec file with the fork as the attribution trail.
## Corrections after checking the sources (2026-11-11, Phase 1 build)

The values in `composeApp/src/commonMain/kotlin/org/spotube/spotube/core/ui/theming/AppPalette.kt` are the ones that ship. They differ from the tables above where the sources said otherwise:

- **Feishin Default Dark has real source values** in `src/shared/themes/default.ts` (`default-dark.ts` only overrides `primary`). Used verbatim: background `#0C0C0C`, background-alternate → inset `#080808`, surface `#141414`, foreground → text `#E1E1E1`, foreground-muted → muted `#969696`, primary `#3574FC`, state-success `#32CC32`. Feishin's state-error is `#CC3232`.
- **Millennium draws `accent`, `danger` and `success` as text and icons**, and uses `accentTop`/`accentBottom` for filled buttons. Feishin's primary and error colours are below 4.5:1 as text on these surfaces, so: dark `accent` `#4C84FD` and `danger` `#E05252`; light `accent` `#0058B8`, `danger` `#B8231A`, `success` `#177332`. Filled buttons keep Feishin's exact primary in both modes.
- **White on the primary button** is 4.15:1 (dark) and 4.02:1 (light): Feishin's own look, above the 3:1 minimum for large text and controls, below the 4.5:1 guardrail above. Kept verbatim; darken `accentTop`/`accentBottom` if it reads weak.
- **`highlight` is `#2A2A2A` in dark, not `surface`:** Millennium uses it for icon-button hover feedback, which would vanish at `surface`.
- **Millennium's palette list is a fixed enum of three**, so the family is added as an app-level `AppPalette` (Blue, Orange, Lime, Feishin) stored in a new `appPalette` setting. The built-in role sets are untouched. Dracula, Catppuccin Mocha and Everforest are not in Phase 1.
- Measured contrast, dark: text 15.0 / 14.1 / 13.0 and muted 6.6 / 6.2 / 5.8 on background / surface / raised. Light: text 14.8 / 13.4 / 17.6, muted 6.8 / 6.2 / 8.1.
