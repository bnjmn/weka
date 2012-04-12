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

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
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
 <!-- globalinfo-start -->
 * AODE achieves highly accurate classification by averaging over all of a small space of alternative naive-Bayes-like models that have weaker (and hence less detrimental) independence assumptions than naive Bayes. The resulting algorithm is computationally efficient while delivering highly accurate classification on many learning  tasks.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * G. Webb, J. Boughton, Z. Wang (2005). Not So Naive Bayes: Aggregating One-Dependence Estimators. Machine Learning. 58(1):5-24.<br/>
 * <br/>
 * Further papers are available at<br/>
 *   http://www.csse.monash.edu.au/~webb/.<br/>
 * <br/>
 * Can use an m-estimate for smoothing base probability estimates in place of the Laplace correction (via option -M).<br/>
 * Default frequency limit set to 1.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Webb2005,
 *    author = {G. Webb and J. Boughton and Z. Wang},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {5-24},
 *    title = {Not So Naive Bayes: Aggregating One-Dependence Estimators},
 *    volume = {58},
 *    year = {2005}
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
 * <pre> -F &lt;int&gt;
 *  Impose a frequency limit for superParents
 *  (default is 1)</pre>
 * 
 * <pre> -M
 *  Use m-estimate instead of laplace correction
 * </pre>
 * 
 * <pre> -W &lt;int&gt;
 *  Specify a weight to use with m-estimate
 *  (default is 1)</pre>
 * 
 <!-- options-end -->
 *
 * @author Janice Boughton (jrbought@csse.monash.edu.au)
 * @author Zhihai Wang (zhw@csse.monash.edu.au)
 * @version $Revision$
 */
