package weka.classifiers.functions.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import weka.core.Instance;
import weka.core.Instances;

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
    INDArray data = Nd4j.ones(insts.numInstances(), insts.numAttributes() - 1);
    double[][] outcomes =
      new double[insts.numInstances()][insts.classAttribute().numValues()];

    for (int i = 0; i < insts.numInstances(); i++) {
      double[] independent = new double[insts.numAttributes() - 1];
      int index = 0;
      Instance current = insts.instance(i);
      for (int j = 0; j < insts.numAttributes(); j++) {
        if (j != insts.classIndex()) {
          independent[index++] = current.value(j);
        } else {
          outcomes[i][(int) current.classValue()] = 1;
        }
      }
      data.putRow(i, Nd4j.create(independent));
    }

    DataSet dataSet = new DataSet(data, Nd4j.create(outcomes));
    return dataSet;
  }

  /**
   * Converts an instance to an INDArray. Only the values of the non-class
   * attributes are copied over.
   *
   * @param inst
   * @return
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
