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
 *    VotedPerceptron.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;
import weka.core.*;
import java.util.*;

/**
 * Implements the voted perceptron algorithm by Freund and
 * Schapire. Globally replaces all missing values, and transforms
 * nominal attributes into binary ones. For more information, see<p>
 *
 * Y. Freund and R. E. Schapire (1998). <i> Large margin
 * classification using the perceptron algorithm</i>.  Proc. 11th
 * Annu. Conf. on Comput. Learning Theory, pp. 209-217, ACM Press, New
 * York, NY. <p>
 *
 * Valid options are:<p>
 *
 * -I num <br>
 * The number of iterations to be performed. (default 1)<p>
 *
 * -E num <br>
 * The exponent for the polynomial kernel. (default 1)<p>
 *
 * -S num <br>
 * The seed for the random number generator. (default 1)<p>
 *
 * -M num <br>
 * The maximum number of alterations allowed. (default 10000) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.17 $ 
*/
public class VotedPerceptron extends Classifier implements OptionHandler {
  
  /** The maximum number of alterations to the perceptron */
  private int m_MaxK = 10000;

  /** The number of iterations */
  private int m_NumIterations = 1;

  /** The exponent */
  private double m_Exponent = 1.0;

  /** The actual number of alterations */
  private int m_K = 0;

  /** The training instances added to the perceptron */
  private int[] m_Additions = null;

  /** Addition or subtraction? */
  private boolean[] m_IsAddition = null;

  /** The weights for each perceptron */
  private int[] m_Weights = null;
  
  /** The training instances */
  private Instances m_Train = null;

