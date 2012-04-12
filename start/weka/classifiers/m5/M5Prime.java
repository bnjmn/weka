/*
 *    M5Prime.java
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

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.filters.*;

/**
 * Class for contructing and evaluating model trees; M5' algorithm
 *
 * Valid options are: <p>
 *
 * -O <l|r|m> <br>
 * Type of model to be used. (l: linear regression, 
 * r: regression tree, m: model tree) <p>
 *
 * -U <br>
 * Use unsmoothed tree. <p>
 *
 * -F factor <br>
 * Set pruning factor. <p>
 *
 * -V <0|1|2> <br>
 * Verbosity. <p>
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version 1.1
 */

public final class  M5Prime extends Classifier implements OptionHandler {
  
  Node root[];
  Options options;
  boolean m_UseUnsmoothed = false;
  double m_PruningFactor = 2;
  int m_Model = Node.MODEL_TREE;
  int m_Verbosity = 0;
  ReplaceMissingValuesFilter m_ReplaceMissingValuesFilter;
  NominalToBinaryFilter m_NominalToBinaryFilter;

  /**
   * Construct a model tree by training instances
   * @param inst training instances
   * @param options information for constructing the model tree, 
   *        mostly from command line options
   * @return the root of the model tree
   * @exception Exception if the classifier can't be built
   */
  public final void buildClassifier(Instances inst) throws Exception{

    options = new Options(inst);

    options.model = m_Model;
    options.smooth = !m_UseUnsmoothed;
    options.pruningFactor = m_PruningFactor;
    options.verbosity = m_Verbosity;

    if(inst.classAttribute().isNominal()) throw new Exception("class is nominal."); 

    inst = new Instances(inst);
    inst.deleteWithMissingClass();
    m_ReplaceMissingValuesFilter = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValuesFilter.inputFormat(inst);
    inst = Filter.useFilter(inst, m_ReplaceMissingValuesFilter);
    m_NominalToBinaryFilter = new NominalToBinaryFilter();
    m_NominalToBinaryFilter.inputFormat(inst);
    inst = Filter.useFilter(inst, m_NominalToBinaryFilter);
    
    root = new Node[2];
    options.deviation = M5Utils.stdDev(inst.classIndex(),inst);

    root[0] = new Node(inst,null,options);       // build an empty tree
    root[0].split(inst);         // build the unpruned initial tree
    root[0].numLeaves(0);       // set tree leaves' number of the unpruned treee

    root[1] = root[0].copy(null);  // make a copy of the unpruned tree
    root[1].prune();            // prune the tree

    if(options.model != Node.LINEAR_REGRESSION){
      root[1].smoothen();    // compute the smoothed linear models at the leaves
      root[1].numLeaves(0);  // set tree leaves' number of the pruned tree
    }
  }

  /**
   * Classifies the given test instance.
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @exception Exception if the instance can't be classified
   */

  public double classifyInstance(Instance ins) throws Exception {
    m_ReplaceMissingValuesFilter.input(ins);
    ins = m_ReplaceMissingValuesFilter.output();
    m_NominalToBinaryFilter.input(ins);
    ins = m_NominalToBinaryFilter.output();
    return root[1].predict(ins,!m_UseUnsmoothed);
  }

