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
 *    Copyright (C) 2001 Richard Kirkby
 *
 */

package weka.classifiers;
import weka.core.Matrix;
import weka.core.Instances;
import weka.core.Utils;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Random;

/**
 * Class for storing and manipulating a misclassification cost matrix.
 * The element at position i,j in the matrix is the penalty for classifying
 * an instance of class j as class i.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.9.2.1 $
 */
public class CostMatrix extends Matrix {

  /** The deafult file extension for cost matrix files */
  public static String FILE_EXTENSION = ".cost";

  /**
   * Creates a cost matrix that is a copy of another.
   *
   * @param toCopy the matrix to copy.
   */
  public CostMatrix(CostMatrix toCopy) {
    
    super(toCopy.size(), toCopy.size());

    for (int x=0; x<toCopy.size(); x++) 
      for (int y=0; y<toCopy.size(); y++) 
	setElement(x, y, toCopy.getElement(x, y)); 
  }

  /**
   * Creates a default cost matrix of a particular size. All values will be 0.
   *
   * @param numOfClasses the number of classes that the cost matrix holds.
   */  
  public CostMatrix(int numOfClasses) {
    
    super(numOfClasses, numOfClasses);
  }

  /**
   * Creates a cost matrix from a reader.
   *
   * @param reader the reader to get the values from.
   * @exception Exception if the matrix is invalid.
   */  
  public CostMatrix(Reader reader) throws Exception {

    super(reader);

    // make sure that the matrix is square
    if (numRows() != numColumns())
      throw new Exception("Trying to create a non-square cost matrix");
  }

  /**
   * Sets the cost of all correct classifications to 0, and all
   * misclassifications to 1.
   *
   */ 
  public void initialize() {

    for (int i = 0; i < size(); i++) {
      for (int j = 0; j < size(); j++) {
	setElement(i, j, i == j ? 0.0 : 1.0);
      }
    }
  }

  /**
   * Gets the size of the matrix.
   *
   * @return the size.
   */
  public int size() {

    return numColumns();
  }

  /**
   * Applies the cost matrix to a set of instances. If a random number generator is 
   * supplied the instances will be resampled, otherwise they will be rewighted. 
   * Adapted from code once sitting in Instances.java
   *
   * @param data the instances to reweight.
   * @param random a random number generator for resampling, if null then instances are
   * rewighted.
   * @return a new dataset reflecting the cost of misclassification.
   * @exception Exception if the data has no class or the matrix in inappropriate.
   */
  public Instances applyCostMatrix(Instances data, Random random) throws Exception {

    double sumOfWeightFactors = 0, sumOfMissClassWeights,
      sumOfWeights;
    double [] weightOfInstancesInClass, weightFactor, weightOfInstances;
    Instances newData;

    if (data.classIndex() < 0) {
      throw new Exception("Class index is not set!");
    }
 
    if (size() != data.numClasses()) { 
      throw new Exception("Misclassification cost matrix has "+
			  "wrong format!");
    }

    weightFactor = new double[data.numClasses()];
    weightOfInstancesInClass = new double[data.numClasses()];
    for (int j = 0; j < data.numInstances(); j++) {
      weightOfInstancesInClass[(int)data.instance(j).classValue()] += 
	data.instance(j).weight();
    }
    sumOfWeights = Utils.sum(weightOfInstancesInClass);

    // normalize the matrix if not already
    for (int i=0; i<size(); i++)
      if (!Utils.eq(getElement(i, i),0)) {
	CostMatrix normMatrix = new CostMatrix(this);
	normMatrix.normalize();
	return normMatrix.applyCostMatrix(data, random);
      }
    
    for (int i = 0; i < data.numClasses(); i++) {

      // Using Kai Ming Ting's formula for deriving weights for 
      // the classes and Breiman's heuristic for multiclass 
      // problems.
      sumOfMissClassWeights = 0;
      for (int j = 0; j < data.numClasses(); j++) {
	if (Utils.sm(getElement(i,j),0)) {
	  throw new Exception("Neg. weights in misclassification "+
			      "cost matrix!"); 
	}
	sumOfMissClassWeights += getElement(i,j);
      }
      weightFactor[i] = sumOfMissClassWeights * sumOfWeights;
      sumOfWeightFactors += sumOfMissClassWeights * 
	weightOfInstancesInClass[i];
    }
    for (int i = 0; i < data.numClasses(); i++) {
      weightFactor[i] /= sumOfWeightFactors;
    }
    
    // Store new weights
    weightOfInstances = new double[data.numInstances()];
    for (int i = 0; i < data.numInstances(); i++) {
      weightOfInstances[i] = data.instance(i).weight()*
	weightFactor[(int)data.instance(i).classValue()];
    } 

    // Change instances weight or do resampling
    if (random != null) {
      return data.resampleWithWeights(random, weightOfInstances);
    } else { 
      Instances instances = new Instances(data);
      for (int i = 0; i < data.numInstances(); i++) {
	instances.instance(i).setWeight(weightOfInstances[i]);
      }
      return instances;
    }
  }

  /**
   * Calculates the expected misclassification cost for each possible class value,
   * given class probability estimates. 
   *
   * @param classProbs the class probability estimates.
   * @return the expected costs.
   * @exception Exception if the wrong number of class probabilities is supplied.
   */
  public double[] expectedCosts(double[] classProbs) throws Exception {

    if (classProbs.length != size())
      throw new Exception("Length of probability estimates don't match cost matrix");

    double[] costs = new double[size()];

    for (int x=0; x<size(); x++)
      for (int y=0; y<size(); y++) 
	costs[x] += classProbs[y] * getElement(y, x);

    return costs;
  }

  /**
   * Gets the maximum cost for a particular class value.
   *
   * @param classVal the class value.
   * @return the maximum cost.
   */
  public double getMaxCost(int classVal) {

    double maxCost = Double.NEGATIVE_INFINITY;

    for (int i=0; i<size(); i++) {
      double cost = getElement(classVal, i);
      if (cost > maxCost) maxCost = cost;
    }

    return maxCost;
  }

  /**
   * Normalizes the matrix so that the diagonal contains zeros.
   *
   */
  public void normalize() {

    for (int y=0; y<size(); y++) {
      double diag = getElement(y, y);
      for (int x=0; x<size(); x++)
	setElement(x, y, getElement(x, y) - diag);
    }
  }

  /**
   * Loads a cost matrix in the old format from a reader. Adapted from code once sitting 
   * in Instances.java
   *
   * @param reader the reader to get the values from.
   * @exception Exception if the matrix cannot be read correctly.
   */  
  public void readOldFormat(Reader reader) throws Exception {

    StreamTokenizer tokenizer;
    int currentToken;
    double firstIndex, secondIndex, weight;

    tokenizer = new StreamTokenizer(reader);

    initialize();

    tokenizer.commentChar('%');
    tokenizer.eolIsSignificant(true);
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
      firstIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)firstIndex,firstIndex)) {
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
      secondIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)secondIndex,secondIndex)) {
	throw new Exception("Second number in line has to be "+
			    "index of a class!");
      }
      if ((int)secondIndex >= size()) {
	throw new Exception("Class index out of range!");
      }
      if ((int)secondIndex == (int)firstIndex) {
	throw new Exception("Diagonal of cost matrix non-zero!");
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
      weight = tokenizer.nval;
      if (!Utils.gr(weight,0)) {
	throw new Exception("Only positive weights allowed!");
      }
      setElement((int)firstIndex, (int)secondIndex, weight);
    }
  }
}

