/*
 *    Cleanser.java
 *    Copyright (C) 2001 Malcolm Ware
 */

package weka.classifiers;

import weka.core.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This meta-classifier removes incorrectly classified instances from the 
 * training set until the model built perfectly classifies all the instances in
 * the training set.
 *
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class Cleanser extends DistributionClassifier
  implements OptionHandler, Drawable {
  
  /** The classifier to build the model. */
  protected Classifier m_classifier = new weka.classifiers.ZeroR();
  
  /** A copy of the instances header used to build the current model. */
  protected Instances m_dataHeader;
  
  /** The threshold for numeric instances before they are classified 
   * incorrectly. */
  protected double m_threshold;
  
  /** True if cross-validation should be used to determine if an instance
   * is classified correctly or not. */
  protected boolean m_crossValidation;

  /** The number of folds to use in cross-validation. */
  protected int m_numFolds;
  
  /** 
   * Constructs the Cleanser to use ZeroR as it's default
   * classifier.
   */
  public Cleanser() {
    this(new weka.classifiers.ZeroR());
  }
  
  /** 
   * Constructs the Cleanser to use the passed classifier.
   * @param classifier The classifier used to build the model.
   */
  public Cleanser(Classifier classifier) {
    m_classifier = classifier;
    m_dataHeader = null;
    m_threshold = .1;
    m_crossValidation = false;
    m_numFolds = 10;
  }
  
  /**
   * Set the threshold to identify when a numeric class is incorrectly 
   * classified.
   * @param t The new threshold (must be positive).
   */
  public void setThreshold(double t) {
    if (t >= 0) {
      m_threshold = t;
    }
  }
  
  /**
   * @return the threshold.
   */
  public double getThreshold() {
    return m_threshold;
  }
  
  /**
   * @param classifier The classifier to be used (with its options set).
   */
  public void setClassifier(Classifier classifier) {
    m_classifier = classifier;
  }
  
  /**
   * @return The classifier to be used.
   */
  public Classifier getClassifier() {
    return m_classifier;
  }
  
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }
  
  
  /**
   * @param c True if cross-validation should be used to identify incorrectly
   * classified instances.
   */
  public void setCrossValidation(boolean c) {
    m_crossValidation = c;
  }
  
  /**
   * @return True if cross-validation is used.
   */
  public boolean getCrossValidation() {
    return m_crossValidation;
  }
  
  /**
   * @param n The number of folds to use in cross-validation. (must be 2 or
   * above).
   */
  public void setNumFolds(int n) {
    if (n > 1) {
      m_numFolds = n;
    }
  }
  
  /**
   * @return The number of folds.
   */
  public int getNumFolds() {
    return m_numFolds;
  }

  /**
   * Call this to build a model on cleansed data.
   * the data is cleansed by using the standard test set to build the
   * model and using this to remove instances.
   * @param data The data to cleanse and build from
   */
  private void cleanseNormal(Instances data) throws Exception {
    
    Instance inst;
    Instances buildSet = new Instances(data);
    Instances temp = new Instances(m_dataHeader, buildSet.numInstances());    
    int count = 0;
    double ans;
    while(count != buildSet.numInstances()) {
      count = buildSet.numInstances();
      m_classifier.buildClassifier(buildSet);
      temp = new Instances(buildSet, buildSet.numInstances());
      //now test
      for (int noa = 0; noa < buildSet.numInstances(); noa++) {
	inst = buildSet.instance(noa);
	ans = m_classifier.classifyInstance(inst);
	if (buildSet.classAttribute().isNumeric()) {
	  if (ans >= inst.classValue() 
	      - m_threshold && ans <=
	      inst.classValue() + m_threshold) {
	    temp.add(inst);
	  }
	}
	else { //class is nominal
	  if (ans == inst.classValue()) {
	    temp.add(inst);
	  }
	}
      }
      buildSet = temp;
    }
  }
  
  /**
   * Call this to build a model on cleansed data.
   * the data is cleansed by using the cross-validation test to build the
   * model and use to remove instances.
   * @param data The data to cleanse and build from
   */
  private void cleanseCross(Instances data) throws Exception {
    Instance inst;
    Instances crossSet = new Instances(data);
    Instances temp = new Instances(data, data.numInstances());
    double ans;
    int count = 0;
    while (count != crossSet.numInstances() && 
	   crossSet.numInstances() >= m_numFolds) {
      count = crossSet.numInstances();
      if (crossSet.classAttribute().isNominal()) {
	crossSet.stratify(m_numFolds);
      }
      // Do the folds
      temp = new Instances(crossSet, crossSet.numInstances());
      for (int i = 0; i < m_numFolds; i++) {
	Instances train = crossSet.trainCV(m_numFolds, i);
	m_classifier.buildClassifier(train);
	Instances test = crossSet.testCV(m_numFolds, i);
	//now test
	for (int noa = 0; noa < test.numInstances(); noa++) {
	  inst = test.instance(noa);
	  ans = m_classifier.classifyInstance(inst);
	  if (crossSet.classAttribute().isNumeric()) {
	    if (ans >= inst.classValue() 
		- m_threshold && ans <=
		inst.classValue() + m_threshold) {
	      temp.add(inst);
	    }
	  }
	  else { //class is nominal
	    if (ans == inst.classValue()) {
	      temp.add(inst);
	    }
	  }
	}
      }
      crossSet = temp;
    }
    m_classifier.buildClassifier(crossSet);
     
  }
  
  /**
   * Call this function to build and train a cleansed classifier for the 
   * training data provided.
   * @param data The training data.
   * @exception Throws exception if can't build classification properly.
   */
  public void buildClassifier(Instances data) throws Exception {
    if (data.numInstances() == 0) {
      throw new IllegalArgumentException("Zero training instances in dataset");
    }
    if (m_classifier == null) {
      throw new Exception("No base classifiers have been set!");
    }
    m_dataHeader = new Instances(data, 0);
    if (m_crossValidation) {
      cleanseCross(data);
    }
    else {
      //just use training
      cleanseNormal(data);
    }
    
  }
  
  /**
   * Call this function to predict the class of an instance once a 
   * classification model has been built with the buildClassifier call.
   * @param instance The instance to classify.
   * @return A double array filled with the probabilities of each class type.
   * @exception if can't classify instance.
   */
  public double [] distributionForInstance(Instance instance)
    throws Exception {
    
    if (m_classifier instanceof DistributionClassifier) {
      //then this is actually a distribution classifier
      return ((DistributionClassifier)m_classifier)
	.distributionForInstance(instance);
    }
    double pred = m_classifier.classifyInstance(instance);
    double [] result = new double [m_dataHeader.numClasses()];
    if (Instance.isMissingValue(pred)) {
      return result;
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      result[(int) pred] = 1.0;
      break;
    case Attribute.NUMERIC:
      result[0] = pred;
      break;
    default:
      throw new Exception("Unknown class type");
    }
    return result;
    
  }
  
  
  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(4);
    
    newVector.addElement(new Option(
	      "\tThreshold for the max error when predicting numeric class.\n"
	      +"\t(Value should be >= 0, Default = 0.1).",
	      "L", 1, "-L <Threshold>"));
    newVector.addElement(new Option(
	      "\tThe number of folds to use for cross-validation cleansing.\n"
	      +"\t(Value should be >= 2, Default = 10).",
	      "N", 1, "-N <Number of Folds>"));
    newVector.addElement(new Option(
              "\tCrossValidation will be used to test which instances should" +
	      " be removed.\n" +
	      "\t(Set this to use CrossValidation).",
	      "V", 0,"-V"));
    newVector.addElement(new Option(
	      "\tFull class name of classifier to use, followed\n"
	      + "\tby scheme options. (required)\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -D\"",
	      "C", 1, "-C <classifier specification>"));
  
    
    return newVector.elements();
  }



  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -L num <br>
   * Set the error threshold for numeric class prediction.
   * (default 0.1) <p>
   *
   * -N num <br>
   * Set the number of folds to use for cross-validation cleansing.
   * <p>
   *
   * -V <br>
   * Use cross-validation to do cleansing.
   * <p>
   *
   * -C classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier.
   * (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    //the defaults can be found here!!!!
    String thresholdString = Utils.getOption('L', options);
    if (thresholdString.length() != 0) {
      setThreshold((new Double(thresholdString)).doubleValue());
    } else {
      setThreshold(0.1);
    }

    String foldsString = Utils.getOption('N', options);
    if (foldsString.length() != 0) {
      setNumFolds((new Double(foldsString)).intValue());
    } else {
      setNumFolds(10);
    }
    
    if (Utils.getFlag('V', options)) {
      setCrossValidation(true);
    } else {
      setCrossValidation(false);
    }
    
    String classifierString = Utils.getOption('C', options);
    if (classifierString.length() == 0) {
      throw new Exception("A classifier must be specified"
			  + " with the -C option.");
    }
    String[] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Invalid classifier specification string");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setClassifier(Classifier.forName(classifierName, classifierSpec));
    
    Utils.checkForRemainingOptions(options);
  }



  /**
   * Gets the current settings of the Cleanser.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    String [] options = new String [7];
    int current = 0;
    options[current++] = "-L"; options[current++] = "" + getThreshold();
    options[current++] = "-N"; options[current++] = "" + getNumFolds();
    
    if (getCrossValidation()) {
      options[current++] = "-V";
    }
    options[current++] = "-C"; options[current++] = "" + getClassifierSpec();

    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
  
  
  /**
   * If the current scheme implements Drawable.
   * Returns graph describing the tree.
   *
   * @exception Exception if graph can't be computed, or classifier is
   * not graphable.
   */
  public String graph() throws Exception {
    if (!(m_classifier instanceof Drawable)) {
      throw new Exception("Classifier: " + getClassifierSpec() +
			  " is not graphable.");
    }
    return ((Drawable)m_classifier).graph();
  }
    

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_dataHeader == null) {
      return "Cleanser: No model built yet.";
    }

    String result = "Cleanser using "
      + getClassifierSpec()
      + "\n\nClassifier Model\n"
      + m_classifier.toString();
    return result;
  }

  /**
   * @return a string describing this meta-classifier.
   */
  public String globalInfo() {
    return "Cleanse data by removing instances the selected classifier" +
      " incorrectly classifies. Until model built perfectly classifies" +
      " the remaining training instances.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Set allowable error for numeric predictions.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String crossValidationTipText() {
    return "Use cross-validation to test for incorrect classifications.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "The number of folds to use for cross-validation.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Set the classifier to use in this meta-classifier.";
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel
			 (new Cleanser(),
			  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
  
}
