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
 *    IBk.java
 *    Copyright (C) 1999 Stuart Inglis,Len Trigg,Eibe Frank
 *
 */

package weka.classifiers.lazy;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.UpdateableClassifier;
import java.io.*;
import java.util.*;
import weka.core.*;


/**
 * <i>K</i>-nearest neighbours classifier. For more information, see <p>
 * 
 * Aha, D., and D. Kibler (1991) "Instance-based learning algorithms",
 * <i>Machine Learning</i>, vol.6, pp. 37-66.<p>
 *
 * Valid options are:<p>
 *
 * -K num <br>
 * Set the number of nearest neighbors to use in prediction
 * (default 1) <p>
 *
 * -W num <br>
 * Set a fixed window size for incremental train/testing. As
 * new training instances are added, oldest instances are removed
 * to maintain the number of training instances at this size.
 * (default no window) <p>
 *
 * -I <br>
 * Neighbors will be weighted by the inverse of their distance
 * when voting. (default equal weighting) <p>
 *
 * -F <br>
 * Neighbors will be weighted by their similarity when voting.
 * (default equal weighting) <p>
 *
 * -X <br>
 * Select the number of neighbors to use by hold-one-out cross
 * validation, with an upper limit given by the -K option. <p>
 *
 * -E <br>
 * When k is selected by cross-validation for numeric class attributes,
 * minimize mean-squared error. (default mean absolute error) <p>
 *
 * -N <br>
 * Turns off normalization. <p>
 *
 * @author Stuart Inglis (singlis@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.32 $
 */
