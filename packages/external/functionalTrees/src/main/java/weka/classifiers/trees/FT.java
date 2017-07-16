/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    FT.java
 *    Copyright (C) 2007 University of Porto, Porto, Portugal
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.ft.FTInnerNode;
import weka.classifiers.trees.ft.FTLeavesNode;
import weka.classifiers.trees.ft.FTNode;
import weka.classifiers.trees.ft.FTtree;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Builds 'functional trees' for classification, more specifically, functional trees with  logistic regression functions at the inner nodes and/or leaves. The algorithm can deal with binary and multi-class target variables, numeric and nominal attributes and missing values.<br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Joao Gama (2004). Functional Trees.  Machine Learning. 55(3):219-250.<br/>
 * <br/>
 * Niels Landwehr, Mark Hall, Eibe Frank (2005). Logistic Model Trees. Machine Learning. 95(1-2):161-205.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Gama2004,
 *    author = {Joao Gama},
 *    booktitle = {Machine Learning},
 *    number = {3},
 *    pages = {219-250},
 *    title = {Functional Trees},
 *    volume = {55},
 *    year = {2004}
 * }
 * 
 * &#64;article{Landwehr2005,
 *    author = {Niels Landwehr and Mark Hall and Eibe Frank},
 *    booktitle = {Machine Learning},
 *    number = {1-2},
 *    pages = {161-205},
 *    title = {Logistic Model Trees},
 *    volume = {95},
 *    year = {2005}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -B
 *  Binary splits (convert nominal attributes to binary ones) </pre>
 * 
 * <pre> -P
 *  Use error on probabilities instead of misclassification error for stopping criterion of LogitBoost.</pre>
 * 
 * <pre> -I &lt;numIterations&gt;
 *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)</pre>
 * 
 * <pre> -F &lt;modelType&gt;
 *  Set Funtional Tree type to be generate:  0 for FT, 1 for FTLeaves and 2 for FTInner</pre>
 * 
 * <pre> -M &lt;numInstances&gt;
 *  Set minimum number of instances at which a node can be split (default 15)</pre>
 * 
 * <pre> -W &lt;beta&gt;
 *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.</pre>
 * 
 * <pre> -A
 *  The AIC is used to choose the best iteration.</pre>
 * 
 <!-- options-end -->
 *
 * @author Jo\~{a}o Gama
 * @author Carlos Ferreira  
 * @version $Revision$
 */
