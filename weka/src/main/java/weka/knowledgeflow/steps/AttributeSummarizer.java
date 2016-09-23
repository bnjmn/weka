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
 *    AttributeSummarizer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.PluginManager;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.OffscreenChartRenderer;
import weka.gui.beans.WekaOffscreenChartRenderer;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step that collects data to display in a summary overview of attribute
 * distributions
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "AttributeSummarizer", category = "Visualization",
  toolTipText = "Visualize datasets in a matrix of histograms",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "AttributeSummarizer.gif")
public class AttributeSummarizer extends BaseSimpleDataVisualizer {

  private static final long serialVersionUID = 2313372820072708102L;

  /** The x-axis attribute name */
  protected String m_xAxis = "";

  /** The offscreen renderer to use */
  protected transient OffscreenChartRenderer m_offscreenRenderer;

  /** Name of the renderer to use for offscreen chart rendering */
  protected String m_offscreenRendererName = "Weka Chart Renderer";

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
   * Set the width (in pixels) of the offscreen image to generate.
   *
   * @param width the width in pixels.
   */
  @OptionMetadata(displayName = "Chart width (pixels)",
    description = "Width of the rendered chart", displayOrder = 2)
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
    description = "Height of the rendered chart", displayOrder = 3)
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
   * Process incoming data
   *
   * @param data the data to process
   */
  @Override
  public synchronized void processIncoming(Data data) {
    super.processIncoming(data, false);

    if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_IMAGE) > 0) {
      setupOffscreenRenderer();
      createOffscreenPlot(data);
    }
    getStepManager().finished();
  }

  /**
   * Create an offscreen plot
   *
   * @param data the data to create the plot from
   */
  protected void createOffscreenPlot(Data data) {
    List<Instances> offscreenPlotData = new ArrayList<Instances>();
    Instances predictedI = data.getPrimaryPayload();
    boolean colorSpecified = false;

    String additional = m_additionalOptions;
    if (m_additionalOptions.length() > 0) {
      additional = environmentSubstitute(additional);
    }

    if (!additional.contains("-color")
      && m_offscreenRendererName.contains("Weka Chart Renderer")) {
      // for WekaOffscreenChartRenderer only
      if (additional.length() > 0) {
        additional += ",";
      }
      if (predictedI.classIndex() >= 0) {
        additional += "-color=" + predictedI.classAttribute().name();
      } else {
        additional += "-color=/last";
      }
    } else {
      colorSpecified = true;
    }

    if (predictedI.classIndex() >= 0 && predictedI.classAttribute().isNominal()
      && !colorSpecified) {
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
        offscreenPlotData.add(classe);
      }
    } else {
      offscreenPlotData.add(new Instances(predictedI));
    }

    List<String> options = new ArrayList<String>();

    String[] optionsParts = additional.split(",");
    for (String p : optionsParts) {
      options.add(p.trim());
    }

    // only need the x-axis (used to specify the attribute to plot)
    String xAxis = m_xAxis;
    xAxis = environmentSubstitute(xAxis);

    String width = m_width;
    String height = m_height;
    int defWidth = 500;
    int defHeight = 400;
    width = environmentSubstitute(width);
    height = environmentSubstitute(height);

    defWidth = Integer.parseInt(width);
    defHeight = Integer.parseInt(height);

    try {
      getStepManager().logDetailed("Creating image");
      BufferedImage osi =
        m_offscreenRenderer.renderHistogram(defWidth, defHeight,
          offscreenPlotData, xAxis, options);

      Data imageData = new Data(StepManager.CON_IMAGE, osi);
      String relationName = predictedI.relationName();
      if (relationName.length() > 10) {
        relationName = relationName.substring(0, 10);
      }
      imageData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        relationName + ":" + m_xAxis);
      getStepManager().outputData(imageData);
    } catch (Exception e1) {
      e1.printStackTrace();
    }

  }

  /**
   * Get a map of popup viewers that can be used with this step
   *
   * @return a map of popup viewers
   */
  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_data.size() > 0) {
      views.put("Show plots",
        "weka.gui.knowledgeflow.steps.AttributeSummarizerInteractiveView");
    }

    return views;
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

  /**
   * Get a list of outgoing connections that this step can produce at this time
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnections() > 0 ? Arrays
      .asList(StepManager.CON_IMAGE) : new ArrayList<String>();
  }

  /**
   * Get the fully qualified class name of the custom editor for this step
   *
   * @return the class name of the custom editor for this step
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.AttributeSummarizerStepEditorDialog";
  }
}
