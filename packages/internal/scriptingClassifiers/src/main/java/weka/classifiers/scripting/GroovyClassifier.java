/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    GroovyClassifier.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.scripting;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.scripting.Groovy;

/**
 * <!-- globalinfo-start --> A wrapper class for Groovy code. Even though the
 * classifier is serializable, the trained classifier cannot be stored
 * persistently. I.e., one cannot store a model file and re-load it at a later
 * point in time again to make predictions.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -G &lt;filename&gt;
 *  The Groovy module to load (full path)
 *  Options after '--' will be passed on to the Groovy module.
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after "--" will be passed on to the Groovy module.
 * <p/>
 * In order to use <a href="http://groovy.codehaus.org/"
 * target="_blank">Groovy</a>, the jar containing all the classes must be
 * present in the CLASSPATH. This jar is normally found in the <i>embeddable</i>
 * sub-directory of the Groovy installation.
 * <p/>
 * Tested with Groovy 1.5.7.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see Groovy
 */
public class GroovyClassifier extends AbstractClassifier {

  /** for serialization. */
  private static final long serialVersionUID = -9078371491735496175L;

  /** the Groovy module. */
  protected File m_GroovyModule = new File(System.getProperty("user.dir"));

  /** the options for the Groovy module. */
  protected String[] m_GroovyOptions = new String[0];

  /** the loaded Groovy object. */
  transient protected Classifier m_GroovyObject = null;

  /**
   * default constructor.
   */
  public GroovyClassifier() {
    super();
  }

  /**
   * Returns a string describing classifier.
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "A wrapper class for Groovy code. Even though the classifier is "
      + "serializable, the trained classifier cannot be stored persistently. "
      + "I.e., one cannot store a model file and re-load it at a later point "
      + "in time again to make predictions.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe Groovy module to load (full path)\n"
      + "\tOptions after '--' will be passed on to the Groovy module.", "G", 1,
      "-G <filename>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    m_GroovyOptions = new String[0];

    tmpStr = Utils.getOption('G', options);
    if (tmpStr.length() != 0) {
      setGroovyModule(new File(tmpStr));
    } else {
      setGroovyModule(new File(System.getProperty("user.dir")));
    }

    setGroovyOptions(Utils.joinOptions(Utils.partitionOptions(options).clone()));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-G");
    result.add("" + getGroovyModule().getAbsolutePath());

    Collections.addAll(result, super.getOptions());

    if (m_GroovyOptions.length > 0) {
      String[] options = m_GroovyOptions;
      result.add("--");
      for (String option : options) {
        result.add(option);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String GroovyModuleTipText() {
    return "The Groovy module to load and execute.";
  }

  /**
   * Sets the Groovy module.
   * 
   * @param value the Groovy module
   */
  public void setGroovyModule(File value) {
    m_GroovyModule = value;
    initGroovyObject();
  }

  /**
   * Gets the Groovy module.
   * 
   * @return the Groovy module
   */
  public File getGroovyModule() {
    return m_GroovyModule;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String GroovyOptionsTipText() {
    return "The options for the Groovy module.";
  }

  /**
   * Sets the Groovy module options.
   * 
   * @param value the options
   */
  public void setGroovyOptions(String value) {
    try {
      m_GroovyOptions = Utils.splitOptions(value).clone();
      initGroovyObject();
    } catch (Exception e) {
      m_GroovyOptions = new String[0];
      e.printStackTrace();
    }
  }

  /**
   * Gets the Groovy module options.
   * 
   * @return the options
   */
  public String getGroovyOptions() {
    return Utils.joinOptions(m_GroovyOptions);
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (m_GroovyObject == null) {
      result = new Capabilities(this);
      result.disableAll();
    } else {
      result = m_GroovyObject.getCapabilities();
    }

    result.enableAllAttributeDependencies();
    result.enableAllClassDependencies();

    return result;
  }

  /**
   * tries to initialize the groovy object and set its options.
   */
  protected void initGroovyObject() {
    try {
      if (m_GroovyModule.isFile()) {
        m_GroovyObject = (Classifier) Groovy.newInstance(m_GroovyModule,
          Classifier.class);
      } else {
        m_GroovyObject = null;
      }

      if (m_GroovyObject != null) {
        ((OptionHandler) m_GroovyObject).setOptions(m_GroovyOptions.clone());
      }
    } catch (Exception e) {
      m_GroovyObject = null;
      e.printStackTrace();
    }
  }

  /**
   * Generates the classifier.
   * 
   * @param instances set of instances serving as training data
   * @throws Exception if the classifier has not been generated successfully
   */
  @Override
  public void buildClassifier(Instances instances) throws Exception {
      //    if (!Groovy.isPresent()) {
      //      throw new Exception("Groovy classes not in CLASSPATH!");
      //    }

    // try loading the module
    initGroovyObject();

    // build the model
    if (m_GroovyObject != null) {
      m_GroovyObject.buildClassifier(instances);
    } else {
      System.err.println("buildClassifier: No Groovy object present!");
    }
  }

  /**
   * Classifies a given instance.
   * 
   * @param instance the instance to be classified
   * @return index of the predicted class
   * @throws Exception if an error occurred during the prediction
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {
    if (m_GroovyObject != null) {
      return m_GroovyObject.classifyInstance(instance);
    } else {
      return Utils.missingValue();
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   * 
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if class is numeric
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {
    if (m_GroovyObject != null) {
      return m_GroovyObject.distributionForInstance(instance);
    } else {
      return new double[instance.numClasses()];
    }
  }

  /**
   * Returns a description of the classifier.
   * 
   * @return a description of the classifier as a string.
   */
  @Override
  public String toString() {
    if (m_GroovyObject != null) {
      return m_GroovyObject.toString();
    } else {
      return "No Groovy module loaded.";
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    runClassifier(new GroovyClassifier(), args);
  }
}
