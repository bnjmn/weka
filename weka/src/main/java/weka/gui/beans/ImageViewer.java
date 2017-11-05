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
 *    ImageViewer.java
 *    Copyright (C) 2002-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

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
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import weka.core.Environment;
import weka.core.Utils;
import weka.gui.Logger;
import weka.gui.ResultHistoryPanel;

/**
 * A KF component that can accept imageEvent connections in order to display
 * static images in a popup window
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Visualization", toolTipText = "Display static images")
public class ImageViewer extends JPanel implements ImageListener, BeanCommon,
  Visible, Serializable, UserRequestAcceptor {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 7976930810628389750L;

  /** Panel for displaying the image */
  protected ImageDisplayer m_plotter;

  /** Keeps a history of the images received */
  protected ResultHistoryPanel m_history;

  /** Frame for displaying the images in */
  private transient JFrame m_resultsFrame = null;

  /**
   * Default visual for data sources
   */
  protected BeanVisual m_visual = new BeanVisual("ImageVisualizer",
    BeanVisual.ICON_PATH + "StripChart.gif", BeanVisual.ICON_PATH
      + "StripChart_animated.gif");

  /**
   * The log for this bean
   */
  protected transient weka.gui.Logger m_logger = null;

  /**
   * The environment variables.
   */
  protected transient Environment m_env;

  /**
   * Constructs a new ImageViewer
   */
  public ImageViewer() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();
    m_plotter = new ImageDisplayer();
    // m_plotter.setBorder(BorderFactory.createTitledBorder("Image"));
    m_plotter.setMinimumSize(new Dimension(810, 610));
    m_plotter.setPreferredSize(new Dimension(810, 610));
    setUpResultHistory();
  }

  /**
   * Global info for this bean
   * 
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Display static images";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "StripChart.gif",
      BeanVisual.ICON_PATH + "StripChart_animated.gif");
    m_visual.setText("ImageViewer");
  }

  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isBusy() {
    return false;
  }

  @Override
  public void setLog(Logger logger) {
    m_logger = logger;
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {

    return connectionAllowed(esd.getName());
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    return true;
  }

  @Override
  public void connectionNotification(String eventName, Object source) {
  }

  @Override
  public void disconnectionNotification(String eventName, Object source) {
  }

  @Override
  public synchronized void acceptImage(ImageEvent imageE) {

    BufferedImage image = imageE.getImage();
    String name = (new SimpleDateFormat("HH:mm:ss:SS")).format(new Date());
    name =
      (imageE.getImageName() == null || imageE.getImageName().length() == 0 ? "Image at "
        : imageE.getImageName() + " ")
        + name;

    m_history.addResult(name, new StringBuffer());
    m_history.addObject(name, image);
    m_plotter.setImage(image);
    m_plotter.repaint();
  }

  /**
   * Popup the window to display the images in
   */
  protected void showResults() {
    if (m_resultsFrame == null) {
      if (m_history == null) {
        setUpResultHistory();
      }
      m_resultsFrame = Utils.getWekaJFrame("Image Viewer", m_visual);
      m_resultsFrame.getContentPane().setLayout(new BorderLayout());
      m_resultsFrame.getContentPane().add(new MainPanel(m_history, m_plotter),
        BorderLayout.CENTER);
      m_resultsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          m_resultsFrame.dispose();
          m_resultsFrame = null;
        }
      });
      m_resultsFrame.pack();
      m_resultsFrame.setLocationRelativeTo(SwingUtilities.getWindowAncestor(m_visual));
      m_resultsFrame.setVisible(true);
    } else {
      m_resultsFrame.toFront();
    }
  }

  private void setUpResultHistory() {
    if (m_history == null) {
      m_history = new ResultHistoryPanel(null);
    }
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
  }

  /**
   * Small inner class for laying out the main parts of the popup display
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  private static class MainPanel extends JPanel {

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
        new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH
          + "zoom_in.png")));

      zoomInB.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
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
        new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH
          + "zoom_out.png")));
      zoomOutB.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
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
      topP.add(holder, BorderLayout.CENTER);
      topP.add(p, BorderLayout.WEST);

      add(topP, BorderLayout.CENTER);
    }
  }

  /**
   * Inner class for displaying a BufferedImage.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  private static class ImageDisplayer extends JPanel {

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

  @Override
  public Enumeration<String> enumerateRequests() {
    Vector<String> newVector = new Vector<String>(0);
    newVector.addElement("Show results");

    return newVector.elements();
  }

  @Override
  public void performRequest(String request) {
    if (request.compareTo("Show results") == 0) {
      showResults();
    } else {
      throw new IllegalArgumentException(request
        + " not supported (ImageViewer)");
    }
  }
}
