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
 *    KDTree.java
 *    Copyright (C) 2000 University of Waikato
 *
 */

package weka.core;
import  weka.core.*;

import java.io.Serializable;
import java.util.*;
import java.lang.Math;


/** 
 * This is a KD-Tree structure that stores instances using a divide and conquer 
 * method.
 * The connection to dataset is only a reference. For the tree structure the 
 * indexes are stored in a dynamic array
 * Building the tree:
 * If a node has <maxleaf> (option -L) instances no further splitting is done.
 * Also if the split would leave one side empty, the branch is not split any 
 * further even if the instances in the resulting node are more than <maxleaf> 
 * instances.
 *
 * -P <br>
 * Pruning flag. <p>
 *
 * -W <minimal-box-relative-width> <br>
 * minimal width of a box
 *
 * -L <maximal-inst-number> <br>
 * maximal instance number in a leaf
 * 
 * -D <distance function>
 * Distance function to be used (default = Euclidean distance)
 *
 * -N <br>
 * Set Normalization. Used when building the tree and selecting the
 * widest dimension, each dimension is 'normalized' to the universe range.
 *
 * -U <debuglevel> <br>
 * Set debuglevel. <p>
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class KDTree implements OptionHandler, Serializable{

  /** flag for pruning */
  boolean m_Prune = false;

  /** flag for normalizing */
  boolean m_Normalize = false;

  /** for debugging: debug level */
  private int m_DebugLevel = 0;

  /*
   * Ranges of the whole KDTree.
   * lowest and highest value and width (= high - low) for each 
   * dimension
   */
  private double[][] m_Universe;

  /** the distance function used */
  private DistanceFunction m_DistanceFunction = null;

  /** value to split on. */
  private double m_SplitValue;

  /** attribute to split on. */
  private int m_SplitDim;

  /** root node */
  private KDTreeNode m_Root = null;

  /** 
   * Indexlist of the instances of this kdtree. 
   * Instances get sorted according to the splits.
   * the nodes of the KDTree just hold their start and end indices 
   */
  private DynamicArrayOfPosInt m_InstList;

  /** Reference to the instances of the universe. */
  private Instances m_Instances;
 
  /** 
   * Flag that can be set to signalize that the KDTree is not valid anymore for the 
   * dataset given, 
   * the flag is false after the tree is newly 
   * initialized and therefore empty 
   * or if the dataset has changed in a way that couldn't be followed up for 
   * the KDTree, for instance if an instance was deleted.
   **/
  private boolean m_Valid = false;

  /* minimal relative width of a KDTree rectangle */ 
  double m_MinBoxRelWidth = 1.0E-2;

  /** maximal number of leaves in a KDTree */
  // int m_MaxLeafNumber = 100; not yet implemented

  /** maximal number of instances in a leaf */
  int m_MaxInstInLeaf = 40;

  /** This will cache the pruning value for future use. */
  private double m_pruneValue = Double.NaN; //todo not used

  /**
   * Index in ranges for LOW and HIGH and WIDTH
   */
  public static int R_MIN = 0;
  public static int R_MAX = 1;
  public static int R_WIDTH = 2;

  /**
   * Default Constructor
   */
  public KDTree() {
  }

  /**
   * Constructor, copies all options from an existing KDTree.
   * @param tree the KDTree to copy from
   */
  public KDTree(KDTree tree) {
    m_Universe = tree.m_Universe;
    m_Instances = tree.m_Instances;
    m_MinBoxRelWidth = tree.m_MinBoxRelWidth; 
    m_MaxInstInLeaf = tree.m_MaxInstInLeaf;
    m_Valid = tree.m_Valid; 
  }

  /**************************************************************************
   *
   * A class for storing a KDTree node.
   *
   **************************************************************************/
  private class KDTreeNode implements Serializable {

    /** node number (only for debug) */
    private int m_NodeNumber;

    /** left subtree; contains instances with smaller or equal to split value. */
    private KDTreeNode m_Left = null;
    
    /** right subtree; contains instances with larger than split value. */
    private KDTreeNode m_Right = null;
    
    /** value to split on. */
    private double m_SplitValue;
    
    /** attribute to split on. */
    private int m_SplitDim;

    /**
     * Every subtree stores the beginning index and the end index of the range
     * in the main instancelist, that contains its own instances
     */
    private int m_Start = 0;
    private int m_End = 0;
    
    /*
     * lowest and highest value and width (= high - low) for each
     * dimension
     */
    private double[][] m_NodeRanges;

    /**
     * Gets the splitting dimension.
     * @return splitting dimension
     */
    public int getSplitDim() {
      return m_SplitDim;
    }
    
    /**
     * Gets the splitting value.
     * @return splitting value
     */
    public double getSplitValue() {
      return m_SplitValue;
    }

    /**
     * Checks if node is a leaf.
     * @return true if it is a leaf
     */
    public boolean isALeaf () {
      return (m_Left == null);
    }  

    /**
     * Tidies up after delete. This means it changes start and end ranges. 
     * @param deleted index of the already deleted instance
     */
    public void tidyUpAfterDelete(int deleted) {
    
      boolean changed = false;

      // test, if one of the childrens last instance gets deleted
      if (!isALeaf()) {
	boolean deleteLastOne = false;
	if ((m_Left.m_Start == deleted) && (m_Left.m_End == deleted)) {
	  deleteLastOne = true;
	}
	if ((m_Right.m_Start == deleted) && (m_Right.m_End == deleted)) {
	  deleteLastOne = true;
	}

	if (deleteLastOne) {
	  // make a leaf
	  m_Right = null;
	  m_Left = null;
	}
      }

      // test if start or/and end needs to be changed
      if (deleted <= m_End) {
	m_End--;
	changed = true;
	if (deleted < m_Start) {
	  m_Start--;
	}
      }

      if (changed) {
	// prepare local instance list to work with
	int numInst = m_End - m_Start + 1;
	int [] instList = new int[numInst];
	int index = 0;
	for (int i = m_Start; i <= m_End; i++) {
	  instList[index++] = m_InstList.get(i);
	}

	// set ranges and split parameter
	m_NodeRanges = m_Instances.initializeRanges(instList);
      }
    }

    /**
     * Makes a KDTreeNode. 
     * Use this, if ranges are already defined.
     * @param num number of the current node 
     * @param ranges the ranges 
     * @param start start index of the instances
     * @param end index of the instances
     * @exception thrown if instance couldn't be retrieved
     */
    private void makeKDTreeNode(int[] num, double[][] ranges, int start,
			       int end) throws Exception {
      m_NodeRanges = ranges;
      makeKDTreeNode(num, start, end);
    }

    /**
     * Makes a KDTreeNode. 
     * @param num the node number 
     * @param start the start index of the instances in the index list
     * @param end the end index of the instances in the index list
     * @exception thrown if instance couldn't be retrieved
     */
    private void makeKDTreeNode(int [] num, 
		     	        int start,
			        int end) throws Exception {
      num[0]++;
      m_NodeNumber =  num[0];
      m_Start = start;
      m_End = end;
      m_Left = null;
      m_Right = null;
      m_SplitDim = -1;
      m_SplitValue = -1;

      double relWidth = 0.0;
      boolean makeALeaf = false;
      int numInst = end - start + 1;

      // if number of instances is under a maximum, then the node is a leaf
      if (numInst <= m_MaxInstInLeaf) {
	makeALeaf = true;
      }

      // prepare local instance list to work with
      int [] instList = new int[numInst];
      int index = 0;
      for (int i = start; i <= end; i++) {
	instList[index++] = m_InstList.get(i);
      }

      // set ranges and split parameter
      if (m_NodeRanges == null)
	m_NodeRanges = m_Instances.initializeRanges(instList);

      // set outer ranges
      if (m_Universe == null) {
	m_Universe = m_NodeRanges;
      }

      
      m_SplitDim = widestDim(m_Normalize);
      if (m_SplitDim >= 0) {
	m_SplitValue = splitValue(m_SplitDim);
	// set relative width
	relWidth = m_NodeRanges[m_SplitDim][R_WIDTH] 
                   / m_Universe[m_SplitDim][R_WIDTH];
      }

      /* calculate bias 
      double bias = 0.0;
      for (int i = 0; i < m_Instances.numAttributes(); i++) {
	double tmp =  m_NodeRanges[i][R_WIDTH] / m_Universe[i][R_WIDTH];
	bias += tmp * tmp;
      }
      m_Bias = bias * numInst;
      */

      // check if thin enough to make a leaf
      if (relWidth <= m_MinBoxRelWidth) {
	makeALeaf = true;
      }
      
      // split instance list into two  
      // first define which one have to go left and right..
      int numLeft = 0;
      boolean [] left = new boolean[numInst];
      if (!makeALeaf) {
	numLeft = checkSplitInstances(left, instList,
				      m_SplitDim, m_SplitValue);
      
	// if one of the sides would be empty, make a leaf
	// which means, do nothing
	if ((numLeft == 0) || (numLeft == numInst)) {
	  makeALeaf = true; 
	  //OOPS("makeKDTreeNode: " + m_NodeNumber 
          //     + " one side was empty after split");
	}
      }

      if (makeALeaf) {
	//TODO I think we don't need any of the following:
	// sum = 
	// sum is a row vector that has added up all rows 
	// summags =
	// is one double that contains the sum of the scalar product
	// of all row vectors with themselves
      } else {
	// and now really make two lists
	int [] leftInstList = new int[numLeft];
	int [] rightInstList = new int[numInst - numLeft];
	int startLeft = start;
	int startRight = start + numLeft;
	splitInstances(left, instList, startLeft, startRight);

	/**
	for (int i = 0; i < m_InstList.length(); i++) {
          if (i == startLeft) System.out.print(" /LE/ "); 
          if (i == startRight) System.out.print(" /RI/ "); 
	  System.out.print(" / " + m_InstList.get(i));
        }

        int[] debugInstList = new int[m_InstList.length()];
	for (int i = 0; i < debugInstList.length; i++) {
	  debugInstList[i] = m_InstList.get(i);
	}
	boolean [] debugleft = new boolean[m_InstList.length()];
	int debugnumLeft = checkSplitInstances(debugleft, debugInstList,
				      m_SplitDim, m_SplitValue);
	boolean first = true;
	for (int i = start; i < end; i++) {
          if (first && !debugleft[i]) {
	    first = false;

	  }
	  System.out.print(" " + debugleft[i]);
	}
	OOPS(" ");

	end debug **/
       

	// make left subKDTree  
	int endLeft = startLeft + numLeft - 1;
	m_Left = new KDTreeNode();    
	m_Left.makeKDTreeNode(num, startLeft, endLeft);

	// m_Sum += m_Left.getSum(); 
	// m_SumMags += m_Left.getSumMags();

	// make right subKDTree
	int endRight = end;
	m_Right = new KDTreeNode(); 
	m_Right.makeKDTreeNode(num, startRight, endRight);
	// m_Sum += m_Right.getSum(); 
	// m_SumMags += m_Right.getSumMags();      
      }
    }

    /**
     * Returns the widest dimension.
     * @param normalize if true normalization is used
     * @return attribute index that has widest range
     */
    private int widestDim(boolean normalize) {
      
      double widest = 0.0;
      int w = -1;
      if (normalize) {
	for (int i = 0; i < m_NodeRanges.length; i++) {
	  double newWidest = m_NodeRanges[i][R_WIDTH] / m_Universe[i][R_WIDTH];
	  if (newWidest > widest) {
	    widest = newWidest;
	    w = i;
	  }
	}
      }
      else {
	for (int i = 0; i < m_NodeRanges.length; i++) {
	  if (m_NodeRanges[i][R_WIDTH] > widest) {
	    widest = m_NodeRanges[i][R_WIDTH];
	    w = i;
	  }
	}
      }
      return w;
    }
    
    /**
     * Returns the split value of a given dimension.
     * @param dim dimension where split happens
     * @return the split value
     */
    private double splitValue(int dim) {
      
      double split = m_DistanceFunction.getMiddle(m_NodeRanges[dim]);
      // split = m_NodeRanges[dim][R_MIN] + m_NodeRanges[dim][R_WIDTH] * 0.5;
      return split;
    }

    /**
     * Add an instance to the node or subnode. Returns false if adding cannot 
     * be done.
     * Looks for the subnode the instance actually belongs to.
     * Corrects the end boundary of the instance list by coming up
     * @param instance the instance to add
     * @return true if adding was done
     **/
    public boolean addInstance(Instance instance) throws Exception {
      
      boolean success = false;
      if (!isALeaf()) {
	// go further down the tree to look for the leaf the instance should be in
	double instanceValue = instance.value(m_SplitDim);
	boolean instanceInLeft = instanceValue <= m_SplitValue;
	if (instanceInLeft) {
	  success = m_Left.addInstance(instance);
	  if (success) {
	    // go into right branch to correct instance list boundaries
	    m_Right.afterAddInstance();
	  }
	}
	else {
	  success = m_Right.addInstance(instance);
	}

	// instance was included
	if (success) {
	  // correct end index of instance list of this node
	  m_End++;
	  // correct ranges
	  m_NodeRanges = Instances.updateRanges(instance, m_NodeRanges);
	}
        
      }
      else { // found the leaf to insert instance

        // ranges have been updated 
 
	m_NodeRanges = Instances.updateRanges(instance, m_NodeRanges);

	int index = m_Instances.numInstances() - 1;
	m_InstList.squeezeIn(m_End, index);
	m_End++; 

	int numInst = m_End - m_Start + 1;

	// leaf did get too big?
	if (numInst > m_MaxInstInLeaf) {
	  //split leaf
	  int [] num = new int[1];
	  num[0] = m_NodeNumber;
	  this.makeKDTreeNode(num, m_NodeRanges, m_Start,
			      m_End);
        }
	success = true;
      }
      return success;
    }
    
    /**
     * Corrects the boundaries of all nodes to the right of the leaf where 
     * the instance was added to.
     **/
    public void afterAddInstance() {

      m_Start++;
      m_End++;      
      if (!isALeaf()) {
	m_Left.afterAddInstance();
	m_Right.afterAddInstance();
      }
    }

    /**
     * Deletes one instance in this KDTree node or its subsequent nodes.
     * @param index the index of the instance to be deleted
     * @return true if instance was deleted
     */
    public boolean deleteInstance(int index) throws Exception {

      int pos = m_InstList.deleteOneIndex(index);
      if (pos >= 0) {
	m_Root.tidyUpAfterDelete(pos);
	return true;
      } else {
	return false;
      }
    }

    /**
     * Returns statistics about the KDTree.
     * @param num give number of nodes
     * @param leaves give number of leaves
     * @return a text string that contains the statistics to the KDTree
     */
    public String statToString (boolean nodes, boolean leaves) {
      
      int count = 1;
      int stats[] = new int [2];
      if (this.m_Left != null) count = this.m_Left.collectStats(count, stats);
      if (this.m_Right != null) count = this.m_Right.collectStats(count, stats);
      
      StringBuffer text = new StringBuffer();
      if (nodes)
	text.append("\n  Number of nodes in the tree " + count + " \n");
      if (leaves)
	text.append("  Number of leaves in the tree " + stats[0] + " \n");
      return text.toString();
    }

    /**
     * Returns statistics about the KDTree.
     * @param count number of nodes so far
     * @param stats array with stats info 
     * @return the number of nodes 
     */
    public int collectStats (int count, int[] stats) {

      count++;
      if (this.m_Left != null) count = this.m_Left.collectStats(count, stats);
      if (this.m_Right != null) 
	count = this.m_Right.collectStats(count, stats); 
      else // is a leaf
	stats[0]++;
      return count;
    }

    /**
     * Returns the KDTree node and its underlying branches as string.
     * @param leaves adds the instances of the leaves
     * @return a string representing the node
     */
    public String nodeToString (boolean leaves) {
      StringBuffer text = new StringBuffer();
      text.append("NODE-Nr:          " + m_NodeNumber + "\n");
      int len = m_End - m_Start + 1;
      text.append("Num of instances: " + len + "\n");
      text.append("start " + m_Start + " == end " + m_End + "\n");
      if (!isALeaf()) {
	text.append("attribute: " + this.m_SplitDim);
	text.append("split at: " + this.m_SplitValue + "\n");
      }
      else {
	text.append("is a leaf\n");
	if (leaves) {
	  for (int i = m_Start; i <= m_End; i++) {
	    int instIndex = m_InstList.get(i);
            text.append(instIndex + ": ");
	    text.append(m_Instances.instance(instIndex).toString() + "\n");
	  } 
	}
      }
      text.append("------------------\n");
      if (this.m_Left != null) text.append(this.m_Left.nodeToString(leaves));
      if (this.m_Right != null) text.append(this.m_Right.nodeToString(leaves));
      return text.toString();
    }

    /**
     * Assigns instances to the current centers called candidates.
     * 
     * @param centers all the current centers
     * @param candidates the current centers the method works on
     * @param assignments the center index for each instance
     * @param pc the threshold value for pruning
     * @param p True if pruning should be used
     */
    private void determineAssignments(Instances centers,
		        	       int[] candidates,
				       int[] assignments,
				       double pc, boolean p) 
      throws Exception {
      
      // reduce number of owners for current hyper rectangle
      int [] owners = refineOwners(centers, candidates);
      
      // only one owner    
      if (owners.length == 1) {
	// all instances of this node are owned by one center
	for (int i = m_Start; i <= m_End; i++) {
	  assignments[m_InstList.get(i)] // the assignment of this instance
	    = owners[0]; // is the current owner
	}
      }
      else 
	if (!this.isALeaf()) {
	  // more than one owner and it is not a leaf
	  m_Left.determineAssignments(centers, owners, assignments, pc, p);
	  m_Right.determineAssignments(centers, owners, assignments, pc, p);
	}
	else {
	  // this is a leaf and there are more than 1 owner
	  //XMeans.
	  assignSubToCenters(m_NodeRanges, centers, owners, assignments);
	}
    }
    
    /**
     * Refines the ownerlist.
     * @param centers all centers
     * @param candidates the indexes of those centers that are candidates
     * @return list of owners
     */
    private int [] refineOwners(Instances centers, int [] candidates) 
      throws Exception {
      
      int [] owners = new int [candidates.length];
      double minDistance = Double.MAX_VALUE;
      int ownerIndex = -1;
      Instance owner;
      int numCand = candidates.length;
      double [] distance = new double[numCand];
      boolean [] inside = new boolean[numCand];
      for (int i = 0; i < numCand; i++) {
	distance[i] = distanceToHrect(centers.instance(candidates[i]));
	inside[i] = (distance[i] == 0.0);
	if (distance[i] < minDistance) {
	  minDistance = distance[i];
	  ownerIndex = i;
	}
      }
      owner = new Instance(centers.instance(candidates[ownerIndex]));
      
      // are there other owners
      // loop also goes over already found owner, keeps order 
      // in owner list
      int index = 0;
      for (int i = 0; i < numCand; i++) {
	// 1.  all centers that are points within rectangle are owners
	if ((inside[i]) 
	    
	    // 2. take all points with same distance to the rect. as the owner 
	    || (distance[i] == distance[ownerIndex]))
	  {
	    
	    // add competitor to owners list
	    owners[index++] = candidates[i];
	  }
	else {
	  
	  Instance competitor = new Instance(centers.instance(candidates[i]));
	  if 
	    
	    // 3. point has larger distance to rectangle but still can compete 
	    // with owner for some points in the rectangle
	    (!candidateIsFullOwner(owner, competitor)) 
	    
	    {
	      // also add competitor to owners list
	      owners[index++] = candidates[i];
	    }
	}
      }
      int [] result = new int [index];
      for (int i = 0; i < index; i++) result[i] = owners[i];
      return result;
    }
    
    /*
     * Returns true if candidate is a full owner in respect to a
     * competitor.<p>
     *
     * The candidate has been the closer point to the current rectangle 
     * or even has been a point within the rectangle.
     * The competitor is competing with the candidate for a few points 
     * out of the rectangle although it is a point further away 
     * from the rectangle then the candidate.
     * The extrem point is the corner of the rectangle that is furthest 
     * away from the candidate towards the direction of the competitor.
     *
     * If the distance candidate to this extreme point is smaller
     * then the distance competitor to this extreme point, then it is 
     * proven that none of the points in the rectangle can be owned be 
     * the competitor and the candidate is full owner of the rectangle 
     * in respect to this competitor. 
     * See also D. Pelleg and A. Moore's paper 'Accelerating exact k-means 
     * Algorithms with Geometric Reasoning'. <p>
     *
     * @param candidate instance that is candidate to be owner
     * @param competitor instance that competes against the candidate
     * @return true if candidate is full owner
     */
    private boolean candidateIsFullOwner(Instance candidate, 
					 Instance competitor) 
      throws Exception {
      // get extreme point
      Instance extreme = new Instance(candidate);
      for (int i = 0; i < m_Instances.numAttributes(); i++) {
	if ((competitor.value(i) - candidate.value(i)) > 0) {
	  extreme.setValue(i, m_NodeRanges[i][R_MAX]);
	}
	else {
	  extreme.setValue(i, m_NodeRanges[i][R_MIN]);
	}
      }
      boolean isFullOwner = 
	//
	m_DistanceFunction.distance(extreme, candidate) <
	m_DistanceFunction.distance(extreme, competitor);
      
      return isFullOwner;
    }

    /**
     * Returns the distance between a point and an hyperrectangle.
     * @param x the point
     * @return the distance
     */
    private double distanceToHrect(Instance x) throws Exception {
      double distance = 0.0;
      
      Instance closestPoint = new Instance(x); 
      boolean inside;
      inside = clipToInsideHrect(closestPoint);
      if (!inside)
	distance = m_DistanceFunction.distance(closestPoint, x);
      return distance;
    } 
    
    
    /**
     * Finds the closest point in the hyper rectangle to a given point.
     * Change the given point to this closest point by clipping of 
     * at all the dimensions to be clipped of.
     * If the point is inside the rectangle it stays unchanged.
     * The return value is true if the point was not changed, so the
     * the return value is true if the point was inside the rectangle.
     *
     * @param x a point
     * @return true if the input point stayed unchanged.
     */
    private boolean clipToInsideHrect(Instance x) {
      boolean inside = true; 
      for (int i = 0; i < m_Instances.numAttributes(); i++) {
	//TODO treat nominals differently!??
	
	if (x.value(i) < m_NodeRanges[i][R_MIN]) {
	  x.setValue(i, m_NodeRanges[i][R_MIN]);
	  inside = false;
	}
	else if (x.value(i) > m_NodeRanges[i][R_MAX]) {
	  x.setValue(i, m_NodeRanges[i][R_MAX]);
	  inside = false;
	}
      }
      return inside;
    }

    /**
     * Assigns instances of this node to center. Center to be assign to
     * is decided by the distance function.
     *
     * @param ranges min's and max's of attributes
     * @param centers all the input centers
     * @param centList the list of centers to work with
     * @param assignments index list of last assignments
     */
    public void assignSubToCenters(double [][] ranges,
				   Instances centers, 
				   int [] centList, 
				   int [] assignments) 
      throws Exception {
      //todo: undecided situations
      int numCent = centList.length;
      
      // WARNING:  assignments is "input/output-parameter"
      // should not be null and the following should not happen
      if (assignments == null) {
	assignments = new int[m_Instances.numInstances()];
	for (int i = 0; i < assignments.length; i++) {
	  assignments[i] = -1;
	}
      }

      // set assignments for all instances of this node
      for (int i = m_Start; i <= m_End; i++) {
	int instIndex = m_InstList.get(i);
	Instance inst = m_Instances.instance(instIndex);
	//if (instList[i] == 664) System.out.println("664***");
	int newC = m_DistanceFunction.closestPoint(inst, centers, centList);
	// int newC = clusterProcessedInstance(inst, centers);
	assignments[instIndex] = newC;
      }
    }

    /**
     * Find k nearest neighbours to target by simply searching through all instances
     * in the leaf.
     * No check on missing class.
     * @param target the instance to find nearest neighbour for
     * @param minDist the minimal distance found so far
     * @return the minimal distance found
     */
    public double simpleKNearestNeighbour(Instance target, 
					  double minDist) 
      throws Exception {
      double dist = 0;
      int currIndex;
      // sets and uses:
      //   double m_MinDist
      //   double m_MaxMinDist
      //   int m_FurthestNear
      int i = 0;
      int index = m_Start;

      // if no instances, return max value as distance
      if (m_End < m_Start) 
	return Double.MAX_VALUE;
      
      if (m_NearestListLength == 0) {
	for (;(index <= m_End) && (i < m_kNN);) {
          currIndex = m_InstList.get(index);
	  Instance trainInstance = m_Instances.instance(m_InstList.get(index));

	  if (target != trainInstance) { // for hold-one-out cross-validation
	    dist = m_DistanceFunction.distance(target, trainInstance);
	    m_NearestList[i] = currIndex;
	    m_DistanceList[i] = dist;
	    if (dist < minDist) minDist = dist; 
	    i++;
	  }
	  index++;
	}
	m_NearestListLength = m_kNN; 
      }
      
      /*
	System.out.print("dist: ");
      for (int j = 0; j < m_kNN; j++) {
	System.out.print(" " + m_DistanceList[j]);
      }
      System.out.println();
      System.out.print("near: ");
      for (int j = 0; j < m_kNN; j++) {
	System.out.print(" " + m_NearestList[j]);
      }
      System.out.println();
      */
      
      // set the new furthest nearest
      m_FurthestNear = checkFurthestNear();
      m_MaxMinDist = m_DistanceList[m_FurthestNear];

      // check all or rest of instances if nearer
      for (; index <= m_End; index++) {
	
      	currIndex = m_InstList.get(index);
	Instance trainInstance = m_Instances.instance(currIndex);
	if (target != trainInstance) { // for hold-one-out cross-validation
	  dist = m_DistanceFunction.distance(target, trainInstance);
	  
	  // is instance one of the nearest?
	  if (dist < m_MaxMinDist) {

	    // set instance as one of the nearest, 
	    // replacing the last furthest nearest
	    m_NearestList[m_FurthestNear] = currIndex;
	    m_DistanceList[m_FurthestNear] = dist;
	    
	    if (m_MultipleFurthest) {
	      // remove multiple entries of old furthest nearest
	      m_NearestListLength = m_kNN;
	      m_MultipleFurthest = false;
	    }
	    
	    // set the new furthest nearest
	    m_FurthestNear = checkFurthestNear();
	    m_MaxMinDist = m_DistanceList[m_FurthestNear];

	    // minimal value of distances did change too
	    if (dist < minDist)
	      minDist = dist;
	  }
	  else {
	    if (dist == m_MaxMinDist) {
	      // instance is at same distance as furthest nearest
	      m_MultipleFurthest = true;
	      m_NearestList[m_NearestListLength] = currIndex;
	      m_DistanceList[m_NearestListLength] = dist;
	      m_NearestListLength++;
	    }
	  }
	}
      }
      return minDist;
    }
    
    /**
     * Finds the nearest neighbour to target, this method is called recursively.
     * @param target the instance to find nearest neighbour for
     * @param maxDist the distance to the nearest neighbour so far
     * @return the minimal distance found
     */
    private double kNearestNeighbour(Instance target, 
				     double maxDist) throws Exception {
      double newDist;
      KDTreeNode nearer, further;

      // if is a leaf then the instance is in this hyperrectangle
      if (this.isALeaf()) {
        // return distance to nearest (and index of all
	// all k nearest in m_NearestList) 
	return this.simpleKNearestNeighbour(target, maxDist);
      }
      boolean targetInLeft = m_DistanceFunction.valueIsSmallerEqual(
	     target, 
	     m_SplitDim,
             m_SplitValue);

      if (targetInLeft) {
	nearer = m_Left;
	further = m_Right;
      } else {
	nearer = m_Right;
	further = m_Left;
      }
      // look for nearer neighbours in nearer half
      maxDist = nearer.kNearestNeighbour(target, maxDist); 
      
      // ... now look in further half if maxDist reaches into it
      Instance splitPoint = new Instance(target);
      splitPoint.setValue(m_SplitDim, m_SplitValue);
      double distanceToSplit = m_DistanceFunction.distance(target, splitPoint);
      boolean lookInSecondHalf = maxDist >= distanceToSplit;

      if (lookInSecondHalf) {
	// look for nearer neighbours in further half 
	maxDist = further.kNearestNeighbour(target, maxDist);
      } 
      return maxDist;
    }

  } 
  // 
  // End of class KDTreeNode

  /**
   * Adds one instance to KDTree loosly. It only changes the ranges. The ranges are
   * important for the distance function.
   * @param instance the new instance
   */
  public void addLooslyInstance(Instance instance) {
    m_DistanceFunction.updateRanges(instance);
  }

  /**
   * Builds the KDTree.
   * @param instances instances to build the tree of
   */
  public void buildKDTree(Instances instances) 
  throws Exception {

    double [][] ranges = instances.initializeRanges();
    buildKDTree(instances, ranges);
  }

  /**
   * Builds the KDTree.
   * It is adviseable to run the replace missing attributes filter on the
   * passed instances first.
   * @param instances instances to build the tree of
   * @ranges the ranges of this instances
   */
  public void buildKDTree(Instances instances,
			  double [][] ranges) throws Exception {

    m_Instances = instances;
    int numInst = m_Instances.numInstances();

    // Make the global index list 
    m_InstList = new DynamicArrayOfPosInt(numInst);
 
    for (int i = 0; i < numInst; i++) {
      m_InstList.set(i, i);
    }

    // make the tree starting with the roor node
    m_Root = new KDTreeNode(); 
    if (ranges == null) {
      ranges = instances.initializeRanges();
    }

    // set global ranges
    m_Universe = ranges;

    // set distance function
    m_DistanceFunction = new weka.core.EuclideanDistance(m_Instances, m_Universe);
    checkInstances();


    // build the tree 
    int [] num = new int[1];
    num[0] = 0;
    m_Root.makeKDTreeNode(num,
			  ranges, 
                          0,            // index of first instance index
                          numInst - 1); // index of last instance index
    // tree is valid for instances
    m_Valid = true;
  }

  /**
   * Checks the instances.
   * No checks in this KDTree but it calls the check of the distance function.
   */
  private void checkInstances () throws Exception {
    
    m_DistanceFunction.checkInstances();
  }


  /**
   * Adds one instance to the KDTree.
   * @param instance the instance to be added 
  */
  public void updateKDTree(Instance instance) 
  throws Exception {

    boolean success = m_Root.addInstance(instance);
    if (!success) {
      // make a new tree
      double [][] dummyRanges = null; 
      buildKDTree(m_Instances, dummyRanges);
    }
  }

  /**
   * Deletes one instance in the KDTree.
   * @param instance the instance to be deleted
   */
  public void deleteInstance(Instance instance) 
  throws Exception {
    int index =0;
    //    for (int 
    deleteInstance(index);
  }

  /**
   * Deletes one instance in the KDTree.
   * @param index the index of the instance to be deleted
   * @return true if instance was deleted
   */
  public boolean deleteInstance(int index) throws Exception {
    
    boolean success = false;
    int pos = m_InstList.deleteOneIndex(index);
    if (pos >= 0) {
      m_Root.tidyUpAfterDelete(pos);
      success =  true;
    } 
    if (!success) {
      // make a new tree
      double [][] dummyRanges = null; 
      buildKDTree(m_Instances, dummyRanges);
    }
    return success;
  }

  /**
   * toString  
   * @return string representing the tree
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    KDTreeNode tree = m_Root;
    int[] num = new int[1];
    num[0] = 0;
    // index list in string format:
    //for (int i = 0; i <= m_InstList.length(); i++) {
    //  int instIndex = m_InstList.get(i);
    //  text.append(instIndex + "/ ");
    //} 

    text.append("\nKDTree build:");
    text.append(tree.statToString(true, true));
    // tree in string format:
    text.append(tree.nodeToString(true));
    return text.toString();
  }  

  /**
   * Assigns instances to centers using KDTree. 
   * 
   * @param centers the current centers
   * @param assignments the centerindex for each instance
   * @param pc the threshold value for pruning.
   * @param p True if pruning should be used.
   */
  public void centerInstances(Instances centers, int [] assignments,
				double pc) throws Exception {
    
    int [] centList = new int[centers.numInstances()];
    for (int i = 0; i < centers.numInstances(); i++)
      centList[i] = i;

    m_Root.determineAssignments(centers, centList, 
			 assignments, pc, m_Prune);
  }
  
  /**
   * Used for debug println's.
   * @param output string that is printed
   */
  private void OOPS(String output) {
    System.out.println(output);
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   * @param r the ranges for each attribute
   */
  private double norm(double x, int i, double[][] r) {

    if (Double.isNaN(r[i][0]) || (r[i][1]) == 0) {
      return 0;
    } else {
      return (x - r[i][0]) / (r[i][1]);
    }
  }

  /**
   * Returns array of boolean set true or false if instance is part
   * of next left kdtree.
   * @param left list of boolean values, true if instance belongs to left
   * @param instList list of indexes of instances of this node
   * @param splitDim index of splitting attribute
   * @param splitValue value at which the node is split
   * @return number of instances that belong to the left
   */
  private int checkSplitInstances(boolean [] left,
				  int [] instList,
				  int splitDim, double splitValue) {

    // length of left should be same as length of instList
    int numLeft = 0;
    for (int i = 0; i < instList.length; i++) {
      // value <= splitValue
      if (m_DistanceFunction.valueIsSmallerEqual(
	     m_Instances.instance(instList[i]), 
	     splitDim,
             splitValue)) {
	left[i] = true;
        numLeft++;
      } else {
	left[i] = false;
      }
    }
    return numLeft;
  }

  /**
   * Sorts instances newly into left and right part.
   * @param left list of flags, set true by this method if instance
   * should go to the left follow node
   * @param instList list of instances of this node
   * @param iLeft index to the left 
   * @param iLeft index to the left 
   */
  private void splitInstances(boolean [] left,
			      int [] instList, 
			      int iLeft,
			      int iRight) {

    for (int i = 0; i < instList.length; i++) {
      if (left[i]) {
	m_InstList.set(iLeft++, instList[i]);
      } else {
	m_InstList.set(iRight++, instList[i]);
      }
    }
  }


  /** -------------------------------------------------------------------------------- 
   ** variables for nearest neighbour search 
   *  --------------------------------------------------------------------------------*/

  /** index/indices of current target */
  private int [] m_NearestList;

  /** length of nearest list (can be larger than k) */
  private int m_NearestListLength = 0;

  /** true if more than of k nearest neighbours */
  private boolean m_MultipleFurthest = false;

  /** number of nearest neighbours k */
  private int m_kNN = 0; 

  /** distance to current nearest neighbour */
  private double m_MinDist = Double.MAX_VALUE;

  /** distance to current furthest of the neighbours */
  private double m_MaxMinDist = Double.MAX_VALUE;

  /** index of the furthest of the neighbours in m_NearestList */
  private int m_FurthestNear = 0;

  /** distance to current nearest neighbour */
  private double [] m_DistanceList;

  /**
   * Find k nearest neighbours to target. This is the main method.
   *
   * @param target the instance to find nearest neighbour for
   * @param maxDist the distance to the nearest neighbor so far
   * @param nearest the index of the nearest neighbor (second return value)
   */
  public int findKNearestNeighbour(Instance target,
                                   int kNN,
                                   int [] nearestList,
                                   double [] distanceList) 
    throws Exception {
    m_kNN = kNN;
    double maxDist = Double.MAX_VALUE;
    m_NearestList = nearestList;
    m_DistanceList = distanceList;
    m_NearestListLength = 0;
    int[] num = new int[1]; 
    num[0] = 0;
    double minDist = m_Root.kNearestNeighbour(target, maxDist);
    return m_NearestListLength;
  }

  /**
   * Get the distance of the furthest of the nearest neighbour 
   * the index of this instance 
   * in the index list.
   * @param nearest index of k nearest instances
   */
  private int checkFurthestNear() {
    double max = 0.0;
    int furthestNear = 0;
    for (int i = 0; i < m_kNN; i++) {
      if (m_DistanceList[i] > max) {
	max = m_DistanceList[i];
	furthestNear = i;
      } 
    }
    return furthestNear;
  }

  /**
   * the GET and SET - functions ===============================================
   **/

  /**
   * Sets KDTree to be valid for dataset in m_Instances.
   * @param flag if KDtree is valid 
   */
  public void setValid(boolean valid) {
    m_Valid = valid;
  }

  /**
   * Returns true if valid flag is true.
   * @return flag if KDtree is valid 
   */
  public boolean isValid() {
    return m_Valid;
  }

  /**
   * Gets number of instances in KDTree.
   * @return number of instances
   */
  public int numInstances() {
    return m_Instances.numInstances();
  }

  /**
   * Gets instances used in the tree.
   * @return model information
   */
  public Instances getInstances() {
    return m_Instances;
  }

  /**
   * Gets instance list  used in the tree.
   * @return instance list
   */
  public DynamicArrayOfPosInt getInstList() {
    return m_InstList;
  }


  /**
   * Gets the distance function specification string, 
   * which contains the class name of distance function
   * the filter and any options to the filter
   *
   * @return the filter string.
   */
  protected String getDistanceFunctionSpec() {
    
    DistanceFunction c = getDistanceFunction();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Sets the distance function.
   * @param distanceF the distance function with all options set
   */
  public void setDistanceFunction(DistanceFunction distanceF) {
    m_DistanceFunction = distanceF;
  }

  /**
   * Gets the distance function.
   * @return the distance function
   */
  public DistanceFunction getDistanceFunction() {
    return m_DistanceFunction;
  }

  /**
   * Sets the minimum relative box width.
   * @param i the minimum relative box width
   */
  public void setMinBoxRelWidth(double i) throws Exception {
    m_MinBoxRelWidth = i;
  }

  /**
   * Gets the minimum relative box width.
   * @return the minimum relative box width
   */
  public double getMinBoxRelWidth() {
    return  m_MinBoxRelWidth;
  }

  /**
   * Sets the maximum number of instances in a leaf.
   * @param i the maximum number of instances in a leaf
   */
  public void setMaxInstInLeaf(int i) throws Exception {
    m_MaxInstInLeaf = i;
  }

  /**
   * Get the maximum number of instances in a leaf.
   * @return the maximum number of instances in a leaf
   */
  public int getMaxInstInLeaf() {
    return  m_MaxInstInLeaf;
  }

  /**
   * Sets the flag for pruning of the blacklisting algorithm.
   * @param p true to use pruning.
   */
  public void setPrune(boolean p) {
      m_Prune = p;
  }

  /**
   * Gets the pruning flag.
   * @return True if pruning
   */
  public boolean getPrune() {
    return m_Prune;
  }

  /**
   * Sets the flag for normalizing the widths of a KDTree Node by the width
   * of the dimension in the universe. 
   * @param n true to use normalizing.
   */
  public void setNormalize(boolean n) {
      m_Normalize = n;
  }

  /**
   * Gets the normalize flag.
   * @return True if normalizing
   */
  public boolean getNormalize() {
    return m_Normalize;
  }

  /**
   * Sets the debug level.
   * debug level = 0, means no output
   * @param d debuglevel
   */
  public void setDebugLevel(int d) {
    m_DebugLevel = d;
  }

  /**
   * Gets the debug level.
   * @return debug level
   */
  public int getDebugLevel() {
    return m_DebugLevel;
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);
    newVector.addElement(new Option(
				    "\tPruning will be done\n"
				    +"\t(Use this to prune).",
				    "P", 0,"-P"));
    newVector.addElement(new Option(
				    "\tSet minimal width of a box\n"
				    +"\t(default = 1.0E-2).",
				    "W", 0,"-W <value>"));
    newVector.addElement(new Option(
				    "\tMaximal number of instances in a leaf\n"
				    +"\t(default = 40).",
				    "L", 0,"-L"));
    newVector.addElement(new Option(
				    "\tDistance function\n"
				    +"\t(default = Euclidean Distance).",
				    "D", 0,"-D"));
    newVector.addElement(new Option(
				    "\tNormalizing will be done\n"
				    +"\t(Select dimension for split, with normalising to universe).",
				    "N", 0,"-N"));
     return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options)
    throws Exception {

    if (Utils.getFlag('P', options)) {
      setPrune(true);
    } else {
      setPrune(false);
    }

    String optionString = Utils.getOption('W', options);
    if (optionString.length() != 0) {
      setMinBoxRelWidth(Double.parseDouble(optionString));
    }

    optionString = Utils.getOption('L', options);
    if (optionString.length() != 0) {
      setMaxInstInLeaf(Integer.parseInt(optionString));
    }

    String funcString = Utils.getOption('D', options);
    if (funcString.length() != 0) {
      String [] funcSpec = Utils.splitOptions(funcString);
      String funcName = funcSpec[0];
      funcSpec[0] = "";
      setDistanceFunction((DistanceFunction) Utils.forName(DistanceFunction.class,
							   funcName, funcSpec));
    }

    if (Utils.getFlag('N', options)) {
      setNormalize(true);
    } else {
      setNormalize(false);
    }

    optionString = Utils.getOption('U', options);
    int debugLevel = 0;
    if (optionString.length() != 0) {
      try {
	debugLevel = Integer.parseInt(optionString);
      } catch (NumberFormatException e) {
	throw new Exception(optionString +
                            "is an illegal value for option U"); 
      }
    }
    setDebugLevel(debugLevel);
  }

  /**
   * Gets the current settings of KDtree.
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[10];
    int current = 0;
    
    if (getPrune()) {
      options[current++] = "-P";
    }
    options[current++] = "-W";
    options[current++] = "" + getMinBoxRelWidth();
    options[current++] = "-L";
    options[current++] = "" + getMaxInstInLeaf();
    options[current++] = "-D";
    options[current++] = "" + getDistanceFunctionSpec();
    if (getNormalize()) {
      options[current++] = "-N";
    }
    int dL = getDebugLevel();
    if (dL > 0) {
      options[current++] = "-U";
      options[current++] = "" + dL;
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
  }

  /**
   * Main method for testing this class
   */
  public static void main(String [] args) {
    try {
      if (args.length < 1 ) {
	System.err.println("Usage : weka.gui.visualize.VisualizePanel "
			   +"<dataset> [<dataset> <dataset>...]");
	System.exit(1);
      }
    }catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
