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
 *    RandomizedDataSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Knowledge flow step for the randomly shuffle/stratify data Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark",
  toolTipText = "Creates a randomly shuffled (and stratified) dataset")
public class RandomizedDataSparkJob extends AbstractSparkJob {

  public RandomizedDataSparkJob() {
    super();
    m_job = new weka.distributed.spark.RandomizedDataSparkJob();
    m_visual.setText("RandomlyShuffleDataSparkJob");
  }

  public String globalInfo() {
    return "Creates randomly shuffled (and stratified if a nominal class is set)"
      + " data chunks. As a sub-task, also computes quartiles for numeric"
      + " attributes and updates the summary attributes in the ARFF header";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "RandomizedDataSparkJob.gif", BeanVisual.ICON_PATH
      + "RandomizedDataSparkJob.gif");
  }
}
