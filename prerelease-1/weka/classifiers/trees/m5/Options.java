/*
 *    Options.java
 *    Copyright (C) 1999 Yong Wang
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
package weka.classifiers.m5;

import java.lang.*;
import java.io.*;
import weka.core.*;
 
/**
 * Class for handing options 
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version 1.0
 */

public final class Options implements Serializable {

  boolean smooth;           // =true, smoothed result; otherwise, unsmoothed result
  int     randomSeed;       // random seed for cross-validation 
  int     classcol;         // class column 
  int     verbosity;        // verbosity level, 0-2 
  int     model;            // output model type could be linearRegression (1),
                            // regressionTree (2), modelTree (3) 
  int     numFolds;         // the number of folds for cross-validation
  double  pruningFactor;    // pruning factor a in (n+ak)/(n-k)
  String  trainFile;        // name of the training file
  String  testFile ;        // name of the test file
  int     lmNo;             // linear model number,  falling into which tested instances will be printed 
  double  deviation;        // the global standard deviation of the class attribute of the instances, used for splitting stopping, scale determination of the class attribute
 
  final static String   VERSION="v1.1";

  public Options(Instances inst) {
    smooth = true;         
    randomSeed = 1;     
    classcol = inst.classIndex();       
    verbosity = 0;      
    model = Node.MODEL_TREE;          
    numFolds =10;     
    pruningFactor =2;
    trainFile = null;    
    testFile =null;    
    lmNo = 0;         
  }


  /**
   * Constructs an object to store command line options and other necessary 
   *     information
   * @param argv command line auguments
   */
  public Options(String [] argv){

    int count;
    char sw;
    String rest = new String();
    
    rest=null;
    classcol=-3;
    pruningFactor=1.0;
    randomSeed=1;
    model=Node.MODEL_TREE;
    trainFile=null;
    testFile=null;
    numFolds=0;
    lmNo = 0;

    if(argv.length<1 || argv[0].startsWith("-") != true)
      M5Utils.errorMsg("no training file specified. See -help.");

    count=0;
    while (count < argv.length && argv[count].startsWith("-") == true && 
	   argv[count].length() >=2) {
      sw  = argv[count].charAt(1);            // sw = switch 
      if(argv[count].length()>2)
	rest= argv[count].substring(2);       // rest = rest of string after sw
      else if(argv[count].length()==2 && count+1 < argv.length) {
	if (argv[count+1].startsWith("-") == false) { 
	  count++; rest = argv[count].toString(); 
	}
      }

      switch (sw) {
      case 'c':
	if(rest!=null){
	  if(rest.charAt(0)>48 && rest.charAt(0)<58)
	    classcol = Integer.parseInt(rest) - 1;
	  else if(rest.charAt(0)=='f')classcol=0;
	  else if(rest.charAt(0)=='l')classcol=-1;
	  else classcol=-2;
	}
	break;
      case 'f':
	if(rest!=null){
	  pruningFactor = Double.valueOf(rest).doubleValue();
	  if(pruningFactor<-0.01 || pruningFactor>10.01)
	    M5Utils.errorMsg("pruning factor out of limit (0.0 - 10.0).\n" +
			     "Default value 1.0. (0.0 - 3.0) is the " + 
			     "recommended range.");
	}
	break;
      case 'h':printValidOptions();
      case 'L': 
	lmNo = Integer.parseInt(rest);
	break;
      case 'o':
	model=Node.MODEL_TREE;
	if(rest!=null){
	  switch(rest.charAt(0)){
	  case '1':
	  case 'l':
	  case 'L':model=Node.LINEAR_REGRESSION; break;
	  case '2':
	  case 'r':
	  case 'R':model=Node.REGRESSION_TREE; break;
	  case '3':
	  case 'm':
	  case 'M':model=Node.MODEL_TREE; break;
	  default: M5Utils.errorMsg("unknown model type -o " + rest + 
				    " . See -help");System.exit(1);
	  }
	}
	break;	  
      case 's': numFolds=10;
	if(rest!=null){
	  randomSeed = Integer.parseInt(rest);
	  if(randomSeed<0){
	    M5Utils.errorMsg("randomization seed must be >= 0. " + 
			     "Default value is 1.");
	  }
	}
	break;
      case 't': 
	if(rest!=null){trainFile=rest.substring(0);} 
	else trainFile=null;
	break;
      case 'T': 
	if(rest!=null){testFile=rest.substring(0);} 
	else testFile=null;
       	break;
      case 'v':
	if(rest!=null){
	  verbosity = Integer.parseInt(rest); 
	  if(verbosity<0 || verbosity>2)
	    M5Utils.errorMsg("verbosity level should range within (0-2). " + 
			     "See -help.");
	}
	break;
    case 'x': numFolds=10;
      if (rest!=null) {
	numFolds=Integer.parseInt(rest); 
	if (numFolds<=1 || numFolds>100) {
	  M5Utils.errorMsg("fold number for cross-validation must be within" + 
			   " (2 - 100). See -help.");
	}
      }
      break;
      default:  if(rest==null)
	System.out.println("M5' error: Invalid option -" + sw);
      else M5Utils.errorMsg("invalid option -" + sw + " " + rest);
      System.exit(1);
      }
      rest=null;
      count++;
    }
    if(trainFile==null)
      M5Utils.errorMsg("no training file specified. See -help.");
  }
  
