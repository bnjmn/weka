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
 *    Decorate.java
 *    Copyright (C) 2002 Prem Melville
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.RandomizableIteratedSingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.UnsupportedClassTypeException;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * DECORATE is a meta-learner for building diverse ensembles of classifiers by using specially constructed artificial training examples. Comprehensive experiments have demonstrated that this technique is consistently more accurate than the base classifier, Bagging and Random Forests.Decorate also obtains higher accuracy than Boosting on small training sets, and achieves comparable performance on larger training sets. <br/>
 * <br/>
 * For more details see: <br/>
 * <br/>
 * P. Melville, R. J. Mooney: Constructing Diverse Classifier Ensembles Using Artificial Training Examples. In: Eighteenth International Joint Conference on Artificial Intelligence, 505-510, 2003.<br/>
 * <br/>
 * P. Melville, R. J. Mooney (2004). Creating Diversity in Ensembles Using Artificial Data. Information Fusion: Special Issue on Diversity in Multiclassifier Systems..
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Melville2003,
 *    author = {P. Melville and R. J. Mooney},
 *    booktitle = {Eighteenth International Joint Conference on Artificial Intelligence},
 *    pages = {505-510},
 *    title = {Constructing Diverse Classifier Ensembles Using Artificial Training Examples},
 *    year = {2003}
 * }
 * 
 * &#64;article{Melville2004,
 *    author = {P. Melville and R. J. Mooney},
 *    journal = {Information Fusion: Special Issue on Diversity in Multiclassifier Systems},
 *    note = {submitted},
 *    title = {Creating Diversity in Ensembles Using Artificial Data},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -E
 *  Desired size of ensemble.
 *  (default 10)</pre>
 * 
 * <pre> -R
 *  Factor that determines number of artificial examples to generate.
 *  Specified proportional to training set size.
 *  (default 1.0)</pre>
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
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Prem Melville (melville@cs.utexas.edu)
 * @version $Revision$ 
 */
