/*
 * Copyright (c) 2007-2013 University of Waikato.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither name of copyright holders nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package weka.classifiers.functions;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WekaException;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;


/**
  <!-- globalinfo-start -->
  * A wrapper class for the liblinear classifier.<br/>
  * Rong-En Fan, Kai-Wei Chang, Cho-Jui Hsieh, Xiang-Rui Wang, Chih-Jen Lin (2008). LIBLINEAR - A Library for Large Linear Classification. URL http://www.csie.ntu.edu.tw/~cjlin/liblinear/.
  * <p/>
  <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;misc{Fan2008,
 *    author = {Rong-En Fan and Kai-Wei Chang and Cho-Jui Hsieh and Xiang-Rui Wang and Chih-Jen Lin},
 *    note = {The Weka classifier works with version 1.33 of LIBLINEAR},
 *    title = {LIBLINEAR - A Library for Large Linear Classification},
 *    year = {2008},
 *    URL = {http://www.csie.ntu.edu.tw/\~cjlin/liblinear/}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -S &lt;int&gt;
 *  Set type of solver (default: 1)
 *   for multi-class classification
 *     0 -- L2-regularized logistic regression (primal)
 *     1 -- L2-regularized L2-loss support vector classification (dual)
 *     2 -- L2-regularized L2-loss support vector classification (primal)
 *     3 -- L2-regularized L1-loss support vector classification (dual)
 *     4 -- support vector classification by Crammer and Singer
 *     5 -- L1-regularized L2-loss support vector classification
 *     6 -- L1-regularized logistic regression
 *     7 -- L2-regularized logistic regression (dual)
 *  for regression
 *    11 -- L2-regularized L2-loss support vector regression (primal)
 *    12 -- L2-regularized L2-loss support vector regression (dual)
 *    13 -- L2-regularized L1-loss support vector regression (dual)</pre>
 *
 * <pre> -C &lt;double&gt;
 *  Set the cost parameter C
 *   (default: 1)</pre>
 *
 * <pre> -Z
 *  Turn on normalization of input data (default: off)</pre>
 *
 * <pre>
 * -L &lt;double&gt;
 *  The epsilon parameter in epsilon-insensitive loss function.
 *  (default 0.1)
 * </pre>
 *
 * <pre>
 * -I &lt;int&gt;
 *  The maximum number of iterations to perform.
 *  (default 0.1)
 * </pre>
 *
 * <pre> -P
 *  Use probability estimation (default: off)
 * currently for L2-regularized logistic regression only! </pre>
 *
 * <pre> -E &lt;double&gt;
 *  Set tolerance of termination criterion (default: 0.001)</pre>
 *
 * <pre> -W &lt;double&gt;
 *  Set the parameters C of class i to weight[i]*C
 *   (default: 1)</pre>
 *
 * <pre> -B &lt;double&gt;
 *  Add Bias term with the given value if &gt;= 0; if &lt; 0, no bias term added (default: 1)</pre>
 *
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 <!-- options-end -->
 *
 * @author  Benedikt Waldvogel (mail at bwaldvogel.de)
 * @version 1.9.0
 */
public class LibLINEAR extends AbstractClassifier implements TechnicalInformationHandler {

    public static final String  REVISION         = "1.9.0";

    /** serial UID */
    protected static final long serialVersionUID = 230504711;

    /** LibLINEAR Model */
    protected Model             m_Model;


    public Model getModel() {
        return m_Model;
    }

    /** for normalizing the data */
    protected Filter               m_Filter               = null;

    /** normalize input data */
    protected boolean              m_Normalize            = false;

    /** SVM solver types */
    public static final Tag[]      TAGS_SVMTYPE           = {new Tag(SolverType.L2R_LR.getId(), "L2-regularized logistic regression (primal)"),
            new Tag(SolverType.L2R_L2LOSS_SVC_DUAL.getId(), "L2-regularized L2-loss support vector classification (dual)"),
            new Tag(SolverType.L2R_L2LOSS_SVC.getId(), "L2-regularized L2-loss support vector classification (primal)"),
            new Tag(SolverType.L2R_L1LOSS_SVC_DUAL.getId(), "L2-regularized L1-loss support vector classification (dual)"),
            new Tag(SolverType.MCSVM_CS.getId(), "support vector classification by Crammer and Singer"),
            new Tag(SolverType.L1R_L2LOSS_SVC.getId(), "L1-regularized L2-loss support vector classification"),
            new Tag(SolverType.L1R_LR.getId(), "L1-regularized logistic regression"),
            new Tag(SolverType.L2R_LR_DUAL.getId(), "L2-regularized logistic regression (dual)"),
            new Tag(SolverType.L2R_L2LOSS_SVR.getId(), "L2-regularized L2-loss support vector regression (primal)"),
            new Tag(SolverType.L2R_L2LOSS_SVR_DUAL.getId(), "L2-regularized L2-loss support vector regression (dual)"),
            new Tag(SolverType.L2R_L1LOSS_SVR_DUAL.getId(), "L2-regularized L1-loss support vector regression (dual)")};

