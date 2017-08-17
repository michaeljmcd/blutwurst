# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/). This project follows the conventions of [semver.org](http://semver.org/).

## [Unreleased]
### Added
- Command-line option to control the number of rows generated for each table.
- Support for date / datetime / timestamp generators.
- Improved number type recognition so that types like BIGINT and DOUBLE would be recognized.
- Added headers to CSV output.

## [0.2.0] - 2017-08-17
### Added
- SQL `INSERT` statement generation.
- Added a single-file output sink.
- Support for JSON format output.
- Support for detecting foreign key relationships and organizing generated SQL accordingly.
- Added option to use driver JARs on the classpath rather than requiring all SQL drivers to be compiled into the JAR.
- Command line options to filter tables scanned by schema.
- Detection for NVARCHAR, CHAR and SMALLINT types to the value generator.

### Fixed
- Passing `-h` or `--help` did not cause the usage information to be printed.

### Removed
- No JDBC drivers are included in the final JAR at this point. These will have to be provided at runtime.

## [0.1.0] - 2017-08-04
### Added
- Basic database schema scanning (support for SQLite and H2 databases only).
- Support for outputting to individual CSV files in a directory.
- Support for dumping generated data to standard out.
- Basic data generators for integers, decimals and strings.

[Unreleased]: https://github.com/michaeljmcd/blutwurst/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/michaeljmcd/blutwurst/compare/e92f36c4...v0.1.0
