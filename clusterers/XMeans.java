/*
 *    XMeans.java
 *    Copyright (C) 2000 Mark Hall, Malcolm Ware, Gabi Schmidberger
 *
 */
package weka.clusterers;
import  weka.clusterers.KDTree;
import  weka.clusterers.AlgVector;

import  java.io.*;
import  java.util.*;
import  weka.core.Instances;
import  weka.core.Instance;
import  weka.core.Attribute;
import  weka.core.Utils;
import  weka.core.Option;
import  weka.core.OptionHandler;

import  weka.filters.Filter;
import  weka.filters.unsupervised.attribute.ReplaceMissingValues;
import  weka.clusterers.KDTree;

/**
 * XMeans clustering class.
 *
 * X-Means is K-Means extended by an Improve-Structure part In this part of 
 * the algorithm the centers are attempted to be split in its region. 
 * The decision between the children of 
 * each center and itself is done comparing the BIC-values of 
 * the two structures.
 * See also D. Pelleg and A. Moore's paper 'X-means: Extending 
 * K-means with Efficient Estimation of the Number of Clusters'. <p>
 *
 * Valid options are:<p>
 *
 * -I <max iterations> <br>
 * Terminate after this many iterations in all the K-means iterations,<br>
 * This means in improve params and in improve structure. <p>
 *
 * -L <minimal number of clusters> <br>
 * Specify the number of clusters to start with. <p>
 *
 * -H <maximal number of clusters> <br>
 * Specify the maximal number of clusters. <p>
 *
 * -K <br>
 * Flag to use KDTrees. <p>
 *
 * -P <br>
 * Pruning flag. <p>
 *
 * -B <minimal-box-relative-width> <br>
 * todo
 *
 * -E <maximal-leaf-number> <br>
 * todo
 * 
 * -C <cutoff factor> <br>
 * If none of the children are better, percentage of the best splits<br>
 * to be taken.<p>
 * 
 * -N <file name> <br>
 * Input starting cluster centers from file (ARFF-format). <p>
 *
 * -O <file name> <br>
 * Output cluster centers to file (ARFF-format). <p>
 *
 * -S <seed> <br>
 * Specify random number seed. <p>
 *
 * -D <debuglevel> <br>
 * Set debuglevel. <p>
 *
 * -Y <file name> <br>
 * Used for debugging: Input random vektors from file. <p>
 *
 * major TODOS
 * make BIC-Score replaceable by other scores
 *
 * @author Gabi Schmidberger <gabi@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Malcolm Ware <mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 * @see Clusterer
 * @see OptionHandler
 */
public class XMeans extends Clusterer implements OptionHandler {

  private AlgVector algv; // TODO just a trick

  /* training instances */
  private Instances m_Instances = null;

  /* model information, should increase readability */
  private Instances m_Model = null;
  
  /* replace missing values in training instances */
  private ReplaceMissingValues m_ReplaceMissingFilter;

  /* BIC-Score of the current model */
  double m_Bic = Double.MIN_VALUE;

  /* Distortion  */
  double [] m_Mle = null;

  /* maximum overall iterations */
  private int m_MaxIterations = 1;

  /** 
   * maximum iterations to perform  kmeans part 
   * if negative, iterations are not checked
   */
  private int m_MaxKMeans = 1000;

  /* see above, but for kMeans of splitted clusters
   */
  private int m_MaxKMeansForChildren = 1000;

  /* The actual number of clusters */
  private int m_NumClusters = 2;

  /* min number of clusters to generate */
  private int m_MinNumClusters = 2;

  /* max number of clusters to generate */
  private int m_MaxNumClusters = 4;

  /* cluster centers */
  private Instances m_ClusterCenters;

  /* file name of the output file for the cluster centers */
  String m_InputCenterFile = null;

  /*--> DebugVektors - USED FOR DEBUGGING */
  /* input file for the random vektors --> USED FOR DEBUGGING */
  Reader m_DebugVektorsInput = null;
  int m_DebugVektorsIndex = 0;
  Instances m_DebugVektors = null;

  /* file name of the input file for the random vektors */
  String m_DebugVektorsFile = null;

  /* input file for the cluster centers */
  Reader m_CenterInput = null;
    
