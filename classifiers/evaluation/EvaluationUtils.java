/*
 *    EvaluationUtils.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
 * @version $Revision: 1.1 $
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

    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class must be nominal.");
    }

    FastVector predictions = new FastVector();
    Instances runInstances = new Instances(data);
    Random random = new Random(m_Seed);
    runInstances.randomize(random);
    runInstances.stratify(numFolds);
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

    FastVector predictions = new FastVector();
    classifier.buildClassifier(train);
    for (int i = 0; i < test.numInstances(); i++) {
      Instance curr = test.instance(i);
      int actual = (int) curr.classValue();
      double [] dist = classifier.distributionForInstance(curr);
      predictions.addElement(new NominalPrediction(actual, dist, 
                                                   curr.weight()));
    }
    return predictions;
  }
}
