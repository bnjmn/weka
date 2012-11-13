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
 *    EvaluationMetricSelectionDialog.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * A GUI dialog for selecting classification/regression evaluation metrics to be
 * output.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class EvaluationMetricSelectionDialog extends JDialog {

  /** For serialization */
  private static final long serialVersionUID = 4451184027143094270L;

  /** The list of selected evaluation metrics */
  protected List<String> m_selectedEvalMetrics;

  /**
   * Constructor
   * 
   * @param parent parent Dialog
   * @param evalMetrics initial list of selected evaluation metrics
   */
  public EvaluationMetricSelectionDialog(Dialog parent, List<String> evalMetrics) {
    super(parent, "Manage evaluation metrics", ModalityType.DOCUMENT_MODAL);
    m_selectedEvalMetrics = evalMetrics;
    init();
  }

  /**
   * Constructor
   * 
   * @param parent parent Window
   * @param evalMetrics initial list of selected evaluation metrics
   */
  public EvaluationMetricSelectionDialog(Window parent, List<String> evalMetrics) {
    super(parent, "Manage evaluation metrics", ModalityType.DOCUMENT_MODAL);
    m_selectedEvalMetrics = evalMetrics;
    init();
  }

  /**
   * Constructor
   * 
   * @param parent parent Frame
   * @param evalMetrics initial list of selected evaluation metrics
   */
  public EvaluationMetricSelectionDialog(Frame parent, List<String> evalMetrics) {
    super(parent, "Manage evaluation metrics", ModalityType.DOCUMENT_MODAL);
    m_selectedEvalMetrics = evalMetrics;
    init();
  }

  /**
   * Get the list of selected evaluation metrics
   * 
   * @return the list of selected evaluation metrics
   */
  public List<String> getSelectedEvalMetrics() {
    return m_selectedEvalMetrics;
  }

  private void init() {
    final weka.gui.AttributeSelectionPanel evalConfigurer = new weka.gui.AttributeSelectionPanel(
        true, true, true, true);

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    List<String> allEvalMetrics = Evaluation.getAllEvaluationMetricNames();
    for (String s : allEvalMetrics) {
      atts.add(new Attribute(s));
    }

    final Instances metricInstances = new Instances("Metrics", atts, 1);
    boolean[] selectedMetrics = new boolean[metricInstances.numAttributes()];
    if (m_selectedEvalMetrics == null) {
      // one-off initialization
      m_selectedEvalMetrics = new ArrayList<String>();
      for (int i = 0; i < metricInstances.numAttributes(); i++) {
        m_selectedEvalMetrics.add(metricInstances.attribute(i).name());
      }
    }

    for (int i = 0; i < selectedMetrics.length; i++) {
      if (m_selectedEvalMetrics.contains(metricInstances.attribute(i).name())) {
        selectedMetrics[i] = true;
      }
    }

    try {
      evalConfigurer.setInstances(metricInstances);
      evalConfigurer.setSelectedAttributes(selectedMetrics);
    } catch (Exception ex) {
      ex.printStackTrace();
      return;
    }

    setLayout(new BorderLayout());
    JPanel holder = new JPanel();
    holder.setLayout(new BorderLayout());
    holder.add(evalConfigurer, BorderLayout.CENTER);
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    holder.add(butHolder, BorderLayout.SOUTH);
    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] selected = evalConfigurer.getSelectedAttributes();
        m_selectedEvalMetrics.clear();
        for (int i = 0; i < selected.length; i++) {
          m_selectedEvalMetrics.add(metricInstances.attribute(selected[i])
              .name());
        }
        dispose();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    getContentPane().add(holder, BorderLayout.CENTER);
    pack();
  }
}
