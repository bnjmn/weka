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
 *    Join.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step that performs an inner join on one or more key fields from two incoming
 * batch or streaming datasets.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "Join",
  category = "Flow",
  toolTipText = "Performs an inner join on two incoming datasets/instance streams (IMPORTANT: assumes that "
    + "both datasets are sorted in ascending order of the key fields). If data is not sorted then use"
    + "a Sorter step to sort both into ascending order of the key fields. Does not handle the case where"
    + "keys are not unique in one or both inputs.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "Join.gif")
public class Join extends BaseStep {

  /** Separator used to separate first and second input key specifications */
  public static final String KEY_SPEC_SEPARATOR = "@@KS@@";

  private static final long serialVersionUID = -8248954818247532014L;

  /** First source of data */
  protected StepManager m_firstInput;

  /** Second source of data */
  protected StepManager m_secondInput;

  /** Whether the first is finished (incremental mode) */
  protected transient boolean m_firstFinished;

  /** Whether the second is finished (incremental mode) */
  protected transient boolean m_secondFinished;

  /** Connection type of the first input */
  protected String m_firstInputConnectionType = "";

  /** Connection type of the second input */
  protected String m_secondInputConnectionType = "";

  /** Buffer for the first input (capped at 100 for incremental) */
  protected transient Queue<Sorter.InstanceHolder> m_firstBuffer;

  /** Buffer for the second input (capped at 100 for incremental) */
  protected transient Queue<Sorter.InstanceHolder> m_secondBuffer;

  /** Reusable data object for streaming output */
  protected Data m_streamingData;

  /** The structure of the first incoming dataset */
  protected transient Instances m_headerOne;

  /** The structure of the second incoming dataset */
  protected transient Instances m_headerTwo;

  /** The structure of the outgoing dataset */
  protected transient Instances m_mergedHeader;

  /**
   * A set of copied outgoing structure instances. Used when there are string
   * attributes present in the incremental case in order to prevent concurrency
   * problems where string values could get clobbered in the header.
   */
  protected transient List<Instances> m_headerPool;

  /** Used to cycle over the headers in the header pool */
  protected transient AtomicInteger m_count;

  /** True if string attributes are present in the incoming data */
  protected boolean m_stringAttsPresent;

  /** True if the step is running incrementally */
  protected boolean m_runningIncrementally;

  /** Indexes of the key fields for the first input */
  protected int[] m_keyIndexesOne;

  /** Indexes of the key fields for the second input */
  protected int[] m_keyIndexesTwo;

  /** Holds the internal representation of the key specification */
  protected String m_keySpec = "";

  /** Holds indexes of string attributes, keyed by attribute name */
  protected Map<String, Integer> m_stringAttIndexesOne;

  /** Holds indexes of string attributes, keyed by attribute name */
  protected Map<String, Integer> m_stringAttIndexesTwo;

  /**
   * True if the first input stream is waiting due to a full buffer (incremental
   * mode only)
   */
  protected boolean m_firstIsWaiting;

  /**
   * True if the second input stream is waiting due to a full buffer
   * (incremental mode only)
   */
  protected boolean m_secondIsWaiting;

  /**
   * Set the key specification (in internal format -
   * k11,k12,...,k1nKEY_SPEC_SEPARATORk21,k22,...,k2n)
   *
   * @param ks the keys specification
   */
  public void setKeySpec(String ks) {
    m_keySpec = ks;
  }

  /**
   * Get the key specification (in internal format -
   * k11,k12,...,k1nKEY_SPEC_SEPARATORk21,k22,...,k2n)
   *
   * @return the keys specification
   */
  public String getKeySpec() {
    return m_keySpec;
  }

  /**
   * Get the names of the connected steps as a list
   *
   * @return the names of the connected steps as a list
   */
  public List<String> getConnectedInputNames() {
    // see what's connected (if anything)
    establishFirstAndSecondConnectedInputs();

    List<String> connected = new ArrayList<String>();
    connected.add(m_firstInput != null ? m_firstInput.getName() : null);
    connected.add(m_secondInput != null ? m_secondInput.getName() : null);

    return connected;
  }

