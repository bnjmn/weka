/*
 *    DecisionTable.java
 *    Copyright (C) 1999 Mark Hall
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

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.*;
import weka.classifiers.j48.*;

/**
 * Class for building and using a simple decision table majority classifier.<p>
 *
 * Valid options are: <br>
 *
 * -O <br>
 * Select and order attributes by forward hill climbing
 * rather than best first search (default). <p>
 *
 * -S num <br>
 * Number of fully expanded non improving subsets to consider
 * before terminating a best first search. Use in conjunction with -B. 
 * (Default = 5) <p>
 *
 * -C <br>
 * Use C45 style error estimate to evaluate features (nominal class 
 * only). <p>
 *
 * -P num <br>
 * [0.0 - 1.0] Set confidence factor for C45 error estimate.
 * Use in conjunction with -C. (Default =  0.65) <p>
 *
 * -M <br>
 * Use MDL to evaluate features (nominal class only). <p>
 *
 * -X num <br>
 * Use cross validation to evaluate features. Use number of folds = 1 for
 * leave one out CV. (Default = leave one out CV) <p>
 *
 * -D <br>
 * Make binary attributes when discretising. <p>
 * 
 * -I <br>
 * Use nearest neighbour instead of global table majority. <p>
 * 
 * -R <br>
 * Display decision table rules. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 - 19 Nov 1998 - Initial version (Mark)
 */
