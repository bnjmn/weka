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
 * Tests OrdinalClassClassifier. Run from the command line with:<p>
 * java weka.classifiers.OrdinalClassClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class OrdinalClassClassifierTest extends AbstractClassifierTest {

  public OrdinalClassClassifierTest(String name) { super(name);  }

  /** Creates a default OrdinalClassClassifier */
  public Classifier getClassifier() {
    return new OrdinalClassClassifier();
  }

  public static Test suite() {
    return new TestSuite(OrdinalClassClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
