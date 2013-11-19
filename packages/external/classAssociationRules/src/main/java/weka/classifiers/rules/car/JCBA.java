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
 *    JCBA.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */
package weka.classifiers.rules.car;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import weka.associations.AbstractAssociator;
import weka.associations.Associator;
import weka.associations.CARuleMiner;
import weka.associations.classification.CrNode;
import weka.associations.classification.JCBAPruning;
import weka.classifiers.rules.car.utils.Stopwatch;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

/**
 * Class implemting a java version of the CBA algorithm using a CrTree.
 * Alternatively the classifier can be used as a standard decision list
 * classifier.
 * 
 * The Tree Structure is described in: W. Li, J. Han, J.Pei: CMAR: Accurate and
 * Efficient Classification Based on Multiple Class-Association Rules. In
 * ICDM'01:369-376,2001.
 * 
 * The CBA algorithm is described in: B. Liu, W. Hsu, Y. Ma: Integrating
 * Classification and Association Rule Mining. In KDD'98:80-86,1998.
 * 
 * Valid options are:
 * <p>
 * 
 * -A Class Association Rule Miner String <br>
 * Class Association Rule Miner String should contain the full class name of a
 * scheme included for selection followed by options to the Class Association
 * Rule Miner.
 * <p>
 * 
 * -C confidence value <br>
 * Sets the confidence value for the optional pessimistic-error-rate-based
 * pruning (default 0.25).
 * <p>
 * 
 * -E <br>
 * If set the optional pessimistic-error-rate-based pruning is enabled.
 * <p>
 * 
 * -N <br>
 * If set the optional and the obligatory pruning step are disabled. Classifier
 * behaves like a standard decision list classifier.
 * <p>
 * 
 * -V <br>
 * If set the mined rule set is printed out as well.
 * <p>
 * 
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public class JCBA extends CarClassifier implements OptionHandler,
  AdditionalMeasureProducer, CapabilitiesHandler, TechnicalInformationHandler {

  /** For serialization */
  static final long serialVersionUID = -4039170500265315498L;

  /** The pruning algorithm. */
  private JCBAPruning m_prune = new weka.associations.classification.JCBAPruning();

  /** The mining algorithm. */
  private CARuleMiner m_assoc = new weka.associations.Apriori();

  /** The instances. */
  private Instances m_instances;

  /** Flag indicating if classification rules should be part of the output. */
  private boolean m_outputTree = false;

  /**
   * Flag indicating if classifier is CBA or a standard decision list
   * classifier.
   */
  private boolean m_CBA = true;

  /** Flag turning on/off optional pessimistic-error-rate-based pruning. */
  private boolean m_optPruning = false;

  /**
   * The confidence value for the optional pessimistic-error-rate-based pruning
   * .
   */
  private float m_cf = (float) 0.25;

  /** The metric type string. */
  private String m_metricType;

  /** Watch for mining runtime behaviour. */
  private Stopwatch m_miningWatch;

  /** Watch for pruning runtime behaviour. */
  private Stopwatch m_pruningWatch;

  /** The mining time. */
  private double m_miningTime;

  /** The pruning time. */
  private double m_pruningTime;

  /** The filter to discretise attribute values. */
  private Discretize m_filter;

  /**
   * Constructor
   * 
   */
  /*
   * public JCBA(){ m_filter = new Discretize(); }
   */

  /**
   * Returns default capabilities of the base associator.
   * 
   * @return the capabilities of the base classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getCarMiner() != null && getCarMiner() instanceof CapabilitiesHandler) {
      result = ((CapabilitiesHandler) getCarMiner()).getCapabilities();
      result.disable(Capability.NO_CLASS);
    } else {
      result = new Capabilities(this);
      result.disableAll();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    result.setOwner(this);

    return result;
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
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Bing Liu and Wynne Hsu and Yiming Ma");
    result.setValue(Field.TITLE,
      "Integrating Classification and Association Rule Mining");
    result.setValue(Field.BOOKTITLE,
      "Fourth International Conference on Knowledge Discovery and Data Mining");
    result.setValue(Field.YEAR, "1998");
    result.setValue(Field.PAGES, "80-86");
    result.setValue(Field.PUBLISHER, "AAAI Press");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "W. Li and J. Han and J.Pei");
    additional
      .setValue(
        Field.TITLE,
        "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules");
    additional.setValue(Field.BOOKTITLE, "ICDM'01");
    additional.setValue(Field.YEAR, "2001");
    additional.setValue(Field.PAGES, "369-376");
    // additional.setValue(Field.PUBLISHER, "AAAI Press");

    return result;
  }

  /**
   * Gets a description of the JCBA algorithm
   * 
   * @return a description of the JCBA algorithm
   */
  public String globalInfo() {

    return "A Java implementation of the CBA algorithm. The classifier works with class association rules."
      + "\nThat are association rules where exclusively one class attribute-value-pair is allowed in the consequence."
      + "\nThe algorithm works as a decision list classifier and has an obligatory and an optional pruning step"
      + "\nBoth steps can be disbaled. If both are disbaled it works like a unpruned decision list und uses the first rule that covers a test instance for prediction. "
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration listOptions() {

    FastVector newVector = new FastVector(5);

    newVector.addElement(new Option(
      "\tFull class name of class association rule miner to include, followed\n"
        + "\tby scheme options.\n"
        + "\tMust produce class association rules.\n"
        + "\teg: \"weka.associations.Apriori\"", "A", 1,
      "-A <class association rule miner specification>"));

    newVector
      .addElement(new Option(
        "\t Enables CBA's optional pruning step (pessimistic-error-rate-based pruning) (default: no).\n\tDefault confidence value is 0.25\n",
        "E", 0, "-E"));

    newVector
      .addElement(new Option(
        "\t If set class association rules are also part of the output(default no).\n",
        "V", 0, "-V"));

    newVector
      .addElement(new Option(
        "\t If set CBA's obligatory and optional pruning steps are both turned off (default: CBA's obligatory pruning step).\n",
        "N", 0, "-N"));

    newVector
      .addElement(new Option(
        "\tSets the confidence value for pessimistic-error-rate-based pruning (default: 0.25).\n",
        "C", 1, "-C"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * 
   * -A Class Association Rule Miner String <br>
   * Class Association Rule Miner String should contain the full class name of a
   * scheme included for selection followed by options to the Class Association
   * Rule Miner.
   * <p>
   * 
   * -C confidence value <br>
   * Sets the confidence value for the optional pessimistic-error-rate-based
   * pruning (default 0.25).
   * 
   * -E <br>
   * If set the optional pessimistic-error-rate-based pruning is enabled.
   * <p>
   * 
   * -N <br>
   * If set the optional and the obligatory pruning step are disabled.
   * Classifier behaves like a standard decision list classifier.
   * <p>
   * 
   * -V <br>
   * If set the mined rule set is printed out as well.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String assocString = Utils.getOption('A', options);
    if (assocString.length() > 0) {
      // throw new Exception("class association rule miner must be specified");

      String[] assocSpec = Utils.splitOptions(assocString);

      if (assocSpec.length == 0) {
        throw new Exception(
          "Invalid class association rule miner specification string");
      }
      String assocName = assocSpec[0];
      assocSpec[0] = "";
      Associator toUse = (AbstractAssociator.forName(assocName, assocSpec));
      if (!(toUse instanceof CARuleMiner)) {
        throw new Exception(
          "Association Rule Miner has to be able to mine classification association rules");
      }
      setCarMiner((CARuleMiner) toUse);
    }

    m_outputTree = Utils.getFlag('V', options);

    m_optPruning = Utils.getFlag('E', options);
    String optPruning = Utils.getOption('C', options);
    if (optPruning.length() != 0) {
      m_cf = Float.parseFloat(optPruning);
    }

    if (Utils.getFlag('N', options)) {
      m_CBA = false;
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    String[] options = new String[10];
    int current = 0;

    options[current++] = "-A";
    options[current++] = "" + getAssocSpec();

    if (m_outputTree) {
      options[current++] = "-V";
    }

    if (m_optPruning) {
      options[current++] = "-E ";
      options[current++] = "-C " + m_cf;
    }
    if (!m_CBA) {
      options[current++] = "-N";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets the options for the class association rule miner
   * 
   * @return string with the options
   */
  protected String getAssocSpec() {

    if (m_assoc instanceof OptionHandler) {
      return m_assoc.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) m_assoc).getOptions());
    }
    return m_assoc.getClass().getName();
  }

  /**
   * Gets the tipText for this option.
   * 
   * @return the tipText for this option.
   */
  public String carMinerTipText() {

    return "The class association rule miner with its options.";
  }

  /**
   * Sets the class association rule miner
   * 
   * @param assoc the class association rule miner
   */
  public void setCarMiner(CARuleMiner assoc) {

    m_assoc = assoc;
  }

  /**
   * Gets the class association rule miner
   * 
   * @return the class association rule miner
   */
  public CARuleMiner getCarMiner() {

    return m_assoc;
  }

  /**
   * Gets the pruning algorithm: JCBAPruning
   * 
   * @return the pruning algorithm
   */
  public JCBAPruning getPrune() {

    return m_prune;
  }

  /**
   * Gets the tipText for the specified option.
   * 
   * @return the tipText for the specified option.
   */
  public String optPruningTipText() {

    return "Enables or disbaled the optional pessimistic-error-rate-based pruning step";
  }

  /**
   * Sets optional pruning
   * 
   * @param flag boolean flag
   */
  public void setOptPruning(boolean flag) {

    m_optPruning = flag;
  }

  /**
   * Gets whether or not optional pruning is turned on
   * 
   * @return true if optional pruning is turned on, false otherwise.
   */
  public boolean getOptPruning() {

    return m_optPruning;
  }

  /**
   * Gets the tipText for the specified option.
   * 
   * @return the tipText for the specified option.
   */
  public String CFTipText() {

    return "Sets the confidence value for the optional pessimistic-error-rate-based pruning step.";
  }

  /**
   * Sets the confidence value for pessimistic-error-rate-based pruning
   * 
   * @param value the confidence value
   */
  public void setCF(float value) {

    m_cf = value;
  }

  /**
   * Gets the confidence value for pessimistic-error-rate-based pruning
   * 
   * @return the confidence value
   */
  public float getCF() {

    return m_cf;
  }

  /**
   * Gets the tipText for the specified option.
   * 
   * @return the tipText for the specified option.
   */
  public String treeOutputTipText() {

    return "Enables/Disables the output of the mined rule set.";
  }

  /**
   * Sets whether or not mined rule set is part of the output
   * 
   * @param flag boolean flag
   */
  public void setTreeOutput(boolean flag) {

    m_outputTree = flag;
  }

  /**
   * Gets whether or not the mined rule set is part of the output
   * 
   * @return true if mined rule set is part of the output, false otherwise
   */
  public boolean getTreeOutput() {

    return m_outputTree;
  }

  /**
   * Gets the tipText for the specified option.
   * 
   * @return the tipText for the specified option.
   */
  public String CBATipText() {

    return "If set to false, a simple decision list classification without pruning (no opt. and no obligatory pruning step) is performed,"
      + "otherwise JCBA performs at least its obligatory pruning step.";
  }

  /**
   * Sets whether or not CBA or a standard decision list classifier is used
   * 
   * @param flag true if CBA is used, false otherwise
   */
  public void setCBA(boolean flag) {

    m_CBA = flag;
  }

  /**
   * Gets whether or not CBA or a standard decision list classifier is used
   * 
   * @return true if CBA is used, false otherwise
   */
  public boolean getCBA() {

    return m_CBA;
  }

  /**
   * Generates the classifier.
   * 
   * @param newInstances set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  @Override
  public void buildClassifier(Instances newInstances) throws Exception {

    ArrayList[] allRules = { new ArrayList(), new ArrayList(), new ArrayList() };
    Instances instancesNoClass;
    Instances instancesOnlyClass;

    boolean needsDisc = false;
    for (int i = 0; i < newInstances.numAttributes(); i++) {
      if (i != newInstances.classIndex()
        && newInstances.attribute(i).isNumeric()) {
        needsDisc = true;
        break;
      }
    }

    Instances instances = newInstances;
    if (needsDisc) {
      m_filter = new Discretize();
      m_filter.setInputFormat(newInstances);
      instances = Filter.useFilter(newInstances, m_filter);
    } else {
      m_filter = null;
    }
    m_instances = instances;
    getCapabilities().testWithFail(instances);

    m_miningWatch = new Stopwatch();
    m_miningWatch.start();
    m_assoc.setClassIndex(m_instances.classIndex() + 1);
    allRules = m_assoc.mineCARs(m_instances);
    instancesNoClass = m_assoc.getInstancesNoClass();
    instancesOnlyClass = m_assoc.getInstancesOnlyClass();
    m_metricType = m_assoc.metricString();
    m_miningTime = m_miningWatch.getSeconds();
    m_miningWatch.stop();
    m_pruningWatch = new Stopwatch();
    m_pruningWatch.start();
    m_prune.setInstancesNoClass(instancesNoClass);
    m_prune.setInstancesOnlyClass(instancesOnlyClass);
    m_prune.setInstances(m_instances);
    m_prune.optPruning(m_optPruning, m_cf);
    m_prune.preprocess(allRules[0], allRules[1], allRules[2]);
    if (m_CBA) {
      if (!(m_prune.isEmpty())) {
        m_prune.prune();
      } else {
        m_prune.setDefaultClass(-1);
      }
    } else {
      m_prune.setDefaultClass(-1);
    }
    m_pruningTime = m_pruningWatch.getSeconds();
    m_pruningWatch.stop();
    m_instances = new Instances(instances, 0);

  }

  /**
   * Classifies an instance
   * 
   * @param instance the instance
   * @throws Exception exception if instance cannot be classified
   * @return the predicted class
   */
  @Override
  public double classifyInstance(Instance instance) throws Exception {

    boolean checkEquals = false;
    FastVector list = m_prune.getPrecedenceList(), classify;
    int stop = m_prune.getStopIndex(), attNum, attValue, size;

    classify = sortAttributes(m_instances, instance, m_prune);
    size = classify.size();

    if (m_prune.isEmpty()) {
      return m_prune.getDefaultClass();
    }

    for (int j = 0; j <= stop; j++) {
      CrNode start = (CrNode) list.elementAt(j);
      int index = size - 1;
      do {
        do {
          attNum = ((Integer) classify.elementAt(index - 1)).intValue();
          attValue = ((Integer) classify.elementAt(index)).intValue();
          if (!instance.isMissing(attNum) && start.equals(attNum, attValue)) {
            checkEquals = true;
          }
          index = index - 2;
        } while (index > 0 && (!(start.equals(attNum, attValue))));
        if (checkEquals) {
          while (start.getTreeParent() == null) {
            start = start.getLastSibling();
          }
          start = start.getTreeParent();
          if (start == m_prune.getRoot()) {
            return m_prune.getClassValue(j);
          }
          checkEquals = false;
        } else {
          start = null;
        }
      } while (start != null && index > 0);

    }
    return m_prune.getDefaultClass();
  }

  /**
   * Returns the class distribution for an instance
   * 
   * @param newInstance the instance
   * @throws Exception exception if it cannot be calculated
   * @return the class distribution
   */
  @Override
  public double[] distributionForInstance(Instance newInstance)
    throws Exception {

    boolean checkEquals = false;
    double[] classDistribution = new double[m_instances.numClasses()];
    FastVector list = m_prune.getPrecedenceList(), classify;
    int stop = m_prune.getStopIndex(), attNum, attValue, size;

    Instance instance = newInstance;

    if (m_filter != null) {
      if (m_filter.numPendingOutput() > 0) {
        throw new Exception("Filter output queue not empty!");
      }

      if (!m_filter.input(newInstance)) {
        throw new Exception("Filter didn't make the test instance"
          + " immediately available!");
      }

      m_filter.batchFinished();
      instance = m_filter.output();
    }

    classify = sortAttributes(m_instances, instance, m_prune);
    size = classify.size();

    for (int i = 0; i < classDistribution.length; i++) {
      classDistribution[i] = 0;
    }

    if (m_prune.isEmpty()) {
      classDistribution[m_prune.getDefaultClass()] = 1;
      return classDistribution;
    }
    for (int j = 0; j <= stop; j++) {
      CrNode start = (CrNode) list.elementAt(j);
      int index = size - 1;
      do {
        do {
          attNum = ((Integer) classify.elementAt(index - 1)).intValue();
          attValue = ((Integer) classify.elementAt(index)).intValue();
          if (!instance.isMissing(attNum) && start.equals(attNum, attValue)) {
            checkEquals = true;
          }
          index = index - 2;
        } while (index > 0 && (!(start.equals(attNum, attValue))));
        if (checkEquals) {
          while (start.getTreeParent() == null) {
            start = start.getLastSibling();
          }
          start = start.getTreeParent();
          if (start == m_prune.getRoot()) {
            classDistribution[m_prune.getClassValue(j)]++;
            return classDistribution;
          }
          checkEquals = false;
        } else {
          start = null;
        }
      } while (start != null && index > 0);
    }
    classDistribution[m_prune.getDefaultClass()] = 1;
    return classDistribution;
  }

  /**
   * Does the intermediate classification step during the CBA's obligatory
   * pruning
   * 
   * @param instance the instance to classifiy
   * @param tree the actual CrTree with the actual rule set
   * @param instances the instances
   * @throws Exception exception if instance cannot be classified
   * @return the predicted class label
   */
  public double intermediateClassificationForInstance(Instance instance,
    JCBAPruning tree, Instances instances) throws Exception {

    m_prune = tree;
    m_instances = instances;
    return classifyInstance(instance);
  }

  /**
   * Prints the rules
   * 
   * @return the rules
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();
    if (m_outputTree) {
      text.append(m_assoc.toString());
      text.append("\n\n");
    }
    text.append(m_prune.toString(m_metricType));
    text.append("Mining Time in sec.: " + measureMiningTime() + "\n");
    text.append("Pruning Time in sec. : " + measurePruningTime());
    text.append("\n");
    return text.toString();
  }

  /**
   * Lists all additional statistics that are available
   * 
   * @return list of the names of the additional measures
   */
  @Override
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(5);
    newVector.addElement("measureMiningTime");
    newVector.addElement("measurePruningTime");
    newVector.addElement("measureNumMinedRules");
    newVector.addElement("measureNumRulesAfterOptPruning");
    newVector.addElement("measureNumClassRules");
    return newVector.elements();
  }

  /**
   * Gets the additional statistics
   * 
   * @param additionalMeasureName the name of the additional measure
   * @return the value of the measure
   */
  @Override
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareTo("measureMiningTime") == 0) {
      return measureMiningTime();
    } else if (additionalMeasureName.compareTo("measurePruningTime") == 0) {
      return measurePruningTime();
    } else if (additionalMeasureName.compareTo("measureNumMinedRules") == 0) {
      return measureNumMinedRules();
    } else if (additionalMeasureName
      .compareTo("measureNumRulesAfterOptPruning") == 0) {
      return measureNumPrunedRules();
    } else if (additionalMeasureName.compareTo("measureNumClassRules") == 0) {
      return measureNumClassRules();
    } else {
      throw new IllegalArgumentException(additionalMeasureName
        + " not supported (CBA)");
    }
  }

  /**
   * Gets the mining time
   * 
   * @return the mining time in seconds
   */
  public double measureMiningTime() {

    return m_miningTime;
  }

  /**
   * Gets the pruning time in seconds
   * 
   * @return the pruning time in seconds
   */
  public double measurePruningTime() {

    return m_pruningTime;
  }

  /**
   * Gets the number of mined rules
   * 
   * @return the number of mined rules
   */
  public double measureNumMinedRules() {

    return m_prune.numMinedRules();
  }

  /**
   * Gets the number of rules after the optional pruning step
   * 
   * @return the number of rules after the optional pruning step
   */
  public double measureNumPrunedRules() {

    return m_prune.numPrunedRules();
  }

  /**
   * Gets the number of rules used for classification
   * 
   * @return the number of rules used for classification
   */
  public double measureNumClassRules() {

    return m_prune.numClassRules();
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
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new JCBA(), argv);
  }
}
