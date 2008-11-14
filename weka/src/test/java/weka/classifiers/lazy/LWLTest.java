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
 * Tests LWL. Run from the command line with:<p>
 * java weka.classifiers.LWLTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LWLTest extends AbstractClassifierTest {

  public LWLTest(String name) { super(name);  }

  /** Creates a default LWL */
  public Classifier getClassifier() {
    return new LWL();
  }

  public static Test suite() {
    return new TestSuite(LWLTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
