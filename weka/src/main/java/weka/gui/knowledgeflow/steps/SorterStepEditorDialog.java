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
 *    SorterStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.JListHelper;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.Sorter;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Step editor dialog for the Sorter step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SorterStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -1258170590422372948L;

  /** Combo box for selecting attribute to sort on */
  protected JComboBox<String> m_attCombo = new JComboBox<String>();

  /** Combo box for choosing to sort descending or not */
  protected JComboBox<String> m_descending = new JComboBox<String>();

  /** Holds the individual sorting criteria */
  protected JList<Sorter.SortRule> m_list = new JList<Sorter.SortRule>();

  /** List model for sort rules */
  protected DefaultListModel<Sorter.SortRule> m_listModel;

  /** Button for creating a new rule */
  protected JButton m_newBut = new JButton("New");

  /** Button for deleting a selected rule */
  protected JButton m_deleteBut = new JButton("Delete");

  /** Button for moving a rule up in the list */
  protected JButton m_upBut = new JButton("Move up");

  /** Button for moving a rule down in the list */
  protected JButton m_downBut = new JButton("Move down");

  /**
   * Set the step to edit
   *
   * @param step the step to edit
   */
  @Override
  protected void setStepToEdit(Step step) {
    copyOriginal(step);
    createAboutPanel(step);
    m_editor = new PropertySheetPanel(false);
    m_editor.setUseEnvironmentPropertyEditors(true);
    m_editor.setEnvironment(m_env);
    m_editor.setTarget(m_stepToEdit);

    m_primaryEditorHolder.setLayout(new BorderLayout());
    m_primaryEditorHolder.add(m_editor, BorderLayout.CENTER);

    m_editorHolder.setLayout(new BorderLayout());
    m_editorHolder.add(m_primaryEditorHolder, BorderLayout.NORTH);
    m_editorHolder.add(createSorterPanel(), BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);

    String sString = ((Sorter) getStepToEdit()).getSortDetails();
    m_listModel = new DefaultListModel<Sorter.SortRule>();
    m_list.setModel(m_listModel);
    if (sString != null && sString.length() > 0) {
      String[] parts = sString.split("@@sort-rule@@");

      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String sPart : parts) {
          Sorter.SortRule s = new Sorter.SortRule(sPart);
          m_listModel.addElement(s);
        }
      }

      m_list.repaint();
    }

    // try to set up attribute combo
    if (((Sorter) getStepToEdit()).getStepManager().numIncomingConnections() > 0) {
      // sorter can only have a single incoming connection - get the name
      // of it
      String incomingConnName =
        ((Sorter) getStepToEdit()).getStepManager().getIncomingConnections()
          .keySet().iterator().next();
      try {
        Instances connectedFormat =
          ((Sorter) getStepToEdit()).getStepManager()
            .getIncomingStructureForConnectionType(incomingConnName);
        if (connectedFormat != null) {
          for (int i = 0; i < connectedFormat.numAttributes(); i++) {
            m_attCombo.addItem(connectedFormat.attribute(i).name());
          }
        }
      } catch (WekaException ex) {
        showErrorDialog(ex);
      }
    }
  }

  /**
   * Creates the sort panel
   *
   * @return a {@code JPanel}
   */
  protected JPanel createSorterPanel() {
    JPanel sorterPanel = new JPanel(new BorderLayout());

    JPanel fieldHolder = new JPanel();
    fieldHolder.setLayout(new GridLayout(0, 2));

    JPanel attListP = new JPanel();
    attListP.setLayout(new BorderLayout());
    attListP.setBorder(BorderFactory.createTitledBorder("Sort on attribute"));
    attListP.add(m_attCombo, BorderLayout.CENTER);
    m_attCombo.setEditable(true);
    m_attCombo.setToolTipText("<html>Accepts an attribute name, index or <br> "
      + "the special string \"/first\" and \"/last\"</html>");

    m_descending.addItem("No");
    m_descending.addItem("Yes");
    JPanel descendingP = new JPanel();
    descendingP.setLayout(new BorderLayout());
    descendingP.setBorder(BorderFactory.createTitledBorder("Sort descending"));
    descendingP.add(m_descending, BorderLayout.CENTER);

    fieldHolder.add(attListP);
    fieldHolder.add(descendingP);

    sorterPanel.add(fieldHolder, BorderLayout.NORTH);

    m_list.setVisibleRowCount(5);
    m_deleteBut.setEnabled(false);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BorderLayout());

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 0));
    butHolder.add(m_newBut);
    butHolder.add(m_deleteBut);
    butHolder.add(m_upBut);
    butHolder.add(m_downBut);
    m_upBut.setEnabled(false);
    m_downBut.setEnabled(false);

    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory
      .createTitledBorder("Sort-by list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);

    sorterPanel.add(listPanel, BorderLayout.CENTER);

    m_list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }

          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            Sorter.SortRule m = (Sorter.SortRule) entry;
            m_attCombo.setSelectedItem(m.getAttribute());
            if (m.getDescending()) {
              m_descending.setSelectedIndex(1);
            } else {
              m_descending.setSelectedIndex(0);
            }
          }
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Sorter.SortRule m = new Sorter.SortRule();

        String att =
          (m_attCombo.getSelectedItem() != null) ? m_attCombo.getSelectedItem()
            .toString() : "";
        m.setAttribute(att);
        m.setDescending(m_descending.getSelectedIndex() == 1);

        m_listModel.addElement(m);

        if (m_listModel.size() > 1) {
          m_upBut.setEnabled(true);
          m_downBut.setEnabled(true);
        }

        m_list.setSelectedIndex(m_listModel.size() - 1);
      }
    });

    m_deleteBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_list.getSelectedIndex();
        if (selected >= 0) {
          m_listModel.removeElementAt(selected);

          if (m_listModel.size() <= 1) {
            m_upBut.setEnabled(false);
            m_downBut.setEnabled(false);
          }
        }
      }
    });

    m_upBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_list);
      }
    });

    m_downBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_list);
      }
    });

    m_attCombo.getEditor().getEditorComponent()
      .addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          Object m = m_list.getSelectedValue();
          String text = "";
          if (m_attCombo.getSelectedItem() != null) {
            text = m_attCombo.getSelectedItem().toString();
          }
          java.awt.Component theEditor =
            m_attCombo.getEditor().getEditorComponent();
          if (theEditor instanceof JTextField) {
            text = ((JTextField) theEditor).getText();
          }
          if (m != null) {
            ((Sorter.SortRule) m).setAttribute(text);
            m_list.repaint();
          }
        }
      });

    m_attCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        Object selected = m_attCombo.getSelectedItem();
        if (m != null && selected != null) {
          ((Sorter.SortRule) m).setAttribute(selected.toString());
          m_list.repaint();
        }
      }
    });

    m_descending.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((Sorter.SortRule) m).setDescending(m_descending.getSelectedIndex() == 1);
          m_list.repaint();
        }
      }
    });

    return sorterPanel;
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  public void okPressed() {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < m_listModel.size(); i++) {
      Sorter.SortRule m = (Sorter.SortRule) m_listModel.elementAt(i);

      buff.append(m.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append("@@sort-rule@@");
      }
    }

    ((Sorter) getStepToEdit()).setSortDetails(buff.toString());
  }
}
