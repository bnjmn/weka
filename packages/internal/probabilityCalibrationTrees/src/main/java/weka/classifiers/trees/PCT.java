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
 *    PCT.java
 *    Copyright (C) 2003-2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.ModelSelection;
import weka.classifiers.trees.lmt.PCTNode;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.gui.ProgrammaticProperty;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <!-- globalinfo-start --> Classifier for building probability calibration trees. To be used as meta learner
 * in CascadeGeneralization.
 * <br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Tim Leathart, Eibe Frank, Geoffrey Holmes, Bernhard Pfahringer
 * (2017). Probability Calibration Trees. Asian Conference on Machine
 * Learning 2017<br/>
 * <br/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p/>
 *
 * <pre>
 * -C
 *  Use cross-validation for boosting at all nodes (i.e., disable heuristic)
 * </pre>
 *
 * <pre>
 * -I &lt;numIterations&gt;
 *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)
 * </pre>
 *
 * <pre>
 * -M &lt;numInstances&gt;
 *  Set minimum number of instances at which a node can be split (default 15)
 * </pre>
 *
 * <pre>
 * -W &lt;beta&gt;
 *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.
 * </pre>
 *
 * <pre>
 * -E
 *  Use root mean squared error for pruning.
 * </pre>
 *
 * <pre>
 * -A
 *  The AIC is used to choose the best iteration.
 * </pre>
 *
 * <pre>
 * -doNotMakeSplitPointActualValue
 *  Do not make split point actual value.
 * </pre>
 *
 * <!-- options-end -->
 *
 * @author Niels Landwehr
 * @author Marc Sumner
 * @author Tim Leathart
 * @version $Revision: 11568 $
 */
