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
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.evaluation.AggregateableEvaluation;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

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
   * @return a single eval Evaluation
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

  /**
   * Represents basic evaluation and information retrieval results in a single
   * instance dataset.
   * 
   * @param eval the Evaluation object to use
   * @return a single instance dataset
   * @throws Exception if a problem occurs
   */
  public static Instances evaluationResultsToInstances(Evaluation eval)
    throws Exception {
    // basic stats - always output all of these (regardless of class attribute
    // type)
    double numCorrect = eval.correct();
    double numIncorrect = eval.incorrect();
    double MAE = eval.meanAbsoluteError();
    double RMSE = eval.rootMeanSquaredError();
    double RAE = eval.relativeAbsoluteError();
    double RRSE = eval.rootRelativeSquaredError();
    double totalNumberOfInstances = eval.numInstances();

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    atts.add(new Attribute("Correctly classified instances"));
    atts.add(new Attribute("Incorrectly classified instances"));
    atts.add(new Attribute("Mean absolute error"));
    atts.add(new Attribute("Root mean squared error"));
    atts.add(new Attribute("Relative absolute error"));
    atts.add(new Attribute("Root relative squared error"));
    atts.add(new Attribute("Total number of instances"));

    // int capacity = 1;

    // info retrieval stats (only add these if class is nominal)
    if (eval.getHeader().classAttribute().isNominal()) {
      // capacity = eval.getHeader().classAttribute().numValues();

      atts.add(new Attribute("Kappa statistic"));

      for (int i = 0; i < eval.getHeader().classAttribute().numValues(); i++) {
        String classLabel = eval.getHeader().classAttribute().value(i)
          + "_";
        atts.add(new Attribute(classLabel + "TP Rate"));
        atts.add(new Attribute(classLabel + "FP Rate"));
        atts.add(new Attribute(classLabel + "Precision"));
        atts.add(new Attribute(classLabel + "Recall"));
        atts.add(new Attribute(classLabel + "F-Measure"));
        atts.add(new Attribute(classLabel + "MCC"));
        atts.add(new Attribute(classLabel + "ROC Area"));
        atts.add(new Attribute(classLabel + "PRC Area"));
      }
    }

    Instances evalInsts = new Instances("Evaluation results: "
      + eval.getHeader().relationName(), atts, 1);

    // make instances

    double[] vals = new double[atts.size()];

    vals[0] = numCorrect;
    vals[1] = numIncorrect;
    vals[2] = MAE;
    vals[3] = RMSE;
    vals[4] = RAE;
    vals[5] = RRSE;
    vals[6] = totalNumberOfInstances;

    int offset = 7;
    if (eval.getHeader().classAttribute().isNominal()) {
      vals[offset++] = eval.kappa();
      for (int i = 0; i < eval.getHeader().classAttribute().numValues(); i++) {
        vals[offset++] = eval.truePositiveRate(i);
        vals[offset++] = eval.falseNegativeRate(i);
        vals[offset++] = eval.precision(i);
        vals[offset++] = eval.recall(i);
        vals[offset++] = eval.fMeasure(i);
        vals[offset++] = eval.areaUnderROC(i);
        vals[offset++] = eval.areaUnderPRC(i);
      }
    }

    Instance inst = new DenseInstance(1.0, vals);
    evalInsts.add(inst);
    evalInsts.compactify();

    return evalInsts;
  }
}
