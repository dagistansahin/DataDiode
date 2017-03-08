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
import codec.DataDiodeLogger.log;
import codec.DataPoint;
import codec.DataPointSend;
import codec.DataTable;
import communicators.ReceiveEthernetStrategy;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import javolution.util.FastTable;

/**
 * This class holds an object that is specific to the Yokogawa GX20 recorder.
 * It contains procedures that are specific to reading the GX20 configuration
 * file and communicating with the GX20 recorder.
 * 
 * @author Scott Arneson
 */
public class YokogawaGX20 extends RecorderAbstract implements RecorderInterface {

    private FastTable<YokogawaGX20Module> _modules;

    /**
     * Creates a YokogawaGX20 object. Sets various parameters needed for the
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
    public YokogawaGX20(String modelNumber, String configFile, String ipAddress, String unitID, String priority) {
        super(modelNumber, configFile, ipAddress, unitID, priority);
    }

    /**
     * Returns the list of YokogawaGX20Module objects that are associated with
     * the recorder.
     *
     * @return FastTable of YokogawaGX20Module objects.
     */
    public FastTable<YokogawaGX20Module> getModules() {
        return _modules;
    }

    /**
     * Sets the list of YokogawaGX20Module objects that are associated with the
     * recorder.
     *
     * @param modules FastTable of YokogawaGX20Module objects to be set.
     */
    public void setModules(FastTable<YokogawaGX20Module> modules) {
        this._modules = modules;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void parseConfiguration(List<String> lines) {
        this._modules = new FastTable<>();

        //Varaibles to count the number of data points
        int numberOfDataPoints = 0;
        int numberOfMathPoints = 0;
        int channelNumber = -1;

        //String to hold the alarmType data for each data point
        String[] alarmsType;
        alarmsType = new String[4];

        //Split the lines into two strings, trim them, and make uppercase
        for (String line : lines) {
            String[] data = line.toUpperCase().split(",", 2);

            //Split the second string into individual config values
            if (data.length > 1) {
                String[] configValues = data[1].split(",");

                //Lines starting with "SRANGEAI" are to set values
                if (data[0].startsWith("SRANGEAI")) {
                    if (channelNumber == -1) {
                        channelNumber = Integer.parseInt(configValues[0]);
                        YokogawaGX20Module module = new YokogawaGX20Module(this, channelNumber, lines);
                        getModules().addLast(module);
                    } else {
                        int number = Integer.parseInt(configValues[0]);
                        if (number != (channelNumber + 1)) {
                            getModules().addLast(new YokogawaGX20Module(this, number, lines));
                        }
                        channelNumber = number;
                    }
                }

                //Lines starting with "SRANGEMATH" indicate math channels setup
                if (data[0].startsWith("SRANGEMATH")) {
                    if (!data[1].contains("OFF")) {
                        numberOfMathPoints++;
                        if (configValues[1].equalsIgnoreCase("ON")) {
                            this.addUnit(configValues[configValues.length - 1].trim()
                                    .replace("'", ""));
                            this.addDecimal(Integer.parseInt(configValues[4]));
                        } else {
                            this.addUnit("UNUSED");
                            this.addDecimal(0);
                        }
                    }
                }

                //Lines starting with "SALARMIO" indicate the alarms for each channel
                if (data[0].startsWith("SALARMMATH")) {
                    if (this.getAlarmsType().size() < numberOfMathPoints) {
                        if (configValues[2].equalsIgnoreCase("ON")) {
                            alarmsType[Integer.valueOf(configValues[1]) - 1] = configValues[3];
                        } else {
                            alarmsType[Integer.valueOf(configValues[1]) - 1] = "UNUSED";
                        }
                        if (Integer.valueOf(configValues[1]) == 4) {
                            this.addAlarmType(alarmsType);
                            alarmsType = new String[4];
                        }
                    }
                }

                //Lines starting with "STAGIO" are for the the tags for each channel
                if (data[0].startsWith("STAGMATH")) {
                    if (this.getTags().size() < numberOfMathPoints) {
                        if (!configValues[1].equalsIgnoreCase("''")) {
                            this.addTag(configValues[configValues.length - 1]
                                    .replace("'", ""));
                        } else {
                            this.addTag("NO TAG/UNUSED");
                        }
                    }
                }
            }
        }

        //Set the values for the recorder
        //Data register addresses and sizes come from tech manual section 4.5
        this.setPort(502);
        this.setNumberOfDataPoints(numberOfDataPoints);
        this.setNumberOfMathPoints(numberOfMathPoints);
        this.setStartDataRegisterAddress(0);            //Reference for register 300001
        this.setDataRegisterMultiplicationConstant(2);
        this.setStartAlarmsRegisterAddress(2500);       //Reference for register 302501
        this.setStartMathRegisterAddress(5000);         //Reference for register 305001
        this.setMathRegisterMultiplicationConstant(2);
        this.setStartMathStatusRegisterAddress(5500);   //Reference for register 305501
        this._count = 9;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void getData() {
        FastTable<DataPoint> data = new FastTable<>();
        Boolean recorderDataGood = true;

        //Establish connection
        this.connect();
        if (this.getModbusConnection().isOpen()) {
            //Get time from recorder
            Timestamp time = this.getTime();

            //Get data from individual YokogawaGX20 modules
            for (YokogawaGX20Module module : getModules()) {
                FastTable<DataPoint> moduleData = module.getData();
                if (moduleData.isEmpty()) {
                    recorderDataGood = false;
                }
                while (!moduleData.isEmpty()) {
                    data.addLast(moduleData.removeFirst());
                }
            }

            //Get recorder data
            int[] recorderData = ReceiveEthernetStrategy.getInstance().getLittleEndianData(this,
                    this.getStartDataRegisterAddress(), this.getNumberOfDataPoints());
            short[] dataAlarms = ReceiveEthernetStrategy.getInstance().getShortData(this,
                    this.getStartAlarmsRegisterAddress(), this.getNumberOfDataPoints());
            int[] mathData = ReceiveEthernetStrategy.getInstance().getLittleEndianData(this,
                    this.getStartMathRegisterAddress(), this.getNumberOfMathPoints());
            short[] mathAlarms = ReceiveEthernetStrategy.getInstance().getShortData(this,
                    this.getStartMathStatusRegisterAddress(), this.getNumberOfMathPoints());

            this.closeConnection();

            //Prepare FastTable of DataPoints for the new data
            FastTable<DataPoint> dataPoints = fillDataPoints();

            //Evaluate the register data recieved from the recorders.
            //Each data point in the recorders is stored in two registers, little endian style (section 4.5 of tech manual)
            if (recorderData.length > 0) {
                for (int i = 0; i < this.getNumberOfDataPoints(); i++) {
                    dataPoints.get(i).setData((double) recorderData[i] / Math.pow(10, this.getDecimals().get(i)));
                }
            }
            if (mathData.length > 0) {
                for (int i = recorderData.length; i < (recorderData.length + mathData.length); i++) {
                    dataPoints.get(i).setData((double) mathData[i - recorderData.length]
                            / Math.pow(10, this.getDecimals().get(i)));
                }
            }

            //Evaluate Alarms
            short[] alarms = Arrays.copyOf(dataAlarms, dataAlarms.length + mathAlarms.length);
            System.arraycopy(mathAlarms, 0, alarms, dataAlarms.length, mathAlarms.length);

            //Evaluate the alarm data for each data point (section 4.5, page 4-28 of tech manual)
            if (alarms.length > 0) {
                for (int i = 0; i < alarms.length; i++) {
                    int alarmData = alarms[i];
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
                            alarm4 = 1;
                            break;
                    }
                    dataPoints.get(i).setAlarmsStatus(new Integer[]{alarm1, alarm2, alarm3, alarm4});
                }
            }

            //Remove unused datapoints
            for (int i = 0; i < dataPoints.size(); i++) {
                if (!dataPoints.get(i).getUnits().equalsIgnoreCase("UNUSED")) {
                    data.addLast(dataPoints.get(i));
                }
            }

            //Create DataTable
            DataTable dataTable = new DataTable(time, "Recorder", this.getRecorderID());
            FastTable<DataPointSend> sendDataPoints = new FastTable<>();
            for (DataPoint dataPoint : data) {
                DataPointSend dataPointSend = new DataPointSend(
                        dataPoint.getData(), dataPoint.getAlarmsStatus());
                sendDataPoints.addLast(dataPointSend);
            }
            if ((recorderData.length == this.getNumberOfDataPoints())
                    && (mathData.length == this.getNumberOfMathPoints()) && recorderDataGood) {
                dataTable.setData(sendDataPoints);
            }

            //Place collected data into GlobalDataHandler collectedDataThreads
            if (dataTable.getData().size() > 0) {
                this.getCollectedData().addLast(dataTable);
            } else {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
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
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void updateDatabase(DataTable dataTable) {
        this._count++;
        int i = 0;
        Timestamp time = dataTable.getTime();
        FastTable<String> tags = new FastTable<>();
        FastTable<String> units = new FastTable<>();
        FastTable<String[]> alarmTypes = new FastTable<>();
        for (YokogawaGX20Module module : this.getModules()) {
            for (String tag : module.getTags()) {
                tags.addLast(tag);
            }
            for (String unit : module.getUnits()) {
                units.addLast(unit);
            }
            for (String[] alarmtype : module.getAlarmsType()) {
                alarmTypes.addLast(alarmtype);
            }
        }
        for (String tag : this.getTags()) {
            tags.addLast(tag);
        }
        for (String unit : this.getUnits()) {
            units.addLast(unit);
        }
        for (String[] alarmtype : this.getAlarmsType()) {
            alarmTypes.addLast(alarmtype);
        }
        FastTable<DataPointSend> dataPoints = dataTable.getData();
        for (int j = 0; j < units.size(); j++) {
            if (!units.get(j).equalsIgnoreCase("UNUSED")) {
                switch (this._count) {
                    case 10:
                        this.getDatabase().addTagRecord(time, dataPoints.get(i), tags.get(j),
                                this, units.get(j), alarmTypes.get(j));
                        i++;
                        break;
                    default:
                        this.getDatabase().updateCurrentValue(tags.get(j), dataPoints.get(i), time);
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
