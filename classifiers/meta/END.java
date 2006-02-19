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
 *    END.java
 *    Copyright (C) 200-2005 University of Waikato
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Randomizable;
import weka.core.Utils;

import java.util.Hashtable;
import java.util.Random;

/**
 * Class for creating an ensemble of nested dichotomies to tackle
 * multi-class problems with two-class classifiers. For more info, check
 *
 * Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of
 * Balanced Nested Dichotomies for Multi-Class Problems. PKDD,
 * Porto. Springer-Verlag.
 *
 * <p>and<p>
 * 
 * Eibe Frank and Stefan Kramer (2004). Ensembles of Nested
 * Dichotomies for Multi-class Problems. Proceedings of the
 * International Conference on Machine Learning, Banff. Morgan
 * Kaufmann.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a learner that generates
 * a system of nested dichotomies.<p>
 *
 * -I num <br>
 * Set the number of committee members (default 10). <p>
 *
 * -S seed <br>
 * Random number seed for the randomization process (default 1). <p>
 *
 * -H <br>
 * Use hashtable to store the classifiers (default true).<p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Eibe Frank
 * @author Lin Dong
 * @version $Revision: 1.3 $
 */
public class END extends RandomizableIteratedSingleClassifierEnhancer {
  
  static final long serialVersionUID = -4143242362912214956L;
  
  /**
   * The hashtable containing the classifiers for the END.
   */
  protected Hashtable m_hashtable = null;
  
  /**
   * Constructor.
   */
  public END() {
    
    m_Classifier = new weka.classifiers.meta.nestedDichotomies.ND();
  }
  
  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.meta.nestedDichotomies.ND";
  }
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    
    return "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building an ensemble of nested dichotomies. For more info, check\n\n"
      + "Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of "
      + "Balanced Nested Dichotomies for Multi-Class Problems. PKDD, Porto. Springer-Verlag\n\nand\n\n"
      + "Eibe Frank and Stefan Kramer (2004). Ensembles of Nested Dichotomies for Multi-class Problems. "
      + "Proceedings of the International Conference on Machine Learning, Banff. Morgan Kaufmann.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // instances
    result.setMinimumNumberInstances(1);  // at least 1 for the RandomNumberGenerator!
    
    return result;
  }
  
  /**
   * Builds the committee of randomizable classifiers.
   *
   * @param data the training data to be used for generating the
   * bagged classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    if (!(m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.ND) && 
	!(m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.ClassBalancedND) &&  
	!(m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.DataNearBalancedND)) {
      throw new IllegalArgumentException("END only works with ND, ClassBalancedND " +
					 "or DataNearBalancedND classifier");
    }
    
    m_hashtable = new Hashtable();
    
    m_Classifiers = Classifier.makeCopies(m_Classifier, m_NumIterations);
    
    Random random = data.getRandomNumberGenerator(m_Seed);
    for (int j = 0; j < m_Classifiers.length; j++) {
      
      // Set the random number seed for the current classifier.
      ((Randomizable) m_Classifiers[j]).setSeed(random.nextInt());
      
      // Set the hashtable
      if (m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.ND) 
	((weka.classifiers.meta.nestedDichotomies.ND)m_Classifiers[j]).setHashtable(m_hashtable);
      else if (m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.ClassBalancedND) 
	((weka.classifiers.meta.nestedDichotomies.ClassBalancedND)m_Classifiers[j]).setHashtable(m_hashtable);
      else if (m_Classifier instanceof weka.classifiers.meta.nestedDichotomies.DataNearBalancedND) 
	((weka.classifiers.meta.nestedDichotomies.DataNearBalancedND)m_Classifiers[j]).
	  setHashtable(m_hashtable);
      
      // Build the classifier.
      m_Classifiers[j].buildClassifier(data);
    }
  }
  
  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @exception Exception if distribution can't be computed successfully 
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double [] sums = new double [instance.numClasses()], newProbs; 
    
    for (int i = 0; i < m_NumIterations; i++) {
      if (instance.classAttribute().isNumeric() == true) {
	sums[0] += m_Classifiers[i].classifyInstance(instance);
      } else {
	newProbs = m_Classifiers[i].distributionForInstance(instance);
	for (int j = 0; j < newProbs.length; j++)
	  sums[j] += newProbs[j];
      }
    }
    if (instance.classAttribute().isNumeric() == true) {
      sums[0] /= (double)m_NumIterations;
      return sums;
    } else if (Utils.eq(Utils.sum(sums), 0)) {
      return sums;
    } else {
      Utils.normalize(sums);
      return sums;
    }
  }
  
  /**
   * Returns description of the committee.
   *
   * @return description of the committee as a string
   */
  public String toString() {
    
    if (m_Classifiers == null) {
      return "END: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("All the base classifiers: \n\n");
    for (int i = 0; i < m_Classifiers.length; i++)
      text.append(m_Classifiers[i].toString() + "\n\n");
    
    return text.toString();
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(Evaluation.
			 evaluateModel(new END(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