public class IBk extends Classifier implements
  OptionHandler, UpdateableClassifier, WeightedInstancesHandler {

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "K-nearest neighbours classifier. Normalizes attributes by default. Can "
      + "select appropriate value of K based on cross-validation. Can also do "
      + "distance weighting. For more information, see\n\n"
      + "Aha, D., and D. Kibler (1991) \"Instance-based learning algorithms\", "
      + "Machine Learning, vol.6, pp. 37-66.";
  }

  /*
   * A class for storing data about a neighboring instance
   */
  protected class NeighborNode {

    /** The neighbor instance */
    protected Instance m_Instance;

    /** The distance from the current instance to this neighbor */
    protected double m_Distance;

    /** A link to the next neighbor instance */
    protected NeighborNode m_Next;
    
    /**
     * Create a new neighbor node.
     *
     * @param distance the distance to the neighbor
     * @param instance the neighbor instance
     * @param next the next neighbor node
     */
    public NeighborNode(double distance, Instance instance, NeighborNode next){
      m_Distance = distance;
      m_Instance = instance;
      m_Next = next;
    }

    /**
     * Create a new neighbor node that doesn't link to any other nodes.
     *
     * @param distance the distance to the neighbor
     * @param instance the neighbor instance
     */
    public NeighborNode(double distance, Instance instance) {

      this(distance, instance, null);
    }
  }

  /*
   * A class for a linked list to store the nearest k neighbours
   * to an instance. We use a list so that we can take care of
   * cases where multiple neighbours are the same distance away.
   * i.e. the minimum length of the list is k.
   */
  protected class NeighborList {

    /** The first node in the list */
    protected NeighborNode m_First;

    /** The last node in the list */
    protected NeighborNode m_Last;

    /** The number of nodes to attempt to maintain in the list */
    protected int m_Length = 1;
    
    /**
     * Creates the neighborlist with a desired length
     *
     * @param length the length of list to attempt to maintain
     */
    public NeighborList(int length) {

      m_Length = length;
    }

    /**
     * Gets whether the list is empty.
     *
     * @return true if so
     */
    public boolean isEmpty() {

      return (m_First == null);
    }

    /**
     * Gets the current length of the list.
     *
     * @return the current length of the list
     */
    public int currentLength() {

      int i = 0;
      NeighborNode current = m_First;
      while (current != null) {
	i++;
	current = current.m_Next;
      }
      return i;
    }

    /**
     * Inserts an instance neighbor into the list, maintaining the list
     * sorted by distance.
     *
     * @param distance the distance to the instance
     * @param instance the neighboring instance
     */
    public void insertSorted(double distance, Instance instance) {

      if (isEmpty()) {
	m_First = m_Last = new NeighborNode(distance, instance);
      } else {
	NeighborNode current = m_First;
	if (distance < m_First.m_Distance) {// Insert at head
	  m_First = new NeighborNode(distance, instance, m_First);
	} else { // Insert further down the list
	  for( ;(current.m_Next != null) && 
		 (current.m_Next.m_Distance < distance); 
	       current = current.m_Next);
	  current.m_Next = new NeighborNode(distance, instance,
					    current.m_Next);
	  if (current.equals(m_Last)) {
	    m_Last = current.m_Next;
	  }
	}

	// Trip down the list until we've got k list elements (or more if the
	// distance to the last elements is the same).
	int valcount = 0;
	for(current = m_First; current.m_Next != null; 
	    current = current.m_Next) {
	  valcount++;
	  if ((valcount >= m_Length) && (current.m_Distance != 
					 current.m_Next.m_Distance)) {
	    m_Last = current;
	    current.m_Next = null;
	    break;
	  }
	}
      }
    }

    /**
     * Prunes the list to contain the k nearest neighbors. If there are
     * multiple neighbors at the k'th distance, all will be kept.
     *
     * @param k the number of neighbors to keep in the list.
     */
    public void pruneToK(int k) {

      if (isEmpty()) {
	return;
      }
      if (k < 1) {
	k = 1;
      }
      int currentK = 0;
      double currentDist = m_First.m_Distance;
      NeighborNode current = m_First;
      for(; current.m_Next != null; current = current.m_Next) {
	currentK++;
	currentDist = current.m_Distance;
	if ((currentK >= k) && (currentDist != current.m_Next.m_Distance)) {
	  m_Last = current;
	  current.m_Next = null;
	  break;
	}
      }
    }

    /**
     * Prints out the contents of the neighborlist
     */
    public void printList() {

      if (isEmpty()) {
	System.out.println("Empty list");
      } else {
	NeighborNode current = m_First;
	while (current != null) {
	  System.out.println("Node: instance " + current.m_Instance 
			     + ", distance " + current.m_Distance);
	  current = current.m_Next;
	}
	System.out.println();
      }
    }
  }

  /** The training instances used for classification. */
  protected Instances m_Train;

  /** The number of class values (or 1 if predicting numeric) */
  protected int m_NumClasses;

  /** The class attribute type */
  protected int m_ClassType;

  /** The minimum values for numeric attributes. */
  protected double [] m_Min;

  /** The maximum values for numeric attributes. */
  protected double [] m_Max;

  /** The number of neighbours to use for classification (currently) */
  protected int m_kNN;

  /**
   * The value of kNN provided by the user. This may differ from
   * m_kNN if cross-validation is being used
   */
  protected int m_kNNUpper;

  /**
   * Whether the value of k selected by cross validation has
   * been invalidated by a change in the training instances
   */
  protected boolean m_kNNValid;

  /**
   * The maximum number of training instances allowed. When
   * this limit is reached, old training instances are removed,
   * so the training data is "windowed". Set to 0 for unlimited
   * numbers of instances.
   */
  protected int m_WindowSize;

  /** Whether the neighbours should be distance-weighted */
  protected int m_DistanceWeighting;

  /** Whether to select k by cross validation */
  protected boolean m_CrossValidate;

  /**
   * Whether to minimise mean squared error rather than mean absolute
   * error when cross-validating on numeric prediction tasks
   */
  protected boolean m_MeanSquared;

  /** True if normalization is turned off */
  protected boolean m_DontNormalize;

  /* Define possible instance weighting methods */
  public static final int WEIGHT_NONE = 1;
  public static final int WEIGHT_INVERSE = 2;
  public static final int WEIGHT_SIMILARITY = 4;
  public static final Tag [] TAGS_WEIGHTING = {
    new Tag(WEIGHT_NONE, "No distance weighting"),
    new Tag(WEIGHT_INVERSE, "Weight by 1/distance"),
    new Tag(WEIGHT_SIMILARITY, "Weight by 1-distance")
  };

  /** The number of attributes the contribute to a prediction */
  protected double m_NumAttributesUsed;
								   
  /**
   * IBk classifier. Simple instance-based learner that uses the class
   * of the nearest k training instances for the class of the test
   * instances.
   *
   * @param k the number of nearest neighbors to use for prediction
   */
  public IBk(int k) {

    init();
    setKNN(k);
  }  

  /**
   * IB1 classifer. Instance-based learner. Predicts the class of the
   * single nearest training instance for each test instance.
   */
  public IBk() {

    init();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String KNNTipText() {
    return "The number of neighbours to use.";
  }
  
  /**
   * Set the number of neighbours the learner is to use.
   *
   * @param k the number of neighbours.
   */
  public void setKNN(int k) {

    m_kNN = k;
    m_kNNUpper = k;
    m_kNNValid = false;
  }

  /**
   * Gets the number of neighbours the learner will use.
   *
   * @return the number of neighbours.
   */
  public int getKNN() {

    return m_kNN;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String windowSizeTipText() {
    return "Gets the maximum number of instances allowed in the training " +
      "pool. The addition of new instances above this value will result " +
      "in old instances being removed. A value of 0 signifies no limit " +
      "to the number of training instances.";
  }
  
  /**
   * Gets the maximum number of instances allowed in the training
   * pool. The addition of new instances above this value will result
   * in old instances being removed. A value of 0 signifies no limit
   * to the number of training instances.
   *
   * @return Value of WindowSize.
   */
  public int getWindowSize() {
    
    return m_WindowSize;
  }
  
  /**
   * Sets the maximum number of instances allowed in the training
   * pool. The addition of new instances above this value will result
   * in old instances being removed. A value of 0 signifies no limit
   * to the number of training instances.
   *
   * @param newWindowSize Value to assign to WindowSize.
   */
  public void setWindowSize(int newWindowSize) {
    
    m_WindowSize = newWindowSize;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String distanceWeightingTipText() {

    return "Gets the distance weighting method used.";
  }
  
  /**
   * Gets the distance weighting method used. Will be one of
   * WEIGHT_NONE, WEIGHT_INVERSE, or WEIGHT_SIMILARITY
   *
   * @return the distance weighting method used.
   */
  public SelectedTag getDistanceWeighting() {

    return new SelectedTag(m_DistanceWeighting, TAGS_WEIGHTING);
  }
  
  /**
   * Sets the distance weighting method used. Values other than
   * WEIGHT_NONE, WEIGHT_INVERSE, or WEIGHT_SIMILARITY will be ignored.
   *
   * @param newDistanceWeighting the distance weighting method to use
   */
  public void setDistanceWeighting(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_WEIGHTING) {
      m_DistanceWeighting = newMethod.getSelectedTag().getID();
    }
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String meanSquaredTipText() {

    return "Whether the mean squared error is used rather than mean "
      + "absolute error when doing cross-validation for regression problems.";
  }

  /**
   * Gets whether the mean squared error is used rather than mean
   * absolute error when doing cross-validation.
   *
   * @return true if so.
   */
  public boolean getMeanSquared() {
    
    return m_MeanSquared;
  }
  
  /**
   * Sets whether the mean squared error is used rather than mean
   * absolute error when doing cross-validation.
   *
   * @param newMeanSquared true if so.
   */
  public void setMeanSquared(boolean newMeanSquared) {
    
    m_MeanSquared = newMeanSquared;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String crossValidateTipText() {

    return "Whether hold-one-out cross-validation will be used " +
      "to select the best k value.";
  }
  
  /**
   * Gets whether hold-one-out cross-validation will be used
   * to select the best k value
   *
   * @return true if cross-validation will be used.
   */
  public boolean getCrossValidate() {
    
    return m_CrossValidate;
  }
  
  /**
   * Sets whether hold-one-out cross-validation will be used
   * to select the best k value
   *
   * @param newCrossValidate true if cross-validation should be used.
   */
  public void setCrossValidate(boolean newCrossValidate) {
    
    m_CrossValidate = newCrossValidate;
  }
  
  /**
   * Get the number of training instances the classifier is currently using
   */
  public int getNumTraining() {

    return m_Train.numInstances();
  }

  /**
   * Get an attributes minimum observed value
   */
  public double getAttributeMin(int index) throws Exception {

    if (m_Min == null) {
      throw new Exception("Minimum value for attribute not available!");
    }
    return m_Min[index];
  }

  /**
   * Get an attributes maximum observed value
   */
  public double getAttributeMax(int index) throws Exception {

    if (m_Max == null) {
      throw new Exception("Maximum value for attribute not available!");
    }
    return m_Max[index];
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String noNormalizationTipText() {

    return "Whether attribute normalization is turned off.";
  }
  
  /**
   * Gets whether normalization is turned off.
   * @return Value of DontNormalize.
   */
  public boolean getNoNormalization() {
    
    return m_DontNormalize;
  }
  
  /**
   * Set whether normalization is turned off.
   * @param v  Value to assign to DontNormalize.
   */
  public void setNoNormalization(boolean v) {
    
    m_DontNormalize = v;
  }
  
  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */

  public void buildClassifier(Instances instances) throws Exception {

    if (instances.classIndex() < 0) {
      throw new Exception ("No class attribute assigned to instances");
    }
    if (instances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    try {
      m_NumClasses = instances.numClasses();
      m_ClassType = instances.classAttribute().type();
    } catch (Exception ex) {
      throw new Error("This should never be reached");
    }

    // Throw away training instances with missing class
    m_Train = new Instances(instances, 0, instances.numInstances());
    m_Train.deleteWithMissingClass();

    // Throw away initial instances until within the specified window size
    if ((m_WindowSize > 0) && (instances.numInstances() > m_WindowSize)) {
      m_Train = new Instances(m_Train, 
			      m_Train.numInstances()-m_WindowSize, 
			      m_WindowSize);
    }

    // Calculate the minimum and maximum values
    if (m_DontNormalize) {
      m_Min = null; m_Max = null;
    } else {
      m_Min = new double [m_Train.numAttributes()];
      m_Max = new double [m_Train.numAttributes()];
      for (int i = 0; i < m_Train.numAttributes(); i++) {
	m_Min[i] = m_Max[i] = Double.NaN;
      }
      Enumeration enu = m_Train.enumerateInstances();
      while (enu.hasMoreElements()) {
	updateMinMax((Instance) enu.nextElement());
      }
    }

    // Compute the number of attributes that contribute
    // to each prediction
    m_NumAttributesUsed = 0.0;
    for (int i = 0; i < m_Train.numAttributes(); i++) {
      if ((i != m_Train.classIndex()) && 
	  (m_Train.attribute(i).isNominal() ||
	   m_Train.attribute(i).isNumeric())) {
	m_NumAttributesUsed += 1.0;
      }
    }

    // Invalidate any currently cross-validation selected k
    m_kNNValid = false;
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
    if (instance.classIsMissing()) {
      return;
    }
    if (!m_DontNormalize) {
      updateMinMax(instance);
    }
    m_Train.add(instance);
    m_kNNValid = false;
    if ((m_WindowSize > 0) && (m_Train.numInstances() > m_WindowSize)) {
      while (m_Train.numInstances() > m_WindowSize) {
	m_Train.delete(0);
      }
    }
  }

  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if an error occurred during the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception{

    if (m_Train.numInstances() == 0) {
      throw new Exception("No training instances!");
    }
    if ((m_WindowSize > 0) && (m_Train.numInstances() > m_WindowSize)) {
      m_kNNValid = false;
      while (m_Train.numInstances() > m_WindowSize) {
	m_Train.delete(0);
      }
    }

    // Select k by cross validation
    if (!m_kNNValid && (m_CrossValidate) && (m_kNNUpper >= 1)) {
      crossValidate();
    }
    if (!m_DontNormalize) {
      updateMinMax(instance);
    }

    NeighborList neighborlist = findNeighbors(instance);
    return makeDistribution(neighborlist);
  }
 

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(8);

    newVector.addElement(new Option(
	      "\tWeight neighbours by the inverse of their distance\n"
	      +"\t(use when k > 1)",
	      "I", 0, "-I"));
    newVector.addElement(new Option(
	      "\tWeight neighbours by 1 - their distance\n"
	      +"\t(use when k > 1)",
	      "F", 0, "-F"));
    newVector.addElement(new Option(
	      "\tNumber of nearest neighbours (k) used in classification.\n"
	      +"\t(Default = 1)",
	      "K", 1,"-K <number of neighbors>"));
    newVector.addElement(new Option(
              "\tMinimise mean squared error rather than mean absolute\n"
	      +"\terror when using -X option with numeric prediction.",
	      "E", 0,"-E"));
    newVector.addElement(new Option(
              "\tMaximum number of training instances maintained.\n"
	      +"\tTraining instances are dropped FIFO. (Default = no window)",
	      "W", 1,"-W <window size>"));
    newVector.addElement(new Option(
	      "\tSelect the number of nearest neighbours between 1\n"
	      +"\tand the k value specified using hold-one-out evaluation\n"
	      +"\ton the training data (use when k > 1)",
	      "X", 0,"-X"));
    newVector.addElement(new Option(
	      "\tDon't normalize the data.\n",
	      "N", 0, "-N"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -K num <br>
   * Set the number of nearest neighbors to use in prediction
   * (default 1) <p>
   *
   * -W num <br>
   * Set a fixed window size for incremental train/testing. As
   * new training instances are added, oldest instances are removed
   * to maintain the number of training instances at this size.
   * (default no window) <p>
   *
   * -I <br>
   * Neighbors will be weighted by the inverse of their distance
   * when voting. (default equal weighting) <p>
   *
   * -F <br>
   * Neighbors will be weighted by their similarity when voting.
   * (default equal weighting) <p>
   *
   * -X <br>
   * Select the number of neighbors to use by hold-one-out cross
   * validation, with an upper limit given by the -K option. <p>
   *
   * -E <br>
   * When k is selected by cross-validation for numeric class attributes,
   * minimize mean-squared error. (default mean absolute error) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String knnString = Utils.getOption('K', options);
    if (knnString.length() != 0) {
      setKNN(Integer.parseInt(knnString));
    } else {
      setKNN(1);
    }
    String windowString = Utils.getOption('W', options);
    if (windowString.length() != 0) {
      setWindowSize(Integer.parseInt(windowString));
    } else {
      setWindowSize(0);
    }
    if (Utils.getFlag('I', options)) {
      setDistanceWeighting(new SelectedTag(WEIGHT_INVERSE, TAGS_WEIGHTING));
    } else if (Utils.getFlag('F', options)) {
      setDistanceWeighting(new SelectedTag(WEIGHT_SIMILARITY, TAGS_WEIGHTING));
    } else {
      setDistanceWeighting(new SelectedTag(WEIGHT_NONE, TAGS_WEIGHTING));
    }
    setCrossValidate(Utils.getFlag('X', options));
    setMeanSquared(Utils.getFlag('E', options));
    setNoNormalization(Utils.getFlag('N', options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    String [] options = new String [11];
    int current = 0;
    options[current++] = "-K"; options[current++] = "" + getKNN();
    options[current++] = "-W"; options[current++] = "" + m_WindowSize;
    if (getCrossValidate()) {
      options[current++] = "-X";
    }
    if (getMeanSquared()) {
      options[current++] = "-E";
    }
    if (m_DistanceWeighting == WEIGHT_INVERSE) {
      options[current++] = "-I";
    } else if (m_DistanceWeighting == WEIGHT_SIMILARITY) {
      options[current++] = "-F";
    }
    if (m_DontNormalize) {
      options[current++] = "-N";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    if (m_Train == null) {
      return "IBk: No model built yet.";
    }

    if (!m_kNNValid && m_CrossValidate) {
      crossValidate();
    }

    String result = "IB1 instance-based classifier\n" +
      "using " + m_kNN;

    switch (m_DistanceWeighting) {
    case WEIGHT_INVERSE:
      result += " inverse-distance-weighted";
      break;
    case WEIGHT_SIMILARITY:
      result += " similarity-weighted";
      break;
    }
    result += " nearest neighbour(s) for classification\n";

    if (m_WindowSize != 0) {
      result += "using a maximum of " 
	+ m_WindowSize + " (windowed) training instances\n";
    }
    return result;
  }

  /**
   * Initialise scheme variables.
   */
  protected void init() {

    setKNN(1);
    m_WindowSize = 0;
    m_DistanceWeighting = WEIGHT_NONE;
    m_CrossValidate = false;
    m_MeanSquared = false;
    m_DontNormalize = false;
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances, between 0 and 1
   */          
  protected double distance(Instance first, Instance second) {  

    double distance = 0;
    int firstI, secondI;

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
    }
    
    return Math.sqrt(distance / m_NumAttributesUsed);
  }
   
  /**
   * Computes the difference between two given attribute
   * values.
   */
  protected double difference(int index, double val1, double val2) {

    switch (m_Train.attribute(index).type()) {
    case Attribute.NOMINAL:
      
      // If attribute is nominal
      if (Instance.isMissingValue(val1) || 
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
	if (Instance.isMissingValue(val1) && 
	    Instance.isMissingValue(val2)) {
	  return 1;
	} else {
	  double diff;
	  if (Instance.isMissingValue(val2)) {
	    diff = norm(val1, index);
	  } else {
	    diff = norm(val2, index);
	  }
	  if (diff < 0.5) {
	    diff = 1.0 - diff;
	  }
	  return diff;
	}
      } else {
	return norm(val1, index) - norm(val2, index);
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
  protected double norm(double x, int i) {

    if (m_DontNormalize) {
      return x;
    } else if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i],m_Min[i])) {
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
  protected void updateMinMax(Instance instance) {  

    for (int j = 0;j < m_Train.numAttributes(); j++) {
      if (!instance.isMissing(j)) {
	if (Double.isNaN(m_Min[j])) {
	  m_Min[j] = instance.value(j);
	  m_Max[j] = instance.value(j);
	} else {
	  if (instance.value(j) < m_Min[j]) {
	    m_Min[j] = instance.value(j);
	  } else {
	    if (instance.value(j) > m_Max[j]) {
	      m_Max[j] = instance.value(j);
	    }
	  }
	}
      }
    }
  }
    
  /**
   * Build the list of nearest k neighbors to the given test instance.
   *
   * @param instance the instance to search for neighbours of
   * @return a list of neighbors
   */
  protected NeighborList findNeighbors(Instance instance) {

    double distance;
    NeighborList neighborlist = new NeighborList(m_kNN);
    Enumeration enu = m_Train.enumerateInstances();
    int i = 0;

    while (enu.hasMoreElements()) {
      Instance trainInstance = (Instance) enu.nextElement();
      if (instance != trainInstance) { // for hold-one-out cross-validation
	distance = distance(instance, trainInstance);
	if (neighborlist.isEmpty() || (i < m_kNN) || 
	    (distance <= neighborlist.m_Last.m_Distance)) {
	  neighborlist.insertSorted(distance, trainInstance);
	}
	i++;
      }
    }

    return neighborlist;
  }

  /**
   * Turn the list of nearest neighbors into a probability distribution
   *
   * @param neighborlist the list of nearest neighboring instances
   * @return the probability distribution
   */
  protected double [] makeDistribution(NeighborList neighborlist) 
    throws Exception {

    double total = 0, weight;
    double [] distribution = new double [m_NumClasses];
    
    // Set up a correction to the estimator
    if (m_ClassType == Attribute.NOMINAL) {
      for(int i = 0; i < m_NumClasses; i++) {
	distribution[i] = 1.0 / Math.max(1,m_Train.numInstances());
      }
      total = (double)m_NumClasses / Math.max(1,m_Train.numInstances());
    }

    if (!neighborlist.isEmpty()) {
      // Collect class counts
      NeighborNode current = neighborlist.m_First;
      while (current != null) {
	switch (m_DistanceWeighting) {
	case WEIGHT_INVERSE:
	  weight = 1.0 / (current.m_Distance + 0.001); // to avoid div by zero
	  break;
	case WEIGHT_SIMILARITY:
	  weight = 1.0 - current.m_Distance;
	  break;
	default:                                       // WEIGHT_NONE:
	  weight = 1.0;
	  break;
	}
	weight *= current.m_Instance.weight();
	try {
	  switch (m_ClassType) {
	  case Attribute.NOMINAL:
	    distribution[(int)current.m_Instance.classValue()] += weight;
	    break;
	  case Attribute.NUMERIC:
	    distribution[0] += current.m_Instance.classValue() * weight;
	    break;
	  }
	} catch (Exception ex) {
	  throw new Error("Data has no class attribute!");
	}
	total += weight;

	current = current.m_Next;
      }
    }

    // Normalise distribution
    if (total > 0) {
      Utils.normalize(distribution, total);
    }
    return distribution;
  }

  /**
   * Select the best value for k by hold-one-out cross-validation.
   * If the class attribute is nominal, classification error is
   * minimised. If the class attribute is numeric, mean absolute
   * error is minimised
   */
  protected void crossValidate() {

    try {
      double [] performanceStats = new double [m_kNNUpper];
      double [] performanceStatsSq = new double [m_kNNUpper];

      for(int i = 0; i < m_kNNUpper; i++) {
	performanceStats[i] = 0;
	performanceStatsSq[i] = 0;
      }


      m_kNN = m_kNNUpper;
      Instance instance;
      NeighborList neighborlist;
      for(int i = 0; i < m_Train.numInstances(); i++) {
	if (m_Debug && (i % 50 == 0)) {
	  System.err.print("Cross validating "
			   + i + "/" + m_Train.numInstances() + "\r");
	}
	instance = m_Train.instance(i);
	neighborlist = findNeighbors(instance);

	for(int j = m_kNNUpper - 1; j >= 0; j--) {
	  // Update the performance stats
	  double [] distribution = makeDistribution(neighborlist);
	  double thisPrediction = Utils.maxIndex(distribution);
	  if (m_Train.classAttribute().isNumeric()) {
	    thisPrediction = distribution[0];
	    double err = thisPrediction - instance.classValue();
	    performanceStatsSq[j] += err * err;   // Squared error
	    performanceStats[j] += Math.abs(err); // Absolute error
	  } else {
	    if (thisPrediction != instance.classValue()) {
	      performanceStats[j] ++;             // Classification error
	    }
	  }
	  if (j >= 1) {
	    neighborlist.pruneToK(j);
	  }
	}
      }

      // Display the results of the cross-validation
      for(int i = 0; i < m_kNNUpper; i++) {
	if (m_Debug) {
	  System.err.print("Hold-one-out performance of " + (i + 1)
			   + " neighbors " );
	}
	if (m_Train.classAttribute().isNumeric()) {
	  if (m_Debug) {
	    if (m_MeanSquared) {
	      System.err.println("(RMSE) = "
				 + Math.sqrt(performanceStatsSq[i]
					     / m_Train.numInstances()));
	    } else {
	      System.err.println("(MAE) = "
				 + performanceStats[i]
				 / m_Train.numInstances());
	    }
	  }
	} else {
	  if (m_Debug) {
	    System.err.println("(%ERR) = "
			       + 100.0 * performanceStats[i]
			       / m_Train.numInstances());
	  }
	}
      }


      // Check through the performance stats and select the best
      // k value (or the lowest k if more than one best)
      double [] searchStats = performanceStats;
      if (m_Train.classAttribute().isNumeric() && m_MeanSquared) {
	searchStats = performanceStatsSq;
      }
      double bestPerformance = Double.NaN;
      int bestK = 1;
      for(int i = 0; i < m_kNNUpper; i++) {
	if (Double.isNaN(bestPerformance)
	    || (bestPerformance > searchStats[i])) {
	  bestPerformance = searchStats[i];
	  bestK = i + 1;
	}
      }
      m_kNN = bestK;
      if (m_Debug) {
	System.err.println("Selected k = " + bestK);
      }
      
      m_kNNValid = true;
    } catch (Exception ex) {
      throw new Error("Couldn't optimize by cross-validation: "
		      +ex.getMessage());
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line options (see setOptions)
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new IBk(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}





