package weka.gui.knowledgeflow;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class DesignPanel extends JPanel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3324733191950871564L;

  public DesignPanel(final StepTree stepTree) {
    super();
    setLayout(new BorderLayout());
    JScrollPane treeView = new JScrollPane(stepTree);
    setBorder(BorderFactory.createTitledBorder("Design"));
    add(treeView, BorderLayout.CENTER);
    final JTextField searchField = new JTextField();
    add(searchField, BorderLayout.NORTH);
    searchField.setToolTipText("Search (clear field to reset)");

    searchField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        String searchTerm = searchField.getText();
        List<DefaultMutableTreeNode> nonhits =
          new ArrayList<DefaultMutableTreeNode>();
        List<DefaultMutableTreeNode> hits =
          new ArrayList<DefaultMutableTreeNode>();
        DefaultTreeModel model = (DefaultTreeModel) stepTree
          .getModel();
        model.reload(); // collapse all nodes first

        for (Map.Entry<String, DefaultMutableTreeNode> entry : stepTree
          .getNodeTextIndex()
          .entrySet()) {
          if (entry.getValue() instanceof InvisibleNode) {
            ((InvisibleNode) entry.getValue()).setVisible(true);
          }

          if (searchTerm != null && searchTerm.length() > 0) {
            if (entry.getKey().contains(searchTerm.toLowerCase())) {
              hits.add(entry.getValue());
            } else {
              nonhits.add(entry.getValue());
            }
          }
        }

        if (searchTerm == null || searchTerm.length() == 0) {
          model.reload(); // just reset everything
        }
        // if we have some hits then set all the non-hits to invisible
        if (hits.size() > 0) {
          for (DefaultMutableTreeNode h : nonhits) {
            if (h instanceof InvisibleNode) {
              ((InvisibleNode) h).setVisible(false);
            }
          }
          model.reload(); // collapse all the nodes first

          // expand all the hits
          for (DefaultMutableTreeNode h : hits) {
            TreeNode[] path = model.getPathToRoot(h);
            TreePath tpath = new TreePath(path);
            tpath = tpath.getParentPath();
            stepTree.expandPath(tpath);
          }
        }
      }
    });
  }
}
