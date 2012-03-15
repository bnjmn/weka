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
 *    EMImputation.java
 *    Copyright (C) 2009 Amri Napolitano
 *
 */


package weka.filters.unsupervised.attribute;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.Instance; 
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;
import weka.core.matrix.Matrix;
import weka.filters.Filter;
import weka.experiment.Stats;

/** 
 <!-- globalinfo-start -->
 * Replaces missing numeric values using Expectation Maximization with a multivariate normal model. Described in " Schafer, J.L. Analysis of Incomplete Multivariate Data, New York: Chapman and Hall, 1997."
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Maximum number of iterations for Expectation 
 *  Maximization. (-1 = no maximum)</pre>
 * 
 * <pre> -E
 *  Threshold for convergence in Expectation 
 *  Maximization. If the change in the observed data 
 *  log-likelihood (posterior density if a ridge prior 
 *   is being used) across iterations is no more than 
 *  this value, then convergence is considered to be 
 *  achieved and the iterative process is ceased. 
 *  (default = 0.0001)</pre>
 * 
 * <pre> -P
 *  Use a ridge prior instead of the noninformative 
 *  prior. This helps when the data has a singular 
 *  covariance matrix.</pre>
 * 
 * <pre> -Q
 *  The ridge parameter for when a ridge prior is 
 *  used.</pre>
 * 
 <!-- options-end -->
 * 
 * @author Amri Napolitano 
 * @version $Revision$
 */
