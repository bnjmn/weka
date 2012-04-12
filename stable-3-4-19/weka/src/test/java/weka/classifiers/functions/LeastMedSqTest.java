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
 * Tests LeastMedSq. Run from the command line with:<p>
 * java weka.classifiers.LeastMedSqTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LeastMedSqTest extends AbstractClassifierTest {

  public LeastMedSqTest(String name) { super(name);  }

  /** Creates a default LeastMedSq */
  public Classifier getClassifier() {
    return new LeastMedSq();
  }

  public static Test suite() {
    return new TestSuite(LeastMedSqTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
