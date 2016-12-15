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
 *    TextViewer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A step for collecting and viewing textual data
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "TextViewer", category = "Visualization",
  toolTipText = "View textual output", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "DefaultText.gif")
public class TextViewer extends BaseStep implements DataCollector {

  private static final long serialVersionUID = 8602416209256135064L;

  /** Holds textual results */
  protected Map<String, String> m_results = new LinkedHashMap<String, String>();

  /**
   * The interactive popup viewer registers to receive updates when new textual
   * results arrive
   */
  protected transient TextNotificationListener m_viewerListener;

  /**
   * Initialize the step
   */
  @Override
  public void stepInit() {
    // nothing to do
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
    return Arrays.asList(StepManager.CON_TEXT, StepManager.CON_DATASET,
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
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
    return getStepManager().numIncomingConnections() > 0 ? Arrays
      .asList(StepManager.CON_TEXT) : new ArrayList<String>();
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String title = data.getPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE);

    if (title == null
      && (data.getConnectionName().equals(StepManager.CON_DATASET)
        || data.getConnectionName().equals(StepManager.CON_TRAININGSET) || data
        .getConnectionName().equals(StepManager.CON_TESTSET))) {
      title = ((Instances) data.getPrimaryPayload()).relationName();
    }

    if (title != null) {
      getStepManager().logDetailed("Storing result: " + title);
    }

    String body = data.getPayloadElement(data.getConnectionName()).toString();
    Integer setNum =
      (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    if (title != null && body != null) {
      String name =
        (new SimpleDateFormat("HH:mm:ss.SSS - ")).format(new Date());
      name = name + title + (setNum != null ? " (" + setNum + ")" : "");
      if (m_results.containsKey(name)) {
        try {
          Thread.sleep(5);
          name =
            (new SimpleDateFormat("HH:mm:ss.SSS - ")).format(new Date());
          name = name + title + (setNum != null ? " (" + setNum + ")" : "");
        } catch (InterruptedException e) {
          // ignore
        }
      }
      m_results.put(name, body);
      if (m_viewerListener != null) {
        m_viewerListener.acceptTextResult(name + title
          + (setNum != null ? " (" + setNum + ")" : ""), body);
      }
    }

    Data textData = new Data(StepManager.CON_TEXT, body);
    textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, title);
    // pass on downstream
    getStepManager().outputData(textData);
    getStepManager().finished();
  }

  /**
   * When running in a graphical execution environment a step can make one or
   * more popup Viewer components available. These might be used to display
   * results, graphics etc. Returning null indicates that the step has no such
   * additional graphical views. The map returned by this method should be keyed
   * by action name (e.g. "View results"), and values should be fully qualified
   * names of the corresponding StepInteractiveView implementation. Furthermore,
   * the contents of this map can (and should) be dependent on whether a
   * particular viewer should be made available - i.e. if execution hasn't
   * occurred yet, or if a particular incoming connection type is not present,
   * then it might not be possible to view certain results.
   *
   * Viewers can implement StepInteractiveView directly (in which case they need
   * to extends JPanel), or extends the AbstractInteractiveViewer class. The
   * later extends JPanel, uses a BorderLayout, provides a "Close" button and a
   * method to add additional buttons.
   *
   * @return a map of viewer component names, or null if this step has no
   *         graphical views
   */
  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_viewerListener == null) {
      views.put("Show results",
        "weka.gui.knowledgeflow.steps.TextViewerInteractiveView");
    }

    return views;
  }

  /**
   * Get the textual results stored in this step
   *
   * @return a map of results
   */
  public synchronized Map<String, String> getResults() {
    return m_results;
  }

  /**
   * Get the results stored in this step. Calls {@code getResults()}
   *
   * @return the results (a map of named textual results) as an Object
   */
  @Override
  public Object retrieveData() {
    return getResults();
  }

  /**
   * Restore/set the data in this step
   *
   * @param data the data to set (is expected to be a map of Strings)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void restoreData(Object data) {
    if (!(data instanceof Map)) {
      throw new IllegalArgumentException("Argument must be a Map");
    }
    m_results = (Map<String, String>) data;
  }

  /**
   * Set the listener to be notified about new textual results
   *
   * @param l the listener to receive notifications
   */
  public void setTextNotificationListener(TextNotificationListener l) {
    m_viewerListener = l;
  }

  /**
   * Remove the listener for textual results
   *
   * @param l the listener to remove
   */
  public void removeTextNotificationListener(TextNotificationListener l) {
    if (l == m_viewerListener) {
      m_viewerListener = null;
    }
  }

  /**
   * Interface for listeners of textual results
   */
  public static interface TextNotificationListener {

    /**
     * Accept a new textual result
     *
     * @param name the name of the result
     * @param text the text of the result
     */
    void acceptTextResult(String name, String text);
  }
}
