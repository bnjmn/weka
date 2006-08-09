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
 *    NNge.java
 *    Copyright (C) 2002 Brent Martin
 *
 */

package weka.classifiers.rules;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;


/**
 <!-- globalinfo-start -->
 * Nearest-neighbor-like algorithm using non-nested generalized exemplars (which are hyperrectangles that can be viewed as if-then rules). For more information, see <br/>
 * <br/>
 * Brent Martin (1995). Instance-Based learning: Nearest Neighbor With Generalization. Hamilton, New Zealand.<br/>
 * <br/>
 * Sylvain Roy (2002). Nearest Neighbor With Generalization. Christchurch, New Zealand.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;mastersthesis{Martin1995,
 *    address = {Hamilton, New Zealand},
 *    author = {Brent Martin},
 *    school = {University of Waikato},
 *    title = {Instance-Based learning: Nearest Neighbor With Generalization},
 *    year = {1995}
 * }
 * 
 * &#64;unpublished{Roy2002,
 *    address = {Christchurch, New Zealand},
 *    author = {Sylvain Roy},
 *    school = {University of Canterbury},
 *    title = {Nearest Neighbor With Generalization},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -G &lt;value&gt;
 *  Number of attempts of generalisation.
 * </pre>
 * 
 * <pre> -I &lt;value&gt;
 *  Number of folder for computing the mutual information.
 * </pre>
 * 
 <!-- options-end -->
 *
 * @author Brent Martin (bim20@cosc.canterbury.ac.nz)
 * @author Sylvain Roy (sro33@student.canterbury.ac.nz)
 * @version $Revision: 1.6 $
 */
