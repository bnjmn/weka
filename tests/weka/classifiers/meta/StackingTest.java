/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests Stacking. Run from the command line with:<p>
 * java weka.classifiers.StackingTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class StackingTest extends AbstractClassifierTest {

  public StackingTest(String name) { super(name);  }

  /** Creates a default Stacking */
  public Classifier getClassifier() {
    return new Stacking();
  }

  public static Test suite() {
    return new TestSuite(StackingTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
