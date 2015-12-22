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
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "StripChart", category = "Visualization",
  toolTipText = "Plot streaming data", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "StripChart.gif")
public class StripChart extends BaseStep {

  protected List<PlotNotificationListener> m_plotListeners =
    new ArrayList<PlotNotificationListener>();

  protected int m_xValFreq = 500;

  /**
   * Plot every m_refreshFrequency'th point
   */
  private int m_refreshFrequency = 5;

  private int m_userRefreshWidth = 1;

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

  public int getXLabelFreq() {
    return m_xValFreq;
  }

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

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_CHART);
    }

    return new ArrayList<String>();
  }

  public synchronized void addPlotNotificationListener(
    PlotNotificationListener listener) {
    m_plotListeners.add(listener);
  }

  public synchronized void removePlotNotificationListener(
    PlotNotificationListener l) {
    m_plotListeners.remove(l);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return new ArrayList<String>();
  }

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
