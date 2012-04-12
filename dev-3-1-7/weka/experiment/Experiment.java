/*
 *    DatabaseUtils.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.experiment;

import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Option;
import weka.core.Instances;
import weka.core.FastVector;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;
import java.beans.PropertyDescriptor;
import javax.swing.DefaultListModel;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;

import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.IntrospectionException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyEditorManager;
import java.beans.PropertyVetoException;
import java.beans.Beans;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Holds all the necessary configuration information for a standard
 * type experiment. This object is able to be serialized for storage
 * on disk.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class Experiment implements Serializable, OptionHandler {
  
  /** Where results will be sent */
  protected ResultListener m_ResultListener = new CSVResultListener();
  
  /** The result producer */
  protected ResultProducer m_ResultProducer = new RandomSplitResultProducer();

  /** Lower run number */
  protected int m_RunLower = 1;

  /** Upper run number */
  protected int m_RunUpper = 10;

  /** An array of dataset files */
  protected DefaultListModel m_Datasets = new DefaultListModel();

  /** True if the exp should also iterate over a property of the RP */
  protected boolean m_UsePropertyIterator = false;
  
  /** The path to the iterator property */
  protected PropertyNode [] m_PropertyPath;
  
  /** The array of values to set the property to */
  protected Object m_PropertyArray;

  /** User notes about the experiment */
  protected String m_Notes = "";

  /** Method names of additional measures of objects contained in the 
      custom property iterator. Only methods names beginning with "measure"
      and returning doubles are recognised */
  protected String [] m_AdditionalMeasures = null;

  /**
   * Gets whether the custom property iterator should be used.
   *
   * @return true if so
   */
  public boolean getUsePropertyIterator() {
    
    return m_UsePropertyIterator;
  }

  /**
   * Sets whether the custom property iterator should be used.
   *
   * @param newUsePropertyIterator true if so
   */
  public void setUsePropertyIterator(boolean newUsePropertyIterator) {
    
    m_UsePropertyIterator = newUsePropertyIterator;
  }

  /**
   * Gets the path of properties taken to get to the custom property
   * to iterate over.
   *
   * @return an array of PropertyNodes
   */
  public PropertyNode [] getPropertyPath() {
    
    return m_PropertyPath;
  }
  
  /**
   * Sets the path of properties taken to get to the custom property
   * to iterate over.
   *
   * @newPropertyPath an array of PropertyNodes
   */
  public void setPropertyPath(PropertyNode [] newPropertyPath) {
    
    m_PropertyPath = newPropertyPath;
  }
  
  /**
   * Sets the array of values to set the custom property to.
   *
   * @param newPropArray a value of type Object which should be an
   * array of the appropriate values.
   */
  public void setPropertyArray(Object newPropArray) {

    m_PropertyArray = newPropArray;
  }

  /**
   * Gets the array of values to set the custom property to.
   *
   * @return a value of type Object which should be an
   * array of the appropriate values.
   */
  public Object getPropertyArray() {

    return m_PropertyArray;
  }

  /**
   * Gets the number of custom iterator values that have been defined
   * for the experiment.
   *
   * @return the number of custom property iterator values.
   */
  public int getPropertyArrayLength() {

    return Array.getLength(m_PropertyArray);
  }

  /**
   * Gets a specified value from the custom property iterator array.
   *
   * @param index the index of the value wanted
   * @return the property array value
   */
  public Object getPropertyArrayValue(int index) {

    return Array.get(m_PropertyArray, index);
  }
  
  /* These may potentially want to be made un-transient if it is decided
   * that experiments may be saved mid-run and later resumed
   */
  /** The current run number when the experiment is running */
  protected transient int m_RunNumber;
  /** The current dataset number when the experiment is running */
  protected transient int m_DatasetNumber;
  /** The current custom property value index when the experiment is running */
  protected transient int m_PropertyNumber;
  /** True if the experiment has finished running */
  protected transient boolean m_Finished = true;
  /** The dataset currently being used */
  protected transient Instances m_CurrentInstances;
  /** The custom property value that has actually been set */
  protected transient int m_CurrentProperty;

  /**
   * When an experiment is running, this returns the current run number.
   *
   * @return the current run number.
   */
  public int getCurrentRunNumber() {
    return m_RunNumber;
  }

  /**
   * When an experiment is running, this returns the current dataset number.
   *
   * @return the current dataset number.
   */
  public int getCurrentDatasetNumber() {
    return m_DatasetNumber;
  }

  /**
   * When an experiment is running, this returns the index of the
   * current custom property value.
   *
   * @return the index of the current custom property value.
   */
  public int getCurrentPropertyNumber() {
    return m_PropertyNumber;
  }
  
  /**
   * Prepares an experiment for running, initializing current iterator
   * settings.
   *
   * @exception Exception if an error occurs
   */
  public void initialize() throws Exception {
    
    m_RunNumber = getRunLower();
    m_DatasetNumber = 0;
    m_PropertyNumber = 0;
    m_CurrentProperty = -1;
    m_CurrentInstances = null;
    m_Finished = false;
    if (m_UsePropertyIterator && (m_PropertyArray == null)) {
      throw new Exception("Null array for property iterator");
    }
    if (getRunLower() > getRunUpper()) {
      throw new Exception("Lower run number is greater than upper run number");
    }
    if (getDatasets().size() == 0) {
      throw new Exception("No datasets have been specified");
    }
    if (m_ResultProducer == null) {
      throw new Exception("No ResultProducer set");
    }
    if (m_ResultListener == null) {
      throw new Exception("No ResultListener set");
    }

    if (m_UsePropertyIterator && (m_PropertyArray != null)) {
      determineAdditionalResultMeasures();
    }

    m_ResultProducer.setResultListener(m_ResultListener);
    m_ResultProducer.setAdditionalMeasures(m_AdditionalMeasures);
    m_ResultProducer.preProcess();

    // constrain the additional measures to be only those allowable
    // by the ResultListener
    String [] columnConstraints = m_ResultListener.
      determineColumnConstraints(m_ResultProducer);

    if (columnConstraints != null) {
      m_ResultProducer.setAdditionalMeasures(columnConstraints);
    }
  }

  /**
   * Iterate over the objects in the property array to determine what
   * (if any) additional measures they support
   */
  private void determineAdditionalResultMeasures() {
    m_AdditionalMeasures = null;
    FastVector measureNames = new FastVector();
    MethodDescriptor methods[];
    
    for (int i = 0; i < Array.getLength(m_PropertyArray); i++) {
      Object current = Array.get(m_PropertyArray, i);
      try {
	BeanInfo bi = Introspector.getBeanInfo(current.getClass());
	methods = bi.getMethodDescriptors();
      } catch (IntrospectionException ex) {
	System.err.println("determineAdditionalResultMeasures: Couldn't "
			   +"introspect");
	return;
      }

      // look for methods that begin with "measure" indicating that the
      // method returns auxilliary information related to the object
      for (int j=0; j<methods.length; j++) {
	String name = methods[j].getDisplayName();
	Method meth = methods[j].getMethod();
	if (name.startsWith("measure")) {
	
	  if (meth.getReturnType().equals(double.class)) {
	    if (measureNames.indexOf(name) == -1) {
	      measureNames.addElement(name);
	    }
	  }
	}
      }
    }
    if (measureNames.size() > 0) {
      m_AdditionalMeasures = new String [measureNames.size()];
      for (int i=0;i<measureNames.size();i++) {
	m_AdditionalMeasures[i] = (String)measureNames.elementAt(i);
      }
    }
  }

  
  /**
   * Recursively sets the custom property value, by setting all values
   * along the property path.
   *
   * @param propertyDepth the current position along the property path
   * @param origValue the value to set the property to
   * @exception Exception if an error occurs
   */
  protected void setProperty(int propertyDepth, Object origValue)
    throws Exception {
    
    PropertyDescriptor current = m_PropertyPath[propertyDepth].property;
    Object subVal = null;
    if (propertyDepth < m_PropertyPath.length - 1) {
      Method getter = current.getReadMethod();
      Object getArgs [] = { };
      subVal = getter.invoke(origValue, getArgs);
      setProperty(propertyDepth + 1, subVal);
    } else {
      subVal = Array.get(m_PropertyArray, m_PropertyNumber);
    }
    Method setter = current.getWriteMethod();
    Object [] args = { subVal };
    setter.invoke(origValue, args);
  }

  /**
   * Returns true if there are more iterations to carry out in the experiment.
   *
   * @return true if so
   */
  public boolean hasMoreIterations() {

    return !m_Finished;
  }
  
  /**
   * Carries out the next iteration of the experiment.
   *
   * @exception Exception if an error occurs
   */
  public void nextIteration() throws Exception {
    
    if (m_UsePropertyIterator) {
      if (m_CurrentProperty != m_PropertyNumber) {
	setProperty(0, m_ResultProducer);
	m_CurrentProperty = m_PropertyNumber;
      }
    }
    
    if (m_CurrentInstances == null) {
      File currentFile = (File) getDatasets().elementAt(m_DatasetNumber);
      Reader reader = new FileReader(currentFile);
      Instances data = new Instances(new BufferedReader(reader));
      data.setClassIndex(data.numAttributes() - 1);
      m_CurrentInstances = data;
      m_ResultProducer.setInstances(m_CurrentInstances);
    }
    
    m_ResultProducer.doRun(m_RunNumber);

    advanceCounters();
  }

  /**
   * Increments iteration counters appropriately.
   */
  public void advanceCounters() {

    m_RunNumber ++;
    if (m_RunNumber > getRunUpper()) {
      m_RunNumber = getRunLower();
      m_DatasetNumber ++;
      m_CurrentInstances = null;
      if (m_DatasetNumber >= getDatasets().size()) {
	m_DatasetNumber = 0;
	if (m_UsePropertyIterator) {
	  m_PropertyNumber ++;
	  if (m_PropertyNumber >= Array.getLength(m_PropertyArray)) {
	    m_Finished = true;
	  }
	} else {
	  m_Finished = true;
	}
      }
    }
  }

  /**
   * Runs all iterations of the experiment, continuing past errors.
   */
  public void runExperiment() {

    while (hasMoreIterations()) {
      try {
	nextIteration();
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
	advanceCounters(); // Try to keep plowing through
      }
    }
  }

  /**
   * Signals that the experiment is finished running, so that cleanup
   * can be done.
   *
   * @exception Exception if an error occurs
   */
  public void postProcess() throws Exception {

    m_ResultProducer.postProcess();
  }
  
  /**
   * Gets the datasets in the experiment.
   *
   * @return the datasets in the experiment.
   */
  public DefaultListModel getDatasets() {

    return m_Datasets;
  }

  /**
   * Gets the result listener where results will be sent.
   *
   * @return the result listener where results will be sent.
   */
  public ResultListener getResultListener() {
    
    return m_ResultListener;
  }
  
  /**
   * Sets the result listener where results will be sent.
   *
   * @param newResultListener the result listener where results will be sent.
   */
  public void setResultListener(ResultListener newResultListener) {
    
    m_ResultListener = newResultListener;
  }
  
  /**
   * Get the result producer used for the current experiment.
   *
   * @return the result producer used for the current experiment.
   */
  public ResultProducer getResultProducer() {
    
    return m_ResultProducer;
  }
  
  /**
   * Set the result producer used for the current experiment.
   *
   * @param newResultProducer result producer to use for the current 
   * experiment.
   */
  public void setResultProducer(ResultProducer newResultProducer) {
    
    m_ResultProducer = newResultProducer;
  }
  
  /**
   * Get the upper run number for the experiment.
   *
   * @return the upper run number for the experiment.
   */
  public int getRunUpper() {
    
    return m_RunUpper;
  }
  
  /**
   * Set the upper run number for the experiment.
   *
   * @param newRunUpper the upper run number for the experiment.
   */
  public void setRunUpper(int newRunUpper) {
    
    m_RunUpper = newRunUpper;
  }
  
  /**
   * Get the lower run number for the experiment.
   *
   * @return the lower run number for the experiment.
   */
  public int getRunLower() {
    
    return m_RunLower;
  }
  
  /**
   * Set the lower run number for the experiment.
   *
   * @param newRunLower the lower run number for the experiment.
   */
  public void setRunLower(int newRunLower) {
    
    m_RunLower = newRunLower;
  }

  
  /**
   * Get the user notes.
   *
   * @return User notes associated with the experiment.
   */
  public String getNotes() {
    
    return m_Notes;
  }
  
  /**
   * Set the user notes.
   *
   * @param newNotes New user notes.
   */
  public void setNotes(String newNotes) {
    
    m_Notes = newNotes;
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	     "\tThe lower run number to start the experiment from.\n"
	      +"\t(default 1)", 
	     "L", 1, 
	     "-L <num>"));
    newVector.addElement(new Option(
	     "\tThe upper run number to end the experiment at (inclusive).\n"
	      +"\t(default 10)", 
	     "U", 1, 
	     "-U <num>"));
    newVector.addElement(new Option(
	     "\tThe dataset to run the experiment on.\n"
	     + "\t(required, may be specified multiple times)", 
	     "T", 1, 
	     "-T <arff file>"));
    newVector.addElement(new Option(
	     "\tThe full class name of a ResultProducer (required).\n"
	      +"\teg: weka.experiment.RandomSplitResultProducer", 
	     "P", 1, 
	     "-P <class name>"));
    newVector.addElement(new Option(
	     "\tThe full class name of a ResultListener (required).\n"
	      +"\teg: weka.experiment.CSVResultListener", 
	     "D", 1, 
	     "-D <class name>"));
    newVector.addElement(new Option(
	     "\tA string containing any notes about the experiment.\n"
	      +"\t(default none)", 
	     "N", 1, 
	     "-N <string>"));

    if ((m_ResultProducer != null) &&
	(m_ResultProducer instanceof OptionHandler)) {
      newVector.addElement(new Option(
	     "",
	     "", 0, "\nOptions specific to result producer "
	     + m_ResultProducer.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_ResultProducer).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -L num <br>
   * The lower run number to start the experiment from.
   * (default 1) <p>
   *
   * -U num <br>
   * The upper run number to end the experiment at (inclusive).
   * (default 10) <p>
   *
   * -T arff_file <br>
   * The dataset to run the experiment on.
   * (required, may be specified multiple times) <p>
   *
   * -P class_name <br>
   * The full class name of a ResultProducer (required).
   * eg: weka.experiment.RandomSplitResultProducer <p>
   *
   * -D class_name <br>
   * The full class name of a ResultListener (required).
   * eg: weka.experiment.CSVResultListener <p>
   *
   * All option after -- will be passed to the result producer
   * (there is currently no way to pass options to the result listener). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String [] options) throws Exception {

    String lowerString = Utils.getOption('L', options);
    if (lowerString.length() != 0) {
      setRunLower(Integer.parseInt(lowerString));
    } else {
      setRunLower(1);
    }
    String upperString = Utils.getOption('U', options);
    if (upperString.length() != 0) {
      setRunUpper(Integer.parseInt(upperString));
    } else {
      setRunUpper(10);
    }
    if (getRunLower() > getRunUpper()) {
      throw new Exception("Lower (" + getRunLower() 
			  + ") is greater than upper (" 
			  + getRunUpper() + ")");
    }
    
    setNotes(Utils.getOption('N', options));
    
    getDatasets().removeAllElements();
    String dataName;
    do {
      dataName = Utils.getOption('T', options);
      if (dataName.length() != 0) {
	File dataset = new File(dataName);
	getDatasets().addElement(dataset);
      }
    } while (dataName.length() != 0);
    if (getDatasets().size() == 0) {
      throw new Exception("Required: -T <arff file name>");
    }

    String rlName = Utils.getOption('D', options);
    if (rlName.length() == 0) {
      throw new Exception("Required: -D <ResultListener class name>");
    }
    setResultListener((ResultListener)Utils.forName(ResultListener.class,
						    rlName, null));

    String rpName = Utils.getOption('P', options);
    if (rpName.length() == 0) {
      throw new Exception("Required: -P <ResultProducer class name>");
    }
    // Do it first without options, so if an exception is thrown during
    // the option setting, listOptions will contain options for the actual
    // RP.
    setResultProducer((ResultProducer)Utils.forName(
		      ResultProducer.class,
		      rpName,
		      null));
    if (getResultProducer() instanceof OptionHandler) {
      ((OptionHandler) getResultProducer())
	.setOptions(Utils.partitionOptions(options));
    }
  }

  /**
   * Gets the current settings of the experiment iterator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    // Currently no way to set custompropertyiterators from the command line

    m_UsePropertyIterator = false;
    m_PropertyPath = null;
    m_PropertyArray = null;
    
    String [] rpOptions = new String [0];
    if ((m_ResultProducer != null) && 
	(m_ResultProducer instanceof OptionHandler)) {
      rpOptions = ((OptionHandler)m_ResultProducer).getOptions();
    }
    
    String [] options = new String [rpOptions.length 
				   + getDatasets().size() * 2
				   + 11];
    int current = 0;

    options[current++] = "-L"; options[current++] = "" + getRunLower();
    options[current++] = "-U"; options[current++] = "" + getRunUpper();
    if (getDatasets().size() != 0) {
      for (int i = 0; i < getDatasets().size(); i++) {
	options[current++] = "-T";
	options[current++] = getDatasets().elementAt(i).toString();
      }
    }
    if (getResultListener() != null) {
      options[current++] = "-D";
      options[current++] = getResultListener().getClass().getName();
    }
    if (getResultProducer() != null) {
      options[current++] = "-P";
      options[current++] = getResultProducer().getClass().getName();
    }
    if (!getNotes().equals("")) {
      options[current++] = "-N"; options[current++] = getNotes();
    }
    options[current++] = "--";

    System.arraycopy(rpOptions, 0, options, current, 
		     rpOptions.length);
    current += rpOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets a string representation of the experiment configuration.
   *
   * @return a value of type 'String'
   */
  public String toString() {

    String result = "Runs from: " + m_RunLower + " to: " + m_RunUpper + '\n';
    result += "Datasets:";
    for (int i = 0; i < m_Datasets.size(); i ++) {
      result += " " + m_Datasets.elementAt(i);
    }
    result += '\n';
    result += "Custom property iterator: "
      + (m_UsePropertyIterator ? "on" : "off")
      + "\n";
    if (m_UsePropertyIterator) {
      if (m_PropertyPath == null) {
	throw new Error("*** null propertyPath ***");
      }
      if (m_PropertyArray == null) {
	throw new Error("*** null propertyArray ***");
      }
      if (m_PropertyPath.length > 1) {
	result += "Custom property path:\n";
	for (int i = 0; i < m_PropertyPath.length - 1; i++) {
	  PropertyNode pn = m_PropertyPath[i];
	  result += "" + (i + 1) + "  " + pn.parentClass.getName()
	    + "::" + pn.toString()
	    + ' ' + pn.value.toString() + '\n';
	}
      }
      result += "Custom property name:"
	+ m_PropertyPath[m_PropertyPath.length - 1].toString() + '\n';
      result += "Custom property values:\n";
      for (int i = 0; i < Array.getLength(m_PropertyArray); i++) {
	Object current = Array.get(m_PropertyArray, i);
	result += " " + (i + 1)
	  + " " + current.getClass().getName()
	  + " " + current.toString() + '\n';
      }
    }
    result += "ResultProducer: " + m_ResultProducer + '\n';
    result += "ResultListener: " + m_ResultListener + '\n';
    if (!getNotes().equals("")) {
      result += "Notes: " + getNotes();
    }
    return result;
  }

  /**
   * Configures/Runs the Experiment from the command line.
   *
   * @param args command line arguments to the Experiment.
   */
  public static void main(String[] args) {

    try {
      Experiment exp = null;
      String expFile = Utils.getOption('l', args);
      String saveFile = Utils.getOption('s', args);
      boolean runExp = Utils.getFlag('r', args);
      if (expFile.length() == 0) {
	exp = new Experiment();
	try {
	  exp.setOptions(args);
	  Utils.checkForRemainingOptions(args);
	} catch (Exception ex) {
	  ex.printStackTrace();
	  String result = "Usage:\n\n"
	    + "-l <exp file>\n"
	    + "\tLoad experiment from file (default use cli options)\n"
	    + "-s <exp file>\n"
	    + "\tSave experiment to file after setting other options\n"
	    + "\t(default don't save)\n"
	    + "-r\n"
	    + "\tRun experiment (default don't run)\n\n";
	  Enumeration enum = ((OptionHandler)exp).listOptions();
	  while (enum.hasMoreElements()) {
	    Option option = (Option) enum.nextElement();
	    result += option.synopsis() + "\n";
	    result += option.description() + "\n";
	  }
	  throw new Exception(result + "\n" + ex.getMessage());
	}
      } else {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	exp = (Experiment)oi.readObject();
	oi.close();
      }
      System.err.println("Experiment:\n" + exp.toString());

      if (saveFile.length() != 0) {
	FileOutputStream fo = new FileOutputStream(saveFile);
	ObjectOutputStream oo = new ObjectOutputStream(
				new BufferedOutputStream(fo));
	oo.writeObject(exp);
	oo.close();
      }
      
      if (runExp) {
	System.err.println("Initializing...");
	exp.initialize();
	System.err.println("Iterating...");
	exp.runExperiment();
	System.err.println("Postprocessing...");
	exp.postProcess();
      }
      
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
} // Experiment
