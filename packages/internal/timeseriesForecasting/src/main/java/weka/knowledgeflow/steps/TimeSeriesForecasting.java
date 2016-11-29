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
 *    TimeSeriesForecasting.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.knowledgeflow.steps;

import org.apache.commons.codec.binary.Base64;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.AbstractForecaster;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.SerializationHelper;
import weka.filters.supervised.attribute.TSLagMaker;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import javax.swing.JFileChooser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Knowledge Flow step that encapsulates a time series forecasting model and
 * uses it to produce forecasts given incoming historical data.
 *
 * @author Mark Hall
 * @version $Revision: $
 */
@KFStep(name = "TimeSeriesForecasting", category = "TimeSeries",
  toolTipText = "Encapsulates a time series forecasting model and uses it to"
    + " produce forecasts given incoming historical data. Forecaster "
    + "can optionally be rebuilt using the incoming data before a "
    + "forecast is generated.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultClassifier.gif")
public class TimeSeriesForecasting extends BaseStep {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -7826178727365267059L;

  /** The structure of the data used to train the forecaster */
  protected transient Instances m_header;

  /** The forecaster to use for forecasting */
  protected transient WekaForecaster m_forecaster;

  /**
   * The output instances structure - typically the same as the input structure.
   * Will have additional attributes for upper and lower confidence intervals if
   * the forecaster produces them
   */
  protected transient Instances m_outgoingStructure;

  /**
   * The filename to load from - takes precendence over an encoded forecaster if
   * not null and not equal to "-NONE-"
   */
  protected File m_fileName = new File("");

  /**
   * The file name to save the updated forecaster to if the user has opted to
   * rebuild the forecasting model on the incoming data
   */
  protected File m_saveFileName = new File("");

  /**
   * Base 64 encoded forecasting model - this allows the model to be embeded in
   * the XML knowledge flow file format rather than loaded from a file at
   * execution time.
   */
  protected String m_encodedForecaster = "-NONE-";

  /**
   * Number of future time steps to forecast - will be ignored if overlay data
   * is being used since the number of instances containing overlay values will
   * dictate the number of forecasted values that can be produced
   */
  protected String m_numberOfStepsToForecast = "1";

  /** True if the forecaster should be rebuilt on incoming data */
  protected boolean m_rebuildForecaster;

  /**
   * The number of time units beyond the end of the training data used to train
   * the forecaster that the most recent incoming priming instance is. This is
   * used to adjust the artificial time stamp (if one is being used) to the
   * right value before a forecast is produced
   */
  protected String m_artificialTimeStartOffset = "0";

  /** holds overlay data (if present in the incoming data) */
  protected transient Instances m_overlayData;

  /**
   * holds the incoming data for either priming the forecaster or for rebuilding
   * the forecaster
   */
  protected transient Instances m_bufferedPrimeData;

  /** true if the forecaster is using overlay data */
  protected transient boolean m_isUsingOverlayData;

  /** the lag maker in use by the forecaster */
  protected transient TSLagMaker m_modelLagMaker;

  /** name of the time stamp attribute */
  protected transient String m_timeStampName = "";

  /** the fields that the forecaster is predicting */
  protected transient List<String> m_fieldsToForecast;

  /** True if the step has been reset */
  protected boolean m_isReset;

  /** True if the step has incoming streaming data */
  protected boolean m_isStreaming;

  /** Reusable data object for outputting forecast data */
  protected Data m_streamingData;

  /**
   * Set the base 64 encoded forecaster.
   *
   * @param encodedForecaster a base 64 encoded List<Object> containing the
   *          forecaster and header
   */
  @ProgrammaticProperty
  public void setEncodedForecaster(String encodedForecaster) {
    m_encodedForecaster = encodedForecaster;
  }

  /**
   * Gets the base 64 encoded forecaster
   *
   * @return a base 64 string encoding a List<Object> that contains the
   *         forecasting model and the header
   */
  public String getEncodedForecaster() {
    return m_encodedForecaster;
  }

