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
 *    GraphViewer
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

/**
 * Step for collecting and visualizing graph output from Drawable schemes.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "GraphViewer", category = "Visualization",
  toolTipText = "Visualize graph output from Drawable schemes",
  iconPath = StepVisual.BASE_ICON_PATH + "DefaultGraph.gif")
public class GraphViewer extends BaseSimpleDataVisualizer {

  private static final long serialVersionUID = -3256888744740965144L;

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_GRAPH);
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnections() > 0 ? Arrays
      .asList(StepManager.CON_TEXT) : null;
  }

  @Override
  public void processIncoming(Data data) {
    getStepManager().processing();
    String graphTitle =
      data.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE);
    getStepManager().logDetailed(graphTitle);
    m_data.add(data);
    getStepManager().finished();
  }

  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_data.size() > 0) {
      views.put("Show plots",
        "weka.gui.knowledgeflow.steps.GraphViewerInteractiveView");
    }

    return views;
  }
}
