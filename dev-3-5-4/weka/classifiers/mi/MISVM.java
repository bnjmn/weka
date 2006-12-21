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
 *    Foundation, InumBag., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * MISVM.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
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
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.instance.SparseToNonSparse;

import java.util.Enumeration;
import java.util.Vector;


/**
 <!-- globalinfo-start -->
 * Implements Stuart Andrews' mi_SVM (Maximum pattern Margin Formulation of MIL). Applying weka.classifiers.functions.SMO to solve multiple instances problem.<br/>
 * The algorithm first assign the bag label to each instance in the bag as its initial class label.  After that applying SMO to compute SVM solution for all instances in positive bags And then reassign the class label of each instance in the positive bag according to the SVM result Keep on iteration until labels do not change anymore.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Stuart Andrews, Ioannis Tsochantaridis, Thomas Hofmann: Support Vector Machines for Multiple-Instance Learning. In: Advances in Neural Information Processing Systems 15, 561-568, 2003.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Andrews2003,
 *    author = {Stuart Andrews and Ioannis Tsochantaridis and Thomas Hofmann},
 *    booktitle = {Advances in Neural Information Processing Systems 15},
 *    pages = {561-568},
 *    publisher = {MIT Press},
 *    title = {Support Vector Machines for Multiple-Instance Learning},
 *    year = {2003}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -C &lt;double&gt;
 *  The complexity constant C. (default 1)</pre>
 * 
 * <pre> -N &lt;default 0&gt;
 *  Whether to 0=normalize/1=standardize/2=neither.
 *  (default: 0=normalize)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  The maximum number of iterations to perform.
 *  (default: 500)</pre>
 * 
 * <pre> -K &lt;classname and parameters&gt;
 *  The Kernel to use.
 *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
 * 
 * <pre> 
 * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
 * </pre>
 * 
 * <pre> -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)</pre>
 * 
 * <pre> -no-checks
 *  Turns off all checks - use with caution!
 *  (default: checks on)</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number).
 *  (default: 250007)</pre>
 * 
 * <pre> -E &lt;num&gt;
 *  The Exponent to use.
 *  (default: 1.0)</pre>
 * 
 * <pre> -L
 *  Use lower-order terms.
 *  (default: no)</pre>
 * 
 <!-- options-end -->
 *
 * @author Lin Dong (ld21@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $ 
 * @see weka.classifiers.functions.SMO
 */
