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

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.UnsupportedClassTypeException;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * The implementation of a RIpple-DOwn Rule learner.<br/>
 * <br/>
 * It generates a default rule first and then the exceptions for the default rule with the least (weighted) error rate.  Then it generates the "best" exceptions for each exception and iterates until pure.  Thus it performs a tree-like expansion of exceptions.The exceptions are a set of rules that predict classes other than the default. IREP is used to generate the exceptions.
 * <p/>
 <!-- globalinfo-end -->
 * 
 * There are five inner classes defined in this class. <br>
 * The first is Ridor_node, which implements one node in the Ridor tree.  It's basically
 * composed of a default class and a set of exception rules to the default class.<br>
 * The second inner class is RidorRule, which implements a single exception rule 
 * using REP.<br>
 * The last three inner classes are only used in RidorRule.  They are Antd, NumericAntd 
 * and NominalAntd, which all implement a single antecedent in the RidorRule. <br>
 * The Antd class is an abstract class, which has two subclasses, NumericAntd and 
 * NominalAntd, to implement the corresponding abstract functions.  These two subclasses
 * implement the functions related to a antecedent with a nominal attribute and a numeric 
 * attribute respectively.<p>
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F &lt;number of folds&gt;
 *  Set number of folds for IREP
 *  One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -S &lt;number of shuffles&gt;
 *  Set number of shuffles to randomize
 *  the data in order to get better rule.
 *  (default 10)</pre>
 * 
 * <pre> -A
 *  Set flag of whether use the error rate 
 *  of all the data to select the default class
 *  in each step. If not set, the learner will only use the error rate in the pruning data</pre>
 * 
 * <pre> -M
 *   Set flag of whether use the majority class as
 *  the default class in each step instead of 
 *  choosing default class based on the error rate
 *  (if the flag is not set)</pre>
 * 
 * <pre> -N &lt;min. weights&gt;
 *  Set the minimal weights of instances
 *  within a split.
 *  (default 2.0)</pre>
 * 
 <!-- options-end -->
 *
 * @author Xin XU (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.14 $ 
 */

public class Ridor 
  extends Classifier
  implements OptionHandler, AdditionalMeasureProducer, WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = -7261533075088314436L;
  
  /** The number of folds to split data into Grow and Prune for IREP */
  private int m_Folds = 3;
    
  /** The number of shuffles performed on the data for randomization */
  private int m_Shuffle = 1;

  /** Random object for randomization */
  private Random m_Random = null;
    
  /** The seed to perform randomization */
  private int m_Seed = 1;

  /** Whether use error rate on all the data */
  private boolean m_IsAllErr = false;

  /** Whether use majority class as default class */
  private boolean m_IsMajority = false;
    
  /** The root of Ridor */
  private Ridor_node m_Root = null;
    
  /** The class attribute of the data */
  private Attribute m_Class;

  /** Statistics of the data */
  private double m_Cover, m_Err;

  /** The minimal number of instance weights within a split*/
  private double m_MinNo = 2.0;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "The implementation of a RIpple-DOwn Rule learner.\n\n" 
      + "It generates a default rule first and then the exceptions for the default rule "
      + "with the least (weighted) error rate.  Then it generates the \"best\" exceptions for "
      + "each exception and iterates until pure.  Thus it performs a tree-like expansion of "
      + "exceptions."
      + "The exceptions are a set of rules that predict classes other than the default. "
      + "IREP is used to generate the exceptions.";
  }
    
  /** 
   * Private class implementing the single node of Ridor. 
   * It consists of a default class label, a set of exceptions to the default rule
   * and the exceptions to each exception
   */
  private class Ridor_node 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = -581370560157467677L;
	
    /** The default class label */
    private double defClass = Double.NaN;
	
    /** The set of exceptions of the default rule. 
	Each element also has its own exceptions and the consequent of each rule 
	is determined by its exceptions */
    private RidorRule[] rules = null;
	
    /** The exceptions of the exception rules */
    private Ridor_node[] excepts = null; 

    /** The level of this node */
    private int level;

    /**
     * Gets the default class label
     *
     * @return the default class label
     */
    public double getDefClass() { 
      return defClass; 
    }
    
    /**
     * Gets the set of exceptions
     * 
     * @return the set of exceptions
     */
    public RidorRule[] getRules() { 
      return rules; 
    }
    
    /**
     * Gets the exceptions of the exceptions rules
     * 
     * @return the exceptions of the exceptions rules
     */
    public Ridor_node[] getExcepts() { 
      return excepts; 
    }

    /**
     * Builds a ripple-down manner rule learner.
     *
     * @param dataByClass the divided data by their class label. The real class
     * labels of the instances are all set to 0
     * @param lvl the level of the parent node
     * @throws Exception if ruleset of this node cannot be built
     */
    public void findRules(Instances[] dataByClass, int lvl) throws Exception {
      Vector finalRules = null;
      int clas = -1;
      double[] isPure = new double[dataByClass.length];
      int numMajority = 0;
	    
      level = lvl + 1;
	    
      for(int h=0; h < dataByClass.length; h++){
	isPure[h] = dataByClass[h].sumOfWeights();
	if(Utils.grOrEq(isPure[h], m_Folds))
	  numMajority++;  // Count how many class labels have enough instances
      }
	    
      if(numMajority <= 1){	                   // The data is pure or not enough
	defClass = (double)Utils.maxIndex(isPure);
	return;
      }
      double total = Utils.sum(isPure);	 
	    
      if(m_IsMajority){
	defClass = (double)Utils.maxIndex(isPure);
	Instances data = new Instances(dataByClass[(int)defClass]);
	int index = data.classIndex();
		
	for(int j=0; j<data.numInstances(); j++)
	  data.instance(j).setClassValue(1);       // Set one class as default
		
	for(int k=0; k < dataByClass.length; k++)    // Merge into one dataset
	  if(k != (int)defClass){
	    if(data.numInstances() >= dataByClass[k].numInstances())
	      data = append(data, dataByClass[k]);
	    else data = append(dataByClass[k], data);
	  }
		
	data.setClassIndex(index);           // Position new class label
		
	double classCount = total - isPure[(int)defClass];
	finalRules = new Vector();
	buildRuleset(data, classCount, finalRules);
	if(finalRules.size() == 0)           // No good rules built
	  return;
      }
      else{
	double maxAcRt = isPure[Utils.maxIndex(isPure)] / total;
		
	// Find default class
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
	    double classCount = data.sumOfWeights() - isPure[i];
	    Vector ruleset = new Vector();
	    double wAcRt = buildRuleset(data, classCount, ruleset); 
			
	    if(Utils.gr(wAcRt, maxAcRt)){
	      finalRules = ruleset;
	      maxAcRt = wAcRt;
	      clas = i;
	    }
	  }
	}
		
	if(finalRules == null){ // No good rules found, set majority class as default
	  defClass = (double)Utils.maxIndex(isPure);
	  return;
	}
		
	defClass = (double)clas;
      }
			
      /* Store the exception rules and default class in this node */
      int size = finalRules.size();
      rules = new RidorRule[size];
      excepts = new Ridor_node[size];
      for(int l=0; l < size; l++)
	rules[l] = (RidorRule)finalRules.elementAt(l);
	    
      /* Build exceptions for each exception rule */
      Instances[] uncovered = dataByClass; 
      if(level == 1)  // The error of default rule
	m_Err = total - uncovered[(int)defClass].sumOfWeights();			

      uncovered[(int)defClass] = new Instances(uncovered[(int)defClass], 0);    
	    
      for(int m=0; m < size; m++){
	/* The data covered by this rule, they are also deducted from the original data */
	Instances[][] dvdData = divide(rules[m], uncovered);
	Instances[] covered = dvdData[0];    // Data covered by the rule
	//uncovered = dvdData[1];            // Data not covered by the rule
	excepts[m] = new Ridor_node();
	excepts[m].findRules(covered, level);// Find exceptions on the covered data
      }
    }

    /**	
     * Private function to build a rule set and return the weighted avg of accuracy
     * rate of rules in the set.
     *
     * @param insts the data used to build ruleset
     * @param classCount the counts of the instances with the predicted class but not
     *                   yet covered by the ruleset
     * @param ruleset the ruleset to be built
     * @return the weighted accuracy rate of the ruleset
     * @throws if the rules cannot be built properly
     */
    private double buildRuleset(Instances insts, double classCount, Vector ruleset) 
      throws Exception {	    
      Instances data = new Instances(insts);
      double wAcRt = 0;  // The weighted accuracy rate of this ruleset
      double total = data.sumOfWeights();
	    
      while( classCount >= m_Folds ){      // Data is not pure
	RidorRule bestRule = null;
	double bestWorthRate= -1;        // The best worth achieved by
	double bestWorth = -1;           // randomization of the data
		
	RidorRule rule = new RidorRule();                                
	rule.setPredictedClass(0);       // Predict the classes other than default
		
	for(int j = 0; j < m_Shuffle; j++){
	  if(m_Shuffle > 1)
	    data.randomize(m_Random);
		    
	  rule.buildClassifier(data);
		    
	  double wr, w; // Worth rate and worth
	  if(m_IsAllErr){
	    wr = (rule.getWorth()+rule.getAccuG()) / 
	      (rule.getCoverP()+rule.getCoverG());
	    w = rule.getWorth() + rule.getAccuG();
	  }
	  else{
	    wr = rule.getWorthRate();
	    w = rule.getWorth(); 
	  }
		    
	  if(Utils.gr(wr, bestWorthRate) ||
	     (Utils.eq(wr, bestWorthRate) && Utils.gr(w, bestWorth))){
	    bestRule = rule;
	    bestWorthRate = wr;
	    bestWorth = w;
	  }
	}
		
	if (bestRule == null)
	  throw new Exception("Something wrong here inside findRule()!");
		
	if(Utils.sm(bestWorthRate, 0.5) || (!bestRule.hasAntds()))
	  break;                       // No more good rules generated
		
	Instances newData = new Instances(data); 
	data = new Instances(newData, 0);// Empty the data
	classCount = 0;
	double cover = 0;                // Coverage of this rule on whole data
		
	for(int l=0; l<newData.numInstances(); l++){
	  Instance datum = newData.instance(l);
	  if(!bestRule.isCover(datum)){// Data not covered by the previous rule
	    data.add(datum);
	    if(Utils.eq(datum.classValue(), 0)) 
	      classCount += datum.weight(); // The predicted class in the data
	  }
	  else cover += datum.weight();
	}			
		
	wAcRt += computeWeightedAcRt(bestWorthRate, cover, total);
	ruleset.addElement(bestRule);			
      }  
	    
      /* The weighted def. accuracy */
      double wDefAcRt = (data.sumOfWeights()-classCount) / total;		    
      wAcRt += wDefAcRt;
	    
      return wAcRt;
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
     * (worth rate) * (coverage proportion)
     *
     *                               coverage of the rule on the whole data
     * where coverage proportion = -----------------------------------------
     *                              the whole data size fed into the ruleset
     *
     * @param worthRt the worth rate
     * @param cover the coverage of the rule on the whole data
     * @param total the total data size fed into the ruleset
     * @return the weighted accuracy rate of this rule
     */
    private double computeWeightedAcRt(double worthRt, double cover, double total){
	  
      return (worthRt * (cover/total));	
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
    private Instances[][] divide(RidorRule rule, Instances[] dataByClass){
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
	    
      if(level == 1)
	text.append(m_Class.name() + " = " + m_Class.value((int)getDefClass())+
		    "  ("+m_Cover+"/"+m_Err+")\n");
      if(rules != null){
	for(int i=0; i < rules.length; i++){
	  for(int j=0; j < level; j++)
	    text.append("         ");
	  String cl = m_Class.value((int)(excepts[i].getDefClass()));
	  text.append("  Except " + 
		      rules[i].toString(m_Class.name(), cl)+
		      "\n" + excepts[i].toString());
	}
      }
	    
      return text.toString();
    }
  }    

  /**
   * This class implements a single rule that predicts the 2-class distribution.  
   *
   * A rule consists of antecedents "AND"ed together and the consequent (class value) 
   * for the classification.  In this case, the consequent is the distribution of
   * the available classes (always 2 classes) in the dataset.  
   * In this class, the Information Gain (p*[log(p/t) - log(P/T)]) is used to select 
   * an antecedent and Reduced Error Prunning (REP) is used to prune the rule. 
   *
   */
  private class RidorRule 
    implements WeightedInstancesHandler, Serializable {
	
    /** for serialization */
    static final long serialVersionUID = 4375199423973848157L;
    
    /** The internal representation of the class label to be predicted*/
    private double m_Class = -1;	
	
    /** The class attribute of the data*/
    private Attribute m_ClassAttribute;
	
    /** The vector of antecedents of this rule*/
    protected FastVector m_Antds = null;
	
    /** The worth rate of this rule, in this case, accuracy rate in the pruning data*/
    private double m_WorthRate = 0;
	
    /** The worth value of this rule, in this case, accurate # in pruning data*/
    private double m_Worth = 0;
	
    /** The sum of weights of the data covered by this rule in the pruning data */
    private double m_CoverP = 0;   
	
    /** The accurate and covered data of this rule in the growing data */
    private double m_CoverG = 0, m_AccuG = 0;   	
  
    /** The access functions for parameters */
    public void setPredictedClass(double cl){  m_Class = cl; }
    public double getPredictedClass(){ return m_Class; }
	
    /**
     * Builds a single rule learner with REP dealing with 2 classes.
     * This rule learner always tries to predict the class with label 
     * m_Class.
     *
     * @param instances the training data
     * @throws Exception if classifier can't be built successfully
     */
    public void buildClassifier(Instances instances) throws Exception {
      m_ClassAttribute = instances.classAttribute();
      if (!m_ClassAttribute.isNominal()) 
	throw new UnsupportedClassTypeException(" Only nominal class, please.");
      if(instances.numClasses() != 2)
	throw new Exception(" Only 2 classes, please.");
	    
      Instances data = new Instances(instances);
      if(Utils.eq(data.sumOfWeights(),0))
	throw new Exception(" No training data.");
	    
      data.deleteWithMissingClass();
      if(Utils.eq(data.sumOfWeights(),0))
	throw new Exception(" The class labels of all the training data are missing.");	
	    
      if(data.numInstances() < m_Folds)
	throw new Exception(" Not enough data for REP.");
	    
      m_Antds = new FastVector();	
	    
      /* Split data into Grow and Prune*/
      m_Random = new Random(m_Seed);
      data.randomize(m_Random);
      data.stratify(m_Folds);
      Instances growData=data.trainCV(m_Folds, m_Folds-1, m_Random);
      Instances pruneData=data.testCV(m_Folds, m_Folds-1);
	    
      grow(growData);      // Build this rule
	    
      prune(pruneData);    // Prune this rule
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
	    
      m_AccuG = computeDefAccu(growData);
      m_CoverG = growData.sumOfWeights();
      /* Compute the default accurate rate of the growing data */
      double defAcRt= m_AccuG / m_CoverG; 
	    
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
	    if(coveredData != null){
	      double infoGain = antd.getMaxInfoGain();			
	      if(Utils.gr(infoGain, maxInfoGain)){
		oneAntd=antd;
		coverData = coveredData;  
		maxInfoGain = infoGain;
	      }		    
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
	m_CoverP = coverage[antdsSize-1];
	Antd last = (Antd)m_Antds.lastElement();
	m_CoverG = last.getCover();
	m_AccuG = last.getAccu();
      }
      else{                                        // Default rule    
	m_Worth = defAccu;                       // Default WorthValues
	m_WorthRate = defAccuRate;
	m_CoverP = total;
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
    public double getCoverP(){ return m_CoverP; }
    public double getCoverG(){ return m_CoverG; }
    public double getAccuG(){ return m_AccuG; }

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
      text.append(" => " + att + " = " + cl);
      text.append("  ("+m_CoverG+"/"+(m_CoverG - m_AccuG)+") ["+
		  m_CoverP+"/"+(m_CoverP - m_Worth)+"]");
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
  }
    
    
  /** 
   * The single antecedent in the rule, which is composed of an attribute and 
   * the corresponding value.  There are two inherited classes, namely NumericAntd
   * and NominalAntd in which the attributes are numeric and nominal respectively.
   */
  private abstract class Antd 
    implements Serializable {
    
    /** The attribute of the antecedent */
    protected Attribute att;
	
    /** The attribute value of the antecedent.  
       For numeric attribute, value is either 0(1st bag) or 1(2nd bag) */
    protected double value; 
	
    /** The maximum infoGain achieved by this antecedent test */
    protected double maxInfoGain;
	
    /** The accurate rate of this antecedent test on the growing data */
    protected double accuRate;
	
    /** The coverage of this antecedent */
    protected double cover;
	
    /** The accurate data for this antecedent */
    protected double accu;
	
    /** Constructor*/
    public Antd(Attribute a){
      att=a;
      value=Double.NaN; 
      maxInfoGain = 0;
      accuRate = Double.NaN;
      cover = Double.NaN;
      accu = Double.NaN;
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
    public double getAccu(){ return accu; } 
    public double getCover(){ return cover; } 
  }
    
  /** 
   * The antecedent with numeric attribute
   */
  private class NumericAntd 
    extends Antd {
    
    /** for serialization */
    static final long serialVersionUID = 1968761518014492214L;
	
    /** The split point for this numeric antecedent */
    private double splitPoint;
	
    /** Constructor*/
    public NumericAntd(Attribute a){ 
      super(a);
      splitPoint = Double.NaN;
    }    
	
    /** Get split point of this numeric antecedent */
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

      // Compute minimum number of Instances required in each split
      double minSplit =  0.1 * (data.sumOfWeights()) / 2.0;
      if (Utils.smOrEq(minSplit,m_MinNo)) 
	minSplit = m_MinNo;
      else if (Utils.gr(minSplit,25)) 
	minSplit = 25;	    
	    
      double fstCover=0, sndCover=0, fstAccu=0, sndAccu=0;
	    
      for(int x=0; x<data.numInstances(); x++){
	Instance inst = data.instance(x);
	if(inst.isMissing(att)){
	  total = x;
	  break;
	}
		
	sndCover += inst.weight();
	if(Utils.eq(inst.classValue(), cl))
	  sndAccu += inst.weight();
      }
	    
      // Enough Instances with known values?
      if (Utils.sm(sndCover,(2*minSplit)))
	return null;
	    
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
		    
	  if(Utils.sm(fstCover, minSplit) || Utils.sm(sndCover, minSplit)){
	    prev=split;  // Cannot split because either
	    continue;    // split has not enough data
	  }
		    
	  double fstAccuRate = 0, sndAccuRate = 0;
	  if(!Utils.eq(fstCover,0))
	    fstAccuRate = fstAccu/fstCover;		
	  if(!Utils.eq(sndCover,0))
	    sndAccuRate = sndAccu/sndCover;
		    
	  /* Which bag has higher information gain? */
	  boolean isFirst; 
	  double fstInfoGain, sndInfoGain;
	  double accRate, infoGain, coverage, accurate;
		    
	  fstInfoGain = Utils.eq(fstAccuRate, 0) ? 
	    0 : (fstAccu*(Utils.log2(fstAccuRate) - Utils.log2(defAcRt)));
	  sndInfoGain = Utils.eq(sndAccuRate, 0) ? 
	    0 : (sndAccu*(Utils.log2(sndAccuRate) - Utils.log2(defAcRt)));
	  if(Utils.gr(fstInfoGain,sndInfoGain) || 
	     (Utils.eq(fstInfoGain,sndInfoGain)&&(Utils.grOrEq(fstAccuRate,sndAccuRate)))){
	    isFirst = true;
	    infoGain = fstInfoGain;
	    accRate = fstAccuRate;
	    accurate = fstAccu;
	    coverage = fstCover;
	  }
	  else{
	    isFirst = false;
	    infoGain = sndInfoGain;
	    accRate = sndAccuRate;
	    accurate = sndAccu;
	    coverage = sndCover;
	  }
		    
	  boolean isUpdate = Utils.gr(infoGain, maxInfoGain);
		    
	  /* Check whether so far the max infoGain */
	  if(isUpdate){
	    splitPoint = (data.instance(split).value(att) + 
			  data.instance(prev).value(att))/2;
	    value = ((isFirst) ? 0 : 1);
	    accuRate = accRate;
	    accu = accurate;
	    cover = coverage;
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
      return (att.name() + symbol + Utils.doubleToString(splitPoint, 6));
    }   
  }
    
    
  /** 
   * The antecedent with nominal attribute
   */
  private class NominalAntd 
    extends Antd {
    
    /** for serialization */
    static final long serialVersionUID = -256386137196078004L;
	
    /* The parameters of infoGain calculated for each attribute value */
    private double[] accurate;
    private double[] coverage;
    private double[] infoGain;
	
    /** Constructor*/
    public NominalAntd(Attribute a){ 
      super(a);
      int bag = att.numValues();
      accurate = new double[bag];
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
	accurate[x] = coverage[x] = infoGain[x] = 0;
	splitData[x] = new Instances(data, data.numInstances());
      }
	    
      for(int x=0; x<data.numInstances(); x++){
	Instance inst=data.instance(x);
	if(!inst.isMissing(att)){
	  int v = (int)inst.value(att);
	  splitData[v].add(inst);
	  coverage[v] += inst.weight();
	  if(Utils.eq(inst.classValue(), cl))
	    accurate[v] += inst.weight();
	}
      }
	    
      // Check if >=2 splits have more than the minimal data
      int count=0; 
      for(int x=0; x<bag; x++){
	double t = coverage[x];
	if(Utils.grOrEq(t, m_MinNo)){
	  double p = accurate[x];		
		    
	  if(!Utils.eq(t, 0.0))
	    infoGain[x] = p *((Utils.log2(p/t)) - (Utils.log2(defAcRt)));
	  ++count;
	}
      }
	        
      if(count < 2) // Don't split
	return null;
	    
      value = (double)Utils.maxIndex(infoGain);
	    
      cover = coverage[(int)value];
      accu = accurate[(int)value];
	    
      if(!Utils.eq(cover,0))
	accuRate = accu / cover;
      else accuRate = 0;
	    
      maxInfoGain = infoGain [(int)value];
	    
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

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Builds a ripple-down manner rule learner.
   *
   * @param data the training data
   * @throws Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    Instances data = new Instances(instances);
    data.deleteWithMissingClass();
    
    int numCl = data.numClasses();
    m_Root = new Ridor_node();
    m_Class = instances.classAttribute();     // The original class label
	
    int index = data.classIndex();
    m_Cover = data.sumOfWeights();
	
    /* Create a binary attribute */
    FastVector binary_values = new FastVector(2);
    binary_values.addElement("otherClasses");
    binary_values.addElement("defClass");
    Attribute attr = new Attribute ("newClass", binary_values);
    data.insertAttributeAt(attr, index);	
    data.setClassIndex(index);                 // The new class label

    /* Partition the data into bags according to their original class values */
    Instances[] dataByClass = new Instances[numCl];
    for(int i=0; i < numCl; i++)
      dataByClass[i] = new Instances(data, data.numInstances()); // Empty bags
    for(int i=0; i < data.numInstances(); i++){ // Partitioning
      Instance inst = data.instance(i);
      inst.setClassValue(0);           // Set new class vaue to be 0
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
    RidorRule[] rules = node.getRules();

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
   * -A <br>
   * Set flag of whether use the error rate of all the data to select
   * the default class in each step. If not set, the learner will only use
   * the error rate in the pruning data <p>
   *
   * -M <br>
   * Set flag of whether use the majority class as the default class
   * in each step instead of choosing default class based on the error rate
   * (if the flag is not set) <p>  
   * 
   * -N number <br>
   * Set the minimal weights of instances within a split.
   * (Default: 2) <p>
   *    
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(5);
	
    newVector.addElement(new Option("\tSet number of folds for IREP\n" +
				    "\tOne fold is used as pruning set.\n" +
				    "\t(default 3)","F", 1, "-F <number of folds>"));
    newVector.addElement(new Option("\tSet number of shuffles to randomize\n" +
				    "\tthe data in order to get better rule.\n" +
				    "\t(default 10)","S", 1, "-S <number of shuffles>"));
    newVector.addElement(new Option("\tSet flag of whether use the error rate \n"+
				    "\tof all the data to select the default class\n"+
				    "\tin each step. If not set, the learner will only use"+
				    "\tthe error rate in the pruning data","A", 0, "-A"));
    newVector.addElement(new Option("\t Set flag of whether use the majority class as\n"+
				    "\tthe default class in each step instead of \n"+
				    "\tchoosing default class based on the error rate\n"+
				    "\t(if the flag is not set)","M", 0, "-M"));
    newVector.addElement(new Option("\tSet the minimal weights of instances\n" +
				    "\twithin a split.\n" +
				    "\t(default 2.0)","N", 1, "-N <min. weights>"));		
    return newVector.elements();
  }
    
  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -F &lt;number of folds&gt;
   *  Set number of folds for IREP
   *  One fold is used as pruning set.
   *  (default 3)</pre>
   * 
   * <pre> -S &lt;number of shuffles&gt;
   *  Set number of shuffles to randomize
   *  the data in order to get better rule.
   *  (default 10)</pre>
   * 
   * <pre> -A
   *  Set flag of whether use the error rate 
   *  of all the data to select the default class
   *  in each step. If not set, the learner will only use the error rate in the pruning data</pre>
   * 
   * <pre> -M
   *   Set flag of whether use the majority class as
   *  the default class in each step instead of 
   *  choosing default class based on the error rate
   *  (if the flag is not set)</pre>
   * 
   * <pre> -N &lt;min. weights&gt;
   *  Set the minimal weights of instances
   *  within a split.
   *  (default 2.0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
	
    String numFoldsString = Utils.getOption('F', options);
    if (numFoldsString.length() != 0) 
      m_Folds = Integer.parseInt(numFoldsString);
    else 
      m_Folds = 3;
	
    String numShuffleString = Utils.getOption('S', options);
    if (numShuffleString.length() != 0) 
      m_Shuffle = Integer.parseInt(numShuffleString);
    else 
      m_Shuffle = 1;

    String seedString = Utils.getOption('s', options);
    if (seedString.length() != 0) 
      m_Seed = Integer.parseInt(seedString);
    else 
      m_Seed = 1;
	
    String minNoString = Utils.getOption('N', options);
    if (minNoString.length() != 0) 
      m_MinNo = Double.parseDouble(minNoString);
    else 
      m_MinNo = 2.0;
	
    m_IsAllErr = Utils.getFlag('A', options);
    m_IsMajority = Utils.getFlag('M', options);
  }
    
  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
	
    String [] options = new String [8];
    int current = 0;
    options[current++] = "-F"; options[current++] = "" + m_Folds;
    options[current++] = "-S"; options[current++] = "" + m_Shuffle;
    options[current++] = "-N"; options[current++] = "" + m_MinNo;
	
    if(m_IsAllErr)
      options[current++] = "-A";
    if(m_IsMajority)
      options[current++] = "-M";	
    while (current < options.length) 
      options[current++] = "";
    return options;
  }
    
  /** Set and get members for parameters */

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String foldsTipText() {
    return "Determines the amount of data used for pruning. One fold is used for "
      + "pruning, the rest for growing the rules.";
  }

  public void setFolds(int fold){ m_Folds = fold; }
  public int getFolds(){ return m_Folds; }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String shuffleTipText() {
    return "Determines how often the data is shuffled before a rule "
      + "is chosen. If > 1, a rule is learned multiple times and the "
      + "most accurate rule is chosen.";
  }

  public void setShuffle(int sh){ m_Shuffle = sh; }
  public int getShuffle(){ return m_Shuffle; }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The seed used for randomizing the data.";
  }

  public void setSeed(int s){ m_Seed = s; }
  public int getSeed(){ return m_Seed; }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String wholeDataErrTipText() {
    return "Whether worth of rule is computed based on all the data "
      + "or just based on data covered by rule.";
  }

  public void setWholeDataErr(boolean a){ m_IsAllErr = a; }
  public boolean getWholeDataErr(){ return m_IsAllErr; }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String majorityClassTipText() {
    return "Whether the majority class is used as default.";
  }
  public void setMajorityClass(boolean m){ m_IsMajority = m; }
  public boolean getMajorityClass(){ return m_IsMajority; }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minNoTipText() {
    return "The minimum total weight of the instances in a rule.";
  }

  public void setMinNo(double m){  m_MinNo = m; }
  public double getMinNo(){ return m_MinNo; }
    
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
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureNumRules") == 0) 
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
	
    return (double)(size+1); // Add the default rule
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
