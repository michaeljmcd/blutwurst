# blutwurst - A test data generator.

"You don't want to know how the sausage is made."

Blutwurst is a utility for building test data sets based on some sort of
schema. These can then be exported to files for use in filling up a new
database or adding to unit tests.

## Installation

No installation archives exist (yet). We will add them once we reach a certain
level of completion on this project. In the meantime, clone this repository and run:

    lein uberjar

## Usage

To generate data, execute the JAR adding any other JDBC drivers to the classpath. An example is below that includes the sqlite jar:

    $  java  -Xverify:none -cp '.;lib/sqlite-jdbc-3.19.3.jar;target/uberjar/blutwurst-0.2.0-SNAPSHOT-standalone.jar' blutwurst.core [args]

This is, admittedly, not a great user experience. We will be developing wrapper scripts to make this nicer.    

## Options

In order to get a full list of the arguments supported by this application, run the following command:

    $ java -jar blutwurst.jar -h

## Examples

Stay tuned.

### Bugs

No reported bugs, as yet.

## License

Copyright © 2017 Michael McDermott

Distributed under the MIT license.
