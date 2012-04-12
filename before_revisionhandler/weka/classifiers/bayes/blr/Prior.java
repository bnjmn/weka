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
 *    Prior.java
 *    Copyright (C) 2008 Illinois Institute of Technology
 *
 */
package weka.classifiers.bayes.blr;

import weka.classifiers.bayes.BayesianLogisticRegression;

import weka.core.Instance;
import weka.core.Instances;

import java.io.Serializable;


/**
 *
 *
 * This is an interface to plug various priors into
 * the Bayesian Logistic Regression Model.
 *
 * @version 1.0
 * @author Navendu Garg (gargnav@iit.edu)
 *
 */
public abstract class Prior implements Serializable {
  protected Instances m_Instances;
  protected double Beta = 0.0;
  protected double Hyperparameter = 0.0;
  protected double DeltaUpdate;
  protected double[] R;
  protected double Delta = 0.0;
  protected double log_posterior = 0.0;
  protected double log_likelihood = 0.0;
  protected double penalty = 0.0;

  /**
   * Interface for the update functions for different types of
   * priors.
   *
   */
  public double update(int j, Instances instances, double beta,
    double hyperparameter, double[] r, double deltaV) {
    return 0.0;
  }

  /**
   * Function computes the log-likelihood value:
   * -sum{1 to n}{ln(1+exp(-Beta*x(i)*y(i))}
   * @param betas
   * @param instances
   */
  public void computelogLikelihood(double[] betas, Instances instances) {
    Instance instance;
    log_likelihood = 0.0;

    for (int i = 0; i < instances.numInstances(); i++) {
      instance = instances.instance(i);

      double log_row = 0.0;

      for (int j = 0; j < instance.numAttributes(); j++) {
        if (instance.value(j) != 0.0) {
          log_row += (betas[j] * instance.value(j) * instance.value(j));
        }
      }

      log_row = log_row * BayesianLogisticRegression.classSgn(instance.classValue());
      log_likelihood += Math.log(1.0 + Math.exp(0.0 - log_row));
    }

    log_likelihood = 0 - log_likelihood;
  }

  /**
   * Skeleton function to compute penalty terms.
   * @param betas
   * @param hyperparameters
   */
  public void computePenalty(double[] betas, double[] hyperparameters) {
    //implement specific penalties in the prior implmentation.
  }

  /**
   *
   * @return log-likelihood value.
   */
  public double getLoglikelihood() {
    return log_likelihood;
  }

  /**
   *
   * @return regularized log posterior value.
   */
  public double getLogPosterior() {
    log_posterior = log_likelihood + penalty;

    return log_posterior;
  }

  /**
   *
   * @return penalty term.
   */
  public double getPenalty() {
    return penalty;
  }
}
