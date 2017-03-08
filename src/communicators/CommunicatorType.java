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

/**
 * Enumeration to make coding easier to read.
 *  public static final int SEND = 0
 *  public static final int RECEIVE = 1
 *  public static final int ETHERNET = 0
 *  public static final int SERIAL = 1;
 * 
 * @author Scott Arneson
 */
public enum CommunicatorType {

    SEND(0),
    RECEIVE(1),
    ETHERNET(0),
    SERIAL(1);
    private final int value;

    private CommunicatorType(int value) {
        this.value = value;
    }
}
