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
 *    SVMAttributeEval.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.functions.SMO;
import weka.filters.Filter;
import weka.filters.AttributeFilter;

/** 
 * Class for Evaluating attributes individually by using the SVM
 * classifier. <p>
 *
 * No options. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class SVMAttributeEval extends AttributeEvaluator {

  /** The attribute scores */
  private double[] m_attScores;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "SVMAttributeEval :\n\nEvaluates the worth of an attribute by "
      +"using an SVM classifier.\n";
  }

  /**
   * Constructor
   */
  public SVMAttributeEval () {
    resetOptions();
  }

  /**
   * Initializes the evaluator.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception {
    
    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    
    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class must be nominal!");
    }
    
    if (data.classAttribute().numValues() != 2) {
      throw new Exception("Can only deal with binary class problems!");
    }
    
    // Holds a mapping into the original array of attribute indices
    int[] origIndices = new int[data.numAttributes()];
    for (int i = 0; i < origIndices.length; i++) {
      if (data.attribute(i).isNominal() && 
	  (data.attribute(i).numValues() != 2)) {
	throw new Exception("All nominal attributes must be binary!");
      }
      origIndices[i] = i;
    }
    
    // We need to repeat the following loop until we've computed
    // a weight for every attribute (excluding the class).
    m_attScores = new double[data.numAttributes() - 1];
    Instances trainCopy = new Instances(data);
    for (int i = 0; i < m_attScores.length; i++) {
      
      // Build the linear SVM with default parameters
      SMO smo = new SMO();
      smo.buildClassifier(trainCopy);

      // Find the attribute with maximum weight^2
      FastVector weightsAndIndices = smo.weights();
      double[] weightsSparse = (double[])weightsAndIndices.elementAt(0);
      int[] indicesSparse = (int[])weightsAndIndices.elementAt(1);
      double[] weights = new double[trainCopy.numAttributes()];
      for (int j = 0; j < weightsSparse.length; j++) {
	weights[indicesSparse[j]] = weightsSparse[j] * weightsSparse[j];
      }
      weights[trainCopy.classIndex()] = Double.MAX_VALUE;
      int minWeightIndex = Utils.minIndex(weights);
      m_attScores[origIndices[minWeightIndex]] = weights[minWeightIndex];
      
      // Delete the best attribute. 
      AttributeFilter delTransform = new AttributeFilter();
      delTransform.setInvertSelection(false);
      int[] featArray = new int[1];
      featArray[0] = minWeightIndex;
      delTransform.setAttributeIndicesArray(featArray);
      delTransform.setInputFormat(trainCopy);
      trainCopy = Filter.useFilter(trainCopy, delTransform);
      
      // Update the array of indices
      int[] temp = new int[origIndices.length - 1];
      System.arraycopy(origIndices, 0, temp, 0, minWeightIndex);
      for (int j = minWeightIndex + 1; j < origIndices.length; j++) {
	temp[j - 1] = origIndices[j];
      }
      origIndices = temp;
    }
  }

  /**
   * Resets options to defaults.
   */
  protected void resetOptions () {

    m_attScores = null;
  }

  /**
   * Evaluates an attribute by returning the square of its coefficient in a
   * linear support vector machine.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute (int attribute) throws Exception {

    return  m_attScores[attribute];
  }

  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {

    StringBuffer text = new StringBuffer();

    if (m_attScores == null) {
      text.append("\tSVM feature evaluator has not been built yet");
    } else {
      text.append("\tSVM feature evaluator");
    }

    text.append("\n");
    return  text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {

    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new SVMAttributeEval(), args));
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}

