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
 *    GraphViewerInteractiveView.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
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
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Drawable;
import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.visualize.plugins.PrefuseGraph;
import weka.gui.visualize.plugins.PrefuseTree;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.PrefuseGraphViewer;

/**
 * Interactive viewer for the PrefuseGraphViewer step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class PrefuseGraphViewerInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = 2109423349272114409L;

  /** Holds a list of results */
  protected ResultHistoryPanel m_history;

  /** Button for clearing the results */
  protected JButton m_clearButton = new JButton("Clear results");

  /** Split pane for separating the results list from the visualization */
  protected JSplitPane m_splitPane;

  /** Visualization component for trees */
  protected PrefuseTree m_treeVisualizer = new PrefuseTree();

  /** Visualization generator for graphs */
  protected PrefuseGraph m_graphVisualizer = new PrefuseGraph();

  /** Will hold the interactive graph */
  protected JComponent m_prefuseHolder;

  /** Holder panel for layout purposes */
  protected JPanel m_holderPanel = new JPanel(new BorderLayout());

  /**
   * Get the name of this viewr
   *
   * @return the name of this viewer
   */
  @Override
  public String getViewerName() {
    return "Graph Viewer";
  }

  /**
   * Initializes the viewer
   *
   * @throws WekaException if a problem occurs
   */
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
            Object data = m_history.getNamedObject(name);
            if (data instanceof Data) {
              String grphString = ((Data) data).getPrimaryPayload();
              Integer grphType =
                ((Data) data)
                  .getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE);
              if (m_prefuseHolder != null) {
                m_holderPanel.remove(m_prefuseHolder);
              }
              try {
                if (grphType == Drawable.TREE) {
                  m_prefuseHolder = m_treeVisualizer.getDisplay(grphString);
                  m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
                  m_splitPane.revalidate();
                } else if (grphType == Drawable.BayesNet) {
                  m_prefuseHolder = m_graphVisualizer.getDisplay(grphString);

                  m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
                  m_splitPane.revalidate();
                }
              } catch (Exception ex) {
                getMainKFPerspective().showErrorDialog(ex);
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
                    String grphString = ((Data) data).getPrimaryPayload();
                    Integer grphType =
                      ((Data) data)
                        .getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE);
                    if (m_prefuseHolder != null) {
                      m_holderPanel.remove(m_prefuseHolder);
                    }
                    try {
                      if (grphType == Drawable.TREE) {
                        m_prefuseHolder =
                          m_treeVisualizer.getDisplay(grphString);
                        m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
                        m_splitPane.revalidate();
                      } else if (grphType == Drawable.BayesNet) {
                        m_prefuseHolder =
                          m_graphVisualizer.getDisplay(grphString);

                        m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
                        m_splitPane.revalidate();
                      }
                    } catch (Exception ex) {
                      getMainKFPerspective().showErrorDialog(ex);
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
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_history, m_holderPanel);
    // m_splitPane.setLeftComponent(m_history);
    add(m_splitPane, BorderLayout.CENTER);
    m_holderPanel.setPreferredSize(new Dimension(800, 600));

    boolean first = true;
    for (Data d : ((PrefuseGraphViewer) getStep()).getDatasets()) {
      String title = d.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE);
      m_history.addResult(title, new StringBuffer());
      m_history.addObject(title, d);
      if (first) {
        String grphString = d.getPrimaryPayload();
        Integer grphType =
          d.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE);
        try {
          if (grphType == Drawable.TREE) {
            m_prefuseHolder = m_treeVisualizer.getDisplay(grphString);
            // m_splitPane.setRightComponent(m_treeVisualizer);
            m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
          } else if (grphType == Drawable.BayesNet) {
            m_prefuseHolder = m_graphVisualizer.getDisplay(grphString);
            m_holderPanel.add(m_prefuseHolder, BorderLayout.CENTER);
          }
        } catch (Exception ex) {
          getMainKFPerspective().showErrorDialog(ex);
        }
        m_splitPane.revalidate();
        first = false;
      }
    }

    if (m_history.getList().getModel().getSize() > 0) {
      m_history.getList().setSelectedIndex(0);
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((PrefuseGraphViewer) getStep()).getDatasets().clear();
        if (m_treeVisualizer != null || m_graphVisualizer != null) {
          m_splitPane.remove(m_holderPanel);
          // invalidate();
          revalidate();
        }
      }
    });
  }
}
