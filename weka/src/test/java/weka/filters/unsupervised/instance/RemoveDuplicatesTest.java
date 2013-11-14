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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests Resample. Run from the command line with:
 * <p>
 * java weka.filters.unsupervised.instance.RemoveDuplicatesTest
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class RemoveDuplicatesTest extends AbstractFilterTest {

  public RemoveDuplicatesTest(String name) {
    super(name);
  }

  /** Creates a default Resample */
  @Override
  public Filter getFilter() {
    RemoveDuplicates f = new RemoveDuplicates();
    return f;
  }

  public static Test suite() {
    return new TestSuite(RemoveDuplicatesTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
