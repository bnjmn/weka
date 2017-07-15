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
 *    Utils.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Utility routines for the Dl4jMlpClassifier
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class Utils {

  /**
   * Converts a set of training instances to a DataSet. Assumes that the
   * instances have been suitably preprocessed - i.e. missing values replaced
   * and nominals converted to binary/numeric. Also assumes that the class index
   * has been set
   *
   * @param insts the instances to convert
   * @return a DataSet
   */
  public static DataSet instancesToDataSet(Instances insts) {
    INDArray data = Nd4j.zeros(insts.numInstances(), insts.numAttributes() - 1);
    INDArray outcomes = Nd4j.zeros(insts.numInstances(), insts.numClasses());

    for (int i = 0; i < insts.numInstances(); i++) {
      double[] independent = new double[insts.numAttributes() - 1];
      double[] dependent = new double[insts.numClasses()];
      Instance current = insts.instance(i);
      for (int j = 0; j < current.numValues(); j++) {
        int index = current.index(j);
        if (index < insts.classIndex()) {
          independent[index] = current.valueSparse(j);
        } else if (index > insts.classIndex()) {
          independent[index - 1] = current.valueSparse(j);
        } else {
          if (insts.numClasses() > 1) {
            dependent[(int)current.valueSparse(j)] = 1.0;
          } else {
            dependent[0] = current.valueSparse(j);
          }
        }
      }
      data.putRow(i, Nd4j.create(independent));
      outcomes.putRow(i, Nd4j.create(dependent));
    }

    DataSet dataSet = new DataSet(data, outcomes);
    return dataSet;
  }

  /**
   * Converts a set of training instances corresponding to a time series to a
   * DataSet. For RNNs, the input of the network is 3 dimensional of dimensions
   * [numExamples, numInputs, numTimeSteps] Assumes that the instances have been
   * suitably preprocessed - i.e. missing values replaced and nominals converted
   * to binary/numeric. Also assumes that the class index has been set
   *
   * @param insts the instances to convert
   * @return a DataSet
   */
  public static DataSet RNNinstancesToDataSet(Instances insts) {
    INDArray data =
      Nd4j.ones(1, insts.numAttributes() - 1, insts.numInstances());
    INDArray labels = Nd4j.ones(1, insts.numClasses(), insts.numInstances());
    double[] outcomes = new double[insts.numInstances()];

    for (int i = 0; i < insts.numInstances(); i++) {
      double[] independent = new double[insts.numAttributes() - 1];
      int index = 0;
      Instance current = insts.instance(i);
      for (int j = 0; j < insts.numAttributes(); j++) {
        if (j != insts.classIndex()) {
          independent[index++] = current.value(j);
        } else {
          outcomes[i] = current.classValue();
        }
      }

      INDArray ind =
        Nd4j.create(independent, new int[] { 1, insts.numAttributes() - 1, 1 });
      for (int k = 0; k < independent.length; k++) {
        data.putScalar(0, k, i, ind.getDouble(k));
      }
    }
    INDArray outcomesNDArray =
      Nd4j.create(outcomes, new int[] { 1, insts.numInstances(), 1 });
    labels.putColumn(0, outcomesNDArray);

    DataSet dataSet = new DataSet(data, labels);
    return dataSet;
  }

  /**
   * Converts an instance to an INDArray. Only the values of the non-class
   * attributes are copied over.
   *
   * @param inst
   * @return the INDArray
   */
  public static INDArray instanceToINDArray(Instance inst) {
    INDArray result = Nd4j.ones(1, inst.numAttributes() - 1);
    double[] independent = new double[inst.numAttributes() - 1];
    int index = 0;
    for (int i = 0; i < inst.numAttributes(); i++) {
      if (i != inst.classIndex()) {
        independent[index++] = inst.value(i);
      }
    }
    result.putRow(0, Nd4j.create(independent));

    return result;
  }
}
