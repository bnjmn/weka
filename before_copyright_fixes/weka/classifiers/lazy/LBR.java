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
 *    aint with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


/*
 *    LBR.java
 *    The naive Bayesian classifier provides a simple and effective approach to 
 *    classifier learning, but its attribute independence assumption is often 
 *    violated in the real world. Lazy Bayesian Rules selectively relaxes the 
 *    independence assumption, achieving lower error rates over a range of 
 *    learning tasks.  LBR defers processing to classification time, making it 
 *    a highly efficient and accurate classification algorithm when small
 *    numbers of objects are to be classified.
 *
 *    For more information, see
 <!-- technical-plaintext-start -->
 * Zijian Zheng, G. Webb (2000). Lazy Learning of Bayesian Rules. Machine Learning. 4(1):53-84.
 <!-- technical-plaintext-end -->
 *
 *    http://www.cm.deakin.edu.au/webb
 *
 *    Copyright (C) 2001 Deakin University
 *    School of Computing and Mathematics
 *    Deakin University
 *    Geelong, Vic, 3217, Australia
 *
 *    Email: zhw@deakin.edu.au
 *
 */

package weka.classifiers.lazy;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Statistics;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.io.Serializable;
import java.util.ArrayList;

/**
 <!-- globalinfo-start -->
 * Lazy Bayesian Rules Classifier. The naive Bayesian classifier provides a simple and effective approach to classifier learning, but its attribute independence assumption is often violated in the real world. Lazy Bayesian Rules selectively relaxes the independence assumption, achieving lower error rates over a range of learning tasks. LBR defers processing to classification time, making it a highly efficient and accurate classification algorithm when small numbers of objects are to be classified.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Zijian Zheng, G. Webb (2000). Lazy Learning of Bayesian Rules. Machine Learning. 4(1):53-84.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Zheng2000,
 *    author = {Zijian Zheng and G. Webb},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {53-84},
 *    title = {Lazy Learning of Bayesian Rules},
 *    volume = {4},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Zhihai Wang (zhw@deakin.edu.au) : July 2001 implemented the algorithm
 * @author Jason Wells (wells@deakin.edu.au) : November 2001 added instance referencing via indexes
 * @version $Revision: 1.11 $
 */
