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
 *    WekaClassifierHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
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
import weka.distributed.hadoop.WekaClassifierHadoopMapper;

/**
 * Knowledge Flow step for executing the WekaClassifierHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Hadoop", toolTipText = "Builds an aggregated Weka classifier")
public class WekaClassifierHadoopJob extends AbstractHadoopJob {

  /** For serialization */
  private static final long serialVersionUID = -2028182072204579710L;

  /** Downstream listeners for classifier model output */
  protected List<BatchClassifierListener> m_classifierListeners = new ArrayList<BatchClassifierListener>();

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  /**
   * Constructor
   */
  public WekaClassifierHadoopJob() {
    super();

    m_job = new weka.distributed.hadoop.WekaClassifierHadoopJob();
    m_visual.setText("WekaClassifierHadoopJob");
  }

  /**
   * Help information
   * 
   * @return help information
   */
  public String globalInfo() {
    return "Builds an aggregated classifier in Hadoop. "
      + "If the base classifier is not aggregateable then "
      + "an ensemble is created by combining all the "
      + "the map-generated classifiers in a Vote meta classifier.";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "WekaClassifierHadoopJob.gif",
      BeanVisual.ICON_PATH + "WekaClassifierHadoopJob.gif");
  }

  @Override
  protected void notifyJobOutputListeners() {
    weka.classifiers.Classifier finalClassifier = ((weka.distributed.hadoop.WekaClassifierHadoopJob) m_runningJob)
      .getClassifier();
    Instances modelHeader = ((weka.distributed.hadoop.WekaClassifierHadoopJob) m_runningJob)
      .getTrainingHeader();
    String classAtt = ((weka.distributed.hadoop.WekaClassifierHadoopJob) m_runningJob)
      .getClassAttribute();
    try {
      WekaClassifierHadoopMapper.setClassIndex(classAtt, modelHeader, true);
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

        String title = "Hadoop: ";
        String classifierSpec = finalClassifier.getClass().getName();
        if (finalClassifier instanceof OptionHandler) {
          classifierSpec += " "
            + Utils.joinOptions(((OptionHandler) finalClassifier).getOptions());
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
        BatchClassifierEvent be = new BatchClassifierEvent(this,
          finalClassifier, dse, dse, 1, 1);
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
  public synchronized void addBatchClassifierListener(BatchClassifierListener l) {
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
