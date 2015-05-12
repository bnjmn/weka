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
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.clusterers;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the ScikitLearnClusterer
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ScikitLearnClustererTest extends
  weka.clusterers.AbstractClustererTest {

  public ScikitLearnClustererTest(String name) {
    super(name);
  }

  @Override
  public Clusterer getClusterer() {
    ScikitLearnClusterer c = new ScikitLearnClusterer();
    c.setLearnerOpts("random_state=1");
    return c;
  }

  public static Test suite() {
    return new TestSuite(ScikitLearnClustererTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
