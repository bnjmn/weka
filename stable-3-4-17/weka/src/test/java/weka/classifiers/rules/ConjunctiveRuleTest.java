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
 * Tests ConjunctiveRule. Run from the command line with:<p>
 * java weka.classifiers.ConjunctiveRuleTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class ConjunctiveRuleTest extends AbstractClassifierTest {

  public ConjunctiveRuleTest(String name) { super(name);  }

  /** Creates a default ConjunctiveRule */
  public Classifier getClassifier() {
    return new ConjunctiveRule();
  }

  public static Test suite() {
    return new TestSuite(ConjunctiveRuleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
