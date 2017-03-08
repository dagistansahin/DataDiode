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
import communicators.Communicator;
import communicators.CommunicatorFactory;
import communicators.CommunicatorType;
import java.io.IOException;
import javolution.context.ConcurrentContext;

/**
 * Class sets up and runs the Anode (Transmitter) of the DataDiode.
 *
 * @author Scott Arneson
 */
public final class Anode implements Runnable {

    private Communicator _sender1;
    private Communicator _sender2;
    private Communicator _sender3;

    /**
     * Creates an Anode object. Creates three serial communicators to send data
     * to the cathode side of the data diode.
     */
    public Anode() {
        try {
            this._sender1 = CommunicatorFactory.getInstance().getCommunicator(CommunicatorType.SEND, CommunicatorType.SERIAL, 1);
            this._sender2 = CommunicatorFactory.getInstance().getCommunicator(CommunicatorType.SEND, CommunicatorType.SERIAL, 2);
            this._sender3 = CommunicatorFactory.getInstance().getCommunicator(CommunicatorType.SEND, CommunicatorType.SERIAL, 3);
            GlobalDataHandler.getInstance().setCommunicator1(_sender1);
            GlobalDataHandler.getInstance().setCommunicator2(_sender2);
            GlobalDataHandler.getInstance().setCommunicator3(_sender3);
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Unable to create serial communications.\n" + ex.toString());
        }
    }

    /**
     * Starts the anode program. Creates four new threads: one thread gets data
     * from each of the recorders on the list of recorders and list of Modbus
     * devices, and three threads serialize the data from the recorder and send
     * the data across the serial cables to the cathode side of the data diode.
     */
    @Override
    public void run() {
        ConcurrentContext ctx = GlobalDataHandler.getInstance().getCtx();

        //Create and execute threads for the receiver and senders
        ctx.execute(new AnodeReceiver(GlobalDataHandler.getInstance().getRecorders(),
                GlobalDataHandler.getInstance().getModbusDevices()));
        ctx.execute(new AnodeSender(_sender1, GlobalDataHandler.getInstance().getCollectedDataThread1()));
        ctx.execute(new AnodeSender(_sender2, GlobalDataHandler.getInstance().getCollectedDataThread2()));
        ctx.execute(new AnodeSender(_sender3, GlobalDataHandler.getInstance().getCollectedDataThread3()));
    }
}
