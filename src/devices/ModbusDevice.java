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
import codec.DataPointSend;
import codec.DataTable;
import codec.GlobalDataHandler;
import com.focus_sw.fieldtalk.MbusTcpMasterProtocol;
import communicators.Database;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import javolution.util.FastTable;

/**
 * This class hold the information needed to communicate with each Modbus
 * Device. It also holds a list of ModbusDeviceModules that contain the
 * necessary information on the specific registers for the data to be gathered.
 *
 * @author Scott Arneson
 */
public class ModbusDevice {

    private String _deviceName;
    private InetAddress _ipAddress;
    private int _unitID;
    private int _deviceID;
    private int _port;
    private FastTable<ModbusDeviceModule> _modules;
    private FastTable<DataTable> _collectedData;
    private MbusTcpMasterProtocol _modbusConnection;
    private Database _database;
    private int _count;
    private Boolean _disconnectFlag;
    private int _disconnectCount;

    /**
     * Creates a ModbusDevice object sets up basic default parameters.
     * 
     * @param deviceName String value device name for Modbus device. 
     */
    public ModbusDevice(String deviceName) {
        this._deviceName = deviceName;
        this._port = 502;
        this._modules = new FastTable<>();
        this._modbusConnection = new MbusTcpMasterProtocol();
        this._modbusConnection.configureCountFromZero();
        this._modbusConnection.setTimeout(0);
        this._modbusConnection.setPollDelay(0);
        this._database = new Database();
        this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread3();
        this._count = 9;
        this._disconnectFlag = false;
        this._disconnectCount = 0;
    }

