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
 * Copyright (C) 2006 University of Waikato 
 */

package weka.core.converters;

import weka.core.CheckOptionHandler;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.TestInstances;

import junit.framework.TestCase;

/**
 * Abstract Test class for Converters.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public abstract class AbstractConverterTest 
  extends TestCase {

  /** whether the option handling is fully enabled */
  public final static boolean TEST_OPTION_HANDLING = false;

  /** for generating test data */
  protected TestInstances m_TestInstances;
  
  /** the loader */
  protected AbstractLoader m_Loader;
  
  /** the saver */
  protected AbstractSaver m_Saver;
  
  /** the OptionHandler tester for the loader */
  protected CheckOptionHandler m_OptionTesterLoader;
  
  /** the OptionHandler tester for the saver */
  protected CheckOptionHandler m_OptionTesterSaver;
  
  /** the reference dataset */
  protected Instances m_Instances;
  
  /**
   * Constructs the <code>AbstractConverterTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractConverterTest(String name) { 
    super(name); 
  }

  /**
   * returns the loader used in the tests
   * 
   * @return the configured loader
   */
  public abstract AbstractLoader getLoader();

  /**
   * returns the saver used in the tests
   * 
   * @return the configured saver
   */
  public abstract AbstractSaver getSaver();
  
  /**
   * Configures the CheckOptionHandler uses for testing the optionhandling.
   * Sets the scheme to test.
   * 
   * @return	the fully configured CheckOptionHandler
   */
  protected CheckOptionHandler getOptionTester(Object o) {
    CheckOptionHandler		result;
    
    result = new CheckOptionHandler();
    if (o instanceof OptionHandler)
      result.setOptionHandler((OptionHandler) o);
    else
      result.setOptionHandler(null);
    result.setUserOptions(new String[0]);
    result.setSilent(true);
    
    return result;
  }
  
  /**
   * Configures the CheckOptionHandler used for testing the option handling
   * of the loader.
   * Sets the scheme to test.
   * 
   * @return	the fully configured CheckOptionHandler
   */
  protected CheckOptionHandler getOptionTesterLoader() {
    return getOptionTester(getLoader());
  }
  
  /**
   * Configures the CheckOptionHandler uses for testing the option handling
   * of the saver.
   * Sets the scheme to test.
   * 
   * @return	the fully configured CheckOptionHandler
   */
  protected CheckOptionHandler getOptionTesterSaver() {
    return getOptionTester(getSaver());
  }

  /**
   * returns the test data generator
   * 
   * @return 	the configured test data generator
   */
  protected TestInstances getTestInstances() {
    return new TestInstances();
  }
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Loader = getLoader();
    m_Saver  = getSaver();
    
    m_OptionTesterLoader = getOptionTesterLoader();
    m_OptionTesterSaver  = getOptionTesterSaver();
    
    m_TestInstances = getTestInstances();
    m_Instances     = m_TestInstances.generate();
  }
  
  /** 
   * Called by JUnit after each test method
   */
  protected void tearDown() throws Exception {
    m_Loader = null;
    m_Saver  = null;

    m_OptionTesterLoader = null;
    m_OptionTesterSaver  = null;
    
    m_TestInstances = null;
    m_Instances     = null;
    
    super.tearDown();
  }
  
  /**
   * tests the listing of the options
   */
  public void testListOptions() throws Exception {
    if (m_OptionTesterLoader.getOptionHandler() != null) {
      if (!m_OptionTesterLoader.checkListOptions())
	fail("Loader: Options cannot be listed via listOptions.");
    }

    if (m_OptionTesterSaver.getOptionHandler() != null) {
      if (!m_OptionTesterSaver.checkListOptions())
	fail("Saver: Options cannot be listed via listOptions.");
    }
  }
  
  /**
   * tests the setting of the options
   */
  public void testSetOptions() throws Exception {
    // TODO: currently disabled
    if (!TEST_OPTION_HANDLING)
      return;
    
    if (m_OptionTesterLoader.getOptionHandler() != null) {
      if (!m_OptionTesterLoader.checkSetOptions())
	fail("Loader: setOptions method failed.");
    }

    if (m_OptionTesterSaver.getOptionHandler() != null) {
      if (!m_OptionTesterSaver.checkSetOptions())
	fail("Saver: setOptions method failed.");
    }
  }
  
  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() throws Exception {
    // TODO: currently disabled
    if (!TEST_OPTION_HANDLING)
      return;
    
    if (m_OptionTesterLoader.getOptionHandler() != null) {
      if (!m_OptionTesterLoader.checkRemainingOptions())
	fail("Loader: There were 'left-over' options.");
    }

    if (m_OptionTesterSaver.getOptionHandler() != null) {
      if (!m_OptionTesterSaver.checkRemainingOptions())
	fail("Saver: There were 'left-over' options.");
    }
  }
  
  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   * 
   * @see 	#getOptionTester()
   */
  public void testCanonicalUserOptions() throws Exception {
    // TODO: currently disabled
    if (!TEST_OPTION_HANDLING)
      return;
    
    if (m_OptionTesterLoader.getOptionHandler() != null) {
      if (!m_OptionTesterLoader.checkCanonicalUserOptions())
	fail("Loader: setOptions method failed");
    }

    if (m_OptionTesterSaver.getOptionHandler() != null) {
      if (!m_OptionTesterSaver.checkCanonicalUserOptions())
	fail("Saver: setOptions method failed");
    }
  }
  
  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() throws Exception {
    // TODO: currently disabled
    if (!TEST_OPTION_HANDLING)
      return;
    
    if (m_OptionTesterLoader.getOptionHandler() != null) {
      if (!m_OptionTesterLoader.checkSetOptions())
	fail("Loader: Resetting of options failed");
    }

    if (m_OptionTesterSaver.getOptionHandler() != null) {
      if (!m_OptionTesterSaver.checkSetOptions())
	fail("Saver: Resetting of options failed");
    }
  }
}
