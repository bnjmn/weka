/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests AddFilter. Run from the command line with:<p>
 * java weka.filters.AddFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class AddFilterTest extends AbstractFilterTest {
  
  public AddFilterTest(String name) { super(name);  }

  /** Creates a default AddFilter */
  public Filter getFilter() {
    return new AddFilter();
  }

  /** Creates a specialized AddFilter */
  public Filter getFilter(int pos) {
    AddFilter af = new AddFilter();
    af.setAttributeIndex(pos);
    return af;
  }

  public void testAddFirst() {
    m_Filter = getFilter(0);
    testBuffered();
  }

  public void testAddLast() {
    m_Filter = getFilter(m_Instances.numAttributes() - 1);
    testBuffered();
  }

  public void testAddNominal() {
    m_Filter = getFilter();
    try {
      ((AddFilter)m_Filter).setNominalLabels("hello,there,bob");
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Couldn't set list of nominal labels for AddFilter");
    }
    testBuffered();
  }

  public static Test suite() {
    return new TestSuite(AddFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
