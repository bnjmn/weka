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
 *    MultiClassFLDA.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.attribute;

import java.util.ArrayList;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;
import weka.core.*;
import weka.filters.SimpleBatchFilter;

/**
 <!-- globalinfo-start -->
 * Implements multi-class LDA for dimensionality reduction.
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 *
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 12341 $
 */
public class MultiClassFLDA extends SimpleBatchFilter {

  /**
   * for serialization
   */
  static final long serialVersionUID = -251536442147283133L;

  /**
   * Constant to avoid division by zero.
   */
  public static double SMALL = 1e-6;

  /**
   * Stores the weighting matrix.
   */
  protected Matrix m_WeightingMatrix;

  /**
   * Returns the Capabilities of this filter.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = new Capabilities(this);
    result.disableAll();

    result.setMinimumNumberInstances(0);

    // attributes
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Provides information regarding this class.
   *
   * @return string describing the method that this class implements
   */
  @Override
  public String globalInfo() {
    return "Implements multi-class LDA for dimensionality reduction.";
  }

  /**
   * Returns whether to allow the determineOutputFormat(Instances) method access
   * to the full dataset rather than just the header.
   * <p/>
   * Default implementation returns false.
   *
   * @return whether determineOutputFormat has access to the full input dataset
   */
  public boolean allowAccessToFullInputFormat() {
    return true;
  }

  /**
   * Computes the mean vector for the given dataset.
   */
  protected Vector computeMean(Instances data, double[] totalWeight, int aI) {

    Vector meanVector = new DenseVector(data.numAttributes() - 1);
    totalWeight[aI] = 0;
    for (Instance inst : data) {
      if (!inst.classIsMissing()) {
        int index = 0;
        for (int j = 0; j < inst.numAttributes(); j++) {
          if (j != inst.classIndex()) {
            meanVector.add(index, inst.weight() * inst.value(index));
            index++;
          }
        }
        totalWeight[aI] += inst.weight();
      }
    }
    meanVector.scale(1.0 / totalWeight[aI]);
    return meanVector;
  }

  /**
   * Computes the covariance matrix for the given dataset.
   */
  protected Matrix computeCovarianceMatrix(Instances data, Vector mean) {

    double[] totalWeight = new double[1];
    Vector meanVector = computeMean(data, totalWeight, 0);
    for (int i = 0; i < meanVector.size(); i++) {
      mean.set(i, meanVector.get(i));
    }
    Matrix C = new UpperSymmDenseMatrix(meanVector.size());
    for (Instance inst : data) {
      if (!inst.classIsMissing()) {
        int i1 = 0;
        for (int i = 0; i < inst.numAttributes(); i++) {
          if (i != inst.classIndex()) {
            int i2 = 0;
            for (int j = 0; j < inst.numAttributes(); j++) {
              if (j != inst.classIndex()) {
                C.add(i1, i2, inst.weight() *
                        (inst.value(i) - meanVector.get(i1)) * (inst.value(j) - meanVector.get(i2)));
                i2++;
              }
            }
            i1++;
          }
        }
      }
    }
    return C;
  }

  /**
     * Determines the output format for the data that is produced by this filter.
     *
     * @param inputFormat the input format to base the output format on
     * @return the output format
     * @throws Exception if a problem occurs when the output format is generated
     */
    @Override
    protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

      // Determine number of attributes
      int m = inputFormat.numAttributes() - 1;

      // Compute global covariance matrix
      Vector globalMean = new DenseVector(m);
      Matrix Cw = computeCovarianceMatrix(inputFormat, globalMean);

      // Compute subset for each class
      Instances[] subsets = new Instances[inputFormat.numClasses()];
      for (int j = 0; j < subsets.length; j++) {
        subsets[j] = new Instances(inputFormat, inputFormat.numInstances());
      }
      for (Instance inst : inputFormat) {
        if (!inst.classIsMissing()) {
          subsets[(int) inst.classValue()].add(inst);
        }
      }

      // Compute mean vector and weight for each class
      Vector[] perClassMeans = new DenseVector[inputFormat.numClasses()];
      double[] perClassWeights = new double[inputFormat.numClasses()];
      for (int i = 0; i < inputFormat.numClasses(); i++) {
        perClassMeans[i] = computeMean(subsets[i], perClassWeights, i);
      }

