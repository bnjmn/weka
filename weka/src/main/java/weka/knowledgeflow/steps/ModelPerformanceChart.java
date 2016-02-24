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
 *    ModelPerformanceChart.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.OffscreenChartRenderer;
import weka.core.PluginManager;
import weka.gui.beans.WekaOffscreenChartRenderer;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.visualize.PlotData2D;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Step that collects and displays either classifier error plots or threshold
 * curves
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ModelPerformanceChart", category = "Visualization",
  toolTipText = "Visualize performance charts (such as ROC).",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ModelPerformanceChart.gif")
public class ModelPerformanceChart extends BaseStep implements DataCollector {

  private static final long serialVersionUID = 6166590810777938147L;

  /** Current set of plots. First element is the master plot */
  protected List<PlotData2D> m_plots = new ArrayList<PlotData2D>();

  /** For rendering plots to encapsulate in Image connections */
  protected transient List<Instances> m_offscreenPlotData;
  protected transient List<String> m_thresholdSeriesTitles;
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

  /** True if the collected plots contain threshold data */
  protected boolean m_dataIsThresholdData;

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
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnections() == 0) {
      result.add(StepManager.CON_THRESHOLD_DATA);
      result.add(StepManager.CON_VISUALIZABLE_ERROR);
    } else {
      // we can accept multiple inputs of threshold data, as long
      // as they are comparable (we assume this)
      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_THRESHOLD_DATA) > 0) {
        result.add(StepManager.CON_THRESHOLD_DATA);
      }
    }

    return result;
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
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_IMAGE);
    }
    return result;
  }

  /**
   * Add a threshold plot to the offscreen data collection
   *
   * @param thresholdD the plot data to add
   * @return the image that was added
   * @throws WekaException if a problem occurs
   */
  protected BufferedImage addOffscreenThresholdPlot(PlotData2D thresholdD)
    throws WekaException {
    m_offscreenPlotData.add(thresholdD.getPlotInstances());
    m_thresholdSeriesTitles.add(thresholdD.getPlotName());
    List<String> options = new ArrayList<String>();
    String additional = "-color=/last";
    if (m_additionalOptions != null && m_additionalOptions.length() > 0) {
      additional = m_additionalOptions;
      additional = getStepManager().environmentSubstitute(additional);
    }
    String[] optsParts = additional.split(",");
    for (String p : optsParts) {
      options.add(p.trim());
    }

    String xAxis = "False Positive Rate";
    if (m_xAxis != null && m_xAxis.length() > 0) {
      xAxis = m_xAxis;
      xAxis = getStepManager().environmentSubstitute(xAxis);
    }
    String yAxis = "True Positive Rate";
    if (m_yAxis != null && m_yAxis.length() > 0) {
      yAxis = m_yAxis;
      yAxis = getStepManager().environmentSubstitute(yAxis);
    }

    String width = m_width;
    String height = m_height;
    int defWidth = 500;
    int defHeight = 400;
    width = getStepManager().environmentSubstitute(width);
    height = getStepManager().environmentSubstitute(height);
    defWidth = Integer.parseInt(width);
    defHeight = Integer.parseInt(height);
    List<Instances> series = new ArrayList<Instances>();
    for (int i = 0; i < m_offscreenPlotData.size(); i++) {
      Instances temp = new Instances(m_offscreenPlotData.get(i));
      temp.setRelationName(m_thresholdSeriesTitles.get(i));
      series.add(temp);
    }
    try {
      return m_offscreenRenderer.renderXYLineChart(defWidth, defHeight, series,
        xAxis, yAxis, options);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Add an error plot to the offscreen plot collection
   *
   * @param plotData the plot to add
   * @return the image that was added
   * @throws WekaException if a problem occurs
   */
  protected BufferedImage addOffscreenErrorPlot(PlotData2D plotData)
    throws WekaException {
    Instances predictedI = plotData.getPlotInstances();
    if (predictedI.classAttribute().isNominal()) {

      // split the classes out into individual series.
      // add a new attribute to hold point sizes - correctly
      // classified instances get default point size (2);
      // misclassified instances get point size (5).
      // WekaOffscreenChartRenderer can take advantage of this
      // information - other plugin renderers may or may not
      // be able to use it
      ArrayList<Attribute> atts = new ArrayList<Attribute>();
      for (int i = 0; i < predictedI.numAttributes(); i++) {
        atts.add((Attribute) predictedI.attribute(i).copy());
      }
      atts.add(new Attribute("@@size@@"));
      Instances newInsts =
        new Instances(predictedI.relationName(), atts,
          predictedI.numInstances());
      newInsts.setClassIndex(predictedI.classIndex());

      for (int i = 0; i < predictedI.numInstances(); i++) {
        double[] vals = new double[newInsts.numAttributes()];
        for (int j = 0; j < predictedI.numAttributes(); j++) {
          vals[j] = predictedI.instance(i).value(j);
        }
        vals[vals.length - 1] = 2; // default shape size
        Instance ni = new DenseInstance(1.0, vals);
        newInsts.add(ni);
      }

      // predicted class attribute is always actualClassIndex - 1
      Instances[] classes = new Instances[newInsts.numClasses()];
      for (int i = 0; i < newInsts.numClasses(); i++) {
        classes[i] = new Instances(newInsts, 0);
        classes[i].setRelationName(newInsts.classAttribute().value(i));
      }
      Instances errors = new Instances(newInsts, 0);
      int actualClass = newInsts.classIndex();
      for (int i = 0; i < newInsts.numInstances(); i++) {
        Instance current = newInsts.instance(i);
        classes[(int) current.classValue()].add((Instance) current.copy());

        if (current.value(actualClass) != current.value(actualClass - 1)) {
          Instance toAdd = (Instance) current.copy();

          // larger shape for an error
          toAdd.setValue(toAdd.numAttributes() - 1, 5);

          // swap predicted and actual class value so
          // that the color plotted for the error series
          // is that of the predicted class
          double actualClassV = toAdd.value(actualClass);
          double predictedClassV = toAdd.value(actualClass - 1);
          toAdd.setValue(actualClass, predictedClassV);
          toAdd.setValue(actualClass - 1, actualClassV);

          errors.add(toAdd);
        }
      }

      errors.setRelationName("Errors");
      m_offscreenPlotData.add(errors);

      for (Instances classe : classes) {
        m_offscreenPlotData.add(classe);
      }

    } else {
      // numeric class - have to make a new set of instances
      // with the point sizes added as an additional attribute
      ArrayList<Attribute> atts = new ArrayList<Attribute>();
      for (int i = 0; i < predictedI.numAttributes(); i++) {
        atts.add((Attribute) predictedI.attribute(i).copy());
      }
      atts.add(new Attribute("@@size@@"));
      Instances newInsts =
        new Instances(predictedI.relationName(), atts,
          predictedI.numInstances());

      int[] shapeSizes = plotData.getShapeSize();

      for (int i = 0; i < predictedI.numInstances(); i++) {
        double[] vals = new double[newInsts.numAttributes()];
        for (int j = 0; j < predictedI.numAttributes(); j++) {
          vals[j] = predictedI.instance(i).value(j);
        }
        vals[vals.length - 1] = shapeSizes[i];
        Instance ni = new DenseInstance(1.0, vals);
        newInsts.add(ni);
      }
      newInsts.setRelationName(predictedI.classAttribute().name());
      m_offscreenPlotData.add(newInsts);
    }

    List<String> options = new ArrayList<String>();

    String additional =
      "-color=" + predictedI.classAttribute().name() + ",-hasErrors";
    if (m_additionalOptions != null && m_additionalOptions.length() > 0) {
      additional += "," + m_additionalOptions;
      additional = environmentSubstitute(additional);
    }
    String[] optionsParts = additional.split(",");
    for (String p : optionsParts) {
      options.add(p.trim());
    }

    // if (predictedI.classAttribute().isNumeric()) {
    options.add("-shapeSize=@@size@@");
    // }

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

    try {
      return m_offscreenRenderer.renderXYScatterPlot(defWidth, defHeight,
        m_offscreenPlotData, xAxis, yAxis, options);
    } catch (Exception e1) {
      throw new WekaException(e1);
    }
  }

  /**
   * Process incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    PlotData2D errorD =
      (PlotData2D) data.getPayloadElement(StepManager.CON_VISUALIZABLE_ERROR);
    PlotData2D thresholdD =
      (PlotData2D) data.getPayloadElement(StepManager.CON_THRESHOLD_DATA);

    getStepManager().logDetailed(
      "Processing "
        + (errorD != null ? " error data " + errorD.getPlotName()
          : " threshold data " + thresholdD.getPlotName()));

    if (data.getConnectionName().equals(StepManager.CON_VISUALIZABLE_ERROR)) {
      m_plots.clear();
      m_plots.add(errorD);
      m_dataIsThresholdData = false;

      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_IMAGE) > 0) {
        // configure renderer if necessary
        setupOffscreenRenderer();
        m_offscreenPlotData = new ArrayList<Instances>();
        BufferedImage bi = addOffscreenErrorPlot(errorD);
        Data imageD = new Data(StepManager.CON_IMAGE);
        imageD.setPayloadElement(StepManager.CON_IMAGE, bi);
        getStepManager().outputData(StepManager.CON_IMAGE, imageD);
      }
    } else if (data.getConnectionName().equals(StepManager.CON_THRESHOLD_DATA)) {
      if (m_plots.size() == 0) {
        m_plots.add(thresholdD);
      } else {
        if (!m_plots.get(0).getPlotInstances().relationName()
          .equals(thresholdD.getPlotInstances().relationName())) {
          m_plots.clear();
        }
        m_plots.add(thresholdD);
      }
      m_dataIsThresholdData = true;

      if (getStepManager().numOutgoingConnectionsOfType(StepManager.CON_IMAGE) > 0) {
        // configure renderer if necessary
        setupOffscreenRenderer();
        if (m_offscreenPlotData == null || m_offscreenPlotData.size() == 0
          || !m_offscreenPlotData.get(0).relationName()
            .equals(thresholdD.getPlotInstances().relationName())) {
          m_offscreenPlotData = new ArrayList<Instances>();
          m_thresholdSeriesTitles = new ArrayList<String>();
        }
        BufferedImage bi = addOffscreenThresholdPlot(thresholdD);
        Data imageD = new Data(StepManager.CON_IMAGE);
        imageD.setPayloadElement(StepManager.CON_IMAGE, bi);
        imageD.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
          thresholdD.getPlotName());
        getStepManager().outputData(StepManager.CON_IMAGE, imageD);
      }
    }

    getStepManager().finished();
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

    if (m_plots.size() > 0) {
      views.put("Show chart",
        "weka.gui.knowledgeflow.steps.ModelPerformanceChartInteractiveView");
    }

    return views;
  }

  /**
   * Get the plots currently stored in this step
   *
   * @return
   */
  public List<PlotData2D> getPlots() {
    return m_plots;
  }

  /**
   * Returns true if the plots being stored are threshold plots
   *
   * @return true if the plots are threshold plots
   */
  public boolean isDataIsThresholdData() {
    return m_dataIsThresholdData;
  }

  /**
   * Clear all plot data (both onscreen and offscreen)
   */
  public void clearPlotData() {
    m_plots.clear();

    if (m_offscreenPlotData != null) {
      m_offscreenPlotData.clear();
    }
  }

  /**
   * Retrieve the data (plots) stored in this step
   *
   * @return the data stored in this step
   */
  @Override
  public Object retrieveData() {
    Object[] onAndOffScreen = new Object[2];
    onAndOffScreen[0] = m_plots;
    // onAndOffScreen[1] = m_offscreenPlotData;
    onAndOffScreen[1] = m_dataIsThresholdData;

    return onAndOffScreen;
  }

  /**
   * Restore the data (plots) for this step
   *
   * @param data the data to set
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public void restoreData(Object data) throws WekaException {
    if (!(data instanceof Object[])) {
      throw new WekaException("Argument must be a three element array, "
        + "where the first element holds a list of Plot2D objects, the "
        + "second a list of Instances objects and the third "
        + "a boolean - true if the data is threshold data");
    }
    m_plots = ((List<PlotData2D>) ((Object[]) data)[0]);
    // m_offscreenPlotData = ((List<Instances>) ((Object[]) data)[1]);
    m_dataIsThresholdData = ((Boolean) ((Object[]) data)[1]);
    m_offscreenPlotData = new ArrayList<Instances>();
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    // nothing to do
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.ModelPerformanceChartStepEditorDialog";
  }
}
