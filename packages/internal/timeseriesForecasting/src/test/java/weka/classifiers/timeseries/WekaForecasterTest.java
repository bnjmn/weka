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
 *    WekaForecasterTest.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries;

import weka.core.Instances;
import weka.classifiers.evaluation.NumericPrediction;
import weka.filters.supervised.attribute.TSLagMaker;

import java.io.*;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs a regression test on the output of the WekaForecaster for
 * a sample time series data set
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class WekaForecasterTest extends TestCase {
  
  public WekaForecasterTest(String name) {
    super(name);
  }

  private String predsToString(List<List<NumericPrediction>> preds, int steps) {
    StringBuffer b = new StringBuffer();

    for (int i = 0; i < steps; i++) {
      List<NumericPrediction> predsForTargetsAtStep = 
        preds.get(i);
      
      for (int j = 0; j < predsForTargetsAtStep.size(); j++) {
        NumericPrediction p = predsForTargetsAtStep.get(j);
        double[][] limits = p.predictionIntervals();
        b.append(p.predicted() + " ");
        if (limits != null && limits.length > 0) {
          b.append(limits[0][0] + " " + limits[0][1] + " ");
        }
      }
      b.append("\n");
    }

    return b.toString();
  }

  public Instances getData(String name) {
    Instances data = null;
    try {
      data =
        new Instances(new BufferedReader(new InputStreamReader(
          ClassLoader.getSystemResourceAsStream("weka/classifiers/timeseries/data/" + name))));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return data;
  }

  public void testRegressionForecastTwoTargetsConfidenceIntervals() throws Exception {

    boolean success = false;
    Instances wine = getData("wine_date.arff");
    weka.test.Regression reg = new weka.test.Regression(this.getClass());

    WekaForecaster forecaster = new WekaForecaster();
    TSLagMaker lagMaker = forecaster.getTSLagMaker();

    try {
      forecaster.setFieldsToForecast("Fortified,Dry-white");
      forecaster.setCalculateConfIntervalsForForecasts(12);
      lagMaker.setTimeStampField("Date");
      lagMaker.setMinLag(1);
      lagMaker.setMaxLag(12);
      lagMaker.setAddMonthOfYear(true);
      lagMaker.setAddQuarterOfYear(true);
      forecaster.buildForecaster(wine, System.out);
      forecaster.primeForecaster(wine);

      int numStepsToForecast = 12;
      List<List<NumericPrediction>> forecast = 
        forecaster.forecast(numStepsToForecast, System.out);
    
      String forecastString = predsToString(forecast, numStepsToForecast);
      success = true;
      reg.println(forecastString);
    } catch (Exception ex) {
      ex.printStackTrace();
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("not in classpath") > -1) {
        return;
      }
    }
    
    if (!success) {
      fail("Problem during regression testing: no successful predictions generated");
    }

    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating.");
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  public static Test suite() {
    return new TestSuite(weka.classifiers.timeseries.WekaForecasterTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
