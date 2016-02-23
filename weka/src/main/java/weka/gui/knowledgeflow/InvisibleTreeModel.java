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
 *    InvisibleTreeModel
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * Subclass of {@code DefaultTreeModel} that contains {@code InvisibleNode}s.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class InvisibleTreeModel extends DefaultTreeModel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6940101211275068260L;

  /** True if the visibility filter is active */
  protected boolean m_filterIsActive;

  /**
   * Constructor
   *
   * @param root the root of the tree
   */
  public InvisibleTreeModel(TreeNode root) {
    this(root, false);
  }

  /**
   * Constuctor
   * 
   * @param root the root of the tree
   * @param asksAllowsChildren asksAllowsChildren - a boolean, false if any node
   *          can have children, true if each node is asked to see if it can
   *          have children
   */
  public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren) {
    this(root, false, false);
  }

  /**
   * Constructor
   *
   * @param root the root of the tree
   * @param asksAllowsChildren asksAllowsChildren - a boolean, false if any node
   *          can have children, true if each node is asked to see if it can
   *          have children
   * @param filterIsActive true if the visibility filter is active
   */
  public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren,
    boolean filterIsActive) {
    super(root, asksAllowsChildren);
    this.m_filterIsActive = filterIsActive;
  }

  /**
   * Activate/deactivate the visibility filter
   *
   * @param newValue true if the visibility filter should be active
   */
  public void activateFilter(boolean newValue) {
    m_filterIsActive = newValue;
  }

  /**
   * Return true if the visibility filter is active
   *
   * @return true if the visibility filter is active
   */
  public boolean isActivatedFilter() {
    return m_filterIsActive;
  }

  @Override
  public Object getChild(Object parent, int index) {
    if (m_filterIsActive) {
      if (parent instanceof InvisibleNode) {
        return ((InvisibleNode) parent).getChildAt(index, m_filterIsActive);
      }
    }
    return ((TreeNode) parent).getChildAt(index);
  }

  @Override
  public int getChildCount(Object parent) {
    if (m_filterIsActive) {
      if (parent instanceof InvisibleNode) {
        return ((InvisibleNode) parent).getChildCount(m_filterIsActive);
      }
    }
    return ((TreeNode) parent).getChildCount();
  }
}
