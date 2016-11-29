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
 *    PairedDataHelper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * A helper class that Step implementations can use when processing paired data
 * (e.g. train and test sets). Has the concept of a primary and secondary
 * connection/data type, where the secondary connection/data for a given set
 * number typically needs to be processed using a result generated from the
 * corresponding primary connection/data. This class takes care of ensuring that
 * the secondary connection/data is only processed once the primary has
 * completed. Users of this helper need to provide an implementation of the
 * PairedProcessor inner interface, where the processPrimary() method will be
 * called to process the primary data/connection (and return a result), and
 * processSecondary() called to deal with the secondary connection/data. The
 * result of execution on a particular primary data set number can be retrieved
 * by calling the getIndexedPrimaryResult() method, passing in the set number of
 * the primary result to retrieve.
 * </p>
 *
 * This class also provides an arbitrary storage mechanism for additional
 * results beyond the primary type of result. It also takes care of invoking
 * processing() and finished() on the client step's StepManager.<br>
 * <br>
 *
 * <pre>
 *     public class MyFunkyStep extends BaseStep
 *       implements PairedDataHelper.PairedProcessor<MyFunkyMainResult> {
 *       ...
 *       protected PairedDataHelper<MyFunkyMainResult> m_helper;
 *       ...
 *       public void stepInit() {
 *         m_helper = new PairedDataHelper<MyFunkyMainResult>(this, this,
 *         StepManager.[CON_WHATEVER_YOUR_PRIMARY_CONNECTION_IS],
 *         StepManager.[CON_WHATEVER_YOUR_SECONDARY_CONNECTION_IS]);
 * 
 *         ...
 *       }
 * 
 *       public void processIncoming(Data data) throws WekaException {
 *         // delegate to our helper to handle primary/secondary synchronization
 *         // issues
 *         m_helper.process(data);
 *       }
 * 
 *       public MyFunkyMainResult processPrimary(Integer setNum, Integer maxSetNun,
 *         Data data, PairedDataHelper<MyFunkyMainResult> helper) throws WekaException {
 *           SomeDataTypeToProcess someData = data.getPrimaryPayload();
 * 
 *           MyFunkyMainResult processor = new MyFunkyMainResult();
 *           // do some processing using MyFunkyMainResult and SomeDataToProcess
 *           ...
 *           // output some data to downstream steps if necessary
 *           ...
 * 
 *           return processor;
 *       }
 * 
 *       public void processSecondary(Integer setNum, Integer maxSetNum, Data data,
 *         PairedDataHelper<MyFunkyMainResult> helper) throws WekaException {
 *         SomeDataTypeToProcess someData = data.getPrimaryPayload();
 * 
 *         // get the MyFunkyMainResult for this set number
 *         MyFunkyMainResult result = helper.getIndexedPrimaryResult(setNum);
 * 
 *         // do some stuff with the result and the secondary data
 *         ...
 *         // output some data to downstream steps if necessary
 *       }
 *     }
 * </pre>
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class PairedDataHelper<P> implements java.io.Serializable {

  /** For serialization */
  private static final long serialVersionUID = -7813465607881227514L;

  /**
   * Storage of arbitrary indexed results computed during execution of
   * PairedProcessor.processPrimary()
   */
  protected Map<String, Map<Integer, Object>> m_namedIndexedStore =
    new ConcurrentHashMap<String, Map<Integer, Object>>();

  /** Storage of the indexed primary result */
  protected Map<Integer, P> m_primaryResultMap =
    new ConcurrentHashMap<Integer, P>();

  /**
   * Holds the secondary data objects, if they arrive before the corresponding
   * primary has been computed
   */
  protected Map<Integer, Data> m_secondaryDataMap =
    new ConcurrentHashMap<Integer, Data>();

  /** The type of connection to route to PairedProcessor.processPrimary() */
  protected String m_primaryConType;

  /** The type of connection to route to PairedProcessor.processSecondary() */
  protected String m_secondaryConType;

  /** The PairedProcessor implementation that will do the actual work */
  protected transient PairedProcessor m_processor;

  /** The step that owns this helper */
  protected transient Step m_ownerStep;

  /** Keep track of completed primary/secondary pairs */
  protected transient AtomicInteger m_setCount;

  /**
   * Constructor
   * 
   * @param owner the owner step
   * @param processor the PairedProcessor implementation
   * @param primaryConType the primary connection type
   * @param secondaryConType the secondary connection type
   */
  public PairedDataHelper(Step owner, PairedProcessor processor,
    String primaryConType, String secondaryConType) {
    m_primaryConType = primaryConType;
    m_secondaryConType = secondaryConType;
    m_ownerStep = owner;
    m_processor = processor;
  }

  /**
   * Initiate routing and processing for a particular data object
   * 
   * @param data the data object to process
   * @throws WekaException if a problem occurs
   */
  public void process(Data data) throws WekaException {
    if (m_ownerStep.getStepManager().isStopRequested()) {
      m_ownerStep.getStepManager().interrupted();
      return;
    }
    String connType = data.getConnectionName();
    if (connType.equals(m_primaryConType)) {
      processPrimary(data);
    } else if (m_secondaryConType != null
      && connType.equals(m_secondaryConType)) {
      processSecondary(data);
    } else {
      throw new WekaException("Illegal connection/data type: " + connType);
    }

    if (!m_ownerStep.getStepManager().isStopRequested()) {
      if (m_setCount != null && m_setCount.get() == 0) {
        m_ownerStep.getStepManager().finished();
        // save memory
        m_primaryResultMap.clear();
        m_secondaryDataMap.clear();
        m_namedIndexedStore.clear();
      }
    } else {
      m_ownerStep.getStepManager().interrupted();
    }
  }

  /**
   * Handle the processing of the primary data/connection. Performs
   * initialization in the case of receiving the first data object in a batch.
   * Delegates actual processing work to the PairedProcessor.
   * 
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  private void processPrimary(Data data) throws WekaException {
    Integer setNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    if (m_setCount == null) {
      m_setCount = new AtomicInteger(maxSetNum);
    }

    if (setNum == 1) {
      m_ownerStep.getStepManager().processing();
      m_ownerStep.getStepManager().statusMessage(
        "Processing set/fold " + setNum + " out of " + maxSetNum);
    }

    if (!m_ownerStep.getStepManager().isStopRequested()) {
      P result = (P) m_processor.processPrimary(setNum, maxSetNum, data, this);
      if (result != null) {
        m_primaryResultMap.put(setNum, result);
      }
    } else {
      m_ownerStep.getStepManager().interrupted();
      return;
    }

    Data waitingSecondary = m_secondaryDataMap.get(setNum);
    if (waitingSecondary != null) {
      processSecondary(waitingSecondary);
    } else if (m_secondaryConType == null) {
      // no secondary connection
      m_setCount.decrementAndGet();
    }
  }

  /**
   * Handle processing of the secondary data/connection. Stores the secondary if
   * there is no corresponding primary result generated yet. Delegates actual
   * processing work to the PairedProcessor
   * 
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  private synchronized void processSecondary(Data data) throws WekaException {
    Integer setNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

    P primaryData = m_primaryResultMap.get(setNum);
    if (primaryData == null) {
      // store, ready for the arrival of the matching primary data
      m_secondaryDataMap.put(setNum, data);
      return;
    }

    if (!m_ownerStep.getStepManager().isStopRequested()) {
      m_processor.processSecondary(setNum, maxSetNum, data, this);
    } else {
      m_ownerStep.getStepManager().interrupted();
      return;
    }

    m_setCount.decrementAndGet();
  }

  /**
   * Retrieve the primary result corresponding to a given set number
   *
   * @param index the set number of the result to get
   * @return the primary result
   */
  public P getIndexedPrimaryResult(int index) {
    return m_primaryResultMap.get(index);
  }

  /**
   * Reset the helper. The helper must be reset between runs if it is being
   * re-used (as opposed to a new helper instance being created).
   */
  public void reset() {
    // dont' reset if we're still processing!
    if (m_setCount != null && m_setCount.get() > 0
      && !m_ownerStep.getStepManager().isStopRequested()) {
      return;
    }
    m_setCount = null;
  }

  /**
   * Return true if there is no further processing to be done
   *
   * @return true if processing is done
   */
  public boolean isFinished() {
    return m_setCount.get() == 0;
  }

  /**
   * Create a indexed store with a given name
   * 
   * @param name the name of the store to create
   */
  public void createNamedIndexedStore(String name) {
    m_namedIndexedStore.put(name, new ConcurrentHashMap<Integer, Object>());
  }

  /**
   * Gets an indexed value from a named store
   * 
   * @param storeName the name of the store to retrieve from
   * @param index the index of the value to get
   * @param <T> the type of the value
   * @return the requested value or null if either the store does not exist or
   *         the value does not exist in the store.
   */
  @SuppressWarnings("unchecked")
  public <T> T getIndexedValueFromNamedStore(String storeName, Integer index) {
    Map<Integer, Object> store = m_namedIndexedStore.get(storeName);
    if (store != null) {
      return (T) store.get(index);
    }

    return null;
  }

  /**
   * Adds a value to a named store with the given index. Creates the named store
   * if it doesn't already exist.
   *
   * @param storeName the name of the store to add to
   * @param index the index to associate with the value
   * @param value the value to store
   */
  public synchronized void addIndexedValueToNamedStore(String storeName,
    Integer index, Object value) {
    Map<Integer, Object> store = m_namedIndexedStore.get(storeName);
    if (store == null) {
      createNamedIndexedStore(storeName);
      store = m_namedIndexedStore.get(storeName);
    }
    store.put(index, value);
  }

  /**
   * Interface for processors of paired data to implement. See the description
   * in the class documentation of PairedDataHelper.
   */
  public interface PairedProcessor<P> {
    P processPrimary(Integer setNum, Integer maxSetNum, Data data,
      PairedDataHelper<P> helper) throws WekaException;

    void processSecondary(Integer setNum, Integer maxSetNum, Data data,
      PairedDataHelper<P> helper) throws WekaException;
  }
}
