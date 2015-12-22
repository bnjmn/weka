package weka.knowledgeflow.steps;

import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.filters.unsupervised.attribute.Add;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.SubstringLabelerRules;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SubstringLabeler", category = "Tools",
  toolTipText = "Label instances according to substring matches in String "
    + "attributes", iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultFilter.gif")
public class SubstringLabeler extends BaseStep {

  private static final long serialVersionUID = 1409175779108600014L;

  /** Internally encoded list of match rules */
  protected String m_matchDetails = "";

  /** Encapsulates our match rules */
  protected transient SubstringLabelerRules m_matches;

  /**
   * Whether to make the binary match/non-match attribute a nominal (rather than
   * numeric) binary attribute.
   */
  protected boolean m_nominalBinary;

  /**
   * For multi-valued labeled rules, whether or not to consume non-matching
   * instances or output them with missing value for the match attribute.
   */
  protected boolean m_consumeNonMatchingInstances;

  /** Add filter for adding the new attribute */
  protected Add m_addFilter;

  /** Name of the new attribute */
  protected String m_attName = "Match";

  /** Step has been reset - i.e. start of processing? */
  protected boolean m_isReset;

  /** Reusable data object for output */
  protected Data m_streamingData;

  /** Streaming instances? */
  protected boolean m_streaming;

  /**
   * Set internally encoded list of match rules
   *
   * @param details the list of match rules
   */
  @ProgrammaticProperty
  public void setMatchDetails(String details) {
    m_matchDetails = details;
  }

  /**
   * Get the internally encoded list of match rules
   *
   * @return the match rules
   */
  public String getMatchDetails() {
    return m_matchDetails;
  }

  /**
   * Set whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @param nom true if the attribute should be a nominal binary one
   */
  @OptionMetadata(displayName = "Make a nominal binary attribute",
    description = "Whether to encode the new attribute as nominal "
      + "when it is binary (as opposed to numeric)", displayOrder = 1)
  public void setNominalBinary(boolean nom) {
    m_nominalBinary = nom;
  }

  /**
   * Get whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @return true if the attribute should be a nominal binary one
   */
  public boolean getNominalBinary() {
    return m_nominalBinary;
  }

  /**
   * Set whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @param consume true if non matching instances should be consumed by the
   *          component.
   */
  @OptionMetadata(displayName = "Consume non matching instances",
    description = "Instances that do not match any rules will be consumed, "
      + "rather than being output with a missing value for the new attribute",
    displayOrder = 2)
  public void setConsumeNonMatching(boolean consume) {
    m_consumeNonMatchingInstances = consume;
  }

  /**
   * Get whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @return true if non matching instances should be consumed by the component.
   */
  public boolean getConsumeNonMatching() {
    return m_consumeNonMatchingInstances;
  }

  /**
   * Set the name of the new attribute that is created to indicate the match
   *
   * @param name the name of the new attribute
   */
  @OptionMetadata(displayName = "Name of the new attribute",
    description = "Name to give the new attribute", displayOrder = 0)
  public void setMatchAttributeName(String name) {
    m_attName = name;
  }

  /**
   * Get the name of the new attribute that is created to indicate the match
   *
   * @return the name of the new attribute
   */
  public String getMatchAttributeName() {
    return m_attName;
  }

  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_streamingData = new Data(StepManager.CON_INSTANCE);
    m_streaming = false;
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return getStepManager().numIncomingConnections() == 0 ? Arrays.asList(
      StepManager.CON_INSTANCE, StepManager.CON_DATASET,
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET) : null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    for (Map.Entry<String, List<StepManager>> e : getStepManager()
      .getIncomingConnections().entrySet()) {
      if (e.getValue().size() > 0) {
        result.add(e.getKey());
      }
    }

    return result;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    Instances structure;
    Instance inst;
    if (m_isReset) {
      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0) {
        inst = data.getPrimaryPayload();
        structure = inst.dataset();
        m_streaming = true;
      } else {
        structure = data.getPrimaryPayload();
        structure = new Instances(structure, 0);
      }
      try {
        m_matches =
          new SubstringLabelerRules(m_matchDetails, m_attName,
            getConsumeNonMatching(), getNominalBinary(), structure,
            ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix(),
            getStepManager().getLog(), getStepManager()
              .getExecutionEnvironment().getEnvironmentVariables());
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      m_isReset = false;
    }

    if (m_streaming) {
      if (getStepManager().isStreamFinished(data)) {
        m_streamingData.clearPayload();
        getStepManager().throughputFinished(m_streamingData);
        return;
      } else {
        processStreaming(data);
      }
    } else {
      processBatch(data);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (!m_streaming) {
      getStepManager().finished();
    }
  }

  protected void processStreaming(Data data) throws WekaException {
    getStepManager().throughputUpdateStart();
    Instance toProcess = data.getPrimaryPayload();
    try {
      Instance result = m_matches.makeOutputInstance(toProcess, false);
      if (result != null) {
        m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, result);
        getStepManager().outputData(m_streamingData);
        getStepManager().throughputUpdateEnd();
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected void processBatch(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    Instances batch = data.getPrimaryPayload();
    for (int i = 0; i < batch.numInstances(); i++) {
      Instance current = batch.instance(i);
      Instance result = null;
      try {
        result = m_matches.makeOutputInstance(current, true);
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      if (isStopRequested()) {
        return;
      }

      if (result != null) {
        m_matches.getOutputStructure().add(result);
      }
    }

    Data outputD =
      new Data(data.getConnectionName(), m_matches.getOutputStructure());
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM));
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM));
    getStepManager().outputData(outputD);
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    // we output the same structure as we receive + one additional attribute
    if (getStepManager().numIncomingConnections() > 0) {
      for (Map.Entry<String, List<StepManager>> e : getStepManager()
        .getIncomingConnections().entrySet()) {
        if (e.getValue().size() > 0) {
          StepManager incoming = e.getValue().get(0);
          String incomingConnType = e.getKey();
          Instances incomingStruc =
            getStepManager().getIncomingStructureFromStep(incoming,
              incomingConnType);
          if (incomingStruc == null) {
            return null;
          }

          try {
            SubstringLabelerRules rules =
              new SubstringLabelerRules(m_matchDetails, m_attName,
                getConsumeNonMatching(), getNominalBinary(), incomingStruc,
                ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix(),
                null, Environment.getSystemWide());

            return rules.getOutputStructure();
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }
      }
    }

    return null;
  }

  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SubstringLabelerStepEditorDialog";
  }
}