  /**
   * Get the Instances structure being produced by the first input
   *
   * @return the Instances structure from the first input
   * @throws WekaException if a problem occurs
   */
  public Instances getFirstInputStructure() throws WekaException {
    if (m_firstInput == null) {
      establishFirstAndSecondConnectedInputs();
    }

    if (m_firstInput != null) {
      return getStepManager().getIncomingStructureFromStep(m_firstInput,
        m_firstInputConnectionType);
    }

    return null;
  }

  /**
   * Get the Instances structure being produced by the second input
   *
   * @return the Instances structure from the second input
   * @throws WekaException if a problem occurs
   */
  public Instances getSecondInputStructure() throws WekaException {
    if (m_secondInput == null) {
      establishFirstAndSecondConnectedInputs();
    }

    if (m_secondInput != null) {
      return getStepManager().getIncomingStructureFromStep(m_secondInput,
        m_secondInputConnectionType);
    }

    return null;
  }

  /**
   * Look for, and configure with respect to, first and second inputs
   */
  protected void establishFirstAndSecondConnectedInputs() {
    m_firstInput = null;
    m_secondInput = null;
    for (Map.Entry<String, List<StepManager>> e : getStepManager()
      .getIncomingConnections().entrySet()) {

      if (m_firstInput != null && m_secondInput != null) {
        break;
      }

      for (StepManager m : e.getValue()) {
        if (m_firstInput == null) {
          m_firstInput = m;
          m_firstInputConnectionType = e.getKey();
        } else if (m_secondInput == null) {
          m_secondInput = m;
          m_secondInputConnectionType = e.getKey();
        }

        if (m_firstInput != null && m_secondInput != null) {
          break;
        }
      }
    }
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_firstBuffer = new LinkedList<Sorter.InstanceHolder>();
    m_secondBuffer = new LinkedList<Sorter.InstanceHolder>();
    m_streamingData = new Data(StepManager.CON_INSTANCE);
    m_firstInput = null;
    m_secondInput = null;
    m_headerOne = null;
    m_headerTwo = null;
    m_firstFinished = false;
    m_secondFinished = false;

    if (getStepManager().numIncomingConnections() < 2) {
      throw new WekaException("Two incoming connections are required for the "
        + "Join step");
    }

    establishFirstAndSecondConnectedInputs();
  }

  /**
   * Process some incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {

    if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
      processStreaming(data);
      if (isStopRequested()) {
        getStepManager().interrupted();
      }
    } else {
      processBatch(data);
      if (isStopRequested()) {
        getStepManager().interrupted();
      }
      return;
    }
  }

  /**
   * Handle streaming data
   *
   * @param data an instance of streaming data
   * @throws WekaException if a problem occurs
   */
  protected synchronized void processStreaming(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      if (data.getSourceStep().getStepManager() == m_firstInput) {
        m_firstFinished = true;
        getStepManager().logBasic(
          "Finished receiving from " + m_firstInput.getName());
      } else if (data.getSourceStep().getStepManager() == m_secondInput) {
        m_secondFinished = true;
        getStepManager().logBasic(
          "Finished receiving from " + m_secondInput.getName());
      }

      if (m_firstFinished && m_secondFinished) {
        clearBuffers();
        m_streamingData.clearPayload();
        getStepManager().throughputFinished(m_streamingData);
      }

      return;
    }

