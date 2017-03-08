/*******************************************************************************
 *
 * This software was developed at the National Institute of Standards and
 * Technology (NIST) by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United States
 * Code, this software is not subject to copyright protection and is in the
 * public domain. NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic.
 *
 * This software can be redistributed and/or modified freely provided that any
 * derivative works bear some notice that they are derived from it, and any
 * modified versions bear some notice that they have been modified.
 *
 * Author(s): Scott Arneson (NIST)
 *
 * Description: This software was developed to facilitate communication through
 * a one-way data diode. This software is installed on computers on either side
 * of the data diode and will, using configuration files, gather data from
 * Modbus enabled devices, transfer the data across the data diode to a
 * receiving computer, and then transfer the received data to a database server.
 * 
 ******************************************************************************/
package communicators;

import codec.DataDiodeLogger;
import codec.DataDiodeLogger.log;
import codec.DataPointSend;
import codec.GlobalDataHandler;
import devices.ModbusDevice;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import javolution.util.FastMap;
import javolution.util.FastTable;
import devices.RecorderAbstract;

/**
 * This class handles the communication with, setting up of, and maintaining of
 * the database server. The communication is designed to work specifically with
 * an MS-SQL database.
 *
 * @author Scott Arneson
 */
public class Database {

    private static Database _dbInstance;
    private Connection _connection;
    private FastTable<String> _tables;
    private FastMap<String, Integer> _tagID;
    private FastMap<RecorderAbstract, Integer> _recorderID;
    private FastMap<String, Integer> _unitsID;
    private FastMap<String, Integer> _alarmTypeID;
    private int _count;

    /**
     * Creates a Database object.
     */
    public Database() {
        this._tables = new FastTable<>();
        this._tagID = new FastMap<>();
        this._recorderID = new FastMap<>();
        this._unitsID = new FastMap<>();
        this._alarmTypeID = new FastMap<>();
        this._count = 0;
    }

