/*
 *    AttributeSelection.java
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

/** 
 * Attribute selection class. Takes the name of a search class and
 * an evaluation class on the command line. <p>
 *
 * Valid options are: <p>
 *
 * -h <br>
 * Display help. <p>
 *
 * -I <name of input file> <br>
 * Specify the training arff file. <p>
 * 
 * -C <class index> <br>
 * The index of the attribute to use as the class. <p>
 * 
 * -S <search method> <br>
 * The full class name of the search method followed by search method options
 * (if any).<br>
 * Eg. -S "weka.attributeSelection.BestFirst -N 10" <p>
 *
 * -P <range> <br>
 * Specify a set of attributes from which to start a search for subset
 * evaluators, OR, a list of attributes to IGNORE for attribute evaluators.
 * <br> Eg. -P 2,3,7-9. <p>
 *
 * -T <cutoff> <br>
 * Specify a threshold by which to discard attributes for attribute evaluators.
 * <p>
 *
 * -X <number of folds> <br>
 * Perform a cross validation. <p>
 *
 * -N <random number seed> <br>
 * Specify a random number seed. Use in conjuction with -X. (Default = 1). <p>
 * 
 * ------------------------------------------------------------------------ <p>
 * 
 * Example usage as the main of an attribute evaluator (called FunkyEvaluator):
 * <code> <pre>
 * public static void main(String [] args) {
 *   try {
 *     ASEvaluator eval = new FunkyEvaluator();
 *     System.out.println(SelectAttributes(Evaluator, args));
 *   } catch (Exception e) {
 *     System.err.println(e.getMessage());
 *   }
 * }
 * </code> </pre>
 * <p>
 *
 * ------------------------------------------------------------------------ <p>
 *
 * @author   Mark Hall (mhall@cs.waikato.ac.nz)
 * @version  $Revision: 1.4 $
 */
public class AttributeSelection {

  /**
   * Perform attribute selection.
   *
   * @param EvaluatorString strubg containing the class name of an
   * attribute/subset evaluator
   * @param options an array of options specifying arguments for the 
   * evaluator and the class name of a search method 
   * (if necessary) and its arguments.
   * @return the results of attribute selection as a String.
   */
  public static String SelectAttributes(String EvaluatorString, 
					String [] options) throws Exception
  {
    ASEvaluation ASEvaluator;

    try
      {
	ASEvaluator = (ASEvaluation)Class.forName(EvaluatorString).
	  newInstance();
      }
    catch (Exception ex)
      {
	throw new Exception("Can't find attribute/subset evaluator with class "
			    +"name: "
			    +options[0]);
      }

    return SelectAttributes(ASEvaluator, options);
  }

  /**
   * Perform attribute selection with a particular evaluator and
   * a set of options specifying search method and input file etc.
   *
   * @param ASEvaluator an evaluator object
   * @param options an array of options, not only for the evaluator
   * but also the search method (if any) and an input data file
   * @return the results of attribute selection as a String
   */
  public static String SelectAttributes(ASEvaluation ASEvaluator,
				       String [] options)
				       throws Exception
  {
    int [][] finalFeatSet = new int [1][0];
    String trainFileName, searchName;
    Instances train = null;
    ASSearch searchMethod = null;

    try
      {
	// get basic options (options the same for all attribute selectors
	 trainFileName = Utils.getOption('I', options); 
	 if (trainFileName.length() == 0)
	   {
	     searchName = Utils.getOption('S', options); 
	     if (searchName.length() != 0)	 
	       {
		 searchMethod = 
		   (ASSearch)Class.forName(searchName).newInstance();
	       }
	     throw new Exception("No training file given.");
	   }
      }
    catch (Exception e)
      {
	throw new Exception('\n' + e.getMessage()
			    + makeOptionString(ASEvaluator, searchMethod));
      }
    
    train = new Instances(new FileReader(trainFileName));

    return SelectAttributes(ASEvaluator, options, finalFeatSet, train);
  }

