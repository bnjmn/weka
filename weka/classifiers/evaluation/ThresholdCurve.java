/*
 *    ThresholdCurve.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
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
package weka.classifiers.evaluation;

import weka.core.Utils;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.DistributionClassifier;

/**
 * Generates points illustrating prediction tradeoffs that can be obtained
 * by varying the threshold value between classes. For example, the typical 
 * threshold value of 0.5 means the predicted probability of "positive" must be
 * higher than 0.5 for the instance to be predicted as "positive". The 
 * resulting dataset can be used to visualize precision/recall tradeoff, or 
 * for ROC curve analysis (true positive rate vs false positive rate).
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.2 $
 */
public class ThresholdCurve {

  /**
   * Calculates the performance stats for the default class and return 
   * results as a set of Instances.
   *
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

    Instances insts = makeHeader();
    int totPos = 0, totNeg = 0;
    double [] probs = getProbabilities(predictions, classIndex);
    int [] sorted = Utils.sort(probs);

    // Get distribution of positive/negatives
    for (int i = 0; i < probs.length; i++) {
      NominalPrediction pred = (NominalPrediction)predictions.elementAt(i);
      if (pred.actual() == classIndex) {
        totPos += pred.weight();
      } else {
        totNeg += pred.weight();
      }
    }

    TwoClassStats tc = new TwoClassStats(totPos, totNeg, 0, 0);
    for (int i = 0; i < sorted.length; i++) {
      NominalPrediction pred = (NominalPrediction)predictions.elementAt(sorted[i]);
      if (pred.actual() == classIndex) {
        tc.setTruePositive(tc.getTruePositive() - pred.weight());
        tc.setFalseNegative(tc.getFalseNegative() + pred.weight());
      } else {
        tc.setFalsePositive(tc.getFalsePositive() - pred.weight());
        tc.setTrueNegative(tc.getTrueNegative() + pred.weight());
      }
      System.out.println(tc + " " + probs[sorted[i]] 
                         + " " + (pred.actual() == classIndex));
      if ((i == 0) || (i == (sorted.length - 1)) || 
          (probs[sorted[i]] != probs[sorted[i - 1]])) {
        insts.add(makeInstance(tc, probs[sorted[i]]));
      }
    }
    return insts;
  }

  private double [] getProbabilities(FastVector predictions, int classIndex) {

    // sort by predicted probability of the desired class.
    double [] probs = new double [predictions.size()];
    for (int i = 0; i < probs.length; i++) {
      NominalPrediction pred = (NominalPrediction)predictions.elementAt(i);
      probs[i] = pred.distribution()[classIndex];
    }
    return probs;
  }

  private Instances makeHeader() {

    FastVector fv = new FastVector();
    fv.addElement(new Attribute("True Positives"));
    fv.addElement(new Attribute("False Negatives"));
    fv.addElement(new Attribute("False Positives"));
    fv.addElement(new Attribute("True Negatives"));
    fv.addElement(new Attribute("False Positive Rate"));
    fv.addElement(new Attribute("True Positive Rate"));
    fv.addElement(new Attribute("Precision"));
    fv.addElement(new Attribute("Recall"));
    fv.addElement(new Attribute("Threshold"));
    return new Instances("Threshold Curve", fv, 100);
  }
  
  private Instance makeInstance(TwoClassStats tc, double prob) {

    int count = 0;
    double [] vals = new double[9];
    vals[count++] = tc.getTruePositive();
    vals[count++] = tc.getFalseNegative();
    vals[count++] = tc.getFalsePositive();
    vals[count++] = tc.getTrueNegative();
    vals[count++] = tc.getFalsePositiveRate();
    vals[count++] = tc.getTruePositiveRate();
    vals[count++] = tc.getPrecision();
    vals[count++] = tc.getRecall();
    vals[count++] = prob;
    return new Instance(1.0, vals);
  }
  
  /**
   * Tests the ThresholdCurve generation from the command line.
   * The classifier is currently hardcoded. Pipe in an arff file.
   *
   * @param args currently ignored
   */
  public static void main(String [] args) {

    try {
      Utils.SMALL = 0;
      Instances inst = new Instances(new java.io.InputStreamReader(System.in));
      inst.setClassIndex(inst.numAttributes() - 1);
      ThresholdCurve tc = new ThresholdCurve();
      EvaluationUtils eu = new EvaluationUtils();
      DistributionClassifier classifier = new weka.classifiers.SMO();
      FastVector predictions = new FastVector();
      for (int i = 0; i < 2; i++) {
        eu.setSeed(i);
        predictions.appendElements(eu.getCVPredictions(classifier, inst, 10));
        Instances result = tc.getCurve(predictions);
        System.out.println("\n\n\n");
        //System.out.println(result);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}