public class DecisionTable extends DistributionClassifier 
  implements OptionHandler, WeightedInstancesHandler {

  /** The hashtable used to hold training instances */
  private Hashtable entries;

  /** Holds the final feature set */
  private int [] decisionFeatures;

  /** discrestisation filter */
  private DiscretizeFilter disTransform;
  private NominalToBinaryFilter bTransform;

  /** Filter used to remove columns discarded by feature selection */
  private DeleteFilter delTransform;

  /**
   * IB1 classifier
   * used to classify non matiching instances rather than assigning
   * majority class
   */
  private IBk ibk;

  /** Holds the training instances */
  private Instances theInstances;

  private int numAttributes;
  private int numInstances;
  private boolean classIsNominal;

  private boolean useBinaryAtts;
  private boolean debug;
  private boolean useIBK;
  private boolean displayRules;

  private boolean evalC45;
  private boolean evalCV;
  private boolean evalMDL;

  /**
   * maximum number of fully expanded non improving subsets
   * for a best first search.
   */
  private int maxStale;

  /** pruning confidence when using c45 error estimate */
  private double c45PF;

  /** Number of folds for cross validating feature sets */
  private int CVFolds;

  /** Use best first search, if false use hill climbing */
  private boolean searchBF;

  /** Random numbers for use in cross validation */
  private Random rr;

  /** Holds the majority class */
  private double majority;


  /**
   * Class for a node in a linked list. Used in best first search.
   * @version 1.0
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   */
  public class Link2 {

    BitSet group;
    double merit;
   
    // Constructor
    public Link2 (BitSet gr, double mer) {

      group = (BitSet)gr.clone();
      merit = mer;
    }
  
    public BitSet getGroup() {

      return group;
    }

    public String toString() {
      return ("Node: " + group.toString() + "  " + merit);
    }
  }
  
  /**
   * Class for handling a linked list. Used in best first search.
   * Extends the Vector class.
   *
   * @version 1.0
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   */
  public class LinkedList2 extends FastVector {

    // ================
    // Public methods
    // ================

    /**
     * Removes an element (Link) at a specific index from the list.
     *
     * @param index the index of the element to be removed.
     */
    public void removeLinkAt(int index) throws Exception {

      if ((index >= 0) && (index < size())) {
	removeElementAt(index);
      } else {
	throw new Exception("index out of range (removeLinkAt)");
      }
    }

    /**
     * Returns the element (Link) at a specific index from the list.
     *
     * @param index the index of the element to be returned.
     */
    public Link2 getLinkAt(int index) throws Exception {

      if (size()==0) {
	throw new Exception("List is empty (getLinkAt)");
      } else if ((index >= 0) && (index < size())) {
	return ((Link2)(elementAt(index)));
      } else {
	throw new Exception("index out of range (getLinkAt)");
      }
    }

    /**
     * Adds an element (Link) to the list.
     *
     * @param gr the feature set specification
     * @param mer the "merit" of this feature set
     */
    public void addToList(BitSet gr, double mer) {

      Link2 newL = new Link2(gr, mer);
	
      if (size()==0) {
	addElement(newL);
      } else if (mer > ((Link2)(firstElement())).merit) {
	insertElementAt(newL,0);
      } else {
	int i = 0;
	int size = size();
	boolean done = false;
	while ((!done) && (i < size)) {
	  if (mer > ((Link2)(elementAt(i))).merit) {
	    insertElementAt(newL,i);
	    done = true;
	  } else if (i == size - 1) {
	    addElement(newL);
	    done = true;
	  } else {
	    i++;
	  }
	}
      }
    }
  }

  /**
   * Class providing keys to the hash table
   *
   * @version 1.0
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   */
  public class hashKey {
  
    /** Array of attribute values for an instance */
    private double [] attributes;

    /**
     * Array boolean. Set true for an index if the corresponding
     * attribute value is missing.
     */
    private boolean [] missing;
    
    private String [] values;

    private int key;

    /**
     * Constructor for a hashKey
     *
     * @param t an instance from which to generate a key
     * @param numAtts the number of attributes
     */
    public hashKey(Instance t, int numAtts) throws Exception {

      int cindex = t.classIndex();

      key = -999;

      attributes = new double [numAtts];
      missing = new boolean [numAtts];

      for (int i = 0; i < numAtts; i++) {
	if (i == cindex) {
	  missing[i] = true;
	} else {
	  if ((missing[i] = t.isMissing(i)) == false) {
	    attributes[i] = t.value(i);
	  }
	}
      }
    }

    /**
     * Convert a hash entry to a string
     *
     * @param t the set of instances
     */
    public String toString(Instances t) {

      int i;
      int cindex = t.classIndex();
      StringBuffer text = new StringBuffer();
      
      for (i = 0; i < attributes.length; i++) {
	if (i != cindex) {
	  int l = t.attribute(i).name().length();
	  if (missing[i]) {
	    text.append("?  ");
	    for (int j = 0; j < (l - 1); j++) {
	      text.append(" ");
	    }
	  } else {
	    String ss = t.attribute(i).value((int)attributes[i]);
	    StringBuffer sb = new StringBuffer(ss);
	    if (l < ss.length()) {
	      sb.setLength(l);
	    } else {
	      for (int j = 0; j < (l - ss.length()); j++) {
		sb.append(" ");
	      }
	    }
	    text.append(sb.toString() + "  ");
	  }
	}
      }

      return text.toString();
    }

    /**
     * Constructor for a hashKey
     *
     * @param t an array of feature values
     */
    public hashKey(double [] t)
    {
      int l = t.length;

      key = -999;

      attributes = new double [l];
      missing = new boolean [l];

      for (int i = 0; i < l; i++) {
	if (t[i] == Double.MAX_VALUE) {
	  missing[i] = true;
	} else {
	  missing[i] = false;
	  attributes[i] = t[i];
	}
      }
    }
    
    /**
     * Calculates a hash code
     *
     * @return the hash code as an integer
     */
    public int hashCode() {

      int hv = 0;

      if (key != -999) {
	return key;
      }

      for (int i = 0; i < attributes.length; i++) {
	if (missing[i]) {
	  hv += i * 13;
	} else {
	  hv += i * 5 * (attributes[i]+1);
	}
      }

      if (key == -999) {
	key = hv;
      }
      return hv;
    }

    /**
     * Tests if two instances are equal
     *
     * @param b a key to compare with
     */
    public boolean equals(Object b) {

      boolean ok = true;
      boolean l;
      if (b instanceof hashKey) {
	hashKey n = (hashKey)b;
	for (int i = 0; i < attributes.length; i++) {
	  l = n.missing[i];
	  if (missing[i] || l) {
	    if ((missing[i] && !l) || (!missing[i] && l)) {
	      ok = false;
	      break;
	    }
	  } else {
	    if (attributes[i] != n.attributes[i]) {
	      ok = false;
	      break;
	    }
	  }
	}
      } else {
	return false;
      }

      return ok;
    }

    /**
     * Prints the hash code
     */
    public void print_hash_code() {

      System.out.println("Hash val: " + hashCode());
    }
  }

  // *******************************************************************


  // ==============
  // Private methods
  // ==============

  /**
   * Inserts an instance into the hash table
   *
   * @param inst instance to be inserted
   * @exception Exception if the instance can't be inserted
   */
  private void insertIntoTable(Instance inst, double [] instA)
    throws Exception {

    double [] tempClassDist2;
    double [] newDist;
    hashKey thekey;

    if (instA != null) {
      thekey = new hashKey(instA);
    } else {
      thekey = new hashKey(inst, inst.numAttributes());
    }
      
    // see if this one is already in the table
    tempClassDist2 = (double []) entries.get(thekey);
    if (tempClassDist2 == null) {
      if (classIsNominal) {
	newDist = new double [theInstances.classAttribute().numValues()];
	
	newDist[(int)inst.classValue()] = inst.weight();
	// add to the table
	entries.put(thekey, newDist);
      } else {
	newDist = new double [2];
	newDist[0] = inst.classValue() * inst.weight();
	newDist[1] = inst.weight();
	// add to the table
	entries.put(thekey, newDist);
      }
    } else { // update the distribution for this instance
      if (classIsNominal) {
	tempClassDist2[(int)inst.classValue()]+=inst.weight();
	// update the table
	entries.put(thekey, tempClassDist2);
      } else {
	tempClassDist2[0] += (inst.classValue() * inst.weight());
	tempClassDist2[1] += inst.weight();
	// update the table
	entries.put(thekey, tempClassDist2);
      }
    }
  }


  /**
   * Classifies an instance for internal leave one out cross validation
   * of feature sets
   *
   * @param instance instance to be "left out" and classified
   * @param instA feature values of the selected features for the instance
   * @return the classification of the instance
   */
  double classifyInstanceLeaveOneOut(Instance instance, double [] instA)
    throws Exception {

    hashKey thekey;
    double [] tempDist;
    double [] normDist;

    thekey = new hashKey(instA);
      
    if (classIsNominal) {
      // if this one is not in the table
      if ((tempDist = (double [])entries.get(thekey)) == null) {
	System.out.println("Shouldnt get here!");
	System.exit(1);
      } else {
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist, 0, normDist, 0, tempDist.length);

	normDist[(int)instance.classValue()] -= instance.weight();
	// update the table
	// first check to see if the class counts are all zero now
	boolean ok = false;
	for (int i = 0; i < normDist.length; i++) {
	  if (!Utils.eq(normDist[i], 0.0)) {
	    ok = true;
	    break;
	  }
	}
	if (ok) {
	  Utils.normalize(normDist);
	  return Utils.maxIndex(normDist);
	} else {
	  return majority;
	}
      }
      return Utils.maxIndex(tempDist);
    } else {
      // see if this one is already in the table
      if ((tempDist = (double[])entries.get(thekey)) != null) {
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist,0,normDist,0,tempDist.length);

	normDist[0] -= (instance.classValue() * instance.weight());
	normDist[1] -= instance.weight();

	if (Utils.eq(normDist[1],0.0)) {
	  return majority;
	} else {
	  return (normDist[0] / normDist[1]);
	}
      } else {
	System.out.println("Shouldnt get here");
	System.exit(1);
      }
    }
    
    // shouldn't get here 
    return 0.0;
  }


  /**
   * Calculates the accuracy on a test fold for internal cross validation
   * of feature sets
   *
   * @param fold set of instances to be "left out" and classified
   * @param fs currently selected feature set
   * @return the accuracy for the fold
   */
  double classifyFoldCV(Instances fold, int [] fs) throws Exception {

    int i;
    int ruleCount = 0;
    int numFold = fold.numInstances();
    int numCl = theInstances.classAttribute().numValues();
    double [][] class_distribs = new double [numFold][numCl];
    double [] instA = new double [fs.length];
    double [] normDist;
    hashKey thekey;
    double acc = 0.0;
    int classI = theInstances.classIndex();
    Instance inst;

    if (classIsNominal) {
      normDist = new double [numCl];
    } else {
      normDist = new double [2];
    }

    // first *remove* instances
    for (i = 0; i < numFold; i++) {
      inst = fold.instance(i);
      
      for (int j = 0; j < fs.length; j++) {
	if (fs[j] == classI) {
	  instA[j] = Double.MAX_VALUE; // missing for the class
	} else if (inst.isMissing(fs[j])) {
	  instA[j] = Double.MAX_VALUE;
	} else {
	  instA[j] = inst.value(fs[j]);
	}
      }
      thekey = new hashKey(instA);

      if ((class_distribs[i] = (double [])entries.get(thekey)) == null) {
	System.out.println("Shouldnt get here!");
	System.exit(1);
      } else {
	if (classIsNominal) {
	  class_distribs[i][(int)inst.classValue()] -= inst.weight();
	} else {
	  class_distribs[i][0] -= (inst.classValue() * inst.weight());
	  class_distribs[i][1] -= inst.weight();
	}
	  
	ruleCount++;
      }
    }
    
    // now classify instances
    for (i = 0; i < numFold; i++) {
      inst = fold.instance(i);
      System.arraycopy(class_distribs[i], 0, normDist, 0, normDist.length);
      
      if (classIsNominal) {
	boolean ok = false;
	for (int j = 0; j < normDist.length; j++) {
	  if (!Utils.eq(normDist[j], 0.0)) {
	    ok = true;
	    break;
	  }
	}
	
	if (ok) {
	  Utils.normalize(normDist);
	  if (Utils.maxIndex(normDist) == inst.classValue()) {
	    acc += inst.weight();
	  }
	} else {
	  if (inst.classValue() == majority) {
	    acc += inst.weight();
	  }
	}
      } else {
	if (Utils.eq(normDist[1],0.0)) {
	  acc += ((inst.weight() * (majority - inst.classValue())) * 
		  (inst.weight() * (majority - inst.classValue())));
	} else {
	  double t = (normDist[0] / normDist[1]);
	  acc += ((inst.weight() * (t - inst.classValue())) * 
		  (inst.weight() * (t - inst.classValue())));
	}
      }
    }

    // now re-insert instances
    for (i = 0; i < numFold; i++) {
      inst = fold.instance(i);
      if (classIsNominal) {
	class_distribs[i][(int)inst.classValue()] += inst.weight();
      } else {
	class_distribs[i][0] += (inst.classValue() * inst.weight());
	class_distribs[i][1] += inst.weight();
      }
    }

    return acc;
  }


  /**
   * Evaluates a feature subset either by cross validation, c45 error
   * estimates, or an MDL criterion.
   *
   * @param feature_set the subset to be evaluated
   * @param num_atts the number of attributes in the subset
   * @param evC45 evaluate by c45 error estimate
   * @param evCV evaluate by cross validation
   * @param evMDL evaluate by MDL
   * @return the estimated accuracy or mdl score
   * @exception Exception if subset can't be evaluated
   */
  private double estimateAccuracy(BitSet feature_set, 
				  int num_atts,
				  boolean evC45,
				  boolean evCV,
				  boolean evMDL) throws Exception {

    int i;
    Instances newInstances;
    int [] fs = new int [num_atts];
    double acc = 0.0;
    double [][] evalArray;
    double [] instA = new double [num_atts];
    int classI = theInstances.classIndex();
    
    int index = 0;
    for (i = 0; i < numAttributes; i++) {
      if (feature_set.get(i)) {
	fs[index++] = i;
      }
    }
    
    // create new hash table
    entries = new Hashtable((int)(theInstances.numInstances() * 1.5));

    // insert instances into the hash table
    for (i = 0; i < numInstances; i++) {
      Instance inst = theInstances.instance(i);
      
      for (int j = 0; j < fs.length; j++) {
	if (fs[j] == classI) {
	  instA[j] = Double.MAX_VALUE; // missing for the class
	} else if (inst.isMissing(fs[j])) {
	  instA[j] = Double.MAX_VALUE;
	} else {
	  instA[j] = inst.value(fs[j]);
	}
      }
      insertIntoTable(inst, instA);
    }
    
    if (evC45) {
      return c45Pruning();
    } else if (evMDL) {
      if (classIsNominal) {
	evalArray = new double [entries.size()][theInstances
					       .classAttribute().numValues()];
	// set up this array
	i = 0;
	Enumeration e = entries.elements();
	while (e.hasMoreElements()) {
	  evalArray[i++] = (double [])e.nextElement();
	}
	acc = MDLNominal(evalArray, index);
	return acc;
      } else {
	throw new Exception("MDL criterion requires nominal class!");
      }
    }
    else { // cross validation
      if (CVFolds == 1) {
	// calculate leave one out error
	for (i = 0; i < numInstances; i++) {
	  Instance inst = theInstances.instance(i);
	  
	  for (int j=0;j<fs.length;j++) {
	    if (fs[j] == classI) {
	      instA[j] = Double.MAX_VALUE; // missing for the class
	    } else if (inst.isMissing(fs[j])) {
	      instA[j] = Double.MAX_VALUE;
	    } else {
	      instA[j] = inst.value(fs[j]);
	    }
	  }
	  double t = classifyInstanceLeaveOneOut(inst, instA);
	  
	  if (classIsNominal) {
	    if (t == inst.classValue()) {
	      acc += inst.weight();
	    }
	  } else {
	    acc += ((inst.weight() * (t - inst.classValue())) * 
		    (inst.weight() * (t - inst.classValue())));
	  }
	  // weight_sum += inst.weight();
	}
      } else {
	theInstances.randomize(rr);
	theInstances.stratify(CVFolds);
	// calculate 10 fold cross validation error
	for (i = 0; i < CVFolds; i++) {
	  Instances insts = theInstances.testCV(CVFolds, i);
	  
	  acc += classifyFoldCV(insts, fs);
	}
      }
    }
    if (classIsNominal) {
      return (acc / theInstances.sumOfWeights());
    } else {
      return -(Math.sqrt(acc / theInstances.sumOfWeights()));   
    }
  }
  
  private double LogStar(double n) {

    if (n <= 1.0) {
      return 0;
    }
    return LogStar(Utils.log2(n)) + Utils.log2(n);
  }

  private double universalPrior(int n, int b) {

    double l_star;
    double logc = 0;

    if (n == 0) {
      return 0;
    }

    for (int i = 1; i <= b; i++) {
      logc += Math.pow(2, -LogStar(i));
    }
    logc = (Utils.log2(logc));

    l_star = LogStar(n);
    
    return (l_star + logc);
  }

  /**
   * Calculates an MDL-like score for a feature subset
   *
   * @param tA an array of disjuncts and class distributions
   * @param numSelected the number of attributes selected
   * @return the mdl score
   * @exception Exception if the instance can't be inserted
   */
  private double MDLNominal(double [][] tA, int numSelected) throws Exception {

    double mdl = 0.0;
    int non_zero;
    double distAfter = 0, instAfter = 0, after;
    int numClassesTotal = theInstances.classAttribute().numValues();
    double sum;

    // Encode distributions and instances after split.
    /* for (int i=0;i<numClassesTotal;i++)
       {
       int nDis = 0;
       sum = 0;
       for (int j=0;j<tA.length;j++)
       {
       if (tA[j][i] > 0) {
       nDis++;
       sum += tA[j][i];
       }
       }

       if (sum > 0) {
       distAfter += Utils.log2(tA.length); //universalPrior(nDis, tA.length);
       distAfter += SpecialFunctions.log2Binomial(tA.length, nDis);
       distAfter += SpecialFunctions.log2Binomial(sum + nDis - 1, nDis - 1);
       } 
       } */

    for (int i = 0; i < tA.length; i++) {
      sum = Utils.sum(tA[i]);
      non_zero = 0;
      for (int z = 0; z < tA[i].length; z++) {
	if (tA[i][z] > 0.0) {
	  non_zero++;
	}
      }
	  
      distAfter += Utils.log2(non_zero);
      distAfter += SpecialFunctions.log2Binomial(numClassesTotal, non_zero);
      
      distAfter += SpecialFunctions.log2Binomial(sum + non_zero - 1,
						 non_zero - 1);
      instAfter += SpecialFunctions.log2Multinomial(sum, tA[i]);
    }

    // Coding cost after split

    after = distAfter + instAfter;
    //    System.out.println("instAfter: "+instAfter);
  
    mdl = SpecialFunctions.log2Binomial(numAttributes - 1, numSelected - 1);

    mdl += after;

    if (numSelected > 1) {
      mdl += Utils.log2(numSelected - 1);
      // mdl += universalPrior(numSelected-1, numAttributes-1);
    }

    if (mdl == 0) {
      mdl = Double.MAX_VALUE;
    }
    return -mdl;
  }

  /**
   * Calculates an MDL score for a feature subset
   * uses Kononenko's MDL measure
   *
   * @param tA an array of disjuncts and class distributions
   * @param numSelected the number of attributes selected
   * @return the mdl score
   * @exception Exception if the instance can't be inserted
   */
  private double MDLNominalKon(double [][] tA, int numSelected)
    throws Exception {

    double mdl = 0.0;
    int non_zero;
    double distAfter = 0, instAfter = 0, after;
    int numClassesTotal = theInstances.classAttribute().numValues();
    double sum;

    for (int i = 0; i < tA.length; i++) {
      sum = Utils.sum(tA[i]);
      /* non_zero = 0;
	 for (int z=0;z<tA[i].length;z++)
	 if (tA[i][z] > 0.0)
	 non_zero++; */
	  
      // distAfter += Utils.log2(non_zero);
      // distAfter += SpecialFunctions.log2Binomial(numClassesTotal, non_zero);
      
      distAfter += SpecialFunctions.log2Binomial(sum + numClassesTotal - 1,
						 numClassesTotal - 1);
      instAfter += SpecialFunctions.log2Multinomial(sum, tA[i]);
    }

    // Coding cost after split

    after = distAfter + instAfter;
    //    System.out.println("instAfter: "+instAfter);
  
    mdl = SpecialFunctions.log2Binomial(numAttributes-1, numSelected-1);

    mdl += after;

    if (numSelected > 1) {
      mdl += Utils.log2(numSelected - 1);
    // mdl += universalPrior(numSelected-1, numAttributes-1);
    }
    if (mdl == 0) {
      mdl = Double.MAX_VALUE;
    }
    return -mdl;
  }

  
  /**
   * Returns a String representation of a feature subset
   *
   * @param sub BitSet representation of a subset
   * @return String containing subset
   */
  private String printSub(BitSet sub) {

    int i;

    String s = "";
	    
    for (int jj = 0;jj < numAttributes; jj++) {
      if (sub.get(jj)) {
	s += " " + (jj + 1);
      }
    }

    return s;
  }


  /**
   * Calculates the log of a double
   *
   * @param num the number
   * @return the log of the number
   */
  private static double lnFunc(double num) {

    // Constant hard coded for efficiency reasons
    
    if (num < 1e-6) {
      return 0;
    } else {
      return num * Math.log(num);
    }
  }


  /**
   * Calculates the c45 style adjusted error for a set of rules
   *
   * @return the adjusted accuracy
   */
  private double c45Pruning() {

    double err = 0.0;
    double sum, re;
    double [] dd;
    double sum_sum = 0.0;
    Enumeration e = entries.elements();
    while (e.hasMoreElements()) {
      dd = (double [])e.nextElement();
      sum = Utils.sum(dd);
      sum_sum += sum;
      re = (sum - dd[Utils.maxIndex(dd)]);
      
      err += re + (Stats.addErrs(sum, re, (float)c45PF));
    }
      
    return (sum_sum - err) / sum_sum;
  }

  /**
   * Calculates the gain ratio for a set of rules
   *
   * @param matrix class distributions of the rules. Each row is a rule
   * @param gain will hold the information gain for the rules
   * @return the gain ratio
   */
  private double adjustedMutualInfo(double [][] matrix, double [] gain) {

    double preSplit = 0, postSplit = 0, splitEnt = 0,
      sumForRow, sumForColumn, total = 0, infoGain;

    for (int i = 0; i < matrix[0].length; i++) {
      sumForColumn = 0;
      for (int j = 0; j < matrix.length; j++) {
        sumForColumn += matrix[j][i];
      }
      preSplit += lnFunc(sumForColumn);
      total += sumForColumn;
    }
    preSplit -= lnFunc(total);

    // Compute entropy after split and split entropy (gain ratio)

    for (int i = 0; i < matrix.length; i++) {
      sumForRow = 0;
      for (int j = 0; j < matrix[0].length; j++) {
        postSplit += lnFunc(matrix[i][j]);
        sumForRow += matrix[i][j];
      }
      splitEnt += lnFunc(sumForRow);
    }

    postSplit -= splitEnt;
    splitEnt -= lnFunc(total);

    // for adjusted mutual info
    // splitEnt = Math.log(matrix.length);
    // splitEnt = matrix.length * (lnFunc(total / matrix.length));

    infoGain = preSplit - postSplit;
    gain[0] = -infoGain;

    if (Utils.eq(splitEnt, 0)) {
      return 0;
    }

    return infoGain / splitEnt;
  }


  /**
   * Calculates the merit of a feature subset (set of rules)
   * by cv, mdl, c45 error. This function is called by orderByHillClimb.
   * OrderByHillClimb requires gain as a cutoff to stop ordering when
   * evaluation is by mdl or c45 error.
   *
   * @param feature_set the selected features
   * @num_atts the number of attributes selected
   * @gain will store the information gain if ordering using mdl or c45 error
   * @return the adjusted accuracy
   */
  private double orderingCriteria(BitSet feature_set, 
				  int num_atts, 
				  double [] gain)
    throws Exception {

    int i, j;
    int [] fs = new int [num_atts];
    double [] instA = new double [num_atts];
    int classI = theInstances.classIndex();
    double adjustedMutual = 0.0;
    
    double [][] matrix;

    // if class is numeric then evaluate by CV
    if (!classIsNominal || evalCV) {
      return estimateAccuracy(feature_set, num_atts, evalC45, 
			      evalCV, evalMDL);
    }
    int index = 0;
    for (i = 0; i < numAttributes; i++) {
      if (feature_set.get(i)) {
	fs[index++] = i;
      }
    }

    // create new hash table
    entries = new Hashtable((int)(theInstances.numInstances()));

    // insert instances into the hash table
    for (i = 0; i < numInstances; i++) {
      Instance inst = theInstances.instance(i);

      for (j = 0; j < fs.length; j++) {
	if (fs[j] == classI) {
	  instA[j] = Double.MAX_VALUE; // missing for the class
	} else if (inst.isMissing(fs[j])) {
	  instA[j] = Double.MAX_VALUE;
	} else {
	  instA[j] = inst.value(fs[j]);
	}
      }
      insertIntoTable(inst, instA);
    }

    matrix = new double [entries.size()][theInstances.classAttribute()
					.numValues()];

    // set up this array
    i = 0;
    Enumeration e = entries.elements();
    while (e.hasMoreElements()) {
      matrix[i++] = (double [])e.nextElement();
    }

    // get the value for info gain. This is used as a cutoff
    // when ordering by mdl or c45 error. An addition of an
    // attribute must increase the gain over the previous
    // level in order to be acceptable
    adjustedMutual = adjustedMutualInfo(matrix, gain);

    if (evalC45) {
      return c45Pruning();
    } else if (evalMDL) {
      return (MDLNominalKon(matrix, num_atts));
    } else {
      return adjustedMutual;
    }
  }

  /**
   * Orders attributes by hillclimbing. CV is used to prune the ordering
   */
  private void orderByHillClimb() throws Exception {

    int i, j, classI, size, count;
    double merit;
    double best_merit = -Double.MAX_VALUE;
    double temp_merit;
    int [] orderedFs = new int [numAttributes];
    BitSet best_group = new BitSet(numAttributes);
    boolean ok = true;
    boolean ok2 = false;
    double [] gain = new double[2];
    double best_gain = 0.0;
    double temp_gain = 0;
    double [][] level = new double [3][numAttributes];
    
    for (i = 0; i < numAttributes; i++) {
      orderedFs[i] = -1;
    }
    // set class
    classI = theInstances.classIndex();
    best_group.set(classI);
    size = 1;
    orderedFs[0] = classI;

    while (ok) {
      ok = false;
      if (!evalCV) {
	best_merit = -Double.MAX_VALUE;
      }

      BitSet curr_group = (BitSet)best_group.clone();
      count = 0;
      for (i = 0; i < numAttributes; i++) {
	BitSet temp_group = (BitSet)curr_group.clone();

	if ((i != classI) && (!curr_group.get(i))) {
	  temp_group.set(i);

	  merit = orderingCriteria(temp_group, size+1, gain);
	   
	  if (merit > best_merit) {
	    if ((gain[0] > 0) || (evalCV) || (!classIsNominal)) {
	      best_merit = merit;
	    }

	    if ((gain[0] > best_gain) || (evalCV) || (!classIsNominal)) {
	      // System.out.println("new best "+printSub(temp_group)+" "+merit+" "+gain[0]);
	      
	      temp_gain = gain[0]; // note the best gain for this level
	      best_group = (BitSet)temp_group.clone();
	      orderedFs[size] = i;
	      ok = true;
	    }
	  } 
	}
      }
      
      size++;
      best_gain = temp_gain;
      
      if (size == numAttributes) {
	ok = false;
      }
    }
    
    count = 0;
    for (i = 0; i < orderedFs.length; i++) {
      if (orderedFs[i] != -1) {
	System.out.print((orderedFs[i] + 1) + " ");
	count++;
      }
    }
    System.out.println();
    
    decisionFeatures = new int [count];
    for (i = 0; i < count; i++) {
      decisionFeatures[i] = orderedFs[i];
    }
    // System.exit(1);
    // if ((classIsNominal) && (!evalCV))
    pruneCV();
  }

  /**
   * Evaluate an ordered list of features (stored in decisionFeatures)
   * by cross validation. Each feature from the ordered list is added
   * one at a time to a set of features (initially empty) and cross
   * validated against the training data (if an addition of a feature
   * improves cross validation accuracy it is retained. The final set
   * of features is stored in decisionFeatures.
   */
  private void pruneCV() throws Exception {

    int i, j;
    int classI;
    BitSet best_group, temp_group;
    double merit, best_merit;
    int numSet = 1;

    classI = theInstances.classIndex();
    temp_group = new BitSet(numAttributes);
    temp_group.set(classI);
    best_group = (BitSet)temp_group.clone();
  
    best_merit = estimateAccuracy(temp_group, numSet, false, true, false);
    
    for (i = 0; i < decisionFeatures.length; i++) {
      if (decisionFeatures[i] != classI) {
	temp_group.set(decisionFeatures[i]);
	
	merit = estimateAccuracy(temp_group, numSet+1, false, true, false);

	if (merit > best_merit) {
	  numSet++;
	  // System.out.println("new best feature set: "+printSub(temp_group)+" "+merit);
	  best_merit = merit;
	} else {
	  // unselect this attribute
	  temp_group.clear(decisionFeatures[i]);
	}
	// numSet++;
      }
    }

    best_group = temp_group;
    // set selected features
    for (i = 0, j = 0; i < numAttributes; i++) {
      if (best_group.get(i)) {
	j++;
      }
    }

    int [] td = new int [decisionFeatures.length];
    System.arraycopy(decisionFeatures, 0, td, 0, decisionFeatures.length);

    decisionFeatures = new int[j];

    for (i = 1, j = 0; i < td.length; i++) {
      if (best_group.get(td[i])) {
	decisionFeatures[j++] = td[i];    
      }
    }

    // append class to end
    decisionFeatures[j] = td[0];
  }
      
    
  /**
   * Does a best first search 
   *
   */
  private void best_first() throws Exception {

    int i, j, classI, count = 0, fc, tree_count = 0;
    int evals = 0;
    BitSet best_group, temp_group;
    int [] stale;
    double [] best_merit;
    double merit;
    boolean z;
    boolean added;
    Link2 tl;
  
    Hashtable lookup = new Hashtable((int)(200.0 * numAttributes * 1.5));
    LinkedList2 bfList = new LinkedList2();
    
    best_merit = new double[1]; best_merit[0] = 0.0;
    stale = new int[1]; stale[0] = 0;
    best_group = new BitSet(numAttributes);

    // Add class to initial subset
    classI = theInstances.classIndex();
    best_group.set(classI);
  
    best_merit[0] = estimateAccuracy(best_group, 1, evalC45, evalCV,
				     evalMDL);
    
    if (debug) {
      System.out.println("Accuracy of initial subset: " + best_merit[0]);
    }

    // add the initial group to the list
    bfList.addToList(best_group, best_merit[0]);
    // add initial subset to the hashtable
    lookup.put(best_group, "");
      
    while (stale[0] < maxStale) {
      added = false;
      
	// finished search?
      if (bfList.size() == 0) {
	stale[0] = maxStale;
	break;
      }

      // copy the feature set at the head of the list
      tl = bfList.getLinkAt(0);
      temp_group = (BitSet)(tl.getGroup().clone());
      
      // remove the head of the list
      bfList.removeLinkAt(0);
      
      for (i = 0; i < numAttributes; i++) {
	// if (search_direction == 1)
	z = ((i != classI) && (!temp_group.get(i)));

	if (z) {
	  // set the bit (feature to add/delete) */
	  temp_group.set(i);
	  
	  /* if this subset has been seen before, then it is already in 
	     the list (or has been fully expanded) */
	  BitSet tt = (BitSet)temp_group.clone();
	  if (lookup.containsKey(tt) == false) {
	    fc = 0;
	    
	    for (int jj = 0; jj < numAttributes; jj++) {
	      if (tt.get(jj)) {
		fc++;
	      }
	    }

	    merit = estimateAccuracy(tt, fc, evalC45, evalCV, evalMDL);

	    if (debug) {
	      System.out.println("evaluating: " + printSub(tt) + " " + merit);
	    }
	    
	    // is this better than the best?
	    // if (search_direction == 1)
	 
	    z = ((merit - best_merit[0]) > 0.00001);
	 
	    // else
	    // z = ((best_merit[0] - merit) > 0.00001);

	    if (z) {
	      if (debug) {
		System.out.println("new best feature set: " + printSub(tt)
				   + " " + merit);
	      }
	      added = true;
	      stale[0] = 0;
	      best_merit[0] = merit;

	      best_group = (BitSet)(temp_group.clone());
	    }

	    // insert this one in the list and the hash table
	    bfList.addToList(tt, merit);
	    lookup.put(tt, "");
	    count++;
	  }
	  // unset this addition(deletion)
	  temp_group.clear(i);
	}
      }
	/* if we haven't added a new feature subset then full expansion 
	   of this node hasn't resulted in anything better */
      if (!added) {
	  stale[0]++;
      }
    }
   
    // set selected features
    for (i = 0, j = 0; i < numAttributes; i++) {
      if (best_group.get(i)) {
	j++;
      }
    }
    
    decisionFeatures = new int[j];

    for (i = 0, j = 0; i < numAttributes; i++) {
      if (best_group.get(i)) {
	decisionFeatures[j++] = i;    
      }
    }
  }
 


  // ==============
  // Public methods
  // ==============

  /**
   * Constructor for a DecisionTable
   */
  public DecisionTable() {
    resetOptions();
  }

  protected void resetOptions() {

    entries = null;
    decisionFeatures = null;

    useBinaryAtts = false;
    debug = false;

    useIBK = false;

    evalC45 = false;
    evalCV = true;
    evalMDL = false;
    CVFolds = 1;
    searchBF = true;

    c45PF = 0.65;

    maxStale = 5;
    displayRules = false;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(10);

    newVector.addElement(new Option(
				    "\tSelect and order attributes by "
				    + "forward hill climbing\n"
				    + "\trather than best first search "
				    + "(default).",
				    "O", 0, "-O"));

    newVector.addElement(new Option(
				    "\tNumber of fully expanded non "
				    + "improving subsets to consider\n"
				    + "\tbefore terminating a best "
				    + "first search.\n"
				    + "\tUse in conjunction with -B. "
				    + "(Default = 5)",
				    "S", 1, 
				    "-S <number of non improving nodes>"));
    
    newVector.addElement(new Option(
				    "\tUse C45 style error estimate to "
				    + "evaluate features (nominal class "
				    + "only).",
				    "C", 0, "-C"));

    newVector.addElement(new Option(
				    "\t[0.0 - 1.0] Set confidence factor "
				    + "for C45 error estimate.\n"
				    + "\tUse in conjunction with -C. "
				    + "(Default =  0.65)",
				    "P", 1, "-P"));
    
    newVector.addElement(new Option(
				    "\tUse MDL to evaluate features "
				    + "(nominal class only).\n",
				    "M", 0, "-M"));

    newVector.addElement(new Option(
				    "\tUse cross validation to "
				    + "evaluate features.\n"
				    + "\tUse number of folds = 1 for "
				    + "leave one out CV. "
				    + "(Default = leave one out CV)",
				    "X", 1, "-X <number of folds>"));

    newVector.addElement(new Option(
				    "\tMake binary attributes when "
				    + "discretising.",
				    "D", 0, "-D"));

    newVector.addElement(new Option(
				    "\tUse nearest neighbour instead of "
				    + "global table majority.",
				    "I", 0, "-I"));

    newVector.addElement(new Option(
				    "\tDisplay decision table rules.",
				    "R", 0, "-R"));
    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -O <br>
   * Select and order attributes by forward hill climbing
   * rather than best first search (default). <p>
   *
   * -S num <br>
   * Number of fully expanded non improving subsets to consider
   * before terminating a best first search. Use in conjunction with -B. 
   * (Default = 5) <p>
   *
   * -C <br>
   * Use C45 style error estimate to evaluate features (nominal class 
   * only). <p>
   *
   * -P num <br>
   * [0.0 - 1.0] Set confidence factor for C45 error estimate.
   * Use in conjunction with -C. (Default =  0.65) <p>
   *
   * -M <br>
   * Use MDL to evaluate features (nominal class only). <p>
   *
   * -X num <br>
   * Use cross validation to evaluate features. Use number of folds = 1 for
   * leave one out CV. (Default = leave one out CV) <p>
   *
   * -D <br>
   * Make binary attributes when discretising. <p>
   * 
   * -I <br>
   * Use nearest neighbour instead of global table majority. <p>
   * 
   * -R <br>
   * Display decision table rules. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    resetOptions();

    String optionString = Utils.getOption('X',options);
    if (optionString.length() != 0) {
      evalCV = true;
      CVFolds = Integer.parseInt(optionString);
    }

    searchBF = !Utils.getFlag('O', options);
    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0) {
      searchBF = true;
      maxStale = Integer.parseInt(optionString);
    }

    optionString = Utils.getOption('P',options);
    if (optionString.length() != 0) {
      c45PF = Double.valueOf(optionString).doubleValue();
    }

    evalC45 = Utils.getFlag('C', options);
    evalMDL = Utils.getFlag('M', options);
    useBinaryAtts = Utils.getFlag('D', options);
    useIBK = Utils.getFlag('I',options);
    displayRules = Utils.getFlag('R',options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [12];
    int current = 0;

    options[current++] = "-X"; options[current++] = "" + CVFolds;
    options[current++] = "-P"; options[current++] = "" + c45PF;
    if (evalC45) {
      options[current++] = "-C";
    }
    if (evalMDL) {
      options[current++] = "-M";
    }
    if (!searchBF) {
      options[current++] = "-O";
    } else {
      options[current++] = "-S"; options[current++] = "" + maxStale;
    }
    if (useBinaryAtts) {
      options[current++] = "-D";
    }
    if (useIBK) {
      options[current++] = "-I";
    }
    if (displayRules) {
      options[current++] = "-R";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Generates the classifier.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    int i;

    rr = new Random(1);
    theInstances = data;
    
    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    disTransform = new DiscretizeFilter();

    if (theInstances.classAttribute().isNumeric()) {
      classIsNominal = false;
      // use binned discretisation if the class is numeric
      disTransform.setUseMDL(false);
      disTransform.setBins(10);
      disTransform.setInvertSelection(true);
      
      // Discretize all attributes EXCEPT the class 
      String rangeList = "";
      rangeList += theInstances.classIndex() + 1;
       
      disTransform.setAttributeIndices(rangeList);
    } else {
      disTransform.setUseBetterEncoding(true);
      classIsNominal = true;
    }

    if (useBinaryAtts) {
      bTransform = new NominalToBinaryFilter();
      bTransform.setBinaryAttributesNominal(true);
      bTransform.inputFormat(theInstances);
      theInstances = Filter.useFilter(theInstances, bTransform);
      disTransform.setMakeBinary(true);
    }

    disTransform.inputFormat(theInstances);
    theInstances = Filter.useFilter(theInstances, disTransform);

    numAttributes = theInstances.numAttributes();
    numInstances = theInstances.numInstances();
    majority = theInstances.meanOrMode(theInstances.classAttribute());

    if (searchBF) {
      best_first();
    } else {
      orderByHillClimb();
    }
    
    // reduce instances to selected features
    delTransform = new DeleteFilter();
    delTransform.setInvertSelection(true);
    // set features to keep
    delTransform.setAttributeIndicesArray(decisionFeatures); 
    delTransform.inputFormat(theInstances);
    theInstances = Filter.useFilter(theInstances, delTransform);

    // reset the number of attributes
    numAttributes = theInstances.numAttributes();
  

    // create hash table
    entries = new Hashtable((int)(theInstances.numInstances() * 1.5));

    // insert instances into the hash table
    for (i = 0; i < numInstances; i++) {
      Instance inst = theInstances.instance(i);
      insertIntoTable(inst, null);
    }

    // Replace the global table majority with nearest neighbour?
    if (useIBK) {
      ibk = new IBk();
      ibk.buildClassifier(theInstances);
    }

    // Save memory
    theInstances = new Instances(theInstances, 0);
  }

 
  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {

    hashKey thekey;
    double [] tempDist;

    if (classIsNominal) {
      return Utils.maxIndex(distributionForInstance(instance)); 
    }

    disTransform.input(instance);
    instance = disTransform.output();

    delTransform.input(instance);
    instance = delTransform.output();

    thekey = new hashKey(instance, instance.numAttributes());
      
    // see if this one is already in the table
    if ((tempDist = (double[])entries.get(thekey)) != null) {
      return (tempDist[0] / tempDist[1]);
    } else {
      if (useIBK) {
	// ibkuse++;
	return ibk.classifyInstance(instance);
      } else {
	  return majority;
      }
    }
  }


  /**
   * Calculates the class membership probabilities for the given 
   * test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if distribution can't be computed
   */
  public double [] distributionForInstance(Instance instance)
    throws Exception {

    hashKey thekey;
    double [] tempDist;
    double [] normDist;

    if (useBinaryAtts) {
      bTransform.input(instance);
      
      instance = bTransform.output();
    }

    disTransform.input(instance);
    instance = disTransform.output();

    delTransform.input(instance);
    instance = delTransform.output();

    if (!classIsNominal) {
      throw new Exception("Class is numeric!");
    } else {
      thekey = new hashKey(instance, instance.numAttributes());
      // if this one is not in the table
      if ((tempDist = (double [])entries.get(thekey)) == null) {
	if (useIBK) {
	  tempDist = ibk.distributionForInstance(instance);
	} else {
	  tempDist = new double [theInstances.classAttribute().numValues()];
	  tempDist[(int)majority] = 1.0;
	}
      } else {
	// normalise distribution
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist, 0, normDist, 0, tempDist.length);
	Utils.normalize(normDist);
	tempDist=normDist;
      }
    }

    return tempDist;
  }

  /**
   * Returns a string description of the features selected
   *
   * @return a string of features
   */
  public String printFeatures() {

    int i;
    String s = "";
   
    for (i = 0; i < decisionFeatures.length; i++) {
      if (i == 0) {
	s = "" + (decisionFeatures[i] + 1);
      } else {
	s += "," + (decisionFeatures[i] + 1);
      }
    }
    return s;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    if (entries == null) {
      return "Can't print decision table classifier!";
    } else {
      StringBuffer text = new StringBuffer();
      
      text.append("Decision Table classifier:" +
		  "\n\nNumber of training instances: " + numInstances +
		  "\nNumber of Rules : " + entries.size() + "\n");

      if (useIBK) {
	text.append("Non matches covered by IB1.\n");
      } else {
	text.append("Non matches covered by Majority class.\n");
      }
      if (searchBF) {
	text.append("Best first search for feature set,\nterminated after "+
		    maxStale + " non improving subsets.\n");
      } else {
	text.append("Forward hill climbing search for feature set.\n");
      }
      if (evalC45) {
	text.append("Evaluation (for feature selection): "
		    + "C45 error estimate (" + c45PF + ") ");
      } else if (evalCV) {
	text.append("Evaluation (for feature selection): CV ");
	if (CVFolds > 1) {
	  text.append("(" + CVFolds + " fold) ");
	} else {
	  text.append("(leave one out) ");
	}
      } else {
	text.append("Evaluation (for feature selection): MDL criterion ");
      }
      if ((!searchBF) && (!evalCV)) {
	text.append("+ CV");
      }
      if (!searchBF) {
	text.append("\nFeature set (ordered + class): " + printFeatures());
      } else { 
	text.append("\nFeature set: " + printFeatures());
      }
      if (displayRules) {
	text.append("\n\nRules:\n");
	StringBuffer tm = new StringBuffer();
	for (int i = 0; i < decisionFeatures.length; i++) {
	  tm.append(theInstances.attribute(i).name() + "  ");	
	}

	for (int i = 0; i < tm.length() + 10; i++) {
	  text.append("=");
	}
	text.append("\n");
	text.append(tm);
	text.append("\n");
	for (int i = 0; i < tm.length() + 10; i++) {
	  text.append("=");
	}
	text.append("\n");
    

	Enumeration e = entries.keys();
	while (e.hasMoreElements()) {
	  hashKey tt = (hashKey)e.nextElement();
	  text.append(tt.toString(theInstances));
	  double [] ClassDist = (double []) entries.get(tt);

	  if (classIsNominal) {
	    int m = Utils.maxIndex(ClassDist);
	    try {
	      text.append(theInstances.classAttribute().value(m)+"\n");
	    } catch (Exception ee) {
	      System.out.println(ee.getMessage());
	    }
	  } else {
	    text.append((ClassDist[0] / ClassDist[1]));
	  }
	}

	for (int i = 0; i < tm.length() + 10; i++) {
	  text.append("=");
	}
	text.append("\n");
	text.append("\n");
      }
      return text.toString();
    }
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    Classifier scheme;

    try {
      scheme = new DecisionTable();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}

