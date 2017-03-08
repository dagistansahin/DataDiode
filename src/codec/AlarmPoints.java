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
import java.util.Arrays;

/**
 * This class is designed to hold the alarm parameters for each data point. Each
 * data point can have up to four different alarms associated with it. This
 * class will hold the information about the alarm status and the type of the
 * alarm.
 *
 * @author Scott Arneson
 */
public class AlarmPoints implements Serializable {

    private Integer[] _alarmStatus;
    private String[] _alarmType;

    /**
     * Creates a new AlarmPoints object with empty arrays of size 4.
     */
    public AlarmPoints() {
        this._alarmStatus = new Integer[4];
        this._alarmType = new String[4];
    }

    /**
     * Returns the alarm status array.
     *
     * @return An Integer array that contains the alarm status values.
     */
    public Integer[] getAlarmStatus() {
        return _alarmStatus;
    }

    /**
     * Returns a specific alarm within the alarm status array.
     *
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     * @return An integer value that indicates status of the alarm at at
     * provided reference value within the array.
     */
    public int getAlarmStatus(int ref) {
        return _alarmStatus[ref];
    }

    /**
     * Sets the alarm status array to provided Integer array.
     *
     * @param alarmStatus Integer array to which the alarm status array will be
     * set.
     */
    public void setAlarmStatus(Integer[] alarmStatus) {
        this._alarmStatus = alarmStatus;
    }

    /**
     * Sets the specific alarm status within the alarm status array.
     *
     * @param alarmStatus Integer value alarm status to set.
     * @param ref References array position of desired alarm status to set.
     * Values can be between 0 and 3.
     */
    public void setAlarmStatus(int alarmStatus, int ref) {
        this._alarmStatus[ref] = alarmStatus;
    }

    /**
     * Returns the alarm types array.
     *
     * @return String array indicating each alarms type.
     */
    public String[] getAlarmType() {
        return _alarmType;
    }

    /**
     * Returns a specific alarm type within the alarm types array.
     *
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     * @return An string value that indicates type of the alarm at at provided
     * reference value within the array.
     */
    public String getAlarmType(int ref) {
        return _alarmType[ref];
    }

    /**
     * Sets the alarm types array to provided string array.
     *
     * @param alarmType String array to which the alarm types array will be set.
     */
    public void setAlarmType(String[] alarmType) {
        this._alarmType = alarmType;
    }

    /**
     * Sets the specific alarm type within the alarm types array.
     *
     * @param alarmType String value alarm type to set.
     * @param ref References array position of desired alarm type to set.
     * Values can be between 0 and 3.
     */
    public void setAlarmType(String alarmType, int ref) {
        this._alarmType[ref] = alarmType;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + Arrays.deepHashCode(this._alarmStatus);
        hash = 31 * hash + Arrays.deepHashCode(this._alarmType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AlarmPoints other = (AlarmPoints) obj;
        if (!Arrays.deepEquals(this._alarmStatus, other._alarmStatus)) {
            return false;
        }
        if (!Arrays.deepEquals(this._alarmType, other._alarmType)) {
            return false;
        }
        return true;
    }
}
