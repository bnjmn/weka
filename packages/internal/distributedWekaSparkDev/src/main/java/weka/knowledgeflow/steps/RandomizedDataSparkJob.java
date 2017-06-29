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
 *    RandomizedDataSparkStep
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * Knowledge flow step for the randomly shuffle/stratify data Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "RandomlyShuffleDataSparkJob", category = "Spark",
  toolTipText = "Creates a randomly shuffled (and stratified) dataset",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "RandomizedDataSparkJob.gif")
public class RandomizedDataSparkJob extends AbstractSparkJob {

  private static final long serialVersionUID = -5501163893364663805L;

  public RandomizedDataSparkJob() {
    super();
    m_job = new weka.distributed.spark.RandomizedDataSparkJob();
  }
}