    Instance inst = data.getPrimaryPayload();
    StepManager source = data.getSourceStep().getStepManager();
    if (m_headerOne == null || m_headerTwo == null) {
      if (m_headerOne == null && source == m_firstInput) {
        m_headerOne = new Instances(inst.dataset(), 0);
        getStepManager().logBasic(
          "Initializing buffer for " + m_firstInput.getName());
        m_stringAttIndexesOne = new HashMap<String, Integer>();
        for (int i = 0; i < m_headerOne.numAttributes(); i++) {
          if (m_headerOne.attribute(i).isString()) {
            m_stringAttIndexesOne.put(m_headerOne.attribute(i).name(), i);
          }
        }
      }

      if (m_headerTwo == null && source == m_secondInput) {
        m_headerTwo = new Instances(inst.dataset(), 0);
        getStepManager().logBasic(
          "Initializing buffer for " + m_secondInput.getName());
        m_stringAttIndexesTwo = new HashMap<String, Integer>();
        for (int i = 0; i < m_headerTwo.numAttributes(); i++) {
          if (m_headerTwo.attribute(i).isString()) {
            m_stringAttIndexesTwo.put(m_headerTwo.attribute(i).name(), i);
          }
        }
      }

      if (m_mergedHeader == null) {
        // can we determine the header?
        if (m_headerOne != null && m_headerTwo != null && m_keySpec != null
          && m_keySpec.length() > 0) {

          // construct merged header & check validity of indexes
          generateMergedHeader();
        }
      }
    }

    if (source == m_firstInput) {
      addToFirstBuffer(inst);
    } else {
      addToSecondBuffer(inst);
    }

    if (source == m_firstInput && m_secondBuffer.size() <= 100
      && m_secondIsWaiting) {
      m_secondIsWaiting = false;
      notifyAll();
    } else if (source == m_secondInput && m_secondBuffer.size() <= 100
      && m_firstIsWaiting) {
      m_firstIsWaiting = false;
      notifyAll();
    }

    if (isStopRequested()) {
      return;
    }

