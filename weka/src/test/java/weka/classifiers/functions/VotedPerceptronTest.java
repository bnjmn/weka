/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.functions;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests VotedPerceptron. Run from the command line with:<p>
 * java weka.classifiers.VotedPerceptronTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class VotedPerceptronTest extends AbstractClassifierTest {

  public VotedPerceptronTest(String name) { super(name);  }

  /** Creates a default VotedPerceptron */
  public Classifier getClassifier() {
    return new VotedPerceptron();
  }

  public static Test suite() {
    return new TestSuite(VotedPerceptronTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
