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
 *    sIB.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.matrix.Matrix;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Cluster data using the sequential information bottleneck algorithm.<br/>
 * <br/>
 * Note: only hard clustering scheme is supported. sIB assign for each instance the cluster that have the minimum cost/distance to the instance. The trade-off beta is set to infinite so 1/beta is zero.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Noam Slonim, Nir Friedman, Naftali Tishby: Unsupervised document classification using sequential information maximization. In: Proceedings of the 25th International ACM SIGIR Conference on Research and Development in Information Retrieval, 129-136, 2002.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Slonim2002,
 *    author = {Noam Slonim and Nir Friedman and Naftali Tishby},
 *    booktitle = {Proceedings of the 25th International ACM SIGIR Conference on Research and Development in Information Retrieval},
 *    pages = {129-136},
 *    title = {Unsupervised document classification using sequential information maximization},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -I &lt;num&gt;
 *  maximum number of iterations
 *  (default 100).</pre>
 * 
 * <pre> -M &lt;num&gt;
 *  minimum number of changes in a single iteration
 *  (default 0).</pre>
 * 
 * <pre> -N &lt;num&gt;
 *  number of clusters.
 *  (default 2).</pre>
 * 
 * <pre> -R &lt;num&gt;
 *  number of restarts.
 *  (default 5).</pre>
 * 
 * <pre> -U
 *  set not to normalize the data
 *  (default true).</pre>
 * 
 * <pre> -V
 *  set to output debug info
 *  (default false).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 <!-- options-end --> 
 *
 * @author Noam Slonim
 * @author <a href="mailto:lh92@cs.waikato.ac.nz">Anna Huang</a> 
 * @version $Revision: 1.2 $
 */
