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
import java.util.Objects;

/**
 * This class is designed to hold the individual data points collected. The
 * class includes a double data value, a string tag name, a string units type,
 * and an AlarmPoints object to hold the alarm information.
 *
 * @author Scott Arneson
 */
public class DataPoint implements Serializable {

    private Double _data;
    private String _tag;
    private String _units;
    private AlarmPoints _alarms;

    /**
     * Creates a DataPoint object with data equal to data parameter. The tag,
     * units, and alarms will be null.
     *
     * @param data Double data value to be set.
     */
    public DataPoint(Double data) {
        this(data, null);
    }

    /**
     * Creates a DataPoint object with data and tag equal to the parameters. The
     * units and alarms will be null.
     *
     * @param data Double data value to be set.
     * @param tag String tag to be set.
     */
    public DataPoint(Double data, String tag) {
        this._data = data;
        this._tag = tag;
        this._alarms = new AlarmPoints();
    }

    /**
     * Creates a DataPoint object with data, tag, units, and alarms equal to the
     * parameters.
     *
     * @param data Double data value to be set.
     * @param tag String tag to be set.
     * @param units String units to be set.
     * @param alarms AlarmPoints alarm object to be set.
     */
    public DataPoint(Double data, String tag, String units, AlarmPoints alarms) {
        this._data = data;
        this._tag = tag;
        this._units = units;
        this._alarms = alarms;
    }

    /**
     * Fills in the DataPoint with the DataPointSend information sent over the
     * serial connection.
     *
     * @param data DataPointSend object that contains data and alarm values to
     * be set.
     */
    public void fillData(DataPointSend data) {
        this._data = data.getData();
        this._alarms.setAlarmStatus(data.getAlarmStatus());
    }

    /**
     * Returns the data value.
     *
     * @return Double data value.
     */
    public Double getData() {
        return _data;
    }

    /**
     * Sets the data value to the parameter.
     *
     * @param data Double data value to be set.
     */
    public void setData(Double data) {
        this._data = data;
    }

    /**
     * Returns the tag value.
     *
     * @return String tag value.
     */
    public String getTag() {
        return _tag;
    }

    /**
     * Sets the tag value to the parameter.
     *
     * @param tag String tag to be set.
     */
    public void setTag(String tag) {
        this._tag = tag;
    }

    /**
     * Returns the units value.
     *
     * @return String units value.
     */
    public String getUnits() {
        return _units;
    }

    /**
     * Sets the units value to the parameter.
     *
     * @param units String units to be set.
     */
    public void setUnits(String units) {
        this._units = units;
    }

    /**
     * Returns the alarms object.
     *
     * @return AlarmPoints class alarms value.
     */
    public AlarmPoints getAlarms() {
        return _alarms;
    }

    /**
     * Sets the alarms value to the parameter.
     *
     * @param alarms AlarmPoints class alarms to be set.
     */
    public void setAlarms(AlarmPoints alarms) {
        this._alarms = alarms;
    }

    /**
     * Returns the alarms status array from the alarms object.
     *
     * @return Integer array alarm status values.
     */
    public Integer[] getAlarmsStatus() {
        return _alarms.getAlarmStatus();
    }

    /**
     * Sets the alarms status in alarms object to the parameter.
     *
     * @param alarmsStatus Integer array alarms status to be set.
     */
    public void setAlarmsStatus(Integer[] alarmsStatus) {
        this._alarms.setAlarmStatus(alarmsStatus);
    }

    /**
     * Returns specified alarm status value from the alarms object.
     *
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     * @return Integer alarm status value.
     */
    public Integer getAlarmStatus(int ref) {
        return _alarms.getAlarmStatus(ref);
    }

    /**
     * Sets specified alarm status value in the alarms object.
     *
     * @param alarmStatus Integer alarms status to be set.
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     */
    public void setAlarmStatus(Integer alarmStatus, int ref) {
        this._alarms.setAlarmStatus(alarmStatus, ref);
    }

    /**
     * Gets the alarm types from the alarms object.
     *
     * @return String array alarm types.
     */
    public String[] getAlarmsType() {
        return _alarms.getAlarmType();
    }

    /**
     * Sets the alarm types in the alarms object.
     *
     * @param alarmsType String array alarm types to be set.
     */
    public void setAlarmsType(String[] alarmsType) {
        this._alarms.setAlarmType(alarmsType);
    }

    /**
     * Returns specified alarm type value from the alarms object.
     *
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     * @return String alarm type value.
     */
    public String getAlarmType(int ref) {
        return _alarms.getAlarmType(ref);
    }

    /**
     * Sets specified alarm type value in the alarms object.
     *
     * @param alarmType String alarm type to be set.
     * @param ref References array position of desired alarm status. Values can
     * be between 0 and 3.
     */
    public void setAlarmType(String alarmType, int ref) {
        this._alarms.setAlarmType(alarmType, ref);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this._data);
        hash = 43 * hash + Objects.hashCode(this._tag);
        hash = 43 * hash + Objects.hashCode(this._units);
        hash = 43 * hash + Objects.hashCode(this._alarms);
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
        final DataPoint other = (DataPoint) obj;
        if (!Objects.equals(this._tag, other._tag)) {
            return false;
        }
        if (!Objects.equals(this._units, other._units)) {
            return false;
        }
        if (!Objects.equals(this._data, other._data)) {
            return false;
        }
        return Objects.equals(this._alarms, other._alarms);
    }
}
