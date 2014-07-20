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

/**
 * RainbowTest.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */
package weka.core.stopwords;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the Rainbow stopwords algorithm. Run from commandline as follows:<br/>
 *   java weka.core.stopwords.RainbowTest
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class RainbowTest
  extends AbstractStopwordsTest {

  /**
   * Constructs the test.
   * 
   * @param name the name of the test
   */
  public RainbowTest(String name) {
    super(name);
  }

  /**
   * Used to create an instance of a specific stopwords scheme.
   * 
   * @return a suitably configured <code>StopwordsHandler</code> value
   */
  @Override
  public StopwordsHandler getStopwords() {
    return new Rainbow();
  }

  public static Test suite() {
    return new TestSuite(RainbowTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
