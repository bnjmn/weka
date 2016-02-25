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
 *    DataVisualizer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.PluginManager;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.OffscreenChartRenderer;
import weka.gui.beans.WekaOffscreenChartRenderer;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.visualize.PlotData2D;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A step that provides a visualization based on
 * weka.gui.visualize.VisualizePanel
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "DataVisualizer", category = "Visualization",
  toolTipText = "Visualize training/test sets in a 2D scatter plot.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultDataVisualizer.gif")
public class DataVisualizer extends BaseStep implements DataCollector {

  private static final long serialVersionUID = -8013077913672918384L;

  /** Current set of plots. First element is the master plot */
  protected List<PlotData2D> m_plots = new ArrayList<PlotData2D>();

  protected transient OffscreenChartRenderer m_offscreenRenderer;

  /** Name of the renderer to use for offscreen chart rendering */
  protected String m_offscreenRendererName = "Weka Chart Renderer";

  /**
   * The name of the attribute to use for the x-axis of offscreen plots. If left
   * empty, False Positive Rate is used for threshold curves
   */
  protected String m_xAxis = "";

  /**
   * The name of the attribute to use for the y-axis of offscreen plots. If left
   * empty, True Positive Rate is used for threshold curves
   */
  protected String m_yAxis = "";

  /**
   * Additional options for the offscreen renderer
   */
  protected String m_additionalOptions = "";

  /** Width of offscreen plots */
  protected String m_width = "500";

  /** Height of offscreen plots */
  protected String m_height = "400";

  /**
   * Set the name of the attribute for the x-axis in offscreen plots. This
   * defaults to "False Positive Rate" for threshold curves if not specified.
   *
   * @param xAxis the name of the xAxis
   */
  @OptionMetadata(displayName = "X-axis attribute",
    description = "Attribute name " + "or /first, /last or /<index>",
    displayOrder = 1)
  public void setOffscreenXAxis(String xAxis) {
    m_xAxis = xAxis;
  }

  /**
   * Get the name of the attribute for the x-axis in offscreen plots
   *
   * @return the name of the xAxis
   */
  public String getOffscreenXAxis() {
    return m_xAxis;
  }

  /**
   * Set the name of the attribute for the y-axis in offscreen plots. This
   * defaults to "True Positive Rate" for threshold curves if not specified.
   *
   * @param yAxis the name of the xAxis
   */
  @OptionMetadata(displayName = "Y-axis attribute",
    description = "Attribute name " + "or /first, /last or /<index>",
    displayOrder = 2)
  public void setOffscreenYAxis(String yAxis) {
    m_yAxis = yAxis;
  }

  /**
   * Get the name of the attribute for the y-axix of offscreen plots.
   *
   * @return the name of the yAxis.
   */
  public String getOffscreenYAxis() {
    return m_yAxis;
  }

  /**
   * Set the width (in pixels) of the offscreen image to generate.
   *
   * @param width the width in pixels.
   */
  @OptionMetadata(displayName = "Chart width (pixels)",
    description = "Width of the rendered chart", displayOrder = 3)
  public void setOffscreenWidth(String width) {
    m_width = width;
  }

  /**
   * Get the width (in pixels) of the offscreen image to generate.
   *
   * @return the width in pixels.
   */
  public String getOffscreenWidth() {
    return m_width;
  }

  /**
   * Set the height (in pixels) of the offscreen image to generate
   *
   * @param height the height in pixels
   */
  @OptionMetadata(displayName = "Chart height (pixels)",
    description = "Height of the rendered chart", displayOrder = 4)
  public void setOffscreenHeight(String height) {
    m_height = height;
  }

  /**
   * Get the height (in pixels) of the offscreen image to generate
   *
   * @return the height in pixels
   */
  public String getOffscreenHeight() {
    return m_height;
  }

  /**
   * Set the name of the renderer to use for offscreen chart rendering
   * operations
   *
   * @param rendererName the name of the renderer to use
   */
  @ProgrammaticProperty
  public void setOffscreenRendererName(String rendererName) {
    m_offscreenRendererName = rendererName;
    m_offscreenRenderer = null;
  }

  /**
   * Get the name of the renderer to use for offscreen chart rendering
   * operations
   *
   * @return the name of the renderer to use
   */
  public String getOffscreenRendererName() {
    return m_offscreenRendererName;
  }

  /**
   * Set the additional options for the offscreen renderer
   *
   * @param additional additional options
   */
  @ProgrammaticProperty
  public void setOffscreenAdditionalOpts(String additional) {
    m_additionalOptions = additional;
  }

  /**
   * Get the additional options for the offscreen renderer
   *
   * @return the additional options
   */
  public String getOffscreenAdditionalOpts() {
    return m_additionalOptions;
  }

