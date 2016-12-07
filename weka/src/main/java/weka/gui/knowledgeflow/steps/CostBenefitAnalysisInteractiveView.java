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

/*`
 *    CostBenefitAnalysisInteractiveView.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.WekaException;
import weka.gui.CostBenefitAnalysisPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.visualize.PlotData2D;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.CostBenefitAnalysis;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive view for the CostBenefitAnalysis step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class CostBenefitAnalysisInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = 4624182171551712860L;

  /** Holds result entries */
  protected ResultHistoryPanel m_history;

  /** Button for clearing all results */
  protected JButton m_clearButton = new JButton("Clear results");

  /** The actual cost benefit panel */
  protected CostBenefitAnalysisPanel m_cbPanel = new CostBenefitAnalysisPanel();

  /** JSplit pane for separating the result list from the visualization */
  protected JSplitPane m_splitPane;

  /**
   * Get the name of this viewer
   *
   * @return the name of this viewer
   */
  @Override
  public String getViewerName() {
    return "Cost-benefit Analysis";
  }

  /**
   * Initialize and layout the viewer
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void init() throws WekaException {
    addButton(m_clearButton);

    m_history = new ResultHistoryPanel(null);
    m_history.setBorder(BorderFactory.createTitledBorder("Result list"));
    m_history.setHandleRightClicks(false);
    m_history.setDeleteListener(new ResultHistoryPanel.RDeleteListener() {
      @Override public void entryDeleted(String name, int index) {
        ((CostBenefitAnalysis)getStep()).getDatasets().remove(index);
      }

      @Override public void entriesDeleted(java.util.List<String> names,
        java.util.List<Integer> indexes) {
        List<Data> ds = ((CostBenefitAnalysis) getStep()).getDatasets();
        List<Data> toRemove = new ArrayList<Data>();
        for (int i : indexes) {
          toRemove.add(ds.get(i));
        }

        ds.removeAll(toRemove);
      }
    });

    m_history.getList().addMouseListener(
      new ResultHistoryPanel.RMouseAdapter() {
        private static final long serialVersionUID = -5174882230278923704L;

        @Override
        public void mouseClicked(MouseEvent e) {
          int index = m_history.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_history.getNameAtIndex(index);
            // doPopup(name);
            Object data = m_history.getNamedObject(name);
            if (data instanceof Data) {
              PlotData2D threshData = ((Data) data).getPrimaryPayload();
              Attribute classAtt =
                ((Data) data)
                  .getPayloadElement(StepManager.CON_AUX_DATA_CLASS_ATTRIBUTE);
              try {
                m_cbPanel.setDataSet(threshData, classAtt);
                m_cbPanel.repaint();
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          }
        }
      });

    m_history.getList().getSelectionModel()
      .addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            ListSelectionModel lm = (ListSelectionModel) e.getSource();
            for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
              if (lm.isSelectedIndex(i)) {
                // m_AttSummaryPanel.setAttribute(i);
                if (i != -1) {
                  String name = m_history.getNameAtIndex(i);
                  Object data = m_history.getNamedObject(name);
                  if (data != null && data instanceof Data) {
                    PlotData2D threshData = ((Data) data).getPrimaryPayload();
                    Attribute classAtt =
                      ((Data) data)
                        .getPayloadElement(StepManager.CON_AUX_DATA_CLASS_ATTRIBUTE);
                    try {
                      m_cbPanel.setDataSet(threshData, classAtt);
                      m_cbPanel.repaint();
                    } catch (Exception ex) {
                      ex.printStackTrace();
                    }
                  }
                }
                break;
              }
            }
          }
        }
      });

    m_splitPane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_history, m_cbPanel);
    add(m_splitPane, BorderLayout.CENTER);
    m_cbPanel.setPreferredSize(new Dimension(1000, 600));

    boolean first = true;
    for (Data d : ((CostBenefitAnalysis) getStep()).getDatasets()) {
      PlotData2D threshData = d.getPrimaryPayload();
      Attribute classAtt =
        d.getPayloadElement(StepManager.CON_AUX_DATA_CLASS_ATTRIBUTE);
      String title = threshData.getPlotName();
      m_history.addResult(title, new StringBuffer());
      m_history.addObject(title, d);
      if (first) {
        try {
          m_cbPanel.setDataSet(threshData, classAtt);
          m_cbPanel.repaint();
          first = false;
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((CostBenefitAnalysis) getStep()).getDatasets().clear();
        m_splitPane.remove(m_cbPanel);
        m_splitPane.revalidate();
      }
    });
  }
}
