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
 *    BoundaryPlotter.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.DensityBasedClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.boundaryvisualizer.DataGenerator;
import weka.gui.boundaryvisualizer.KDDataGenerator;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.ExecutionResult;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepTask;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * A step that computes visualization data for class/cluster decision
 * boundaries.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "BoundaryPlotter", category = "Visualization",
  toolTipText = "Visualize class/cluster decision boundaries in a 2D plot",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultDataVisualizer.gif")
public class BoundaryPlotter extends BaseStep implements DataCollector {

  /** default colours for classes */
  public static final Color[] DEFAULT_COLORS = { Color.red, Color.green,
    Color.blue, new Color(0, 255, 255), // cyan
    new Color(255, 0, 255), // pink
    new Color(255, 255, 0), // yellow
    new Color(255, 255, 255), // white
    new Color(0, 0, 0) };

  private static final long serialVersionUID = 7864251468395026619L;

  /** Holds colors to use */
  protected List<Color> m_Colors = new ArrayList<Color>();

  /**
   * Number of rows of the visualization to compute in parallel. We don't want
   * to dominate the thread pool that is used for executing all steps and step
   * sub-tasks in the KF (this is currently fixed at 50 threads by FlowRunner).
   */
  protected int m_maxRowsInParallel = 10;

  /** Width of images to generate */
  protected int m_imageWidth = 400;

  /** Height of images to generate */
  protected int m_imageHeight = 400;

  /** X axis attribute name/index */
  protected String m_xAttName = "/first";

  /** Y axis attribute name/index */
  protected String m_yAttName = "2";

  /** Superimpose the training data on the plot? */
  protected boolean m_plotTrainingData = true;

  // attribute indices for visualizing on
  protected int m_xAttribute;
  protected int m_yAttribute;

  // min, max and ranges of these attributes
  protected double m_minX;
  protected double m_minY;
  protected double m_maxX;
  protected double m_maxY;
  protected double m_rangeX;
  protected double m_rangeY;

  // pixel width and height in terms of attribute values
  protected double m_pixHeight;
  protected double m_pixWidth;

  /** The currently rendering image */
  protected transient BufferedImage m_osi;

  /** The spec of the scheme being used to render the current image */
  protected String m_currentDescription;

  /** Completed images */
  protected transient Map<String, BufferedImage> m_completedImages;

  /** Classifiers to use */
  protected List<Classifier> m_classifierTemplates;

  /** Clusterers to use */
  protected List<DensityBasedClusterer> m_clustererTemplates;

  /** Copies of trained classifier to use in parallel for prediction */
  protected weka.classifiers.Classifier[] m_threadClassifiers;

  /** Copies of trained clusterer to use in parallel for prediction */
  protected weka.clusterers.Clusterer[] m_threadClusterers;

  /** Data generator copies to use in parallel */
  protected DataGenerator[] m_threadGenerators;

  /** The data generator to use */
  protected KDDataGenerator m_dataGenerator;

  /** User-specified bandwidth */
  protected String m_kBand = "3";

  /** User-specified num samples */
  protected String m_nSamples = "2";

  /** User-specified base for sampling */
  protected String m_sBase = "2";

  /** Parsed bandwidth */
  protected int m_kernelBandwidth = 3;

  /** Parsed samples */
  protected int m_numSamplesPerRegion = 2;

  /** Parsed base */
  protected int m_samplesBase = 2;

  /** Open interactive view? */
  protected transient RenderingUpdateListener m_plotListener;

  /** True if we've been reset */
  protected boolean m_isReset;

  /**
   * Constructor
   */
  public BoundaryPlotter() {
    for (Color element : DEFAULT_COLORS) {
      m_Colors.add(new Color(element.getRed(), element.getGreen(), element
        .getBlue()));
    }
  }

  /**
   * Set the name/index of the X axis attribute
   *
   * @param xAttName name/index of the X axis attribute
   */
  // make programmatic as our dialog will handle these directly, rather than
  // deferring to the GOE
  @ProgrammaticProperty
  @OptionMetadata(displayName = "X attribute",
    description = "Attribute to visualize on the x-axis", displayOrder = 1)
  public void setXAttName(String xAttName) {
    m_xAttName = xAttName;
  }

