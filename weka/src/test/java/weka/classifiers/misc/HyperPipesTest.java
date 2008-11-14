/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.misc;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests HyperPipes. Run from the command line with:<p>
 * java weka.classifiers.HyperPipesTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class HyperPipesTest extends AbstractClassifierTest {

  public HyperPipesTest(String name) { super(name);  }

  /** Creates a default HyperPipes */
  public Classifier getClassifier() {
    return new HyperPipes();
  }

  public static Test suite() {
    return new TestSuite(HyperPipesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
