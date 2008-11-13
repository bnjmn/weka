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
 *    JythonClassifier.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Jython;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A wrapper class for Jython code. Even though the classifier is serializable, the trained classifier cannot be stored persistently. I.e., one cannot store a model file and re-load it at a later point in time again to make predictions.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -J &lt;filename&gt;
 *  The Jython module to load (full path)
 *  Options after '--' will be passed on to the Jython module.</pre>
 * 
 * <pre> -P &lt;directory[,directory,...]&gt;
 *  The paths to add to 'sys.path' (comma-separated list).</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * Options after "--" will be passed onto the Jython module.
 * <p/>
 * Partially based on <a href="http://wiki.python.org/jython/JythonMonthly/Articles/September2006/1" target="_blank">this</a>
 * code.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class JythonClassifier 
  extends Classifier {

  /** for serialization */
  private static final long serialVersionUID = -9078371491735496175L;
  
  /** the Jython module */
  protected File m_JythonModule = new File(System.getProperty("user.dir"));

  /** the options for the Jython module */
  protected String[] m_JythonOptions = new String[0];

  /** additional paths for the Jython module (will be added to "sys.path") */
  protected File[] m_JythonPaths = new File[0];

  /** the loaded Jython object */
  transient protected Classifier m_JythonObject = null;

  /**
   * default constructor
   */
  public JythonClassifier() {
    super();
  }
  
  /**
   * Returns a string describing classifier
   * 
   * @return 		a description suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "A wrapper class for Jython code. Even though the classifier is "
      + "serializable, the trained classifier cannot be stored persistently. "
      + "I.e., one cannot store a model file and re-load it at a later point "
      + "in time again to make predictions.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
	"\tThe Jython module to load (full path)\n"
	+ "\tOptions after '--' will be passed on to the Jython module.",
	"J", 1, "-J <filename>"));

    result.addElement(new Option(
	"\tThe paths to add to 'sys.path' (comma-separated list).",
	"P", 1, "-P <directory[,directory,...]>"));

    Enumeration en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * 
   * @param options 	the list of options as an array of strings
   * @throws Exception 	if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String		tmpStr;

    m_JythonOptions = new String[0];

    setJythonPaths(Utils.getOption('P', options));

    tmpStr = Utils.getOption('J', options);
    if (tmpStr.length() != 0)
      setJythonModule(new File(tmpStr));
    else
      setJythonModule(new File(System.getProperty("user.dir")));

    setJythonOptions(Utils.joinOptions(Utils.partitionOptions(options).clone()));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return 		an array of strings suitable for passing to 
   * 			setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result = new Vector<String>();

    if (getJythonPaths().length() > 0) {
      result.add("-P");
      result.add("" + getJythonPaths());
    }

    result.add("-J");
    result.add("" + getJythonModule().getAbsolutePath());

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (m_JythonOptions.length > 0) {
      options = m_JythonOptions;
      result.add("--");
      for (i = 0; i < options.length; i++)
	result.add(options[i]);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String jythonModuleTipText() {
    return "The Jython module to load and execute.";
  }

  /**
   * Sets the Jython module.
   *
   * @param value 	the Jython module
   */
  public void setJythonModule(File value) {
    m_JythonModule = value;
    initJythonObject();
  }

  /**
   * Gets the Jython module.
   *
   * @return 		the Jython module
   */
  public File getJythonModule() {
    return m_JythonModule;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String jythonOptionsTipText() {
    return "The options for the Jython module.";
  }

  /**
   * Sets the Jython module options.
   *
   * @param value 	the options
   */
  public void setJythonOptions(String value) {
    try {
      m_JythonOptions = Utils.splitOptions(value).clone();
      initJythonObject();
    }
    catch (Exception e) {
      m_JythonOptions = new String[0];
      e.printStackTrace();
    }
  }

  /**
   * Gets the Jython module options.
   *
   * @return 		the options
   */
  public String getJythonOptions() {
    return Utils.joinOptions(m_JythonOptions);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String jythonPathsTipText() {
    return "Comma-separated list of additional paths that get added to 'sys.path'.";
  }

  /**
   * Sets the additional Jython paths.
   *
   * @param value 	the paths (comma-separated list)
   */
  public void setJythonPaths(String value) {
    String[]	paths;
    int		i;

    if (value.length() == 0) {
      m_JythonPaths = new File[0];
    }
    else {
      paths = value.split(",");
      m_JythonPaths = new File[paths.length];
      for (i = 0; i < m_JythonPaths.length; i++)
	m_JythonPaths[i] = new File(paths[i]);
    }
  }

  /**
   * Gets the additional Jython paths.
   *
   * @return 		the paths
   */
  public String getJythonPaths() {
    String	result;
    int		i;

    result = "";

    for (i = 0; i < m_JythonPaths.length; i++) {
      if (i > 0)
	result += ",";
      result += m_JythonPaths[i].getAbsolutePath();
    }

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return		the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities	result;

    if (m_JythonObject == null)
      result = new Capabilities(this);
    else
      result = m_JythonObject.getCapabilities();

    result.enableAllAttributeDependencies();
    result.enableAllClassDependencies();

    return result;
  }

  /**
   * tries to initialize the python object and set its options
   */
  protected void initJythonObject() {
    try {
      if (m_JythonModule.isFile())
	m_JythonObject = (Classifier) Jython.newInstance(m_JythonModule, Classifier.class, m_JythonPaths);
      else
	m_JythonObject = null;
      
      if (m_JythonObject != null)
	m_JythonObject.setOptions(m_JythonOptions.clone());
    }
    catch (Exception e) {
      m_JythonObject = null;
      e.printStackTrace();
    }
  }

  /**
   * Generates the classifier.
   *
   * @param instances 	set of instances serving as training data 
   * @throws Exception	if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    if (!Jython.isPresent())
      throw new Exception("Jython classes not in CLASSPATH!");

    // try loading the module
    initJythonObject();

    // build the model
    if (m_JythonObject != null)
      m_JythonObject.buildClassifier(instances);
    else
      System.err.println("buildClassifier: No Jython object present!");
  }

  /**
   * Classifies a given instance.
   *
   * @param instance	the instance to be classified
   * @return 		index of the predicted class
   * @throws Exception 	if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {
    if (m_JythonObject != null)
      return m_JythonObject.classifyInstance(instance);
    else
      return Instance.missingValue();
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance	the instance to be classified
   * @return 		predicted class probability distribution
   * @throws Exception	if class is numeric
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    if (m_JythonObject != null)
      return m_JythonObject.distributionForInstance(instance);
    else
      return new double[instance.numClasses()];
  }

  /**
   * Returns a description of the classifier.
   *
   * @return 		a description of the classifier as a string.
   */
  public String toString() {
    if (m_JythonObject != null)
      return m_JythonObject.toString();
    else
      return "No Jython module loaded.";
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param args 	the options
   */
  public static void main(String [] args) {
    runClassifier(new JythonClassifier(), args);
  }
}
