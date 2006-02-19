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
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

/*
 *    PaceRegression.java
 *    Copyright (C) 2002 Yong Wang
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.pace.ChisqMixture;
import weka.classifiers.functions.pace.MixtureDistribution;
import weka.classifiers.functions.pace.NormalMixture;
import weka.classifiers.functions.pace.PaceMatrix;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NoSupportForMissingValuesException;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.WekaException;
import weka.core.Capabilities.Capability;
import weka.core.matrix.DoubleVector;
import weka.core.matrix.IntVector;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Class for building pace regression linear models and using them for
 * prediction. <p>
 * 
 * Under regularity conditions, pace regression is provably optimal when
 * the number of coefficients tends to infinity. It consists of a group of
 * estimators that are either overall optimal or optimal under certain
 * conditions. <p>
 *
 * The current work of the pace regression theory, and therefore also this
 * implementation, do not handle: <p>
 *
 * - missing values <br>
 * - non-binary nominal attributes <br>
 * - the case that n - k is small where n is number of instances and k is  
 *   number of coefficients (the threshold used in this implmentation is 20) 
 * <p>
 *  
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 * -E estimator <br>
 * The estimator can be one of the following: <br>
 * <ul>
 * <li>eb -- Empirical Bayes estimator for noraml mixture (default) <br>
 * <li>nested -- Optimal nested model selector for normal mixture <br>
 * <li>subset -- Optimal subset selector for normal mixture <br>
 * <li>pace2 -- PACE2 for Chi-square mixture <br>
 * <li>pace4 -- PACE4 for Chi-square mixture<br>
 * <li>pace6 -- PACE6 for Chi-square mixture <br>
 * <li>ols -- Ordinary least squares estimator <br>
 * <li>aic -- AIC estimator <br>
 * <li>bic -- BIC estimator <br>
 * <li>ric -- RIC estimator <br>
 * <li>olsc -- Ordinary least squares subset selector with a threshold <br>
 * </ul>
 * -S <threshold value <br>
 * Threshold for the olsc estimator<p>
 *
 * <p>
 * REFERENCES <p>
 * 
 * Wang, Y. (2000). "A new approach to fitting linear models in high
 * dimensional spaces." PhD Thesis. Department of Computer Science,
 * University of Waikato, New Zealand. <p>
 * 
 * Wang, Y. and Witten, I. H. (2002). "Modeling for optimal probability
 * prediction." Proceedings of ICML'2002. Sydney. <p>
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $ */
public class PaceRegression extends Classifier implements OptionHandler,
					       WeightedInstancesHandler {

  static final long serialVersionUID = 7230266976059115435L;
  
  /** The model used */
  Instances m_Model = null;

  /** Array for storing coefficients of linear regression. */
  private double[] m_Coefficients;

  /** The index of the class attribute */
  private int m_ClassIndex;

  /** True if debug output will be printed */
  private boolean m_Debug;

  private static final int olsEstimator = 0;
  private static final int ebEstimator = 1;
  private static final int nestedEstimator = 2;
  private static final int subsetEstimator = 3; 
  private static final int pace2Estimator = 4; 
  private static final int pace4Estimator = 5; 
  private static final int pace6Estimator = 6; 
  private static final int olscEstimator = 7;
  private static final int aicEstimator = 8;
  private static final int bicEstimator = 9;
  private static final int ricEstimator = 10;
  public static final Tag [] TAGS_ESTIMATOR = {
    new Tag(olsEstimator, "Ordinary least squares"),
    new Tag(ebEstimator, "Empirical Bayes"),
    new Tag(nestedEstimator, "Nested model selector"),
    new Tag(subsetEstimator, "Subset selector"),
    new Tag(pace2Estimator, "PACE2"),
    new Tag(pace4Estimator, "PACE4"),
    new Tag(pace6Estimator, "PACE6"),
    new Tag(olscEstimator, "Ordinary least squares selection"),
    new Tag(aicEstimator, "AIC"),
    new Tag(bicEstimator, "BIC"),
    new Tag(ricEstimator, "RIC")
  };

  private int paceEstimator = ebEstimator;  
  private double olscThreshold = 2;  // AIC
  
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for building pace regression linear models and using them for "
      +"prediction. \n\n"
      +"Under regularity conditions, pace regression is provably optimal when "
      +"the number of coefficients tends to infinity. It consists of a group of "
      +"estimators that are either overall optimal or optimal under certain "
      +"conditions.\n\n"
      +"The current work of the pace regression theory, and therefore also this "
      +"implementation, do not handle: \n\n"
      +"- missing values \n"
      +"- non-binary nominal attributes \n"
      +"- the case that n - k is small where n is the number of instances and k is "  
      +"the number of coefficients (the threshold used in this implmentation is 20)\n\n"
      +"For more information see:\n\n"
      +"Wang, Y. (2000). A new approach to fitting linear models in high "
      +"dimensional spaces. PhD Thesis. Department of Computer Science, "
      +"University of Waikato, New Zealand. \n\n"
      +"Wang, Y. and Witten, I. H. (2002). Modeling for optimal probability "
      +"prediction. Proceedings of ICML'2002. Sydney.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.BINARY_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Builds a pace regression model for the given data.
   *
   * @param data the training data to be used for generating the
   * linear regression function
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    Capabilities cap = getCapabilities();
    cap.setMinimumNumberInstances(20 + data.numAttributes());
    cap.testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    /*
     * initialize the following
     */
    m_Model = new Instances(data, 0);
    m_ClassIndex = data.classIndex();
    double[][] transformedDataMatrix = 
    getTransformedDataMatrix(data, m_ClassIndex);
    double[] classValueVector = data.attributeToDoubleArray(m_ClassIndex);
    
    m_Coefficients = null;

    /* 
     * Perform pace regression
     */
    m_Coefficients = pace(transformedDataMatrix, classValueVector);
  }

  /**
   * pace regression
   *
   * @param matrix_X matrix with observations
   * @param vector_Y vektor with class values
   * @return vector with coefficients
   * @exception Exception if pace regression cannot be done successfully
   */
  private double [] pace(double[][] matrix_X, double [] vector_Y) {
    
    PaceMatrix X = new PaceMatrix( matrix_X );
    PaceMatrix Y = new PaceMatrix( vector_Y, vector_Y.length );
    IntVector pvt = IntVector.seq(0, X.getColumnDimension()-1);
    int n = X.getRowDimension();
    int kr = X.getColumnDimension();

    X.lsqrSelection( Y, pvt, 1 );
    X.positiveDiagonal( Y, pvt );
    
    int k = pvt.size();

    PaceMatrix sol = (PaceMatrix) Y.clone();
    X.rsolve( sol, pvt, pvt.size() );
    DoubleVector betaHat = sol.getColumn(0).unpivoting(pvt, kr); 
    DoubleVector r = Y.getColumn( pvt.size(), n-1, 0);
    double sde = Math.sqrt(r.sum2() / r.size());
    
    DoubleVector aHat = Y.getColumn( 0, pvt.size()-1, 0).times( 1./sde );

    DoubleVector aTilde = null;
    switch( paceEstimator) {
    case ebEstimator: 
    case nestedEstimator:
    case subsetEstimator:
      NormalMixture d = new NormalMixture();
      d.fit( aHat, MixtureDistribution.NNMMethod ); 
      if( paceEstimator == ebEstimator ) 
	aTilde = d.empiricalBayesEstimate( aHat );
      else if( paceEstimator == ebEstimator ) 
	aTilde = d.subsetEstimate( aHat );
      else aTilde = d.nestedEstimate( aHat );
      break;
    case pace2Estimator: 
    case pace4Estimator:
    case pace6Estimator:
      DoubleVector AHat = aHat.square();
      ChisqMixture dc = new ChisqMixture();
      dc.fit( AHat, MixtureDistribution.NNMMethod ); 
      DoubleVector ATilde; 
      if( paceEstimator == pace6Estimator ) 
	ATilde = dc.pace6( AHat );
      else if( paceEstimator == pace2Estimator ) 
	ATilde = dc.pace2( AHat );
      else ATilde = dc.pace4( AHat );
      aTilde = ATilde.sqrt().times( aHat.sign() );
      break;
    case olsEstimator: 
      aTilde = aHat.copy();
      break;
    case aicEstimator: 
    case bicEstimator:
    case ricEstimator: 
    case olscEstimator:
      if(paceEstimator == aicEstimator) olscThreshold = 2;
      else if(paceEstimator == bicEstimator) olscThreshold = Math.log( n );
      else if(paceEstimator == ricEstimator) olscThreshold = 2*Math.log( kr );
      aTilde = aHat.copy();
      for( int i = 0; i < aTilde.size(); i++ )
	if( Math.abs(aTilde.get(i)) < Math.sqrt(olscThreshold) ) 
	  aTilde.set(i, 0);
    }
    PaceMatrix YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    DoubleVector betaTilde = YTilde.getColumn(0).unpivoting( pvt, kr );
    
    return betaTilde.getArrayCopy();
  }

  /**
   * Checks if an instance has a missing value.
   * @param instance the instance
   * @return true if missing value is present
   */
  public boolean checkForMissing(Instance instance, Instances model) {

    for (int j = 0; j < instance.numAttributes(); j++) {
      if (j != model.classIndex()) {
	if (instance.isMissing(j)) {
	  return true;
	}
      }
    }
    return false;
  }

  /**
   * Transforms dataset into a two-dimensional array.
   *
   * @param data dataset
   * @param classIndex index of the class attribute
   */
  private double [][] getTransformedDataMatrix(Instances data, 
					       int classIndex) {
    int numInstances = data.numInstances();
    int numAttributes = data.numAttributes();
    int middle = classIndex;
    if (middle < 0) { 
      middle = numAttributes;
    }

    double[][] result = new double[numInstances]
    [numAttributes];
    for (int i = 0; i < numInstances; i++) {
      Instance inst = data.instance(i);
      
      result[i][0] = 1.0;

      // the class value (lies on index middle) is left out
      for (int j = 0; j < middle; j++) {
	result[i][j + 1] = inst.value(j);
      }
      for (int j = middle + 1; j < numAttributes; j++) {
	result[i][j] = inst.value(j);
      }
    }
    return result;
  }


  /**
   * Classifies the given instance using the linear regression function.
   *
   * @param instance the test instance
   * @return the classification
   * @exception Exception if classification can't be done successfully
   */
  public double classifyInstance(Instance instance) throws Exception {
    
    if (m_Coefficients == null) {
      throw new Exception("Pace Regression: No model built yet.");
    }
    
    // check for missing data and throw exception if some are found
    if (checkForMissing(instance, m_Model)) {
      throw new NoSupportForMissingValuesException("Can't handle missing values!");
    }

    // Calculate the dependent variable from the regression model
    return regressionPrediction(instance,
				m_Coefficients);
  }

  /**
   * Outputs the linear regression model as a string.
   */
  public String toString() {

    if (m_Coefficients == null) {
      return "Pace Regression: No model built yet.";
    }
    //    try {
    StringBuffer text = new StringBuffer();
    
    text.append("\nPace Regression Model\n\n");
    
    text.append(m_Model.classAttribute().name()+" =\n\n");
    int index = 0;	  
    
    text.append(Utils.doubleToString(m_Coefficients[0],
				     12, 4) );
    
    for (int i = 1; i < m_Coefficients.length; i++) {
      
      // jump over the class attribute
      if (index == m_ClassIndex) index++;
      
      if (m_Coefficients[i] != 0.0) {
	// output a coefficient if unequal zero
	text.append(" +\n");
	text.append(Utils.doubleToString(m_Coefficients[i], 12, 4)
		    + " * ");
	text.append(m_Model.attribute(index).name());
      }
      index ++;
    }
    
    return text.toString();
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    newVector.addElement(new Option("\tProduce debugging output.\n"
				    + "\t(default no debugging output)",
				    "D", 0, "-D"));
    newVector.addElement(new Option("\tThe estimator can be one of the following:\n" + 
				    "\t\teb\tEmpirical Bayes(default)\n" +
				    "\t\tnested\tOptimal nested model\n" + 
				    "\t\tsubset\tOptimal subset\n" +
				    "\t\tpace2\tPACE2\n" +
				    "\t\tpace4\tPACE4\n" +
				    "\t\tpace6\tPACE6\n\n" + 
				    "\t\tols\tOrdinary least squares\n" +  
				    "\t\taic\tAIC\n" +  
				    "\t\tbic\tBIC\n" +  
				    "\t\tric\tRIC\n" +  
				    "\t\tolsc\tOLSC", 
				    "E", 0, "-E <estimator>"));
    newVector.addElement(new Option("\tThreshold value for the OLSC estimator",
				    "S", 0, "-S <threshold value>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p>
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setDebug(Utils.getFlag('D', options));

    String estimator = Utils.getOption('E', options);
    if ( estimator.equals("ols") ) paceEstimator = olsEstimator;
    else if ( estimator.equals("olsc") ) paceEstimator = olscEstimator;
    else if( estimator.equals("eb") || estimator.equals("") ) 
      paceEstimator = ebEstimator;
    else if ( estimator.equals("nested") ) paceEstimator = nestedEstimator;
    else if ( estimator.equals("subset") ) paceEstimator = subsetEstimator;
    else if ( estimator.equals("pace2") ) paceEstimator = pace2Estimator; 
    else if ( estimator.equals("pace4") ) paceEstimator = pace4Estimator;
    else if ( estimator.equals("pace6") ) paceEstimator = pace6Estimator;
    else if ( estimator.equals("aic") ) paceEstimator = aicEstimator;
    else if ( estimator.equals("bic") ) paceEstimator = bicEstimator;
    else if ( estimator.equals("ric") ) paceEstimator = ricEstimator;
    else throw new WekaException("unknown estimator " + estimator + 
				 " for -E option" );

    String string = Utils.getOption('S', options);
    if( ! string.equals("") ) olscThreshold = Double.parseDouble( string );
    
  }

  /**
   * Returns the coefficients for this linear model.
   */
  public double[] coefficients() {

    double[] coefficients = new double[m_Coefficients.length];
    for (int i = 0; i < coefficients.length; i++) {
      coefficients[i] = m_Coefficients[i];
    }
    return coefficients;
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [6];
    int current = 0;

    if (getDebug()) {
      options[current++] = "-D";
    }

    options[current++] = "-E";
    switch (paceEstimator) {
    case olsEstimator: options[current++] = "ols";
      break;
    case olscEstimator: options[current++] = "olsc";
      options[current++] = "-S";
      options[current++] = "" + olscThreshold;
      break;
    case ebEstimator: options[current++] = "eb";
      break;
    case nestedEstimator: options[current++] = "nested";
      break;
    case subsetEstimator: options[current++] = "subset";
      break;
    case pace2Estimator: options[current++] = "pace2";
      break; 
    case pace4Estimator: options[current++] = "pace4";
      break;
    case pace6Estimator: options[current++] = "pace6";
      break;
    case aicEstimator: options[current++] = "aic";
      break;
    case bicEstimator: options[current++] = "bic";
      break;
    case ricEstimator: options[current++] = "ric";
      break;
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  
  /**
   * Get the number of coefficients used in the model
   *
   * @return the number of coefficients
   */
  public int numParameters()
  {
    return m_Coefficients.length-1;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Output debug information to the console.";
  }

  /**
   * Controls whether debugging output will be printed
   *
   * @param debug true if debugging output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Controls whether debugging output will be printed
   *
   * @param debug true if debugging output should be printed
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String estimatorTipText() {
    return "The estimator to use.\n\n"
      +"eb -- Empirical Bayes estimator for noraml mixture (default)\n"
      +"nested -- Optimal nested model selector for normal mixture\n"
      +"subset -- Optimal subset selector for normal mixture\n"
      +"pace2 -- PACE2 for Chi-square mixture\n"
      +"pace4 -- PACE4 for Chi-square mixture\n"
      +"pace6 -- PACE6 for Chi-square mixture\n"
      +"ols -- Ordinary least squares estimator\n"
      +"aic -- AIC estimator\n"
      +"bic -- BIC estimator\n"
      +"ric -- RIC estimator\n"
      +"olsc -- Ordinary least squares subset selector with a threshold";
  }
  
  /**
   * Gets the estimator
   *
   * @return the estimator
   */
  public SelectedTag getEstimator() {

    return new SelectedTag(paceEstimator, TAGS_ESTIMATOR);
  }
  
  /**
   * Sets the estimator.
   *
   * @param estimator the new estimator
   */
  public void setEstimator(SelectedTag estimator) {
    
    if (estimator.getTags() == TAGS_ESTIMATOR) {
      paceEstimator = estimator.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Threshold for the olsc estimator.";
  }

  /**
   * Set threshold for the olsc estimator
   *
   * @param threshold the threshold for the olsc estimator
   */
  public void setThreshold(double newThreshold) {

    olscThreshold = newThreshold;
  }

  /**
   * Gets the threshold for olsc estimator
   *
   * @return the threshold
   */
  public double getThreshold() {

    return olscThreshold;
  }


  /**
   * Calculate the dependent value for a given instance for a
   * given regression model.
   *
   * @param transformedInstance the input instance
   * @param selectedAttributes an array of flags indicating which 
   * attributes are included in the regression model
   * @param coefficients an array of coefficients for the regression
   * model
   * @return the regression value for the instance.
   * @exception Exception if the class attribute of the input instance
   * is not assigned
   */
  private double regressionPrediction(Instance transformedInstance,
				      double [] coefficients) 
    throws Exception {

    int column = 0;
    double result = coefficients[column];
    for (int j = 0; j < transformedInstance.numAttributes(); j++) {
      if (m_ClassIndex != j) {
	column++;
	result += coefficients[column] * transformedInstance.value(j);
      }
    }
    
    return result;
  }

  /**
   * Generates a linear regression function predictor.
   *
   * @param String the options
   */
  public static void main(String argv[]) {
    
    Classifier scheme;
    try {
      scheme = new PaceRegression();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      // System.out.println(e.getMessage());
    }
  }
}

