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
 *    CoordinateDescent.java
 *    Copyright (C) 2015 Nikhil Murali Kishore
 *
 */

package weka.classifiers.functions.coordinate_descent;

import java.util.ArrayList;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Class implementing the cyclic coordinate descent algorithm. 
 * Used to obtain a single set of coefficients from a given lambda and alpha.
 */
public class CoordinateDescent extends AbstractClassifier {
	
	/*** For serialization */
	private static final long serialVersionUID = 3058063989927918091L;
	
	/*** The dataset used to build the models */
	protected Instances m_dataset;
	
	/*** Number of predictors */
	protected int m_numPredictors;
	
	/*** Number of instances */
	protected int m_numInstances;
	
	/*** Magnitude of the penalty */
	protected double m_lambda;
	
	/*** Nature of the penalty; range [0,1]; 0-ridge, 1-lasso */
	protected double m_alpha;
	
	/*** Class index for the dataset */
	protected int m_classIndex;
	
	/*** Covariances between attribute vectorss */
	protected double[][] m_covariance_matrix;
	
	/*** Whether a row in the covariance matrix has been filled */
	protected boolean[] m_covariances_filled;
	
	/*** Equals the mean of the class attribute */
	protected double m_classMean = 0.0;	
	
	/*** Equals the std deviation of the class attribute */
	protected double m_class_stdDev = 0.0;
	
	/*** Coefficient vector */
	protected double[] m_coefficients;
	
	/*** Whether to use covariance or naive updates */
	protected boolean m_covarianceMode = true;
	
	/*** Whether to treat the data as a sparse matrix */
	protected boolean m_sparse = false;
	
	/*** Convergence threshold for coordinate descent */
	protected double m_threshold;
	
	/*** Indices of the sparse predictors */
	protected ArrayList<ArrayList<Integer>> m_sparseIndices;
	
	/*** Essentially lambda_max */
	protected double m_lambdaZero;
	
	/*** Value of the soft threshold */
	protected double m_softThreshold;
	protected double m_denominator_param;
	
	/*** The coefficient of the ridge term in the penalty */
	protected double m_ridgeCoeff;
	
	/*** The weighted mean of each predictor */
	protected double[] m_weighted_means;
	
	/*** The weighted sum of squares of each predictor */
	protected double[] m_weighted_sumSquares;
	
	/*** The 'residual covariances' of each predictor */
	protected double[] m_residual_covariances;
	
	/*** The 'part residuals' for each instance */
	protected double[] m_partResiduals;
	
	/*** The weighted sum of all the 'part residuals' */
	protected double m_partResidual_sum = 0.0;
	
	/*** The weighted sum of squares of all the 'part residuals' */
	protected double m_partResidual_sumSquared = 0.0;
	
	/*** The sum of weights of each instance */
	protected double m_sumOfWeights = 0.0;
	
	/*** The total squared error */
	protected double m_squaredError_term;
	
	/*** The penalty term divided by lambda */
	protected double m_unscaled_penalty = 0.0;
	
	/*** The sum of coefficient_i * mean_i for all predictors */
	protected double m_coeffsMeans_product = 0.0;
	
	/*** The term that measures the change in the objective */
	protected double m_significance_checkVal;
	
	/*** The maximum number of iterations */
	protected int m_maxIt;
	
