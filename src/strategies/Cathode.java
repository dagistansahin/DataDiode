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
import codec.GlobalDataHandler;
import communicators.CommunicatorFactory;
import communicators.CommunicatorType;
import java.io.IOException;
import javolution.context.ConcurrentContext;

/**
 * This class sets up the cathode (receiver) of the data diode.
 * 
 * @author Scott Arneson
 */
public class Cathode implements Runnable {

    private CathodeSender _sender;
    private CathodeReceiver _receiver1;
    private CathodeReceiver _receiver2;
    private CathodeReceiver _receiver3;

    /**
     * Creates a Cathod object. Creates four communicators, three serial
     * receivers and one ethernet sender.
     */
    public Cathode() {
        try {
            this._sender = new CathodeSender(CommunicatorFactory.getInstance().getCommunicator(
                    CommunicatorType.SEND, CommunicatorType.ETHERNET, 0));
            this._receiver1 = new CathodeReceiver(CommunicatorFactory.getInstance().getCommunicator(
                    CommunicatorType.RECEIVE, CommunicatorType.SERIAL, 1));
            this._receiver2 = new CathodeReceiver(CommunicatorFactory.getInstance().getCommunicator(
                    CommunicatorType.RECEIVE, CommunicatorType.SERIAL, 2));
            this._receiver3 = new CathodeReceiver(CommunicatorFactory.getInstance().getCommunicator(
                    CommunicatorType.RECEIVE, CommunicatorType.SERIAL, 3));
            GlobalDataHandler.getInstance().setCommunicator1(_receiver1.getReceiver());
            GlobalDataHandler.getInstance().setCommunicator2(_receiver2.getReceiver());
            GlobalDataHandler.getInstance().setCommunicator3(_receiver3.getReceiver());
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Unable to create all communicators in cathode.\n" + ex.toString());
        }
    }

    /**
     * Starts the cathode program. Creates four threads: three to receive
     * information from the anode over the serial cables, and one to send data
     * to the database.
     */
    @Override
    public void run() {
        ConcurrentContext ctx = GlobalDataHandler.getInstance().getCtx();

        ctx.execute(_sender);
        ctx.execute(_receiver1);
        ctx.execute(_receiver2);
        ctx.execute(_receiver3);
    }
}