public class LBR 
  extends Classifier
  implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5648559277738985156L;
  
  /**
   * Class for handling instances and the associated attributes. <p>
   * Enables a set of indexes to a given dataset to be created and used
   * with an algorithm.  This reduces the memory overheads and time required 
   * when manipulating and referencing Instances and their Attributes.  
   */
  public class Indexes
    implements Serializable {

    /** for serialization */
    private static final long serialVersionUID = -2771490019751421307L;
    
    /** the array instance indexes **/
    public boolean [] m_InstIndexes;
    
    /** the array attribute indexes **/
    public boolean [] m_AttIndexes;
    
    /** the number of instances indexed **/
    private int m_NumInstances;
    
    /** the number of attributes indexed **/
    private int m_NumAtts;
    
    /** the array of instance indexes that are set to a either true or false **/
    public int [] m_SequentialInstIndexes;
    
    /** an array of attribute indexes that are set to either true or false **/
    public int [] m_SequentialAttIndexes;
    
    /** flag to check if sequential array must be rebuilt due to changes to the instance index*/
    private boolean m_SequentialInstanceIndex_valid = false;
    
   /** flag to check if sequential array must be rebuilt due to changes to the attribute index */
    private boolean m_SequentialAttIndex_valid = false;
    
    /** the number of instances "in use"  or set to a the original value (true or false) **/
    public int m_NumInstsSet;
    
    /** the number of attributes "in use"  or set to a the original value (true or false) **/
    public int m_NumAttsSet;
    
    /** the number of sequential instances "in use"  or set to a the original value (true or false) **/
    public int m_NumSeqInstsSet;
    
    /** the number of sequential attributes "in use"  or set to a the original value (true or false) **/
    public int m_NumSeqAttsSet;
    
    /** the Class Index for the data set **/
    public int m_ClassIndex;
    
    /**
     * constructor
     * @param numInstances the number of instances in dataset
     * @param numAtts the number of attributes in dataset
     * @param value either true or false
     * @param classIndex  Set to -1 if you want class attribute switched on or the value of the instances 
     * class index will be switched of and the class attibute will not be considered.
     */
    public Indexes(int numInstances, int numAtts, boolean value, int classIndex) {
      /* to create an empty DATASET with all attributes indexed use FALSE
       * to create a index of all instances and attributes use TRUE
       */
      // initialise counts
      m_NumInstsSet =  m_NumInstances = numInstances;
      m_NumAttsSet = m_NumAtts = numAtts;
      
      m_InstIndexes = new boolean [(int)numInstances];
      
      /* set all indexes to value */
      int i = 0;
      while(i < numInstances) {
	m_InstIndexes[i] = value;
	i++;
      }
      
      m_AttIndexes = new boolean [(int)numAtts];
      
      /* set all indexes to true */
      i = 0;
      while(i < numAtts) {
	m_AttIndexes[i] = true;
	i++;
      }
      // if the value is false the dataset has no instances therefore no instances are set
      if(value == false)
        m_NumInstsSet = 0;
      // no sequential array has been created
      m_SequentialInstanceIndex_valid = false;
      m_SequentialAttIndex_valid = false;
      
      // switch class attr to false as the class is not used in the dataset.  Set to -1 if you want the class attr included
      if(classIndex != -1)
        setAttIndex(classIndex, false);
      m_ClassIndex = classIndex;
    }
    
    /**
     * constructor
     * @param FromIndexes the object you want to copy
     */
    public Indexes(Indexes FromIndexes) {
      // set counts to the FromIndexes counts
      m_NumInstances = FromIndexes.getNumInstances();
      m_NumInstsSet = FromIndexes.m_NumInstsSet;
      m_NumAtts = FromIndexes.m_NumAtts;
      m_NumAttsSet = FromIndexes.m_NumAttsSet;
      m_InstIndexes = new boolean [m_NumInstances];
      
      System.arraycopy(FromIndexes.m_InstIndexes, 0, m_InstIndexes, 0, m_NumInstances);
      
      m_AttIndexes = new boolean [(int)m_NumAtts];
      
      System.arraycopy(FromIndexes.m_AttIndexes, 0, m_AttIndexes, 0, m_NumAtts);
      m_ClassIndex = FromIndexes.m_ClassIndex;
      m_SequentialInstanceIndex_valid = false;
      m_SequentialAttIndex_valid = false;
    }
    
    /**
     * 
     * Changes the boolean value at the specified index in the InstIndexes array
     *
     * @param index the index of the instance
     * @param value the value to set at the specified index
     *
     */
    public void setInstanceIndex(int index, boolean value) {
      if(index < 0 || index >= m_NumInstances)
	throw new IllegalArgumentException("Invalid Instance Index value");
      // checks that the index isn't alreading set to value
      if(m_InstIndexes[(int)index] != value) {
	
	// set the value
	m_InstIndexes[(int)index] = value;
	
	// a change has been made, so sequential array is invalid
	m_SequentialInstanceIndex_valid = false;
	
	// change the number of values "in use" to appropriate value
	if(value == false)
	  m_NumInstsSet--;
	else
	  m_NumInstsSet++;
      }
   }
    
    /**
     * 
     * Changes the boolean value at the specified index in the InstIndexes array
     *
     * @param Attributes array of attributes
     * @param value the value to set at the specified index
     *
     */
    public void setAtts(int [] Attributes, boolean value) {
      for(int i = 0; i < m_NumAtts; i++) {
        m_AttIndexes[i] = !value;
      }
      for (int i = 0; i < Attributes.length; i++)  {
        m_AttIndexes[Attributes[i]] = value;
      }
      m_NumAttsSet = Attributes.length;
      m_SequentialAttIndex_valid = false;
    }
    
    /**
     * 
     * Changes the boolean value at the specified index in the InstIndexes array
     *
     * @param Instances array of instances
     * @param value the value to set at the specified index
     *
     */
    public void setInsts(int [] Instances, boolean value) {
      resetInstanceIndex(!value);
      for (int i = 0; i < Instances.length; i++)  {
        m_InstIndexes[Instances[i]] = value;
      }
      m_NumInstsSet = Instances.length;
      m_SequentialInstanceIndex_valid = false;
    }
    
    
    /**
     * 
     * Changes the boolean value at the specified index in the AttIndexes array
     *
     * @param index the index of the instance
     * @param value the value to set at the specified index
     *
     */
    public void setAttIndex(int index, boolean value) {
      if(index < 0 || index >= m_NumAtts)
	throw new IllegalArgumentException("Invalid Attribute Index value");
      // checks that the index isn't alreading set to value
      if(m_AttIndexes[(int)index] != value) {
	
	// set the value
	m_AttIndexes[(int)index] = value;
	
	// a change has been made, so sparse array is invalid
	m_SequentialAttIndex_valid = false;  
	
	 // change the number of values "in use" to appropriate value
	if(value == false)
	  m_NumAttsSet--;
	else
	  m_NumAttsSet++;
      }
    }
    
    /**
     * 
     * Returns the boolean value at the specified index in the Instance Index array
     *
     * @param index the index of the instance
     * @return the boolean value at the specified index
     */
    public boolean getInstanceIndex(int index) {
      
      if(index < 0 || index >= m_NumInstances)
	throw new IllegalArgumentException("Invalid index value");
      
      return m_InstIndexes[(int)index]; 
    }
    
    /**
     * 
     * Returns the boolean value at the specified index in the Sequential Instance Indexes array
     *
     * @param index the index of the instance
     * @return the requested value
     */
    public int getSequentialInstanceIndex(int index) {
      
      if(index < 0 || index >= m_NumInstances)
	throw new IllegalArgumentException("Invalid index value");
      
      return m_SequentialInstIndexes[(int)index]; 
    }
    
    /**
     * 
     * Resets the boolean value in the Instance Indexes array to a specified value
     *
     * @param value the value to set all indexes
     * 
    */
    public void resetInstanceIndex(boolean value) {
      m_NumInstsSet = m_NumInstances;
      for(int i = 0; i < m_NumInstances; i++) {
	m_InstIndexes[i] = value;
      }
      if(value == false)
	m_NumInstsSet =  0;
      m_SequentialInstanceIndex_valid = false;
    }
    
   /**
    * 
    * Resets the boolean values in Attribute and Instance array to reflect an empty dataset withthe same attributes set as in the incoming Indexes Object
    *
    * @param FromIndexes the Indexes to be copied
    * 
    */
    public void resetDatasetBasedOn(Indexes FromIndexes) {
      resetInstanceIndex(false);
      resetAttIndexTo(FromIndexes);
    }
   
    /**
     * 
     * Resets the boolean value in AttIndexes array
     *
     * @param value the value to set the attributes to
     * 
     */
    public void resetAttIndex(boolean value) {
      m_NumAttsSet =  m_NumAtts;
      for(int i = 0; i < m_NumAtts; i++) {
	m_AttIndexes[i] = value;
      }
      if(m_ClassIndex != -1)
	setAttIndex(m_ClassIndex, false);
      if(value == false)
	m_NumAttsSet =  0;
     m_SequentialAttIndex_valid = false;
    }
    
    /**
     * 
     * Resets the boolean value in AttIndexes array based on another set of Indexes
     *
     * @param FromIndexes the Indexes to be copied
     * 
    */
    public void resetAttIndexTo(Indexes FromIndexes) {
      System.arraycopy(FromIndexes.m_AttIndexes, 0, m_AttIndexes, 0, m_NumAtts);
      m_NumAttsSet =  FromIndexes.getNumAttributesSet();
      m_ClassIndex = FromIndexes.m_ClassIndex;
      m_SequentialAttIndex_valid = false;
    }
    
    /**
     * 
     * Returns the boolean value at the specified index in the Attribute Indexes array
     *
     * @param index the index of the Instance
     * @return the boolean value
     */
    public boolean getAttIndex(int index) {
      
      if(index < 0 || index >= m_NumAtts)
         throw new IllegalArgumentException("Invalid index value");
      
      return m_AttIndexes[(int)index];
    }
    
    /**
     * 
     * Returns the boolean value at the specified index in the Sequential Attribute Indexes array
     *
     * @param index the index of the Attribute
     * @return the requested value
     */
    public int getSequentialAttIndex(int index) {
      
      if(index < 0 || index >= m_NumAtts)
	throw new IllegalArgumentException("Invalid index value");
      
      return m_SequentialAttIndexes[(int)index];
    }
    
    /**
     * 
     * Returns the number of instances "in use"
     * 
     * @return the number of instances "in use"
     */
    public int getNumInstancesSet() {
      
      return m_NumInstsSet;
   }

    /**
     * 
     * Returns the number of instances in the dataset
     * 
     * @return the number of instances in the dataset
     */
    public int getNumInstances() {
      
      return m_NumInstances;
    }

    /**
     * 
     * Returns the number of instances in the Sequential array
     * 
     * @return the number of instances in the sequential array
     */
    public int getSequentialNumInstances() {
      // will always be the number set as the sequential array is for referencing only
      return m_NumSeqInstsSet;
    }
    
    /**
     * 
     * Returns the number of attributes in the dataset
     * 
     * @return the number of attributes
     */
    public int getNumAttributes() {
      
      return m_NumAtts;
    }
   
    /**
     * 
     * Returns the number of attributes "in use"
     * 
     * @return the number of attributes "in use"
     */
    public int getNumAttributesSet() {
      
      return m_NumAttsSet;
    }
    
    /**
     * 
     * Returns the number of attributes in the Sequential array
     * 
     * @return the number of attributes in the sequentual array
     */
    public int getSequentialNumAttributes() {
      // will always be the number set as the sequential array is for referencing only
      return m_NumSeqAttsSet;
    }
    
    /**
     * 
     * Returns whether or not the Sequential Instance Index requires rebuilding due to a change 
     * 
     * @return true if the sequential instance index needs rebuilding
     */
    public boolean isSequentialInstanceIndexValid() {
      
      return m_SequentialInstanceIndex_valid;
    }
    
    /**
     * 
     * Returns whether or not the Sequential Attribute Index requires rebuilding due to a change 
     * 
     * @return true if the sequential attribute index needs rebuilding
     */
    public boolean isSequentialAttIndexValid() {
      
      return m_SequentialAttIndex_valid;
    }
    
    /**
     * 
     * Sets both the Instance and Attribute indexes to a specified value
     * 
     * @param value the value for the Instance and Attribute indices
     */
    public void setSequentialDataset(boolean value) {
      setSequentialInstanceIndex(value);
      setSequentialAttIndex(value);
    }
    
    /**
     * 
     * A Sequential Instance index is all those Instances that are set to the specified value placed in a sequential array.
     * Each value in the sequential array contains the Instance index within the Indexes.
     *
     * @param value the sequential instance index
     */
    public void setSequentialInstanceIndex(boolean value) {
      
      if(m_SequentialInstanceIndex_valid == true)
	return;
      
      /* needs to be recalculated */
      int size;
      size = m_NumInstsSet;
      
      m_SequentialInstIndexes = new int [(int)size];
      
      int j = 0;
      for(int i = 0; i < m_NumInstances; i++) {
	if(m_InstIndexes[i] == value) {
	  m_SequentialInstIndexes[j] = i;
	  j++;
	}
      }
      
      m_SequentialInstanceIndex_valid = true;
      m_NumSeqInstsSet = j;
    }
    
    /**
     * 
     * A Sequential Attribute index is all those Attributes that are set to the specified value placed in a sequential array.
     * Each value in the sequential array contains the Attribute index within the Indexes
     * 
     * @param value the sequential attribute index
     */
    public void setSequentialAttIndex(boolean value) {
      
      if(m_SequentialAttIndex_valid == true)
	return;
      
      /* needs to be recalculated */
      int size;
      size = m_NumAttsSet;
      
      m_SequentialAttIndexes = new int [(int)size];
      
      int j = 0;
      for(int i = 0; i < m_NumAtts; i++) {
	if(m_AttIndexes[i] == value) {
	  m_SequentialAttIndexes[j] = i;
	  j++;
	 }
      }
      
      m_SequentialAttIndex_valid = true;
      m_NumSeqAttsSet = j;
    }
  } /* end of Indexes inner-class */
  

  /** All the counts for nominal attributes. */
  protected int [][][] m_Counts;
  /** All the counts for nominal attributes. */
  protected int [][][] m_tCounts;
  /** The prior probabilities of the classes. */
  protected int [] m_Priors;
  /** The prior probabilities of the classes. */
  protected int [] m_tPriors;
  
  /** number of attributes for the dataset ***/
  protected int m_numAtts;
  
  /** number of classes for dataset ***/
  protected int m_numClasses;
  
 /** number of instances in dataset ***/
  protected int m_numInsts;
  
  /** The set of instances used for current training. */
  protected Instances m_Instances = null;
  
  /** leave-one-out errors on the training dataset. */
  protected int m_Errors;
  
  /** leave-one-out error flags on the training dataaet. */
  protected boolean [] m_ErrorFlags;
  
  /** best attribute's index list. maybe as output result */
  protected ArrayList leftHand = new ArrayList();
  
  /** significantly lower */
  protected static final double SIGNLOWER = 0.05;
  
  /** following is defined by wangzh, 
   * the number of instances to be classified incorrectly
   * on the subset. */
  protected boolean [] m_subOldErrorFlags;
  
  /** the number of instances to be classified incorrectly
   * besides the subset. */
  protected int m_RemainderErrors = 0;
  
  /** the number of instance to be processed */
  protected int m_Number = 0;
  
  /** the Number of Instances to be used in building a classifiers */
  protected int m_NumberOfInstances = 0;
  
  /** for printing in n-fold cross validation */
  protected boolean m_NCV = false;
  
  /** index of instances and attributes for the given dataset */
  protected Indexes m_subInstances;
  
  /** index of instances and attributes for the given dataset */
  protected Indexes tempSubInstances;
  
  /** probability values array */
  protected double [] posteriorsArray;
  protected int bestCnt;
  protected int tempCnt;
  protected int forCnt;
  protected int whileCnt;

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return 
        "Lazy Bayesian Rules Classifier. The naive Bayesian classifier "
      + "provides a simple and effective approach to classifier learning, "
      + "but its attribute independence assumption is often violated in the "
      + "real world. Lazy Bayesian Rules selectively relaxes the independence "
      + "assumption, achieving lower error rates over a range of learning "
      + "tasks. LBR defers processing to classification time, making it a "
      + "highly efficient and accurate classification algorithm when small "
      + "numbers of objects are to be classified.\n\n"
      + "For more information, see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Zijian Zheng and G. Webb");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.TITLE, "Lazy Learning of Bayesian Rules");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "4");
    result.setValue(Field.NUMBER, "1");
    result.setValue(Field.PAGES, "53-84");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * For lazy learning, building classifier is only to prepare their inputs
   * until classification time.
   *
   * @param instances set of instances serving as training data
   * @throws Exception if the preparation has not been generated.
   */
  public void buildClassifier(Instances instances) throws Exception {
    int attIndex, i, j;
    bestCnt = 0;
    tempCnt = 0;
    forCnt = 0;
    whileCnt = 0;
    
    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    m_numAtts = instances.numAttributes();
    m_numClasses = instances.numClasses();
    m_numInsts = instances.numInstances();

    // Reserve space
    m_Counts = new int[m_numClasses][m_numAtts][0];
    m_Priors = new int[m_numClasses];
    m_tCounts = new int[m_numClasses][m_numAtts][0];
    m_tPriors = new int[m_numClasses];
    m_subOldErrorFlags = new boolean[m_numInsts+1];
    
    m_Instances = instances;
    
    m_subInstances = new Indexes(m_numInsts, m_numAtts, true, m_Instances.classIndex());
    tempSubInstances = new Indexes(m_numInsts, m_numAtts, true, m_Instances.classIndex());
    
    
    posteriorsArray = new double[m_numClasses];
    
    // prepare arrays
    for (attIndex = 0; attIndex < m_numAtts; attIndex++) {
      Attribute attribute = (Attribute) instances.attribute(attIndex);
      for (j = 0; j < m_numClasses; j++) {
        m_Counts[j][attIndex] = new int[attribute.numValues()];
        m_tCounts[j][attIndex] = new int[attribute.numValues()];
      }
    }

    // Compute counts and priors
    for(i = 0; i < m_numInsts; i++) {
      Instance instance = (Instance) instances.instance(i);
      int classValue = (int)instance.classValue();
      // pointer for more efficient access to counts matrix in loop
      int [][] countsPointer = m_tCounts[classValue];
      for(attIndex = 0; attIndex < m_numAtts; attIndex++) {
        countsPointer[attIndex][(int)instance.value(attIndex)]++;
      }
      m_tPriors[classValue]++;
    }
    
    // Step 2: Leave-one-out on the training data set.
    // get m_Errors and its flags array using leave-one-out.
    m_ErrorFlags = new boolean[m_numInsts];
    
    m_Errors = leaveOneOut(m_subInstances, m_tCounts, m_tPriors, m_ErrorFlags);

    if (m_Number == 0) {
      m_NumberOfInstances = m_Instances.numInstances();
    } else {
      System.out.println(" ");
      System.out.println("N-Fold Cross Validation: ");
      m_NCV = true;
    }
  }
  
  /**
   * Calculates the class membership probabilities
   * for the given test instance.
   * This is the most important method for Lazy Bayesian Rule algorithm.
   *
   * @param testInstance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed
   */
  public double[] distributionForInstance(Instance testInstance)
  throws Exception {
    
    int inst;
    int subAttrIndex = 0;
    int subInstIndex = 0;
    int tempInstIndex = 0;
    int attributeBest;
    int subLocalErrors = 0;
    int tempErrorsBest = 0;
    boolean [] tempErrorFlagBest = null;
    int [] tempD_subsetBestInsts = null;
    int [] tempD_subsetBestAtts = null;
    Indexes subInstances = new Indexes(m_numInsts, m_numAtts, true, m_Instances.classIndex());
    
    boolean [] subLocalErrorFlags = new  boolean [(int)subInstances.getNumInstances()+1];
    // Step 2': Get localErrors, localErrorFlags, and training data set.
    int localErrors = m_Errors;
    boolean [] localErrorFlags = (boolean []) m_ErrorFlags.clone();
    
    // The number of errors on New, Not on Old in the subset.
    int errorsNewNotOld = 0;
    // The number of errors on Old, Not on New in the subset.
    int errorsOldNotNew = 0;
    
    // Step 3:
    leftHand.clear();

    // Step 4: Beginning Repeat.
    // Selecting all the attributes that can be moved to the lefthand.
    while (localErrors >= 5) {
      attributeBest = -1;
      whileCnt++;
      // Step 5:
      tempErrorsBest = subInstances.getNumInstancesSet() + 1;
      subInstances.setSequentialDataset(true);
      // Step 6: selecting an attribute.
      for (int attr = 0; attr < subInstances.m_NumSeqAttsSet; attr++){
        forCnt++;
        subAttrIndex = subInstances.m_SequentialAttIndexes[attr];
        // Step 7: get the corresponding subset.
        
        m_RemainderErrors = 0;

        // reset array to true
        for(int i = 0; i < m_numInsts; i++) {
          m_subOldErrorFlags[i] = true;
        }
        // reset indexes to reflect an empty dataset but with the same attrs as another dataset
        tempSubInstances.resetDatasetBasedOn(subInstances);
        // Get subset of the instances and its m_LastSecondErrors
        for(inst = 0; inst < subInstances.m_NumSeqInstsSet; inst++) {
          subInstIndex = subInstances.m_SequentialInstIndexes[inst];
          if (m_Instances.instance(subInstIndex).value(subAttrIndex) == testInstance.value(subAttrIndex))  {
            // add instance to subset list
            tempSubInstances.setInstanceIndex(subInstIndex, true);
            if (localErrorFlags[subInstIndex] == false ) {
              m_subOldErrorFlags[subInstIndex] = false;
            }
          }
          else  {
            if (localErrorFlags[subInstIndex] == false ) {
              m_RemainderErrors++;
            }
          }
        } // end of for

        // Step 7':
        if (tempSubInstances.m_NumInstsSet < subInstances.m_NumInstsSet) {
          // remove attribute from index
          tempSubInstances.setAttIndex(subAttrIndex, false);
          // Step 9: create a classifier on the subset.
          // Compute counts and priors
          // create sequential index of instances and attributes that are to be considered
                
          localNaiveBayes(tempSubInstances);
          
          subLocalErrors = leaveOneOut(tempSubInstances, m_Counts, m_Priors, subLocalErrorFlags);

          errorsNewNotOld = 0;
          errorsOldNotNew = 0;
          
          tempSubInstances.setSequentialDataset(true);
          
          for(int t_inst = 0; t_inst < tempSubInstances.m_NumSeqInstsSet; t_inst++) {
            tempInstIndex = tempSubInstances.m_SequentialInstIndexes[t_inst];
            if (subLocalErrorFlags[tempInstIndex] == false) {
              // The number of errors on New, Not on Old in the subset.
              if (m_subOldErrorFlags[tempInstIndex] == true) {
                errorsNewNotOld ++;
              }
            } else {
              // The number of errors on Old, Not on New in the subset.
              if(m_subOldErrorFlags[tempInstIndex] == false) {
                errorsOldNotNew ++;
              }
            }
          } //end of for
          
          // Step 10 and Step 11:
          int tempErrors = subLocalErrors + m_RemainderErrors;
          // Step 12:
          // Step 13: stopping criteria.
          if((tempErrors < tempErrorsBest) && (binomP(errorsNewNotOld, errorsNewNotOld + errorsOldNotNew, 0.5 ) < SIGNLOWER))      {
            // Step 14:
            tempCnt++;
            // --------------------------------------------------
            //tempD_subsetBest = new Indexes(tempSubInstances);
            
            // -------------------------------------------------------------------------------
            tempSubInstances.setSequentialDataset(true);
            tempD_subsetBestInsts = (int []) tempSubInstances.m_SequentialInstIndexes.clone();
            tempD_subsetBestAtts = (int []) tempSubInstances.m_SequentialAttIndexes.clone();
            // -------------------------------------------------------------------------------
            // Step 15:
            tempErrorsBest = tempErrors;

            tempErrorFlagBest = (boolean []) subLocalErrorFlags.clone();

            // Step 16:
            attributeBest = subAttrIndex;
          } // end of if
        } // end of if
      } // end of main for
      
      // Step 20:
      if(attributeBest != -1)  {
        bestCnt++;
        // Step 21:
        leftHand.add(testInstance.attribute(attributeBest));
        // ------------------------------------------------
        // Step 22:
        //tempD_subsetBest.setAttIndex(attributeBest, false);
        //subInstances = tempD_subsetBest;
        // ------------------------------------------------ 
        subInstances.setInsts(tempD_subsetBestInsts, true);
        subInstances.setAtts(tempD_subsetBestAtts, true);
        subInstances.setAttIndex(attributeBest, false);
        // -------------------------------------------------
        // Step 25:
        localErrors = tempErrorsBest;
        localErrorFlags =  tempErrorFlagBest;
        
      } else {
        break;
      }
    } // end of while
    
    // Step 27:
    localNaiveBayes(subInstances);
    return localDistributionForInstance(testInstance, subInstances);
  }
  
  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
    
    if (m_Instances == null) {
      return "Lazy Bayesian Rule: No model built yet.";
    }
    
    try {
      StringBuffer text = new StringBuffer
      ("=== LBR Run information ===\n\n");
      
      text.append("Scheme:       weka.classifiers.LBR\n");
      
      text.append("Relation:     "
      + m_Instances.attribute(m_Instances.classIndex()).name()
      + "\n");
      
      text.append("Instances:    "+m_Instances.numInstances()+"\n");
      
      text.append("Attributes:   "+m_Instances.numAttributes()+"\n");
      
      // Remains are printed by Evaulation.java
      return text.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return "Can't Print Lazy Bayes Rule Classifier!";
    }
  }

  /**
   * Leave-one-out strategy. For a given sample data set with n instances,
   * using (n - 1) instances by leaving one out and tested on the single
   * remaining case.
   * This is repeated n times in turn.
   * The final "Error" is the sum of the instances to be classified
   * incorrectly.
   *
   * @param instanceIndex set of instances serving as training data.
   * @param counts serving as all the counts of training data.
   * @param priors serving as the number of instances in each class.
   * @param errorFlags for the errors
   *
   * @return error flag array about each instance.
   * @throws Exception if something goes wrong
   **/
  public int leaveOneOut(Indexes instanceIndex, int [][][] counts, int [] priors, boolean [] errorFlags)  throws Exception {
    
    
    // ###### START LEAVE ONE OUT #############
    int tempClassValue;
    double posteriors;
    double sumForPriors;
    double sumForCounts;
    double max = 0;
    int maxIndex = 0;
    int AIndex, attIndex, clss;
    int inst;
    int errors = 0;
    int instIndex;
    
    instanceIndex.setSequentialDataset(true);
    int tempInstanceClassValue;
    int [] tempAttributeValues = new int[(int)instanceIndex.m_NumSeqAttsSet+1];
    Instance tempInstance;
    for(inst = 0; inst < instanceIndex.m_NumSeqInstsSet; inst++) {
      instIndex = instanceIndex.m_SequentialInstIndexes[inst];
      //get the leave-one-out instance
      tempInstance = (Instance) m_Instances.instance(instIndex);
      if (!tempInstance.classIsMissing()) {
      tempInstanceClassValue = (int)tempInstance.classValue();
      // pointer to first index of counts matrix for efficiency
      int [][] countsPointer = counts[tempInstanceClassValue];
      // Compute the counts and priors for (n-1) instances.
      for(attIndex = 0; attIndex < instanceIndex.m_NumSeqAttsSet; attIndex++) {
        AIndex = instanceIndex.m_SequentialAttIndexes[attIndex];
        tempAttributeValues[attIndex] = (int)tempInstance.value(AIndex);
        countsPointer[AIndex][tempAttributeValues[attIndex]]--;
      }
      
      priors[tempInstanceClassValue]--;
      max = 0;
      maxIndex= 0;
      // ###### LOCAL CLASSIFY INSTANCE ###########
      sumForPriors = Utils.sum(priors);
      for (clss = 0; clss < m_numClasses; clss++) {
        posteriors = 0.0;
        posteriors = (priors[clss] + 1) / (sumForPriors + m_numClasses);
        
	countsPointer = counts[clss];
        for(attIndex = 0; attIndex < instanceIndex.m_NumSeqAttsSet; attIndex++) {
          AIndex = instanceIndex.m_SequentialAttIndexes[attIndex];
          if (!tempInstance.isMissing(AIndex)) {
            sumForCounts = Utils.sum(countsPointer[AIndex]);
            posteriors *= ((countsPointer[AIndex][tempAttributeValues[attIndex]] + 1) / (sumForCounts + (double)tempInstance.attribute(AIndex).numValues()));
          }
        }
        
        if (posteriors > max) {
          maxIndex = clss;
          max = posteriors;
        }
      } // end of for
      
      if (max > 0) {
        tempClassValue = maxIndex;
      } else {
        tempClassValue = (int)Instance.missingValue();
      }
      // ###### END LOCAL CLASSIFY INSTANCE ###########
      
      // Adjudge error. Here using classIndex is incorrect,
      // it is index of the class attribute.
      if(tempClassValue == tempInstanceClassValue){
        errorFlags[instIndex] = true;
      } else {
        errorFlags[instIndex] = false;
        errors++;
      }
      
      countsPointer = counts[tempInstanceClassValue];
      for(attIndex = 0; attIndex < instanceIndex.m_NumSeqAttsSet; attIndex++) {
        AIndex = instanceIndex.m_SequentialAttIndexes[attIndex];
        counts[tempInstanceClassValue][AIndex][tempAttributeValues[attIndex]]++;
      }
      
      priors[tempInstanceClassValue]++;
      }
    } // end of for
    // ###### END LEAVE ONE OUT #############
    return errors;
  }
 
  /**
   * Class for building and using a simple Naive Bayes classifier.
   * For more information, see<p>
   *
   * Richard Duda and Peter Hart (1973).<i>Pattern
   * Classification and Scene Analysis</i>. Wiley, New York.
   *
   * This method only get m_Counts and m_Priors.
   *
   * @param instanceIndex set of instances serving as training data
   * @throws Exception if m_Counts and m_Priors have not been
   *  generated successfully
   */
  public void localNaiveBayes(Indexes instanceIndex) throws Exception {
    int attIndex = 0;
    int i, AIndex;
    int attVal = 0;
    int classVal = 0;
    Instance instance;

    instanceIndex.setSequentialDataset(true);

    // reset local counts
    for(classVal = 0; classVal < m_numClasses; classVal++) {
      // counts pointer mcTimesaver
      int [][] countsPointer1 = m_Counts[classVal];
      for(attIndex = 0; attIndex < m_numAtts; attIndex++) {
        Attribute attribute = m_Instances.attribute(attIndex);
         // love those pointers for saving time
         int [] countsPointer2 = countsPointer1[attIndex];
        for(attVal = 0; attVal < attribute.numValues(); attVal++)  {
          countsPointer2[attVal] = 0;
        }
     }
     m_Priors[classVal] = 0;
   }

    for(i = 0; i < instanceIndex.m_NumSeqInstsSet; i++) {
      instance = (Instance) m_Instances.instance(instanceIndex.m_SequentialInstIndexes[i]);
      for(attIndex = 0; attIndex < instanceIndex.m_NumSeqAttsSet; attIndex++) {
        AIndex = instanceIndex.m_SequentialAttIndexes[attIndex];
        m_Counts[(int)instance.classValue()][AIndex][(int)instance.value(AIndex)]++;
      }
      m_Priors[(int)instance.classValue()]++;
    }
  }
    
  /**
   * Calculates the class membership probabilities.
   * for the given test instance.
   *
   * @param instance the instance to be classified
   * @param instanceIndex 
   *
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed
   */
  public double[] localDistributionForInstance(Instance instance, Indexes instanceIndex) throws Exception {
    
    double sumForPriors = 0;
    double sumForCounts = 0;
    int attIndex, AIndex;
    int numClassesOfInstance = instance.numClasses();
    
    sumForPriors = 0;
    sumForCounts = 0;
    instanceIndex.setSequentialDataset(true);
    // Calculate all of conditional probabilities.
    sumForPriors = Utils.sum(m_Priors) + numClassesOfInstance;
    for (int j = 0; j < numClassesOfInstance; j++) {
      // pointer to counts to make access more efficient in loop
      int [][] countsPointer = m_Counts[j];
      posteriorsArray[j] = (m_Priors[j] + 1) / (sumForPriors);
      for(attIndex = 0; attIndex < instanceIndex.m_NumSeqAttsSet; attIndex++) {
        AIndex = instanceIndex.m_SequentialAttIndexes[attIndex];
        sumForCounts = Utils.sum(countsPointer[AIndex]);
        if (!instance.isMissing(AIndex)) {
          posteriorsArray[j] *= ((countsPointer[AIndex][(int)instance.value(AIndex)] + 1) / (sumForCounts + (double)instance.attribute(AIndex).numValues()));
        }
      }
    }
    
    // Normalize probabilities
    Utils.normalize(posteriorsArray);
    
    return posteriorsArray;
  }
  
  /**
   * Significance test
   * binomp:
   *
   * @param r
   * @param n
   * @param p
   * @return returns the probability of obtaining r or fewer out of n
   * if the probability of an event is p.
   * @throws Exception if computation fails
   */
  public double binomP(double r, double n, double p) throws Exception {
    
    if (n == r) return 1.0;
    return Statistics.incompleteBeta(n-r, r+1.0, 1.0-p);
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new LBR(), argv);
  }
}
