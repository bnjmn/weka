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
 * Tests Winnow. Run from the command line with:<p>
 * java weka.classifiers.WinnowTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class WinnowTest extends AbstractClassifierTest {

  public WinnowTest(String name) { super(name);  }

  /** Creates a default Winnow */
  public Classifier getClassifier() {
    return new Winnow();
  }

  public static Test suite() {
    return new TestSuite(WinnowTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
