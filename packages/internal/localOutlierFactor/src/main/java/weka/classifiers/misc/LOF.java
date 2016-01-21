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
 *    LOF.java
 *    Copyright (C) 1999-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.misc;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.neighboursearch.LinearNNSearch;
import weka.core.neighboursearch.NearestNeighbourSearch;
import weka.filters.Filter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A Classifier that applies the LOF (Local Outlier Factor) algorithm to compute an "outlier" score for each instance in the data. The data is expected to have a unary or binary class attribute, which is ignored at training time. The distributionForInstance() method returns 1 - normalized outlier score in the first element of the distribution. If the class attribute is binary, then the second element holds the normalized outlier score. To evaluate performance of this method for a dataset where outliers/anomalies are known, simply code the outliers using the class attribute: normal cases should correspond to the first value of the class attribute; outliers to the second one.<br>
 * <br>
 * Can use multiple cores/cpus to speed up the LOF computation for large datasets. Nearest neighbor search methods and distance functions are pluggable.<br>
 * <br>
 * For more information, see:<br>
 * <br>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond T. Ng, Jorg Sander (2000). LOF: Identifying Density-Based Local Outliers. ACM SIGMOD Record. 29(2):93-104.
 * <br><br>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Breunig2000,
 *    author = {Markus M. Breunig and Hans-Peter Kriegel and Raymond T. Ng and Jorg Sander},
 *    journal = {ACM SIGMOD Record},
 *    number = {2},
 *    pages = {93-104},
 *    publisher = {ACM New York},
 *    title = {LOF: Identifying Density-Based Local Outliers},
 *    volume = {29},
 *    year = {2000}
 * }
 * </pre>
 * <br><br>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -min &lt;num&gt;
 *  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
 *  (default = 10)</pre>
 * 
 * <pre> -max &lt;num&gt;
 *  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
 *  (default = 40)</pre>
 * 
 * <pre> -A
 *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
 * </pre>
 * 
 * <pre> -num-slots &lt;num&gt;
 *  Number of execution slots.
 *  (default 1 - i.e. no parallelism)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * 
 */
