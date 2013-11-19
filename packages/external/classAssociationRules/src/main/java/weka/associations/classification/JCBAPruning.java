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
 *    CBALikePruning.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations.classification;

import java.io.Serializable;
import java.util.ArrayList;

import weka.associations.ItemSet;
import weka.classifiers.rules.car.JCBA;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Class implemting the pruning step of the CBA algorithm using a CrTree.
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
 * -C the confidence value <br>
 * The confidence value for the optional pessimistic-error-rate-based pruning
 * step (default: 0.25).
 * <p>
 * 
 * -N <br>
 * If set no pessimistic-error-rate-based pruning is performed.
 * <p>
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */
public class JCBAPruning extends CrTree implements OptionHandler, Serializable {

  /** The confidence value for the optional pruning step. */
  protected float m_cf;

  /** The number of pruned rules in the optionol pruning step. */
  protected int m_prunedRules;

  /** The initial number of rules. */
  protected int m_numberUnprunedRules;

  /** Flag indicating whether optional (pessimistic-error-rate-based pruning). */
  protected boolean m_noPessError;

  /** FastVector containing the sort order information for a set of rules. */
  protected FastVector[] m_precedence;

  /** the instances including the class attribute. */
  protected Instances m_instancesWithClass;

  /**
   * Constructor
   */
  public JCBAPruning() {

    super();
    resetOptions();
  }

  /**
   * Resets the options to the default values.
   */
  public void resetOptions() {

    m_cf = (float) 0.25;
    m_prunedRules = 0;
    m_noPessError = false;
  }

  /**
   * Performs the (optional) pessimistic-error.rate-based pruning step. If the
   * rule is not pruned, the method inserts it into the CrTree.
   * 
   * @param premise the rule premise
   * @param consequence the consequence and interestingness measures
   */
  @Override
  public void pruneBeforeInsertion(FastVector premise, FastVector consequence) {

    FastVector modified = premise.copy();
    double estimatedError = calculateError(premise, consequence);
    for (int i = 0; i < premise.size(); i = i + 2) {
      Object numHelp = modified.elementAt(i);
      Object valueHelp = modified.elementAt(i + 1);
      modified.removeElementAt(i);
      modified.removeElementAt(i);
      double newError = calculateError(modified, consequence);
      if (estimatedError > newError) {
        m_prunedRules++;
        return;
      }
      modified.insertElementAt(numHelp, i);
      modified.insertElementAt(valueHelp, i + 1);
    }
    insertNode(premise, consequence);
  }

  /**
   * Insert the consequence and the interestingness measures of a rule and
   * builds up the precedence information that allows a ranking according to the
   * interestingness measures
   * 
   * @param node the node in the tree where the consequence should be inserted
   * @param input the consequence
   */
  @Override
  public void insertContent(CrNode node, FastVector input) {

    node.addContent(input);
    m_precedence[0].addElement(node);
    m_precedence[1].addElement(new Integer(((Integer) input.firstElement())
      .intValue()));
    m_precedence[2].addElement(new Integer(0));
    m_precedence[3].addElement(new Integer(0));
  }

  /**
   * Calculates the pessimistic error rate of a rule
   * 
   * @param premise the premise
   * @param consequence the consequence
   * @return the pessimistic error rate
   */
  public double calculateError(FastVector premise, FastVector consequence) {

    int countInstances = 0, count, index, error = 0;
    int[] classLabels = new int[(m_class.attribute(0)).numValues()];

    for (int i = 0; i < classLabels.length; i++) {
      classLabels[i] = 0;
    }
    for (int i = 0; i < m_instances.numInstances(); i++) {
      Instance actual = m_instances.instance(i);
      for (count = 0; count < premise.size(); count = count + 2) {
        if (actual.value(((Integer) premise.elementAt(count)).intValue()) != ((Integer) premise
          .elementAt(count + 1)).intValue()) {
          break;
        }
      }
      if (count == premise.size()) {
        countInstances++;
        classLabels[(int) (m_class.instance(i)).value(0)]++;
      }
    }
    index = ((Integer) consequence.firstElement()).intValue();
    for (int i = 0; i < classLabels.length; i++) {
      if (i != index) {
        error = error + classLabels[i];
      }
    }
    return addErrs(countInstances, error, m_cf);

  }

