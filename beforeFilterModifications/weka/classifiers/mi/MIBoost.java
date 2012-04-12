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
 * MIBoost.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.SingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * MI AdaBoost method, considers the geometric mean of posterior of instances inside a bag (arithmatic mean of log-posterior) and the expectation for a bag is taken inside the loss function.<br/>
 * <br/>
 * For more information about Adaboost, see:<br/>
 * <br/>
 * Yoav Freund, Robert E. Schapire: Experiments with a new boosting algorithm. In: Thirteenth International Conference on Machine Learning, San Francisco, 148-156, 1996.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;incproceedings{Freund1996,
 *    address = {San Francisco},
 *    author = {Yoav Freund and Robert E. Schapire},
 *    booktitle = {Thirteenth International Conference on Machine Learning},
 *    pages = {148-156},
 *    publisher = {Morgan Kaufmann},
 *    title = {Experiments with a new boosting algorithm},
 *    year = {1996}
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
 * <pre> -B &lt;num&gt;
 *  The number of bins in discretization
 *  (default 0, no discretization)</pre>
 * 
 * <pre> -R &lt;num&gt;
 *  Maximum number of boost iterations.
 *  (default 10)</pre>
 * 
 * <pre> -W &lt;class name&gt;
 *  Full name of classifier to boost.
 *  eg: weka.classifiers.bayes.NaiveBayes</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $ 
 */
public class MIBoost 
  extends SingleClassifierEnhancer
  implements OptionHandler, MultiInstanceCapabilitiesHandler,
             TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -3808427225599279539L;
  
  /** the models for the iterations */
  protected Classifier[] m_Models;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** attributes name for the new dataset used to build the model  */
  protected Instances m_Attributes;

  /** Number of iterations */   
  private int m_NumIterations = 100;

  /** Voting weights of models */ 
  protected double[] m_Beta;

  /** the maximum number of boost iterations */
  protected int m_MaxIterations = 10;

  /** the number of discretization bins */
  protected int m_DiscretizeBin = 0;

  /** filter used for discretization */
  protected Discretize m_Filter = null;

  /** filter used to convert the MI dataset into single-instance dataset */
  protected MultiInstanceToPropositional m_ConvertToSI = new MultiInstanceToPropositional();

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "MI AdaBoost method, considers the geometric mean of posterior "
      + "of instances inside a bag (arithmatic mean of log-posterior) and "
      + "the expectation for a bag is taken inside the loss function.\n\n"
      + "For more information about Adaboost, see:\n\n"
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
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Yoav Freund and Robert E. Schapire");
    result.setValue(Field.TITLE, "Experiments with a new boosting algorithm");
    result.setValue(Field.BOOKTITLE, "Thirteenth International Conference on Machine Learning");
    result.setValue(Field.YEAR, "1996");
    result.setValue(Field.PAGES, "148-156");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann");
    result.setValue(Field.ADDRESS, "San Francisco");
    
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
          "\tThe number of bins in discretization\n"
          + "\t(default 0, no discretization)",
          "B", 1, "-B <num>"));	

    result.addElement(new Option(
          "\tMaximum number of boost iterations.\n"
          + "\t(default 10)",
          "R", 1, "-R <num>"));	

    result.addElement(new Option(
          "\tFull name of classifier to boost.\n"
          + "\teg: weka.classifiers.bayes.NaiveBayes",
          "W", 1, "-W <class name>"));

    Enumeration enu = ((OptionHandler)m_Classifier).listOptions();
    while (enu.hasMoreElements()) {
      result.addElement(enu.nextElement());
    }

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -B &lt;num&gt;
   *  The number of bins in discretization
   *  (default 0, no discretization)</pre>
   * 
   * <pre> -R &lt;num&gt;
   *  Maximum number of boost iterations.
   *  (default 10)</pre>
   * 
   * <pre> -W &lt;class name&gt;
   *  Full name of classifier to boost.
   *  eg: weka.classifiers.bayes.NaiveBayes</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));

    String bin = Utils.getOption('B', options);
    if (bin.length() != 0) {
      setDiscretizeBin(Integer.parseInt(bin));
    } else {
      setDiscretizeBin(0);
    }

    String boostIterations = Utils.getOption('R', options);
    if (boostIterations.length() != 0) {
      setMaxIterations(Integer.parseInt(boostIterations));
    } else {
      setMaxIterations(10);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result  = new Vector();
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (getDebug())
      result.add("-D");
    
    result.add("-R");
    result.add("" + getMaxIterations());

    result.add("-B");
    result.add("" + getDiscretizeBin());

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxIterationsTipText() {
    return "The maximum number of boost iterations.";
  }

  /**
   * Set the maximum number of boost iterations
   *
   * @param maxIterations the maximum number of boost iterations
   */
  public void setMaxIterations(int maxIterations) {	
    m_MaxIterations = maxIterations;
  }

  /**
   * Get the maximum number of boost iterations
   *
   * @return the maximum number of boost iterations
   */
  public int getMaxIterations() {

    return m_MaxIterations;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String discretizeBinTipText() {
    return "The number of bins in discretization.";
  }

  /**
   * Set the number of bins in discretization
   *
   * @param bin the number of bins in discretization
   */
  public void setDiscretizeBin(int bin) {	
    m_DiscretizeBin = bin;
  }

  /**
   * Get the number of bins in discretization
   *
   * @return the number of bins in discretization
   */
  public int getDiscretizeBin() {	
    return m_DiscretizeBin;
  }

  private class OptEng 
    extends Optimization {
    
    private double[] weights, errs;

    public void setWeights(double[] w){
      weights = w;
    }

    public void setErrs(double[] e){
      errs = e;
    }

    /** 
     * Evaluate objective function
     * @param x the current values of variables
     * @return the value of the objective function 
     * @throws Exception if result is NaN
     */
    protected double objectiveFunction(double[] x) throws Exception{
      double obj=0;
      for(int i=0; i<weights.length; i++){
        obj += weights[i]*Math.exp(x[0]*(2.0*errs[i]-1.0));
        if(Double.isNaN(obj))
          throw new Exception("Objective function value is NaN!");

      }
      return obj;
    }

    /** 
     * Evaluate Jacobian vector
     * @param x the current values of variables
     * @return the gradient vector 
     * @throws Exception if gradient is NaN
     */
    protected double[] evaluateGradient(double[] x)  throws Exception{
      double[] grad = new double[1];
      for(int i=0; i<weights.length; i++){
        grad[0] += weights[i]*(2.0*errs[i]-1.0)*Math.exp(x[0]*(2.0*errs[i]-1.0));
        if(Double.isNaN(grad[0]))
          throw new Exception("Gradient is NaN!");

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
    result.disableAllClasses();
    result.disableAllClassDependencies();
    if (super.getCapabilities().handles(Capability.BINARY_CLASS))
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
    
    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param exps the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances exps) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(exps);

    // remove instances with missing class
    Instances train = new Instances(exps);
    train.deleteWithMissingClass();

    m_NumClasses = train.numClasses();
    m_NumIterations = m_MaxIterations;

    if (m_Classifier == null)
      throw new Exception("A base classifier has not been specified!");
    if(!(m_Classifier instanceof WeightedInstancesHandler))
      throw new Exception("Base classifier cannot handle weighted instances!");

    m_Models = Classifier.makeCopies(m_Classifier, getMaxIterations());
    if(m_Debug)
      System.err.println("Base classifier: "+m_Classifier.getClass().getName());

    m_Beta = new double[m_NumIterations];

    /* modified by Lin Dong. (use MIToSingleInstance filter to convert the MI datasets) */

    //Initialize the bags' weights
    double N = (double)train.numInstances(), sumNi=0;
    for(int i=0; i<N; i++)
      sumNi += train.instance(i).relationalValue(1).numInstances();	
    for(int i=0; i<N; i++){
      train.instance(i).setWeight(sumNi/N);
    }

    //convert the training dataset into single-instance dataset
    m_ConvertToSI.setInputFormat(train);
    Instances data = Filter.useFilter( train, m_ConvertToSI);
    data.deleteAttributeAt(0); //remove the bagIndex attribute;


    // Assume the order of the instances are preserved in the Discretize filter
    if(m_DiscretizeBin > 0){
      m_Filter = new Discretize();
      m_Filter.setInputFormat(new Instances(data, 0));
      m_Filter.setBins(m_DiscretizeBin);
      data = Filter.useFilter(data, m_Filter);
    }

    // Main algorithm
    int dataIdx;
iterations:
    for(int m=0; m < m_MaxIterations; m++){
      if(m_Debug)
        System.err.println("\nIteration "+m); 


      // Build a model
      m_Models[m].buildClassifier(data);

      // Prediction of each bag
      double[] err=new double[(int)N], weights=new double[(int)N];
      boolean perfect = true, tooWrong=true;
      dataIdx = 0;
      for(int n=0; n<N; n++){
        Instance exn = train.instance(n);
        // Prediction of each instance and the predicted class distribution
        // of the bag		
        double nn = (double)exn.relationalValue(1).numInstances();
        for(int p=0; p<nn; p++){
          Instance testIns = data.instance(dataIdx++);			
          if((int)m_Models[m].classifyInstance(testIns) 
              != (int)exn.classValue()) // Weighted instance-wise 0-1 errors
            err[n] ++;		       		       
        }
        weights[n] = exn.weight();
        err[n] /= nn;
        if(err[n] > 0.5)
          perfect = false;
        if(err[n] < 0.5)
          tooWrong = false;
      }

      if(perfect || tooWrong){ // No or 100% classification error, cannot find beta
        if (m == 0)
          m_Beta[m] = 1.0;
        else		    
          m_Beta[m] = 0;		
        m_NumIterations = m+1;
        if(m_Debug)  System.err.println("No errors");
        break iterations;
      }

      double[] x = new double[1];
      x[0] = 0;
      double[][] b = new double[2][x.length];
      b[0][0] = Double.NaN;
      b[1][0] = Double.NaN;

      OptEng opt = new OptEng();	
      opt.setWeights(weights);
      opt.setErrs(err);
      //opt.setDebug(m_Debug);
      if (m_Debug)
        System.out.println("Start searching for c... ");
      x = opt.findArgmin(x, b);
      while(x==null){
        x = opt.getVarbValues();
        if (m_Debug)
          System.out.println("200 iterations finished, not enough!");
        x = opt.findArgmin(x, b);
      }	
      if (m_Debug)
        System.out.println("Finished.");    
      m_Beta[m] = x[0];

      if(m_Debug)
        System.err.println("c = "+m_Beta[m]);

      // Stop if error too small or error too big and ignore this model
      if (Double.isInfinite(m_Beta[m]) 
          || Utils.smOrEq(m_Beta[m], 0)
         ) {
        if (m == 0)
          m_Beta[m] = 1.0;
        else		    
          m_Beta[m] = 0;
        m_NumIterations = m+1;
        if(m_Debug)
          System.err.println("Errors out of range!");
        break iterations;
         }

      // Update weights of data and class label of wfData
      dataIdx=0;
      double totWeights=0;
      for(int r=0; r<N; r++){		
        Instance exr = train.instance(r);
        exr.setWeight(weights[r]*Math.exp(m_Beta[m]*(2.0*err[r]-1.0)));
        totWeights += exr.weight();
      }

      if(m_Debug)
        System.err.println("Total weights = "+totWeights);

      for(int r=0; r<N; r++){		
        Instance exr = train.instance(r);
        double num = (double)exr.relationalValue(1).numInstances();
        exr.setWeight(sumNi*exr.weight()/totWeights);
        //if(m_Debug)
        //    System.err.print("\nExemplar "+r+"="+exr.weight()+": \t");
        for(int s=0; s<num; s++){
          Instance inss = data.instance(dataIdx);	
          inss.setWeight(exr.weight()/num);		   
          //    if(m_Debug)
          //  System.err.print("instance "+s+"="+inss.weight()+
          //			 "|ew*iw*sumNi="+data.instance(dataIdx).weight()+"\t");
          if(Double.isNaN(inss.weight()))
            throw new Exception("instance "+s+" in bag "+r+" has weight NaN!"); 
          dataIdx++;
        }
        //if(m_Debug)
        //    System.err.println();
      }	       
    }
  }		

  /**
   * Computes the distribution for a given exemplar
   *
   * @param exmp the exemplar for which distribution is computed
   * @return the classification
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance exmp) 
    throws Exception { 

    double[] rt = new double[m_NumClasses];

    Instances insts = new Instances(exmp.dataset(), 0);
    insts.add(exmp);

    // convert the training dataset into single-instance dataset
    insts = Filter.useFilter( insts, m_ConvertToSI);
    insts.deleteAttributeAt(0); //remove the bagIndex attribute	

    double n = insts.numInstances();

    if(m_DiscretizeBin > 0)
      insts = Filter.useFilter(insts, m_Filter);

    for(int y=0; y<n; y++){
      Instance ins = insts.instance(y);	
      for(int x=0; x<m_NumIterations; x++){ 
        rt[(int)m_Models[x].classifyInstance(ins)] += m_Beta[x]/n;
      }
    }

    for(int i=0; i<rt.length; i++)
      rt[i] = Math.exp(rt[i]);

    Utils.normalize(rt);
    return rt;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {

    if (m_Models == null) {
      return "No model built yet!";
    }
    StringBuffer text = new StringBuffer();
    text.append("MIBoost: number of bins in discretization = "+m_DiscretizeBin+"\n");
    if (m_NumIterations == 0) {
      text.append("No model built yet.\n");
    } else if (m_NumIterations == 1) {
      text.append("No boosting possible, one classifier used: Weight = " 
          + Utils.roundDouble(m_Beta[0], 2)+"\n");
      text.append("Base classifiers:\n"+m_Models[0].toString());
    } else {
      text.append("Base classifiers and their weights: \n");
      for (int i = 0; i < m_NumIterations ; i++) {
        text.append("\n\n"+i+": Weight = " + Utils.roundDouble(m_Beta[i], 2)
            +"\nBase classifier:\n"+m_Models[i].toString() );
      }
    }

    text.append("\n\nNumber of performed Iterations: " 
        + m_NumIterations + "\n");

    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new MIBoost(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
