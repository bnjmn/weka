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
 * Tests Cleanser. Run from the command line with:<p>
 * java weka.classifiers.CleanserTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class CleanserTest extends AbstractClassifierTest {

  public CleanserTest(String name) { super(name);  }

  /** Creates a default Cleanser */
  public Classifier getClassifier() {
    return new Cleanser();
  }

  public static Test suite() {
    return new TestSuite(CleanserTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
