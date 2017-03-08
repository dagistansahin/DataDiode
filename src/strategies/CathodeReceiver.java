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
package strategies;

import codec.GlobalDataHandler;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import communicators.Communicator;
import communicators.ReceiveSerialStrategy;

/**
 * This class handles the receiving of data from the anode. It waits for a data
 * available event on the serial port assigned to it, then has the communicator
 * assigned to it handle the receiving of the information.
 *
 * @author Scott Arneson
 */
public class CathodeReceiver implements SerialPortDataListener, Runnable {

    private final ReceiveSerialStrategy _receiver;

    /**
     * Creates a CathodeReceiver object.
     *
     * @param receiver Communicator that the object will use. Must be a
     * ReceiveSerialStrategy implementation of the Communicator interface.
     */
    public CathodeReceiver(Communicator receiver) {
        this._receiver = (ReceiveSerialStrategy) receiver;
        this._receiver.getSerialPort().addDataListener(this);
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPort.LISTENING_EVENT_DATA_AVAILABLE:
                this._receiver.getData();

            default:
                break;
        }
    }

    @Override
    public void run() {
        Boolean running = GlobalDataHandler.getInstance().getDiodeRun();
        while (running) {
            running = GlobalDataHandler.getInstance().getDiodeRun();
        }
    }

    /**
     * Returns the communicator assigned to the CathodeReceiver.
     * 
     * @return ReceiveSerialStrategy object.
     */
    public ReceiveSerialStrategy getReceiver() {
        return _receiver;
    }
}
