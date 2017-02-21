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
 * TLC.java
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.mi;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.meta.LogitBoost;
import weka.classifiers.trees.J48;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.PartitionGenerator;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.PartitionMembership;
import weka.filters.unsupervised.attribute.MultiInstanceWrapper;
import weka.filters.unsupervised.attribute.Remove;

/**
 * <!-- globalinfo-start --> Implements basic two-level classification method
 * for multi-instance data, without attribute selection.<br>
 * <br>
 * For more information see:<br>
 * <br>
 * Nils Weidmann, Eibe Frank, Bernhard Pfahringer: A two-level learning method
 * for generalized multi-instance problems. In: Fourteenth European Conference
 * on Machine Learning, 468-479, 2003.
 * <br>
 * Eibe Frank and Bernhard Pfahringer: Propositionalisation of Multi-instance Data Using Random Forests.
 * In: AI 2013: Advances in Artificial Intelligence, 362-373, 2013.
 * <p>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Weidmann2003,
 *    author = {Nils Weidmann and Eibe Frank and Bernhard Pfahringer},
 *    booktitle = {Fourteenth European Conference on Machine Learning},
 *    pages = {468-479},
 *    publisher = {Springer},
 *    title = {A two-level learning method for generalized multi-instance problems},
 *    year = {2003}
 * }
 * </pre>
 * <pre>
 * &#64;inproceedings{FrankAndPfahringer203,
 *    author = {Eibe Frank and Bernhard Pfahringer},
 *    booktitle = {AI 2013: Advances in Artificial Intelligence},
 *    pages = {362-373},
 *    publisher = {Springer},
 *    title = {Propositionalisation of Multi-instance Data Using Random Forests},
 *    year = {2013}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -P "&lt;name and options of partition generator&gt;"
 *  Partition generator to use, including options.
 *  Quotes are needed when options are specified.
 *  (default: weka.classifiers.trees.J48)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.meta.LogitBoost)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.meta.LogitBoost:
 * </pre>
 * 
 * <pre>
 * -Q
 *  Use resampling instead of reweighting for boosting.
 * </pre>
 * 
 * <pre>
 * -P &lt;percent&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)
 * </pre>
 * 
 * <pre>
 * -F &lt;num&gt;
 *  Number of folds for internal cross-validation.
 *  (default 0 -- no cross-validation)
 * </pre>
 * 
 * <pre>
 * -R &lt;num&gt;
 *  Number of runs for internal cross-validation.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -L &lt;num&gt;
 *  Threshold on the improvement of the likelihood.
 *  (default -Double.MAX_VALUE)
 * </pre>
 * 
 * <pre>
 * -H &lt;num&gt;
 *  Shrinkage parameter.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * Options specific to partition generator weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre>
 * -U
 *  Use unpruned tree.
 * </pre>
 * 
 * <pre>
 * -O
 *  Do not collapse tree.
 * </pre>
 * 
 * <pre>
 * -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)
 * </pre>
 * 
 * <pre>
 * -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)
 * </pre>
 * 
 * <pre>
 * -R
 *  Use reduced error pruning.
 * </pre>
 * 
 * <pre>
 * -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)
 * </pre>
 * 
 * <pre>
 * -B
 *  Use binary splits only.
 * </pre>
 * 
 * <pre>
 * -S
 *  Don't perform subtree raising.
 * </pre>
 * 
 * <pre>
 * -L
 *  Do not clean up after the tree has been built.
 * </pre>
 * 
 * <pre>
 * -A
 *  Laplace smoothing for predicted probabilities.
 * </pre>
 * 
 * <pre>
 * -J
 *  Do not use MDL correction for info gain on numeric attributes.
 * </pre>
 * 
 * <pre>
 * -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@.cs.waikato.ac.nz)
 * @version $Revision$
 */
