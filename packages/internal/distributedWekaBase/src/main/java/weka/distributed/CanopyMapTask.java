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
 *    CanopyMapTask.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.clusterers.Canopy;
import weka.clusterers.Clusterer;
import weka.clusterers.PreconstructedFilteredClusterer;
import weka.core.Attribute;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.StreamableFilterHelper;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NumericStats;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.filters.unsupervised.attribute.PreconstructedMissingValuesReplacer;
import distributed.core.DistributedJobConfig;

/**
 * Map task for building partial canopies
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CanopyMapTask implements OptionHandler, EnvironmentHandler,
  Serializable {

  /** For serialization */
  private static final long serialVersionUID = 5107020708019202338L;

  /** Environment variables */
  protected transient Environment m_env;

  /** Training data header */
  protected Instances m_header;

  /** Canopy clusterer */
  protected Canopy m_canopy;

  /** The list of filters to use */
  protected List<Filter> m_filtersToUse;

  /** The missing values replacer to use */
  protected PreconstructedFilter m_missingValuesReplacer;

  /**
   * The final pre-processing filter to use (encapsulating all specified filters
   * and the missing values replacer. This can be null if all we are using is
   * missing values replacement. In this case the missing values replacer gets
   * set directly on the canopy clusterer
   */
  protected PreconstructedFilter m_finalFullPreprocess;

  protected String m_userT1 = "" + Canopy.DEFAULT_T1;
  protected String m_userT2 = "" + Canopy.DEFAULT_T2;

  /** Requested number of clusters */
  protected String m_numClusters = "2";

  /**
   * Prune low-density candidate canopies after every x instances have been seen
   */
  protected String m_periodicPruningRate = "10000";

  /**
   * The minimum cluster density (according to T2 distance) allowed. Used when
   * periodically pruning candidate canopies
   */
  protected String m_minClusterDensity = "2";

  /** The maximum number of candidate canopies to hold in memory at any one time */
  protected String m_maxCanopyCandidates = "100";

  /** If true then don't replace missing values with global means/modes */
  protected boolean m_dontReplaceMissing;

  /** heuristic value for T1 */
  public double m_hT1 = -1;

  /** heuristic value for T2 */
  public double m_hT2 = -1;

  /** True once all updates are completed and updateFinished() has been called */
  protected boolean m_finalized;

  /**
   * Substitute environment variables in the supplied string.
   * 
   * @param orig the string to modify
   * @return the string with environment variables resolved
   */
  public String environmentSubstitute(String orig) {
    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    if (m_env != null) {
      try {
        orig = m_env.substitute(orig);
      } catch (Exception ex) {
        // not interested if there are no variables substituted
      }
    }

    return orig;
  }

  public void init(Instances headerWithSummary) throws DistributedWekaException {
    // to be called after setOptions();

    m_header = headerWithSummary;

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(m_header);
    Instances dummyDistancePrimer =
      CanopyReduceTask.getPrimingDataForDistanceFunction(m_header);

    // heuristic T2
    m_hT2 = getHeuristicT2(headerWithSummary);

    // deal with filters
    if (!m_dontReplaceMissing) {
      try {
        m_missingValuesReplacer =
          new PreconstructedMissingValuesReplacer(m_header);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }
    configureFilters(headerNoSummary);

    configureCanopyClusterer(headerNoSummary, dummyDistancePrimer);
  }

  public void update(Instance inst) throws DistributedWekaException {
    if (m_canopy == null) {
      throw new DistributedWekaException(
        "CanopyMapTask has not been initialized yet!");
    }

    if (m_finalized) {
      throw new DistributedWekaException(
        "This map task has been finalized - can't process any more updates");
    }

    Instance toProcess = inst;
    if (m_finalFullPreprocess != null) {
      try {
        ((Filter) m_finalFullPreprocess).input(toProcess);
        toProcess = ((Filter) m_finalFullPreprocess).output();

        if (toProcess == null) {
          throw new Exception(
            "Preprocessing filter did not make instance available immediately!");
        }
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    try {
      m_canopy.updateClusterer(toProcess);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }
  }

  public void updateFinished() {
    if (m_canopy != null && !m_finalized) {
      m_canopy.updateFinished();
      m_finalized = true;
    }
  }

  public Clusterer getFinalizedClusterer() throws DistributedWekaException {
    if (m_canopy == null) {
      throw new DistributedWekaException(
        "CanopyMapTask has not been initialized yet!");
    }

    if (!m_finalized) {
      throw new DistributedWekaException(
        "This map task has note been finalized yet!");
    }

    if (m_finalFullPreprocess == null) {
      return m_canopy;
    }

    PreconstructedFilteredClusterer fc = new PreconstructedFilteredClusterer();
    fc.setFilter((Filter) m_finalFullPreprocess);
    fc.setClusterer(m_canopy);

    return fc;
  }

  protected void configureFilters(Instances headerNoSummary)
    throws DistributedWekaException {
    // setOptions() will have set up the pre-processing filters. Now
    // we just adjust the final set depending on whether missing values
    // are to be replaced as well. We always want missing values first
    // in the list so that it processes the original data
    if (m_filtersToUse != null && m_filtersToUse.size() > 0) {
      List<StreamableFilter> filters = new ArrayList<StreamableFilter>();
      if (!getDontReplaceMissingValues()) {
        filters.add((StreamableFilter) m_missingValuesReplacer);
      }
      for (Filter f : m_filtersToUse) {
        if (!(f instanceof StreamableFilter)) {
          throw new DistributedWekaException("Filter " + f.getClass().getName()
            + " is not a StreamableFilter!");
        }

        filters.add((StreamableFilter) f);
      }

      try {
        m_finalFullPreprocess =
          StreamableFilterHelper.wrapStreamableFilters(filters);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    if (m_finalFullPreprocess != null) {
      try {
        ((Filter) m_finalFullPreprocess).setInputFormat(headerNoSummary);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }
  }

  protected void configureCanopyClusterer(Instances headerNoSummary,
    Instances dummyDistancePrimer) throws DistributedWekaException {

    m_canopy = new ECanopy();
    if (!DistributedJobConfig.isEmpty(getMaxNumCanopies())) {
      String nC = environmentSubstitute(getMaxNumCanopies());
      System.err.println("[CanopyMap] max canopy clusters: " + nC);
      try {
        m_canopy.setNumClusters(Integer.parseInt(nC));
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    if (!DistributedJobConfig
      .isEmpty(getMaxNumCandidateCanopiesToHoldInMemory())) {
      m_canopy
        .setMaxNumCandidateCanopiesToHoldInMemory(Integer
          .parseInt(environmentSubstitute(getMaxNumCandidateCanopiesToHoldInMemory())));
    }

    if (!DistributedJobConfig.isEmpty(getPeriodicPruningRate())) {
      m_canopy.setPeriodicPruningRate(Integer
        .parseInt(environmentSubstitute(getPeriodicPruningRate())));
    }

    if (!DistributedJobConfig.isEmpty(getMinimumCanopyDensity())) {
      m_canopy.setMinimumCanopyDensity(Double
        .parseDouble(environmentSubstitute(getMinimumCanopyDensity())));
    }

    double userT2 = Double.parseDouble(environmentSubstitute(m_userT2));
    if (userT2 > 0) {
      m_hT2 = userT2;
    }

    m_canopy.setT2(m_hT2);

    double userT1 = Double.parseDouble(environmentSubstitute(m_userT1));
    m_hT1 = userT1 > 0 ? userT1 : -userT1 * m_hT2;
    m_canopy.setT1(m_hT1);

    // Set missing values replacer directly on the canopy clusterer
    // if there are no other pre-processing filters
    if (m_filtersToUse == null && m_missingValuesReplacer != null) {
      m_canopy.setMissingValuesReplacer((Filter) m_missingValuesReplacer);
    }

    try {
      Instances initInsts = headerNoSummary;
      if (m_finalFullPreprocess != null) {
        initInsts = ((Filter) m_finalFullPreprocess).getOutputFormat();
      }
      m_canopy.buildClusterer(initInsts);

      // if there are any other filters (besides missing values)
      // in play then we can't initialize the distance function
      // with min/max dummy data (since we'd need the min/max
      // attribute info from the transformed data)
      if (m_finalFullPreprocess == null) {
        m_canopy.initializeDistanceFunction(dummyDistancePrimer);
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  public static double getHeuristicT2(Instances headerWithSummary)
    throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    double[] mins = new double[headerNoSummary.numAttributes()];
    double[] maxes = new double[headerNoSummary.numAttributes()];
    double normalizedStdDevSum = 0;

    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      Attribute orig = headerNoSummary.attribute(i);
      Attribute summary =
        headerWithSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + orig.name());

      if (orig.isNumeric()) {

        // number of non-missing values
        double count =
          NumericStats.attributeToStats(summary).getStats()[ArffSummaryNumericMetric.COUNT
            .ordinal()];
        if (count > 2) {
          mins[i] =
            NumericStats.attributeToStats(summary).getStats()[ArffSummaryNumericMetric.MIN
              .ordinal()];
          maxes[i] =
            NumericStats.attributeToStats(summary).getStats()[ArffSummaryNumericMetric.MAX
              .ordinal()];

          double stdD =
            NumericStats.attributeToStats(summary).getStats()[ArffSummaryNumericMetric.STDDEV
              .ordinal()];
          if (!Utils.isMissingValue(stdD) && maxes[i] - mins[i] > 0) {
            stdD = 0.5 * stdD / (maxes[i] - mins[i]);
            normalizedStdDevSum += stdD;
          }
        }
      } else if (orig.isNominal()) {
        // doesn't matter for non numeric attributes
        mins[i] = Utils.missingValue();
        maxes[i] = Utils.missingValue();
        normalizedStdDevSum += 0.25;
      }
    }

    normalizedStdDevSum = Math.sqrt(normalizedStdDevSum);

    return normalizedStdDevSum > 0 ? normalizedStdDevSum : 0;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.addElement(new Option(
      "\tMaximum mumber of canopies to find in each map task.\n"
        + "\t(default 2).", "max-map-canopies", 1, "-max-map-canopies <num>"));

    options
      .addElement(new Option(
        "\tMaximum number of candidate canopies to retain in memory\n\t"
          + "at any one time. T2 distance plus, data characteristics,\n\t"
          + "will determine how many candidate canopies are formed before\n\t"
          + "periodic and final pruning are performed, which might result\n\t"
          + "in exceess memory consumption. This setting avoids large numbers\n\t"
          + "of candidate canopies consuming memory. (default = 100)",
        "-max-candidates", 1, "-max-candidates <num>"));

    options.addElement(new Option(
      "\tHow often to prune low density canopies. \n\t"
        + "(default = every 10,000 training instances)", "periodic-pruning", 1,
      "-periodic-pruning <num>"));

    options.addElement(new Option(
      "\tMinimum canopy density, below which a canopy will be pruned\n\t"
        + "during periodic pruning. (default = 2 instances)", "min-density", 1,
      "-min-density"));

    options
      .addElement(new Option(
        "\tThe T2 distance to use in the map phase. Values < 0 indicate that\n\t"
          + "a heuristic based on attribute std. deviation should be used to set this.\n\t"
          + "(default = -1.0)", "t2-map", 1, "-t2-map <num>"));

    options
      .addElement(new Option(
        "\tThe T1 distance to use in the map phase. A value < 0 is taken as a\n\t"
          + "positive multiplier for T2. (default = -1.5)", "t1-map", 1,
        "-t1-map <num>"));

    options.addElement(new Option(
      "\tDon't replace missing values with mean/mode when "
        + "running in batch mode.\n", "dont-replace-missing", 0,
      "-dont-replace-missing"));

    Enumeration<Option> fOpts = StreamableFilterHelper.listOptions();
    while (fOpts.hasMoreElements()) {
      options.add(fOpts.nextElement());
    }

    return options.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    String temp = Utils.getOption("max-map-canopies", options);
    if (temp.length() > 0) {
      setMaxNumCanopies(temp);
    }

    setDontReplaceMissingValues(Utils.getFlag("dont-replace-missing", options));

    temp = Utils.getOption("max-candidates", options);
    if (temp.length() > 0) {
      setMaxNumCandidateCanopiesToHoldInMemory(temp);
    }

    temp = Utils.getOption("periodic-pruning", options);
    if (temp.length() > 0) {
      setPeriodicPruningRate(temp);
    }

    temp = Utils.getOption("min-density", options);
    if (temp.length() > 0) {
      setMinimumCanopyDensity(temp);
    }

    temp = Utils.getOption("t2-map", options);
    if (temp.length() > 0) {
      setT2MapPhase(temp);
    }

    temp = Utils.getOption("t1-map", options);
    if (temp.length() > 0) {
      setT1MapPhase(temp);
    }

    while (true) {
      String filterString = Utils.getOption("filter", options);
      if (DistributedJobConfig.isEmpty(filterString)) {
        break;
      }

      if (m_filtersToUse == null) {
        m_filtersToUse = new ArrayList<Filter>();
      }

      String[] spec = Utils.splitOptions(filterString);
      if (spec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid filter specification string");
      }
      String filterClass = spec[0];
      Filter f = (Filter) Class.forName(filterClass).newInstance();
      spec[0] = "";
      if (f instanceof OptionHandler) {
        ((OptionHandler) f).setOptions(spec);
      }
      m_filtersToUse.add(f);
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    options.add("-max-map-canopies");
    options.add(getMaxNumCanopies());

    options.add("-max-candidates");
    options.add("" + getMaxNumCandidateCanopiesToHoldInMemory());
    options.add("-periodic-pruning");
    options.add("" + getPeriodicPruningRate());
    options.add("-min-density");
    options.add("" + getMinimumCanopyDensity());

    if (getDontReplaceMissingValues()) {
      options.add("-dont-replace-missing");
    }

    options.add("-t2-map");
    options.add(getT2MapPhase());
    options.add("-t1-map");
    options.add(getT1MapPhase());

    if (m_filtersToUse != null) {
      for (Filter f : m_filtersToUse) {
        options.add("-filter");
        options.add(StreamableFilterHelper.getFilterSpec(f));
      }
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Set the maximum number of clusters to find in this map task. Fewer clusters
   * may be found than the specified maximum.
   * 
   * @param c the maximum number of clusters to find
   */
  public void setMaxNumCanopies(String c) {
    m_numClusters = c;
  }

  /**
   * Get the number of clusters to find by this map task. Fewer clusters may be
   * found than the specified maximum.
   * 
   * @return the maximum number of clusters to find
   */
  public String getMaxNumCanopies() {
    return m_numClusters;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String periodicPruningRateTipText() {
    return "How often to prune low density canopies during training";
  }

  /**
   * Set the how often to prune low density canopies during training
   * 
   * @param p how often (every p instances) to prune low density canopies
   */
  public void setPeriodicPruningRate(String p) {
    m_periodicPruningRate = p;
  }

  /**
   * Get the how often to prune low density canopies during training
   * 
   * @return how often (every p instances) to prune low density canopies
   */
  public String getPeriodicPruningRate() {
    return m_periodicPruningRate;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minimumCanopyDensityTipText() {
    return "The minimum T2-based density below which a canopy will be pruned during periodic pruning";
  }

  /**
   * Set the minimum T2-based density below which a canopy will be pruned during
   * periodic pruning.
   * 
   * @param dens the minimum canopy density
   */
  public void setMinimumCanopyDensity(String dens) {
    m_minClusterDensity = dens;
  }

  /**
   * Get the minimum T2-based density below which a canopy will be pruned during
   * periodic pruning.
   * 
   * @return the minimum canopy density
   */
  public String getMinimumCanopyDensity() {
    return m_minClusterDensity;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxNumCandidateCanopiesToHoldInMemory() {
    return "The maximum number of candidate canopies to retain in main memory during training. "
      + "T2 distance and data characteristics determine how many candidate "
      + "canopies are formed before periodic and final pruning are performed. There "
      + "may not be enough memory available if T2 is set too low.";
  }

  /**
   * Set the maximum number of candidate canopies to retain in memory during
   * training. T2 distance and data characteristics determine how many candidate
   * canopies are formed before periodic and final pruning are performed. There
   * may not be enough memory available if T2 is set too low.
   * 
   * @param max the maximum number of candidate canopies to retain in memory
   *          during training
   */
  public void setMaxNumCandidateCanopiesToHoldInMemory(String max) {
    m_maxCanopyCandidates = max;
  }

  /**
   * Get the maximum number of candidate canopies to retain in memory during
   * training. T2 distance and data characteristics determine how many candidate
   * canopies are formed before periodic and final pruning are performed. There
   * may not be enough memory available if T2 is set too low.
   * 
   * @return the maximum number of candidate canopies to retain in memory during
   *         training
   */
  public String getMaxNumCandidateCanopiesToHoldInMemory() {
    return m_maxCanopyCandidates;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontReplaceMissingValuesTipText() {
    return "Replace missing values globally with mean/mode.";
  }

  /**
   * Sets whether missing values are to be replaced.
   * 
   * @param r true if missing values are to be replaced
   */
  public void setDontReplaceMissingValues(boolean r) {
    m_dontReplaceMissing = r;
  }

  /**
   * Gets whether missing values are to be replaced.
   * 
   * @return true if missing values are to be replaced
   */
  public boolean getDontReplaceMissingValues() {
    return m_dontReplaceMissing;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String t1MapPhaseTipText() {
    return "The T1 distance to use. Values < 0 are taken as a positive "
      + "multiplier for the T2 distance";
  }

  /**
   * Set the T1 distance. Values < 0 are taken as a positive multiplier for the
   * T2 distance - e.g. T1_actual = Math.abs(t1) * t2;
   * 
   * @param t1 the T1 distance to use
   */
  public void setT1MapPhase(String t1) {
    m_userT1 = t1;
  }

  /**
   * Get the T1 distance. Values < 0 are taken as a positive multiplier for the
   * T2 distance - e.g. T1_actual = Math.abs(t1) * t2;
   * 
   * @return the T1 distance to use
   */
  public String getT1MapPhase() {
    return m_userT1;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String t2MapPhaseTipText() {
    return "The T2 distance to use. Values < 0 indicate that this should be set using "
      + "a heuristic based on attribute standard deviation (note that this only"
      + "works when batch training)";
  }

  /**
   * Set the T2 distance to use. Values < 0 indicate that a heuristic based on
   * attribute standard deviation should be used to set this (note that the
   * heuristic is only applicable when batch training).
   * 
   * @param t2 the T2 distance to use
   */
  public void setT2MapPhase(String t2) {
    m_userT2 = t2;
  }

  /**
   * Get the T2 distance to use. Values < 0 indicate that a heuristic based on
   * attribute standard deviation should be used to set this (note that the
   * heuristic is only applicable when batch training).
   * 
   * @return the T2 distance to use
   */
  public String getT2MapPhase() {
    return m_userT2;
  }

  /**
   * Get the filters to wrap up with the base classifier
   * 
   * @return the filters to wrap up with the base classifier
   */
  public Filter[] getFiltersToUse() {
    List<Filter> finalList = new ArrayList<Filter>();
    for (Filter f : m_filtersToUse) {
      if (!(f instanceof PreconstructedFilter)) {
        finalList.add(f);
      }
    }

    Filter[] result = new Filter[finalList.size()];
    int count = 0;
    for (Filter f : m_filtersToUse) {
      if (!(f instanceof PreconstructedFilter)) {
        result[count++] = f;
      }
    }

    return result;
  }

  /**
   * Set the filters to wrap up with the base classifier
   * 
   * @param toUse filters to wrap up with the base classifier
   */
  public void setFiltersToUse(Filter[] toUse) {
    m_filtersToUse.clear();

    if (toUse != null && toUse.length > 0) {
      for (Filter f : toUse) {
        if (!(f instanceof PreconstructedFilter)) {
          m_filtersToUse.add(f);
        }
      }
    }
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Subclass of Canopy that gives us access to the distance function
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class ECanopy extends Canopy implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -1411760192553529471L;

    public NormalizableDistance getDistanceFunction() {
      return m_distanceFunction;
    }
  }

}
