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
 *    For more info refer to :
 <!-- technical-plaintext-start -->
 * Geoffrey I. Webb (2000). MultiBoosting: A Technique for Combining Boosting and Wagging. Machine Learning. Vol.40(No.2).
 <!-- technical-plaintext-end -->
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Class for boosting a classifier using the
 * MultiBoosting method.<br/>
 * <br/>
 * MultiBoosting is an extension to the highly successful AdaBoost technique for
 * forming decision committees. MultiBoosting can be viewed as combining
 * AdaBoost with wagging. It is able to harness both AdaBoost's high bias and
 * variance reduction with wagging's superior variance reduction. Using C4.5 as
 * the base learning algorithm, Multi-boosting is demonstrated to produce
 * decision committees with lower error than either AdaBoost or wagging
 * significantly more often than the reverse over a large representative
 * cross-section of UCI data sets. It offers the further advantage over AdaBoost
 * of suiting parallel execution.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * Geoffrey I. Webb (2000). MultiBoosting: A Technique for Combining Boosting
 * and Wagging. Machine Learning. Vol.40(No.2).
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Webb2000,
 *    address = {Boston},
 *    author = {Geoffrey I. Webb},
 *    journal = {Machine Learning},
 *    number = {No.2},
 *    publisher = {Kluwer Academic Publishers},
 *    title = {MultiBoosting: A Technique for Combining Boosting and Wagging},
 *    volume = {Vol.40},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -C &lt;num&gt;
 *  Number of sub-committees. (Default 3)
 * </pre>
 * 
 * <pre>
 * -P &lt;num&gt;
 *  Percentage of weight mass to base training on.
 *  (default 100, reduce to around 90 speed up)
 * </pre>
 * 
 * <pre>
 * -Q
 *  Use resampling for boosting.
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * Options after -- are passed to the designated classifier.
 * <p>
 * 
 * @author Shane Butler (sbutle@deakin.edu.au)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MultiBoostAB extends AdaBoostM1 implements
  TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -6681619178187935148L;

  /** The number of sub-committees to use */
  protected int m_NumSubCmtys = 3;

  /** Random number generator */
  protected Random m_Random = null;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  @Override
  public String globalInfo() {

    return "Class for boosting a classifier using the MultiBoosting method.\n\n"
      + "MultiBoosting is an extension to the highly successful AdaBoost "
      + "technique for forming decision committees. MultiBoosting can be "
      + "viewed as combining AdaBoost with wagging. It is able to harness "
      + "both AdaBoost's high bias and variance reduction with wagging's "
      + "superior variance reduction. Using C4.5 as the base learning "
      + "algorithm, Multi-boosting is demonstrated to produce decision "
      + "committees with lower error than either AdaBoost or wagging "
      + "significantly more often than the reverse over a large "
      + "representative cross-section of UCI data sets. It offers the "
      + "further advantage over AdaBoost of suiting parallel execution.\n\n"
      + "For more information, see\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Geoffrey I. Webb");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.TITLE,
      "MultiBoosting: A Technique for Combining Boosting and Wagging");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "Vol.40");
    result.setValue(Field.NUMBER, "No.2");
    result.setValue(Field.PUBLISHER, "Kluwer Academic Publishers");
    result.setValue(Field.ADDRESS, "Boston");

    return result;
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> vec = new Vector<Option>(1);

    vec.addElement(new Option("\tNumber of sub-committees. (Default 3)", "C",
      1, "-C <num>"));

    vec.addAll(Collections.list(super.listOptions()));

    return vec.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -C &lt;num&gt;
   *  Number of sub-committees. (Default 3)
   * </pre>
   * 
   * <pre>
   * -P &lt;num&gt;
   *  Percentage of weight mass to base training on.
   *  (default 100, reduce to around 90 speed up)
   * </pre>
   * 
   * <pre>
   * -Q
   *  Use resampling for boosting.
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)
   * </pre>
   * 
   * <pre>
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * Options after -- are passed to the designated classifier.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String subcmtyString = Utils.getOption('C', options);
    if (subcmtyString.length() != 0) {
      setNumSubCmtys(Integer.parseInt(subcmtyString));
    } else {
      setNumSubCmtys(3);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-C");
    options.add("" + getNumSubCmtys());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numSubCmtysTipText() {
    return "Sets the (approximate) number of subcommittees.";
  }

  /**
   * Set the number of sub committees to use
   * 
   * @param subc the number of sub committees
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
   * 
   * @param training the data to train with
   * @throws Exception if the training fails
   */
  @Override
  public void buildClassifier(Instances training) throws Exception {

    m_Random = new Random(m_Seed);

    super.buildClassifier(training);

    m_Random = null;
  }

  /**
   * Sets the weights for the next iteration.
   * 
   * @param training the data to train with
   * @param reweight the reweighting factor
   * @throws Exception in case of an error
   */
  @Override
  protected void setWeights(Instances training, double reweight)
    throws Exception {

    int subCmtySize = m_Classifiers.length / m_NumSubCmtys;

    if ((m_NumIterationsPerformed + 1) % subCmtySize == 0) {

      if (getDebug()) {
        System.err.println(m_NumIterationsPerformed + " " + subCmtySize);
      }

      double oldSumOfWeights = training.sumOfWeights();

      // Randomly set the weights of the training instances to the poisson
      // distributon
      for (int i = 0; i < training.numInstances(); i++) {
        training.instance(i).setWeight(
          -Math.log((m_Random.nextDouble() * 9999) / 10000));
      }

      // Renormailise weights
      double sumProbs = training.sumOfWeights();
      for (int i = 0; i < training.numInstances(); i++) {
        training.instance(i).setWeight(
          training.instance(i).weight() * oldSumOfWeights / sumProbs);
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
  @Override
  public String toString() {

    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "")
        .replaceAll(".", "=")
        + "\n\n");
      buf
        .append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }

    StringBuffer text = new StringBuffer();

    if (m_NumIterations == 0) {
      text.append("MultiBoostAB: No model built yet.\n");
    } else if (m_NumIterations == 1) {
      text.append("MultiBoostAB: No boosting possible, one classifier used!\n");
      text.append(m_Classifiers[0].toString() + "\n");
    } else {
      text.append("MultiBoostAB: Base classifiers and their weights: \n\n");
      for (int i = 0; i < m_NumIterations; i++) {
        if ((m_Classifiers != null) && (m_Classifiers[i] != null)) {
          text.append(m_Classifiers[i].toString() + "\n\n");
          text.append("Weight: " + Utils.roundDouble(m_Betas[i], 2) + "\n\n");
        } else {
          text.append("not yet initialized!\n\n");
        }
      }
      text.append("Number of performed Iterations: " + m_NumIterations + "\n");
    }

    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new MultiBoostAB(), argv);
  }
}
