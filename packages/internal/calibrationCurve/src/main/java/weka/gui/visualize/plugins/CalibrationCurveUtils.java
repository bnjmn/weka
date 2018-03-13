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
 *    CalibrationCurveUtils.java
 *    Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.visualize.plugins;

import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility routine for generating a calibration curve for a particular class
 * value
 *
 * @author eibe
 * @version $Revision: $
 */
public class CalibrationCurveUtils {

  public static Instances getCalibrationCurveAsInstances(
    ArrayList<Prediction> preds, Attribute classAtt, int classValue)
    throws Exception {

    return (Instances) getCalibrationCurveData(preds, classAtt, classValue)
      .get(0);
  }

  public static List<Object> getCalibrationCurveData(
    ArrayList<Prediction> preds, Attribute classAtt, int classValue)
    throws Exception {
    List<Object> result = new ArrayList<>();

    // Remove prediction objects where either the prediction or the actual value
    // are missing
    ArrayList<Prediction> newPreds = new ArrayList<>();
    for (Prediction p : preds) {
      if (!Utils.isMissingValue(p.actual())
        && !Utils.isMissingValue(p.predicted())) {
        newPreds.add(p);
      }
    }
    preds = newPreds;

    ArrayList<Attribute> attributes = new ArrayList<>(1);
    attributes.add(new Attribute("class_prob"));
    Instances data =
      new Instances("class_probabilities", attributes, preds.size());

    for (int i = 0; i < preds.size(); i++) {
      double[] inst =
        { ((NominalPrediction) preds.get(i)).distribution()[classValue] };
      data.add(new DenseInstance(preds.get(i).weight(), inst));
    }

    Discretize d = new Discretize();
    d.setUseEqualFrequency(true);
    d.setBins(Integer.max(1, (int) Math.round(Math.sqrt(data.sumOfWeights()))));
    d.setUseBinNumbers(true);
    d.setInputFormat(data);
    data = Filter.useFilter(data, d);

    int numBins = data.attribute(0).numValues();
    double[] sumClassProb = new double[numBins];
    double[] sumTrueClass = new double[numBins];
    double[] sizeOfBin = new double[numBins];
    for (int i = 0; i < data.numInstances(); i++) {
      int binIndex = (int) data.instance(i).value(0);
      sizeOfBin[binIndex] += preds.get(i).weight();
      sumTrueClass[binIndex] +=
        preds.get(i).weight()
          * ((((int) preds.get(i).actual()) == classValue) ? 1.0 : 0.0);
      sumClassProb[binIndex] +=
        preds.get(i).weight()
          * ((NominalPrediction) preds.get(i)).distribution()[classValue];
    }

    ArrayList<Attribute> atts = new ArrayList<>(1);
    atts.add(new Attribute("average_class_prob"));
    atts.add(new Attribute("average_true_class_value"));

    Instances cdata =
      new Instances("Calibration curve (x: estimated probability, y: observed probability) for "
        + classAtt.value(classValue) + " based on" + " " + numBins
        + " equal-frequency bins", atts, numBins + 2);

    for (int i = 0; i < numBins; i++) {
      double[] v = new double[2];
      v[0] = sumClassProb[i] / sizeOfBin[i];
      v[1] = sumTrueClass[i] / sizeOfBin[i];
      cdata.add(new DenseInstance(sizeOfBin[i], v));
    }
    double[] zero = new double[2];
    double[] one = new double[2];
    one[0] = 1.0;
    one[1] = 1.0;
    cdata.add(new DenseInstance(0.0, zero));
    cdata.add(new DenseInstance(0.0, one));

    result.add(cdata);
    result.add(numBins);
    result.add(sumClassProb);
    result.add(sumTrueClass);
    result.add(sizeOfBin);

    return result;
  }
}
