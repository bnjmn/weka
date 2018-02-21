package weka.classifiers.functions;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Nystroem;
import weka.filters.unsupervised.instance.RemoveRange;

import java.util.ArrayList;
import java.util.Random;

public class XNV extends RandomizableClassifier {

  /** The two Nystroem filters to be used */
  Nystroem m_N1;
  Nystroem m_N2;

  /** The coefficients from CCA regression */
  Matrix m_wCCA;

  /** The sample size for each Nystroem filter */
  int m_M = 20;

  /** The kernel function to use. */
  protected RBFKernel m_Kernel = new RBFKernel();

  /** The regularization parameter. */
  double m_Gamma = 0.1;

  /**
   * Turns the given set of instances into a centered data matrix, with one instance per column.
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
   * Perform Canonical Correlation Analysis (CCA)
   *
   * @param X1 the first data matrix
   * @param X2 the second data matrix
   * @return the eigenvalue decomposition giving the basis in X1
   */
  public static SymmDenseEVD CCA(Matrix X1, Matrix X2) throws Exception {

    int M = X1.numRows();
    int N = X1.numColumns();
    UpperSPDDenseMatrix CX1X1 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X1);
    CX1X1.scale(1.0 / (N - 1.0));
    for (int i = 0; i < M; i++) {
      CX1X1.set(i, i, CX1X1.get(i, i) + 1e-8);
    }
    System.err.println("CX1X1\n" + CX1X1);

