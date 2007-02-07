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
 *    CrossValidationFoldMaker.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Instances;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Bean for splitting instances into training ant test sets according to
 * a cross validation
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.8 $
 */
public class CrossValidationFoldMaker 
  extends AbstractTrainAndTestSetProducer
  implements DataSourceListener, TrainingSetListener, TestSetListener, 
	     UserRequestAcceptor, EventConstraints, Serializable {

  /** for serialization */
  private static final long serialVersionUID = -6350179298851891512L;

  private int m_numFolds = 10;
  private int m_randomSeed = 1;

  private Thread m_foldThread = null;

  public CrossValidationFoldMaker() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
		       +"CrossValidationFoldMaker.gif",
		       BeanVisual.ICON_PATH
		       +"CrossValidationFoldMaker_animated.gif");
    m_visual.setText("CrossValidationFoldMaker");
  }

  /**
   * Global info for this bean
   *
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Split an incoming data set into cross validation folds. "
      +"Separate train and test sets are produced for each of the k folds.";
  }

  /**
   * Accept a training set
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  public void acceptTrainingSet(TrainingSetEvent e) {
    Instances trainingSet = e.getTrainingSet();
    DataSetEvent dse = new DataSetEvent(this, trainingSet);
    acceptDataSet(dse);
  }

  /**
   * Accept a test set
   *
   * @param e a <code>TestSetEvent</code> value
   */
  public void acceptTestSet(TestSetEvent e) {
    Instances testSet = e.getTestSet();
    DataSetEvent dse = new DataSetEvent(this, testSet);
    acceptDataSet(dse);
  }

  /**
   * Accept a data set
   *
   * @param e a <code>DataSetEvent</code> value
   */
  public void acceptDataSet(DataSetEvent e) {
    if (e.isStructureOnly()) {
      // Pass on structure to training and test set listeners
      TrainingSetEvent tse = new TrainingSetEvent(this, e.getDataSet());
      TestSetEvent tsee = new TestSetEvent(this, e.getDataSet());
      notifyTrainingSetProduced(tse);
      notifyTestSetProduced(tsee);
      return;
    }
    if (m_foldThread == null) {
      final Instances dataSet = new Instances(e.getDataSet());
      m_foldThread = new Thread() {
	  public void run() {
	    try {
	      Random random = new Random(getSeed());
	      dataSet.randomize(random);
	      if (dataSet.classIndex() >= 0 && 
		  dataSet.attribute(dataSet.classIndex()).isNominal()) {
		dataSet.stratify(getFolds());
		if (m_logger != null) {
		  m_logger.logMessage("CrossValidationFoldMaker : "
				      +"stratifying data");
		}
	      }
	      
	      for (int i = 0; i < getFolds(); i++) {
		if (m_foldThread == null) {
		  if (m_logger != null) {
		    m_logger.logMessage("Cross validation has been canceled!");
		    m_logger.statusMessage("OK");
		  }
		  // exit gracefully
		  break;
		}
		Instances train = dataSet.trainCV(getFolds(), i, random);
		Instances test  = dataSet.testCV(getFolds(), i);
		
		// inform all training set listeners
		TrainingSetEvent tse = new TrainingSetEvent(this, train);
		tse.m_setNumber = i+1; tse.m_maxSetNumber = getFolds();
		if (m_foldThread != null) {
		  //		  System.err.println("--Just before notify training set");
		  notifyTrainingSetProduced(tse);
		  //		  System.err.println("---Just after notify");
		}
	      
		// inform all test set listeners
		TestSetEvent teste = new TestSetEvent(this, test);
		teste.m_setNumber = i+1; teste.m_maxSetNumber = getFolds();
		if (m_foldThread != null) {
		  notifyTestSetProduced(teste);
		}
	      }
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    } finally {
	      if (isInterrupted()) {
		System.err.println("Cross validation interrupted");
	      }
	      block(false);
	    }
	  }
	};
      m_foldThread.setPriority(Thread.MIN_PRIORITY);
      m_foldThread.start();

      //      if (m_foldThread.isAlive()) {
      block(true);
	//      }
      m_foldThread = null;
    }
  }


  /**
   * Notify all test set listeners of a TestSet event
   *
   * @param tse a <code>TestSetEvent</code> value
   */
  private void notifyTestSetProduced(TestSetEvent tse) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_testListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	//	System.err.println("Notifying test listeners "
	//			   +"(cross validation fold maker)");
	((TestSetListener)l.elementAt(i)).acceptTestSet(tse);
      }
    }
  }

  /**
   * Notify all listeners of a TrainingSet event
   *
   * @param tse a <code>TrainingSetEvent</code> value
   */
  protected void notifyTrainingSetProduced(TrainingSetEvent tse) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_trainingListeners.clone();
    }
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	//	System.err.println("Notifying training listeners "
	//			   +"(cross validation fold maker)");
	((TrainingSetListener)l.elementAt(i)).acceptTrainingSet(tse);
      }
    }
  }

  /**
   * Set the number of folds for the cross validation
   *
   * @param numFolds an <code>int</code> value
   */
  public void setFolds(int numFolds) {
    m_numFolds = numFolds;
  }
  
  /**
   * Get the currently set number of folds
   *
   * @return an <code>int</code> value
   */
  public int getFolds() {
    return m_numFolds;
  }

  /**
   * Tip text for this property
   *
   * @return a <code>String</code> value
   */
  public String foldsTipText() {
    return "The number of train and test splits to produce";
  }
    
  /**
   * Set the seed
   *
   * @param randomSeed an <code>int</code> value
   */
  public void setSeed(int randomSeed) {
    m_randomSeed = randomSeed;
  }
  
  /**
   * Get the currently set seed
   *
   * @return an <code>int</code> value
   */
  public int getSeed() {
    return m_randomSeed;
  }
  
  /**
   * Tip text for this property
   *
   * @return a <code>String</code> value
   */
  public String seedTipText() {
    return "The randomization seed";
  }

  /**
   * Stop any action
   */
  public void stop() {
    // tell the listenee (upstream bean) to stop
    if (m_listenee instanceof BeanCommon) {
      System.err.println("Listener is BeanCommon");
      ((BeanCommon)m_listenee).stop();
    }

    // stop the fold thread
    if (m_foldThread != null) {
      //      m_buildThread.interrupt();
      Thread temp = m_foldThread;
      //      m_buildThread.stop();
      m_foldThread = null;
      temp.interrupt();
    }
  }

  /**
   * Function used to stop code that calls acceptDataSet. This is 
   * needed as cross validation is performed inside a separate
   * thread of execution.
   *
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {
    if (tf) {
      try {
	// make sure the thread is still running before we block
	if (m_foldThread.isAlive()) {
	  wait();
	}
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  /**
   * Return an enumeration of user requests
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_foldThread != null) {
      newVector.addElement("Stop");
    }
    return newVector.elements();
  }

  /**
   * Perform the named request
   *
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Stop") == 0) {
      stop();
    } else {
      throw new IllegalArgumentException(request
					 + " not supported (CrossValidation)");
    }
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
    if (m_listenee == null) {
      return false;
    }
    
    if (m_listenee instanceof EventConstraints) {
      if (((EventConstraints)m_listenee).eventGeneratable("dataSet") ||
	  ((EventConstraints)m_listenee).eventGeneratable("trainingSet") ||
	  ((EventConstraints)m_listenee).eventGeneratable("testSet")) {
	return true;
      } else {
	return false;
      }
    }
    return true;
  }
}
