/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.classifiers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests ZeroR. Run from the command line with:<p>
 * java weka.classifiers.ZeroRTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class ZeroRTest extends AbstractClassifierTest {
  
  public ZeroRTest(String name) { super(name);  }

  /** Creates a default ZeroR */
  public Classifier getClassifier() {
    return new ZeroR();
  }

  public static Test suite() {
    return new TestSuite(ZeroRTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