  /**
   * Set the number of time steps to forecast beyond the end of the incoming
   * priming data. This will be ignored if the forecaster is using overlay data
   * as the number of instances for which overlay data is present (and targets
   * are missing) in the incoming data will determine how many forecasted values
   * are produced.
   *
   * @param n the number of steps to forecast.
   */
  @OptionMetadata(displayName = "Number of steps to forecast",
    description = "The number of steps to forecast beyond the end of the "
      + "incoming priming data. This will be ignored if the forecaster "
      + "is using overlay data, as the number of instances for which overlay "
      + "data is present (and targets are missing) in the incoming data "
      + "will determine how many forecasted values are produced",
    displayOrder = 0)
  public void setNumStepsToForecast(String n) {
    m_numberOfStepsToForecast = n;
  }

  /**
   * Get the number of time steps to forecast beyond the end of the incoming
   * priming data. This will be ignored if the forecaster is using overlay data
   * as the number of instances for which overlay data is present (and targets
   * are missing) in the incoming data will determine how many forecasted values
   * are produced.
   *
   * @return the number of steps to forecast.
   */
  public String getNumStepsToForecast() {
    return m_numberOfStepsToForecast;
  }

  /**
   * Set the offset, from the value associated with the last training instance,
   * for the artificial time stamp. Has no effect if an artificial time stamp is
   * not in use by the forecaster. If in use, this needs to be set so that the
   * forecaster knows what time stamp value corresponds to the first requested
   * forecast (i.e. it should be equal to the number of recent historical
   * priming instances that occur after the last training instance in time).
   *
   * @param art the offset from the last artificial time value in the training
   *          data for which the forecast is requested.
   */
  @OptionMetadata(displayName = "Artificial time start offset",
    description = "Set the offset, from the value associated with the last training "
      + "instance, for the artificial timestamp. Has no effect if an artificial "
      + "timestamp is not in use by the forecaster. If in use, this needs to be "
      + "set so that the forecaster knows what timestamp value corresponds to "
      + "the first requested forecast (i.e. it should be equal to the number of "
      + "recent historical priming instances that occur after the last "
      + "training instance in time",
    displayOrder = 1)
  public void setArtificialTimeStartOffset(String art) {
    m_artificialTimeStartOffset = art;
  }

  /**
   * Get the offset, from the value associated with the last training instance,
   * for the artificial time stamp. Has no effect if an artificial time stamp is
   * not in use by the forecaster. If in use, this needs to be set so that the
   * forecaster knows what time stamp value corresponds to the first requested
   * forecast (i.e. it should be equal to the number of recent historical
   * priming instances that occur after the last training instance in time).
   *
   * @return the offset from the last artificial time value in the training data
   *         for which the forecast is requested.
   */
  public String getArtificialTimeStartOffset() {
    return m_artificialTimeStartOffset;
  }

  /**
   * Set the filename to load from.
   *
   * @param filename the filename to load from
   */
  @FilePropertyMetadata(fileChooserDialogType = JFileChooser.OPEN_DIALOG,
    directoriesOnly = false)
  @ProgrammaticProperty
  @OptionMetadata(displayName = "File to load forecaster from",
    description = "File to load a forecaster from at runtime", displayOrder = 2)
  public void setFilename(File filename) {
    m_fileName = filename;
  }

  /**
   * Get the filename to load from.
   *
   * @return the filename to load from.
   */
  public File getFilename() {
    return m_fileName;
  }

  /**
   * Set the name of the file to save the forecasting model out to if the user
   * has opted to rebuild the forecaster using the incoming data.
   *
   * @param fileName the file name to save to.
   */
  @FilePropertyMetadata(fileChooserDialogType = JFileChooser.SAVE_DIALOG,
    directoriesOnly = false)
  @OptionMetadata(displayName = "File to save forecaster to",
    description = "File to save forecaster to (only applies when rebuilding forecaster)",
    displayOrder = 4)
  public void setSaveFilename(File fileName) {
    m_saveFileName = fileName;
  }

  /**
   * Get the name of the file to save the forecasting model to if the user has
   * opted to rebuild the forecaster using the incoming data.
   *
   * @return the name of the file to save the forecaster to.
   */
  public File getSaveFilename() {
    return m_saveFileName;
  }

  /**
   * Set whether the forecaster should be rebuilt/re-estimated on the incoming
   * data.
   *
   * @param rebuild true if the forecaster should be rebuilt using the incoming
   *          data
   */
  @OptionMetadata(displayName = "Rebuild forecaster",
    description = "Rebuild forecaster on incoming data", displayOrder = 3)
  public void setRebuildForecaster(boolean rebuild) {
    m_rebuildForecaster = rebuild;
  }

