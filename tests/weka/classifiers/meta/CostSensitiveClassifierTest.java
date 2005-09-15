/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import weka.core.Instances;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.InputStreamReader;

/**
 * Tests CostSensitiveClassifier. Run from the command line with:<p>
 * java weka.classifiers.meta.CostSensitiveClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.2.2.1 $
 */
public class CostSensitiveClassifierTest extends AbstractClassifierTest {

  public CostSensitiveClassifierTest(String name) { super(name);  }

  /** Creates a default CostSensitiveClassifier */
  public Classifier getClassifier() {

    CostSensitiveClassifier cl = new CostSensitiveClassifier();
    
    // load costmatrix
    try {
      cl.setCostMatrix(
          new CostMatrix(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream(
                  "weka/classifiers/data/ClassifierTest.cost"))));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return cl;
  }

  public static Test suite() {
    return new TestSuite(CostSensitiveClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
