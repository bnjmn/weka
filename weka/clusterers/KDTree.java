/*
 *    KDTree.java
 *    Copyright (C) 2000 Malcolm Ware
 *
 */

package weka.clusterers;
import  weka.core.*;
/** 
 * This is a KD-Tree structure used to contain and break instances based on
 * their attributes.
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class KDTree {

  /** 
   * contains the ranges of the universe = of the root node
   */
  private double[][] m_Universe;

  /** 
   * contains lowest and highest value and width = high - low for each 
   * dimension (=attribute)
   */
  private double[][] m_Ranges;

  /** The value to split on. */
  private double m_SplitValue;

  /** The attribute to split on. */
  private int m_SplitDim;

  /** Bias. 
   * TODO
   * used later to compare with cutoff??
   * calculate bias: 
   *      num-of-inst * (SUM(over dimensions)[hrect-width / universe-width])
   */
  private double m_Bias;
  private double m_Sum;
  private double m_SumMags;

  /** The left subtree (instances with smaller or equal to split value, or if
   * nominal attributes, those that equal the value). */
  private KDTree m_Left = null;
  
  /** The right subtree (instances with larger than split value, or if
   * nominal attributes, those that don't equal the value). */
  private KDTree m_Right = null;

  /** The indexes of the instances contained by this node. */
  private int[] m_InstList;

  /** Reference to the instances of the universe. */
  private Instances m_Instances;

  /** This can be used to cache the bounds in when a call to
   * certain functions is made. */
  private double[][] m_bounds;//todo not used


  /** This can be used to cache the totals for each attribute when
   * it's required by a center. */
  private double[][] m_totals; //todo not used

  /** This will cache the pruning value for future use. */
  private double m_pruneValue = Double.NaN;//todo not used


  /**
   * Index in ranges for LOW and HIGH and WIDTH
   */
  public static int R_LOW = 0;
  public static int R_HIGH = 1;
  public static int R_WIDTH = 2;

  /**
   * It is advisable to run the replace missing attributes filter on the
   * passed instances first.
   * @param i The set of instances to construct this tree from.
   * @param a The index of the attribute to use at this depth.
   */
  public KDTree(double [][] universe, Instances instances, int [] instList,
                double minBoxRelWidth, int maxLeafNum) throws Exception {

    m_Universe = universe;
    m_Instances = instances;
    m_Left = null;
    m_Right = null;
    //m_Leaf = null;
    //m_Totals = null;
    m_Sum = 0.0;
    m_SumMags = 0.0;
    m_SplitDim = -1;
    m_SplitValue = -1;
    double relWidth = 0.0; 

    int numInst = instList.length;
    if (numInst == 0) {
      throw new Exception("KDTree-Node contains no instances.");
    }
    
    // set local instance list
    m_InstList = new int[numInst];
    for (int i = 0; i < numInst; i++) {
      m_InstList[i] = instList[i];
    }

    // set ranges and split parameter
    m_Ranges = XMeans.initializeRanges(m_Instances, instList);
    if (m_Universe == null) (m_Universe = m_Ranges);

    m_SplitDim = widestDim();
    if (m_SplitDim > 0) {
      m_SplitValue = splitValue(m_SplitDim);
      // set relative width
      relWidth = m_Ranges[m_SplitDim][R_WIDTH] /
	m_Universe[m_SplitDim][R_WIDTH];
    }
    
    // calculate bias 
    double bias = 0.0;
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      double tmp =  m_Ranges[i][R_WIDTH] / m_Universe[i][R_WIDTH];
      bias += tmp * tmp;
    }
    m_Bias = bias * numInst;

    // check if thin enough to make a leaf
    boolean makeALeaf = false;
    if ((relWidth <= minBoxRelWidth) && (numInst <= maxLeafNum)) {
      makeALeaf = true;
    }

    // if not a leaf split instance list into two  
    // first define which one have to go left and right..
    int numLeft = 0;
    boolean [] left = new boolean[numInst];
    if (!makeALeaf) {
      // split instance list into two  
      // first define which one have to go left and right..
      numLeft = checkSplitInstances(left, instList, 
					m_SplitDim, m_SplitValue);
    }
    // if one of the sides would be empty, make a leaf
    // which means, do nothing
    //
    if ((numLeft == 0) || (numLeft == numInst)) {
      makeALeaf = true;
    }

    if (makeALeaf) {
      //TODO I think we don't need any of the following:
      // sum= 
      // sum is a row vector that has added up all rows 
      // summags =
      // is one double that contains the sum of the scalar product
      // of all row vectors with themselves
    } else {	
      // and now really make two lists
      int [] leftInstList = new int[numLeft];
      int [] rightInstList = new int[numInst - numLeft];
      splitInstances(left, instList, leftInstList, rightInstList);
      
      // make two new kdtrees       
      m_Left = new KDTree(m_Universe, m_Instances, leftInstList,
			  minBoxRelWidth, maxLeafNum);
      //m_Sum += m_Left.getSum(); 
      //m_SumMags += m_Left.getSumMags(); 
      m_Right = new KDTree(m_Universe, m_Instances, rightInstList,
			   minBoxRelWidth, maxLeafNum);
      //m_Sum += m_Right.getSum(); 
      //m_SumMags += m_Right.getSumMags(); 
      
    }
  }
  

  /**
   * isALeaf
   */
  public boolean isALeaf () {
    return (m_Left == null);
  }  

  /**
   * toString  
   */
  public String toString () {
    StringBuffer text = new StringBuffer();
    text.append(nodeToString(this, 0));
    return text.toString();
  }  

  /**
   * Returns the KDTree node and its underlying branches as string.
   * Numbers are assigned to the nodes.
   * @param kdt the root of the kdtree that is transformed to a string
   * @param num the number to start counting the nodes
   */
  public String nodeToString (KDTree kdt, int num) {
    num++;
    StringBuffer text = new StringBuffer();
    text.append("NODE-Nr:  " + num + "\n");
    text.append("value     " + kdt.m_SplitValue + "\n");
    text.append("attribute " + kdt.m_SplitDim + "\n");
    if (kdt.m_Right == null) {
      text.append("right     null\n");
    } else {
      text.append("right     LATER\n");
    }
    if (kdt.m_Left == null) {
      text.append("left      null\n");
    } else {
      text.append("left      NEXT\n");
    }
    text.append("inst " + m_InstList.length + "\n");
    text.append("------------------\n");
    if (kdt.m_Left != null) text.append(nodeToString(kdt.m_Left, num));
    if (kdt.m_Right != null) text.append(nodeToString(kdt.m_Right, num));
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
				double pc, boolean p) {
    
    int [] centList = new int[centers.numInstances()];
    for (int i = 0; i < centers.numInstances(); i++)
      centList[i] = i;

    determineAssignments(centers, centList, assignments, pc, p);
  }
  
  
  /**
   * Assigns instances to the current centers called candidates.
   * 
   * @param centers all the current centers
   * @param candidates the current centers the method works on
   * @param assignments the centerindex for each instance
   * @param pc the threshold value for pruning
   * @param p True if pruning should be used
   */
  private void  determineAssignments(Instances centers, 
				     int[] candidates,
				     int[] assignments,
				     double pc, boolean p) {

    // reduce number of owners for current hyper rectangle
    int [] owners = refineOwners(centers, candidates);

    // only one owner    
    if (owners.length == 1) {
      // all instances of this node are owned by one center
      for (int i = 0; i < m_InstList.length; i++) {
        assignments[m_InstList[i]] // the assignment of this instance
	  = owners[0]; // is the current owner
      }
    }
    else 
      if (!this.isALeaf()) {
      // more than one owner and it is not a leaf
	m_Left.determineAssignments(centers, owners,
				    assignments, pc, p);
        m_Right.determineAssignments(centers, owners,
				     assignments, pc, p);
      }
    else {
      // this is a leaf and there are more than 1 owner
      XMeans.assignSubToCenters(m_Ranges,
				centers,
				owners,
				m_Instances, 
				m_InstList, 
				assignments);
    }
  }

  /**
   * refine ownerlist
   *
   */
  private int [] refineOwners(Instances centers, int [] candidates) {
  
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
          // wit owner for some points in the rectangle
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
   * Returns true if candidate is full owner in respect to a
   * competitor
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
   */
  private boolean candidateIsFullOwner(Instance candidate, 
				       Instance competitor) {
  
    // get extreme point
    Instance extreme = new Instance(candidate);
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      if ((competitor.value(i) - candidate.value(i)) > 0) {
	extreme.setValue(i, m_Ranges[i][R_HIGH]);
      }
      else {
	extreme.setValue(i, m_Ranges[i][R_LOW]);
      }
    } 
    boolean isFullOwner = 
      XMeans.distance(m_Instances, m_Ranges, extreme, candidate) <
      XMeans.distance(m_Instances, m_Ranges, extreme, competitor);
    
    return isFullOwner;
  }

  /**
   * return the distance between a point and an hyper rectangle
   */
  private double distanceToHrect(Instance x) {
    double distance = 0.0;

    Instance closestPoint = new Instance(x); 
    boolean inside;
    inside = clipToInsideHrect(closestPoint);
    if (!inside)
      distance = XMeans.distance(m_Instances, m_Ranges, closestPoint, x);
    return distance;
  } 

  /**
   * Find the closest point in the hyper rectangle to a given point.
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

      if (x.value(i) < m_Ranges[i][R_LOW]) {
	x.setValue(i, m_Ranges[i][R_LOW]);
        inside = false;
      }
      else if (x.value(i) > m_Ranges[i][R_HIGH]) {
	x.setValue(i, m_Ranges[i][R_HIGH]);
	inside = false;
      }
    }
    return inside;
  }


  /**
   * This is used to recurse the tree and findout which center owns each
   * instance in the tree. This works by a process of elimination.
   * @param c The centers.
   * @param t The instances for each node.
   * @param b The bounds for the hyperRectangle at this point.
   * @param r The ranges for each attribute. This is used to normalize
   * the distance properly
   * @param pc The prune threshold value.
   * @param p True if pruning should be used.
   
  private void determineOwners(Instances c, Instances[] t, double[][] b,
			       double[][] r, double pc, boolean p) {


    double closestDistance = Double.POSITIVE_INFINITY;
    double temp;
    int closestCenter = 0;
    if (m_leaf == null && m_left == null) {
      return;
    }
    if (m_leaf != null) {
      for (int i = 0; i < c.numInstances(); i++) {
	temp = distance(c.instance(i), m_leaf, r);
	if (temp < closestDistance) {
	  closestDistance = temp;
	  closestCenter = i;
	}
      }
      for (int i = 0; i < m_instances.numInstances(); i++) {
	t[closestCenter].add(m_instances.instance(i));
      }
      return;
    }

    if (p) {
      //perform pruning test.
      if (prune(b, r, pc)) {
	//then everything lower is pruned to this node.
	Instance closestPoint;
	int count = 0; 
	for (int i = 0; i < c.numInstances(); i++) {
	  if (c.instance(i).weight() == 1) {
	    count++;
	  }
	}
	for (int i = 0; i < c.numInstances(); i++) {
	  if (c.instance(i).weight() == 1) {
	    closestPoint = findClosestPoint(c.instance(i), b);
	    for (int j = 0; j < m_instances.numInstances() / count; j++){
	      t[i].add(closestPoint);
	    }
	  }
	}
	return;
      }
    }

    Instance closestPoint;
    boolean multiple = false;
    for (int i = 0; i < c.numInstances(); i++) {
      if (c.instance(i).weight() == 1) {
	closestPoint = findClosestPoint(c.instance(i), b);
	temp = distance(closestPoint, c.instance(i), r);
	if (temp < closestDistance) {
	  multiple = false;
	  closestCenter = i;
	  closestDistance = temp;
	}
	else if (temp == closestDistance) {
	  multiple = true;
	}
      }
    }
    //now if multiple is true, I have to search lower nodes for an owner
    //otherwise I can see if the closest Center is an owner of this node
    //Although it could make the program run faster if I still do blacklisting
    //I suspect that the extra code may make it not worth while for something
    //that will probably never happen.
    if (!multiple) {
      boolean[] changes = determineDomination(closestCenter, c, b, r);
      boolean owner = true;
      for (int i = 0; i < c.numInstances(); i++) {
	if (c.instance(i).weight() != 0 && i != closestCenter) {
	  owner = false;
	}
      }
      if (owner) {
	for (int i = 0; i < m_instances.numInstances(); i++) {
	  t[closestCenter].add(m_instances.instance(i));
	}
      }
      else {
	if (m_instances.attribute(m_SplitDim).isNominal()) {
	  double[] current = new double[b[m_SplitDim].length];
	  for (int i = 0; i < b[m_SplitDim].length; i++) {
	    current[i] = b[m_SplitDim][i];
	    b[m_SplitDim][i] = 0;
	  }
	  b[m_SplitDim][(int)m_SplitValue] = 1;
	  m_left.determineOwners(c, t, b, r, pc, p);
	  for (int i = 0; i < b[m_SplitDim].length; i++) {
	    b[m_SplitDim][i] = current[i];
	  }
	  b[m_SplitDim][(int)m_SplitValue] = 0;
	  m_right.determineOwners(c, t, b, r, pc, p);
	  b[m_SplitDim][(int)m_SplitValue] = current[(int)m_SplitValue];
	  
	}
	else {
	  double store;
	  store = b[m_SplitDim][1];
	  b[m_SplitDim][1] = m_SplitValue;
	  m_left.determineOwners(c, t, b, r, pc, p);
	  b[m_SplitDim][1] = store;
	  store = b[m_SplitDim][0];
	  b[m_SplitDim][0] = m_SplitValue;
	  m_right.determineOwners(c, t, b, r, pc, p);
	  b[m_SplitDim][0] = store;
	}
      }
      for (int i = 0; i < c.numInstances(); i++) {
	if (changes[i]) {
	  c.instance(i).setWeight(1);
	}
      }
    }
    else {
      if (m_instances.attribute(m_SplitDim).isNominal()) {
	double[] current = new double[b[m_SplitDim].length];
	for (int i = 0; i < b[m_SplitDim].length; i++) {
	  current[i] = b[m_SplitDim][i];
	  b[m_SplitDim][i] = 0;
	}
	b[m_SplitDim][(int)m_SplitValue] = 1;
	m_left.determineOwners(c, t, b, r, pc, p);
	for (int i = 0; i < b[m_SplitDim].length; i++) {
	  b[m_SplitDim][i] = current[i];
	}
	b[m_SplitDim][(int)m_SplitValue] = 0;
	m_right.determineOwners(c, t, b, r, pc, p);
	b[m_SplitDim][(int)m_SplitValue] = current[(int)m_SplitValue];
	
      }
      else {
	double store;
	store = b[m_SplitDim][1];
	b[m_SplitDim][1] = m_SplitValue;
	m_left.determineOwners(c, t, b, r, pc, p);
	b[m_SplitDim][1] = store;
	store = b[m_SplitDim][0];
	b[m_SplitDim][0] = m_SplitValue;
	m_right.determineOwners(c, t, b, r, pc, p);
	b[m_SplitDim][0] = store;  
      } 
    }
  }
  

  /**
   * This determines if the pruning criteria for is met.
   * If so then then further descending should stop and 
   * an approximation should be done.
   * @param b The bounds for the hyper rectangle at this point.
   * @param r The bounds for the universal hyper rectangle.
   * @param c The threshold value
   * @return If the pruning criteria was met.
   
  private boolean prune(double[][] b, double[][] r, double c) {
    
    double ans = 0;
    if (Double.isNaN(m_pruneValue)) {
      for (int i = 0; i < m_instances.numAttributes(); i++) {
	if (m_instances.attribute(i).isNominal()) {
	  int v = 0, d = m_instances.attribute(i).numValues();
	  for (int j = 0; j < m_instances.attribute(i).numValues(); 
	       j++) {
	    if (b[i][j] == 1) {
	      v++;
	    }
	  }
	  ans += Math.pow(v / (double)d, 2);
	}
	else {
	  ans += Math.pow((b[i][1] - b[i][0]) / r[i][1], 2);
	}
      }
    
      ans *= m_instances.numInstances() / (double)m_instances.numAttributes();
      m_pruneValue = ans;
    }
    return m_pruneValue <= c;
  }

  /**
   * This determines if the chosen center is closer to all of the
   * hyper rectangle than the rest, and if so marks those centers as
   * dominated. To mark them as dominated, there weight is set to zero.
   * @param o The potential owner of this node.
   * @param c The centers.
   * @param b The bounds of the hyper rectangle.
   * @param r The ranges used for the distance function.
   * @return An array stating which centers were set to dominated.
   
  private boolean[] determineDomination(int o, Instances c, double[][] b,
					double[][] r) {
    
    Instance corner;
    double d1;
    double d2;
    boolean[] changed = new boolean[c.numInstances()];
    for (int i = 0; i < c.numInstances(); i++) {
      if (i != o && c.instance(i).weight() == 1) {
	corner = findMostBiasCorner(c.instance(i), c.instance(o), b);
	d1 = distance(corner, c.instance(i), r);
	d2 = distance(corner, c.instance(o), r);
	if (d1 > d2) {
	  c.instance(i).setWeight(0);
	  changed[i] = true;
	}
      }
    }
    /*boolean dom = false; 
    for (int i = 0; i < c.numInstances(); i++) {
      if (i != o && c.instance(i).weight() == 1) {
	for (int j = 0; j < Math.pow(2, c.numAttributes()); 
	     j++) {
	  corner = new Instance(c.instance(o));
	  corner.setDataset(m_instances);
	  for (int noc = 0; noc < c.numAttributes(); noc++) {
	    if ((j & (1 << noc)) == (1 << noc)) {
	      corner.setValue(noc, b[noc][1]);
	    }
	    else {
	      corner.setValue(noc, b[noc][0]);
	    }
	  }
	  dom = true;
	  d1 = distance(corner, c.instance(i), r);
	  d2 = distance(corner, c.instance(o), r);
	  if (d1 <= d2) {
	    dom = false;
	    break;
	  }
	}
       	if (dom) {
	  c.instance(i).setWeight(0);
	  changed[i] = true;
	}
      }
      }
  return changed;
}*/
  
  /**
   * This function determines which corner has the best chance of being
   * under control of the first center, when working against the second
   * center.
   * @param f The first center.
   * @param s The second center.
   * @param b The Ranges of the hyper cube.
   * @return An instance that represents the corner.
   
  private Instance findMostBiasCorner(Instance f, Instance s, double[][] b) {
    Instance corner = new Instance(f);
    corner.setDataset(m_instances);
    int ind = 0;
    for (int i = 0; i < m_instances.numAttributes(); i++) {
      if (m_instances.attribute(i).isNominal()) {
	if (b[i][(int)f.value(i)] == 1) {
	  corner.setValue(i, f.value(i));
	}
	else {
	  for (int j = 0; j < m_instances.attribute(i).numValues(); 
	       j++) {
	    if (b[i][j] == 1 && s.value(i) == j) {
	      ind = j;
	    }
	    else if (b[i][j] == 1) {
	      ind = j;
	      break;
	    }
	  }
	  corner.setValue(i, ind);
	}
	
      }
      else {
	if (f.value(i) > s.value(i)) {
	  corner.setValue(i, b[i][1]);
	}
	else {
	  corner.setValue(i, b[i][0]);
	}
      }
    }
    return corner;
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
      if (m_Instances.instance(instList[i]).value(splitDim) <= splitValue) {
	left[i] = true;
        numLeft++;
      } else {
	left[i] = false;
      }
    }
    return numLeft;
  }

  /**
   * Returns array of boolean set true or false if instance is part
   * of next left kdtree.
   * @param left list of flags, set true by this method if
   * instance should go to the left follow node
   * @param instList
   * @param 
   * @return list of flags saying true if instance belongs to left
   *         kdtree
   */
  private boolean [] splitInstances(boolean [] left,
				    int [] instList, 
				    int [] leftInstList, 
				    int [] rightInstList) {
    int iLeft = 0;
    int iRight = 0;
    for (int i = 0; i < instList.length; i++) {
      if (left[i]) {
	leftInstList[iLeft++] = instList[i];
      } else {
	rightInstList[iRight++] = instList[i];
      }
    }
    return left;
  }

   /**
   * Returns the widest dimension.
   * @return attribute index that has widest range
   */
  private int widestDim() {
    
    double widest = 0.0;
    int w = -1;
    for (int i = 0; i < m_Ranges.length; i++) {
      if (m_Ranges[i][R_WIDTH] > widest) {
        widest = m_Ranges[i][R_WIDTH];
	w = i;
      }
    }
    return w;
  }

  /**
   * Returns the split value of a given dimension.
   * @return attribute index that has widest range
   */
  private double splitValue(int dim) {

    double split = m_Ranges[dim][R_WIDTH] * 0.5;
    return split;
  }

  /**
   * Function should be in the Instances class!!
   *
   * Initialize the minimum and maximum values
   * based on all instances.
   *
   * @param instList list of indexes 
   
  private double [][] initializeRanges(int[] instList) {
    int numAtt = m_Instances.numAttributes();
    
    double [][] ranges = new double [numAtt][3];
    
    // initialize ranges using the first instance
    updateRangesFirst(m_Instances.instance(instList[0]), numAtt,
		      ranges);
    // update ranges, starting from the second
    for (int i = 1; i < instList.length; i++) {
      updateRanges(m_Instances.instance(instList[i]), numAtt,
		   ranges);
    }
    return ranges;
  }

  /**
   * Function should be in the Instances class!!
   *x
   * Used to initialize the ranges. For this the values
   * of the first instance is used to save time.
   * Sets low and high to the values of the first instance and
   * width to zero.
   *
   * @param instance the new instance
   * @param numAtt number of attributes in the model
   
  public static void updateRangesFirst(Instance instance, int numAtt,
				       double[][] ranges) {  

    for (int j = 0; j < numAtt; j++) {
      if (!instance.isMissing(j)) {
	  ranges[j][R_LOW] = instance.value(j);
	  ranges[j][R_HIGH] = instance.value(j);
	  ranges[j][R_WIDTH] = 0.0;
	} 
      else { // if value was missing
	  ranges[j][R_LOW] = Double.MIN_VALUE;
	  ranges[j][R_HIGH] = Double.MAX_VALUE;
	  ranges[j][R_WIDTH] = 0.0; //todo??
	}
    }
  }
  

  /**
   * Function should be in the Instances class!!
   *
   * Updates the minimum and maximum and width values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   * @param numAtt number of attributes in the model
   * @param ranges low, high and width values for all attributes
   
  public static void updateRanges(Instance instance, int numAtt,
				  double [][] ranges) {  

    // updateRangesFirst must have been called on ranges
    for (int j = 0; j < numAtt; j++) {
      double value = instance.value(j);
      if (!instance.isMissing(j)) {
	if (value < ranges[j][R_LOW]) {
	  ranges[j][R_LOW] = value;
	  ranges[j][R_WIDTH] = ranges[j][R_HIGH] - ranges[j][R_LOW];
	} else {
	  if (instance.value(j) > ranges[j][R_HIGH]) {
	    ranges[j][R_HIGH] = value;
	    ranges[j][R_WIDTH] = ranges[j][R_HIGH] - ranges[j][R_LOW];
	  }
	}
      }
      
    }
  }*/


}






