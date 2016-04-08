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
 *    FLDA.java
 *    Copyright (C) 2004, 2014 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.functions;

import weka.classifiers.AbstractClassifier;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.RevisionUtils;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Option;
import weka.core.Utils;

import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveUseless;

import no.uib.cipr.matrix.*;
import no.uib.cipr.matrix.Matrix;

import java.util.Enumeration;
import java.util.Collections;

/**
 * <!-- globalinfo-start -->
 * Builds Fisher's Linear Discriminant function. The threshold is selected so that the separator is half-way between centroids. The class must be binary and all other attributes must be numeric. Missing values are not permitted. Constant attributes are removed using RemoveUseless. No standardization or normalization of attributes is performed.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R
 *  The ridge parameter.
 *  (default is 1e-6)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank, University of Waikato
 * @version $Revision: 10382 $
 */
public class FLDA extends AbstractClassifier {
  
  /** for serialization */
  static final long serialVersionUID = -9212385698193681291L;

  /** Holds header of training date */
  protected Instances m_Data;
  
  /** The weight vector */
  protected Vector m_Weights;

  /** The threshold */
  protected double m_Threshold;

  /** Ridge parameter */
  protected double m_Ridge = 1e-6;

  /** Rmeove useless filter */
  protected RemoveUseless m_RemoveUseless;

