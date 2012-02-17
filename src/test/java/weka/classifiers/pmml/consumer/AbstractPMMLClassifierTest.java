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

package weka.classifiers.pmml.consumer;

import weka.core.Instances;
import weka.core.FastVector;
import weka.core.Attribute;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.test.Regression;
import weka.classifiers.evaluation.EvaluationUtils;

import java.io.*;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractPMMLClassifierTest extends TestCase {

  protected FastVector m_modelNames = new FastVector();
  protected FastVector m_dataSetNames = new FastVector();

  public AbstractPMMLClassifierTest(String name) { 
    super(name); 
  }

  public Instances getData(String name) {
    Instances elnino = null;
    try {
      elnino = 
        new Instances(new BufferedReader(new InputStreamReader(
          ClassLoader.getSystemResourceAsStream("weka/classifiers/pmml/data/" + name))));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return elnino;
  }

  public PMMLClassifier getClassifier(String name) {
    PMMLClassifier regression = null;
    try {
      PMMLModel model = 
        PMMLFactory.getPMMLModel(new BufferedInputStream(ClassLoader.getSystemResourceAsStream(
                  "weka/classifiers/pmml/data/" + name)));

      regression = (PMMLClassifier)model;

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return regression;
  }

  public void testRegression() throws Exception {

    PMMLClassifier classifier = null;
    Instances testData = null;
    EvaluationUtils evalUtils = null; 
    weka.test.Regression reg = new weka.test.Regression(this.getClass());

    FastVector predictions = null;
    boolean success = false;
    for (int i = 0; i < m_modelNames.size(); i++) {
      classifier = getClassifier((String)m_modelNames.elementAt(i));
      testData = getData((String)m_dataSetNames.elementAt(i));
      evalUtils = new EvaluationUtils();

      try {
        String  className = classifier.getMiningSchema().getFieldsAsInstances().classAttribute().name();
        Attribute classAtt = testData.attribute(className);
        testData.setClass(classAtt);
        predictions = evalUtils.getTestPredictions(classifier, testData);
        success = true;
        String predsString = weka.classifiers.AbstractClassifierTest.predictionsToString(predictions);
        reg.println(predsString);
      } catch (Exception ex) {
        ex.printStackTrace();
        String msg = ex.getMessage().toLowerCase();
        if (msg.indexOf("not in classpath") > -1) {
          return;
        }
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
    }  catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }    
  }
}