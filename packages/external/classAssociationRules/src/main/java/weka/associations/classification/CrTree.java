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
 *    CrTree.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations.classification;

import java.io.Serializable;
import java.util.ArrayList;

import weka.associations.ItemSet;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;

/**
 * Class for building and using a CrTree. A CrTree(n-ary tree in child-sibling
 * representation) can store classification association rules(CARs) and allows
 * pruning and classification. Tree Structure described at: W. Li, J. Han,
 * J.Pei: CMAR: Accurate and Efficient Classification Based on Multiple
 * Class-Association Rules. In ICDM'01:369-376,2001.
 * 
 * The child sibling representation for n-ary trees is used
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */
public class CrTree extends PruneCAR implements Serializable {

  /** The root node of the tree. */
  protected CrNode m_root;

  /** The actual node of the tree. */
  protected CrNode m_actual;

  /** The CrList attached to the tree structure. */
  private CrList m_list;

  /** Indicates if node has to be inserted as sibling. */
  private boolean m_sibling;

  /**
   * The instances to which the mined rules belong to (class attribute splitted
   * off)
   */
  protected Instances m_instances;

  /** Instances containing only the class attribute */
  protected Instances m_class;

  /** List to remember sibling nodes when passing through the tree */
  protected FastVector m_storeSiblings;

  /** List to remember the sorted order of attribute value pairs */
  protected FastVector m_sortedFrequencies;

  /** counts number of rules in the tree. */
  private int m_counter;

  /** counts number of rules in the tree. */
  protected int m_deleteCount;

  /**
   * Constructor constructs an empty CrTree
   */
  public CrTree() {

    m_root = new CrNode(-1, -1, 0);
    m_list = new CrList();
    m_sibling = false;
  }

  /**
   * Constructor
   * 
   * @param root the root node
   * @param instances the instances without class attribute
   * @param onlyClass the class attribute and its values for all instances
   */
  public CrTree(CrNode root, Instances instances, Instances onlyClass) {

    m_root = root;
    m_list = new CrList();
    ;
    m_sibling = false;
    m_instances = instances;
    m_class = onlyClass;
  }

  /**
   * Constructor
   * 
   * @param instances the instances without class attribute
   * @param onlyClass the class attribute and its values for all instances
   */
  public CrTree(Instances instances, Instances onlyClass) {

    m_root = new CrNode(-1, -1, 0);
    m_list = new CrList();
    m_sibling = false;
    m_instances = instances;
    m_class = onlyClass;
  }

  /**
   * Inner class for handling the frequencies of items (attribute-value pairs).
   * It is needed to implement a CrTree as described at: W. Li, J. Han, J.Pei:
   * CMAR: Accurate and Efficient Classification Based on Multiple
   * Class-Association Rules. In ICDM'01:369-376,2001.
   * 
   * This class allows items to be sorted according to their frequency. Note:
   * this class has a natural ordering that is inconsistent with equals.
   * 
   * @author Stefan Mutter
   * @version $Revision$
   */
  protected class FrequencyObjects implements Comparable, Serializable {

    /** The attribute number. */
    private final int m_attributeNumber;

    /** The attribute value. */
    private final int m_attributeValue;

    /** The frequency value of an item (attribute-value pair). */
    protected int m_frequency;

    /**
     * Constructor
     * 
     * @param num the attribute number
     * @param value the attribute value
     * @param freq the frequency in the dataset of the attribute-value pair
     */
    public FrequencyObjects(int num, int value, int freq) {

      m_attributeNumber = num;
      m_attributeValue = value;
      m_frequency = freq;
    }

    /**
     * Compares two items according to their frequency Note: this class has a
     * natural ordering that is inconsistent with equals
     * 
     * @param obj the object that has to be compared
     * @return 0, if their frequencies are equak 1, if the frequency of the
     *         actual item is greater -1, otherwise
     */
    @Override
    public int compareTo(Object obj) {

      if (this.m_frequency == ((FrequencyObjects) obj).m_frequency) {
        return 0;
      }
      if (this.m_frequency < ((FrequencyObjects) obj).m_frequency) {
        return -1;
      }
      return 1;
    }

