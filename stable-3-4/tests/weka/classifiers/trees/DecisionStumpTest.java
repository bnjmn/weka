/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.trees;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests DecisionStump. Run from the command line with:<p>
 * java weka.classifiers.DecisionStumpTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class DecisionStumpTest extends AbstractClassifierTest {

  public DecisionStumpTest(String name) { super(name);  }

  /** Creates a default DecisionStump */
  public Classifier getClassifier() {
    return new DecisionStump();
  }

  public static Test suite() {
    return new TestSuite(DecisionStumpTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
