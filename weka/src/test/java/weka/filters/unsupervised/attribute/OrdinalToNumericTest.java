/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2017 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests OrdinalToNumericTest.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class OrdinalToNumericTest extends AbstractFilterTest {

  @Override
  public Filter getFilter() {
    return new OrdinalToNumeric();
  }

  public OrdinalToNumericTest(String name) {
    super(name);
  }

  public void testTypical() {
    m_Filter = getFilter();

    Instances result = useFilter();

    assertEquals(m_Instances.numAttributes(), result.numAttributes());

    // check that the two nominal attributes are now numeric
    assertTrue(result.attribute(1).isNumeric());
    assertTrue(result.attribute(4).isNumeric());
  }

  public void testNoNominalsInRange() {
    m_Filter = getFilter();
    ((OrdinalToNumeric) m_Filter).setAttributesToOperateOn("1,3-4,6,last");
    Instances result = useFilter();

    assertTrue(result.attribute(1).isNominal());
    assertTrue(result.attribute(4).isNominal());
  }

  public static Test suite() {
    return new TestSuite(OrdinalToNumericTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
