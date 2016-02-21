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
 *    Nystroem.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.*;
import weka.core.matrix.EigenvalueDecomposition;
import weka.core.matrix.Matrix;
import weka.core.matrix.SingularValueDecomposition;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.unsupervised.instance.Resample;

import java.util.ArrayList;

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
public class Nystroem extends SimpleBatchFilter {

    /** Constant to avoid division by zero. */
    public static double SMALL = 1e-6;

    /** The filter to use for sub sampling. */
    protected Filter m_Filter;

    /** The kernel function to use. */
    protected Kernel m_Kernel;

    /** Determines whether singular value decomposition is used instead of eigendecomposition. */
    protected boolean m_useSVD;

    /** Stores the sample used for the approximation. */
    protected Instances m_Sample;

    /** Stores the weighting matrix. */
    protected Matrix m_WeightingMatrix;

    /**
     * Gets whether to use singular value decomposition instead of eigendecomposition.
     *
     * @return true if SVD is to be used
     */
    public boolean getUseSVD() { return m_useSVD; }

    /**
     * Sets whether to use singular value decomposition instead of eigendecomposition.
     * @param flag true if singular value decomposition is to be used.
     */
    public void setUseSVD(boolean flag) {m_useSVD = flag; }

    /**
     * Gets the filter that is used for sub sampling.
     *
     * @return the filter
     */
    public Filter getFilter() { return m_Filter; }

    /**
     * Sets the filter to use for sub sampling.
     *
     * @param filter the filter to use
     */
    public void setFilter(Filter filter) { this.m_Filter = filter; }

    /**
     * sets the kernel to use
     *
     * @param value	the kernel to use
     */
    public void setKernel(Kernel value) {
        m_Kernel = value;
    }

    /**
     * Returns the kernel to use
     *
     * @return 		the current kernel
     */
    public Kernel getKernel() {
        return m_Kernel;
    }

    public Nystroem() {
        m_Filter = new Resample();
        ((Resample)m_Filter).setNoReplacement(true);
        ((Resample)m_Filter).setSampleSizePercent(100);
        m_Kernel = new PolyKernel();
    }

    /**
     * Provides information regarding this class.
     *
     * @return string describing the method that this class implements
     */
    @Override
    public String globalInfo() {
        return "Implements the the Nystroem method for feature extraction using a kernel function.";
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
     * Determines the output format for the data that is produced by this filter.
     *
     * @param inputFormat the input format to base the output format on
     * @return the output format
     * @throws Exception if a problem occurs when the output format is generated
     */
    @Override
    protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

        // Sample subset of instances
        Filter filter = Filter.makeCopy(getFilter());
        filter.setInputFormat(inputFormat);
        m_Sample = Filter.useFilter(inputFormat, filter);

        // Compute kernel-based matrices for subset
        m_Kernel = Kernel.makeCopy(m_Kernel);
        m_Kernel.buildKernel(m_Sample);
        int m = m_Sample.numInstances();
        int n = inputFormat.numInstances();
        Matrix khatM = new Matrix(m, m);
        for (int i = 0; i < m; i++) {
            for (int j = i; j < m; j++) {
                khatM.set(i, j, m_Kernel.eval(i, j, m_Sample.instance(i)));
                khatM.set(j, i, khatM.get(i, j));
            }
        }

        if (m_Debug) {
            Matrix kbM = new Matrix(n, m);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    kbM.set(i, j, m_Kernel.eval(-1, j, inputFormat.instance(i)));
                }
            }

            // Calculate SVD of kernel matrix
            SingularValueDecomposition svd = new SingularValueDecomposition(khatM);

            double[] singularValues = svd.getSingularValues();
            Matrix sigmaI = new Matrix(m, m);
            for (int i = 0; i < singularValues.length; i++) {
                if (singularValues[i] > SMALL) {
                    sigmaI.set(i, i, 1.0 / singularValues[i]);
                }
            }

            System.err.println("U :\n" + svd.getU());
            System.err.println("V :\n" + svd.getV());
            System.err.println("Reciprocal of singular values :\n" + sigmaI);

            Matrix pseudoInverse = svd.getV().times(sigmaI).times(svd.getU().transpose());

            // Compute reduced-rank version
            Matrix khatr = kbM.times(pseudoInverse).times(kbM.transpose());

            System.err.println("Reduced rank matrix: \n" + khatr);
        }

        // Compute weighting matrix
        if (getUseSVD()) {
            SingularValueDecomposition svd = new SingularValueDecomposition(khatM);
            double[] e = svd.getSingularValues();
            Matrix dhatr = new Matrix(e.length, e.length);
            for (int i = 0; i < e.length; i++) {
                if (Math.sqrt(e[i]) > SMALL) {
                    dhatr.set(i, i, 1.0 / Math.sqrt(e[i]));
                }
            }
            if (m_Debug) {
                System.err.println("U matrix :\n" + svd.getU());
                System.err.println("V matrix :\n" + svd.getV());
                System.err.println("Singluar value matrix \n" + svd.getS());
                System.err.println("Reciprocal of square root of singular values :\n" + dhatr);
            }
            m_WeightingMatrix = dhatr.times(svd.getV().transpose());
        } else {
            EigenvalueDecomposition evd = new EigenvalueDecomposition(khatM);
            double[] e = evd.getRealEigenvalues();
            Matrix dhatr = new Matrix(e.length, e.length);
            for (int i = 0; i < e.length; i++) {
                if (Math.sqrt(e[i]) > SMALL) {
                    dhatr.set(i, i, 1.0 / Math.sqrt(e[i]));
                }
            }
            if (m_Debug) {
                System.err.println("Eigenvector matrix :\n" + evd.getV());
                System.err.println("Eigenvalue matrix \n" + evd.getD());
                System.err.println("Reciprocal of square root of eigenvalues :\n" + dhatr);
            }
            m_WeightingMatrix = dhatr.times(evd.getV().transpose());
        }

        if (m_Debug) {
            System.err.println("Weighting matrix: \n" + m_WeightingMatrix);
        }

        // Construct header for output format
        boolean hasClass = (inputFormat.classIndex() >= 0);
        ArrayList<Attribute> atts = new ArrayList<Attribute>(m + ((hasClass) ? 1 : 0));
        for (int i = 0; i < m; i++) {
            atts.add(new Attribute("z" + (i + 1)));
        }
        if (hasClass) {
            atts.add((Attribute) inputFormat.classAttribute().copy());
        }
        Instances d = new Instances("", atts, 0);
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
        boolean hasClass = (instances.classIndex() >= 0);
        int m = m_Sample.numInstances();
        for (Instance inst : instances) {
            Matrix n = new Matrix(transformed.numAttributes() - ((hasClass) ? 1 : 0), 1);
            for (int i = 0; i < m; i++) {
                n.set(i, 0, m_Kernel.eval(-1, i, inst));
            }
            Matrix newInst = m_WeightingMatrix.times(n);
            double[] newVals = new double[m + ((hasClass) ? 1 : 0)];
            for (int i = 0; i < m; i++) {
                newVals[i] = newInst.get(i, 0);
            }
            if (hasClass) {
                newVals[transformed.classIndex()] = inst.classValue();
            }
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
        runFilter(new Nystroem(), argv);
    }
}
