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
 * Tests BayesNet. Run from the command line with:<p>
 * java weka.classifiers.BayesNetTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class BayesNetTest extends AbstractClassifierTest {

  public BayesNetTest(String name) { super(name);  }

  /** Creates a default BayesNet */
  public Classifier getClassifier() {
    return new BayesNet();
  }

  public static Test suite() {
    return new TestSuite(BayesNetTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
