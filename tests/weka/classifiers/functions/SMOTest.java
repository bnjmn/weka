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
 * Tests SMO. Run from the command line with:<p>
 * java weka.classifiers.SMOTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.3 $
 */
public class SMOTest extends AbstractClassifierTest {

  public SMOTest(String name) { super(name);  }

  /** Creates a default SMO */
  public Classifier getClassifier() {
    return new SMO();
  }

  public static Test suite() {
    return new TestSuite(SMOTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
