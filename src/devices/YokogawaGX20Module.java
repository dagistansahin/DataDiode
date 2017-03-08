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

import codec.DataPoint;
import communicators.ReceiveEthernetStrategy;
import java.net.InetAddress;
import java.util.List;
import javolution.util.FastTable;

/**
 * This class holds the information for the individual tags on the Yokogawa GX20
 * recorders. The Yokogawa GX20 recorders use modules and expansion modules that
 * do not necessarily have consecutive register numbers. In order to keep track
 * of the jumps in register numbers, the a new YokogawaGX20Module object is
 * created for each interval of consecutive register numbers.
 *
 * @author Scott Arneson
 */
public class YokogawaGX20Module {

    private YokogawaGX20 _recorder;
    private InetAddress _ipAddress;
    private int _port;
    private int _unitID;
    private int _startDataRegisterAddress;
    private int _startAlarmsRegisterAddress;
    private int _numberOfDataPoints;
    private FastTable<String> _tags;
    private FastTable<String> _units;
    private FastTable<Integer> _decimals;
    private FastTable<String[]> _alarmsType;

    /**
     * Creates a YokogawaGX20Module object. Sets various parameters needed for
     * the communication and setup of the recorder object.
     *
     * @param recorder YokogawaGX20 object that the module is associated with.
     * @param registerStart Int value reference to the start register address.
     * @param lines List of Strings that are the lines in the recorder
     * configuration file.
     */
    public YokogawaGX20Module(YokogawaGX20 recorder, int registerStart, List<String> lines) {
        this._recorder = recorder;
        this._ipAddress = recorder.getIpAddress();
        this._port = recorder.getPort();
        this._unitID = recorder.getUnitID();
        this._startDataRegisterAddress = registerStart;
        this._startAlarmsRegisterAddress = this._startDataRegisterAddress + 2500;
        this._tags = new FastTable<>();
        this._units = new FastTable<>();
        this._decimals = new FastTable<>();
        this._alarmsType = new FastTable<>();
        this.parseConfiguration(lines);
    }

