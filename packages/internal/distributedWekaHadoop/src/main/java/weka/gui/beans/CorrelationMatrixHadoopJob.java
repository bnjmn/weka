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
 *    CorrelationMatrixHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import distributed.core.DistributedJobConfig;

/**
 * Knowledge Flow step for executing the CorrelationMatrixHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Hadoop",
  toolTipText = "Computes a correlation/covariance matrix for numeric data")
public class CorrelationMatrixHadoopJob extends AbstractHadoopJob {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3480843012291224815L;

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  /** Downstream listeners for image events */
  protected List<ImageListener> m_imageListeners =
    new ArrayList<ImageListener>();

  /**
   * Constructor
   */
  public CorrelationMatrixHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.CorrelationMatrixHadoopJob();
    m_visual.setText("CorrelationMatrixHadoopJob");
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "CorrelationMatrixHadoopJob.gif",
      BeanVisual.ICON_PATH + "CorrelationMatrixEvaluationHadoopJob.gif");
  }

  /**
   * Help for this KF step
   * 
   * @return the help for this step
   */
  public String globalInfo() {
    return "Computes a correlation (or covariance) matrix for numeric data "
      + "in Hadoop. The data can include a class attribute, which "
      + "can be part of the correlation analysis if it is numeric "
      + "or ignored if it is nominal. The user can optionally have "
      + "the job perform a PCA analysis using the computed "
      + "correlation/covariance matrix as input. Note that this "
      + "is done outside of Hadoop on the client machine as a "
      + "postprocessing step, so is suitable for data that does not "
      + "conatain a large number of columns. The PCA analysis will "
      + "be written back into the output directory in Hadoop, along "
      + "with a serialized PCA filter that can be used for preprocessing "
      + "data in the WekaClassfierHadoop job.";
  }

  @Override
  protected void notifyJobOutputListeners() {
    if (((weka.distributed.hadoop.CorrelationMatrixHadoopJob) m_job)
      .getRunPCA()) {
      String pcaText =
        ((weka.distributed.hadoop.CorrelationMatrixHadoopJob) m_runningJob)
          .getText();

      if (!DistributedJobConfig.isEmpty(pcaText)) {
        for (TextListener t : m_textListeners) {
          t.acceptText(new TextEvent(this, pcaText, "Hadoop - PCA analysis"));
        }
      }
    }

    Image heatmap =
      ((weka.distributed.hadoop.CorrelationMatrixHadoopJob) m_runningJob)
        .getImage();
    if (heatmap != null) {
      for (ImageListener i : m_imageListeners) {
        i.acceptImage(new ImageEvent(this, (BufferedImage) heatmap));
      }
    }
  }

  /**
   * Add a text listener
   * 
   * @param l the text listener to add
   */
  public synchronized void addTextListener(TextListener l) {
    m_textListeners.add(l);
  }

  /**
   * Remove a text listener
   * 
   * @param l the text listener to remove
   */
  public synchronized void removeTextListener(TextListener l) {
    m_textListeners.remove(l);
  }

  /**
   * Add an image listener
   * 
   * @param l the image listener to add
   */
  public synchronized void addImageListener(ImageListener l) {
    m_imageListeners.add(l);
  }

  /**
   * Remove an image listener
   * 
   * @param l an image listener
   */
  public synchronized void removeImageListener(ImageListener l) {
    m_imageListeners.remove(l);
  }
}
