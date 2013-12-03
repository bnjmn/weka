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
 * Copyright (C) 2010 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests Denormalize. Run from the command line with:
 * <p>
 * java weka.filters.unsupervised.instance.DenormalizeTest
 * 
 * @author Mark Hall
 * @version $Revision$
 */
public class DenormalizeTest extends AbstractFilterTest {

  public DenormalizeTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);
    m_FilteredClassifier = null;
  }

  /** Creates an example Denormalize */
  @Override
  public Filter getFilter() {
    Denormalize f = new Denormalize();
    f.setGroupingAttribute("2");
    return f;
  }

  public static Test suite() {
    return new TestSuite(DenormalizeTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
