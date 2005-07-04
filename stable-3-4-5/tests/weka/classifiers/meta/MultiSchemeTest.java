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
 * Tests MultiScheme. Run from the command line with:<p>
 * java weka.classifiers.MultiSchemeTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class MultiSchemeTest extends AbstractClassifierTest {

  public MultiSchemeTest(String name) { super(name);  }

  /** Creates a default MultiScheme */
  public Classifier getClassifier() {
    return new MultiScheme();
  }

  public static Test suite() {
    return new TestSuite(MultiSchemeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
