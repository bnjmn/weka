/*
 *    C45Split.java
 *    Copyright (C) 1999 Eibe Frank
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

package weka.classifiers.j48;

import java.util.*;
import weka.core.*;

/**
 * Class implementing a C4.5-type split on an attribute.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class C45Split extends ClassifierSplitModel{

  // ==================
  // Private variables.
  // ==================

  private int complexityIndex;  // Desired number of branches.
  private int attIndeX;         // Attribute to split on.
  private int minNoObJ;         // Minimum number of objects in a split.  
  private double splitPoinT;    // Value of split point.
  private double infoGaiN;      // InfoGain of split.
  private double gainRatiO;     // GainRatio of split. 
  private double sumOfWeightS;  // The sum of the weights of the instances.
  private int indeX;            // Number of split points.

  /**
   * Static references to splitting criteria.
   */

  private static InfoGainSplitCrit infoGainCrit = new InfoGainSplitCrit();
  private static GainRatioSplitCrit gainRatioCrit = new GainRatioSplitCrit();

  // ===============
  // Public methods.
  // ===============

  /**
   * Initializes the split model.
   */

  public C45Split(int attIndex,int minNoObj, double sumOfWeights){

    // Get index of attribute to split on.
    
    attIndeX = attIndex;
        
    // Set minimum number of objects.

    minNoObJ = minNoObj;

    // Set the sum of the weights

    sumOfWeightS = sumOfWeights;
  }

  /**
   * Creates a C4.5-type split on the given data. Assumes that none of
   * the class values is missing.
   * @exception Exception if something goes wrong
   */

  public void buildClassifier(Instances trainInstances) 
       throws Exception {

    // Initialize the remaining instance variables.
    
    numSubsetS = 0;
    splitPoinT = Double.MAX_VALUE;
    infoGaiN = 0;
    gainRatiO = 0;

    // Different treatment for enumerated and numeric
    // attributes.
    
    if (trainInstances.attribute(attIndeX).isNominal()){
      complexityIndex = trainInstances.attribute(attIndeX).numValues();
      indeX = complexityIndex;
      handleEnumeratedAttribute(trainInstances);
    }else{
      complexityIndex = 2;
      indeX = 0;
      trainInstances.sort(trainInstances.attribute(attIndeX));
      handleNumericAttribute(trainInstances);
    }
  }    

  /**
   * Returns index of attribute for which split was generated.
   */

  public final int attIndex(){

    return attIndeX;
  }

  /**
   * Gets class probability for instance.
   * @exception Exception if something goes wrong
   */
  
  public final double classProb(int classIndex,Instance instance)
       throws Exception {

    int theSubset = whichSubset(instance);
    
    if (theSubset <= -1)
      return distributioN.prob(classIndex);
    else
      if (Utils.gr(distributioN.perBag(theSubset),0))
	return distributioN.prob(classIndex,theSubset);
      else
	if (distributioN.maxClass() == classIndex)
	  return 1;
	else
	  return 0;
  }
 
  /**
   * Returns coding cost for split (used in rule learner).
   */

  public final double codingCost() {

    return Utils.log2(indeX);
  }
 
  /**
   * Returns (C4.5-type) gain ratio for the generated split.
   */

  public final double gainRatio(){
    return gainRatiO;
  }

  /**
   * Creates split on enumerated attribute.
   * @exception Exception if something goes wrong
   */

  private void handleEnumeratedAttribute(Instances trainInstances)
       throws Exception {
    
    Instance instance;

    distributioN = new Distribution(complexityIndex,
			      trainInstances.numClasses());
    
    // Only Instances with known values are relevant.
    
    Enumeration enum = trainInstances.enumerateInstances();
    while (enum.hasMoreElements()) {
      instance = (Instance) enum.nextElement();
      if (!instance.isMissing(attIndeX))
	distributioN.add((int)instance.value(attIndeX),instance);
    }
    
    // Check if minimum number of Instances in at least two
    // subsets.
    
    if (distributioN.check(minNoObJ)){
      numSubsetS = complexityIndex;
      infoGaiN = infoGainCrit.
	splitCritValue(distributioN,sumOfWeightS);
      gainRatiO = 
	gainRatioCrit.splitCritValue(distributioN,sumOfWeightS,
				     infoGaiN);
    }
  }
  
  /**
   * Creates split on numeric attribute.
   * @exception Exception if something goes wrong
   */

  private void handleNumericAttribute(Instances trainInstances)
       throws Exception {
  
    int firstMiss;
    int next = 1;
    int last = 0;
    int splitIndex = -1;
    double currentInfoGain;
    double defaultEnt;
    double minSplit;
    Instance instance;
    int i;

    // Current attribute is a numeric attribute.

    distributioN = new Distribution(2,trainInstances.numClasses());
    
    // Only Instances with known values are relevant.

    Enumeration enum = trainInstances.enumerateInstances();
    i = 0;
    while (enum.hasMoreElements()) {
      instance = (Instance) enum.nextElement();
      if (instance.isMissing(attIndeX))
	break;
      distributioN.add(1,instance);
      i++;
    }
    firstMiss = i;
	
    // Compute minimum number of Instances required in each
    // subset.

    minSplit =  0.1*(distributioN.total())/
      ((double)trainInstances.numClasses());
    if (Utils.smOrEq(minSplit,minNoObJ)) 
      minSplit = minNoObJ;
    else
      if (Utils.gr(minSplit,25)) 
	minSplit = 25;
	
    // Enough Instances with known values?

    if (Utils.sm((double)firstMiss,2*minSplit))
      return;
    
    // Compute values of criteria for all possible split
    // indices.

    defaultEnt = infoGainCrit.oldEnt(distributioN);
    while (next < firstMiss){
	  
      if (trainInstances.instance(next-1).value(attIndeX)+1e-5 < 
	  trainInstances.instance(next).value(attIndeX)){ 
	
	// Move class values for all Instances up to next 
	// possible split point.
	
	distributioN.shiftRange(1,0,trainInstances,last,next);
	
	// Check if enough Instances in each subset and compute
	// values for criteria.
	
	if (Utils.grOrEq(distributioN.perBag(0),minSplit) &&
	    Utils.grOrEq(distributioN.perBag(1),minSplit)){
	  currentInfoGain = infoGainCrit.
	    splitCritValue(distributioN,sumOfWeightS,
			   defaultEnt);
	  if (Utils.gr(currentInfoGain,infoGaiN)){
	    infoGaiN = currentInfoGain;
	    splitIndex = next-1;
	  }
	  indeX++;
	}
	last = next;
      }
      next++;
    }
    
    // Was there any useful split?

    if (indeX == 0)
      return;
    
    // Compute modified information gain for best split.

    infoGaiN = infoGaiN-(Utils.log2(indeX)/sumOfWeightS);
    if (Utils.smOrEq(infoGaiN,0))
      return;
    
    // Set instance variables' values to values for
    // best split.

    numSubsetS = 2;
    splitPoinT = 
      (trainInstances.instance(splitIndex+1).value(attIndeX)+
       trainInstances.instance(splitIndex).value(attIndeX))/2;

    // Restore distributioN for best split.

    distributioN = new Distribution(2,trainInstances.numClasses());
    distributioN.addRange(0,trainInstances,0,splitIndex+1);
    distributioN.addRange(1,trainInstances,splitIndex+1,firstMiss);

    // Compute modified gain ratio for best split.

    gainRatiO = gainRatioCrit.
      splitCritValue(distributioN,sumOfWeightS,
		     infoGaiN);
  }

  /**
   * Returns (C4.5-type) information gain for the generated split.
   */

  public final double infoGain(){
    return infoGaiN;
  }

  /**
   * Prints left side of condition..
   * @param index of subset and training set.
   */

  public final String leftSide(Instances data){

    return data.attribute(attIndeX).name();
  }

  /**
   * Prints the condition satisfied by instances in a subset.
   * @param index of subset and training set.
   */

  public final String rightSide(int index,Instances data){

    StringBuffer text;

    text = new StringBuffer();
    if (data.attribute(attIndeX).isNominal())
      text.append(" = "+
		  data.attribute(attIndeX).value(index));
    else
      if (index == 0)
	text.append(" <= "+
		    Utils.doubleToString(splitPoinT,6));
      else
	text.append(" > "+
		    Utils.doubleToString(splitPoinT,6));
    
    return text.toString();
  }
  
  /**
   * Sets split point to greatest value in given data smaller or equal to
   * old split point.
   * (C4.5 does this for some strange reason).
   */

  public final void setSplitPoint(Instances allInstances){
    
    double newSplitPoint = -Double.MAX_VALUE;
    double tempValue;
    Instance instance;
    
    if ((allInstances.attribute(attIndeX).isNumeric()) &&
	(numSubsetS > 1)){
      Enumeration enum = allInstances.enumerateInstances();
      while (enum.hasMoreElements()) {
	instance = (Instance) enum.nextElement();
	if (!instance.isMissing(attIndeX)){
	  tempValue = instance.value(attIndeX);
	  if (Utils.gr(tempValue,newSplitPoint) && 
	      Utils.smOrEq(tempValue,splitPoinT))
	    newSplitPoint = tempValue;
	}
      }
      splitPoinT = newSplitPoint;
    }
  }
  
  /**
   * Returns the minsAndMaxs of the index.th subset.
   */

  public final double [][] minsAndMaxs(Instances data, double [][] minsAndMaxs,
				       int index){

    double [][] newMinsAndMaxs = new double[data.numAttributes()][2];
    
    for (int i = 0; i < data.numAttributes(); i++){
      newMinsAndMaxs[i][0] = minsAndMaxs[i][0];
      newMinsAndMaxs[i][1] = minsAndMaxs[i][1];
      if (i == attIndeX)
	if (data.attribute(attIndeX).isNominal())
	  newMinsAndMaxs[attIndeX][1] = 1;
	else
	  newMinsAndMaxs[attIndeX][1-index] = splitPoinT;
    }

    return newMinsAndMaxs;
  }

  /**
   * Returns weights if instance is assigned to more than one subset.
   * Returns null if instance is only assigned to one subset.
   */

  //public static double rate = 0.75;

  public final double [] weights(Instance instance){
    
    double [] weights;
    int i;
    
    if (instance.isMissing(attIndeX)){
      weights = new double [numSubsetS];
      for (i=0;i<numSubsetS;i++)
	weights [i] = distributioN.perBag(i)/distributioN.total();
      return weights;
    }else{

      return null;
    }
  }
  
  /**
   * Returns index of subset instance is assigned to.
   * Returns -1 if instance is assigned to more than one subset.
   * @exception Exception if something goes wrong
   */

  public final int whichSubset(Instance instance) 
       throws Exception {
    
    if (instance.isMissing(attIndeX))
      return -1;
    else{
      if (instance.attribute(attIndeX).isNominal())
	return (int)instance.value(attIndeX);
      else
	if (Utils.smOrEq(instance.value(attIndeX),splitPoinT))
	  return 0;
	else
	  return 1;
    }
  }
}
