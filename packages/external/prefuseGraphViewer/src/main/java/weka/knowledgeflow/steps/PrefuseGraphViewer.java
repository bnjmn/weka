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
 *    PrefuseGraphViewer
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A step for collecting and visualizing graph output using the prefuse tree and
 * graph visualizer
 *
 * @author Mark Hall
 * @version $Revision: $
 */
@KFStep(name = "PrefuseGraphViewer", category = "Visualization",
  toolTipText = "Visualize graph output from Drawable schemes using the "
    + "prefuse tool", iconPath = StepVisual.BASE_ICON_PATH + "DefaultGraph.gif")
public class PrefuseGraphViewer extends BaseSimpleDataVisualizer {

  private static final long serialVersionUID = 8960956834768128197L;

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
    return Arrays.asList(StepManager.CON_GRAPH);
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
      .asList(StepManager.CON_TEXT) : null;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String graphTitle =
      data.getPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE);
    getStepManager().logDetailed(graphTitle);
    m_data.add(data);

    Data textOut = new Data(StepManager.CON_TEXT, data.getPrimaryPayload());
    textOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, graphTitle);
    getStepManager().outputData(textOut);
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

    if (m_data.size() > 0) {
      views.put("Show plots",
        "weka.gui.knowledgeflow.steps.PrefuseGraphViewerInteractiveView");
    }

    return views;
  }
}
