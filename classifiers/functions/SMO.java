/*
 *    SMO.java
 *    Copyright (C) 1999 Eibe Frank
 *
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
package weka.classifiers;

import java.util.*;
import java.io.*;
import weka.core.*;
import weka.filters.*;

/**
 * Implements John C. Platt's sequential minimal optimization
 * algorithm for training a support vector classifier using polynomial
 * kernels. Transforms output of SVM into probabilities by applying a
 * standard sigmoid function that is not fitted to the data.
 *
 * This implementation globally replaces all missing values,
 * transforms nominal attributes into binary ones, and normalizes all
 * numeric attributes. For more information on the SMO algorithm,
 * see<p>
 *
 * J. Platt (1998). <i>Fast Training of Support Vector
 * Machines using Sequential Minimal Optimization</i>. Advances in Kernel
 * Methods - Support Vector Learning, B. Schölkopf, C. Burges, and
 * A. Smola, eds., MIT Press. <p>
 *
 * S.S. Keerthi, S.K. Shevade, C. Bhattacharyya, K.R.K. Murthy (1999).
 * <i> Improvements to Platt's SMO Algorithm for SVM Classifier Design</i>.
 * Technical Report CD-99-14. Control Division, Dept of Mechanical and
 * Production Engineering, National University of Singapore. <p>
 *
 * Valid options are:<p>
 *
 * -C num <br>
 * The complexity constant C. (default 1)<p>
 *
 * -E num <br>
 * The exponent for the polynomial kernel. (default 1)<p>
 *
 * -S num <br>
 * The seed for the random number generator. (default 1)<p>
 *
 * -N <br>
 * Don't normalize the training instances. <p>
 *
 * -L <br>
 * Rescale kernel. <p>
 *
 * -O <br>
 * Use lower-order terms. <p>
 *
 * -A num <br>
 * Sets the size of the kernel cache. Should be a prime number. 
 * (default 1000003) <p>
 *
 * -T num <br>
 * Sets the tolerance parameter. (default 1.0e-3)<p>
 *
 * -P num <br>
 * Sets the epsilon for round-off error. (default 1.0e-15)<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@intelligenesis.net) (sparse vector code)
 * @version $Revision: 1.8 $ */
public class SMO extends DistributionClassifier implements OptionHandler {

  /**
   * Stores a set of a given size.
   */
  private class SMOset {

    /** The current number of elements in the set */
    private int m_number;

    /** The first element in the set */
    private int m_first;

    /** Indicators */
    private boolean[] m_indicators;

    /** The next element for each element */
    private int[] m_next;

    /** The previous element for each element */
    private int[] m_previous;

    /**
     * Creates a new set of the given size.
     */
    private SMOset(int size) {
      
      m_indicators = new boolean[size];
      m_next = new int[size];
      m_previous = new int[size];
      m_number = 0;
      m_first = -1;
    }
 
    /**
     * Checks whether an element is in the set.
     */
    private boolean contains(int index) {

      return m_indicators[index];
    }

    /**
     * Deletes an element from the set.
     */
    private void delete(int index) {

      if (m_indicators[index]) {
	if (m_first == index) {
	  m_first = m_next[index];
	} else {
	  m_next[m_previous[index]] = m_next[index];
	}
	if (m_next[index] != -1) {
	  m_previous[m_next[index]] = m_previous[index];
	}
	m_indicators[index] = false;
	m_number--;
      }
    }

    /**
     * Inserts an element into the set.
     */
    private void insert(int index) {

      if (!m_indicators[index]) {
	if (m_number == 0) {
	  m_first = index;
	  m_next[index] = -1;
	  m_previous[index] = -1;
	} else {
	  m_previous[m_first] = index;
	  m_next[index] = m_first;
	  m_previous[index] = -1;
	  m_first = index;
	}
	m_indicators[index] = true;
	m_number++;
      }
    }

    /** 
     * Gets the next element in the set. -1 gets the first one.
     */
    private int getNext(int index) {

      if (index == -1) {
	return m_first;
      } else {
	return m_next[index];
      }
    }

