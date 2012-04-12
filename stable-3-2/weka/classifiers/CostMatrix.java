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
 *    CostMatrix.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
 */

package weka.classifiers;

import weka.core.Utils;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Matrix;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Random;

/**
 * Class for a misclassification cost matrix. The element in the i'th column
 * of the j'th row is the cost for (mis)classifying an instance of class j as 
 * having class i. It is valid to have non-zero values down the diagonal 
 * (these are typically negative to indicate some varying degree of "gain" 
 * from making a correct prediction).
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.8 $
 */
public class CostMatrix extends Matrix {

  /** The filename extension that should be used for cost files */
  public static String FILE_EXTENSION = ".cost";

  /**
   * Creates a cost matrix identical to an existing matrix.
   *
   * @param toCopy the matrix to copy.
   */
  public CostMatrix(CostMatrix toCopy) {

    this(toCopy.size());
    for (int i = 0; i < size(); i++) {
      for (int j = 0; j < size(); j++) {
	setElement(i, j, toCopy.getElement(i, j));
      }
    }
  }

  /**
   * Creates a default cost matrix for the given number of classes. The 
   * default misclassification cost is 1.
   *
   * @param numClasses the number of classes
   */
  public CostMatrix(int numClasses) {
    
    super(numClasses, numClasses);
  }  

  /**
   * Creates a cost matrix from a cost file.
   *
   * @param r a reader from which the cost matrix will be read
   * @exception Exception if an error occurs
   */
  public CostMatrix(Reader r) throws Exception {

    super(r);
    if (numColumns() != numRows()) {
      throw new Exception("Cost matrix is not square");
    }
  }


  /**
   * Creates a cost matrix for the class attribute of the supplied instances, 
   * where the misclassification costs are higher for misclassifying a rare
   * class as a frequent one. The cost of classifying an instance of class i 
   * as class j is weight * Pj / Pi. (Pi and Pj are laplace estimates)
   *
   * @param instances a value of type 'Instances'
   * @param weight a value of type 'double'
   * @return a value of type CostMatrix
   * @exception Exception if no class attribute is assigned, or the class
   * attribute is not nominal
   */
  public static CostMatrix makeFrequencyDependentMatrix(Instances instances,
                                                        double weight) 
    throws Exception {

    if (!instances.classAttribute().isNominal()) {
      throw new Exception("Class attribute is not nominal!");
    }
    int numClasses = instances.numClasses();

    // Collect class probabilities
    double probs [] = new double [numClasses];
    for (int i = 0; i < probs.length; i++) {
      probs[i]++;
    }
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance current = instances.instance(i);
      if (!current.classIsMissing()) {
        probs[(int)current.classValue()]++;
      }
    }
    Utils.normalize(probs);

    // Create and populate the cost matrix
    CostMatrix newMatrix = new CostMatrix(numClasses);
    for (int i = 0; i < numClasses; i++) {
      for (int j = 0; j < numClasses; j++) {
        if (i != j) {
          newMatrix.setElement(i, j, weight * probs[j] / probs[i]);
        }
      }
    }
    
