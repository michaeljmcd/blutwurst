# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/). This project follows the conventions of [semver.org](http://semver.org/).

## [Unreleased]
### Fixed
- The application would dump a stack trace and die if no command line arguments were provided. This has been fixed so that usage notes (the same ones as `--help`) will be printed to the screen instead.
- Fixed bug where maximum length constraints on text-based columns were not being respected. The existing test passed due to a luckily long test length.

## [0.5.0] - 2017-09-06
### Fixed
- Corrected the identification of SQL Server's "INT IDENTITY" as a string.
- Added limits to prevent usage of the lorem ipsum text generator from causing stack overflow exceptions on large column inputs.
- Fixed error where trace logging would be prepended to a CSV file output.

### Added
- Added the option to pass in a fixed list of tables from the command line rather than scanning the full database.
- Added a command line option (`--ignore` / `-i`) to ignore a column during data generation.
- Added a null data generation option to give the user a way to force a column to null.
- Money / currency types to the list recognized by the decimal generator.
- Basic support for JSON Schema input.
- Support for formatting data in XML.

### Changed
- Changed the long option for the output directory to `--directory` for `--output-dir`.
- Removed H2 from compiled application. It was still being included despite being a test dependency.

## [0.4.1] - 2017-08-25
### Fixed
- The wrong output directory option was listed in the quickstart. This has been fixed.

## [0.4.0] - 2017-08-25
### Added
- Convenience scripts to invoke Blutwurst in both bash and Windows Cmd.
- Wrote a basic user manual in `doc/intro.md`.
- Added phone, city, URL, zip code and email value generators.
- Errors are now printed for bad command-line inputs.

### Changed
- Change string generation to be lorem ipsum based.

### Fixed
- Ensure that regex string generation respects column sizes.
- Support for SQL Server's datetimeoffset type.
- Added support for detecting types not returned in all-caps.

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

[Unreleased]: https://github.com/michaeljmcd/blutwurst/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.4.1...v0.5.0
[0.4.1]: https://github.com/michaeljmcd/blutwurst/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/michaeljmcd/blutwurst/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/michaeljmcd/blutwurst/compare/e92f36c4...v0.1.0
