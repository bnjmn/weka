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
 *    WekaClassifierEvaluationMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.evaluation.AggregateableEvaluationWithPriors;
import weka.classifiers.evaluation.Evaluation;
import weka.core.BatchPredictor;
import weka.core.Instance;
import weka.core.Instances;

import java.io.Serializable;
import java.util.Random;

/**
 * Map task for evaluating a trained classifier. Uses Instances.train/testCV()
 * methods for extracting folds for batch trained classifiers or modulus
 * operation for incrementally trained classifiers.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierEvaluationMapTask implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 7662934051268629543L;

  /** Total number of folds involved in the evaluation. Default = use all data */
  protected int m_totalFolds = 1;

  /** The fold to evaluate on. Default - use all the data */
  protected int m_foldNumber = -1;

  /** Whether an incremental classifier has been batch trained */
  protected boolean m_batchTrainedIncremental = false;

  /** The classifier to evaluate */
  protected Classifier m_classifier = null;

  /** The evaluation object ot use */
  protected Evaluation m_eval = null;

  /** The header of the training data */
  protected Instances m_trainingHeader = null;

  /** Number of test instances processed */
  protected int m_numTestInstances;

  /** Total number of instances seen */
  protected int m_numInstances;

  /** Seed for randomizing the data (batch case only) */
  protected long m_seed = 1;

  /**
   * Fraction of predictions to retain for computing AUC/AUPRC (0 means don't
   * compute these metrics)
   */
  protected double m_predFrac = 0;

  /**
   * Get the evaluation object
   * 
   * @return the evaluation object
   */
  public Evaluation getEvaluation() {
    return m_eval;
  }

  /**
   * Set the classifier to use
   * 
   * @param c the classifier to use
   */
  public void setClassifier(Classifier c) {
    m_classifier = c;
  }

  /**
   * Get the classifier used
   * 
   * @return the classifier used
   */
  public Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Set the fold number to evaluate on. -1 indicates that all the data should
   * be used for evaluation
   * 
   * @param fn the fold number to evaluate on or -1 to evaluate on all the data
   */
  public void setFoldNumber(int fn) {
    m_foldNumber = fn;
  }

  /**
   * Get the fold number to evaluate on
   * 
   * @return the fold number to evaluate on
   */
  public int getFoldNumber() {
    return m_foldNumber;
  }

  /**
   * Set the total number of folds (1 for evaluating on all the data)
   * 
   * @param t the total number of folds
   */
  public void setTotalNumFolds(int t) {
    m_totalFolds = t;
  }

  /**
   * Get the total number of folds
   * 
   * @return the total number of folds
   */
  public int getTotalNumFolds() {
    return m_totalFolds;
  }

  /**
   * Set whether the classifier is an incremental one that has been batch
   * trained
   * 
   * @param b true if the classifier is incremental and it has been batch
   *          trained.
   */
  public void setBatchTrainedIncremental(boolean b) {
    m_batchTrainedIncremental = b;
  }

  /**
   * Get whether the classifier is an incremental one that has been batch
   * trained
   * 
   * @return true if the classifier is incremental and it has been batch
   *         trained.
   */
  public boolean getBatchTrainedIncremental() {
    return m_batchTrainedIncremental;
  }

  /**
   * Setup the task.
   * 
   * @param trainingHeader the header of the training data used to create the
   *          classifier to be evaluated
   * @param priors priors for the class (frequency counts for the values of a
   *          nominal class or sum of target for a numeric class)
   * @param count the total number of class values seen (with respect to the
   *          priors)
   * @param seed the random seed to use for shuffling the data in the batch case
   * @param predFrac the fraction of the total number of predictions to retain
   *          for computing AUC/AUPRC
   * @throws Exception if a problem occurs
   */
  public void setup(Instances trainingHeader, double[] priors, double count,
    long seed, double predFrac) throws Exception {

    m_trainingHeader = new Instances(trainingHeader, 0);
    if (m_trainingHeader.classIndex() < 0) {
      throw new Exception("No class index set in the data!");
    }

    m_eval = new AggregateableEvaluationWithPriors(m_trainingHeader);
    if (priors != null) {
      ((AggregateableEvaluationWithPriors) m_eval).setPriors(priors, count);
    }
    m_numInstances = 0;
    m_numTestInstances = 0;
    m_seed = seed;
    m_predFrac = predFrac;
  }

  /**
   * Process an instance for evaluation
   * 
   * @param inst the instance to process
   * @throws Exception if a problem occurs
   */
  public void processInstance(Instance inst) throws Exception {
    if (m_classifier != null
      && (m_classifier instanceof UpdateableClassifier && !m_batchTrainedIncremental)) {

      boolean ok = true;
      if (m_totalFolds > 1 && m_foldNumber >= 1) {
        int fn = m_numInstances % m_totalFolds;

        // is this instance in our test fold
        if (fn != (m_foldNumber - 1)) {
          ok = false;
        }
      }

      if (ok) {
        if (m_predFrac > 0) {
          m_eval.evaluateModelOnceAndRecordPrediction(m_classifier, inst);
        } else {
          m_eval.evaluateModelOnce(m_classifier, inst);
        }
        m_numTestInstances++;
      }
    } else {
      // store the instance
      m_trainingHeader.add(inst);
    }

    m_numInstances++;
  }

  /**
   * Finalize this task. This is where the actual evaluation occurs in the batch
   * case - the order of the data gets randomized (and stratified if class is
   * nominal) and the test fold extracted.
   * 
   * @throws Exception if a problem occurs
   */
  public void finalizeTask() throws Exception {
    if (m_classifier == null) {
      throw new Exception("No classifier has been set");
    }

    if (m_classifier instanceof UpdateableClassifier
      && !m_batchTrainedIncremental) {
      // nothing to do except possibly down-sample predictions for
      // auc/prc
      if (m_predFrac > 0) {
        ((AggregateableEvaluationWithPriors) m_eval).prunePredictions(
          m_predFrac, m_seed);
      }

      return;
    }

    m_trainingHeader.compactify();

    Instances test = m_trainingHeader;
    Random r = new Random(m_seed);
    test.randomize(r);
    if (test.classAttribute().isNominal() && m_totalFolds > 1) {
      test.stratify(m_totalFolds);
    }

    if (m_totalFolds > 1 && m_foldNumber >= 1) {
      test = test.testCV(m_totalFolds, m_foldNumber - 1);
    }

    m_numTestInstances = test.numInstances();

    if (m_classifier instanceof BatchPredictor
      && ((BatchPredictor) m_classifier)
        .implementsMoreEfficientBatchPrediction()) {

      // this method always stores the predictions for AUC, so we need to get
      // rid of them if we're not doing any AUC computation
      m_eval.evaluateModel(m_classifier, test);
      if (m_predFrac <= 0) {
        ((AggregateableEvaluationWithPriors) m_eval).deleteStoredPredictions();
      }
    } else {
      for (int i = 0; i < test.numInstances(); i++) {
        if (m_predFrac > 0) {
          m_eval.evaluateModelOnceAndRecordPrediction(m_classifier,
            test.instance(i));
        } else {
          m_eval.evaluateModelOnce(m_classifier, test.instance(i));
        }
      }
    }

    // down-sample predictions for auc/prc
    if (m_predFrac > 0) {
      ((AggregateableEvaluationWithPriors) m_eval).prunePredictions(m_predFrac,
        m_seed);
    }
  }

  public static void main(String[] args) {
    try {
      Instances inst =
        new Instances(new java.io.BufferedReader(
          new java.io.FileReader(args[0])));
      inst.setClassIndex(inst.numAttributes() - 1);

      weka.classifiers.evaluation.AggregateableEvaluation agg = null;

      WekaClassifierEvaluationMapTask task =
        new WekaClassifierEvaluationMapTask();
      weka.classifiers.trees.J48 classifier = new weka.classifiers.trees.J48();

      WekaClassifierMapTask trainer = new WekaClassifierMapTask();
      trainer.setClassifier(classifier);
      trainer.setTotalNumFolds(10);
      for (int i = 0; i < 10; i++) {
        System.err.println("Processing fold " + (i + 1));
        trainer.setFoldNumber((i + 1));
        trainer.setup(new Instances(inst, 0));
        trainer.addToTrainingHeader(inst);
        trainer.finalizeTask();

        Classifier c = trainer.getClassifier();

        task.setClassifier(c);
        task.setTotalNumFolds(10);
        task.setFoldNumber((i + 1));

        // TODO set the priors properly here.
        task.setup(new Instances(inst, 0), null, -1, 1L, 0);
        for (int j = 0; j < inst.numInstances(); j++) {
          task.processInstance(inst.instance(j));
        }
        task.finalizeTask();

        Evaluation eval = task.getEvaluation();
        if (agg == null) {
          agg = new weka.classifiers.evaluation.AggregateableEvaluation(eval);
        }
        agg.aggregate(eval);
      }

      System.err.println(agg.toSummaryString());
      System.err.println("\n" + agg.toClassDetailsString());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
