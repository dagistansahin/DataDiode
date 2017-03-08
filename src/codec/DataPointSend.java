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

/**
 * This class is designed to hold only the necessary data to be serialized and
 * sent across the the serial connection. As such, it contains only a double
 * data value and an Integer array that contains the current alarm status. The
 * idea is that the receiving side will also have the same configuration files
 * and therefore will already have the other necessary information such as tag
 * name, units, and alarm types.
 *
 * @author Scott Arneson
 */
public class DataPointSend implements Serializable {

    private Double _data;
    private Integer[] _alarmStatus;

    /**
     * Creates a DataPointSend object with filled data and alarmStatus values.
     *
     * @param data Double value data to be set.
     * @param alarmStatus Integer array alarmsStatus value to be set.
     */
    public DataPointSend(Double data, Integer[] alarmStatus) {
        this._data = data;
        this._alarmStatus = alarmStatus;
    }

    /**
     * Creates a DataPointSend object with only the data value filled.
     *
     * @param data Double value data to be set.
     */
    public DataPointSend(Double data) {
        this._data = data;
    }

    /**
     * Creates a DataPointSend object that is empty.
     */
    public DataPointSend() {
    }

    /**
     * Returns the Double data value.
     *
     * @return Double data value.
     */
    public Double getData() {
        return _data;
    }

    /**
     * Sets the data value.
     *
     * @param data Double data value to be set.
     */
    public void setData(Double data) {
        this._data = data;
    }

    /**
     * Returns the alarm status array.
     *
     * @return Integer array containing the alarm status.
     */
    public Integer[] getAlarmStatus() {
        return _alarmStatus;
    }

    /**
     * Sets the alarm status array.
     *
     * @param alarmStatus Integer array alarm status to be set.
     */
    public void setAlarmStatus(Integer[] alarmStatus) {
        this._alarmStatus = alarmStatus;
    }
}
