/*
 *    CostCurve.java
 *    Copyright (C) 2001 Mark Hall
 *
 */

package weka.classifiers.evaluation;

import weka.core.Utils;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.DistributionClassifier;

/**
 * Generates points illustrating probablity cost tradeoffs that can be 
 * obtained by varying the threshold value between classes. For example, 
 * the typical threshold value of 0.5 means the predicted probability of 
 * "positive" must be higher than 0.5 for the instance to be predicted as 
 * "positive".
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */

public class CostCurve {

  /** The name of the relation used in cost curve datasets */
  public final static String RELATION_NAME = "CostCurve";

  public final static String PROB_COST_FUNC_NAME = "Probability Cost Function";
  public final static String NORM_EXPECTED_COST_NAME = 
    "Normalized Expected Cost";
  public final static String THRESHOLD_NAME = "Threshold";

  /**
   * Calculates the performance stats for the default class and return 
   * results as a set of Instances. The
   * structure of these Instances is as follows:<p> <ul> 
   * <li> <b>Probability Cost Function </b>
   * <li> <b>Normalized Expected Cost</b>
   * <li> <b>Threshold</b> contains the probability threshold that gives
   * rise to the previous performance values. 
   * </ul> <p>
   *
   * @see TwoClassStats
   * @param classIndex index of the class of interest.
   * @return datapoints as a set of instances, null if no predictions
   * have been made.
   */
  public Instances getCurve(FastVector predictions) {

    if (predictions.size() == 0) {
      return null;
    }
    return getCurve(predictions, 
                    ((NominalPrediction)predictions.elementAt(0))
                    .distribution().length - 1);
  }

  /**
   * Calculates the performance stats for the desired class and return 
   * results as a set of Instances.
   *
   * @param classIndex index of the class of interest.
   * @return datapoints as a set of instances.
   */
  public Instances getCurve(FastVector predictions, int classIndex) {

    if ((predictions.size() == 0) ||
        (((NominalPrediction)predictions.elementAt(0))
         .distribution().length <= classIndex)) {
      return null;
    }
    
    ThresholdCurve tc = new ThresholdCurve();
    Instances threshInst = tc.getCurve(predictions, classIndex);

    Instances insts = makeHeader();
    int fpind = threshInst.attribute(ThresholdCurve.FP_RATE_NAME).index();
    int tpind = threshInst.attribute(ThresholdCurve.TP_RATE_NAME).index();
    int threshind = threshInst.attribute(ThresholdCurve.THRESHOLD_NAME).index();
    
    double [] vals;
    double fpval, tpval, thresh;
    for (int i = 0; i< threshInst.numInstances(); i++) {
      fpval = threshInst.instance(i).value(fpind);
      tpval = threshInst.instance(i).value(tpind);
      thresh = threshInst.instance(i).value(threshind);
      vals = new double [3];
      vals[0] = 0; vals[1] = fpval; vals[2] = thresh;
      insts.add(new Instance(1.0, vals));
      vals = new double [3];
      vals[0] = 1; vals[1] = 1.0 - tpval; vals[2] = thresh;
      insts.add(new Instance(1.0, vals));
    }
    
    return insts;
  }

  private Instances makeHeader() {

    FastVector fv = new FastVector();
    fv.addElement(new Attribute(PROB_COST_FUNC_NAME));
    fv.addElement(new Attribute(NORM_EXPECTED_COST_NAME));
    fv.addElement(new Attribute(THRESHOLD_NAME));
    return new Instances(RELATION_NAME, fv, 100);
  }

  /**
   * Tests the CostCurve generation from the command line.
   * The classifier is currently hardcoded. Pipe in an arff file.
   *
   * @param args currently ignored
   */
  public static void main(String [] args) {

    try {
      
      Instances inst = new Instances(new java.io.InputStreamReader(System.in));
      
      inst.setClassIndex(inst.numAttributes() - 1);
      CostCurve cc = new CostCurve();
      EvaluationUtils eu = new EvaluationUtils();
      DistributionClassifier classifier = new weka.classifiers.SMO();
      FastVector predictions = new FastVector();
      for (int i = 0; i < 2; i++) { // Do two runs.
	eu.setSeed(i);
	predictions.appendElements(eu.getCVPredictions(classifier, inst, 10));
	//System.out.println("\n\n\n");
      }
      Instances result = cc.getCurve(predictions);
      System.out.println(result);
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
