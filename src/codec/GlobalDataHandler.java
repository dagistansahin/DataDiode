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

import codec.DataDiodeLogger.log;
import communicators.Communicator;
import gui.MainFrame;
import gui.Properties;
import gui.dbSetup;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javolution.context.ConcurrentContext;
import javolution.util.FastTable;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import devices.ModbusDevice;
import devices.RecorderAbstract;
import javolution.lang.Parallelizable;

/**
 * This class is a singleton class that holds variables and lists that will need
 * to be accessed by multiple threads. The class also stores a couple of
 * parameters in an outside XML file for use in future program runs.
 *
 * @author Scott Arneson
 */
public class GlobalDataHandler {

    private static GlobalDataHandler _globalHandlerInstance;

    //Tables for devices
    private FastTable<RecorderAbstract> _recorders;
    private FastTable<ModbusDevice> _modbusDevices;

    //Tables for Data
    private FastTable<DataTable> _collectedDataThread1;
    private FastTable<DataTable> _collectedDataThread2;
    private FastTable<DataTable> _collectedDataThread3;

    //Serial communicators
    private Communicator _communicator1;
    private Communicator _communicator2;
    private Communicator _communicator3;

    //Database information
    private String _databaseURL;
    private String _databaseUsername;
    private String _databasePassword;

    //Various application variables
    private String _function;
    @Parallelizable
    private volatile Boolean _diodeRun;
    private Integer _dataGatherInterval;

    //Hold gui frames
    private MainFrame _mainframe;
    private dbSetup _dbsetup;
    private Properties _properties;

    //Concurrent Context for the application
    private ConcurrentContext _ctx;

    /**
     * Returns an instance of the GlobalDataHandler class. If an instance has
     * not already been created, one will be created. Only one instance of the
     * class can be made.
     *
     * @return An instance of the GlobalDataHandler.
     */
    public static GlobalDataHandler getInstance() {
        if (_globalHandlerInstance == null) {
            _globalHandlerInstance = new GlobalDataHandler();
        }
        return _globalHandlerInstance;
    }
    /*-Djavolution.context.ConcurrentContext#CONCURRENCY=10 */

    /**
     * Creates an instance of the GlobalDataHandler class. Function call is
     * private to control creation of class.
     */
    private GlobalDataHandler() {
        this._recorders = new FastTable<>();
        this._modbusDevices = new FastTable<>();
        this._collectedDataThread1 = new FastTable<>();
        this._collectedDataThread2 = new FastTable<>();
        this._collectedDataThread3 = new FastTable<>();
        this._diodeRun = false;
        this._dataGatherInterval = 1000;
        this._ctx = ConcurrentContext.enter();

        this.readSettings();
    }

    /**
     * Returns the list of Yokogawa recorders in a FastTable.
     * 
     * @return FastTable of Yokogawa recorders.
     */
    public FastTable<RecorderAbstract> getRecorders() {
        return _recorders.shared();
    }

    /**
     * Sets the list of Yokogawa recorders.
     * 
     * @param recorders FastTable of Yokogawa recorders to be set.
     */
    public void setRecorders(FastTable<RecorderAbstract> recorders) {
        this._recorders = recorders;
    }

    /**
     * Returns the list of Modbus devices, that are not Yokogawa recorders, in a
     * FastTable.
     *
     * @return FastTable of Modbus devices.
     */
    public FastTable<ModbusDevice> getModbusDevices() {
        return _modbusDevices.shared();
    }

    /**
     * Sets the list of Modbus devices, that are not Yokogawa recorders.
     * 
     * @param modbusDevices FastTable of Modbus devices to be set.
     */
    public void setModbusDevices(FastTable<ModbusDevice> modbusDevices) {
        this._modbusDevices = modbusDevices;
    }

    /**
     * Returns the FastTable that holds collected data from first thread.
     * 
     * @return FastTable of type DataTable
     */
    public synchronized FastTable<DataTable> getCollectedDataThread1() {
        return _collectedDataThread1;
    }

    /**
     * Sets the collected data from first thread to the given FastTable.
     * 
     * @param collectedDataThread1 FastTable of type DataTable to be set.
     */
    public void setCollectedDataThread1(FastTable<DataTable> collectedDataThread1) {
        this._collectedDataThread1 = collectedDataThread1;
    }

    /**
     * Returns the FastTable that holds collected data from second thread.
     * 
     * @return FastTable of type DataTable
     */
    public synchronized FastTable<DataTable> getCollectedDataThread2() {
        return _collectedDataThread2;
    }

    /**
     * Sets the collected data from second thread to the given FastTable.
     * 
     * @param collectedDataThread2 FastTable of type DataTable to be set.
     */
    public void setCollectedDataThread2(FastTable<DataTable> collectedDataThread2) {
        this._collectedDataThread2 = collectedDataThread2;
    }

