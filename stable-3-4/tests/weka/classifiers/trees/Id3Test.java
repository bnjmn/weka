/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.trees;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests Id3. Run from the command line with:<p>
 * java weka.classifiers.Id3Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class Id3Test extends AbstractClassifierTest {

  public Id3Test(String name) { super(name);  }

  /** Creates a default Id3 */
  public Classifier getClassifier() {
    return new Id3();
  }

  public static Test suite() {
    return new TestSuite(Id3Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