    /**
     * Reads the recorder configuration file. It will determine how many data
     * points are to be read, their names, units, number of decimal points, and
     * alarms associated with those data points.
     *
     * @param lines List of Strings that are each line of the recorder
     * configuration file.
     */
    private void parseConfiguration(List<String> lines) {
        String[] alarmsType;
        alarmsType = new String[4];
        int numberOfDataPoints = 0;
        FastTable<Integer> channelNumbers = new FastTable<>();
        channelNumbers.addLast(this._startDataRegisterAddress);

        //Split the lines into two strings, trim them, and make uppercase
        for (String line : lines) {
            String[] data = line.toUpperCase().split(",", 2);

            //Split the second string into individual config values
            if (data.length > 1) {
                String[] configValues = data[1].split(",");

                //Lines starting with "SRANGEAI" are to set values
                if (data[0].startsWith("SRANGEAI")) {
                    int channelNumber = Integer.parseInt(configValues[0]);
                    if ((channelNumber == channelNumbers.getLast())
                            || (channelNumber == (channelNumbers.getLast() + 1))) {
                        channelNumbers.addLast(channelNumber);
                        numberOfDataPoints++;
                        if (data[1].contains("SKIP") || data[1].contains("VOLT,2V,OFF,-20000,20000,0")
                                || data[1].contains("VOLT,200MV,SCALE,4000,20000,0,1,0,10000,")) {
                            this._units.addLast("UNUSED");
                            this._decimals.addLast(0);
                        } else if (data[1].contains("DELTA")) {
                            if (data[1].contains("VOLT")) {
                                this._units.addLast("NO UNITS");
                                if (data[1].contains("1V") || data[1].contains("2V")) {
                                    this._decimals.addLast(4);
                                } else if (data[1].contains("20MV") || data[1].contains("6V") || data[1].contains("20V")) {
                                    this._decimals.addLast(3);
                                } else {
                                    this._decimals.addLast(2);
                                }
                            } else if (data[1].contains("TC")) {
                                this._units.addLast("F");
                                this._decimals.addLast(1);
                            } else if (data[1].contains("RTD")) {
                                this._units.addLast("F");
                                if (data[1].contains("PT100-H") || data[1].contains("JPT100-H")) {
                                    this._decimals.addLast(2);
                                } else {
                                    this._decimals.addLast(1);
                                }
                            } else if (data[1].contains("DI")) {
                                this._units.addLast("NO UNITS");
                                this._decimals.addLast(0);
                            } else {
                                this._units.addLast("NO UNTIS");
                                this._decimals.addLast(2);
                            }
                        } else if (data[1].contains("SCALE")) {
                            if (data[1].contains("GS")) {
                                if (configValues[configValues.length - 3].equalsIgnoreCase("''")) {
                                    this._units.addLast("NO UNITS");
                                } else {
                                    this._units.addLast(configValues[configValues.length - 3].trim()
                                            .replace("'", ""));
                                }
                                this._decimals.addLast(Integer.parseInt(configValues[configValues.length - 6]));
                            } else {
                                if (configValues[configValues.length - 1].equalsIgnoreCase("''")) {
                                    this._units.addLast("NO UNITS");
                                } else {
                                    this._units.addLast(configValues[configValues.length - 1].trim()
                                            .replace("'", ""));
                                }
                                this._decimals.addLast(Integer.parseInt(configValues[configValues.length - 4]));
                            }
                        } else if (data[1].contains("SQRT")) {
                            if (configValues[configValues.length - 4].equalsIgnoreCase("''")) {
                                this._units.addLast("NO UNITS");
                            } else {
                                this._units.addLast(configValues[configValues.length - 4].trim()
                                        .replace("'", ""));
                            }
                            this._decimals.addLast(Integer.parseInt(configValues[configValues.length - 7]));
                        } else if (data[1].contains("LOG")) {
                            if (configValues[configValues.length - 1].equalsIgnoreCase("''")) {
                                this._units.addLast("NO UNITS");
                            } else {
                                this._units.addLast(configValues[configValues.length - 1].trim()
                                        .replace("'", ""));
                            }
                            this._decimals.addLast(Integer.parseInt(configValues[configValues.length - 4]));
                        } else if (data[1].contains("VOLT")) {
                            this._units.addLast("V");
                            if (data[1].contains("1V") || data[1].contains("2V")) {
                                this._decimals.addLast(4);
                            } else if (data[1].contains("20MV") || data[1].contains("6V") || data[1].contains("20V")) {
                                this._decimals.addLast(3);
                            } else {
                                this._decimals.addLast(2);
                            }
                        } else if (data[1].contains("TC")) {
                            this._units.addLast("F");
                            this._decimals.addLast(1);
                        } else if (data[1].contains("RTD")) {
                            this._units.addLast("F");
                            if (data[1].contains("PT100-H") || data[1].contains("JPT100-H")) {
                                this._decimals.addLast(2);
                            } else {
                                this._decimals.addLast(1);
                            }
                        } else if (data[1].contains("DI")) {
                            this._units.addLast("NO UNITS");
                            this._decimals.addLast(0);
                        }
                    }
                }

                //Lines starting with "SALARMIO" indicate the alarms for each channel
                if (data[0].startsWith("SALARMIO")) {
                    int channelNumber = Integer.parseInt(configValues[0]);
                    Boolean containsChannelNumber = false;
                    for (int i = 0; i < channelNumbers.size(); i++) {
                        if (channelNumber == channelNumbers.get(i)) {
                            containsChannelNumber = true;
                        }
                    }
                    if (containsChannelNumber) {
                        if (configValues[2].equalsIgnoreCase("ON")) {
                            alarmsType[Integer.valueOf(configValues[1]) - 1] = configValues[3];
                        } else {
                            alarmsType[Integer.valueOf(configValues[1]) - 1] = "UNUSED";
                        }
                        if (Integer.parseInt(configValues[1]) == 4) {
                            this._alarmsType.addLast(alarmsType);
                            alarmsType = new String[4];
                        }
                    }
                }

                //Lines starting with "STAGIO" are for the the tags for each channel
                if (data[0].startsWith("STAGIO")) {
                    int channelNumber = Integer.parseInt(configValues[0]);
                    Boolean containsChannelNumber = false;
                    for (int i = 0; i < channelNumbers.size(); i++) {
                        if (channelNumber == channelNumbers.get(i)) {
                            containsChannelNumber = true;
                        }
                    }
                    if (containsChannelNumber) {
                        if (!configValues[1].equalsIgnoreCase("''")) {
                            this._tags.addLast(configValues[configValues.length - 1]
                                    .replace("'", ""));
                        } else {
                            this._tags.addLast("NO TAG/UNUSED");
                        }
                    }
                }
            }
        }

        this.setNumberOfDataPoints(numberOfDataPoints);
    }

