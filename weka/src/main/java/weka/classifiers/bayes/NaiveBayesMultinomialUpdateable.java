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
 * NaiveBayesMultinomialUpdateable.java
 * Copyright (C) 2003-2017 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.bayes;

import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 <!-- globalinfo-start -->
 * Class for building and using an updateable multinomial Naive Bayes classifier. For more information see,<br/>
 * <br/>
 * Andrew Mccallum, Kamal Nigam: A Comparison of Event Models for Naive Bayes Text Classification. In: AAAI-98 Workshop on 'Learning for Text Categorization', 1998.<br/>
 * <br/>
 * The core equation for this classifier:<br/>
 * <br/>
 * P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)<br/>
 * <br/>
 * where Ci is class i and D is a document.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Mccallum1998,
 *    author = {Andrew Mccallum and Kamal Nigam},
 *    booktitle = {AAAI-98 Workshop on 'Learning for Text Categorization'},
 *    title = {A Comparison of Event Models for Naive Bayes Text Classification},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * -output-debug-info <br>
 * If set, classifier is run in debug mode and may output additional info to
 * the console.
 * <p>
 *
 * -do-not-check-capabilities <br>
 * If set, classifier capabilities are not checked before classifier is built
 * (use with caution).
 * <p>
 *
 * -num-decimal-laces <br>
 * The number of decimal places for the output of numbers in the model.
 * <p>
 *
 * -batch-size <br>
 * The desired batch size for batch prediction.
 * <p>
 *
 <!-- options-end -->
 *
 * @author Andrew Golightly (acg4@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$ 
 */
public class NaiveBayesMultinomialUpdateable extends NaiveBayesMultinomial implements UpdateableClassifier {
  
  /** for serialization */
  static final long serialVersionUID = -7204398796974263186L;

  /** the number of words per class. */
  protected double[] m_wordsPerClass;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Class for building and using an updateable multinomial Naive Bayes classifier. "
      + "For more information see,\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "The core equation for this classifier:\n\n"
      + "P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes' rule)\n\n"
      + "where Ci is class i and D is a document.";
  }


  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    initializeClassifier(instances);

    //enumerate through the instances 
    m_wordsPerClass = new double[m_numClasses];
    for (int i = 0; i < m_numClasses; i++) {
      m_wordsPerClass[i] = m_numAttributes - 1;
    }

    for (Instance instance : instances) {
      updateClassifier(instance);
    }
  }

  /**
   * Updates the classifier with information from one training instance.
   *
   * @param instance the instance to be incorporated
   * @throws Exception if the instance cannot be processed successfully.
   */
  public void updateClassifier(Instance instance) throws Exception {

    double classValue = instance.value(instance.classIndex());
    if (!Utils.isMissingValue(classValue)) {
      int classIndex = (int) classValue;
      m_probOfClass[classIndex] += instance.weight();
      for (int a = 0; a < instance.numValues(); a++) {
        if (instance.index(a) != instance.classIndex()) {
          if (!instance.isMissingSparse(a)) {
            double numOccurrences = instance.valueSparse(a) * instance.weight();
            if (numOccurrences < 0)
              throw new Exception("Numeric attribute values must all be greater or equal to zero.");
            m_wordsPerClass[classIndex] += numOccurrences;
            m_probOfWordGivenClass[classIndex][instance.index(a)] += numOccurrences;
          }
        }
      }
    }
  }
    
  /**
   * log(N!) + (sum for all the words i)(log(Pi^ni) - log(ni!))
   *  
   *  where 
   *      N is the total number of words
   *      Pi is the probability of obtaining word i
   *      ni is the number of times the word at index i occurs in the document
   *
   * Actually, this method just computes (sum for all the words i)(log(Pi^ni) because the factorials are irrelevant
   * when posterior class probabilities are computed.
   *
   * @param inst       The instance to be classified
   * @param classIndex The index of the class we are calculating the probability with respect to
   *
   * @return The log of the probability of the document occuring given the class
   */
    
  protected double probOfDocGivenClass(Instance inst, int classIndex) {

    double answer = 0;

    for(int i = 0; i < inst.numValues(); i++) {
      if (inst.index(i) != inst.classIndex()) {
        answer += inst.valueSparse(i) * (Math.log(m_probOfWordGivenClass[classIndex][inst.index(i)]) -
                Math.log(m_wordsPerClass[classIndex]));
      }
    }

    return answer;
  }
    
  /**
   * Returns a string representation of the classifier.
   * 
   * @return a string representation of the classifier
   */
  public String toString()
  {
    StringBuffer result = new StringBuffer("The class counts (including Laplace correction)\n-----------------------------------------------\n");
	
    for(int c = 0; c<m_numClasses; c++)
      result.append(m_headerInfo.classAttribute().value(c)).append("\t").
              append(Utils.doubleToString(m_probOfClass[c], getNumDecimalPlaces())).append("\n");
	
    result.append("\nThe probability of a word given the class\n-----------------------------------------\n\t");

    for(int c = 0; c<m_numClasses; c++)
      result.append(m_headerInfo.classAttribute().value(c)).append("\t");
	
    result.append("\n");

    for(int w = 0; w<m_numAttributes; w++)
    {
      if (w != m_headerInfo.classIndex()) {
        result.append(m_headerInfo.attribute(w).name()).append("\t");
        for(int c = 0; c<m_numClasses; c++)
          result.append(Utils.doubleToString(m_probOfWordGivenClass[c][w] / m_wordsPerClass[c], getNumDecimalPlaces())).append("\t");
        result.append("\n");
      }
    }

    return result.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new NaiveBayesMultinomialUpdateable(), argv);
  }
}