  /**
   * Get the name/index of the X axis attribute
   *
   * @return the name/index of the X axis attribute
   */
  public String getXAttName() {
    return m_xAttName;
  }

  /**
   * Set the name/index of the Y axis attribute
   *
   * @param attName name/index of the Y axis attribute
   */
  // make programmatic as our dialog will handle these directly, rather than
  // deferring to the GOE
  @ProgrammaticProperty
  @OptionMetadata(displayName = "Y attribute",
    description = "Attribute to visualize on the y-axis", displayOrder = 2)
  public void setYAttName(String attName) {
    m_yAttName = attName;
  }

  /**
   * Get the name/index of the Y axis attribute
   *
   * @return the name/index of the Y axis attribute
   */
  public String getYAttName() {
    return m_yAttName;
  }

  /**
   * Set the base for sampling
   *
   * @param base the base to use
   */
  @OptionMetadata(displayName = "Base for sampling (r)",
    description = "The base for sampling", displayOrder = 3)
  public void setBaseForSampling(String base) {
    m_sBase = base;
  }

  /**
   * Get the base for sampling
   *
   * @return the base to use
   */
  public String getBaseForSampling() {
    return m_sBase;
  }

  /**
   * Set the number of locations/samples per pixel
   *
   * @param num the number of samples to use
   */
  @OptionMetadata(displayName = "Num. locations per pixel",
    description = "Number of locations per pixel", displayOrder = 4)
  public void setNumLocationsPerPixel(String num) {
    m_nSamples = num;
  }

  /**
   * Get the number of locations/samples per pixel
   *
   * @return the number of samples to use
   */
  public String getNumLocationsPerPixel() {
    return m_nSamples;
  }

  /**
   * Set the kernel bandwidth
   *
   * @param band the bandwidth
   */
  @OptionMetadata(displayName = "Kernel bandwidth (k)",
    description = "Kernel bandwidth", displayOrder = 4)
  public void setKernelBandwidth(String band) {
    m_kBand = band;
  }

  /**
   * Get the kernel bandwidth
   *
   * @return the bandwidth
   */
  public String getKernelBandwidth() {
    return m_kBand;
  }

  /**
   * Set the image width (in pixels)
   *
   * @param width the width to use
   */
  @OptionMetadata(displayName = "Image width (pixels)",
    description = "Image width in pixels", displayOrder = 5)
  public void setImageWidth(int width) {
    m_imageWidth = width;
  }

  /**
   * Get the image width (in pixels)
   *
   * @return the width to use
   */
  public int getImageWidth() {
    return m_imageWidth;
  }

  /**
   * Set the image height (in pixels)
   *
   * @param height the height to use
   */
  @OptionMetadata(displayName = "Image height (pixels)",
    description = "Image height in pixels", displayOrder = 6)
  public void setImageHeight(int height) {
    m_imageHeight = height;
  }

  /**
   * Get the image height (in pixels)
   *
   * @return the height to use
   */
  public int getImageHeight() {
    return m_imageHeight;
  }

  /**
   * Set the maximum number of threads to use when computing image rows
   *
   * @param max maximum number of rows to compute in parallel
   */
  @OptionMetadata(displayName = "Max image rows to compute in parallel",
    description = "Use this many tasks for computing rows of the image",
    displayOrder = 7)
  public void setComputeMaxRowsInParallel(int max) {
    if (max > 0) {
      m_maxRowsInParallel = max;
    }
  }

  /**
   * Get the maximum number of threads to use when computing image rows
   *
   * @return the maximum number of rows to compute in parallel
   */
  public int getComputeMaxRowsInParallel() {
    return m_maxRowsInParallel;
  }

  /**
   * Set whether to superimpose the training data points on the plot or not
   *
   * @param plot true to plot the training data
   */
  @OptionMetadata(displayName = "Plot training points",
    description = "Superimpose the training data over the top of the plot",
    displayOrder = 8)
  public void setPlotTrainingData(boolean plot) {
    m_plotTrainingData = plot;
  }

