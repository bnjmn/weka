/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import java.io.File;

/**
 * Tests CostSensitiveClassifier. Run from the command line with:<p>
 * java weka.classifiers.CostSensitiveClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.2 $
 */
public class CostSensitiveClassifierTest extends AbstractClassifierTest {

  public CostSensitiveClassifierTest(String name) { super(name);  }

  /** Creates a default CostSensitiveClassifier */
  public Classifier getClassifier() {

    CostSensitiveClassifier cl = new CostSensitiveClassifier();
    String str = ClassLoader.getSystemResource("weka/classifiers/data").toString();
    str = str.substring(5);
    cl.setOnDemandDirectory(new File(str));
    return cl;
  }

  public static Test suite() {
    return new TestSuite(CostSensitiveClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
