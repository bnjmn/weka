package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.JListHelper;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SubstringLabelerFilterCustomizer extends JPanel implements
  GOECustomizer, EnvironmentHandler {

  private static final long serialVersionUID = -824609973469186671L;

  /** The substring replacer filter to edit */
  protected weka.filters.unsupervised.attribute.SubstringLabeler m_filter;

  protected Environment m_env = Environment.getSystemWide();

  protected EnvironmentField m_matchAttNameField;
  protected EnvironmentField m_attListField;
  protected EnvironmentField m_matchField;
  protected EnvironmentField m_labelField;
  protected JCheckBox m_regexCheck = new JCheckBox();
  protected JCheckBox m_ignoreCaseCheck = new JCheckBox();
  protected JCheckBox m_nominalBinaryCheck = new JCheckBox();
  protected JCheckBox m_consumeNonMatchingCheck = new JCheckBox();

  protected JList<SubstringLabelerRules.SubstringLabelerMatchRule> m_list =
    new JList<SubstringLabelerRules.SubstringLabelerMatchRule>();
  protected DefaultListModel<SubstringLabelerRules.SubstringLabelerMatchRule> m_listModel;

  protected JButton m_newBut = new JButton("New");
  protected JButton m_deleteBut = new JButton("Delete");
  protected JButton m_upBut = new JButton("Move up");
  protected JButton m_downBut = new JButton("Move down");

  protected void initialize() {
    String mlString = m_filter.convertToSingleInternalString();
    m_listModel =
      new DefaultListModel<SubstringLabelerRules.SubstringLabelerMatchRule>();
    m_list.setModel(m_listModel);
    if (mlString != null && mlString.length() > 0) {
      String[] parts =
        mlString.split(SubstringLabelerRules.MATCH_RULE_SEPARATOR);
      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String mPart : parts) {
          SubstringLabelerRules.SubstringLabelerMatchRule m =
            new SubstringLabelerRules.SubstringLabelerMatchRule(mPart);
          m_listModel.addElement(m);
        }

        m_list.repaint();
      }
    }
  }

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
    JPanel labelP = new JPanel();
    labelP.setLayout(new BorderLayout());
    labelP.setBorder(BorderFactory.createTitledBorder("Label"));
    m_labelField = new EnvironmentField(m_env);
    labelP.add(m_labelField, BorderLayout.CENTER);
    fieldHolder.add(attListP);
    fieldHolder.add(matchP);
    fieldHolder.add(labelP);

    controlHolder.add(fieldHolder, BorderLayout.NORTH);

    JPanel checkHolder = new JPanel();
    checkHolder.setLayout(new GridLayout(0, 2));
    JLabel attNameLab =
      new JLabel("Name of label attribute", SwingConstants.RIGHT);
    checkHolder.add(attNameLab);
    m_matchAttNameField = new EnvironmentField(m_env);
    m_matchAttNameField.setText(m_filter.getLabelAttributeName());
    checkHolder.add(m_matchAttNameField);
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
    JLabel nominalBinaryLab =
      new JLabel("Make binary label attribute nominal", SwingConstants.RIGHT);
    nominalBinaryLab
      .setToolTipText("<html>If the label attribute is binary (i.e. no <br>"
        + "explicit labels have been declared) then<br>this makes the resulting "
        + "attribute nominal<br>rather than numeric.</html>");
    checkHolder.add(nominalBinaryLab);
    checkHolder.add(m_nominalBinaryCheck);
    m_nominalBinaryCheck.setSelected(m_filter
      .getMakeBinaryLabelAttributeNominal());
    JLabel consumeNonMatchLab =
      new JLabel("Consume non-matching instances", SwingConstants.RIGHT);
    consumeNonMatchLab
      .setToolTipText("<html>When explicit labels have been defined, consume "
        + "<br>(rather than output with missing value) instances</html>");
    checkHolder.add(consumeNonMatchLab);
    checkHolder.add(m_consumeNonMatchingCheck);
    m_consumeNonMatchingCheck.setSelected(m_filter.getConsumeNonMatching());

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
      .createTitledBorder("Match-list list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);

    mainHolder.add(listPanel, BorderLayout.CENTER);
    add(mainHolder, BorderLayout.CENTER);

    m_attListField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabelerRules.SubstringLabelerMatchRule) m)
            .setAttsToApplyTo(m_attListField.getText());
          m_list.repaint();
        }
      }
    });

    m_matchField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabelerRules.SubstringLabelerMatchRule) m)
            .setMatch(m_matchField.getText());
          m_list.repaint();
        }
      }
    });

    m_labelField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabelerRules.SubstringLabelerMatchRule) m)
            .setLabel(m_labelField.getText());
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

          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            SubstringLabelerRules.SubstringLabelerMatchRule m =
              (SubstringLabelerRules.SubstringLabelerMatchRule) entry;
            m_attListField.setText(m.getAttsToApplyTo());
            m_matchField.setText(m.getMatch());
            m_labelField.setText(m.getLabel());
            m_regexCheck.setSelected(m.getRegex());
            m_ignoreCaseCheck.setSelected(m.getIgnoreCase());
          }
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SubstringLabelerRules.SubstringLabelerMatchRule m =
          new SubstringLabelerRules.SubstringLabelerMatchRule();

        String atts =
          (m_attListField.getText() != null) ? m_attListField.getText() : "";
        m.setAttsToApplyTo(atts);
        String match =
          (m_matchField.getText() != null) ? m_matchField.getText() : "";
        m.setMatch(match);
        String label =
          (m_labelField.getText() != null) ? m_labelField.getText() : "";
        m.setLabel(label);
        m.setRegex(m_regexCheck.isSelected());
        m.setIgnoreCase(m_ignoreCaseCheck.isSelected());

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

    m_regexCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabelerRules.SubstringLabelerMatchRule) m)
            .setRegex(m_regexCheck.isSelected());
          m_list.repaint();
        }
      }
    });

    m_ignoreCaseCheck.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabelerRules.SubstringLabelerMatchRule) m)
            .setIgnoreCase(m_ignoreCaseCheck.isSelected());
          m_list.repaint();
        }
      }
    });
  }

  @Override
  public void setEnvironment(Environment env) {

  }

  @Override
  public void dontShowOKCancelButtons() {

  }

  @Override
  public void closingOK() {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < m_listModel.size(); i++) {
      SubstringLabelerRules.SubstringLabelerMatchRule mr =
        (SubstringLabelerRules.SubstringLabelerMatchRule) m_listModel
          .elementAt(i);

      buff.append(mr.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append(SubstringLabelerRules.MATCH_RULE_SEPARATOR);
      }
    }

    m_filter.convertFromSingleInternalString(buff.toString());
  }

  @Override
  public void closingCancel() {

  }

  @Override
  public void setModifiedListener(ModifyListener l) {

  }

  @Override
  public void setObject(Object bean) {
    if (!(bean instanceof weka.filters.unsupervised.attribute.SubstringLabeler)) {
      throw new IllegalArgumentException("Object must be a SubstringLabeler!");
    }

    m_filter = (weka.filters.unsupervised.attribute.SubstringLabeler) bean;
    layoutEditor();
  }
}
