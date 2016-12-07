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
 *    ImageViewerInteractiveView.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.ResultHistoryPanel;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.steps.ImageViewer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Interactive viewer for the ImageViewer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ImageViewerInteractiveView extends BaseInteractiveViewer {

  private static final long serialVersionUID = -6652203133445653870L;

  /** Button for clearing the results */
  protected JButton m_clearButton = new JButton("Clear results");

  /** A panel for holding a list of results */
  protected ResultHistoryPanel m_history;

  /** Panel for displaying the image */
  protected ImageDisplayer m_plotter;

  /**
   * Get the name of the viewer
   *
   * @return the name of the viewer
   */
  @Override
  public String getViewerName() {
    return "Image Viewer";
  }

  /**
   * Initialize the viewer and the layout
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void init() throws WekaException {
    addButton(m_clearButton);

    m_plotter = new ImageDisplayer();
    m_plotter.setMinimumSize(new Dimension(810, 610));
    m_plotter.setPreferredSize( new Dimension( 810, 610 ) );

    m_history = new ResultHistoryPanel(null);
    m_history.setBorder(BorderFactory.createTitledBorder("Image list"));
    m_history.setHandleRightClicks( false );
    m_history.setDeleteListener(new ResultHistoryPanel.RDeleteListener() {
      @Override public void entryDeleted(String name, int index) {
        ((ImageViewer)getStep()).getImages().remove(name);
      }

      @Override
      public void entriesDeleted(List<String> names, List<Integer> indexes) {
        for (String name : names) {
          ((ImageViewer) getStep()).getImages().remove(name);
        }
      }
    });
    m_history.getList().addMouseListener( new ResultHistoryPanel.RMouseAdapter() {
        /** for serialization */
        private static final long serialVersionUID = -4984130887963944249L;

        @Override public void mouseClicked( MouseEvent e ) {
          int index = m_history.getList().locationToIndex( e.getPoint() );
          if ( index != -1 ) {
            String name = m_history.getNameAtIndex( index );
            // doPopup(name);
            Object pic = m_history.getNamedObject( name );
            if ( pic instanceof BufferedImage ) {
              m_plotter.setImage( (BufferedImage) pic );
              m_plotter.repaint();
            }
          }
        }
      } );

    m_history.getList().getSelectionModel()
      .addListSelectionListener( new ListSelectionListener() {
        @Override public void valueChanged( ListSelectionEvent e ) {
          if ( !e.getValueIsAdjusting() ) {
            ListSelectionModel lm = (ListSelectionModel) e.getSource();
            for ( int i = e.getFirstIndex(); i <= e.getLastIndex(); i++ ) {
              if ( lm.isSelectedIndex( i ) ) {
                // m_AttSummaryPanel.setAttribute(i);
                if ( i != -1 ) {
                  String name = m_history.getNameAtIndex( i );
                  Object pic = m_history.getNamedObject( name );
                  if ( pic != null && pic instanceof BufferedImage ) {
                    m_plotter.setImage( (BufferedImage) pic );
                    m_plotter.repaint();
                  }
                }
                break;
              }
            }
          }
        }
      } );

    MainPanel mainPanel = new MainPanel(m_history, m_plotter);
    add(mainPanel, BorderLayout.CENTER);

    boolean first = true;
    for (Map.Entry<String, BufferedImage> e : ((ImageViewer) getStep())
      .getImages().entrySet()) {
      m_history.addResult(e.getKey(), new StringBuffer());
      m_history.addObject(e.getKey(), e.getValue());
      if (first) {
        m_plotter.setImage(e.getValue());
        m_plotter.repaint();
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
        ((ImageViewer) getStep()).getImages().clear();
        m_plotter.setImage( null );
        m_plotter.repaint();
      }
    });
  }

  /**
   * Small inner class for laying out the main parts of the popup display
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class MainPanel extends JPanel {

    private static Image loadImage(String path) {
      Image pic = null;
      java.net.URL imageURL =
        ImageViewer.class.getClassLoader().getResource(path);

      // end modifications
      if (imageURL == null) {
      } else {
        pic = Toolkit.getDefaultToolkit().getImage(imageURL);
      }
      return pic;
    }

    /**
     * For serialization
     */
    private static final long serialVersionUID = 5648976848887609072L;

    /**
     * Constructor
     *
     * @param p a {@code ResultHistoryPanel} to add
     * @param id the {@code ImageDisplayer} to add
     */
    public MainPanel(ResultHistoryPanel p, final ImageDisplayer id) {
      super();
      setLayout(new BorderLayout());

      JPanel topP = new JPanel();
      topP.setLayout(new BorderLayout());

      JPanel holder = new JPanel();
      holder.setLayout(new BorderLayout());
      holder.setBorder(BorderFactory.createTitledBorder("Image"));
      JToolBar tools = new JToolBar();
      tools.setOrientation(JToolBar.HORIZONTAL);
      JButton zoomInB =
        new JButton(new ImageIcon(loadImage(StepVisual.BASE_ICON_PATH
          + "zoom_in.png")));

      zoomInB.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
          int z = id.getZoom();
          z += 25;
          if (z >= 200) {
            z = 200;
          }

          id.setZoom(z);
          id.repaint();
        }
      });

      JButton zoomOutB =
        new JButton(new ImageIcon(loadImage(StepVisual.BASE_ICON_PATH
          + "zoom_out.png")));
      zoomOutB.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
          int z = id.getZoom();
          z -= 25;
          if (z <= 50) {
            z = 50;
          }

          id.setZoom(z);
          id.repaint();
        }
      });

      tools.add(zoomInB);
      tools.add(zoomOutB);
      holder.add(tools, BorderLayout.NORTH);

      JScrollPane js = new JScrollPane(id);
      holder.add(js, BorderLayout.CENTER);
      JSplitPane p2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p, holder);

      topP.add(p2, BorderLayout.CENTER);
      // topP.add(p, BorderLayout.WEST);

      add(topP, BorderLayout.CENTER);
    }
  }

  /**
   * Inner class for displaying a BufferedImage.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision: 10883 $
   */
  protected static class ImageDisplayer extends JPanel {

    /** For serialization */
    private static final long serialVersionUID = 4161957589912537357L;

    /** The image to display */
    private BufferedImage m_image;

    private int m_imageZoom = 100;

    /**
     * Set the image to display
     *
     * @param image the image to display
     */
    public void setImage(BufferedImage image) {
      m_image = image;
    }

    public void setZoom(int zoom) {
      m_imageZoom = zoom;
    }

    public int getZoom() {
      return m_imageZoom;
    }

    /**
     * Render the image
     *
     * @param g the graphics context
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (m_image != null) {
        double lz = m_imageZoom / 100.0;
        ((Graphics2D) g).scale(lz, lz);
        int plotWidth = m_image.getWidth();
        int plotHeight = m_image.getHeight();

        int ourWidth = getWidth();
        int ourHeight = getHeight();

        // center if plot is smaller than us
        int x = 0, y = 0;
        if (plotWidth < ourWidth) {
          x = (ourWidth - plotWidth) / 2;
        }
        if (plotHeight < ourHeight) {
          y = (ourHeight - plotHeight) / 2;
        }

        g.drawImage(m_image, x, y, this);
        setPreferredSize(new Dimension(plotWidth, plotHeight));
        revalidate();
      }
    }
  }
}