public class FT 
  extends AbstractClassifier 
  implements OptionHandler, AdditionalMeasureProducer, Drawable,
             TechnicalInformationHandler {
    
  /** for serialization */
  static final long serialVersionUID = -1113212459618105000L;
  
  /** Filter to replace missing values*/
  protected ReplaceMissingValues m_replaceMissing;
  
  /** Filter to replace nominal attributes*/
  protected NominalToBinary m_nominalToBinary;
    
  /** root of the logistic model tree*/
  protected FTtree m_tree;
  
  /** convert nominal attributes to binary ?*/
  protected boolean m_convertNominal;
  
  /**use error on probabilties instead of misclassification for stopping criterion of LogitBoost?*/
  protected boolean m_errorOnProbabilities;
    
  /**minimum number of instances at which a node is considered for splitting*/
  protected int m_minNumInstances;

  /**if non-zero, use fixed number of iterations for LogitBoost*/
  protected int m_numBoostingIterations;
  
  /**Model Type, value: 0 is FT, 1 is FTLeaves, 2  is FTInner*/
  protected int m_modelType;
    
  /**Threshold for trimming weights. Instances with a weight lower than this (as a percentage
   * of total weights) are not included in the regression fit.
   **/
  protected double m_weightTrimBeta;
  
  /** If true, the AIC is used to choose the best LogitBoost iteration*/
  protected boolean m_useAIC ;

  /** model types */
  public static final int MODEL_FT = 0;
  public static final int MODEL_FTLeaves = 1;
  public static final int MODEL_FTInner = 2;

  /** possible model types. */
  public static final Tag [] TAGS_MODEL = {
    new Tag(MODEL_FT, "FT"),
    new Tag(MODEL_FTLeaves, "FTLeaves"),
    new Tag(MODEL_FTInner, "FTInner")
  };

  
  /**
   * Creates an instance of FT with standard options
   */
  public FT() {
    m_numBoostingIterations=15;
    m_minNumInstances = 15;
    m_weightTrimBeta = 0;
    m_useAIC = false;
    m_modelType=0;
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
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Builds the classifier.
   *
   * @param data the data to train with
   * @throws Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances data) throws Exception{
	
      
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances filteredData = new Instances(data);
    filteredData.deleteWithMissingClass();
    
    //replace missing values
    m_replaceMissing = new ReplaceMissingValues();
    m_replaceMissing.setInputFormat(filteredData);	
    filteredData = Filter.useFilter(filteredData, m_replaceMissing);
    
    //possibly convert nominal attributes globally
    if (m_convertNominal) {	    
      m_nominalToBinary = new NominalToBinary();
      m_nominalToBinary.setInputFormat(filteredData);	
      filteredData = Filter.useFilter(filteredData, m_nominalToBinary);
    }
	
    int minNumInstances = 2;  
    
    
    //create a FT  tree root
    if (m_modelType==0)
      m_tree = new FTNode( m_errorOnProbabilities, m_numBoostingIterations, m_minNumInstances, 
                           m_weightTrimBeta, m_useAIC);
                       
    //create a FTLeaves  tree root
    if (m_modelType==1){ 
      m_tree = new FTLeavesNode(m_errorOnProbabilities, m_numBoostingIterations, m_minNumInstances, 
                                m_weightTrimBeta, m_useAIC);
    }
    //create a FTInner  tree root
    if (m_modelType==2)
      m_tree = new FTInnerNode(m_errorOnProbabilities, m_numBoostingIterations, m_minNumInstances, 
                               m_weightTrimBeta, m_useAIC);
        
    //build tree
    m_tree.buildClassifier(filteredData);
    // prune tree
    m_tree.prune();
    m_tree.assignIDs(0);
    m_tree.cleanup();         
  }
  
  /** 
   * Returns class probabilities for an instance.
   *
   * @param instance the instance to compute the distribution for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  public double [] distributionForInstance(Instance instance) throws Exception {
     
    //replace missing values
    m_replaceMissing.input(instance);
    instance = m_replaceMissing.output();
    
    //possibly convert nominal attributes
    if (m_convertNominal) {
      m_nominalToBinary.input(instance);
      instance = m_nominalToBinary.output();
    }
    return m_tree.distributionForInstance(instance);
  }

  /**
   * Classifies an instance.
   *
   * @param instance the instance to classify
   * @return the classification
   * @throws Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    double maxProb = -1;
    int maxIndex = 0;
   
    //classify by maximum probability
    double[] probs = distributionForInstance(instance);       
    for (int j = 0; j < instance.numClasses(); j++) {
      if (Utils.gr(probs[j], maxProb)) {
	maxIndex = j;
	maxProb = probs[j];
      }
    }     
    return (double)maxIndex;      
  }    
     
  /**
   * Returns a description of the classifier.
   * 
   * @return a string representation of the classifier
   */
  public String toString() {
    if (m_tree!=null) {
      if (m_modelType==0)
        return "FT tree \n------------------\n" + m_tree.toString();
      else { 
        if (m_modelType==1)
          return "FT Leaves tree \n------------------\n" + m_tree.toString();
        else  
          return "FT Inner tree \n------------------\n" + m_tree.toString();
      }
    }else{
      return "No tree built";
    }
  }    

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(8);
    
    newVector.addElement(new Option("\tBinary splits (convert nominal attributes to binary ones) ",
                                    "B", 0, "-B"));
    
    newVector.addElement(new Option("\tUse error on probabilities instead of misclassification error "+
                                    "for stopping criterion of LogitBoost.",
                                    "P", 0, "-P"));
    
    newVector.addElement(new Option("\tSet fixed number of iterations for LogitBoost (instead of using "+
                                    "cross-validation)",
                                    "I",1,"-I <numIterations>"));
    
    newVector.addElement(new Option("\tSet Funtional Tree type to be generate: "+
                                    " 0 for FT, 1 for FTLeaves and 2 for FTInner",
                                    "F",1,"-F <modelType>"));
    
    newVector.addElement(new Option("\tSet minimum number of instances at which a node can be split (default 15)",
                                    "M",1,"-M <numInstances>"));
    
    newVector.addElement(new Option("\tSet beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.",
                                    "W",1,"-W <beta>"));
    
    newVector.addElement(new Option("\tThe AIC is used to choose the best iteration.",
                                    "A", 0, "-A"));
    
    return newVector.elements();
  }
    
  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -B
   *  Binary splits (convert nominal attributes to binary ones) </pre>
   * 
   * <pre> -P
   *  Use error on probabilities instead of misclassification error for stopping criterion of LogitBoost.</pre>
   * 
   * <pre> -I &lt;numIterations&gt;
   *  Set fixed number of iterations for LogitBoost (instead of using cross-validation)</pre>
   * 
   * <pre> -F &lt;modelType&gt;
   *  Set Funtional Tree type to be generate:  0 for FT, 1 for FTLeaves and 2 for FTInner</pre>
   * 
   * <pre> -M &lt;numInstances&gt;
   *  Set minimum number of instances at which a node can be split (default 15)</pre>
   * 
   * <pre> -W &lt;beta&gt;
   *  Set beta for weight trimming for LogitBoost. Set to 0 (default) for no weight trimming.</pre>
   * 
   * <pre> -A
   *  The AIC is used to choose the best iteration.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setBinSplit(Utils.getFlag('B', options));
    setErrorOnProbabilities(Utils.getFlag('P', options));

    String optionString = Utils.getOption('I', options);
    if (optionString.length() != 0) {
      setNumBoostingIterations((new Integer(optionString)).intValue());
    }

    optionString = Utils.getOption('F', options);
    if (optionString.length() != 0) {
      setModelType(new SelectedTag(Integer.parseInt(optionString), TAGS_MODEL));
      // setModelType((new Integer(optionString)).intValue());
    }
    
    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMinNumInstances((new Integer(optionString)).intValue());
    }

    optionString = Utils.getOption('W', options);
    if (optionString.length() != 0) {
      setWeightTrimBeta((new Double(optionString)).doubleValue());
    }
    
    setUseAIC(Utils.getFlag('A', options));        
    
    Utils.checkForRemainingOptions(options);
	
  } 
    
  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[11];
    int current = 0;

    if (getBinSplit()) {
      options[current++] = "-B";
    } 
    
    if (getErrorOnProbabilities()) {
      options[current++] = "-P";
    }
    
    options[current++] = "-I"; 
    options[current++] = ""+getNumBoostingIterations();
    
    options[current++] = "-F"; 
    //    options[current++] = ""+getModelType();
    options[current++] = ""+getModelType().getSelectedTag().getID();

    options[current++] = "-M"; 
    options[current++] = ""+getMinNumInstances();
        
    options[current++] = "-W";
    options[current++] = ""+getWeightTrimBeta();
    
    if (getUseAIC()) {
      options[current++] = "-A";
    }
    
    while (current < options.length) {
      options[current++] = "";
    } 
    return options;
  } 

  
  /**
   * Get the value of weightTrimBeta.
   */
  public double getWeightTrimBeta(){
    return m_weightTrimBeta;
  }
  
  /**
   * Get the value of useAIC.
   *
   * @return Value of useAIC.
   */
  public boolean getUseAIC(){
    return m_useAIC;
  }
 
  
  /**
   * Set the value of weightTrimBeta.
   */
  public void setWeightTrimBeta(double n){
    m_weightTrimBeta = n;
  }
  
  /**
   * Set the value of useAIC.
   *
   * @param c Value to assign to useAIC.
   */
  public void setUseAIC(boolean c){
    m_useAIC = c;
  }
  
  /**
   * Get the value of binarySplits.
   *
   * @return Value of binarySplits.
   */
  public boolean getBinSplit(){
    return m_convertNominal;
  }

  /**
   * Get the value of errorOnProbabilities.
   *
   * @return Value of errorOnProbabilities.
   */
  public boolean getErrorOnProbabilities(){
    return m_errorOnProbabilities;
  }
  
  /**
   * Get the value of numBoostingIterations.
   *
   * @return Value of numBoostingIterations.
   */
  public int getNumBoostingIterations(){
    return m_numBoostingIterations;
  }
  
  /**
   * Get the type of functional tree model being used.
   *
   * @return the type of functional tree model.
   */
  public SelectedTag getModelType() {
    return new SelectedTag(m_modelType, TAGS_MODEL);
  } 

  /**
   * Set the Functional Tree type.
   *
   * @param newMethod Value corresponding to tree type.
   */
  public void setModelType(SelectedTag newMethod){
    if (newMethod.getTags() == TAGS_MODEL) {
      int c = newMethod.getSelectedTag().getID();
      if (c==0 || c==1 || c==2) {
        m_modelType = c;
      } else  {
        throw new IllegalArgumentException("Wrong model type, -F value should be: 0, for FT, 1, " +
                                           "for FTLeaves, and 2, for FTInner "); 
      }
    }
  }
  
  /**
   * Get the value of minNumInstances.
   *
   * @return Value of minNumInstances.
   */
  public int getMinNumInstances(){
    return m_minNumInstances;
  }
    
  /**
   * Set the value of binarySplits.
   *
   * @param c Value to assign to binarySplits.
   */
  public void setBinSplit(boolean c){
    m_convertNominal=c;
  }

  /**
   * Set the value of errorOnProbabilities.
   *
   * @param c Value to assign to errorOnProbabilities.
   */
  public void setErrorOnProbabilities(boolean c){
    m_errorOnProbabilities = c;
  }
  
  /**
   * Set the value of numBoostingIterations.
   *
   * @param c Value to assign to numBoostingIterations.
   */
  public void setNumBoostingIterations(int c){
    m_numBoostingIterations = c;
  }
   
  /**
   * Set the value of minNumInstances.
   *
   * @param c Value to assign to minNumInstances.
   */
  public void setMinNumInstances(int c){
    m_minNumInstances = c;
  }
    
  /**
   *  Returns the type of graph this classifier
   *  represents.
   *  @return Drawable.TREE
   */   
  public int graphType() {
    return Drawable.TREE;
  }

  /**
   * Returns graph describing the tree.
   *
   * @return the graph describing the tree
   * @throws Exception if graph can't be computed
   */
  public String graph() throws Exception {

    return m_tree.graph();
  }

  /**
   * Returns the size of the tree
   * @return the size of the tree
   */
  public int measureTreeSize(){
    return m_tree.numNodes();
  }
    
  /**
   * Returns the number of leaves in the tree
   * @return the number of leaves in the tree
   */
  public int measureNumLeaves(){
    return m_tree.numLeaves();
  }
     
  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(2);
    newVector.addElement("measureTreeSize");
    newVector.addElement("measureNumLeaves");
	
    return newVector.elements();
  }
    

  /**
   * Returns the value of the named measure
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureTreeSize") == 0) {
      return measureTreeSize();
    } else if (additionalMeasureName.compareToIgnoreCase("measureNumLeaves") == 0) {
      return measureNumLeaves();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
					 + " not supported (FT)");
    }
  }    
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Builds 'functional trees' for classification, more specifically, functional trees with "
      +"logistic regression functions at the inner nodes and/or leaves. The algorithm can deal with " 
      +"binary and multi-class target variables, numeric and nominal attributes and missing values.\n\n"
      +"For more information see: \n\n"
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
      
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Joao Gama");
    result.setValue(Field.TITLE, "Functional Trees");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.VOLUME, "55");
    result.setValue(Field.PAGES, "219-250");
    result.setValue(Field.NUMBER, "3");
    
    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "Niels Landwehr and Mark Hall and Eibe Frank");
    additional.setValue(Field.TITLE, "Logistic Model Trees");
    additional.setValue(Field.JOURNAL, "Machine Learning");
    additional.setValue(Field.YEAR, "2005");
    additional.setValue(Field.VOLUME, "95");
    additional.setValue(Field.PAGES, "161-205");
    additional.setValue(Field.NUMBER, "1-2");
    
    return result;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String modelTypeTipText() {
    return "The type of FT model. 0, for FT, 1, " +
      "for FTLeaves, and 2, for FTInner";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binSplitTipText() {
    return "Convert all nominal attributes to binary ones before building the tree. "
      +"This means that all splits in the final tree will be binary.";
    
  } 
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String errorOnProbabilitiesTipText() {
    return "Minimize error on probabilities instead of misclassification error when cross-validating the number "
      +"of LogitBoost iterations. When set, the number of LogitBoost iterations is chosen that minimizes "
      +"the root mean squared error instead of the misclassification error.";	   
  } 
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numBoostingIterationsTipText() {
    return "Set a fixed number of iterations for LogitBoost. If >= 0, this sets a fixed number of LogitBoost "
      +"iterations that is used everywhere in the tree. If < 0, the number is cross-validated.";
  }  

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minNumInstancesTipText() {
    return "Set the minimum number of instances at which a node is considered for splitting. "
      +"The default value is 15.";
  } 
      
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightTrimBetaTipText() {
    return "Set the beta value used for weight trimming in LogitBoost. "
      +"Only instances carrying (1 - beta)% of the weight from previous iteration "
      +"are used in the next iteration. Set to 0 for no weight trimming. "
      +"The default value is 0.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useAICTipText() {
    return "The AIC is used to determine when to stop LogitBoost iterations. "
      +"The default is not to use AIC.";
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
   * Main method for testing this class
   *
   * @param argv the commandline options 
   */
  public static void main (String [] argv) {	
    runClassifier(new FT(), argv);
  } 
}