  /**
   * Computes estimated pessimistic error rate for given total number of
   * instances and error using normal approximation to binomial distribution
   * (and continuity correction).
   * 
   * @param N number of instances
   * @param e observed error
   * @param CF confidence value
   * @return estimated pessimistic error rate
   */
  public static double addErrs(double N, double e, float CF) {

    // Ignore stupid values for CF
    if (CF > 0.5) {
      System.err.println("WARNING: confidence value for pruning "
        + " too high. Error estimate not modified.");
      return 0;
    }

    // Check for extreme cases at the low end because the
    // normal approximation won't work
    if (e < 1) {

      // Base case (i.e. e == 0) from documenta Geigy Scientific
      // Tables, 6th edition, page 185
      double base = N * (1 - Math.pow(CF, 1 / N));
      if (e == 0) {
        return base;
      }

      // Use linear interpolation between 0 and 1 like C4.5 does
      return base + e * (addErrs(N, 1, CF) - base);
    }

    // Use linear interpolation at the high end (i.e. between N - 0.5
    // and N) because of the continuity correction
    if (e + 0.5 >= N) {

      // Make sure that we never return anything smaller than zero
      return Math.max(N - e, 0);
    }

    // Get z-score corresponding to CF
    double z = Statistics.normalInverse(1 - CF);

    // Compute upper limit of confidence interval
    double f = (e + 0.5) / N;
    double r = (f + (z * z) / (2 * N) + z
      * Math.sqrt((f / N) - (f * f / N) + (z * z / (4 * N * N))))
      / (1 + (z * z) / N);

    return r;
  }

  /**
   * The preprocessing step before a rule is inserted into a CrTree. The main
   * purpose is to sort the items according to their frequencies in the
   * datsaset. More frequent items will be found in nodes closer to the root.
   * 
   * @param premises the premises
   * @param consequences the consequences
   * @param confidences the interestingness measures
   * @throws Exception throws exception if preprocessing is not possible
   */
  @Override
  public void preprocess(ArrayList<Object> premises,
    ArrayList<Object> consequences, ArrayList<Object> confidences)
    throws Exception {

    ArrayList<Object> kSets;
    FastVector unsorted;
    double[] supports;
    int[] indices;

    this.makeEmpty();
    m_sortedFrequencies = new FastVector();
    unsorted = new FastVector();
    kSets = ItemSet.singletons(m_instances);
    ItemSet.upDateCounters(kSets, m_instances);
    for (int i = 0; i < kSets.size(); i++) {
      ItemSet current = (ItemSet) kSets.get(i);
      int j = 0;
      while (current.itemAt(j) == -1) {
        j++;
      }
      FrequencyObjects obj = new FrequencyObjects(j, current.itemAt(j),
        current.support());
      unsorted.addElement(obj);
    }
    supports = new double[unsorted.size()];
    for (int i = 0; i < unsorted.size(); i++) {
      supports[i] = ((double) ((FrequencyObjects) unsorted.elementAt(i)).m_frequency)
        * (-1);
    }
    indices = Utils.stableSort(supports);
    for (int i = 0; i < unsorted.size(); i++) {
      m_sortedFrequencies.addElement(unsorted.elementAt(indices[i]));
    }

    m_prunedRules = 0;
    m_precedence = new FastVector[4];
    m_precedence[0] = new FastVector();
    m_precedence[1] = new FastVector();
    m_precedence[2] = new FastVector();
    m_precedence[3] = new FastVector();
    m_numberUnprunedRules = premises.size();
    for (int i = 0; i < premises.size(); i++) {
      ItemSet current = (ItemSet) premises.get(i);
      FastVector insertPremise = new FastVector();
      FastVector insertConsequence = new FastVector();

      ItemSet cons = (ItemSet) consequences.get(i);
      insertPremise = sortItemSet(current);
      insertConsequence.addElement(new Integer(cons.itemAt(0)));
      insertConsequence.addElement(confidences.get(i));
      insertConsequence.addElement(new Double(cons.support()));
      if (!m_noPessError) {
        pruneBeforeInsertion(insertPremise, insertConsequence);
      } else {
        insertNode(insertPremise, insertConsequence);
      }
    }

  }