    /**
     * Equality test for items
     * 
     * @param obj the object to be compared
     * @return true if the two items are identical, e.g. they have the same
     *         attribute and attribute value
     */
    public boolean equals(FrequencyObjects obj) {

      if ((obj.m_attributeNumber == m_attributeNumber)
        && (obj.m_attributeValue == m_attributeValue)) {
        return true;
      }
      return false;
    }

    /**
     * Returns a string conatining attribute number, attribute value and
     * frequency in the dataset.
     * 
     * @return string description of an item with frequency value
     */
    @Override
    public String toString() {

      return (m_attributeNumber + " " + m_attributeValue + " " + m_frequency);
    }
  }

  /**
   * Sets the instances (without class attribute)
   * 
   * @param instances instances without classattribute
   */
  @Override
  public final void setInstancesNoClass(Instances instances) {

    m_instances = instances;
  }

  /**
   * Sets the class information
   * 
   * @param instances the class attribute and its values
   */
  @Override
  public final void setInstancesOnlyClass(Instances instances) {

    m_class = instances;
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

    for (int i = 0; i < premises.size(); i++) {
      ItemSet current = (ItemSet) premises.get(i);
      FastVector insertPremise = new FastVector();
      FastVector insertConsequence = new FastVector();
      FastVector fullConsequence = new FastVector();
      ItemSet cons = (ItemSet) consequences.get(i);
      insertPremise = sortItemSet(current);
      insertConsequence.addElement(new Integer(cons.itemAt(0)));
      insertConsequence.addElement(confidences.get(i));
      insertConsequence.addElement(new Double(cons.support()));
      fullConsequence = pruningCriterions(insertConsequence);

      pruneBeforeInsertion(insertPremise, fullConsequence);
    }
    setDefaultClass();
  }

  /**
   * Sets the default class as the amjority class in the instances. The default
   * class is stored in the head of the associated CrList.
   */
  public void setDefaultClass() {

    int[] defaultClass = new int[2], classes = new int[(m_class.attribute(0))
      .numValues()];
    defaultClass[0] = 0;
    for (int i = 0; i < classes.length; i++) {
      classes[i] = 0;
    }
    for (int i = 0; i < m_class.numInstances(); i++) {
      classes[(int) (m_class.instance(i)).value(0)]++;
    }
    defaultClass[1] = Utils.maxIndex(classes);
    (m_list.getHead()).setContent(defaultClass);
  }

  /**
   * Gets the default class
   * 
   * @return the default class coded as integer
   */
  public int getDefaultClass() {

    int[] defaultClass = (m_list.getHead()).getContent();
    return defaultClass[1];
  }

  /**
   * Sorts an item set accoring to the frequencies values of the items. For
   * every item the resulting FastVector conatins two integer: attribute number
   * and value
   * 
   * @param current the item set
   * @return a Fastvector with sorted attributes accoring to their frequencies.
   */
  public final FastVector sortItemSet(ItemSet current) {

    FastVector sortedPremise = new FastVector();

    for (int j = 0; j < m_sortedFrequencies.size(); j++) {
      if (current
        .itemAt(((FrequencyObjects) (m_sortedFrequencies.elementAt(j))).m_attributeNumber) == ((FrequencyObjects) (m_sortedFrequencies
        .elementAt(j))).m_attributeValue) {
        sortedPremise
          .addElement(new Integer(((FrequencyObjects) (m_sortedFrequencies
            .elementAt(j))).m_attributeNumber));
        sortedPremise
          .addElement(new Integer(((FrequencyObjects) (m_sortedFrequencies
            .elementAt(j))).m_attributeValue));
      }
    }
    return sortedPremise;
  }

