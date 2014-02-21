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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.clusterers;

import weka.clusterers.AbstractClustererTest;
import weka.clusterers.Clusterer;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Canopy. Run from the command line with:<p/>
 * java weka.clusterers.CanopyTest
 *
 * @author Mark Hall
 * @version $Revision$
 */
public class CanopyTest 
  extends AbstractClustererTest {

  public CanopyTest(String name) { 
    super(name);  
  }

  /** Creates a default SimpleKMeans */
  public Clusterer getClusterer() {
    return new SimpleKMeans();
  }

  public static Test suite() {
    return new TestSuite(CanopyTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
