/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    FPGrowth.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 <!-- globalinfo-start -->
 * Class implementing the FP-growth algorithm for finding large item sets without candidate generation. Iteratively reduces the minimum support until it finds the required number of rules with the given minimum metric. For more information see:<br/>
 * <br/>
 * J. Han, J.Pei, Y. Yin: Mining frequent patterns without candidate generation. In: Proceedings of the 2000 ACM-SIGMID International Conference on Management of Data, 1-12, 2000.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Han2000,
 *    author = {J. Han and J.Pei and Y. Yin},
 *    booktitle = {Proceedings of the 2000 ACM-SIGMID International Conference on Management of Data},
 *    pages = {1-12},
 *    title = {Mining frequent patterns without candidate generation},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -P &lt;attribute index of positive value&gt;
 *  Set the index of the attribute value to consider as 'positive'
 *  for binary attributes in normal dense instances. Index 2 is always
 *  used for sparse instances. (default = 2)</pre>
 * 
 * <pre> -I &lt;max items&gt;
 *  The maximum number of items to include in large items sets (and rules). (default = -1, i.e. no limit.)</pre>
 * 
 * <pre> -N &lt;require number of rules&gt;
 *  The required number of rules. (default = 10)</pre>
 * 
 * <pre> -T &lt;0=confidence | 1=lift | 2=leverage | 3=Conviction&gt;
 *  The metric by which to rank rules. (default = confidence)</pre>
 * 
 * <pre> -C &lt;minimum metric score of a rule&gt;
 *  The minimum metric score of a rule. (default = 0.9)</pre>
 * 
 * <pre> -U &lt;upper bound for minimum support&gt;
 *  Upper bound for minimum support. (default = 1.0)</pre>
 * 
 * <pre> -M &lt;lower bound for minimum support&gt;
 *  The lower bound for the minimum support. (default = 0.1)</pre>
 * 
 * <pre> -D &lt;delta for minimum support&gt;
 *  The delta by which the minimum support is decreased in
 *  each iteration. (default = 0.05)</pre>
 * 
 * <pre> -S
 *  Find all rules that meet the lower bound on
 *  minimum support and the minimum metric constraint.
 *  Turning this mode on will disable the iterative support reduction
 *  procedure to find the specified number of rules.</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class FPGrowth extends AbstractAssociator 
  implements OptionHandler, TechnicalInformationHandler {
  
  /** For serialization */
  private static final long serialVersionUID = 3620717108603442911L;

  /**
   * Inner class that handles a single binary item
   */
  public static class BinaryItem implements Serializable, Comparable<BinaryItem> {
    
    /** For serialization */
    private static final long serialVersionUID = -3372941834914147669L;
    
    /** The frequency of the item */
    protected int m_frequency;
    
    /** The attribute that the item corresponds to */
    protected Attribute m_attribute;
    
    /** The index of the value considered to be positive */
    protected int m_valueIndex;
    
    public BinaryItem(Attribute att, int valueIndex) throws Exception {
      if (att.isNumeric() || (att.isNominal() && att.numValues() > 2)) {
        throw new Exception("BinaryItem must be constructed using a nominal attribute" +
        		" with at most 2 values!");
      }
      m_attribute = att;
      if (m_attribute.numValues() == 1) {
        m_valueIndex = 0; // unary attribute (? used to indicate absence from a basket)
      } else {
        m_valueIndex = valueIndex;
      }
    }
    
    /**
     * Increase the frequency of this item.
     * 
     * @param f the amount to increase the frequency by.
     */
    public void increaseFrequency(int f) {
      m_frequency += f;
    }
    
    /**
     * Decrease the frequency of this item.
     * 
     * @param f the amount by which to decrease the frequency.
     */
    public void decreaseFrequency(int f) {
      m_frequency -= f;
    }
    
    /**
     * Increment the frequency of this item.
     */
    public void increaseFrequency() {
      m_frequency++;
    }
    
    /**
     * Decrement the frequency of this item.
     */
    public void decreaseFrequency() {
      m_frequency--;
    }
    
    /**
     * Get the frequency of this item.
     * 
     * @return the frequency.
     */
    public int getFrequency() {
      return m_frequency;
    }
    
    /**
     * Get the attribute that this item corresponds to.
     * 
     * @return the corresponding attribute.
     */
    public Attribute getAttribute() {
      return m_attribute;
    }
    
    /**
     * Get the value index for this item.
     * 
     * @return the value index.
     */
    public int getValueIndex() {
      return m_valueIndex;
    }
    
    /**
     * A string representation of this item.
     * 
     * @return a string representation of this item.
     */
    public String toString() {
      return toString(false);
    }
    
    /**
     * A string representation of this item.
     * 
     * @param freq true if the frequency should be included.
     * @return a string representation of this item. 
     */
    public String toString(boolean freq) {
      String result = m_attribute.name() + "=" + m_attribute.value(m_valueIndex);
      if (freq) {
        result += ":" + m_frequency;
      }
      return result;
    }
    
    /**
     * Ensures that items will be sorted in descending order of frequency.
     * Ties are ordered by attribute name.
     * 
     * @param comp the BinaryItem to compare against.
     */
    public int compareTo(BinaryItem comp) {
      if (m_frequency == comp.getFrequency()) {
        // sort by name
        return -1 * m_attribute.name().compareTo(comp.getAttribute().name());
      }
      if (comp.getFrequency() < m_frequency) {
        return -1;
      }
      return 1;
    }
    
    public boolean equals(Object compareTo) {
      if (!(compareTo instanceof BinaryItem)) {
        return false;
      }
      
      BinaryItem b = (BinaryItem)compareTo;
      if (m_attribute.equals(b.getAttribute()) && m_frequency == b.getFrequency()) {
        return true;
      }
      
      return false;
    }
    
    public int hashCode() {
      return (m_attribute.name().hashCode() ^ 
          m_attribute.numValues()) * m_frequency;
    }
  }
  
  /**
   * Class for maintaining a frequent item set.
   */
  protected static class FrequentBinaryItemSet 
    implements Serializable, Cloneable {
    
    /** For serialization */
    private static final long serialVersionUID = -6543815873565829448L;

    /** The list of items in the item set */
    protected ArrayList<BinaryItem> m_items = new ArrayList<BinaryItem>();
    
    /** the support of this item set **/
    protected int m_support;
    
    /**
     * Constructor
     * 
     * @param items the items that make up the frequent item set.
     * @param support the support of this item set.
     */
    public FrequentBinaryItemSet(ArrayList<BinaryItem> items, int support) {
      m_items = items;
      m_support = support;
      Collections.sort(m_items);
    }
    
    /**
     * Add an item to this item set.
     * 
     * @param i the item to add.
     */
    public void addItem(BinaryItem i) {
      m_items.add(i);
      Collections.sort(m_items);
    }
    
    /**
     * Set the support for this item set.
     * 
     * @param support the support for this item set.
     */
    public void setSupport(int support) {
      m_support = support;
    }
    
    /**
     * Get the support of this item set.
     * 
     * @return the support of this item set.
     */
    public int getSupport() {
      return m_support;
    }
    
    /**
     * Get the items in this item set.
     * 
     * @return the items in this item set.
     */
    public Collection<BinaryItem> getItems() {
      return m_items;
    }
    
    /**
     * Get a particular item from this item set.
     * 
     * @param index the index of the item to get.
     * @return the item.
     */
    public BinaryItem getItem(int index) {
      return m_items.get(index);
    }
    
    /**
     * Get the number of items in this item set.
     * 
     * @return the number of items in this item set.
     */
    public int numberOfItems() {
      return m_items.size();
    }
    
    /**
     * Get a textual description of this item set.
     * 
     * @return a textual description of this item set.
     */
    public String toString() {
      StringBuffer buff = new StringBuffer();
      Iterator<BinaryItem> i = m_items.iterator();
      
      while (i.hasNext()) {
        buff.append(i.next().toString() + " ");        
      }
      buff.append(": " + m_support);
      return buff.toString();
    }
    
    /**
     * Make a copy of this item set.
     * 
     * @return a copy of this item set.
     */
    public Object clone() {
      ArrayList<BinaryItem> items = new ArrayList<BinaryItem>(m_items);
      return new FrequentBinaryItemSet(items, m_support);
    }
  }
  
  /**
   * Maintains a list of frequent item sets.
   */
  protected static class FrequentItemSets implements Serializable {
    
    /** For serialization */
    private static final long serialVersionUID = 4173606872363973588L;

    /** The list of frequent item sets */
    protected ArrayList<FrequentBinaryItemSet> m_sets = 
      new ArrayList<FrequentBinaryItemSet>();
    
    /** The total number of transactions in the data */
    protected int m_numberOfTransactions;
    
    /**
     * Constructor.
     * 
     * @param numTransactions the total number of transactions in the data.
     */
    public FrequentItemSets(int numTransactions) {
      m_numberOfTransactions = numTransactions;
    }
    
    /**
     * Get an item set.
     * 
     * @param index the index of the item set to get.
     * @return an item set.
     */
    public FrequentBinaryItemSet getItemSet(int index) {
      return m_sets.get(index);
    }
    
    /**
     * Get an iterator that can be used to access all the item sets.
     * 
     * @return an iterator.
     */
    public Iterator<FrequentBinaryItemSet> iterator() {
      return m_sets.iterator();
    }
    
    /**
     * Get the total number of transactions in the data that the item
     * sets were derived from.
     * 
     * @return the total number of transactions in the data.
     */
    public int getNumberOfTransactions() {
      return m_numberOfTransactions;
    }
    
    /**
     * Add an item set.
     * 
     * @param setToAdd the item set to add.
     */
    public void addItemSet(FrequentBinaryItemSet setToAdd) {
      m_sets.add(setToAdd);
    }
    
    /**
     * Sort the item sets according to the supplied comparator.
     * 
     * @param comp the comparator to use.
     */
    public void sort(Comparator<FrequentBinaryItemSet> comp) {
      Collections.sort(m_sets, comp);
    }
    
    /**
     * Get the number of item sets.
     * 
     * @return the number of item sets.
     */
    public int size() {
      return m_sets.size();
    }
    
    /**
     * Sort the item sets. Sorts by item set length. Ties are broken by comparing
     * the items in the two item sets.
     */
    public void sort() {
      Comparator<FrequentBinaryItemSet> compF = new Comparator<FrequentBinaryItemSet>() {
        public int compare(FrequentBinaryItemSet one, FrequentBinaryItemSet two) {
          Collection<BinaryItem> compOne = one.getItems();
          Collection<BinaryItem> compTwo = two.getItems();
          
//          if (one.getSupport() == two.getSupport()) {
            // if supports are equal then list shorter item sets before longer ones
            if (compOne.size() < compTwo.size()) {
              return -1;
            } else if (compOne.size() > compTwo.size()) {
              return 1;
            } else {
              // compare items
              Iterator<BinaryItem> twoIterator = compTwo.iterator();
              for (BinaryItem oneI : compOne) {
                BinaryItem twoI = twoIterator.next();
                int result = oneI.compareTo(twoI);
                if (result != 0) {
                  return result;
                }
              }
              return 0; // equal
            }
            
//            return 0;
    /*      } else if (one.getSupport() > two.getSupport()) {
            // reverse ordering (i.e. descending by support)
            return -1;
          } */
          
    //      return 1;
        }
      };
      
      sort(compF);
    }
    
    /**
     * Get a textual description of this list of item sets.
     * 
     * @param numSets the number of item sets to display.
     * @return a textual description of the item sets.
     */
    public String toString(int numSets) {
      if (m_sets.size() == 0) {
        return "No frequent items sets found!";
      }
      
      StringBuffer result = new StringBuffer();
      result.append("" + m_sets.size() + " frequent item sets found");
      if (numSets > 0) {
        result.append(" , displaying " + numSets);
      }
      result.append(":\n\n");
      
      int count = 0;
      for (FrequentBinaryItemSet i : m_sets) {
        if (numSets > 0 && count > numSets) {
          break;
        }
        result.append(i.toString() + "\n");
        count++;
      }
      
      return result.toString();
    }
  }
  
  /**
   * This class holds the counts for projected tree nodes
   * and header lists.
   */
  protected static class ShadowCounts implements Serializable {
    
    /** For serialization */
    private static final long serialVersionUID = 4435433714185969155L;
    
    /** Holds the counts at different recursion levels */
    private ArrayList<Integer> m_counts = new ArrayList<Integer>();
    
    /**
     * Get the count at the specified recursion depth.
     * 
     * @param recursionLevel the depth of the recursion.
     * @return the count.
     */
    public int getCount(int recursionLevel) {
      if (recursionLevel >= m_counts.size()) {
        return 0;
      } else {
        return m_counts.get(recursionLevel);
      }
    }
    
    /**
     * Increase the count at a given recursion level.
     * 
     * @param recursionLevel the level at which to increase the count.
     * @param incr the amount by which to increase the count.
     */
    public void increaseCount(int recursionLevel, int incr) {
      // basically treat the list like a stack where we
      // can add a new element, or increment the element
      // at the top
      
      if (recursionLevel == m_counts.size()) {
        // new element
        m_counts.add(incr);
      } else if (recursionLevel == m_counts.size() - 1) {
        // otherwise increment the top
        int n = m_counts.get(recursionLevel).intValue();
        m_counts.set(recursionLevel, (n + incr));
      }
    }
    
    /**
     * Remove the count at the given recursion level.
     * 
     * @param recursionLevel the level at which to remove the count.
     */
    public void removeCount(int recursionLevel) {
      if (recursionLevel < m_counts.size()) {
        m_counts.remove(recursionLevel);
      }
    }
  }
  
  /**
   * A node in the FP-tree.
   */
  protected static class FPTreeNode implements Serializable {
                
    /** For serialization */
    private static final long serialVersionUID = 4396315323673737660L;

    /** link to another sibling at this level in the tree */
    protected FPTreeNode m_levelSibling;
    
    /** link to the parent node */
    protected FPTreeNode m_parent;
    
    /** item at this node */
    protected BinaryItem m_item;
    
    /** ID (for graphing the tree) */
    protected int m_ID;
    
    /** the children of this node */
    protected Map<BinaryItem, FPTreeNode> m_children = 
      new HashMap<BinaryItem, FPTreeNode>();
    
    /** counts associated with projected versions of this node */
    protected ShadowCounts m_projectedCounts = new ShadowCounts();
    
    /**
     * Construct a new node with the given parent link and item.
     * 
     * @param parent a pointer to the parent of this node.
     * @param item the item at this node.
     */
    public FPTreeNode(FPTreeNode parent, BinaryItem item) {
      m_parent = parent;
      m_item = item;
    }
    
    /**
     * Insert an item set into the tree at this node. Removes the first item
     * from the supplied item set and makes a recursive call to insert the
     * remaining items.
     * 
     * @param itemSet the item set to insert.
     * @param headerTable the header table for the tree.
     * @param incr the amount by which to increase counts.
     */
    public void addItemSet(Collection<BinaryItem> itemSet, 
        Map<BinaryItem, FPTreeRoot.Header> headerTable, int incr) {
     
      Iterator<BinaryItem> i = itemSet.iterator();
      
      if (i.hasNext()) {
        BinaryItem first = i.next();
        
        FPTreeNode aChild;
        if (!m_children.containsKey(first)) {
          // not in the tree, so add it.
          aChild = new FPTreeNode(this, first);
          m_children.put(first, aChild);
          
          // update the header
          if (!headerTable.containsKey(first)) {
            headerTable.put(first, new FPTreeRoot.Header());
          }
          
          // append new node to header list
          headerTable.get(first).addToList(aChild);
        } else {
          // get the appropriate child node
          aChild = m_children.get(first);
        }
        
        // update counts in header table
        headerTable.get(first).getProjectedCounts().increaseCount(0, incr);
        
        // increase the child's count
        aChild.increaseProjectedCount(0, incr);
        
        // proceed recursively
        itemSet.remove(first);        
        aChild.addItemSet(itemSet, headerTable, incr);
      }
    }
    
    /**
     * Increase the projected count at the given recursion level at this
     * node
     * 
     * @param recursionLevel the recursion level to increase the node count
     * at.
     * @param incr the amount by which to increase the count.
     */
    public void increaseProjectedCount(int recursionLevel, int incr) {
      m_projectedCounts.increaseCount(recursionLevel, incr);
    }
    
    /**
     * Remove the projected count at the given recursion level for this
     * node.
     * 
     * @param recursionLevel the recursion level at which to remove the count.
     */
    public void removeProjectedCount(int recursionLevel) {
      m_projectedCounts.removeCount(recursionLevel);
    }
    
    /**
     * Get the projected count at the given recursion level for this node.
     * 
     * @param recursionLevel the recursion level at which to get the count.
     * @return the count.
     */
    public int getProjectedCount(int recursionLevel) {
      return m_projectedCounts.getCount(recursionLevel);
    }
    
    /**
     * Get the parent node.
     * 
     * @return the parent node.
     */
    public FPTreeNode getParent() {
      return m_parent;
    }
    
    /**
     * Get the item at this node.
     * 
     * @return the item at this node.
     */
    public BinaryItem getItem() {
      return m_item;
    }    
    
    /**
     * Return a textual description of this node for a given recursion
     * level.
     * 
     * @param recursionLevel the recursion depth to use.
     * @return a textual description of this node.
     */
    public String toString(int recursionLevel) {
      return toString("", recursionLevel);
    }

    /**
     * Return a textual description of this node for a given recursion
     * level.
     * 
     * @param prefix a prefix string to prepend.
     * @param recursionLevel the recursion level to use.
     * @return a textual description of this node. 
     */
    public String toString(String prefix, int recursionLevel) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(prefix);
      buffer.append("|  ");
      buffer.append(m_item.toString());
      buffer.append(" (");
      buffer.append(m_projectedCounts.getCount(recursionLevel));
      buffer.append(")\n");
      
      for (FPTreeNode node : m_children.values()) {
        buffer.append(node.toString(prefix + "|  ", recursionLevel));
      }
      return buffer.toString();
    }
    
    protected int assignIDs(int lastID) {
      int currentLastID = lastID + 1;
      m_ID = currentLastID;
      if (m_children != null) {
        Collection<FPTreeNode> kids = m_children.values();
        for (FPTreeNode n : kids) {
          currentLastID = n.assignIDs(currentLastID);
        }
      }
      return currentLastID;
    }
    
    /**
     * Generate a dot graph description string for the tree.
     * 
     * @param text a StringBuffer to store the graph description
     * in.
     */
    public void graphFPTree(StringBuffer text) {
      if (m_children != null) {
        Collection<FPTreeNode> kids = m_children.values();
        for (FPTreeNode n : kids) {
          text.append("N" + n.m_ID);
          text.append(" [label=\"");
          text.append(n.getItem().toString() + " (" + n.getProjectedCount(0) + ")\\n");
          text.append("\"]\n");
          n.graphFPTree(text);
          text.append("N" + m_ID + "->" + "N" + n.m_ID + "\n");
        }
      }
    }
  }
  
  /**
   * Root of the FPTree
   */
  private static class FPTreeRoot extends FPTreeNode {
    
    /** For serialization */
    private static final long serialVersionUID = 632150939785333297L;

    /**
     * Stores a header entry for an FPTree 
     */
    protected static class Header implements Serializable {
      
      /** For serialization */
      private static final long serialVersionUID = -6583156284891368909L;
      
      /** The list of pointers into the tree structure */
      protected List<FPTreeNode> m_headerList = new LinkedList<FPTreeNode>();
      
      /** Projected header counts for this entry */
      protected ShadowCounts m_projectedHeaderCounts = new ShadowCounts();
      
      /**
       * Add a tree node into the list for this header entry.
       * 
       * @param toAdd the node to add.
       */
      public void addToList(FPTreeNode toAdd) {
        m_headerList.add(toAdd);
      }
      
      /**
       * Get the list of nodes for this header entry.
       * 
       * @return the list of nodes for this header entry.
       */
      public List<FPTreeNode> getHeaderList() {
        return m_headerList;
      }
      
      /**
       * Get the projected counts for this header entry.
       * 
       * @return the projected counts for this header entry.
       */
      public ShadowCounts getProjectedCounts() {
        return m_projectedHeaderCounts;
      }
    }
    
    /** Stores the header table as mapped Header entries */
    protected Map<BinaryItem, Header> m_headerTable = 
      new HashMap<BinaryItem, Header>();
    
    /**
     * Create a new FPTreeRoot.
     */
    public FPTreeRoot() {
      super(null, null);
    }
    
    /**
     * Insert an item set into the tree.
     * 
     * @param itemSet the item set to insert into the tree.
     * @param incr the increment by which to increase counters.
     */
    public void addItemSet(Collection<BinaryItem> itemSet, int incr) {
      super.addItemSet(itemSet, m_headerTable, incr);
    }
    
    /**
     * Get the header table for this tree.
     * 
     * @return the header table for this tree.
     */
    public Map<BinaryItem, Header> getHeaderTable() {
      return m_headerTable;
    }
    
    public boolean isEmpty(int recursionLevel) {
      for (FPTreeNode c : m_children.values()) {
        if (c.getProjectedCount(recursionLevel) > 0) {
          return false;
        }
      }
      return true;
    }
    
    /**
     * Get a textual description of the tree at a given recursion
     * (projection) level.
     * 
     * @param pad the string to use as a prefix for indenting nodes.
     * @param recursionLevel the recursion level (projection) to use.
     * @return the textual description of the tree.
     */
    public String toString(String pad, int recursionLevel) {
      StringBuffer result = new StringBuffer();
      result.append(pad);
      result.append("+ ROOT\n");

      for (FPTreeNode node : m_children.values()) {
        result.append(node.toString(pad + "|  ", recursionLevel));
      }
      return result.toString();
    }

    /**
     * Get a textual description of the header table for this tree.
     * 
     * @param recursionLevel the recursion level to use.
     * @return a textual description of the header table for this
     * tree at a given recursion level.
     */
    public String printHeaderTable(int recursionLevel) {
      StringBuffer buffer = new StringBuffer();
      for (BinaryItem item : m_headerTable.keySet()) {
        buffer.append(item.toString());
        buffer.append(" : ");
        buffer.append(m_headerTable.get(item).getProjectedCounts().getCount(recursionLevel));
        buffer.append("\n");
      }
      return buffer.toString();
    }
    
    public void graphHeaderTable(StringBuffer text, int maxID) {

      for (BinaryItem item : m_headerTable.keySet()) {
        Header h = m_headerTable.get(item);
        List<FPTreeNode> headerList = h.getHeaderList();
        if (headerList.size() > 1) {
          text.append("N" + maxID + " [label=\"" + headerList.get(0).getItem().toString() 
              + " (" + h.getProjectedCounts().getCount(0) + ")"
              + "\" shape=plaintext]\n");

          text.append("N" + maxID + "->" + "N" + headerList.get(1).m_ID + "\n");
          for (int i = 1; i < headerList.size() - 1; i++) {
            text.append("N" + headerList.get(i).m_ID + "->" + "N" + headerList.get(i+1).m_ID + "\n");
          }
          maxID++;
        }
      }
    }
  }
  
  /**
   * Class for storing and manipulating an association rule. Also has a utility
   * routine for generating (by brute force) all the association rules that meet
   * a given metric threshold from a list of large item sets.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com).
   */
  /**
   * @author mhall
   *
   */
  public static class AssociationRule implements Serializable, Comparable<AssociationRule> {
    
    /** For serialization */
    private static final long serialVersionUID = -661269018702294489L;

    /** Enum for holding different metric types */
    public static enum METRIC_TYPE {
      CONFIDENCE("conf") {
        double compute(int premiseSupport, int consequenceSupport, 
            int totalSupport, int totalTransactions) {
          
          return (double)totalSupport / (double)premiseSupport;
        }
      },
      LIFT("lift") {
        double compute(int premiseSupport, int consequenceSupport, 
            int totalSupport, int totalTransactions) {
          
          double confidence = 
            METRIC_TYPE.CONFIDENCE.compute(premiseSupport, consequenceSupport, 
                totalSupport, totalTransactions);
          return confidence / ((double)consequenceSupport /
              (double)totalTransactions);
        }
      },
      LEVERAGE("lev") {
        double compute(int premiseSupport, int consequenceSupport, 
            int totalSupport, int totalTransactions) {
          
          double coverageForItemSet = (double)totalSupport /
            (double)totalTransactions;
          double expectedCoverageIfIndependent = 
            ((double)premiseSupport / (double)totalTransactions) *
            ((double)consequenceSupport / (double)totalTransactions);
          return coverageForItemSet - expectedCoverageIfIndependent;
        }
      },
      CONVICTION("conv") {
        double compute(int premiseSupport, int consequenceSupport, 
            int totalSupport, int totalTransactions) {
          
          double num = 
            (double)premiseSupport * (double)(totalTransactions - consequenceSupport) /
            (double)totalTransactions;
          double denom = premiseSupport - totalSupport + 1;
          return num / denom;
        }
      };
      
      private final String m_stringVal;
      METRIC_TYPE(String name) {
        m_stringVal = name;
      }
      
      abstract double compute(int premiseSupport, int consequenceSupport, 
          int totalSupport, int totalTransactions);
      
      public String toString() {
        return m_stringVal;
      }
      
      public String toStringMetric(int premiseSupport, int consequenceSupport,
          int totalSupport, int totalTransactions) {
        return m_stringVal + ":(" + Utils.doubleToString(compute(premiseSupport, consequenceSupport,
            totalSupport, totalTransactions), 2) + ")";
      }
    }
    
    /** Tags for display in the GUI */
    public static final Tag[] TAGS_SELECTION = {
      new Tag(METRIC_TYPE.CONFIDENCE.ordinal(), "Confidence"),
      new Tag(METRIC_TYPE.LIFT.ordinal(), "Lift"),
      new Tag(METRIC_TYPE.LEVERAGE.ordinal(), "Leverage"),
      new Tag(METRIC_TYPE.CONVICTION.ordinal(), "Conviction")
    };
    
    /** The metric type for this rule */
    protected METRIC_TYPE m_metricType = METRIC_TYPE.CONFIDENCE;
    
    /** The premise of the rule */
    protected Collection<BinaryItem> m_premise;
    
    /** The consequence of the rule */
    protected Collection<BinaryItem> m_consequence;
    
    /** The support for the premise */
    protected int m_premiseSupport;
    
    /** The support for the consequence */
    protected int m_consequenceSupport;
    
    /** The total support for the item set (premise + consequence) */
    protected int m_totalSupport;
    
    /** The total number of transactions in the data */
    protected int m_totalTransactions;
    
    /**
     * Construct a new association rule.
     * 
     * @param premise the premise of the rule
     * @param consequence the consequence of the rule
     * @param metric the metric for the rule
     * @param premiseSupport the support of the premise
     * @param consequenceSupport the support of the consequence
     * @param totalSupport the total support of the rule
     * @param totalTransactions the number of transactions in the data
     */
    public AssociationRule(Collection<BinaryItem> premise, 
        Collection<BinaryItem> consequence, METRIC_TYPE metric,
        int premiseSupport, int consequenceSupport,
        int totalSupport, int totalTransactions) {
      m_premise = premise;
      m_consequence = consequence;
      m_metricType = metric;
      m_premiseSupport = premiseSupport;
      m_consequenceSupport = consequenceSupport;
      m_totalSupport = totalSupport;
      m_totalTransactions = totalTransactions;
    }
    
    /**
     * Get the premise of this rule.
     * 
     * @return the premise of this rule.
     */
    public Collection<BinaryItem> getPremise() {
      return m_premise;
    }
    
    /**
     * Get the consequence of this rule.
     * 
     * @return the consequence of this rule.
     */
    public Collection<BinaryItem> getConsequence() {
      return m_consequence;
    }
    
    /**
     * Get the metric type of this rule (e.g. confidence).
     * 
     * @return the metric type of this rule.
     */
    public METRIC_TYPE getMetricType() {
      return m_metricType;
    }
    
    /**
     * Get the value of the metric for this rule. 
     * 
     * @return the value of the metric for this rule.
     */
    public double getMetricValue() {
      return m_metricType.compute(m_premiseSupport, m_consequenceSupport, 
          m_totalSupport, m_totalTransactions);
    }
    
    /**
     * Get the support for the premise.
     * 
     * @return the support for the premise.
     */
    public int getPremiseSupport() {
      return m_premiseSupport;
    }
    
    /**
     * Get the support for the consequence.
     * 
     * @return the support for the consequence.
     */
    public int getConsequenceSupport() {
      return m_consequenceSupport;
    }
    
    /**
     * Get the total support for this rule (premise + consequence).
     * 
     * @return the total support for this rule.
     */
    public int getTotalSupport() {
      return m_totalSupport;
    }
    
    /**
     * Get the total number of transactions in the data.
     * 
     * @return the total number of transactions in the data.
     */
    public int getTotalTransactions() {
      return m_totalTransactions;
    }
    
    /**
     * Compare this rule to the supplied rule.
     * 
     * @param other the rule to compare to.
     * @return the result of the comparison.
     */
    public int compareTo(AssociationRule other) {
      return -Double.compare(getMetricValue(), other.getMetricValue());
    }
    
    /**
     * Return true if this rule is equal to the supplied one.
     * 
     * @return true if this rule is the same as the supplied rule.
     */
    public boolean equals(Object other) {
      if (!(other instanceof AssociationRule)) {
        return false;
      }
      
      AssociationRule otherRule = (AssociationRule)other;
      boolean result = m_premise.equals(otherRule.getPremise()) &&
        m_consequence.equals(otherRule.getConsequence()) && 
        (getMetricValue() == otherRule.getMetricValue());
      
      return result;
    }
    
    /**
     * Get a textual description of this rule.
     * 
     * @return a textual description of this rule.
     */
    public String toString() {
      StringBuffer result = new StringBuffer();
      
      result.append(m_premise.toString() + ": " + m_premiseSupport 
          + " ==> " + m_consequence.toString() + ": " + m_totalSupport 
          + "   ");
      for (METRIC_TYPE m : METRIC_TYPE.values()) {
        if (m.equals(m_metricType)) {
          result.append("<" + 
              m.toStringMetric(m_premiseSupport, m_consequenceSupport, 
                  m_totalSupport, m_totalTransactions) + "> ");
        } else {
          result.append("" + 
              m.toStringMetric(m_premiseSupport, m_consequenceSupport, 
                  m_totalSupport, m_totalTransactions) + " ");
        }
      }
      return result.toString();
    }
    
    private static void nextSubset(boolean[] subset) {
      for (int i = 0; i < subset.length; i++) {
        if (!subset[i]) {
          subset[i] = true;
          break;
        } else {
          subset[i] = false;
        }
      }
    }
    
    private static Collection<BinaryItem> getPremise(FrequentBinaryItemSet fis, 
        boolean[] subset) {
      boolean ok = false;
      for (int i = 0; i < subset.length; i++){
        if (!subset[i]) {
          ok = true;
          break;
        }
      }      
      
      if (!ok) {
        return null;
      }
      
      List<BinaryItem> premise = new ArrayList<BinaryItem>();
      ArrayList<BinaryItem> items = new ArrayList<BinaryItem>(fis.getItems());

      
      for (int i = 0; i < subset.length; i++) {
        if (subset[i]) {
          premise.add(items.get(i));
        }
      }
      return premise;
    }
    
    private static Collection<BinaryItem> getConsequence(FrequentBinaryItemSet fis,
        boolean[] subset) {
      List<BinaryItem> consequence = new ArrayList<BinaryItem>();
      ArrayList<BinaryItem> items = new ArrayList<BinaryItem>(fis.getItems());
      
      for (int i = 0; i < subset.length; i++) {
        if (!subset[i]) {
          consequence.add(items.get(i));
        }
      }
      return consequence;
    }
    
    
    /**
     * Generate all association rules, from the supplied frequet item sets,
     * that meet a given minimum metric threshold. Uses a brute force approach.
     * 
     * @param largeItemSets the set of frequent item sets
     * @param metricToUse the metric to use
     * @param metricThreshold the threshold value that a rule must meet
     * @param totalTransactions the total number of transactions in the data
     * @return a list of association rules
     */
    public static List<AssociationRule> 
      generateRulesBruteForce(FrequentItemSets largeItemSets, METRIC_TYPE metricToUse, 
          double metricThreshold, int totalTransactions) {
      
      List<AssociationRule> rules = new ArrayList<AssociationRule>();
      largeItemSets.sort();
      Map<Collection<BinaryItem>, Integer> frequencyLookup =
        new HashMap<Collection<BinaryItem>, Integer>();
      
      Iterator<FrequentBinaryItemSet> setI = largeItemSets.iterator();
      // process each large item set
      while (setI.hasNext()) {
        FrequentBinaryItemSet fis = setI.next();
        frequencyLookup.put(fis.getItems(), fis.getSupport());
        if (fis.getItems().size() > 1) {
          // generate all the possible subsets for the premise
          boolean[] subset = new boolean[fis.getItems().size()];
          Collection<BinaryItem> premise = null;
          Collection<BinaryItem> consequence = null;
          while ((premise = getPremise(fis, subset)) != null) {
            if (premise.size() > 0 && premise.size() < fis.getItems().size()) {
              consequence = getConsequence(fis, subset);
              int totalSupport = fis.getSupport();
              int supportPremise = frequencyLookup.get(premise).intValue();
              int supportConsequence = frequencyLookup.get(consequence).intValue();
              
              // a candidate rule
              AssociationRule candidate = 
                new AssociationRule(premise, consequence, metricToUse, supportPremise,
                    supportConsequence, totalSupport, totalTransactions);
              if (candidate.getMetricValue() > metricThreshold) {
                // accept this rule
                rules.add(candidate);
              }              
            }
            nextSubset(subset);
          }
        }
      }
      
      return rules;
    }
  }
  
  /** The number of rules to find */
  protected int m_numRulesToFind = 10;
  //protected double m_upperBoundMinSupport = 0.36;
  
  /** The upper bound on the minimum support */
  protected double m_upperBoundMinSupport = 1.0;
  
  /** The lower bound on minimum support */
  protected double m_lowerBoundMinSupport = 0.1;
  
  /** The amount by which to decrease the support in each iteration */
  protected double m_delta = 0.05;
  
  /** 
   * If true, just all rules meeting the lower bound on the minimum
   * support will be found. The number of rules to find will be
   * ignored and the iterative reduction of support will not
   * be done. 
   */
  protected boolean m_findAllRulesForSupportLevel = false;
  
  //protected double m_lowerBoundMinSupport = 0.0;
  
  /** The index (1 based) of binary attributes to treat as the positive value */
  protected int m_positiveIndex = 2;
  
  protected AssociationRule.METRIC_TYPE m_metric = 
    AssociationRule.METRIC_TYPE.CONFIDENCE;
  
  protected double m_metricThreshold = 0.9;
  
  /** Holds the large item sets found */
  protected FrequentItemSets m_largeItemSets;
  
  /** Holds the rules */
  protected List<AssociationRule> m_rules;
  
  // maximum number of items in a large item set (zero means no limit)
  protected int m_maxItems = -1;
  
  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // enable what we can handle
    
    // attributes
    result.enable(Capability.UNARY_ATTRIBUTES);
    result.enable(Capability.BINARY_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    result.enable(Capability.NO_CLASS);
    
    return result;
  }
  
  /**
   * Returns a string describing this associator
   * 
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class implementing the FP-growth algorithm for finding" +
    		" large item sets without candidate generation. Iteratively" +
    		" reduces the minimum support until it finds the required" +
    		" number of rules with the given minimum metric." +
    		" For more information see:\n\n" +
    		getTechnicalInformation().toString();
  }
  
  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation        result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "J. Han and J.Pei and Y. Yin");
    result.setValue(Field.TITLE, "Mining frequent patterns without candidate generation");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 2000 ACM-SIGMID International" +
    		" Conference on Management of Data");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.PAGES, "1-12");
    
    return result;
  }
  
  
  /**
   * Get the singleton items in the data
   * 
   * @param data the Instances to process
   * @return a list of singleton item sets
   * @throws Exception if the singletons can't be found for some reason
   */
  protected ArrayList<BinaryItem> getSingletons(Instances data) throws Exception {
    ArrayList<BinaryItem> singletons = new ArrayList<BinaryItem>();
    
    for (int i = 0; i < data.numAttributes(); i++) {
      singletons.add(new BinaryItem(data.attribute(i), m_positiveIndex - 1));
    }
    
    for (int i = 0; i < data.numInstances(); i++) {
      Instance current = data.instance(i);
      if (current instanceof SparseInstance) {
        for (int j = 0; j < current.numValues(); j++) {
          int attIndex = current.index(j);
          singletons.get(attIndex).increaseFrequency();
        }
      } else {
        for (int j = 0; j < data.numAttributes(); j++) {
          if (!current.isMissing(j)) {
            if (current.attribute(j).numValues() == 1 
                || current.value(j) == m_positiveIndex - 1) {
              singletons.get(j).increaseFrequency();
            }
          }
        }
      }
    }
    
    return singletons;
  }
  
  /*protected ArrayList<BinaryItem> getFrequent(ArrayList<BinaryItem> items, int minSupport) {
    ArrayList<BinaryItem> frequent = new ArrayList<BinaryItem>();
    for (BinaryItem b : items) {
      if (b.getFrequency() > minSupport) {
        frequent.add(b);
      }
    }
    
    // sort in descending order of support
    Collections.sort(frequent);
    return frequent;
  } */
  
  /**
   * Construct the frequent pattern tree by inserting each transaction
   * in the data into the tree. Only those items from each transaction that
   * meet the minimum support threshold are inserted.
   * 
   * @param singletons the singleton item sets
   * @param data the Instances containing the transactions
   * @param minSupport the minimum support
   * @return the root of the tree
   */
  protected FPTreeRoot buildFPTree(ArrayList<BinaryItem> singletons, 
      Instances data, int minSupport) {
    
    FPTreeRoot tree = new FPTreeRoot();
   
    for (int i = 0; i < data.numInstances(); i++) {
      Instance current = data.instance(i);
      ArrayList<BinaryItem> transaction = new ArrayList<BinaryItem>();
      if (current instanceof SparseInstance) {
        for (int j = 0; j < current.numValues(); j++) {
          int attIndex = current.index(j);
          if (singletons.get(attIndex).getFrequency() >= minSupport) {
            transaction.add(singletons.get(attIndex));
          }
        }
        Collections.sort(transaction);
        tree.addItemSet(transaction, 1);
      } else {
        for (int j = 0; j < data.numAttributes(); j++) {
          if (!current.isMissing(j)) {
            if (current.attribute(j).numValues() == 1 
                || current.value(j) == m_positiveIndex - 1) {
              if (singletons.get(j).getFrequency() >= minSupport) {
                transaction.add(singletons.get(j));
              }
            }
          }
        }
        Collections.sort(transaction);
        tree.addItemSet(transaction, 1);
      }
    }
    
    return tree;
  }
  
  /**
   * Find large item sets in the FP-tree.
   * 
   * @param tree the root of the tree to mine
   * @param largeItemSets holds the large item sets found
   * @param recursionLevel the recursion level for the current projected
   * counts
   * @param conditionalItems the current set of items that the current
   * (projected) tree is conditional on
   * @param minSupport the minimum acceptable support
   */
  protected void mineTree(FPTreeRoot tree, FrequentItemSets largeItemSets, 
      int recursionLevel, FrequentBinaryItemSet conditionalItems, int minSupport) {
    
    if (!tree.isEmpty(recursionLevel)) {
      if (m_maxItems > 0 && recursionLevel >= m_maxItems) {
        // don't mine any further
        return;
      }
      
      Map<BinaryItem, FPTreeRoot.Header> headerTable = tree.getHeaderTable();
      Set<BinaryItem> keys = headerTable.keySet();
//      System.err.println("Number of freq item sets collected " + largeItemSets.size());
      Iterator<BinaryItem> i = keys.iterator();
      while (i.hasNext()) {
        BinaryItem item = i.next();
        FPTreeRoot.Header itemHeader = headerTable.get(item);
        
        // check for minimum support at this level
        int support = itemHeader.getProjectedCounts().getCount(recursionLevel);
        if (support >= minSupport) {          
          // process header list at this recursion level
          for (FPTreeNode n : itemHeader.getHeaderList()) {
            // push count up path to root
            int currentCount = n.getProjectedCount(recursionLevel);
            if (currentCount > 0) {                            
              FPTreeNode temp = n.getParent();
              while (temp != tree) {
                // set/increase for the node
                temp.increaseProjectedCount(recursionLevel + 1, currentCount);

                // set/increase for the header table
                headerTable.get(temp.getItem()).
                getProjectedCounts().increaseCount(recursionLevel + 1, currentCount);
                
                temp = temp.getParent();
              }
            }
          }
          
          FrequentBinaryItemSet newConditional = 
            (FrequentBinaryItemSet) conditionalItems.clone();
          
          // this item gets added to the conditional items
          newConditional.addItem(item);
          newConditional.setSupport(support);
          
          // now add this conditional item set to the list of large item sets
          largeItemSets.addItemSet(newConditional);
          
          // now recursively process the new tree
          mineTree(tree, largeItemSets, recursionLevel + 1, newConditional,
              minSupport);
          
          // reverse the propagated counts
          for (FPTreeNode n : itemHeader.getHeaderList()) {
            FPTreeNode temp = n.getParent();
            while (temp != tree) {
              temp.removeProjectedCount(recursionLevel + 1);
              temp = temp.getParent();
            }
          }
          
          // reverse the propagated counts in the header list
          // at this recursion level
          for (FPTreeRoot.Header h : headerTable.values()) {
            h.getProjectedCounts().removeCount(recursionLevel + 1);
          }          
        }
      }
    }
  }
  
  /**
   * Construct a new FPGrowth object.
   */
  public FPGrowth() {
    resetOptions();
  }
  
  /**
   * Reset all options to their default values.
   */
  public void resetOptions() {
    m_delta = 0.05;
    m_metricThreshold = 0.9;
    m_numRulesToFind = 10;
    m_lowerBoundMinSupport = 0.1;
    m_upperBoundMinSupport = 1.0;
//    m_minSupport = -1;
    m_positiveIndex = 2;
  }
  
  /**
   * Tip text for this property suitable for displaying
   * in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String positiveIndexTipText() {
    return "Set the index of binary valued attributes that is to be considered" +
    		" the positive index. Has no effect for sparse data (in this case" +
    		" the first index (i.e. non-zero values) is always treated as " +
    		" positive. Also has no effect for unary valued attributes (i.e." +
    		" when using the Weka Apriori-style format for market basket data," +
    		" which uses missing value \"?\" to indicate" +
    		" absence of an item.";
  }
  
  /**
   * Set the index of the attribute value to consider as positive
   * for binary attributes in normal dense instances. Index 1 is always
   * used for sparse instances.
   * 
   * @param index the index to use for positive values in binary attributes.
   */
  public void setPositiveIndex(int index) {
    m_positiveIndex = index;
  }
  
  /**
   * Get the index of the attribute value to consider as positive
   * for binary attributes in normal dense instances. Index 1 is always
   * used for sparse instances.
   * 
   * @return the index to use for positive values in binary attributes.
   */
  public int getPositiveIndex() {
    return m_positiveIndex;
  }
  
  /**
   * Set the desired number of rules to find.
   * 
   * @param numR the number of rules to find.
   */
  public void setNumRulesToFind(int numR) {
    m_numRulesToFind = numR;
  }
  
  /**
   * Get the number of rules to find.
   * 
   * @return the number of rules to find.
   */
  public int getNumRulesToFind() {
    return m_numRulesToFind;
  }
  
  /**
   * Tip text for this property suitable for displaying
   * in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String numRulesToFindTipText() {
    return "The number of rules to output";
  }
  
  /**
   * Set the metric type to use.
   * 
   * @param d the metric type
   */
  public void setMetricType(SelectedTag d) {
    int ordinal =  d.getSelectedTag().getID();
    for (AssociationRule.METRIC_TYPE m : AssociationRule.METRIC_TYPE.values()) {
      if (m.ordinal() == ordinal) {
        m_metric = m;
        break;
      }
    }
  }
  
  /**
   * Set the maximum number of items to include in large items sets.
   * 
   * @param max the maxim number of items to include in large item sets.
   */
  public void setMaxNumberOfItems(int max) {
    m_maxItems = max;
  }
  
  /**
   * Gets the maximum number of items to be included in large item sets.
   * 
   * @return the maximum number of items to be included in large items sets.
   */
  public int getMaxNumberOfItems() {
    return m_maxItems;
  }
  
  /**
   * Tip text for this property suitable for displaying
   * in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String maxNumberOfItemsTipText() {
    return "The maximum number of items to include in frequent item sets. -1 " +
    		"means no limit.";
  }
  
  /**
   * Get the metric type to use.
   * 
   * @return the metric type to use.
   */
  public SelectedTag getMetricType() {
    return new SelectedTag(m_metric.ordinal(), AssociationRule.TAGS_SELECTION);
  }
  
  /**
   * Tip text for this property suitable for displaying
   * in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String metricTypeTipText() {
    return "Set the type of metric by which to rank rules. Confidence is "
    +"the proportion of the examples covered by the premise that are also "
    +"covered by the consequence(Class association rules can only be mined using confidence). Lift is confidence divided by the "
    +"proportion of all examples that are covered by the consequence. This "
    +"is a measure of the importance of the association that is independent "
    +"of support. Leverage is the proportion of additional examples covered "
    +"by both the premise and consequence above those expected if the "
    +"premise and consequence were independent of each other. The total "
    +"number of examples that this represents is presented in brackets "
    +"following the leverage. Conviction is "
    +"another measure of departure from independence.";
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minMetricTipText() {
    return "Minimum metric score. Consider only rules with scores higher than "
      +"this value.";
  }

  /**
   * Get the value of minConfidence.
   *
   * @return Value of minConfidence.
   */
  public double getMinMetric() {
    
    return m_metricThreshold;
  }
  
  /**
   * Set the value of minConfidence.
   *
   * @param v  Value to assign to minConfidence.
   */
  public void setMinMetric(double v) {
    
    m_metricThreshold = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String deltaTipText() {
    return "Iteratively decrease support by this factor. Reduces support "
      +"until min support is reached or required number of rules has been "
      +"generated.";
  }
    
  /**
   * Get the value of delta.
   *
   * @return Value of delta.
   */
  public double getDelta() {
    
    return m_delta;
  }
  
  /**
   * Set the value of delta.
   *
   * @param v  Value to assign to delta.
   */
  public void setDelta(double v) {
    
    m_delta = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lowerBoundMinSupportTipText() {
    return "Lower bound for minimum support.";
  }

  /**
   * Get the value of lowerBoundMinSupport.
   *
   * @return Value of lowerBoundMinSupport.
   */
  public double getLowerBoundMinSupport() {
    
    return m_lowerBoundMinSupport;
  }
  
  /**
   * Set the value of lowerBoundMinSupport.
   *
   * @param v  Value to assign to lowerBoundMinSupport.
   */
  public void setLowerBoundMinSupport(double v) {
    
    m_lowerBoundMinSupport = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String upperBoundMinSupportTipText() {
    return "Upper bound for minimum support. Start iteratively decreasing "
      +"minimum support from this value.";
  }

  /**
   * Get the value of upperBoundMinSupport.
   *
   * @return Value of upperBoundMinSupport.
   */
  public double getUpperBoundMinSupport() {
    
    return m_upperBoundMinSupport;
  }
  
  /**
   * Set the value of upperBoundMinSupport.
   *
   * @param v  Value to assign to upperBoundMinSupport.
   */
  public void setUpperBoundMinSupport(double v) {
    
    m_upperBoundMinSupport = v;
  }
  
  /**
   * Tip text for this property suitable for displaying
   * in the GUI.
   * 
   * @return the tip text for this property.
   */
  public String findAllRulesForSupportLevelTipText() {
    return "Find all rules that meet " +
    "the lower bound on minimum support and the minimum metric constraint. " +
    "Turning this mode on will disable the iterative support reduction " +
    "procedure to find the specified number of rules.";
  }
  
  /**
   * If true then turn off the iterative support reduction method
   * of finding x rules that meet the minimum support and metric
   * thresholds and just return all the rules that meet the
   * lower bound on minimum support and the minimum metric.
   * 
   * @param s true if all rules meeting the lower bound on the support
   * and minimum metric thresholds are to be found.
   */
  public void setFindAllRulesForSupportLevel(boolean s) {
    m_findAllRulesForSupportLevel = s;
  }
  
  /**
   * Get whether all rules meeting the lower bound on min support
   * and the minimum metric threshold are to be found.
   * 
   * @return true if all rules meeting the lower bound on min
   * support and the min metric threshold are to be found.
   */
  public boolean getFindAllRulesForSupportLevel() {
    return m_findAllRulesForSupportLevel;
  }
  
  /* public void setMinimumSupport(double minSupp) {
    m_minSupport = minSupp;
  }
  
  public double getMinimumSupport() {
    return m_minSupport;
  } */    
  
  /**
   * Gets the list of mined association rules.
   * 
   * @return the list of association rules discovered during mining.
   * Returns null if mining hasn't been performed yet.
   */
  public List<AssociationRule> getAssociationRules() {
    return m_rules;
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>();
    
    String string00 = "\tSet the index of the attribute value to consider as 'positive'\n\t"
   + "for binary attributes in normal dense instances. Index 2 is always\n\t"
   + "used for sparse instances. (default = 2)";    
    String string0 = "\tThe maximum number of items to include " +
    		"in large items sets (and rules). (default " +
    		"= -1, i.e. no limit.)"; 
      
    String string1 = "\tThe required number of rules. (default = " 
      + m_numRulesToFind + ")";
    String string2 = "\tThe minimum metric score of a rule. (default" +
    		" = " + m_metricThreshold + ")";
    String string3 = "\tThe metric by which to rank rules. (default"
      + " = confidence)";
    String string4 = "\tThe lower bound for the minimum support. (default = "
      + m_lowerBoundMinSupport + ")";
    String string5 = "\tUpper bound for minimum support. "
      + "(default = 1.0)";
    String string6 = "\tThe delta by which the minimum support is decreased in\n"
     + "\teach iteration. (default = " + m_delta + ")";
    String string7 = "\tFind all rules that meet the lower bound on\n\t" +
    		"minimum support and the minimum metric constraint.\n\t" +
    		"Turning this mode on will disable the iterative support reduction\n\t" +
    		"procedure to find the specified number of rules.";
    
    newVector.add(new Option(string00, "P", 1, "-P <attribute index of positive value>"));
    newVector.add(new Option(string0, "I", 1, "-I <max items>"));
    newVector.add(new Option(string1, "N", 1, "-N <require number of rules>"));
    newVector.add(new Option(string3, "T", 1, "-T <0=confidence | 1=lift | "
                                    + "2=leverage | 3=Conviction>"));
    newVector.add(new Option(string2, "C", 1, "-C <minimum metric score of a rule>"));
    newVector.add(new Option(string5, "U", 1, "-U <upper bound for minimum support>"));
    newVector.add(new Option(string4, "M", 1, "-M <lower bound for minimum support>"));
    newVector.add(new Option(string6, "D", 1, "-D <delta for minimum support>"));
    newVector.add(new Option(string7, "S", 0, "-S"));
    
    return newVector.elements();
  }
  
  /**
   * 
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -P &lt;attribute index of positive value&gt;
   *  Set the index of the attribute value to consider as 'positive'
   *  for binary attributes in normal dense instances. Index 2 is always
   *  used for sparse instances. (default = 2)</pre>
   * 
   * <pre> -I &lt;max items&gt;
   *  The maximum number of items to include in large items sets (and rules). (default = -1, i.e. no limit.)</pre>
   * 
   * <pre> -N &lt;require number of rules&gt;
   *  The required number of rules. (default = 10)</pre>
   * 
   * <pre> -T &lt;0=confidence | 1=lift | 2=leverage | 3=Conviction&gt;
   *  The metric by which to rank rules. (default = confidence)</pre>
   * 
   * <pre> -C &lt;minimum metric score of a rule&gt;
   *  The minimum metric score of a rule. (default = 0.9)</pre>
   * 
   * <pre> -U &lt;upper bound for minimum support&gt;
   *  Upper bound for minimum support. (default = 1.0)</pre>
   * 
   * <pre> -M &lt;lower bound for minimum support&gt;
   *  The lower bound for the minimum support. (default = 0.1)</pre>
   * 
   * <pre> -D &lt;delta for minimum support&gt;
   *  The delta by which the minimum support is decreased in
   *  each iteration. (default = 0.05)</pre>
   * 
   * <pre> -S
   *  Find all rules that meet the lower bound on
   *  minimum support and the minimum metric constraint.
   *  Turning this mode on will disable the iterative support reduction
   *  procedure to find the specified number of rules.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    resetOptions();
    String positiveIndexString = Utils.getOption('P', options);
    String maxItemsString = Utils.getOption('I', options);
    String numRulesString = Utils.getOption('N', options);
    String minMetricString = Utils.getOption('C', options);
    String metricTypeString = Utils.getOption("T", options);
    String lowerBoundSupportString = Utils.getOption("M", options);
    String upperBoundSupportString = Utils.getOption("U", options);
    String deltaString = Utils.getOption("D", options);

    if (positiveIndexString.length() != 0) {
      setPositiveIndex(Integer.parseInt(positiveIndexString));
    }
    
    if (maxItemsString.length() != 0) {
      setMaxNumberOfItems(Integer.parseInt(maxItemsString));
    }
    
    if (metricTypeString.length() != 0) {
      setMetricType(new SelectedTag(Integer.parseInt(metricTypeString),
          AssociationRule.TAGS_SELECTION));
    }
    
    if (numRulesString.length() != 0) {
      setNumRulesToFind(Integer.parseInt(numRulesString));
    }
    
    if (minMetricString.length() != 0) {
      setMinMetric(Double.parseDouble(minMetricString));
    }
    
    if (deltaString.length() != 0) {
      setDelta(Double.parseDouble(deltaString));
    }
    
    if (lowerBoundSupportString.length() != 0) {
      setLowerBoundMinSupport(Double.parseDouble(lowerBoundSupportString));
    }
    
    if (upperBoundSupportString.length() != 0) {
      setUpperBoundMinSupport(Double.parseDouble(upperBoundSupportString));
    }
    
    setFindAllRulesForSupportLevel(Utils.getFlag('S', options));
  }
  
  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();
    
    options.add("-P"); options.add("" + getPositiveIndex());
    options.add("-I"); options.add("" + getMaxNumberOfItems());
    options.add("-N"); options.add("" + getNumRulesToFind());
    options.add("-T"); options.add("" + getMetricType().getSelectedTag().getID());
    options.add("-C"); options.add("" + getMinMetric());
    options.add("-D"); options.add("" + getDelta());
    options.add("-U"); options.add("" + getUpperBoundMinSupport());
    options.add("-M"); options.add("" + getLowerBoundMinSupport());
    if (getFindAllRulesForSupportLevel()) {
      options.add("-S");
    }
    
    return options.toArray(new String[1]);
  }

  /**
   * Method that generates all large item sets with a minimum support, and from
   * these all association rules with a minimum metric (i.e. confidence, 
   * lift etc.).
   *
   * @param data the instances to be used for generating the associations
   * @throws Exception if rules can't be built successfully
   */
  public void buildAssociations(Instances data) throws Exception {
    
    // can we handle the data?
    getCapabilities().testWithFail(data);
    
    double currentSupport = m_upperBoundMinSupport;
    
    if (m_findAllRulesForSupportLevel) {
      currentSupport = m_lowerBoundMinSupport;
    }
    // first compute singletons
    ArrayList<BinaryItem> singletons = getSingletons(data);
    //ArrayList<BinaryItem> singletonsCopy = new ArrayList<BinaryItem>(singletons);
/*    Collections.sort(singletonsCopy);
    for (int i = 0; i < singletonsCopy.size(); i++) {
      System.out.println(singletonsCopy.get(i).toString(true));
    }
    System.out.println("---------"); */
//    System.out.println("Finished finding singletons...");
    
    // while not enough rules
    do {
      int currentSupportAsInstances = (currentSupport > 1)
      ? (int)currentSupport
      : (int)Math.ceil(currentSupport * data.numInstances());
      
      //System.err.println("Current support " + currentSupportAsInstances);
      //ArrayList<BinaryItem> prunedSingletons = removeNonFrequent(singletons);

      // build the FPTree
      FPTreeRoot tree = buildFPTree(singletons, data, currentSupportAsInstances);
//      System.out.println("Finished building tree...");
//      System.out.println(tree.toString(0));
    /*System.out.println(tree.printHeaderTable(0)); */

      FrequentItemSets largeItemSets = new FrequentItemSets(data.numInstances());

      // mine the tree
      FrequentBinaryItemSet conditionalItems = 
        new FrequentBinaryItemSet(new ArrayList<BinaryItem>(), 0);
      mineTree(tree, largeItemSets, 0, conditionalItems, currentSupportAsInstances);

      m_largeItemSets = largeItemSets;
//         System.err.println("Number of large item sets: " + m_largeItemSets.size());
  //    System.err.println(m_largeItemSets.toString(100));

      //    m_largeItemSets.sort(compF);
//      System.err.println("Finished mining tree...");
      
      // save memory
      tree = null;
      
      m_rules = 
        AssociationRule.generateRulesBruteForce(m_largeItemSets, m_metric, 
            m_metricThreshold, data.numInstances());
      
      if (!m_findAllRulesForSupportLevel) {
        currentSupport -= m_delta;
        if (currentSupport < m_lowerBoundMinSupport) {
          if (currentSupport + m_delta > m_lowerBoundMinSupport) {
            // ensure that the lower bound does get evaluated
            currentSupport = m_lowerBoundMinSupport;
          } else {
            break;
          }
        }
      } else {
        // just break out of the loop as we are just finding all rules
        // with a minimum support + metric
        break;
      }
    } while (m_rules.size() < m_numRulesToFind);
    
    Collections.sort(m_rules);
//    for (AssociationRule)
    
//    System.out.println(graph(tree));
  }
    
  /**
   * Output the association rules.
   * 
   * @return a string representation of the model.
   */
  public String toString() {
//    return m_largeItemSets.toString(m_numItemSetsToFind);
    if (m_rules == null) {
      return "FPGrowth hasn't been trained yet!";
    }

    StringBuffer result = new StringBuffer();
    int numRules = (m_rules.size() < m_numRulesToFind)
      ? m_rules.size()
      : m_numRulesToFind;
      
    if (m_rules.size() == 0) {
      return "No rules found!";
    } else {      
      result.append("FPGrowth found " + m_rules.size() + " rules");
      if (!m_findAllRulesForSupportLevel) {
        result.append(" (displaying top " + numRules + ")");
      }
      result.append("\n\n");
    }

    int count = 0;
    for (AssociationRule r : m_rules) {
      result.append(Utils.doubleToString((double)count+1,
          (int)(Math.log(numRules)/Math.log(10)+1), 0) + ". ");
      result.append(r + "\n");
      count++;
      if (!m_findAllRulesForSupportLevel && count == m_numRulesToFind) {
        break;
      }
    }
    return result.toString();
  }
  
  /**
   * Assemble a dot graph representation of the FP-tree.
   * 
   * @param tree the root of the FP-tree
   * @return a graph representation as a String in dot format.
   */
  public String graph(FPTreeRoot tree) {
    //int maxID = tree.assignIDs(-1);
    
    
    StringBuffer text = new StringBuffer();
    text.append("digraph FPTree {\n");
    text.append("N0 [label=\"ROOT\"]\n");
    tree.graphFPTree(text);
    
//    tree.graphHeaderTable(text, maxID+1);
    text.append("}\n");
    
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return            the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
  
  /**
   * Main method.
   * 
   * @param args the commandline options
   */
  public static void main(String[] args) {
    runAssociator(new FPGrowth(), args);
  }
}

