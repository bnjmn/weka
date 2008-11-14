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
 * Tests Logistic. Run from the command line with:<p>
 * java weka.classifiers.LogisticTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LogisticTest extends AbstractClassifierTest {

  public LogisticTest(String name) { super(name);  }

  /** Creates a default Logistic */
  public Classifier getClassifier() {
    return new Logistic();
  }

  public static Test suite() {
    return new TestSuite(LogisticTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
