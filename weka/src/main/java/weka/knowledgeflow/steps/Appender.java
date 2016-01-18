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
 *    Appender.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.converters.SerializedInstancesLoader;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bean that appends multiple incoming data connections into a single data
 * set. The incoming connections can be either all instance connections or all
 * batch-oriented connections (i.e. data set, training set and test set).
 * Instance and batch connections can't be mixed. An amalgamated output is
 * created that is a combination of all the incoming attributes. Missing values
 * are used to fill columns that don't exist in a particular incoming data set.
 * If all incoming connections are instance connections, then the outgoing
 * connection must be an instance connection (and vice versa for incoming batch
 * connections).
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Appender", category = "Flow",
  toolTipText = "Append multiple sets of instances",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "Appender.png")
public class Appender extends BaseStep {

  private static final long serialVersionUID = -3003135257112845998L;

  /**
   * Used to keep track of how many upstream steps have sent us complete data
   * sets (batch) or headers (incremental) so far.
   */
  protected Map<Step, Instances> m_completed;

  /** Handles on temp files used to store batches of instances in batch mode */
  protected Map<Step, File> m_tempBatchFiles;

  /** Used to hold the final header in the case of incremental operation */
  protected Instances m_completeHeader;

  /** Gets decremented for each incoming instance stream that has finished */
  protected AtomicInteger m_streamingCountDown;

  /**
   * Holds savers used for incrementally saving incoming instance streams. After
   * we've seen the structure from each incoming connection we can create the
   * final output structure, pull any saved instances from the temp files and
   * discard these savers as they will no longer be needed.
   */
  protected transient Map<Step, ObjectOutputStream> m_incrementalSavers;

  /** Holds a files in play for incremental incoming connections */
  protected transient Map<Step, File> m_incrementalFiles;

  /** Re-usable data object for streaming mode */
  protected Data m_streamingData;