  /**
   * Returns an enumeration describing the available options
   *
   * Valid options are: <p>
   *
   * -O <l|r|m> <br>
   * Type of model to be used. (l: linear regression, 
   * r: regression tree, m: model tree) <p>
   *
   * -U <br>
   * Use unsmoothed tree. <p>
   *
   * -F factor <br>
   * Set pruning factor. <p>
   *
   * -V <0|1|2> <br>
   * Verbosity. <p>
   *
   * @return an enumeration of all the available options
   */

  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tType of model to be used.\n"+
				    "\tl: linear regression"+
				    "\tr: regression tree"+
				    "\tm: model tree",
				    "-O", 1, "-O <l|r|m>"));
    newVector.addElement(new Option("\tUse unsmoothed tree.", "C", 0, 
				    "-U"));
    newVector.addElement(new Option("\tPruning factor.",
				    "-F", 1, "-F <double>"));
    newVector.addElement(new Option("\tVerbosity.",
				    "-V", 1, "-V <0|1|2>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */

  public void setOptions(String[] options) throws Exception{

    String modelString = Utils.getOption('O', options);
    if (modelString.length() != 0) {
      if (modelString.equals("l"))
	m_Model = Node.LINEAR_REGRESSION;
      else if (modelString.equals("r"))
	m_Model = Node.REGRESSION_TREE;
      else if (modelString.equals("m"))
	m_Model = Node.MODEL_TREE;
      else
	throw new Exception("Don't know model type "+modelString);
    } else {
      m_Model = Node.MODEL_TREE;
    }

    m_UseUnsmoothed = Utils.getFlag('U', options);
    if (m_Model != Node.MODEL_TREE)
      m_UseUnsmoothed = true;

    String pruningString = Utils.getOption('F', options);
    if (pruningString.length() != 0) {
      m_PruningFactor = (new Double(pruningString)).doubleValue();
    } else {
      m_PruningFactor = 2;
    }
    
    String verbosityString = Utils.getOption('V', options);
    if (verbosityString.length() != 0) 
      m_Verbosity = Integer.parseInt(verbosityString);
    else
      m_Verbosity = 0;
  }
  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [7];
    int current = 0;

    switch (m_Model) {
    case Node.MODEL_TREE:
      options[current++] = "-O"; options[current++] = "m";
      if (m_UseUnsmoothed) {
	options[current++] = "-U";
      }
      break;
    case Node.REGRESSION_TREE:
      options[current++] = "-O"; options[current++] = "r";
      break;
    case Node.LINEAR_REGRESSION:
      options[current++] = "-O"; options[current++] = "l";
      break;
    }
    options[current++] = "-F"; options[current++] = "" + m_PruningFactor;
    options[current++] = "-V"; options[current++] = "" + m_Verbosity;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Converts the output of the training precess into a string
   * @param root[] both the unpruned and the pruned trees stored in 
   *        root[0] and root[1]
   * @param measures the evaluation results for both the unsmoothed and 
   *        smoothed results of the pruned model tree
   * @param str either "t" for training, or "f" for fold training
   * @return the converted string
   */
  public final String toString() {

    try{
      StringBuffer text = new StringBuffer();
      double absDev = M5Utils.absDev(root[0].instances.classIndex(),
				     root[0].instances);
      
      if(options.verbosity >= 1 && options.model != Node.LINEAR_REGRESSION){
	switch(root[0].model){
	case Node.LINEAR_REGRESSION: 
	  break;
	case Node.REGRESSION_TREE  : 
	  text.append("Unpruned training regression tree:\n"); break;
	case Node.MODEL_TREE       : 
	  text.append("Unpruned training model tree:\n"); break;
	}     
	if(root[0].type == false)text.append("\n");
	
	text.append(root[0].treeToString(0,absDev)+ "\n");
	text.append("Models at the leaves:\n\n");
	//    the linear models at the leaves of the unpruned tree
	text.append(root[0].formulaeToString(false) + "\n");;  
      }
      
      if(root[0].model != Node.LINEAR_REGRESSION){
	switch(root[0].model){
	case Node.LINEAR_REGRESSION: 
	  break;
	case Node.REGRESSION_TREE  : 
	  text.append("Pruned training regression tree:\n"); break;
	case Node.MODEL_TREE       : 
	  text.append("Pruned training model tree:\n"); break;
	}
	if(root[1].type == false)text.append("\n");
	text.append(root[1].treeToString(0,absDev) + "\n"); //the pruned tree
	text.append("Models at the leaves:\n\n");
	if ((root[0].model != Node.LINEAR_REGRESSION) &&
	    (m_UseUnsmoothed)) {
	  text.append("  Unsmoothed (simple):\n\n");
	  //     the unsmoothed linear models at the leaves of the pruned tree
	  text.append(root[1].formulaeToString(false) + "\n");
	}
	if ((root[0].model == Node.MODEL_TREE) &&
	    (!m_UseUnsmoothed)) {
	  text.append("  Smoothed (complex):\n\n");
	  text.append(root[1].formulaeToString(true) + "\n");
	  //   the smoothed linear models at the leaves of the pruned tree
	}
      }
      else {
	text.append("Training linear regression model:\n\n");
	text.append(root[1].unsmoothed.toString(root[1].instances,0) + "\n\n");
	//       print the linear regression model
      }
      
      text.append("Number of Leaves : "+root[1].numberOfLinearModels());

      return text.toString();
    } catch (Exception e) {
      return "can't print m5' tree";
    }
  }

  /**
   * Main method for M5' algorithm
   * @param argv command line arguments
   */
  public static void  main(String [] argv){

    try {
      System.out.println(Evaluation.evaluateModel(new M5Prime(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}

     





