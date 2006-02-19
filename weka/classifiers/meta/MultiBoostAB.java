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
 *    MultiBoostAB.java
 *
 *    MultiBoosting is an extension to the highly successful AdaBoost
 *    technique for forming decision committees. MultiBoosting can be
 *    viewed as combining AdaBoost with wagging. It is able to harness
 *    both AdaBoost's high bias and variance reduction with wagging's
 *    superior variance reduction. Using C4.5 as the base learning
 *    algorithm, Multi-boosting is demonstrated to produce decision
 *    committees with lower error than either AdaBoost or wagging
 *    significantly more often than the reverse over a large
 *    representative cross-section of UCI data sets. It offers the
 *    further advantage over AdaBoost of suiting parallel execution.
 *    
 *    For more info refer to : G. Webb, (2000). <i>MultiBoosting: A
 *    Technique for Combining Boosting and Wagging.</i> Machine
 *    Learning, 40(2): 159-196.  
 *
 *    Originally based on AdaBoostM1.java
 *    
 *    http://www.cm.deakin.edu.au/webb
 *
 *    School of Computing and Mathematics
 *    Deakin University
 *    Geelong, Vic, 3217, Australia
 *    Copyright (C) 2001 Deakin University
 * 
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Class for boosting a classifier using the MultiBoosting method.<BR>
 * MultiBoosting is an extension to the highly successful AdaBoost
 * technique for forming decision committees. MultiBoosting can be
 * viewed as combining AdaBoost with wagging. It is able to harness
 * both AdaBoost's high bias and variance reduction with wagging's
 * superior variance reduction. Using C4.5 as the base learning
 * algorithm, Multi-boosting is demonstrated to produce decision
 * committees with lower error than either AdaBoost or wagging
 * significantly more often than the reverse over a large
 * representative cross-section of UCI data sets. It offers the
 * further advantage over AdaBoost of suiting parallel execution.<BR>
 * For more information, see<p>
 *
 * Geoffrey I. Webb (2000). <i>MultiBoosting: A Technique for
 * Combining Boosting and Wagging</i>.  Machine Learning, 40(2):
 * 159-196, Kluwer Academic Publishers, Boston<BR><BR>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier as the basis for
 * boosting (required).<p>
 *
 * -I num <br>
 * Set the number of boost iterations (default 10). <p>
 *
 * -P num <br>
 * Set the percentage of weight mass used to build classifiers
 * (default 100). <p>
 *
 * -Q <br>
 * Use resampling instead of reweighting.<p>
 *
 * -S seed <br>
 * Random number seed for resampling (default 1). <p>
 *
 * -C subcommittees <br>
 * Number of sub-committees. (Default 3), <p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Shane Butler (sbutle@deakin.edu.au)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $ 
