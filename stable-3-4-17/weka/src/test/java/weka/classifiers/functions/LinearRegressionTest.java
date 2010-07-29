/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.functions;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests LinearRegression. Run from the command line with:<p>
 * java weka.classifiers.LinearRegressionTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LinearRegressionTest extends AbstractClassifierTest {

  public LinearRegressionTest(String name) { super(name);  }

  /** Creates a default LinearRegression */
  public Classifier getClassifier() {
    return new LinearRegression();
  }

  public static Test suite() {
    return new TestSuite(LinearRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
