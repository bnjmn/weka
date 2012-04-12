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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;
import weka.core.SelectedTag;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests GridSearch. Run from the command line with:<p/>
 * java weka.classifiers.meta.GridSearchTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class GridSearchTest 
  extends AbstractClassifierTest {

  public GridSearchTest(String name) { 
    super(name);  
  }

  /**
   * Creates a GridSearch with J48 + unsup. Discretize filter sinze the random
   * test dataset always results in singular matrix in LU decomposition of
   * the PLSFilter.
   * 
   * @return		the configured classifier
   */
  public Classifier getClassifier() {
    GridSearch		result;
    
    result = new GridSearch();
    
    result.setEvaluation(new SelectedTag(GridSearch.EVALUATION_ACC, GridSearch.TAGS_EVALUATION));
    
    // classifier
    result.setClassifier(new weka.classifiers.trees.J48());
    result.setYProperty("classifier.confidenceFactor");
    result.setYMin(0.2);
    result.setYMax(0.4);
    result.setYStep(0.1);
    result.setYExpression("I");
    
    // filter
    result.setFilter(new weka.filters.unsupervised.attribute.Discretize());
    result.setXProperty("filter.bins");
    result.setXMin(2);
    result.setXMax(10);
    result.setXStep(2);
    result.setXExpression("I");
    
    return result;
  }
  
  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return	the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier	result;
    
    result = super.getTester();
    result.setNumNumeric(7);
    result.setNumInstances(100);
    
    return result;
  }

  public static Test suite() {
    return new TestSuite(GridSearchTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
