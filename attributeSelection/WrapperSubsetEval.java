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
 * Wrapper attribute subset evaluator. <p>
 * For more information see: <br>
 * 
 * Kohavi, R., John G., Wrappers for Feature Subset Selection. 
 * In <i>Artificial Intelligence journal</i>, special issue on relevance, 
 * Vol. 97, Nos 1-2, pp.273-324. <p>
 *
 * Valid options are:<p>
 *
 * -B <base learner> <br>
 * Class name of base learner to use for accuracy estimation.
 * Place any classifier options last on the command line following a
 * "--". Eg  -B weka.classifiers.NaiveBayes ... -- -K <p>
 *
 * -F <num> <br>
 * Number of cross validation folds to use for estimating accuracy.
 * <default=5> <p>
 *
 * -T <num> <br>
 * Threshold by which to execute another cross validation (standard deviation
 * ---expressed as a percentage of the mean). <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class WrapperSubsetEval 
  extends SubsetEvaluator
  implements OptionHandler {

  /** training instances */
  private Instances m_trainInstances;

  /** class index */
  private int m_classIndex;
  
  /** number of attributes in the training data */
  private int m_numAttribs;

  /** number of instances in the training data */
  private int m_numInstances;

  /** holds an evaluation object */
  private Evaluation m_Evaluation;

  /** holds the name of the classifer to estimate accuracy with */
  private String m_ClassifierString;

  /** holds the options to the classifier as a string */
  private String m_ClassifierOptionsString;

  /** holds the split-up options to the classifier */
  private String [] m_ClassifierOptions;
  
  /** number of folds to use for cross validation */
  private int m_folds;

  /** random number seed */
  private int m_seed;

  /** 
   * the threshold by which to do further cross validations when
   * estimating the accuracy of a subset
   */
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
				    +"\n\t(default=5)", "F", 1,"-F <num>"));

    //    newVector.addElement(new Option("\tseed", "S", 1,"-S <num>"));

    newVector.addElement(new Option("\tthreshold by which to execute "
				    +"another cross validation"
				    +"\n\t(standard deviation---"
				    +"expressed as a percentage of the "
				    +"mean).\n\t(default=0.01(1%))",
				    "T", 1,"-T <num>"));

    if (m_ClassifierString != null)
      {
	try
	  {
	    Classifier temp = (Classifier)
	      Class.forName(m_ClassifierString).newInstance();
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
   *
   * Valid options are:<p>
   *
   * -B <base learner> <br>
   * Class name of base learner to use for accuracy estimation.
   * Place any classifier options last on the command line following a
   * "--". Eg  -B weka.classifiers.NaiveBayes ... -- -K <p>
   *
   * -F <num> <br>
   * Number of cross validation folds to use for estimating accuracy.
   * <default=5> <p>
   *
   * -T <num> <br>
   * Threshold by which to execute another cross validation (standard deviation
   * ---expressed as a percentage of the mean). <p>
   *
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
       {
	 addLearner(optionString, options);
       }

     optionString = Utils.getOption('F',options);
     if (optionString.length() != 0)
       {
	 m_folds = Integer.parseInt(optionString);
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
    int extra=0;
    if (m_ClassifierOptions != null)
      {
	extra = m_ClassifierOptions.length;
      }
    String [] options = new String [7+extra];
    int current = 0;

    options[current++] = "-B"; options[current++] = ""+m_ClassifierString;
    options[current++] = "-F"; options[current++] = ""+m_folds;
    options[current++] = "-T"; options[current++] = ""+threshold;

    if (m_ClassifierOptions != null)
      {
	options[current++]="--";
	for (int i=0;i<m_ClassifierOptions.length;i++)
	  {
	    options[current++] = ""+m_ClassifierOptions[i];
	  }
      }

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
    m_ClassifierString = learnerString;

     Classifier tempClassifier;
    try 
      {
	tempClassifier = (Classifier)Class.
	  forName(m_ClassifierString).newInstance();
      } 
    catch (Exception ex) 
      {
	throw new Exception("Can't find Classifier with class name: "
			    + m_ClassifierString);
      }
    
     if (tempClassifier instanceof OptionHandler) 
      {
	String [] learnerOptions = Utils.partitionOptions(options);
	if (learnerOptions.length != 0)
	  {
	    StringBuffer tempS = new StringBuffer();
	    m_ClassifierOptions = new String [learnerOptions.length];

	    for (int i=0;i<learnerOptions.length;i++)
	      {
		tempS.append(" "+learnerOptions[i]);
	      }

	    System.arraycopy(learnerOptions,0,m_ClassifierOptions,
			     0,m_ClassifierOptions.length);
	    
	    m_ClassifierOptionsString = tempS.toString();

	    ((OptionHandler)tempClassifier).setOptions(learnerOptions);
	  }
      }
  }

  protected void resetOptions()
  {
    m_trainInstances = null;
    m_Evaluation = null;
    m_ClassifierString = null;
    m_ClassifierOptionsString = null;
    m_ClassifierOptions = null;
    m_folds = 5;
    m_seed = 1;
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
    if (data.checkForStringAttributes()) 
      {
	throw new Exception("Can't handle string attributes!");
      }

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();
  }
  
  /**
   * Evaluates a subset of attributes
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
    Random Rnd = new Random(m_seed);

    AttributeFilter delTransform = new AttributeFilter();
    delTransform.setInvertSelection(true);

    // copy the instances
    Instances trainCopy = new Instances(m_trainInstances);

    // count attributes set in the BitSet
    for (i=0;i<m_numAttribs;i++)
      {
	if (subset.get(i))
	  {
	    numAttributes++;
	  }
      }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int [] featArray = new int [numAttributes+1];
    for (i=0,j=0;i<m_numAttribs;i++)
      {
	if (subset.get(i))
	  {
	    featArray[j++] = i;
	  }
      }
    featArray[j] = m_classIndex;

    delTransform.setAttributeIndicesArray(featArray);
    delTransform.inputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy,delTransform);

    // max of 5 repititions ofcross validation
    for (i=0;i<5;i++)
      {
	// copy the learners options
	String [] tempOptions = null;
	if (m_ClassifierOptions != null)
	  {
	    tempOptions = new String[m_ClassifierOptions.length];
	    System.arraycopy(m_ClassifierOptions,0,tempOptions,
			     0,m_ClassifierOptions.length);
	  }

	trainCopy.randomize(Rnd); // randomize instances
	m_Evaluation = new Evaluation(trainCopy);
	m_Evaluation.crossValidateModel(m_ClassifierString,
					trainCopy,
					m_folds,
					tempOptions);

	repError[i] = m_Evaluation.errorRate();

	// check on the standard deviation
	if (!repeat(repError, i+1))
	  {
	    break;
	  }
      }

    for (j=0;j<i;j++)
      {
	errorRate += repError[j];
      }
    errorRate /= (double)i;
   
    return -errorRate;
  }

  /**
   * Returns a string describing the wrapper
   *
   * @return the description as a string
   */
  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    if (m_trainInstances == null)
      {
	text.append("\tWrapper subset evaluator has not been built yet\n");
      }
    else
      {
	text.append("\tWrapper Subset Evaluator\n");
	text.append("\tLearning scheme: "+m_ClassifierString+"\n");
	text.append("\tScheme options: ");
	if (m_ClassifierOptions != null)
	  {
	    for (int i=0;i<m_ClassifierOptions.length;i++)
	      {
		text.append(m_ClassifierOptions[i]+" ");
	      }
	  }
	text.append("\n");
	text.append("\tNumber of folds for accuracy estimation: "
		    +m_folds
		    +"\n");
      }
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
      {
	return true;
      }

    for (i=0;i<entries;i++)
      {
	mean += repError[i];
      }

    mean /= (double)entries;

    for (i=0;i<entries;i++)
      {
	variance += ((repError[i] - mean)*(repError[i] - mean));
      }

    variance /= (double)entries;
    if (variance > 0)
      {
	variance = Math.sqrt(variance);
      }

    if ((variance / mean) > threshold)
      {
	return true;
      }
    
    return false;
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String [] args)
  {
    try 
      {
	System.out.println(AttributeSelection.
			   SelectAttributes(new WrapperSubsetEval(), args));
      }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
}
