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
 *    Winnow.java
 *    Copyright (C) 2002 J. Lindgren
 *
 */

package weka.classifiers.functions;

import weka.classifiers.*;
import weka.filters.*;
import weka.core.*;
import java.util.*;

/**
 * Implements Winnow and Balanced Winnow algorithms by
 * N. Littlestone. For more information, see<p>
 *
 * N. Littlestone (1988). <i> Learning quickly when irrelevant
 * attributes are abound: A new linear threshold algorithm</i>.
 * Machine Learning 2, pp. 285-318.<p>
 *
 * Valid options are:<p>
 *
 * -L <br>
 * Use the baLanced variant (default: false)<p>
 *
 * -I num <br>
 * The number of iterations to be performed. (default 1)<p>
 *
 * -A double <br>
 * Promotion coefficient alpha. (default 1.3)<p>
 *
 * -B double <br>
 * Demotion coefficient beta. (default 0.8)<p>
 *
 * -W double <br>
 * Starting weights of the prediction coeffs. (default 2.0)<p>
 *
 * -H double <br>
 * Prediction threshold. (default -1.0 == number of attributes)<p>
 *
 * -S int <br>
 * Random seed to shuffle the input. (default 1), -1 == no shuffling<p>
 *
 * @author J. Lindgren (jtlindgr<at>cs.helsinki.fi)
 * @version $Revision: 1.1 $ 
 */
public class Winnow extends Classifier implements OptionHandler,UpdateableClassifier {
  
  /** Use the balanced variant? **/
  protected boolean m_Balanced;
  
  /** The number of iterations **/
  protected int m_NumIterations = 1;

  /** The promotion coefficient **/
  protected double m_Alpha = 1.3;

  /** The demotion coefficient **/
  protected double m_Beta = 0.8;

  /** Threshold for the unbalanced algorithm **/
  protected double m_Threshold = -1.0;
  
  /** Random seed used for shuffling the dataset, -1 == disable **/
  protected int m_Seed = 1;

  /** Default weights for the prediction vector(s) **/
  protected double m_DefaultWeight = 2.0;
  
  /** The weight vectors for prediction**/
  private double[] predPosVector = null;
  private double[] predNegVector = null;

  /** The true threshold used for prediction **/
  private double actualThreshold;

  /** Length of the vectors (numAttributes()-1) **/
  private int predVectorLen = 0;

  /** The training instances */
  private Instances m_Train = null;

