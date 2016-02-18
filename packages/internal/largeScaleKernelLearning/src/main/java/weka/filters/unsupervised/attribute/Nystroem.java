package weka.filters.unsupervised.attribute;

import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.*;
import weka.core.matrix.Matrix;
import weka.core.matrix.SingularValueDecomposition;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.unsupervised.instance.Resample;

import java.util.ArrayList;

/**
 * Created by eibe on 18/02/16.
 */
public class Nystroem extends SimpleBatchFilter {

    protected Filter m_Filter;

    protected Kernel m_Kernel;

    protected Instances m_Sample;

    protected Matrix m_WeightingMatrix;

    public Filter getFilter() { return m_Filter; }

    public void setFilter(Filter m_Filter) { this.m_Filter = m_Filter; }

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
        double[][] khat = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i; j < m; j++) {
                khat[i][j] = m_Kernel.eval(i, j, m_Sample.instance(i));
                khat[j][i] = khat[i][j];
            }
        }
        Matrix khatM = new Matrix(khat);
        /*double[][] kb = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = i; j < n; j++) {
                kb[i][j] = m_Kernel.eval(-1, i, inputFormat.instance(i));
            }
        }
        Matrix kbM = new Matrix(kb).transpose();*/

        // Calculate SVD of kernel matrix
        SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(khat));

        double[] singularValues = svd.getSingularValues();
        Matrix sigmaI = new Matrix(m,m);
        for (int i = 0; i < singularValues.length; i++) {
            sigmaI.set(i, i, 1.0 / singularValues[i]);
        }

        m_WeightingMatrix = sigmaI.times(svd.getV().transpose());


        /* Matrix pseudoInverse = svd.getV().transpose().times(sigmaI).times(svd.getU().transpose());

        // Compute reduced-rank version
        Matrix khatr = kbM.times(pseudoInverse).times(kbM.transpose());

        // Get eigenvalues and eigenvectors of reduced-rank matrix
        EigenvalueDecomposition evd = new EigenvalueDecomposition(khatr);
        double[] e = evd.getRealEigenvalues();
        Matrix dhatr = new Matrix(e.length, e.length);
        for (int i  = 0; i < e.length; i++) {
            dhatr.set(i, i, 1./Math.sqrt(e[i]));
        }
        m_WeightingMatrix = dhatr.times(evd.getV()); */

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

    @Override
    protected Instances process(Instances instances) throws Exception {

        Instances transformed = getOutputFormat();
        boolean hasClass = (instances.classIndex() >= 0);
        int m = m_Sample.numInstances();
        for (Instance inst : instances) {
            double[][] n = new double[1][transformed.numAttributes() - ((hasClass) ? 1 : 0)];
            for (int i = 0; i < m; i++) {
                n[0][i] = m_Kernel.eval(-1, i, inst);
            }
            Matrix newInst = m_WeightingMatrix.times(new Matrix(n).transpose());
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
}