  /**
   * Get whether the forecaster will be rebuilt/re-estimated on the incoming
   * data.
   *
   * @return true if the forecaster is to be rebuilt on the incoming data
   */
  public boolean getRebuildForecaster() {
    return m_rebuildForecaster;
  }

  @Override
  public void stepInit() throws WekaException {
    if ((m_encodedForecaster == null || m_encodedForecaster.equals("-NONE-")) &&
      (m_fileName == null || isEmpty(m_fileName.toString()))) {
      throw new WekaException("No forecaster specified!");
    }

    m_isReset = true;
    m_isStreaming = false;
    m_overlayData = null;
    m_bufferedPrimeData = null;
    m_streamingData = new Data(StepManager.CON_INSTANCE);
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    Instance inst;
    Instances incomingStructure = null;
    boolean first = false;
    if (m_isReset) {
      m_isReset = false;
      loadOrDecodeForecaster();
      first = true;
      if (getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
        m_isStreaming = true;
        inst = data.getPrimaryPayload();
        incomingStructure = inst.dataset();
      } else {
        incomingStructure = data.getPrimaryPayload();
        incomingStructure = new Instances(incomingStructure, 0);
      }

      // check the structure of the incoming data
      if (!m_header.equalHeaders(incomingStructure)) {
        throw new WekaException(m_header.equalHeadersMsg(incomingStructure));
      }
      try {
        getStepManager().logBasic("Making output structure");
        // makeOutputStructure(incomingStructure);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    if (m_isStreaming) {
      if (getStepManager().isStreamFinished(data)) {
        try {
          processInstance(null, false); // finished
          generateForecast();
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
        m_streamingData.clearPayload();
        getStepManager().throughputFinished(m_streamingData);
        return;
      } else {
        processStreaming(data, first);
      }
    } else {
      processBatch(data);

      // we output streaming data
      m_streamingData.clearPayload();
      getStepManager().throughputFinished(m_streamingData);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (!m_isStreaming) {
      getStepManager().finished();
    }
  }

  protected void processStreaming(Data data, boolean first)
    throws WekaException {
    Instance toProcess = data.getPrimaryPayload();
    try {
      processInstance(toProcess, first);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected void processBatch(Data data) throws WekaException {

    try {
      processInstance(null, true);
      Instances toProcess = data.getPrimaryPayload();
      for (int i = 0; i < toProcess.numInstances(); i++) {
        processInstance(toProcess.instance(i), false);
      }

      processInstance(null, false); // finished

      generateForecast();
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected void processInstance(Instance toProcess, boolean first)
    throws Exception {
    getStepManager().throughputUpdateStart();
    if (first) {
      getStepManager().statusMessage("Configuring forecaster...");
      getStepManager().logBasic("Configuring forecaster.");

      m_modelLagMaker = m_forecaster.getTSLagMaker();
      if (!m_modelLagMaker.isUsingAnArtificialTimeIndex()
        && m_modelLagMaker.getAdjustForTrends()) {
        m_timeStampName = m_modelLagMaker.getTimeStampField();
      }

      m_isUsingOverlayData = m_forecaster.isUsingOverlayData();

      if (!m_rebuildForecaster) {
        getStepManager()
          .logBasic("Forecaster will be primed " + "incrementally.");

        // first reset lag histories

        m_forecaster.primeForecaster(new Instances(m_header, 0));
      } else {
        getStepManager().logBasic(
          "Forecaster will be rebuilt/re-estimated " + "on incoming data");
      }

      if (m_isUsingOverlayData) {
        getStepManager().logDetailed("Forecaster is using overlay data. "
          + "We expect to see overlay attribute values for the "
          + "forecasting period.");
        m_overlayData = new Instances(m_header, 0);
      }

      if (m_rebuildForecaster) {
        m_bufferedPrimeData = new Instances(m_header, 0);
      }

      m_fieldsToForecast =
        AbstractForecaster.stringToList(m_forecaster.getFieldsToForecast());

      m_outgoingStructure = new Instances(m_header);
      if (m_forecaster.isProducingConfidenceIntervals()) {
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        for (int i = 0; i < m_header.numAttributes(); i++) {
          atts.add((Attribute) m_header.attribute(i).copy());
        }
        for (String f : m_fieldsToForecast) {
          Attribute lb = new Attribute(f + "_lowerBound");
          Attribute ub = new Attribute(f + "_upperBound");
          atts.add(lb);
          atts.add(ub);
        }
        m_outgoingStructure = new Instances(
          m_header.relationName() + "_" + "plus_forecast", atts, 0);
      }
    } else if (toProcess == null) {
      // No more input. Rebuild forecaster if necessary
      if (m_rebuildForecaster && m_bufferedPrimeData.numInstances() > 0) {
        // push out historical data first
        for (int i = 0; i < m_bufferedPrimeData.numInstances(); i++) {
          m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
            m_bufferedPrimeData.instance(i));
          getStepManager().outputData(m_streamingData);
        }

        // rebuild the forecaster
        getStepManager().statusMessage("Rebuilding the forecasting model...");
        getStepManager().logBasic("Rebuilding the forecasting model");
        m_forecaster.buildForecaster(m_bufferedPrimeData);

        getStepManager().statusMessage("Priming the forecasting model...");
        getStepManager().logBasic("Priming the forecasting model");
      }

      if (m_rebuildForecaster && !isEmpty(m_saveFileName.toString())) {
        // save the forecaster
        getStepManager().statusMessage("Saving rebuilt forecasting model...");
        getStepManager().logBasic("Saving rebuilt forecasting model to \""
          + m_saveFileName.toString() + "\"");
        OutputStream os = new FileOutputStream(m_saveFileName);
        if (m_saveFileName.toString().endsWith(".gz")) {
          os = new GZIPOutputStream(os);
        }
        ObjectOutputStream oos =
          new ObjectOutputStream(new BufferedOutputStream(os));
        try {
          oos.writeObject(m_forecaster);
          oos.writeObject(m_header);
        } finally {
          oos.flush();
          oos.close();
        }
      }
    } else {
      // if we are expecting overlay data, then check this instance to see if
      // all
      // target values predicted by the forecaster are missing. If so, then this
      // *might* indicate the start of the overlay data. We will start buffering
      // instances into the overlay buffer. If we get an instace with all
      // non-missing targets
      // at some future point then we will flush the overlay buffer either into
      // the
      // forecaster as priming instances (if forecaster is incrementally
      // primeable)
      // or into the buffered prime/training data if forecaster is not
      // incrementally
      // primeable or we are rebuilding/re-estimating the model

      if (m_isUsingOverlayData) {
        boolean allMissing = true;
        for (String field : m_fieldsToForecast) {
          if (!toProcess.isMissing(m_header.attribute(field))) {
            allMissing = false;
            break;
          }
        }

        if (allMissing) {
          // add it to the overlay buffer
          m_overlayData.add(toProcess);
          getStepManager().statusMessage("buffering overlay instance...");
        } else {
          // check the overlay buffer - if it's not empty then flush it
          // into either the forecaster directly (if incrementally primeable)
          // or into the priming buffer
          if (m_overlayData.numInstances() > 0) {
            // first buffer this one (will get flushed anyway)
            m_overlayData.add(toProcess);
            getStepManager().logWarning("Encountered a supposed "
              + "overlay instance with non-missing target values - "
              + "converting buffered overlay data into "
              + (m_rebuildForecaster ? "training" : "priming") + " data...");
            getStepManager().statusMessage("Flushing overlay buffer.");

            for (int i = 0; i < m_overlayData.numInstances(); i++) {
              if (!m_rebuildForecaster) {
                m_forecaster
                  .primeForecasterIncremental(m_overlayData.instance(i));

                // output this instance immediately (make sure that we include
                // any attributes for confidence intervals - these will be
                // necessarily missing for historical instances)
                Instance outgoing =
                  convertToOutputFormat(m_overlayData.instance(i));
                m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
                  outgoing);
                getStepManager().outputData(m_streamingData);

              } else {
                // transfer to the priming buffer
                m_bufferedPrimeData.add(m_overlayData.instance(i));
              }
            }

            m_overlayData = new Instances(m_header, 0);
          } else {
            // not all missing and overlay buffer is empty then it's a priming
            // instance

            // either buffer it or send it directly to the forecaster (if
            // incrementally
            // primeable
            if (!m_rebuildForecaster) {
              m_forecaster.primeForecasterIncremental(toProcess);

              // output this instance immediately (make sure that we include
              // any attributes for confidence intervals - these will be
              // necessarily missing for historical instances)
              Instance outgoing = convertToOutputFormat(toProcess);
              m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
                outgoing);
              getStepManager().outputData(m_streamingData);
            } else {
              // buffer
              m_bufferedPrimeData.add(toProcess);
            }
          }
        }
      } else {
        if (!m_rebuildForecaster) {
          m_forecaster.primeForecasterIncremental(toProcess);

          // output this instance immediately (make sure that we include
          // any attributes for confidence intervals - these will be
          // necessarily missing for historical instances)
          Instance outgoing = convertToOutputFormat(toProcess);
          m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, outgoing);
          getStepManager().outputData(m_streamingData);
        } else {
          // buffer
          m_bufferedPrimeData.add(toProcess);
        }
      }
    }

    getStepManager().throughputUpdateEnd();
  }

  private Instance convertToOutputFormat(Instance incoming) {

    Instance output = (Instance) incoming.copy();
    if (m_forecaster.isProducingConfidenceIntervals()) {
      double[] values =
        new double[incoming.numAttributes() + (m_fieldsToForecast.size() * 2)];
      for (int i = 0; i < incoming.numAttributes(); i++) {
        values[i] = incoming.value(i);
      }

      // set all bounds to missing (initially)
      for (int i = incoming.numAttributes(); i < incoming.numAttributes()
        + (m_fieldsToForecast.size() * 2); i++) {
        values[i] = Utils.missingValue();
      }

      output = new DenseInstance(1.0, values);
    }
    output.setDataset(m_outgoingStructure);

    return output;
  }

  private void generateForecast() throws Exception {

    // doesn't matter if we're not using a time stamp
    double lastTimeFromPrime = -1;

    if (m_modelLagMaker.getAdjustForTrends()
      && m_modelLagMaker.getTimeStampField() != null
      && m_modelLagMaker.getTimeStampField().length() > 0
      && !m_modelLagMaker.isUsingAnArtificialTimeIndex()) {

      lastTimeFromPrime = m_modelLagMaker.getCurrentTimeStampValue();
    } else if (m_modelLagMaker.getAdjustForTrends()
      && m_modelLagMaker.isUsingAnArtificialTimeIndex()) {

      // If an artificial time stamp is in use then we need to set the
      // initial value to whatever offset from training that the user has
      // indicated to be the first forecasted point.

      String artOff = m_artificialTimeStartOffset;
      artOff = environmentSubstitute(artOff);

      double artificialStartValue =
        m_modelLagMaker.getArtificialTimeStartValue();
      artificialStartValue += Integer.parseInt(artOff);
      m_modelLagMaker.setArtificialTimeStartValue(artificialStartValue);
    }

    boolean overlay = (m_overlayData != null && m_overlayData.numInstances() > 0
      && m_isUsingOverlayData);

    String numS = m_numberOfStepsToForecast;
    numS = environmentSubstitute(numS);

    int numSteps =
      (overlay) ? m_overlayData.numInstances() : Integer.parseInt(numS);

    List<List<NumericPrediction>> forecast = null;

    // TODO adapt the log to PrintStream for the forecasting methods

    if (overlay) {
      forecast = m_forecaster.forecast(numSteps, m_overlayData);
    } else {
      forecast = m_forecaster.forecast(numSteps);
    }

    // now convert the forecast into instances. If we have overlay
    // data then we can just fill in the forecasted values (and
    // potentially add for confidence intervals)
    double time = lastTimeFromPrime;
    int timeStampIndex = -1;
    if (m_timeStampName.length() > 0) {
      Attribute timeStampAtt = m_outgoingStructure.attribute(m_timeStampName);
      if (timeStampAtt == null) {
        getStepManager().logError(
          "couldn't find time stamp: " + m_timeStampName + "in the input data",
          null);
      }
      timeStampIndex = timeStampAtt.index();
    }

    getStepManager().statusMessage("Generating forecast...");
    getStepManager().logBasic("Generating forecast.");
    for (int i = 0; i < numSteps; i++) {
      if (m_isStreaming) {
        getStepManager().throughputUpdateStart();
      }
      Instance outputI = null;
      double[] outVals = new double[m_outgoingStructure.numAttributes()];
      for (int j = 0; j < outVals.length; j++) {
        if (overlay) {
          outVals[j] = m_overlayData.instance(i).value(j);
        } else {
          outVals[j] = Utils.missingValue();
        }
      }
      List<NumericPrediction> predsForStep = forecast.get(i);

      if (timeStampIndex != -1) {
        // set time value
        time = m_modelLagMaker.advanceSuppliedTimeValue(time);
        outVals[timeStampIndex] = time;
      }

      for (int j = 0; j < m_fieldsToForecast.size(); j++) {
        String target = m_fieldsToForecast.get(j);
        int targetI = m_outgoingStructure.attribute(target).index();
        NumericPrediction predForTargetAtStep = predsForStep.get(j);
        double y = predForTargetAtStep.predicted();
        double yHigh = y;
        double yLow = y;
        double[][] conf = predForTargetAtStep.predictionIntervals();
        if (!Utils.isMissingValue(y)) {
          outVals[targetI] = y;
        }

        // any confidence bounds?
        if (conf.length > 0) {
          yLow = conf[0][0];
          yHigh = conf[0][1];
          int indexOfLow =
            m_outgoingStructure.attribute(target + "_lowerBound").index();
          int indexOfHigh =
            m_outgoingStructure.attribute(target + "_upperBound").index();
          outVals[indexOfLow] = yLow;
          outVals[indexOfHigh] = yHigh;
        }
      }
      outputI = new DenseInstance(1.0, outVals);
      outputI.setDataset(m_outgoingStructure);

      // notify listeners of output instance
      m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, outputI);
      getStepManager().outputData(m_streamingData);
      if (m_isStreaming) {
        getStepManager().throughputUpdateEnd();
      }
    }

    getStepManager()
      .logBasic("Finished. Generated " + numSteps + " forecasted values.");
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() == 0) {
      result.add(StepManager.CON_DATASET);
      result.add(StepManager.CON_INSTANCE);
    }
    return result;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
  }

  protected List<Object> loadModel(File filename) throws WekaException {

    List<Object> loaded = new ArrayList<Object>();
    try {
      if (!isEmpty(filename.toString())
        && !filename.toString().equals("-NONE-")) {

        String filenameN = filename.toString();
        filenameN = environmentSubstitute(filenameN);

        InputStream is = new FileInputStream(filenameN);
        if (filenameN.toLowerCase().endsWith(".gz")) {
          is = new GZIPInputStream(is);
        }
        ObjectInputStream ois = SerializationHelper.getObjectInputStream(is);
        WekaForecaster forecaster = (WekaForecaster) ois.readObject();

        Instances header = (Instances) ois.readObject();
        is.close();

        loaded.add(forecaster);
        loaded.add(header);
        return loaded;
      } else {
        throw new WekaException(
          "Model is null or no filename specified to load from!");
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Get the forecaster. Loads the forecaster from a file (if necessary).
   *
   * @return the forecasting model
   * @throws Exception if there is a problem loading the forecaster
   */
  public WekaForecaster getForecaster() throws Exception {
    if (m_forecaster != null) {
      return m_forecaster;
    } else {

      // try and decode the base64 string (if set)
      List<Object> model = getForecaster(m_encodedForecaster);
      if (model != null) {
        m_forecaster = (WekaForecaster) model.get(0);
        m_header = (Instances) model.get(1);
        return m_forecaster;
      }
    }

    return null;
  }

  private void loadOrDecodeForecaster() throws WekaException {
    // filename takes precedence over encoded forecaster (if any)
    if (!isEmpty(m_fileName.toString())) {
      List<Object> loaded = loadModel(m_fileName);
      if (loaded == null) {
        throw new WekaException("problem loading forecasting model.");
      } else {
        m_forecaster = (WekaForecaster) loaded.get(0);
        m_header = (Instances) loaded.get(1);
      }
    } else if (m_encodedForecaster != null && m_encodedForecaster.length() > 0
      && !m_encodedForecaster.equals("-NONE-")) {
      try {
        getForecaster();
      } catch (Exception ex) {
        throw new WekaException("a problem occurred while decoding the model.",
          ex);
      }
    } else {
      throw new WekaException("unable to obtain a forecasting model to use.");
    }
  }

  /**
   * Decodes and returns a forecasting model (list containing the forecaster and
   * Instances object containing the structure of the data used to train the
   * forecaster) from a base 64 string.
   *
   * @param base64encoded a List<Object> containing forecaster and header
   *          encoded as a base 64 string
   *
   * @return the decoded List<Object> containing forecaster and header
   * @throws Exception if there is a problem decoding
   */
  @SuppressWarnings("unchecked")
  public static List<Object> getForecaster(String base64encoded)
    throws Exception {
    if (base64encoded != null && base64encoded.length() > 0
      && !base64encoded.equals("-NONE-")) {

      byte[] decoded = decodeFromBase64(base64encoded);
      ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
      // ObjectInputStream ois = new ObjectInputStream(bis);
      ObjectInputStream ois = SerializationHelper.getObjectInputStream(bis);

      List<Object> model = (List<Object>) ois.readObject();
      ois.close();
      return model;
    }

    return null;
  }

  /**
   * Decodes a base 64 encoded string to a byte array.
   *
   * @param string the base 64 encoded string
   * @return the decoded bytes
   * @throws Exception if a problem occurs
   */
  protected static byte[] decodeFromBase64(String string) throws Exception {

    byte[] bytes;
    if (string == null) {
      bytes = new byte[] {};
    } else {
      bytes = Base64.decodeBase64(string.getBytes());
    }
    if (bytes.length > 0) {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      GZIPInputStream gzip = new GZIPInputStream(bais);
      BufferedInputStream bi = new BufferedInputStream(gzip);
      byte[] result = new byte[] {};

      byte[] extra = new byte[1000000];
      int nrExtra = bi.read(extra);
      while (nrExtra >= 0) {
        // add it to bytes...
        //
        int newSize = result.length + nrExtra;
        byte[] tmp = new byte[newSize];
        for (int i = 0; i < result.length; i++)
          tmp[i] = result[i];
        for (int i = 0; i < nrExtra; i++)
          tmp[result.length + i] = extra[i];

        // change the result
        result = tmp;
        nrExtra = bi.read(extra);
      }
      bytes = result;
      gzip.close();
    }

    return bytes;
  }

  /**
   * Encode a byte array to a base 64 string
   *
   * @param val the byte array to encode
   * @return a base 64 encoded string
   * @throws IOException if a problem occurs during encoding
   */
  protected static String encodeToBase64(byte[] val) throws IOException {
    String string;
    if (val == null) {
      string = null;
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzos = new GZIPOutputStream(baos);
      BufferedOutputStream bos = new BufferedOutputStream(gzos);
      bos.write(val);
      bos.flush();
      bos.close();

      string = new String(Base64.encodeBase64(baos.toByteArray()));
    }
    return string;
  }

  /**
   * Encode the model and header into a base 64 string. A List
   * <Object> containing first the model and then the header is encoded.
   *
   * @param model the forecasting model to encode
   * @param header empty instances object containing just the structure of the
   *          data used to train the forecaster
   * @return a base 64 encoded String
   * @throws Exception if a problem occurs.
   */
  public static String encodeForecasterToBase64(WekaForecaster model,
    Instances header) throws Exception {

    if (model != null && header != null) {
      List<Object> modelAndHeader = new ArrayList<Object>();
      modelAndHeader.add(model);
      modelAndHeader.add(header);

      ByteArrayOutputStream bao = new ByteArrayOutputStream();
      BufferedOutputStream bos = new BufferedOutputStream(bao);
      ObjectOutputStream oo = new ObjectOutputStream(bos);
      oo.writeObject(modelAndHeader);
      oo.flush();

      byte[] modelBytes = bao.toByteArray();

      return encodeToBase64(modelBytes);
    } else {
      throw new Exception("[TimeSeriesForecasting] unable to encode model!");
    }

  }

  /**
   * Utility method to check if a String is null or empty ("").
   *
   * @param aString the String to check.
   * @return true if the supplied String is null or empty.
   */
  public static boolean isEmpty(String aString) {
    if (aString == null || aString.length() == 0) {
      return true;
    }
    return false;
  }

  /**
   * Get the fully qualified name of the GUI editor for this step
   *
   * @return the fully qualified name of the editor for this step
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.TimeSeriesForecastingStepEditorDialog";
  }
}