public class EMImputation 
extends SimpleBatchFilter
implements UnsupervisedFilter {
  
  /** for serialization */
  static final long serialVersionUID = -2519262133734188184L;
  
  /** Number of EM iterations */
  private int m_numIterations = -1;
  
  /** Threshold for convergence of EM */
  private double m_LogLikelihoodThreshold = 1e-4;
  
  /** Whether to use a ridge prior */
  private boolean m_ridgePrior = false;
  
  /** Ridge value for ridge prior */
  private double m_ridge = 1.0e-8;
  
  /** Number of attributes to be involved in imputation */
  private int m_numAttributes;
  
  /** Means of original data (for standardization) */
  private double [] m_means = null;
  
  /** Standard deviations of original data (for standardization) */
  private double [] m_stdDevs = null;
  
  /** Filter to remove unused attributes */
  private Remove m_unusedAttributeRemover = null;
  
  /** Flags indicating if attributes won't be used for imputation */
  private boolean [] m_unusedAtts = null;
  
  /** Parameters of data (for imputation of future instances) */
  private Matrix m_theta = null;
  
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    
    return "Replaces missing numeric values using Expectation Maximization with a" +
        " multivariate normal model. Described in \" Schafer, J.L. Analysis of" +
        " Incomplete Multivariate Data, New York: Chapman and Hall, 1997.\"";
  }
  
  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();
    
    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }
  
  /**
   * Returns an enumeration describing the available options. <p>
   *
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector options = new Vector(4);
    options.addElement(new Option("\tMaximum number of iterations for Expectation \n" +
                                  "\tMaximization. (-1 = no maximum)", "N", 1, "-N"));
    
    options.addElement(new Option("\tThreshold for convergence in Expectation \n" +
                                  "\tMaximization. If the change in the observed data \n" +
                                  "\tlog-likelihood (posterior density if a ridge prior \n" +
                                  "\t is being used) across iterations is no more than \n" +
                                  "\tthis value, then convergence is considered to be \n" +
                                  "\tachieved and the iterative process is ceased. \n" +
                                  "\t(default = 0.0001)", "E",1,"-E"));
    
    options.addElement(new Option("\tUse a ridge prior instead of the noninformative \n" +
                                  "\tprior. This helps when the data has a singular \n" +
                                  "\tcovariance matrix.", "P", 0, "-P"));
    options.addElement(new Option("\tThe ridge parameter for when a ridge prior is \n" +
                                  "\tused.", "Q", 1, "-Q"));
    return  options.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N
   *  Maximum number of iterations for Expectation 
   *  Maximization. (-1 = no maximum)</pre>
   * 
   * <pre> -E
   *  Threshold for convergence in Expectation 
   *  Maximization. If the change in the observed data 
   *  log-likelihood (posterior density if a ridge prior 
   *   is being used) across iterations is no more than 
   *  this value, then convergence is considered to be 
   *  achieved and the iterative process is ceased. 
   *  (default = 0.0001)</pre>
   * 
   * <pre> -P
   *  Use a ridge prior instead of the noninformative 
   *  prior. This helps when the data has a singular 
   *  covariance matrix.</pre>
   * 
   * <pre> -Q
   *  The ridge parameter for when a ridge prior is 
   *  used.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions (String[] options)
  throws Exception {
    String optionString;
    
    // set # EM iterations
    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setNumIterations(Integer.parseInt(optionString));
    }
    // set log-likelihood EM convergence threshold
    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      setLogLikelihoodThreshold(Double.valueOf(optionString).doubleValue());
    }
    // set whether to use ridge prior
    setUseRidgePrior(Utils.getFlag('P', options));
    // set ridge parameter
    optionString = Utils.getOption('Q', options);
    if (optionString.length() != 0) {
      setRidge(Double.valueOf(optionString));
    }
  }
  
  /**
   * Gets the current settings of EMImputation
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    
    String[] options = new String[7];
    int current = 0;
    
    options[current++] = "-N";
    options[current++] = "" + getNumIterations();
    
    options[current++] = "-E";
    options[current++] = "" + getLogLikelihoodThreshold();
    
    options[current++] = "-Q";
    options[current++] = "" + getRidge();
    
    if(getUseRidgePrior()) {
      options[current++] = "-P";
    }
    
    while(current < options.length) {
      options[current++] = "";
    }
    
    return  options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "Maximum number of iterations for Expectation Maximization. " +
            "EM is used to initialize the parameters of the multivariate normal " +
            "distribution. (-1 = no maximum)";
  }
  
  /**
   * Sets the maximum number of EM iterations
   * @param newIterations the maximum number of EM iterations
   */
  public void setNumIterations(int newIterations) {
      m_numIterations = newIterations;
  }
  
  /**
   * Gets the maximum number of EM iterations
   * @return the maximum number of EM iterations
   */
  public int getNumIterations() {
    return m_numIterations;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String logLikelihoodThresholdTipText() {
    return "Log-likelihood threshold for convergence in Expectation Maximization. " +
            "If the change in the observed data log-likelihood across iterations " +
            "is no more than this value, then convergence is considered to be " +
            "achieved and the iterative process is ceased. (default = 0.0001)";
  }
  
  /**
   * Sets the EM log-likelihood convergence threshold
   * @param newThreshold the EM log-likelihood convergence threshold
   */
  public void setLogLikelihoodThreshold(double newThreshold) {
      m_LogLikelihoodThreshold = newThreshold;
  }
  
  /**
   * Gets the EM log-likelihood convergence threshold
   * @return the EM log-likelihood convergence threshold
   */
  public double getLogLikelihoodThreshold() {
    return m_LogLikelihoodThreshold;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return    tip text for this property suitable for
   *      displaying in the explorer/experimenter gui
   */
  public String useRidgePriorTipText() {
    return "Use a ridge prior instead of noninformative prior.";
  }

  /**
   * Get whether to use a ridge prior.
   *
   * @return    whether to use a ridge prior.
   */
  public boolean getUseRidgePrior() {
    return m_ridgePrior;
  }

  /**
   * Set whether to use a ridge prior. 
   *
   * @param prior whether to use a ridge prior.
   */
  public void setUseRidgePrior(boolean prior) {
    m_ridgePrior = prior;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return    tip text for this property suitable for
   *      displaying in the explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "Ridge parameter for ridge prior.";
  }

  /**
   * Get ridge parameter.
   *
   * @return   the ridge parameter
   */
  public double getRidge() {
    return m_ridge;
  }

  /**
   * Set ridge parameter 
   *
   * @param ridge new ridge parameter
   */
  public void setRidge(double ridge) {
    m_ridge = ridge;
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
  throws Exception {
    
    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    return true;
  }
  
  /**
   * Determines the output format based on the input format and returns 
   * this.
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   * @see   #hasImmediateOutputFormat()
   * @see   #batchFinished()
   */
  protected Instances determineOutputFormat(Instances inputFormat) {
    
    return inputFormat;
  }
  
  /**
   * Processes the given data (may change the provided dataset) and returns
   * the modified version. This method is called in batchFinished().
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the arguments are inappropriate or the processing goes wrong
   * @see               #batchFinished()
   */
  protected Instances process(Instances instances) throws Exception {
    
    int numInstances = instances.numInstances();
    int numAttributes = instances.numAttributes();
    int classIndex = instances.classIndex();
    // if only one numeric attribute, don't do procedure
    if (((classIndex < 0 || instances.classAttribute().isNumeric()) && 
        numAttributes < 2) || numAttributes < 3) {
      throw new Exception("Must have 2 or more numeric attributes for EM Imputation");     
    }
    
    // check for only numeric independent attributes
    for (int i = 0; i < numAttributes; i++) {
      if (instances.attribute(i).isNumeric() == false && instances.classIndex() != i) {
        throw new Exception("EM Imputation can only handle numeric attributes");
      }
    }
    
    // check inputs for ok values
    if (m_LogLikelihoodThreshold < 0) { // if loglikelihood threshold is negative, convergence would never occur
      throw new Exception("Log-likelihood threshold must be non-negative.");
    }
    if (m_ridgePrior == true && m_ridge <= 0) {
      throw new Exception("Ridge parameter should be positive.");
    }
    
    // create datasets ready for processing
    Instances preprocessed, dataWithClass;
    Instances [] datasets = null;
    if (isFirstBatchDone() == false) { // model hasn't been built yet
      // check that n > p + 1
      if (numInstances < numAttributes + 2) {
        throw new Exception("EMImputation: Number of instances must be >= number of attributes + 2");
      }
      datasets = prepareData(instances, true);
    } else { // model has been built
      datasets = prepareData(instances, false);
    }
    preprocessed = datasets[0]; // holds data sorted by missing data pattern with attribute about pattern
    dataWithClass = datasets[1]; // holds data in same sorted order without the class removed
    
    // build model if it hasn't been built yet
    if (isFirstBatchDone() == false) {
      // get matrix of complete-data sufficient statistics, T, for observed values
      Matrix t_obs = getTObs(preprocessed);
      
      // perform expectation maximization to determine model parameters
      m_theta = EM(preprocessed, t_obs);
    }

    // fill in missing values from model
    impute(preprocessed, m_theta);
    
    // produce final output dataset
    dataWithClass = postProcessData(preprocessed, dataWithClass);
    
    if (dataWithClass.numInstances() != 0) {
      return dataWithClass;
    } else {
      return instances;
    }
  }
  
  /**
   * Standardizes a (preprocessed) dataset using m_means and m_stdDevs.
   * @param data        dataset to standardize
   */
  private void standardize(Instances data) {
    
    for (int i = 0; i < data.numInstances(); i++) {
      for (int j = 1; j < m_numAttributes + 1; j++) {
        if (data.instance(i).isMissing(j) == false) {
          double value = data.instance(i).value(j);
            data.instance(i).setValue(j, (value - m_means[j - 1]) / m_stdDevs[j - 1]);
        }
      }
    }
  }
  
  /**
   * Preprocesses data to be used for EM
   * @param data            the data to be processed
   * @return preprocessed   the prepared data
   * @throws Exception      if processing goes wrong
   */
  private Instances prepareMissing(Instances data) throws Exception {
    
    // properties of data
    int numInstances = data.numInstances();
    int numAttributes = data.numAttributes();
    
    Instances preprocessed = new Instances(data, data.numInstances());
    preprocessed.insertAttributeAt(new Attribute("NumInPattern"), 0);
    // add instances to preprocessed sorted by missingness pattern
    int currentPatternStart = 0; // index of 1st instance in current pattern
    int numInCurrentPattern = 0; // number of instances with current pattern
    int totalAdded = 0; // total instances added to new dataset
    boolean [] addedAlready = new boolean[numInstances];
    boolean [] missing = new boolean[numAttributes]; // missing columns
    
    while (totalAdded < numInstances) { // until all instances have been added
      // set up pattern to find
      numInCurrentPattern = 0;
      int numMissing = 0;
      int newPatternIndex = -1; // index of instance with new pattern
      for (int i = 0; newPatternIndex == -1 && i < numInstances; i++) {
        if (!addedAlready[i]) {
          newPatternIndex = i;
          addedAlready[i] = true;
          numInCurrentPattern++;
          totalAdded++;
        }
      }
      // store new missing pattern
      for (int j = 0; j < numAttributes; j++) {
        if (m_unusedAtts[j] == true) {
          numMissing++;
          continue;
        }
        if (data.instance(newPatternIndex).isMissing(j)) {
          missing[j] = true;
          numMissing++;
        } else {
          missing[j] = false;
        }
      }
      if (numMissing == numAttributes) { // all attributes are missing - don't use
        continue;
      }
      
      // create modified instance and add it to the new dataset
      double [] attributes = new double[numAttributes + 1];
      for (int j = 0; j < numAttributes; j++) {
        attributes[j + 1] = data.instance(newPatternIndex).value(j);
      }
      Instance newInstance = new DenseInstance(data.instance(newPatternIndex).weight(), attributes);
      preprocessed.add(newInstance);
        
      // go through all remaining instances to find those with same pattern
      for (int i = 0; i < numInstances; i++) {
        if (addedAlready[i]) {
          continue;
        }
        // go through all the attributes to see if they match the pattern
        boolean match = true; // if current instance matches the pattern
        for (int j = 0; j < numAttributes && match == true; j++) {
          if (m_unusedAtts[j] == true) {
            continue;
          }
          if (data.instance(i).isMissing(j) != missing[j]) {
            match = false; // not a matching pattern
          }
        }
        if (match == true) { // if pattern matches
          // create modified instance and add it to the new dataset
          attributes = new double[numAttributes + 1];
          for (int l = 0; l < numAttributes; l++) {
            attributes[l + 1] = data.instance(i).value(l);
          }
          newInstance = new DenseInstance(data.instance(i).weight(), attributes);
          preprocessed.add(newInstance);
          
          // mark current instance added
          addedAlready[i] = true;
          
          // increment number of instances in current pattern and total
          numInCurrentPattern++;
          totalAdded++;
        }
      } // end iterating through instances remaining in original data
      
      // set number of iterations for fully found pattern
      preprocessed.instance(currentPatternStart).setValue(0, numInCurrentPattern);
      currentPatternStart += numInCurrentPattern;
    } // end adding instances to new dataset
    
    return preprocessed;
  }
  
  /**
   * Sets up data for performing imputation. If firstTime is true, it sets some 
   * initial global parameters (e.g. means/std devs for standardization).
   * @param originalData    dataset preprocess
   * @param firstTime       flag for whether or not to set data parameters
   * @return data           array of preprocessed datasets necessary for other imputation functions
   * @throws Exception      if processing goes wrong
   */
  private Instances [] prepareData(Instances originalData, boolean firstTime) throws Exception {
    
    int numAttributes = originalData.numAttributes();
    int classIndex = originalData.classIndex();
    
    // determine attributes that won't be used in the imputation (all missing or all one value or nonnumeric class attribute)
    if (firstTime == true) {
      String unusedAttributeIndices = "";
      m_unusedAtts = new boolean[numAttributes];
      for (int j = 0; j < numAttributes; j++) {
        AttributeStats currentStats = originalData.attributeStats(j);
        if ((classIndex == j && originalData.classAttribute().isNumeric() == false) ||
            currentStats.distinctCount < 2) {
          m_unusedAtts[j] = true;
          unusedAttributeIndices += (j + 2) + ",";
        } else {
          m_unusedAtts[j] = false;
        }
      }
      if (unusedAttributeIndices.contentEquals("") == false) {
        // set up remove filter
        m_unusedAttributeRemover = new Remove();
        m_unusedAttributeRemover.setInvertSelection(false);
        m_unusedAttributeRemover.setAttributeIndices(unusedAttributeIndices);
      }
    }
    
    // preprocess for working with the missing values
    Instances preprocessed = prepareMissing(originalData);
    
    // remove attributes that won't be used
    Instances withClass = new Instances(preprocessed, 0, preprocessed.numInstances()); // to store class values
    if (m_unusedAttributeRemover != null) {
      m_unusedAttributeRemover.setInputFormat(preprocessed);
      preprocessed = Filter.useFilter(preprocessed, m_unusedAttributeRemover);
      preprocessed.setClassIndex(-1);
    }
    
    // if preparing the first time data, set parameters
    if (firstTime == true) {
      m_numAttributes = preprocessed.numAttributes() - 1;
      m_means = new double[m_numAttributes];
      m_stdDevs = new double[m_numAttributes];
      for (int i = 1; i < m_numAttributes + 1; i++) {
        Stats columnStats = preprocessed.attributeStats(i).numericStats;
        columnStats.calculateDerived();
        m_means[i - 1] = columnStats.mean;
        m_stdDevs[i - 1] = columnStats.stdDev;
      }
    }
    
    // standardize data (to avoid rounding errors)
    standardize(preprocessed);
    
    // return array of datasets
    Instances [] data = new Instances[2];
    data[0] = preprocessed;
    data[1] = withClass;
    return data;
  }
  
  /**
   * Postprocesses data to make ready for output
   * @param imputedData   dataset with final (standardized) imputed values
   * @param withClass     dataset without class attribute removed
   * @return withClass    the final imputed, postprocessed data
   * @throws Exception    if processing goes wrong
   */
  private Instances postProcessData(Instances imputedData, Instances withClass) throws Exception {
    
    // remove missingPattern attribute from withClass
    Remove patternRemover = new Remove();
    patternRemover.setInvertSelection(false);
    patternRemover.setAttributeIndices("1");
    patternRemover.setInputFormat(withClass);
    withClass = Filter.useFilter(withClass, patternRemover);
    
    // destandardize imputed values and place in proper spots in data with class
    for (int i = 0; i < withClass.numInstances(); i++) {
      int usedAttributes = 0;
      for (int j = 0; j < withClass.numAttributes(); j++) {
        if (m_unusedAtts[j] == true) {
          continue;
        }
        if (withClass.instance(i).isMissing(j)) {
          withClass.instance(i).setValue(j, imputedData.instance(i).value(usedAttributes + 1) 
                                            * m_stdDevs[usedAttributes] + m_means[usedAttributes]);
        }
        usedAttributes++;
      }
    }
    return withClass;
  }
  
  /**
   * Calculates T_obs (complete data sufficient statistics for the observed values). 
   * @param data        preprocessed dataset with missing values
   * @return t          T_obs
   * @throws Exception  if processing goes wrong
   */
  private Matrix getTObs(Instances data) throws Exception {
    
    int p = m_numAttributes; // number of columns
    
    // matrix of complete-data sufficient statistics for observed values
    Matrix t = new Matrix(p+1, p+1);
    
    // initialize T for observed data (T_obs)
    int currentPatternStart = 0;
    while (currentPatternStart < data.numInstances()) {
      // number of instances in current missingness pattern is held in 1st attribute
      int numInCurrentPattern = (int) data.instance(currentPatternStart).value(0);
      t.set(0, 0, t.get(0,0) + numInCurrentPattern);
      for (int i = 1; i < p + 1; i++) {
        // if current column is not missing in this pattern
        if (!data.instance(currentPatternStart).isMissing(i)) {
          // add values in this column for this pattern
          for (int k = 0; k < numInCurrentPattern; k++) {
            t.set(0, i, t.get(0, i) + data.instance(currentPatternStart + k).value(i));
          }
          // iterate through columns to add appropriate squares and crossproducts
          for (int j = i; j < p + 1; j++) {
            // if this column is also not missing in the current missingness pattern
            if (!data.instance(currentPatternStart).isMissing(j)) {
              for (int k = 0; k < numInCurrentPattern; k++) {
                t.set(i, j, t.get(i, j) + 
                      data.instance(currentPatternStart + k).value(i) * 
                      data.instance(currentPatternStart + k).value(j));
              } // end iterating through instances in currrent missingness pattern
            }
          } // end iterating through column indices (inner loop) 
        }
      } // end iterating through column indices (outer loop)
      
      // move counter to start of next missingness pattern (or end of dataset)
      currentPatternStart += numInCurrentPattern;
    } // end iterating through missingness patterns
    
    // copy to symmetric lower triangular portion
    for (int i = 0; i < p + 1; i++) {
      for (int j = 1; j < p + 1; j++) {
        t.set(j, i, t.get(i, j));
      }
    }
    
    return t;
  }
  
  /**
   * Performs the expectation maximization (EM) algorithm to find the maximum 
   * likelihood estimate (or posterior mode if ridge prior is being used)
   * for the multivariate normal parameters of a dataset with missing values. 
   * @param data          preprocessed dataset with missing values
   * @param t_obs         the complete data sufficient statistics for the observed values
   * @return theta        the maximum likelihood estimate for the parameters of the multivariate normal distribution
   * @throws Exception    if processing goes wrong
   */
  private Matrix EM(Instances data, Matrix t_obs) throws Exception {
    
    int p = m_numAttributes; // number of columns
    Matrix theta = new Matrix(p+1, p+1); // parameter matrix
    
    // if numIterations is -1, change to largest int
    int numIterations = m_numIterations;
    if (numIterations < 0) {
      numIterations = Integer.MAX_VALUE;
    }
    
    // starting theta value (means and variances of each column, correlations left at zero)
    // values are standardized so means are 0 and variances are 1
    theta.set(0, 0, -1);
    for (int i = 1; i < data.numAttributes(); i++) {
      theta.set(0, i, 0); // mu_i
      theta.set(i, 0, 0);
      theta.set(i, i, 1); // sigma_ii
    }
    
    double likelihood = logLikelihood(data, theta);
    double deltaLikelihood = Double.MAX_VALUE;
    for (int i = 0; i < numIterations && deltaLikelihood > m_LogLikelihoodThreshold; i++) {
      theta = doEMIteration(data, theta, t_obs);
      double newLikelihood = logLikelihood(data, theta);
      deltaLikelihood = newLikelihood - likelihood;
      likelihood = newLikelihood;
    }
    
    return theta;
  }
  
  /**
   * Performs one iteration of expectation maximization (EM).
   * @param data          preprocessed dataset with missing values
   * @param theta         a matrix containing the starting estimate of the multivariate normal parameters
   * @param t_obs         the complete data sufficient statistics for the observed values
   * @return theta        the maximum likelihood estimate (or posterior mode for ridge prior) for the parameters of the multivariate normal distribution at the end of the iteration
   * @throws Exception    if arguments are inappropriate or processing goes wrong
   */
  private Matrix doEMIteration(Instances data, Matrix theta, Matrix t_obs) throws Exception {
    
    int p = m_numAttributes; // number of columns
    Matrix t = t_obs.copy();
    
    // go through each pattern
    int currentPatternStart = 0;
    while (currentPatternStart < data.numInstances()) {
      // number of instances in current missingness pattern is held in 1st attribute
      int numInCurrentPattern = (int) data.instance(currentPatternStart).value(0);
      // set up binary flags for if column is observed in this pattern
      boolean [] r = new boolean[p + 1];
      int [] observedColumns = new int[p + 1]; // indices of observed columns
      int [] missingColumns = new int[p + 1]; // indices of missing columns
      int numMissing = 0; // number of missing columns
      int numObserved = 0; // number of observed columns
      for (int l = 1; l < p + 1; l++) {
        if (data.instance(currentPatternStart).isMissing(l)) {
          missingColumns[numMissing++] = l;
        } else {
          observedColumns[numObserved++] = l;
          r[l] = true;
        }
      }
      observedColumns[numObserved] = -1; // list end indicator
      missingColumns[numMissing] = -1; // list end indicator
      
      for (int j = 1; j < p + 1; j++) {
        if (r[j] == true && theta.get(j, j) > 0) {
          theta = swp(theta, j);
        } else if (r[j] == false && theta.get(j, j) < 0) {
          theta = rsw(theta, j);
        }
      }
      
      double [] c = new double[p + 1]; // temporary workspace to hold y_ij^*
      for (int i = 0; i < numInCurrentPattern; i++) {
        
        // calculate y_ij^* values
        for (int jCounter = 0, j = missingColumns[jCounter]; j != -1; 
             jCounter++, j = missingColumns[jCounter]) {
          c[j] = theta.get(0, j);
          
          for (int kCounter = 0, k = observedColumns[kCounter]; k != -1;
               kCounter++, k = observedColumns[kCounter]) {
            c[j] += theta.get(k, j) * data.instance(currentPatternStart + i).value(k);
          }
        }
        
        // update t
        for (int jCounter = 0, j = missingColumns[jCounter]; j != -1; 
             jCounter++, j = missingColumns[jCounter]) {
          t.set(0, j, t.get(0, j) + c[j]);
          t.set(j, 0, t.get(0, j));
          
          for (int kCounter = 0, k = observedColumns[kCounter]; k != -1;
               kCounter++, k = observedColumns[kCounter]) {
            t.set(k, j, t.get(k, j) + c[j] * data.instance(currentPatternStart + i).value(k));
            t.set(j, k, t.get(k, j));
          }
          
          for (int kCounter = 0, k = missingColumns[kCounter]; k != -1;
               kCounter++, k = missingColumns[kCounter]) {
            if (k >= j) {
              t.set(k, j, t.get(k, j) + theta.get(k, j) + c[k] * c[j]);
              t.set(j, k, t.get(k, j));
            }
          }
        }
      }
      
      // move counter to start of next missingness pattern (or end of dataset)
      currentPatternStart += numInCurrentPattern;
    } // end iterating through missingness patterns
    
    // modify complete data sufficient statistics if using ridge prior
    if (m_ridgePrior) {
      double n = data.numInstances();
      double m = m_ridge;
      Matrix deltaInv = Matrix.identity(m_numAttributes, m_numAttributes).times(m_ridge); // delta^(-1)
      // sufficient statistics t1 and t2
      Matrix t1 = t.getMatrix(1, (int)p, 0, 0);
      Matrix t2 = t.getMatrix(1, (int)p, 1, (int)p);
      // modified sufficient statistics t1~ and t2~
      Matrix t1Tilde, t2Tilde;
      
      t1Tilde = t1;
      
      t2Tilde = t2.minus(t1.times(t1.transpose()).times(1.0 / n));
      t2Tilde = t2Tilde.plus(deltaInv).times(n / (n + m + p + 2));
      t2Tilde.plusEquals(t1Tilde.times(t1Tilde.transpose()).times(1.0 / n));
      
      // replace t1 and t2 in t with t1~ and t2~
      t.setMatrix(1, (int)p, 0, 0, t1Tilde); // replacing t1 in first row
      t.setMatrix(0, 0, 1, (int)p, t1Tilde.transpose()); // replacing t1' in first column
      t.setMatrix(1, (int)p, 1, (int)p, t2Tilde); // replacing t2
    }
    
    return swp(t.times(1.0 / data.numInstances()), 0);
  }
  
  /**
   * Calculates the observed data log-likelihood for theta (or observed data log posterior 
   * density if ridge prior is being used).
   * @param data          numeric dataset with instances ordered by missingness pattern and an extra attribute added for keeping track of the patterns
   * @param theta       a matrix containing the multivariate normal parameters
   * @return likelihood the observed data log-likelihood for theta (or log posterior density if using a ridge prior)
   * @throws Exception  if processing goes wrong
   */
  private double logLikelihood(Instances data, Matrix theta) throws Exception {
    
    int p = m_numAttributes; // number of columns
    double likelihood = 0.0; // return value
    double d = 0;
    
    double [] c = new double[p + 1]; // temporary storage for mu
    for (int j = 1; j < p + 1; j++) {
      c[j] = theta.get(0, j);
    }
    
    // matrix for calculating the log posterior density if necessary
    Matrix sigma = theta.getMatrix(1, p, 1, p); // covariance matrix from theta
    
    // go through each pattern
    int currentPatternStart = 0;
    while (currentPatternStart < data.numInstances()) {
      // number of instances in current missingness pattern is held in 1st attribute
      int numInCurrentPattern = (int) data.instance(currentPatternStart).value(0);
      // set up binary flags for if column is observed in this pattern
      boolean [] r = new boolean[p + 1];
      int [] observedColumns = new int[p + 1]; // indices of observed columns
      int [] missingColumns = new int[p + 1]; // indices of missing columns
      int numMissing = 0; // number of missing columns
      int numObserved = 0; // number of observed columns
      for (int l = 1; l < p + 1; l++) {
        if (data.instance(currentPatternStart).isMissing(l)) {
          missingColumns[numMissing++] = l;
        } else {
          observedColumns[numObserved++] = l;
          r[l] = true;
        }
      }
      observedColumns[numObserved] = -1; // list end indicator
      missingColumns[numMissing] = -1; // list end indicator
      
      for (int j = 1; j < p + 1; j++) {
        if (r[j] == true && theta.get(j, j) > 0) {
          d += Math.log(theta.get(j, j));
          theta = swp(theta, j);
        } else if (r[j] == false && theta.get(j, j) < 0) {
          theta = rsw(theta, j);
          d -= Math.log(theta.get(j, j));
        }
      }
      
      Matrix m = new Matrix(p + 1, p + 1);
      for (int i = 0; i < numInCurrentPattern; i++) {
        for (int jCounter = 0, j = observedColumns[jCounter]; j != -1; 
             jCounter++, j = observedColumns[jCounter]) {
          
          for (int kCounter = jCounter, k = observedColumns[kCounter]; k != -1;
               kCounter++, k = observedColumns[kCounter]) {
            m.set(j, k, m.get(j, k) + (data.instance(currentPatternStart + i).value(j) -
                  c[j]) * (data.instance(currentPatternStart + i).value(k) - c[k]));
            m.set(k, j, m.get(j, k));
          }
        }
      }
      
      double t = 0;
      
      for (int jCounter = 0, j = observedColumns[jCounter]; j != -1; 
           jCounter++, j = observedColumns[jCounter]) {
        
        for (int kCounter = 0, k = observedColumns[kCounter]; k != -1;
             kCounter++, k = observedColumns[kCounter]) {
            t -= theta.get(j, k) * m.get(j, k);
        }
      }
      
      //update likelihood
      likelihood -= ((double)numInCurrentPattern * d + t) / 2.0;
      
      // move counter to start of next missingness pattern (or end of dataset)
      currentPatternStart += numInCurrentPattern;
    } // end iterating through missingness patterns
    
    // modify to produce log posterior density if ridge prior is being used
    if (m_ridgePrior) {
      Matrix deltaInv = Matrix.identity(p, p).times(m_ridge); // delta^(-1)
      double m = m_ridge;
      double logPi = 0; // log pi(theta) term to be added to MLE to get log posterior density
      
      Matrix M0 = deltaInv;
      
      // compute log pi(theta)
      logPi = Math.log(Math.abs(sigma.det()));
      logPi *= -0.5 * (m + p + 2);
      logPi -= 0.5 * sigma.inverse().times(M0).trace();
      
      // modify result to give log posterior density
      likelihood += logPi;
    }
    
    return likelihood;
  }
  
  /**
   * Performs the imputation. Draws Y_mis(i+1) as the expected value of (Y_mis | Y_obs, theta(i)).
   * @param data          preprocessed dataset with missing values
   * @param theta         a matrix containing the multivariate normal parameters
   * @throws Exception    if processing goes wrong
   */
  private void impute(Instances data, Matrix theta) throws Exception {
    
    int p = m_numAttributes; // number of columns
    
    // go through each pattern
    int currentPatternStart = 0;
    while (currentPatternStart < data.numInstances()) {
      // number of instances in current missingness pattern is held in 1st attribute
      int numInCurrentPattern = (int) data.instance(currentPatternStart).value(0);
      // set up binary flags for if column is observed in this pattern
      boolean [] r = new boolean[p + 1];
      int [] observedColumns = new int[p + 1]; // indices of observed columns
      int [] missingColumns = new int[p + 1]; // indices of missing columns
      int numMissing = 0; // number of missing columns
      int numObserved = 0; // number of observed columns
      for (int l = 1; l < p + 1; l++) {
        if (data.instance(currentPatternStart).isMissing(l)) {
          missingColumns[numMissing++] = l;
        } else {
          observedColumns[numObserved++] = l;
          r[l] = true;
        }
      }
      observedColumns[numObserved] = -1; // list end indicator
      missingColumns[numMissing] = -1; // list end indicator
      
      for (int j = 1; j < p + 1; j++) {
        if (r[j] == true && theta.get(j, j) > 0) {
          theta = swp(theta, j);
        } else if (r[j] == false && theta.get(j, j) < 0) {
          theta = rsw(theta, j);
        }
      }
      
      for (int i = 0; i < numInCurrentPattern; i++) {
        for (int jCounter = 0, j = missingColumns[jCounter]; j != -1; 
             jCounter++, j = missingColumns[jCounter]) {
          // impute the missing values
          data.instance(currentPatternStart + i).setValue(j, theta.get(0, j));
          for (int kCounter = 0, k = observedColumns[kCounter]; k != -1;
               kCounter++, k = observedColumns[kCounter]) {
            double y_ij = data.instance(currentPatternStart + i).value(j);
            double y_ik = data.instance(currentPatternStart + i).value(k);
            double theta_kj = theta.get(k, j);
            data.instance(currentPatternStart + i).setValue(j, y_ij + theta_kj * y_ik);
          }
        }
      } // end iterating through instances in pattern
      // move counter to start of next missingness pattern (or end of dataset)
      currentPatternStart += numInCurrentPattern;
    } // end iterating through missingness patterns
  }
  
  /**
   * Performs the sweep operation on a matrix at the given position. 
   * @param g           a matrix
   * @param k           the pivot position
   * @param dir         the direction to do the sweep in (1 = normal sweep, -1 = reverse sweep)
   * @return h          the matrix after being swept on position k
   * @throws Exception  if processing goes wrong
   */
  private static Matrix doSweep(Matrix g, int k, int dir) throws Exception {
    
    // number of rows/columns
    int p = g.getRowDimension();
    
    // check if k is in range
    if (k < 0 || k >= p) {
      throw new Exception("Position to be swept on must be within range.");
    }
    
    // check if dir is 1 or -1
    if (dir != 1 && dir != -1) {
      throw new Exception("Sweep direction must be 1 or -1.");
    }
    
    // result matrix
    Matrix h = g.copy();
    
    // check that pivot value is not zero
    double kkValue = g.get(k, k);
    if (kkValue == 0) {
      throw new Exception("Sweep: Division by zero (pivot value).");
    }
    
    // process elements
    for (int i = 0; i < p; i++) {
      for (int j = i; j < p; j++) {
        if (i == k && j == k) { // pivot position
          h.set(i, j, -1.0 / kkValue);
        } else if (i == k || j == k) { // value in row or column k
          h.set(i, j, (double)dir * g.get(i, j) / kkValue);
          h.set(j, i, h.get(i, j)); // copy to symmetric value
        } else {
          h.set(i, j, g.get(i, j) - g.get(i, k) * g.get(k, j) / kkValue);
          h.set(j, i, h.get(i, j)); // copy to symmetric value
        }
      }
    }
    
    return h;
  }
  
  /**
   * Performs the normal sweep operation. 
   * @param g           a matrix
   * @param k           the pivot position
   * @return h          the matrix after being swept on position k
   * @throws Exception  if processing goes wrong
   */
  private static Matrix swp(Matrix g, int k) throws Exception {
    
    try {
      return doSweep(g, k, 1); // call actual sweep function with proper parameters
    } catch (Exception e) {
      throw e;
    }
  }
  
  /**
   * Performs the reverse sweep operation. 
   * @param g           a matrix
   * @param k           the pivot position
   * @return h          the matrix after being swept on position k
   * @throws Exception  if processing goes wrong
   */
  private static Matrix rsw(Matrix g, int k) throws Exception {
    
    try {
      return doSweep(g, k, -1); // call actual sweep function with proper parameters
    } catch (Exception e) {
      throw e;
    }
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
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {
    
    runFilter(new EMImputation(), argv);
  }
}

