/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.classifiers.evaluation.NominalPrediction;

/**
 * Tests ThresholdSelector. Run from the command line with:<p>
 * java weka.classifiers.ThresholdSelectorTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.4 $
 */
public class ThresholdSelectorTest extends AbstractClassifierTest {

  private class DummyClassifier extends DistributionClassifier {
    
    double [] m_Preds;
    int m_Pos;
    Random m_Rand = new Random(42);
    public DummyClassifier(double [] preds) {
      m_Preds = preds;
    }
    public void buildClassifier(Instances train) { }
    public double [] distributionForInstance(Instance test) throws Exception {

      double [] result = new double[test.numClasses()];
      int pred = 0;
      result[pred] = m_Preds[m_Pos];
      double residual = (1.0 - result[pred]) / (result.length - 1);
      for (int i = 0; i < result.length; i++) {
        if (i != pred) {
          result[i] = residual;
        }
      }
      m_Pos = (m_Pos + 1) % m_Preds.length;
      return result;
    }
  }

  private static double [] DIST1 = new double [] {
    0.25,
    0.375,
    0.5,
    0.625,
    0.75,
    0.875,
    1.0
  };

  public ThresholdSelectorTest(String name) { super(name);  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return getClassifier(DIST1);
  }

  /**
   * Creates a ThresholdSelector that returns predictions from a 
   * given distribution
   */
  public Classifier getClassifier(double []dist) {
    return getClassifier(new DummyClassifier(dist));
  }

  /**
   * Creates a ThresholdSelector with the given subclassifier.
   *
   * @param classifier a <code>DistributionClassifier</code> to use as the
   * subclassifier
   * @return a new <code>ThresholdSelector</code>
   */
  public Classifier getClassifier(DistributionClassifier classifier) {
    ThresholdSelector t = new ThresholdSelector();
    t.setDistributionClassifier(classifier);
    return t;
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
