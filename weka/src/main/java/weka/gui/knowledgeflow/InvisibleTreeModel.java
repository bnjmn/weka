package weka.gui.knowledgeflow;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class InvisibleTreeModel extends DefaultTreeModel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6940101211275068260L;

  protected boolean m_filterIsActive;

  public InvisibleTreeModel(TreeNode root) {
    this(root, false);
  }

  public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren) {
    this(root, false, false);
  }

  public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren,
    boolean filterIsActive) {
    super(root, asksAllowsChildren);
    this.m_filterIsActive = filterIsActive;
  }

  public void activateFilter(boolean newValue) {
    m_filterIsActive = newValue;
  }

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
