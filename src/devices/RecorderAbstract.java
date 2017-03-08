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
import codec.DataTable;
import codec.GlobalDataHandler;
import com.focus_sw.fieldtalk.MbusTcpMasterProtocol;
import communicators.Database;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javolution.util.FastTable;

/**
 * This abstract holds the information needed generically for Yokogawa
 * Recorders. It holds information needed for communicating with the recorder as
 * well as information about the data the is collected by the recorder.
 *
 * @author Scott Arneson
 */
public abstract class RecorderAbstract implements RecorderInterface {

    private String _modelNumber;
    private InetAddress _ipAddress;
    private int _port;
    private int _unitID;
    private int _recorderID;
    private String _configurationFile;
    private int _startDataRegisterAddress;
    private int _startMathRegisterAddress;
    private int _startAlarmsRegisterAddress;
    private int _startMathStatusRegisterAddress;
    private int _numberOfDataPoints;
    private int _dataRegisterMultiplicationConstant;
    private int _numberOfMathPoints;
    private int _mathRegisterMultiplicationConstant;
    private FastTable<String> _tags;
    private FastTable<String> _units;
    private FastTable<Integer> _decimals;
    private FastTable<String[]> _alarmsType;
    private FastTable<DataTable> _collectedData;
    private MbusTcpMasterProtocol _modbusConnection;
    private Database _database;
    protected int _count;
    protected Boolean _disconnectFlag;
    protected int _disconnectCount;