    /**
     * Communicates with and gets data from the recorder. The data is then
     * compiled into a list of DataPoint objects and returned to the
     * YokogawaGX20 parent object for processing.
     *
     * @return FastTable object of DataPoint objects filled with recorder data
     * from module.
     */
    public FastTable<DataPoint> getData() {

        FastTable<DataPoint> dataPoints = this.fillDataPoints();

        //Get recorder data
        int[] moduleData = ReceiveEthernetStrategy.getInstance().getLittleEndianData(this._recorder,
                this._startDataRegisterAddress + 8999, this._numberOfDataPoints);
        short[] moduleAlarms = ReceiveEthernetStrategy.getInstance().getShortData(this._recorder,
                this._startAlarmsRegisterAddress + 8999, this._numberOfDataPoints);

        if (moduleData.length == this._numberOfDataPoints) {
            for (int i = 0; i < moduleData.length; i++) {
                //Convert the number into double value and divide by 10 to the power of 
                //  number of decimal points since the registers do not hold any decimal place infomation
                dataPoints.get(i).setData((double) moduleData[i] / Math.pow(10, this.getDecimals().get(i)));
            }

            //Evaluate the alarm data for each data point (section 4.5, page 4-28 of tech manual)
            if (moduleAlarms.length == this._numberOfDataPoints) {
                for (int i = 0; i < moduleAlarms.length; i++) {
                    int alarmData = moduleAlarms[i];
                    Integer alarm1, alarm2, alarm3, alarm4;
                    switch (alarmData & 0x0001) {
                        case 0:
                            alarm1 = 0;
                            break;
                        default:
                            alarm1 = 1;
                            break;
                    }
                    switch (alarmData & 0x0002) {
                        case 0:
                            alarm2 = 0;
                            break;
                        default:
                            alarm2 = 1;
                            break;
                    }
                    switch (alarmData & 0x0004) {
                        case 0:
                            alarm3 = 0;
                            break;
                        default:
                            alarm3 = 1;
                            break;
                    }
                    switch (alarmData & 0x0008) {
                        case 0:
                            alarm4 = 0;
                            break;
                        default:
                            alarm4 = 0;
                            break;
                    }
                    dataPoints.get(i).setAlarmsStatus(new Integer[]{alarm1, alarm2, alarm3, alarm4});
                }

                //Remove unused datapoints
                FastTable<DataPoint> data = new FastTable<>();
                for (int i = 0; i < dataPoints.size(); i++) {
                    if (!dataPoints.get(i).getUnits().equalsIgnoreCase("UNUSED")) {
                        data.addLast(dataPoints.get(i));
                    }
                }

                //Return datapoints
                return data;
            }
        }
        //Return empty FastTable if problem with data recieved
        return new FastTable<>();
    }

    /**
     * Prepares a new FastTable of DataPoints to be filled with data. The
     * DataPoints are filled appropriate data tag, units, and alarm types. The
     * rest of the DataPoint is left NULL to be filled in later.
     *
     * @return FastTable of DataPoints.
     */
    private FastTable<DataPoint> fillDataPoints() {
        FastTable<DataPoint> table = new FastTable<>();
        for (int i = 0; i < this.getNumberOfDataPoints(); i++) {
            DataPoint dataPoint = new DataPoint(null, this.getTags().get(i));
            dataPoint.setUnits(this.getUnits().get(i));
            dataPoint.setAlarmsType(this.getAlarmsType().get(i));
            table.addLast(dataPoint);
        }
        return table;
    }

    /**
     * Returns the recorder IP address.
     * 
     * @return InetAddress object that is the IP address of the recorder.
     */
    public InetAddress getIpAddress() {
        return _ipAddress;
    }

    /**
     * Sets the IP address of the recorder.
     * 
     * @param ipAddress InetAddress of the recorder IP address to be set.
     */
    public void setIpAddress(InetAddress ipAddress) {
        this._ipAddress = ipAddress;
    }

