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
 *    CascadeGeneralization.java
 *    Copyright (C) 1999-2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.*;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.RandomizableParallelMultipleClassifiersCombiner;
import weka.classifiers.trees.PCT;
import weka.core.*;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 <!-- globalinfo-start -->
 * Combines several classifiers using cascade generalization. Implementation differs from algorithm in original
 * paper in that cross-validation is used to obtain class probability estimates for meta data. Also, optionally,
 * log-odds transformation can be applied to the probabilities obtained using cross-validation. If PCT is
 * used as the meta learner, PCT will be made aware of which attributes are original input attributes
 * and which attributes correspond to class probability estimates.
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * @article{gama2000cascade,
 *   title={Cascade generalization},
 *   author={Gama, Jo{\~a}o and Brazdil, Pavel},
 *   journal={Machine Learning},
 *   volume={41},
 *   number={3},
 *   pages={315--343},
 *   year={2000},
 *   publisher={Springer}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -M &lt;scheme specification&gt;
 *  Full name of meta classifier, followed by options.
 *  (default: "weka.classifiers.rules.Zero")</pre>
 *
 * <pre> -X &lt;number of folds&gt;
 *  Sets the number of cross-validation folds.</pre>
 *
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 *
 *  <pre>
 *  -U
 *  Keep original attributes as well as probabilities to pass to next layer
 *  </pre>
 *
 *  <pre>
 *  -L
 *  Transform probabilities to log-odds.
 *  </pre>
 *
 *  <pre>
 *  -C
 *  Concatenate the predictions of classifiers together rather than averaging
 *  </pre>
 *
 * <pre> -B &lt;classifier specification&gt;
 *  Full class name of classifier to include, followed
 *  by scheme options. May be specified multiple times.
 *  </pre>
 *
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 <!-- options-end -->
 *
 * @author Tim Leathart
 */
public class CascadeGeneralization
        extends RandomizableParallelMultipleClassifiersCombiner
        implements TechnicalInformationHandler {

    /** for serialization */
    static final long serialVersionUID = 5134738957195845152L;

    /** The meta classifier */
    protected Classifier m_MetaClassifier = new PCT();

    /** Format for meta data */
    protected Instances m_MetaFormat = null;

    /** Format for base data */
    protected Instances m_BaseFormat = null;

    /** Keep original attributes */
    protected boolean m_keepOriginal = true;

    /** Convert probabilities to log odds before inputting to meta-classifier */
    protected boolean m_useLogOdds = true;

    /** Concatenate predictions */
    protected boolean m_ConcatenatePredictions = true;

    /** Set the number of folds for the cross-validation */
    protected int m_NumFolds = 5;

    /**
     * Returns a string describing classifier
     * @return a description suitable for
     * displaying in the explorer/experimenter gui
     */
    public String globalInfo() {

        return "Combines several classifiers using cascade generalization. This implementation differs from the algorithm in the original" +
                " paper in that cross-validation is used to obtain class probability estimates for the meta data. Also, if specified, a" +
                " log-odds transformation is applied to the probabilities obtained using cross-validation. Finally, if multiple base" +
                " classifiers are specified, one can either average their class probability estimates or concatenate them. "
                + "For more information, see\n\n"
                + getTechnicalInformation().toString() + "\n\n"
                + "If a probability calibration tree (PCT) is used as the meta learner, it will be made aware of which attributes are the original input attributes"
                + " and which attributes correspond to class probability estimates.";
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     *
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation 	result;

        result = new TechnicalInformation(Type.ARTICLE);
        result.setValue(Field.AUTHOR, "João Gama and Pavel Brazdil");
        result.setValue(Field.YEAR, "2000");
        result.setValue(Field.TITLE, "Cascade generalization");
        result.setValue(Field.JOURNAL, "Machine Learning");
        result.setValue(Field.VOLUME, "41");
        result.setValue(Field.PAGES, "315--343");
        result.setValue(Field.PUBLISHER, "Springer");

        return result;
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration<Option> listOptions() {

        Vector<Option> newVector = new Vector<Option>(7);
        newVector.addElement(new Option(
                metaOption(),
                "M", 0, "-M <scheme specification>"));
        newVector.addElement(new Option(
                "\tSets the number of cross-validation folds.",
                "X", 1, "-X <number of folds>"));
        newVector.addElement(new Option(
                "\tUse original attributes and probabilities in meta classifier, not just probabilities",
                "U", 0, "-U"));
        newVector.addElement(new Option(
                "\tConvert probabilities to log-odds before training meta classifier",
                "L", 0, "-L"));
        newVector.addElement(new Option(
                "\tSets whether the predictions are concatenated or averaged",
                "C", 0, "-C"
                ));

        newVector.addAll(Collections.list(super.listOptions()));

        if (getMetaClassifier() instanceof OptionHandler) {
            newVector.addElement(new Option(
                    "",
                    "", 0, "\nOptions specific to meta classifier "
                    + getMetaClassifier().getClass().getName() + ":"));
            newVector.addAll(Collections.list(((OptionHandler)getMetaClassifier()).listOptions()));
        }
        return newVector.elements();
    }

    /**
     * String describing option for setting meta classifier
     *
     * @return the string describing the option
     */
    protected String metaOption() {

        return "\tFull name of meta classifier, followed by options.\n" +
                "\t(default: \"weka.classifiers.trees.PCT\")";
    }

    /**
     * Parses a given list of options. <p/>
     *
     <!-- options-start -->
     * Valid options are: <p/>
     *
     * <pre> -M &lt;scheme specification&gt;
     *  Full name of meta classifier, followed by options.
     *  (default: "weka.classifiers.rules.Zero")</pre>
     *
     * <pre> -X &lt;number of folds&gt;
     *  Sets the number of cross-validation folds.</pre>
     *
     * <pre> -S &lt;num&gt;
     *  Random number seed.
     *  (default 1)</pre>
     *
     *  <pre>
     *  -U
     *  Keep original attributes as well as probabilities to pass to next layer
     *  </pre>
     *
     *  <pre>
     *  -L
     *  Transform probabilities to log-odds.
     *  </pre>
     *
     *  <pre>
     *  -C
     *  Concatenate the predictions of classifiers together rather than averaging
     *  </pre>
     *
     * <pre> -B &lt;classifier specification&gt;
     *  Full class name of classifier to include, followed
     *  by scheme options. May be specified multiple times.
     *  </pre>
     *
     * <pre> -D
     *  If set, classifier is run in debug mode and
     *  may output additional info to the console</pre>
     *
     <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {

        setKeepOriginal(Utils.getFlag('U', options));
        setUseLogOdds(Utils.getFlag('L', options));
        setConcatenatePredictions(Utils.getFlag('C', options));

        String numFoldsString = Utils.getOption('X', options);
        if (numFoldsString.length() != 0) {
            setNumFolds(Integer.parseInt(numFoldsString));
        } else {
            setNumFolds(10);
        }

        processMetaOptions(options);
        super.setOptions(options);
    }

    /**
     * Process options setting meta classifier.
     *
     * @param options the options to parse
     * @throws Exception if the parsing fails
     */
    protected void processMetaOptions(String[] options) throws Exception {

        String classifierString = Utils.getOption('M', options);
        String [] classifierSpec = Utils.splitOptions(classifierString);
        String classifierName;
        if (classifierSpec.length == 0) {
            classifierName = "weka.classifiers.trees.PCT";
        } else {
            classifierName = classifierSpec[0];
            classifierSpec[0] = "";
        }
        setMetaClassifier(AbstractClassifier.forName(classifierName, classifierSpec));
    }

    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {
        ArrayList<String> options = new ArrayList<String>(10);

        if (getKeepOriginal()) options.add("-U");
        if (getUseLogOdds()) options.add("-L");
        if (getConcatenatePredictions()) options.add("-C");
        options.add("-X"); options.add("" + getNumFolds());
        options.add("-M");
        options.add(getMetaClassifier().getClass().getName() + " "
                + Utils.joinOptions(((OptionHandler)getMetaClassifier()).getOptions()));

        options.addAll(Arrays.asList(super.getOptions()));

        String[] optionsArray = new String[options.size()];
        optionsArray = options.toArray(optionsArray);

        return optionsArray;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String numFoldsTipText() {
        return "The number of folds used for cross-validation.";
    }

    /**
     * Gets the number of folds for the cross-validation.
     *
     * @return the number of folds for the cross-validation
     */
    public int getNumFolds() {

        return m_NumFolds;
    }

    /**
     * Sets the number of folds for the cross-validation.
     *
     * @param numFolds the number of folds for the cross-validation
     * @throws Exception if parameter illegal
     */
    public void setNumFolds(int numFolds) throws Exception {

        if (numFolds < 0) {
            throw new IllegalArgumentException("CascadeGeneralization: Number of cross-validation " +
                    "folds must be positive.");
        }
        m_NumFolds = numFolds;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String keepOriginalTipText() {
        return "Whether to include original attributes in data passed to meta learner.";
    }

    /**
     * Get the value of keepOriginal
     * @return value of keepOriginal
     */
    public boolean getKeepOriginal() {
        return m_keepOriginal;
    }

    /**
     * Set the value of keepOriginal
     * @param keepOriginal value to assign to keepOriginal
     */
    public void setKeepOriginal(boolean keepOriginal) {
        m_keepOriginal = keepOriginal;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String useLogOddsTipText() {
        return "Whether class probability estimates are transformed into log-odds before passed to meta learner.";
    }

    /**
     * Get the value of logOdds
     *
     * @return value of logOdds
     */
    public boolean getUseLogOdds() {
        return m_useLogOdds;
    }

    /**
     * Sets the value of useLogOdds
     * @param useLogOdds value to assign to useLogOdds
     */
    public void setUseLogOdds(boolean useLogOdds) {
        m_useLogOdds = useLogOdds;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String concatenatePredictionsTipText() {
        return "Whether to concatenate base classifiers' probability " +
        "estimates or just average them.";
    }

    /**
     * Get the value of concatenatePredictions
     * @return value of concatenatePredictions
     */
    public boolean getConcatenatePredictions() {
        return m_ConcatenatePredictions;
    }

    /**
     * Set the value of concatenatePredictions
     *
     * @param concatenatePredictions value to assign to concatenatePredictions
     */
    public void setConcatenatePredictions(boolean concatenatePredictions) {
        m_ConcatenatePredictions = concatenatePredictions;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String metaClassifierTipText() {
        return "The meta classifiers to be used.";
    }

    /**
     * Adds meta classifier
     *
     * @param classifier the classifier with all options set.
     */
    public void setMetaClassifier(Classifier classifier) {

        m_MetaClassifier = classifier;
    }

    /**
     * Gets the meta classifier.
     *
     * @return the meta classifier
     */
    public Classifier getMetaClassifier() {

        return m_MetaClassifier;
    }

    /**
     * Returns combined capabilities of the base classifiers, i.e., the
     * capabilities all of them have in common.
     *
     * @return      the capabilities of the base classifiers
     */
    public Capabilities getCapabilities() {
        Capabilities      result;

        result = super.getCapabilities();
        result.setMinimumNumberInstances(getNumFolds());

        return result;
    }

    /**
     * Build classifier method.
     *
     * @param data the training data to be used for generating classifier
     * @throws Exception if the classifier could not be built successfully
     */
    public void buildClassifier(Instances data) throws Exception {

        // can classifier handle the data?
        getCapabilities().testWithFail(data);

        Random random = new Random(m_Seed);

        if (m_MetaClassifier == null) {
            throw new IllegalArgumentException("No meta classifier has been set");
        }

        // remove instances with missing class
        Instances newData = new Instances(data);
        m_BaseFormat = new Instances(data, 0);
        newData.deleteWithMissingClass();

        newData.randomize(random);
        if (newData.classAttribute().isNominal()) {
            newData.stratify(m_NumFolds);
        }

        // Make sure correct attributes are used with PCT
        if (m_MetaClassifier instanceof PCT) {
            int bottomAttribute = getKeepOriginal() ? data.numAttributes() : 1;
            int topAttribute = bottomAttribute + (newData.numClasses() *
                    (m_ConcatenatePredictions ? m_Classifiers.length : 1)) - 1;

            ((PCT) m_MetaClassifier).setProbabilityAttributes(bottomAttribute + "-" + topAttribute);
        }

        // restart the executor pool because at the end of processing
        // a set of classifiers it gets shutdown to prevent the program
        // executing as a server
        super.buildClassifier(newData);

        // Rebuild all the base classifiers on the full training data
        buildClassifiers(newData);

        Classifier[] tmpClassifiers = new Classifier[m_Classifiers.length];
        for (int i = 0; i < m_Classifiers.length; i++) {
            tmpClassifiers[i] = AbstractClassifier.makeCopy(m_Classifiers[i]);
        }

        // Create meta level
        generateMetaLevel(newData, random);

        // Return classifiers from before
        for (int i = 0; i < m_Classifiers.length; i++) {
            m_Classifiers[i] = tmpClassifiers[i];
        }
    }

    /**
     * Generates the meta data
     *
     * @param newData the data to work on
     * @param random the random number generator to use for cross-validation
     * @throws Exception if generation fails
     */
    protected void generateMetaLevel(Instances newData, Random random)
            throws Exception {

        Instances metaData = metaFormat(newData);
        m_MetaFormat = new Instances(metaData, 0);
        for (int j = 0; j < m_NumFolds; j++) {
            Instances train = newData.trainCV(m_NumFolds, j, random);

            // start the executor pool (if necessary)
            // has to be done after each set of classifiers as the
            // executor pool gets shut down in order to prevent the
            // program executing as a server (and not returning to
            // the command prompt when run from the command line
            super.buildClassifier(train);

            // construct the actual classifiers
            buildClassifiers(train);

            // Classify test instances and add to meta data
            Instances test = newData.testCV(m_NumFolds, j);
            for (int i = 0; i < test.numInstances(); i++) {
                metaData.add(metaInstance(test.instance(i)));
            }
        }

        m_MetaClassifier.buildClassifier(metaData);
    }

    /**
     * Returns class probabilities.
     *
     * @param instance the instance to be classified
     * @return the distribution
     * @throws Exception if instance could not be classified
     * successfully
     */
    public double[] distributionForInstance(Instance instance) throws Exception {

        return m_MetaClassifier.distributionForInstance(metaInstance(instance));
    }

    /**
     * Output a representation of this classifier
     *
     * @return a string representation of the classifier
     */
    public String toString() {

        if (m_Classifiers.length == 0) {
            return "CascadeGeneralization: No base schemes entered.";
        }
        if (m_MetaClassifier == null) {
            return "CascadeGeneralization: No meta scheme selected.";
        }
        if (m_MetaFormat == null) {
            return "CascadeGeneralization: No model built yet.";
        }
        String result = "CascadeGeneralization\n\nBase classifiers\n\n";
        for (int i = 0; i < m_Classifiers.length; i++) {
            result += getClassifier(i).toString() +"\n\n";
        }

        result += "\n\nMeta classifier\n\n";
        result += m_MetaClassifier.toString();

        return result;
    }

    /**
     * Makes the format for the level-1 data.
     *
     * @param instances the level-0 format
     * @return the format for the meta data
     * @throws Exception if the format generation fails
     */
    protected Instances metaFormat(Instances instances) throws Exception {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        Instances metaFormat;

        if (getKeepOriginal()) {
            Enumeration<Attribute> originalAttributes = instances.enumerateAttributes();
            while (originalAttributes.hasMoreElements()) {
                attributes.add(originalAttributes.nextElement());
            }
        }

        if (!m_ConcatenatePredictions) {
            if (m_BaseFormat.classAttribute().isNumeric()) {
                attributes.add(new Attribute("Averaged predictions from base learners"));
            } else {
                for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
                    attributes.add(new Attribute(
                                    "Averaged probability for class:" + m_BaseFormat.classAttribute().value(j)));
                }
            }
        } else {
            for (int k = 0; k < m_Classifiers.length; k++) {
                Classifier classifier = getClassifier(k);
                String name = classifier.getClass().getName() + "-" + (k + 1);
                if (m_BaseFormat.classAttribute().isNumeric()) {
                    attributes.add(new Attribute(name));
                } else {
                    for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
                        attributes.add(
                                new Attribute(
                                        name + ":" + m_BaseFormat.classAttribute().value(j)));
                    }
                }
            }
        }
        attributes.add((Attribute) m_BaseFormat.classAttribute().copy());
        metaFormat = new Instances("Meta format", attributes, 0);
        metaFormat.setClassIndex(metaFormat.numAttributes() - 1);
        return metaFormat;
    }

    /**
     * Makes a level-1 instance from the given instance.
     *
     * @param instance the instance to be transformed
     * @return the level-1 instance
     * @throws Exception if the instance generation fails
     */
    protected Instance metaInstance(Instance instance) throws Exception {

        double[] values = new double[m_MetaFormat.numAttributes()];
        for (int v = 0; v < values.length; v++) {
            values[v] = 0.0;
        }

        Instance metaInstance;

        int i = 0;

        if (getKeepOriginal()) {
            for (int k = 0; k < instance.numAttributes(); k++) {
                if (instance.classIndex() == k) continue;

                values[i++] = instance.value(k);
            }
        }

        int numClasses = 1;

        for (int k = 0; k < m_Classifiers.length; k++) {

            Classifier classifier = getClassifier(k);

            if (m_BaseFormat.classAttribute().isNumeric()) {
                values[i++] = classifier.classifyInstance(instance);
            } else {
                double[] dist = classifier.distributionForInstance(instance);

                numClasses = dist.length;

                for (int j = 0; j < dist.length; j++) {

                    // Convert to log-odds if necessary
                    if (getUseLogOdds()) {
                        if (dist[j] == 0)
                            dist[j] = 0.00001;
                        else if (dist[j] == 1)
                            dist[j] = 0.99999;

                        dist[j] = Math.log(dist[j] / (1 - dist[j]));
                    }

                    values[i++] += dist[j];
                }
            }

            if (!m_ConcatenatePredictions) {
                i -= numClasses;
            }
        }

        if (!m_ConcatenatePredictions) {
            for (int k = 0; k < numClasses; k++) {
                values[i++] /= (double)m_Classifiers.length;
            }
        }

        values[i] = instance.classValue();
        metaInstance = new DenseInstance(1, values);
        metaInstance.setDataset(m_MetaFormat);
        return metaInstance;
    }

    /**
     * Returns the revision string.
     *
     * @return		the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 10141 $");
    }

    /**
     * Main method for testing this class.
     *
     * @param argv should contain the following arguments:
     * -t training file [-T test file] [-c class index]
     */
    public static void main(String [] argv) {
        runClassifier(new CascadeGeneralization(), argv);
    }
}
