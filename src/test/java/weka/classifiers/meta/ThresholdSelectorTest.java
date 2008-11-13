/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.NominalPrediction;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NoSupportForMissingValuesException;
import weka.core.SelectedTag;
import weka.core.UnsupportedAttributeTypeException;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests ThresholdSelector. Run from the command line with:<p>
 * java weka.classifiers.meta.ThresholdSelectorTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.6.2.1 $
 */
public class ThresholdSelectorTest 
  extends AbstractClassifierTest {

  private static double[] DIST1 = new double [] {
    0.25,
    0.375,
    0.5,
    0.625,
    0.75,
    0.875,
    1.0
  };

  /** A set of instances to test with */
  protected transient Instances m_Instances;

  /** Used to generate various types of predictions */
  protected transient EvaluationUtils m_Evaluation;

  public ThresholdSelectorTest(String name) { 
    super(name);  
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_Evaluation = new EvaluationUtils();
    m_Instances = new Instances(
                    new BufferedReader(
                      new InputStreamReader(
                        ClassLoader.getSystemResourceAsStream(
                          "weka/classifiers/data/ClassifierTest.arff"))));
  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return getClassifier(DIST1);
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    super.tearDown();

    m_Evaluation = null;
  }

  /**
   * Creates a ThresholdSelector that returns predictions from a 
   * given distribution
   */
  public Classifier getClassifier(double[] dist) {
    return getClassifier(new ThresholdSelectorDummyClassifier(dist));
  }

  /**
   * Creates a ThresholdSelector with the given subclassifier.
   *
   * @param classifier a <code>Classifier</code> to use as the
   * subclassifier
   * @return a new <code>ThresholdSelector</code>
   */
  public Classifier getClassifier(Classifier classifier) {
    ThresholdSelector t = new ThresholdSelector();
    t.setClassifier(classifier);
    return t;
  }

  /**
   * Builds a model using the current classifier using the first
   * half of the current data for training, and generates a bunch of
   * predictions using the remaining half of the data for testing.
   *
   * @return a <code>FastVector</code> containing the predictions.
   */
  protected FastVector useClassifier() throws Exception {

    Classifier dc = null;
    int tot = m_Instances.numInstances();
    int mid = tot / 2;
    Instances train = null;
    Instances test = null;
    try {
      train = new Instances(m_Instances, 0, mid);
      test = new Instances(m_Instances, mid, tot - mid);
      dc = m_Classifier;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Problem setting up to use classifier: " + ex);
    }
    int counter = 0;
    do {
      try {
	return m_Evaluation.getTrainTestPredictions(dc, train, test);
      } catch (UnsupportedAttributeTypeException ex) {
	SelectedTag tag = null;
	boolean invert = false;
	String msg = ex.getMessage();
	if ((msg.indexOf("string") != -1) && 
	    (msg.indexOf("attributes") != -1)) {
	  System.err.println("\nDeleting string attributes.");
	  tag = new SelectedTag(Attribute.STRING,
				RemoveType.TAGS_ATTRIBUTETYPE);
	} else if ((msg.indexOf("only") != -1) && 
		   (msg.indexOf("nominal") != -1)) {
	  System.err.println("\nDeleting non-nominal attributes.");
	  tag = new SelectedTag(Attribute.NOMINAL,
				RemoveType.TAGS_ATTRIBUTETYPE);
	  invert = true;
	} else if ((msg.indexOf("only") != -1) && 
		   (msg.indexOf("numeric") != -1)) {
	  System.err.println("\nDeleting non-numeric attributes.");
	  tag = new SelectedTag(Attribute.NUMERIC,
				RemoveType.TAGS_ATTRIBUTETYPE);
	  invert = true;
	}  else {
	  throw ex;
	}
	RemoveType attFilter = new RemoveType();
	attFilter.setAttributeType(tag);
	attFilter.setInvertSelection(invert);
	attFilter.setInputFormat(train);
	train = Filter.useFilter(train, attFilter);
	attFilter.batchFinished();
	test = Filter.useFilter(test, attFilter);
	counter++;
	if (counter > 2) {
	  throw ex;
	}
      } catch (NoSupportForMissingValuesException ex2) {
	System.err.println("\nReplacing missing values.");
	ReplaceMissingValues rmFilter = new ReplaceMissingValues();
	rmFilter.setInputFormat(train);
	train = Filter.useFilter(train, rmFilter);
	rmFilter.batchFinished();
	test = Filter.useFilter(test, rmFilter);
      } catch (IllegalArgumentException ex3) {
	String msg = ex3.getMessage();
	if (msg.indexOf("Not enough instances") != -1) {
	  System.err.println("\nInflating training data.");
	  Instances trainNew = new Instances(train);
	  for (int i = 0; i < train.numInstances(); i++) {
	    trainNew.add(train.instance(i));
	  }
	  train = trainNew;
	} else {
	  throw ex3;
	}
      }
    } while (true);
  }

  public void testRangeNone() throws Exception {
    
    int cind = 0;
    ((ThresholdSelector)m_Classifier).setDesignatedClass(new SelectedTag(ThresholdSelector.OPTIMIZE_0, ThresholdSelector.TAGS_OPTIMIZE));
    ((ThresholdSelector)m_Classifier).setRangeCorrection(new SelectedTag(ThresholdSelector.RANGE_NONE, ThresholdSelector.TAGS_RANGE));
    FastVector result = null;
    m_Instances.setClassIndex(1);
    result = useClassifier();
    assertTrue(result.size() != 0);
    double minp = 0;
    double maxp = 0;
    for (int i = 0; i < result.size(); i++) {
      NominalPrediction p = (NominalPrediction)result.elementAt(i);
      double prob = p.distribution()[cind];
      if ((i == 0) || (prob < minp)) minp = prob;
      if ((i == 0) || (prob > maxp)) maxp = prob;
    }
    assertTrue("Upper limit shouldn't increase", maxp <= 1.0);
    assertTrue("Lower limit shouldn'd decrease", minp >= 0.25);
  }
  
  public void testDesignatedClass() throws Exception {
    
    int cind = 0;
    for (int i = 0; i < ThresholdSelector.TAGS_OPTIMIZE.length; i++) {
      ((ThresholdSelector)m_Classifier).setDesignatedClass(new SelectedTag(ThresholdSelector.TAGS_OPTIMIZE[i].getID(), ThresholdSelector.TAGS_OPTIMIZE));
      m_Instances.setClassIndex(1);
      FastVector result = useClassifier();
      assertTrue(result.size() != 0);
    }
  }

  public void testEvaluationMode() throws Exception {
    
    int cind = 0;
    for (int i = 0; i < ThresholdSelector.TAGS_EVAL.length; i++) {
      ((ThresholdSelector)m_Classifier).setEvaluationMode(new SelectedTag(ThresholdSelector.TAGS_EVAL[i].getID(), ThresholdSelector.TAGS_EVAL));
      m_Instances.setClassIndex(1);
      FastVector result = useClassifier();
      assertTrue(result.size() != 0);
    }
  }

  public void testNumXValFolds() throws Exception {
    
    try {
      ((ThresholdSelector)m_Classifier).setNumXValFolds(0);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }

    int cind = 0;
    for (int i = 2; i < 20; i += 2) {
      ((ThresholdSelector)m_Classifier).setNumXValFolds(i);
      m_Instances.setClassIndex(1);
      FastVector result = useClassifier();
      assertTrue(result.size() != 0);
    }
  }

  public static Test suite() {
    return new TestSuite(ThresholdSelectorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
