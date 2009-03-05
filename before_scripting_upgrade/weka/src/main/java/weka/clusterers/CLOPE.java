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
 *    Copyright (C) 2008
 *    & Alexander Smirnov (austellus@gmail.com)
 */
package weka.clusterers;

import java.io.Serializable;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;

/**
<!-- globalinfo-start -->
* Yiling Yang, Xudong Guan, Jinyuan You: CLOPE: a fast and effective clustering algorithm for transactional data. In: Proceedings of the eighth ACM SIGKDD international conference on Knowledge discovery and data mining, 682-687, 2002.
* <p/>
<!-- globalinfo-end -->
 *
<!-- technical-bibtex-start -->
* BibTeX:
* <pre>
* &#64;inproceedings{Yang2002,
*    author = {Yiling Yang and Xudong Guan and Jinyuan You},
*    booktitle = {Proceedings of the eighth ACM SIGKDD international conference on Knowledge discovery and data mining},
*    pages = {682-687},
*    publisher = {ACM  New York, NY, USA},
*    title = {CLOPE: a fast and effective clustering algorithm for transactional data},
*    year = {2002}
* }
* </pre>
* <p/>
<!-- technical-bibtex-end -->
 *
<!-- options-start -->
* Valid options are: <p/>
* 
* <pre> -R &lt;num&gt;
*  Repulsion
*  (default 2.6)</pre>
* 
<!-- options-end -->
 *
 * @author Alexander Smirnov (austellus@gmail.com)
 * @version $Revision$
 */
