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
 * CitationKNN.java
 * Copyright (C) 2005 Miguel Garcia Torres
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
/**
 <!-- globalinfo-start -->
 * Modified version of the Citation kNN multi instance classifier.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Jun Wang, Zucker, Jean-Daniel: Solving Multiple-Instance Problem: A Lazy Learning Approach. In: 17th International Conference on Machine Learning, 1119-1125, 2000.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Wang2000,
 *    author = {Jun Wang and Zucker and Jean-Daniel},
 *    booktitle = {17th International Conference on Machine Learning},
 *    editor = {Pat Langley},
 *    pages = {1119-1125},
 *    title = {Solving Multiple-Instance Problem: A Lazy Learning Approach},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;number of references&gt;
 *  Number of Nearest References (default 1)</pre>
 * 
 * <pre> -C &lt;number of citers&gt;
 *  Number of Nearest Citers (default 1)</pre>
 * 
 * <pre> -H &lt;rank&gt;
 *  Rank of the Hausdorff Distance (default 1)</pre>
 * 
 <!-- options-end -->
 *
 * @author Miguel Garcia Torres (mgarciat@ull.es)
 * @version $Revision: 1.9 $ 
 */
public class CitationKNN 
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler,
             TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -8435377743874094852L;
  
  /** The index of the class attribute */
  protected int m_ClassIndex;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** */
  protected int m_IdIndex;    

  /** Debugging output */
  protected boolean m_Debug;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** attribute name structure of the relational attribute*/
  protected Instances m_Attributes;

  /** Number of references */
  protected int m_NumReferences = 1;

  /** Number of citers*/
  protected int m_NumCiters = 1;

  /** Training bags*/
  protected Instances m_TrainBags;

  /** Different debugging output */
  protected boolean m_CNNDebug = false;

  protected boolean m_CitersDebug = false;

  protected boolean m_ReferencesDebug = false;

  protected boolean m_HDistanceDebug = false;

  protected boolean m_NeighborListDebug = false;

  /** C nearest neighbors considering all the bags*/
  protected NeighborList[] m_CNN;

  /** C nearest citers */
  protected int[] m_Citers;

  /** R nearest references */
  protected int[] m_References;

  /** Rank associated to the Hausdorff distance*/
  protected int m_HDRank = 1;

  /** Normalization of the euclidean distance */
  private double[] m_Diffs;

  private double[] m_Min;

  private double m_MinNorm = 0.95;

  private double[] m_Max;

  private double m_MaxNorm = 1.05;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Modified version of the Citation kNN multi instance classifier.\n\n"
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
    result.setValue(Field.AUTHOR, "Jun Wang and Zucker and Jean-Daniel");
    result.setValue(Field.TITLE, "Solving Multiple-Instance Problem: A Lazy Learning Approach");
    result.setValue(Field.BOOKTITLE, "17th International Conference on Machine Learning");
    result.setValue(Field.EDITOR, "Pat Langley");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.PAGES, "1119-1125");
    
    return result;
  }

  /** 
   * Calculates the normalization of each attribute.
   */
  public void preprocessData(){
    int i,j, k;
    double min, max;
    Instances instances;
    Instance instance;
    // compute the min/max of each feature

    for (i=0;i<m_Attributes.numAttributes();i++) {
      min=Double.POSITIVE_INFINITY ;
      max=Double.NEGATIVE_INFINITY ;
      for(j = 0; j < m_TrainBags.numInstances(); j++){
        instances = m_TrainBags.instance(j).relationalValue(1);
        for (k=0;k<instances.numInstances();k++) {
          instance = instances.instance(k);
          if(instance.value(i) < min)
            min= instance.value(i);
          if(instance.value(i) > max)
            max= instance.value(i);
        }
      }
      m_Min[i] = min * m_MinNorm;
      m_Max[i] = max * m_MaxNorm;
      m_Diffs[i]= max * m_MaxNorm - min * m_MinNorm;
    }	    

  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String HDRankTipText() {
    return "The rank associated to the Hausdorff distance.";
  }

  /**
   * Sets the rank associated to the Hausdorff distance
   * @param hDRank the rank of the Hausdorff distance
   */
  public void setHDRank(int hDRank){
    m_HDRank = hDRank;
  }

  /**
   * Returns the rank associated to the Hausdorff distance
   * @return the rank number
   */
  public int getHDRank(){
    return m_HDRank;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numReferencesTipText() {
    return 
        "The number of references considered to estimate the class "
      + "prediction of tests bags.";
  }

  /**
   * Sets the number of references considered to estimate
   * the class prediction of tests bags
   * @param numReferences the number of references
   */
  public void setNumReferences(int numReferences){
    m_NumReferences = numReferences;
  }

  /**
   * Returns the number of references considered to estimate
   * the class prediction of tests bags
   * @return the number of references
   */
  public int getNumReferences(){
    return m_NumReferences;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numCitersTipText() {
    return 
        "The number of citers considered to estimate the class "
      + "prediction of test bags.";
  }

  /**
   * Sets the number of citers considered to estimate
   * the class prediction of tests bags
   * @param numCiters the number of citers
   */
  public void setNumCiters(int numCiters){
    m_NumCiters = numCiters;
  }

  /**
   * Returns the number of citers considered to estimate
   * the class prediction of tests bags
   * @return the number of citers
   */
  public int getNumCiters(){
    return m_NumCiters;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    
    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param train the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances train) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(train);

    // remove instances with missing class
    train = new Instances(train);
    train.deleteWithMissingClass();
    
    m_TrainBags = train;
    m_ClassIndex = train.classIndex();
    m_IdIndex = 0;
    m_NumClasses = train.numClasses();

    m_Classes  = new int [train.numInstances()]; // Class values
    m_Attributes = train.instance(0).relationalValue(1).stringFreeStructure();

    m_Citers = new int[train.numClasses()];
    m_References = new int[train.numClasses()];

    m_Diffs = new double[m_Attributes.numAttributes()];
    m_Min = new double[m_Attributes.numAttributes()];
    m_Max = new double[m_Attributes.numAttributes()];	

    preprocessData();

    buildCNN();

    if(m_CNNDebug){
      System.out.println("########################################### ");
      System.out.println("###########CITATION######################## ");
      System.out.println("########################################### ");
      for(int i = 0; i < m_CNN.length; i++){
        System.out.println("Bag: " + i);
        m_CNN[i].printReducedList();
      }
    }		
  }

  /**
   * generates all the variables associated to the citation
   * classifier
   * 
   * @throws Exception if generation fails
   */
  public void buildCNN() throws Exception {

    int numCiters = 0;

    if((m_NumCiters >= m_TrainBags.numInstances()) ||
        (m_NumCiters < 0))
      throw new Exception("Number of citers is out of the range [0, numInstances)");
    else
      numCiters = m_NumCiters;

    m_CNN = new NeighborList[m_TrainBags.numInstances()]; 
    Instance bag;

    for(int i = 0; i< m_TrainBags.numInstances(); i++){
      bag = m_TrainBags.instance(i);
      //first we find its neighbors
      NeighborList neighborList = findNeighbors(bag, numCiters, m_TrainBags);
      m_CNN[i] = neighborList;
    }
  }

  /**
   * calculates the citers associated to a bag
   * @param bag the bag cited
   */
  public void countBagCiters(Instance bag){

    //Initialization of the vector
    for(int i = 0; i < m_TrainBags.numClasses(); i++)
      m_Citers[i] = 0;
    //
    if(m_CitersDebug == true)
      System.out.println("-------CITERS--------");

    NeighborList neighborList;
    NeighborNode current;
    boolean stopSearch = false;
    int index;

    // compute the distance between the test bag and each training bag. Update
    // the bagCiter count in case it be a neighbour

    double bagDistance = 0;
    for(int i = 0; i < m_TrainBags.numInstances(); i++){
      //measure the distance
      bagDistance =  distanceSet(bag, m_TrainBags.instance(i));
      if(m_CitersDebug == true){
        System.out.print("bag - bag(" + i + "): " + bagDistance);
        System.out.println("   <" + m_TrainBags.instance(i).classValue() + ">");
      }
      //compare the distance to see if it would belong to the
      // neighborhood of each training exemplar
      neighborList = m_CNN[i];
      current = neighborList.mFirst;

      while((current != null) && (!stopSearch)) {
        if(m_CitersDebug == true)
          System.out.println("\t\tciter Distance: " + current.mDistance);
        if(current.mDistance < bagDistance){
          current = current.mNext;
        } else{  
          stopSearch = true;		    
          if(m_CitersDebug == true){
            System.out.println("\t***");
          }
        }
      } 

      if(stopSearch == true){
        stopSearch = false;
        index = (int)(m_TrainBags.instance(i)).classValue();
        m_Citers[index] += 1;
      }

    }

    if(m_CitersDebug == true){
      for(int i= 0; i < m_Citers.length; i++){
        System.out.println("[" + i + "]: " + m_Citers[i]);
      }
    }

  }

  /**
   * Calculates the references of the exemplar bag
   * @param bag the exemplar to which the nearest references
   * will be calculated
   */
  public void countBagReferences(Instance bag){
    int index = 0, referencesIndex = 0;

    if(m_TrainBags.numInstances() < m_NumReferences)
      referencesIndex = m_TrainBags.numInstances() - 1;
    else
      referencesIndex = m_NumReferences;

    if(m_CitersDebug == true){
      System.out.println("-------References (" + referencesIndex+ ")--------");
    }
    //Initialization of the vector
    for(int i = 0; i < m_References.length; i++)
      m_References[i] = 0;

    if(referencesIndex > 0){
      //first we find its neighbors
      NeighborList neighborList = findNeighbors(bag, referencesIndex, m_TrainBags);
      if(m_ReferencesDebug == true){
        System.out.println("Bag: " + bag + " Neighbors: ");
        neighborList.printReducedList();
      }
      NeighborNode current = neighborList.mFirst;
      while(current != null){
        index = (int) current.mBag.classValue();
        m_References[index] += 1;
        current = current.mNext;
      }
    }
    if(m_ReferencesDebug == true){
      System.out.println("References:");
      for(int j = 0; j < m_References.length; j++)
        System.out.println("[" + j + "]: " + m_References[j]);
    }
  }

  /**
   * Build the list of nearest k neighbors to the given test instance.
   * @param bag the bag to search for neighbors of
   * @param kNN the number of nearest neighbors
   * @param bags the data
   * @return a list of neighbors
   */
  protected NeighborList findNeighbors(Instance bag, int kNN, Instances bags){
    double distance;
    int index = 0;

    if(kNN > bags.numInstances())
      kNN = bags.numInstances() - 1;

    NeighborList neighborList = new NeighborList(kNN);
    for(int i = 0; i < bags.numInstances(); i++){
      if(bag != bags.instance(i)){ // for hold-one-out cross-validation
        distance =  distanceSet(bag, bags.instance(i)) ; //mDistanceSet.distance(bag, mInstances, bags.exemplar(i), mInstances);
        if(m_NeighborListDebug)
          System.out.println("distance(bag, " + i + "): " + distance);
        if(neighborList.isEmpty() || (index < kNN) || (distance <= neighborList.mLast.mDistance))
          neighborList.insertSorted(distance, bags.instance(i), i);
        index++;
      } 
    }

    if(m_NeighborListDebug){
      System.out.println("bag neighbors:");
      neighborList.printReducedList();
    }

    return neighborList;
  }

  /**
   * Calculates the distance between two instances
   * @param first instance
   * @param second instance
   * @return the distance value
   */
  public double distanceSet(Instance first, Instance second){
    double[] h_f = new double[first.relationalValue(1).numInstances()];
    double distance;

    //initilization
    for(int i = 0; i < h_f.length; i++)
      h_f[i] = Double.MAX_VALUE;


    int rank;


    if(m_HDRank >= first.relationalValue(1).numInstances())
      rank = first.relationalValue(1).numInstances();
    else if(m_HDRank < 1)
      rank = 1;
    else 
      rank = m_HDRank;

    if(m_HDistanceDebug){
      System.out.println("-------HAUSDORFF DISTANCE--------");
      System.out.println("rank: " + rank + "\nset of instances:");
      System.out.println("\tset 1:");
      for(int i = 0; i < first.relationalValue(1).numInstances(); i++)
        System.out.println(first.relationalValue(1).instance(i));

      System.out.println("\n\tset 2:");
      for(int i = 0; i < second.relationalValue(1).numInstances(); i++)
        System.out.println(second.relationalValue(1).instance(i));

      System.out.println("\n");
    }

    //for each instance in bag first
    for(int i = 0; i < first.relationalValue(1).numInstances(); i++){
      // calculate the distance to each instance in 
      // bag second
      if(m_HDistanceDebug){
        System.out.println("\nDistances:");
      }
      for(int j = 0; j < second.relationalValue(1).numInstances(); j++){
        distance = distance(first.relationalValue(1).instance(i), second.relationalValue(1).instance(j));
        if(distance < h_f[i])
          h_f[i] = distance;
        if(m_HDistanceDebug){
          System.out.println("\tdist(" + i + ", "+ j + "): " + distance + "  --> h_f[" + i + "]: " + h_f[i]);
        }
      }
    }
    int[] index_f = Utils.stableSort(h_f);

    if(m_HDistanceDebug){
      System.out.println("\nRanks:\n");
      for(int i = 0; i < index_f.length; i++)
        System.out.println("\trank " + (i + 1) + ": " + h_f[index_f[i]]);

      System.out.println("\n\t\t>>>>> rank " + rank + ": " + h_f[index_f[rank - 1]] + " <<<<<");
    }

    return h_f[index_f[rank - 1]];
  }

  /**
   * distance between two instances
   * @param first the first instance
   * @param second the other instance
   * @return the distance in double precision
   */
  public double distance(Instance first, Instance second){

    double sum = 0, diff;
    for(int i = 0; i < m_Attributes.numAttributes(); i++){
      diff = (first.value(i) - m_Min[i])/ m_Diffs[i] - 
        (second.value(i) - m_Min[i])/ m_Diffs[i];
      sum += diff * diff;
    }
    return sum = Math.sqrt(sum);
  }

  /**
   * Computes the distribution for a given exemplar
   *
   * @param bag the exemplar for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance bag) 
    throws Exception {

    if(m_TrainBags.numInstances() == 0)
      throw new Exception("No training bags!");

    updateNormalization(bag);

    //build references (R nearest neighbors)
    countBagReferences(bag);

    //build citers
    countBagCiters(bag);

    return makeDistribution();
  }

  /** 
   * Updates the normalization of each attribute.
   * 
   * @param bag the exemplar to update the normalization for
   */
  public void updateNormalization(Instance bag){
    int i, k;
    double min, max;
    Instances instances;
    Instance instance;
    // compute the min/max of each feature
    for (i = 0; i < m_TrainBags.attribute(1).relation().numAttributes(); i++) {
      min = m_Min[i] / m_MinNorm;
      max = m_Max[i] / m_MaxNorm;

      instances = bag.relationalValue(1);
      for (k=0;k<instances.numInstances();k++) {
        instance = instances.instance(k);
        if(instance.value(i) < min)
          min = instance.value(i);
        if(instance.value(i) > max)
          max = instance.value(i);
      }
      m_Min[i] = min * m_MinNorm;
      m_Max[i] = max * m_MaxNorm;
      m_Diffs[i]= max * m_MaxNorm - min * m_MinNorm;
    }
  }

  /**
   * Wether the instances of two exemplars are or  are not equal
   * @param exemplar1 first exemplar
   * @param exemplar2 second exemplar
   * @return if the instances of the exemplars are equal or not
   */
  public boolean equalExemplars(Instance exemplar1, Instance exemplar2){
    if(exemplar1.relationalValue(1).numInstances() == 
        exemplar2.relationalValue(1).numInstances()){
      Instances instances1 = exemplar1.relationalValue(1);
      Instances instances2 = exemplar2.relationalValue(1);
      for(int i = 0; i < instances1.numInstances(); i++){
        Instance instance1 = instances1.instance(i);
        Instance instance2 = instances2.instance(i);
        for(int j = 0; j < instance1.numAttributes(); j++){
          if(instance1.value(j) != instance2.value(j)){
            return false;
          }
        }
      }
      return true;
        }
    return false;
  }

  /**
   * Turn the references and citers list into a probability distribution
   *
   * @return the probability distribution
   * @throws Exception if computation of distribution fails
   */
  protected double[] makeDistribution() throws Exception {
    
    double total = 0;
    double[] distribution = new double[m_TrainBags.numClasses()];
    boolean debug = false;

    total = (double)m_TrainBags.numClasses() / Math.max(1, m_TrainBags.numInstances());

    for(int i = 0; i < m_TrainBags.numClasses(); i++){
      distribution[i] = 1.0 / Math.max(1, m_TrainBags.numInstances());
      if(debug) System.out.println("distribution[" + i + "]: " + distribution[i]);
    }

    if(debug)System.out.println("total: " + total);

    for(int i = 0; i < m_TrainBags.numClasses(); i++){
      distribution[i] += m_References[i];
      distribution[i] += m_Citers[i];
    }

    total = 0;
    //total
    for(int i = 0; i < m_TrainBags.numClasses(); i++){
      total += distribution[i];
      if(debug)System.out.println("distribution[" + i + "]: " + distribution[i]);
    }

    for(int i = 0; i < m_TrainBags.numClasses(); i++){
      distribution[i] = distribution[i] / total;
      if(debug)System.out.println("distribution[" + i + "]: " + distribution[i]);

    }

    return distribution;
  }

  /**
   * Returns an enumeration of all the available options..
   *
   * @return an enumeration of all available options.
   */
  public Enumeration listOptions(){
    Vector result = new Vector();

    result.addElement(new Option(
          "\tNumber of Nearest References (default 1)",
          "R", 0, "-R <number of references>"));
    
    result.addElement(new Option(
          "\tNumber of Nearest Citers (default 1)",
          "C", 0, "-C <number of citers>"));
    
    result.addElement(new Option(
          "\tRank of the Hausdorff Distance (default 1)",
          "H", 0, "-H <rank>"));

    return result.elements();
  }

  /**
   * Sets the OptionHandler's options using the given list. All options
   * will be set (or reset) during this call (i.e. incremental setting
   * of options is not possible). <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R &lt;number of references&gt;
   *  Number of Nearest References (default 1)</pre>
   * 
   * <pre> -C &lt;number of citers&gt;
   *  Number of Nearest Citers (default 1)</pre>
   * 
   * <pre> -H &lt;rank&gt;
   *  Rank of the Hausdorff Distance (default 1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{
    setDebug(Utils.getFlag('D', options));

    String option = Utils.getOption('R', options);
    if(option.length() != 0)
      setNumReferences(Integer.parseInt(option));
    else
      setNumReferences(1);

    option = Utils.getOption('C', options);
    if(option.length() != 0)
      setNumCiters(Integer.parseInt(option));
    else
      setNumCiters(1);

    option = Utils.getOption('H', options);
    if(option.length() != 0)
      setHDRank(Integer.parseInt(option));
    else
      setHDRank(1);
  }
  /**
   * Gets the current option settings for the OptionHandler.
   *
   * @return the list of current option settings as an array of strings
   */
  public String[] getOptions() {
    Vector        result;
    
    result = new Vector();

    if (getDebug())
      result.add("-D");
    
    result.add("-R");
    result.add("" + getNumReferences());
    
    result.add("-C");
    result.add("" + getNumCiters());
    
    result.add("-H");
    result.add("" + getHDRank());

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * returns a string representation of the classifier
   * 
   * @return		the string representation
   */
  public String toString() {
    StringBuffer	result;
    int			i;
    
    result = new StringBuffer();
    
    // title
    result.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
    result.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");

    if (m_Citers == null) {
      result.append("no model built yet!\n");
    }
    else {
      // internal representation
      result.append("Citers....: " + Utils.arrayToString(m_Citers) + "\n");

      result.append("References: " + Utils.arrayToString(m_References) + "\n");

      result.append("Min.......: ");
      for (i = 0; i < m_Min.length; i++) {
	if (i > 0)
	  result.append(",");
	result.append(Utils.doubleToString(m_Min[i], 3));
      }
      result.append("\n");

      result.append("Max.......: ");
      for (i = 0; i < m_Max.length; i++) {
	if (i > 0)
	  result.append(",");
	result.append(Utils.doubleToString(m_Max[i], 3));
      }
      result.append("\n");

      result.append("Diffs.....: ");
      for (i = 0; i < m_Diffs.length; i++) {
	if (i > 0)
	  result.append(",");
	result.append(Utils.doubleToString(m_Diffs[i], 3));
      }
      result.append("\n");
    }
    
    return result.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.9 $");
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new CitationKNN(), argv);
  }

  //########################################################################
  //########################################################################
  //########################################################################
  //########################################################################
  //########################################################################

  /**
   * A class for storing data about a neighboring instance
   */
  private class NeighborNode 
    implements Serializable, RevisionHandler {

    /** for serialization */
    static final long serialVersionUID = -3947320761906511289L;
    
    /** The neighbor bag */
    private Instance mBag;

    /** The distance from the current instance to this neighbor */
    private double mDistance;

    /** A link to the next neighbor instance */
    private NeighborNode mNext;

    /** the position in the bag */
    private int mBagPosition;    
    
    /**
     * Create a new neighbor node.
     *
     * @param distance the distance to the neighbor
     * @param bag the bag instance
     * @param position the position in the bag
     * @param next the next neighbor node
     */
    public NeighborNode(double distance, Instance bag, int position, NeighborNode next){
      mDistance = distance;
      mBag = bag;
      mNext = next;
      mBagPosition = position;
    }

    /**
     * Create a new neighbor node that doesn't link to any other nodes.
     *
     * @param distance the distance to the neighbor
     * @param bag the neighbor instance
     * @param position the position in the bag
     */
    public NeighborNode(double distance, Instance bag, int position) {
      this(distance, bag, position, null);
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.9 $");
    }
  }
  
  //##################################################
  /**
   * A class for a linked list to store the nearest k neighbours
   * to an instance. We use a list so that we can take care of
   * cases where multiple neighbours are the same distance away.
   * i.e. the minimum length of the list is k.
   */
  private class NeighborList 
    implements Serializable, RevisionHandler {
    
    /** for serialization */
    static final long serialVersionUID = 3432555644456217394L;
    
    /** The first node in the list */
    private NeighborNode mFirst;
    /** The last node in the list */
    private NeighborNode mLast;

    /** The number of nodes to attempt to maintain in the list */
    private int mLength = 1;

    /**
     * Creates the neighborlist with a desired length
     *
     * @param length the length of list to attempt to maintain
     */
    public NeighborList(int length) {
      mLength = length;
    }
    /**
     * Gets whether the list is empty.
     *
     * @return true if so
     */
    public boolean isEmpty() {
      return (mFirst == null);
    }
    /**
     * Gets the current length of the list.
     *
     * @return the current length of the list
     */
    public int currentLength() {

      int i = 0;
      NeighborNode current = mFirst;
      while (current != null) {
        i++;
        current = current.mNext;
      }
      return i;
    }

    /**
     * Inserts an instance neighbor into the list, maintaining the list
     * sorted by distance.
     *
     * @param distance the distance to the instance
     * @param bag the neighboring instance
     * @param position the position in the bag
     */
    public void insertSorted(double distance, Instance bag, int position) {

      if (isEmpty()) {
        mFirst = mLast = new NeighborNode(distance, bag, position);
      } else {
        NeighborNode current = mFirst;
        if (distance < mFirst.mDistance) {// Insert at head
          mFirst = new NeighborNode(distance, bag, position, mFirst);
        } else { // Insert further down the list
          for( ;(current.mNext != null) && 
              (current.mNext.mDistance < distance); 
              current = current.mNext);
          current.mNext = new NeighborNode(distance, bag, position, current.mNext);
          if (current.equals(mLast)) {
            mLast = current.mNext;
          }
        }

        // Trip down the list until we've got k list elements (or more if the
        // distance to the last elements is the same).
        int valcount = 0;
        for(current = mFirst; current.mNext != null; 
            current = current.mNext) {
          valcount++;
          if ((valcount >= mLength) && (current.mDistance != 
                current.mNext.mDistance)) {
            mLast = current;
            current.mNext = null;
            break;
                }
            }
      }
    }

    /**
     * Prunes the list to contain the k nearest neighbors. If there are
     * multiple neighbors at the k'th distance, all will be kept.
     *
     * @param k the number of neighbors to keep in the list.
     */
    public void pruneToK(int k) {
      if (isEmpty())
        return;
      if (k < 1)
        k = 1;

      int currentK = 0;
      double currentDist = mFirst.mDistance;
      NeighborNode current = mFirst;
      for(; current.mNext != null; current = current.mNext) {
        currentK++;
        currentDist = current.mDistance;
        if ((currentK >= k) && (currentDist != current.mNext.mDistance)) {
          mLast = current;
          current.mNext = null;
          break;
        }
      }
    }

    /**
     * Prints out the contents of the neighborlist
     */
    public void printList() {

      if (isEmpty()) {
        System.out.println("Empty list");
      } else {
        NeighborNode current = mFirst;
        while (current != null) {
          System.out.print("Node: instance " + current.mBagPosition + "\n");
          System.out.println(current.mBag);
          System.out.println(", distance " + current.mDistance);
          current = current.mNext;
        }
        System.out.println();
      }
    }
    /**
     * Prints out the contents of the neighborlist
     */
    public void printReducedList() {

      if (isEmpty()) {
        System.out.println("Empty list");
      } else {
        NeighborNode current = mFirst;
        while (current != null) {
          System.out.print("Node: bag " + current.mBagPosition + "  (" + current.mBag.relationalValue(1).numInstances() +"): ");
          //for(int i = 0; i < current.mBag.getInstances().numInstances(); i++){
          //System.out.print(" " + (current.mBag).getInstances().instance(i));
          //}
          System.out.print("   <" + current.mBag.classValue() + ">");
          System.out.println("  (d: " + current.mDistance + ")");
          current = current.mNext;
        }
        System.out.println();
      }
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.9 $");
    }
  }
}