    /**
     * Connects to the database server using information from the
     * GlobalDataHandler.
     */
    public void connect() {
        this._count++;
        if (this.dbConnected()) {
            if (_count == 60) {
                this.closeConnection();
                this._count = 0;
            } else {
                return;
            }
        }

        String user = GlobalDataHandler.getInstance().getDatabaseUsername();
        String pass = GlobalDataHandler.getInstance().getDatabasePassword();
        String url = GlobalDataHandler.getInstance().getDatabaseURL();

       // DataDiodeLogger.getInstance().addLogs(log.NORMAL,
        //       "Connecting to database at IP Address " + _url + ".");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            this._connection = DriverManager.getConnection("jdbc:sqlserver://" + url, user, pass);
            //    DataDiodeLogger.getInstance().addLogs(log.NORMAL, "Database connected.");
        } catch (ClassNotFoundException | SQLException ex) {
            GlobalDataHandler.getInstance().getDbsetup().jErrorLabel.setText("Could not connect to database!");
            GlobalDataHandler.getInstance().getDbsetup().jErrorLabel.setVisible(true);
            GlobalDataHandler.getInstance().getDbsetup().setLocationRelativeTo(null);
            GlobalDataHandler.getInstance().getDbsetup().setVisible(true);
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Unable to connect to database.\n" + ex.toString());
        }
        try {
            this._connection.setAutoCommit(true);
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                    "Error setting database connection to auto-commit.\n" + ex.toString());
        }
        this.getTablesFromDatabase();
        this.createDatabase();
    }

    /**
     * Closes connection to the database server.
     */
    public void closeConnection() {
        if (dbConnected()) {
            try {
                this._connection.close();
            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error while trying to close connection.");
            }
        }
    }

    /**
     * Determines whether the or not the database server is still connected.
     * Will send and receive a transmission to determine connection validity.
     *
     * @return Boolean value status of connection. True for connected, false for
     * disconnected.
     */
    public Boolean dbConnected() {
        try {
            if ((this._connection != null) && (this._connection.isValid(0))) {
                return true;
            }
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error when determining if connected.\n" + ex.toString());
        }
        return false;
    }

    /**
     * Gets the list of tables that exist in the database.
     */
    private void getTablesFromDatabase() {
        try {
            this.connect();
            PreparedStatement prepared = _connection.prepareStatement(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES");
            ResultSet rs = prepared.executeQuery();
            while (rs.next()) {
                this._tables.addLast(rs.getString("TABLE_NAME"));
            }

        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error getting table names from database.\n" + ex.toString());
        }
    }

    /**
     * Determines whether a table exists in the database.
     *
     * @param tableName String table name.
     * @return Boolean value. True if table exists in database, false if table
     * does not exist in database.
     */
    private Boolean tableExists(String tableName) {
        return this._tables.contains(tableName);
    }

    /**
     * Performs initial check and creation of database. Verifies that the basic
     * tables exist in the database, if any table does not already exist in the
     * database it will be created.
     */
    private void createDatabase() {
        Statement stmt;
        String sql;
        try {
            stmt = this._connection.createStatement();
            if (!this.tableExists("RecordersTbl")) {
                sql = "CREATE TABLE RecordersTbl "
                        + "(id INTEGER NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                        + "Model VARCHAR(255), "
                        + "IPAddress VARCHAR(255), "
                        + "UnitID INTEGER, "
                        + "ConfigFile VARCHAR(255))";
                stmt.executeUpdate(sql);
            }
            if (!this.tableExists("UnitsTbl")) {
                sql = "CREATE TABLE UnitsTbl "
                        + "(id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                        + "Units VARCHAR(255))";
                stmt.executeUpdate(sql);
            }
            if (!this.tableExists("AlarmTypeTbl")) {
                sql = "CREATE TABLE AlarmTypeTbl "
                        + "(id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                        + "Type VARCHAR(255))";
                stmt.executeUpdate(sql);
            }
            if (!this.tableExists("ListTagsTbl")) {
                sql = "CREATE TABLE ListTagsTbl "
                        + "(id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                        + "TagName VARCHAR(255) NOT NULL, "
                        + "Units INT REFERENCES UnitsTbl(id), "
                        + "Recorder INT REFERENCES RecordersTbl(id), "
                        + "Alarm1Type INT REFERENCES AlarmTypeTbl(id), "
                        + "Alarm2Type INT REFERENCES AlarmTypeTbl(id), "
                        + "Alarm3Type INT REFERENCES AlarmTypeTbl(id), "
                        + "Alarm4Type INT REFERENCES AlarmTypeTbl(id))";
                stmt.executeUpdate(sql);
            }
            if (!this.tableExists("CurrentValuesTbl")) {
                sql = "CREATE TABLE CurrentValuesTbl "
                        + "(id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                        + "TagName VARCHAR(255) NOT NULL, "
                        + "Timestamp DATETIME, "
                        + "Value DECIMAL(20,4), "
                        + "Alarm1Status INT, "
                        + "Alarm2Status INT, "
                        + "Alarm3Status INT, "
                        + "Alarm4Status INT)";
                stmt.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Error creating database tables.\n" + ex.toString());
        }
    }

    /**
     * Adds the given tag into the CurrentValuesTbl in database.
     * 
     * @param tag String value of tag to be added to the CurrentValuesTbl.
     */
    private void addTag(String tag) {
        try {
            PreparedStatement prepared = this._connection.prepareStatement(
                    "INSERT INTO CurrentValuesTbl(TagName, Value, Alarm1Status, Alarm2Status, "
                    + "Alarm3Status, Alarm4Status) "
                    + "VALUES(?,?,?,?,?,?)");
            prepared.setString(1, tag);
            prepared.setNull(2, java.sql.Types.DOUBLE);
            prepared.setNull(3, java.sql.Types.INTEGER);
            prepared.setNull(4, java.sql.Types.INTEGER);
            prepared.setNull(5, java.sql.Types.INTEGER);
            prepared.setNull(6, java.sql.Types.INTEGER);
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding tag to CurrentValuesTbl.\n" + ex.toString());
        }
    }

    /**
     * Returns the ID number of the given tag from the CurrentValuesTbl in the
     * database.
     *
     * @param tag String value tag whose ID is needed.
     * @return Int value ID of the tag.
     */
    private int getTagID(String tag) {
        if (this._tagID.containsKey(tag)) {
            return this._tagID.get(tag);
        } else {
            try {
                PreparedStatement prepared = this._connection.prepareStatement(
                        "SELECT id FROM CurrentValuesTbl WHERE "
                        + "TagName=?");
                prepared.setString(1, tag);
                ResultSet rs = prepared.executeQuery();
                if (!rs.next()) {
                    this.addTag(tag);
                    return this.getTagID(tag);
                } else {
                    int id = rs.getInt("id");
                    this._tagID.add(tag, id);
                    return id;
                }
            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error getting tag ID.\n" + ex.toString());
            }
            return -1;
        }
    }

    /**
     * Adds the given Yokogawa recorder to the RecordersTbl in database.
     * 
     * @param recorder Yokogawa recorder to be added to the RecordersTbl.
     */
    private void addRecorder(RecorderAbstract recorder) {
        try {
            PreparedStatement prepared = this._connection.prepareStatement(
                    "INSERT INTO RecordersTbl(Model, IPAddress, UnitID, ConfigFile) "
                    + "VALUES(?,?,?,?)");
            prepared.setString(1, recorder.getModelNumber());
            prepared.setString(2, recorder.getIpAddress().toString());
            prepared.setInt(3, recorder.getUnitID());
            prepared.setString(4, recorder.getConfigurationFile());
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding Recorder to RecordersTbl.\n" + ex.toString());
        }
    }

    /**
     * Returns the ID of the recorder from the RecordersTbl in the database.
     * 
     * @param recorder Yokogawa recorder whose ID is needed.
     * @return Int value ID of the recorder.
     */
    private int getRecorderID(RecorderAbstract recorder) {
        if (this._recorderID.containsKey(recorder)) {
            return this._recorderID.get(recorder);
        } else {
            try {
                PreparedStatement prepared = this._connection.prepareStatement(
                        "SELECT id FROM RecordersTbl WHERE "
                        + "Model=? AND IPAddress=? AND UnitID=?");
                prepared.setString(1, recorder.getModelNumber());
                prepared.setString(2, recorder.getIpAddress().toString());
                prepared.setInt(3, recorder.getUnitID());
                ResultSet rs = prepared.executeQuery();
                if (!rs.next()) {
                    this.addRecorder(recorder);
                    return this.getRecorderID(recorder);
                } else {
                    int id = rs.getInt("id");
                    this._recorderID.add(recorder, id);
                    return id;
                }
            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error getting recorder ID.\n" + ex.toString());
            }
            return -1;
        }
    }

    /**
     * Adds the given units to the UnitsTbl in the database.
     * 
     * @param units String value units to be added to the UnitsTbl.
     */
    private void addUnits(String units) {
        try {
            PreparedStatement prepared = this._connection.prepareStatement(
                    "INSERT INTO UnitsTbl(Units) VALUES(?)");
            prepared.setString(1, units);
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding units to UnitsTbl.\n" + ex.toString());
        }
    }

    /**
     * Returns the ID of the given units from the UnitsTbl in the database.
     * 
     * @param units String value units whose ID is needed.
     * @return Int value ID of the units.
     */
    private int getUnitsID(String units) {
        if (this._unitsID.containsKey(units)) {
            return this._unitsID.get(units);
        } else {
            try {
                PreparedStatement prepared = this._connection.prepareStatement(
                        "SELECT id FROM UnitsTbl WHERE "
                        + "Units=?");
                prepared.setString(1, units);
                ResultSet rs = prepared.executeQuery();
                if (!rs.next()) {
                    this.addUnits(units);
                    return this.getUnitsID(units);
                } else {
                    int id = rs.getInt("id");
                    this._unitsID.add(units, id);
                    return id;
                }
            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error getting units ID.\n" + ex.toString());
            }
            return -1;
        }
    }

    /**
     * Adds the given alarm type to the AlarmTypeTbl in the database
     *
     * @param alarmType String value alarm type to be added to the AlarmTypeTbl.
     */
    private void addAlarmType(String alarmType) {
        try {
            PreparedStatement prepared = this._connection.prepareStatement(
                    "INSERT INTO AlarmTypeTbl(Type) VALUES(?)");
            prepared.setString(1, alarmType);
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding alarm type to AlarmTypeTbl.\n"
                    + ex.toString());
        }
    }

    /**
     * Returns the ID of the given alarm type from the AlarmTypeTbl in the database.
     * 
     * @param alarmType String value alarm type whose ID is needed.
     * @return Int value ID of the alarm type.
     */
    private int getAlarmTypeID(String alarmType) {
        if (this._alarmTypeID.containsKey(alarmType)) {
            return this._alarmTypeID.get(alarmType);
        } else {
            try {
                this.addAlarmType(alarmType);
                PreparedStatement prepared = this._connection.prepareStatement(
                        "SELECT id FROM AlarmTypeTbl WHERE "
                        + "Type=?");
                prepared.setString(1, alarmType);
                ResultSet rs = prepared.executeQuery();
                if (!rs.next()) {
                    this.addAlarmType(alarmType);
                    return this.getAlarmTypeID(alarmType);
                } else {
                    int id = rs.getInt("id");
                    this._alarmTypeID.add(alarmType, id);
                    return id;
                }
            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error getting alarm type ID.\n" + ex.toString());
            }
            return -1;
        }
    }

    /**
     * Creates a table for the given tag.
     * 
     * @param tag String value tag.
     */
    private void createTagTable(String tag) {
        Boolean tableCreated = false;
        Statement stmt;
        String sql;
        try {
            stmt = this._connection.createStatement();
            sql = "CREATE TABLE " + tag
                    + " (id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                    + "Timestamp DATETIME, "
                    + "Value DECIMAL(20,4), "
                    + "Alarm1Status INT, "
                    + "Alarm2Status INT, "
                    + "Alarm3Status INT, "
                    + "Alarm4Status INT)";
            stmt.executeUpdate(sql);
            tableCreated = true;
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error creating data table for "
                    + tag + ".\n" + ex.toString());
        }
    }

    /**
     * Adds a tag record into the historical tag table. Also adds the tag into
     * the ListTagsTbl in database if the tag is new.
     *
     * @param time Timestamp value time of record.
     * @param data DataPointSend object containing data for record.
     * @param tag String value tag of recorded to be added.
     * @param recorder Yokogawa recorder associated with tag.
     * @param units String value units of given tag.
     * @param alarmTypes Array of String values for the alarm types of given
     * tag.
     */
    public void addTagRecord(Timestamp time, DataPointSend data, String tag, RecorderAbstract recorder, String units, String[] alarmTypes) {
        this.connect();
        this.updateCurrentValue(tag, data, time);
        String tagName = "[" + tag + "]";
        if (!this.tableExists(tag)) {
            this.createTagTable(tagName);
            this._tables.addLast(tag);
            try {
                int unitsID = this.getUnitsID(units);
                int recorderID = this.getRecorderID(recorder);
                int alarm1ID = this.getAlarmTypeID(alarmTypes[0]);
                int alarm2ID = this.getAlarmTypeID(alarmTypes[1]);
                int alarm3ID = this.getAlarmTypeID(alarmTypes[2]);
                int alarm4ID = this.getAlarmTypeID(alarmTypes[3]);
                PreparedStatement prepared = this._connection.prepareStatement(
                        "INSERT INTO ListTagsTbl(TagName, Units, Recorder, Alarm1Type, "
                        + "Alarm2Type, Alarm3Type, Alarm4Type) "
                        + "VALUES(?,?,?,?,?,?,?)");
                prepared.setString(1, tagName);
                if (unitsID != -1) {
                    prepared.setInt(2, unitsID);
                } else {
                    prepared.setNull(2, java.sql.Types.INTEGER);
                }
                if (recorderID != -1) {
                    prepared.setInt(3, recorderID);
                } else {
                    prepared.setNull(3, java.sql.Types.INTEGER);
                }
                if (alarm1ID != -1) {
                    prepared.setInt(4, alarm1ID);
                } else {
                    prepared.setNull(4, java.sql.Types.INTEGER);
                }
                if (alarm2ID != -1) {
                    prepared.setInt(5, alarm2ID);
                } else {
                    prepared.setNull(5, java.sql.Types.INTEGER);
                }
                if (alarm3ID != -1) {
                    prepared.setInt(6, alarm3ID);
                } else {
                    prepared.setNull(6, java.sql.Types.INTEGER);
                }
                if (alarm4ID != -1) {
                    prepared.setInt(7, alarm4ID);
                } else {
                    prepared.setNull(7, java.sql.Types.INTEGER);
                }
                prepared.executeUpdate();
                prepared = this._connection.prepareStatement(
                        "SELECT id FROM ListTagsTbl WHERE "
                        + "TagName=?");
                prepared.setString(1, tagName);
                ResultSet rs = prepared.executeQuery();

            } catch (SQLException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Database error adding " + tag + " to ListTagsTbl.\n"
                        + ex.toString());
            }
        }
        try {
            PreparedStatement prepared = this._connection.prepareStatement("INSERT INTO " + tagName + "(Timestamp, Value, Alarm1Status, Alarm2Status, "
                    + "Alarm3Status, Alarm4Status) VALUES(?,?,?,?,?,?)");
            prepared.setTimestamp(1, time);
            prepared.setDouble(2, data.getData());
            prepared.setInt(3, data.getAlarmStatus()[0]);
            prepared.setInt(4, data.getAlarmStatus()[1]);
            prepared.setInt(5, data.getAlarmStatus()[2]);
            prepared.setInt(6, data.getAlarmStatus()[3]);
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding new data row to " + tag + " table.\n"
                    + ex.toString());
        }
    }

    /**
     * Updates the current value on the CurrentValuesTbl for the given tag in
     * the database.
     *
     * @param tag String value tag of current value to be set.
     * @param data DataPointSend object containing current value data.
     * @param time Timestamp value time of record.
     */
    public void updateCurrentValue(String tag, DataPointSend data, Timestamp time) {
        this.connect();
        String tagName = "[" + tag + "]";
        int tagID = this.getTagID(tagName);
        try {
            PreparedStatement prepared = this._connection.prepareStatement(
                    "Update CurrentValuesTbl SET "
                    + "Timestamp = ?, Value=?, Alarm1Status=?,"
                    + "Alarm2Status=?, Alarm3Status=?, Alarm4Status=? "
                    + "WHERE id=?");
            prepared.setTimestamp(1, time);
            prepared.setDouble(2, data.getData());
            prepared.setInt(3, data.getAlarmStatus()[0]);
            prepared.setInt(4, data.getAlarmStatus()[1]);
            prepared.setInt(5, data.getAlarmStatus()[2]);
            prepared.setInt(6, data.getAlarmStatus()[3]);
            prepared.setInt(7, tagID);
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error updating current values for " + tag + ".\n"
                    + ex.toString());
        }
        DataDiodeLogger.getInstance().updateTimeLastSent();
    }

    /**
     * Creates a table to hold the historical data from a Modbus device that is
     * not a Yokogawa recorder.
     *
     * @param deviceName String name of Modbus device.
     * @param tags FastTable of String value tags that are associated with the
     * Modbus device.
     */
    public void createModbusDeviceTables(String deviceName, FastTable<String> tags) {
        try {
            String sql = "CREATE TABLE " + deviceName
                    + " (id INT NOT NULL PRIMARY KEY IDENTITY(1, 1), "
                    + "Timestamp DATETIME";
            for (String tag : tags) {
                sql = sql + ", " + tag + " DECIMAL(20,4)";
            }
            sql = sql + ")";
            Statement stmt = this._connection.createStatement();
            stmt.execute(sql);
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error creating data table for "
                    + deviceName + ".\n" + ex.toString());
        }
    }

    /**
     * Creates a table for the current values from a Modbus device that is not a Yokogawa recorder.
     * 
     * @param deviceName String name of Modbus device.
     * @param tags FastTable of String value tags that are associated with the
     * Modbus device.
     */
    public void createModbusDeviceTablesCurrent(String deviceName, FastTable<String> tags) {
        this.createModbusDeviceTables(deviceName, tags);
        String sql = "INSERT INTO " + deviceName + "(Timestamp";
        for (String tag : tags) {
            sql = sql + ", " + tag;
        }
        sql = sql + ") VALUES(?";
        for (int i = 0; i < tags.size(); i++) {
            sql = sql + ",?";
        }
        sql = sql + ")";
        try {
            PreparedStatement prepared = this._connection.prepareStatement(sql);
            prepared.setNull(1, java.sql.Types.TIMESTAMP);
            for (int i = 2; i < (tags.size() + 2); i++) {
                prepared.setNull(i, java.sql.Types.DOUBLE);
            }
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding new data row to " + deviceName + " table.\n"
                    + ex.toString());
        }
        DataDiodeLogger.getInstance().updateTimeLastSent();
    }

    /**
     * Updates the historical data for a Modbus device in the database.
     * 
     * @param device ModbusDevice object whose data is to be updated.
     * @param time Timestamp value time of data record.
     * @param data FastTable of Doubles containing the data to be recorded.
     */
    public void updateModbusDeviceRecord(ModbusDevice device, Timestamp time, FastTable<Double> data) {
        this.updateModbusDeviceCurrent(device, time, data);
        String deviceName = device.getDeviceName();
        String name = "[" + deviceName + "]";
        FastTable<String> tags = device.getTags();
        for (int i = 0; i < tags.size(); i++) {
            String tag = "[" + tags.get(i) + "]";
            tags.set(i, tag);
        }
        if (!this.tableExists(deviceName)) {
            this.createModbusDeviceTables(name, tags);
            this._tables.addLast(deviceName);
        }
        String sql = "INSERT INTO " + name + "(Timestamp, " + tags.removeFirst();
        while (!tags.isEmpty()) {
            sql = sql + ", " + tags.removeFirst();
        }
        sql = sql + ") VALUES(?";
        for (int i = 0; i < data.size(); i++) {
            sql = sql + ",?";
        }
        sql = sql + ")";
        try {
            PreparedStatement prepared = this._connection.prepareStatement(sql);
            prepared.setTimestamp(1, time);
            for (int i = 2; i < (data.size() + 2); i++) {
                prepared.setDouble(i, data.get(i - 2));
            }
            prepared.executeUpdate();
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error adding new data row to " + deviceName + " table.\n"
                    + ex.toString());
        }
    }

    /**
     * Updates the current data for a Modbus device in the database.
     * 
     * @param device ModbusDevice object whose data is to be updated.
     * @param time Timestamp value time of data record.
     * @param data FastTable of Doubles containing the data to be recorded.
     */
    public void updateModbusDeviceCurrent(ModbusDevice device, Timestamp time, FastTable<Double> data) {
        this.connect();
        String deviceName = device.getDeviceName() + " Current";
        String name = "[" + deviceName + "]";
        FastTable<String> tags = device.getTags();
        for (int i = 0; i < tags.size(); i++) {
            String tag = "[" + tags.get(i) + "]";
            tags.set(i, tag);
        }
        if (!this.tableExists(deviceName)) {
            this.createModbusDeviceTablesCurrent(name, tags);
            this._tables.addLast(deviceName);
        }
        String sql = "UPDATE " + name + " SET Timestamp=?, " + tags.removeFirst() + "=?";
        while (!tags.isEmpty()) {
            sql = sql + ", " + tags.removeFirst() + "=?";
        }
        try {
            PreparedStatement prepared = this._connection.prepareStatement(sql);
            prepared.setTimestamp(1, time);
            for (int i = 0; i < data.size(); i++) {
                prepared.setDouble(i + 2, data.get(i));
            }
            prepared.executeUpdate();
            System.out.println("Updated " + device.getDeviceName() + " current records.");
        } catch (SQLException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Database error updating current values for " + deviceName + ".\n"
                    + ex.toString());
        }
    }
}
