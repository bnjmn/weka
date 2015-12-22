package weka.gui.knowledgeflow.steps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.knowledgeflow.steps.BoundaryPlotter;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class BoundaryPlotterInteractiveView extends BaseInteractiveViewer
  implements BoundaryPlotter.RenderingUpdateListener {

  private static final long serialVersionUID = 5567187861739468636L;
  protected JButton m_clearButton = new JButton("Clear results");

  protected ResultHistoryPanel m_history;

  /** Panel for displaying the image */
  protected ImageViewerInteractiveView.ImageDisplayer m_plotter;

  @Override
  public String getViewerName() {
    return "Boundary Visualizer";
  }

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

  @Override
  public void currentPlotRowCompleted(int row) {
    m_plotter.repaint();
  }

  @Override
  public void closePressed() {
    ((BoundaryPlotter) getStep()).removeRenderingListener(this);
  }
}
