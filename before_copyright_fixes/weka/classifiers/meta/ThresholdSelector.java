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
 *    ThresholdSelector.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.meta;

import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A metaclassifier that selecting a mid-point threshold on the probability output by a Classifier. The midpoint threshold is set so that a given performance measure is optimized. Currently this is the F-measure. Performance is measured either on the training data, a hold-out set or using cross-validation. In addition, the probabilities returned by the base learner can have their range expanded so that the output probabilities will reside between 0 and 1 (this is useful if the scheme normally produces probabilities in a very narrow range).
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -C &lt;integer&gt;
 *  The class for which threshold is determined. Valid values are:
 *  1, 2 (for first and second classes, respectively), 3 (for whichever
 *  class is least frequent), and 4 (for whichever class value is most
 *  frequent), and 5 (for the first class named any of "yes","pos(itive)"
 *  "1", or method 3 if no matches). (default 5).</pre>
 * 
 * <pre> -X &lt;number of folds&gt;
 *  Number of folds used for cross validation. If just a
 *  hold-out set is used, this determines the size of the hold-out set
 *  (default 3).</pre>
 * 
 * <pre> -R &lt;integer&gt;
 *  Sets whether confidence range correction is applied. This
 *  can be used to ensure the confidences range from 0 to 1.
 *  Use 0 for no range correction, 1 for correction based on
 *  the min/max values seen during threshold selection
 *  (default 0).</pre>
 * 
 * <pre> -E &lt;integer&gt;
 *  Sets the evaluation mode. Use 0 for
 *  evaluation using cross-validation,
 *  1 for evaluation using hold-out set,
 *  and 2 for evaluation on the
 *  training data (default 1).</pre>
 * 
 * <pre> -M [FMEASURE|ACCURACY|TRUE_POS|TRUE_NEG|TP_RATE|PRECISION|RECALL]
 *  Measure used for evaluation (default is FMEASURE).
 * </pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.functions.Logistic)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.functions.Logistic:
 * </pre>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -R &lt;ridge&gt;
 *  Set the ridge in the log-likelihood.</pre>
 * 
 * <pre> -M &lt;number&gt;
 *  Set the maximum number of iterations (default -1, until convergence).</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated sub-classifier. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.37 $ 
 */