  /**
   * Potentially adds additional measures that can be used as pruning criterias
   * to a rule. This method is provided for the children of CrTree. This class
   * implements no pruning and therefore needs no additional pruning criterias
   * 
   * @param input pruning criteria
   * @return a FastVector with the added criterias
   */
  @Override
  public FastVector pruningCriterions(FastVector input) {

    return input;
  }

  /**
   * Method that provides a pruning step before a rule is inserted into the
   * CrTree. In its general form here, no pruning is done.
   * 
   * @param premise the rule premise
   * @param consequence the rule consequence
   */
  @Override
  public void pruneBeforeInsertion(FastVector premise, FastVector consequence) {

    insertNode(premise, consequence);
  }

  /**
   * Checks if the the CrTree is empty
   * 
   * @return true if it is empty, false otherwise
   */
  @Override
  public final boolean isEmpty() {

    return m_root.getTreeChild() == null;
  }

  /**
   * Gets the root node
   * 
   * @return the root node
   */
  public final CrNode getRoot() {

    return m_root;
  }

  /**
   * Gets the associated list
   * 
   * @return the associated list
   */
  public final CrList getAssociateList() {

    return m_list;
  }

  /**
   * Deletes a node out of the child list.
   * 
   * @param node the node that has to be deletet out of the child list
   */
  public final void removeAtSibling(CrNode node) {

    (node.getLastSibling()).setNextSibling(node.getNextSibling());
    (node.getNextSibling()).setLastSibling(node.getLastSibling());
    node.setNextSibling(null);
    node.setLastSibling(null);
  }

  /**
   * Deletes a node out of the parent list.
   * 
   * @param node the node that has to be deleted
   * @param sibling a sibling node
   */
  public final void removeAtChild(CrNode node, CrNode sibling) {

    (node.getTreeParent()).setTreeChild(sibling);
    if (sibling != null) {
      sibling.setTreeParent(node.getTreeParent());
    }
    node.setTreeParent(null);
  }

  /**
   * deletes the node out of the list that connects nodes storing the same item.
   * If no other node stores the same item, the element in the associated list
   * is deleted as well.
   * 
   * @param node the node that has to be deleted
   */
  public final void removeAtList(CrNode node) {

    if (node.getListSibling() == null) {
      updateHeight(node);
      if (node.getNextListSibling() == null) {
        (node.getLastListSibling()).setNextListSibling(null);
        node.setLastListSibling(null);
      } else {
        (node.getLastListSibling()).setNextListSibling(node
          .getNextListSibling());
        (node.getNextListSibling()).setLastListSibling(node
          .getLastListSibling());
        node.setLastListSibling(null);
        node.setNextListSibling(null);
      }
    } else {
      if (node.getNextListSibling() == null) {
        m_list.deleteListElement(node.getListSibling());
        node.setListSibling(null);
      } else {
        updateHeight(node);
        (node.getNextListSibling()).setListSibling(node.getListSibling());
        (node.getNextListSibling()).setLastListSibling(null);
        (node.getListSibling()).setSiblingNode(node.getNextListSibling());
        node.setListSibling(null);
        node.setNextListSibling(null);
      }
    }
  }

  /**
   * Adjust the (minimum) height of a CrListElement
   * 
   * @param node the node that has to deleted
   */
  public final void updateHeight(CrNode node) {

    int height = node.getHeight(), compare = Integer.MAX_VALUE;
    CrNode start = node;
    while (start.getNextListSibling() != null) {
      start = start.getNextListSibling();
      height = Math.min(start.getHeight(), compare);
      compare = height;
    }
    start = node;
    while (start.getListSibling() == null) {
      start = start.getLastListSibling();
      height = Math.min(start.getHeight(), compare);
      compare = height;
    }
    if (((start.getListSibling()).getHeight()) != height) {
      (start.getListSibling()).setHeight(height);
    }
  }

