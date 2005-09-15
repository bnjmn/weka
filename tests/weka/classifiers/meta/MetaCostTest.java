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
 * Tests MetaCost. Run from the command line with:<p>
 * java weka.classifiers.meta.MetaCostTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.2.2.1 $
 */
public class MetaCostTest extends AbstractClassifierTest {

  public MetaCostTest(String name) { super(name);  }

  /** Creates a default MetaCost */
  public Classifier getClassifier() {

    MetaCost cl = new MetaCost();
    
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
    return new TestSuite(MetaCostTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
