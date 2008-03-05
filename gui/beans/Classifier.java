/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Classifier.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import weka.classifiers.rules.ZeroR;
import weka.core.Instances;
import weka.core.xml.KOML;
import weka.core.xml.XStream;
import weka.gui.Logger;
import weka.gui.ExtensionFileFilter;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Bean that wraps around weka.classifiers
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.25.2.5 $
 * @since 1.0
 * @see JPanel
 * @see BeanCommon
 * @see Visible
 * @see WekaWrapper
 * @see Serializable
 * @see UserRequestAcceptor
 * @see TrainingSetListener
 * @see TestSetListener
 */
public class Classifier
  extends JPanel
  implements BeanCommon, Visible, 
	     WekaWrapper, EventConstraints,
	     Serializable, UserRequestAcceptor,
	     TrainingSetListener, TestSetListener,
	     InstanceListener {

  /** for serialization */
  private static final long serialVersionUID = 659603893917736008L;

  protected BeanVisual m_visual = 
    new BeanVisual("Classifier",
		   BeanVisual.ICON_PATH+"DefaultClassifier.gif",
		   BeanVisual.ICON_PATH+"DefaultClassifier_animated.gif");

  private static int IDLE = 0;
  private static int BUILDING_MODEL = 1;
  private static int CLASSIFYING = 2;

  private int m_state = IDLE;

  private Thread m_buildThread = null;

  /**
   * Global info for the wrapped classifier (if it exists).
   */
  protected String m_globalInfo;

  /**
   * Objects talking to us
   */
  private Hashtable m_listenees = new Hashtable();

  /**
   * Objects listening for batch classifier events
   */
  private Vector m_batchClassifierListeners = new Vector();

  /**
   * Objects listening for incremental classifier events
   */
  private Vector m_incrementalClassifierListeners = new Vector();

  /**
   * Objects listening for graph events
   */
  private Vector m_graphListeners = new Vector();

  /**
   * Objects listening for text events
   */
  private Vector m_textListeners = new Vector();

  /**
   * Holds training instances for batch training. Not transient because
   * header is retained for validating any instance events that this
   * classifier might be asked to predict in the future.
   */
  private Instances m_trainingSet;
  private transient Instances m_testingSet;
  private weka.classifiers.Classifier m_Classifier = new ZeroR();
  private IncrementalClassifierEvent m_ie = 
    new IncrementalClassifierEvent(this);

  /** the extension for serialized models (binary Java serialization) */
  public final static String FILE_EXTENSION = "model";

  private transient JFileChooser m_fileChooser = 
    new JFileChooser(new File(System.getProperty("user.dir")));

  protected FileFilter m_binaryFilter =
    new ExtensionFileFilter("."+FILE_EXTENSION, "Binary serialized model file (*"
                            + FILE_EXTENSION + ")");

  protected FileFilter m_KOMLFilter =
    new ExtensionFileFilter(KOML.FILE_EXTENSION + FILE_EXTENSION,
                            "XML serialized model file (*"
                            + KOML.FILE_EXTENSION + FILE_EXTENSION + ")");

  protected FileFilter m_XStreamFilter =
    new ExtensionFileFilter(XStream.FILE_EXTENSION + FILE_EXTENSION,
                            "XML serialized model file (*"
                            + XStream.FILE_EXTENSION + FILE_EXTENSION + ")");

  /**
   * If the classifier is an incremental classifier, should we
   * update it (ie train it on incoming instances). This makes it
   * possible incrementally test on a separate stream of instances
   * without updating the classifier, or mix batch training/testing
   * with incremental training/testing
   */
  private boolean m_updateIncrementalClassifier = true;

  private transient Logger m_log = null;

  /**
   * Event to handle when processing incremental updates
   */
  private InstanceEvent m_incrementalEvent;
  private Double m_dummy = new Double(0.0);

  /**
   * Global info (if it exists) for the wrapped classifier
   *
   * @return the global info
   */
  public String globalInfo() {
    return m_globalInfo;
  }

  /**
   * Creates a new <code>Classifier</code> instance.
   */
  public Classifier() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
    setClassifier(m_Classifier);
    
    setupFileChooser();
  }

  protected void setupFileChooser() {
    if (m_fileChooser == null) {
      m_fileChooser = 
        new JFileChooser(new File(System.getProperty("user.dir")));
    }

    m_fileChooser.addChoosableFileFilter(m_binaryFilter);
    if (KOML.isPresent()) {
      m_fileChooser.addChoosableFileFilter(m_KOMLFilter);
    }
    if (XStream.isPresent()) {
      m_fileChooser.addChoosableFileFilter(m_XStreamFilter);
    }
    m_fileChooser.setFileFilter(m_binaryFilter);
  }

  /**
   * Set the classifier for this wrapper
   *
   * @param c a <code>weka.classifiers.Classifier</code> value
   */
  public void setClassifier(weka.classifiers.Classifier c) {
    boolean loadImages = true;
    if (c.getClass().getName().
	compareTo(m_Classifier.getClass().getName()) == 0) {
      loadImages = false;
    } else {
      // classifier has changed so any batch training status is now
      // invalid
      m_trainingSet = null;
    }
    m_Classifier = c;
    String classifierName = c.getClass().toString();
    classifierName = classifierName.substring(classifierName.
					      lastIndexOf('.')+1, 
					      classifierName.length());
    if (loadImages) {
      if (!m_visual.loadIcons(BeanVisual.ICON_PATH+classifierName+".gif",
		       BeanVisual.ICON_PATH+classifierName+"_animated.gif")) {
	useDefaultVisual();
      }
    }
    m_visual.setText(classifierName);

    if (!(m_Classifier instanceof weka.classifiers.UpdateableClassifier) &&
	(m_listenees.containsKey("instance"))) {
      if (m_log != null) {
	m_log.logMessage("WARNING : "+m_Classifier.getClass().getName()
			 +" is not an incremental classifier (Classifier)");
      }
    }
    // get global info
    m_globalInfo = KnowledgeFlowApp.getGlobalInfo(m_Classifier);
  }

  /**
   * Returns true if this classifier has an incoming connection that is
   * an instance stream
   *
   * @return true if has an incoming connection that is an instance stream
   */
  public boolean hasIncomingStreamInstances() {
    if (m_listenees.size() == 0) {
      return false;
    }
    if (m_listenees.containsKey("instance")) {
      return true;
    }
    return false;
  }

  /**
   * Returns true if this classifier has an incoming connection that is
   * a batch set of instances
   *
   * @return a <code>boolean</code> value
   */
  public boolean hasIncomingBatchInstances() {
    if (m_listenees.size() == 0) {
      return false;
    }
    if (m_listenees.containsKey("trainingSet") ||
	m_listenees.containsKey("testSet")) {
      return true;
    }
    return false;
  }

  /**
   * Get the classifier currently set for this wrapper
   *
   * @return a <code>weka.classifiers.Classifier</code> value
   */
  public weka.classifiers.Classifier getClassifier() {
    return m_Classifier;
  }

  /**
   * Sets the algorithm (classifier) for this bean
   *
   * @param algorithm an <code>Object</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void setWrappedAlgorithm(Object algorithm) 
    {

    if (!(algorithm instanceof weka.classifiers.Classifier)) { 
      throw new IllegalArgumentException(algorithm.getClass()+" : incorrect "
					 +"type of algorithm (Classifier)");
    }
    setClassifier((weka.classifiers.Classifier)algorithm);
  }

  /**
   * Returns the wrapped classifier
   *
   * @return an <code>Object</code> value
   */
  public Object getWrappedAlgorithm() {
    return getClassifier();
  }

  public boolean getUpdateIncrementalClassifier() {
    return m_updateIncrementalClassifier;
  }

  public void setUpdateIncrementalClassifier(boolean update) {
    m_updateIncrementalClassifier = update;
  }

