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
 * Tests M5Rules. Run from the command line with:<p>
 * java weka.classifiers.M5RulesTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class M5RulesTest extends AbstractClassifierTest {

  public M5RulesTest(String name) { super(name);  }

  /** Creates a default M5Rules */
  public Classifier getClassifier() {
    return new M5Rules();
  }

  public static Test suite() {
    return new TestSuite(M5RulesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