    protected final SolverType     DEFAULT_SOLVER         = SolverType.L2R_L2LOSS_SVC_DUAL;

    /** the SVM solver type */
    protected SolverType           m_SolverType           = DEFAULT_SOLVER;

    /** stopping criteria */
    protected double               m_eps                  = 0.001;

    /** epsilon of epsilon-insensitive cost function **/
    protected double m_epsilon = 1e-1;

    /** cost Parameter C */
    protected double               m_Cost                 = 1;

    /** the maximum number of iterations */
    protected int m_MaxIts = 1000;

    /** bias term value */
    protected double               m_Bias                 = 1;

    protected int[]                m_WeightLabel          = new int[0];

    protected double[]             m_Weight               = new double[0];

    /** whether to generate probability estimates instead of +1/-1 in case of
     * classification problems */
    protected boolean              m_ProbabilityEstimates = false;

    /** The filter used to make attributes numeric. */
    protected NominalToBinary      m_NominalToBinary;

    /** The filter used to replace missing values. */
    protected ReplaceMissingValues      m_ReplaceMissingValues;

    /** Header of instances for output. */
    protected Instances m_Header;

    /** Class counts (needed for output) */
    protected double[] m_Counts;

    /** coefficients used by normalization filter for doing its linear transformation **/
    protected double m_x1 = 1.0;
    protected double m_x0 = 0.0;

