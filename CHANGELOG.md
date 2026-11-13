# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Maven build configuration (`pom.xml`) for Java 17 and JUnit 4.13.2.
- Console entry point (`engine.ConsoleMain`) with `--demo` walkthrough and interactive terminal play.
- Gameplay test suite (`tests.EngineGameplayTest`) covering hero loading, movement, combat, specials, curing, and CLI execution.
- Architecture and turn-loop diagrams (`docs/diagrams/`), community health files (`.github/`), and CodeQL/Dependabot workflows.

### Changed

- Updated `README.md` with verified mechanics, complete 9-hero roster table from `heroes.csv`, and working Maven and `javac` commands.
- Replaced Franco-Arabic comments and variable names in `Game`, `Hero`, and `Zombie` with English identifiers, and deduplicated spawn-placement loops in `Game`.

### Fixed

- Fixed `Medic.useSpecial()` so healing a full-HP hero succeeds without throwing `InvalidTargetException` and removed the redundant `setSpecialAction(false)` call in the adjacency fallback branch.
- Fixed test-order dependency in `M1PublicTests.testConstructorInitializationZombie()` by resetting `Zombie.ZOMBIES_COUNT` and added `@AfterClass` cleanup for temporary `test_*.csv` files.
- Silenced `-Xlint:serial` warnings across all five classes in `exceptions` with `@SuppressWarnings("serial")`.

## [1.0.0] - 2026-03-19

### Added

- Initial published release of *The Last of Us: Legacy* Java game engine (`engine`, `exceptions`, `model.characters`, `model.collectibles`, `model.world`) with `heroes.csv` and course test suites (`M1PublicTests`, `M2PublicTests`).

[Unreleased]: https://github.com/andrew-abdelmalak/cs4-game/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/andrew-abdelmalak/cs4-game/releases/tag/v1.0.0
