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
 *    LWL.java
 *    Copyright (C) 1999, 2002, 2003 Len Trigg, Eibe Frank, Ashraf M. Kibriya
 *
 */

package weka.classifiers.lazy;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.SingleClassifierEnhancer;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Locally-weighted learning. Uses an instance-based algorithm to
 * assign instance weights which are then used by a specified
 * WeightedInstancesHandler.  A good choice for classification is
 * NaiveBayes. LinearRegression is suitable for regression problems.
 * For more information, see<p>
 *
 * Eibe Frank, Mark Hall, and Bernhard Pfahringer (2003). Locally
 * Weighted Naive Bayes. Working Paper 04/03, Department of Computer
 * Science, University of Waikato.
 *
 * Atkeson, C., A. Moore, and S. Schaal (1996) <i>Locally weighted
 * learning</i>
 * <a href="ftp://ftp.cc.gatech.edu/pub/people/cga/air1.ps.gz">download 
 * postscript</a>. <p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Produce debugging output. <p>
 *
 * -N <br>
 * Do not normalize numeric attributes' values in distance calculation.<p>
 *
 * -K num <br>
 * Set the number of neighbours used for setting kernel bandwidth.
 * (default all) <p>
 *
 * -U num <br>
 * Set the weighting kernel shape to use. 0 = Linear, 1 = Epnechnikov, 
 * 2 = Tricube, 3 = Inverse, 4 = Gaussian and 5 = Constant.
 * (default 0 = Linear) <p>
 *
 * -W classname <br>
 * Specify the full class name of a base classifier (which needs
 * to be a WeightedInstancesHandler).<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Ashraf M. Kibriya (amk14@waikato.ac.nz)
 * @version $Revision: 1.12.2.1 $ 
 */
