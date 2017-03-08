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

import java.io.IOException;

/**
 * This class creates and returns different types of communicators based given
 * inputs. This class is a singleton class in which only one instance can be
 * created during the program run.
 *
 * @author Scott Arneson
 */
public class CommunicatorFactory {

    private static CommunicatorFactory _communicatorFactory;

    /**
     * Private class that creates a CommunicatorFactor object.
     */
    private CommunicatorFactory() {
    }

    /**
     * Returns an instance of the CommunicatorFactor class. If an instance has
     * not already been created, one will be created and returned. Only one
     * instance can be created in a program run.
     *
     * @return An instance of the CommunicatorFactory class.
     */
    public static CommunicatorFactory getInstance() {
        if (_communicatorFactory == null) {
            _communicatorFactory = new CommunicatorFactory();
        }
        return _communicatorFactory;
    }

    /**
     * Returns desired communicator based in inputs.
     *
     * @param direction Indicates direction, either SEND or RECEIVE.
     * @param method Indicates method of communication, either ETHERNET or
     * SERIAL.
     * @param num Indicates communication channel for serial communication,
     * either 1, 2, or 3. Has no importance for Ethernet communication.
     * @return Desired communicator.
     * @throws IOException Exception thrown in the event that setting up
     * communication encounters a problem.
     */
    public Communicator getCommunicator(CommunicatorType direction, CommunicatorType method, int num) throws IOException {
        switch (direction) {
            case SEND:
                switch (method) {
                    case ETHERNET:
                        return SendEthernetStrategy.getInstance();
                    case SERIAL:
                        return new SendSerialStrategy(num);
                    default:
                        return null;
                }
            case RECEIVE:
                switch (method) {
                    case ETHERNET:
                        return ReceiveEthernetStrategy.getInstance();
                    case SERIAL:
                        return new ReceiveSerialStrategy(num);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }
}
