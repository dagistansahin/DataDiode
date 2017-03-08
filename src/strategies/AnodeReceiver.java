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
import devices.ModbusDevice;
import java.sql.Timestamp;
import java.util.Date;
import javolution.util.FastTable;
import devices.RecorderAbstract;

/**
 * This class handles the gathering of data from the recorders and Modbus
 * devices. It will gather data, then ensure that the amount of time before the
 * next data gather is equal to or greater than the data gathering interval
 * parameter set by the user.
 *
 * @author Scott Arneson
 */
public class AnodeReceiver implements Runnable {

    private final FastTable<RecorderAbstract> _recorders;
    private final FastTable<ModbusDevice> _devices;
    private Timestamp _dataLastCollected;
    private int _count;

    /**
     * Creates an AnodeReceiver object. Fills in the necessary information such
     * as the list of recorders and Modbus devices that will be communicated
     * with.
     *
     * @param recorders FastTable of recorders that will be communicated with.
     * @param devices FastTable of Modbus devices that will be communicated
     * with.
     */
    public AnodeReceiver(FastTable<RecorderAbstract> recorders, FastTable<ModbusDevice> devices) {
        this._recorders = recorders;
        this._devices = devices;
        Date date = new Date();
        this._dataLastCollected = new Timestamp(date.getTime());
        this._count = 0;
    }

    /**
     * Gathers data from the each of the recorders and Modbus devices. After
     * each gather, it waits until time has passed that is at least as long as
     * the data gather interval set by the user. Also calls the garbage
     * collector at regular intervals.
     */
    @Override
    public void run() {
        while (GlobalDataHandler.getInstance().getDiodeRun()) {
            Date date = new Date();
            Timestamp time = new Timestamp(date.getTime());
            if ((time.getTime() - this._dataLastCollected.getTime())
                    >= GlobalDataHandler.getInstance().getDataGatherInterval()) {
                this._dataLastCollected = time;
                for (RecorderAbstract recorder : _recorders) {
                    recorder.getData();
                }
                for (ModbusDevice device : _devices) {
                    device.getData();
                }
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
        for (RecorderAbstract recorder : _recorders) {
            recorder.closeConnection();
        }
        for (ModbusDevice device : _devices) {
            device.closeConnection();
        }
    }
}
