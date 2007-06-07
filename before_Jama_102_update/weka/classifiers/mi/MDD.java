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
 * MDD.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
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
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Modified Diverse Density algorithm, with collective assumption.<br/>
 * <br/>
 * More information about DD:<br/>
 * <br/>
 * Oded Maron (1998). Learning from ambiguity.<br/>
 * <br/>
 * O. Maron, T. Lozano-Perez (1998). A Framework for Multiple Instance Learning. Neural Information Processing Systems. 10.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;phdthesis{Maron1998,
 *    author = {Oded Maron},
 *    school = {Massachusetts Institute of Technology},
 *    title = {Learning from ambiguity},
 *    year = {1998}
 * }
 * 
 * &#64;article{Maron1998,
 *    author = {O. Maron and T. Lozano-Perez},
 *    journal = {Neural Information Processing Systems},
 *    title = {A Framework for Multiple Instance Learning},
 *    volume = {10},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -N &lt;num&gt;
 *  Whether to 0=normalize/1=standardize/2=neither.
 *  (default 1=standardize)</pre>
 * 
 <!-- options-end -->
 *    
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $ 
 */
public class MDD 
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler,
             TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = -7273119490545290581L;

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
  protected Filter m_Filter =null;

  /** Whether to normalize/standardize/neither, default:standardize */
  protected int m_filterType = FILTER_STANDARDIZE;

  /** Normalize training data */
  public static final int FILTER_NORMALIZE = 0;
  /** Standardize training data */
  public static final int FILTER_STANDARDIZE = 1;
  /** No normalization/standardization */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
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
        "Modified Diverse Density algorithm, with collective assumption.\n\n"
      + "More information about DD:\n\n"
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
    
    result = new TechnicalInformation(Type.PHDTHESIS);
    result.setValue(Field.AUTHOR, "Oded Maron");
    result.setValue(Field.YEAR, "1998");
    result.setValue(Field.TITLE, "Learning from ambiguity");
    result.setValue(Field.SCHOOL, "Massachusetts Institute of Technology");
    
    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "O. Maron and T. Lozano-Perez");
    additional.setValue(Field.YEAR, "1998");
    additional.setValue(Field.TITLE, "A Framework for Multiple Instance Learning");
    additional.setValue(Field.JOURNAL, "Neural Information Processing Systems");
    additional.setValue(Field.VOLUME, "10");
    
    return result;
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
    extends Optimization{
    
    /** 
     * Evaluate objective function
     * @param x the current values of variables
     * @return the value of the objective function 
     */
    protected double objectiveFunction(double[] x){
      double nll = 0; // -LogLikelihood
      for(int i=0; i<m_Classes.length; i++){ // ith bag
        int nI = m_Data[i][0].length; // numInstances in ith bag
        double bag = 0;  // NLL of each bag

        for(int j=0; j<nI; j++){
          double ins=0.0; 
          for(int k=0; k<m_Data[i].length; k++) {
            ins += (m_Data[i][k][j]-x[k*2])*(m_Data[i][k][j]-x[k*2])/
              (x[k*2+1]*x[k*2+1]);
          }
          ins = Math.exp(-ins); 

          if(m_Classes[i] == 1)
            bag += ins/(double)nI;
          else
            bag += (1.0-ins)/(double)nI;   
        }		
        if(bag<=m_Zero) bag=m_Zero; 
        nll -= Math.log(bag);
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
            exp += (m_Data[i][k][j]-x[k*2])*(m_Data[i][k][j]-x[k*2])/
              (x[k*2+1]*x[k*2+1]);			
          exp = Math.exp(-exp);
          if(m_Classes[i]==1)
            denom += exp;
          else
            denom += (1.0-exp);		   

          // Instance-wise update
          for(int p=0; p<m_Data[i].length; p++){  // pth variable
            numrt[2*p] += exp*2.0*(x[2*p]-m_Data[i][p][j])/
              (x[2*p+1]*x[2*p+1]);
            numrt[2*p+1] += 
              exp*(x[2*p]-m_Data[i][p][j])*(x[2*p]-m_Data[i][p][j])/
              (x[2*p+1]*x[2*p+1]*x[2*p+1]);
          }			
        }

        if(denom <= m_Zero){
          denom = m_Zero;
        }

        // Bag-wise update 
        for(int q=0; q<m_Data[i].length; q++){
          if(m_Classes[i]==1){
            grad[2*q] += numrt[2*q]/denom;
            grad[2*q+1] -= numrt[2*q+1]/denom;
          }else{
            grad[2*q] -= numrt[2*q]/denom;
            grad[2*q+1] += numrt[2*q+1]/denom;
          }
        }
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
    
    m_ClassIndex = train.classIndex();
    m_NumClasses = train.numClasses();

    int nR = train.attribute(1).relation().numAttributes();
    int nC = train.numInstances();
    int [] bagSize=new int [nC];
    Instances datasets= new Instances(train.attribute(1).relation(),0);

    m_Data  = new double [nC][nR][];              // Data values
    m_Classes  = new int [nC];                    // Class values
    m_Attributes = datasets.stringFreeStructure();		
    double sY1=0, sY0=0;                          // Number of classes

    if (m_Debug) {
      System.out.println("Extracting data...");
    }
    FastVector maxSzIdx=new FastVector();
    int maxSz=0;

    for(int h=0; h<nC; h++){
      Instance current = train.instance(h);
      m_Classes[h] = (int)current.classValue();  // Class value starts from 0
      Instances currInsts = current.relationalValue(1);
      int nI = currInsts.numInstances();
      bagSize[h]=nI;

      for (int i=0; i<nI;i++){
        Instance inst=currInsts.instance(i);
        datasets.add(inst);
      }

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

      // Class count	
      if (m_Classes[h] == 1)
        sY1++;		
      else
        sY0++;
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

    // Largest positive exemplar
    for(int s=0; s<maxSzIdx.size(); s++){
      int exIdx = ((Integer)maxSzIdx.elementAt(s)).intValue();
      for(int p=0; p<m_Data[exIdx][0].length; p++){
        for (int q=0; q < nR;q++){
          x[2*q] = m_Data[exIdx][q][p];  // pick one instance
          x[2*q+1] = 1.0;
        }		

        opt = new OptEng();	
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
    distribution[1]=0.0;  // Prob. for class 1

    for(int i=0; i<nI; i++){
      double exp = 0.0;
      for(int r=0; r<nA; r++)
        exp += (m_Par[r*2]-dat[i][r])*(m_Par[r*2]-dat[i][r])/
          ((m_Par[r*2+1])*(m_Par[r*2+1]));
      exp = Math.exp(-exp);

      // Prob. updated for one instance
      distribution[1] += exp/(double)nI;
      distribution[0] += (1.0-exp)/(double)nI;
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

    result += "\nCoefficients...\n"
      + "Variable      Coeff.\n";
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
    runClassifier(new MDD(), argv);
  }
}
