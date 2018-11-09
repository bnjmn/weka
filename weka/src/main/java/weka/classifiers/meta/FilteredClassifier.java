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
 *    FilteredClassifier.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.IterativeClassifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.Reorder;

import java.util.*;

/**
 * <!-- globalinfo-start --> Class for running an arbitrary classifier on data
 * that has been passed through an arbitrary filter. Like the classifier, the
 * structure of the filter is based exclusively on the training data and test
 * instances will be processed by the filter without changing their structure.
 * If unequal instance weights or attribute weights are present, and the filter
 * or the classifier are unable to deal with them, the instances and/or attributes
 * are resampled with replacement based on the weights before they are passed
 * to the filter or the classifier (as appropriate).
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  default: "weka.filters.supervised.attribute.Discretize -R first-last -precision 6"
 * </pre>
 *
 * <pre>
 * -W &lt;classifier name&gt;
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)
 * </pre>
 *
 * <pre> -S num
 * The random number seed to be used (default 1). </pre>
 *
 * -doNotCheckForModifiedClassAttribute <br>
 * If this is set, the classifier will not check whether the filter modifies the class attribute (use with caution).
 * <p>
 *
 * -output-debug-info <br>
 * If set, classifier is run in debug mode and may output additional info to
 * the console.
 * <p>
 *
 * -do-not-check-capabilities <br>
 * If set, classifier capabilities are not checked before classifier is built
 * (use with caution).
 * <p>
 *
 * -num-decimal-places <br>
 * The number of decimal places for the output of numbers in the model.
 * <p>
 *
 * -batch-size <br>
 * The desired batch size for batch prediction.
 * <p>
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
 * -S &lt;seed&gt;
 *  Seed for random data shuffling (default 1).
 * </pre>
 * 
 * <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class FilteredClassifier extends RandomizableSingleClassifierEnhancer
  implements Drawable, PartitionGenerator, IterativeClassifier, BatchPredictor,
        WeightedInstancesHandler, WeightedAttributesHandler {

  /** for serialization */
  static final long serialVersionUID = -4523450618538717400L;

  /** The filter */
  protected Filter m_Filter = new AttributeSelection();

  /** The instance structure of the filtered instances */
  protected Instances m_FilteredInstances;

  /** Flag that can be set to true if class attribute is not to be checked for modifications by the filer. */
  protected boolean m_DoNotCheckForModifiedClassAttribute = false;

  /** If the attributes are resampled, we store the filter for this */
  protected Reorder m_ReorderOriginal;
  protected Reorder m_ReorderFiltered;

  /**
   * Returns a string describing this classifier
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for running an arbitrary classifier on data that has been passed "
            + "through an arbitrary filter. Like the classifier, the structure of the filter "
            + "is based exclusively on the training data and test instances will be processed "
            + "by the filter without changing their structure.\n\n" +
            "If unequal instance weights or attribute weights are present, and the filter " +
            "or the classifier are unable to deal with them, the instances and/or attributes " +
            "are resampled with replacement based on the weights before they are passed " +
            "to the filter or the classifier (as appropriate).";
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
   * String describing default filter.
   */
  protected String defaultFilterString() {

    return "weka.filters.supervised.attribute.Discretize -R first-last -precision 6";
  }

  /**
   * Default constructor.
   */
  public FilteredClassifier() {

    m_Classifier = new weka.classifiers.trees.J48();
    m_Filter = new weka.filters.supervised.attribute.Discretize();
  }

  /**
   * Returns the type of graph this classifier represents.
   * 
   * @return the graph type of this classifier
   */
  public int graphType() {

    if (m_Classifier instanceof Drawable)
      return ((Drawable) m_Classifier).graphType();
    else
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @throws Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {

    if (m_Classifier instanceof Drawable)
      return ((Drawable) m_Classifier).graph();
    else
      throw new Exception(
        "Classifier: " + getClassifierSpec() + " cannot be graphed");
  }

  /**
   * Builds the classifier to generate a partition. (If the base classifier
   * supports this.)
   */
  public void generatePartition(Instances data) throws Exception {

    if (m_Classifier instanceof PartitionGenerator)
      buildClassifier(data);
    else
      throw new Exception(
        "Classifier: " + getClassifierSpec() + " cannot generate a partition");
  }

  /**
   * Computes an array that has a value for each element in the partition. (If
   * the base classifier supports this.)
   */
  public double[] getMembershipValues(Instance inst) throws Exception {

    if (m_Classifier instanceof PartitionGenerator) {
      if (m_ReorderOriginal != null) {
        m_ReorderOriginal.input(inst);
        inst = m_ReorderOriginal.output();
      }
      Instance newInstance = filterInstance(inst);
      if (newInstance == null) {
        double[] unclassified = new double[numElements()];
        for (int i = 0; i < unclassified.length; i++) {
          unclassified[i] = Utils.missingValue();
        }
        return unclassified;
      } else {
        if (m_ReorderFiltered != null) {
          m_ReorderFiltered.input(newInstance);
          newInstance = m_ReorderFiltered.output();
        }
        return ((PartitionGenerator) m_Classifier).getMembershipValues(newInstance);
      }
    } else
      throw new Exception(
        "Classifier: " + getClassifierSpec() + " cannot generate a partition");
  }

  /**
   * Returns the number of elements in the partition. (If the base classifier
   * supports this.)
   */
  public int numElements() throws Exception {

    if (m_Classifier instanceof PartitionGenerator)
      return ((PartitionGenerator) m_Classifier).numElements();
    else
      throw new Exception(
        "Classifier: " + getClassifierSpec() + " cannot generate a partition");
  }

  /**
   * Initializes an iterative classifier. (If the base classifier supports
   * this.)
   *
   * @param data the instances to be used in induction
   * @exception Exception if the model cannot be initialized
   */
  @Override public void initializeClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    getCapabilities().testWithFail(data);

    if (m_Classifier instanceof IterativeClassifier) {
      Random r = (data.numInstances() > 0) ? data.getRandomNumberGenerator(getSeed()) : new Random(getSeed());
      data = setUp(data, r);
      if (!data.allInstanceWeightsIdentical() && !(m_Classifier instanceof WeightedInstancesHandler)) {
        data = data.resampleWithWeights(r); // The filter may have assigned weights.
      }
      if (!data.allAttributeWeightsIdentical() && !(m_Classifier instanceof WeightedAttributesHandler)) {
        data = resampleAttributes(data, false, r);
      }

      // can classifier handle the data?
      getClassifier().getCapabilities().testWithFail(data);

      if (m_Classifier instanceof Randomizable) {
        ((Randomizable)m_Classifier).setSeed(r.nextInt());
      }

      ((IterativeClassifier) m_Classifier).initializeClassifier(data);
     } else {
      throw new Exception("Classifier: " + getClassifierSpec() + " is not an IterativeClassifier");
    }
  }

  /**
   * Performs one iteration. (If the base classifier supports this.)
   *
   * @return false if no further iterations could be performed, true otherwise
   * @exception Exception if this iteration fails for unexpected reasons
   */
  @Override public boolean next() throws Exception {

    if (m_Classifier instanceof IterativeClassifier)
      return ((IterativeClassifier) m_Classifier).next();
    else
      throw new Exception("Classifier: " + getClassifierSpec() + " is not an IterativeClassifier");
  }

  /**
   * Signal end of iterating, useful for any house-keeping/cleanup (If the base
   * classifier supports this.)
   *
   * @exception Exception if cleanup fails
   */
  @Override public void done() throws Exception {

    if (m_Classifier instanceof IterativeClassifier)
      ((IterativeClassifier) m_Classifier).done();
    else
      throw new Exception("Classifier: " + getClassifierSpec() + " is not an IterativeClassifier");
  }

  /**
   * Tool tip text for finalize property
   *
   * @return the tool tip text for the finalize property
   */
  public String resumeTipText() {
    return "Set whether classifier can continue training after performing the"
      + "requested number of iterations. \n\tNote that setting this to true will "
      + "retain certain data structures which can increase the \n\t"
      + "size of the model. Only applicable when the base classifier \n\t"
      + "is an IterativeClassifier.";
  }

  /**
   * If called with argument true, then the next time done() is called the model is effectively
   * "frozen" and no further iterations can be performed
   *
   * @param resume true if the model is to be finalized after performing iterations
   */
  public void setResume(boolean resume) throws Exception {
    if (m_Classifier instanceof IterativeClassifier) {
      ((IterativeClassifier) m_Classifier).setResume(resume);
    }
  }

  /**
   * Returns true if the model is to be finalized (or has been finalized) after
   * training.
   *
   * @return the current value of finalize
   */
  public boolean getResume() {
    if (m_Classifier instanceof IterativeClassifier) {
      return ((IterativeClassifier) m_Classifier).getResume();
    }
    return false;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);
    newVector.addElement(new Option(
      "\tFull class name of filter to use, followed\n"
        + "\tby filter options.\n"
        + "\tdefault: \"" + defaultFilterString() + "\"",
      "F", 1, "-F <filter specification>"));

    newVector.addElement(new Option(
            "\tIf set, classifier will not check whether the filter modifies the class (use with caution).",
            "doNotCheckForModifiedClassAttribute", 0, "-doNotCheckForModifiedClassAttribute"));

    newVector.addAll(Collections.list(super.listOptions()));

    if (getFilter() instanceof OptionHandler) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to filter "
        + getFilter().getClass().getName() + ":"));
      newVector
        .addAll(Collections.list(((OptionHandler) getFilter()).listOptions()));
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   * <!-- options-start --> Valid options are:
   * <p/>
   *
   * <pre>
   * -F &lt;filter specification&gt;
   *  Full class name of filter to use, followed
   *  by filter options.
   *  default: "weka.filters.supervised.attribute.Discretize -R first-last -precision 6"
   * </pre>
   *
   * <pre>
   * -W &lt;classifier name&gt;
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.J48)
   * </pre>
   *
   * <pre> -S num
   * The random number seed to be used. </pre>
   *
   * -doNotCheckForModifiedClassAttribute <br>
   * If this is set, the classifier will not check whether the filter modifies the class attribute (use with caution).
   * <p>
   *
   * -output-debug-info <br>
   * If set, classifier is run in debug mode and may output additional info to
   * the console.
   * <p>
   *
   * -do-not-check-capabilities <br>
   * If set, classifier capabilities are not checked before classifier is built
   * (use with caution).
   * <p>
   *
   * -num-decimal-laces <br>
   * The number of decimal places for the output of numbers in the model.
   * <p>
   *
   * -batch-size <br>
   * The desired batch size for batch prediction.
   * <p>
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
  public void setOptions(String[] options) throws Exception {

    String filterString = Utils.getOption('F', options);
    if (filterString.length() <= 0) {
      filterString = defaultFilterString();
    }
    String[] filterSpec = Utils.splitOptions(filterString);
    if (filterSpec.length == 0) {
      throw new IllegalArgumentException("Invalid filter specification string");
    }
    String filterName = filterSpec[0];
    filterSpec[0] = "";
    setFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));

    setDoNotCheckForModifiedClassAttribute(Utils.getFlag("doNotCheckForModifiedClassAttribute", options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String doNotCheckForModifiedClassAttributeTipText() {
    return "Turns off check for modified class attribute - use with caution.";
  }

  /**
   * Returns true if classifier checks whether class attribute has been modified by filter.
   */
  public boolean getDoNotCheckForModifiedClassAttribute() {

    return m_DoNotCheckForModifiedClassAttribute;
  }

  /**
   * Use this method to determine whether classifier checks whether class attribute has been modified by filter.
   */
  public void setDoNotCheckForModifiedClassAttribute(boolean flag) {

    m_DoNotCheckForModifiedClassAttribute = flag;
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-F");
    options.add("" + getFilterSpec());

    if (getDoNotCheckForModifiedClassAttribute()) {
      options.add("-doNotCheckForModifiedClassAttribute");
    }

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTipText() {
    return "The filter to be used.";
  }

  /**
   * Sets the filter
   *
   * @param filter the filter with all options set.
   */
  public void setFilter(Filter filter) {

    m_Filter = filter;
  }

  /**
   * Gets the filter used.
   *
   * @return the filter
   */
  public Filter getFilter() {

    return m_Filter;
  }

  /**
   * Gets the filter specification string, which contains the class name of the
   * filter and any options to the filter
   *
   * @return the filter string.
   */
  protected String getFilterSpec() {

    Filter c = getFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getFilter() == null)
      result = super.getCapabilities();
    else {
      result = getFilter().getCapabilities();

      // By default, check that classifier can handle the class attribute
     if (!getDoNotCheckForModifiedClassAttribute()) {
        Capabilities classes = super.getCapabilities().getClassCapabilities();
        Iterator<Capability> iter = classes.capabilities();
        result.disableAllClasses();
        while (iter.hasNext()) {
          result.enable(iter.next());
        }
      }
    }

    // the filtered classifier always needs a class
    result.disable(Capability.NO_CLASS);

    // set dependencies
    for (Capability cap : Capability.values())
      result.enableDependency(cap);

    result.setOwner(this);

    return result;
  }

  /**
   * Sets up the filter and runs checks.
   *
   * @return filtered data
   */
  protected Instances setUp(Instances data, Random random) throws Exception {

    m_ReorderOriginal = null;
    m_ReorderFiltered = null;

    if (data.allInstanceWeightsIdentical() || (m_Filter instanceof WeightedInstancesHandler)) {
      data = new Instances(data);
    } else {
      data = data.resampleWithWeights(random);
    }
    if (!data.allAttributeWeightsIdentical() && !(m_Filter instanceof WeightedAttributesHandler)) {
      data = resampleAttributes(data, true, random);
    }

    /*
     * String fname = m_Filter.getClass().getName(); fname =
     * fname.substring(fname.lastIndexOf('.') + 1); util.Timer t =
     * util.Timer.getTimer("FilteredClassifier::" + fname); t.start();
     */
    Attribute classAttribute = (Attribute)data.classAttribute().copy();

    if (m_Filter instanceof Randomizable) {
      ((Randomizable)m_Filter).setSeed(random.nextInt());
    }

    m_Filter.setInputFormat(data); // filter capabilities are checked here
    data = Filter.useFilter(data, m_Filter);
    if ((!classAttribute.equals(data.classAttribute())) && (!m_DoNotCheckForModifiedClassAttribute)) {
      throw new IllegalArgumentException("Cannot proceed: " + getFilterSpec() + " has modified the class attribute!");
    }
    // t.stop();

    m_FilteredInstances = data.stringFreeStructure();
    return data;
  }

  /**
   * Resamples set of attributes.
   *
   * @param data the data to be filtered
   * @param original whether the original or the filtered data are to be resampled
   * @param random the random number generator to use
   */
  protected Instances resampleAttributes(Instances data, boolean original, Random random) throws Exception {

    // Need to sample attributes so that weights are represented by sample
    int nAtt = (data.classIndex() >= 0) ? data.numAttributes() - 1: data.numAttributes();
    int index = 0;
    double[] attributeWeights = new double[nAtt];
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {
        attributeWeights[index++] = data.attribute(i).weight();
      }
    }
    int[] frequencies = Utils.takeSample(attributeWeights, random);

    // Make list of attribute indices based on frequencies in sample
    ArrayList<Integer> al = new ArrayList<Integer>();
    index = 0;
    for (int j = 0; j < data.numAttributes(); j++) {
      if (j == data.classIndex()) {
        al.add(j);
      } else {
        for (int i = 0; i < frequencies[index]; i++) {
          al.add(j);
        }
        index++;
      }
    }

    // Filter data
    if (original) {
      m_ReorderOriginal = new Reorder();
      m_ReorderOriginal.setAttributeIndicesArray(al.stream().mapToInt(j -> j).toArray());
      m_ReorderOriginal.setAllAttributeWeightsToOne(true);
      m_ReorderOriginal.setInputFormat(data);
      return Filter.useFilter(data, m_ReorderOriginal);
    } else {
      m_ReorderFiltered = new Reorder();
      m_ReorderFiltered.setAttributeIndicesArray(al.stream().mapToInt(j -> j).toArray());
      m_ReorderFiltered.setAllAttributeWeightsToOne(true);
      m_ReorderFiltered.setInputFormat(data);
      return Filter.useFilter(data, m_ReorderFiltered);
    }
  }

  /**
   * Build the classifier on the filtered data.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    getCapabilities().testWithFail(data);

    Random r = (data.numInstances() > 0) ? data.getRandomNumberGenerator(getSeed()) : new Random(getSeed());
    data = setUp(data, r);
    if (!data.allInstanceWeightsIdentical() && !(m_Classifier instanceof WeightedInstancesHandler)) {
      data = data.resampleWithWeights(r); // The filter may have assigned weights.
    }
    if (!data.allAttributeWeightsIdentical() && !(m_Classifier instanceof WeightedAttributesHandler)) {
      data = resampleAttributes(data, false, r);
    }

    // can classifier handle the data?
    getClassifier().getCapabilities().testWithFail(data);

    if (m_Classifier instanceof Randomizable) {
      ((Randomizable)m_Classifier).setSeed(r.nextInt());
    }

    m_Classifier.buildClassifier(data);
  }

  /**
   * Filters the instance so that it can subsequently be classified.
   */
  protected Instance filterInstance(Instance instance) throws Exception {

    /*
     * System.err.println("FilteredClassifier:: " +
     * m_Filter.getClass().getName() + " in: " + instance);
     */
    if (m_Filter.numPendingOutput() > 0) {
      throw new Exception("Filter output queue not empty!");
    }
    /*
     * String fname = m_Filter.getClass().getName(); fname =
     * fname.substring(fname.lastIndexOf('.') + 1); util.Timer t =
     * util.Timer.getTimer("FilteredClassifier::" + fname); t.start();
     */
    if (!m_Filter.input(instance)) {
      if (!m_Filter.mayRemoveInstanceAfterFirstBatchDone()) {
        throw new Exception(
          "Filter didn't make the test instance" + " immediately available!");
      } else {
        m_Filter.batchFinished();
        return null;
      }
    }
    m_Filter.batchFinished();
    return m_Filter.output();
    // t.stop();
    /*
     * System.err.println("FilteredClassifier:: " +
     * m_Filter.getClass().getName() + " out: " + newInstance);
     */
  }

  /**
   * Classifies a given instance after filtering.
   *
   * @param instance the instance to be classified
   * @return the class distribution for the given instance
   * @throws Exception if instance could not be classified successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (m_ReorderOriginal != null) {
      m_ReorderOriginal.input(instance);
      instance = m_ReorderOriginal.output();
    }
    Instance newInstance = filterInstance(instance);
    if (newInstance == null) {

      // filter has consumed the instance (e.g. RemoveWithValues
      // may do this). We will indicate no prediction for this
      // instance
      double[] unclassified = null;
      if (instance.classAttribute().isNumeric()) {
        unclassified = new double[1];
        unclassified[0] = Utils.missingValue();
      } else {
        // all zeros
        unclassified = new double[instance.classAttribute().numValues()];
      }
      return unclassified;
    } else {
      if (m_ReorderFiltered != null) {
        m_ReorderFiltered.input(newInstance);
        newInstance = m_ReorderFiltered.output();
      }
      return m_Classifier.distributionForInstance(newInstance);
    }
  }

  /**
   * Tool tip text for this property
   * 
   * @return the tool tip for this property
   */
  public String batchSizeTipText() {
    return "Batch size to use if base learner is a BatchPredictor";
  }

  /**
   * Set the batch size to use. Gets passed through to the base learner if it
   * implements BatchPredictor. Otherwise it is just ignored.
   *
   * @param size the batch size to use
   */
  public void setBatchSize(String size) {

    if (getClassifier() instanceof BatchPredictor) {
      ((BatchPredictor) getClassifier()).setBatchSize(size);
    } else {
      super.setBatchSize(size);
    }
  }

  /**
   * Gets the preferred batch size from the base learner if it implements
   * BatchPredictor. Returns 1 as the preferred batch size otherwise.
   *
   * @return the batch size to use
   */
  public String getBatchSize() {

    if (getClassifier() instanceof BatchPredictor) {
      return ((BatchPredictor) getClassifier()).getBatchSize();
    } else {
      return super.getBatchSize();
    }
  }

  /**
   * Batch scoring method. Calls the appropriate method for the base learner if
   * it implements BatchPredictor. Otherwise it simply calls the
   * distributionForInstance() method repeatedly.
   * 
   * @param insts the instances to get predictions for
   * @return an array of probability distributions, one for each instance
   * @throws Exception if a problem occurs
   */
  public double[][] distributionsForInstances(Instances insts)
    throws Exception {

    if (getClassifier() instanceof BatchPredictor) {
      if (m_ReorderOriginal != null) {
        insts = Filter.useFilter(insts, m_ReorderOriginal);
      }
      Instances filteredInsts = Filter.useFilter(insts, m_Filter);
      if (filteredInsts.numInstances() != insts.numInstances()) {
        throw new WekaException(
          "FilteredClassifier: filter has returned more/less instances than required.");
      }
      if (m_ReorderOriginal != null) {
        filteredInsts = Filter.useFilter(filteredInsts, m_ReorderFiltered);
      }
      return ((BatchPredictor) getClassifier()).distributionsForInstances(filteredInsts);
    } else {
      double[][] result = new double[insts.numInstances()][insts.numClasses()];
      for (int i = 0; i < insts.numInstances(); i++) {
        result[i] = distributionForInstance(insts.instance(i));
      }
      return result;
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
      return super.implementsMoreEfficientBatchPrediction();
    }

    return ((BatchPredictor) getClassifier())
      .implementsMoreEfficientBatchPrediction();
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a representation of this classifier
   */
  public String toString() {

    if (m_FilteredInstances == null) {
      return "FilteredClassifier: No model built yet.";
    }

    String result = "FilteredClassifier using " + getClassifierSpec()
      + " on data filtered through " + getFilterSpec() + "\n\nFiltered Header\n"
      + m_FilteredInstances.toString() + "\n\nClassifier Model\n"
      + m_Classifier.toString();
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments: -t training file [-T
   *          test file] [-c class index]
   */
  public static void main(String[] argv) {
    runClassifier(new FilteredClassifier(), argv);
  }
}