  /**
   * Initializes for constucting model trees
   * @param dataset a dataset 
   * @exception Exception if something goes wrong
   */
  public final void  initialize(Instances inst) throws Exception {
    
    FileInputStream inputStream;
    int i,j;
    int [] index = null;

    if(numFolds > inst.numInstances())
      M5Utils.errorMsg("fold number for cross-validation greater than the " +
		       "number of instances.");
    if(classcol==-3 || classcol==-1)classcol=inst.numAttributes()-1;
    if(inst.classAttribute().isNominal()==true)
      M5Utils.errorMsg("class column must be real or integer attribute.");
    if(verbosity<0 && (testFile==null || numFolds>=1))verbosity=0;
  }
  
  /**
   * Prints information stored in an 'Options' object, basically containing 
   *     command line options
   * @param dataset a dataset
   * @exception Exception if something goes wrong
   */
  public final String  toString(Instances inst) throws Exception {
    
    StringBuffer text = new StringBuffer();
    
    text.append("    Options:\n\n");
    text.append("        Training file   :     " + trainFile + "\n");
    if(testFile!=null) text.append("        Test file       :     "+testFile + 
				   "\n");
    text.append("        Class attribute :     "+inst.classAttribute().name()
		+ " (column "+ (classcol+1) +")\n");
    if(numFolds>1) text.append("        Cross-Validation:     "+numFolds+
			      "-fold with random seed "+randomSeed + "\n");
    text.append("        Verbosity level :     "+verbosity + "\n"); 
    if(model==Node.LINEAR_REGRESSION) 
      text.append("        Output model    :     linear regression" + "\n");
    if(model==Node.REGRESSION_TREE) 
      text.append("        Output model    :     regression tree" + "\n");
    if(model==Node.MODEL_TREE){
      text.append("        Pruning factor  :     "+pruningFactor + "\n"); 
      text.append("        Output model    :     model tree\n");
    }  
    text.append("\n");

    return text.toString();
  }

  /**
   * Prints valid command line options and simply explains the output
   */
  public final void printValidOptions()
  {
    System.out.println("Usage:");
    System.out.println("      M5Java [-options]\n");
    System.out.println("Options:");
    System.out.println("  -c (<num>|first|last)  column to predict values "+
		       "(default last)");
    System.out.println("  -f <num>               pruning factor 0.0 - 10.0 "+
		       "(default 1.0)");
    System.out.println("  -h                     displays this help");
    System.out.println("  -o <l|m|r>             output model: linear, "+
		       "model tree, or regression tree");
    System.out.println("  -s <num>               random seed for "+
		       "cross-validation only. No randomization");
    System.out.println("                         while 0 (default 1)");
    System.out.println("  -t <file>              training set file ");
    System.out.println("  -T <file>              test set file");
    System.out.println("  -v <num>               verbosity level 0,1,2 "+
		       "(default 0)");
    System.out.println("  -x <num>               cross validation "+
		       "(default 10-fold)\n");
    System.out.println("Definitions:");
    System.out.println("  Correlation coefficient: correlation between actual "+
		       "values and predictions");
    System.out.println("  Mean absolute error: average absolute prediction "+
		       "error");
    System.out.println("  Root mean squared error: square root of the average "+
		       "squared prediction error");
    System.out.println("  Relative absolute error: ratio of the mean absolute "+
		       "residuals to the absolute");
    System.out.println("      deviation of the target values");
    System.out.println("  Root relative squared error: square root of the "+
		       "ratio of the variance of the ");
    System.out.println("      residuals to the variance of the target values\n");
    System.out.println("  Note: 100% relative error is the same as would be "+
		       "obtained by predicting a");
    System.out.println("      simple average\n");
    System.out.println("Description:");
    System.out.println("  An unsmoothed prediction is calculated directly by "+
		       "the function at the leaf.");
    System.out.println("  A smoothed prediction uses the value calculated at "+
		       "the leaf of the tree,");
    System.out.println("  and passes it back up the tree, smoothing at each "+
		       "higher node.\n");
    System.out.println("  Let");
    System.out.println("\tp' be the model passed up to the next higher node,");
    System.out.println("\tp be the model passed to this node from below,");
    System.out.println("\tq be the model at this node,");
    System.out.println("\tn be the number of training instances that reach "+
		       "the node below,");
    System.out.println("\tk be a constant (default value 15),\n");
    System.out.println("  then the smoothed model at this node is:\n");
    System.out.println("\tp' = (n*p+k*q) / (n+k)\n");
    System.out.println("Version:");
    System.out.println("\t" + Options.VERSION);
    System.exit(1);
  }

}
