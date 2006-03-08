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
 * MILR.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Uses either standard or collective multi-instance assumption, but within linear regression. For the collective assumption, it offers arithmetic or geometric mean for the posteriors.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -R &lt;ridge&gt;
 *  Set the ridge in the log-likelihood.</pre>
 * 
 * <pre> -A [0|1|2]
 *  Defines the type of algorithm:
 *   0. standard MI assumption
 *   1. collective MI assumption, arithmetic mean for posteriors
 *   2. collective MI assumption, geometric mean for posteriors</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $ 
 */
public class MILR
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler {

  /** for serialization */
  static final long serialVersionUID = 1996101190172373826L;
  
  protected double[] m_Par;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** The ridge parameter. */
  protected double m_Ridge = 1e-6;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** MI data */ 
  protected double[][][] m_Data;

  /** All attribute names */
  protected Instances m_Attributes;

  protected double[] xMean = null, xSD = null;

  /** the type of processing */
  protected int m_AlgorithmType = ALGORITHMTYPE_DEFAULT;

  /** standard MI assumption */
  public static final int ALGORITHMTYPE_DEFAULT = 0;
  /** collective MI assumption, arithmetic mean for posteriors */
  public static final int ALGORITHMTYPE_ARITHMETIC = 1;
  /** collective MI assumption, geometric mean for posteriors */
  public static final int ALGORITHMTYPE_GEOMETRIC = 2;
  /** the types of algorithms */
  public static final Tag [] TAGS_ALGORITHMTYPE = {
    new Tag(ALGORITHMTYPE_DEFAULT, "standard MI assumption"),
    new Tag(ALGORITHMTYPE_ARITHMETIC, "collective MI assumption, arithmetic mean for posteriors"),
    new Tag(ALGORITHMTYPE_GEOMETRIC, "collective MI assumption, geometric mean for posteriors"),
  };

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Uses either standard or collective multi-instance assumption, but "
      + "within linear regression. For the collective assumption, it offers "
      + "arithmetic or geometric mean for the posteriors.";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    
    result.addElement(new Option(
          "\tTurn on debugging output.",
          "D", 0, "-D"));
    
    result.addElement(new Option(
        "\tSet the ridge in the log-likelihood.",
        "R", 1, "-R <ridge>"));

    result.addElement(new Option(
        "\tDefines the type of algorithm:\n"
        + "\t 0. standard MI assumption\n"
        + "\t 1. collective MI assumption, arithmetic mean for posteriors\n"
        + "\t 2. collective MI assumption, geometric mean for posteriors",
        "A", 1, "-A [0|1|2]"));

    return result.elements();
  }

  /**
   * Parses a given list of options. 
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;

    setDebug(Utils.getFlag('D', options));

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) 
      setRidge(Double.parseDouble(tmpStr));
    else 
      setRidge(1.0e-6);

    tmpStr = Utils.getOption('A', options);
    if (tmpStr.length() != 0) {
      setAlgorithmType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_ALGORITHMTYPE));
    } else {
      setAlgorithmType(new SelectedTag(ALGORITHMTYPE_DEFAULT, TAGS_ALGORITHMTYPE));
    }     
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    
    result = new Vector();

    if (getDebug())
      result.add("-D");
    
    result.add("-R");
    result.add("" + getRidge());
    
    result.add("-A");
    result.add("" + m_AlgorithmType);

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "The ridge in the log-likelihood.";
  }

  /**
   * Sets the ridge in the log-likelihood.
   *
   * @param ridge the ridge
   */
  public void setRidge(double ridge) {
    m_Ridge = ridge;
  }

  /**
   * Gets the ridge in the log-likelihood.
   *
   * @return the ridge
   */
  public double getRidge() {
    return m_Ridge;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String algorithmTypeTipText() {
    return "The mean type for the posteriors.";
  }

  /**
   * Gets the type of algorithm.
   *
   * @return the algorithm type
   */
  public SelectedTag getAlgorithmType() {
    return new SelectedTag(m_AlgorithmType, TAGS_ALGORITHMTYPE);
  }

  /**
   * Sets the algorithm type.
   *
   * @param newType the new algorithm type
   */
  public void setAlgorithmType(SelectedTag newType) {
    if (newType.getTags() == TAGS_ALGORITHMTYPE) {
      m_AlgorithmType = newType.getSelectedTag().getID();
    }
  }

  private class OptEng 
    extends Optimization {
    
    /** the type to use 
     * @see MILR#TAGS_ALGORITHMTYPE */
    private int m_Type;
    
    /**
     * initializes the object
     * 
     * @param type      the type top use
     * @see MILR#TAGS_ALGORITHMTYPE
     */
    public OptEng(int type) {
      super();
      
      m_Type = type;
    }
    
    /** 
     * Evaluate objective function
     * @param x the current values of variables
     * @return the value of the objective function 
     */
    protected double objectiveFunction(double[] x){
      double nll = 0; // -LogLikelihood
      
      switch (m_Type) {
        case ALGORITHMTYPE_DEFAULT:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag
            double bag = 0.0, // NLL of each bag 
                   prod = 0.0;   // Log-prob. 

            for(int j=0; j<nI; j++){
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];
              exp = Math.exp(exp);

              if(m_Classes[i]==1)
                prod -= Math.log(1.0+exp);
              else
                bag += Math.log(1.0+exp);
            }

            if(m_Classes[i]==1)
              bag = -Math.log(1.0-Math.exp(prod));

            nll += bag;
          }   
          break;
        
        case ALGORITHMTYPE_ARITHMETIC:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag
            double bag = 0;  // NLL of each bag

            for(int j=0; j<nI; j++){
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];
              exp = Math.exp(exp);

              if(m_Classes[i] == 1)
                bag += 1.0-1.0/(1.0+exp); // To avoid exp infinite
              else
                bag += 1.0/(1.0+exp);                  
            }   
            bag /= (double)nI;

            nll -= Math.log(bag);
          }   
          break;
          
        case ALGORITHMTYPE_GEOMETRIC:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag
            double bag = 0;   // Log-prob. 

            for(int j=0; j<nI; j++){
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];

              if(m_Classes[i]==1)
                bag -= exp/(double)nI;
              else
                bag += exp/(double)nI;
            }

            nll += Math.log(1.0+Math.exp(bag));
          }   
          break;
      }

      // ridge: note that intercepts NOT included
      for(int r=1; r<x.length; r++)
        nll += m_Ridge*x[r]*x[r];

      return nll;
    }

    /** 
     * Evaluate Jacobian vector
     * @param x the current values of variables
     * @return the gradient vector 
     */
    protected double[] evaluateGradient(double[] x){
      double[] grad = new double[x.length];
      
      switch (m_Type) {
        case ALGORITHMTYPE_DEFAULT:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag

            double denom = 0.0; // denominator, in log-scale       
            double[] bag = new double[grad.length]; //gradient update with ith bag

            for(int j=0; j<nI; j++){
              // Compute exp(b0+b1*Xi1j+...)/[1+exp(b0+b1*Xi1j+...)]
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];
              exp = Math.exp(exp)/(1.0+Math.exp(exp));

              if(m_Classes[i]==1)
                // Bug fix: it used to be denom += Math.log(1.0+exp);
                // Fixed 21 Jan 2005 (Eibe)
                denom -= Math.log(1.0-exp);

              // Instance-wise update of dNLL/dBk
              for(int p=0; p<x.length; p++){  // pth variable
                double m = 1.0;
                if(p>0) m=m_Data[i][p-1][j];
                bag[p] += m*exp;
              }     
            }

            denom = Math.exp(denom);

            // Bag-wise update of dNLL/dBk
            for(int q=0; q<grad.length; q++){
              if(m_Classes[i]==1)
                grad[q] -= bag[q]/(denom-1.0);
              else
                grad[q] += bag[q];
            }   
          }
          break;
        
        case ALGORITHMTYPE_ARITHMETIC:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag 

            double denom=0.0;
            double[] numrt = new double[x.length];

            for(int j=0; j<nI; j++){
              // Compute exp(b0+b1*Xi1j+...)/[1+exp(b0+b1*Xi1j+...)]
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];
              exp = Math.exp(exp);
              if(m_Classes[i]==1)
                denom += exp/(1.0+exp);
              else
                denom += 1.0/(1.0+exp);      

              // Instance-wise update of dNLL/dBk
              for(int p=0; p<x.length; p++){  // pth variable
                double m = 1.0;
                if(p>0) m=m_Data[i][p-1][j];
                numrt[p] += m*exp/((1.0+exp)*(1.0+exp));   
              }     
            }

            // Bag-wise update of dNLL/dBk
            for(int q=0; q<grad.length; q++){
              if(m_Classes[i]==1)
                grad[q] -= numrt[q]/denom;
              else
                grad[q] += numrt[q]/denom;          
            }
          }
          break;

        case ALGORITHMTYPE_GEOMETRIC:
          for(int i=0; i<m_Classes.length; i++){ // ith bag
            int nI = m_Data[i][0].length; // numInstances in ith bag    
            double bag = 0;
            double[] sumX = new double[x.length];
            for(int j=0; j<nI; j++){
              // Compute exp(b0+b1*Xi1j+...)/[1+exp(b0+b1*Xi1j+...)]
              double exp=0.0;
              for(int k=m_Data[i].length-1; k>=0; k--)
                exp += m_Data[i][k][j]*x[k+1];
              exp += x[0];

              if(m_Classes[i]==1){
                bag -= exp/(double)nI;
                for(int q=0; q<grad.length; q++){
                  double m = 1.0;
                  if(q>0) m=m_Data[i][q-1][j];
                  sumX[q] -= m/(double)nI;
                }
              }
              else{
                bag += exp/(double)nI;
                for(int q=0; q<grad.length; q++){
                  double m = 1.0;
                  if(q>0) m=m_Data[i][q-1][j];
                  sumX[q] += m/(double)nI;
                }     
              }
            }

            for(int p=0; p<x.length; p++)
              grad[p] += Math.exp(bag)*sumX[p]/(1.0+Math.exp(bag));
          }
          break;
      }

      // ridge: note that intercepts NOT included
      for(int r=1; r<x.length; r++){
        grad[r] += 2.0*m_Ridge*x[r];
      }

      return grad;
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    
    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param train the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances train) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(train);

    // remove instances with missing class
    train = new Instances(train);
    train.deleteWithMissingClass();

    m_NumClasses = train.numClasses();

    int nR = train.attribute(1).relation().numAttributes();
    int nC = train.numInstances();

    m_Data  = new double [nC][nR][];              // Data values
    m_Classes  = new int [nC];                    // Class values
    m_Attributes = train.attribute(1).relation();

    xMean = new double [nR];             // Mean of mean
    xSD   = new double [nR];             // Mode of stddev

    double sY1=0, sY0=0, totIns=0;                          // Number of classes
    int[] missingbags = new int[nR];

    if (m_Debug) {
      System.out.println("Extracting data...");
    }

    for(int h=0; h<m_Data.length; h++){
      Instance current = train.instance(h);
      m_Classes[h] = (int)current.classValue();  // Class value starts from 0
      Instances currInsts = current.relationalValue(1);
      int nI = currInsts.numInstances();
      totIns += (double)nI;

      for (int i = 0; i < nR; i++) {  		
        // initialize m_data[][][]		
        m_Data[h][i] = new double[nI];
        double avg=0, std=0, num=0;
        for (int k=0; k<nI; k++){
          if(!currInsts.instance(k).isMissing(i)){
            m_Data[h][i][k] = currInsts.instance(k).value(i);
            avg += m_Data[h][i][k];
            std += m_Data[h][i][k]*m_Data[h][i][k];
            num++;
          }
          else
            m_Data[h][i][k] = Double.NaN;
        }
        
        if(num > 0){
          xMean[i] += avg/num;
          xSD[i] += std/num;
        }
        else
          missingbags[i]++;
      }	    

      // Class count	
      if (m_Classes[h] == 1)
        sY1++;
      else
        sY0++;
    }

    for (int j = 0; j < nR; j++) {
      xMean[j] = xMean[j]/(double)(nC-missingbags[j]);
      xSD[j] = Math.sqrt(Math.abs(xSD[j]/((double)(nC-missingbags[j])-1.0)
            -xMean[j]*xMean[j]*(double)(nC-missingbags[j])/
            ((double)(nC-missingbags[j])-1.0)));
    }

    if (m_Debug) {	    
      // Output stats about input data
      System.out.println("Descriptives...");
      System.out.println(sY0 + " bags have class 0 and " +
          sY1 + " bags have class 1");
      System.out.println("\n Variable     Avg       SD    ");
      for (int j = 0; j < nR; j++) 
        System.out.println(Utils.doubleToString(j,8,4) 
            + Utils.doubleToString(xMean[j], 10, 4) 
            + Utils.doubleToString(xSD[j], 10,4));
    }

    // Normalise input data and remove ignored attributes
    for (int i = 0; i < nC; i++) {
      for (int j = 0; j < nR; j++) {
        for(int k=0; k < m_Data[i][j].length; k++){
          if(xSD[j] != 0){
            if(!Double.isNaN(m_Data[i][j][k]))
              m_Data[i][j][k] = (m_Data[i][j][k] - xMean[j]) / xSD[j];
            else
              m_Data[i][j][k] = 0;
          }
        }
      }
    }

    if (m_Debug) {
      System.out.println("\nIteration History..." );
    }

    double x[] = new double[nR + 1];
    x[0] =  Math.log((sY1+1.0) / (sY0+1.0));
    double[][] b = new double[2][x.length];
    b[0][0] = Double.NaN;
    b[1][0] = Double.NaN;
    for (int q=1; q < x.length;q++){
      x[q] = 0.0;		
      b[0][q] = Double.NaN;
      b[1][q] = Double.NaN;
    }

    OptEng opt = new OptEng(m_AlgorithmType);	
    opt.setDebug(m_Debug);
    m_Par = opt.findArgmin(x, b);
    while(m_Par==null){
      m_Par = opt.getVarbValues();
      if (m_Debug)
        System.out.println("200 iterations finished, not enough!");
      m_Par = opt.findArgmin(m_Par, b);
    }
    if (m_Debug)
      System.out.println(" -------------<Converged>--------------");

    // feature selection use
    if (m_AlgorithmType == ALGORITHMTYPE_ARITHMETIC) {
      double[] fs = new double[nR];
      for(int k=1; k<nR+1; k++)
        fs[k-1] = Math.abs(m_Par[k]);
      int[] idx = Utils.sort(fs);
      double max = fs[idx[idx.length-1]];
      for(int k=idx.length-1; k>=0; k--)
        System.out.println(m_Attributes.attribute(idx[k]).name()+"\t"+(fs[idx[k]]*100/max));
    }

    // Convert coefficients back to non-normalized attribute units
    for(int j = 1; j < nR+1; j++) {
      if (xSD[j-1] != 0) {
        m_Par[j] /= xSD[j-1];
        m_Par[0] -= m_Par[j] * xMean[j-1];
      }
    }
  }		

  /**
   * Computes the distribution for a given exemplar
   *
   * @param exmp the exemplar for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance exmp) 
    throws Exception {

    // Extract the data
    Instances ins = exmp.relationalValue(1);
    int nI = ins.numInstances(), nA = ins.numAttributes();
    double[][] dat = new double [nI][nA+1];
    for(int j=0; j<nI; j++){
      dat[j][0]=1.0;
      int idx=1;
      for(int k=0; k<nA; k++){ 
        if(!ins.instance(j).isMissing(k))
          dat[j][idx] = ins.instance(j).value(k);
        else
          dat[j][idx] = xMean[idx-1];
        idx++;
      }
    }

    // Compute the probability of the bag
    double [] distribution = new double[2];
    switch (m_AlgorithmType) {
      case ALGORITHMTYPE_DEFAULT:
        distribution[0]=0.0;  // Log-Prob. for class 0

        for(int i=0; i<nI; i++){
          double exp = 0.0; 
          for(int r=0; r<m_Par.length; r++)
            exp += m_Par[r]*dat[i][r];
          exp = Math.exp(exp);

          // Prob. updated for one instance
          distribution[0] -= Math.log(1.0+exp);
        }

        // Prob. for class 0
        distribution[0] = Math.exp(distribution[0]);
        // Prob. for class 1
        distribution[1] = 1.0 - distribution[0];
        break;
      
      case ALGORITHMTYPE_ARITHMETIC:
        distribution[0]=0.0;  // Prob. for class 0

        for(int i=0; i<nI; i++){
          double exp = 0.0;
          for(int r=0; r<m_Par.length; r++)
            exp += m_Par[r]*dat[i][r];
          exp = Math.exp(exp);

          // Prob. updated for one instance
          distribution[0] += 1.0/(1.0+exp);
        }

        // Prob. for class 0
        distribution[0] /= (double)nI;
        // Prob. for class 1
        distribution[1] = 1.0 - distribution[0];
        break;

      case ALGORITHMTYPE_GEOMETRIC:
        for(int i=0; i<nI; i++){
          double exp = 0.0;
          for(int r=0; r<m_Par.length; r++)
            exp += m_Par[r]*dat[i][r];
          distribution[1] += exp/(double)nI; 
        }

        // Prob. for class 1
        distribution[1] = 1.0/(1.0+Math.exp(-distribution[1]));
        // Prob. for class 0
        distribution[0] = 1-distribution[1];
        break;
    }

    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {

    String result = "Modified Logistic Regression";
    if (m_Par == null) {
      return result + ": No model built yet.";
    }

    result += "\nMean type: " + getAlgorithmType().getSelectedTag().getReadable() + "\n";
    result += "\nCoefficients...\n"
      + "Variable      Coeff.\n";
    for (int j = 1, idx=0; j < m_Par.length; j++, idx++) {
      result += m_Attributes.attribute(idx).name();
      result += " "+Utils.doubleToString(m_Par[j], 12, 4); 
      result += "\n";
    }

    result += "Intercept:";
    result += " "+Utils.doubleToString(m_Par[0], 10, 4); 
    result += "\n";

    result += "\nOdds Ratios...\n"
      + "Variable         O.R.\n";
    for (int j = 1, idx=0; j < m_Par.length; j++, idx++) {
      result += " " + m_Attributes.attribute(idx).name(); 
      double ORc = Math.exp(m_Par[j]);
      result += " " + ((ORc > 1e10) ?  "" + ORc : Utils.doubleToString(ORc, 12, 4));
    }
    result += "\n";
    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new MILR(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
