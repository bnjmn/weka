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
 *    Logisticf.java
 *    Copyright (C) 1999 Len Trigg, Eibe Frank, Tony Voyle
 *
 */

package weka.classifiers;

import java.util.*;
import java.io.*;
import weka.core.*;
import weka.filters.*;

/**
 * Class for building and using a two-class logistic regression model
 * with a ridge estimator.  <p>
 * 
 * This class utilizes globally convergent Newtons Method adapted from
 * Numerical Recipies in C. <p>
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
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Tony Voyle (tv6@cs.waikato.ac.nz)
 * @version $Revision: 1.13 $ 
 */
public class Logistic extends DistributionClassifier implements OptionHandler {

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

  /** The ridge parameter. */
  protected double m_Ridge = 1e-8;

  /** The filter used to make attributes numeric. */
  private NominalToBinaryFilter m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValues;

  /** Debugging output */
  protected boolean m_Debug;
  
  private double m_f;

  private int m_nn;
  
  private int m_check;
  
  private double m_ALF = 1.0e-4;

  private double m_TOLX = 1.0e-7;
  
  private double m_TOLMIN = 1.0e-6;

  private double m_STPMX = 100.0;

  private int m_MAXITS = 200;
  
  /**
   * Finds a new point x in the direction p from
   * a point xold at which the value of the function
   * has decreased sufficiently.
   *
   * @param n number of variables
   * @param xold old point
   * @param fold value at that point
   * @param g gtradient at that point
   * @param p direction
   * @param x new value along direction p from xold
   * @param stpmax maximum step length
   * @param X instance data
   * @param Y class values
   * @exception Exception if an error occurs
   */
  public void lnsrch(int n, double[] xold, double fold, double[] g, double[] p, 
		     double[] x, double stpmax, double[][] X, double[] Y)
    throws Exception {
    
    
    int i, j, k, outl=0, outx = 0;
    double a,alam,alam2 = 0,alamin,b,disc,f2 = 0,fold2 = 0,rhs1,
      rhs2,slope,sum,temp,test,tmplam;
    m_check = 0;
    for (sum=0.0,i=0;i<=n-1;i++)sum += p[i]*p[i];
    sum = Math.sqrt(sum);
    if (m_Debug)
      System.out.print("fold:   " + Utils.doubleToString(fold,10,7)+ "\n");
    if (sum > stpmax)
      for (i=0;i<=n-1;i++) p[i] *= stpmax/sum;
    for (slope=0.0,i=0;i<=n-1;i++)
      slope += g[i]*p[i];
    if (m_Debug)
      System.out.print("slope:  " + Utils.doubleToString(slope,10,7)+ "\n");
    test=0.0;
    for (i=0;i<=n-1;i++) {
      temp=Math.abs(p[i])/Math.max(Math.abs(xold[i]),1.0);
      if (temp > test) test=temp;
    }
    alamin = m_TOLX/test;
    if (m_Debug)
      System.out.print("alamin: " + Utils.doubleToString(alamin,10,7)+ "\n");
    
    alam=1.0;
    for (k=0;;k++) {
      if (m_Debug)
	System.out.print("itteration: " + k + "\n");
      for (i=0;i<=n-1;i++) {
	x[i]=xold[i]+alam*p[i];
      }
      m_f = fmin(x,X,Y);
      if (m_Debug) {
	System.out.print("m_f:    " + 
			 Utils.doubleToString(m_f, 10, 7)+ "\n");
	System.out.print("cLL:    " + 
			 Utils.doubleToString(cLL(X,Y,x), 10, 7)+ "\n");
      
	System.out.print("alam:   " + alam + "\n");
	System.out.print("max:    " + 
			 Utils.doubleToString(fold-m_ALF*alam*slope,10,7)+ "\n");
      }
      if (Double.isInfinite(m_f)){
	if(m_Debug)
	  for (i=0;i<=n-1;i++) {
	    System.out.println("x[" + i + "] = " + 
			       Utils.doubleToString(x[i], 10, 4));
	  }
	System.err.println("Infinite");
	if(Double.isNaN(alam))
	  return;
      }
      if (Double.isInfinite(f2)){
	m_check = 2;
	return;
      }
      if (alam < alamin) {
	for (i=0;i<=n-1;i++) x[i]=xold[i];
	m_check=1;
	return;
      } else if (m_f <= fold-m_ALF*alam*slope) return;
      else {
	if (alam == 1.0)
	  tmplam = -1*slope/(2.0*(m_f-fold-slope));
	else {
	  rhs1 = m_f-fold-alam*slope;
	  rhs2 = f2-fold2-alam2*slope;
	  a=(rhs1/(alam*alam)-rhs2/(alam*alam2))/(alam-alam2);
	  b=(-1*alam2*rhs1/(alam*alam)+alam*rhs2/(alam2*alam2))/(alam-alam2);
	  if (a == 0.0) tmplam = -1*slope/(2.0*b);
	  else {
	    disc=b*b-3.0*a*slope;
	    if (disc < 0.0) disc = disc*-1;
	    tmplam=(-1*b+Math.sqrt(disc))/(3.0*a);
	  }
	  if (m_Debug)
	    System.out.print("newstuff: \n" + 
			     "a:   " + Utils.doubleToString(alam,10,7)+ "\n" +
			     "b:   " + Utils.doubleToString(alam,10,7)+ "\n" +
			     "disc:   " + Utils.doubleToString(alam,10,7)+ "\n" +
			     "tmplam:   " + alam + "\n" +
			     "alam:   " + Utils.doubleToString(alam,10,7)+ "\n");
	  if (tmplam>0.5*alam)
	    tmplam=0.5*alam;
	}
      }
      alam2=alam;
      f2=m_f;
      fold2=fold;
      alam=Math.max(tmplam,0.1*alam);
    }
  }
    
