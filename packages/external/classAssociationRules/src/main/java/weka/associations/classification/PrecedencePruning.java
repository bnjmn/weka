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
 *    PrecedencePrunung.java
 *    Copyright (C) 2003 Stefan Mutter
 *
 */

package weka.associations.classification;

import java.io.Serializable;
import java.util.ArrayList;

import weka.associations.ItemSet;
import weka.core.FastVector;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Class that allows a simple pessimistic-error-rate based pruning or omits
 * pruning at all and just builds a CrTree with all mined rules. In addition to
 * the CrTree a list containing information about the rule sorting is maintained
 * (precedence list). This list allows investigating the sort oder of rules
 * induced by the mining algorithm even when no pruning is performed.
 * 
 * The Tree Structure is described in: W. Li, J. Han, J.Pei: CMAR: Accurate and
 * Efficient Classification Based on Multiple Class-Association Rules. In
 * ICDM'01:369-376,2001.
 * 
 * The optional pessimistic-error-rate-based pruning step is described in: B.
 * Liu, W. Hsu, Y. Ma: Integrating Classification and Association Rule Mining.
 * In KDD'98:80-86,1998.
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

public class PrecedencePruning extends JCBAPruning implements OptionHandler,
  Serializable {

  /** The default class. */
  protected int m_default;

  /**
   * Constructor
   */
  public PrecedencePruning() {

    super();
    super.resetOptions();
  }

  /**
   * Insert consequence to a node and updates the precedence list
   * 
   * @param node the node
   * @param input the consequence and additional measures
   */
  @Override
  public void insertContent(CrNode node, FastVector input) {

    node.addContent(input);
    m_precedence[0].addElement(node);
    m_precedence[1].addElement(new Integer(((Integer) input.firstElement())
      .intValue()));

  }

  /**
   * Preprocesses a rule before it gets inserted into the tree. If set,
   * pessimistic-error-rate based pruning is performed.
   * 
   * @param premises the premises
   * @param consequences the consequences
   * @param confidences the metrices
   * @throws Exception throws exception is preprocessing is not possible.
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
    m_precedence = new FastVector[2];
    m_precedence[0] = new FastVector();
    m_precedence[1] = new FastVector();
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
      insertConsequence.addElement(new Double(i));
      if (!m_noPessError) {
        pruneBeforeInsertion(insertPremise, insertConsequence);
      } else {
        insertNode(insertPremise, insertConsequence);
      }
    }
    setDefaultClass();

  }

  /**
   * No pruning is perfomed (except of the optional pruning step in the
   * preprocessing method).
   */
  @Override
  public void prune() {

  }

  /**
   * Sets the default class as the majority class label of all instances
   */
  @Override
  public void setDefaultClass() {

    int[] classes = new int[(m_class.attribute(0)).numValues()];
    for (int i = 0; i < classes.length; i++) {
      classes[i] = 0;
    }
    for (int i = 0; i < m_class.numInstances(); i++) {
      classes[(int) (m_class.instance(i)).value(0)]++;
    }
    m_default = Utils.maxIndex(classes);
  }

  /**
   * Gets the default class
   * 
   * @return the default class
   */
  @Override
  public int getDefaultClass() {

    return m_default;
  }

  /**
   * Gets options
   * 
   * @return the options
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
   * Lists options
   * 
   * @return list of all available options
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
   * Sets pessimistic-error-rate-based pruning
   * 
   * @param flag true, if no pessimistic-error-rate-based pruning should be
   *          done, false otherwise
   */
  public void setNoPessPruning(boolean flag) {

    m_noPessError = flag;
  }

  /**
   * Sets options
   * 
   * @param options the options
   * @throws Exception exception if options cannot be set.
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
   * Outputs the CrTree
   * 
   * @param metricString the metric used to mine class association rules
   * @return the CrTree as a a string
   */
  public String toString(String metricString, int numMinedRules) {

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
      text.append("\n\n");
    }

    text.append("Additional Information:\n");
    text
      .append("Number of Classification Associations Rules generated by Rule Miner: "
        + numMinedRules + "\n");
    if (!m_noPessError) {
      text
        .append("Number of Classification Associations Rules after pessimistic error pruning : "
          + (m_numberUnprunedRules - m_prunedRules) + "\n");
    }
    text.append("Number of Classification Rules: " + (m_precedence[0].size())
      + "\n");

    return text.toString();
  }

  /**
   * Gets the number of mined rules
   * 
   * @return the number of mined rules
   */
  @Override
  public int numMinedRules() {

    return m_numberUnprunedRules;
  }

  /**
   * Gets the number of rules after the optional pessimistic-error.rate-based
   * pruning
   * 
   * @return the number of rules after the optional pessimistic-error.rate-based
   *         pruning
   */
  @Override
  public int numPrunedRules() {

    return (m_numberUnprunedRules - m_prunedRules);
  }

  /**
   * Gets the number of classification rules
   * 
   * @return the number of classification rules
   */
  @Override
  public int numClassRules() {

    return m_precedence[0].size();
  }

}
