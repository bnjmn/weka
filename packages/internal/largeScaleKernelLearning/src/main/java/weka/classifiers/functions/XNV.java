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

package weka.classifiers.functions;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Nystroem;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.instance.RemoveRange;

import java.util.ArrayList;
import java.util.Random;

/**
 <!-- globalinfo-start -->
 * Implements the XNV method for semi-supervised learning using a kernel function (default: RBFKernel). Standardizes all attributes, including the target, by default. Applies (unsupervised) NominalToBinary and ReplaceMissingValues before anything else is done.<br>
 * <br>
 * For more information on the algorithm, see<br>
 * <br>
 * Brian McWilliams, David Balduzzi, Joachim M. Buhmann: Correlated random features for fast semi-supervised learning. In: Proc 27th Annual Conference on Neural Information Processing Systems, 440--448, 2013.
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{McWilliams2013,
 *    author = {Brian McWilliams and David Balduzzi and Joachim M. Buhmann},
 *    booktitle = {Proc 27th Annual Conference on Neural Information Processing Systems},
 *    pages = {440--448},
 *    title = {Correlated random features for fast semi-supervised learning},
 *    year = {2013},
 *    URL = {http://papers.nips.cc/paper/5000-correlated-random-features-for-fast-semi-supervised-learning.pdf}
 * }
 * </pre>
 * <br><br>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -G
 *  The regularization parameter gamma.</pre>
 * 
 * <pre> -M
 *  The sample size for the Nystroem method.</pre>
 * 
 * <pre> -K &lt;kernel specification&gt;
 *  The kernel function to use.</pre>
 * 
 * <pre> -S
 *  If true, standardization will not be performed.</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 * <pre> -batch-size
 *  The desired batch size for batch prediction  (default 100).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12341 $
 */