    /**
     * Abstract for creating a Yokogawa Recorder object. Sets various parameters
     * needed for the communication and setup of the recorder object.
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
    public RecorderAbstract(String modelNumber, String configFile, String ipAddress, String unitID, String priority) {
        this._modelNumber = modelNumber;
        this._configurationFile = configFile;
        this._unitID = Integer.valueOf(unitID);
        this._tags = new FastTable<>();
        this._units = new FastTable<>();
        this._decimals = new FastTable<>();
        this._alarmsType = new FastTable<>();
        this._modbusConnection = new MbusTcpMasterProtocol();
        this._modbusConnection.configureCountFromZero();
        this._modbusConnection.setTimeout(0);
        this._modbusConnection.setPollDelay(0);
        this._database = new Database();
        this._disconnectFlag = false;
        this._disconnectCount = 0;
        switch (Integer.valueOf(priority)) {
            case 1:
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread1();
                break;
            case 2:
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread2();
                break;
            case 3:
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread3();
                break;
            default:
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread3();
                DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                        "Priority level for " + this._modelNumber + " at IP address " + ipAddress
                        + "is not between 1 and 3.  Setting priority level to 3.");
        }
        try {
            this._ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "IP Address " + ipAddress + " appears invalid.\n" + ex.toString());
        }

        try {
            loadConfigurationFile();
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Could not load the configuration file for the recorder.\n" + ex.toString());
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getModelNumber() {
        return _modelNumber;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setModelNumber(String modelNumber) {
        this._modelNumber = modelNumber;
    }

    /**
     * @inheritDoc
     */
    @Override
    public java.net.InetAddress getIpAddress() {
        return _ipAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setIpAddress(java.net.InetAddress ipAddress) {
        this._ipAddress = ipAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getPort() {
        return _port;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setPort(int port) {
        this._port = port;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getUnitID() {
        return _unitID;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setUnitID(int unitID) {
        this._unitID = unitID;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getRecorderID() {
        return _recorderID;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setRecorderID(int recorderID) {
        this._recorderID = recorderID;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getConfigurationFile() {
        return _configurationFile;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setConfigurationFile(String configurationFile) {
        this._configurationFile = configurationFile;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getStartDataRegisterAddress() {
        return _startDataRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setStartDataRegisterAddress(int startDataRegisterAddress) {
        this._startDataRegisterAddress = startDataRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getStartMathRegisterAddress() {
        return _startMathRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setStartMathRegisterAddress(int startMathRegisterAddress) {
        this._startMathRegisterAddress = startMathRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getStartAlarmsRegisterAddress() {
        return _startAlarmsRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setStartAlarmsRegisterAddress(int startAlarmsRegisterAddress) {
        this._startAlarmsRegisterAddress = startAlarmsRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getStartMathStatusRegisterAddress() {
        return _startMathStatusRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setStartMathStatusRegisterAddress(int startMathStatusRegisterAddress) {
        this._startMathStatusRegisterAddress = startMathStatusRegisterAddress;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getNumberOfDataPoints() {
        return _numberOfDataPoints;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setNumberOfDataPoints(int numberOfDataPoints) {
        this._numberOfDataPoints = numberOfDataPoints;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getDataRegisterMultiplicationConstant() {
        return _dataRegisterMultiplicationConstant;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setDataRegisterMultiplicationConstant(int dataRegisterMultiplicationConstant) {
        this._dataRegisterMultiplicationConstant = dataRegisterMultiplicationConstant;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getNumberOfMathPoints() {
        return _numberOfMathPoints;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setNumberOfMathPoints(int numberOfMathPoints) {
        this._numberOfMathPoints = numberOfMathPoints;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getMathRegisterMultiplicationConstant() {
        return _mathRegisterMultiplicationConstant;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setMathRegisterMultiplicationConstant(int mathRegisterMultiplicationConstant) {
        this._mathRegisterMultiplicationConstant = mathRegisterMultiplicationConstant;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addTag(String tag) {
        _tags.addLast(tag);
    }

    /**
     * @inheritDoc
     */
    @Override
    public FastTable<String> getTags() {
        return _tags;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setTags(FastTable<String> tags) {
        this._tags = tags;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addUnit(String unit) {
        _units.addLast(unit);
    }

    /**
     * @inheritDoc
     */
    @Override
    public FastTable<String> getUnits() {
        return _units;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setUnits(FastTable<String> units) {
        this._units = units;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addDecimal(int decimal) {
        _decimals.addLast(decimal);
    }

    /**
     * @inheritDoc
     */
    @Override
    public FastTable<Integer> getDecimals() {
        return _decimals;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setDecimals(FastTable<Integer> decimals) {
        this._decimals = decimals;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addAlarmType(String[] alarmType) {
        _alarmsType.addLast(alarmType);
    }

    /**
     * @inheritDoc
     */
    @Override
    public FastTable<String[]> getAlarmsType() {
        return _alarmsType;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setAlarmsType(FastTable<String[]> alarmsTypes) {
        this._alarmsType = alarmsTypes;
    }

    /**
     * @inheritDoc
     */
    @Override
    public FastTable<DataTable> getCollectedData() {
        return _collectedData;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setCollectedData(FastTable<DataTable> collectedData) {
        this._collectedData = collectedData;
    }

    /**
     * @inheritDoc
     */
    @Override
    public MbusTcpMasterProtocol getModbusConnection() {
        return _modbusConnection;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setModbusConnection(MbusTcpMasterProtocol modbusConnection) {
        this._modbusConnection = modbusConnection;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Database getDatabase() {
        return _database;
    }

    /**
     * Loads the recorder configuration file.
     *
     * @throws IOException Throws IOException if there is a problem reading the
     * configuration file.
     */
    private void loadConfigurationFile() throws IOException {
        Path filePath = Paths.get(this._configurationFile);
        List<String> lines = Files.readAllLines(filePath, Charset.forName("ISO-8859-1"));
        parseConfiguration(lines);
    }

    /**
     * Prepares a new FastTable of DataPoints to be filled with data. The
     * DataPoints are filled appropriate data tag, units, and alarm types. The
     * rest of the DataPoint is left NULL to be filled in later.
     *
     * @return FastTable of DataPoints.
     */
    protected FastTable<DataPoint> fillDataPoints() {
        FastTable<DataPoint> table = new FastTable<>();
        for (int i = 0; i < (this.getNumberOfDataPoints() + this.getNumberOfMathPoints()); i++) {
            DataPoint dataPoint = new DataPoint(null, this.getTags().get(i));
            dataPoint.setUnits(this.getUnits().get(i));
            dataPoint.setAlarmsType(this.getAlarmsType().get(i));
            table.addLast(dataPoint);
        }
        return table;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void connect() {
        if (!this._disconnectFlag) {
            try {
                if (this._modbusConnection.isOpen()) {
                    this._modbusConnection.closeProtocol();
                }
                this._modbusConnection.openProtocol(this._ipAddress.toString().replaceAll("/", ""));
            } catch (IOException ex) {
                try {
                    this._modbusConnection.closeProtocol();
                } catch (IOException ex1) {
                    DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                            "Error closing connection after failed connection attempt.\n" + ex1.toString());
                }
                this._disconnectFlag = true;
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error connecting to " + this._modelNumber + " at IP Address: "
                        + this._ipAddress.toString() + ". Device is disconnected.\n" + ex.toString());
            }
        } else {
            this._disconnectCount++;
            if (this._disconnectCount == 5) {
                this._disconnectCount = 0;
                try {
                    this._modbusConnection.openProtocol(this.getIpAddress().toString().replaceAll("/", ""));
                    this._disconnectFlag = false;
                    DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                            "Reconnected to " + this.getModelNumber() + " at IP Address: "
                            + this.getIpAddress().toString());
                } catch (IOException ex) {
                    try {
                        this._modbusConnection.closeProtocol();
                    } catch (IOException ex2) {
                        DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                                "Error closing connection after failed connection attempt.\n" + ex2.toString());
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void closeConnection() {
        if (this._modbusConnection.isOpen()) {
            try {
                this._modbusConnection.closeProtocol();
                this._modbusConnection.resetSuccessCounter();
                this._modbusConnection.resetTotalCounter();
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error disconnecting from " + this._modelNumber + " at IP Address: "
                        + this._ipAddress.toString() + ".\n" + ex.toString());
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void dbConnect() {
        this._database.connect();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this._modelNumber);
        hash = 29 * hash + Objects.hashCode(this._ipAddress);
        hash = 29 * hash + this._port;
        hash = 29 * hash + this._unitID;
        hash = 29 * hash + this._recorderID;
        hash = 29 * hash + Objects.hashCode(this._configurationFile);
        hash = 29 * hash + this._startDataRegisterAddress;
        hash = 29 * hash + this._startMathRegisterAddress;
        hash = 29 * hash + this._startAlarmsRegisterAddress;
        hash = 29 * hash + this._startMathStatusRegisterAddress;
        hash = 29 * hash + this._numberOfDataPoints;
        hash = 29 * hash + this._dataRegisterMultiplicationConstant;
        hash = 29 * hash + this._numberOfMathPoints;
        hash = 29 * hash + this._mathRegisterMultiplicationConstant;
        hash = 29 * hash + Objects.hashCode(this._tags);
        hash = 29 * hash + Objects.hashCode(this._units);
        hash = 29 * hash + Objects.hashCode(this._decimals);
        hash = 29 * hash + Objects.hashCode(this._alarmsType);
        hash = 29 * hash + Objects.hashCode(this._collectedData);
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
        final RecorderAbstract other = (RecorderAbstract) obj;
        if (this._port != other._port) {
            return false;
        }
        if (this._unitID != other._unitID) {
            return false;
        }
        if (this._recorderID != other._recorderID) {
            return false;
        }
        if (this._startDataRegisterAddress != other._startDataRegisterAddress) {
            return false;
        }
        if (this._startMathRegisterAddress != other._startMathRegisterAddress) {
            return false;
        }
        if (this._startAlarmsRegisterAddress != other._startAlarmsRegisterAddress) {
            return false;
        }
        if (this._startMathStatusRegisterAddress != other._startMathStatusRegisterAddress) {
            return false;
        }
        if (this._numberOfDataPoints != other._numberOfDataPoints) {
            return false;
        }
        if (this._dataRegisterMultiplicationConstant != other._dataRegisterMultiplicationConstant) {
            return false;
        }
        if (this._numberOfMathPoints != other._numberOfMathPoints) {
            return false;
        }
        if (this._mathRegisterMultiplicationConstant != other._mathRegisterMultiplicationConstant) {
            return false;
        }
        if (!Objects.equals(this._modelNumber, other._modelNumber)) {
            return false;
        }
        if (!Objects.equals(this._configurationFile, other._configurationFile)) {
            return false;
        }
        if (!Objects.equals(this._ipAddress, other._ipAddress)) {
            return false;
        }
        if (!Objects.equals(this._tags, other._tags)) {
            return false;
        }
        if (!Objects.equals(this._units, other._units)) {
            return false;
        }
        if (!Objects.equals(this._decimals, other._decimals)) {
            return false;
        }
        if (!Objects.equals(this._alarmsType, other._alarmsType)) {
            return false;
        }
        if (!Objects.equals(this._collectedData, other._collectedData)) {
            return false;
        }
        return true;
    }
}
