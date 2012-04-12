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
 *    DecisionTable.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.classifiers.rules;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.IB1;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Class for building and using a simple decision table majority classifier.
 * For more information see: <p>
 *
 * Kohavi R. (1995).<i> The Power of Decision Tables.</i> In Proc
 * European Conference on Machine Learning.<p>
 *
 * Valid options are: <p>
 *
 * -S num <br>
 * Number of fully expanded non improving subsets to consider
 * before terminating a best first search.
 * (Default = 5) <p>
 *
 * -X num <br>
 * Use cross validation to evaluate features. Use number of folds = 1 for
 * leave one out CV. (Default = leave one out CV) <p>
 * 
 * -I <br>
 * Use nearest neighbour instead of global table majority. <p>
 *
 * -R <br>
 * Prints the decision table. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.30 $ 
 */
public class DecisionTable extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, 
	     AdditionalMeasureProducer {
  
  /** The hashtable used to hold training instances */
  private Hashtable m_entries;

  /** Holds the final feature set */
  private int [] m_decisionFeatures;

  /** Discretization filter */
  private Filter m_disTransform;

  /** Filter used to remove columns discarded by feature selection */
  private Remove m_delTransform;

  /** IB1 used to classify non matching instances rather than majority class */
  private IBk m_ibk;
  
  /** Holds the training instances */
  private Instances m_theInstances;
  
  /** The number of attributes in the dataset */
  private int m_numAttributes;

  /** The number of instances in the dataset */
  private int m_numInstances;

  /** Class is nominal */
  private boolean m_classIsNominal;

  /** Output debug info */
  private boolean m_debug;

  /** Use the IBk classifier rather than majority class */
  private boolean m_useIBk;

  /** Display Rules */
  private boolean m_displayRules;

  /** 
   * Maximum number of fully expanded non improving subsets for a best 
   * first search. 
   */
  private int m_maxStale;

  /** Number of folds for cross validating feature sets */
  private int m_CVFolds;

  /** Random numbers for use in cross validation */
  private Random m_rr;

  /** Holds the majority class */
  private double m_majority;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Class for building and using a simple decision table majority "
      + "classifier. For more information see: \n\n"
      + "Kohavi R. (1995). \"The Power of Decision Tables.\" In Proc "
      + "European Conference on Machine Learning.";
  }

  /**
   * Class for a node in a linked list. Used in best first search.
   */
  public class Link {

    /** The group */
    BitSet m_group;

    /** The merit */
    double m_merit;

    /**
     * The constructor.
     *
     * @param gr the group
     * @param mer the merit
     */
    public Link (BitSet gr, double mer) {

      m_group = (BitSet)gr.clone();
      m_merit = mer;
    }
  
    /**
     * Gets the group.
     */
    public BitSet getGroup() {

      return m_group;
    }
  
    /**
     * Gets the merit.
     */
    public double getMerit() {

      return m_merit;
    }

    /**
     * Returns string representation.
     */
    public String toString() {

      return ("Node: "+m_group.toString()+"  "+m_merit);
    }
  }
  
  /**
   * Class for handling a linked list. Used in best first search.
   * Extends the Vector class.
   */
  public class LinkedList extends FastVector {

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
    public Link getLinkAt(int index) throws Exception {

      if (size()==0) {
	throw new Exception("List is empty (getLinkAt)");
      } else if ((index >= 0) && (index < size())) {
	return ((Link)(elementAt(index)));
      } else {
	throw new Exception("index out of range (getLinkAt)");
      }
    }

    /**
     * Aadds an element (Link) to the list.
     *
     * @param gr the feature set specification
     * @param mer the "merit" of this feature set
     */
    public void addToList(BitSet gr, double mer) {

      Link newL = new Link(gr, mer);
	
      if (size()==0) {
	addElement(newL);
      }
      else if (mer > ((Link)(firstElement())).getMerit()) {
	insertElementAt(newL,0);
      } else {
	int i = 0;
	int size = size();
	boolean done = false;
	while ((!done) && (i < size)) {
	  if (mer > ((Link)(elementAt(i))).getMerit()) {
	    insertElementAt(newL,i);
	    done = true;
	  } else if (i == size-1) {
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
   */
  public static class hashKey implements Serializable {
    
    /** Array of attribute values for an instance */
    private double [] attributes;
    
    /** True for an index if the corresponding attribute value is missing. */
    private boolean [] missing;

    /** The values */
    private String [] values;

    /** The key */
    private int key;

    /**
     * Constructor for a hashKey
     *
     * @param t an instance from which to generate a key
     * @param numAtts the number of attributes
     * @param ignoreClass if true treat the class as a normal attribute
     */
    public hashKey(Instance t, int numAtts, boolean ignoreClass) throws Exception {

      int i;
      int cindex = t.classIndex();

      key = -999;
      attributes = new double [numAtts];
      missing = new boolean [numAtts];
      for (i=0;i<numAtts;i++) {
	if (i == cindex && !ignoreClass) {
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
     * @param maxColWidth width to make the fields
     */
    public String toString(Instances t, int maxColWidth) {

      int i;
      int cindex = t.classIndex();
      StringBuffer text = new StringBuffer();
      
      for (i=0;i<attributes.length;i++) {
	if (i != cindex) {
	  if (missing[i]) {
	    text.append("?");
	    for (int j=0;j<maxColWidth;j++) {
	      text.append(" ");
	    }
	  } else {
	    String ss = t.attribute(i).value((int)attributes[i]);
	    StringBuffer sb = new StringBuffer(ss);
	    
	    for (int j=0;j < (maxColWidth-ss.length()+1); j++) {
		sb.append(" ");
	    }
	    text.append(sb);
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
    public hashKey(double [] t) {

      int i;
      int l = t.length;

      key = -999;
      attributes = new double [l];
      missing = new boolean [l];
      for (i=0;i<l;i++) {
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
      
      if (key != -999)
	return key;
      for (int i=0;i<attributes.length;i++) {
	if (missing[i]) {
	  hv += (i*13);
	} else {
	  hv += (i * 5 * (attributes[i]+1));
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
      
      if ((b == null) || !(b.getClass().equals(this.getClass()))) {
        return false;
      }
      boolean ok = true;
      boolean l;
      if (b instanceof hashKey) {
	hashKey n = (hashKey)b;
	for (int i=0;i<attributes.length;i++) {
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
      
      System.out.println("Hash val: "+hashCode());
    }
  }

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
      thekey = new hashKey(inst, inst.numAttributes(), false);
    }
      
    // see if this one is already in the table
    tempClassDist2 = (double []) m_entries.get(thekey);
    if (tempClassDist2 == null) {
      if (m_classIsNominal) {
	newDist = new double [m_theInstances.classAttribute().numValues()];
	newDist[(int)inst.classValue()] = inst.weight();

	// add to the table
	m_entries.put(thekey, newDist);
      } else {
	newDist = new double [2];
	newDist[0] = inst.classValue() * inst.weight();
	newDist[1] = inst.weight();

	// add to the table
	m_entries.put(thekey, newDist);
      }
    } else { 

      // update the distribution for this instance
      if (m_classIsNominal) {
	tempClassDist2[(int)inst.classValue()]+=inst.weight();
	
	// update the table
	m_entries.put(thekey, tempClassDist2);
      } else  {
	tempClassDist2[0] += (inst.classValue() * inst.weight());
	tempClassDist2[1] += inst.weight();

	// update the table
	m_entries.put(thekey, tempClassDist2);
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
    if (m_classIsNominal) {

      // if this one is not in the table
      if ((tempDist = (double [])m_entries.get(thekey)) == null) {
	throw new Error("This should never happen!");
      } else {
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist,0,normDist,0,tempDist.length);
	normDist[(int)instance.classValue()] -= instance.weight();

	// update the table
	// first check to see if the class counts are all zero now
	boolean ok = false;
	for (int i=0;i<normDist.length;i++) {
	  if (!Utils.eq(normDist[i],0.0)) {
	    ok = true;
	    break;
	  }
	}
	if (ok) {
	  Utils.normalize(normDist);
	  return Utils.maxIndex(normDist);
	} else {
	  return m_majority;
	}
      }
      //      return Utils.maxIndex(tempDist);
    } else {

      // see if this one is already in the table
      if ((tempDist = (double[])m_entries.get(thekey)) != null) {
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist,0,normDist,0,tempDist.length);
	normDist[0] -= (instance.classValue() * instance.weight());
	normDist[1] -= instance.weight();
	if (Utils.eq(normDist[1],0.0)) {
	    return m_majority;
	} else {
	  return (normDist[0] / normDist[1]);
	}
      } else {
	throw new Error("This should never happen!");
      }
    }
    
    // shouldn't get here 
    // return 0.0;
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
    int numCl = m_theInstances.classAttribute().numValues();
    double [][] class_distribs = new double [numFold][numCl];
    double [] instA = new double [fs.length];
    double [] normDist;
    hashKey thekey;
    double acc = 0.0;
    int classI = m_theInstances.classIndex();
    Instance inst;

    if (m_classIsNominal) {
      normDist = new double [numCl];
    } else {
      normDist = new double [2];
    }

    // first *remove* instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);
      for (int j=0;j<fs.length;j++) {
	if (fs[j] == classI) {
	  instA[j] = Double.MAX_VALUE; // missing for the class
	} else if (inst.isMissing(fs[j])) {
	  instA[j] = Double.MAX_VALUE;
	} else{
	  instA[j] = inst.value(fs[j]);
	}
      }
      thekey = new hashKey(instA);
      if ((class_distribs[i] = (double [])m_entries.get(thekey)) == null) {
	throw new Error("This should never happen!");
      } else {
	if (m_classIsNominal) {
	  class_distribs[i][(int)inst.classValue()] -= inst.weight();
	} else {
	  class_distribs[i][0] -= (inst.classValue() * inst.weight());
	  class_distribs[i][1] -= inst.weight();
	}
	ruleCount++;
      }
    }

    // now classify instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);
      System.arraycopy(class_distribs[i],0,normDist,0,normDist.length);
      if (m_classIsNominal) {
	boolean ok = false;
	for (int j=0;j<normDist.length;j++) {
	  if (!Utils.eq(normDist[j],0.0)) {
	    ok = true;
	    break;
	  }
	}
	if (ok) {
	  Utils.normalize(normDist);
	  if (Utils.maxIndex(normDist) == inst.classValue())
	    acc += inst.weight();
	} else {
	  if (inst.classValue() == m_majority) {
	    acc += inst.weight();
	  }
	}
      } else {
	if (Utils.eq(normDist[1],0.0)) {
	    acc += ((inst.weight() * (m_majority - inst.classValue())) * 
		    (inst.weight() * (m_majority - inst.classValue())));
	} else {
	  double t = (normDist[0] / normDist[1]);
	  acc += ((inst.weight() * (t - inst.classValue())) * 
		  (inst.weight() * (t - inst.classValue())));
	}
      }
    }

    // now re-insert instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);
      if (m_classIsNominal) {
	class_distribs[i][(int)inst.classValue()] += inst.weight();
      } else {
	class_distribs[i][0] += (inst.classValue() * inst.weight());
	class_distribs[i][1] += inst.weight();
      }
    }
    return acc;
  }


  /**
   * Evaluates a feature subset by cross validation
   *
   * @param feature_set the subset to be evaluated
   * @param num_atts the number of attributes in the subset
   * @return the estimated accuracy
   * @exception Exception if subset can't be evaluated
   */
  private double estimateAccuracy(BitSet feature_set, int num_atts)
    throws Exception {

    int i;
    Instances newInstances;
    int [] fs = new int [num_atts];
    double acc = 0.0;
    double [][] evalArray;
    double [] instA = new double [num_atts];
    int classI = m_theInstances.classIndex();
    
    int index = 0;
    for (i=0;i<m_numAttributes;i++) {
      if (feature_set.get(i)) {
	fs[index++] = i;
      }
    }

    // create new hash table
    m_entries = new Hashtable((int)(m_theInstances.numInstances() * 1.5));

    // insert instances into the hash table
    for (i=0;i<m_numInstances;i++) {

      Instance inst = m_theInstances.instance(i);
      for (int j=0;j<fs.length;j++) {
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
    
    
    if (m_CVFolds == 1) {

      // calculate leave one out error
      for (i=0;i<m_numInstances;i++) {
	Instance inst = m_theInstances.instance(i);
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
	if (m_classIsNominal) {
	  if (t == inst.classValue()) {
	    acc+=inst.weight();
	  }
	} else {
	  acc += ((inst.weight() * (t - inst.classValue())) * 
		  (inst.weight() * (t - inst.classValue())));
	}
	// weight_sum += inst.weight();
      }
    } else {
      m_theInstances.randomize(m_rr);
      m_theInstances.stratify(m_CVFolds);

      // calculate 10 fold cross validation error
      for (i=0;i<m_CVFolds;i++) {
	Instances insts = m_theInstances.testCV(m_CVFolds,i);
	acc += classifyFoldCV(insts, fs);
      }
    }
  
    if (m_classIsNominal) {
      return (acc / m_theInstances.sumOfWeights());
    } else {
      return -(Math.sqrt(acc / m_theInstances.sumOfWeights()));   
    }
  }

  /**
   * Returns a String representation of a feature subset
   *
   * @param sub BitSet representation of a subset
   * @return String containing subset
   */
  private String printSub(BitSet sub) {

    int i;

    String s="";
    for (int jj=0;jj<m_numAttributes;jj++) {
      if (sub.get(jj)) {
	s += " "+(jj+1);
      }
    }
    return s;
  }
    
  /**
   * Does a best first search 
   */
  private void best_first() throws Exception {

    int i,j,classI,count=0,fc,tree_count=0;
    int evals=0;
    BitSet best_group, temp_group;
    int [] stale;
    double [] best_merit;
    double merit;
    boolean z;
    boolean added;
    Link tl;
  
    Hashtable lookup = new Hashtable((int)(200.0*m_numAttributes*1.5));
    LinkedList bfList = new LinkedList();
    best_merit = new double[1]; best_merit[0] = 0.0;
    stale = new int[1]; stale[0] = 0;
    best_group = new BitSet(m_numAttributes);

    // Add class to initial subset
    classI = m_theInstances.classIndex();
    best_group.set(classI);
    best_merit[0] = estimateAccuracy(best_group, 1);
    if (m_debug)
      System.out.println("Accuracy of initial subset: "+best_merit[0]);

    // add the initial group to the list
    bfList.addToList(best_group,best_merit[0]);

    // add initial subset to the hashtable
    lookup.put(best_group,"");
    while (stale[0] < m_maxStale) {

      added = false;

      // finished search?
      if (bfList.size()==0) {
	stale[0] = m_maxStale;
	break;
      }

      // copy the feature set at the head of the list
      tl = bfList.getLinkAt(0);
      temp_group = (BitSet)(tl.getGroup().clone());

      // remove the head of the list
      bfList.removeLinkAt(0);

      for (i=0;i<m_numAttributes;i++) {

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
	    for (int jj=0;jj<m_numAttributes;jj++) {
	      if (tt.get(jj)) {
		fc++;
	      }
	    }
	    merit = estimateAccuracy(tt, fc);
	    if (m_debug) {
	      System.out.println("evaluating: "+printSub(tt)+" "+merit); 
	    }
	    
	    // is this better than the best?
	    // if (search_direction == 1)
	    z = ((merit - best_merit[0]) > 0.00001);
	 
	    // else
	    // z = ((best_merit[0] - merit) > 0.00001);

	    if (z) {
	      if (m_debug) {
		System.out.println("new best feature set: "+printSub(tt)+
				   " "+merit);
	      }
	      added = true;
	      stale[0] = 0;
	      best_merit[0] = merit;
	      best_group = (BitSet)(temp_group.clone());
	    }

	    // insert this one in the list and the hash table
	    bfList.addToList(tt, merit);
	    lookup.put(tt,"");
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
    for (i=0,j=0;i<m_numAttributes;i++) {
      if (best_group.get(i)) {
	j++;
      }
    }
    
    m_decisionFeatures = new int[j];
    for (i=0,j=0;i<m_numAttributes;i++) {
      if (best_group.get(i)) {
	m_decisionFeatures[j++] = i;    
      }
    }
  }
 

  /**
   * Resets the options.
   */
  protected void resetOptions()  {

    m_entries = null;
    m_decisionFeatures = null;
    m_debug = false;
    m_useIBk = false;
    m_CVFolds = 1;
    m_maxStale = 5;
    m_displayRules = false;
  }

   /**
   * Constructor for a DecisionTable
   */
  public DecisionTable() {

    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
              "\tNumber of fully expanded non improving subsets to consider\n" +
	      "\tbefore terminating a best first search.\n" +
	      "\tUse in conjunction with -B. (Default = 5)",
              "S", 1, "-S <number of non improving nodes>"));
    

    newVector.addElement(new Option(
              "\tUse cross validation to evaluate features.\n" +
	      "\tUse number of folds = 1 for leave one out CV.\n" +
	      "\t(Default = leave one out CV)",
              "X", 1, "-X <number of folds>"));

     newVector.addElement(new Option(
              "\tUse nearest neighbour instead of global table majority.\n",
              "I", 0, "-I"));

     newVector.addElement(new Option(
              "\tDisplay decision table rules.\n",
              "R", 0, "-R")); 
    return newVector.elements();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String crossValTipText() {
    return "Sets the number of folds for cross validation (1 = leave one out).";
  }

  /**
   * Sets the number of folds for cross validation (1 = leave one out)
   *
   * @param folds the number of folds
   */
  public void setCrossVal(int folds) {

    m_CVFolds = folds;
  }

  /**
   * Gets the number of folds for cross validation
   *
   * @return the number of cross validation folds
   */
  public int getCrossVal() {

    return m_CVFolds;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxStaleTipText() {
    return "Sets the number of non improving decision tables to consider " 
      + "before abandoning the search.";
  }

  /**
   * Sets the number of non improving decision tables to consider
   * before abandoning the search.
   *
   * @param stale the number of nodes
   */
  public void setMaxStale(int stale) {

    m_maxStale = stale;
  }

  /**
   * Gets the number of non improving decision tables
   *
   * @return the number of non improving decision tables
   */
  public int getMaxStale() {

    return m_maxStale;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useIBkTipText() {
    return "Sets whether IBk should be used instead of the majority class.";
  }

  /**
   * Sets whether IBk should be used instead of the majority class
   *
   * @param ibk true if IBk is to be used
   */
  public void setUseIBk(boolean ibk) {

    m_useIBk = ibk;
  }
  
  /**
   * Gets whether IBk is being used instead of the majority class
   *
   * @return true if IBk is being used
   */
  public boolean getUseIBk() {

    return m_useIBk;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String displayRulesTipText() {
    return "Sets whether rules are to be printed.";
  }

  /**
   * Sets whether rules are to be printed
   *
   * @param rules true if rules are to be printed
   */
  public void setDisplayRules(boolean rules) {

    m_displayRules = rules;
  }
  
  /**
   * Gets whether rules are being printed
   *
   * @return true if rules are being printed
   */
  public boolean getDisplayRules() {

    return m_displayRules;
  }

  /**
   * Parses the options for this object.
   *
   * Valid options are: <p>
   *
   * -S num <br>
   * Number of fully expanded non improving subsets to consider
   * before terminating a best first search.
   * (Default = 5) <p>
   *
   * -X num <br>
   * Use cross validation to evaluate features. Use number of folds = 1 for
   * leave one out CV. (Default = leave one out CV) <p>
   * 
   * -I <br>
   * Use nearest neighbour instead of global table majority. <p>
   *
   * -R <br>
   * Prints the decision table. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String optionString;

    resetOptions();

    optionString = Utils.getOption('X',options);
    if (optionString.length() != 0) {
      m_CVFolds = Integer.parseInt(optionString);
    }

    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0) {
      m_maxStale = Integer.parseInt(optionString);
    }

    m_useIBk = Utils.getFlag('I',options);

    m_displayRules = Utils.getFlag('R',options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [7];
    int current = 0;

    options[current++] = "-X"; options[current++] = "" + m_CVFolds;
    options[current++] = "-S"; options[current++] = "" + m_maxStale;
    if (m_useIBk) {
      options[current++] = "-I";
    }
    if (m_displayRules) {
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

    m_rr = new Random(1);
    m_theInstances = new Instances(data);
    m_theInstances.deleteWithMissingClass();
    if (m_theInstances.numInstances() == 0) {
      throw new Exception("No training instances without missing class!");
    }
    if (m_theInstances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }

    if (m_theInstances.classAttribute().isNumeric()) {
      m_disTransform = new weka.filters.unsupervised.attribute.Discretize();
      m_classIsNominal = false;
      
      // use binned discretisation if the class is numeric
      ((weka.filters.unsupervised.attribute.Discretize)m_disTransform).
	setBins(10);
      ((weka.filters.unsupervised.attribute.Discretize)m_disTransform).
	setInvertSelection(true);
      
      // Discretize all attributes EXCEPT the class 
      String rangeList = "";
      rangeList+=(m_theInstances.classIndex()+1);
      //System.out.println("The class col: "+m_theInstances.classIndex());
      
      ((weka.filters.unsupervised.attribute.Discretize)m_disTransform).
	setAttributeIndices(rangeList);
    } else {
      m_disTransform = new weka.filters.supervised.attribute.Discretize();
      ((weka.filters.supervised.attribute.Discretize)m_disTransform).setUseBetterEncoding(true);
      m_classIsNominal = true;
    }

    m_disTransform.setInputFormat(m_theInstances);
    m_theInstances = Filter.useFilter(m_theInstances, m_disTransform);
    
    m_numAttributes = m_theInstances.numAttributes();
    m_numInstances = m_theInstances.numInstances();
    m_majority = m_theInstances.meanOrMode(m_theInstances.classAttribute());
    
    best_first();
    
    // reduce instances to selected features
    m_delTransform = new Remove();
    m_delTransform.setInvertSelection(true);
    
    // set features to keep
    m_delTransform.setAttributeIndicesArray(m_decisionFeatures); 
    m_delTransform.setInputFormat(m_theInstances);
    m_theInstances = Filter.useFilter(m_theInstances, m_delTransform);
    
    // reset the number of attributes
    m_numAttributes = m_theInstances.numAttributes();
    
    // create hash table
    m_entries = new Hashtable((int)(m_theInstances.numInstances() * 1.5));
    
    // insert instances into the hash table
    for (i=0;i<m_numInstances;i++) {
      Instance inst = m_theInstances.instance(i);
      insertIntoTable(inst, null);
    }
    
    // Replace the global table majority with nearest neighbour?
    if (m_useIBk) {
      m_ibk = new IBk();
      m_ibk.buildClassifier(m_theInstances);
    }
    
    // Save memory
    m_theInstances = new Instances(m_theInstances, 0);
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

    m_disTransform.input(instance);
    m_disTransform.batchFinished();
    instance = m_disTransform.output();

    m_delTransform.input(instance);
    m_delTransform.batchFinished();
    instance = m_delTransform.output();

    thekey = new hashKey(instance, instance.numAttributes(), false);
    
    // if this one is not in the table
    if ((tempDist = (double [])m_entries.get(thekey)) == null) {
      if (m_useIBk) {
	tempDist = m_ibk.distributionForInstance(instance);
      } else {
	if (!m_classIsNominal) {
	  tempDist = new double[1];
	  tempDist[0] = m_majority;
	} else {
	  tempDist = new double [m_theInstances.classAttribute().numValues()];
	  tempDist[(int)m_majority] = 1.0;
	}
      }
    } else {
      if (!m_classIsNominal) {
	normDist = new double[1];
	normDist[0] = (tempDist[0] / tempDist[1]);
	tempDist = normDist;
      } else {
	
	// normalise distribution
	normDist = new double [tempDist.length];
	System.arraycopy(tempDist,0,normDist,0,tempDist.length);
	Utils.normalize(normDist);
	tempDist = normDist;
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
   
    for (i=0;i<m_decisionFeatures.length;i++) {
      if (i==0) {
	s = ""+(m_decisionFeatures[i]+1);
      } else {
	s += ","+(m_decisionFeatures[i]+1);
      }
    }
    return s;
  }

  /**
   * Returns the number of rules
   * @return the number of rules
   */
  public double measureNumRules() {
    return m_entries.size();
  }

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
    if (additionalMeasureName.compareToIgnoreCase("measureNumRules") == 0) {
      return measureNumRules();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
			  + " not supported (DecisionTable)");
    }
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    if (m_entries == null) {
      return "Decision Table: No model built yet.";
    } else {
      StringBuffer text = new StringBuffer();
      
      text.append("Decision Table:"+
		  "\n\nNumber of training instances: "+m_numInstances+
		  "\nNumber of Rules : "+m_entries.size()+"\n");
      
      if (m_useIBk) {
	text.append("Non matches covered by IB1.\n");
      } else {
	text.append("Non matches covered by Majority class.\n");
      }
      
      text.append("Best first search for feature set,\nterminated after "+
		  m_maxStale+" non improving subsets.\n");
      
      text.append("Evaluation (for feature selection): CV ");
      if (m_CVFolds > 1) {
	text.append("("+m_CVFolds+" fold) ");
      } else {
	  text.append("(leave one out) ");
      }
      text.append("\nFeature set: "+printFeatures());
      
      if (m_displayRules) {

	// find out the max column width
	int maxColWidth = 0;
	for (int i=0;i<m_theInstances.numAttributes();i++) {
	  if (m_theInstances.attribute(i).name().length() > maxColWidth) {
	    maxColWidth = m_theInstances.attribute(i).name().length();
	  }

	  if (m_classIsNominal || (i != m_theInstances.classIndex())) {
	    Enumeration e = m_theInstances.attribute(i).enumerateValues();
	    while (e.hasMoreElements()) {
	      String ss = (String)e.nextElement();
	      if (ss.length() > maxColWidth) {
		maxColWidth = ss.length();
	      }
	    }
	  }
	}

	text.append("\n\nRules:\n");
	StringBuffer tm = new StringBuffer();
	for (int i=0;i<m_theInstances.numAttributes();i++) {
	  if (m_theInstances.classIndex() != i) {
	    int d = maxColWidth - m_theInstances.attribute(i).name().length();
	    tm.append(m_theInstances.attribute(i).name());
	    for (int j=0;j<d+1;j++) {
	      tm.append(" ");
	    }
	  }
	}
	tm.append(m_theInstances.attribute(m_theInstances.classIndex()).name()+"  ");

	for (int i=0;i<tm.length()+10;i++) {
	  text.append("=");
	}
	text.append("\n");
	text.append(tm);
	text.append("\n");
	for (int i=0;i<tm.length()+10;i++) {
	  text.append("=");
	}
	text.append("\n");

	Enumeration e = m_entries.keys();
	while (e.hasMoreElements()) {
	  hashKey tt = (hashKey)e.nextElement();
	  text.append(tt.toString(m_theInstances,maxColWidth));
	  double [] ClassDist = (double []) m_entries.get(tt);

	  if (m_classIsNominal) {
	    int m = Utils.maxIndex(ClassDist);
	    try {
	      text.append(m_theInstances.classAttribute().value(m)+"\n");
	    } catch (Exception ee) {
	      System.out.println(ee.getMessage());
	    }
	  } else {
	    text.append((ClassDist[0] / ClassDist[1])+"\n");
	  }
	}
	
	for (int i=0;i<tm.length()+10;i++) {
	  text.append("=");
	}
	text.append("\n");
	text.append("\n");
      }
      return text.toString();
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the command-line options
   */
  public static void main(String [] argv) {
    
    Classifier scheme;
    
    try {
      scheme = new DecisionTable();
      System.out.println(Evaluation.evaluateModel(scheme,argv));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}