public class LOF extends AbstractClassifier
  implements Serializable, CapabilitiesHandler, OptionHandler,
  TechnicalInformationHandler, RevisionHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -2736613569494944202L;

  protected weka.filters.unsupervised.attribute.LOF m_lof;

  /** The lower bound on the minimum number of points (k) */
  protected String m_minPtsLB = "10";

  /** The upper bound on the minimum number of points (k) */
  protected String m_minPtsUB = "40";

  /** The nearest neighbor search to use */
  protected NearestNeighbourSearch m_nnTemplate = new LinearNNSearch();

  /** The number of threads to use */
  protected String m_numSlots = "1";

  /** Maximum LOF score seen in the training data */
  protected double m_minScore;

  /** Minimum LOF score seen in the training data */
  protected double m_maxScore;

  /** Minimum probability value */
  protected static final double m_tol = 1e-6;

  /**
   * Returns a string describing this scheme
   * 
   * @return a description of the scheme suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "A Classifier that applies the LOF (Local Outlier Factor) algorithm "
      + "to compute an \"outlier\" score for each instance in the data. "
      + "The data is expected to have a unary or binary class attribute, which "
      + "is ignored at training time. The distributionForInstance() method returns "
      + "1 - normalized outlier score in the first element of the distribution. If the class "
      + "attribute is binary, then the second element holds the normalized outlier score. "
      + "To evaluate performance of this method for a dataset where outliers/anomalies "
      + "are known, simply code the outliers using the class attribute: normal cases "
      + "should correspond to the first value of the class attribute; outliers to the "
      + "second one.\n\nCan use "
      + "multiple cores/cpus to speed up the LOF computation for large datasets. "
      + "Nearest neighbor search methods and distance functions are pluggable."
      + "\n\nFor more information, see:\n\n"
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

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Markus M. Breunig and Hans-Peter "
      + "Kriegel and Raymond T. Ng and Jorg Sander");
    result.setValue(Field.TITLE,
      "LOF: Identifying Density-Based Local Outliers");
    result.setValue(Field.JOURNAL, "ACM SIGMOD Record");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.VOLUME, "29");
    result.setValue(Field.NUMBER, "2");
    result.setValue(Field.PAGES, "93-104");
    result.setValue(Field.PUBLISHER, "ACM New York");
    return result;
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.UNARY_CLASS);
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(1);

    return result;
  }

  /**
   * Gets an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();
    newVector.add(new Option(
      "\tLower bound on the k nearest neighbors "
        + "for finding max LOF (minPtsLB)\n\t(default = 10)",
      "min", 1, "-min <num>"));
    newVector.add(new Option(
      "\tUpper bound on the k nearest neighbors "
        + "for finding max LOF (minPtsUB)\n\t(default = 40)",
      "max", 1, "-max <num>"));
    newVector.addElement(new Option(
      "\tThe nearest neighbour search algorithm to use "
        + "(default: weka.core.neighboursearch.LinearNNSearch).\n",
      "A", 0, "-A"));
    newVector.addElement(new Option(
      "\tNumber of execution slots.\n" + "\t(default 1 - i.e. no parallelism)",
      "num-slots", 1, "-num-slots <num>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * <p>
   * Parses a given list of options.
   * </p>
   * 
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -min &lt;num&gt;
   *  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
   *  (default = 10)</pre>
   * 
   * <pre> -max &lt;num&gt;
   *  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
   *  (default = 40)</pre>
   * 
   * <pre> -A
   *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
   * </pre>
   * 
   * <pre> -num-slots &lt;num&gt;
   *  Number of execution slots.
   *  (default 1 - i.e. no parallelism)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   * 
   * <pre> -num-decimal-places
   *  The number of decimal places for the output of numbers in the model (default 2).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String minP = Utils.getOption("min", options);
    if (minP.length() > 0) {
      setMinPointsLowerBound(minP);
    }

    String maxP = Utils.getOption("max", options);
    if (maxP.length() > 0) {
      setMinPointsUpperBound(maxP);
    }

    String nnSearchClass = Utils.getOption('A', options);
    if (nnSearchClass.length() != 0) {
      String nnSearchClassSpec[] = Utils.splitOptions(nnSearchClass);
      if (nnSearchClassSpec.length == 0) {
        throw new Exception("Invalid NearestNeighbourSearch algorithm "
          + "specification string.");
      }
      String className = nnSearchClassSpec[0];
      nnSearchClassSpec[0] = "";

      setNNSearch((NearestNeighbourSearch) Utils
        .forName(NearestNeighbourSearch.class, className, nnSearchClassSpec));
    } else {
      this.setNNSearch(new LinearNNSearch());
    }

    String slotsS = Utils.getOption("num-slots", options);
    if (slotsS.length() > 0) {
      setNumExecutionSlots(slotsS);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the scheme.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-min");
    options.add(getMinPointsLowerBound());
    options.add("-max");
    options.add(getMinPointsUpperBound());

    options.add("-A");
    options.add(m_nnTemplate.getClass().getName() + " "
      + Utils.joinOptions(m_nnTemplate.getOptions()));
    options.add("-num-slots");
    options.add(getNumExecutionSlots());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minPointsLowerBoundTipText() {
    return "The lower bound (minPtsLB) to use on the range for k "
      + "when determining the maximum LOF value";
  }

  /**
   * Set the lower bound (minPtsLB) to use on the range for k when determining
   * the maximum LOF value
   * 
   * @param pts the lower bound
   */
  public void setMinPointsLowerBound(String pts) {
    m_minPtsLB = pts;
  }

  /**
   * Get the lower bound (minPtsLB) to use on the range for k when determining
   * the maximum LOF value
   * 
   * @return the lower bound
   */
  public String getMinPointsLowerBound() {
    return m_minPtsLB;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minPointsUpperBoundTipText() {
    return "The upper bound (minPtsUB) to use on the range for k "
      + "when determining the maximum LOF value";
  }

  /**
   * Set the upper bound (minPtsUB) to use on the range for k when determining
   * the maximum LOF value
   * 
   * @param pts the upper bound
   */
  public void setMinPointsUpperBound(String pts) {
    m_minPtsUB = pts;
  }

  /**
   * Get the upper bound (minPtsUB) to use on the range for k when determining
   * the maximum LOF value
   * 
   * @return the upper bound
   */
  public String getMinPointsUpperBound() {
    return m_minPtsUB;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String NNSearchTipText() {
    return "The nearest neighbour search algorithm to use "
      + "(Default: weka.core.neighboursearch.LinearNNSearch).";
  }

  /**
   * Set the nearest neighbor search method to use
   * 
   * @param s the nearest neighbor search method to use
   */
  public void setNNSearch(NearestNeighbourSearch s) {
    m_nnTemplate = s;
  }

  /**
   * Get the nearest neighbor search method to use
   * 
   * @return the nearest neighbor search method to use
   */
  public NearestNeighbourSearch getNNSearch() {
    return m_nnTemplate;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numExecutionSlotsTipText() {
    return "The number of execution slots (threads) to use for "
      + "finding LOF values.";
  }

  /**
   * Set the degree of parallelism to use.
   * 
   * @param slots the number of tasks to run in parallel when computing the
   *          nearest neighbors and evaluating different values of k between the
   *          lower and upper bounds
   */
  public void setNumExecutionSlots(String slots) {
    m_numSlots = slots;
  }

  /**
   * Get the degree of parallelism to use.
   * 
   * @return the number of tasks to run in parallel when computing the nearest
   *         neighbors and evaluating different values of k between the lower
   *         and upper bounds
   */
  public String getNumExecutionSlots() {
    return m_numSlots;
  }

  @Override
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_maxScore = Double.MIN_VALUE;
    m_minScore = Double.MAX_VALUE;
    m_lof = new weka.filters.unsupervised.attribute.LOF();
    m_lof.setInputFormat(data);
    m_lof.setMinPointsLowerBound(m_minPtsLB);
    m_lof.setMinPointsUpperBound(m_minPtsUB);
    m_lof.setNNSearch(m_nnTemplate);
    m_lof.setNumExecutionSlots(m_numSlots);

    Instances temp = Filter.useFilter(data, m_lof);

    for (int i = 0; i < temp.numInstances(); i++) {
      double current = temp.instance(i).value(temp.numAttributes() - 1);
      if (!Double.isNaN(current)) {
        if (current > m_maxScore) {
          m_maxScore = current;
        }
        if (current < m_minScore) {
          m_minScore = current;
        }
      }
    }
  }

  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    double[] scores = new double[inst.classAttribute().numValues()];

    double lofScore;
    if (m_maxScore == m_minScore) {
      lofScore = 0;
    } else {
      m_lof.input(inst);
      Instance scored = m_lof.output();
      lofScore = scored.value(scored.numAttributes() - 1);

      lofScore -= m_minScore;
      if (lofScore <= 0) {
        lofScore = m_tol;
      }

      lofScore = lofScore / (m_maxScore - m_minScore);
      if (lofScore >= 1) {
        lofScore = 1 - m_tol;
      }
    }

    scores[0] = 1 - lofScore;
    if (scores.length > 1) {
      scores[1] = lofScore;
    }

    return scores;
  }

  @Override
  public String toString() {
    if (m_lof == null) {
      return "No model built yet!";
    }

    return "Local Outlier Factor classifier\n\n";
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9723 $");
  }

  /**
   * Main method for this class.
   *
   * @param args arguments to use
   */
  public static void main(String[] args) {

    runClassifier(new LOF(), args);
  }
}

