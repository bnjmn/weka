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

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.core.Utils;

import java.io.Serializable;

/**
 * Implementation of maximum likelihood Multivariate Distribution Estimation using Normal
 * Distribution.
 * 
 * @author Uday Kamath, PhD, George Mason University
 * @author Eibe Frank, University of Waikato
 * @version $Revision$
 *
 */
public class MultivariateGaussianEstimator implements MultivariateEstimator, Serializable {

  /** Mean vector */
  protected DenseVector mean;

  /** Inverse of covariance matrix */
  protected UpperSPDDenseMatrix covarianceInverse;

  /** Factor to make density integrate to one (log of this factor) */
  protected double lnconstant;

  /** Ridge parameter to add to diagonal of covariance matrix */
  protected double m_Ridge = 1e-6;

  /**
   * Log of twice the number pi: log(2*pi).
   */
  public static final double Log2PI = Math.log(2 * Math.PI);

  /**
   * Returns string summarizing the estimator.
   */
  public String toString() {

    StringBuffer sb = new StringBuffer();
    sb.append("Natural logarithm of normalizing factor: " + lnconstant + "\n\n");
    sb.append("Mean vector:\n\n" + mean + "\n");
    sb.append("Inverse of covariance matrix:\n\n" + covarianceInverse + "\n");
    return sb.toString();
  }

  /**
   * Returns the mean vector.
   */
  public double[] getMean() {

    return mean.getData();
  }

  /**
   * Returns the log of the density value for the given vector.
   *
   * @param valuePassed input vector
   * @return log density based on given distribution
   */
  @Override
  public double logDensity(double[] valuePassed) {

    // calculate mean subtractions
    Vector x = new DenseVector(valuePassed);

    return lnconstant - 0.5 * x.dot(covarianceInverse.mult(x.add(-1.0, mean), new DenseVector(x.size())));
  }

  /**
   * Generates the estimator based on the given observations and weight vector.
   * Equal weights are assumed if the weight vector is null.
   */
  @Override
  public void estimate(double[][] observations, double[] weights) {

    if (weights == null) {
      weights = new double[observations.length];
      for (int i = 0; i < weights.length; i++) {
        weights[i] = 1.0;
      }
    }

    DenseVector weightVector = new DenseVector(weights);
    weightVector = weightVector.scale(1.0 / weightVector.norm(Vector.Norm.One));

    mean = weightedMean(observations, weightVector);
    Matrix cov = weightedCovariance(observations, weightVector, mean);

    // Compute inverse of covariance matrix
    DenseCholesky chol = new DenseCholesky(observations[0].length, true).factor((UpperSPDDenseMatrix)cov);
    covarianceInverse = new UpperSPDDenseMatrix(chol.solve(Matrices.identity(observations[0].length)));

    double logDeterminant = 0;
    for (int i = 0; i < observations[0].length; i++) {
      logDeterminant += Math.log(chol.getU().get(i, i));
    }
    logDeterminant *= 2;
    lnconstant = -(Log2PI * observations[0].length + logDeterminant) * 0.5;
  }

  /**
   * Generates pooled estimator for linear discriminant analysis based on the given groups of
   * observations and weight vectors. The pooled covariance matrix is the weighted mean
   * of the per-group covariance matrices. The pooled mean vector is the mean vector for all observations.
   *
   * @return the per group mean vectors
   */
  public double[][] estimatePooled(double[][][] observations, double[][] weights) {

    // Establish number of attributes and number of classes
    int m = -1;
    int c = observations.length;
    for (int i = 0; i < observations.length; i++) {
      if (observations[i].length > 0) {
        m = observations[i][0].length;
      }
    }
    if (m == -1) {
      throw new IllegalArgumentException("Cannot compute pooled estimates with no data.");
    }

    // Compute per-group covariance matrices and mean vectors
    Matrix[] groupCovariance = new Matrix[c];
    DenseVector[] groupMean = new DenseVector[c];
    double[] groupWeights = new double[c];
    for (int i = 0; i < groupCovariance.length; i++) {
      if (observations[i].length > 0) {
	DenseVector weightVector = new DenseVector(weights[i]);
	weightVector = weightVector.scale(1.0 / weightVector.norm(Vector.Norm.One));
        groupMean[i] = weightedMean(observations[i], weightVector);
        groupCovariance[i] = weightedCovariance(observations[i], weightVector, groupMean[i]);
        groupWeights[i] = Utils.sum(weights[i]);
      }
    }
    Utils.normalize(groupWeights);

    // Pool covariance matrices and means
    double[][] means = new double[c][];
    Matrix cov = new UpperSPDDenseMatrix(m);
    mean = new DenseVector(groupMean[0].size());
    for (int i = 0; i < c; i++) {
      if (observations[i].length > 0) {
        cov = cov.add(groupWeights[i], groupCovariance[i]);
        mean = (DenseVector) mean.add(groupWeights[i], groupMean[i]);
        means[i] = groupMean[i].getData();
      }
    }

    // Compute inverse of covariance matrix
    DenseCholesky chol = new DenseCholesky(m, true).factor((UpperSPDDenseMatrix)cov);
    covarianceInverse = new UpperSPDDenseMatrix(chol.solve(Matrices.identity(m)));

    double logDeterminant = 0;
    for (int i = 0; i < m; i++) {
      logDeterminant += Math.log(chol.getU().get(i, i));
    }
    logDeterminant *= 2;
    lnconstant = -(Log2PI * m + logDeterminant) * 0.5;

    return means;
  }