  /**
   * Global info for this classifier.
   */
  public String globalInfo() {
    return "Builds Fisher\'s Linear Discriminant function. The threshold is selected "
      + "so that the separator is half-way between centroids. The class must be "
      + "binary and all other attributes must be numeric. Missing values are not "
      + "permitted. Constant attributes are removed using RemoveUseless. No "
      + "standardization or normalization of attributes is performed.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Computes the mean vector for each class.
   */
  protected Vector[] getClassMeans(Instances data, int[] counts) {
    
    double[][] centroids = new double[2][data.numAttributes() - 1];
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      int index = 0;
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          centroids[(int)inst.classValue()][index++] += inst.value(j);
        } 
      }
      counts[(int)inst.classValue()] ++;
    }
    Vector[] centroidVectors = new DenseVector[2];
    for (int i = 0; i < 2; i++) {
      centroidVectors[i] = new DenseVector(centroids[i]);
      centroidVectors[i].scale(1.0 / (double)counts[i]);
    }
    if (m_Debug) {
      System.out.println("Count for class 0: " + counts[0]);
      System.out.println("Centroid 0:" + centroidVectors[0]);
      System.out.println("Count for class 11: " + counts[1]);
      System.out.println("Centroid 1:" + centroidVectors[1]);
    }

    return centroidVectors;
  } 
  
  /**
   * Computes centered subsets as matrix with instances as columns.
   */
  protected Matrix[] getCenteredData(Instances data, int[] counts, Vector[] centroids) {

    Matrix[] centeredData = new Matrix[2];
    for (int i = 0; i < 2; i++) {
      centeredData[i] = new DenseMatrix(data.numAttributes() - 1, counts[i]);
    }
    int[] indexC = new int[2];
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      int classIndex = (int)inst.classValue();
      int index = 0;
      for (int j = 0; j < data.numAttributes(); j++) {
        if (j != data.classIndex()) {
          centeredData[classIndex].set(index, indexC[classIndex],
                                       inst.value(j) - centroids[classIndex].get(index));
          index++;
        }
      }
      indexC[classIndex]++;
    }
    if (m_Debug) {
      System.out.println("Centered data for class 0:\n" + centeredData[0]); 
      System.out.println("Centered data for class 1:\n" + centeredData[1]); 
    }
    return centeredData;
  }

  /**
   * Builds the classifier.
   */
  public void buildClassifier(Instances insts) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // Remove constant attributes
    m_RemoveUseless = new RemoveUseless();
    m_RemoveUseless.setInputFormat(insts);
    insts = Filter.useFilter(insts, m_RemoveUseless);
    insts.deleteWithMissingClass();

    // Establish class frequencies and centroids
    int[] classCounts = new int[2];
    Vector[] centroids = getClassMeans(insts, classCounts);

    // Compute difference of centroids
    Vector diff = centroids[0].copy().add(-1.0, centroids[1]);

    // Center data for each class
    Matrix[] data = getCenteredData(insts, classCounts, centroids);

    // Compute scatter matrix and add ridge
    Matrix scatter = new UpperSymmDenseMatrix(data[0].numRows()).rank1(data[0]).
            add(new UpperSymmDenseMatrix(data[1].numRows()).rank1(data[1]));
    for (int i = 0; i < scatter.numColumns(); i++) {
      scatter.add(i, i, m_Ridge);
    }
    if (m_Debug) {
      System.out.println("Scatter:\n" + scatter);
    }

    // Establish and normalize weight vector
    m_Weights = scatter.solve(diff, new DenseVector(scatter.numColumns()));
    m_Weights.scale(1.0 / m_Weights.norm(Vector.Norm.Two));

    // Compute threshold
    m_Threshold = 0.5 * m_Weights.dot(centroids[0].copy().add(centroids[1]));

    // Store header only
    m_Data = new Instances(insts, 0);
  }   
    
  /**
   * Output class "probabilities". These need to be calibrated.
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    // Filter instance
    m_RemoveUseless.input(inst);
    inst = m_RemoveUseless.output();
    
    // Convert instance to matrix
    Vector instM = new DenseVector(inst.numAttributes() - 1);
    int index = 0;
    for (int i = 0; i < inst.numAttributes(); i++) {
      if (i != m_Data.classIndex()) {
        instM.set(index++, inst.value(i));
      }
    }

    // Pipe output through sigmoid
    double[] dist = new double[2];
    dist[1] = 1/(1 + Math.exp(instM.dot(m_Weights) - m_Threshold));
    dist[0] = 1 - dist[1];
    return dist;
  }

  /**
   * Outputs description of classifier as a string.
   * @return the description
   */
  public String toString() {

    if (m_Weights == null) {
      return "No model has been built yet.";
    }
    StringBuffer result = new StringBuffer();
    result.append("Fisher's Linear Discriminant Analysis\n\n");
    result.append("Threshold: " + m_Threshold + "\n\n");
    result.append("Weights:\n\n");
    int index = 0;
    for (int i = 0; i < m_Data.numAttributes(); i++) {
      if (i != m_Data.classIndex()) {
        result.append(m_Data.attribute(i).name() + ": \t");
        double weight = m_Weights.get(index++);
        if (weight >= 0) {
          result.append(" ");
        }
        result.append(weight + "\n");
      }
    }
    return result.toString();
  }  

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "The value of the ridge parameter.";
  }

  /**
   * Get the value of Ridge.
   * 
   * @return Value of Ridge.
   */
  public double getRidge() {

    return m_Ridge;
  }

  /**
   * Set the value of Ridge.
   * 
   * @param newRidge Value to assign to Ridge.
   */
  public void setRidge(double newRidge) {

    m_Ridge = newRidge;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    java.util.Vector<Option> newVector = new java.util.Vector<Option>(7);

    newVector.addElement(new Option(
	      "\tThe ridge parameter.\n"+
	      "\t(default is 1e-6)",
	      "R", 0, "-R"));

    newVector.addAll(Collections.list(super.listOptions()));
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   * <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R
   *  The ridge parameter.
   *  (default is 1e-6)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   * 
   * <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String ridgeString = Utils.getOption('R', options);
    if (ridgeString.length() != 0) {
      setRidge(Double.parseDouble(ridgeString));
    } else {
      setRidge(1e-6);
    }
    
    super.setOptions(options);
    
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    java.util.Vector<String> options = new java.util.Vector<String>();
    options.add("-R"); options.add("" + getRidge());
    
    Collections.addAll(options, super.getOptions());
    
    return options.toArray(new String[0]);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
    public String getRevision() {
    return RevisionUtils.extract("$Revision: 10382 $");
  }
  
  /**
   * Generates an FLDA classifier.
   * 
   * @param argv the options
   */
  public static void main(String [] argv){  
    runClassifier(new FLDA(), argv);
  }
}

