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
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.unsupervised.instance.Resample;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;

import java.util.ArrayList;

/**
 <!-- globalinfo-start -->
 * Implements the Nystroem method for feature extraction using a kernel function.<br>
 * <br>
 * For more information on the algorithm, see<br>
 * <br>
 * Tianbao Yang, Yu-Feng Li, Mehrdad Mahdavi, Rong Jin, Zhi-Hua Zhou: Nystr"{o}m Method vs Random Fourier Features: A Theoretical and Empirical Comparison. In: Proc 26th Annual Conference on Neural Information Processing Systems, 485--493, 2012.
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Yang2012,
 *    author = {Tianbao Yang and Yu-Feng Li and Mehrdad Mahdavi and Rong Jin and Zhi-Hua Zhou},
 *    booktitle = {Proc 26th Annual Conference on Neural Information Processing Systems},
 *    pages = {485--493},
 *    title = {Nystr"{o}m Method vs Random Fourier Features: A Theoretical and Empirical Comparison},
 *    year = {2012},
 *    URL = {http://papers.nips.cc/paper/4588-nystrom-method-vs-random-fourier-features-a-theoretical-and-empirical-comparison}
 * }
 * </pre>
 * <br><br>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -K &lt;kernel specification&gt;
 *  The kernel function to use.</pre>
 * 
 * <pre> -F &lt;filter specification&gt;
 *  The filter to use, which should be a filter that takes a sample of instances.</pre>
 * 
 * <pre> -use-svd
 *  Whether to use singular value decomposition instead of eigendecomposition.</pre>
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
public class Nystroem extends SimpleBatchFilter implements TechnicalInformationHandler {

    /** for serialization */
    static final long serialVersionUID = -251931442147263433L;

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
     * Returns the Capabilities of this filter.
     *
     * @return the capabilities of this object
     * @see Capabilities
     */
    @Override
    public Capabilities getCapabilities() {

        Capabilities result = getKernel().getCapabilities();
        result.setOwner(this);

        result.setMinimumNumberInstances(0);

        result.enableAllClasses();
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
        result.enable(Capabilities.Capability.NO_CLASS);

        return result;
    }

    /**
     * Gets whether to use singular value decomposition instead of eigendecomposition.
     *
     * @return true if SVD is to be used
     */
    @OptionMetadata(
            displayName = "Use SVD and not eigendecomposition",
            description = "Whether to use singular value decomposition instead of eigendecomposition.",
            displayOrder = 3,
            commandLineParamName = "use-svd",
            commandLineParamSynopsis = "-use-svd",
            commandLineParamIsFlag = true)
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
    @OptionMetadata(
            displayName = "Filter for sampling instances",
            description = "The filter to use, which should be a filter that takes a sample of instances.",
            displayOrder = 2,
            commandLineParamName = "F",
            commandLineParamSynopsis = "-F <filter specification>")
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
    @OptionMetadata(
            displayName = "Kernel function",
            description = "The kernel function to use.", displayOrder = 1,
            commandLineParamName = "K",
            commandLineParamSynopsis = "-K <kernel specification>")
    public Kernel getKernel() {
        return m_Kernel;
    }

    /**
     * Default constructor. Sets filter to Resample filter without replacement and sample size percentage to 10%.
     * The kernel is set to PolyKernel with default options.
     */
    public Nystroem() {
        m_Filter = new Resample();
        ((Resample)m_Filter).setNoReplacement(true);
        ((Resample)m_Filter).setSampleSizePercent(10);
        m_Kernel = new PolyKernel();
    }

    /**
     * Provides information regarding this class.
     *
     * @return string describing the method that this class implements
     */
    @Override
    public String globalInfo() {
        return "Implements the Nystroem method for feature extraction using a kernel function.\n\n" +
                "For more information on the algorithm, see\n\n" + getTechnicalInformation().toString();
    }

    /**
     * Returns a reference to the algorithm implemented by this class.
     *
     * @return a reference to the algorithm implemented by this class
     */
    @Override
    public TechnicalInformation getTechnicalInformation() {

        TechnicalInformation result = new TechnicalInformation(TechnicalInformation.Type.INPROCEEDINGS);
        result.setValue(TechnicalInformation.Field.AUTHOR, "Tianbao Yang and Yu-Feng Li and Mehrdad Mahdavi and Rong Jin and Zhi-Hua Zhou");
        result.setValue(TechnicalInformation.Field.TITLE, "Nystr\"{o}m Method vs Random Fourier Features: A Theoretical and Empirical Comparison");
        result.setValue(TechnicalInformation.Field.BOOKTITLE, "Proc 26th Annual Conference on Neural Information Processing Systems");
        result.setValue(TechnicalInformation.Field.PAGES, "485--493");
        result.setValue(TechnicalInformation.Field.YEAR, "2012");
        result.setValue(TechnicalInformation.Field.URL, "http://papers.nips.cc/paper/4588-nystrom-method-vs-random-fourier-features-a-theoretical-and-empirical-comparison");

        return result;
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
        Matrix khatM = new UpperSymmDenseMatrix(m);
        for (int i = 0; i < m; i++) {
            for (int j = i; j < m; j++) {
                khatM.set(i, j, m_Kernel.eval(i, j, m_Sample.instance(i)));
            }
        }
        m_Kernel.clean();

        if (m_Debug) {
            Matrix kbM = new DenseMatrix(n, m);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    kbM.set(i, j, m_Kernel.eval(-1, j, inputFormat.instance(i)));
                }
            }

            // Calculate SVD of kernel matrix
            SVD svd = SVD.factorize(khatM);

            double[] singularValues = svd.getS();
            Matrix sigmaI = new UpperSymmDenseMatrix(m);
            for (int i = 0; i < singularValues.length; i++) {
                if (singularValues[i] > SMALL) {
                    sigmaI.set(i, i, 1.0 / singularValues[i]);
                }
            }

            System.err.println("U :\n" + svd.getU());
            System.err.println("Vt :\n" + svd.getVt());
            System.err.println("Reciprocal of singular values :\n" + sigmaI);

            Matrix pseudoInverse = svd.getU().mult(sigmaI, new DenseMatrix(m,m)).mult(svd.getVt(), new DenseMatrix(m,m));

            // Compute reduced-rank version
            Matrix khatr = kbM.mult(pseudoInverse, new DenseMatrix(n, m)).mult(kbM.transpose(new DenseMatrix(m, n)), new DenseMatrix(n,n));

            System.err.println("Reduced rank matrix: \n" + khatr);
        }

        // Compute weighting matrix
        if (getUseSVD()) {
            SVD svd = SVD.factorize(khatM);
            double[] e = svd.getS();
            Matrix dhatr = new UpperSymmDenseMatrix(e.length);
            for (int i = 0; i < e.length; i++) {
                if (Math.sqrt(e[i]) > SMALL) {
                    dhatr.set(i, i, 1.0 / Math.sqrt(e[i]));
                }
            }
            if (m_Debug) {
                System.err.println("U matrix :\n" + svd.getU());
                System.err.println("Vt matrix :\n" + svd.getVt());
                System.err.println("Singluar values \n" + Utils.arrayToString(svd.getS()));
                System.err.println("Reciprocal of square root of singular values :\n" + dhatr);
            }
            m_WeightingMatrix = dhatr.mult(svd.getVt(), new DenseMatrix(m,m));
        } else {

            SymmDenseEVD evd = SymmDenseEVD.factorize(khatM);
            double[] e = evd.getEigenvalues();
            Matrix dhatr = new UpperSymmDenseMatrix(e.length);
            for (int i = 0; i < e.length; i++) {
                if (Math.sqrt(e[i]) > SMALL) {
                    dhatr.set(i, i, 1.0 / Math.sqrt(e[i]));
                }
            }
            if (m_Debug) {
                System.err.println("Eigenvector matrix :\n" + evd.getEigenvectors());
                System.err.println("Eigenvalues \n" + Utils.arrayToString(evd.getEigenvalues()));
                System.err.println("Reciprocal of square root of eigenvalues :\n" + dhatr);
            }
            m_WeightingMatrix = dhatr.mult(evd.getEigenvectors().transpose(), new DenseMatrix(m,m));
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
        Instances d = new Instances(inputFormat.relationName(), atts, 0);
        if (hasClass) {
          d.setClassIndex(d.numAttributes() - 1);
        }
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
            Vector n = new DenseVector(m);
            for (int i = 0; i < m; i++) {
                n.set(i, m_Kernel.eval(-1, i, inst));
            }
            Vector newInst = m_WeightingMatrix.mult(n, new DenseVector(m));
            double[] newVals = new double[m + ((hasClass) ? 1 : 0)];
            for (int i = 0; i < m; i++) {
                newVals[i] = newInst.get(i);
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