public class MISVM 
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler,
             TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 7622231064035278145L;
  
  /** The filter used to transform the sparse datasets to nonsparse */
  protected Filter m_SparseFilter = new SparseToNonSparse();

  /** The SMO classifier used to compute SVM soluton w,b for the dataset */
  protected SVM m_SVM;

  /** the kernel to use */
  protected Kernel m_kernel = new PolyKernel();

  /** The complexity parameter. */
  protected double m_C = 1.0;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter =null;

  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_NORMALIZE;

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

  /** the maximum number of iterations to perform */
  protected int m_MaxIterations = 500;
  
  /** filter used to convert the MI dataset into single-instance dataset */
  protected MultiInstanceToPropositional m_ConvertToProp = new MultiInstanceToPropositional();

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
         "Implements Stuart Andrews' mi_SVM (Maximum pattern Margin "
       + "Formulation of MIL). Applying weka.classifiers.functions.SMO "
       + "to solve multiple instances problem.\n"
       + "The algorithm first assign the bag label to each instance in the "
       + "bag as its initial class label.  After that applying SMO to compute "
       + "SVM solution for all instances in positive bags And then reassign "
       + "the class label of each instance in the positive bag according to "
       + "the SVM result Keep on iteration until labels do not change "
       + "anymore.\n\n"
       + "For more information see:\n\n"
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
    result.setValue(Field.AUTHOR, "Stuart Andrews and Ioannis Tsochantaridis and Thomas Hofmann");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.TITLE, "Support Vector Machines for Multiple-Instance Learning");
    result.setValue(Field.BOOKTITLE, "Advances in Neural Information Processing Systems 15");
    result.setValue(Field.PUBLISHER, "MIT Press");
    result.setValue(Field.PAGES, "561-568");
    
    return result;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    
    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    result.addElement(new Option(
          "\tThe complexity constant C. (default 1)",
          "C", 1, "-C <double>"));
    
    result.addElement(new Option(
        "\tWhether to 0=normalize/1=standardize/2=neither.\n"
        + "\t(default: 0=normalize)",
        "N", 1, "-N <default 0>"));
  
    result.addElement(new Option(
        "\tThe maximum number of iterations to perform.\n"
        + "\t(default: 500)",
        "I", 1, "-I <num>"));
  
    result.addElement(new Option(
	"\tThe Kernel to use.\n"
	+ "\t(default: weka.classifiers.functions.supportVector.PolyKernel)",
	"K", 1, "-K <classname and parameters>"));

    result.addElement(new Option(
	"",
	"", 0, "\nOptions specific to kernel "
	+ getKernel().getClass().getName() + ":"));
    
    enm = ((OptionHandler) getKernel()).listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    return result.elements();
  }



  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -C &lt;double&gt;
   *  The complexity constant C. (default 1)</pre>
   * 
   * <pre> -N &lt;default 0&gt;
   *  Whether to 0=normalize/1=standardize/2=neither.
   *  (default: 0=normalize)</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  The maximum number of iterations to perform.
   *  (default: 500)</pre>
   * 
   * <pre> -K &lt;classname and parameters&gt;
   *  The Kernel to use.
   *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
   * 
   * <pre> 
   * Options specific to kernel weka.classifiers.functions.supportVector.PolyKernel:
   * </pre>
   * 
   * <pre> -D
   *  Enables debugging output (if available) to be printed.
   *  (default: off)</pre>
   * 
   * <pre> -no-checks
   *  Turns off all checks - use with caution!
   *  (default: checks on)</pre>
   * 
   * <pre> -C &lt;num&gt;
   *  The size of the cache (a prime number).
   *  (default: 250007)</pre>
   * 
   * <pre> -E &lt;num&gt;
   *  The Exponent to use.
   *  (default: 1.0)</pre>
   * 
   * <pre> -L
   *  Use lower-order terms.
   *  (default: no)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    String[]	tmpOptions;
    
    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length() != 0)
      setC(Double.parseDouble(tmpStr));
    else
      setC(1.0);
    
    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setFilterType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_FILTER));
    else
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));

    tmpStr = Utils.getOption('I', options);
    if (tmpStr.length() != 0)
      setMaxIterations(Integer.parseInt(tmpStr));
    else
      setMaxIterations(500);
    
    tmpStr     = Utils.getOption('K', options);
    tmpOptions = Utils.splitOptions(tmpStr);
    if (tmpOptions.length != 0) {
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setKernel(Kernel.forName(tmpStr, tmpOptions));
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
    
    result = new Vector();

    if (getDebug())
      result.add("-D");
    
    result.add("-C");
    result.add("" + getC());
    
    result.add("-N");
    result.add("" + m_filterType);

    result.add("-K");
    result.add("" + getKernel().getClass().getName() + " " + Utils.joinOptions(getKernel().getOptions()));

    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String kernelTipText() {
    return "The kernel to use.";
  }

  /**
   * Gets the kernel to use.
   *
   * @return 		the kernel
   */
  public Kernel getKernel() {
    return m_kernel;
  }
    
  /**
   * Sets the kernel to use.
   *
   * @param value	the kernel
   */
  public void setKernel(Kernel value) {
    m_kernel = value;
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
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String cTipText() {
    return "The value for C.";
  }

  /**
   * Get the value of C.
   *
   * @return Value of C.
   */
  public double getC() {

    return m_C;
  }

  /**
   * Set the value of C.
   *
   * @param v  Value to assign to C.
   */
  public void setC(double v) {
    m_C = v;
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String maxIterationsTipText() {
    return "The maximum number of iterations to perform.";
  }

  /**
   * Gets the maximum number of iterations.
   *
   * @return 		the maximum number of iterations.
   */
  public int getMaxIterations() {
    return m_MaxIterations;
  }

  /**
   * Sets the maximum number of iterations.
   *
   * @param value	the maximum number of iterations.
   */
  public void setMaxIterations(int value) {
    if (value < 1)
      System.out.println(
	  "At least 1 iteration is necessary (provided: " + value + ")!");
    else
      m_MaxIterations = value;
  }

  /**
   * adapted version of SMO
   */
  private class SVM extends SMO {
    
    /** for serialization */
    static final long serialVersionUID = -8325638229658828931L;
    
    /**
     * Constructor
     */
    protected SVM (){
      super();
    }

    /**
     * Computes SVM output for given instance.
     *
     * @param index the instance for which output is to be computed
     * @param inst the instance 
     * @return the output of the SVM for the given instance
     * @throws Exception in case of an error
     */
    protected double output(int index, Instance inst) throws Exception {
      double output = 0;
      output = m_classifiers[0][1].SVMOutput(index, inst);
      return output;
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
    SVM 		classifier;
    Capabilities 	result;

    classifier = null;
    result     = null;
    
    try {
      classifier = new SVM();
      classifier.setKernel(Kernel.makeCopy(getKernel()));
      result = classifier.getCapabilities();
      result.setOwner(this);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
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
    
    int numBags = train.numInstances(); //number of bags
    int []bagSize= new int [numBags];   
    int classes [] = new int [numBags];

    Vector instLabels = new Vector();  //store the class label assigned to each single instance
    Vector pre_instLabels=new Vector();

    for(int h=0; h<numBags; h++)  {//h_th bag 
      classes[h] = (int) train.instance(h).classValue();  
      bagSize[h]=train.instance(h).relationalValue(1).numInstances();
      for (int i=0; i<bagSize[h];i++)
        instLabels.addElement(new Double(classes[h]));	       	  
    }

    // convert the training dataset into single-instance dataset
    m_ConvertToProp.setWeightMethod(
        new SelectedTag(
          MultiInstanceToPropositional.WEIGHTMETHOD_1, 
          MultiInstanceToPropositional.TAGS_WEIGHTMETHOD)); 
    m_ConvertToProp.setInputFormat(train);
    train = Filter.useFilter( train, m_ConvertToProp);
    train.deleteAttributeAt(0); //remove the bagIndex attribute;

    if (m_filterType == FILTER_STANDARDIZE)  
      m_Filter = new Standardize();
    else if (m_filterType == FILTER_NORMALIZE)
      m_Filter = new Normalize();
    else 
      m_Filter = null;

    if (m_Filter!=null) {
      m_Filter.setInputFormat(train);
      train = Filter.useFilter(train, m_Filter);
    }	

    if (m_Debug) {
      System.out.println("\nIteration History..." );
    }

    if (getDebug())
      System.out.println("\nstart building model ...");

    int index;
    double sum, max_output; 
    Vector max_index = new Vector();
    Instance inst=null;

    int loopNum=0;
    do {
      loopNum++;
      index=-1;
      if (m_Debug)
        System.out.println("=====================loop: "+loopNum);

      //store the previous label information
      pre_instLabels=(Vector)instLabels.clone();   

      // set the proper SMO options in order to build a SVM model
      m_SVM = new SVM();
      m_SVM.setC(getC());
      m_SVM.setKernel(Kernel.makeCopy(getKernel()));
      // SVM model do not normalize / standardize the input dataset as the the dataset has already been processed  
      m_SVM.setFilterType(new SelectedTag(FILTER_NONE, TAGS_FILTER));  

      m_SVM.buildClassifier(train); 

      for(int h=0; h<numBags; h++)  {//h_th bag
        if (classes[h]==1) { //positive bag
          if (m_Debug)
            System.out.println("--------------- "+h+" ----------------");
          sum=0;

          //compute outputs f=(w,x)+b for all instance in positive bags
          for (int i=0; i<bagSize[h]; i++){
            index ++; 

            inst=train.instance(index); 
            double output =m_SVM.output(-1, inst); //System.out.println(output); 
            if (output<=0){
              if (inst.classValue()==1.0) {	
                train.instance(index).setClassValue(0.0);
                instLabels.set(index, new Double(0.0));

                if (m_Debug)
                  System.out.println( index+ "- changed to 0");
              }
            }
            else { 
              if (inst.classValue()==0.0) {
                train.instance(index).setClassValue(1.0);
                instLabels.set(index, new Double(1.0));

                if (m_Debug)
                  System.out.println(index+ "+ changed to 1");
              }
            }
            sum += train.instance(index).classValue();  
          }

          /* if class value of all instances in a positive bag 
             are changed to 0.0, find the instance with max SVMOutput value 
             and assign the class value 1.0 to it.
             */
          if (sum==0){ 
            //find the instance with max SVMOutput value  
            max_output=-Double.MAX_VALUE;
            max_index.clear();
            for (int j=index-bagSize[h]+1; j<index+1; j++){
              inst=train.instance(j);
              double output = m_SVM.output(-1, inst);
              if(max_output<output) {
                max_output=output;
                max_index.clear();
                max_index.add(new Integer(j));
              }
              else if(max_output==output) 
                max_index.add(new Integer(j));
            }

            //assign the class value 1.0 to the instances with max SVMOutput
            for (int vecIndex=0; vecIndex<max_index.size(); vecIndex ++) {
              Integer i =(Integer)max_index.get(vecIndex);
              train.instance(i.intValue()).setClassValue(1.0);
              instLabels.set(i.intValue(), new Double(1.0));

              if (m_Debug)
                System.out.println("##change to 1 ###outpput: "+max_output+" max_index: "+i+ " bag: "+h);
            }

          }
        }else   //negative bags
          index += bagSize[h];
      }
    }while(!instLabels.equals(pre_instLabels) && loopNum < m_MaxIterations);

    if (getDebug())
      System.out.println("finish building model.");
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

    double sum=0;
    double classValue;
    double[] distribution = new double[2];

    Instances testData = new Instances(exmp.dataset(), 0);
    testData.add(exmp);

    // convert the training dataset into single-instance dataset
    testData = Filter.useFilter(testData, m_ConvertToProp);	
    testData.deleteAttributeAt(0); //remove the bagIndex attribute	

    if (m_Filter != null)	
      testData = Filter.useFilter(testData, m_Filter); 

    for(int j = 0; j < testData.numInstances(); j++){
      Instance inst = testData.instance(j);
      double output = m_SVM.output(-1, inst); 
      if (output <= 0)
        classValue = 0.0;
      else
        classValue = 1.0;
      sum += classValue;
    }
    if (sum == 0)
      distribution[0] = 1.0;
    else 
      distribution[0] = 0.0;
    distribution [1] = 1.0 - distribution[0];

    return distribution;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new MISVM(), argv);
  }
}
