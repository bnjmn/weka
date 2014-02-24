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
 *    MultivariateNormalEstimator.java
 *    Copyright (C) 2013 University of Waikato
 */

package weka.estimators;

import weka.core.matrix.CholeskyDecomposition;
import weka.core.matrix.Matrix;

/**
 * Implementation of Multivariate Distribution Estimation using Normal
 * Distribution. *
 * 
 * @author Uday Kamath, PhD, George Mason University
 * @version $Revision$
 * 
 */
public class MultivariateGaussianEstimator implements MultivariateEstimator,
  Cloneable {
  // Distribution parameters
  protected double[] mean;
  protected double[][] covariance;
  // cholesky decomposition for fast determinant calculation
  private CholeskyDecomposition chol;
  private double lnconstant;

  @Override
  public MultivariateGaussianEstimator clone() {
    MultivariateGaussianEstimator clone = new MultivariateGaussianEstimator();
    clone.mean = this.mean;
    clone.covariance = this.covariance;
    clone.lnconstant = this.lnconstant;
    if (this.chol != null) {
      clone.chol = new CholeskyDecomposition((Matrix) this.chol.getL().clone());
    }  
    return clone;
  }

  public MultivariateGaussianEstimator() {

  }

  public MultivariateGaussianEstimator(double[] means, double[][] covariance) {
    this.mean = means;
    this.covariance = covariance;
    this.chol = new CholeskyDecomposition(new Matrix(covariance));
   this.recalculate(this.mean, this.covariance, this.chol);  
}

  /**
   * Log of twice number pi: log(2*pi).
   */
  public static final double Log2PI = 1.837877066409345483556;

  /**
   * Returns the probability density estimate at the given point.
   * 
   * @param value the value at which to evaluate
   * @return the the density estimate at the given value
   */
  @Override
  public double getProbability(double[] value) {
    double prob = Math.exp(logDensity(value));
    return prob > 1 ? 1 : prob;
  }

  /**
   * Returns the log likelihood of density value for the Multivariate
   * distribution
   * 
   * @param input vector
   * @return log density based on given distribution
   */
  @Override
  public double logDensity(double[] valuePassed) {
    double[] value = valuePassed.clone();
    double logProb = 0;
    // calculate mean subtractions
    double[] subtractedMean = new double[value.length];
    for (int i = 0; i < value.length; i++) {
      subtractedMean[i] = value[i] - mean[i];
    }
    value = subtractedMean.clone();
    double[][] L = this.chol.getL().getArray();
    int n = this.chol.getL().getRowDimension();
    // Solve L*Y = B;
    for (int k = 0; k < this.chol.getL().getRowDimension(); k++) {
      for (int i = 0; i < k; i++) {
        value[k] -= value[i] * L[k][i];
      }

      value[k] /= L[k][k];
    }

    // Solve L'*X = Y;
    for (int k = n - 1; k >= 0; k--) {
      for (int i = k + 1; i < n; i++) {
        value[k] -= value[i] * L[i][k];
      }
      value[k] /= L[k][k];
    }

    // compute dot product
    double innerProduct = 0;
    // do a fast dot product
    for (int i = 0; i < value.length; i++) {
      innerProduct += value[i] * subtractedMean[i];
    }
    logProb = lnconstant - innerProduct * 0.5;
    return logProb;
  }

  /**
   * 
   * @see weka.estimators.MultivariateEstimator#estimate(double[][], double[])
   */
  @Override
  public void estimate(double[][] observations, double[] weights) {
    double[] means;
    double[][] cov;

    if (weights != null) {
      double sum = 0;
      for (double weight : weights) {
        if (Double.isNaN(weight) || Double.isInfinite(weight)) {
          throw new IllegalArgumentException(
            "Invalid numbers in the weight vector");
        }
        sum += weight;
      }

      if (Math.abs(sum - 1.0) > 1e-10) {
        throw new IllegalArgumentException("Weights do not sum to one");
      }

      means = weightedMean(observations, weights, 0);
      cov = weightedCovariance(observations, weights, means);

    } else {
      // Compute mean vector
      means = mean(observations);
      cov = covariance(observations, means);
    }

    CholeskyDecomposition chol = new CholeskyDecomposition(new Matrix(cov));

    // Become the newly fitted distribution.
    recalculate(means, cov, chol);
  }

  public double[] getMean() {
    return this.mean;
  }

  public double[][] getCovariance() {
    return this.covariance;
  }

  private void recalculate(double[] m, double[][] cov, CholeskyDecomposition cd) {
    int k = m.length;

    this.mean = m;
    this.covariance = cov;
    this.chol = cd;

    // Original code:
    // double det = chol.Determinant;
    // double detSqrt = Math.sqrt(Math.abs(det));
    // constant = 1.0 / (Math.Pow(2.0 * System.Math.PI, k / 2.0) *
    // detSqrt);

    // Transforming to log operations, we have:
    double lndet = getLogDeterminant(cd.getL());

    // Let lndet = log( abs(det) )
    //
    // detSqrt = sqrt(abs(det)) = sqrt(abs(exp(log(det)))
    // = sqrt(exp(log(abs(det))) = sqrt(exp(lndet)
    //
    // log(detSqrt) = log(sqrt(exp(lndet)) = (1/2)*log(exp(lndet))
    // = lndet/2.
    //
    //
    // Let lndetsqrt = log(detsqrt) = lndet/2
    //
    // constant = 1 / ( ((2PI)^(k/2)) * detSqrt)
    //
    // log(constant) = log( 1 / ( ((2PI)^(k/2)) * detSqrt) )
    // = log(1)-log(((2PI)^(k/2)) * detSqrt) )
    // = 0 -log(((2PI)^(k/2)) * detSqrt) )
    // = -log( ((2PI)^(k/2)) * detSqrt) )
    // = -log( ((2PI)^(k/2)) * exp(log(detSqrt))))
    // = -log( ((2PI)^(k/2)) * exp(lndetsqrt)))
    // = -log( ((2PI)^(k/2)) * exp(lndet/2)))
    // = -log( ((2PI)^(k/2))) - log(exp(lndet/2)))
    // = -log( ((2PI)^(k/2))) - lndet/2)
    // = -log( 2PI) * (k/2) - lndet/2)
    // =(-log( 2PI) * k - lndet ) / 2
    // =-(log( 2PI) * k + lndet ) / 2
    // =-(log( 2PI) * k + lndet ) / 2
    // =-( LN2PI * k + lndet ) / 2
    //

    // So the log(constant) could be computed as:
    lnconstant = -(Log2PI * k + lndet) * 0.5;
  }

  private double getLogDeterminant(Matrix L) {
    double logDeterminant;
    double detL = 0;
    int n = L.getRowDimension();
    double[][] matrixAsArray = L.getArray();
    for (int i = 0; i < n; i++) {

      detL += Math.log(matrixAsArray[i][i]);
    }
    logDeterminant = detL * 2;

    return logDeterminant;
  }

  private double[] weightedMean(double[][] matrix, double[] weights,
    int columnSum) {
    int rows = matrix.length;
    if (rows == 0) {
      return new double[0];
    }
    int cols = matrix[0].length;
    double[] mean;

    if (columnSum == 0) {
      mean = new double[cols];

      // for each row
      for (int i = 0; i < rows; i++) {
        double[] row = matrix[i];
        double w = weights[i];

        // for each column
        for (int j = 0; j < cols; j++) {
          mean[j] += row[j] * w;
        }
      }
    } else if (columnSum == 1) {
      mean = new double[rows];

      // for each row
      for (int j = 0; j < rows; j++) {
        double[] row = matrix[j];
        double w = weights[j];

        // for each column
        for (int i = 0; i < cols; i++) {
          mean[j] += row[i] * w;
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid dimension");
    }

    return mean;
  }

  /**
   * Calculates the scatter matrix of a sample matrix.
   * 
   * 
   * By dividing the Scatter matrix by the sample size, we get the population
   * Covariance matrix. By dividing by the sample size minus one, we get the
   * sample Covariance matrix.
   * 
   * @param matrix A number multi-dimensional array containing the matrix
   *          values.
   * @param weights An unit vector containing the importance of each sample in
   *          <see param="values"/>. The sum of this array elements should add
   *          up to 1.
   * @param means The values' mean vector, if already known.
   * @return The covariance matrix.
   */
  private double[][] weightedCovariance(double[][] matrix, double[] weights,
    double[] means) {
    double sw = 1.0;
    for (double weight : weights) {
      sw -= weight * weight;
    }

    return weightedScatter(matrix, weights, means, sw, 0);
  }

  /**
   * Calculates the scatter matrix of a sample matrix.
   * 
   * 
   * By dividing the Scatter matrix by the sample size, we get the population
   * Covariance matrix. By dividing by the sample size minus one, we get the
   * sample Covariance matrix.
   * 
   * @param matrix A number multi-dimensional array containing the matrix
   *          values.
   * @param weights An unit vector containing the importance of each sample in
   *          <see param="values"/>. The sum of this array elements should add
   *          up to 1.
   * @param means The values' mean vector, if already known.
   * @param divisor A real number to divide each member of the matrix.
   * @param dimension Pass 0 to if mean vector is a row vector, 1 otherwise.
   *          Default value is 0.
   * 
   * @return The covariance matrix.
   */
  private double[][] weightedScatter(double[][] matrix, double[] weights,
    double[] means, double divisor, int dimension) {
    int rows = matrix.length;
    if (rows == 0) {
      return new double[0][0];
    }
    int cols = matrix[0].length;

    double[][] cov;

    if (dimension == 0) {
      if (means.length != cols) {
        throw new IllegalArgumentException(
          "Length of the mean vector should equal the number of columns");
      }

      cov = new double[cols][cols];
      for (int i = 0; i < cols; i++) {
        for (int j = i; j < cols; j++) {
          double s = 0.0;
          for (int k = 0; k < rows; k++) {
            s += weights[k] * (matrix[k][j] - means[j])
              * (matrix[k][i] - means[i]);
          }
          s /= divisor;
          cov[i][j] = s;
          cov[j][i] = s;
        }
      }
    } else if (dimension == 1) {
      if (means.length != rows) {
        throw new IllegalArgumentException(
          "Length of the mean vector should equal the number of rows");
      }

      cov = new double[rows][rows];
      for (int i = 0; i < rows; i++) {
        for (int j = i; j < rows; j++) {
          double s = 0.0;
          for (int k = 0; k < cols; k++) {
            s += weights[k] * (matrix[j][k] - means[j])
              * (matrix[i][k] - means[i]);
          }
          s /= divisor;
          cov[i][j] = s;
          cov[j][i] = s;
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid dimension");
    }

    return cov;
  }

  private double[] mean(double[][] matrix) {
    return mean(matrix, 0);
  }

  private double[] mean(double[][] matrix, int dimension) {
    int rows = matrix.length;
    int cols = matrix[0].length;
    double[] mean;

    if (dimension == 0) {
      mean = new double[cols];
      double N = rows;

      // for each column
      for (int j = 0; j < cols; j++) {
        // for each row
        for (int i = 0; i < rows; i++) {
          mean[j] += matrix[i][j];
        }

        mean[j] /= N;
      }
    } else if (dimension == 1) {
      mean = new double[rows];
      double N = cols;

      // for each row
      for (int j = 0; j < rows; j++) {
        // for each column
        for (int i = 0; i < cols; i++) {
          mean[j] += matrix[j][i];
        }

        mean[j] /= N;
      }
    } else {
      throw new IllegalArgumentException("Invalid dimension");
    }

    return mean;
  }

  /**
   * Calculates the covariance matrix of a sample matrix.
   * 
   * 
   * 
   * In statistics and probability theory, the covariance matrix is a matrix of
   * covariances between elements of a vector. It is the natural generalization
   * to higher dimensions of the concept of the variance of a scalar-valued
   * random variable.
   * 
   * 
   * @param matrix A number multi-dimensional array containing the matrix
   *          values.
   * @param means The values' mean vector, if already known.
   * 
   * @return The covariance matrix.
   * 
   */
  public static double[][] covariance(double[][] matrix, double[] means) {
    return scatter(matrix, means, matrix.length - 1, 0);
  }

  /**
   * Calculates the scatter matrix of a sample matrix.
   * 
   * 
   * By dividing the Scatter matrix by the sample size, we get the population
   * Covariance matrix. By dividing by the sample size minus one, we get the
   * sample Covariance matrix.
   * 
   * @param matrix A number multi-dimensional array containing the matrix
   *          values.
   * @param means The values' mean vector, if already known.
   * @param divisor A real number to divide each member of the matrix.
   * @param dimension Pass 0 to if mean vector is a row vector, 1 otherwise.
   *          Default value is 0.
   * 
   * @return The covariance matrix.
   */
  public static double[][] scatter(double[][] matrix, double[] means,
    double divisor, int dimension) {
    int rows = matrix.length;
    if (rows == 0) {
      return new double[0][0];
    }
    int cols = matrix[0].length;

    double[][] cov;

    if (dimension == 0) {
      if (means.length != cols) {
        throw new IllegalArgumentException(
          "Length of the mean vector should equal the number of columns");
      }

      cov = new double[cols][cols];
      for (int i = 0; i < cols; i++) {
        for (int j = i; j < cols; j++) {
          double s = 0.0;
          for (int k = 0; k < rows; k++) {
            s += (matrix[k][j] - means[j]) * (matrix[k][i] - means[i]);
          }
          s /= divisor;
          cov[i][j] = s;
          cov[j][i] = s;
        }
      }
    } else if (dimension == 1) {
      if (means.length != rows) {
        throw new IllegalArgumentException(
          "Length of the mean vector should equal the number of rows");
      }

      cov = new double[rows][rows];
      for (int i = 0; i < rows; i++) {
        for (int j = i; j < rows; j++) {
          double s = 0.0;
          for (int k = 0; k < cols; k++) {
            s += (matrix[j][k] - means[j]) * (matrix[i][k] - means[i]);
          }
          s /= divisor;
          cov[i][j] = s;
          cov[j][i] = s;
        }
      }
    } else {
      throw new IllegalArgumentException("Invalid dimension");
    }

    return cov;
  }

  public static void main(String[] args) {
    double[][] dataset = new double[4][3];
    dataset[0][0] = 10.0;
    dataset[0][1] = 3.0;
    dataset[0][2] = 38.0;
    dataset[1][0] = 12.0;
    dataset[1][1] = 4.0;
    dataset[1][2] = 34.0;
    dataset[2][0] = 20.0;
    dataset[2][1] = 10.0;
    dataset[2][2] = 74.0;
    dataset[3][0] = 10.0;
    dataset[3][1] = 1.0;
    dataset[3][2] = 40.0;

    MultivariateEstimator mv = new MultivariateGaussianEstimator();
    mv.estimate(dataset, new double[] { 0.7, 0.2, 0.05, 0.05 });
    double[] newData = new double[] { 12, 4, 34 };
    System.out.println(mv.getProbability(newData));

  }
}
