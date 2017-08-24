# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/). This project follows the conventions of [semver.org](http://semver.org/).

## [Unreleased]
### Added
- Convenience scripts to invoke Blutwurst in both bash and Windows Cmd.
- Wrote a basic user manual in `doc/intro.md`.

## [0.3.0] - 2017-08-24
### Added
- Command-line option to control the number of rows generated for each table.
- Support for date / datetime / timestamp generators.
- Improved number type recognition so that types like BIGINT and DOUBLE would be recognized.
- Added headers to CSV output.
- Added random generators to select first and last names from U.S. Census Data.
- Added composite generator for full names.
- Added `--list-generators` command-line option so that the user can see what generators are provided.
- Added command line option (`-s`) to limit the schemas being scanned. By default, all schemas are scanned but provision of a list of schemas will limit scans to those.
- Added pair of command line options (`--column` and `--generator`) to allow a user to override the matched generators.
- Added option (`-K`, `--config`) to source command line options from a file.
- Generators for state names and state abbreviations, from both the U.S. and Canada.
- Added regex-based generators, allowing for custom strings to be generated.

### Fixed

- Integer generation will not exceed the specified size.

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

[Unreleased]: https://github.com/michaeljmcd/blutwurst/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/michaeljmcd/blutwurst/compare/e92f36c4...v0.1.0
