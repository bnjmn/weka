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
 *    Copyright (C) 2001 Len Trigg, Eibe Frank, Tony Voyle, Xin Xu
 *
 */

package weka.classifiers;

import java.util.*;
import java.io.*;
import weka.core.*;
import weka.filters.*;

/**
 * Class for building and using a multi-class logistic regression model
 * with a ridge estimator.  <p>
 * 
 * There are some modifications, however, compared to the paper:
 *
 * If there are k classes for n instances with m attributes, 
 * the parameter matrix B to be calculated will be an m*(k-1) matrix
 *
 * The probability for class j except the last class is 
 * Pj(Xi) = exp(XiBj)/((sum[j=1..(k-1)]exp(Xi*Bj))+1) 
 * The last class has probability
 * 1-(sum[j=1..(k-1)]Pj(Xi)) = 1/((sum[j=1..(k-1)]exp(Xi*Bj))+1) 
 *
 * The (negative) log-likelihood is thus:
 * L = -sum[i=1..n]{
 * sum[j=1..(k-1)](Yij * ln(Pj(Xi))) +
 * (1 - (sum[j=1..(k-1)]Yij)) * ln(1 - sum[j=1..(k-1)]Pj(Xi))
 * } + ridge * (B^2)
 *
 * In order to find the matrix B for which L is minimised, the Newton-Raphson Method 
 * is used to identify the parameters for which the first partial derivatives equal 0.
 *  
 * The first partial derivatives for Bxy are:
 * D1(Bxy) = -sum[i=1..n]{ 
 * Xix * (Yiy - Py(Xi)) 
 * } + 2 * ridge * Bxy
 * 
 * Since there are m*(k-1) variables, there are m*(k-1) functions to be solved 
 * simultaneously.  Thus the Jacobian matrix containing the second partial derivatives
 * for Bpq (i.e. the partial derivative of D1(Bxy) for Bpq) is a [m*(k-1)]*[m*(k-1)] 
 * matrix as follows:
 * 
 * if ((y == q) && (x == p))
 * D2 = sum[i=1..n]{ 
 * Xix * (Py(Xi) * (1 - Py(Xi)) * Xix
 * } + 2 * ridge * I
 *
 * if ((y == q) && (x != p))
 * D2 = sum[i=1..n]{ Xix * (Py(Xi) * (1 - Py(Xi)) * Xip }
 *
 * if ((y != q) && (x != p))
 * D2 = -sum[i=1..n]{ Xix * (Py(Xi) * Pq(Xi)) * Xip }
 *
 * The globally convergent Newtons Method utilized in this class is adapted from
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
 * -P precision <br>
 * Set the precision of stopping criteria in Newton method.<p>
 *
 * -R ridge <br>
 * Set the ridge parameter for the log-likelihood.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Tony Voyle (tv6@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.16 $ 
 */
public class Logistic extends DistributionClassifier implements OptionHandler {
    
    /** The log-likelihood of the built model */
    protected double m_LL;
    
    /** The log-likelihood of the null model */
    protected double m_LLn;
    
    /** The coefficients of the model */
    protected double [][] m_Par;
    
    /** The number of attributes in the model */
    protected int m_NumPredictors;
    
    /** The index of the class attribute */
    protected int m_ClassIndex;
    
    /** The number of the class labels */
    protected int m_NumClasses;
    
    /** The ridge parameter. */
    protected double m_Ridge = 1e-8;
    
    /** The filter used to make attributes numeric. */
    private NominalToBinaryFilter m_NominalToBinary;
    
    /** The filter used to get rid of missing values. */
    private ReplaceMissingValuesFilter m_ReplaceMissingValues;
    
    /** Debugging output */
    protected boolean m_Debug;

    /** The precision parameter */   
    private double m_Precision = 1.0e-45;

    /** The following parameters are used in Newton method */
    private double m_f;
    
    private int m_nn;
    
    private int m_check;
    
    private double m_ALF = 1.0e-4;
    
    private double m_TOLF = 1.0e-4;
    