    Instance outputI = processBuffers();
    if (outputI != null) {
      getStepManager().throughputUpdateStart();
      m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, outputI);
      getStepManager().outputData(m_streamingData);
      getStepManager().throughputUpdateEnd();
    }
  }

  /**
   * Copy the string values out of an instance into the temporary storage in
   * InstanceHolder
   *
   * @param holder the InstanceHolder encapsulating the instance and it's string
   *          values
   * @param stringAttIndexes indices of string attributes in the instance
   */
  private static void copyStringAttVals(Sorter.InstanceHolder holder,
    Map<String, Integer> stringAttIndexes) {

    for (String attName : stringAttIndexes.keySet()) {
      Attribute att = holder.m_instance.dataset().attribute(attName);
      String val = holder.m_instance.stringValue(att);

      if (holder.m_stringVals == null) {
        holder.m_stringVals = new HashMap<String, String>();
      }

      holder.m_stringVals.put(attName, val);
    }
  }

  /**
   * Add an instance to the first buffer
   *
   * @param inst the instance to add
   */
  protected synchronized void addToFirstBuffer(Instance inst) {
    if (isStopRequested()) {
      return;
    }

    Sorter.InstanceHolder newH = new Sorter.InstanceHolder();
    newH.m_instance = inst;
    copyStringAttVals(newH, m_stringAttIndexesOne);
    m_firstBuffer.add(newH);

    if (m_firstBuffer.size() > 100 && !m_secondFinished) {
      try {
        m_firstIsWaiting = true;
        wait();
      } catch (InterruptedException ex) {
        // ignore
      }
    }
  }

  /**
   * Add an instance to the second buffer
   *
   * @param inst the instance to add
   */
  protected synchronized void addToSecondBuffer(Instance inst) {
    if (isStopRequested()) {
      return;
    }

    Sorter.InstanceHolder newH = new Sorter.InstanceHolder();
    newH.m_instance = inst;
    copyStringAttVals(newH, m_stringAttIndexesTwo);
    m_secondBuffer.add(newH);

    if (m_secondBuffer.size() > 100 && !m_firstFinished) {
      try {
        m_secondIsWaiting = true;
        wait();
      } catch (InterruptedException e) {
        //
      }
    }
  }

  /**
   * Clear the buffers
   *
   * @throws WekaException if a problem occurs
   */
  protected synchronized void clearBuffers() throws WekaException {
    while (m_firstBuffer.size() > 0 && m_secondBuffer.size() > 0) {
      if (isStopRequested()) {
        return;
      }
      getStepManager().throughputUpdateStart();
      Instance newInst = processBuffers();
      getStepManager().throughputUpdateEnd();
      m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, newInst);
      getStepManager().outputData(m_streamingData);
    }
  }

  /**
   * Process batch data.
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected synchronized void processBatch(Data data) throws WekaException {
    Instances insts = data.getPrimaryPayload();

    if (data.getSourceStep().getStepManager() == m_firstInput) {
      m_headerOne = new Instances(insts, 0);
      getStepManager().logDetailed(
        "Receiving batch from " + m_firstInput.getName());

      for (int i = 0; i < insts.numInstances() && !isStopRequested(); i++) {
        Sorter.InstanceHolder tempH = new Sorter.InstanceHolder();
        tempH.m_instance = insts.instance(i);
        m_firstBuffer.add(tempH);
      }
    } else if (data.getSourceStep().getStepManager() == m_secondInput) {
      m_headerTwo = new Instances(insts, 0);
      getStepManager().logDetailed(
        "Receiving batch from " + m_secondInput.getName());
      for (int i = 0; i < insts.numInstances() && !isStopRequested(); i++) {
        Sorter.InstanceHolder tempH = new Sorter.InstanceHolder();
        tempH.m_instance = insts.instance(i);
        m_secondBuffer.add(tempH);
      }
    } else {
      throw new WekaException("This should never happen");
    }

    if (m_firstBuffer.size() > 0 && m_secondBuffer.size() > 0) {
      getStepManager().processing();
      generateMergedHeader();

      Instances newData = new Instances(m_mergedHeader, 0);
      while (!isStopRequested() && m_firstBuffer.size() > 0
        && m_secondBuffer.size() > 0) {
        Instance newI = processBuffers();
        if (newI != null) {
          newData.add(newI);
        }
      }

      for (String outConnType : getStepManager().getOutgoingConnections()
        .keySet()) {
        if (isStopRequested()) {
          return;
        }
        Data outputD = new Data(outConnType, newData);
        outputD.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        outputD.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

        getStepManager().outputData(outputD);
      }
      getStepManager().finished();
    }
  }

  /**
   * Check both buffers and return a joined instance (if possible at this time)
   * or null
   *
   * @return a joined instance or null
   */
  protected synchronized Instance processBuffers() {
    if (m_firstBuffer.size() > 0 && m_secondBuffer.size() > 0) {
      Sorter.InstanceHolder firstH = m_firstBuffer.peek();
      Sorter.InstanceHolder secondH = m_secondBuffer.peek();
      Instance first = firstH.m_instance;
      Instance second = secondH.m_instance;

      int cmp = compare(first, second, firstH, secondH);
      if (cmp == 0) {
        // match on all keys - output joined instance
        Instance newInst =
          generateMergedInstance(m_firstBuffer.remove(),
            m_secondBuffer.remove());

        return newInst;
      } else if (cmp < 0) {
        // second is ahead of first - discard rows from first
        do {
          m_firstBuffer.remove();
          if (m_firstBuffer.size() > 0) {
            firstH = m_firstBuffer.peek();
            first = firstH.m_instance;
            cmp = compare(first, second, firstH, secondH);
          }
        } while (cmp < 0 && m_firstBuffer.size() > 0);
      } else {
        // first is ahead of second - discard rows from second
        do {
          m_secondBuffer.remove();
          if (m_secondBuffer.size() > 0) {
            secondH = m_secondBuffer.peek();
            second = secondH.m_instance;
            cmp = compare(first, second, firstH, secondH);
          }
        } while (cmp > 0 && m_secondBuffer.size() > 0);
      }
    }

    return null;
  }

  /**
   * Compares two instances according to the keys
   *
   * @param one the first instance
   * @param two the second instance
   * @param oneH the first instance holder (in case string attributes are
   *          present and we are running incrementally)
   * @param twoH the second instance holder
   * @return the comparison according to the keys
   */
  protected int compare(Instance one, Instance two, Sorter.InstanceHolder oneH,
    Sorter.InstanceHolder twoH) {

    for (int i = 0; i < m_keyIndexesOne.length; i++) {
      if (one.isMissing(m_keyIndexesOne[i])
        && two.isMissing(m_keyIndexesTwo[i])) {
        continue;
      }

      if (one.isMissing(m_keyIndexesOne[i])
        || two.isMissing(m_keyIndexesTwo[i])) {

        // ensure that the input with the missing value gets discarded
        if (one.isMissing(m_keyIndexesOne[i])) {
          return -1;
        } else {
          return 1;
        }
      }

      if (m_mergedHeader.attribute(m_keyIndexesOne[i]).isNumeric()) {
        double v1 = one.value(m_keyIndexesOne[i]);
        double v2 = two.value(m_keyIndexesTwo[i]);

        if (v1 != v2) {
          return v1 < v2 ? -1 : 1;
        }
      } else if (m_mergedHeader.attribute(m_keyIndexesOne[i]).isNominal()) {
        String oneS = one.stringValue(m_keyIndexesOne[i]);
        String twoS = two.stringValue(m_keyIndexesTwo[i]);

        int cmp = oneS.compareTo(twoS);

        if (cmp != 0) {
          return cmp;
        }
      } else if (m_mergedHeader.attribute(m_keyIndexesOne[i]).isString()) {
        String attNameOne = m_mergedHeader.attribute(m_keyIndexesOne[i]).name();
        String attNameTwo = m_mergedHeader.attribute(m_keyIndexesTwo[i]).name();

        String oneS =
          oneH.m_stringVals == null || oneH.m_stringVals.size() == 0 ? one
            .stringValue(m_keyIndexesOne[i]) : oneH.m_stringVals
            .get(attNameOne);
        String twoS =
          twoH.m_stringVals == null || twoH.m_stringVals.size() == 0 ? two
            .stringValue(m_keyIndexesTwo[i]) : twoH.m_stringVals
            .get(attNameTwo);

        int cmp = oneS.compareTo(twoS);

        if (cmp != 0) {
          return cmp;
        }
      }
    }

    return 0;
  }

  /**
   * Generate a merged instance from two input instances that match on the key
   * fields
   *
   * @param one the first input instance
   * @param two the second input instance
   * @return the merged instance
   */
  protected synchronized Instance generateMergedInstance(
    Sorter.InstanceHolder one, Sorter.InstanceHolder two) {

    double[] vals = new double[m_mergedHeader.numAttributes()];
    int count = 0;
    Instances currentStructure = m_mergedHeader;

    if (m_runningIncrementally && m_stringAttsPresent) {
      currentStructure = m_headerPool.get(m_count.getAndIncrement() % 10);
    }

    for (int i = 0; i < m_headerOne.numAttributes(); i++) {
      vals[count] = one.m_instance.value(i);
      if (one.m_stringVals != null && one.m_stringVals.size() > 0
        && m_mergedHeader.attribute(count).isString()) {
        String valToSetInHeader =
          one.m_stringVals.get(one.m_instance.attribute(i).name());
        currentStructure.attribute(count).setStringValue(valToSetInHeader);
        vals[count] = 0;
      }
      count++;
    }

    for (int i = 0; i < m_headerTwo.numAttributes(); i++) {
      vals[count] = two.m_instance.value(i);
      if (two.m_stringVals != null && two.m_stringVals.size() > 0
        && m_mergedHeader.attribute(count).isString()) {
        String valToSetInHeader =
          one.m_stringVals.get(two.m_instance.attribute(i).name());
        currentStructure.attribute(count).setStringValue(valToSetInHeader);
        vals[count] = 0;
      }

      count++;
    }

    Instance newInst = new DenseInstance(1.0, vals);
    newInst.setDataset(currentStructure);

    return newInst;
  }

  /**
   * Generate the header of the output instance structure
   */
  protected void generateMergedHeader() throws WekaException {
    // check validity of key fields first

    if (m_keySpec == null || m_keySpec.length() == 0) {
      throw new WekaException("Key fields are null!");
    }

    String resolvedKeySpec = m_keySpec;
    resolvedKeySpec = environmentSubstitute(resolvedKeySpec);

    String[] parts = resolvedKeySpec.split(KEY_SPEC_SEPARATOR);
    if (parts.length != 2) {
      throw new WekaException("Invalid key specification");
    }

    // try to parse as a Range first
    for (int i = 0; i < 2; i++) {
      String rangeS = parts[i].trim();

      Range r = new Range();
      r.setUpper(i == 0 ? m_headerOne.numAttributes() : m_headerTwo
        .numAttributes());
      try {
        r.setRanges(rangeS);
        if (i == 0) {
          m_keyIndexesOne = r.getSelection();
        } else {
          m_keyIndexesTwo = r.getSelection();
        }
      } catch (IllegalArgumentException e) {
        // assume a list of attribute names
        String[] names = rangeS.split(",");
        if (i == 0) {
          m_keyIndexesOne = new int[names.length];
        } else {
          m_keyIndexesTwo = new int[names.length];
        }

        for (int j = 0; j < names.length; j++) {
          String aName = names[j].trim();
          Attribute anAtt =
            (i == 0) ? m_headerOne.attribute(aName) : m_headerTwo
              .attribute(aName);

          if (anAtt == null) {
            throw new WekaException("Invalid key attribute name");
          }

          if (i == 0) {
            m_keyIndexesOne[j] = anAtt.index();
          } else {
            m_keyIndexesTwo[j] = anAtt.index();
          }
        }
      }
    }

    if (m_keyIndexesOne == null || m_keyIndexesTwo == null) {
      throw new WekaException("Key fields are null!");
    }

    if (m_keyIndexesOne.length != m_keyIndexesTwo.length) {
      throw new WekaException(
        "Number of key fields are different for each input");
    }

    // check types
    for (int i = 0; i < m_keyIndexesOne.length; i++) {
      if (m_headerOne.attribute(m_keyIndexesOne[i]).type() != m_headerTwo
        .attribute(m_keyIndexesTwo[i]).type()) {
        throw new WekaException("Type of key corresponding to key fields "
          + "differ: input 1 - "
          + Attribute.typeToStringShort(m_headerOne
            .attribute(m_keyIndexesOne[i]))
          + " input 2 - "
          + Attribute.typeToStringShort(m_headerTwo
            .attribute(m_keyIndexesTwo[i])));
      }
    }

    ArrayList<Attribute> newAtts = new ArrayList<Attribute>();

    Set<String> nameLookup = new HashSet<String>();
    for (int i = 0; i < m_headerOne.numAttributes(); i++) {
      newAtts.add((Attribute) m_headerOne.attribute(i).copy());
      nameLookup.add(m_headerOne.attribute(i).name());
    }

    for (int i = 0; i < m_headerTwo.numAttributes(); i++) {
      String name = m_headerTwo.attribute(i).name();
      if (nameLookup.contains(name)) {
        name = name + "_2";
      }

      newAtts.add(m_headerTwo.attribute(i).copy(name));
    }

    m_mergedHeader =
      new Instances(m_headerOne.relationName() + "+"
        + m_headerTwo.relationName(), newAtts, 0);

    m_stringAttsPresent = false;
    if (m_mergedHeader.checkForStringAttributes()) {
      m_stringAttsPresent = true;
      m_headerPool = new ArrayList<Instances>();
      m_count = new AtomicInteger();
      for (int i = 0; i < 10; i++) {
        try {
          m_headerPool.add((Instances) (new SerializedObject(m_mergedHeader))
            .getObject());

        } catch (Exception e) {
          e.printStackTrace();
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
      return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
    }

    if (getStepManager().numIncomingConnections() == 1) {
      result.addAll(getStepManager().getIncomingConnections().keySet());
      return result;
    }

    return null;
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
    if (getStepManager().numIncomingConnections() > 0) {
      // we output the same connection type as the inputs
      List<String> result = new ArrayList<String>();
      result.addAll(getStepManager().getIncomingConnections().keySet());
      return result;
    }

    return null;
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
    return "weka.gui.knowledgeflow.steps.JoinStepEditorDialog";
  }
}
