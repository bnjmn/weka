package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.JListHelper;
import weka.gui.PropertySheetPanel;

public class SubstringLabelerCustomizer extends JPanel implements
    EnvironmentHandler, BeanCustomizer, CustomizerCloseRequester {
  
  /** For serialization */
  private static final long serialVersionUID = 7636584212353183751L;
  protected Environment m_env = Environment.getSystemWide();
  protected ModifyListener m_modifyL = null;
  protected SubstringLabeler m_labeler;
  
  protected EnvironmentField m_matchAttNameField;
  protected JTextField m_attListField = new JTextField(15);
  protected JTextField m_matchField = new JTextField(15);
  protected JTextField m_labelField = new JTextField(15);
  protected JCheckBox m_regexCheck = new JCheckBox();
  protected JCheckBox m_ignoreCaseCheck = new JCheckBox();
  protected JCheckBox m_nominalBinaryCheck = new JCheckBox();
  protected JCheckBox m_consumeNonMatchingCheck = new JCheckBox();
  
  protected JList m_list = new JList();
  protected DefaultListModel m_listModel;
  
  protected JButton m_newBut = new JButton("New");
  protected JButton m_deleteBut = new JButton("Delete");
  protected JButton m_upBut = new JButton("Move up");
  protected JButton m_downBut = new JButton("Move down");
  
  protected Window m_parent;
  
  protected PropertySheetPanel m_tempEditor =
    new PropertySheetPanel();
  
  public SubstringLabelerCustomizer() {
    setLayout(new BorderLayout());
  }
  
  private void setup() {
    JPanel aboutAndControlHolder = new JPanel();
    aboutAndControlHolder.setLayout(new BorderLayout());
    
    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    JPanel fieldHolder = new JPanel();
    JPanel attListP = new JPanel();
    attListP.setLayout(new BorderLayout());
    attListP.setBorder(BorderFactory.createTitledBorder("Apply to attributes"));
    attListP.add(m_attListField, BorderLayout.CENTER);
    m_attListField.setToolTipText("<html>Accepts a range of indexes (e.g. '1,2,6-10')<br> " +
                "or a comma-separated list of named attributes</html>");
    JPanel matchP = new JPanel();
    matchP.setLayout(new BorderLayout());
    matchP.setBorder(BorderFactory.createTitledBorder("Match"));
    matchP.add(m_matchField, BorderLayout.CENTER);
    JPanel labelP = new JPanel();
    labelP.setLayout(new BorderLayout());
    labelP.setBorder(BorderFactory.createTitledBorder("Label"));
    labelP.add(m_labelField, BorderLayout.CENTER);
    fieldHolder.add(attListP);
    fieldHolder.add(matchP); fieldHolder.add(labelP);
    controlHolder.add(fieldHolder, BorderLayout.NORTH);
    
    JPanel checkHolder = new JPanel();
    checkHolder.setLayout(new GridLayout(0,2));
    JLabel attNameLab = new JLabel("Name of label attribute", SwingConstants.RIGHT);
    checkHolder.add(attNameLab);
    m_matchAttNameField = new EnvironmentField(m_env);
    m_matchAttNameField.setText(m_labeler.getMatchAttributeName());
    checkHolder.add(m_matchAttNameField);
    JLabel regexLab = new JLabel("Match using a regular expression", 
        SwingConstants.RIGHT);
    regexLab.setToolTipText("Use a regular expression rather than literal match");
    checkHolder.add(regexLab);
    checkHolder.add(m_regexCheck);
    JLabel caseLab = new JLabel("Ignore case when matching", 
        SwingConstants.RIGHT);
    checkHolder.add(caseLab);
    checkHolder.add(m_ignoreCaseCheck);
    JLabel nominalBinaryLab = new JLabel("Make binary label attribute nominal");
    nominalBinaryLab.setToolTipText("<html>If the label attribute is binary (i.e. no <br>" +
    		"explicit labels have been declared) then<br>this makes the resulting " +
    		"attribute nominal<br>rather than numeric.</html>");
    checkHolder.add(nominalBinaryLab);
    checkHolder.add(m_nominalBinaryCheck);
    m_nominalBinaryCheck.setSelected(m_labeler.getNominalBinary());
    JLabel consumeNonMatchLab = new JLabel("Consume non-matching instances", 
        SwingConstants.RIGHT);
    consumeNonMatchLab.setToolTipText("<html>When explicit labels have been defined, consume " +
    		"<br>(rather than output with missing value) instances</html>");
    checkHolder.add(consumeNonMatchLab);
    checkHolder.add(m_consumeNonMatchingCheck);
    m_consumeNonMatchingCheck.setSelected(m_labeler.getConsumeNonMatching());
    
    controlHolder.add(checkHolder, BorderLayout.SOUTH);
    
    aboutAndControlHolder.add(controlHolder, BorderLayout.SOUTH);
    JPanel aboutP = m_tempEditor.getAboutPanel();
    aboutAndControlHolder.add(aboutP, BorderLayout.NORTH);
    add(aboutAndControlHolder, BorderLayout.NORTH);
    
    m_list.setVisibleRowCount(5);
    m_deleteBut.setEnabled(false);
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BorderLayout());
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 0));
    butHolder.add(m_newBut); butHolder.add(m_deleteBut);
    butHolder.add(m_upBut); butHolder.add(m_downBut);
    m_upBut.setEnabled(false); m_downBut.setEnabled(false);
    
    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory.
        createTitledBorder("Match-list list (rows applied in order)"));
    listPanel.add(js, BorderLayout.CENTER);
    add(listPanel, BorderLayout.CENTER);
    
    addButtons();
    
    m_list.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteBut.isEnabled()) {
            m_deleteBut.setEnabled(true);
          }
          
          Object entry = m_list.getSelectedValue();
          if (entry != null) {
            SubstringLabeler.Match m = (SubstringLabeler.Match)entry;
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
      public void actionPerformed(ActionEvent e) {
        SubstringLabeler.Match m =
          new SubstringLabeler.Match();
        
        String atts = (m_attListField.getText() != null) 
          ? m_attListField.getText() : "";
        m.setAttsToApplyTo(atts);
        String match = (m_matchField.getText() != null)
          ? m_matchField.getText() : "";
        m.setMatch(match);
        String label = (m_labelField.getText() != null)
          ? m_labelField.getText() : "";
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
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_list);
      }
    });
    
    m_downBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_list);
      }
    });
    
    m_attListField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabeler.Match)m).
            setAttsToApplyTo(m_attListField.getText());
          m_list.repaint();
        }
      }
    });
    
    m_matchField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabeler.Match)m).
            setMatch(m_matchField.getText());
          m_list.repaint();
        }
      }
    });
    
    m_labelField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabeler.Match)m).
            setLabel(m_labelField.getText());
          m_list.repaint();
        }
      }
    });
    
    m_regexCheck.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabeler.Match)m).
            setRegex(m_regexCheck.isSelected());
          m_list.repaint();
        }
      }
    });
    
    m_ignoreCaseCheck.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object m = m_list.getSelectedValue();
        if (m != null) {
          ((SubstringLabeler.Match)m).
            setIgnoreCase(m_ignoreCaseCheck.isSelected());
          m_list.repaint();
        }
      }
    });
    
  }
  
  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");
    
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut); butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);        
    
    okBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {   
        closingOK();
        
        m_parent.dispose();
      }
    });
    
    cancelBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closingCancel();
        
        m_parent.dispose();
      }
    });
  }
  
  protected void initialize() {
    String mString = m_labeler.getMatchDetails();
    m_listModel = new DefaultListModel();
    m_list.setModel(m_listModel);
    
    if (mString != null && mString.length() > 0) {
      String[] parts = mString.split("@@match-rule@@");
      
      if (parts.length > 0) {
        m_upBut.setEnabled(true);
        m_downBut.setEnabled(true);
        for (String mPart : parts) {
          SubstringLabeler.Match m = 
            new SubstringLabeler.Match(mPart);
          m_listModel.addElement(m);
        }
        
        m_list.repaint();
      }
    }
  }
    
  /**
   * Set the SubstringLabeler to edit
   * 
   * @param o the SubtringLabeler to edit
   */
  public void setObject(Object o) {
    if (o instanceof SubstringLabeler) {
      m_labeler = (SubstringLabeler)o;
      m_tempEditor.setTarget(o);
      setup();
      initialize();
    }
  }

  /**
   * Set a reference to the parent window/dialog containing this
   * panel
   * 
   * @param parent the parent window
   */
  public void setParentWindow(Window parent) {
    m_parent = parent;
  }

  /**
   * Set a listener interested in knowing if the object being
   * edited has changed.
   * 
   * @param l the interested listener
   */
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }

  /**
   * Set environment variables to use
   * 
   * @param env
   */
  public void setEnvironment(Environment env) {
    m_env = env;
  }
  
  /**
   * Handle a closing event under an OK condition 
   */
  protected void closingOK() {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < m_listModel.size(); i++) {
      SubstringLabeler.Match m =
        (SubstringLabeler.Match)m_listModel.elementAt(i);
      
      buff.append(m.toStringInternal());
      if (i < m_listModel.size() - 1) {
        buff.append("@@match-rule@@");
      }
    }
    
    m_labeler.setMatchDetails(buff.toString());
    m_labeler.setNominalBinary(m_nominalBinaryCheck.isSelected());
    m_labeler.setConsumeNonMatching(m_consumeNonMatchingCheck.isSelected());
    m_labeler.setMatchAttributeName(m_matchAttNameField.getText());
  }
  
  /**
   * Handle a closing event under a CANCEL condition
   */
  protected void closingCancel() {
    // nothing to do
  }
}
