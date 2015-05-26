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
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

/**
 * Knowledge Flow step for executing the WekaScoringHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Hadoop", toolTipText = "Scores data using a Weka model")
public class WekaScoringHadoopJob extends AbstractHadoopJob {

  /** For serialization */
  private static final long serialVersionUID = 2877175777737840036L;

  /**
   * Constructor
   */
  public WekaScoringHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.WekaScoringHadoopJob();
    m_visual.setText("WekaScoringHadoopJob");
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
    m_visual.loadIcons(BeanVisual.ICON_PATH + "WekaClassifierHadoopJob.gif",
      BeanVisual.ICON_PATH + "WekaClassifierHadoopJob.gif");
  }
}
