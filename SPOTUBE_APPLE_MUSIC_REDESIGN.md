# Spotube — Apple Music-Inspired UI Redesign

> Complete presentation-layer redesign specification for Spotube.
> Visual reference: https://music.apple.com/us/new
> Repository: https://github.com/abazic/spotube-to-apple-music

## 1. Mission

Redesign the entire visible Spotube interface so it has the editorial hierarchy, restrained surfaces, artwork-led content, navigation clarity, and polished controls associated with Apple Music Web. This applies to every reachable screen and shared UI surface: application shell, sidebar/navigation, page headers, cards, lists, icons, buttons, dialogs, menus, settings, plugin screens, and playback UI.

This is a visual redesign, not a product rewrite. Retain Spotube's name, identity, assets, and existing capabilities. Apple Music is a visual reference only; do not copy proprietary code, logos, or assets.

## 2. Hard boundaries: preserve all behavior

### Allowed: presentation-only changes
- Layout, sizing, spacing, alignment, responsive composition, and visual hierarchy.
- Sidebar appearance and grouping, provided every existing destination/action remains available and its semantics are unchanged.
- Icon glyphs, size, stroke, placement, and visual treatment, provided meaning and accessibility labels remain correct.
- Button/control shape, color, padding, typography, hover/focus/pressed/disabled states.
- Colors, typography, surfaces, borders, dividers, shadows, corner radii, artwork presentation, cards, lists, and player styling.
- Reusable visual components and theme tokens.

### Forbidden: behavior, backend, and system changes
Do not modify:
- Playback engine/audio pipeline, queue semantics, playback state, seeking, volume, shuffle, repeat, device/media integration.
- Plugin architecture, plugin APIs, plugin loading/execution, plugin settings semantics, compatibility, or extension points.
- Music providers, metadata/search sources, networking, authentication, persistence, caching, downloads, or data models.
- ViewModels, repositories, services, use cases, domain logic, business rules, or state ownership.
- Navigation destinations, route semantics, deep links, screen flow, or user task sequences.
- Keyboard shortcuts, context menus, drag-and-drop, gestures, accessibility actions, or platform integrations.
- Cross-platform targets, build configuration, or platform initialization.

Do not remove, hide, repurpose, or make harder to discover any existing function. Do not introduce new product behavior to imitate Apple Music. Existing UX flows and functional meanings must remain intact. Presentation wiring may change only when strictly necessary and must preserve the same events and state.

## 3. Mandatory repository reconnaissance

Before editing UI code:
1. Read `AGENTS.md` and `README.md` completely.
2. Inspect the repository tree and identify all UI entry points and modules under `composeApp`.
3. Identify shared UI in `commonMain` and platform-specific source sets (desktop/JVM, Android, iOS, etc.).
4. Trace the navigation graph and enumerate all reachable screens.
5. Locate theme, typography, color, dimensions, icon dependencies, reusable components, sidebar, and global player.
6. Map each visible control to its existing callback/action. Record behavior before restyling.
7. Inspect ViewModels and UiState only to understand their presentation contract; do not move logic into Composables.
8. Create a screen/component checklist and implement in small, reviewable phases.

Do not guess paths, symbols, or architecture. Search for declarations and follow existing patterns.

## 4. Repository-specific implementation rules

This repository uses Kotlin Multiplatform and Compose Multiplatform. Do not convert it to a web app, React, Flutter, or another UI stack.

- Prefer shared implementations in `commonMain` when supported by existing Compose APIs.
- Keep platform-specific code in its existing source sets. Do not introduce `expect/actual` without a genuine platform requirement.
- ViewModels own state and business logic. Composables render supplied state and forward events to existing ViewModel functions.
- Do not add filtering, sorting, queue manipulation, business rules, or state synchronization to Composables.
- Do not create `remember`-based mirrors of ViewModel-owned state.
- Reuse adaptive patterns such as `AdaptiveDropdownBottomSheet` and `AdaptiveDialogBottomSheet` where appropriate.
- `gradle/libs.versions.toml` remains the single source of truth for dependency versions.
- Do not add a new UI framework or large styling dependency. Prefer existing Compose, Material, and icon dependencies.
- Preserve desktop Swing dispatcher setup, JavaFX/WebView configuration, native integrations, and platform initialization.
- All user-facing strings must use `composeApp/src/commonMain/composeResources/values/strings.xml` and `stringResource(Res.string.xxx)`; never hardcode user-facing text in Composables.
- Use the Gradle wrapper and existing project checks. Do not invent lint/typecheck tasks that the repository does not define.

