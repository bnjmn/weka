package weka.gui.beans;

import distributed.core.DistributedJobConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark",
  toolTipText = "Computes a correlation/covariance matrix for numeric data")
public class CorrelationMatrixSparkJob extends AbstractSparkJob {

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  /** Downstream listeners for image events */
  protected List<ImageListener> m_imageListeners =
    new ArrayList<ImageListener>();

  public CorrelationMatrixSparkJob() {
    super();
    m_job = new weka.distributed.spark.CorrelationMatrixSparkJob();
    m_visual.setText("CorrelationMatrixSparkJob");
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "CorrelationMatrixSparkJob.gif",
      BeanVisual.ICON_PATH + "CorrelationMatrixEvaluationSparkJob.gif");
  }

  /**
   * Help for this KF step
   *
   * @return the help for this step
   */
  public String globalInfo() {
    return "Computes a correlation (or covariance) matrix for numeric data "
      + "in Spark. The data can include a class attribute, which "
      + "can be part of the correlation analysis if it is numeric "
      + "or ignored if it is nominal. The user can optionally have "
      + "the job perform a PCA analysis using the computed "
      + "correlation/covariance matrix as input. Note that this "
      + "is done outside of Spark on the client machine as a "
      + "postprocessing step, so is suitable for data that does not "
      + "conatain a large number of columns. The PCA analysis will "
      + "be written back into the output directory, along "
      + "with a serialized PCA filter that can be used for preprocessing "
      + "data in the WekaClassfierSpark job.";
  }

  @Override
  protected void notifyJobOutputListeners() {
    if (((weka.distributed.spark.CorrelationMatrixSparkJob) m_job)
      .getRunPCA()) {
      String pcaText =
        ((weka.distributed.spark.CorrelationMatrixSparkJob) m_runningJob)
          .getText();

      if (!DistributedJobConfig.isEmpty(pcaText)) {
        for (TextListener t : m_textListeners) {
          t.acceptText(new TextEvent(this, pcaText, "Spark - PCA analysis"));
        }
      }
    }

    Image heatmap =
      ((weka.distributed.spark.CorrelationMatrixSparkJob) m_runningJob)
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
