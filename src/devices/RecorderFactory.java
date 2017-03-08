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
package devices;

/**
 * This class is sent the lines from the main configuration file that contain
 * the list of recorders. The factory then produces the various specific
 * recorder classes and sets the Model, configuration file location, IP address,
 * Unit ID number, and priority number for each recorder. Each recorder is added
 * to a list of recorders maintained by the GlobalDataHandler. This class is a
 * singleton class and only one instance of the class is able to be created
 * during a program run.
 *
 * @author Scott Arneson
 */
public class RecorderFactory {

    private static RecorderFactory _recorderFactory;

    /**
     * Creates a RecorderFactory object.
     */
    private RecorderFactory() {
    }

    /**
     * Returns an instance of the RecorderFactory object. If a RecorderFactory
     * object has not yet been created, one will be created. Only one
     * RecorderFactory object can be created during a program run.
     *
     * @return An instance of the RecorderFactory object.
     */
    public static RecorderFactory getInstance() {
        if (_recorderFactory == null) {
            _recorderFactory = new RecorderFactory();
        }
        return _recorderFactory;
    }

    /**
     * Reads the line of text sent to it and produces the desired Yokogawa
     * Recorder object.
     *
     * @param line line from recorder configuration file that has recorder
     * information.
     * Format: Model,Config File,IP address,Unit ID, Priority
     * Example: YokogawaGX20,recoderConfig.pdl,192.168.30.1,1,3
     *
     * @return A Yokogawa recorder object specified by the input line of text.
     */
    public RecorderAbstract getRecorder(String line) {
        //Split the line into its individual components
        String[] recorder = line.split(",");
        for (String string : recorder) {
            string = string.trim();
        }

        //Determine the type of recorder needed
        if (recorder[0].equalsIgnoreCase("YokogawaGX20") || recorder[0].equalsIgnoreCase("GX20")) {
            return new YokogawaGX20("YokogawaGX20", recorder[1], recorder[2], recorder[3], recorder[4]);
        } else if (recorder[0].equalsIgnoreCase("YokogawaDX200") || recorder[0].equalsIgnoreCase("DX200")) {
            return new YokogawaDX200("YokogawaDX200", recorder[1], recorder[2], recorder[3], recorder[4]);
        } else if (recorder[0].equalsIgnoreCase("YokogawaDX1000") || recorder[0].equalsIgnoreCase("DX1000")) {
            return new YokogawaDX1000("YokogawaDX1000", recorder[1], recorder[2], recorder[3], recorder[4]);
        } else {
            return null;
        }
    }
}
