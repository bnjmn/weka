/*
 *    EvaluationUtils.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 */

package weka.classifiers.evaluation;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.DistributionClassifier;
import java.util.Random;

/**
 * Contains utility functions for generating lists of predictions in 
 * various manners.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.5 $
 */
public class EvaluationUtils {

  /** Seed used to randomize data in cross-validation */
  private int m_Seed = 1;

  /** Sets the seed for randomization during cross-validation */
  public void setSeed(int seed) { m_Seed = seed; }

  /** Gets the seed for randomization during cross-validation */
  public int getSeed() { return m_Seed; }
  
  /**
   * Generate a bunch of predictions ready for processing, by performing a
   * cross-validation on the supplied dataset.
   *
   * @param classifier the DistributionClassifier to evaluate
   * @param data the dataset
   * @param numFolds the number of folds in the cross-validation.
   * @exception Exception if an error occurs
   */
  public FastVector getCVPredictions(DistributionClassifier classifier, 
                                     Instances data, 
                                     int numFolds) 
    throws Exception {

    FastVector predictions = new FastVector();
    Instances runInstances = new Instances(data);
    Random random = new Random(m_Seed);
    runInstances.randomize(random);
    if (runInstances.classAttribute().isNominal() && (numFolds > 1)) {
      runInstances.stratify(numFolds);
    }
    int inst = 0;
    for (int fold = 0; fold < numFolds; fold++) {
      Instances train = runInstances.trainCV(numFolds, fold);
      Instances test = runInstances.testCV(numFolds, fold);
      FastVector foldPred = getTrainTestPredictions(classifier, train, test);
      predictions.appendElements(foldPred);
    } 
    return predictions;
  }

  /**
   * Generate a bunch of predictions ready for processing, by performing a
   * evaluation on a test set after training on the given training set.
   *
   * @param classifier the DistributionClassifier to evaluate
   * @param train the training dataset
   * @param test the test dataset
   * @exception Exception if an error occurs
   */
  public FastVector getTrainTestPredictions(DistributionClassifier classifier, 
                                            Instances train, Instances test) 
    throws Exception {
    
    classifier.buildClassifier(train);
    return getTestPredictions(classifier, test);
  }

  /**
   * Generate a bunch of predictions ready for processing, by performing a
   * evaluation on a test set assuming the classifier is already trained.
   *
   * @param classifier the pre-trained DistributionClassifier to evaluate
   * @param test the test dataset
   * @exception Exception if an error occurs
   */
  public FastVector getTestPredictions(DistributionClassifier classifier, 
                                       Instances test) 
    throws Exception {
    
    FastVector predictions = new FastVector();
    for (int i = 0; i < test.numInstances(); i++) {
      if (!test.instance(i).classIsMissing()) {
        predictions.addElement(getPrediction(classifier, test.instance(i)));
      }
    }
    return predictions;
  }

  
  /**
   * Generate a single prediction for a test instance given the pre-trained
   * classifier.
   *
   * @param classifier the pre-trained DistributionClassifier to evaluate
   * @param test the test instance
   * @exception Exception if an error occurs
   */
  public Prediction getPrediction(DistributionClassifier classifier,
                                  Instance test)
    throws Exception {
   
    double actual = test.classValue();
    double [] dist = classifier.distributionForInstance(test);
    if (test.classAttribute().isNominal()) {
      return new NominalPrediction(actual, dist, test.weight());
    } else {
      return new NumericPrediction(actual, dist[0], test.weight());
    }
  }
}

