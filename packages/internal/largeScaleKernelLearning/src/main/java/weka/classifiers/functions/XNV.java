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

import java.util.Random;

public class XNV extends RandomizableClassifier {

  /** The two Nystroem filters to be used */
  Nystroem m_N1;
  Nystroem m_N2;

  /** The sample size for each Nystroem filter */
  int m_M = 5;

  /** The kernel function to use. */
  protected RBFKernel m_Kernel = new RBFKernel();

  /**
   * Turns the given set of instances into a centered data matrix, with one instance per column.
   *
   * @param data set of instances
   * @param center whether to center the matrix
   * @param transpose whether to transpose the matrix
   * @return the matrix
   */
  protected DenseMatrix getMatrix(Instances data, boolean center, boolean transpose) {

    double[] means = new double[data.numAttributes()];
    if (center) {
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          means[j] = data.meanOrMode(j);
        }
      }
    }
    int numColumns = transpose ? data.numInstances() : data.numAttributes() - 1;
    int numRows = transpose ? data.numAttributes() - 1 : data.numInstances();
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
   * Returns the inverse of the given Matrix.
   *
   * @param M the matrix to invert
   * @return the inverse
   */
  public UpperSPDDenseMatrix inverse(UpperSPDDenseMatrix M) {

    DenseMatrix Minv = new DenseMatrix(M.numRows(), M.numRows());
    UpperSPDDenseMatrix I = new UpperSPDDenseMatrix(Matrices.identity(M.numRows()));
    M.solve(I, Minv);
    return new UpperSPDDenseMatrix(Minv);
  }

  /**
   * Builds the XNV regressor.
   *
   * @param data set of instances serving as training data
   * @throws Exception
   */
  public void buildClassifier(Instances data) throws Exception {

    m_Kernel.setGamma(1); // Standardize data to start off with!

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
    UpperSPDDenseMatrix CX1X1 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X1);
    CX1X1.scale(1.0 / (data.numInstances() - 1));
    for (int i = 0; i < M; i++) {
      CX1X1.set(i, i, CX1X1.get(i, i) + 1e-8);
    }
    System.err.println(CX1X1);

    // Build second Nystroem filter and generate second view, including covariance matrix
    m_N2 = new Nystroem();
    RemoveRange rr2 = new RemoveRange();
    rr2.setInvertSelection(true);
    rr2.setInstancesIndices((M + 1) + "-" + (2 * M));
    m_N2.setFilter(rr2);
    m_N2.setKernel((Kernel) new SerializedObject(m_Kernel).getObject());
    m_N2.setInputFormat(data);
    Matrix X2 = getMatrix(Filter.useFilter(data, m_N2), true, true);
    UpperSPDDenseMatrix CX2X2 = (UpperSPDDenseMatrix) new UpperSPDDenseMatrix(M).rank1(X2);
    CX2X2.scale(1.0 / (data.numInstances() - 1));
    for (int i = 0; i < M; i++) {
      CX2X2.set(i, i, CX2X2.get(i, i) + 1e-8);
    }
    System.err.println(CX2X2);

    // Compute covariance between views
    Matrix CX1X2 = X1.mult(X2.transpose(new DenseMatrix(X2.numColumns(), M)), new UpperSymmDenseMatrix(M)); // Maybe PD matrix?

    // Perform eigenvalue decomposition
    UpperSPDDenseMatrix CX1X1invMultCX1X2 = (UpperSPDDenseMatrix)inverse(CX1X1).mult(CX1X2, new UpperSPDDenseMatrix(M));
    UpperSPDDenseMatrix CX2X2invMultCX1X2 = (UpperSPDDenseMatrix)inverse(CX2X2).mult(CX1X2, new UpperSPDDenseMatrix(M));
    UpperSPDDenseMatrix CX1X1invMultCX1X2MultCX2X2invMultCX1X2 =
            (UpperSPDDenseMatrix) CX1X1invMultCX1X2.mult(CX2X2invMultCX1X2, new UpperSPDDenseMatrix(M));
    SymmDenseEVD evd = SymmDenseEVD.factorize(CX1X1invMultCX1X2MultCX2X2invMultCX1X2);
    double[] e1 = evd.getEigenvalues();
    Matrix B1 = evd.getEigenvectors();

    // Get labeled training data
    Instances labeledOrig = new Instances(data, data.numInstances());
    for (Instance inst : data) {
      if (!inst.classIsMissing()) {
        labeledOrig.add(inst);
      }
    }

    System.err.println(B1);
    // Compute canonical ridge regression
    DenseMatrix Z1 = getMatrix(Filter.useFilter(labeledOrig, m_N2), false, false);
    Matrix Z = Z1.mult(B1, new DenseMatrix(labeledOrig.numInstances(), M));
  }



  public double[] distributionForInstance(Instance inst) throws Exception {

    double[] probs = new double[inst.numClasses()];


    return probs;
  }


  /**
   * Generates an XNV predictor.
   *
   * @param argv the options
   */
  public static void main(String argv[]) {
    runClassifier(new XNV(), argv);
  }
}
