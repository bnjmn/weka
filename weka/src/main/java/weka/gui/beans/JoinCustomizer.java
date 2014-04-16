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
 *    JoinCustomizer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.gui.JListHelper;
import weka.gui.PropertySheetPanel;

/**
 * Customizer component for the Join step
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JoinCustomizer extends JPanel implements EnvironmentHandler,
  BeanCustomizer, CustomizerCloseRequester {

  /** For serialization */
  private static final long serialVersionUID = 5797383368777382010L;
  protected Environment m_env = Environment.getSystemWide();
  protected ModifyListener m_modifyL = null;

  protected Join m_join;

  protected JComboBox m_firstKeyFields = new EnvironmentField.WideComboBox();
  protected JComboBox m_secondKeyFields = new EnvironmentField.WideComboBox();

  protected JList m_firstList = new JList();
  protected JList m_secondList = new JList();
  protected DefaultListModel m_firstListModel;
  protected DefaultListModel m_secondListModel;

  protected JButton m_addOneBut = new JButton("Add");
  protected JButton m_deleteOneBut = new JButton("Delete");
  protected JButton m_upOneBut = new JButton("Up");
  protected JButton m_downOneBut = new JButton("Down");

  protected JButton m_addTwoBut = new JButton("Add");
  protected JButton m_deleteTwoBut = new JButton("Delete");
  protected JButton m_upTwoBut = new JButton("Up");
  protected JButton m_downTwoBut = new JButton("Down");

  protected Window m_parent;

  protected PropertySheetPanel m_tempEditor = new PropertySheetPanel();

  public JoinCustomizer() {
    setLayout(new BorderLayout());
  }

  @SuppressWarnings("unchecked")
  private void setup() {
    JPanel aboutAndControlHolder = new JPanel();
    aboutAndControlHolder.setLayout(new BorderLayout());

    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());

    // input source names
    JPanel firstSourceP = new JPanel();
    firstSourceP.setLayout(new BorderLayout());
    firstSourceP.add(new JLabel("First input ", SwingConstants.RIGHT),
      BorderLayout.CENTER);
    Object firstInput = m_join.getFirstInput();
    String firstName = "<not connected>";
    if (firstInput != null && firstInput instanceof BeanCommon) {
      firstName = ((BeanCommon) firstInput).getCustomName();
    }
    firstSourceP.add(new JLabel(firstName, SwingConstants.LEFT),
      BorderLayout.EAST);

    JPanel secondSourceP = new JPanel();
    secondSourceP.setLayout(new BorderLayout());
    secondSourceP.add(new JLabel("Second input ", SwingConstants.RIGHT),
      BorderLayout.CENTER);
    Object secondInput = m_join.getSecondInput();
    String secondName = "<not connected>";
    if (secondInput != null && secondInput instanceof BeanCommon) {
      secondName = ((BeanCommon) secondInput).getCustomName();
    }
    secondSourceP.add(new JLabel(secondName, SwingConstants.LEFT),
      BorderLayout.EAST);

    JPanel sourcePHolder = new JPanel();
    sourcePHolder.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    sourcePHolder.setLayout(new BorderLayout());
    sourcePHolder.add(firstSourceP, BorderLayout.NORTH);
    sourcePHolder.add(secondSourceP, BorderLayout.SOUTH);
    controlHolder.add(sourcePHolder, BorderLayout.NORTH);

    // lists and controls
    m_firstList.setVisibleRowCount(5);
    m_secondList.setVisibleRowCount(5);

    m_firstKeyFields.setEditable(true);
    JPanel listOneP = new JPanel();
    m_deleteOneBut.setEnabled(false);
    listOneP.setLayout(new BorderLayout());
    JPanel butOneHolder = new JPanel();
    butOneHolder.setLayout(new GridLayout(1, 0));
    butOneHolder.add(m_addOneBut);
    butOneHolder.add(m_deleteOneBut);
    butOneHolder.add(m_upOneBut);
    butOneHolder.add(m_downOneBut);
    m_upOneBut.setEnabled(false);
    m_downOneBut.setEnabled(false);

    JPanel fieldsAndButsOne = new JPanel();
    fieldsAndButsOne.setLayout(new BorderLayout());
    fieldsAndButsOne.add(m_firstKeyFields, BorderLayout.NORTH);
    fieldsAndButsOne.add(butOneHolder, BorderLayout.SOUTH);
    listOneP.add(fieldsAndButsOne, BorderLayout.NORTH);
    JScrollPane js1 = new JScrollPane(m_firstList);

    js1.setBorder(BorderFactory.createTitledBorder("First input key fields"));
    listOneP.add(js1, BorderLayout.CENTER);

    controlHolder.add(listOneP, BorderLayout.WEST);

    m_secondKeyFields.setEditable(true);
    JPanel listTwoP = new JPanel();
    m_deleteTwoBut.setEnabled(false);
    listTwoP.setLayout(new BorderLayout());
    JPanel butTwoHolder = new JPanel();
    butTwoHolder.setLayout(new GridLayout(1, 0));
    butTwoHolder.add(m_addTwoBut);
    butTwoHolder.add(m_deleteTwoBut);
    butTwoHolder.add(m_upTwoBut);
    butTwoHolder.add(m_downTwoBut);
    m_upTwoBut.setEnabled(false);
    m_downTwoBut.setEnabled(false);

    JPanel fieldsAndButsTwo = new JPanel();
    fieldsAndButsTwo.setLayout(new BorderLayout());
    fieldsAndButsTwo.add(m_secondKeyFields, BorderLayout.NORTH);
    fieldsAndButsTwo.add(butTwoHolder, BorderLayout.SOUTH);

    listTwoP.add(fieldsAndButsTwo, BorderLayout.NORTH);
    JScrollPane js2 = new JScrollPane(m_secondList);

    js2.setBorder(BorderFactory.createTitledBorder("Second input key fields"));
    listTwoP.add(js2, BorderLayout.CENTER);

    controlHolder.add(listTwoP, BorderLayout.EAST);

    aboutAndControlHolder.add(controlHolder, BorderLayout.SOUTH);
    JPanel aboutP = m_tempEditor.getAboutPanel();
    aboutAndControlHolder.add(aboutP, BorderLayout.NORTH);
    add(aboutAndControlHolder, BorderLayout.NORTH);

    // setup incoming atts combos
    if (m_join.getFirstInputStructure() != null) {
      m_firstKeyFields.removeAllItems();
      Instances incoming = m_join.getFirstInputStructure();
      for (int i = 0; i < incoming.numAttributes(); i++) {
        m_firstKeyFields.addItem(incoming.attribute(i).name());
      }
    }

    if (m_join.getSecondInputStructure() != null) {
      m_secondKeyFields.removeAllItems();
      Instances incoming = m_join.getSecondInputStructure();
      for (int i = 0; i < incoming.numAttributes(); i++) {
        m_secondKeyFields.addItem(incoming.attribute(i).name());
      }
    }

    m_firstList.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteOneBut.isEnabled()) {
            m_deleteOneBut.setEnabled(true);
          }
        }
      }
    });

    m_secondList.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (!m_deleteTwoBut.isEnabled()) {
            m_deleteTwoBut.setEnabled(true);
          }
        }
      }
    });

    m_addOneBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_firstKeyFields.getSelectedItem() != null
          && m_firstKeyFields.getSelectedItem().toString().length() > 0) {
          m_firstListModel.addElement(m_firstKeyFields.getSelectedItem());

          if (m_firstListModel.size() > 1) {
            m_upOneBut.setEnabled(true);
            m_downOneBut.setEnabled(true);
          }
        }
      }
    });

    m_addTwoBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_secondKeyFields.getSelectedItem() != null
          && m_secondKeyFields.getSelectedItem().toString().length() > 0) {

          m_secondListModel.addElement(m_secondKeyFields.getSelectedItem());
          if (m_secondListModel.size() > 1) {
            m_upTwoBut.setEnabled(true);
            m_downTwoBut.setEnabled(true);
          }
        }
      }
    });

    m_deleteOneBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_firstList.getSelectedIndex();
        if (selected >= 0) {
          m_firstListModel.remove(selected);
        }

        if (m_firstListModel.size() <= 1) {
          m_upOneBut.setEnabled(false);
          m_downOneBut.setEnabled(false);
        }
      }
    });

    m_deleteTwoBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = m_secondList.getSelectedIndex();
        if (selected >= 0) {
          m_secondListModel.remove(selected);
        }

        if (m_secondListModel.size() <= 1) {
          m_upTwoBut.setEnabled(false);
          m_downTwoBut.setEnabled(false);
        }
      }
    });

    m_upOneBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_firstList);
        ;
      }
    });

    m_upTwoBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveUp(m_secondList);
      }
    });

    m_downOneBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_firstList);
      }
    });

    m_downTwoBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JListHelper.moveDown(m_secondList);
        ;
      }
    });

    addButtons();
  }

  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingOK();

        m_parent.dispose();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingCancel();

        m_parent.dispose();
      }
    });
  }

  @SuppressWarnings("unchecked")
  protected void initialize() {
    m_firstListModel = new DefaultListModel();
    m_secondListModel = new DefaultListModel();
    m_firstList.setModel(m_firstListModel);
    m_secondList.setModel(m_secondListModel);

    String keySpec = m_join.getKeySpec();
    if (keySpec != null && keySpec.length() > 0) {
      try {
        keySpec = m_env.substitute(keySpec);
      } catch (Exception ex) {
      }

      String[] parts = keySpec.split(Join.KEY_SPEC_SEPARATOR);
      if (parts.length > 0) {
        String[] firstParts = parts[0].trim().split(",");
        for (String s : firstParts) {
          m_firstListModel.addElement(s);
        }
      }

      if (parts.length > 1) {
        String[] secondParts = parts[1].trim().split(",");
        for (String s : secondParts) {
          m_secondListModel.addElement(s);
        }
      }
    }
  }

  @Override
  public void setObject(Object bean) {
    if (bean instanceof Join) {
      m_join = (Join) bean;
      m_tempEditor.setTarget(bean);
      setup();
      initialize();
    }
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  private void closingOK() {
    StringBuilder b = new StringBuilder();

    for (int i = 0; i < m_firstListModel.size(); i++) {
      if (i != 0) {
        b.append(",");
      }
      b.append(m_firstListModel.get(i));
    }
    b.append(Join.KEY_SPEC_SEPARATOR);
    for (int i = 0; i < m_secondListModel.size(); i++) {
      if (i != 0) {
        b.append(",");
      }
      b.append(m_secondListModel.get(i));
    }

    m_join.setKeySpec(b.toString());
  }

  private void closingCancel() {
    // nothing to do
  }
}
