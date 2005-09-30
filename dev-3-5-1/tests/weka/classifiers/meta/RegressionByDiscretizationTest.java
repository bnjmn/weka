/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests RegressionByDiscretization. Run from the command line with:<p>
 * java weka.classifiers.RegressionByDiscretizationTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class RegressionByDiscretizationTest extends AbstractClassifierTest {

  public RegressionByDiscretizationTest(String name) { super(name);  }

  /** Creates a default RegressionByDiscretization */
  public Classifier getClassifier() {
    return new RegressionByDiscretization();
  }

  public static Test suite() {
    return new TestSuite(RegressionByDiscretizationTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
