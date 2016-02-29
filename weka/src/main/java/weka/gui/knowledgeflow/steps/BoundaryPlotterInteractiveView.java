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
 *    BoundaryPlotterInteractiveView.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.knowledgeflow.steps.BoundaryPlotter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Interactive viewer component for the boundary plotter step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class BoundaryPlotterInteractiveView extends BaseInteractiveViewer
  implements BoundaryPlotter.RenderingUpdateListener {

  private static final long serialVersionUID = 5567187861739468636L;
  protected JButton m_clearButton = new JButton("Clear results");

  /** Holds a list of plots */
  protected ResultHistoryPanel m_history;

  /** Panel for displaying the image */
  protected ImageViewerInteractiveView.ImageDisplayer m_plotter;

  /**
   * Get the name of this viewer
   *
   * @return the name of this viewer
   */
  @Override
  public String getViewerName() {
    return "Boundary Visualizer";
  }

  /**
   * Initialize/layout the viewer
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void init() throws WekaException {
    addButton(m_clearButton);

    m_plotter = new ImageViewerInteractiveView.ImageDisplayer();
    m_plotter.setMinimumSize(new Dimension(810, 610));
    m_plotter.setPreferredSize(new Dimension(810, 610));

    m_history = new ResultHistoryPanel(null);
    m_history.setBorder(BorderFactory.createTitledBorder("Image list"));
    m_history.setHandleRightClicks(false);
    m_history.getList().addMouseListener(
      new ResultHistoryPanel.RMouseAdapter() {
        /** for serialization */
        private static final long serialVersionUID = -4984130887963944249L;

        @Override
        public void mouseClicked(MouseEvent e) {
          int index = m_history.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_history.getNameAtIndex(index);
            // doPopup(name);
            Object pic = m_history.getNamedObject(name);
            if (pic instanceof BufferedImage) {
              m_plotter.setImage((BufferedImage) pic);
              m_plotter.repaint();
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
                  Object pic = m_history.getNamedObject(name);
                  if (pic != null && pic instanceof BufferedImage) {
                    m_plotter.setImage((BufferedImage) pic);
                    m_plotter.repaint();
                  }
                }
                break;
              }
            }
          }
        }
      });

    ImageViewerInteractiveView.MainPanel mainPanel =
      new ImageViewerInteractiveView.MainPanel(m_history, m_plotter);
    add(mainPanel, BorderLayout.CENTER);

    boolean first = true;

    Map<String, BufferedImage> images =
      ((BoundaryPlotter) getStep()).getImages();
    if (images != null) {
      for (Map.Entry<String, BufferedImage> e : images.entrySet()) {
        m_history.addResult(e.getKey(), new StringBuffer());
        m_history.addObject(e.getKey(), e.getValue());
        if (first) {
          m_plotter.setImage(e.getValue());
          m_plotter.repaint();
          first = false;
        }
      }
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();

        ((BoundaryPlotter) getStep()).getImages().clear();
        m_plotter.setImage(null);
        m_plotter.repaint();
      }
    });

    ((BoundaryPlotter) getStep()).setRenderingListener(this);
  }

  /**
   * Called when there is an update to rendering of the current image
   */
  @Override
  public void renderingImageUpdate() {
    m_plotter.repaint();
  }

  @Override
  public void newPlotStarted(String description) {
    BufferedImage currentImage =
      ((BoundaryPlotter) getStep()).getCurrentImage();
    if (currentImage != null) {
      m_history.addResult(description, new StringBuffer());
      m_history.addObject(description, currentImage);
      m_history.setSelectedListValue(description);
      m_plotter.setImage(currentImage);
      m_plotter.repaint();
    }
  }

  /**
   * Called when a row of the image being plotted has been completed
   *
   * @param row the index of the row that was completed
   */
  @Override
  public void currentPlotRowCompleted(int row) {
    m_plotter.repaint();
  }

  /**
   * Called when the viewer's window is closed
   */
  @Override
  public void closePressed() {
    ((BoundaryPlotter) getStep()).removeRenderingListener(this);
  }
}
