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
 *    SerializedModelSaver.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.UpdateableBatchProcessor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Step that can save models encapsulated in incoming {@code Data} objects to
 * the filesystem.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SerializedModelSaver", category = "DataSinks",
  toolTipText = "A step that saves models to the file system",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "SerializedModelSaver.gif")
public class SerializedModelSaver extends BaseStep {

  private static final long serialVersionUID = -8343162241983197708L;

  /** Stores the header of data used to build an incremental model */
  protected Instances m_incrementalHeader;

  /**
   * How often to save an incremental classifier (<= 0 means only at the end of
   * the stream)
   */
  protected int m_incrementalSaveSchedule;

  /**
   * Whether to include the relation name of the data in the file name for the
   * model
   */
  protected boolean m_includeRelationName;

  /**
   * The prefix for the file name (model + training set info will be appended)
   */
  private String m_filenamePrefix = "";

  /**
   * The directory to hold the saved model(s)
   */
  private File m_directory = new File(System.getProperty("user.dir"));

  /** Counter for use when processing incremental classifier connections */
  protected int m_counter;

  /**
   * Set the directory to save to
   *
   * @param directory the directory to save to
   */
  @FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.SAVE_DIALOG,
    directoriesOnly = true)
  @OptionMetadata(displayName = "Output directory",
    description = "The directory to save models to", displayOrder = 0)
  public void setOutputDirectory(File directory) {
    m_directory = directory;
  }

  /**
   * Get the directory to save to
   *
   * @return the directory to save to
   */
  public File getOutputDirectory() {
    return m_directory;
  }

  /**
   * Set the text to prepend to the filename
   *
   * @param filenamePrefix the prefix to add to the filename
   */
  @OptionMetadata(displayName = "Filename prefix",
    description = "A prefix to prepend to the filename", displayOrder = 1)
  public void setFilenamePrefix(String filenamePrefix) {
    m_filenamePrefix = filenamePrefix;
  }

  /**
   * Get the text to prepend to the filename
   *
   * @return the prefix to add to the filename
   */
  public String getFilenamePrefix() {
    return m_filenamePrefix;
  }

  /**
   * Set how frequently to save an incremental model
   *
   * @param schedule how often (i.e. every x updates) to save the model. <= 0
   *          indicates that the save will happen just once, at the end of the
   *          stream.
   */
  @OptionMetadata(displayName = "Incremental save schedule",
    description = "How frequently to save incremental classifiers ("
      + "<= 0 indicates that the save will happen just once, at the "
      + "end of the stream", displayOrder = 4)
  public void setIncrementalSaveSchedule(int schedule) {
    m_incrementalSaveSchedule = schedule;
  }

  /**
   * Get how frequently to save an incremental model
   *
   * @return how often (i.e. every x updates) to save the model. <= 0 indicates
   *         that the save will happen just once, at the end of the stream.
   */
  public int getIncrementalSaveSchedule() {
    return m_incrementalSaveSchedule;
  }

  /**
   * Set whether to include the relation name as part of the filename
   *
   * @param includeRelationName true to include the relation name as part of the
   *          filename
   */
  @OptionMetadata(
    displayName = "Include relation name in file name",
    description = "Whether to include the relation name of the data as part of the "
      + "file name", displayOrder = 2)
  public
    void setIncludeRelationNameInFilename(boolean includeRelationName) {
    m_includeRelationName = includeRelationName;
  }

  /**
   * Get whether to include the relation name as part of the filename
   *
   * @return true if the relation name will be included as part of the filename
   */
  public boolean getIncludeRelationNameInFilename() {
    return m_includeRelationName;
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
    result.add(StepManager.CON_BATCH_CLASSIFIER);
    result.add(StepManager.CON_INCREMENTAL_CLASSIFIER);
    result.add(StepManager.CON_BATCH_CLUSTERER);
    result.add(StepManager.CON_BATCH_ASSOCIATOR);

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
    return new ArrayList<String>();
  }

  /**
   * Initialize the step
   */
  @Override
  public void stepInit() {
    m_incrementalHeader = null;
    m_counter = 0;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Object modelToSave = null;
    Instances modelHeader = null;
    Integer setNum = null;
    Integer maxSetNum = null;

    if (data.getConnectionName().equals(StepManager.CON_INCREMENTAL_CLASSIFIER)) {
      if (m_incrementalHeader == null
        && !getStepManager().isStreamFinished(data)) {
        m_incrementalHeader =
          ((Instance) data
            .getPayloadElement(StepManager.CON_AUX_DATA_TEST_INSTANCE))
            .dataset();
      }
      if (getStepManager().isStreamFinished(data)
        || (m_incrementalSaveSchedule > 0
          && m_counter % m_incrementalSaveSchedule == 0 && m_counter > 0)) {
        modelToSave =
          (weka.classifiers.Classifier) data
            .getPayloadElement(StepManager.CON_INCREMENTAL_CLASSIFIER);
        modelHeader = m_incrementalHeader;
      }
    } else {
      modelToSave = data.getPayloadElement(data.getConnectionName());
      modelHeader =
        (Instances) data
          .getPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET);
      setNum =
        (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
      maxSetNum =
        (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
      if (modelHeader == null) {
        modelHeader =
          (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TESTSET);
      }
    }

    if (modelToSave != null) {
      if (modelToSave instanceof UpdateableBatchProcessor) {
        try {
          // make sure model cleans up before saving
          ((UpdateableBatchProcessor) modelToSave).batchFinished();
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }

      if (modelHeader != null) {
        modelHeader = new Instances(modelHeader, 0);
      }

      getStepManager().processing();
      String prefix = getStepManager().environmentSubstitute(m_filenamePrefix);
      String relationName =
        m_includeRelationName && modelHeader != null ? modelHeader
          .relationName() : "";
      String setSpec =
        maxSetNum != null && setNum != null ? "_" + setNum + "_" + maxSetNum
          + "_" : "";

      String modelName = modelToSave.getClass().getCanonicalName();
      modelName =
        modelName.substring(modelName.lastIndexOf(".") + 1, modelName.length());
      String filename = "" + prefix + relationName + setSpec + modelName;
      filename = sanitizeFilename(filename);

      String dirName =
        getStepManager().environmentSubstitute(m_directory.toString());
      File tempFile = new File(dirName);
      filename = tempFile.getAbsolutePath() + File.separator + filename;

      getStepManager().logBasic(
        "Saving model " + modelToSave.getClass().getCanonicalName() + " to "
          + filename + ".model");
      getStepManager().statusMessage(
        "Saving model: " + modelToSave.getClass().getCanonicalName());

      ObjectOutputStream oos = null;
      try {
        oos =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
            new File(filename + ".model"))));
        oos.writeObject(modelToSave);
        if (modelHeader != null) {
          oos.writeObject(modelHeader);
        }
        oos.close();
      } catch (Exception ex) {
        throw new WekaException(ex);
      } finally {
        if (data.getConnectionName() != StepManager.CON_INCREMENTAL_CLASSIFIER
          || getStepManager().isStreamFinished(data)) {
          getStepManager().finished();
        }
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }
      }
    }

    m_counter++;
  }

  /**
   * makes sure that the filename is valid, i.e., replaces slashes, backslashes
   * and colons with underscores ("_").
   *
   * @param filename the filename to cleanse
   * @return the cleansed filename
   */
  protected static String sanitizeFilename(String filename) {
    return filename.replaceAll("\\\\", "_").replaceAll(":", "_")
      .replaceAll("/", "_");
  }
}
