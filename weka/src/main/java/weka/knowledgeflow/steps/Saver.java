package weka.knowledgeflow.steps;

import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.converters.DatabaseConverter;
import weka.core.converters.DatabaseSaver;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.ExecutionResult;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.StepTask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

@KFStep(name = "Saver", category = "DataSinks",
  toolTipText = "Weka saver wrapper", iconPath = "")
public class Saver extends WekaAlgorithmWrapper implements Serializable {

  private static final long serialVersionUID = 6831606284211403465L;

  /**
   * Holds the structure
   */
  protected Instances m_structure;

  /** The actual saver instance to use */
  protected weka.core.converters.Saver m_saver;

  /** True if the saver is a DatabaseSaver */
  protected boolean m_isDBSaver;

  protected transient Future<ExecutionResult<Void>> m_future;

  /**
   * For file-based savers - if true (default), relation name is used as the
   * primary part of the filename. If false, then the prefix is used as the
   * filename. Useful for preventing filenames from getting too long when there
   * are many filters in a flow.
   */
  private boolean m_relationNameForFilename = true;

  /** True if we've be reset */
  protected boolean m_isReset;

  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.core.converters.Saver.class;
  }

  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultDataSink.gif";
  }

  public weka.core.converters.Saver getSaver() {
    return (weka.core.converters.Saver) getWrappedAlgorithm();
  }

  @ProgrammaticProperty
  public void setSaver(weka.core.converters.Saver saver) {
    setWrappedAlgorithm(saver);
  }

  /**
   * Get whether the relation name is the primary part of the filename.
   *
   * @return true if the relation name is part of the filename.
   */
  public boolean getRelationNameForFilename() {
    return m_relationNameForFilename;
  }

  /**
   * Set whether to use the relation name as the primary part of the filename.
   * If false, then the prefix becomes the filename.
   *
   * @param r true if the relation name is to be part of the filename.
   */
  public void setRelationNameForFilename(boolean r) {
    m_relationNameForFilename = r;
  }

  @Override
  public void stepInit() throws WekaException {
    m_saver = null;

    if (!(getWrappedAlgorithm() instanceof weka.core.converters.Saver)) {
      throw new WekaException("Incorrect type of algorithm");
    }

    if (getWrappedAlgorithm() instanceof DatabaseConverter) {
      m_isDBSaver = true;
    }

    int numNonInstanceInputs =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET)
        + getStepManager().numIncomingConnectionsOfType(
          StepManager.CON_TRAININGSET)
        + getStepManager()
          .numIncomingConnectionsOfType(StepManager.CON_TESTSET);

    int numInstanceInput =
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE);

    if (numNonInstanceInputs > 0 && numInstanceInput > 0) {
      WekaException cause =
        new WekaException(
          "Can't have both instance and batch-based incomming connections!");
      cause.fillInStackTrace();
      getStepManager().logError(cause.getMessage(), cause);
      throw new WekaException(cause);
    }

    // m_incrementalSaving = numInstanceInput > 0;
  }

  @Override
  public void stop() {
    super.stop();
    if (m_future != null) {
      m_future.cancel(true);
    }
  }

  protected void saveBatch(Instances data, Integer setNum, Integer maxSetNum,
    String connectionName) throws WekaException {
    getStepManager().processing();

    try {
      m_saver =
        (weka.core.converters.Saver) new SerializedObject(getWrappedAlgorithm())
          .getObject();

      String fileName = sanitizeFilename(data.relationName());

      String additional =
        setNum != null && (setNum + maxSetNum != 2) ? "_" + connectionName
          + "_" + setNum + "_of_" + maxSetNum : "";

      if (!m_isDBSaver) {
        m_saver.setDirAndPrefix(fileName, additional);
      } else {
        ((DatabaseSaver) m_saver).setTableName(fileName);
        ((DatabaseSaver) m_saver).setRelationForTableName(false);
        String setName = ((DatabaseSaver) m_saver).getTableName();
        setName =
          setName.replaceFirst("_" + connectionName + "_[0-9]+_of_[0-9]+", "");
        ((DatabaseSaver) m_saver).setTableName(setName + additional);
      }
      m_saver.setInstances(data);

      getStepManager().logBasic("Saving " + data.relationName() + additional);
      getStepManager().statusMessage(
        "Saving " + data.relationName() + additional);

      BatchSaverTask b = new BatchSaverTask(this, m_saver);
      m_future = getStepManager().getExecutionEnvironment().submitTask(b);

      ExecutionResult<Void> result = m_future.get();
      if (result != null && result.getError() != null) {
        result.getError().printStackTrace();
        throw result.getError();
      }

      if (!isStopRequested()) {
        getStepManager().statusMessage("Finished.");
        getStepManager().logBasic("Save successful");
      } else {
        getStepManager().interrupted();
      }
    } catch (Exception ex) {
      WekaException e = new WekaException(ex);
      // e.printStackTrace();
      throw e;
    } finally {
      getStepManager().finished();
    }
  }

  @Override
  public synchronized void processIncoming(Data data) throws WekaException {

    if (m_saver == null) {
      try {
        m_saver =
          (weka.core.converters.Saver) new SerializedObject(
            getWrappedAlgorithm()).getObject();

        if (m_saver instanceof EnvironmentHandler) {
          ((EnvironmentHandler) m_saver).setEnvironment(getStepManager()
            .getExecutionEnvironment().getEnvironmentVariables());
        }

        if (data.getConnectionName().equals(StepManager.CON_DATASET)
          || data.getConnectionName().equals(StepManager.CON_TRAININGSET)
          || data.getConnectionName().equals(StepManager.CON_TESTSET)) {
          m_saver.setRetrieval(weka.core.converters.Saver.BATCH);
          Instances theData =
            (Instances) data.getPayloadElement(data.getConnectionName());
          Integer setNum =
            (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
          Integer maxSetNum =
            (Integer) data
              .getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);

          saveBatch(theData, setNum, maxSetNum, data.getConnectionName());

          return;
        } else if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          Instance forStructure =
            (Instance) data.getPayloadElement(StepManager.CON_INSTANCE);
          if (forStructure != null) {
            // processing();
            m_saver.setRetrieval(weka.core.converters.Saver.INCREMENTAL);
            String fileName =
              sanitizeFilename(forStructure.dataset().relationName());
            m_saver.setDirAndPrefix(fileName, "");
            m_saver.setInstances(forStructure.dataset());

            if (m_isDBSaver) {
              if (((DatabaseSaver) m_saver).getRelationForTableName()) {
                ((DatabaseSaver) m_saver).setTableName(fileName);
                ((DatabaseSaver) m_saver).setRelationForTableName(false);
              }
            }
          }
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    // incremental saving
    Instance toSave =
      (Instance) data.getPayloadElement(StepManager.CON_INSTANCE);
    boolean streamEnd = getStepManager().isStreamFinished(data);
    try {
      if (streamEnd) {
        m_saver.writeIncremental(null);
        getStepManager().throughputFinished(
          new Data(StepManagerImpl.CON_INSTANCE));
        return;
      }

      if (!isStopRequested()) {
          getStepManager().throughputUpdateStart();
        m_saver.writeIncremental(toSave);
      } else {
        // make sure that saver finishes and closes file
        m_saver.writeIncremental(null);
      }
      getStepManager().throughputUpdateEnd();
    } catch (Exception ex) {
      // getStepManager().throughputFinished();
      throw new WekaException(ex);
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {

    int numInstance =
      getStepManager().getIncomingConnectedStepsOfConnectionType(
        StepManager.CON_INSTANCE).size();

    int numNonInstance =
      getStepManager().getIncomingConnectedStepsOfConnectionType(
        StepManager.CON_DATASET).size()
        + getStepManager().getIncomingConnectedStepsOfConnectionType(
          StepManager.CON_TRAININGSET).size()
        + getStepManager().getIncomingConnectedStepsOfConnectionType(
          StepManager.CON_TESTSET).size();

    if (numInstance + numNonInstance == 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET,
        StepManager.CON_INSTANCE);
    }

    return new ArrayList<String>();
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    // no outgoing connections
    return new ArrayList<String>();
  }

  /**
   * makes sure that the filename is valid, i.e., replaces slashes, backslashes
   * and colons with underscores ("_"). Also try to prevent filename from
   * becoming insanely long by removing package part of class names.
   * 
   * @param filename the filename to cleanse
   * @return the cleansed filename
   */
  protected String sanitizeFilename(String filename) {
    filename =
      filename.replaceAll("\\\\", "_").replaceAll(":", "_")
        .replaceAll("/", "_");
    filename =
      Utils.removeSubstring(filename, "weka.filters.supervised.instance.");
    filename =
      Utils.removeSubstring(filename, "weka.filters.supervised.attribute.");
    filename =
      Utils.removeSubstring(filename, "weka.filters.unsupervised.instance.");
    filename =
      Utils.removeSubstring(filename, "weka.filters.unsupervised.attribute.");
    filename = Utils.removeSubstring(filename, "weka.clusterers.");
    filename = Utils.removeSubstring(filename, "weka.associations.");
    filename = Utils.removeSubstring(filename, "weka.attributeSelection.");
    filename = Utils.removeSubstring(filename, "weka.estimators.");
    filename = Utils.removeSubstring(filename, "weka.datagenerators.");

    if (!m_isDBSaver && !m_relationNameForFilename) {
      filename = "";
      try {
        if (m_saver.filePrefix().equals("")) {
          m_saver.setFilePrefix("no-name");
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return filename;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SaverStepEditorDialog";
  }

  protected static class BatchSaverTask extends StepTask<Void> implements
    Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 1L;

    protected weka.core.converters.Saver m_saver;

    public BatchSaverTask(Step source, weka.core.converters.Saver saver) {
      super(source);
      m_saver = saver;
    }

    @Override
    public void process() throws Exception {

      try {
        m_saver.writeBatch();
      } catch (Exception ex) {
        m_result.setError(ex);
      }
    }
  }
}