  /**
   * Perform a cross validation for attribute selection. With subset
   * evaluators the number of times each attribute is selected over
   * the cross validation is reported. For attribute evaluators, the
   * average merit and average ranking + std deviation is reported for
   * each attribute.
   *
   * @param ASEvaluator an evaluator object
   * @param options an array of options, not only for the evaluator
   * but also the search method (if any) and an input data file
   * @return the results of cross validation as a String
   */
  public static String CrossValidateAttributes(ASEvaluation ASEvaluator,
					     int [] initialSet,
					     ASSearch searchMethod,
					     boolean ranking,
					     Instances data,
					     int folds,
					     int seed,
					     double cutoff)
					     throws Exception
  {
    StringBuffer CvString = new StringBuffer();
    Instances cvData = new Instances(data);
    Instances train;
    double [][] rankResults;
    double [] subsetResults;
    double [][] attributeRanking = null;

    
    if (ASEvaluator instanceof UnsupervisedSubsetEvaluator)
      {
	subsetResults = new double [cvData.numAttributes()];
      }
    else
      {
	subsetResults = new double [cvData.numAttributes()-1];
      }
    
    if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	!(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
      {
	cvData.stratify(folds);
	rankResults = new double [4][cvData.numAttributes()-1];
      }
    else
      {
	rankResults = new double [4][cvData.numAttributes()];
      }
    
    for (int i=0;i<folds;i++)
      {
	// Perform attribute selection
	train = cvData.trainCV(folds, i);
	ASEvaluator.buildEvaluator(train);

	// Do the search
	int [] attributeSet = searchMethod.search(initialSet,ASEvaluator,train);
	
	// Do any postprocessing that a attribute selection method might 
	// require
	attributeSet = ASEvaluator.postProcess(attributeSet);

	if ((searchMethod instanceof RankedOutputSearch) && (ranking == true))
	  {
	    attributeRanking = 
	      ((RankedOutputSearch)searchMethod).rankedAttributes();

	    // System.out.println(attributeRanking[0][1]);
	    for (int j=0;j<attributeRanking.length;j++)
	      {
		// merit
		rankResults[0][(int)attributeRanking[j][0]] 
		  += attributeRanking[j][1];
		// squared merit
		rankResults[2][(int)attributeRanking[j][0]]
		  += (attributeRanking[j][1] * attributeRanking[j][1]);
		// rank
		rankResults[1][(int)attributeRanking[j][0]] += (j+1);
		// squared rank
		rankResults[3][(int)attributeRanking[j][0]]
		  += (j+1)*(j+1);
		// += (attributeRanking[j][0] * attributeRanking[j][0]);
	      }
	  }
	else
	  {
	    for (int j=0;j<attributeSet.length;j++)
	      subsetResults[attributeSet[j]]++;
	  }
      }

    CvString.append("\n\n=== Attribute selection "+folds+
		    " fold cross-validation ");
    if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	 !(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
      {
	CvString.append("(stratified) ===\n\n");
      }
    else CvString.append("===\n\n");
    
    if ((searchMethod instanceof RankedOutputSearch) && (ranking == true))
      {
	CvString.append("average merit      average rank  attribute\n");
	// calcualte means and std devs
	for (int i=0;i<attributeRanking.length;i++)
	  {
	    rankResults[0][i] /= folds; // mean merit
	    double var = rankResults[0][i]*rankResults[0][i]*folds;
	    var = (rankResults[2][i] - var);
	    var /= folds;
	    if (var <= 0.0)
	      {
		var = 0.0;
		rankResults[2][i] = 0;
	      }
	    else
	      {
		rankResults[2][i] = Math.sqrt(var);
	      }

	    rankResults[1][i] /= folds; // mean rank
	    var = rankResults[1][i]*rankResults[1][i]*folds;
	    var = (rankResults[3][i] - var);
	    var /= folds;
	    if (var <= 0.0)
	      {
		var = 0.0;
		rankResults[3][i] = 0;
	      }
	    else
	      {
		rankResults[3][i] = Math.sqrt(var);
	      }
	  }

	// now sort them by mean merit
	int [] s = Utils.sort(rankResults[0]);
	for (int i=s.length-1;i>=0;i--)
	  CvString.append(Utils.doubleToString(rankResults[0][s[i]],6,3)
			  +"+-"
			  +Utils.doubleToString(rankResults[2][s[i]],6,3)
			  +"   "
			  +Utils.doubleToString(rankResults[1][s[i]],6,1)
			  +"+-"
			  +Utils.doubleToString(rankResults[3][s[i]],5,2)
			  +"    "+(s[i]+1)
			  +" "+cvData.attribute(s[i]).name()+"\n");
      }
    else
      {
	CvString.append("number of folds (%)  attribute\n");
	for (int i=0;i<subsetResults.length;i++)
	  {
	    CvString.append(Utils.doubleToString(subsetResults[i],12,0)
			    +"("+Utils.doubleToString((subsetResults[i] 
						   / folds*100.0),3,0)
			    +" %)  "
			    +(i+1)+" "
			    +cvData.attribute(i).name()+"\n");
	  }
      }
    return CvString.toString();
  }

  /**
   * Perform attribute selection with a particular evaluator and
   * a set of options specifying search method and options for the
   * search method and evaluator.
   *
   * @param ASEvaluator an evaluator object
   * @param options an array of options, not only for the evaluator
   * but also the search method (if any) and an input data file
   * @param outAttributes index 0 will contain the array of selected
   * attribute indices
   * @param train the input instances
   * @return the results of attribute selection as a String
   */
  public static String SelectAttributes(ASEvaluation ASEvaluator,
				      String [] options,
				      int [][] outAttributes,
				      Instances train)
				      throws Exception
  {
    int seed = 1, folds = 10;
    String cutString, foldsString, seedString, searchName, rangeString;
    String classString;
    String searchClassName;
    String [] searchOptions = null;
    Random random;
    ASSearch searchMethod = null;
    boolean ranking = false;
    boolean doCrossVal = false;
    Range initialRange;
    int [] initialSet = null;
    int classIndex = -1;
    int [] selectedAttributes;
    double cutoff = -Double.MAX_VALUE;
    boolean helpRequested = false;

    StringBuffer text = new StringBuffer();
    initialRange = new Range();

    try
      {
	if (Utils.getFlag('h',options))
	  {
	    helpRequested = true;
	  }
	
	// get basic options (options the same for all attribute selectors
	 classString = Utils.getOption('C', options);
	 if (classString.length() != 0) 
	   {
	     classIndex = Integer.parseInt(classString);
	   }

	 if ((classIndex != -1) && 
	     ((classIndex == 0) || (classIndex > train.numAttributes())))
	   {
	     throw new Exception("Class index out of range.");
	   }

	 if (classIndex != -1)
	   {
	     train.setClassIndex(classIndex-1);
	   }
	 else
	   {
	     classIndex = train.numAttributes();
	     train.setClassIndex(classIndex-1);
	   }

	 foldsString = Utils.getOption('X', options);
	 if (foldsString.length() != 0) 
	   {
	     folds = Integer.parseInt(foldsString);
	     doCrossVal=true;
	   }

	 seedString = Utils.getOption('N', options);
	 if (seedString.length() != 0) 
	   {
	     seed = Integer.parseInt(seedString);
	   }
	 
	 ranking = Utils.getFlag('R',options);

	 // Attribute Evaluators always result in a ranked list of attributes
	 if (ASEvaluator instanceof AttributeEvaluator)
	   {
	     ranking = true;
	   }

	 searchName = Utils.getOption('S', options); 
	 if ((searchName.length() == 0) && 
	     (!(ASEvaluator instanceof AttributeEvaluator)))
	   {
	     throw new Exception("No search method given.");
	   }
	 
	 if ((searchName.length() != 0) &&
	     (ASEvaluator instanceof AttributeEvaluator))
	   {
	     throw new Exception("Can't specify search method for "
				 +"attribute evaluators.");
	   }

	 if (!(ASEvaluator instanceof AttributeEvaluator))
	   {
	     searchName = searchName.trim();
	     // split off any search options
	     int breakLoc = searchName.indexOf(' ');
	     searchClassName = searchName;
	     String searchOptionsString = "";
	     if (breakLoc != -1) 
	       {
		 searchClassName = searchName.substring(0, breakLoc);
		 searchOptionsString = searchName.substring(breakLoc).trim();
		 searchOptions = Utils.splitOptions(searchOptionsString);
	       }
	   }
	 else
	   {
	     searchClassName = new String("weka.attributeSelection.Ranker");
	   }

	 searchMethod = (ASSearch)Class.forName(searchClassName).newInstance();

	 if (ranking && !(searchMethod instanceof RankedOutputSearch))
	   {
	     throw new Exception(searchName+" is not capable of ranking "+
				 "attributes.");
	   }

	 cutString = Utils.getOption('T', options);
	 if (cutString.length() != 0)
	   {
	     Double temp;
	     temp = Double.valueOf(cutString);
	     cutoff = temp.doubleValue();
	   }

	 if (cutoff != -Double.MAX_VALUE && 
	     !(searchMethod instanceof RankedOutputSearch))
	   {
	     throw new Exception("can't use a cuttof with a non ranking "
				 +"search method.\n");
	   }

	 rangeString = Utils.getOption('P', options); 
	 if (rangeString.length() != 0)
	   {
	     initialRange.setUpper(train.numAttributes()-1);
	     initialRange.setRanges(rangeString);
	     initialSet = initialRange.getSelection();
	   }
      }
    catch (Exception e)
      {
	 throw new Exception('\n' + e.getMessage()
			     + makeOptionString(ASEvaluator, searchMethod));
       }

    try
      {
	// Set options for ASEvaluator
	if (ASEvaluator instanceof OptionHandler)
	  {
	    ((OptionHandler)ASEvaluator).setOptions(options);
	  }
	
	// Set options for Search method
	if (searchMethod instanceof OptionHandler)
	  {
	    if (searchOptions != null)
	      {
		((OptionHandler)searchMethod).setOptions(searchOptions);
	      }
	  }
	Utils.checkForRemainingOptions(searchOptions);
      }
    catch (Exception e)
      {
	throw new Exception("\n" + e.getMessage()+
			    makeOptionString(ASEvaluator, searchMethod));
      }
    
    try
      {
	Utils.checkForRemainingOptions(options);
      }
    catch (Exception e)
      {
	throw new Exception('\n' + e.getMessage()
			    + makeOptionString(ASEvaluator, searchMethod));
      }

    if (helpRequested)
      {
	System.out.println(makeOptionString(ASEvaluator, searchMethod));
	System.exit(0);
      }

    // Initialize the attribute evaluator
    ASEvaluator.buildEvaluator(train);

    // Do the search
    int [] attributeSet = searchMethod.search(initialSet,ASEvaluator,train);

    // Do any postprocessing that a attribute selection method might require
    attributeSet = ASEvaluator.postProcess(attributeSet);

    text.append(printSelectionResults(ASEvaluator, searchMethod, train));
    
    if ((searchMethod instanceof RankedOutputSearch) && ranking == true)
      {
	double [][] attributeRanking = 
	  ((RankedOutputSearch)searchMethod).rankedAttributes();

	text.append("Ranked attributes:\n");
	for (int i=0;i<attributeRanking.length;i++)
	  {
	    if (attributeRanking[i][1] > cutoff)
	      {
		text.append(Utils.doubleToString(attributeRanking[i][1],6,3)
			    +Utils.doubleToString((attributeRanking[i][0]+1)
						  ,5,0)
			    +" "
			    +train.attribute((int)attributeRanking[i][0]).
			    name()
			    +"\n");
	      }
	  }

	int count = 0;
	for (int i=0;i<attributeRanking.length;i++)
	  {
	    if (attributeRanking[i][1] > cutoff)
	      {
		count++;
	      }
	  }

	// set up the selected attributes array - usable by a filter or
	// whatever
	if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	    !(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
	  // one more for the class
	  {
	    selectedAttributes = new int [count+1];
	    selectedAttributes[count] = train.classIndex();
	  }
	else
	  {
	    selectedAttributes = new int [count];
	  }

	text.append("\nSelected attributes: ");
	for (int i=0;i<attributeRanking.length;i++)
	  {
	    if (attributeRanking[i][1] > cutoff)
	      {
		selectedAttributes[i] = (int)attributeRanking[i][0];
	      }

	    if (i == (attributeRanking.length-1))
	      {
		if (attributeRanking[i][1] > cutoff)
		  {
		    text.append(((int)attributeRanking[i][0]+1)+" : " 
			      + (i+1)
			      + "\n");
		  }
	      }
	    else
	      {
		if (attributeRanking[i][1] > cutoff)
		  {
		    text.append(((int)attributeRanking[i][0]+1));
		    if (attributeRanking[i+1][1] > cutoff)
		      {
			text.append(",");
		      }
		    else
		      {
			text.append(" : "+(i+1)+"\n");
		      }
		  }
	      }
	  }
      }
    else
      {
	// set up the selected attributes array - usable by a filter or
	// whatever
	if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	    !(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
	  // one more for the class
	  {
	    selectedAttributes = new int [attributeSet.length+1];
	    selectedAttributes[attributeSet.length] = train.classIndex();
	  }
	else
	  {
	    selectedAttributes = new int [attributeSet.length];
	  }
	
	for (int i=0;i<attributeSet.length;i++)
	  {
	    selectedAttributes[i] = attributeSet[i];
	  }

	text.append("Selected attributes: ");
	for (int i=0;i<attributeSet.length;i++)
	  {
	    if (i == (attributeSet.length-1))
	      {
		text.append((attributeSet[i]+1)
			    +" : "
			    +attributeSet.length
			    +"\n\t");
	      }
	    else
	      {
		text.append((attributeSet[i]+1)+",");
	      }
	  }
      }

    /* outAttributes = new int[selectedAttributes.length];
    System.arraycopy(selectedAttributes, 0, outAttributes, 0, 
    selectedAttributes.length); */

    outAttributes[0] = selectedAttributes;
    
    // Cross validation should be called from here
    if (doCrossVal == true)
      {
	text.append(CrossValidateAttributes(ASEvaluator, initialSet, 
					    searchMethod, ranking, 
					    train, folds, seed, 
					    cutoff));
      }

    return text.toString();
  }

  /**
   * Assembles a text description of the attribute selection results.
   *
   * @param ASEvaluator the attribute/subset evaluator
   * @param searchMethod the search method
   * @param train the input instances
   * @return a string describing the results of attribute selection.
   */
  private static String printSelectionResults(ASEvaluation ASEvaluator,
				       ASSearch searchMethod,
				       Instances train)
  {
    StringBuffer text = new StringBuffer();

    text.append("\n\n=== Attribute Selection on all input data ===\n\n"+
		"Search Method:\n");
    text.append(searchMethod.toString());
    text.append("\nAttribute ");
    if (ASEvaluator instanceof SubsetEvaluator)
      {
	text.append("Subset Evaluator (");
      }
    else
      {
	text.append("Evaluator (");
      }

    if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	!(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
      {
	text.append("supervised, ");
	text.append("Class (");
	if (train.attribute(train.classIndex()).isNumeric())
	  {
	    text.append("numeric): ");
	  }
	else
	  {
	    text.append("nominal): ");
	  }
	text.append((train.classIndex()+1)+" "+
		    train.attribute(train.classIndex()).name()+
		    "):\n");
      }
    else
      {
	text.append("unsupervised):\n");
      }

    text.append(ASEvaluator.toString()+"\n");
    
    return text.toString();
  }


  /**
   * Make up the help string giving all the command line options
   *
   * @param ASEvaluator the attribute evaluator to include options for
   * @param searchMethod the search method to include options for
   * @return a string detailing the valid command line options
   */
  private static String makeOptionString(ASEvaluation ASEvaluator,
					 ASSearch searchMethod)
    throws Exception
  {

    StringBuffer optionsText = new StringBuffer("");

    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-h display this help\n");
    optionsText.append("-I <name of input file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-C <class index>\n");
    optionsText.append("\tSets the class index for supervised attribute\n");
    optionsText.append("\tselection. Default=last column.\n");
    optionsText.append("-S <Class name>\n");
    optionsText.append("\tSets search method for subset evaluators.\n");
    optionsText.append("-P <range>\n");
    optionsText.append("\tSpecify a (optional) set of attributes to start\n");
    optionsText.append("\tthe search from, eg 1,2,5-9.\n");
    optionsText.append("-R\n");
    optionsText.append("\tProduce a attribute ranking if the specified\n");
    optionsText.append("\tsearch method is capable of doing so.\n");
    optionsText.append("-T <cutoff>\n");
    optionsText.append("\tThreshold by which to discard attributes\n");
    optionsText.append("\tfor attribute evaluators\n");
    optionsText.append("\tfrom a ranked list "
		       +"(use with attribute evaluators\n");
    optionsText.append("\tand ranked search.\n");
    optionsText.append("-X <number of folds>\n");
    optionsText.append("\tPerform a cross validation.\n");
    optionsText.append("-N <random number seed>\n");
    optionsText.append("\tUse in conjunction with -X.\n");

    // Get attribute evaluator-specific options

    if (ASEvaluator instanceof OptionHandler) 
    {
      optionsText.append("\nOptions specific to "
			  + ASEvaluator.getClass().getName()
			  + ":\n\n");

      Enumeration enum = ((OptionHandler)ASEvaluator).listOptions();
      while (enum.hasMoreElements()) 
      {
	Option option = (Option) enum.nextElement();
	optionsText.append(option.synopsis() + '\n');
	optionsText.append(option.description() + "\n");
      }
    }

    if (searchMethod != null)
      {
	if (searchMethod instanceof OptionHandler) 
	  {
	    optionsText.append("\nOptions specific to "
			       + searchMethod.getClass().getName()
			       + ":\n\n");

	    Enumeration enum = ((OptionHandler)searchMethod).listOptions();
	    while (enum.hasMoreElements()) 
	      {
		Option option = (Option) enum.nextElement();
		optionsText.append(option.synopsis() + '\n');
		optionsText.append(option.description() + "\n");
	      }
	  }
      }
    else System.out.println("No search method given.");

    return optionsText.toString();
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
	if (args.length == 0)
	  {
	    throw new Exception("The first argument must be the name of an "
				+"attribute/subset evaluator");
	  }
	String Evaluator = args[0];
	args[0]="";
	System.out.println(SelectAttributes(Evaluator, args));
      }
    catch (Exception e)
      {
	System.out.println(e.getMessage());
      }
  }
}
