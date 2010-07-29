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
 *    AODEsr.java
 *    Copyright (C) 2007
 *    Algorithm developed by: Fei ZHENG and Geoff Webb
 *    Code written by: Fei ZHENG and Janice Boughton
 */

package weka.classifiers.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 *
 <!-- globalinfo-start -->
 * AODEsr augments AODE with Subsumption Resolution.AODEsr detects specializations between two attribute values at classification time and deletes the generalization attribute value.<br/>
 * For more information, see:<br/>
 * Fei Zheng, Geoffrey I. Webb: Efficient Lazy Elimination for Averaged-One Dependence Estimators. In: Proceedings of the Twenty-third International Conference on Machine  Learning (ICML 2006), 1113-1120, 2006.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Zheng2006,
 *    author = {Fei Zheng and Geoffrey I. Webb},
 *    booktitle = {Proceedings of the Twenty-third International Conference on Machine  Learning (ICML 2006)},
 *    pages = {1113-1120},
 *    publisher = {ACM Press},
 *    title = {Efficient Lazy Elimination for Averaged-One Dependence Estimators},
 *    year = {2006},
 *    ISBN = {1-59593-383-2}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Output debugging information
 * </pre>
 * 
 * <pre> -C
 *  Impose a critcal value for specialization-generalization relationship
 *  (default is 50)</pre>
 * 
 * <pre> -F
 *  Impose a frequency limit for superParents
 *  (default is 1)</pre>
 * 
 * <pre> -L
 *  Using Laplace estimation
 *  (default is m-esimation (m=1))</pre>
 * 
 * <pre> -M
 *  Weight value for m-estimation
 *  (default is 1.0)</pre>
 * 
 <!-- options-end -->
 *
 * @author Fei Zheng
 * @author Janice Boughton
 * @version $Revision$
 */
