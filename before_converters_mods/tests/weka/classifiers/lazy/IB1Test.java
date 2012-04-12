/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.lazy;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests IB1. Run from the command line with:<p>
 * java weka.classifiers.IB1Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class IB1Test extends AbstractClassifierTest {

  public IB1Test(String name) { super(name);  }

  /** Creates a default IB1 */
  public Classifier getClassifier() {
    return new IB1();
  }

  public static Test suite() {
    return new TestSuite(IB1Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
