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
 *    StackingC.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.RandomizableParallelMultipleClassifiersCombiner;
import weka.classifiers.functions.LinearRegression;
import weka.core.*;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.attribute.Remove;

import java.util.*;

/**
 <!-- globalinfo-start -->
 * Implements StackingC (more efficient version of stacking).<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * A.K. Seewald: How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness. In: Nineteenth International Conference on Machine Learning, 554-561, 2002.<br/>
 * <br/>
 * Note: requires meta classifier to be a numeric prediction scheme.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Seewald2002,
 *    author = {A.K. Seewald},
 *    booktitle = {Nineteenth International Conference on Machine Learning},
 *    editor = {C. Sammut and A. Hoffmann},
 *    pages = {554-561},
 *    publisher = {Morgan Kaufmann Publishers},
 *    title = {How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness},
 *    year = {2002}
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
 *  Must be a numeric prediction scheme. Default: Linear Regression.</pre>
 * 
 * <pre> -X &lt;number of folds&gt;
 *  Sets the number of cross-validation folds.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -B &lt;classifier specification&gt;
 *  Full class name of classifier to include, followed
 *  by scheme options. May be specified multiple times.
 *  (default: "weka.classifiers.rules.ZeroR")</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Alexander K. Seewald (alex@seewald.at)
 * @version $Revision$ 
 */
