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
 *    WekaClassifierEvaluationTest.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Test class for the evaluation map and reduce tasks
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierEvaluationTest {

  @Test
  public void testCrossValidateBatchMapOnly() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierEvaluationMapTask evaluator = new WekaClassifierEvaluationMapTask();
    WekaClassifierMapTask trainer = new WekaClassifierMapTask();
    trainer.setClassifier(new weka.classifiers.trees.J48());
    trainer.setTotalNumFolds(10);

    for (int i = 0; i < 10; i++) {
      trainer.setFoldNumber((i + 1));
      trainer.setup(new Instances(train, 0));
      trainer.addToTrainingHeader(train);
      trainer.finalizeTask();

      Classifier c = trainer.getClassifier();

      evaluator.setClassifier(c);
      evaluator.setTotalNumFolds(10);
      evaluator.setFoldNumber(i + 1);

      // priors for iris (just using priors + count from all the data for
      // simplicity)
      double[] priors = { 50.0, 50.0, 50.0 };
      evaluator.setup(new Instances(train, 0), priors, 150, 1L, 0);

      for (int j = 0; j < train.numInstances(); j++) {
        evaluator.processInstance(train.instance(j));
      }
      evaluator.finalizeTask();

      Evaluation eval = evaluator.getEvaluation();
      assertTrue(eval != null);

      // there should be predictions for exactly 15 instances per test fold
      assertEquals(15, (int) eval.numInstances());

      // there shouldn't be any AUC/AUPRC stats computed
      assertTrue(Utils.isMissingValue(eval.areaUnderROC(0)));
    }
  }

  @Test
  public void testCrossValidateBatchMapOnlyRetainPredsForAUC() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierEvaluationMapTask evaluator = new WekaClassifierEvaluationMapTask();
    WekaClassifierMapTask trainer = new WekaClassifierMapTask();
    trainer.setClassifier(new weka.classifiers.trees.J48());
    trainer.setTotalNumFolds(10);

    for (int i = 0; i < 10; i++) {
      trainer.setFoldNumber((i + 1));
      trainer.setup(new Instances(train, 0));
      trainer.addToTrainingHeader(train);
      trainer.finalizeTask();

      Classifier c = trainer.getClassifier();

      evaluator.setClassifier(c);
      evaluator.setTotalNumFolds(10);
      evaluator.setFoldNumber(i + 1);

      // priors for iris (just using priors + count from all the data for
      // simplicity)
      double[] priors = { 50.0, 50.0, 50.0 };

      // retain 50% of the predictions for computing AUC
      evaluator.setup(new Instances(train, 0), priors, 150, 1L, 0.5);

      for (int j = 0; j < train.numInstances(); j++) {
        evaluator.processInstance(train.instance(j));
      }
      evaluator.finalizeTask();

      Evaluation eval = evaluator.getEvaluation();
      assertTrue(eval != null);

      // there should be predictions for exactly 15 instances per test fold
      assertEquals(15, (int) eval.numInstances());

      // should be only 7 (50% rounded down of 15) predictions retained for AUC
      // comp
      assertEquals(7, eval.predictions().size());

      // AUC shouldn't be a missing value...
      assertTrue(!Utils.isMissingValue(eval.areaUnderROC(0)));
    }
  }

  @Test
  public void testCrossValidateIncrementalMapOnly() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierEvaluationMapTask evaluator = new WekaClassifierEvaluationMapTask();
    WekaClassifierMapTask trainer = new WekaClassifierMapTask();
    trainer.setClassifier(new weka.classifiers.bayes.NaiveBayesUpdateable());
    trainer.setTotalNumFolds(10);

    for (int i = 0; i < 10; i++) {
      trainer.setFoldNumber((i + 1));
      trainer.setup(new Instances(train, 0));

      for (int j = 0; j < train.numInstances(); j++) {
        trainer.processInstance(train.instance(j));
      }
      trainer.finalizeTask();

      Classifier c = trainer.getClassifier();

      evaluator.setClassifier(c);
      evaluator.setTotalNumFolds(10);
      evaluator.setFoldNumber(i + 1);

      // priors for iris (just using priors + count from all the data for
      // simplicity)
      double[] priors = { 50.0, 50.0, 50.0 };
      evaluator.setup(new Instances(train, 0), priors, 150, 1L, 0);

      for (int j = 0; j < train.numInstances(); j++) {
        evaluator.processInstance(train.instance(j));
      }
      evaluator.finalizeTask();

      Evaluation eval = evaluator.getEvaluation();
      assertTrue(eval != null);

      // there should be predictions for exactly 15 instances per test fold
      assertEquals(15, (int) eval.numInstances());

      // there shouldn't be any AUC/AUPRC stats computed
      assertTrue(Utils.isMissingValue(eval.areaUnderROC(0)));
    }
  }

  @Test
  public void testCrossValidateIncrementalMapOnlyRetainPredsForAUC()
    throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierEvaluationMapTask evaluator = new WekaClassifierEvaluationMapTask();
    WekaClassifierMapTask trainer = new WekaClassifierMapTask();
    trainer.setClassifier(new weka.classifiers.bayes.NaiveBayesUpdateable());
    trainer.setTotalNumFolds(10);

    for (int i = 0; i < 10; i++) {
      trainer.setFoldNumber((i + 1));
      trainer.setup(new Instances(train, 0));

      for (int j = 0; j < train.numInstances(); j++) {
        trainer.processInstance(train.instance(j));
      }
      trainer.finalizeTask();

      Classifier c = trainer.getClassifier();

      evaluator.setClassifier(c);
      evaluator.setTotalNumFolds(10);
      evaluator.setFoldNumber(i + 1);

      // priors for iris (just using priors + count from all the data for
      // simplicity)
      double[] priors = { 50.0, 50.0, 50.0 };

      // retain all the predictions for AUC
      evaluator.setup(new Instances(train, 0), priors, 150, 1L, 1);

      for (int j = 0; j < train.numInstances(); j++) {
        evaluator.processInstance(train.instance(j));
      }
      evaluator.finalizeTask();

      Evaluation eval = evaluator.getEvaluation();
      assertTrue(eval != null);

      // there should be predictions for exactly 15 instances per test fold
      assertEquals(15, (int) eval.numInstances());

      // should be all the predictions in this fold retained for AUC
      // comp
      assertEquals(15, eval.predictions().size());

      // AUC shouldn't be a missing value
      assertTrue(!Utils.isMissingValue(eval.areaUnderROC(0)));
    }
  }

  @Test
  public void testReduceOverFolds() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierEvaluationMapTask evaluator = new WekaClassifierEvaluationMapTask();
    WekaClassifierMapTask trainer = new WekaClassifierMapTask();
    trainer.setClassifier(new weka.classifiers.trees.J48());
    trainer.setTotalNumFolds(10);

    List<Evaluation> foldEvals = new ArrayList<Evaluation>();

    for (int i = 0; i < 10; i++) {
      trainer.setFoldNumber((i + 1));
      trainer.setup(new Instances(train, 0));
      trainer.addToTrainingHeader(train);
      trainer.finalizeTask();

      Classifier c = trainer.getClassifier();

      evaluator.setClassifier(c);
      evaluator.setTotalNumFolds(10);
      evaluator.setFoldNumber(i + 1);

      // priors for iris (just using priors + count from all the data for
      // simplicity)
      double[] priors = { 50.0, 50.0, 50.0 };
      evaluator.setup(new Instances(train, 0), priors, 150, 1L, 0);

      for (int j = 0; j < train.numInstances(); j++) {
        evaluator.processInstance(train.instance(j));
      }
      evaluator.finalizeTask();

      Evaluation eval = evaluator.getEvaluation();
      assertTrue(eval != null);

      // there should be predictions for exactly 15 instances per test fold
      assertEquals(15, (int) eval.numInstances());

      // there shouldn't be any AUC/AUPRC stats computed
      assertTrue(Utils.isMissingValue(eval.areaUnderROC(0)));

      foldEvals.add(eval);
    }

    WekaClassifierEvaluationReduceTask reducer = new WekaClassifierEvaluationReduceTask();

    Evaluation aggregated = reducer.aggregate(foldEvals);

    // now should be predictions for all 150 iris instances
    assertEquals(150, (int) aggregated.numInstances());

    // still shouldn't be any AUC :-)
    assertTrue(Utils.isMissingValue(aggregated.areaUnderROC(0)));
  }

  public static void main(String[] args) {
    try {
      WekaClassifierEvaluationTest t = new WekaClassifierEvaluationTest();
      t.testCrossValidateBatchMapOnly();
      t.testCrossValidateBatchMapOnlyRetainPredsForAUC();
      t.testCrossValidateIncrementalMapOnly();
      t.testCrossValidateIncrementalMapOnlyRetainPredsForAUC();
      t.testReduceOverFolds();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