    /**
     * Returns the FastTable that holds collected data from third thread.
     * 
     * @return FastTable of type DataTable
     */
    public synchronized FastTable<DataTable> getCollectedDataThread3() {
        return _collectedDataThread3;
    }

    /**
     * Sets the collected data from third thread to the given FastTable.
     * 
     * @param collectedDataThread3 FastTable of type DataTable to be set.
     */
    public void setCollectedDataThread3(FastTable<DataTable> collectedDataThread3) {
        this._collectedDataThread3 = collectedDataThread3;
    }

    /**
     * Returns the communicator used by first thread.
     * 
     * @return Communicator of first thread.
     */
    public Communicator getCommunicator1() {
        return _communicator1;
    }

    /**
     * Sets the communicator used by first thread.
     * 
     * @param communicator1 Communicator to be set.
     */
    public void setCommunicator1(Communicator communicator1) {
        this._communicator1 = communicator1;
    }

    /**
     * Returns the communicator used by second thread.
     * 
     * @return Communicator of second thread.
     */
    public Communicator getCommunicator2() {
        return _communicator2;
    }

    /**
     * Sets the communicator used by second thread.
     * 
     * @param communicator2 Communicator to be set.
     */
    public void setCommunicator2(Communicator communicator2) {
        this._communicator2 = communicator2;
    }

    /**
     * Returns the communicator used by third thread.
     * 
     * @return Communicator of third thread.
     */
    public Communicator getCommunicator3() {
        return _communicator3;
    }

    /**
     * Sets the communicator used by third thread.
     * 
     * @param communicator3 Communicator to be set.
     */
    public void setCommunicator3(Communicator communicator3) {
        this._communicator3 = communicator3;
    }

    /**
     * Returns a String containing the URL of the database.
     * 
     * @return String value of database URL.
     */
    public String getDatabaseURL() {
        return _databaseURL;
    }

    /**
     * Sets the database URL to given String value.
     * 
     * @param databaseURL String value database URL to be set.
     */
    public void setDatabaseURL(String databaseURL) {
        this._databaseURL = databaseURL;
    }

    /**
     * Returns a String containing database username.
     * 
     * @return String value of database username.
     */
    public String getDatabaseUsername() {
        return _databaseUsername;
    }

    /**
     * Sets the database username to given String value.
     * 
     * @param databaseUsername String value database username to be set.
     */
    public void setDatabaseUsername(String databaseUsername) {
        this._databaseUsername = databaseUsername;
    }

    /**
     * Returns a String containing the database password.
     * 
     * @return String value of database password.
     */
    public String getDatabasePassword() {
        return _databasePassword;
    }

    /**
     * Sets the database password to given String value.
     * 
     * @param databasePassword String value database password to be set.
     */
    public void setDatabasePassword(String databasePassword) {
        this._databasePassword = databasePassword;
    }

    /**
     * Returns a string containing function of program (either "Transmit" or
     * "Receive").
     *
     * @return String value function of program.
     */
    public String getFunction() {
        return _function;
    }

    /**
     * Sets the function of program to the given String value.
     *
     * @param function String value function to be set (either "Transmit" or
     * "Receive").
     */
    public void setFunction(String function) {
        this._function = function;
    }

    /**
     * Returns the boolean value that determines whether the data diode is
     * running or stopped.
     *
     * @return Boolean value status of data diode. True for running, false for
     * stopped.
     */
    public synchronized Boolean getDiodeRun() {
        return _diodeRun;
    }

    /**
     * Sets the status of the data diode to given boolean value, running or
     * stopped.
     *
     * @param diodeRun Boolean value status to be set. True for running, false
     * for stopped.
     */
    public void setDiodeRun(Boolean diodeRun) {
        this._diodeRun = diodeRun;

        //Starting run
        if (diodeRun) {
            if (_ctx == null) {
                _ctx = ConcurrentContext.enter();
            }
        }
    }

    /**
     * Returns the ConcurrentContext that manages the multiple threads.
     *
     * @return ConcurrentContext of program.
     */
    public ConcurrentContext getCtx() {
        return _ctx;
    }

    /**
     * Returns the data gathering interval in milliseconds as an Integer value.
     *
     * @return Integer value data gathering interval in milliseconds.
     */
    public Integer getDataGatherInterval() {
        return _dataGatherInterval;
    }

    /**
     * Sets the data gathering interval in milliseconds to given Integer value.
     *
     * @param dataGatherInterval Integer value data gathering interval to be
     * set.
     */
    public void setDataGatherInterval(Integer dataGatherInterval) {
        this._dataGatherInterval = dataGatherInterval;
    }

    /**
     * Returns the main GUI frame.
     * 
     * @return Main GUI frame.
     */
    public MainFrame getMainframe() {
        return _mainframe;
    }

