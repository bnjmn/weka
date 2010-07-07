/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    WAODE.java
 *    Copyright 2006 Liangxiao Jiang
 */

package weka.classifiers.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * WAODE contructs the model called Weightily Averaged One-Dependence Estimators.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * L. Jiang, H. Zhang: Weightily Averaged One-Dependence Estimators. In: Proceedings of the 9th Biennial Pacific Rim International Conference on Artificial Intelligence, PRICAI 2006, 970-974, 2006.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Jiang2006,
 *    author = {L. Jiang and H. Zhang},
 *    booktitle = {Proceedings of the 9th Biennial Pacific Rim International Conference on Artificial Intelligence, PRICAI 2006},
 *    pages = {970-974},
 *    series = {LNAI},
 *    title = {Weightily Averaged One-Dependence Estimators},
 *    volume = {4099},
 *    year = {2006}
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
 * <pre> -I
 *  Whether to print some more internals.
 *  (default: no)</pre>
 * 
 <!-- options-end -->
 *
 * @author  Liangxiao Jiang (ljiang@cug.edu.cn)
 * @author  H. Zhang (hzhang@unb.ca)
 * @version $Revision$
 */
public class WAODE 
  extends AbstractClassifier
  implements TechnicalInformationHandler {
  
  /** for serialization */
  private static final long serialVersionUID = 2170978824284697882L;

  /** The number of each class value occurs in the dataset */
  private double[] m_ClassCounts;
  
  /** The number of each attribute value occurs in the dataset */
  private double[] m_AttCounts;
  
  /** The number of two attributes values occurs in the dataset */
  private double[][] m_AttAttCounts;
  
  /** The number of class and two attributes values occurs in the dataset */
  private double[][][] m_ClassAttAttCounts;
  
  /** The number of values for each attribute in the dataset */
  private int[] m_NumAttValues;
  
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
  
  /** The array of mutual information between each attribute and class */
  private double[] m_mutualInformation;
  
  /** the header information of the training data */
  private Instances m_Header = null;
  
  /** whether to print more internals in the toString method
   * @see #toString() */
  private boolean m_Internals = false;

  /** a ZeroR model in case no model can be built from the data */
  private Classifier m_ZeroR;
  
  /**
   * Returns a string describing this classifier
   * 
   * @return 		a description of the classifier suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "WAODE contructs the model called Weightily Averaged One-Dependence "
      + "Estimators.\n\n"
      + "For more information, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      result.add(enm.nextElement());
      
    result.addElement(new Option(
	"\tWhether to print some more internals.\n"
	+ "\t(default: no)",
	"I", 0, "-I"));

    return result.elements();
  }


  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -I
   *  Whether to print some more internals.
   *  (default: no)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    super.setOptions(options);

    setInternals(Utils.getFlag('I', options));
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;

    result = new Vector();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (getInternals())
      result.add("-I");

    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String internalsTipText() {
    return "Prints more internals of the classifier.";
  }

  /** 
   * Sets whether internals about classifier are printed via toString().
   *
   * @param value if internals should be printed
   * @see #toString()
   */
  public void setInternals(boolean value) {
    m_Internals = value;
  }

  /**
   * Gets whether more internals of the classifier are printed.
   *
   * @return true if more internals are printed
   */
  public boolean getInternals() {
    return m_Internals;
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
    result.setValue(Field.AUTHOR, "L. Jiang and H. Zhang");
    result.setValue(Field.TITLE, "Weightily Averaged One-Dependence Estimators");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 9th Biennial Pacific Rim International Conference on Artificial Intelligence, PRICAI 2006");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.PAGES, "970-974");
    result.setValue(Field.SERIES, "LNAI");
    result.setValue(Field.VOLUME, "4099");

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

    // class
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }
  
  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    
    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // only class? -> build ZeroR model
    if (instances.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data!), "
	  + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(instances);
      return;
    }
    else {
      m_ZeroR = null;
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
    // each attribute and the total number of values for all attributes (not including class).
    for (int i = 0; i < m_NumAttributes; i++) {
      if (i != m_ClassIndex) {
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
    m_AttCounts = new double[m_TotalAttValues];
    m_AttAttCounts = new double[m_TotalAttValues][m_TotalAttValues];
    m_ClassAttAttCounts = new double[m_NumClasses][m_TotalAttValues][m_TotalAttValues];
    m_Header = new Instances(instances, 0);
    
    // Calculate the counts
    for (int k = 0; k < m_NumInstances; k++) {
      int classVal=(int)instances.instance(k).classValue();
      m_ClassCounts[classVal] ++;
      int[] attIndex = new int[m_NumAttributes];
      for (int i = 0; i < m_NumAttributes; i++) {
	if (i == m_ClassIndex){
	  attIndex[i] = -1;
	}
	else{
	  attIndex[i] = m_StartAttIndex[i] + (int)instances.instance(k).value(i);
	  m_AttCounts[attIndex[i]]++;
	}
      }
      for (int Att1 = 0; Att1 < m_NumAttributes; Att1++) {
	if (attIndex[Att1] == -1) continue;
	for (int Att2 = 0; Att2 < m_NumAttributes; Att2++) {
	  if ((attIndex[Att2] != -1)) {
	    m_AttAttCounts[attIndex[Att1]][attIndex[Att2]] ++;
	    m_ClassAttAttCounts[classVal][attIndex[Att1]][attIndex[Att2]] ++;
	  }
	}
      }
    }
    
    //compute mutual information between each attribute and class
    m_mutualInformation=new double[m_NumAttributes];
    for (int att=0;att<m_NumAttributes;att++){
      if (att == m_ClassIndex) continue;
      m_mutualInformation[att]=mutualInfo(att);
    }
  }
  
  /**
   * Computes mutual information between each attribute and class attribute.
   *
   * @param att is the attribute
   * @return the conditional mutual information between son and parent given class
   */
  private double mutualInfo(int att) {
    
    double mutualInfo=0;
    int attIndex=m_StartAttIndex[att];
    double[] PriorsClass = new double[m_NumClasses];
    double[] PriorsAttribute = new double[m_NumAttValues[att]];
    double[][] PriorsClassAttribute=new double[m_NumClasses][m_NumAttValues[att]];
    
    for (int i=0;i<m_NumClasses;i++){
      PriorsClass[i]=m_ClassCounts[i]/m_NumInstances;
    }
    
    for (int j=0;j<m_NumAttValues[att];j++){
      PriorsAttribute[j]=m_AttCounts[attIndex+j]/m_NumInstances;
    }
    
    for (int i=0;i<m_NumClasses;i++){
      for (int j=0;j<m_NumAttValues[att];j++){
	PriorsClassAttribute[i][j]=m_ClassAttAttCounts[i][attIndex+j][attIndex+j]/m_NumInstances;
      }
    }
    
    for (int i=0;i<m_NumClasses;i++){
      for (int j=0;j<m_NumAttValues[att];j++){
	mutualInfo+=PriorsClassAttribute[i][j]*log2(PriorsClassAttribute[i][j],PriorsClass[i]*PriorsAttribute[j]);
      }
    }
    return mutualInfo;
  }
  
  /**
   * compute the logarithm whose base is 2.
   *
   * @param x numerator of the fraction.
   * @param y denominator of the fraction.
   * @return the natual logarithm of this fraction.
   */
  private double log2(double x,double y){
    
    if (x < Utils.SMALL || y < Utils.SMALL)
      return 0.0;
    else
      return Math.log(x/y)/Math.log(2);
  }
  
  /**
   * Calculates the class membership probabilities for the given test instance
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if there is a problem generating the prediction
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(instance);
    }
    
    //Definition of local variables
    double[] probs = new double[m_NumClasses];
    double prob;
    double mutualInfoSum;
    
    // store instance's att values in an int array
    int[] attIndex = new int[m_NumAttributes];
    for (int att = 0; att < m_NumAttributes; att++) {
      if (att == m_ClassIndex)
	attIndex[att] = -1;
      else
	attIndex[att] = m_StartAttIndex[att] + (int)instance.value(att);
    }
    
    // calculate probabilities for each possible class value
    for (int classVal = 0; classVal < m_NumClasses; classVal++) {
      probs[classVal] = 0;
      prob=1;
      mutualInfoSum=0.0;
      for (int parent = 0; parent < m_NumAttributes; parent++) {
	if (attIndex[parent]==-1) continue;
	prob=(m_ClassAttAttCounts[classVal][attIndex[parent]][attIndex[parent]] + 1.0/(m_NumClasses*m_NumAttValues[parent]))/(m_NumInstances + 1.0);
	for (int son = 0; son < m_NumAttributes; son++) {
	  if (attIndex[son]==-1 || son == parent) continue;
	  prob*=(m_ClassAttAttCounts[classVal][attIndex[parent]][attIndex[son]] + 1.0/m_NumAttValues[son])/(m_ClassAttAttCounts[classVal][attIndex[parent]][attIndex[parent]] + 1.0);
	}
	mutualInfoSum+=m_mutualInformation[parent];
	probs[classVal]+=m_mutualInformation[parent]*prob;
      }
      probs[classVal]/=mutualInfoSum;
    }
    if (!Double.isNaN(Utils.sum(probs)))
      Utils.normalize(probs);
    return probs;
  }
  
  /**
   * returns a string representation of the classifier
   * 
   * @return string representation of the classifier
   */
  public String toString() {
    StringBuffer	result;
    String		classname;
    int			i;
    
    // only ZeroR model?
    if (m_ZeroR != null) {
      result = new StringBuffer();
      result.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      result.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
      result.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      result.append(m_ZeroR.toString());
    }
    else {
      classname = this.getClass().getName().replaceAll(".*\\.", "");
      result    = new StringBuffer();
      result.append(classname + "\n");
      result.append(classname.replaceAll(".", "=") + "\n\n");

      if (m_Header == null) {
	result.append("No Model built yet.\n");
      }
      else {
	if (getInternals()) {
	  result.append("Mutual information of attributes with class attribute:\n");
	  for (i = 0; i < m_Header.numAttributes(); i++) {
	    // skip class
	    if (i == m_Header.classIndex())
	      continue;

	    result.append(
		(i+1) + ". " + m_Header.attribute(i).name() + ": " 
		+ Utils.doubleToString(m_mutualInformation[i], 6) + "\n");
	  }
	}
	else {
	  result.append("Model built successfully.\n");
	}
      }
    }
    
    return result.toString();
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
   * @param argv the commandline options, use -h to list all options
   */
  public static void main(String[] argv) {
    runClassifier(new WAODE(), argv);
  }
}
