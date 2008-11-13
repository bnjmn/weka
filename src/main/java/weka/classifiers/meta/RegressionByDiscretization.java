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
 *    RegressionByDiscretization.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.SingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A regression scheme that employs any classifier on a copy of the data that has the class attribute (equal-width) discretized. The predicted value is the expected value of the mean class value for each discretized interval (based on the predicted probabilities for each interval).
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -B &lt;int&gt;
 *  Number of bins for equal-width discretization
 *  (default 10).
 * </pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RegressionByDiscretization 
  extends SingleClassifierEnhancer {
  
  /** for serialization */
  static final long serialVersionUID = 5066426153134050378L;
  
  /** The discretization filter. */
  protected Discretize m_Discretizer = new Discretize();

  /** The number of discretization intervals. */
  protected int m_NumBins = 10;

  /** The mean values for each Discretized class interval. */
  protected double [] m_ClassMeans;

  /** Whether to delete empty intervals. */
  protected boolean m_DeleteEmptyBins;

  /** Header of discretized data. */
  protected Instances m_DiscretizedHeader = null;

  /** Use equal-frequency binning */
  protected boolean m_UseEqualFrequency = false;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A regression scheme that employs any "
      + "classifier on a copy of the data that has the class attribute (equal-width) "
      + "discretized. The predicted value is the expected value of the "
      + "mean class value for each discretized interval (based on the "
      + "predicted probabilities for each interval).";
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Default constructor.
   */
  public RegressionByDiscretization() {

    m_Classifier = new weka.classifiers.trees.J48();
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    // Discretize the training data
    m_Discretizer.setIgnoreClass(true);
    m_Discretizer.setAttributeIndices("" + (instances.classIndex() + 1));
    m_Discretizer.setBins(getNumBins());
    m_Discretizer.setUseEqualFrequency(getUseEqualFrequency());
    m_Discretizer.setInputFormat(instances);
    Instances newTrain = Filter.useFilter(instances, m_Discretizer);

    // Should empty bins be deleted?
    if (m_DeleteEmptyBins) {

      // Figure out which classes are empty after discretization
      int numNonEmptyClasses = 0;
      boolean[] notEmptyClass = new boolean[newTrain.numClasses()];
      for (int i = 0; i < newTrain.numInstances(); i++) {
        if (!notEmptyClass[(int)newTrain.instance(i).classValue()]) {
          numNonEmptyClasses++;
          notEmptyClass[(int)newTrain.instance(i).classValue()] = true;
        }
      }
      
      // Compute new list of non-empty classes and mapping of indices
      FastVector newClassVals = new FastVector(numNonEmptyClasses);
      int[] oldIndexToNewIndex = new int[newTrain.numClasses()];
      for (int i = 0; i < newTrain.numClasses(); i++) {
        if (notEmptyClass[i]) {
          oldIndexToNewIndex[i] = newClassVals.size();
          newClassVals.addElement(newTrain.classAttribute().value(i));
        }
      }
      
      // Compute new header information
      Attribute newClass = new Attribute(newTrain.classAttribute().name(), 
                                         newClassVals);
      FastVector newAttributes = new FastVector(newTrain.numAttributes());
      for (int i = 0; i < newTrain.numAttributes(); i++) {
        if (i != newTrain.classIndex()) {
          newAttributes.addElement(newTrain.attribute(i).copy());
        } else {
          newAttributes.addElement(newClass);
        }
      }
      
      // Create new header and modify instances
      Instances newTrainTransformed = new Instances(newTrain.relationName(), 
                                                    newAttributes,
                                                    newTrain.numInstances());
      newTrainTransformed.setClassIndex(newTrain.classIndex());
      for (int i = 0; i < newTrain.numInstances(); i++) {
        Instance inst = newTrain.instance(i);
        newTrainTransformed.add(inst);
        newTrainTransformed.lastInstance().
          setClassValue(oldIndexToNewIndex[(int)inst.classValue()]);
      }
      newTrain = newTrainTransformed;
    }
    m_DiscretizedHeader = new Instances(newTrain, 0);

    int numClasses = newTrain.numClasses();

    // Calculate the mean value for each bin of the new class attribute
    m_ClassMeans = new double [numClasses];
    int [] classCounts = new int [numClasses];
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = newTrain.instance(i);
      if (!inst.classIsMissing()) {
	int classVal = (int) inst.classValue();
	classCounts[classVal]++;
	m_ClassMeans[classVal] += instances.instance(i).classValue();
      }
    }

    for (int i = 0; i < numClasses; i++) {
      if (classCounts[i] > 0) {
	m_ClassMeans[i] /= classCounts[i];
      }
    }

    if (m_Debug) {
      System.out.println("Bin Means");
      System.out.println("==========");
      for (int i = 0; i < m_ClassMeans.length; i++) {
	System.out.println(m_ClassMeans[i]);
      }
      System.out.println();
    }

    // Train the sub-classifier
    m_Classifier.buildClassifier(newTrain);
  }

  /**
   * Returns a predicted class for the test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class value
   * @throws Exception if the prediction couldn't be made
   */
  public double classifyInstance(Instance instance) throws Exception {  

    // Make sure structure of class attribute correct
    Instance newInstance = (Instance)instance.copy();
    newInstance.setDataset(m_DiscretizedHeader);
    double [] probs = m_Classifier.distributionForInstance(newInstance);

    // Copmute actual prediction
    double prediction = 0, probSum = 0;
    for (int j = 0; j < probs.length; j++) {
      prediction += probs[j] * m_ClassMeans[j];
      probSum += probs[j];
    }
    
    return prediction /  probSum;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	      "\tNumber of bins for equal-width discretization\n"
	      + "\t(default 10).\n",
	      "B", 1, "-B <int>"));

    newVector.addElement(new Option(
	      "\tWhether to delete empty bins after discretization\n"
	      + "\t(default false).\n",
	      "E", 0, "-E"));
    
    newVector.addElement(new Option(
	     "\tUse equal-frequency instead of equal-width discretization.",
	     "F", 0, "-F"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-end -->
   * -D <br>
   * Produce debugging output. <p>
   *
   * -W classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier
   * (default: weka.classifiers.rules.ZeroR).<p>
   *
   * -B int <br>
   * Number of bins for equal-width discretization (default 10).<p>
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String binsString = Utils.getOption('B', options);
    if (binsString.length() != 0) {
      setNumBins(Integer.parseInt(binsString));
    } else {
      setNumBins(10);
    }

    setDeleteEmptyBins(Utils.getFlag('E', options));
    setUseEqualFrequency(Utils.getFlag('F', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 4];
    int current = 0;

    options[current++] = "-B";
    options[current++] = "" + getNumBins();

    if (getDeleteEmptyBins()) {
      options[current++] = "-E";
    }
    
    if (getUseEqualFrequency()) {
      options[current++] = "-F";
    }

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numBinsTipText() {

    return "Number of bins for discretization.";
  }

  /**
   * Gets the number of bins numeric attributes will be divided into
   *
   * @return the number of bins.
   */
  public int getNumBins() {

    return m_NumBins;
  }

  /**
   * Sets the number of bins to divide each selected numeric attribute into
   *
   * @param numBins the number of bins
   */
  public void setNumBins(int numBins) {

    m_NumBins = numBins;
  }


  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String deleteEmptyBinsTipText() {

    return "Whether to delete empty bins after discretization.";
  }


  /**
   * Gets the number of bins numeric attributes will be divided into
   *
   * @return the number of bins.
   */
  public boolean getDeleteEmptyBins() {

    return m_DeleteEmptyBins;
  }

  /**
   * Sets the number of bins to divide each selected numeric attribute into
   *
   * @param numBins the number of bins
   */
  public void setDeleteEmptyBins(boolean b) {

    m_DeleteEmptyBins = b;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useEqualFrequencyTipText() {

    return "If set to true, equal-frequency binning will be used instead of" +
      " equal-width binning.";
  }
  
  /**
   * Get the value of UseEqualFrequency.
   *
   * @return Value of UseEqualFrequency.
   */
  public boolean getUseEqualFrequency() {
    
    return m_UseEqualFrequency;
  }
  
  /**
   * Set the value of UseEqualFrequency.
   *
   * @param newUseEqualFrequency Value to assign to UseEqualFrequency.
   */
  public void setUseEqualFrequency(boolean newUseEqualFrequency) {
    
    m_UseEqualFrequency = newUseEqualFrequency;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    text.append("Regression by discretization");
    if (m_ClassMeans == null) {
      text.append(": No model built yet.");
    } else {
      text.append("\n\nClass attribute discretized into " 
		  + m_ClassMeans.length + " values\n");

      text.append("\nClassifier spec: " + getClassifierSpec() 
		  + "\n");
      text.append(m_Classifier.toString());
    }
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
 
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new RegressionByDiscretization(), argv);
  }
}
