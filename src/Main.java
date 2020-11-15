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
  static boolean useServerPostgresDB = true;
  static final String DB_PREFIX = "jdbc:postgresql://";
  static String DB_TABLE_NAME = "";

  // LOCAL Configurations
  static final String local_DB_HOST = "localhost";
  static final String local_DB_NAME = "thesis_data_ingestion";
  static final String local_DB_USER = "postgres";
  static final String local_DB_PASS = "silvia";

  // Configurations to server PostgreSQL database
  static final String server_DB_HOST = "ironmaiden.inf.unibz.it";
  static final int server_DB_PORT = 5433;
  static final String server_DB_NAME = "sfracalossi";
  static String server_DB_USER;
  static String server_DB_PASS;

  // Defining the connection and statement variables for PostgreSQL
  static Connection pos_conn = null;
  static Statement pos_stmt = null;

  // Logger names date formatter
  static Logger logger;
  static String logs_path = "logs/";
  static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
    "YYYY-MM-dd__HH.mm.ss");

  // Information about data
  static String data_loaded = "";
  static String[] index_types = {"no", "timestamp", "timestamp_and_value"};
  static String max, min;


  public static void main(String[] args) throws IOException {

    // Instantiating the input scanner
    Scanner sc = new Scanner(System.in);

    try {

      // Getting information from user
      if (args.length != 2) {
          talkToUser();
      } else {
          useServerPostgresDB = (args[0].compareTo("s") == 0);
          DB_TABLE_NAME = args[1];
      }

      // Instantiate general logger
      logger = instantiateLogger("general");

      // Loading the credentials to the new postgresql database
      if (useServerPostgresDB) {
        logger.info("Reading database credentials");
        try {
          File myObj = new File("resources/server_postgresql_credentials.txt");
          Scanner myReader = new Scanner(myObj);
          server_DB_USER = myReader.nextLine();
          server_DB_PASS = myReader.nextLine();
          myReader.close();
        } catch (FileNotFoundException e) {
          System.out.println("Please, remember to create the database"+
            "credentials file (see README)");
          e.printStackTrace();
        }
      }

      // Opening a connection to the postgreSQL database
      logger.info("Connecting to the PostgreSQL database...");
      createDBConnection();

      // Counting the number of rows inserted and min/max time
      getDBCount();
      getMaxMin();

      // Marking start of tests
      logger.info("Starting queries execution");

      // Iterating through the different indexes
      Index index = new Index(pos_conn, pos_stmt, DB_TABLE_NAME);
      for (int index_no=0; index_no<index_types.length; index_no++) {

        // Printing the index name
        String index_appl_response = index.applyIndex(index_no);
        System.out.println(index_appl_response);
        logger.info("Index: " + index_types[index_no]);

        // Executing the queries
        try {
          lastThirtyMinutes_avgMaxMin();
          lastTwoDays_timedMovingAverage();
          allData_windowsAnalysis();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }

    } catch(Exception e) {
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

  // 0. Count
  public static void getDBCount() throws SQLException {

    // Printing method name
    logger.info("==0. Count==");

    // Creating the query
    String count_query = "SELECT COUNT(*) FROM "+DB_TABLE_NAME;

    // Executing the query
    ResultSet rs = pos_stmt.executeQuery(count_query);

    // Printing the result
    while (rs.next()) {
      logger.info("Result: Count of rows: " +rs.getString(1));
      System.out.println("Count: " +rs.getString(1));
    }

    // Closing the set
    rs.close();
  }

  // 0. Max and Min
  public static void getMaxMin() throws SQLException {

    // Printing method name
    logger.info("==0. Max and Min==");

    // Creating the query
    String count_query = "SELECT MIN(time), MAX(time) FROM "+DB_TABLE_NAME;

    // Executing the query
    ResultSet rs = pos_stmt.executeQuery(count_query);

    // Printing the result
    while (rs.next()) {
      min = rs.getString(1);
      max = rs.getString(2);
    }

    // Printing retrieved data
    logger.info("Result: Min: " +min+ " and max: "+max);
    System.out.println("Min: " +min+ " and max: "+max);

    // Closing the set
    rs.close();
  }

  //-----------------------FIRST QUERY----------------------------------------------

  // For windows of 30 minutes, calculate mean, max and min.
  public static void allData_windowsAnalysis() throws SQLException {

    // Printing method name
    System.out.println("1) allData_windowsAnalysis");

    // Creating the query
    String window_size = "30 minutes";

    String allData_query = ""+
      " WITH t AS ( \n"+
      "   SELECT date_trunc('hour', hour_trunc_t) as hour_trunc, minute_part \n"+
      "   FROM generate_series('"+min+"'::timestamp, '"+max+"'::timestamp, '1 hour') AS hour_trunc_t,  \n"+
      "        generate_series(0, 1) AS minute_part \n"+
      " ) \n"+

      " SELECT hour_trunc, minute_part, ROUND(AVG(value),2), MAX(value), MIN(value) \n"+
      " FROM t \n"+
      " LEFT OUTER JOIN "+DB_TABLE_NAME+" ON date_trunc('hour', "+DB_TABLE_NAME+".time) = t.hour_trunc  \n"+
      "     AND FLOOR((EXTRACT(minutes FROM time) / 30))::int = minute_part \n"+
      " GROUP BY hour_trunc, minute_part";

    // Executing the query
    logger.info("Executing windowsAnalysis on AllData");
    ResultSet rs = pos_stmt.executeQuery(allData_query);
    logger.info("Completed execution");

    // Printing the result
    printFirstQuery(rs);
  }

  // Printing the results from the first query
  public static void printFirstQuery(ResultSet rs) throws SQLException{

    // Iterating through all the rows
    while (rs.next()) {
      logger.info("Result:" +
          " From " + rs.getString(1).replace(" ", "T") + "Z" +
          " to " + rs.getString(2).replace(" ", "T") + "Z" +
          " AVG: " + rs.getString(3) +
          " Max: " +rs.getString(4) +
          " Min: " + rs.getString(5));
    }

    // Closing the set
    rs.close();
  }


  //-----------------------SECOND QUERY----------------------------------------------
  // 2. Every 2 minutes of data, computes the average of the current temperature value
  //    and the ones of the previous 4 minutes on last 2 days of data
  public static void lastTwoDays_timedMovingAverage() throws SQLException {

    // Printing method name
    System.out.println("2) lastTwoDays_timedMovingAverage");

    String lastTwoDays_query = "\n"+
      " WITH t AS ( \n"+
      " SELECT date_trunc('hour', hour_trunc_t) as hour_trunc, minute_part \n"+
      " FROM generate_series( \n"+
      "	 	date_trunc('hour', '"+max+"'::timestamp + interval '1 hour') - interval '2 days', \n"+
      "	 	date_trunc('hour', '"+max+"'::timestamp + interval '1 hour'), \n"+
      "	 	'1 hour') AS hour_trunc_t, \n"+
      "	  generate_series(0, 29) AS minute_part \n"+
      ") \n"+

      " SELECT hour_trunc, (minute_part*2) as min, ROUND(AVG(AVG(value)) OVER (ORDER BY hour_trunc, minute_part ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 2) as AVG \n"+
      " FROM t \n"+
      "   LEFT OUTER JOIN "+DB_TABLE_NAME+" ON date_trunc('hour', "+DB_TABLE_NAME+".time) = t.hour_trunc AND (EXTRACT(minutes FROM time) / 2)::int = minute_part \n"+
      " GROUP BY hour_trunc, minute_part";

    // Executing the query
    logger.info("Executing timedMovingAverage on LastTwoDays");
    ResultSet rs = pos_stmt.executeQuery(lastTwoDays_query);
    logger.info("Completed execution");

    // Printing the result
    printSecondQuery(rs);
  }

  // Printing the results from the second query
  public static void printSecondQuery(ResultSet rs) throws SQLException {

    // Iterating through all the rows
    while (rs.next()) {
      logger.info("Result: " + rs.getString(1).replace(" ", "T") + "Z__"+rs.getString(2) +
          " " + rs.getString(3));
    }
    // Closing the set
    rs.close();
  }

  //-----------------------THIRD QUERY----------------------------------------------
  // 3. Calculate mean, max and min on last (arbitrary) 30 minutes of data
  public static void lastThirtyMinutes_avgMaxMin() throws SQLException {

    // Printing method name
    System.out.println("3) lastThirtyMinutes_avgMaxMin");

    // Creating the query
    String lastThirtyMinutes_query = "" +
        " SELECT (max_time - interval '30 minutes') AS start_time, max_time AS end_time, "+
        "   ROUND(AVG(value), 2) AS avg, MAX(value), MIN(value) "+
        " FROM "+DB_TABLE_NAME+", "+
      	"   (SELECT date_trunc('minute', '"+max+"'::timestamp + interval '1 minute') as max_time) t "+
        " WHERE time > (max_time - interval '30 minutes') "+
        " GROUP BY start_time, end_time; ";

    // Executing the query
    logger.info("Executing AvgMaxMin on LastThirtyMinutes");
    ResultSet rs = pos_stmt.executeQuery(lastThirtyMinutes_query);
    logger.info("Completed execution");

    // Printing the result
    printThirdQuery(rs);
  }

  // Printing the results from the third query
  public static void printThirdQuery(ResultSet rs) throws SQLException {

    // Iterating through all the rows
    while (rs.next()) {
      logger.info("Result:" +
          " From " + rs.getString(1).replace(" ", "T") + "Z" +
          " to " + rs.getString(2).replace(" ", "T") + "Z" +
          " AVG: " + rs.getString(3) +
          " Max: " +rs.getString(4) +
          " Min: " + rs.getString(5));
    }
    // Closing the set
    rs.close();
  }

  //-----------------------UTILITY----------------------------------------------

  public static void talkToUser () {
    // Instantiating the input scanner
    Scanner sc = new Scanner(System.in);
    String response = "";
    boolean correct_answer = false;

    // While the answer is not correct
    while (response.compareTo("") == 0) {
        System.out.print("Where do you want the script to be executed?"
                + " (\"s\" for server, \"l\" for local): ");
        response = sc.nextLine().replace(" ", "");

        // Understanding what the user wants
        if (response.compareTo("l") == 0 || response.compareTo("s") == 0) {
            if (response.compareTo("l") == 0) {
              useServerPostgresDB = false;
            }
        } else {
          response = "";
        }
    }

    // Understanding the DB table
    while (DB_TABLE_NAME.compareTo("test_table") != 0 && DB_TABLE_NAME.compareTo("test_table_n") != 0) {
        System.out.print("What is the name of the database?"
                + " (Type \"test_table\" or \"test_table_n\"): ");
        DB_TABLE_NAME = sc.nextLine().replace(" ", "");
    }
  }

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
    String log_complete_path = logs_path + dateAsString + "__postgresql_data_querying.xml";
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
         pos_complete_url = DB_PREFIX + server_DB_HOST + ":" + server_DB_PORT
         + "/" + server_DB_NAME + "?user=" + server_DB_USER + "&password="
         + server_DB_PASS;
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