  /**
   * Globaly convergent Newtons method utilising 
   * line search and backtracking
   *
   * @param x initial point
   * @param n dimension of point
   * @param X instance data
   * @param Y class values
   * @exception Exception if an error occurs
   */
  private void newtn(double[] x, int n, double[][] X, double[] Y ) 
    throws Exception{
    
    int i,its,j,indx[],startnum;;
    double pLL,d,den,f,fold,stpmax,sum,temp,test,g[],p[],xold[];
     
    indx = new int[n];
    Matrix fjac = new Matrix(n,n);
    double[] fvec = new double[n];
    g = new double[n];
    p = new double[n];
    xold = new double[n];

    m_nn = n;
    m_f = fmin(x, X,Y);
    
    for (sum=0.0,i=0;i<=n-1;i++) sum+=x[i]*x[i];
    stpmax=m_STPMX*Math.max(Math.sqrt(sum),(float)n);

    m_LL=calculateLogLikelihood(X,Y,fjac,fvec);
    
    for (its=1,startnum=0;its<=m_MAXITS;its++) {
      pLL=m_LL;
      if (m_Debug) {
	System.out.println("\n-2 Log Likelihood = " 
			   + Utils.doubleToString(m_LL, 10, 5)
			   + ((m_LLn == m_LL) ? " (Null Model)" : ""));
	System.err.println("-2 Log Likelihood = " 
			   + Utils.doubleToString(m_LL, 10, 5)
			   + ((m_LLn == m_LL) ? " (Null Model)" : ""));
      }
      for (i=1;i<=n-1;i++) {
	for (sum=0.0,j=0;j<=n-1;j++) sum+=fjac.getElement(j,i)*fvec[i];
	g[i]=sum;
      }
      for (i=0;i<=n-1;i++) xold[i]=x[i];
      fold=m_f;
      for (i=0;i<=n-1;i++) p[i] = fvec[i];
      fjac.lubksb(fjac.ludcmp(),p);
      lnsrch(n,xold,fold,g,p,x,stpmax,X,Y);
      for (j = 0; j < x.length; j++) { 
	m_Par[j] = x[j];
      }
      m_LL = calculateLogLikelihood(X, Y, fjac, fvec);
      if(Math.abs(pLL-m_LL) < 0.00001)
	return;
      
    }
    throw new Exception("MAXITS exceeded in newtn");
  }

  /**
   * returns 1/2*cLL^2 at x
   *
   * @param x a point
   * @param X instance data
   * @param Y class values
   * @return function value
   */
  private double fmin( double[] x, double[][] X, double[] Y){
    int i;
    double sum;
    sum=cLL(X,Y,x);
    return sum * sum * 0.5;
  }
   

  /**
   * Returns probability.
   */
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
   * Returns the -2 Log likelihood with parameters in x
   *
   * @param X instance data
   * @param Y class values
   * @param x a point
   * @return function value
   */
  private double cLL(double[][] X, double[] Y, double[] x) {
  
    double LL = 0;
    for (int i=0; i < X.length; i++) {
      

      double v = x[0];
      for(int k = 1; k <= m_NumPredictors; k++)
	v += x[k] * X[i][k];
      v = 1 / (1 + Math.exp(-v));
      if ( v == 1.0 || v == 0.0 ) continue; 
      // Update the log-likelihood of this set of coefficients
      if( Y[i]==1 ) {
	LL = LL - 2 * Math.log(v);
      } else {
	LL = LL - 2 * Math.log(1 - v);
      }
    }
    return LL;
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
      if (p == 1.0 || p == 0.0) continue;
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

    // Add ridge adjustment to first derivative
    for (int j = 0; j < m_Par.length; j++) {
      deltas[j] -= 2 * m_Ridge * m_Par[j];
    }

    // Add ridge adjustment to second derivative
    for (int j = 0; j < Arr.length; j++) {
      Arr[j][j] += 2 * m_Ridge;
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
    return LL;
  }

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
    train = new Instances(train);
    train.deleteWithMissingClass();
    if (train.numInstances() == 0) {
      throw new Exception("No train instances without missing class value!");
    }
    m_ReplaceMissingValues = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValues.setInputFormat(train);
    train = Filter.useFilter(train, m_ReplaceMissingValues);
    m_NominalToBinary = new NominalToBinaryFilter();
    m_NominalToBinary.setInputFormat(train);
    train = Filter.useFilter(train, m_NominalToBinary);
    m_ClassIndex = train.classIndex();

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

    // Set up parameters for null model
    m_Par[0] = Math.log((sY1+1) / (sY0+1));
    for (int j = 1; j < m_Par.length; j++) {
      m_Par[j] = 0;
    }
    
    double x[] = new double[m_Par.length];
    for (int q=0; q < x.length;q++) x[q] = 0;

    newtn(x,x.length,X,Y);

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
    String result = "Logistic Regression (2 classes)";
    if (m_Par == null) {
      return result + ": No model built yet.";
    }
    result += "\n\nOverall Model Fit...\n" 
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









