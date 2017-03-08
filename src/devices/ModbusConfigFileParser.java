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
import codec.GlobalDataHandler;
import devices.ModbusDeviceModule.DataType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javolution.util.FastTable;

/**
 * This class is to parse through the Modbus device configuration file. The
 * configuration file is for Modbus devices that are not Yokogawa Recorders. The
 * class will create ModbusDevice objects and ModbusDeviceModules and fill in
 * the necessary information for these objects. This class is a singleton class
 * and only one ModbusConfigFileParser object can be created during a program
 * run.
 *
 * @author Scott Arneson
 */
public class ModbusConfigFileParser {

    private static ModbusConfigFileParser _instance;

    /**
     * Returns an instance of the ModbusConfigFileParser class. If an instance
     * has not already been created, one will be created. Only one instance of
     * this class can be created during a program run.
     *
     * @return An instance of the ModbusConfigFileParser class.
     */
    public static ModbusConfigFileParser getInstance() {
        if (_instance == null) {
            _instance = new ModbusConfigFileParser();
        }
        return _instance;
    }

    /**
     * Creates a ModbusConfigFileParser object.
     */
    private ModbusConfigFileParser() {
    }

    /**
     * Reads through the Modbus device configuration file. Creates ModbusDevice
     * objects and ModbusDeviceModule objects listed in the configuration file
     * and fills in the necessary information to communicate with each Modbus
     * Device.
     *
     * @param fileName String filename of the configuration file.
     */
    public void parseFile(String fileName) {
        Path filePath = Paths.get(fileName);
        try {
            List<String> listOfLines = Files.readAllLines(filePath, Charset.forName("ISO-8859-1"));
            FastTable<String> lines = new FastTable<>();
            for (String line : listOfLines) {
                lines.addLast(line.trim());
            }
            int i = 0;
            while (!lines.isEmpty()) {
                String line = lines.removeFirst();
                if (line.contains("**") || line.isEmpty()) {
                } else if (line.startsWith("Device Name:")) {
                    String deviceName = line.substring(line.indexOf(':') + 1).trim();
                    ModbusDevice device = new ModbusDevice(deviceName);
                    device.setDeviceID(i);
                    GlobalDataHandler.getInstance().getModbusDevices().addLast(device);
                    i++;
                } else if (line.startsWith("IP Address:")) {
                    String ipAddress = line.substring(line.indexOf(':') + 1).trim();
                    GlobalDataHandler.getInstance().getModbusDevices().getLast().setIpAddress(ipAddress);
                } else if (line.startsWith("Slave number:")) {
                    int unitID = Integer.valueOf(line.substring(line.indexOf(':') + 1).trim());
                    GlobalDataHandler.getInstance().getModbusDevices().getLast().setUnitID(unitID);
                } else if (line.startsWith("Port:")) {
                    int port = Integer.valueOf(line.substring(line.indexOf(':') + 1).trim());
                    GlobalDataHandler.getInstance().getModbusDevices().getLast().setPort(port);
                } else if (line.startsWith("Priority:")) {
                    int priority = Integer.valueOf(line.substring(line.indexOf(':') + 1).trim());
                    GlobalDataHandler.getInstance().getModbusDevices().getLast().setPriority(priority);
                } else if (line.startsWith("Registers:")) {
                    ModbusDeviceModule module = new ModbusDeviceModule();
                    int startRegister = Integer.valueOf(line.substring(line.indexOf(':') + 1, line.indexOf(',')).trim());
                    int endRegister = Integer.valueOf(line.substring(line.indexOf(',') + 1).trim());
                    module.setStartRegisterAddress(startRegister - 1);
                    module.setNumberOfRegisters(endRegister - startRegister + 1);
                    GlobalDataHandler.getInstance().getModbusDevices().getLast().addModule(module);
                } else if (line.startsWith("Data Type:")) {
                    String type = line.substring(line.indexOf(':') + 1).trim();
                    if (type.equalsIgnoreCase("short holding")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.SHORTHOLDING);
                    } else if (type.equalsIgnoreCase("short input")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.SHORTINPUT);
                    } else if (type.equalsIgnoreCase("big endian holding")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.BIGENDIANHOLDING);
                    } else if (type.equalsIgnoreCase("big endian input")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.BIGENDIANINPUT);
                    } else if (type.equalsIgnoreCase("little endian holding")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.LITTLEENDIANHOLDING);
                    } else if (type.equalsIgnoreCase("little endian input")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.LITTLEENDIANINPUT);
                    } else if (type.equalsIgnoreCase("single bit holding")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.SINGLEBITHOLDING);
                    } else if (type.equalsIgnoreCase("single bit input")) {
                        GlobalDataHandler.getInstance().getModbusDevices().getLast().
                                getModules().getLast().setDataType(DataType.SINGLEBITINPUT);
                    }
                } else {
                    String[] tagInfo = line.split(",");
                    String tagName = tagInfo[0].trim();
                    String units = tagInfo[1].trim();
                    int decimals = Integer.valueOf(tagInfo[2].trim());
                    int register = Integer.valueOf(tagInfo[3].trim());
                    ModbusDeviceModule module = GlobalDataHandler.getInstance().
                            getModbusDevices().getLast().getModules().getLast();
                    int startRegister = module.getStartRegisterAddress();
                    module.addTag(tagName, units, decimals, register - startRegister - 1);
                }
            }
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Error reading ModbusDevicesConfig file.\n" + ex.toString());
        }
    }
}