    /**
     * Prints all the current elements in the set.
     */
    private void printElements() {

      for (int i = getNext(-1); i != -1; i = getNext(i)) {
	System.err.print(i + " ");
      }
      System.err.println();
      for (int i = 0; i < m_indicators.length; i++) {
	if (m_indicators[i]) {
	  System.err.print(i + " ");
	}
      }
      System.err.println();
      System.err.println(m_number);
    }

    /** 
     * Returns the number of elements in the set.
     */
    private int numElements() {
      
      return m_number;
    }
  }

  /** The exponent for the polnomial kernel. */
  private double m_exponent = 1.0;

  /** The complexity parameter. */
  private double m_C = 1.0;

  /** The seed for the random number generation. */
  private int m_seed = 1;

  /** Epsilon for rounding. */
  private double m_eps = 1.0e-15;
  
  /** Tolerance for accuracy of result. */
  private double m_tol = 1.0e-3;

  /** The Lagrange multipliers. */
  private double[] m_alpha;

  /** The thresholds. */
  private double m_b, m_bLow, m_bUp;

  /** The indices for m_bLow and m_bUp */
  private int m_iLow, m_iUp;

  /** The training data. */
  private Instances m_data;

  /** Compressed instances for fast dot products. */
  private int m_compressIndex[][];
  private double m_compressValue[][];

  /** Weight vector for linear machine. */
  private double[] m_weights;

  /** Kernel function cache */
  private double[] m_storage;
  private int[] m_keys;

  /** The transformed class values. */
  private double[] m_class;

  /** The current set of errors for all non-bound examples. */
  private double[] m_errors;

  /** The five different sets used by the algorithm. */
  private SMOset m_I0; // {i: 0 < m_alpha[i] < C}
  private SMOset m_I1; // {i: m_class[i] = 1, m_alpha[i] = 0}
  private SMOset m_I2; // {i: m_class[i] = -1, m_alpha[i] =C}
  private SMOset m_I3; // {i: m_class[i] = 1, m_alpha[i] = C}
  private SMOset m_I4; // {i: m_class[i] = -1, m_alpha[i] = 0}

  /** The set of support vectors */
  private SMOset m_supportVectors; // {i: 0 < m_alpha[i]}

  /** The filter used to make attributes numeric. */
  private NominalToBinaryFilter m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValues;

  /** The filter used to normalize all values. */
  private NormalizationFilter m_Normalization;

  /** Counts the number of kernel evaluations. */
  private int m_kernelEvals;

  /** The size of the cache (a prime number) */
  private int m_cacheSize = 1000003;

  /** A random number generator */
  private Random m_random;

  /** True if we don't want to normalize */
  private boolean m_dontNormalize = false;

  /** Rescale? */
  private boolean m_rescale = false;
  
  /** Use lower-order terms? */
  private boolean m_lowerOrder = false;

  /** Only numeric attributes in the dataset? */
  private boolean m_onlyNumeric = true;

