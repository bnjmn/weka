/*
 *    ThresholdSelector.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.AttributeStats;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Attribute;

/**
 * Class for selecting a threshold on a probability output by a
 * distribution classifier. The treshold is set so that a given
 * performance measure is optimized. Currently this is the
 * F-measure. Performance is measured on the training data, a hold-out
 * set or using cross-validation.<p>
 *
 * Valid options are:<p>
 *
 * -C num <br>
 * The class for which threshold is determined. Valid values are:
 * 1, 2 (for first and second classes, respectively), 3 (for whichever
 * class is least frequent), 4 (for whichever class value is most 
 * frequent), and 5 (for the first class named any of "yes","pos(itive)",
 * "1", or method 3 if no matches). (default 5). <p>
 *
 * -W classname <br>
 * Specify the full class name of the base classifier. <p>
 *
 * -X num <br> 
 * Number of folds used for cross validation. If just a
 * hold-out set is used, this determines the size of the hold-out set
 * (default 3).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -E integer <br>
 * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
 * 1 for evaluation using hold-out set, and 2 for evaluation on the
 * training data (default 1).<p>
 *
 * Options after -- are passed to the designated sub-classifier. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $ 
 */
public class ThresholdSelector extends DistributionClassifier 
  implements OptionHandler {

  /** The evaluation modes */
  public final static int EVAL_TRAINING_SET = 2;
  public final static int EVAL_TUNED_SPLIT = 1;
  public final static int EVAL_CROSS_VALIDATION = 0;
  public static final Tag [] TAGS_EVAL = {
    new Tag(EVAL_TRAINING_SET, "Entire training set"),
    new Tag(EVAL_TUNED_SPLIT, "Single tuned fold"),
    new Tag(EVAL_CROSS_VALIDATION, "N-Fold cross validation")
  };

  /* How to determine which class value to optimize for */
  public final static int OPTIMIZE_0     = 0;
  public final static int OPTIMIZE_1     = 1;
  public final static int OPTIMIZE_LFREQ = 2;
  public final static int OPTIMIZE_MFREQ = 3;
  public final static int OPTIMIZE_POS_NAME = 4;
  public static final Tag [] TAGS_OPTIMIZE = {
    new Tag(OPTIMIZE_0, "First class value"),
    new Tag(OPTIMIZE_1, "Second class value"),
    new Tag(OPTIMIZE_LFREQ, "Least frequent class value"),
    new Tag(OPTIMIZE_MFREQ, "Most frequent class value"),
    new Tag(OPTIMIZE_POS_NAME, "Class value named: \"yes\", \"pos(itive)\",\"1\"")
  };

  /** The generated base classifier */
  protected DistributionClassifier m_Classifier = 
    new weka.classifiers.ZeroR();

  /** The threshold that lead to the best performance */
  protected double m_BestThreshold = -Double.MAX_VALUE;

  /** The best value that has been observed */
  protected double m_BestValue = - Double.MAX_VALUE;
  
  /** The number of folds used in cross-validation */
  protected int m_NumXValFolds = 3;

  /** Random number seed */
  protected int m_Seed = 1;

  /** Designated class value, determined during building */
  protected int m_DesignatedClass = 0;

  /** Method to determine which class to optimize for */
  protected int m_ClassMode = OPTIMIZE_POS_NAME;

  /** The evaluation mode */
  protected int m_EvalMode = EVAL_TUNED_SPLIT;

  /** The minimum value for the criterion. If threshold adjustment
      yields less than that, the default threshold of 0.5 is used. */
  protected final static double MIN_VALUE = 0.05;

  /**
   * Collects the classifier predictions using the specified evaluation method.
   */
  protected FastVector getPredictions(Instances instances, int mode) 
    throws Exception {

    EvaluationUtils eu = new EvaluationUtils();
    eu.setSeed(m_Seed);
    
    switch (mode) {
    case EVAL_TUNED_SPLIT:
      Instances trainData = null, evalData = null;
      Instances data = new Instances(instances);
      data.randomize(new Random(m_Seed));
      data.stratify(m_NumXValFolds);
      
      // Make sure that both subsets contain at least one positive instance
      for (int subsetIndex = 0; subsetIndex < m_NumXValFolds; subsetIndex++) {
        trainData = data.trainCV(m_NumXValFolds, subsetIndex);
        evalData = data.testCV(m_NumXValFolds, subsetIndex);
        if (checkForInstance(trainData) && checkForInstance(evalData)) {
          break;
        }
      }
      return eu.getTrainTestPredictions(m_Classifier, trainData, evalData);
    case EVAL_TRAINING_SET:
      return eu.getTrainTestPredictions(m_Classifier, instances, instances);
    case EVAL_CROSS_VALIDATION:
      return eu.getCVPredictions(m_Classifier, instances, m_NumXValFolds);
    default:
      throw new Exception("Unrecognized evaluation mode");
    }
  }

  /**
   * Finds the best threshold, this implementation searches for the
   * highest FMeasure. If no FMeasure higher than MIN_VALUE is found,
   * the default threshold of 0.5 is used.
   */
  protected void findThreshold(FastVector predictions) throws Exception {

    Instances curve = (new ThresholdCurve()).getCurve(predictions, m_DesignatedClass);
    
    m_BestThreshold = 0.5;
    m_BestValue = MIN_VALUE;
    if (curve.numInstances() > 0) {
      Instance maxFM = curve.instance(0);
      int indexFM = curve.attribute(ThresholdCurve.FMEASURE_NAME).index();
      int indexThreshold = curve.attribute(ThresholdCurve.THRESHOLD_NAME).index();
      for (int i = 1; i < curve.numInstances(); i++) {
        Instance current = curve.instance(i);
        if (current.value(indexFM) > maxFM.value(indexFM)) {
          maxFM = current;
        }
      }
      if (maxFM.value(indexFM) > MIN_VALUE) {
        m_BestThreshold = maxFM.value(indexThreshold);
        m_BestValue = maxFM.value(indexFM);
      }
    }
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
              "\tThe class for which threshold is determined. Valid values are:\n" +
              "\t1, 2 (for first and second classes, respectively), 3 (for whichever\n" +
              "\tclass is least frequent), and 4 (for whichever class value is most\n" +
              "\tfrequent), and 5 (for the first class named any of \"yes\",\"pos(itive)\"\n" +
              "\t\"1\", or method 3 if no matches). (default 5).",
	      "C", 1, "-C <integer>"));
    newVector.addElement(new Option(
	      "\tFull name of classifier to perform parameter selection on.\n"
	      + "\teg: weka.classifiers.NaiveBayes",
	      "W", 1, "-W <classifier class name>"));
    newVector.addElement(new Option(
	      "\tNumber of folds used for cross validation. If just a\n" +
	      "\thold-out set is used, this determines the size of the hold-out set\n" +
	      "\t(default 3).",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed (default 1).",
	      "S", 1, "-S <random number seed>"));
    newVector.addElement(new Option(
	      "\tSets the evaluation mode. Use 0 for\n" +
	      "\tevaluation using cross-validation,\n" +
	      "\t1 for evaluation using hold-out set,\n" +
	      "\tand 2 for evaluation on the\n" +
	      "\ttraining data (default 1).",
	      "E", 1, "-E <integer>"));

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("",
	        "", 0,
		"\nOptions specific to sub-classifier "
	        + m_Classifier.getClass().getName()
		+ ":\n(use -- to signal start of sub-classifier options)"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -C num <br>
   * The class for which threshold is determined. Valid values are:
   * 1, 2 (for first and second classes, respectively), 3 (for whichever
   * class is least frequent), 4 (for whichever class value is most 
   * frequent), and 5 (for the first class named any of "yes","pos(itive)",
   * "1", or method 3 if no matches). (default 3). <p>
   *
   * -W classname <br>
   * Specify the full class name of classifier to perform cross-validation
   * selection on.<p>
   *
   * -X num <br> 
   * Number of folds used for cross validation. If just a
   * hold-out set is used, this determines the size of the hold-out set
   * (default 3).<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -E integer <br>
   * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
   * 1 for evaluation using hold-out set, and 2 for evaluation on the
   * training data (default 1).<p>
   *
   * Options after -- are passed to the designated sub-classifier. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String classString = Utils.getOption('C', options);
    if (classString.length() != 0) {
      setDesignatedClass(new SelectedTag(Integer.parseInt(classString) - 1, 
                                         TAGS_OPTIMIZE));
    } else {
      setDesignatedClass(new SelectedTag(OPTIMIZE_LFREQ, 
                                         TAGS_OPTIMIZE));
    }

    String foldsString = Utils.getOption('X', options);
    if (foldsString.length() != 0) {
      setNumXValFolds(Integer.parseInt(foldsString));
    } else {
      setNumXValFolds(3);
    }

    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }

    String modeString = Utils.getOption('E', options);
    if (modeString.length() != 0) {
      m_EvalMode = Integer.parseInt(modeString);
    } else {
      m_EvalMode = EVAL_TUNED_SPLIT;
    }

    setDistributionClassifier((DistributionClassifier)Classifier.
		  forName(classifierName,
			  Utils.partitionOptions(options)));
    if (!(m_Classifier instanceof OptionHandler)) {
      throw new Exception("Base classifier must accept options");
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    int current = 0;
    String [] options = new String [classifierOptions.length + 11];

    options[current++] = "-C"; options[current++] = "" + (m_DesignatedClass + 1);
    options[current++] = "-X"; options[current++] = "" + getNumXValFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();

    if (getDistributionClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getDistributionClassifier().getClass().getName();
    }
    options[current++] = "-E"; options[current++] = "" + m_EvalMode;
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (instances.numClasses() != 2) {
      throw new Exception("Only works for two-class datasets!");
    }
    AttributeStats stats = instances.attributeStats(instances.classIndex());
    if (stats.distinctCount <= 1) {
      throw new Exception("Need two class values occuring in training data!");
    }
    if (stats.missingCount > 0) {
      instances = new Instances(instances);
      instances.deleteWithMissingClass();
    }
    if (instances.numInstances() < m_NumXValFolds) {
      throw new Exception("Number of training instances smaller than number of folds.");
    }

    // Determine which class value to look for
    switch (m_ClassMode) {
    case OPTIMIZE_0:
      m_DesignatedClass = 0;
      break;
    case OPTIMIZE_1:
      m_DesignatedClass = 1;
      break;
    case OPTIMIZE_POS_NAME:
      Attribute cAtt = instances.classAttribute();
      boolean found = false;
      for (int i = 0; i < cAtt.numValues() && !found; i++) {
        String name = cAtt.value(i).toLowerCase();
        if (name.equals("yes") || name.equals("1") || 
            name.equals("pos") || name.equals("positive")) {
          found = true;
          m_DesignatedClass = i;
        }
      }
      if (found) {
        break;
      }
      // No named class found, so fall through to default of least frequent
    case OPTIMIZE_LFREQ:
      m_DesignatedClass = (stats.nominalCounts[0] > stats.nominalCounts[1]) ? 1 : 0;
      break;
    case OPTIMIZE_MFREQ:
      m_DesignatedClass = (stats.nominalCounts[0] > stats.nominalCounts[1]) ? 0 : 1;
      break;
    default:
      throw new Exception("Unrecognized class value selection mode");
    }

    // If data contains only one instance of positive data
    // optimize on training data
    if (stats.nominalCounts[m_DesignatedClass] == 1) {
      System.err.println("Only 1 positive found: optimizing on training data");
      findThreshold(getPredictions(instances, EVAL_TRAINING_SET));
    } else {
      findThreshold(getPredictions(instances, m_EvalMode));
      if (m_EvalMode != EVAL_TRAINING_SET) {
	m_Classifier.buildClassifier(instances);
      }
    }
  }

  /**
   * Checks whether instance of designated class is in subset.
   */
  private boolean checkForInstance(Instances data) throws Exception {

    for (int i = 0; i < data.numInstances(); i++) {
      if (((int)data.instance(i).classValue()) == m_DesignatedClass) {
	return true;
      }
    }
    return false;
  }


  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {
    
    double [] pred = m_Classifier.distributionForInstance(instance);
    double prob = pred[m_DesignatedClass];

    // Warp probability
    if (prob > m_BestThreshold) {
      prob = 0.5 + (prob - m_BestThreshold) / ((1 - m_BestThreshold) * 2);
    } else {
      prob = prob / (m_BestThreshold * 2);
    }

    // Alter the distribution
    pred[m_DesignatedClass] = prob;
    pred[(m_DesignatedClass + 1) % 2] = 1.0 - prob;
    return pred;
  }
    
  /**
   * Gets the method to determine which class value to optimize. Will
   * be one of OPTIMIZE_0, OPTIMIZE_1, OPTIMIZE_LFREQ, OPTIMIZE_MFREQ,
   * OPTIMIZE_POS_NAME.
   *
   * @return the class selection mode.
   */
  public SelectedTag getDesignatedClass() {

    try {
      return new SelectedTag(m_ClassMode, TAGS_OPTIMIZE);
    } catch (Exception ex) {
      return null;
    }
  }
  
  /**
   * Sets the method to determine which class value to optimize. Will
   * be one of OPTIMIZE_0, OPTIMIZE_1, OPTIMIZE_LFREQ, OPTIMIZE_MFREQ,
   * OPTIMIZE_POS_NAME.
   *
   * @param newMethod the new class selection mode.
   */
  public void setDesignatedClass(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_OPTIMIZE) {
      m_ClassMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Sets the evaluation mode used. Will be one of
   * EVAL_TRAINING, EVAL_TUNED_SPLIT, or EVAL_CROSS_VALIDATION
   *
   * @param newMethod the new evaluation mode.
   */
  public void setEvaluationMode(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_EVAL) {
      m_EvalMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Gets the evaluation mode used. Will be one of
   * EVAL_TRAINING, EVAL_TUNED_SPLIT, or EVAL_CROSS_VALIDATION
   *
   * @return the evaluation mode.
   */
  public SelectedTag getEvaluationMode() {

    try {
      return new SelectedTag(m_EvalMode, TAGS_EVAL);
    } catch (Exception ex) {
      return null;
    }
  }
  
  /**
   * Sets the seed for random number generation.
   *
   * @param seed the random number seed
   */
  public void setSeed(int seed) {
    
    m_Seed = seed;
  }

  /**
   * Gets the random number seed.
   * 
   * @return the random number seed
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Get the number of folds used for cross-validation.
   *
   * @return the number of folds used for cross-validation.
   */
  public int getNumXValFolds() {
    
    return m_NumXValFolds;
  }
  
  /**
   * Set the number of folds used for cross-validation.
   *
   * @param newNumFolds the number of folds used for cross-validation.
   */
  public void setNumXValFolds(int newNumFolds) {
    
    m_NumXValFolds = newNumFolds;
  }

  /**
   * Set the DistributionClassifier for which threshold is set. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setDistributionClassifier(DistributionClassifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the DistributionClassifier used as the classifier.
   *
   * @return the classifier used as the classifier
   */
  public DistributionClassifier getDistributionClassifier() {

    return m_Classifier;
  }

 
  /**
   * Returns description of the cross-validated classifier.
   *
   * @return description of the cross-validated classifier as a string
   */
  public String toString() {

    if (m_BestValue == -Double.MAX_VALUE)
      return "ThresholdSelector: No model built yet.";

    String result = "Threshold Selector.\n"
    + "Classifier: " + m_Classifier.getClass().getName() + "\n";

    result += "Index of designated class: " + m_DesignatedClass + "\n";

    result += "Evaluation mode: ";
    switch (m_EvalMode) {
    case EVAL_CROSS_VALIDATION:
      result += m_NumXValFolds + "-fold cross-validation";
      break;
    case EVAL_TUNED_SPLIT:
      result += "tuning on 1/" + m_NumXValFolds + " of the data";
      break;
    case EVAL_TRAINING_SET:
    default:
      result += "tuning on the training data";
    }
    result += "\n";

    result += "Threshold: " + m_BestThreshold + "\n";
    result += "Best value: " + m_BestValue + "\n";

    result += m_Classifier.toString();
    return result;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new ThresholdSelector(), 
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