public class ThresholdSelector 
  extends RandomizableSingleClassifierEnhancer 
  implements OptionHandler, Drawable {

  /** for serialization */
  static final long serialVersionUID = -1795038053239867444L;

  /** no range correction */
  public static final int RANGE_NONE = 0;
  /** Correct based on min/max observed */
  public static final int RANGE_BOUNDS = 1;
  /** Type of correction applied to threshold range */ 
  public static final Tag [] TAGS_RANGE = {
    new Tag(RANGE_NONE, "No range correction"),
    new Tag(RANGE_BOUNDS, "Correct based on min/max observed")
  };

  /** entire training set */
  public static final int EVAL_TRAINING_SET = 2;
  /** single tuned fold */
  public static final int EVAL_TUNED_SPLIT = 1;
  /** n-fold cross-validation */
  public static final int EVAL_CROSS_VALIDATION = 0;
  /** The evaluation modes */
  public static final Tag [] TAGS_EVAL = {
    new Tag(EVAL_TRAINING_SET, "Entire training set"),
    new Tag(EVAL_TUNED_SPLIT, "Single tuned fold"),
    new Tag(EVAL_CROSS_VALIDATION, "N-Fold cross validation")
  };

  /** first class value */
  public static final int OPTIMIZE_0     = 0;
  /** second class value */
  public static final int OPTIMIZE_1     = 1;
  /** least frequent class value */
  public static final int OPTIMIZE_LFREQ = 2;
  /** most frequent class value */
  public static final int OPTIMIZE_MFREQ = 3;
  /** class value name, either 'yes' or 'pos(itive)' */
  public static final int OPTIMIZE_POS_NAME = 4;
  /** How to determine which class value to optimize for */
  public static final Tag [] TAGS_OPTIMIZE = {
    new Tag(OPTIMIZE_0, "First class value"),
    new Tag(OPTIMIZE_1, "Second class value"),
    new Tag(OPTIMIZE_LFREQ, "Least frequent class value"),
    new Tag(OPTIMIZE_MFREQ, "Most frequent class value"),
    new Tag(OPTIMIZE_POS_NAME, "Class value named: \"yes\", \"pos(itive)\",\"1\"")
  };

  /** F-measure */
  public static final int FMEASURE  = 1;
  /** accuracy */
  public static final int ACCURACY  = 2;
  /** true-positive */
  public static final int TRUE_POS  = 3;
  /** true-negative */
  public static final int TRUE_NEG  = 4;
  /** true-positive rate */
  public static final int TP_RATE   = 5;
  /** precision */
  public static final int PRECISION = 6;
  /** recall */
  public static final int RECALL    = 7;
  /** the measure to use */
  public static final Tag[] TAGS_MEASURE = {
    new Tag(FMEASURE,  "FMEASURE"),
    new Tag(ACCURACY,  "ACCURACY"),
    new Tag(TRUE_POS,  "TRUE_POS"),
    new Tag(TRUE_NEG,  "TRUE_NEG"), 
    new Tag(TP_RATE,   "TP_RATE"),   
    new Tag(PRECISION, "PRECISION"), 
    new Tag(RECALL,    "RECALL")
  };

  /** The upper threshold used as the basis of correction */
  protected double m_HighThreshold = 1;

  /** The lower threshold used as the basis of correction */
  protected double m_LowThreshold = 0;

  /** The threshold that lead to the best performance */
  protected double m_BestThreshold = -Double.MAX_VALUE;

  /** The best value that has been observed */
  protected double m_BestValue = - Double.MAX_VALUE;
  
  /** The number of folds used in cross-validation */
  protected int m_NumXValFolds = 3;

  /** Designated class value, determined during building */
  protected int m_DesignatedClass = 0;

  /** Method to determine which class to optimize for */
  protected int m_ClassMode = OPTIMIZE_POS_NAME;

  /** The evaluation mode */
  protected int m_EvalMode = EVAL_TUNED_SPLIT;

  /** The range correction mode */
  protected int m_RangeMode = RANGE_NONE;

  /** evaluation measure used for determining threshold **/
  int m_nMeasure = FMEASURE;

  /** The minimum value for the criterion. If threshold adjustment
      yields less than that, the default threshold of 0.5 is used. */
  protected static final double MIN_VALUE = 0.05;
    
  /**
   * Constructor.
   */
  public ThresholdSelector() {
    
    m_Classifier = new weka.classifiers.functions.Logistic();
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.functions.Logistic";
  }

  /**
   * Collects the classifier predictions using the specified evaluation method.
   *
   * @param instances the set of <code>Instances</code> to generate
   * predictions for.
   * @param mode the evaluation mode.
   * @param numFolds the number of folds to use if not evaluating on the
   * full training set.
   * @return a <code>FastVector</code> containing the predictions.
   * @throws Exception if an error occurs generating the predictions.
   */
  protected FastVector getPredictions(Instances instances, int mode, int numFolds) 
    throws Exception {

    EvaluationUtils eu = new EvaluationUtils();
    eu.setSeed(m_Seed);
    
    switch (mode) {
    case EVAL_TUNED_SPLIT:
      Instances trainData = null, evalData = null;
      Instances data = new Instances(instances);
      Random random = new Random(m_Seed);
      data.randomize(random);
      data.stratify(numFolds);
      
      // Make sure that both subsets contain at least one positive instance
      for (int subsetIndex = 0; subsetIndex < numFolds; subsetIndex++) {
        trainData = data.trainCV(numFolds, subsetIndex, random);
        evalData = data.testCV(numFolds, subsetIndex);
        if (checkForInstance(trainData) && checkForInstance(evalData)) {
          break;
        }
      }
      return eu.getTrainTestPredictions(m_Classifier, trainData, evalData);
    case EVAL_TRAINING_SET:
      return eu.getTrainTestPredictions(m_Classifier, instances, instances);
    case EVAL_CROSS_VALIDATION:
      return eu.getCVPredictions(m_Classifier, instances, numFolds);
    default:
      throw new RuntimeException("Unrecognized evaluation mode");
    }
  }

    /** set measure used for determining threshold
     * @param newMeasure Tag representing measure to be used
     **/
    public void setMeasure(SelectedTag newMeasure) {
	if (newMeasure.getTags() == TAGS_MEASURE) {
	    m_nMeasure = newMeasure.getSelectedTag().getID();
	}
    }

    /** get measure used for determining threshold 
     * @return Tag representing measure used
     **/
    public SelectedTag getMeasure() {
	return new SelectedTag(m_nMeasure, TAGS_MEASURE);
    }


  /**
   * Finds the best threshold, this implementation searches for the
   * highest FMeasure. If no FMeasure higher than MIN_VALUE is found,
   * the default threshold of 0.5 is used.
   *
   * @param predictions a <code>FastVector</code> containing the predictions.
   */
  protected void findThreshold(FastVector predictions) {

    Instances curve = (new ThresholdCurve()).getCurve(predictions, m_DesignatedClass);

    double low = 1.0;
    double high = 0.0;

    //System.err.println(curve);
    if (curve.numInstances() > 0) {
      Instance maxInst = curve.instance(0);
      double maxValue = 0; 
      int index1 = 0;
      int index2 = 0;
      switch (m_nMeasure) {
        case FMEASURE:
          index1 = curve.attribute(ThresholdCurve.FMEASURE_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case TRUE_POS:
          index1 = curve.attribute(ThresholdCurve.TRUE_POS_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case TRUE_NEG:
          index1 = curve.attribute(ThresholdCurve.TRUE_NEG_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case TP_RATE:
          index1 = curve.attribute(ThresholdCurve.TP_RATE_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case PRECISION:
          index1 = curve.attribute(ThresholdCurve.PRECISION_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case RECALL:
          index1 = curve.attribute(ThresholdCurve.RECALL_NAME).index();
          maxValue = maxInst.value(index1);
          break;
        case ACCURACY:
          index1 = curve.attribute(ThresholdCurve.TRUE_POS_NAME).index();
          index2 = curve.attribute(ThresholdCurve.TRUE_NEG_NAME).index();
          maxValue = maxInst.value(index1) + maxInst.value(index2);
          break;
      }
      int indexThreshold = curve.attribute(ThresholdCurve.THRESHOLD_NAME).index();
      for (int i = 1; i < curve.numInstances(); i++) {
        Instance current = curve.instance(i);
        double currentValue = 0;
        if (m_nMeasure ==  ACCURACY) {
          currentValue= current.value(index1) + current.value(index2);
	  } else {
	      currentValue= current.value(index1);
	  }

	  if (currentValue> maxValue) {
	      maxInst = current;
	      maxValue = currentValue;
	  }
	  if (m_RangeMode == RANGE_BOUNDS) {
	      double thresh = current.value(indexThreshold);
	      if (thresh < low) {
		  low = thresh;
	      }
	      if (thresh > high) {
		  high = thresh;
	      }
	  }
      }
      if (maxValue > MIN_VALUE) {
        m_BestThreshold = maxInst.value(indexThreshold);
        m_BestValue = maxValue;
        //System.err.println("maxFM: " + maxFM);
      }
      if (m_RangeMode == RANGE_BOUNDS) {
	  m_LowThreshold = low;
	  m_HighThreshold = high;
        //System.err.println("Threshold range: " + low + " - " + high);
      }
    }

  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
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
	      "\tNumber of folds used for cross validation. If just a\n" +
	      "\thold-out set is used, this determines the size of the hold-out set\n" +
	      "\t(default 3).",
	      "X", 1, "-X <number of folds>"));
    
    newVector.addElement(new Option(
        "\tSets whether confidence range correction is applied. This\n" +
        "\tcan be used to ensure the confidences range from 0 to 1.\n" +
        "\tUse 0 for no range correction, 1 for correction based on\n" +
        "\tthe min/max values seen during threshold selection\n"+
        "\t(default 0).",
        "R", 1, "-R <integer>"));
    
    newVector.addElement(new Option(
	      "\tSets the evaluation mode. Use 0 for\n" +
	      "\tevaluation using cross-validation,\n" +
	      "\t1 for evaluation using hold-out set,\n" +
	      "\tand 2 for evaluation on the\n" +
	      "\ttraining data (default 1).",
	      "E", 1, "-E <integer>"));

    newVector.addElement(new Option(
	      "\tMeasure used for evaluation (default is FMEASURE).\n",
	      "M", 1, "-M [FMEASURE|ACCURACY|TRUE_POS|TRUE_NEG|TP_RATE|PRECISION|RECALL]"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -C &lt;integer&gt;
   *  The class for which threshold is determined. Valid values are:
   *  1, 2 (for first and second classes, respectively), 3 (for whichever
   *  class is least frequent), and 4 (for whichever class value is most
   *  frequent), and 5 (for the first class named any of "yes","pos(itive)"
   *  "1", or method 3 if no matches). (default 5).</pre>
   * 
   * <pre> -X &lt;number of folds&gt;
   *  Number of folds used for cross validation. If just a
   *  hold-out set is used, this determines the size of the hold-out set
   *  (default 3).</pre>
   * 
   * <pre> -R &lt;integer&gt;
   *  Sets whether confidence range correction is applied. This
   *  can be used to ensure the confidences range from 0 to 1.
   *  Use 0 for no range correction, 1 for correction based on
   *  the min/max values seen during threshold selection
   *  (default 0).</pre>
   * 
   * <pre> -E &lt;integer&gt;
   *  Sets the evaluation mode. Use 0 for
   *  evaluation using cross-validation,
   *  1 for evaluation using hold-out set,
   *  and 2 for evaluation on the
   *  training data (default 1).</pre>
   * 
   * <pre> -M [FMEASURE|ACCURACY|TRUE_POS|TRUE_NEG|TP_RATE|PRECISION|RECALL]
   *  Measure used for evaluation (default is FMEASURE).
   * </pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.functions.Logistic)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.functions.Logistic:
   * </pre>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -R &lt;ridge&gt;
   *  Set the ridge in the log-likelihood.</pre>
   * 
   * <pre> -M &lt;number&gt;
   *  Set the maximum number of iterations (default -1, until convergence).</pre>
   * 
   <!-- options-end -->
   *
   * Options after -- are passed to the designated sub-classifier. <p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String classString = Utils.getOption('C', options);
    if (classString.length() != 0) {
      setDesignatedClass(new SelectedTag(Integer.parseInt(classString) - 1, 
                                         TAGS_OPTIMIZE));
    } else {
      setDesignatedClass(new SelectedTag(OPTIMIZE_LFREQ, TAGS_OPTIMIZE));
    }

    String modeString = Utils.getOption('E', options);
    if (modeString.length() != 0) {
      setEvaluationMode(new SelectedTag(Integer.parseInt(modeString), 
                                         TAGS_EVAL));
    } else {
      setEvaluationMode(new SelectedTag(EVAL_TUNED_SPLIT, TAGS_EVAL));
    }

    String rangeString = Utils.getOption('R', options);
    if (rangeString.length() != 0) {
      setRangeCorrection(new SelectedTag(Integer.parseInt(rangeString), 
                                         TAGS_RANGE));
    } else {
      setRangeCorrection(new SelectedTag(RANGE_NONE, TAGS_RANGE));
    }

    String measureString = Utils.getOption('M', options);
    if (measureString.length() != 0) {
      setMeasure(new SelectedTag(measureString, TAGS_MEASURE));
    } else {
      setMeasure(new SelectedTag(FMEASURE, TAGS_MEASURE));
    }

    String foldsString = Utils.getOption('X', options);
    if (foldsString.length() != 0) {
      setNumXValFolds(Integer.parseInt(foldsString));
    } else {
      setNumXValFolds(3);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 10];

    int current = 0;

    options[current++] = "-C"; options[current++] = "" + (m_DesignatedClass + 1);
    options[current++] = "-X"; options[current++] = "" + getNumXValFolds();
    options[current++] = "-E"; options[current++] = "" + m_EvalMode;
    options[current++] = "-R"; options[current++] = "" + m_RangeMode;
    options[current++] = "-M"; options[current++] = "" + getMeasure().getSelectedTag().getReadable();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.BINARY_CLASS);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) 
    throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    AttributeStats stats = instances.attributeStats(instances.classIndex());
    m_BestThreshold = 0.5;
    m_BestValue = MIN_VALUE;
    m_HighThreshold = 1;
    m_LowThreshold = 0;
    // If data contains only one instance of positive data
    // optimize on training data
    if (stats.distinctCount != 2) {
      System.err.println("Couldn't find examples of both classes. No adjustment.");
      m_Classifier.buildClassifier(instances);
    } else {
      
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
          if (name.startsWith("yes") || name.equals("1") || 
              name.startsWith("pos")) {
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
      
      /*
        System.err.println("ThresholdSelector: Using mode=" 
        + TAGS_OPTIMIZE[m_ClassMode].getReadable());
        System.err.println("ThresholdSelector: Optimizing using class "
        + m_DesignatedClass + "/" 
        + instances.classAttribute().value(m_DesignatedClass));
      */
      
      
      if (stats.nominalCounts[m_DesignatedClass] == 1) {
        System.err.println("Only 1 positive found: optimizing on training data");
        findThreshold(getPredictions(instances, EVAL_TRAINING_SET, 0));
      } else {
        int numFolds = Math.min(m_NumXValFolds, stats.nominalCounts[m_DesignatedClass]);
        //System.err.println("Number of folds for threshold selector: " + numFolds);
        findThreshold(getPredictions(instances, m_EvalMode, numFolds));
        if (m_EvalMode != EVAL_TRAINING_SET) {
          m_Classifier.buildClassifier(instances);
        }
      }
    }
  }

  /**
   * Checks whether instance of designated class is in subset.
   * 
   * @param data the data to check for instance
   * @return true if the instance is in the subset
   * @throws Exception if checking fails
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
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {
    
    double [] pred = m_Classifier.distributionForInstance(instance);
    double prob = pred[m_DesignatedClass];

    // Warp probability
    if (prob > m_BestThreshold) {
      prob = 0.5 + (prob - m_BestThreshold) / 
        ((m_HighThreshold - m_BestThreshold) * 2);
    } else {
      prob = (prob - m_LowThreshold) / 
        ((m_BestThreshold - m_LowThreshold) * 2);
    }
    if (prob < 0) {
      prob = 0.0;
    } else if (prob > 1) {
      prob = 1.0;
    }

    // Alter the distribution
    pred[m_DesignatedClass] = prob;
    if (pred.length == 2) { // Handle case when there's only one class
      pred[(m_DesignatedClass + 1) % 2] = 1.0 - prob;
    }
    return pred;
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A metaclassifier that selecting a mid-point threshold on the "
      + "probability output by a Classifier. The midpoint "
      + "threshold is set so that a given performance measure is optimized. "
      + "Currently this is the F-measure. Performance is measured either on "
      + "the training data, a hold-out set or using cross-validation. In "
      + "addition, the probabilities returned by the base learner can "
      + "have their range expanded so that the output probabilities will "
      + "reside between 0 and 1 (this is useful if the scheme normally "
      + "produces probabilities in a very narrow range).";
  }
    
  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String designatedClassTipText() {

    return "Sets the class value for which the optimization is performed. "
      + "The options are: pick the first class value; pick the second "
      + "class value; pick whichever class is least frequent; pick whichever "
      + "class value is most frequent; pick the first class named any of "
      + "\"yes\",\"pos(itive)\", \"1\", or the least frequent if no matches).";
  }

  /**
   * Gets the method to determine which class value to optimize. Will
   * be one of OPTIMIZE_0, OPTIMIZE_1, OPTIMIZE_LFREQ, OPTIMIZE_MFREQ,
   * OPTIMIZE_POS_NAME.
   *
   * @return the class selection mode.
   */
  public SelectedTag getDesignatedClass() {

    return new SelectedTag(m_ClassMode, TAGS_OPTIMIZE);
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluationModeTipText() {

    return "Sets the method used to determine the threshold/performance "
      + "curve. The options are: perform optimization based on the entire "
      + "training set (may result in overfitting); perform an n-fold "
      + "cross-validation (may be time consuming); perform one fold of "
      + "an n-fold cross-validation (faster but likely less accurate).";
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

    return new SelectedTag(m_EvalMode, TAGS_EVAL);
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String rangeCorrectionTipText() {

    return "Sets the type of prediction range correction performed. "
      + "The options are: do not do any range correction; "
      + "expand predicted probabilities so that the minimum probability "
      + "observed during the optimization maps to 0, and the maximum "
      + "maps to 1 (values outside this range are clipped to 0 and 1).";
  }

  /**
   * Sets the confidence range correction mode used. Will be one of
   * RANGE_NONE, or RANGE_BOUNDS
   *
   * @param newMethod the new correciton mode.
   */
  public void setRangeCorrection(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_RANGE) {
      m_RangeMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Gets the confidence range correction mode used. Will be one of
   * RANGE_NONE, or RANGE_BOUNDS
   *
   * @return the confidence correction mode.
   */
  public SelectedTag getRangeCorrection() {

    return new SelectedTag(m_RangeMode, TAGS_RANGE);
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numXValFoldsTipText() {

    return "Sets the number of folds used during full cross-validation "
      + "and tuned fold evaluation. This number will be automatically "
      + "reduced if there are insufficient positive examples.";
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
    
    if (newNumFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be greater than 1");
    }
    m_NumXValFolds = newNumFolds;
  }

  /**
   * Returns the type of graph this classifier
   * represents.
   *  
   * @return the type of graph this classifier represents
   */   
  public int graphType() {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graphType();
    else 
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @throws Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + getClassifierSpec()
			     + " cannot be graphed");
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
    if (m_RangeMode == RANGE_BOUNDS) {
      result += "Expanding range [" + m_LowThreshold + "," + m_HighThreshold
        + "] to [0, 1]\n";
    }
    result += "Measure: " + getMeasure().getSelectedTag().getReadable() + "\n";
    result += m_Classifier.toString();
    return result;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new ThresholdSelector(), argv);
  }
}
