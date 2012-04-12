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
 * Tests NaiveBayes. Run from the command line with:<p>
 * java weka.classifiers.NaiveBayesTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class NaiveBayesTest extends AbstractClassifierTest {

  public NaiveBayesTest(String name) { super(name);  }

  /** Creates a default NaiveBayes */
  public Classifier getClassifier() {
    return new NaiveBayes();
  }

  public static Test suite() {
    return new TestSuite(NaiveBayesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
