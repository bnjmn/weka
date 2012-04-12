/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.rules.ZeroR;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Dummy test for user classifier. Actually uses ZeroR. Run from the
 * command line with:<p> java weka.classifiers.UserClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.2 $
 */
public class UserClassifierTest extends AbstractClassifierTest {

  public UserClassifierTest(String name) { super(name);  }

  /** Creates a default UserClassifier */
  public Classifier getClassifier() {
    return new ZeroR();
  }

  public static Test suite() {
    return new TestSuite(UserClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
