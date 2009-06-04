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
 * Tests Ridor. Run from the command line with:<p>
 * java weka.classifiers.RidorTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class RidorTest extends AbstractClassifierTest {

  public RidorTest(String name) { super(name);  }

  /** Creates a default Ridor */
  public Classifier getClassifier() {
    return new Ridor();
  }

  public static Test suite() {
    return new TestSuite(RidorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
