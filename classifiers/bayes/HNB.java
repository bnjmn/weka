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

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * Contructs Hidden Naive Bayes classification model with high classification accuracy and AUC.<br/>
 * <br/>
 * For more information refer to:<br/>
 * <br/>
 * H. Zhang, L. Jiang, J. Su: Hidden Naive Bayes. In: Twentieth National Conference on Artificial Intelligence, 919-924, 2005.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;incproceedings{Zhang2005,
 *    author = {H. Zhang and L. Jiang and J. Su},
 *    booktitle = {Twentieth National Conference on Artificial Intelligence},
 *    pages = {919-924},
 *    publisher = {AAAI Press},
 *    title = {Hidden Naive Bayes},
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
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author H. Zhang (hzhang@unb.ca)
 * @author Liangxiao Jiang (ljiang@cug.edu.cn)
 * @version $Revision: 1.6 $
 */
public class HNB  
  extends Classifier
  implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -4503874444306113214L;
  
  /** The number of each class value occurs in the dataset */
  private double [] m_ClassCounts;

  /** The number of class and two attributes values occurs in the dataset */
  private double [][][] m_ClassAttAttCounts;

  /** The number of values for each attribute in the dataset */
  private int [] m_NumAttValues;

  /** The number of values for all attributes in the dataset */
  private int m_TotalAttValues;

  /** The number of classes in the dataset */
  private int m_NumClasses;

  /** The number of attributes including class in the dataset */
  private int m_NumAttributes;

  /** The number of instances in the dataset */
  private int m_NumInstances;

  /** The index of the class attribute in the dataset */
  private int m_ClassIndex;

  /** The starting index of each attribute in the dataset */
  private int[] m_StartAttIndex;

  /** The 2D array of conditional mutual information of each pair attributes */
  private double[][] m_condiMutualInfo;

  /**
   * Returns a string describing this classifier.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return
      "Contructs Hidden Naive Bayes classification model with high "
      + "classification accuracy and AUC.\n\n"
      + "For more information refer to:\n\n"
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
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "H. Zhang and L. Jiang and J. Su");
    result.setValue(Field.TITLE, "Hidden Naive Bayes");
    result.setValue(Field.BOOKTITLE, "Twentieth National Conference on Artificial Intelligence");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.PAGES, "919-924");
    result.setValue(Field.PUBLISHER, "AAAI Press");
    
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

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
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
    // each attribute and the total number of values for all attributes (not including class).
    for(int i = 0; i < m_NumAttributes; i++) {
      if(i != m_ClassIndex) {
        m_StartAttIndex[i] = m_TotalAttValues;
        m_NumAttValues[i] = instances.attribute(i).numValues();
        m_TotalAttValues += m_NumAttValues[i];
      }
      else {
        m_StartAttIndex[i] = -1;
        m_NumAttValues[i] = m_NumClasses;
      }
    }

    // allocate space for counts and frequencies
    m_ClassCounts = new double[m_NumClasses];
    m_ClassAttAttCounts = new double[m_NumClasses][m_TotalAttValues][m_TotalAttValues];

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
            m_ClassAttAttCounts[classVal][attIndex[Att1]][attIndex[Att2]] ++;
          }
        }
      }
    }

    //compute conditional mutual information of each pair attributes (not including class)
    m_condiMutualInfo=new double[m_NumAttributes][m_NumAttributes];
    for(int son=0;son<m_NumAttributes;son++){
      if(son == m_ClassIndex) continue;
      for(int parent=0;parent<m_NumAttributes;parent++){
        if(parent == m_ClassIndex || son==parent) continue;
        m_condiMutualInfo[son][parent]=conditionalMutualInfo(son,parent);
      }
    }
  }

  /**
   * Computes conditional mutual information between a pair of attributes.
   *
   * @param son the son attribute
   * @param parent the parent attribute
   * @return the conditional mutual information between son and parent given class
   * @throws Exception if computation fails
   */
  private double conditionalMutualInfo(int son, int parent) throws Exception{

    double CondiMutualInfo=0;
    int sIndex=m_StartAttIndex[son];
    int pIndex=m_StartAttIndex[parent];
    double[] PriorsClass = new double[m_NumClasses];
    double[][] PriorsClassSon=new double[m_NumClasses][m_NumAttValues[son]];
    double[][] PriorsClassParent=new double[m_NumClasses][m_NumAttValues[parent]];
    double[][][] PriorsClassParentSon=new double[m_NumClasses][m_NumAttValues[parent]][m_NumAttValues[son]];

    for(int i=0;i<m_NumClasses;i++){
      PriorsClass[i]=m_ClassCounts[i]/m_NumInstances;
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[son];j++){
        PriorsClassSon[i][j]=m_ClassAttAttCounts[i][sIndex+j][sIndex+j]/m_NumInstances;
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        PriorsClassParent[i][j]=m_ClassAttAttCounts[i][pIndex+j][pIndex+j]/m_NumInstances;
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        for(int k=0;k<m_NumAttValues[son];k++){
          PriorsClassParentSon[i][j][k]=m_ClassAttAttCounts[i][pIndex+j][sIndex+k]/m_NumInstances;
        }
      }
    }

    for(int i=0;i<m_NumClasses;i++){
      for(int j=0;j<m_NumAttValues[parent];j++){
        for(int k=0;k<m_NumAttValues[son];k++){
          CondiMutualInfo+=PriorsClassParentSon[i][j][k]*log2(PriorsClassParentSon[i][j][k]*PriorsClass[i],PriorsClassParent[i][j]*PriorsClassSon[i][k]);
        }
      }
    }
    return CondiMutualInfo;
  }

  /**
   * compute the logarithm whose base is 2.
   *
   * @param x numerator of the fraction.
   * @param y denominator of the fraction.
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
    double condiMutualInfoSum;

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
      probs[classVal]=(m_ClassCounts[classVal]+1.0/m_NumClasses)/(m_NumInstances+1.0);
      for(int son = 0; son < m_NumAttributes; son++) {
        if(attIndex[son]==-1) continue;
        sIndex=attIndex[son];
        attIndex[son]=-1;
        prob=0;
        condiMutualInfoSum=0;
        for(int parent=0; parent<m_NumAttributes; parent++) {
          if(attIndex[parent]==-1) continue;
          condiMutualInfoSum+=m_condiMutualInfo[son][parent];
          prob+=m_condiMutualInfo[son][parent]*(m_ClassAttAttCounts[classVal][attIndex[parent]][sIndex]+1.0/m_NumAttValues[son])/(m_ClassAttAttCounts[classVal][attIndex[parent]][attIndex[parent]] + 1.0);
        }
        if(condiMutualInfoSum>0){
          prob=prob/condiMutualInfoSum;
          probs[classVal] *= prob;
        }
        else{
          prob=(m_ClassAttAttCounts[classVal][sIndex][sIndex]+1.0/m_NumAttValues[son])/(m_ClassCounts[classVal]+1.0);
          probs[classVal]*= prob;
        }
        attIndex[son] = sIndex;
      }
    }
    Utils.normalize(probs);
    return probs;
  }

  /**
   * returns a string representation of the classifier
   * 
   * @return a representation of the classifier
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
