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
 *    StripChart.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A step that can display a viewer showing a right-to-left scrolling chart
 * for streaming data
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "StripChart", category = "Visualization",
  toolTipText = "Plot streaming data", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "StripChart.gif")
public class StripChart extends BaseStep {

  private static final long serialVersionUID = -2569383350174947630L;

  /** Parties interested in knowing about updates */
  protected List<PlotNotificationListener> m_plotListeners =
    new ArrayList<PlotNotificationListener>();

  /** Frequency for plotting x values */
  protected int m_xValFreq = 500;

  /**
   * Plot every m_refreshFrequency'th point
   */
  private int m_refreshFrequency = 5;

  private int m_userRefreshWidth = 1;

  /** True if we've been reset */
  protected boolean m_reset;

  /**
   * Holds the number of attribute values (10 max) to plot if processing an
   * incoming instance stream
   */
  protected int m_instanceWidth;

  /**
   * GUI Tip text
   *
   * @return the tip text for this option
   */
  public String xLabelFreqTipText() {
    return "Show x axis labels this often";
  }

  /**
   * Get the x label frequency
   *
   * @return the x label frequency
   */
  public int getXLabelFreq() {
    return m_xValFreq;
  }

  /**
   * Set the x label frequency
   *
   * @param freq the x label frequency
   */
  public void setXLabelFreq(int freq) {
    m_xValFreq = freq;
  }

  /**
   * GUI Tip text
   *
   * @return a <code>String</code> value
   */
  public String refreshFreqTipText() {
    return "Plot every x'th data point";
  }

  /**
   * Set how often (in x axis points) to refresh the display
   *
   * @param freq an <code>int</code> value
   */
  public void setRefreshFreq(int freq) {
    m_refreshFrequency = freq;
  }

  /**
   * Get the refresh frequency
   *
   * @return an <code>int</code> value
   */
  public int getRefreshFreq() {
    return m_refreshFrequency;
  }

  /**
   * GUI Tip text
   *
   * @return a <code>String</code> value
   */
  public String refreshWidthTipText() {
    return "The number of pixels to shift the plot by every time a point"
      + " is plotted.";
  }

  /**
   * Set how many pixels to shift the plot by every time a point is plotted
   *
   * @param width the number of pixels to shift the plot by
   */
  public void setRefreshWidth(int width) {
    if (width > 0) {
      m_userRefreshWidth = width;
    }
  }

  /**
   * Get how many pixels to shift the plot by every time a point is plotted
   *
   * @return the number of pixels to shift the plot by
   */
  public int getRefreshWidth() {
    return m_userRefreshWidth;
  }

  @Override
  public void stepInit() throws WekaException {
    m_reset = true;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      // done
      // notify downstream steps of end of stream
      Data d = new Data(data.getConnectionName());
      getStepManager().throughputFinished(d);
      return;
    }

    getStepManager().throughputUpdateStart();
    if (m_plotListeners.size() > 0) {
      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0) {
        Instance instance =
          (Instance) data.getPayloadElement(StepManager.CON_INSTANCE);
        if (m_reset) {
          m_reset = false;
          List<String> legendEntries = new ArrayList<String>();
          int i;
          for (i = 0; i < instance.dataset().numAttributes() && i < 10; i++) {
            legendEntries.add(instance.dataset().attribute(i).name());
          }
          m_instanceWidth = i;

          for (PlotNotificationListener l : m_plotListeners) {
            l.setLegend(legendEntries, 0.0, 1.0);
          }
        }

        double[] dataPoint = new double[m_instanceWidth];
        for (int i = 0; i < dataPoint.length; i++) {
          if (!instance.isMissing(i)) {
            dataPoint[i] = instance.value(i);
          }
        }
        for (PlotNotificationListener l : m_plotListeners) {
          l.acceptDataPoint(dataPoint);
        }

      } else if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_CHART) > 0) {
        if (m_reset) {
          m_reset = false;
          double min =
            data.getPayloadElement(StepManager.CON_AUX_DATA_CHART_MIN, 0.0);
          double max =
            data.getPayloadElement(StepManager.CON_AUX_DATA_CHART_MAX, 1.0);
          List<String> legend =
            (List<String>) data
              .getPayloadElement(StepManager.CON_AUX_DATA_CHART_LEGEND);
          for (PlotNotificationListener l : m_plotListeners) {
            l.setLegend(legend, min, max);
          }
        }
        double[] dataPoint =
          (double[]) data
            .getPayloadElement(StepManager.CON_AUX_DATA_CHART_DATA_POINT);
        for (PlotNotificationListener l : m_plotListeners) {
          l.acceptDataPoint(dataPoint);
        }
      }
    }
    getStepManager().throughputUpdateEnd();
  }

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_CHART);
    }

    return new ArrayList<String>();
  }

  /**
   * Add a plot notification listener
   *
   * @param listener the listener to be notified
   */
  public synchronized void addPlotNotificationListener(
    PlotNotificationListener listener) {
    m_plotListeners.add(listener);
  }

  /**
   * Remove a plot notification listener
   *
   * @param l the listener to remove
   */
  public synchronized void removePlotNotificationListener(
    PlotNotificationListener l) {
    m_plotListeners.remove(l);
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return new ArrayList<String>();
  }

  /**
   * When running in a graphical execution environment a step can make one or
   * more popup Viewer components available. These might be used to display
   * results, graphics etc. Returning null indicates that the step has no such
   * additional graphical views. The map returned by this method should be keyed
   * by action name (e.g. "View results"), and values should be fully qualified
   * names of the corresponding StepInteractiveView implementation. Furthermore,
   * the contents of this map can (and should) be dependent on whether a
   * particular viewer should be made available - i.e. if execution hasn't
   * occurred yet, or if a particular incoming connection type is not present,
   * then it might not be possible to view certain results.
   *
   * Viewers can implement StepInteractiveView directly (in which case they need
   * to extends JPanel), or extends the AbstractInteractiveViewer class. The
   * later extends JPanel, uses a BorderLayout, provides a "Close" button and a
   * method to add additional buttons.
   *
   * @return a map of viewer component names, or null if this step has no
   *         graphical views
   */
  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    views.put("Show chart",
      "weka.gui.knowledgeflow.steps.StripChartInteractiveView");

    return views;
  }

  /**
   * StripChartInteractiveView implements this in order to receive data points.
   * Other potential viewer implementations could as well.
   */
  public interface PlotNotificationListener {

    void setLegend(List<String> legendEntries, double min, double max);

    void acceptDataPoint(double[] dataPoint);
  }
}