## 5. Visual direction and design system

Target a minimal, refined, editorial, music-first interface:
- Strong page titles and clear section hierarchy.
- Artwork-led discovery and library content.
- Neutral surfaces, restrained accent color, subtle separators, consistent radii.
- Deliberate whitespace without wasting usable area.
- Consistent typography and iconography.
- Quiet, immediate interaction feedback rather than noisy effects.

Create or consolidate reusable tokens for:
- Light and dark backgrounds/surfaces.
- Primary, secondary, and disabled text.
- Accent and semantic colors.
- Dividers, outlines, hover/focus/pressed overlays.
- Typography roles (page title, section title, item title, body, metadata).
- Spacing scale, component heights, sidebar width, artwork radii, and shape scale.

Do not scatter arbitrary raw colors, dimensions, or corner radii through individual screens. Support both light and dark themes intentionally; dark mode is not merely an inverted light palette. Preserve semantic colors where they convey status or meaning. Maintain accessible contrast.

## 6. Application shell and responsive layout

### Desktop / large windows
Use a consistent three-region shell:
1. Left navigation sidebar.
2. Main content region with appropriate independent vertical scrolling.
3. Persistent playback region at the bottom.

Requirements:
- Sidebar width is stable and content remains primary.
- Main content uses consistent gutters and sensible maximum/usable width.
- Scrolling content must not cause the sidebar or player to jump.
- Keep the player persistent wherever it is persistent today.
- Avoid nested scroll containers unless existing behavior requires them.
- Align page headers, sections, cards, and lists consistently.

### Mobile / narrow windows
- Use the app's existing mobile navigation model; do not force a desktop sidebar into a narrow viewport.
- Keep essential playback controls accessible.
- Respect safe areas/system insets.
- Ensure artwork, text, menus, dialogs, and controls fit without clipping.
- Preserve existing gestures and navigation semantics.

### Intermediate widths
Adapt to available window size, not device labels. Resizing a desktop window must not create overflow, clipped controls, or unusable navigation.

## 7. Sidebar and navigation

Restyle the existing navigation without changing routes or destination semantics:
- Clear grouping for primary navigation, library, playlists, and secondary destinations only where those groups already exist.
- Consistent icon size, baseline, label alignment, and row height.
- Subtle but unmistakable selected state.
- Restrained unselected state.
- Consistent hover, pressed, focus, and disabled states.
- Understated section labels and separators.
- Preserve all existing destinations, badges, context actions, and navigation callbacks.
- Do not reorder destinations if doing so changes established workflows or discoverability.

## 8. Icons and controls

### Icons
- Use the existing icon library/dependency wherever possible.
- Establish consistent sizing and stroke/weight.
- Use semantically correct icons; do not substitute a visually similar glyph with a different meaning.
- Preserve content descriptions, accessibility labels, tooltips, and platform-specific semantics.
- Do not introduce icon-only controls where the existing UI provides an accessible label.

### Buttons and controls
Restyle primary, secondary, tertiary, icon, toggle, text, and destructive controls consistently. Preserve:
- Existing callbacks and event parameters.
- Enabled/disabled conditions.
- Toggle state and selected state.
- Loading/progress behavior.
- Tooltips, keyboard activation, focus traversal, and accessibility semantics.
- Existing confirmation flows for destructive actions.

Do not change what a button does, when it is enabled, or which action it triggers.

## 9. Screen-by-screen coverage

Audit the actual navigation graph first; apply the design system to every reachable screen that exists in this repository.

### Home / discovery / New-style content
- Editorial page title and section hierarchy.
- Large featured content only when existing data supports it.
- Artwork-forward album, playlist, artist, and track cards.
- Clear metadata and section spacing.
- Preserve existing data, ordering, pagination, and actions. Do not fabricate content or add new recommendation logic.

### Search
- Refined search field, focus state, clear affordance, and result hierarchy.
- Distinguish result types through existing metadata and labels.
- Preserve query handling, provider behavior, filters, and result actions.

