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
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PaceRegression. Run from the command line with:<p>
 * java weka.classifiers.functions.PaceRegressionTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 1.4 $
 */
public class PaceRegressionTest 
  extends AbstractClassifierTest {

  public PaceRegressionTest(String name) { 
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
    result.setNumInstances(60);
    
    return result;
  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return new PaceRegression();
  }

  public static Test suite() {
    return new TestSuite(PaceRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
