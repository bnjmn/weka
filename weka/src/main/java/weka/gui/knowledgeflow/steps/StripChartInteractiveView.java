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
 *    StripChartInteractiveView.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.Utils;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.gui.visualize.PrintableComponent;
import weka.knowledgeflow.steps.StripChart;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the actual strip chart view
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StripChartInteractiveView extends BaseInteractiveViewer implements
  StripChart.PlotNotificationListener {

  private static final long serialVersionUID = 7697752421621805402L;

  /** default colours for colouring lines */
  protected Color[] m_colorList = { Color.green, Color.red,
    new Color(6, 80, 255), Color.cyan, Color.pink, new Color(255, 0, 255),
    Color.orange, new Color(255, 0, 0), new Color(0, 255, 0), Color.white };

  /** the background color. */
  protected Color m_BackgroundColor = Color.BLACK;

  /** the color of the legend panel's border. */
  protected Color m_LegendPanelBorderColor = new Color(253, 255, 61);

  protected StripPlotter m_plotPanel;

  /** the scale. */
  protected final ScalePanel m_scalePanel = new ScalePanel();

  /**
   * The off screen image for rendering to.
   */
  protected transient Image m_osi = null;

  /**
   * Width and height of the off screen image.
   */
  protected int m_iheight;
  protected int m_iwidth;

  /**
   * Max value for the y axis.
   */
  protected double m_max = 1;

  /**
   * Min value for the y axis.
   */
  protected double m_min = 0;

  /**
   * Scale update requested.
   */
  protected boolean m_yScaleUpdate = false;
  protected double m_oldMax;
  protected double m_oldMin;

  /** data point count */
  protected int m_xCount = 0;

  /**
   * Shift the plot by this many pixels every time a point is plotted
   */
  private int m_refreshWidth = 1;

  /** Font to use on the plot */
  protected final Font m_labelFont = new Font("Monospaced", Font.PLAIN, 10);

  /** Font metrics for string placement calculations */
  protected FontMetrics m_labelMetrics;

  /** Holds the legend */
  protected final LegendPanel m_legendPanel = new LegendPanel();

  /** Holds the legend entries */
  protected List<String> m_legendText = new ArrayList<String>();

  /** Previous data point */
  private double[] m_previousY = new double[1];

  /**
   * Initialize the viewer
   */
  @Override
  public void init() {
    m_plotPanel = new StripPlotter();
    m_plotPanel.setBackground(m_BackgroundColor);
    m_scalePanel.setBackground(m_BackgroundColor);
    m_legendPanel.setBackground(m_BackgroundColor);
    m_xCount = 0;

    JPanel panel = new JPanel(new BorderLayout());
    new PrintableComponent(panel);
    add(panel, BorderLayout.CENTER);
    panel.add(m_legendPanel, BorderLayout.WEST);
    panel.add(m_plotPanel, BorderLayout.CENTER);
    panel.add(m_scalePanel, BorderLayout.EAST);
    m_legendPanel.setMinimumSize(new Dimension(100, getHeight()));
    m_legendPanel.setPreferredSize(new Dimension(100, getHeight()));
    m_scalePanel.setMinimumSize(new Dimension(30, getHeight()));
    m_scalePanel.setPreferredSize(new Dimension(30, getHeight()));

    // setPreferredSize(new Dimension(600, 150));

    m_parent.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        ((StripChart) getStep())
          .removePlotNotificationListener(StripChartInteractiveView.this);
      }
    });
    ((StripChart) getStep()).addPlotNotificationListener(this);
    applySettings(getSettings());
  }

  /**
   * Called when the close button is pressed
   */
  @Override
  public void closePressed() {
    ((StripChart) getStep())
      .removePlotNotificationListener(StripChartInteractiveView.this);
  }

  /**
   * Called by the KnowledgeFlow application once the enclosing JFrame is
   * visible
   */
  @Override
  public void nowVisible() {
    m_parent.setSize(600, 180);
    ((JFrame) m_parent).setResizable(false);
    m_parent.setAlwaysOnTop(true);
    m_parent.validate();

    int iwidth = m_plotPanel.getWidth();
    int iheight = m_plotPanel.getHeight();
    m_osi = m_plotPanel.createImage(iwidth, iheight);
    Graphics m = m_osi.getGraphics();
    m.setColor(m_BackgroundColor);
    m.fillRect(0, 0, iwidth, iheight);
    m_previousY[0] = -1;
    setRefreshWidth();
  }

  private int convertToPanelY(double yval) {
    int height = m_plotPanel.getHeight();
    double temp = (yval - m_min) / (m_max - m_min);
    temp = temp * height;
    temp = height - temp;
    return (int) temp;
  }

  /**
   * Get the name of this viewer
   *
   * @return the name of this viewer
   */
  @Override
  public String getViewerName() {
    return "Strip Chart";
  }

  /**
   * Set the entries for the legend
   *
   * @param legendEntries a list of legend entries
   * @param min initial minimum for the series being plotted
   * @param max initial maximum for the series being plotted
   */
  @Override
  public void setLegend(List<String> legendEntries, double min, double max) {
    m_legendText = legendEntries;
    m_max = max;
    m_min = min;
    m_xCount = 0;
    m_legendPanel.repaint();
  }

  /**
   * Pre-process a data point
   *
   * @param dataPoint the data point to process
   * @return the data point
   */
  protected double[] preProcessDataPoint(double[] dataPoint) {
    // check for out of scale values
    for (double element : dataPoint) {
      if (element < m_min) {
        m_oldMin = m_min;
        m_min = element;
        m_yScaleUpdate = true;
      }

      if (element > m_max) {
        m_oldMax = m_max;
        m_max = element;
        m_yScaleUpdate = true;
      }
    }

    if (m_yScaleUpdate) {
      m_scalePanel.repaint();
      m_yScaleUpdate = false;
    }

    // return dp;
    return dataPoint;
  }

  /**
   * Accept and process a data point
   *
   * @param dataPoint the data point to process
   */
  @Override
  public void acceptDataPoint(double[] dataPoint) {
    if (m_xCount % ((StripChart) getStep()).getRefreshFreq() != 0) {
      m_xCount++;
      return;
    }
    dataPoint = preProcessDataPoint(dataPoint);

    if (m_previousY[0] == -1) {
      int iw = m_plotPanel.getWidth();
      int ih = m_plotPanel.getHeight();
      m_osi = m_plotPanel.createImage(iw, ih);
      Graphics m = m_osi.getGraphics();
      m.setColor(m_BackgroundColor);
      m.fillRect(0, 0, iw, ih);
      m_previousY[0] = convertToPanelY(0);
      m_iheight = ih;
      m_iwidth = iw;
    }

    if (dataPoint.length != m_previousY.length) {
      m_previousY = new double[dataPoint.length];
      for (int i = 0; i < dataPoint.length; i++) {
        m_previousY[i] = convertToPanelY(0);
      }
    }

    Graphics osg = m_osi.getGraphics();
    Graphics g = m_plotPanel.getGraphics();

    osg.copyArea(m_refreshWidth, 0, m_iwidth - m_refreshWidth, m_iheight,
      -m_refreshWidth, 0);
    osg.setColor(m_BackgroundColor);
    osg.fillRect(m_iwidth - m_refreshWidth, 0, m_iwidth, m_iheight);

    // paint the old scale onto the plot if a scale update has occurred
    if (m_yScaleUpdate) {
      String maxVal = numToString(m_oldMax);
      String minVal = numToString(m_oldMin);
      String midVal = numToString((m_oldMax - m_oldMin) / 2.0);
      if (m_labelMetrics == null) {
        m_labelMetrics = g.getFontMetrics(m_labelFont);
      }
      osg.setFont(m_labelFont);
      int wmx = m_labelMetrics.stringWidth(maxVal);
      int wmn = m_labelMetrics.stringWidth(minVal);
      int wmd = m_labelMetrics.stringWidth(midVal);

      int hf = m_labelMetrics.getAscent();
      osg.setColor(m_colorList[m_colorList.length - 1]);
      osg.drawString(maxVal, m_iwidth - wmx, hf - 2);
      osg.drawString(midVal, m_iwidth - wmd, (m_iheight / 2) + (hf / 2));
      osg.drawString(minVal, m_iwidth - wmn, m_iheight - 1);
      m_yScaleUpdate = false;
    }

    double pos;
    for (int i = 0; i < dataPoint.length; i++) {
      if (Utils.isMissingValue(dataPoint[i])) {
        continue;
      }
      osg.setColor(m_colorList[(i % m_colorList.length)]);
      pos = convertToPanelY(dataPoint[i]);
      osg.drawLine(m_iwidth - m_refreshWidth, (int) m_previousY[i],
        m_iwidth - 1, (int) pos);
      m_previousY[i] = pos;
      if (m_xCount % ((StripChart) getStep()).getXLabelFreq() == 0) {
        // draw the actual y value onto the plot for this curve
        String val = numToString(dataPoint[i]);
        if (m_labelMetrics == null) {
          m_labelMetrics = g.getFontMetrics(m_labelFont);
        }
        int hf = m_labelMetrics.getAscent();
        if (pos - hf < 0) {
          pos += hf;
        }
        int w = m_labelMetrics.stringWidth(val);
        osg.setFont(m_labelFont);
        osg.drawString(val, m_iwidth - w, (int) pos);
      }
    }

    if (m_xCount % ((StripChart) getStep()).getXLabelFreq() == 0) {

      String xVal = "" + m_xCount;
      osg.setColor(m_colorList[m_colorList.length - 1]);
      int w = m_labelMetrics.stringWidth(xVal);
      osg.setFont(m_labelFont);
      osg.drawString(xVal, m_iwidth - w, m_iheight - 1);
    }
    g.drawImage(m_osi, 0, 0, m_plotPanel);
    m_xCount++;
  }

  private void setRefreshWidth() {
    m_refreshWidth = ((StripChart) getStep()).getRefreshWidth();
    if (m_labelMetrics == null) {
      getGraphics().setFont(m_labelFont);
      m_labelMetrics = getGraphics().getFontMetrics(m_labelFont);
    }

    int refWidth = m_labelMetrics.stringWidth("99000");
    // compute how often x label will be rendered
    int z =
      (((StripChart) getStep()).getXLabelFreq() / ((StripChart) getStep())
        .getRefreshFreq());
    if (z < 1) {
      z = 1;
    }

    if (z * m_refreshWidth < refWidth + 5) {
      m_refreshWidth *= (((refWidth + 5) / z) + 1);
    }
  }

  /**
   * Class providing a panel for the plot.
   */
  private class StripPlotter extends JPanel {

    /** for serialization. */
    private static final long serialVersionUID = -7056271598761675879L;

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (m_osi != null) {
        g.drawImage(m_osi, 0, 0, this);
      }
    }
  }

  /**
   * Class providing a panel for displaying the y axis.
   */
  private class ScalePanel extends JPanel {

    /** for serialization. */
    private static final long serialVersionUID = 6416998474984829434L;

    @Override
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      if (m_labelMetrics == null) {
        m_labelMetrics = gx.getFontMetrics(m_labelFont);
      }
      gx.setFont(m_labelFont);
      int hf = m_labelMetrics.getAscent();
      String temp = "" + m_max;
      gx.setColor(m_colorList[m_colorList.length - 1]);
      gx.drawString(temp, 1, hf - 2);
      temp = "" + (m_min + ((m_max - m_min) / 2.0));
      gx.drawString(temp, 1, (this.getHeight() / 2) + (hf / 2));
      temp = "" + m_min;
      gx.drawString(temp, 1, this.getHeight() - 1);
    }
  };

  /**
   * Class providing a panel for the legend.
   */
  protected class LegendPanel extends JPanel {

    /** for serialization. */
    private static final long serialVersionUID = 7713986576833797583L;

    @Override
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);

      if (m_labelMetrics == null) {
        m_labelMetrics = gx.getFontMetrics(m_labelFont);
      }
      int hf = m_labelMetrics.getAscent();
      int x = 10;
      int y = hf + 15;
      gx.setFont(m_labelFont);
      for (int i = 0; i < m_legendText.size(); i++) {
        String temp = m_legendText.get(i);
        gx.setColor(m_colorList[(i % m_colorList.length)]);
        gx.drawString(temp, x, y);
        y += hf;
      }
      StripChartInteractiveView.this.revalidate();
    }
  };

  private static String numToString(double num) {
    int precision = 1;
    int whole = (int) Math.abs(num);
    double decimal = Math.abs(num) - whole;
    int nondecimal;
    nondecimal = (whole > 0) ? (int) (Math.log(whole) / Math.log(10)) : 1;

    precision =
      (decimal > 0) ? (int) Math
        .abs(((Math.log(Math.abs(num)) / Math.log(10)))) + 2 : 1;
    if (precision > 5) {
      precision = 1;
    }

    String numString =
      weka.core.Utils
        .doubleToString(num, nondecimal + 1 + precision, precision);

    return numString;
  }

  /**
   * Get the default settings for this viewer
   *
   * @return the default settings for this viewer
   */
  @Override
  public Defaults getDefaultSettings() {
    return new StripChartInteractiveViewDefaults();
  }

  /**
   * Apply settings from the supplied settings object
   *
   * @param settings the settings object that might (or might not) have been
   */
  @Override
  public void applySettings(Settings settings) {
    m_BackgroundColor =
      settings.getSetting(StripChartInteractiveViewDefaults.ID,
        StripChartInteractiveViewDefaults.BACKGROUND_COLOR_KEY,
        StripChartInteractiveViewDefaults.BACKGROUND_COLOR,
        Environment.getSystemWide());
    m_plotPanel.setBackground(m_BackgroundColor);
    m_scalePanel.setBackground(m_BackgroundColor);
    m_legendPanel.setBackground(m_BackgroundColor);

    m_LegendPanelBorderColor =
      settings.getSetting(StripChartInteractiveViewDefaults.ID,
        StripChartInteractiveViewDefaults.LEGEND_BORDER_COLOR_KEY,
        StripChartInteractiveViewDefaults.LEGEND_BORDER_COLOR,
        Environment.getSystemWide());

    Font lf = new Font("Monospaced", Font.PLAIN, 12);
    m_legendPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createEtchedBorder(Color.gray, Color.darkGray), "Legend",
      TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, lf,
      m_LegendPanelBorderColor));

    m_colorList[m_colorList.length - 1] =
      settings.getSetting(StripChartInteractiveViewDefaults.ID,
        StripChartInteractiveViewDefaults.X_LABEL_COLOR_KEY,
        StripChartInteractiveViewDefaults.X_LABEL_COLOR,
        Environment.getSystemWide());
  }

  /**
   * Class defining default settings for this viewer
   */
  protected static final class StripChartInteractiveViewDefaults extends
    Defaults {

    public static final String ID = "weka.gui.knowledgeflow.steps.stripchart";

    protected static final Settings.SettingKey BACKGROUND_COLOR_KEY =
      new Settings.SettingKey(ID + ".outputBackgroundColor",
        "Output background color", "Output background color");
    protected static final Color BACKGROUND_COLOR = Color.black;

    protected static final Settings.SettingKey LEGEND_BORDER_COLOR_KEY =
      new Settings.SettingKey(ID + ".legendBorderColor", "Legend border color",
        "Legend border color");
    protected static final Color LEGEND_BORDER_COLOR = new Color(253, 255, 61);

    protected static final Settings.SettingKey X_LABEL_COLOR_KEY =
      new Settings.SettingKey(ID + ".xLabelColor", "Color for x label text",
        "Color for x label text");
    protected static final Color X_LABEL_COLOR = Color.white;

    private static final long serialVersionUID = 2247370679260844812L;

    public StripChartInteractiveViewDefaults() {
      super(ID);
      m_defaults.put(BACKGROUND_COLOR_KEY, BACKGROUND_COLOR);
      m_defaults.put(LEGEND_BORDER_COLOR_KEY, LEGEND_BORDER_COLOR);
      m_defaults.put(X_LABEL_COLOR_KEY, X_LABEL_COLOR);
    }
  }
}
