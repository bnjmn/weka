
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
 * MIDD.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Capabilities;
import weka.core.FastVector;
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
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Re-implement the Diverse Density algorithm, changes the testing procedure.
 * <p/>
 * 
 * More information about DD:<br/>
 * Oded Maron (1998), Learning from ambiguity. PhD thesis, 
 * Massachusetts Institute of Technology, United States. <br/>
 * Maron, O. & Lozano-Perez, T. (1998). A framework for multiple-instance 
 * learning. 
 * <p/>
 * 
 * Valid options are:<p/>
 *
 * -D <br/>
 * Turn on debugging output.<p/>
 *
 * -N 0|1|2 <br/>
 * Whether to 0=normalize/1=standardize/2=neither. (default 1=standardize) <p/>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */

public class MIDD 
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler {

  static final long serialVersionUID = 4263507733600536168L;
  
  /** The index of the class attribute */
  protected int m_ClassIndex;

  protected double[] m_Par;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** MI data */ 
  protected double[][][] m_Data;

  /** All attribute names */
  protected Instances m_Attributes;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither, default:standardize */
  protected int m_filterType = FILTER_STANDARDIZE;

  /** The filter to apply to the training data */
  public static final int FILTER_NORMALIZE = 0;
  public static final int FILTER_STANDARDIZE = 1;
  public static final int FILTER_NONE = 2;
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"),
  };

  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing = new ReplaceMissingValues();

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Re-implement the Diverse Density algorithm, changes the testing "
      + "procedure.\n\n"
      + "More information about DD:\n"
      + "Oded Maron (1998), Learning from ambiguity. PhD thesis, "
      + "Massachusetts Institute of Technology, United States.\n"
      + "Maron, O. & Lozano-Perez, T. (1998). A framework for multiple-instance "
      + "learning.";
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
          "\tWhether to 0=normalize/1=standardize/2=neither.\n"
          + "\t(default 1=standardize)",
          "N", 1, "-N <num>"));

    return result.elements();
  }

  /**
   * Parses a given list of options. 
   *     
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));

    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_STANDARDIZE, TAGS_FILTER));
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
    
    result.add("-N");
    result.add("" + m_filterType);

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "The filter type for transforming the training data.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  private class OptEng 
    extends Optimization {

    /** 
     * Evaluate objective function
     * @param x the current values of variables
     * @return the value of the objective function 
     */
    protected double objectiveFunction(double[] x){
      double nll = 0; // -LogLikelihood
      for(int i=0; i<m_Classes.length; i++){ // ith bag
        int nI = m_Data[i][0].length; // numInstances in ith bag
        double bag = 0.0;  // NLL of pos bag

        for(int j=0; j<nI; j++){
          double ins=0.0;
          for(int k=0; k<m_Data[i].length; k++)
            ins += (m_Data[i][k][j]-x[k*2])*(m_Data[i][k][j]-x[k*2])*
              x[k*2+1]*x[k*2+1];
          ins = Math.exp(-ins);
          ins = 1.0-ins;

          if(m_Classes[i] == 1)
            bag += Math.log(ins);
          else{
            if(ins<=m_Zero) ins=m_Zero;
            nll -= Math.log(ins);
          }   
        }		

        if(m_Classes[i] == 1){
          bag = 1.0 - Math.exp(bag);
          if(bag<=m_Zero) bag=m_Zero;
          nll -= Math.log(bag);
        }
      }		
      return nll;
    }

    /** 
     * Evaluate Jacobian vector
     * @param x the current values of variables
     * @return the gradient vector 
     */
    protected double[] evaluateGradient(double[] x){
      double[] grad = new double[x.length];
      for(int i=0; i<m_Classes.length; i++){ // ith bag
        int nI = m_Data[i][0].length; // numInstances in ith bag 

        double denom=0.0;	
        double[] numrt = new double[x.length];

        for(int j=0; j<nI; j++){
          double exp=0.0;
          for(int k=0; k<m_Data[i].length; k++)
            exp += (m_Data[i][k][j]-x[k*2])*(m_Data[i][k][j]-x[k*2])
              *x[k*2+1]*x[k*2+1];			
          exp = Math.exp(-exp);
          exp = 1.0-exp;
          if(m_Classes[i]==1)
            denom += Math.log(exp);		   		    

          if(exp<=m_Zero) exp=m_Zero;
          // Instance-wise update
          for(int p=0; p<m_Data[i].length; p++){  // pth variable
            numrt[2*p] += (1.0-exp)*2.0*(x[2*p]-m_Data[i][p][j])*x[p*2+1]*x[p*2+1]
              /exp;
            numrt[2*p+1] += 2.0*(1.0-exp)*(x[2*p]-m_Data[i][p][j])*(x[2*p]-m_Data[i][p][j])
              *x[p*2+1]/exp;
          }					    
        }		    

        // Bag-wise update 
        denom = 1.0-Math.exp(denom);
        if(denom <= m_Zero) denom = m_Zero;
        for(int q=0; q<m_Data[i].length; q++){
          if(m_Classes[i]==1){
            grad[2*q] += numrt[2*q]*(1.0-denom)/denom;
            grad[2*q+1] += numrt[2*q+1]*(1.0-denom)/denom;
          }else{
            grad[2*q] -= numrt[2*q];
            grad[2*q+1] -= numrt[2*q+1];
          }
        }
      } // one bag

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
    
    m_ClassIndex = train.classIndex();
    m_NumClasses = train.numClasses();

    int nR = train.attribute(1).relation().numAttributes();
    int nC = train.numInstances();
    FastVector maxSzIdx=new FastVector();
    int maxSz=0;
    int [] bagSize=new int [nC];
    Instances datasets= new Instances(train.attribute(1).relation(),0);

    m_Data  = new double [nC][nR][];              // Data values
    m_Classes  = new int [nC];                    // Class values
    m_Attributes = datasets.stringFreeStructure();	
    if (m_Debug) {
      System.out.println("Extracting data...");
    }

    for(int h=0; h<nC; h++)  {//h_th bag
      Instance current = train.instance(h);
      m_Classes[h] = (int)current.classValue();  // Class value starts from 0
      Instances currInsts = current.relationalValue(1);
      for (int i=0; i<currInsts.numInstances();i++){
        Instance inst=currInsts.instance(i);
        datasets.add(inst);
      }

      int nI = currInsts.numInstances();
      bagSize[h]=nI;
      if(m_Classes[h]==1){  
        if(nI>maxSz){
          maxSz=nI;
          maxSzIdx=new FastVector(1);
          maxSzIdx.addElement(new Integer(h));
        }
        else if(nI == maxSz)
          maxSzIdx.addElement(new Integer(h));
      }

    }

    /* filter the training data */
    if (m_filterType == FILTER_STANDARDIZE)  
      m_Filter = new Standardize();
    else if (m_filterType == FILTER_NORMALIZE)
      m_Filter = new Normalize();
    else 
      m_Filter = null; 

    if (m_Filter!=null) {
      m_Filter.setInputFormat(datasets);
      datasets = Filter.useFilter(datasets, m_Filter); 	
    }

    m_Missing.setInputFormat(datasets);
    datasets = Filter.useFilter(datasets, m_Missing);


    int instIndex=0;
    int start=0;	
    for(int h=0; h<nC; h++)  {	
      for (int i = 0; i < datasets.numAttributes(); i++) {
        // initialize m_data[][][]
        m_Data[h][i] = new double[bagSize[h]];
        instIndex=start;
        for (int k=0; k<bagSize[h]; k++){
          m_Data[h][i][k]=datasets.instance(instIndex).value(i);
          instIndex ++;
        }
      }
      start=instIndex;
    }


    if (m_Debug) {
      System.out.println("\nIteration History..." );
    }

    double[] x = new double[nR*2], tmp = new double[x.length];
    double[][] b = new double[2][x.length]; 

    OptEng opt;
    double nll, bestnll = Double.MAX_VALUE;
    for (int t=0; t<x.length; t++){
      b[0][t] = Double.NaN; 
      b[1][t] = Double.NaN;
    }

    // Largest Positive exemplar
    for(int s=0; s<maxSzIdx.size(); s++){
      int exIdx = ((Integer)maxSzIdx.elementAt(s)).intValue();
      for(int p=0; p<m_Data[exIdx][0].length; p++){
        for (int q=0; q < nR;q++){
          x[2*q] = m_Data[exIdx][q][p];  // pick one instance
          x[2*q+1] = 1.0;
        }

        opt = new OptEng();	
        //opt.setDebug(m_Debug);
        tmp = opt.findArgmin(x, b);
        while(tmp==null){
          tmp = opt.getVarbValues();
          if (m_Debug)
            System.out.println("200 iterations finished, not enough!");
          tmp = opt.findArgmin(tmp, b);
        }
        nll = opt.getMinFunction();

        if(nll < bestnll){
          bestnll = nll;
          m_Par = tmp;
          tmp = new double[x.length]; // Save memory
          if (m_Debug)
            System.out.println("!!!!!!!!!!!!!!!!Smaller NLL found: "+nll);
        }
        if (m_Debug)
          System.out.println(exIdx+":  -------------<Converged>--------------");
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
    if(m_Filter!=null)
      ins = Filter.useFilter(ins, m_Filter);

    ins = Filter.useFilter(ins, m_Missing);

    int nI = ins.numInstances(), nA = ins.numAttributes();
    double[][] dat = new double [nI][nA];
    for(int j=0; j<nI; j++){
      for(int k=0; k<nA; k++){ 
        dat[j][k] = ins.instance(j).value(k);
      }
    }

    // Compute the probability of the bag
    double [] distribution = new double[2];
    distribution[0]=0.0;  // log-Prob. for class 0

    for(int i=0; i<nI; i++){
      double exp = 0.0;
      for(int r=0; r<nA; r++)
        exp += (m_Par[r*2]-dat[i][r])*(m_Par[r*2]-dat[i][r])*
          m_Par[r*2+1]*m_Par[r*2+1];
      exp = Math.exp(-exp);

      // Prob. updated for one instance
      distribution[0] += Math.log(1.0-exp);
    }

    distribution[0] = Math.exp(distribution[0]);
    distribution[1] = 1.0-distribution[0];

    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {

    //double CSq = m_LLn - m_LL;
    //int df = m_NumPredictors;
    String result = "Diverse Density";
    if (m_Par == null) {
      return result + ": No model built yet.";
    }

    result += "\nCoefficients...\n"
      + "Variable       Point       Scale\n";
    for (int j = 0, idx=0; j < m_Par.length/2; j++, idx++) {
      result += m_Attributes.attribute(idx).name();
      result += " "+Utils.doubleToString(m_Par[j*2], 12, 4); 
      result += " "+Utils.doubleToString(m_Par[j*2+1], 12, 4)+"\n";
    }

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
      System.out.println(Evaluation.evaluateModel(new MIDD(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
