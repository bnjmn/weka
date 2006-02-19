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
 *    SMOreg.java
 *    Copyright (C) 2002 Sylvain Roy
 *
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.NormalizedPolyKernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.functions.supportVector.SMOset;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Implements Alex J.Smola and Bernhard Scholkopf sequential minimal optimization
 * algorithm for training a support vector regression using polynomial
 * or RBF kernels. 
 *
 * This implementation globally replaces all missing values and
 * transforms nominal attributes into binary ones. It also
 * normalizes all attributes by default. (Note that the coefficients
 * in the output are based on the normalized/standardized data, not the
 * original data.)
 *
 *
 * For more information on the SMO algorithm, see<p>
 *
 * Alex J. Smola, Bernhard Scholkopf (1998). <i>A Tutorial on Support Vector Regression</i>. 
 * NeuroCOLT2 Technical Report Series - NC2-TR-1998-030. <p>
 *
 * S.K. Shevade, S.S. Keerthi, C. Bhattacharyya, K.R.K. Murthy, 
 * <i>Improvements to SMO Algorithm for SVM Regression</i>. 
 * Technical Report CD-99-16, Control Division Dept of Mechanical and Production Engineering, 
 * National University of Singapore.<p>
 *
 *
 * @author Sylvain Roy (sro33@student.canterbury.ac.nz)
 * @version $Revision: 1.9 $
 */
