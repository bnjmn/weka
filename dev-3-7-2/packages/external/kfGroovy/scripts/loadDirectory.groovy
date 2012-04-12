import java.beans.*
import java.io.Serializable
import java.util.Vector
import java.util.Enumeration
import javax.swing.JFileChooser
import org.pentaho.dm.kf.KFGroovyScript
import org.pentaho.dm.kf.GroovyHelper
import weka.core.*
import weka.gui.Logger
import weka.gui.beans.*
// add further imports here if necessary

/**
 * Groovy script that loads all arff files in a directory
 * supplied by the user.
 *
 * @author Mark Hall
 */
class DirectoryLoader
	implements
		KFGroovyScript,
		EnvironmentHandler,
		BeanCommon,
		EventConstraints,
		UserRequestAcceptor,
		TrainingSetListener,
		TestSetListener,
		DataSourceListener,
		InstanceListener,
		TextListener,
		BatchClassifierListener,
		IncrementalClassifierListener,
		BatchClustererListener,
		GraphListener,
                Startable,
		Serializable {

  /** Don't delete!! */
  GroovyHelper m_helper

  String m_filePath = "\${DATA_SETS}"

  JFileChooser m_fileChooser = 
    new JFileChooser(new File(System.getProperty("user.dir")))

  boolean m_stop = false

  Environment m_env = Environment.getSystemWide()

  Logger m_log

  /** Don't delete!! */
  void setManager(GroovyHelper manager) { m_helper = manager }

  /** Alter or add to in order to tell the KnowlegeFlow
   *  environment whether a certain incoming connection type is allowed
   */
  boolean connectionAllowed(String eventName) {
    return false
  }

  /** Alter or add to in order to tell the KnowlegeFlow
   *  environment whether a certain incoming connection type is allowed
   */
  boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName())
  }

  /** Add (optional) code to do something when you have been
   *  registered as a listener with a source for the named event
   */
  void connectionNotification(String eventName, Object source) { }

  /** Add (optional) code to do something when you have been
   *  deregistered as a listener with a source for the named event
   */
  void disconnectionNotification(String eventName, Object source) { }

  /** Custom name of this component. Do something with it if you
   *  like. GroovyHelper already stores it and alters the icon text
   *  for you	 */
  void setCustomName(String name) { }

  /** Custom name of this component. No need to return anything
   *  GroovyHelper already stores it and alters the icon text
   *  for you	 */
  String getCustomName() { return null }

  /** Add code to return true when you are busy doing something
   */
  boolean isBusy() { return false }

  /** Store and use this logging object in order to post messages
   *  to the log
   */
  void setLog(Logger logger) {
    m_log = logger
  }

  /** Store and use this Environment object in order to lookup and
   *  use the values of environment variables
   */
  void setEnvironment(Environment env) { 
    m_env = env
  }

  /** Stop any processing (if possible)
   */
  void stop() { 
    m_stop = true
  }

  /** Alter or add to in order to tell the KnowlegeFlow
   *  whether, at the current time, the named event could
   *  be generated.
   */
  boolean eventGeneratable(String eventName) {
    if (eventName.equals("dataSet")) { return true }
    return false
  }

  String getStartMessage() {
    String startMessage = "Start loading"

    String fp = m_filePath
    try {
      fp = m_env.substitute(m_filePath)
    } catch (Exception ex) { }

    File directory = new File(fp)

    if (!directory.exists()) {
      startMessage = "\$" + startMessage 
    }

    return startMessage
  }

  /**
   * Where the action begins.
   */
  void start() {
    m_stop = false
    String fp = m_filePath

    try {
      fp = m_env.substitute(m_filePath)
    } catch (Exception ex) { }

    File directory = new File(fp)

    if (directory.exists()) {
      File[] contents = directory.listFiles()
      for (int i = 0; i < contents.length; i++) {
        if (contents[i].toString().endsWith(".arff")) {
          if (m_log != null) {
            m_log.statusMessage("DirectoryLoader\$" + hashCode() + "|Loading " + contents[i])
            m_log.logMessage("[DirectoryLoader] Loading " + contents[i])
          }
          Instances tempI = new Instances(new BufferedReader(new FileReader(contents[i])))

          m_helper.notifyDataSourceListeners(new DataSetEvent(this, tempI))
         }
         if (m_stop) {
           System.err.println("Groovy component received stop notification!!")
           break
         }
      }
      if (m_log != null) {
        m_log.statusMessage("DirectoryLoader\$" + hashCode() + "|Finished.") 
      }
    }
  }

  /** Implement this to tell KnowledgeFlow about any methods
   *  that the user could invoke (i.e. to show a popup visualization
   *  or something).
   */
  Enumeration enumerateRequests() {
    Vector newVec = new Vector()

    String entry = "Set directory..."
    newVec.addElement(entry)

    return newVec.elements()
  }

  /** Make the user-requested action happen here.
   */
  void performRequest(String requestName) {
    if (requestName.equals("Set directory...")) {
      m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int retVal = m_fileChooser.showOpenDialog()
        if (retVal == JFileChooser.APPROVE_OPTION) {
          File selectedFile = m_fileChooser.getSelectedFile()
          if (selectedFile.isDirectory()) {
            m_filePath = selectedFile.getAbsolutePath()
          } 
        }       
    } 
  }

  //--------------- Incoming events ------------------
  //--------------- Implement as necessary -----------

  void acceptTrainingSet(TrainingSetEvent e) { }

  void acceptTestSet(TestSetEvent e) { }

  void acceptDataSet(DataSetEvent e) { }

  void acceptInstance(InstanceEvent e) { }

  void acceptText(TextEvent e) { }

  void acceptClassifier(BatchClassifierEvent e) { }

  void acceptClassifier(IncrementalClassifierEvent e) { }

  void acceptClusterer(BatchClustererEvent e) { }

  void acceptGraph(GraphEvent e) { }

}