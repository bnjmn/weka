/*
 *    Logistic.java
 *    Copyright (C) 1999 Len Trigg
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
 * Class for building and using a two-class logistic regression model.
 * Missing values are replaced using a ReplaceMissingValuesFilter, and
 * nominal attributes are transformed into numeric attributes using a
 * NominalToBinaryFilter.<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class Logistic extends DistributionClassifier implements OptionHandler {

  // =================
  // Protected members
  // =================

  /** The log-likelihood of the built model */
  protected double m_LL;

  /** The log-likelihood of the null model */
  protected double m_LLn;

  /** The coefficients of the model */
  protected double [] m_Par;

  /** The number of attributes in the model */
  protected int m_NumPredictors;

  /** The index of the class attribute */
  protected int m_ClassIndex;

  /** The filter used to make attributes numeric. */
  private NominalToBinaryFilter m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValues;

  /** Debugging output */
  protected boolean m_Debug;


  // =================
  // Protected methods
  // =================
  
  protected static double Norm(double z) { 

    return Statistics.chiSquaredProbability(z * z, 1);
  }

  /**
   * Evaluate the probability for this point using the current coefficients
   *
   * @param instDat the instance data
   * @return the probability for this instance
   */
  protected double evaluateProbability(double [] instDat) {

    double v = m_Par[0];
    for(int k = 1; k <= m_NumPredictors; k++)
	v += m_Par[k] * instDat[k];
    v = 1 / (1 + Math.exp(-v));
    return v;
  }

  /**
   * Calculates the log likelihood of the current set of
   * coefficients (stored in m_Par), given the data.
   *
   * @param X the instance data
   * @param Y the class values for each instance
   * @param jacobian the matrix which will contain the jacobian matrix after
   * the method returns
   * @param deltas an array which will contain the parameter adjustments after
   * the method returns
   * @return the log likelihood of the data.
   */
  protected double calculateLogLikelihood(double [][] X, double [] Y, 
					  Matrix jacobian, double [] deltas) {

    double LL = 0;
    double [][] Arr = new double [jacobian.numRows()][jacobian.numRows()];

    for (int j = 0; j < Arr.length; j++) {
      for (int k = 0; k < Arr.length; k++) {
	Arr[j][k] = 0;
      }
      deltas[j] = 0;
    }

    // For each data point
    for (int i = 0; i < X.length; i++) {	
      // Evaluate the probability for this point using the current coefficients
      double p = evaluateProbability(X[i]);

      // Update the log-likelihood of this set of coefficients
      if( Y[i]==1 ) {
	LL = LL - 2 * Math.log(p);
      } else {
	LL = LL - 2 * Math.log(1 - p);
      }

      double w = p * (1 - p);     // Weight
      double z = (Y[i] - p);      // The error of this prediction

      for (int j = 0; j < Arr.length; j++) {
	double xij = X[i][j];
	deltas[j] += xij * z;
	for (int k = j; k < Arr.length; k++) {
	  Arr[j][k] += xij * X[i][k] * w;
	}
      }
    }
    // Fill out the rest of the array
    for (int j = 1; j < Arr.length; j++) {
      for (int k = 0; k < j; k++) {
	Arr[j][k] = Arr[k][j];
      }
    }

    for (int j = 0; j < Arr.length; j++) {
      jacobian.setRow(j, Arr[j]);
    }
    /*
    System.out.println("Jacobian:\n"+jacobian.toString());
    System.out.print("Deltas: ");
    for(int j = 0; j < deltas.length; j++)
      System.out.print(" "+Utils.doubleToString(deltas[j],10,3));
    System.out.println("");
    */
    return LL;
  }

  // ==============
  // Public methods
  // ==============

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tTurn on debugging output.",
              "D", 0, "-D"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [1];
    int current = 0;

    if (getDebug()) {
      options[current++] = "-D";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets whether debugging output will be printed.
   *
   * @param debug true if debugging output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Gets whether debugging output will be printed.
   *
   * @return true if debugging output will be printed
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Builds the classifier
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances train) throws Exception {

    if (train.classAttribute().type() != Attribute.NOMINAL) {
      throw new Exception("Class attribute must be nominal.");
    }
    if (train.classAttribute().numValues() != 2) {
      throw new Exception("Only 2-class problems allowed.");
    }
    if (train.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    m_ClassIndex = train.classIndex();
    train = new Instances(train);
    train.deleteWithMissingClass();
    if (train.numInstances() == 0) {
      throw new Exception("No train instances without missing class value!");
    }
    m_ReplaceMissingValues = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValues.inputFormat(train);
    train = Filter.useFilter(train, m_ReplaceMissingValues);
    m_NominalToBinary = new NominalToBinaryFilter();
    m_NominalToBinary.inputFormat(train);
    train = Filter.useFilter(train, m_NominalToBinary);

    int nR = m_NumPredictors = train.numAttributes() - 1;
    int nC = train.numInstances();

    double [][] X  = new double [nC][nR + 1];       // Data values
    double [] Y    = new double [nC];               // Class values
    double [] xMean= new double [nR + 1];           // Attribute means
    double [] xSD  = new double [nR + 1];           // Attribute stddev's
    double sY0 = 0;                                 // Number of class 0
    double sY1 = 0;                                 // Number of class 1

    if (m_Debug) {
      System.out.println("Extracting data...");
    }
    for (int i = 0; i < X.length; i++) {
      Instance current = train.instance(i);
      X[i][0] = 1;
      int j = 1;
      for (int k = 0; k <= nR; k++) {
	if (k != m_ClassIndex) {
	  double x = current.value(k);
	  if ((i == j) && (i <= nR)) {
	    x += 1e-8;
	  }
	  X[i][j] = x;
	  xMean[j] = xMean[j] + x;
	  xSD[j] = xSD[j] + x*x;
	  j++;
	}
      }
      Y[i] = current.classValue();
      if (Y[i] != 0) {
	Y[i] = 1;
      }
      if (Y[i] == 0) {
	sY0 = sY0 + 1;
      } else {
	sY1 = sY1 + 1;
      }
    }
    xMean[0] = 0; xSD[0] = 1;
    for (int j = 1; j <= nR; j++) {
      xMean[j] = xMean[j] / nC;
      xSD[j] = xSD[j] / nC;
      xSD[j] = Math.sqrt(Math.abs( xSD[j] - xMean[j] * xMean[j]));
    }
    if (m_Debug) {
      // Output stats about input data
      System.out.println("Descriptives...");
      System.out.println("" + sY0 + " cases have Y=0; " 
			 + sY1 + " cases have Y=1.");
      System.out.println("\n Variable     Avg       SD    ");
      for (int j = 1; j <= nR; j++) 
	System.out.println(Utils.doubleToString(j,8,4) 
			   + Utils.doubleToString(xMean[j], 10, 4) 
			   + Utils.doubleToString(xSD[j], 10, 4)
			   );
    }
    
    // Normalise input data and remove ignored attributes
    for (int i = 0; i < nC; i++) {
      for (int j = 0; j <= nR; j++) {
	if (xSD[j] != 0) {
	  X[i][j] = (X[i][j] - xMean[j]) / xSD[j];
	}
      }
    }

    if (m_Debug) {
      System.out.println("\nIteration History..." );
    }
    m_Par = new double [nR + 1];    // Coefficients
    double LLp = 2e+10;             // Log-likelihood on previous iteration
    m_LL  = 1e+10;                  // Log-likelihood of current iteration
    m_LLn = 0;                      // Log-likelihood of null hypothesis

    double [] deltas = new double [nR + 1];
    Matrix jacobian = new Matrix(nR + 1, nR + 1);

    // Set up parameters for null model
    m_Par[0] = Math.log((sY1+1) / (sY0+1));
    for (int j = 1; j < m_Par.length; j++) {
      m_Par[j] = 0;
    }
    
    // While the log-likelihood is changing (i.e. no maxima found)
    while(Math.abs(LLp-m_LL) > 0.00001) {
      LLp = m_LL;
      m_LL = calculateLogLikelihood(X, Y, jacobian, deltas);
      if (LLp == 1e+10) {
	m_LLn = m_LL; 
      }
      if (m_Debug) {
	System.out.println("-2 Log Likelihood = " 
			   + Utils.doubleToString(m_LL, 10, 5)
			   + ((m_LLn == m_LL) ? " (Null Model)" : ""));
      }
      
      jacobian.lubksb(jacobian.ludcmp(), deltas);

      // Adjust the coefficients
      for (int j = 0; j < deltas.length; j++) {
	m_Par[j] += deltas[j];
      }
    }
    if (m_Debug) {
      System.out.println(" (Converged)");
    }


    // Convert coefficients back to non-normalized attribute units
    for(int j = 1; j <= nR; j++) {
      if (xSD[j] != 0) {
	m_Par[j] = m_Par[j] / xSD[j];
	m_Par[0] = m_Par[0] - m_Par[j] * xMean[j];
      }
    }
  }		

  /**
   * Computes the distribution for a given instance
   *
   * @param instance the instance for which distribution is computed
   * @return the distribution
   * @exception Exception if the distribution can't be computed successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {

    m_ReplaceMissingValues.input(instance);
    instance = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(instance);
    instance = m_NominalToBinary.output();

    // Extract the predictor columns into an array
    double [] instDat = new double [m_NumPredictors + 1];
    int j = 1;
    for (int k = 0; k <= m_NumPredictors; k++) {
      if (k != m_ClassIndex) {
	instDat[j++] = instance.value(k);
      }
    }

    double [] distribution = new double [2];
    distribution[1] = evaluateProbability(instDat);
    distribution[0] = 1.0-distribution[1];
    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {

    double CSq = m_LLn - m_LL;
    int df = m_NumPredictors;
    String result = "Logistic Regression (2 classes)\n";
    if (m_Par == null) {
      return result + "No model built yet.";
    }
    result += "\nOverall Model Fit...\n" 
      +"  Chi Square=" + Utils.doubleToString(CSq, 10, 4) 
      + ";  df=" + df
      + ";  p=" 
      + Utils.doubleToString(Statistics.chiSquaredProbability(CSq, df), 10, 2)
      + "\n";
    
    result += "\nCoefficients...\n"
      + "Variable      Coeff.\n";
    for (int j = 1; j <= m_NumPredictors; j++) {
      result += Utils.doubleToString(j, 8, 0) 
      + Utils.doubleToString(m_Par[j], 12, 4) 
      + "\n";
    }
    result += "Intercept " + Utils.doubleToString(m_Par[0], 10, 4) + "\n";
    
    result += "\nOdds Ratios...\n"
      + "Variable         O.R.\n";
    for (int j = 1; j <= m_NumPredictors; j++) {
      double ORc = Math.exp( m_Par[j] );
      result += Utils.doubleToString(j, 8, 0) 
	+ " " 
	+ ((ORc > 1e10) ?  "" + ORc : Utils.doubleToString(ORc, 12, 4))
	+ "\n";
    }
    return result;
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new Logistic(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
