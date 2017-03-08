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
package codec;

import java.io.Serializable;
import java.sql.Timestamp;
import javolution.util.FastTable;

/**
 * This class holds all the data from each individual separate Modbus device.
 * The class will then be serialized and sent across the data diode. This class
 * holds a timestamp, a device type (either "Recorder" or "Modbus Device"), a
 * device ID number, and the data itself.
 *
 * @author Scott Arneson
 */
public class DataTable implements Serializable {
    private Timestamp _time;
    private FastTable<DataPointSend> _data;
    private String _deviceType;
    private Integer _deviceID;

    /**
     * Creates a DataTable object and fills the time, data, deviceType, and
     * deviceID values.
     *
     * @param time Timestamp value time to be set.
     * @param data FastTable of DataPointSend objects data to be set.
     * @param deviceType String value deviceType to be set.
     * @param deviceID Integer value deviceID to be set.
     */
    public DataTable(Timestamp time, FastTable<DataPointSend> data,
            String deviceType, Integer deviceID) {
        this._time = time;
        this._data = data;
        this._deviceType = deviceType;
        this._deviceID = deviceID;
    }

    /**
     * Creates a DataTable object and fills the data values only.
     * 
     * @param data FastTable of DataPointSend objects data to be set.
     */
    public DataTable(FastTable<DataPointSend> data) {
        this._data = data;
    }

    /**
     * Creates a DataTable object and fills the time, deviceType, and deviceID
     * values. Data value is left null.
     *
     * @param time Timestamp value time to be set.
     * @param deviceType String value deviceType to be set.
     * @param deviceID Integer value deviceID to be set.
     */
    public DataTable(Timestamp time, String deviceType, Integer deviceID) {
        this._time = time;
        this._deviceType = deviceType;
        this._deviceID = deviceID;
    }

    /**
     * Creates a DataTable object in which the parameters are null.
     */
    public DataTable() {
    }

    /**
     * Returns the Timestamp time value. 
     * 
     * @return Timestamp time value.
     */
    public Timestamp getTime() {
        return _time;
    }

    /**
     * Sets the Timestamp time value.
     * 
     * @param time Timestamp value time to be set.
     */
    public void setTime(Timestamp time) {
        this._time = time;
    }

    /**
     * Returns the FastTable of DataPointSend objects data value.
     * 
     * @return FastTable of DataPointSend objects data value.
     */
    public FastTable<DataPointSend> getData() {
        return _data;
    }

    /**
     * Sets the FastTable of DataPointSend objects data value.
     * 
     * @param data FastTable of DataPointSend objects to be set.
     */
    public void setData(FastTable<DataPointSend> data) {
        this._data = data;
    }

    /**
     * Returns the String deviceType value.
     * 
     * @return The String deviceType value.
     */
    public String getDeviceType() {
        return _deviceType;
    }

    /**
     * Sets the String deviceType value.
     * 
     * @param deviceType The String deviceType value to be set.
     */
    public void setDeviceType(String deviceType) {
        this._deviceType = deviceType;
    }

    /**
     * Returns the Integer deviceID value.
     * 
     * @return The Integer deviceID value.
     */
    public Integer getDeviceID() {
        return _deviceID;
    }

    /**
     * Sets the Integer deviceID value.
     * 
     * @param deviceID The Integer deviceID value to be set.
     */
    public void setRecorderID(Integer deviceID) {
        this._deviceID = deviceID;
    }
}
