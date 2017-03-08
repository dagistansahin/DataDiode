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
import communicators.Communicator;
import communicators.SendEthernetStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles the sending of data to the database. It will call on the
 * communicator assigned to it to continually check the lists of data for
 * available data. If the communicator finds available data, it calls on the
 * recorder or Modbus device owner of the data to send the data to the database.
 *
 * @author Scott Arneson
 */
public class CathodeSender implements Runnable {

    private final SendEthernetStrategy _sender;
    private int _count;

    /**
     * Creates a Cathode Sender object.
     *
     * @param sender Communicator assigned to the object. Must be a
     * SendEthernetStrategy implementation of the Communicator interface.
     */
    public CathodeSender(Communicator sender) {
        this._sender = (SendEthernetStrategy) sender;
        this._count = 0;
    }

    /**
     * Continuously calls on the communicator to check for available data. If
     * data is available the communicator then handles it. Also calls the
     * garbage collector at regular intervals.
     */
    @Override
    public void run() {
        while (GlobalDataHandler.getInstance().getDiodeRun()) {
            _sender.sendData();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(SendEthernetStrategy.class.getName()).log(Level.SEVERE, null, ex);
            }
            this._count++;
            if (this._count == 60) {
                Thread gc = new Thread() {
                    @Override
                    public void run() {
                        System.gc();
                    }
                };
                gc.start();
                this._count = 0;
            }
        }
    }

    /**
     * Returns the communicator assigned to the CathodeSender.
     * 
     * @return SendEthernetStrategy object.
     */
    public SendEthernetStrategy getSender() {
        return _sender;
    }
}