public class Decorate 
    extends RandomizableIteratedSingleClassifierEnhancer
    implements TechnicalInformationHandler {
      
    /** for serialization */
    static final long serialVersionUID = -6020193348750269931L;
  
    /** Vector of classifiers that make up the committee/ensemble. */
    protected Vector m_Committee = null;
    
    /** The desired ensemble size. */
    protected int m_DesiredSize = 10;
    
    /** Amount of artificial/random instances to use - specified as a
        fraction of the training data size. */
    protected double m_ArtSize = 1.0 ;

    /** The random number generator. */
    protected Random m_Random = new Random(0);
    
    /** Attribute statistics - used for generating artificial examples. */
    protected Vector m_AttributeStats = null;

    
  /**
   * Constructor.
   */
  public Decorate() {
    
    m_Classifier = new weka.classifiers.trees.J48();
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
     * Returns an enumeration describing the available options
     *
     * @return an enumeration of all the available options
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(8);

	newVector.addElement(new Option(
              "\tDesired size of ensemble.\n" 
              + "\t(default 10)",
              "E", 1, "-E"));
	newVector.addElement(new Option(
 	    "\tFactor that determines number of artificial examples to generate.\n"
           +"\tSpecified proportional to training set size.\n" 
          + "\t(default 1.0)",
	    "R", 1, "-R"));

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
     * <pre> -E
     *  Desired size of ensemble.
     *  (default 10)</pre>
     * 
     * <pre> -R
     *  Factor that determines number of artificial examples to generate.
     *  Specified proportional to training set size.
     *  (default 1.0)</pre>
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
     * Options after -- are passed to the designated classifier.<p>
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {

	String desiredSize = Utils.getOption('E', options);
	if (desiredSize.length() != 0) {
	    setDesiredSize(Integer.parseInt(desiredSize));
	} else {
	    setDesiredSize(10);
	}
	
	String artSize = Utils.getOption('R', options);
	if (artSize.length() != 0) {
	    setArtificialSize(Double.parseDouble(artSize));
	} else {
	    setArtificialSize(1.0);
	}

        super.setOptions(options);
    }
    
    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String [] getOptions() {

      String [] superOptions = super.getOptions();
      String [] options = new String [superOptions.length + 4];
      
      int current = 0;
      options[current++] = "-E"; options[current++] = "" + getDesiredSize();
      options[current++] = "-R"; options[current++] = "" + getArtificialSize();
      
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
  public String desiredSizeTipText() {
      return "the desired number of member classifiers in the Decorate ensemble. Decorate may terminate "
	+"before this size is reached (depending on the value of numIterations). "
	+"Larger ensemble sizes usually lead to more accurate models, but increases "
	+"training time and model complexity.";
  }
    
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "the maximum number of Decorate iterations to run. Each iteration generates a classifier, "
	+"but does not necessarily add it to the ensemble. Decorate stops when the desired ensemble "
	+"size is reached. This parameter should be greater than "
	+"equal to the desiredSize. If the desiredSize is not being reached it may help to "
	+"increase this value.";
  }
    
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String artificialSizeTipText() {
    return "determines the number of artificial examples to use during training. Specified as "
	+"a proportion of the training data. Higher values can increase ensemble diversity.";
  }

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
      return "DECORATE is a meta-learner for building diverse ensembles of "
	  +"classifiers by using specially constructed artificial training "
	  +"examples. Comprehensive experiments have demonstrated that this "
	  +"technique is consistently more accurate than the base classifier, Bagging and Random Forests."
	  +"Decorate also obtains higher accuracy than Boosting on small training sets, and achieves "
	  +"comparable performance on larger training sets. \n\n"
	  +"For more details see: \n\n"
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
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "P. Melville and R. J. Mooney");
    result.setValue(Field.TITLE, "Constructing Diverse Classifier Ensembles Using Artificial Training Examples");
    result.setValue(Field.BOOKTITLE, "Eighteenth International Joint Conference on Artificial Intelligence");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.PAGES, "505-510");

    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "P. Melville and R. J. Mooney");
    additional.setValue(Field.TITLE, "Creating Diversity in Ensembles Using Artificial Data");
    additional.setValue(Field.JOURNAL, "Information Fusion: Special Issue on Diversity in Multiclassifier Systems");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.NOTE, "submitted");
    
    return result;
  }

    /**
     * Factor that determines number of artificial examples to generate.
     *
     * @return factor that determines number of artificial examples to generate
     */
    public double getArtificialSize() {
	return m_ArtSize;
    }
  
    /**
     * Sets factor that determines number of artificial examples to generate.
     *
     * @param newArtSize factor that determines number of artificial examples to generate
     */
    public void setArtificialSize(double newArtSize) {
	m_ArtSize = newArtSize;
    }
    
    /**
     * Gets the desired size of the committee.
     *
     * @return the desired size of the committee
     */
    public int getDesiredSize() {
	return m_DesiredSize;
    }
    
    /**
     * Sets the desired size of the committee.
     *
     * @param newDesiredSize the desired size of the committee
     */
    public void setDesiredSize(int newDesiredSize) {
	m_DesiredSize = newDesiredSize;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return      the capabilities of this classifier
     */
    public Capabilities getCapabilities() {
      Capabilities result = super.getCapabilities();

      // class
      result.disableAllClasses();
      result.disableAllClassDependencies();
      result.enable(Capability.NOMINAL_CLASS);

      // instances
      result.setMinimumNumberInstances(m_DesiredSize);

      return result;
    }

    /**
     * Build Decorate classifier
     *
     * @param data the training data to be used for generating the classifier
     * @throws Exception if the classifier could not be built successfully
     */
    public void buildClassifier(Instances data) throws Exception {
      if(m_Classifier == null) {
        throw new Exception("A base classifier has not been specified!");
      }
      
      // can classifier handle the data?
      getCapabilities().testWithFail(data);
      
      // remove instances with missing class
      data = new Instances(data);
      data.deleteWithMissingClass();
  
	//initialize random number generator
	if(m_Seed==-1) m_Random = new Random();
	else m_Random = new Random(m_Seed);
	
	int i = 1;//current committee size
	int numTrials = 1;//number of Decorate iterations 
	Instances divData = new Instances(data);//local copy of data - diversity data
	Instances artData = null;//artificial data

	//compute number of artficial instances to add at each iteration
	int artSize = (int) (Math.abs(m_ArtSize)*divData.numInstances());
	if(artSize==0) artSize=1;//atleast add one random example
	computeStats(data);//Compute training data stats for creating artificial examples
	
	//initialize new committee
	m_Committee = new Vector();
	Classifier newClassifier = m_Classifier;
	newClassifier.buildClassifier(divData);
	m_Committee.add(newClassifier);
	double eComm = computeError(divData);//compute ensemble error
	if(m_Debug) System.out.println("Initialize:\tClassifier "+i+" added to ensemble. Ensemble error = "+eComm);
	
	//repeat till desired committee size is reached OR the max number of iterations is exceeded 
	while(i<m_DesiredSize && numTrials<m_NumIterations){
	    //Generate artificial training examples
	    artData = generateArtificialData(artSize, data);
	    
	    //Label artificial examples
	    labelData(artData);
	    addInstances(divData, artData);//Add new artificial data
	    
	    //Build new classifier
	    Classifier tmp[] = AbstractClassifier.makeCopies(m_Classifier,1);
	    newClassifier = tmp[0]; 
	    newClassifier.buildClassifier(divData);
	    //Remove all the artificial data
	    removeInstances(divData, artSize);
	    
	    //Test if the new classifier should be added to the ensemble
	    m_Committee.add(newClassifier);//add new classifier to current committee
	    double currError = computeError(divData);
	    if(currError <= eComm){//adding the new member did not increase the error
		i++;
		eComm = currError;
		if(m_Debug) System.out.println("Iteration: "+(1+numTrials)+"\tClassifier "+i+" added to ensemble. Ensemble error = "+eComm);
	    }else{//reject the current classifier because it increased the ensemble error 
		m_Committee.removeElementAt(m_Committee.size()-1);//pop the last member
	    }
	    numTrials++;
	}
    }
    
    /** 
     * Compute and store statistics required for generating artificial data.
     *
     * @param data training instances
     * @throws Exception if statistics could not be calculated successfully
     */
    protected void computeStats(Instances data) throws Exception{
	int numAttributes = data.numAttributes();
	m_AttributeStats = new Vector(numAttributes);//use to map attributes to their stats
	
	for(int j=0; j<numAttributes; j++){
	    if(data.attribute(j).isNominal()){
		//Compute the probability of occurence of each distinct value 
		int []nomCounts = (data.attributeStats(j)).nominalCounts;
		double []counts = new double[nomCounts.length];
		if(counts.length < 2) throw new Exception("Nominal attribute has less than two distinct values!"); 
		//Perform Laplace smoothing
		for(int i=0; i<counts.length; i++)
		    counts[i] = nomCounts[i] + 1;
		Utils.normalize(counts);
		double []stats = new double[counts.length - 1];
		stats[0] = counts[0];
		//Calculate cumulative probabilities
		for(int i=1; i<stats.length; i++)
		    stats[i] = stats[i-1] + counts[i];
		m_AttributeStats.add(j,stats);
	    }else if(data.attribute(j).isNumeric()){
		//Get mean and standard deviation from the training data
		double []stats = new double[2];
		stats[0] = data.meanOrMode(j);
		stats[1] = Math.sqrt(data.variance(j));
		m_AttributeStats.add(j,stats);
	    }else System.err.println("Decorate can only handle numeric and nominal values.");
	}
    }

    /**
     * Generate artificial training examples.
     * @param artSize size of examples set to create
     * @param data training data
     * @return the set of unlabeled artificial examples
     */
    protected Instances generateArtificialData(int artSize, Instances data){
	int numAttributes = data.numAttributes();
	Instances artData = new Instances(data, artSize);
	double []att; 
	Instance artInstance;
	
	for(int i=0; i<artSize; i++){
	    att = new double[numAttributes];
	    for(int j=0; j<numAttributes; j++){
		if(data.attribute(j).isNominal()){
		    //Select nominal value based on the frequency of occurence in the training data  
		    double []stats = (double [])m_AttributeStats.get(j);
		    att[j] =  (double) selectIndexProbabilistically(stats);
		}
		else if(data.attribute(j).isNumeric()){
		    //Generate numeric value from the Guassian distribution 
		    //defined by the mean and std dev of the attribute
		    double []stats = (double [])m_AttributeStats.get(j);
		    att[j] = (m_Random.nextGaussian()*stats[1])+stats[0];
		}else System.err.println("Decorate can only handle numeric and nominal values.");
	    }
	    artInstance = new Instance(1.0, att);
	    artData.add(artInstance);
	}
	return artData;
    }
    
    
    /** 
     * Labels the artificially generated data.
     *
     * @param artData the artificially generated instances
     * @throws Exception if instances cannot be labeled successfully 
     */
    protected void labelData(Instances artData) throws Exception {
	Instance curr;
	double []probs;
	
	for(int i=0; i<artData.numInstances(); i++){
	    curr = artData.instance(i);
	    //compute the class membership probs predicted by the current ensemble 
	    probs = distributionForInstance(curr);
	    //select class label inversely proportional to the ensemble predictions
	    curr.setClassValue(inverseLabel(probs));
	}	
    }
    

    /** 
     * Select class label such that the probability of selection is
     * inversely proportional to the ensemble's predictions.
     *
     * @param probs class membership probabilities of instance
     * @return index of class label selected
     * @throws Exception if instances cannot be labeled successfully 
     */
    protected int inverseLabel(double []probs) throws Exception{
	double []invProbs = new double[probs.length];
	//Produce probability distribution inversely proportional to the given
	for(int i=0; i<probs.length; i++){
	    if(probs[i]==0){
		invProbs[i] = Double.MAX_VALUE/probs.length; 
		//Account for probability values of 0 - to avoid divide-by-zero errors
		//Divide by probs.length to make sure normalizing works properly
	    }else{
		invProbs[i] = 1.0 / probs[i];
	    }
	}
	Utils.normalize(invProbs);
	double []cdf = new double[invProbs.length];
	//Compute cumulative probabilities 
	cdf[0] = invProbs[0];
	for(int i=1; i<invProbs.length; i++){
	    cdf[i] = invProbs[i]+cdf[i-1];
	}
	
	if(Double.isNaN(cdf[invProbs.length-1]))
	    System.err.println("Cumulative class membership probability is NaN!"); 
	return selectIndexProbabilistically(cdf);
    }
    
    /** 
     * Given cumulative probabilities select a nominal attribute value index 
     *
     * @param cdf array of cumulative probabilities
     * @return index of attribute selected based on the probability distribution 
     */
    protected int selectIndexProbabilistically(double []cdf){
	double rnd = m_Random.nextDouble();
	int index = 0;
	while(index < cdf.length && rnd > cdf[index]){
	    index++;
	}
	return index;
    } 
    
    /**
     * Removes a specified number of instances from the given set of instances.
     *
     * @param data given instances
     * @param numRemove number of instances to delete from the given instances
     */
    protected void removeInstances(Instances data, int numRemove){
	int num = data.numInstances();
	for(int i=num - 1; i>num - 1 - numRemove;i--){
	    data.delete(i);
	}
    }
    
    /**
     * Add new instances to the given set of instances.
     *
     * @param data given instances
     * @param newData set of instances to add to given instances
     */
    protected void addInstances(Instances data, Instances newData){
	for(int i=0; i<newData.numInstances(); i++)
	    data.add(newData.instance(i));
    }
    
    /** 
     * Computes the error in classification on the given data.
     *
     * @param data the instances to be classified
     * @return classification error
     * @throws Exception if error can not be computed successfully
     */
    protected double computeError(Instances data) throws Exception {
	double error = 0.0;
	int numInstances = data.numInstances();
	Instance curr;
	
	for(int i=0; i<numInstances; i++){
	    curr = data.instance(i);
	    //Check if the instance has been misclassified
	    if(curr.classValue() != ((int) classifyInstance(curr))) error++;
	}
	return (error/numInstances);
    }
    
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
      if (instance.classAttribute().isNumeric()) {
	  throw new UnsupportedClassTypeException("Decorate can't handle a numeric class!");
      }
      double [] sums = new double [instance.numClasses()], newProbs; 
      Classifier curr;
      
      for (int i = 0; i < m_Committee.size(); i++) {
	  curr = (Classifier) m_Committee.get(i);
	  newProbs = curr.distributionForInstance(instance);
	  for (int j = 0; j < newProbs.length; j++)
	    sums[j] += newProbs[j];
      }
      if (Utils.eq(Utils.sum(sums), 0)) {
	  return sums;
      } else {
	  Utils.normalize(sums);
	  return sums;
      }
  }
    
    /**
     * Returns description of the Decorate classifier.
     *
     * @return description of the Decorate classifier as a string
     */
    public String toString() {
	
	if (m_Committee == null) {
	    return "Decorate: No model built yet.";
	}
	StringBuffer text = new StringBuffer();
	text.append("Decorate base classifiers: \n\n");
	for (int i = 0; i < m_Committee.size(); i++)
	    text.append(((Classifier) m_Committee.get(i)).toString() + "\n\n");
	text.append("Number of classifier in the ensemble: "+m_Committee.size()+"\n");
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
    
    /**
     * Main method for testing this class.
     *
     * @param argv the options
     */
    public static void main(String [] argv) {
        runClassifier(new Decorate(), argv);
    }
}
