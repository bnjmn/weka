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
 * Tests AdaBoostM1. Run from the command line with:<p>
 * java weka.classifiers.AdaBoostM1Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class AdaBoostM1Test extends AbstractClassifierTest {

  public AdaBoostM1Test(String name) { super(name);  }

  /** Creates a default AdaBoostM1 */
  public Classifier getClassifier() {
    return new AdaBoostM1();
  }

  public static Test suite() {
    return new TestSuite(AdaBoostM1Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
