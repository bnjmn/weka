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
 * PredictionDecimals.java
 * Copyright (C) 2018 University of Waikato, Hamilton, NZ
 */

package wekaexamples.classifiers;

import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.CSV;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.Random;

/**
 * Collects the predictions from a cross-validation run and stores them
 * in CSV format with 12 decimals.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class PredictionDecimals {

  /**
   * Expects the first argument to be the dataset filename, with the last
   * attribute being the class.
   *
   * @param args	the commandline attributes
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);

    // configure classifier
    J48 cls = new J48();

    // cross-validate (10-fold) classifier, store predictions as CSV in stringbuffer
    Evaluation eval = new Evaluation(data);
    StringBuffer buffer = new StringBuffer();
    CSV csv = new CSV();
    csv.setBuffer(buffer);
    csv.setNumDecimals(12);
    // If you want to store the predictions in a file
    //csv.setOutputFile(new java.io.File("/some/where.csv"));
    eval.crossValidateModel(cls, data, 10, new Random(1), csv);

    // output collected predictions
    System.out.println(buffer.toString());
  }
}
