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
 *    Logistic.java
 *    Copyright (C) 2003 Xin Xu
 *
 */

package weka.classifiers.functions;

import weka.classifiers.*;
import java.util.*;
import java.io.*;
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

/**
 * Second implementation for building and using a multinomial logistic
 * regression model with a ridge estimator.  <p>
 * 
 * There are some modifications, however, compared to the paper of le
 * Cessie and van Houwelingen(1992): <br>
 *
 * If there are k classes for n instances with m attributes, the
 * parameter matrix B to be calculated will be an m*(k-1) matrix.<br>
 *
 * The probability for class j except the last class is <br>
 * Pj(Xi) = exp(XiBj)/((sum[j=1..(k-1)]exp(Xi*Bj))+1) <br>
 * The last class has probability <br>
 * 1-(sum[j=1..(k-1)]Pj(Xi)) = 1/((sum[j=1..(k-1)]exp(Xi*Bj))+1) <br>
 *
 * The (negative) multinomial log-likelihood is thus: <br>
 * L = -sum[i=1..n]{
 * sum[j=1..(k-1)](Yij * ln(Pj(Xi))) +
 * (1 - (sum[j=1..(k-1)]Yij)) * ln(1 - sum[j=1..(k-1)]Pj(Xi))
 * } + ridge * (B^2) <br>
 *
 * In order to find the matrix B for which L is minimised, a
 * Quasi-Newton Method is used to search for the optimized values of
 * the m*(k-1) variables.  Note that before we use the optimization
 * procedure, we "squeeze" the matrix B into a m*(k-1) vector.  For
 * details of the optimization procedure, please check
 * weka.core.Optimization class. <p>
 *
 * Although original Logistic Regression does not deal with instance
 * weights, we modify the algorithm a little bit to handle the
 * instance weights. <p>
 *
 * Reference: le Cessie, S. and van Houwelingen, J.C. (1992). <i>
 * Ridge Estimators in Logistic Regression.</i> Applied Statistics,
 * Vol. 41, No. 1, pp. 191-201. <p>
 *
 * Missing values are replaced using a ReplaceMissingValuesFilter, and
 * nominal attributes are transformed into numeric attributes using a
 * NominalToBinaryFilter.<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -R <ridge> <br>
 * Set the ridge parameter for the log-likelihood.<p>
 * 
 * -M <number of iterations> <br> Set the maximum number of iterations
 * (default -1, iterates until convergence).<p>
 *
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.32 $ */
public class Logistic extends Classifier 
  implements OptionHandler, WeightedInstancesHandler {
  
  /** The coefficients (optimized parameters) of the model */
  protected double [][] m_Par;
    
  /** The data saved as a matrix */
  protected double [][] m_Data;
    
  /** The number of attributes in the model */
  protected int m_NumPredictors;
    
  /** The index of the class attribute */
  protected int m_ClassIndex;
    
  /** The number of the class labels */
  protected int m_NumClasses;
    
  /** The ridge parameter. */
  protected double m_Ridge = 1e-8;
    
  /* An attribute filter */
  private RemoveUseless m_AttFilter;
    
  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;
    
  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;
    
  /** Debugging output */
  protected boolean m_Debug;

  /** Log-likelihood of the searched model */
  protected double m_LL;
    
  /** The maximum number of iterations. */
  private int m_MaxIts = -1;
    
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for building and using a multinomial logistic "
      +"regression model with a ridge estimator.\n\n"
      +"There are some modifications, however, compared to the paper of "
      +"leCessie and van Houwelingen(1992): \n\n" 
      +"If there are k classes for n instances with m attributes, the "
      +"parameter matrix B to be calculated will be an m*(k-1) matrix.\n\n"
      +"The probability for class j with the exception of the last class is\n\n"
      +"Pj(Xi) = exp(XiBj)/((sum[j=1..(k-1)]exp(Xi*Bj))+1) \n\n"
      +"The last class has probability\n\n"
      +"1-(sum[j=1..(k-1)]Pj(Xi)) \n\t= 1/((sum[j=1..(k-1)]exp(Xi*Bj))+1)\n\n"
      +"The (negative) multinomial log-likelihood is thus: \n\n"
      +"L = -sum[i=1..n]{\n\tsum[j=1..(k-1)](Yij * ln(Pj(Xi)))"
      +"\n\t+(1 - (sum[j=1..(k-1)]Yij)) \n\t* ln(1 - sum[j=1..(k-1)]Pj(Xi))"
      +"\n\t} + ridge * (B^2)\n\n"
      +"In order to find the matrix B for which L is minimised, a "
      +"Quasi-Newton Method is used to search for the optimized values of "
      +"the m*(k-1) variables.  Note that before we use the optimization "
      +"procedure, we 'squeeze' the matrix B into a m*(k-1) vector.  For "
      +"details of the optimization procedure, please check "
      +"weka.core.Optimization class.\n\n"
      +"Although original Logistic Regression does not deal with instance "
      +"weights, we modify the algorithm a little bit to handle the "
      +"instance weights.\n\n"
      +"For more information see:\n\nle Cessie, S. and van Houwelingen, J.C. (1992). " 
      +"Ridge Estimators in Logistic Regression.  Applied Statistics, "
      +"Vol. 41, No. 1, pp. 191-201. \n\n"
      +"Note: Missing values are replaced using a ReplaceMissingValuesFilter, and "
      +"nominal attributes are transformed into numeric attributes using a "
      +"NominalToBinaryFilter.";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tTurn on debugging output.",
				    "D", 0, "-D"));
    newVector.addElement(new Option("\tSet the ridge in the log-likelihood.",
				    "R", 1, "-R <ridge>"));
    newVector.addElement(new Option("\tSet the maximum number of iterations"+
				    " (default -1, until convergence).",
				    "M", 1, "-M <number>"));
    return newVector.elements();
  }
    
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -R ridge <br>
   * Set the ridge parameter for the log-likelihood.<p>
   * 
   * -M num <br>
   * Set the maximum number of iterations.
   * (default -1, until convergence)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));

    String ridgeString = Utils.getOption('R', options);
    if (ridgeString.length() != 0) 
      m_Ridge = Double.parseDouble(ridgeString);
    else 
      m_Ridge = 1.0e-8;
	
    String maxItsString = Utils.getOption('M', options);
    if (maxItsString.length() != 0) 
      m_MaxIts = Integer.parseInt(maxItsString);
    else 
      m_MaxIts = -1;
  }
    
  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
	
    String [] options = new String [5];
    int current = 0;
	
    if (getDebug()) 
      options[current++] = "-D";
    options[current++] = "-R";
    options[current++] = ""+m_Ridge;	
    options[current++] = "-M";
    options[current++] = ""+m_MaxIts;
    while (current < options.length) 
      options[current++] = "";
    return options;
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "Set the Ridge value in the log-likelihood.";
  }

  /**
   * Sets the ridge in the log-likelihood.
   *
   * @param ridge the ridge
   */
  public void setRidge(double ridge) {
    m_Ridge = ridge;
  }
    
  /**
   * Gets the ridge in the log-likelihood.
   *
   * @return the ridge
   */
  public double getRidge() {
    return m_Ridge;
  }
   
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxItsTipText() {
    return "Maximum number of iterations to perform.";
  }

  /**
   * Get the value of MaxIts.
   *
   * @return Value of MaxIts.
   */
  public int getMaxIts() {
	
    return m_MaxIts;
  }
    
  /**
   * Set the value of MaxIts.
   *
   * @param newMaxIts Value to assign to MaxIts.
   */
  public void setMaxIts(int newMaxIts) {
	
    m_MaxIts = newMaxIts;
  }    
    
  private class OptEng extends Optimization{
    // Weights of instances in the data
    private double[] weights;

    // Class labels of instances
    private int[] cls;
	
    /* Set the weights of instances
     * @param d the weights to be set
     */ 
    public void setWeights(double[] w) {
      weights = w;
    }
	
    /* Set the class labels of instances
     * @param d the class labels to be set
     */ 
    public void setClassLabels(int[] c) {
      cls = c;
    }
	
    /** 
     * Evaluate objective function
     * @param x the current values of variables
     * @return the value of the objective function 
     */
    protected double objectiveFunction(double[] x){
      double nll = 0; // -LogLikelihood
      int dim = m_NumPredictors+1; // Number of variables per class
	    
      for(int i=0; i<cls.length; i++){ // ith instance

	double[] exp = new double[m_NumClasses-1];
	int index;
	for(int offset=0; offset<m_NumClasses-1; offset++){ 
	  index = offset * dim;
	  for(int j=0; j<dim; j++)
	    exp[offset] += m_Data[i][j]*x[index + j];
	}
	double max = exp[Utils.maxIndex(exp)];
	double denom = Math.exp(-max);
	double num;
	if (cls[i] == m_NumClasses - 1) { // Class of this instance
	  num = -max;
	} else {
	  num = exp[cls[i]] - max;
	}
	for(int offset=0; offset<m_NumClasses-1; offset++){
	  denom += Math.exp(exp[offset] - max);
	}
		
	nll -= weights[i]*(num - Math.log(denom)); // Weighted NLL
      }
	    
      // Ridge: note that intercepts NOT included
      for(int offset=0; offset<m_NumClasses-1; offset++){
	for(int r=1; r<dim; r++)
	  nll += m_Ridge*x[offset*dim+r]*x[offset*dim+r];
      }
	    
      return nll;
    }

    /** 
     * Evaluate Jacobian vector
     * @param x the current values of variables
     * @return the gradient vector 
     */
    protected double[] evaluateGradient(double[] x){
      double[] grad = new double[x.length];
      int dim = m_NumPredictors+1; // Number of variables per class
	    
      for(int i=0; i<cls.length; i++){ // ith instance
	double[] num=new double[m_NumClasses-1]; // numerator of [-log(1+sum(exp))]'
	int index;
	for(int offset=0; offset<m_NumClasses-1; offset++){ // Which part of x
	  double exp=0.0;
	  index = offset * dim;
	  for(int j=0; j<dim; j++)
	    exp += m_Data[i][j]*x[index + j];
	  num[offset] = exp;
	}

	double max = num[Utils.maxIndex(num)];
	double denom = Math.exp(-max); // Denominator of [-log(1+sum(exp))]'
	for(int offset=0; offset<m_NumClasses-1; offset++){
	  num[offset] = Math.exp(num[offset] - max);
	  denom += num[offset];
	}
	Utils.normalize(num, denom);
		
	// Update denominator of the gradient of -log(Posterior)
	double firstTerm;
	for(int offset=0; offset<m_NumClasses-1; offset++){ // Which part of x
	  index = offset * dim;
	  firstTerm = weights[i] * num[offset];
	  for(int q=0; q<dim; q++){
	    grad[index + q] += firstTerm * m_Data[i][q];
	  }
	}
		
	if(cls[i] != m_NumClasses-1){ // Not the last class
	  for(int p=0; p<dim; p++){
	    grad[cls[i]*dim+p] -= weights[i]*m_Data[i][p]; 
	  }
	}
      }
	    
      // Ridge: note that intercepts NOT included
      for(int offset=0; offset<m_NumClasses-1; offset++){
	for(int r=1; r<dim; r++)
	  grad[offset*dim+r] += 2*m_Ridge*x[offset*dim+r];
      }
	    
      return grad;
    }
  }
    
  /**
   * Builds the classifier
   *
   * @param train the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances train) throws Exception {
    if (train.classAttribute().type() != Attribute.NOMINAL) {
      throw new UnsupportedClassTypeException("Class attribute must be nominal.");
    }
    if (train.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }
    train = new Instances(train);
    train.deleteWithMissingClass();
    if (train.numInstances() == 0) {
      throw new IllegalArgumentException("No train instances without missing class value!");
    }

    // Replace missing values	
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(train);
    train = Filter.useFilter(train, m_ReplaceMissingValues);

    // Remove useless attributes
    m_AttFilter = new RemoveUseless();
    m_AttFilter.setInputFormat(train);
    train = Filter.useFilter(train, m_AttFilter);
	
    // Transform attributes
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(train);
    train = Filter.useFilter(train, m_NominalToBinary);
	
    // Extract data
    m_ClassIndex = train.classIndex();
    m_NumClasses = train.numClasses();

    int nK = m_NumClasses - 1;                     // Only K-1 class labels needed 
    int nR = m_NumPredictors = train.numAttributes() - 1;
    int nC = train.numInstances();
	
    m_Data = new double[nC][nR + 1];               // Data values
    int [] Y  = new int[nC];                       // Class labels
    double [] xMean= new double[nR + 1];           // Attribute means
    double [] xSD  = new double[nR + 1];           // Attribute stddev's
    double [] sY = new double[nK + 1];             // Number of classes
    double [] weights = new double[nC];            // Weights of instances
    double totWeights = 0;                         // Total weights of the instances
    m_Par = new double[nR + 1][nK];                // Optimized parameter values
	
    if (m_Debug) {
      System.out.println("Extracting data...");
    }
	
    for (int i = 0; i < nC; i++) {
      // initialize X[][]
      Instance current = train.instance(i);
      Y[i] = (int)current.classValue();  // Class value starts from 0
      weights[i] = current.weight();     // Dealing with weights
      totWeights += weights[i];
	    
      m_Data[i][0] = 1;
      int j = 1;
      for (int k = 0; k <= nR; k++) {
	if (k != m_ClassIndex) {
	  double x = current.value(k);
	  m_Data[i][j] = x;
	  xMean[j] += weights[i]*x;
	  xSD[j] += weights[i]*x*x;
	  j++;
	}
      }
	    
      // Class count
      sY[Y[i]]++;	
    }
	
    if((totWeights <= 1) && (nC > 1))
      throw new Exception("Sum of weights of instances less than 1, please reweight!");

    xMean[0] = 0; xSD[0] = 1;
    for (int j = 1; j <= nR; j++) {
      xMean[j] = xMean[j] / totWeights;
      if(totWeights > 1)
	xSD[j] = Math.sqrt(Math.abs(xSD[j] - totWeights*xMean[j]*xMean[j])/(totWeights-1));
      else
	xSD[j] = 0;
    }

    if (m_Debug) {	    
      // Output stats about input data
      System.out.println("Descriptives...");
      for (int m = 0; m <= nK; m++)
	System.out.println(sY[m] + " cases have class " + m);
      System.out.println("\n Variable     Avg       SD    ");
      for (int j = 1; j <= nR; j++) 
	System.out.println(Utils.doubleToString(j,8,4) 
			   + Utils.doubleToString(xMean[j], 10, 4) 
			   + Utils.doubleToString(xSD[j], 10, 4)
			   );
    }
	
    // Normalise input data 
    for (int i = 0; i < nC; i++) {
      for (int j = 0; j <= nR; j++) {
	if (xSD[j] != 0) {
	  m_Data[i][j] = (m_Data[i][j] - xMean[j]) / xSD[j];
	}
      }
    }
	
    if (m_Debug) {
      System.out.println("\nIteration History..." );
    }
	
    double x[] = new double[(nR+1)*nK];
    double[][] b = new double[2][x.length]; // Boundary constraints, N/A here

    // Initialize
    for(int p=0; p<nK; p++){
      int offset=p*(nR+1);	 
      x[offset] =  Math.log(sY[p]+1.0) - Math.log(sY[nK]+1.0); // Null model
      b[0][offset] = Double.NaN;
      b[1][offset] = Double.NaN;   
      for (int q=1; q <= nR; q++){
	x[offset+q] = 0.0;		
	b[0][offset+q] = Double.NaN;
	b[1][offset+q] = Double.NaN;
      }	
    }
	
    OptEng opt = new OptEng();	
    opt.setDebug(m_Debug);
    opt.setWeights(weights);
    opt.setClassLabels(Y);

    if(m_MaxIts == -1){  // Search until convergence
      x = opt.findArgmin(x, b);
      while(x==null){
	x = opt.getVarbValues();
	if (m_Debug)
	  System.out.println("200 iterations finished, not enough!");
	x = opt.findArgmin(x, b);
      }
      if (m_Debug)
	System.out.println(" -------------<Converged>--------------");
    }
    else{
      opt.setMaxIteration(m_MaxIts);
      x = opt.findArgmin(x, b);
      if(x==null) // Not enough, but use the current value
	x = opt.getVarbValues();
    }
	
    m_LL = -opt.getMinFunction(); // Log-likelihood

    // Don't need data matrix anymore
    m_Data = null;
	    
    // Convert coefficients back to non-normalized attribute units
    for(int i=0; i < nK; i++){
      m_Par[0][i] = x[i*(nR+1)];
      for(int j = 1; j <= nR; j++) {
	m_Par[j][i] = x[i*(nR+1)+j];
	if (xSD[j] != 0) {
	  m_Par[j][i] /= xSD[j];
	  m_Par[0][i] -= m_Par[j][i] * xMean[j];
	}
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
    m_AttFilter.input(instance);
    instance = m_AttFilter.output();
    m_NominalToBinary.input(instance);
    instance = m_NominalToBinary.output();
	
    // Extract the predictor columns into an array
    double [] instDat = new double [m_NumPredictors + 1];
    int j = 1;
    instDat[0] = 1;
    for (int k = 0; k <= m_NumPredictors; k++) {
      if (k != m_ClassIndex) {
	instDat[j++] = instance.value(k);
      }
    }
	
    double [] distribution = evaluateProbability(instDat);
    return distribution;
  }

  /**
   * Compute the posterior distribution using optimized parameter values
   * and the testing instance.
   * @param data the testing instance
   * @return the posterior probability distribution
   */ 
  private double[] evaluateProbability(double[] data){
    double[] prob = new double[m_NumClasses],
      v = new double[m_NumClasses];

    // Log-posterior before normalizing
    for(int j = 0; j < m_NumClasses-1; j++){
      for(int k = 0; k <= m_NumPredictors; k++){
	v[j] += m_Par[k][j] * data[k];
      }
    }
    v[m_NumClasses-1] = 0;
	
    // Do so to avoid scaling problems
    for(int m=0; m < m_NumClasses; m++){
      double sum = 0;
      for(int n=0; n < m_NumClasses-1; n++)
	sum += Math.exp(v[n] - v[m]);
      prob[m] = 1 / (sum + Math.exp(-v[m]));
    }
	
    return prob;
  } 
    
  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {
	
    double CSq;
    int df = m_NumPredictors;
    String result = "Logistic Regression with ridge parameter of "+m_Ridge;
    if (m_Par == null) {
      return result + ": No model built yet.";
    }
	
    result += "\nCoefficients...\n"
      + "Variable      Coeff.\n";
    for (int j = 1; j <= m_NumPredictors; j++) {
      result += Utils.doubleToString(j, 8, 0);
      for (int k = 0; k < m_NumClasses-1; k++)
	result += " "+Utils.doubleToString(m_Par[j][k], 12, 4); 
      result += "\n";
    }
	
    result += "Intercept ";
    for (int k = 0; k < m_NumClasses-1; k++)
      result += " "+Utils.doubleToString(m_Par[0][k], 10, 4); 
    result += "\n";
	
    result += "\nOdds Ratios...\n"
      + "Variable         O.R.\n";
    for (int j = 1; j <= m_NumPredictors; j++) {
      result += Utils.doubleToString(j, 8, 0); 
      for (int k = 0; k < m_NumClasses-1; k++){
	double ORc = Math.exp(m_Par[j][k]);
	result += " " + ((ORc > 1e10) ?  "" + ORc : Utils.doubleToString(ORc, 12, 4));
      }
      result += "\n";
    }
    return result;
  }
    
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
