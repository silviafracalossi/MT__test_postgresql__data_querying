# Test - Postgresql - Data Querying

Tester of the Postgresql ability of querying speed on series data

## Requirements

-   PostgreSQL JDBC Driver (42.2.14)

## Installation

-   Create the folder `build`;
-   Create the folder `logs`;
-   Inside the folder `resources`,
    -   Create a file called `server_postgresql_credentials.txt`, containing the username (first line) and the password (second line) to access the server PostgreSQL database;
    -   Copy-paste the indicated PostgreSQL driver (called `postgresql-42.2.14.jar`);

## Running the project

-   `bash compile_and_run.bash`

## Repository Structure

-   `build/`, containing the generated .class files after compiling the java code;
-   `logs/`, containing the log information of the queries executed;
-   `resources/`, containing the postgresql driver, the database credentials file and the logger properties;
-   `src/`, containing the java source files.

In the main directory, there is:

-   `compile_and_run.bash`, a bash file containing the commands for compiling the java code and running it.