  /**
   * Method for building the classifier.
   *
   * @param insts the set of training instances
   * @exception Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances insts) throws Exception {

    int m_kernelEvals = 0;

    if (insts.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new Exception("SMO can't handle a numeric class!");
    }

    m_ReplaceMissingValues = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValues.inputFormat(insts);
    insts = Filter.useFilter(insts, m_ReplaceMissingValues);

    for (int i = 0; i < insts.numAttributes(); i++) {
      if (i != insts.classIndex()) {
	if (!insts.attribute(i).isNumeric()) {
	  m_onlyNumeric = false;
	  break;
	}
      }
    }

    if (!m_onlyNumeric) {
      m_NominalToBinary = new NominalToBinaryFilter();
      m_NominalToBinary.inputFormat(insts);
      insts = Filter.useFilter(insts, m_NominalToBinary);
    }

    if (!m_dontNormalize) {
      m_Normalization = new NormalizationFilter();
      m_Normalization.inputFormat(insts);
      insts = Filter.useFilter(insts, m_Normalization); 
    }

    // Randomize the data set
    m_random = new Random(m_seed);
    insts.randomize(m_random);

    // If machine is linear, reserve space for weights
    if (m_exponent == 1.0) {
      m_weights = new double[insts.numAttributes()];
    }

    // The kernel calculations are cached
    m_storage = new double[m_cacheSize];
    m_keys = new int[m_cacheSize];

    // Build compressed representations of the instance
    // vectors for fast dot products
    m_data = insts;
    m_compressIndex = new int[m_data.numInstances()+1][];
    m_compressValue = new double[m_data.numInstances()+1][];
    
    for (int i = 0; i < m_data.numInstances(); i++) {
      int[] tempCompressIndex = new int[m_data.numAttributes()];
      double[] tempCompressValue = new double[m_data.numAttributes()];
      int k = 0;
      
      for (int j = 0; j < m_data.numAttributes(); j++) {
	if( m_data.instance(i).value(j) != 0.0 && j != m_data.classIndex() ) {
	  tempCompressIndex[k] = j;
	  tempCompressValue[k] = m_data.instance(i).value(j);
	  k++;
	}
      }
      
      m_compressIndex[i] = new int[k];
      m_compressValue[i] = new double[k];
      
      System.arraycopy(tempCompressIndex, 0, m_compressIndex[i], 0, k);
      System.arraycopy(tempCompressValue, 0, m_compressValue[i], 0, k);
    }	

    // Set class values
    m_class = new double[insts.numInstances()];
    m_iUp = -1; m_iLow = -1;
    for (int i = 0; i < m_class.length; i++) {
      if ((int) insts.instance(i).classValue() == 0) {
	m_class[i] = -1; m_iLow = i;
      } else {
	m_class[i] = 1; m_iUp = i;
      }
    }
    if ((m_iUp == -1) || (m_iLow == -1)) {
      throw new Exception ("Only instances of one class in the data!");
    }

    // Initialize alpha array to zero
    m_alpha = new double[insts.numInstances()];

    // Initialize thresholds
    m_bUp = -1; m_bLow = 1;

    // Initialize error cache
    m_errors = new double[insts.numInstances()];
    m_errors[m_iLow] = 1; m_errors[m_iUp] = -1;

    // Initialize sets
    m_supportVectors = new SMOset(insts.numInstances());
    m_I0 = new SMOset(insts.numInstances());
    m_I1 = new SMOset(insts.numInstances());
    m_I2 = new SMOset(insts.numInstances());
    m_I3 = new SMOset(insts.numInstances());
    m_I4 = new SMOset(insts.numInstances());

    // Build up I1 and I4
    for (int i = 0; i < m_class.length; i++ ) {
      if (m_class[i] == 1) {
	m_I1.insert(i);
      } else {
	m_I4.insert(i);
      }
    }

    // Loop to find all the support vectors
    int numChanged = 0;
    boolean examineAll = true;
    while ((numChanged > 0) || examineAll) {
      numChanged = 0;
      if (examineAll) {
	for (int i = 0; i < m_alpha.length; i++) {
	  if (examineExample(i)) {
	    numChanged++;
	  }
	}
      } else {

	// This code implements Modification 1 from Keerthi et al.'s paper
	for (int i = 0; i < m_alpha.length; i++) {
	  if ((m_alpha[i] > 0) && (m_alpha[i] < m_C)) {
	    if (examineExample(i)) {
	      numChanged++;
	    }
	    
	    // Is optimality on unbound vectors obtained?
	    if (m_bUp > m_bLow - 2 * m_tol) {
	      numChanged = 0;
	      break;
	    }
	  }
	}

	/* This is the code for Modification 2 from Keerthi et al.'s paper

	boolean innerLoopSuccess = true;
	while ((m_bUp < m_bLow - 2 * m_tol) && (innerLoopSuccess == true)) {
	  innerLoopSuccess = takeStep(m_iUp, m_iLow, m_errors[m_iLow]);
	  if (innerLoopSuccess) {
	    numChanged++;
	  }
	}
	numChanged = 0; // Strange!*/
      }
      if (examineAll) {
	examineAll = false;
      } else if (numChanged == 0) {
	examineAll = true;
      }
    }
    
    // Set threshold
    m_b = (m_bLow + m_bUp) / 2.0;
  }
  
  /**
   * Computes SVM output for given instance.
   *
   * @param index the instance for which output is to be computed
   * @return the output of the SVM for the given instance
   */
  private double SVMOutput(int index) throws Exception {

    double result = 0;

    // Is the machine linear?
    if (m_exponent == 1.0) {
      for (int p = 0; p < m_compressIndex[index].length; p++) {
	result += m_weights[m_compressIndex[index][p]] * 
	  m_compressValue[index][p];
      }
    } else {
      for (int i = m_supportVectors.getNext(-1); i != -1; 
	   i = m_supportVectors.getNext(i)) {
	result += m_class[i] * m_alpha[i] * kernel(index, i);
      }
    }
    result -= m_b;
    
    return result;
  }


  /**
   * Outputs the distribution for the given output.
   *
   * Pipes output of SVM through sigmoid function.
   * @param inst the instance for which distribution is to be computed
   * @return the distribution
   * @exception Exception if something goes wrong
   */
  public double[] distributionForInstance(Instance inst) throws Exception {

    // Filter instance
    m_ReplaceMissingValues.input(inst);
    inst = m_ReplaceMissingValues.output();
    
    if (!m_onlyNumeric) {
      m_NominalToBinary.input(inst);
      inst = m_NominalToBinary.output();
    }
    
    if (!m_dontNormalize) {
      m_Normalization.input(inst);
      inst = m_Normalization.output();
    }
    
    int numInstances = m_alpha.length;

    // Put the compressed test instance at the end of the training data 
    // for speed
    m_compressIndex[numInstances] = new int[m_data.numAttributes()];
    m_compressValue[numInstances] = new double[m_data.numAttributes()];
    
    int k = 0;
    for (int j = 0; j < inst.numAttributes(); j++) {
      if (inst.value(j) != 0.0 && j != inst.classIndex()) {
	m_compressIndex[numInstances][k] = j;
	m_compressValue[numInstances][k] = inst.value(j);
	k++;
      }
    }

    // Get probabilities

    /* We pass in index=numInstances as the compressed test instance is stored
       in that location */
    double output = SVMOutput(numInstances);
    double[] result = new double[2];
    result[1] = 1.0 / (1.0 + Math.exp(-output));
    result[0] = 1.0 - result[1];

    return result;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(9);

    newVector.addElement(new Option("\tThe complexity constant C. (default 1)",
				    "C", 1, "-C <double>"));
    newVector.addElement(new Option("\tThe exponent for the "
				    + "polynomial kernel. (default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tThe seed for the random "
				    + "number generation. "+
				    "(default 1)",
				    "S", 1, "-S <int>"));
    newVector.addElement(new Option("\tDon't normalize the data.",
				    "N", 0, "-N"));
    newVector.addElement(new Option("\tRescale the kernel.",
				    "L", 0, "-L"));
    newVector.addElement(new Option("\tUse lower-order terms.",
				    "O", 0, "-O"));
    newVector.addElement(new Option("\tThe size of the kernel cache. " +
				    "(default 1000003)",
				    "A", 1, "-A <int>"));
    newVector.addElement(new Option("\tThe tolerance parameter. " +
				    "(default 1.0e-3)",
				    "T", 1, "-T <double>"));
    newVector.addElement(new Option("\tThe epsilon for round-off error. " +
				    "(default 1.0e-15)",
				    "P", 1, "-P <double>"));
    

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -C num <br>
   * The complexity constant C <p>
   *
   * -E num <br>
   * The exponent for the polynomial kernel <p>
   *
   * -S num <br>
   * The seed for the random number generator. (default 1)<p>
   *
   * -N <br>
   * Don't normalize the training instances. <p>
   *
   * -L <br>
   * Rescale kernel. <p>
   *
   * -O <br>
   * Use lower-order terms. <p>
   *
   * -A num <br>
   * Sets the size of the kernel cache. Should be a prime number. (default 1000003) <p>
   *
   * -T num <br>
   * Sets the tolerance parameter. (default 1.0e-3)<p>
   *
   * -P num <br>
   * Sets the epsilon for round-off error. (default 1.0e-15)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
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
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      m_seed = Integer.parseInt(seedString);
    } else {
      m_seed = 1;
    }
    String cacheString = Utils.getOption('A', options);
    if (cacheString.length() != 0) {
      m_cacheSize = Integer.parseInt(cacheString);
    } else {
      m_cacheSize = 1000003;
    }
    String toleranceString = Utils.getOption('T', options);
    if (toleranceString.length() != 0) {
      m_tol = (new Double(toleranceString)).doubleValue();
    } else {
      m_tol = 1.0e-3;
    }
    String epsilonString = Utils.getOption('P', options);
    if (epsilonString.length() != 0) {
      m_eps = (new Double(epsilonString)).doubleValue();
    } else {
      m_eps = 1.0e-15;
    }
    m_dontNormalize = Utils.getFlag('N', options);
    m_rescale = Utils.getFlag('L', options);
    if ((m_exponent == 1.0) && (m_rescale)) {
      throw new Exception("Can't use rescaling with linear machine.");
    }
    m_lowerOrder = Utils.getFlag('O', options);
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

    String [] options = new String [15];
    int current = 0;

    options[current++] = "-C"; options[current++] = "" + m_C;
    options[current++] = "-E"; options[current++] = "" + m_exponent;
    options[current++] = "-S"; options[current++] = "" + m_seed;
    options[current++] = "-A"; options[current++] = "" + m_cacheSize;
    options[current++] = "-T"; options[current++] = "" + m_tol;
    options[current++] = "-P"; options[current++] = "" + m_eps;
    if (m_dontNormalize) {
      options[current++] = "-N";
    }
    if (m_rescale) {
      options[current++] = "-L";
    }
    if (m_lowerOrder) {
      options[current++] = "-O";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Prints out the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    int printed = 0;

    if (m_alpha == null) {
      return "SMO: No model built yet.";
    }
    try {
      text.append("SMO\n\n");

      // If machine linear, print weight vector
      if (m_exponent == 1.0) {
	text.append("Machine linear: showing attribute weights, ");
	text.append("not support vectors.\n\n");
	for (int i = 0; i < m_weights.length; i++) {
	  if (i != (int)m_data.classIndex()) {
	    if (printed > 0) {
	      text.append(" + ");
	    } else {
	      text.append("   ");
	    }
	    text.append(m_weights[i] + " * x" + (i + 1) + "\n");
	    printed++;
	  }
	}
      } else {
	for (int i = 0; i < m_alpha.length; i++) {
	  if (m_supportVectors.contains(i)) {
	    if (printed > 0) {
	      text.append(" + ");
	    } else {
	      text.append("   ");
	    }
	    text.append(((int)m_class[i]) + " * " +
			m_alpha[i] + " * K[X(" + i + ") * X]\n");
	    printed++;
	  }
	}
      }
      text.append(" - " + m_b);
      text.append("\n\nNumber of support vectors: " + m_supportVectors.numElements());
      text.append("\n\nNumber of kernel evaluations: " + m_kernelEvals);
    } catch (Exception e) {
      return "Can't print SMO classifier.";
    }
    
    return text.toString();
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
      m_rescale = false;
      m_lowerOrder = false;
    }
    m_exponent = v;
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
   * Get the value of seed.
   *
   * @return Value of seed.
   */
  public int getSeed() {
    
    return m_seed;
  }
  
  /**
   * Set the value of seed.
   *
   * @param v  Value to assign to seed.
   */
  public void setSeed(int v) {
    
    m_seed = v;
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
   * Get the value of epsilon.
   * @return Value of epsilon.
   */
  public double getEpsilon() {
    
    return m_eps;
  }
  
  /**
   * Set the value of epsilon.
   * @param v  Value to assign to epsilon.
   */
  public void setEpsilon(double v) {
    
    m_eps = v;
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
   * Check whether data is to be normalized.
   * @return Value of dontNormalize.
   */
  public boolean getDontNormalizeData() {
    
    return m_dontNormalize;
  }
  
  /**
   * Set whether data is to be normalized.
   * @param v  Value to assign to dontNormalize.
   */
  public void setDontNormalizeData(boolean v) {
    
    m_dontNormalize = v;
  }
  
  /**
   * Check whether kernel is being rescaled.
   * @return Value of rescale.
   */
  public boolean getRescaleKernel() throws Exception {

    return m_rescale;
  }
  
  /**
   * Set whether kernel is to be rescaled. Defaults
   * to false if a linear machine is built.
   * @param v  Value to assign to rescale.
   */
  public void setRescaleKernel(boolean v) throws Exception {
    
    if (m_exponent == 1.0) {
      m_rescale = false;
    } else {
      m_rescale = v;
    }
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
    
    if (m_exponent == 1.0) {
      m_lowerOrder = false;
    } else {
      m_lowerOrder = v;
    }
  }

  /**
   * Computes the result of the kernel function for two instances.
   *
   * @param id1 the index of the first instance
   * @param id2 the index of the second instance
   * @return the result of the kernel function
   */
  private double kernel(int id1, int id2 ) throws Exception {

    double result = 0;
    int key = -1, location = -1;

    // we can't cache if we don't know the indexes
    if (id1 < m_alpha.length && id2 < m_alpha.length ) {
      
      if (id1 > id2) {
	key = id1 * m_alpha.length + id2;
      } else {
	key = id2 * m_alpha.length + id1;
      }
      location = key % m_keys.length;
       if (m_keys[location] == (key + 1)) {
	return m_storage[location];
      }
    }
	
    // we can do a fast dot product
    int length1 = m_compressIndex[id1].length;
    int length2 = m_compressIndex[id2].length;
    
    for (int p1 = 0, p2 = 0; p1 < length1 && p2 < length2;) {
      if (m_compressIndex[id1][p1] == m_compressIndex[id2][p2]) {
	result += m_compressValue[id1][p1] * 
	  m_compressValue[id2][p2];
	p1++;
	p2++;
      } else if (m_compressIndex[id1][p1] > m_compressIndex[id2][p2]) {
	p2++;
      } else { 
	p1++;
      }
    }
    
    // Use lower order terms?
    if (m_lowerOrder) {
      result += 1.0;
    }

    // Rescale kernel?
    if (m_rescale) {
      result /= (double)m_data.numAttributes() - 1;
    }      
    
    if (m_exponent != 1.0) {
      result = Math.pow(result, m_exponent);
    }
    m_kernelEvals++;
    
    // store result in cache 	
    if (key != -1){
      m_storage[location] = result;
      m_keys[location] = (key + 1);
    }
    return result;
  }

  /**
   * Examines instance.
   *
   * @param i2 index of instance to examine
   * @return true if examination was successfull
   * @exception Exception if something goes wrong
   */
  private boolean examineExample(int i2) throws Exception {
    
    double y2, alph2, F2;
    int i1 = -1;
    
    y2 = m_class[i2];
    alph2 = m_alpha[i2];
    if (m_I0.contains(i2)) {
      F2 = m_errors[i2];
    } else {
      F2 = SVMOutput(i2) + m_b - y2;
      m_errors[i2] = F2;
      
      // Update thresholds
      if ((m_I1.contains(i2) || m_I2.contains(i2)) && (F2 < m_bUp)) {
	m_bUp = F2; m_iUp = i2;
      } else if ((m_I3.contains(i2) || m_I4.contains(i2)) && (F2 > m_bLow)) {
	m_bLow = F2; m_iLow = i2;
      }
    }

    // Check optimality using current bLow and bUp and, if
    // violated, find an index i1 to do joint optimization
    // with i2...
    boolean optimal = true;
    if (m_I0.contains(i2) || m_I1.contains(i2) || m_I2.contains(i2)) {
      if (m_bLow - F2 > 2 * m_tol) {
	optimal = false; i1 = m_iLow;
      }
    }
    if (m_I0.contains(i2) || m_I3.contains(i2) || m_I4.contains(i2)) {
      if (F2 - m_bUp > 2 * m_tol) {
	optimal = false; i1 = m_iUp;
      }
    }
    if (optimal) {
      return false;
    }

    // For i2 unbound choose the better i1...
    if (m_I0.contains(i2)) {
      if (m_bLow - F2 > F2 - m_bUp) {
	i1 = m_iLow;
      } else {
	i1 = m_iUp;
      }
    }
    if (i1 == -1) {
      throw new Exception("This should never happen!");
    }
    return takeStep(i1, i2, F2);
  }

  /**
   * Method solving for the Lagrange multipliers for
   * two instances.
   *
   * @param i1 index of the first instance
   * @param i2 index of the second instance
   * @return true if multipliers could be found
   * @exception Exception if something goes wrong
   */
  private boolean takeStep(int i1, int i2, double F2) throws Exception {

    double alph1, alph2, y1, y2, F1, s, L, H, k11, k12, k22, eta,
      a1, a2, f1, f2, v1, v2, Lobj, Hobj, b1, b2, bOld;

    // Don't do anything if the two instances are the same
    if (i1 == i2) {
      return false;
    }

    // Initialize variables
    alph1 = m_alpha[i1]; alph2 = m_alpha[i2];
    y1 = m_class[i1]; y2 = m_class[i2];
    F1 = m_errors[i1];
    s = y1 * y2;

    // Find the constraints on a2
    if (y1 != y2) {
      L = Math.max(0, alph2 - alph1); 
      H = Math.min(m_C, m_C + alph2 - alph1);
    } else {
      L = Math.max(0, alph1 + alph2 - m_C);
      H = Math.min(m_C, alph1 + alph2);
    }
    if (L == H) {
      return false;
    }

    // Compute second derivative of objective function
    k11 = kernel(i1, i1);
    k12 = kernel(i1, i2);
    k22 = kernel(i2, i2);
    eta = 2 * k12 - k11 - k22;

    // Check if second derivative is negative
    if (eta < 0) {

      // Compute unconstrained maximum
      a2 = alph2 - y2 * (F1 - F2) / eta;

      // Compute constrained maximum
      if (a2 < L) {
	a2 = L;
      } else if (a2 > H) {
	a2 = H;
      }
    } else {

      // Look at endpoints of diagonal
      f1 = SVMOutput(i1);
      f2 = SVMOutput(i2);
      v1 = f1 + m_b - y1 * alph1 * k11 - y2 * alph2 * k12; 
      v2 = f2 + m_b - y1 * alph1 * k12 - y2 * alph2 * k22; 
      double gamma = alph1 + s * alph2;
      Lobj = (gamma - s * L) + L - 0.5 * k11 * (gamma - s * L) * (gamma - s * L) - 
	0.5 * k22 * L * L - s * k12 * (gamma - s * L) * L - 
	y1 * (gamma - s * L) * v1 - y2 * L * v2;
      Hobj = (gamma - s * H) + H - 0.5 * k11 * (gamma - s * H) * (gamma - s * H) - 
	0.5 * k22 * H * H - s * k12 * (gamma - s * H) * H - 
	y1 * (gamma - s * H) * v1 - y2 * H * v2;
      if (Lobj > Hobj + m_eps) {
	a2 = L;
      } else if (Lobj < Hobj - m_eps) {
	a2 = H;
      } else {
	a2 = alph2;
      }
    }
    if (Math.abs(a2 - alph2) < m_eps * (a2 + alph2 + m_eps)) {
      return false;
    }

    // Compute new value of a1
    a1 = alph1 + s * (alph2 - a2);
    
    // Update weight vector to reflect change a1 and a2, if linear SVM
    if (m_exponent == 1.0) {
      for (int p1 = 0; p1 < m_compressIndex[i1].length; p1++) {
	  m_weights[m_compressIndex[i1][p1]] += 
	    y1 * (a1 - alph1) * m_compressValue[i1][p1];
      }
      for (int p2 = 0; p2 < m_compressIndex[i2].length; p2++) {
	  m_weights[m_compressIndex[i2][p2]] += 
	    y2 * (a2 - alph2) * m_compressValue[i2][p2];
      }
    }

    // Update error cache using new Lagrange multipliers
    for (int j = m_I0.getNext(-1); j != -1; j = m_I0.getNext(j)) {
      if ((j != i1) && (j != i2)) {
	m_errors[j] += 
	  y1 * (a1 - alph1) * kernel(i1, j) + 
	  y2 * (a2 - alph2) * kernel(i2, j);
      }
    }

    // Update sets
    if (a1 > 0) {
      m_supportVectors.insert(i1);
    } else {
      m_supportVectors.delete(i1);
    }
    if ((a1 > 0) && (a1 < m_C)) {
      m_I0.insert(i1);
    } else {
      m_I0.delete(i1);
    }
    if ((y1 == 1) && (!(a1 > 0))) {
      m_I1.insert(i1);
    } else {
      m_I1.delete(i1);
    }
    if ((y1 == -1) && (!(a1 < m_C))) {
      m_I2.insert(i1);
    } else {
      m_I2.delete(i1);
    }
    if ((y1 == 1) && (!(a1 < m_C))) {
      m_I3.insert(i1);
    } else {
      m_I3.delete(i1);
    }
    if ((y1 == -1) && (!(a1 > 0))) {
      m_I4.insert(i1);
    } else {
      m_I4.delete(i1);
    }
    if (a2 > 0 + m_eps) {
      m_supportVectors.insert(i2);
    } else {
      m_supportVectors.delete(i2);
    }
    if ((a2 > 0) && (a2 < m_C)) {
      m_I0.insert(i2);
    } else {
      m_I0.delete(i2);
    }
    if ((y2 == 1) && (!(a2 > 0))) {
      m_I1.insert(i2);
    } else {
      m_I1.delete(i2);
    }
    if ((y2 == -1) && (!(a2 < m_C))) {
      m_I2.insert(i2);
    } else {
      m_I2.delete(i2);
    }
    if ((y2 == 1) && (!(a2 < m_C))) {
      m_I3.insert(i2);
    } else {
      m_I3.delete(i2);
    }
    if ((y2 == -1) && (!(a2 > 0))) {
      m_I4.insert(i2);
    } else {
      m_I4.delete(i2);
    }

    // Update array with Lagrange multipliers
    m_alpha[i1] = a1;
    m_alpha[i2] = a2;

    // Update error cache for i1 and i2
    m_errors[i1] += y1 * (a1 - alph1) * k11 + y2 * (a2 - alph2) * k12;
    m_errors[i2] += y1 * (a1 - alph1) * k12 + y2 * (a2 - alph2) * k22;
      
    // Update thresholds
    m_bLow = -Double.MAX_VALUE; m_bUp = Double.MAX_VALUE;
    m_iLow = -1; m_iUp = -1;
    for (int j = m_I0.getNext(-1); j != -1; j = m_I0.getNext(j)) {
      if (m_errors[j] < m_bUp) {
	m_bUp = m_errors[j]; m_iUp = j;
      }
      if (m_errors[j] > m_bLow) {
	m_bLow = m_errors[j]; m_iLow = j;
      }
    }
    if (m_errors[i1] < m_bUp) {
      m_bUp = m_errors[i1]; m_iUp = i1;
    }
    if (m_errors[i1] > m_bLow) {
      m_bLow = m_errors[i1]; m_iLow = i1;
    }
    if (m_errors[i2] < m_bUp) {
      m_bUp = m_errors[i2]; m_iUp = i2;
    }
    if (m_errors[i2] > m_bLow) {
      m_bLow = m_errors[i2]; m_iLow = i2;
    }
    if ((m_iLow == -1) || (m_iUp == -1)) {
      throw new Exception("This should never happen!");
    }

    // Made some progress.
    return true;
  }
  
  /**
   * Quick and dirty check whether the quadratic programming problem is solved.
   */
  private void checkClassifier() throws Exception {

    double sum = 0;
    for (int i = 0; i < m_alpha.length; i++) {
      if (m_alpha[i] > 0) {
	sum += m_class[i] * m_alpha[i];
      }
    }
    System.err.println("Sum of y(i) * alpha(i): " + sum);

    for (int i = 0; i < m_alpha.length; i++) {
      double output = SVMOutput(i);
      if (Utils.eq(m_alpha[i], 0)) {
	if (Utils.sm(m_class[i] * output, 1)) {
	  System.err.println("KKT condition 1 violated: " + m_class[i] * output);
	}
      } 
      if (Utils.gr(m_alpha[i], 0) && Utils.sm(m_alpha[i], m_C)) {
	if (!Utils.eq(m_class[i] * output, 1)) {
	  System.err.println("KKT condition 2 violated: " + m_class[i] * output);
	}
      } 
      if (Utils.eq(m_alpha[i], m_C)) {
	if (Utils.gr(m_class[i] * output, 1)) {
	  System.err.println("KKT condition 3 violated: " + m_class[i] * output);
	}
      } 
    }
  }  

  /**
   * Main method for testing this class.
   */
  public static void main(String[] argv) {

    Classifier scheme;

    try {
      scheme = new SMO();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
} 
   
