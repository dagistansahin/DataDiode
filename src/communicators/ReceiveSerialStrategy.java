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
import codec.DataTable;
import codec.GlobalDataHandler;
import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.ObjectInputStream;
import javolution.util.FastTable;

/**
 * This class handles communication that is received over serial cables. It
 * holds the serial port open and will deserialize the DataTable objects that
 * come across and place them in collected data FastTables held by the
 * GlobalDataHandler.
 *
 * @author Scott Arneson
 */
public class ReceiveSerialStrategy implements Communicator {

    private SerialPort _serialPort;
    private FastTable<DataTable> _collectedData;

    /**
     * Creates a ReceiveSerialStrategy object. Also opens the serial port that
     * is indicated by the parameter and sets up serial communication.
     *
     * @param num Int value number that indicates which serial port to open and
     * which GlobalHandlerData FastTable to use to hold incoming data.
     */
    public ReceiveSerialStrategy(int num) {

        //Setup desired port name base on input number and FastTable to hold data\
        String portName = null;
        switch (num) {
            case 1:
                portName = "/dev/ttyS0";
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread1();
                break;
            case 2:
                portName = "/dev/ttyS1";
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread2();
                break;
            case 3:
                portName = "/dev/ttyS2";
                this._collectedData = GlobalDataHandler.getInstance().getCollectedDataThread3();
                break;
            default:
                break;
        }
        if (portName != null) {
            //Open Serial port and set parameters
            _serialPort = SerialPort.getCommPort(portName);
            _serialPort.openPort();
            _serialPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            _serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            if (_serialPort.isOpen()) {
                DataDiodeLogger.getInstance().addLogs(log.NORMAL, "Serial Port "
                        + _serialPort.getSystemPortName() + " is open.");
            }
        }
    }

    /**
     * Subroutine to get the data from the port previously set up. Expects a
     * serialized DataTable object. It will deserialize the DataTable and add it
     * to the appropriate collected data table.
     */
    public void getData() {
        DataTable data = null;
        try {

            //Once data available it deserializes it and convert into DataTable
            ObjectInputStream in = new ObjectInputStream(this.getSerialPort().getInputStream());
            data = (DataTable) in.readObject();
            in.close();

            //Add the deserialized FastTable to the collected data table
            _collectedData.addLast(data);

        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Unable to deserialize data.\n" + ex.toString());
        }
    }

    /**
     * Returns the serial port that is held open by the ReceiveSerialStrategy
     * object.
     *
     * @return SerialPort object.
     */
    public SerialPort getSerialPort() {
        return _serialPort;
    }

    /**
     * Closes serial port connection.
     */
    public void close() {
        this._serialPort.closePort();
        DataDiodeLogger.getInstance().addLogs(log.NORMAL, "Serial Port "
                + _serialPort.getSystemPortName() + " is closed.");
    }
}
