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
 * Tests BayesNetB2. Run from the command line with:<p>
 * java weka.classifiers.BayesNetB2Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class BayesNetB2Test extends AbstractClassifierTest {

  public BayesNetB2Test(String name) { super(name);  }

  /** Creates a default BayesNetB2 */
  public Classifier getClassifier() {
    return new BayesNetB2();
  }

  public static Test suite() {
    return new TestSuite(BayesNetB2Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
