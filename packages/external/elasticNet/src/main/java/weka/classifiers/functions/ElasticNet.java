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
 *    ElasticNet.java
 *    Copyright (C) 2015 Nikhil Murali Kishore
 *
 */

package weka.classifiers.functions;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.coordinate_descent.CoordinateDescent;
import weka.core.*;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * Class for solving the 'elastic net' problem for linear regression using coordinate descent. This is a Java implementation of a component of the R package glmnet. Can perform attribute selection based on the tuning parameters alpha and lambda. Model can deal with weighted instances. For more information, refer to the report PDF included in the package distribution.
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -m2 &lt;mode&gt;
 *  Whether to use Method 2 (covariance updates), y or n</pre>
 * 
 * <pre> -alpha &lt;alpha&gt;
 *  Set alpha value</pre>
 * 
 * <pre> -lambda_seq &lt;lambda_seq&gt;
 *  Provide custom lambda sequence of comma seperated non-negative floating point values OR leave blank to let classifier build own sequence</pre>
 * 
 * <pre> -thr &lt;thr&gt;
 *  Set convergence threshold</pre>
 * 
 * <pre> -mxit &lt;mxit&gt;
 *  Set maximum iterations for convergence</pre>
 * 
 * <pre> -numModels &lt;numModels&gt;
 *  Set number of models for pathwise descent</pre>
 * 
 * <pre> -infolds &lt;infolds&gt;
 *  Set number of folds for inner CV</pre>
 * 
 * <pre> -eps &lt;eps&gt;
 *  Set epsilon value for pathwise descent</pre>
 * 
 * <pre> -sparse &lt;sparse&gt;
 *  Set whether to turn on sparse updates, y or n</pre>
 * 
 * <pre> -stderr_rule &lt;stderr_rule&gt;
 *  Select model based on 1 S.E rule, y or n</pre>
 * 
 * <pre> -addStats &lt;addStats&gt;
 *  Whether to print additional statistics, y or n</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 * 
 <!-- options-end -->
 * @author  Nikhil Murali Kishore
 * @version $Revision: 1.0 $
 */
public class ElasticNet extends AbstractClassifier implements OptionHandler, WeightedInstancesHandler {

	/*** For serialization */
	private static final long serialVersionUID = -257033407031904867L;
	
	/*** Number of predictors */
	protected int m_numPredictors;
	
	/*** Number of instances */
	protected int m_numInstances;
	
	/*** Sequence of lambda values */
	protected String m_lambda_seq = ""; 
	
	/*** Nature of the penalty; range [0,1]; 0-ridge, 1-lasso */
	protected double m_alpha = 1e-3;
	
	/*** Class index for the dataset */
	protected int m_classIndex; 
	
	/*** Class name for toString() */
	protected String m_class_name;
	
	/*** Predictor names for toString() */
	protected String[] m_predictor_names;
	
	/*** Time taken to train classifier */
	protected String m_train_time;
	
	/*** Total number of models to be computed going from lambda_max to lambda_min */
	protected int m_numModels = 100;
	
	/*** Number of internal cross-validation folds */
	protected int m_numInnerFolds = 10;
	
	/*** To find lambda_min using lambda_min = Epsilon * lambda_max */
	protected double m_epsilon = 1e-4; 
	
	/*** Vector of logarithmically descending lambda values */
	protected double[] m_lambda_values; 
	
	/*** Index of the lambda value the final model is built on */
	protected int m_bestModel_index = 0;
	
	/*** Final coordinate descent model built using the best lambda value */
	protected CoordinateDescent m_modelUsed; 
	
	/*** Convergence threshold for coordinate descent */
	protected double m_threshold = 1e-7;
	
	/*** Max iterations for each coordinate descent run */
	protected int m_maxIt = (int) 1e7;
	
	/*** Whether to use covariance or naive updates */
	protected boolean m_covarianceMode = true;
	
	/*** Whether to treat the data as a sparse matrix */
	protected boolean m_sparse = false; 
	
	/*** Whether to use the 1 std error rule for choosing the best lambda */
	protected boolean m_stderr_rule = false;
	
	/*** Whether to output additional stats in toString() */
	protected boolean m_additionalStats = false;

