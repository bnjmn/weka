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
 *    InstancesUtil.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.estimators.DiscreteEstimator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * This class contains some methods for working with objects of 
 * type <code> Instance </code> and <code> Instances, </code> not
 * provided by there respective classes.
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.3 $
 */
public class InstancesUtil
  implements RevisionHandler {

  /**
   * Compares two instances, ignoring the class attribute (if any)
   *
   * @param i1 the first instance
   * @param i2 the second instance
   * @return true if both instances are equal (ignoring the class
   * attribute), false otherwise
   */
  public static boolean equalIgnoreClass(Instance i1, Instance i2) {
    int n = i1.numAttributes();
    int classIndex = i1.classIndex();
    if (i2.numAttributes() != n || classIndex != i2.classIndex()) {
      return false;
    }
    int i = 0;
    while(i < n && (i == classIndex 
	|| Utils.eq(i1.value(i), i2.value(i)))) {
      i++;
    }
    return i == n;
  }

  /**
   * Get the index of an instance in a set of instances, where 
   * instances are compared ignoring the class attribute.
   *
   * @param instances the set of instances
   * @param instance to instance to be found in the given set of instances
   * @return the index of the first instance that equals the given instance
   * (ignoring the class attribute), -1 if the instance was not found
   */
  public static int containsIgnoreClass(Instances instances, Instance instance) {
    double[] dd = instance.toDoubleArray();
    int classIndex = instances.classIndex();
    int n = instances.numAttributes();
    Iterator it = 
      new EnumerationIterator(instances.enumerateInstances());
    int index = 0;
    while(it.hasNext()) {
      Instance tmp = (Instance) it.next();
      int i = 0;
      while(i < n && 
	  (i == classIndex || Utils.eq(dd[i], tmp.value(i)))) {
	i++;
      }
      if (i == n) { 
	return index;  // found it
      }
      index++;
    }
    return -1;
  }

  /**
   * Find the next occurence of an instance, ignoring the class,
   * for which the index in the dataset is at least <code> index. </code>
   *
   * @param instances the set of instances to be searched
   * @param instance the instance to be found
   * @param index the minimum index that might be returned
   * @return the index of the first instance with index at 
   * least <code> index </code> that equals the given instance
   * (ignoring the class attribute), -1 if the instance was not found
   */
  public static int nextOccurenceIgnoreClass(Instances instances, Instance instance, int index) {
    double[] dd = instance.toDoubleArray();
    int classIndex = instances.classIndex();
    int n = instances.numAttributes();
    int numInstances = instances.numInstances();
    int currentIndex = index;
    while(currentIndex < numInstances) {
      Instance tmp = instances.instance(currentIndex);
      int i = 0;
      while(i < n && 
	  (i == classIndex || Utils.eq(dd[i], tmp.value(i)))) {
	i++;
      }
      if (i == n) { 
	return currentIndex;  // found it
      }
      currentIndex++;
    }
    return -1; // not present
  }

  /**
   * Check if all instances have the same class value.
   *
   * @param instances the instances to be checked for homogeneity
   * @return true if the instances have the same class value, false otherwise 
   */
  public static boolean isHomogeneous(Instances instances) {
    Iterator it = new EnumerationIterator(instances.enumerateInstances());
    if (it.hasNext()) {
      double classValue = ((Instance) it.next()).classValue();
      while(it.hasNext()) {
	if (((Instance) it.next()).classValue() != classValue) {
	  return false;
	}
      }
    }
    return true; // empty or all identical
  }

  /**
   * Compares two instances in the data space, this is ignoring the class
   * attribute.  An instance is strictly smaller than another instance
   * if the same holds for the <code> Coordinates </code> based on
   * these instances.
   *
   * @param i1 the first instance
   * @param i2 the second instance
   * @return <code> true </code> if the first instance is strictly smaller
   * than the second instance, <code> false </code> otherwise
   */
  public static boolean strictlySmaller(Instance i1, Instance i2) {
    // XXX implementation can be done faster
    Coordinates c1 = new Coordinates(i1);
    Coordinates c2 = new Coordinates(i2);

    return c1.strictlySmaller(c2);
  }

  /**
   * Compares two instances in the data space, this is, ignoring the class
   * attribute.  An instance is smaller or equal than another instance
   * if the same holds for the <code> Coordinates </code> based on
   * these instances.
   *
   * @param i1 the first instance
   * @param i2 the second instance
   * @return <code> true </code> if the first instance is smaller or equal
   * than the second instance, <code> false </code> otherwise
   */
  public static boolean smallerOrEqual(Instance i1,Instance i2) {
    // XXX implementation can be done faster
    Coordinates c1 = new Coordinates(i1);
    Coordinates c2 = new Coordinates(i2);

    return c1.smallerOrEqual(c2);
  }

  /**
   * Checks if two instances are comparable in the data space, this is 
   * ignoring the class attribute.  Two instances are comparable if the
   * first is smaller or equal than the second, or the other way around.
   *
   * @param i1 the first instance
   * @param i2 the second instance
   * @return <code> true </code> if the given instances are comparable,
   * <code> false </code> otherwise
   * @throws IllegalArgumentException if the two instances don't have the
   * same length
   */
  public static boolean comparable(Instance i1, Instance i2) throws IllegalArgumentException {
    // XXX maybe we should think about using 'equalHeaders' of Instance
    // to obtain a fool proof implementation
    Coordinates c1 = new Coordinates(i1);
    Coordinates c2 = new Coordinates(i2);

    return c1.smallerOrEqual(c2) || c2.smallerOrEqual(c1);
  }

  /**
   * Checks it two instances give rise to doubt.  There is doubt between
   * two instances if their <code> Coordinates </code> are equal, but 
   * their class value is different.
   *
   * @param i1 the first instance
   * @param i2 the second instance
   * @return <code> true </code> if there is doubt between the two 
   * given instances, <code> false </code> otherwise
   */
  public static boolean doubt(Instance i1, Instance i2) {
    // XXX use equalHeaders ?
    if (i1.classValue() == i2.classValue()) {
      return false;
    }
    Coordinates c1 = new Coordinates(i1);
    Coordinates c2 = new Coordinates(i2);

    return c1.equals(c2); 
  }

  /** 
   *  Checks if two instances give rise to reversed preference.  
   *  Two instances give rise to reversed preference in the data space,
   *  if their <code> Coordinates </code> are comparable but different,
   *  and their class values are not related in the same way.
   *  
   *  @param i1 the first instance
   *  @param i2 the second instance
   *  @return <code> true </code> if <code> i1 </code> and <code> i2 </code>
   *  give rise to reversed preference, <code> false </code> otherwise
   *  @throws IllegalArgumentException if the two instances don't have 
   *  the same length
   */
  public static boolean reversedPreference(Instance i1, Instance i2) throws IllegalArgumentException {
    // XXX should the implementation be made fool proof by use of 
    // 'equalHeaders'?  It can also be speeded up I think.

    if (i1.classValue() == i2.classValue()) {
      return false;
    }
    Coordinates c1 = new Coordinates(i1);
    Coordinates c2 = new Coordinates(i2);

    if (i1.classValue() > i2.classValue() && c1.strictlySmaller(c2)) {
      return true;
    }
    if (i2.classValue() > i1.classValue() && c2.strictlySmaller(c1)) {
      return true;
    }

    return false;
  }

  /** 
   *  Checks if the given data set is monotone.  We say that a data set
   *  is monotone if it contains doubt nor reversed preferences.
   *
   *  @param instances the data set to be checked
   *  @return <code> true </code> if the given data set if monotone,
   *  <code> false </code> otherwise
   */  
  public static boolean isMonotone(Instances instances) {
    int n = instances.numInstances();
    for (int i = 0; i < n; i++) {
      Instance i1 = instances.instance(i);
      for (int j = i + 1; j < n; j++) {
	if ( doubt(i1, instances.instance(j)) ||
	    reversedPreference(i1, instances.instance(j))) {
	  return false;
	}
      }
    }
    return true;
  }

  /**
   * Test if a set of instances is quasi monotone.  We say that a set
   * of instances <code> S </code> is quasi monotone with respect to 
   * a set of instances <code> D </code> iff 
   * <code> [x,y] \cap D \neq \emptyset \implies class(x) \leq class(y).
   * </code>  This implies that <code> D </code> itself is monotone.
   *
   * @param ground the instances playing the role of <code> D </code>
   * @param other the instances playing the role of <code> S </code>
   * @return true if the instances are quasi monotone, false otherwise
   */
  public static boolean isQuasiMonotone(Instances ground, Instances other) {
    if (!isMonotone(ground)) {
      return false;
    }
    Iterator it1 = new EnumerationIterator(ground.enumerateInstances());
    while(it1.hasNext()) {
      Instance inst1 = (Instance) it1.next();
      Iterator it2 = new EnumerationIterator(other.enumerateInstances());
      while(it2.hasNext()) {
	Instance inst2 = (Instance) it2.next();
	if (doubt(inst1, inst2) || reversedPreference(inst1, inst2)) {
	  return false;
	}
      }
    }
    return true;
  }

  /** 
   * Gather some statistics regarding reversed preferences. 
   * 
   * @param instances the instances to be examined
   * @return array of length 3; position 0 indicates the number of 
   *  couples that have reversed preference, position 1 the number of 
   *  couples that are comparable, and position 2 the total
   *  number of couples
   * @see #reversedPreference(Instance, Instance)
   */  
  public static int[] nrOfReversedPreferences(Instances instances) {
    int[] stats = new int[3];
    int n = instances.numInstances();
    stats[0] = 0;
    stats[1] = 0;
    // number of couples
    stats[2] = n * (n - 1) / 2; 
    for (int i = 0; i < n; i++) {
      Instance i1 = instances.instance(i);
      for (int j = i + 1; j < n; j++) {
	Instance j1 = instances.instance(j);
	if (comparable(i1, j1)) {
	  stats[1]++;  // comparable
	  if (reversedPreference(i1, j1)) {
	    stats[0]++; // reversed preference
	  }
	}
      }
    }
    return stats;
  }

  /**
   * Find the number of stochastic reversed preferences in the dataset.
   *
   * @param instances the instances to be examined
   * @return an array of integers containing at position
   * <ul>
   * <li> 0: number of different coordinates, this is the size of S_X </li> 
   * <li> 1: number of couples showing reversed preference:<br> 
   * <code> x &lt; y </code> and
   *        <code> not (F_x leqstoch F_y) </code> </li>
   * <li> 2: number of couples having<br>
   *   <code> x &lt; y </code> and <code> F_y leqstoch F_x </code>
   *                                  and <code> F_x neq F_y </code> </li>
   * <li> 3: number of couples that are comparable <br>
   *          <code> |\{ (x,y)\in S_X \times S_x | x &lt; y\}| </code> </li>
   * <li> 4: number of couples in S_X </li>
   * </ul>
   * @throws IllegalArgumentException if there are no instances with
   * a non-missing class value, or if the class is not set
   */
  public static int[] nrStochasticReversedPreference(Instances instances)
    throws IllegalArgumentException {

    if (instances.classIndex() < 0) {
      throw new IllegalArgumentException("Class is not set");
    }

    // copy the dataset 
    Instances data = new Instances(instances);

    // new dataset where examples with missing class value are removed
    data.deleteWithMissingClass();
    if (data.numInstances() == 0) {
      throw new IllegalArgumentException
      ("No instances with a class value!");
    }

    // build the Map for the estimatedDistributions 
    Map distributions = new HashMap(data.numInstances()/2);

    // cycle through all instances 
    Iterator i = 
      new EnumerationIterator(instances.enumerateInstances());

    while (i.hasNext()) { 
      Instance instance = (Instance) i.next();
      Coordinates c = new Coordinates(instance);

      // get DiscreteEstimator from the map
      DiscreteEstimator df = 
	(DiscreteEstimator) distributions.get(c);

      // if no DiscreteEstimator is present in the map, create one 
      if (df == null) {
	df = new DiscreteEstimator(instances.numClasses(), 0);
      }
      df.addValue(instance.classValue(),instance.weight()); // update
      distributions.put(c,df); // put back in map
    }


    // build the map of cumulative distribution functions 
    Map cumulativeDistributions = 
      new HashMap(distributions.size());

    // Cycle trough the map of discrete distributions, and create a new
    // one containing cumulative discrete distributions
    for (Iterator it=distributions.keySet().iterator();
    it.hasNext();) {
      Coordinates c = (Coordinates) it.next();
      DiscreteEstimator df = 
	(DiscreteEstimator) distributions.get(c);
      cumulativeDistributions.put
      (c, new CumulativeDiscreteDistribution(df));
    }
    int[] revPref = new int[5]; 
    revPref[0] = cumulativeDistributions.size();
    Iterator it = cumulativeDistributions.keySet().iterator();
    while (it.hasNext()) {
      Coordinates c1 = (Coordinates) it.next();
      CumulativeDiscreteDistribution cdf1 = 
	(CumulativeDiscreteDistribution) 
	cumulativeDistributions.get(c1);
      Iterator it2 = cumulativeDistributions.keySet().iterator();
      while(it2.hasNext()) {
	Coordinates c2 = (Coordinates) it2.next();
	CumulativeDiscreteDistribution cdf2 = 
	  (CumulativeDiscreteDistribution)
	  cumulativeDistributions.get(c2);
	if (c2.equals(c1)) {
	  continue;
	}

	revPref[4]++;

	if (c1.strictlySmaller(c2) == true) {
	  revPref[3]++; //vergelijkbaar
	  if (cdf1.stochasticDominatedBy(cdf2) == false ) {
	    revPref[1]++;
	    if (cdf2.stochasticDominatedBy(cdf1) == true) {
	      revPref[2]++;
	    }
	  }
	} 
      }
    }
    revPref[4] /= 2; 
    return revPref;
  }

  /**
   * Counts the number of redundant pairs in the sense of OLM.
   * Two instances are redundant if they are comparable and have the same
   * class value.
   * 
   * @param instances the instances to be checked
   * @return the number of redundant pairs in the given set of instances
   */
  public static int nrOfRedundant(Instances instances) {
    int n = instances.numInstances();
    int nrRedundant = 0;
    for (int i = 0; i < n; i++) {
      Instance i1 = instances.instance(i);
      for (int j = i + 1; j < n; j++) {
	Instance j1 = instances.instance(j);
	if (j1.classValue() == i1.classValue() && comparable(i1, j1) ) {
	  nrRedundant++; 
	}
      }
    }

    return nrRedundant;
  }

  /**
   * Calulates the total loss over the <code> instances </code>, 
   * using the trained <code> classifier </code> and the 
   * specified <code> lossFunction. </code>  The instances 
   * should not contain missing values in the class attribute.
   *
   * @param classifier the trained classifier to use
   * @param instances the test instances
   * @param lossFunction the loss function to use
   * @return the total loss of all the instances using the given classifier and loss function
   */
  public static double totalLoss(Classifier classifier, Instances instances, 
      NominalLossFunction lossFunction) {

    double loss = 0;
    int n = instances.numInstances();
    for (int i = 0; i < n; i++) {
      try {
	loss += lossFunction.loss(instances.instance(i).classValue(), 
	    classifier.classifyInstance(instances.instance(i)));
      } catch (Exception e) { 
	// what should we do here ?? 
      }
    }
    return loss;
  }


  /**
   * Classify a set of instances using a given classifier.  The class value
   * of the instances are set.
   *
   * @param instances the instances to be classified
   * @param classifier a built classifier 
   * @throws Exception if one of the instances could no be classified
   */
  public static void classifyInstances(Instances instances, Classifier classifier)
    throws Exception {
    
    Iterator it = new EnumerationIterator(instances.enumerateInstances());
    while(it.hasNext()) {
      Instance instance = (Instance) it.next();
      instance.setClassValue(classifier.classifyInstance(instance));
    }
  }


  /**
   * Calculates the relation (poset) formed by the instances.
   *
   * @param instances the instances for which the poset is to be formed
   * @return a <code> BooleanBitMatrix </code> for which position 
   * <code> bm.get(i,j) == true </code> iff <code> 
   * InstancesUtil.strictlySmaller(instances.instance(i), 
   * instances.instance(j)) == true </code>
   */
  public static BooleanBitMatrix getBitMatrix(Instances instances) {
    int numInstances = instances.numInstances();
    BooleanBitMatrix bm = 
      new BooleanBitMatrix(numInstances, numInstances);
    for (int i = 0; i < numInstances; i++ ) {
      Instance instance1 = instances.instance(i);
      for (int j = 0; j < numInstances; j++) {
	Instance instance2 = instances.instance(j);
	if (InstancesUtil.strictlySmaller(instance1, instance2)) {
	  bm.set(i, j); // arc from instance1 to instance2
	}
      }
    }
    return bm;
  }

  /**
   * Calculatus the number of elements in the closed interval 
   * <code> [low,up]. </code>  If the class index is set, then
   * the class attribute does not play part in the calculations,
   * this is we work in the data space.  The code also works with 
   * numeric attributes, but is primarily intended for ordinal attributes.
   *
   * @param low the lower bound of the interval
   * @param up the upper bound of the interval
   * @return the size of the interval (in floating point format)
   * @throws IllegalArgumentException if the given instances do not
   * constitute an interval.
   */
  public static double numberInInterval(Instance low, Instance up)
    throws IllegalArgumentException {
    
    Coordinates cLow = new Coordinates(low);
    Coordinates cUp = new Coordinates(up);
    if (cLow.smallerOrEqual(cUp) == false) {
      throw new IllegalArgumentException
      ("The given instances are not the bounds of an interval");
    }
    double number = 1;
    int dim = cLow.dimension();
    for (int i = 0; i < dim; i++) {
      number *= (cUp.getValue(i) - cLow.getValue(i) + 1);
    }
    return number;
  }


  /**
   * Calculatutes the number of vectors in the data space that are smaller 
   * or equal than the given instance.
   *
   * @param instance the given instance
   * @return the number of vectors in the data space smaller or equal 
   * than the given instance
   * @throws IllegalArgumentException if there are numeric attributes
   */
  public static double numberOfSmallerVectors(Instance instance) 
    throws IllegalArgumentException {
    
    double[] values = InstancesUtil.toDataDouble(instance);
    double nr = 1;

    for (int i = 0; i < values.length; i++) {
      if (instance.attribute(i).isNumeric()) {
	throw new IllegalArgumentException
	("Numeric attributes are not supported"); 
      }
      nr *= (values[i] + 1);
    }

    return nr;
  }

  /**
   * Calculatutes the number of vectors in the data space that are 
   * greater or equal than the given instance.
   *
   * @param instance the given instance
   * @return the number of vectors in the data space greater of equal
   * than the given instance
   * @throws IllegalArgumentException if there are numeric attributes
   */
  public static double numberOfGreaterVectors(Instance instance) 
    throws IllegalArgumentException {
    
    double[] values = InstancesUtil.toDataDouble(instance);
    double nr = 1;

    for (int i = 0; i < values.length; i++) {
      if (instance.attribute(i).isNumeric()) {
	throw new IllegalArgumentException
	("Numeric attributes are not supported"); 
      }
      nr *= (instance.attribute(i).numValues() - values[i]);
    }

    return nr;
  }

  /**
   * Write the instances in ARFF-format to the indicated 
   * <code> BufferedWriter </code>.
   * @param instances the instances to write
   * @param file the <code> BufferedWriter </code> to write to
   * @throws IOException if something goes wrong while writing the instances
   */
  public static void write(Instances instances, BufferedWriter file)
    throws IOException{
    
    file.write(instances.toString()); // XXX can probably be done better 
  }


  /**
   * Return a histogram of the values for the specified attribute.
   *
   * @param instances the instances
   * @param attributeIndex the attribute to consider
   * @return a <code> DiscreteEstimator </code> where the <code>i</code>th
   * @throws IllegalArgumentException if the attribute at the specified 
   * index is numeric
   */
  public static DiscreteEstimator countValues(Instances instances, int attributeIndex) 
    throws IllegalArgumentException{
    
    int numValues = instances.attribute(attributeIndex).numValues();
    if (numValues == 0) {
      throw new IllegalArgumentException
      ("Can't create histogram for numeric attribute");
    }

    DiscreteEstimator de = new DiscreteEstimator(numValues, false);
    Iterator it = new EnumerationIterator(instances.enumerateInstances());
    while (it.hasNext()) {
      Instance instance = (Instance) it.next();
      if (!instance.isMissing(attributeIndex)) {
	de.addValue(instance.value(attributeIndex), instance.weight());
      }
    }
    return de;
  }

  /**
   * Create, without replacement, a random subsample of the given size 
   * from the given instances.
   *
   * @param instances the instances to sample from
   * @param size the requested size of the sample
   * @param random the random generator to use
   * @return a sample of the requested size, drawn from the given
   * instances without replacement
   * @throws IllegalArgumentException if the size exceeds the number
   * of instances
   */
  public static Instances sampleWithoutReplacement(
      Instances instances, int size, Random random) {
    
    if (size > instances.numInstances()) {
      throw new IllegalArgumentException
      ("Size of requested sample exceeds number of instances");
    }

    int numInstances = instances.numInstances();
    int[] indices = new int[instances.numInstances()];
    for (int i = 0; i < numInstances; i++) {
      indices[i] = i;
    }

    Instances sample = new Instances(instances, size);
    int index;
    for (int i = 0; i < size; i++) {
      index = random.nextInt(numInstances--);
      sample.add(instances.instance(indices[index]));
      swap(indices, index, numInstances);
    }
    return sample;
  }

  /**
   * Swaps two elements of the given array.
   *
   * @param aa the array 
   * @param i the index of the first element
   * @param j the index of the second element
   */
  final private static void swap(int[] aa, int i, int j) {
    int tmp = aa[i];
    aa[i] = aa[j];
    aa[j] = tmp;
  }
  /**
   * Generates a random sample of instances.  Each attribute must be nominal, and the 
   * class labels are not set.
   * 
   * @param headerInfo Instances whose header information is used to determine how the 
   * set of returned instances will look
   * @param numberOfExamples the desired size of the returned set
   * @param random the random number generator to use
   * @return a set of Instances containing the random sample.
   * @throws IllegalArgumentException if numeric attributes are given
   */
  public static Instances generateRandomSample(
      Instances headerInfo, int numberOfExamples, Random random) 
    throws IllegalArgumentException {
    
    int n = headerInfo.numAttributes();
    double[] info = new double[n];
    int classIndex = headerInfo.classIndex();
    for (int i = 0; i < n; i++) {
      info[i] = headerInfo.attribute(i).numValues();
      if (i != classIndex && info[i] == 0) {
	throw new IllegalArgumentException
	("Numeric attributes are currently not supported");
      }
    }
    Instances sample = new Instances(headerInfo, numberOfExamples);
    sample.setRelationName(headerInfo.relationName() + 
	".random.sample.of." + numberOfExamples);
    for (int i = 0; i < numberOfExamples; i++) {
      sample.add(randomSample(info, classIndex, random));
    }
    return sample;
  }
  /**
   * Generates a random instance.
   * 
   * @param info array that gives for each attribute the number of possible values
   * @param classIndex the index of the class attribute
   * @param random the random number generator used
   * @return a random instance
   */
  private static Instance randomSample(double[] info, 
      int classIndex, Random random) {
    
    double[] attValues = new double[info.length];
    for (int i = 0; i < attValues.length; i++) {
      if (i != classIndex) {
	attValues[i] = random.nextInt( (int) info[i]); 
      }
    }
    return new Instance(1, attValues);
  }



  /**
   * Returns an array containing the attribute values (in internal floating 
   * point format) of the given instance in data space, this is, the class 
   * attribute (if any) is removed.
   *
   * @param instance the instance to get the attribute values from
   * @return array of doubles containing the attribute values
   */
  public static double[] toDataDouble(Instance instance) {
    double[] vector = null;
    int classIndex = instance.classIndex();
    if(classIndex >= 0) {
      vector = new double[instance.numAttributes() - 1];
    } else {
      vector = new double[instance.numAttributes()];
    }
    int index = 0;
    for (int i = 0; i < instance.numAttributes(); i++) {
      if(i != classIndex) {
	vector[index++] = instance.value(i);
      }
    }
    return vector;
  }

  /**
   * Computes the minimal extension for a given instance.
   *
   * @param instances the set of instances 
   * @param instance the instance for which the minimal extension is to be
   * calculated
   * @return the value of the minimal extension, in internal floating point
   * format
   */
  public static double minimalExtension(Instances instances, Instance instance) {
    return minimalExtension(instances, instance, 0);
  }

  /**
   * Computes the minimal extension of a given instance, but the 
   * minimal value returned is <code> minValue. </code>  This method
   * may have its applications when the training set is divided into
   * multiple Instances objects.
   *
   * @param instances the set of instances
   * @param instance the instance for which the minimal extension is to 
   * be calculated
   * @param minValue a double indicating the minimal value that should 
   * be returned
   * @return the label of the minimal extension, in internal floating point format
   */
  public static double minimalExtension(
      Instances instances, Instance instance, double minValue) {
    
    double value = minValue;

    Iterator it = 
      new EnumerationIterator(instances.enumerateInstances());
    while(it.hasNext()) {
      Instance tmp = (Instance) it.next();
      if (tmp.classValue() > value 
	  && InstancesUtil.smallerOrEqual(tmp, instance) ) {
	value = tmp.classValue();
      }
    }
    return value;
  }

  /**
   * Computes the maximal extension for a given instance.
   *
   * @param instances the set of instances 
   * @param instance the instance for which the minimal extension is to be
   * calculated
   * @return the value of the minimal extension, in internal floating point
   * format
   */
  public static double maximalExtension(Instances instances, Instance instance) {
    return maximalExtension(instances, instance, instances.numClasses() - 1);
  }

  /**
   * Computes the maximal extension of a given instance, but the 
   * maximal value returned is <code> maxValue. </code>  This method
   * may have its applications when the training set is divided into
   * multiple Instances objects.
   *
   * @param instances the set of instances
   * @param instance the instance for which the maximal extension is to 
   * be calculated
   * @param maxValue a double indicating the maximal value that should 
   * be returned
   * @return the value of the minimal extension, in internal floating point
   * format
   */
  public static double maximalExtension(
      Instances instances, Instance instance, double maxValue) {
    
    double value = maxValue;

    Iterator it = 
      new EnumerationIterator(instances.enumerateInstances());
    while(it.hasNext()) {
      Instance tmp = (Instance) it.next();
      if (tmp.classValue() < value 
	  && InstancesUtil.smallerOrEqual(instance, tmp) ) {
	value = tmp.classValue();
      }
    }
    return value;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.3 $");
  }
}
