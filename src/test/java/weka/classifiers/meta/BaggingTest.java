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
 * Tests Bagging. Run from the command line with:<p>
 * java weka.classifiers.BaggingTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class BaggingTest extends AbstractClassifierTest {

  public BaggingTest(String name) { super(name);  }

  /** Creates a default Bagging */
  public Classifier getClassifier() {
    return new Bagging();
  }

  public static Test suite() {
    return new TestSuite(BaggingTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
