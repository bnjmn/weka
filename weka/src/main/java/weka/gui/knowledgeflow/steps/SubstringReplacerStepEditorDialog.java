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
 *    SubstringReplacerStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.gui.EnvironmentField;
import weka.gui.JListHelper;
import weka.gui.beans.SubstringReplacerRules;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.SubstringReplacer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Step editor dialog for the SubstringReplacer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SubstringReplacerStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 7804721324137987443L;

  /** Field for specifying the attributes to match on */
  protected EnvironmentField m_attListField;

  /** Field for specifying the string/regex to match */
  protected EnvironmentField m_matchField;

  /** Field for specifying the value to replace a match with */
  protected EnvironmentField m_replaceField;

  /** Checkbox to specify that the match is a regex */
  protected JCheckBox m_regexCheck = new JCheckBox();

  /** Checkbox for specifying that case should be ignored when matching */
  protected JCheckBox m_ignoreCaseCheck = new JCheckBox();

  /** Holds the list of match rules */
  protected JList<SubstringReplacerRules.SubstringReplacerMatchRule> m_list =
    new JList<SubstringReplacerRules.SubstringReplacerMatchRule>();

  /** List model */
  protected DefaultListModel<SubstringReplacerRules.SubstringReplacerMatchRule> m_listModel;

  /** Button for adding a new match rule */
  protected JButton m_newBut = new JButton("New");

  /** Button for deleting a match rule */
  protected JButton m_deleteBut = new JButton("Delete");

  /** Button for moving a match rule up in the list */
  protected JButton m_upBut = new JButton("Move up");

  /** Button for moving a match rule down in the list */
  protected JButton m_downBut = new JButton("Move down");

  /**
   * Initialize the editor
   */
  protected void initialize() {
    String mrString =
      ((SubstringReplacer) getStepToEdit()).getMatchReplaceDetails();
    m_listModel =
      new DefaultListModel<SubstringReplacerRules.SubstringReplacerMatchRule>();
    m_list.setModel(m_listModel);
    if (mrString != null && mrString.length() > 0) {
      String[] parts = mrString.split("@@match-replace@@");

      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String mrPart : parts) {
          SubstringReplacerRules.SubstringReplacerMatchRule mr =
            new SubstringReplacerRules.SubstringReplacerMatchRule(mrPart);
          m_listModel.addElement(mr);
        }

        m_list.repaint();
      }
    }
  }

  /**
   * Layout the editor dialog
   */
  @Override
  protected void layoutEditor() {
    initialize();
    JPanel mainHolder = new JPanel(new BorderLayout());

    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    JPanel fieldHolder = new JPanel();
    JPanel attListP = new JPanel();
    attListP.setLayout(new BorderLayout());
    attListP.setBorder(BorderFactory.createTitledBorder("Apply to attributes"));
    m_attListField = new EnvironmentField(m_env);
    attListP.add(m_attListField, BorderLayout.CENTER);
    attListP
      .setToolTipText("<html>Accepts a range of indexes (e.g. '1,2,6-10')<br> "
        + "or a comma-separated list of named attributes</html>");
    JPanel matchP = new JPanel();
    matchP.setLayout(new BorderLayout());
    matchP.setBorder(BorderFactory.createTitledBorder("Match"));
    m_matchField = new EnvironmentField(m_env);
    matchP.add(m_matchField, BorderLayout.CENTER);
    JPanel replaceP = new JPanel();
    replaceP.setLayout(new BorderLayout());
    replaceP.setBorder(BorderFactory.createTitledBorder("Replace"));
    m_replaceField = new EnvironmentField(m_env);
    replaceP.add(m_replaceField, BorderLayout.CENTER);
    fieldHolder.add(attListP);
    fieldHolder.add(matchP);
    fieldHolder.add(replaceP);
    controlHolder.add(fieldHolder, BorderLayout.NORTH);

    final JPanel checkHolder = new JPanel();
    checkHolder.setLayout(new GridLayout(0, 2));
    JLabel regexLab =
      new JLabel("Match using a regular expression", SwingConstants.RIGHT);
    regexLab
      .setToolTipText("Use a regular expression rather than literal match");
    checkHolder.add(regexLab);
    checkHolder.add(m_regexCheck);
    JLabel caseLab =
      new JLabel("Ignore case when matching", SwingConstants.RIGHT);
    checkHolder.add(caseLab);
    checkHolder.add(m_ignoreCaseCheck);

    controlHolder.add(checkHolder, BorderLayout.SOUTH);

    mainHolder.add(controlHolder, BorderLayout.NORTH);

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
      .createTitledBorder("Match-replace list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);

    mainHolder.add(listPanel, BorderLayout.CENTER);
    add(mainHolder, BorderLayout.CENTER);

    m_attListField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacerRules.SubstringReplacerMatchRule) mr)
            .setAttsToApplyTo(m_attListField.getText());
          m_list.repaint();
        }
      }
    });

    m_matchField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacerRules.SubstringReplacerMatchRule) mr)
            .setMatch(m_matchField.getText());
          m_list.repaint();
        }
      }
    });

    m_replaceField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacerRules.SubstringReplacerMatchRule) mr)
            .setReplace(m_replaceField.getText());
          m_list.repaint();
        }
      }
    });

    m_list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }
          checkUpDown();

          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            SubstringReplacerRules.SubstringReplacerMatchRule mr =
              (SubstringReplacerRules.SubstringReplacerMatchRule) entry;
            m_attListField.setText(mr.getAttsToApplyTo());
            m_matchField.setText(mr.getMatch());
            m_replaceField.setText(mr.getReplace());
            m_regexCheck.setSelected(mr.getRegex());
            m_ignoreCaseCheck.setSelected(mr.getIgnoreCase());
          }
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SubstringReplacerRules.SubstringReplacerMatchRule mr =
          new SubstringReplacerRules.SubstringReplacerMatchRule();

        String atts =
          (m_attListField.getText() != null) ? m_attListField.getText() : "";
        mr.setAttsToApplyTo(atts);
        String match =
          (m_matchField.getText() != null) ? m_matchField.getText() : "";
        mr.setMatch(match);
        String replace =
          (m_replaceField.getText() != null) ? m_replaceField.getText() : "";
        mr.setReplace(replace);
        mr.setRegex(m_regexCheck.isSelected());
        mr.setIgnoreCase(m_ignoreCaseCheck.isSelected());

        m_listModel.addElement(mr);

        if (m_listModel.size() > 1) {
          m_upBut.setEnabled(true);
          m_downBut.setEnabled(true);
        }

        m_list.setSelectedIndex(m_listModel.size() - 1);
        checkUpDown();
      }
    });

    m_deleteBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_list.getSelectedIndex();
        if (selected >= 0) {
          m_listModel.removeElementAt(selected);
          checkUpDown();

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
        checkUpDown();
      }
    });

    m_downBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_list);
        checkUpDown();
      }
    });

    m_regexCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacerRules.SubstringReplacerMatchRule) mr)
            .setRegex(m_regexCheck.isSelected());
          m_list.repaint();
        }
      }
    });

    m_ignoreCaseCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object mr = m_list.getSelectedValue();
        if (mr != null) {
          ((SubstringReplacerRules.SubstringReplacerMatchRule) mr)
            .setIgnoreCase(m_ignoreCaseCheck.isSelected());
          m_list.repaint();
        }
      }
    });
  }

  /**
   * Set the enabled state of the up and down buttons based on the currently
   * selected row in the table
   */
  protected void checkUpDown() {
    if (m_list.getSelectedValue() != null && m_listModel.size() > 1) {
      m_upBut.setEnabled(m_list.getSelectedIndex() > 0);
      m_downBut.setEnabled(m_list.getSelectedIndex() < m_listModel.size() - 1);
    }
  }

  /**
   * Called when the OK button is pressed
   */
  @Override
  protected void okPressed() {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < m_listModel.size(); i++) {
      SubstringReplacerRules.SubstringReplacerMatchRule mr =
        (SubstringReplacerRules.SubstringReplacerMatchRule) m_listModel
          .elementAt(i);

      buff.append(mr.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append("@@match-replace@@");
      }
    }

    ((SubstringReplacer) getStepToEdit()).setMatchReplaceDetails(buff
      .toString());
  }
}