public class AODE
    extends AbstractClassifier
    implements OptionHandler, WeightedInstancesHandler, UpdateableClassifier, 
               TechnicalInformationHandler {
    
  /** for serialization */
  static final long serialVersionUID = 9197439980415113523L;
  
  /**
   * 3D array (m_NumClasses * m_TotalAttValues * m_TotalAttValues)
   * of attribute counts, i.e., the number of times an attribute value occurs
   * in conjunction with another attribute value and a class value.  
   */
  private double [][][] m_CondiCounts;
    
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
   * (not including class). E.g., for three atts each with two possible values,
   * m_TotalAttValues would be 9 (6 values + 3 missing).
   * This variable is used when allocating space for m_CondiCounts matrix.
   */
  private int m_TotalAttValues;
    
  /** The starting index (in the m_CondiCounts matrix) of the values for each
   * attribute */
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

  /** flag for using m-estimates */
  private boolean m_MEstimates = false;

  /** value for m in m-estimate */
  private int m_Weight = 1;

 
  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "AODE achieves highly accurate classification by averaging over "
      +"all of a small space of alternative naive-Bayes-like models that have "
      +"weaker (and hence less detrimental) independence assumptions than "
      +"naive Bayes. The resulting algorithm is computationally efficient "
      +"while delivering highly accurate classification on many learning  "
      +"tasks.\n\n"
      +"For more information, see\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      +"Further papers are available at\n"
      +"  http://www.csse.monash.edu.au/~webb/.\n\n"
      + "Can use an m-estimate for smoothing base probability estimates "
      + "in place of the Laplace correction (via option -M).\n"
      + "Default frequency limit set to 1.";
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
    result.setValue(Field.AUTHOR, "G. Webb and J. Boughton and Z. Wang");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.TITLE, "Not So Naive Bayes: Aggregating One-Dependence Estimators");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "58");
    result.setValue(Field.NUMBER, "1");
    result.setValue(Field.PAGES, "5-24");

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
    m_NumAttributes = m_Instances.numAttributes();
    m_NumClasses = m_Instances.numClasses();
 
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
   */
    public void updateClassifier(Instance instance) {
	this.addToCounts(instance);
    }

    /** 
     * Puts an instance's values into m_CondiCounts, m_ClassCounts and 
     * m_SumInstances.
     *
     * @param instance  the instance whose values are to be put into the counts
     *                  variables
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
   * @throws Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception {
 
    // accumulates posterior probabilities for each class
    double [] probs = new double[m_NumClasses];

    // index for parent attribute value, and a count of parents used
    int pIndex, parentCount;
 
    // pointers for efficiency
    // for current class, point to joint frequency for any pair of att values
    double [][] countsForClass;
    // for current class & parent, point to joint frequency for any att value
    double [] countsForClassParent;

    // store instance's att indexes in an int array, so accessing them 
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
       double spodeP = 0;  // P(X,y) for current parent and class
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

          // joint frequency of class and parent
          double classparentfreq = countsForClassParent[pIndex];

          // find the number of missing values for parent's attribute
          double missing4ParentAtt = 
              m_Frequencies[m_StartAttIndex[parent] + m_NumAttValues[parent]];

          // calculate the prior probability -- P(parent & classVal)
          if (!m_MEstimates) {
             spodeP = (classparentfreq + 1.0)
                / ((m_SumInstances - missing4ParentAtt) + m_NumClasses
	           * m_NumAttValues[parent]);
          } else {
             spodeP = (classparentfreq + ((double)m_Weight
                        / (double)(m_NumClasses * m_NumAttValues[parent])))
                / ((m_SumInstances - missing4ParentAtt) + m_Weight);
          }

          // take into account the value of each attribute
          for(int att = 0; att < m_NumAttributes; att++) {
             if(attIndex[att] == -1)
                continue;
 
             double missingForParentandChildAtt = 
              countsForClassParent[m_StartAttIndex[att] + m_NumAttValues[att]];

             if(!m_MEstimates) {
                spodeP *= (countsForClassParent[attIndex[att]] + 1.0)
                    / ((classparentfreq - missingForParentandChildAtt)
                       + m_NumAttValues[att]);
             } else {
                spodeP *= (countsForClassParent[attIndex[att]]
                           + ((double)m_Weight / (double)m_NumAttValues[att]))
                    / ((classparentfreq - missingForParentandChildAtt)
                       + m_Weight);
             }
          }

          // add this probability to the overall probability
          probs[classVal] += spodeP;
 
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
   */
  public double NBconditionalProb(Instance instance, int classVal) {
    
    double prob;
    double [][] pointer;

    // calculate the prior probability
    if(!m_MEstimates) {
       prob = (m_ClassCounts[classVal] + 1.0) / (m_SumInstances + m_NumClasses);
    } else {
       prob = (m_ClassCounts[classVal]
               + ((double)m_Weight / (double)m_NumClasses))
             / (m_SumInstances + m_Weight);
    }
    pointer = m_CondiCounts[classVal];
    
    // consider effect of each att value
    for(int att = 0; att < m_NumAttributes; att++) {
       if(att == m_ClassIndex || instance.isMissing(att))
          continue;

       // determine correct index for att in m_CondiCounts
       int aIndex = m_StartAttIndex[att] + (int)instance.value(att);

       if(!m_MEstimates) {
          prob *= (double)(pointer[aIndex][aIndex] + 1.0)
              / ((double)m_SumForCounts[classVal][att] + m_NumAttValues[att]);
       } else {
          prob *= (double)(pointer[aIndex][aIndex] 
                    + ((double)m_Weight / (double)m_NumAttValues[att]))
                 / (double)(m_SumForCounts[classVal][att] + m_Weight);
       }
    }
    return prob;
  }


  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
        
    Vector newVector = new Vector(4);
        
    newVector.addElement(
       new Option("\tOutput debugging information\n",
                  "D", 0,"-D"));
    newVector.addElement(
       new Option("\tImpose a frequency limit for superParents\n"
                  + "\t(default is 1)", "F", 1,"-F <int>"));
    newVector.addElement(
       new Option("\tUse m-estimate instead of laplace correction\n",
                  "M", 0,"-M"));
    newVector.addElement(
       new Option("\tSpecify a weight to use with m-estimate\n"
                  + "\t(default is 1)", "W", 1,"-W <int>"));
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
   * <pre> -F &lt;int&gt;
   *  Impose a frequency limit for superParents
   *  (default is 1)</pre>
   * 
   * <pre> -M
   *  Use m-estimate instead of laplace correction
   * </pre>
   * 
   * <pre> -W &lt;int&gt;
   *  Specify a weight to use with m-estimate
   *  (default is 1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    m_Debug = Utils.getFlag('D', options);

    String Freq = Utils.getOption('F', options);
    if (Freq.length() != 0) 
       m_Limit = Integer.parseInt(Freq);
    else
       m_Limit = 1;
 
    m_MEstimates = Utils.getFlag('M', options);
    String weight = Utils.getOption('W', options);
    if (weight.length() != 0) {
       if (!m_MEstimates)
          throw new Exception("Can't use Laplace AND m-estimate weight. Choose one.");
       m_Weight = Integer.parseInt(weight);
    } 
    else {
       if (m_MEstimates)
          m_Weight = 1;
    }

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

    if (m_MEstimates) {
       result.add("-M");
       result.add("-W");
       result.add("" + m_Weight);
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
    
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightTipText() {
    return "Set the weight for m-estimate.";
  }

  /**
   * Sets the weight for m-estimate
   *
   * @param w the weight
   */
  public void setWeight(int w) {
    if (!getUseMEstimates()) {
      System.out.println(
          "Weight is only used in conjunction with m-estimate - ignored!");
    }
    else {
      if (w > 0)
        m_Weight = w;
      else
        System.out.println("Weight must be greater than 0!");
    }
  }

  /**
   * Gets the weight used in m-estimate
   *
   * @return the frequency limit
   */
  public int getWeight() {
    return m_Weight;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useMEstimatesTipText() {
    return "Use m-estimate instead of laplace correction.";
  }

  /**
   * Gets if m-estimaces is being used.
   *
   * @return Value of m_MEstimates.
   */
  public boolean getUseMEstimates() {
    return m_MEstimates;
  }

  /**
   * Sets if m-estimates is to be used.
   *
   * @param value     Value to assign to m_MEstimates.
   */
  public void setUseMEstimates(boolean value) {
    m_MEstimates = value;
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
          text.append("Correction: ");
          if (!m_MEstimates)
             text.append("laplace\n");
          else
             text.append("m-estimate (m=" + m_Weight + ")\n");
                
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
    runClassifier(new AODE(), argv);
  }
}
