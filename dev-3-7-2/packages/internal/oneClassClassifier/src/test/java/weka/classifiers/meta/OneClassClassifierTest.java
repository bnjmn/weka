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
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests OneClassClassifier. Run from the command line with:<p/>
 * java weka.classifiers.meta.OneClassClassifierTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OneClassClassifierTest 
  extends AbstractClassifierTest {

  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public OneClassClassifierTest(String name) { 
    super(name);  
  }
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Tester.setNumInstances(40);
  }

  /**
   * Creates a default OneClassClassifier.
   * 
   * @return		the default classifier
   */
  public Classifier getClassifier() {
    OneClassClassifier	result;
    
    result = new OneClassClassifier();
    result.setTargetClassLabel("class1");
    
    return result;
  }

  /**
   * Returns a test suite.
   * 
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(OneClassClassifierTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
