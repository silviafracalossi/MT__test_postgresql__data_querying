# Test - Postgresql - Data Querying

Tester of the Postgresql ability of querying speed on series data

## Repository Structure
-   `build/`, containing the generated .class files after compiling the java code;
-   `logs/`, containing the log information of the queries executed;
-   `resources/`, containing the postgresql driver, the database credentials file and the logger properties;
-   `src/`, containing the java source files.

In the main directory, there is:
-   `compile_and_run.bash`, a bash file containing the commands for compiling the java code and running it.

## Requirements
-   PostgreSQL JDBC Driver (42.2.14)

## Installation and running the project
-   Create the folder `build`;
-   Create the folder `logs`;
-   Inside the folder `resources`,
    -   Create a file called `server_postgresql_credentials.txt`, containing the username (first line) and the password (second line) to access the server PostgreSQL database;
    -   Copy-paste the indicated PostgreSQL driver (called `postgresql-42.2.14.jar`);
-   `bash compile_and_run.bash`

## Preparing an executable jar file
Since I couldn't manage to find a way with the command line, I used Eclipse:
-   Create the folder `standalone`;
-   Open the project in Eclipse;
-   Set Java 8 as the default JRE:
    -   `Window > Preferences > Java > Installed JREs`;
    -   Select Java 8;
    -   `Apply and Close`;
-   Set Java 8 as the compiler version:
    -   `Window > Preferences > Java > Compiler`;
    -   Compiler compliance level: `1.8`;
    -   `Apply and Close`;
-   Create the JAR file:
    -   Right-click on the project folder > `Export`;
    -   `Java > Runnable JAR file > Next`;
    -   Launch Configuration: `Main`;
    -   Export destination: `test_postgresql_data_querying/standalone/DataQueryingTest.jar`;
    -   `Finish`.
-   Execute the JAR file:
    -   If you have this repository available:
        -   From the main directory, execute `java -jar standalone/DataQueryingTest.jar`.
    -   If you need a proper standalone version:
        -   Check the next paragraph.

## Preparing the standalone version on the server
-   Connect to the unibz VPN through Cisco AnyConnect;
-   Open the terminal:
    -   Execute `ssh -t sfracalossi@ironlady.inf.unibz.it "cd /data/sfracalossi ; bash"`;
    -   Execute `mkdir standalone_query`;
    -   Execute `mkdir standalone_query/resources`;
-   Send the JAR and the help files from another terminal (not connected through SSH):
    -   Execute `scp standalone/DataQueryingTest.jar sfracalossi@ironlady.inf.unibz.it:/data/sfracalossi/standalone_query`;
    -   Execute `scp resources/server_postgresql_credentials.txt sfracalossi@ironlady.inf.unibz.it:/data/sfracalossi/standalone_query/resources`;
    -   Execute `scp resources/logging.properties sfracalossi@ironlady.inf.unibz.it:/data/sfracalossi/standalone_query/resources`;
-   Execute the JAR file (use the terminal connected through SSH):
    -   Execute `cd standalone_query`;
    -   Execute `java -jar DataQueryingTest.jar`.
