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
 *    LeafNode.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;

import weka.core.Instance;

/**
 * Leaf node in a HoeffdingTree
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class LeafNode extends HNode implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -3359429731894384404L;

  /** The actual node for this leaf */
  public HNode m_theNode;

  /** Parent split node */
  public SplitNode m_parentNode;

  /** Parent branch leading to this node */
  public String m_parentBranch;

  /**
   * Construct an empty leaf node
   */
  public LeafNode() {
  }

  /**
   * Construct a leaf node with the given actual node, parent and parent branch
   * 
   * @param node the actual node at this leaf
   * @param parentNode the parent split node
   * @param parentBranch the branch leading to this node
   */
  public LeafNode(HNode node, SplitNode parentNode, String parentBranch) {
    m_theNode = node;
    m_parentNode = parentNode;
    m_parentBranch = parentBranch;
  }

  @Override
  public void updateNode(Instance inst) throws Exception {
    if (m_theNode != null) {
      m_theNode.updateDistribution(inst);
    } else {
      super.updateDistribution(inst);
    }
  }
}
