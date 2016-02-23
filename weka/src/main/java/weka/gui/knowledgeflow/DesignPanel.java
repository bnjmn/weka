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
 *    DesignPanel.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel that contains the tree view of steps and the search
 * field.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class DesignPanel extends JPanel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3324733191950871564L;

  /**
   * Constructor
   *
   * @param stepTree the {@code StepTree} to display
   */
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
