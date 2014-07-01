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
 *    WekaScoringTaskTest.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Test class for WekaScoringMapTask
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringTaskTest {

  @Test
  public void testScoreWithClassifier() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    NaiveBayes bayes = new NaiveBayes();

    bayes.buildClassifier(train);

    WekaScoringMapTask task = new WekaScoringMapTask();
    task.setModel(bayes, train, train);

    assertEquals(0, task.getMissingMismatchAttributeInfo().length());
    assertEquals(3, task.getPredictionLabels().size());

    for (int i = 0; i < train.numInstances(); i++) {
      assertEquals(3, task.processInstance(train.instance(i)).length);
    }
  }

  @Test
  public void testScoreWithClassifierSomeMissingFields() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    NaiveBayes bayes = new NaiveBayes();

    bayes.buildClassifier(train);

    WekaScoringMapTask task = new WekaScoringMapTask();
    Remove r = new Remove();
    r.setAttributeIndices("1");
    r.setInputFormat(train);
    Instances test = Filter.useFilter(train, r);

    task.setModel(bayes, train, test);

    assertTrue(task.getMissingMismatchAttributeInfo().length() > 0);
    assertTrue(task.getMissingMismatchAttributeInfo().equals(
      "sepallength missing from incoming data\n"));
    assertEquals(3, task.getPredictionLabels().size());

    for (int i = 0; i < test.numInstances(); i++) {
      assertEquals(3, task.processInstance(test.instance(i)).length);
    }
  }

  public static void main(String[] args) {
    try {
      WekaScoringTaskTest t = new WekaScoringTaskTest();

      t.testScoreWithClassifier();
      t.testScoreWithClassifierSomeMissingFields();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
