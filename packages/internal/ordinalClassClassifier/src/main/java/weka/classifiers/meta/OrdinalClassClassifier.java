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
 *    OrdinalClassClassifier.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;

/**
 * <!-- globalinfo-start --> Meta classifier that allows standard classification
 * algorithms to be applied to ordinal class problems.<br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Eibe Frank, Mark Hall: A Simple Approach to Ordinal Classification. In: 12th
 * European Conference on Machine Learning, 145-156, 2001.<br/>
 * <br/>
 * Robert E. Schapire, Peter Stone, David A. McAllester, Michael L. Littman,
 * Janos A. Csirik: Modeling Auction Price Uncertainty Using Boosting-based
 * Conditional Density Estimation. In: Machine Learning, Proceedings of the
 * Nineteenth International Conference (ICML 2002), 546-553, 2002.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Frank2001,
 *    author = {Eibe Frank and Mark Hall},
 *    booktitle = {12th European Conference on Machine Learning},
 *    pages = {145-156},
 *    publisher = {Springer},
 *    title = {A Simple Approach to Ordinal Classification},
 *    year = {2001}
 * }
 * 
 * &#64;inproceedings{Schapire2002,
 *    author = {Robert E. Schapire and Peter Stone and David A. McAllester and Michael L. Littman and Janos A. Csirik},
 *    booktitle = {Machine Learning, Proceedings of the Nineteenth International Conference (ICML 2002)},
 *    pages = {546-553},
 *    publisher = {Morgan Kaufmann},
 *    title = {Modeling Auction Price Uncertainty Using Boosting-based Conditional Density Estimation},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -S
 *  Turn off Schapire et al.'s smoothing heuristic (ICML02, pp. 550).
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
 *  (default: weka.classifiers.trees.J48)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre>
 * -U
 *  Use unpruned tree.
 * </pre>
 * 
 * <pre>
 * -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)
 * </pre>
 * 
 * <pre>
 * -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)
 * </pre>
 * 
 * <pre>
 * -R
 *  Use reduced error pruning.
 * </pre>
 * 
 * <pre>
 * -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)
 * </pre>
 * 
 * <pre>
 * -B
 *  Use binary splits only.
 * </pre>
 * 
 * <pre>
 * -S
 *  Don't perform subtree raising.
 * </pre>
 * 
 * <pre>
 * -L
 *  Do not clean up after the tree has been built.
 * </pre>
 * 
 * <pre>
 * -A
 *  Laplace smoothing for predicted probabilities.
 * </pre>
 * 
 * <pre>
 * -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall
 * @author Eibe Frank
 * @version $Revision$
 * @see OptionHandler
 */