public class CLOPE
  extends AbstractClusterer
  implements OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -567567567567588L;

  /**
   * Inner class for cluster of CLOPE.
   *
   * @see Serializable
   */
  private class CLOPECluster implements Serializable {

    /**
     * Number of transactions
     */
    public int N = 0; //number of transactions
    
    /**
     * Number of distinct items (or width)
     */
    public int W = 0;
    
    /**
     * Size of cluster
     */
    public int S = 0;
    
    /**
     * Hash of <item, occurrence> pairs
     */
    public HashMap occ = new HashMap();

    /**
     *  Add item to cluster
     */
    public void AddItem(String Item) {
      int count;
      if (!this.occ.containsKey(Item)) {
	this.occ.put(Item, 1);
      } else {
	count = (Integer) this.occ.get(Item);
	count++;
	this.occ.remove(Item);
	this.occ.put(Item, count);
      }
      this.S++;
    }

    public void AddItem(Integer Item) {
      int count;
      if (!this.occ.containsKey(Item)) {
	this.occ.put(Item, 1);
      } else {
	count = (Integer) this.occ.get(Item);
	count++;
	this.occ.remove(Item);
	this.occ.put(Item, count);
      }
      this.S++;
    }

    /**
     *  Delete item from cluster
     */
     public void DeleteItem(String Item) {
      int count;

      count = (Integer) this.occ.get(Item);

      if (count == 1) {
	this.occ.remove(Item);

      } else {
	count--;
	this.occ.remove(Item);
	this.occ.put(Item, count);
      }
      this.S--;
     }

     public void DeleteItem(Integer Item) {
       int count;

       count = (Integer) this.occ.get(Item);

       if (count == 1) {
	 this.occ.remove(Item);

       } else {
	 count--;
	 this.occ.remove(Item);
	 this.occ.put(Item, count);
       }
       this.S--;
     }

     /**
      * Calculate Delta
      */
      public double DeltaAdd(Instance inst, double r) {
	//System.out.println("DeltaAdd");
	int S_new;
	int W_new;
	double profit;
	double profit_new;
	double deltaprofit;
	S_new = 0;
	W_new = occ.size();

	if (inst instanceof SparseInstance) {
	  //System.out.println("DeltaAddSparceInstance");
	  for (int i = 0; i < inst.numValues(); i++) {
	    S_new++;

	    if ((Integer) this.occ.get(inst.index(i)) == null) {
	      W_new++;
	    }
	  }
	} else {
	  for (int i = 0; i < inst.numAttributes(); i++) {
	    if (!inst.isMissing(i)) {
	      S_new++;
	      if ((Integer) this.occ.get(i + inst.toString(i)) == null) {
		W_new++;
	      }
	    }
	  }
	}
	S_new += S;


	if (N == 0) {
	  deltaprofit = S_new / Math.pow(W_new, r);
	} else {
	  profit = S * N / Math.pow(W, r);
	  profit_new = S_new * (N + 1) / Math.pow(W_new, r);
	  deltaprofit = profit_new - profit;
	}
	return deltaprofit;
      }

      /**
       * Add instance to cluster
       */
      public void AddInstance(Instance inst) {
	if (inst instanceof SparseInstance) {
	  //  System.out.println("AddSparceInstance");
	  for (int i = 0; i < inst.numValues(); i++) {
	    AddItem(inst.index(i));
	    //  for(int i=0;i<inst.numAttributes();int++){
	    // AddItem(inst.index(i)+inst.value(i));
	  }
	} else {
	  for (int i = 0; i < inst.numAttributes(); i++) {

	    if (!inst.isMissing(i)) {

	      AddItem(i + inst.toString(i));
	    }
	  }
	}
	this.W = this.occ.size();
	this.N++;
      }

      /**
       * Delete instance from cluster
       */
      public void DeleteInstance(Instance inst) {
	if (inst instanceof SparseInstance) {
	  //   System.out.println("DeleteSparceInstance");
	  for (int i = 0; i < inst.numValues(); i++) {
	    DeleteItem(inst.index(i));
	  }
	} else {
	  for (int i = 0; i <= inst.numAttributes() - 1; i++) {

	    if (!inst.isMissing(i)) {
	      DeleteItem(i + inst.toString(i));
	    }
	  }
	}
	this.W = this.occ.size();
	this.N--;
      }
  }
  /**
   * Array of clusters
   */
  public ArrayList<CLOPECluster> clusters = new ArrayList<CLOPECluster>();
  
  /**
   * Specifies the repulsion default
   */
  protected double m_RepulsionDefault = 2.6;
  
  /**
   * Specifies the repulsion
   */
  protected double m_Repulsion = m_RepulsionDefault;
  
  /**
   * Number of clusters 
   */
  protected int m_numberOfClusters = -1;
  
  /**
   * Counter for the processed instances
   */
  protected int m_processed_InstanceID;
  
  /**
   * Number of instances
   */
  protected int m_numberOfInstances;
  
  /**
   * 
   */
  protected ArrayList<Integer> m_clusterAssignments = new ArrayList();
  
  /** 
   * whether the number of clusters was already determined
   */
  protected boolean m_numberOfClustersDetermined = false;

  public int numberOfClusters() {
    determineNumberOfClusters();
    return m_numberOfClusters;
  }

  protected void determineNumberOfClusters() {

    m_numberOfClusters = clusters.size();

    m_numberOfClustersDetermined = true;
  }

  public Enumeration listOptions() {
    Vector result = new Vector();
    result.addElement(new Option(
	"\tRepulsion\n" + "\t(default " + m_RepulsionDefault + ")",
	"R", 1, "-R <num>"));
    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
    <!-- options-start -->
    * Valid options are: <p/>
    * 
    * <pre> -R &lt;num&gt;
    *  Repulsion
    *  (default 2.6)</pre>
    * 
    <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setRepulsion(Double.parseDouble(tmpStr));
    } else {
      setRepulsion(m_RepulsionDefault);
    }
  }

  /**
   * Gets the current settings of CLOPE
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    Vector result;

    result = new Vector();

    result.add("-R");
    result.add("" + getRepulsion());

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String repulsionTipText() {
    return "Repulsion to be used.";
  }

  /**
   * set the repulsion
   *
   * @param value the repulsion
   * @throws Exception if number of clusters is negative
   */
  public void setRepulsion(double value) {
    m_Repulsion = value;
  }

  /**
   * gets the repulsion
   *
   * @return the repulsion
   */
  public double getRepulsion() {
    return m_Repulsion;
  }

  /**
   * Returns default capabilities of the clusterer.
   *
   * @return      the capabilities of this clusterer
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    // result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
  }

  /**
   * Generate Clustering via CLOPE
   * @param data The instances that need to be clustered
   * @throws java.lang.Exception If clustering was not successful
   */
  public void buildClusterer(Instances data) throws Exception {
    clusters.clear();
    m_processed_InstanceID = 0;
    m_clusterAssignments.clear();
    m_numberOfInstances = data.numInstances();
    boolean moved;
    //Phase 1 
    for (int i = 0; i < data.numInstances(); i++) {
      int clusterid = AddInstanceToBestCluster(data.instance(i));
      m_clusterAssignments.add(clusterid);

    }
    //Phase 2
    do {
      moved = false;
      for (int i = 0; i < data.numInstances(); i++) {
	m_processed_InstanceID = i;
	int clusterid = MoveInstanceToBestCluster(data.instance(i));
	if (clusterid != m_clusterAssignments.get(i)) {
	  moved = true;
	  m_clusterAssignments.set(i, clusterid);
	}
      }
    } while (!moved);
    m_processed_InstanceID = 0;
  }

  /**
   * the default constructor
   */
  public CLOPE() {
    super();
  }

  /**
   * Add instance to best cluster
   */
  public int AddInstanceToBestCluster(Instance inst) {

    double delta;
    double deltamax;
    int clustermax = -1;
    if (clusters.size() > 0) {
      int tempS = 0;
      int tempW = 0;
      if (inst instanceof SparseInstance) {
	for (int i = 0; i < inst.numValues(); i++) {
	  tempS++;
	  tempW++;
	}
      } else {
	for (int i = 0; i < inst.numAttributes(); i++) {
	  if (!inst.isMissing(i)) {
	    tempS++;
	    tempW++;
	  }
	}
      }

      deltamax = tempS / Math.pow(tempW, m_Repulsion);

      for (int i = 0; i < clusters.size(); i++) {
	CLOPECluster tempcluster = clusters.get(i);
	delta = tempcluster.DeltaAdd(inst, m_Repulsion);
	//  System.out.println("delta " + delta);
	if (delta > deltamax) {
	  deltamax = delta;
	  clustermax = i;
	}
      }
    } else {
      CLOPECluster newcluster = new CLOPECluster();
      clusters.add(newcluster);
      newcluster.AddInstance(inst);
      return clusters.size() - 1;
    }

    if (clustermax == -1) {
      CLOPECluster newcluster = new CLOPECluster();
      clusters.add(newcluster);
      newcluster.AddInstance(inst);
      return clusters.size() - 1;
    }
    clusters.get(clustermax).AddInstance(inst);
    return clustermax;
  }

  /**
   * Move instance to best cluster
   */
  public int MoveInstanceToBestCluster(Instance inst) {

    clusters.get(m_clusterAssignments.get(m_processed_InstanceID)).DeleteInstance(inst);
    m_clusterAssignments.set(m_processed_InstanceID, -1);
    double delta;
    double deltamax;
    int clustermax = -1;
    int tempS = 0;
    int tempW = 0;

    if (inst instanceof SparseInstance) {
      for (int i = 0; i < inst.numValues(); i++) {
	tempS++;
	tempW++;
      }
    } else {
      for (int i = 0; i < inst.numAttributes(); i++) {
	if (!inst.isMissing(i)) {
	  tempS++;
	  tempW++;
	}
      }
    }

    deltamax = tempS / Math.pow(tempW, m_Repulsion);
    for (int i = 0; i < clusters.size(); i++) {
      CLOPECluster tempcluster = clusters.get(i);
      delta = tempcluster.DeltaAdd(inst, m_Repulsion);
      // System.out.println("delta " + delta);
      if (delta > deltamax) {
	deltamax = delta;
	clustermax = i;
      }
    }
    if (clustermax == -1) {
      CLOPECluster newcluster = new CLOPECluster();
      clusters.add(newcluster);
      newcluster.AddInstance(inst);
      return clusters.size() - 1;
    }
    clusters.get(clustermax).AddInstance(inst);
    return clustermax;
  }

  /**
   * Classifies a given instance.
   *
   * @param instance The instance to be assigned to a cluster
   * @return int The number of the assigned cluster as an integer
   * @throws java.lang.Exception If instance could not be clustered
   * successfully
   */
  public int clusterInstance(Instance instance) throws Exception {
    if (m_processed_InstanceID >= m_numberOfInstances) {
      m_processed_InstanceID = 0;
    }
    int i = m_clusterAssignments.get(m_processed_InstanceID);
    m_processed_InstanceID++;
    return i;
  }

  /**
   * return a string describing this clusterer
   *
   * @return a description of the clusterer as a string
   */
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("CLOPE clustering results\n" +
    "========================================================================================\n\n");
    stringBuffer.append("Clustered instances: " + m_clusterAssignments.size() + "\n");
    return stringBuffer.toString() + "\n";
  }

  /**
   * Returns a string describing this DataMining-Algorithm
   * @return String Information for the gui-explorer
   */
  public String globalInfo() {
    return getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Yiling Yang and Xudong Guan and Jinyuan You");
    result.setValue(Field.TITLE, "CLOPE: a fast and effective clustering algorithm for transactional data");
    result.setValue(Field.BOOKTITLE, "Proceedings of the eighth ACM SIGKDD international conference on Knowledge discovery and data mining");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.PAGES, "682-687");
    result.setValue(Field.PUBLISHER, "ACM  New York, NY, USA");

    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments: <p>
   * -t training file [-R repulsion]
   */
  public static void main(String[] argv) {
    runClusterer(new CLOPE(), argv);
  }
}

