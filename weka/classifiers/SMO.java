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
import weka.core.*;
import weka.filters.*;

/**
 * Implements John C. Platt's sequential minimal optimization algorithm for 
 * training a support vector classifier. Does not implement speed-up
 * for linear feature space and sparse input data. Globally replaces all 
 * missing values, and transforms nominal attributes into binary ones. <p>
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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class SMO extends DistributionClassifier implements OptionHandler {

  /** The exponent for the polnomial kernel. */
  private double m_exponent = 1.0;

  /** The complexity parameter. */
  private double m_C = 1.0;

  /** The seed for the random number generation. */
  private int m_seed = 1;

  /** Epsilon for rounding. */
  private double m_eps = 1.0e-6;
  
  /** Tolerance for accuracy of result. */
  private double m_tol = 1.0e-3;

  /** The Lagrange multipliers. */
  private double[] m_alpha;

  /** The threshold. */
  private double m_b;

  /** The training data. */
  private Instances m_data;

  /** A random number generator. */
  private Random m_random;

  /** Permuted indices. */
  private int[] m_indices;

  /** The transformed class values. */
  private double[] m_class;

  /** The current set of errors for all non-bound examples. */
  private double[] m_errors;

  /** A hashtable for all unbound examples. */
  private int[] m_nextUnbound;

  /** The number of support vectors in the classifier. */
  private double m_numSupportVectors;

  /** The filter used to make attributes numeric. */
  private NominalToBinaryFilter m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValues;

  /**
   * Method for building the classifier.
   *
   * @param insts the set of training instances
   * @exception Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances insts) throws Exception {

    int numChanged = 0;
    boolean examineAll = true;

    if (insts.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new Exception("SMO can't handle a numeric class!");
    }

    // Filter data
    m_ReplaceMissingValues = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValues.inputFormat(insts);
    insts = Filter.useFilter(insts, m_ReplaceMissingValues);
    m_NominalToBinary = new NominalToBinaryFilter();
    m_NominalToBinary.inputFormat(insts);
    insts = Filter.useFilter(insts, m_NominalToBinary);

    // Get support vectors
    m_numSupportVectors = 0;
    m_random = new Random(m_seed);
    m_indices = new int[insts.numInstances()];
    for (int i = 0; i < m_indices.length; i++) {
      m_indices[i] = i;
    }
    m_class = new double[insts.numInstances()];
    m_errors = new double[insts.numInstances()];
    for (int i = 0; i < m_class.length; i++) {
      if ((int) insts.instance(i).classValue() == 0) {
	m_class[i] = -1;
      } else {
	m_class[i] = 1;
      }
    }
    m_nextUnbound = new int[insts.numInstances() + 1];
    for (int i = 0; i < insts.numInstances() + 1; i++) {
      m_nextUnbound[i] = -1;
    }
    m_alpha = new double[insts.numInstances()];
    m_b = 0;
    m_data = insts;
    while ((numChanged > 0) || examineAll) {
      numChanged = 0;
      if (examineAll) {
	for (int i = 0; i < insts.numInstances(); i++) {
	  if (examineExample(i)) {
	    numChanged ++;
	  }
	}
      } else {
	for (int i = m_nextUnbound[0]; i != -1; i = m_nextUnbound[i + 1]) {
	  if (m_alpha[i] < m_C) {
	    if (examineExample(i)) {
	      numChanged++;
	    }
	  }
	}
      }
      if (examineAll) {
	examineAll = false;
      } else if (numChanged == 0) {
	examineAll = true;
      }
    }
  }

  /**
   * Computes SVM output for given instance.
   *
   * @param inst the instance for which output is to be computed
   * @return the output of the SVM for the given instance
   */
  private double SVMOutput(Instance inst) throws Exception {

    double result = 0;

    for (int i = m_nextUnbound[0]; i != -1; i = m_nextUnbound[i + 1]) {
      result += m_class[i] * m_alpha[i] * kernel(inst, m_data.instance(i));
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
    m_NominalToBinary.input(inst);
    inst = m_NominalToBinary.output();
    
    // Get probabilities
    double output = SVMOutput(inst);
    double[] result = new double[2];
    result[1] = 1 / (1 + Math.exp(-output));
    result[0] = 1 - result[1];

    return result;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option("\tThe complexity constant C. (default 1)",
				    "C", 1, "-C <double>"));
    newVector.addElement(new Option("\tThe exponent for the "
				    + "polynomial kernel. (default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tThe seed for the random "
				    + "number generation."+
				    "\t(default 1)",
				    "S", 1, "-S <int>"));

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
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-C"; options[current++] = "" + m_C;
    options[current++] = "-E"; options[current++] = "" + m_exponent;
    options[current++] = "-S"; options[current++] = "" + m_seed;

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

    try {
      text.append("SMO classifier\n\n");
      for (int i = 0; i < m_alpha.length; i++) {
	if (m_alpha[i] > 0) {
	  printed++;
	  if (printed > 0) {
	    text.append(" + ");
	  } else {
	    text.append("   ");
	  }
	  text.append(((int)m_class[i]) + " * " +
		      m_alpha[i] + " * X(" + i + ") * X\n");
	}
      }
      text.append(" + " + m_b);
      text.append("\n\nNumber of support vectors: " + m_numSupportVectors);
    } catch (Exception e) {
      return "Can't print SMO classifier.";
    }
    
    return text.toString();
  }

  /**
   * Computes the result of the kernel function for two instances.
   *
   * @param inst1 the first instance
   * @param inst2 the second instance
   * @return the result of the kernel function
   */
  private double kernel(Instance inst1, Instance inst2) throws Exception {
    
    double result = 0;
    int classIndex = m_data.classIndex();
    
    for (int i = 0; i < inst1.numAttributes(); i++) {
      if (i != classIndex) {
	result += inst1.value(i) * inst2.value(i);
      }
    }
    result += 1;
    
    if (m_exponent != 1.0) {
      result = Math.pow(result, m_exponent);
    }

    return result;
  }

  /**
   * Permutes the integers in the given array.
   *
   * @param the array to be permuted
   */
  private void permute(int[] numbers) {

    int index, help;

    for (int i = numbers.length - 1; i > 0; i--) {
      index = (int) (m_random.nextDouble() * ((double) i));
      help = numbers[i];
      numbers[i] = numbers[index];
      numbers[index] = help;
    }
  }

  /**
   * Examines instance.
   *
   * @param i2 index of instance to examine
   * @return true if examination was successfull
   * @exception Exception if something goes wrong
   */
  private boolean examineExample(int i2) throws Exception {
    
    double y2, alph2, E2, r2, E1, maxDiffE = -Double.MAX_VALUE;
    boolean foundOne, foundTwo;
    int choice = -1;
    
    y2 = m_class[i2];
    alph2 = m_alpha[i2];
    if ((alph2 > 0) && (alph2 < m_C)) {
      E2 = m_errors[i2];
    } else {
      E2 = SVMOutput(m_data.instance(i2)) - y2;
    }
    r2 = E2 * y2;
    if (((r2 < -m_tol) && (alph2 < m_C)) || 
	((r2 > m_tol) && (alph2 > 0))) {
      
      // Are there at least two non-zero and non-C alphas?
      
      foundOne = false; foundTwo = false;
      for (int i = m_nextUnbound[0]; i != -1; i = m_nextUnbound[i + 1]) {
	if (m_alpha[i] < m_C) {
	  if (foundOne) {
	    foundTwo = true;
	    break;
	  } else {
	    foundOne = true;
	  }
	}
      }
      if (foundTwo) {
	
	// Use second choice heuristic
	
	for (int i = m_nextUnbound[0]; i != -1; i = m_nextUnbound[i + 1]) {
	  if (m_alpha[i] < m_C) {
	    E1 = m_errors[i];
	    if (Math.abs(E1 - E2) > maxDiffE) {
	      maxDiffE = Math.abs(E1 - E2);
	      choice = i;
	    }
	  }
	}
	if (takeStep(choice, i2, E2)) {
	  return true;
	}
      }
      
      // Permute indices
      
      permute(m_indices);

      // Loop over all non-zero and non-C alpha

      for (int i = 0; i < m_alpha.length; i++) {
	if ((m_alpha[m_indices[i]] > 0) && (m_alpha[m_indices[i]] < m_C) &&
	    takeStep(m_indices[i], i2, E2)) {
	  return true;
	}
      }
      
      // Permute indices

      permute(m_indices);
	  
      // Loop over all instances
	
      for (int i = 0; i < m_alpha.length; i++) {
	if (takeStep(m_indices[i], i2, E2)) {
	  return true;
	}
      }
    }
    return false;
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
  private boolean takeStep(int i1, int i2, double E2) throws Exception {

    Instance inst1, inst2;
    double alph1, alph2, y1, y2, E1, s, L, H, k11, k12, k22, eta,
      a1, a2, f1, f2, v1, v2, Lobj, Hobj, b1, b2, bOld;
    
    // Don't do anything if the two instances are the same

    if (i1 == i2) {
      return false;
    }

    // Initialize variables
    
    inst1 = m_data.instance(i1); inst2 = m_data.instance(i2);
    alph1 = m_alpha[i1]; alph2 = m_alpha[i2];
    y1 = m_class[i1]; y2 = m_class[i2];
    if ((alph1 > 0) && (alph1 < m_C)) {
      E1 = m_errors[i1];
    } else {
      E1 = SVMOutput(inst1) - y1;
    }
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

    k11 = kernel(inst1, inst1);
    k12 = kernel(inst1, inst2);
    k22 = kernel(inst2, inst2);
    eta = 2 * k12 - k11 - k22;

    // Check if second derivative is negative

    if (eta < 0) {

      // Compute unconstrained maximum

      a2 = alph2 - y2 * (E1 - E2) / eta;

      // Compute constrained maximum

      if (a2 < L) {
	a2 = L;
      } else if (a2 > H) {
	a2 = H;
      }
    } else {

      // Look at endpoints of diagonal

      f1 = SVMOutput(inst1);
      f2 = SVMOutput(inst2);
      v1 = f1 + m_b - y1 * alph1 * k11 - y2 * alph2 * k12; 
      v2 = f2 + m_b - y1 * alph1 * k12 - y2 * alph2 * k22; 
      Lobj = alph1 + L - 0.5 * k11 * alph1 * alph1 - 
	0.5 * k22 * L * L - s * k12 * alph1 * L - 
	y1 * alph1 * v1 - y2 * L * v2;
      Hobj = alph1 + H - 0.5 * k11 * alph1 * alph1 - 
	0.5 * k22 * H * H - s * k12 * alph1 * H - 
	y1 * alph1 * v1 - y2 * H * v2;
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

    // Update threshold to reflect change in Lagrange multipliers

    bOld = m_b;
    b1 = E1 + y1 * (a1 - alph1) * k11 + y2 * (a2 - alph2) * k12 + m_b;
    b2 = E2 + y1 * (a1 - alph1) * k12 + y2 * (a2 - alph2) * k22 + m_b;
    if ((a1 > 0) && (a1 < m_C)) {
      m_b = b1;
    } else if ((a2 > 0) && (a2 < m_C)) {
      m_b = b2;
    } else if (L != H) {
      m_b = (b1 + b2) / 2;
    } else {
      throw new Exception("This should never happen!");
    }
    
    // Update weight vector to reflect change a1 and a2, if linear SVM
    // (not implemented)

    // Update array with Lagrange multipliers

    if ((a1 > 0) && (m_alpha[i1] == 0)) {
      for (int i = i1; 
	   (i >= 0) && (m_nextUnbound[i] == m_nextUnbound[i1 + 1]); i--) {
	m_nextUnbound[i] = i1;
      }
      m_numSupportVectors++;
    }
    if ((a1 == 0) && (m_alpha[i1] > 0)) {
      for (int i = i1; (i >= 0) && (m_nextUnbound[i] == i1); i--) {
	m_nextUnbound[i] = m_nextUnbound[i1 + 1];
      }
      m_numSupportVectors--;
    }
    m_alpha[i1] = a1;
    if ((a2 > 0) && (m_alpha[i2] == 0)) {
      for (int i = i2; 
	   (i >= 0) && (m_nextUnbound[i] == m_nextUnbound[i2 + 1]); i--) {
	m_nextUnbound[i] = i2;
      }
      m_numSupportVectors++;
    }
    if ((a2 == 0) && (m_alpha[i2] > 0)) {
      for (int i = i2; (i >= 0) && (m_nextUnbound[i] == i2); i--) {
	m_nextUnbound[i] = m_nextUnbound[i2 + 1];
      }
      m_numSupportVectors--;
    }
    m_alpha[i2] = a2;

    // Update error cache using new Lagrange multipliers

    for (int j = m_nextUnbound[0]; j != -1; j = m_nextUnbound[j + 1]) {
      if (m_alpha[j] < m_C) {
	if ((j == i1) || (j == i2)) {
	  m_errors[j] = 0;
	} else {
	  m_errors[j] += 
	    y1 * (a1 - alph1) * kernel(inst1, m_data.instance(j)) +
	    y2 * (a2 - alph2) * kernel(inst2, m_data.instance(j)) + bOld - m_b;
	}
      }
    }

    // Made some progress.

    return true;
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
   
