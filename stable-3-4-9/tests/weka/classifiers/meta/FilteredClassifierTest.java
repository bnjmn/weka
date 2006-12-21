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
 * Tests FilteredClassifier. Run from the command line with:<p>
 * java weka.classifiers.FilteredClassifierTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class FilteredClassifierTest extends AbstractClassifierTest {

  public FilteredClassifierTest(String name) { super(name);  }

  /** Creates a default FilteredClassifier */
  public Classifier getClassifier() {
    return new FilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(FilteredClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