public class sIB
  extends RandomizableClusterer
  implements TechnicalInformationHandler {
  
  /** for serialization. */
  private static final long    serialVersionUID = -8652125897352654213L;
  
  /**
   * Inner class handling status of the input data
   * 
   * @see Serializable
   */
  private class Input
    implements Serializable, RevisionHandler {
    
    /** for serialization */
    static final long serialVersionUID = -2464453171263384037L;
    
    /** Prior probability of each instance */
    private double[] Px;
    
    /** Prior probability of each attribute */
    private double[] Py;
    
    /** Joint distribution of attribute and instance */
    private Matrix Pyx;
    
    /** P[y|x] */
    private Matrix Py_x;
    
    /** Mutual information between the instances and the attributes */
    private double Ixy;
    
    /** Entropy of the attributes */ 
    private double Hy;
    
    /** Entropy of the instances */
    private double Hx;
    
    /** Sum values of the dataset */
    private double sumVals;
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.2 $");
    }
  }
  
  /**
   * Internal class handling the whole partition
   * 
   * @see Serializable
   */
  private class Partition
    implements Serializable, RevisionHandler {
    
    /** for serialization */
    static final long serialVersionUID = 4957194978951259946L;
    
    /** Cluster assignment for each instance */
    private int[]    Pt_x;
    
    /** Prior probability of each cluster */
    private double[] Pt;
    
    /** sIB equation score, to evaluate the quality of the partition */
    private double   L;
    
    /** Number of changes during the generation of this partition */
    private int      counter;
    
    /** Attribute probablities for each cluster */
    private Matrix   Py_t;

    /** 
     * Create a new empty <code>Partition</code> instance.
     */
    public Partition() {
      Pt_x = new int[m_numInstances];
      for (int i = 0; i < m_numInstances; i++) {
	Pt_x[i] = -1;
      }
      Pt = new double[m_numCluster];
      Py_t = new Matrix(m_numAttributes, m_numCluster);
      counter = 0;
    }

    /**
     * Find all the instances that have been assigned to cluster i
     * @param i index of the cluster
     * @return an arraylist of the instance ids that have been assigned to cluster i
     */
    private ArrayList<Integer> find(int i) {
      ArrayList<Integer> indices = new ArrayList<Integer>();
      for (int x = 0; x < Pt_x.length; x++) {
	if (Pt_x[x] == i) {
	  indices.add(x);
	}
      }
      return indices;
    }

    /**
     * Find the size of the cluster i
     * @param i index of the cluster
     * @return the size of cluster i
     */
    private int size(int i) {
      int count = 0;
      for (int x = 0; x < Pt_x.length; x++) {
	if (Pt_x[x] == i) {
	  count++;
	}
      }
      return count;
    }

    /**
     * Copy the current partition into T
     * @param T the target partition object
     */
    private void copy(Partition T) {
      if (T == null) {
	T = new Partition();
      }
      System.arraycopy(Pt_x, 0, T.Pt_x, 0, Pt_x.length);
      System.arraycopy(Pt, 0, T.Pt, 0, Pt.length);
      T.L = L;
      T.counter = counter;

      double[][] mArray = Py_t.getArray();
      double[][] tgtArray = T.Py_t.getArray();
      for (int i = 0; i < mArray.length; i++) {
	System.arraycopy(mArray[i], 0, tgtArray[i], 0, mArray[0].length);
      }
    }

    /**
     * Output the current partition
     * @param insts
     * @return a string that describes the partition
     */
    public String toString() {
      StringBuffer text = new StringBuffer();
      text.append("score (L) : " + Utils.doubleToString(L, 4) + "\n");
      text.append("number of changes : " + counter +"\n");
      for (int i = 0; i < m_numCluster; i++) {
	text.append("\nCluster "+i+"\n");
	text.append("size : "+size(i)+"\n");
	text.append("prior prob : "+Utils.doubleToString(Pt[i], 4)+"\n");
      }
      return text.toString();
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.2 $");
    }
  }

  /** Training data */
  private Instances m_data;

  /** Number of clusters */
  private int m_numCluster = 2;

  /** Number of restarts */
  private int m_numRestarts = 5;
  
  /** Verbose? */
  private boolean m_verbose = false;

  /** Uniform prior probability of the documents */
  private boolean m_uniformPrior = true;

  /** Max number of iterations during each restart */
  private int m_maxLoop = 100;

  /** Minimum number of changes */
  private int m_minChange = 0;

  /** Globally replace missing values */
  private ReplaceMissingValues m_replaceMissing;

  /** Number of instances */
  private int m_numInstances;

  /** Number of attributes */
  private int m_numAttributes;
  
  /** Randomly generate initial partition */
  private Random random;
  
  /** Holds the best partition built */
  private Partition bestT;
  
  /** Holds the statistics about the input dataset */
  private Input input;
  
  /**
   * Generates a clusterer. 
   *
   * @param data the training instances 
   * @throws Exception if something goes wrong
   */
  public void buildClusterer(Instances data) throws Exception {
    // can clusterer handle the data ?
    getCapabilities().testWithFail(data);

    m_replaceMissing = new ReplaceMissingValues();
    Instances instances = new Instances(data);
    instances.setClassIndex(-1);
    m_replaceMissing.setInputFormat(instances);
    data = weka.filters.Filter.useFilter(instances, m_replaceMissing);
    instances = null;
    
    // initialize all fields that are not being set via options
    m_data = data;
    m_numInstances = m_data.numInstances();
    m_numAttributes = m_data.numAttributes();
    random = new Random(getSeed()); 
    
    // initialize the statistics of the input training data
    input = sIB_ProcessInput();
    
    // object to hold the best partition
    bestT = new Partition();

    // the real clustering
    double bestL = Double.NEGATIVE_INFINITY;
    for (int k = 0; k < m_numRestarts; k++) {   
      if(m_verbose) {
	System.out.format("restart number %s...\n", k);
      }
      
      // initialize the partition and optimize it
      Partition tmpT = sIB_InitT(input);      
      tmpT = sIB_OptimizeT(tmpT, input);

      // if a better partition is found, save it
      if (tmpT.L > bestL) {
	tmpT.copy(bestT);
	bestL = bestT.L;
      }
      
      if(m_verbose) {
	System.out.println("\nPartition status : ");
	System.out.println("------------------");
	System.out.println(tmpT.toString()+"\n");
      }
    }
    
    if(m_verbose){
      System.out.println("\nBest Partition");
      System.out.println("===============");      
      System.out.println(bestT.toString());
    }
    
    // save memory
    m_data = new Instances(m_data, 0);
  }
  
  /**
   * Cluster a given instance, this is the method defined in Clusterer
   * interface do nothing but just return the cluster assigned to it
   */
  public int clusterInstance(Instance instance) throws Exception {
    double prior = (double) 1 / input.sumVals;
    double[] distances = new double[m_numCluster]; 
    for(int i = 0; i < m_numCluster; i++){
      double Pnew = bestT.Pt[i] + prior;
      double pi1 = prior / Pnew;
      double pi2 = bestT.Pt[i] / Pnew;
      distances[i] = Pnew * JS(instance, i, pi1, pi2);
    }
    return Utils.minIndex(distances);
  }

  /**
   * Process the input and compute the statistics of the training data
   * @return an Input object which holds the statistics about the training data
   */
  private Input sIB_ProcessInput() {
    double valSum = 0.0;
    for (int i = 0; i < m_numInstances; i++) {
      valSum = 0.0;
      for (int v = 0; v < m_data.instance(i).numValues(); v++) {
	valSum += m_data.instance(i).valueSparse(v);
      }
      if (valSum <= 0) {
	if(m_verbose){
	  System.out.format("Instance %s sum of value = %s <= 0, removed.\n", i, valSum);
	}
	m_data.delete(i);
	m_numInstances--;
      }
    }

    // get the term-document matrix
    Input input = new Input();
    input.Py_x = getTransposedNormedMatrix(m_data);    
    if (m_uniformPrior) {
      input.Pyx = input.Py_x.copy();  
      normalizePrior(m_data);
    } 
    else {
      input.Pyx = getTransposedMatrix(m_data);
    }
    input.sumVals = getTotalSum(m_data);    
    input.Pyx.timesEquals((double) 1 / input.sumVals);

    // prior probability of documents, ie. sum the columns from the Pyx matrix
    input.Px = new double[m_numInstances];
    for (int i = 0; i < m_numInstances; i++) {
      for (int j = 0; j < m_numAttributes; j++) {
	input.Px[i] += input.Pyx.get(j, i);
      }           
    }

    // prior probability of terms, ie. sum the rows from the Pyx matrix
    input.Py = new double[m_numAttributes];
    for (int i = 0; i < input.Pyx.getRowDimension(); i++) {
      for (int j = 0; j < input.Pyx.getColumnDimension(); j++) {
	input.Py[i] += input.Pyx.get(i, j);
      }
    }
    
    MI(input.Pyx, input);
    return input;
  }

  /**
   * Initialize the partition
   * @param input object holding the statistics of the training data
   * @return the initialized partition
   */
  private Partition sIB_InitT(Input input) {
    Partition T = new Partition();
    int avgSize = (int) Math.ceil((double) m_numInstances / m_numCluster);    
    
    ArrayList<Integer> permInstsIdx = new ArrayList<Integer>();
    ArrayList<Integer> unassigned = new ArrayList<Integer>();
    for (int i = 0; i < m_numInstances; i++) {
      unassigned.add(i);
    }
    while (unassigned.size() != 0) {
      int t = random.nextInt(unassigned.size());
      permInstsIdx.add(unassigned.get(t));
      unassigned.remove(t);
    }

    for (int i = 0; i < m_numCluster; i++) {
      int r2 = avgSize > permInstsIdx.size() ? permInstsIdx.size() : avgSize;
      for (int j = 0; j < r2; j++) {
	T.Pt_x[permInstsIdx.get(j)] = i;
      }      
      for (int j = 0; j < r2; j++) {	
	permInstsIdx.remove(0);
      }
    }
    
    // initialize the prior prob of each cluster, and the probability 
    // for each attribute within the cluster
    for (int i = 0; i < m_numCluster; i++) {
      ArrayList<Integer> indices = T.find(i);
      for (int j = 0; j < indices.size(); j++) {
	T.Pt[i] += input.Px[indices.get(j)];
      }
      double[][] mArray = input.Pyx.getArray();
      for (int j = 0; j < m_numAttributes; j++) {
	double sum = 0.0;
	for (int k = 0; k < indices.size(); k++) {
	  sum += mArray[j][indices.get(k)];
	}
	sum /= T.Pt[i];
	T.Py_t.set(j, i, sum);
      }
    }
    
    if(m_verbose) {
      System.out.println("Initializing...");
    }
    return T;
  }

  /**
   * Optimize the partition
   * @param tmpT partition to be optimized
   * @param input object describing the statistics of the training dataset
   * @return the optimized partition
   */
  private Partition sIB_OptimizeT(Partition tmpT, Input input) {
    boolean done = false;
    int change = 0, loopCounter = 0;
    if(m_verbose) {
      System.out.println("Optimizing...");
      System.out.println("-------------");
    }
    while (!done) {
      change = 0;
      for (int i = 0; i < m_numInstances; i++) {
	int old_t = tmpT.Pt_x[i];
	// If the current cluster only has one instance left, leave it.
	if (tmpT.size(old_t) == 1) {
	  if(m_verbose){
	    System.out.format("cluster %s has only 1 doc remain\n", old_t);
	  }
	  continue;
	}
	// draw the instance out from its previous cluster
	reduce_x(i, old_t, tmpT, input);
	
	// re-cluster the instance
	int new_t = clusterInstance(i, input, tmpT);	
	if (new_t != old_t) {	  
	  change++;
	  updateAssignment(i, new_t, tmpT, input.Px[i], input.Py_x);
	}
      }
      
      tmpT.counter += change;
      if(m_verbose){
	System.out.format("iteration %s , changes : %s\n", loopCounter, change);
      }
      done = checkConvergence(change, loopCounter);
      loopCounter++;
    }

    // compute the sIB score
    tmpT.L = sIB_local_MI(tmpT.Py_t, tmpT.Pt);
    if(m_verbose){
      System.out.format("score (L) : %s \n", Utils.doubleToString(tmpT.L, 4));
    }
    return tmpT;
  }

  /**
   * Draw a instance out from a cluster. 
   * @param instIdx index of the instance to be drawn out
   * @param t index of the cluster which the instance previously belong to
   * @param T the current working partition
   * @param input the input statistics
   */
  private void reduce_x(int instIdx, int t, Partition T, Input input) {
    // Update the prior probability of the cluster
    ArrayList<Integer> indices = T.find(t);
    double sum = 0.0;
    for (int i = 0; i < indices.size(); i++) {
      if (indices.get(i) == instIdx)
	continue;      
      sum += input.Px[indices.get(i)];
    }
    T.Pt[t] = sum;
    
    if (T.Pt[t] < 0) {
      System.out.format("Warning: probability < 0 (%s)\n", T.Pt[t]);
      T.Pt[t] = 0;
    }
    
    // Update prob of each attribute in the cluster    
    double[][] mArray = input.Pyx.getArray();
    for (int i = 0; i < m_numAttributes; i++) {
      sum = 0.0;
      for (int j = 0; j < indices.size(); j++) {
	if (indices.get(j) == instIdx)
	  continue;
	sum += mArray[i][indices.get(j)];
      }
      T.Py_t.set(i, t, sum / T.Pt[t]);
    }    
  }

  /**
   * Put an instance into a new cluster and update.
   * @param instIdx instance to be updated
   * @param newt index of the new cluster this instance has been assigned to
   * @param T the current working partition
   * @param Px an array of prior probabilities of the instances
   */
  private void updateAssignment(int instIdx, int newt, Partition T, double Px, Matrix Py_x) {    
    T.Pt_x[instIdx] = newt;
    
    // update probability of attributes in the cluster 
    double mass = Px + T.Pt[newt];
    double pi1 = Px / mass;
    double pi2 = T.Pt[newt] / mass;
    for (int i = 0; i < m_numAttributes; i++) {
      T.Py_t.set(i, newt, pi1 * Py_x.get(i, instIdx) + pi2 * T.Py_t.get(i, newt));
    }

    T.Pt[newt] = mass;
  }
  
  /**
   * Check whether the current iteration is converged
   * @param change number of changes in current iteration
   * @param loops number of iterations done
   * @return true if the iteration is converged, false otherwise
   */
  private boolean checkConvergence(int change, int loops) {
    if (change <= m_minChange || loops >= m_maxLoop) {
      if(m_verbose){
	System.out.format("\nsIB converged after %s iterations with %s changes\n", loops,
	  change);
      }
      return true;
    }
    return false;
  }

  /**
   * Cluster an instance into the nearest cluster. 
   * @param instIdx Index of the instance to be clustered
   * @param input Object which describe the statistics of the training dataset
   * @param T Partition
   * @return index of the cluster that has the minimum distance to the instance
   */
  private int clusterInstance(int instIdx, Input input, Partition T) {
    double[] distances = new double[m_numCluster];
    for (int i = 0; i < m_numCluster; i++) {
      double Pnew = input.Px[instIdx] + T.Pt[i];
      double pi1 = input.Px[instIdx] / Pnew;
      double pi2 = T.Pt[i] / Pnew;
      distances[i] = Pnew * JS(instIdx, input, T, i, pi1, pi2);
    }
    return Utils.minIndex(distances);    
  }

  /**
   * Compute the JS divergence between an instance and a cluster, used for training data
   * @param instIdx index of the instance
   * @param input statistics of the input data
   * @param T the whole partition
   * @param t index of the cluster 
   * @param pi1
   * @param pi2
   * @return the JS divergence
   */
  private double JS(int instIdx, Input input, Partition T, int t, double pi1, double pi2) {
    if (Math.min(pi1, pi2) <= 0) {
      System.out.format("Warning: zero or negative weights in JS calculation! (pi1 %s, pi2 %s)\n", pi1, pi2);
      return 0;
    }
    Instance inst = m_data.instance(instIdx);
    double kl1 = 0.0, kl2 = 0.0, tmp = 0.0;    
    for (int i = 0; i < inst.numValues(); i++) {
      tmp = input.Py_x.get(inst.index(i), instIdx);      
      if(tmp != 0) {
	kl1 += tmp * Math.log(tmp / (tmp * pi1 + pi2 * T.Py_t.get(inst.index(i), t)));
      }
    }
    for (int i = 0; i < m_numAttributes; i++) {
      if ((tmp = T.Py_t.get(i, t)) != 0) {
	kl2 += tmp * Math.log(tmp / (input.Py_x.get(i, instIdx) * pi1 + pi2 * tmp));
      }
    }    
    return pi1 * kl1 + pi2 * kl2;
  }
  
  /**
   * Compute the JS divergence between an instance and a cluster, used for test data
   * @param inst instance to be clustered
   * @param t index of the cluster
   * @param pi1
   * @param pi2
   * @return the JS divergence
   */
  private double JS(Instance inst, int t, double pi1, double pi2) {
    if (Math.min(pi1, pi2) <= 0) {
      System.out.format("Warning: zero or negative weights in JS calculation! (pi1 %s, pi2 %s)\n", pi1, pi2);
      return 0;
    }
    double sum = Utils.sum(inst.toDoubleArray());
    double kl1 = 0.0, kl2 = 0.0, tmp = 0.0;    
    for (int i = 0; i < inst.numValues(); i++) {
      tmp = inst.valueSparse(i) / sum;      
      if(tmp != 0) {
	kl1 += tmp * Math.log(tmp / (tmp * pi1 + pi2 * bestT.Py_t.get(inst.index(i), t)));
      }
    }
    for (int i = 0; i < m_numAttributes; i++) {
      if ((tmp = bestT.Py_t.get(i, t)) != 0) {
	kl2 += tmp * Math.log(tmp / (inst.value(i) * pi1  / sum + pi2 * tmp));
      }
    }    
    return pi1 * kl1 + pi2 * kl2;
  }
  
  /**
   * Compute the sIB score
   * @param m a term-cluster matrix, with m[i, j] is the probability of term i given cluster j  
   * @param Pt an array of cluster prior probabilities
   * @return the sIB score which indicates the quality of the partition
   */
  private double sIB_local_MI(Matrix m, double[] Pt) {
    double Hy = 0.0, Ht = 0.0;
    for (int i = 0; i < Pt.length; i++) {
      Ht += Pt[i] * Math.log(Pt[i]);
    }
    Ht = -Ht;
    
    for (int i = 0; i < m_numAttributes; i++) {
      double Py = 0.0;
      for (int j = 0; j < m_numCluster; j++) {
	Py += m.get(i, j) * Pt[j];	
      }     
      if(Py == 0) continue;
      Hy += Py * Math.log(Py);
    }
    Hy = -Hy;
    
    double Hyt = 0.0, tmp = 0.0;
    for (int i = 0; i < m.getRowDimension(); i++) {
      for (int j = 0; j < m.getColumnDimension(); j++) {
	if ((tmp = m.get(i, j)) == 0 || Pt[j] == 0) {
	  continue;
	}
	tmp *= Pt[j];
	Hyt += tmp * Math.log(tmp);
      }
    }
    return Hy + Ht + Hyt;
  }
  
  /**
   * Get the sum of value of the dataset
   * @param data set of instances to handle
   * @return sum of all the attribute values for all the instances in the dataset
   */
  private double getTotalSum(Instances data) {
    double sum = 0.0;
    for (int i = 0; i < data.numInstances(); i++) {
      for (int v = 0; v < data.instance(i).numValues(); v++) {
	sum += data.instance(i).valueSparse(v);
      }
    }
    return sum;
  }

  /**
   * Transpose the document-term matrix to term-document matrix
   * @param data instances with document-term info
   * @return a term-document matrix transposed from the input dataset
   */
  private Matrix getTransposedMatrix(Instances data) {
    double[][] temp = new double[data.numAttributes()][data.numInstances()];
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      for (int v = 0; v < inst.numValues(); v++) {
	temp[inst.index(v)][i] = inst.valueSparse(v);
      }
    }
    Matrix My_x = new Matrix(temp);
    return My_x;
  }

  /**
   * Normalize the document vectors
   * @param data instances to be normalized
   */
  private void normalizePrior(Instances data) {
    for (int i = 0; i < data.numInstances(); i++) {
      normalizeInstance(data.instance(i));
    }
  }
  
  /**
   * Normalize the instance
   * @param inst instance to be normalized
   * @return a new Instance with normalized values
   */
  private Instance normalizeInstance(Instance inst) {
    double[] vals = inst.toDoubleArray();
    double sum = Utils.sum(vals);
    for(int i = 0; i < vals.length; i++) {
      vals[i] /= sum;
    }
    return new Instance(inst.weight(), vals);
  }
  
  private Matrix getTransposedNormedMatrix(Instances data) {
    Matrix matrix = new Matrix(data.numAttributes(), data.numInstances());
    for(int i = 0; i < data.numInstances(); i++){
      double[] vals = data.instance(i).toDoubleArray();
      double sum = Utils.sum(vals);
      for (int v = 0; v < vals.length; v++) {
	vals[v] /= sum;
	matrix.set(v, i, vals[v]);
      }      
    }
    return matrix;
  }
  
  /**
   * Compute the MI between instances and attributes
   * @param m the term-document matrix
   * @param input object that describes the statistics about the training data
   */
  private void MI(Matrix m, Input input){    
    int minDimSize = m.getColumnDimension() < m.getRowDimension() ? m.getColumnDimension() : m.getRowDimension();
    if(minDimSize < 2){
      System.err.println("Warning : This is not a JOINT distribution");
      input.Hx = Entropy (m);
      input.Hy = 0;
      input.Ixy = 0;
      return;
    }
    
    input.Hx = Entropy(input.Px);
    input.Hy = Entropy(input.Py);
    
    double entropy = input.Hx + input.Hy;    
    for (int i=0; i < m_numInstances; i++) {
      Instance inst = m_data.instance(i);
      for (int v = 0; v < inst.numValues(); v++) {
	double tmp = m.get(inst.index(v), i);
	if(tmp <= 0) continue;
	entropy += tmp * Math.log(tmp);
      }
    }
    input.Ixy = entropy;
    if(m_verbose) {
      System.out.println("Ixy = " + input.Ixy);
    }
  }
  
  /**
   * Compute the entropy score based on an array of probabilities
   * @param probs array of non-negative and normalized probabilities
   * @return the entropy value
   */
  private double Entropy(double[] probs){
    for (int i = 0; i < probs.length; i++){
      if (probs[i] <= 0) {
	if(m_verbose) {
	  System.out.println("Warning: Negative probability.");
	}
	return Double.NaN;
      }
    }
    // could be unormalized, when normalization is not specified 
    if(Math.abs(Utils.sum(probs)-1) >= 1e-6) {
      if(m_verbose) {
	System.out.println("Warning: Not normalized.");
      }
      return Double.NaN;
    }
    
    double mi = 0.0;
    for (int i = 0; i < probs.length; i++) {
      mi += probs[i] * Math.log(probs[i]);
    }
    mi = -mi;
    return mi;
  }
  
  /**
   * Compute the entropy score based on a matrix
   * @param p a matrix with non-negative and normalized probabilities
   * @return the entropy value
   */
  private double Entropy(Matrix p) {
    double mi = 0;
    for (int i = 0; i < p.getRowDimension(); i++) {
      for (int j = 0; j < p.getColumnDimension(); j++) {
	if(p.get(i, j) == 0){
	  continue;
	}
	mi += p.get(i, j) + Math.log(p.get(i, j)); 
      }
    }
    mi = -mi;
    return mi;
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -I &lt;num&gt;
   *  maximum number of iterations
   *  (default 100).</pre>
   * 
   * <pre> -M &lt;num&gt;
   *  minimum number of changes in a single iteration
   *  (default 0).</pre>
   * 
   * <pre> -N &lt;num&gt;
   *  number of clusters.
   *  (default 2).</pre>
   * 
   * <pre> -R &lt;num&gt;
   *  number of restarts.
   *  (default 5).</pre>
   * 
   * <pre> -U
   *  set not to normalize the data
   *  (default true).</pre>
   * 
   * <pre> -V
   *  set to output debug info
   *  (default false).</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String optionString = Utils.getOption('I', options);
    if (optionString.length() != 0) {
      setMaxIterations(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMinChange((new Integer(optionString)).intValue());
    }
    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setNumClusters(Integer.parseInt(optionString));
    } 
    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setNumRestarts((new Integer(optionString)).intValue());
    }    
    setNotUnifyNorm(Utils.getFlag('U', options));    
    setDebug(Utils.getFlag('V', options));
    
    super.setOptions(options);
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector<Option> result = new Vector<Option>();
    result.addElement(new Option("\tmaximum number of iterations\n"
	+ "\t(default 100).", "I", 1, "-I <num>"));
    result.addElement(new Option(
	"\tminimum number of changes in a single iteration\n"
	+ "\t(default 0).", "M", 1, "-M <num>"));
    result.addElement(new Option("\tnumber of clusters.\n" + "\t(default 2).",
	"N", 1, "-N <num>"));
    result.addElement(new Option("\tnumber of restarts.\n" 
	+ "\t(default 5).", "R", 1, "-R <num>"));
    result.addElement(new Option("\tset not to normalize the data\n" 
	+ "\t(default true).", "U", 0, "-U"));
    result.addElement(new Option("\tset to output debug info\n" 
	+ "\t(default false).", "V", 0, "-V"));
    
    Enumeration en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement((Option) en.nextElement());
    
    return result.elements();
  }
  
  /**
   * Gets the current settings.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    Vector<String> result;
    result = new Vector<String>();
    result.add("-I"); 
    result.add("" + getMaxIterations());
    result.add("-M"); 
    result.add("" + getMinChange());
    result.add("-N"); 
    result.add("" + getNumClusters());
    result.add("-R"); 
    result.add("" + getNumRestarts());
    if(getNotUnifyNorm()) {
      result.add("-U");
    }
    if(getDebug()) {
      result.add("-V");
    }
    
    String[] options = super.getOptions();
    for (int i = 0; i < options.length; i++){
      result.add(options[i]);
    }
    return result.toArray(new String[result.size()]);	  
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, clusterer may output additional info to " +
      "the console.";
  }
  
  /**
   * Set debug mode - verbose output
   * @param v true for verbose output
   */
  public void setDebug (boolean v) {
    m_verbose = v;
  }
  
  /**
   * Get debug mode
   * @return true if debug mode is set
   */
  public boolean getDebug () {
    return  m_verbose;
  }
  
  /**
   * Returns the tip text for this property.
   * @return tip text for this property
   */
  public String maxIterationsTipText() {
    return "set maximum number of iterations (default 100)";
  }

  /**
   * Set the max number of iterations
   * @param i max number of iterations
   */
  public void setMaxIterations(int i) {
    m_maxLoop = i;
  }

  /**
   * Get the max number of iterations
   * @return max number of iterations
   */
  public int getMaxIterations() {
    return m_maxLoop;
  }

  /**
   * Returns the tip text for this property.
   * @return tip text for this property
   */
  public String minChangeTipText() {
    return "set minimum number of changes (default 0)";
  }
  
  /**
   * set the minimum number of changes
   * @param m the minimum number of changes
   */
  public void setMinChange(int m) {
    m_minChange = m;
  }
  
  /**
   * get the minimum number of changes
   * @return the minimum number of changes
   */
  public int getMinChange() {
    return m_minChange;
  }
  
  /**
   * Returns the tip text for this property.
   * @return tip text for this property
   */
  public String numClustersTipText() {
    return "set number of clusters (default 2)";
  }

  /**
   * Set the number of clusters
   * @param n number of clusters
   */
  public void setNumClusters(int n) {
    m_numCluster = n;
  }
  
  /**
   * Get the number of clusters
   * @return the number of clusters
   */
  public int getNumClusters() {
    return m_numCluster;
  }
  
  /**
   * Get the number of clusters
   * @return the number of clusters
   */
  public int numberOfClusters() {
    return m_numCluster;
  }
  
  /**
   * Returns the tip text for this property.
   * @return tip text for this property
   */
  public String numRestartsTipText() {    
    return "set number of restarts (default 5)";
  }
  
  /**
   * Set the number of restarts
   * @param i number of restarts
   */
  public void setNumRestarts(int i) {
    m_numRestarts = i;
  }
  
  /**
   * Get the number of restarts
   * @return number of restarts
   */
  public int getNumRestarts(){
    return m_numRestarts;
  } 
  
  /**
   * Returns the tip text for this property.
   * @return tip text for this property
   */
  public String notUnifyNormTipText() {
    return "set whether to normalize each instance to a unify prior probability (eg. 1).";
  }
  
  /**
   * Set whether to normalize instances to unify prior probability 
   * before building the clusterer
   * @param b true to normalize, otherwise false
   */
  public void setNotUnifyNorm(boolean b){
    m_uniformPrior = !b;
  }
  
  /**
   * Get whether to normalize instances to unify prior probability 
   * before building the clusterer
   * @return true if set to normalize, false otherwise 
   */
  public boolean getNotUnifyNorm() {
    return !m_uniformPrior;
  }
  
  /**
   * Returns a string describing this clusterer
   * @return a description of the clusterer suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the sequential information bottleneck algorithm.\n\n" +
    		"Note: only hard clustering scheme is supported. sIB assign for each " +
    		"instance the cluster that have the minimum cost/distance to the instance. " +
    		"The trade-off beta is set to infinite so 1/beta is zero.\n\n" +
    		"For more information, see:\n\n"
    		+getTechnicalInformation().toString();
  }
  
  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Noam Slonim and Nir Friedman and Naftali Tishby");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.TITLE, "Unsupervised document classification using sequential information maximization");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 25th International ACM SIGIR Conference on Research and Development in Information Retrieval");
    result.setValue(Field.PAGES, "129-136");
    
    return result;
  }
  
  /**
   * Returns default capabilities of the clusterer.
   * @return      the capabilities of this clusterer
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    return result;
  }
  
  public String toString(){
    StringBuffer text = new StringBuffer();
    text.append("\nsIB\n===\n");
    text.append("\nNumber of clusters: " + m_numCluster + "\n");
    
    for (int j = 0; j < m_numCluster; j++) {
      text.append("\nCluster: " + j + " Size : " + bestT.size(j) + " Prior probability: " 
		  + Utils.doubleToString(bestT.Pt[j], 4) + "\n\n");
      for (int i = 0; i < m_numAttributes; i++) {
	text.append("Attribute: " + m_data.attribute(i).name() + "\n");
	text.append("Probability given the cluster = " 
	      + Utils.doubleToString(bestT.Py_t.get(i, j), 4) 
	      + "\n");
      }
    }
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }
  
  public static void main(String[] argv) {
    runClusterer(new sIB(), argv);  
  }
}
