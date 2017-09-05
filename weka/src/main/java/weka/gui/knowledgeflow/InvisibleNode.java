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
 *    InvisibleNode.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.WekaEnumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

/**
 * Subclass of {@code DefaultMutableTreeNode} that can hide itself in a
 * {@code JTree}.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class InvisibleNode extends DefaultMutableTreeNode {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -9064396835384819887L;

  /** True if the node is visible */
  protected boolean m_isVisible;

  /**
   * Constructor
   */
  public InvisibleNode() {
    this(null);
  }

  /**
   * Constructor for a new node that allows children and is visible
   *
   * @param userObject the user object to wrap at the node
   */
  public InvisibleNode(Object userObject) {
    this(userObject, true, true);
  }

  /**
   * Constructor
   *
   * @param userObject the user object to wrap at the node
   * @param allowsChildren true if this node allows children (not a leaf)
   * @param isVisible true if this node is visible initially
   */
  public InvisibleNode(Object userObject, boolean allowsChildren,
    boolean isVisible) {
    super(userObject, allowsChildren);
    this.m_isVisible = isVisible;
  }

  /**
   * Get a child node
   *
   * @param index the index of the node to get
   * @param filterIsActive true if the visible filter is active
   * @return a child node
   */
  public TreeNode getChildAt(int index, boolean filterIsActive) {
    if (!filterIsActive) {
      return super.getChildAt(index);
    }
    if (children == null) {
      throw new ArrayIndexOutOfBoundsException("node has no children");
    }

    int realIndex = -1;
    int visibleIndex = -1;
    Enumeration<TreeNode> e = new WekaEnumeration<TreeNode>(children);
    while (e.hasMoreElements()) {
      InvisibleNode node = (InvisibleNode)e.nextElement();
      if (node.isVisible()) {
        visibleIndex++;
      }
      realIndex++;
      if (visibleIndex == index) {
        return (TreeNode) children.elementAt(realIndex);
      }
    }

    throw new ArrayIndexOutOfBoundsException("index unmatched");
  }

  /**
   * Get the number of children nodes
   * 
   * @param filterIsActive true if the visible filter is active (alters the
   *          count according to visibility)
   * @return the number of child nodes
   */
  public int getChildCount(boolean filterIsActive) {
    if (!filterIsActive) {
      return super.getChildCount();
    }
    if (children == null) {
      return 0;
    }

    int count = 0;
    Enumeration<TreeNode> e = new WekaEnumeration<TreeNode>(children);
    while (e.hasMoreElements()) {
      InvisibleNode node = (InvisibleNode)e.nextElement();
      if (node.isVisible()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Set the visible status of this node
   *
   * @param visible true if this node should be visible
   */
  public void setVisible(boolean visible) {
    this.m_isVisible = visible;
  }

  /**
   * Returns true if this node is visible
   *
   * @return true if this node is visible
   */
  public boolean isVisible() {
    return m_isVisible;
  }
}
