/*

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
 * HierarchicalClusterer.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
*/
/**
 <!-- globalinfo-start -->
 * Hierarchical clustering class.
 * Implements a number of classic hierarchical clustering methods.
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  number of clusters
 * </pre>
 * 
 * 
 * <pre> -L
 *  Link type (Single, Complete, Average, Mean, Centroid, Ward, Adjusted complete)
 *  [SINGLE|COMPLETE|AVERAGE|MEAN|CENTROID|WARD|ADJCOMLPETE]
 * </pre>
 * 
 * <pre> -A
 * Distance function to use. (default: weka.core.EuclideanDistance)
 * </pre>
 *
 * <pre> -P
 * Print hierarchy in Newick format, which can be used for display in other programs.
 * </pre>
 *  
 *<!-- options-end -->
 *
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz, remco@cs.waikato.ac.nz)
 * @version $Revision$
 */

package weka.clusterers;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.DistanceFunction;
import weka.core.Drawable;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

public class HierarchicalClusterer extends AbstractClusterer implements OptionHandler, CapabilitiesHandler, Drawable {
	private static final long serialVersionUID = 1L;

	/** training data **/
	Instances m_instances;

	/** number of clusters desired in clustering **/
	int m_nNumClusters = 2;
	public void setNumClusters(int nClusters) {m_nNumClusters = Math.max(1,nClusters);}
	public int getNumClusters() {return m_nNumClusters;}
	
	/** distance function used for comparing members of a cluster **/
	protected DistanceFunction m_DistanceFunction = new EuclideanDistance();
	public DistanceFunction getDistanceFunction() {return m_DistanceFunction;}
  	public void setDistanceFunction(DistanceFunction distanceFunction) {m_DistanceFunction = distanceFunction;}

	/** used for priority queue for efficient retrieval of pair of clusters to merge**/
	class Tuple {
		public Tuple(double d, int i, int j) {
			m_fDist = d;
			m_iCluster1 = i;
			m_iCluster2 = j;
		}
		double m_fDist;
		int m_iCluster1;
		int m_iCluster2;
	}
	/** comparator used by priority queue**/
	class TupleComparator implements Comparator<Tuple> {
		public int compare(Tuple o1, Tuple o2) {
			if (o1.m_fDist < o2.m_fDist) {
				return -1;
			} else if (o1.m_fDist == o2.m_fDist) {
				return 0;
			}
			return 1;
		}
	}

	/** the various link types */
	final static int SINGLE = 0;
	final static int COMPLETE = 1;
	final static int AVERAGE = 2;
	final static int MEAN = 3;
	final static int CENTROID = 4;
	final static int WARD = 5;
	final static int ADJCOMLPETE = 6;
	public static final Tag[] TAGS_LINK_TYPE = {
	  new Tag(SINGLE, "SINGLE"),
	  new Tag(COMPLETE, "COMPLETE"),
	  new Tag(AVERAGE, "AVERAGE"),
	  new Tag(MEAN, "MEAN"),
	  new Tag(CENTROID, "CENTROID"),
	  new Tag(WARD, "WARD"),
	  new Tag(ADJCOMLPETE,"ADJCOMLPETE")
	};

	/**
	 * Holds the Link type used calculate distance between clusters
	 */
	int m_nLinkType = SINGLE;
	
	boolean m_bPrintNewick = true;;
	public boolean getPrintNewick() {return m_bPrintNewick;}
	public void setPrintNewick(boolean bPrintNewick) {m_bPrintNewick = bPrintNewick;}
	
	public void setLinkType(SelectedTag newLinkType) {
		if (newLinkType.getTags() == TAGS_LINK_TYPE) {
			m_nLinkType = newLinkType.getSelectedTag().getID();
		}
	}

	public SelectedTag getLinkType() {
		return new SelectedTag(m_nLinkType, TAGS_LINK_TYPE);
	}

