/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.lazy;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests LWR. Run from the command line with:<p>
 * java weka.classifiers.LWRTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LWRTest extends AbstractClassifierTest {

  public LWRTest(String name) { super(name);  }

  /** Creates a default LWR */
  public Classifier getClassifier() {
    return new LWR();
  }

  public static Test suite() {
    return new TestSuite(LWRTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
