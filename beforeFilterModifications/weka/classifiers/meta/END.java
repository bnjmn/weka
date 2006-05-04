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
 *    Copyright (C) 2004-2005 University of Waikato
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
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

import java.util.Hashtable;
import java.util.Random;

/**
 <!-- globalinfo-start -->
 * A meta classifier for handling multi-class datasets with 2-class classifiers by building an ensemble of nested dichotomies.<br/>
 * <br/>
 * For more info, check<br/>
 * <br/>
 * Lin Dong, Eibe Frank, Stefan Kramer: Ensembles of Balanced Nested Dichotomies for Multi-class Problems. In: PKDD, 84-95, 2005.<br/>
 * <br/>
 * Eibe Frank, Stefan Kramer: Ensembles of nested dichotomies for multi-class problems. In: Twenty-first International Conference on Machine Learning, , 2004.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;incproceedings{Dong2005,
 *    author = {Lin Dong and Eibe Frank and Stefan Kramer},
 *    booktitle = {PKDD},
 *    pages = {84-95},
 *    publisher = {Springer},
 *    title = {Ensembles of Balanced Nested Dichotomies for Multi-class Problems},
 *    year = {2005}
 * }
 * 
 * &#64;incproceedings{Frank2004,
 *    author = {Eibe Frank and Stefan Kramer},
 *    booktitle = {Twenty-first International Conference on Machine Learning},
 *    publisher = {ACM},
 *    title = {Ensembles of nested dichotomies for multi-class problems},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.meta.nestedDichotomies.ND)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.meta.nestedDichotomies.ND:
 * </pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Eibe Frank
 * @author Lin Dong
 * @version $Revision: 1.4 $
 */
public class END 
  extends RandomizableIteratedSingleClassifierEnhancer
  implements TechnicalInformationHandler {
  
  /** for serialization */
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
   * 
   * @return the default classifier classname
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
      + "classifiers by building an ensemble of nested dichotomies.\n\n"
      + "For more info, check\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Lin Dong and Eibe Frank and Stefan Kramer");
    result.setValue(Field.TITLE, "Ensembles of Balanced Nested Dichotomies for Multi-class Problems");
    result.setValue(Field.BOOKTITLE, "PKDD");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.PAGES, "84-95");
    result.setValue(Field.PUBLISHER, "Springer");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "Eibe Frank and Stefan Kramer");
    additional.setValue(Field.TITLE, "Ensembles of nested dichotomies for multi-class problems");
    additional.setValue(Field.BOOKTITLE, "Twenty-first International Conference on Machine Learning");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.PUBLISHER, "ACM");
    
    return result;
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
   * @throws Exception if the classifier could not be built successfully
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
   * @throws Exception if distribution can't be computed successfully 
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
