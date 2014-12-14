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
 *    PreconstructedKMeans.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Preconstructed;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.filters.Filter;
import weka.gui.GPCIgnore;
import weka.gui.beans.KFIgnore;

/**
 * A "preconstructed" version of SimpleKMeans that has it's cluster centroids
 * and cluster statistics supplied by an external client.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFIgnore
@GPCIgnore
public class PreconstructedKMeans extends SimpleKMeans implements
  Preconstructed {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 8069464935594480015L;

  public PreconstructedKMeans() {

    // we'll be wrapped in a PreconstructedFilteredClusterer for filtering...
    m_dontReplaceMissing = true;
  }

  public void setClusterCentroids(Instances centers) {
    m_ClusterCentroids = centers;
    m_squaredErrors = new double[centers.numInstances()];
    m_NumClusters = centers.numInstances();
  }

  public void setInitialStartingPoints(Instances starting) {
    m_initialStartPoints = starting;
  }

  public void setFinalNumberOfIterations(int numIts) {
    m_Iterations = numIts;
  }

  public void setWithinClustersSumOfErrors(double[] clusterErrors) {
    m_squaredErrors = clusterErrors;
  }

  public void setClusterStats(List<Instances> summaryStatsForCentroids)
    throws Exception {
    if (m_ClusterCentroids == null) {
      throw new Exception("Must set cluster centroids first!");
    }

    if (m_NumClusters != summaryStatsForCentroids.size()) {
      throw new Exception("Number of centroids does not match number of "
        + "summary stats instances provided!");
    }

    m_ClusterNominalCounts =
      new double[m_NumClusters][m_ClusterCentroids.numAttributes()][];
    m_ClusterMissingCounts =
      new double[m_NumClusters][m_ClusterCentroids.numAttributes()];
    m_ClusterStdDevs = new Instances(m_ClusterCentroids, 0);
    m_ClusterSizes = new double[m_NumClusters];

    m_FullMeansOrMediansOrModes =
      new double[m_ClusterCentroids.numAttributes()];
    m_FullStdDevs = new double[m_ClusterCentroids.numAttributes()];
    m_FullNominalCounts = new double[m_ClusterCentroids.numAttributes()][];
    m_FullMissingCounts = new double[m_ClusterCentroids.numAttributes()];

    // stats for individual clusters
    for (int i = 0; i < m_NumClusters; i++) {
      double[] stdDevVals = new double[m_ClusterCentroids.numAttributes()];
      Instances summary = summaryStatsForCentroids.get(i);
      for (int j = 0; j < m_ClusterCentroids.numAttributes(); j++) {
        Attribute orig = m_ClusterCentroids.attribute(j);
        Attribute summaryAtt =
          summary
            .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + orig.name());
        if (orig.isNominal()) {
          NominalStats stats = NominalStats.attributeToStats(summaryAtt);
          double numMissing = stats.getNumMissing();

          m_ClusterMissingCounts[i][j] = (int) numMissing;
          double sumNonMissing = 0;
          m_ClusterNominalCounts[i][j] = new double[orig.numValues()];
          for (int k = 0; k < orig.numValues(); k++) {
            m_ClusterNominalCounts[i][j][k] =
              (int) stats.getCount(orig.value(k));
            sumNonMissing += m_ClusterNominalCounts[i][j][k];
          }
          if (j == 0) {
            m_ClusterSizes[i] = (int) (sumNonMissing + numMissing);
          }
        } else if (orig.isNumeric()) {
          stdDevVals[j] =
            ArffSummaryNumericMetric.STDDEV.valueFromAttribute(summaryAtt);
          m_ClusterMissingCounts[i][j] =
            (int) ArffSummaryNumericMetric.MISSING
              .valueFromAttribute(summaryAtt);

          if (j == 0) {
            m_ClusterSizes[i] =
              (int) (ArffSummaryNumericMetric.COUNT
                .valueFromAttribute(summaryAtt) + m_ClusterMissingCounts[i][j]);
          }
        }
      }

      m_ClusterStdDevs.add(new DenseInstance(1.0, stdDevVals));
    }

    // overall dataset stats
    CSVToARFFHeaderReduceTask reduceTask = new CSVToARFFHeaderReduceTask();
    Instances fullSummary = reduceTask.aggregate(summaryStatsForCentroids);
    for (int i = 0; i < m_ClusterCentroids.numAttributes(); i++) {
      Attribute orig = m_ClusterCentroids.attribute(i);
      Attribute summary =
        fullSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + orig.name());
      if (orig.isNominal()) {
        NominalStats stats = NominalStats.attributeToStats(summary);
        m_FullMissingCounts[i] = (int) stats.getNumMissing();
        String mode = stats.getModeLabel();
        m_FullMeansOrMediansOrModes[i] = orig.indexOfValue(mode);
        double modeCount = stats.getCount(mode);
        m_FullNominalCounts[i] = new double[orig.numValues()];
        for (int j = 0; j < orig.numValues(); j++) {
          m_FullNominalCounts[i][j] = (int) stats.getCount(orig.value(j));
        }
        if (stats.getNumMissing() > modeCount) {
          m_FullMeansOrMediansOrModes[i] = -1; // mark missing as most common
        }
      } else if (orig.isNumeric()) {
        m_FullStdDevs[i] =
          ArffSummaryNumericMetric.STDDEV.valueFromAttribute(summary);
        m_FullMissingCounts[i] =
          (int) ArffSummaryNumericMetric.MISSING.valueFromAttribute(summary);
        m_FullMeansOrMediansOrModes[i] =
          ArffSummaryNumericMetric.MEAN.valueFromAttribute(summary);
        if (ArffSummaryNumericMetric.COUNT.valueFromAttribute(summary) == 0) {
          // mark missing as mean
          m_FullMeansOrMediansOrModes[i] = Utils.missingValue();
        }
      }
    }

    // get the within cluster errors
    double[] clusterErrors = new double[summaryStatsForCentroids.size()];
    int i = 0;
    for (Instances summary : summaryStatsForCentroids) {
      String relationName = summary.relationName();
      String[] parts = relationName.split(":");
      clusterErrors[i++] = Double.parseDouble(parts[1]);
    }

    setWithinClustersSumOfErrors(clusterErrors);
  }

  public int clusterProcessedInstance(Filter preprocess, Instance inst,
    boolean updateDistanceFunction, long[] instanceCanopies) throws Exception {

    if (preprocess != null) {
      preprocess.input(inst);
      inst = preprocess.output();
    }

    if (updateDistanceFunction) {
      m_DistanceFunction.update(inst);
    }

    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;

    // no fast distance calculations in this version as we
    // need the within cluster errors
    for (int i = 0; i < m_NumClusters; i++) {
      double dist;

      if (m_speedUpDistanceCompWithCanopies && instanceCanopies != null
        && instanceCanopies.length > 0) {
        try {
          if (!Canopy.nonEmptyCanopySetIntersection(
            m_centroidCanopyAssignments.get(i), instanceCanopies)) {
            continue;
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        dist =
          m_DistanceFunction.distance(inst, m_ClusterCentroids.instance(i));
      } else {
        dist =
          m_DistanceFunction.distance(inst, m_ClusterCentroids.instance(i));
      }

      if (dist < minDist) {
        minDist = dist;
        bestCluster = i;
      }
    }

    if (m_DistanceFunction instanceof EuclideanDistance) {
      // Euclidean distance to Squared Euclidean distance
      minDist *= minDist * inst.weight();
    }
    m_squaredErrors[bestCluster] += minDist;

    return bestCluster;
  }

  public double[] getErrorsForClusters() {
    return m_squaredErrors;
  }

  @Override
  public void buildClusterer(Instances data) throws Exception {
    throw new Exception("Can't call buildClusterer() on PreconstructedKMeans");
  }

  @Override
  public boolean isConstructed() {
    return true;
  }

  @Override
  public void resetPreconstructed() {
    // TODO Auto-generated method stub
  }

}