  /* file name of the output file for the cluster centers */
  String m_OutputCenterFile = null;
  
  /* output file for the cluster centers */
  PrintWriter m_CenterOutput = null;
    
  /**
   * temporary variable holding cluster assignments while iterating
   */
  private int [] m_ClusterAssignments;

  /* minimal relative width of a kdtree rectangle */ 
  double  m_MinBoxRelWidth = 1.0E-2; // TODO set with option

  /* maximal number of leaves in an KDTree */
  int m_MaxLeafNumber = 40; // TODO set with option

  /* cutoff factor - percentage of splits done in Improve-Structure part
     only relevant, if all children lost */ 
  double m_CutOffFactor = 0.5;

  /**
   * random seed
   */
  private int m_Seed = 10;

  /**
   * Ranges of the universe of data, lowest value, highest value and width
   */
  double [][] m_Ranges;

  /**
   * Index in ranges for LOW and HIGH and WIDTH
   */
  public static int R_LOW = 0;
  public static int R_HIGH = 1;
  public static int R_WIDTH = 2;

  /**
   * pruning flag.
   */
  private boolean m_Prune = false;

  /**
   * flag that says KDTrees are used
   */
  private boolean m_KDTree = false;

  /* counts iterations done in main loop */
  private int m_IterationCount = 0;

  /* counter to say how often kMeans was stopped by loop counter */
  private int m_KMeansStopped = 0;

  /* Number of splits prepared */
  private int m_NumSplits = 0;

  /* Number of splits accepted (including cutoff factor decisions) */
  private int m_NumSplitsDone = 0;

  /* Number of splits accepted just because of cutoff factor */
  private int m_NumSplitsStillDone = 0;

  /**
   * level of debug output, 0 is no output.
   */
  private int m_DebugLevel = 0;
  public static int D_PRINTCENTERS = 1;
  // follows the splitting of the centers
  public static int D_FOLLOWSPLIT = 2;
  // have a closer look at converge children
  public static int D_CONVCHCLOSER = 3;
  // check on random vektors
  public static int D_RANDOMVEKTOR = 4;
  // check on kdtree
  public static int D_KDTREE = 5;
  // follow iterations
  public static int D_ITERCOUNT = 6;
  // functions were maybe misused 
  public static int D_METH_MISUSE = 80; 
  // for current debug 
  public static int D_CURR = 88;
  public static int D_GENERAL = 99;

  // Flag: I'm debugging
  public boolean m_CurrDebugFlag = true;

   /**
   * Returns a string describing this clusterer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the X-means algorithm" +
      ", as described in D. Pelleg and A. Moore's paper 'X-means: Extending" +
      " K-means with Efficient Estimation of the Number of Clusters'.";
  }
  
  /**
   * Function should be in the Instances class!!
   *
   * Initializes the minimum and maximum values
   * based on all instances.
   *
   * @param instList list of indexes 
   */
  public static double [][] initializeRanges(Instances instances,
                                             int[] instList) {
    int numAtt = instances.numAttributes();
    
    double [][] ranges = new double [numAtt][3];
    
    // initialize ranges using the first instance
    updateRangesFirst(instances.instance(instList[0]), numAtt,
		      ranges);
    // update ranges, starting from the second
    for (int i = 1; i < instList.length; i++) {
      updateRanges(instances.instance(instList[i]), numAtt,
		   ranges);
    }
    return ranges;
  }

  /**
   * Function should be in the Instances class!!
   *
   * Prints a range.
   *
   * @param ranges the ranges to print
   */
  public static void printRanges(Instances model,
				 double[][] ranges) {

    System.out.println("printRanges");
    for (int j = 0; j < model.numAttributes(); j++) {
      System.out.print("Attribute "+ j +" LOW: " + ranges[j][R_LOW]);
      System.out.print(" HIGH: " + ranges[j][R_HIGH]);
      System.out.print(" WIDTH: " + ranges[j][R_WIDTH]);
      System.out.println(" ");
    }
  }

