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
 *    IterativeClassifierOptimizer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Random;

import weka.classifiers.meta.LogitBoost;
import weka.classifiers.IterativeClassifier;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.RevisionUtils;

/**
 * Abstract utility class for handling settings common to meta
 * classifiers that use a single base learner.
 *
 <!-- globalinfo-start -->
 * Optimizes the number of iterations of the given iterative classifier using cross-validation.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F &lt;num&gt;
 *  Number of folds for cross-validation.
 *  (default 10)</pre>
 * 
 * <pre> -R &lt;num&gt;
 *  Number of runs for cross-validation.
 *  (default 1)</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.meta.LogitBoost)</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.meta.LogitBoost:
 * </pre>
 * 
 * <pre> -Q
 *  Use resampling instead of reweighting for boosting.</pre>
 * 
 * <pre> -P &lt;percent&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)</pre>
 * 
 * <pre> -L &lt;num&gt;
 *  Threshold on the improvement of the likelihood.
 *  (default -Double.MAX_VALUE)</pre>
 * 
 * <pre> -H &lt;num&gt;
 *  Shrinkage parameter.
 *  (default 1)</pre>
 * 
 * <pre> -Z &lt;num&gt;
 *  Z max threshold for responses.
 *  (default 3)</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10141 $
 */
public class IterativeClassifierOptimizer extends RandomizableClassifier {

  /** for serialization */
  private static final long serialVersionUID = -3665485256313525864L;

  /** The base classifier to use */
  protected IterativeClassifier m_IterativeClassifier = new LogitBoost();

  /** The number of folds for the cross-validation. */
  protected int m_NumFolds = 10;

  /** The number of runs for the cross-validation. */
  protected int m_NumRuns = 1;

