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
import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * This class handles communication that is sent over serial cables. It holds
 * the serial port open and will serialize DataTable objects and send them over
 * the serial cables to the receiving side of the data diode.
 *
 * @author Scott Arneson
 */
public class SendSerialStrategy implements Communicator {

    SerialPort _serialPort;

    /**
     * Creates a SendSerialStrategy object. Opens and holds a serial port based
     * on the parameter and sets up serial communication.
     *
     * @param num Int value number that indicates which serial port to open.
     */
    public SendSerialStrategy(int num) {
        String portName = null;

        //Setup desired port name based on input number
        switch (num) {
            case 1:
                portName = "/dev/ttyS0";
                break;
            case 2:
                portName = "/dev/ttyS1";
                break;
            case 3:
                portName = "/dev/ttyS2";
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
     * Subroutine that sends serialized DataTable objects across the serial
     * communication channel.
     *
     * @param data DataTable object to be serialized and sent over serial
     * cables.
     */
    public void sendData(DataTable data) {
        //Serializes and sends object
        try (ObjectOutputStream out = new ObjectOutputStream(this._serialPort.getOutputStream())) {
            out.writeObject(data);
            out.close();
            DataDiodeLogger.getInstance().updateTimeLastSent();
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(DataDiodeLogger.log.SEVERE,
                    "Unable to send data on COM port " + _serialPort.getSystemPortName()
                    + ".\n" + ex.toString());
        }
    }

    /**
     * Returns the serial port held open by the SendSerialStrategy object.
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
