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
 * Tests JRip. Run from the command line with:<p>
 * java weka.classifiers.JRipTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class JRipTest extends AbstractClassifierTest {

  public JRipTest(String name) { super(name);  }

  /** Creates a default JRip */
  public Classifier getClassifier() {
    return new JRip();
  }

  public static Test suite() {
    return new TestSuite(JRipTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
