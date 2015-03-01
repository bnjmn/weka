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
 *    WekaScoringSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Knowledge flow step for the Weka scoring Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark", toolTipText = "Scores data using a Weka model")
public class WekaScoringSparkJob extends AbstractSparkJob {

  public WekaScoringSparkJob() {
    super();
    m_job = new weka.distributed.spark.WekaScoringSparkJob();
    m_visual.setText("WekaScoringSparkJob");
  }

  /**
   * Help information
   *
   * @return help information
   */
  public String globalInfo() {
    return "Scores data using a Weka model. Handles Weka classifiers and "
      + "clusterers. User can opt to output all or some of the "
      + "original input attributes in the scored data.";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif",
      BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif");
  }
}