public class OrdinalClassClassifier extends SingleClassifierEnhancer implements
  OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -3461971774059603636L;

  /** The classifiers. (One for each class.) */
  private Classifier[] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicator[] m_ClassFilters;

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /** Whether to use smoothing to prevent negative "probabilities". */
  private boolean m_UseSmoothing = true;

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {

    return "weka.classifiers.trees.J48";
  }

  /**
   * Default constructor.
   */
  public OrdinalClassClassifier() {
    m_Classifier = new weka.classifiers.trees.J48();
  }

  /**
   * Returns a string describing this attribute evaluator
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Meta classifier that allows standard classification algorithms "
      + "to be applied to ordinal class problems.\n\n"
      + "For more information see: \n\n" + getTechnicalInformation().toString();
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
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Eibe Frank and Mark Hall");
    result.setValue(Field.TITLE, "A Simple Approach to Ordinal Classification");
    result.setValue(Field.BOOKTITLE,
      "12th European Conference on Machine Learning");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "145-156");
    result.setValue(Field.PUBLISHER, "Springer");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR,
      "Robert E. Schapire and Peter Stone and David A. McAllester "
        + "and Michael L. Littman and Janos A. Csirik");
    additional.setValue(Field.TITLE,
      "Modeling Auction Price Uncertainty Using Boosting-based "
        + "Conditional Density Estimation");
    additional.setValue(Field.BOOKTITLE,
      "Machine Learning, Proceedings of the Nineteenth "
        + "International Conference (ICML 2002)");
    additional.setValue(Field.YEAR, "2002");
    additional.setValue(Field.PAGES, "546-553");
    additional.setValue(Field.PUBLISHER, "Morgan Kaufmann");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);

    return result;
  }

  /**
   * Builds the classifiers.
   * 
   * @param insts the training data.
   * @throws Exception if a classifier can't be built
   */
  @Override
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);

    int numClassifiers = insts.numClasses() - 1;

    numClassifiers = (numClassifiers == 0) ? 1 : numClassifiers;

    if (numClassifiers == 1) {
      m_Classifiers = AbstractClassifier.makeCopies(m_Classifier, 1);
      m_Classifiers[0].buildClassifier(insts);
    } else {
      m_Classifiers = AbstractClassifier.makeCopies(m_Classifier,
        numClassifiers);
      m_ClassFilters = new MakeIndicator[numClassifiers];

      for (int i = 0; i < m_Classifiers.length; i++) {
        m_ClassFilters[i] = new MakeIndicator();
        m_ClassFilters[i].setAttributeIndex("" + (insts.classIndex() + 1));
        m_ClassFilters[i].setValueIndices("" + (i + 2) + "-last");
        m_ClassFilters[i].setNumeric(false);
        m_ClassFilters[i].setInputFormat(insts);
        newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
        m_Classifiers[i].buildClassifier(newInsts);
      }
    }
  }

  /**
   * Compute class probabilities based on base classifiers class probabilities.
   */
  protected double[] computeProbabilities(double[][] distributions, int numClasses) {

    double[] probs = new double[numClasses];

    // Use Schapire et al.'s smoothing heuristic?
    if (getUseSmoothing()) {

      double[] fScores = new double[distributions.length + 2];
      fScores[0] = 1;
      fScores[distributions.length + 1] = 0;
      for (int i = 0; i < distributions.length; i++) {
        fScores[i + 1] = distributions[i][1];
      }

      // Sort scores in ascending order
      int[] sortOrder = Utils.sort(fScores);

      // Compute pointwise maximum of lower bound
      int minSoFar = sortOrder[0];
      int index = 0;
      double[] pointwiseMaxLowerBound = new double[fScores.length];
      for (int i = 0; i < sortOrder.length; i++) {

        // Progress to next higher value if possible
        while (minSoFar > sortOrder.length - i - 1) {
          minSoFar = sortOrder[++index];
        }
        pointwiseMaxLowerBound[sortOrder.length - i - 1] = fScores[minSoFar];
      }

      // Get scores in descending order
      int[] newSortOrder = new int[sortOrder.length];
      for (int i = sortOrder.length - 1; i >= 0; i--) {
        newSortOrder[sortOrder.length - i - 1] = sortOrder[i];
      }
      sortOrder = newSortOrder;

      // Compute pointwise minimum of upper bound
      int maxSoFar = sortOrder[0];
      index = 0;
      double[] pointwiseMinUpperBound = new double[fScores.length];
      for (int i = 0; i < sortOrder.length; i++) {

        // Progress to next lower value if possible
        while (maxSoFar < i) {
          maxSoFar = sortOrder[++index];
        }
        pointwiseMinUpperBound[i] = fScores[maxSoFar];
      }

      // Compute average
      for (int i = 0; i < distributions.length; i++) {
        distributions[i][1] = (pointwiseMinUpperBound[i + 1] + pointwiseMaxLowerBound[i + 1]) / 2.0;
      }
    }

    for (int i = 0; i < numClasses; i++) {
      if (i == 0) {
        probs[i] = 1.0 - distributions[0][1];
      } else if (i == numClasses - 1) {
        probs[i] = distributions[i - 1][1];
      } else {
        probs[i] = distributions[i - 1][1] - distributions[i][1];
        if (!(probs[i] >= 0)) {
          System.err.println("Warning: estimated probability " + probs[i]
                  + ". Rounding to 0.");
          probs[i] = 0;
        }
      }
    }

    return probs;
  }

  /**
   * Returns the distribution for an instance.
   * 
   * @param inst the instance to compute the distribution for
   * @return the class distribution for the given instance
   * @throws Exception if the distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    if (m_Classifiers.length == 1) {
      return m_Classifiers[0].distributionForInstance(inst);
    }

    double[][] distributions = new double[m_ClassFilters.length][0];
    for (int i = 0; i < m_ClassFilters.length; i++) {
      m_ClassFilters[i].input(inst);
      m_ClassFilters[i].batchFinished();
      distributions[i] = m_Classifiers[i].distributionForInstance(m_ClassFilters[i].output());
    }

    double[] probs = computeProbabilities(distributions, inst.numClasses());

    if (Utils.gr(Utils.sum(probs), 0)) {
      Utils.normalize(probs);
      return probs;
    } else {
      return m_ZeroR.distributionForInstance(inst);
    }
  }

  /**
   * Returns true if the base classifier implements BatchPredictor and is able
   * to generate batch predictions efficiently
   *
   * @return true if the base classifier can generate batch predictions
   *         efficiently
   */
  public boolean implementsMoreEfficientBatchPrediction() {
    if (!(getClassifier() instanceof BatchPredictor)) {
      return false;
    }

    return ((BatchPredictor) getClassifier())
            .implementsMoreEfficientBatchPrediction();
  }

  /**
   * Returns the distributions for a set of instances. Calls
   * base classifier's distributionForInstance() if it does not support
   * distributionForInstances().
   *
   * @param insts the instances to compute the distribution for
   * @return the class distributions for the given instances
   * @throws Exception if the distribution can't be computed successfully
   */
  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    if (m_Classifiers.length == 1) {
      if (m_Classifier instanceof BatchPredictor) {
        return ((weka.core.BatchPredictor)m_Classifiers[0]).distributionsForInstances(insts);
      }
      double[][] dists = new double[insts.numInstances()][];
      for (int i = 0; i < dists.length; i++) {
        dists[i] = m_Classifiers[0].distributionForInstance(insts.instance(i));
      }
      return dists;
    }

    double[][][] distributions = new double[insts.numInstances()][m_ClassFilters.length][0];
    for (int i = 0; i < m_ClassFilters.length; i++) {
      if (m_Classifier instanceof BatchPredictor) {
        Instances filtered = Filter.useFilter(insts, m_ClassFilters[i]);
        double[][] currentDist = ((BatchPredictor) m_Classifiers[i]).distributionsForInstances(filtered);
        for (int j = 0; j < currentDist.length; j++) {
          distributions[j][i] = currentDist[j];
        }
      } else {
        for (int j = 0; j < insts.numInstances(); j++) {
          m_ClassFilters[i].input(insts.instance(j));
          m_ClassFilters[i].batchFinished();
          distributions[j][i] = m_Classifiers[i].distributionForInstance(m_ClassFilters[i].output());
        }
      }
    }

    double[][] probs = new double[insts.numInstances()][];
    for (int i = 0; i < probs.length; i++) {
      probs[i] = computeProbabilities(distributions[i], insts.numClasses());
      if (Utils.gr(Utils.sum(probs[i]), 0)) {
        Utils.normalize(probs[i]);
      } else {
        probs[i] = m_ZeroR.distributionForInstance(insts.instance(i));
      }
    }
    return probs;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> vec = new Vector<Option>();
    vec.addElement(new Option("\tTurn off Schapire et al.'s smoothing "
      + "heuristic (ICML02, pp. 550).", "S", 0, "-S"));

    vec.addAll(Collections.list(super.listOptions()));

    return vec.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -S
   *  Turn off Schapire et al.'s smoothing heuristic (ICML02, pp. 550).
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.J48)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.J48:
   * </pre>
   * 
   * <pre>
   * -U
   *  Use unpruned tree.
   * </pre>
   * 
   * <pre>
   * -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)
   * </pre>
   * 
   * <pre>
   * -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)
   * </pre>
   * 
   * <pre>
   * -R
   *  Use reduced error pruning.
   * </pre>
   * 
   * <pre>
   * -N &lt;number of folds&gt;
   *  Set number of folds for reduced error
   *  pruning. One fold is used as pruning set.
   *  (default 3)
   * </pre>
   * 
   * <pre>
   * -B
   *  Use binary splits only.
   * </pre>
   * 
   * <pre>
   * -S
   *  Don't perform subtree raising.
   * </pre>
   * 
   * <pre>
   * -L
   *  Do not clean up after the tree has been built.
   * </pre>
   * 
   * <pre>
   * -A
   *  Laplace smoothing for predicted probabilities.
   * </pre>
   * 
   * <pre>
   * -Q &lt;seed&gt;
   *  Seed for random data shuffling (default 1).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setUseSmoothing(!Utils.getFlag('S', options));
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

    Vector<String> options = new Vector<String>();

    if (!getUseSmoothing()) {
      options.add("-S");
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Tip text method.
   * 
   * @return a tip text string suitable for displaying as a popup in the GUI.
   */
  public String useSmoothingTipText() {
    return "If true, use Schapire et al.'s heuristic (ICML02, pp. 550).";
  }

  /**
   * Determines whether Schapire et al.'s smoothing method is used.
   * 
   * @param b true if the smoothing heuristic is to be used.
   */
  public void setUseSmoothing(boolean b) {

    m_UseSmoothing = b;
  }

  /**
   * Checks whether Schapire et al.'s smoothing method is used.
   * 
   * @return true if the smoothing heuristic is to be used.
   */
  public boolean getUseSmoothing() {

    return m_UseSmoothing;
  }

  /**
   * Prints the classifiers.
   * 
   * @return a string representation of this classifier
   */
  @Override
  public String toString() {

    if (m_Classifiers == null) {
      return "OrdinalClassClassifier: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("OrdinalClassClassifier\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append("Classifier ").append(i + 1);
      if (m_Classifiers[i] != null) {
        if ((m_ClassFilters != null) && (m_ClassFilters[i] != null)) {
          text.append(", using indicator values: ");
          text.append(m_ClassFilters[i].getValueRange());
        }
        text.append('\n');
        text.append(m_Classifiers[i].toString() + "\n");
      } else {
        text.append(" Skipped (no training examples)\n");
      }
    }

    return text.toString();
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
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new OrdinalClassClassifier(), argv);
  }
}
