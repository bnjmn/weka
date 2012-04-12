import java.beans.*
import java.io.Serializable
import java.util.Vector
import java.util.Enumeration
import org.pentaho.dm.kf.KFGroovyScript
import org.pentaho.dm.kf.GroovyHelper
import weka.core.*
import weka.gui.Logger
import weka.gui.beans.*
import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.functions.Logistic
import weka.classifiers.Evaluation
import weka.classifiers.Classifier
import weka.classifiers.AbstractClassifier

import groovy.swing.SwingBuilder
import javax.swing.*
import java.awt.*


// add further imports here if necessary

/**
 * Example Groovy script that generates a learning curve for a classifier.
 * Allows the classifier to be connected via a "configuration" event or
 * specified via an environment variable (CLASSIFIER_NAME). Classifier options
 * and the parameters of the learning curve could be specified via environment
 * variables as well through just minor changes to the script.
 *
 * Generates both a "TextEvent" containing the curve information and a
 * "DataSetEvent". The latter can be visualized in a DataVisualizer component.
 *
 * Also demonstrates how to allow the user to set options for the script
 * via a graphical pop-up window.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 */
class LearningCurve
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
		ChartListener,
		ThresholdDataListener,
		VisualizableErrorListener,
                ConfigurationListener,
		Serializable {

  /** Don't delete!!
   *  GroovyHelper has the following useful methods:
   *
   *  notifyListenerType(Object event) - GroovyHelper will pass on event
   *    appropriate listener type for you
   *  ArrayList<TrainingSetListener> getTrainingSetListeners() - get
   *    a list of any directly connected components that are listening
   *    for TrainingSetEvents from us
   *  ArrayList<TestSetListener> getTestSetListeners()
   *  ArrayList<InstanceListener> getInstanceListeners()
   *  ArrayList<TextListener> getTextListeners()
   *  ArrayList<DataSourceListener> getDataSourceListeners()
   *  ArrayList<BatchClassifierListener> getBatchClassifierListeners()
   *  ArrayList<IncrementalClassifierListener> getIncrementalClassifierListeners()
   *  ArrayList<BatchClustererListener> getBatchClustererListeners()
   *  ArrayList<GraphListenerListener> getGraphListeners()
   *  ArrayList<ChartListener> getChartListeners()
   *  ArrayList<ThresholdDataListener> getThresholdDataListeners()
   *  ArrayList<VisualizableErrorListener> getVisualizableErrorListeners()
   */
  GroovyHelper m_helper

  Logger m_log = null

  Environment m_env = Environment.getSystemWide()

  String m_holdoutSize = "33.0"
  String m_stepSize = "100"
  String m_numSteps = "10"
  String m_classifierName = "\${CLASSIFIER_NAME}"
  String m_classifierOptions = null

  Object m_incomingConnection = null

  weka.gui.beans.Classifier m_connectedConfigurable = null

  /** Don't delete!! */
  void setManager(GroovyHelper manager) { m_helper = manager }

  /** Alter or add to in order to tell the KnowlegeFlow
   *  environment whether a certain incoming connection type is allowed
   */
  boolean connectionAllowed(String eventName) {
    if (eventName.equals("trainingSet") && 
        m_incomingConnection == null) { return true }

    if (eventName.equals("configuration") &&
        m_connectedConfigurable == null) { return true}

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
  void connectionNotification(String eventName, Object source) {
    if (eventName.equals("trainingSet")) {
      m_incomingConnection = source
    }

    if (eventName.equals("configuration")) {
      // check the type of the configurable
      if (source instanceof weka.gui.beans.Classifier) {
        m_connectedConfigurable = (weka.gui.beans.Classifier)source
      } else {
        if (m_log != null) {
          m_log.statusMessage("LearningCurve\$"+hashCode()+"|ERROR (see log for details)")
            m_log.logMessage("[LearningCurve] Connected configurable is not a classifier!!")
        }
      } 
    }
  }

  /** Add (optional) code to do something when you have been
   *  deregistered as a listener with a source for the named event
   */
  void disconnectionNotification(String eventName, Object source) { 
    if (eventName.equals("trainingSet")) {
      m_incomingConnection = null
    }

    if (eventName.equals("configuration")) {
      m_connectedConfigurable = null 
    }
  }

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
  void stop() { }

  /** Alter or add to in order to tell the KnowlegeFlow
   *  whether, at the current time, the named event could
   *  be generated.
   */
  boolean eventGeneratable(String eventName) {
    if (eventName.equals("text")) { return true }
    if (eventName.equals("dataSet")) { return true }
    return false
  }

  /** Implement this to tell KnowledgeFlow about any methods
   *  that the user could invoke (i.e. to show a popup visualization
   *  or something).
   */
  Enumeration enumerateRequests() {
    Vector items = new Vector(0)
      items.add("Set options...")
      return items.elements()
  }

  /** Make the user-requested action happen here.
   */
  void performRequest(String requestName) {
    if (requestName.equals("Set options...")) {
      def swing = new SwingBuilder()
        def holderP1 = {
        swing.panel() {
          borderLayout()
          label (text:'Holdout set size: ', constraints:BorderLayout.WEST)
          hSize = textField(text:m_holdoutSize, columns:6, 
                            actionPerformed: {
                              m_holdoutSize = hSize.text 
                            }, constraints:BorderLayout.CENTER)
        }
      }

      def holderP2 = {
        swing.panel() {
          borderLayout()
          label (text:'Number of steps: ', constraints:BorderLayout.WEST)
          nSteps = textField(text:m_numSteps, columns:6,
                             actionPerformed: {
                               m_numSteps = nSteps.text 
                             }, constraints:BorderLayout.CENTER)
        } 
      }

      def holderP3 = {
        swing.panel() {
          borderLayout()
          label (text:'Step size: ', constraints:BorderLayout.WEST)
          sSize = textField(text:m_stepSize, columns:6,
                            actionPerformed: {
                              m_stepSize = sSize.text 
                            }, constraints:BorderLayout.CENTER)
        } 
      }

      def holderP4 = {
        swing.panel() {
          boxLayout(axis:BoxLayout.Y_AXIS)
          widget(holderP1())
          widget(holderP2())
          widget(holderP3()) 
        } 
      }

      def holderP5 = {
        swing.panel() {
          boxLayout(axis:BoxLayout.X_AXIS)
          button(text:'OK',
                 actionPerformed: {
                   m_holdoutSize = hSize.text
                   m_numSteps = nSteps.text
                   m_stepSize = sSize.text
                   dispose()
                 })
          button(text:"CANCEL",
                 actionPerformed: {
                   dispose() 
                 })
        } 
      }
      def frame = swing.frame(title:'Learning Curve Options', size:[300,600]) {
        borderLayout()
        widget(holderP4(), constraints:BorderLayout.NORTH)
        widget(holderP5(), constraints:BorderLayout.SOUTH)
      }
      frame.pack()
        frame.show()
      
    }
  }

  //--------------- Incoming events ------------------
  //--------------- Implement as necessary -----------

  void acceptTrainingSet(TrainingSetEvent e) {
    if (e.isStructureOnly()) {
      return 
    }

    StringBuffer buff = new StringBuffer()
    Instances insts = new Instances(e.getTrainingSet())
    insts.randomize(new Random(1))

    String hSize = m_holdoutSize
    String sSize = m_stepSize
    String nSteps = m_numSteps
    String classifierName = m_classifierName
    String classifierOptions = m_classifierOptions
    String[] splitOptions = null

    if (m_env != null) {
      try {
        hSize = m_env.substitute(hSize)
        sSize = m_env.substitute(sSize)
        nSteps = m_env.substitute(nSteps)
        if (classifierName != null && classifierName.length() > 0) {
          classifierName = m_env.substitute(classifierName) 
        }
        if (classifierOptions != null && classifierOptions.length() > 0) {
          classifierOptions = m_env.substitute(classifierOptions) 
        }
      } catch (Exception ex) { 
      } 
    }

    weka.classifiers.Classifier classifierToUse = null
    if (m_connectedConfigurable == null) {
      // try and instantiate from the supplied classifier name
      if (classifierName == null || classifierName.length() == 0) {
        if (m_log != null) {
          m_log.statusMessage("LearningCurve\$"+hashCode()+"|ERROR (see log for details)")
          m_log.logMessage("[LearningCurve] No classifier supplied!")
        }
        return 
      }
      if (classifierOptions != null && classifierOptions.length() > 0) {
        try {
          splitOptions = Utils.splitOptions(classifierOptions)
        } catch (Exception ex) {
          if (m_log != null) {
            m_log.statusMessage("LearningCurve\$"+hashCode()+"ERROR (see log for details)")
            m_log.logMessage("[LearningCurve] Problem parsing classifier options") 
          } 
          return
        }
      }
      classifierToUse = AbstractClassifier.forName(classifierName, splitOptions)
    } else {
        classifierToUse = m_connectedConfigurable.getClassifierTemplate()
        classifierToUse = weka.classifiers.AbstractClassifier.makeCopy(classifierToUse)
    }

    double hS = Double.parseDouble(hSize)
    hS /= 100
    int sS = Integer.parseInt(sSize)
    int nS = Integer.parseInt(nSteps)

    int numInHoldout = hS * insts.numInstances()
    Instances holdoutI = new Instances(insts, numInHoldout)
    for (int i = insts.numInstances() - numInHoldout; i < insts.numInstances(); i++) {
          holdoutI.add(insts.instance(i)) 
    }

    String classifierSetUpString = classifierToUse.class.toString() + " "
    if (classifierToUse instanceof OptionHandler) {
      classifierSetUpString += Utils.joinOptions(((OptionHandler)classifierToUse).getOptions()) 
    }

    if (m_log != null) {
      m_log.logMessage("[LearningCurve] Using classifier " + classifierSetUpString) 
    }

    // create the instances structure to hold the learning curve results
    Attribute setSize = new Attribute("NumInstances")
    Attribute aucA = new Attribute("PercentCorrect")
    FastVector atts = new FastVector()
    atts.addElement(setSize)
    atts.addElement(aucA)

    // The preceeding "__" tells the DataVisualizer to connect the points with lines
    Instances learnCInstances = new Instances("__Learning curve: " + classifierSetUpString, atts, 0)

    boolean done = false
    Instances training = new Instances(insts, 0)
    for (int i = 0; i < nS; i++) {
       if (m_log != null) {
         m_log.statusMessage("LearningCurve\$"+hashCode()+"|Processing set "+(i+1))
       }
     
       int numInThisStep = ((i + 1) * sS)
       if (numInThisStep >= (insts.numInstances() - numInHoldout)) {
         numInThisStep = (insts.numInstances() - numInHoldout)
         done = true 
       }
       for (int k = (i * sS); k < numInThisStep; k++) {
         training.add(insts.instance(k)) 
       }
    

       // train on this set
       Classifier newModel = AbstractClassifier.makeCopies(classifierToUse, 1)[0]
       newModel.buildClassifier(training)

       Evaluation eval = new Evaluation(holdoutI)
       eval.evaluateModel(newModel, holdoutI)
       double pc = (1.0 - eval.errorRate()) * 100.0
       //double auc = 1.0 - eval.errorRate();
       buff.append(""+numInThisStep+","+pc+"\n")
       //System.err.println(""+numInThisStep+","+auc+"\n")
       Instance newInst = new DenseInstance(2)
       newInst.setValue(0, (double)numInThisStep)
       newInst.setValue(1, pc)
       learnCInstances.add(newInst)
       if (done) {
         break 
       } 
    }

    if (m_log != null) {
      m_log.statusMessage("LearningCurve\$"+hashCode()+"|Finished.")
    }
    //System.err.println(buff.toString())
    m_helper.notifyTextListeners(new TextEvent(this, buff.toString(), "learning curve"))
    m_helper.notifyDataSourceListeners(new DataSetEvent(this, learnCInstances))
  }

  void acceptTestSet(TestSetEvent e) { }

  void acceptDataSet(DataSetEvent e) { }

  void acceptInstance(InstanceEvent e) { }

  void acceptText(TextEvent e) { }

  void acceptClassifier(BatchClassifierEvent e) { }

  void acceptClassifier(IncrementalClassifierEvent e) { }

  void acceptClusterer(BatchClustererEvent e) { }

  void acceptGraph(GraphEvent e) { }

  void acceptDataPoint(ChartEvent e) { }

  void acceptDataSet(ThresholdDataEvent e) { }

  void acceptDataSet(VisualizableErrorEvent e) { }

  void acceptConfiguration(ConfigurationEvent e) { }

}