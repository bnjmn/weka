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
import weka.core.RevisionUtils;

/**
 * Implementation of the Gaussian Prior update function based on modified
 *  CLG Algorithm (CLG-Lasso) with a certain Trust Region Update based
 * on Laplace Priors.
 *
 * @author Navendu Garg(gargnav@iit.edu)
 * @version $Revision: 1.2 $
 */
public class LaplacePriorImpl
  extends Prior {
  
  /** for serialization. */
  private static final long serialVersionUID = 2353576123257012607L;
  
  Instances m_Instances;
  double Beta;
  double Hyperparameter;
  double DeltaUpdate;
  double[] R;
  double Delta;

  /**
   * Update function specific to Laplace Prior.
   */
  public double update(int j, Instances instances, double beta,
    double hyperparameter, double[] r, double deltaV) {
    double sign = 0.0;
    double change = 0.0;
    DeltaUpdate = 0.0;
    m_Instances = instances;
    Beta = beta;
    Hyperparameter = hyperparameter;
    R = r;
    Delta = deltaV;

    if (Beta == 0) {
      sign = 1.0;
      DeltaUpdate = laplaceUpdate(j, sign);

      if (DeltaUpdate <= 0.0) { // positive direction failed.
        sign = -1.0;
        DeltaUpdate = laplaceUpdate(j, sign);

        if (DeltaUpdate >= 0.0) {
          DeltaUpdate = 0;
        }
      }
    } else {
      sign = Beta / Math.abs(Beta);
      DeltaUpdate = laplaceUpdate(j, sign);
      change = Beta + DeltaUpdate;
      change = change / Math.abs(change);

      if (change < 0) {
        DeltaUpdate = 0 - Beta;
      }
    }

    return DeltaUpdate;
  }

  /**
   * This is the CLG-lasso update function described in the

  *<pre>
  * &#64;TechReport{blrtext04,
  *author = {Alexander Genkin and David D. Lewis and David Madigan},
  *title = {Large-scale bayesian logistic regression for text categorization},
  *institution = {DIMACS},
  *year = {2004},
  *url = "http://www.stat.rutgers.edu/~madigan/PAPERS/shortFat-v3a.pdf",
  *OPTannote = {}
  *}</pre>
   *
   * @param j
   * @return double value
   */
  public double laplaceUpdate(int j, double sign) {
    double value = 0.0;
    double numerator = 0.0;
    double denominator = 0.0;

    Instance instance;

    for (int i = 0; i < m_Instances.numInstances(); i++) {
      instance = m_Instances.instance(i);

      if (instance.value(j) != 0) {
        numerator += (instance.value(j) * BayesianLogisticRegression.classSgn(instance.classValue()) * (1.0 / (1.0 +
        Math.exp(R[i]))));
        denominator += (instance.value(j) * instance.value(j) * BayesianLogisticRegression.bigF(R[i],
          Delta * instance.value(j)));
      }
    }

    numerator -= (Math.sqrt(2.0 / Hyperparameter) * sign);

    if (denominator != 0.0) {
      value = numerator / denominator;
    }

    return value;
  }

  /**
   * Computes the log-likelihood values using the implementation in the Prior class.
   * @param betas
   * @param instances
   * @param hyperparameter
   */
  public void computeLogLikelihood(double[] betas, Instances instances) {
    //Basic implementation done in the prior class.
    super.computelogLikelihood(betas, instances);
  }

  /**
   * This function computes the penalty term specific to Laplacian distribution.
   * @param betas
   * @param hyperparameters
   */
  public void computePenalty(double[] betas, double[] hyperparameters) {
    penalty = 0.0;

    double lambda = 0.0;

    for (int j = 0; j < betas.length; j++) {
      lambda = Math.sqrt(hyperparameters[j]);
      penalty += (Math.log(2) - Math.log(lambda) +
      (lambda * Math.abs(betas[j])));
    }

    penalty = 0 - penalty;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }
}
