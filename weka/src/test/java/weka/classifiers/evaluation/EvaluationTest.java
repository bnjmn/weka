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
 * Copyright 2013 University of Waikato
 */

package weka.classifiers.evaluation;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests Evaluation. So far just does a simple regression test for
 * classification with standard output and information retrieval stats.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class EvaluationTest extends TestCase {

  public static final String DATA = "@relation test\n@attribute one numeric\n@attribute two numeric\n@attribute class {T, F}\n@data\n\n"
      + "6.5,3.0,T\n"
      + "6.2,3.4,T\n"
      + "5.9,3.0,T\n"
      + "7.0,3.2,F\n"
      + "6.4,3.2,F\n" + "6.9,3.1,F\n";

  public static final double[][] PREDS = { { 0.231, 0.769 }, { 1, 0 },
      { 0.956, 0.044 }, { 0.013, 0.987 }, { 0.484, 0.516 }, { 0.068, 0.932 } };

  public EvaluationTest(String name) {
    super(name);
  }

  public void testRegression() throws Exception {
    Instances inst = new Instances(new StringReader(DATA));
    inst.setClassIndex(inst.numAttributes() - 1);
    Evaluation eval = new Evaluation(inst);

    for (int i = 0; i < inst.numInstances(); i++) {
      eval.evaluateModelOnceAndRecordPrediction(PREDS[i], inst.instance(i));
    }

    String standard = eval.toSummaryString();
    String info = eval.toClassDetailsString();

    weka.test.Regression reg = new weka.test.Regression(getClass());

    reg.println(standard);
    reg.println(info);

    try {
      String diff = reg.diff();

      if (diff == null) {
        System.err.println("Warning: No reference available, creating.");
      } else if (!diff.equals("")) {
        fail("Regression tst failed. Difference:\n" + diff);
      }
    } catch (IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  public static Test suite() {
    return new TestSuite(weka.classifiers.evaluation.EvaluationTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
