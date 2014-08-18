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
 *    CorrelationMatrixMapTaskTest.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import weka.core.Instances;
import weka.core.Utils;

/**
 * Test class for CorrelationMatrixMapTask
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixMapTaskTest {
  public static final String IRIS_HEADER = "@relation iris\n"
    + "@attribute sepallength numeric\n" + "@attribute sepalwidth numeric\n"
    + "@attribute petallength numeric\n" + "@attribute petalwidth numeric\n"
    + "@attribute class {Iris-setosa, Iris-versicolor, Iris-virginica}\n"
    + "@data\n";

  public static final String IRIS_DATA = "5.1,3.5,1.4,0.2,Iris-setosa\n"
    + "4.9,3.0,1.4,0.2,Iris-setosa\n" + "4.7,3.2,1.3,0.2,Iris-setosa\n"
    + "4.6,3.1,1.5,0.2,Iris-setosa\n" + "5.0,3.6,1.4,0.2,Iris-setosa\n"
    + "5.4,3.9,1.7,0.4,Iris-setosa\n" + "4.6,3.4,1.4,0.3,Iris-setosa\n"
    + "5.0,3.4,1.5,0.2,Iris-setosa\n" + "4.4,2.9,1.4,0.2,Iris-setosa\n"
    + "4.9,3.1,1.5,0.1,Iris-setosa\n" + "5.4,3.7,1.5,0.2,Iris-setosa\n"
    + "4.8,3.4,1.6,0.2,Iris-setosa\n" + "4.8,3.0,1.4,0.1,Iris-setosa\n"
    + "4.3,3.0,1.1,0.1,Iris-setosa\n" + "5.8,4.0,1.2,0.2,Iris-setosa\n"
    + "5.7,4.4,1.5,0.4,Iris-setosa\n" + "5.4,3.9,1.3,0.4,Iris-setosa\n"
    + "5.1,3.5,1.4,0.3,Iris-setosa\n" + "5.7,3.8,1.7,0.3,Iris-setosa\n"
    + "5.1,3.8,1.5,0.3,Iris-setosa\n" + "5.4,3.4,1.7,0.2,Iris-setosa\n"
    + "5.1,3.7,1.5,0.4,Iris-setosa\n" + "4.6,3.6,1.0,0.2,Iris-setosa\n"
    + "5.1,3.3,1.7,0.5,Iris-setosa\n" + "4.8,3.4,1.9,0.2,Iris-setosa\n"
    + "5.0,3.0,1.6,0.2,Iris-setosa\n" + "5.0,3.4,1.6,0.4,Iris-setosa\n"
    + "5.2,3.5,1.5,0.2,Iris-setosa\n" + "5.2,3.4,1.4,0.2,Iris-setosa\n"
    + "4.7,3.2,1.6,0.2,Iris-setosa\n" + "4.8,3.1,1.6,0.2,Iris-setosa\n"
    + "5.4,3.4,1.5,0.4,Iris-setosa\n" + "5.2,4.1,1.5,0.1,Iris-setosa\n"
    + "5.5,4.2,1.4,0.2,Iris-setosa\n" + "4.9,3.1,1.5,0.1,Iris-setosa\n"
    + "5.0,3.2,1.2,0.2,Iris-setosa\n" + "5.5,3.5,1.3,0.2,Iris-setosa\n"
    + "4.9,3.1,1.5,0.1,Iris-setosa\n" + "4.4,3.0,1.3,0.2,Iris-setosa\n"
    + "5.1,3.4,1.5,0.2,Iris-setosa\n" + "5.0,3.5,1.3,0.3,Iris-setosa\n"
    + "4.5,2.3,1.3,0.3,Iris-setosa\n" + "4.4,3.2,1.3,0.2,Iris-setosa\n"
    + "5.0,3.5,1.6,0.6,Iris-setosa\n" + "5.1,3.8,1.9,0.4,Iris-setosa\n"
    + "4.8,3.0,1.4,0.3,Iris-setosa\n" + "5.1,3.8,1.6,0.2,Iris-setosa\n"
    + "4.6,3.2,1.4,0.2,Iris-setosa\n" + "5.3,3.7,1.5,0.2,Iris-setosa\n"
    + "5.0,3.3,1.4,0.2,Iris-setosa\n"

    + "7.0,3.2,4.7,1.4,Iris-versicolor\n" + "6.4,3.2,4.5,1.5,Iris-versicolor\n"
    + "6.9,3.1,4.9,1.5,Iris-versicolor\n" + "5.5,2.3,4.0,1.3,Iris-versicolor\n"
    + "6.5,2.8,4.6,1.5,Iris-versicolor\n" + "5.7,2.8,4.5,1.3,Iris-versicolor\n"
    + "6.3,3.3,4.7,1.6,Iris-versicolor\n" + "4.9,2.4,3.3,1.0,Iris-versicolor\n"
    + "6.6,2.9,4.6,1.3,Iris-versicolor\n" + "5.2,2.7,3.9,1.4,Iris-versicolor\n"
    + "5.0,2.0,3.5,1.0,Iris-versicolor\n" + "5.9,3.0,4.2,1.5,Iris-versicolor\n"
    + "6.0,2.2,4.0,1.0,Iris-versicolor\n" + "6.1,2.9,4.7,1.4,Iris-versicolor\n"
    + "5.6,2.9,3.6,1.3,Iris-versicolor\n" + "6.7,3.1,4.4,1.4,Iris-versicolor\n"
    + "5.6,3.0,4.5,1.5,Iris-versicolor\n" + "5.8,2.7,4.1,1.0,Iris-versicolor\n"
    + "6.2,2.2,4.5,1.5,Iris-versicolor\n" + "5.6,2.5,3.9,1.1,Iris-versicolor\n"
    + "5.9,3.2,4.8,1.8,Iris-versicolor\n" + "6.1,2.8,4.0,1.3,Iris-versicolor\n"
    + "6.3,2.5,4.9,1.5,Iris-versicolor\n" + "6.1,2.8,4.7,1.2,Iris-versicolor\n"
    + "6.4,2.9,4.3,1.3,Iris-versicolor\n" + "6.6,3.0,4.4,1.4,Iris-versicolor\n"
    + "6.8,2.8,4.8,1.4,Iris-versicolor\n" + "6.7,3.0,5.0,1.7,Iris-versicolor\n"
    + "6.0,2.9,4.5,1.5,Iris-versicolor\n" + "5.7,2.6,3.5,1.0,Iris-versicolor\n"
    + "5.5,2.4,3.8,1.1,Iris-versicolor\n" + "5.5,2.4,3.7,1.0,Iris-versicolor\n"
    + "5.8,2.7,3.9,1.2,Iris-versicolor\n" + "6.0,2.7,5.1,1.6,Iris-versicolor\n"
    + "5.4,3.0,4.5,1.5,Iris-versicolor\n" + "6.0,3.4,4.5,1.6,Iris-versicolor\n"
    + "6.7,3.1,4.7,1.5,Iris-versicolor\n" + "6.3,2.3,4.4,1.3,Iris-versicolor\n"
    + "5.6,3.0,4.1,1.3,Iris-versicolor\n" + "5.5,2.5,4.0,1.3,Iris-versicolor\n"
    + "5.5,2.6,4.4,1.2,Iris-versicolor\n" + "6.1,3.0,4.6,1.4,Iris-versicolor\n"
    + "5.8,2.6,4.0,1.2,Iris-versicolor\n" + "5.0,2.3,3.3,1.0,Iris-versicolor\n"
    + "5.6,2.7,4.2,1.3,Iris-versicolor\n" + "5.7,3.0,4.2,1.2,Iris-versicolor\n"
    + "5.7,2.9,4.2,1.3,Iris-versicolor\n" + "6.2,2.9,4.3,1.3,Iris-versicolor\n"
    + "5.1,2.5,3.0,1.1,Iris-versicolor\n" + "5.7,2.8,4.1,1.3,Iris-versicolor\n"

    + "6.3,3.3,6.0,2.5,Iris-virginica\n" + "5.8,2.7,5.1,1.9,Iris-virginica\n"
    + "7.1,3.0,5.9,2.1,Iris-virginica\n" + "6.3,2.9,5.6,1.8,Iris-virginica\n"
    + "6.5,3.0,5.8,2.2,Iris-virginica\n" + "7.6,3.0,6.6,2.1,Iris-virginica\n"
    + "4.9,2.5,4.5,1.7,Iris-virginica\n" + "7.3,2.9,6.3,1.8,Iris-virginica\n"
    + "6.7,2.5,5.8,1.8,Iris-virginica\n" + "7.2,3.6,6.1,2.5,Iris-virginica\n"
    + "6.5,3.2,5.1,2.0,Iris-virginica\n" + "6.4,2.7,5.3,1.9,Iris-virginica\n"
    + "6.8,3.0,5.5,2.1,Iris-virginica\n" + "5.7,2.5,5.0,2.0,Iris-virginica\n"
    + "5.8,2.8,5.1,2.4,Iris-virginica\n" + "6.4,3.2,5.3,2.3,Iris-virginica\n"
    + "6.5,3.0,5.5,1.8,Iris-virginica\n" + "7.7,3.8,6.7,2.2,Iris-virginica\n"
    + "7.7,2.6,6.9,2.3,Iris-virginica\n" + "6.0,2.2,5.0,1.5,Iris-virginica\n"
    + "6.9,3.2,5.7,2.3,Iris-virginica\n" + "5.6,2.8,4.9,2.0,Iris-virginica\n"
    + "7.7,2.8,6.7,2.0,Iris-virginica\n" + "6.3,2.7,4.9,1.8,Iris-virginica\n"
    + "6.7,3.3,5.7,2.1,Iris-virginica\n" + "7.2,3.2,6.0,1.8,Iris-virginica\n"
    + "6.2,2.8,4.8,1.8,Iris-virginica\n" + "6.1,3.0,4.9,1.8,Iris-virginica\n"
    + "6.4,2.8,5.6,2.1,Iris-virginica\n" + "7.2,3.0,5.8,1.6,Iris-virginica\n"
    + "7.4,2.8,6.1,1.9,Iris-virginica\n" + "7.9,3.8,6.4,2.0,Iris-virginica\n"
    + "6.4,2.8,5.6,2.2,Iris-virginica\n" + "6.3,2.8,5.1,1.5,Iris-virginica\n"
    + "6.1,2.6,5.6,1.4,Iris-virginica\n" + "7.7,3.0,6.1,2.3,Iris-virginica\n"
    + "6.3,3.4,5.6,2.4,Iris-virginica\n" + "6.4,3.1,5.5,1.8,Iris-virginica\n"
    + "6.0,3.0,4.8,1.8,Iris-virginica\n" + "6.9,3.1,5.4,2.1,Iris-virginica\n"
    + "6.7,3.1,5.6,2.4,Iris-virginica\n" + "6.9,3.1,5.1,2.3,Iris-virginica\n"
    + "5.8,2.7,5.1,1.9,Iris-virginica\n" + "6.8,3.2,5.9,2.3,Iris-virginica\n"
    + "6.7,3.3,5.7,2.5,Iris-virginica\n" + "6.7,3.0,5.2,2.3,Iris-virginica\n"
    + "6.3,2.5,5.0,1.9,Iris-virginica\n" + "6.5,3.0,5.2,2.0,Iris-virginica\n"
    + "6.2,3.4,5.4,2.3,Iris-virginica\n" + "5.9,3.0,5.1,1.8,Iris-virginica\n";

  public static final String IRIS = IRIS_HEADER + IRIS_DATA;

  protected static final String BOLTS = "@relation bolts\n"
    + "@attribute RUN integer\n" + "@attribute SPEED1 integer\n"
    + "@attribute TOTAL integer\n" + "@attribute SPEED2 integer\n"
    + "@attribute NUMBER2 integer\n" + "@attribute SENS integer\n"
    + "@attribute TIME real\n" + "@attribute T20BOLT real\n" + "@data\n"
    + "25, 2, 10, 1.5, 0,  6,   5.70, 11.40\n"
    + "24, 2, 10, 1.5, 0, 10,  17.56, 35.12\n"
    + "30, 2, 10, 1.5, 2,  6,  11.28, 22.56\n"
    + " 2, 2, 10, 1.5, 2, 10,   8.39, 16.78\n"
    + "40, 2, 10, 2.5, 0,  6,  16.67, 33.34\n"
    + "37, 2, 10, 2.5, 0, 10,  12.04, 24.08\n"
    + "16, 2, 10, 2.5, 2,  6,   9.22, 18.44\n"
    + "22, 2, 10, 2.5, 2, 10,   3.94,  7.88\n"
    + "33, 2, 30, 1.5, 0,  6,  27.02, 18.01\n"
    + "17, 2, 30, 1.5, 0, 10,  19.46, 12.97\n"
    + "28, 2, 30, 1.5, 2,  6,  18.54, 12.36\n"
    + "27, 2, 30, 1.5, 2, 10,  25.70, 17.13\n"
    + "14, 2, 30, 2.5, 0,  6,  19.02, 12.68\n"
    + "13, 2, 30, 2.5, 0, 10,  22.39, 14.93\n"
    + " 4, 2, 30, 2.5, 2,  6,  23.85, 15.90\n"
    + "21, 2, 30, 2.5, 2, 10,  30.12, 20.08\n"
    + "23, 6, 10, 1.5, 0,  6,  13.42, 26.84\n"
    + "35, 6, 10, 1.5, 0, 10,  34.26, 68.52\n"
    + "19, 6, 10, 1.5, 2,  6,  39.74, 79.48\n"
    + "34, 6, 10, 1.5, 2, 10,  10.60, 21.20\n"
    + "31, 6, 10, 2.5, 0,  6,  28.89, 57.78\n"
    + " 9, 6, 10, 2.5, 0, 10,  35.61, 71.22\n"
    + "38, 6, 10, 2.5, 2,  6,  17.20, 34.40\n"
    + "15, 6, 10, 2.5, 2, 10,   6.00, 12.00\n"
    + "39, 6, 30, 1.5, 0,  6, 129.45, 86.30\n"
    + " 8, 6, 30, 1.5, 0,  0, 107.38, 71.59\n"
    + "26, 6, 30, 1.5, 2,  6, 111.66, 74.44\n"
    + "11, 6, 30, 1.5, 2,  0, 109.10, 72.73\n"
    + " 6, 6, 30, 2.5, 0,  6, 100.43, 66.95\n"
    + "20, 6, 30, 2.5, 0,  0, 109.28, 72.85\n"
    + "10, 6, 30, 2.5, 2,  6, 106.46, 70.97\n"
    + "32, 6, 30, 2.5, 2,  0, 134.01, 89.34\n"
    + " 1, 4, 20, 2.0, 1,  8,  10.78, 10.78\n"
    + " 3, 4, 20, 2.0, 1,  8,   9.39,  9.39\n"
    + " 5, 4, 20, 2.0, 1,  8,   9.84,  9.84\n"
    + " 7, 4, 20, 2.0, 1,  8,  13.94, 13.94\n"
    + "12, 4, 20, 2.0, 1,  8,  12.33, 12.33\n"
    + "18, 4, 20, 2.0, 1,  8,   7.32,  7.32\n"
    + "29, 4, 20, 2.0, 1,  8,   7.91,  7.91\n"
    + "36, 4, 20, 2.0, 1,  8,  15.58, 15.58\n";

  public static Instances getIris() throws IOException {
    Instances iris = new Instances(new BufferedReader(new StringReader(IRIS)));

    return iris;
  }

  public static Instances getBolts() throws IOException {
    Instances bolts =
      new Instances(new BufferedReader(new StringReader(BOLTS)));

    return bolts;
  }

  protected Instances getSummaryInsts(Instances orig, String[] args)
    throws Exception {
    List<String> attNames = new ArrayList<String>();
    for (int i = 0; i < orig.numAttributes(); i++) {
      attNames.add(orig.attribute(i).name());
    }

    CSVToARFFHeaderMapTask arffTask = new CSVToARFFHeaderMapTask();
    arffTask.setOptions(args);
    for (int i = 0; i < orig.numInstances(); i++) {
      arffTask.processRow(orig.instance(i).toString(), attNames);
    }
    Instances withSummary = arffTask.getHeader();
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(withSummary);
    withSummary = arffReduce.aggregate(instList);

    withSummary.setClassIndex(orig.classIndex());

    return withSummary;
  }

  protected static final double TOL = 1e-6;

  protected void checkAgainstUtilsCorr(double[][] matrix, Instances orig,
    Instances withSummary, boolean deleteClassIfSet) throws Exception {
    CorrelationMatrixRowReduceTask reduce =
      new CorrelationMatrixRowReduceTask();
    double[][] finalM = new double[matrix.length][];
    for (int i = 0; i < matrix.length; i++) {
      List<double[]> toAgg = new ArrayList<double[]>();
      toAgg.add(matrix[i]);
      double[] computed = reduce.aggregate(i, toAgg, null, withSummary, true,
        false, deleteClassIfSet);
      // for (int j = 0; j < matrix[i].length; j++) {
      // System.err.print(computed[j] + " ");
      // }
      finalM[i] = computed;
      // System.err.println();
    }

    double[][] utilsC = new double[matrix.length][];
    for (int i = 0; i < orig.numAttributes(); i++) {
      double[] row = new double[i + 1];
      for (int j = 0; j <= i; j++) {
        if (i != orig.classIndex() && j != orig.classIndex()) {
          if (i == j) {
            row[j] = 1.0;
          } else {
            double[] ii = new double[orig.numInstances()];
            double[] jj = new double[orig.numInstances()];
            for (int k = 0; k < orig.numInstances(); k++) {
              ii[k] = orig.instance(k).value(i);
              jj[k] = orig.instance(k).value(j);
            }

            row[j] = Utils.correlation(ii, jj, orig.numInstances());
          }
          // System.err.print(row[j] + " ");
        }
      }
      if (i != orig.classIndex()) {
        utilsC[i] = row;
      }
      // System.err.println();
    }

    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < finalM[i].length; j++) {
        double diff = Math.abs(finalM[i][j] - utilsC[i][j]);

        assertTrue(diff < TOL);
      }
    }
  }

  @Test
  public void testNumericTarget() throws Exception {
    Instances orig = getBolts();

    orig.setClassIndex(orig.numAttributes() - 1);
    Instances withSummary = getSummaryInsts(orig, new String[0]);

    CorrelationMatrixMapTask corrTask = new CorrelationMatrixMapTask();
    corrTask.setup(withSummary);

    for (int i = 0; i < orig.numInstances(); i++) {
      corrTask.processInstance(orig.instance(i));
    }

    double[][] matrix = corrTask.getMatrix();
    assertTrue(matrix != null);
    assertEquals(7, matrix.length); // numeric class is not part of the matrix
    checkAgainstUtilsCorr(matrix, orig, withSummary, true);
  }

  @Test
  public void testNumericTargetWithTargetIncluded() throws Exception {
    Instances orig = getBolts();

    orig.setClassIndex(orig.numAttributes() - 1);
    Instances withSummary = getSummaryInsts(orig, new String[0]);

    CorrelationMatrixMapTask corrTask = new CorrelationMatrixMapTask();
    corrTask.setKeepClassAttributeIfSet(true);
    corrTask.setup(withSummary);

    for (int i = 0; i < orig.numInstances(); i++) {
      corrTask.processInstance(orig.instance(i));
    }

    double[][] matrix = corrTask.getMatrix();

    orig.setClassIndex(-1);
    assertTrue(matrix != null);
    assertEquals(8, matrix.length); // numeric class is part of the matrix
    checkAgainstUtilsCorr(matrix, orig, withSummary, false);
  }

  @Test
  public void testNominalTarget() throws Exception {
    Instances orig = getIris();

    orig.setClassIndex(orig.numAttributes() - 1);
    Instances withSummary = getSummaryInsts(orig, new String[0]);

    CorrelationMatrixMapTask corrTask = new CorrelationMatrixMapTask();
    corrTask.setup(withSummary);

    for (int i = 0; i < orig.numInstances(); i++) {
      corrTask.processInstance(orig.instance(i));
    }

    double[][] matrix = corrTask.getMatrix();

    assertTrue(matrix != null);

    assertEquals(4, matrix.length);

    checkAgainstUtilsCorr(matrix, orig, withSummary, true);
  }

  public static void main(String[] args) {
    try {
      CorrelationMatrixMapTaskTest t = new CorrelationMatrixMapTaskTest();
      t.testNominalTarget();
      t.testNumericTarget();
      t.testNumericTargetWithTargetIncluded();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