    /**
     * Sets the main GUI frame.
     * 
     * @param mainframe Main GUI frame to be set.
     */
    public void setMainframe(MainFrame mainframe) {
        this._mainframe = mainframe;
    }

    /**
     * Returns the GUI frame for setting up the database information.
     * 
     * @return GUI frame for setting up database information.
     */
    public dbSetup getDbsetup() {
        return _dbsetup;
    }

    /**
     * Sets the GUI frame for setting up database information.
     * 
     * @param dbsetup GUI frame to be set.
     */
    public void setDbsetup(dbSetup dbsetup) {
        this._dbsetup = dbsetup;
    }

    /**
     * Returns the GUI frame for setting program properties.
     * 
     * @return GUI frame for setting program properties.
     */
    public Properties getProperties() {
        return _properties;
    }

    /**
     * Sets the GUI frame for setting program properties.
     * 
     * @param properties GUI frame to be set.
     */
    public void setProperties(Properties properties) {
        this._properties = properties;
    }

    /**
     * Reads XML file to preload database URL, database username, and data
     * gathering interval from last time program was run.
     */
    private void readSettings() {
        //Check if file exists and create it if it does not
        Boolean newFile = this.createSettingsXML();

        //Only read settings if file was not just created
        if (!newFile) {
            try {
                //Open and read xml document
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = dbf.newDocumentBuilder();
                Document doc = builder.parse("./Settings.xml");

                //Get settings node
                Node settings = doc.getElementsByTagName("Settings").item(0);
                NamedNodeMap attributes = settings.getAttributes();

                //Get attributes nodes
                Node gather = attributes.getNamedItem("gatherInterval");
                Node dbURL = attributes.getNamedItem("dbURL");
                Node dbUsername = attributes.getNamedItem("dbUsername");

                //Set variables to previously stored values
                this.setDataGatherInterval(Integer.parseInt(gather.getTextContent()));
                this.setDatabaseURL(dbURL.getTextContent());
                this.setDatabaseUsername(dbUsername.getTextContent());
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                        "Error reading settings file.\n" + ex.toString());
            }
        }
    }

    /**
     * Saves the database URL, database username, and data gathering interval in
     * an XML file for use during next program run.
     */
    public void saveSettings() {
        //Check if file exists and create it if it does not
        this.createSettingsXML();
        try {
            //Open and read xml document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse("./Settings.xml");

            //Get settings node
            Node settings = doc.getElementsByTagName("Settings").item(0);
            NamedNodeMap attributes = settings.getAttributes();

            //Get attributes nodes
            Node gather = attributes.getNamedItem("gatherInterval");
            Node dbURL = attributes.getNamedItem("dbURL");
            Node dbUsername = attributes.getNamedItem("dbUsername");

            //Rewrite attribute nodes
            gather.setTextContent(this._dataGatherInterval.toString());
            dbURL.setTextContent(this._databaseURL);
            dbUsername.setTextContent(this._databaseUsername);

            //Write the xml file
            this.writeXML(doc, new File("./Settings.xml"));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                    "Error saving settings to file.\n" + ex.toString());
        }
    }

    /**
     * Creates an XML file to store program settings into. Returns true if a
     * file was created and false if the file already exists.
     *
     * @return Boolean value. True if file was created. False if file already
     * exists.
     */
    private Boolean createSettingsXML() {
        Boolean createdFile = false;
        File file = new File("./Settings.xml");
        if (!file.exists()) {
            try {
                //Setup for creating document
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = dbf.newDocumentBuilder();
                Document doc = builder.newDocument();

                //Create root element
                Element root = doc.createElement("Settings");
                doc.appendChild(root);

                //Add comment to document
                Comment comment = doc.createComment("This file holds the settings previously used in the program.");
                doc.insertBefore(comment, root);

                //Create attributes to hold data
                root.setAttribute("gatherInterval", this._dataGatherInterval.toString());
                root.setAttribute("dbURL", "");
                root.setAttribute("dbUsername", "");

                //Write data to file
                this.writeXML(doc, file);
                createdFile = true;
            } catch (ParserConfigurationException ex) {
                DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                        "Error creating settings file.\n" + ex.toString());
            }
        }
        return createdFile;
    }

    /**
     * Writes the XML file to given file with the given document file.
     * 
     * @param doc Document file to write.
     * @param file File to be written.
     */
    private void writeXML(Document doc, File file) {
        try {
            //Write data to file
            TransformerFactory transfact = TransformerFactory.newInstance();
            Transformer transformer = transfact.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                    "Error writing to settings file.\n" + ex.toString());
        } catch (TransformerException ex) {
            DataDiodeLogger.getInstance().addLogs(log.NORMAL,
                    "Error writing to settings file.\n" + ex.toString());
        }
    }
}