    return newMatrix;
  }

  /**
   * Reads misclassification cost matrix from given reader. 
   * Each line has to contain three numbers: the index of the true 
   * class, the index of the incorrectly assigned class, and the 
   * weight, separated by white space characters. Comments can be 
   * appended to the end of a line by using the '%' character.
   *
   * @param reader the reader from which the cost matrix is to be read
   * @exception Exception if the cost matrix does not have the 
   * right format
   */
  public void readOldFormat(Reader reader)throws Exception {

    initialize();

    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    tokenizer.commentChar('%');
    tokenizer.eolIsSignificant(true);

    int currentToken;
    while (StreamTokenizer.TT_EOF != 
	   (currentToken = tokenizer.nextToken())) {
      
      // Skip empty lines 
      if (currentToken == StreamTokenizer.TT_EOL) {
	continue;
      }
      
      // Get index of first class.
      if (currentToken != StreamTokenizer.TT_NUMBER) {
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      }
      double firstIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)firstIndex, firstIndex)) {
	throw new Exception("First number in line has to be "+
			    "index of a class!");
      }
      if ((int)firstIndex >= size()) {
	throw new Exception("Class index out of range!");
      }

      // Get index of second class.
      if (StreamTokenizer.TT_EOF == 
	  (currentToken = tokenizer.nextToken())) {
	throw new Exception("Premature end of file!");
      }
      if (currentToken == StreamTokenizer.TT_EOL) {
	throw new Exception("Premature end of line!");
      }
      if (currentToken != StreamTokenizer.TT_NUMBER) {
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      }
      double secondIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)secondIndex,secondIndex)) {
	throw new Exception("Second number in line has to be "+
			    "index of a class!");
      }
      if ((int)secondIndex >= size()) {
	throw new Exception("Class index out of range!");
      }

      // Get cost factor.
      if (StreamTokenizer.TT_EOF == 
	  (currentToken = tokenizer.nextToken())) {
	throw new Exception("Premature end of file!");
      }
      if (currentToken == StreamTokenizer.TT_EOL) {
	throw new Exception("Premature end of line!");
      }
      if (currentToken != StreamTokenizer.TT_NUMBER) {
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      }
      double weight = tokenizer.nval;
      setElement((int)firstIndex, (int)secondIndex, weight);
    }
  }

  /**
   * Sets the costs to default values (i.e. 0 down the diagonal, and 1 for
   * any misclassification).
   */
  public void initialize() {

    for (int i = 0; i < numRows(); i++) {
      for (int j = 0; j < numColumns(); j++) {
	if (i != j) {
	  setElement(i, j, 1);
	} else {
	  setElement(i, j, 0);
	}
      }
    }
  }

  /**
   * Gets the number of classes.
   *
   * @return the number of classes
   */
  public int size() {

    return numColumns();
  }

  
  /**
   * Normalizes the cost matrix so that diagonal elements are zero. The value
   * of non-zero diagonal elements is subtracted from the row containing the
   * value. For example: <p>
   *
   * <pre><code>
   * 2  5
   * 3 -1
   * </code></pre>
   * 
   * <p> becomes <p>
   *
   * <pre><code>
   * 0  3
   * 4  0
   * </code></pre><p>
   *
   * This normalization will affect total classification cost during 
   * evaluation, but will not affect the decision made by applying minimum
   * expected cost criteria during prediction.
   */
  public void normalize() {

    for (int i = 0; i < size(); i++) {
      double diag = getElement(i, i);
      for (int j = 0; j < size(); j++) {
        addElement(i, j, -diag);
      }
    }
  }

  /** 
   * Changes the dataset to reflect a given set of costs.
   * Sets the weights of instances according to the misclassification
   * cost matrix, or does resampling according to the cost matrix (if
   * a random number generator is provided). Returns a new dataset.
   *
   * @param instances the instances to apply cost weights to.
   * @param random a random number generator 
   * @return the new dataset
   * @exception Exception if the cost matrix does not have the right
   * format 
   */
  public Instances applyCostMatrix(Instances instances, Random random) 
       throws Exception {

    if (instances.classIndex() < 0) {
      throw new Exception("Class index is not set!");
    }
    if (size() != instances.numClasses()) {
      throw new Exception("Cost matrix and instances have different class"
			  + " size!");
    }

    // If this cost matrix hasn't been normalized, apply a normalized
    // version instead.
    for (int i = 0; i < size(); i++) {
      if (!Utils.eq(m_Elements[i][i], 0)) {
        CostMatrix cm = new CostMatrix(this);
        cm.normalize();
        return cm.applyCostMatrix(instances, random);
      }
    }
      
    // Determine the prior weights of all instances in each class
    double [] weightOfInstancesInClass = new double [size()];
    for (int j = 0; j < instances.numInstances(); j++) {
      Instance current = instances.instance(j);
      weightOfInstancesInClass[(int)current.classValue()] += 
	current.weight();
    }
    double sumOfWeights = Utils.sum(weightOfInstancesInClass);

    double [] weightFactor = new double [size()];
    double sumOfWeightFactors = 0;
    for (int i = 0; i < size(); i++) {

      // Using Kai Ming Ting's formula for deriving weights for 
      // the classes and Breiman's heuristic for multiclass 
      // problems.
      double sumOfMissClassWeights = 0;
      for (int j = 0; j < size(); j++) {
	if (Utils.sm(m_Elements[i][j], 0)) {
	  throw new Exception("Neg. weights in misclassification "+
			      "cost matrix!"); 
	}
	sumOfMissClassWeights += m_Elements[i][j];
      }
      weightFactor[i] = sumOfMissClassWeights * sumOfWeights;
      sumOfWeightFactors += sumOfMissClassWeights 
	* weightOfInstancesInClass[i];
    }
    for (int i = 0; i < size(); i++) {
      weightFactor[i] /= sumOfWeightFactors;
    }
    
    // Store new weights
    double [] weightOfInstances = new double[instances.numInstances()];
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance current = instances.instance(i);
      weightOfInstances[i] = current.weight() 
	* weightFactor[(int)current.classValue()];
    } 

    // Change instances weight or do resampling
    if (random != null) {
      return instances.resampleWithWeights(random, weightOfInstances);
    } else { 
      instances = new Instances(instances);
      for (int i = 0; i < instances.numInstances(); i++) {
	instances.instance(i).setWeight(weightOfInstances[i]);
      }
      return instances;
    }
  }


  /**
   * Calculates the expected misclassification cost for each possible
   * class value, given class probability estimates.
   *
   * @param probabilities an array containing probability estimates for each 
   * class value.
   * @return an array containing the expected misclassification cost for each
   * class.
   * @exception Exception if the number of probabilities does not match the 
   * number of classes.
   */
  public double [] expectedCosts(double [] probabilities) throws Exception {

    if (probabilities.length != size()) {
      throw new Exception("Number of classes in probability estimates does not"
			  + " match size of cost matrix!");
    }

    double [] costs = new double[size()];
    for (int i = 0; i < size(); i++) {
      double expectedCost = 0;
      for (int j = 0; j < size(); j++) {
	expectedCost += m_Elements[j][i] * probabilities[j];
      }
      costs[i] = expectedCost;
    }
    return costs;
  }

  /**
   * Gets the maximum misclassification cost possible for a given actual
   * class value
   *
   * @param actualClass the index of the actual class value
   * @return the highest cost possible for misclassifying this class
   */
  public double getMaxCost(int actualClass) {

    return m_Elements[actualClass][Utils.maxIndex(m_Elements[actualClass])];
  }

  /**
   * Tests out creation of a frequency dependent cost matrix from the command
   * line. Either pipe a set of instances into system.in or give the name of
   * a dataset as an argument. The last column will be treated as the class
   * attribute and a cost matrix with weight 1000 output.
   *
   * @param []args a value of type 'String'
   */
  public static void main(String []args) {

    try {
      Reader r = null;
      if (args.length > 1) {
	throw (new Exception("Usage: Instances <filename>"));
      } else if (args.length == 0) {
        r = new BufferedReader(new InputStreamReader(System.in));
      } else {
        r = new BufferedReader(new FileReader(args[0]));
      }
      Instances i = new Instances(r);
      i.setClassIndex(i.numAttributes() - 1);
      CostMatrix.makeFrequencyDependentMatrix(i, 1000)
        .write(new java.io.PrintWriter(System.out));

    } catch (Exception ex) {
      System.err.println(ex);
    }
  }
} // CostMatrix