  /**
   * Configures the offscreen renderer to use
   */
  protected void setupOffscreenRenderer() {
    getStepManager().logDetailed(
      "Initializing offscreen renderer: " + getOffscreenRendererName());
    if (m_offscreenRenderer == null) {
      if (m_offscreenRendererName == null
        || m_offscreenRendererName.length() == 0) {
        m_offscreenRenderer = new WekaOffscreenChartRenderer();
        return;
      }

      if (m_offscreenRendererName.equalsIgnoreCase("weka chart renderer")) {
        m_offscreenRenderer = new WekaOffscreenChartRenderer();
      } else {
        try {
          Object r =
            PluginManager.getPluginInstance(
              "weka.gui.beans.OffscreenChartRenderer", m_offscreenRendererName);
          if (r != null && r instanceof weka.gui.beans.OffscreenChartRenderer) {
            m_offscreenRenderer = (OffscreenChartRenderer) r;
          } else {
            // use built-in default
            getStepManager().logWarning(
              "Offscreen renderer '" + getOffscreenRendererName()
                + "' is not available, using default weka chart renderer "
                + "instead");
            m_offscreenRenderer = new WekaOffscreenChartRenderer();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
          // use built-in default
          getStepManager().logWarning(
            "Offscreen renderer '" + getOffscreenRendererName()
              + "' is not available, using default weka chart renderer "
              + "instead");
          m_offscreenRenderer = new WekaOffscreenChartRenderer();
        }
      }
    }
  }

  @Override
  public void stepInit() throws WekaException {
    // nothing to do
  }

  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    Instances toPlot = data.getPrimaryPayload();
    String name = (new SimpleDateFormat("HH:mm:ss.SSS - ")).format(new Date());
    String relationName = toPlot.relationName();
    PlotData2D pd = new PlotData2D(toPlot);
    if (relationName.startsWith("__")) {
      boolean[] connect = new boolean[toPlot.numInstances()];
      for (int i = 1; i < toPlot.numInstances(); i++) {
        connect[i] = true;
      }
      try {
        pd.setConnectPoints(connect);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      relationName = relationName.substring(2);
    }

    String title = name + relationName;
    getStepManager().logDetailed("Processing " + title);
    pd.setPlotName(title);
    m_plots.add(pd);

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_IMAGE) > 0) {
      setupOffscreenRenderer();
      BufferedImage osi = createOffscreenPlot(pd);

      Data imageData = new Data(StepManager.CON_IMAGE, osi);
      if (relationName.length() > 10) {
        relationName = relationName.substring(0, 10);
      }
      imageData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        relationName + ":" + m_xAxis + "," + m_yAxis);
      getStepManager().outputData(imageData);
    }
    getStepManager().finished();
  }

  protected BufferedImage createOffscreenPlot(PlotData2D pd)
    throws WekaException {
    setupOffscreenRenderer();

    List<Instances> offscreenPlotInstances = new ArrayList<Instances>();
    Instances predictedI = pd.getPlotInstances();
    if (predictedI.classIndex() >= 0 && predictedI.classAttribute().isNominal()) {
      // set up multiple series - one for each class
      Instances[] classes = new Instances[predictedI.numClasses()];
      for (int i = 0; i < predictedI.numClasses(); i++) {
        classes[i] = new Instances(predictedI, 0);
        classes[i].setRelationName(predictedI.classAttribute().value(i));
      }
      for (int i = 0; i < predictedI.numInstances(); i++) {
        Instance current = predictedI.instance(i);
        classes[(int) current.classValue()].add((Instance) current.copy());
      }
      for (Instances classe : classes) {
        offscreenPlotInstances.add(classe);
      }
    } else {
      offscreenPlotInstances.add(new Instances(predictedI));
    }

    List<String> options = new ArrayList<String>();
    String additional = m_additionalOptions;
    if (m_additionalOptions != null && m_additionalOptions.length() > 0) {
      additional = environmentSubstitute(additional);
    }
    if (additional != null && !additional.contains("-color")) {
      // for WekaOffscreenChartRenderer only
      if (additional.length() > 0) {
        additional += ",";
      }
      if (predictedI.classIndex() >= 0) {
        additional += "-color=" + predictedI.classAttribute().name();
      } else {
        additional += "-color=/last";
      }
    }
    String[] optionsParts = additional.split(",");
    for (String p : optionsParts) {
      options.add(p.trim());
    }

    String xAxis = m_xAxis;
    xAxis = environmentSubstitute(xAxis);

    String yAxis = m_yAxis;

    yAxis = environmentSubstitute(yAxis);

    String width = m_width;
    String height = m_height;
    int defWidth = 500;
    int defHeight = 400;

    width = environmentSubstitute(width);
    height = environmentSubstitute(height);

    defWidth = Integer.parseInt(width);
    defHeight = Integer.parseInt(height);

    getStepManager().logDetailed("Creating image");

    try {
      return predictedI.relationName().startsWith("__") ? m_offscreenRenderer
        .renderXYLineChart(defWidth, defHeight, offscreenPlotInstances, xAxis,
          yAxis, options) : m_offscreenRenderer.renderXYScatterPlot(defWidth,
        defHeight, offscreenPlotInstances, xAxis, yAxis, options);
    } catch (Exception e) {
      throw new WekaException(e);
    }
  }

  public List<PlotData2D> getPlots() {
    return m_plots;
  }

  public void clearPlotData() {
    m_plots.clear();
  }

  @Override
  public Object retrieveData() {
    return getPlots();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void restoreData(Object data) throws WekaException {
    if (!(data instanceof List)) {
      throw new WekaException("Argument must be a List<PlotData2D>");
    }
    m_plots = (List<PlotData2D>) data;

    // need to generate the outgoing Image data...
    for (PlotData2D pd : m_plots) {
      createOffscreenPlot(pd);
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnections() > 0 ? Arrays
      .asList(StepManager.CON_IMAGE) : new ArrayList<String>();
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.DataVisualizerStepEditorDialog";
  }

  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_plots.size() > 0) {
      views.put("Show charts",
        "weka.gui.knowledgeflow.steps.DataVisualizerInteractiveView");
    }

    return views;
  }

}
