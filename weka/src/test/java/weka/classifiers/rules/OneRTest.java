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
 * Tests OneR. Run from the command line with:<p>
 * java weka.classifiers.OneRTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class OneRTest extends AbstractClassifierTest {

  public OneRTest(String name) { super(name);  }

  /** Creates a default OneR */
  public Classifier getClassifier() {
    return new OneR();
  }

  public static Test suite() {
    return new TestSuite(OneRTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
