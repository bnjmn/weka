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

package weka.filters.unsupervised.instance;

import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Normalize. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.NormalizeTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class NormalizeTest 
  extends AbstractFilterTest {

  /** for comparing the instances */
  protected InstanceComparator m_Comparator;
  
  public NormalizeTest(String name) { 
    super(name);  
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);
    m_Comparator = new InstanceComparator(true);
  }
  
  /** Creates a default Normalize */
  public Filter getFilter() {
    Normalize f = new Normalize();
    return f;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    // at least one instance must be different
    boolean equal = true;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (m_Comparator.compare(
            m_Instances.instance(i), result.instance(i)) != 0) {
        equal = false;
        break;
      }
    }
    if (equal)
      fail("Nothing changed!");
  }

  public static Test suite() {
    return new TestSuite(NormalizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