  /** The filter used to make attributes numeric. */
  private NominalToBinaryFilter m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValues;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);
    
    newVector.addElement(new Option("\tUse the baLanced version\n"
				    + "\t(default false)",
				    "L", 0, "-L"));
    newVector.addElement(new Option("\tThe number of iterations to be performed.\n"
				    + "\t(default 1)",
				    "I", 1, "-I <int>"));
    newVector.addElement(new Option("\tPromotion coefficient alpha.\n"
				    + "\t(default 1.3)",
				    "A", 1, "-A <double>"));
    newVector.addElement(new Option("\tDemotion coefficient beta.\n"
				    + "\t(default 0.85)",
				    "B", 1, "-B <double>"));
    newVector.addElement(new Option("\tPrediction threshold.\n"
				    + "\t(default -1.0 == number of attributes)",
				    "H", 1, "-H <double>"));
    newVector.addElement(new Option("\tDefault weight.\n"
				    + "\t(default 1.0)",
				    "W", 1, "-W <double>"));
    newVector.addElement(new Option("\tDefault random seed.\n"
				    + "\t(default 1)",
				    "S", 1, "-S <int>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    m_Balanced = Utils.getFlag('L', options);
    
    String iterationsString = Utils.getOption('I', options);
    if (iterationsString.length() != 0) {
      m_NumIterations = Integer.parseInt(iterationsString);
    }
    String alphaString = Utils.getOption('A', options);
    if (alphaString.length() != 0) { 
      m_Alpha = (new Double(alphaString)).doubleValue();
    }
    String betaString = Utils.getOption('B', options);
    if (betaString.length() != 0) {
      m_Beta = (new Double(betaString)).doubleValue();
    }
    String tString = Utils.getOption('H', options);
    if (tString.length() != 0) {
      m_Threshold = (new Double(tString)).doubleValue();
    }
    String wString = Utils.getOption('W', options);
    if (wString.length() != 0) {
      m_DefaultWeight = (new Double(wString)).doubleValue();
    }
    String rString = Utils.getOption('S', options);
    if (rString.length() != 0) {
      m_Seed = Integer.parseInt(rString);
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String [13];
    int current = 0;

    if(m_Balanced) {
      options[current++] = "-L"; 
    }

    options[current++] = "-I"; options[current++] = "" + m_NumIterations;
    options[current++] = "-A"; options[current++] = "" + m_Alpha;
    options[current++] = "-B"; options[current++] = "" + m_Beta;
    options[current++] = "-H"; options[current++] = "" + m_Threshold;
    options[current++] = "-W"; options[current++] = "" + m_DefaultWeight;
    options[current++] = "-S"; options[current++] = "" + m_Seed;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Builds the classifier
   *
   * @exception Exception if something goes wrong during building
   */
  public void buildClassifier(Instances insts) throws Exception {
    if (insts.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new Exception("Can't handle a numeric class!");
    }
    Enumeration enum = insts.enumerateAttributes();
    while (enum.hasMoreElements()) {
      Attribute attr = (Attribute) enum.nextElement();
      if (!attr.isNominal()) {
        throw new Exception("Winnow: only nominal attributes, please.");
      }
    }

    // Filter data
    m_Train = new Instances(insts);
    m_Train.deleteWithMissingClass();
    m_ReplaceMissingValues = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValues.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_ReplaceMissingValues);
    
    m_NominalToBinary = new NominalToBinaryFilter();
    m_NominalToBinary.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_NominalToBinary);

    /** Randomize training data */
    if(m_Seed!=-1)
      m_Train.randomize(new Random(m_Seed));

    /** Make space to store weight vectors */
    predVectorLen = m_Train.numAttributes()-1;
    predPosVector = new double[predVectorLen];

    if(m_Balanced)
      predNegVector = new double[predVectorLen];
    
    /** Init prediction vector(s) **/
    for(int it = 0 ; it < predVectorLen ; it++) {
      predPosVector[it]=m_DefaultWeight;
    }
       
    if(m_Balanced)
      for(int it = 0 ; it < predVectorLen ; it++) {
	predNegVector[it]=m_DefaultWeight;
      }
  
    /** Set actual prediction threshold **/
    if(m_Threshold<0)
      actualThreshold = (double)predVectorLen;
    else    
      actualThreshold = m_Threshold;

    /** Compute the weight vectors **/
    if(m_Balanced)
      for (int it = 0; it < m_NumIterations; it++) {
	for (int i = 0; i < m_Train.numInstances(); i++) {
	  actualUpdateClassifierBalanced(m_Train.instance(i));
	}
      }
    else
      for (int it = 0; it < m_NumIterations; it++) {
	for (int i = 0; i < m_Train.numInstances(); i++) {
	  actualUpdateClassifier(m_Train.instance(i));
	}
      }
  }
  
  /**
   * Updates the classifier with a new learning example
   *
   * @exception Exception if something goes wrong
   */
  public void updateClassifier(Instance instance) throws Exception {
        
    m_ReplaceMissingValues.input(instance);
    m_ReplaceMissingValues.batchFinished();
    instance = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(instance);
    m_NominalToBinary.batchFinished();
    instance = m_NominalToBinary.output();

    if(m_Balanced)
      actualUpdateClassifierBalanced(instance);
    else
      actualUpdateClassifier(instance);
  }
  
  /**
   * Actual update routine for prefiltered instances
   *
   * @exception Exception if something goes wrong
   */
  private void actualUpdateClassifier(Instance instance) throws Exception {
    
    double posmultiplier;

    if (!instance.classIsMissing()) {
      double prediction = makePrediction(instance);
        
      if (prediction != instance.classValue()) {
	if(prediction == 0)
	  {
	    /* promote */
	    posmultiplier=m_Alpha;
	  }
	else
	  {
	    /* demote */
	    posmultiplier=m_Beta;
	  }
	for(int l = 0 ; l < predVectorLen ; l++) {
	  if(instance.value(l)==1)
	    {
	      predPosVector[l]*=posmultiplier;
	    }
	}
      }
    }
    else
      System.out.println("CLASS MISSING");
  }
  
  /**
   * Actual update routine (balanced) for prefiltered instances
   *
   * @exception Exception if something goes wrong
   */
  private void actualUpdateClassifierBalanced(Instance instance) throws Exception {
    
    double posmultiplier,negmultiplier;

    if (!instance.classIsMissing()) {
      double prediction = makePredictionBalanced(instance);
        
      if (prediction != instance.classValue()) {
	if(prediction == 0)
	  {
	    /* promote positive, demote negative*/
	    posmultiplier=m_Alpha;
	    negmultiplier=m_Beta;
	  }
	else
	  {
	    /* demote positive, promote negative */
	    posmultiplier=m_Beta;
	    negmultiplier=m_Alpha;
	  }
	for(int l = 0 ; l < predVectorLen ; l++) {
	  if(instance.value(l)==1)
	    {
	      predPosVector[l]*=posmultiplier;
	      predNegVector[l]*=negmultiplier;
	    }
	}
      }
    }
    else
      System.out.println("CLASS MISSING");
  }

  /**
   * Outputs the prediction for the given instance.
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  public double classifyInstance(Instance inst) throws Exception {
    
    m_ReplaceMissingValues.input(inst);
    m_ReplaceMissingValues.batchFinished();
    inst = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(inst);
    m_NominalToBinary.batchFinished();
    inst = m_NominalToBinary.output();
    
    if(m_Balanced)
      return(makePredictionBalanced(inst));
    else    
      return(makePrediction(inst));
  }
  
  /** 
   * Compute the actual prediction for prefiltered instance
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  private double makePrediction(Instance inst) throws Exception {

    double total = 0;

    for(int i=0;i<predVectorLen;i++) {
      if(inst.value(i)==1) {
	total+=predPosVector[i];
      }
    }
      
    if(total>actualThreshold)
      return(1);
    else
      return(0);
  }
  
  /** 
   * Compute our prediction (Balanced) for prefiltered instance 
   *
   * @param inst the instance for which prediction is to be computed
   * @return the prediction
   * @exception Exception if something goes wrong
   */
  private double makePredictionBalanced(Instance inst) throws Exception {
    double total=0;

    for(int i=0;i<predVectorLen;i++)
      {
        if(inst.value(i)==1)
	  {
            total+=predPosVector[i]-predNegVector[i];
	  }
      }
     
    if(total>0)
      return(1);
    else
      return(0);
  }

  /**
   * Returns textual description of the classifier.
   */
  public String toString() {

    if(predVectorLen==0 || predPosVector==null)
      return("Winnow: No model built yet.");

    String result = "Winnow\n\nWeight vector(s)\n\n";
    result += "Pos: ";
    for( int i = 0 ; i < predVectorLen; i++) {
      result += predPosVector[i] + " ";
    }
    result += "\n\n";
    
    if(m_Balanced)
      {
        result+= "Neg: ";
        for( int i = 0 ; i < predVectorLen; i++) {
	  result += predNegVector[i] + " ";
        }
        result += "\n\n";
      }

    return(result);
  }
  
  /**
   * Get the value of Balanced.
   *
   * @return Value of Balanced.
   */
  public boolean getBalanced() {
    
    return m_Balanced;
  }
  
  /**
   * Set the value of Balanced.
   *
   * @param b  Value to assign to Balanced.
   */
  public void setBalanced(boolean b) {
    
    m_Balanced = b;
  }
  
  /**
   * Get the value of Alpha.
   *
   * @return Value of Alpha.
   */
  public double getAlpha() {
    
    return(m_Alpha);
  }
  
  /**
   * Set the value of Alpha.
   *
   * @param a  Value to assign to Alpha.
   */
  public void setAlpha(double a) {
    
    m_Alpha=a;
  }
  
  /**
   * Get the value of Beta.
   *
   * @return Value of Beta.
   */
  public double getBeta() {
    
    return(m_Beta);
  }
  
  /**
   * Set the value of Beta.
   *
   * @param b  Value to assign to Beta.
   */
  public void setBeta(double b) {
    
    m_Beta=b;
  }
  
  /**
   * Get the value of Threshold.
   *
   * @return Value of Threshold.
   */
  public double getThreshold() {
    
    return m_Threshold;
  }
  
  /**
   * Set the value of Threshold.
   *
   * @param t  Value to assign to Threshold.
   */
  public void setThreshold(double t) {
    
    m_Threshold = t;
  }
  
  /**
   * Get the value of DefaultWeight.
   *
   * @return Value of DefaultWeight.
   */
  public double getDefaultWeight() {
    
    return m_DefaultWeight;
  }
  
  /**
   * Set the value of DefaultWeight.
   *
   * @param w  Value to assign to DefaultWeight.
   */
  public void setDefaultWeight(double w) {
    
    m_DefaultWeight = w;
  }
  

  /**
   * Get the value of NumIterations.
   *
   * @return Value of NumIterations.
   */
  public int getNumIterations() {
    
    return m_NumIterations;
  }
  
  /**
   * Set the value of NumIterations.
   *
   * @param v  Value to assign to NumIterations.
   */
  public void setNumIterations(int v) {
    
    m_NumIterations = v;
  }

  /**
   * Get the value of Seed.
   *
   * @return Value of Seed.
   */
  public int getSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the value of Seed.
   *
   * @param v  Value to assign to Seed.
   */
  public void setSeed(int v) {
    
    m_Seed = v;
  }
  
  /**
   * Main method.
   */
  public static void main(String[] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new Winnow(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
    
