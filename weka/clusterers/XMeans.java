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
 *    XMeans.java
 *    Copyright (C) 2000 Mark Hall, Malcolm Ware, Gabi Schmidberger
 *
 */

package weka.clusterers;

import weka.core.AlgVector;
import weka.core.Capabilities;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.KDTree;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Cluster data using the X-means algorithm.<br/>
 * <br/>
 * X-Means is K-Means extended by an Improve-Structure part In this part of the algorithm the centers are attempted to be split in its region. The decision between the children of each center and itself is done comparing the BIC-values of the two structures.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Dan Pelleg, Andrew W. Moore: X-means: Extending K-means with Efficient Estimation of the Number of Clusters. In: Seventeenth International Conference on Machine Learning, 727-734, 2000.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;incproceedings{Pelleg2000,
 *    author = {Dan Pelleg and Andrew W. Moore},
 *    booktitle = {Seventeenth International Conference on Machine Learning},
 *    pages = {727-734},
 *    publisher = {Morgan Kaufmann},
 *    title = {X-means: Extending K-means with Efficient Estimation of the Number of Clusters},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -I &lt;num&gt;
 *  maximum number of overall iterations
 *  (default = 1).</pre>
 * 
 * <pre> -M &lt;num&gt;
 *  maximum number of iterations in the kMeans loop in
 *  the Improve-Parameter part 
 *  (default = 1000).</pre>
 * 
 * <pre> -J &lt;num&gt;
 *  maximum number of iterations in the kMeans loop
 *  for the splitted centroids in the Improve-Structure part 
 *  (default = 1000).</pre>
 * 
 * <pre> -L &lt;num&gt;
 *  minimum number of clusters
 *  (default = 2).</pre>
 * 
 * <pre> -H &lt;num&gt;
 *  maximum number of clusters
 *  (default = 4).</pre>
 * 
 * <pre> -V &lt;value&gt;
 *  distance value for binary attributes
 *  (default = 1.0).</pre>
 * 
 * <pre> -K &lt;KDTree class specification&gt;
 *  Full class name of KDTree class to use, followed
 *  by scheme options.
 *  eg: "weka.core.KDTree -P"
 *  (default = no KDTree class used).</pre>
 * 
 * <pre> -C &lt;value&gt;
 *  cutoff factor, takes the given percentage of the splitted 
 *  centroids if none of the children win
 *  (default = 0.0).</pre>
 * 
 * <pre> -K &lt;distance function class specification&gt;
 *  Full class name of Distance function class to use, followed
 *  by scheme options.
 *  eg: "weka.core.MahalanobisDistance"
 *  (default = weka.core.EuclideanDistance).</pre>
 * 
 * <pre> -N &lt;file name&gt;
 *  file to read starting centers from (ARFF format).</pre>
 * 
 * <pre> -O &lt;file name&gt;
 *  file to write centers to (ARFF format).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  random number seed (default 10).</pre>
 * 
 <!-- options-end -->
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 * @see Clusterer
 * @see OptionHandler
 */
public class XMeans 
  extends Clusterer 
  implements OptionHandler, TechnicalInformationHandler {

  /*
   * major TODOS:
   *
   * make BIC-Score replaceable by other scores
   */

  /** for serialization */
  static final long serialVersionUID = -7941793078404132616L;
  
  /** training instances */
  private Instances m_Instances = null;

  /** model information, should increase readability */
  private Instances m_Model = null;
  
  /** replace missing values in training instances */
  private ReplaceMissingValues m_ReplaceMissingFilter;

  /**
   * Distance value between true and false of binary attributes and 
   * "same" and "different" of nominal attributes (default = 1.0).
   */
  private double m_BinValue = 1.0;

  /** BIC-Score of the current model */
  double m_Bic = Double.MIN_VALUE;

  /** Distortion  */
  double [] m_Mle = null;

  /** maximum overall iterations */
  private int m_MaxIterations = 1;

  /**
   * maximum iterations to perform Kmeans part 
   * if negative, iterations are not checked
   */
  private int m_MaxKMeans = 1000;

  /** see above, but for kMeans of splitted clusters
   */
  private int m_MaxKMeansForChildren = 1000;

  /** The actual number of clusters */
  private int m_NumClusters = 2;

  /** min number of clusters to generate */
  private int m_MinNumClusters = 2;

  /** max number of clusters to generate */
  private int m_MaxNumClusters = 4;

  /** the distance function used */
  private EuclideanDistance m_DistanceF = new EuclideanDistance();

  /** cluster centers */
  private Instances m_ClusterCenters;

  /** file name of the output file for the cluster centers */
  String m_InputCenterFile = null;

  /* --> DebugVektors - USED FOR DEBUGGING */
  /** input file for the random vektors --> USED FOR DEBUGGING */
  Reader m_DebugVektorsInput = null;
  int m_DebugVektorsIndex = 0;
  Instances m_DebugVektors = null;

  /** file name of the input file for the random vektors */
  String m_DebugVektorsFile = null;

  /** input file for the cluster centers */
  Reader m_CenterInput = null;
    
  /** file name of the output file for the cluster centers */
  String m_OutputCenterFile = null;
  
  /** output file for the cluster centers */
  PrintWriter m_CenterOutput = null;
    
  /**
   * temporary variable holding cluster assignments while iterating
   */
  private int [] m_ClusterAssignments;

  /** cutoff factor - percentage of splits done in Improve-Structure part
     only relevant, if all children lost */ 
  double m_CutOffFactor = 0.5;

  /**
   * random seed
   */
  private int m_Seed = 10;

  /**
   * Index in ranges for LOW and HIGH and WIDTH
   */
  public static int R_LOW = 0;
  public static int R_HIGH = 1;
  public static int R_WIDTH = 2;

  /**
   * KDTrees class if KDTrees are used
   */
  private KDTree m_KDTree = null;

  /** counts iterations done in main loop */
  private int m_IterationCount = 0;

  /** counter to say how often kMeans was stopped by loop counter */
  private int m_KMeansStopped = 0;

  /** Number of splits prepared */
  private int m_NumSplits = 0;

  /** Number of splits accepted (including cutoff factor decisions) */
  private int m_NumSplitsDone = 0;

  /** Number of splits accepted just because of cutoff factor */
  private int m_NumSplitsStillDone = 0;

  /**
   * level of debug output, 0 is no output.
   */
  private int m_DebugLevel = 0;
  
  public static int D_PRINTCENTERS = 1;
  /** follows the splitting of the centers */
  public static int D_FOLLOWSPLIT = 2;
  /** have a closer look at converge children */
  public static int D_CONVCHCLOSER = 3;
  /** check on random vektors */
  public static int D_RANDOMVEKTOR = 4;
  /** check on kdtree */
  public static int D_KDTREE = 5;
  /** follow iterations */
  public static int D_ITERCOUNT = 6;
  /** functions were maybe misused  */
  public static int D_METH_MISUSE = 80; 
  /** for current debug  */
  public static int D_CURR = 88;
  public static int D_GENERAL = 99;

  /** Flag: I'm debugging */
  public boolean m_CurrDebugFlag = true;

   /**
   * Returns a string describing this clusterer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Cluster data using the X-means algorithm.\n\n" 
      + "X-Means is K-Means extended by an Improve-Structure part In this "
      + "part of the algorithm the centers are attempted to be split in "
      + "its region. The decision between the children of each center and "
      + "itself is done comparing the BIC-values of the two structures.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Dan Pelleg and Andrew W. Moore");
    result.setValue(Field.TITLE, "X-means: Extending K-means with Efficient Estimation of the Number of Clusters");
    result.setValue(Field.BOOKTITLE, "Seventeenth International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.PAGES, "727-734");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann");
    
    return result;
  }

  /**
   * Returns default capabilities of the clusterer.
   *
   * @return      the capabilities of this clusterer
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
  }
 
  /**
   * Generates the X-Means clusterer. 
   *
   * @param data set of instances serving as training data 
   * @throws Exception if the clusterer has not been 
   * generated successfully
   */
  public void buildClusterer(Instances data) throws Exception {

    // can clusterer handle the data?
    getCapabilities().testWithFail(data);

    m_NumSplits = 0;
    m_NumSplitsDone = 0;
    m_NumSplitsStillDone = 0;

    // replace missing values
    m_ReplaceMissingFilter = new ReplaceMissingValues();
    m_ReplaceMissingFilter.setInputFormat(data);
    m_Instances = Filter.useFilter(data, m_ReplaceMissingFilter);
    
    // initialize random function
    Random random0 = new Random(m_Seed);

    // num of clusters to start with
    m_NumClusters =  m_MinNumClusters;

    // set distance function to default
    if (m_DistanceF == null) {
      m_DistanceF = new EuclideanDistance();
    }

    m_DistanceF.setInstances(m_Instances);
    checkInstances();

    if (m_DebugVektorsFile != null)
      initDebugVektorsInput();

    // make list of indexes for m_Instances
    int [] allInstList = new int[m_Instances.numInstances()]; 
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      allInstList[i] = i;
    }
    
    // set model used (just for convenience)
    m_Model = new Instances(m_Instances, 0);

    // produce the starting centers
    if (m_CenterInput != null) {
      // read centers from file
      m_ClusterCenters = new Instances(m_CenterInput);
      m_NumClusters = m_ClusterCenters.numInstances();
    }
    else
      // makes the first centers randomly
      m_ClusterCenters = makeCentersRandomly(random0,
					     m_Instances, m_NumClusters);
    PFD(D_FOLLOWSPLIT, "\n*** Starting centers ");
    for (int k = 0; k < m_ClusterCenters.numInstances(); k++) {
      PFD(D_FOLLOWSPLIT, "Center " + k + ": " + m_ClusterCenters.instance(k));
    }

    PrCentersFD(D_PRINTCENTERS);

    boolean finished = false;
    Instances children; 

    // builds up a KDTree
    if (m_KDTree != null) {
      m_KDTree.setInstances(m_Instances);
    }
  
    // loop counter of main loop
    m_IterationCount = 0;

    /**
     * "finished" does get true as soon as:
     * 1. number of clusters gets >= m_MaxClusters, 
     * 2. in the last round, none of the centers have been split
     * 
     * if number of clusters is already >= m_MaxClusters 
     * part 1 (= Improve-Params) is done at least once.
     */
    while (!finished &&
           !stopIteration(m_IterationCount, m_MaxIterations)) {
      
      /* ====================================================================
       * 1. Improve-Params                  
       *    conventional K-means
       */


      PFD(D_FOLLOWSPLIT, "\nBeginning of main loop - centers:");
      PrCentersFD(D_FOLLOWSPLIT);

      PFD(D_ITERCOUNT, "\n*** 1. Improve-Params " + m_IterationCount + 
	  ". time");
      m_IterationCount++;

      // prepare to converge
      boolean converged = false;

      // initialize assignments to -1
      m_ClusterAssignments = initAssignments(m_Instances.numInstances());
      // stores a list of indexes of instances belonging to each center
      int [][] instOfCent = new int[m_ClusterCenters.numInstances()][];

      // KMeans loop counter
      int kMeansIteration = 0;

      // converge in conventional K-means ----------------------------------
      PFD(D_FOLLOWSPLIT, "\nConverge in K-Means:");
      while (!converged && 
	     !stopKMeansIteration(kMeansIteration, m_MaxKMeans)) {
	
	kMeansIteration++;
	converged = true;
	
        // assign instances to centers -------------------------------------
        converged = assignToCenters(m_KDTree,
				    m_ClusterCenters, 
				    instOfCent,
				    allInstList, 
				    m_ClusterAssignments,
				    kMeansIteration);
	
	PFD(D_FOLLOWSPLIT, "\nMain loop - Assign - centers:");
	PrCentersFD(D_FOLLOWSPLIT);
	// compute new centers = centers of mass of points
        converged = recomputeCenters(m_ClusterCenters, // clusters
				     instOfCent,       // their instances
				     m_Model);         // model information
      PFD(D_FOLLOWSPLIT, "\nMain loop - Recompute - centers:");
      PrCentersFD(D_FOLLOWSPLIT);
      }
      PFD(D_FOLLOWSPLIT, "");
      PFD(D_FOLLOWSPLIT, "End of Part: 1. Improve-Params - conventional K-means");

      /** =====================================================================
       * 2. Improve-Structur
       */

      // BIC before split distortioning the centres
      m_Mle = distortion(instOfCent, m_ClusterCenters);
      m_Bic = calculateBIC(instOfCent, m_ClusterCenters, m_Mle);
      PFD(D_FOLLOWSPLIT, "m_Bic " + m_Bic);

      int currNumCent = m_ClusterCenters.numInstances();
      Instances splitCenters = new Instances(m_ClusterCenters, 
					     currNumCent * 2);
      
      // store BIC values of parent and children
      double [] pbic = new double [currNumCent];
      double [] cbic = new double [currNumCent];
            
      // split each center
      for (int i = 0; i < currNumCent 
	   // this could help to optimize the algorithm
	   //	     && currNumCent + numSplits <= m_MaxNumClusters
           ; 
	   i++) {
	
	PFD(D_FOLLOWSPLIT, "\nsplit center " + i +
		      " " + m_ClusterCenters.instance(i));
	Instance currCenter = m_ClusterCenters.instance(i);
	int [] currInstList = instOfCent[i];
	int currNumInst = instOfCent[i].length;
	
	// not enough instances; than continue with next
	if (currNumInst <= 2) {
	  pbic[i] = Double.MAX_VALUE;
	  cbic[i] = 0.0;
	  // add center itself as dummy
	  splitCenters.add(currCenter);
	  splitCenters.add(currCenter);
	  continue;
	}
	
	// split centers  ----------------------------------------------
	double variance = m_Mle[i] / (double)currNumInst;
	children = splitCenter(random0, currCenter, variance, m_Model);
	
	// initialize assignments to -1
	int[] oneCentAssignments = initAssignments(currNumInst);
	int[][] instOfChCent = new int [2][]; // todo maybe split didn't work
	
	// converge the children  --------------------------------------
	converged = false;
	int kMeansForChildrenIteration = 0;
	PFD(D_FOLLOWSPLIT, "\nConverge, K-Means for children: " + i);
	while (!converged && 
          !stopKMeansIteration(kMeansForChildrenIteration, 
			       m_MaxKMeansForChildren)) {
	  kMeansForChildrenIteration++;
	  
	  converged =
	    assignToCenters(children, instOfChCent,
			    currInstList, oneCentAssignments);

	  if (!converged) {       
	    recomputeCentersFast(children, instOfChCent, m_Model);
	  }
	} 

	// store new centers for later decision if they are taken
	splitCenters.add(children.instance(0));
	splitCenters.add(children.instance(1));

	PFD(D_FOLLOWSPLIT, "\nconverged cildren ");
	PFD(D_FOLLOWSPLIT, " " + children.instance(0));
	PFD(D_FOLLOWSPLIT, " " + children.instance(1));

	// compare parent and children model by their BIC-value
	pbic[i] = calculateBIC(currInstList, currCenter,  m_Mle[i], m_Model);
	double [] chMLE = distortion(instOfChCent, children);
	cbic[i] = calculateBIC(instOfChCent, children, chMLE);

      } // end of loop over clusters

      // decide which one to split and make new list of cluster centers
      Instances newClusterCenters = null;
      newClusterCenters = newCentersAfterSplit(pbic, cbic, m_CutOffFactor,
                                                 splitCenters);
      /**
       * Compare with before Improve-Structure
       */
      int newNumClusters = newClusterCenters.numInstances();
      if (newNumClusters != m_NumClusters) {
	
	PFD(D_FOLLOWSPLIT, "Compare with non-split");

	// initialize assignments to -1
	int [] newClusterAssignments = 
	  initAssignments(m_Instances.numInstances());
	
	// stores a list of indexes of instances belonging to each center
	int [][] newInstOfCent = new int[newClusterCenters.numInstances()][];
	
	// assign instances to centers -------------------------------------
	converged = assignToCenters(m_KDTree,
				    newClusterCenters, 
				    newInstOfCent,
				    allInstList, 
				    newClusterAssignments,
				    m_IterationCount);
	
	double [] newMle = distortion(newInstOfCent, newClusterCenters);
	double newBic = calculateBIC(newInstOfCent, newClusterCenters, newMle);
	PFD(D_FOLLOWSPLIT, "newBic " + newBic);
	if (newBic > m_Bic) {
          PFD(D_FOLLOWSPLIT, "*** decide for new clusters");
	  m_Bic = newBic;
	  m_ClusterCenters = newClusterCenters;
	  m_ClusterAssignments = newClusterAssignments;
	} else {
          PFD(D_FOLLOWSPLIT, "*** keep old clusters");
        }
      }

      newNumClusters = m_ClusterCenters.numInstances();
      // decide if finished: max num cluster reached 
      // or last centers where not split at all 
      if ((newNumClusters >= m_MaxNumClusters) 
	  || (newNumClusters == m_NumClusters)) {
	finished = true;
      }
      m_NumClusters = newNumClusters;
    }
  }

  /**
   * Checks for nominal attributes in the dataset.
   * Class attribute is ignored.
   * @param data the data to check
   * @return false if no nominal attributes are present
   */
  public boolean checkForNominalAttributes(Instances data) {

    int i = 0;
    while (i < data.numAttributes()) {
      if ((i != data.classIndex()) && data.attribute(i++).isNominal()) {
	return true;
      }
    }
    return false;
  }

  /**
   * Set array of int, used to store assignments, to -1.
   * @param ass integer array used for storing assignments
   * @return integer array used for storing assignments
   */
  private int [] initAssignments(int [] ass) {
    for (int i = 0; i < ass.length; i++)
      ass[i] = -1;
    return ass;
  }    
 
  /**
   * Creates and initializes integer array, used to store assignments.
   * @param numInstances length of array used for assignments
   * @return integer array used for storing assignments
   */
  private int [] initAssignments(int numInstances) {
    int [] ass = new int[numInstances];
    for (int i = 0; i < numInstances; i++)
      ass[i] = -1;
    return ass;
  }    
  
  /**
   * Creates and initializes boolean array.
   * @param len length of new array
   * @return the new array
   */
  boolean [] initBoolArray(int len) {
    boolean[] boolArray = new boolean [len];
    for (int i = 0; i < len; i++) {
      boolArray[i] = false;
    }
    return boolArray;
  }

  /**
   * Returns new center list.
   *
   * The following steps 1. and 2. both take care that the number of centers
   * does not exceed maxCenters.
   *
   * 1. Compare BIC values of parent and children and takes the one as
   * new centers which do win (= BIC-value is smaller).
   *
   * 2. If in 1. none of the children are chosen 
   *    && and cutoff factor is > 0
   * cutoff factor is taken as the percentage of "best" centers that are
   * still taken.
   * @param pbic array of parents BIC-values
   * @param cbic array of childrens BIC-values
   * @param cutoffFactor cutoff factor 
   * @param splitCenters all children 
   * @return the new centers
   */
  private Instances newCentersAfterSplit(double [] pbic, 
					 double [] cbic,
					 double cutoffFactor,
					 Instances splitCenters) {

    // store if split won
    boolean splitPerCutoff = false;
    boolean takeSomeAway = false;
    boolean [] splitWon = initBoolArray(m_ClusterCenters.numInstances());
    int numToSplit = 0;
    Instances newCenters = null;
    
    // how many would be split, because the children have a better bic value
    for (int i = 0; i < cbic.length; i++) {
      if (cbic[i] > pbic[i]) {
	// decide for splitting ----------------------------------------
	splitWon[i] = true; numToSplit++;
	PFD(D_FOLLOWSPLIT, "Center " + i + " decide for children");
      }
      else {
	// decide for parents and finished stays true  -----------------
	PFD(D_FOLLOWSPLIT, "Center " + i + " decide for parent");
      }
    }

    // no splits yet so split per cutoff factor
    if ((numToSplit == 0) && (cutoffFactor > 0)) {
      splitPerCutoff = true;
      
      // how many to split per cutoff factor
      numToSplit = (int) 
        ((double) m_ClusterCenters.numInstances() * m_CutOffFactor); 
    }

    // prepare indexes of values in ascending order  
    double [] diff = new double [m_NumClusters];
    for (int j = 0; j < diff.length; j++) {
      diff[j] = pbic[j] - cbic[j];
    }    
    int [] sortOrder = Utils.sort(diff);
    
    // check if maxNumClusters would be exceeded
    int possibleToSplit = m_MaxNumClusters - m_NumClusters; 

    if (possibleToSplit > numToSplit) {
      // still enough possible, do the whole amount
      possibleToSplit = numToSplit;
    }
    else
      takeSomeAway = true;

    // prepare for splitting the one that are supposed to be split
    if (splitPerCutoff) {
      for (int j = 0; (j < possibleToSplit) && (cbic[sortOrder[j]] > 0.0);
	   j++) {
	splitWon[sortOrder[j]] = true;
      }
      m_NumSplitsStillDone += possibleToSplit;
    } 
    else {
      // take some splits away if max number of clusters would be exceeded
      if (takeSomeAway) {
	int count = 0;
	int j = 0;
	for (;j < splitWon.length && count < possibleToSplit; j++){
	  if (splitWon[sortOrder[j]] == true) count++;
	}
	
	while (j < splitWon.length) {
	  splitWon[sortOrder[j]] = false;
	  j++;
	}
      }
    }
   
    // finally split
    if (possibleToSplit > 0) 
      newCenters = newCentersAfterSplit(splitWon, splitCenters);
    else
      newCenters = m_ClusterCenters;
    return newCenters;
  }

  /**
   * Returns new centers.
   * Depending on splitWon: if true takes children, if false
   * takes parent = current center.
   * @param splitWon array of boolean to indicate to take split or not
   * @param splitCenters list of splitted centers
   * @return the new centers
   */
  private Instances newCentersAfterSplit(boolean [] splitWon, 
                                         Instances splitCenters) {
    Instances newCenters = new Instances(splitCenters, 0);

    int sIndex = 0;
    for (int i = 0; i < splitWon.length; i++) {
      if (splitWon[i]) {
        m_NumSplitsDone++;
	newCenters.add(splitCenters.instance(sIndex++));
	newCenters.add(splitCenters.instance(sIndex++));
      } else {
	sIndex++; sIndex++;
        newCenters.add(m_ClusterCenters.instance(i));
      }
    }
    return newCenters;
  }

  /**
   * Controls that counter does not exceed max iteration value.
   * Special function for kmeans iterations.
   * @param iterationCount current value of counter
   * @param max maximum value for counter
   * @return true if iteration should be stopped
   */ 
  private boolean stopKMeansIteration(int iterationCount, int max) {
    boolean stopIterate = false;
    if (max >= 0) 
      stopIterate = (iterationCount >= max);
    if (stopIterate) 
      m_KMeansStopped++;
    return stopIterate;
  }

  /**
   * Checks if iterationCount has to be checked and if yes
   * (this means max is > 0) compares it with max
   * 
   * @param iterationCount the current iteration count
   * @param max the maximum number of iterations
   * @return true if maximum has been reached
   */ 
  private boolean stopIteration(int iterationCount, int max) {
    boolean stopIterate = false;
    if (max >= 0) 
      stopIterate = (iterationCount >= max);
    return stopIterate;
  }

  /**
   * Recompute the new centers. New cluster center is center of mass of its 
   * instances. Returns true if cluster stays the same.
   * @param centers the input and output centers
   * @param instOfCent the instances to the centers 
   * @param model data model information
   * @return true if converged.
   */
   private boolean recomputeCenters(Instances centers,          
				   int [][] instOfCent, 
				   Instances model) {
    boolean converged = true;
    
    for (int i = 0; i < centers.numInstances(); i++) {
      double val;
      for (int j = 0; j < model.numAttributes(); j++) {
	val = meanOrMode(m_Instances, instOfCent[i], j);

	for (int k = 0; k < instOfCent[i].length; k++)

	if (converged && m_ClusterCenters.instance(i).value(j) != val) 
	  converged = false;
	if (!converged)
	  m_ClusterCenters.instance(i).setValue(j, val);
      }
    }
    return converged;
  }

  /**
   * Recompute the new centers - 2nd version 
   * Same as recomputeCenters, but does not check if center stays the same.
   * 
   * @param centers the input center and output centers
   * @param instOfCentIndexes the indexes of the instances to the centers 
   * @param model data model information
   */
  private void recomputeCentersFast(Instances centers,          
				    int [][] instOfCentIndexes, 
				    Instances model   
				    ) {
    for (int i = 0; i < centers.numInstances(); i++) {
      double val;
      for (int j = 0; j < model.numAttributes(); j++) {
	val = meanOrMode(m_Instances, instOfCentIndexes[i], j);
	centers.instance(i).setValue(j, val);
      }
    }
  }

  /**
   * Computes Mean Or Mode of one attribute on a subset of m_Instances. 
   * The subset is defined by an index list.
   * @param instances all instances
   * @param instList the indexes of the instances the mean is computed from
   * @param attIndex the index of the attribute
   * @return mean value
   */
  private double meanOrMode(Instances instances, 
			    int [] instList, 
			    int attIndex) {
    double result, found;
    int [] counts;
    int numInst = instList.length;

    if (instances.attribute(attIndex).isNumeric()) {
      result = found = 0;
      for (int j = 0; j < numInst; j++) {
	Instance currInst = instances.instance(instList[j]);
	if (!currInst.isMissing(attIndex)) {
	  found += currInst.weight();
	  result += currInst.weight() * 
	    currInst.value(attIndex);
	}
      }
      if (Utils.eq(found, 0)) {
	return 0;
      } else {
	return result / found;
      }
    } else if (instances.attribute(attIndex).isNominal()) {
      counts = new int[instances.attribute(attIndex).numValues()];
      for (int j = 0; j < numInst; j++) {
	Instance currInst = instances.instance(instList[j]);
	if (!currInst.isMissing(attIndex)) {
	  counts[(int) currInst.value(attIndex)] += currInst.weight();
	}
      }
      return (double)Utils.maxIndex(counts);
    } else {
      return 0;
    }
  }

 
  /**
   * Assigns instances to centers.
   *
   * @param tree KDTree on all instances
   * @param centers all the input centers
   * @param instOfCent the instances to each center
   * @param allInstList list of all instances
   * @param assignments assignments of instances to centers
   * @param iterationCount the number of iteration 
   * @return true if converged
   * @throws Exception is something goes wrong
   */
  private boolean assignToCenters(KDTree tree,
                                  Instances centers, 
				  int [][] instOfCent, 
                                  int [] allInstList,
				  int [] assignments,
                                  int iterationCount) throws Exception {
    
    boolean converged = true;
    if (tree != null) {
      // using KDTree structure for assigning
      converged = assignToCenters(tree,
				  centers, 
				  instOfCent,
				  assignments,
				  iterationCount);
    } else {
      converged = assignToCenters(centers, 
				  instOfCent,
				  allInstList, 
				  assignments);
    }
    return converged;
  }

  /**
   * Assign instances to centers using KDtree.
   * First part of conventionell K-Means, returns true if new assignment
   * is the same as the last one.
   *
   * @param kdtree KDTree on all instances
   * @param centers all the input centers
   * @param instOfCent the instances to each center
   * @param assignments assignments of instances to centers
   * @param iterationCount the number of iteration 
   * @return true if converged
   * @throws Exception in case instances are not assigned to cluster
   */
  private boolean assignToCenters(KDTree kdtree,
				  Instances centers, 
				  int [][] instOfCent, 
				  int [] assignments,
                                  int iterationCount) throws Exception {

    int numCent = centers.numInstances();
    int numInst = m_Instances.numInstances(); 
    int [] oldAssignments = new int[numInst];
    
    // WARNING:  assignments is "input/output-parameter"
    // should not be null
    if (assignments == null) {
      assignments = new int[numInst];
      for (int i = 0; i < numInst; i++) {
	assignments[0] = -1;
      }
    }
    
    // WARNING:  instOfCent is "input/output-parameter"
    // should not be null
    if (instOfCent == null) {
      instOfCent = new int [numCent][];
    }
    
    // save old assignments
    for (int i = 0; i < assignments.length; i++) {
      oldAssignments[i] = assignments[i];
    }
    
    // use tree to get new assignments
    kdtree.centerInstances(centers, assignments,
			   Math.pow(.8, iterationCount));	
    boolean converged = true;
  
    // compare with previous assignment
    for (int i = 0; converged && (i < assignments.length); i++) {
      converged = (oldAssignments[i] == assignments[i]);
      if (assignments[i] == -1) 
	throw new Exception("Instance " + i + 
			    " has not been assigned to cluster.");
    }

    if (!converged) {
      int [] numInstOfCent = new int[numCent];
      for (int i = 0; i < numCent; i++)
	numInstOfCent[i] = 0;

      // count num of assignments per center
      for (int i = 0; i < numInst; i++)
	numInstOfCent[assignments[i]]++;
      
      // prepare instancelists per center
      for (int i = 0; i < numCent; i++){
	instOfCent[i] = new int[numInstOfCent[i]];
      }
      // write instance lists per center
      for (int i = 0; i < numCent; i++) {
	int index = -1;   
	for (int j = 0; j < numInstOfCent[i]; j++) {
	  index = nextAssignedOne(i, index, assignments);
	  instOfCent[i][j] = index;
	}
      }
    }
  
    return converged;
  }

  /**
   * Assign instances to centers.
   * Part of conventionell K-Means, returns true if new assignment
   * is the same as the last one.
   *
   * @param centers all the input centers
   * @param instOfCent the instances to each center
   * @param allInstList list of all indexes
   * @param assignments assignments of instances to centers
   * @return true if converged
   * @throws Exception if something goes wrong
   */
  private boolean assignToCenters(Instances centers, 
				  int [][] instOfCent,
				  int [] allInstList,
				  int [] assignments) 
    throws Exception {
    
    // todo: undecided situations
    boolean converged = true; // true if new assignment is the same 
                              // as the old one

    int numInst = allInstList.length; 
    int numCent = centers.numInstances();
    int [] numInstOfCent = new int [numCent];
    for (int i = 0; i < numCent; i++) numInstOfCent[i] = 0;

    // WARNING:  assignments is "input/output-parameter"
    // should not be null
    if (assignments == null) {
      assignments = new int[numInst];
      for (int i = 0; i < numInst; i++) {
	assignments[i] = -1;
      }
    }

    // WARNING: instOfCent is "input/output-parameter"
    // should not be null
    if (instOfCent == null) {
      instOfCent = new int [numCent][];
    }

    // set assignments
    for (int i = 0; i < numInst; i++) {
      Instance inst = m_Instances.instance(allInstList[i]);
      int newC = clusterProcessedInstance(inst, centers);
      
      if (converged && newC != assignments[i]) {
	converged = false;
      }

      numInstOfCent[newC]++; 
      if (!converged)
	assignments[i] = newC;
    }

    // the following is only done
    // if assignments are not the same, because too much effort
    if (!converged) {
      PFD(D_FOLLOWSPLIT, "assignToCenters -> it has NOT converged");
      for (int i = 0; i < numCent; i++) {
	instOfCent[i] = new int [numInstOfCent[i]];
      }

      for (int i = 0; i < numCent; i++) {
	int index = -1;   
	for (int j = 0; j < numInstOfCent[i]; j++) {
	  index = nextAssignedOne(i, index, assignments);
	  instOfCent[i][j] = allInstList[index];
	}
      }
    }
    else
      PFD(D_FOLLOWSPLIT, "assignToCenters -> it has converged");

    return converged;
  }

  /**
   * Searches along the assignment array for the next entry of the center 
   * in question.
   * @param cent index of the center 
   * @param lastIndex index to start searching
   * @param assignments assignments
   * @return index of the instance the center cent is assigned to
   */
  private int nextAssignedOne(int cent, int lastIndex, 
			      int [] assignments) {
    int len = assignments.length;
    int index = lastIndex + 1;
    while (index < len) {
      if (assignments[index] == cent) {
	return (index);
      }
      index++;
    }
    return (-1);
  }

  /**
   * Split centers in their region. Generates random vektor of 
   * length = variance and
   * adds and substractsx to cluster vektor to get two new clusters.
   * 
   * @param random random function
   * @param center the center that is split here
   * @param variance variance of the cluster 
   * @param model data model valid
   * @return a pair of new centers
   * @throws something in AlgVector goes wrong
   */
  private Instances splitCenter(Random random,
			        Instance center,
			        double variance,
			        Instances model) throws Exception {
    m_NumSplits++;
    AlgVector r = null;
    Instances children = new Instances(model, 2);

    if (m_DebugVektorsFile != null) {
      Instance nextVektor = getNextDebugVektorsInstance(model);
      PFD(D_RANDOMVEKTOR, "Random Vector from File " + nextVektor);
      r = new AlgVector(nextVektor);
    }
    else {
      // random vector of length = variance
      r = new AlgVector(model, random);
    }
    r.changeLength(Math.pow(variance, 0.5));
    PFD(D_RANDOMVEKTOR, "random vector *variance "+ r);
    
    // add random vector to center
    AlgVector c = new AlgVector(center);
    AlgVector c2 = (AlgVector) c.clone();
    c = c.add(r);
    Instance newCenter = c.getAsInstance(model, random);
    children.add(newCenter);
    PFD(D_FOLLOWSPLIT, "first child "+ newCenter);
    
    // substract random vector to center
    c2 = c2.substract(r);
    newCenter = c2.getAsInstance(model, random);
    children.add(newCenter);
    PFD(D_FOLLOWSPLIT, "second child "+ newCenter);

    return children;
  }

  /**
   * Split centers in their region.
   * (*Alternative version of splitCenter()*) 
   * 
   * @param random the random number generator
   * @param instances of the region
   * @param model 
   * @return a pair of new centers
   */
  private Instances splitCenters(Random random,
				 Instances instances,
				 Instances model) {
    Instances children = new Instances(model, 2);
    int instIndex = Math.abs(random.nextInt()) % 
      instances.numInstances();
    children.add(instances.instance(instIndex));
    int instIndex2 = instIndex;
    int count = 0;
    while ((instIndex2 == instIndex) && count < 10) {
      count++;
      instIndex2 = Math.abs(random.nextInt()) %
	instances.numInstances();
    }
    children.add(instances.instance(instIndex2));
    
    return children;
  }

  /**
   * Generates new centers randomly. Used for starting centers.
   *
   * @param random0 random number generator
   * @param model data model of the instances
   * @param numClusters number of clusters
   * @return new centers
   */
  private Instances makeCentersRandomly(Random random0,
					Instances model,
					int numClusters) {
    Instances clusterCenters = new Instances(model, numClusters);
    m_NumClusters = numClusters;

    // makes the new centers randomly
    for (int i = 0; i < numClusters; i++) {
      int instIndex = Math.abs(random0.nextInt()) % m_Instances.numInstances();
      clusterCenters.add(m_Instances.instance(instIndex));
    }
    return clusterCenters;
  }

  /**
   * Returns the BIC-value for the given center and instances.
   * @param instList The indices of the instances that belong to the center
   * @param center the center.
   * @param mle maximum likelihood
   * @param model the data model
   * @return the BIC value 
   */   
  private double calculateBIC(int [] instList, Instance center,
			      double mle, Instances model) {
    int [][] w1 = new int[1][instList.length];
    for (int i = 0; i < instList.length; i++) {
      w1[0][i] = instList[i];
    }
    double [] m = {mle};
    Instances w2 = new Instances(model, 1);
    w2.add(center);
    return calculateBIC(w1, w2, m);
    }
  
  /**
   * Calculates the BIC for the given set of centers and instances.
   * @param instOfCent The instances that belong to their respective centers
   * @param centers the centers
   * @param mle maximum likelihood
   * @return The BIC for the input.
   */
  private double calculateBIC(int [][] instOfCent, Instances centers,
			      double [] mle) {
    double loglike = 0.0;
    int numInstTotal = 0;
    int numCenters = centers.numInstances();
    int numDimensions = centers.numAttributes();
    int numParameters = (numCenters - 1) + //probabilities
      numCenters * numDimensions + //means
      numCenters; // variance params
    for (int i = 0; i < centers.numInstances(); i++) {
      loglike += logLikelihoodEstimate(instOfCent[i].length, centers.instance(i),
				       mle[i], centers.numInstances() * 2);
      numInstTotal += instOfCent[i].length;
    }
    /* diff
       thats how we did it
    loglike -= ((centers.numAttributes() + 1.0) * centers.numInstances() * 1)
      * Math.log(count);
      */
    loglike -= numInstTotal * Math.log(numInstTotal);
    //System.out.println ("numInstTotal " + numInstTotal +
    //                    "calculateBIC res " + loglike);
    loglike -= (numParameters / 2.0) * Math.log(numInstTotal);
    //System.out.println ("numParam " +
    //                     + numParameters +
    //			" calculateBIC res " + loglike);
    return loglike;
  }
  
  /**
   * Calculates the log-likelihood of the data for the given model, taken
   * at the maximum likelihood point.
   *
   * @param numInst number of instances that belong to the center
   * @param center the center
   * @param distortion distortion 
   * @param numCent number of centers 
   * @return the likelihood estimate
   */
  private double logLikelihoodEstimate(int numInst, 
				       Instance center, 
				       double distortion, 
				       int numCent) {
    // R(n) num of instances of the center -> numInst
    // K num of centers -> not used
    //
    //todo take the diff comments away
    double loglike = 0;
    /* if is new */
    if (numInst > 1) {
      /* diff variance is new */
      //
      // distortion = Sum over instances x of the center(x-center)
      // different to paper; sum should be squared
      //
      // (Sum of distances to center) / R(n) - 1.0
      // different to paper; should be R(n)-K
      double variance =  distortion / (numInst - 1.0); 
  
      //
      //  -R(n)/2 * log(pi*2)
      //
      double p1 = - (numInst / 2.0) * Math.log(Math.PI * 2.0);
      /* diff
	 thats how we had it
	 double p2 = -((ni * center.numAttributes()) / 2) * distortion;
      */
      //
      // -(R(n)*M)/2 * log(variance) 
      //
      double p2 = - (numInst * center.numAttributes()) / 2 * Math.log(variance);
      
      /* diff
	 thats how we had it, the difference is a bug in x-means
	 double p3 = - (numInst - numCent) / 2;
      */
      //
      // -(R(n)-1)/2
      //
      double p3 = - (numInst - 1.0) / 2.0;
      
      //
      // R(n)*log(R(n))
      //
      double p4 = numInst * Math.log(numInst);
      
      /* diff x-means doesn't have this part 
	 double p5 = - numInst * Math.log(numInstTotal);
      */
      
      /*
	loglike = -(ni / 2) * Math.log(Math.PI * 2) 
	- (ni * center.numAttributes()) / 2.0) * logdistortion
	- (ni - k) / 2.0 
	+ ni * Math.log(ni) 
	- ni * Math.log(r);
      */
      loglike = p1 + p2 + p3 + p4; // diff + p5;
      //the log(r) is something that can be reused.
      //as is the log(2 PI), these could provide extra speed up later on.
      //since distortion is so expensive to compute, I only do that once.
    }
    return loglike;
  }
  
  /**
   * Calculates the maximum likelihood estimate for the variance.
   * @param instOfCent indices of instances to each center
   * @param centers the centers
   * @return the list of distortions distortion.
   */
  private double [] distortion(int[][] instOfCent, Instances centers) 
    throws Exception {
    double [] distortion = new double [centers.numInstances()];
    for (int i = 0; i < centers.numInstances(); i++) {
      distortion[i] = 0.0;
      for (int j = 0; j < instOfCent[i].length; j++) {
	distortion[i] += m_DistanceF.distance(
                                 m_Instances.instance(instOfCent[i][j]), 
				 centers.instance(i));
      }
    }
    /* diff not done in x-means
    res *= 1.0 / (count - centers.numInstances());
    */
    return distortion;
  }
  
  /**
   * Clusters an instance.
   * @param instance the instance to assign a cluster to.
   * @param centers the centers to cluster the instance to.
   * @return a cluster index.
   */
  private int clusterProcessedInstance(Instance instance, Instances centers)
throws Exception{
    
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < centers.numInstances(); i++) {
      double dist = m_DistanceF.distance(instance, centers.instance(i));

      if (dist < minDist) {
	minDist = dist;     
	bestCluster = i;    
      }                     
    }
;                         
    return bestCluster;
  }
  
  /**
   * Clusters an instance that has been through the filters.
   *
   * @param instance the instance to assign a cluster to
   * @return a cluster number
   */
  private int clusterProcessedInstance(Instance instance) throws Exception {
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < m_NumClusters; i++) {
      double dist = m_DistanceF.distance(instance, m_ClusterCenters.instance(i));
      if (dist < minDist) {
	minDist = dist;
	bestCluster = i;
      }
    }
    return bestCluster;
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be assigned to a cluster
   * @return the number of the assigned cluster as an integer
   * if the class is enumerated, otherwise the predicted value
   * @throws if instance could not be classified
   * successfully
   */
  public int clusterInstance(Instance instance) throws Exception {
    m_ReplaceMissingFilter.input(instance);
    Instance inst = m_ReplaceMissingFilter.output();

    return clusterProcessedInstance(inst);
  }


  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   */
  public int numberOfClusters() {
    return m_NumClusters;
  }


  /**
   * Returns an enumeration describing the available options. 
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions() {
    Vector newVector = new Vector(4);

     newVector.addElement(new Option(
       "\tmaximum number of overall iterations\n" +
       "\t(default = 1).", 
       "I", 1, "-I <num>"));
     newVector.addElement(new Option(
       "\tmaximum number of iterations in the kMeans loop in\n" +
       "\tthe Improve-Parameter part \n"+
       "\t(default = 1000).", 
       "M", 1, "-M <num>"));
     newVector.addElement(new Option(
       "\tmaximum number of iterations in the kMeans loop\n" +
       "\tfor the splitted centroids in the Improve-Structure part \n"+
       "\t(default = 1000).",
       "J", 1, "-J <num>"));
     newVector.addElement(new Option(
       "\tminimum number of clusters\n" +
       "\t(default = 2).", 
       "L", 1, "-L <num>"));
     newVector.addElement(new Option(
       "\tmaximum number of clusters\n" +
       "\t(default = 4).",
       "H", 1, "-H <num>"));
     newVector.addElement(new Option(
       "\tdistance value for binary attributes\n" +
       "\t(default = 1.0).",
       "V", 1, "-V <value>"));
     newVector.addElement(new Option(
       "\tFull class name of KDTree class to use, followed\n" +
       "\tby scheme options.\n" +
       "\teg: \"weka.core.KDTree -P\"\n" +
       "\t(default = no KDTree class used).",
       "K", 1, "-K <KDTree class specification>"));
     newVector.addElement(new Option(
       "\tcutoff factor, takes the given percentage of the splitted \n" +
       "\tcentroids if none of the children win\n" +
       "\t(default = 0.0).",
       "C", 1, "-C <value>"));
     newVector.addElement(new Option(
       "\tFull class name of Distance function class to use, followed\n" +
       "\tby scheme options.\n" +
       "\teg: \"weka.core.MahalanobisDistance\"\n" +
       "\t(default = weka.core.EuclideanDistance).",
       "K", 1, "-K <distance function class specification>"));
     newVector.addElement(new Option(
       "\tfile to read starting centers from (ARFF format).",
       "N", 1, "-N <file name>"));
     newVector.addElement(new Option(
       "\tfile to write centers to (ARFF format).",
       "O", 1, "-O <file name>"));
     newVector.addElement(new Option(
       "\trandom number seed (default 10).",
       "S", 1, "-S <num>"));

     return  newVector.elements();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property 
   */
  public String minNumClustersTipText() {
    return "set minimum number of clusters";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property 
   */
  public String maxNumClustersTipText() {
    return "set maximum number of clusters";
  }

  /**
   * Sets the maximum number of iterations to perform.
   * @param i the number of iterations
   * @throws Exception if i is less than 1
   */
  public void setMaxIterations(int i) throws Exception {
    if (i < 0) 
      throw new Exception("Only positive values for iteration number" +
                           " allowed (Option I)."); 
    m_MaxIterations = i;
  }

  /**
   * Gets the maximum number of iterations.
   * @return the number of iterations
   */
  public int getMaxIterations() {
    return  m_MaxIterations;
  }

  /**
   * Set the maximum number of iterations to perform in KMeans
   * @param i the number of iterations
   */
  public void setMaxKMeans(int i) {
    m_MaxKMeans = i;
    m_MaxKMeansForChildren = i;
  }

  /**
   * Gets the maximum number of iterations in KMeans.
   * @return the number of iterations
   */
  public int getMaxKMeans() {
    return  m_MaxKMeans;
  }

  /**
   * Sets the maximum number of iterations KMeans that is performed 
   * on the child centers.
   * @param i the number of iterations
   */
  public void setMaxKMeansForChildren(int i) throws Exception {
    m_MaxKMeansForChildren = i;
  }

  /**
   * Gets the maximum number of iterations in KMeans.
   * @return the number of iterations
   */
  public int getMaxKMeansForChildren() {
    return  m_MaxKMeansForChildren;
  }

  /**
   * Sets a new cutoff factor.
   * @param i the new cutoff factor
   */
  public void setCutOffFactor(double i) throws Exception {
    m_CutOffFactor = i;
  }

  /**
   * Gets the cutoff factor.
   * @return the cutoff factor
   */
  public double getCutOffFactor() {
    return  m_CutOffFactor;
  }

  /**
   * Sets the minimum number of clusters to generate.
   *
   * @param n the minimum number of clusters to generate
   */
  public void setMinNumClusters(int n) {
    if (n <= m_MaxNumClusters) {
      m_MinNumClusters = n;
    }
  }

  /**
   * Sets the maximum number of clusters to generate.
   * @param n the maximum number of clusters to generate
   */
  public void setMaxNumClusters(int n) {
    if (n >= m_MinNumClusters) {
      m_MaxNumClusters = n;
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binValueTipText() {
    return "Set the value that represents true in the new attributes.";
  }
  /**
   * Gets value that represents true in a new numeric attribute.
   * (False is always represented by 0.0.)
   * @return the value that represents true in a new numeric attribute
   */
  public double getBinValue() {
    return m_BinValue;
  }

  /**
   * Sets the distance e value between true and false of binary attributes 
   * and  "same" and "different" of nominal attributes    
   * @param double value
   */
  public void setBinValue(double value) {
    m_BinValue = value;
  }

  /**
   * gets the "binary" distance value 
   * @param distanceF the distance function with all options set
   */
  public void setDistanceF(EuclideanDistance distanceF) {
    m_DistanceF = distanceF;
  }

  /**
   * Gets the distance function.
   * @return the distance function
   */
  public EuclideanDistance getDistanceF() {
    return m_DistanceF;
  }

  /**
   * Gets the distance function specification string, which contains the 
   * class name of the distance function class and any options to it
   *
   * @return the distance function specification string
   */
  protected String getDistanceFSpec() {
    
    EuclideanDistance d = getDistanceF();
    if (d instanceof OptionHandler) {
      return d.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler) d).getOptions());
    }
    return d.getClass().getName();
  }
  /**
   * Sets a file name for a file that has the random vektors stored.
   * Just used for debugging reasons.
   * @param fileName file name for the file to read the random vektors from
   */
  public void setDebugVektorsFile(String fileName) {
    m_DebugVektorsFile = fileName;
  }

  /**
   * Initialises the debug vektor input.
   */
  public void initDebugVektorsInput() throws Exception {
    m_DebugVektorsInput = 
      new BufferedReader(new FileReader(m_DebugVektorsFile));
    m_DebugVektors = new Instances(m_DebugVektorsInput);
    m_DebugVektorsIndex = 0;
  }

  /**
   * Read an instance from debug vektors file.
   * @param model the data model for the instance
   */
  public Instance getNextDebugVektorsInstance(Instances model) 
    throws Exception {
    if (m_DebugVektorsIndex >= m_DebugVektors.numInstances())
      throw new Exception("no more prefabricated Vektors");
    Instance nex = m_DebugVektors.instance(m_DebugVektorsIndex);
    nex.setDataset(model);
    m_DebugVektorsIndex++;
    return nex;
  }

  /**
   * Sets the name of the file to read the list of centers from.
   *
   * @param fileName file name of file to read centers from
   */
  public void setInputCenterFile(String fileName) {
    m_InputCenterFile = fileName;
  }
    
  /**
   * Sets the name of the file to write the list of centers to. 
   *
   * @param fileName file to write centers to
   */
  public void setOutputCenterFile(String fileName) {
    m_OutputCenterFile = fileName;
  }
    
  /**
   * Gets the name of the file to read the list of centers from.
   *
   * @return filename of the file to read the centers from
   */
  public String getInputCenterFile() {
    return m_InputCenterFile;
  }

  /**
   * Gets the name of the file to write the list of centers to. 
   * @return filename of the file to write centers to
   */
  public String getOutputCenterFile() {
    return m_OutputCenterFile;
  }
    
  /**
   * Sets the KDTree class.
   * @param k a KDTree object with all options set
   */
  public void setKDTree(KDTree k) {
    m_KDTree = k;
  }

  /**
   * Gets the KDTree class.
   * @return flag if KDTrees are used
   */
  public KDTree getKDTree() {
    return m_KDTree;
  }

  /**
   * Gets the KDTree specification string, which contains the class name of
   * the KDTree class and any options to the KDTree
   *
   * @return the KDTree string.
   */
  protected String getKDTreeSpec() {
    
    KDTree c = getKDTree();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
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
   * Gets the minimum number of clusters to generate.
   * @return the minimum number of clusters to generate
   */
  public int getMinNumClusters() {
    return m_MinNumClusters;
  }
    
  /**
   * Gets the maximum number of clusters to generate.
   * @return the maximum number of clusters to generate
   */
  public int getMaxNumClusters() {
    return m_MaxNumClusters;
  }

  /**
   * Returns the tip text for this property.
   * @return tip text for this property 
   */
  public String seedTipText() {
    return "random number seed";
  }

  /**
   * Sets the random number seed.
   * @param s the seed
   */
  public void setSeed(int s) {
    m_Seed = s;
  }


  /**
   * Gets the random number seed.
   * @return the seed
   */
  public int getSeed() {
    return  m_Seed;
  }

  /**
   * Checks the instances.
   * No checks in this KDTree but it calls the check of the distance function.
   */
  private void checkInstances () throws Exception {
    
   // m_DistanceF.checkInstances();
  }
  
  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -I &lt;num&gt;
   *  maximum number of overall iterations
   *  (default = 1).</pre>
   * 
   * <pre> -M &lt;num&gt;
   *  maximum number of iterations in the kMeans loop in
   *  the Improve-Parameter part 
   *  (default = 1000).</pre>
   * 
   * <pre> -J &lt;num&gt;
   *  maximum number of iterations in the kMeans loop
   *  for the splitted centroids in the Improve-Structure part 
   *  (default = 1000).</pre>
   * 
   * <pre> -L &lt;num&gt;
   *  minimum number of clusters
   *  (default = 2).</pre>
   * 
   * <pre> -H &lt;num&gt;
   *  maximum number of clusters
   *  (default = 4).</pre>
   * 
   * <pre> -V &lt;value&gt;
   *  distance value for binary attributes
   *  (default = 1.0).</pre>
   * 
   * <pre> -K &lt;KDTree class specification&gt;
   *  Full class name of KDTree class to use, followed
   *  by scheme options.
   *  eg: "weka.core.KDTree -P"
   *  (default = no KDTree class used).</pre>
   * 
   * <pre> -C &lt;value&gt;
   *  cutoff factor, takes the given percentage of the splitted 
   *  centroids if none of the children win
   *  (default = 0.0).</pre>
   * 
   * <pre> -K &lt;distance function class specification&gt;
   *  Full class name of Distance function class to use, followed
   *  by scheme options.
   *  eg: "weka.core.MahalanobisDistance"
   *  (default = weka.core.EuclideanDistance).</pre>
   * 
   * <pre> -N &lt;file name&gt;
   *  file to read starting centers from (ARFF format).</pre>
   * 
   * <pre> -O &lt;file name&gt;
   *  file to write centers to (ARFF format).</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  random number seed (default 10).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options)
    throws Exception {

    String optionString = Utils.getOption('I', options);
    if (optionString.length() != 0) {
      setMaxIterations(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMaxKMeans(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('J', options);
    if (optionString.length() != 0) {
      setMaxKMeansForChildren(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('L', options);
    if (optionString.length() != 0) {
      setMinNumClusters(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('H', options);
    if (optionString.length() != 0) {
      setMaxNumClusters(Integer.parseInt(optionString));
    }
    optionString = Utils.getOption('B', options);
    if (optionString.length() != 0) {
      setBinValue(Double.parseDouble(optionString));
    }

    String funcString = Utils.getOption('K', options);
    if (funcString.length() != 0) {
      String [] funcSpec = Utils.splitOptions(funcString);
      if (funcSpec.length == 0) {
	throw new Exception("Invalid function specification string");
      }
      String funcName = funcSpec[0];
      funcSpec[0] = "";
      setKDTree((KDTree) Utils.forName(KDTree.class, funcName, funcSpec));
    }

   optionString = Utils.getOption('C', options);
    if (optionString.length() != 0) {
      setCutOffFactor(Double.parseDouble(optionString));
    }
    funcString = Utils.getOption('D', options);
    if (funcString.length() != 0) {
      String [] funcSpec = Utils.splitOptions(funcString);
      if (funcSpec.length == 0) {
	throw new Exception("Invalid function specification string");
      }
      String funcName = funcSpec[0];
      funcSpec[0] = "";
      setDistanceF((EuclideanDistance) Utils.forName(EuclideanDistance.class,
						    funcName, funcSpec));
    } 

    optionString  = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setInputCenterFile(optionString);
      m_CenterInput = 
	new BufferedReader(new FileReader(optionString));
    }

    optionString  = Utils.getOption('O', options);
    if (optionString.length() != 0) {
      setOutputCenterFile(optionString);
      m_CenterOutput = new PrintWriter(new FileOutputStream(optionString));
    }

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }


    optionString = Utils.getOption('U', options);
    int debugLevel = 0;
    if (optionString.length() != 0) {
      try {
	debugLevel = Integer.parseInt(optionString);
      } catch (NumberFormatException e) {
	throw new Exception(optionString +
                            "is an illegal value for option D"); 
      }
    }
    setDebugLevel(debugLevel);

    optionString  = Utils.getOption('Y', options);
    if (optionString.length() != 0) {
      setDebugVektorsFile(optionString);
    }
  }
  
  /**
   * Gets the current settings of SimpleKMeans.
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[27];
    int current = 0;
    
    options[current++] = "-I";
    options[current++] = "" + getMaxIterations();
    options[current++] = "-M";
    options[current++] = "" + getMaxKMeans();
    options[current++] = "-J";
    options[current++] = "" + getMaxKMeansForChildren();
    options[current++] = "-L";
    options[current++] = "" + getMinNumClusters();
    options[current++] = "-H";
    options[current++] = "" + getMaxNumClusters();
    options[current++] = "-B";
    options[current++] = "" + getBinValue();
    if (getKDTree() != null) {
      options[current++] = "-K";
      options[current++] = "" + getKDTreeSpec();
    }
    options[current++] = "-C";
    options[current++] = "" + getCutOffFactor();
    if (getDistanceF() != null) {
      options[current++] = "-D";
      options[current++] = "" + getDistanceFSpec();
    }
    
    if (getInputCenterFile() != null) {
      options[current++] = "-N";
      options[current++] = "" + getInputCenterFile();
    }
    if (getOutputCenterFile() != null) {
      options[current++] = "-O";
      options[current++] = "" + getOutputCenterFile();
    }
    options[current++] = "-S";
    options[current++] = "" + getSeed();
    int dL = getDebugLevel();
    if (dL > 0) {
      options[current++] = "-U";
      options[current++] = "" + getDebugLevel();
    }
    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }

  /**
   * Return a string describing this clusterer.
   * @return a description of the clusterer as a string
   */
  public String toString() {
    StringBuffer temp = new StringBuffer();

    temp.append("\nkMeans\n======\n");

    temp.append("Requested iterations            : " + m_MaxIterations + "\n");
    temp.append("Iterations performed            : " + m_IterationCount+ "\n");
    temp.append("kMeans did not converge\n");
    temp.append("  but was stopped by max-loops " 
		+ m_KMeansStopped + " times (max kMeans-iter) = \n\n");
    temp.append("Splits prepared                 : " + m_NumSplits + "\n");
    temp.append("Splits performed                : " + m_NumSplitsDone + "\n");
    temp.append("Cutoff factor                   : " + m_CutOffFactor + "\n");
    double perc;
    if (m_NumSplitsDone > 0)
      perc = (((double)m_NumSplitsStillDone)/((double) m_NumSplitsDone))
             * 100.0;
    else
      perc = 0.0;
    temp.append("Percentage of splits accepted \n" +
                "by cutoff factor                : " 
		+ Utils.doubleToString(perc,2) + " %\n");
    temp.append("------\n");

    temp.append("Cutoff factor                   : " + m_CutOffFactor + "\n");
    temp.append("------\n");
    temp.append("\nCluster centers                 : " + m_NumClusters + " centers\n");
    for (int i = 0; i < m_NumClusters; i++) {
      temp.append("\nCluster "+i+"\n           ");
      for (int j = 0; j < m_ClusterCenters.numAttributes(); j++) {
	if (m_ClusterCenters.attribute(j).isNominal()) {
	  temp.append(" "+m_ClusterCenters.attribute(j).
		      value((int)m_ClusterCenters.instance(i).value(j)));
	} else {
	  temp.append(" "+m_ClusterCenters.instance(i).value(j));
	}
      }
    }
    if (m_Mle != null)
      temp.append("\n\nDistortion: " + 
		  Utils.doubleToString(Utils.sum(m_Mle),6) + "\n");
    temp.append("BIC-Value : " + Utils.doubleToString(m_Bic,6) + "\n");
    return temp.toString();
  }

  /**
   * Print centers for debug.
   * @param debugLevel level that gives according messages
   * @return true if debug level is set
   */
  private void PrCentersFD(int debugLevel) {
    if (debugLevel == m_DebugLevel) {
      for (int i = 0; i < m_ClusterCenters.numInstances(); i++) {
	System.out.println(m_ClusterCenters.instance(i));
      }
    }
  }

  /**
   * Tests on debug status.
   * @param debugLevel level that gives according messages
   * @return true if debug level is set
   */
  private boolean TFD(int debugLevel) {
    return (debugLevel == m_DebugLevel);
  }

  /**
   * Does debug printouts.
   * @param debugLevel level that gives according messages
   * @param output string that is printed
   */
  private void PFD(int debugLevel, String output) {
    if (debugLevel == m_DebugLevel)
      System.out.println(output);
  }
  /**
   * Does debug printouts.
   * @param debugLevel level that gives according messages
   * @param output string that is printed
   */
  private void PFD_CURR(String output) {
    if (m_CurrDebugFlag)
      System.out.println(output);
  }

  /**
   * Main method for testing this class.
   * @param argv should contain options 
   */
  public static void main(String[] argv) {
    try {
      System.out.println(ClusterEvaluation.
			 evaluateClusterer(new XMeans(), argv));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
