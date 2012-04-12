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

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.CheckClassifier.PostProcessor;
import weka.core.Instances;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Decorate. Run from the command line with:<p/>
 * java weka.classifiers.meta.Decorate
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.3 $
 */
public class DecorateTest 
  extends AbstractClassifierTest {
  
  /** 
   * a class for postprocessing the test-data: all nominal attributes with less 
   * than 2 labels are removed from the data.
   * 
   * @author  FracPete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 1.1.2.3 $
   */
  public static class SingularPostProcessor 
    extends PostProcessor {
    
    /**
     * initializes the PostProcessor
     */
    public SingularPostProcessor() {
      super();
    }
    
    /**
     * Provides a hook for derived classes to further modify the data. Deletes
     * all nominal attributes with less than 2 labels.
     * 
     * @param data	the data to process
     * @return		the processed data
     */
    public Instances process(Instances data) {
      Instances	result;
      int		i;
      
      result = new Instances(super.process(data));
      
      i = 0;
      while (i < result.numAttributes()) {
        if (result.attribute(i).isNominal() && (result.attribute(i).numValues() < 2))
  	result.deleteAttributeAt(i);
        else
  	i++;
      }
      
      return result;
    }
  }

  public DecorateTest(String name) { 
    super(name);  
  }

  /** Creates a default Decorate */
  public Classifier getClassifier() {
    return new Decorate();
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
    result.setPostProcessor(new SingularPostProcessor());
    
    return result;
  }

  public static Test suite() {
    return new TestSuite(DecorateTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
