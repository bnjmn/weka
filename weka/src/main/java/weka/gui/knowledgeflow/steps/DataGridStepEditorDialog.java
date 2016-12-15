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
 *    DataGridStepEditorDialog.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.EnvironmentField;
import weka.gui.JListHelper;
import weka.gui.arffviewer.ArffPanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.DataGrid;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step editor dialog for the data grid
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class DataGridStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = -7314471130292016602L;

  /** Tabbed pane for the two parts of the editor */
  protected JTabbedPane m_tabbedPane = new JTabbedPane();

  /** Panel for viewing/editing the instances data */
  protected ArffViewerPanel m_viewerPanel = new ArffViewerPanel();

  /** Field for editing an attribute name */
  protected EnvironmentField m_attNameField = new EnvironmentField();

  /** Combo box for selecting the type of the attribute */
  protected JComboBox<String> m_attTypeField = new JComboBox<String>();

  /** Field for editing nominal values/date format string */
  protected EnvironmentField m_nominalOrDateFormatField =
    new EnvironmentField();

  /** Holds the list of attribute defs */
  protected JList<AttDef> m_list = new JList<AttDef>();

  /** List model */
  protected DefaultListModel<AttDef> m_listModel;

  /** Button for adding a new attribute def */
  protected JButton m_newBut = new JButton("New");

  /** Button for deleting an attribute def */
  protected JButton m_deleteBut = new JButton("Delete");

  /** Button for moving an attribute up in the list */
  protected JButton m_upBut = new JButton("Move up");

  /** Button for moving an attribute down in the list */
  protected JButton m_downBut = new JButton("Move down");

  /** The instances data from the step as a string */
  protected String m_stringInstances;

  /**
   * Initialize with data from the step
   */
  protected void initialize() {
    m_stringInstances = ((DataGrid) getStepToEdit()).getData();
    m_listModel = new DefaultListModel<>();
    m_list.setModel(m_listModel);

    if (m_stringInstances != null && m_stringInstances.length() > 0) {
      try {
        Instances insts = new Instances(new StringReader(m_stringInstances));
        for (int i = 0; i < insts.numAttributes(); i++) {
          Attribute a = insts.attribute(i);
          String nomOrDate = "";
          if (a.isNominal()) {
            for (int j = 0; j < a.numValues(); j++) {
              nomOrDate += a.value(j) + ",";
            }
            nomOrDate = nomOrDate.substring(0, nomOrDate.length() - 1);
          } else if (a.isDate()) {
            nomOrDate = a.getDateFormat();
          }
          AttDef def = new AttDef(a.name(), a.type(), nomOrDate);
          m_listModel.addElement(def);
        }
        m_viewerPanel.setInstances(insts);
      } catch (Exception ex) {
        showErrorDialog(ex);
      }
    }
  }

  /**
   * Layout the editor
   */
  @Override
  protected void layoutEditor() {
    initialize();
    m_upBut.setEnabled(false);
    m_downBut.setEnabled(false);

    JPanel mainHolder = new JPanel(new BorderLayout());

    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    JPanel fieldHolder = new JPanel();
    fieldHolder.setLayout(new GridLayout(1, 0));

    JPanel attNameP = new JPanel(new BorderLayout());
    attNameP.setBorder(BorderFactory.createTitledBorder("Attribute name"));
    attNameP.add(m_attNameField, BorderLayout.CENTER);
    JPanel attTypeP = new JPanel(new BorderLayout());
    attTypeP.setBorder(BorderFactory.createTitledBorder("Attribute type"));
    attTypeP.add(m_attTypeField, BorderLayout.CENTER);
    m_attTypeField.addItem("numeric");
    m_attTypeField.addItem("nominal");
    m_attTypeField.addItem("date");
    m_attTypeField.addItem("string");
    JPanel nomDateP = new JPanel(new BorderLayout());
    nomDateP.setBorder(BorderFactory
      .createTitledBorder("Nominal vals/date format"));
    nomDateP.add(m_nominalOrDateFormatField, BorderLayout.CENTER);
    fieldHolder.add(attNameP);
    fieldHolder.add(attTypeP);
    fieldHolder.add(nomDateP);

    controlHolder.add(fieldHolder, BorderLayout.NORTH);
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

    listPanel.add(butHolder, BorderLayout.NORTH);
    JScrollPane js = new JScrollPane(m_list);
    js.setBorder(BorderFactory.createTitledBorder("Attributes"));
    listPanel.add(js, BorderLayout.CENTER);

    mainHolder.add(listPanel, BorderLayout.CENTER);

    m_tabbedPane.addTab("Attribute definitions", mainHolder);
    m_tabbedPane.addTab("Data", m_viewerPanel);

    add(m_tabbedPane, BorderLayout.CENTER);

    m_attNameField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        AttDef a = m_list.getSelectedValue();
        if (a != null) {
          a.m_name = m_attNameField.getText();
          m_list.repaint();
        }
      }
    });

    m_nominalOrDateFormatField
      .addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          AttDef a = m_list.getSelectedValue();
          if (a != null) {
            a.m_nomOrDate = m_nominalOrDateFormatField.getText();
            m_list.repaint();
          }
        }
      });

    m_attTypeField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AttDef a = m_list.getSelectedValue();
        if (a != null) {
          String type = m_attTypeField.getSelectedItem().toString();
          a.m_type = AttDef.attStringToType(type);
          m_list.repaint();
        }
      }
    });

    m_tabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (m_tabbedPane.getSelectedIndex() == 1) {
          handleTabChange();
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

          AttDef entry = m_list.getSelectedValue();
          if (entry != null) {
            m_attNameField.setText(entry.m_name);
            m_attTypeField.setSelectedItem(Attribute.typeToString(entry.m_type));
            m_nominalOrDateFormatField
              .setText(entry.m_nomOrDate != null ? entry.m_nomOrDate : "");
          }
        }
      }
    });

    m_newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AttDef def =
          new AttDef(m_attNameField.getText(), AttDef
            .attStringToType(m_attTypeField.getSelectedItem().toString()),
            m_nominalOrDateFormatField.getText());

        m_listModel.addElement(def);
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
  }

  /**
   * Set the enabled state of the up and down buttons based on the
   * currently selected row in the table
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
  public void okPressed() {
    Instances current = m_viewerPanel.getInstances();
    if (current != null) {
      ((DataGrid) getStepToEdit()).setData(current.toString());
    } else {
      ((DataGrid) getStepToEdit()).setData("");
    }
  }

  /**
   * Called when the user changes to the data tab
   */
  protected void handleTabChange() {
    // first construct empty instances from attribute definitions
    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (AttDef a : Collections.list(m_listModel.elements())) {
      if (a.m_type == Attribute.NUMERIC) {
        atts.add(new Attribute(a.m_name));
      } else if (a.m_type == Attribute.STRING) {
        atts.add(new Attribute(a.m_name, (List<String>) null));
      } else if (a.m_type == Attribute.DATE) {
        atts.add(new Attribute(a.m_name, a.m_nomOrDate));
      } else if (a.m_type == Attribute.NOMINAL) {
        List<String> vals = new ArrayList<String>();
        for (String v : a.m_nomOrDate.split(",")) {
          vals.add(v.trim());
        }
        atts.add(new Attribute(a.m_name, vals));
      } else if (a.m_type == Attribute.RELATIONAL) {
        // TODO
      }
    }
    Instances defInsts = new Instances("DataGrid", atts, 0);

    // get current editor instances
    Instances editInsts = m_viewerPanel.getInstances();
    if (editInsts != null) {
      // if there is no data in the editor then just overwrite with
      // the current definition
      if (editInsts.numInstances() == 0) {
        m_viewerPanel.setInstances(defInsts);
      } else {
        Map<Integer, Integer> transferMap = new HashMap<>();
        for (int i = 0; i < editInsts.numAttributes(); i++) {
          Attribute eA = editInsts.attribute(i);
          Attribute dA = defInsts.attribute(eA.name());
          if (dA != null && dA.type() == eA.type()) {
            transferMap.put(i, dA.index());
          }
        }

        if (transferMap.size() > 0) {
          Instances defCopy = new Instances(defInsts, 0);
          for (int i = 0; i < editInsts.numInstances(); i++) {
            double[] vals = new double[defCopy.numAttributes()];
            Instance editInst = editInsts.instance(i);
            for (int j = 0; j < vals.length; j++) {
              vals[j] = Utils.missingValue();
            }
            for (Map.Entry<Integer, Integer> e : transferMap.entrySet()) {
              if (editInst.attribute(e.getKey()).isNumeric()) {
                vals[e.getValue()] = editInst.value(e.getKey());
              } else if (editInst.attribute(e.getKey()).isNominal()) {
                if (!editInst.isMissing(e.getKey())) {
                  int defIndex =
                    defCopy.attribute(e.getValue()).indexOfValue(
                      editInst.stringValue(e.getKey()));
                  vals[e.getValue()] =
                    defIndex >= 0 ? defIndex : Utils.missingValue();
                }
              } else if (editInst.attribute(e.getKey()).isString()) {
                if (!editInst.isMissing(e.getKey())) {
                  String editVal = editInst.stringValue(e.getKey());
                  vals[e.getValue()] =
                    defCopy.attribute(e.getValue()).addStringValue(editVal);
                }
              } else if (editInst.attribute(e.getKey()).isRelationValued()) {
                // TODO
              }
            }
            defCopy.add(new DenseInstance(1.0, vals));
          }
          m_viewerPanel.setInstances(defCopy);
        } else {
          // nothing in common between the two
          m_viewerPanel.setInstances(defInsts);
        }
      }
    } else {
      m_viewerPanel.setInstances(defInsts);
    }
  }

  /**
   * Small holder class for attribute information
   */
  protected static class AttDef {
    protected String m_name = "";
    protected int m_type = Attribute.NUMERIC;
    protected String m_nomOrDate = "";

    /**
     * Constructor
     *
     * @param name name of the attribute
     * @param type type of the attribute
     * @param nomOrDate nominal values or date format string
     */
    public AttDef(String name, int type, String nomOrDate) {
      m_name = name;
      m_type = type;
      m_nomOrDate = nomOrDate;
    }

    /**
     * Creates a string representation of the attribute that
     * gets displayed in the list
     *
     * @return a string representation of the attribute
     */
    public String toString() {
      String result =
        "@attribute "
          + m_name
          + " "
          + (m_type != Attribute.NOMINAL ? Attribute.typeToString(m_type)
            : " {");
      if (m_type == Attribute.NOMINAL) {
        result += m_nomOrDate + "}";
      } else if (m_type == Attribute.DATE) {
        result += " " + m_nomOrDate;
      }

      return result;
    }

    /**
     * Convert a string attribute type to the corresponding integer type code
     *
     * @param type the string attribute type
     * @return the integer code
     */
    public static int attStringToType(String type) {
      if (type.equals("numeric")) {
        return Attribute.NUMERIC;
      } else if (type.equals("nominal")) {
        return Attribute.NOMINAL;
      } else if (type.equals("string")) {
        return Attribute.STRING;
      } else if (type.equals("date")) {
        return Attribute.DATE;
      } else if (type.equals("relational")) {
        return Attribute.RELATIONAL;
      }
      return Attribute.NUMERIC;
    }
  }

  /**
   * Small wrapper pane for the ArffPanel. Provides an undo and
   * add instance button
   */
  protected static class ArffViewerPanel extends JPanel {
    private static final long serialVersionUID = 2965315087365186710L;

    /** Click to undo the last action */
    protected JButton m_UndoButton = new JButton("Undo");

    /** Click to add a new instance to the end of the dataset */
    protected JButton m_addInstanceButton = new JButton("Add instance");

    /** the panel to display the Instances-object */
    protected ArffPanel m_ArffPanel = new ArffPanel();

    /**
     * Constructor
     */
    public ArffViewerPanel() {

      setLayout(new BorderLayout());
      add(m_ArffPanel, BorderLayout.CENTER);

      // Buttons
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(m_addInstanceButton);
      panel.add(m_UndoButton);

      add(panel, BorderLayout.SOUTH);
      m_UndoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          undo();
        }
      });
      m_addInstanceButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_ArffPanel.addInstanceAtEnd();
        }
      });
    }

    /**
     * sets the instances to display
     */
    public void setInstances(Instances inst) {
      m_ArffPanel.setInstances(new Instances(inst));
      m_ArffPanel.setOptimalColWidths();
    }

    /**
     * returns the currently displayed instances
     */
    public Instances getInstances() {
      return m_ArffPanel.getInstances();
    }

    /**
     * sets the state of the buttons
     */
    protected void setButtons() {
      m_UndoButton.setEnabled(m_ArffPanel.canUndo());
    }

    /**
     * undoes the last action
     */
    private void undo() {
      m_ArffPanel.undo();
    }
  }
}
