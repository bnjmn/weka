/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.rules;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests DecisionTable. Run from the command line with:<p>
 * java weka.classifiers.DecisionTableTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class DecisionTableTest extends AbstractClassifierTest {

  public DecisionTableTest(String name) { super(name);  }

  /** Creates a default DecisionTable */
  public Classifier getClassifier() {
    return new DecisionTable();
  }

  public static Test suite() {
    return new TestSuite(DecisionTableTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