  /**
   * Method that implements the obligatory pruning step
   */
  @Override
  public void prune() {

    double initialError = 0;
    Instance inst, classInst;
    FastVector saveIndex = new FastVector();
    Instances temp = new Instances(m_instances);
    Instances tempClass = new Instances(m_class);

    setDefaultClass(-1);
    for (int h = 1; h < m_class.numInstances(); h++) {
      if ((m_class.instance(h)).value(0) != this.getDefaultClass()) {
        initialError++;
      }
    }
    for (int i = 0; i < m_precedence[0].size(); i++) {
      boolean mark = false;
      m_actual = (CrNode) m_precedence[0].elementAt(i);
      CrNode start = m_actual;
      for (int j = 0; j < temp.numInstances(); j++) {
        inst = temp.instance(j);
        // satisfy
        int[] path = start.getPathInfo();
        while (start != m_root) {
          if (inst.isMissing(path[0]) || inst.value(path[0]) != path[1]) {
            break;
          }
          while (start.getTreeParent() == null) {
            start = start.getLastSibling();
          }
          start = start.getTreeParent();
          path = start.getPathInfo();
        }
        if (start == m_root) {// if(path[0] == -1 && path[1] ==-1){
          saveIndex.addElement(new Integer(j));
          classInst = tempClass.instance(j);
          if ((int) classInst.value(0) == ((Integer) m_precedence[1]
            .elementAt(i)).intValue()) {
            mark = true;
          }
        }
        start = m_actual;
      }
      if (mark) {
        int k = saveIndex.size() - 1;
        while (k >= 0) {
          temp.delete(((Integer) saveIndex.elementAt(k)).intValue());
          tempClass.delete(((Integer) saveIndex.elementAt(k)).intValue());
          k--;
        }
        m_precedence[2].setElementAt(new Integer(
          calculateDefaultClass(tempClass)), i);
        setDefaultClass(i);

        // compute error
        try {
          double error = 0;
          double[] distr = new double[(m_class.attribute(0)).numValues()];
          for (int w = 0; w < distr.length; w++) {
            distr[w] = 0;
          }
          for (int j = 0; j < m_instances.numInstances(); j++) {
            double value;
            JCBA classifier = new JCBA();
            value = classifier.intermediateClassificationForInstance(
              m_instancesWithClass.instance(j), this, m_instancesWithClass);
            if (value != (m_class.instance(j)).value(0)) {
              error++;
              distr[(int) (m_class.instance(j)).value(0)]++;
            }
          }
          if (temp.numInstances() == 0) {
            int max = Utils.maxIndex(distr);
            m_precedence[2].setElementAt(new Integer(max), i);
            setDefaultClass(i);
          }
          m_precedence[3].setElementAt(new Integer((int) error), i);
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println(e.getMessage());
        }
      } else {
        int j;
        for (j = 0; j < (m_actual.getContent()).size(); j++) {
          FastVector actual = (FastVector) (m_actual.getContent()).elementAt(j);
          int classNumber = ((Integer) actual.firstElement()).intValue();
          if (classNumber == ((Integer) m_precedence[1].elementAt(i))
            .intValue()) {
            break;
          }
        }
        m_precedence[0].removeElementAt(i);
        m_precedence[1].removeElementAt(i);
        m_precedence[2].removeElementAt(i);
        m_precedence[3].removeElementAt(i);
        deleteContent(m_actual, j);
        i--;
      }
      saveIndex.removeAllElements();
    }
    Object[] errorArray1 = m_precedence[3].toArray();
    int[] errorArray = new int[errorArray1.length];
    for (int i = 0; i < errorArray.length; i++) {
      errorArray[i] = ((Integer) errorArray1[i]).intValue();
    }
    int min = Utils.minIndex(errorArray);
    if (m_precedence[3].size() == 0 || errorArray[min] >= initialError) {
      min = -1;
    } else {
      // added to get same results as the CBA paper --- only helped in same
      // cases
      while (min < errorArray.length - 1
        && errorArray[min] == errorArray[min + 1]) {
        min++;
        // added ends
      }
    }
    setDefaultClass(min);
    int i = min + 1;
    while (i < m_precedence[0].size()) {
      int j;
      m_actual = (CrNode) m_precedence[0].elementAt(i);
      for (j = 0; j < (m_actual.getContent()).size(); j++) {
        FastVector actual = (FastVector) (m_actual.getContent()).elementAt(j);
        int classNumber = ((Integer) actual.firstElement()).intValue();
        if (classNumber == ((Integer) m_precedence[1].elementAt(i)).intValue()) {
          break;
        }
      }
      m_precedence[0].removeElementAt(i);
      m_precedence[1].removeElementAt(i);
      m_precedence[2].removeElementAt(i);
      m_precedence[3].removeElementAt(i);
      deleteContent(m_actual, j);
    }
  }

