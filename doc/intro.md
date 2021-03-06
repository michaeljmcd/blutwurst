# Introduction to blutwurst

## Motivation

A common step at the start of a project is to create important data designs.
Database tables are a big one, though web service contracts are similar. In all
these cases, there is a certain chicken and the egg problem where data needs to
exist to properly test, but there is no existing data because the tables and
such are new.

In order to get moving on different areas of development faster, it is useful to
have tables pre-populated with test data. Blutwurst is a tool to dynamically
generate data in order to make it easier to bootstrap development.

## Quickstart

Download the latest zip file from
https://github.com/michaeljmcd/blutwurst/releases and unpack it. Scripts to run
the app are in the `bin` folder. To make things simple, there is an example
Derby database in the `samples` folder. This can be used to explore the basic
functionality of the application.

The JDBC drivers have not been archive, due to the potentially unwieldy size
that would result. Download the Derby driver from
http://mvnrepository.com/artifact/org.apache.derby/derby/10.13.1.1 by clicking
on "Download (JAR)" and place the driver in the `lib` folder.

At this point, we should have a database and a driver installed and can proceed
to data generation.

The provided epic database has the following schema:

![Epic Database Schema](https://github.com/michaeljmcd/blutwurst/blob/master/doc/images/hero_db.png)

Assuming you are running from the base of the install directory, you can begin
a data generation with this command:

    .\bin\blutwurst.cmd --connection-string "jdbc:derby:samples/epic.db" --format csv --directory .

This will generate CSV files in the current directory, one for each of the three
tables.

If we were to change the command to 

    .\bin\blutwurst.cmd --connection-string "jdbc:derby:samples/epic.db" --format json --directory .

We would instead have JSON files generated. Obviously, this is just a quick
overview. The sections below give more of a standard listing out of functionality available. At any time,
you can run

    .\bin\blutwurst.cmd --help

To see a list of supported options.

The `samples/epic.cfg` file contains a more robust example. The `-K` option can be used to
pass in these options and try it out. Good luck!

## Recipes

The [quickstart] section above assumes a "happy path" where the database can be
scanned and data generated without any additional effort. Real-world databases
tend to have corner cases and warts that require work. Some of the recipes below
should help.

### Handling codes

Many databases, particularly older ones, have string types that, like an enum,
have a fixed set of valid values. In these cases, the regex generator can be
used to generate codes from a valid set, like so:

    blutwurst --generator-name 'Some name' --generator-regex '(Code1)|(Code2)|(Code3)' --column 'MY_COLUMN' --generator 'Some name' 

This will cause a generator named "Some name" to be created that generates only
values matching the provided regex (in this case, a list of values). The last two options specify
that any columns matching the pattern `MY_COLUMN` use the provided generator.

### Exporting to JSON

To export to JSON, simply change the format to `json`.

    blutwurst --format json <rest of your arguments...>

### More Complex Generation

The options to do several tables can become exhausting. Placing them in a file and then passing
the file to Blutwust with the `-K` option will cause Blutwurst to read options from the file as though
they were passed from the command line.

    blutwurst -K myoptions.cfg

Where myoptions.cfg could be something like:

    -v
    --format csv
    <rest of the paramters>

It is also possible to mix command line arguments with those mixed into the file. Look at `samples/epic.cfg`
for an example of this sort of operation.

### Overriding the Generator for a Column

A few basic heuristics are provided with Blutwurst. These are fairly conservative and will
not capture all cases. To force a specific generator to be used, use the `--column` / `--generator` 
argument pair. 

To view available generators, including those specified at the command line, run the following command:

    blutwurst --list-generators

This will print something like the following:

    State Abbreviation Selector (U.S. and Canada)
    State Name Selector (U.S. and Canada)
    Full Name Selector (U.S.)
    First Name Selector (U.S.)
    Last Name Selector (U.S.)
    String Generator
    Integer Generator
    Decimal Generator
    Date / Timestamp Generator

These names are the names of the different generators. You can force a generator to be used like so:

    blutwurst --column '.*_NOM' --generator 'Last Name Selector (U.S.)'

Be aware that most databases give the columns in all caps.

As shown in [Handling Codes], this also extends to generators defined at the 
command line.

### Generating Data in XML Format

Passing a `--format` value of `xml` will cause the exported data to be written in XML format. 

    blutwurst --format xml <other options>

Each distinct document will be written to a separate file.

### Generating Data from a JSON Schema

Blutwurst can accept a [JSON Schema](http://json-schema.org/) to determine how
to parse data. Passing the path to the schema document to `-c`
/ `--connection-string` will cause it to be parsed as a JSON Schema. Complex
JSON objects can be generated but there are a few caveats of which to be aware.

Blutwurst does not implement the entire JSON Schema standard. Over time, this
may be corrected, but in the meantime there are JSON Schema documents that will
not parse correctly. Definitions and hyperlinks are entirely unimplemented.
Almost none of the validation options are used and Hyper-Schema is entirely
unimplemented.

With all of that said, the provided functionality will be sufficient for
generating data for many real-life schemas. The address example from the JSON
Schema site is included in the `samples` directory. To get a taste of how this
can be used, run the following command:

    ./bin/blutwurst.cmd -c samples/address.json -f json -d .

This will generate JSON files in the current directory answering to the schema
provided in address.json.

## Reference

The current list of options can be viewed with the `--help` switch. For convenience's sake,
the list is provided below.

	  -o, --output OUTPUT_FILE            -             Individual file to which to write the generated data.
	  -K, --config CONFIG_FILE                          Use options in CONFIG_FILE as though they were command line options.
	  -d, --directory OUTPUT_DIRECTORY                  Output directory to which to write individual table-named files.
	  -s, --schema SCHEMA                 []            Database schemas to include in the database scan.
	  -t, --table TABLE                   []            Database tables to include in the database scan. If provided, only listed tables are included.
	  -c, --connection-string CONNECTION  jdbc:h2:mem:  Connection string to scan. If a connection that is not a JDBC connection string is passed, it is assumed to be a JSON Schema instead.
	  -f, --format FORMAT                 :csv          Format to which test data should be exported. Valid options are csv, json, xml, edn and sql.
	  -n, --number-of-rows NUMBER         100           Number of rows to be generated for each table.
		  --column PATTERN                []            Specifies a Java-style regex to be used in assigning a generator to a column.
		  --generator NAME                []            Specifies that columns matching the pattern given previously must use the generator name.
		  --generator-name NAME           []            Specifies the name of a generator to be created through command line arguments.
		  --generator-regex REGEX         []            Specifies a regex to be used when generating data.
		  --list-generators                             List out registered generators.
	  -i, --ignore COLUMN                 []            Ignore a column entirely when generating data.
	  -v, --verbose
	  -h, --help

## Conclusion

There are still a number of features that would be nice to implement. Nonetheless, the 
provided set should be enough to get most projects started.

<!-- vim: set tw=80 fo+=t ft=markdown: -->
