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
 * Tests KStar. Run from the command line with:<p>
 * java weka.classifiers.KStarTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class KStarTest extends AbstractClassifierTest {

  public KStarTest(String name) { super(name);  }

  /** Creates a default KStar */
  public Classifier getClassifier() {
    return new KStar();
  }

  public static Test suite() {
    return new TestSuite(KStarTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