      // Compute between-class scatter matrix
      Matrix Cb = new UpperSymmDenseMatrix(m);
      for (int i = 0; i < inputFormat.numClasses(); i++) {
        Vector diff = perClassMeans[i].copy();
        diff = diff.add(-1.0, globalMean);
        Cb = Cb.rank1(perClassWeights[i], diff);
      }

      // Compute inverse of within-class scatter matrix
      Matrix I = new UpperSymmDenseMatrix(m);
      for (int i = 0; i < I.numRows(); i++) {
        I.set(i, i, 1.0);
      }
      Matrix CwInverse = Cw.solve(I, new DenseMatrix(I));

      // Compute square root of between-class scatter matrix
      SymmDenseEVD evdCb = SymmDenseEVD.factorize(Cb);
      Matrix evCb = evdCb.getEigenvectors();
      double[] evs = evdCb.getEigenvalues();
      Matrix D = new UpperSymmDenseMatrix(evs.length);
      for (int i = 0; i < evs.length; i++) {
        D.set(i, i, Math.sqrt(evs[i]));
      }
      Matrix temp = evCb.mult(D, new UpperSymmDenseMatrix(evs.length));
      Matrix sqrtCb = temp.mult(evCb.transpose(), new UpperSymmDenseMatrix(evs.length));

      // Compute symmetric matrix using square root
      Matrix symmMatrix = sqrtCb.mult(CwInverse, new DenseMatrix(evs.length, evs.length)).
              mult(sqrtCb, new UpperSymmDenseMatrix(evs.length));

      // Perform eigendecomposition on symmetric matrix
      SymmDenseEVD evd = SymmDenseEVD.factorize(symmMatrix);

      if (m_Debug) {
        System.err.println("Eigenvector matrix :\n" + evd.getEigenvectors());
        System.err.println("Eigenvalues \n" + Utils.arrayToString(evd.getEigenvalues()));
      }

      // Only keep non-zero eigenvectors
      ArrayList<Integer> indices = new ArrayList<Integer>();
      for (int i = 0; i < evs.length; i++) {
        if (Utils.gr(evs[i], 0)) {
          indices.add(i);
        }
      }
      int[] cols = new int[indices.size()];
      for (int i = 0; i < indices.size(); i++) {
        cols[i] = indices.get(i);
      }
      int[] rows = new int[evd.getEigenvectors().numRows()];
      for (int i = 0; i < rows.length; i++) {
        rows[i] = i;
      }
      Matrix reducedMatrix = Matrices.getSubMatrix(evd.getEigenvectors(), rows, cols);

      // Compute weighting Matrix
      m_WeightingMatrix = sqrtCb.solve(reducedMatrix, new DenseMatrix(rows.length, cols.length)).
              transpose(new DenseMatrix(cols.length, rows.length));

      if (m_Debug) {
        System.err.println("Weighting matrix: \n" + m_WeightingMatrix);
      }

      // Construct header for output format
      ArrayList<Attribute> atts = new ArrayList<Attribute>(cols.length + 1);
      for (int i = 0; i < cols.length; i++) {
        atts.add(new Attribute("z" + (i + 1)));
      }
      atts.add((Attribute) inputFormat.classAttribute().copy());
      Instances d = new Instances(inputFormat.relationName(), atts, 0);
      d.setClassIndex(d.numAttributes() - 1);
      return d;
    }

    /**
     * Takes a batch of data and transforms it.
     *
     * @param instances the data to process
     * @return the processed instances
     * @throws Exception is thrown if a problem occurs
     */
    @Override
    protected Instances process(Instances instances) throws Exception {

      Instances transformed = getOutputFormat();
      for (Instance inst : instances) {
        Vector n = new DenseVector(inst.numAttributes() - 1);
        int index = 0;
        for (int i = 0; i < inst.numAttributes(); i++) {
          if (i != inst.classIndex()) {
            n.set(index++, inst.value(i));
          }
        }
        Vector newInst = m_WeightingMatrix.mult(n, new DenseVector(m_WeightingMatrix.numRows()));
        double[] newVals = new double[m_WeightingMatrix.numRows() + 1];
        for (int i = 0; i < m_WeightingMatrix.numRows(); i++) {
          newVals[i] = newInst.get(i);
        }
        newVals[transformed.classIndex()] = inst.classValue();
        transformed.add(new DenseInstance(inst.weight(), newVals));
      }
      return transformed;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 12037 $");
    }

    /**
     * Main method for testing this class.
     *
     * @param argv should contain arguments to the filter: use -h for help
     */
    public static void main(String[] argv) {
        runFilter(new MultiClassFLDA(), argv);
    }
}

