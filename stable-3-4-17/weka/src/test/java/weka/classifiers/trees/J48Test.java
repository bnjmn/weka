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
 * Tests J48. Run from the command line with:<p>
 * java weka.classifiers.J48Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class J48Test extends AbstractClassifierTest {

  public J48Test(String name) { super(name);  }

  /** Creates a default J48 */
  public Classifier getClassifier() {
    return new J48();
  }

  public static Test suite() {
    return new TestSuite(J48Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
