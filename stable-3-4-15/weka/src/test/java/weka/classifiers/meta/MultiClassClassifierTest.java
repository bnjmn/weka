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
 * Tests MultiClassClassifier. Run from the command line with:<p>
 * java weka.classifiers.MultiClassClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class MultiClassClassifierTest extends AbstractClassifierTest {

  public MultiClassClassifierTest(String name) { super(name);  }

  /** Creates a default MultiClassClassifier */
  public Classifier getClassifier() {
    return new MultiClassClassifier();
  }

  public static Test suite() {
    return new TestSuite(MultiClassClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
