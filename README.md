# blutwurst - A test data generator.

"You don't want to know how the sausage is made."

Blutwurst is a utility for building test data sets based on some sort of
schema. These can then be exported to files for use in filling up a new
database or adding to unit tests.

## Installation

To install, simply unpack an archive and add any needed JDBC drivers to the
`lib` folder. 

To build from source, you will need the Java runtime and Leiningen. With these
prerequisites, simply build with `clj`.

    clj -T:build uber

## Usage

To generate data, execute the JAR adding any other JDBC drivers to the classpath. An example is below that includes the sqlite jar:

    $  ./bin/blutwurst.cmd [args]

This is, admittedly, not a great user experience. We will be developing wrapper scripts to make this nicer.

## Options

In order to get a full list of the arguments supported by this application, run the following command:

    $ ./bin/blutwurst -h

## Examples

Imagine you have a [Derby](https://db.apache.org/derby/) database on your local machine. Running a command like so:

    ./bin/blutwurst.cmd --connection-string 'jdbc:derby:test.db' --format csv --directory mydata 

Will cause Blutwurst to start up, connect to Derby, scan the tables, generate data and export it as a CSV file per 
table in the `mydata` directory. This assumes that you have permissions to `test.db` and that the JAR files for the Derby client
are in the `lib` folder.

There are a number of other options. Running

    ./bin/blutwurst.cmd --help

Will print out a list of options. Please see the [manual](doc/intro.md) for a more comprehensive overview.

## Data Sources

The data used the random selection generators (like the person names or state names) was sourced
from the following locations:

* https://statetable.com/ (states, provinces and territories of the United States and Canada).
* https://www.census.gov/topics/population/genealogy/data/2010_surnames.html (Surnames in the 2010 United States Census).
* https://www.census.gov/topics/population/genealogy/data/1990_census/1990_census_namefiles.html (Personal names in the 1990 United States Census).
* https://sourceforge.net/p/squirrel-sql/git/ci/master/tree/sql12/core/src/net/sourceforge/squirrel_sql/client/resources/defaults/default_drivers.xml (classnames for various JDBC drivers).

### Bugs

No reported bugs, as yet.

## License

Copyright © 2017 Michael McDermott

Distributed under the MIT license.
