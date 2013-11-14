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
 *    SplitNode.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.util.LinkedHashMap;
import java.util.Map;

import weka.core.Instance;

/**
 * Class for a node that splits the data in a Hoeffding tree
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class SplitNode extends HNode {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 1558033628618451073L;

  /** The split itself */
  protected Split m_split;

  /** Child nodes */
  protected Map<String, HNode> m_children = new LinkedHashMap<String, HNode>();

  /**
   * Construct a new SplitNode
   * 
   * @param classDistrib the class distribution
   * @param split the split
   */
  public SplitNode(Map<String, WeightMass> classDistrib, Split split) {
    super(classDistrib);

    m_split = split;
  }

  /**
   * Return the branch that the supplied instance goes down
   * 
   * @param inst the instance to find the branch for
   * @return the branch that the supplied instance goes down
   */
  public String branchForInstance(Instance inst) {
    return m_split.branchForInstance(inst);
  }

  @Override
  public boolean isLeaf() {
    return false;
  }

  /**
   * Number of child nodes
   * 
   * @return the number of child nodes
   */
  public int numChildred() {
    return m_children.size();
  }

  /**
   * Add a child
   * 
   * @param branch the branch for the child
   * @param child the child itself
   */
  public void setChild(String branch, HNode child) {
    m_children.put(branch, child);
  }

  @Override
  public LeafNode leafForInstance(Instance inst, SplitNode parent,
    String parentBranch) {

    String branch = branchForInstance(inst);
    if (branch != null) {
      HNode child = m_children.get(branch);
      if (child != null) {
        return child.leafForInstance(inst, this, branch);
      }
      return new LeafNode(null, this, branch);
    }
    return new LeafNode(this, parent, parentBranch);
  }

  @Override
  public void updateNode(Instance inst) {
    // don't update the distribution
  }

  @Override
  protected int dumpTree(int depth, int leafCount, StringBuffer buff) {

    for (Map.Entry<String, HNode> e : m_children.entrySet()) {

      HNode child = e.getValue();
      String branch = e.getKey();

      if (child != null) {

        buff.append("\n");

        for (int i = 0; i < depth; i++) {
          buff.append("|   ");
        }

        buff.append(m_split.conditionForBranch(branch).trim());
        buff.append(": ");
        leafCount = child.dumpTree(depth + 1, leafCount, buff);
      }
    }
    return leafCount;
  }

  @Override
  public int installNodeNums(int nodeNum) {
    nodeNum = super.installNodeNums(nodeNum);

    for (Map.Entry<String, HNode> e : m_children.entrySet()) {

      HNode child = e.getValue();

      if (child != null) {
        nodeNum = child.installNodeNums(nodeNum);
      }
    }

    return nodeNum;
  }

  @Override
  public void graphTree(StringBuffer buff) {
    boolean first = true;
    for (Map.Entry<String, HNode> e : m_children.entrySet()) {

      HNode child = e.getValue();
      String branch = e.getKey();

      if (child != null) {
        String conditionForBranch = m_split.conditionForBranch(branch);
        if (first) {
          String testAttName = null;

          if (conditionForBranch.indexOf("<=") < 0) {
            testAttName = conditionForBranch.substring(0,
              conditionForBranch.indexOf("=")).trim();
          } else {
            testAttName = conditionForBranch.substring(0,
              conditionForBranch.indexOf("<")).trim();
          }
          first = false;
          buff.append("N" + m_nodeNum + " [label=\"" + testAttName + "\"]\n");
        }

        int startIndex = 0;
        if (conditionForBranch.indexOf("<=") > 0) {
          startIndex = conditionForBranch.indexOf("<") - 1;
        } else if (conditionForBranch.indexOf("=") > 0) {
          startIndex = conditionForBranch.indexOf("=") - 1;
        } else {
          startIndex = conditionForBranch.indexOf(">") - 1;
        }
        conditionForBranch = conditionForBranch.substring(startIndex,
          conditionForBranch.length()).trim();

        buff.append(
          "N" + m_nodeNum + "->" + "N" + child.m_nodeNum + "[label=\""
            + conditionForBranch + "\"]\n").append("\n");

      }
    }

    for (Map.Entry<String, HNode> e : m_children.entrySet()) {
      HNode child = e.getValue();

      if (child != null) {
        child.graphTree(buff);
      }
    }
  }

  @Override
  protected void printLeafModels(StringBuffer buff) {
    for (Map.Entry<String, HNode> e : m_children.entrySet()) {

      HNode child = e.getValue();

      if (child != null) {
        child.printLeafModels(buff);
      }
    }
  }
}
