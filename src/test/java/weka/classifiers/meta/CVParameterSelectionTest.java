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
 * Tests CVParameterSelection. Run from the command line with:<p>
 * java weka.classifiers.CVParameterSelectionTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class CVParameterSelectionTest extends AbstractClassifierTest {

  public CVParameterSelectionTest(String name) { super(name);  }

  /** Creates a default CVParameterSelection */
  public Classifier getClassifier() {
    return new CVParameterSelection();
  }

  public static Test suite() {
    return new TestSuite(CVParameterSelectionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
