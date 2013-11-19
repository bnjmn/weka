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
 *    WeightedClassifier.java
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
import weka.associations.classification.CrTree;
import weka.associations.classification.PrecedencePruning;
import weka.associations.classification.PruneCAR;
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
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

/**
 * Class implemting three different weighted classifiers for class association
 * rules. a) all rules are weighted equally with weight one (default behaviour).
 * b) all rules are weighted linearly c) all rules are weighted using the
 * inverse function 1/position in sort order of mining algorithm
 * 
 * The pruning step is omitted. All mined rules are used for classifcation, if
 * not specified otherwise with the -L option.
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
 * -W weighting scheme <br>
 * Sets the weighting scheme: inverse|linear|equal (default).
 * <p>
 * 
 * -L rule limit<br>
 * Set a rule limit for the number of rules used for classification after the
 * pruning step. Uses the first N rules only.
 * <p>
 * 
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public class WeightedClassifier extends CarClassifier implements OptionHandler,
  AdditionalMeasureProducer, CapabilitiesHandler {

  /** For serialization */
  static final long serialVersionUID = -7484303771900257946L;

  /** The pruning algorithm. */
  private final PrecedencePruning m_prune = new weka.associations.classification.PrecedencePruning();

  /** The mining algorithm. */
  // private CARuleMiner m_assoc = new
  // weka.associations.classification.CarApriori();
  private CARuleMiner m_assoc = new weka.associations.Apriori();

  /** The instances. */
  private Instances m_instances;

  protected static final int EQUAL = 0;
  protected static final int INVERSE = 1;
  protected static final int LINEAR = 2;

  public static final Tag[] TAGS_SELECTION = { new Tag(EQUAL, "Equal"),
    new Tag(INVERSE, "Inverse"), new Tag(LINEAR, "Linear") };

  /** The type string for the weighting scheme. */
  // private String m_weightScheme = "equal";
  private int m_weightScheme = 0;

  /** The metric type string. */
  private String m_metricType;

  /** Watch for mining runtime behaviour. */
  private Stopwatch m_miningWatch;

  /** Watch for pruning runtime behaviour. */
  private Stopwatch m_pruningWatch;

  /** The number of mined rules. */
  private int m_numMinedRules;

  /** The number of rules after pruning. */
  private int m_numPrunedRules;

  /**
   * The limit for the number of classification rules after pruning. -1
   * indicates no limit. Valid integers are >= 1.
   */
  private int m_numRules = -1;

  /** The number of rules for classification. */
  private int m_stop;

  /** The mining time. */
  private double m_miningTime;

  /** The filter to discretise attribute values. */
  private Discretize m_filter;

  /** The pruning time. */
  private double m_pruningTime;

  /**
   * The average rank of the first rule that covers an instance and predicts it
   * correctly.
   */
  private double m_averageCorrect;

  /** The average rank of the first rule that covers an instance. */
  private double m_averageFirst;

  /**
   * The sum of the rank of the first rule that covers an instance and predicts
   * it correctly.
   */
  private double m_sumCorrect;

  /** The sum of the rank of the first rule that covers an instance. */
  private double m_sumFirst;

  /** The number of instances. */
  private int m_numInstances;

  /** The number of test instances. */
  private int m_numTests;

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
   * Gets a description of WeightedClassifier
   * 
   * @return a description of WeightedClassifier
   */
  public String globalInfo() {

    return "Classifier that weights a set of class association rules. That are association rules where exclusively one class attribute-value-pair is allowed in the consequence."
      + "\nNo pruning is perfomed (except that the number of mined rules can be limited and from that set the user can choose to consider only the top ranked rules with the -L option). For classification there are three options:"
      + "\na) all rules are equally weighted with 1 (default)"
      + "\nb) all rules are linearly weighted in a decreasing manner according to the sort order of the mining algorithm"
      + "\nc) all rules are weighted with the inverse function 1/rank in sort order.";
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration listOptions() {

    FastVector newVector = new FastVector(3);

    newVector.addElement(new Option(
      "\tFull class name of class association rule miner to include, followed\n"
        + "\tby scheme options.\n"
        + "\teg: \"weka.associations.classification.CarApriori -N 100\"", "A",
      1, "-A <class association rule miner specification>"));

    newVector.addElement(new Option(
      "\tThe type of the weighting function: inverse | linear | equal (default)\n"
        + "\teg: -T linear", "W", 1, "-W <weighting scheme>"));
    newVector
      .addElement(new Option(
        "\tThe number of rules used for classification (default: all left after pruning) \n",
        "L", 1, "-L <number of rules>"));

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
   * -W weighting scheme <br>
   * Sets the weighting scheme: inverse|linear|equal (default).
   * <p>
   * 
   * -L rule limit<br>
   * Set a rule limit for the number of rules used for classification after the
   * pruning step. Uses the first N rules only.
   * <p>
   * 
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String assocString = Utils.getOption('A', options);
    if (assocString.length() > 0) {
      // throw new Exception("Asociation Rule Miner must be specified");
      // }
      String[] assocSpec = Utils.splitOptions(assocString);
      if (assocSpec.length == 0) {
        throw new Exception(
          "Invalid Asociation Rule Miner specification string");
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

    String ws = Utils.getOption('W', options);
    if (ws.length() > 0) {
      if (ws.equalsIgnoreCase("inverse")) {
        setWeightScheme(new SelectedTag(INVERSE, TAGS_SELECTION));
      } else if (ws.equalsIgnoreCase("linear")) {
        setWeightScheme(new SelectedTag(LINEAR, TAGS_SELECTION));
      } else {
        // default
        setWeightScheme(new SelectedTag(EQUAL, TAGS_SELECTION));
      }
    }
    /*
     * m_weightScheme = Utils.getOption('W',options);
     * if(!m_weightScheme.equalsIgnoreCase("inverse") &&
     * !m_weightScheme.equalsIgnoreCase("linear")) m_weightScheme = "equal";
     */

    String numRulesString = Utils.getOption('L', options);
    if (numRulesString.length() != 0) {
      m_numRules = Integer.parseInt(numRulesString);
    } else {
      m_numRules = -1;
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

    options[current++] = "-L";
    options[current++] = "" + m_numRules;

    options[current++] = "-W";
    options[current++] = "" + getWeightScheme().getSelectedTag().getReadable();

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
   * Gets the pruning algorithm: PrecedencePruning
   * 
   * @return the pruning algorithm
   */
  public PruneCAR getPrune() {

    return m_prune;
  }

  /**
   * Generates the classifier.
   * 
   * @param newInstances set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  @Override
  public void buildClassifier(Instances newInstances) throws Exception {

    ArrayList<Object>[] allRules;
    FastVector limRules1 = new FastVector(), limRules2 = new FastVector(), limRules3 = new FastVector();
    Instances instancesNoClass, instancesOnlyClass;

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

    m_averageCorrect = 0;
    m_averageFirst = 0;
    m_sumCorrect = 0;
    m_sumFirst = 0;
    m_numTests = 0;

    m_numInstances = m_instances.numInstances();
    m_miningWatch = new Stopwatch();
    m_miningWatch.start();
    m_assoc.setClassIndex(m_instances.classIndex() + 1);
    allRules = m_assoc.mineCARs(m_instances);
    // System.out.println(m_assoc);
    m_numMinedRules = allRules[0].size();
    instancesNoClass = m_assoc.getInstancesNoClass();
    instancesOnlyClass = m_assoc.getInstancesOnlyClass();
    m_metricType = m_assoc.metricString();
    m_miningTime = m_miningWatch.getSeconds();
    m_miningWatch.stop();
    m_pruningWatch = new Stopwatch();
    m_pruningWatch.start();
    m_prune.setNoPessPruning(true);
    m_prune.setInstancesNoClass(instancesNoClass);
    m_prune.setInstancesOnlyClass(instancesOnlyClass);
    if (m_numRules > 0 && allRules[0].size() > m_numRules) {
      m_stop = m_numRules;
      for (int i = 0; i < m_stop; i++) {
        limRules1.addElement(allRules[0].get(i));
        limRules2.addElement(allRules[1].get(i));
        limRules3.addElement(allRules[2].get(i));
      }
      m_prune.preprocess(limRules1, limRules2, limRules3);
    } else {
      m_stop = allRules[0].size();
      m_prune.preprocess(allRules[0], allRules[1], allRules[2]);
    }
    if (!(m_prune.isEmpty())) {
      m_prune.prune();
    }
    m_numPrunedRules = m_stop - m_prune.prunedRules();
    m_pruningTime = m_pruningWatch.getSeconds();
    m_pruningWatch.stop();
    m_instances = new Instances(instances, 0);

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

    CrTree pruneTree = new CrTree();
    pruneTree = m_prune;

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

    FastVector treeNodes, classify = sortAttributes(m_instances, instance,
      pruneTree);
    boolean checkEquals = false, pathOk = false;
    int attNum, attValue, size = classify.size();
    boolean firstFound = false;
    FastVector list = m_prune.getPrecedenceList();
    int k = 0;
    m_averageFirst = 0;
    m_averageCorrect = 0;
    m_numTests++;

    // calculate average ranks
    out: while (k < list.size() && !firstFound && size > 0) {
      CrNode start = (CrNode) list.elementAt(k);
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
            m_averageFirst = k + 1;
            int predictedClass = m_prune.getClassValue(k);
            if (predictedClass == (int) instance.value(instance.classIndex())) {
              firstFound = true;
              m_averageCorrect = k + 1;
              break out;
            }
          }
          checkEquals = false;
        } else {
          start = null;
        }
      } while (start != null && index > 0);
      k++;

    }
    if (m_averageCorrect == 0) {
      if ((int) instance.value(instance.classIndex()) == m_prune
        .getDefaultClass()) {
        m_averageCorrect = m_prune.numPrunedRules() + 1;
      } else {
        m_averageCorrect = m_prune.numPrunedRules() + 2;
      }
    }
    if (m_averageFirst == 0) {
      m_averageFirst = m_prune.numPrunedRules() + 1;
    }
    m_sumFirst += m_averageFirst;
    m_sumCorrect += m_averageCorrect;

    // calculate distribution
    double sum = 0;
    double[] classDistribution = new double[m_instances.numClasses()], temp = new double[m_instances
      .numClasses()];
    ;
    CrNode start;

    for (int i = 0; i < classDistribution.length; i++) {
      classDistribution[i] = 0;
      temp[i] = 0;
    }

    if (pruneTree.isEmpty()) {
      classDistribution[pruneTree.getDefaultClass()] = 1;
      return classDistribution;
    }
    treeNodes = new FastVector();
    treeNodes.addElement((pruneTree.getRoot()).getTreeChild());
    while (treeNodes.size() != 0 && size > 0) {
      start = (CrNode) treeNodes.firstElement();
      treeNodes.removeElementAt(0);
      int index = 0;
      do {
        if ((start.getNextSibling()).getTreeParent() == null) {
          treeNodes.addElement(start.getNextSibling());
        }

        do {
          attNum = ((Integer) classify.elementAt(index)).intValue();
          attValue = ((Integer) classify.elementAt(index + 1)).intValue();
          if (!instance.isMissing(attNum) && start.equals(attNum, attValue)) {
            checkEquals = true;
            for (int j = 0; j < (start.getContent()).size(); j++) {
              int classAtt = ((Integer) ((FastVector) (start.getContent())
                .elementAt(j)).firstElement()).intValue();
              temp[classAtt] = temp[classAtt]
                + weight(getWeightScheme().getSelectedTag().getReadable(),
                  ((Double) ((FastVector) (start.getContent()).elementAt(j))
                    .elementAt(3)).doubleValue());
              pathOk = true;
            }
          }
          index = index + 2;
        } while (index < size && (!(start.equals(attNum, attValue))));
        if (checkEquals) {
          start = start.getTreeChild();
          checkEquals = false;
        } else {
          start = null;
        }
      } while (start != null && index < size);
      for (int i = 0; i < temp.length; i++) {
        if (pathOk) {
          classDistribution[i] = classDistribution[i] + temp[i];
          sum = sum + temp[i];
        }
        temp[i] = 0;
      }
      pathOk = false;
    }
    if (sum != 0) {
      Utils.normalize(classDistribution, sum);
    } else {
      classDistribution[pruneTree.getDefaultClass()] = 1;
    }
    return classDistribution;
  }

  /**
   * Gets the tipText for this option.
   * 
   * @return the tipText for this option.
   */
  public String weightSchemeTipText() {

    return "Specify the type of the weighting scheme: equal(default)|linear|invers"
      + "\nEqual weights all instances equally with 1, linear uses a linear weighting function depending on the sort order of the mining algorithm, and"
      + "\ninverse uses the inverse function 1/(rank in the sort order).";
  }

  /**
   * Sets the weighting scheme
   * 
   * @param type the type of the weighting scheme: inverse | linear | equal
   */
  public void setWeightScheme(SelectedTag type) {

    /*
     * m_weightScheme = type; if(!m_weightScheme.equalsIgnoreCase("inverse") &&
     * !m_weightScheme.equalsIgnoreCase("linear")) m_weightScheme = "equal";
     */
    m_weightScheme = type.getSelectedTag().getID();
  }

  /**
   * Gets the type of the weighting scheme
   * 
   * @return type of the weighting scheme: inverse | linear | equal
   */
  public SelectedTag getWeightScheme() {

    // return m_weightScheme;
    return new SelectedTag(m_weightScheme, TAGS_SELECTION);
  }

  /**
   * Gets the tipText for this option.
   * 
   * @return the tipText for this option.
   */
  public String ruleLimitTipText() {

    return "If set to -1 or 0, all rules in the pruned rule set are used for classification (default), otherwise it uses the specified number of top ranked rules in the pruned rule set.";
  }

  /**
   * Sets a rule limit. Only the first n rules after pruning are then used for
   * classification (-1 means no limit).
   * 
   * @param n the rule limit
   */
  public void setRuleLimit(int n) {

    m_numRules = n;
  }

  /**
   * Gets the rule limit (-1 means no limit)
   * 
   * @return the rule limit
   */
  public int getRuleLimit() {

    return m_numRules;
  }

  /**
   * Weights a rule
   * 
   * @return the weight of a rule
   * @param type the weighting scheme
   * @param rank the rank of the rule in the sort order
   */
  public double weight(String type, double rank) {
    if (type.equalsIgnoreCase("inverse")) {
      return 1.0 / (rank + 1);
    }
    if (type.equalsIgnoreCase("linear")) {
      return 1 - ((1.0 / (m_stop + 1)) * rank);
    }
    return 1;
  }

  /**
   * Prints the rules
   * 
   * @return the rules
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();
    text.append(m_prune.toString(m_metricType, (int) measureNumMinedRules()));
    text.append("Mining Time in sec.: " + measureMiningTime() + "\n");
    text.append("Pruning Time in sec. : " + measurePruningTime());
    text.append("\n");
    text.append("The used weighting scheme: ");

    if (getWeightScheme().getSelectedTag().getReadable()
      .equalsIgnoreCase("inverse")) {
      text.append("inverse\n");
    } else {
      if (getWeightScheme().getSelectedTag().getReadable()
        .equalsIgnoreCase("linear")) {
        text.append("linear\n");
      } else {
        text.append("equally weighted with 1\n\n");
      }
    }
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
    newVector.addElement("measureNumRulesAfterAddPruning");
    newVector.addElement("measureNumClassRules");
    newVector.addElement("measureAverageRankFirstRuleFires");
    newVector.addElement("measureAverageRankFirstCorrectRule");
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
      .compareTo("measureNumRulesAfterAddPruning") == 0) {
      return measureNumPrunedRules();
    } else if (additionalMeasureName.compareTo("measureNumClassRules") == 0) {
      return measureNumClassRules();
    } else if (additionalMeasureName
      .compareTo("measureAverageRankFirstRuleFires") == 0) {
      return measureAverageRankFires();
    } else if (additionalMeasureName
      .compareTo("measureAverageRankFirstCorrectRule") == 0) {
      return measureAverageRankCorrect();
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

    return m_numMinedRules;
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

    return m_prune.numPrunedRules();
  }

  /**
   * Gets the average rank of the first rule that covers an instance and
   * predicts it correctly.
   * 
   * @return the average rank of the first rule that covers an instance and
   *         predicts it correctly.
   */
  public double measureAverageRankFires() {

    return m_sumFirst / m_numTests;
  }

  /**
   * Gets the average rank of the first rule that covers an instance.
   * 
   * @return the average rank of the first rule that covers an instance.
   */
  public double measureAverageRankCorrect() {

    return m_sumCorrect / m_numTests;
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
    runClassifier(new WeightedClassifier(), argv);
  }
}
