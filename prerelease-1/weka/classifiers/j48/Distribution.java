/*
 *    Distribution.java
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

import java.io.*;

import java.util.*;
import weka.core.*;

/**
 * Class for handling a distribution of class values.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class Distribution implements Cloneable, Serializable {
  
  // ==================
  // Private variables.
  // ==================

  private double perClassPerBaG[][]; // Weight of instances per class per bag.     
  private double perBaG[];           // Weight of instances per bag.
  private double perClasS[];         // Weight of instances per class.
  private double totaL;              // Total weight of instances.
 
  // ===============
  // Public methods.
  // ===============

  /**
   * Creates and initializes a new distribution.
   */
  
  public Distribution(int numBags,int numClasses){

    int i;

    perClassPerBaG = new double [numBags][0];
    perBaG = new double [numBags];
    perClasS = new double [numClasses];
    for (i=0;i<numBags;i++)
      perClassPerBaG[i] = new double [numClasses];
    totaL = 0;
  }

  /**
   * Creates and initializes a new distribution using the given
   * array. WARNING: it just copies a reference to this array.
   */
  
  public Distribution(double [][] table){

    int i, j;

    perClassPerBaG = table;
    perBaG = new double [table.length];
    perClasS = new double [table[0].length];
    for (i = 0; i < table.length; i++) 
      for (j  = 0; j < table[i].length; j++) {
	perBaG[i] += table[i][j];
	perClasS[j] += table[i][j];
	totaL += table[i][j];
      }
  }

  /**
   * Creates a distribution with only one bag according
   * to instances in source.
   * @exception Exception if something goes wrong
   */

  public Distribution(Instances source) throws Exception {
    
    perClassPerBaG = new double [1][0];
    perBaG = new double [1];
    totaL = 0;
    perClasS = new double [source.numClasses()];
    perClassPerBaG[0] = new double [source.numClasses()];
    Enumeration enum = source.enumerateInstances();
    while (enum.hasMoreElements())
      add(0,(Instance) enum.nextElement());
  }

  /**
   * Creates a distribution according to given instances and
   * model.
   * @exception Exception if something goes wrong
   */

  public Distribution(Instances source, 
		      ClassifierSplitModel modelToUse)
       throws Exception {

    int index;
    Instance instance;
    double[] weights;

    perClassPerBaG = new double [modelToUse.numSubsets()][0];
    perBaG = new double [modelToUse.numSubsets()];
    totaL = 0;
    perClasS = new double [source.numClasses()];
    for (int i = 0; i < modelToUse.numSubsets(); i++)
      perClassPerBaG[i] = new double [source.numClasses()];
    Enumeration enum = source.enumerateInstances();
    while (enum.hasMoreElements()) {
      instance = (Instance) enum.nextElement();
      index = modelToUse.whichSubset(instance);
      if (index != -1)
	add(index, instance);
      else {
	weights = modelToUse.weights(instance);
	addWeights(instance, weights);
      }
    }
  }

  /**
   * Creates distribution with only one bag by merging all
   * bags of given distribution.
   */

  public Distribution(Distribution toMerge){

    totaL = toMerge.totaL;
    perClasS = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.perClasS,0,perClasS,0,toMerge.numClasses());
    perClassPerBaG = new double [1] [0];
    perClassPerBaG[0] = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.perClasS,0,perClassPerBaG[0],0,
		     toMerge.numClasses());
    perBaG = new double [1];
    perBaG[0] = totaL;
  }

  /**
   * Creates distribution with two bags by merging all bags apart of
   * the indicated one.
   */

  public Distribution(Distribution toMerge, int index){

    int i;

    totaL = toMerge.totaL;
    perClasS = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.perClasS,0,perClasS,0,toMerge.numClasses());
    perClassPerBaG = new double [2] [0];
    perClassPerBaG[0] = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.perClassPerBaG[index],0,perClassPerBaG[0],0,
		     toMerge.numClasses());
    perClassPerBaG[1] = new double [toMerge.numClasses()];
    for (i=0;i<toMerge.numClasses();i++)
      perClassPerBaG[1][i] = toMerge.perClasS[i]-perClassPerBaG[0][i];
    perBaG = new double [2];
    perBaG[0] = toMerge.perBaG[index];
    perBaG[1] = totaL-perBaG[0];
  }
  
  /**
   * Returns number of non-empty bags of distribution.
   */
  
  public final int actualNumBags(){
    
    int returnValue = 0;
    int i;

    for (i=0;i<perBaG.length;i++)
      if (Utils.gr(perBaG[i],0))
	returnValue++;
    
    return returnValue;
  }

  /**
   * Returns number of classes actually occuring in distribution.
   */

  public final int actualNumClasses(){

    int returnValue = 0;
    int i;

    for (i=0;i<perClasS.length;i++)
      if (Utils.gr(perClasS[i],0))
	returnValue++;
    
    return returnValue;
  }

  /**
   * Returns number of classes actually occuring in given bag.
   */

  public final int actualNumClasses(int bagIndex){

    int returnValue = 0;
    int i;

    for (i=0;i<perClasS.length;i++)
      if (Utils.gr(perClassPerBaG[bagIndex][i],0))
	returnValue++;
    
    return returnValue;
  }

  /**
   * Adds given instance to given bag.
   * @exception Exception if something goes wrong
   */

  public final void add(int bagIndex,Instance instance) 
       throws Exception {
    
    int classIndex;
    double weight;

    classIndex = (int)instance.classValue();
    weight = instance.weight();
    perClassPerBaG[bagIndex][classIndex] = 
      perClassPerBaG[bagIndex][classIndex]+weight;
    perBaG[bagIndex] = perBaG[bagIndex]+weight;
    perClasS[classIndex] = perClasS[classIndex]+weight;
    totaL = totaL+weight;
  }

  /**
   * Subtracts given instance from given bag.
   * @exception Exception if something goes wrong
   */

  public final void sub(int bagIndex,Instance instance) 
       throws Exception {
    
    int classIndex;
    double weight;

    classIndex = (int)instance.classValue();
    weight = instance.weight();
    perClassPerBaG[bagIndex][classIndex] = 
      perClassPerBaG[bagIndex][classIndex]-weight;
    perBaG[bagIndex] = perBaG[bagIndex]-weight;
    perClasS[classIndex] = perClasS[classIndex]-weight;
    totaL = totaL-weight;
  }

  /**
   * Adds counts to given bag.
   */

  public final void add(int bagIndex, double[] counts){
    
    double sum = Utils.sum(counts);

    for (int i = 0; i < counts.length; i++)
      perClassPerBaG[bagIndex][i] += counts[i];
    perBaG[bagIndex] = perBaG[bagIndex]+sum;
    for (int i = 0; i < counts.length; i++)
      perClasS[i] = perClasS[i]+counts[i];
    totaL = totaL+sum;
  }

  /**
   * Adds all instances with unknown values for given attribute, weighted
   * according to frequency of instances in each bag.
   * @exception Exception if something goes wrong
   */

  public final void addInstWithUnknown(Instances source,
				       int attIndex)
       throws Exception {

    double [] probs;
    double weight,newWeight;
    int classIndex;
    Instance instance;
    int j;

    probs = new double [perBaG.length];
    for (j=0;j<perBaG.length;j++)
      probs[j] = perBaG[j]/totaL;
    Enumeration enum = source.enumerateInstances();
    while (enum.hasMoreElements()){
      instance = (Instance) enum.nextElement();
      if (instance.isMissing(attIndex)){
	classIndex = (int)instance.classValue();
	weight = instance.weight();
	perClasS[classIndex] = perClasS[classIndex]+weight;
	totaL = totaL+weight;
	for (j = 0; j < perBaG.length; j++){
	  newWeight = probs[j]*weight;
	  perClassPerBaG[j][classIndex] = perClassPerBaG[j][classIndex]+
	    newWeight;
	  perBaG[j] = perBaG[j]+newWeight;
	}
      }
    }
  }

  /**
   * Adds all instances in given range to given bag.
   * @exception Exception if something goes wrong
   */

  public final void addRange(int bagIndex,Instances source,
			     int startIndex, int lastPlusOne)
       throws Exception {

    double sumOfWeights = 0;
    int classIndex;
    Instance instance;
    int i;

    for (i = startIndex; i < lastPlusOne; i++) {
      instance = (Instance) source.instance(i);
      classIndex = (int)instance.classValue();
      sumOfWeights = sumOfWeights+instance.weight();
      perClassPerBaG[bagIndex][classIndex] += instance.weight();
      perClasS[classIndex] += instance.weight();
    }
    perBaG[bagIndex] += sumOfWeights;
    totaL += sumOfWeights;
  }

  /**
   * Adds given instance to all bags weighting it according to given weights.
   * @exception Exception if something goes wrong
   */

  public final void addWeights(Instance instance, 
			       double [] weights)
       throws Exception {

    int classIndex;
    int i;

    classIndex = (int)instance.classValue();
    for (i=0;i<perBaG.length;i++){
      perClassPerBaG[i][classIndex] = perClassPerBaG[i][classIndex]+weights[i];
      perBaG[i] = perBaG[i]+weights[i];
      perClasS[classIndex] = perClasS[classIndex]+weights[i];
      totaL = totaL+weights[i];
    }
  }

  /**
   * Checks if at least two bags contain a minimum number of instances.
   */

  public final boolean check(double minNoObj){

    int counter = 0;
    int i;

    for (i=0;i<perBaG.length;i++)
      if (Utils.grOrEq(perBaG[i],minNoObj))
	counter++;
    if (counter > 1)
      return true;
    else
      return false;
  }

  /**
   * Clones distribution (Deep copy of distribution).
   */

  public final Object clone(){

    int i,j;

    Distribution newDistribution = new Distribution (perBaG.length,
						     perClasS.length);
    for (i=0;i<perBaG.length;i++){
      newDistribution.perBaG[i] = perBaG[i];
      for (j=0;j<perClasS.length;j++)
	newDistribution.perClassPerBaG[i][j] = perClassPerBaG[i][j];
    }
    for (j=0;j<perClasS.length;j++)
      newDistribution.perClasS[j] = perClasS[j];
    newDistribution.totaL = totaL;
  
    return newDistribution;
  }

  /**
   * Deletes given instance from given bag.
   * @exception Exception if something goes wrong
   */

  public final void del(int bagIndex,Instance instance) 
       throws Exception {

    int classIndex;
    double weight;

    classIndex = (int)instance.classValue();
    weight = instance.weight();
    perClassPerBaG[bagIndex][classIndex] = 
      perClassPerBaG[bagIndex][classIndex]-weight;
    perBaG[bagIndex] = perBaG[bagIndex]-weight;
    perClasS[classIndex] = perClasS[classIndex]-weight;
    totaL = totaL-weight;
  }

  /**
   * Deletes all instances in given range from given bag.
   * @exception Exception if something goes wrong
   */

  public final void delRange(int bagIndex,Instances source,
			     int startIndex, int lastPlusOne)
       throws Exception {

    double sumOfWeights = 0;
    int classIndex;
    Instance instance;
    int i;

    for (i = startIndex; i < lastPlusOne; i++){
      instance = (Instance) source.instance(i);
      classIndex = (int)instance.classValue();
      sumOfWeights = sumOfWeights+instance.weight();
      perClassPerBaG[bagIndex][classIndex] -= instance.weight();
      perClasS[classIndex] -= instance.weight();
    }
    perBaG[bagIndex] -= sumOfWeights;
    totaL -= sumOfWeights;
  }

  /**
   * Prints distribution.
   */
  
  public final String dumpDistribution(){

    StringBuffer text;
    int i,j;

    text = new StringBuffer();
    for (i=0;i<perBaG.length;i++){
      text.append("Bag num "+i+"\n");
      for (j=0;j<perClasS.length;j++)
	text.append("Class num "+j+" "+perClassPerBaG[i][j]+"\n");
    }
    return text.toString();
  }

  /**
   * Sets all counts to zero.
   */

  public final void initialize() {

    for (int i = 0; i < perClasS.length; i++) 
      perClasS[i] = 0;
    for (int i = 0; i < perBaG.length; i++)
      perBaG[i] = 0;
    for (int i = 0; i < perBaG.length; i++)
      for (int j = 0; j < perClasS.length; j++)
	perClassPerBaG[i][j] = 0;
    totaL = 0;
  }

  /**
   * Returns matrix with distribution of class values.
   */
  
  public final double[][] matrix() {

    return perClassPerBaG;
  }
  
  /**
   * Returns index of bag containing maximum number of instances.
   */

  public final int maxBag(){

    double max;
    int maxIndex;
    int i;
    
    max = 0;
    maxIndex = -1;
    for (i=0;i<perBaG.length;i++)
      if (Utils.grOrEq(perBaG[i],max)){
	max = perBaG[i];
	maxIndex = i;
      }
    return maxIndex;
  }

  /**
   * Returns class with highest frequency over all bags.
   */

  public final int maxClass(){

    double maxCount = 0;
    int maxIndex = 0;
    int i;

    for (i=0;i<perClasS.length;i++)
      if (Utils.gr(perClasS[i],maxCount)){
	maxCount = perClasS[i];
	maxIndex = i;
      }

    return maxIndex;
  }

  /**
   * Returns class with highest frequency for given bag.
   */
  
  public final int maxClass(int index){

    double maxCount = 0;
    int maxIndex = 0;
    int i;

    if (Utils.gr(perBaG[index],0)){
      for (i=0;i<perClasS.length;i++)
	if (Utils.gr(perClassPerBaG[index][i],maxCount)){
	  maxCount = perClassPerBaG[index][i];
	  maxIndex = i;
	}
      return maxIndex;
    }else
      return maxClass();
  }

  /**
   * Returns number of bags.
   */

  public final int numBags(){
    
    return perBaG.length;
  }

  /**
   * Returns number of classes.
   */

  public final int numClasses(){

    return perClasS.length;
  }

  /**
   * Returns perClass(maxClass()).
   */

  public final double numCorrect(){

    return perClasS[maxClass()];
  }

  /**
   * Returns perClassPerBag(index,maxClass(index)).
   */

  public final double numCorrect(int index){

    return perClassPerBaG[index][maxClass(index)];
  }

  /**
   * Returns total-numCorrect().
   */

  public final double numIncorrect(){

    return totaL-numCorrect();
  }

  /**
   * Returns perBag(index)-numCorrect(index).
   */

  public final double numIncorrect(int index){

    return perBaG[index]-numCorrect(index);
  }

  /**
   * Returns number of (possibly fractional) instances of given class in 
   * given bag.
   */

  public final double perClassPerBag(int bagIndex, int classIndex){

    return perClassPerBaG[bagIndex][classIndex];
  }

  /**
   * Returns number of (possibly fractional) instances in given bag.
   */

  public final double perBag(int bagIndex){

    return perBaG[bagIndex];
  }

  /**
   * Returns number of (possibly fractional) instances of given class.
   */

  public final double perClass(int classIndex){

    return perClasS[classIndex];
  }

  /**
   * Returns relative frequency of class over all bags.
   */ 
 
  public final double prob(int classIndex){

    return perClasS[classIndex]/totaL;
  }

  /**
   * Returns relative frequency of class for given bag.
   */

  public final double prob(int classIndex,int intIndex){

    if (Utils.gr(perBaG[intIndex],0))
      return perClassPerBaG[intIndex][classIndex]/perBaG[intIndex];
    else
      return prob(classIndex);
  }

  /** 
   * Subtracts the given distribution from this one. The results
   * has only one bag.
   */

  public final Distribution subtract(Distribution toSubstract) {

    Distribution newDist = new Distribution(1,perClasS.length);

    newDist.perBaG[0] = totaL-toSubstract.totaL;
    newDist.totaL = newDist.perBaG[0];
    for (int i = 0; i < perClasS.length; i++) {
      newDist.perClassPerBaG[0][i] = perClasS[i] - toSubstract.perClasS[i];
      newDist.perClasS[i] = newDist.perClassPerBaG[0][i];
    }
    return newDist;
  }

  /**
   * Returns total number of (possibly fractional) instances.
   */

  public final double total(){

    return totaL;
  }

  /**
   * Shifts given instance from one bag to another one.
   * @exception Exception if something goes wrong
   */

  public final void shift(int from,int to,Instance instance) 
       throws Exception {
    
    int classIndex;
    double weight;

    classIndex = (int)instance.classValue();
    weight = instance.weight();
    perClassPerBaG[from][classIndex] -= weight;
    perClassPerBaG[to][classIndex] += weight;
    perBaG[from] -= weight;
    perBaG[to] += weight;
  }

  /**
   * Shifts all instances in given range from one bag to another one.
   * @exception Exception if something goes wrong
   */

  public final void shiftRange(int from,int to,Instances source,
			       int startIndex,int lastPlusOne) 
       throws Exception {
    
    int classIndex;
    double weight;
    Instance instance;
    int i;

    for (i = startIndex; i < lastPlusOne; i++){
      instance = (Instance) source.instance(i);
      classIndex = (int)instance.classValue();
      weight = instance.weight();
      perClassPerBaG[from][classIndex] -= weight;
      perClassPerBaG[to][classIndex] += weight;
      perBaG[from] -= weight;
      perBaG[to] += weight;
    }
  }
}