    /**
     * Returns a string describing classifier
     *
     * @return a description suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String globalInfo() {
        return "A wrapper class for the liblinear classifier.\n" + getTechnicalInformation().toString();
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     *
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;

        result = new TechnicalInformation(Type.MISC);
        result.setValue(TechnicalInformation.Field.AUTHOR, "Rong-En Fan and Kai-Wei Chang and Cho-Jui Hsieh and Xiang-Rui Wang and Chih-Jen Lin");
        result.setValue(TechnicalInformation.Field.TITLE, "LIBLINEAR - A Library for Large Linear Classification");
        result.setValue(TechnicalInformation.Field.YEAR, "2008");
        result.setValue(TechnicalInformation.Field.URL, "http://www.csie.ntu.edu.tw/~cjlin/liblinear/");
        result.setValue(TechnicalInformation.Field.NOTE, "The Weka classifier works with version 1.95 of the Java port of LIBLINEAR");

        return result;
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration listOptions() {

        Vector<Object> result = new Vector<Object>();

        result.addElement(new Option("\tSet type of solver (default: 1)\n" //
            + "\tfor multi-class classification\n" //
            + "\t\t 0 -- L2-regularized logistic regression (primal)\n" //
            + "\t\t 1 -- L2-regularized L2-loss support vector classification (dual)\n" //
            + "\t\t 2 -- L2-regularized L2-loss support vector classification (primal)\n" //
            + "\t\t 3 -- L2-regularized L1-loss support vector classification (dual)\n" //
            + "\t\t 4 -- support vector classification by Crammer and Singer\n" //
            + "\t\t 5 -- L1-regularized L2-loss support vector classification\n" //
            + "\t\t 6 -- L1-regularized logistic regression\n" //
            + "\t\t 7 -- L2-regularized logistic regression (dual)\n" //
            + "\tfor regression\n" //
            + "\t\t11 -- L2-regularized L2-loss support vector regression (primal)\n" //
            + "\t\t12 -- L2-regularized L2-loss support vector regression (dual)\n" //
            + "\t\t13 -- L2-regularized L1-loss support vector regression (dual)", //
            "S", 1, "-S <int>"));

        result.addElement(new Option("\tSet the cost parameter C\n" + "\t (default: 1)", "C", 1, "-C <double>"));

        result.addElement(new Option("\tTurn on normalization of input data (default: off)", "Z", 0, "-Z"));

        result.addElement(new Option("\tUse probability estimation (default: off)\n"
            + "currently for L2-regularized logistic regression, L1-regularized logistic regression or L2-regularized logistic regression (dual)! ", "P", 0,
            "-P"));

        result.addElement(new Option("\tSet tolerance of termination criterion (default: 0.001)", "E", 1, "-E <double>"));

        result.addElement(new Option("\tSet the parameters C of class i to weight[i]*C\n" + "\t (default: 1)", "W", 1, "-W <double>"));

        result.addElement(new Option("\tAdd Bias term with the given value if >= 0; if < 0, no bias term added (default: 1)", "B", 1, "-B <double>"));

        result.addElement(new Option(
                "\tThe epsilon parameter in epsilon-insensitive loss function.\n"
                        + "\t(default 0.1)", "L", 1, "-L <double>"));

        result.addElement(new Option(
                "\tThe maximum number of iterations to perform.\n"
                        + "\t(default 1000)", "I", 1, "-I <int>"));

        Enumeration en = super.listOptions();
        while (en.hasMoreElements())
            result.addElement(en.nextElement());

        return result.elements();
    }

    /**
     * Sets the classifier options <p/>
     *
     <!-- options-start -->
     * Valid options are: <p/>
     *
     * <pre> -S &lt;int&gt;
     *  Set type of solver (default: 1)
     *   for multi-class classification
     *     0 -- L2-regularized logistic regression (primal)
     *     1 -- L2-regularized L2-loss support vector classification (dual)
     *     2 -- L2-regularized L2-loss support vector classification (primal)
     *     3 -- L2-regularized L1-loss support vector classification (dual)
     *     4 -- support vector classification by Crammer and Singer
     *     5 -- L1-regularized L2-loss support vector classification
     *     6 -- L1-regularized logistic regression
     *     7 -- L2-regularized logistic regression (dual)
     *  for regression
     *    11 -- L2-regularized L2-loss support vector regression (primal)
     *    12 -- L2-regularized L2-loss support vector regression (dual)
     *    13 -- L2-regularized L1-loss support vector regression (dual)</pre>
     *
     * <pre> -C &lt;double&gt;
     *  Set the cost parameter C
     *   (default: 1)</pre>
     *
     * <pre> -Z
     *  Turn on normalization of input data (default: off)</pre>
     *
     * <pre>
     * -L &lt;double&gt;
     *  The epsilon parameter in epsilon-insensitive loss function.
     *  (default 0.1)
     * </pre>
     *
     * <pre>
     * -I &lt;int&gt;
     *  The maximum number of iterations to perform.
     *  (default 0.1)
     * </pre>
     *
     * <pre> -P
     *  Use probability estimation (default: off)
     * currently for L2-regularized logistic regression only! </pre>
     *
     * <pre> -E &lt;double&gt;
     *  Set tolerance of termination criterion (default: 0.001)</pre>
     *
     * <pre> -W &lt;double&gt;
     *  Set the parameters C of class i to weight[i]*C
     *   (default: 1)</pre>
     *
     * <pre> -B &lt;double&gt;
     *  Add Bias term with the given value if &gt;= 0; if &lt; 0, no bias term added (default: 1)</pre>
     *
     * <pre> -D
     *  If set, classifier is run in debug mode and
     *  may output additional info to the console</pre>
     *
     <!-- options-end -->
     *
     * @param options     the options to parse
     * @throws Exception  if parsing fails
     */
    public void setOptions(String[] options) throws Exception {
        String tmpStr;

        tmpStr = Utils.getOption('S', options);
        if (tmpStr.length() != 0)
            setSVMType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_SVMTYPE));
        else
            setSVMType(new SelectedTag(DEFAULT_SOLVER.getId(), TAGS_SVMTYPE));

        tmpStr = Utils.getOption('C', options);
        if (tmpStr.length() != 0)
            setCost(Double.parseDouble(tmpStr));
        else
            setCost(1);

        tmpStr = Utils.getOption('E', options);
        if (tmpStr.length() != 0)
            setEps(Double.parseDouble(tmpStr));
        else
            setEps(0.001);

        setNormalize(Utils.getFlag('Z', options));

        tmpStr = Utils.getOption('B', options);
        if (tmpStr.length() != 0)
            setBias(Double.parseDouble(tmpStr));
        else
            setBias(1);

        setWeights(Utils.getOption('W', options));

        setProbabilityEstimates(Utils.getFlag('P', options));

        tmpStr = Utils.getOption('L', options);
        if (tmpStr.length() != 0) {
            setEpsilonParameter(Double.parseDouble(tmpStr));
        } else {
            setEpsilonParameter(0.1);
        }

        tmpStr = Utils.getOption('I', options);
        if (tmpStr.length() != 0) {
            setMaximumNumberOfIterations(Integer.parseInt(tmpStr));
        } else {
            setMaximumNumberOfIterations(1000);
        }

        super.setOptions(options);
    }

    /**
     * Returns the current options
     *
     * @return            the current setup
     */
    public String[] getOptions() {

        List<String> options = new ArrayList<String>();

        options.add("-S");
        options.add("" + m_SolverType.getId());

        options.add("-C");
        options.add("" + getCost());

        options.add("-E");
        options.add("" + getEps());

        options.add("-B");
        options.add("" + getBias());

        if (getNormalize()) options.add("-Z");

        if (getWeights().length() != 0) {
            options.add("-W");
            options.add("" + getWeights());
        }

        if (getProbabilityEstimates()) options.add("-P");

        options.add("-L");
        options.add("" + getEpsilonParameter());

        options.add("-I");
        options.add("" + getMaximumNumberOfIterations());

        return options.toArray(new String[options.size()]);
    }

    /**
     * Sets type of SVM (default SVMTYPE_L2)
     *
     * @param value       the type of the SVM
     */
    public void setSVMType(SelectedTag value) {
        if (value.getTags() == TAGS_SVMTYPE) {
            setSolverType(SolverType.getById(value.getSelectedTag().getID()));
        }
    }

    protected void setSolverType(SolverType solverType) {
        m_SolverType = solverType;
    }

    protected SolverType getSolverType() {
        return m_SolverType;
    }

    /**
     * Gets type of SVM
     *
     * @return            the type of the SVM
     */
    public SelectedTag getSVMType() {
        return new SelectedTag(m_SolverType.getId(), TAGS_SVMTYPE);
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String SVMTypeTipText() {
        return "The type of SVM to use.";
    }

    /**
     * Get the value of epsilon parameter of the epsilon insensitive loss
     * function.
     *
     * @return Value of epsilon parameter.
     */
    public double getEpsilonParameter() {
        return m_epsilon;
    }

    /**
     * Set the value of epsilon parameter of the epsilon insensitive loss
     * function.
     *
     * @param v Value to assign to epsilon parameter.
     */
    public void setEpsilonParameter(double v) {
        m_epsilon = v;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String epsilonParameterTipText() {
        return "The epsilon parameter of the epsilon insensitive loss function.";
    }

    /**
     * Get the number of iterations to perform.
     *
     * @return maximum number of iterations to perform.
     */
    public int getMaximumNumberOfIterations() {
        return m_MaxIts;
    }

    /**
     * Set the number of iterations to perform.
     *
     * @param v the number of iterations to perform.
     */
    public void setMaximumNumberOfIterations(int v) {
        m_MaxIts = v;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String maximumNumberOfIterationsTipText() {
        return "The maximum number of iterations to perform.";
    }

    /**
     * Sets the cost parameter C (default 1)
     *
     * @param value       the cost value
     */
    public void setCost(double value) {
        m_Cost = value;
    }

    /**
     * Returns the cost parameter C
     *
     * @return            the cost value
     */
    public double getCost() {
        return m_Cost;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String costTipText() {
        return "The cost parameter C.";
    }

    /**
     * Sets tolerance of termination criterion (default 0.001)
     *
     * @param value       the tolerance
     */
    public void setEps(double value) {
        m_eps = value;
    }

    /**
     * Gets tolerance of termination criterion
     *
     * @return            the current tolerance
     */
    public double getEps() {
        return m_eps;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String epsTipText() {
        return "The tolerance of the termination criterion.";
    }

    /**
     * Sets bias term value (default 1)
     * No bias term is added if value &lt; 0
     *
     * @param value       the bias term value
     */
    public void setBias(double value) {
        m_Bias = value;
    }

    /**
     * Returns bias term value (default 1)
     * No bias term is added if value &lt; 0
     *
     * @return             the bias term value
     */
    public double getBias() {
        return m_Bias;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String biasTipText() {
        return "If >= 0, a bias term with that value is added; " + "otherwise (<0) no bias term is added (default: 1).";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String normalizeTipText() {
        return "Whether to normalize the data.";
    }

    /**
     * whether to normalize input data
     *
     * @param value       whether to normalize the data
     */
    public void setNormalize(boolean value) {
        m_Normalize = value;
    }

    /**
     * whether to normalize input data
     *
     * @return            true, if the data is normalized
     */
    public boolean getNormalize() {
        return m_Normalize;
    }

    /**
     * Sets the parameters C of class i to weight[i]*C (default 1).
     * Blank separated list of doubles.
     *
     * @param weightsStr          the weights (doubles, separated by blanks)
     */
    public void setWeights(String weightsStr) {
        StringTokenizer tok = new StringTokenizer(weightsStr, " ");
        m_Weight = new double[tok.countTokens()];
        m_WeightLabel = new int[tok.countTokens()];

        if (m_Weight.length == 0) System.out.println("Zero Weights processed. Default weights will be used");

        for (int i = 0; i < m_Weight.length; i++) {
            m_Weight[i] = Double.parseDouble(tok.nextToken());
            m_WeightLabel[i] = i;
        }
    }

    /**
     * Gets the parameters C of class i to weight[i]*C (default 1).
     * Blank separated doubles.
     *
     * @return            the weights (doubles separated by blanks)
     */
    public String getWeights() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m_Weight.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(m_Weight[i]);
        }

        return sb.toString();
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String weightsTipText() {
        return "The weights to use for the classes, if empty 1 is used by default.";
    }

    /**
     * Returns whether probability estimates are generated instead of -1/+1 for
     * classification problems.
     *
     * @param value       whether to predict probabilities
     */
    public void setProbabilityEstimates(boolean value) {
        m_ProbabilityEstimates = value;
    }

    /**
     * Sets whether to generate probability estimates instead of -1/+1 for
     * classification problems.
     *
     * @return            true, if probability estimates should be returned
     */
    public boolean getProbabilityEstimates() {
        return m_ProbabilityEstimates;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for
     *         displaying in the explorer/experimenter gui
     */
    public String probabilityEstimatesTipText() {
        return "Whether to generate probability estimates instead of -1/+1 for classification problems "
            + "(currently for L2-regularized logistic regression only!)";
    }

    /**
     * transfers the local variables into a svm_parameter object
     *
     * @return the configured svm_parameter object
     */
    protected Parameter getParameters() {

        Parameter parameter = new Parameter(m_SolverType, m_Cost, m_eps, m_MaxIts, m_epsilon);

        if (m_Weight.length > 0) {
            parameter.setWeights(m_Weight, m_WeightLabel);
        }

        return parameter;
    }

    /**
     * returns the svm_problem
     *
     * @param vx the x values
     * @param vy the y values
     * @param max_index
     * @return the Problem object
     */
    protected Problem getProblem(FeatureNode[][] vx, double[] vy, int max_index) {

        if (vx.length != vy.length) throw new IllegalArgumentException("vx and vy must have same size");

        Problem problem = new Problem();

        problem.l = vy.length;
        problem.n = max_index;
        problem.bias = getBias();
        problem.x = vx;
        problem.y = vy;

        return problem;
    }

    /**
     * returns an instance into a sparse liblinear array
     *
     * @param instance	the instance to work on
     * @return		the liblinear array
     * @throws Exception	if setup of array fails
     */
    protected FeatureNode[] instanceToArray(Instance instance) throws Exception {
        // determine number of non-zero attributes
        int count = 0;

        for (int i = 0; i < instance.numValues(); i++) {
            if (instance.index(i) == instance.classIndex()) continue;
            if (instance.valueSparse(i) != 0) count++;
        }

        if (m_Bias >= 0) {
            count++;
        }

        // fill array
        FeatureNode[] nodes = new FeatureNode[count];
        int index = 0;
        for (int i = 0; i < instance.numValues(); i++) {

            int idx = instance.index(i);
            double val = instance.valueSparse(i);

            if (idx == instance.classIndex()) continue;
            if (val == 0) continue;

            nodes[index] = new FeatureNode(idx + 1, val);
            index++;
        }

        // add bias term
        if (m_Bias >= 0) {
            nodes[index] = new FeatureNode(instance.numAttributes() + 1, m_Bias);
        }

        return nodes;
    }

    /**
     * Computes the distribution for a given instance.
     *
     * @param instance 		the instance for which distribution is computed
     * @return 			the distribution
     * @throws Exception 		if the distribution can't be computed successfully
     */
    public double[] distributionForInstance(Instance instance) throws Exception {

        m_ReplaceMissingValues.input(instance);
        m_ReplaceMissingValues.batchFinished();
        instance = m_ReplaceMissingValues.output();

        m_NominalToBinary.input(instance);
        m_NominalToBinary.batchFinished();
        instance = m_NominalToBinary.output();

        if (m_Filter != null) {
            m_Filter.input(instance);
            m_Filter.batchFinished();
            instance = m_Filter.output();
        }

        FeatureNode[] x = instanceToArray(instance);
        double[] result = new double[instance.numClasses()];
        if (instance.classAttribute().isNominal() && (m_ProbabilityEstimates)) {
            if (m_SolverType != SolverType.L2R_LR && m_SolverType != SolverType.L2R_LR_DUAL && m_SolverType != SolverType.L1R_LR) {
                throw new WekaException("probability estimation is currently only " + "supported for L2-regularized logistic regression");
            }

            int[] labels = m_Model.getLabels();
            double[] prob_estimates = new double[instance.numClasses()];

            Linear.predictProbability(m_Model, x, prob_estimates);

            // Return order of probabilities to canonical weka attribute order
            for (int k = 0; k < labels.length; k++) {
                result[labels[k]] = prob_estimates[k];
            }
        } else {
            double prediction = Linear.predict(m_Model, x);
            if (instance.classAttribute().isNominal()) {
                result[(int) prediction] = 1;
            } else {
                result[0] = prediction * m_x1 + m_x0;
                ;
            }
        }

        return result;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return      the capabilities of this classifier
     */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
	result.enable(Capability.MISSING_VALUES);

	// class
	result.enableDependency(Capability.NOMINAL_CLASS);
	result.enableDependency(Capability.NUMERIC_CLASS);

        // class
	if (m_SolverType.ordinal() >= 0 && m_SolverType.ordinal() <= 7) {
	    result.enable(Capability.NOMINAL_CLASS);
	} else if (m_SolverType.ordinal() >= 8 && m_SolverType.ordinal() <= 10) {
	    result.enable(Capability.NUMERIC_CLASS);
	} else {
	    throw new IllegalArgumentException("Solver " + m_SolverType + " is not supported!");
	}
        result.enable(Capability.MISSING_CLASS_VALUES);
        return result;
    }

    /**
     * builds the classifier
     *
     * @param insts       the training instances
     * @throws Exception  if liblinear classes not in classpath or liblinear
     *                    encountered a problem
     */
    public void buildClassifier(Instances insts) throws Exception {
        m_NominalToBinary = null;
        m_Filter = null;

        getCapabilities().testWithFail(insts);

        // remove instances with missing class
        insts = new Instances(insts);
        insts.deleteWithMissingClass();

	      m_ReplaceMissingValues = new ReplaceMissingValues();
	      m_ReplaceMissingValues.setInputFormat(insts);
	      insts = Filter.useFilter(insts, m_ReplaceMissingValues);

        m_NominalToBinary = new NominalToBinary();
	      m_NominalToBinary.setInputFormat(insts);
	      insts = Filter.useFilter(insts, m_NominalToBinary);
        // retrieve two different class values used to determine filter transformation

        double y0 = insts.instance(0).classValue();
        int index = 1;
        while (index < insts.numInstances() && insts.instance(index).classValue() == y0) {
            index++;
        }
        if (index == insts.numInstances()) {
            // degenerate case, all class values are equal
            // we don't want to deal with this, too much hassle
            throw new Exception("All class values are the same. At least two class values should be different");
        }
        double y1 = insts.instance(index).classValue();

        if (getNormalize()) {
            m_Filter = new Normalize();
            ((Normalize)m_Filter).setIgnoreClass(true); // Normalize class as well
            m_Filter.setInputFormat(insts);
            insts = Filter.useFilter(insts, m_Filter);
        }

        if (m_Filter != null) {
            double z0 = insts.instance(0).classValue();
            double z1 = insts.instance(index).classValue();
            m_x1 = (y0-y1) / (z0 - z1); // no division by zero, since y0 != y1 guaranteed => z0 != z1 ???
            m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1
        } else {
            m_x1 = 1.0;
            m_x0 = 0.0;
        }
        // Find out which classes are empty (needed for output of model)
        if (insts.classAttribute().isNominal()) {
            m_Counts = new double[insts.numClasses()];
            for (Instance inst : insts) {
                m_Counts[(int) inst.classValue()]++;
            }
        }

        double[] vy = new double[insts.numInstances()];
        FeatureNode[][] vx = new FeatureNode[insts.numInstances()][];
        int max_index = 0;

        for (int d = 0; d < insts.numInstances(); d++) {
            Instance inst = insts.instance(d);
            FeatureNode[] x = instanceToArray(inst);
            if (x.length > 0) {
                max_index = Math.max(max_index, x[x.length - 1].index);
            }
            vx[d] = x;
            double classValue = inst.classValue();
	          // int classValueInt = (int)classValue;
            // if (classValueInt != classValue) throw new RuntimeException("unsupported class value: " + classValue);
            vy[d] = classValue;
        }

        if (!m_Debug) {
            Linear.disableDebugOutput();
        } else {
            Linear.enableDebugOutput();
        }

        // reset the PRNG for regression-stable results
        Linear.resetRandom();

        // train model
        m_Model = Linear.train(getProblem(vx, vy, max_index), getParameters());

        // Store header of instances for output
        m_Header = new Instances(insts, 0);
    }

    /**
     * returns a string representation
     *
     * @return a string representation
     */
    public String toString() {

        if (getModel() == null) {
            return "LibLINEAR: No model built yet.";
        }
        StringBuffer sb = new StringBuffer();
        double[] w = getModel().getFeatureWeights();
        sb.append("LibLINEAR wrapper" + "\n\n" + getModel() + "\n\n");

        if (m_Header.classAttribute().isNominal()) {
            int numNonEmptyClasses = 0;
            for (int i = 0; i < m_Counts.length; i++) {
                if (m_Counts[i] > 0) {
                    numNonEmptyClasses++;
                }
            }
            int start = 0;
            for (int i = 0; i < ((m_Header.numClasses() == 2) ? 1 : m_Header.numClasses()); i++) {
                if (m_Counts[(m_Header.numClasses() == 2) ? 1 : i] > 0) {
                    sb.append("Model for class " +
                            ((m_Header.numClasses() == 2) ? m_Header.classAttribute().value(1)
                                    : m_Header.classAttribute().value(i)) + "\n\n");
                    int index = start++;
                    for (int j = 0; j < m_Header.numAttributes(); j++) {
                        if (j != m_Header.classIndex()) {
                            if (w[index] >= 0) {
                                sb.append((j > 0) ? "+" : " ");
                            } else {
                                sb.append("-");
                            }
                            sb.append(Utils.doubleToString(Math.abs(w[index]), 12, getNumDecimalPlaces()) + " * " +
                                    (getNormalize() ? "(normalized) " : "") + m_Header.attribute(j).name() + "\n");
                        }
                        index += ((m_Header.numClasses() == 2) ? 1 : numNonEmptyClasses);
                    }
                    if (m_Bias >= 0) {
                        if (w[index] >= 0) {
                            sb.append("+");
                        }else {
                            sb.append("-");
                        }
                        sb.append(Utils.doubleToString(Math.abs(w[index]), 12, getNumDecimalPlaces()) + " * " + getModel().getBias() +
                                "\n\n");
                    }
                }
            }
        } else { // Numeric class
            if (getNormalize()) {
                sb.append("NOTE: CLASS HAS ALSO BEEN NORMALIZED.\n\n");
            }
            int index = 0;
            for (int j = 0; j < m_Header.numAttributes(); j++) {
                if (j != m_Header.classIndex()) {
                    if (w[index] >= 0) {
                        sb.append((j > 0) ? "+" : " ");
                    } else {
                        sb.append("-");
                    }
                    sb.append(Utils.doubleToString(Math.abs(w[index]), 12, getNumDecimalPlaces()) + " * " +
                            (getNormalize() ? "(normalized) " : "") + m_Header.attribute(j).name() + "\n");
                }
                index++;
            }
            if (m_Bias >= 0) {
                if (w[index] >= 0) {
                    sb.append("+");
                } else {
                    sb.append("-");
                }
                sb.append(Utils.doubleToString(Math.abs(w[index]), 12, getNumDecimalPlaces()) + " * " + getModel().getBias() +
                        "\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    public String getRevision() {
        return REVISION;
    }

    /**
     * Main method for testing this class.
     *
     * @param args the options
     */
    public static void main(String[] args) {
        runClassifier(new LibLINEAR(), args);
    }
}
