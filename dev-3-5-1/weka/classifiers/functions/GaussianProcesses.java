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
 *    LinearRegression.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.functions;

import weka.classifiers.functions.supportVector.*;
import weka.classifiers.Classifier;
import weka.classifiers.IntervalEstimator;
import weka.classifiers.Evaluation;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.Filter;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.core.matrix.*;

/**
 * Class for using Gaussian Processes for prediction. <p>
 *
 * Valid options are:<p>
 *
 *
 * For more information on the Gaussian Processes algorithm, see<p>
 *
 * David J.C. Mackay, <i>Introduction to Gaussian Processes</i>.
 * http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz
 * 
 * @author Kurt Driessens (kurtd@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 *
 */
public class GaussianProcesses extends Classifier implements OptionHandler, IntervalEstimator {

  /** Only numeric attributes in the dataset? */
  protected boolean m_onlyNumeric;

  /** The filter used to make attributes numeric. */
  protected NominalToBinary m_NominalToBinary;
    
  /** The filter to apply to the training data */
  public static final int FILTER_NORMALIZE = 0;
  public static final int FILTER_STANDARDIZE = 1;
  public static final int FILTER_NONE = 2;
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"),
  };
    
  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;
    
  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_NORMALIZE;
  
  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing;
    
  /** Turn off all checks and conversions? Turning them off assumes
      that data is purely numeric, doesn't contain any missing values,
      and has a numeric class. */
  protected boolean m_checksTurnedOff = false;
    
  /** Feature-space normalization? */ 
  protected boolean m_featureSpaceNormalization = false;

  /** Use Polynomial kernel? (default: RBF) */
  protected boolean m_usePoly = false;
    
  /** The size of the cache (a prime number) */
  protected int m_cacheSize = 1;
    
  /** Use lower-order terms? */
  protected boolean m_lowerOrder = false;

  /** The exponent for the polynomial kernel. */
  protected double m_exponent = 1.0;
    
  /** Gamma for the RBF kernel. */
  protected double m_gamma = 1.0;

  /** Gaussian Noise Value. */
  protected double m_delta = 1.0;

  /** The class index from the training data */
  protected int m_classIndex = -1;

  /** The parameters of the linear transforamtion realized 
   * by the filter on the class attribute */
  protected double m_Alin;
  protected double m_Blin;

  /** Kernel to use **/
  protected Kernel m_kernel;

  /** The training data. */
  protected Instances m_data;
    
  /** The training data. */
  protected double m_avg_target;
    
  /** The covariance matrix. */
  protected weka.core.matrix.Matrix m_C;
    
  /** The vector of target values. */
  protected weka.core.matrix.Matrix m_t;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements Gaussian Processes for regression "
	+ "without hyperparameter-tuning. "
	+ "For more information see\n\n"
	+ "David J.C. Mackay, Introduction to Gaussian Processes. " 
	+ "http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz";
  }

  /**
   * Method for building the classifier. 
   *
   * @param insts the set of training instances
   * @exception Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances insts) throws Exception {

    /* check the set of training instances */

    if (!m_checksTurnedOff) {
      if (insts.checkForStringAttributes()) {
	throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
      }
      if (insts.classAttribute().isNominal()) {
	throw new UnsupportedClassTypeException("This implementation of Gaussian Processes can't handle a nominal class!");
      }
      insts = new Instances(insts);
      insts.deleteWithMissingClass();
      if (insts.numInstances() == 0) {
	throw new Exception("No training instances without a missing class!");
      }
    }
      
    m_onlyNumeric = true;
    if (!m_checksTurnedOff) {
      for (int i = 0; i < insts.numAttributes(); i++) {
	if (i != insts.classIndex()) {
	  if (!insts.attribute(i).isNumeric()) {
	    m_onlyNumeric = false;
	    break;
	  }
	}
      }
    }

    if (!m_checksTurnedOff) {
      m_Missing = new ReplaceMissingValues();
      m_Missing.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Missing); 
    } else {
      m_Missing = null;
    }

    if (!m_onlyNumeric) {
      m_NominalToBinary = new NominalToBinary();
      m_NominalToBinary.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_NominalToBinary);
    } else {
      m_NominalToBinary = null;
    }

    m_classIndex = insts.classIndex();
    if (m_filterType == FILTER_STANDARDIZE) {
      m_Filter = new Standardize();
      //((Standardize)m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter); 
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
      //((Normalize)m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter); 
    } else {
      m_Filter = null;
    }

    m_data = insts;

    // determine which linear transformation has been 
    // applied to the class by the filter
    if (m_Filter != null) {
      Instance witness = (Instance)insts.instance(0).copy();
      witness.setValue(m_classIndex, 0);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      Instance res = m_Filter.output();
      m_Blin = res.value(m_classIndex);
      witness.setValue(m_classIndex, 1);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      res = m_Filter.output();
      m_Alin = res.value(m_classIndex) - m_Blin;
    } else {
      m_Alin = 1.0;
      m_Blin = 0.0;
    }

    // Initialize kernel
    if(!m_usePoly) { 
      m_kernel = new RBFKernel(m_data, m_cacheSize, m_gamma);
    } else {
      if (m_featureSpaceNormalization) {
	m_kernel = new NormalizedPolyKernel(m_data, m_cacheSize, m_exponent, m_lowerOrder);
      } else {
	m_kernel = new PolyKernel(m_data, m_cacheSize, m_exponent, m_lowerOrder);
      }
    }

    // Build Inverted Covariance Matrix

    m_C = new weka.core.matrix.Matrix(m_data.numInstances(),m_data.numInstances());
    double kv;
    double sum = 0.0;

    for (int i = 0; i < m_data.numInstances(); i++) {
	sum += m_data.instance(i).classValue();
      for (int j = 0; j < i; j++) {
	kv = m_kernel.eval(i,j,m_data.instance(i));
	m_C.set(i,j,kv);
	m_C.set(j,i,kv);
      }
      kv = m_kernel.eval(i,i,m_data.instance(i));
      m_C.set(i,i,kv+(m_delta*m_delta));
    }

    m_avg_target = sum/m_data.numInstances();

    //weka.core.matrix.CholeskyDecomposition cd = new weka.core.matrix.CholeskyDecomposition(m_C);

    //if (!cd.isSPD())
    //throw new Exception("No semi-positive-definite kernel?!?");

    weka.core.matrix.LUDecomposition lu = new weka.core.matrix.LUDecomposition(m_C);
    if (!lu.isNonsingular())
	throw new Exception("Singular Matrix?!?");

    weka.core.matrix.Matrix iMat = weka.core.matrix.Matrix.identity(m_data.numInstances(),m_data.numInstances());

    m_C = lu.solve(iMat);

    m_t = new weka.core.matrix.Matrix(m_data.numInstances(),1);

    for (int i = 0; i < m_data.numInstances(); i++) 
	m_t.set(i,0,m_data.instance(i).classValue()-m_avg_target);

    m_t = m_C.times(m_t);

  }

  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   * @return the classification
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance inst) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (!m_onlyNumeric) {
      m_NominalToBinary.input(inst);
      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    // Build K vector

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_data.numInstances(),1);
    for (int i = 0; i < m_data.numInstances(); i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double result = k.transpose().times(m_t).get(0,0)+m_avg_target;

    return result;

  }

  /**
   * Predicts a confidence interval for the given instance and confidence level.
   *
   * @param inst the instance to make the prediction for
   * @param confidenceLevel the percentage of cases the interval should cover
   * @return a 1*2 array that contains the boundaries of the interval
   * @exception Exception if interval could not be estimated
   * successfully
   */
  public double[][] predictInterval(Instance inst, double confidenceLevel) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (!m_onlyNumeric) {
      m_NominalToBinary.input(inst);
      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    // Build K vector (and Kappa)

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_data.numInstances(),1);
    for (int i = 0; i < m_data.numInstances(); i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double kappa = m_kernel.eval(-1,-1,inst) + m_delta*m_delta;
      
    double estimate = k.transpose().times(m_t).get(0,0)+m_avg_target;

    double sigma = Math.sqrt(kappa - k.transpose().times(m_C).times(k).get(0,0));

    confidenceLevel = 1.0 - ((1.0 - confidenceLevel)/2.0);

    double z = Statistics.normalInverse(confidenceLevel);
    
    double[][] interval = new double[1][2];

    interval[0][0] = estimate - z * sigma;
    interval[0][1] = estimate + z * sigma;

    return interval;
    
  }
  
  /**
   * Gives the variance of the prediction at the given instance
   *
   * @param inst the instance to get the variance for
   * @return tha variance
   */
    public double getStandardDeviation(Instance inst) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (!m_onlyNumeric) {
      m_NominalToBinary.input(inst);m_Alin = 1.0;
      m_Blin = 0.0;

      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_data.numInstances(),1);
    for (int i = 0; i < m_data.numInstances(); i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double kappa = m_kernel.eval(-1,-1,inst) + m_delta*m_delta;
    
    double var = kappa - k.transpose().times(m_C).times(k).get(0,0);

    if (var < 0) System.out.println("Aiaiai: variance is negative (" + var + ")!!!");
  
    double sigma = Math.sqrt(var);

    return sigma;
    }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
	
    Vector newVector = new Vector(6);

    newVector.addElement(new Option("\tThe exponent for the "
				    + "polynomial kernel. (default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tGamma for the "
				    + "RBF kernel. (default 0.01)",
				    "G", 1, "-G <double>"));
    newVector.addElement(new Option("\tLevel of Gaussian Noise."
				    + " (default 0.1)",
				    "L", 1, "-L <double>"));
    newVector.addElement(new Option("\tWhether to 0=normalize/1=standardize/2=neither. " +
				    "(default 0=normalize)",
				    "N", 1, "-N"));
    newVector.addElement(new Option("\tFeature-space normalization (only for\n"
				    +"\tnon-linear polynomial kernels).",
				    "F", 0, "-F"));
    newVector.addElement(new Option("\tUse lower-order terms (only for non-linear\n"
				    +"\tpolynomial kernels).",
				    "O", 0, "-O"));
    newVector.addElement(new Option("\tUse Polynomial kernel. " +
				    "(default false)",
				    "P", 0, "-P"));
    return newVector.elements();
  }
    
    
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -E num <br>
   * The exponent for the polynomial kernel. (default 1) <p>
   *
   * -G num <br>
   * Gamma for the RBF kernel. (default 1.0) <p>
   *
   * -L num <br>
   * Gaussian Noise level. (default 1.0) <p>
   *
   * -N <0|1|2> <br>
   * Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)<p>
   *
   * -F <br>
   * Feature-space normalization (only for non-linear polynomial kernels). <p>
   *
   * -O <br>
   * Use lower-order terms (only for non-linear polynomial kernels). <p>
   *
   * -P <br>
   * Use Polynomial kernel (default false). <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    String exponentsString = Utils.getOption('E', options);
    if (exponentsString.length() != 0) {
      m_exponent = (new Double(exponentsString)).doubleValue();
    } else {
      m_exponent = 1.0;
    }
    String gammaString = Utils.getOption('G', options);
    if (gammaString.length() != 0) {
      m_gamma = (new Double(gammaString)).doubleValue();
    } else {
      m_gamma = 1.0;
    }
    String noiseString = Utils.getOption('L', options);
    if (noiseString.length() != 0) {
      m_delta = (new Double(noiseString)).doubleValue();
    } else {
      m_delta = 1.0;
    }
    m_usePoly = Utils.getFlag('P', options);
    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));
    }
    m_featureSpaceNormalization = Utils.getFlag('F', options);
    if ((!m_usePoly) && (m_featureSpaceNormalization)) {
      throw new Exception("RBF machine doesn't require feature-space normalization.");
    }
    if ((m_exponent == 1.0) && (m_featureSpaceNormalization)) {
      throw new Exception("Can't use feature-space normalization with linear kernel.");
    }
    m_lowerOrder = Utils.getFlag('O', options);
    if ((!m_usePoly) && (m_lowerOrder)) {
      throw new Exception("Can't use lower-order terms with RBF kernel.");
    }
    if ((m_exponent == 1.0) && (m_lowerOrder)) {
      throw new Exception("Can't use lower-order terms with linear kernel.");
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    Vector options = new Vector();

    options.add("-E"); options.add("" + m_exponent);
    options.add("-G"); options.add("" + m_gamma);
    options.add("-L"); options.add("" + m_delta);
    options.add("-N"); options.add("" + m_filterType);
    if (m_featureSpaceNormalization) {
      options.add("-F");
    }
    if (m_lowerOrder) {
      options.add("-O");
    }
    if (m_usePoly) {
      options.add("-P");
    }
    return (String[])options.toArray(new String[options.size()]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "Determines how/if the data will be transformed.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.2200Instances
   *
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {
	
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

    
  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {
	
    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String exponentTipText() {
    return "The exponent for the polynomial kernel.";
  }
  
  /**
   * Get the value of exponent. 
   *
   * @return Value of exponent.
   */
  public double getExponent() {
    
    return m_exponent;
  }

  /**
   * Set the value of exponent. If linear kernel
   * is used, rescaling and lower-order terms are
   * turned off.
   *
   * @param v  Value to assign to exponent.
   */
  public void setExponent(double v) {
    
    if (v == 1.0) {
      m_featureSpaceNormalization = false;
      m_lowerOrder = false;
    }
    m_exponent = v;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String gammaTipText() {
    return "The value of the gamma parameter for RBF kernels.";
  }
  
  /**
   * Get the value of gamma. 
   *
   * @return Value of gamma.
   */
  public double getGamma() {
    
    return m_gamma;
  }
  
  /**
   * Set the value of gamma. 
   *
   * @param v  Value to assign to gamma.
   */
  public void setGamma(double v) {
    
    m_gamma = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String noiseTipText() {
    return "The level of Gaussian Noise (added to the diagonal of the Covariance Matrix).";
  }
  
  /**
   * Get the value of noise. 
   *
   * @return Value of noise.
   */
  public double getNoise() {
    
    return m_delta;
  }
  
  /**
   * Set the level of Gaussian Noise. 
   *
   * @param v  Value to assign to noise.
   */
  public void setNoise(double v) {
    
    m_delta = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String usePolyTipText() {
    return "Whether to use an Polynomial kernel instead of an RBF one.";
  }
  
  /**
   * Check if the Polynomial kernel is to be used.
   * @return true if Poly
   */
  public boolean getUsePoly() {
    
    return m_usePoly;
  }

  /**
   * Set if the Polynomial kernel is to be used.
   * @param v  true if Poly
   */
  public void setUsePoly(boolean v) {

    if (!v) {
      m_featureSpaceNormalization = false;
      m_lowerOrder = false;
    }
    m_usePoly = v;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String featureSpaceNormalizationTipText() {
    return "Whether feature-space normalization is performed (only "
      + "available for non-linear polynomial kernels).";
  }
  
  /**
   * Check whether feature spaces is being normalized.
   * @return true if feature space is normalized.
   */
  public boolean getFeatureSpaceNormalization() throws Exception {

    return m_featureSpaceNormalization;
  }

  /**
   * Set whether feature space is normalized.
   * @param v  true if feature space is to be normalized.
   */
  public void setFeatureSpaceNormalization(boolean v) throws Exception {
    
    if ((!m_usePoly) || (m_exponent == 1.0)) {
      m_featureSpaceNormalization = false;
    } else {
      m_featureSpaceNormalization = v;
    }
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lowerOrderTermsTipText() {
    return "Whether lower order polyomials are also used (only "
      + "available for non-linear polynomial kernels).";
  }

  /**
   * Check whether lower-order terms are being used.
   * @return Value of lowerOrder.
   */
  public boolean getLowerOrderTerms() {
    
    return m_lowerOrder;
  }

  /**
   * Set whether lower-order terms are to be used. Defaults
   * to false if a linear machine is built.
   * @param v  Value to assign to lowerOrder.
   */
  public void setLowerOrderTerms(boolean v) {
    
    if (m_exponent == 1.0 || (!m_usePoly)) {
      m_lowerOrder = false;
    } else {
      m_lowerOrder = v;
    }
  }


  /**
   * Prints out the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_t == null)
      return "Gaussian Processes: No model built yet.";

    try {

      text.append("Gaussian Processes\n\n");

      text.append("Kernel used : \n");
      if(!m_usePoly) {
	text.append("  RBF kernel : K(x,y) = e^-(" + m_gamma + "* <x-y,x-y>^2)");
      } else if (m_exponent == 1){
	text.append("  Linear Kernel : K(x,y) = <x,y>");
      } else {
	if (m_featureSpaceNormalization) {
	  if (m_lowerOrder){
	    text.append("  Normalized Poly Kernel with lower order : K(x,y) = (<x,y>+1)^" + m_exponent + "/" + 
			"((<x,x>+1)^" + m_exponent + "*" + "(<y,y>+1)^" + m_exponent + ")^(1/2)");		    
	  } else {
	    text.append("  Normalized Poly Kernel : K(x,y) = <x,y>^" + m_exponent + "/" + "(<x,x>^" + 
			m_exponent + "*" + "<y,y>^" + m_exponent + ")^(1/2)");
	  }
	} else {
	  if (m_lowerOrder){
	    text.append("  Poly Kernel with lower order : K(x,y) = (<x,y> + 1)^" + m_exponent);
	  } else {
	    text.append("  Poly Kernel : K(x,y) = <x,y>^" + m_exponent);		
	  }
	}
      }
      text.append("\n\n");

      text.append("Average Target Value : " + m_avg_target + "\n");

      text.append("Inverted Covariance Matrix:\n");
      double min = m_C.get(0,0);
      double max = m_C.get(0,0);
      for (int i = 0; i < m_data.numInstances(); i++)
	for (int j = 0; j < m_data.numInstances(); j++) {
	    if (m_C.get(i,j) < min) min = m_C.get(i,j);
	    else if (m_C.get(i,j) > max) max = m_C.get(i,j);
	}
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n");
      text.append("Inverted Covariance Matrix * Target-value Vector:\n");
      min = m_t.get(0,0);
      max = m_t.get(0,0);
      for (int i = 0; i < m_data.numInstances(); i++) {
	    if (m_t.get(i,0) < min) min = m_t.get(i,0);
	    else if (m_t.get(i,0) > max) max = m_t.get(i,0);
	}
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n \n");   
      
    } catch (Exception e) {
      return "Can't print the classifier.";
    }

    return text.toString();
  }
 
 /**
   * Main method for testing this class.
   */
  public static void main(String[] argv) {
	
    Classifier scheme;
    try {
      scheme = new GaussianProcesses();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
 

}