	/**
	 * Constructor that performs all the required precomputation
	 * @param data
	 * @param alpha
	 * @param threshold
	 * @param maxIt
	 * @param covarianceMode
	 * @param sparse
	 */
	public CoordinateDescent(Instances data, double alpha, double threshold, int maxIt, boolean covarianceMode, boolean sparse) {
		m_dataset = data;
		m_numPredictors = data.numAttributes()-1;
		m_numInstances = data.numInstances();
		m_alpha = alpha;
		m_classIndex = data.classIndex();
		m_covariance_matrix = new double[m_numPredictors][m_numPredictors];
		m_covariances_filled = new boolean[m_numPredictors];
		
		m_coefficients = new double[m_numPredictors];
		m_covarianceMode = covarianceMode;
		m_threshold = threshold;
		m_maxIt = maxIt;
		m_sparse = sparse;
		m_ridgeCoeff = (1-m_alpha)/2;
				
		m_weighted_means = new double[m_numPredictors];
		m_weighted_sumSquares = new double[m_numPredictors];
		m_residual_covariances = new double[m_numPredictors];
		m_partResiduals = new double[m_numInstances];
		
		if(sparse) {
			m_sparse = true;
			m_sparseIndices = new ArrayList<ArrayList<Integer>>();
			for(int i=0; i<m_numPredictors; i++) {
				m_sparseIndices.add(new ArrayList<Integer>());
			}
		}
				
		// For each instance
		for(int i=0; i<m_numInstances; i++) {
			Instance currInstance = data.instance(i);
			m_sumOfWeights += currInstance.weight();
			m_classMean += currInstance.value(m_classIndex) * currInstance.weight();
			m_class_stdDev += currInstance.value(m_classIndex) * currInstance.value(m_classIndex) * currInstance.weight();
			
			// For each attribute
			for(int m=0; m<=m_numPredictors; m++) {
				if(m==m_classIndex) continue;
				int j = m>m_classIndex ? m-1 : m; // skips class attribute; j is index of predictor
				m_weighted_means[j] += currInstance.value(m) * currInstance.weight();
				m_weighted_sumSquares[j] += currInstance.value(m) * currInstance.value(m) * currInstance.weight();
				m_residual_covariances[j] += currInstance.value(m) * currInstance.value(m_classIndex) * currInstance.weight();
				
				if(m_sparse && currInstance.value(m) != 0) {
					m_sparseIndices.get(j).add(i);
				}
			}
		}
		m_classMean /= m_sumOfWeights;
		m_class_stdDev /= m_sumOfWeights;
		m_class_stdDev -= m_classMean * m_classMean;
		m_class_stdDev = Math.sqrt(m_class_stdDev);
		double maxGradient = 0.0;
		
		// For each predictor
		for(int j=0; j<m_numPredictors; j++) {
			m_weighted_means[j] /= m_sumOfWeights;
			m_weighted_sumSquares[j] -= m_weighted_means[j] * m_weighted_means[j] * m_sumOfWeights;
			m_residual_covariances[j] -= m_weighted_means[j] * m_classMean * m_sumOfWeights;
			m_residual_covariances[j] /= m_class_stdDev;
			
			if(Math.abs(m_residual_covariances[j]) > maxGradient) {
				maxGradient = Math.abs(m_residual_covariances[j]);
			}
		}
		
		m_lambdaZero = maxGradient/(m_alpha*m_sumOfWeights);
		// Setting partResiduals for naive mode
		for(int i=0; i<m_numInstances; i++) {
			Instance currInstance = data.instance(i);
			m_partResiduals[i] = (currInstance.value(m_classIndex) - m_classMean)/m_class_stdDev;
			m_partResidual_sum += currInstance.weight() * m_partResiduals[i];
			m_partResidual_sumSquared += currInstance.weight()*m_partResiduals[i]*m_partResiduals[i];
		}
		m_squaredError_term = m_partResidual_sumSquared/(2*m_sumOfWeights);
		m_significance_checkVal = m_threshold*m_squaredError_term;
	}
	
	/**
	 * Runs the CoordinateDescent algorithm
	 */
	public void run() {
		if(m_covarianceMode) {
			covarianceUpdateMethod();
		} else {
			naiveUpdateMethod();
		}
	}
	
