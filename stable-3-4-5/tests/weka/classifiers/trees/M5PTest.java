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
 * Tests M5P. Run from the command line with:<p>
 * java weka.classifiers.M5PTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class M5PTest extends AbstractClassifierTest {

  public M5PTest(String name) { super(name);  }

  /** Creates a default M5P */
  public Classifier getClassifier() {
    return new M5P();
  }

  public static Test suite() {
    return new TestSuite(M5PTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