  /**
   * Computes the mean vector
   * @param matrix the data (assumed to contain at least one row)
   * @param weights the observation weights, normalized to sum to 1.
   * @return the weighted mean
   */
  private DenseVector weightedMean(double[][] matrix, DenseVector weights) {

    return (DenseVector)new DenseMatrix(matrix).transMult(weights, new DenseVector(matrix[0].length));
  }

  /**
   * Computes the estimate of the covariance matrix.
   *
   * @param matrix A multi-dimensional array containing the matrix values (assumed to contain at least one row).
   * @param weights The observation weights, normalized to sum to 1.
   * @param mean The values' mean vector.
   * @return The covariance matrix, including the ridge.
   */
  private UpperSPDDenseMatrix weightedCovariance(double[][] matrix, DenseVector weights,  Vector mean) {

    int rows = matrix.length;
    int cols = matrix[0].length;

    if (mean.size() != cols) {
      throw new IllegalArgumentException("Length of the mean vector must match matrix.");
    }

    // Create matrix with centered transposed data, weighted appropriately
    DenseMatrix transposed = new DenseMatrix(cols, rows);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        transposed.set(j, i, Math.sqrt(weights.get(i)) * (matrix[i][j] - mean.get(j)));
      }
    }

    UpperSPDDenseMatrix covT = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(cols).rank1(transposed);
    for (int i = 0; i < cols; i++) {
      covT.add(i, i, m_Ridge);
    }

    return covT;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "The value of the ridge parameter.";
  }

  /**
   * Get the value of Ridge.
   *
   * @return Value of Ridge.
   */
  public double getRidge() {

    return m_Ridge;
  }

  /**
   * Set the value of Ridge.
   *
   * @param newRidge Value to assign to Ridge.
   */
  public void setRidge(double newRidge) {

    m_Ridge = newRidge;
  }

  /**
   * Main method for testing this class.
   * @param args command-line parameters
   */
  public static void main(String[] args) {

    double[][] dataset1 = new double[4][1];
    dataset1[0][0] = 0.49;
    dataset1[1][0] = 0.46;
    dataset1[2][0] = 0.51;
    dataset1[3][0] = 0.55;

    MultivariateEstimator mv1 = new MultivariateGaussianEstimator();
    mv1.estimate(dataset1, new double[]{0.7, 0.2, 0.05, 0.05});

    System.err.println(mv1);

    double integral1 = 0;
    int numVals = 1000;
    for (int i = 0; i < numVals; i++) {
      double[] point = new double[1];
      point[0] = (i + 0.5) * (1.0 / numVals);
      double logdens = mv1.logDensity(point);
      if (!Double.isNaN(logdens)) {
        integral1 += Math.exp(logdens) * (1.0 / numVals);
      }
    }
    System.err.println("Approximate integral: " + integral1);

    double[][] dataset = new double[4][3];
    dataset[0][0] = 0.49;
    dataset[0][1] = 0.51;
    dataset[0][2] = 0.53;
    dataset[1][0] = 0.46;
    dataset[1][1] = 0.47;
    dataset[1][2] = 0.52;
    dataset[2][0] = 0.51;
    dataset[2][1] = 0.49;
    dataset[2][2] = 0.47;
    dataset[3][0] = 0.55;
    dataset[3][1] = 0.52;
    dataset[3][2] = 0.54;

    MultivariateEstimator mv = new MultivariateGaussianEstimator();
    mv.estimate(dataset, new double[]{2, 0.2, 0.05, 0.05});

    System.err.println(mv);

    double integral = 0;
    int numVals2 = 200;
    for (int i = 0; i < numVals2; i++) {
      for (int j = 0; j < numVals2; j++) {
        for (int k = 0; k < numVals2; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals2);
          point[1] = (j + 0.5) * (1.0 / numVals2);
          point[2] = (k + 0.5) * (1.0 / numVals2);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral += Math.exp(logdens) / (numVals2 * numVals2 * numVals2);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral);

    double[][] dataset3 = new double[5][3];
    dataset3[0][0] = 0.49;
    dataset3[0][1] = 0.51;
    dataset3[0][2] = 0.53;
    dataset3[4][0] = 0.49;
    dataset3[4][1] = 0.51;
    dataset3[4][2] = 0.53;
    dataset3[1][0] = 0.46;
    dataset3[1][1] = 0.47;
    dataset3[1][2] = 0.52;
    dataset3[2][0] = 0.51;
    dataset3[2][1] = 0.49;
    dataset3[2][2] = 0.47;
    dataset3[3][0] = 0.55;
    dataset3[3][1] = 0.52;
    dataset3[3][2] = 0.54;

    MultivariateEstimator mv3 = new MultivariateGaussianEstimator();
    mv3.estimate(dataset3, new double[]{1, 0.2, 0.05, 0.05, 1});

    System.err.println(mv3);

    double integral3 = 0;
    int numVals3 = 200;
    for (int i = 0; i < numVals3; i++) {
      for (int j = 0; j < numVals3; j++) {
        for (int k = 0; k < numVals3; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals3);
          point[1] = (j + 0.5) * (1.0 / numVals3);
          point[2] = (k + 0.5) * (1.0 / numVals3);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral3 += Math.exp(logdens) / (numVals3 * numVals3 * numVals3);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral3);

    double[][][] dataset4 = new double[2][][];
    dataset4[0] = new double[2][3];
    dataset4[1] = new double[3][3];
    dataset4[0][0][0] = 0.49;
    dataset4[0][0][1] = 0.51;
    dataset4[0][0][2] = 0.53;
    dataset4[0][1][0] = 0.49;
    dataset4[0][1][1] = 0.51;
    dataset4[0][1][2] = 0.53;
    dataset4[1][0][0] = 0.46;
    dataset4[1][0][1] = 0.47;
    dataset4[1][0][2] = 0.52;
    dataset4[1][1][0] = 0.51;
    dataset4[1][1][1] = 0.49;
    dataset4[1][1][2] = 0.47;
    dataset4[1][2][0] = 0.55;
    dataset4[1][2][1] = 0.52;
    dataset4[1][2][2] = 0.54;
    double[][] weights = new double[2][];
    weights[0] = new double[] {1, 3};
    weights[1] = new double[] {2, 1, 1};

    MultivariateGaussianEstimator mv4 = new MultivariateGaussianEstimator();
    mv4.estimatePooled(dataset4, weights);

    System.err.println(mv4);

    double integral4 = 0;
    int numVals4 = 200;
    for (int i = 0; i < numVals4; i++) {
      for (int j = 0; j < numVals4; j++) {
        for (int k = 0; k < numVals4; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals4);
          point[1] = (j + 0.5) * (1.0 / numVals4);
          point[2] = (k + 0.5) * (1.0 / numVals4);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral4 += Math.exp(logdens) / (numVals4 * numVals4 * numVals4);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral4);

    double[][][] dataset5 = new double[2][][];
    dataset5[0] = new double[4][3];
    dataset5[1] = new double[4][3];
    dataset5[0][0][0] = 0.49;
    dataset5[0][0][1] = 0.51;
    dataset5[0][0][2] = 0.53;
    dataset5[0][1][0] = 0.49;
    dataset5[0][1][1] = 0.51;
    dataset5[0][1][2] = 0.53;
    dataset5[0][2][0] = 0.49;
    dataset5[0][2][1] = 0.51;
    dataset5[0][2][2] = 0.53;
    dataset5[0][3][0] = 0.49;
    dataset5[0][3][1] = 0.51;
    dataset5[0][3][2] = 0.53;
    dataset5[1][0][0] = 0.46;
    dataset5[1][0][1] = 0.47;
    dataset5[1][0][2] = 0.52;
    dataset5[1][1][0] = 0.46;
    dataset5[1][1][1] = 0.47;
    dataset5[1][1][2] = 0.52;
    dataset5[1][2][0] = 0.51;
    dataset5[1][2][1] = 0.49;
    dataset5[1][2][2] = 0.47;
    dataset5[1][3][0] = 0.55;
    dataset5[1][3][1] = 0.52;
    dataset5[1][3][2] = 0.54;
    double[][] weights2 = new double[2][];
    weights2[0] = new double[] {1, 1, 1, 1};
    weights2[1] = new double[] {1, 1, 1, 1};

    MultivariateGaussianEstimator mv5 = new MultivariateGaussianEstimator();
    mv5.estimatePooled(dataset5, weights2);

    System.err.println(mv5);

    double integral5 = 0;
    int numVals5 = 200;
    for (int i = 0; i < numVals5; i++) {
      for (int j = 0; j < numVals5; j++) {
        for (int k = 0; k < numVals5; k++) {
          double[] point = new double[3];
          point[0] = (i + 0.5) * (1.0 / numVals5);
          point[1] = (j + 0.5) * (1.0 / numVals5);
          point[2] = (k + 0.5) * (1.0 / numVals5);
          double logdens = mv.logDensity(point);
          if (!Double.isNaN(logdens)) {
            integral5 += Math.exp(logdens) / (numVals5 * numVals5 * numVals5);
          }
        }
      }
    }
    System.err.println("Approximate integral: " + integral5);
  }
}