public class AODEsr extends Classifier
    implements OptionHandler, WeightedInstancesHandler, UpdateableClassifier,
               TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5602143019183068848L;

  /**
   * 3D array (m_NumClasses * m_TotalAttValues * m_TotalAttValues)
   * of attribute counts, i.e. the number of times an attribute value occurs
   * in conjunction with another attribute value and a class value.  
   */
  private double [][][] m_CondiCounts;
 
  /**
   * 2D array (m_TotalAttValues * m_TotalAttValues) of attributes counts.
   * similar to m_CondiCounts, but ignoring class value.
   */  
  private double [][] m_CondiCountsNoClass; 
    
  /** The number of times each class value occurs in the dataset */
  private double [] m_ClassCounts;
    
  /** The sums of attribute-class counts  
   *    -- if there are no missing values for att, then
   *       m_SumForCounts[classVal][att] will be the same as
   *       m_ClassCounts[classVal] 
   */
  private double [][] m_SumForCounts;

  /** The number of classes */
  private int m_NumClasses;
 
  /** The number of attributes in dataset, including class */
  private int m_NumAttributes;
    
  /** The number of instances in the dataset */
  private int m_NumInstances;
    
  /** The index of the class attribute */
  private int m_ClassIndex;
    
  /** The dataset */
  private Instances m_Instances;
    
  /**
   * The total number of values (including an extra for each attribute's 
   * missing value, which are included in m_CondiCounts) for all attributes 
   * (not including class).  Eg. for three atts each with two possible values,
   * m_TotalAttValues would be 9 (6 values + 3 missing).
   * This variable is used when allocating space for m_CondiCounts matrix.
   */
  private int m_TotalAttValues;
    
  /** The starting index (in the m_CondiCounts matrix) of the values for each attribute */
  private int [] m_StartAttIndex;
    
  /** The number of values for each attribute */
  private int [] m_NumAttValues;
    
  /** The frequency of each attribute value for the dataset */
  private double [] m_Frequencies;

  /** The number of valid class values observed in dataset 
   *  -- with no missing classes, this number is the same as m_NumInstances.
   */
  private double m_SumInstances;

  /** An att's frequency must be this value or more to be a superParent */
  private int m_Limit = 1;

  /** If true, outputs debugging info */
  private boolean m_Debug = false;
  
  /** m value for m-estimation */
  protected  double m_MWeight = 1.0;
  
  /** Using LapLace estimation or not*/
  private boolean m_Laplace = false;
  
  /** the critical value for the specialization-generalization */
  private int m_Critical = 50;

 
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "AODEsr augments AODE with Subsumption Resolution."
      +"AODEsr detects specializations between two attribute "
      +"values at classification time and deletes the generalization "
      +"attribute value.\n"
      +"For more information, see:\n"
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
    TechnicalInformation        result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Fei Zheng and Geoffrey I. Webb");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE, "Efficient Lazy Elimination for Averaged-One Dependence Estimators");
    result.setValue(Field.PAGES, "1113-1120");
    result.setValue(Field.BOOKTITLE, "Proceedings of the Twenty-third International Conference on Machine  Learning (ICML 2006)");
    result.setValue(Field.PUBLISHER, "ACM Press");
    result.setValue(Field.ISBN, "1-59593-383-2");

    return result;
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
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @throws Exception if the classifier has not been generated
   * successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    m_Instances = new Instances(instances);
    m_Instances.deleteWithMissingClass();

    // reset variable for this fold
    m_SumInstances = 0;
    m_ClassIndex = instances.classIndex();
    m_NumInstances = m_Instances.numInstances();
    m_NumAttributes = instances.numAttributes();
    m_NumClasses = instances.numClasses();

    // allocate space for attribute reference arrays
    m_StartAttIndex = new int[m_NumAttributes];
    m_NumAttValues = new int[m_NumAttributes];
 
    m_TotalAttValues = 0;
    for(int i = 0; i < m_NumAttributes; i++) {
       if(i != m_ClassIndex) {
          m_StartAttIndex[i] = m_TotalAttValues;
          m_NumAttValues[i] = m_Instances.attribute(i).numValues();
          m_TotalAttValues += m_NumAttValues[i] + 1;
          // + 1 so room for missing value count
       } else {
          // m_StartAttIndex[i] = -1;  // class isn't included
          m_NumAttValues[i] = m_NumClasses;
       }
    }

    // allocate space for counts and frequencies
    m_CondiCounts = new double[m_NumClasses][m_TotalAttValues][m_TotalAttValues];
    m_ClassCounts = new double[m_NumClasses];
    m_SumForCounts = new double[m_NumClasses][m_NumAttributes];
    m_Frequencies = new double[m_TotalAttValues];
    m_CondiCountsNoClass = new double[m_TotalAttValues][m_TotalAttValues];
    
    // calculate the counts
    for(int k = 0; k < m_NumInstances; k++) {
       addToCounts((Instance)m_Instances.instance(k));
    }

    // free up some space
    m_Instances = new Instances(m_Instances, 0);
  }
 

  /**
   * Updates the classifier with the given instance.
   *
   * @param instance the new training instance to include in the model 
   * @throws Exception if the instance could not be incorporated in
   * the model.
   */
  public void updateClassifier(Instance instance) {
    this.addToCounts(instance);
  }

  /**
   * Puts an instance's values into m_CondiCounts, m_ClassCounts and 
   * m_SumInstances.
   *
   * @param instance the instance whose values are to be put into the 
   *                 counts variables
   */
  private void addToCounts(Instance instance) {
 
    double [] countsPointer;
    double [] countsNoClassPointer;
 
    if(instance.classIsMissing())
       return;   // ignore instances with missing class

    int classVal = (int)instance.classValue();
    double weight = instance.weight();
 
    m_ClassCounts[classVal] += weight;
    m_SumInstances += weight;
   
    // store instance's att val indexes in an array, b/c accessing it 
    // in loop(s) is more efficient
    int [] attIndex = new int[m_NumAttributes];
    for(int i = 0; i < m_NumAttributes; i++) {
       if(i == m_ClassIndex)
          attIndex[i] = -1;  // we don't use the class attribute in counts
       else {
          if(instance.isMissing(i))
             attIndex[i] = m_StartAttIndex[i] + m_NumAttValues[i];
          else
             attIndex[i] = m_StartAttIndex[i] + (int)instance.value(i);
       }
    }

    for(int Att1 = 0; Att1 < m_NumAttributes; Att1++) {
       if(attIndex[Att1] == -1)
          continue;   // avoid pointless looping as Att1 is currently the class attribute

       m_Frequencies[attIndex[Att1]] += weight;
       
       // if this is a missing value, we don't want to increase sumforcounts
       if(!instance.isMissing(Att1))
          m_SumForCounts[classVal][Att1] += weight;

       // save time by referencing this now, rather than repeatedly in the loop
       countsPointer = m_CondiCounts[classVal][attIndex[Att1]];
       countsNoClassPointer = m_CondiCountsNoClass[attIndex[Att1]];

       for(int Att2 = 0; Att2 < m_NumAttributes; Att2++) {
          if(attIndex[Att2] != -1) {
             countsPointer[attIndex[Att2]] += weight;
             countsNoClassPointer[attIndex[Att2]] += weight;
          }
       }
    }
  }
 
 
  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception {

    // accumulates posterior probabilities for each class
    double [] probs = new double[m_NumClasses];

    // index for parent attribute value, and a count of parents used
    int pIndex, parentCount; 

    int [] SpecialGeneralArray = new int[m_NumAttributes];
    
    // pointers for efficiency
    double [][] countsForClass;
    double [] countsForClassParent;
    double [] countsForAtti;
    double [] countsForAttj;

    // store instance's att values in an int array, so accessing them 
    // is more efficient in loop(s).
    int [] attIndex = new int[m_NumAttributes];
    for(int att = 0; att < m_NumAttributes; att++) {
       if(instance.isMissing(att) || att == m_ClassIndex)
          attIndex[att] = -1; // can't use class & missing vals in calculations
       else
          attIndex[att] = m_StartAttIndex[att] + (int)instance.value(att);
    }
    // -1 indicates attribute is not a generalization of any other attributes
    for(int i = 0; i < m_NumAttributes; i++) {
       SpecialGeneralArray[i] = -1;
    }

    // calculate the specialization-generalization array
    for(int i = 0; i < m_NumAttributes; i++){
       // skip i if it's the class or is missing
       if(attIndex[i] == -1)  continue;
       countsForAtti = m_CondiCountsNoClass[attIndex[i]];
 
       for(int j = 0; j < m_NumAttributes; j++) {
          // skip j if it's the class, missing, is i or a generalization of i
          if((attIndex[j] == -1) || (i == j) || (SpecialGeneralArray[j] == i))
            continue;
         
          countsForAttj = m_CondiCountsNoClass[attIndex[j]];

          // check j's frequency is above critical value
          if(countsForAttj[attIndex[j]] > m_Critical) {

             // skip j if the frequency of i and j together is not equivalent
	     // to the frequency of j alone
             if(countsForAttj[attIndex[j]] == countsForAtti[attIndex[j]]) {

             // if attributes i and j are both a specialization of each other
             // avoid deleting both by skipping j
                if((countsForAttj[attIndex[j]] == countsForAtti[attIndex[i]])
                 && (i < j)){
                  continue;
                } else {
                    // set the specialization relationship
                    SpecialGeneralArray[i] = j;
                    break; // break out of j loop because a specialization has been found
                }
             }
          }
       }
    }
 
    // calculate probabilities for each possible class value
    for(int classVal = 0; classVal < m_NumClasses; classVal++) {
 
       probs[classVal] = 0;
       double x = 0;
       parentCount = 0;
 
       countsForClass = m_CondiCounts[classVal];

       // each attribute has a turn of being the parent
       for(int parent = 0; parent < m_NumAttributes; parent++) {
          if(attIndex[parent] == -1)
             continue;  // skip class attribute or missing value

          // determine correct index for the parent in m_CondiCounts matrix
          pIndex = attIndex[parent];

          // check that the att value has a frequency of m_Limit or greater
	  if(m_Frequencies[pIndex] < m_Limit) 
             continue;
          
          // delete the generalization attributes.
          if(SpecialGeneralArray[parent] != -1)
             continue;

          countsForClassParent = countsForClass[pIndex];

          // block the parent from being its own child
          attIndex[parent] = -1;

          parentCount++;

          double classparentfreq = countsForClassParent[pIndex];

          // find the number of missing values for parent's attribute
          double missing4ParentAtt = 
            m_Frequencies[m_StartAttIndex[parent] + m_NumAttValues[parent]];

          // calculate the prior probability -- P(parent & classVal)
           if (m_Laplace){
             x = LaplaceEstimate(classparentfreq, m_SumInstances - missing4ParentAtt, 
                                    m_NumClasses * m_NumAttValues[parent]);
          } else {
          
             x = MEstimate(classparentfreq, m_SumInstances - missing4ParentAtt, 
                                    m_NumClasses * m_NumAttValues[parent]);
          }


    
          // take into account the value of each attribute
          for(int att = 0; att < m_NumAttributes; att++) {
             if(attIndex[att] == -1) // skip class attribute or missing value
                continue;
             // delete the generalization attributes.
             if(SpecialGeneralArray[att] != -1)
                continue;
            
 
             double missingForParentandChildAtt = 
                      countsForClassParent[m_StartAttIndex[att] + m_NumAttValues[att]];

             if (m_Laplace){
                x *= LaplaceEstimate(countsForClassParent[attIndex[att]], 
                    classparentfreq - missingForParentandChildAtt, m_NumAttValues[att]);
             } else {
                x *= MEstimate(countsForClassParent[attIndex[att]], 
                    classparentfreq - missingForParentandChildAtt, m_NumAttValues[att]);
             }
          }

          // add this probability to the overall probability
          probs[classVal] += x;
 
          // unblock the parent
          attIndex[parent] = pIndex;
       }
 
       // check that at least one att was a parent
       if(parentCount < 1) {

          // do plain naive bayes conditional prob
	  probs[classVal] = NBconditionalProb(instance, classVal);
          //probs[classVal] = Double.NaN;

       } else {
 
          // divide by number of parent atts to get the mean
          probs[classVal] /= (double)(parentCount);
       }
    }
    Utils.normalize(probs);
    return probs;
  }


  /**
   * Calculates the probability of the specified class for the given test
   * instance, using naive Bayes.
   *
   * @param instance the instance to be classified
   * @param classVal the class for which to calculate the probability
   * @return predicted class probability
   * @throws Exception if there is a problem generating the prediction
   */
  public double NBconditionalProb(Instance instance, int classVal)
                                                     throws Exception {
    double prob;
    int attIndex;
    double [][] pointer;

    // calculate the prior probability
    if(m_Laplace) {
       prob = LaplaceEstimate(m_ClassCounts[classVal],m_SumInstances,m_NumClasses); 
    } else {
       prob = MEstimate(m_ClassCounts[classVal], m_SumInstances, m_NumClasses);
    }
    pointer = m_CondiCounts[classVal];
    
    // consider effect of each att value
    for(int att = 0; att < m_NumAttributes; att++) {
       if(att == m_ClassIndex || instance.isMissing(att))
          continue;
       
       // determine correct index for att in m_CondiCounts
       attIndex = m_StartAttIndex[att] + (int)instance.value(att);
       if (m_Laplace){
         prob *= LaplaceEstimate((double)pointer[attIndex][attIndex], 
                   (double)m_SumForCounts[classVal][att], m_NumAttValues[att]);
       } else {
           prob *= MEstimate((double)pointer[attIndex][attIndex], 
                   (double)m_SumForCounts[classVal][att], m_NumAttValues[att]);
       }
    }
    return prob;
  }


  /**
   * Returns the probability estimate, using m-estimate
   *
   * @param frequency frequency of value of interest
   * @param total count of all values
   * @param numValues number of different values
   * @return the probability estimate
   */
  public double MEstimate(double frequency, double total,
                          double numValues) {
    
    return (frequency + m_MWeight / numValues) / (total + m_MWeight);
  }
   
  /**
   * Returns the probability estimate, using laplace correction
   *
   * @param frequency frequency of value of interest
   * @param total count of all values
   * @param numValues number of different values
   * @return the probability estimate
   */
  public double LaplaceEstimate(double frequency, double total,
                                double numValues) {
    
    return (frequency + 1.0) / (total + numValues);
  }
    
   
  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);
        
    newVector.addElement(
       new Option("\tOutput debugging information\n",
                  "D", 0,"-D"));
    newVector.addElement(
       new Option("\tImpose a critcal value for specialization-generalization relationship\n"
                  + "\t(default is 50)", "C", 1,"-C"));
    newVector.addElement(
       new Option("\tImpose a frequency limit for superParents\n"
                  + "\t(default is 1)", "F", 2,"-F"));
    newVector.addElement(
       new Option("\tUsing Laplace estimation\n"
                  + "\t(default is m-esimation (m=1))",
                  "L", 3,"-L"));
    newVector.addElement(
       new Option("\tWeight value for m-estimation\n"
                  + "\t(default is 1.0)", "M", 4,"-M"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Output debugging information
   * </pre>
   * 
   * <pre> -C
   *  Impose a critcal value for specialization-generalization relationship
   *  (default is 50)</pre>
   * 
   * <pre> -F
   *  Impose a frequency limit for superParents
   *  (default is 1)</pre>
   * 
   * <pre> -L
   *  Using Laplace estimation
   *  (default is m-esimation (m=1))</pre>
   * 
   * <pre> -M
   *  Weight value for m-estimation
   *  (default is 1.0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    m_Debug = Utils.getFlag('D', options);

    String Critical = Utils.getOption('C', options);
    if(Critical.length() != 0) 
       m_Critical = Integer.parseInt(Critical);
    else
       m_Critical = 50;
    
    String Freq = Utils.getOption('F', options);
    if(Freq.length() != 0) 
       m_Limit = Integer.parseInt(Freq);
    else
       m_Limit = 1;
    
    m_Laplace = Utils.getFlag('L', options);
    String MWeight = Utils.getOption('M', options); 
    if(MWeight.length() != 0) {
       if(m_Laplace)
          throw new Exception("weight for m-estimate is pointless if using laplace estimation!");
       m_MWeight = Double.parseDouble(MWeight);
    } else
       m_MWeight = 1.0;
    
    Utils.checkForRemainingOptions(options);
  }
    
  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
        
    Vector result  = new Vector();

    if (m_Debug)
       result.add("-D");

    result.add("-F");
    result.add("" + m_Limit);

    if (m_Laplace) {
       result.add("-L");
    } else {
       result.add("-M");
       result.add("" + m_MWeight);
    }
        
    result.add("-C");
    result.add("" + m_Critical);

    return (String[]) result.toArray(new String[result.size()]);
  }
 
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String mestWeightTipText() {
    return "Set the weight for m-estimate.";
  }

  /**
   * Sets the weight for m-estimate
   *
   * @param w the weight
   */
  public void setMestWeight(double w) {
    if (getUseLaplace()) {
       System.out.println(
          "Weight is only used in conjunction with m-estimate - ignored!");
    } else {
      if(w > 0)
         m_MWeight = w;
      else
         System.out.println("M-Estimate Weight must be greater than 0!");
    }
  }

  /**
   * Gets the weight used in m-estimate
   *
   * @return the weight for m-estimation
   */
  public double getMestWeight() {
    return m_MWeight;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useLaplaceTipText() {
    return "Use Laplace correction instead of m-estimation.";
  }

  /**
   * Gets if laplace correction is being used.
   *
   * @return Value of m_Laplace.
   */
  public boolean getUseLaplace() {
    return m_Laplace;
  }

  /**
   * Sets if laplace correction is to be used.
   *
   * @param value Value to assign to m_Laplace.
   */
  public void setUseLaplace(boolean value) {
    m_Laplace = value;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String frequencyLimitTipText() {
    return "Attributes with a frequency in the train set below "
           + "this value aren't used as parents.";
  }

  /**
   * Sets the frequency limit
   *
   * @param f the frequency limit
   */
  public void setFrequencyLimit(int f) {
    m_Limit = f;
  }

  /**
   * Gets the frequency limit.
   *
   * @return the frequency limit
   */
  public int getFrequencyLimit() {
    return m_Limit;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String criticalValueTipText() {
    return "Specify critical value for specialization-generalization "
           + "relationship (default 50).";
  }

  /**
   * Sets the critical value
   *
   * @param c the critical value
   */
  public void setCriticalValue(int c) {
    m_Critical = c;
  }

  /**
   * Gets the critical value.
   *
   * @return the critical value
   */
  public int getCriticalValue() {
    return m_Critical;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
 
    StringBuffer text = new StringBuffer();
        
    text.append("The AODEsr Classifier");
    if (m_Instances == null) {
       text.append(": No model built yet.");
    } else {
       try {
          for (int i = 0; i < m_NumClasses; i++) {
             // print to string, the prior probabilities of class values
             text.append("\nClass " + m_Instances.classAttribute().value(i) +
                       ": Prior probability = " + Utils.
                          doubleToString(((m_ClassCounts[i] + 1)
                       /(m_SumInstances + m_NumClasses)), 4, 2)+"\n\n");
          }
                
          text.append("Dataset: " + m_Instances.relationName() + "\n"
                      + "Instances: " + m_NumInstances + "\n"
                      + "Attributes: " + m_NumAttributes + "\n"
		      + "Frequency limit for superParents: " + m_Limit + "\n"
                      + "Critical value for the specializtion-generalization "
                      + "relationship: " + m_Critical + "\n");
          if(m_Laplace) {
            text.append("Using LapLace estimation.");
          } else {
              text.append("Using m-estimation, m = " + m_MWeight); 
          }
       } catch (Exception ex) {
          text.append(ex.getMessage());
       }
    }
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new AODEsr(), argv);
  }
}

