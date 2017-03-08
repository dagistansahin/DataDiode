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

import codec.DataPointSend;
import communicators.ReceiveEthernetStrategy;
import javolution.util.FastTable;

/**
 * This class holds specific information regarding the registers on the Modbus
 * device that have desired data. This object will hold the information
 * regarding the register reference numbers, number of registers, tags for the
 * data, and the type of data to be collected.
 *
 * @author Scott Arneson
 */
public class ModbusDeviceModule {

    private int _startRegisterAddress;
    private int _numberOfRegisters;
    private DataType _dataType;
    private FastTable<String> _tags;
    private FastTable<String> _units;
    private FastTable<Integer> _decimals;
    private FastTable<Integer> _referenceNumbers;
    private ModbusDevice _parentDevice;

    /**
     * Enumeration for the data types to make programming more easily readable.
     * Data types hold information regarding the size of the data and how the
     * data is stored in the registers.
     */
    public enum DataType {

        SHORTHOLDING(0),
        SHORTINPUT(1),
        BIGENDIANHOLDING(2),
        BIGENDIANINPUT(3),
        LITTLEENDIANHOLDING(4),
        LITTLEENDIANINPUT(5),
        SINGLEBITHOLDING(6),
        SINGLEBITINPUT(7);
        private final int value;

        private DataType(int value) {
            this.value = value;
        }
    }

    /**
     * Creates a ModbusDeviceModule object that is mostly empty with a few
     * default values set.
     */
    public ModbusDeviceModule() {
        this._dataType = DataType.SHORTHOLDING;
        this._tags = new FastTable<>();
        this._units = new FastTable<>();
        this._decimals = new FastTable<>();
        this._referenceNumbers = new FastTable<>();
    }

    /**
     * Creates a ModbusDeviceModule object that is mostly empty with the parent
     * Modbus device set.
     *
     * @param device ModbusDevice object that owns the module.
     */
    public ModbusDeviceModule(ModbusDevice device) {
        this._dataType = DataType.SHORTHOLDING;
        this._tags = new FastTable<>();
        this._units = new FastTable<>();
        this._decimals = new FastTable<>();
        this._referenceNumbers = new FastTable<>();
        this._parentDevice = device;
    }

    /**
     * Creates a ModubsDevice Module with the register parameters and parent
     * Modbus device set.
     *
     * @param startRegisterAddress Int value reference of start register set.
     * @param numberOfRegisters Int value number of registers to gather set.
     * @param device ModbusDevice object that owns the module.
     */
    public ModbusDeviceModule(int startRegisterAddress, int numberOfRegisters, ModbusDevice device) {
        this._startRegisterAddress = startRegisterAddress;
        this._numberOfRegisters = numberOfRegisters;
        this._dataType = DataType.SHORTHOLDING;
        this._tags = new FastTable<>();
        this._units = new FastTable<>();
        this._decimals = new FastTable<>();
        this._referenceNumbers = new FastTable<>();
        this._parentDevice = device;
    }

    /**
     * Adds a tag to the module. Sets necessary parameters such as tag name,
     * units, decimals, and tag register reference value.
     *
     * @param tag String value tag name.
     * @param units String value units of tag.
     * @param decimal Integer value number of decimals associated with tag if
     * decimal information is not stored in the register of the tag. If decimal
     * information is stored in the registers, enter 0.
     * @param reference Integer value reference number of register associated
     * with this tag.
     */
    public void addTag(String tag, String units, Integer decimal, Integer reference) {
        this._tags.addLast(tag);
        this._units.addLast(units);
        this._decimals.addLast(decimal);
        this._referenceNumbers.addLast(reference);
    }

