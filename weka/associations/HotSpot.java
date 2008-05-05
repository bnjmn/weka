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
 *    alo0ng with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    HotSpot.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.associations;

import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.SingleIndex;
import weka.core.Drawable;
import weka.core.Capabilities.Capability;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 <!-- globalinfo-start -->
 * HotSpot learns a set of rules (displayed in a tree-like structure) <br>
 * that maximize/minimize a target variable/value of interest. <br>
 * With a nominal target, one might want to look for segments of the <br>
 * data where there is a high probability of a minority value occuring (<br>
 * given the constraint of a minimum support). For a numeric target, <br>
 * one might be interested in finding segments where this is higher <br>
 * on average than in the whole data set. For example, in a health <br>
 * insurance scenario, find which health insurance groups are at <br>
 * the highest risk (have the highest claim ratio), or, which groups <br>
 * have the highest average insurance payout. <br>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -c &lt;num | first | last&gt;
 * The target index. (default = last)
 * </pre>
 * 
 * <pre> -V &lt;num | first | last&gt;
 *  The target value (nominal target only, default = first)
 * </pre>
 *
 * <pre> -L
 * Minimize rather than maximize
 * </pre>
 *
 * <pre> -S &lt;num&gt;
 *  Minimum value count (nominal target)/segment size (numeric target). 
 *  Values between 0 and 1 are interpreted
 *  as a percentage of the total population; values > 1 are
 *  interpreted as an absolute number of instances
 *  (default = 0.3)
 * </pre>
 *
 * <pre> -M &lt;num&gt;
 * Maximum branching factor. The maximum number of children
 * to consider extending each node with. (default = 2)
 * </pre>
 *
 * <pre> -I &lt;num&gt;
 * Minimum improvement in target value in order to
 * consider adding a new branch/test (default = 0.01 (1%))
 * </pre>
 *
 * <pre> -D
 * Output debugging info (duplicate rule lookup hash table stats)
 * </pre>
 *
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org
 * @version $Revision: 1.1 $
 */
