/*
 *    Cobweb.java
 *    Copyright (C) 1999 Ian H. Witten
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

/**
 * Implementation of cobweb and classit algorithms for incremental clustering.
 * <p>
 * Valid options are:<p>
 *
 * -A <0-100> <br>
 * Acuity. <p>
 *
 * -C <0-100> <br>
 * Cutoff. <p>
 *
 * @author Ian H. Witten (ihw@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
package weka.clusterers;

import java.io.*;
import java.util.*; 
import weka.core.*; 

public class Cobweb extends Clusterer implements OptionHandler{

  static final double norm = 1/Math.sqrt(2*Math.PI);

  // Inner Class
  class CTree {
    public CTree siblings = null, children = null;
    public int size; // number of leaves under this node
    public int nchildren; // number of direct children of this node
    public int [][] counts; // these are used for nominal attributes
    public int clusterNum;
    public double [][] totals; // these are used for numeric attributes
    public boolean cutoff = false; // true if this node is cut off
    public Instance instance = null;
    

    //---- Constructor -------------------------------------------------------
    public CTree(Instance i) throws Exception {
      instance = i;
      size = 1;
      nchildren = 0;
      counts = makeCounts(); totals = makeTotals();
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  counts[a][(int) instance.value(a)]++;
	else {
	  totals[a][0] = instance.value(a);
	  totals[a][1] = instance.value(a)*instance.value(a);
	}
    }

    public CTree copyNode() throws Exception { 
      // just copies stats, not nchildren

      CTree copy = new CTree(instance);
      copy.size = size;
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  for (int v=0; v < instance.attribute(a).numValues(); v++)
	    copy.counts[a][v] = counts[a][v];
	else for (int v=0; v < 2; v++) copy.totals[a][v] = totals[a][v];
      return copy;
    }

    public void updateStats(CTree node) 
      throws Exception { // just updates stats, not nchildren
      size += node.size;
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  for (int v=0; v < instance.attribute(a).numValues(); v++)
	    counts[a][v] += node.counts[a][v];
	else for (int v=0; v < 2; v++) totals[a][v] += node.totals[a][v];
    }
  
    public void downdateStats(CTree node) throws Exception {
      size -= node.size;
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  for (int v=0; v < instance.attribute(a).numValues(); v++)
	    counts[a][v] -= node.counts[a][v];
	else for (int v=0; v < 2; v++) totals[a][v] -= node.totals[a][v];
    }
  
    public double CU() throws Exception { 
      // category utility
      
      return TU()/nchildren;
    }

    public double TU() throws Exception { 
      // total utility of children wrt this
      
      return(TU(children));
    }

    public double TU(CTree node) 
      throws Exception { 
      // total utility of node + siblings wrt this
      
      double t = 0;
      for (CTree n = node; n != null; n = n.siblings)
	t += UIndividual(n);
      return t;
    }

    public double UIndividual(CTree node) 
      throws Exception { 
      // utility of one node wrt "this"
      
      double s = 0;
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  for (int v=0; v < instance.attribute(a).numValues(); v++) {
	    double x = (double) node.counts[a][v]/node.size;
	    double y = (double) counts[a][v]/size;
	    s += x*x - y*y;
	  }
	else
	  s += Cobweb.norm/node.sigma(a) - Cobweb.norm/sigma(a);
      return(((double) node.size/size) * s);
    }

    public double sigma(int a) {
      double x = totals[a][1] - totals[a][0]*totals[a][0]/size;
      if (x <= 0) x = 0; else x = Math.sqrt(x/size);
      return Math.max(Cobweb.acuity, x);
    }

    public double UMerged(CTree n1, CTree n2) 
      throws Exception {
      // utility of n1 merged with n2

      n1.updateStats(n2);
      double U = UIndividual(n1);
      n1.downdateStats(n2);
      return U;
    }

    public void addChildren(CTree node) {
      if (node == null); // do nothing
      else {
	CTree n;
	nchildren++;
	for (n = node; n.siblings != null; n = n.siblings) nchildren++;
	n.siblings = children;
	children = node;
      }
    }

    public void addChild(CTree node) {
      nchildren++;
      if (children == null) children = node;
      else children.addSibling(node);
    }

    public void removeChild(CTree node) {
      if (children == null) return;
      if (children == node) {
	children = children.siblings;
	node.siblings = null;
	nchildren--;
	return;
      }
      for (CTree n = children; n.siblings != null; n = n.siblings) 
	if (n.siblings == node) {
	  n.siblings = node.siblings;
	  node.siblings = null;
	  nchildren--;
	  return;
	}
    }

    public void addSibling(CTree node) {
      node.siblings = siblings;
      siblings = node;
    }

    private int[][] makeCounts() throws Exception {
      int [][] counts = new int[instance.numAttributes()][];
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNominal())
	  counts[a] = new int[instance.attribute(a).numValues()];
	else counts[a] = null;
      return counts;
    }
  
    private double[][] makeTotals() throws Exception {
      double [][] totals = new double[instance.numAttributes()][];
      for (int a = 0; a < instance.numAttributes(); a++)
	if (instance.attribute(a).isNumeric())
	  totals[a] = new double[2];
	else totals[a] = null;
      return totals;
    }

    // Returns a description of the tree
    public String toString() {
      try {
	StringBuffer buf = new StringBuffer("\n");
	print(buf, 0);
	return buf.toString();
      } catch (Exception e) {
	e.printStackTrace();
	return "Can't print clusterer.";
      }
    }

    private int nChildren() {
      int number = 0;
      for (CTree n = children; n != null; n = n.siblings) number++;
      return number;
    }

    private int nDescendants() {
      int number = 0;
      if (children == null) number = 1;
      else for (CTree n = children; n != null; n = n.siblings)
	number += n.nDescendants();
      return number;
    }

    // Appends a description of the tree (at given level) to the StringBuffer
    private void print(StringBuffer buf, int level) throws Exception {
      if (!cutoff) {
	if (nchildren != nChildren()) 
	  System.out.println("Problem: nchildren = " + nchildren +
			     " but there are "+ nChildren() + " children!");
	if (size != nDescendants()) 
	  System.out.println("Problem: size = " + size +
			     " but there are "+ nDescendants() +
			     " descendants!");
	for (int j=0; j < level; j++) buf.append("    ");
	buf.append(size + "  | ");
	for (int a = 0; a < instance.numAttributes(); a++) {
	  if (instance.attribute(a).isNominal())
	    for (int v=0; v < instance.attribute(a).numValues(); v++)
	      buf.append(((double) counts[a][v]) + " ");
	  else
	    buf.append(totals[a][0]/size + // " (" + totals[a][1]/size + ")" +
		       " " + sigma(a));
	  buf.append("| ");
	}
	if (this.nchildren > 0) buf.append(" (" + this.CU() + ")");

	buf.append(" ["+clusterNum+"]");
	buf.append("\n");

	if (children != null) children.print(buf, level+1);
      } // cut off nodes can't have children, but they often have siblings
      if (siblings != null) siblings.print(buf, level);
    }

    private void assignClusterNums(int [] cl_num) throws Exception {
      if (!cutoff) {
	if (nchildren != nChildren()) 
	  System.out.println("Problem: nchildren = " + nchildren +
			     " but there are "+ nChildren() + " children!");
	if (size != nDescendants()) 
	  System.out.println("Problem: size = " + size +
			     " but there are "+ nDescendants() +
			     " descendants!");
	
	this.clusterNum = cl_num[0];
	cl_num[0]++;
	if (children != null) 
	  children.assignClusterNums(cl_num);
      }
      
      if (siblings != null)
	siblings.assignClusterNums(cl_num);
    }
  }

  /** acuity */
  static double acuity = 1.0;
  
  /** cutoff */
  static double cutoff = 0.01 * Cobweb.norm;
  
  /** the cobweb tree */
  private CTree tree = null;

  /** number of clusters */
  private int numClusters = -1;

  /**
   * Builds the clusterer.
   *
   * @param data the training instances.
   * @exception Exception if something goes wrong.
   */
  public void buildClusterer(Instances data) throws Exception {

    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    tree = null;
    numClusters = -1;
    makeweb(data);
  }

  private void makeweb(Instances data) throws Exception {
    Enumeration e = data.enumerateInstances();
    while (e.hasMoreElements()) {
      Instance i = (Instance) e.nextElement();
      CTree node = new CTree(i);
      if (tree == null) {
	tree = new CTree(i);
	tree.addChild(node);
      }
      else add(node, tree);
    }

    if (numClusters == -1)
      {
	int [] cl_num = new int [1];
	cl_num[0] = 0;
	tree.assignClusterNums(cl_num);
	numClusters = cl_num[0];
      }
    
    // System.out.println(tree);
  }
  
  /**
   * Returns the number of clusters.
   *
   * @exception Exception if something goes wrong.
   */
  public int numberOfClusters() throws Exception {
    return numClusters;
  }

  /**
   * Clusters an instance.
   *
   * @param instance the instance to cluster.
   * @exception Exception if something goes wrong.
   */
  public int clusterInstance(Instance instance) throws Exception {
    CTree node = new CTree(instance);
 
    double T;
    int n;
    double baseU; // base CU over child nodes
    double U;
    CTree host = tree;
    CTree temp = null;
    do {
      if (host.children == null) {
	temp = null;
      }
      else {
	T = host.TU();
	n = host.nchildren;
	baseU = T/n; // base CU over child nodes
	U = (host.UIndividual(node) + T)/(n+1);
	temp = bestHostCluster(host, node, U, baseU);
      }
      if (temp != null) {
	host = temp;
      }
    } while (temp != null);

    //    System.out.println(host.clusterNum);
    return host.clusterNum;
  }

  /**
   * Adds an example to the tree.
   *
   * @param node the node to be added.
   * @param tree the tree.
   * @exception Exception if something goes wrong.
   */
  public void add(CTree node, CTree tree) throws Exception {
    if (tree.children == null) {
      tree.addChild(tree.copyNode());
      tree.addChild(node);
      tree.updateStats(node);
    }
    else {
      double T = tree.TU();
      int n = tree.nchildren;
      double baseU = T/n; // base CU over child nodes
      double U = (tree.UIndividual(node) + T)/(n+1);
      CTree host = bestHost(tree, node, U, baseU);
      tree.updateStats(node);
      if (host == null) tree.addChild(node);
      else if (host.children == null) {
	host.addChild(host.copyNode());
	host.addChild(node);
	host.updateStats(node);
	if (host.CU() < cutoff) {
	  for (CTree n1 = host.children; n1 != null; n1 = n1.siblings)
	    n1.cutoff = true; // mark them cut off
	  return;
	}
      }
      else add(node, host);
    }
  }

  /**
   * Finds the cluster that an unseen instance belongs to.
   *
   * @param tree the tree.
   * @param node the node to be added.
   * @param aU ??
   * @param baseU ??
   * @exception Exception if something goes wrong.
   */
  public CTree bestHostCluster(CTree tree, CTree node, double aU, double baseU) 
    throws Exception {
    double oldaU = aU;
    CTree a = null; // a is the best node so far
    CTree b = null; double bU = 0; // b is the second-best
    for (CTree n = tree.children; n != null; n = n.siblings)
      if (!n.cutoff) {
	n.updateStats(node);
      double nU = tree.CU();
      n.downdateStats(node);
      if (nU > bU)
	if (nU > aU) { // n becomes best, a becomes second-best
	  b = a; a = n;
	  bU = aU; aU = nU;
	}
	else { // n becomes second-best
	  b = n;
	  bU = nU;
	}
      }
    return a;
  }

  /**
   * Finds the best place to add a new node during training.
   *
   * @param tree the tree.
   * @param node the node to be added.
   * @param aU ??
   * @param baseU ??
   * @exception Exception if something goes wrong.
   */
  public CTree bestHost(CTree tree, CTree node, double aU, double baseU) 
    throws Exception {
    double oldaU = aU;
    CTree a = null; // a is the best node so far
    CTree b = null; double bU = 0; // b is the second-best
    for (CTree n = tree.children; n != null; n = n.siblings)
      if (!n.cutoff) {
      n.updateStats(node);
      double nU = tree.CU();
      n.downdateStats(node);
      if (nU > bU)
	if (nU > aU) { // n becomes best, a becomes second-best
	  b = a; a = n;
	  bU = aU; aU = nU;
	}
	else { // n becomes second-best
	  b = n;
	  bU = nU;
	}
      }
    if (a == null) return null;
      // consider merging best and second-best nodes
      // current CU is baseU = (a + b + rest)/n
      // new CU would be (c + rest)/(n-1)  [c is merged node]
      // merge if (c + rest)/(n-1) > U, ie c - (a+b) + baseU > 0
    if (b != null &&
	tree.UMerged(a, b) - (tree.UIndividual(a)+tree.UIndividual(b)) + baseU
	  > 0) {
      CTree c = a.copyNode();
      c.updateStats(b);
      tree.addChild(c);
      tree.removeChild(a); tree.removeChild(b);
      c.addChild(a); c.addChild(b);
      return(c);
    }
    // change notation: now c will be the node we split
    CTree c = a; // call c's children a, b, ...
    // current CU is baseU = (c + rest)/n
    // new CU would be (a + b + ... + rest)/(n - 1 + nchildren)
    // split if (a + b + ... + rest)/(n - 1 + nchildren) > baseU
    // ie if (a + b + ...) - c > (nchildren - 1)*baseU
    if (a.children != null &&
      c.TU() - tree.UIndividual(c) > (c.nchildren - 1)*baseU) {
      tree.removeChild(c);
      tree.addChildren(c.children);
      // Now find the best host again
      a = null; aU = 0; // a is the best node so far
      for (CTree n = tree.children; n != null; n = n.siblings) {
	n.updateStats(node);
	double nU = tree.CU();
	n.downdateStats(node);
	if (nU > aU) {
	  a = n;
	  aU = nU;
	}
      }
    }
    return a;
  }

  /**
   * Returns a description of the clusterer as a string.
   *
   * @return a string describing the clusterer.
   */
  public String toString() { 
    if (tree == null) {
      return "Cobweb hasn't been built yet!";
    }
    else {
      return "Number of clusters: "+numClusters+"\n"+tree.toString(); 
    }
    
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    
    newVector.addElement(new Option("\tAcuity.\n"
				    +"\t(default=100)", "A", 1,"-A <0-100%>"));
    newVector.addElement(new Option("\tCutoff.\n"
				    +"a\t(default=0)", "C", 1,"-C <0-100%>"));
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   *
   * -A <0-100> <br>
   * Acuity. <p>
   *
   * -C <0-100> <br>
   * Cutoff. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception {
    String optionString;

    optionString = Utils.getOption('A', options); 
    if (optionString.length() != 0) {
	setAcuity(Integer.parseInt(optionString));
    }
    else {
      acuity = 1.0;
    }
    optionString = Utils.getOption('C', options); 
    if (optionString.length() != 0) {
      setCutoff(Integer.parseInt(optionString));
    }
    else {
      cutoff = 0.01 * Cobweb.norm;
    }
  }

  /**
   * set the accuity.
   * @param a the accuity between 0 and 100
   */
  public void setAcuity(int a) {
    acuity = (double) a / 100.0;
  }

  /**
   * get the accuity value
   * @return the accuity as a value between 0 and 100
   */
  public int getAcuity() {
    return (int) (acuity * 100.0);
  }

  /**
   * set the cutoff
   * @param c the cutoff between 0 and 100
   */
  public void setCutoff(int c) {
    cutoff = (double) c / 100.0;;
  }

  /**
   * get the cutoff
   * @return the cutoff as a value between 1 and 100\
   */
  public int getCutoff() {
    return (int) (cutoff * 100.0);
  }

  /**
   * Gets the current settings of Cobweb.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {
    
    String [] options = new String [4];
    int current = 0;
    options[current++] = "-A"; 
    options[current++] = "" + ((int)(acuity * 100.0));
    options[current++] = "-C"; 
    options[current++] = "" + ((int)(cutoff * 100.0));
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  // Main method for testing this class
  public static void main(String [] argv)
  {
    try {
      System.out.println(ClusterEvaluation.evaluateClusterer(new Cobweb(), 
							     argv));
    }
    catch (Exception e)
    {
      System.out.println(e.getMessage());
    }
  }
}
