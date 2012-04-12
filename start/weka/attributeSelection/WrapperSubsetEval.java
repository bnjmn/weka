/*
 *    WrapperSubsetEval.java
 *    Copyright (C) 1999 Mark Hall
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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.filters.*;

/** 
 * Wrapper attribute subset evaluator
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */


public class WrapperSubsetEval 
  extends SubsetEvaluator
  implements OptionHandler {

  /** training instances */
  private Instances trainInstances;

  /** class index */
  private int classIndex;
  
  /** number of attributes in the training data */
  private int numAttribs;

  /** number of instances in the training data */
  private int numInstances;

  /** holds an evaluation object */
  private Evaluation w_Evaluation;

  /** holds the name of the classifer to estimate accuracy with */
  private String w_ClassifierString;

  /** holds the options to the classifier as a string */
  private String w_ClassifierOptionsString;

  /** holds the split-up options to the classifier */
  private String [] w_ClassifierOptions;
  
  /** number of folds to use for cross validation */
  private int folds;

  /** random number seed */
  private int seed;

  /** the threshold by which to do further cross validations when
   estimating the accuracy of a subset */
  private double threshold;


  /**
   * Constructor. Calls restOptions to set default options
   **/
  public WrapperSubsetEval()
  {
    resetOptions();
  }


  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   *
   **/
  public Enumeration listOptions() 
  {
    
    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
				    "\tclass name of base learner to use for"
				    +"\n\taccuracy estimation. Place any"
				    +"\n\tclassifier options LAST on the"
				    +"\n\tcommand line following a \"--\"."
				    +"\n\teg. -B weka.classifiers.NaiveBayes ... "
				    +"-- -K",
				    "B", 1,"-B <base learner>"));
    
    newVector.addElement(new Option("\tnumber of cross validation folds to "
				    +"use\n\tfor estimating accuracy."
				    +"\n\t<default=5>", "F", 1,"-F <num>"));

    //    newVector.addElement(new Option("\tseed", "S", 1,"-S <num>"));

    newVector.addElement(new Option("\tthreshold by which to execute "
				    +"another cross validation"
				    +"\n\t(standard deviation---"
				    +"expressed as a percentage of the "
				    +"mean).\n\t<default=0.01(1%)>",
				    "T", 1,"-T <num>"));

    if (w_ClassifierString != null)
      {
	try
	  {
	    Classifier temp = (Classifier)
	      Class.forName(w_ClassifierString).newInstance();
	    if (temp instanceof OptionHandler)
	      {
		newVector.
		  addElement(new Option("", "", 0, "\nOptions specific to"+
					"scheme "
					+temp.getClass().getName() + ":"));

		Enumeration enum = ((OptionHandler)temp).listOptions();

		while (enum.hasMoreElements()) 
		  {
		    newVector.addElement(enum.nextElement());
		  }
	      }
	  }
	catch (Exception e)
	  {
	    e.printStackTrace();
	  }
      }

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception
  {
     String optionString;

     resetOptions();

     optionString = Utils.getOption('B',options);
     if (optionString.length()==0)
       {
	 throw new Exception("A learning scheme must be specified.");
       }
     else
       addLearner(optionString, options);

     optionString = Utils.getOption('F',options);
     if (optionString.length() != 0)
       {
	 folds = Integer.parseInt(optionString);
       }

//       optionString = Utils.getOption('S',options);
//       if (optionString.length() != 0)
//         {
//  	 seed = Integer.parseInt(optionString);
//         }

     optionString = Utils.getOption('T',options);
     if (optionString.length() != 0)
       {
	 Double temp;
	 temp = Double.valueOf(optionString);
	 threshold = temp.doubleValue();
       }
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] options = new String [6];
    int current = 0;

    options[current++] = "-C"; options[current++] = ""+w_ClassifierString+
				 w_ClassifierOptionsString;
    options[current++] = "-X"; options[current++] = ""+folds;
    options[current++] = "-T"; options[current++] = ""+threshold;

     while (current < options.length) 
       {
	 options[current++] = "";
       }
     return options;
  }

  /**
   * Add the scheme to use for accuracy estimation.
   *
   * @param learnerString a string consisting of the class name of a classifier
   * @param options option list containing (potentially) options after "--"
   * for the classifier
   * @exception Exception if the learner class name is not valid or the 
   * classifier does not accept the supplied options
   */
  public void addLearner(String learnerString, String [] options)
    throws Exception
  {
    w_ClassifierString = learnerString;

     Classifier tempClassifier;
    try 
      {
      tempClassifier = (Classifier)Class.forName(w_ClassifierString).newInstance();
      } 
    catch (Exception ex) 
      {
      throw new Exception("Can't find Classifier with class name: "
                          + w_ClassifierString);
      }
    
     if (tempClassifier instanceof OptionHandler) 
      {
	String [] learnerOptions = Utils.partitionOptions(options);
	if (learnerOptions.length != 0)
	  {
	    StringBuffer tempS = new StringBuffer();
	    w_ClassifierOptions = new String [learnerOptions.length];
	    for (int i=0;i<learnerOptions.length;i++)
		tempS.append(" "+learnerOptions[i]);

	    System.arraycopy(learnerOptions,0,w_ClassifierOptions,
			     0,w_ClassifierOptions.length);
	    
	    w_ClassifierOptionsString = tempS.toString();

	    ((OptionHandler)tempClassifier).setOptions(learnerOptions);
	  }
      }
  }

  protected void resetOptions()
  {
    trainInstances = null;
    w_Evaluation = null;
    w_ClassifierString = null;
    w_ClassifierOptionsString = null;
    w_ClassifierOptions = null;
    folds = 5;
    seed = 1;
    threshold = 0.01;
  }

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data)  throws Exception
  { 
    trainInstances = data;
    classIndex = trainInstances.classIndex();
    numAttribs = trainInstances.numAttributes();
    numInstances = trainInstances.numInstances();
  }
  
  /**
   * evaluates a subset of attributes
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @exception Exception if the subset could not be evaluated
   */
  public double evaluateSubset(BitSet subset) throws Exception
  {
    double errorRate = 0;
    double [] repError = new double [5];
    boolean ok = true;
    int numAttributes=0;
    int i,j;
    Random Rnd = new Random(seed);

    DeleteFilter delTransform = new DeleteFilter();
    delTransform.setInvertSelection(true);

    // copy the instances
    Instances trainCopy = new Instances(trainInstances);

    // count attributes set in the BitSet
    for (i=0;i<numAttribs;i++)
      if (subset.get(i))
	numAttributes++;

    // set up an array of attribute indexes for the filter (+1 for the class)
    int [] featArray = new int [numAttributes+1];
    for (i=0,j=0;i<numAttribs;i++)
      if (subset.get(i))
	featArray[j++] = i;
    featArray[j] = classIndex;

    delTransform.setAttributeIndicesArray(featArray);
    delTransform.inputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy,delTransform);

    // max of 5 repititions ofcross validation
    for (i=0;i<5;i++)
      {
	// copy the learners options
	String [] tempOptions = null;
	if (w_ClassifierOptions != null)
	  {
	    tempOptions = new String[w_ClassifierOptions.length];
	    System.arraycopy(w_ClassifierOptions,0,tempOptions,
			     0,w_ClassifierOptions.length);
	  }

	trainCopy.randomize(Rnd); // randomize instances
	w_Evaluation = new Evaluation(trainCopy);
	w_Evaluation.crossValidateModel(w_ClassifierString,
					trainCopy,
					folds,
					tempOptions);

	repError[i] = w_Evaluation.errorRate();

	// check on the standard deviation
	if (!repeat(repError, i+1))
	  break;
      }

    for (j=0;j<i;j++)
      errorRate += repError[j];
    errorRate /= (double)i;
   
    return -errorRate;
  }

  /**
   * returns a string describing the wrapper
   *
   * @return the description as a string
   */
  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    text.append("\tWrapper Subset Evaluator\n");
    text.append("\tLearning scheme: "+w_ClassifierString+"\n");
    text.append("\tScheme options: ");
    if (w_ClassifierOptions != null)
      for (int i=0;i<w_ClassifierOptions.length;i++)
	text.append(w_ClassifierOptions[i]+" ");
    text.append("\n");

    text.append("\tNumber of folds for accuracy estimation: "+folds+"\n");

    return text.toString();
      
  }

  /**
   * decides whether to do another repeat of cross validation. If the
   * standard deviation of the cross validations
   * is greater than threshold% of the mean (default 1%) then another 
   * repeat is done. 
   *
   * @param repError an array of cross validation results
   * @param entries the number of cross validations done so far
   * @return true if another cv is to be done
   */
  private boolean repeat(double [] repError, int entries)
  {
    int i;
    double mean = 0;
    double variance = 0;

    if (entries == 1)
      return true;

    for (i=0;i<entries;i++)
      mean += repError[i];

    mean /= (double)entries;

    for (i=0;i<entries;i++)
      variance += ((repError[i] - mean)*(repError[i] - mean));

    variance /= (double)entries;
    if (variance > 0)
      variance = Math.sqrt(variance);

    if ((variance / mean) > threshold)
      return true;
    
    return false;
    
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file
   */
  
  public static void main(String [] argv)
  {
    
    try {
      System.out.println(AttributeSelection.SelectAttributes(new WrapperSubsetEval(), argv));
    }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
}
