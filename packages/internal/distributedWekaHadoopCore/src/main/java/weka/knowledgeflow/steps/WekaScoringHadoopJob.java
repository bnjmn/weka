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
 *    WekaScoringHadoopJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * Knowledge Flow step for the scoring Spark job.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "WekaScoringHadoopJob", category = "Hadoop",
  toolTipText = "Scores data using a Weka model",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "WekaScoringHadoopJob.gif")
public class WekaScoringHadoopJob extends AbstractHadoopJob {

  private static final long serialVersionUID = 4818114415560956347L;

  /**
   * Constructor
   */
  public WekaScoringHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.WekaScoringHadoopJob();
  }
}
