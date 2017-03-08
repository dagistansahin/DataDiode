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
package devices;

import codec.DataTable;
import com.focus_sw.fieldtalk.MbusTcpMasterProtocol;
import communicators.Database;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import javolution.util.FastTable;

/**
 * This interface holds the information needed generically for Yokogawa
 * Recorders. It holds information needed for communicating with the recorder as
 * well as information about the data the is collected by the recorder.
 * 
 * @author Scott Arneson
 */
public interface RecorderInterface {

    /**
     * Adds a set of alarm types to the list of alarm types. Each tag on the
     * Yokogawa recorders can have up to four alarms of different types. This
     * information is stored in an array of Strings of size four.
     *
     * @param alarmType String array of size 4 containing tag alarm types.
     */
    void addAlarmType(String[] alarmType);

    /**
     * Adds a decimal to the list of decimals to be set. The Yokogawa recorder
     * registers do not contain decimal information, therefore the decimal
     * information is obtained from the recorder configuration file and stored.
     *
     * @param decimal Int value decimal to be added.
     */
    void addDecimal(int decimal);

    /**
     * Adds a tag name to the list of tag names.
     * 
     * @param tag String value tag name to be added to tag name list.
     */
    void addTag(String tag);

    /**
     * Adds a unit to the list of units.
     * 
     * @param unit String value unit to be added to units list.
     */
    void addUnit(String unit);

    /**
     * Returns the list of alarm types.
     * 
     * @return FastTable of String arrays containing tag alarm types.
     */
    FastTable<String[]> getAlarmsType();

    /**
     * Returns the recorder configuration file location.
     * 
     * @return String value configuration file location.
     */
    String getConfigurationFile();

    /**
     * Communicates with and gets data from the recorder. The data is then
     * compiled into a DataTable object and added to the collected data list
     * maintained by the GlobalDataHandler.
     */
    void getData();

    /**
     * Returns the time from the recorder as a Timestamp object.
     * 
     * @return Timestamp object time from the recorder.
     */
    Timestamp getTime();

    /**
     * Sends the data contained in a DataTable object to the database.
     *
     * @param dataTable DataTable object containing recorder data to be sent to
     * the database.
     */
    void updateDatabase(DataTable dataTable);

    /**
     * Returns the list of decimals. The Yokogawa recorder registers do not
     * contain decimal information, therefore the decimal information is
     * obtained from the recorder configuration file and stored.
     *
     * @return FastTable of Integer value decimals.
     */
    FastTable<Integer> getDecimals();

    /**
     * Returns the recorder IP address.
     * 
     * @return InetAddress object that is the IP address of the recorder.
     */
    InetAddress getIpAddress();

    /**
     * Returns the model of the recorder.
     *
     * @return String value recorder model.
     */
    String getModelNumber();

    /**
     * Returns the number of data points in the measurement registers.
     * 
     * @return Int value number of data points in the measurement registers.
     */
    int getNumberOfDataPoints();

    /**
     * Returns the number of data points in the math registers.
     * 
     * @return Int value number of data points in the math registers.
     */
    int getNumberOfMathPoints();

    /**
     * Returns the port number of the recorder.
     * 
     * @return Int value port number of the recorder.
     */
    int getPort();

    /**
     * Returns the reference for the start of the measurement alarm status
     * registers.
     *
     * @return Int value reference to the start of the measurement alarm status
     * registers.
     */
    int getStartAlarmsRegisterAddress();

    /**
     * Returns the reference for the start of the measurement registers.
     * 
     * @return Int value reference to the start of the measurement registers.
     */
    int getStartDataRegisterAddress();

    /**
     * Returns the reference for the start of the math registers.
     * 
     * @return Int value reference to the start of the math registers.
     */
    int getStartMathRegisterAddress();

    /**
     * Returns the reference for the start of the math alarm status registers.
     *
     * @return Int value reference to the start of the math alarm status
     * registers.
     */
    int getStartMathStatusRegisterAddress();

    /**
     * Returns the list of tag names.
     * 
     * @return FastTable of String value tag names.
     */
    FastTable<String> getTags();

    /**
     * Returns the recorder unit ID number.
     * 
     * @return Int value recorder unit ID number.
     */
    int getUnitID();

    /**
     * Returns the list of units.
     * 
     * @return FastTable of String value units.
     */
    FastTable<String> getUnits();

    /**
     * Returns the recorder number. This number identifies a specific recorder
     * to allow coordination across the data diode.
     *
     * @return Int value recorder ID number.
     */
    int getRecorderID();

    /**
     * Sets the recorder number to the given int value. This number identifies a
     * specific recorder to allow coordination across the data diode.
     *
     * @param recorderID Int value recorder ID number to be set.
     */
    void setRecorderID(int recorderID);

    /**
     * Sets the list of alarm types to given list.
     *
     * @param alarmsTypes FastTable of String arrays containing tag alarm types
     * to be set.
     */
    void setAlarmsType(FastTable<String[]> alarmsTypes);

    /**
     * Sets the recorder configuration file location.
     *
     * @param configurationFile String value configuration file location to be
     * set.
     */
    void setConfigurationFile(String configurationFile);

    /**
     * Reads the recorder configuration file. It will determine how many data
     * points are to be read, their names, units, number of decimal points, and
     * alarms associated with those data points.
     *
     * @param lines List of Strings that are each line of the recorder
     * configuration file.
     */
    void parseConfiguration(List<String> lines);

