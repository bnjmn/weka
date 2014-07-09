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
 * Copyright (C) 2014 University of Waikato 
 */

package weka.filters.supervised.instance;

import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Resample. Run from the command line with:<p>
 * java weka.filters.supervised.instance.ClassBalancer
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 8034 $
 */
public class ClassBalancerTest extends AbstractFilterTest {
  
  public ClassBalancerTest(String name) { super(name);  }
  
  /** Creates a default StratifiedRemoveFolds */
  public Filter getFilter() {
    ClassBalancer f = new ClassBalancer();
    return f;
  }
  
  /** Remove string attributes from default fixture instances */
  protected void setUp() throws Exception {
    
    super.setUp();
    m_Instances.setClassIndex(1);
  }

  public static Test suite() {
    return new TestSuite(ClassBalancerTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
