package weka.classifiers.functions;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Nystroem;
import weka.filters.unsupervised.instance.RemoveRange;

import java.util.Random;

public class XNV extends RandomizableClassifier {

  /** The two Nystroem filters to be used */
  Nystroem m_N1;
  Nystroem m_N2;

  /** The coefficients from CCA regression */
  Matrix m_wCCA;

  /** The CCA projection */
  Matrix m_B1;

  /** The sample size for each Nystroem filter */
  int m_M = 100;

  /** The kernel function to use. */
  protected Kernel m_Kernel = new RBFKernel();

  /** The regularization parameter. */
  double m_Gamma = 0.01;

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
  public static EVD CCA(Matrix X1, Matrix X2) throws Exception {

    System.err.println("X1\n" + X1);
    System.err.println("X2\n" + X2);

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
    CX1X2.scale(1.0 / (N - 1.0));
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

    //((RBFKernel)m_Kernel).setGamma(1);

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
    System.err.println(N1data);
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
    System.err.println(N2data);
    Matrix X2 = getMatrix(N2data, true, true);

    EVD evd = CCA(X1, X2);
    X1 = X2 = null;

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

    System.err.println("B1\n" + m_B1);
    for (int i = 0; i < e1.length; i++) {
      System.err.println(e1[i]);
    }

    // Get labeled training data
    Instances labeledN1 = new Instances(N1data, N1data.numInstances());
    for (Instance inst : N1data) {
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
    System.err.println(Z1);
    double sum = 0;
    for (int i = 0; i < Z.numRows(); i++) {
      sum += Z.get(i,0);
    }
    System.err.println("Sum: " + sum);
    System.err.println(Z);
    Matrix CCA_reg = new DenseMatrix(M, M);
    Matrix reg = new DenseMatrix(M, M);
    for (int i = 0; i < e1.length; i++) {
      CCA_reg.set(i, i, (1.0 - e1[i]) / e1[i]);
      reg.set(i, i, m_Gamma);
    }
    Matrix inv = inverse(Z.transAmult(Z, new DenseMatrix(M, M)).add(CCA_reg).add(reg));
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
   * Generates an XNV predictor.
   *
   * @param argv the options
   */
  public static void main(String argv[]) throws Exception {

    /* double[][] x1 = {{1.0, 2.0}, {3.0, 1.9}};
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
    }*/
    /*double[][] x1 = {{1.0, 2.0, 2}, {3.0, 1.9, 4}};
    double[][] x2 = {{1.1, 1.2, 1}, {2.1, 2.2, 3}};
    ArrayList<Attribute> atts = new ArrayList<>(2);
    for (int i = 0; i < x1[0].length - 1; i++) {
      atts.add(new Attribute("x" + (i + 1)));
    }
    atts.add(new Attribute("y"));
    Instances D = new Instances("D", atts, x1.length);
    for (int i = 0; i < x1.length; i++) {
      D.add(new DenseInstance(1.0, x1[i]));
    }
    for (int i = 0; i < x2.length; i++) {
      D.add(new DenseInstance(1.0, x2[i]));
    }
    D.setClassIndex(D.numAttributes() - 1);

    XNV xnv = new XNV();
    xnv.buildClassifier(D);
    double[][] preds = xnv.distributionsForInstances(D);
    for (int i = 0; i < preds.length; i++) {
      System.err.println(preds[i][0]);
    }*/
    runClassifier(new XNV(), argv);
  }
}
