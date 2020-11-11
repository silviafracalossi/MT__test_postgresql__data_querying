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
  static final String DB_TABLE_NAME = "test_table_n";

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


  // TODO: create methods to change the indexes directly from this script

	public static void main(String[] args) throws IOException {

    // Instantiating the input scanner
    Scanner sc = new Scanner(System.in);

    try {

      // Getting information from user
      if (args.length != 2) {
          talkToUser();
      } else {
          useServerPostgresDB = (args[0].compareTo("s") == 0);
          data_loaded = args[2];
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

      // Counting the number of rows inserted
      getDBCount();

      // Marking start of tests
      logger.info("Starting queries execution");
      try {
        allData_windowsAnalysis();
        // lastTwoDays_timedMovingAverage();
        // lastThirtyMinutes_avgMaxMin();
      } catch (SQLException e) {
        e.printStackTrace();
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

  //-----------------------FIRST QUERY----------------------------------------------

  // For windows of 30 minutes, calculate mean, max and min.
  public static void allData_windowsAnalysis() throws SQLException {

    // Printing method name
    System.out.println("1) allData_windowsAnalysis");

    // Creating the query
    String window_size = "30 minutes";
    String allData_query = ""+
      " with t as ( \n"+
      "   select \n"+
      "     generate_series(date_trunc('hour', min_time),max_time,'"+window_size+"') as interv \n"+
      "     from (select min(time) as min_time, max(time) as max_time from "+DB_TABLE_NAME+") a \n"+
      " ) \n"+
      " select interv as start_time, (interv + interval '"+window_size+"') as end_time, \n"+
      "   ROUND(AVG(value),2) as avg, max(value) as max, min(value) as min \n"+
      " from t \n"+
      "	  left join "+DB_TABLE_NAME+" on "+
      "     ("+DB_TABLE_NAME+".time > t.interv and "+
      "     "+DB_TABLE_NAME+".time <= (interv + interval '"+window_size+"')) \n"+
      " group by start_time, end_time \n"+
      " order by start_time;";

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
  // RANGE BETWEEN '1 day' PRECEDING AND '10 days' FOLLOWING
  //
  // // Every 2 minutes of data, computes the average of the current temperature value
  // //      and the ones of the previous 4 minutes on last 2 days of data
  // public static void lastTwoDays_timedMovingAverage() {
  //
  //     // Printing method name
  //     System.out.println("2) lastTwoDays_timedMovingAverage");
  //
  //     // Creating the query
        // int year = (data_loaded.compareTo("1GB") == 0) ? 2009 : 2017;
        // String start_two_days = year+"-12-03T00:00:00Z";
        // String end_two_days = year+"-12-05T00:00:00Z";
  //     String lastTwoDays_query = "" +
  //             "from(bucket:\"" + bucket_name + "\")" +
  //             " |> range(start: " + start_two_days + ", stop: " + end_two_days + ")" +
  //             " |> filter(fn:(r) => " +
  //             "       r._measurement == \"" + measurement + "\"" +
  //             " )" +
  //             " |> timedMovingAverage(every: 2m, period: 4m)";
  //
  //     // Executing the query
  //     logger.info("Executing timedMovingAverage on LastTwoDays");
  //     List<FluxTable> tables = queryApi.query(lastTwoDays_query);
  //     logger.info("Completed execution");
  //
  //     // Printing the result
  //     printSecondQuery(tables);
  // }
  //
  // // Printing the results from the second query
  // public static void printSecondQuery(List<FluxTable> tables) {
  //
  //     // Iterating through tables (in this case: only "temperature" table)
  //     for (FluxTable fluxTable : tables) {
  //         List<FluxRecord> records = fluxTable.getRecords();
  //
  //         // Iterating through all the rows
  //         for (FluxRecord fluxRecord : records) {
  //             double value = Double.parseDouble(fluxRecord.getValueByKey("_value") + "");
  //             logger.info("Result: " + fluxRecord.getValueByKey("_time") + " " + df.format(value));
  //         }
  //     }
  // }
  //
  // //-----------------------THIRD QUERY----------------------------------------------
  // // 3. Calculate mean, max and min on last (arbitrary) 30 minutes of data
  // public static void lastThirtyMinutes_avgMaxMin() {
  //
  //     // Printing method name
  //     System.out.println("3) lastThirtyMinutes_avgMaxMin");
  //
  //     // Creating the query
  //     int year = (data_loaded.compareTo("1GB") == 0) ? 2009 : 2017;
  //     String start_thirty_minutes = year + "-12-01T11:00:00Z";
  //     String end_thirty_minutes = year + "-12-01T11:30:00Z";
  //     String lastThirtyMinutes_query = "" +
  //             " SELECT MEAN(value), MAX(value), MIN(value) " +
  //             " FROM " + measurement +
  //             " WHERE time > '" + start_thirty_minutes + "' " +
  //             "   AND time < '" + end_thirty_minutes + "' ";
  //
  //     // Executing the query
  //     logger.info("Executing AvgMaxMin on LastThirtyMinutes");
  //     QueryResult queryResult = influxDB.query(new Query(lastThirtyMinutes_query, dbName));
  //     logger.info("Completed execution");
  //
  //     // Printing the result
  //     printThirdQuery(queryResult, start_thirty_minutes, end_thirty_minutes);
  // }
  //
  // // Printing the results from the third query
  // public static void printThirdQuery(QueryResult qr, String start_thirty_minutes, String end_thirty_minutes) {
  //
  //     // Getting all the variables
  //     List<List<Object>> values = qr.getResults().get(0).getSeries().get(0).getValues();
  //
  //     // Printing the result
  //     logger.info("Result:" +
  //             " From " + start_thirty_minutes +
  //             " to " + end_thirty_minutes +
  //             " AVG: " + df.format(values.get(0).get(1)) +
  //             " Max: " + values.get(0).get(2) +
  //             " Min: " + values.get(0).get(3));
  //
  // }

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

    // Understanding what the index configured
    while (data_loaded.compareTo("1GB") != 0 && data_loaded.compareTo("light") != 0) {
        System.out.print("What data is uploaded?"
                + " (Type \"1GB\" or \"light\"): ");
        data_loaded = sc.nextLine().replace(" ", "");
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
