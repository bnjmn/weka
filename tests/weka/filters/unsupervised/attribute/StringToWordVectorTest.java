/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests StringToWordVector. Run from the command line with:<p>
 * java weka.filters.StringToWordVectorTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class StringToWordVectorTest extends AbstractFilterTest {
  
  public StringToWordVectorTest(String name) { super(name);  }

  /** Creates an example StringToWordVector */
  public Filter getFilter() {
    StringToWordVector f = new StringToWordVector();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public void testWordsToKeep() {
    ((StringToWordVector)m_Filter).setWordsToKeep(3);
    Instances result = useFilter();
    // Number of instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Number of attributes will be minus 2 string attributes plus
    // the word attributes (aiming for 3 -- could be higher in the case of ties)
    assertEquals(m_Instances.numAttributes() - 2 + 3, result.numAttributes());
  }


  public static Test suite() {
    return new TestSuite(StringToWordVectorTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
