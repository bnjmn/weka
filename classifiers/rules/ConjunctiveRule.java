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
 *    ConjunctiveRule.java
 *    Copyright (C) 2001 Xin Xu
 *
 */

package weka.classifiers.rules;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * This class implements a single rule that predicts the 2-class distribution.  
 *
 * A rule consists of antecedents "AND"ed together and the consequent (class value) 
 * for the classification.  In this case, the consequent is the distribution of
 * the available classes (always 2 classes) in the dataset.  

 * In this class, the Information Gain (p*[log(p/t) - log(P/T)]) is used to select 
 * an antecedent and Reduced Error Prunning (REP) is used to prune the rule. 
 *
 * @author: Xin XU (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */

public class ConjunctiveRule extends DistributionClassifier implements OptionHandler, WeightedInstancesHandler{
    
    /** The internal representation of the class label to be predicted*/
    private double m_Class = -1;
    
    /** The number of folds to split data into Grow and Prune for REP*/
    private int m_Folds = 3;

    /** The class attribute of the data*/
    private Attribute m_ClassAttribute;
    
    /** The vector of antecedents of this rule*/
    protected FastVector m_Antds = null;
    
    /** The default distribution of the training data*/
    protected double[] m_DefDstr = null;
    
    /** The consequent of this rule, i.e. the classification*/
    protected double[] m_Cnsqt = null;
    
    /** The worth rate of this rule, in this case, accuracy rate in the pruning data*/
    private double m_WorthRate = 0;
    
    /** The worth value of this rule, in this case, accurate # in pruning data*/
    private double m_Worth = 0;
   
    /**
     * Returns an enumeration describing the available options
     * Valid options are: <p>
     *
     * -N number <br>
     * Set number of folds for REP. One fold is
     * used as the pruning set. (Default: 3) <p>
     *
     * -P predicted_class <br>
     * Set the class label to be predicted.  If not set, it always predicts 
     * the minority of the two classes. (Default: -1)<p> 
     * 
     * @return an enumeration of all the available options
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(2);
	
	newVector.addElement(new Option("\tSet number of folds for REP\n" +
					"\tOne fold is used as pruning set.\n" +
					"\t(default 3)","N", 1, "-N <number of folds>"));
	newVector.addElement(new Option("\tSet the class label to be predicted.\n" +
					"\tIf not set, it always predicts the minority\n" +
					"\tof the two classes.(Default: -1)",
					"P", 1, "-P <predicted_class>"));
	return newVector.elements();
    }
    
    /**
     * Parses a given list of options.
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception{
	
	String numFoldsString = Utils.getOption('N', options);
	if (numFoldsString.length() != 0) 
	    m_Folds = Integer.parseInt(numFoldsString);
	else 
	    m_Folds = 3;

	String classString = Utils.getOption('P', options);
	if (classString.length() != 0) 
	    m_Class = Double.parseDouble(classString);
	else 
	    m_Class = -1.0;
    }
    
    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String [] getOptions() {
	
	String [] options = new String [4];
	int current = 0;
	options[current++] = "-N"; options[current++] = "" + m_Folds;
	options[current++] = "-P"; options[current++] = "" + m_Class;

	while (current < options.length) 
	    options[current++] = "";
	return options;
    }

    /** The access functions for parameters */
    public void setFolds(int folds){  m_Folds = folds; }
    public int getFolds(){ return m_Folds; }
    public void setPredictedClass(double cl){  m_Class = cl; }
    public double getPredictedClass(){ return m_Class; }

    /**
     * Builds a single rule learner with REP dealing with 2 classes.
     * This rule learner always tries to predict the class with label 
     * m_Class.
     *
     * @param instances the training data
     * @exception Exception if classifier can't be built successfully
     */
    public void buildClassifier(Instances instances) throws Exception{
	m_ClassAttribute = instances.classAttribute();
	if (!m_ClassAttribute.isNominal()) 
	    throw new Exception(" Only nominal class, please.");
	if(instances.numClasses() != 2)
	    throw new Exception(" Only 2 classes, please.");

	Instances data = new Instances(instances);
	data.deleteWithMissingClass();
	
	if(data.numInstances() < m_Folds)
	    throw new Exception(" Not enough data for REP.");

	m_Antds = new FastVector();
	m_DefDstr = new double[2];
	m_Cnsqt = new double[2];
	
	for(int i=0; i<2; i++){
	    m_DefDstr[i] = 0;
	    m_Cnsqt[i] = 0;
	}
	
	/* Calculate the default distribution of the data */	
	for(int j=0; j<data.numInstances(); j++){
	    Instance inst = data.instance(j);
	    m_DefDstr[(int)inst.classValue()] += inst.weight();
	}	
	
	if(Utils.eq(m_Class, -1.0)){
	    m_Class = (double)Utils.minIndex(m_DefDstr);   
	}

	/* The minority class has no weights.  Cannot derive rule for minority class */
	if(Utils.eq(m_DefDstr[(int)m_Class],0)){
	    m_Cnsqt[(int)m_Class] = 0;
	    m_Cnsqt[1-(int)m_Class] = 1;
	    return;
	}
	
	/* Split data into Grow and Prune*/
	data.stratify(m_Folds);
	Instances growData=data.trainCV(m_Folds, m_Folds-1);
	Instances pruneData=data.testCV(m_Folds, m_Folds-1);
	
	grow(growData);      // Build this rule
		    
	prune(pruneData);    // Prune this rule

	/* Compute the Consequent (i.e. the distribution) on the whole data*/
	Instances coveredData = coveredByRule(data)[0];
	for(int k=0; k<coveredData.numInstances(); k++){
	    Instance inst = coveredData.instance(k);
	    m_Cnsqt[(int)inst.classValue()] += inst.weight();
	}
    }
    
    /**
     * Computes class distribution for instance of the two classes.
     *
     * @param instance the instance for which distribution is to be computed
     * @return the class distribution for the given instance
     */
    public double[] distributionForInstance(Instance instance) throws Exception{
	if (isCover(instance)){
	    Utils.normalize(m_Cnsqt);
	    return m_Cnsqt;
	}
	else{
	    double[] dist = new double[2];
	    for(int i=0; i<2; i++)
		dist[i] = m_DefDstr[i] - m_Cnsqt[i];
	    Utils.normalize(dist);
	    return dist;
	}
    }

    /**
     * Find all the instances in the dataset covered by this rule.
     * The instances not covered will also be deducted from the the original data
     * and returned by this procedure.
     * 
     * @param insts the dataset to be covered by this rule.
     * @return the instances covered and not covered by this rule
     */
    public Instances[] coveredByRule(Instances insts){
	Instances[] data = new Instances[2];
	data[0] = new Instances(insts, insts.numInstances());
	data[1] = new Instances(insts, insts.numInstances());
	
	for(int i=0; i<insts.numInstances(); i++){
	    Instance datum = insts.instance(i);
	    if(isCover(datum))
		data[0].add(datum);        // Covered by this rule
	    else
		data[1].add(datum);        // Not covered by this rule
	}

	return data;
    }
    
    /**
     * Whether the instance covered by this rule
     * 
     * @param inst the instance in question
     * @return the boolean value indicating whether the instance is covered by this rule
     */
    public boolean isCover(Instance datum){
	boolean isCover=true;

	for(int i=0; i<m_Antds.size(); i++){
	    Antd antd = (Antd)m_Antds.elementAt(i);
	    if(!antd.isCover(datum)){
		isCover = false;
		break;
	    }
	}
	
	return isCover;
    }        
    
    /**
     * Whether this rule has antecedents, i.e. whether it is a default rule
     * 
     * @return the boolean value indicating whether the rule has antecedents
     */
    public boolean hasAntds(){
	if (m_Antds == null)
	    return false;
	else
	    return (m_Antds.size() > 0);
    }      

    /**
     * Build one rule using the growing data
     *
     * @param data the growing data used to build the rule
     */    
    private void grow(Instances data){
	Instances growData = new Instances(data);
	
	/* Compute the default accurate rate of the growing data */
	double defAcRt= computeDefAccu(growData) / growData.sumOfWeights();
	
	/* Keep the record of which attributes have already been used*/    
	boolean[] used=new boolean [growData.numAttributes()];
	for (int k=0; k<used.length; k++)
	    used[k]=false;
	int numUnused=used.length;
	
	double maxInfoGain;
	boolean isContinue = true; // The stopping criterion of this rule
	
	while (isContinue){   
	    maxInfoGain = 0;       // We require that infoGain be positive
	    
	    /* Build a list of antecedents */
	    Antd oneAntd=null;
	    Instances coverData = null;
	    Enumeration enumAttr=growData.enumerateAttributes();	    
	    int index=-1;  
	    
	    /* Build one condition based on all attributes not used yet*/
	    while (enumAttr.hasMoreElements()){
		Attribute att= (Attribute)(enumAttr.nextElement());
		index++;
		
		Antd antd =null;	
		if(att.isNumeric())
		    antd = new NumericAntd(att);
		else
		    antd = new NominalAntd(att);
		
		if(!used[index]){
		    /* Compute the best information gain for each attribute,
		       it's stored in the antecedent formed by this attribute.
		       This procedure returns the data covered by the antecedent*/
		    Instances coveredData = computeInfoGain(growData, defAcRt, antd);
		    double infoGain = antd.getMaxInfoGain();
		
		    if(Utils.gr(infoGain, maxInfoGain)){
			oneAntd=antd;
			coverData = coveredData;  
			maxInfoGain = infoGain;		    
		    }
		}
	    }
	    
	    if(oneAntd == null)	 return;
	    
	    //Numeric attributes can be used more than once
	    if(!oneAntd.getAttr().isNumeric()){ 
		used[oneAntd.getAttr().index()]=true;
		numUnused--;
	    }

	    m_Antds.addElement((Object)oneAntd);
	    growData = coverData;// Grow data size is shrinking 
	    
	    defAcRt = oneAntd.getAccuRate();
	    
	    /* Stop if no more data, rule perfect, no more attributes */
	    if(Utils.eq(growData.sumOfWeights(), 0.0) || Utils.eq(defAcRt, 1.0) || (numUnused == 0))
		isContinue = false;
	}
    }
    
    /** 
     * Compute the best information gain for the specified antecedent
     *  
     * @param data the data based on which the infoGain is computed
     * @param defAcRt the default accuracy rate of data
     * @param antd the specific antecedent
     * @return the data covered by the antecedent
     */
    private Instances computeInfoGain(Instances instances, double defAcRt, Antd antd){
	Instances data = new Instances(instances);
	
	/* Split the data into bags.
	   The information gain of each bag is also calculated in this procedure */
	Instances[] splitData = antd.splitData(data, defAcRt, m_Class); 
	
	/* Get the bag of data to be used for next antecedents */
	if(splitData != null)
	    return splitData[(int)antd.getAttrValue()];
	else return null;
    }
    
    /**
     * Prune the rule using the pruning data and update the worth parameters for this rule
     * The accuracy rate is used to prune the rule.
     *
     * @param pruneData the pruning data used to prune the rule
     */    
    private void prune(Instances pruneData){
	Instances data=new Instances(pruneData);

	double total = data.sumOfWeights();

	/* The default accurate# and the the accuracy rate on pruning data */
	double defAccu=0, defAccuRate=0;
	
	int size=m_Antds.size();
	if(size == 0) return; // Default rule before pruning
	
	double[] worthRt = new double[size];
	double[] coverage = new double[size];
	double[] worthValue = new double[size];
	for(int w=0; w<size; w++){
	    worthRt[w]=coverage[w]=worthValue[w]=0.0;
	}
	
	/* Calculate accuracy parameters for all the antecedents in this rule */
	for(int x=0; x<size; x++){
	    Antd antd=(Antd)m_Antds.elementAt(x);
	    Attribute attr= antd.getAttr();
	    Instances newData = new Instances(data);
	    data = new Instances(newData, newData.numInstances()); // Make data empty
	    
	    for(int y=0; y<newData.numInstances(); y++){
		Instance ins=newData.instance(y);
		if(!ins.isMissing(attr)){              // Attribute not missing
		    if(antd.isCover(ins)){             // Covered by this antecedent
			coverage[x] += ins.weight();
			data.add(ins);                 // Add to data for further pruning
			if(Utils.eq(ins.classValue(), m_Class)) // Accurate prediction
			    worthValue[x] += ins.weight();
		    }
		}
	    }
	    
	    if(coverage[x] != 0)  
		worthRt[x] = worthValue[x]/coverage[x];
	}
	
	/* Prune the antecedents according to the accuracy parameters */
	for(int z=(size-1); z > 0; z--)
	    if(Utils.sm(worthRt[z], worthRt[z-1]))
		m_Antds.removeElementAt(z);
	    else  break;
	
	/* Check whether this rule is a default rule */
	if(m_Antds.size() == 1){
	    defAccu = computeDefAccu(pruneData);
	    defAccuRate = defAccu/total;                // Compute def. accuracy
	    if(Utils.sm(worthRt[0], defAccuRate)){      // Becomes a default rule
		m_Antds.removeAllElements();
	    }
	}   
	
	/* Update the worth parameters of this rule*/
	int antdsSize = m_Antds.size();
	if(antdsSize != 0){                          // Not a default rule
	    m_Worth = worthValue[antdsSize-1];       // WorthValues of the last antecedent
	    m_WorthRate = worthRt[antdsSize-1];
	}
	else{                                        // Default rule    
	    m_Worth = defAccu;                       // Default WorthValues
	    m_WorthRate = defAccuRate;
	}
    }

    /**
     * Private function to compute default number of accurate instances
     * in the specified data for m_Class
     * 
     * @param data the data in question
     * @return the default accuracy number
     */
    private double computeDefAccu(Instances data){ 
      	double defAccu=0;
	for(int i=0; i<data.numInstances(); i++){
	    Instance inst = data.instance(i);
	    if(Utils.eq(inst.classValue(), m_Class))
		defAccu += inst.weight();
	}
	return defAccu;
    }
    
    /** The following are get functions after prune() has set the value of worthRate and worth*/
    public double getWorthRate(){ return m_WorthRate; }
    public double getWorth(){ return m_Worth; }
    
    /**
     * Prints this rule with the specified class label
     *
     * @param att the string standing for attribute in the consequent of this rule
     * @param cl the string standing for value in the consequent of this rule
     * @return a textual description of this rule with the specified class label
     */
    public String toString(String att, String cl) {
	StringBuffer text =  new StringBuffer();
	if(m_Antds.size() > 0){
	    for(int j=0; j< (m_Antds.size()-1); j++)
		text.append("(" + ((Antd)(m_Antds.elementAt(j))).toString()+ ") and ");
	    text.append("("+((Antd)(m_Antds.lastElement())).toString() + ")");
	}
	text.append(" => " + att + " = " 
		    + cl);
	return text.toString();
    }

    /**
     * Prints this rule
     *
     * @return a textual description of this rule
     */
    public String toString() {
	return toString(m_ClassAttribute.name(), m_ClassAttribute.value((int)m_Class));
    }
    
    /**
     * Main method.
     *
     * @param args the options for the classifier
     */
    public static void main(String[] args) {	
	try {
	    System.out.println(Evaluation.evaluateModel(new ConjunctiveRule(), args));
	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println(e.getMessage());
	}
    }
}

/** 
 * The single antecedent in the rule, which is composed of an attribute and 
 * the corresponding value.  There are two inherited classes, namely NumericAntd
 * and NominalAntd in which the attributes are numeric and nominal respectively.
 */

abstract class Antd{
    /* The attribute of the antecedent */
    protected Attribute att;
    
    /* The attribute value of the antecedent.  
       For numeric attribute, value is either 0(1st bag) or 1(2nd bag) */
    protected double value; 

    /* The maximum infoGain achieved by this antecedent test */
    protected double maxInfoGain;

    /* The accurate rate of this antecedent test on the growing data */
    protected double accuRate;

    /* Constructor*/
    public Antd(Attribute a){
	att=a;
	value=Double.NaN; 
	maxInfoGain = 0;
	accuRate = Double.NaN;
    }
    
    /* The abstract members for inheritance */
    public abstract Instances[] splitData(Instances data, double defAcRt, double cla);
    public abstract boolean isCover(Instance inst);
    public abstract String toString();
 
    /* Get functions of this antecedent */
    public Attribute getAttr(){ return att; }
    public double getAttrValue(){ return value; }
    public double getMaxInfoGain(){ return maxInfoGain; }
    public double getAccuRate(){ return accuRate; } 
}

/** 
 * The antecedent with numeric attribute
 */
class NumericAntd extends Antd{

    /* The split point for this numeric antecedent */
    private double splitPoint;
    
    /* Constructor*/
    public NumericAntd(Attribute a){ 
	super(a);
	splitPoint = Double.NaN;
    }    

    /* Get split point of this numeric antecedent */
    public double getSplitPoint(){ return splitPoint; }

    /**
     * Implements the splitData function.  
     * This procedure is to split the data into two bags according 
     * to the information gain of the numeric attribute value
     * The maximum infoGain is also calculated.  
     * 
     * @param insts the data to be split
     * @param defAcRt the default accuracy rate for data
     * @param cl the class label to be predicted
     * @return the array of data after split
     */
    public Instances[] splitData(Instances insts, double defAcRt, double cl){
	Instances data = new Instances(insts);
	data.sort(att);
	int total=data.numInstances();// Total number of instances without 
	                              // missing value for att

	int split=1;                  // Current split position
	int prev=0;                   // Previous split position
	int finalSplit=split;         // Final split position
	maxInfoGain = 0;
	value = 0;	
	
	double fstCover=0, sndCover=0, fstAccu=0, sndAccu=0;
	
	for(int x=0; x<data.numInstances(); x++){
	    Instance inst = data.instance(x);
	    if(inst.isMissing(att)){
		total = x;
		break;
	    }

	    if(x < split){
		fstCover += inst.weight();
		if(Utils.eq(inst.classValue(), cl))
		    fstAccu += inst.weight();
	    }  
	    else{
		sndCover += inst.weight();
		if(Utils.eq(inst.classValue(), cl))
		    sndAccu += inst.weight();
	    }
	}
	
	if(total == 0) return null; // Data all missing for the attribute 	
	splitPoint = data.instance(total-1).value(att);	

	for(; split < total; split++){
	    if(!Utils.eq(data.instance(split).value(att), 
			 data.instance(prev).value(att))){ // Can't split within same value
		
		for(int y=prev; y<split; y++){
		    Instance inst = data.instance(y);
		    fstCover += inst.weight(); sndCover -= inst.weight();
		    if(Utils.eq(data.instance(y).classValue(), cl)){
			fstAccu += inst.weight();  // First bag positive# ++
			sndAccu -= inst.weight();  // Second bag positive# --
		    }
		}
		
		double fstAccuRate = 0, sndAccuRate = 0;
		if(!Utils.eq(fstCover,0))
		    fstAccuRate = fstAccu/fstCover;
		if(!Utils.eq(sndCover,0))
		    sndAccuRate = sndAccu/sndCover;
		
		boolean isFirst; 
		
		/* Which bag has higher accuracy? */
		if(Utils.eq(fstAccuRate, sndAccuRate))
		    isFirst = (Utils.grOrEq(fstAccu,sndAccu)) ? true : false;
		else
		    isFirst = (Utils.gr(fstAccuRate,sndAccuRate)) ? true : false;
		
		double accRate= ((isFirst) ? fstAccuRate : sndAccuRate);
		double accu = ((isFirst) ? fstAccu : sndAccu);
		
		/* Compute infoGain according to the higher accuracy*/
		double infoGain = Utils.eq(accRate, 0) ? 
		    0 : (accu*(Utils.log2(accRate) - Utils.log2(defAcRt)));
	
		/* Check whether so far the max infoGain */
		if(Utils.gr(infoGain, maxInfoGain)){
		    splitPoint = (data.instance(split).value(att) + 
				  data.instance(prev).value(att))/2;
		    value = ((isFirst) ? 0 : 1);
		    accuRate = accRate;
		    maxInfoGain = infoGain;
		    finalSplit = split;
		}
		prev=split;
	    }
	}

	/* Split the data */
	Instances[] splitData = new Instances[2];
	splitData[0] = new Instances(data, 0, finalSplit);
	splitData[1] = new Instances(data, finalSplit, total-finalSplit);

	return splitData;
    }

    /**
     * Whether the instance is covered by this antecedent
     * 
     * @param inst the instance in question
     * @return the boolean value indicating whether the instance is covered 
     *         by this antecedent
     */
    public boolean isCover(Instance inst){
	boolean isCover=false;
	if(!inst.isMissing(att)){
	    if(Utils.eq(value, 0)){
		if(Utils.smOrEq(inst.value(att), splitPoint))
		    isCover=true;
	    }
	    else if(Utils.gr(inst.value(att), splitPoint))
		isCover=true;
	}
	return isCover;
    }
    
    /**
     * Prints this antecedent
     *
     * @return a textual description of this antecedent
     */
    public String toString() {
	String symbol = Utils.eq(value, 0.0) ? " <= " : " > ";
	return (att.name() + symbol + splitPoint);
    }   
}


/** 
 * The antecedent with nominal attribute
 */
class NominalAntd extends Antd{
   
    /* The parameters of infoGain calculated for each attribute value */
    private double[] accu;
    private double[] coverage;
    private double[] infoGain;

    /* Constructor*/
    public NominalAntd(Attribute a){ 
	super(a);
	int bag = att.numValues();
	accu = new double[bag];
	coverage = new double[bag];
	infoGain = new double[bag];
    }   
    
    /**
     * Implements the splitData function.  
     * This procedure is to split the data into bags according 
     * to the nominal attribute value
     * The infoGain for each bag is also calculated.  
     * 
     * @param data the data to be split
     * @param defAcRt the default accuracy rate for data
     * @param cl the class label to be predicted
     * @return the array of data after split
     */
    public Instances[] splitData(Instances data, double defAcRt, double cl){
	int bag = att.numValues();
	Instances[] splitData = new Instances[bag];
	
	for(int x=0; x<bag; x++){
	    accu[x] = coverage[x] = infoGain[x] = 0;
	    splitData[x] = new Instances(data, data.numInstances());
	}
	
	for(int x=0; x<data.numInstances(); x++){
	    Instance inst=data.instance(x);
	    if(!inst.isMissing(att)){
		int v = (int)inst.value(att);
		splitData[v].add(inst);
		coverage[v] += inst.weight();
		if(Utils.eq(inst.classValue(), cl))
		    accu[v] += inst.weight();
	    }
	}
	
	for(int x=0; x<bag; x++){
	    double p = accu[x];
	    double t = coverage[x];
	    if(!Utils.eq(t, 0.0))
		infoGain[x] = p *((Utils.log2(p/t)) - (Utils.log2(defAcRt)));
	}
	value = (double)Utils.maxIndex(infoGain);
	return splitData;
    }
    
    /**
     * This function overrides the corresponding member of super class
     * 
     * @return maximum infoGain achieved by this nominal antecedent test
     */
    public double getMaxInfoGain(){ 
	maxInfoGain = infoGain [(int)value];
	return maxInfoGain;
    } 
    
    /**
     * This function overrides the corresponding member of super class
     * 
     * @return accuracy rate of this nominal antecedent test
     */
    public double getAccuRate(){ 
	if(!Utils.eq(coverage[(int)value],0))
	    accuRate = accu [(int)value] / coverage[(int)value];
	else accuRate = 0;
	return accuRate;
    } 


    /**
     * Whether the instance is covered by this antecedent
     * 
     * @param inst the instance in question
     * @return the boolean value indicating whether the instance is covered 
     *         by this antecedent
     */
    public boolean isCover(Instance inst){
	boolean isCover=false;
	if(!inst.isMissing(att)){
	    if(Utils.eq(inst.value(att), value))
		isCover=true;	    
	}
	return isCover;
    }
    
    /**
     * Prints this antecedent
     *
     * @return a textual description of this antecedent
     */
    public String toString() {
	return (att.name() + " = " +att.value((int)value));
    } 
}
