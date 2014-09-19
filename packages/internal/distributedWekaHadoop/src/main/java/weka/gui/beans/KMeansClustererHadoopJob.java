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
 *    KMeansClustererHadoopJob.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Knowledge Flow step for exeucting the KMeansClustererHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Hadoop", toolTipText = "Learns a k-means++ clusterer")
public class KMeansClustererHadoopJob extends AbstractHadoopJob {

  /** For serialization */
  private static final long serialVersionUID = -9001697775900513614L;

  /** Downstream listeners for clusterer model output */
  protected List<BatchClustererListener> m_clustererListeners =
    new ArrayList<BatchClustererListener>();

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  /**
   * Constructor
   */
  public KMeansClustererHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.KMeansClustererHadoopJob();
    m_visual.setText("KMeansClustererHadoopJob");
  }

  /**
   * Help information
   * 
   * @return help information
   */
  public String globalInfo() {
    return "Builds a k-means++ model in hadoop.";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "KMeansClustererHadoopJob.gif",
      BeanVisual.ICON_PATH + "KMeansClustererHadoopJob.gif");
  }

  @Override
  public void notifyJobOutputListeners() {
    weka.clusterers.Clusterer finalClusterer =
      ((weka.distributed.hadoop.KMeansClustererHadoopJob) m_runningJob)
        .getClusterer();
    Instances modelHeader =
      ((weka.distributed.hadoop.KMeansClustererHadoopJob) m_runningJob)
        .getTrainingHeader();

    if (finalClusterer == null) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix() + "No clusterer produced!");
      }
    }

    if (modelHeader == null) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix()
          + "No training header available for the model!");
      }
    }

    if (finalClusterer != null) {
      if (m_textListeners.size() > 0) {
        String textual = finalClusterer.toString();

        String title = "Hadoop: ";
        String clustererSpec = finalClusterer.getClass().getName();
        clustererSpec +=
          " "
            + Utils.joinOptions(((OptionHandler) finalClusterer).getOptions());
        title += clustererSpec;
        TextEvent te = new TextEvent(this, textual, title);
        for (TextListener t : m_textListeners) {
          t.acceptText(te);
        }
      }

      if (modelHeader != null) {
        // have to add a single bogus instance to the header to trick
        // the SerializedModelSaver into saving it (since it ignores
        // structure only DataSetEvents) :-)
        double[] vals = new double[modelHeader.numAttributes()];
        for (int i = 0; i < vals.length; i++) {
          vals[i] = Utils.missingValue();
        }
        Instance tempI = new DenseInstance(1.0, vals);
        modelHeader.add(tempI);
        DataSetEvent dse = new DataSetEvent(this, modelHeader);
        BatchClustererEvent be =
          new BatchClustererEvent(this, finalClusterer, dse, 1, 1, 1);
        for (BatchClustererListener b : m_clustererListeners) {
          b.acceptClusterer(be);
        }
      }
    }
  }

  /**
   * Add a batch classifier listener
   * 
   * @param l a batch classifier listener
   */
  public synchronized void addBatchClustererListener(BatchClustererListener l) {
    m_clustererListeners.add(l);
  }

  /**
   * Remove a batch classifier listener
   * 
   * @param l a batch classifier listener
   */
  public synchronized void
    removeBatchClustererListener(BatchClustererListener l) {
    m_clustererListeners.remove(l);
  }

  /**
   * Add a text listener
   * 
   * @param l a text listener
   */
  public synchronized void addTextListener(TextListener l) {
    m_textListeners.add(l);
  }

  /**
   * Remove a text listener
   * 
   * @param l a text listener
   */
  public synchronized void removeTextListener(TextListener l) {
    m_textListeners.remove(l);
  }
}