public class XNV extends RandomizableClassifier implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -1585383626378691736L;

  /** The two Nystroem filters to be used */
  protected Nystroem m_N1;
  protected Nystroem m_N2;

  /** The coefficients from CCA regression */
  protected Matrix m_wCCA;

  /** The CCA projection */
  protected Matrix m_B1;

  /** The filter used for standardizing the data */
  protected Standardize m_Standardize;

  /** The filter used to make attributes numeric. */
  protected NominalToBinary m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing;

  /** Coefficients used compensate for standardization of the target */
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  /** The sample size for each Nystroem filter */
  protected int m_M = 100;

  /** The kernel function to use. */
  protected Kernel m_Kernel = new RBFKernel();

  /** The regularization parameter. */
  protected double m_Gamma = 0.01;

  /** Stores the number of labeled instances found in the training set. */
  protected int m_numLabeled;

  /** Whether to apply standardization or not. */
  protected boolean m_doNotStandardize;
  
  /**
   * Provides information regarding this class.
   *
   * @return string describing the method that this class implements
   */
  public String globalInfo() {
    return "Implements the XNV method for semi-supervised learning using a kernel function (default: RBFKernel). " +
            "Standardizes all attributes, including the target, by default. Applies (unsupervised) " +
            "NominalToBinary and ReplaceMissingValues before anything else is done.\n\n" +
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
          displayName = "Do not apply standardization",
          description = "If true, standardization will not be performed.",
          displayOrder = 4,
          commandLineParamName = "S",
          commandLineParamSynopsis = "-S")
  public boolean getDoNotStandardize() { return m_doNotStandardize; }
  public void setDoNotStandardize(boolean v) {m_doNotStandardize = v; }

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

    m_Missing = new ReplaceMissingValues();
    m_Missing.setInputFormat(data);
    data = Filter.useFilter(data, m_Missing);
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_NominalToBinary);

    // Shuffle the data
    data = new Instances(data);
    data.randomize(new Random(getSeed()));
    
    if (!getDoNotStandardize()) {

      // Retrieve two different class values
      int index0 = 0;
      while (index0 < data.numInstances() && data.instance(index0).classIsMissing()) {
        index0++;
      }
      if (index0 >= data.numInstances()) {
        throw new Exception("Need at least two instances with different target values.");
      }
      double y0 = data.instance(index0).classValue();
      int index1 = index0 + 1;
      while (index1 < data.numInstances() && (data.instance(index1).classIsMissing() ||
              data.instance(index1).classValue() == y0)) {
        index1++;
      }
      if (index1 >= data.numInstances()) {
        throw new Exception("Need at least two instances with different target values.");
      }
      double y1 = data.instance(index1).classValue();
      
      // Apply filter
      m_Standardize = new Standardize();
      m_Standardize.setIgnoreClass(true);
      m_Standardize.setInputFormat(data);
      data = Filter.useFilter(data, m_Standardize);
      
      // Establish coefficients enabling reversal of filter transformation for target
      double z0 = data.instance(index0).classValue();
      double z1 = data.instance(index1).classValue();
      m_x1 = (y0 - y1) / (z0 - z1);
      m_x0 = (y0 - m_x1 * z0);
    }

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

    // Remove eigenvalues that are not positive and corresponding eigenvectors; also take sqrt of eigenvalues
    ArrayList<Integer> toKeep = new ArrayList<>(e1.length);
    for (int i = 0; i < e1.length; i++) {
      if (Double.isNaN(e1[i])) {
        throw new IllegalStateException("XNV: Eigenvalue is NaN, aborting. Consider modifying parameters.");
      }
      if (e1[i] > 0) {
        toKeep.add(i);
      }
    }
    double[] e1New = new double[toKeep.size()];
    Matrix m_B1New = new DenseMatrix(m_B1.numRows(), e1New.length);
    int currentColumn = 0;
    for (int index : toKeep) {
      e1New[currentColumn] = Math.sqrt(e1[index]); // Take square root of eigenvalue
      for (int j = 0; j < m_B1.numRows(); j++) {
        m_B1New.set(j, currentColumn, m_B1.get(j, index));
      }
      currentColumn++;
    }
    e1 = e1New;
    m_B1 = m_B1New;

    // Reduce M accordingly
    M = toKeep.size();

    // Get labeled training data
    Instances labeledN1 = new Instances(N1data, N1data.numInstances());
    for (Instance inst : N1data) {
      if (!inst.classIsMissing()) {
        labeledN1.add(inst);
      }
    }
    m_numLabeled = labeledN1.numInstances();

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

    m_Missing.input(inst);
    m_Missing.batchFinished();
    inst = m_Missing.output();
    m_NominalToBinary.input(inst);
    m_NominalToBinary.batchFinished();
    inst = m_NominalToBinary.output();

    if (!getDoNotStandardize()) {
      m_Standardize.input(inst);
      inst = m_Standardize.output();
    }

    m_N1.input(inst);
    inst = m_N1.output();
    Matrix result = new DenseMatrix(1, inst.numAttributes() - 1);
    int index = 0;
    for (int i = 0; i < inst.numAttributes(); i++) {
      if (i != inst.classIndex()) {
        result.set(0, index++, inst.value(i));
      }
    }
    result = result.mult(m_B1, new DenseMatrix(1, m_B1.numColumns()));
    result = result.mult(m_wCCA, new DenseMatrix(1, 1));

    double[] pred = new double[1];
    if (getDoNotStandardize()) {
      pred[0] = result.get(0, 0);
    } else {
      pred[0] = result.get(0, 0) * m_x1 + m_x0;
    }

    return pred;
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

    m_Missing = new ReplaceMissingValues();
    m_Missing.setInputFormat(insts);
    insts = Filter.useFilter(insts, m_Missing);
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(insts);
    insts = Filter.useFilter(insts, m_NominalToBinary);

    if (!getDoNotStandardize()) {
      insts = Filter.useFilter(insts, m_Standardize);
    }
    Matrix result = getMatrix(Filter.useFilter(insts, m_N1), false, false);
    result = result.mult(m_B1, new DenseMatrix(result.numRows(), m_B1.numColumns()));
    result = result.mult(m_wCCA, new DenseMatrix(insts.numInstances(), 1));
    double[][] preds = new double[insts.numInstances()][1];
    for (int i = 0; i < insts.numInstances(); i++) {
      if (getDoNotStandardize()) {
        preds[i][0] = result.get(i, 0);
      } else {
        preds[i][0] = result.get(i, 0) * m_x1 + m_x0;
      }
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
      return "XNV weight vector (beta) based on " + m_numLabeled + " instances:\n\n" + m_wCCA;
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
    result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

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

