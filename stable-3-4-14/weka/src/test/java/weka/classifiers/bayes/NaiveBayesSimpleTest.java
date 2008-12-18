/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.bayes;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests NaiveBayesSimple. Run from the command line with:<p>
 * java weka.classifiers.NaiveBayesSimpleTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class NaiveBayesSimpleTest extends AbstractClassifierTest {

  public NaiveBayesSimpleTest(String name) { super(name);  }

  /** Creates a default NaiveBayesSimple */
  public Classifier getClassifier() {
    return new NaiveBayesSimple();
  }

  public static Test suite() {
    return new TestSuite(NaiveBayesSimpleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
