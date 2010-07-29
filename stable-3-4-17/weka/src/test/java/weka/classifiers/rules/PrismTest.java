/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.rules;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests Prism. Run from the command line with:<p>
 * java weka.classifiers.PrismTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class PrismTest extends AbstractClassifierTest {

  public PrismTest(String name) { super(name);  }

  /** Creates a default Prism */
  public Classifier getClassifier() {
    return new Prism();
  }

  public static Test suite() {
    return new TestSuite(PrismTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