  /**
   * Deletes one rule out of the CrTree
   * 
   * @param node the node where the consequence of the rule is stored
   * @param index the index of the consequence
   */
  @Override
  public final void deleteContent(CrNode node, int index) {

    m_deleteCount++;
    if (index < (node.getContent()).size()) {
      (node.getContent()).removeElementAt(index);
      if ((node.getContent()).size() == 0) {
        deleteNode(node);
      }
    }
  }

  /**
   * deletes a node out of the CrTree if possible
   * 
   * @param node the node that should be deleted
   */
  public final void deleteNode(CrNode node) {

    if ((node.getContent()).size() == 0 && node.getTreeChild() == null) {
      removeAtList(node);
      if (node.getTreeParent() == null) {
        removeAtSibling(node);
      } else {
        if (node.getNextSibling() == node) {
          CrNode parent = node.getTreeParent();
          removeAtChild(node, null);
          if ((parent.getContent()).size() == 0 && parent != m_root) {
            deleteNode(parent);
          }
        } else {
          removeAtChild(node, node.getNextSibling());
          removeAtSibling(node);
        }
      }
    }
  }

  /**
   * Adds another consequence to an existing premise.
   * 
   * @param node the node where the conesequence should be added
   * @param input the consequence
   */
  @Override
  public void insertContent(CrNode node, FastVector input) {

    node.addContent(input);
  }

  /**
   * insert a new rule into the CrTree in three steps: 1. search for existing
   * paths and the right node to start the insertion. 2. insert (rest of)
   * premise 3. insert consequence and additional measures (by calling
   * insertContent)
   * 
   * @param rulePremise the rule premise
   * @param ruleConsequence the consequence and additional measures
   */
  public final void insertNode(FastVector rulePremise,
    FastVector ruleConsequence) {

    int premiseSize = rulePremise.size();
    // 1. search the node to start the insertion
    FastVector remainder = search(rulePremise);
    // premise already exists; just add new consequence
    if (remainder.size() == 0) {
      insertContent(m_actual, ruleConsequence);
      return;
    }
    // 2. premise does not exist in its full length => create new nodes
    while (remainder.size() > 0) {
      int[] pathInfo = new int[2];
      pathInfo[0] = ((Integer) remainder.elementAt(0)).intValue();
      pathInfo[1] = ((Integer) remainder.elementAt(1)).intValue();
      CrListElement contained = m_list.searchListElement(pathInfo[0],
        pathInfo[1]);
      int height = 1 + ((premiseSize - remainder.size()) / 2);

      // item nowhere in the CrTree
      if (contained == null) {
        // new list entry
        CrListElement list = m_list.insertListElement(pathInfo[0], pathInfo[1],
          height);
        // new node
        CrNode node = list.getSiblingNode();
        node.setListSibling(list);
        if (!m_sibling) {
          m_actual.setTreeChild(node);
          node.setTreeParent(m_actual);
          node.setNextSibling(node);
          node.setLastSibling(node);
        } else {
          CrNode start = m_actual.getNextSibling();
          m_actual.setNextSibling(node);
          node.setLastSibling(m_actual);
          node.setNextSibling(start);
          start.setLastSibling(node);
          m_sibling = false;
        }
        m_actual = node;
      }

      // item already exists at another place in the CrTree
      else {
        CrNode node = new CrNode(pathInfo[0], pathInfo[1], height);
        if (!m_sibling) {
          m_actual.setTreeChild(node);
          node.setTreeParent(m_actual);
          node.setNextSibling(node);
          node.setLastSibling(node);
        } else {
          CrNode start = m_actual.getNextSibling();
          m_actual.setNextSibling(node);
          node.setLastSibling(m_actual);
          node.setNextSibling(start);
          start.setLastSibling(node);
          m_sibling = false;
        }
        // update associated list and pointers
        CrNode listSibling = contained.getSiblingNode();
        while (listSibling.getNextListSibling() != null) {
          listSibling = listSibling.getNextListSibling();
        }
        listSibling.setNextListSibling(node);
        node.setLastListSibling(listSibling);
        if ((contained.getHeight()) > height) {
          contained.setHeight(height);
        }
        m_actual = node;
      }
      remainder.removeElementAt(0);
      remainder.removeElementAt(0);
    }
    // 3. add consequence
    insertContent(m_actual, ruleConsequence);
  }

