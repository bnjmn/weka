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
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.beans.EventSetDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.gui.Logger;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Flow", toolTipText = "Inner join on one or more key fields")
public class Join extends JPanel implements BeanCommon, Visible, Serializable,
  DataSource, DataSourceListener, TrainingSetListener, TestSetListener,
  InstanceListener, EventConstraints, StructureProducer, EnvironmentHandler {

  /** Separator used to separate first and second input key specifications */
  protected static final String KEY_SPEC_SEPARATOR = "@@KS@@";

  /** For serialization */
  private static final long serialVersionUID = 398021880509558185L;

  /** Logging */
  protected transient Logger m_log;

  /** Environment variables */
  protected transient Environment m_env;

  /** Upstream components sending us data */
  protected boolean m_incomingBatchConnections;

  /** The first source of data */
  protected Object m_firstInput;

  /** The second source of data */
  protected Object m_secondInput;

  /** Whether the first is finished (incremental mode) */
  protected transient boolean m_firstFinished;

  /** Whether the second is finished (incremental mode) */
  protected transient boolean m_secondFinished;

  /** Connection type of the first input */
  protected String m_firstInputConnectionType = "";

  /** Connection type of the second input */
  protected String m_secondInputConnectionType = "";

  /** Buffer for the first input (capped at 100 for incremental) */
  protected transient Queue<InstanceHolder> m_firstBuffer;

  /** Buffer for the second input (capped at 100 for incremental) */
  protected transient Queue<InstanceHolder> m_secondBuffer;

  /** Instance event to use for incremental mode */
  protected InstanceEvent m_ie = new InstanceEvent(this);

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

  /** True if we are busy */
  protected boolean m_busy;

  /** True if the step has been told to stop processing */
  protected AtomicBoolean m_stopRequested;

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
   * Default visual for data sources
   */
  protected BeanVisual m_visual = new BeanVisual("Join", BeanVisual.ICON_PATH
    + "Join.gif", BeanVisual.ICON_PATH + "Join.gif");

  /** Downstream steps listening to batch data events */
  protected ArrayList<DataSourceListener> m_dataListeners =
    new ArrayList<DataSourceListener>();

  /** Downstream steps listening to instance events */
  protected ArrayList<InstanceListener> m_instanceListeners =
    new ArrayList<InstanceListener>();

  /** Used for computing streaming throughput stats */
  protected transient StreamThroughput m_throughput;

  /**
   * Small helper class for holding an instance and the values of any string
   * attributes it might have. This is needed in the streaming case as the
   * KnowledgeFlow only ever keeps the current instances string values in the
   * header (to save memory). Since we have to buffer a certain number of
   * instances from each input, it is necessary to have a separate copy of the
   * string values so that they can be restored to correct values in the
   * outgoing instances.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class InstanceHolder implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -2554438923824758088L;

    /** The instance */
    protected Instance m_instance;

    /**
     * for incremental operation, if string attributes are present then we need
     * to store them with each instance - since incremental streaming in the
     * knowledge flow only maintains one string value in memory (and hence in
     * the header) at any one time
     */
    protected Map<String, String> m_stringVals;
  }

  /**
   * Constructor
   */
  public Join() {
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);

    m_env = Environment.getSystemWide();

    m_stopRequested = new AtomicBoolean(false);
  }

  /**
   * Global info for the method
   * 
   * @return the global help info
   */
  public String globalInfo() {
    return "Performs an inner join on two incoming datasets/instance streams (IMPORTANT: assumes that "
      + "both datasets are sorted in ascending order of the key fields). If data is not sorted then use"
      + "a Sorter step to sort both into ascending order of the key fields. Does not handle the case where"
      + "keys are not unique in one or both inputs.";
  }

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

  @Override
  public boolean eventGeneratable(String eventName) {
    // Auto-generated method stub

    if (m_firstInput == null || m_secondInput == null) {
      return false;
    }

    if (eventName.equals("instance") && m_incomingBatchConnections) {
      return false;
    }

    if (!eventName.equals("instance") && !m_incomingBatchConnections) {
      return false;
    }

    return true;
  }

  /**
   * Generate the header of the output instance structure
   */
  protected void generateMergedHeader() {
    // check validity of key fields first

    if (m_keySpec == null || m_keySpec.length() == 0) {
      if (m_log != null) {
        String msg = statusMessagePrefix() + "ERROR: Key fields are null!";
        m_log.statusMessage(msg);
        m_log.logMessage(msg);
        stop();
        m_busy = false;
        return;
      }
    }

    String resolvedKeySpec = m_keySpec;
    try {
      resolvedKeySpec = m_env.substitute(m_keySpec);
    } catch (Exception ex) {
    }

    String[] parts = resolvedKeySpec.split(KEY_SPEC_SEPARATOR);
    if (parts.length != 2) {
      if (m_log != null) {
        String msg =
          statusMessagePrefix() + "ERROR: Invalid key specification: "
            + m_keySpec;
        m_log.statusMessage(msg);
        m_log.logMessage(msg);
        stop();
        m_busy = false;
        return;
      }
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
            String msg =
              statusMessagePrefix() + "ERROR: Invalid key attribute name: "
                + aName;
            if (m_log != null) {
              m_log.statusMessage(msg);
              m_log.logMessage(msg);
            } else {
              System.err.println(msg);
            }
            stop();
            m_busy = false;
            return;
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
      if (m_log != null) {
        String msg = statusMessagePrefix() + "ERROR: Key fields are null!";
        m_log.statusMessage(msg);
        m_log.logMessage(msg);
        stop();
        m_busy = false;
        return;
      }
    }

    if (m_keyIndexesOne.length != m_keyIndexesTwo.length) {
      if (m_log != null) {
        String msg =
          statusMessagePrefix()
            + "ERROR: number of key fields are different for each input!";
        m_log.statusMessage(msg);
        m_log.logMessage(msg);
        stop();
        m_busy = false;
        return;
      }
    }

    // check types
    for (int i = 0; i < m_keyIndexesOne.length; i++) {
      if (m_headerOne.attribute(m_keyIndexesOne[i]).type() != m_headerTwo
        .attribute(m_keyIndexesTwo[i]).type()) {
        if (m_log != null) {
          String msg =
            statusMessagePrefix()
              + "ERROR: type of key corresponding key fields differ: "
              + "input 1 - "
              + Attribute.typeToStringShort(m_headerOne
                .attribute(m_keyIndexesOne[i]))
              + " input 2 - "
              + Attribute.typeToStringShort(m_headerTwo
                .attribute(m_keyIndexesTwo[i]));
          m_log.statusMessage(msg);
          m_log.logMessage(msg);
          stop();
          m_busy = false;
          return;
        }
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
    m_ie.setStructure(m_mergedHeader);
    notifyInstanceListeners(m_ie);

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
   * Generate a merged instance from two input instances that match on the key
   * fields
   * 
   * @param one the first input instance
   * @param two the second input instance
   * @return the merged instance
   */
  protected synchronized Instance generateMergedInstance(InstanceHolder one,
    InstanceHolder two) {

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

  @Override
  public synchronized void acceptInstance(InstanceEvent e) {

    if (e.m_formatNotificationOnly) {
      return;
    }
    m_busy = true;

    Object source = e.getSource();
    if (e.getStatus() == InstanceEvent.FORMAT_AVAILABLE) {
      m_runningIncrementally = true;
      m_stopRequested.set(false);

      if (!m_stopRequested.get() && source == m_firstInput
        && m_firstBuffer == null) {
        System.err.println("Allocating first buffer");
        m_firstFinished = false;
        m_firstBuffer = new LinkedList<InstanceHolder>();
        m_headerOne = e.getStructure();
        m_stringAttIndexesOne = new HashMap<String, Integer>();
        for (int i = 0; i < m_headerOne.numAttributes(); i++) {
          if (m_headerOne.attribute(i).isString()) {
            m_stringAttIndexesOne.put(m_headerOne.attribute(i).name(),
              new Integer(i));
          }
        }
      }

      if (!m_stopRequested.get() && source == m_secondInput
        && m_secondBuffer == null) {
        System.err.println("Allocating second buffer");
        m_secondFinished = false;
        m_secondBuffer = new LinkedList<InstanceHolder>();
        m_headerTwo = e.getStructure();
        m_stringAttIndexesTwo = new HashMap<String, Integer>();
        for (int i = 0; i < m_headerTwo.numAttributes(); i++) {
          if (m_headerTwo.attribute(i).isString()) {
            m_stringAttIndexesTwo.put(m_headerTwo.attribute(i).name(),
              new Integer(i));
          }
        }
      }

      if (m_stopRequested.get()) {
        return;
      }

      if (m_mergedHeader == null) {
        // can we determine the header?

        m_throughput = new StreamThroughput(statusMessagePrefix());
        if (m_headerOne != null && m_headerTwo != null && m_keySpec != null
          && m_keySpec.length() > 0) {

          // construct merged header & check validity of indexes
          generateMergedHeader();
        }
      }
    } else {
      if (m_stopRequested.get()) {
        return;
      }

      Instance current = e.getInstance();
      if (current == null || e.getStatus() == InstanceEvent.BATCH_FINISHED) {
        if (source == m_firstInput) {
          System.err.println("Finished first");
          m_firstFinished = true;
        }
        if (source == m_secondInput) {
          System.err.println("Finished second");
          m_secondFinished = true;
        }
      }

      if (current != null) {
        if (source == m_firstInput) {
          // m_firstBuffer.add(current);
          addToFirstBuffer(current);
        } else if (source == m_secondInput) {
          // m_secondBuffer.add(current);
          addToSecondBuffer(current);
        }
      }

      if (source == m_firstInput && m_secondBuffer != null
        && m_secondBuffer.size() <= 100 && m_secondIsWaiting) {
        notifyAll();
        m_secondIsWaiting = false;
      } else if (source == m_secondInput && m_firstBuffer != null
        && m_firstBuffer.size() <= 100 && m_firstIsWaiting) {
        notifyAll();
        m_firstIsWaiting = false;
      }

      if (m_firstFinished && m_secondFinished && !m_stopRequested.get()) {
        // finished - now just clear buffers and reset headers etc
        clearBuffers();
        return;
      }

      if (m_stopRequested.get()) {
        return;
      }

      m_throughput.updateStart();
      Instance outputI = processBuffers();
      m_throughput.updateEnd(m_log);

      if (outputI != null && !m_stopRequested.get()) {
        // notify instance listeners
        m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
        m_ie.setInstance(outputI);
        notifyInstanceListeners(m_ie);
      }
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
  private static void copyStringAttVals(InstanceHolder holder,
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
   * Adds an instance to the first input's buffer
   * 
   * @param inst the instance to add
   */
  protected synchronized void addToFirstBuffer(Instance inst) {
    if (m_stopRequested.get()) {
      return;
    }

    InstanceHolder newH = new InstanceHolder();
    newH.m_instance = inst;
    copyStringAttVals(newH, m_stringAttIndexesOne);

    if (!m_stopRequested.get()) {
      m_firstBuffer.add(newH);
    } else {
      return;
    }

    if (m_firstBuffer.size() > 100 && !m_secondFinished) {

      try {
        m_firstIsWaiting = true;
        wait();
      } catch (InterruptedException ex) {
      }
    }
  }

  /**
   * Adds an instance to the second input's buffer
   * 
   * @param inst the instance to add
   */
  protected synchronized void addToSecondBuffer(Instance inst) {
    if (m_stopRequested.get()) {
      return;
    }

    InstanceHolder newH = new InstanceHolder();
    newH.m_instance = inst;
    copyStringAttVals(newH, m_stringAttIndexesTwo);

    if (!m_stopRequested.get()) {
      m_secondBuffer.add(newH);
    } else {
      return;
    }

    if (m_secondBuffer.size() > 100 && !m_firstFinished) {

      try {
        m_secondIsWaiting = true;
        wait();
      } catch (InterruptedException e) {
      }
    }
  }

  /**
   * Clear remaining instances in the buffers
   */
  protected synchronized void clearBuffers() {
    while (m_firstBuffer.size() > 0 && m_secondBuffer.size() > 0) {
      m_throughput.updateStart();
      Instance newInst = processBuffers();
      m_throughput.updateEnd(m_log);

      if (newInst != null) {
        m_ie.setInstance(newInst);
        m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
        notifyInstanceListeners(m_ie);
      }
    }

    // indicate end of stream
    m_ie.setInstance(null);
    m_ie.setStatus(InstanceEvent.BATCH_FINISHED);
    notifyInstanceListeners(m_ie);

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Finished");
    }

    m_headerOne = null;
    m_headerTwo = null;
    m_mergedHeader = null;
    m_firstBuffer = null;
    m_secondBuffer = null;
    m_firstFinished = false;
    m_secondFinished = false;
    m_busy = false;
  }

  /**
   * Process the current state of the two buffers. Compares the head of both
   * buffers and will return a merged instance if they match on the key fields.
   * Will remove instances from one or the other buffer if it is behind
   * according to the keys comparison
   * 
   * @return a merged instance if keys match, otherwise null
   */
  protected synchronized Instance processBuffers() {
    if (m_firstBuffer != null && m_secondBuffer != null
      && m_firstBuffer.size() > 0 && m_secondBuffer.size() > 0) {
      // System.err.println("Buffer sizes: " + m_firstBuffer.size() + " "
      // + m_secondBuffer.size());

      if (m_stopRequested.get()) {
        return null;
      }

      InstanceHolder firstH = m_firstBuffer.peek();
      InstanceHolder secondH = m_secondBuffer.peek();
      Instance first = firstH.m_instance;
      Instance second = secondH.m_instance;
      // System.err.println("Keys " + first.value(0) + " " + second.value(0));
      // try {
      // Thread.sleep(500);
      // } catch (InterruptedException ex) {
      //
      // }

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
  protected int compare(Instance one, Instance two, InstanceHolder oneH,
    InstanceHolder twoH) {

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
   * Accept and process a test set
   * 
   * @param e the test set event encapsulating the test set
   */
  @Override
  public void acceptTestSet(TestSetEvent e) {
    DataSetEvent de = new DataSetEvent(e.getSource(), e.getTestSet());
    acceptDataSet(de);
  }

  /**
   * Accept and process a training set
   * 
   * @param e the training set event encapsulating the training set
   */
  @Override
  public void acceptTrainingSet(TrainingSetEvent e) {
    DataSetEvent de = new DataSetEvent(e.getSource(), e.getTrainingSet());
    acceptDataSet(de);
  }

  /**
   * Accept and process a data set
   * 
   * @param e the data set event encapsulating the data set
   */
  @Override
  public synchronized void acceptDataSet(DataSetEvent e) {
    m_runningIncrementally = false;
    m_stopRequested.set(false);

    if (e.getSource() == m_firstInput) {

      if (e.isStructureOnly() || e.getDataSet().numInstances() == 0) {
        m_headerOne = e.getDataSet();
        return;
      }

      if (m_headerOne == null) {
        m_headerOne = new Instances(e.getDataSet(), 0);
      }
      m_firstBuffer = new LinkedList<InstanceHolder>();
      for (int i = 0; i < e.getDataSet().numInstances()
        && !m_stopRequested.get(); i++) {
        InstanceHolder tempH = new InstanceHolder();
        tempH.m_instance = e.getDataSet().instance(i);
        m_firstBuffer.add(tempH);
      }
    } else if (e.getSource() == m_secondInput) {
      if (e.isStructureOnly() || e.getDataSet().numInstances() == 0) {
        m_headerTwo = e.getDataSet();
        return;
      }

      if (m_headerTwo == null) {
        m_headerTwo = new Instances(e.getDataSet(), 0);
      }
      m_secondBuffer = new LinkedList<InstanceHolder>();
      for (int i = 0; i < e.getDataSet().numInstances()
        && !m_stopRequested.get(); i++) {
        InstanceHolder tempH = new InstanceHolder();
        tempH.m_instance = e.getDataSet().instance(i);
        m_secondBuffer.add(tempH);
      }
    }

    if (m_firstBuffer != null && m_firstBuffer.size() > 0
      && m_secondBuffer != null && m_secondBuffer.size() > 0) {
      m_busy = true;

      generateMergedHeader();
      DataSetEvent dse = new DataSetEvent(this, m_mergedHeader);
      notifyDataListeners(dse);

      Instances newData = new Instances(m_mergedHeader, 0);
      while (!m_stopRequested.get() && m_firstBuffer.size() > 0
        && m_secondBuffer.size() > 0) {
        Instance newI = processBuffers();

        if (newI != null) {
          newData.add(newI);
        }
      }

      if (!m_stopRequested.get()) {
        dse = new DataSetEvent(this, newData);
        notifyDataListeners(dse);
      }
      m_busy = false;
      m_headerOne = null;
      m_headerTwo = null;
      m_mergedHeader = null;
      m_firstBuffer = null;
      m_secondBuffer = null;
    }
  }

  /**
   * Add a data source listener
   * 
   * @param dsl the data source listener to add
   */
  @Override
  public void addDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.add(dsl);
  }

  /**
   * Remove a data souce listener
   * 
   * @param dsl the data source listener to remove
   */
  @Override
  public void removeDataSourceListener(DataSourceListener dsl) {
    m_dataListeners.remove(dsl);
  }

  /**
   * Add an instance listener
   * 
   * @param dsl the instance listener to add
   */
  @Override
  public void addInstanceListener(InstanceListener dsl) {
    m_instanceListeners.add(dsl);
  }

  /**
   * Remove an instance listener
   * 
   * @param dsl the instance listener to remove
   */
  @Override
  public void removeInstanceListener(InstanceListener dsl) {
    m_instanceListeners.remove(dsl);
  }

  /**
   * Use the default visual for this step
   */
  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "Join.gif", BeanVisual.ICON_PATH
      + "Join.gif");
    m_visual.setText("Join");
  }

  /**
   * Set the visual for this step
   * 
   * @param newVisual the visual to use
   */
  @Override
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual for this step
   * 
   * @return the visual (icon)
   */
  @Override
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Set a custom name for this step
   * 
   * @param name the custom name to use
   */
  @Override
  public void setCustomName(String name) {
    m_visual.setText(name);
  }

  /**
   * Get the custom name of this step
   * 
   * @return the custom name of this step
   */
  @Override
  public String getCustomName() {
    return m_visual.getText();
  }

  /**
   * Attempt to stop processing
   */
  @Override
  public void stop() {
    if (m_firstInput != null && m_firstInput instanceof BeanCommon) {
      ((BeanCommon) m_firstInput).stop();
    }

    if (m_secondInput != null && m_secondInput instanceof BeanCommon) {
      ((BeanCommon) m_secondInput).stop();
    }

    if (m_log != null) {
      m_log.statusMessage(statusMessagePrefix() + "Stopped");
    }

    m_busy = false;
    m_stopRequested.set(true);
    try {
      Thread.sleep(500);
    } catch (InterruptedException ex) {
    }

    if (m_firstIsWaiting || m_secondIsWaiting) {
      notifyAll();
    }

    m_firstBuffer = null;
    m_secondBuffer = null;
    m_headerOne = null;
    m_headerTwo = null;
    m_firstFinished = false;
    m_secondFinished = false;
    m_mergedHeader = null;
  }

  /**
   * Returns true if we are doing something
   * 
   * @return true if processing is occurring
   */
  @Override
  public boolean isBusy() {
    return m_busy;
  }

  /**
   * Set a log to use
   * 
   * @param logger the log to use
   */
  @Override
  public void setLog(Logger logger) {
    m_log = logger;
  }

  /**
   * Returns true if the named connection can be made at this time
   * 
   * @param esd the event set descriptor of the connection
   * @return true if the connection is allowed
   */
  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return connectionAllowed(esd.getName());
  }

  /**
   * Returns true if the named connection can be made at this time
   * 
   * @param eventName the name of the connection
   * @return true if the connection is allowed
   */
  @Override
  public boolean connectionAllowed(String eventName) {
    if (m_firstInput != null && m_secondInput != null) {
      return false;
    }

    if (m_firstInput == null || m_secondInput == null) {
      if (m_firstInput != null) {
        if (m_firstInputConnectionType.equals("instance")
          && !eventName.equals("instance")) {
          return false;
        } else if (!m_firstInputConnectionType.equals("instance")
          && eventName.equals("instance")) {
          return false;
        }

        return true;
      } else if (m_secondInput != null) {
        if (m_secondInputConnectionType.equals("instance")
          && !eventName.equals("instance")) {
          return false;
        } else if (!m_secondInputConnectionType.equals("instance")
          && eventName.equals("instance")) {
          return false;
        }
        return true;
      }

      // both unset as yet
      return true;
    }

    return false;
  }

  /**
   * Deals with a new connection
   * 
   * @param eventName the event type of the connection
   * @param source the source step
   */
  @Override
  public void connectionNotification(String eventName, Object source) {
    if (connectionAllowed(eventName)) {
      if (m_firstInput == null) {
        m_firstInput = source;
        m_firstInputConnectionType = eventName;
      } else {
        m_secondInput = source;
        m_secondInputConnectionType = eventName;
      }
    }

    if (m_firstInput != null && m_secondInput != null) {
      if (m_firstInputConnectionType.length() > 0
        || m_secondInputConnectionType.length() > 0) {
        if (!m_firstInputConnectionType.equals("instance")
          && !m_secondInputConnectionType.equals("instance")) {
          m_incomingBatchConnections = true;
        } else {
          m_incomingBatchConnections = false;
        }
      } else {
        m_incomingBatchConnections = false;
      }
    }
  }

  /**
   * Handles cleanup when an upstream step disconnects
   * 
   * @param eventName the event type of the connection
   * @param source the source step
   */
  @Override
  public void disconnectionNotification(String eventName, Object source) {
    if (source == m_firstInput) {
      m_firstInput = null;
      m_firstInputConnectionType = "";
    } else if (source == m_secondInput) {
      m_secondInput = null;
      m_secondInputConnectionType = "";
    }

    if (m_firstInput != null && m_secondInput != null) {
      if (m_firstInputConnectionType.length() > 0
        || m_secondInputConnectionType.length() > 0) {
        if (!m_firstInputConnectionType.equals("instance")
          && !m_secondInputConnectionType.equals("instance")) {
          m_incomingBatchConnections = true;
        } else {
          m_incomingBatchConnections = false;
        }
      } else {
        m_incomingBatchConnections = false;
      }
    }
  }

  /**
   * Unique prefix to use for status messages to the log
   * 
   * @return the prefix to use
   */
  private String statusMessagePrefix() {
    return getCustomName() + "$" + hashCode() + "|";
  }

  /**
   * Notify instance listeners of an output instance
   * 
   * @param e the event to notify with
   */
  private void notifyInstanceListeners(InstanceEvent e) {
    for (InstanceListener il : m_instanceListeners) {
      il.acceptInstance(e);
    }
  }

  /**
   * Notify data listeners of an output data set
   * 
   * @param e the event to notify with
   */
  private void notifyDataListeners(DataSetEvent e) {
    for (DataSourceListener l : m_dataListeners) {
      l.acceptDataSet(e);
    }
  }

  /**
   * Get the incoming instance structure from the first upstream step (if
   * possible)
   * 
   * @return the incoming instance structure of the first upstream step
   */
  protected Instances getUpstreamStructureFirst() {
    if (m_firstInput != null && m_firstInput instanceof StructureProducer) {
      return ((StructureProducer) m_firstInput)
        .getStructure(m_firstInputConnectionType);
    }
    return null;
  }

  /**
   * Get the incoming instance structure from the second upstream step (if
   * possible)
   * 
   * @return the incoming instance structure of the second upstream step
   */
  protected Instances getUpstreamStructureSecond() {
    if (m_secondInput != null && m_secondInput instanceof StructureProducer) {
      return ((StructureProducer) m_secondInput)
        .getStructure(m_secondInputConnectionType);
    }
    return null;
  }

  /**
   * Get the first input step
   * 
   * @return the first input step
   */
  protected Object getFirstInput() {
    return m_firstInput;
  }

  /**
   * Get the first input structure
   * 
   * @return the first incoming instance structure (or null if unavailable)
   */
  protected Instances getFirstInputStructure() {
    Instances result = null;

    if (m_firstInput instanceof StructureProducer) {
      result =
        ((StructureProducer) m_firstInput)
          .getStructure(m_firstInputConnectionType);
    }

    return result;
  }

  /**
   * Get the second input step
   * 
   * @return the second input step
   */
  protected Object getSecondInput() {
    return m_secondInput;
  }

  /**
   * Get the second input structure
   * 
   * @return the second incoming instance structure (or null if unavailable)
   */
  protected Instances getSecondInputStructure() {
    Instances result = null;

    if (m_secondInput instanceof StructureProducer) {
      result =
        ((StructureProducer) m_secondInput)
          .getStructure(m_secondInputConnectionType);
    }

    return result;
  }

  /**
   * Get the output instances structure given an input event type
   * 
   * @param eventName the name of the input event type
   * @return the output instances structure (or null)
   */
  @Override
  public Instances getStructure(String eventName) {
    if (!eventName.equals("dataSet") && !eventName.equals("instance")) {
      return null;
    }

    if (eventName.equals("dataSet") && m_dataListeners.size() == 0) {
      return null;
    }

    if (eventName.equals("instance") && m_instanceListeners.size() == 0) {
      return null;
    }

    if (m_mergedHeader == null) {
      generateMergedHeader();
    }

    return m_mergedHeader;
  }

  /**
   * Set environment variables to use
   * 
   * @param env the environment variables to use
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }
}
