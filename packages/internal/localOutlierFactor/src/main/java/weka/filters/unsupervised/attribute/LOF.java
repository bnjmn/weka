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
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.SparseInstance;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.neighboursearch.LinearNNSearch;
import weka.core.neighboursearch.NearestNeighbourSearch;
import weka.filters.Filter;

/**
 * <!-- globalinfo-start --> A filter that applies the LOF (Local Outlier
 * Factor) algorithm to compute an "outlier" score for each instance in the
 * data. Can use multiple cores/cpus to speed up the LOF computation for large
 * datasets. Nearest neighbor search methods and distance functions are
 * pluggable.<br>
 * <br>
 * For more information, see:<br>
 * <br>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond T. Ng, Jorg Sander (2000).
 * LOF: Identifying Density-Based Local Outliers. ACM SIGMOD Record.
 * 29(2):93-104.
 * <p>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * </p>
 * 
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
 * <p>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * </p>
 * 
 * <pre>
 * -min &lt;num&gt;
 *  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
 *  (default = 10)
 * </pre>
 * 
 * <pre>
 * -max &lt;num&gt;
 *  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
 *  (default = 40)
 * </pre>
 * 
 * <pre>
 * -A
 *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
 * </pre>
 * 
 * <pre>
 * -num-slots &lt;num&gt;
 *  Number of execution slots.
 *  (default 1 - i.e. no parallelism)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class LOF extends Filter implements OptionHandler,
  WeightedInstancesHandler, TechnicalInformationHandler, EnvironmentHandler {

  /** For serialization */
  private static final long serialVersionUID = 3843951651734143371L;

  /** The lower bound on the minimum number of points (k) */
  protected String m_minPtsLB = "10";

  /** The upper bound on the minimum number of points (k) */
  protected String m_minPtsUB = "40";

  /** The nearest neighbor search to use */
  protected NearestNeighbourSearch m_nnSearch;

  /** The nearest neighbor search to use */
  protected NearestNeighbourSearch m_nnTemplate = new LinearNNSearch();

  /** Environment variables */
  protected transient Environment m_env;

  /** Lower bound for k */
  protected int m_lbK = 10;

  /** Upper bound for k */
  protected int m_ubK = 40;

  /** True if a class attribute is set in the data */
  protected boolean m_classSet = false;

  /** Holds the lookup key for each training instance */
  protected transient DecisionTableHashKey[] m_instKeys;

  /** Map of training instances to their neighborhood information */
  protected Map<DecisionTableHashKey, Neighborhood> m_kDistanceContainer;

  /** For parallel execution mode */
  protected transient ThreadPoolExecutor m_executorPool;
  protected String m_numSlots = "1";
  protected int m_numExecutionSlots = 1;
  protected int m_completed;
  protected int m_failed;

  /** 
   * If true, then the LOF scores for the training data do not
   * need to be computed.
   */
  protected boolean m_classifierMode = false;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "A filter that applies the LOF (Local Outlier Factor) algorithm "
      + "to compute an \"outlier\" score for each instance in the data. Can use "
      + "multiple cores/cpus to speed up the LOF computation for large datasets. "
      + "Nearest neighbor search methods and distance functions are pluggable."
      + "\n\nFor more information, see:\n\n"
      + getTechnicalInformation().toString();
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
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.UNARY_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
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
   * Gets an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();
    newVector.add(new Option("\tLower bound on the k nearest neighbors "
      + "for finding max LOF (minPtsLB)\n\t(default = 10)", "min", 1,
      "-min <num>"));
    newVector.add(new Option("\tUpper bound on the k nearest neighbors "
      + "for finding max LOF (minPtsUB)\n\t(default = 40)", "max", 1,
      "-max <num>"));
    newVector.addElement(new Option(
      "\tThe nearest neighbour search algorithm to use "
        + "(default: weka.core.neighboursearch.LinearNNSearch).\n", "A", 0,
      "-A"));
    newVector.addElement(new Option("\tNumber of execution slots.\n"
      + "\t(default 1 - i.e. no parallelism)", "num-slots", 1,
      "-num-slots <num>"));

    return newVector.elements();
  }

  /**
   * <p>Parses a given list of options.
   * </p>
   * 
   * <!-- options-start --> Valid options are:
   * <br>
   * 
   * <pre>
   * -min &lt;num&gt;
   *  Lower bound on the k nearest neighbors for finding max LOF (minPtsLB)
   *  (default = 10)
   * </pre>
   * 
   * <pre>
   * -max &lt;num&gt;
   *  Upper bound on the k nearest neighbors for finding max LOF (minPtsUB)
   *  (default = 40)
   * </pre>
   * 
   * <pre>
   * -A
   *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
   * </pre>
   * 
   * <pre>
   * -num-slots &lt;num&gt;
   *  Number of execution slots.
   *  (default 1 - i.e. no parallelism)
   * </pre>
   * 
   * <!-- options-end -->
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

      setNNSearch((NearestNeighbourSearch) Utils.forName(
        NearestNeighbourSearch.class, className, nnSearchClassSpec));
    } else {
      this.setNNSearch(new LinearNNSearch());
    }

    String slotsS = Utils.getOption("num-slots", options);
    if (slotsS.length() > 0) {
      setNumExecutionSlots(slotsS);
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
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

    return options.toArray(new String[0]);
  }

  /**
   * Set whether to work in classifier mode or not. In classifier
   * mode the LOF scores for the training data do not need to
   * be computed at training time
   *
   * @param cm true if working as a classifier
   */
  public void setClassifierMode(boolean cm) {
    m_classifierMode = cm;
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

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);

    m_nnSearch = null;

    return false;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been defined.
   */
  @Override
  public boolean input(Instance instance) {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (m_nnSearch != null) {
      try {
        postFirstBatch(instance);
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new IllegalStateException(ex.getMessage());
      }
      return true;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Signifies that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances.
   * 
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  @Override
  public boolean batchFinished() {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    if (m_nnSearch == null) {
      setOutputFormat();

      if (m_numSlots != null && m_numSlots.length() > 0) {
        String nS = m_numSlots;
        try {
          nS = m_env.substitute(nS);
          m_numExecutionSlots = Integer.parseInt(nS);

          if (m_numExecutionSlots < 1) {
            m_numExecutionSlots = 1;
          }
        } catch (Exception ex) {
        }
      }

      try {
        // can't do much if the only attribute in the data is the class
        if (getInputFormat().classIndex() >= 0
          && getInputFormat().numAttributes() == 1) {
          Instances input = getInputFormat();
          for (int i = 0; i < input.numInstances(); i++) {
            Instance inst = makeOutputInstance(input.instance(i), 1.0);
            push(inst);
          }
        } else {
          if (m_numExecutionSlots < 2) {
            LOFFirstBatch();
          } else {
            LOFFirstBatchParallel();
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new IllegalStateException(ex.getMessage());
      }
    }

    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Set the output format. Takes the currently defined cutpoints and
   * m_InputFormat and calls setOutputFormat(Instances) appropriately.
   */
  protected void setOutputFormat() {
    ArrayList<Attribute> atts = new ArrayList<Attribute>();

    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      atts.add((Attribute) getInputFormat().attribute(i).copy());
    }

    atts.add(new Attribute("LOF"));

    Instances outputFormat = new Instances(getInputFormat().relationName(),
      atts, 0);
    outputFormat.setClassIndex(getInputFormat().classIndex());
    setOutputFormat(outputFormat);
  }

  /**
   * Inner class that encapsulates neighborhood information for a given data
   * point. Stores nearest neighbors, distances and lrd/lof values corresponding
   * to different values of k between the lower and upper bounds
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected class Neighborhood implements Serializable {
    /**
     * For serialization
     */
    private static final long serialVersionUID = 3381174623146672703L;

    // nearest neighbor list
    public Instances m_neighbors;
    public double[] m_distances;

    public double[] m_tempCardinality;
    public double[] m_lrd;
    public double[] m_lof;
  }

  /**
   * Computes LOF for an instance that is not part of the training data (first
   * batch)
   * 
   * @param inst the instance to compute the LOF for
   * @throws Exception if a problem occurs
   */
  protected void postFirstBatch(Instance inst) throws Exception {

    // can't do much if the data only contains the class
    if (inst.classIndex() >= 0 && inst.numAttributes() == 1) {
      Instance newInst = makeOutputInstance(inst, 1.0);
      push(newInst);
      return;
    }

    Instances nn = null;
    int nnFactor = 2;
    Neighborhood currentN = new Neighborhood();

    do {
      nn = m_nnSearch.kNearestNeighbours(inst, m_ubK * nnFactor);
      currentN.m_neighbors = nn;
      currentN.m_distances = m_nnSearch.getDistances();
      trimZeroDistances(currentN);

      nnFactor++;
    } while (nn.numInstances() < m_ubK);

    currentN.m_tempCardinality = new double[m_ubK - m_lbK];
    currentN.m_lof = new double[m_ubK - m_lbK];
    currentN.m_lrd = new double[m_ubK - m_lbK];

    // for each k in the range minPtsLB to maxPtsLB
    for (int k = m_lbK; k < m_ubK; k++) {

      int indexOfKDistanceForK = k - 1;
      while (indexOfKDistanceForK < currentN.m_distances.length - 1
        && currentN.m_distances[indexOfKDistanceForK] == currentN.m_distances[indexOfKDistanceForK + 1]) {
        indexOfKDistanceForK++;
      }

      // lrd first
      double cardinality = 0;
      double sumReachability = 0;

      for (int j = 0; j <= indexOfKDistanceForK; j++) {
        Instance b = currentN.m_neighbors.instance(j);
        cardinality += b.weight();
        sumReachability += reachability(inst, b, currentN.m_distances[j], k);
      }

      currentN.m_lrd[k - m_lbK] = cardinality / sumReachability;
      // temporarily store the cardinality at k
      currentN.m_tempCardinality[k - m_lbK] = cardinality;

      double lofK = lof(currentN, k);

      // set lof to the maximum
      if (lofK > currentN.m_lof[k - m_lbK]) {
        currentN.m_lof[k - m_lbK] = lofK;
      }
    }

    double maxLOF = currentN.m_lof[Utils.maxIndex(currentN.m_lof)];

    Instance newInst = makeOutputInstance(inst, maxLOF);
    push(newInst);
  }

  /**
   * Initialize various bits and pieces
   * 
   * @param training the training data (first batch)
   * @throws Exception if a problem occurs
   */
  protected void init(Instances training) throws Exception {
    m_classSet = (training.classIndex() >= 0);

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    String lbKS = m_minPtsLB;
    String ubKS = m_minPtsUB;

    try {
      lbKS = m_env.substitute(lbKS);
      m_lbK = Integer.parseInt(lbKS);
    } catch (Exception ex) {
    }
    try {
      ubKS = m_env.substitute(ubKS);
      m_ubK = Integer.parseInt(ubKS);
    } catch (Exception ex) {
    }

    m_ubK++; // upper bound is inclusive (our loops are exclusive)

    if (m_ubK >= training.numInstances()) {
      System.err.println("Can't have more neighbors than data points.");
      m_ubK = training.numInstances() - 1;
    }
    if (m_ubK <= m_lbK) {
      System.err.println("Upper bound on k can't be < lower bound - "
        + "setting equal to the lower bound");
      m_ubK = m_lbK + 1; // upper bound is inclusive (our loops are exclusive)
    }

    // global search for use when processing after the first batch is done
    SerializedObject o = new SerializedObject(m_nnTemplate);
    m_nnSearch = (NearestNeighbourSearch) o.getObject();
    m_nnSearch.setInstances(new Instances(training));

    if (m_numExecutionSlots > 1) {
      m_kDistanceContainer = new ConcurrentHashMap<DecisionTableHashKey, Neighborhood>();
    } else {
      m_kDistanceContainer = new HashMap<DecisionTableHashKey, Neighborhood>();
    }

    m_instKeys = new DecisionTableHashKey[training.numInstances()];
  }

  /**
   * Inner class for finding nearest neighbors in parallel mode
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected class NNFinder implements Runnable {

    protected Instances m_nnTrain;
    protected int m_start;
    protected int m_end;
    protected NearestNeighbourSearch m_search;

    public NNFinder(Instances training, int start, int end,
      NearestNeighbourSearch search) {
      m_nnTrain = training;
      m_start = start;
      m_end = end;
      m_search = search;
    }

    @Override
    public void run() {
      try {
        for (int i = m_start; i < m_end; i++) {
          Instance current = m_nnTrain.get(i);
          DecisionTableHashKey key = new DecisionTableHashKey(current,
            current.numAttributes(), !m_classSet);
          Neighborhood n = new Neighborhood();
          if (addToKDistanceContainer(key, n)) {
            Instances nn = null;
            int nnFactor = 2;
            do {
              nn = m_search.kNearestNeighbours(current, m_ubK * nnFactor);
              n.m_neighbors = nn;
              n.m_distances = m_search.getDistances();
              trimZeroDistances(n);

              nnFactor++;
            } while (nn.numInstances() < m_ubK);

            n.m_tempCardinality = new double[m_ubK - m_lbK];
            n.m_lrd = new double[m_ubK - m_lbK];
            n.m_lof = new double[m_ubK - m_lbK];
          }
          m_instKeys[i] = key;
        }
        completedTask("NN search", true, m_numExecutionSlots);
      } catch (Exception ex) {
        ex.printStackTrace();
        completedTask("NN search", false, m_numExecutionSlots);
      }
    }
  }

  /**
   * Inner class for finding LOF values in parallel
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected class LOFFinder implements Runnable {
    protected Instances m_lofTrain;
    protected int m_k;

    public LOFFinder(Instances training, int k) {
      m_lofTrain = training;
      m_k = k;
    }

    @Override
    public void run() {
      try {
        // lrd's first
        for (int i = 0; i < m_lofTrain.numInstances(); i++) {
          Instance current = m_lofTrain.instance(i);

          Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);
          // this defines the k-neighborhood and may be larger than k because of
          // ties in
          // distance at the k-th nearest neighbor
          int indexOfKDistanceForK = m_k - 1;
          while (indexOfKDistanceForK < currentN.m_distances.length - 1
            && currentN.m_distances[indexOfKDistanceForK] == currentN.m_distances[indexOfKDistanceForK + 1]) {
            indexOfKDistanceForK++;
          }

          // lrd for current point with k nearest neighbors
          double cardinality = 0;
          double sumReachability = 0;

          for (int j = 0; j <= indexOfKDistanceForK; j++) {
            Instance b = currentN.m_neighbors.instance(j);

            cardinality += b.weight();
            sumReachability += reachability(current, b,
              currentN.m_distances[j], m_k);
          }

          currentN.m_lrd[m_k - m_lbK] = cardinality / sumReachability;

          // store the cardinality at k
          currentN.m_tempCardinality[m_k - m_lbK] = cardinality;
        }

        // now lof's
        for (int i = 0; i < m_lofTrain.numInstances(); i++) {
          Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);

          double lofK = lof(currentN, m_k);

          // set lof
          currentN.m_lof[m_k - m_lbK] = lofK;
        }
        completedTask("LOF finder", true, (m_ubK - m_lbK));
      } catch (Exception ex) {
        ex.printStackTrace();
        completedTask("LOF finder", false, (m_ubK - m_lbK));
      }
    }
  }

  /**
   * Finds LOF values for all training instances (first batch) in parallel mode.
   * First finds the neighborhood (nearest neighbors) for each training instance
   * in parallel. Then it finds computes LOF values for each value of k between
   * the lower and upper bound in parallel
   * 
   * @throws Exception if a problem occurs
   */
  protected void LOFFirstBatchParallel() throws Exception {
    Instances training = getInputFormat();
    init(training);
    m_completed = 0;
    m_failed = 0;
    startExecutorPool();

    // first find all the nearest neighbours in parallel
    int numPerThread = training.numInstances() / m_numExecutionSlots;
    if (numPerThread < 1) {
      m_numExecutionSlots = training.numInstances();
      numPerThread = 1;
    }
    int start = 0;
    int end = 0;
    for (int i = 0; i < m_numExecutionSlots; i++) {
      if (i == m_numExecutionSlots - 1) {
        end = training.numInstances();
      } else {
        end = start + numPerThread;
      }
      SerializedObject oo = new SerializedObject(m_nnTemplate);
      NearestNeighbourSearch s = (NearestNeighbourSearch) oo.getObject();
      s.setInstances(new Instances(training));
      NNFinder finder = new NNFinder(new Instances(training), start, end, s);
      m_executorPool.execute(finder);
      start += numPerThread;
    }

    if (m_completed + m_failed < m_numExecutionSlots) {
      block(true, m_numExecutionSlots);
    }

    if (m_failed > 0) {
      throw new Exception(
        "Can't continue - some tasks failed during the nearest "
          + "neighbour phase");
    }
    // m_kDistanceContainer = Collections.unmodifiableMap(m_kDistanceContainer);

    // now evaluate the minPts range between minPtsLB and minPtsUB in parallel
    m_completed = 0;
    m_failed = 0;
    int numLOFFinders = m_ubK - m_lbK;

    // for each k in the range minPtsLB to maxPtsLB
    for (int k = m_lbK; k < m_ubK; k++) {
      LOFFinder finder = new LOFFinder(training, k);
      m_executorPool.execute(finder);
    }

    if (m_completed + m_failed < numLOFFinders) {
      block(true, numLOFFinders);
    }

    if (m_failed > 0) {
      throw new Exception("Can't continue - some tasks failed during the LOF "
        + "phase");
    }
    m_executorPool.shutdown();

    // make the output instances
    if (!m_classifierMode) {
      for (int i = 0; i < training.numInstances(); i++) {
        Instance current = training.instance(i);
        Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);

        // use the maximum LOF found over the range of k's explored
        double maxLOF = currentN.m_lof[Utils.maxIndex(currentN.m_lof)];
        Instance inst = makeOutputInstance(current, maxLOF);
        push(inst);
      }
    }
  }

  protected synchronized boolean addToKDistanceContainer(
    DecisionTableHashKey key, Neighborhood n) {

    if (!m_kDistanceContainer.containsKey(key)) {
      m_kDistanceContainer.put(key, n);
      return true;
    }

    return false;
  }

  protected synchronized void completedTask(String taskType, boolean success,
    int totalTasks) {
    if (!success) {
      System.err.println("A " + taskType + " task failed!");
      m_failed++;
    } else {
      m_completed++;
    }

    if (m_completed + m_failed == totalTasks) {
      if (m_failed > 0) {
        System.err.println("Problem executing " + taskType
          + " tasks - some iterations failed.");
      }

      block(false, totalTasks);
    }
  }

  private synchronized void block(boolean tf, int totalTasks) {
    if (tf) {
      try {
        if (m_completed + m_failed < totalTasks) {
          wait();
        }
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  /**
   * Start the pool of execution threads
   */
  protected void startExecutorPool() {
    if (m_executorPool != null) {
      m_executorPool.shutdownNow();
    }

    m_executorPool = new ThreadPoolExecutor(m_numExecutionSlots,
      m_numExecutionSlots, 120, TimeUnit.SECONDS,
      new LinkedBlockingQueue<Runnable>());
  }

  protected synchronized void trimZeroDistances(Neighborhood n) {
    int index = n.m_neighbors.numInstances();
    for (int i = 0; i < n.m_neighbors.numInstances(); i++) {
      if (n.m_distances[i] > 0) {
        index = i;
        break;
      }
    }

    if (index > 0) {
      // trim zero distances
      for (int i = 0; i < index; i++) {
        n.m_neighbors.remove(0);
      }
      double[] newDist = new double[n.m_distances.length - index];
      System.arraycopy(n.m_distances, index, newDist, 0, newDist.length);
      n.m_distances = newDist;
    }
  }

  /**
   * Computes LOF value for each training instance in sequential mode
   * 
   * @throws Exception if a problem occurs
   */
  protected void LOFFirstBatch() throws Exception {
    Instances training = getInputFormat();
    init(training);

    for (int i = 0; i < training.numInstances(); i++) {
      Instance current = training.get(i);
      DecisionTableHashKey key = new DecisionTableHashKey(current,
        current.numAttributes(), !m_classSet);
      if (!m_kDistanceContainer.containsKey(key)) {
        // allow for a few more neighbors than m_ubK in case of ties
        int nnFactor = 2;
        Instances nn = null;
        Neighborhood n = new Neighborhood();
        do {
          nn = m_nnSearch.kNearestNeighbours(current, m_ubK * nnFactor);
          n.m_neighbors = nn;
          n.m_distances = m_nnSearch.getDistances();
          trimZeroDistances(n);

          nnFactor++;
        } while (nn.numInstances() < m_ubK);

        n.m_tempCardinality = new double[m_ubK - m_lbK];
        n.m_lrd = new double[m_ubK - m_lbK];
        n.m_lof = new double[m_ubK - m_lbK];

        m_kDistanceContainer.put(key, n);
      }
      m_instKeys[i] = key;
    }

    // for each k in the range minPtsLB to maxPtsLB
    for (int k = m_lbK; k < m_ubK; k++) {

      // lrd's first
      for (int i = 0; i < training.numInstances(); i++) {
        Instance current = training.instance(i);

        Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);
        // this defines the k-neighborhood and may be larger than k because of
        // ties in
        // distance at the k-th nearest neighbor
        int indexOfKDistanceForK = k - 1;
        while (indexOfKDistanceForK < currentN.m_distances.length - 1
          && currentN.m_distances[indexOfKDistanceForK] == currentN.m_distances[indexOfKDistanceForK + 1]) {
          indexOfKDistanceForK++;
        }

        // lrd for current point with k nearest neighbors
        double cardinality = 0;
        double sumReachability = 0;

        for (int j = 0; j <= indexOfKDistanceForK; j++) {
          Instance b = currentN.m_neighbors.instance(j);
          cardinality += b.weight();
          sumReachability += reachability(current, b, currentN.m_distances[j],
            k);
        }

        currentN.m_lrd[k - m_lbK] = cardinality / sumReachability;

        // store the cardinality at k
        currentN.m_tempCardinality[k - m_lbK] = cardinality;
      }

      // now lof's
      for (int i = 0; i < training.numInstances(); i++) {
        Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);

        double lofK = lof(currentN, k);

        // set lof
        currentN.m_lof[k - m_lbK] = lofK;
      }
    }

    // make the output instances
    if (!m_classifierMode) {
      for (int i = 0; i < training.numInstances(); i++) {
        Instance current = training.instance(i);
        Neighborhood currentN = m_kDistanceContainer.get(m_instKeys[i]);

        // use the maximum LOF found over the range of k's explored
        double maxLOF = currentN.m_lof[Utils.maxIndex(currentN.m_lof)];
        Instance inst = makeOutputInstance(current, maxLOF);
        push(inst);
      }
    }
  }

  /**
   * Makes an output instance with LOF value as an attribute
   * 
   * @param original original instance
   * @param lof largest LOF value found in the range between the lower and upper
   *          bounds on k
   * 
   * @return the new instance
   */
  protected Instance makeOutputInstance(Instance original, double lof) {
    int numAtts = getInputFormat().numAttributes() + 1;

    double[] vals = new double[numAtts];

    // existing values
    for (int j = 0; j < original.numAttributes(); j++) {
      vals[j] = original.value(j);
    }

    // new lof value
    vals[numAtts - 1] = lof;
    Instance inst = null;
    if (original instanceof SparseInstance) {
      inst = new SparseInstance(original.weight(), vals);
    } else {
      inst = new DenseInstance(original.weight(), vals);
    }
    inst.setDataset(getOutputFormat());
    copyValues(inst, false, original.dataset(), getOutputFormat());
    inst.setDataset(getOutputFormat());

    return inst;
  }

  /**
   * Computes the LOF value for a particular instance A
   * 
   * @param neighborhoodA the neighborhood of A
   * @param k the k at which to compute the LOF
   * @return the LOF value
   */
  protected double lof(Neighborhood neighborhoodA, int k) {
    double sumlrdb = 0;

    int indexOfKDistanceForK = k - 1;
    while (indexOfKDistanceForK < neighborhoodA.m_distances.length - 1
      && neighborhoodA.m_distances[indexOfKDistanceForK] == neighborhoodA.m_distances[indexOfKDistanceForK + 1]) {
      indexOfKDistanceForK++;
    }

    for (int i = 0; i <= indexOfKDistanceForK; i++) {
      Instance b = neighborhoodA.m_neighbors.get(i);
      DecisionTableHashKey bkey = null;
      try {
        bkey = new DecisionTableHashKey(b, b.numAttributes(), !m_classSet);
      } catch (Exception ex) {
      }
      Neighborhood bTemp = m_kDistanceContainer.get(bkey);

      sumlrdb += bTemp.m_lrd[k - m_lbK];
    }

    return sumlrdb
      / (neighborhoodA.m_tempCardinality[k - m_lbK] * neighborhoodA.m_lrd[k
        - m_lbK]);
  }

  /**
   * Computes the reachability of instance a to instance b
   * 
   * @param a instance a
   * @param b instance b
   * @param distAB the distance between a and b
   * @param k the k at which to compute the reachability
   * @return the reachability
   */
  protected double reachability(Instance a, Instance b, double distAB, int k) {

    // k-distance for instance b
    DecisionTableHashKey bkey = null;
    try {
      bkey = new DecisionTableHashKey(b, b.numAttributes(), !m_classSet);
    } catch (Exception ex) {
    }
    Neighborhood bN = m_kDistanceContainer.get(bkey);

    // make k a zero-based index
    k--;
    double kDistanceB = bN.m_distances[k];
    while (k < bN.m_distances.length - 1 && kDistanceB == bN.m_distances[k + 1]) {
      k++;
      kDistanceB = bN.m_distances[k];
    }

    return Math.max(kDistanceB, distAB);
  }

  /**
   * Set environment variables to use
   * 
   * @param env the evnironment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
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
   * Main method for testing this class
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    runFilter(new LOF(), args);
  }
}