    private double m_TOLX = 1.0e-7;
    
    private double m_TOLMIN = 1.0e-6;
    
    private double m_STPMX = 100.0;
    
    private int m_MAXITS = 200;

    private double m_ZERO = 0.0;
    
    /**
     * For one class, finds a new point x in the direction p from
     * a point xold at which the value of the function
     * has decreased sufficiently.
     *
     * @param n number of variables for one class
     * @param c number of classes
     * @param xold old point
     * @param fold value at that point
     * @param g gradient at that point
     * @param p direction
     * @param x new value along direction p from xold
     * @param stpmax maximum step length
     * @param X instance data
     * @param Y class values
     * @exception Exception if an error occurs
     */
    private void lnsrch(int n, int c, double[][] xold, double fold, double[] g, double[] p, 
			double[][] x, double stpmax, double[][] X, double[][] Y)
	throws Exception {
	
	int i, j, k, outl=0, outx = 0,len=(n*c);
	double a,alam,alam2 = 0,alamin,b,disc=0,f2 = 0,fold2 = 0,rhs1,rhs2,slope,sum,temp,test,tmplam;
	m_check = 0;
	
	// Scale the step
	for (sum=0.0,i=0;i<len;i++){
	    sum += p[i]*p[i];
	}
	sum = Math.sqrt(sum);
	if (m_Debug)
	    System.out.print("fold:   " + Utils.doubleToString(fold,10,7)+ "\n");
	if (sum > stpmax)
	    for (i=0;i<len;i++) p[i] *= stpmax/sum;  
	
	// Compute LAMBDAmin
	test=0.0;
	for (i=0;i<c;i++) 
	    for(j=0;j<n;j++){
		temp=Math.abs(p[i*n+j])/Math.max(Math.abs(xold[j][i]),1.0);
		if (temp > test) test=temp;
	    }
	
	alamin = m_TOLX/test;
	if (m_Debug)
	    System.out.print("alamin: " + Utils.doubleToString(alamin,10,7)+ "\n");
	
	alam=1.0;  // Always try full newton step 
	
	// Compute initial rate of decrease 
	for (slope=0.0,i=0;i<len;i++)
	    slope += g[i]*p[i];
	
	if (m_Debug){
	    System.out.print("slope:  " + Utils.doubleToString(slope,10,7)+ "\n");
	}
	
	// Iteration of one newton step, if necessary, backtracking is done
	for (k=0;;k++) {
	    if (m_Debug)
		System.out.print("iteration: " + k + "\n");
	    for (i=0;i<n;i++)
		for(j=0;j<c;j++)
		    x[i][j] = xold[i][j]+alam*p[j*n+i];  // Compute xnew	
	    m_f = fmin(x,X,Y);                           // Compute fnew
	    
	    if (m_Debug) {
		System.out.print("m_f:    " + 
				 Utils.doubleToString(m_f, 10, 7)+ "\n");
		System.out.print("alam:   " + alam + "\n");
		System.out.print("max:    " + 
				 Utils.doubleToString(fold+m_ALF*alam*slope,10,7)+ "\n");
	    }
	    if (Double.isInfinite(m_f)){
		if(m_Debug)
		    for (i=0;i<n;i++) 
			for(j=0;j<c;j++)
			    System.out.println("x[" + i + "] ["+ j + "] = " + 
					       Utils.doubleToString(x[i][j], 10, 4));
		
		System.err.println("Infinite");
		if(Double.isNaN(alam))
		    return;
	    }
	    if (Double.isInfinite(f2)){
		m_check = 2;
		return;
	    }
	    
	    if (alam < alamin) {                     // Convergence on delta(x)
		for (i=0;i<n;i++)
		    for(j=0;j<c;j++)
			x[i][j]=xold[i][j];
		m_check=1;
		return;
	    } 
	    else if (m_f <= fold+m_ALF*alam*slope){  // Sufficient function decrease
		return;           
	    }
	    else {                                   // Backtracking
		if (Utils.eq(alam, 1.0))             // First time
		    tmplam = -slope/(2.0*(m_f-fold-slope));
		else {                               // Subsequent backtrack 
		    rhs1 = m_f-fold-alam*slope;
		    rhs2 = f2-fold2-alam2*slope;
		    a=(rhs1/(alam*alam)-rhs2/(alam2*alam2))/(alam-alam2);
		    b=(-alam2*rhs1/(alam*alam)+alam*rhs2/(alam2*alam2))/(alam-alam2);
		    if (Utils.eq(a, 0)) tmplam = -slope/(2.0*b);
		    else {
			disc=b*b-3.0*a*slope;
			if (disc < 0.0) disc = -disc;
			tmplam=(-1*b+Math.sqrt(disc))/(3.0*a);
		    }
		    if (m_Debug)
			System.out.print("newstuff: \n" + 
					 "a:   " + Utils.doubleToString(a,10,7)+ "\n" +
					 "b:   " + Utils.doubleToString(b,10,7)+ "\n" +
					 "disc:   " + Utils.doubleToString(disc,10,7)+ "\n" +
					 "tmplam:   " + alam + "\n" +
					 "alam:   " + Utils.doubleToString(alam,10,7)+ "\n");
		    if (tmplam>0.5*alam)
			tmplam=0.5*alam;             // lambda <= 0.5*lambda1
		}
	    }
	    alam2=alam;
	    f2=m_f;
	    fold2=fold;
	    alam=Math.max(tmplam,0.1*alam);          // lambda >= 0.1*lambda1
	}                                            // Try again
    }
    
