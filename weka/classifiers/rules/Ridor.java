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
 *    Ridor.java
 *    Copyright (C) 2001 Xin Xu
 *
 */

package weka.classifiers.rules;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * The implementation of a RIpple-DOwn Rule learner.
 *
 * It generates the default rule first and then the exceptions for the default rule
 * with the least (weighted) error rate.  Then it generates the "best" exceptions for
 * each exception and iterates until pure.  Thus it performs a tree-like expansion of
 * exceptions and the leaf has only default rule but no exceptions.
 *
 * The exceptions are a set of rules that predict the class other than class in default
 * rule.  IREP is used to find out the exceptions.
 *
 * @author: Xin XU (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */

public class Ridor extends Classifier implements OptionHandler, AdditionalMeasureProducer{
    /** The number of folds to split data into Grow and Prune for IREP */
    private int m_Folds = 3;

    /** The number of shuffles performed on the data for randomization */
    private int m_Shuffle = 10;

    /** The seed to perform randomization */
    private int m_Seed = 1;

    /** The root of Ridor */
    private Ridor_node m_Root = null;
    
    /** The class attribute of the data */
    private Attribute m_Class;
    
    /** 
     * Private class implementing the single node of Ridor. 
     * It consists of a default class label, a set of exceptions to the default rule
     * and the exceptions to each exception
     */
    private class Ridor_node{
	
	/** The default class label */
	private double defClass = Double.NaN;
	
	/** The set of exceptions of the default rule. 
	    Each element also has its own exceptions and the consequent of each rule 
	    is determined by its exceptions */
	private ConjunctiveRule[] rules = null;
	
	/** The exceptions of the exception rules */
	private Ridor_node[] excepts = null; 

	/** The level of this node */
	private int level;

	/** "Get" member functions */
	public double getDefClass() { return defClass; }
	public ConjunctiveRule[] getRules() { return rules; }
	public Ridor_node[] getExcepts() { return excepts; }

	/**
	 * Builds a ripple-down manner rule learner.
	 *
	 * @param dataByClass the divided data by their class label. The real class
	 * labels of the instances are all set to 0
	 * @param lvl the level of the parent node
	 * @exception Exception if ruleset of this node cannot be built
	 */
	public void findRules(Instances[] dataByClass, int lvl) throws Exception{
	    Vector finalRules = null;
	    double maxAcRt;
	    int clas = -1;
	    int[] isPure = new int[dataByClass.length];
	    int numMajority = 0;
    
	    level = lvl + 1;

	    for(int h=0; h < dataByClass.length; h++){
		isPure[h] = dataByClass[h].numInstances();
		if(isPure[h] >= m_Folds)
		    numMajority++;  // Count how many class labels have enough instances
	    }
	    
	    if(numMajority <= 1){	                   // The data is pure or not enough
		defClass = (double)Utils.maxIndex(isPure);
		return;
	    }
	    int total = Utils.sum(isPure);	 
	    maxAcRt = (double)isPure[Utils.maxIndex(isPure)] / ((double)total);

	findDefClass: 
	    for(int i=0; i < dataByClass.length; i++){
		if(isPure[i] >= m_Folds){
		    Instances data = new Instances(dataByClass[i]);
		    int index = data.classIndex();
		    
		    for(int j=0; j<data.numInstances(); j++)
			data.instance(j).setClassValue(1);       // Set one class as default
		    
		    for(int k=0; k < dataByClass.length; k++)    // Merge into one dataset
			if(k != i){
			    if(data.numInstances() >= dataByClass[k].numInstances())
				data = append(data, dataByClass[k]);
			    else data = append(dataByClass[k], data);
			}
		    
		    data.setClassIndex(index);           // Position new class label
		    
		    /* Build a set of rules */
		    Vector ruleset = new Vector();
		    int classCount = data.numInstances() - isPure[i];
		    double wAcRt = 0;  // The weighted accuracy rate of this ruleset

		    while( classCount >= m_Folds ){      // Data is not pure
			ConjunctiveRule bestRule = null;
			double bestWorthRate= -1;        // The best worth achieved by
			double bestWorth = -1;           // randomization of the data
			
			ConjunctiveRule rule = new ConjunctiveRule();                                
			rule.setFolds(m_Folds);
			rule.setPredictedClass(0);       // Predict the classes other than default
			Random ran = new Random(m_Seed);

			for(int j = 0; j < m_Shuffle; j++){
			    data.randomize(ran);
			    rule.buildClassifier(data);

			    double wr = rule.getWorthRate();
			    double w = rule.getWorth(); 
			   
			    if(Utils.gr(wr, bestWorthRate) ||
			       (Utils.eq(wr, bestWorthRate) && Utils.gr(w, bestWorth))){
				bestRule = rule;
				bestWorthRate = rule.getWorthRate();
				bestWorth = rule.getWorth();
			    }
			}
			
			if(Utils.sm(bestRule.getWorthRate(), 0.5) 
			   || (!bestRule.hasAntds()))
			    break;                       // No more good rules generated
			
			Instances newData = new Instances(data); 
			data = new Instances(newData, 0);// Empty the data
			classCount = 0;
			int cover = 0;                   // Coverage of this rule on whole data

			for(int l=0; l<newData.numInstances(); l++){
			    Instance datum = newData.instance(l);
			    if(!bestRule.isCover(datum)){    // Data not covered by the previous rule
				data.add(datum);
				if(Utils.eq(datum.classValue(), 0)) 
				    classCount ++;       // The predicted class in the data
			    }
			    else cover++;
			}

			wAcRt += computeWeightedAcRt(bestRule, (double)cover, (double)total);
			ruleset.addElement((Object)bestRule);			
		    }  
		    
		    double wDefAcRt = (double)(data.numInstances()-classCount)
			/ ((double)total);            // The weighted def. accuracy
		    wAcRt += wDefAcRt;

		    if(Utils.gr(wAcRt, maxAcRt)){
			finalRules = ruleset;
			maxAcRt = wAcRt;
			clas = i;
		    }
		}
	    }
		
	    /* No good rules found, set the majority class to be the default */
	    if(finalRules == null){
		defClass = (double)Utils.maxIndex(isPure);
		return;
	    }
		
	    /* Store the exception rules and default class in this node */
	    int size = finalRules.size();
	    rules = new ConjunctiveRule[size];
	    excepts = new Ridor_node[size];
	    for(int l=0; l < size; l++)
		rules[l] = (ConjunctiveRule)finalRules.elementAt(l);
		
	    defClass = (double)clas;
	    
	    /* Build exceptions for each exception rule */
	    Instances[] uncovered = dataByClass; 
	    for(int m=0; m < size; m++){
		/* The data covered by this rule, they are also deducted from the original data */
		Instances[][] dvdData = divide(rules[m], uncovered);
		Instances[] covered = dvdData[0];    // Data covered by the rule
		uncovered = dvdData[1];              // Data not covered by the rule
		excepts[m] = new Ridor_node();
		excepts[m].findRules(covered, level);// Find exceptions on the covered data
	    }
	}
	
	/**
	 * Private function to combine two data
	 *
	 * @param data1 the data to which data2 is appended 
	 * @param data2 the data to be appended to data1
	 * @return the merged data
	 */
	private Instances append(Instances data1, Instances data2){
	    Instances data = new Instances(data1);
	    for(int i=0; i<data2.numInstances(); i++)
		data.add(data2.instance(i));
	    
	    return data;
	}
	
	/**
	 * Compute the weighted average of accuracy rate of a certain rule
	 * Each rule is weighted by its coverage proportion in the whole data.  
	 * So the accuracy rate of one ruleset is actually 
	 * 
	 * (accuracy on the pruning data) * (coverage proportion)
	 *
	 *                               coverage of the rule on the whole data
	 * where coverage proportion = -----------------------------------------
	 *                              the whole data size fed into the ruleset
	 *
	 * @param rule the rule to be computed
	 * @param cover the coverage of the rule on the whole data
	 * @param total the total data size fed into the ruleset
	 * @return the weighted accuracy rate of this rule
	 */
	private double computeWeightedAcRt(ConjunctiveRule rule, double cover, double total){
	  
	    return (rule.getWorthRate() * (cover/total));	
	}
	
	/**
	 * Builds an array of data according to their true class label
	 * Each bag of data is filtered through the rule specified and
	 * is totally covered by this rule.  
	 * Both the data covered and uncovered by the rule will be returned
	 * by the procedure.  
	 *
	 * @param rule the rule covering the data
	 * @param dataByClass the array of data to be covered by the rule
	 * @return the arrays of data both covered and not covered by the rule
	 */
	private Instances[][] divide(ConjunctiveRule rule, Instances[] dataByClass){
	    int len = dataByClass.length;
	    Instances[][] dataBags = new Instances[2][len];
	    
	    for(int i=0; i < len; i++){
		Instances[] dvdData = rule.coveredByRule(dataByClass[i]);
		dataBags[0][i] = dvdData[0];     // Covered by the rule
		dataBags[1][i] = dvdData[1];     // Not covered by the rule
	    }
	    
	    return dataBags;
	}
	/**
	 * The size of the certain node of Ridor, i.e. the 
	 * number of rules generated within and below this node
	 *
	 * @return the size of this node
	 */
	public int size(){
	    int size = 0;
	    if(rules != null){
		for(int i=0; i < rules.length; i++)
		    size += excepts[i].size(); // The children's size
		size += rules.length;          // This node's size
	    }
	    return size;
	}
	
	/**
	 * Prints the all the rules of one node of Ridor.
	 *
	 * @return a textual description of one node of Ridor
	 */
	public String toString(){
	    StringBuffer text =  new StringBuffer();

	    text.append(m_Class.value((int)defClass)+"\n");
	    if(rules != null){
		for(int i=0; i < rules.length; i++){
		    for(int j=0; j < level; j++)
			text.append("         ");
		    text.append("  Except " + rules[i].toString(m_Class.name(), "")
				+ excepts[i].toString());
		}
	    }
	    
	    return text.toString();
	}
    }
    
    /**
     * Builds a ripple-down manner rule learner.
     *
     * @param data the training data
     * @exception Exception if classifier can't be built successfully
     */
    public void buildClassifier(Instances instances) throws Exception{
	Instances data = new Instances(instances);
	int numCl = data.numClasses();
	m_Root = new Ridor_node();
	m_Class = instances.classAttribute();     // The original class label
	int index = data.classIndex();
	
	/** Create a binary attribute*/
	FastVector binary_values = new FastVector(2);
	binary_values.addElement("otherClasses");
	binary_values.addElement("defClass");
	Attribute attr = new Attribute ("newClass", binary_values);
	data.insertAttributeAt(attr, index);	
	data.setClassIndex(index);                 // The new class label

	/** Partition the data into bags according to their original class values */
	Instances[] dataByClass = new Instances[numCl];
	for(int i=0; i < numCl; i++)
	    dataByClass[i] = new Instances(data, data.numInstances()); // Empty bags
	for(int i=0; i < data.numInstances(); i++){ // Partitioning
	    Instance inst = data.instance(i);
	    inst.setClassValue(0);           // Set new class vaue to be 0
	    inst.setWeight(1);
	    dataByClass[(int)inst.value(index+1)].add(inst); 
	}	
	
	for(int i=0; i < numCl; i++)    
	    dataByClass[i].deleteAttributeAt(index+1);   // Delete original class
	
	m_Root.findRules(dataByClass, 0);
    
    }
    
    /**
     * Classify the test instance with the rule learner 
     *
     * @param instance the instance to be classified
     * @return the classification
     */
    public double classifyInstance(Instance datum){
	return classify(m_Root, datum);
    }
    
    /**
     * Classify the test instance with one node of Ridor 
     *
     * @param node the node of Ridor to classify the test instance
     * @param instance the instance to be classified
     * @return the classification
     */
    private double classify(Ridor_node node, Instance datum){
	double classValue = node.getDefClass();
	ConjunctiveRule[] rules = node.getRules();

	if(rules != null){
	    Ridor_node[] excepts = node.getExcepts();	
	    for(int i=0; i < excepts.length; i++){
		if(rules[i].isCover(datum)){
		    classValue = classify(excepts[i], datum);
		    break;
		}
	    }
	}
	
	return classValue;
    }

    /**
     * Returns an enumeration describing the available options
     * Valid options are: <p>
     *
     * -F number <br>
     * Set number of folds for reduced error pruning. One fold is
     * used as the pruning set. (Default: 3) <p>
     *
     * -S number <br>
     * Set number of shuffles for randomization. (Default: 10) <p>
     * 
     * @return an enumeration of all the available options
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(2);

	newVector.addElement(new Option("\tSet number of folds for IREP\n" +
					"\tOne fold is used as pruning set.\n" +
					"\t(default 3)","F", 1, "-F <number of folds>"));
	newVector.addElement(new Option("\tSet number of shuffles to randomize\n" +
					"\tthe data in order to get better rule.\n" +
					"\t(default 10)","S", 1, "-S <number of shuffles>"));

	return newVector.elements();
    }
    
    /**
     * Parses a given list of options.
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception{
	
	String numFoldsString = Utils.getOption('F', options);
	if (numFoldsString.length() != 0) 
	    m_Folds = Integer.parseInt(numFoldsString);
	else 
	    m_Folds = 3;

	String numShuffleString = Utils.getOption('S', options);
	if (numShuffleString.length() != 0) 
	    m_Shuffle = Integer.parseInt(numShuffleString);
	else 
	    m_Shuffle = 10;

	String seedString = Utils.getOption('s', options);
	if (seedString.length() != 0) 
	    m_Seed = Integer.parseInt(seedString);
	else 
	    m_Seed = 1;

    }
    
    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String [] getOptions() {
	
	String [] options = new String [4];
	int current = 0;
	options[current++] = "-F"; options[current++] = "" + m_Folds;
	options[current++] = "-S"; options[current++] = "" + m_Shuffle;
	while (current < options.length) 
	    options[current++] = "";
	return options;
    }

    /** Set and get members for parameters */
    public void setFolds(int fold){ m_Folds = fold; }
    public int getFolds(){ return m_Folds; }
    public void setShuffle(int sh){ m_Shuffle = sh; }
    public int getShuffle(){ return m_Shuffle; }
    public void setSeed(int s){ m_Seed = s; }
    public int getSeed(){ return m_Seed; }

    /**
     * Returns an enumeration of the additional measure names
     * @return an enumeration of the measure names
     */
    public Enumeration enumerateMeasures() {
	Vector newVector = new Vector(1);
	newVector.addElement("measureNumRules");
	return newVector.elements();
    }
    
    /**
     * Returns the value of the named measure
     * @param measureName the name of the measure to query for its value
     * @return the value of the named measure
     * @exception IllegalArgumentException if the named measure is not supported
     */
    public double getMeasure(String additionalMeasureName) {
	if (additionalMeasureName.compareTo("measureNumRules") == 0) 
	    return numRules();
	else 
	    throw new IllegalArgumentException(additionalMeasureName+" not supported (Ripple down rule learner)");
    }  
    
    /**
     * Measure the number of rules in total in the model
     *
     * @return the number of rules
     */  
    private double numRules(){
	int size = 0;
	if(m_Root != null)
	    size = m_Root.size();
	
	return (double)(size+1);
    }
   
    /**
     * Prints the all the rules of the rule learner.
     *
     * @return a textual description of the classifier
     */
    public String toString() {
	if (m_Root == null) 
	    return "RIpple DOwn Rule Learner(Ridor): No model built yet.";
	
	return ("RIpple DOwn Rule Learner(Ridor) rules\n"+
		"--------------------------------------\n\n" + 
		m_Root.toString() +
		"\nTotal number of rules (incl. the default rule): " + (int)numRules());
    }
    
    /**
     * Main method.
     *
     * @param args the options for the classifier
     */
    public static void main(String[] args) {	
	try {
	    System.out.println(Evaluation.evaluateModel(new Ridor(), args));
	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println(e.getMessage());
	}
    }
}
