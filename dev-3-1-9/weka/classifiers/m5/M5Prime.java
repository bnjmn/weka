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
 * Class for contructing and evaluating model trees; M5' algorithm. <p>
 *
 * Reference: Wang, Y. and Witten, I.H. (1997). <i> Induction of model
 * trees for predicting continuous classes.</i> Proceedings of the poster
 * papers of the European Conference on Machine Learning. University of
 * Economics, Faculty of Informatics and Statistics, Prague. <p>
 *
 * Valid options are: <p>
 *
 * -O <l|r|m> <br>
 * Type of model to be used. (l: linear regression, 
 * r: regression tree, m: model tree) (default: m) <p>
 *
 * -U <br>
 * Use unsmoothed tree. <p>
 *
 * -F factor <br>
 * Set pruning factor (default: 2). <p>
 *
 * -V <0|1|2> <br>
 * Verbosity (default: 0). <p>
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $
 */
public final class  M5Prime extends Classifier implements OptionHandler,
 AdditionalMeasureProducer {
  
  /** The root node */
  private Node m_root[];

  /** The options */
  private Options options;

  /** No smoothing? */
  private boolean m_UseUnsmoothed = false;

  /** Pruning factor */
  private double m_PruningFactor = 2;

  /** Type of model */
  private int m_Model = Node.MODEL_TREE;

  /** Verbosity */
  private int m_Verbosity = 0;

  /** Filter for replacing missing values. */
  private ReplaceMissingValuesFilter m_ReplaceMissingValuesFilter;

  /** Filter for replacing nominal attributes with numeric binary ones. */
  private NominalToBinaryFilter m_NominalToBinaryFilter;

  public static final int MODEL_LINEAR_REGRESSION = Node.LINEAR_REGRESSION;
  public static final int MODEL_REGRESSION_TREE = Node.REGRESSION_TREE;
  public static final int MODEL_MODEL_TREE = Node.MODEL_TREE;
  public static final Tag [] TAGS_MODEL_TYPES = {
    new Tag(MODEL_LINEAR_REGRESSION, "Simple linear regression"),
    new Tag(MODEL_REGRESSION_TREE, "Regression tree"),
    new Tag(MODEL_MODEL_TREE, "Model tree")
  };
  
  /**
   * Construct a model tree by training instances
   *
   * @param inst training instances
   * @param options information for constructing the model tree, 
   * mostly from command line options
   * @return the root of the model tree
   * @exception Exception if the classifier can't be built
   */
  public final void buildClassifier(Instances inst) throws Exception{

    if (inst.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    options = new Options(inst);

    options.model = m_Model;
    options.smooth = !m_UseUnsmoothed;
    options.pruningFactor = m_PruningFactor;
    options.verbosity = m_Verbosity;

    if(!inst.classAttribute().isNumeric()) 
      throw new Exception("Class has to be numeric."); 

    inst = new Instances(inst);
    inst.deleteWithMissingClass();
    m_ReplaceMissingValuesFilter = new ReplaceMissingValuesFilter();
    m_ReplaceMissingValuesFilter.inputFormat(inst);
    inst = Filter.useFilter(inst, m_ReplaceMissingValuesFilter);
    m_NominalToBinaryFilter = new NominalToBinaryFilter();
    m_NominalToBinaryFilter.inputFormat(inst);
    inst = Filter.useFilter(inst, m_NominalToBinaryFilter);
    
    m_root = new Node[2];
    options.deviation = M5Utils.stdDev(inst.classIndex(),inst);

    m_root[0] = new Node(inst,null,options);       // build an empty tree
    m_root[0].split(inst);         // build the unpruned initial tree
    m_root[0].numLeaves(0);       // set tree leaves' number of the unpruned treee

    m_root[1] = m_root[0].copy(null);  // make a copy of the unpruned tree
    m_root[1].prune();            // prune the tree

    if(options.model != Node.LINEAR_REGRESSION){
      m_root[1].smoothen();    // compute the smoothed linear models at the leaves
      m_root[1].numLeaves(0);  // set tree leaves' number of the pruned tree
    }
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance ins) throws Exception {

    m_ReplaceMissingValuesFilter.input(ins);
    ins = m_ReplaceMissingValuesFilter.output();
    m_NominalToBinaryFilter.input(ins);
    ins = m_NominalToBinaryFilter.output();
    return m_root[1].predict(ins,!m_UseUnsmoothed);
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * Valid options are: <p>
   *
   * -O <l|r|m> <br>
   * Type of model to be used. (l: linear regression, 
   * r: regression tree, m: model tree) (default: m) <p>
   *
   * -U <br>
   * Use unsmoothed tree. <p>
   *
   * -F factor <br>
   * Set pruning factor (default: 2). <p>
   *
   * -V <0|1|2> <br>
   * Verbosity (default: 0). <p>
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tType of model to be used.\n"+
				    "\tl: linear regression\n"+
				    "\tr: regression tree\n"+
				    "\tm: model tree\n"+
				    "\t(default: m)",
				    "-O", 1, "-O <l|r|m>"));
    newVector.addElement(new Option("\tUse unsmoothed tree.", "C", 0, 
				    "-U"));
    newVector.addElement(new Option("\tPruning factor (default: 2).",
				    "-F", 1, "-F <double>"));
    newVector.addElement(new Option("\tVerbosity (default: 0).",
				    "-V", 1, "-V <0|1|2>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{

    String modelString = Utils.getOption('O', options);
    if (modelString.length() != 0) {
      if (modelString.equals("l"))
	setModelType(new SelectedTag(MODEL_LINEAR_REGRESSION,
				     TAGS_MODEL_TYPES));
      else if (modelString.equals("r"))
	setModelType(new SelectedTag(MODEL_REGRESSION_TREE,
				     TAGS_MODEL_TYPES));
      else if (modelString.equals("m"))
	setModelType(new SelectedTag(MODEL_MODEL_TREE,
				     TAGS_MODEL_TYPES));
      else
	throw new Exception("Don't know model type " + modelString);
    } else {
      setModelType(new SelectedTag(MODEL_MODEL_TREE,
				   TAGS_MODEL_TYPES));
    }
    
    setUseUnsmoothed(Utils.getFlag('U', options));
    if (m_Model != Node.MODEL_TREE) {
      setUseUnsmoothed(true);
    }

    String pruningString = Utils.getOption('F', options);
    if (pruningString.length() != 0) {
      setPruningFactor((new Double(pruningString)).doubleValue());
    } else {
      setPruningFactor(2);
    }
    
    String verbosityString = Utils.getOption('V', options);
    if (verbosityString.length() != 0) {
      setVerbosity(Integer.parseInt(verbosityString));
    } else {
      setVerbosity(0);
    }
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
    case MODEL_MODEL_TREE:
      options[current++] = "-O"; options[current++] = "m";
      if (m_UseUnsmoothed) {
	options[current++] = "-U";
      }
      break;
    case MODEL_REGRESSION_TREE:
      options[current++] = "-O"; options[current++] = "r";
      break;
    case MODEL_LINEAR_REGRESSION:
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
   * Converts the output of the training process into a string
   *
   * @return the converted string
   */
  public final String toString() {

    try{
      StringBuffer text = new StringBuffer();
      double absDev = M5Utils.absDev(m_root[0].instances.classIndex(),
				     m_root[0].instances);
      
      if(options.verbosity >= 1 && options.model != Node.LINEAR_REGRESSION){
	switch(m_root[0].model){
	case Node.LINEAR_REGRESSION: 
	  break;
	case Node.REGRESSION_TREE  : 
	  text.append("Unpruned training regression tree:\n"); break;
	case Node.MODEL_TREE       : 
	  text.append("Unpruned training model tree:\n"); break;
	}     
	if(m_root[0].type == false)text.append("\n");
	
	text.append(m_root[0].treeToString(0,absDev)+ "\n");
	text.append("Models at the leaves:\n\n");

	//    the linear models at the leaves of the unpruned tree
	text.append(m_root[0].formulaeToString(false) + "\n");;  
      }
      
      if(m_root[0].model != Node.LINEAR_REGRESSION){
	switch(m_root[0].model){
	case Node.LINEAR_REGRESSION: 
	  break;
	case Node.REGRESSION_TREE  : 
	  text.append("Pruned training regression tree:\n"); break;
	case Node.MODEL_TREE       : 
	  text.append("Pruned training model tree:\n"); break;
	}
	if(m_root[1].type == false)text.append("\n");
	text.append(m_root[1].treeToString(0,absDev) + "\n"); //the pruned tree
	text.append("Models at the leaves:\n\n");
	if ((m_root[0].model != Node.LINEAR_REGRESSION) &&
	    (m_UseUnsmoothed)) {
	  text.append("  Unsmoothed (simple):\n\n");

	  //     the unsmoothed linear models at the leaves of the pruned tree
	  text.append(m_root[1].formulaeToString(false) + "\n");
	}
	if ((m_root[0].model == Node.MODEL_TREE) &&
	    (!m_UseUnsmoothed)) {
	  text.append("  Smoothed (complex):\n\n");
	  text.append(m_root[1].formulaeToString(true) + "\n");
	  //   the smoothed linear models at the leaves of the pruned tree
	}
      }
      else {
	text.append("Training linear regression model:\n\n");
	text.append(m_root[1].unsmoothed.toString(m_root[1].instances,0) + "\n\n");
	//       print the linear regression model
      }
      
      text.append("Number of Rules : "+m_root[1].numberOfLinearModels());

      return text.toString();
    } catch (Exception e) {
      return "can't print m5' tree";
    }
  }

  /**
   * return the number of linear models
   * @return the number of linear models
   */
  public double measureNumLinearModels() {
    return m_root[1].numberOfLinearModels();
  }

  /**
   * return the number of leaves in the tree
   * @return the number leaves in the tree (same as # linear models &
   * # rules)
   */
  public double measureNumLeaves() {
    return measureNumLinearModels();
  }

  /**
   * return the number of rules
   * @return the number of rules (same as # linear models &
   * # leaves in the tree)
   */
  public double measureNumRules() {
    return measureNumLinearModels();
  }

  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(3);
    newVector.addElement("measureNumLinearModels");
    newVector.addElement("measureNumLeaves");
    newVector.addElement("measureNumRules");
    return newVector.elements();
  }

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception Exception if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) throws Exception {
    if (additionalMeasureName.compareTo("measureNumRules") == 0) {
      return measureNumRules();
    } else if (additionalMeasureName.compareTo("measureNumLinearModels") == 0){
      return measureNumLinearModels();
    } else if (additionalMeasureName.compareTo("measureNumLeaves") == 0) {
      return measureNumLeaves();
    } else {
      throw new Exception(additionalMeasureName 
			  + " not supported (M5)");
    }
  }
  
  /**
   * Get the value of UseUnsmoothed.
   *
   * @return Value of UseUnsmoothed.
   */
  public boolean getUseUnsmoothed() {
    
    return m_UseUnsmoothed;
  }
  
  /**
   * Set the value of UseUnsmoothed.
   *
   * @param v  Value to assign to UseUnsmoothed.
   */
  public void setUseUnsmoothed(boolean v) {
    
    m_UseUnsmoothed = v;
  }
  
  /**
   * Get the value of PruningFactor.
   *
   * @return Value of PruningFactor.
   */
  public double getPruningFactor() {
    
    return m_PruningFactor;
  }
  
  /**
   * Set the value of PruningFactor.
   *
   * @param v  Value to assign to PruningFactor.
   */
  public void setPruningFactor(double v) {
    
    m_PruningFactor = v;
  }
  
  /**
   * Get the value of Model.
   *
   * @return Value of Model.
   */
  public SelectedTag getModelType() {
    
    try {
      return new SelectedTag(m_Model, TAGS_MODEL_TYPES);
    } catch (Exception ex) {
      return null;
    }
  }
  
  /**
   * Set the value of Model.
   *
   * @param v  Value to assign to Model.
   */
  public void setModelType(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_MODEL_TYPES) {
      m_Model = newMethod.getSelectedTag().getID();
    }
  }
  
  /**
   * Get the value of Verbosity.
   *
   * @return Value of Verbosity.
   */
  public int getVerbosity() {
    
    return m_Verbosity;
  }
  
  /**
   * Set the value of Verbosity.
   *
   * @param v  Value to assign to Verbosity.
   */
  public void setVerbosity(int v) {
    
    m_Verbosity = v;
  }

  /**
   * Main method for M5' algorithm
   *
   * @param argv command line arguments
   */
  public static void  main(String [] argv){

    try {
      System.out.println(Evaluation.evaluateModel(new M5Prime(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}

     





