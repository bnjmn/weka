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
 * Tests AttributeSelectedClassifier. Run from the command line with:<p>
 * java weka.classifiers.AttributeSelectedClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class AttributeSelectedClassifierTest extends AbstractClassifierTest {

  public AttributeSelectedClassifierTest(String name) { super(name);  }

  /** Creates a default AttributeSelectedClassifier */
  public Classifier getClassifier() {
    return new AttributeSelectedClassifier();
  }

  public static Test suite() {
    return new TestSuite(AttributeSelectedClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