### Library, playlists, albums, artists, and tracks
- Consistent list/card patterns and artwork sizing.
- Clear title/artist/album/metadata hierarchy.
- Restyle empty, loading, error, and populated states.
- Preserve sorting, filtering, selection, context menus, playlist edits, and all existing actions.

### Now playing / mini player / queue
- Cohesive artwork and track metadata presentation.
- Clear hierarchy for transport controls and progress.
- Consistent volume, shuffle, repeat, queue, and device controls.
- Preserve exact playback callbacks, queue behavior, timing, and state.
- Keep existing compact/expanded player transitions and platform integrations.

### Settings and plugin-related screens
- Apply consistent section headers, rows, descriptions, switches, dropdowns, and dialogs.
- Preserve every setting, plugin capability, permission, loading state, error, and configuration workflow.
- Do not alter plugin APIs, execution, or compatibility.

### Dialogs, sheets, menus, tooltips, and transient UI
- Apply shared shape, spacing, typography, elevation, and color rules.
- Preserve dismissal behavior, focus handling, keyboard behavior, confirmation steps, and adaptive presentation.
- Check every transient state, not only the default screen.

## 10. Artwork, cards, and lists

- Prefer existing artwork and metadata; do not alter provider/data behavior.
- Use consistent aspect ratios and intentional cropping.
- Keep artwork crisp and avoid stretching.
- Use consistent card padding, metadata spacing, and hover/selection feedback.
- Lists must remain information-dense enough for music libraries; do not turn every row into an oversized card.
- Preserve item click, double-click, context menu, selection, and drag/drop semantics.

## 11. Accessibility and platform quality

- Maintain or improve text contrast and scalable typography.
- Preserve semantic roles, content descriptions, labels, and keyboard focus.
- Provide visible focus indication.
- Keep touch targets practical on mobile.
- Verify hover-only affordances have an equivalent accessible path.
- Avoid animation that interferes with reduced-motion/platform preferences.
- Test light/dark mode and narrow/large windows.
- Do not regress screen readers, keyboard navigation, or platform conventions.

## 12. Step-by-step implementation plan

Implement in separate, reviewable phases. Do not make one giant unverified rewrite.

### Phase 0 — Audit and baseline
- Read project instructions and architecture.
- Map all screens, shared components, theme, navigation, player, and callbacks.
- Run the existing baseline checks if practical and record failures that predate the work.
- Produce a concise checklist of UI surfaces and behavior invariants.

### Phase 1 — Design tokens and theme
- Consolidate palette, typography, spacing, shapes, and component dimensions.
- Implement coherent light/dark variants.
- Confirm theme changes do not affect app state or platform setup.

### Phase 2 — Global shell
- Restyle app background, sidebar container, content frame, page gutters, and persistent player container.
- Verify navigation and playback remain functional before proceeding.

### Phase 3 — Shared primitives
- Restyle reusable buttons, icon buttons, text fields, list rows, cards, section headers, menus, dialogs, and sheets.
- Preserve all callback wiring and accessibility semantics.

### Phase 4 — Primary screens
- Apply shared patterns to Home/discovery, Search, Library, and other primary destinations.
- Verify each existing interaction after each screen.

### Phase 5 — Player and music entities
- Restyle mini/expanded player, queue, album, artist, playlist, and track surfaces.
- Explicitly verify playback and queue behavior is unchanged.

### Phase 6 — Settings and plugins
- Restyle settings and plugin surfaces without changing settings semantics, plugin APIs, execution, or compatibility.

### Phase 7 — Responsive and platform pass
- Validate desktop, mobile, and intermediate window widths.
- Validate platform-specific entry points and integrations.

### Phase 8 — Verification and cleanup
- Run the repository's documented Gradle checks and relevant tests.
- Fix regressions introduced by the redesign; distinguish pre-existing failures.
- Review changed files to ensure no forbidden backend/domain/platform behavior was altered.
- Summarize completed phases, files changed, checks run, and known gaps.

## 13. Strict implementation workflow

For each phase:
1. Inspect the exact files and surrounding call sites before editing.
2. Identify the current state inputs and event callbacks.
3. Make the smallest cohesive presentation-only change.
4. Compare before/after behavior for every affected control.
5. Build or run the most relevant existing check.
6. Review the diff for accidental behavior changes.
7. Continue only when the phase is stable.

