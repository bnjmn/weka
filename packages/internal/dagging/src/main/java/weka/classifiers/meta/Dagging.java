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
 * Dagging.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> This meta classifier creates a number of disjoint,
 * stratified folds out of the data and feeds each chunk of data to a copy of
 * the supplied base classifier. Predictions are made via majority vote, since
 * all the generated base classifiers are put into the Vote meta classifier. <br/>
 * Useful for base classifiers that are quadratic or worse in time behavior,
 * regarding number of instances in the training data. <br/>
 * <br/>
 * For more information, see: <br/>
 * Ting, K. M., Witten, I. H.: Stacking Bagged and Dagged Models. In: Fourteenth
 * international Conference on Machine Learning, San Francisco, CA, 367-375,
 * 1997.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Ting1997,
 *    address = {San Francisco, CA},
 *    author = {Ting, K. M. and Witten, I. H.},
 *    booktitle = {Fourteenth international Conference on Machine Learning},
 *    editor = {D. H. Fisher},
 *    pages = {367-375},
 *    publisher = {Morgan Kaufmann Publishers},
 *    title = {Stacking Bagged and Dagged Models},
 *    year = {1997}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -F &lt;folds&gt;
 *  The number of folds for splitting the training set into
 *  smaller chunks for the base classifier.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -verbose
 *  Whether to print some more information during building the
 *  classifier.
 *  (default is off)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated classifier.
 * <p/>
 * 
 * @author Bernhard Pfahringer (bernhard at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see Vote
 */
public class Dagging extends RandomizableSingleClassifierEnhancer implements
  TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 4560165876570074309L;

  /** the number of folds to use to split the training data */
  protected int m_NumFolds = 10;

  /** the classifier used for voting */
  protected Vote m_Vote = null;

  /** whether to output some progress information during building */
  protected boolean m_Verbose = false;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "This meta classifier creates a number of disjoint, stratified folds out "
      + "of the data and feeds each chunk of data to a copy of the supplied "
      + "base classifier. Predictions are made via averaging, since all the "
      + "generated base classifiers are put into the Vote meta classifier. \n"
      + "Useful for base classifiers that are quadratic or worse in time "
      + "behavior, regarding number of instances in the training data. \n"
      + "\n"
      + "For more information, see: \n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Ting, K. M. and Witten, I. H.");
    result.setValue(Field.TITLE, "Stacking Bagged and Dagged Models");
    result.setValue(Field.BOOKTITLE,
      "Fourteenth international Conference on Machine Learning");
    result.setValue(Field.EDITOR, "D. H. Fisher");
    result.setValue(Field.YEAR, "1997");
    result.setValue(Field.PAGES, "367-375");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann Publishers");
    result.setValue(Field.ADDRESS, "San Francisco, CA");

    return result;
  }

  /**
   * Constructor.
   */
  public Dagging() {
    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {
    return weka.classifiers.trees.DecisionStump.class.getName();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tThe number of folds for splitting the training set into\n"
        + "\tsmaller chunks for the base classifier.\n" + "\t(default 10)",
      "F", 1, "-F <folds>"));

    result.addElement(new Option(
      "\tWhether to print some more information during building the\n"
        + "\tclassifier.\n" + "\t(default is off)", "verbose", 0, "-verbose"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -F &lt;folds&gt;
   *  The number of folds for splitting the training set into
   *  smaller chunks for the base classifier.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -verbose
   *  Whether to print some more information during building the
   *  classifier.
   *  (default is off)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0) {
      setNumFolds(Integer.parseInt(tmpStr));
    } else {
      setNumFolds(10);
    }

    setVerbose(Utils.getFlag("verbose", options));

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

    result.add("-F");
    result.add("" + getNumFolds());

    if (getVerbose()) {
      result.add("-verbose");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the number of folds to use for splitting the training set.
   * 
   * @return the number of folds
   */
  public int getNumFolds() {
    return m_NumFolds;
  }

  /**
   * Sets the number of folds to use for splitting the training set.
   * 
   * @param value the new number of folds
   */
  public void setNumFolds(int value) {
    if (value > 0) {
      m_NumFolds = value;
    } else {
      System.out.println("At least 1 fold is necessary (provided: " + value
        + ")!");
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "The number of folds to use for splitting the training set into smaller chunks for the base classifier.";
  }

  /**
   * Set the verbose state.
   * 
   * @param value the verbose state
   */
  public void setVerbose(boolean value) {
    m_Verbose = value;
  }

  /**
   * Gets the verbose state
   * 
   * @return the verbose state
   */
  public boolean getVerbose() {
    return m_Verbose;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String verboseTipText() {
    return "Whether to ouput some additional information during building.";
  }

  /**
   * Bagging method.
   * 
   * @param data the training data to be used for generating the bagged
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    Classifier[] base;
    int i;
    // int n; NOT USED
    // int fromIndex; NOT USED
    // int toIndex; NOT USED
    Instances train;
    // double chunkSize; NOT USED

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    m_Vote = new Vote();
    base = new Classifier[getNumFolds()];
    // chunkSize = (double) data.numInstances() / (double) getNumFolds(); NOT
    // USED

    // stratify data
    if (getNumFolds() > 1) {
      data.randomize(data.getRandomNumberGenerator(getSeed()));
      data.stratify(getNumFolds());
    }

    // generate <folds> classifiers
    for (i = 0; i < getNumFolds(); i++) {
      base[i] = makeCopy(getClassifier());

      // generate training data
      if (getNumFolds() > 1) {
        // some progress information
        if (getVerbose()) {
          System.out.print(".");
        }

        train = data.testCV(getNumFolds(), i);
      } else {
        train = data;
      }

      // train classifier
      base[i].buildClassifier(train);
    }

    // init vote
    m_Vote.setClassifiers(base);

    if (getVerbose()) {
      System.out.println();
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   * 
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @throws Exception if distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {
    return m_Vote.distributionForInstance(instance);
  }

  /**
   * Returns description of the classifier.
   * 
   * @return description of the classifier as a string
   */
  @Override
  public String toString() {
    if (m_Vote == null) {
      return this.getClass().getName().replaceAll(".*\\.", "")
        + ": No model built yet.";
    } else {
      return m_Vote.toString();
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
    runClassifier(new Dagging(), args);
  }
}
