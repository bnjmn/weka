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
 *    Sorter.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

/**
 * Step for sorting instances according to one or more attributes.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Sorter", category = "Tools",
  toolTipText = "Sort instances in ascending or descending order according "
    + "to the values of user-specified attributes. Instances can be sorted "
    + "according to multiple attributes (defined in order). Handles datasets "
    + "larger than can be fit into main memory via instance connections and "
    + "specifying the in-memory buffer size. Implements a merge-sort by writing "
    + "the sorted in-memory buffer to a file when full and then interleaving "
    + "instances from the disk-based file(s) when the incoming stream has "
    + "finished.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "Sorter.gif")
public class Sorter extends BaseStep {

  private static final long serialVersionUID = 3373283983192467264L;

  /** Comparator that applies the sort rules */
  protected transient SortComparator m_sortComparator;

  /** In memory buffer for incremental operation */
  protected transient List<InstanceHolder> m_incrementalBuffer;

  /** List of sorted temp files for incremental operation */
  protected transient List<File> m_bufferFiles;

  /** Size of the in-memory buffer */
  protected String m_bufferSize = "10000";

  /** Size of the in-memory buffer after resolving any environment vars */
  protected int m_bufferSizeI = 10000;

  /** Holds indexes of string attributes, keyed by attribute name */
  protected Map<String, Integer> m_stringAttIndexes;

  /** Holds the internal textual description of the sort definitions */
  protected String m_sortDetails;

  /**
   * The directory to hold the temp files - if not set the system tmp directory
   * is used
   */
  protected File m_tempDirectory = new File("");

  /** format of instances for current incoming connection (if any) */
  protected Instances m_connectedFormat;

  /** True if we've been reset */
  protected boolean m_isReset;

  /** True if processing streaming data */
  protected boolean m_streaming;

  /** To (re)use when streaming */
  protected Data m_streamingData;

  /**
   * Get the size of the in-memory buffer
   *
   * @return the size of the in-memory buffer
   */
  public String getBufferSize() {
    return m_bufferSize;
  }

  /**
   * Set the size of the in-memory buffer
   *
   * @param buffSize the size of the in-memory buffer
   */
  @OptionMetadata(displayName = "Size of in-mem streaming buffer",
    description = "Number of instances to sort in memory before writing to a "
      + "temp file (instance connections only)", displayOrder = 1)
  public void setBufferSize(String buffSize) {
    m_bufferSize = buffSize;
  }

  /**
   * Set the directory to use for temporary files during incremental operation
   *
   * @param tempDir the temp dir to use
   */
  @FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.OPEN_DIALOG,
    directoriesOnly = true)
  @OptionMetadata(displayName = "Directory for temp files",
    description = "Where to store temporary files when spilling to disk",
    displayOrder = 2)
  public void setTempDirectory(File tempDir) {
    m_tempDirectory = tempDir;
  }

  /**
   * Get the directory to use for temporary files during incremental operation
   *
   * @return the temp dir to use
   */
  public File getTempDirectory() {
    return m_tempDirectory;
  }

  /**
   * Set the sort rules to use
   *
   * @param sortDetails the sort rules in internal string representation
   */
  @ProgrammaticProperty
  public void setSortDetails(String sortDetails) {
    m_sortDetails = sortDetails;
  }

  /**
   * Get the sort rules to use
   *
   * @return the sort rules in internal string representation
   */
  public String getSortDetails() {
    return m_sortDetails;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_streaming = false;
    m_stringAttIndexes = new HashMap<String, Integer>();
    m_bufferFiles = new ArrayList<File>();
    m_streamingData = new Data(StepManager.CON_INSTANCE);
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
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
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
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      result.add(StepManager.CON_DATASET);
    }

    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  /**
   * Initialize given the supplied instances structure
   *
   * @param structure the structure to initialize with
   */
  protected void init(Instances structure) {
    m_connectedFormat = structure;
    List<SortRule> sortRules = new ArrayList<SortRule>();

    if (m_sortDetails != null && m_sortDetails.length() > 0) {
      String[] sortParts = m_sortDetails.split("@@sort-rule@@");

      for (String s : sortParts) {
        SortRule r = new SortRule(s.trim());

        r.init(getStepManager().getExecutionEnvironment()
          .getEnvironmentVariables(), structure);
        sortRules.add(r);
      }

      m_sortComparator = new SortComparator(sortRules);
    }

    // check for string attributes
    m_stringAttIndexes = new HashMap<String, Integer>();
    for (int i = 0; i < structure.numAttributes(); i++) {
      if (structure.attribute(i).isString()) {
        m_stringAttIndexes.put(structure.attribute(i).name(), new Integer(i));
      }
    }
    if (m_stringAttIndexes.size() == 0) {
      m_stringAttIndexes = null;
    }

    if (m_streaming) {
      String buffSize = environmentSubstitute(m_bufferSize);
      m_bufferSizeI = Integer.parseInt(buffSize);
      m_incrementalBuffer = new ArrayList<InstanceHolder>(m_bufferSizeI);
    }
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset) {
      Instances structure;
      if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        Instance inst = data.getPrimaryPayload();
        structure = new Instances(inst.dataset(), 0);
        m_streaming = true;
        getStepManager().logBasic(
          "Starting streaming sort. Using streaming " + "buffer size: "
            + m_bufferSizeI);
        m_isReset = false;
      } else {
        structure = data.getPrimaryPayload();
        structure = new Instances(structure, 0);
      }
      init(structure);
    }

    if (m_streaming) {
      processIncremental(data);
    } else {
      processBatch(data);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (!m_streaming) {
      getStepManager().finished();
    }
  }

  /**
   * Process batch data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected void processBatch(Data data) throws WekaException {
    getStepManager().processing();

    Instances insts = data.getPrimaryPayload();
    getStepManager().logBasic("Sorting " + insts.relationName());
    List<InstanceHolder> instances = new ArrayList<InstanceHolder>();
    for (int i = 0; i < insts.numInstances(); i++) {
      InstanceHolder h = new InstanceHolder();
      h.m_instance = insts.instance(i);
      instances.add(h);
    }
    Collections.sort(instances, m_sortComparator);
    Instances output = new Instances(insts, 0);
    for (int i = 0; i < instances.size(); i++) {
      output.add(instances.get(i).m_instance);
    }

    Data outputD = new Data(data.getConnectionName(), output);
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM));
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM));
    getStepManager().outputData(outputD);
  }

  /**
   * Process incremental data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected void processIncremental(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      emitBufferedInstances();
    } else {
      getStepManager().throughputUpdateStart();
      InstanceHolder tempH = new InstanceHolder();
      tempH.m_instance = data.getPrimaryPayload();
      tempH.m_fileNumber = -1; // unused here
      if (m_stringAttIndexes != null) {
        copyStringAttVals(tempH);
      }
      m_incrementalBuffer.add(tempH);

      if (m_incrementalBuffer.size() == m_bufferSizeI) {
        // time to sort and write this to a temp file
        try {
          sortBuffer(true);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }
      getStepManager().throughputUpdateEnd();
    }
  }

  /**
   * Output any buffered instances
   *
   * @throws WekaException if a problem occurs
   */
  protected void emitBufferedInstances() throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (m_incrementalBuffer.size() > 0) {
      try {
        getStepManager().throughputUpdateStart();
        sortBuffer(false);
        getStepManager().throughputUpdateEnd();
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      if (m_bufferFiles.size() == 0) {
        // we only have the in memory buffer
        getStepManager().logDetailed("Emitting in memory buffer");
        Instances newHeader =
          new Instances(m_incrementalBuffer.get(0).m_instance.dataset(), 0);
        for (int i = 0; i < m_incrementalBuffer.size(); i++) {
          getStepManager().throughputUpdateStart();
          InstanceHolder currentH = m_incrementalBuffer.get(i);
          currentH.m_instance.setDataset(newHeader);
          if (m_stringAttIndexes != null) {
            for (String attName : m_stringAttIndexes.keySet()) {
              boolean setValToZero =
                newHeader.attribute(attName).numValues() > 0;
              newHeader.attribute(attName).setStringValue(
                currentH.m_stringVals.get(attName));
              if (setValToZero) {
                currentH.m_instance.setValue(newHeader.attribute(attName), 0);
              }
            }
          }
          if (isStopRequested()) {
            return;
          }
          m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
            currentH.m_instance);
          getStepManager().throughputUpdateEnd();
          getStepManager().outputData(m_streamingData);
          if (i == m_incrementalBuffer.size() - 1) {
            // end of stream
            m_streamingData.clearPayload();
            getStepManager().throughputFinished(m_streamingData);
          }
        }
        return;
      }
    }

    List<ObjectInputStream> inputStreams = new ArrayList<ObjectInputStream>();
    // for the interleaving part of the merge sort
    List<InstanceHolder> merger = new ArrayList<InstanceHolder>();

    Instances tempHeader = new Instances(m_connectedFormat, 0);

    // add an instance from the in-memory buffer first
    if (m_incrementalBuffer.size() > 0) {
      InstanceHolder tempH = m_incrementalBuffer.remove(0);
      merger.add(tempH);
    }

    if (isStopRequested()) {
      return;
    }

    if (m_bufferFiles.size() > 0) {
      getStepManager().logDetailed("Merging temp files");
    }
    // open all temp buffer files and read one instance from each
    for (int i = 0; i < m_bufferFiles.size(); i++) {
      ObjectInputStream ois = null;
      try {
        FileInputStream fis = new FileInputStream(m_bufferFiles.get(i));
        BufferedInputStream bis = new BufferedInputStream(fis, 50000);
        ois = new ObjectInputStream(bis);

        InstanceHolder tempH = (InstanceHolder) ois.readObject();
        if (tempH != null) {
          inputStreams.add(ois);

          tempH.m_fileNumber = i;
          merger.add(tempH);
        } else {
          // no instances?!??
          ois.close();
        }
      } catch (Exception ex) {
        if (ois != null) {
          try {
            ois.close();
          } catch (Exception e) {
            throw new WekaException(e);
          }
        }
        throw new WekaException(ex);
      }
    }
    Collections.sort(merger, m_sortComparator);

    int mergeCount = 0;
    do {
      if (isStopRequested()) {
        return;
      }
      InstanceHolder holder = merger.remove(0);
      holder.m_instance.setDataset(tempHeader);

      if (m_stringAttIndexes != null) {
        for (String attName : m_stringAttIndexes.keySet()) {
          boolean setValToZero =
            (tempHeader.attribute(attName).numValues() > 1);
          tempHeader.attribute(attName).setStringValue(
            holder.m_stringVals.get(attName));
          if (setValToZero) {
            holder.m_instance.setValue(tempHeader.attribute(attName), 0);
          }
        }
      }

      m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
        holder.m_instance);
      mergeCount++;
      getStepManager().outputData(m_streamingData);
      getStepManager().throughputUpdateStart();

      if (mergeCount % m_bufferSizeI == 0) {
        getStepManager().logDetailed("Merged " + mergeCount + " instances");
      }
      int smallest = holder.m_fileNumber;

      // now get another instance from the source of "smallest"
      InstanceHolder nextH = null;
      if (smallest == -1) {
        if (m_incrementalBuffer.size() > 0) {
          nextH = m_incrementalBuffer.remove(0);
          nextH.m_fileNumber = -1;
        }
      } else {
        ObjectInputStream tis = inputStreams.get(smallest);

        try {
          InstanceHolder tempH = (InstanceHolder) tis.readObject();
          if (tempH != null) {
            nextH = tempH;
            nextH.m_fileNumber = smallest;
          } else {
            throw new Exception("end of buffer");
          }
        } catch (Exception ex) {
          // EOF
          try {
            getStepManager().logDetailed("Closing temp file");
            tis.close();
          } catch (Exception e) {
            throw new WekaException(ex);
          }
          File file = m_bufferFiles.remove(smallest);
          // file.delete();
          inputStreams.remove(smallest);

          // update file numbers
          for (InstanceHolder h : merger) {
            if (h.m_fileNumber != -1 && h.m_fileNumber > smallest) {
              h.m_fileNumber--;
            }
          }
        }
      }

      if (nextH != null) {
        // find the correct position (i.e. interleave) for this new Instance
        int index = Collections.binarySearch(merger, nextH, m_sortComparator);
        if (index < 0) {
          merger.add(index * -1 - 1, nextH);
        } else {
          merger.add(index, nextH);
        }
        nextH = null;
      }
      getStepManager().throughputUpdateEnd();
    } while (merger.size() > 0 && !isStopRequested());

    if (!isStopRequested()) {
      // signal end of stream
      m_streamingData.clearPayload();
      getStepManager().throughputFinished(m_streamingData);
    } else {
      // try an close any input streams still open...
      for (ObjectInputStream is : inputStreams) {
        try {
          is.close();
        } catch (Exception ex) {
          // ignore
        }
      }
    }
  }

  /**
   * Sort the buffer
   *
   * @param write true if the buffer sould be written to a tmp file
   * @throws Exception if a problem occurs
   */
  private void sortBuffer(boolean write) throws Exception {
    getStepManager().logBasic("Sorting in memory buffer");
    Collections.sort(m_incrementalBuffer, m_sortComparator);
    if (!write) {
      return;
    }

    if (isStopRequested()) {
      return;
    }

    String tmpDir = m_tempDirectory.toString();
    File tempFile = File.createTempFile("Sorter", ".tmp");

    if (tmpDir != null && tmpDir.length() > 0) {
      tmpDir = environmentSubstitute(tmpDir);
      File tempDir = new File(tmpDir);
      if (tempDir.exists() && tempDir.canWrite()) {
        String filename = tempFile.getName();
        tempFile = new File(tmpDir + File.separator + filename);
        tempFile.deleteOnExit();
      }
    }
    getStepManager().logDebug("Temp file: " + tempFile.toString());

    m_bufferFiles.add(tempFile);
    FileOutputStream fos = new FileOutputStream(tempFile);
    BufferedOutputStream bos = new BufferedOutputStream(fos, 50000);
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    getStepManager().logDetailed(
      "Writing buffer to temp file " + m_bufferFiles.size()
        + ". Buffer contains " + m_incrementalBuffer.size() + " instances");

    for (int i = 0; i < m_incrementalBuffer.size(); i++) {
      InstanceHolder temp = m_incrementalBuffer.get(i);
      temp.m_instance.setDataset(null);
      oos.writeObject(temp);
      if (i % (m_bufferSizeI / 10) == 0) {
        oos.reset();
      }
    }

    bos.flush();
    oos.close();
    m_incrementalBuffer.clear();
  }

  private void copyStringAttVals(InstanceHolder holder) {
    for (String attName : m_stringAttIndexes.keySet()) {
      Attribute att = holder.m_instance.dataset().attribute(attName);
      String val = holder.m_instance.stringValue(att);

      if (holder.m_stringVals == null) {
        holder.m_stringVals = new HashMap<String, String>();
      }

      holder.m_stringVals.put(attName, val);
    }
  }

  /**
   * Inner class that holds instances and the index of the temp file that holds
   * them (if operating in incremental mode)
   */
  protected static class InstanceHolder implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -3985730394250172995L;

    /** The instance */
    protected Instance m_instance;

    /** index into the list of files on disk */
    protected int m_fileNumber;

    /**
     * for incremental operation, if string attributes are present then we need
     * to store them with each instance - since incremental streaming in the
     * knowledge flow only maintains one string value in memory (and hence in
     * the header) at any one time
     */
    protected Map<String, String> m_stringVals;
  }

  /**
   * Comparator that applies the sort rules to {@code InstanceHolder}s
   */
  protected static class SortComparator implements Comparator<InstanceHolder> {

    /** The rules to apply */
    protected List<SortRule> m_sortRules;

    /**
     * Constructor
     *
     * @param sortRules the rules to apply
     */
    public SortComparator(List<SortRule> sortRules) {
      m_sortRules = sortRules;
    }

    /**
     * @param o1 the first {@code InstanceHolder} to compare
     * @param o2 the second {@code InstanceHolder} to compare
     * @return the result of the comparison - the first rule that returns a
     *         non-zero comparison value
     */
    @Override
    public int compare(InstanceHolder o1, InstanceHolder o2) {

      int cmp = 0;
      for (SortRule sr : m_sortRules) {
        cmp = sr.compare(o1, o2);
        if (cmp != 0) {
          return cmp;
        }
      }

      return 0;
    }
  }

  /**
   * Implements a sorting rule based on a single attribute
   */
  public static class SortRule implements Comparator<InstanceHolder> {

    /** Name or index of the attribute to compare on */
    protected String m_attributeNameOrIndex;

    /** The actual attribute to compare on */
    protected Attribute m_attribute;

    /** True for descending instead of ascending order */
    protected boolean m_descending;

    /**
     * Constructor
     * 
     * @param att the name or index of the attribute to compare on
     * @param descending true if order should be descending
     */
    public SortRule(String att, boolean descending) {
      m_attributeNameOrIndex = att;
      m_descending = descending;
    }

    /**
     * Constructor
     */
    public SortRule() {
    }

    /**
     * Constructor
     *
     * @param setup the definition of a sort rule
     */
    public SortRule(String setup) {
      parseFromInternal(setup);
    }

    protected void parseFromInternal(String setup) {
      String[] parts = setup.split("@@SR@@");

      if (parts.length != 2) {
        throw new IllegalArgumentException("Malformed sort rule: " + setup);
      }

      m_attributeNameOrIndex = parts[0].trim();
      m_descending = parts[1].equalsIgnoreCase("Y");
    }

    /**
     * Gets the rule in internal format
     *
     * @return the rule in internal format
     */
    public String toStringInternal() {
      return m_attributeNameOrIndex + "@@SR@@" + (m_descending ? "Y" : "N");
    }

    /**
     * Prints the rule in human readable format
     *
     * @return a human readable formatted rule
     */
    @Override
    public String toString() {
      StringBuffer res = new StringBuffer();

      res.append("Attribute: " + m_attributeNameOrIndex + " - sort "
        + (m_descending ? "descending" : "ascending"));

      return res.toString();
    }

    /**
     * Set the name or index of the attribute to sort on
     *
     * @param att the name or index of tha attribute to sort on
     */
    public void setAttribute(String att) {
      m_attributeNameOrIndex = att;
    }

    /**
     * Get the name or index of the attribute to sort on
     *
     * @return the name or index of the attribute to sort on
     */
    public String getAttribute() {
      return m_attributeNameOrIndex;
    }

    /**
     * Set whether the sort should be descending rather than ascending
     *
     * @param d true for a descending sort
     */
    public void setDescending(boolean d) {
      m_descending = d;
    }

    /**
     * Return true if the sort is descending
     * 
     * @return true if the sort is descending
     */
    public boolean getDescending() {
      return m_descending;
    }

    /**
     * Initialize the rule
     * 
     * @param env the environment variables to use
     * @param structure the structure of the instances that the rule will
     *          opperate on
     */
    public void init(Environment env, Instances structure) {
      String attNameI = m_attributeNameOrIndex;
      try {
        attNameI = env.substitute(attNameI);
      } catch (Exception ex) {
      }

      if (attNameI.equalsIgnoreCase("/first")) {
        m_attribute = structure.attribute(0);
      } else if (attNameI.equalsIgnoreCase("/last")) {
        m_attribute = structure.attribute(structure.numAttributes() - 1);
      } else {
        // try actual attribute name
        m_attribute = structure.attribute(attNameI);

        if (m_attribute == null) {
          // try as an index
          try {
            int index = Integer.parseInt(attNameI);
            m_attribute = structure.attribute(index);
          } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Unable to locate attribute "
              + attNameI + " as either a named attribute or as a valid "
              + "attribute index");
          }
        }
      }
    }

    /**
     * Compare two instances according to the rule
     *
     * @param o1 the first instance
     * @param o2 the second instance
     * @return the result of the comparison
     */
    @Override
    public int compare(InstanceHolder o1, InstanceHolder o2) {

      // both missing is equal
      if (o1.m_instance.isMissing(m_attribute)
        && o2.m_instance.isMissing(m_attribute)) {
        return 0;
      }

      // one missing - missing instances should all be at the end
      // regardless of whether order is ascending or descending
      if (o1.m_instance.isMissing(m_attribute)) {
        return 1;
      }

      if (o2.m_instance.isMissing(m_attribute)) {
        return -1;
      }

      int cmp = 0;

      if (!m_attribute.isString() && !m_attribute.isRelationValued()) {
        double val1 = o1.m_instance.value(m_attribute);
        double val2 = o2.m_instance.value(m_attribute);

        cmp = Double.compare(val1, val2);
      } else if (m_attribute.isString()) {
        String val1 = o1.m_stringVals.get(m_attribute.name());
        String val2 = o2.m_stringVals.get(m_attribute.name());

        /*
         * String val1 = o1.stringValue(m_attribute); String val2 =
         * o2.stringValue(m_attribute);
         */

        // TODO case insensitive?
        cmp = val1.compareTo(val2);
      } else {
        throw new IllegalArgumentException("Can't sort according to "
          + "relation-valued attribute values!");
      }

      if (m_descending) {
        return -cmp;
      }

      return cmp;
    }
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
    return "weka.gui.knowledgeflow.steps.SorterStepEditorDialog";
  }
}
