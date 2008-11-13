/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.rules;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests PART. Run from the command line with:<p>
 * java weka.classifiers.PARTTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class PARTTest extends AbstractClassifierTest {

  public PARTTest(String name) { super(name);  }

  /** Creates a default PART */
  public Classifier getClassifier() {
    return new PART();
  }

  public static Test suite() {
    return new TestSuite(PARTTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