	/**
	 * Returns the revision string.
	 *
	 * @return the revision
	 */
	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 11970 $");
	}

	/**
	 * Method used to train the classifier
	 * @param data set of instances serving as training data
	 * @throws Exception
	 */
	@Override
	public void buildClassifier(Instances data) throws Exception {

		getCapabilities().testWithFail(data);

		data = new Instances(data);

		String[] opts = getOptions();
		StringBuilder optList = new StringBuilder();
		for(String opt : opts) {
			optList.append(opt).append(" ");
		}
		
		m_numPredictors = data.numAttributes()-1;
		m_numInstances = data.numInstances();
		m_classIndex = data.classIndex();
		double t1 = System.nanoTime();
		Random rand = new Random(1);
		data.randomize(rand);
		double[] errorMetric_means = new double[m_numModels];
		double[] errorMetric_stdErrs = new double[m_numModels];
		
		m_modelUsed = new CoordinateDescent(data, m_alpha, m_threshold, m_maxIt, m_covarianceMode, m_sparse);
		if(m_lambda_values == null) {
			double lambdaZero = m_modelUsed.getLambdaZero();
			m_lambda_values = logspace(m_epsilon*lambdaZero, lambdaZero, m_numModels);
		}
		
		for(int m=0; m<m_numInnerFolds; m++) {
			Instances trainData = data.trainCV(m_numInnerFolds, m);
			Instances testData = data.testCV(m_numInnerFolds, m);			
			CoordinateDescent innerModel = new CoordinateDescent(trainData, m_alpha, m_threshold, m_maxIt, m_covarianceMode, m_sparse);
			
			for(int n=0; n<m_numModels; n++) {
				innerModel.setLambda(m_lambda_values[n]);
				innerModel.run();
				
				Evaluation evalObj = new Evaluation(data);
				evalObj.evaluateModel(innerModel, testData);
				double errorMetric = evalObj.errorRate();
				errorMetric *= errorMetric;
				errorMetric_means[n] += errorMetric;
				errorMetric_stdErrs[n] += errorMetric * errorMetric;
			}
		}
		
		if(m_numInnerFolds > 0) {
			for(int i=0; i<m_numModels; i++) {
				errorMetric_means[i] /= m_numInnerFolds;
				errorMetric_stdErrs[i] -= m_numInnerFolds*errorMetric_means[i]*errorMetric_means[i];
				errorMetric_stdErrs[i] /= m_numInnerFolds*(m_numInnerFolds-1);
				errorMetric_stdErrs[i] = Math.sqrt(errorMetric_stdErrs[i]);
			}
		}
		
		m_bestModel_index = minIndex(errorMetric_means);
		double minPlusSE = errorMetric_means[m_bestModel_index] + errorMetric_stdErrs[m_bestModel_index];
		int lambdaSE_index = 0;
		while(errorMetric_means[lambdaSE_index++] > minPlusSE);
		lambdaSE_index--;
		
		if(m_stderr_rule) {
			m_bestModel_index = lambdaSE_index;
		}
		
		for(int i=0; i<=m_bestModel_index; i++) {
			double currentLambda = m_lambda_values[i];
			m_modelUsed.setLambda(currentLambda);
			m_modelUsed.run();
		}
		
		double t2 = System.nanoTime();
		m_class_name = data.attribute(m_classIndex).name();
		m_predictor_names = new String[m_numPredictors];
		for(int m=0; m<=m_numPredictors; m++) {
			if(m==m_classIndex) continue;
			int i = m>m_classIndex ? m-1 : m; // skips class attribute
			m_predictor_names[i] = data.attribute(m).name();
		}
		m_train_time = String.format("%2.3f",(t2-t1)/1000000)+" ms";
	}
	
	/** 
	 * Classifies a test instance using the model saved in m_modelUsed
	 */
	@Override
	public double classifyInstance(Instance instance) throws Exception {
		return m_modelUsed.classifyInstance(instance);
	}
	
	/**
	 * Builds a vector of values between max and min of length len
	 * Values are evenly spaced on the log scale and are sorted in desc order
	 * @param min
	 * @param max
	 * @param len
	 * @return the vector of values
	 */
	public double[] logspace(double min, double max, int len) {
		double[] result = new double[len];
		double exponent = Math.pow(min/max, 1.0/(len-1));
		result[0] = max;
		for(int i=1; i<len; i++) {
			result[i] = result[i-1] * exponent;
		}
		return result;
	}
	
	/**
	 * Returns the index of the minimum element in an array
	 * @param array
	 * @return the index of the minimum element
	 */
	public int minIndex(double[] array) {
		int result = 0;
		for(int i=0; i<array.length; i++) {
			if(array[i] < array[result])
				result = i;
		}
		return result;
	}
	
	/** 
	 * Returns string representation of the classifier
	 */
	@Override
	public String toString() {
		if(m_modelUsed != null) {
			double[] coefficients = m_modelUsed.getCoefficients();
			double classMean = m_modelUsed.get_classMean();
			double classStdDev = m_modelUsed.get_class_stdDev();
			double coeffsMeansProduct = m_modelUsed.get_coeffsMeans_product();
			double intercept = classMean - coeffsMeansProduct*classStdDev;
			
			StringBuilder sb = new StringBuilder("Lambda sequence:\n");
			int oldLen = sb.length();
			for(double lambdaValue : m_lambda_values) {
				sb.append(String.format("%2.3f",lambdaValue*classStdDev)).append(',');
				int currLen = sb.length();
				if(currLen-oldLen > 120) {
					oldLen = currLen;
					sb.append("\n");
				}
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("\nBest model index: "+m_bestModel_index+"\n\n");
			if(!m_additionalStats) {
				sb = new StringBuilder();
			}
			
			sb.append(m_class_name+" = \n");
			for(int i=0; i<m_numPredictors; i++) {
				if(coefficients[i] != 0)
					sb.append(String.format("%2.3f",coefficients[i]*classStdDev)+" * "+m_predictor_names[i]+" + \n");
			}
			sb.append(String.format("%2.3f",intercept));
			return sb.toString();
		} else {
			return "Elastic net";
		}
	}
	
	/**
	   * Returns an enumeration describing the available options.
	   * 
	   * @return an enumeration of all the available options.
	   */
	@Override
	public Enumeration<Option> listOptions() {
				
		Vector<Option> newVector = new Vector<Option>();
		newVector.addElement(new Option("\tWhether to use Method 2 (covariance updates), y or n", "m2",
		      1, "-m2 <mode>"));
		newVector.addElement(new Option("\tSet alpha value", "alpha",
		      1, "-alpha <alpha>"));
		newVector.addElement(new Option("\tProvide custom lambda sequence of comma seperated non-negative floating point values "
			  + "OR leave blank to let classifier build own sequence", "lambda_seq",
		      1, "-lambda_seq <lambda_seq>"));
		newVector.addElement(new Option("\tSet convergence threshold", "thr",
		      1, "-thr <thr>"));
		newVector.addElement(new Option("\tSet maximum iterations for convergence", "mxit",
		      1, "-mxit <mxit>"));
		newVector.addElement(new Option("\tSet number of models for pathwise descent", "numModels",
		      1, "-numModels <numModels>"));
		newVector.addElement(new Option("\tSet number of folds for inner CV", "infolds",
		     1, "-infolds <infolds>"));
		newVector.addElement(new Option("\tSet epsilon value for pathwise descent", "eps",
		      1, "-eps <eps>"));
		newVector.addElement(new Option("\tSet whether to turn on sparse updates, y or n", "sparse",
		      1, "-sparse <sparse>"));
		newVector.addElement(new Option("\tSelect model based on 1 S.E rule, y or n", "stderr_rule",
		      1, "-stderr_rule <stderr_rule>"));
		newVector.addElement(new Option("\tWhether to print additional statistics, y or n", "addStats",
			      1, "-addStats <addStats>"));
		 
		newVector.addAll(Collections.list(super.listOptions()));
		return newVector.elements();
	}
	
	/**
	 * Validates a list of options
	 */
	public void validate() {
		if(m_alpha > 1) {
			m_alpha = 1;
		} else if(m_alpha < 1e-3) {
			m_alpha = 1e-3;
		}
		if(m_epsilon > 1 || m_epsilon < 0) {
			m_epsilon = 1e-4;
		}
		if(m_numModels < 2) {
			m_numModels = 100;
		}
		if(m_maxIt <= 0) {
			m_maxIt = (int) 1e7;
		}
		if(m_threshold <= 0) {
			m_threshold = 1e-7;
		}
		if(m_numInnerFolds < 2) {
			m_numInnerFolds = 10;
		}
		
		String[] splits = m_lambda_seq.split(",");
		int len = splits.length;
		double[] lambdaVals = new double[len];
		int index = 0;
		
		while(index < len) {
			try {
				lambdaVals[index] = Double.parseDouble(splits[index]);
				if(lambdaVals[index] < 0) { break; }
				index++;
			} catch(NumberFormatException e) { break; }
		}
		if(index == len) {
			Arrays.sort(lambdaVals);
			m_lambda_values = new double[len];
			m_numModels = len;
			
			while(--index >= 0) { 
				m_lambda_values[len-index-1] = lambdaVals[index];
			}
			if(len == 1) {
				m_numInnerFolds = 0;
			}
		} else {
			m_lambda_seq = "";
			m_lambda_values = null;
		}
	}
	
	public String globalInfo() {
		return "Class for solving the 'elastic net' problem for linear "
						+ "regression using coordinate descent. This is a Java implementation of a component of the R package glmnet. "
						+ "Can perform attribute selection based on the tuning parameters alpha and lambda. "
						+ "Model can deal with weighted instances. "
						+ "For more information, refer to the report PDF included in the package distribution.";
	}
	
	/** 
	 * Parses a list of options
	 * Valid options are:
	 * -m2, possible values 'y' or 'n'. Whether or not method2 (covariance update method) is used. Defaults to 'y'
	 * -alpha, any double value from 1e-3 to 1. Defaults to 1e-3
	 * -lambda_seq, custom lambda sequence. If absent, classifier builds own sequence
	 * -thr, sets convergence threshold. Should be non-negative. Defaults to 1e-7
	 * -mxit, sets maximum number of iterations. Defaults to 1e7
	 * -numModels, sets length of lambda sequence to be built. Defaults to 100
	 * -infolds, sets number of folds for internal cross validation. Defaults to 10
	 * -eps, sets epsilon value for building lambda sequence. Should be non-negative. Defaults to 1e-4
	 * -sparse, 'y' or 'n'. Whether to treat data as a sparse matrix. Defaults to 'n'
	 * -stderr_rule, 'y' or 'n'. Whether the 1 std error rule is used for choosing the best lambda. Defaults to 'n'
	 * -addStats, 'y' or 'n'. Whether to print additional statistics
	 */
	@Override
	public void setOptions(String[] options) throws Exception {
		
		String covarianceMode = Utils.getOption("m2", options);
		String alpha = Utils.getOption("alpha", options);
		String lambda_seq = Utils.getOption("lambda_seq", options);
		String threshold = Utils.getOption("thr", options);
		String maxIt = Utils.getOption("mxit", options);
		String numModels = Utils.getOption("numModels", options);
		String numInnerFolds = Utils.getOption("infolds", options);
		String epsilon = Utils.getOption("eps", options);
		String sparse = Utils.getOption("sparse", options);
		String stderr_rule = Utils.getOption("stderr_rule", options);
		String additionalStats = Utils.getOption("addStats", options);
		
		if(!"".equals(lambda_seq)) {
			m_lambda_seq = lambda_seq;
		} if(!"".equals(alpha)) {
			try { m_alpha = Double.parseDouble(alpha); }
			catch(NumberFormatException e) {}
		} if(!"".equals(threshold)) {
			try { m_threshold = Double.parseDouble(threshold); }
			catch(NumberFormatException e) {}
		} if(!"".equals(epsilon)) {
			try { m_epsilon = Double.parseDouble(epsilon); }
			catch(NumberFormatException e) {}
		} if(!"".equals(numModels)) {
			try { m_numModels = Integer.parseInt(numModels); }
			catch(NumberFormatException e) {}
		} if(!"".equals(numInnerFolds)) {
			try { m_numInnerFolds = Integer.parseInt(numInnerFolds); }
			catch(NumberFormatException e) {}
		} if(!"".equals(maxIt)) {
			try { m_maxIt = Integer.parseInt(maxIt); }
			catch(NumberFormatException e) {}
		} if(!"".equals(covarianceMode)) {
			m_covarianceMode = "n".equalsIgnoreCase(covarianceMode) ? false : true;
		} if(!"".equals(sparse)) {
			m_sparse = "y".equalsIgnoreCase(sparse) ? true : false;
		} if(!"".equals(stderr_rule)) {
			m_stderr_rule = "y".equalsIgnoreCase(stderr_rule) ? true : false;
		} if(!"".equals(additionalStats)) {
			m_additionalStats = "y".equalsIgnoreCase(additionalStats) ? true : false;
		}
		super.setOptions(options);
		validate();
	}
	
	/**
	   * Gets the current settings of the classifier.
	   * 
	   * @return an array of strings suitable for passing to setOptions
	   */
	@Override
	public String[] getOptions() {
				
		Vector<String> result = new Vector<String>();
		result.add("-m2");
		result.add(m_covarianceMode==true ? "y" : "n");
	    result.add("-alpha");
	    result.add(String.valueOf(m_alpha));
	    result.add("-lambda_seq");
	    result.add(String.valueOf(m_lambda_seq));
	    result.add("-thr");
	    result.add(String.valueOf(m_threshold));
	    result.add("-mxit");
	    result.add(String.valueOf(m_maxIt));
	    result.add("-numModels");
	    result.add(String.valueOf(m_numModels));
	    result.add("-infolds");
	    result.add(String.valueOf(m_numInnerFolds));
	    result.add("-eps");
	    result.add(String.valueOf(m_epsilon));
	    result.add("-sparse");
		result.add(m_sparse==true ? "y" : "n");
		result.add("-stderr_rule");
		result.add(m_stderr_rule==true ? "y" : "n");
		result.add("-addStats");
		result.add(m_additionalStats==true ? "y" : "n");

	    Collections.addAll(result, super.getOptions());
	    validate();
	    return result.toArray(new String[result.size()]);
	}
	
	/** 
	 * Returns default capabilities for this class.
	 */
	@Override
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		
		// Attributes
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		
		// Class
		result.enable(Capability.NUMERIC_CLASS);
		return result;
	}
	
	/**
	 * Generates a linear regression model solving the elastic net problem
	 * @param ops
	 */
	public static void main(String[] ops) {
		runClassifier(new ElasticNet(),ops);
	}
	
	/**
	   * Gets the method used to determine the user-defined lambda sequence
	   * 
	   * @return a comma separated list of lambda values
	*/
	public String getCustom_lambda_sequence() {		
		return m_lambda_seq;
	}
	/**
	   * Sets the method used to provide an user-defined lambda sequence
	   * 
	   * @param lambda_seq, a comma separated list of lambda values
	*/
	public void setCustom_lambda_sequence(String lambda_seq) {
		this.m_lambda_seq = lambda_seq;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String custom_lambda_sequenceTipText() {
		return "Provide custom lambda sequence of comma seperated non-negative floating point values "
				+ "OR leave blank to let classifier build own sequence";
	}
	
	/**
	   * Gets the method used to determine the alpha value
	   * 
	   * @return the value of alpha
	*/
	public double getAlpha() {
		return m_alpha;
	}
	/**
	   * Sets the method used to choose an alpha value
	   * 
	   * @param alpha, the value of alpha to be set
	*/
	public void setAlpha(double alpha) {
		this.m_alpha = alpha;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String alphaTipText() {
	    return "Set the alpha value";
	}
	
	/**
	   * Gets the method used to determine whether the 1 std error rule will be applied
	   * 
	   * @return a boolean parameter determining whether the 1 std error rule will be applied
	*/
	public boolean getUse_stderr_rule() {
		return m_stderr_rule;
	}
	/**
	   * Sets the method used to choose whether the 1 std error rule should be applied
	   * 
	   * @param stderr_rule, boolean parameter choosing whether the 1 std error rule will be applied
	*/
	public void setUse_stderr_rule(boolean stderr_rule) {
		this.m_stderr_rule = stderr_rule;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String use_stderr_ruleTipText() {
	    return "If true, the one standard error rule is applied for choosing lambda";
	}
	
	/**
	   * Gets the method used to determine whether method2 will be applied
	   * 
	   * @return a boolean parameter determining whether method2 will be applied
	*/
	public boolean getUse_method2() {
		return m_covarianceMode;
	}
	/**
	   * Sets the method used to choose whether method2 should be applied
	   * 
	   * @param covarianceMode, boolean parameter choosing whether method2 should be applied
	*/
	public void setUse_method2(boolean covarianceMode) {
		this.m_covarianceMode = covarianceMode;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String use_method2TipText() {
	    return "If true, the covariance update method is used";
	}
	
	/**
	   * Gets the method used to determine the length of lambda sequence to be built
	   * 
	   * @return the length of the lambda sequence to be built
	*/
	public int getNumModels() {
		return m_numModels;
	}
	/**
	   * Sets the method used to choose the length of lambda sequence to be built
	   * 
	   * @param numModels, the length of the lambda sequence to be built
	*/
	public void setNumModels(int numModels) {
		this.m_numModels = numModels;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String numModelsTipText() {
	    return "Set the length of the lambda sequence to be generated";
	}
	
	/**
	   * Gets the method used to determine the epsilon value
	   * 
	   * @return the value of epsilon
	*/
	public double getEpsilon() {
		return m_epsilon;
	}
	/**
	   * Sets the method used to choose an epsilon value
	   * 
	   * @param epsilon, the value of epsilon
	*/
	public void setEpsilon(double epsilon) {
		this.m_epsilon = epsilon;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String epsilonTipText() {
	    return "Set the epsilon value for generating the lambda sequence";
	}
	
	/**
	   * Gets the method used to determine the number of folds in internal cross validation
	   * 
	   * @return the number of internal cross validation folds
	*/
	public int getNumInnerFolds() {
		return m_numInnerFolds;
	}
	/**
	   * Sets the method used to choose the number of folds in internal cross validation
	   * 
	   * @param numInnerFolds, the number of internal cross validation folds
	*/
	public void setNumInnerFolds(int numInnerFolds) {
		this.m_numInnerFolds = numInnerFolds;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String numInnerFoldsTipText() {
	    return "Set the number of folds for internal cross validation";
	}
	
	/**
	   * Gets the method used to determine the convergence threshold
	   * 
	   * @return the convergence threshold
	*/
	public double getThreshold() {
		return m_threshold;
	}
	/**
	   * Sets the method used to choose a convergence threshold
	   * 
	   * @param threshold, the convergence threshold
	*/
	public void setThreshold(double threshold) {
		this.m_threshold = threshold;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String thresholdTipText() {
	    return "Set the convergence threshold";
	}
	
	/**
	   * Gets the method used to determine the maximum number of iterations
	   * 
	   * @return the maximum number of iterations
	*/
	public int getMaxIt() {
		return m_maxIt;
	}
	/**
	   * Sets the method used to choose the maximum number of iterations
	   * 
	   * @param maxIt, the maximum number of iterations
	*/
	public void setMaxIt(int maxIt) {
		this.m_maxIt = maxIt;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String maxItTipText() {
	    return "Set the maximum number of iterations for a coordinate descent run";
	}
	
	/**
	   * Gets the method used to determine whether the data will be treated as a sparse matrix
	   * 
	   * @return a boolean value determining whether the data will be treated as a sparse matrix
	*/
	public boolean getSparse() {
		return m_sparse;
	}
	/**
	   * Sets the method used to choose whether to treat the data as sparse matrix
	   * 
	   * @param sparse, a boolean value choosing whether the data should be treated as a sparse matrix
	*/
	public void setSparse(boolean sparse) {
		this.m_sparse = sparse;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String sparseTipText() {
	    return "If true, data is treated as sparse matrix";
	}
	
	/**
	   * Gets the method used to determine whether additional statistics will be printed
	   * 
	   * @return a boolean value determining whether additional statistics will be printed
	*/
	public boolean getAdditionalStats() {
		return m_additionalStats;
	}
	/**
	   * Sets the method used to choose whether to print additional statistics
	   * 
	   * @param additionalStats, a boolean value choosing whether to print additional statistics
	*/
	public void setAdditionalStats(boolean additionalStats) {
		this.m_additionalStats = additionalStats;
	}
	/**
	   * Returns the tip text for this property.
	   * 
	   * @return tip text for this property suitable for displaying in the
	   *         explorer/experimenter gui
	*/
	public String additionalStatsTipText() {
	    return "If true, additional statistics are printed";
	}
}

