/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;

/**
 * Tests StringToWordVectorFilter. Run from the command line with:<p>
 * java weka.filters.StringToWordVectorFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class StringToWordVectorFilterTest extends AbstractFilterTest {
  
  public StringToWordVectorFilterTest(String name) { super(name);  }

  /** Creates an example StringToWordVectorFilter */
  public Filter getFilter() {
    StringToWordVectorFilter f = new StringToWordVectorFilter();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public void testWordsToKeep() {
    ((StringToWordVectorFilter)m_Filter).setWordsToKeep(3);
    Instances result = useFilter();
    // Number of instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Number of attributes will be minus 2 string attributes plus
    // the word attributes (aiming for 3 -- could be higher in the case of ties)
    assertEquals(m_Instances.numAttributes() - 2 + 3, result.numAttributes());
  }


  public static Test suite() {
    return new TestSuite(StringToWordVectorFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
