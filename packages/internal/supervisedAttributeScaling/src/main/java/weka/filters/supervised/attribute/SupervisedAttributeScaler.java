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
 * SupervisedAttributeScaler.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.supervised.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.functions.NonNegativeLogisticRegression;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.neighboursearch.LinearNNSearch;
import weka.core.neighboursearch.NearestNeighbourSearch;
import weka.filters.SimpleBatchFilter;
import weka.filters.SupervisedFilter;

/**
 * <!-- globalinfo-start --> Rescales the attributes in a classification problem
 * based on their discriminative power. This is useful as a pre-processing step
 * for learning algorithms such as the k-nearest-neighbour method, to replace
 * simple normalization. Each attribute is rescaled by multiplying it with a
 * learned weight. All attributes excluding the class are assumed to be numeric
 * and missing values are not permitted.<br/><br/>
 * The attribute weights are learned by taking the original labeled dataset with
 * N instances and creating a new dataset with N*K instances, where K is the
 * number of neighbours selected. To this end, each instance in the original dataset is
 * paired with its K nearest neighbours, creating K pairs. Then, an instance in
 * the new dataset is created for each pair, with the same number of attributes
 * as in the original data. An attribute's value in this new instance is set to
 * the absolute difference between the corresponding attribute values in the pair of original
 * instances. The new instance's label depends on whether the two instances in the pair have
 * the same class label or not, yielding a
 * two-class classification problem. A logistic regression model with
 * non-negative coefficients is learned from this data and the resulting
 * coefficients are used as weights to rescale the original data.<br/><br/>
 * This process assumes that distance in the original space is measured using
 * Manhattan distance because the absolute difference is taken between attribute
 * values. The method can optionally be used to learn weights for a Euclidean
 * distance. In this case, squared differences are taken rather than absolute
 * differences, and the square root of the learned coefficients is used to
 * rescale the attributes in the original data.<br/>
 * <br/>
 * The approach is based on the Probabilistic Global Distance Metric Learning
 * method included in the experimental comparison in<br/>
 * <br/>
 * L. Yang, R. Jin, R. Sukthankar, Y. Liu: An efficient algorithm for local
 * distance metric learning. In: Proceedings of the National Conference on
 * Artificial Intelligence, 543-548, 2006.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Yang2006,
 *    author = {L. Yang and R. Jin and R. Sukthankar and Y. Liu},
 *    booktitle = {Proceedings of the National Conference on Artificial Intelligence},
 *    pages = {543-548},
 *    publisher = {AAAI Press},
 *    title = {An efficient algorithm for local distance metric learning},
 *    year = {2006}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -D
 *  Turns on output of debugging information.
 * </pre>
 * 
 * <pre>
 * -assume-Euclidean-distance
 *  If set, weights are learned for Euclidean distance.
 * </pre>
 * 
 * <pre>
 * -K
 *  The number of neighbours to use (default: 30).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class SupervisedAttributeScaler extends SimpleBatchFilter implements
  SupervisedFilter, TechnicalInformationHandler {

  /** For serialization */
  static final long serialVersionUID = -4448107323933117974L;

  /** The weights used to rescale the data */
  protected double[] m_weights = null;

  /** Whether to use Euclidean distance assumption */
  protected boolean m_AssumeEuclideanDistance = false;

  /** How many neighbours to use */
  protected int m_numNeighbours = 30;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Rescales the attributes in a classification problem based on their discriminative power. This is "
      + "useful as a pre-processing step for learning algorithms such as the k-nearest-neighbour method, to "
      + "replace simple normalization. Each attribute is rescaled by multiplying it with a learned weight. "
      + "All attributes excluding the class are assumed to be numeric and missing values are not permitted.\n\n"
      + "The attribute weights are learned by taking the original labeled dataset with N instances and "
      + "creating a new dataset with N*K instances, where K is the number of neighbours selected. To this end, "
      + "each instance in the original dataset is paired with its K nearest neighbours, creating K pairs. Then, an instance "
      + "in the new dataset is created for each pair, with the same number of attributes as in the original "
      + "data. An attribute's value in this new instance is set to the "
      + "absolute difference between the corresponding attribute values in the pair of original instances. "
      + "The new instance's label depends on whether the two instances in the pair have the same class "
      + "label or not, yielding a two-class classification problem. A "
      + "logistic regression model with non-negative coefficients is learned from this data and "
      + "the resulting coefficients are used as weights to rescale the original data.\n\n"
      + "This process assumes that distance in the original space is measured using Manhattan distance "
      + "because the absolute difference is taken between attribute values. The method can optionally "
      + "be used to learn weights for a Euclidean distance. In this case, squared differences are "
      + "taken rather than absolute differences, and the square root of the learned coefficients is "
      + "used to rescale the attributes in the original data.\n\n"
      + "The approach is based on the Probabilistic Global Distance Metric Learning method included "
      + "in the experimental comparison in\n\n"
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
    result.setValue(Field.AUTHOR,
      "L. Yang and R. Jin and R. Sukthankar and Y. Liu");
    result.setValue(Field.TITLE,
      "An efficient algorithm for local distance metric learning");
    result.setValue(Field.BOOKTITLE,
      "Proceedings of the National Conference on Artificial Intelligence");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.PAGES, "543-548");
    result.setValue(Field.PUBLISHER, "AAAI Press");

    return result;
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
      "\tIf set, weights are learned for Euclidean distance.\n",
      "-assume-Euclidean-distance", 0, "-assume-Euclidean-distance"));

    result.addElement(new Option(
      "\tThe number of neighbours to use (default: 30).\n", "-K", 1, "-K"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (getAssumeEuclideanDistance()) {
      result.add("-assume-Euclidean-distance");
    }

    result.add("-K");
    result.add("" + getNumNeighbours());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -assume-Euclidean-distance
   *  If set, weights are learned for Euclidean distance.
   * </pre>
   * 
   * <pre>
   * -K
   *  The number of neighbours to use (default: 30).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setAssumeEuclideanDistance(Utils.getFlag("assume-Euclidean-distance",
      options));

    String knnString = Utils.getOption('K', options);
    if (knnString.length() != 0) {
      setNumNeighbours(Integer.parseInt(knnString));
    } else {
      setNumNeighbours(30);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String assumeEuclideanDistanceTipText() {
    return "Whether to assume Euclidean distance rather than Manhattan distance.";
  }

  /**
   * Gets whether Euclidean distance is to be assumed.
   * 
   * @return true if Euclidean distance is to be assumed.
   */
  public boolean getAssumeEuclideanDistance() {
    return m_AssumeEuclideanDistance;
  }

  /**
   * Sets whether Euclidean distance is to be assumed.
   * 
   * @param value if true, Euclidean distance is assumed.
   */
  public void setAssumeEuclideanDistance(boolean value) {
    m_AssumeEuclideanDistance = value;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numNeighboursTipText() {
    return "The number of neighbours to use.";
  }

  /**
   * Gets the number of neighbours.
   * 
   * @return the number of neighbours
   */
  public int getNumNeighbours() {
    return m_numNeighbours;
  }

  /**
   * Sets the number of neighbours.
   * 
   * @param value the number of neighbours
   */
  public void setNumNeighbours(int value) {
    m_numNeighbours = value;
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) {
    return new Instances(inputFormat);
  }

  /**
   * Initializes the filter with the given dataset.
   * 
   * @param instances the data to initialize with
   * @throws Exception if building the filter model fails
   */
  public void initFilter(Instances instances) throws Exception {

    // Establish dimensions of input data
    int N = instances.numInstances();
    int M = instances.numAttributes();

    // Reserve space for weights
    m_weights = new double[M];

    // Construct header for pairwise data
    ArrayList<Attribute> atts = new ArrayList<Attribute>(M + 1);
    atts.add(new Attribute("-1"));
    for (int i = 0; i < M; i++) {
      if (i == instances.classIndex()) {
        ArrayList<String> classValues = new ArrayList<String>(2);
        classValues.add("different_class_values");
        classValues.add("same_class_values");
        atts.add(new Attribute("Class", classValues));
      } else {
        atts.add((Attribute) instances.attribute(i).copy());
      }
    }

    // Set up the object used for the nearest neighbour search
    DistanceFunction ed = null;
    if (getAssumeEuclideanDistance()) {
      EuclideanDistance d = new EuclideanDistance();
      d.setDontNormalize(true);
      ed = d;
    } else {
      ManhattanDistance d = new ManhattanDistance();
      d.setDontNormalize(true);
      ed = d;
    }
    NearestNeighbourSearch nnSearch = new LinearNNSearch();
    nnSearch.setDistanceFunction(ed);
    nnSearch.setInstances(instances);

    // Create pairwise data
    int numNeighbours = m_numNeighbours;
    if (m_numNeighbours >= instances.numInstances()) {
      numNeighbours = instances.numInstances() - 1;
    }
    Instances pairwiseData = new Instances("pairwise_data", atts, N
      * numNeighbours);
    for (int i = 0; i < N; i++) {
      Instance inst1 = instances.instance(i);
      nnSearch.addInstanceInfo(inst1);
      Instances neighbours = nnSearch.kNearestNeighbours(inst1, numNeighbours);
      for (int j = 0; j < numNeighbours; j++) {
        Instance inst2 = neighbours.instance(j);
        double[] diffInst = new double[M + 1];
        diffInst[0] = -1.0; // Want to learn negative intercept
        for (int k = 0; k < M; k++) {
          if (k != instances.classIndex()) {
            double diff = inst1.value(k) - inst2.value(k);
            if (getAssumeEuclideanDistance()) {
              diffInst[k + 1] = diff * diff;
            } else {
              diffInst[k + 1] = Math.abs(diff);
            }
          } else {
            diffInst[k + 1] = (inst1.classValue() == inst2.classValue()) ? 1.0
              : 0.0;
          }
        }
        pairwiseData.add(new DenseInstance(1.0, diffInst));
      }
    }
    pairwiseData.setClassIndex(instances.classIndex() + 1);

    // Run logistic regression
    NonNegativeLogisticRegression nnlr = new NonNegativeLogisticRegression();
    nnlr.buildClassifier(pairwiseData);

    double[] coefficients = nnlr.getCoefficients();
    for (int i = 1; i < coefficients.length; i++) {
      m_weights[i - 1] = coefficients[i];
      if (getAssumeEuclideanDistance()) {
        m_weights[i - 1] = Math.sqrt(m_weights[i - 1]);
      }
    }
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = super.getCapabilities();
    result.disableAll();

    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    return result;
  }

  /**
   * Processes the given data.
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    // Do we need to build the filter model?
    if (!isFirstBatchDone()) {
      initFilter(instances);
    }

    // Generate the output and return it
    Instances result = new Instances(instances, instances.numInstances());
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      double[] newData = new double[instances.numAttributes()];
      for (int j = 0; j < instances.numAttributes(); j++) {
        newData[j] = inst.value(j);
        if (j != instances.classIndex()) {
          newData[j] *= m_weights[j];
        }
      }
      result.add(new DenseInstance(1.0, newData));
    }
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new SupervisedAttributeScaler(), args);
  }
}
