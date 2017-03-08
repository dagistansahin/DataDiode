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

import codec.DataDiodeLogger;
import codec.DataPoint;
import codec.DataPointSend;
import codec.DataTable;
import communicators.ReceiveEthernetStrategy;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import javolution.util.FastTable;

/**
 * This class holds an object that is specific to the Yokogawa DX1000 recorder.
 * It contains procedures that are specific to reading the DX1000 configuration
 * file and communicating with the DX1000 recorder.
 *
 * @author Scott Arneson
 */
public class YokogawaDX1000 extends RecorderAbstract implements RecorderInterface {

    /**
     * Creates a YokogawaDX1000 object. Sets various parameters needed for the
     * communication and setup of the recorder object.
     *
     * @param modelNumber String value that identifies the Yokogawa recorder
     * model used.
     * @param configFile String value that indicates the file location of the
     * recorder configuration file.
     * @param ipAddress String value of the recorder IP address.
     * @param unitID String value of the unit ID number needed for Modbus
     * communication.
     * @param priority String value for the priority number that indicates which
     * thread to handle the data.
     */
    public YokogawaDX1000(String modelNumber, String configFile, String ipAddress, String unitID, String priority) {
        super(modelNumber, configFile, ipAddress, unitID, priority);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void parseConfiguration(List<String> lines) {
        //Varaibles to count the number of data points
        int numberOfDataPoints = 0;

        //String to hold the alarmType data for each data point
        String[] alarmsType;
        alarmsType = new String[4];

        //Split the lines into two strings, trim them, and make uppercase
        for (String line : lines) {
            String[] data = line.toUpperCase().split(",", 2);

            //Split the second string into individual config values
            if (data.length > 1) {
                String[] configValues = data[1].split(",");

                //Lines starting with "SR" are for "Set Register" to set values
                if (data[0].startsWith("SR")) {
                    numberOfDataPoints++;
                    if (data[1].contains("SKIP") || data[1].contains("VOLT,2V,-20000,20000")) {
                        this.addUnit("UNUSED");
                        this.addDecimal(0);
                    } else if (data[1].contains("DELTA")) {
                        if (data[1].contains("VOLT")) {
                            this.addUnit("NO UNITS");
                            if (data[1].contains("2V")) {
                                this.addDecimal(4);
                            } else if (data[1].contains("20MV") || data[1].contains("6V") || data[1].contains("20V")) {
                                this.addDecimal(3);
                            } else {
                                this.addDecimal(2);
                            }
                        } else if (data[1].contains("RTD") || data[1].contains("TC")) {
                            this.addUnit("F");
                            this.addDecimal(1);
                        } else if (data[1].contains("DI")) {
                            this.addUnit("NO UNITS");
                            this.addDecimal(0);
                        } else if (!Character.isDigit(configValues[configValues.length - 1].charAt(0))) {
                            this.addUnit(configValues[configValues.length - 1]);
                            this.addDecimal(2);
                        } else {
                            this.addUnit("NO UNITS");
                            this.addDecimal(2);
                        }
                    } else if (data[1].contains("SCALE")) {
                        if (data[1].contains("VOLT") || data[1].contains("DI")) {
                            this.addUnit(configValues[configValues.length - 1].trim());
                            this.addDecimal(Integer.parseInt(configValues[configValues.length - 2]));
                        } else if (data[1].contains("RTD") || data[1].contains("TC")) {
                            this.addUnit("F");
                            this.addDecimal(Integer.parseInt(configValues[configValues.length - 2]));
                        } else if (data[1].contains("1-5V")) {
                            this.addUnit(configValues[configValues.length - 2].trim());
                            this.addDecimal(Integer.parseInt(configValues[configValues.length - 3]));
                        } else {
                            this.addUnit(configValues[configValues.length - 1].trim());
                            this.addDecimal(0);
                        }
                    } else if (data[1].contains("SQRT")) {
                        if (data[1].contains("ON")) {
                            this.addUnit(configValues[configValues.length - 3].trim());
                            this.addDecimal(Integer.parseInt(configValues[configValues.length - 4]));
                        } else {
                            this.addUnit(configValues[configValues.length - 2].trim());
                            this.addDecimal(Integer.parseInt(configValues[configValues.length - 3]));
                        }
                    } else if (data[1].contains("VOLT")) {
                        this.addUnit("V");
                        if (data[1].contains("2V")) {
                            this.addDecimal(4);
                        } else if (data[1].contains("20MV") || data[1].contains("6V") || data[1].contains("20V")) {
                            this.addDecimal(3);
                        } else {
                            this.addDecimal(2);
                        }
                    } else if (data[1].contains("RTD") || data[1].contains("TC")) {
                        this.addUnit("F");
                        this.addDecimal(1);
                    } else if (data[1].contains("DI")) {
                        this.addUnit("NO UNITS");
                        this.addDecimal(0);
                    }
                }

                //Lines starting with "SA" indicate the alarms for each channel
                if (data[0].startsWith("SA")) {
                    if (configValues[1].equalsIgnoreCase("ON")) {
                        alarmsType[Integer.valueOf(configValues[0]) - 1] = configValues[2];
                    } else {
                        alarmsType[Integer.valueOf(configValues[0]) - 1] = "UNUSED";
                    }
                    if (Integer.valueOf(configValues[0]) == 4) {
                        this.addAlarmType(alarmsType);
                        alarmsType = new String[4];
                    }
                }

                //Lines starting with "ST" are for the the tags for each channel
                if (data[0].startsWith("ST")) {
                    if (!Character.isSpaceChar(data[1].charAt(0))) {
                        this.addTag(configValues[0].trim());
                    } else {
                        this.addTag("NO TAG/UNUSED");
                    }
                }
            }
        }

        //Set the values for the recorder
        //Data register addresses and sizes come from tech manual section 6.3 (page 6-8)
        this.setPort(502);
        this.setNumberOfDataPoints(numberOfDataPoints);
        this.setNumberOfMathPoints(0);                  //DX1000 does not have math channels
        this.setStartDataRegisterAddress(0);            //Reference for register 300001
        this.setDataRegisterMultiplicationConstant(1);
        this.setStartAlarmsRegisterAddress(1000);       //Reference for register 301001
        this.setStartMathRegisterAddress(2000);         //Reference for register 302001
        this.setMathRegisterMultiplicationConstant(2);
        this.setStartMathStatusRegisterAddress(3000);   //reference for register 303001
        this._count = 9;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void getData() {

        //Establish connection
        this.connect();
        if (this.getModbusConnection().isOpen()) {
            //Get time from recorder
            Timestamp time = this.getTime();

            //Get recorder data
            short[] recorderData = ReceiveEthernetStrategy.getInstance().getShortData(this,
                    this.getStartDataRegisterAddress(), this.getNumberOfDataPoints());
            short[] dataAlarms = ReceiveEthernetStrategy.getInstance().getShortData(this,
                    this.getStartAlarmsRegisterAddress(), this.getNumberOfDataPoints());
            int[] mathData = ReceiveEthernetStrategy.getInstance().getLittleEndianData(this,
                    this.getStartMathRegisterAddress(), this.getNumberOfMathPoints());
            short[] mathAlarms = ReceiveEthernetStrategy.getInstance().getShortData(this,
                    this.getStartMathStatusRegisterAddress(), this.getNumberOfMathPoints());

            this.closeConnection();

            //Prepare FastTable o f DataPoints for the new data
            FastTable<DataPoint> dataPoints = fillDataPoints();

            //Evaluate the register data recieved from the recorders.
            if (recorderData.length > 0) {
                for (int i = 0; i < recorderData.length; i++) {
                    dataPoints.get(i).setData((double) recorderData[i] / Math.pow(10, this.getDecimals().get(i)));

                }
            }
            if (mathData.length > 0) {
                //Evaluate the math data which is 32bit stored ins 2 16bit registers little endian style (Section 6.3 of tech manual)
                for (int i = recorderData.length; i < (recorderData.length + mathData.length); i++) {
                    dataPoints.get(i).setData((double) mathData[i - recorderData.length]
                            / Math.pow(10, this.getDecimals().get(i)));
                }
            }

            //Evaluate Alarms
            short[] alarms = Arrays.copyOf(dataAlarms, dataAlarms.length + mathAlarms.length);
            System.arraycopy(mathAlarms, 0, alarms, dataAlarms.length, mathAlarms.length);

            //Alarm information is given in section 6.3 (page 6-8) of the tech manual
            if (alarms.length > 0) {
                for (int i = 0; i < alarms.length; i++) {
                    int alarmData = alarms[i];
                    Integer alarm1, alarm2, alarm3, alarm4;
                    switch (alarmData & 0x0F00) {
                        case 0:
                            alarm1 = 0;
                            break;
                        default:
                            alarm1 = 1;
                            break;
                    }
                    switch (alarmData & 0xF000) {
                        case 0:
                            alarm2 = 0;
                            break;
                        default:
                            alarm2 = 1;
                            break;
                    }
                    switch (alarmData & 0x000F) {
                        case 0:
                            alarm3 = 0;
                            break;
                        default:
                            alarm3 = 1;
                            break;
                    }
                    switch (alarmData & 0x00F0) {
                        case 0:
                            alarm4 = 0;
                            break;
                        default:
                            alarm4 = 1;
                            break;
                    }
                    dataPoints.get(i).setAlarmsStatus(new Integer[]{alarm1, alarm2, alarm3, alarm4});
                }
            }

            //Remove unused datapoints and create DataTable
            DataTable dataTable = new DataTable(time, "Recorder", this.getRecorderID());
            FastTable<DataPointSend> dataPointsSend = new FastTable<>();
            for (int i = 0; i < dataPoints.size(); i++) {
                if (!dataPoints.get(i).getUnits().equalsIgnoreCase("UNUSED")) {
                    dataPointsSend.addLast(new DataPointSend(dataPoints.get(i).getData(),
                            dataPoints.get(i).getAlarmsStatus()));
                }
            }
            dataTable.setData(dataPointsSend);

            //Place collected data into GlobalDataHandler collectedDataThread
            if ((recorderData.length == this.getNumberOfDataPoints())
                    && (mathData.length == this.getNumberOfMathPoints())) {
                this.getCollectedData().addLast(dataTable);
            } else {
                DataDiodeLogger.getInstance().addLogs(DataDiodeLogger.log.SEVERE,
                        "Problem getting data from " + this.getModelNumber()
                        + " at IP Address: " + this.getIpAddress().toString());
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public Timestamp getTime() {
        short[] timeValues = ReceiveEthernetStrategy.getInstance().getShortData(this,
                9000, 7);
        Timestamp time = new Timestamp(timeValues[0] - 1900, timeValues[1] - 1, timeValues[2],
                timeValues[3], timeValues[4], timeValues[5], timeValues[6]);
        return time;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void updateDatabase(DataTable dataTable) {
        this._count++;
        int i = 0;
        Timestamp time = dataTable.getTime();
        FastTable<DataPointSend> dataPoints = dataTable.getData();
        for (int j = 0; j < this.getUnits().size(); j++) {
            if (!this.getUnits().get(j).equalsIgnoreCase("UNUSED")) {
                switch (this._count) {
                    case 10:
                        this.getDatabase().addTagRecord(time, dataPoints.get(i), this.getTags().get(j),
                                this, this.getUnits().get(j), this.getAlarmsType().get(j));
                        i++;
                        break;
                    default:
                        this.getDatabase().updateCurrentValue(this.getTags().get(j), dataPoints.get(i), time);
                        i++;
                        break;
                }
            }
        }
        if (this._count == 10) {
            this._count = 0;
        }
    }
}
