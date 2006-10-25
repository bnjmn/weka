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

package weka.datagenerators;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

/**
 * Abstract Test class for DataGenerators.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public abstract class AbstractDataGeneratorTest 
  extends TestCase {

  /** The datagenerator to be tested */
  protected DataGenerator m_Generator;

  /** for storing the result */
  protected StringWriter m_Output;

  /**
   * Constructs the <code>AbstractDataGeneratorTest</code>. 
   * Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractDataGeneratorTest(String name) { 
    super(name); 
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default datagenerator to test.
   *
   * @throws Exception if an error occurs 
   */
  protected void setUp() throws Exception {
    m_Generator = getGenerator();
    m_Output    = new StringWriter();
    m_Generator.setOutput(new PrintWriter(m_Output));
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Generator = null;
    m_Output    = null;
  }

  /**
   * Used to create an instance of a specific DataGenerator.
   *
   * @return a suitably configured <code>DataGenerator</code> value
   */
  public abstract DataGenerator getGenerator();

  /**
   * tests whether setting the options returned by getOptions() works
   */
  public void testOptions() {
    try {
      m_Generator.setOptions(m_Generator.getOptions());
    }
    catch (Exception e) {
      fail("setOptions(getOptions()) does not work: " + e.getMessage());
    }
  }

  /**
   * tests whether data can be generated with the default options
   */
  public void testMakeData() {
    try {
      m_Generator.makeData(m_Generator, new String[0]);
    }
    catch (Exception e) {
      fail("Generation of data failed: " + e.getMessage());
    }
  }
}