  /**
   * Gets the number of rules that should be used for classification
   * 
   * @return index in the sorted list before which the rules are used for
   *         classification.
   */
  public int getStopIndex() {

    return m_precedence[0].size() - 1;
  }

  /**
   * Gets the sorted list (according to the interestingness measure) of all
   * rules. The FastVector contains a pointer to a node containing the
   * consequence and the least frequent item of the premise of a rule.
   * 
   * @return FastVector containing CrNodes.
   */
  public FastVector getPrecedenceList() {

    return m_precedence[0];
  }

  /**
   * Gets the consequence (the class label) of a rule as an integer value.
   * 
   * @param index the rank of the rule in the sort order induced by the
   *          interestingness measure.
   * @return the consequence of a rule (that is a class label) as a integer.
   */
  public int getClassValue(int index) {

    return ((Integer) m_precedence[1].elementAt(index)).intValue();
  }

  /**
   * Sets the instances (including the class attribute)
   * 
   * @param instances the instances for which class association rules are mined.
   */
  public void setInstances(Instances instances) {

    m_instancesWithClass = instances;
  }

  /**
   * Calculates the default class as the majority class in the instances
   * 
   * @param RemainingClassInstances the set of instances
   * @return the default class label
   */
  public int calculateDefaultClass(Instances RemainingClassInstances) {

    int[] classes = new int[(m_class.attribute(0)).numValues()];
    int defaultClass;
    for (int i = 0; i < classes.length; i++) {
      classes[i] = 0;
    }
    for (int i = 0; i < RemainingClassInstances.numInstances(); i++) {
      classes[(int) (RemainingClassInstances.instance(i)).value(0)]++;
    }
    defaultClass = Utils.maxIndex(classes);
    return defaultClass;
  }

  /**
   * Sets the default class in each step during obligatory pruning.
   * 
   * @param i -1, if the default class is the majority class in the data the
   *          index of the rule in the sort order induced by the interestingness
   *          measure, if the default class is set during obligatory pruning.
   */
  public void setDefaultClass(int i) {

    int[] defaultClass = new int[2];
    defaultClass[0] = 0;
    if (i != -1) {
      defaultClass[1] = ((Integer) m_precedence[2].elementAt(i)).intValue();
    } else {
      defaultClass[1] = calculateDefaultClass(m_class);
    }
    ((this.getAssociateList()).getHead()).setContent(defaultClass);

  }

