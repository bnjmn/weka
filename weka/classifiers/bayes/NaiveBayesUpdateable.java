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
 *    NaiveBayesUpdateable.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.bayes;

import weka.classifiers.UpdateableClassifier;
import weka.classifiers.Evaluation;

/**
 * Class for a Naive Bayes classifier using estimator classes. This is the
 * updateable version of NaiveBayes.
 * This classifier will use a default precision of 0.1 for numeric attributes
 * when buildClassifier is called with zero training instances.
 * <p>
 * For more information on Naive Bayes classifiers, see<p>
 *
 * George H. John and Pat Langley (1995). <i>Estimating
 * Continuous Distributions in Bayesian Classifiers</i>. Proceedings
 * of the Eleventh Conference on Uncertainty in Artificial
 * Intelligence. pp. 338-345. Morgan Kaufmann, San Mateo.<p>
 *
 * Valid options are:<p>
 *
 * -K <br>
 * Use kernel estimation for modelling numeric attributes rather than
 * a single normal distribution.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class NaiveBayesUpdateable extends NaiveBayes 
  implements UpdateableClassifier {
  
  static final long serialVersionUID = -5354015843807192221L;
 
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for a Naive Bayes classifier using estimator classes. This is the "
      +"updateable version of NaiveBayes."
      +"This classifier will use a default precision of 0.1 for numeric attributes "
      +"when buildClassifier is called with zero training instances.\n\n"
      +"For more information on Naive Bayes classifiers, see\n\n"
      +"George H. John and Pat Langley (1995). Estimating "
      +"Continuous Distributions in Bayesian Classifiers. Proceedings "
      +"of the Eleventh Conference on Uncertainty in Artificial "
      +"Intelligence. pp. 338-345. Morgan Kaufmann, San Mateo.\n\n";
  }

  /**
   * Set whether supervised discretization is to be used.
   *
   * @param newblah true if supervised discretization is to be used.
   */
  public void setUseSupervisedDiscretization(boolean newblah) {

    if (newblah) {
      throw new IllegalArgumentException("Can't use discretization " + 
					 "in NaiveBayesUpdateable!");
    }
    m_UseDiscretization = false;
  }
  

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new NaiveBayesUpdateable(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

}
