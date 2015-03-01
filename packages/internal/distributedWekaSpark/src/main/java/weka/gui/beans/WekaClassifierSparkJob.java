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
 *    WekaClassifierSparkSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import weka.core.*;

/**
 * Knowledge flow step for the Weka classifier Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark",
  toolTipText = "Builds an aggregated Weka classifier")
public class WekaClassifierSparkJob extends AbstractSparkJob {

  /** Downstream listeners for classifier model output */
  protected List<BatchClassifierListener> m_classifierListeners =
    new ArrayList<BatchClassifierListener>();

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  public WekaClassifierSparkJob() {
    super();

    m_job = new weka.distributed.spark.WekaClassifierSparkJob();
    m_visual.setText("WekaClassifierSparkJob");
  }

  public String globalInfo() {
    return "Builds an aggregated classifier in Spark. "
      + "If the base classifier is not aggregatable then "
      + "an ensemble is created by combining all the "
      + "the map-generated classifiers in a Vote meta classifier.";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif",
      BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif");
  }

  @Override
  protected void notifyJobOutputListeners() {
    weka.classifiers.Classifier finalClassifier =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getClassifier();
    Instances modelHeader =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getTrainingHeader();
    String classAtt =
      ((weka.distributed.spark.WekaClassifierSparkJob) m_runningJob)
        .getClassAttribute();
    try {
      weka.distributed.spark.WekaClassifierSparkJob.setClassIndex(classAtt,
        modelHeader, true);
    } catch (Exception ex) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix() + ex.getMessage());
      }
      ex.printStackTrace();
    }

    if (finalClassifier == null) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix() + "No classifier produced!");
      }
    }

    if (modelHeader == null) {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix()
          + "No training header available for the model!");
      }
    }

    if (finalClassifier != null) {
      if (m_textListeners.size() > 0) {
        String textual = finalClassifier.toString();

        String title = "Spark: ";
        String classifierSpec = finalClassifier.getClass().getName();
        if (finalClassifier instanceof OptionHandler) {
          classifierSpec +=
            " "
              + Utils.joinOptions(((OptionHandler) finalClassifier)
                .getOptions());
        }
        title += classifierSpec;
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
        BatchClassifierEvent be =
          new BatchClassifierEvent(this, finalClassifier, dse, dse, 1, 1);
        for (BatchClassifierListener b : m_classifierListeners) {
          b.acceptClassifier(be);
        }
      }
    }
  }

  /**
   * Add a batch classifier listener
   *
   * @param l a batch classifier listener
   */
  public synchronized void
    addBatchClassifierListener(BatchClassifierListener l) {
    m_classifierListeners.add(l);
  }

  /**
   * Remove a batch classifier listener
   *
   * @param l a batch classifier listener
   */
  public synchronized void removeBatchClassifierListener(
    BatchClassifierListener l) {
    m_classifierListeners.remove(l);
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
