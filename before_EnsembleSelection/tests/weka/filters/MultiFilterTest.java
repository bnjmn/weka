/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.filters;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.AddExpression;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests MultiFilter. Run from the command line with: <p/>
 * java weka.filters.MultiFilterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision $
 */
public class MultiFilterTest extends AbstractFilterTest {
  
  public MultiFilterTest(String name) { 
    super(name);  
  }

  /** Creates a default MultiFilter */
  public Filter getFilter() {
    return new MultiFilter();
  }

  /** Creates a configured MultiFilter */
  public Filter getConfiguredFilter() {
    MultiFilter result = new MultiFilter();
    
    Filter[] filters = new Filter[2];
    filters[0] = new Add();
    ((Add) filters[0]).setAttributeIndex("last");
    filters[1] = new AddExpression();
    ((AddExpression) filters[1]).setExpression("a3+a6");
    
    result.setFilters(filters);
    
    return result;
  }

  public void testDefault() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public void testConfigured() {
    m_Filter = getConfiguredFilter();
    Instances result = useFilter();
    // Number of attributes should be 2 more
    assertEquals(m_Instances.numAttributes() + 2, result.numAttributes());
    // Number of instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(MultiFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