public class TLC extends SingleClassifierEnhancer implements
  TechnicalInformationHandler, MultiInstanceCapabilitiesHandler {

  /** For serialization */
  private static final long serialVersionUID = -4444591375578585231L;

  /** The partition generator to use. */
  protected PartitionGenerator m_partitionGenerator = new J48();

  /** The filter to use in conjunction with the partition generator. */
  protected MultiFilter m_Filter = null;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Implements basic two-level classification method for multi-instance data"
      + ", without attribute selection.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
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

    TechnicalInformation result, additional;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR,
      "Nils Weidmann and Eibe Frank and Bernhard Pfahringer");
    result.setValue(Field.TITLE,
      "A two-level learning method for generalized multi-instance problems");
    result.setValue(Field.BOOKTITLE,
      "Fourteenth European Conference on Machine Learning");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.PAGES, "468-479");
    result.setValue(Field.PUBLISHER, "Springer");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(TechnicalInformation.Field.AUTHOR, "Eibe Frank and Bernhard Pfahringer");
    additional.setValue(TechnicalInformation.Field.TITLE,
            "Propositionalisation of Multi-instance Data Using Random Forests");
    additional.setValue(TechnicalInformation.Field.BOOKTITLE, "AI 2013: Advances in Artificial Intelligence");
    additional.setValue(TechnicalInformation.Field.YEAR, "2013");
    additional.setValue(TechnicalInformation.Field.PUBLISHER, "Springer");
    additional.setValue(TechnicalInformation.Field.PAGES, "362-373");

    return result;
  }

  /**
   * Constructor that sets default base learner.
   */
  public TLC() {

    m_Classifier = new LogitBoost();
  }

  /**
   * String describing default classifier.
   */
  @Override
  protected String defaultClassifierString() {

    return "weka.classifiers.meta.LogitBoost";
  }

  /**
   * Returns a description of this option suitable for display as a tip text in
   * the gui.
   * 
   * @return description of this option
   */
  public String partitionGeneratorTipText() {

    return "The partition generator that will generate membership values for the instances.";
  }

  /**
   * Set the generator for use in filtering
   * 
   * @param newPartitionGenerator the generator to use
   */
  public void setPartitionGenerator(PartitionGenerator newPartitionGenerator) {

    m_partitionGenerator = newPartitionGenerator;
  }

  /**
   * Get the generator used by this filter
   * 
   * @return the generator used
   */
  public PartitionGenerator getPartitionGenerator() {

    return m_partitionGenerator;
  }

  /**
   * Gets the partition generator specification string, which contains the class
   * name of the partition generator and any options to the partition generator.
   * 
   * @return the filter string.
   */
  protected String getPartitionGeneratorSpec() {

    PartitionGenerator c = getPartitionGenerator();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.addElement(new Option(
      "\tPartition generator to use, including options.\n"
        + "\tQuotes are needed when options are specified.\n"
        + "\t(default: weka.classifiers.trees.J48)", "P", 1,
      "-P \"<name and options of partition generator>\""));

    newVector.addAll(Collections.list(super.listOptions()));

    newVector.addElement(new Option("", "", 0,
      "\nOptions specific to partition generator "
        + getPartitionGenerator().getClass().getName() + ":"));

    newVector.addAll(Collections.list(((OptionHandler) getPartitionGenerator())
      .listOptions()));

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
   * -P "&lt;name and options of partition generator&gt;"
   *  Partition generator to use, including options.
   *  Quotes are needed when options are specified.
   *  (default: weka.classifiers.trees.J48)
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.meta.LogitBoost)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.meta.LogitBoost:
   * </pre>
   * 
   * <pre>
   * -Q
   *  Use resampling instead of reweighting for boosting.
   * </pre>
   * 
   * <pre>
   * -P &lt;percent&gt;
   *  Percentage of weight mass to base training on.
   *  (default 100, reduce to around 90 speed up)
   * </pre>
   * 
   * <pre>
   * -F &lt;num&gt;
   *  Number of folds for internal cross-validation.
   *  (default 0 -- no cross-validation)
   * </pre>
   * 
   * <pre>
   * -R &lt;num&gt;
   *  Number of runs for internal cross-validation.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -L &lt;num&gt;
   *  Threshold on the improvement of the likelihood.
   *  (default -Double.MAX_VALUE)
   * </pre>
   * 
   * <pre>
   * -H &lt;num&gt;
   *  Shrinkage parameter.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * Options specific to partition generator weka.classifiers.trees.J48:
   * </pre>
   * 
   * <pre>
   * -U
   *  Use unpruned tree.
   * </pre>
   * 
   * <pre>
   * -O
   *  Do not collapse tree.
   * </pre>
   * 
   * <pre>
   * -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)
   * </pre>
   * 
   * <pre>
   * -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)
   * </pre>
   * 
   * <pre>
   * -R
   *  Use reduced error pruning.
   * </pre>
   * 
   * <pre>
   * -N &lt;number of folds&gt;
   *  Set number of folds for reduced error
   *  pruning. One fold is used as pruning set.
   *  (default 3)
   * </pre>
   * 
   * <pre>
   * -B
   *  Use binary splits only.
   * </pre>
   * 
   * <pre>
   * -S
   *  Don't perform subtree raising.
   * </pre>
   * 
   * <pre>
   * -L
   *  Do not clean up after the tree has been built.
   * </pre>
   * 
   * <pre>
   * -A
   *  Laplace smoothing for predicted probabilities.
   * </pre>
   * 
   * <pre>
   * -J
   *  Do not use MDL correction for info gain on numeric attributes.
   * </pre>
   * 
   * <pre>
   * -Q &lt;seed&gt;
   *  Seed for random data shuffling (default 1).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after the -- are passed on to the clusterer.
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    // Set partition generator specification
    String partitionGeneratorString = Utils.getOption('P', options);
    if (partitionGeneratorString.length() > 0) {
      String[] partitionGeneratorSpec = Utils
        .splitOptions(partitionGeneratorString);
      if (partitionGeneratorSpec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid partition generator specification string");
      }
      String partitionGeneratorName = partitionGeneratorSpec[0];
      partitionGeneratorSpec[0] = "";
      setPartitionGenerator((PartitionGenerator) Utils.forName(
        PartitionGenerator.class, partitionGeneratorName,
        partitionGeneratorSpec));
    } else {
      setPartitionGenerator(new J48());
    }
    super.setOptions(options);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-P");
    options.add("" + getPartitionGeneratorSpec());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Builds the classifier from the given training data.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    data = new Instances(data);
    data.deleteWithMissingClass();

    getCapabilities().testWithFail(data);

    m_Filter = new MultiFilter();
    Filter[] twoFilters = new Filter[2];
    PartitionMembership pm = new PartitionMembership();
    pm.setPartitionGenerator(getPartitionGenerator());
    MultiInstanceWrapper miw = new MultiInstanceWrapper();
    miw.setFilter(pm);
    twoFilters[0] = miw;
    twoFilters[1] = new Remove();
    ((Remove) twoFilters[1]).setAttributeIndices("1");
    m_Filter.setFilters(twoFilters);
    m_Filter.setInputFormat(data);
    Instances propositionalData = Filter.useFilter(data, m_Filter);

    // can classifier handle the data?
    getClassifier().getCapabilities().testWithFail(propositionalData);

    m_Classifier.buildClassifier(propositionalData);
  }

  /**
   * Returns a description of the classifier as a string.
   */
  @Override
  public String toString() {

    if (m_Classifier == null) {
      return "Classifier not built yet.";
    }
    return "Partition Generator:\n\n" + getPartitionGenerator().toString()
      + "\n\nClassifier:\n\n" + getClassifier().toString();
  }

  /**
   * Returns class probabilities for the given instance.
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    inst = (Instance) inst.copy();
    m_Filter.input(inst);
    m_Filter.batchFinished();
    return m_Classifier.distributionForInstance(m_Filter.output());
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {

    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.disableAllAttributes();
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    if (super.getCapabilities().handles(Capability.NOMINAL_CLASS)) {
      result.enable(Capability.NOMINAL_CLASS);
    }
    if (super.getCapabilities().handles(Capability.BINARY_CLASS)) {
      result.enable(Capability.BINARY_CLASS);
    }
    result.enable(Capability.MISSING_CLASS_VALUES);

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance filter for the relational
   * data (i.e., the bags).
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {

    Capabilities result = m_partitionGenerator.getCapabilities();
    result.enable(Capability.NO_CLASS);

    // other
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Main method for running this class from the command-line.
   */
  public static void main(String[] options) {

    runClassifier(new TLC(), options);
  }
}
