/*
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

/*
 *    GaussianPrior.java
 *    Copyright (C) 2008 Illinois Institute of Technology
 *
 */
package weka.classifiers.bayes.blr;

import weka.classifiers.bayes.BayesianLogisticRegression;

import weka.core.Instance;
import weka.core.Instances;


/**
 *
 *
 * Implementation of the Gaussian Prior update function based on
 * CLG Algorithm with a certain Trust Region Update.
 *
 * The values are updated in the BayesianLogisticRegressionV variables
 * used by the algorithm.
 *
 *
 * @author Navendu Garg(gargnav@iit.edu)
 * @version 1.0
 */
public class GaussianPriorImpl extends Prior {
  /**
   * Update function specific to Laplace Prior.
   */
  public double update(int j, Instances instances, double beta,
    double hyperparameter, double[] r, double deltaV) {
    int i;
    double numerator = 0.0;
    double denominator = 0.0;
    double value = 0.0;
    Instance instance;

    m_Instances = instances;
    Beta = beta;
    Hyperparameter = hyperparameter;
    Delta = deltaV;
    R = r;

    //Compute First Derivative i.e. Numerator
    //Compute the Second Derivative i.e.
    for (i = 0; i < m_Instances.numInstances(); i++) {
      instance = m_Instances.instance(i);

      if (instance.value(j) != 0) {
        //Compute Numerator (Note: (0.0-1.0/(1.0+Math.exp(R[i]) 
        numerator += ((instance.value(j) * BayesianLogisticRegression.classSgn(instance.classValue())) * (0.0 -
        (1.0 / (1.0 + Math.exp(R[i])))));

        //Compute Denominator
        denominator += (instance.value(j) * instance.value(j) * BayesianLogisticRegression.bigF(R[i],
          Delta * Math.abs(instance.value(j))));
      }
    }

    numerator += ((2.0 * Beta) / Hyperparameter);
    denominator += (2.0 / Hyperparameter);
    value = numerator / denominator;

    return (0 - (value));
  }

  /**
   * This method calls the log-likelihood implemented in the Prior
   * abstract class.
   * @param betas
   * @param instances
   */
  public void computeLoglikelihood(double[] betas, Instances instances) {
    super.computelogLikelihood(betas, instances);
  }

  /**
   * This function computes the penalty term specific to Gaussian distribution.
   * @param betas
   * @param hyperparameters
   */
  public void computePenalty(double[] betas, double[] hyperparameters) {
    penalty = 0.0;

    for (int j = 0; j < betas.length; j++) {
      penalty += (Math.log(Math.sqrt(hyperparameters[j])) +
      (Math.log(2 * Math.PI) / 2) +
      ((betas[j] * betas[j]) / (2 * hyperparameters[j])));
    }

    penalty = 0 - penalty;
  }
}
