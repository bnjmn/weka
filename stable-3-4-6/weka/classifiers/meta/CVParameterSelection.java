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
 *    CVParameterSelection.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for performing parameter selection by cross-validation for any
 * classifier. For more information, see<p>
 *
 * R. Kohavi (1995). <i>Wrappers for Performance
 * Enhancement and Oblivious Decision Graphs</i>. PhD
 * Thesis. Department of Computer Science, Stanford University. <p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of classifier to perform cross-validation
 * selection on.<p>
 *
 * -X num <br>
 * Number of folds used for cross validation (default 10). <p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -P "N 1 5 10" <br>
 * Sets an optimisation parameter for the classifier with name -N,
 * lower bound 1, upper bound 5, and 10 optimisation steps.
 * The upper bound may be the character 'A' or 'I' to substitute 
 * the number of attributes or instances in the training data,
 * respectively.
 * This parameter may be supplied more than once to optimise over
 * several classifier options simultaneously. <p>
 *
 * Options after -- are passed to the designated sub-classifier. <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.27.2.3 $ 
*/
public class CVParameterSelection extends RandomizableSingleClassifierEnhancer
  implements Drawable, Summarizable {

  /*
   * A data structure to hold values associated with a single
   * cross-validation search parameter
   */
  protected class CVParameter implements Serializable {

    /**  Char used to identify the option of interest */
    private char m_ParamChar;    

    /**  Lower bound for the CV search */
    private double m_Lower;      

    /**  Upper bound for the CV search */
    private double m_Upper;      

    /**  Number of steps during the search */
    private double m_Steps;      

    /**  The parameter value with the best performance */
    private double m_ParamValue; 

    /**  True if the parameter should be added at the end of the argument list */
    private boolean m_AddAtEnd;  

    /**  True if the parameter should be rounded to an integer */
    private boolean m_RoundParam;

    /**
     * Constructs a CVParameter.
     */
    public CVParameter(String param) throws Exception {
     
      // Tokenize the string into it's parts
      StreamTokenizer st = new StreamTokenizer(new StringReader(param));
      if (st.nextToken() != StreamTokenizer.TT_WORD) {
	throw new Exception("CVParameter " + param 
			    + ": Character parameter identifier expected");
      }
      m_ParamChar = st.sval.charAt(0);
      if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
	throw new Exception("CVParameter " + param 
			    + ": Numeric lower bound expected");
      }
      m_Lower = st.nval;
      if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
	m_Upper = st.nval;
	if (m_Upper < m_Lower) {
	  throw new Exception("CVParameter " + param
			      + ": Upper bound is less than lower bound");
	}
      } else if (st.ttype == StreamTokenizer.TT_WORD) {
	if (st.sval.toUpperCase().charAt(0) == 'A') {
	  m_Upper = m_Lower - 1;
	} else if (st.sval.toUpperCase().charAt(0) == 'I') {
	  m_Upper = m_Lower - 2;
	} else {
	  throw new Exception("CVParameter " + param 
	      + ": Upper bound must be numeric, or 'A' or 'N'");
	}
      } else {
	throw new Exception("CVParameter " + param 
	      + ": Upper bound must be numeric, or 'A' or 'N'");
      }
      if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
	throw new Exception("CVParameter " + param 
			    + ": Numeric number of steps expected");
      }
      m_Steps = st.nval;
      if (st.nextToken() == StreamTokenizer.TT_WORD) {
	if (st.sval.toUpperCase().charAt(0) == 'R') {
	  m_RoundParam = true;
	}
      }
    }

    /**
     * Returns a CVParameter as a string.
     */
    public String toString() {

      String result = m_ParamChar + " " + m_Lower + " ";
      switch ((int)(m_Lower - m_Upper + 0.5)) {
      case 1:
	result += "A";
	break;
      case 2:
	result += "I";
	break;
      default:
	result += m_Upper;
	break;
      }
      result += " " + m_Steps;
      if (m_RoundParam) {
	result += " R";
      }
      return result;
    }
  }

  /**
   * The base classifier options (not including those being set
   * by cross-validation)
   */
  protected String [] m_ClassifierOptions;

  /** The set of all classifier options as determined by cross-validation */
  protected String [] m_BestClassifierOptions;

  /** The set of all options at initialization time. So that getOptions
      can return this. */
  protected String [] m_InitOptions;

  /** The cross-validated performance of the best options */
  protected double m_BestPerformance;

  /** The set of parameters to cross-validate over */
  protected FastVector m_CVParams = new FastVector();

  /** The number of attributes in the data */
  protected int m_NumAttributes;

  /** The number of instances in a training fold */
  protected int m_TrainFoldSize;
  
  /** The number of folds used in cross-validation */
  protected int m_NumFolds = 10;

  /**
   * Create the options array to pass to the classifier. The parameter
   * values and positions are taken from m_ClassifierOptions and
   * m_CVParams.
   *
   * @return the options array
   */
  protected String [] createOptions() {
    
    String [] options = new String [m_ClassifierOptions.length 
				   + 2 * m_CVParams.size()];
    int start = 0, end = options.length;

    // Add the cross-validation parameters and their values
    for (int i = 0; i < m_CVParams.size(); i++) {
      CVParameter cvParam = (CVParameter)m_CVParams.elementAt(i);
      double paramValue = cvParam.m_ParamValue;
      if (cvParam.m_RoundParam) {
	paramValue = (double)((int) (paramValue + 0.5));
      }
      if (cvParam.m_AddAtEnd) {
	options[--end] = "" + 
	Utils.doubleToString(paramValue,4);
	options[--end] = "-" + cvParam.m_ParamChar;
      } else {
	options[start++] = "-" + cvParam.m_ParamChar;
	options[start++] = "" 
	+ Utils.doubleToString(paramValue,4);
      }
    }
    // Add the static parameters
    System.arraycopy(m_ClassifierOptions, 0,
		     options, start,
		     m_ClassifierOptions.length);

    return options;
  }

  /**
   * Finds the best parameter combination. (recursive for each parameter
   * being optimised).
   *
   * @param depth the index of the parameter to be optimised at this level
   * @exception Exception if an error occurs
   */
  protected void findParamsByCrossValidation(int depth, Instances trainData,
					     Random random)
    throws Exception {

    if (depth < m_CVParams.size()) {
      CVParameter cvParam = (CVParameter)m_CVParams.elementAt(depth);

      double upper;
      switch ((int)(cvParam.m_Lower - cvParam.m_Upper + 0.5)) {
      case 1:
	upper = m_NumAttributes;
	break;
      case 2:
	upper = m_TrainFoldSize;
	break;
      default:
	upper = cvParam.m_Upper;
	break;
      }
      double increment = (upper - cvParam.m_Lower) / (cvParam.m_Steps - 1);
      for(cvParam.m_ParamValue = cvParam.m_Lower; 
	  cvParam.m_ParamValue <= upper; 
	  cvParam.m_ParamValue += increment) {
	findParamsByCrossValidation(depth + 1, trainData, random);
      }
    } else {
      
      Evaluation evaluation = new Evaluation(trainData);

      // Set the classifier options
      String [] options = createOptions();
      if (m_Debug) {
	System.err.print("Setting options for " 
			 + m_Classifier.getClass().getName() + ":");
	for (int i = 0; i < options.length; i++) {
	  System.err.print(" " + options[i]);
	}
	System.err.println("");
      }
      ((OptionHandler)m_Classifier).setOptions(options);
      for (int j = 0; j < m_NumFolds; j++) {

        // We want to randomize the data the same way for every 
        // learning scheme.
	Instances train = trainData.trainCV(m_NumFolds, j, new Random(1));
	Instances test = trainData.testCV(m_NumFolds, j);
	m_Classifier.buildClassifier(train);
	evaluation.setPriors(train);
	evaluation.evaluateModel(m_Classifier, test);
      }
      double error = evaluation.errorRate();
      if (m_Debug) {
	System.err.println("Cross-validated error rate: " 
			   + Utils.doubleToString(error, 6, 4));
      }
      if ((m_BestPerformance == -99) || (error < m_BestPerformance)) {
	
	m_BestPerformance = error;
	m_BestClassifierOptions = createOptions();
      }
    }
  }

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return  "Class for performing parameter selection by cross-validation "+
	    "for any classifier. For more information, see:\n"+
	    "R. Kohavi (1995). Wrappers for Performance "+
	    "Enhancement and Oblivious Decision Graphs. PhD "+
	    "Thesis. Department of Computer Science, Stanford University.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
	      "\tNumber of folds used for cross validation (default 10).",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tClassifier parameter options.\n"
	      + "\teg: \"N 1 5 10\" Sets an optimisation parameter for the\n"
	      + "\tclassifier with name -N, with lower bound 1, upper bound\n"
	      + "\t5, and 10 optimisation steps. The upper bound may be the\n"
	      + "\tcharacter 'A' or 'I' to substitute the number of\n"
	      + "\tattributes or instances in the training data,\n"
	      + "\trespectively. This parameter may be supplied more than\n"
	      + "\tonce to optimise over several classifier options\n"
	      + "\tsimultaneously.",
	      "P", 1, "-P <classifier parameter>"));


    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of classifier to perform cross-validation
   * selection on.<p>
   *
   * -X num <br>
   * Number of folds used for cross validation (default 10). <p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -P "N 1 5 10" <br>
   * Sets an optimisation parameter for the classifier with name -N,
   * lower bound 1, upper bound 5, and 10 optimisation steps.
   * The upper bound may be the character 'A' or 'I' to substitute 
   * the number of attributes or instances in the training data,
   * respectively.
   * This parameter may be supplied more than once to optimise over
   * several classifier options simultaneously. <p>
   *
   * Options after -- are passed to the designated sub-classifier. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String foldsString = Utils.getOption('X', options);
    if (foldsString.length() != 0) {
      setNumFolds(Integer.parseInt(foldsString));
    } else {
      setNumFolds(10);
    }

    String cvParam;
    m_CVParams = new FastVector();
    do {
      cvParam = Utils.getOption('P', options);
      if (cvParam.length() != 0) {
	addCVParameter(cvParam);
      }
    } while (cvParam.length() != 0);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String[] superOptions;

    if (m_InitOptions != null) {
      try {
	m_Classifier.setOptions((String[])m_InitOptions.clone());
	superOptions = super.getOptions();
	m_Classifier.setOptions((String[])m_BestClassifierOptions.clone());
      } catch (Exception e) {
	throw new RuntimeException("CVParameterSelection: could not set options " +
				   "in getOptions().");
      } 
    } else {
      superOptions = super.getOptions();
    }
    String [] options = new String [superOptions.length + m_CVParams.size() * 2 + 2];

    int current = 0;
    for (int i = 0; i < m_CVParams.size(); i++) {
      options[current++] = "-P"; options[current++] = "" + getCVParameter(i);
    }
    options[current++] = "-X"; options[current++] = "" + getNumFolds();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    return options;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    Instances trainData = new Instances(instances);
    trainData.deleteWithMissingClass();
    if (trainData.numInstances() == 0) {
      throw new IllegalArgumentException("No training instances without " +
					 "missing class.");
    }
    if (trainData.numInstances() < m_NumFolds) {
      throw new IllegalArgumentException("Number of training instances " +
					 "smaller than number of folds.");
    }
    if (!(m_Classifier instanceof OptionHandler)) {
      throw new IllegalArgumentException("Base classifier should be OptionHandler.");
    }
    m_InitOptions = ((OptionHandler)m_Classifier).getOptions();
    m_BestPerformance = -99;
    m_NumAttributes = trainData.numAttributes();
    Random random = new Random(m_Seed);
    trainData.randomize(random);
    m_TrainFoldSize = trainData.trainCV(m_NumFolds, 0).numInstances();

    // Check whether there are any parameters to optimize
    if (m_CVParams.size() == 0) {
       m_Classifier.buildClassifier(trainData);
       m_BestClassifierOptions = m_InitOptions;
       return;
    }

    if (trainData.classAttribute().isNominal()) {
      trainData.stratify(m_NumFolds);
    }
    m_BestClassifierOptions = null;
    
    // Set up m_ClassifierOptions -- take getOptions() and remove
    // those being optimised.
    m_ClassifierOptions = ((OptionHandler)m_Classifier).getOptions();
    for (int i = 0; i < m_CVParams.size(); i++) {
      Utils.getOption(((CVParameter)m_CVParams.elementAt(i)).m_ParamChar,
		      m_ClassifierOptions);
    }
    findParamsByCrossValidation(0, trainData, random);

    String [] options = (String [])m_BestClassifierOptions.clone();
    ((OptionHandler)m_Classifier).setOptions(options);
    m_Classifier.buildClassifier(trainData);
  }


  /**
   * Predicts the class distribution for the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class value
   * @exception Exception if an error occurred during the prediction
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    return m_Classifier.distributionForInstance(instance);
  }

  /**
   * Adds a scheme parameter to the list of parameters to be set
   * by cross-validation
   *
   * @param cvParam the string representation of a scheme parameter. The
   * format is: <br>
   * param_char lower_bound upper_bound number_of_steps <br>
   * eg to search a parameter -P from 1 to 10 by increments of 1: <br>
   * P 1 10 11 <br>
   * @exception Exception if the parameter specifier is of the wrong format
   */
  public void addCVParameter(String cvParam) throws Exception {

    CVParameter newCV = new CVParameter(cvParam);
    
    m_CVParams.addElement(newCV);
  }

  /**
   * Gets the scheme paramter with the given index.
   */
  public String getCVParameter(int index) {

    if (m_CVParams.size() <= index) {
      return "";
    }
    return ((CVParameter)m_CVParams.elementAt(index)).toString();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String CVParametersTipText() {
    return "Sets the scheme parameters which are to be set "+
	   "by cross-validation.\n"+
	   "The format for each string should be:\n"+
	   "param_char lower_bound upper_bound number_of_steps\n"+
	   "eg to search a parameter -P from 1 to 10 by increments of 1:\n"+
	   "    \"P 1 10 11\" ";
  }

  /**
   * Get method for CVParameters.
   */
  public Object[] getCVParameters() {
      
      Object[] CVParams = m_CVParams.toArray();
      
      String params[] = new String[CVParams.length];
      
      for(int i=0; i<CVParams.length; i++) 
          params[i] = CVParams[i].toString();
      
      return params;
      
  }
  
  /**
   * Set method for CVParameters.
   */
  public void setCVParameters(Object[] params) throws Exception {
      
      FastVector backup = m_CVParams;
      m_CVParams = new FastVector();
      
      for(int i=0; i<params.length; i++) {
          try{
          addCVParameter((String)params[i]);
          }
          catch(Exception ex) { m_CVParams = backup; throw ex; }
      }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "Get the number of folds used for cross-validation.";
  }

  /** 
   * Gets the number of folds for the cross-validation.
   *
   * @return the number of folds for the cross-validation
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Sets the number of folds for the cross-validation.
   *
   * @param numFolds the number of folds for the cross-validation
   * @exception Exception if parameter illegal
   */
  public void setNumFolds(int numFolds) throws Exception {
    
    if (numFolds < 0) {
      throw new IllegalArgumentException("Stacking: Number of cross-validation " +
					 "folds must be positive.");
    }
    m_NumFolds = numFolds;
  }
 
  /**
   *  Returns the type of graph this classifier
   *  represents.
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
   * @exception Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + 
			     m_Classifier.getClass().getName() + " " +
			     Utils.joinOptions(m_BestClassifierOptions)
			     + " cannot be graphed");
  }

  /**
   * Returns description of the cross-validated classifier.
   *
   * @return description of the cross-validated classifier as a string
   */
  public String toString() {

    if (m_InitOptions == null)
      return "CVParameterSelection: No model built yet.";

    String result = "Cross-validated Parameter selection.\n"
    + "Classifier: " + m_Classifier.getClass().getName() + "\n";
    try {
      for (int i = 0; i < m_CVParams.size(); i++) {
	CVParameter cvParam = (CVParameter)m_CVParams.elementAt(i);
	result += "Cross-validation Parameter: '-" 
	  + cvParam.m_ParamChar + "'"
	  + " ranged from " + cvParam.m_Lower 
	  + " to ";
	switch ((int)(cvParam.m_Lower - cvParam.m_Upper + 0.5)) {
	case 1:
	  result += m_NumAttributes;
	  break;
	case 2:
	  result += m_TrainFoldSize;
	  break;
	default:
	  result += cvParam.m_Upper;
	  break;
	}
	result += " with " + cvParam.m_Steps + " steps\n";
      }
    } catch (Exception ex) {
      result += ex.getMessage();
    }
    result += "Classifier Options: "
      + Utils.joinOptions(m_BestClassifierOptions)
      + "\n\n" + m_Classifier.toString();
    return result;
  }

  public String toSummaryString() {

    String result = "Selected values: "
      + Utils.joinOptions(m_BestClassifierOptions);
    return result + '\n';
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new CVParameterSelection(), 
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


  