Prefer shared components over duplicated screen-specific styling. Avoid broad search-and-replace edits that can alter callback or state wiring. Do not modify files outside the UI scope unless a presentation resource (such as strings or theme resources) requires it.

## 14. Definition of done

- Every reachable screen and shared UI surface has been audited and brought into the new visual system.
- Sidebar, icons, buttons, cards, lists, dialogs, settings, and player look coherent.
- Desktop, mobile, and intermediate layouts are usable.
- Light and dark themes are intentionally designed.
- Existing navigation, UX flows, playback, plugins, providers, data, persistence, downloads, keyboard shortcuts, gestures, and platform integrations remain unchanged.
- Accessibility semantics are preserved or improved.
- Existing project checks pass, or any pre-existing failures are documented separately.
- Final report lists phases completed, key files changed, validation performed, and any incomplete screens.

## 15. Final report format

At completion, report:
- **Phases completed**
- **UI surfaces/screens redesigned**
- **Files changed**
- **Behavior-preservation checks**
- **Build/tests/checks run and outcomes**
- **Known gaps or blockers**

If a screen cannot be completed without changing forbidden behavior, stop and explain the blocker rather than silently changing the behavior.

## 16. Implementation progress log

This section is updated alongside each implementation batch. The specification above remains the governing scope and behavior-preservation contract.

### Completed implementation slices

- **Foundation and shell:** shared theme typography/shape refinements; sidebar hierarchy and spacing.
- **Primary discovery:** Home and Search layout/spacing refinements.
- **Playback surfaces:** compact and desktop player presentation; playback callbacks/state wiring retained.
- **Reusable media cards:** artwork corner treatment, card sizing, and metadata rhythm.
- **Library and collections:** Library tab/content spacing, reusable grid/list rhythm, and shared album/artist/playlist collection hero sizing and spacing.
- **Settings and plugins:** compact Settings content gutters; large-screen Settings sidebar spacing; Plugin manager responsive page gutters and vertical rhythm; installed/discovered plugin card spacing and shared plugin section/list panel rhythm.

### Current validation state

- Repository changes are committed to `feature/apple-music-inspired-ui` and tracked by the open draft PR.
- Changes in these slices are limited to Compose presentation/layout values and shared UI styling. No plugin manager state, install/remove/login actions, permissions, settings state, persistence, or backend behavior was intentionally modified.
- `./gradlew :composeApp:check` has not yet been run. Compilation and runtime appearance remain unverified.
- Remaining work: inspect other reachable shared controls/dialogs and settings/plugin sub-surfaces, perform responsive/light-dark review, run the documented Gradle check, and review the final diff.


### UI primitives and playback overlays

- **Adaptive dialogs and bottom sheets:** refined title/content spacing, scrollable content height, and footer separation while retaining the existing responsive dialog-versus-sheet breakpoint and dismiss behavior.
- **Adaptive dropdown menus:** standardized sheet padding, menu item rhythm, and row height without changing filtering, selection, enabled states, or item callbacks.
- **Queue surface:** refined desktop end-drawer width/padding and mobile bottom-sheet padding/spacing; queue state and dismissal wiring remain unchanged.
- **Playable list tiles:** adjusted artwork corner radius, row height, and icon/content spacing; track selection and playback actions remain unchanged.

### Updated validation state

- These changes are presentation-only Compose adjustments in shared UI primitives and queue container layout.
- No queue ordering, playback, filtering, menu action, navigation, plugin, persistence, or ViewModel behavior was intentionally changed.
- Build/test execution remains outstanding: `./gradlew :composeApp:check` has not been run from this GitHub-connected environment.

### Follow-up shared UI polish

- **Adaptive dialogs:** bounded large-screen dialog content height and enabled vertical scrolling, matching the existing mobile sheet behavior so long forms and permission/release content remain reachable without overflowing the dialog.
- **Adaptive dropdown sheets:** removed a duplicated separator render in mobile menu rows; each `dividerBefore` item now renders one separator, consistent with desktop dropdowns.
- **Behavior preservation:** menu callbacks, selection, filtering, dismiss handling, and dialog actions remain unchanged.
- **Validation:** source-level diff reviewed. `./gradlew :composeApp:check` remains unrun because this GitHub-connected session has no local Gradle execution environment.
