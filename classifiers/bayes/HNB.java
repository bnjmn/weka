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
 *    HNB.java
 *    Copyright (C) 2004 Liangxiao Jiang
 */

package weka.classifiers.bayes;

import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * Class for contructing Hidden Naive Bayes classification model
 * with high classification accuracy and AUC.<p/>
 *
 * For more information on HNB, see<p/>
 *
 * H. Zhang, L. Jiang and J. Su, Hidden Naive Bayes,
 * Proceedings of the Twentieth National Conference on Artificial
 * Intelligence(AAAI-05), pp.919-924, AAAI Press(2005).
 *
 * @author H. Zhang (hzhang@unb.ca)
 * @author Liangxiao Jiang (ljiang@cug.edu.cn)
 * @version $Revision: 1.2 $
 */

/**
 * Implement an Hidden Naive Bayes classifier.
 */
public class HNB 
  extends Classifier {

  /** The number of each class value occurs in the dataset */
  private double[] m_ClassCounts;

  /** 3D array of attribute with two conditions counts (class and another attribute) */
  private double[][][] m_CondiCounts;

  /** The starting index in the m_CondiCounts array of each attribute */
  private int[] m_StartAttIndex;

  /** The number of values for each attribute */
  private int[] m_NumAttValues;

  /** The total number of values for all attributes (not including class) */
  private int m_TotalAttValues;

  /** The number of classes */
  private int m_NumClasses;

  /** The number of attributes in dataset (including class) */
  private int m_NumAttributes;

  /** The number of instances in the dataset */
  private int m_NumInstances;

  /** The index of the class attribute */
  private int m_ClassIndex;

  /** The 2D array of conditional mutual information of each pair attributes */
  private double[][] m_conditionalMutualInformation;

  /**
   * Returns a string describing this classifier.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return  
      "Contructs Hidden Naive Bayes classification model with high "
      + "classification accuracy and AUC.\n"
      + "For more information refer to:\n"
      + "H. Zhang, L. Jiang and J. Su, Hidden Naive Bayes, Proceedings "
      + "of the Twentieth National Conference on Artificial "
      + "Intelligence(AAAI-05), pp.919-924, AAAI Press(2005).";
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // judge dataset
    // class
    if (instances.classIndex() == -1)
      throw new UnassignedClassException("No class attribute set!");
    if (!instances.classAttribute().isNominal())
      throw new UnsupportedClassTypeException("Handles only nominal classes!");
    
    // attributes
    Enumeration enumInsts,enumAtts;
    enumAtts = instances.enumerateAttributes();
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (attribute.type()!= Attribute.NOMINAL){
        throw new UnsupportedAttributeTypeException("Handles nominal variables only.");
      }
      enumInsts = instances.enumerateInstances();
      while (enumInsts.hasMoreElements()) {
        if (((Instance) enumInsts.nextElement()).isMissing(attribute)) {
          throw new NoSupportForMissingValuesException("Can't handle missing values.");
        }
      }
    }

    // reset variable
    m_NumClasses = instances.numClasses();
    m_ClassIndex = instances.classIndex();
    m_NumAttributes = instances.numAttributes();
    m_NumInstances = instances.numInstances();
    m_TotalAttValues = 0;

    // allocate space for attribute reference arrays
    m_StartAttIndex = new int[m_NumAttributes];
    m_NumAttValues = new int[m_NumAttributes];

    // set the starting index of each attribute and the number of values for
    // each attribute and the total number of values for all attributes (not
    // including class).
    for(int i = 0; i < m_NumAttributes; i++) {
      if(i != m_ClassIndex) {
        m_StartAttIndex[i] = m_TotalAttValues;
        m_NumAttValues[i] = instances.attribute(i).numValues();
        m_TotalAttValues += m_NumAttValues[i];
      }
      else {
        m_StartAttIndex[i] = -1; // class needn't be included
        m_NumAttValues[i] = m_NumClasses;
      }
    }

    // allocate space for counts and frequencies
    m_ClassCounts = new double[m_NumClasses];
    m_CondiCounts = new double[m_NumClasses][m_TotalAttValues][m_TotalAttValues];

    // Calculate the counts
    for(int k = 0; k < m_NumInstances; k++) {
      int classVal=(int)instances.instance(k).classValue();
      m_ClassCounts[classVal] ++;
      int[] attIndex = new int[m_NumAttributes];
      for(int i = 0; i < m_NumAttributes; i++) {
        if(i == m_ClassIndex)
          attIndex[i] = -1;
        else
          attIndex[i] = m_StartAttIndex[i] + (int)instances.instance(k).value(i);
      }
      for(int Att1 = 0; Att1 < m_NumAttributes; Att1++) {
        if(attIndex[Att1] == -1) continue;
        for(int Att2 = 0; Att2 < m_NumAttributes; Att2++) {
          if((attIndex[Att2] != -1)) {
            m_CondiCounts[classVal][attIndex[Att1]][attIndex[Att2]] ++;
          }
        }
      }
    }

    //compute conditional mutual information of each pair attributes
    m_conditionalMutualInformation=new double[instances.numAttributes()-1][instances.numAttributes()-1];
    for(int son=0;son<instances.numAttributes()-1;son++){
      for(int parent=0;parent<instances.numAttributes()-1;parent++){
        if(son==parent)continue;
        m_conditionalMutualInformation[son][parent]=conditionalMutualInfo(son,parent);
      }
    }

  }

  /**
   * Computes conditional mutual information between a pair of attributes.
   *
   * @param son and parent are a pair of attributes
   * @return the conditional mutual information between son and parent given
   * class
   */
  private double conditionalMutualInfo(int son, int parent)throws Exception{

    double CondiMutualInfo=0;
    int sIndex=m_StartAttIndex[son];
    int pIndex=m_StartAttIndex[parent];

    double[] PriorsClass = new double[m_NumClasses];
    double[][] PriorsClassSon=new double[m_NumClasses][m_NumAttValues[son]];
    double[][] PriorsClassParent=new double[m_NumClasses][m_NumAttValues[parent]];
    double[][][] PosterioriClassParentSon=new double[m_NumClasses][m_NumAttValues[parent]][m_NumAttValues[son]];

    for(int i=0;i<m_NumClasses;i++){
      PriorsClass[i]=m_ClassCounts[i]/m_NumInstances;
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[son];j++){
        PriorsClassSon[i][j]=m_CondiCounts[i][sIndex+j][sIndex+j]/m_NumInstances;
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        PriorsClassParent[i][j]=m_CondiCounts[i][pIndex+j][pIndex+j]/m_NumInstances;
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        for(int k=0;k<m_NumAttValues[son];k++){
          if(m_CondiCounts[i][pIndex+j][pIndex+j]<1e-6){
            PosterioriClassParentSon[i][j][k]=0;
          }
          else{
            PosterioriClassParentSon[i][j][k]=m_CondiCounts[i][pIndex+j][sIndex+k]/m_CondiCounts[i][pIndex+j][pIndex+j];
          }
        }
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        for(int k=0;k<m_NumAttValues[son];k++){
          CondiMutualInfo+=PosterioriClassParentSon[i][j][k]*PriorsClassParent[i][j]*log2(PosterioriClassParentSon[i][j][k]*PriorsClass[i],PriorsClassSon[i][k]);
        }
      }
    }
    return CondiMutualInfo;

  }

  /**
   * compute the logarithm whose base is 2.
   *
   * @param args x,y are numerator and denominator of the fraction.
   * @return the natual logarithm of this fraction.
   */
  private double log2(double x,double y){

    if(x<1e-6||y<1e-6)
      return 0.0;
    else
      return Math.log(x/y)/Math.log(2);

  }

  /**
   * Calculates the class membership probabilities for the given test instance
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    //Definition of local variables
    double[] probs = new double[m_NumClasses];
    int sIndex;
    double prob;
    double CondiMutualInfoSum;

    // store instance's att values in an int array
    int[] attIndex = new int[m_NumAttributes];
    for(int att = 0; att < m_NumAttributes; att++) {
      if(att == m_ClassIndex)
        attIndex[att] = -1;
      else
        attIndex[att] = m_StartAttIndex[att] + (int)instance.value(att);
    }

    // calculate probabilities for each possible class value
    for(int classVal = 0; classVal < m_NumClasses; classVal++) {
      probs[classVal]=(m_ClassCounts[classVal]+1.0)/(m_NumInstances+m_NumClasses);// calculate the prior probability
      for(int son = 0; son < m_NumAttributes; son++) {
        if(attIndex[son]==-1) continue;
        sIndex=attIndex[son];
        attIndex[son]=-1;// block the parent from being its own child
        prob=0;
        CondiMutualInfoSum=0;
        for(int parent=0; parent<m_NumAttributes; parent++) {
          if(attIndex[parent]==-1) continue;
          CondiMutualInfoSum+=m_conditionalMutualInformation[son][parent];
          prob+=m_conditionalMutualInformation[son][parent]*(m_CondiCounts[classVal][attIndex[parent]][sIndex]+1.0)/(m_CondiCounts[classVal][attIndex[parent]][attIndex[parent]] + m_NumAttValues[son]);
        }
        if(CondiMutualInfoSum>0){
          prob=prob/CondiMutualInfoSum;
          probs[classVal] *= prob;
        }
        else{
          prob=(m_CondiCounts[classVal][sIndex][sIndex]+1.0)/(m_ClassCounts[classVal]+m_NumAttValues[son]);
          probs[classVal]*= prob;
        }
        attIndex[son] = sIndex; //unblock the parent
      }
    }
    Utils.normalize(probs);
    return probs;
  }

  /**
   * returns a string representation of the classifier
   */
  public String toString() {
    return "HNB (Hidden Naive Bayes)";
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String[] args) {
    try {
      System.out.println(Evaluation.evaluateModel(new HNB(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

}