public class StackingC 
  extends RandomizableParallelMultipleClassifiersCombiner
  implements OptionHandler, TechnicalInformationHandler {

  /**
   * for serialization
   */
  static final long serialVersionUID = -6717545616603725198L;

  /**
   * The meta classifiers (one for each class, like in ClassificationViaRegression)
   */
  protected Classifier[] m_MetaClassifiers = null;

  /**
   * Filter to transform metaData - Remove
   */
  protected Remove m_attrFilter = null;
  /**
   * Filter to transform metaData - MakeIndicator
   */
  protected MakeIndicator m_makeIndicatorFilter = null;

  /**
   * The meta classifier
   */
  protected Classifier m_MetaClassifier;

  /**
   * Format for meta data
   */
  protected Instances m_MetaFormat = null;

  /**
   * Format for base data
   */
  protected Instances m_BaseFormat = null;

  /**
   * Set the number of folds for the cross-validation
   */
  protected int m_NumFolds = 10;

  /**
   * The constructor.
   */
  public StackingC() {
    m_MetaClassifier = new weka.classifiers.functions.LinearRegression();
    ((LinearRegression) (getMetaClassifier())).
            setAttributeSelectionMethod(new
                    weka.core.SelectedTag(1, LinearRegression.TAGS_SELECTION));
  }

  /**
   * Returns combined capabilities of the base classifiers, i.e., the
   * capabilities all of them have in common.
   *
   * @return the capabilities of the base classifiers
   */
  public Capabilities getCapabilities() {
    Capabilities result;

    result = super.getCapabilities();
    result.disable(Capabilities.Capability.NUMERIC_CLASS);
    result.disableDependency(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.setMinimumNumberInstances(getNumFolds());

    return result;
  }

  /**
   * Returns a string describing classifier
   *
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Implements StackingC (more efficient version of stacking).\n\n"
            + "For more information, see\n\n"
            + getTechnicalInformation().toString() + "\n\n"
            + "Note: requires meta classifier to be a numeric prediction scheme.";
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

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "A.K. Seewald");
    result.setValue(Field.TITLE, "How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness");
    result.setValue(Field.BOOKTITLE, "Nineteenth International Conference on Machine Learning");
    result.setValue(Field.EDITOR, "C. Sammut and A. Hoffmann");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.PAGES, "554-561");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann Publishers");

    return result;
  }

  /**
   * String describing option for setting meta classifier
   *
   * @return string describing the option
   */
  protected String metaOption() {

    return "\tFull name of meta classifier, followed by options.\n"
            + "\tMust be a numeric prediction scheme. Default: Linear Regression.";
  }

  /**
   * Process options setting meta classifier.
   *
   * @param options the meta options to parse
   * @throws Exception if parsing fails
   */
  protected void processMetaOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('M', options);
    String[] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length != 0) {
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      setMetaClassifier(AbstractClassifier.forName(classifierName, classifierSpec));
    } else {
      ((LinearRegression) (getMetaClassifier())).
              setAttributeSelectionMethod(new
                      weka.core.SelectedTag(1, LinearRegression.TAGS_SELECTION));
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(2);
    newVector.addElement(new Option(metaOption(), "M", 0, "-M <scheme specification>"));
    newVector.addElement(new Option("\tSets the number of cross-validation folds.",
            "X", 1, "-X <number of folds>"));

    newVector.addAll(Collections.list(super.listOptions()));

    if (getMetaClassifier() instanceof OptionHandler) {
      newVector.addElement(new Option("",
              "", 0, "\nOptions specific to meta classifier "
              + getMetaClassifier().getClass().getName() + ":"));
      newVector.addAll(Collections.list(((OptionHandler) getMetaClassifier()).listOptions()));
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * <p>
   * <!-- options-start -->
   * Valid options are: <p/>
   * <p>
   * <pre> -M &lt;scheme specification&gt;
   *  Full name of meta classifier, followed by options.
   *  (default: "weka.classifiers.rules.Zero")</pre>
   * <p>
   * <pre> -X &lt;number of folds&gt;
   *  Sets the number of cross-validation folds.</pre>
   * <p>
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * <p>
   * <pre> -B &lt;classifier specification&gt;
   *  Full class name of classifier to include, followed
   *  by scheme options. May be specified multiple times.
   *  (default: "weka.classifiers.rules.ZeroR")</pre>
   * <p>
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * <p>
   * <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

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
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] superOptions = super.getOptions();
    String[] options = new String[superOptions.length + 4];

    int current = 0;
    options[current++] = "-X";
    options[current++] = "" + getNumFolds();
    options[current++] = "-M";
    options[current++] = getMetaClassifier().getClass().getName() + " "
            + Utils.joinOptions(((OptionHandler) getMetaClassifier()).getOptions());

    System.arraycopy(superOptions, 0, options, current, superOptions.length);
    return options;
  }

  /**
   * Returns the tip text for this property
   *
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
      throw new IllegalArgumentException("Stacking: Number of cross-validation folds must be positive.");
    }
    m_NumFolds = numFolds;
  }

  /**
   * Returns the tip text for this property
   *
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
   * Returns true if the meta classifier or any of the base classifiers are able to generate batch predictions
   * efficiently and all of them implement BatchPredictor.
   *
   * @return true if batch prediction can be done efficiently
   */
  public boolean implementsMoreEfficientBatchPrediction() {

    if (!(getMetaClassifier() instanceof BatchPredictor)) {
      return super.implementsMoreEfficientBatchPrediction();
    }
    boolean atLeastOneIsEfficient = false;
    for (Classifier c : getClassifiers()) {
      if (!(c instanceof BatchPredictor)) {
        return super.implementsMoreEfficientBatchPrediction();
      }
      atLeastOneIsEfficient |= ((BatchPredictor) c).implementsMoreEfficientBatchPrediction();
    }
    return atLeastOneIsEfficient || ((BatchPredictor) getMetaClassifier()).implementsMoreEfficientBatchPrediction();
  }

  /**
   * Returns true if any of the base classifiers are able to generate batch predictions
   * efficiently and all of them implement BatchPredictor.
   *
   * @return true if the base classifiers can do batch prediction efficiently
   */
  public boolean baseClassifiersImplementMoreEfficientBatchPrediction() {

    boolean atLeastOneIsEfficient = false;
    for (Classifier c : getClassifiers()) {
      if (!(c instanceof BatchPredictor)) {
        return super.implementsMoreEfficientBatchPrediction();
      }
      atLeastOneIsEfficient |= ((BatchPredictor) c).implementsMoreEfficientBatchPrediction();
    }
    return atLeastOneIsEfficient;
  }

  /**
   * Method that builds meta level.
   *
   * @param newData the data to work with
   * @param random  the random number generator to use for cross-validation
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

      // Classify test instances to add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      if (baseClassifiersImplementMoreEfficientBatchPrediction()) {
        metaData.addAll(metaInstances(test));
      } else {
        for (int i = 0; i < test.numInstances(); i++) {
          metaData.add(metaInstance(test.instance(i)));
        }
      }
    }

    m_MetaClassifiers = AbstractClassifier.makeCopies(m_MetaClassifier,
            m_BaseFormat.numClasses());

    int[] arrIdc = new int[m_Classifiers.length + 1];
    arrIdc[m_Classifiers.length] = metaData.numAttributes() - 1;
    Instances newInsts;
    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
        arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter = new weka.filters.unsupervised.attribute.MakeIndicator();
      m_makeIndicatorFilter.setAttributeIndex("" + (metaData.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(metaData);
      newInsts = Filter.useFilter(metaData, m_makeIndicatorFilter);

      m_attrFilter = new weka.filters.unsupervised.attribute.Remove();
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      newInsts = Filter.useFilter(newInsts, m_attrFilter);

      newInsts.setClassIndex(newInsts.numAttributes() - 1);

      m_MetaClassifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Returns class probabilities for all given instances if the class is nominal or corresponding predicted
   * numeric values if the class is numeric. The meta classifier must implement BatchPredictor, otherwise an
   * exception will be thrown.
   *
   * @param instances the instance sto be classified
   * @return the distributions
   * @throws Exception if instances could not be classified successfully
   */
  public double[][] distributionsForInstances(Instances instances) throws Exception {

    Instances metaData;
    if (!baseClassifiersImplementMoreEfficientBatchPrediction()) {
      metaData = new Instances(m_MetaFormat, 0);
      for (Instance inst : instances) {
        metaData.add(metaInstance(inst));
      }
    } else {
      metaData = metaInstances(instances);
    }

    int[] arrIdc = new int[m_Classifiers.length + 1];
    arrIdc[m_Classifiers.length] = metaData.numAttributes() - 1;
    double[][] classProbs = new double[instances.numInstances()][m_BaseFormat.numClasses()];
    Instances newInsts;
    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
        arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter = new weka.filters.unsupervised.attribute.MakeIndicator();
      m_makeIndicatorFilter.setAttributeIndex("" + (metaData.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(metaData);
      newInsts = Filter.useFilter(metaData, m_makeIndicatorFilter);

      m_attrFilter = new weka.filters.unsupervised.attribute.Remove();
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      newInsts = Filter.useFilter(newInsts, m_attrFilter);

      newInsts.setClassIndex(newInsts.numAttributes() - 1);

      double[][] preds = ((BatchPredictor)m_MetaClassifiers[i]).distributionsForInstances(newInsts);
      for (int k = 0; k < preds.length; k++) {
        classProbs[k][i] = preds[k][0];
      }
    }

    for (int l = 0; l < classProbs.length; l++) {
      double sum = 0;
      for (int i = 0; i < instances.numClasses(); i++) {
        if (classProbs[l][i] > 1) {
          classProbs[l][i] = 1;
        }
        if (classProbs[l][i] < 0) {
          classProbs[l][i] = 0;
        }
        sum += classProbs[l][i];
      }
      if (sum != 0) Utils.normalize(classProbs[l], sum);
    }

    return classProbs;
  }

  /**
   * Builds a classifier using stacking. The base classifiers' output is fed into the meta classifiers
   * to make the final decision. The training data for the meta classifier is generated using
   * (stratified) cross-validation.
   *
   * @param data the training data to be used for generating the stacked classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_MetaClassifier == null) {
      throw new IllegalArgumentException("No meta classifier has been set");
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();

    Random random = new Random(m_Seed);
    newData.randomize(random);
    if (newData.classAttribute().isNominal()) {
      newData.stratify(m_NumFolds);
    }

    // Create meta level
    generateMetaLevel(newData, random);

    // restart the executor pool because at the end of processing
    // a set of classifiers it gets shutdown to prevent the program
    // executing as a server
    super.buildClassifier(newData);

    // Rebuild all the base classifiers on the full training data
    buildClassifiers(newData);
  }

  /**
   * Classifies a given instance using the stacked classifier.
   *
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified
   *                   successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    int[] arrIdc = new int[m_Classifiers.length + 1];
    arrIdc[m_Classifiers.length] = m_MetaFormat.numAttributes() - 1;
    double[] classProbs = new double[m_BaseFormat.numClasses()];
    Instance newInst;
    double sum = 0;

    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
        arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter.setAttributeIndex("" + (m_MetaFormat.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(m_MetaFormat);
      m_makeIndicatorFilter.input(metaInstance(instance));
      m_makeIndicatorFilter.batchFinished();
      newInst = m_makeIndicatorFilter.output();

      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      m_attrFilter.input(newInst);
      m_attrFilter.batchFinished();
      newInst = m_attrFilter.output();

      classProbs[i] = m_MetaClassifiers[i].classifyInstance(newInst);
      if (classProbs[i] > 1) {
        classProbs[i] = 1;
      }
      if (classProbs[i] < 0) {
        classProbs[i] = 0;
      }
      sum += classProbs[i];
    }

    if (sum != 0) Utils.normalize(classProbs, sum);

    return classProbs;
  }

  /**
   * Output a representation of this classifier
   *
   * @return a string representation of the classifier
   */
  public String toString() {

    if (m_MetaFormat == null) {
      return "StackingC: No model built yet.";
    }
    String result = "StackingC\n\nBase classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += getClassifier(i).toString() + "\n\n";
    }

    result += "\n\nMeta classifiers (one for each class)\n\n";
    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      result += m_MetaClassifiers[i].toString() + "\n\n";
    }

    return result;
  }

  /**
   * Determines the format of the level-1 data.
   *
   * @param instances the level-0 format
   * @return the format for the meta data
   * @throws Exception if the format generation fails
   */
  protected Instances metaFormat(Instances instances) throws Exception {
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    Instances metaFormat;

    for (int k = 0; k < m_Classifiers.length; k++) {
      Classifier classifier = getClassifier(k);
      String name = classifier.getClass().getName() + "-" + (k + 1);
      if (m_BaseFormat.classAttribute().isNumeric()) {
        attributes.add(new Attribute(name));
      } else {
        for (int j = 0; j < m_BaseFormat.classAttribute().numValues(); j++) {
          attributes.add(new Attribute(name + ":" + m_BaseFormat.classAttribute().value(j)));
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
    Instance metaInstance;
    int i = 0;
    for (int k = 0; k < m_Classifiers.length; k++) {
      Classifier classifier = getClassifier(k);
      if (m_BaseFormat.classAttribute().isNumeric()) {
        values[i++] = classifier.classifyInstance(instance);
      } else {
        double[] dist = classifier.distributionForInstance(instance);
        for (int j = 0; j < dist.length; j++) {
          values[i++] = dist[j];
        }
      }
    }
    values[i] = instance.classValue();
    metaInstance = new DenseInstance(1, values);
    metaInstance.setDataset(m_MetaFormat);
    return metaInstance;
  }

  /**
   * Makes a set of level-1 instances from the given instances. More efficient if at least one base classifier
   * implements efficient batch prediction. Requires all base classifiers to implement BatchPredictor.
   *
   * @param instances the instances to be transformed
   * @return the level-1 instances
   * @throws Exception if the instance generation fails
   */
  protected Instances metaInstances(Instances instances) throws Exception {

    double[][][] predictions = new double[m_Classifiers.length][][];
    for (int k = 0; k < m_Classifiers.length; k++) {
      predictions[k] = ((BatchPredictor) getClassifier(k)).distributionsForInstances(instances);
    }

    Instances metaData = new Instances(m_MetaFormat, 0);
    for (int l = 0; l < instances.numInstances(); l++) {
      double[] values = new double[m_MetaFormat.numAttributes()];
      int i = 0;
      for (int k = 0; k < m_Classifiers.length; k++) {
        if (m_BaseFormat.classAttribute().isNumeric()) {
          values[i++] = predictions[k][l][0];
        } else {
          System.arraycopy(predictions[k][l], 0, values, i, predictions[k][l].length);
          i += predictions[k][l].length;
        }
      }
      values[i] = instances.instance(l).classValue();
      metaData.add(new DenseInstance(1, values));
    }
    return metaData;
  }

  @Override
  public void preExecution() throws Exception {
    super.preExecution();
    if (getMetaClassifier() instanceof CommandlineRunnable) {
      ((CommandlineRunnable) getMetaClassifier()).preExecution();
    }
  }

  @Override
  public void postExecution() throws Exception {
    super.postExecution();
    if (getMetaClassifier() instanceof CommandlineRunnable) {
      ((CommandlineRunnable) getMetaClassifier()).postExecution();
    }
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   *             -t training file [-T test file] [-c class index]
   */
  public static void main(String[] argv) {
    runClassifier(new StackingC(), argv);
  }
}
