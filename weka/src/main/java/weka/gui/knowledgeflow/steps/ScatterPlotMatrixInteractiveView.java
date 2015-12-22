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
 *    ScatterPlotMatrixInteractiveView.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.knowledgeflow.ScatterPlotMatrixPerspective;
import weka.gui.visualize.MatrixPanel;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.ScatterPlotMatrix;

/**
 * Interactive viewer for the ScatterPlotMatrix step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ScatterPlotMatrixInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = 275603100387301133L;

  protected ResultHistoryPanel m_history;
  protected MatrixPanel m_matrixPanel = new MatrixPanel();

  protected JButton m_clearButton = new JButton("Clear results");

  protected JSplitPane m_splitPane;

  @Override
  public String getViewerName() {
    return "Scatter Plot Matrix";
  }

  @Override
  public void init() throws WekaException {
    addButton(m_clearButton);

    m_history = new ResultHistoryPanel(null);
    m_history.setBorder(BorderFactory.createTitledBorder("Result list"));
    m_history.setHandleRightClicks(false);

    m_history.getList().addMouseListener(
      new ResultHistoryPanel.RMouseAdapter() {
        private static final long serialVersionUID = -5174882230278923704L;

        @Override
        public void mouseClicked(MouseEvent e) {
          int index = m_history.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_history.getNameAtIndex(index);
            // doPopup(name);
            Object insts = m_history.getNamedObject(name);
            if (insts instanceof Instances) {
              try {

                m_matrixPanel.setInstances((Instances) insts);
                m_matrixPanel.repaint();
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
                  Object insts = m_history.getNamedObject(name);
                  if (insts != null && insts instanceof Instances) {
                    try {
                      m_matrixPanel.setInstances((Instances) insts);
                      m_matrixPanel.repaint();
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

    // JScrollPane scatterScroller = new JScrollPane( m_matrixPanel );
    m_matrixPanel.setPreferredSize(new Dimension(800, 600));
    m_history.setMinimumSize(new Dimension(150, 600));
    m_splitPane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_history, m_matrixPanel);

    add(m_splitPane, BorderLayout.CENTER);
    boolean first = true;
    for (Data d : ((ScatterPlotMatrix) getStep()).getDatasets()) {
      String title =
        d.getPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE).toString();
      m_history.addResult(title, new StringBuffer());
      Instances instances = d.getPrimaryPayload();
      m_history.addObject(title, instances);
      if (first) {
        m_matrixPanel.setInstances(instances);
        m_matrixPanel.repaint();
        first = false;
      }
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((ScatterPlotMatrix) getStep()).getDatasets().clear();
        m_splitPane.remove(m_matrixPanel);
      }
    });
    applySettings(getSettings());
  }

  @Override
  public void applySettings(Settings settings) {
    int pointSize =
      settings.getSetting(ScatterPlotMatrixPerspective.ScatterDefaults.ID,
        ScatterPlotMatrixPerspective.ScatterDefaults.POINT_SIZE_KEY,
        ScatterPlotMatrixPerspective.ScatterDefaults.POINT_SIZE,
        Environment.getSystemWide());
    int plotSize =
      settings.getSetting(ScatterPlotMatrixPerspective.ScatterDefaults.ID,
        ScatterPlotMatrixPerspective.ScatterDefaults.PLOT_SIZE_KEY,
        ScatterPlotMatrixPerspective.ScatterDefaults.PLOT_SIZE,
        Environment.getSystemWide());
    m_matrixPanel.setPointSize(pointSize);
    m_matrixPanel.setPlotSize(plotSize);
    m_matrixPanel.updatePanel();
  }

  @Override
  public Defaults getDefaultSettings() {
    return new ScatterPlotMatrixPerspective().getDefaultSettings();
  }
}
