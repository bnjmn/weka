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
 *    CanopyReduceTask.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import weka.clusterers.Canopy;
import weka.clusterers.Clusterer;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.PreconstructedFilteredClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NumericStats;
import weka.distributed.CanopyMapTask.ECanopy;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PreconstructedMissingValuesReplacer;

/**
 * Reduce task for building a canopy clusterer
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class CanopyReduceTask implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = -4795209402122764790L;

  /** Maximum final number of clusters/canopies */
  protected int m_maxFinalNumCanopies = 2;

  /** If true then don't replace missing values */
  protected boolean m_dontReplaceMissingValues;

  /** T2 for aggregation */
  protected double m_aggregationT2 = Canopy.DEFAULT_T2;

  /** T1 for aggregation */
  protected double m_aggregationT1 = Canopy.DEFAULT_T1;

  public void setMaxFinalNumCanopies(int max) {
    m_maxFinalNumCanopies = max;
  }

  public void setAggregationT1(double t1) {
    m_aggregationT1 = t1;
  }

  public void setAggregationT2(double t2) {
    m_aggregationT2 = t2;
  }

  /**
   * Initializes the final distance function using range information in the
   * distance functions of the individual Canopy clusterers. We use this
   * initialization when there is more than just a missing values filter being
   * used because, in this case, the min/max info in the global attribute
   * summary info is not applicable (i.e. filter(s) might transform or create
   * new attributes for which we don't have summary information for in the
   * global ARFF header).
   * 
   * @param clist the list of individual Canopy clusterers
   * @param finalDistance the distance function to initialize
   * @throws Exception if a problem occurs
   */
  protected void initFinalDistanceFunctionFiltersInPlay(List<Canopy> clist,
    NormalizableDistance finalDistance) throws Exception {

    Instances filteredStructure =
      new Instances(((ECanopy) clist.get(0)).getDistanceFunction()
        .getInstances(), 0);

    double[] globalMax = new double[filteredStructure.numAttributes()];
    double[] globalMin = new double[filteredStructure.numAttributes()];

    double[][] ranges =
      ((ECanopy) clist.get(0)).getDistanceFunction().getRanges();
    for (int i = 0; i < filteredStructure.numAttributes(); i++) {
      globalMin[i] = ranges[i][NormalizableDistance.R_MIN];
      globalMax[i] = ranges[i][NormalizableDistance.R_MAX];
    }

    for (int i = 1; i < clist.size(); i++) {
      ECanopy currentC = ((ECanopy) clist.get(i));
      ranges = currentC.getDistanceFunction().getRanges();
      for (int k = 0; k < filteredStructure.numAttributes(); k++) {
        if (ranges[k][NormalizableDistance.R_MIN] < globalMin[k]) {
          globalMin[k] = ranges[k][NormalizableDistance.R_MIN];
        }

        if (ranges[k][NormalizableDistance.R_MAX] > globalMax[k]) {
          globalMax[k] = ranges[k][NormalizableDistance.R_MAX];
        }
      }
    }

    for (int i = 0; i < filteredStructure.numAttributes(); i++) {
      if (filteredStructure.attribute(i).isNominal()) {
        // doesn't matter for non-numeric
        globalMin[i] = Utils.missingValue();
        globalMax[i] = Utils.missingValue();
      }
    }

    filteredStructure.add(new DenseInstance(1.0, globalMin));
    filteredStructure.add(new DenseInstance(1.0, globalMax));

    finalDistance.setInstances(filteredStructure);
  }

  public Clusterer reduceCanopies(List<Clusterer> canopies,
    Instances headerWithSummary) throws DistributedWekaException {

    if (m_aggregationT2 < 0) {
      System.err
        .println("[CanopyReduceTask] aggregation T2 < 0 - using heuristic (-T2 * heursticT2).");
      m_aggregationT2 =
        -m_aggregationT2 * CanopyMapTask.getHeuristicT2(headerWithSummary);

      System.err.println("[CanopyReduceTask] Using reduce T2: "
        + m_aggregationT2);
    }

    NormalizableDistance reducerDistance = new EuclideanDistance();
    Clusterer first = canopies.get(0);
    if (first instanceof Canopy) {
      Instances dummyPriming =
        getPrimingDataForDistanceFunction(headerWithSummary);
      reducerDistance.setInstances(dummyPriming);
    }

    List<Canopy> cList = new ArrayList<Canopy>();
    Filter filters = null;
    Filter missingValuesHandler = null;
    if (!m_dontReplaceMissingValues) {
      try {
        missingValuesHandler =
          new PreconstructedMissingValuesReplacer(headerWithSummary);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    for (Clusterer c : canopies) {
      if (c instanceof Canopy) {
        cList.add((Canopy) c);
      } else {
        // must be a filtered clusterer
        if (filters == null) {
          // all FilteredClusterers should be using the same (compatible) set of
          // filters
          filters = ((FilteredClusterer) c).getFilter();

          // The first filter in the MultiFilter will be a missing values
          // handler if the user has opted to replace missing values
          missingValuesHandler = null;
        }
        cList.add((Canopy) ((FilteredClusterer) c).getClusterer());
      }
    }

    if (!(first instanceof Canopy)) {
      // need to get the Canopy clusters from
      // the filtered clusterer, and then get the
      // distance functions. Finally get the
      // the dataset (for the structure) and then the
      // range information so that we can construct
      // a final set of ranges with global min/max

      try {
        initFinalDistanceFunctionFiltersInPlay(cList, reducerDistance);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    double t1 =
      m_aggregationT1 > 0 ? m_aggregationT1 : -m_aggregationT1
        * m_aggregationT2;

    Canopy finalCanopy =
      Canopy.aggregateCanopies(cList, t1, m_aggregationT2, reducerDistance,
        missingValuesHandler, m_maxFinalNumCanopies);

    // save some memory
    finalCanopy.cleanUp();
    Clusterer result = finalCanopy;
    if (filters != null) {
      result = new PreconstructedFilteredClusterer();
      ((PreconstructedFilteredClusterer) result).setFilter(filters);
      ((PreconstructedFilteredClusterer) result).setClusterer(finalCanopy);
    }

    return result;
  }

  public static Instances getPrimingDataForDistanceFunction(
    Instances headerWithSummary) throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    double[] mins = new double[headerNoSummary.numAttributes()];
    double[] maxes = new double[headerNoSummary.numAttributes()];

    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      Attribute orig = headerNoSummary.attribute(i);
      Attribute summary =
        headerWithSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + orig.name());

      atts.add((Attribute) orig.copy());

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
        }
      } else if (orig.isNominal()) {
        // doesn't matter for non numeric attributes
        mins[i] = Utils.missingValue();
        maxes[i] = Utils.missingValue();
      }
    }

    Instances dummy = new Instances("Dummy", atts, 0);
    dummy.add(new DenseInstance(1.0, mins));
    dummy.add(new DenseInstance(1.0, maxes));

    return dummy;
  }

}
