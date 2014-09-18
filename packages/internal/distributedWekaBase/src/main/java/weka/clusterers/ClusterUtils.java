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
 *    ClusterUtils.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.KMeansReduceTask;

/**
 * Some static utils methods for clustering
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ClusterUtils {

  /**
   * Constructor
   */
  private ClusterUtils() {
    // prevent instantiation of this utility class
  }

  /**
   * Utility method to perform the last phase of the k-means|| initialization -
   * i.e. takes the final centroid sketches and the clustering stats that show
   * how many training points are assigned to each instance (candidate center)
   * in each sketch, and then weights the sketch centroids and runs a local
   * KMeans on the weighted data to get the final requested number of starting
   * points.
   * 
   * @param numRuns number of runs of k-means being done
   * @param numClusters the requested number of clusters (and thus starting
   *          points for k-means)
   * @param finalSketches the final centroid sketches produced by the first
   *          phase of k-means||
   * @param statsForSketches the clustering statistics for the final sketches
   * @param debug true if debugging info is to be output
   * @return a final list of starting points (one set of instances for each run
   *         of k-means to be performed)
   * @throws DistributedWekaException if a problem occurs
   */
  public static List<Instances> weightSketchesAndClusterToFinalStartPoints(
    int numRuns, int numClusters, CentroidSketch[] finalSketches,
    KMeansReduceTask[] statsForSketches, boolean debug)
    throws DistributedWekaException {

    List<Instances> finalStartPointsForRuns = new ArrayList<Instances>();

    for (int i = 0; i < numRuns; i++) {
      List<Instances> centroidSummaries =
        statsForSketches[i].getAggregatedCentroidSummaries();
      Instances sketchForRun = finalSketches[i].getCurrentSketch();

      // empty clusters shouldn't be a problem - in
      // one iteration each sketch member should at minimum
      // have itself assigned (i.e. count >= 1). NOTE: The only exception
      // could occur if the sketch contains duplicate instances. However,
      // this shouldn't happen within a single WeightedReservoirSampling
      // as candidate instances with weight 0 (i.e. distance 0 to the sketch
      // in this case) are never added to the sketch.
      if (centroidSummaries.size() != sketchForRun.numInstances()) {
        throw new DistributedWekaException(
          "Was expecting as many summary headers as "
            + "there are center candidates in the sketch for run " + i);
      }

      for (int j = 0; j < sketchForRun.numInstances(); j++) {
        Instance centerCandidate = sketchForRun.instance(j);
        Instances centerStats = centroidSummaries.get(j);
        double weightForCandidate = -1.0;
        // now grab the first summary attribute and get count
        for (int k = 0; k < sketchForRun.numAttributes(); k++) {

          if (sketchForRun.attribute(k).isNumeric()) {
            Attribute statsAtt =
              centerStats
                .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                  + sketchForRun.attribute(k).name());
            weightForCandidate =
              ArffSummaryNumericMetric.COUNT.valueFromAttribute(statsAtt) +
                ArffSummaryNumericMetric.MISSING.valueFromAttribute(statsAtt);
            break;
          } else if (sketchForRun.attribute(k).isNominal()) {
            Attribute statsAtt =
              centerStats
                .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
                  + sketchForRun.attribute(k).name());
            NominalStats ns = NominalStats.attributeToStats(statsAtt);
            weightForCandidate = 0;
            for (String s : ns.getLabels()) {
              weightForCandidate += ns.getCount(s);
            }
            weightForCandidate += ns.getNumMissing();
          }
        }

        if (weightForCandidate < 0) {
          throw new DistributedWekaException(
            "Unable to compute the number of training instances "
              + "assigned to sketch member " + j + " in run " + i);
        }

        // finally - set the weight
        centerCandidate.setWeight(weightForCandidate);
      }

      if (debug) {
        System.err
          .println("********** Final weighted sketch prior to local KMeans:\n"
            + sketchForRun);
      }

      // now run standard k-means on the weighted sketch to
      // (hopefully) get the requested number of start points
      SimpleKMeans localKMeans = new SimpleKMeans();
      try {
        localKMeans.setNumClusters(numClusters);
        localKMeans.setInitializationMethod(new SelectedTag(
          SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));
        localKMeans.buildClusterer(sketchForRun);
        finalStartPointsForRuns.add(localKMeans.getClusterCentroids());
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    return finalStartPointsForRuns;
  }

  /**
   * Compute priming data for a distance function (i.e. data that can be used to
   * intialize the mins and maxes of numeric variables for normalization
   * purposes).
   * 
   * @param headerWithSummary header with summary attributes
   * @return a two-instance data set containing min and maxes for all numeric
   *         attributes. Nominal attributes will have missing values
   * @throws DistributedWekaException if a problem occurs
   */
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
