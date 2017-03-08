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
package communicators;

import codec.DataDiodeLogger;
import codec.DataDiodeLogger.log;
import java.io.IOException;
import devices.ModbusDevice;
import devices.RecorderAbstract;

/**
 * This class handles the communication using Modbus/TCP protocol with Yokogawa
 * recorders and other Modbus devices. This class is a singleton class and only
 * one instance can be created during a program run.
 *
 * @author Scott Arneson
 */
public class ReceiveEthernetStrategy implements Communicator {

    private static ReceiveEthernetStrategy _receiveEthernetStrategy;

    /**
     * Returns an instance of the ReceiveEthernetStrategy class. If an instance
     * has not already been created, one will be created. Only one instance of
     * the class can be created during a program run.
     *
     * @return An instance of the ReceiveEthernetStrategy class.
     */
    public static ReceiveEthernetStrategy getInstance() {
        if (_receiveEthernetStrategy == null) {
            _receiveEthernetStrategy = new ReceiveEthernetStrategy();
        }
        return _receiveEthernetStrategy;
    }

    /**
     * Creates a ReceiveEthernetStrategy object.
     */
    private ReceiveEthernetStrategy() {
    }

    /**
     * Returns data from Yokogawa recorder that is 16 bits in size.
     *
     * @param recorder Yokogawa recorder to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of short value data from Yokogawa recorder.
     */
    public short[] getShortData(RecorderAbstract recorder, int startRegister, int numberOfRegisters) {
        short[] data = new short[numberOfRegisters];
        if (numberOfRegisters > 0) {
            try {
                recorder.getModbusConnection().readInputRegisters(recorder.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + recorder.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Yokogawa recorder that is 32 bits in size and stored
     * Big Endian style.
     *
     * @param recorder Yokogawa recorder to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Yokogawa recorder.
     */
    public int[] getBigEndianData(RecorderAbstract recorder, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        recorder.getModbusConnection().configureBigEndianInts();
        if (numberOfRegisters > 0) {
            try {
                recorder.getModbusConnection().readInputRegisters(recorder.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + recorder.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Yokogawa recorder that is 32 bits in size and stored
     * Little Endian style.
     * 
     * @param recorder Yokogawa recorder to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Yokogawa recorder.
     */
    public int[] getLittleEndianData(RecorderAbstract recorder, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        recorder.getModbusConnection().configureLittleEndianInts();
        if (numberOfRegisters > 0) {
            try {
                recorder.getModbusConnection().readInputRegisters(recorder.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + recorder.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 16 bits in size and stored in
     * input registers.
     *
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of short value data from Modbus device.
     */
    public short[] getShortInputData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        short[] data = new short[numberOfRegisters];
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readInputRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 16 bits in size and stored in
     * holding registers.
     * 
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of short value data from Modbus device.
     */
    public short[] getShortHoldData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        short[] data = new short[numberOfRegisters];
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readMultipleRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 32 bits in size and stored Big
     * Endian style in input registers.
     *
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Modbus device.
     */
    public int[] getBigEndianInputData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        device.getModbusConnection().configureBigEndianInts();
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readInputRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 32 bits in size and stored Big
     * Endian style in holding registers.
     * 
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Modbus device.
     */
    public int[] getBigEndianHoldData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        device.getModbusConnection().configureBigEndianInts();
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readMultipleRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 32 bits in size and stored Little
     * Endian style in input registers.
     *
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Modbus device.
     */
    public int[] getLittleEndianInputData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        device.getModbusConnection().configureLittleEndianInts();
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readInputRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }

    /**
     * Returns data from Modbus device that is 32 bits in size and stored Little
     * Endian style in holding registers.
     * 
     * @param device Modbus device to be communicated with.
     * @param startRegister Int value reference of starting register of desired
     * data.
     * @param numberOfRegisters Int value number of registers to be gathered.
     * @return Array of int value data from Modbus device.
     */
    public int[] getLittleEndianHoldData(ModbusDevice device, int startRegister, int numberOfRegisters) {
        int[] data = new int[numberOfRegisters];
        device.getModbusConnection().configureLittleEndianInts();
        if (numberOfRegisters > 0) {
            try {
                device.getModbusConnection().readMultipleRegisters(device.getUnitID(), startRegister, data);
            } catch (IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                        "Error getting data from recorder at IP Address: "
                        + device.getIpAddress().toString() + ".\n" + ex.toString());
            }
        }
        return data;
    }
}
