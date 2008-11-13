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
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.rules;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.ContingencyTables;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * This class implements a single conjunctive rule learner that can predict for numeric and nominal class labels.<br/>
 * <br/>
 * A rule consists of antecedents "AND"ed together and the consequent (class value) for the classification/regression.  In this case, the consequent is the distribution of the available classes (or mean for a numeric value) in the dataset. If the test instance is not covered by this rule, then it's predicted using the default class distributions/value of the data not covered by the rule in the training data.This learner selects an antecedent by computing the Information Gain of each antecendent and prunes the generated rule using Reduced Error Prunning (REP) or simple pre-pruning based on the number of antecedents.<br/>
 * <br/>
 * For classification, the Information of one antecedent is the weighted average of the entropies of both the data covered and not covered by the rule.<br/>
 * For regression, the Information is the weighted average of the mean-squared errors of both the data covered and not covered by the rule.<br/>
 * <br/>
 * In pruning, weighted average of the accuracy rates on the pruning data is used for classification while the weighted average of the mean-squared errors on the pruning data is used for regression.<br/>
 * <br/>
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for REP
 *  One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -R
 *  Set if NOT uses randomization
 *  (default:use randomization)</pre>
 * 
 * <pre> -E
 *  Set whether consider the exclusive
 *  expressions for nominal attributes
 *  (default false)</pre>
 * 
 * <pre> -M &lt;min. weights&gt;
 *  Set the minimal weights of instances
 *  within a split.
 *  (default 2.0)</pre>
 * 
 * <pre> -P &lt;number of antecedents&gt;
 *  Set number of antecedents for pre-pruning
 *  if -1, then REP is used
 *  (default -1)</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Set the seed of randomization
 *  (default 1)</pre>
 * 
 <!-- options-end -->
 *
 * @author Xin XU (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.16 $ 
 */
public class ConjunctiveRule 
  extends Classifier 
  implements OptionHandler, WeightedInstancesHandler{
    
  /** for serialization */
  static final long serialVersionUID = -5938309903225087198L;
  
  /** The number of folds to split data into Grow and Prune for REP*/
  private int m_Folds = 3;
    
  /** The class attribute of the data*/
  private Attribute m_ClassAttribute;
    
  /** The vector of antecedents of this rule*/
  protected FastVector m_Antds = null;
    
  /** The default rule distribution of the data not covered*/
  protected double[] m_DefDstr = null;
    
  /** The consequent of this rule */
  protected double[] m_Cnsqt = null;
        
  /** Number of classes in the training data */
  private int m_NumClasses = 0;
    
  /** The seed to perform randomization */
  private long m_Seed = 1;
    
  /** The Random object used for randomization */
  private Random m_Random = null;
    
  /** The predicted classes recorded for each antecedent in the growing data */
  private FastVector m_Targets;

  /** Whether to use exlusive expressions for nominal attributes */
  private boolean m_IsExclude = false;

  /** The minimal number of instance weights within a split*/
  private double m_MinNo = 2.0;
    
  /** The number of antecedents in pre-pruning */
  private int m_NumAntds = -1;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "This class implements a single conjunctive rule learner that can predict "
      + "for numeric and nominal class labels.\n\n"
      + "A rule consists of antecedents \"AND\"ed together and the consequent (class value) "
      + "for the classification/regression.  In this case, the consequent is the "
      + "distribution of the available classes (or mean for a numeric value) in the dataset. " 
      + "If the test instance is not covered by this rule, then it's predicted "
      + "using the default class distributions/value of the data not covered by the "
      + "rule in the training data."
      + "This learner selects an antecedent by computing the Information Gain of each "
      + "antecendent and prunes the generated rule using Reduced Error Prunning (REP) "
      + "or simple pre-pruning based on the number of antecedents.\n\n"
      + "For classification, the Information of one antecedent is the weighted average of "
      + "the entropies of both the data covered and not covered by the rule.\n"
      + "For regression, the Information is the weighted average of the mean-squared errors "
      + "of both the data covered and not covered by the rule.\n\n"
      + "In pruning, weighted average of the accuracy rates on the pruning data is used "
      + "for classification while the weighted average of the mean-squared errors "
      + "on the pruning data is used for regression.\n\n";
  }

  /** 
   * The single antecedent in the rule, which is composed of an attribute and 
   * the corresponding value.  There are two inherited classes, namely NumericAntd
   * and NominalAntd in which the attributes are numeric and nominal respectively.
   */
  private abstract class Antd
    implements Serializable, RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = -8729076306737827571L;

    /** The attribute of the antecedent */
    protected Attribute att;
	
    /** The attribute value of the antecedent.  
	For numeric attribute, value is either 0(1st bag) or 1(2nd bag) */
    protected double value; 
	
    /** The maximum infoGain achieved by this antecedent test */
    protected double maxInfoGain;
	
    /** The information of this antecedent test on the growing data */
    protected double inform;
	
    /** The parameter related to the meanSquaredError of the data not covered 
	by the previous antecedents when the class is numeric */
    protected double uncoverWtSq, uncoverWtVl, uncoverSum;
	
    /** The parameters related to the data not covered by the previous
	antecedents when the class is nominal */
    protected double[] uncover;
	
    /** Constructor for nominal class */
    public Antd(Attribute a, double[] unc){
      att=a;
      value=Double.NaN; 
      maxInfoGain = 0;
      inform = Double.NaN;
      uncover = unc;	
    }
	
    /** 
     * Constructor for numeric class
     */
    public Antd(Attribute a, double uncoveredWtSq, 
		double uncoveredWtVl, double uncoveredWts){
      att=a;
      value=Double.NaN; 
      maxInfoGain = 0;
      inform = Double.NaN;
      uncoverWtSq = uncoveredWtSq;
      uncoverWtVl = uncoveredWtVl;
      uncoverSum = uncoveredWts;
    }
	
    /* The abstract members for inheritance */
    public abstract Instances[] splitData(Instances data, double defInfo);
    public abstract boolean isCover(Instance inst);
    public abstract String toString();
	
    /* Get functions of this antecedent */
    public Attribute getAttr(){ return att; }
    public double getAttrValue(){ return value; }
    public double getMaxInfoGain(){ return maxInfoGain; }
    public double getInfo(){ return inform;}
	
    /** 
     * Function used to calculate the weighted mean squared error,
     * i.e., sum[x-avg(x)]^2 based on the given elements of the formula:
     * meanSquaredError = sum(Wi*Xi^2) - (sum(WiXi))^2/sum(Wi)
     * 
     * @param weightedSq sum(Wi*Xi^2)
     * @param weightedValue sum(WiXi)
     * @param sum sum of weights
     * @return the weighted mean-squared error
     */
    protected double wtMeanSqErr(double weightedSq, double weightedValue, double sum){
      if(Utils.smOrEq(sum, 1.0E-6))
	return 0;	    
      return (weightedSq - (weightedValue * weightedValue) / sum);
    }
	
    /**
     * Function used to calculate the entropy of given vector of values
     * entropy = (1/sum)*{-sigma[i=1..P](Xi*log2(Xi)) + sum*log2(sum)}
     * where P is the length of the vector
     *
     * @param value the given vector of values
     * @param sum the sum of the given values.  It's provided just for efficiency.
     * @return the entropy
     */
    protected double entropy(double[] value, double sum){	   
      if(Utils.smOrEq(sum, 1.0E-6))
	return 0;

      double entropy = 0;	    
      for(int i=0; i < value.length; i++){
	if(!Utils.eq(value[i],0))
	  entropy -= value[i] * Utils.log2(value[i]);
      }
      entropy += sum * Utils.log2(sum);
      entropy /= sum;
      return entropy;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.16 $");
    }
  }
    
  /** 
   * The antecedent with numeric attribute
   */
  private class NumericAntd 
    extends Antd {
    
    /** for serialization */
    static final long serialVersionUID = -7957266498918210436L;
	
    /** The split point for this numeric antecedent */
    private double splitPoint;
	
    /** 
     * Constructor for nominal class
     */
    public NumericAntd(Attribute a, double[] unc){ 
      super(a, unc);
      splitPoint = Double.NaN;
    }    
	
    /** 
     * Constructor for numeric class
     */
    public NumericAntd(Attribute a, double sq, double vl, double wts){ 
      super(a, sq, vl, wts);
      splitPoint = Double.NaN;
    }
	
    /** 
     * Get split point of this numeric antecedent
     * 
     * @return the split point
     */
    public double getSplitPoint(){ 
      return splitPoint; 
    }
	
    /**
     * Implements the splitData function.  
     * This procedure is to split the data into two bags according 
     * to the information gain of the numeric attribute value
     * the data with missing values are stored in the last split.
     * The maximum infoGain is also calculated.  
     * 
     * @param insts the data to be split
     * @param defInfo the default information for data
     * @return the array of data after split
     */
    public Instances[] splitData(Instances insts, double defInfo){	
      Instances data = new Instances(insts);
      data.sort(att);
      int total=data.numInstances();// Total number of instances without 
      // missing value for att
      maxInfoGain = 0;
      value = 0;	
	    
      // Compute minimum number of Instances required in each split
      double minSplit;
      if(m_ClassAttribute.isNominal()){
	minSplit =  0.1 * (data.sumOfWeights()) /
	  ((double)m_ClassAttribute.numValues());
	if (Utils.smOrEq(minSplit,m_MinNo)) 
	  minSplit = m_MinNo;
	else if (Utils.gr(minSplit,25)) 
	  minSplit = 25;
      }
      else
	minSplit = m_MinNo;
 
      double[] fst=null, snd=null, missing=null;
      if(m_ClassAttribute.isNominal()){
	fst = new double[m_NumClasses];
	snd = new double[m_NumClasses];
	missing = new double[m_NumClasses];
		
	for(int v=0; v < m_NumClasses; v++)
	  fst[v]=snd[v]=missing[v]=0.0;
      }
      double fstCover=0, sndCover=0, fstWtSq=0, sndWtSq=0, fstWtVl=0, sndWtVl=0;
	    
      int split=1;                  // Current split position
      int prev=0;                   // Previous split position		    
      int finalSplit=split;         // Final split position
		    
      for(int x=0; x<data.numInstances(); x++){
	Instance inst = data.instance(x);
	if(inst.isMissing(att)){
	  total = x;
	  break;
	}
		
	sndCover += inst.weight();
	if(m_ClassAttribute.isNominal()) // Nominal class
	  snd[(int)inst.classValue()] += inst.weight();		
	else{                            // Numeric class
	  sndWtSq += inst.weight() * inst.classValue() * inst.classValue();
	  sndWtVl += inst.weight() * inst.classValue();
	}
      }
	    
	    
      // Enough Instances with known values?
      if (Utils.sm(sndCover,(2*minSplit)))
	return null;
	    
      double msingWtSq=0, msingWtVl=0;
      Instances missingData = new Instances(data, 0);
      for(int y=total; y < data.numInstances(); y++){	    
	Instance inst = data.instance(y);
	missingData.add(inst);
	if(m_ClassAttribute.isNominal())
	  missing[(int)inst.classValue()] += inst.weight();
	else{ 
	  msingWtSq += inst.weight() * inst.classValue() * inst.classValue();
	  msingWtVl += inst.weight() * inst.classValue();
	}			
      }	    
	    
      if(total == 0) return null; // Data all missing for the attribute 	
	    
      splitPoint = data.instance(total-1).value(att);	
	    
      for(; split < total; split++){	
	if(!Utils.eq(data.instance(split).value(att), // Can't split 
		     data.instance(prev).value(att))){// within same value 	 
		    
	  // Move the split point
	  for(int y=prev; y<split; y++){
	    Instance inst = data.instance(y);
	    fstCover += inst.weight(); sndCover -= inst.weight();
	    if(m_ClassAttribute.isNominal()){ // Nominal class
	      fst[(int)inst.classValue()] += inst.weight();
	      snd[(int)inst.classValue()] -= inst.weight();
	    }   
	    else{                             // Numeric class
	      fstWtSq += inst.weight() * inst.classValue() * inst.classValue();
	      fstWtVl += inst.weight() * inst.classValue();
	      sndWtSq -= inst.weight() * inst.classValue() * inst.classValue();
	      sndWtVl -= inst.weight() * inst.classValue();
	    }
	  }
		    
	  if(Utils.sm(fstCover, minSplit) || Utils.sm(sndCover, minSplit)){
	    prev=split;  // Cannot split because either
	    continue;    // split has not enough data
	  }
		    
	  double fstEntp = 0, sndEntp = 0;
		    
	  if(m_ClassAttribute.isNominal()){
	    fstEntp = entropy(fst, fstCover);
	    sndEntp = entropy(snd, sndCover);
	  }
	  else{
	    fstEntp = wtMeanSqErr(fstWtSq, fstWtVl, fstCover)/fstCover;
	    sndEntp = wtMeanSqErr(sndWtSq, sndWtVl, sndCover)/sndCover;
	  }
		    
	  /* Which bag has higher information gain? */
	  boolean isFirst; 
	  double fstInfoGain, sndInfoGain;
	  double info, infoGain, fstInfo, sndInfo;
	  if(m_ClassAttribute.isNominal()){
	    double sum = data.sumOfWeights();
	    double otherCover, whole = sum + Utils.sum(uncover), otherEntropy; 
	    double[] other = null;
			
	    // InfoGain of first bag			
	    other = new double[m_NumClasses];
	    for(int z=0; z < m_NumClasses; z++)
	      other[z] = uncover[z] + snd[z] + missing[z];   
	    otherCover = whole - fstCover;			
	    otherEntropy = entropy(other, otherCover);
	    // Weighted average
	    fstInfo = (fstEntp*fstCover + otherEntropy*otherCover)/whole;
	    fstInfoGain = defInfo - fstInfo;
			
	    // InfoGain of second bag 			
	    other = new double[m_NumClasses];
	    for(int z=0; z < m_NumClasses; z++)
	      other[z] = uncover[z] + fst[z] + missing[z]; 
	    otherCover = whole - sndCover;			
	    otherEntropy = entropy(other, otherCover);
	    // Weighted average
	    sndInfo = (sndEntp*sndCover + otherEntropy*otherCover)/whole;			    
	    sndInfoGain = defInfo - sndInfo;			
	  }
	  else{
	    double sum = data.sumOfWeights();
	    double otherWtSq = (sndWtSq + msingWtSq + uncoverWtSq), 
	      otherWtVl = (sndWtVl + msingWtVl + uncoverWtVl),
	      otherCover = (sum - fstCover + uncoverSum);
			
	    fstInfo = Utils.eq(fstCover, 0) ? 0 : (fstEntp * fstCover);
	    fstInfo += wtMeanSqErr(otherWtSq, otherWtVl, otherCover);
	    fstInfoGain = defInfo - fstInfo;
			
	    otherWtSq = (fstWtSq + msingWtSq + uncoverWtSq); 
	    otherWtVl = (fstWtVl + msingWtVl + uncoverWtVl);
	    otherCover = sum - sndCover + uncoverSum;
	    sndInfo = Utils.eq(sndCover, 0) ? 0 : (sndEntp * sndCover);
	    sndInfo += wtMeanSqErr(otherWtSq, otherWtVl, otherCover);
	    sndInfoGain = defInfo - sndInfo;
	  }
		    
	  if(Utils.gr(fstInfoGain,sndInfoGain) || 
	     (Utils.eq(fstInfoGain,sndInfoGain)&&(Utils.sm(fstEntp,sndEntp)))){ 
	    isFirst = true;
	    infoGain = fstInfoGain;
	    info = fstInfo;
	  }
	  else{
	    isFirst = false;
	    infoGain = sndInfoGain;
	    info = sndInfo;
	  }
		    
	  boolean isUpdate = Utils.gr(infoGain, maxInfoGain);
		    
	  /* Check whether so far the max infoGain */
	  if(isUpdate){
	    splitPoint = ((data.instance(split).value(att)) + (data.instance(prev).value(att)))/2.0;
	    value = ((isFirst) ? 0 : 1);
	    inform = info;
	    maxInfoGain = infoGain;
	    finalSplit = split;			
	  }
	  prev=split;
	}
      }
	    
      /* Split the data */
      Instances[] splitData = new Instances[3];
      splitData[0] = new Instances(data, 0, finalSplit);
      splitData[1] = new Instances(data, finalSplit, total-finalSplit);
      splitData[2] = new Instances(missingData);
	    
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
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.16 $");
    }
  }
    
    
  /** 
   * The antecedent with nominal attribute
   */
  class NominalAntd 
    extends Antd {
	
    /** for serialization */
    static final long serialVersionUID = -5949864163376447424L;
    
    /* The parameters of infoGain calculated for each attribute value */
    private double[][] stats;
    private double[] coverage;
    private boolean isIn;
	
    /** 
     * Constructor for nominal class
     */
    public NominalAntd(Attribute a, double[] unc){ 
      super(a, unc);
      int bag = att.numValues();
      stats = new double[bag][m_NumClasses];
      coverage = new double[bag];
      isIn = true;
    }   
	
    /** 
     * Constructor for numeric class
     */
    public NominalAntd(Attribute a, double sq, double vl, double wts){ 
      super(a, sq, vl, wts);
      int bag = att.numValues();	    
      stats = null;
      coverage = new double[bag];
      isIn = true;
    }
	
    /**
     * Implements the splitData function.  
     * This procedure is to split the data into bags according 
     * to the nominal attribute value
     * the data with missing values are stored in the last bag.
     * The infoGain for each bag is also calculated.  
     * 
     * @param data the data to be split
     * @param defInfo the default information for data
     * @return the array of data after split
     */
    public Instances[] splitData(Instances data, double defInfo){
      int bag = att.numValues();
      Instances[] splitData = new Instances[bag+1];
      double[] wSq = new double[bag];
      double[] wVl = new double[bag];
      double totalWS=0, totalWV=0, msingWS=0, msingWV=0, sum=data.sumOfWeights();
      double[] all = new double[m_NumClasses];
      double[] missing = new double[m_NumClasses];	   
	    
      for(int w=0; w < m_NumClasses; w++)
	all[w] = missing[w] = 0;

      for(int x=0; x<bag; x++){
	coverage[x] = wSq[x] = wVl[x] = 0;
	if(stats != null)
	  for(int y=0; y < m_NumClasses; y++)
	    stats[x][y] = 0;		
	splitData[x] = new Instances(data, data.numInstances());
      }
      splitData[bag] = new Instances(data, data.numInstances());
	    
      // Record the statistics of data
      for(int x=0; x<data.numInstances(); x++){
	Instance inst=data.instance(x);
	if(!inst.isMissing(att)){
	  int v = (int)inst.value(att);
	  splitData[v].add(inst);
	  coverage[v] += inst.weight();
	  if(m_ClassAttribute.isNominal()){ // Nominal class			
	    stats[v][(int)inst.classValue()] += inst.weight();
	    all[(int)inst.classValue()] += inst.weight();	    
	  }
	  else{                             // Numeric class
	    wSq[v] += inst.weight() * inst.classValue() * inst.classValue();
	    wVl[v] += inst.weight() * inst.classValue();
	    totalWS += inst.weight() * inst.classValue() * inst.classValue();
	    totalWV += inst.weight() * inst.classValue();
	  }
	}
	else{
	  splitData[bag].add(inst);
	  if(m_ClassAttribute.isNominal()){ // Nominal class
	    all[(int)inst.classValue()] += inst.weight();
	    missing[(int)inst.classValue()] += inst.weight();
	  }
	  else{                            // Numeric class
	    totalWS += inst.weight() * inst.classValue() * inst.classValue();
	    totalWV += inst.weight() * inst.classValue();
	    msingWS += inst.weight() * inst.classValue() * inst.classValue();
	    msingWV += inst.weight() * inst.classValue();		 
	  }
	}
      }
	    
      // The total weights of the whole grow data
      double whole;
      if(m_ClassAttribute.isNominal())
	whole = sum + Utils.sum(uncover);
      else
	whole = sum + uncoverSum;
	  
      // Find the split  
      double minEntrp=Double.MAX_VALUE;
      maxInfoGain = 0;
	    
      // Check if >=2 splits have more than the minimal data
      int count=0;
      for(int x=0; x<bag; x++)
	if(Utils.grOrEq(coverage[x], m_MinNo))		    
	  ++count;
	    
      if(count < 2){ // Don't split
	maxInfoGain = 0;
	inform = defInfo;
	value = Double.NaN;
	return null;
      }
	    
      for(int x=0; x<bag; x++){		
	double t = coverage[x], entrp, infoGain;

	if(Utils.sm(t, m_MinNo))
	  continue;
		
	if(m_ClassAttribute.isNominal()){ // Nominal class	   
	  double[] other = new double[m_NumClasses];
	  for(int y=0; y < m_NumClasses; y++)
	    other[y] = all[y] - stats[x][y] + uncover[y]; 
	  double otherCover = whole - t;	
		    
	  // Entropies of data covered and uncovered 	
	  entrp = entropy(stats[x], t);
	  double uncEntp = entropy(other, otherCover);
		    
	  // Weighted average
	  infoGain = defInfo - (entrp*t + uncEntp*otherCover)/whole;		   
	}	
	else{                             // Numeric class
	  double weight = (whole - t);
	  entrp = wtMeanSqErr(wSq[x], wVl[x], t)/t;
	  infoGain = defInfo - (entrp * t) - 
	    wtMeanSqErr((totalWS-wSq[x]+uncoverWtSq),
			(totalWV-wVl[x]+uncoverWtVl), 
			weight);		  
	}   		
		
	// Test the exclusive expression
	boolean isWithin =true;		
	if(m_IsExclude){
	  double infoGain2, entrp2;
	  if(m_ClassAttribute.isNominal()){ // Nominal class	
	    double[] other2 = new double[m_NumClasses];
	    double[] notIn = new double[m_NumClasses];
	    for(int y=0; y < m_NumClasses; y++){
	      other2[y] = stats[x][y] + missing[y] + uncover[y];
	      notIn[y] = all[y] - stats[x][y] - missing[y];
	    } 
			
	    double msSum = Utils.sum(missing);
	    double otherCover2 = t + msSum + Utils.sum(uncover);
			
	    entrp2 = entropy(notIn, (sum-t-msSum));
	    double uncEntp2 = entropy(other2, otherCover2);
	    infoGain2 = defInfo - 
	      (entrp2*(sum-t-msSum) + uncEntp2*otherCover2)/whole;
	  }
	  else{                             // Numeric class
	    double msWts = splitData[bag].sumOfWeights();
	    double weight2 = t + uncoverSum + msWts;
			
	    entrp2 = wtMeanSqErr((totalWS-wSq[x]-msingWS),
				 (totalWV-wVl[x]-msingWV),(sum-t-msWts))
	      /(sum-t-msWts);
	    infoGain2 = defInfo - entrp2 * (sum-t-msWts) -
	      wtMeanSqErr((wSq[x]+uncoverWtSq+msingWS),
			  (wVl[x]+uncoverWtVl+msingWV), 
			  weight2);
	  }
		    
	  // Use the exclusive expression?
	  if (Utils.gr(infoGain2, infoGain) ||
	      (Utils.eq(infoGain2, infoGain) && Utils.sm(entrp2, entrp))){
	    infoGain = infoGain2;
	    entrp = entrp2;
	    isWithin =false;
	  }
	}
		
	// Test this split
	if (Utils.gr(infoGain, maxInfoGain) ||
	    (Utils.eq(infoGain, maxInfoGain) && Utils.sm(entrp, minEntrp))){
	  value = (double)x;
	  maxInfoGain = infoGain;
	  inform = maxInfoGain - defInfo;
	  minEntrp = entrp;
	  isIn = isWithin;
	}		
      }
	    
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
	if(isIn){
	  if(Utils.eq(inst.value(att), value))
	    isCover=true;
	}
	else if(!Utils.eq(inst.value(att), value))
	  isCover=true;
      }
      return isCover;
    }
	
    /**
     * Whether the expression is "att = value" or att != value"
     * for this nominal attribute.  True if in the former expression, 
     * otherwise the latter
     * 
     * @return the boolean value
     */
    public boolean isIn(){	 
      return isIn;
    }
	
    /**
     * Prints this antecedent
     *
     * @return a textual description of this antecedent
     */
    public String toString() {
      String symbol = isIn ? " = " : " != ";	    
      return (att.name() + symbol + att.value((int)value));
    } 
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.16 $");
    }
  }
    
  /**
   * Returns an enumeration describing the available options
   * Valid options are: <p>
   *
   * -N number <br>
   * Set number of folds for REP. One fold is
   * used as the pruning set. (Default: 3) <p>
   *
   * -R <br>
   * Set if NOT randomize the data before split to growing and 
   * pruning data. If NOT set, the seed of randomization is 
   * specified by the -S option. (Default: randomize) <p>
   * 
   * -S <br>
   * Seed of randomization. (Default: 1)<p>
   *
   * -E <br>
   * Set whether consider the exclusive expressions for nominal
   * attribute split. (Default: false) <p>
   *
   * -M number <br>
   * Set the minimal weights of instances within a split.
   * (Default: 2) <p>
   *
   * -P number <br>
   * Set the number of antecedents allowed in the rule if pre-pruning
   * is used.  If this value is other than -1, then pre-pruning will be
   * used, otherwise the rule uses REP. (Default: -1) <p>
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(6);
	
    newVector.addElement(new Option("\tSet number of folds for REP\n" +
				    "\tOne fold is used as pruning set.\n" +
				    "\t(default 3)","N", 1, "-N <number of folds>"));
	
    newVector.addElement(new Option("\tSet if NOT uses randomization\n" +
				    "\t(default:use randomization)","R", 0, "-R"));

    newVector.addElement(new Option("\tSet whether consider the exclusive\n" +
				    "\texpressions for nominal attributes\n"+
				    "\t(default false)","E", 0, "-E"));
	
    newVector.addElement(new Option("\tSet the minimal weights of instances\n" +
				    "\twithin a split.\n" +
				    "\t(default 2.0)","M", 1, "-M <min. weights>"));
    
    newVector.addElement(new Option("\tSet number of antecedents for pre-pruning\n" +
				    "\tif -1, then REP is used\n" +
				    "\t(default -1)","P", 1, "-P <number of antecedents>"));
    
    newVector.addElement(new Option("\tSet the seed of randomization\n" +
				    "\t(default 1)","S", 1, "-S <seed>"));
    
    return newVector.elements();
  }
    
  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;number of folds&gt;
   *  Set number of folds for REP
   *  One fold is used as pruning set.
   *  (default 3)</pre>
   * 
   * <pre> -R
   *  Set if NOT uses randomization
   *  (default:use randomization)</pre>
   * 
   * <pre> -E
   *  Set whether consider the exclusive
   *  expressions for nominal attributes
   *  (default false)</pre>
   * 
   * <pre> -M &lt;min. weights&gt;
   *  Set the minimal weights of instances
   *  within a split.
   *  (default 2.0)</pre>
   * 
   * <pre> -P &lt;number of antecedents&gt;
   *  Set number of antecedents for pre-pruning
   *  if -1, then REP is used
   *  (default -1)</pre>
   * 
   * <pre> -S &lt;seed&gt;
   *  Set the seed of randomization
   *  (default 1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
	
    String numFoldsString = Utils.getOption('N', options);
    if (numFoldsString.length() != 0) 
      m_Folds = Integer.parseInt(numFoldsString);
    else 
      m_Folds = 3;

    String minNoString = Utils.getOption('M', options);
    if (minNoString.length() != 0) 
      m_MinNo = Double.parseDouble(minNoString);
    else 
      m_MinNo = 2.0;
	
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) 
      m_Seed = Integer.parseInt(seedString);
    else 
      m_Seed = 1;
	
    String numAntdsString = Utils.getOption('P', options);
    if (numAntdsString.length() != 0) 
      m_NumAntds = Integer.parseInt(numAntdsString);
    else 
      m_NumAntds = -1;
	
    m_IsExclude = Utils.getFlag('E', options);	
  }
    
  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
	
    String [] options = new String [9];
    int current = 0;
    options[current++] = "-N"; options[current++] = "" + m_Folds;
    options[current++] = "-M"; options[current++] = "" + m_MinNo;
    options[current++] = "-P"; options[current++] = "" + m_NumAntds;
    options[current++] = "-S"; options[current++] = "" + m_Seed;

    if(m_IsExclude)
      options[current++] = "-E";
	
    while (current < options.length) 
      options[current++] = "";
    return options;
  }
    
  /** The access functions for parameters */

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String foldsTipText() {
    return "Determines the amount of data used for pruning. One fold is used for "
      + "pruning, the rest for growing the rules.";
  }

  /**
   * the number of folds to use
   * 
   * @param folds the number of folds to use
   */
  public void setFolds(int folds) {  
    m_Folds = folds; 
  }
  
  /**
   * returns the current number of folds
   * 
   * @return the number of folds
   */
  public int getFolds() { 
    return m_Folds; 
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The seed used for randomizing the data.";
  }

  /**
   * sets the seed for randomizing the data
   * 
   * @param s the seed value
   */
  public void setSeed(long s) { 
    m_Seed = s;
  }
  
  /**
   * returns the current seed value for randomizing the data
   * 
   * @return the seed value
   */
  public long getSeed() { 
    return m_Seed; 
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String exclusiveTipText() {
    return "Set whether to consider exclusive expressions for nominal "
      + "attribute splits.";
  }

  /**
   * Returns whether exclusive expressions for nominal attributes splits are 
   * considered
   * 
   * @return true if exclusive expressions for nominal attributes splits are
   *         considered
   */
  public boolean getExclusive() { 
    return m_IsExclude;
  }
  
  /**
   * Sets whether exclusive expressions for nominal attributes splits are 
   * considered
   * 
   * @param e whether to consider exclusive expressions for nominal attribute
   *          splits
   */
  public void setExclusive(boolean e) { 
    m_IsExclude = e;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minNoTipText() {
    return "The minimum total weight of the instances in a rule.";
  }

  /**
   * Sets the minimum total weight of the instances in a rule
   * 
   * @param m the minimum total weight of the instances in a rule
   */
  public void setMinNo(double m) {  
    m_MinNo = m; 
  }
  
  /**
   * Gets the minimum total weight of the instances in a rule
   * 
   * @return the minimum total weight of the instances in a rule
   */
  public double getMinNo(){ 
    return m_MinNo; 
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numAntdsTipText() {
    return "Set the number of antecedents allowed in the rule if "
      + "pre-pruning is used.  If this value is other than -1, then "
      + "pre-pruning will be used, otherwise the rule uses reduced-error "
      + "pruning.";
  }

  /**
   * Sets the number of antecedants
   * 
   * @param n the number of antecedants
   */
  public void setNumAntds(int n) {  
    m_NumAntds = n; 
  }
  
  /**
   * Gets the number of antecedants
   * 
   * @return the number of antecedants
   */
  public int getNumAntds(){ 
    return m_NumAntds; 
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
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
    
  /**
   * Builds a single rule learner with REP dealing with nominal classes or
   * numeric classes.
   * For nominal classes, this rule learner predicts a distribution on
   * the classes.
   * For numeric classes, this learner predicts a single value.
   *
   * @param instances the training data
   * @throws Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    Instances data = new Instances(instances);
    data.deleteWithMissingClass();
    
    if(data.numInstances() < m_Folds)
      throw new Exception("Not enough data for REP.");

    m_ClassAttribute = data.classAttribute();
    if(m_ClassAttribute.isNominal())
      m_NumClasses = m_ClassAttribute.numValues();
    else
      m_NumClasses = 1;
	
    m_Antds = new FastVector();
    m_DefDstr = new double[m_NumClasses];
    m_Cnsqt = new double[m_NumClasses];
    m_Targets = new FastVector();	    
    m_Random = new Random(m_Seed);
    
    if(m_NumAntds != -1){
      grow(data);
    }
    else{

      data.randomize(m_Random);

      // Split data into Grow and Prune	   
      data.stratify(m_Folds);
	
      Instances growData=data.trainCV(m_Folds, m_Folds-1, m_Random);
      Instances pruneData=data.testCV(m_Folds, m_Folds-1);

      grow(growData);      // Build this rule  
      prune(pruneData);    // Prune this rule		  	  
    }
	
    if(m_ClassAttribute.isNominal()){			   
      Utils.normalize(m_Cnsqt);
      if(Utils.gr(Utils.sum(m_DefDstr), 0))
	Utils.normalize(m_DefDstr);
    }	
  }
    
  /**
   * Computes class distribution for the given instance.
   *
   * @param instance the instance for which distribution is to be computed
   * @return the class distribution for the given instance
   * @throws Exception if given instance is null
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
      if(instance == null)
	  throw new Exception("Testing instance is NULL!");
	
    if (isCover(instance))		
      return m_Cnsqt;
    else
      return m_DefDstr;
  }
 
  /**
   * Whether the instance covered by this rule
   * 
   * @param datum the instance in question
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
    double defInfo;	
    double whole = data.sumOfWeights();

    if(m_NumAntds != 0){
	
      /* Class distribution for data both covered and not covered by one antecedent */
      double[][] classDstr = new double[2][m_NumClasses];
	    
      /* Compute the default information of the growing data */
      for(int j=0; j < m_NumClasses; j++){
	classDstr[0][j] = 0;
	classDstr[1][j] = 0;
      }	
      if(m_ClassAttribute.isNominal()){	    
	for(int i=0; i < growData.numInstances(); i++){
	  Instance datum = growData.instance(i);
	  classDstr[0][(int)datum.classValue()] += datum.weight();
	}
	defInfo = ContingencyTables.entropy(classDstr[0]);    
      }
      else{
	for(int i=0; i < growData.numInstances(); i++){
	  Instance datum = growData.instance(i);
	  classDstr[0][0] += datum.weight() * datum.classValue();
	}
		
	// No need to be divided by the denomitor because
	// it's always the same
	double defMean = (classDstr[0][0] / whole);
	defInfo = meanSquaredError(growData, defMean) * growData.sumOfWeights();    
      }
	    
      // Store the default class distribution	
      double[][] tmp = new double[2][m_NumClasses];
      for(int y=0; y < m_NumClasses; y++){
	if(m_ClassAttribute.isNominal()){	
	  tmp[0][y] = classDstr[0][y];
	  tmp[1][y] = classDstr[1][y];
	}
	else{
	  tmp[0][y] = classDstr[0][y]/whole;
	  tmp[1][y] = classDstr[1][y];
	}
      }
      m_Targets.addElement(tmp); 
	    
      /* Keep the record of which attributes have already been used*/    
      boolean[] used=new boolean[growData.numAttributes()];
      for (int k=0; k<used.length; k++)
	used[k]=false;
      int numUnused=used.length;	
      double maxInfoGain, uncoveredWtSq=0, uncoveredWtVl=0, uncoveredWts=0;	
      boolean isContinue = true; // The stopping criterion of this rule	
	    
      while (isContinue){   
	maxInfoGain = 0;       // We require that infoGain be positive
		
	/* Build a list of antecedents */
	Antd oneAntd=null;
	Instances coverData = null, uncoverData = null;
	Enumeration enumAttr=growData.enumerateAttributes();	    
	int index=-1;  
		
	/* Build one condition based on all attributes not used yet*/
	while (enumAttr.hasMoreElements()){
	  Attribute att= (Attribute)(enumAttr.nextElement());
	  index++;
		    
	  Antd antd =null;	
	  if(m_ClassAttribute.isNominal()){		    
	    if(att.isNumeric())
	      antd = new NumericAntd(att, classDstr[1]);
	    else
	      antd = new NominalAntd(att, classDstr[1]);
	  }
	  else
	    if(att.isNumeric())
	      antd = new NumericAntd(att, uncoveredWtSq, uncoveredWtVl, uncoveredWts);
	    else
	      antd = new NominalAntd(att, uncoveredWtSq, uncoveredWtVl, uncoveredWts); 
		    
	  if(!used[index]){
	    /* Compute the best information gain for each attribute,
	       it's stored in the antecedent formed by this attribute.
	       This procedure returns the data covered by the antecedent*/
	    Instances[] coveredData = computeInfoGain(growData, defInfo, antd);	 
			
	    if(coveredData != null){
	      double infoGain = antd.getMaxInfoGain();			
	      boolean isUpdate = Utils.gr(infoGain, maxInfoGain);
			    
	      if(isUpdate){
		oneAntd=antd;
		coverData = coveredData[0]; 
		uncoverData = coveredData[1];  
		maxInfoGain = infoGain;		    
	      }
	    }
	  }
	}
		
	if(oneAntd == null) 		
	  break;	    
		
	//Numeric attributes can be used more than once
	if(!oneAntd.getAttr().isNumeric()){ 
	  used[oneAntd.getAttr().index()]=true;
	  numUnused--;
	}
		
	m_Antds.addElement(oneAntd);
	growData = coverData;// Grow data size is shrinking 	    
		
	for(int x=0; x < uncoverData.numInstances(); x++){
	  Instance datum = uncoverData.instance(x);
	  if(m_ClassAttribute.isNumeric()){
	    uncoveredWtSq += datum.weight() * datum.classValue() * datum.classValue();
	    uncoveredWtVl += datum.weight() * datum.classValue();
	    uncoveredWts += datum.weight();
	    classDstr[0][0] -= datum.weight() * datum.classValue();
	    classDstr[1][0] += datum.weight() * datum.classValue();
	  }
	  else{
	    classDstr[0][(int)datum.classValue()] -= datum.weight();
	    classDstr[1][(int)datum.classValue()] += datum.weight();
	  }
	}	       
		
	// Store class distribution of growing data
	tmp = new double[2][m_NumClasses];
	for(int y=0; y < m_NumClasses; y++){
	  if(m_ClassAttribute.isNominal()){	
	    tmp[0][y] = classDstr[0][y];
	    tmp[1][y] = classDstr[1][y];
	  }
	  else{
	    tmp[0][y] = classDstr[0][y]/(whole-uncoveredWts);
	    tmp[1][y] = classDstr[1][y]/uncoveredWts;
	  }
	}
	m_Targets.addElement(tmp);  
		
	defInfo = oneAntd.getInfo();
	int numAntdsThreshold = (m_NumAntds == -1) ? Integer.MAX_VALUE : m_NumAntds;

	if(Utils.eq(growData.sumOfWeights(), 0.0) || 
	   (numUnused == 0) ||
	   (m_Antds.size() >= numAntdsThreshold))
	  isContinue = false;
      }
    }
	
    m_Cnsqt = ((double[][])(m_Targets.lastElement()))[0];
    m_DefDstr = ((double[][])(m_Targets.lastElement()))[1];	
  }
    
  /** 
   * Compute the best information gain for the specified antecedent
   *  
   * @param instances the data based on which the infoGain is computed
   * @param defInfo the default information of data
   * @param antd the specific antecedent
   * @return the data covered and not covered by the antecedent
   */
  private Instances[] computeInfoGain(Instances instances, double defInfo, Antd antd){	
    Instances data = new Instances(instances);
	
    /* Split the data into bags.
       The information gain of each bag is also calculated in this procedure */
    Instances[] splitData = antd.splitData(data, defInfo); 
    Instances[] coveredData = new Instances[2];
	
    /* Get the bag of data to be used for next antecedents */
    Instances tmp1 = new Instances(data, 0);
    Instances tmp2 = new Instances(data, 0);
	
    if(splitData == null)	    
      return null;
	
    for(int x=0; x < (splitData.length-1); x++){
      if(x == ((int)antd.getAttrValue()))
	tmp1 = splitData[x];
      else{		
	for(int y=0; y < splitData[x].numInstances(); y++)
	  tmp2.add(splitData[x].instance(y));		    
      }		
    }
	
    if(antd.getAttr().isNominal()){ // Nominal attributes
      if(((NominalAntd)antd).isIn()){ // Inclusive expression
	coveredData[0] = new Instances(tmp1);
	coveredData[1] = new Instances(tmp2);
      }
      else{                           // Exclusive expression
	coveredData[0] = new Instances(tmp2);
	coveredData[1] = new Instances(tmp1);
      }	
    }
    else{                           // Numeric attributes
      coveredData[0] = new Instances(tmp1);
      coveredData[1] = new Instances(tmp2);
    }
	
    /* Add data with missing value */
    for(int z=0; z<splitData[splitData.length-1].numInstances(); z++)
      coveredData[1].add(splitData[splitData.length-1].instance(z));
	
    return coveredData;
  }
    
  /**
   * Prune the rule using the pruning data.
   * The weighted average of accuracy rate/mean-squared error is 
   * used to prune the rule.
   *
   * @param pruneData the pruning data used to prune the rule
   */    
  private void prune(Instances pruneData){
    Instances data=new Instances(pruneData);
    Instances otherData = new Instances(data, 0);
    double total = data.sumOfWeights();
	
    /* The default accurate# and the the accuracy rate on pruning data */
    double defAccu;
    if(m_ClassAttribute.isNumeric())
      defAccu = meanSquaredError(pruneData,
				 ((double[][])m_Targets.firstElement())[0][0]);
    else{
      int predict = Utils.maxIndex(((double[][])m_Targets.firstElement())[0]);
      defAccu = computeAccu(pruneData, predict)/total;
    }
	
    int size=m_Antds.size();
    if(size == 0){
      m_Cnsqt = ((double[][])m_Targets.lastElement())[0];
      m_DefDstr = ((double[][])m_Targets.lastElement())[1];
      return; // Default rule before pruning
    }
	
    double[] worthValue = new double[size];
	
    /* Calculate accuracy parameters for all the antecedents in this rule */
    for(int x=0; x<size; x++){
      Antd antd=(Antd)m_Antds.elementAt(x);
      Instances newData = new Instances(data);
      if(Utils.eq(newData.sumOfWeights(),0.0))
	break;
	    
      data = new Instances(newData, newData.numInstances()); // Make data empty
	    
      for(int y=0; y<newData.numInstances(); y++){
	Instance ins=newData.instance(y);
	if(antd.isCover(ins))              // Covered by this antecedent
	  data.add(ins);                 // Add to data for further 		
	else
	  otherData.add(ins);            // Not covered by this antecedent
      }
	    
      double covered, other;	    
      double[][] classes = 
	(double[][])m_Targets.elementAt(x+1); // m_Targets has one more element
      if(m_ClassAttribute.isNominal()){		
	int coverClass = Utils.maxIndex(classes[0]),
	  otherClass = Utils.maxIndex(classes[1]);
		
	covered = computeAccu(data, coverClass); 
	other = computeAccu(otherData, otherClass);		
      }
      else{
	double coverClass = classes[0][0],
	  otherClass = classes[1][0];
	covered = (data.sumOfWeights())*meanSquaredError(data, coverClass); 
	other = (otherData.sumOfWeights())*meanSquaredError(otherData, otherClass);
      }
	    
      worthValue[x] = (covered + other)/total;
    }
	
    /* Prune the antecedents according to the accuracy parameters */
    for(int z=(size-1); z > 0; z--){	
      // Treatment to avoid precision problems
      double valueDelta;
      if(m_ClassAttribute.isNominal()){
	if(Utils.sm(worthValue[z], 1.0))
	  valueDelta = (worthValue[z] - worthValue[z-1]) / worthValue[z];
	else
	  valueDelta = worthValue[z] - worthValue[z-1];
      }
      else{
	if(Utils.sm(worthValue[z], 1.0))
	  valueDelta = (worthValue[z-1] - worthValue[z]) / worthValue[z];
	else
	  valueDelta = (worthValue[z-1] - worthValue[z]);
      }
	    
      if(Utils.smOrEq(valueDelta, 0.0)){	
	m_Antds.removeElementAt(z);
	m_Targets.removeElementAt(z+1);
      }
      else  break;
    }	
	
    // Check whether this rule is a default rule
    if(m_Antds.size() == 1){
      double valueDelta;
      if(m_ClassAttribute.isNominal()){
	if(Utils.sm(worthValue[0], 1.0))
	  valueDelta = (worthValue[0] - defAccu) / worthValue[0];
	else
	  valueDelta = (worthValue[0] - defAccu);
      }
      else{
	if(Utils.sm(worthValue[0], 1.0))
	  valueDelta = (defAccu - worthValue[0]) / worthValue[0];
	else
	  valueDelta = (defAccu - worthValue[0]);
      }
	    
      if(Utils.smOrEq(valueDelta, 0.0)){
	m_Antds.removeAllElements();
	m_Targets.removeElementAt(1);
      }
    }
	
    m_Cnsqt = ((double[][])(m_Targets.lastElement()))[0];
    m_DefDstr = ((double[][])(m_Targets.lastElement()))[1];
  }
    
  /**
   * Private function to compute number of accurate instances
   * based on the specified predicted class
   * 
   * @param data the data in question
   * @param clas the predicted class
   * @return the default accuracy number
   */
  private double computeAccu(Instances data, int clas){ 
    double accu = 0;
    for(int i=0; i<data.numInstances(); i++){
      Instance inst = data.instance(i);
      if((int)inst.classValue() == clas)
	accu += inst.weight();
    }
    return accu;
  }
    

  /**
   * Private function to compute the squared error of
   * the specified data and the specified mean
   * 
   * @param data the data in question
   * @param mean the specified mean
   * @return the default mean-squared error
   */
  private double meanSquaredError(Instances data, double mean){ 
    if(Utils.eq(data.sumOfWeights(),0.0))
      return 0;
	
    double mSqErr=0, sum = data.sumOfWeights();
    for(int i=0; i < data.numInstances(); i++){
      Instance datum = data.instance(i);
      mSqErr += datum.weight()*
	(datum.classValue() - mean)*
	(datum.classValue() - mean);
    }	 
	
    return (mSqErr / sum);
  }
     
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
	
    return text.toString();
  }
    
  /**
   * Prints this rule
   *
   * @return a textual description of this rule
   */
  public String toString() {
    String title = 
      "\n\nSingle conjunctive rule learner:\n"+
      "--------------------------------\n", body = null;
    StringBuffer text =  new StringBuffer();
    if(m_ClassAttribute != null){
      if(m_ClassAttribute.isNominal()){
	body = toString(m_ClassAttribute.name(), m_ClassAttribute.value(Utils.maxIndex(m_Cnsqt)));
		
	text.append("\n\nClass distributions:\nCovered by the rule:\n");
	for(int k=0; k < m_Cnsqt.length; k++)
	  text.append(m_ClassAttribute.value(k)+ "\t");
	text.append('\n');
	for(int l=0; l < m_Cnsqt.length; l++)
	  text.append(Utils.doubleToString(m_Cnsqt[l], 6)+"\t");
		
	text.append("\n\nNot covered by the rule:\n");
	for(int k=0; k < m_DefDstr.length; k++)
	  text.append(m_ClassAttribute.value(k)+ "\t");
	text.append('\n');
	for(int l=0; l < m_DefDstr.length; l++)
	  text.append(Utils.doubleToString(m_DefDstr[l], 6)+"\t");	    
      }
      else
	body = toString(m_ClassAttribute.name(), Utils.doubleToString(m_Cnsqt[0], 6));
    }
    return (title + body + text.toString());
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.16 $");
  }
    
  /**
   * Main method.
   *
   * @param args the options for the classifier
   */
  public static void main(String[] args) {	
    runClassifier(new ConjunctiveRule(), args);
  }
}
