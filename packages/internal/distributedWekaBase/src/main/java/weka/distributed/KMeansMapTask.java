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
 *    KMeansMapTask
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.clusterers.ClusterUtils;
import weka.clusterers.PreconstructedKMeans;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.StreamableFilterHelper;
import weka.core.Utils;
import weka.core.stats.NumericStats;
import weka.core.stats.Stats;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.filters.unsupervised.attribute.PreconstructedMissingValuesReplacer;
import distributed.core.DistributedJobConfig;

/**
 * Map task for k-means clustering. Uses a "pre-constructed" KMeans cluster
 * internally to perform the clustering (i.e. assigning training points to
 * clusters). This is constructed with the centroids found in the previous
 * iteration. Maintains (partial) summary stats on each centroid (by re-using
 * the ARFF header summary attributes mechanism). Can use an arbitrary number of
 * Streamable filters for preprocessing the data on the fly.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class KMeansMapTask implements OptionHandler, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -2423639970668815722L;

  protected Instances m_headerWithSummary;

  /**
   * The header to use after it has been through any filters that the user has
   * specified
   */
  protected Instances m_transformedHeaderNoSummary;

  /** The list of filters to use */
  protected List<Filter> m_filtersToUse = new ArrayList<Filter>();

  /** The missing values replacer to use */
  protected PreconstructedFilter m_missingValuesReplacer;

  protected boolean m_dontReplaceMissing;

  /**
   * The final pre-processing filter to use (encapsulating all specified filters
   * and the missing values replacer)
   */
  protected PreconstructedFilter m_finalFullPreprocess;

  protected NormalizableDistance m_distanceFunction = new EuclideanDistance();

  /** The current centroids */
  protected Instances m_centroids;

  /** The partial stats for each centroid */
  protected List<Map<String, Stats>> m_centroidSummaryStats;

  /** The KMeans instance to use for clustering the training data */
  protected PreconstructedKMeans m_kMeans;

  /**
   * True if the data goes through more than just the missing values replacement
   */
  protected boolean m_dataIsBeingTransformed;

  /**
   * Whether we should update the distance function (i.e to update range info)
   * with each incoming instance. If no filters (apart from missing) are
   * involved then we don't need to update (as the dummy priming data computed
   * in init() contains global min/max). If we are using filters, then the first
   * k-means iteration will need to update. However, if using filters, after the
   * first iteration the client should call setDummyDistancePrimingData() with
   * the priming data computed by the reduce task (at this point, all
   * transformed instances have been seen and the partial summary metadata for
   * the clusters can be used to compute global min/max in the transformed
   * space). This priming data should be retained somewhere and used in further
   * iterations if the distributed platform requires instantiation of new
   * map/reduce task objects for each pass over the data.
   */
  protected boolean m_updateDistanceFunction;

  /** Client will set this to true once the convergence criteria has been met */
  protected boolean m_converged;

  /**
   * Initilizes the map task. Configures any filters required.
   *
   * @param headerWithSummary header of the incoming instances with summary
   *          attributes included
   * @return the header (without summary attributes) after it has been through
   *         any filters that the user may have specified. This structure is
   *         needed by the KMeansReduceTask
   * @throws DistributedWekaException
   */
  public Instances init(Instances headerWithSummary)
    throws DistributedWekaException {
    // to be called after setOptions();

    m_headerWithSummary = headerWithSummary;

    m_transformedHeaderNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(m_headerWithSummary);
    Instances dummyDistancePrimer =
      ClusterUtils.getPrimingDataForDistanceFunction(m_headerWithSummary);

    // deal with filters
    if (!m_dontReplaceMissing) {
      try {
        m_missingValuesReplacer =
          new PreconstructedMissingValuesReplacer(m_headerWithSummary);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    configureFilters(m_transformedHeaderNoSummary);

    if (!m_dataIsBeingTransformed) {
      m_distanceFunction.setInstances(dummyDistancePrimer);
    } else {
      m_distanceFunction.setInstances(((Filter) m_finalFullPreprocess)
        .getOutputFormat());
      m_updateDistanceFunction = true;
    }

    m_kMeans = new PreconstructedKMeans();
    try {
      m_kMeans.setDistanceFunction(m_distanceFunction);
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    // apply any filters to our header (sans summary atts)
    try {
      m_transformedHeaderNoSummary = applyFilters(m_transformedHeaderNoSummary);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    return m_transformedHeaderNoSummary;
  }

  /**
   * Set the dummy priming data (two-instance dataset that contains global
   * min/max for numeric attributes) for the distance function to use when
   * normalizing numeric attributes. This method should be called when filters
   * that transform the data are being used, and *after* the first iteration of
   * k-means has completed. At this point, the reduce task can compute global
   * min/max for transformed attributes using the partial summary metadata for
   * the clusters computed in the first iteration
   *
   * @param priming the dummy priming data to use in the distance function
   */
  public void setDummyDistancePrimingData(Instances priming)
    throws DistributedWekaException {
    if (m_kMeans == null) {
      throw new DistributedWekaException("Must call init() first");
    }

    m_distanceFunction = new EuclideanDistance();
    m_distanceFunction.setInstances(priming);
    try {
      m_kMeans.setDistanceFunction(m_distanceFunction);
      m_updateDistanceFunction = false;
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  /**
   * Processes a training instance. Uses the k-means clusterer to find the
   * nearest centroid to the supplied instance and then updates the summary meta
   * data header for the corresponding centroid with the training instance.
   *
   * @param toProcess the instance to process
   * @throws DistributedWekaException if a problem occurs
   */
  public void processInstance(Instance toProcess)
    throws DistributedWekaException {
    if (m_centroids == null) {
      throw new DistributedWekaException("No centroids set!");
    }

    try {
      Instance filteredInstance = applyFilters(toProcess);

      int bestCluster =
        m_kMeans.clusterProcessedInstance(/** (Filter) m_finalFullPreprocess */
        null,
          filteredInstance, m_updateDistanceFunction, null);

      Map<String, Stats> summaryStats = m_centroidSummaryStats.get(bestCluster);
      for (int i = 0; i < m_transformedHeaderNoSummary.numAttributes(); i++) {
        if (m_transformedHeaderNoSummary.attribute(i).isNominal()
          || m_transformedHeaderNoSummary.attribute(i).isNumeric()) {

          boolean isNominal = filteredInstance.attribute(i).isNominal();
          CSVToARFFHeaderMapTask
            .updateSummaryStats(
              summaryStats, null,
              m_transformedHeaderNoSummary
                .attribute(i)
                .name(),
              !filteredInstance.isMissing(i) && isNominal ? 1.0
                : filteredInstance.value(i),
              !filteredInstance.isMissing(i) && isNominal ? filteredInstance
                .stringValue(i)
                : null, isNominal, false,
              false, false, NumericStats.Q_COMPRESSION);
        }
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }
  }

  /**
   * Computes the distance between the two supplied instances
   *
   * @param one the first instance
   * @param two the second instance
   * @return the distance between the two
   * @throws DistributedWekaException if a problem occurs
   */
  public double distance(Instance one, Instance two)
    throws DistributedWekaException {
    if (m_kMeans == null) {
      throw new DistributedWekaException(
        "[KMeansMapTask] We haven't been initialized yet");
    }

    return m_kMeans.getDistanceFunction().distance(one, two);
  }

  /**
   * Get the summary stats for each centroid
   *
   * @return the summary stats (one instances object) for each centroid
   */
  public List<Instances> getCentroidStats() {

    double[] clusterErrors = m_kMeans.getErrorsForClusters();

    List<Instances> centerStats = new ArrayList<Instances>();
    for (int i = 0; i < m_centroids.numInstances(); i++) {
      Map<String, Stats> centroidStats = m_centroidSummaryStats.get(i);
      ArrayList<Attribute> atts = new ArrayList<Attribute>();

      for (int j = 0; j < m_transformedHeaderNoSummary.numAttributes(); j++) {
        atts.add((Attribute) m_transformedHeaderNoSummary.attribute(j).copy());
      }

      boolean ok = true;
      for (int j = 0; j < m_transformedHeaderNoSummary.numAttributes(); j++) {
        if (m_transformedHeaderNoSummary.attribute(j).isNominal()
          || m_transformedHeaderNoSummary.attribute(j).isNumeric()) {
          Stats s =
            centroidStats.get(m_transformedHeaderNoSummary.attribute(j).name());
          // if any stats attribute is null then it means no instances
          // were assigned to this centroid
          if (s == null) {
            System.err.println("No instances for centroid: " + i + " "
              + m_centroids.instance(i));
            ok = false;
            break;
          }
          atts.add(s.makeAttribute());
        }
      }

      if (ok) {
        // we embed the partial error for each cluster in the relation name
        Instances cStats =
          new Instances("Partial stats for centroid " + i + " : "
            + clusterErrors[i], atts, 0);
        centerStats.add(cStats);
      } else {
        centerStats.add(null);
      }
    }

    return centerStats;
  }

  /**
   * Configures filters to use when clustering
   *
   * @param headerNoSummary the header of the training data sans summary
   *          attributes
   * @throws DistributedWekaException if a problem occurs
   */
  protected void configureFilters(Instances headerNoSummary)
    throws DistributedWekaException {
    // setOptions() will have set up the pre-processing filters. Now
    // we just adjust the final set depending on whether missing values
    // are to be replaced as well. We always want missing values first
    // in the list so that it processes the original data
    List<StreamableFilter> filters = new ArrayList<StreamableFilter>();
    if (!getDontReplaceMissingValues()) {
      filters.add((StreamableFilter) m_missingValuesReplacer);
    }
    if (m_filtersToUse != null && m_filtersToUse.size() > 0) {
      for (Filter f : m_filtersToUse) {
        if (!(f instanceof StreamableFilter)) {
          throw new DistributedWekaException("Filter " + f.getClass().getName()
            + " is not a StreamableFilter!");
        }

        filters.add((StreamableFilter) f);
      }
    }

    if (filters.size() > 0) {
      try {
        m_finalFullPreprocess =
          StreamableFilterHelper.wrapStreamableFilters(filters);

        if (filters.size() > 1) {
          m_dataIsBeingTransformed = true;
        }
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

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontReplaceMissingValuesTipText() {
    return "Don't replace missing values globally with mean/mode.";
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
   * Get the distance function in use
   *
   * @return the distance function in use
   */
  public NormalizableDistance getDistanceFunction() {
    return m_distanceFunction;
  }

  /**
   * Set the cluster centroids to use for this iteration. NOTE: These should be
   * in the transformed space if any filters (including missing values
   * replacement) are being used.
   *
   * @param centers the centroids to use
   */
  public void setCentroids(Instances centers) {
    m_centroids = centers;
    m_kMeans.setClusterCentroids(centers);

    m_centroidSummaryStats = new ArrayList<Map<String, Stats>>();
    for (int i = 0; i < centers.numInstances(); i++) {
      m_centroidSummaryStats.add(new HashMap<String, Stats>());
    }
  }

  /**
   * Apply the filters (if any) setup for this map task to the supplied
   * instances
   *
   * @param toApplyTo the instances to filer
   * @return a filtered set of instances
   * @throws Exception if a problem occurs
   */
  public Instances applyFilters(Instances toApplyTo) throws Exception {
    Instances result = toApplyTo;
    if (m_finalFullPreprocess != null) {
      result =
        new Instances(((Filter) m_finalFullPreprocess).getOutputFormat(), 0);
      for (int i = 0; i < toApplyTo.numInstances(); i++) {
        ((Filter) m_finalFullPreprocess).input(toApplyTo.instance(i));
        Instance processed = ((Filter) m_finalFullPreprocess).output();
        result.add(processed);
      }
    }

    return result;
  }

  /**
   * Apply the filters (if any) for this map task to the supplied instance
   *
   * @param original the instance in the original space
   * @return a filtered instance
   * @throws Exception if a problem occurs
   */
  public Instance applyFilters(Instance original) throws Exception {
    Instance result = original;

    if (m_finalFullPreprocess != null) {
      ((Filter) m_finalFullPreprocess).input(result);
      result = ((Filter) m_finalFullPreprocess).output();
    }

    return result;
  }

  /**
   * Gets the full set of preprocessing filters
   *
   * @return preprocessing filter(s) or null if no preprocessing/missing values
   *         handling is being done
   */
  public Filter getPreprocessingFilters() {
    return (Filter) m_finalFullPreprocess;
  }

  /**
   * Set whether the run of k-means that this map is associated with has
   * converged or not
   *
   * @param converged true if the run has converged
   */
  public void setConverged(boolean converged) {
    m_converged = converged;
  }

  /**
   * Get whether the run of k-means that this map tasks is associated with has
   * converged
   * 
   * @return true if the run has converged
   */
  public boolean getConverged() {
    return m_converged;
  }

  /**
   * Get the header of the data after it has been through any pre-processing
   * filters specified by the user
   *
   * @return the transformed header
   */
  public Instances getTransformedHeader() {
    return m_transformedHeaderNoSummary;
  }

  /**
   * Get the user-specified filters to use with the k-means clusterer. Does not
   * include the missing values replacement filter that is automatically
   * configured using global ARFF profiling summary data
   *
   * @return the user-specified filters to use with k-means
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
   * Set the user-specified filters to use with the k-means clusterer. Does not
   * include the missing values replacement filter that is automatically
   * configured using global ARFF profiling summary data
   *
   * @param toUse the user-specified filters to use with k-means
   */
  public void setFiltersToUse(Filter[] toUse) {
    m_filtersToUse.clear();

    if (toUse != null && toUse.length > 0) {
      for (Filter f : toUse) {
        if (!(f instanceof PreconstructedFilter)
          && f instanceof StreamableFilter) {
          m_filtersToUse.add(f);
        }
      }
    }
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String filtersToUseTipText() {
    return "Filters to pre-process the data with before "
      + "passing it to k-means. Note that only StreamableFilters can be used.";
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.addElement(new Option(
      "\tDon't replace missing values with mean/mode when "
        + "running in batch mode.", "dont-replace-missing", 0,
      "-dont-replace-missing"));

    Enumeration<Option> fOpts = StreamableFilterHelper.listOptions();
    while (fOpts.hasMoreElements()) {
      options.add(fOpts.nextElement());
    }

    return options.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    setDontReplaceMissingValues(Utils.getFlag("dont-replace-missing", options));

    m_filtersToUse = new ArrayList<Filter>();
    while (true) {
      String filterString = Utils.getOption("filter", options);
      if (DistributedJobConfig.isEmpty(filterString)) {
        break;
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

    if (getDontReplaceMissingValues()) {
      options.add("-dont-replace-missing");
    }

    if (m_filtersToUse != null) {
      for (Filter f : m_filtersToUse) {
        options.add("-filter");
        options.add(StreamableFilterHelper.getFilterSpec(f));
      }
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Utility method to choose start points for a number of runs of k-means given
   * a list of randomly selected instance objects. Avoids choosing duplicate
   * instances as start points for each run.
   *
   * @param numRuns the numeber of runs of k-means
   * @param numClusters the number of clusters/start points for each run
   * @param candidates the list of total candidates to choose randomly from
   * @param headerNoSummary the header of the data
   * @return a list of Instances (one for each run)
   * @throws DistributedWekaException if a problem occurs
   */
  public static List<Instances> assignStartPointsFromList(int numRuns,
    int numClusters, List<Instance> candidates, Instances headerNoSummary)
    throws DistributedWekaException {

    HashSet<DecisionTableHashKey> duplicateCheck;

    /** List of centers - one set of instances/centers per run */
    List<Instances> currentCenters = new ArrayList<Instances>();
    for (int i = 0; i < numRuns; i++) {
      duplicateCheck = new HashSet<DecisionTableHashKey>();
      Instances centers = new Instances(headerNoSummary, 0);
      int index = candidates.size() - 1;
      while (centers.numInstances() < numClusters && index >= 0) {
        Instance current = candidates.get(index);
        if (!duplicateCheck.contains(current)) {
          centers.add(current);
          try {
            duplicateCheck.add(new DecisionTableHashKey(current,
              headerNoSummary.numAttributes(), true));
          } catch (Exception ex) {
            throw new DistributedWekaException(ex);
          }
          if (index == candidates.size() - 1) {
            candidates.remove(candidates.size() - 1);
          } else {
            // swap
            candidates.set(index, candidates.get(candidates.size() - 1));
            candidates.remove(candidates.size() - 1);
          }
        }
        index--;
      }

      if (centers.numInstances() == 0) {
        throw new DistributedWekaException(
          "Unable to find distinct initial centers!");
      }
      currentCenters.add(centers);
    }

    return currentCenters;
  }
}
