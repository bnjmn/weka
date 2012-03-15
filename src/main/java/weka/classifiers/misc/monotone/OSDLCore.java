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
 *    OSDLCore.java
 *    Copyright (C) 2004 Stijn Lievens
 */

package weka.classifiers.misc.monotone;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.estimators.DiscreteEstimator;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * This class is an implementation of the Ordinal Stochastic Dominance Learner.<br/>
 * Further information regarding the OSDL-algorithm can be found in:<br/>
 * <br/>
 * S. Lievens, B. De Baets, K. Cao-Van (2006). A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting. Annals of Operations Research..<br/>
 * <br/>
 * Kim Cao-Van (2003). Supervised ranking: from semantics to algorithms.<br/>
 * <br/>
 * Stijn Lievens (2004). Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken.<br/>
 * <br/>
 * For more information about supervised ranking, see<br/>
 * <br/>
 * http://users.ugent.be/~slievens/supervised_ranking.php
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Lievens2006,
 *    author = {S. Lievens and B. De Baets and K. Cao-Van},
 *    journal = {Annals of Operations Research},
 *    title = {A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting},
 *    year = {2006}
 * }
 * 
 * &#64;phdthesis{Cao-Van2003,
 *    author = {Kim Cao-Van},
 *    school = {Ghent University},
 *    title = {Supervised ranking: from semantics to algorithms},
 *    year = {2003}
 * }
 * 
 * &#64;mastersthesis{Lievens2004,
 *    author = {Stijn Lievens},
 *    school = {Ghent University},
 *    title = {Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -C &lt;REG|WSUM|MAX|MED|RMED&gt;
 *  Sets the classification type to be used.
 *  (Default: MED)</pre>
 * 
 * <pre> -B
 *  Use the balanced version of the Ordinal Stochastic Dominance Learner</pre>
 * 
 * <pre> -W
 *  Use the weighted version of the Ordinal Stochastic Dominance Learner</pre>
 * 
 * <pre> -S &lt;value of interpolation parameter&gt;
 *  Sets the value of the interpolation parameter (not with -W/T/P/L/U)
 *  (default: 0.5).</pre>
 * 
 * <pre> -T
 *  Tune the interpolation parameter (not with -W/S)
 *  (default: off)</pre>
 * 
 * <pre> -L &lt;Lower bound for interpolation parameter&gt;
 *  Lower bound for the interpolation parameter (not with -W/S)
 *  (default: 0)</pre>
 * 
 * <pre> -U &lt;Upper bound for interpolation parameter&gt;
 *  Upper bound for the interpolation parameter (not with -W/S)
 *  (default: 1)</pre>
 * 
 * <pre> -P &lt;Number of parts&gt;
 *  Determines the step size for tuning the interpolation
 *  parameter, nl. (U-L)/P (not with -W/S)
 *  (default: 10)</pre>
 * 
 <!-- options-end -->
 *
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public abstract class OSDLCore
  extends AbstractClassifier 
  implements TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -9209888846680062897L;

  /**
   * Constant indicating that the classification type is 
   * regression (probabilistic weighted sum).
   */
  public static final int CT_REGRESSION = 0;

  /**
   * Constant indicating that the classification type is  
   * the probabilistic weighted sum.
   */
  public static final int CT_WEIGHTED_SUM = 1;

  /**
   * Constant indicating that the classification type is  
   * the mode of the distribution.
   */
  public static final int CT_MAXPROB = 2;

  /** 
   * Constant indicating that the classification type is  
   * the median.
   */
  public static final int CT_MEDIAN = 3;

  /** 
   *  Constant indicating that the classification type is
   *  the median, but not rounded to the nearest class.
   */
  public static final int CT_MEDIAN_REAL = 4;

  /** the classification types */
  public static final Tag[] TAGS_CLASSIFICATIONTYPES = {
    new Tag(CT_REGRESSION, "REG", "Regression"),
    new Tag(CT_WEIGHTED_SUM, "WSUM", "Weighted Sum"),
    new Tag(CT_MAXPROB, "MAX", "Maximum probability"),
    new Tag(CT_MEDIAN, "MED", "Median"),
    new Tag(CT_MEDIAN_REAL, "RMED", "Median without rounding")
  };

  /**
   * The classification type, by default set to CT_MEDIAN.
   */
  private int m_ctype = CT_MEDIAN;

  /** 
   * The training examples.
   */
  private Instances m_train;

  /** 
   * Collection of (Coordinates,DiscreteEstimator) pairs.
   * This Map is build from the training examples.
   * The DiscreteEstimator is over the classes.
   * Each DiscreteEstimator indicates how many training examples
   * there are with the specified classes.
   */
  private Map m_estimatedDistributions;


  /** 
   * Collection of (Coordinates,CumulativeDiscreteDistribution) pairs.
   * This Map is build from the training examples, and more 
   * specifically from the previous map.  
   */
  private Map m_estimatedCumulativeDistributions;


  /** 
   * The interpolationparameter s.  
   * By default set to 1/2.
   */
  private double m_s = 0.5;

  /** 
   * Lower bound for the interpolationparameter s.
   * Default value is 0.
   */
  private double m_sLower = 0.;

  /** 
   * Upper bound for the interpolationparameter s.
   * Default value is 1.
   */
  private double m_sUpper = 1.0;

  /** 
   * The number of parts the interval [m_sLower,m_sUpper] is 
   * divided in, while searching for the best parameter s.
   * This thus determines the granularity of the search.
   * m_sNrParts + 1 values of the interpolationparameter will
   * be tested.
   */
  private int m_sNrParts = 10;

  /** 
   * Indicates whether the interpolationparameter is to be tuned 
   * using leave-one-out cross validation.  <code> true </code> if
   * this is the case (default is <code> false </code>).
   */
  private boolean m_tuneInterpolationParameter = false;

  /**
   * Indicates whether the current value of the interpolationparamter
   * is valid.  More specifically if <code> 
   * m_tuneInterpolationParameter == true </code>, and 
   * <code> m_InterpolationParameter == false </code>, 
   * this means that the current interpolation parameter is not valid.
   * This parameter is only relevant if <code> m_tuneInterpolationParameter
   * == true </code>.
   *
   * If <code> m_tuneInterpolationParameter </code> and <code>
   * m_interpolationParameterValid </code> are both <code> true </code>,
   * then <code> m_s </code> should always be between 
   * <code> m_sLower </code> and <code> m_sUpper </code>. 
   */
  private boolean m_interpolationParameterValid = false;


  /** 
   * Constant to switch between balanced and unbalanced OSDL.
   * <code> true </code> means that one chooses balanced OSDL
   * (default: <code> false </code>).
   */
  private boolean m_balanced = false;

  /** 
   * Constant to choose the weighted variant of the OSDL algorithm.
   */
  private boolean m_weighted = false;

  /**
   * Coordinates representing the smallest element of the data space.
   */
  private Coordinates smallestElement;

  /**
   * Coordinates representing the biggest element of the data space.
   */
  private Coordinates biggestElement;

  /**
   * Returns a string describing the classifier.
   * @return a description suitable for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "This class is an implementation of the Ordinal Stochastic "
    + "Dominance Learner.\n" 
    + "Further information regarding the OSDL-algorithm can be found in:\n\n"
    + getTechnicalInformation().toString() + "\n\n"
    + "For more information about supervised ranking, see\n\n"
    + "http://users.ugent.be/~slievens/supervised_ranking.php";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "S. Lievens and B. De Baets and K. Cao-Van");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE, "A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting");
    result.setValue(Field.JOURNAL, "Annals of Operations Research");

    additional = result.add(Type.PHDTHESIS);
    additional.setValue(Field.AUTHOR, "Kim Cao-Van");
    additional.setValue(Field.YEAR, "2003");
    additional.setValue(Field.TITLE, "Supervised ranking: from semantics to algorithms");
    additional.setValue(Field.SCHOOL, "Ghent University");

    additional = result.add(Type.MASTERSTHESIS);
    additional.setValue(Field.AUTHOR, "Stijn Lievens");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.TITLE, "Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken");
    additional.setValue(Field.SCHOOL, "Ghent University");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Classifies a given instance using the current settings 
   * of the classifier.
   *
   * @param instance the instance to be classified
   * @throws Exception if for some reason no distribution
   *         could be predicted
   * @return the classification for the instance.  Depending on the
   * settings of the classifier this is a double representing 
   * a classlabel (internal WEKA format) or a real value in the sense
   * of regression.
   */
  public double classifyInstance(Instance instance)
    throws Exception { 
    
    try {
      return classifyInstance(instance, m_s, m_ctype);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    }
  }

  /** 
   * Classifies a given instance using the settings in the paramater
   * list.  This doesn't change the internal settings of the classifier.
   * In particular the interpolationparameter <code> m_s </code>
   * and the classification type <code> m_ctype </code> are not changed.
   *
   * @param instance the instance to be classified
   * @param s the value of the interpolationparameter to be used
   * @param ctype the classification type to be used  
   * @throws IllegalStateException for some reason no distribution
   *         could be predicted
   * @throws IllegalArgumentException if the interpolation parameter or the
   *         classification type is not valid 
   * @return the label assigned to the instance.  It is given in internal floating point format.
   */
  private double classifyInstance(Instance instance, double s, int ctype) 
    throws IllegalArgumentException, IllegalStateException {
    
    if (s < 0 || s > 1) {
      throw new IllegalArgumentException("Interpolation parameter is not valid " + s);
    }

    DiscreteDistribution dist = null;
    if (!m_balanced) {
      dist = distributionForInstance(instance, s);
    } else {
      dist = distributionForInstanceBalanced(instance, s);
    }

    if (dist == null) {
      throw new IllegalStateException("Null distribution predicted");
    }

    double value = 0;
    switch(ctype) {
      case CT_REGRESSION:
      case CT_WEIGHTED_SUM:
	value = dist.mean();
	if (ctype == CT_WEIGHTED_SUM) {
	  value = Math.round(value);
	}
	break;

      case CT_MAXPROB:
	value = dist.modes()[0];
	break;

      case CT_MEDIAN:
      case CT_MEDIAN_REAL:
	value = dist.median();
	if (ctype == CT_MEDIAN) {
	  value = Math.round(value);
	}
	break;

      default:
	throw new IllegalArgumentException("Not a valid classification type!"); 
    }
    return value;
  }

  /**
   * Calculates the class probabilities for the given test instance.
   * Uses the current settings of the parameters if these are valid.
   * If necessary it updates the interpolationparameter first, and hence 
   * this may change the classifier.
   *
   * @param instance the instance to be classified
   * @return an array of doubles representing the predicted 
   * probability distribution over the class labels
   */
  public double[] distributionForInstance(Instance instance) {

    if (m_tuneInterpolationParameter 
	&& !m_interpolationParameterValid) {
      tuneInterpolationParameter();
    }

    if (!m_balanced) {
      return distributionForInstance(instance, m_s).toArray();
    } 
    // balanced variant
    return distributionForInstanceBalanced(instance, m_s).toArray();
  }

  /**
   * Calculates the cumulative class probabilities for the given test 
   * instance. Uses the current settings of the parameters if these are 
   * valid. If necessary it updates the interpolationparameter first, 
   * and hence this may change the classifier.
   *
   * @param instance the instance to be classified
   * @return an array of doubles representing the predicted 
   * cumulative probability distribution over the class labels
   */
  public double[] cumulativeDistributionForInstance(Instance instance) {

    if (m_tuneInterpolationParameter 
	&& !m_interpolationParameterValid) {
      tuneInterpolationParameter();
    }

    if (!m_balanced) {
      return cumulativeDistributionForInstance(instance, m_s).toArray();
    } 
    return cumulativeDistributionForInstanceBalanced(instance, m_s).toArray();
  }

  /**
   * Calculates the class probabilities for the given test instance.
   * Uses the interpolation parameter from the parameterlist, and
   * always performs the ordinary or weighted OSDL algorithm,
   * according to the current settings of the classifier.
   * This method doesn't change the classifier.  
   *
   * @param instance the instance to classify
   * @param s value of the interpolationparameter to use
   * @return the calculated distribution
   */
  private DiscreteDistribution distributionForInstance(Instance instance, double s) {
    return new DiscreteDistribution(cumulativeDistributionForInstance(instance, s));
  }

  /**
   * Calculates the class probabilities for the given test 
   * instance. Uses the interpolationparameter from the parameterlist, and
   * always performs the balanced OSDL algorithm.
   * This method doesn't change the classifier.  
   *
   * @param instance the instance to classify
   * @param s value of the interpolationparameter to use
   * @return the calculated distribution
   */
  private DiscreteDistribution distributionForInstanceBalanced(
      Instance instance, double s) {
    
    return new DiscreteDistribution(cumulativeDistributionForInstanceBalanced(instance,s));
  }

  /**
   * Calculates the cumulative class probabilities for the given test 
   * instance. Uses the interpolationparameter from the parameterlist, and
   * always performs the ordinary or weighted OSDL algorithm,
   * according to the current settings of the classifier.
   * This method doesn't change the classifier.  
   *
   * @param instance the instance to classify
   * @param s value of the interpolationparameter to use
   * @return the calculated distribution
   */
  private CumulativeDiscreteDistribution cumulativeDistributionForInstance(
      Instance instance, double s) {
    
    Coordinates xc = new Coordinates(instance);
    int n = instance.numClasses();
    int nrSmaller = 0; 
    int nrGreater = 0;

    if (!containsSmallestElement()) {
      // corresponds to adding the minimal element to the data space
      nrSmaller = 1; // avoid division by zero
    }

    if (!containsBiggestElement()) {
      // corresponds to adding the maximal element to the data space
      nrGreater = 1; // avoid division by zero	
    }


    // Create fMin and fMax 
    CumulativeDiscreteDistribution fMin =
      DistributionUtils.getMinimalCumulativeDiscreteDistribution(n);
    CumulativeDiscreteDistribution fMax =
      DistributionUtils.getMaximalCumulativeDiscreteDistribution(n);

    // Cycle through all the map of cumulative distribution functions
    for (Iterator i = m_estimatedCumulativeDistributions.keySet().iterator();
    i.hasNext(); ) {
      Coordinates yc = (Coordinates) i.next();
      CumulativeDiscreteDistribution cdf = 
	(CumulativeDiscreteDistribution) 
	m_estimatedCumulativeDistributions.get(yc);

      if (yc.equals(xc)) {
	nrSmaller++;
	fMin = DistributionUtils.takeMin(fMin,cdf);
	nrGreater++;
	fMax = DistributionUtils.takeMax(fMax,cdf);
      } else if (yc.strictlySmaller(xc)) {
	nrSmaller++;
	fMin = DistributionUtils.takeMin(fMin,cdf);
      } else if (xc.strictlySmaller(yc)) {
	nrGreater++;
	fMax = DistributionUtils.takeMax(fMax,cdf);
      }
    }

    if (m_weighted) {
      s = ( (double) nrSmaller) / (nrSmaller + nrGreater);
      if (m_Debug) {
	System.err.println("Weighted OSDL: interpolation parameter"
	    + " is s = " + s);
      }
    }

    // calculate s*fMin + (1-s)*fMax
    return DistributionUtils.interpolate(fMin, fMax, 1 - s);
  }

  /**
   * @return true if the learning examples contain an element for which 
   * the coordinates are the minimal element of the data space, false 
   * otherwise
   */
  private boolean containsSmallestElement() {
    return m_estimatedCumulativeDistributions.containsKey(smallestElement);	
  }

  /**
   * @return true if the learning examples contain an element for which 
   * the coordinates are the maximal element of the data space, false 
   * otherwise
   */
  private boolean containsBiggestElement() {
    return m_estimatedCumulativeDistributions.containsKey(biggestElement);	
  }


  /**
   * Calculates the cumulative class probabilities for the given test 
   * instance. Uses the interpolationparameter from the parameterlist, and
   * always performs the single or double balanced OSDL algorithm.
   * This method doesn't change the classifier.  
   *
   * @param instance the instance to classify
   * @param s value of the interpolationparameter to use
   * @return the calculated distribution
   */
  private CumulativeDiscreteDistribution cumulativeDistributionForInstanceBalanced(
      Instance instance, double s) {

    Coordinates xc = new Coordinates(instance);
    int n = instance.numClasses();

    // n_m[i] represents the number of examples smaller or equal
    // than xc and with a class label strictly greater than i
    int[] n_m = new int[n];

    // n_M[i] represents the number of examples greater or equal
    // than xc and with a class label smaller or equal than i
    int[] n_M = new int[n];

    // Create fMin and fMax 
    CumulativeDiscreteDistribution fMin =
      DistributionUtils.getMinimalCumulativeDiscreteDistribution(n);
    CumulativeDiscreteDistribution fMax =
      DistributionUtils.getMaximalCumulativeDiscreteDistribution(n);

    // Cycle through all the map of cumulative distribution functions
    for (Iterator i = 
      m_estimatedCumulativeDistributions.keySet().iterator();
    i.hasNext(); ) {
      Coordinates yc = (Coordinates) i.next();
      CumulativeDiscreteDistribution cdf = 
	(CumulativeDiscreteDistribution) 
	m_estimatedCumulativeDistributions.get(yc);

      if (yc.equals(xc)) {
	// update n_m and n_M
	DiscreteEstimator df = 
	  (DiscreteEstimator) m_estimatedDistributions.get(yc);
	updateN_m(n_m,df);
	updateN_M(n_M,df);

	fMin = DistributionUtils.takeMin(fMin,cdf);
	fMax = DistributionUtils.takeMax(fMax,cdf);
      } else if (yc.strictlySmaller(xc)) {
	// update n_m 
	DiscreteEstimator df = 
	  (DiscreteEstimator) m_estimatedDistributions.get(yc);
	updateN_m(n_m, df);
	fMin = DistributionUtils.takeMin(fMin,cdf);
      }
      else if (xc.strictlySmaller(yc)) {
	// update n_M
	DiscreteEstimator df = 
	  (DiscreteEstimator) m_estimatedDistributions.get(yc);
	updateN_M(n_M, df);
	fMax = DistributionUtils.takeMax(fMax,cdf);
      }
    }

    double[] dd = new double[n];

    // for each label decide what formula to use, either using
    // n_m[i] and n_M[i] (if fMin[i]<fMax[i]) or using the
    // interpolationparameter s or using the double balanced version
    for (int i = 0; i < n; i++) {
      double fmin = fMin.getCumulativeProbability(i);
      double fmax = fMax.getCumulativeProbability(i);

      if (m_weighted == true) { // double balanced version
	if (fmin < fmax) { // reversed preference
	  dd[i] =  (n_m[i] * fmin + n_M[i] * fmax) 
	  / (n_m[i] + n_M[i]);
	} else {
	  if (n_m[i] + n_M[i] == 0) { // avoid division by zero
	    dd[i] = s * fmin + (1 - s) * fmax;
	  } else {
	    dd[i] = (n_M[i] * fmin + n_m[i] * fmax) 
	    / (n_m[i] + n_M[i]) ;
	  }
	}
      } else {  // singly balanced version
	dd[i] = (fmin < fmax) 
	? (n_m[i] * fmin + n_M[i] * fmax) / (n_m[i] + n_M[i])
	    : s * fmin + (1 - s) * fmax;
      }
    } try {
      return new CumulativeDiscreteDistribution(dd);
    } catch (IllegalArgumentException e) {
      // this shouldn't happen.
      System.err.println("We tried to create a cumulative "
	  + "discrete distribution from the following array");
      for (int i = 0; i < dd.length; i++) {
	System.err.print(dd[i] + " ");
      }
      System.err.println();
      throw new AssertionError(dd);
    }
  }


  /**
   * Update the array n_m using the given <code> DiscreteEstimator </code>.
   * 
   * @param n_m the array n_m that will be updated.
   * @param de the <code> DiscreteEstimator </code> that gives the 
   *        count over the different class labels.
   */
  private void updateN_m(int[] n_m, DiscreteEstimator de) {
    int[] tmp = new int[n_m.length];

    // all examples have a class labels strictly greater 
    // than 0, except those that have class label 0.
    tmp[0] = (int) de.getSumOfCounts() - (int) de.getCount(0);
    n_m[0] += tmp[0];
    for (int i = 1; i < n_m.length; i++) {

      // the examples with a class label strictly greater
      // than i are exactly those that have a class label strictly
      // greater than i-1, except those that have class label i.
      tmp[i] = tmp[i - 1] - (int) de.getCount(i);
      n_m[i] += tmp[i];
    }

    if (n_m[n_m.length - 1] != 0) {
      // this shouldn't happen
      System.err.println("******** Problem with n_m in " 
	  + m_train.relationName());
      System.err.println("Last argument is non-zero, namely : " 
	  + n_m[n_m.length - 1]);
    }
  }

  /**
   * Update the array n_M using the given <code> DiscreteEstimator </code>.
   * 
   * @param n_M the array n_M that will be updated.
   * @param de the <code> DiscreteEstimator </code> that gives the 
   *        count over the different class labels.
   */
  private void updateN_M(int[] n_M, DiscreteEstimator de) {
    int n = n_M.length;
    int[] tmp = new int[n];

    // all examples have a class label smaller or equal
    // than n-1 (which is the maximum class label)
    tmp[n - 1] = (int) de.getSumOfCounts();
    n_M[n - 1] += tmp[n - 1];
    for (int i = n - 2; i >= 0; i--) {

      // the examples with a class label smaller or equal 
      // than i are exactly those that have a class label
      // smaller or equal than i+1, except those that have 
      // class label i+1.
      tmp[i] = tmp[i + 1] - (int) de.getCount(i + 1);
      n_M[i] += tmp[i];
    }
  }

  /**
   * Builds the classifier.
   * This means that all relevant examples are stored into memory.
   * If necessary the interpolation parameter is tuned.
   *
   * @param instances the instances to be used for building the classifier
   * @throws Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    getCapabilities().testWithFail(instances);

    // copy the dataset 
    m_train = new Instances(instances);

    // new dataset in which examples with missing class value are removed
    m_train.deleteWithMissingClass();

    // build the Map for the estimatedDistributions 
    m_estimatedDistributions = new HashMap(m_train.numInstances()/2);

    // cycle through all instances 
    for (Iterator it = 
      new EnumerationIterator(instances.enumerateInstances()); 
    it.hasNext();) {
      Instance instance = (Instance) it.next();
      Coordinates c = new Coordinates(instance);

      // get DiscreteEstimator from the map
      DiscreteEstimator df = 
	(DiscreteEstimator) m_estimatedDistributions.get(c);

      // if no DiscreteEstimator is present in the map, create one 
      if (df == null) {
	df = new DiscreteEstimator(instances.numClasses(),0);
      }
      df.addValue(instance.classValue(),instance.weight()); // update
      m_estimatedDistributions.put(c,df); // put back in map
    }


    // build the map of cumulative distribution functions 
    m_estimatedCumulativeDistributions = 
      new HashMap(m_estimatedDistributions.size()/2);

    // Cycle trough the map of discrete distributions, and create a new
    // one containing cumulative discrete distributions
    for (Iterator it=m_estimatedDistributions.keySet().iterator();
    it.hasNext();) {
      Coordinates c = (Coordinates) it.next();
      DiscreteEstimator df = 
	(DiscreteEstimator) m_estimatedDistributions.get(c);
      m_estimatedCumulativeDistributions.put
      (c, new CumulativeDiscreteDistribution(df));
    }

    // check if the interpolation parameter needs to be tuned
    if (m_tuneInterpolationParameter && !m_interpolationParameterValid) {
      tuneInterpolationParameter();
    }

    // fill in the smallest and biggest element (for use in the
    // quasi monotone version of the algorithm)
    double[] tmpAttValues = new double[instances.numAttributes()];
    Instance instance = new DenseInstance(1, tmpAttValues);
    instance.setDataset(instances);
    smallestElement = new Coordinates(instance);
    if (m_Debug) {
      System.err.println("minimal element of data space = " 
	  + smallestElement);
    }
    for (int i = 0; i < tmpAttValues.length; i++) {
      tmpAttValues[i] = instances.attribute(i).numValues() - 1; 
    }

    instance = new DenseInstance(1, tmpAttValues);
    instance.setDataset(instances);
    biggestElement = new Coordinates(instance);
    if (m_Debug) {
      System.err.println("maximal element of data space = " 
	  + biggestElement);
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String classificationTypeTipText() {
    return "Sets the way in which a single label will be extracted "
    + "from the estimated distribution.";
  }

  /**
   * Sets the classification type.  Currently <code> ctype </code>
   * must be one of:
   * <ul>
   * <li> <code> CT_REGRESSION </code> : use expectation value of
   * distribution.  (Non-ordinal in nature).
   * <li> <code> CT_WEIGHTED_SUM </code> : use expectation value of
   * distribution rounded to nearest class label. (Non-ordinal in
   * nature).
   * <li> <code> CT_MAXPROB </code> : use the mode of the distribution.
   * (May deliver non-monotone results).
   * <li> <code> CT_MEDIAN </code> : use the median of the distribution
   * (rounded to the nearest class label).
   * <li> <code> CT_MEDIAN_REAL </code> : use the median of the distribution
   * but not rounded to the nearest class label.
   * </ul>
   *
   * @param value the classification type
   */
  public void setClassificationType(SelectedTag value) {
    if (value.getTags() == TAGS_CLASSIFICATIONTYPES)
      m_ctype = value.getSelectedTag().getID();
  }

  /** 
   * Returns the classification type.
   *
   * @return the classification type
   */
  public SelectedTag getClassificationType() {
    return new SelectedTag(m_ctype, TAGS_CLASSIFICATIONTYPES);
  }


  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String tuneInterpolationParameterTipText() {
    return "Whether to tune the interpolation parameter based on the bounds.";
  }
  
  /**
   * Sets whether the interpolation parameter is to be tuned based on the
   * bounds.
   * 
   * @param value if true the parameter is tuned
   */
  public void setTuneInterpolationParameter(boolean value) {
    m_tuneInterpolationParameter = value;
  }
  
  /**
   * Returns whether the interpolation parameter is to be tuned based on the
   * bounds.
   * 
   * @return true if the parameter is to be tuned
   */
  public boolean getTuneInterpolationParameter() {
    return m_tuneInterpolationParameter;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String interpolationParameterLowerBoundTipText() {
    return "Sets the lower bound for the interpolation parameter tuning (0 <= x < 1).";
  }
  
  /**
   * Sets the lower bound for the interpolation parameter tuning 
   * (0 &lt;= x &lt; 1).
   * 
   * @param value the tne lower bound
   * @throws IllegalArgumentException if bound is invalid
   */
  public void setInterpolationParameterLowerBound(double value) {
    if ( (value < 0) || (value >= 1) || (value > getInterpolationParameterUpperBound()) )
      throw new IllegalArgumentException("Illegal lower bound");
    
    m_sLower = value;
    m_tuneInterpolationParameter = true;
    m_interpolationParameterValid = false;
  }
  
  /**
   * Returns the lower bound for the interpolation parameter tuning
   * (0 &lt;= x &lt; 1).
   * 
   * @return the lower bound
   */
  public double getInterpolationParameterLowerBound() {
    return m_sLower;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String interpolationParameterUpperBoundTipText() {
    return "Sets the upper bound for the interpolation parameter tuning (0 < x <= 1).";
  }
  
  /**
   * Sets the upper bound for the interpolation parameter tuning 
   * (0 &lt; x &lt;= 1).
   * 
   * @param value the tne upper bound
   * @throws IllegalArgumentException if bound is invalid
   */
  public void setInterpolationParameterUpperBound(double value) {
    if ( (value <= 0) || (value > 1) || (value < getInterpolationParameterLowerBound()) )
      throw new IllegalArgumentException("Illegal upper bound");
    
    m_sUpper = value;
    m_tuneInterpolationParameter = true;
    m_interpolationParameterValid = false;
  }
  
  /**
   * Returns the upper bound for the interpolation parameter tuning
   * (0 &lt; x &lt;= 1).
   * 
   * @return the upper bound
   */
  public double getInterpolationParameterUpperBound() {
    return m_sUpper;
  }
  
  /**
   * Sets the interpolation bounds for the interpolation parameter.
   * When tuning the interpolation parameter only values in the interval
   * <code> [sLow, sUp] </code> are considered.
   * It is important to note that using this method immediately
   * implies that the interpolation parameter is to be tuned.
   *
   * @param sLow lower bound for the interpolation parameter, 
   * should not be smaller than 0 or greater than <code> sUp </code>
   * @param sUp upper bound for the interpolation parameter,
   * should not exceed 1 or be smaller than <code> sLow </code>
   * @throws IllegalArgumentException if one of the above conditions 
   * is not satisfied.
   */
  public void setInterpolationParameterBounds(double sLow, double sUp) 
    throws IllegalArgumentException {
    
    if (sLow < 0. || sUp > 1. || sLow > sUp) 
      throw new IllegalArgumentException("Illegal upper and lower bounds");
    m_sLower = sLow;
    m_sUpper = sUp;
    m_tuneInterpolationParameter = true;
    m_interpolationParameterValid = false;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String interpolationParameterTipText() {
    return "Sets the value of the interpolation parameter s;"
    + "Estimated distribution is s * f_min + (1 - s) *  f_max. ";
  }

  /**
   * Sets the interpolation parameter.  This immediately means that
   * the interpolation parameter is not to be tuned.
   *
   * @param s value for the interpolation parameter.
   * @throws IllegalArgumentException if <code> s </code> is not in
   * the range [0,1].
   */
  public void setInterpolationParameter(double s) 
    throws IllegalArgumentException {
    
    if (0 > s || s > 1)
      throw new IllegalArgumentException("Interpolationparameter exceeds bounds");
    m_tuneInterpolationParameter = false;
    m_interpolationParameterValid = false;
    m_s = s;
  }

  /**
   * Returns the current value of the interpolation parameter.
   *
   * @return the value of the interpolation parameter
   */
  public double getInterpolationParameter() {
    return m_s;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String numberOfPartsForInterpolationParameterTipText() {
    return "Sets the granularity for tuning the interpolation parameter; "
    + "For instance if the value is 32 then 33 values for the "
    + "interpolation are checked.";  
  }

  /**
   * Sets the granularity for tuning the interpolation parameter.
   * The interval between lower and upper bounds for the interpolation
   * parameter is divided into <code> sParts </code> parts, i.e.
   * <code> sParts + 1 </code> values will be checked when 
   * <code> tuneInterpolationParameter </code> is invoked.
   * This also means that the interpolation parameter is to
   * be tuned.
   * 
   * @param sParts the number of parts
   * @throws IllegalArgumentException if <code> sParts </code> is 
   * smaller or equal than 0.
   */
  public void setNumberOfPartsForInterpolationParameter(int sParts) 
    throws IllegalArgumentException {
    
    if (sParts <= 0)
      throw new IllegalArgumentException("Number of parts is negative");

    m_tuneInterpolationParameter = true;
    if (m_sNrParts != sParts) {
      m_interpolationParameterValid = false;
      m_sNrParts = sParts;
    }
  }

  /**
   * Gets the granularity for tuning the interpolation parameter.
   * 
   * @return the number of parts in which the interval 
   * <code> [s_low, s_up] </code> is to be split
   */
  public int getNumberOfPartsForInterpolationParameter() {
    return m_sNrParts;
  }

  /**
   * Returns a string suitable for displaying in the gui/experimenter.
   * 
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String balancedTipText() {
    return "If true, the balanced version of the OSDL-algorithm is used\n"
    + "This means that distinction is made between the normal and "
    + "reversed preference situation.";
  }

  /**
   * If <code> balanced </code> is <code> true </code> then the balanced
   * version of OSDL will be used, otherwise the ordinary version of 
   * OSDL will be in effect.
   *
   * @param balanced if <code> true </code> then B-OSDL is used, otherwise
   * it is OSDL
   */
  public void setBalanced(boolean balanced) {
    m_balanced = balanced;
  }

  /** 
   * Returns if the balanced version of OSDL is in effect.
   *
   * @return <code> true </code> if the balanced version is in effect,
   * <code> false </code> otherwise
   */
  public boolean getBalanced() {
    return m_balanced;
  }

  /** 
   * Returns a string suitable for displaying in the gui/experimenter.
   * 
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String weightedTipText() {
    return "If true, the weighted version of the OSDL-algorithm is used";
  }

  /**
   * If <code> weighted </code> is <code> true </code> then the
   * weighted version of the OSDL is used.
   * Note: using the weighted (non-balanced) version only ensures the 
   * quasi monotonicity of the results w.r.t. to training set.
   *
   * @param weighted <code> true </code> if the weighted version to be used,
   * <code> false </code> otherwise
   */
  public void setWeighted(boolean weighted) {
    m_weighted = weighted;
  }

  /**
   * Returns if the weighted version is in effect.
   *
   * @return <code> true </code> if the weighted version is in effect,
   * <code> false </code> otherwise.
   */
  public boolean getWeighted() {
    return m_weighted;
  }

  /**
   * Returns the current value of the lower bound for the interpolation 
   * parameter.
   *
   * @return the current value of the lower bound for the interpolation
   * parameter
   */
  public double getLowerBound() {
    return m_sLower;
  }

  /**
   * Returns the current value of the upper bound for the interpolation 
   * parameter.
   *
   * @return the current value of the upper bound for the interpolation
   * parameter
   */
  public double getUpperBound() {
    return m_sUpper;
  }

  /**
   * Returns the number of instances in the training set.
   *
   * @return the number of instances used for training
   */
  public int getNumInstances() {
    return m_train.numInstances();
  }

  /** Tune the interpolation parameter using the current
   *  settings of the classifier.
   *  This also sets the interpolation parameter.
   *  @return the value of the tuned interpolation parameter.
   */
  public double tuneInterpolationParameter() {
    try {
      return tuneInterpolationParameter(m_sLower, m_sUpper, m_sNrParts, m_ctype);
    } catch (IllegalArgumentException e) {
      throw new AssertionError(e);
    }
  }

  /**
   *  Tunes the interpolation parameter using the given settings.
   *  The parameters of the classifier are updated accordingly!
   *  Marks the interpolation parameter as valid.
   *  
   *  @param sLow lower end point of interval of paramters to be examined
   *  @param sUp upper end point of interval of paramters to be examined
   *  @param sParts number of parts the interval is divided into.  This thus determines
   *  the granularity of the search
   *  @param ctype the classification type to use
   *  @return the value of the tuned interpolation parameter
   *  @throws IllegalArgumentException if the given parameter list is not
   *  valid
   */
  public double tuneInterpolationParameter(double sLow, double sUp, int sParts, int ctype) 
    throws IllegalArgumentException {
    
    setInterpolationParameterBounds(sLow, sUp);
    setNumberOfPartsForInterpolationParameter(sParts);
    setClassificationType(new SelectedTag(ctype, TAGS_CLASSIFICATIONTYPES));

    m_s = crossValidate(sLow, sUp, sParts, ctype);
    m_tuneInterpolationParameter = true;
    m_interpolationParameterValid = true;
    return m_s;
  }

  /** 
   *  Tunes the interpolation parameter using the current settings
   *  of the classifier.  This doesn't change the classifier, i.e.
   *  none of the internal parameters is changed!
   *
   *  @return the tuned value of the interpolation parameter
   *  @throws IllegalArgumentException if somehow the current settings of the 
   *  classifier are illegal.
   */
  public double crossValidate() throws IllegalArgumentException {
    return crossValidate(m_sLower, m_sUpper, m_sNrParts, m_ctype);
  }

  /**
   *  Tune the interpolation parameter using leave-one-out
   *  cross validation, the loss function used is the 1-0 loss
   *  function.
   *  <p>
   *  The given settings are used, but the classifier is not
   *  updated!.  Also, the interpolation parameter s is not 
   *  set.
   *  </p>
   * 
   *  @param sLow lower end point of interval of paramters to be examined
   *  @param sUp upper end point of interval of paramters to be examined
   *  @param sNrParts number of parts the interval is divided into.  This thus determines
   *  the granularity of the search
   *  @param ctype the classification type to use
   *  @return the best value for the interpolation parameter
   *  @throws IllegalArgumentException if the settings for the
   *  interpolation parameter are not valid or if the classification 
   *  type is not valid
   */
  public double crossValidate (double sLow, double sUp, int sNrParts, int ctype) 
    throws IllegalArgumentException {

    double[] performanceStats = new double[sNrParts + 1];
    return crossValidate(sLow, sUp, sNrParts, ctype, 
	performanceStats, new ZeroOneLossFunction());
  }

  /**
   * Tune the interpolation parameter using leave-one-out
   * cross validation.  The given parameters are used, but 
   * the classifier is not changed, in particular, the interpolation
   * parameter remains unchanged.
   *
   * @param sLow lower bound for interpolation parameter
   * @param sUp upper bound for interpolation parameter
   * @param sNrParts determines the granularity of the search
   * @param ctype the classification type to use
   * @param performanceStats array acting as output, and that will
   * contain the total loss of the leave-one-out cross validation for
   * each considered value of the interpolation parameter
   * @param lossFunction the loss function to use
   * @return the value of the interpolation parameter that is considered
   * best
   * @throws IllegalArgumentException the length of the array 
   * <code> performanceStats </code> is not sufficient
   * @throws IllegalArgumentException if the interpolation parameters 
   * are not valid
   * @throws IllegalArgumentException if the classification type is 
   * not valid
   */
  public double crossValidate(double sLow, double sUp, int sNrParts, 
      int ctype, double[] performanceStats, 
      NominalLossFunction lossFunction) throws IllegalArgumentException {

    if (performanceStats.length < sNrParts + 1) {
      throw new IllegalArgumentException("Length of array is not sufficient");
    }

    if (!interpolationParametersValid(sLow, sUp, sNrParts)) {
      throw new IllegalArgumentException("Interpolation parameters are not valid");
    }

    if (!classificationTypeValid(ctype)) {
      throw new IllegalArgumentException("Not a valid classification type " + ctype);
    }

    Arrays.fill(performanceStats, 0, sNrParts + 1, 0);

    // cycle through all instances
    for (Iterator it = 
      new EnumerationIterator(m_train.enumerateInstances());
    it.hasNext(); ) {
      Instance instance = (Instance) it.next();
      double classValue = instance.classValue();
      removeInstance(instance); 

      double s = sLow;
      double step = (sUp - sLow) / sNrParts; //step size
      for (int i = 0; i <= sNrParts; i++, s += step) {
	try {
	  performanceStats[i] += 
	    lossFunction.loss(classValue,
		classifyInstance(instance, s, ctype));
	} catch (Exception exception) {

	  // XXX what should I do here, normally we shouldn't be here
	  System.err.println(exception.getMessage());
	  System.exit(1);
	}
      }

      // XXX may be done more efficiently
      addInstance(instance); // update
    }

    // select the 'best' value for s
    // to this end, we sort the array with the leave-one-out
    // performance statistics, and we choose the middle one
    // off all those that score 'best'

    // new code, august 2004
    // new code, june 2005.  If performanceStats is longer than
    // necessary, copy it first
    double[] tmp = performanceStats;
    if (performanceStats.length > sNrParts + 1) {
      tmp = new double[sNrParts + 1];
      System.arraycopy(performanceStats, 0, tmp, 0, tmp.length);
    }
    int[] sort = Utils.stableSort(tmp);
    int minIndex = 0;
    while (minIndex + 1 < tmp.length 
	&& tmp[sort[minIndex + 1]] == tmp[sort[minIndex]]) {
      minIndex++;
    }
    minIndex = sort[minIndex / 2];  // middle one 
    // int minIndex = Utils.minIndex(performanceStats); // OLD code

    return  sLow + minIndex * (sUp - sLow) / sNrParts;
  }

  /**
   * Checks if <code> ctype </code> is a valid classification 
   * type.
   * @param ctype the int to be checked
   * @return true if ctype is a valid classification type, false otherwise
   */
  private boolean classificationTypeValid(int ctype) {
    return ctype == CT_REGRESSION || ctype == CT_WEIGHTED_SUM 
    || ctype == CT_MAXPROB || ctype == CT_MEDIAN 
    || ctype == CT_MEDIAN_REAL;
  }

  /**
   * Checks if the given parameters are valid interpolation parameters.
   * @param sLow lower bound for the interval
   * @param sUp upper bound for the interval
   * @param sNrParts the number of parts the interval has to be divided in
   * @return true is the given parameters are valid interpolation parameters,
   * false otherwise
   */
  private boolean interpolationParametersValid(double sLow, double sUp, int sNrParts) {
    return sLow >= 0 && sUp <= 1 && sLow < sUp && sNrParts > 0
    || sLow == sUp && sNrParts == 0; 
    // special case included
  }

  /** 
   * Remove an instance from the classifier.  Updates the hashmaps.
   * @param instance the instance to be removed.  
   */
  private void removeInstance(Instance instance) {
    Coordinates c = new Coordinates(instance);

    // Remove instance temporarily from the Maps with the distributions
    DiscreteEstimator df = 
      (DiscreteEstimator) m_estimatedDistributions.get(c);

    // remove from df
    df.addValue(instance.classValue(),-instance.weight());

    if (Math.abs(df.getSumOfCounts() - 0) < Utils.SMALL) {

      /* There was apparently only one example with coordinates c
       * in the training set, and now we removed it.
       * Remove the key c from both maps. 
       */
      m_estimatedDistributions.remove(c);
      m_estimatedCumulativeDistributions.remove(c);
    }
    else {

      // update both maps
      m_estimatedDistributions.put(c,df);
      m_estimatedCumulativeDistributions.put
      (c, new CumulativeDiscreteDistribution(df));
    }
  }

  /**
   * Update the classifier using the given instance.  Updates the hashmaps
   * @param instance the instance to be added
   */
  private void addInstance(Instance instance) {

    Coordinates c = new Coordinates(instance);

    // Get DiscreteEstimator from the map
    DiscreteEstimator df = 
      (DiscreteEstimator) m_estimatedDistributions.get(c);

    // If no DiscreteEstimator is present in the map, create one 
    if (df == null) {
      df = new DiscreteEstimator(instance.dataset().numClasses(),0);
    }
    df.addValue(instance.classValue(),instance.weight()); // update df
    m_estimatedDistributions.put(c,df); // put back in map
    m_estimatedCumulativeDistributions.put
    (c, new CumulativeDiscreteDistribution(df));
  }

  /**
   * Returns an enumeration describing the available options.
   * For a list of available options, see <code> setOptions </code>.
   *
   * @return an enumeration of all available options.
   */
  public Enumeration listOptions() {
    Vector options = new Vector();

    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      options.addElement(enm.nextElement());

    String description = 
      "\tSets the classification type to be used.\n"
      + "\t(Default: " + new SelectedTag(CT_MEDIAN, TAGS_CLASSIFICATIONTYPES) + ")";
    String synopsis = "-C " + Tag.toOptionList(TAGS_CLASSIFICATIONTYPES);
    String name = "C";
    options.addElement(new Option(description, name, 1, synopsis));

    description = "\tUse the balanced version of the "  
      + "Ordinal Stochastic Dominance Learner";
    synopsis = "-B";
    name = "B";
    options.addElement(new Option(description, name, 1, synopsis));

    description = "\tUse the weighted version of the " 
      + "Ordinal Stochastic Dominance Learner";
    synopsis = "-W";
    name = "W";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tSets the value of the interpolation parameter (not with -W/T/P/L/U)\n" 
      + "\t(default: 0.5).";
    synopsis = "-S <value of interpolation parameter>";
    name = "S";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tTune the interpolation parameter (not with -W/S)\n" 
      + "\t(default: off)";
    synopsis = "-T";
    name = "T";
    options.addElement(new Option(description, name, 0, synopsis));

    description = 
      "\tLower bound for the interpolation parameter (not with -W/S)\n" 
      + "\t(default: 0)";
    synopsis = "-L <Lower bound for interpolation parameter>";
    name="L";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tUpper bound for the interpolation parameter (not with -W/S)\n" 
      + "\t(default: 1)";
    synopsis = "-U <Upper bound for interpolation parameter>";
    name="U";
    options.addElement(new Option(description, name, 1, synopsis));

    description = 
      "\tDetermines the step size for tuning the interpolation\n" 
      + "\tparameter, nl. (U-L)/P (not with -W/S)\n"
      + "\t(default: 10)";
    synopsis = "-P <Number of parts>";
    name="P";
    options.addElement(new Option(description, name, 1, synopsis));

    return options.elements();
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -C &lt;REG|WSUM|MAX|MED|RMED&gt;
   *  Sets the classification type to be used.
   *  (Default: MED)</pre>
   * 
   * <pre> -B
   *  Use the balanced version of the Ordinal Stochastic Dominance Learner</pre>
   * 
   * <pre> -W
   *  Use the weighted version of the Ordinal Stochastic Dominance Learner</pre>
   * 
   * <pre> -S &lt;value of interpolation parameter&gt;
   *  Sets the value of the interpolation parameter (not with -W/T/P/L/U)
   *  (default: 0.5).</pre>
   * 
   * <pre> -T
   *  Tune the interpolation parameter (not with -W/S)
   *  (default: off)</pre>
   * 
   * <pre> -L &lt;Lower bound for interpolation parameter&gt;
   *  Lower bound for the interpolation parameter (not with -W/S)
   *  (default: 0)</pre>
   * 
   * <pre> -U &lt;Upper bound for interpolation parameter&gt;
   *  Upper bound for the interpolation parameter (not with -W/S)
   *  (default: 1)</pre>
   * 
   * <pre> -P &lt;Number of parts&gt;
   *  Determines the step size for tuning the interpolation
   *  parameter, nl. (U-L)/P (not with -W/S)
   *  (default: 10)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String args;

    args = Utils.getOption('C',options);
    if (args.length() != 0) 
      setClassificationType(new SelectedTag(args, TAGS_CLASSIFICATIONTYPES));
    else
      setClassificationType(new SelectedTag(CT_MEDIAN, TAGS_CLASSIFICATIONTYPES));

    setBalanced(Utils.getFlag('B',options));

    if (Utils.getFlag('W', options)) {
      m_weighted = true;
      // ignore any T, S, P, L and U options
      Utils.getOption('T', options);
      Utils.getOption('S', options);
      Utils.getOption('P', options);
      Utils.getOption('L', options);
      Utils.getOption('U', options);
    } else {
      m_tuneInterpolationParameter = Utils.getFlag('T', options);

      if (!m_tuneInterpolationParameter) {
	// ignore P, L, U
	Utils.getOption('P', options);
	Utils.getOption('L', options);
	Utils.getOption('U', options);

	// value of s 
	args = Utils.getOption('S',options);
	if (args.length() != 0)
	  setInterpolationParameter(Double.parseDouble(args));
	else
	  setInterpolationParameter(0.5);
      }
      else {
	// ignore S
	Utils.getOption('S', options);
	
	args = Utils.getOption('L',options);
	double l = m_sLower;
	if (args.length() != 0)
	  l = Double.parseDouble(args);
	else
	  l = 0.0;

	args = Utils.getOption('U',options);
	double u = m_sUpper;
	if (args.length() != 0)
	  u = Double.parseDouble(args);
	else
	  u = 1.0;

	if (m_tuneInterpolationParameter)
	  setInterpolationParameterBounds(l, u);

	args = Utils.getOption('P',options);
	if (args.length() != 0)
	  setNumberOfPartsForInterpolationParameter(Integer.parseInt(args));
	else
	  setNumberOfPartsForInterpolationParameter(10);
      }
    }
    
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the OSDLCore classifier.
   *
   * @return an array of strings suitable for passing 
   * to <code> setOptions </code>
   */
  public String[] getOptions() {
    int       	i;
    Vector    	result;
    String[]  	options;

    result = new Vector();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    // classification type
    result.add("-C");
    result.add("" + getClassificationType());

    if (m_balanced)
      result.add("-B");

    if (m_weighted) {
      result.add("-W");
    }
    else {
      // interpolation parameter
      if (!m_tuneInterpolationParameter) {
        result.add("-S");
        result.add(Double.toString(m_s));
      }
      else {
        result.add("-T");
        result.add("-L");
        result.add(Double.toString(m_sLower));
        result.add("-U");
        result.add(Double.toString(m_sUpper));
        result.add("-P");
        result.add(Integer.toString(m_sNrParts));
      }
    }

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns a description of the classifier.
   * Attention: if debugging is on, the description can be become
   * very lengthy.
   *
   * @return a string containing the description
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();

    // balanced or ordinary OSDL
    if (m_balanced) {
      sb.append("Balanced OSDL\n=============\n\n");
    } else {
      sb.append("Ordinary OSDL\n=============\n\n");
    }

    if (m_weighted) {
      sb.append("Weighted variant\n");
    }

    // classification type used
    sb.append("Classification type: " + getClassificationType() + "\n");

    // parameter s 
    if (!m_weighted) {
      sb.append("Interpolation parameter: " + m_s + "\n");
      if (m_tuneInterpolationParameter) {
	sb.append("Bounds and stepsize: " + m_sLower + " " + m_sUpper + 
	    " " + m_sNrParts + "\n");
	if (!m_interpolationParameterValid) {
	  sb.append("Interpolation parameter is not valid");
	}
      }
    }


    if(m_Debug) {

      if (m_estimatedCumulativeDistributions != null) { 
	/* 
	 * Cycle through all the map of cumulative distribution functions
	 * and print each cumulative distribution function
	 */
	for (Iterator i = 
	  m_estimatedCumulativeDistributions.keySet().iterator();
	i.hasNext(); ) {
	  Coordinates yc = (Coordinates) i.next();
	  CumulativeDiscreteDistribution cdf = 
	    (CumulativeDiscreteDistribution) 
	    m_estimatedCumulativeDistributions.get(yc);
	  sb.append( "[" + yc.hashCode() + "] " + yc.toString() 
	      + " --> " + cdf.toString() + "\n");
	}
      }
    }
    return sb.toString();
  }
}
