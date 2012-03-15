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
 *    EnsembleMetricHelper.java
 *    Copyright (C) 2006 David Michael
 *
 */

package weka.classifiers.meta.ensembleSelection;

import weka.classifiers.Evaluation;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * This class is used by Ensemble Selection.  It provides the "enumeration" of the
 * metrics that can be used by ensemble selection, as well as a helper function for
 * computing the metric using an Evaluation class.
 * 
 * @author  David Michael
 * @version $Revision$
 */
public class EnsembleMetricHelper
  implements RevisionHandler {
  
  /** metric: Accuracy */
  public static final int METRIC_ACCURACY = 0;
  /** metric: RMSE */
  public static final int METRIC_RMSE = 1;
  /** metric: ROC */
  public static final int METRIC_ROC = 2;
  /** metric: Precision */
  public static final int METRIC_PRECISION = 3;
  /** metric: Recall */
  public static final int METRIC_RECALL = 4;
  /** metric: FScore */
  public static final int METRIC_FSCORE = 5;
  /** metric: All */
  public static final int METRIC_ALL = 6;
  
  /**
   * Given an Evaluation object and metric, call the appropriate function to get
   * the value for that metric and return it.  Metrics are returned so that
   * "bigger is better".  For instance, we return 1.0 - RMSE instead of RMSE, because
   * bigger RMSE is better. 
   * 
   * @param eval		the evaluation object to use
   * @param metric_index	the metric to use
   * @return			the value for the metric
   */
  public static double getMetric(Evaluation eval, int metric_index) {
    switch (metric_index) {
      case METRIC_ACCURACY:
	return eval.pctCorrect();
      case METRIC_RMSE:
	return 1.0 - eval.rootMeanSquaredError();
      case METRIC_ROC:
	return eval.areaUnderROC(1); //TODO - is 1 right?
      case METRIC_PRECISION:
	return eval.precision(1); //TODO - same question
      case METRIC_RECALL:
	return eval.recall(1); //TODO - same question
      case METRIC_FSCORE:
	return eval.fMeasure(1); //TODO - same question
      case METRIC_ALL:
	double average = 0;
	int num_metrics = 0;
	average += eval.pctCorrect();
	++num_metrics;
	average += 1.0 - eval.rootMeanSquaredError();
	++num_metrics;
	average += eval.areaUnderROC(1);
	++num_metrics;
	average += eval.precision(1);
	++num_metrics;
	average += eval.recall(1);
	++num_metrics;
	average += eval.fMeasure(1);
	++num_metrics;
	return average / num_metrics;
      default:
	return 0.0; //FIXME TODO - this should probably be an exception?
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
