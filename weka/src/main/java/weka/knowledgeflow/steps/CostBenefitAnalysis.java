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
 *    CostBenefitAnalysis
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.visualize.PlotData2D;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step for storing and viewing threshold data in a cost-benefit visualization
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "CostBenefitAnalysis", category = "Visualization",
  toolTipText = "View threshold data in an interactive cost-benefit visualization",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ModelPerformanceChart.gif")
public class CostBenefitAnalysis extends BaseSimpleDataVisualizer {

  private static final long serialVersionUID = 7756281775575854085L;

  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_THRESHOLD_DATA);
  }

  @Override
  public void processIncoming(Data data) {
    getStepManager().processing();

    PlotData2D pd = data.getPrimaryPayload();

    getStepManager().logDetailed("Processing " + pd.getPlotName());
    m_data.add(data);
    getStepManager().finished();
  }

  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_data.size() > 0) {
      views.put("Show plots",
        "weka.gui.knowledgeflow.steps.CostBenefitAnalysisInteractiveView");
    }

    return views;
  }
}
