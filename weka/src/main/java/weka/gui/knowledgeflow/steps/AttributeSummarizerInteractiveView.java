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
 *    AttributeSummarizerInteractiveView.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Defaults;
import weka.core.Instances;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.AttributeSummaryPerspective;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.AttributeSummarizer;

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
 * Interactive viewer for the AttributeSummarizer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class AttributeSummarizerInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = -8080574605631027263L;

  /** Holds results */
  protected ResultHistoryPanel m_history;

  /** Holds the actual visualization */
  protected AttributeSummaryPerspective m_summarizer =
    new AttributeSummaryPerspective();

  /** Button for clearing all results */
  protected JButton m_clearButton = new JButton("Clear results");

  /** Split pane to separate result list from visualization */
  protected JSplitPane m_splitPane;

  /** The instances being visualized */
  protected Instances m_currentInstances;

  /**
   * The name of this viewer
   *
   * @return
   */
  @Override
  public String getViewerName() {
    return "Attribute Summarizer";
  }

  /**
   * Initialize the viewer - layout widgets etc.
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
      @Override
      public void entryDeleted(String name, int index) {
        ((AttributeSummarizer) getStep()).getDatasets().remove(index);
      }

      @Override
      public void entriesDeleted(java.util.List<String> names,
        java.util.List<Integer> indexes) {
        List<Data> ds = ((AttributeSummarizer) getStep()).getDatasets();
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
            Object inst = m_history.getNamedObject(name);
            if (inst instanceof Instances) {
              m_currentInstances = (Instances) inst;
              m_summarizer.setInstances((Instances) inst, getSettings());
              m_summarizer.repaint();
              m_parent.revalidate();
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
                  Object inst = m_history.getNamedObject(name);
                  if (inst != null && inst instanceof Instances) {
                    m_currentInstances = (Instances) inst;
                    m_summarizer.setInstances((Instances) inst, getSettings());
                    m_summarizer.repaint();
                    m_parent.revalidate();
                  }
                }
                break;
              }
            }
          }
        }
      });

    m_summarizer.setPreferredSize(new Dimension(800, 600));
    m_splitPane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_history, m_summarizer);
    add(m_splitPane, BorderLayout.CENTER);
    boolean first = true;
    for (Data d : ((AttributeSummarizer) getStep()).getDatasets()) {
      String title =
        d.getPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE).toString();
      m_history.addResult(title, new StringBuffer());
      Instances instances = d.getPrimaryPayload();
      m_history.addObject(title, instances);
      if (first) {
        m_summarizer.setInstances(instances, getSettings());
        m_summarizer.repaint();
        first = false;
      }
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((AttributeSummarizer) getStep()).getDatasets().clear();
        m_splitPane.remove(m_summarizer);
      }
    });
  }

  /**
   * Get the default settings for this viewer
   *
   * @return the default settings for this viewer
   */
  @Override
  public Defaults getDefaultSettings() {
    return new AttributeSummaryPerspective().getDefaultSettings();
  }

  /**
   * Apply user-changed settings
   *
   * @param settings the settings object that might (or might not) have been
   */
  @Override
  public void applySettings(Settings settings) {
    m_summarizer.setInstances(m_currentInstances, getSettings());
    m_summarizer.repaint();
    m_parent.revalidate();
  }
}