`*/
public class MultiBoostAB extends AdaBoostM1 {

  static final long serialVersionUID = -6681619178187935148L;
  
  /** The number of sub-committees to use */
  protected int m_NumSubCmtys = 3;

  /** Random number generator */
  protected Random m_Random = null;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Class for boosting a classifier using the MultiBoosting method.\n\n"
      + "MultiBoosting is an extension to the highly successful AdaBoost "
      + "technique for forming decision committees. MultiBoosting can be "
      + "viewed as combining AdaBoost with wagging. It is able to harness "
      + "both AdaBoost's high bias and variance reduction with wagging's "
      + "superior variance reduction. Using C4.5 as the base learning "
      + "algorithm, Multi-boosting is demonstrated to produce decision "
      + "committees with lower error than either AdaBoost or wagging "
      + "significantly more often than the reverse over a large "
      + "representative cross-section of UCI data sets. It offers the "
      + "further advantage over AdaBoost of suiting parallel execution. "
      + "For more information, see\n\n"
      + "Geoffrey I. Webb (2000). \"MultiBoosting: A Technique for "
      + "Combining Boosting and Wagging\".  Machine Learning, 40(2): "
      + "159-196, Kluwer Academic Publishers, Boston";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Enumeration enu = super.listOptions();
    Vector vec = new Vector(1);

    vec.addElement(new Option(
	      "\tNumber of sub-committees. (Default 10)",
	      "C", 1, "-C <num>"));
    while (enu.hasMoreElements()) {
      vec.addElement(enu.nextElement());
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of a classifier as the basis for
   * boosting (required).<p>
   *
   * -I num <br>
   * Set the number of boost iterations (default 10). <p>
   *
   * -P num <br>
   * Set the percentage of weight mass used to build classifiers
   * (default 100). <p>
   *
   * -Q <br>
   * Use resampling instead of reweighting.<p>
   *
   * -S seed <br>
   * Random number seed for resampling (default 1).<p>
   *
   * -C subcommittees <br>
   * Number of sub-committees. (Default 3), <p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String subcmtyString = Utils.getOption('C', options);
    if (subcmtyString.length() != 0) {
      setNumSubCmtys(Integer.parseInt(subcmtyString));
    } else {
      setNumSubCmtys(3);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] ops = super.getOptions();
    String [] options = new String[ops.length + 2];
    options[0] = "-C"; options[1] = "" + getNumSubCmtys();
    System.arraycopy(ops, 0, options, 2, ops.length);
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numSubCmtysTipText() {
    return "Sets the (approximate) number of subcommittees.";
  }


  /**
   * Set the number of sub committees to use
   *
   * @param seed the seed for resampling
   */
  public void setNumSubCmtys(int subc) {

    m_NumSubCmtys = subc;
  }

  /**
   * Get the number of sub committees to use
   *
   * @return the seed for resampling
   */
  public int getNumSubCmtys() {

    return m_NumSubCmtys;
  }

  /**
   * Method for building this classifier.
   */
  public void buildClassifier(Instances training) throws Exception {

    m_Random = new Random(m_Seed);

    super.buildClassifier(training);

    m_Random = null;
  }

  /**
   * Sets the weights for the next iteration.
   */
  protected void setWeights(Instances training, double reweight) 
    throws Exception {

    int subCmtySize = m_Classifiers.length / m_NumSubCmtys;

    if ((m_NumIterationsPerformed + 1) % subCmtySize == 0) {

      System.err.println(m_NumIterationsPerformed + " " + subCmtySize);

      double oldSumOfWeights = training.sumOfWeights();

      // Randomly set the weights of the training instances to the poisson distributon
      for (int i = 0; i < training.numInstances(); i++) {
	training.instance(i).setWeight( - Math.log((m_Random.nextDouble() * 9999) / 10000) );
      }

      // Renormailise weights
      double sumProbs = training.sumOfWeights();
      for (int i = 0; i < training.numInstances(); i++) {
	training.instance(i).setWeight(training.instance(i).weight() * oldSumOfWeights / sumProbs);
      }
    } else {
      super.setWeights(training, reweight);
    }
  }
  
  /**
   * Returns description of the boosted classifier.
   *
   * @return description of the boosted classifier as a string
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();
    
    if (m_NumIterations == 0) {
      text.append("MultiBoostAB: No model built yet.\n");
    } else if (m_NumIterations == 1) {
      text.append("MultiBoostAB: No boosting possible, one classifier used!\n");
      text.append(m_Classifiers[0].toString() + "\n");
    } else {
      text.append("MultiBoostAB: Base classifiers and their weights: \n\n");
      for (int i = 0; i < m_NumIterations ; i++) {
        if ( (m_Classifiers != null) && (m_Classifiers[i] != null) ) {
          text.append(m_Classifiers[i].toString() + "\n\n");
          text.append("Weight: " + Utils.roundDouble(m_Betas[i], 2) + "\n\n");
        }
        else {
          text.append("not yet initialized!\n\n");
        }
      }
      text.append("Number of performed Iterations: " + m_NumIterations + "\n");
    }
    
    return text.toString();
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new MultiBoostAB(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