  /**
   * Get whether to superimpose the training data points on the plot or not
   *
   * @return true if plotting the training data
   */
  public boolean getPlotTrainingData() {
    return m_plotTrainingData;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {

    List<StepManager> infos =
      getStepManager().getIncomingConnectedStepsOfConnectionType(
        StepManager.CON_INFO);
    if (infos.size() == 0) {
      throw new WekaException(
        "One or more classifiers/clusterers need to be supplied via an 'info' "
          + "connection type");
    }

    m_classifierTemplates = new ArrayList<Classifier>();
    m_clustererTemplates = new ArrayList<DensityBasedClusterer>();
    for (StepManager m : infos) {
      Step info = m.getInfoStep();

      if (info instanceof weka.knowledgeflow.steps.Classifier) {
        m_classifierTemplates.add(((weka.knowledgeflow.steps.Classifier) info)
          .getClassifier());
      } else if (info instanceof weka.knowledgeflow.steps.Clusterer) {
        weka.clusterers.Clusterer c =
          ((weka.knowledgeflow.steps.Clusterer) info).getClusterer();
        if (!(c instanceof DensityBasedClusterer)) {
          throw new WekaException("Clusterer "
            + c.getClass().getCanonicalName()
            + " is not a DensityBasedClusterer");
        }
        m_clustererTemplates.add((DensityBasedClusterer) c);
      }
    }

    m_completedImages = new LinkedHashMap<String, BufferedImage>();

    if (m_nSamples != null && m_nSamples.length() > 0) {
      String nSampes = environmentSubstitute(m_nSamples);
      try {
        m_numSamplesPerRegion = Integer.parseInt(nSampes);
      } catch (NumberFormatException ex) {
        getStepManager().logWarning(
          "Unable to parse '" + nSampes + "' for num "
            + "samples per region parameter, using default: "
            + m_numSamplesPerRegion);
      }
    }

    if (m_sBase != null && m_sBase.length() > 0) {
      String sBase = environmentSubstitute(m_sBase);
      try {
        m_samplesBase = Integer.parseInt(sBase);
      } catch (NumberFormatException ex) {
        getStepManager().logWarning(
          "Unable to parse '" + sBase + "' for "
            + "the base for sampling parameter, using default: "
            + m_samplesBase);
      }
    }

    if (m_kBand != null && m_kBand.length() > 0) {
      String kBand = environmentSubstitute(m_kBand);
      try {
        m_kernelBandwidth = Integer.parseInt(kBand);
      } catch (NumberFormatException ex) {
        getStepManager().logWarning(
          "Unable to parse '" + kBand + "' for kernel "
            + "bandwidth parameter, using default: " + m_kernelBandwidth);
      }
    }

    /*
     * m_osi = new BufferedImage(m_imageWidth, m_imageHeight,
     * BufferedImage.TYPE_INT_RGB);
     */
    m_isReset = true;
  }

  protected void computeMinMaxAtts(Instances trainingData) {
    m_minX = Double.MAX_VALUE;
    m_minY = Double.MAX_VALUE;
    m_maxX = Double.MIN_VALUE;
    m_maxY = Double.MIN_VALUE;

    boolean allPointsLessThanOne = true;

    if (trainingData.numInstances() == 0) {
      m_minX = m_minY = 0.0;
      m_maxX = m_maxY = 1.0;
    } else {
      for (int i = 0; i < trainingData.numInstances(); i++) {
        Instance inst = trainingData.instance(i);
        double x = inst.value(m_xAttribute);
        double y = inst.value(m_yAttribute);
        if (!Utils.isMissingValue(x) && !Utils.isMissingValue(y)) {
          if (x < m_minX) {
            m_minX = x;
          }
          if (x > m_maxX) {
            m_maxX = x;
          }

          if (y < m_minY) {
            m_minY = y;
          }
          if (y > m_maxY) {
            m_maxY = y;
          }
          if (x > 1.0 || y > 1.0) {
            allPointsLessThanOne = false;
          }
        }
      }
    }

    if (m_minX == m_maxX) {
      m_minX = 0;
    }
    if (m_minY == m_maxY) {
      m_minY = 0;
    }
    if (m_minX == Double.MAX_VALUE) {
      m_minX = 0;
    }
    if (m_minY == Double.MAX_VALUE) {
      m_minY = 0;
    }
    if (m_maxX == Double.MIN_VALUE) {
      m_maxX = 1;
    }
    if (m_maxY == Double.MIN_VALUE) {
      m_maxY = 1;
    }
    if (allPointsLessThanOne) {
      // m_minX = m_minY = 0.0;
      m_maxX = m_maxY = 1.0;
    }

    m_rangeX = (m_maxX - m_minX);
    m_rangeY = (m_maxY - m_minY);

    m_pixWidth = m_rangeX / m_imageWidth;
    m_pixHeight = m_rangeY / m_imageHeight;
  }

  protected int getAttIndex(String attName, Instances data)
    throws WekaException {
    attName = environmentSubstitute(attName);
    int index = -1;

    if (attName.equalsIgnoreCase("first") || attName.equalsIgnoreCase("/first")) {
      index = 0;
    } else if (attName.equalsIgnoreCase("last")
      || attName.equalsIgnoreCase("/last")) {
      index = data.numAttributes() - 1;
    } else {
      Attribute a = data.attribute(attName);
      if (a != null) {
        index = a.index();
      } else {
        // try parsing as a number
        try {
          index = Integer.parseInt(attName);
          index--;
        } catch (NumberFormatException ex) {
        }
      }
    }

    if (index == -1) {
      throw new WekaException("Unable to find attribute '" + attName
        + "' in the data " + "or to parse it as an index");
    }

    return index;
  }

  protected void initDataGenerator(Instances trainingData) throws WekaException {
    boolean[] attsToWeightOn;
    // build DataGenerator
    attsToWeightOn = new boolean[trainingData.numAttributes()];
    attsToWeightOn[m_xAttribute] = true;
    attsToWeightOn[m_yAttribute] = true;

    m_dataGenerator = new KDDataGenerator();
    m_dataGenerator.setWeightingDimensions(attsToWeightOn);
    m_dataGenerator.setKernelBandwidth(m_kernelBandwidth);
    try {
      m_dataGenerator.buildGenerator(trainingData);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  @Override
  public synchronized void processIncoming(Data data) throws WekaException {

    getStepManager().processing();
    Instances training = data.getPrimaryPayload();
    Integer setNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

    m_xAttribute = getAttIndex(m_xAttName, training);
    m_yAttribute = getAttIndex(m_yAttName, training);

    computeMinMaxAtts(training);
    initDataGenerator(training);

    for (Classifier c : m_classifierTemplates) {
      if (isStopRequested()) {
        getStepManager().interrupted();
        return;
      }
      // do classifiers
      doScheme(c, null, training, setNum, maxSetNum);
    }

    for (DensityBasedClusterer c : m_clustererTemplates) {
      if (isStopRequested()) {
        getStepManager().interrupted();
        return;
      }
      doScheme(null, c, training, setNum, maxSetNum);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else {
      getStepManager().finished();
    }
  }

  protected void doScheme(Classifier classifier, DensityBasedClusterer clust,
    Instances trainingData, int setNum, int maxSetNum) throws WekaException {
    try {
      m_osi =
        new BufferedImage(m_imageWidth, m_imageHeight,
          BufferedImage.TYPE_INT_RGB);
      m_currentDescription =
        makeSchemeSpec(classifier != null ? classifier : clust, setNum,
          maxSetNum);
      // notify listeners
      getStepManager()
        .logBasic("Starting new plot for " + m_currentDescription);
      if (m_plotListener != null) {
        m_plotListener.newPlotStarted(m_currentDescription);
      }

      Graphics m = m_osi.getGraphics();
      m.fillRect(0, 0, m_imageWidth, m_imageHeight);

      Classifier toTrainClassifier = null;
      weka.clusterers.DensityBasedClusterer toTrainClusterer = null;
      if (classifier != null) {
        toTrainClassifier =
          (Classifier) AbstractClassifier.makeCopy(classifier);
        toTrainClassifier.buildClassifier(trainingData);
      } else {
        int tempClassIndex = trainingData.classIndex();
        trainingData.setClassIndex(-1);
        toTrainClusterer =
          (DensityBasedClusterer) weka.clusterers.AbstractClusterer
            .makeCopy((weka.clusterers.Clusterer) clust);
        toTrainClusterer.buildClusterer(trainingData);
        trainingData.setClassIndex(tempClassIndex);
      }

      // populate the thread classifiers ready for parallel processing
      if (toTrainClassifier != null) {
        m_threadClassifiers =
          AbstractClassifier.makeCopies(toTrainClassifier, m_maxRowsInParallel);
      } else {
        m_threadClusterers =
          AbstractClusterer.makeCopies(toTrainClusterer, m_maxRowsInParallel);
      }
      m_threadGenerators = new DataGenerator[m_maxRowsInParallel];
      SerializedObject so = new SerializedObject(m_dataGenerator);
      for (int i = 0; i < m_maxRowsInParallel; i++) {
        m_threadGenerators[i] = (DataGenerator) so.getObject();
      }

      int taskCount = 0;
      List<Future<ExecutionResult<RowResult>>> results =
        new ArrayList<Future<ExecutionResult<RowResult>>>();
      for (int i = 0; i < m_imageHeight; i++) {
        if (taskCount < m_maxRowsInParallel) {
          getStepManager().logDetailed(
            "Launching task to compute image row " + i);
          SchemeRowTask t = new SchemeRowTask(this);
          t.setResourceIntensive(isResourceIntensive());
          t.m_classifier = null;
          t.m_clusterer = null;
          if (toTrainClassifier != null) {
            t.m_classifier = m_threadClassifiers[taskCount];
          } else {
            t.m_clusterer =
              (DensityBasedClusterer) m_threadClusterers[taskCount];
          }
          t.m_rowNum = i;
          t.m_xAtt = m_xAttribute;
          t.m_yAtt = m_yAttribute;
          t.m_imageWidth = m_imageWidth;
          t.m_imageHeight = m_imageHeight;
          t.m_pixWidth = m_pixWidth;
          t.m_pixHeight = m_pixHeight;
          t.m_dataGenerator = m_threadGenerators[taskCount];
          t.m_trainingData = trainingData;
          t.m_minX = m_minX;
          t.m_maxX = m_maxX;
          t.m_minY = m_minY;
          t.m_maxY = m_maxY;
          t.m_numOfSamplesPerRegion = m_numSamplesPerRegion;
          t.m_samplesBase = m_samplesBase;

          results.add(getStepManager().getExecutionEnvironment().submitTask(t));
          taskCount++;
        } else {
          // wait for running tasks
          for (Future<ExecutionResult<RowResult>> r : results) {
            double[][] rowProbs = r.get().getResult().m_rowProbs;
            for (int j = 0; j < m_imageWidth; j++) {
              plotPoint(m_osi, j, r.get().getResult().m_rowNumber, rowProbs[j],
                j == m_imageWidth - 1);
            }
            getStepManager().statusMessage(
              "Completed row " + r.get().getResult().m_rowNumber);
            getStepManager().logDetailed(
              "Completed image row " + r.get().getResult().m_rowNumber);
          }
          results.clear();
          taskCount = 0;
          if (i != m_imageHeight - 1) {
            i--;
          }
          if (isStopRequested()) {
            return;
          }
        }
      }
      if (results.size() > 0) {
        // wait for running tasks
        for (Future<ExecutionResult<RowResult>> r : results) {
          double[][] rowProbs = r.get().getResult().m_rowProbs;
          for (int i = 0; i < m_imageWidth; i++) {
            plotPoint(m_osi, i, r.get().getResult().m_rowNumber, rowProbs[i],
              i == m_imageWidth - 1);
          }
          getStepManager().statusMessage(
            "Completed row " + r.get().getResult().m_rowNumber);
          getStepManager().logDetailed(
            "Completed image row " + r.get().getResult().m_rowNumber);
        }
        if (isStopRequested()) {
          return;
        }
      }

      if (m_plotTrainingData) {
        plotTrainingData(trainingData);
      }

      m_completedImages.put(m_currentDescription, m_osi);
      Data imageOut = new Data(StepManager.CON_IMAGE, m_osi);
      imageOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
        m_currentDescription);
      getStepManager().outputData(imageOut);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected String makeSchemeSpec(Object scheme, int setNum, int maxSetNum) {
    String name = scheme.getClass().getCanonicalName();
    name = name.substring(name.lastIndexOf('.') + 1, name.length());
    if (scheme instanceof OptionHandler) {
      name += " " + Utils.joinOptions(((OptionHandler) scheme).getOptions());
    }
    if (maxSetNum != 1) {
      name += " (set " + setNum + " of " + maxSetNum + ")";
    }

    return name;
  }

  protected void plotPoint(BufferedImage osi, int x, int y, double[] probs,
    boolean update) {
    Graphics osg = osi.getGraphics();
    osg.setPaintMode();

    float[] colVal = new float[3];

    float[] tempCols = new float[3];
    for (int k = 0; k < probs.length; k++) {
      Color curr = m_Colors.get(k % m_Colors.size());

      curr.getRGBColorComponents(tempCols);
      for (int z = 0; z < 3; z++) {
        colVal[z] += probs[k] * tempCols[z];
      }
    }

    for (int z = 0; z < 3; z++) {
      if (colVal[z] < 0) {
        colVal[z] = 0;
      } else if (colVal[z] > 1) {
        colVal[z] = 1;
      }
    }

    osg.setColor(new Color(colVal[0], colVal[1], colVal[2]));
    osg.fillRect(x, y, 1, 1);

    if (update) {
      // end of row
      // generate an update event for interactive viewer to consume
      if (m_plotListener != null) {
        m_plotListener.currentPlotRowCompleted(y);
      }
    }
  }

  public void plotTrainingData(Instances trainingData) {
    Graphics2D osg = (Graphics2D) m_osi.getGraphics();
    // Graphics g = m_plotPanel.getGraphics();
    osg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
    double xval = 0;
    double yval = 0;

    for (int i = 0; i < trainingData.numInstances(); i++) {
      if (!trainingData.instance(i).isMissing(m_xAttribute)
        && !trainingData.instance(i).isMissing(m_yAttribute)) {

        xval = trainingData.instance(i).value(m_xAttribute);
        yval = trainingData.instance(i).value(m_yAttribute);

        int panelX = convertToImageX(xval);
        int panelY = convertToImageY(yval);
        Color colorToPlotWith = Color.white;
        if (trainingData.classIndex() > 0) {
          colorToPlotWith =
            m_Colors.get((int) trainingData.instance(i).value(
              trainingData.classIndex())
              % m_Colors.size());
        }
        if (colorToPlotWith.equals(Color.white)) {
          osg.setColor(Color.black);
        } else {
          osg.setColor(Color.white);
        }
        osg.fillOval(panelX - 3, panelY - 3, 7, 7);
        osg.setColor(colorToPlotWith);
        osg.fillOval(panelX - 2, panelY - 2, 5, 5);
      }
    }

    if (m_plotListener != null) {
      m_plotListener.renderingImageUpdate();
    }
  }

  private int convertToImageX(double xval) {
    double temp = (xval - m_minX) / m_rangeX;
    temp = temp * m_imageWidth;

    return (int) temp;
  }

  private int convertToImageY(double yval) {
    double temp = (yval - m_minY) / m_rangeY;
    temp = temp * m_imageHeight;
    temp = m_imageHeight - temp;

    return (int) temp;
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
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_INFO);
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
    return Arrays.asList(StepManager.CON_IMAGE);
  }

  /**
   * Get the completed images
   *
   * @return a map of completed images
   */
  public Map<String, BufferedImage> getImages() {
    return m_completedImages;
  }

  /**
   * Get the currently rendering image
   *
   * @return the current image
   */
  public BufferedImage getCurrentImage() {
    return m_osi;
  }

  /**
   * Set a listener to receive rendering updates
   *
   * @param l the {@code RenderingUpdateListener} to add
   */
  public void setRenderingListener(RenderingUpdateListener l) {
    m_plotListener = l;
  }

  /**
   * Remove the rendering update listener
   *
   * @param l the {@code RenderingUpdateListener} to remove
   */
  public void removeRenderingListener(RenderingUpdateListener l) {
    if (l == m_plotListener) {
      m_plotListener = null;
    }
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
    if (m_plotListener == null) {
      views.put("Show plots",
        "weka.gui.knowledgeflow.steps.BoundaryPlotterInteractiveView");
    }
    return views;
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
    return "weka.gui.knowledgeflow.steps.BoundaryPlotterStepEditorDialog";
  }

  /**
   * Get the map of completed images
   *
   * @return the map of completed images
   */
  @Override
  public Object retrieveData() {
    return ImageViewer.bufferedImageMapToSerializableByteMap(m_completedImages);
  }

  /**
   * Set a map of images.
   *
   * @param data the images to set
   * @throws WekaException if a problem occurs
   */
  @Override
  @SuppressWarnings("unchecked")
  public void restoreData(Object data) throws WekaException {
    if (!(data instanceof Map)) {
      throw new IllegalArgumentException("Argument must be a Map");
    }

    try {
      m_completedImages =
        ImageViewer
          .byteArrayImageMapToBufferedImageMap((Map<String, byte[]>) data);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Interface for something that wants to be informed of rendering progress
   * updates
   */
  public interface RenderingUpdateListener {

    /**
     * Called when a new plot is started
     * 
     * @param description the description/title of the plot
     */
    void newPlotStarted(String description);

    /**
     * Called when rendering of a row in the current plot has completed
     * 
     * @param row the index of the row that was completed
     */
    void currentPlotRowCompleted(int row);

    /**
     * Called when a change (other than rendering a row) to the current plot has
     * occurred.
     */
    void renderingImageUpdate();
  }

  /**
   * Holds computed image data for a row of an image
   */
  protected static class RowResult {
    /** Probabilities for the pixels in a row of the image */
    protected double[][] m_rowProbs;

    /** The row number of this result */
    protected int m_rowNumber;
  }

  /**
   * A task for computing a row of an image using a trained model
   */
  protected static class SchemeRowTask extends StepTask<RowResult> implements
    Serializable {

    private static final long serialVersionUID = -4144732293602550066L;

    protected int m_xAtt;
    protected int m_yAtt;
    protected int m_rowNum;
    protected int m_imageWidth;
    protected int m_imageHeight;
    protected double m_pixWidth;
    protected double m_pixHeight;
    protected weka.classifiers.Classifier m_classifier;
    protected weka.clusterers.DensityBasedClusterer m_clusterer;
    protected DataGenerator m_dataGenerator;
    protected Instances m_trainingData;
    protected double m_minX;
    protected double m_maxX;
    protected double m_minY;
    protected double m_maxY;
    protected int m_numOfSamplesPerRegion;
    protected double m_samplesBase;

    private Random m_random;
    private int m_numOfSamplesPerGenerator;
    private boolean[] m_attsToWeightOn;
    private double[] m_weightingAttsValues;
    private double[] m_vals;
    private double[] m_dist;
    Instance m_predInst;

    public SchemeRowTask(Step source) {
      super(source);
    }

    @Override
    public void process() throws Exception {
      RowResult result = new RowResult();
      result.m_rowNumber = m_rowNum;
      result.m_rowProbs = new double[m_imageWidth][0];

      m_random = new Random(m_rowNum * 11);
      m_dataGenerator.setSeed(m_rowNum * 11);

      m_numOfSamplesPerGenerator =
        (int) Math.pow(m_samplesBase, m_trainingData.numAttributes() - 3);
      if (m_trainingData == null) {
        throw new Exception("No training data set");
      }
      if (m_classifier == null && m_clusterer == null) {
        throw new Exception("No scheme set");
      }
      if (m_dataGenerator == null) {
        throw new Exception("No data generator set");
      }
      if (m_trainingData.attribute(m_xAtt).isNominal()
        || m_trainingData.attribute(m_yAtt).isNominal()) {
        throw new Exception("Visualization dimensions must be numeric");
      }

      m_attsToWeightOn = new boolean[m_trainingData.numAttributes()];
      m_attsToWeightOn[m_xAtt] = true;
      m_attsToWeightOn[m_yAtt] = true;

      // generate samples
      m_weightingAttsValues = new double[m_attsToWeightOn.length];
      m_vals = new double[m_trainingData.numAttributes()];
      m_predInst = new DenseInstance(1.0, m_vals);
      m_predInst.setDataset(m_trainingData);
      getLogHandler().logDetailed("Computing row number: " + m_rowNum);
      for (int j = 0; j < m_imageWidth; j++) {
        double[] preds = calculateRegionProbs(j, m_rowNum);
        result.m_rowProbs[j] = preds;
      }

      getExecutionResult().setResult(result);
    }

    private double[] calculateRegionProbs(int j, int i) throws Exception {
      double[] sumOfProbsForRegion =
        new double[m_classifier != null ? m_trainingData.classAttribute()
          .numValues() : ((weka.clusterers.Clusterer) m_clusterer)
          .numberOfClusters()];

      double sumOfSums = 0;
      for (int u = 0; u < m_numOfSamplesPerRegion; u++) {

        double[] sumOfProbsForLocation =
          new double[m_classifier != null ? m_trainingData.classAttribute()
            .numValues() : ((weka.clusterers.Clusterer) m_clusterer)
            .numberOfClusters()];

        m_weightingAttsValues[m_xAtt] = getRandomX(j);
        m_weightingAttsValues[m_yAtt] = getRandomY(m_imageHeight - i - 1);

        m_dataGenerator.setWeightingValues(m_weightingAttsValues);

        double[] weights = m_dataGenerator.getWeights();
        double sumOfWeights = Utils.sum(weights);
        sumOfSums += sumOfWeights;
        int[] indices = Utils.sort(weights);

        // Prune 1% of weight mass
        int[] newIndices = new int[indices.length];
        double sumSoFar = 0;
        double criticalMass = 0.99 * sumOfWeights;
        int index = weights.length - 1;
        int counter = 0;
        for (int z = weights.length - 1; z >= 0; z--) {
          newIndices[index--] = indices[z];
          sumSoFar += weights[indices[z]];
          counter++;
          if (sumSoFar > criticalMass) {
            break;
          }
        }
        indices = new int[counter];
        System.arraycopy(newIndices, index + 1, indices, 0, counter);

        for (int z = 0; z < m_numOfSamplesPerGenerator; z++) {

          m_dataGenerator.setWeightingValues(m_weightingAttsValues);
          double[][] values = m_dataGenerator.generateInstances(indices);

          for (int q = 0; q < values.length; q++) {
            if (values[q] != null) {
              System.arraycopy(values[q], 0, m_vals, 0, m_vals.length);
              m_vals[m_xAtt] = m_weightingAttsValues[m_xAtt];
              m_vals[m_yAtt] = m_weightingAttsValues[m_yAtt];

              // classify/cluster the instance
              m_dist =
                m_classifier != null ? m_classifier
                  .distributionForInstance(m_predInst) : m_clusterer
                  .distributionForInstance(m_predInst);

              for (int k = 0; k < sumOfProbsForLocation.length; k++) {
                sumOfProbsForLocation[k] += (m_dist[k] * weights[q]);
              }
            }
          }
        }

        for (int k = 0; k < sumOfProbsForRegion.length; k++) {
          sumOfProbsForRegion[k] +=
            (sumOfProbsForLocation[k] / m_numOfSamplesPerGenerator);
        }
      }

      if (sumOfSums > 0) {
        // average
        Utils.normalize(sumOfProbsForRegion, sumOfSums);
      } else {
        throw new Exception(
          "Arithmetic underflow. Please increase value of kernel bandwidth " +
                  "parameter (k).");
      }

      // cache
      double[] tempDist = new double[sumOfProbsForRegion.length];
      System.arraycopy(sumOfProbsForRegion, 0, tempDist, 0,
        sumOfProbsForRegion.length);

      return tempDist;
    }

    /**
     * Return a random x attribute value contained within the pix'th horizontal
     * pixel
     *
     * @param pix the horizontal pixel number
     * @return a value in attribute space
     */
    private double getRandomX(int pix) {

      double minPix = m_minX + (pix * m_pixWidth);

      return minPix + m_random.nextDouble() * m_pixWidth;
    }

    /**
     * Return a random y attribute value contained within the pix'th vertical
     * pixel
     *
     * @param pix the vertical pixel number
     * @return a value in attribute space
     */
    private double getRandomY(int pix) {

      double minPix = m_minY + (pix * m_pixHeight);

      return minPix + m_random.nextDouble() * m_pixHeight;
    }
  }
}