	/** class representing node in cluster hierarchy **/
	class Node implements Serializable {
		Node m_left;
		Node m_right;
		Node m_parent;
		int m_iLeftInstance;
		int m_iRightInstance;
		double m_height = 0;
		public String toString(int attIndex) {
			if (m_left == null) {
				if (m_right == null) {
					double m_fLength = m_height;  
					return "(" + m_instances.instance(m_iLeftInstance).stringValue(attIndex) + ":" + m_fLength + "," +
					             m_instances.instance(m_iRightInstance).stringValue(attIndex) +":" + m_fLength + ")";// + ":" + m_height;
				} else {
					double m_fLeftLength = m_height;  
					double m_fRightLength = m_height- m_right.m_height;  
					return "(" + m_instances.instance(m_iLeftInstance).stringValue(attIndex) + ":" + m_fLeftLength + "," +
						m_right.toString(attIndex) + ":" + m_fRightLength + ")";// + ":" + m_height;
				}
			} else {
				if (m_right == null) {
					double m_fLeftLength = m_height- m_left.m_height;  
					double m_fRightLength = m_height;  
					return "(" + m_left.toString(attIndex) + ":" + m_fLeftLength + "," +
					             m_instances.instance(m_iRightInstance).stringValue(attIndex) + ":" + m_fRightLength + ")";// + ":" + m_height;
				} else {
					double m_fLeftLength = m_height- m_left.m_height;  
					double m_fRightLength = m_height- m_right.m_height;  
					return "(" + m_left.toString(attIndex) + ":" + m_fLeftLength + "," +m_right.toString(attIndex) + ":" + m_fRightLength + ")";// + ":" + m_height;
				}
			}
		}
	} // class Node
	Node [] m_clusters;
	int [] m_nClusterNr;
	
	
	@Override
	public void buildClusterer(Instances data) throws Exception {
		m_instances = data;
		int nInstances = m_instances.numInstances();
		if (nInstances == 0) {
			return;
		}
		m_DistanceFunction.setInstances(m_instances);
		// use array of integer vectors to store cluster indices,
		// starting with one cluster per instance
		Vector<Integer> [] nClusterID = new Vector[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++) {
			nClusterID[i] = new Vector<Integer>();
			nClusterID[i].add(i);
		}
		// calculate distance matrix
		int nClusters = data.numInstances();
		PriorityQueue<Tuple> queue = new PriorityQueue<Tuple>(nClusters*nClusters/2, new TupleComparator());
		double [][] fDistance0 = new double[nClusters][nClusters];
		for (int i = 0; i < nClusters; i++) {
			fDistance0[i][i] = 0;
			for (int j = i+1; j < nClusters; j++) {
				fDistance0[i][j] = getDistance0(nClusterID[i], nClusterID[j]);
				fDistance0[j][i] = fDistance0[i][j];
				queue.add(new Tuple(fDistance0[i][j], i, j));
			}
		}

		
		// used for keeping track of hierarchy
		Node [] clusterNodes = new Node[nInstances];
		
		while (nClusters > m_nNumClusters) {
			// find closest two clusters
			/* simple but inefficient implementation
			double fMinDistance = Double.MAX_VALUE;
			int iMin1 = -1;
			int iMin2 = -1;
			for (int i = 0; i < nInstances; i++) {
				if (nClusterID[i] != null) {
					for (int j = i+1; j < nInstances; j++) {
						if (nClusterID[j] != null) {
							double fDist = fDistance[i][j];
							if (fDist < fMinDistance) {
								fMinDistance = fDist;
								iMin1 = i;
								iMin2 = j;
							}
						}
					}
				}
			}
			*/
			// use priority queue to find next best pair to cluster
			Tuple t;
			do {
				t = queue.poll();
			} while (t!=null && (nClusterID[t.m_iCluster1] == null || nClusterID[t.m_iCluster2] == null));
			int iMin1 = t.m_iCluster1;
			int iMin2 = t.m_iCluster2;
			
			// merge  clusters
			nClusterID[iMin1].addAll(nClusterID[iMin2]);
			nClusterID[iMin2] = null;
			for (int i = 0; i < nInstances; i++) {
				if (i != iMin1 && nClusterID[i] != null) {
					int i1 = Math.min(iMin1,i);
					int i2 = Math.max(iMin1,i);
					double fDistance = getDistance(fDistance0, nClusterID[i1], nClusterID[i2]);
					queue.add(new Tuple(fDistance, i1, i2));
				}
			}
			
			// track hierarchy
			Node node = new Node();
			if (clusterNodes[iMin1] == null) {
				node.m_iLeftInstance = iMin1;
				node.m_height = 1; 
			} else {
				node.m_left = clusterNodes[iMin1];
				clusterNodes[iMin1].m_parent = node;
				node.m_height = clusterNodes[iMin1].m_height + 1; 
			}
			if (clusterNodes[iMin2] == null) {
				node.m_iRightInstance = iMin2;
				node.m_height = Math.max(1,node.m_height); 
			} else {
				node.m_right = clusterNodes[iMin2];
				clusterNodes[iMin2].m_parent = node;
				node.m_height = Math.max(clusterNodes[iMin2].m_height + 1, node.m_height); 
			}
			clusterNodes[iMin1] = node;
			
			nClusters--;
		}
		
		// move all clusters in m_nClusterID array
		// & collect hierarchy
		int iCurrent = 0;
		m_clusters = new Node[m_nNumClusters];
		m_nClusterNr = new int[nInstances];
		for (int i = 0; i < nInstances; i++) {
			if (nClusterID[i] != null) {
				for (int j = 0; j < nClusterID[i].size(); j++) {
					m_nClusterNr[nClusterID[i].elementAt(j)] = iCurrent;
				}
				m_clusters[iCurrent] = clusterNodes[i];
				iCurrent++;
			}
		}
		
	} // buildClusterer