    /**
     * Creates a ModbuseDevice object and fills in some of the specific
     * parameters.
     *
     * @param deviceName String value device name for Modbus device.
     * @param ipAddress String value IP address for Modbus device.
     * @param unitID Int value unit ID for modbus device. Needed for Modbus
     * protocol.
     * @param priority Int value priority number. Determines which thread the
     * collected data will be sent to.
     */
    public ModbusDevice(String deviceName, String ipAddress, int unitID, int priority) {
        this._deviceName = deviceName;
        this._unitID = unitID;
        this._port = 502;
        this._count = 9;
        this._modules = new FastTable<>();
        this._modbusConnection = new MbusTcpMasterProtocol();
        this._modbusConnection.configureCountFromZero();
        this._modbusConnection.setTimeout(0);
        this._modbusConnection.setPollDelay(0);
        this._database = new Database();
        switch (priority) {
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
                        "Priority level for " + this._deviceName + " at IP address " + ipAddress
                        + "is not between 1 and 3.  Setting priority level to 3.");
        }
        try {
            this._ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "IP Address for " + deviceName + " appears invalid: "
                    + ipAddress + ".\n" + ex.toString());
        }
    }

    /**
     * Connects to the database.
     */
    public void dbConnect() {
        this._database.connect();
    }

    /**
     * Adds a ModbusDeviceModule to module table. ModbuseDeviceModule objects
     * hold specific information regarding the registers to be gathered.
     *
     * @param module ModbusDeviceModule object to be added.
     */
    public void addModule(ModbusDeviceModule module) {
        module.setParentDevice(this);
        this._modules.addLast(module);
    }

    /**
     * Gets the data from the Modbus device.
     */
    public void getData() {
        this.connect();
        if (this._modbusConnection.isOpen()) {
            FastTable<DataPointSend> dataPoints = new FastTable<>();
            Timestamp time = new Timestamp(System.currentTimeMillis());
            int tagCount = 0;
            for (ModbusDeviceModule module : this._modules) {
                tagCount += module.getTags().size();
                FastTable<DataPointSend> data = module.getData();
                for (DataPointSend dataPointSend : data) {
                    dataPoints.addLast(dataPointSend);
                }
            }
            this.closeConnection();
            if (dataPoints.size() == tagCount) {
                DataTable dataTable = new DataTable(time, "Modbus Device", this._deviceID);
                dataTable.setData(dataPoints);
                this._collectedData.addLast(dataTable);
            } else {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Problem getting data from " + this._deviceName + " at IP Address: "
                        + this.getIpAddress().toString());
            }
        }
    }

    /**
     * Updates the database.
     *
     * @param dataTable DataTable object that holds the data to be sent to the
     * database.
     */
    public void updateDatabase(DataTable dataTable) {
        Timestamp time = dataTable.getTime();
        FastTable<Double> data = new FastTable<>();
        for (DataPointSend dataPoint : dataTable.getData()) {
            data.addLast(dataPoint.getData());
        }
        this._count++;
        switch (this._count) {
            case 10:
                this._database.updateModbusDeviceRecord(this, time, data);
                this._count = 0;
                break;
            default:
                this._database.updateModbusDeviceCurrent(this, time, data);
                break;
        }
    }

    /**
     * Connects to the Modbus device. If connection fails, a flag is set and the
     * program will wait 5 communication cycles before trying to reconnect. This
     * process repeats until communication is reestablished.
     */
    public void connect() {
        if (!this._disconnectFlag) {
            try {
                if (this._modbusConnection.isOpen()) {
                    this._modbusConnection.closeProtocol();
                }
                this._modbusConnection.openProtocol(this.getIpAddress().toString().replaceAll("/", ""));
            } catch (IOException ex) {
                try {
                    this._modbusConnection.closeProtocol();
                } catch (IOException ex1) {
                    DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                            "Error closing connection after failed connection attempt.\n" + ex1.toString());
                }
                this._disconnectFlag = true;
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error connecting to " + this.getDeviceName() + " at IP Address: "
                        + this.getIpAddress().toString() + ". Device is disconnected.\n" + ex.toString());
            }
        } else {
            this._disconnectCount++;
            if (this._disconnectCount == 5) {
                this._disconnectCount = 0;
                try {
                    this._modbusConnection.openProtocol(this.getIpAddress().toString().replaceAll("/", ""));
                    this._disconnectFlag = false;
                    DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                            "Reconnected to " + this.getDeviceName() + " at IP Address: "
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
     * Closes the connection to the Modbus device.
     */
    public void closeConnection() {
        if (this.getModbusConnection().isOpen()) {
            try {
                this._modbusConnection.closeProtocol();
                this._modbusConnection.resetSuccessCounter();
                this._modbusConnection.resetTotalCounter();
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error disconnecting from " + this.getDeviceName() + " at IP Address: "
                        + this.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
    }

    /**
     * Sets the priority of the Modbus device. The priority determines which
     * thread will send the data across the serial cables.
     *
     * @param priority Int value priority to be set.
     */
    public void setPriority(int priority) {
        switch (priority) {
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
                        "Priority level for " + this._deviceName + " at IP address " + _ipAddress
                        + "is not between 1 and 3.  Setting priority level to 3.");
        }
    }

    /**
     * Returns a FastTable of String values that are all the tags associated
     * with the Modbus device.
     *
     * @return FastTable of String value tags.
     */
    public FastTable<String> getTags() {
        FastTable<String> tags = new FastTable<>();
        for (ModbusDeviceModule module : this._modules) {
            FastTable<String> moduleTags = module.getTags();
            for (String tag : moduleTags) {
                tags.addLast(tag);
            }
        }
        return tags;
    }

    /**
     * Returns the String value that is the Modbus device name.
     * 
     * @return String value Modbus device name.
     */
    public String getDeviceName() {
        return _deviceName;
    }

    /**
     * Sets the Modbus device name to the given String value.
     * 
     * @param deviceName String value Modbus device name to be set.
     */
    public void setDeviceName(String deviceName) {
        this._deviceName = deviceName;
    }

    /**
     * Returns the Modbus device IP address as a InetAddress object.
     * 
     * @return InetAddress object that is the IP address of the Modbus device.
     */
    public InetAddress getIpAddress() {
        return _ipAddress;
    }

    /**
     * Sets the Modbus device IP address to the given InetAddress object.
     * 
     * @param ipAddress InetAddress object Modbus device IP address to be set.
     */
    public void setIpAddress(InetAddress ipAddress) {
        this._ipAddress = ipAddress;
    }

    /**
     * Sets the Modbus device IP address to the given String object.
     * 
     * @param ipAddress String object Modbus device IP address to be set.
     */
    public void setIpAddress(String ipAddress) {
        try {
            this._ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "IP Address for " + _deviceName + " appears invalid: "
                    + ipAddress + ".\n" + ex.toString());
        }
    }

    /**
     * Returns the Modbus device unit ID number.
     * 
     * @return Int value Modbus device unit ID number.
     */
    public int getUnitID() {
        return _unitID;
    }

    /**
     * Sets the Modbus device unit ID number to the given int value.
     * 
     * @param unitID Int value Modbus device unit ID number to be set.
     */
    public void setUnitID(int unitID) {
        this._unitID = unitID;
    }

    /**
     * Returns the Modbus device number. This number identifies a specific
     * Modbus device to allow coordination across the data diode.
     *
     * @return Int value Modbus device number.
     */
    public int getDeviceID() {
        return _deviceID;
    }

    /**
     * Sets the Modbus device number to the given int value. This number
     * identifies a specific Modbus device to allow coordination across the data
     * diode.
     *
     * @param deviceID Int value Modbus device number to be set.
     */
    public void setDeviceID(int deviceID) {
        this._deviceID = deviceID;
    }

    /**
     * Returns the Modbus device port number.
     * 
     * @return Int value Modbus device port number.
     */
    public int getPort() {
        return _port;
    }

    /**
     * Sets the Modbus device port number to the given int value.
     * 
     * @param port Int value Modbus device port number to be set.
     */
    public void setPort(int port) {
        this._port = port;
        this._modbusConnection.setPort(port);
    }

    /**
     * Returns the FastTable of ModbusDeviceModule objects that are associated
     * with this Modbus device.
     *
     * @return FastTable of ModusDeviceModule objects.
     */
    public FastTable<ModbusDeviceModule> getModules() {
        return _modules;
    }

    /**
     * Sets the table of ModbusDeviceModule objects associated with this Modbus
     * device to given FastTable.
     *
     * @param modules FastTable of ModbusDeviceModule objects to be set.
     */
    public void setModules(FastTable<ModbusDeviceModule> modules) {
        this._modules = modules;
    }

    /**
     * Returns the Modbus connection protocol object that connects to this
     * Modbus device.
     *
     * @return MbusTcpMasterProtocol object that connects to the Modbus device.
     */
    public MbusTcpMasterProtocol getModbusConnection() {
        return _modbusConnection;
    }

    /**
     * Sets the Modbus connection protocol object that will connect to this
     * Modbus device.
     *
     * @param modbusConnection MbusTcpMasterProtocol object to be set to connect
     * to the Modbus device.
     */
    public void setModbusConnection(MbusTcpMasterProtocol modbusConnection) {
        this._modbusConnection = modbusConnection;
    }
}
