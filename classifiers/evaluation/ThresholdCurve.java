package weka.classifiers.evaluation;

import weka.core.Utils;
import weka.core.FastVector;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.DistributionClassifier;
import java.util.Random;


public class ThresholdCurve {

  /** Number of runs to average over */
  private int m_Runs = 10;

  /** The classifiert to analyze */
  private DistributionClassifier m_Classifier;

  /** The full predictions for each run */
  private double [][][] m_Predictions;

  /** The class values */
  private int [][] m_Classes;

  public class TwoClassStats {

    double m_TruePos;
    double m_FalsePos;
    double m_TrueNeg;
    double m_FalseNeg;

    public TwoClassStats(double tp, double fp, double tn, double fn) {
      
      m_TruePos = tp; 
      m_FalsePos = fp;
      m_TrueNeg = tn; 
      m_FalseNeg = fn;
    }
    public void setTruePos(double tp) { m_TruePos = tp; }
    public void setFalsePos(double fp) { m_FalsePos = fp; }
    public void setTrueNeg(double tn) { m_TrueNeg = tn; }
    public void setFalseNeg(double fn) { m_FalseNeg = fn; }
    public double getTruePos() { return m_TruePos; }
    public double getFalsePos() { return m_FalsePos; }
    public double getTrueNeg() { return m_TrueNeg; }
    public double getFalseNeg() { return m_FalseNeg; }
    public double getTruePosRate() { 
      if (0 == (m_TruePos + m_FalseNeg)) {
        return 0;
      } else {
        return m_TruePos / (m_TruePos + m_FalseNeg); 
      }
    }
    public double getFalsePosRate() { 
      if (0 == (m_FalsePos + m_TrueNeg)) {
        return 0;
      } else {
        return m_FalsePos / (m_FalsePos + m_TrueNeg); 
      }
    }
    public double getPrecision() { 
      if (0 == (m_TruePos + m_FalsePos)) {
        return 0;
      } else {
        return m_TruePos / (m_TruePos + m_FalsePos); 
      }
    }
    public double getRecall() { return getTruePosRate(); }

    public String toString() {
      StringBuffer res = new StringBuffer();
      res.append(getTruePos()).append(' ');
      res.append(getFalseNeg()).append(' ');
      res.append(getTrueNeg()).append(' ');
      res.append(getFalsePos()).append(' ');
      res.append(getFalsePosRate()).append(' ');
      res.append(getTruePosRate()).append(' ');
      res.append(getPrecision()).append(' ');
      res.append(getRecall()).append(' ');
      return res.toString();
    }
  }


  /**
   * Gets the Classifier.
   *
   * @return the classifier.
   */
  public DistributionClassifier getClassifier() {

    return m_Classifier;
  }
  
  /**
   * Sets the Classifier (must have all options set).
   *
   * @param newClassifier the classifier to use during analysis.
   */
  public void setClassifier(DistributionClassifier newClassifier) {

    m_Classifier = newClassifier;
  }
  
  /**
   * Generate all the predictions ready for processing by performing a
   * cross-validation on the suuplied dataset.
   *
   * @param data the dataset
   * @param numFolds the number of folds in the cross-validation.
   * @exception Exception if an error occurs
   */
  public void process(Instances data, int numFolds) throws Exception {

    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class must be nominal.");
    }

    Instances runInstances = new Instances(data);
    Random random = new Random(1);
    m_Predictions = new double [m_Runs][data.numInstances()][data.numClasses()];
    m_Classes = new int [m_Runs][data.numInstances()];
    for (int run = 0; run < m_Runs; run++) {
      runInstances.randomize(random);
      runInstances.stratify(numFolds);
      int inst = 0;
      for (int fold = 0; fold < numFolds; fold++) {
	Instances train = runInstances.trainCV(numFolds, fold);
	Instances test = runInstances.testCV(numFolds, fold);
        
        m_Classifier.buildClassifier(train);
        for (int i = 0; i < test.numInstances(); i++) {
          Instance curr = test.instance(i);
          m_Predictions[run][inst] = m_Classifier.distributionForInstance(curr);
          m_Classes[run][inst++] = (int) curr.classValue();
        }
      } 
    }
  }


  /**
   * Calculates the performance stats for the default class and return 
   * results as a set of Instances.
   *
   * @param classIndex index of the class of interest.
   * @return datapoints as a set of instances, null if no predictions
   * have been made.
   */
  public Instances getCurve() {

    if ((m_Predictions == null) ||
        (m_Predictions[0] == null) ||
        (m_Predictions[0][0] == null)) {
      return null;
    }
    return getCurve(m_Predictions[0][0].length - 1);
  }

  /**
   * Calculates the performance stats for the desired class and return 
   * results as a set of Instances.
   *
   * @param classIndex index of the class of interest.
   * @return datapoints as a set of instances.
   */
  public Instances getCurve(int classIndex) {

    if ((m_Predictions == null) ||
        (m_Predictions[0] == null) ||
        (m_Predictions[0][0] == null) ||
        (m_Predictions[0][0].length <= classIndex)) {
      return null;
    }

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
    Instances insts = new Instances("Threshold Curve", fv, 100);

    int totPos = 0, totNeg = 0;
    for (int run = 0; run < m_Runs; run++) {

      // sort by predicted probability of the desired class.
      double [] probs = new double [m_Predictions[run].length];
      for (int i = 0; i < probs.length; i++) {
        probs[i] = m_Predictions[run][i][classIndex];
      }
      int [] sorted = Utils.sort(probs);

      if (run == 0) {   // Get class distribution
        for (int i = 0; i < probs.length; i++) {
          if (m_Classes[run][i] == classIndex) {
            totPos++;
          } else {
            totNeg++;
          }
        }
      }

      TwoClassStats tc = new TwoClassStats(totPos, totNeg, 0, 0);
      for (int i = 0; i < sorted.length; i++) {
        if (m_Classes[run][sorted[i]] == classIndex) {
          tc.setTruePos(tc.getTruePos() - 1);
          tc.setFalseNeg(tc.getFalseNeg() + 1);
        } else {
          tc.setFalsePos(tc.getFalsePos() - 1);
          tc.setTrueNeg(tc.getTrueNeg() + 1);
        }
        if ((i == 0) || (i == (sorted.length - 1)) || 
            (probs[sorted[i]] != probs[sorted[i - 1]])) {
          //System.out.println(tc + " " + probs[sorted[i]]);
          int count = 0;
          double [] vals = new double[9];
          vals[count++] = tc.getTruePos();
          vals[count++] = tc.getFalseNeg();
          vals[count++] = tc.getFalsePos();
          vals[count++] = tc.getTrueNeg();
          vals[count++] = tc.getFalsePosRate();
          vals[count++] = tc.getTruePosRate();
          vals[count++] = tc.getPrecision();
          vals[count++] = tc.getRecall();
          vals[count++] = probs[sorted[i]];
          insts.add(new Instance(1.0, vals));
        }
      }
    }
    return insts;
  }

  public static void main(String [] args) {

    try {
      Utils.SMALL = 0;
      Instances inst = new Instances(new java.io.InputStreamReader(System.in));
      inst.setClassIndex(inst.numAttributes() - 1);
      ThresholdCurve tc = new ThresholdCurve();
      tc.setClassifier(new weka.classifiers.SMO());
      tc.process(inst, 10);
      Instances result = tc.getCurve();
      System.out.println(result);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}



