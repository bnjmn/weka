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
 * Tests LogitBoost. Run from the command line with:<p>
 * java weka.classifiers.LogitBoostTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class LogitBoostTest extends AbstractClassifierTest {

  public LogitBoostTest(String name) { super(name);  }

  /** Creates a default LogitBoost */
  public Classifier getClassifier() {
    return new LogitBoost();
  }

  public static Test suite() {
    return new TestSuite(LogitBoostTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
