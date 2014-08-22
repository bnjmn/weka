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
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FLDA. Run from the command line with:<p/>
 * java weka.classifiers.functions.FLDATest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8109 $
 */
public class FLDATest 
  extends AbstractClassifierTest {

  public FLDATest(String name) { 
    super(name);  
  }

  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return	the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier 	result;
    
    result = super.getTester();
    result.setNumInstances(40);
    
    return result;
  }

  /** Creates a default FLDA */
  public Classifier getClassifier() {
    return new FLDA();
  }

  public static Test suite() {
    return new TestSuite(FLDATest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
