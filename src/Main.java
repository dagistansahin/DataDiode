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
import codec.DataDiodeLogger;
import codec.DataDiodeLogger.log;
import codec.GlobalDataHandler;
import devices.ModbusConfigFileParser;
import gui.MainFrame;
import gui.Properties;
import gui.dbSetup;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javolution.util.FastTable;
import devices.RecorderAbstract;
import devices.RecorderFactory;

/**
 * This is the main class that the program enters. It opens and parses through
 * the main configuration file, setting up the various recorders and Modbus
 * devices that are to be communicated with. It also sets the basic function,
 * either receive or transmit, that the program will accomplish. Finally it sets
 * up and begins the GUI.
 *
 * @author Scott Arneson
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //Setup variables to hold data
        String function = null;
        RecorderAbstract recorder;
        FastTable<RecorderAbstract> recorders = new FastTable<>();
        List<String> lines = null;

        //Set config file path
        Path filePath = Paths.get("/home/engineer/Documents/config.txt");

        //Read config file
        try {
            lines = Files.readAllLines(filePath, Charset.forName("ISO-8859-1"));
        } catch (IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Unable to read configuration file." + ex.toString());
        }

        //Parse config file
        if (lines != null) {
            int i = 0;
            for (String line : lines) {
                if (line.startsWith("**") || line.isEmpty()) {
                } else if (line.startsWith("Function:")) {
                    function = line.substring(line.lastIndexOf(":") + 1).trim();
                } else if (line.startsWith("Modbus")) {
                    String filename = line.substring(line.indexOf(",") + 1).trim();
                    ModbusConfigFileParser.getInstance().parseFile(filename);
                } else {
                    recorder = RecorderFactory.getInstance().getRecorder(line);
                    if (recorder != null) {
                        recorder.setRecorderID(i);
                        recorders.addLast(recorder);
                        i++;
                    }
                }
            }

            //Store list of recorders and function
            GlobalDataHandler.getInstance().setRecorders(recorders);
            GlobalDataHandler.getInstance().setFunction(function);
        }

        //Start GUI
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            DataDiodeLogger.getInstance().addLogs(log.SEVERE,
                    "Error setting up GUI.\n" + ex.toString());
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });

        if ((function != null) && ((function.equalsIgnoreCase("Receive"))
                || function.equalsIgnoreCase("Recieve"))) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new dbSetup().setVisible(true);
                }
            });
        }

        if ((function != null) && (function.equalsIgnoreCase("Transmit"))) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new Properties().setVisible(false);
                }
            });
        }
    }
}
