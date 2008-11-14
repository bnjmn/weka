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
 *    AODE.java
 *    Copyright (C) 2003
 *    Algorithm developed by: Geoff Webb
 *    Code written by: Janice Boughton & Zhihai Wang
 */

package weka.classifiers.bayes;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;

/**
 * AODE achieves highly accurate classification by averaging over all
 * of a small space of alternative naive-Bayes-like models that have
 * weaker (and hence less detrimental) independence assumptions than
 * naive Bayes. The resulting algorithm is computationally efficient while
 * delivering highly accurate classification on many learning tasks.<br>
 * For more information, see<p>
 * G. Webb, J. Boughton & Z. Wang (2004). <i>Not So Naive Bayes.</i>
 * To be published in Machine Learning.<br>
 * G. Webb, J. Boughton & Z. Wang (2002). <i>Averaged One-Dependence
 * Estimators: Preliminary Results.</i> AI2002 Data Mining Workshop, Canberra.
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Debugging information is printed if this flag is specified.<p>
 * 
 * -F <br>
 * Specify the frequency limit for parent attributes.<p>
 *
 * @author Janice Boughton (jrbought@csse.monash.edu.au) & Zhihai Wang (zhw@csse.monash.edu.au)
 * @version $Revision: 1.8.2.4 $
 *  this version resolves errors in the handling of missing attribute values.
 */
