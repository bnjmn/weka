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
 *    AggregateableEvaluationWithPriors.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.evaluation;

import java.util.Random;

import weka.classifiers.CostMatrix;
import weka.core.FastVector;
import weka.core.Instances;

/**
 * A version of AggregateableEvaluation that allows priors to be provided
 * (rather than being computed from the training data).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 * 
 */
@SuppressWarnings("deprecation")
public class AggregateableEvaluationWithPriors extends AggregateableEvaluation {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1733116764384564276L;

  /**
   * Constructs a new AggregateableEvaluation object
   * 
   * @param data the Instances to use
   * @throws Exception if a problem occurs
   */
  public AggregateableEvaluationWithPriors(Instances data) throws Exception {
    super(data);
  }

  /**
   * Constructs a new AggregateableEvaluationWithPriors object
   * 
   * @param data the Instances to use
   * @param costMatrix the cost matrix to use
   * @throws Exception if a problem occurs
   */
  public AggregateableEvaluationWithPriors(Instances data, CostMatrix costMatrix)
    throws Exception {
    super(data, costMatrix);
  }

  /**
   * Constructs a new AggregateableEvaluationWithPriors based on another
   * Evaluation object
   * 
   * @param eval an Evaluation object
   * @throws Exception if a problem occurs
   */
  public AggregateableEvaluationWithPriors(Evaluation eval) throws Exception {
    super(eval);
  }

  /**
   * Set the priors to use. In the case of classification it is the count of
   * each class value; in the case of regression it is the sum of the target.
   * 
   * @param priors the priors to use
   * @param count the number of observations the priors were computed from
   * @throws Exception if a problem occurs
   */
  public void setPriors(double[] priors, double count) throws Exception {

    if (priors.length != m_ClassPriors.length) {
      throw new Exception("Supplied priors array is not of the right length");
    }

    m_NoPriors = false;

    if (!m_ClassIsNominal) {
      m_NumTrainClassVals = 0;
      m_TrainClassVals = null;
      m_TrainClassWeights = null;
      m_PriorEstimator = null;

      m_MinTarget = Double.MAX_VALUE;
      m_MaxTarget = -Double.MAX_VALUE;

      m_ClassPriors[0] = priors[0];
      m_ClassPriorsSum = count;
    } else {
      m_ClassPriorsSum = m_NumClasses;

      for (int i = 0; i < m_NumClasses; i++) {
        m_ClassPriors[i] = 1 + priors[i];

        m_ClassPriorsSum += priors[i];
      }

      m_MaxTarget = m_NumClasses;
      m_MinTarget = 0;
    }
  }

  /**
   * Randomly downsample the predictions
   * 
   * @param retain the fraction of the predictions to retain
   * @param seed the random seed to use
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings({ "cast", "deprecation" })
  public void prunePredictions(double retain, long seed) throws Exception {
    if (m_Predictions == null || m_Predictions.size() == 0 || retain == 1) {
      return;
    }

    int numToRetain = (int) (retain * m_Predictions.size());
    if (numToRetain < 1) {
      numToRetain = 1;
    }

    Random r = new Random(seed);
    for (int i = 0; i < 50; i++) {
      r.nextInt();
    }

    FastVector<Prediction> downSampled =
      new FastVector<Prediction>(numToRetain);
    FastVector<Prediction> tmpV = new FastVector<Prediction>();
    tmpV.addAll(m_Predictions);
    for (int i = m_Predictions.size() - 1; i >= 0; i--) {
      int index = r.nextInt(i + 1);
      // downSampled.addElement(m_Predictions.elementAt(index));

      // cast necessary for 3.7.10 compatibility
      downSampled.add(tmpV.get(index));
      // downSampled.add(m_Predictions.get(index));

      if (downSampled.size() == numToRetain) {
        break;
      }

      // m_Predictions.swap(i, index);
      tmpV.swap(i, index);
    }

    m_Predictions = downSampled;
  }

  /**
   * Delete any buffered predictions
   */
  public void deleteStoredPredictions() {
    m_Predictions = null;
  }
}