    /**
     * Sets the list of decimals to given list. The Yokogawa recorder registers
     * do not contain decimal information, therefore the decimal information is
     * obtained from the recorder configuration file and stored.
     *
     * @param decimals FastTable of Integer value decimals to be set.
     */
    void setDecimals(FastTable<Integer> decimals);

    /**
     * Sets the IP address of the recorder.
     * 
     * @param ipAddress InetAddress of the recorder IP address to be set.
     */
    void setIpAddress(InetAddress ipAddress);

    /**
     * Sets the model of the recorder.
     * 
     * @param modelNumber String value model to be set.
     */
    void setModelNumber(String modelNumber);

    /**
     * Sets the number of data points in the measurement registers.
     *
     * @param numberOfDataPoints Int value number of data points in the
     * measurement registers to be set.
     */
    void setNumberOfDataPoints(int numberOfDataPoints);

    /**
     * Returns the constant that indicates how many registers for each data
     * point in the measurement registers.
     *
     * @return Int value number of registers per data point in measurement
     * registers.
     */
    int getDataRegisterMultiplicationConstant();

    /**
     * Sets the constant that indicates how many registers for each data point
     * in the measurement registers.
     *
     * @param dataRegisterMultiplicationConstant Int value number of registers
     * per data point in the measurement channels to be set.
     */
    void setDataRegisterMultiplicationConstant(int dataRegisterMultiplicationConstant);

    /**
     * Sets the number of data points in the math registers.
     *
     * @param numberOfMathPoints Int value number of data points in the math
     * registers to be set.
     */
    void setNumberOfMathPoints(int numberOfMathPoints);

    /**
     * Returns the constant that indicates how many registers for each data
     * point in the math registers.
     *
     * @return Int value number of registers per data point in the math
     * registers.
     */
    int getMathRegisterMultiplicationConstant();

    /**
     * Sets the constant the indicates how many registers for each data point in
     * the math registers.
     *
     * @param mathRegisterMultiplicationConstant Int value number of registers
     * per data point in the math registers to be set.
     */
    void setMathRegisterMultiplicationConstant(int mathRegisterMultiplicationConstant);

    /**
     * Sets the port number for the recorder.
     * 
     * @param port Int value port number to be set.
     */
    void setPort(int port);

    /**
     * Sets the reference for the start of the measurement alarm status
     * registers.
     *
     * @param startAlarmsRegisterAddress Int value reference to the start of the
     * measurement alarm status registers to be set.
     */
    void setStartAlarmsRegisterAddress(int startAlarmsRegisterAddress);

    /**
     * Sets the reference for the start of the measurement registers.
     *
     * @param startDataRegisterAddress Int value of the reference to the start
     * of the measurement registers to be set.
     */
    void setStartDataRegisterAddress(int startDataRegisterAddress);

    /**
     * Sets the reference for the start of the math registers.
     * 
     * @param startMathRegisterAddress Int value of the reference to the start
     * of the math registers to be set.
     */
    void setStartMathRegisterAddress(int startMathRegisterAddress);

    /**
     * Sets the reference for the start of the math alarm status registers.
     *
     * @param startMathStatusRegisterAddress Int value reference to the start of
     * the math alarm status registers to be set.
     */
    void setStartMathStatusRegisterAddress(int startMathStatusRegisterAddress);

    /**
     * Sets the list of tag names to given list.
     * 
     * @param tags FastTable of String value tag names to be set.
     */
    void setTags(FastTable<String> tags);

    /**
     * Sets the recorder unit ID number.
     * 
     * @param unitID Int value recorder unit ID to be set.
     */
    void setUnitID(int unitID);

    /**
     * Sets the list of units to the given list.
     * 
     * @param units FastTable of String value units to be set.
     */
    void setUnits(FastTable<String> units);

    /**
     * Returns the list of collected data maintained by the GlobalDataHandler.
     *
     * @return FastTable of DataTable objects.
     */
    FastTable<DataTable> getCollectedData();

    /**
     * Sets the list of collected data maintained by the GlobalDataHandler.
     * 
     * @param collectedData FastTable of DataTable objects to be set.
     */
    void setCollectedData(FastTable<DataTable> collectedData);
    
    /**
     * Returns the Modbus connection protocol object that connects to this
     * recorder.
     *
     * @return MbusTcpMasterProtocol object that connects to the recorder.
     */
    public MbusTcpMasterProtocol getModbusConnection();
    
    /**
     * Sets the Modbus connection protocol object that will connect to this
     * recorder.
     *
     * @param modbusConnection MbusTcpMasterProtocol object to be set to connect
     * to the recorder.
     */
    public void setModbusConnection(MbusTcpMasterProtocol modbusConnection);
    
    /**
     * Returns the Database object used to communicate with the database.
     * 
     * @return Database object.
     */
    public Database getDatabase();
    
    /**
     * Connects to the recorder. If connection fails, a flag is set and the
     * program will wait 5 communication cycles before trying to reconnect. This
     * process repeats until communication is reestablished.
     */
    public void connect();
    
    /**
     * Closes the connection to the recorder.
     */
    public void closeConnection();
    
    /**
     * Connects to the database.
     */
    public void dbConnect();
}