  /** The random number generator used */
  protected Random m_RandomInstance;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Optimizes the number of iterations of the given iterative " +
      "classifier using cross-validation.";
  }

  /**
   * String describing default classifier.
   */
  protected String defaultIterativeClassifierString() {

    return "weka.classifiers.meta.LogitBoost";
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numRunsTipText() {
    return "Number of runs for cross-validation.";
  }
  
  /**
   * Get the value of NumRuns.
   *
   * @return Value of NumRuns.
   */
  public int getNumRuns() {
    
    return m_NumRuns;
  }
  
  /**
   * Set the value of NumRuns.
   *
   * @param newNumRuns Value to assign to NumRuns.
   */
  public void setNumRuns(int newNumRuns) {
    
    m_NumRuns = newNumRuns;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "Number of folds for cross-validation.";
  }
  
  /**
   * Get the value of NumFolds.
   *
   * @return Value of NumFolds.
   */
  public int getNumFolds() {
    
    return m_NumFolds;
  }
  
  /**
   * Set the value of NumFolds.
   *
   * @param newNumFolds Value to assign to NumFolds.
   */
  public void setNumFolds(int newNumFolds) {
    
    m_NumFolds = newNumFolds;
  }

  /**
   * Builds the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_IterativeClassifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }

    // Can classifier handle the data?
    getCapabilities().testWithFail(data);

    // Need a random number generator for data shuffling
    m_RandomInstance = new Random(m_Seed);

    // Need to shuffle the data
    Random randomInstance = new Random(m_Seed);

    // Save reference to original data
    Instances origData = data;

    // Remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
	
    // Initialize datasets and classifiers
    Instances[][] trainingSets = new Instances[m_NumRuns][m_NumFolds];
    Instances[][] testSets = new Instances[m_NumRuns][m_NumFolds];
    IterativeClassifier[][] classifiers = 
      new IterativeClassifier[m_NumRuns][m_NumFolds];
    for (int j = 0; j < m_NumRuns; j++) {
      data.randomize(m_RandomInstance);
      data.stratify(m_NumFolds);
      for (int i = 0; i < m_NumFolds; i++) {
        trainingSets[j][i] = data.trainCV(m_NumFolds, i, randomInstance);
        testSets[j][i] = data.testCV(m_NumFolds, i);
        classifiers[j][i] = (IterativeClassifier)AbstractClassifier.makeCopy(m_IterativeClassifier);
        classifiers[j][i].initializeClassifier(trainingSets[j][i]);
      }
    }

    // Perform evaluation
    int numIts = 0;
    double oldResult = Double.MAX_VALUE;
    while (true) {
      double result = 0;
      for (int r = 0; r < m_NumRuns; r++) {
        for (int i = 0; i < m_NumFolds; i++) {	  
          Evaluation eval = new Evaluation(trainingSets[r][i]);
          eval.evaluateModel(classifiers[r][i], testSets[r][i]);
          result += eval.rootMeanSquaredError();
          if (!classifiers[r][i].next()) {
            break; // Break out if one classifier fails to iterate
          }
        }
      }
      if (m_Debug) {
        System.out.println("Iteration: " + numIts + " " + "Measure: " + result);
      }
      if (result >= oldResult) {
        break; // No improvement
      }
      oldResult = result;
      numIts++;
    }
    classifiers = null;
    trainingSets = null;
    testSets = null;
    data = null;

    // Build classifieer based on identified number of iterations
    m_IterativeClassifier.initializeClassifier(origData);
    int i = 0;
    while (i++ < numIts && m_IterativeClassifier.next()) {};
    m_IterativeClassifier.done();
  }

  /**
   * Returns the class distribution for an instance.
   */
  public double[] distributionForInstance(Instance inst) throws Exception {

    return m_IterativeClassifier.distributionForInstance(inst);
  }

  /**
   * Returns a string describing the classifier.
   */
  public String toString() {

    if (m_IterativeClassifier == null) {
      return "No classifier built yet.";
    } else {
      return m_IterativeClassifier.toString();
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(3);

    newVector.addElement(new Option(
                                    "\tNumber of folds for cross-validation.\n"
                                    +"\t(default 10)",
                                    "F", 1, "-F <num>"));
    newVector.addElement(new Option(
                                    "\tNumber of runs for cross-validation.\n"
                                    +"\t(default 1)",
                                    "R", 1, "-R <num>"));

    newVector.addElement(new Option(
          "\tFull name of base classifier.\n"
          + "\t(default: " + defaultIterativeClassifierString() +")",
          "W", 1, "-W"));
    
    newVector.addAll(Collections.list(super.listOptions()));

    newVector.addElement(new Option(
          "",
          "", 0, "\nOptions specific to classifier "
          + m_IterativeClassifier.getClass().getName() + ":"));
    newVector.addAll(Collections.list(((OptionHandler)m_IterativeClassifier).listOptions()));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of the base learner.<p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    super.setOptions(options);
    
    String numFolds = Utils.getOption('F', options);
    if (numFolds.length() != 0) {
      setNumFolds(Integer.parseInt(numFolds));
    } else {
      setNumFolds(10);
    }
    
    String numRuns = Utils.getOption('R', options);
    if (numRuns.length() != 0) {
      setNumRuns(Integer.parseInt(numRuns));
    } else {
      setNumRuns(1);
    }

    String classifierName = Utils.getOption('W', options);

    if (classifierName.length() > 0) {
      setIterativeClassifier(getIterativeClassifier(classifierName,
                                                    Utils.partitionOptions(options)));
    } else {
      setIterativeClassifier(getIterativeClassifier(defaultIterativeClassifierString(),
                                                    Utils.partitionOptions(options)));
    }
  }

  /**
   * Get classifier for string.
   */
  protected IterativeClassifier getIterativeClassifier(String name,
                                                       String[] options) 
    throws Exception {

    Classifier c = AbstractClassifier.forName(name, options);
    if (c instanceof IterativeClassifier) {
      return (IterativeClassifier)c;
    } else {
      throw new IllegalArgumentException(name + " is not an IterativeClassifier.");
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    Vector<String> options = new Vector<String>();
       
    options.add("-W");
    options.add(getIterativeClassifier().getClass().getName());

    options.add("-F"); options.add("" + getNumFolds());
    options.add("-R"); options.add("" + getNumRuns());
    
    Collections.addAll(options, super.getOptions());
    
    String[] classifierOptions = ((OptionHandler)m_IterativeClassifier).getOptions();
    if (classifierOptions.length > 0) {
      options.add("--");
      Collections.addAll(options, classifierOptions);
    }

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "The iterative classifier to be optimized.";
  }

  /**
   * Returns default capabilities of the base classifier.
   *
   * @return      the capabilities of the base classifier
   */
  public Capabilities getCapabilities() {
    Capabilities        result;

    if (getIterativeClassifier() != null) {
      result = getIterativeClassifier().getCapabilities();
    } else {
      result = new Capabilities(this);
      result.disableAll();
    }

    // set dependencies
    for (Capability cap: Capability.values())
      result.enableDependency(cap);

    result.setOwner(this);

    return result;
  }

  /**
   * Set the base learner.
   *
   * @param newIterativeClassifier the classifier to use.
   */
  public void setIterativeClassifier(IterativeClassifier newIterativeClassifier) {

    m_IterativeClassifier = newIterativeClassifier;
  }

  /**
   * Get the classifier used as the base learner.
   *
   * @return the classifier used as the classifier
   */
  public IterativeClassifier getIterativeClassifier() {

    return m_IterativeClassifier;
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string
   */
  protected String getIterativeClassifierSpec() {

    IterativeClassifier c = getIterativeClassifier();
    return c.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler)c).getOptions());
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10649 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new IterativeClassifierOptimizer(), argv);
  }
}

