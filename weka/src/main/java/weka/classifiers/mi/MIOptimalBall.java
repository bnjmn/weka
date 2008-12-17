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
 * MIOptimalBall.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.matrix.DoubleVector;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.PropositionalToMultiInstance;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * This classifier tries to find a suitable ball in the multiple-instance space, with a certain data point in the instance space as a ball center. The possible ball center is a certain instance in a positive bag. The possible radiuses are those which can achieve the highest classification accuracy. The model selects the maximum radius as the radius of the optimal ball.<br/>
 * <br/>
 * For more information about this algorithm, see:<br/>
 * <br/>
 * Peter Auer, Ronald Ortner: A Boosting Approach to Multiple Instance Learning. In: 15th European Conference on Machine Learning, 63-74, 2004.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Auer2004,
 *    author = {Peter Auer and Ronald Ortner},
 *    booktitle = {15th European Conference on Machine Learning},
 *    note = {LNAI 3201},
 *    pages = {63-74},
 *    publisher = {Springer},
 *    title = {A Boosting Approach to Multiple Instance Learning},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;num&gt;
 *  Whether to 0=normalize/1=standardize/2=neither. 
 *  (default 0=normalize)</pre>
 * 
 <!-- options-end -->
 * 
 * @author Lin Dong (ld21@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $ 
 */
public class MIOptimalBall 
  extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, 
             MultiInstanceCapabilitiesHandler, TechnicalInformationHandler {  

  /** for serialization */
  static final long serialVersionUID = -6465750129576777254L;
  
  /** center of the optimal ball */
  protected double[] m_Center;

  /** radius of the optimal ball */
  protected double m_Radius;

  /** the distances from each instance in a positive bag to each bag*/
  protected double [][][]m_Distance;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_NORMALIZE;

  /** Normalize training data */
  public static final int FILTER_NORMALIZE = 0;
  /** Standardize training data */
  public static final int FILTER_STANDARDIZE = 1;
  /** No normalization/standardization */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"),
  };

  /** filter used to convert the MI dataset into single-instance dataset */
  protected MultiInstanceToPropositional m_ConvertToSI = new MultiInstanceToPropositional();

  /** filter used to convert the single-instance dataset into MI dataset */
  protected PropositionalToMultiInstance m_ConvertToMI = new PropositionalToMultiInstance();

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
         "This classifier tries to find a suitable ball in the "
       + "multiple-instance space, with a certain data point in the instance "
       + "space as a ball center. The possible ball center is a certain "
       + "instance in a positive bag. The possible radiuses are those which can "
       + "achieve the highest classification accuracy. The model selects the "
       + "maximum radius as the radius of the optimal ball.\n\n"
       + "For more information about this algorithm, see:\n\n"
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
    result.setValue(Field.AUTHOR, "Peter Auer and Ronald Ortner");
    result.setValue(Field.TITLE, "A Boosting Approach to Multiple Instance Learning");
    result.setValue(Field.BOOKTITLE, "15th European Conference on Machine Learning");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.PAGES, "63-74");
    result.setValue(Field.PUBLISHER, "Springer");
    result.setValue(Field.NOTE, "LNAI 3201");
    
    return result;
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
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.BINARY_CLASS);
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
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances train = new Instances(data);
    train.deleteWithMissingClass();
    
    int numAttributes = train.attribute(1).relation().numAttributes();	
    m_Center = new double[numAttributes];

    if (getDebug())
      System.out.println("Start training ..."); 

    // convert the training dataset into single-instance dataset
    m_ConvertToSI.setInputFormat(train);	
    train = Filter.useFilter( train, m_ConvertToSI);

    if (m_filterType == FILTER_STANDARDIZE) 
      m_Filter = new Standardize();
    else if (m_filterType == FILTER_NORMALIZE)
      m_Filter = new Normalize();
    else 
      m_Filter = null;

    if (m_Filter!=null) {
      // normalize/standardize the converted training dataset
      m_Filter.setInputFormat(train);
      train = Filter.useFilter(train, m_Filter);
    }

    // convert the single-instance dataset into multi-instance dataset
    m_ConvertToMI.setInputFormat(train);
    train = Filter.useFilter(train, m_ConvertToMI);

    /*calculate all the distances (and store them in m_Distance[][][]), which
      are from each instance in all positive bags to all bags */
    calculateDistance(train);

    /*find the suitable ball center (m_Center) and the corresponding radius (m_Radius)*/
    findRadius(train); 

    if (getDebug())
      System.out.println("Finish building optimal ball model");
  }		



  /** 
   * calculate the distances from each instance in a positive bag to each bag.
   * All result distances are stored in m_Distance[i][j][k], where
   * m_Distance[i][j][k] refers the distances from the jth instance in ith bag
   * to the kth bag 
   * 
   * @param train the multi-instance dataset (with relational attribute)   
   */
  public void calculateDistance (Instances train) {
    int numBags =train.numInstances();
    int numInstances;
    Instance tempCenter;

    m_Distance = new double [numBags][][];
    for (int i=0; i<numBags; i++) {
      if (train.instance(i).classValue() == 1.0) { //positive bag
        numInstances = train.instance(i).relationalValue(1).numInstances();
        m_Distance[i]= new double[numInstances][];
        for (int j=0; j<numInstances; j++) {
          tempCenter = train.instance(i).relationalValue(1).instance(j);
          m_Distance[i][j]=new double [numBags];  //store the distance from one center to all the bags
          for (int k=0; k<numBags; k++){
            if (i==k)
              m_Distance[i][j][k]= 0;     
            else 
              m_Distance[i][j][k]= minBagDistance (tempCenter, train.instance(k));    	    
          }
        }
      } 
    }
  } 

  /**
   * Calculate the distance from one data point to a bag
   *
   * @param center the data point in instance space
   * @param bag the bag 
   * @return the double value as the distance.
   */
  public double minBagDistance (Instance center, Instance bag){
    double distance;
    double minDistance = Double.MAX_VALUE;
    Instances temp = bag.relationalValue(1);  
    //calculate the distance from the data point to each instance in the bag and return the minimum distance 
    for (int i=0; i<temp.numInstances(); i++){
      distance =0;
      for (int j=0; j<center.numAttributes(); j++)
        distance += (center.value(j)-temp.instance(i).value(j))*(center.value(j)-temp.instance(i).value(j));

      if (minDistance>distance)
        minDistance = distance;
    }
    return Math.sqrt(minDistance); 
  }

  /**
   * Find the maximum radius for the optimal ball.
   *
   * @param train the multi-instance data 
   */ 
  public void findRadius(Instances train) {
    int numBags, numInstances;
    double radius, bagDistance;
    int highestCount=0;

    numBags = train.numInstances();
    //try each instance in all positive bag as a ball center (tempCenter),   	
    for (int i=0; i<numBags; i++) {
      if (train.instance(i).classValue()== 1.0) {//positive bag   
        numInstances = train.instance(i).relationalValue(1).numInstances();
        for (int j=0; j<numInstances; j++) {   	    		
          Instance tempCenter = train.instance(i).relationalValue(1).instance(j);

          //set the possible set of ball radius corresponding to each tempCenter,
          double sortedDistance[] = sortArray(m_Distance[i][j]); //sort the distance value    	          
          for (int k=1; k<sortedDistance.length; k++){
            radius = sortedDistance[k]-(sortedDistance[k]-sortedDistance[k-1])/2.0 ;

            //evaluate the performance on the training data according to
            //the curren selected tempCenter and the set of radius   
            int correctCount =0;
            for (int n=0; n<numBags; n++){
              bagDistance=m_Distance[i][j][n];  
              if ((bagDistance <= radius && train.instance(n).classValue()==1.0) 
                  ||(bagDistance > radius && train.instance(n).classValue ()==0.0))
                correctCount += train.instance(n).weight();

            }

            //and keep the track of the ball center and the maximum radius which can achieve the highest accuracy. 
            if (correctCount > highestCount || (correctCount==highestCount && radius > m_Radius)){
              highestCount = correctCount;
              m_Radius = radius;
              for (int p=0; p<tempCenter.numAttributes(); p++)
                m_Center[p]= tempCenter.value(p);
            }      
          }
        }
      }
    } 
  }

  /**
   * Sort the array.
   *
   * @param distance the array need to be sorted
   * @return sorted array
   */ 
  public double [] sortArray(double [] distance) {
    double [] sorted = new double [distance.length];

    //make a copy of the array
    double []disCopy = new double[distance.length];
    for (int i=0;i<distance.length; i++)
      disCopy[i]= distance[i];

    DoubleVector sortVector = new DoubleVector(disCopy);
    sortVector.sort();
    sorted = sortVector.getArrayCopy(); 
    return sorted;
  }


  /**
   * Computes the distribution for a given multiple instance
   *
   * @param newBag the instance for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance newBag)
    throws Exception {  

    double [] distribution = new double[2];	
    double distance; 
    distribution[0]=0;	 
    distribution[1]=0;

    Instances insts = new Instances(newBag.dataset(),0);
    insts.add(newBag);  

    // Filter instances 
    insts= Filter.useFilter( insts, m_ConvertToSI); 	
    if (m_Filter!=null) 
      insts = Filter.useFilter(insts, m_Filter);     

    //calculate the distance from each single instance to the ball center
    int numInsts = insts.numInstances(); 		
    insts.deleteAttributeAt(0); //remove the bagIndex attribute, no use for the distance calculation

    for (int i=0; i<numInsts; i++){
      distance =0;	   
      for (int j=0; j<insts.numAttributes()-1; j++)
        distance += (insts.instance(i).value(j) - m_Center[j])*(insts.instance(i).value(j)-m_Center[j]);  

      if (distance <=m_Radius*m_Radius){  // check whether this single instance is inside the ball
        distribution[1]=1.0;  //predicted as a positive bag   	  
        break;
      }	
    }

    distribution[0]= 1-distribution[1]; 

    return distribution; 
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
          "\tWhether to 0=normalize/1=standardize/2=neither. \n"
          + "\t(default 0=normalize)",
          "N", 1, "-N <num>"));

    return result.elements();
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    
    result = new Vector();

    if (getDebug())
      result.add("-D");
    
    result.add("-N");
    result.add("" + m_filterType);

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;num&gt;
   *  Whether to 0=normalize/1=standardize/2=neither. 
   *  (default 0=normalize)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));

    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));
    }
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "The filter type for transforming the training data.";
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {

    return new SelectedTag(m_filterType, TAGS_FILTER);
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.5 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new MIOptimalBall(), argv);
  }
}
