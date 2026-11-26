# Changelog

## 0.2.0 - 2026-11-20

### Added

- Add the Zipline `NewPipeAPI` host service for fallback YouTube searches and video stream extraction.
- Add serializable search-kind, search-result, and video-info models to the plugin interface.

### Changed

- Bump the plugin interfaces artifact and Spotube plugin API to `0.2.0`. Plugins requesting NewPipe must declare the `newpipe` capability.
