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
 * Tests SMOreg. Run from the command line with:<p>
 * java weka.classifiers.SMOregTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class SMOregTest extends AbstractClassifierTest {

  public SMOregTest(String name) { super(name);  }

  /** Creates a default SMOreg */
  public Classifier getClassifier() {
    return new SMOreg();
  }

  public static Test suite() {
    return new TestSuite(SMOregTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