public class SMOreg extends Classifier implements OptionHandler, 
  WeightedInstancesHandler {
    
  static final long serialVersionUID = 5783729368717679645L;

  /** Feature-space normalization? */
  protected boolean m_featureSpaceNormalization = false;
  
  /** Use RBF kernel? (default: poly) */
  protected boolean m_useRBF = false;
    
  /** The size of the cache (a prime number) */
  protected int m_cacheSize = 250007;
    
  /** Kernel to use **/
  protected Kernel m_kernel;

  /** Use lower-order terms? */
  protected boolean m_lowerOrder = false;

  /** The exponent for the polynomial kernel. */
  protected double m_exponent = 1.0;
    
  /** Gamma for the RBF kernel. */
  protected double m_gamma = 0.01;

  /** The class index from the training data */
  protected int m_classIndex = -1;

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
      and has a numeric class. Turning them off also means that
      no header information will be stored if the machine is linear. 
      Finally, it also assumes that no instance has a weight equal to 0.*/
  protected boolean m_checksTurnedOff = false;
    
  /** The training data. */
  protected Instances m_data;
    
  /** The complexity parameter */
  protected double m_C = 1.0;

  /** The Lagrange multipliers */
  protected double[] m_alpha;
  protected double[] m_alpha_;

  /** The thresholds. */
  protected double m_b, m_bLow, m_bUp;

  /** The indices for m_bLow and m_bUp */
  protected int m_iLow, m_iUp;

  /** Weight vector for linear machine. */
  protected double[] m_weights;

  /** The current set of errors for all non-bound examples. */
  protected double[] m_fcache;

  /** The five different sets used by the algorithm. */
  protected SMOset m_I0; // {i: 0 < m_alpha[i] < C || 0 < m_alpha_[i] < C}
  protected SMOset m_I1; // {i: m_class[i] = 0, m_alpha_[i] = 0}
  protected SMOset m_I2; // {i: m_class[i] = 0, m_alpha_[i] = C}
  protected SMOset m_I3; // {i: m_class[i] = C, m_alpha_[i] = 0}

  /** The parameter epsilon */
  protected double m_epsilon = 1e-3;

  /** The parameter tol */
  protected double m_tol = 1.0e-3;

  /** The parameter eps */
  protected double m_eps = 1.0e-12;

  /** Precision constant for updating sets */
  protected static double m_Del = 1e-10;
    
  /** The parameters of the linear transforamtion realized 
   * by the filter on the class attribute */
  protected double m_Alin;
  protected double m_Blin;

  /** Variables to hold weight vector in sparse form.
      (To reduce storage requirements.) */
  protected double[] m_sparseWeights;
  protected int[] m_sparseIndices;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements Alex Smola and Bernhard Scholkopf's sequential minimal "
      + "optimization algorithm for training a support vector regression model. "
      + "This implementation globally replaces all missing values and "
      + "transforms nominal attributes into binary ones. It also "
      + "normalizes all attributes by default. (Note that the coefficients "
      + "in the output are based on the normalized/standardized data, not the "
      + "original data.) "
      + "For more information on the SMO algorithm, see\n\n"
      + "Alex J. Smola, Bernhard Scholkopf (1998). \"A Tutorial on Support Vector "
      + "Regression\".  NeuroCOLT2 Technical Report Series - NC2-TR-1998-030.\n\n"
      + "S.K. Shevade, S.S. Keerthi, C. Bhattacharyya, K.R.K. Murthy,  "
      + "\"Improvements to SMO Algorithm for SVM Regression\".  "
      + "Technical Report CD-99-16, Control Division Dept of Mechanical and "
      + "Production Engineering, National University of Singapore. ";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
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
      // can classifier handle the data?
      getCapabilities().testWithFail(insts);

      // remove instances with missing class
      insts = new Instances(insts);
      insts.deleteWithMissingClass();
	
      /* Removes all the instances with weight equal to 0.
       MUST be done since condition (6) of Shevade's paper 
       is made with the assertion Ci > 0 (See equation (1a). */
      Instances data = new Instances(insts, insts.numInstances());
      for(int i = 0; i < insts.numInstances(); i++){
        if(insts.instance(i).weight() > 0)
          data.add(insts.instance(i));
      }
      if (data.numInstances() == 0) {
        throw new Exception("No training instances left after removing " + 
        "instances with weight 0!");
      }
      insts = data;
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
      ((Standardize)m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter); 
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
      ((Normalize)m_Filter).setIgnoreClass(true);
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
    if(m_useRBF) {
      m_kernel = new RBFKernel(m_data, m_cacheSize, m_gamma);
    } else {
      if (m_featureSpaceNormalization) {
	m_kernel = new NormalizedPolyKernel(m_data, m_cacheSize, m_exponent, m_lowerOrder);
      } else {
	m_kernel = new PolyKernel(m_data, m_cacheSize, m_exponent, m_lowerOrder);
      }
    }
	
    // If machine is linear, reserve space for weights
    if (!m_useRBF && m_exponent == 1.0) {
      m_weights = new double[m_data.numAttributes()];
    } else {
      m_weights = null;
    }

    // Initialize fcache
    m_fcache = new double[m_data.numInstances()];

    // Initialize sets
    m_I0 = new SMOset(m_data.numInstances());
    m_I1 = new SMOset(m_data.numInstances());
    m_I2 = new SMOset(m_data.numInstances());
    m_I3 = new SMOset(m_data.numInstances());


    /* MAIN ROUTINE FOR MODIFICATION 1 */
    // Follows the specification of the first modification of Shevade's paper 
		
    // Initialize alpha array to zero
    m_alpha = new double[m_data.numInstances()];
    m_alpha_ = new double[m_data.numInstances()];
	
    // set I1 to contain all the examples
    for(int i = 0; i < m_data.numInstances(); i++){
      m_I1.insert(i);
    }
	
    // choose any example i from the training set : i = 0 
    m_bUp = m_data.instance(0).classValue() + m_epsilon;
    m_bLow = m_data.instance(0).classValue() - m_epsilon;
    m_iUp = m_iLow = 0;
	
    int numChanged = 0;
    boolean examineAll = true;
    while(numChanged > 0 || examineAll){
      numChanged = 0;
      if(examineAll){
	// loop over all the example
	for(int I = 0; I < m_alpha.length; I++){
	  numChanged += examineExample(I);
	}
      } else {
	// loop over I_0
	for (int I = m_I0.getNext(-1); I != -1; I = m_I0.getNext(I)) {
	  numChanged += examineExample(I);
	  if(m_bUp > m_bLow - 2 * m_tol){
	    numChanged = 0;
	    break;
	  }
	}
      }
      if (examineAll)
	examineAll = false;
      else if (numChanged == 0)
	examineAll = true;
    }
		
    /* END OF MAIN ROUTINE FOR MODIFICATION 1 */

    // debuggage
    // checkOptimality();
    // checkAlphas();
    // displayB();

    // Set threshold
    m_b = (m_bLow + m_bUp) / 2.0;
	
    // Save memory
    m_kernel.clean(); m_fcache = null; 
    m_I0 = m_I1 = m_I2 = m_I3 = null;
	
    // If machine is linear, delete training data
    // and store weight vector in sparse format
    if (!m_useRBF && m_exponent == 1.0) {
	    
      // compute weight vector
      for(int j = 0; j < m_weights.length; j++){
	m_weights[j] = 0;
      }
      for(int k = 0; k < m_alpha.length; k++){
	for(int j = 0; j < m_weights.length; j++){
	  if(j == m_data.classIndex())
	    continue;
	  m_weights[j] += (m_alpha[k] - m_alpha_[k]) * m_data.instance(k).value(j);
	}
      }

      // Convert weight vector
      double[] sparseWeights = new double[m_weights.length];
      int[] sparseIndices = new int[m_weights.length];
      int counter = 0;
      for (int ii = 0; ii < m_weights.length; ii++) {
	if (m_weights[ii] != 0.0) {
	  sparseWeights[counter] = m_weights[ii];
	  sparseIndices[counter] = ii;
	  counter++;
	}
      }
      m_sparseWeights = new double[counter];
      m_sparseIndices = new int[counter];
      System.arraycopy(sparseWeights, 0, m_sparseWeights, 0, counter);
      System.arraycopy(sparseIndices, 0, m_sparseIndices, 0, counter);

      // Clean out training data
      if (!m_checksTurnedOff) {
      	m_data = new Instances(m_data, 0);
      } else {
      	m_data = null;
      }
	    
      // Clean out weight vector
      m_weights = null;
	    
      // We don't need the alphas in the linear case
      m_alpha = null;
      m_alpha_ = null;
    }
  }

  /**
   * Examines instance. (As defined in Shevade's paper.)
   *
   * @param i2 index of instance to examine
   * @return true if examination was successfull
   * @exception Exception if something goes wrong
   */    
  protected int examineExample(int i2) throws Exception{

    // Lagrange multipliers for i2
    double alpha2 = m_alpha[i2];
    double alpha2_ = m_alpha_[i2];
	
    double F2 = 0;
    if(m_I0.contains(i2)){
      F2 = m_fcache[i2];
    } else {
      // compute F2 = F_i2 and set f-cache[i2] = F2
      F2 = m_data.instance(i2).classValue();
      for(int j = 0; j < m_alpha.length; j++){
	F2 -= (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i2, j, m_data.instance(i2));
      }
      m_fcache[i2] = F2;

      // Update (b_low, i_low) or (b_up, i_up) using (F2, i2)...
      if(m_I1.contains(i2)){
	if(F2 + m_epsilon < m_bUp){
	  m_bUp = F2 + m_epsilon;
	  m_iUp = i2;
	} else if (F2 - m_epsilon > m_bLow){
	  m_bLow = F2 - m_epsilon;
	  m_iLow = i2;
	}
      } else if(m_I2.contains(i2) && (F2+m_epsilon > m_bLow)){
	m_bLow = F2 + m_epsilon;
	m_iLow = i2;
      } else if(m_I3.contains(i2) && (F2-m_epsilon < m_bUp)){
	m_bUp = F2 - m_epsilon;
	m_iUp = i2;
      }
    }

    // check optimality using current b_low and b_up and, if 
    // violated, find an index i1 to do joint optimization with i2...
    boolean optimality = true;
    int i1 = -1;
	
    // case 1 : i2 is in I_0a
    if(m_I0.contains(i2) && 0 < alpha2 && alpha2 < m_C * m_data.instance(i2).weight()){
      if(m_bLow-(F2-m_epsilon) > 2 * m_tol){
	optimality = false;
	i1 = m_iLow;
	// for i2 in I_0a choose the better i1
	if((F2 - m_epsilon) - m_bUp > m_bLow - (F2 - m_epsilon)){
	  i1 = m_iUp;
	}
      }else if((F2 - m_epsilon)-m_bUp > 2 * m_tol){
	optimality = false;
	i1 = m_iUp;
	// for i2 in I_0a choose the better i1...
	if(m_bLow-(F2-m_epsilon) > (F2-m_epsilon)-m_bUp){
	  i1 = m_iLow;
	}
      }   
    } 
	
    // case 2 : i2 is in I_0b	
    else if(m_I0.contains(i2) && 0 < alpha2_ && alpha2_ < m_C * m_data.instance(i2).weight()){
      if(m_bLow-(F2+m_epsilon) > 2 * m_tol){
	optimality = false;
	i1 = m_iLow;
	// for i2 in I_0b choose the better i1
	if((F2 + m_epsilon) - m_bUp > m_bLow - (F2 + m_epsilon)){
	  i1 = m_iUp;
	}
      }else if((F2 + m_epsilon)-m_bUp > 2 * m_tol){
	optimality = false;
	i1 = m_iUp;
	// for i2 in I_0b choose the better i1...
	if(m_bLow-(F2+m_epsilon) > (F2+m_epsilon)-m_bUp){
	  i1 = m_iLow;
	}
      }   
    }
	
    // case 3 : i2 is in I_1	
    else if(m_I1.contains(i2)){
      if(m_bLow-(F2+m_epsilon) > 2 * m_tol){
	optimality = false;
	i1 = m_iLow;
	// for i2 in I_1 choose the better i1
	if((F2 + m_epsilon) - m_bUp > m_bLow - (F2 + m_epsilon)){
	  i1 = m_iUp;
	}
      } else if((F2 - m_epsilon)-m_bUp > 2 * m_tol){
	optimality = false;
	i1 = m_iUp;
	// for i2 in I_1 choose the better i1...
	if(m_bLow-(F2-m_epsilon) > (F2-m_epsilon)-m_bUp){
	  i1 = m_iLow;
	}
      }   
    }

    // case 4 : i2 is in I_2	
    else if(m_I2.contains(i2)){
      if((F2+m_epsilon)-m_bUp > 2 * m_tol){
	optimality = false;
	i1 = m_iUp;
      }
    }
	
    // case 5 : i2 is in I_3	
    else if(m_I3.contains(i2)){
      if(m_bLow-(F2-m_epsilon) > 2 * m_tol){
	optimality = false;
	i1 = m_iLow;
      }
    }

    else{
      throw new RuntimeException("Inconsistent state ! I0, I1, I2 and I3 " +
				 "must cover the whole set of indices.");
    }

    if(optimality){
      return 0;
    }

    if(takeStep(i1, i2)){
      return 1;
    } else {
      return 0;
    }
  }


  /**
   * Method solving for the Lagrange multipliers for
   * two instances. (As defined in Shevade's paper.)
   *
   * @param i1 index of the first instance
   * @param i2 index of the second instance
   * @return true if multipliers could be found
   * @exception Exception if something goes wrong
   */
  protected boolean takeStep(int i1, int i2) throws Exception{

    if(i1 == i2){
      return false;
    }

    double alpha1 = m_alpha[i1];
    double alpha1_ = m_alpha_[i1];
    double alpha2 = m_alpha[i2];
    double alpha2_ = m_alpha_[i2];

    double F1 = m_fcache[i1];
    double F2 = m_fcache[i2];

    double k11 = m_kernel.eval(i1, i1, m_data.instance(i1));
    double k12 = m_kernel.eval(i1, i2, m_data.instance(i1));
    double k22 = m_kernel.eval(i2, i2, m_data.instance(i2));	
    double eta = -2*k12+k11+k22;
    double gamma = alpha1 - alpha1_ + alpha2 - alpha2_;

    // In case of numerical instabilies eta might be sligthly less than 0.
    // (Theoretically, it cannot happen with a kernel which respects Mercer's condition.)
    if(eta < 0)	    
      eta = 0;
	
    boolean case1 = false, case2 = false, case3 = false, case4 = false, finished = false;
    double deltaphi = F1 - F2;
    double L, H;
    boolean changed = false;
    double a1, a2;
	
    while(!finished){
	    
      // this loop is passed at most three times
      // Case variables needed to avoid attempting small changes twices

      if(!case1 && 
	 (alpha1 > 0 || (alpha1_ == 0 && deltaphi > 0)) &&
	 (alpha2 > 0 || (alpha2_ == 0 && deltaphi < 0)) ){

	// compute L, H (wrt alpha1, alpha2)
	L = java.lang.Math.max(0, gamma - m_C * m_data.instance(i1).weight());
	H = java.lang.Math.min(m_C * m_data.instance(i2).weight(), gamma);

	if(L < H){
	  if(eta > 0){
	    a2 = alpha2 - (deltaphi / eta);
	    if(a2 > H) a2 = H;
	    else if(a2 < L) a2 = L;
	  } else {
	    double Lobj = -L*deltaphi;
	    double Hobj = -H*deltaphi;
	    if(Lobj > Hobj)
	      a2 = L;
	    else
	      a2 = H;
	  }
	  a1 = alpha1 - (a2 - alpha2);
	  // update alpha1, alpha2 if change is larger than some eps
	  if(java.lang.Math.abs(a1-alpha1) > m_eps || 
	     java.lang.Math.abs(a2-alpha2) > m_eps){
	    alpha1 = a1;
	    alpha2 = a2;
	    changed = true;
	  }
	} else {
	  finished = true;
	}
	case1 = true;
	    
      } else if(!case2 && 
		(alpha1 > 0 || (alpha1_ == 0 && deltaphi > 2 * m_epsilon)) &&
		(alpha2_ > 0 || (alpha2 == 0 && deltaphi > 2 * m_epsilon)) ){
		
	// compute L, H (wrt alpha1, alpha2_)
	L = java.lang.Math.max(0, -gamma);
	H = java.lang.Math.min(m_C * m_data.instance(i2).weight(), -gamma + m_C*m_data.instance(i1).weight());

	if(L < H){
	  if(eta > 0){
	    a2 = alpha2_ + ((deltaphi - 2*m_epsilon) / eta);
	    if(a2 > H) a2 = H;
	    else if(a2 < L) a2 = L;
	  } else {
	    double Lobj = L*(-2*m_epsilon + deltaphi);
	    double Hobj = H*(-2*m_epsilon + deltaphi);
	    if(Lobj > Hobj)
	      a2 = L;
	    else
	      a2 = H;
	  }
	  a1 = alpha1 + (a2 - alpha2_);
	  // update alpha1, alpha2_ if change is larger than some eps
	  if(java.lang.Math.abs(a1-alpha1) > m_eps || 
	     java.lang.Math.abs(a2-alpha2_) > m_eps){
	    alpha1 = a1;
	    alpha2_ = a2;
	    changed = true;
	  }
	} else {
	  finished = true;
	}
	case2 = true;

      } else if(!case3 && 
		(alpha1_ > 0 || (alpha1 == 0 && deltaphi < -2 * m_epsilon)) &&
		(alpha2 > 0 || (alpha2_ == 0 && deltaphi < -2 * m_epsilon)) ){
		
	// compute L, H (wrt alpha1_, alpha2)
	L = java.lang.Math.max(0, gamma);
	H = java.lang.Math.min(m_C * m_data.instance(i2).weight(), m_C * m_data.instance(i1).weight() + gamma);

	if(L < H){
	  if(eta > 0){
	    a2 = alpha2 - ((deltaphi + 2*m_epsilon) / eta);
	    if(a2 > H) a2 = H;
	    else if(a2 < L) a2 = L;
	  } else {
	    double Lobj = -L*(2*m_epsilon + deltaphi);
	    double Hobj = -H*(2*m_epsilon + deltaphi);
	    if(Lobj > Hobj)
	      a2 = L;
	    else
	      a2 = H;
	  }
	  a1 = alpha1_ + (a2 - alpha2);
	  // update alpha1_, alpha2 if change is larger than some eps
	  if(java.lang.Math.abs(a1-alpha1_) > m_eps || 
	     java.lang.Math.abs(a2-alpha2) > m_eps){
	    alpha1_ = a1;
	    alpha2 = a2;
	    changed = true;
	  }
	} else {
	  finished = true;
	}
	case3 = true;

      } else if(!case4 && 
		(alpha1_ > 0 || (alpha1 == 0 && deltaphi < 0)) &&
		(alpha2_ > 0 || (alpha2 == 0 && deltaphi > 0)) ){
		
	// compute L, H (wrt alpha1_, alpha2_)
	L = java.lang.Math.max(0, -gamma - m_C * m_data.instance(i1).weight());
	H = java.lang.Math.min(m_C * m_data.instance(i2).weight(), -gamma);

	if(L < H){
	  if(eta > 0){
	    a2 = alpha2_ + deltaphi / eta;
	    if(a2 > H) a2 = H;
	    else if(a2 < L) a2 = L;
	  } else {
	    double Lobj = L*deltaphi;
	    double Hobj = H*deltaphi;
	    if(Lobj > Hobj)
	      a2 = L;
	    else
	      a2 = H;
	  }
	  a1 = alpha1_ - (a2 - alpha2_);
	  // update alpha1_, alpha2_ if change is larger than some eps
	  if(java.lang.Math.abs(a1-alpha1_) > m_eps || 
	     java.lang.Math.abs(a2-alpha2_) > m_eps){
	    alpha1_ = a1;
	    alpha2_ = a2;	
	    changed = true;
	  }
	} else {
	  finished = true;
	}
	case4 = true;
      } else {
	finished = true;
      }
	    
      // update deltaphi
      deltaphi += eta * ((alpha2 - alpha2_) - (m_alpha[i2] - m_alpha_[i2]));
    }
	
    if(changed){

      // update f-cache[i] for i in I_0 using new Lagrange multipliers
      for (int i = m_I0.getNext(-1); i != -1; i = m_I0.getNext(i)) {
	if (i != i1 && i != i2){
	  m_fcache[i] += 
	    ((m_alpha[i1] - m_alpha_[i1]) - (alpha1 - alpha1_)) * m_kernel.eval(i1, i, m_data.instance(i1)) +
	    ((m_alpha[i2] - m_alpha_[i2]) - (alpha2 - alpha2_)) * m_kernel.eval(i2, i, m_data.instance(i2));
	}
      }
	    
      // update f-cache[i1] and f-cache[i2]
      m_fcache[i1] += 
	((m_alpha[i1] - m_alpha_[i1]) - (alpha1 - alpha1_)) * k11 +
	((m_alpha[i2] - m_alpha_[i2]) - (alpha2 - alpha2_)) * k12;
      m_fcache[i2] += 
	((m_alpha[i1] - m_alpha_[i1]) - (alpha1 - alpha1_)) * k12 +
	((m_alpha[i2] - m_alpha_[i2]) - (alpha2 - alpha2_)) * k22;

      // to prevent precision problems
      if(alpha1 > m_C * m_data.instance(i1).weight() - m_Del * m_C * m_data.instance(i1).weight()){
	alpha1 = m_C * m_data.instance(i1).weight();
      } else if (alpha1 <= m_Del * m_C * m_data.instance(i1).weight()){
	alpha1 = 0;
      }
      if(alpha1_ > m_C * m_data.instance(i1).weight() - m_Del * m_C * m_data.instance(i1).weight()){
	alpha1_ = m_C * m_data.instance(i1).weight();
      } else if (alpha1_ <= m_Del * m_C * m_data.instance(i1).weight()){
	alpha1_ = 0;
      }
      if(alpha2 > m_C * m_data.instance(i2).weight() - m_Del * m_C * m_data.instance(i2).weight()){
	alpha2 = m_C * m_data.instance(i2).weight();
      } else if (alpha2 <= m_Del * m_C * m_data.instance(i2).weight()){
	alpha2 = 0;
      }
      if(alpha2_ > m_C * m_data.instance(i2).weight() - m_Del * m_C * m_data.instance(i2).weight()){
	alpha2_ = m_C * m_data.instance(i2).weight();
      } else if (alpha2_ <= m_Del * m_C * m_data.instance(i2).weight()){
	alpha2_ = 0;
      }
	
      // Store the changes in alpha, alpha' array
      m_alpha[i1] = alpha1;
      m_alpha_[i1] = alpha1_;
      m_alpha[i2] = alpha2;
      m_alpha_[i2] = alpha2_;

      // update I_0, I_1, I_2, I_3
      if((0 < alpha1 && alpha1 < m_C * m_data.instance(i1).weight()) ||
	 (0 < alpha1_ && alpha1_ < m_C * m_data.instance(i1).weight())){
	m_I0.insert(i1);
      } else { 
	m_I0.delete(i1);
      }
      if((alpha1 == 0 && alpha1_ == 0)){
	m_I1.insert(i1);
      } else { 
	m_I1.delete(i1);
      }
      if((alpha1 == 0 && alpha1_ == m_C * m_data.instance(i1).weight())){
	m_I2.insert(i1);
      } else { 
	m_I2.delete(i1);
      }
      if((alpha1 == m_C * m_data.instance(i1).weight() && alpha1_ == 0)){
	m_I3.insert(i1);
      } else { 
	m_I3.delete(i1);
      }
      if((0 < alpha2 && alpha2 < m_C * m_data.instance(i2).weight()) ||
	 (0 < alpha2_ && alpha2_ < m_C * m_data.instance(i2).weight())){
	m_I0.insert(i2);
      } else { 
	m_I0.delete(i2);
      }
      if((alpha2 == 0 && alpha2_ == 0)){
	m_I1.insert(i2);
      } else { 
	m_I1.delete(i2);
      }
      if((alpha2 == 0 && alpha2_ == m_C * m_data.instance(i2).weight())){
	m_I2.insert(i2);
      } else { 
	m_I2.delete(i2);
      }
      if((alpha2 == m_C * m_data.instance(i2).weight() && alpha2_ == 0)){
	m_I3.insert(i2);
      } else { 
	m_I3.delete(i2);
      }

      // for debuggage
      // checkSets();
      // checkAlphas();

      // Compute (i_low, b_low) and (i_up, b_up) by applying the conditions
      // mentionned above, using only i1, i2 and indices in I_0
      m_bLow = -Double.MAX_VALUE; m_bUp = Double.MAX_VALUE;
      m_iLow =-1; m_iUp = -1;
      for (int i = m_I0.getNext(-1); i != -1; i = m_I0.getNext(i)) {
	if(0 < m_alpha_[i] && m_alpha_[i] < m_C * m_data.instance(i).weight() && 
	   (m_fcache[i] + m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i] + m_epsilon; m_iLow = i;
	} else if(0 < m_alpha[i] && m_alpha[i] < m_C * m_data.instance(i).weight() && 
		  (m_fcache[i] - m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i] - m_epsilon; m_iLow = i;
	}
	if(0 < m_alpha[i] && m_alpha[i] < m_C * m_data.instance(i).weight() && 
	   (m_fcache[i] - m_epsilon < m_bUp)){
	  m_bUp = m_fcache[i] - m_epsilon; m_iUp = i;
	} else if(0 < m_alpha_[i] && m_alpha_[i] < m_C * m_data.instance(i).weight() && 
		  (m_fcache[i] + m_epsilon < m_bUp)){		    
	  m_bUp = m_fcache[i] + m_epsilon; m_iUp = i;
	}
      }

      if(!m_I0.contains(i1)){
	if(m_I2.contains(i1) && 
	   (m_fcache[i1] + m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i1] + m_epsilon; m_iLow = i1;
	} else if (m_I1.contains(i1) && 
		   (m_fcache[i1] - m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i1] - m_epsilon; m_iLow = i1;
	}
	if(m_I3.contains(i1) && 
	   (m_fcache[i1] - m_epsilon < m_bUp)){
	  m_bUp = m_fcache[i1] - m_epsilon; m_iUp = i1;
	} else if (m_I1.contains(i1) && 
		   (m_fcache[i1] + m_epsilon < m_bUp)){
	  m_bUp = m_fcache[i1] + m_epsilon; m_iUp = i1;
	}
      }
	    
      if(!m_I0.contains(i2)){
	if(m_I2.contains(i2) && 
	   (m_fcache[i2] + m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i2] + m_epsilon; m_iLow = i2;
	} else if (m_I1.contains(i2) && 
		   (m_fcache[i2] - m_epsilon > m_bLow)){
	  m_bLow = m_fcache[i2] - m_epsilon; m_iLow = i2;
	}
	if(m_I3.contains(i2) && 
	   (m_fcache[i2] - m_epsilon < m_bUp)){
	  m_bUp = m_fcache[i2] - m_epsilon; m_iUp = i2;
	} else if (m_I1.contains(i2) && 
		   (m_fcache[i2] + m_epsilon < m_bUp)){
	  m_bUp = m_fcache[i2] + m_epsilon; m_iUp = i2;
	}
      }
      if(m_iLow == -1 || m_iUp == -1){
	throw new RuntimeException("Fatal error! The program failled to "
				   + "initialize i_Low, iUp.");
      }
      return true;
    } else {
      return false;
    }    
  }


  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
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

    // classification :

    double result = m_b;

    // Is the machine linear?
    if (!m_useRBF && m_exponent == 1.0) {
	
      // Is weight vector stored in sparse format?
      if (m_sparseWeights == null) {
	int n1 = inst.numValues(); 
	for (int p = 0; p < n1; p++) {
	  if (inst.index(p) != m_classIndex) {
	    result += m_weights[inst.index(p)] * inst.valueSparse(p);
	  }
	}
      } else {
	int n1 = inst.numValues(); int n2 = m_sparseWeights.length;
	for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
	  int ind1 = inst.index(p1); 
	  int ind2 = m_sparseIndices[p2];
	  if (ind1 == ind2) {
	    if (ind1 != m_classIndex) {
	      result += inst.valueSparse(p1) * m_sparseWeights[p2];
	    }
	    p1++; p2++;
	  } else if (ind1 > ind2) {
	    p2++;
	  } else { 
	    p1++;
	  }
	}
      }
    } else {
      for (int i = 0; i < m_alpha.length; i++) { 
	result += (m_alpha[i] - m_alpha_[i]) * m_kernel.eval(-1, i, inst);
      }
    }

    // apply the inverse transformation 
    // (due to the normalization/standardization)
    return (result - m_Blin) / m_Alin;
  }
    

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
	
    Vector newVector = new Vector(11);

    newVector.addElement(new Option("\tThe amount up to which deviations are\n"
				    + "\ttolerated (epsilon). (default 1e-3)",
				    "S", 1, "-S <double>"));
    newVector.addElement(new Option("\tThe complexity constant C. (default 1)",
				    "C", 1, "-C <double>"));
    newVector.addElement(new Option("\tThe exponent for the "
				    + "polynomial kernel. (default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tGamma for the "
				    + "RBF kernel. (default 0.01)",
				    "G", 1, "-G <double>"));
    newVector.addElement(new Option("\tWhether to 0=normalize/1=standardize/2=neither. " +
				    "(default 0=normalize)",
				    "N", 1, "-N"));
    newVector.addElement(new Option("\tFeature-space normalization (only for\n"
				    +"\tnon-linear polynomial kernels).",
				    "F", 0, "-F"));
    newVector.addElement(new Option("\tUse lower-order terms (only for non-linear\n"
				    +"\tpolynomial kernels).",
				    "O", 0, "-O"));
    newVector.addElement(new Option("\tUse RBF kernel. " +
				    "(default poly)",
				    "R", 0, "-R"));
    newVector.addElement(new Option("\tThe size of the kernel cache. " +
				    "(default 250007, use 0 for full cache)",
				    "A", 1, "-A <int>"));
    newVector.addElement(new Option("\tThe tolerance parameter. " +
				    "(default 1.0e-3)",
				    "T", 1, "-T <double>"));
    newVector.addElement(new Option("\tThe epsilon for round-off error. " +
				    "(default 1.0e-12)",
				    "P", 1, "-P <double>"));
    return newVector.elements();
  }
    
    
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -S num <br>
   * The amount up to which deviation are tolerated (epsilon). (default 1e-3)
   * Watch out, the value of epsilon is used with the (normalized/standardize) data<p>
   *
   * -C num <br>
   * The complexity constant C. (default 1)<p>
   *
   * -E num <br>
   * The exponent for the polynomial kernel. (default 1) <p>
   *
   * -G num <br>
   * Gamma for the RBF kernel. (default 0.01) <p>
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
   * -R <br>
   * Use RBF kernel (default poly). <p>
   * 
   * -A num <br>
   * Sets the size of the kernel cache. Should be a prime number. (default 250007, use 0 for full cache) <p>
   *
   * -T num <br>
   * Sets the tolerance parameter. (default 1.0e-3)<p>
   *
   * -P num <br> 
   * Sets the epsilon for round-off error. (default 1.0e-12)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    String epsilonString = Utils.getOption('S', options);
    if (epsilonString.length() != 0) {
      m_epsilon = (new Double(epsilonString)).doubleValue();
    } else {
      m_epsilon = 1e-3;
    }
    String complexityString = Utils.getOption('C', options);
    if (complexityString.length() != 0) {
      m_C = (new Double(complexityString)).doubleValue();
    } else {
      m_C = 1.0;
    }
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
      m_gamma = 0.01;
    }
    String cacheString = Utils.getOption('A', options);
    if (cacheString.length() != 0) {
      m_cacheSize = Integer.parseInt(cacheString);
    } else {
      m_cacheSize = 250007;
    }
    String toleranceString = Utils.getOption('T', options);
    if (toleranceString.length() != 0) {
      m_tol = (new Double(toleranceString)).doubleValue();
    } else {
      m_tol = 1.0e-3;
    }
    String epsString = Utils.getOption('P', options);
    if (epsString.length() != 0) {
      m_eps = (new Double(epsString)).doubleValue();
    } else {
      m_eps = 1.0e-12;
    }
    m_useRBF = Utils.getFlag('R', options);
    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));
    }
    m_featureSpaceNormalization = Utils.getFlag('F', options);
    if ((m_useRBF) && (m_featureSpaceNormalization)) {
      throw new Exception("RBF machine doesn't require feature-space normalization.");
    }
    if ((m_exponent == 1.0) && (m_featureSpaceNormalization)) {
      throw new Exception("Can't use feature-space normalization with linear machine.");
    }
    m_lowerOrder = Utils.getFlag('O', options);
    if ((m_useRBF) && (m_lowerOrder)) {
      throw new Exception("Can't use lower-order terms with RBF machine.");
    }
    if ((m_exponent == 1.0) && (m_lowerOrder)) {
      throw new Exception("Can't use lower-order terms with linear machine.");
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [20];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + m_epsilon;
    options[current++] = "-C"; options[current++] = "" + m_C;
    options[current++] = "-E"; options[current++] = "" + m_exponent;
    options[current++] = "-G"; options[current++] = "" + m_gamma;
    options[current++] = "-A"; options[current++] = "" + m_cacheSize;
    options[current++] = "-T"; options[current++] = "" + m_tol;
    options[current++] = "-P"; options[current++] = "" + m_eps;
    options[current++] = "-N"; options[current++] = "" + m_filterType;
    if (m_featureSpaceNormalization) {
      options[current++] = "-F";
    }
    if (m_lowerOrder) {
      options[current++] = "-O";
    }
    if (m_useRBF) {
      options[current++] = "-R";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
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
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
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
  public String cTipText() {
    return "The complexity parameter C.";
  }
  
  /**
   * Get the value of C.
   *
   * @return Value of C.
   */
  public double getC() {
    
    return m_C;
  }
  
  /**
   * Set the value of C.
   *
   * @param v  Value to assign to C.
   */
  public void setC(double v) {
    
    m_C = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String toleranceParameterTipText() {
    return "The tolerance parameter (shouldn't be changed).";
  }

  /**
   * Get the value of tolerance parameter.
   * @return Value of tolerance parameter.
   */
  public double getToleranceParameter() {
    
    return m_tol;
  }
  
  /**
   * Set the value of tolerance parameter.
   * @param v  Value to assign to tolerance parameter.
   */
  public void setToleranceParameter(double v) {
    
    m_tol = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String epsTipText() {
    return "The epsilon for round-off error (shouldn't be changed).";
  }
  
  /**
   * Get the value of eps.
   * @return Value of eps.
   */
  public double getEps() {
    
    return m_eps;
  }
  
  /**
   * Set the value of eps.
   * @param v  Value to assign to epsilon.
   */
  public void setEps(double v) {
    
    m_eps = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String epsilonTipText() {
    return "The amount up to which deviations are tolerated. " 
      + "Watch out, the value of epsilon is used with the (normalized/standardized) "
      + "data.";
  }

  /**
   * Get the value of epsilon.
   * @return Value of epsilon.
   */
  public double getEpsilon() {
    
    return m_epsilon;
  }
  
  /**
   * Set the value of epsilon.
   * @param v  Value to assign to epsilon.
   */
  public void setEpsilon(double v) {
    
    m_epsilon = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String cacheSizeTipText() {
    return "The size of the kernel cache (should be a prime number).";
  }
  
  /**
   * Get the size of the kernel cache
   * @return Size of kernel cache.
   */
  public int getCacheSize() {
    
    return m_cacheSize;
  }
  
  /**
   * Set the value of the kernel cache.
   * @param v  Size of kernel cache.
   */
  public void setCacheSize(int v) {
    
    m_cacheSize = v;
  }
     
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useRBFTipText() {
    return "Whether to use an RBF kernel instead of a polynomial one.";
  }
  
  /**
   * Check if the RBF kernel is to be used.
   * @return true if RBF
   */
  public boolean getUseRBF() {
    
    return m_useRBF;
  }

  
  /**
   * Set if the RBF kernel is to be used.
   * @param v  true if RBF
   */
  public void setUseRBF(boolean v) {

    if (v) {
      m_featureSpaceNormalization = false;
      m_lowerOrder = false;
    }
    m_useRBF = v;
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
    
    if ((m_useRBF) || (m_exponent == 1.0)) {
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
    
    if (m_exponent == 1.0 || m_useRBF) {
      m_lowerOrder = false;
    } else {
      m_lowerOrder = v;
    }
  }


  /**
   * Turns off checks for missing values, etc. Use with caution.
   */
  public void turnChecksOff() {
	
    m_checksTurnedOff = true;
  }
    

  /**
   * Turns on checks for missing values, etc.
   */
  public void turnChecksOn() {
	
    m_checksTurnedOff = false;
  }


  /**
   * Prints out the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    int printed = 0;
	
    if ((m_alpha == null) && (m_sparseWeights == null)) {
      return "SMOreg : No model built yet.";
    }
    try {
      text.append("SMOreg\n\n");
	    
      text.append("Kernel used : \n");
      if(m_useRBF) {
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

      // display the linear transformation
      String trans = "";
      if (m_filterType == FILTER_STANDARDIZE) {
	//text.append("LINEAR TRANSFORMATION APPLIED : \n");
	trans = "(standardized) ";
	//text.append(trans + m_data.classAttribute().name() + "  = " + 
	//	    m_Alin + " * " + m_data.classAttribute().name() + " + " + m_Blin + "\n\n");
      } else if (m_filterType == FILTER_NORMALIZE) {
	//text.append("LINEAR TRANSFORMATION APPLIED : \n");
	trans = "(normalized) ";
	//text.append(trans + m_data.classAttribute().name() + "  = " + 
	//	    m_Alin + " * " + m_data.classAttribute().name() + " + " + m_Blin + "\n\n");
      }

      // If machine linear, print weight vector
      if (!m_useRBF && m_exponent == 1.0) {
	text.append("Machine Linear: showing attribute weights, ");
	text.append("not support vectors.\n");
		
	// We can assume that the weight vector is stored in sparse
	// format because the classifier has been built
	text.append(trans + m_data.classAttribute().name() + " =\n");
	for (int i = 0; i < m_sparseWeights.length; i++) {
	  if (m_sparseIndices[i] != (int)m_classIndex) {
	    if (printed > 0) {
	      text.append(" + ");
	    } else {
	      text.append("   ");
	    }
	    text.append(Utils.doubleToString(m_sparseWeights[i], 12, 4) +
			" * ");
	    if (m_filterType == FILTER_STANDARDIZE) {
	      text.append("(standardized) ");
	    } else if (m_filterType == FILTER_NORMALIZE) {
	      text.append("(normalized) ");
	    }
	    if (!m_checksTurnedOff) {
	      text.append(m_data.attribute(m_sparseIndices[i]).name()+"\n");
	    } else {
	      text.append("attribute with index " + 
			  m_sparseIndices[i] +"\n");
	    }
	    printed++;
	  }
	}
      } else {
	text.append("Support Vector Expansion :\n");
	text.append(trans + m_data.classAttribute().name() + " =\n");
	printed = 0;
	for (int i = 0; i < m_alpha.length; i++) {
	  double val = m_alpha[i] - m_alpha_[i];
	  if (java.lang.Math.abs(val) < 1e-4)
	    continue;
	  if (printed > 0) {
	    text.append(" + ");
	  } else {
	    text.append("   ");		    
	  }
	  text.append(Utils.doubleToString(val, 12, 4) 
		      + " * K[X(" + i + "), X]\n");
	  printed++;
	}
      }
      if (m_b > 0) {
	text.append(" + " + Utils.doubleToString(m_b, 12, 4));
      } else {
	text.append(" - " + Utils.doubleToString(-m_b, 12, 4));
      }
      if (m_useRBF || m_exponent != 1.0) {
	text.append("\n\nNumber of support vectors: " + printed);
      }
      int numEval = 0;
      int numCacheHits = -1;
      if(m_kernel != null)
	{
	  numEval = m_kernel.numEvals();
	  numCacheHits = m_kernel.numCacheHits();
	}
      text.append("\n\nNumber of kernel evaluations: " + numEval);
      if (numCacheHits >= 0 && numEval > 0)
	{
	  double hitRatio = 1 - numEval/(numCacheHits+numEval);
	  text.append(" (" + Utils.doubleToString(hitRatio*100, 7, 3) + "% cached)");
	}
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
      scheme = new SMOreg();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }




  /**
   * Debuggage function.
   * Compute the value of the objective function.
   */
  protected double objFun() throws Exception {
	
    double res = 0;
    double t = 0, t2 = 0;
    for(int i = 0; i < m_alpha.length; i++){
      for(int j = 0; j < m_alpha.length; j++){
	t += (m_alpha[i] - m_alpha_[i]) * (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i,j,m_data.instance(i));
      }
      t2 += m_data.instance(i).classValue() * (m_alpha[i] - m_alpha_[i]) - m_epsilon * (m_alpha[i] + m_alpha_[i]);
    }	
    res += -0.5 * t + t2;
    return res;
  }


  /**
   * Debuggage function.
   * Compute the value of the objective function.
   */
  protected double objFun(int i1, int i2, 
			double alpha1, double alpha1_, 
			double alpha2, double alpha2_) throws Exception {
	
    double res = 0;
    double t = 0, t2 = 0;
    for(int i = 0; i < m_alpha.length; i++){
      double alphai;
      double alphai_;
      if(i == i1){
	alphai = alpha1; alphai_ = alpha1_;
      } else if(i == i2){
	alphai = alpha2; alphai_ = alpha2_;
      } else {
	alphai = m_alpha[i]; alphai_ = m_alpha_[i];
      }
      for(int j = 0; j < m_alpha.length; j++){
	double alphaj;
	double alphaj_;
	if(j == i1){
	  alphaj = alpha1; alphaj_ = alpha1_;
	} else if(j == i2){
	  alphaj = alpha2; alphaj_ = alpha2_;
	} else {
	  alphaj = m_alpha[j]; alphaj_ = m_alpha_[j];		
	}
	t += (alphai - alphai_) * (alphaj - alphaj_) * m_kernel.eval(i,j,m_data.instance(i));
      }
      t2 += m_data.instance(i).classValue() * (alphai - alphai_) - m_epsilon * (alphai + alphai_);
    }	
    res += -0.5 * t + t2;
    return res;
  }



  /** 
   * Debuggage function.
   * Check that the set I0, I1, I2 and I3 cover the whole set of index 
   * and that no attribute appears in two different sets. 
   */
  protected void checkSets() throws Exception{
	
    boolean[] test = new boolean[m_data.numInstances()];
    for (int i = m_I0.getNext(-1); i != -1; i = m_I0.getNext(i)) {
      if(test[i]){
	throw new Exception("Fatal error! indice " + i + " appears in two different sets.");
      } else {
	test[i] = true;
      }
      if( !((0 < m_alpha[i] && m_alpha[i] < m_C * m_data.instance(i).weight()) ||
	    (0 < m_alpha_[i] && m_alpha_[i] < m_C * m_data.instance(i).weight())) ){
	throw new Exception("Warning! I0 contains an incorrect indice.");
      }	    
    }
    for (int i = m_I1.getNext(-1); i != -1; i = m_I1.getNext(i)) {
      if(test[i]){
	throw new Exception("Fatal error! indice " + i + " appears in two different sets.");
      } else {
	test[i] = true;
      }
      if( !( m_alpha[i] == 0 && m_alpha_[i] == 0) ){
	throw new Exception("Fatal error! I1 contains an incorrect indice.");
      }
    }
    for (int i = m_I2.getNext(-1); i != -1; i = m_I2.getNext(i)) {
      if(test[i]){
	throw new Exception("Fatal error! indice " + i + " appears in two different sets.");
      } else {
	test[i] = true;
      }
      if( !(m_alpha[i] == 0 && m_alpha_[i] == m_C * m_data.instance(i).weight()) ){
	throw new Exception("Fatal error! I2 contains an incorrect indice.");
      }
    }
    for (int i = m_I3.getNext(-1); i != -1; i = m_I3.getNext(i)) {
      if(test[i]){
	throw new Exception("Fatal error! indice " + i + " appears in two different sets.");
      } else {
	test[i] = true;
      }
      if( !(m_alpha_[i] == 0 && m_alpha[i] == m_C * m_data.instance(i).weight()) ){
	throw new Exception("Fatal error! I3 contains an incorrect indice.");
      }
    }
    for (int i = 0; i < test.length; i++){
      if(!test[i]){
	throw new Exception("Fatal error! indice " + i + " doesn't belong to any set.");
      }
    }
  }


  /**
   * Debuggage function
   * Checks that : 
   *      alpha*alpha_=0 
   *      sum(alpha[i] - alpha_[i]) = 0 
   */
  protected void checkAlphas() throws Exception{

    double sum = 0;
    for(int i = 0; i < m_alpha.length; i++){
      if(!(0 == m_alpha[i] || m_alpha_[i] == 0)){
	throw new Exception("Fatal error! Inconsistent alphas!");
      }
      sum += (m_alpha[i] - m_alpha_[i]);
    }
    if(sum > 1e-10){
      throw new Exception("Fatal error! Inconsistent alphas' sum = " + sum);
    }
  }


    
  /**
   * Debuggage function.
   * Display the current status of the program.
   * @param i1 the first current indice
   * @param i2 the second current indice
   */
  protected void displayStat(int i1, int i2) throws Exception {
	
    System.err.println("\n-------- Status : ---------");
    System.err.println("\n i, alpha, alpha'\n");
    for(int i = 0; i < m_alpha.length; i++){
      double result = (m_bLow + m_bUp)/2.0; 
      for (int j = 0; j < m_alpha.length; j++) { 
	result += (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i, j, m_data.instance(i));
      }
      System.err.print(" " + i + ":  (" + m_alpha[i] + ", " + m_alpha_[i] + 
		       "),       " + (m_data.instance(i).classValue() - m_epsilon) + " <= " + 
		       result + " <= " +  (m_data.instance(i).classValue() + m_epsilon));
      if(i == i1){
	System.err.print(" <-- i1");
      }
      if(i == i2){
	System.err.print(" <-- i2");
      }
      System.err.println();
    }
    System.err.println("bLow = " + m_bLow + "  bUp = " + m_bUp);
    System.err.println("---------------------------\n");
  }

    
  /**
   * Debuggage function
   * Compute and display bLow, lUp and so on...
   */
  protected void displayB() throws Exception {

    //double bUp =  Double.NEGATIVE_INFINITY;
    //double bLow = Double.POSITIVE_INFINITY;
    //int iUp = -1, iLow = -1;
    for(int i = 0; i < m_data.numInstances(); i++){
      double Fi = m_data.instance(i).classValue();
      for(int j = 0; j < m_alpha.length; j++){
	Fi -= (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i, j, m_data.instance(i));
      }
      System.err.print("(" + m_alpha[i] + ", " + m_alpha_[i] + ") : ");
      System.err.print((Fi - m_epsilon) + ",  " + (Fi + m_epsilon));
      double fim = Fi - m_epsilon, fip =  Fi + m_epsilon;
      String s = "";
      if (m_I0.contains(i)){
	if ( 0 < m_alpha[i] && m_alpha[i] < m_C * m_data.instance(i).weight()){ 
	  s += "(in I0a) bUp = min(bUp, " + fim + ")   bLow = max(bLow, " + fim + ")";
	}
	if ( 0 < m_alpha_[i] && m_alpha_[i] < m_C * m_data.instance(i).weight()){ 
	  s += "(in I0a) bUp = min(bUp, " + fip + ")   bLow = max(bLow, " + fip + ")";
	}
      } 
      if (m_I1.contains(i)){
	s += "(in I1) bUp = min(bUp, " + fip + ")   bLow = max(bLow, " + fim + ")";
      } 
      if (m_I2.contains(i)){
	s += "(in I2) bLow = max(bLow, " + fip + ")";
      } 
      if (m_I3.contains(i)){
	s += "(in I3) bUp = min(bUp, " + fim + ")";
      }
      System.err.println(" " + s + " {" + (m_alpha[i]-1) + ", " + (m_alpha_[i]-1) + "}");	    
    }
    System.err.println("\n\n");	
  }





  /**
   * Debuggage function.
   * Checks if the equations (6), (8a), (8b), (8c), (8d) hold.
   * (Refers to "Improvements to SMO Algorithm for SVM Regression".)
   * Prints warnings for each equation which doesn't hold.
   */
  protected void checkOptimality() throws Exception {

    double bUp =  Double.POSITIVE_INFINITY;
    double bLow = Double.NEGATIVE_INFINITY;
    int iUp = -1, iLow = -1;

    for(int i = 0; i < m_data.numInstances(); i++){

      double Fi = m_data.instance(i).classValue();
      for(int j = 0; j < m_alpha.length; j++){
	Fi -= (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i, j, m_data.instance(i));
      }
	    
      double fitilde = 0, fibarre = 0;
      if(m_I0.contains(i) && 0 < m_alpha[i] && m_alpha[i] < m_C * m_data.instance(i).weight()){
	fitilde = Fi - m_epsilon;
	fibarre = Fi - m_epsilon;
      }
      if(m_I0.contains(i) && 0 < m_alpha_[i] && m_alpha_[i] < m_C * m_data.instance(i).weight()){
	fitilde = Fi + m_epsilon;
	fibarre = Fi + m_epsilon;
      }
      if(m_I1.contains(i)){
	fitilde = Fi - m_epsilon;
	fibarre = Fi + m_epsilon;
      }
      if(m_I2.contains(i)){
	fitilde = Fi + m_epsilon;
	fibarre = Double.POSITIVE_INFINITY;
      }
      if(m_I3.contains(i)){
	fitilde = Double.NEGATIVE_INFINITY;
	fibarre = Fi - m_epsilon;		
      }
      if(fibarre < bUp){
	bUp = fibarre;
	iUp = i;
      }
      if(fitilde > bLow){
	bLow = fitilde;
	iLow = i;
      }
    }
    if(!(bLow <= bUp + 2 * m_tol)){
      System.err.println("Warning! Optimality not reached : inequation (6) doesn't hold!");
    }

    boolean noPb = true;	
    for(int i = 0; i < m_data.numInstances(); i++){
      double Fi = m_data.instance(i).classValue();
      for(int j = 0; j < m_alpha.length; j++){
	Fi -= (m_alpha[j] - m_alpha_[j]) * m_kernel.eval(i, j, m_data.instance(i));
      }
      double Ei = Fi - ((m_bUp + m_bLow) / 2.0);
      if((m_alpha[i] > 0) && !(Ei >= m_epsilon - m_tol)){
	System.err.println("Warning! Optimality not reached : inequation (8a) doesn't hold for " + i);
	noPb = false;
      }
      if((m_alpha[i] < m_C * m_data.instance(i).weight()) && !(Ei <= m_epsilon + m_tol)){
	System.err.println("Warning! Optimality not reached : inequation (8b) doesn't hold for " + i);
	noPb = false;
      }
      if((m_alpha_[i] > 0) && !(Ei <= -m_epsilon + m_tol)){
	System.err.println("Warning! Optimality not reached : inequation (8c) doesn't hold for " + i);
	noPb = false;
      }
      if((m_alpha_[i] < m_C * m_data.instance(i).weight()) && !(Ei >= -m_epsilon - m_tol)){
	System.err.println("Warning! Optimality not reached : inequation (8d) doesn't hold for " + i);
	noPb = false;
      }
    }
    if(!noPb){
      System.err.println();
      //displayStat(-1,-1);
      //displayB();
    }
  }

}
