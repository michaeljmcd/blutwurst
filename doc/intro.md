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

![Epic Database Schema](images/epic_db.png)

Assuming you are running from the base of the install directory, you can begin
a data generation with this command:

    .\bin\blutwurst.cmd --connection-string "jdbc:derby:samples/epic.db" --format csv --directory .

This will generate CSV files in the current directory, one for each of the three
tables.

## Recipes

The [quickstart] section above assumes a "happy path" where the 

## Reference

???
???
???
