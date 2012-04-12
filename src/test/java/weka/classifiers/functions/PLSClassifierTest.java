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

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;
import weka.core.SelectedTag;
import weka.filters.supervised.attribute.PLSFilter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PLSClassifier. Run from the command line with:<p/>
 * java weka.classifiers.functions.PLSClassifierTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class PLSClassifierTest 
  extends AbstractClassifierTest {

  /** the number of PLS components to generate */
  public final static int NUM_COMPONENTS = 5;
  
  public PLSClassifierTest(String name) { 
    super(name);  
  }
  
  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return	the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier	result;
    
    result = super.getTester();
    result.setNumNominal(NUM_COMPONENTS * 2);
    result.setNumNumeric(NUM_COMPONENTS * 2);
    result.setNumString(NUM_COMPONENTS * 2);
    result.setNumDate(NUM_COMPONENTS * 2);
    result.setNumRelational(NUM_COMPONENTS * 2);
    
    return result;
  }

  /** Creates a default PLSClassifier */
  public Classifier getClassifier() {
    PLSClassifier classifier = new PLSClassifier();
    
    PLSFilter filter = new PLSFilter();
    filter.setReplaceMissing(true);
    filter.setPreprocessing(new SelectedTag(PLSFilter.PREPROCESSING_CENTER, PLSFilter.TAGS_PREPROCESSING));
    filter.setNumComponents(NUM_COMPONENTS);
    filter.setAlgorithm(new SelectedTag(PLSFilter.ALGORITHM_SIMPLS, PLSFilter.TAGS_ALGORITHM));
    try {
      classifier.setFilter(filter);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return classifier;
  }

  public static Test suite() {
    return new TestSuite(PLSClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