	double getDistance0(Vector<Integer> cluster1, Vector<Integer> cluster2) {
		double fBestDist = Double.MAX_VALUE;
		switch (m_nLinkType) {
		case SINGLE:
			// find single link distance aka minimum link, which is the closest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = Double.MAX_VALUE;
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				for (int j = 0; j < cluster2.size(); j++) {
					Instance instance2 = m_instances.instance(cluster2.elementAt(j));
					double fDist = m_DistanceFunction.distance(instance1, instance2);
					if (fBestDist > fDist) {
						fBestDist = fDist;
					}
				}
			}
			break;
		case COMPLETE:
		case ADJCOMLPETE:
			// find complete link distance aka maximum link, which is the largest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				for (int j = 0; j < cluster2.size(); j++) {
					Instance instance2 = m_instances.instance(cluster2.elementAt(j));
					double fDist = m_DistanceFunction.distance(instance1, instance2);
					if (fBestDist < fDist) {
						fBestDist = fDist;
					}
				}
			}
			if (m_nLinkType == COMPLETE) {
				break;
			}
			// calculate adjustment, which is the largest within cluster distance
			double fMaxDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				for (int j = i+1; j < cluster1.size(); j++) {
					Instance instance2 = m_instances.instance(cluster1.elementAt(j));
					double fDist = m_DistanceFunction.distance(instance1, instance2);
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			for (int i = 0; i < cluster2.size(); i++) {
				Instance instance1 = m_instances.instance(cluster2.elementAt(i));
				for (int j = i+1; j < cluster2.size(); j++) {
					Instance instance2 = m_instances.instance(cluster2.elementAt(j));
					double fDist = m_DistanceFunction.distance(instance1, instance2);
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			fBestDist -= fMaxDist;
			break;
		case AVERAGE:
			// finds average distance between the elements of the two clusters
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				for (int j = 0; j < cluster2.size(); j++) {
					Instance instance2 = m_instances.instance(cluster2.elementAt(j));
					fBestDist += m_DistanceFunction.distance(instance1, instance2);
				}
			}
			fBestDist /= (cluster1.size() * cluster2.size());
			break;
		case MEAN: 
			{
				// calculates the mean distance of a merged cluster (akak Group-average agglomerative clustering)
				Vector<Integer> merged = new Vector<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				fBestDist = 0;
				for (int i = 0; i < merged.size(); i++) {
					Instance instance1 = m_instances.instance(merged.elementAt(i));
					for (int j = i+1; j < merged.size(); j++) {
						Instance instance2 = m_instances.instance(merged.elementAt(j));
						fBestDist += m_DistanceFunction.distance(instance1, instance2);
					}
				}
				int n = merged.size();
				fBestDist /= (n*(n-1.0)/2.0);
			}
			break;
		case CENTROID:
			// finds the distance of the centroids of the clusters
			double [] fValues1 = new double[m_instances.numAttributes()];
			double [] fValues2 = new double[m_instances.numAttributes()];
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				Instance instance2 = m_instances.instance(cluster1.elementAt(i));
				for (int j = 0; j < m_instances.numAttributes(); j++) {
					fValues1[j] += instance1.value(j);
					fValues2[j] += instance2.value(j);
				}
			}
			for (int j = 0; j < m_instances.numAttributes(); j++) {
				fValues1[j] /= cluster1.size();
				fValues2[j] += cluster2.size();
			}
			// set up two instances for distance function
			Instance instance1 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
			Instance instance2 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
			for (int j = 0; j < m_instances.numAttributes(); j++) {
				instance1.setValue(j, fValues1[j]);
				instance2.setValue(j, fValues1[j]);
			}
			fBestDist = m_DistanceFunction.distance(instance1, instance2);
			break;
		case WARD:
			{
				// finds the distance of the change in caused by merging the cluster.
				// The information of a cluster is calculated as the error sum of squares of the
				// centroids of the cluster and its members.
				double ESS1 = calcESS(cluster1);
				double ESS2 = calcESS(cluster2);
				Vector<Integer> merged = new Vector<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				double ESS = calcESS(merged);
				fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
			}
			break;
		}
		return fBestDist;
	} // getDistance

	/** calculate the distance between two clusters 
	 * @param cluster1 list of indices of instances in the first cluster
	 * @param cluster2 dito for second cluster
	 * @return distance between clusters based on link type
	 */
	double getDistance(double [][] fDistance, Vector<Integer> cluster1, Vector<Integer> cluster2) {
		double fBestDist = Double.MAX_VALUE;
		switch (m_nLinkType) {
		case SINGLE:
			// find single link distance aka minimum link, which is the closest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = Double.MAX_VALUE;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2  = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist > fDist) {
						fBestDist = fDist;
					}
				}
			}
			break;
		case COMPLETE:
		case ADJCOMLPETE:
			// find complete link distance aka maximum link, which is the largest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist < fDist) {
						fBestDist = fDist;
					}
				}
			}
			if (m_nLinkType == COMPLETE) {
				break;
			}
			// calculate adjustment, which is the largest within cluster distance
			double fMaxDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = i+1; j < cluster1.size(); j++) {
					int i2 = cluster1.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			for (int i = 0; i < cluster2.size(); i++) {
				int i1 = cluster2.elementAt(i);
				for (int j = i+1; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			fBestDist -= fMaxDist;
			break;
		case AVERAGE:
			// finds average distance between the elements of the two clusters
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.elementAt(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.elementAt(j);
					fBestDist += fDistance[i1][i2];
				}
			}
			fBestDist /= (cluster1.size() * cluster2.size());
			break;
		case MEAN: 
			{
				// calculates the mean distance of a merged cluster (akak Group-average agglomerative clustering)
				Vector<Integer> merged = new Vector<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				fBestDist = 0;
				for (int i = 0; i < merged.size(); i++) {
					int i1 = merged.elementAt(i);
					for (int j = i+1; j < merged.size(); j++) {
						int i2 = merged.elementAt(j);
						fBestDist += fDistance[i1][i2];
					}
				}
				int n = merged.size();
				fBestDist /= (n*(n-1.0)/2.0);
			}
			break;
		case CENTROID:
			// finds the distance of the centroids of the clusters
			double [] fValues1 = new double[m_instances.numAttributes()];
			double [] fValues2 = new double[m_instances.numAttributes()];
			for (int i = 0; i < cluster1.size(); i++) {
				Instance instance1 = m_instances.instance(cluster1.elementAt(i));
				Instance instance2 = m_instances.instance(cluster1.elementAt(i));
				for (int j = 0; j < m_instances.numAttributes(); j++) {
					fValues1[j] += instance1.value(j);
					fValues2[j] += instance2.value(j);
				}
			}
			for (int j = 0; j < m_instances.numAttributes(); j++) {
				fValues1[j] /= cluster1.size();
				fValues2[j] += cluster2.size();
			}
			// set up two instances for distance function
			Instance instance1 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
			Instance instance2 = (Instance) m_instances.instance(cluster1.elementAt(0)).copy();
			for (int j = 0; j < m_instances.numAttributes(); j++) {
				instance1.setValue(j, fValues1[j]);
				instance2.setValue(j, fValues1[j]);
			}
			fBestDist = m_DistanceFunction.distance(instance1, instance2);
			break;
		case WARD:
			{
				// finds the distance of the change in caused by merging the cluster.
				// The information of a cluster is calculated as the error sum of squares of the
				// centroids of the cluster and its members.
				double ESS1 = calcESS(cluster1);
				double ESS2 = calcESS(cluster2);
				Vector<Integer> merged = new Vector<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				double ESS = calcESS(merged);
				fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
			}
			break;
		}
		return fBestDist;
	} // getDistance

	/** calculated error sum-of-squares for instances wrt centroid **/
	double calcESS(Vector<Integer> cluster) {
		double [] fValues1 = new double[m_instances.numAttributes()];
		for (int i = 0; i < cluster.size(); i++) {
			Instance instance = m_instances.instance(cluster.elementAt(i));
			for (int j = 0; j < m_instances.numAttributes(); j++) {
				fValues1[j] += instance.value(j);
			}
		}
		for (int j = 0; j < m_instances.numAttributes(); j++) {
			fValues1[j] /= cluster.size();
		}
		// set up two instances for distance function
		Instance centroid = (Instance) m_instances.instance(cluster.elementAt(0)).copy();
		for (int j = 0; j < m_instances.numAttributes(); j++) {
			centroid.setValue(j, fValues1[j]);
		}
		double fESS = 0;
		for (int i = 0; i < cluster.size(); i++) {
			Instance instance = m_instances.instance(cluster.elementAt(i));
			fESS += m_DistanceFunction.distance(centroid, instance);
		}
		return fESS / cluster.size(); 
	} // calcESS
	
	@Override
	/** instances are assigned a cluster by finding the instance in the training data 
	 * with the closest distance to the instance to be clustered. The cluster index of
	 * the training data point is taken as the cluster index.
	 */
	public int clusterInstance(Instance instance) throws Exception {
		if (m_instances.numInstances() == 0) {
			return 0;
		}
		double fBestDist = Double.MAX_VALUE;
		int iBestInstance = -1;
		for (int i = 0; i < m_instances.numInstances(); i++) {
			double fDist = m_DistanceFunction.distance(instance, m_instances.instance(i));
			if (fDist < fBestDist) {
				fBestDist = fDist;
				iBestInstance = i;
			}
		}
		return m_nClusterNr[iBestInstance];
	}

	@Override
	/** create distribution with all clusters having zero probability, except the
	 * cluster the instance is assigned to.
	 */
	public double[] distributionForInstance(Instance instance) throws Exception {
		if (numberOfClusters() == 0) {
			double [] p = new double[1];
			p[0] = 1;
			return p;
		}
		double [] p = new double[numberOfClusters()];
		p[clusterInstance(instance)] = 1.0;
		return p;
	}

	@Override
	public Capabilities getCapabilities() {
	    Capabilities result = new Capabilities(this);
	    result.disableAll();
	    result.enable(Capability.NO_CLASS);

	    // attributes
	    result.enable(Capability.NOMINAL_ATTRIBUTES);
	    result.enable(Capability.NUMERIC_ATTRIBUTES);
	    result.enable(Capability.DATE_ATTRIBUTES);
	    result.enable(Capability.MISSING_VALUES);
	    result.enable(Capability.STRING_ATTRIBUTES);

	    // other
	    result.setMinimumNumberInstances(0);
	    return result;
	}

	@Override
	public int numberOfClusters() throws Exception {
		return Math.min(m_nNumClusters, m_instances.numInstances());
	}

	  /**
	   * Returns an enumeration describing the available options.
	   *
	   * @return an enumeration of all the available options.
	   */
	  public Enumeration listOptions() {

	    Vector newVector = new Vector(8);

	    newVector.addElement(new Option(
	            "\tnumber of clusters",
	  	      "N", 1,"-N <Nr Of Clusters>"));
	    newVector.addElement(new Option(
	            "\tFlag to indicate the cluster should be printed in Newick format.",
	  	      "P", 0,"-P"));
		newVector.addElement(
				new Option(
					"Link type (Single, Complete, Average, Mean, Centroid, Ward, Adjusted complete)", "L", 1,
					"-L [SINGLE|COMPLETE|AVERAGE|MEAN|CENTROID|WARD|ADJCOMLPETE]"));
	    newVector.add(new Option(
	    		"\tDistance function to use.\n"
	    		+ "\t(default: weka.core.EuclideanDistance)",
	    		"A", 1,"-A <classname and options>"));
	    return newVector.elements();
	  }

	  /**
	   * Parses a given list of options. <p/>
	   *
	   <!-- options-start -->
	   * Valid options are: <p/>
	   * 
	   <!-- options-end -->
	   *
	   * @param options the list of options as an array of strings
	   * @throws Exception if an option is not supported
	   */
	  public void setOptions(String[] options) throws Exception {
		    m_bPrintNewick = Utils.getFlag('P', options);

		    String optionString = Utils.getOption('N', options); 
		    if (optionString.length() != 0) {
		      Integer temp = new Integer(optionString);
		      setNumClusters(temp);
		    }
		    else {
		      setNumClusters(2);
		    }
	    

	  String sLinkType = Utils.getOption('L', options);


		if (sLinkType.compareTo("SINGLE") == 0) {setLinkType(new SelectedTag(SINGLE, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("COMPLETE") == 0) {setLinkType(new SelectedTag(COMPLETE, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("AVERAGE") == 0) {setLinkType(new SelectedTag(AVERAGE, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("MEAN") == 0) {setLinkType(new SelectedTag(MEAN, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("CENTROID") == 0) {setLinkType(new SelectedTag(CENTROID, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("WARD") == 0) {setLinkType(new SelectedTag(WARD, TAGS_LINK_TYPE));}
		if (sLinkType.compareTo("ADJCOMLPETE") == 0) {setLinkType(new SelectedTag(ADJCOMLPETE, TAGS_LINK_TYPE));}
		
	    String nnSearchClass = Utils.getOption('A', options);
	    if(nnSearchClass.length() != 0) {
	      String nnSearchClassSpec[] = Utils.splitOptions(nnSearchClass);
	      if(nnSearchClassSpec.length == 0) { 
	        throw new Exception("Invalid DistanceFunction specification string."); 
	      }
	      String className = nnSearchClassSpec[0];
	      nnSearchClassSpec[0] = "";

	      setDistanceFunction( (DistanceFunction)
	                            Utils.forName( DistanceFunction.class, 
	                                           className, nnSearchClassSpec) );
	    }
	    else {
	      setDistanceFunction(new EuclideanDistance());
	    }
	    
	    Utils.checkForRemainingOptions(options);
	  }

	  /**
	   * Gets the current settings of the clusterer.
	   *
	   * @return an array of strings suitable for passing to setOptions()
	   */
	  public String [] getOptions() {

	    String [] options = new String [12];
	    int current = 0;

	    options[current++] = "-N";
	    options[current++] = "" + getNumClusters();
	    
	    options[current++] = "-L";
		switch (m_nLinkType) {
			case (SINGLE) :options[current++] = "SINGLE";break;
			case (COMPLETE) :options[current++] = "COMPLETE";break;
			case (AVERAGE) :options[current++] = "AVERAGE";break;
			case (MEAN) :options[current++] = "MEAN";break;
			case (CENTROID) :options[current++] = "CENTROID";break;
			case (WARD) :options[current++] = "WARD";break;
			case (ADJCOMLPETE) :options[current++] = "ADJCOMLPETE";break;
		}
		if (m_bPrintNewick) {
			options[current++] = "-P";
		}
		options[current++] = "-A";
		options[current++] = (m_DistanceFunction.getClass().getName() + " " +
	                   Utils.joinOptions(m_DistanceFunction.getOptions())).trim();
	    
	    while (current < options.length) {
	      options[current++] = "";
	    }
	    
	    return options;
	  }
	  public String toString() {
		  StringBuffer buf = new StringBuffer();
		  int attIndex = m_instances.classIndex();
		  if (attIndex < 0) {
			  // try find a string, or last attribute otherwise
			  attIndex = 0;
			  while (attIndex < m_instances.numAttributes()-1) {
				  if (m_instances.attribute(attIndex).isString()) {
					  break;
				  }
				  attIndex++;
			  }
		  }
		  try {
			if (m_bPrintNewick && (numberOfClusters() > 0)) {
				  for (int i = 0; i < m_clusters.length; i++) {
					  if (m_clusters[i] != null) {
						  buf.append("Cluster " + i + "\n");
						  buf.append(m_clusters[i].toString(attIndex));
						  buf.append("\n\n");
					  }
				  }
			  }
		} catch (Exception e) {
			e.printStackTrace();
		}
		  return buf.toString();
	  }

	  /**
	   * @return a string to describe the NumClusters
	   */
	  public String numClustersTipText() {
	    return "Sets the number of clusters. " +
	    "If a single hierarchy is desired, set this to 1.";
	  }

	  /**
	   * @return a string to describe the print Newick flag
	   */
	  public String printNewickTipText() {
	    return "Flag to indicate whether the cluster should be print in Newick format." +
	    " This can be useful for display in other programs. However, for large datasets" +
	    " a lot of text may be produced, which may not be a nuisance when the Newick format" +
	    " is not required";
	  }

	  /**
	   * @return a string to describe the distance function
	   */
	  public String distanceFunctionTipText() {
	    return "Sets the distance function, which measures the distance between two individual. " +
	    "instances (or possibly the distance between an instance and the centroid of a cluster" +
	    "depending on the Link type).";
	  }

	  /**
	   * @return a string to describe the Link type
	   */
	  public String linkTypeTipText() {
	    return "Sets the method used to measure the distance between two clusters.\n" +
	    "SINGLE:\n" +
	    " find single link distance aka minimum link, which is the closest distance between" +
	    " any item in cluster1 and any item in cluster2\n" +
	    "COMPLETE:\n" +
	    " find complete link distance aka maximum link, which is the largest distance between" +
	    " any item in cluster1 and any item in cluster2\n" +
	    "ADJCOMLPETE:\n" +
	    " as COMPLETE, but with adjustment, which is the largest within cluster distance\n" +
	    "AVERAGE:\n" +
	    " finds average distance between the elements of the two clusters\n" +
	    "MEAN: \n" +
	    " calculates the mean distance of a merged cluster (akak Group-average agglomerative clustering)\n" +
	    "CENTROID:\n" +
	    " finds the distance of the centroids of the clusters\n" +
	    "WARD:\n" +
	    " finds the distance of the change in caused by merging the cluster." +
	    " The information of a cluster is calculated as the error sum of squares of the" +
	    " centroids of the cluster and its members.\n";
	  }

	  /**
	   * This will return a string describing the clusterer.
	   * @return The string.
	   */
	  public String globalInfo() {
	    return 
	    "Hierarchical clustering class.\n" +
	    "Implements a number of classic agglomorative (i.e. bottom up) hierarchical clustering methods" +
	    "based on .";
	  }
	  
	  public static void main(String [] argv) {
		    runClusterer(new HierarchicalClusterer(), argv);
		  }
	@Override
	public String graph() throws Exception {
		if (numberOfClusters() == 0) {
			  return "Newick:(no,clusters)";
		}
		  int attIndex = m_instances.classIndex();
		  if (attIndex < 0) {
			  // try find a string, or last attribute otherwise
			  attIndex = 0;
			  while (attIndex < m_instances.numAttributes()-1) {
				  if (m_instances.attribute(attIndex).isString()) {
					  break;
				  }
				  attIndex++;
			  }
		  }
		  String sNewick = m_clusters[0].toString(attIndex);
		  return "Newick:" + sNewick;
	}
	@Override
	public int graphType() {
		return Drawable.Newick;
	}
} // class HierarchicalClusterer
