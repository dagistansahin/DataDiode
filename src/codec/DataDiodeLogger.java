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
package codec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is designed to handle the logging of all important events that
 * happen during the program run. It has two logging levels, normal and severe.
 * Normal logging levels are displayed only on the GUI. Severe logging levels
 * are also placed on the GUI but are also saved in a text file. This class is a
 * singleton class.
 *
 * @author Scott Arneson
 */
public class DataDiodeLogger {

    private static DataDiodeLogger _myDataLogger;
    private javax.swing.JTextArea _jTextAreaConsole;
    private javax.swing.JLabel _jLabelTimeLastSent;
    private StringBuilder _logsString;

    /**
     * Returns the instance of this singleton class. If the class has not yet
     * been instantiated, it instantiates it.
     *
     * @return The instance of the DataDiodeLogger class.
     */
    public static DataDiodeLogger getInstance() {
        if (_myDataLogger == null) {
            _myDataLogger = new DataDiodeLogger();
        }
        return _myDataLogger;
    }

    /**
     * Creates a new DataDiodeLogger object. Can only be called within the
     * class.
     */
    private DataDiodeLogger() {
        _logsString = new StringBuilder();
    }

    /**
     * Enumeration for the logging levels to make programming more easily
     * readable. Logging levels are NORMAL and SEVERE.
     */
    public enum log {
        NORMAL(0),
        SEVERE(1);
        private final int value;

        private log(int value) {
            this.value = value;
        }
    }

    /**
     * Sets up the link between the logger and the GUI text area allowing the
     * logger to output to this text area.
     *
     * @param jTextAreaConsole GUI JTextArea for logger to link to.
     */
    public void setupTextArea(javax.swing.JTextArea jTextAreaConsole) {
        this._jTextAreaConsole = jTextAreaConsole;
    }

    /**
     * Sets up jLabel for indicating the time when data was last sent. This
     * provides another method to determine if the program is running properly.
     *
     * @param jLabel A javax.swing.JLabel object to be set.
     */
    public void setupTimeLastSent(javax.swing.JLabel jLabel) {
        this._jLabelTimeLastSent = jLabel;
    }

    /**
     * Adds log to the linked GUI text area and log text file depending on log
     * level.
     *
     * @param level Indicates the type of the log. NORMAL adds logText to linked
     * GUI text area only. SEVERE adds logText to both the linked GUI text area
     * as well as a log text file.
     * @param logText The string value to add to the logs.
     */
    public void addLogs(log level, String logText) {
        Date date = new Date();
        _logsString.append(date.toString() + ": " + logText + "\n");
        if (this._jTextAreaConsole != null) {
            this._jTextAreaConsole.setText(_logsString.toString());
        }
        if (level == log.SEVERE) {
            this.writeLogFile(logText, date);
        }
    }

    /**
     * Returns all logs currently logged on the GUI during the run.
     *
     * @return A String that contains all logs logged on the GUI during the run.
     */
    public String getLogs() {
        return _logsString.toString();
    }

    /**
     * Writes logs to log text file. If no text file exists, one will be
     * created.
     *
     * @param log String to add to log text file.
     * @param date Date value of log to include in log entry.
     */
    private void writeLogFile(String log, Date date) {
        String logOutput = date.toString() + ": " + log + "\n\n";
        File file = new File("./logfile.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            BufferedWriter bwriter = new BufferedWriter(writer);
            bwriter.write(logOutput);
            bwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(DataDiodeLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Updates the GUI "Time Last Data Sent" value.
     */
    public void updateTimeLastSent() {
        Date date = new Date();
        this._jLabelTimeLastSent.setText(date.toString());
    }
}
