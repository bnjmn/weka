package weka.knowledgeflow.steps;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.WekaException;
import weka.core.converters.FileSourcedConverter;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.StreamThroughput;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@KFStep(name = "Loader", category = "DataSources",
  toolTipText = "Weka loader wrapper", iconPath = "")
public class Loader extends WekaAlgorithmWrapper implements Serializable {

  private static final long serialVersionUID = -788869066035779154L;

  /**
   * Global info for the wrapped loader (if it exists).
   */
  protected String m_globalInfo;

  /** True if we're going to be streaming instance objects */
  protected boolean m_instanceGeneration;

  /** True if there are no outgoing connections */
  protected boolean m_noOutputs;

  /** Reusable data container */
  protected Data m_instanceData;

  /** For measuring the overall flow throughput */
  protected StreamThroughput m_flowThroughput;

  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.core.converters.Loader.class;
  }

  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultDataSource.gif";
  }

  public weka.core.converters.Loader getLoader() {
    return (weka.core.converters.Loader) getWrappedAlgorithm();
  }

  @ProgrammaticProperty
  public void setLoader(weka.core.converters.Loader loader) {
    setWrappedAlgorithm(loader);
  }

  @Override
  public void stepInit() throws WekaException {

    if (!(getWrappedAlgorithm() instanceof weka.core.converters.Loader)) {
      throw new WekaException("Incorrect type of algorithm");
    }

    int numDatasetOutputs =
      getStepManager().numOutgoingConnectionsOfType(StepManager.CON_DATASET);
    int numInstanceOutputs =
      getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE);

    m_noOutputs = numInstanceOutputs == 0 && numDatasetOutputs == 0;

    if (numDatasetOutputs > 0 && numInstanceOutputs > 0) {
      throw new WekaException(
        "Can't have both instance and dataSet outgoing connections!");
    }

    if (getWrappedAlgorithm() instanceof EnvironmentHandler) {
      ((EnvironmentHandler) getWrappedAlgorithm())
        .setEnvironment(getStepManager().getExecutionEnvironment()
          .getEnvironmentVariables());
    }

    m_instanceGeneration = numInstanceOutputs > 0;
    m_instanceData = new Data(StepManager.CON_INSTANCE);
  }

  @Override
  public void start() throws WekaException {
    if (m_noOutputs) {
      return;
    }
    getStepManager().processing();

    weka.core.converters.Loader theLoader =
      (weka.core.converters.Loader) getWrappedAlgorithm();

    String startMessage =
      (theLoader instanceof FileSourcedConverter) ? "Loading "
        + ((FileSourcedConverter) theLoader).retrieveFile().getName()
        : "Loading...";

    getStepManager().logBasic(startMessage);
    getStepManager().statusMessage(startMessage);

    if (!m_instanceGeneration) {
      try {
        theLoader.reset();
        theLoader.setRetrieval(weka.core.converters.Loader.BATCH);
        Instances dataset = theLoader.getDataSet();
        getStepManager().logBasic("Loaded " + dataset.relationName());
        Data data = new Data();
        data.setPayloadElement(StepManager.CON_DATASET, dataset);
        getStepManager().outputData(StepManager.CON_DATASET, data);
      } catch (Exception ex) {
        throw new WekaException(ex);
      } finally {
        getStepManager().finished();
      }
    } else {
      String stm =
        getName() + "$" + hashCode() + 99 + "| overall flow throughput -|";
      m_flowThroughput =
        new StreamThroughput(stm, "Starting flow...",
          ((StepManagerImpl) getStepManager()).getLog());

      Instance nextInstance = null;
      Instances structure = null;
      Instances structureCopy = null;
      Instances currentStructure = null;
      boolean stringAttsPresent = false;

      try {
        theLoader.reset();
        theLoader.setRetrieval(weka.core.converters.Loader.INCREMENTAL);
        structure = theLoader.getStructure();
        if (structure.checkForStringAttributes()) {
          structureCopy =
            (Instances) (new SerializedObject(structure).getObject());
          stringAttsPresent = true;
        }
        currentStructure = structure;
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      if (isStopRequested()) {
        return;
      }

      try {
        nextInstance = theLoader.getNextInstance(structure);
      } catch (Exception ex) {
        // getStepManager().throughputFinished(m_instanceData);
        throw new WekaException(ex);
      }

      while (!isStopRequested() && nextInstance != null) {
        m_flowThroughput.updateStart();
        getStepManager().throughputUpdateStart();

        if (stringAttsPresent) {
          if (currentStructure == structure) {
            currentStructure = structureCopy;
          } else {
            currentStructure = structure;
          }
        }

        m_instanceData
          .setPayloadElement(StepManager.CON_INSTANCE, nextInstance);

        try {
          nextInstance = theLoader.getNextInstance(currentStructure);
        } catch (Exception ex) {
          getStepManager().throughputFinished(m_instanceData);
          throw new WekaException(ex);
        }
        getStepManager().throughputUpdateEnd(); // finished read operation
        getStepManager().outputData(StepManager.CON_INSTANCE, m_instanceData);

        m_flowThroughput.updateEnd(((StepManagerImpl) getStepManager())
          .getLog());
      }

      if (isStopRequested()) {
        ((StepManagerImpl) getStepManager()).getLog().statusMessage(
          stm + "remove");
        return;
      }
      m_flowThroughput.finished(((StepManagerImpl) getStepManager()).getLog());

      // signal end of input
      m_instanceData.clearPayload();
      getStepManager().throughputFinished(m_instanceData);
      // int flowSpeed = m_flowThroughput.getAverageInstancesPerSecond();
      // String finalMessage += ("" + flowSpeed +
      // " insts/sec (flow throughput)");
    }
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    // can't reset the laoder to get the structure if we're actively
    // processing...
    if (getStepManager().isStepBusy()) {
      return null;
    }

    try {
      weka.core.converters.Loader theLoader =
        (weka.core.converters.Loader) getWrappedAlgorithm();
      theLoader.reset();
      if (theLoader instanceof EnvironmentHandler) {
        ((EnvironmentHandler) theLoader).setEnvironment(Environment
          .getSystemWide());
      }
      return theLoader.getStructure();
    } catch (Exception ex) {
      getStepManager().logError(ex.getMessage(), ex);
    }

    return null;
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    // doesn't accept incoming connections
    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> outgoing = new ArrayList<String>();
    int numDatasetOutputs =
      getStepManager().numOutgoingConnectionsOfType(StepManager.CON_DATASET);
    int numInstanceOutputs =
      getStepManager().numOutgoingConnectionsOfType(StepManager.CON_INSTANCE);

    if (numDatasetOutputs == 0 && numInstanceOutputs == 0) {
      outgoing.add(StepManager.CON_DATASET);
      outgoing.add(StepManager.CON_INSTANCE);
    } else if (numDatasetOutputs > 0) {
      outgoing.add(StepManager.CON_DATASET);
    } else if (numInstanceOutputs > 0) {
      outgoing.add(StepManager.CON_INSTANCE);
    }

    return outgoing;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.LoaderStepEditorDialog";
  }
}
