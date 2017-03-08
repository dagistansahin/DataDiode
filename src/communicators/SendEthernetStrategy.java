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

import codec.DataTable;
import codec.GlobalDataHandler;
import devices.ModbusDevice;
import devices.RecorderInterface;

/**
 * This class checks each of the GlobalDataHandler data tables to see if any
 * data is available to be sent to the database server. If any data is
 * available, it determines which recorder or Modbus device is responsible for
 * the data and has it send the data to the database. This class is a singleton
 * class and only one instance of this object can be made in a program run.
 *
 * @author Scott Arneson
 */
public class SendEthernetStrategy implements Communicator {

    private static SendEthernetStrategy _sendEthernetStrategy;
    private DataTable _dataTable;
    private RecorderInterface _recorder;
    private ModbusDevice _device;

    /**
     * Returns an instance of the SendEthernetStrategy object. If a
     * SendEthernetStrategy object has not already been created, one will be
     * created. Only one instance of this object can exist in a program run.
     *
     * @return An instance of the SendEthernetStrategy object.
     */
    public static SendEthernetStrategy getInstance() {
        if (_sendEthernetStrategy == null) {
            _sendEthernetStrategy = new SendEthernetStrategy();
        }
        return _sendEthernetStrategy;
    }

    /**
     * Creates an instance of the SendEthernetStrategy object.
     */
    private SendEthernetStrategy() {
    }

    /**
     * Checks to see if there is data available on any of the three data
     * threads. If any data is available, looks up the applicable recorder or
     * Modbus device and calls function to update database.
     */
    public void sendData() {
        try {
            if (!GlobalDataHandler.getInstance().getCollectedDataThread1().isEmpty()) {

                _dataTable = GlobalDataHandler.getInstance().getCollectedDataThread1()
                        .removeFirst();

                if (_dataTable.getDeviceType().equalsIgnoreCase("Recorder")) {
                    _recorder = GlobalDataHandler.getInstance().getRecorders()
                            .get(_dataTable.getDeviceID());
                    _recorder.updateDatabase(_dataTable);
                } else {
                    _device = GlobalDataHandler.getInstance().getModbusDevices()
                            .get(_dataTable.getDeviceID());
                    _device.updateDatabase(_dataTable);
                }
            }

            if (!GlobalDataHandler.getInstance().getCollectedDataThread2().isEmpty()) {

                _dataTable = GlobalDataHandler.getInstance().getCollectedDataThread2()
                        .removeFirst();

                if (_dataTable.getDeviceType().equalsIgnoreCase("Recorder")) {
                    _recorder = GlobalDataHandler.getInstance().getRecorders()
                            .get(_dataTable.getDeviceID());
                    _recorder.updateDatabase(_dataTable);
                } else {
                    _device = GlobalDataHandler.getInstance().getModbusDevices()
                            .get(_dataTable.getDeviceID());
                    _device.updateDatabase(_dataTable);
                }
            }

            if (!GlobalDataHandler.getInstance().getCollectedDataThread3().isEmpty()) {

                _dataTable = GlobalDataHandler.getInstance().getCollectedDataThread3()
                        .removeFirst();

                if (_dataTable.getDeviceType().equalsIgnoreCase("Recorder")) {
                    _recorder = GlobalDataHandler.getInstance().getRecorders()
                            .get(_dataTable.getDeviceID());
                    _recorder.updateDatabase(_dataTable);
                } else {
                    _device = GlobalDataHandler.getInstance().getModbusDevices()
                            .get(_dataTable.getDeviceID());
                    _device.updateDatabase(_dataTable);
                }
            }

        } catch (Exception ex) {
            return;
        }
    }
}
