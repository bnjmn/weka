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
  protected Map<String, String> m_results = new LinkedHashMap<String, String>();

  protected transient TextNotificationListener m_viewerListener;

  @Override
  public void stepInit() {
    // nothing to do
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_TEXT, StepManager.CON_DATASET,
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnections() > 0 ? Arrays
      .asList(StepManager.CON_TEXT) : new ArrayList<String>();
  }

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
      m_results.put(name + title + (setNum != null ? " (" + setNum + ")" : ""),
        body);
      if (m_viewerListener != null) {
        m_viewerListener.acceptTextResult(name + title
          + (setNum != null ? " (" + setNum + ")" : ""), body);
      }
    }

    // pass on downstream
    getStepManager().outputData(data);
    getStepManager().finished();
  }

  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_viewerListener == null) {
      views.put("Show results",
        "weka.gui.knowledgeflow.steps.TextViewerInteractiveView");
    }

    return views;
  }

  public synchronized Map<String, String> getResults() {
    return m_results;
  }

  @Override
  public Object retrieveData() {
    return getResults();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void restoreData(Object data) {
    if (!(data instanceof Map)) {
      throw new IllegalArgumentException("Argument must be a Map");
    }
    m_results = (Map<String, String>) data;
  }

  public void setTextNotificationListener(TextNotificationListener l) {
    m_viewerListener = l;
  }

  public void removeTextNotificationListener(TextNotificationListener l) {
    if (l == m_viewerListener) {
      m_viewerListener = null;
    }
  }

  public static interface TextNotificationListener {
    void acceptTextResult(String name, String text);
  }
}
