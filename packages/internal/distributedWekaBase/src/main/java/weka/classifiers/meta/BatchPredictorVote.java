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
 *    BatchPredictorVote.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.core.BatchPredictor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.GPCIgnore;
import weka.gui.beans.KFIgnore;

/**
 * Class that extends Vote in order to implement BatchPredictor. This is of
 * particular use for forming ensembles of R classifiers learned via the
 * MLRClassifier in the RPlugin.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFIgnore
@GPCIgnore
public class BatchPredictorVote extends Vote implements BatchPredictor {

  /** For serialization */
  private static final long serialVersionUID = 3807444052796114383L;

  /** Batch prediction size */
  protected String m_batchPredictionSize = "1000";

  @Override
  public void setBatchSize(String size) {
    m_batchPredictionSize = size;
  }

  @Override
  public String getBatchSize() {
    return m_batchPredictionSize;
  }

  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {
    // For speed we won't do any checks that the base classifiers are all
    // BatchPredictors

    // TODO if this gets used anywhere but in distributed Weka then the other
    // combination strategies will need to be implemented!!

    return distributionsForInstancesAverage(insts);
  }

  /**
   * Computes the average of probabilities combination rule
   * 
   * @param insts the instances to predict
   * @return an array of averaged predictions
   * @throws Exception if a problem occurs
   */
  protected double[][] distributionsForInstancesAverage(Instances insts)
    throws Exception {
    double[][] preds = new double[insts.numInstances()][insts.numClasses()];
    double[] numPredictions = new double[insts.numInstances()];

    for (int i = 0; i < m_Classifiers.length; i++) {
      double[][] dists =
        ((BatchPredictor) getClassifier(i)).distributionsForInstances(insts);

      for (int j = 0; j < insts.numInstances(); j++) {
        Instance instance = insts.instance(j);
        if (!instance.classAttribute().isNumeric()
          || !Utils.isMissingValue(dists[j][0])) {
          for (int k = 0; k < dists[j].length; k++) {
            preds[j][k] += dists[j][k];
          }
          numPredictions[j]++;
        }
      }
    }

    for (int i = 0; i < m_preBuiltClassifiers.size(); i++) {
      double[][] dists =
        ((BatchPredictor) m_preBuiltClassifiers.get(i))
          .distributionsForInstances(insts);

      for (int j = 0; j < insts.numInstances(); j++) {
        Instance instance = insts.instance(j);
        if (!instance.classAttribute().isNumeric()
          || !Utils.isMissingValue(dists[j][0])) {
          for (int k = 0; k < dists[j].length; k++) {
            preds[j][k] += dists[j][k];
          }
          numPredictions[j]++;
        }
      }
    }

    for (int i = 0; i < insts.numInstances(); i++) {
      if (insts.classAttribute().isNumeric()) {
        if (numPredictions[i] == 0) {
          preds[i][0] = Utils.missingValue();
        } else {
          for (int j = 0; j < preds[i].length; j++) {
            preds[i][j] /= numPredictions[i];
          }
        }
      } else {

        // Should normalize "probability" distribution
        if (Utils.sum(preds[i]) > 0) {
          Utils.normalize(preds[i]);
        }
      }
    }

    return preds;
  }

  @Override
  public boolean implementsMoreEfficientBatchPrediction() {
    for (int i = 0; i < m_Classifiers.length; i++) {
      if (!((BatchPredictor) m_Classifiers[i])
        .implementsMoreEfficientBatchPrediction()) {
        return false;
      }
    }
    return true;
  }
}