    /**
     * Returns the port number of the recorder.
     * 
     * @return Int value port number of the recorder.
     */
    public int getPort() {
        return _port;
    }

    /**
     * Sets the port number for the recorder.
     * 
     * @param port Int value port number to be set.
     */
    public void setPort(int port) {
        this._port = port;
    }

    /**
     * Returns the recorder unit ID number.
     * 
     * @return Int value recorder unit ID number.
     */
    public int getUnitID() {
        return _unitID;
    }

    /**
     * Sets the recorder unit ID number.
     * 
     * @param unitID Int value recorder unit ID to be set.
     */
    public void setUnitID(int unitID) {
        this._unitID = unitID;
    }

    /**
     * Returns the reference for the start of the module registers.
     * 
     * @return Int value reference to the start of the module registers.
     */
    public int getStartDataRegisterAddress() {
        return _startDataRegisterAddress;
    }

    /**
     * Sets the reference for the start of the module registers.
     *
     * @param startDataRegisterAddress Int value of the reference to the start
     * of the module registers to be set.
     */
    public void setStartDataRegisterAddress(int startDataRegisterAddress) {
        this._startDataRegisterAddress = startDataRegisterAddress;
    }

    /**
     * Returns the reference for the start of the module alarm status registers.
     *
     * @return Int value reference to the start of the module alarm status
     * registers.
     */
    public int getStartAlarmsRegisterAddress() {
        return _startAlarmsRegisterAddress;
    }

    /**
     * Sets the reference for the start of the module alarm status registers.
     *
     * @param startAlarmsRegisterAddress Int value reference to the start of the
     * module alarm status registers to be set.
     */
    public void setStartAlarmsRegisterAddress(int startAlarmsRegisterAddress) {
        this._startAlarmsRegisterAddress = startAlarmsRegisterAddress;
    }

    /**
     * Returns the number of data points in the module.
     * 
     * @return Int value number of data points in the module.
     */
    public int getNumberOfDataPoints() {
        return _numberOfDataPoints;
    }

    /**
     * Sets the number of data points in the module.
     *
     * @param numberOfDataPoints Int value number of data points in the module
     * to be set.
     */
    public void setNumberOfDataPoints(int numberOfDataPoints) {
        this._numberOfDataPoints = numberOfDataPoints;
    }

    /**
     * Returns the list of tag names.
     * 
     * @return FastTable of String value tag names.
     */
    public FastTable<String> getTags() {
        return _tags;
    }

    /**
     * Sets the list of tag names to given list.
     * 
     * @param tags FastTable of String value tag names to be set.
     */
    public void setTags(FastTable<String> tags) {
        this._tags = tags;
    }

    /**
     * Returns the list of units.
     * 
     * @return FastTable of String value units.
     */
    public FastTable<String> getUnits() {
        return _units;
    }

    /**
     * Sets the list of units to the given list.
     * 
     * @param units FastTable of String value units to be set.
     */
    public void setUnits(FastTable<String> units) {
        this._units = units;
    }

    /**
     * Returns the list of decimals. The Yokogawa recorder registers do not
     * contain decimal information, therefore the decimal information is
     * obtained from the recorder configuration file and stored.
     *
     * @return FastTable of Integer value decimals.
     */
    public FastTable<Integer> getDecimals() {
        return _decimals;
    }

    /**
     * Sets the list of decimals to given list. The Yokogawa recorder registers
     * do not contain decimal information, therefore the decimal information is
     * obtained from the recorder configuration file and stored.
     *
     * @param decimals FastTable of Integer value decimals to be set.
     */
    public void setDecimals(FastTable<Integer> decimals) {
        this._decimals = decimals;
    }

    /**
     * Returns the list of alarm types.
     * 
     * @return FastTable of String arrays containing tag alarm types.
     */
    public FastTable<String[]> getAlarmsType() {
        return _alarmsType;
    }

    /**
     * Sets the list of alarm types to given list.
     *
     * @param alarmsTypes FastTable of String arrays containing tag alarm types
     * to be set.
     */
    public void setAlarmsType(FastTable<String[]> alarmsTypes) {
        this._alarmsType = alarmsTypes;
    }
}
