/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.bayes;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests BayesNetB. Run from the command line with:<p>
 * java weka.classifiers.BayesNetBTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class BayesNetBTest extends AbstractClassifierTest {

  public BayesNetBTest(String name) { super(name);  }

  /** Creates a default BayesNetB */
  public Classifier getClassifier() {
    return new BayesNetB();
  }

  public static Test suite() {
    return new TestSuite(BayesNetBTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
