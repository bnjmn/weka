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
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Drawable;
import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.graphvisualizer.BIFFormatException;
import weka.gui.graphvisualizer.GraphVisualizer;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.GraphViewer;

/**
 * Interactive viewer for the GraphViewer step.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class GraphViewerInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = 2109423349272114409L;
  protected ResultHistoryPanel m_history;
  protected JButton m_clearButton = new JButton("Clear results");

  protected JSplitPane m_splitPane;

  protected TreeVisualizer m_treeVisualizer;
  protected GraphVisualizer m_graphVisualizer;

  JPanel m_holderPanel = new JPanel(new BorderLayout());

  @Override
  public String getViewerName() {
    return "Graph Viewer";
  }

  @Override
  public void init() throws WekaException {
    addButton(m_clearButton);

    m_history = new ResultHistoryPanel(null);
    m_history.setBorder( BorderFactory.createTitledBorder( "Result list" ) );
    m_history.setHandleRightClicks( false );

    m_history.getList().addMouseListener( new ResultHistoryPanel.RMouseAdapter() {
        private static final long serialVersionUID = -5174882230278923704L;

        @Override public void mouseClicked( MouseEvent e ) {
          int index = m_history.getList().locationToIndex( e.getPoint() );
          if ( index != -1 ) {
            String name = m_history.getNameAtIndex( index );
            // doPopup(name);
            Object data = m_history.getNamedObject( name );
            if ( data instanceof Data ) {
              String grphString = ( (Data) data ).getPrimaryPayload();
              Integer grphType = ( (Data) data ).getPayloadElement( StepManager.CON_AUX_DATA_GRAPH_TYPE );
              if ( m_treeVisualizer != null || m_graphVisualizer != null ) {
                m_holderPanel.remove( m_treeVisualizer != null ? m_treeVisualizer : m_graphVisualizer );
              }
              if ( grphType == Drawable.TREE ) {
                m_treeVisualizer = new TreeVisualizer( null, grphString, new PlaceNode2() );
                m_holderPanel.add( m_treeVisualizer, BorderLayout.CENTER );
                m_splitPane.revalidate();
              } else if ( grphType == Drawable.BayesNet ) {
                m_graphVisualizer = new GraphVisualizer();
                try {
                  m_graphVisualizer.readBIF( grphString );
                } catch ( BIFFormatException ex ) {
                  ex.printStackTrace();
                }
                m_graphVisualizer.layoutGraph();
                m_holderPanel.add( m_graphVisualizer, BorderLayout.CENTER );
                m_splitPane.revalidate();
              }
            }
          }
        }
      } );

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
                    if (m_treeVisualizer != null || m_graphVisualizer != null) {
                      m_holderPanel
                        .remove(m_treeVisualizer != null ? m_treeVisualizer
                          : m_graphVisualizer);
                    }
                    if (grphType == Drawable.TREE) {
                      m_treeVisualizer =
                        new TreeVisualizer(null, grphString, new PlaceNode2());
                      m_holderPanel.add(m_treeVisualizer, BorderLayout.CENTER);
                      m_splitPane.revalidate();
                    } else if (grphType == Drawable.BayesNet) {
                      m_graphVisualizer = new GraphVisualizer();
                      try {
                        m_graphVisualizer.readBIF(grphString);
                      } catch (BIFFormatException ex) {
                        ex.printStackTrace();
                      }
                      m_graphVisualizer.layoutGraph();
                      m_holderPanel.add(m_graphVisualizer, BorderLayout.CENTER);
                      m_splitPane.revalidate();
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
    for (Data d : ((GraphViewer) getStep()).getDatasets()) {
      String title = d.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE);
      m_history.addResult(title, new StringBuffer());
      m_history.addObject(title, d);
      if (first) {
        String grphString = d.getPrimaryPayload();
        Integer grphType =
          d.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE);
        if (grphType == Drawable.TREE) {
          m_treeVisualizer =
            new TreeVisualizer(null, grphString, new PlaceNode2());
          // m_splitPane.setRightComponent(m_treeVisualizer);
          m_holderPanel.add(m_treeVisualizer, BorderLayout.CENTER);
        } else if (grphType == Drawable.BayesNet) {
          m_graphVisualizer = new GraphVisualizer();
          try {
            m_graphVisualizer.readBIF(grphString);
          } catch (BIFFormatException ex) {
            ex.printStackTrace();
          }
          m_graphVisualizer.layoutGraph();
          m_holderPanel.add(m_graphVisualizer, BorderLayout.CENTER);
        }
        m_splitPane.revalidate();
        first = false;
      }
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((GraphViewer) getStep()).getDatasets().clear();
        if (m_treeVisualizer != null || m_graphVisualizer != null) {
          m_splitPane.remove(m_holderPanel);
          // invalidate();
          revalidate();
        }
      }
    });
  }
}
