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
 * Tests KernelDensity. Run from the command line with:<p>
 * java weka.classifiers.KernelDensityTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class KernelDensityTest extends AbstractClassifierTest {

  public KernelDensityTest(String name) { super(name);  }

  /** Creates a default KernelDensity */
  public Classifier getClassifier() {
    return new KernelDensity();
  }

  public static Test suite() {
    return new TestSuite(KernelDensityTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
