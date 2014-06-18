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
 *    RandomizedDataChunkJob
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Knowledge Flow step for executing the RandomizedDataChunkJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Hadoop",
  toolTipText = "Creates randomly shuffled (and stratified) data chunks")
public class RandomizedDataChunkHadoopJob extends AbstractHadoopJob {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3578138830742930565L;

  public RandomizedDataChunkHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.RandomizedDataChunkHadoopJob();
    m_visual.setText("RandomizedDataChunkJob");
  }

  public String globalInfo() {
    return "Creates randomly shuffled (and stratified if a nominal class is set)"
      + " data chunks. As a sub-task, also computes quartiles for numeric"
      + " attributes and updates the summary attributes in the ARFF header";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "RandomizedDataChunkHadoopJob.gif", BeanVisual.ICON_PATH
      + "RandomizedDataChunkHadoopJob.gif");
  }
}
