# Change Log

All notable changes to the "function-graph-overview" extension will be documented in this file.

Check [Keep a Changelog](http://keepachangelog.com/) for recommendations on how to structure this file.

## [Unreleased]

## [0.0.11] - 2025-05-07

- Pan & Zoom enabled for graphs

## [0.0.10] - 2025-03-26

### Added

- Python catch-all cases in match statements are now detected.
  When a catch-all is found the "no-match-found" edge is removed,
  and cases after the catch-all are removed as dead code.
- C++ learned `co_yield` and `co_return`
- Python learned `assert`

### Fixed

- Python `match` statements no longer break when a comment is present at their top-level
- `throw` and `raise` statements now cause `finally` block duplication like `return` statements.

### Changed

- In flat-switch mode, fallthrough now goes to the case-node, not the consequence-node.
  This produces cleaner, more readable graphs even if it is less correct.

## [0.0.9] - 2025-02-17

### Added

- Function rendering is now cached, so that they are only re-rendered when needed.
  This results in faster update times when moving the cursor in the same function.
- `finally` blocks are now supported in TypeScript.
- Methods are now supported in TypeScript.
- `continue label` is now supported in Go.

### Changed

- Placeholder ("Hello, World!") graph colors are now determined by the color scheme.
- Flat-Switch is now the default for rendering switch-like control-flow structures.

## [0.0.8] - 2025-01-06

### Fixed

- Fixed an edge-case where some functions led to infinite loops when rendering.

## [0.0.7] - 2024-12-19

### Added

- The plugin can now be configured to change CFG rendering settings & color scheme!

### Fixed

- The plugin no longer throw a null-dereference exception on startup.

## [0.0.6] - 2024-12-18

### Added

- TypeScript, JavaScript, TSX, JSX support

### Fixed

- Fixed loop and labeled continue/break handling in Go

## [0.0.5] - 2024-12-12

### Added

- Click-to-navigate support. Click on the graph to jump to the relevant code.

## [0.0.4] - 2024-12-01

### Added

- Support for C++

## [0.0.3] - 2024-11-13

### Added

- Support for rendering CFGs
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

### Known Issues

- The full feature list of the [VSCode plugin](https://github.com/tmr232/function-graph-overview/) including custom
  color themes and graph-based-navigation is not yet supported.