  /** True if this step has been reset */
  protected boolean m_isReset;

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_completed = new HashMap<Step, Instances>();
    m_tempBatchFiles = new HashMap<Step, File>();
    m_completeHeader = null;
    m_incrementalSavers = new HashMap<Step, ObjectOutputStream>();
    m_incrementalFiles = new HashMap<Step, File>();
    m_streamingCountDown = new AtomicInteger(
      getStepManager().numIncomingConnectionsOfType(StepManager.CON_INSTANCE));
    m_streamingData = new Data(StepManager.CON_INSTANCE);
  }

  /**
   * Get the incoming connection types accepted by this step at this time
   *
   * @return a list of incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() == 0 || getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) == 0) {
      result.addAll(Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET));
    }

    if (getStepManager().numIncomingConnections() == 0 || getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    }

    return result;
  }

  /**
   * Get a list of outgoing connection types that this step can produce at this
   * time
   * 
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INSTANCE) > 0) {
      result.add(StepManager.CON_INSTANCE);
    } else {
      result.addAll(Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET));
    }

    return result;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset
      && !data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
      getStepManager().processing();
      m_isReset = false;
    }

    if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
      processStreaming(data);

      if (m_streamingCountDown.get() == 0) {
        // all done
        m_streamingData.clearPayload();
        getStepManager().throughputFinished(m_streamingData);
      }
    } else {
      processBatch(data);
      if (m_completed.size() == getStepManager().numIncomingConnections()) {
        // done
        getStepManager().finished();
        // save memory
        m_completed.clear();
        m_tempBatchFiles.clear();
      }
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
      // save memory
      m_completed.clear();
      m_tempBatchFiles.clear();
      m_incrementalSavers.clear();
      m_incrementalFiles.clear();
    }
  }

  /**
   * Process batch data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected synchronized void processBatch(Data data) throws WekaException {
    Integer setNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    Instances insts = data.getPrimaryPayload();

    if (setNum > 1 || maxSetNum > 1) {
      // can't accept more than one dataset/batch from a particular source
      throw new WekaException("Source " + data.getSourceStep().getName() + " "
        + "is generating more than one " + data.getConnectionName() + " "
        + "in a batch");
    }

    Instances header = new Instances(insts, 0);
    m_completed.put(data.getSourceStep(), header);
    // write these instances (serialized) to a temp file
    try {
      File tmpF =
        File.createTempFile("weka", SerializedInstancesLoader.FILE_EXTENSION);
      // tmpF.deleteOnExit();
      ObjectOutputStream oos = new ObjectOutputStream(
        new BufferedOutputStream(new FileOutputStream(tmpF)));
      oos.writeObject(insts);
      oos.flush();
      oos.close();

      m_tempBatchFiles.put(data.getSourceStep(), tmpF);
    } catch (IOException e1) {
      throw new WekaException(e1);
    }

    if (isStopRequested()) {
      return;
    }

    // have we seen a dataset from every incoming connection?
    if (m_completed.size() == getStepManager().numIncomingConnections()) {
      // process all headers and create mongo header for new output.
      // missing values will fill columns that don't exist in particular data
      // sets
      Instances output = makeOutputHeader();
      getStepManager().logDetailed("Making output header structure");

      try {
        for (File f : m_tempBatchFiles.values()) {
          ObjectInputStream ois = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(f)));
          Instances temp = (Instances) ois.readObject();
          ois.close();

          // copy each instance over
          for (int i = 0; i < temp.numInstances(); i++) {
            Instance converted = makeOutputInstance(output, temp.instance(i));
            output.add(converted);
          }
        }

        Data outputD = new Data(data.getConnectionName(), output);
        outputD.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
        outputD.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
        getStepManager().outputData(outputD);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }
  }

  /**
   * Process streaming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected synchronized void processStreaming(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    Step source = data.getSourceStep();
    Instance inst = data.getPrimaryPayload();
    if (!m_completed.containsKey(source)) {
      m_completed.put(source, inst.dataset());
    }

    if (m_completed.size() == getStepManager().numIncomingConnections()
      && m_completeHeader == null) {
      // create mondo header...
      getStepManager().logDetailed("Creating output header structure");
      m_completeHeader = makeOutputHeader();

      // now check for any buffered instances
      if (m_incrementalSavers.size() > 0) {
        // read in and convert these instances now
        for (Map.Entry<Step, ObjectOutputStream> e : m_incrementalSavers
          .entrySet()) {
          // for (ObjectOutputStream s : m_incrementalSavers.values()) {
          ObjectOutputStream s = e.getValue();
          // finish off the saving process first
          try {
            // s.writeIncremental(null);
            s.flush();
            s.close();

            // File tmpFile = s.retrieveFile();
            File tmpFile = m_incrementalFiles.get(e.getKey());
            ObjectInputStream ois = new ObjectInputStream(
              new BufferedInputStream(new FileInputStream(tmpFile)));
            Instance tmpLoaded = null;
            do {
              try {
                tmpLoaded = (Instance) ois.readObject();
                Instance converted =
                  makeOutputInstance(m_completeHeader, tmpLoaded);
                m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
                  converted);
                getStepManager().outputData(m_streamingData);
              } catch (Exception ex) {
                // EOF
                ois.close();
                break;
              }
            } while (tmpLoaded != null);

            /*
             * ArffLoader loader = new ArffLoader(); loader.setFile(tmpFile);
             * Instances tempStructure = loader.getStructure(); Instance
             * tempLoaded = loader.getNextInstance(tempStructure); while
             * (tempLoaded != null) { Instance converted =
             * makeOutputInstance(m_completeHeader, tempLoaded);
             * m_streamingData.setPayloadElement(StepManager.CON_INSTANCE,
             * converted); getStepManager().outputData(data);
             * 
             * tempLoaded = loader.getNextInstance(tempStructure); }
             */
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }
        m_incrementalSavers.clear();
        m_incrementalFiles.clear();
      }
    }

    if (isStopRequested()) {
      return;
    }

    if (getStepManager().isStreamFinished(data)) {
      m_streamingCountDown.decrementAndGet();
      return;
    }

    if (m_completeHeader == null) {

      ObjectOutputStream saver = m_incrementalSavers.get(data.getSourceStep());
      if (saver == null) {
        try {
          File tmpFile = File.createTempFile("weka", ".arff");
          saver = new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(tmpFile)));
          m_incrementalSavers.put(data.getSourceStep(), saver);
          m_incrementalFiles.put(data.getSourceStep(), tmpFile);
        } catch (IOException ex) {
          throw new WekaException(ex);
        }
      }

      // ArffSaver saver = m_incrementalSavers.get(data.getSourceStep());
      // if (saver == null) {
      /*
       * saver = new ArffSaver(); try { File tmpFile =
       * File.createTempFile("weka", ".arff"); saver.setFile(tmpFile);
       * saver.setRetrieval(weka.core.converters.Saver.INCREMENTAL);
       * saver.setInstances(new Instances(inst.dataset(), 0));
       * m_incrementalSavers.put(data.getSourceStep(), saver); } catch
       * (IOException e1) { throw new WekaException(e1); }
       */

      try {
        // saver.writeIncremental(inst);
        saver.writeObject(inst);
      } catch (IOException e1) {
        throw new WekaException(e1);
      }
      // }
    } else {
      Instance newI = makeOutputInstance(m_completeHeader, inst);
      m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, newI);
      getStepManager().outputData(m_streamingData);
    }
  }

  /**
   * Makes an output instance
   *
   * @param output the structure of the output
   * @param source the source instance
   * @return an output instance
   */
  private Instance makeOutputInstance(Instances output, Instance source) {

    double[] newVals = new double[output.numAttributes()];
    for (int i = 0; i < newVals.length; i++) {
      newVals[i] = Utils.missingValue();
    }

    for (int i = 0; i < source.numAttributes(); i++) {
      if (!source.isMissing(i)) {
        Attribute s = source.attribute(i);
        int outputIndex = output.attribute(s.name()).index();
        if (s.isNumeric()) {
          newVals[outputIndex] = source.value(s);
        } else if (s.isString()) {
          String sVal = source.stringValue(s);
          newVals[outputIndex] =
            output.attribute(outputIndex).addStringValue(sVal);
        } else if (s.isRelationValued()) {
          Instances rVal = source.relationalValue(s);
          newVals[outputIndex] =
            output.attribute(outputIndex).addRelation(rVal);
        } else if (s.isNominal()) {
          String nomVal = source.stringValue(s);
          newVals[outputIndex] =
            output.attribute(outputIndex).indexOfValue(nomVal);
        }
      }
    }

    Instance newInst = new DenseInstance(source.weight(), newVals);
    newInst.setDataset(output);

    return newInst;
  }

  /**
   * Create the structure of the output
   *
   * @return the structure of the output as a header-only set of instances
   * @throws WekaException if a problem occurs
   */
  protected Instances makeOutputHeader() throws WekaException {
    return makeOutputHeader(m_completed.values());
  }

  /**
   * Create the structure of the output given a collection of input structures
   *
   * @param headers a collection of incoming instance structures
   * @return the structure of the output as a header-only set of instances
   * @throws WekaException if a problem occurs
   */
  protected Instances makeOutputHeader(Collection<Instances> headers)
    throws WekaException {
    // process each header in turn...
    Map<String, Attribute> attLookup = new HashMap<String, Attribute>();
    List<Attribute> attList = new ArrayList<Attribute>();
    Map<String, Set<String>> nominalLookups =
      new HashMap<String, Set<String>>();
    for (Instances h : headers) {
      for (int i = 0; i < h.numAttributes(); i++) {
        Attribute a = h.attribute(i);
        if (!attLookup.containsKey(a.name())) {
          attLookup.put(a.name(), a);
          attList.add(a);
          if (a.isNominal()) {
            TreeSet<String> nVals = new TreeSet<String>();
            for (int j = 0; j < a.numValues(); j++) {
              nVals.add(a.value(j));
            }
            nominalLookups.put(a.name(), nVals);
          }
        } else {
          Attribute storedVersion = attLookup.get(a.name());
          // mismatched types between headers - can't continue
          if (storedVersion.type() != a.type()) {
            throw new WekaException("Conflicting types for attribute "
              + "name '" + a.name() + "' between incoming " + "instance sets");
          }

          if (storedVersion.isNominal()) {
            Set<String> storedVals = nominalLookups.get(a.name());
            for (int j = 0; j < a.numValues(); j++) {
              storedVals.add(a.value(j));
            }
          }
        }
      }
    }

    ArrayList<Attribute> finalAttList = new ArrayList<Attribute>();
    for (Attribute a : attList) {
      Attribute newAtt = null;
      if (a.isDate()) {
        newAtt = new Attribute(a.name(), a.getDateFormat());
      } else if (a.isNumeric()) {
        newAtt = new Attribute(a.name());
      } else if (a.isRelationValued()) {
        newAtt = new Attribute(a.name(), a.relation());
      } else if (a.isNominal()) {
        Set<String> vals = nominalLookups.get(a.name());
        List<String> newVals = new ArrayList<String>();
        for (String v : vals) {
          newVals.add(v);
        }
        newAtt = new Attribute(a.name(), newVals);
      } else if (a.isString()) {
        newAtt = new Attribute(a.name(), (List<String>) null);
      }

      finalAttList.add(newAtt);
    }

    return new Instances(
      "Appended_" + getStepManager().numIncomingConnections() + "_sets",
      finalAttList, 0);
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   * 
   * @param connectionName the name of the connection to get the output structure for
   * @return the output structure or null if it can't be produced
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (getStepManager().numIncomingConnections() > 0) {
      List<Instances> incomingHeaders = new ArrayList<Instances>();
      for (Map.Entry<String, List<StepManager>> e : getStepManager()
        .getIncomingConnections().entrySet()) {
        if (e.getValue().size() > 0) {
          String incomingConType = e.getKey();
          for (StepManager sm : e.getValue()) {
            Instances incomingStruc = getStepManager()
              .getIncomingStructureFromStep(sm, incomingConType);
            if (incomingStruc == null) {
              // can't determine final output structure if any incoming
              // structures are null at present
              return null;
            }
            incomingHeaders.add(incomingStruc);
          }
        }
      }
      if (incomingHeaders.size() > 0) {
        return makeOutputHeader(incomingHeaders);
      }
    }

    return null;
  }
}