//    public void acceptDataSet(DataSetEvent e) {
//      // will wrap up data in a TrainingSetEvent and call acceptTrainingSet
//      // then will do same for TestSetEvent
//      acceptTrainingSet(new TrainingSetEvent(e.getSource(), e.getDataSet()));
//    }

  /**
   * Accepts an instance for incremental processing.
   *
   * @param e an <code>InstanceEvent</code> value
   */
  public void acceptInstance(InstanceEvent e) {
    /*    if (m_buildThread == null) {
	  System.err.println("Starting handler ");
	  startIncrementalHandler();
	  } */
    //    if (m_Classifier instanceof weka.classifiers.UpdateableClassifier) {
    /*      synchronized(m_dummy) {
	    m_state = BUILDING_MODEL;
	    m_incrementalEvent = e;
	    m_dummy.notifyAll();
	    }
	    try {
	    //	  if (m_state == BUILDING_MODEL && m_buildThread != null) {
	    block(true);
	    //	  }
	    } catch (Exception ex) {
	    return;
	    } */
    m_incrementalEvent = e;
    handleIncrementalEvent();
    //    }
  }

  /**
   * Handles initializing and updating an incremental classifier
   */
  private void handleIncrementalEvent() {
    if (m_buildThread != null) {
      String messg = "Classifier is currently batch training!";
      if (m_log != null) {
	m_log.logMessage(messg);
      } else {
	System.err.println(messg);
      }
      return;
    }

    if (m_incrementalEvent.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      //      Instances dataset = m_incrementalEvent.getInstance().dataset();
      Instances dataset = m_incrementalEvent.getStructure();
      // default to the last column if no class is set
      if (dataset.classIndex() < 0) {
	//	System.err.println("Classifier : setting class index...");
	dataset.setClassIndex(dataset.numAttributes()-1);
      }
      try {
	// initialize classifier if m_trainingSet is null
	// otherwise assume that classifier has been pre-trained in batch
	// mode, *if* headers match
	if (m_trainingSet == null || (!dataset.equalHeaders(m_trainingSet))) {
	  if (!(m_Classifier instanceof 
		weka.classifiers.UpdateableClassifier)) {
	    if (m_log != null) {
	      String msg = (m_trainingSet == null)
		? "ERROR : "+m_Classifier.getClass().getName()
		+" has not been batch "
		+"trained; can't process instance events."
		: "ERROR : instance event's structure is different from "
		+"the data that "
		+ "was used to batch train this classifier; can't continue.";
	      m_log.logMessage(msg);
	    }
	    return;
	  }
	  if (m_trainingSet != null && 
	      (!dataset.equalHeaders(m_trainingSet))) {
	    if (m_log != null) {
	      m_log.logMessage("Warning : structure of instance events differ "
			       +"from data used in batch training this "
			       +"classifier. Resetting classifier...");
	    }
	    m_trainingSet = null;
	  }
	  if (m_trainingSet == null) {
	    // initialize the classifier if it hasn't been trained yet
	    m_trainingSet = new Instances(dataset, 0);
	    m_Classifier.buildClassifier(m_trainingSet);
	  }
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
      // Notify incremental classifier listeners of new batch
      System.err.println("NOTIFYING NEW BATCH");
      m_ie.setStructure(dataset); 
      m_ie.setClassifier(m_Classifier);

      notifyIncrementalClassifierListeners(m_ie);
      return;
    } else {
      if (m_trainingSet == null) {
	// simply return. If the training set is still null after
	// the first instance then the classifier must not be updateable
	// and hasn't been previously batch trained - therefore we can't
	// do anything meaningful
	return;
      }
    }

    try {
      // test on this instance
      int status = IncrementalClassifierEvent.WITHIN_BATCH;
      /*      if (m_incrementalEvent.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
	      status = IncrementalClassifierEvent.NEW_BATCH; */
      /* } else */ if (m_incrementalEvent.getStatus() ==
		       InstanceEvent.BATCH_FINISHED) {
	status = IncrementalClassifierEvent.BATCH_FINISHED;
      }

      m_ie.setStatus(status); m_ie.setClassifier(m_Classifier);
      m_ie.setCurrentInstance(m_incrementalEvent.getInstance());

      notifyIncrementalClassifierListeners(m_ie);

      // now update on this instance (if class is not missing and classifier
      // is updateable and user has specified that classifier is to be
      // updated)
      if (m_Classifier instanceof weka.classifiers.UpdateableClassifier &&
	  m_updateIncrementalClassifier == true &&
	  !(m_incrementalEvent.getInstance().
	    isMissing(m_incrementalEvent.getInstance().
		      dataset().classIndex()))) {
	((weka.classifiers.UpdateableClassifier)m_Classifier).
	  updateClassifier(m_incrementalEvent.getInstance());
      }
      if (m_incrementalEvent.getStatus() == 
	  InstanceEvent.BATCH_FINISHED) {
	if (m_textListeners.size() > 0) {
	  String modelString = m_Classifier.toString();
	  String titleString = m_Classifier.getClass().getName();

	  titleString = titleString.
	    substring(titleString.lastIndexOf('.') + 1,
		      titleString.length());
	  modelString = "=== Classifier model ===\n\n" +
	    "Scheme:   " +titleString+"\n" +
	    "Relation: "  + m_trainingSet.relationName() + "\n\n"
	    + modelString;
	  titleString = "Model: " + titleString;
	  TextEvent nt = new TextEvent(this,
				       modelString,
				       titleString);
	  notifyTextListeners(nt);
	}
      }
    } catch (Exception ex) {
      if (m_log != null) {
	m_log.logMessage(ex.toString());
      }
      ex.printStackTrace();
    }
  }

  /**
   * Unused at present
   */
  private void startIncrementalHandler() {
    if (m_buildThread == null) {
      m_buildThread = new Thread() {
	  public void run() {
	    while (true) {
	      synchronized(m_dummy) {
		try {
		  m_dummy.wait();
		} catch (InterruptedException ex) {
		  //		  m_buildThread = null;
		  //		  System.err.println("Here");
		  return;
		}
	      }
	      Classifier.this.handleIncrementalEvent();
	      m_state = IDLE;
	      block(false);
	    }
	  }
	};
      m_buildThread.setPriority(Thread.MIN_PRIORITY);
      m_buildThread.start();
      // give thread a chance to start
      try {
	Thread.sleep(500);
      } catch (InterruptedException ex) {
      }
    }
  }

  /**
   * Accepts a training set and builds batch classifier
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  public void acceptTrainingSet(final TrainingSetEvent e) {
    if (e.isStructureOnly()) {
      // no need to build a classifier, instead just generate a dummy
      // BatchClassifierEvent in order to pass on instance structure to
      // any listeners - eg. PredictionAppender can use it to determine
      // the final structure of instances with predictions appended
      BatchClassifierEvent ce = 
	new BatchClassifierEvent(this, m_Classifier, 
				 new DataSetEvent(this, e.getTrainingSet()),
				 new DataSetEvent(this, e.getTrainingSet()),
				 e.getSetNumber(), e.getMaxSetNumber());

      notifyBatchClassifierListeners(ce);
      return;
    }
    if (m_buildThread == null) {
      try {
	if (m_state == IDLE) {
	  synchronized (this) {
	    m_state = BUILDING_MODEL;
	  }
	  m_trainingSet = e.getTrainingSet();
	  final String oldText = m_visual.getText();
	  m_buildThread = new Thread() {
	      public void run() {
		try {
		  if (m_trainingSet != null) {
		    if (m_trainingSet.classIndex() < 0) {
		      // assume last column is the class
		      m_trainingSet.setClassIndex(m_trainingSet.numAttributes()-1);
		      if (m_log != null) {
			m_log.logMessage("Classifier : assuming last "
					 +"column is the class");
		      }
		    }
		    m_visual.setAnimated();
		    m_visual.setText("Building model...");
		    if (m_log != null) {
		      m_log.statusMessage("Classifier : building model...");
		    }
		    buildClassifier();

                    if (m_batchClassifierListeners.size() > 0) {
                      // notify anyone who might be interested in just the model
                      // and training set
                      BatchClassifierEvent ce = 
                        new BatchClassifierEvent(this, m_Classifier, 
                                                 new DataSetEvent(this, e.getTrainingSet()),
                                                 null, // no test set
                                                 e.getSetNumber(), e.getMaxSetNumber());
                      notifyBatchClassifierListeners(ce);
                    }

		    if (m_Classifier instanceof weka.core.Drawable && 
			m_graphListeners.size() > 0) {
		      String grphString = 
			((weka.core.Drawable)m_Classifier).graph();
                      int grphType = ((weka.core.Drawable)m_Classifier).graphType();
		      String grphTitle = m_Classifier.getClass().getName();
		      grphTitle = grphTitle.substring(grphTitle.
						      lastIndexOf('.')+1, 
						      grphTitle.length());
		      grphTitle = "Set " + e.getSetNumber() + " ("
			+e.getTrainingSet().relationName() + ") "
			+grphTitle;
		      
		      GraphEvent ge = new GraphEvent(Classifier.this, 
						     grphString, 
						     grphTitle,
                                                     grphType);
		      notifyGraphListeners(ge);
		    }

		    if (m_textListeners.size() > 0) {
		      String modelString = m_Classifier.toString();
		      String titleString = m_Classifier.getClass().getName();
		      
		      titleString = titleString.
			substring(titleString.lastIndexOf('.') + 1,
				  titleString.length());
		      modelString = "=== Classifier model ===\n\n" +
			"Scheme:   " +titleString+"\n" +
			"Relation: "  + m_trainingSet.relationName() + 
			((e.getMaxSetNumber() > 1) 
			 ? "\nTraining Fold: "+e.getSetNumber()
			 :"")
			+ "\n\n"
			+ modelString;
		      titleString = "Model: " + titleString;

		      TextEvent nt = new TextEvent(Classifier.this,
						   modelString,
						   titleString);
		      notifyTextListeners(nt);
		    }
		  }
		} catch (Exception ex) {
		  ex.printStackTrace();
		} finally {
		  m_visual.setText(oldText);
		  m_visual.setStatic();
		  m_state = IDLE;
		  if (isInterrupted()) {
		    // prevent any classifier events from being fired
		    m_trainingSet = null;
		    if (m_log != null) {
                      String titleString = m_Classifier.getClass().getName();		      
		      titleString = titleString.
			substring(titleString.lastIndexOf('.') + 1,
				  titleString.length());
		      m_log.logMessage("Build classifier ("
                                       + titleString + ") interrupted!");
		      m_log.statusMessage("Interrupted");
		    }
		  } else {
		    // save header
		    //m_trainingSet = new Instances(m_trainingSet, 0);
		  }
		  if (m_log != null) {
		    m_log.statusMessage("OK");
		  }
		  block(false);
		}
	      }	
	    };
	  m_buildThread.setPriority(Thread.MIN_PRIORITY);
	  m_buildThread.start();
	  // make sure the thread is still running before we block
	  //	  if (m_buildThread.isAlive()) {
	  block(true);
	    //	  }
	  m_buildThread = null;
	  m_state = IDLE;
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }

  /**
   * Accepts a test set for a batch trained classifier
   *
   * @param e a <code>TestSetEvent</code> value
   */
  public void acceptTestSet(TestSetEvent e) {

    if (m_trainingSet != null) {
      try {
	if (m_state == IDLE) {
	  synchronized(this) {
	    m_state = CLASSIFYING;
	  }

	  m_testingSet = e.getTestSet();
	  if (m_testingSet != null) {
	    if (m_testingSet.classIndex() < 0) {
	      m_testingSet.setClassIndex(m_testingSet.numAttributes()-1);
	    }
	  }
	  if (m_trainingSet.equalHeaders(m_testingSet)) {

	    BatchClassifierEvent ce = 
	      new BatchClassifierEvent(this, m_Classifier, 				       
				       new DataSetEvent(this, m_trainingSet),
				       new DataSetEvent(this, e.getTestSet()),
				  e.getSetNumber(), e.getMaxSetNumber());

	    //	    System.err.println("Just before notify classifier listeners");
	    notifyBatchClassifierListeners(ce);
	    //	    System.err.println("Just after notify classifier listeners");
	  }
	  m_state = IDLE;
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }


  private void buildClassifier() throws Exception {
    m_Classifier.buildClassifier(m_trainingSet);
  }

  /**
   * Sets the visual appearance of this wrapper bean
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Gets the visual appearance of this wrapper bean
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Use the default visual appearance for this bean
   */
  public void useDefaultVisual() {
    // try to get a default for this package of classifiers
    String name = m_Classifier.getClass().toString();
    String packageName = name.substring(0, name.lastIndexOf('.'));
    packageName = 
      packageName.substring(packageName.lastIndexOf('.')+1,
                            packageName.length());
    if (!m_visual.loadIcons(BeanVisual.ICON_PATH+"Default_"+packageName
                            +"Classifier.gif",
                            BeanVisual.ICON_PATH+"Default_"+packageName
                            +"Classifier_animated.gif")) {
      m_visual.loadIcons(BeanVisual.
                         ICON_PATH+"DefaultClassifier.gif",
                         BeanVisual.
                         ICON_PATH+"DefaultClassifier_animated.gif");
    }
  }

  /**
   * Add a batch classifier listener
   *
   * @param cl a <code>BatchClassifierListener</code> value
   */
  public synchronized void 
    addBatchClassifierListener(BatchClassifierListener cl) {
    m_batchClassifierListeners.addElement(cl);
  }

  /**
   * Remove a batch classifier listener
   *
   * @param cl a <code>BatchClassifierListener</code> value
   */
  public synchronized void 
    removeBatchClassifierListener(BatchClassifierListener cl) {
    m_batchClassifierListeners.remove(cl);
  }

  /**
   * Notify all batch classifier listeners of a batch classifier event
   *
   * @param ce a <code>BatchClassifierEvent</code> value
   */
  private void notifyBatchClassifierListeners(BatchClassifierEvent ce) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_batchClassifierListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((BatchClassifierListener)l.elementAt(i)).acceptClassifier(ce);
      }
    }
  }

  /**
   * Add a graph listener
   *
   * @param cl a <code>GraphListener</code> value
   */
  public synchronized void addGraphListener(GraphListener cl) {
    m_graphListeners.addElement(cl);
  }

  /**
   * Remove a graph listener
   *
   * @param cl a <code>GraphListener</code> value
   */
  public synchronized void removeGraphListener(GraphListener cl) {
    m_graphListeners.remove(cl);
  }

  /**
   * Notify all graph listeners of a graph event
   *
   * @param ge a <code>GraphEvent</code> value
   */
  private void notifyGraphListeners(GraphEvent ge) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_graphListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((GraphListener)l.elementAt(i)).acceptGraph(ge);
      }
    }
  }

  /**
   * Add a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void addTextListener(TextListener cl) {
    m_textListeners.addElement(cl);
  }

  /**
   * Remove a text listener
   *
   * @param cl a <code>TextListener</code> value
   */
  public synchronized void removeTextListener(TextListener cl) {
    m_textListeners.remove(cl);
  }

  /**
   * Notify all text listeners of a text event
   *
   * @param ge a <code>TextEvent</code> value
   */
  private void notifyTextListeners(TextEvent ge) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_textListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((TextListener)l.elementAt(i)).acceptText(ge);
      }
    }
  }

  /**
   * Add an incremental classifier listener
   *
   * @param cl an <code>IncrementalClassifierListener</code> value
   */
  public synchronized void 
    addIncrementalClassifierListener(IncrementalClassifierListener cl) {
    m_incrementalClassifierListeners.add(cl);
  }

  /**
   * Remove an incremental classifier listener
   *
   * @param cl an <code>IncrementalClassifierListener</code> value
   */
  public synchronized void 
    removeIncrementalClassifierListener(IncrementalClassifierListener cl) {
    m_incrementalClassifierListeners.remove(cl);
  }

  /**
   * Notify all incremental classifier listeners of an incremental classifier
   * event
   *
   * @param ce an <code>IncrementalClassifierEvent</code> value
   */
  private void 
    notifyIncrementalClassifierListeners(IncrementalClassifierEvent ce) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_incrementalClassifierListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((IncrementalClassifierListener)l.elementAt(i)).acceptClassifier(ce);
      }
    }
  }

  /**
   * Returns true if, at this time, 
   * the object will accept a connection with respect to the named event
   *
   * @param eventName the event
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(String eventName) {
    /*    if (eventName.compareTo("instance") == 0) {
      if (!(m_Classifier instanceof weka.classifiers.UpdateableClassifier)) {
	return false;
      }
      } */
    if (m_listenees.containsKey(eventName)) {
      return false;
    }
    return true;
  }

  /**
   * Returns true if, at this time, 
   * the object will accept a connection according to the supplied
   * EventSetDescriptor
   *
   * @param esd the EventSetDescriptor
   * @return true if the object will accept a connection
   */
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

  /**
   * Notify this object that it has been registered as a listener with
   * a source with respect to the named event
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public synchronized void connectionNotification(String eventName,
						  Object source) {
    if (eventName.compareTo("instance") == 0) {
      if (!(m_Classifier instanceof weka.classifiers.UpdateableClassifier)) {
	if (m_log != null) {
	  m_log.logMessage("Warning : " + m_Classifier.getClass().getName()
			   + " is not an updateable classifier. This "
			   +"classifier will only be evaluated on incoming "
			   +"instance events and not trained on them.");
	}
      }
    }

    if (connectionAllowed(eventName)) {
      m_listenees.put(eventName, source);
      /*      if (eventName.compareTo("instance") == 0) {
	startIncrementalHandler();
	} */
    }
  }

  /**
   * Notify this object that it has been deregistered as a listener with
   * a source with respect to the supplied event name
   *
   * @param eventName the event
   * @param source the source with which this object has been registered as
   * a listener
   */
  public synchronized void disconnectionNotification(String eventName,
						     Object source) {
    m_listenees.remove(eventName);
    if (eventName.compareTo("instance") == 0) {
      stop(); // kill the incremental handler thread if it is running
    }
  }

  /**
   * Function used to stop code that calls acceptTrainingSet. This is 
   * needed as classifier construction is performed inside a separate
   * thread of execution.
   *
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {

    if (tf) {
      try {
	  // only block if thread is still doing something useful!
	if (m_buildThread.isAlive() && m_state != IDLE) {
	  wait();
	  }
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }


  /**
   * Stop any classifier action
   */
  public void stop() {
    // tell all listenees (upstream beans) to stop
    Enumeration en = m_listenees.keys();
    while (en.hasMoreElements()) {
      Object tempO = m_listenees.get(en.nextElement());
      if (tempO instanceof BeanCommon) {
	System.err.println("Listener is BeanCommon");
	((BeanCommon)tempO).stop();
      }
    }

    // stop the build thread
    if (m_buildThread != null) {
      m_buildThread.interrupt();
      m_buildThread.stop();
      m_buildThread = null;
      m_visual.setStatic();
    }
  }

  public void loadModel() {
    try {
      if (m_fileChooser == null) {
        // i.e. after de-serialization
        setupFileChooser();
      }
      int returnVal = m_fileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File loadFrom = m_fileChooser.getSelectedFile();

        // add extension if necessary
        if (m_fileChooser.getFileFilter() == m_binaryFilter) {
          if (!loadFrom.getName().toLowerCase().endsWith("." + FILE_EXTENSION)) {
            loadFrom = new File(loadFrom.getParent(),
                                loadFrom.getName() + "." + FILE_EXTENSION);
          }
        } else if (m_fileChooser.getFileFilter() == m_KOMLFilter) {
          if (!loadFrom.getName().toLowerCase().endsWith(KOML.FILE_EXTENSION 
                                                         + FILE_EXTENSION)) {
            loadFrom = new File(loadFrom.getParent(),
                                loadFrom.getName() + KOML.FILE_EXTENSION 
                                + FILE_EXTENSION);
          }
        } else if (m_fileChooser.getFileFilter() == m_XStreamFilter) {
          if (!loadFrom.getName().toLowerCase().endsWith(XStream.FILE_EXTENSION 
                                                        + FILE_EXTENSION)) {
            loadFrom = new File(loadFrom.getParent(),
                                loadFrom.getName() + XStream.FILE_EXTENSION 
                                + FILE_EXTENSION);
          }
        }

        weka.classifiers.Classifier temp = null;
        Instances tempHeader = null;
        // KOML ?
        if ((KOML.isPresent()) &&
            (loadFrom.getAbsolutePath().toLowerCase().
             endsWith(KOML.FILE_EXTENSION + FILE_EXTENSION))) {
          Vector v = (Vector) KOML.read(loadFrom.getAbsolutePath());
          temp = (weka.classifiers.Classifier) v.elementAt(0);
          if (v.size() == 2) {
            // try and grab the header
            tempHeader = (Instances) v.elementAt(1);
          }
        } /* XStream */ else if ((XStream.isPresent()) &&
                                 (loadFrom.getAbsolutePath().toLowerCase().
                                  endsWith(XStream.FILE_EXTENSION + FILE_EXTENSION))) {
          Vector v = (Vector) XStream.read(loadFrom.getAbsolutePath());
          temp = (weka.classifiers.Classifier) v.elementAt(0);
          if (v.size() == 2) {
            // try and grab the header
            tempHeader = (Instances) v.elementAt(1);
          } 
        } /* binary */ else {

          ObjectInputStream is = 
            new ObjectInputStream(new BufferedInputStream(
                                                          new FileInputStream(loadFrom)));
          // try and read the model
          temp = (weka.classifiers.Classifier)is.readObject();
          // try and read the header (if present)
          try {
            tempHeader = (Instances)is.readObject();
          } catch (Exception ex) {
            //            System.err.println("No header...");
            // quietly ignore
          }
          is.close();
        }

        // Update name and icon
        setClassifier(temp);
        // restore header
        m_trainingSet = tempHeader;

        if (m_log != null) {
          m_log.logMessage("Loaded classifier: "
                           + m_Classifier.getClass().toString());
        }
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(Classifier.this,
                                    "Problem loading classifier.\n",
                                    "Load Model",
                                    JOptionPane.ERROR_MESSAGE);
      if (m_log != null) {
        m_log.logMessage("Problem loading classifier. " + ex.getMessage());
      }
    }
  }

  public void saveModel() {
    try {
      if (m_fileChooser == null) {
        // i.e. after de-serialization
        setupFileChooser();
      }
      int returnVal = m_fileChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File saveTo = m_fileChooser.getSelectedFile();
        String fn = saveTo.getAbsolutePath();
        if (m_fileChooser.getFileFilter() == m_binaryFilter) {
          if (!fn.toLowerCase().endsWith("." + FILE_EXTENSION)) {
            fn += "." + FILE_EXTENSION;
          }
        } else if (m_fileChooser.getFileFilter() == m_KOMLFilter) {
          if (!fn.toLowerCase().endsWith(KOML.FILE_EXTENSION + FILE_EXTENSION)) {
            fn += KOML.FILE_EXTENSION + FILE_EXTENSION;
          }
        } else if (m_fileChooser.getFileFilter() == m_XStreamFilter) {
          if (!fn.toLowerCase().endsWith(XStream.FILE_EXTENSION + FILE_EXTENSION)) {
            fn += XStream.FILE_EXTENSION + FILE_EXTENSION;
          }
        }
        saveTo = new File(fn);

        // now serialize model
        // KOML?
        if ((KOML.isPresent()) &&
            saveTo.getAbsolutePath().toLowerCase().
            endsWith(KOML.FILE_EXTENSION + FILE_EXTENSION)) {
          SerializedModelSaver.saveKOML(saveTo,
                                        m_Classifier,
                                        (m_trainingSet != null)
                                        ? new Instances(m_trainingSet, 0)
                                        : null);
          /*          Vector v = new Vector();
          v.add(m_Classifier);
          if (m_trainingSet != null) {
            v.add(new Instances(m_trainingSet, 0));
          }
          v.trimToSize();
          KOML.write(saveTo.getAbsolutePath(), v); */
        } /* XStream */ else if ((XStream.isPresent()) &&
                                 saveTo.getAbsolutePath().toLowerCase().
            endsWith(XStream.FILE_EXTENSION + FILE_EXTENSION)) {

          SerializedModelSaver.saveXStream(saveTo,
                                           m_Classifier,
                                           (m_trainingSet != null)
                                           ? new Instances(m_trainingSet, 0)
                                           : null);
          /*          Vector v = new Vector();
          v.add(m_Classifier);
          if (m_trainingSet != null) {
            v.add(new Instances(m_trainingSet, 0));
          }
          v.trimToSize();
          XStream.write(saveTo.getAbsolutePath(), v); */
        } else /* binary */ {
          ObjectOutputStream os = 
            new ObjectOutputStream(new BufferedOutputStream(
                                   new FileOutputStream(saveTo)));
          os.writeObject(m_Classifier);
          if (m_trainingSet != null) {
            Instances header = new Instances(m_trainingSet, 0);
            os.writeObject(header);
          }
          os.close();
        }
        if (m_log != null) {
          m_log.logMessage("Saved classifier OK.");
        }
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(Classifier.this,
                                    "Problem saving classifier.\n",
                                    "Save Model",
                                    JOptionPane.ERROR_MESSAGE);
      if (m_log != null) {
        m_log.logMessage("Problem saving classifier. " + ex.getMessage());
      }
    }
  }

  /**
   * Set a logger
   *
   * @param logger a <code>Logger</code> value
   */
  public void setLog(Logger logger) {
    m_log = logger;
  }

  /**
   * Return an enumeration of requests that can be made by the user
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_buildThread != null) {
      newVector.addElement("Stop");
    }

    if (m_buildThread == null && 
        m_Classifier != null) {
      newVector.addElement("Save model");
    }

    if (m_buildThread == null) {
      newVector.addElement("Load model");
    }
    return newVector.elements();
  }

  /**
   * Perform a particular request
   *
   * @param request the request to perform
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Stop") == 0) {
      stop();
    } else if (request.compareTo("Save model") == 0) {
      saveModel();
    } else if (request.compareTo("Load model") == 0) {
      loadModel();
    } else {
      throw new IllegalArgumentException(request
					 + " not supported (Classifier)");
    }
  }

  /**
   * Returns true, if at the current time, the event described by the
   * supplied event descriptor could be generated.
   *
   * @param esd an <code>EventSetDescriptor</code> value
   * @return a <code>boolean</code> value
   */
  public boolean eventGeneratable(EventSetDescriptor esd) {
    String eventName = esd.getName();
    return eventGeneratable(eventName);
  }
  
  /**
   * @param name of the event to check
   * @return true if eventName is one of the possible events
   * that this component can generate
   */
  private boolean generatableEvent(String eventName) {
    if (eventName.compareTo("graph") == 0
	|| eventName.compareTo("text") == 0
	|| eventName.compareTo("batchClassifier") == 0
	|| eventName.compareTo("incrementalClassifier") == 0) {
      return true;
    }
    return false;
  }

  /**
   * Returns true, if at the current time, the named event could
   * be generated. Assumes that the supplied event name is
   * an event that could be generated by this bean
   *
   * @param eventName the name of the event in question
   * @return true if the named event could be generated at this point in
   * time
   */
  public boolean eventGeneratable(String eventName) {
    if (!generatableEvent(eventName)) {
      return false;
    }
    if (eventName.compareTo("graph") == 0) {
      // can't generate a GraphEvent if classifier is not drawable
      if (!(m_Classifier instanceof weka.core.Drawable)) {
	return false;
      }
      // need to have a training set before the classifier
      // can generate a graph!
      if (!m_listenees.containsKey("trainingSet")) {
	return false;
      }
      // Source needs to be able to generate a trainingSet
      // before we can generate a graph
      Object source = m_listenees.get("trainingSet");
       if (source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("trainingSet")) {
	  return false;
	}
      }
    }

    if (eventName.compareTo("batchClassifier") == 0) {
      /*      if (!m_listenees.containsKey("testSet")) {
        return false;
      }
      if (!m_listenees.containsKey("trainingSet") && 
          m_trainingSet == null) {
	return false;
        } */
      if (!m_listenees.containsKey("testSet") && 
          !m_listenees.containsKey("trainingSet")) {
        return false;
      }
      Object source = m_listenees.get("testSet");
      if (source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("testSet")) {
	  return false;
	}
      }
      /*      source = m_listenees.get("trainingSet");
      if (source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("trainingSet")) {
	  return false;
	}
        } */
    }

    if (eventName.compareTo("text") == 0) {
      if (!m_listenees.containsKey("trainingSet") &&
	  !m_listenees.containsKey("instance")) {
	return false;
      }
      Object source = m_listenees.get("trainingSet");
      if (source != null && source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("trainingSet")) {
	  return false;
	}
      }
      source = m_listenees.get("instance");
      if (source != null && source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("instance")) {
	  return false;
	}
      }
    }

    if (eventName.compareTo("incrementalClassifier") == 0) {
      /*      if (!(m_Classifier instanceof weka.classifiers.UpdateableClassifier)) {
	return false;
	} */
      if (!m_listenees.containsKey("instance")) {
	return false;
      }
      Object source = m_listenees.get("instance");
      if (source instanceof EventConstraints) {
	if (!((EventConstraints)source).eventGeneratable("instance")) {
	  return false;
	}
      }
    }
    return true;
  }
}