public class PCT extends AbstractClassifier implements OptionHandler,
        AdditionalMeasureProducer, Drawable, TechnicalInformationHandler {

    /** for serialization */
    static final long serialVersionUID = -1113212459618104943L;

    /** Filter to replace missing values */
    protected ReplaceMissingValues m_replaceMissing;

    /** Filter to remove the probability attributes from a dataset */
    protected Remove m_removeProbabilityAttributes;

    /** Filter to remove the original attributes, leaving only the probability attributes, from a dataset */
    protected Remove m_removeOriginalAttributes;

    /** root of the logistic model tree */
    protected PCTNode m_tree;

    /** indices of attributes that are probability estimates and should be used in logistic models*/
    protected Range m_probabilityAttributes = new Range("");

    /**
     * use heuristic that determines the number of LogitBoost iterations only once
     * in the beginning?
     */
    protected boolean m_fastRegression;

    /** minimum number of instances at which a node is considered for splitting */
    protected int m_minNumInstances;

    /** if non-zero, use fixed number of iterations for LogitBoost */
    protected int m_numBoostingIterations;

    /**
     * Use RMSE as pruning criterion
     */
    protected boolean m_useRootMeanSquaredError = true;

    /**
     * Threshold for trimming weights. Instances with a weight lower than this (as
     * a percentage of total weights) are not included in the regression fit.
     **/
    protected double m_weightTrimBeta;

    /** If true, the AIC is used to choose the best LogitBoost iteration */
    protected boolean m_useAIC = false;

    /** Do not relocate split point to actual data value */
    protected boolean m_doNotMakeSplitPointActualValue;

    /** ZeroR model to be used if no probability attributes are given */
    protected ZeroR m_ZeroR;

    /**
     * Creates an instance of PCT with standard options
     */
    public PCT() {
        m_fastRegression = true;
        m_numBoostingIterations = -1;
        m_minNumInstances = 15;
        m_weightTrimBeta = 0;
        m_useAIC = false;
        m_useRootMeanSquaredError = true;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return the capabilities of this classifier
     */
    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.MISSING_CLASS_VALUES);

        return result;
    }

    /**
     * Builds the classifier.
     *
     * @param data the data to train with
     * @throws Exception if classifier can't be built successfully
     */
    @Override
    public void buildClassifier(Instances data) throws Exception {

        // can classifier handle the data?
        getCapabilities().testWithFail(data);


        m_probabilityAttributes.setUpper(data.numAttributes() - 1);

        // ensure that all columns marked as 'probabilities' are numeric and also not the class attribute
        int[] probabilityAttributes = m_probabilityAttributes.getSelection();
        for (int index : probabilityAttributes) {
            Attribute att = data.attribute(index);
            if (!att.isNumeric() || data.classIndex() == index) {
                throw new IllegalArgumentException("Attributes selected for probability must be numeric and not the class attribute");
            }
        }
        if (probabilityAttributes.length == 0) {
            m_ZeroR = new ZeroR();
            m_ZeroR.buildClassifier(data);
            m_tree = null;
            return;
        } else {
            m_ZeroR = null;
        }

        // remove instances with missing class
        Instances filteredData = new Instances(data);
        filteredData.deleteWithMissingClass();

        // replace missing values
        m_replaceMissing = new ReplaceMissingValues();
        m_replaceMissing.setInputFormat(data);
        filteredData = Filter.useFilter(filteredData, m_replaceMissing);

        m_removeProbabilityAttributes = new Remove();
        m_removeProbabilityAttributes.setInvertSelection(false);
        m_removeProbabilityAttributes.setAttributeIndices(m_probabilityAttributes.getRanges());
        m_removeProbabilityAttributes.setInputFormat(data);

        m_removeOriginalAttributes = new Remove();
        m_removeOriginalAttributes.setInvertSelection(true);
        m_removeOriginalAttributes.setAttributeIndices(m_probabilityAttributes.getRanges() + "," + (data.classIndex() + 1));
        m_removeOriginalAttributes.setInputFormat(data);

        Instances originalData = PCT.splitAttributes(filteredData, m_removeProbabilityAttributes);

        int minNumInstances = 2;

        // create ModelSelection object
        ModelSelection modSelection = new C45ModelSelection(minNumInstances, originalData, true,
                    m_doNotMakeSplitPointActualValue);

        // create tree root
        m_tree = new PCTNode(modSelection, m_numBoostingIterations,
                m_fastRegression, m_minNumInstances,
                m_weightTrimBeta, m_useAIC, m_useRootMeanSquaredError,
                m_removeProbabilityAttributes, m_removeOriginalAttributes,
                m_numDecimalPlaces);
        // build tree
        m_tree.buildClassifier(filteredData);

        if (modSelection instanceof C45ModelSelection) {
            ((C45ModelSelection) modSelection).cleanup();
        }
    }

    public static Instances splitAttributes(Instances data, Remove remove) throws Exception {
        return Filter.useFilter(data, remove);
    }

    public static Instance splitAttributes(Instance data, Remove remove) throws Exception {
        remove.input(data);
        return remove.output();
    }

    /**
     * Returns class probabilities for an instance.
     *
     * @param instance the instance to compute the distribution for
     * @return the class probabilities
     * @throws Exception if distribution can't be computed successfully
     */
    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {

        if (m_ZeroR != null) {
            return m_ZeroR.distributionForInstance(instance);
        }

        // replace missing values
        m_replaceMissing.input(instance);
        instance = m_replaceMissing.output();

        return m_tree.distributionForInstance(instance);
    }

    /**
     * Classifies an instance.
     *
     * @param instance the instance to classify
     * @return the classification
     * @throws Exception if instance can't be classified successfully
     */
    @Override
    public double classifyInstance(Instance instance) throws Exception {

        double maxProb = -1;
        int maxIndex = 0;

        // classify by maximum probability
        double[] probs = distributionForInstance(instance);
        for (int j = 0; j < instance.numClasses(); j++) {
            if (Utils.gr(probs[j], maxProb)) {
                maxIndex = j;
                maxProb = probs[j];
            }
        }
        return maxIndex;
    }

    /**
     * Returns a description of the classifier.
     *
     * @return a string representation of the classifier
     */
    @Override
    public String toString() {
        if (m_tree != null) {
            return "Logistic model tree \n------------------\n" + m_tree.toString();
        } else {
            if (m_ZeroR != null) {
                return m_ZeroR.toString();
            } else {
                return "No tree build";
            }
        }
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector<Option>(13);

        newVector
                .addElement(new Option(
                        "\tUse cross-validation for boosting at all nodes (i.e., disable heuristic)",
                        "C", 0, "-C"));

        newVector.addElement(new Option(
                "\tUse RMSE as pruning criterion", "E", 0, "-E"));

        newVector.addElement(new Option(
                "\tSet fixed number of iterations for LogitBoost (instead of using "
                        + "cross-validation)", "I", 1, "-I <numIterations>"));

        newVector
                .addElement(new Option(
                        "\tSet minimum number of instances at which a node can be split (default 15)",
                        "M", 1, "-M <numInstances>"));

        newVector
                .addElement(new Option(
                        "\tSet beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.",
                        "W", 1, "-W <beta>"));

        newVector.addElement(new Option(
                "\tThe AIC is used to choose the best iteration.", "A", 0, "-A"));

        newVector.addElement(new Option("\tDo not make split point actual value.",
                "doNotMakeSplitPointActualValue", 0, "-doNotMakeSplitPointActualValue"));

        newVector.addAll(Collections.list(super.listOptions()));

        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     * <p/>
     *
     * <!-- options-start --> Valid options are:
     * <p/>
      *
     * <pre>
     * -C
     *  Use cross-validation for boosting at all nodes (i.e., disable heuristic)
     * </pre>
     *
     * <pre>
     * -I &lt;numIterations&gt;
     *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)
     * </pre>
     *
     * <pre>
     * -M &lt;numInstances&gt;
     *  Set minimum number of instances at which a node can be split (default 15)
     * </pre>
     *
     * <pre>
     * -W &lt;beta&gt;
     *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.
     * </pre>
     *
     * <pre>
     * -E
     * Use root mean squared error for pruning.
     * </pre>
     *
     * <pre>
     * -A
     *  The AIC is used to choose the best iteration.
     * </pre>
     *
     * <pre>
     * -doNotMakeSplitPointActualValue
     *  Do not make split point actual value.
     * </pre>
     *
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        setFastRegression(!Utils.getFlag('C', options));
        setUseRootMeanSquaredError(Utils.getFlag('E', options));

        String optionString = Utils.getOption('I', options);
        if (optionString.length() != 0) {
            setNumBoostingIterations((new Integer(optionString)).intValue());
        }

        optionString = Utils.getOption('M', options);
        if (optionString.length() != 0) {
            setMinNumInstances((new Integer(optionString)).intValue());
        }

        optionString = Utils.getOption('W', options);
        if (optionString.length() != 0) {
            setWeightTrimBeta((new Double(optionString)).doubleValue());
        }

        setUseAIC(Utils.getFlag('A', options));
        m_doNotMakeSplitPointActualValue = Utils.getFlag(
                "doNotMakeSplitPointActualValue", options);

        super.setOptions(options);

        Utils.checkForRemainingOptions(options);

    }

    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {

        Vector<String> options = new Vector<String>();

        if (!getFastRegression()) {
            options.add("-C");
        }

        if (getUseRootMeanSquaredError()) {
            options.add("-E");
        }

        options.add("-I");
        options.add("" + getNumBoostingIterations());

        options.add("-M");
        options.add("" + getMinNumInstances());

        options.add("-W");
        options.add("" + getWeightTrimBeta());

        if (getUseAIC()) {
            options.add("-A");
        }

        if (m_doNotMakeSplitPointActualValue) {
            options.add("-doNotMakeSplitPointActualValue");
        }

        Collections.addAll(options, super.getOptions());

        return options.toArray(new String[0]);
    }

    /**
     * Get the value of weightTrimBeta.
     */
    public double getWeightTrimBeta() {
        return m_weightTrimBeta;
    }

    /**
     * Get the value of useAIC.
     *
     * @return Value of useAIC.
     */
    public boolean getUseAIC() {
        return m_useAIC;
    }

    /**
     * Set the value of weightTrimBeta.
     */
    public void setWeightTrimBeta(double n) {
        m_weightTrimBeta = n;
    }

    /**
     * Set the value of useAIC.
     *
     * @param c Value to assign to useAIC.
     */
    public void setUseAIC(boolean c) {
        m_useAIC = c;
    }

    /**
     * Get the value of fastRegression.
     *
     * @return Value of fastRegression.
     */
    public boolean getFastRegression() {
        return m_fastRegression;
    }

    /**
     * Get the value of numBoostingIterations.
     *
     * @return Value of numBoostingIterations.
     */
    public int getNumBoostingIterations() {
        return m_numBoostingIterations;
    }

    /**
     * Get the value of minNumInstances.
     *
     * @return Value of minNumInstances.
     */
    public int getMinNumInstances() { return m_minNumInstances; }

    /**
     * Get the value of useRootMeanSquaredError
     *
     * @return Value of useRootMeanSquaredError
     */
    public boolean getUseRootMeanSquaredError() { return m_useRootMeanSquaredError; }

    /**
     * Set the value of fastRegression.
     *
     * @param c Value to assign to fastRegression.
     */
    public void setFastRegression(boolean c) {
        m_fastRegression = c;
    }

    /**
     * Set the value of numBoostingIterations.
     *
     * @param c Value to assign to numBoostingIterations.
     */
    public void setNumBoostingIterations(int c) {
        m_numBoostingIterations = c;
    }

    /**
     * Set the value of minNumInstances.
     *
     * @param c Value to assign to minNumInstances.
     */
    public void setMinNumInstances(int c) { m_minNumInstances = c; }

    /**
     * Set value of useRootMeanSquaredError
     *
     * @param c Value to assign to useRootMeanSquaredError
     */
    public void setUseRootMeanSquaredError(boolean c) { m_useRootMeanSquaredError = c; }

    /**
     * Get the value of probabilityAttributes
     *
     * @return value of probabilityAttributes
     */
    @ProgrammaticProperty
    public String getProbabilityAttributes() { return m_probabilityAttributes.getRanges(); }
    public void setProbabilityAttributes(String c) { m_probabilityAttributes.setRanges(c); }

    /**
     * Returns the type of graph this classifier represents.
     *
     * @return Drawable.TREE
     */
    @Override
    public int graphType() {
        return Drawable.TREE;
    }

    /**
     * Returns graph describing the tree.
     *
     * @return the graph describing the tree
     * @throws Exception if graph can't be computed
     */
    @Override
    public String graph() throws Exception {

        return m_tree.graph();
    }

    /**
     * Returns the size of the tree
     *
     * @return the size of the tree
     */
    public int measureTreeSize() {
        return m_tree.numNodes();
    }

    /**
     * Returns the number of leaves in the tree
     *
     * @return the number of leaves in the tree
     */
    public int measureNumLeaves() {
        return m_tree.numLeaves();
    }

    /**
     * Returns an enumeration of the additional measure names
     *
     * @return an enumeration of the measure names
     */
    @Override
    public Enumeration<String> enumerateMeasures() {
        Vector<String> newVector = new Vector<String>(2);
        newVector.addElement("measureTreeSize");
        newVector.addElement("measureNumLeaves");

        return newVector.elements();
    }

    /**
     * Returns the value of the named measure
     *
     * @param additionalMeasureName the name of the measure to query for its value
     * @return the value of the named measure
     * @throws IllegalArgumentException if the named measure is not supported
     */
    @Override
    public double getMeasure(String additionalMeasureName) {
        if (additionalMeasureName.compareToIgnoreCase("measureTreeSize") == 0) {
            return measureTreeSize();
        } else if (additionalMeasureName.compareToIgnoreCase("measureNumLeaves") == 0) {
            return measureNumLeaves();
        } else {
            throw new IllegalArgumentException(additionalMeasureName
                    + " not supported (PCT)");
        }
    }

    /**
     * Returns a string describing classifier
     *
     * @return a description suitable for displaying in the explorer/experimenter
     *         gui
     */
    public String globalInfo() {
        return "Classifier for building probability calibration trees. To be used as meta learner in " +
                "CascadeGeneralization.\n\n"
                + "For more information see: \n\n" + getTechnicalInformation().toString();
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing detailed
     * information about the technical background of this class, e.g., paper
     * reference or book this class is based on.
     *
     * @return the technical information about this class
     */
    @Override
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;

        result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR, "Tim Leathart and Eibe Frank and Geoffrey Holmes and Bernhard Pfahringer");
        result.setValue(Field.TITLE, "Probability Calibration Trees");
        result.setValue(Field.BOOKTITLE, "Asian Conference on Machine Learning");
        result.setValue(Field.PUBLISHER, "Springer");
        result.setValue(Field.PAGES, "145--160");
        result.setValue(Field.YEAR, "2017");

        return result;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String fastRegressionTipText() {
        return "Use heuristic that avoids cross-validating the number of Logit-Boost iterations at every node. "
                + "When fitting the logistic regression functions at a node, PCT has to determine the number of LogitBoost "
                + "iterations to run. Originally, this number was cross-validated at every node in the tree. "
                + "To save time, this heuristic cross-validates the number only once and then uses that number at every "
                + "node in the tree. Usually this does not decrease accuracy but improves runtime considerably.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String numBoostingIterationsTipText() {
        return "Set a fixed number of iterations for LogitBoost. If >= 0, this sets a fixed number of LogitBoost "
                + "iterations that is used everywhere in the tree. If < 0, the number is cross-validated.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String minNumInstancesTipText() {
        return "Set the minimum number of instances at which a node is considered for splitting. "
                + "The default value is 15.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String weightTrimBetaTipText() {
        return "Set the beta value used for weight trimming in LogitBoost. "
                + "Only instances carrying (1 - beta)% of the weight from previous iteration "
                + "are used in the next iteration. Set to 0 for no weight trimming. "
                + "The default value is 0.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String useAICTipText() {
        return "The AIC is used to determine when to stop LogitBoost iterations. "
                + "The default is not to use AIC.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String doNotMakeSplitPointActualValueTipText() {
        return "If true, the split point is not relocated to an actual data value."
                + " This can yield substantial speed-ups for large datasets with numeric attributes.";
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String useRootMeanSquaredErrorTipText() {
        return "Use root mean squared error for pruning.";
    }
    /**
     * Gets the value of doNotMakeSplitPointActualValue.
     *
     * @return the value
     */
    public boolean getDoNotMakeSplitPointActualValue() {
        return m_doNotMakeSplitPointActualValue;
    }

    /**
     * Sets the value of doNotMakeSplitPointActualValue.
     *
     * @param m_doNotMakeSplitPointActualValue the value to set
     */
    public void setDoNotMakeSplitPointActualValue(
            boolean m_doNotMakeSplitPointActualValue) {
        this.m_doNotMakeSplitPointActualValue = m_doNotMakeSplitPointActualValue;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 11568 $");
    }

    /**
     * Main method for testing this class
     *
     * @param argv the commandline options
     */
    public static void main(String[] argv) {
        runClassifier(new PCT(), argv);
    }
}