  /**
   * Searches for an existing rule premise are parts of an rule premise in the
   * CrTree
   * 
   * @param data the rule premise
   * @return the part of the rule premise that cannot be found in the CrTree
   */
  public final FastVector search(FastVector data) {

    if (isEmpty()) {
      m_actual = m_root;
      return data;
    }
    m_actual = m_root.getTreeChild();
    int i = 0;
    while (data.size() > 0) {
      int[] pathInfo = new int[2];
      pathInfo[0] = ((Integer) data.elementAt(0)).intValue();
      pathInfo[1] = ((Integer) data.elementAt(1)).intValue();
      i++;
      int[] index = new int[2];
      index = m_list.searchListIndex(pathInfo[0], pathInfo[1]);
      if (index[0] == 0 || index[1] > i) {
        m_actual = m_actual.getLastSibling();
        m_sibling = true; // new node is sibling
        return data;
      } else {
        int count = 0;
        CrNode stop = m_actual;
        do {
          if (m_actual.equals(pathInfo[0], pathInfo[1])) {
            count++;
            data.removeElementAt(0);
            data.removeElementAt(0);
            break;
          }
          m_actual = m_actual.getNextSibling();
        } while (stop != m_actual);
        if (stop == m_actual && count == 0) {
          m_actual = m_actual.getLastSibling();
          m_sibling = true;
          return data;
        }
      }
      if (data.size() == 0 || m_actual.getTreeChild() == null) {
        break;
      } else {
        m_actual = m_actual.getTreeChild();
      }

    }
    return data;
  }

  /**
   * Method for pruning a CrTree. In thgis general form of a CrTree there is no
   * pruning involved. Children of this class implement this method with their
   * pruning strategy.
   */
  @Override
  public void prune() {

  }

  /**
   * Deletes the whole CrTree and its associated list by setting the child
   * pointer of the root to null
   */
  public final void makeEmpty() {

    m_root.setTreeChild(null);
    m_list = new CrList();
    m_deleteCount = 0;
  }

  /**
   * Gets the class distribution of the rules from a subtree of a specified node
   * 
   * @param subTreeRoot the root of the subtree
   * @return the class distribution
   */
  public int[] reportSubtreeCount(CrNode subTreeRoot) {

    int[] result = new int[(m_class.attribute(0)).numValues()];
    for (int i = 0; i < result.length; i++) {
      result[i] = 0;
    }
    if ((subTreeRoot.getContent()).size() != 0) {
      for (int i = 0; i < (subTreeRoot.getContent()).size(); i++) {
        FastVector actual = (FastVector) (subTreeRoot.getContent())
          .elementAt(i);
        result[((Integer) actual.firstElement()).intValue()]++;
      }
    }
    if (subTreeRoot.getTreeChild() == null) {
      return result;
    }
    m_storeSiblings = new FastVector();
    m_storeSiblings.addElement(subTreeRoot.getTreeChild());
    while (m_storeSiblings.size() != 0) {
      m_actual = (CrNode) m_storeSiblings.firstElement();
      m_storeSiblings.removeElementAt(0);
      do {
        if ((m_actual.getNextSibling()).getTreeParent() == null) {
          m_storeSiblings.addElement(m_actual.getNextSibling());
        }
        if (m_actual.getContent().size() != 0) {
          for (int i = 0; i < (m_actual.getContent()).size(); i++) {
            FastVector actual = (FastVector) (m_actual.getContent())
              .elementAt(i);
            result[((Integer) actual.firstElement()).intValue()]++;
          }
        }
        m_actual = m_actual.getTreeChild();
      } while (m_actual != null);
    }
    return result;
  }

