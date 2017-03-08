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

import codec.DataDiodeLogger;
import codec.DataDiodeLogger.log;
import codec.DataTable;
import codec.GlobalDataHandler;
import communicators.Communicator;
import communicators.SendSerialStrategy;
import javolution.util.FastTable;

/**
 * This class handles the sending of the data across the serial cables. It waits
 * until there is data available on the the list that it handles, then sends the
 * data using the communicator assigned to it.
 *
 * @author Scott Arneson
 */
public class AnodeSender implements Runnable {

    private final SendSerialStrategy _sender;
    private final FastTable<DataTable> _collectedData;

    /**
     * Creates an AnodeSender object.
     *
     * @param sender Communicator that will be used to send serial
     * communication. Must be a SendSerialStrategy implementation of the
     * Communicator interface.
     * @param collectedData FastTable of DataTable objects that the AnodeSender
     * object will be checking for data.
     */
    public AnodeSender(Communicator sender, FastTable<DataTable> collectedData) {
        this._sender = (SendSerialStrategy) sender;
        this._collectedData = collectedData;
    }

    /**
     * Continuously checks the list of data that is assigned to it. Once there
     * is data available, it takes the data and sends it to the communicator
     * assigned to it to send the data across the serial cables to the cathode
     * side of the data diode.
     */
    @Override
    public void run() {
        while (GlobalDataHandler.getInstance().getDiodeRun()) {
            if (!_collectedData.isEmpty()) {
                try {
                    _sender.sendData(_collectedData.removeFirst());
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                            "Error while calling thread sleep.\n" + ex.toString());
                }
            }
        }
    }

    /**
     * Returns the SendSerialStrategy communicator assigned to the AnodeSender.
     * 
     * @return SendSerialStrategy object.
     */
    public SendSerialStrategy getSender() {
        return _sender;
    }
}