  /**
   * Function should be in the Instances class!!
   *
   * Used to initialize the ranges. For this the values
   * of the first instance is used to save time.
   * Sets low and high to the values of the first instance and
   * width to zero.
   *
   * @param instance the new instance
   * @param numAtt number of attributes in the model
   */
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
   */
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
  }
 
  /**
   * Generates the X-Means clusterer. 
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the clusterer has not been 
   * generated successfully
   */
  public void buildClusterer(Instances data) throws Exception {

    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    if (checkForNominalAttributes(data)) {
      throw  new Exception("Can't handle nominal attributes!");
    }

    Random random0 = new Random(m_Seed);
    m_NumClusters =  m_MinNumClusters;


    // replace missing values
    m_ReplaceMissingFilter = new ReplaceMissingValues();
    m_ReplaceMissingFilter.setInputFormat(data);
    m_Instances = Filter.useFilter(data, m_ReplaceMissingFilter);

    // 
    if (m_DebugVektorsFile != null)
      initDebugVektorsInput();

    // make list of indexes for m_Instances
    int [] allInstList = new int[m_Instances.numInstances()]; 
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      allInstList[i] = i;
    }
    // prepare the min and max value    
    m_Ranges = initializeRanges(m_Instances, allInstList);

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
    KDTree tree = null;
    if (m_KDTree) {
      tree = new KDTree(m_Ranges, m_Instances, allInstList,
			m_MinBoxRelWidth, m_MaxLeafNumber);
      //PFD(D_KDTREE, tree.toString());
    }
  
    // loop counter of main loop
    m_IterationCount = 0;

    // is true for the beginning of first loop, 
    // might false because ass. is already done in the following loops
    boolean firstAssignment = true;

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
        converged = assignToCenters(tree,
				    m_ClusterCenters, 
				    instOfCent,
				    allInstList, 
				    m_ClusterAssignments,
				    kMeansIteration);
	
        /*gabifor (int l = 0; l < instOfCent.length; l++) {
	  System.out.println(" " + instOfCent[l].length + "+++");
          for (int m = 0; m < instOfCent[l].length; m++) {
	  OOPS(" " + instOfCent[l][m]);
	  }
	  }*/
	PFD(D_FOLLOWSPLIT, "\nMain loop - Assign - centers:");
	PrCentersFD(D_FOLLOWSPLIT);
	System.out.println(" ");
	// compute new centers = centers of mass of points
        converged = recomputeCenters(m_ClusterCenters, // clusters
				     instOfCent,       // their instances
				     m_Model);         // model information
      PFD(D_FOLLOWSPLIT, "\nMain loop - Recompute - centers:");
      PrCentersFD(D_FOLLOWSPLIT);
      }
      PFD(D_FOLLOWSPLIT, "");
      PFD(D_FOLLOWSPLIT, "End of Part: 1. Improve-Params - conventional K-means");

      /*
      for (int m = 0; m < instOfCent.length; m++) {
        System.out.println("Center "+m+" : "+ m_ClusterCenters.instance(m));
        for (int n = 0; n < instOfCent[m].length; n++) {
	  System.out.print(instOfCent[m][n]+", ");
	}
	PFD(D_FOLLOWSPLIT, "");
	}*/
    
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
	double currMLE = m_Mle[i];
	
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
	converged = assignToCenters(null,
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
   * Checks for nominal attributes in the dataset
   * @param data 
   * @return false if no nominal attributes are present
   */
  public boolean checkForNominalAttributes(Instances data) {

    int i = 0;
    while (i < data.numAttributes()) {
      if (data.attribute(i++).isNominal()) {
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
   * (this means m_MaxIterations is > 0) compares it with
   * m_MaxIteration
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
   * @param instancesOfCent the instances to the centers 
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
   * Assign instances to center. Center to be assign to
   * is decided by the distance function.
   *
   * @param ranges min's and max's of attributes
   * @param centers all the input centers
   * @param centList the list of centers to work with
   * @param instances all instances of the region
   * @param instList the list of instances to work with
   * @param assignments index list of last assignments
   */
  public static void assignSubToCenters(double [][] ranges,
					Instances centers, 
					int [] centList, 
					Instances instances,
					int [] instList, 
					int [] assignments) {
    //todo: undecided situations
    int numInst = instList.length; 
    int numCent = centList.length;

    // WARNING:  assignments is "input/output-parameter"
    // should not be null
    if (assignments == null) {
      assignments = new int[instances.numInstances()];
      for (int i = 0; i < assignments.length; i++) {
	assignments[i] = -1;
      }
    }

    // set assignments
    for (int i = 0; i < numInst; i++) {
      Instance inst = instances.instance(instList[i]);
      if (instList[i] == 664) System.out.println("664***");
      int newC = assignInstanceToCenter(ranges, inst, centers, centList);
      // int newC = clusterProcessedInstance(inst, centers);
      assignments[instList[i]] = newC;
    }
  }
 
  /**
   * Assigns instances to centers.
   *
   * @param tree KDTree on all instances
   * @param centers all the input centers
   * @param instOfCent the instances to each center
   * @param allInstList TODO
   * @param assignments assignments of instances to centers
   * @param iterationCount the number of iteration 
   * @return true if converged
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
   * @param tree KDTree on all instances
   * @param centers all the input centers
   * @param instOfCent the instances to each center
   * @param assignments assignments of instances to centers
   * @param iterationCount the number of iteration 
   * @return true if converged
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
      OOPS(D_METH_MISUSE, "assignment was null");
      assignments = new int[numInst];
      for (int i = 0; i < numInst; i++) {
	assignments[0] = -1;
      }
    }

    // WARNING:  instOfCent is "input/output-parameter"
    // should not be null
    if (instOfCent == null) {
      OOPS(D_METH_MISUSE, "inst of cent was null");
      instOfCent = new int [numCent][];
    }

    // save old assignments
    for (int i = 0; i < assignments.length; i++) {
      oldAssignments[i] = assignments[i];
    }

    // use tree to get new assignments
    kdtree.centerInstances(centers, assignments,
			   Math.pow(.8, iterationCount), m_Prune);	
    boolean converged = true;

    //PFD_CURR("assignments");
    //for (int d = 0; d < assignments.length; d++) {
    //  System.out.print(" "+assignments[d]+", ");
    //}
    //PFD(D_CURR, " ");
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
      /*      for (int i = 0; i < numInst; i++) {
	  int center = assignments[i];
	  instOfCent[center][numInstOfCent[center]++] = i;
	  }*/
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
   * @param allInstList TODO
   * @param assignments assignments of instances to centers
   * @return true if converged
   */
  private boolean assignToCenters(Instances centers, 
				  int [][] instOfCent,
				  int [] allInstList,
				  int [] assignments) {
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
      OOPS(D_METH_MISUSE, "assignment was null");
      assignments = new int[numInst];
      for (int i = 0; i < numInst; i++) {
	assignments[i] = -1;
      }
    }

    // WARNING: instOfCent is "input/output-parameter"
    // should not be null
    if (instOfCent == null) {
      OOPS(D_METH_MISUSE, "inst of cent was null");
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
   * @param lasIndex index to start searching
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
   * @exception something in AlgVector goes wrong
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
      //OOPS("before split variance "+ variance);
      //OOPS("center to be split "+ center);
      
      // random vector of length = variance
      r = new AlgVector(model, random);
    }
    r.changeLength(Math.pow(variance, 0.5));
    //OOPS(D_FOLLOWSPLIT, "variance " + variance + 
    //                    " sqrt-variance " + Math.pow(variance, 0.5));
    PFD(D_RANDOMVEKTOR, "random vector *variance "+ r);
    
    // add random vector to center
    AlgVector c = new AlgVector(center);
    c.add(r);
    Instance newCenter = c.getAsInstance(model);
    children.add(newCenter);
    PFD(D_FOLLOWSPLIT, "first child "+ newCenter);
    
    // substract random vector to center
    c = new AlgVector(center);
    c.substract(r);
    newCenter = c.getAsInstance(model);
    children.add(newCenter);
    PFD(D_FOLLOWSPLIT, "second child "+ newCenter);

    // todo CATEGORICAL PART 
    return children;
  }

  /**
   * Split centers in their region.
   * (*Alternative version of splitCenter()*) 
   * @param instances of the region
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
      //OOPS("distortion " + distortion);
      //OOPS("variance " + variance);
      //OOPS("p1 " + p1);
      //OOPS("p2 " + p2);
      //OOPS("p3 " + p3);
      //OOPS("p4 " + p4);
      //OOPS(p1 + " " + p2 + " " + p3 + " " + p4 + " " + p5 + " " +
      //	       distortion);
      loglike = p1 + p2 + p3 + p4; // diff + p5;
      //OOPS("loglike " + loglike);
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
  private double [] distortion(int[][] instOfCent, Instances centers) {
    double [] distortion = new double [centers.numInstances()];
    for (int i = 0; i < centers.numInstances(); i++) {
      distortion[i] = 0.0;
      for (int j = 0; j < instOfCent[i].length; j++) {
	distortion[i] += distance(m_Model, m_Ranges,
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
   * Returns the closest centers to the instance. 
   *
   * @param ranges min and max of all attributes 
   * @param instance the instance to assign a cluster to
   * @param centers all centers 
   * @param centList the centers to cluster the instance to
   * @return a cluster index
   */
  private static int assignInstanceToCenter(double [][] ranges,
					    Instance instance, 
					    Instances centers,
					    int [] centList) {
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < centList.length; i++) {
      double dist = distance(centers, ranges,
			     instance, centers.instance(centList[i]));
      if (dist < minDist) {
	minDist = dist;     
	bestCluster = i;    
      }                     
    }                         
    //gabi System.out.print("&&"+centList[bestCluster]+"&&");
    return centList[bestCluster];
  }
 
  /**
   * Clusters an instance.
   * @param instance the instance to assign a cluster to.
   * @param centers the centers to cluster the instance to.
   * @return a cluster index.
   */
  private int clusterProcessedInstance(Instance instance, Instances centers){
    
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < centers.numInstances(); i++) {
      double dist = distance(m_Model, m_Ranges,
			     instance, centers.instance(i));

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
  private int clusterProcessedInstance(Instance instance) {
    double minDist = Integer.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < m_NumClusters; i++) {
      double dist = distance(m_Model, m_Ranges,
                             instance, m_ClusterCenters.instance(i));
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
   * @exception if instance could not be classified
   * successfully
   */
  public int clusterInstance(Instance instance) throws Exception {
    m_ReplaceMissingFilter.input(instance);
    Instance inst = m_ReplaceMissingFilter.output();

    return clusterProcessedInstance(inst);
  }

  /**
   * TODO used by KDTree too, needs to go somewhere else
   *
   * Calculates the distance between two instances.
   *
   * @param model the data model
   * @param ranges the min max values of the attributes
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances,
   */          
  public static double distance(Instances model, double [][] ranges,
                                Instance first, Instance second) {  

    double distance = 0;
    int firstI, secondI;

    for (int p1 = 0, p2 = 0; 
	 p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	firstI = model.numAttributes();
      } else {
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = model.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      if (firstI == model.classIndex()) {
	p1++; continue;
      } 
      if (secondI == model.classIndex()) {
	p2++; continue;
      } 
      double diff;
      if (firstI == secondI) {
	diff = difference(model, ranges,
			  firstI, 
			  first.valueSparse(p1),
			  second.valueSparse(p2));
	p1++; p2++;
      } else if (firstI > secondI) {
	diff = difference(model, ranges,
			  secondI, 
			  0, second.valueSparse(p2));
	p2++;
      } else {
	diff = difference(model, ranges,
			  firstI, 
			  first.valueSparse(p1), 0);
	p1++;
      }
      distance += diff * diff;
    }
    
    return distance;
    /*diff thats how we did write it 
    return Math.sqrt(distance / m_Instances.numAttributes());
    */
  }

  /**
   * Computes the difference between two given attribute values.
   * @param model the data model
   * @param ranges the min max values of the attributes
   * @param index the index of the current attribute
   * @param val1 the first attribute value
   * @param val2 the second attribute value
   * @return the distance between the two given attribute values
   */
  private static double difference(Instances model, double [][] ranges,
				   int index, double val1, double val2) {

    switch (model.attribute(index).type()) {

    case Attribute.NOMINAL:

      // If attribute is nominal
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2) ||
	  ((int)val1 != (int)val2)) {
	return 1;
      } else {
	return 0;
      }

    case Attribute.NUMERIC:

      // If attribute is numeric
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2)) {
	if (Instance.isMissingValue(val1) && 
	    Instance.isMissingValue(val2)) {
	  return 1;
	} else {
	  double diff;
	  if (Instance.isMissingValue(val2)) {
	    diff = norm(ranges, val1, index);
	  } else {
	    diff = norm(ranges, val2, index);
	  }
	  if (diff < 0.5) {
	    diff = 1.0 - diff;
	  }
	  return diff;
	}
      } else {
        return val1 - val2;
        /* diff thats how we did it
	return norm(val1, index) - norm(val2, index);
	*/
      }
    default:
      return 0;
    }
  }

  /**
   * Normalises a given value of a numeric attribute.
   * @param ranges the min max values of the attributes
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private static double norm(double [][] ranges, double x, int i) {

    if (Double.isNaN(ranges[i][R_LOW]) || 
	Utils.eq(ranges[i][R_HIGH], ranges[i][R_LOW])) {
      return 0;
    } else {
      return (x - ranges[i][R_LOW]) / (ranges[i][R_WIDTH]);
    }
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

     newVector.addElement(new Option("\tminimum number of clusters." +
				     " (default = 2)." 
				    , "L", 1, "-L <num>"));
     newVector.addElement(new Option("\tmaximum number of clusters." +
				     " (default = 4)." 
				    , "H", 1, "-H <num>"));
     newVector.addElement(new Option("\tfile to write centers to." +
				     " (no default)" 
				    , "O", 1, "-O <file name>"));
     newVector.addElement(new Option("\trandom number seed.\n (default 10)"
				     , "S", 1, "-S <num>"));
     newVector.addElement(new Option(
				     "\tPruning will be done.\n"
				     +"\t(Use this to prune).",
				     "P", 0,"-P"));

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
   * Returns the tip text for this property
   * @return tip text for this property 
   */
  public String pruneTipText() {
    return "set pruning of blacklisting algorithm";
  }

  /**
   * Sets the maximum number of iterations to perform.
   * @param i the number of iterations
   * @exception Exception if i is less than 1
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
   * Sets the TODO.
   * @param i the 
   */
  public void setMinBoxRelWidth(double i) throws Exception {
    m_MinBoxRelWidth = i;
  }

  /**
   * Gets the TODO.
   * @return the 
   */
  public double getMinBoxRelWidth() {
    return  m_MinBoxRelWidth;
  }

  /**
   * Sets the TODO
   * @param i the 
   */
  public void setMaxLeafNumber(int i) throws Exception {
    m_MaxLeafNumber = i;
  }


  /**
   * Get the maximum number of TODO
   * @return the 
   */
  public int getMaxLeafNumber() {
    return  m_MaxLeafNumber;
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
   * Sets the flag to use KDTrees.
   * @param k true to use KDTree
   */
  public void setKDTree(boolean k) {
      m_KDTree = k;
  }

  /**
   * Gets the flag if KDTrees are used.
   * @return flag if KDTrees are used
   */
  public boolean getKDTree() {
    return m_KDTree;
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
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
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

    if (Utils.getFlag('K', options)) {
      setKDTree(true);
    } else {
      setKDTree(false);
    }

    if (Utils.getFlag('P', options)) {
      setPrune(true);
    } else {
      setPrune(false);
    }

    optionString = Utils.getOption('B', options);
    if (optionString.length() != 0) {
      setMinBoxRelWidth(Double.parseDouble(optionString));
    }

    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      setMaxLeafNumber(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('C', options);
    if (optionString.length() != 0) {
      setCutOffFactor(Double.parseDouble(optionString));
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

    optionString = Utils.getOption('D', options);
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
    String[] options = new String[9];
    int current = 0;
    
    options[current++] = "-L";
    options[current++] = "" + getMinNumClusters();
    options[current++] = "-H";
    options[current++] = "" + getMaxNumClusters();
    options[current++] = "-N";
    options[current++] = "" + getInputCenterFile();
    options[current++] = "-O";
    options[current++] = "" + getOutputCenterFile();
    options[current++] = "-S";
    options[current++] = "" + getSeed();
    if (getPrune()) {
      options[current++] = "-P";
    }
    int dL = getDebugLevel();
    if (dL > 0) {
    options[current++] = "-D";
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
   * Used for debug println's.
   * @param output string that is printed
   */
  private void OOPS(int debugLevel, String output) {
    if (debugLevel == m_DebugLevel)
      System.out.println(output);
  }
  private void OOPS(String output) {
    System.out.println(output);
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