  /**
   * Prints a rule premise (from bottom to top in the tree, respectively
   * infrequent items to frequent ones).
   * 
   * @return a string with a rule premise
   */
  public String rulePremise() {

    CrNode start = m_actual;
    StringBuffer pathText = new StringBuffer();

    do {
      int[] info = new int[2];
      info = start.getPathInfo();
      pathText.append(m_instances.attribute(info[0]).name() + '=');
      pathText.append(m_instances.attribute(info[0]).value(info[1]) + ' '
        + info[0] + ' ' + info[1] + ' ');
      while (start.getTreeParent() == null) {
        start = start.getLastSibling();
      }
      start = start.getTreeParent();
    } while (start != m_root);
    return pathText.toString();
  }

  /**
   * Returns a string description of the CrTree Object
   * 
   * @param metricString the metric used
   * @return a string describing the CrTree object
   */
  @Override
  public String toString(String metricString) {

    StringBuffer text = new StringBuffer();

    text.append("\nClassification Rules\n==========================\n\n");
    if (isEmpty()) {
      text.append("No Rules generated");
      return text.toString();
    }
    m_counter = 1;
    m_storeSiblings = new FastVector();
    m_storeSiblings.addElement(m_root.getTreeChild());
    while (m_storeSiblings.size() != 0) {
      m_actual = (CrNode) m_storeSiblings.firstElement();
      m_storeSiblings.removeElementAt(0);
      text.append(pathToString(metricString).toString());
    }
    text.append("\n\nDefault Class: " + m_class.attribute(0).name() + '='
      + m_class.attribute(0).value(getDefaultClass()) + "\n\n\n");
    return text.toString();
  }

  /**
   * Prints a rule
   * 
   * @param metricString the metric used for class association rule mining
   * @return strting with rules with the same premise
   */
  public String pathToString(String metricString) {

    StringBuffer text = new StringBuffer();

    do {
      if ((m_actual.getNextSibling()).getTreeParent() == null) {
        m_storeSiblings.addElement(m_actual.getNextSibling());
      }
      if (m_actual.getContent().size() != 0) {
        // print out a rule
        String premise = rulePremise();
        for (int k = 0; k < (m_actual.getContent()).size(); k++) {
          text.append(m_counter + ".\t");
          m_counter++;
          text.append(premise);
          text.append(" ==> ");
          FastVector actualClass = (FastVector) m_actual.getContent()
            .elementAt(k);
          int classInfo = ((Integer) actualClass.firstElement()).intValue();
          text.append(m_class.attribute(0).name() + '=');
          text.append(m_class.attribute(0).value(classInfo) + "     "
            + metricString + ":");
          for (int j = 1; j < actualClass.size(); j++) {
            text.append("(");
            double moreInfo = ((Double) actualClass.elementAt(j)).doubleValue();
            text.append(Utils.doubleToString(moreInfo, 2) + "),  ");
          }
          text.append("\n");
        }
      }
      m_actual = m_actual.getTreeChild();
    } while (m_actual != null);

    return text.toString();
  }

  /**
   * Gets the current settings of the CrTree object. There are no options
   * available.
   * 
   * @return an array with an empty string.
   */
  @Override
  public String[] getOptions() {

    String[] options = new String[1];

    options[0] = "";

    return options;
  }

  /**
   * Returns an enumeration describing the available options. There are no
   * options available
   * 
   * @return a FastVector containing an empty string.
   */
  @Override
  public java.util.Enumeration listOptions() {

    String string1 = "";

    FastVector newVector = new FastVector(1);

    newVector.addElement(new Option(string1, "", 0, ""));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * There are no options available.
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    return;
  }

  /**
   * Returns the number of pruned rules
   * 
   * @return the number of pruned rules
   */
  @Override
  public int prunedRules() {

    return m_deleteCount;
  }

}
