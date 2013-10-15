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
 *    WekaClassifierEvaluationReduceTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.List;

import weka.classifiers.evaluation.AggregateableEvaluation;
import weka.classifiers.evaluation.Evaluation;

/**
 * Reduce task for aggregating Evaluation objects
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierEvaluationReduceTask implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 2007173965424840605L;

  /**
   * Aggregate a list of Evaluation objects.
   * 
   * @param evals the list of Evaluation objects to aggregate
   * @return a single aggregated Evaluation
   * @throws Exception if a problem occurs
   */
  public Evaluation aggregate(List<Evaluation> evals) throws Exception {

    if (evals.size() == 0) {
      throw new Exception("Nothing to aggregate!");
    }

    AggregateableEvaluation aggEval = new AggregateableEvaluation(evals.get(0));

    for (Evaluation e : evals) {
      aggEval.aggregate(e);
    }

    aggEval.finalizeAggregation();

    return aggEval;
  }
}