    /**
     * Globaly convergent Newtons method utilising line search and backtracking
     *
     * @param x initial point
     * @param n dimension of one point, i.e. number of non-class attributes + 1
     * @param k dimension of one dimension of one point, i.e. (numClasses-1)
     * @param X instance data
     * @param Y class values
     * @exception Exception if an error occurs
     */
    private void newtn(double[][] x, int n, int k, double[][] X, double[][] Y ) 
	throws Exception{
	
	int i,its,j,indx[],startnum, len=(n*k);
	double pLL,d,den,f,fold,stpmax=0,sum,temp,test,g[],p[],xold[][];
	
	indx = new int[n];
	Matrix fjac = new Matrix (len, len);
	
	double[] fvec = new double[len];
	g = new double [len]; 
	p = new double [len]; 
	xold = new double[n][k];
	
	m_f = fmin(x,X,Y);
	
	//Calculate stpmax for line search
	for(int h=0; h < k; h++){
	    for (sum=0.0, i=0; i<=n-1;i++) 
		sum+=x[i][h]*x[i][h];
	    stpmax=m_STPMX*Math.max(Math.sqrt(sum),(float)n);
	}
	
	m_LL = calculateLogLikelihood(X,Y,fjac,fvec);  //fjac and fvec is also calculated
	
	for (its=1,startnum=0;its<=m_MAXITS;its++){
	    temp=0;
	    pLL=m_LL; 
	    if (m_Debug) {
		System.out.println("\n-2 Log Likelihood = " 
				   + Utils.doubleToString(m_LL, 10, 5)
				   + ((m_LLn == m_LL) ? " (Null Model)" : ""));
		System.err.println("\n-2 Log Likelihood = " 
				   + Utils.doubleToString(m_LL, 10, 5)
				   + ((m_LLn == m_LL) ? " (Null Model)" : ""));
	    }
	    for (i=1;i<len;i++){ 
		for (sum=0.0,j=0;j<len;j++) sum += fvec[j]*fjac.getElement(j,i);
		g[i]=sum; 
	    }
	    
	    for (i=0;i<n;i++) 
		for(j=0;j<k;j++)
		    xold[i][j]=x[i][j];
	    
	    fold = m_f;
	    for (i=0;i<len;i++)
		p[i] = -fvec[i];                // Right-hand side for linear equations
	    
	    /** 
	     * Solve linear equation J*p = -F using LU decomposition and 
	     * forward & backward substitution.
	     * Use lucmp() to get the LU decomposition of fjac, then use it as input 
	     *  of lubksb to compute LU backward substitution. 
	     * The results are the inverse of fjac J`.  Thus -F*J` is stored in p.
	     */
	    if (m_Debug) 
		System.out.println("\nSolving the linear equation J*p = -F ..."); 
	    fjac.lubksb(fjac.ludcmp(),p);
	    
	    /** One step:try to use line search to calculate new x and m_f for one class*/
	    lnsrch(n,k,xold,fold,g,p,x,stpmax,X,Y);  
	    
	    for(i = 0; i < x.length; i++)
		for (j = 0; j < k; j++)  
		    m_Par[i][j] = x[i][j]; 
	    
	    m_LL=calculateLogLikelihood(X, Y, fjac, fvec);
	    
	    if(Math.abs(pLL-m_LL) < m_Precision)
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
    private double fmin( double[][] x, double[][] X, double[][] Y){
	double sum;
	sum=cLL(X,Y,x);
	return (sum * sum * 0.5);
    }
    
    
    /**
     * Returns probability.
     */
    private static double Norm(double z) { 
	return Statistics.chiSquaredProbability(z * z, 1);
    }
    
    /**
     * Evaluate the probability for this point using the current coefficients
     *
     * @param instDat the instance data
     * @return the probability for this instance
     */
    private double[] evaluateProbability(double [] instDat) {
	int c = m_NumClasses-1;
	double[] v = new double[c+1];
	double[] p = new double[c+1];
	
	for(int j = 0; j < c; j++){
	    for(int k = 0; k <= m_NumPredictors; k++){
		if(k == 0)
		    v[j] = m_Par[k][j];
		else
		    v[j] += m_Par[k][j] * instDat[k];
	    }
	}
	v[c] = 0;
	
	for(int m=0; m <= c; m++){
	    double sum = 0;
	    for(int n=0; n < c; n++)
		sum += Math.exp(v[n] - v[m]);
	    p[m] = 1 / (sum + Math.exp(-v[m]));
	}
	
	return p;
    }
    
    /**
     * Returns the -2 Log likelihood with parameters in x
     *
     * @param X instance data
     * @param Y class values
     * @param x a point
     * @return function value
     */
    private double cLL(double[][] X, double[][] Y, double[][] x) {
	double LL=0;
	int c = m_NumClasses - 1, np = m_NumPredictors + 1; 
	double[] v = new double[c+1];
	double[] p = new double[c+1];
	
    points:
	for (int i=0; i < X.length; i++) {
	    for(int j = 0; j < c; j++){
		for(int k = 0; k < np ; k++){
		    if(k == 0)
			v[j] = x[k][j];
		    else
			v[j] += X[i][k] * x[k][j];
		}
	    }
	    v[c] = 0;
	    
	    for(int m=0; m <= c; m++){
		double sum = 0;
		for(int n=0; n < c; n++)
		    sum += Math.exp(v[n] - v[m]);
		p[m] = 1 / (sum + Math.exp(-v[m]));
		if(Utils.eq(p[m],1.0))
		    continue points;
	    }
	    
	    //Update the log-likelihood of this set of coefficients
	    int sumY=0;
	    for(int l=0; l < c; l++){
		if( Y[i][l] == 1) {
		    if(Utils.smOrEq(p[l], m_ZERO))
			continue points;
		    LL = LL - 2 * Math.log(p[l]);
		    sumY = 1;
		    break;
		} 
	    }
	    
	    if(sumY == 0){
		if(Utils.smOrEq(p[c], m_ZERO))
		    continue points;
		LL = LL - 2 * Math.log(p[c]);
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
    private double calculateLogLikelihood(double [][] X, double [][] Y,
					  Matrix jacobian, double[] deltas) {
	double LL = 0;	
	double[] p;
	int n = m_NumPredictors + 1;  // Plus null model
	int k = m_NumClasses - 1, len = (n*k);
	
	for (int j = 0; j < len; j++) {
	    deltas[j] = 0;
	    for (int l = j; l < len; l++){
		jacobian.setElement(j,l,0); //initialize half of the matrix
	    }
	}
	
	// For each data point
    outer_loop:
	for (int i = 0; i < X.length; i++) {	
	    // Evaluate the probability for this point using the current coefficients
	    p = evaluateProbability(X[i]);
	    int sumY=0;
	    
	    // For each class
	    for (int c = 0; c < k; c++){	
		// Update the log-likelihood of this set of coefficients
		if( Y[i][c]==1 ) {
		    if(Utils.smOrEq(p[c], m_ZERO) || (Utils.eq(p[c], 1.0)))
			continue outer_loop;
		    LL = LL - 2 * Math.log(p[c]);
		    sumY = 1;
		    break;
		} 
	    }
	    if(sumY == 0){
		if(Utils.smOrEq(p[k], m_ZERO) || (Utils.eq(p[k], 1.0)))
		    continue outer_loop;
		LL = LL - 2 * Math.log(p[k]);
	    }
	    
	    //Update the first derivatives
	    for(int c=0; c < k; c++){
		double z = Y[i][c] - p[c];       // The error of this prediction
		for (int j = 0; j < n; j++) {
		    double xij = X[i][j];
		    deltas[c*n+j] -= xij * z;    //partial derivative of Bjc
		}
	    }
	    
	    //Update the second derivatives
	    for(int c=0; c < k; c++){
		double w = p[c] * (1 - p[c]);    // Weight
		for(int b = c; b < k; b++)
		    for(int j = 0; j < n; j++){ 
			double xij = X[i][j];
			// Partial derivative on B of the same class
			if(b == c)             
			    for(int a = j; a < n; a++){
				double value = jacobian.getElement((c*n+j),(b*n+a));
				value = value + (xij * w * X[i][a]);
				jacobian.setElement((c*n+j),(b*n+a),value);
			    }
			// Partial derivative on B of the different class
			else                   
			    for(int a = 0; a < n; a++){
				double value = jacobian.getElement((c*n+j),(b*n+a));
				value = value - (xij * (p[c] * p[b]) * X[i][a]);
				jacobian.setElement((c*n+j),(b*n+a),value);
			    }
		    }
	    }
	}
	
	for(int c = 0; c < k; c++){
	    // Add ridge adjustment to the first derivative
	    for (int j = 0; j < m_Par.length; j++){
		deltas[c*(m_Par.length)+j] += 2 * m_Ridge * m_Par[j][c];
	    }
	    
	    // Add ridge adjustment to the second derivative
	    for (int j = 0; j < len; j++) {
		double value = jacobian.getElement(j,j);
		value += 2 * m_Ridge;
		jacobian.setElement(j,j,value);
	    }
	    
	    // Fill out the rest of the array
	    for (int j = 1; j < len; j++)
		for (int l = 0; l < j; l++)
		    jacobian.setElement(j,l,(jacobian.getElement(l,j)));
	}
	return LL;
    }
    
    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(3);
	newVector.addElement(new Option("\tTurn on debugging output.",
					"D", 0, "-D"));
	newVector.addElement(new Option("\tSet the precision of stopping criteria in Newton method.",
					"P", 1, "-P <precision>"));
	newVector.addElement(new Option("\tSet the ridge in the log-likelihood.",
					"R", 1, "-R <ridge>"));
	return newVector.elements();
    }
    
    /**
     * Parses a given list of options. Valid options are:<p>
     *
     * -D <br>
     * Turn on debugging output.<p>
     *
     * -P precision <br>
     * Set the precision of stopping criteria in Newton method.<p>
     *
     * -R ridge <br>
     * Set the ridge parameter for the log-likelihood.<p>
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {
	setDebug(Utils.getFlag('D', options));
	
	String precisionString = Utils.getOption('P', options);
	if (precisionString.length() != 0) 
	    m_Precision = Double.parseDouble(precisionString);
	else 
	    m_Precision = 1.0e-45;
	
	String ridgeString = Utils.getOption('R', options);
	if (ridgeString.length() != 0) 
	    m_Ridge = Double.parseDouble(ridgeString);
	else 
	    m_Ridge = 1.0e-8;
    }
    
    /**
     * Gets the current settings of the classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String [] getOptions() {
	
	String [] options = new String [5];
	int current = 0;
	
	if (getDebug()) {
	    options[current++] = "-D";
	}
	options[current++] = "-P";
	options[current++] = ""+m_Precision;
	options[current++] = "-R";
	options[current++] = ""+m_Ridge;
	
	while (current < options.length) 
	    options[current++] = "";
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
     * Sets the precision of stopping criteria in Newton method.
     *
     * @param precision the precision
     */
    public void setPrecision(double precision) {
	m_Precision = precision;
    }
    
    /**
     * Gets the precision of stopping criteria in Newton method.
     *
     * @return the precision
     */
    public double getPrecision() {
	return m_Precision;
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
     * Builds the classifier
     *
     * @param train the training data to be used for generating the
     * boosted classifier.
     * @exception Exception if the classifier could not be built successfully
     */
    public void buildClassifier(Instances train) throws Exception {
	
	if (train.classAttribute().type() != Attribute.NOMINAL) {
	    throw new Exception("Class attribute must be nominal.");
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
	m_NumClasses = train.numClasses();
	
	int nK = m_NumClasses - 1;                     // Only K-1 class labels needed 
	int nR = m_NumPredictors = train.numAttributes() - 1;
	int nC = train.numInstances();
	
	double [][] X  = new double [nC][nR + 1];       // Data values
	double [][] Y  = new double [nC][nK];           // Class values
	double [] xMean= new double [nR + 1];           // Attribute means
	double [] xSD  = new double [nR + 1];           // Attribute stddev's
	double [] sY = new double [nK + 1];             // Number of classes
	
	if (m_Debug) {
	    System.out.println("Extracting data...");
	}
	
	for (int s=0; s < nR+1; s++){
	    xMean[s] = 0;
	    xSD[s] = 0;
	}
	for(int t=0; t < nK+1; t++)
	    sY[t] = 0;

	for (int i = 0; i < X.length; i++) {
	    // initialize X[][]
	    Instance current = train.instance(i);
	    int trueClass = (int)current.classValue();  // Class value starts from 0
	  
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
	    
	    // Class count, initialize Y[][]
	    for (int l = 0; l < nK; l++){	
		if (l == trueClass){
		    Y[i][l] = 1;
		    sY[l] = sY[l]+1;
		}
		else
		    Y[i][l] = 0;
	    }

	    if(trueClass == nK)
		sY[nK] = sY[nK]+1;   
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
	    for (int m = 0; m <= nK; m++)
		System.out.println(sY[m] + " cases have class " + m);
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
	m_Par = new double [nR + 1][nK];   // Coefficients
	double LLp = 2e+10;                // Log-likelihood on previous iteration
	m_LL  = 1e+10;                     // Log-likelihood of current iteration
	
	// Set up parameters for null model
	for (int j = 0; j < m_Par.length; j++){ 	
	    for (int k = 0; k < nK; k++){	
		if (j == 0)
		    m_Par[j][k] = Math.log((sY[k]+1) / (sY[nK]+1));
		else
		    m_Par[j][k] = 0;
	    }
	}

	m_LLn = cLL(X, Y, m_Par);       // Log-likelihood of null hypothesis

	double x[][] = new double[nR + 1][nK];
	for (int q=0; q < x.length;q++)
	    for (int r=0; r < nK; r++)
		x[q][r] = m_Par[q][r];
	
	newtn(x, x.length, nK, X, Y);
	
	if (m_Debug)
	    System.out.println(" -------------<Converged>--------------");
	
	// Convert coefficients back to non-normalized attribute units
	for(int i=0; i < nK; i++){
	    for(int j = 1; j <= nR; j++) {
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
	
	double [] distribution = evaluateProbability(instDat);
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
    String result = "Logistic Regression";
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
	    double ORc = Math.exp( m_Par[j][k] );
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
