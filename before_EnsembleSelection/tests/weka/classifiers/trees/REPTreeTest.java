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
 * Tests REPTree. Run from the command line with:<p>
 * java weka.classifiers.REPTreeTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class REPTreeTest extends AbstractClassifierTest {

  public REPTreeTest(String name) { super(name);  }

  /** Creates a default REPTree */
  public Classifier getClassifier() {
    return new REPTree();
  }

  public static Test suite() {
    return new TestSuite(REPTreeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