public class NNge 
  extends Classifier 
  implements UpdateableClassifier, OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 4084742275553788972L;
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Nearest-neighbor-like algorithm using non-nested generalized exemplars "
      + "(which are hyperrectangles that can be viewed as if-then rules). For more "
      + "information, see \n\n"
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
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.MASTERSTHESIS);
    result.setValue(Field.AUTHOR, "Brent Martin");
    result.setValue(Field.YEAR, "1995");
    result.setValue(Field.TITLE, "Instance-Based learning: Nearest Neighbor With Generalization");
    result.setValue(Field.SCHOOL, "University of Waikato");
    result.setValue(Field.ADDRESS, "Hamilton, New Zealand");
    
    additional = result.add(Type.UNPUBLISHED);
    additional.setValue(Field.AUTHOR, "Sylvain Roy");
    additional.setValue(Field.YEAR, "2002");
    additional.setValue(Field.TITLE, "Nearest Neighbor With Generalization");
    additional.setValue(Field.SCHOOL, "University of Canterbury");
    additional.setValue(Field.ADDRESS, "Christchurch, New Zealand");
    
    return result;
  }

  /**
   * Implements Exemplar as used by NNge : parallel axis hyperrectangle.
   */
  private class Exemplar 
    extends Instances {
    
    /** for serialization */
    static final long serialVersionUID = 3960180128928697216L;
    
    /** List of all the Exemplar */
    private Exemplar previous = null;
    private Exemplar next = null;
	
    /** List of all the Exemplar with the same class */
    private Exemplar previousWithClass = null;
    private Exemplar nextWithClass = null;

    /** The NNge which owns this Exemplar */
    private NNge m_NNge;

    /** class of the Exemplar */
    private double m_ClassValue;

    /** Number of correct prediction for this examplar */
    private int m_PositiveCount = 1;
    
    /** Number of incorrect prediction for this examplar */
    private int m_NegativeCount = 0;

    /** The max borders of the rectangle for numeric attributes */
    private double[] m_MaxBorder;
	                     
    /** The min borders of the rectangle for numeric attributes */
    private double[] m_MinBorder;
	                     
    /** The ranges of the hyperrectangle for nominal attributes */
    private boolean[][] m_Range;
	                     
    /** the arrays used by preGeneralise */
    private double[] m_PreMaxBorder = null;
    private double[] m_PreMinBorder = null;
    private boolean[][] m_PreRange = null;
    private Instance m_PreInst = null;


    /**
     * Build a new empty Exemplar
     *
     * @param nnge the classifier which owns this Exemplar
     * @param inst the instances from which the header information is to be taken
     * @param size the capacity of the Exemplar
     * @param classV the class of the Exemplar
     */
    private Exemplar (NNge nnge, Instances inst, int size, double classV){

      super(inst, size);
      m_NNge = nnge;
      m_ClassValue = classV;
      m_MinBorder = new double[numAttributes()];
      m_MaxBorder = new double[numAttributes()];
      m_Range = new boolean[numAttributes()][];
      for(int i = 0; i < numAttributes(); i++){
	if(attribute(i).isNumeric()){
	  m_MinBorder[i] = Double.POSITIVE_INFINITY;
	  m_MaxBorder[i] = Double.NEGATIVE_INFINITY;
	  m_Range[i] = null;
	} else {
	  m_MinBorder[i] = Double.NaN;
	  m_MaxBorder[i] = Double.NaN;
	  m_Range[i] = new boolean[attribute(i).numValues() + 1];
	  for(int j = 0; j < attribute(i).numValues() + 1; j++){
	    m_Range[i][j] = false;
	  }
	}
      }
    }


    /**
     * Generalise the Exemplar with inst
     *
     * @param inst the new example used for the generalisation
     * @throws Exception if either the class of inst is not equal to the class of the Exemplar or inst misses a value.
     */
    private void generalise(Instance inst) throws Exception {

      if(m_ClassValue != inst.classValue())
	throw new Exception("Exemplar.generalise : Incompatible instance's class.");

      add(inst);

      /* extends each range in order to cover inst */
      for(int i = 0; i < numAttributes(); i++){
	 
	if(inst.isMissing(i))
	  throw new Exception("Exemplar.generalise : Generalisation with missing feature impossible.");

	if(i == classIndex())
	  continue;
	    
	if(attribute(i).isNumeric()){
	  if(m_MaxBorder[i] < inst.value(i)) 
	    m_MaxBorder[i] = inst.value(i);
	  if(inst.value(i) < m_MinBorder[i]) 
	    m_MinBorder[i] = inst.value(i);  
		
	} else {
	  m_Range[i][(int) inst.value(i)] = true;
	}
      }
    } 


    /**
     * pre-generalise the Exemplar with inst
     * i.e. the boundaries of the Exemplar include inst but the Exemplar still doesn't 'own' inst.
     * To be complete, the generalisation must be validated with validateGeneralisation.
     * the generalisation can be canceled with cancelGeneralisation.
     * @param inst the new example used for the generalisation
     * @throws Exception if either the class of inst is not equal to the class of the Exemplar or inst misses a value.
     */
    private void preGeneralise(Instance inst) throws Exception {
	
      if(m_ClassValue != inst.classValue())
	throw new Exception("Exemplar.preGeneralise : Incompatible instance's class.");

      m_PreInst = inst;

      /* save the current state */
      m_PreRange = new boolean[numAttributes()][];
      m_PreMinBorder = new double[numAttributes()];
      m_PreMaxBorder = new double[numAttributes()];
      for(int i = 0; i < numAttributes(); i++){
	if(attribute(i).isNumeric()){
	  m_PreMinBorder[i] = m_MinBorder[i];
	  m_PreMaxBorder[i] = m_MaxBorder[i];
	} else {
	  m_PreRange[i] = new boolean[attribute(i).numValues() + 1];
	  for(int j = 0; j < attribute(i).numValues() + 1; j++){
	    m_PreRange[i][j] = m_Range[i][j];
	  }
	}
      }

      /* perform the pre-generalisation */
      for(int i = 0; i < numAttributes(); i++){
	if(inst.isMissing(i))
	  throw new Exception("Exemplar.preGeneralise : Generalisation with missing feature impossible.");
	if(i == classIndex())
	  continue;
	if(attribute(i).isNumeric()){
	  if(m_MaxBorder[i] < inst.value(i)) 
	    m_MaxBorder[i] = inst.value(i);
	  if(inst.value(i) < m_MinBorder[i]) 
	    m_MinBorder[i] = inst.value(i);  
	} else {
	  m_Range[i][(int) inst.value(i)] = true;
	}
      }
    }


    /**
     * Validates a generalisation started with preGeneralise.
     * Watch out, preGeneralise must have been called before.
     *
     * @throws Exception is thrown if preGeneralise hasn't been called before
     */
    private void validateGeneralisation() throws Exception {
      if(m_PreInst == null){
	throw new Exception("Exemplar.validateGeneralisation : validateGeneralisation called without previous call to preGeneralise!");
      }
      add(m_PreInst);
      m_PreRange = null;
      m_PreMinBorder = null;
      m_PreMaxBorder = null;
    }

    
    /**
     * Cancels a generalisation started with preGeneralise.
     * Watch out, preGeneralise must have been called before.
     *
     * @throws Exception is thrown if preGeneralise hasn't been called before
     */
    private void cancelGeneralisation() throws Exception {
      if(m_PreInst == null){
	throw new Exception("Exemplar.cancelGeneralisation : cancelGeneralisation called without previous call to preGeneralise!");
      }
      m_PreInst = null;
      m_Range = m_PreRange;
      m_MinBorder = m_PreMinBorder;
      m_MaxBorder = m_PreMaxBorder;
      m_PreRange = null;
      m_PreMinBorder = null;
      m_PreMaxBorder = null;
    }


    /**
     * return true if inst is held by this Exemplar, false otherwise
     *
     * @param inst an Instance
     * @return true if inst is held by this hyperrectangle, false otherwise
     */
    private boolean holds(Instance inst) {
	
      if(numInstances() == 0)
	return false;

      for(int i = 0; i < numAttributes(); i++){
	if(i != classIndex() && !holds(i, inst.value(i)))
	  return false;
      }
      return true;
    }


    /**
     * return true if value is inside the Exemplar along the attrIndex attribute.
     *
     * @param attrIndex the index of an attribute 
     * @param value a value along the attrIndexth attribute
     * @return true if value is inside the Exemplar along the attrIndex attribute.
     */
    private boolean holds(int attrIndex, double value) {
	
      if (numAttributes() == 0)
	return false;
	
      if(attribute(attrIndex).isNumeric())
	return(m_MinBorder[attrIndex] <= value && value <= m_MaxBorder[attrIndex]);
      else
	return m_Range[attrIndex][(int) value];
    }


    /**
     * Check if the Examplar overlaps ex
     *
     * @param ex an Exemplar
     * @return true if ex is overlapped by the Exemplar
     * @throws Exception
     */
    private boolean overlaps(Exemplar ex) {

      if(ex.isEmpty() || isEmpty())
	return false;

      for (int i = 0; i < numAttributes(); i++){
	    
	if(i == classIndex()){
	  continue;
	}
	if (attribute(i).isNumeric() && 
	    (ex.m_MaxBorder[i] < m_MinBorder[i] || ex.m_MinBorder[i] > m_MaxBorder[i])){
	  return false;
	}
	if (attribute(i).isNominal()) {
	  boolean in = false;
	  for (int j = 0; j < attribute(i).numValues() + 1; j++){
	    if(m_Range[i][j] && ex.m_Range[i][j]){
	      in = true;
	      break;
	    }
	  }
	  if(!in) return false;
	}
      }
      return true;
    }


    /** 
     * Compute the distance between the projection of inst and this Exemplar along the attribute attrIndex.
     * If inst misses its value along the attribute, the function returns 0.
     *
     * @param inst an instance
     * @param attrIndex the index of the attribute 
     * @return the distance between the projection of inst and this Exemplar along the attribute attrIndex.
     */
    private double attrDistance(Instance inst, int attrIndex) {

      if(inst.isMissing(attrIndex))
	return 0;

      /* numeric attribute */
      if(attribute(attrIndex).isNumeric()){

	double norm = m_NNge.m_MaxArray[attrIndex] - m_NNge.m_MinArray[attrIndex];
	if(norm <= 0)
	  norm = 1;

	if (m_MaxBorder[attrIndex] < inst.value(attrIndex)) {
	  return (inst.value(attrIndex) - m_MaxBorder[attrIndex]) / norm;
	} else if (inst.value(attrIndex) < m_MinBorder[attrIndex]) {
	  return (m_MinBorder[attrIndex] - inst.value(attrIndex)) / norm;
	} else {
	  return 0;
	}

	/* nominal attribute */
      } else {
	if(holds(attrIndex, inst.value(attrIndex))){
	  return 0;
	} else {
	  return 1;
	}
      }
    }


    /**
     * Returns the square of the distance between inst and the Exemplar. 
     * 
     * @param inst an instance
     * @return the squared distance between inst and the Exemplar.
     */
    private double squaredDistance(Instance inst) {
	
      double sum = 0, term;
      int numNotMissingAttr = 0;
      for(int i = 0; i < inst.numAttributes(); i++){
	    
	if(i == classIndex())
	  continue;
	    
	term = m_NNge.attrWeight(i) * attrDistance(inst, i);
	term = term * term;
	sum += term;

	if (!inst.isMissing(i))
	  numNotMissingAttr++;

      }
	
      if(numNotMissingAttr == 0){
	return 0;
      } else {
	return sum / (double) (numNotMissingAttr * numNotMissingAttr);
      }
    }


    /**
     * Return the weight of the Examplar
     *
     * @return the weight of the Examplar.
     */
    private double weight(){
      return ((double) (m_PositiveCount + m_NegativeCount)) / ((double) m_PositiveCount);
    }


    /**
     * Return the class of the Exemplar
     *
     * @return the class of this exemplar as a double (weka format)
     */
    private double classValue(){
      return m_ClassValue;
    }


    /**
     * Returns the value of the inf border of the Exemplar. 
     *
     * @param attrIndex the index of the attribute
     * @return the value of the inf border for this attribute
     * @throws Exception is thrown either if the attribute is nominal or if the Exemplar is empty
     */
    private double getMinBorder(int attrIndex) throws Exception {
      if(!attribute(attrIndex).isNumeric())
	throw new Exception("Exception.getMinBorder : not numeric attribute !");
      if(numInstances() == 0)
	throw new Exception("Exception.getMinBorder : empty Exemplar !");
      return m_MinBorder[attrIndex];
    }


    /**
     * Returns the value of the sup border of the hyperrectangle
     * Returns NaN if the HyperRectangle doesn't have any border for this attribute 
     *
     * @param attrIndex the index of the attribute
     * @return the value of the sup border for this attribute
     * @throws Exception is thrown either if the attribute is nominal or if the Exemplar is empty
     */
    private double getMaxBorder(int attrIndex) throws Exception {
      if(!attribute(attrIndex).isNumeric())
	throw new Exception("Exception.getMaxBorder : not numeric attribute !");
      if(numInstances() == 0)
	throw new Exception("Exception.getMaxBorder : empty Exemplar !");
      return m_MaxBorder[attrIndex];
    }


    /**
     * Returns the number of positive classifications
     *
     * @return the number of positive classifications
     */
    private int getPositiveCount(){
      return m_PositiveCount;
    }


    /**
     * Returns the number of negative classifications
     *
     * @return the number of negative classifications
     */
    private int getNegativeCount(){
      return m_NegativeCount;
    }


    /**
     * Set the number of positive classifications
     *
     * @param value an integer value (greater than 0 is wise...)
     */
    private void setPositiveCount(int value) {
      m_PositiveCount = value;
    }


    /**
     * Set the number of negative classifications
     *
     * @param value an integer value
     */
    private void setNegativeCount(int value) {
      m_NegativeCount = value;
    }


    /**
     * Increment the number of positive Classifications
     */
    private void incrPositiveCount(){
      m_PositiveCount++;
    }


    /**
     * Increment the number of negative Classifications
     */
    private void incrNegativeCount(){
      m_NegativeCount++;
    }


    /**
     * Returns true if the Exemplar is empty (i.e. doesn't yield any Instance)
     *
     * @return true if the Exemplar is empty, false otherwise
     */
    private boolean isEmpty(){
      return (numInstances() == 0);
    }

    
    /**
     * Returns a description of this Exemplar
     *
     * @return A string that describes this Exemplar
     */
    private String toString2(){
      String s;
      Enumeration enu = null;
      s = "Exemplar[";
      if (numInstances() == 0) {
	return s + "Empty]";
      }
      s += "{";
      enu = enumerateInstances();
      while(enu.hasMoreElements()){
	s = s + "<" + enu.nextElement().toString() + "> ";
      }
      s = s.substring(0, s.length()-1);
      s = s + "} {" + toRules() + "} p=" + m_PositiveCount + " n=" + m_NegativeCount + "]";
      return s;
    }


    /**
     * Returns a string of the rules induced by this examplar
     *
     * @return a string of the rules induced by this examplar
     */
    private String toRules(){

      if (numInstances() == 0)
	return "No Rules (Empty Exemplar)";

      String s = "", sep = "";
	
      for(int i = 0; i < numAttributes(); i++){
	    
	if(i == classIndex())
	  continue;
	    
	if(attribute(i).isNumeric()){
	  if(m_MaxBorder[i] != m_MinBorder[i]){
	    s += sep + m_MinBorder[i] + "<=" + attribute(i).name() + "<=" + m_MaxBorder[i];
	  } else {
	    s += sep + attribute(i).name() + "=" + m_MaxBorder[i];
	  }
	  sep = " ^ ";
	    
	} else {
	  s += sep + attribute(i).name() + " in {";
	  String virg = "";
	  for(int j = 0; j < attribute(i).numValues() + 1; j++){
	    if(m_Range[i][j]){
	      s+= virg;
	      if(j == attribute(i).numValues())
		s += "?";
	      else
		s += attribute(i).value(j);
	      virg = ",";
	    }
	  }
	  s+="}";
	  sep = " ^ ";
	}	    
      }
      s += "  ("+numInstances() +")";
      return s;
    }

  }



  /** An empty instances to keep the headers, the classIndex, etc... */
  private Instances m_Train;

  /** The list of Exemplars */
  private Exemplar m_Exemplars;

  /** The lists of Exemplars by class */
  private Exemplar m_ExemplarsByClass[];

  /** The minimum values for numeric attributes. */
  double [] m_MinArray;

  /** The maximum values for numeric attributes. */
  double [] m_MaxArray;

  /** The number of try for generalisation */
  private int m_NumAttemptsOfGene = 5;

  /** The number of folder for the Mutual Information */
  private int m_NumFoldersMI = 5;

  /** Values to use for missing value */
  private double [] m_MissingVector;

  /** MUTUAL INFORMATION'S DATAS */
  /* numeric attributes */
  private int [][][] m_MI_NumAttrClassInter;
  private int [][] m_MI_NumAttrInter;
  private double [] m_MI_MaxArray;
  private double [] m_MI_MinArray;
  /* nominal attributes */
  private int [][][] m_MI_NumAttrClassValue;
  private int [][] m_MI_NumAttrValue;
  /* both */
  private int [] m_MI_NumClass;
  private int m_MI_NumInst;
  private double [] m_MI;



  /** MAIN FUNCTIONS OF THE CLASSIFIER */


  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Generates a classifier. Must initialize all fields of the classifier
   * that are not being set via options (ie. multiple calls of buildClassifier
   * must always lead to the same result). Must not change the dataset
   * in any way.
   *
   * @param data set of instances serving as training data 
   * @throws Exception if the classifier has not been 
   * generated successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    /* initialize the classifier */

    m_Train = new Instances(data, 0);
    m_Exemplars = null;
    m_ExemplarsByClass = new Exemplar[m_Train.numClasses()];
    for(int i = 0; i < m_Train.numClasses(); i++){
      m_ExemplarsByClass[i] = null;
    }
    m_MaxArray = new double[m_Train.numAttributes()];
    m_MinArray = new double[m_Train.numAttributes()];
    for(int i = 0; i < m_Train.numAttributes(); i++){
      m_MinArray[i] = Double.POSITIVE_INFINITY;
      m_MaxArray[i] = Double.NEGATIVE_INFINITY;
    }

    m_MI_MinArray = new double [data.numAttributes()];
    m_MI_MaxArray = new double [data.numAttributes()];
    m_MI_NumAttrClassInter = new int[data.numAttributes()][][];
    m_MI_NumAttrInter = new int[data.numAttributes()][];
    m_MI_NumAttrClassValue = new int[data.numAttributes()][][];
    m_MI_NumAttrValue = new int[data.numAttributes()][];
    m_MI_NumClass = new int[data.numClasses()];
    m_MI = new double[data.numAttributes()];
    m_MI_NumInst = 0;
    for(int cclass = 0; cclass < data.numClasses(); cclass++)
      m_MI_NumClass[cclass] = 0;
    for (int attrIndex = 0; attrIndex < data.numAttributes(); attrIndex++) {
	    
      if(attrIndex == data.classIndex())
	continue;
	    
      m_MI_MaxArray[attrIndex] = m_MI_MinArray[attrIndex] = Double.NaN;
      m_MI[attrIndex] = Double.NaN;
	    
      if(data.attribute(attrIndex).isNumeric()){
	m_MI_NumAttrInter[attrIndex] = new int[m_NumFoldersMI];
	for(int inter = 0; inter < m_NumFoldersMI; inter++){
	  m_MI_NumAttrInter[attrIndex][inter] = 0;
	}
      } else {
	m_MI_NumAttrValue[attrIndex] = new int[data.attribute(attrIndex).numValues() + 1];
	for(int attrValue = 0; attrValue < data.attribute(attrIndex).numValues() + 1; attrValue++){
	  m_MI_NumAttrValue[attrIndex][attrValue] = 0;
	}
      }
	    
      m_MI_NumAttrClassInter[attrIndex] = new int[data.numClasses()][];
      m_MI_NumAttrClassValue[attrIndex] = new int[data.numClasses()][];

      for(int cclass = 0; cclass < data.numClasses(); cclass++){
	if(data.attribute(attrIndex).isNumeric()){
	  m_MI_NumAttrClassInter[attrIndex][cclass] = new int[m_NumFoldersMI];
	  for(int inter = 0; inter < m_NumFoldersMI; inter++){
	    m_MI_NumAttrClassInter[attrIndex][cclass][inter] = 0;
	  }
	} else if(data.attribute(attrIndex).isNominal()){
	  m_MI_NumAttrClassValue[attrIndex][cclass] = new int[data.attribute(attrIndex).numValues() + 1];		
	  for(int attrValue = 0; attrValue < data.attribute(attrIndex).numValues() + 1; attrValue++){
	    m_MI_NumAttrClassValue[attrIndex][cclass][attrValue] = 0;
	  }
	}
      }
    }
    m_MissingVector = new double[data.numAttributes()];
    for(int i = 0; i < data.numAttributes(); i++){
      if(i == data.classIndex()){
	m_MissingVector[i] = Double.NaN;
      } else {
	m_MissingVector[i] = data.attribute(i).numValues();
      }
    }

    /* update the classifier with data */
    Enumeration enu = data.enumerateInstances();
    while(enu.hasMoreElements()){
      update((Instance) enu.nextElement());
    }	
  }

    
  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class as a double
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    /* check the instance */
    if (m_Train.equalHeaders(instance.dataset()) == false){
      throw new Exception("NNge.classifyInstance : Incompatible instance types !");
    }
	
    Exemplar matched = nearestExemplar(instance); 
    if(matched == null){
      throw new Exception("NNge.classifyInstance : NNge hasn't been trained !");
    }
    return matched.classValue();
  }


  /**
   * Updates the classifier using the given instance.
   *
   * @param instance the instance to include
   * @throws Exception if instance could not be incorporated
   * successfully
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (m_Train.equalHeaders(instance.dataset()) == false) {
      throw new Exception("Incompatible instance types");
    }	
    update(instance);	
  }



  /** HIGH LEVEL SUB-FUNCTIONS */
    


  /**
   * Performs the update of the classifier
   *
   * @param instance the new instance
   * @throws Exception if the update fails
   */
  private void update(Instance instance) throws Exception {

    if (instance.classIsMissing()) {
      return;
    }

    instance.replaceMissingValues(m_MissingVector);
    m_Train.add(instance);

    /* Update the minimum and maximum for all the attributes */
    updateMinMax(instance);

    /* update the mutual information datas */
    updateMI(instance);

    /* Nearest Exemplar */
    Exemplar nearest = nearestExemplar(instance);
	
    /* Adjust */
    if(nearest == null){
      Exemplar newEx = new Exemplar(this, m_Train, 10, instance.classValue());
      newEx.generalise(instance);
      initWeight(newEx);
      addExemplar(newEx);
      return;
    }
    adjust(instance, nearest);

    /* Generalise */
    generalise(instance);
  }


  /**
   * Returns the nearest Exemplar
   *
   * @param inst an Instance
   * @return the nearest Exemplar to inst, null if no exemplar are found.
   */
  private Exemplar nearestExemplar(Instance inst){

    if (m_Exemplars == null)
      return null;
    Exemplar cur = m_Exemplars, nearest = m_Exemplars;
    double dist, smallestDist = cur.squaredDistance(inst);
    while (cur.next != null){
      cur = cur.next;
      dist = cur.squaredDistance(inst);
      if (dist < smallestDist){
	smallestDist = dist;
	nearest = cur;
      }
    }
    return nearest;
  }

    
  /**
   * Returns the nearest Exemplar with class c
   *
   * @param inst an Instance
   * @param c the class of the Exemplar to return
   * @return the nearest Exemplar to inst with class c, null if no exemplar with class c are found.
   */
  private Exemplar nearestExemplar(Instance inst, double c){

    if (m_ExemplarsByClass[(int) c] == null)
      return null;
    Exemplar cur = m_ExemplarsByClass[(int) c], nearest = m_ExemplarsByClass[(int) c];
    double dist, smallestDist = cur.squaredDistance(inst);
    while (cur.nextWithClass != null){
      cur = cur.nextWithClass;
      dist = cur.squaredDistance(inst);
      if (dist < smallestDist){
	smallestDist = dist;
	nearest = cur;
      }
    }
    return nearest;
  }

    
  /**
   * Generalise an Exemplar (not necessarily predictedExemplar) to match instance.
   * predictedExemplar must be in NNge's lists
   *
   * @param newInst the new instance
   * @throws Exception in case of inconsitent situation
   */
  private void generalise(Instance newInst) throws Exception {

    Exemplar first = m_ExemplarsByClass[(int) newInst.classValue()];
    int n = 0;

    /* try to generalise with the n first exemplars */
    while(n < m_NumAttemptsOfGene && first != null){
	    
      /* find the nearest one starting from first */
      Exemplar closest = first, cur = first;
      double smallestDist = first.squaredDistance(newInst), dist;
      while(cur.nextWithClass != null){
	cur = cur.nextWithClass;
	dist = cur.squaredDistance(newInst);
	if(dist < smallestDist){
	  smallestDist = dist;
	  closest = cur;
	}
      }

      /* remove the Examplar from NNge's lists */
      if(closest == first)
	first = first.nextWithClass;
      removeExemplar(closest); 

      /* try to generalise */
      closest.preGeneralise(newInst);
      if(!detectOverlapping(closest)){
	closest.validateGeneralisation();
	addExemplar(closest);
	return;
      }

      /* it didn't work, put ungeneralised exemplar on the top of the lists */
      closest.cancelGeneralisation();
      addExemplar(closest);			

      n++;
    }

    /* generalisation failled : add newInst as a new Examplar */
    Exemplar newEx = new Exemplar(this, m_Train, 5, newInst.classValue());
    newEx.generalise(newInst);
    initWeight(newEx);
    addExemplar(newEx);
  }


  /**
   * Adjust the NNge.
   *
   * @param newInst the instance to classify
   * @param predictedExemplar the Exemplar that matches newInst
   * @throws Exception in case of inconsistent situation
   */
  private void adjust(Instance newInst, Exemplar predictedExemplar) throws Exception {

    /* correct prediction */
    if(newInst.classValue() == predictedExemplar.classValue()){
      predictedExemplar.incrPositiveCount();
      /* incorrect prediction */
    } else {
      predictedExemplar.incrNegativeCount();

      /* new instance falls inside */
      if(predictedExemplar.holds(newInst)){
	prune(predictedExemplar, newInst);
      }
    }    
  }


  /**
   * Prunes an Exemplar that matches an Instance
   *
   * @param predictedExemplar an Exemplar
   * @param newInst an Instance matched by predictedExemplar
   * @throws Exception in case of inconsistent situation. (shouldn't happen.)
   */
  private void prune(Exemplar predictedExemplar, Instance newInst) throws Exception {

    /* remove the Exemplar */
    removeExemplar(predictedExemplar);

    /* look for the best nominal feature and the best numeric feature to cut */
    int numAttr = -1, nomAttr = -1;
    double smallestDelta = Double.POSITIVE_INFINITY, delta;
    int biggest_N_Nom = -1, biggest_N_Num = -1, n, m;
    for(int i = 0; i < m_Train.numAttributes(); i++){

      if(i == m_Train.classIndex())
	continue;

      /* numeric attribute */
      if(m_Train.attribute(i).isNumeric()){

	/* compute the distance 'delta' to the closest boundary */
	double norm = m_MaxArray[i] - m_MinArray[i];
	if(norm != 0){
	  delta = Math.min((predictedExemplar.getMaxBorder(i) - newInst.value(i)), 
			   (newInst.value(i) - predictedExemplar.getMinBorder(i))) / norm;
	} else {
	  delta = Double.POSITIVE_INFINITY;
	}

	/* compute the size of the biggest Exemplar which would be created */
	n = m = 0;
	Enumeration enu = predictedExemplar.enumerateInstances();
	while(enu.hasMoreElements()){
	  Instance ins = (Instance) enu.nextElement();
	  if(ins.value(i) < newInst.value(i))
	    n++;
	  else if(ins.value(i) > newInst.value(i))
	    m++;
	}
	n = Math.max(n, m);

	if(delta < smallestDelta){
	  smallestDelta = delta;
	  biggest_N_Num = n;
	  numAttr = i;
	} else if(delta == smallestDelta && n > biggest_N_Num){
	  biggest_N_Num = n;
	  numAttr = i;
	}

	/* nominal attribute */
      } else {

	/* compute the size of the Exemplar which would be created */
	Enumeration enu = predictedExemplar.enumerateInstances();
	n = 0;
	while(enu.hasMoreElements()){
	  if(((Instance) enu.nextElement()).value(i) != newInst.value(i))
	    n++;
	}
	if(n > biggest_N_Nom){
	  biggest_N_Nom = n;
	  nomAttr = i;
	} 
      }
    }

    /* selection of the feature to cut between the best nominal and the best numeric */
    int attrToCut;
    if(numAttr == -1 && nomAttr == -1){
      attrToCut = 0;
    } else if (numAttr == -1){
      attrToCut = nomAttr;
    } else if(nomAttr == -1){
      attrToCut = numAttr;
    } else {
      if(biggest_N_Nom > biggest_N_Num)
	attrToCut = nomAttr;
      else
	attrToCut = numAttr;
    }

    /* split the Exemplar */
    Instance curInst;
    Exemplar a, b;
    a = new Exemplar(this, m_Train, 10, predictedExemplar.classValue());
    b = new Exemplar(this, m_Train, 10, predictedExemplar.classValue());
    LinkedList leftAlone = new LinkedList();
    Enumeration enu = predictedExemplar.enumerateInstances();
    if(m_Train.attribute(attrToCut).isNumeric()){
      while(enu.hasMoreElements()){
	curInst = (Instance) enu.nextElement();
	if(curInst.value(attrToCut) > newInst.value(attrToCut)){
	  a.generalise(curInst);
	} else if (curInst.value(attrToCut) < newInst.value(attrToCut)){
	  b.generalise(curInst);
	} else if (notEqualFeatures(curInst, newInst)) {
	  leftAlone.add(curInst);
	}
      }
    } else {
      while(enu.hasMoreElements()){
	curInst = (Instance) enu.nextElement();
	if(curInst.value(attrToCut) != newInst.value(attrToCut)){
	  a.generalise(curInst);
	} else if (notEqualFeatures(curInst, newInst)){
	  leftAlone.add(curInst);
	}
      }
    }
	
    /* treat the left alone Instances */
    while(leftAlone.size() != 0){

      Instance alone = (Instance) leftAlone.removeFirst();
      a.preGeneralise(alone);
      if(!a.holds(newInst)){
	a.validateGeneralisation();
	continue;
      }
      a.cancelGeneralisation();
      b.preGeneralise(alone);
      if(!b.holds(newInst)){
	b.validateGeneralisation();
	continue;
      }
      b.cancelGeneralisation();
      Exemplar exem = new Exemplar(this, m_Train, 3, alone.classValue());
      exem.generalise(alone);
      initWeight(exem);
      addExemplar(exem);
    }

    /* add (or not) the new Exemplars */
    if(a.numInstances() != 0){
      initWeight(a);
      addExemplar(a);
    }
    if(b.numInstances() != 0){
      initWeight(b);
      addExemplar(b);	    
    }
  }


  /**
   * Returns true if the instance don't have the same feature values
   * 
   * @param inst1 an instance
   * @param inst2 an instance
   * @return true if the instance don't have the same feature values
   */
  private boolean notEqualFeatures(Instance inst1, Instance inst2) {

    for(int i = 0; i < m_Train.numAttributes(); i++){
      if(i == m_Train.classIndex())
	continue;
      if(inst1.value(i) != inst2.value(i))
	return true;
    }
    return false;
  }


  /**
   * Returns true if ex overlaps any of the Exemplars in NNge's lists
   *
   * @param ex an Exemplars
   * @return true if ex overlaps any of the Exemplars in NNge's lists
   */
  private boolean detectOverlapping(Exemplar ex){
    Exemplar cur = m_Exemplars;
    while(cur != null){
      if(ex.overlaps(cur)){
	return true;
      }
      cur = cur.next;
    }
    return false;
  }


  /**
   * Updates the minimum, maximum, sum, sumSquare values for all the attributes 
   * 
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance){

    for (int j = 0; j < m_Train.numAttributes(); j++) {
      if(m_Train.classIndex() == j || m_Train.attribute(j).isNominal())
	continue;
      if (instance.value(j) < m_MinArray[j]) 
	m_MinArray[j] = instance.value(j);
      if (instance.value(j) > m_MaxArray[j])
	m_MaxArray[j] = instance.value(j);
    }    
  }


  /**
   * Updates the data for computing the mutual information
   *
   * MUST be called AFTER adding inst in m_Train 
   *
   * @param inst the new instance
   * @throws Exception is thrown if an inconsistent situation is met
   */
  private void updateMI(Instance inst) throws Exception {

    if(m_NumFoldersMI < 1){
      throw new Exception("NNge.updateMI : incorrect number of folders ! Option I must be greater than 1.");
    }

    m_MI_NumClass[(int) inst.classValue()]++;
    m_MI_NumInst++;

    /* for each attribute */
    for(int attrIndex = 0; attrIndex < m_Train.numAttributes(); attrIndex++){

      /* which is the class attribute */
      if(m_Train.classIndex() == attrIndex)
	continue;

      /* which is a numeric attribute */
      else if(m_Train.attribute(attrIndex).isNumeric()){
		
	/* if max-min have to be updated */
	if(Double.isNaN(m_MI_MaxArray[attrIndex]) ||
	   Double.isNaN(m_MI_MinArray[attrIndex]) ||
	   m_MI_MaxArray[attrIndex] < inst.value(attrIndex) || 
	   inst.value(attrIndex) < m_MI_MinArray[attrIndex]){

	  /* then update them */
	  if(Double.isNaN(m_MI_MaxArray[attrIndex])) m_MI_MaxArray[attrIndex] = inst.value(attrIndex);
	  if(Double.isNaN(m_MI_MinArray[attrIndex])) m_MI_MinArray[attrIndex] = inst.value(attrIndex);
	  if(m_MI_MaxArray[attrIndex] < inst.value(attrIndex)) m_MI_MaxArray[attrIndex] = inst.value(attrIndex);
	  if(m_MI_MinArray[attrIndex] > inst.value(attrIndex)) m_MI_MinArray[attrIndex] = inst.value(attrIndex);
		    
	  /* and re-compute everything from scratch... (just for this attribute) */
	  double delta = (m_MI_MaxArray[attrIndex] - m_MI_MinArray[attrIndex]) / (double) m_NumFoldersMI;

	  /* for each interval */
	  for(int inter = 0; inter < m_NumFoldersMI; inter++){

	    m_MI_NumAttrInter[attrIndex][inter] = 0;

	    /* for each class */
	    for(int cclass = 0; cclass < m_Train.numClasses(); cclass++){
			    
	      m_MI_NumAttrClassInter[attrIndex][cclass][inter] = 0;

	      /* count */
	      Enumeration enu = m_Train.enumerateInstances();
	      while(enu.hasMoreElements()){
		Instance cur = (Instance) enu.nextElement();
		if(( (m_MI_MinArray[attrIndex] + inter * delta) <= cur.value(attrIndex)       ) &&
		   ( cur.value(attrIndex) <= (m_MI_MinArray[attrIndex] + (inter + 1) * delta) ) &&
		   ( cur.classValue() == cclass ) ){
		  m_MI_NumAttrInter[attrIndex][inter]++;
		  m_MI_NumAttrClassInter[attrIndex][cclass][inter]++;
		}
	      }
	    }
	  }
		
	  /* max-min don't have to be updated */
	} else {

	  /* still have to incr the card of the correct interval */
	  double delta = (m_MI_MaxArray[attrIndex] - m_MI_MinArray[attrIndex]) / (double) m_NumFoldersMI;
		    
	  /* for each interval */
	  for(int inter = 0; inter < m_NumFoldersMI; inter++){
	    /* which contains inst*/
	    if(( (m_MI_MinArray[attrIndex] + inter * delta) <= inst.value(attrIndex)       ) &&
	       ( inst.value(attrIndex) <= (m_MI_MinArray[attrIndex] + (inter + 1) * delta) )){
	      m_MI_NumAttrInter[attrIndex][inter]++;
	      m_MI_NumAttrClassInter[attrIndex][(int) inst.classValue()][inter]++;
	    }
	  }
	}
		
	/* update the mutual information of this attribute... */
	m_MI[attrIndex] = 0;
		
	/* for each interval, for each class */
	for(int inter = 0; inter < m_NumFoldersMI; inter++){
	  for(int cclass = 0; cclass < m_Train.numClasses(); cclass++){
	    double pXY = ((double) m_MI_NumAttrClassInter[attrIndex][cclass][inter]) / ((double) m_MI_NumInst);
	    double pX = ((double) m_MI_NumClass[cclass]) / ((double) m_MI_NumInst);
	    double pY = ((double) m_MI_NumAttrInter[attrIndex][inter]) / ((double) m_MI_NumInst);

	    if(pXY != 0)
	      m_MI[attrIndex] += pXY * Utils.log2(pXY / (pX * pY));
	  }
	}
		
	/* which is a nominal attribute */
      } else if (m_Train.attribute(attrIndex).isNominal()){
		
	/*incr the card of the correct 'values' */
	m_MI_NumAttrValue[attrIndex][(int) inst.value(attrIndex)]++;
	m_MI_NumAttrClassValue[attrIndex][(int) inst.classValue()][(int) inst.value(attrIndex)]++;
		
	/* update the mutual information of this attribute... */
	m_MI[attrIndex] = 0;
		
	/* for each nominal value, for each class */
	for(int attrValue = 0; attrValue < m_Train.attribute(attrIndex).numValues() + 1; attrValue++){
	  for(int cclass = 0; cclass < m_Train.numClasses(); cclass++){
	    double pXY = ((double) m_MI_NumAttrClassValue[attrIndex][cclass][attrValue]) / ((double) m_MI_NumInst);
	    double pX = ((double) m_MI_NumClass[cclass]) / ((double) m_MI_NumInst);
	    double pY = ((double) m_MI_NumAttrValue[attrIndex][attrValue]) / ((double) m_MI_NumInst);
	    if(pXY != 0)
	      m_MI[attrIndex] += pXY * Utils.log2(pXY / (pX * pY));
	  }
	}

	/* not a nominal attribute, not a numeric attribute */
      } else {
	throw new Exception("NNge.updateMI : Cannot deal with 'string attribute'.");
      }
    }	
  }


  /**
   * Init the weight of ex
   * Watch out ! ex shouldn't be in NNge's lists when initialized
   *
   * @param ex the Exemplar to initialise
   */
  private void initWeight(Exemplar ex) {	
    int pos = 0, neg = 0, n = 0;
    Exemplar cur = m_Exemplars;
    if (cur == null){
      ex.setPositiveCount(1);
      ex.setNegativeCount(0);
      return;
    }
    while(cur != null){
      pos += cur.getPositiveCount();
      neg += cur.getNegativeCount();
      n++;
      cur = cur.next;
    }
    ex.setPositiveCount(pos / n);
    ex.setNegativeCount(neg / n);
  }


  /**
   * Adds an Exemplar in NNge's lists
   * Ensure that the exemplar is not already in a list : the links would be broken...
   *
   * @param ex a new Exemplar to add
   */
  private void addExemplar(Exemplar ex) {
	
    /* add ex at the top of the general list */
    ex.next = m_Exemplars;
    if(m_Exemplars != null)
      m_Exemplars.previous = ex;
    ex.previous = null;
    m_Exemplars = ex;

    /* add ex at the top of the corresponding class list */
    ex.nextWithClass = m_ExemplarsByClass[(int) ex.classValue()];
    if(m_ExemplarsByClass[(int) ex.classValue()] != null)
      m_ExemplarsByClass[(int) ex.classValue()].previousWithClass = ex;
    ex.previousWithClass = null;
    m_ExemplarsByClass[(int) ex.classValue()] = ex;
  }


  /**
   * Removes an Exemplar from NNge's lists
   * Ensure that the Exemplar is actually in NNge's lists. 
   *   Likely to do something wrong if this condition is not respected.
   * Due to the list implementation, the Exemplar can appear only once in the lists : 
   *   once removed, the exemplar is not in the lists anymore.
   *
   * @param ex a new Exemplar to add
   */
  private void removeExemplar(Exemplar ex){

    /* remove from the general list */
    if(m_Exemplars == ex){
      m_Exemplars = ex.next;
      if(m_Exemplars != null)
	m_Exemplars.previous = null;
	
    } else {
      ex.previous.next = ex.next;
      if(ex.next != null){
	ex.next.previous = ex.previous;
      }
    }
    ex.next = ex.previous = null;

    /* remove from the class list */
    if(m_ExemplarsByClass[(int) ex.classValue()] == ex){
      m_ExemplarsByClass[(int) ex.classValue()] = ex.nextWithClass;
      if(m_ExemplarsByClass[(int) ex.classValue()] != null)
	m_ExemplarsByClass[(int) ex.classValue()].previousWithClass = null;
	
    } else {
      ex.previousWithClass.nextWithClass = ex.nextWithClass;
      if(ex.nextWithClass != null){
	ex.nextWithClass.previousWithClass = ex.previousWithClass;
      }
    }
    ex.nextWithClass = ex.previousWithClass = null;
  }


  /**
   * returns the weight of indexth attribute
   *
   * @param index attribute's index
   * @return the weight of indexth attribute
   */
  private double attrWeight (int index) {
    return m_MI[index];
  }

    
  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString(){

    String s;
    Exemplar cur = m_Exemplars;
    int i;	

   if (m_MinArray == null) {
      return "No classifier built";
    }
     int[] nbHypClass = new int[m_Train.numClasses()];
    int[] nbSingleClass = new int[m_Train.numClasses()];
    for(i = 0; i<nbHypClass.length; i++){
      nbHypClass[i] = 0;
      nbSingleClass[i] = 0;
    }
    int nbHyp = 0, nbSingle = 0;

    s = "\nNNGE classifier\n\nRules generated :\n";

    while(cur != null){
      s += "\tclass " + m_Train.attribute(m_Train.classIndex()).value((int) cur.classValue()) + " IF : ";
      s += cur.toRules() + "\n";
      nbHyp++;
      nbHypClass[(int) cur.classValue()]++;	    
      if (cur.numInstances() == 1){
	nbSingle++;
	nbSingleClass[(int) cur.classValue()]++;
      }
      cur = cur.next;
    }
    s += "\nStat :\n";
    for(i = 0; i<nbHypClass.length; i++){
      s += "\tclass " + m_Train.attribute(m_Train.classIndex()).value(i) + 
	" : " + Integer.toString(nbHypClass[i]) + " exemplar(s) including " + 
	Integer.toString(nbHypClass[i] - nbSingleClass[i]) + " Hyperrectangle(s) and " +
	Integer.toString(nbSingleClass[i]) + " Single(s).\n";
    }
    s += "\n\tTotal : " + Integer.toString(nbHyp) + " exemplars(s) including " + 
      Integer.toString(nbHyp - nbSingle) + " Hyperrectangle(s) and " +
      Integer.toString(nbSingle) + " Single(s).\n";
	
    s += "\n";
	
    s += "\tFeature weights : ";

    String space = "[";
    for(int ii = 0; ii < m_Train.numAttributes(); ii++){
      if(ii != m_Train.classIndex()){
	s += space + Double.toString(attrWeight(ii));
	space = " ";
      }
    }
    s += "]";
    s += "\n\n";
    return s;
  }



  /** OPTION HANDLER FUNCTION */
    

  /**
   * Returns an enumeration of all the available options..
   *
   * @return an enumeration of all available options.
   */
  public Enumeration listOptions(){

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
				    "\tNumber of attempts of generalisation.\n",
				    "G", 
				    1, 
				    "-G <value>"));
    newVector.addElement(new Option(
				    "\tNumber of folder for computing the mutual information.\n",
				    "I", 
				    1, 
				    "-I <value>"));

    return newVector.elements();
  }

    
  /**
   * Sets the OptionHandler's options using the given list. All options
   * will be set (or reset) during this call (i.e. incremental setting
   * of options is not possible). <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -G &lt;value&gt;
   *  Number of attempts of generalisation.
   * </pre>
   * 
   * <pre> -I &lt;value&gt;
   *  Number of folder for computing the mutual information.
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String str;

    /* Number max of attempts of generalisation */
    str = Utils.getOption('G', options);
    if(str.length() != 0){
      m_NumAttemptsOfGene = Integer.parseInt(str);
      if(m_NumAttemptsOfGene < 1)
	throw new Exception("NNge.setOptions : G option's value must be greater than 1.");
    } else {
      m_NumAttemptsOfGene = 5;
    }

    /* Number of folder for computing the mutual information */
    str = Utils.getOption('I', options);
    if(str.length() != 0){
      m_NumFoldersMI = Integer.parseInt(str);
      if(m_NumFoldersMI < 1)
	throw new Exception("NNge.setOptions : I option's value must be greater than 1.");
    } else {
      m_NumFoldersMI = 5;
    }
  }

    
  /**
   * Gets the current option settings for the OptionHandler.
   *
   * @return the list of current option settings as an array of strings
   */
  public String[] getOptions(){

    String[] options = new String[5];
    int current = 0;

    options[current++] = "-G"; options[current++] = "" + m_NumAttemptsOfGene;
    options[current++] = "-I"; options[current++] = "" + m_NumFoldersMI;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numAttemptsOfGeneOptionTipText() {
    return "Sets the number of attempts for generalization.";
  }

  /**
   * Gets the number of attempts for generalisation.
   *
   * @return the value of the option G
   */
  public int getNumAttemptsOfGeneOption() {
    return m_NumAttemptsOfGene;
  }


  /**
   * Sets the number of attempts for generalisation.
   *
   * @param newIntParameter the new value.
   */
  public void setNumAttemptsOfGeneOption(int newIntParameter) {
    m_NumAttemptsOfGene = newIntParameter;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldersMIOptionTipText() {
    return "Sets the number of folder for mutual information.";
  }

  /**
   * Gets the number of folder for mutual information.
   *
   * @return the value of the option I
   */
  public int getNumFoldersMIOption() {
    return m_NumFoldersMI;
  }


  /**
   * Sets the number of folder for mutual information.
   *
   * @param newIntParameter the new value.
   */
  public void setNumFoldersMIOption(int newIntParameter) {
    m_NumFoldersMI = newIntParameter;
  }



  /** ENTRY POINT */


  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line arguments for evaluation
   * (see Evaluation).
   */
  public static void main(String [] argv) {
    runClassifier(new NNge(), argv);
  }
}
