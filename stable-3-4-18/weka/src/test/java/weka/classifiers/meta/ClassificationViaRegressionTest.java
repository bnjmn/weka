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
 * Tests ClassificationViaRegression. Run from the command line with:<p>
 * java weka.classifiers.ClassificationViaRegressionTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class ClassificationViaRegressionTest extends AbstractClassifierTest {

  public ClassificationViaRegressionTest(String name) { super(name);  }

  /** Creates a default ClassificationViaRegression */
  public Classifier getClassifier() {
    return new ClassificationViaRegression();
  }

  public static Test suite() {
    return new TestSuite(ClassificationViaRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
