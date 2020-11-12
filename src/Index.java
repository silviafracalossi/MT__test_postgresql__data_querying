import java.sql.*;

public class Index {

  Connection pos_conn;
  Statement pos_stmt;
  String table_name;
  String index_name = "time_index";

  // Class that handles the indexes configurations
  public Index (Connection pos_conn, Statement pos_stmt, String table_name) {
    this.pos_conn = pos_conn;
    this.pos_stmt = pos_stmt;
    this.table_name = table_name;
  }

  // Redirects to the correct configuration based on the test running
  public String applyIndex(int test_no) {

    // Configuration 1: no index
    if (test_no == 0)   return noIndex();

    // Configuration 2: index on the timestamp field
    if (test_no == 1)   return timestampIndex();

    // Configuration 3: indexes on the timestamp and on the value fields
    if (test_no == 2)   return timestampAndValueIndexes();

    return "Impossible";
  }

  // Applying the "no index" configuration
  public String noIndex() {
    dropIndex();
    return "Index: \"No Index\" applied";
  }

  // Applying the "index on timestamp" configuration
  public String timestampIndex() {
    try {
      dropIndex();
      String timestamp_index_creation =
              "CREATE INDEX "+index_name+
              "    ON "+table_name+" (time);";
      if (pos_stmt.executeUpdate(timestamp_index_creation) == 0) {
        return "Index: \"Index on Timestamp\" applied";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Index: \"Index on Timestamp\" - problems with the execution";
  }

  // Applying the "index on timestamp and value" configuration
  public String timestampAndValueIndexes() {
    try {
      dropIndex();
      String timestamp_index_creation =
              "CREATE INDEX "+index_name+
              "    ON "+table_name+" (time, value);";
      if (pos_stmt.executeUpdate(timestamp_index_creation) == 0) {
        return "Index: \"Index on Timestamp and Value\" applied";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Index: \"Index on Timestamp and Value\" - problems with the execution";
  }

  public void dropIndex() {
    try {
      pos_stmt.executeUpdate("DROP INDEX IF EXISTS "+index_name+";");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}
