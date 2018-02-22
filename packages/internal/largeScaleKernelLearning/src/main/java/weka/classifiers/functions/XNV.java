package weka.classifiers.functions;
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
 *    XNV.java
 *    Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 *
 */

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Nystroem;
import weka.filters.unsupervised.instance.RemoveRange;

import java.util.Random;

/**
 <!-- globalinfo-start -->
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12341 $
 */
public class XNV extends RandomizableClassifier {

  /** The two Nystroem filters to be used */
  protected Nystroem m_N1;
  protected Nystroem m_N2;

  /** The coefficients from CCA regression */
  protected Matrix m_wCCA;

  /** The CCA projection */
  protected Matrix m_B1;

  /** The sample size for each Nystroem filter */
  protected int m_M = 100;

  /** The kernel function to use. */
  protected Kernel m_Kernel = new RBFKernel();

  /** The regularization parameter. */
  protected double m_Gamma = 0.01;

  /** The maximum number of labeled training instances to use. */
  protected int m_maxNumLabeled = Integer.MAX_VALUE;

  /**
   * Provides information regarding this class.
   *
   * @return string describing the method that this class implements
   */
  public String globalInfo() {
    return "Implements the XNV method for semi-supervised learning using a kernel function.\n\n" +
            "For more information on the algorithm, see\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns a reference to the algorithm implemented by this class.
   *
   * @return a reference to the algorithm implemented by this class
   */
  public TechnicalInformation getTechnicalInformation() {

    TechnicalInformation result = new TechnicalInformation(TechnicalInformation.Type.INPROCEEDINGS);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Brian McWilliams and David Balduzzi and Joachim M. Buhmann");
    result.setValue(TechnicalInformation.Field.TITLE, "Correlated random features for fast semi-supervised learning");
    result.setValue(TechnicalInformation.Field.BOOKTITLE, "Proc 27th Annual Conference on Neural Information Processing Systems");
    result.setValue(TechnicalInformation.Field.PAGES, "440--448");
    result.setValue(TechnicalInformation.Field.YEAR, "2013");
    result.setValue(TechnicalInformation.Field.URL, "http://papers.nips.cc/paper/5000-correlated-random-features-for-fast-semi-supervised-learning.pdf");

    return result;
  }

  @OptionMetadata(
          displayName = "Regularization parameter gamma",
          description = "The regularization parameter gamma.",
          displayOrder = 1,
          commandLineParamName = "G",
          commandLineParamSynopsis = "-G")
  public double getGamma() { return m_Gamma; }
  public void setGamma(double v) {m_Gamma = v; }

  @OptionMetadata(
          displayName = "Sample size for Nystroem method",
          description = "The sample size for the Nystroem method.",
          displayOrder = 2,
          commandLineParamName = "M",
          commandLineParamSynopsis = "-M")
  public int getM() { return m_M; }
  public void setM(int v) {m_M = v; }

  @OptionMetadata(
          displayName = "Kernel function",
          description = "The kernel function to use.", displayOrder = 3,
          commandLineParamName = "K",
          commandLineParamSynopsis = "-K <kernel specification>")
  public void setKernel(Kernel kernel) { m_Kernel = kernel; }
  public Kernel getKernel() { return m_Kernel; }

  @OptionMetadata(
          displayName = "Maximum number of labeled instances to use",
          description = "The maximum number of labeled instances to use (for experimentation).",
          displayOrder = 4,
          commandLineParamName = "maxNumLabeled",
          commandLineParamSynopsis = "-maxNumLabeled")
  public int getMaxNumLabeled() { return m_maxNumLabeled; }
  public void setMaxNumLabeled(int v) {m_maxNumLabeled = v; }

  /**
   * Turns the given set of instances into a data matrix.
   *
   * @param data set of instances
   * @param center whether to center the matrix
   * @param transpose whether to transpose the matrix
   * @return the matrix
   */
  public static DenseMatrix getMatrix(Instances data, boolean center, boolean transpose) {

    double[] means = new double[data.numAttributes()];
    if (center) {
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          means[j] = data.meanOrMode(j);
        }
      }
    }
    int numColumns = transpose ? data.numInstances() : data.numAttributes() - (data.classIndex() >= 0 ? 1 : 0);
    int numRows = transpose ? data.numAttributes() - (data.classIndex() >= 0 ? 1 : 0) : data.numInstances();
    DenseMatrix X = new DenseMatrix(numRows, numColumns);
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      int index = 0;
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          double value = inst.value(j) - means[j];
          if (transpose) {
            X.set(index++, i, value);
          } else {
            X.set(i, index++, value);
          }
        }
      }
    }
    return X;
  }

  /**
   * Returns the inverse of the given matrix.
   *
   * @param M the matrix to invert
   * @return the inverse
   */
  public static Matrix inverse(Matrix M) throws Exception {

    if (M.numRows() != M.numColumns()) {
      throw new IllegalArgumentException("Matrix is not square: cannot invert it.");
    }
    Matrix Minv = new DenseMatrix(M.numRows(), M.numRows());
    Matrix I = Matrices.identity(M.numRows());
    M.solve(I, Minv);
    return new DenseMatrix(Minv);
  }

  /**
   * Performs Canonical Correlation Analysis (CCA).
   *
   * @param X1 the first data matrix
   * @param X2 the second data matrix
   * @return the eigenvalue decomposition giving the basis in X1
   */
  public static EVD CCA(Matrix X1, Matrix X2) throws Exception {

    int M = X1.numRows();
    int N = X1.numColumns();
    UpperSPDDenseMatrix CX1X1 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X1);
    CX1X1.scale(1.0 / (N - 1.0));
    for (int i = 0; i < M; i++) {
      CX1X1.set(i, i, CX1X1.get(i, i) + 1e-8);
    }

    UpperSPDDenseMatrix CX2X2 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X2);
    CX2X2.scale(1.0 / (N - 1.0));
    for (int i = 0; i < M; i++) {
      CX2X2.set(i, i, CX2X2.get(i, i) + 1e-8);
    }

    // Compute covariance between views
    Matrix CX1X2 = X1.transBmult(X2, new DenseMatrix(M, M));
    CX1X2.scale(1.0 / (N - 1.0));
    Matrix CX2X1 = CX1X2.transpose(new DenseMatrix(M, M));

    // Establish key matrix and perform eigenvalue decomposition
    Matrix CX1X1invMultCX1X2 = inverse(CX1X1).mult(CX1X2, new DenseMatrix(M, M));
    Matrix CX2X2invMultCX2X1 = inverse(CX2X2).mult(CX2X1, new DenseMatrix(M, M));
    Matrix CX1X1invMultCX1X2MultCX2X2invMultCX2X1 = CX1X1invMultCX1X2.mult(CX2X2invMultCX2X1, new DenseMatrix(M, M));
    EVD evd = EVD.factorize(CX1X1invMultCX1X2MultCX2X2invMultCX2X1);

    return evd;
  }

  /**
   * Builds the XNV regressor.
   *
   * @param data set of instances serving as training data
   * @throws Exception
   */
  public void buildClassifier(Instances data) throws Exception {

    getCapabilities().testWithFail(data);

    // Shuffle the data
    data = new Instances(data);
    data.randomize(new Random(getSeed()));

    // Reduce M if necessary
    int M = Math.min(m_M, data.numInstances() / 2);

    // Build first Nystroem filter and generate first view, including covariance matrix
    m_N1 = new Nystroem();
    RemoveRange rr1 = new RemoveRange();
    rr1.setInvertSelection(true);
    rr1.setInstancesIndices("first-" + M);
    m_N1.setFilter(rr1);
    m_N1.setKernel((Kernel) new SerializedObject(m_Kernel).getObject());
    m_N1.setInputFormat(data);
    Instances N1data = Filter.useFilter(data, m_N1);
    Matrix X1 = getMatrix(N1data, true, true);

    // Build second Nystroem filter and generate second view, including covariance matrix
    m_N2 = new Nystroem();
    RemoveRange rr2 = new RemoveRange();
    rr2.setInvertSelection(true);
    rr2.setInstancesIndices((M + 1) + "-" + (2 * M));
    m_N2.setFilter(rr2);
    m_N2.setKernel((Kernel) new SerializedObject(m_Kernel).getObject());
    m_N2.setInputFormat(data);
    Instances N2data = Filter.useFilter(data, m_N2);
    Matrix X2 = getMatrix(N2data, true, true);

    EVD evd = CCA(X1, X2);
    X1 = X2 = null; N2data = null;

    double[] e1 = evd.getRealEigenvalues();
    m_B1 = evd.getRightEigenvectors();

    double[] e1Reordered = new double[e1.length];
    Matrix B1Reordered = new DenseMatrix(m_B1.numRows(), m_B1.numColumns());
    for (int i = 0; i < e1.length; i++) { // We want descending rather than ascending for easier comparison
      e1Reordered[e1.length - (i + 1)] = Math.sqrt(e1[i]);
      for (int j = 0; j < m_B1.numRows(); j++) {
        B1Reordered.set(j, i, m_B1.get(j, (e1.length - (i + 1))));
      }
    }
    e1 = e1Reordered;
    m_B1 = B1Reordered;

    // Get labeled training data
    Instances labeledN1 = new Instances(N1data, N1data.numInstances());
    for (Instance inst : N1data) {
      if (labeledN1.numInstances() >= m_maxNumLabeled) {
        break;
      }
      if (!inst.classIsMissing()) {
        labeledN1.add(inst);
      }
    }

    // Get matrix with labels
    DenseMatrix labels = new DenseMatrix(labeledN1.numInstances(), 1);
    for (int i = 0; i < labeledN1.numInstances(); i++) {
      labels.set(i, 0, labeledN1.instance(i).classValue());
    }

    // Compute CCA regression
    DenseMatrix Z1 = getMatrix(labeledN1, false, false);
    Matrix Z = Z1.mult(m_B1, new DenseMatrix(labeledN1.numInstances(), M));
    Matrix CCA_reg = new DenseMatrix(M, M);
    Matrix reg = new DenseMatrix(M, M);
    for (int i = 0; i < e1.length; i++) {
      CCA_reg.set(i, i, (1.0 - e1[i]) / e1[i]);
      reg.set(i, i, m_Gamma);
    }
    Matrix inv = inverse(Z.transAmult(Z, new DenseMatrix(M, M)).add(CCA_reg).add(reg));
    m_wCCA = inv.transBmult(Z, new DenseMatrix(Z.numColumns(), Z.numRows())).mult(labels, new DenseMatrix(M, 1));
  }

  /**
   * Returns prediction for an instance.
   *
   * @param inst
   * @return a one-element array with the prediction
   */
  public double[] distributionForInstance(Instance inst) throws Exception {

    Instances data = new Instances(inst.dataset(), 0);
    data.add(inst);
    return distributionsForInstances(data)[0];
  }

  /**
   * This class implements efficient batch prediction.
   *
   * @return true
   */
  public boolean implementsMoreEfficientBatchPrediction() { return true; }

  /**
   * Returns predictions for a whole set of instances.
   *
   * @param insts the instances to make predictions for
   * @return the 2D array with results
   */
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    Matrix result = getMatrix(Filter.useFilter(insts, m_N1), false, false);
    result = result.mult(m_B1, new DenseMatrix(result.numRows(), result.numColumns()));
    result = result.mult(m_wCCA, new DenseMatrix(insts.numInstances(), 1));
    double[][] preds = new double[insts.numInstances()][1];
    for (int i = 0; i < insts.numInstances(); i++) {
      preds[i][0] = result.get(i, 0);
    }

    return preds;
  }

  /**
   * Outputs the coefficients of the classifier.
   *
   * @return a textual description of the classifier
   */
  public String toString() {

    if (m_wCCA == null) {
      return "XNV: No model built yet.";
    } else {
      return "XNV weight vector (beta):\n\n" + m_wCCA;
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capabilities.Capability.NUMERIC_CLASS);

    return result;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 12037 $");
  }

  /**
   * Generates an XNV predictor.
   *
   * @param argv the options
   */
  public static void main(String argv[]) throws Exception {

    runClassifier(new XNV(), argv);
  }
}