	/**
	 * Performs coordinate descent with the covariance update method
	 */
	public void covarianceUpdateMethod() {
		boolean convergenceCheck = false; // Not converging
		int numIters = 0;
		
		// Repeat until convergence
		while(!convergenceCheck && numIters<m_maxIt) {
			convergenceCheck = true;
			for(int m=0; m<=m_numPredictors; m++) {
				if(m==m_classIndex) continue;
				int i = m>m_classIndex ? m-1 : m; // skips class attribute; i takes the index of each predictor
				
				// For each coefficient
				double preThresholdingVal = m_residual_covariances[i] + m_coefficients[i] * m_weighted_sumSquares[i];
				double newCoefficient = computeNewCoefficient(preThresholdingVal, i);
				if(newCoefficient != m_coefficients[i]) {
					if(!m_covariances_filled[i]) {
						// Need to fill row i in covariance matrix
						for(int n=0; n<=m_numPredictors; n++) {
							if(n==m_classIndex) continue;
							int j = n>m_classIndex ? n-1 : n; // skips class attribute
							
							if(m_covariances_filled[j]) {
								m_covariance_matrix[i][j] = m_covariance_matrix[j][i];
							} else {
								m_covariance_matrix[i][j] =	computeWeightedCovariance(m, n, i, j);
							}
						}
						m_covariances_filled[i] = true;
					}
					
					// Updates all residual covariances since a coefficient has changed
					double difference = newCoefficient-m_coefficients[i];
					for(int j=0; j<m_numPredictors; j++) {
						m_residual_covariances[j] -= difference * m_covariance_matrix[i][j];
					}
					
					double diffSquared = (m_coefficients[i]+newCoefficient)*difference;
					double squaredErrorDiff = diffSquared*m_weighted_sumSquares[i];
					squaredErrorDiff -= 2*difference*(m_residual_covariances[i] + newCoefficient*m_weighted_sumSquares[i]);
					squaredErrorDiff /= 2*m_sumOfWeights;
					
					double penaltyDiff = m_ridgeCoeff*(m_coefficients[i]+newCoefficient)*difference
							+ m_alpha*(Math.abs(newCoefficient)-Math.abs(m_coefficients[i]));
					m_squaredError_term += squaredErrorDiff;
					m_unscaled_penalty += penaltyDiff;
					 
					double objectiveChange = squaredErrorDiff + m_lambda*penaltyDiff;
					if(convergenceCheck && checkSignificance(objectiveChange)) {
						convergenceCheck = false; // Not converging yet
					}
				}
				m_coefficients[i] = newCoefficient;
			}
			numIters++;
		}
		m_coeffsMeans_product = 0;
		for(int i=0; i<m_numPredictors; i++) {
			m_coeffsMeans_product += m_coefficients[i]*m_weighted_means[i];
		}
	}
	
	/**
	 * Performs coordinate descent with the naive update method
	 */
	public void naiveUpdateMethod() {
		boolean convergenceCheck = false; // Not converging
		int numIters = 0;
		// Repeat until convergence
		while(!convergenceCheck && numIters<m_maxIt) {
			convergenceCheck = true;
			for(int m=0; m<=m_numPredictors; m++) {
				if(m==m_classIndex) continue;
				int i = m>m_classIndex ? m-1 : m; // skips class attribute
				// For each coefficient
				double preThresholdingVal = m_coefficients[i]*m_weighted_sumSquares[i] - m_weighted_means[i]*m_partResidual_sum;
				if(m_sparse) {
					for(int j : m_sparseIndices.get(i)) {
						Instance currInstance = m_dataset.instance(j);
						preThresholdingVal += m_partResiduals[j] * currInstance.value(i) * currInstance.weight();
					}
				} else {
					for(int j=0; j<m_numInstances; j++) {
						Instance currInstance = m_dataset.instance(j);
						preThresholdingVal += m_partResiduals[j] * currInstance.value(i) * currInstance.weight();
					}
				}
				
				// Updates residuals if a coefficient has changed
				double newCoefficient = computeNewCoefficient(preThresholdingVal, i);					
				if(newCoefficient != m_coefficients[i]) {
					double difference = newCoefficient-m_coefficients[i];
					if(m_sparse) {
						for(int j : m_sparseIndices.get(i)) {
							Instance currInstance = m_dataset.instance(j);
							double partResidualChange = difference * currInstance.value(i);
							m_partResiduals[j] -= partResidualChange;
							m_partResidual_sum -= currInstance.weight() * partResidualChange;
							m_partResidual_sumSquared -= currInstance.weight()*partResidualChange*(2*m_partResiduals[j]+partResidualChange);
						}	
					} else {
						for(int j=0; j<m_numInstances; j++) {
							Instance currInstance = m_dataset.instance(j);
							double partResidualChange = difference * currInstance.value(i);
							m_partResiduals[j] -= partResidualChange;
							m_partResidual_sum -= currInstance.weight() * partResidualChange;
							m_partResidual_sumSquared -= currInstance.weight()*partResidualChange*(2*m_partResiduals[j]+partResidualChange);
						}
					}
					
					m_coeffsMeans_product += difference * m_weighted_means[i];
					double oldObjective = m_squaredError_term + m_lambda*m_unscaled_penalty;
					
					m_squaredError_term = (m_partResidual_sumSquared + 2*m_coeffsMeans_product*m_partResidual_sum)/(2*m_sumOfWeights) 
							+ Math.pow(m_coeffsMeans_product, 2)/2;
					m_unscaled_penalty += m_ridgeCoeff*(m_coefficients[i]+newCoefficient)*difference
							+ m_alpha*(Math.abs(newCoefficient)-Math.abs(m_coefficients[i]));
					
					double objectiveChange = m_squaredError_term + m_lambda*m_unscaled_penalty - oldObjective;
					if(convergenceCheck && checkSignificance(objectiveChange)) {
						convergenceCheck = false; // Not converging yet
					}
				}
				m_coefficients[i] = newCoefficient;
			}
			numIters++;
		}
	}
	