public class AODE extends Classifier
    implements OptionHandler, WeightedInstancesHandler, UpdateableClassifier {
    
  /**
   * 3D array (m_NumClasses * m_TotalAttValues * m_TotalAttValues)
   * of attribute counts, i.e. the number of times an attribute value occurs
   * in conjunction with another attribute value and a class value.  
   */
  private double [][][] m_CondiCounts;
    
  /** The number of times each class value occurs in the dataset */
  private double [] m_ClassCounts;
    
  /** The sums of attribute-class counts  
   *    -- if there are no missing values for att, then m_SumForCounts[classVal][att] 
   *       will be the same as m_ClassCounts[classVal] 
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
  private int m_Limit;

  /** If true, outputs debugging info */
  private boolean m_Debug = false;

 
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "AODE achieves highly accurate classification by averaging over all "
      +"of a small space of alternative naive-Bayes-like models that have "
      +"weaker (and hence less detrimental) independence assumptions than "
      +"naive Bayes. The resulting algorithm is computationally efficient while "
      +"delivering highly accurate classification on many learning tasks.\n\n"
      +"For more information, see\n\n"
      +"G. Webb, J. Boughton & Z. Wang (2004). Not So Naive Bayes. "
      +"To be published in Machine Learning. "
      +"G. Webb, J. Boughton & Z. Wang (2002). <i>Averaged One-Dependence "
      +"Estimators: Preliminary Results. AI2002 Data Mining Workshop, Canberra.";
  }
 
  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @exception Exception if the classifier has not been generated
   * successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // reset variable for this fold
    m_SumInstances = 0;
    
    m_NumClasses = instances.numClasses();
    if(instances.classAttribute().isNumeric()) {
       throw new Exception("AODE: Class is numeric!");
    }
    if(m_NumClasses < 2) {
       throw new Exception ("Dataset has no class attribute");
    }
    if(instances.checkForStringAttributes()) {
       throw new Exception("AODE: String attributes are not allowed.");
    }

    m_ClassIndex = instances.classIndex();
    m_NumAttributes = instances.numAttributes();
    for(int att = 0; att < m_NumAttributes; att++) {
       Attribute attribute = (Attribute)instances.attribute(att);
       if(!attribute.isNominal()) {
          throw new Exception("Attributes must be nominal.  " +
                              "Discretize dataset with FilteredClassifer.");
       }
    }

    // copy the instances
    m_Instances = instances;
    m_NumInstances = m_Instances.numInstances();
 
    // allocate space for attribute reference arrays
    m_StartAttIndex = new int[m_NumAttributes];
    m_NumAttValues = new int[m_NumAttributes];
 
    m_TotalAttValues = 0;
    for(int i = 0; i < m_NumAttributes; i++) {
       if(i != m_ClassIndex) {
          m_StartAttIndex[i] = m_TotalAttValues;
          m_NumAttValues[i] = m_Instances.attribute(i).numValues();
          m_TotalAttValues += m_NumAttValues[i] + 1;  // + 1 so room for missing value count
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
   * @exception Exception if the instance could not be incorporated in
   * the model.
   */
    public void updateClassifier(Instance instance) {
	this.addToCounts(instance);
    }

    /** Puts an instance's values into m_CondiCounts, m_ClassCounts and 
     * m_SumInstances.
     *
     * @param instance the instance whose values are to be put into the counts variables
     *
     */
  private void addToCounts(Instance instance) {
 
    double [] countsPointer;
 
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

       // save time by referencing this now, rather than do it repeatedly in the loop
       countsPointer = m_CondiCounts[classVal][attIndex[Att1]];

       for(int Att2 = 0; Att2 < m_NumAttributes; Att2++) {
          if(attIndex[Att2] != -1) {
             countsPointer[attIndex[Att2]] += weight;
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
   * @exception Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception {
 
    double [] probs = new double[m_NumClasses];
    int pIndex, parentCount; 
 
    // pointers for efficiency
    double [][] countsForClass;
    double [] countsForClassParent;

    // store instance's att values in an int array, so accessing them 
    // is more efficient in loop(s).
    int [] attIndex = new int[m_NumAttributes];
    for(int att = 0; att < m_NumAttributes; att++) {
       if(instance.isMissing(att) || att == m_ClassIndex)
          attIndex[att] = -1;   // can't use class or missing values in calculations
       else
	  attIndex[att] = m_StartAttIndex[att] + (int)instance.value(att);
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

          countsForClassParent = countsForClass[pIndex];

          // block the parent from being its own child
          attIndex[parent] = -1;

          parentCount++;

          double classparentfreq = countsForClassParent[pIndex];

          // find the number of missing values for parent's attribute
          double missing4ParentAtt = 
                 m_Frequencies[m_StartAttIndex[parent] + m_NumAttValues[parent]];

          // calculate the prior probability -- P(parent & classVal)
          x = (classparentfreq + 1.0)
             / ((m_SumInstances - missing4ParentAtt) + m_NumClasses
	        * m_NumAttValues[parent]);

          // take into account the value of each attribute
          for(int att = 0; att < m_NumAttributes; att++) {
             if(attIndex[att] == -1)
                continue;
 
             double missingForParentandChildAtt = 
                      countsForClassParent[m_StartAttIndex[att] + m_NumAttValues[att]];

             x *= (countsForClassParent[attIndex[att]] + 1.0)
                 / (classparentfreq - missingForParentandChildAtt + m_NumAttValues[att]);
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
   * @exception Exception if there is a problem generating the prediction
   */
  public double NBconditionalProb(Instance instance, int classVal) {
    
    double prob;
    int attIndex;
    double [][] pointer;

    // calculate the prior probability
    prob = (m_ClassCounts[classVal] + 1.0) / (m_SumInstances + m_NumClasses);
    
    pointer = m_CondiCounts[classVal];
    
    // consider effect of each att value
    for(int att = 0; att < m_NumAttributes; att++) {
       if(att == m_ClassIndex || instance.isMissing(att))
          continue;

       // determine correct index for att in m_CondiCounts
       attIndex = m_StartAttIndex[att] + (int)instance.value(att);

       prob *= (double)(pointer[attIndex][attIndex] + 1.0)
              / ((double)m_SumForCounts[classVal][att] + m_NumAttValues[att]);
    }

    return prob;
  }


  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
        
    Vector newVector = new Vector(2);
        
    newVector.addElement(
       new Option("\tOutput debugging information\n",
                  "D", 0,"-D"));
    newVector.addElement(
       new Option("\tImpose a frequency limit for superParents\n"
                  + "\t(default is 30)", "F", 1,"-F"));

    return newVector.elements();
  }

    
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Debugging information is printed.<p>
   *
   * -F <br>
   * Specify the frequency limit for parent attributes.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    m_Debug = Utils.getFlag('D', options);

    String Freq = Utils.getOption('F', options);
    if(Freq.length() != 0) 
       m_Limit = Integer.parseInt(Freq);
    else
       m_Limit = 30;
    
    Utils.checkForRemainingOptions(options);
  }
    
  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
        
    String [] options = new String [3];
    int current = 0;
        
    if (m_Debug) {
       options[current++] = "-D";
    }
        
    options[current++] = "-F ";
    options[current++] = "" + m_Limit;

    while (current < options.length) {
       options[current++] = "";
    }
    return options;
  }
    
  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
 
    StringBuffer text = new StringBuffer();
        
    text.append("The AODE Classifier");
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
		      + "Frequency limit for superParents: " + m_Limit + "\n");
                
       } catch (Exception ex) {
          text.append(ex.getMessage());
       }
    }
        
    return text.toString();
  }
    
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
 
    try {
       System.out.println(Evaluation.evaluateModel(new AODE(), argv));
    } catch (Exception e) {
       e.printStackTrace();
       System.err.println(e.getMessage());
    }
  }
}