  /**
   * Gets the current settings of the Apriori object.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    String[] options = new String[3];
    int current = 0;

    if (m_noPessError) {
      options[current++] = "-N";
    } else {
      options[current++] = "-C";
      options[current++] = "" + m_cf;
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public java.util.Enumeration listOptions() {

    String string1 = "\tThe confidence value for pessimistic error pruning. (default = "
      + m_cf + ")";

    FastVector newVector = new FastVector(2);

    newVector.addElement(new Option(string1, "C", 1,
      "-C <the confidence value>"));

    newVector.addElement(new Option(string1, "N", 0, "-N"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -C the confidence value <br>
   * The confidence value for the optional pessimistic-error-rate-based pruning
   * step (default: 0.25).
   * <p>
   * 
   * -N <br>
   * If set no pessimistic-error-rate-based pruning is performed.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    resetOptions();
    m_noPessError = Utils.getFlag('N', options);
    if (!m_noPessError) {
      String cfString = Utils.getOption('C', options);
      if (cfString.length() != 0) {
        m_cf = Float.parseFloat(cfString);
      }
    }
  }

  /**
   * Sets optional pruning on or off and its confidence value
   * 
   * @param value the confidence value
   * @param flag flag indicating whether optional pruning should be on or off
   */
  public void optPruning(boolean flag, float value) {

    m_noPessError = !flag;
    m_cf = value;
  }

  /**
   * Returns a string description of the rule set stored in the tree
   * 
   * @param metricString the metric used as interestingness measure
   * @return outputs the stored rule set as a string
   */
  @Override
  public String toString(String metricString) {

    StringBuffer text = new StringBuffer();
    text.append("\n");
    text
      .append("Classification Rules (ordered):\n==========================\n\n");
    if (isEmpty()) {
      text.append("No Rules generated");
      return text.toString();
    }
    for (int i = 0; i < m_precedence[0].size(); i++) {
      int classValue = ((Integer) m_precedence[1].elementAt(i)).intValue(), index;
      CrNode start = (CrNode) m_precedence[0].elementAt(i);
      text.append((i + 1) + ".\t");
      do {
        int[] info = new int[2];
        info = start.getPathInfo();
        text.append(m_instances.attribute(info[0]).name() + '=');
        text.append(m_instances.attribute(info[0]).value(info[1]) + ' '
          + info[0] + ' ' + info[1] + ' ');
        while (start.getTreeParent() == null) {
          start = start.getLastSibling();
        }
        start = start.getTreeParent();
      } while (start != m_root);
      text.append(" ==> ");
      text.append(m_class.attribute(0).name() + '=');
      text.append(m_class.attribute(0).value(classValue) + "     "
        + metricString + ":");
      FastVector info = ((CrNode) m_precedence[0].elementAt(i)).getContent();
      for (index = 0; index < info.size(); index++) {
        FastVector actual = (FastVector) info.elementAt(index);
        int classNumber = ((Integer) actual.firstElement()).intValue();
        if (classNumber == classValue) {
          break;
        }
      }
      FastVector actualClass = (FastVector) info.elementAt(index);
      for (int j = 1; j < actualClass.size(); j++) {
        text.append("(");
        double moreInfo = ((Double) actualClass.elementAt(j)).doubleValue();
        text.append(Utils.doubleToString(moreInfo, 2) + "),  ");
      }
      text.append("\n");
    }
    text.append("\n\nDefault Class: " + m_class.attribute(0).name() + '='
      + m_class.attribute(0).value(getDefaultClass()) + "\n\n");
    text.append("Additional Information:\n");
    text
      .append("Number of Classification Associations Rules generated by Rule Miner: "
        + m_numberUnprunedRules + "\n");
    if (!m_noPessError) {
      text
        .append("Number of Classification Associations Rules after pessimistic error pruning : "
          + (m_numberUnprunedRules - m_prunedRules) + "\n");
    }
    text.append("Number of Classification Rules: " + (m_precedence[0].size())
      + "\n");
    text.append("\n");
    return text.toString();
  }

  /**
   * Gets the number of rules after the mining process that tried to get
   * inserted into the CrTree
   * 
   * @return the (initial) number of rules after the mining step.
   */
  public int numMinedRules() {

    return m_numberUnprunedRules;
  }

  /**
   * Gets the number of rules left after the (optional)
   * pessimistic-error-rate-based pruning step.
   * 
   * @return the number of rules left after the optional pruning step.
   */
  public int numPrunedRules() {

    return (m_numberUnprunedRules - m_prunedRules);
  }

  /**
   * The number of rules in the tree. After the pruning step this number equals
   * the number of rules used for classification.
   * 
   * @return the number of rules stored in the tree.
   */
  public int numClassRules() {

    return m_precedence[0].size();
  }

}
