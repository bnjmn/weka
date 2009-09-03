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
 *    RotationForest.java
 *    Copyright (C) 2008 Juan Jose Rodriguez
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.classifiers.meta;

import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.classifiers.RandomizableParallelIteratedSingleClassifierEnhancer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.WeightedInstancesHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.PrincipalComponents;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.unsupervised.instance.RemovePercentage;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for construction a Rotation Forest. Can do classification and regression depending on the base learner. <br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * Juan J. Rodriguez, Ludmila I. Kuncheva, Carlos J. Alonso (2006). Rotation Forest: A new classifier ensemble method. IEEE Transactions on Pattern Analysis and Machine Intelligence. 28(10):1619-1630. URL http://doi.ieeecomputersociety.org/10.1109/TPAMI.2006.211.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Rodriguez2006,
 *    author = {Juan J. Rodriguez and Ludmila I. Kuncheva and Carlos J. Alonso},
 *    journal = {IEEE Transactions on Pattern Analysis and Machine Intelligence},
 *    number = {10},
 *    pages = {1619-1630},
 *    title = {Rotation Forest: A new classifier ensemble method},
 *    volume = {28},
 *    year = {2006},
 *    ISSN = {0162-8828},
 *    URL = {http://doi.ieeecomputersociety.org/10.1109/TPAMI.2006.211}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Whether minGroup (-G) and maxGroup (-H) refer to
 *  the number of groups or their size.
 *  (default: false)</pre>
 * 
 * <pre> -G &lt;num&gt;
 *  Minimum size of a group of attributes:
 *   if numberOfGroups is true, the minimum number
 *   of groups.
 *   (default: 3)</pre>
 * 
 * <pre> -H &lt;num&gt;
 *  Maximum size of a group of attributes:
 *   if numberOfGroups is true, the maximum number
 *   of groups.
 *   (default: 3)</pre>
 * 
 * <pre> -P &lt;num&gt;
 *  Percentage of instances to be removed.
 *   (default: 50)</pre>
 * 
 * <pre> -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  eg: "weka.filters.unsupervised.attribute.PrincipalComponents-R 1.0"</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Juan Jose Rodriguez (jjrodriguez@ubu.es)
 * @version $Revision$
 */
public class RotationForest 
  extends RandomizableParallelIteratedSingleClassifierEnhancer
  implements WeightedInstancesHandler, TechnicalInformationHandler {
  // It implements WeightedInstancesHandler because the base classifier 
  // can implement this interface, but in this method the weights are
  // not used

  /** for serialization */
  static final long serialVersionUID = -3255631880798499936L;

  /** The minimum size of a group */
  protected int m_MinGroup = 3;

  /** The maximum size of a group */
  protected int m_MaxGroup = 3;

  /** 
   * Whether minGroup and maxGroup refer to the number of groups or their 
   * size */
  protected boolean m_NumberOfGroups = false;

  /** The percentage of instances to be removed */
  protected int m_RemovedPercentage = 50;

  /** The attributes of each group */
  protected int [][][] m_Groups = null;

  /** The type of projection filter */
  protected Filter m_ProjectionFilter = null;

  /** The projection filters */
  protected Filter [][] m_ProjectionFilters = null;

  /** Headers of the transformed dataset */
  protected Instances [] m_Headers = null;

  /** Headers of the reduced datasets */
  protected Instances [][] m_ReducedHeaders = null;

  /** Filter that remove useless attributes */
  protected RemoveUseless m_RemoveUseless = null;

  /** Filter that normalized the attributes */
  protected Normalize m_Normalize = null;
  
  /** Training data */
  protected Instances m_data;

  protected Instances [] m_instancesOfClasses;

  protected Random m_random;

  /**
   * Constructor.
   */
  public RotationForest() {
    
    m_Classifier = new weka.classifiers.trees.J48();
    m_ProjectionFilter = defaultFilter();
  }

  /**
   * Default projection method.
   */
  protected Filter defaultFilter() {
    PrincipalComponents filter = new PrincipalComponents();
    filter.setNormalize(false);
    filter.setVarianceCovered(1.0);
    return filter;
  }
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
 
    return "Class for construction a Rotation Forest. Can do classification "
      + "and regression depending on the base learner. \n\n"
      + "For more information, see\n\n"
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
    
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Juan J. Rodriguez and Ludmila I. Kuncheva and Carlos J. Alonso");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE, "Rotation Forest: A new classifier ensemble method");
    result.setValue(Field.JOURNAL, "IEEE Transactions on Pattern Analysis and Machine Intelligence");
    result.setValue(Field.VOLUME, "28");
    result.setValue(Field.NUMBER, "10");
    result.setValue(Field.PAGES, "1619-1630");
    result.setValue(Field.ISSN, "0162-8828");
    result.setValue(Field.URL, "http://doi.ieeecomputersociety.org/10.1109/TPAMI.2006.211");
    
    return result;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
              "\tWhether minGroup (-G) and maxGroup (-H) refer to"
              + "\n\tthe number of groups or their size."
              + "\n\t(default: false)",
              "N", 0, "-N"));

    newVector.addElement(new Option(
              "\tMinimum size of a group of attributes:"
              + "\n\t\tif numberOfGroups is true, the minimum number"
              + "\n\t\tof groups."
              + "\n\t\t(default: 3)",
              "G", 1, "-G <num>"));

    newVector.addElement(new Option(
              "\tMaximum size of a group of attributes:"
              + "\n\t\tif numberOfGroups is true, the maximum number" 
              + "\n\t\tof groups."
              + "\n\t\t(default: 3)",
              "H", 1, "-H <num>"));

    newVector.addElement(new Option(
              "\tPercentage of instances to be removed."
              + "\n\t\t(default: 50)",
              "P", 1, "-P <num>"));

    newVector.addElement(new Option(
	      "\tFull class name of filter to use, followed\n"
	      + "\tby filter options.\n"
	      + "\teg: \"weka.filters.unsupervised.attribute.PrincipalComponents-R 1.0\"",
	      "F", 1, "-F <filter specification>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N
   *  Whether minGroup (-G) and maxGroup (-H) refer to
   *  the number of groups or their size.
   *  (default: false)</pre>
   * 
   * <pre> -G &lt;num&gt;
   *  Minimum size of a group of attributes:
   *   if numberOfGroups is true, the minimum number
   *   of groups.
   *   (default: 3)</pre>
   * 
   * <pre> -H &lt;num&gt;
   *  Maximum size of a group of attributes:
   *   if numberOfGroups is true, the maximum number
   *   of groups.
   *   (default: 3)</pre>
   * 
   * <pre> -P &lt;num&gt;
   *  Percentage of instances to be removed.
   *   (default: 50)</pre>
   * 
   * <pre> -F &lt;filter specification&gt;
   *  Full class name of filter to use, followed
   *  by filter options.
   *  eg: "weka.filters.unsupervised.attribute.PrincipalComponents-R 1.0"</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.J48)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.trees.J48:
   * </pre>
   * 
   * <pre> -U
   *  Use unpruned tree.</pre>
   * 
   * <pre> -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)</pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)</pre>
   * 
   * <pre> -R
   *  Use reduced error pruning.</pre>
   * 
   * <pre> -N &lt;number of folds&gt;
   *  Set number of folds for reduced error
   *  pruning. One fold is used as pruning set.
   *  (default 3)</pre>
   * 
   * <pre> -B
   *  Use binary splits only.</pre>
   * 
   * <pre> -S
   *  Don't perform subtree raising.</pre>
   * 
   * <pre> -L
   *  Do not clean up after the tree has been built.</pre>
   * 
   * <pre> -A
   *  Laplace smoothing for predicted probabilities.</pre>
   * 
   * <pre> -Q &lt;seed&gt;
   *  Seed for random data shuffling (default 1).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    /* Taken from FilteredClassifier */
    String filterString = Utils.getOption('F', options);
    if (filterString.length() > 0) {
      String [] filterSpec = Utils.splitOptions(filterString);
      if (filterSpec.length == 0) {
	throw new IllegalArgumentException("Invalid filter specification string");
      }
      String filterName = filterSpec[0];
      filterSpec[0] = "";
      setProjectionFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));
    } else {
      setProjectionFilter(defaultFilter());
    }

    String tmpStr;
    
    tmpStr = Utils.getOption('G', options);
    if (tmpStr.length() != 0)
      setMinGroup(Integer.parseInt(tmpStr));
    else
      setMinGroup(3);

    tmpStr = Utils.getOption('H', options);
    if (tmpStr.length() != 0)
      setMaxGroup(Integer.parseInt(tmpStr));
    else
      setMaxGroup(3);

    tmpStr = Utils.getOption('P', options);
    if (tmpStr.length() != 0)
      setRemovedPercentage(Integer.parseInt(tmpStr));
    else
      setRemovedPercentage(50);

    setNumberOfGroups(Utils.getFlag('N', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 9];

    int current = 0;

    if (getNumberOfGroups()) { 
      options[current++] = "-N";
    }

    options[current++] = "-G"; 
    options[current++] = "" + getMinGroup();

    options[current++] = "-H"; 
    options[current++] = "" + getMaxGroup();

    options[current++] = "-P"; 
    options[current++] = "" + getRemovedPercentage();

    options[current++] = "-F";
    options[current++] = getProjectionFilterSpec();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numberOfGroupsTipText() {
    return "Whether minGroup and maxGroup refer to the number of groups or their size.";
  }

  /**
   * Set whether minGroup and maxGroup refer to the number of groups or their 
   * size
   *
   * @param numberOfGroups whether minGroup and maxGroup refer to the number 
   * of groups or their size
   */
  public void setNumberOfGroups(boolean numberOfGroups) {

    m_NumberOfGroups = numberOfGroups;
  }

  /**
   * Get whether minGroup and maxGroup refer to the number of groups or their 
   * size
   *
   * @return whether minGroup and maxGroup refer to the number of groups or 
   * their size
   */
  public boolean getNumberOfGroups() {

    return m_NumberOfGroups;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for displaying in the 
   * explorer/experimenter gui
   */
  public String minGroupTipText() {
    return "Minimum size of a group (if numberOfGrups is true, the minimum number of groups.";
  }

  /**
   * Sets the minimum size of a group.
   *
   * @param minGroup the minimum value.
   * of attributes.
   */
  public void setMinGroup( int minGroup ) throws IllegalArgumentException {

    if( minGroup <= 0 )
      throw new IllegalArgumentException( "MinGroup has to be positive." );
    m_MinGroup = minGroup;
  }

  /**
   * Gets the minimum size of a group.
   *
   * @return 		the minimum value.
   */
  public int getMinGroup() {
    return m_MinGroup;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxGroupTipText() {
    return "Maximum size of a group (if numberOfGrups is true, the maximum number of groups.";
  }

  /**
   * Sets the maximum size of a group.
   *
   * @param maxGroup the maximum value.
   * of attributes.
   */
  public void setMaxGroup( int maxGroup ) throws IllegalArgumentException {
 
    if( maxGroup <= 0 )
      throw new IllegalArgumentException( "MaxGroup has to be positive." );
    m_MaxGroup = maxGroup;
  }

  /**
   * Gets the maximum size of a group.
   *
   * @return 		the maximum value.
   */
  public int getMaxGroup() {
    return m_MaxGroup;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String removedPercentageTipText() {
    return "The percentage of instances to be removed.";
  }

  /**
   * Sets the percentage of instance to be removed
   *
   * @param removedPercentage the percentage.
   */
  public void setRemovedPercentage( int removedPercentage ) throws IllegalArgumentException {

    if( removedPercentage < 0 )
      throw new IllegalArgumentException( "RemovedPercentage has to be >=0." );
    if( removedPercentage >= 100 )
      throw new IllegalArgumentException( "RemovedPercentage has to be <100." );
 
    m_RemovedPercentage = removedPercentage;
  }

  /**
   * Gets the percentage of instances to be removed
   *
   * @return 		the percentage.
   */
  public int getRemovedPercentage() {
    return m_RemovedPercentage;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String projectionFilterTipText() {
    return "The filter used to project the data (e.g., PrincipalComponents).";
  }

  /**
   * Sets the filter used to project the data.
   *
   * @param projectionFilter the filter.
   */
  public void setProjectionFilter( Filter projectionFilter ) {

    m_ProjectionFilter = projectionFilter;
  }

  /**
   * Gets the filter used to project the data.
   *
   * @return 		the filter.
   */
  public Filter getProjectionFilter() {
    return m_ProjectionFilter;
  }

  /**
   * Gets the filter specification string, which contains the class name of
   * the filter and any options to the filter
   *
   * @return the filter string.
   */
  /* Taken from FilteredClassifier */
  protected String getProjectionFilterSpec() {
    
    Filter c = getProjectionFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns description of the Rotation Forest classifier.
   *
   * @return description of the Rotation Forest classifier as a string
   */
  public String toString() {
    
    if (m_Classifiers == null) {
      return "RotationForest: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("All the base classifiers: \n\n");
    for (int i = 0; i < m_Classifiers.length; i++)
      text.append(m_Classifiers[i].toString() + "\n\n");
    
    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
  
  protected class ClassifierWrapper extends weka.classifiers.AbstractClassifier {
    
    /** For serialization */
    private static final long serialVersionUID = 2327175798869994435L;
    
    protected weka.classifiers.Classifier m_wrappedClassifier;
    protected int m_classifierNumber;
    
    public ClassifierWrapper(weka.classifiers.Classifier classifier, int classifierNumber) {
      super();
      
      m_wrappedClassifier = classifier;
      m_classifierNumber = classifierNumber;
    }
    
    @Override
    public void buildClassifier(Instances data) throws Exception {
      // TODO Auto-generated method stub
      
      m_ReducedHeaders[m_classifierNumber] = new Instances[ m_Groups[m_classifierNumber].length ];
      FastVector transformedAttributes = new FastVector( m_data.numAttributes() );
      
      // Construction of the dataset for each group of attributes
      for( int j = 0; j < m_Groups[ m_classifierNumber ].length; j++ ) {
        FastVector fv = new FastVector( m_Groups[m_classifierNumber][j].length + 1 );
        for( int k = 0; k < m_Groups[m_classifierNumber][j].length; k++ ) {
          fv.addElement( m_data.attribute( m_Groups[m_classifierNumber][j][k] ).copy() );
        }
        fv.addElement( m_data.classAttribute( ).copy() );
        Instances dataSubSet = new Instances( "rotated-" + m_classifierNumber + "-" + j + "-", 
            fv, 0);
        dataSubSet.setClassIndex( dataSubSet.numAttributes() - 1 );
        
        // Select instances for the dataset
        m_ReducedHeaders[m_classifierNumber][j] = new Instances( dataSubSet, 0 );
        boolean [] selectedClasses = selectClasses( m_instancesOfClasses.length, 
              m_random );
        for( int c = 0; c < selectedClasses.length; c++ ) {
          if( !selectedClasses[c] )
            continue;
          Enumeration enu = m_instancesOfClasses[c].enumerateInstances();
          while( enu.hasMoreElements() ) {
            Instance instance = (Instance)enu.nextElement();
            Instance newInstance = new Instance(dataSubSet.numAttributes());
            newInstance.setDataset( dataSubSet );
            for( int k = 0; k < m_Groups[m_classifierNumber][j].length; k++ ) {
              newInstance.setValue( k, instance.value( m_Groups[m_classifierNumber][j][k] ) );
            }
            newInstance.setClassValue( instance.classValue( ) );
            dataSubSet.add( newInstance );
          }
        }
        
        dataSubSet.randomize(m_random);
        // Remove a percentage of the instances
        Instances originalDataSubSet = dataSubSet;
        dataSubSet.randomize(m_random);
        RemovePercentage rp = new RemovePercentage();
        rp.setPercentage( m_RemovedPercentage );
        rp.setInputFormat( dataSubSet );
        dataSubSet = Filter.useFilter( dataSubSet, rp );
        if( dataSubSet.numInstances() < 2 ) {
          dataSubSet = originalDataSubSet;
        }
        
        // Project de data
        m_ProjectionFilters[m_classifierNumber][j].setInputFormat( dataSubSet );
        Instances projectedData = null;
        do {
          try {
            projectedData = Filter.useFilter( dataSubSet, 
                m_ProjectionFilters[m_classifierNumber][j] );
          } catch ( Exception e ) {
            // The data could not be projected, we add some random instances
            addRandomInstances( dataSubSet, 10, m_random );
          }
        } while( projectedData == null );

        // Include the projected attributes in the attributes of the 
        // transformed dataset
        for( int a = 0; a < projectedData.numAttributes() - 1; a++ ) {
          transformedAttributes.addElement( projectedData.attribute(a).copy());
        }                        
      }
      
      transformedAttributes.addElement( m_data.classAttribute().copy() );
      Instances transformedData = new Instances( "rotated-" + m_classifierNumber + "-", 
        transformedAttributes, 0 );
      transformedData.setClassIndex( transformedData.numAttributes() - 1 );
      m_Headers[ m_classifierNumber ] = new Instances( transformedData, 0 );

      // Project all the training data
      Enumeration enu = m_data.enumerateInstances();
      while( enu.hasMoreElements() ) {
        Instance instance = (Instance)enu.nextElement();
        Instance newInstance = convertInstance( instance, m_classifierNumber );
        transformedData.add( newInstance );
      }

      // Build the base classifier
      if (m_wrappedClassifier instanceof Randomizable) {
        ((Randomizable) m_wrappedClassifier).setSeed(m_random.nextInt());
      }
      m_wrappedClassifier.buildClassifier( transformedData );            
    }
    
    public double classifierInstance(Instance instance) throws Exception {
      return m_wrappedClassifier.classifyInstance(instance);
    }
    
    public double[] distributionForInstance(Instance instance) throws Exception {
      return m_wrappedClassifier.distributionForInstance(instance);
    }
    
    public String toString() {
      return m_wrappedClassifier.toString();
    }
  }
  
  protected Instances getTrainingSet(int iteration) throws Exception {
    
    // The wrapped base classifiers' buildClassifier method creates the
    // transformed training data
    return m_data;
  }

  /**
   * builds the classifier.
   *
   * @param data 	the training data to be used for generating the
   * 			classifier.
   * @throws Exception 	if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_data = new Instances( data );
    super.buildClassifier(m_data);
    
    // Wrap up the base classifiers
    for (int i = 0; i < m_Classifiers.length; i++) {
      ClassifierWrapper cw = new ClassifierWrapper(m_Classifiers[i], i);
      
      m_Classifiers[i] = cw;
    }

    checkMinMax(m_data);

    if( m_data.numInstances() > 0 ) {
      // This function fails if there are 0 instances
      m_random = m_data.getRandomNumberGenerator(m_Seed);
    }
    else {
      m_random = new Random(m_Seed);
    }

    m_RemoveUseless = new RemoveUseless();
    m_RemoveUseless.setInputFormat(m_data);
    m_data = Filter.useFilter(data, m_RemoveUseless);

    m_Normalize = new Normalize();
    m_Normalize.setInputFormat(m_data);
    m_data = Filter.useFilter(m_data, m_Normalize);

    if(m_NumberOfGroups) {
      generateGroupsFromNumbers(m_data, m_random);
    }
    else {
      generateGroupsFromSizes(m_data, m_random);
    }

    m_ProjectionFilters = new Filter[m_Groups.length][];
    for(int i = 0; i < m_ProjectionFilters.length; i++ ) {
      m_ProjectionFilters[i] = Filter.makeCopies( m_ProjectionFilter, 
          m_Groups[i].length );
    }

    int numClasses = m_data.numClasses();

    m_instancesOfClasses = new Instances[numClasses + 1]; 
    if( m_data.classAttribute().isNumeric() ) {
      m_instancesOfClasses = new Instances[numClasses]; 
      m_instancesOfClasses[0] = m_data;
    }
    else {
      m_instancesOfClasses = new Instances[numClasses+1]; 
      for( int i = 0; i < m_instancesOfClasses.length; i++ ) {
        m_instancesOfClasses[ i ] = new Instances( m_data, 0 );
      }
      Enumeration enu = m_data.enumerateInstances();
      while( enu.hasMoreElements() ) {
        Instance instance = (Instance)enu.nextElement();
        if( instance.classIsMissing() ) {
          m_instancesOfClasses[numClasses].add( instance );
	}
	else {
          int c = (int)instance.classValue();
          m_instancesOfClasses[c].add( instance );
        }
      }
      // If there are not instances with a missing class, we do not need to
      // consider them
      if( m_instancesOfClasses[numClasses].numInstances() == 0 ) {
        Instances [] tmp = m_instancesOfClasses;
        m_instancesOfClasses =  new Instances[ numClasses ];
        System.arraycopy( tmp, 0, m_instancesOfClasses, 0, numClasses );
      }
    }

    // These arrays keep the information of the transformed data set
    m_Headers = new Instances[ m_Classifiers.length ];
    m_ReducedHeaders = new Instances[ m_Classifiers.length ][];
    
    buildClassifiers();

    if(m_Debug){
      printGroups();
    }
    
    // save memory
    m_data = null;
    m_instancesOfClasses = null;
    m_random = null;
  }

  /** 
   * Adds random instances to the dataset.
   * 
   * @param dataset the dataset
   * @param numInstances the number of instances
   * @param random a random number generator
   */
  protected void addRandomInstances( Instances dataset, int numInstances, 
                                  Random random ) {
    int n = dataset.numAttributes();				
    double [] v = new double[ n ];
    for( int i = 0; i < numInstances; i++ ) {
      for( int j = 0; j < n; j++ ) {
        Attribute att = dataset.attribute( j );
        if( att.isNumeric() ) {
	  v[ j ] = random.nextDouble();
	}
	else if ( att.isNominal() ) { 
	  v[ j ] = random.nextInt( att.numValues() );
	}
      }
      dataset.add( new Instance( 1, v ) );
    }
  }

  /** 
   * Checks m_MinGroup and m_MaxGroup
   * 
   * @param data the dataset
   */
  protected void checkMinMax(Instances data) {
    if( m_MinGroup > m_MaxGroup ) {
      int tmp = m_MaxGroup;
      m_MaxGroup = m_MinGroup;
      m_MinGroup = tmp;
    }
    
    int n = data.numAttributes();
    if( m_MaxGroup >= n )
      m_MaxGroup = n - 1;
    if( m_MinGroup >= n )
      m_MinGroup = n - 1;
  }

  /** 
   * Selects a non-empty subset of the classes
   * 
   * @param numClasses         the number of classes
   * @param random 	       the random number generator.
   * @return a random subset of classes
   */
  protected boolean [] selectClasses( int numClasses, Random random ) {

    int numSelected = 0;
    boolean selected[] = new boolean[ numClasses ];

    for( int i = 0; i < selected.length; i++ ) {
      if(random.nextBoolean()) {
        selected[i] = true;
        numSelected++;
      }
    }
    if( numSelected == 0 ) {
      selected[random.nextInt( selected.length )] = true;
    }
    return selected;
  }

  /**
   * generates the groups of attributes, given their minimum and maximum
   * sizes.
   *
   * @param data 	the training data to be used for generating the
   * 			groups.
   * @param random 	the random number generator.
   */
  protected void generateGroupsFromSizes(Instances data, Random random) {
    m_Groups = new int[m_Classifiers.length][][];
    for( int i = 0; i < m_Classifiers.length; i++ ) {
      int [] permutation = attributesPermutation(data.numAttributes(), 
                           data.classIndex(), random);

      // The number of groups that have a given size 
      int [] numGroupsOfSize = new int[m_MaxGroup - m_MinGroup + 1];

      int numAttributes = 0;
      int numGroups;

      // Select the size of each group
      for( numGroups = 0; numAttributes < permutation.length; numGroups++ ) {
        int n = random.nextInt( numGroupsOfSize.length );
        numGroupsOfSize[n]++;
        numAttributes += m_MinGroup + n;
      }

      m_Groups[i] = new int[numGroups][];
      int currentAttribute = 0;
      int currentSize = 0;
      for( int j = 0; j < numGroups; j++ ) {
        while( numGroupsOfSize[ currentSize ] == 0 )
          currentSize++;
        numGroupsOfSize[ currentSize ]--;
        int n = m_MinGroup + currentSize;
        m_Groups[i][j] = new int[n];
        for( int k = 0; k < n; k++ ) {
          if( currentAttribute < permutation.length )
            m_Groups[i][j][k] = permutation[ currentAttribute ];
          else
	    // For the last group, it can be necessary to reuse some attributes
            m_Groups[i][j][k] = permutation[ random.nextInt( 
	        permutation.length ) ];
          currentAttribute++;
        }
      }
    }
  }

  /**
   * generates the groups of attributes, given their minimum and maximum
   * numbers.
   *
   * @param data 	the training data to be used for generating the
   * 			groups.
   * @param random 	the random number generator.
   */
  protected void generateGroupsFromNumbers(Instances data, Random random) {
    m_Groups = new int[m_Classifiers.length][][];
    for( int i = 0; i < m_Classifiers.length; i++ ) {
      int [] permutation = attributesPermutation(data.numAttributes(), 
                           data.classIndex(), random);
      int numGroups = m_MinGroup + random.nextInt(m_MaxGroup - m_MinGroup + 1);
      m_Groups[i] = new int[numGroups][];
      int groupSize = permutation.length / numGroups;

      // Some groups will have an additional attribute
      int numBiggerGroups = permutation.length % numGroups;

      // Distribute the attributes in the groups
      int currentAttribute = 0;
      for( int j = 0; j < numGroups; j++ ) {
        if( j < numBiggerGroups ) {
          m_Groups[i][j] = new int[groupSize + 1];
        }
        else {
          m_Groups[i][j] = new int[groupSize];
        }
        for( int k = 0; k < m_Groups[i][j].length; k++ ) {
          m_Groups[i][j][k] = permutation[currentAttribute++];
        }
      }
    }
  }

  /**
   * generates a permutation of the attributes.
   *
   * @param numAttributes       the number of attributes.
   * @param classAttributes     the index of the class attribute.
   * @param random 	        the random number generator.
   * @return a permutation of the attributes
   */
  protected int [] attributesPermutation(int numAttributes, int classAttribute,
                                         Random random) {
    int [] permutation = new int[numAttributes-1];
    int i = 0;
    for(; i < classAttribute; i++){
      permutation[i] = i;
    }
    for(; i < permutation.length; i++){
      permutation[i] = i + 1;
    }

    permute( permutation, random );

    return permutation;
  }

  /**
   * permutes the elements of a given array.
   *
   * @param v       the array to permute
   * @param random  the random number generator.
   */
  protected void permute( int v[], Random random ) {

    for(int i = v.length - 1; i > 0; i-- ) {
      int j = random.nextInt( i + 1 );
      if( i != j ) {
        int tmp = v[i];
        v[i] = v[j];
        v[j] = tmp;
      }
    }
  }

  /**
   * prints the groups.
   */
  protected void printGroups( ) {
    for( int i = 0; i < m_Groups.length; i++ ) {
      for( int j = 0; j < m_Groups[i].length; j++ ) {
        System.err.print( "( " );
        for( int k = 0; k < m_Groups[i][j].length; k++ ) {
          System.err.print( m_Groups[i][j][k] );
          System.err.print( " " );
        }
        System.err.print( ") " );
      }
      System.err.println( );
    }
  }

  /** 
   * Transforms an instance for the i-th classifier.
   *
   * @param instance the instance to be transformed
   * @param i the base classifier number
   * @return the transformed instance
   * @throws Exception if the instance can't be converted successfully 
   */
  protected Instance convertInstance( Instance instance, int i ) 
  throws Exception {
    Instance newInstance = new Instance( m_Headers[ i ].numAttributes( ) );
    newInstance.setDataset( m_Headers[ i ] );
    int currentAttribute = 0;

    // Project the data for each group
    for( int j = 0; j < m_Groups[i].length; j++ ) {
      Instance auxInstance = new Instance( m_Groups[i][j].length + 1 );
      int k;
      for( k = 0; k < m_Groups[i][j].length; k++ ) {
        auxInstance.setValue( k, instance.value( m_Groups[i][j][k] ) );
      }
      auxInstance.setValue( k, instance.classValue( ) );
      auxInstance.setDataset( m_ReducedHeaders[ i ][ j ] );
      m_ProjectionFilters[i][j].input( auxInstance );
      auxInstance = m_ProjectionFilters[i][j].output( );
      m_ProjectionFilters[i][j].batchFinished();
      for( int a = 0; a < auxInstance.numAttributes() - 1; a++ ) {
        newInstance.setValue( currentAttribute++, auxInstance.value( a ) );
      }
    }

    newInstance.setClassValue( instance.classValue() );
    return newInstance;
  }

  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return preedicted class probability distribution
   * @throws Exception if distribution can't be computed successfully 
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    m_RemoveUseless.input(instance);
    instance =m_RemoveUseless.output();
    m_RemoveUseless.batchFinished();

    m_Normalize.input(instance);
    instance =m_Normalize.output();
    m_Normalize.batchFinished();

    double [] sums = new double [instance.numClasses()], newProbs; 
    
    for (int i = 0; i < m_Classifiers.length; i++) {
      Instance convertedInstance = convertInstance(instance, i);
      if (instance.classAttribute().isNumeric() == true) {
	sums[0] += m_Classifiers[i].classifyInstance(convertedInstance);
      } else {
	newProbs = m_Classifiers[i].distributionForInstance(convertedInstance);
	for (int j = 0; j < newProbs.length; j++)
	  sums[j] += newProbs[j];
      }
    }
    if (instance.classAttribute().isNumeric() == true) {
      sums[0] /= (double)m_NumIterations;
      return sums;
    } else if (Utils.eq(Utils.sum(sums), 0)) {
      return sums;
    } else {
      Utils.normalize(sums);
      return sums;
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new RotationForest(), argv);
  }

}

