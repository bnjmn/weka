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
 * Tests VFI. Run from the command line with:<p>
 * java weka.classifiers.VFITest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class VFITest extends AbstractClassifierTest {

  public VFITest(String name) { super(name);  }

  /** Creates a default VFI */
  public Classifier getClassifier() {
    return new VFI();
  }

  public static Test suite() {
    return new TestSuite(VFITest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