    /**
     * Returns a FastTable of DataPointSend objects data collected from the
     * Modbus device specified by the module.
     *
     * @return FastTable of DataPointSend objects data collected from Modbus
     * device.
     */
    public FastTable<DataPointSend> getData() {
        FastTable<DataPointSend> dataPoints = new FastTable<>();
        switch (this._dataType) {
            case SHORTHOLDING:
                short[] shortHoldData = ReceiveEthernetStrategy.getInstance().getShortHoldData(_parentDevice, _startRegisterAddress, _numberOfRegisters);
                if (shortHoldData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(shortHoldData[this._referenceNumbers.get(i)]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case SHORTINPUT:
                short[] shortInputData = ReceiveEthernetStrategy.getInstance().getShortInputData(_parentDevice, _startRegisterAddress, _numberOfRegisters);
                if (shortInputData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(shortInputData[this._referenceNumbers.get(i)]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case BIGENDIANHOLDING:
                int[] bigHoldData = ReceiveEthernetStrategy.getInstance().getBigEndianHoldData(_parentDevice, _startRegisterAddress, _numberOfRegisters / 2);
                if (bigHoldData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(bigHoldData[this._referenceNumbers.get(i) / 2]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case BIGENDIANINPUT:
                int[] bigInputData = ReceiveEthernetStrategy.getInstance().getBigEndianInputData(_parentDevice, _startRegisterAddress, _numberOfRegisters / 2);
                if (bigInputData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(bigInputData[this._referenceNumbers.get(i) / 2]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case LITTLEENDIANHOLDING:
                int[] littleHoldData = ReceiveEthernetStrategy.getInstance().getLittleEndianHoldData(_parentDevice, _startRegisterAddress, _numberOfRegisters / 2);
                if (littleHoldData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(littleHoldData[this._referenceNumbers.get(i) / 2]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case LITTLEENDIANINPUT:
                int[] littleInputData = ReceiveEthernetStrategy.getInstance().getLittleEndianInputData(_parentDevice, _startRegisterAddress, _numberOfRegisters / 2);
                if (littleInputData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(littleInputData[this._referenceNumbers.get(i) / 2]
                                / Math.pow(10, this._decimals.get(i)));
                        dataPoints.addLast(data);
                    }
                }
                break;
            case SINGLEBITHOLDING:
                short[] singleHoldData = ReceiveEthernetStrategy.getInstance().getShortHoldData(_parentDevice, _startRegisterAddress, _numberOfRegisters);
                if (singleHoldData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(((singleHoldData[this._referenceNumbers.get(i)]
                                >> this._decimals.get(i)) & 0x01) / 1.0);
                        dataPoints.addLast(data);
                    }
                }
                break;
            case SINGLEBITINPUT:
                short[] singleInputData = ReceiveEthernetStrategy.getInstance().getShortInputData(_parentDevice, _startRegisterAddress, _numberOfRegisters);
                if (singleInputData.length > 0) {
                    for (int i = 0; i < this._tags.size(); i++) {
                        DataPointSend data = new DataPointSend(((singleInputData[this._referenceNumbers.get(i)]
                                >> this._decimals.get(i)) & 0x01) / 1.0);
                        dataPoints.addLast(data);
                    }
                }
                break;
        }
        return dataPoints;
    }

    /**
     * Returns the reference of the module start register.
     * 
     * @return Int value reference of the module start register.
     */
    public int getStartRegisterAddress() {
        return _startRegisterAddress;
    }

    /**
     * Sets the reference of the module start register.
     *
     * @param startRegisterAddress Int value reference of module start register
     * to be set.
     */
    public void setStartRegisterAddress(int startRegisterAddress) {
        this._startRegisterAddress = startRegisterAddress;
    }

    /**
     * Returns the number of registers in the module.
     * 
     * @return Int value number of registers in the module.
     */
    public int getNumberOfRegisters() {
        return _numberOfRegisters;
    }

    /**
     * Sets the number of registers in the module.
     * 
     * @param numberOfRegisters Int value number of registers to be set.
     */
    public void setNumberOfRegisters(int numberOfRegisters) {
        this._numberOfRegisters = numberOfRegisters;
    }

    /**
     * Returns the data type of the module.
     * 
     * @return DataType value of the module data.
     */
    public DataType getDataType() {
        return _dataType;
    }

    /**
     * Sets the data type of the module data.
     * 
     * @param dataType DataType of data to be set.
     */
    public void setDataType(DataType dataType) {
        this._dataType = dataType;
    }

    /**
     * Returns a list of the tag names in the module.
     *
     * @return FastTable of String values.
     */
    public FastTable<String> getTags() {
        return _tags;
    }

    /**
     * Sets the list of tag names in the module.
     * 
     * @param tags FastTable of String values tag names to be set.
     */
    public void setTags(FastTable<String> tags) {
        this._tags = tags;
    }

    /**
     * Returns a list of the units in the module.
     * 
     * @return FastTable of String values.
     */
    public FastTable<String> getUnits() {
        return _units;
    }

    /**
     * Sets the list of units in the module.
     * 
     * @param units FastTable of String value units to be set.
     */
    public void setUnits(FastTable<String> units) {
        this._units = units;
    }

    /**
     * Returns a list of decimal values in the module.
     * 
     * @return FastTable of Integer values.
     */
    public FastTable<Integer> getDecimals() {
        return _decimals;
    }

    /**
     * Sets the list of decimal values in the module.
     * 
     * @param decimals FastTable of Integer values to be set.
     */
    public void setDecimals(FastTable<Integer> decimals) {
        this._decimals = decimals;
    }

    /**
     * Returns a list of tag reference numbers in the module.
     * 
     * @return FastTable of Integer values.
     */
    public FastTable<Integer> getReferenceNumbers() {
        return _referenceNumbers;
    }

    /**
     * Sets the list of tag reference values in the module.
     * 
     * @param referenceNumbers FastTable of Integer reference values to be set.
     */
    public void setReferenceNumbers(FastTable<Integer> referenceNumbers) {
        this._referenceNumbers = referenceNumbers;
    }

    /**
     * Returns the parent Modbus device of the module..
     * 
     * @return ModbusDevice parent of module.
     */
    public ModbusDevice getParentDevice() {
        return _parentDevice;
    }

    /**
     * Sets the parent Modbus device of the module.
     * 
     * @param parentDevice ModbusDevice parent to be set.
     */
    public void setParentDevice(ModbusDevice parentDevice) {
        this._parentDevice = parentDevice;
    }
}
