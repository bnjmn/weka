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
 *    FlowByExpressionStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.EnvironmentField;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.FlowByExpression;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Step editor dialog for the FlowByExpression step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FlowByExpressionStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 1545740909421963983L;

  /** Combo box for the LHS of the expression being entered */
  protected JComboBox m_lhsField = new EnvironmentField.WideComboBox();

  /** Combo box for choosing the operator */
  protected JComboBox<String> m_operatorCombo = new JComboBox<String>();

  /** Combo box for the RHS of the expression being entered */
  protected JComboBox m_rhsField = new EnvironmentField.WideComboBox();

  /** Check box for indicating that the RHS is the name of an attribute */
  protected JCheckBox m_rhsIsAttribute = new JCheckBox("RHS is attribute");

  /** Label for displaying the current expression */
  protected JLabel m_expressionLab = new JLabel();

  /**
   * Combo box for choosing the downstream step that
   * instances matching the expression should go to
   */
  protected JComboBox<String> m_trueData = new JComboBox<String>();

  /**
   * Combo box for choosing the downstream step that instances failing
   * to match the expression should go to
   */
  protected JComboBox<String> m_falseData = new JComboBox<String>();

  /** Holds the tree view of the expression */
  protected JTree m_expressionTree;

  /** Root of the tree */
  protected DefaultMutableTreeNode m_treeRoot;

  /** Button for adding a new expression node to the expression tree */
  protected JButton m_addExpressionNode = new JButton("Add Expression node");

  /** Button for adding a new brackets node to the expression tree */
  protected JButton m_addBracketNode = new JButton("Add bracket node");

  /** Button for toggling negation */
  protected JButton m_toggleNegation = new JButton("Toggle negation");

  /** Button for and/or */
  protected JButton m_andOr = new JButton("And/Or");

  /** Button for deleting a node from the expression tree */
  protected JButton m_deleteNode = new JButton("Delete node");

  /**
   * Layout the editor
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void layoutEditor() {

    JPanel outerP = new JPanel(new BorderLayout());
    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    setupTree(outerP);

    JPanel fieldHolder = new JPanel();
    fieldHolder.setLayout(new GridLayout(0, 4));

    JPanel lhsP = new JPanel();
    lhsP.setLayout(new BorderLayout());
    lhsP.setBorder(BorderFactory.createTitledBorder("Attribute"));

    // m_lhsField = new EnvironmentField(m_env);
    m_lhsField.setEditable(true);

    lhsP.add(m_lhsField, BorderLayout.CENTER);
    lhsP.setToolTipText("<html>Name or index of attribute. "
      + "also accepts<br>the special labels \"/first\" and \"/last\""
      + " to indicate<br>the first and last attribute respecitively</html>");
    m_lhsField.setToolTipText("<html>Name or index of attribute. "
      + "also accepts<br>the special labels \"/first\" and \"/last\""
      + " to indicate<br>the first and last attribute respecitively</html>");

    JPanel operatorP = new JPanel();
    operatorP.setLayout(new BorderLayout());
    operatorP.setBorder(BorderFactory.createTitledBorder("Operator"));
    m_operatorCombo.addItem(" = ");
    m_operatorCombo.addItem(" != ");
    m_operatorCombo.addItem(" < ");
    m_operatorCombo.addItem(" <= ");
    m_operatorCombo.addItem(" > ");
    m_operatorCombo.addItem(" >= ");
    m_operatorCombo.addItem(" isMissing ");
    m_operatorCombo.addItem(" contains ");
    m_operatorCombo.addItem(" startsWith ");
    m_operatorCombo.addItem(" endsWith ");
    m_operatorCombo.addItem(" regex ");
    operatorP.add(m_operatorCombo, BorderLayout.CENTER);

    JPanel rhsP = new JPanel();
    rhsP.setLayout(new BorderLayout());
    rhsP.setBorder(BorderFactory.createTitledBorder("Constant or attribute"));
    rhsP.setToolTipText("<html>Constant value to test/check for. If "
      + "testing<br>against an attribute, then this specifies"
      + "an attribute index or name</html>");
    // m_rhsField = new EnvironmentField(m_env);
    m_rhsField.setEditable(true);
    rhsP.add(m_rhsField, BorderLayout.CENTER);

    fieldHolder.add(lhsP);
    fieldHolder.add(operatorP);
    fieldHolder.add(rhsP);
    fieldHolder.add(m_rhsIsAttribute);
    controlHolder.add(fieldHolder, BorderLayout.SOUTH);

    JPanel tempPanel = new JPanel();
    tempPanel.setLayout(new BorderLayout());
    JPanel expressionP = new JPanel();
    expressionP.setLayout(new BorderLayout());
    expressionP.setBorder(BorderFactory.createTitledBorder("Expression"));
    JPanel tempE = new JPanel();
    tempE.setLayout(new BorderLayout());
    tempE.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    tempE.add(m_expressionLab, BorderLayout.CENTER);
    JScrollPane expressionScroller = new JScrollPane(tempE);
    expressionP.add(expressionScroller, BorderLayout.CENTER);
    tempPanel.add(expressionP, BorderLayout.SOUTH);

    //
    JPanel flowControlP = new JPanel();
    flowControlP.setLayout(new GridLayout(2, 2));
    flowControlP.add(new JLabel("Send true instances to node",
      SwingConstants.RIGHT));
    flowControlP.add(m_trueData);
    flowControlP.add(new JLabel("Send false instances to node",
      SwingConstants.RIGHT));
    flowControlP.add(m_falseData);
    tempPanel.add(flowControlP, BorderLayout.NORTH);
    String falseStepN = ((FlowByExpression) getStepToEdit()).getFalseStepName();
    String trueStepN = ((FlowByExpression) getStepToEdit()).getTrueStepName();

    List<String> connectedSteps =
      ((FlowByExpression) getStepToEdit()).getDownstreamStepNames();
    m_trueData.addItem("<none>");
    m_falseData.addItem("<none>");
    for (String s : connectedSteps) {
      m_trueData.addItem(s);
      m_falseData.addItem(s);
    }

    if (falseStepN != null && falseStepN.length() > 0) {
      m_falseData.setSelectedItem(falseStepN);
    }
    if (trueStepN != null && trueStepN.length() > 0) {
      m_trueData.setSelectedItem(trueStepN);
    }

    controlHolder.add(tempPanel, BorderLayout.NORTH);
    outerP.add(controlHolder, BorderLayout.NORTH);
    add(outerP, BorderLayout.CENTER);

    m_rhsIsAttribute.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_expressionTree != null) {
          TreePath p = m_expressionTree.getSelectionPath();
          if (p != null) {
            if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

              DefaultMutableTreeNode tNode =
                (DefaultMutableTreeNode) p.getLastPathComponent();
              FlowByExpression.ExpressionNode thisNode =
                (FlowByExpression.ExpressionNode) tNode.getUserObject();

              if (thisNode instanceof FlowByExpression.ExpressionClause) {
                ((FlowByExpression.ExpressionClause) thisNode)
                  .setRHSIsAnAttribute(m_rhsIsAttribute.isSelected());

                DefaultTreeModel tmodel =
                  (DefaultTreeModel) m_expressionTree.getModel();
                tmodel.nodeStructureChanged(tNode);

                updateExpressionLabel();
              }
            }
          }
        }
      }
    });

    m_operatorCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        if (m_operatorCombo.getSelectedIndex() > 5) {
          m_rhsIsAttribute.setSelected(false);
          m_rhsIsAttribute.setEnabled(false);
        } else {
          m_rhsIsAttribute.setEnabled(true);
        }

        if (m_expressionTree != null) {
          TreePath p = m_expressionTree.getSelectionPath();
          if (p != null) {
            if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

              DefaultMutableTreeNode tNode =
                (DefaultMutableTreeNode) p.getLastPathComponent();
              FlowByExpression.ExpressionNode thisNode =
                (FlowByExpression.ExpressionNode) tNode.getUserObject();

              if (thisNode instanceof FlowByExpression.ExpressionClause) {
                String selection = m_operatorCombo.getSelectedItem().toString();
                FlowByExpression.ExpressionClause.ExpressionType t =
                  FlowByExpression.ExpressionClause.ExpressionType.EQUALS;
                for (FlowByExpression.ExpressionClause.ExpressionType et : FlowByExpression.ExpressionClause.ExpressionType
                  .values()) {
                  if (et.toString().equals(selection)) {
                    t = et;
                    break;
                  }
                }

                ((FlowByExpression.ExpressionClause) thisNode).setOperator(t);
                DefaultTreeModel tmodel =
                  (DefaultTreeModel) m_expressionTree.getModel();
                tmodel.nodeStructureChanged(tNode);

                updateExpressionLabel();
              }
            }
          }
        }
      }
    });

    m_lhsField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_expressionTree != null) {
          TreePath p = m_expressionTree.getSelectionPath();
          if (p != null) {
            if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

              DefaultMutableTreeNode tNode =
                (DefaultMutableTreeNode) p.getLastPathComponent();
              FlowByExpression.ExpressionNode thisNode =
                (FlowByExpression.ExpressionNode) tNode.getUserObject();

              if (thisNode instanceof FlowByExpression.ExpressionClause) {
                Object text = m_lhsField.getSelectedItem();
                if (text != null) {
                  ((FlowByExpression.ExpressionClause) thisNode)
                    .setLHSAttName(text.toString());
                  DefaultTreeModel tmodel =
                    (DefaultTreeModel) m_expressionTree.getModel();
                  tmodel.nodeStructureChanged(tNode);

                  updateExpressionLabel();
                }
              }
            }
          }
        }
      }
    });

    m_lhsField.getEditor().getEditorComponent()
      .addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          if (m_expressionTree != null) {
            TreePath p = m_expressionTree.getSelectionPath();
            if (p != null) {
              if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

                DefaultMutableTreeNode tNode =
                  (DefaultMutableTreeNode) p.getLastPathComponent();
                FlowByExpression.ExpressionNode thisNode =
                  (FlowByExpression.ExpressionNode) tNode.getUserObject();

                if (thisNode instanceof FlowByExpression.ExpressionClause) {
                  String text = "";
                  if (m_lhsField.getSelectedItem() != null) {
                    text = m_lhsField.getSelectedItem().toString();
                  }
                  java.awt.Component theEditor =
                    m_lhsField.getEditor().getEditorComponent();
                  if (theEditor instanceof JTextField) {
                    text = ((JTextField) theEditor).getText();
                  }
                  ((FlowByExpression.ExpressionClause) thisNode)
                    .setLHSAttName(text);
                  DefaultTreeModel tmodel =
                    (DefaultTreeModel) m_expressionTree.getModel();
                  tmodel.nodeStructureChanged(tNode);

                  updateExpressionLabel();
                }
              }
            }
          }
        }
      });

    m_rhsField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_expressionTree != null) {
          TreePath p = m_expressionTree.getSelectionPath();
          if (p != null) {
            if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

              DefaultMutableTreeNode tNode =
                (DefaultMutableTreeNode) p.getLastPathComponent();
              FlowByExpression.ExpressionNode thisNode =
                (FlowByExpression.ExpressionNode) tNode.getUserObject();

              if (thisNode instanceof FlowByExpression.ExpressionClause) {
                Object text = m_rhsField.getSelectedItem();
                if (text != null) {
                  ((FlowByExpression.ExpressionClause) thisNode)
                    .setRHSOperand(text.toString());
                  DefaultTreeModel tmodel =
                    (DefaultTreeModel) m_expressionTree.getModel();
                  tmodel.nodeStructureChanged(tNode);

                  updateExpressionLabel();
                }
              }
            }
          }
        }
      }
    });

    m_rhsField.getEditor().getEditorComponent()
      .addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          if (m_expressionTree != null) {
            TreePath p = m_expressionTree.getSelectionPath();
            if (p != null) {
              if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

                DefaultMutableTreeNode tNode =
                  (DefaultMutableTreeNode) p.getLastPathComponent();
                FlowByExpression.ExpressionNode thisNode =
                  (FlowByExpression.ExpressionNode) tNode.getUserObject();

                if (thisNode instanceof FlowByExpression.ExpressionClause) {
                  String text = "";
                  if (m_rhsField.getSelectedItem() != null) {
                    text = m_rhsField.getSelectedItem().toString();
                  }
                  java.awt.Component theEditor =
                    m_rhsField.getEditor().getEditorComponent();
                  if (theEditor instanceof JTextField) {
                    text = ((JTextField) theEditor).getText();
                  }

                  if (m_rhsField.getSelectedItem() != null) {
                    ((FlowByExpression.ExpressionClause) thisNode)
                      .setRHSOperand(text);
                    DefaultTreeModel tmodel =
                      (DefaultTreeModel) m_expressionTree.getModel();
                    tmodel.nodeStructureChanged(tNode);

                    updateExpressionLabel();
                  }
                }
              }
            }
          }
        }
      });

    try {
      Instances incomingStructure =
        getStepToEdit().getStepManager().getIncomingStructureForConnectionType(
          StepManager.CON_INSTANCE);
      if (incomingStructure == null) {
        incomingStructure =
          getStepToEdit().getStepManager()
            .getIncomingStructureForConnectionType(StepManager.CON_DATASET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          getStepToEdit().getStepManager()
            .getIncomingStructureForConnectionType(StepManager.CON_TRAININGSET);
      }
      if (incomingStructure == null) {
        incomingStructure =
          getStepToEdit().getStepManager()
            .getIncomingStructureForConnectionType(StepManager.CON_TESTSET);
      }
      if (incomingStructure != null) {
        m_lhsField.removeAllItems();
        m_rhsField.removeAllItems();
        for (int i = 0; i < incomingStructure.numAttributes(); i++) {
          m_lhsField.addItem(incomingStructure.attribute(i).name());
          m_rhsField.addItem(incomingStructure.attribute(i).name());
        }
      }
    } catch (WekaException ex) {
      showErrorDialog(ex);
    }
  }

  private void setExpressionEditor(FlowByExpression.ExpressionClause node) {
    String lhs = node.getLHSAttName();
    if (lhs != null) {
      m_lhsField.setSelectedItem(lhs);
    }
    String rhs = node.getRHSOperand();
    if (rhs != null) {
      m_rhsField.setSelectedItem(rhs);
    }
    FlowByExpression.ExpressionClause.ExpressionType opp = node.getOperator();
    int oppIndex = opp.ordinal();
    m_operatorCombo.setSelectedIndex(oppIndex);
    m_rhsIsAttribute.setSelected(node.isRHSAnAttribute());
  }

  private void updateExpressionLabel() {
    StringBuffer buff = new StringBuffer();

    FlowByExpression.ExpressionNode root =
      (FlowByExpression.ExpressionNode) m_treeRoot.getUserObject();
    root.toStringDisplay(buff);
    m_expressionLab.setText(buff.toString());
  }

  private void setupTree(JPanel holder) {
    JPanel treeHolder = new JPanel();
    treeHolder.setLayout(new BorderLayout());
    treeHolder.setBorder(BorderFactory.createTitledBorder("Expression tree"));
    String expressionString =
      ((FlowByExpression) getStepToEdit()).getExpressionString();
    if (expressionString == null || expressionString.length() == 0) {
      expressionString = "()";
    }
    FlowByExpression.BracketNode root = new FlowByExpression.BracketNode();
    root.parseFromInternal(expressionString);
    root.setShowAndOr(false);

    m_treeRoot = root.toJTree(null);

    DefaultTreeModel model = new DefaultTreeModel(m_treeRoot);
    m_expressionTree = new JTree(model);
    m_expressionTree.setEnabled(true);
    m_expressionTree.setRootVisible(true);
    m_expressionTree.setShowsRootHandles(true);
    DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    m_expressionTree.setSelectionModel(selectionModel);

    // add mouse listener to tree
    m_expressionTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();

            FlowByExpression.ExpressionNode thisNode =
              (FlowByExpression.ExpressionNode) tNode.getUserObject();

            if (thisNode instanceof FlowByExpression.ExpressionClause) {
              setExpressionEditor((FlowByExpression.ExpressionClause) thisNode);
            }
          }
        }
      }
    });

    updateExpressionLabel();

    JScrollPane treeView = new JScrollPane(m_expressionTree);
    treeHolder.add(treeView, BorderLayout.CENTER);

    JPanel butHolder = new JPanel();
    // butHolder.setLayout(new BorderLayout());
    butHolder.add(m_addExpressionNode);
    butHolder.add(m_addBracketNode);
    butHolder.add(m_toggleNegation);
    butHolder.add(m_andOr);
    butHolder.add(m_deleteNode);
    treeHolder.add(butHolder, BorderLayout.NORTH);

    holder.add(treeHolder, BorderLayout.CENTER);
    Dimension d = treeHolder.getPreferredSize();
    treeHolder.setPreferredSize(new Dimension(d.width, d.height / 2));

    m_andOr.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();
            FlowByExpression.ExpressionNode thisNode =
              (FlowByExpression.ExpressionNode) tNode.getUserObject();

            thisNode.setIsOr(!thisNode.isOr());
            DefaultTreeModel tmodel =
              (DefaultTreeModel) m_expressionTree.getModel();
            tmodel.nodeStructureChanged(tNode);

            updateExpressionLabel();
          }
        } else {

          showInfoDialog(
            "Please select a node in the tree to alter the boolean operator of",
            "And/Or", false);
        }
      }
    });

    m_toggleNegation.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();
            FlowByExpression.ExpressionNode thisNode =
              (FlowByExpression.ExpressionNode) tNode.getUserObject();

            thisNode.setNegated(!thisNode.isNegated());
            DefaultTreeModel tmodel =
              (DefaultTreeModel) m_expressionTree.getModel();
            tmodel.nodeStructureChanged(tNode);

            updateExpressionLabel();
          }
        } else {
          showInfoDialog(
            "Please select a node in the tree to toggle its negation",
            "Toggle negation", false);
        }
      }
    });

    m_deleteNode.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();

            if (tNode == m_treeRoot) {
              showInfoDialog("You can't delete the root of the tree!",
                "Delete node", true);
            } else {
              FlowByExpression.ExpressionNode thisNode =
                (FlowByExpression.ExpressionNode) tNode.getUserObject();

              FlowByExpression.BracketNode parentNode =
                (FlowByExpression.BracketNode) ((DefaultMutableTreeNode) tNode
                  .getParent()).getUserObject();

              // remove from internal tree structure
              parentNode.removeChild(thisNode);

              // remove from jtree structure
              DefaultTreeModel tmodel =
                (DefaultTreeModel) m_expressionTree.getModel();
              tmodel.removeNodeFromParent(tNode);
              updateExpressionLabel();
            }
          }
        } else {
          showInfoDialog("Please select a node in the tree to delete.",
            "Delete node", false);
        }
      }
    });

    m_addExpressionNode.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();
            FlowByExpression.ExpressionNode thisNode =
              (FlowByExpression.ExpressionNode) tNode.getUserObject();

            if (thisNode instanceof FlowByExpression.BracketNode) {
              FlowByExpression.ExpressionClause newNode =
                new FlowByExpression.ExpressionClause(
                  FlowByExpression.ExpressionClause.ExpressionType.EQUALS,
                  "<att name>", "<value>", false, false);

              ((FlowByExpression.BracketNode) thisNode).addChild(newNode);
              DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(newNode);

              DefaultTreeModel tmodel =
                (DefaultTreeModel) m_expressionTree.getModel();
              tNode.add(childNode);
              tmodel.nodeStructureChanged(tNode);
              updateExpressionLabel();
            } else {
              showInfoDialog(
                "An expression can only be added to a bracket node.",
                "Add expression", false);
            }
          }
        } else {
          showInfoDialog(
            "You must select a bracket node in the tree view to add a new expression to.",
            "Add expression", false);
        }
      }
    });

    m_addBracketNode.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TreePath p = m_expressionTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode tNode =
              (DefaultMutableTreeNode) p.getLastPathComponent();
            FlowByExpression.ExpressionNode thisNode =
              (FlowByExpression.ExpressionNode) tNode.getUserObject();

            if (thisNode instanceof FlowByExpression.BracketNode) {
              FlowByExpression.BracketNode newNode =
                new FlowByExpression.BracketNode();
              ((FlowByExpression.BracketNode) thisNode).addChild(newNode);
              DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(newNode);

              DefaultTreeModel tmodel =
                (DefaultTreeModel) m_expressionTree.getModel();
              tNode.add(childNode);
              tmodel.nodeStructureChanged(tNode);
              updateExpressionLabel();
            } else {
              showInfoDialog(
                "An new bracket node can only be added to an existing bracket node.",
                "Add bracket", false);
            }
          }
        } else {
          showInfoDialog(
            "You must select an existing bracket node in the tree in order to add a new bracket node.",
            "Add bracket", false);
        }
      }
    });
  }

  /**
   * Called when the OK button gets pressed
   */
  @Override
  protected void okPressed() {
    if (m_treeRoot != null) {
      FlowByExpression.ExpressionNode en =
        (FlowByExpression.ExpressionNode) m_treeRoot.getUserObject();
      StringBuffer buff = new StringBuffer();
      en.toStringInternal(buff);

      ((FlowByExpression) getStepToEdit()).setExpressionString(buff.toString());

      if (m_trueData.getSelectedItem() != null
        && m_trueData.getSelectedItem().toString().length() > 0) {
        ((FlowByExpression) getStepToEdit()).setTrueStepName(m_trueData
          .getSelectedItem().toString());
      }

      if (m_falseData.getSelectedItem() != null
        && m_falseData.getSelectedItem().toString().length() > 0) {
        ((FlowByExpression) getStepToEdit()).setFalseStepName(m_falseData
          .getSelectedItem().toString());
      }
    }
  }
}