    UpperSPDDenseMatrix CX2X2 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X2);
    CX2X2.scale(1.0 / (N - 1.0));
    for (int i = 0; i < M; i++) {
      CX2X2.set(i, i, CX2X2.get(i, i) + 1e-8);
    }
    System.err.println("CX2X2\n" + CX2X2);

    // Compute covariance between views
    Matrix CX1X2 = X1.transBmult(X2, new DenseMatrix(M, M));
    Matrix CX2X1 = CX1X2.transpose(new DenseMatrix(M, M));
    System.err.println("CX1X2\n" + CX1X2);
    System.err.println("CX2X1\n" + CX2X1);

    // Perform eigenvalue decomposition
    Matrix CX1X1invMultCX1X2 = inverse(CX1X1).mult(CX1X2, new DenseMatrix(M, M));
    System.err.println("CX1X1invMultCX1X2\n" + CX1X1invMultCX1X2);
    Matrix CX2X2invMultCX2X1 = inverse(CX2X2).mult(CX2X1, new DenseMatrix(M, M));
    System.err.println("CX2X2invMultCX2X1\n" + CX2X2invMultCX2X1);
    Matrix CX1X1invMultCX1X2MultCX2X2invMultCX2X1 = CX1X1invMultCX1X2.mult(CX2X2invMultCX2X1, new DenseMatrix(M, M));
    System.err.println("CX1X1invMultCX1X2MultCX2X2invMultCX2X1\n" + CX1X1invMultCX1X2MultCX2X2invMultCX2X1);
    SymmDenseEVD evd = SymmDenseEVD.factorize(CX1X1invMultCX1X2MultCX2X2invMultCX2X1);

    return evd;
  }

  /**
   * Builds the XNV regressor.
   *
   * @param data set of instances serving as training data
   * @throws Exception
   */
  public void buildClassifier(Instances data) throws Exception {

    m_Kernel.setGamma(0.1); // Standardize data to start off with!

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
    Matrix X1 = getMatrix(Filter.useFilter(data, m_N1), true, true);

    // Build second Nystroem filter and generate second view, including covariance matrix
    m_N2 = new Nystroem();
    RemoveRange rr2 = new RemoveRange();
    rr2.setInvertSelection(true);
    rr2.setInstancesIndices((M + 1) + "-" + (2 * M));
    m_N2.setFilter(rr2);
    m_N2.setKernel((Kernel) new SerializedObject(m_Kernel).getObject());
    m_N2.setInputFormat(data);
    Matrix X2 = getMatrix(Filter.useFilter(data, m_N2), true, true);

    SymmDenseEVD evd = CCA(X1, X2);
    double[] e1 = evd.getEigenvalues();
    Matrix B1 = evd.getEigenvectors();
    double[] e1Reordered = new double[e1.length];
    Matrix B1Reordered = new DenseMatrix(B1.numRows(), B1.numColumns());
    for (int i = 0; i < e1.length; i++) { // We want descending rather than ascending for easier comparison
      e1Reordered[e1.length - (i + 1)] = e1[i];
      for (int j = 0; j < B1.numRows(); j++) {
        B1Reordered.set(j, i, B1.get(j, (e1.length - (i + 1))));
      }
    }
    e1 = e1Reordered;
    B1 = B1Reordered;

    // Get labeled training data
    Instances labeledOrig = new Instances(data, data.numInstances());
    for (Instance inst : data) {
      if (!inst.classIsMissing()) {
        labeledOrig.add(inst);
      }
    }

    // Get matrix with labels
    DenseMatrix labels = new DenseMatrix(labeledOrig.numInstances(), 1);
    for (int i = 0; i < labeledOrig.numInstances(); i++) {
      labels.set(i, 0, labeledOrig.instance(i).classValue());
    }

    System.err.println(B1);
    // Compute canonical ridge regression
    DenseMatrix Z1 = getMatrix(Filter.useFilter(labeledOrig, m_N2), false, false);
    Matrix Z = Z1.mult(B1, new DenseMatrix(labeledOrig.numInstances(), M));
    System.err.println(Z);
    UpperSPDDenseMatrix CCA_reg = new UpperSPDDenseMatrix(M);
    UpperSPDDenseMatrix reg = new UpperSPDDenseMatrix(M);
    for (int i = 0; i < e1.length; i++) {
      CCA_reg.set(i, i, (1.0 - e1[i]) / e1[i]);
      reg.set(i, i, m_Gamma);
    }
    Matrix inv = inverse(Z.transAmult(Z, new DenseMatrix(M, M)).add(CCA_reg.add(reg)));
    System.err.println("Inverse: \n" + inv);
    m_wCCA = inv.transBmult(Z, new DenseMatrix(Z.numColumns(), Z.numRows())).mult(labels, new DenseMatrix(M, 1));
    System.err.println(m_wCCA);
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
   * Returns predictions for a whole set of instances.
   *
   * @param insts the instances to make predictions for
   * @return the 2D array with results
   */
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    DenseMatrix data = getMatrix(Filter.useFilter(insts, m_N1), false, false);
    Matrix result = data.mult(m_wCCA, new DenseMatrix(insts.numInstances(), 1));
    double[][] preds = new double[insts.numInstances()][1];
    for (int i = 0; i < insts.numInstances(); i++) {
      preds[i][0] = result.get(i, 0);
    }

    return preds;
  }


  /**
   * Generates an XNV predictor.
   *
   * @param argv the options
   */
  public static void main(String argv[]) throws Exception {

    double[][] x1 = {{1.0, 2.0}, {3.0, 1.9}};
    double[][] x2 = {{1.1, 1.2}, {2.1, 2.2}};
    ArrayList<Attribute> atts = new ArrayList<>(2);
    for (int i = 0; i < x1[0].length; i++) {
      atts.add(new Attribute("x" + (i + 1)));
    }
    Instances X1 = new Instances("X1", atts, x1.length);
    for (int i = 0; i < x1.length; i++) {
      X1.add(new DenseInstance(1.0, x1[i]));
    }
    System.err.println(X1);
    Instances X2 = new Instances("X2", atts, x2.length);
    for (int i = 0; i < x2.length; i++) {
      X2.add(new DenseInstance(1.0, x2[i]));
    }
    System.err.println(X2);
    SymmDenseEVD evd = XNV.CCA(XNV.getMatrix(X1, true, true), XNV.getMatrix(X2, true, true));
    Matrix B1 = evd.getEigenvectors();
    double[] e1 = evd.getEigenvalues();
    double[] e1Reordered = new double[e1.length];
    Matrix B1Reordered = new DenseMatrix(B1.numRows(), B1.numColumns());
    for (int i = 0; i < e1.length; i++) { // We want descending rather than ascending for easier comparison
      e1Reordered[e1.length - (i + 1)] = e1[i];
      for (int j = 0; j < B1.numRows(); j++) {
        B1Reordered.set(j, i, B1.get(j, (e1.length - (i + 1))));
      }
    }
    e1 = e1Reordered;
    B1 = B1Reordered;
    System.err.println(B1);
    for (int i = 0; i < e1.length; i++) {
      System.err.println(e1[i]);
    }
    runClassifier(new XNV(), argv);
  }
}
