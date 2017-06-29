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
 *    MLlibLogistic.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.knowledgeflow.SingleThreadedExecution;

import java.util.List;

/**
 * Weka wrapper classifier for the MLlib logistic regression scheme
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class MLlibLogistic extends MLlibClassifier {

  private static final long serialVersionUID = -4669498649343351124L;

  /** The learned model */
  protected LogisticRegressionModel m_logisticModel;

  /**
   * Constructor
   */
  public MLlibLogistic() {
    m_numDecimalPlaces = 4;
  }

  /**
   * About information
   *
   * @return about info
   */
  public String globalInfo() {
    return "Spark MLlib logistic regression wrapper classifier.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Learn the underlying MLlib model.
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary header (with summary attributes) for the data
   * @param preprocessors streamable filters for preprocessing the data
   * @param strategy the header of the data (with summary attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  @Override
  public void learnModel(JavaRDD<Instance> wekaData,
    Instances headerWithSummary, List<StreamableFilter> preprocessors,
    CachingStrategy strategy) throws DistributedWekaException {
    Filter preconstructed = loadPreconstructedFilterIfNecessary();

    JavaRDD<LabeledPoint> mlLibData =
      m_datasetMaker.labeledPointRDDFirstBatch(headerWithSummary, wekaData,
        !getDontReplaceMissingValues(), (PreconstructedFilter) preconstructed,
        preprocessors, Integer.MAX_VALUE, false, strategy);

    m_classAtt = headerWithSummary.attribute(m_datasetMaker.getClassIndex());

    if (!m_classAtt.isNominal()) {
      throw new DistributedWekaException("Class attribute must be nominal!");
    }

    LogisticRegressionWithLBFGS logistic = new LogisticRegressionWithLBFGS();
    logistic.setNumClasses(m_classAtt.numValues());
    m_logisticModel = logistic.run(mlLibData.rdd());
  }

  /**
   * Predict a test point using the learned model
   *
   * @param test a {@code Vector} containing the test point
   * @return the predicted value (index of class for classification, or actual
   *         value for regression)
   * @throws DistributedWekaException if a problem occurs
   */
  @Override
  public double predict(Vector test) throws DistributedWekaException {
    if (m_logisticModel == null) {
      throw new DistributedWekaException("No model built yet!");
    }

    return m_logisticModel.predict(test);
  }

  /**
   * Textual description of the learned model
   *
   * @return a textual description of the model
   */
  public String toString() {
    if (m_logisticModel == null) {
      return "No model built yet";
    }

    StringBuilder b = new StringBuilder();
    b.append(m_logisticModel.toString()).append("\n\n");
    Instances data = m_datasetMaker.getTransformedHeader();
    b.append(data.classAttribute().name()).append(" =").append("\n\n");
    double[] coeffs = m_logisticModel.weights().toArray();
    double intercept = m_logisticModel.intercept();
    int index = 0;
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {
        b.append(Utils.doubleToString(coeffs[index], 12, getNumDecimalPlaces()))
          .append(" * ");
        b.append(data.attribute(i).name()).append("\n");
        index++;
      }
    }
    b.append(Utils.doubleToString(intercept, 12, getNumDecimalPlaces()))
      .append("\n");

    return b.toString();
  }

  @Override
  public void run(Object toRun, final String[] options) {
    runClassifier((MLlibLogistic) toRun, options);
  }

  public static void main(String[] args) {
    MLlibLogistic l = new MLlibLogistic();
    l.run(l, args);
  }
}
