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
 *    BaseSimpleDataVisualizer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Abstract base class for simple data visualization steps that just collect
 * data sets for visualization.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class BaseSimpleDataVisualizer extends BaseStep implements
  DataCollector {

  private static final long serialVersionUID = 4955068920302509451L;

  /** The datasets seen so far */
  protected List<Data> m_data = new ArrayList<Data>();

  @Override
  public void stepInit() throws WekaException {
    // Nothing to do
  }

  /**
   * Process incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    processIncoming(data, true);
  }

  /**
   * Process incoming data. Subclasses can override as necessary
   *
   * @param data the data to process
   * @param notifyFinished true to notify the Knowledge Flow environment that we
   *          have finished processing
   */
  protected synchronized void
    processIncoming(Data data, boolean notifyFinished) {
    getStepManager().processing();
    Instances toPlot = data.getPrimaryPayload();
    String name = (new SimpleDateFormat("HH:mm:ss.SSS - ")).format(new Date());
    String title = name + toPlot.relationName();
    int setNum = data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    int maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);

    title += " set " + setNum + " of " + maxSetNum;
    getStepManager().logDetailed("Processing " + title);
    data.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
    m_data.add(data);

    if (notifyFinished) {
      getStepManager().finished();
    }
  }

  /**
   * Get a list of incoming connection types that this step can accept at this
   * time
   * 
   * @return a list of incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
  }

  /**
   * Get a list of outgoing connection types that this step can produce at this
   * time. Subclasses to override (if necessary). This default implementation
   * returns null (i.e. does not produce any outgoing data).
   * 
   * @return a list of outgoing connection types that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return null;
  }

  /**
   * Get the datasets seen so far
   *
   * @return a list of datasets
   */
  public List<Data> getDatasets() {
    return m_data;
  }

  @Override
  public Object retrieveData() {
    return getDatasets();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void restoreData(Object data) throws WekaException {
    if (!(data instanceof List)) {
      throw new WekaException("Was expecting an instance of a List");
    }

    m_data = ((List<Data>) data);
  }
}
