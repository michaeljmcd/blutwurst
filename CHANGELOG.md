# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/). This project follows the conventions of [semver.org](http://semver.org/).

## [Unreleased]
### Added
- SQL `INSERT` statement generation.
- Added a single-file output sink.
- Support for JSON format output.

### Fixed
- Passing `-h` or `--help` did not cause the usage information to be printed.

## [0.1.0] - 2017-08-04
### Added
- Basic database schema scanning (support for SQLite and H2 databases only).
- Support for outputting to individual CSV files in a directory.
- Support for dumping generated data to standard out.
- Basic data generators for integers, decimals and strings.

[Unreleased]: https://github.com/michaeljmcd/blutwurst/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/michaeljmcd/blutwurst/compare/e92f36c4...v0.1.0