	/**
	 * Computes weighted covariance between column index1 and column index2
	 * @param m
	 * @param n
	 * @param i
	 * @param j
	 */
	public double computeWeightedCovariance(int m, int n, int i, int j) {
		double result = 0.0;
		// For each instance
		if(m_sparse) {
			for(int k : m_sparseIndices.get(i)) {
				Instance currInstance = m_dataset.instance(k);
				result +=  currInstance.value(m) * currInstance.value(n) * currInstance.weight();
			}
		} else {
			for(int k=0; k<m_numInstances; k++) {
				Instance currInstance = m_dataset.instance(k);
				result +=  currInstance.value(m) * currInstance.value(n) * currInstance.weight();
			}
		}
		result -= m_weighted_means[i] * m_weighted_means[j] * m_sumOfWeights;
		return result;
	}
	
	public boolean checkSignificance(double change) {
		if(Math.abs(change) > m_significance_checkVal) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Applies soft thresholding and uses a formula to find new coefficient
	 * @param value
	 * @param index
	 */
	public double computeNewCoefficient(double value, int index) {
		if(m_softThreshold < Math.abs(value)) {
			double denominator = m_weighted_sumSquares[index] + m_denominator_param;
			if(value > 0) {
				return (value - m_softThreshold)/denominator;
			} else if(value < 0) {
				return (value + m_softThreshold)/denominator;
			}
		}
		return 0.0;
	}
	
	/**
	 * Classifies an instance using the stored model. 
	 * The model essentially fits a hyperplane through the data (similar to linear regression)
	 */
	@Override
	public double classifyInstance(Instance instance) throws Exception {
		double prediction = -m_coeffsMeans_product;
		for(int m=0; m<=m_numPredictors; m++) {
			if(m==m_classIndex) continue;
			int i = m>m_classIndex ? m-1 : m; // skips class attribute
			prediction += m_coefficients[i] * instance.value(m);
		}
		prediction *= m_class_stdDev;
		prediction += m_classMean;
		return prediction;
	}
		
	/**
	 * Sets a lambda value
	 * @param lambda
	 */
	public void setLambda(double lambda) {
		m_lambda = lambda;
		m_softThreshold = m_lambda*m_alpha*m_sumOfWeights;
		m_denominator_param = m_lambda*(1-m_alpha)*m_sumOfWeights;
	}
	
	/**
	 * Sets a convergence threshold
	 * @param threshold
	 */
	public void setThreshold(double threshold) {
		m_significance_checkVal *= threshold/m_threshold;
		m_threshold = threshold;
	}
	
	/**
	 * Returns the class mean
	 */
	public double get_classMean() {
		return m_classMean;
	}
	
	/**
	 * Returns the class standard deviation
	 */
	public double get_class_stdDev() {
		return m_class_stdDev;
	}
	
	/**
	 * Returns the final coefficients
	 */
	public double[] getCoefficients() {
		return m_coefficients;
	}
	
	/**
	 * Returns lambda zero
	 */
	public double getLambdaZero() {
		return m_lambdaZero;
	}
	
	/**
	 * Returns the sum of coefficient_i * mean_i for all predictors
	 */
	public double get_coeffsMeans_product() {
		return m_coeffsMeans_product;
	}
	
	/**
	 * Empty buildClassifier method to satisfy the AbstractClassifier interface
	 */
	@Override
	public void buildClassifier(Instances data) throws Exception {}
}
