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
 * Tests ADTree. Run from the command line with:<p>
 * java weka.classifiers.ADTreeTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class ADTreeTest extends AbstractClassifierTest {

  public ADTreeTest(String name) { super(name);  }

  /** Creates a default ADTree */
  public Classifier getClassifier() {
    return new ADTree();
  }

  public static Test suite() {
    return new TestSuite(ADTreeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
