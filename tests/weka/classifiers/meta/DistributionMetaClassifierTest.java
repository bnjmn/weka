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
 * Tests DistributionMetaClassifier. Run from the command line with:<p>
 * java weka.classifiers.DistributionMetaClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class DistributionMetaClassifierTest extends AbstractClassifierTest {

  public DistributionMetaClassifierTest(String name) { super(name);  }

  /** Creates a default DistributionMetaClassifier */
  public Classifier getClassifier() {
    return new DistributionMetaClassifier();
  }

  public static Test suite() {
    return new TestSuite(DistributionMetaClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
