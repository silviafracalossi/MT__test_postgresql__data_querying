import java.sql.*;
import java.util.Scanner;
import java.util.logging.*;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.*;
import java.io.*;

public class Main {

  // Defining which database to connect to
  static final boolean useServerPostgresDB = false;
  static final String DB_PREFIX = "jdbc:postgresql://";

  // LOCAL Configurations
  static final String local_DB_HOST = "localhost";
  static final String local_DB_NAME = "thesis_data_ingestion";
  static final String local_DB_USER = "postgres";
  static final String local_DB_PASS = "silvia";

  // Configurations to server PostgreSQL database
  static final String DB_HOST = "ironmaiden.inf.unibz.it";
  static final int DB_PORT = 5433;
  static final String DB_NAME = "sfracalossi";
  static String DB_USER;
  static String DB_PASS;

  // Defining the connection and statement variables for PostgreSQL
  static Connection pos_conn = null;
  static Statement pos_stmt = null;

  // Logger names date formatter
  static Logger general_logger;
  static String logs_path = "logs/";
  static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
    "YYYY-MM-dd__HH.mm.ss");


	public static void main(String[] args) throws IOException {

    // Instantiating the input scanner
    Scanner sc = new Scanner(System.in);

    try {

      // Instantiate general logger
      general_logger = instantiateLogger("general");

      // Loading the credentials to the new postgresql database
      general_logger.info("Reading database credentials");
      try {
        File myObj = new File("resources/server_postgresql_credentials.txt");
        Scanner myReader = new Scanner(myObj);
        DB_USER = myReader.nextLine();
        DB_PASS = myReader.nextLine();
        myReader.close();
      } catch (FileNotFoundException e) {
        System.out.println("Please, remember to create the database"+
          "credentials file (see README)");
        e.printStackTrace();
      }

      // Opening a connection to the postgreSQL database
      general_logger.info("Connecting to the PostgreSQL database...");
      createDBConnection();

      // Asking if the user is ready
      System.out.print("\t\t==\"Ready Statement\"==\nConfirm the other test script, "+
        "then come back here and press enter. ");
      String response = sc.nextLine();

      // Marking start of tests
      general_logger.info("Starting queries execution");

      // Execute queries forever
      while (true) {
        doQueries();
      }

    } catch(Exception e) {
      System.out.println("PSQLException - You probably forgot to stop the script");
      e.printStackTrace();
    } finally {
       try{
          if(pos_stmt!=null) pos_stmt.close();
       } catch(SQLException se2) {
           se2.printStackTrace();
       }
       try {
          if(pos_conn!=null) pos_conn.close();
       } catch(SQLException se){
          se.printStackTrace();
       }
    }
  }

  // Method that actually computes queries to the DB
  public static void doQueries () throws SQLException {

    ResultSet rs = pos_stmt.executeQuery("SELECT COUNT(*) FROM test_table");
    while (rs.next())
    {
        System.out.println(rs.getString(1));
    }
    rs.close();

    // TODO: decomment
    // TODO: find meaningful queries
    // TODO: implement and test queries when database is full

    // query = "SELECT * FROM test_table;";
    //
    // // Executing the query and checking the result
    // try {
    //   if (pos_stmt.executeUpdate(query) != 1) {
    //       general_logger.severe("Problem executing the following script: \n"+query);
    //   } else {
    //       general_logger.info("Query successfully executed: \n"+query);
    //   }
    // } catch (SQLException e) {
    //   general_logger.severe("Problem executing the following script: \n"+query);
    //   e.printStackTrace();
    // }

  }

  //-----------------------UTILITY----------------------------------------------

  // Instantiating the logger for the general information or errors
  public static Logger instantiateLogger (String file_name) throws IOException {

    // Retrieving and formatting current timestamp
    Date date = new Date();
    Timestamp now = new Timestamp(date.getTime());
    String dateAsString = simpleDateFormat.format(now);

    // Setting the name of the folder
    if (file_name.compareTo("general") == 0) {
      logs_path += dateAsString+"/";
      File file = new File(logs_path);
      boolean bool = file.mkdirs();
    }

    // Instantiating general logger
    String log_complete_path = logs_path + dateAsString + "__data_querying.xml";
    Logger logger = Logger.getLogger("DataQueryingGeneralLog_"+file_name);
    logger.setLevel(Level.ALL);

    // Loading properties of log file
    Properties preferences = new Properties();
    try {
        FileInputStream configFile = new FileInputStream("resources/logging.properties");
        preferences.load(configFile);
        LogManager.getLogManager().readConfiguration(configFile);
    } catch (IOException ex) {
        System.out.println("[WARN] Could not load configuration file");
    }

    // Instantiating file handler
    FileHandler gl_fh = new FileHandler(log_complete_path);
    logger.addHandler(gl_fh);

    // Returning the logger
    return logger;
  }


  // Returns the index_no of the specified string in the string array
  public static int returnStringIndex(String[] list, String keyword) {
    for (int i=0; i<list.length; i++) {
      if (list[i].compareTo(keyword) == 0) {
        return i;
      }
    }
    return -1;
  }

  //----------------------DATABASE----------------------------------------------

  // Connecting to the PostgreSQL database
  public static void createDBConnection() {
    try {

      // Creating the connection URL
      String pos_complete_url;
      if (useServerPostgresDB) {
         pos_complete_url = DB_PREFIX + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
         + "?user=" + DB_USER + "&password=" + DB_PASS;
      } else {
         pos_complete_url = DB_PREFIX + local_DB_HOST + "/" + local_DB_NAME
         + "?user=" + local_DB_USER +"&password=" + local_DB_PASS;
      }

      // Connecting and creating a statement
      pos_conn = DriverManager.getConnection(pos_complete_url);
      pos_stmt = pos_conn.createStatement();
    } catch (SQLException e) {
      System.out.println("Problems with creating the database connection");
      e.printStackTrace();
    }
  }


  // Closing the connections to the database
  public static void closeDBConnection() {
    try{
       if(pos_stmt!=null) pos_stmt.close();
    } catch(SQLException se2) {
        se2.printStackTrace();
    }
    try {
       if(pos_conn!=null) pos_conn.close();
    } catch(SQLException se){
       se.printStackTrace();
    }

    // Nulling the database variables
    pos_conn = null;
    pos_stmt = null;
  }
}
