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
 * Tests MetaCost. Run from the command line with:<p>
 * java weka.classifiers.MetaCostTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.2 $
 */
public class MetaCostTest extends AbstractClassifierTest {

  public MetaCostTest(String name) { super(name);  }

  /** Creates a default MetaCost */
  public Classifier getClassifier() {

    MetaCost cl = new MetaCost();
    String str = ClassLoader.getSystemResource("weka/classifiers/data").toString();
    str = str.substring(5);
    cl.setOnDemandDirectory(new File(str));
    return cl;
  }

  public static Test suite() {
    return new TestSuite(MetaCostTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
