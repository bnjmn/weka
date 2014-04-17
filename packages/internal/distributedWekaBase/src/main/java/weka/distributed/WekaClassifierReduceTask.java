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
 *    WekaClassifierMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.meta.BatchPredictorVote;
import weka.classifiers.meta.Vote;
import weka.core.Aggregateable;
import weka.core.BatchPredictor;

/**
 * Reduce task for aggregating classifiers into one final model, if they all
 * directly implement Aggregateable, or into a voted ensemble otherwise
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierReduceTask implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -3184624278865690643L;

  /**
   * Minimum fraction of the largest number of training instances processed by
   * the classifiers to allow a classifier to be aggregated. Default = 50%, so
   * any classifier that has trained on less than 50% of the number of instances
   * processed by the classifier that has seen the most data is discarded.
   */
  protected double m_minTrainingFraction = 0.5;

  /**
   * Holds classifiers that get discarded if they have been trained on less data
   * than the minimum training fraction
   */
  protected List<Integer> m_discarded;

  /**
   * Aggregate the supplied list of classifiers
   * 
   * @param classifiers the classifiers to aggregate
   * @return the final aggregated classifier
   * @throws DistributedWekaException if a problem occurs
   */
  public Classifier aggregate(List<Classifier> classifiers)
    throws DistributedWekaException {
    return aggregate(classifiers, null, false);
  }

  /**
   * Aggregated the supplied list of classifiers. Might discard some classifiers
   * if they have not seen enough training data.
   * 
   * @param classifiers the list of classifiers to aggregate
   * @param numTrainingInstancesPerClassifier a list of integers, where each
   *          entry is the number of training instances seen by the
   *          corresponding classifier
   * @param forceVote true if a Vote ensemble is to be created (even if all
   *          classifiers could be directly aggregated to one model of the same
   *          type
   * @return the aggregated classifier
   * @throws DistributedWekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public Classifier aggregate(List<Classifier> classifiers,
    List<Integer> numTrainingInstancesPerClassifier, boolean forceVote)
    throws DistributedWekaException {

    if (classifiers.size() == 0) {
      throw new DistributedWekaException("Nothing to aggregate!");
    }

    m_discarded = new ArrayList<Integer>();

    boolean allAggregateable = false;
    if (!forceVote) {
      // all classifiers should be homogenous (but we could, at some future
      // stage, allow the user to specify a list of base classifiers and
      // then use the map task number % list size to determine which
      // base classifier to build for a heterogenous ensemble)
      allAggregateable = true;
      for (Classifier c : classifiers) {
        if (!(c instanceof Aggregateable)) {
          allAggregateable = false;
          break;
        }
      }
    }

    // TODO revisit this if we move to homogeneous base classifiers
    boolean batchPredictors = false;
    for (Classifier c : classifiers) {
      if (c instanceof BatchPredictor) {
        batchPredictors = true;
        break;
      }
    }

    if (numTrainingInstancesPerClassifier != null
      && numTrainingInstancesPerClassifier.size() == classifiers.size()) {
      int max = 0;
      int min = Integer.MAX_VALUE;
      int minIndex = -1;
      for (int i = 0; i < numTrainingInstancesPerClassifier.size(); i++) {
        if (numTrainingInstancesPerClassifier.get(i) > max) {
          max = numTrainingInstancesPerClassifier.get(i);
        }

        if (numTrainingInstancesPerClassifier.get(i) < min) {
          min = numTrainingInstancesPerClassifier.get(i);
          minIndex = i;
        }
      }

      if (((double) min / (double) max) < m_minTrainingFraction) {
        classifiers.remove(minIndex);
        numTrainingInstancesPerClassifier.remove(minIndex);
        m_discarded.add(min);
      }
    }

    Classifier base =
      allAggregateable ? classifiers.get(0)
        : batchPredictors ? new BatchPredictorVote() : new Vote();

    // set the batch size based on the base classifier's batch size
    if (base instanceof BatchPredictor) {
      ((BatchPredictor) base)
        .setBatchSize(((BatchPredictor) classifiers.get(0)).getBatchSize());
    }

    int startIndex = allAggregateable ? 1 : 0;

    for (int i = startIndex; i < classifiers.size(); i++) {
      try {
        ((Aggregateable) base).aggregate(classifiers.get(i));
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    if (startIndex < classifiers.size()) {
      try {
        ((Aggregateable) base).finalizeAggregation();
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    return base;
  }

  /**
   * Get list of indices of the classifiers that were discarded (if any)
   * 
   * @return a list of indices of discarded classifiers
   */
  public List<Integer> getDiscarded() {
    return m_discarded;
  }

  /**
   * Set the minimum training fraction by which a classifier is discarded. This
   * is a fraction of the largest number of training instances processed by the
   * classifiers to allow a classifier to be aggregated. Default = 50%, so any
   * classifier that has trained on less than 50% of the number of instances
   * processed by the classifier that has seen the most data is discarded.
   * 
   * @param m a number between 0 and 1.
   */
  public void setMinTrainingFraction(double m) {
    m_minTrainingFraction = m;
  }

  /**
   * Get the minimum training fraction by which a classifier is discarded. This
   * is a fraction of the largest number of training instances processed by the
   * classifiers to allow a classifier to be aggregated. Default = 50%, so any
   * classifier that has trained on less than 50% of the number of instances
   * processed by the classifier that has seen the most data is discarded.
   * 
   * @return the minumum training fraction
   */
  public double getMinTrainingFraction() {
    return m_minTrainingFraction;
  }
}