public class HotSpot
  implements Associator, OptionHandler, RevisionHandler, 
             CapabilitiesHandler, Drawable, Serializable {

  static final long serialVersionUID = 42972325096347677L;

  /** index of the target attribute */
  protected SingleIndex m_targetSI = new SingleIndex("last");
  protected int m_target;
  
  /** Support as a fraction of the total training set */
  protected double m_support;
  
  /** Support as an instance count */
  private int m_supportCount;

  /** The global value of the attribute of interest (mean or probability) */
  protected double m_globalTarget;

  /** The minimum improvement necessary to justify adding a test */
  protected double m_minImprovement;

  /** Actual global support of the target value (discrete target only) */
  protected int m_globalSupport;

  /** For discrete target, the index of the value of interest */
  protected SingleIndex m_targetIndexSI = new SingleIndex("first");
  protected int m_targetIndex;

  /** At each level of the tree consider at most this number extensions */
  protected int m_maxBranchingFactor;

  /** Number of instances in the full data */
  protected int m_numInstances;

  /** The head of the tree */
  protected HotNode m_head;

  /** Header of the training data */
  protected Instances m_header;

  /** Debugging stuff */
  protected int m_lookups = 0;
  protected int m_insertions = 0;
  protected int m_hits = 0;

  protected boolean m_debug;
  
  /** Minimize, rather than maximize the target */
  protected boolean m_minimize;

  /** Error messages relating to too large/small support values */
  protected String m_errorMessage;

  /** Rule lookup table */
  protected HashMap<HotSpotHashKey, String> m_ruleLookup;

  /**
   * Constructor
   */
  public HotSpot() {
    resetOptions();
  }

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "HotSpot learns a set of rules (displayed in a tree-like structure) "
      + "that maximize/minimize a target variable/value of interest. "
      + "With a nominal target, one might want to look for segments of the "
      + "data where there is a high probability of a minority value occuring ("
      + "given the constraint of a minimum support). For a numeric target, "
      + "one might be interested in finding segments where this is higher "
      + "on average than in the whole data set. For example, in a health "
      + "insurance scenario, find which health insurance groups are at "
      + "the highest risk (have the highest claim ratio), or, which groups "
      + "have the highest average insurance payout.";
  }
  
  /**
   * Returns default capabilities of HotSpot
   *
   * @return      the capabilities of HotSpot
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.NOMINAL_CLASS);

    
    return result;
  }

  /**
   * Hash key class for sets of attribute, value tests
   */
  protected class HotSpotHashKey {
    // split values, one for each attribute (0 indicates att not used).
    // for nominal indexes, 1 is added so that 0 can indicate not used.
    protected double[] m_splitValues;

    // 0 = not used, 1 = "<=", 2 = "=", 3 = ">"
    protected byte[] m_testTypes;

    protected boolean m_computed = false;
    protected int m_key;
    
    public HotSpotHashKey(double[] splitValues, byte[] testTypes) {
      m_splitValues = splitValues.clone();
      m_testTypes = testTypes.clone();
    }

    public boolean equals(Object b) {
      if ((b == null) || !(b.getClass().equals(this.getClass()))) {
        return false;
      }
      HotSpotHashKey comp = (HotSpotHashKey)b;
      boolean ok = true;
      for (int i = 0; i < m_splitValues.length; i++) {
        if (m_splitValues[i] != comp.m_splitValues[i] ||
            m_testTypes[i] != comp.m_testTypes[i]) {
          ok = false;
          break;
        }
      }
      return ok;
    }

    public int hashCode() {

      if (m_computed) {
        return m_key;
      } else {
        int hv = 0;
        for (int i = 0; i < m_splitValues.length; i++) {
          hv += (m_splitValues[i] * 5 * i);
          hv += (m_testTypes[i] * i * 3);
        }
        m_computed = true;

        m_key = hv;
      }
      return m_key;
    }
  }

  /**
   * Build the tree
   *
   * @param instances the training instances
   * @throws Exception if something goes wrong
   */
  public void buildAssociations(Instances instances) throws Exception {

    m_errorMessage = null;
    m_targetSI.setUpper(instances.numAttributes() - 1);
    m_target = m_targetSI.getIndex();
    Instances inst = new Instances(instances);
    inst.setClassIndex(m_target);
    inst.deleteWithMissingClass();

    // can associator handle the data?
    getCapabilities().testWithFail(inst);

    if (inst.attribute(m_target).isNominal()) {
      m_targetIndexSI.setUpper(inst.attribute(m_target).numValues() - 1);
      m_targetIndex = m_targetIndexSI.getIndex();
    } else {
      m_targetIndexSI.setUpper(1); // just to stop this SingleIndex from moaning
    }
    
    if (m_support <= 0) {
      throw new Exception("Support must be greater than zero.");
    }

    m_numInstances = inst.numInstances();
    if (m_support >= 1) {
      m_supportCount = (int)m_support;
      m_support = m_support / (double)m_numInstances;
    }
    m_supportCount = (int)Math.floor((m_support * m_numInstances) + 0.5d);
    //    m_supportCount = (int)(m_support * m_numInstances);
    if (m_supportCount < 1) {
      m_supportCount = 1;
    }

    m_header = new Instances(inst, 0);

    if (inst.attribute(m_target).isNumeric()) {
      if (m_supportCount > m_numInstances) {
        m_errorMessage = "Error: support set to more instances than there are in the data!";
        return;
      }
      m_globalTarget = inst.meanOrMode(m_target);
    } else {
      double[] probs = new double[inst.attributeStats(m_target).nominalCounts.length];
      for (int i = 0; i < probs.length; i++) {
        probs[i] = (double)inst.attributeStats(m_target).nominalCounts[i];
      }
      m_globalSupport = (int)probs[m_targetIndex];
      // check that global support is greater than min support
      if (m_globalSupport < m_supportCount) {
        m_errorMessage = "Error: minimum support " + m_supportCount 
          + " is too high. Target value " 
          + m_header.attribute(m_target).value(m_targetIndex) + " has support " 
          + m_globalSupport + ".";
      }

      Utils.normalize(probs);
      m_globalTarget = probs[m_targetIndex];
      /*      System.err.println("Global target " + m_globalTarget); 
              System.err.println("Min support count " + m_supportCount);  */
    }
    
    m_ruleLookup = new HashMap<HotSpotHashKey, String>();
    double[] splitVals = new double[m_header.numAttributes()];
    byte[] tests = new byte[m_header.numAttributes()];

    m_head = new HotNode(inst, m_globalTarget, splitVals, tests);
    //    m_head = new HotNode(inst, m_globalTarget);
  }

  /**
   * Return the tree as a string
   *
   * @return a String containing the tree
   */
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("\nHot Spot\n========");
    if (m_errorMessage != null) {
      buff.append("\n\n" + m_errorMessage + "\n\n");
      return buff.toString();
    }
    if (m_head == null) {
      buff.append("No model built!");
      return buff.toString();
    }
    buff.append("\nTotal population: ");
    buff.append("" + m_numInstances + " instances");
    buff.append("\nTarget attribute: " + m_header.attribute(m_target).name());
    if (m_header.attribute(m_target).isNominal()) {
      buff.append("\nTarget value: " + m_header.attribute(m_target).value(m_targetIndex));
      buff.append(" [value count in total population: " + m_globalSupport + " instances ("
                  + Utils.doubleToString((m_globalTarget * 100.0), 2) + "%)]");

      buff.append("\nMinimum value count for segments: ");
    } else {
      buff.append("\nMinimum segment size: ");
    }
    buff.append("" + m_supportCount + " instances (" 
                + Utils.doubleToString((m_support * 100.0), 2) 
                + "% of total population)");
    buff.append("\nMaximum branching factor: " + m_maxBranchingFactor);
    buff.append("\nMinimum improvement in target: " 
                + Utils.doubleToString((m_minImprovement * 100.0), 2) + "%");
    
    buff.append("\n\n");
    buff.append(m_header.attribute(m_target).name());
    if (m_header.attribute(m_target).isNumeric()) {
      buff.append(" (" + Utils.doubleToString(m_globalTarget, 4) + ")");
    } else {
      buff.append("=" + m_header.attribute(m_target).value(m_targetIndex) + " (");
      buff.append("" + Utils.doubleToString((m_globalTarget * 100.0), 2) + "% [");
      buff.append("" + m_globalSupport 
                  + "/" + m_numInstances + "])");
    }
    
    m_head.dumpTree(0, buff);
    buff.append("\n");
    if (m_debug) {
      buff.append("\n=== Duplicate rule lookup hashtable stats ===\n");
      buff.append("Insertions: "+ m_insertions);
      buff.append("\nLookups : "+ m_lookups);
      buff.append("\nHits: "+ m_hits);
      buff.append("\n");
    }
    return buff.toString();
  }

  public String graph() throws Exception {
    System.err.println("Here");
    m_head.assignIDs(-1);

    StringBuffer text = new StringBuffer();
    
    text.append("digraph HotSpot {\n");
    text.append("rankdir=LR;\n");
    text.append("N0 [label=\"" 
                + m_header.attribute(m_target).name());
    
    if (m_header.attribute(m_target).isNumeric()) {
      text.append("\\n(" + Utils.doubleToString(m_globalTarget, 4) + ")");
    } else {
      text.append("=" + m_header.attribute(m_target).value(m_targetIndex) + "\\n(");
      text.append("" + Utils.doubleToString((m_globalTarget * 100.0), 2) + "% [");
      text.append("" + m_globalSupport 
                  + "/" + m_numInstances + "])");
    }
    text.append("\" shape=plaintext]\n");

    m_head.graphHotSpot(text);

    text.append("}\n");
    return text.toString();
  }

  /**
   * Inner class representing a node/leaf in the tree
   */
  protected class HotNode {
    /**
     * An inner class holding data on a particular attribute value test
     */
    protected class HotTestDetails implements Comparable<HotTestDetails> {
      public double m_merit;
      public int m_support;
      public int m_subsetSize;
      public int m_splitAttIndex;
      public double m_splitValue;
      public boolean m_lessThan;

      public HotTestDetails(int attIndex, 
                            double splitVal, 
                            boolean lessThan,
                            int support,
                            int subsetSize,
                            double merit) {
        m_merit = merit;
        m_splitAttIndex = attIndex;
        m_splitValue = splitVal;
        m_lessThan = lessThan;
        m_support = support;
        m_subsetSize = subsetSize;
      }

      // reverse order for maximize as PriorityQueue has the least element at the head
      public int compareTo(HotTestDetails comp) {
        int result = 0;
        if (m_minimize) {
          if (m_merit == comp.m_merit) {
            // larger support is better
            if (m_support == comp.m_support) {
            } else if (m_support > comp.m_support) {
              result = -1;
            } else {
              result = 1;
            }
          } else if (m_merit < comp.m_merit) {
            result = -1;
          } else {
            result = 1;
          }
        } else {
          if (m_merit == comp.m_merit) {
            // larger support is better
            if (m_support == comp.m_support) {
            } else if (m_support > comp.m_support) {
              result = -1;
            } else {
              result = 1;
            }
          } else if (m_merit < comp.m_merit) {
            result = 1;
          } else {
            result = -1;
          }
        }
        return result;
      }
    }

    // the instances at this node
    protected Instances m_insts;

    // the value (to beat) of the target for these instances
    protected double m_targetValue;

    // child nodes
    protected HotNode[] m_children;
    protected HotTestDetails[] m_testDetails;

    public int m_id;

    /**
     * Constructor
     *
     * @param insts the instances at this node
     * @param targetValue the target value
     * @param splitVals the values of attributes split on so far down this branch
     * @param tests the types of tests corresponding to the split values (<=, =, >)
     */
    public HotNode(Instances insts, 
                   double targetValue, 
                   double[] splitVals,
                   byte[] tests) {
      m_insts = insts;
      m_targetValue = targetValue;
      PriorityQueue<HotTestDetails> splitQueue = new PriorityQueue<HotTestDetails>();

      // Consider each attribute
      for (int i = 0; i < m_insts.numAttributes(); i++) {
        if (i != m_target) {
          if (m_insts.attribute(i).isNominal()) {
            evaluateNominal(i, splitQueue);
          } else {
            evaluateNumeric(i, splitQueue);
          }
        }
      }

      if (splitQueue.size() > 0) {
        int queueSize = splitQueue.size();

        // count how many of the potential children are unique
        ArrayList<HotTestDetails> newCandidates = new ArrayList<HotTestDetails>();
        ArrayList<HotSpotHashKey> keyList = new ArrayList<HotSpotHashKey>();

        for (int i = 0; i < queueSize; i++) {
          if (newCandidates.size() < m_maxBranchingFactor) {
            HotTestDetails temp = splitQueue.poll();
            double[] newSplitVals = splitVals.clone();
            byte[] newTests = tests.clone();
            newSplitVals[temp.m_splitAttIndex] = temp.m_splitValue + 1;
            newTests[temp.m_splitAttIndex] = 
              (m_header.attribute(temp.m_splitAttIndex).isNominal())
              ? (byte)2 // ==
              : (temp.m_lessThan)
              ? (byte)1
              : (byte)3;
            HotSpotHashKey key = new HotSpotHashKey(newSplitVals, newTests);
            m_lookups++;
            if (!m_ruleLookup.containsKey(key)) {
              // insert it into the hash table
              m_ruleLookup.put(key, "");            
              newCandidates.add(temp);
              keyList.add(key);
              m_insertions++;
            } else {
              m_hits++;
            }
          } else {
            break;
          }
        }

        m_children = new HotNode[(newCandidates.size() < m_maxBranchingFactor)
                                 ? newCandidates.size()
                                 : m_maxBranchingFactor];
        // save the details of the tests at this node
        m_testDetails = new HotTestDetails[m_children.length];
        for (int i = 0; i < m_children.length; i++) {
          m_testDetails[i] = newCandidates.get(i);
        }

        // save memory
        splitQueue = null;
        newCandidates = null;
        m_insts = new Instances(m_insts, 0);

        // process the children
        for (int i = 0; i < m_children.length; i++) {
          Instances subset = subset(insts, m_testDetails[i]);
          HotSpotHashKey tempKey = keyList.get(i);
          m_children[i] = new HotNode(subset, m_testDetails[i].m_merit, 
                                      tempKey.m_splitValues, tempKey.m_testTypes);

        }
      }
    }

    /**
     * Create a subset of instances that correspond to the supplied test details
     *
     * @param insts the instances to create the subset from
     * @param test the details of the split
     */
    private Instances subset(Instances insts, HotTestDetails test) {
      Instances sub = new Instances(insts, insts.numInstances());
      for (int i = 0; i < insts.numInstances(); i++) {
        Instance temp = insts.instance(i);
        if (!temp.isMissing(test.m_splitAttIndex)) {
          if (insts.attribute(test.m_splitAttIndex).isNominal()) {
            if (temp.value(test.m_splitAttIndex) == test.m_splitValue) {
              sub.add(temp);
            }
          } else {
            if (test.m_lessThan) {
              if (temp.value(test.m_splitAttIndex) <= test.m_splitValue) {
                sub.add(temp);
              }
            } else {
              if (temp.value(test.m_splitAttIndex) > test.m_splitValue) {
                sub.add(temp);
              }
            }
          }
        }
      }
      sub.compactify();
      return sub;
    }

    /**
     * Evaluate a numeric attribute for a potential split
     *
     * @param attIndex the index of the attribute
     * @param pq the priority queue of candidtate splits
     */
    private void evaluateNumeric(int attIndex, PriorityQueue<HotTestDetails> pq) {
      Instances tempInsts = m_insts;
      tempInsts.sort(attIndex);
      
      // target sums/counts
      double targetLeft = 0;
      double targetRight = 0;

      int numMissing = 0;
      // count missing values and sum/counts for the initial right subset
      for (int i = tempInsts.numInstances() - 1; i >= 0; i--) {
        if (!tempInsts.instance(i).isMissing(attIndex)) {
          targetRight += (tempInsts.attribute(m_target).isNumeric())
            ? (tempInsts.instance(i).value(m_target))
            : ((tempInsts.instance(i).value(m_target) == m_targetIndex)
               ? 1
               : 0);
        } else {
          numMissing++;
        }
      }
      
      // are there still enough instances?
      if (tempInsts.numInstances() - numMissing <= m_supportCount) {
        return;
      }
      
      double bestMerit = 0.0;
      double bestSplit = 0.0;
      double bestSupport = 0.0;
      double bestSubsetSize = 0;
      boolean lessThan = true;

      // denominators
      double leftCount = 0;
      double rightCount = tempInsts.numInstances() - numMissing;
            
      /*      targetRight = (tempInsts.attribute(m_target).isNumeric())
        ? tempInsts.attributeStats(m_target).numericStats.sum
        : tempInsts.attributeStats(m_target).nominalCounts[m_targetIndex]; */
      //      targetRight = tempInsts.attributeStats(attIndexnominalCounts[m_targetIndex];

      // consider all splits
      for (int i = 0; i < tempInsts.numInstances() - numMissing; i++) {
        Instance inst = tempInsts.instance(i);

        if (tempInsts.attribute(m_target).isNumeric()) {
          targetLeft += inst.value(m_target);
          targetRight -= inst.value(m_target);
        } else {
          if ((int)inst.value(m_target) == m_targetIndex) {
            targetLeft++;
            targetRight--;
          }          
        }
        leftCount++;
        rightCount--;
        
        // move to the end of any ties
        if (i < tempInsts.numInstances() - 1 &&
            inst.value(attIndex) == tempInsts.instance(i + 1).value(attIndex)) {
          continue;
        }

        // evaluate split
        if (tempInsts.attribute(m_target).isNominal()) {
          if (targetLeft >= m_supportCount) {
            double delta = (m_minimize) 
              ? (bestMerit - (targetLeft / leftCount))
              : ((targetLeft / leftCount) - bestMerit);
            //            if (targetLeft / leftCount > bestMerit) {
            if (delta > 0) {
              bestMerit = targetLeft / leftCount;
              bestSplit = inst.value(attIndex);
              bestSupport = targetLeft;
              bestSubsetSize = leftCount;
              lessThan = true;
              //            } else if (targetLeft / leftCount == bestMerit) {
            } else if (delta == 0) {
              // break ties in favour of higher support
              if (targetLeft > bestSupport) {
                bestMerit = targetLeft / leftCount;
                bestSplit = inst.value(attIndex);
                bestSupport = targetLeft;
                bestSubsetSize = leftCount;
                lessThan = true;
              }
            }
          }

          if (targetRight >= m_supportCount) {
            double delta = (m_minimize) 
              ? (bestMerit - (targetRight / rightCount))
              : ((targetRight / rightCount) - bestMerit);
            //            if (targetRight / rightCount > bestMerit) {
            if (delta > 0) {
              bestMerit = targetRight / rightCount;
              bestSplit = inst.value(attIndex);
              bestSupport = targetRight;
              bestSubsetSize = rightCount;
              lessThan = false;
              //            } else if (targetRight / rightCount == bestMerit) {
            } else if (delta == 0) {
              // break ties in favour of higher support
              if (targetRight > bestSupport) {
                bestMerit = targetRight / rightCount;
                bestSplit = inst.value(attIndex);
                bestSupport = targetRight;
                bestSubsetSize = rightCount;
                lessThan = false;
              }
            }
          } 
        } else {
          if (leftCount >= m_supportCount) {
            double delta = (m_minimize) 
              ? (bestMerit - (targetLeft / leftCount))
              : ((targetLeft / leftCount) - bestMerit);
            //            if (targetLeft / leftCount > bestMerit) {
            if (delta > 0) {
              bestMerit = targetLeft / leftCount;
              bestSplit = inst.value(attIndex);
              bestSupport = leftCount;
              bestSubsetSize = leftCount;
              lessThan = true;
              //            } else if (targetLeft / leftCount == bestMerit) {
            } else if (delta == 0) {
              // break ties in favour of higher support
              if (leftCount > bestSupport) {
                bestMerit = targetLeft / leftCount;
                bestSplit = inst.value(attIndex);
                bestSupport = leftCount;
                bestSubsetSize = leftCount;
                lessThan = true;
              }
            }
          }

          if (rightCount >= m_supportCount) {
            double delta = (m_minimize) 
              ? (bestMerit - (targetRight / rightCount))
              : ((targetRight / rightCount) - bestMerit);
            //            if (targetRight / rightCount > bestMerit) {
            if (delta > 0) {
              bestMerit = targetRight / rightCount;
              bestSplit = inst.value(attIndex);
              bestSupport = rightCount;
              bestSubsetSize = rightCount;
              lessThan = false;
              //            } else if (targetRight / rightCount == bestMerit) {
            } else if (delta == 0) {
              // break ties in favour of higher support
              if (rightCount > bestSupport) {
                bestMerit = targetRight / rightCount;
                bestSplit = inst.value(attIndex);
                bestSupport = rightCount;
                bestSubsetSize = rightCount;
                lessThan = false;
              }
            }
          }          
        }
      }

      double delta = (m_minimize)
        ? m_targetValue - bestMerit
        : bestMerit - m_targetValue;

      // Have we found a candidate split?
      if (bestSupport > 0 && (delta / m_targetValue > m_minImprovement)) {
        /*        System.err.println("Evaluating " + tempInsts.attribute(attIndex).name());
        System.err.println("Merit : " + bestMerit);
        System.err.println("Support : " + bestSupport); */
        //        double suppFraction = bestSupport / m_numInstances;

        HotTestDetails newD = new HotTestDetails(attIndex, bestSplit, 
                                                 lessThan, (int)bestSupport, 
                                                 (int)bestSubsetSize, 
                                                 bestMerit);
        pq.add(newD);
      }
    }

    /**
     * Evaluate a nominal attribute for a potential split
     *
     * @param attIndex the index of the attribute
     * @param pq the priority queue of candidtate splits
     */
    private void evaluateNominal(int attIndex, PriorityQueue<HotTestDetails> pq) {
      int[] counts = m_insts.attributeStats(attIndex).nominalCounts;
      boolean ok = false;
      // only consider attribute values that result in subsets that meet/exceed min support
      for (int i = 0; i < m_insts.attribute(attIndex).numValues(); i++) {
        if (counts[i] >= m_supportCount) {
          ok = true;
          break;
        }
      }
      if (ok) {
        double[] subsetMerit = 
          new double[m_insts.attribute(attIndex).numValues()];

        for (int i = 0; i < m_insts.numInstances(); i++) {
          Instance temp = m_insts.instance(i);
          if (!temp.isMissing(attIndex)) {
            int attVal = (int)temp.value(attIndex);
            if (m_insts.attribute(m_target).isNumeric()) {
              subsetMerit[attVal] += temp.value(m_target);
            } else {
              subsetMerit[attVal] += 
                ((int)temp.value(m_target) == m_targetIndex)
                ? 1.0
                : 0;
            }
          }
        }
        
        // add to queue if it meets min support and exceeds the merit for the full set
        for (int i = 0; i < m_insts.attribute(attIndex).numValues(); i++) {
          // does the subset based on this value have enough instances, and, furthermore,
          // does the target value (nominal only) occur enough times to exceed min support
          if (counts[i] > m_supportCount &&  
              ((m_insts.attribute(m_target).isNominal())
              ? (subsetMerit[i] > m_supportCount) // nominal only test
               : true)) { 
            double merit = subsetMerit[i] / counts[i]; //subsetMerit[i][1];
            double delta = (m_minimize)
              ? m_targetValue - merit
              : merit - m_targetValue;

            if (delta / m_targetValue > m_minImprovement) {
              double support =
                (m_insts.attribute(m_target).isNominal())
                ? subsetMerit[i]
                : counts[i];

              HotTestDetails newD = new HotTestDetails(attIndex, (double)i, 
                                                       false, (int)support,
                                                       counts[i], 
                                                       merit);
              pq.add(newD);
            }
          }
        }
      }
    }

    public int assignIDs(int lastID) {
      int currentLastID = lastID + 1;
      m_id = currentLastID;
      System.err.println("Assigning " + currentLastID);
      if (m_children != null) {
        for (int i = 0; i < m_children.length; i++) {
          currentLastID = m_children[i].assignIDs(currentLastID);
        }
      }
      return currentLastID;
    }

    private void addNodeDetails(StringBuffer buff, int i, String spacer) {
      buff.append(m_header.attribute(m_testDetails[i].m_splitAttIndex).name());
      if (m_header.attribute(m_testDetails[i].m_splitAttIndex).isNumeric()) {
        if (m_testDetails[i].m_lessThan) {
          buff.append(" <= ");
        } else {
          buff.append(" > ");
        }
        buff.append(Utils.doubleToString(m_testDetails[i].m_splitValue, 4));
      } else {
        buff.append(" = " + m_header.
                    attribute(m_testDetails[i].m_splitAttIndex).
                    value((int)m_testDetails[i].m_splitValue));
      }

      if (m_header.attribute(m_target).isNumeric()) {
        buff.append(spacer + "(" + Utils.doubleToString(m_testDetails[i].m_merit, 4) + " ["
                    + m_testDetails[i].m_support + "])");
      } else {
        buff.append(spacer + "(" + Utils.doubleToString((m_testDetails[i].m_merit * 100.0), 2) + "% ["
                    + m_testDetails[i].m_support 
                    + "/" + m_testDetails[i].m_subsetSize + "])");
      }
    }

    private void graphHotSpot(StringBuffer text) {
      if (m_children != null) {
        for (int i = 0; i < m_children.length; i++) {
          text.append("N" + m_children[i].m_id);
          text.append(" [label=\"");
          addNodeDetails(text, i, "\\n");
          text.append("\" shape=plaintext]\n");
          m_children[i].graphHotSpot(text);
          text.append("N" + m_id + "->" + "N" + m_children[i].m_id + "\n");
        }
      }
    }

    /**
     * Traverse the tree to create a string description
     * 
     * @param depth the depth at this point in the tree
     * @param buff the string buffer to append node details to
     */
    protected void dumpTree(int depth, StringBuffer buff) {
      if (m_children == null) {
        //        buff.append("\n");
      } else {
        for (int i = 0; i < m_children.length; i++) {
          buff.append("\n  ");
          for (int j = 0; j < depth; j++) {
            buff.append("|   ");
          }
          addNodeDetails(buff, i, " ");

          m_children[i].dumpTree(depth + 1, buff);
        }
      }
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String targetTipText() {
    return "The target attribute of interest.";
  }

  /**
   * Set the target index
   *
   * @param target the target index as a string (1-based)
   */
  public void setTarget(String target) {
    m_targetSI.setSingleIndex(target);
  }

  /**
   * Get the target index as a string
   *
   * @return the target index (1-based)
   */
  public String getTarget() {
    return m_targetSI.getSingleIndex();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String targetIndexTipText() {
    return "The value of the target (nominal attributes only) of interest.";
  }

  /**
   * For a nominal target, set the index of the value of interest (1-based)
   *
   * @param index the index of the nominal value of interest
   */
  public void setTargetIndex(String index) {
    m_targetIndexSI.setSingleIndex(index);
  }

  /**
   * For a nominal target, get the index of the value of interest (1-based)
   *
   * @return the index of the nominal value of interest
   */
  public String getTargetIndex() {
    return m_targetIndexSI.getSingleIndex();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minimizeTargetTipText() {
    return "Minimize rather than maximize the target.";
  }

  /**
   * Set whether to minimize the target rather than maximize
   *
   * @param m true if target is to be minimized
   */
  public void setMinimizeTarget(boolean m) {
    m_minimize = m;
  }

  /**
   * Get whether to minimize the target rather than maximize
   *
   * @return true if target is to be minimized
   */
  public boolean getMinimizeTarget() {
    return m_minimize;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String supportTipText() {
    return "The minimum support. Values between 0 and 1 are interpreted "
      + "as a percentage of the total population; values > 1 are "
      + "interpreted as an absolute number of instances";
  }

  /**
   * Get the minimum support
   *
   * @return the minimum support
   */
  public double getSupport() {
    return m_support;
  }

  /**
   * Set the minimum support
   *
   * @param s the minimum support
   */
  public void setSupport(double s) {
    m_support = s;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxBranchingFactorTipText() {
    return "Maximum branching factor. The maximum number of children "
      + "to consider extending each node with.";
  }

  /**
   * Set the maximum branching factor
   *
   * @param b the maximum branching factor
   */
  public void setMaxBranchingFactor(int b) {
    m_maxBranchingFactor = b;
  }

  /**
   * Get the maximum branching factor
   *
   * @return the maximum branching factor
   */
  public int getMaxBranchingFactor() {
    return m_maxBranchingFactor;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minImprovementTipText() {
    return "Minimum improvement in target value in order to "
      + "consider adding a new branch/test";
  }

  /**
   * Set the minimum improvement in the target necessary to add a test
   *
   * @param i the minimum improvement
   */
  public void setMinImprovement(double i) {
    m_minImprovement = i;
  }

  /**
   * Get the minimum improvement in the target necessary to add a test
   *
   * @return the minimum improvement
   */
  public double getMinImprovement() {
    return m_minImprovement;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Output debugging info (duplicate rule lookup hash table stats).";
  }

  /**
   * Set whether debugging info is output
   *
   * @param d true to output debugging info
   */
  public void setDebug(boolean d) {
    m_debug = d;
  }

  /**
   * Get whether debugging info is output
   *
   * @return true if outputing debugging info
   */
  public boolean getDebug() {
    return m_debug;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector();
    newVector.addElement(new Option("\tThe target index. (default = last)",
                                    "c", 1,
                                    "-c <num | first | last>"));
    newVector.addElement(new Option("\tThe target value (nominal target only, default = first)",
                                    "V", 1,
                                    "-V <num | first | last>"));
    newVector.addElement(new Option("\tMinimize rather than maximize.", "L", 0, "-L"));
    newVector.addElement(new Option("\tMinimum value count (nominal target)/segment size "
                                    + "(numeric target)."
                                    +"\n\tValues between 0 and 1 are "
                                    + "\n\tinterpreted as a percentage of "
                                    + "\n\tthe total population; values > 1 are "
                                    + "\n\tinterpreted as an absolute number of "
                                    +"\n\tinstances (default = 0.3)",
                                    "-S", 1,
                                    "-S <num>"));
    newVector.addElement(new Option("\tMaximum branching factor (default = 2)",
                                    "-M", 1,
                                    "-M <num>"));
    newVector.addElement(new Option("\tMinimum improvement in target value in order "
                                    + "\n\tto add a new branch/test (default = 0.01 (1%))",
                                    "-I", 1,
                                    "-I <num>"));
    newVector.addElement(new Option("\tOutput debugging info (duplicate rule lookup "
                                    + "\n\thash table stats)", "-D", 0, "-D"));
    return newVector.elements();
  }

  /**
   * Reset options to their defaults
   */
  public void resetOptions() {
    m_support = 0.33;
    m_minImprovement = 0.01; // 1%
    m_maxBranchingFactor = 2;
    m_minimize = false;
    m_debug = false;
    setTarget("last");
    setTargetIndex("first");
    m_errorMessage = null;
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -c &lt;num | first | last&gt;
   * The target index. (default = last)
   * </pre>
   * 
   * <pre> -V &lt;num | first | last&gt;
   *  The target value (nominal target only, default = first)
   * </pre>
   *
   * <pre> -L
   * Minimize rather than maximize
   * </pre>
   *
   * <pre> -S &lt;num&gt;
   *  Minimum value count (nominal target)/segment size (numeric target). 
   *  Values between 0 and 1 are interpreted
   *  as a percentage of the total population; values > 1 are
   *  interpreted as an absolute number of instances
   *  (default = 0.3)
   * </pre>
   *
   * <pre> -M &lt;num&gt;
   * Maximum branching factor. The maximum number of children
   * to consider extending each node with. (default = 2)
   * </pre>
   *
   * <pre> -I &lt;num&gt;
   * Minimum improvement in target value in order to
   * consider adding a new branch/test (default = 0.01 (1%))
   * </pre>
   *
   * <pre> -D
   * Output debugging info (duplicate rule lookup hash table stats)
   * </pre>
   *
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    resetOptions();

    String tempString = Utils.getOption('c', options);
    if (tempString.length() != 0) {
      setTarget(tempString);
    }
    
    tempString = Utils.getOption('V', options);
    if (tempString.length() != 0) {
      setTargetIndex(tempString);
    }

    setMinimizeTarget(Utils.getFlag('L', options));

    tempString = Utils.getOption('S', options);
    if (tempString.length() != 0) {
      setSupport(Double.parseDouble(tempString));
    }

    tempString = Utils.getOption('M', options);
    if (tempString.length() != 0) {
      setMaxBranchingFactor(Integer.parseInt(tempString));
    }

    tempString = Utils.getOption('I', options);
    if (tempString.length() != 0) {
      setMinImprovement(Double.parseDouble(tempString));
    }

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of HotSpot.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    String[] options = new String[12];
    int current = 0;
    
    options[current++] = "-c"; options[current++] = getTarget();
    options[current++] = "-V"; options[current++] = getTargetIndex();
    if (getMinimizeTarget()) {
      options[current++] = "-L";
    }
    options[current++] = "-S"; options[current++] = "" + getSupport();
    options[current++] = "-M"; options[current++] = "" + getMaxBranchingFactor();
    options[current++] = "-I"; options[current++] = "" + getMinImprovement();
    if (getDebug()) {
      options[current++] = "-D";
    }

    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.1 $");
  }

  /**
   *  Returns the type of graph this scheme
   *  represents.
   *  @return Drawable.TREE
   */   
  public int graphType() {
    return Drawable.TREE;
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String[] args) {
    try {
      HotSpot h = new HotSpot();
      AbstractAssociator.runAssociator(new HotSpot(), args);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}