  /** Seed used for shuffling the dataset */
  private int m_Seed = 1;

  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Implementation of the voted perceptron algorithm by Freund and "
      +"Schapire. Globally replaces all missing values, and transforms "
      +"nominal attributes into binary ones. For more information, see:\n\n"
      +"Y. Freund and R. E. Schapire (1998). Large margin "
      +"classification using the perceptron algorithm.  Proc. 11th "
      +"Annu. Conf. on Comput. Learning Theory, pp. 209-217, ACM Press, New "
      +"York, NY.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tThe number of iterations to be performed.\n"
				    + "\t(default 1)",
				    "I", 1, "-I <int>"));
    newVector.addElement(new Option("\tThe exponent for the polynomial kernel.\n"
				    + "\t(default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tThe seed for the random number generation.\n"
				    + "\t(default 1)",
				    "S", 1, "-S <int>"));
    newVector.addElement(new Option("\tThe maximum number of alterations allowed.\n"
				    + "\t(default 10000)",
				    "M", 1, "-M <int>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -I num <br>
   * The number of iterations to be performed. (default 1)<p>
   *
   * -E num <br>
   * The exponent for the polynomial kernel. (default 1)<p>
   *
   * -S num <br>
   * The seed for the random number generator. (default 1)<p>
   *
   * -M num <br>
   * The maximum number of alterations allowed. (default 10000) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String iterationsString = Utils.getOption('I', options);
    if (iterationsString.length() != 0) {
      m_NumIterations = Integer.parseInt(iterationsString);
    } else {
      m_NumIterations = 1;
    }
    String exponentsString = Utils.getOption('E', options);
    if (exponentsString.length() != 0) {
      m_Exponent = (new Double(exponentsString)).doubleValue();
    } else {
      m_Exponent = 1.0;
    }
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      m_Seed = Integer.parseInt(seedString);
    } else {
      m_Seed = 1;
    }
    String alterationsString = Utils.getOption('M', options);
    if (alterationsString.length() != 0) {
      m_MaxK = Integer.parseInt(alterationsString);
    } else {
      m_MaxK = 10000;
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String [8];
    int current = 0;

    options[current++] = "-I"; options[current++] = "" + m_NumIterations;
    options[current++] = "-E"; options[current++] = "" + m_Exponent;
    options[current++] = "-S"; options[current++] = "" + m_Seed;
    options[current++] = "-M"; options[current++] = "" + m_MaxK;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Builds the ensemble of perceptrons.
   *
   * @exception Exception if something goes wrong during building
   */
  public void buildClassifier(Instances insts) throws Exception {
 
    if (insts.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Can't handle a numeric class!");
    }

    // Filter data
    m_Train = new Instances(insts);
    m_Train.deleteWithMissingClass();
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_ReplaceMissingValues);
    
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_NominalToBinary);

    /** Randomize training data */
    m_Train.randomize(new Random(m_Seed));

    /** Make space to store perceptrons */
    m_Additions = new int[m_MaxK + 1];
    m_IsAddition = new boolean[m_MaxK + 1];
    m_Weights = new int[m_MaxK + 1];

    /** Compute perceptrons */
    m_K = 0;
  out:
    for (int it = 0; it < m_NumIterations; it++) {
      for (int i = 0; i < m_Train.numInstances(); i++) {
	Instance inst = m_Train.instance(i);
	if (!inst.classIsMissing()) {
	  int prediction = makePrediction(m_K, inst);
	  int classValue = (int) inst.classValue();
	  if (prediction == classValue) {
	    m_Weights[m_K]++;
	  } else {
	    m_IsAddition[m_K] = (classValue == 1);
	    m_Additions[m_K] = i;
	    m_K++;
	    m_Weights[m_K]++;
	  }
	  if (m_K == m_MaxK) {
	    break out;
	  }
	}
      }
    }
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
    m_ReplaceMissingValues.batchFinished();
    inst = m_ReplaceMissingValues.output();

    m_NominalToBinary.input(inst);
    m_NominalToBinary.batchFinished();
    inst = m_NominalToBinary.output();
    
    // Get probabilities
    double output = 0, sumSoFar = 0;
    if (m_K > 0) {
      for (int i = 0; i <= m_K; i++) {
	if (sumSoFar < 0) {
	  output -= m_Weights[i];
	} else {
	  output += m_Weights[i];
	}
	if (m_IsAddition[i]) {
	  sumSoFar += innerProduct(m_Train.instance(m_Additions[i]), inst);
	} else {
	  sumSoFar -= innerProduct(m_Train.instance(m_Additions[i]), inst);
	}
      }
    }
    double[] result = new double[2];
    result[1] = 1 / (1 + Math.exp(-output));
    result[0] = 1 - result[1];

    return result;
  }

  /**
   * Returns textual description of classifier.
   */
  public String toString() {

    return "VotedPerceptron: Number of perceptrons=" + m_K;
  }
 
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxKTipText() {
    return "The maximum number of alterations to the perceptron.";
  }

  /**
   * Get the value of maxK.
   *
   * @return Value of maxK.
   */
  public int getMaxK() {
    
    return m_MaxK;
  }
  
  /**
   * Set the value of maxK.
   *
   * @param v  Value to assign to maxK.
   */
  public void setMaxK(int v) {
    
    m_MaxK = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "Number of iterations to be performed.";
  }

  /**
   * Get the value of NumIterations.
   *
   * @return Value of NumIterations.
   */
  public int getNumIterations() {
    
    return m_NumIterations;
  }
  
  /**
   * Set the value of NumIterations.
   *
   * @param v  Value to assign to NumIterations.
   */
  public void setNumIterations(int v) {
    
    m_NumIterations = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String exponentTipText() {
    return "Exponent for the polynomial kernel.";
  }

  /**
   * Get the value of exponent.
   *
   * @return Value of exponent.
   */
  public double getExponent() {
    
    return m_Exponent;
  }
  
  /**
   * Set the value of exponent.
   *
   * @param v  Value to assign to exponent.
   */
  public void setExponent(double v) {
    
    m_Exponent = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed for the random number generator.";
  }

  /**
   * Get the value of Seed.
   *
   * @return Value of Seed.
   */
  public int getSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the value of Seed.
   *
   * @param v  Value to assign to Seed.
   */
  public void setSeed(int v) {
    
    m_Seed = v;
  }

  /** 
   * Computes the inner product of two instances
   */
  private double innerProduct(Instance i1, Instance i2) throws Exception {

    // we can do a fast dot product
    double result = 0;
    int n1 = i1.numValues(); int n2 = i2.numValues();
    int classIndex = m_Train.classIndex();
    for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
        int ind1 = i1.index(p1);
        int ind2 = i2.index(p2);
        if (ind1 == ind2) {
            if (ind1 != classIndex) {
                result += i1.valueSparse(p1) *
                          i2.valueSparse(p2);
            }
            p1++; p2++;
        } else if (ind1 > ind2) {
            p2++;
        } else {
            p1++;
        }
    }
    result += 1.0;
    
    if (m_Exponent != 1) {
      return Math.pow(result, m_Exponent);
    } else {
      return result;
    }
  }

  /** 
   * Compute a prediction from a perceptron
   */
  private int makePrediction(int k, Instance inst) throws Exception {

    double result = 0;
    for (int i = 0; i < k; i++) {
      if (m_IsAddition[i]) {
	result += innerProduct(m_Train.instance(m_Additions[i]), inst);
      } else {
	result -= innerProduct(m_Train.instance(m_Additions[i]), inst);
      }
    }
    if (result < 0) {
      return 0;
    } else {
      return 1;
    }
  }

  /**
   * Main method.
   */
  public static void main(String[] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new VotedPerceptron(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
    
  
