package weka.gui.knowledgeflow;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import weka.core.WekaEnumeration;

public class InvisibleNode extends DefaultMutableTreeNode {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -9064396835384819887L;

  protected boolean m_isVisible;

  public InvisibleNode() {
    this(null);
  }

  public InvisibleNode(Object userObject) {
    this(userObject, true, true);
  }

  public InvisibleNode(Object userObject, boolean allowsChildren,
    boolean isVisible) {
    super(userObject, allowsChildren);
    this.m_isVisible = isVisible;
  }

  public TreeNode getChildAt(int index, boolean filterIsActive) {
    if (!filterIsActive) {
      return super.getChildAt(index);
    }
    if (children == null) {
      throw new ArrayIndexOutOfBoundsException("node has no children");
    }

    int realIndex = -1;
    int visibleIndex = -1;
    @SuppressWarnings("unchecked")
    Enumeration<InvisibleNode> e = new WekaEnumeration<InvisibleNode>(
      children);
    while (e.hasMoreElements()) {
      InvisibleNode node = e.nextElement();
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

  public int getChildCount(boolean filterIsActive) {
    if (!filterIsActive) {
      return super.getChildCount();
    }
    if (children == null) {
      return 0;
    }

    int count = 0;
    @SuppressWarnings("unchecked")
    Enumeration<InvisibleNode> e = new WekaEnumeration<InvisibleNode>(
      children);
    while (e.hasMoreElements()) {
      InvisibleNode node = e.nextElement();
      if (node.isVisible()) {
        count++;
      }
    }

    return count;
  }

  public void setVisible(boolean visible) {
    this.m_isVisible = visible;
  }

  public boolean isVisible() {
    return m_isVisible;
  }
}