public class LWL extends SingleClassifierEnhancer
  implements UpdateableClassifier, WeightedInstancesHandler {


  /** The training instances used for classification. */
  protected Instances m_Train;

  /** The minimum values for numeric attributes. */
  protected double [] m_Min;

  /** The maximum values for numeric attributes. */
  protected double [] m_Max;
    
  /** True if numeric attributes' values should not be normalized in distance 
      calculation. */
  protected boolean m_NoAttribNorm=false;

  /** The number of neighbours used to select the kernel bandwidth */
  protected int m_kNN = -1;

  /** The weighting kernel method currently selected */
  protected int m_WeightKernel = LINEAR;

  /** True if m_kNN should be set to all instances */
  protected boolean m_UseAllK = true;

  /** The available kernel weighting methods */
  protected static final int LINEAR       = 0;
  protected static final int EPANECHNIKOV = 1;
  protected static final int TRICUBE      = 2;  
  protected static final int INVERSE      = 3;
  protected static final int GAUSS        = 4;
  protected static final int CONSTANT     = 5;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Class for performing locally weighted learning. Can do "
      + "classification (e.g. using naive Bayes) or regression (e.g. using "
      + "linear regression). The base learner needs to implement "
      + "WeightedInstancesHandler. For more info, see\n\n"
      + "Eibe Frank, Mark Hall, and Bernhard Pfahringer (2003). \"Locally "
      + "Weighted Naive Bayes\". Conference on Uncertainty in AI.\n\n"
      + "Atkeson, C., A. Moore, and S. Schaal (1996) \"Locally weighted "
      + "learning\" AI Reviews.";
  }
    
  /**
   * Constructor.
   */
  public LWL() {
    
    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.DecisionStump";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tDo not normalize numeric attributes' "
                                    +"values in distance calculation.\n"
                                    +"\t(default DO normalization)",
				    "N", 0, "-N"));
    newVector.addElement(new Option("\tSet the number of neighbours used to set"
				    +" the kernel bandwidth.\n"
				    +"\t(default all)",
				    "K", 1, "-K <number of neighbours>"));
    newVector.addElement(new Option("\tSet the weighting kernel shape to use."
				    +" 0=Linear, 1=Epanechnikov,\n"
				    +"\t2=Tricube, 3=Inverse, 4=Gaussian.\n"
				    +"\t(default 0 = Linear)",
				    "U", 1,"-U <number of weighting method>"));
    
    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Produce debugging output. <p>
   *
   * -N <br>
   * Do not normalize numeric attributes' values in distance calculation.
   * (default DO normalization)<p>
   *
   * -K num <br>
   * Set the number of neighbours used for setting kernel bandwidth.
   * (default all) <p>
   *
   * -U num <br>
   * Set the weighting kernel shape to use. 0 = Linear, 1 = Epnechnikov, 
   * 2 = Tricube, 3 = Inverse, 4 = Gaussian and 5 = Constant.
   * (default 0 = Linear) <p>
   *
   * -W classname <br>
   * Specify the full class name of a base classifier (which needs
   * to be a WeightedInstancesHandler).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String knnString = Utils.getOption('K', options);
    if (knnString.length() != 0) {
      setKNN(Integer.parseInt(knnString));
    } else {
      setKNN(0);
    }

    String weightString = Utils.getOption('U', options);
    if (weightString.length() != 0) {
      setWeightingKernel(Integer.parseInt(weightString));
    } else {
      setWeightingKernel(LINEAR);
    }
    setDontNormalize(Utils.getFlag('N', options));
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 5];

    int current = 0;

    options[current++] = "-U"; options[current++] = "" + getWeightingKernel();
    //if (!m_UseAllK) {
      options[current++] = "-K"; options[current++] = "" + getKNN();
    //}
    if (getDontNormalize()) {
      options[current++] = "-N";
    }
    else
      options[current++] = "";

    System.arraycopy(superOptions, 0, options, current,
                     superOptions.length);

    return options;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String KNNTipText() {
    return "How many neighbours are used to determine the width of the "
      + "weighting function (<= 0 means all neighbours).";
  }

  /**
   * Sets the number of neighbours used for kernel bandwidth setting.
   * The bandwidth is taken as the distance to the kth neighbour.
   *
   * @param knn the number of neighbours included inside the kernel
   * bandwidth, or 0 to specify using all neighbors.
   */
  public void setKNN(int knn) {

    m_kNN = knn;
    if (knn <= 0) {
      m_kNN = 0;
      m_UseAllK = true;
    } else {
      m_UseAllK = false;
    }
  }

  /**
   * Gets the number of neighbours used for kernel bandwidth setting.
   * The bandwidth is taken as the distance to the kth neighbour.
   *
   * @return the number of neighbours included inside the kernel
   * bandwidth, or 0 for all neighbours
   */
  public int getKNN() {

    return m_kNN;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightingKernelTipText() {
    return "Determines weighting function. [0 = Linear, 1 = Epnechnikov,"+
	   "2 = Tricube, 3 = Inverse, 4 = Gaussian and 5 = Constant. "+
	   "(default 0 = Linear)].";
  }

  /**
   * Sets the kernel weighting method to use. Must be one of LINEAR, 
   * EPANECHNIKOV,  TRICUBE, INVERSE, GAUSS or CONSTANT, other values
   * are ignored.
   *
   * @param kernel the new kernel method to use. Must be one of LINEAR,
   * EPANECHNIKOV,  TRICUBE, INVERSE, GAUSS or CONSTANT.
   */
  public void setWeightingKernel(int kernel) {

    if ((kernel != LINEAR)
	&& (kernel != EPANECHNIKOV)
	&& (kernel != TRICUBE)
	&& (kernel != INVERSE)
	&& (kernel != GAUSS)
	&& (kernel != CONSTANT)) {
      return;
    }
    m_WeightKernel = kernel;
  }

  /**
   * Gets the kernel weighting method to use.
   *
   * @return the new kernel method to use. Will be one of LINEAR,
   * EPANECHNIKOV,  TRICUBE, INVERSE, GAUSS or CONSTANT.
   */
  public int getWeightingKernel() {

    return m_WeightKernel;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String dontNormalizeTipText() {
    return "Turns off normalization for attribute values in distance "+
	   "calculation.";
  }

  /** 
   * Gets whether if the numeric attribute values are not to be normalized for 
   * calculating the distances.
   *
   * @return true if normalization is not to be performed
   */
  public boolean getDontNormalize() {
      return m_NoAttribNorm;
  }  
  
  /** 
   * Sets whether if the numeric attribute values are not to be normalized for 
   * calculating the distances between them.
   *
   * @param dontNormalize true if normalization is not to be performed
   */
  public void setDontNormalize(boolean normalize) {
      m_NoAttribNorm = normalize;
  }

  /**
   * Gets an attributes minimum observed value
   *
   * @param index the index of the attribute
   * @return the minimum observed value
   */
  protected double getAttributeMin(int index) {

    return m_Min[index];
  }

  /**
   * Gets an attributes maximum observed value
   *
   * @param index the index of the attribute
   * @return the maximum observed value
   */
  protected double getAttributeMax(int index) {

    return m_Max[index];
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (!(m_Classifier instanceof WeightedInstancesHandler)) {
      throw new IllegalArgumentException("Classifier must be a "
					 + "WeightedInstancesHandler!");
    }

    if (instances.classIndex() < 0) {
      throw new Exception("No class attribute assigned to instances");
    }

    if (instances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string "+
                                                  "attributes!");
    }

    // Throw away training instances with missing class
    m_Train = new Instances(instances, 0, instances.numInstances());
    m_Train.deleteWithMissingClass();

    // only class? -> build ZeroR model
    if (m_Train.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data!), "
          + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(m_Train);
      return;
    }
    else {
      m_ZeroR = null;
    }
    
    // Calculate the minimum and maximum values
    m_Min = new double [m_Train.numAttributes()];
    m_Max = new double [m_Train.numAttributes()];
    for (int i = 0; i < m_Train.numAttributes(); i++) {
      m_Min[i] = m_Max[i] = Double.NaN;
    }
    for (int i = 0; i < m_Train.numInstances(); i++) {
      updateMinMax(m_Train.instance(i));
    }
  }

  /**
   * Adds the supplied instance to the training set
   *
   * @param instance the instance to add
   * @exception Exception if instance could not be incorporated
   * successfully
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (m_Train.equalHeaders(instance.dataset()) == false) {
      throw new Exception("Incompatible instance types");
    }
    if (!instance.classIsMissing()) {
      updateMinMax(instance);
      m_Train.add(instance);
    }
  }
  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @exception Exception if distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(instance);
    }
    
    if (m_Train.numInstances() == 0) {
      throw new Exception("No training instances!");
    }

    updateMinMax(instance);

    //Get the distances to each training instance
    double [] distance = new double [m_Train.numInstances()];
    MyHeap h;
    int k = distance.length-1; //sortKey.length - 1;
    if (!m_UseAllK && (m_kNN < k)) {
        k = m_kNN;
        h = new MyHeap(k);
    }
    else
        h = new MyHeap(distance.length);
    
    for(int i=0, insCount=0; i < m_Train.numInstances(); i++) {
        switch(m_WeightKernel) {
          case LINEAR:
          case EPANECHNIKOV:
          case TRICUBE:      
              if(insCount<k) {
                  distance[i] = distance(instance, m_Train.instance(i));
                  h.put(i, distance[i]);
              }
              else {
                  MyHeapElement temp = h.peek();
                  distance[i] = distance(instance, m_Train.instance(i), 
                                         temp.distance);
                  if(distance[i]<temp.distance) {
                      h.get();
                      h.put(i, distance[i]);
                  }
              }
              break;
          default:
              distance[i] = distance(instance, m_Train.instance(i));
              break;
        }
        insCount++;
    }
    
    int [] sortKey;
    sortKey = Utils.sort(distance);
    
    if (m_Debug) {
      System.out.println("Instance Distances");
      for (int i = 0; i < sortKey.length; i++) {
	System.out.println("" + distance[sortKey[i]]);
      }
    }
    
    // Determine the bandwidth
    double bandwidth = distance[sortKey[k-1]];

    // Check for bandwidth zero
    if (bandwidth <= 0) {
      for (int i = k; i < sortKey.length; i++) {
	if (distance[sortKey[i]] > bandwidth) {
	  bandwidth = distance[sortKey[i]];
	  break;
	}
      }    
      if(bandwidth <= 0) {
	throw new Exception("All training instances coincide with test "+
                            "instance!");
      }
    }
    
    // Rescale the distances by the bandwidth
    for (int i = 0; i < distance.length; i++) {
      distance[i] = distance[i] / bandwidth;
    }

    // Pass the distances through a weighting kernel
    for (int i = 0; i < distance.length; i++) {
      switch (m_WeightKernel) {
      case LINEAR:
	distance[i] = Math.max(1.0001 - distance[i], 0);
	break;
      case EPANECHNIKOV:
          if(distance[i]<=1)
              distance[i] = 3/4D*(1.0001 - distance[i]*distance[i]);
          else
              distance[i] = 0;
          break;
      case TRICUBE:
          if(distance[i]<=1)
              distance[i] = Math.pow( (1.0001 - Math.pow(distance[i], 3)), 3 );
          else
              distance[i] = 0;
          break;
      case CONSTANT:
          //System.err.println("using constant kernel");
          if(distance[i]<=1)
            distance[i] = 1;
          else
            distance[i] = 0;
          break;
      case INVERSE:
	distance[i] = 1.0 / (1.0 + distance[i]);
	break;
      case GAUSS:
	distance[i] = Math.exp(-distance[i] * distance[i]);
	break;
      }
    }

    if (m_Debug) {
      System.out.println("Instance Weights");
      for (int i = 0; i < sortKey.length; i++) {
	System.out.println("" + distance[sortKey[i]]);
      }
    }

    // Set the weights on a copy of the training data
    Instances weightedTrain = new Instances(m_Train, 0);
    double sumOfWeights = 0, newSumOfWeights = 0;
    for (int i = 0; i < sortKey.length; i++) {
      double weight = distance[sortKey[i]];
      if (weight < 1e-20) {
	break;
      }
      Instance newInst = (Instance) m_Train.instance(sortKey[i]).copy();
      sumOfWeights += newInst.weight();
      newSumOfWeights += newInst.weight() * weight;
      newInst.setWeight(newInst.weight() * weight);
      weightedTrain.add(newInst);
    }
    if (m_Debug) {
      System.out.println("Kept " + weightedTrain.numInstances() + " out of "
			 + m_Train.numInstances() + " instances");
    }
    
    // Rescale weights
    for (int i = 0; i < weightedTrain.numInstances(); i++) {
      Instance newInst = weightedTrain.instance(i);
      newInst.setWeight(newInst.weight() * sumOfWeights / newSumOfWeights);
    }

    // Create a weighted classifier
    m_Classifier.buildClassifier(weightedTrain);

    if (m_Debug) {
      System.out.println("Classifying test instance: " + instance);
      System.out.println("Built base classifier:\n" 
			 + m_Classifier.toString());
    }

    // Return the classifier's predictions
    return m_Classifier.distributionForInstance(instance);
  }
 
  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
      buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }
    
    if (m_Train == null) {
      return "Locally weighted learning: No model built yet.";
    }
    String result = "Locally weighted learning\n"
      + "===========================\n";

    result += "Using classifier: " + m_Classifier.getClass().getName() + "\n";

    switch (m_WeightKernel) {
    case LINEAR:
      result += "Using linear weighting kernels\n";
      break;
    case EPANECHNIKOV:
      result += "Using epanechnikov weighting kernels\n";
      break;
    case TRICUBE:
      result += "Using tricube weighting kernels\n";
      break;
    case INVERSE:
      result += "Using inverse-distance weighting kernels\n";
      break;
    case GAUSS:
      result += "Using gaussian weighting kernels\n";
      break;
    case CONSTANT:
      result += "Using constant weighting kernels\n";
      break;
    }
    result += "Using " + (m_UseAllK ? "all" : "" + m_kNN) + " neighbours";
    return result;
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances, between 0 and 1
   */          
  private double distance(Instance first, Instance second) throws Exception {
      return distance(first, second, Math.sqrt(Double.MAX_VALUE));
  }
  
  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @param cutOffValue skips the rest of the calculations and returns Double.Max
   * if distance is going to become larger than this cutOffValue.
   * @return the distance between the two given instances, between 0 and 1
   */          
  private double distance(Instance first, Instance second, double cutOffValue) 
          throws Exception {
    return euclideanDistance(first, second, cutOffValue);
  }    
  
  /**
   * Calculates the euclidean distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @param cutOffValue skips the rest of the calculations and returns Double.Max
   * if distance is going to become larger than this cutOffValue.
   * @return the distance between the two given instances, between 0 and 1
   */
  private double euclideanDistance(Instance first, Instance second, 
                                   double cutOffValue) {

    double distance = 0;
    int firstI, secondI;
    cutOffValue = cutOffValue*cutOffValue;
    
    for (int p1 = 0, p2 = 0; 
	 p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	firstI = m_Train.numAttributes();
      } else {
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = m_Train.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      if (firstI == m_Train.classIndex()) {
	p1++; continue;
      } 
      if (secondI == m_Train.classIndex()) {
	p2++; continue;
      } 
      double diff;
      if (firstI == secondI) {
	diff = difference(firstI, 
			  first.valueSparse(p1),
			  second.valueSparse(p2));
	p1++; p2++;
      } else if (firstI > secondI) {
	diff = difference(secondI, 
			  0, second.valueSparse(p2));
	p2++;
      } else {
	diff = difference(firstI, 
			  first.valueSparse(p1), 0);
	p1++;
      }
      distance += diff * diff;
      if(distance>cutOffValue)
          return Double.MAX_VALUE; //distance;
    }
    distance = Math.sqrt(distance);
    return distance;
  }
   
  /**
   * Computes the difference between two given attribute
   * values.
   */
  private double difference(int index, double val1, double val2) {
    
    switch (m_Train.attribute(index).type()) {
      case Attribute.NOMINAL:
        
        // If attribute is nominal
        if(Instance.isMissingValue(val1) ||
           Instance.isMissingValue(val2) ||
           ((int)val1 != (int)val2)) {
          return 1;
        } else {
          return 0;
        }
      case Attribute.NUMERIC:
        // If attribute is numeric
        if (Instance.isMissingValue(val1) ||
        Instance.isMissingValue(val2)) {
          if(Instance.isMissingValue(val1) &&
             Instance.isMissingValue(val2)) {
            if(m_NoAttribNorm==false)  //We are doing normalization
              return 1;
            else
              return (m_Max[index] - m_Min[index]);
          } else {
            double diff;
            if (Instance.isMissingValue(val2)) {
              diff = (m_NoAttribNorm==false) ? norm(val1, index) : val1;
            } else {
              diff = (m_NoAttribNorm==false) ? norm(val2, index) : val2;
            }
            if (m_NoAttribNorm==false && diff < 0.5) {
              diff = 1.0 - diff;
            }
            else if (m_NoAttribNorm==true) {
              if((m_Max[index]-diff) > (diff-m_Min[index]))
                return m_Max[index]-diff;
              else
                return diff-m_Min[index];
            }
            return diff;
          }
        } else {
          return (m_NoAttribNorm==false) ? 
                                  (norm(val1, index) - norm(val2, index)) :
                                  (val1 - val2);
        }
      default:
        return 0;
    }
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) {

    if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i], m_Min[i])) {
      return 0;
    } else {
      return (x - m_Min[i]) / (m_Max[i] - m_Min[i]);
    }
  }
                      
  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {  

    for (int j = 0; j < m_Train.numAttributes(); j++) {
      if (!instance.isMissing(j)) {
	if (Double.isNaN(m_Min[j])) {
	  m_Min[j] = instance.value(j);
	  m_Max[j] = instance.value(j);
	} else if (instance.value(j) < m_Min[j]) {
	  m_Min[j] = instance.value(j);
	} else if (instance.value(j) > m_Max[j]) {
	  m_Max[j] = instance.value(j);
	}
      }
    }
  }
  

  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(
	    new LWL(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
  
  
  private class MyHeap {
    //m_heap[0].index containts the current size of the heap
    //m_heap[0].distance is unused.
    MyHeapElement m_heap[] = null;
    public MyHeap(int maxSize) {
        if((maxSize%2)==0)
            maxSize++;
        
        m_heap = new MyHeapElement[maxSize+1];
        m_heap[0] = new MyHeapElement(0, 0);
        //System.err.println("m_heap size is: "+m_heap.length);
    }
    public int size() {
        return m_heap[0].index;
    }
    public MyHeapElement peek() {
        return m_heap[1];
    }
    public MyHeapElement get() throws Exception  {
        if(m_heap[0].index==0)
            throw new Exception("No elements present in the heap");
        MyHeapElement r = m_heap[1];
        m_heap[1] = m_heap[m_heap[0].index];
        m_heap[0].index--;
        downheap();
        return r;
    }
    public void put(int i, double d) throws Exception {
        if((m_heap[0].index+1)>(m_heap.length-1))
            throw new Exception("the number of elements cannot exceed the "+
                                "initially set maximum limit");
        m_heap[0].index++;
        m_heap[m_heap[0].index] = new MyHeapElement(i, d);
        //m_heap[m_heap[0].index].index = i;
        //m_heap[m_heap[0].index].distance = d;        
        //System.err.print("new size: "+(int)m_heap[0]+", ");
        upheap();
    }
    private void upheap() {
        int i = m_heap[0].index;
        MyHeapElement temp;
        while( i > 1  && m_heap[i].distance>m_heap[i/2].distance) {
            temp = m_heap[i];
            m_heap[i] = m_heap[i/2];
            i = i/2;
            m_heap[i] = temp; //this is i/2 done here to avoid another division.
        }
    }
    private void downheap() {
        int i = 1;
        MyHeapElement temp;
        while( (2*i) <= m_heap[0].index && 
                  (m_heap[i].distance < m_heap[2*i].distance || 
                   m_heap[i].distance<m_heap[2*i+1].distance )) {
            if((2*i+1)<=m_heap[0].index) {
                if(m_heap[2*i].distance>m_heap[2*i+1].distance) {
                    temp = m_heap[i];
                    m_heap[i] = m_heap[2*i];
                    i = 2*i;
                    m_heap[i] = temp;
                }
                else {
                    temp = m_heap[i];
                    m_heap[i] = m_heap[2*i+1];
                    i = 2*i+1;
                    m_heap[i] = temp;
                }
            }
            else {
                temp = m_heap[i];
                m_heap[i] = m_heap[2*i];
                i = 2*i;
                m_heap[i] = temp;
            }
        }
    }    
    
  }
  
  private class MyHeapElement {
      int index;
      double distance; 
      public MyHeapElement(int i, double d) {
          distance = d; index = i;
      }
  }
  
}
