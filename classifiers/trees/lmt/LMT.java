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
 *    LMT.java
 *    Copyright (C) 2003 Niels Landwehr
 *
 */

package weka.classifiers.trees.lmt;

import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.trees.j48.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.supervised.attribute.NominalToBinary;

/**
 * Class for "logistic model tree" classifier.
 * For more information, see master thesis "Logistic Model Trees" (Niels Landwehr, 2003)<p>
 *
 * Valid options are: <p>
 *
 * -B <br>
 * Binary splits (convert nominal attributes to binary ones).<p>
 *
 * -R <br>
 * Split on residuals instead of class values <p>
 *
 * -C <br>
 *  Use cross-validation for boosting at all nodes (i.e., disable heuristic) <p>
 *
 * -P <br>
 * Use error on probabilities instead of misclassification error for
 * stopping criterion of LogitBoost. <p>
 *
 * -I iterations <br>
 * Set fixed number of iterations for LogitBoost (instead of using cross-validation). <p>
 *
 * -M numInstances <br>
 *
 * Set minimum number of instances at which a node can be split (default 15)
 *
 *
 * @author Niels Landwehr 
 * @version $Revision: 1.1 $
 */


public class LMT extends DistributionClassifier implements OptionHandler, AdditionalMeasureProducer,
Drawable{
    
    //format of serial: 1**date## (** = algorithm id, ##= version)
    static final long serialVersionUID = 1010506200300L;
    
    /** Filter to replace missing values*/
    protected ReplaceMissingValues m_replaceMissing;
    
    /** Filter to replace nominal attributes*/
    protected NominalToBinary m_nominalToBinary;
    
    /** root of the logistic model tree*/
    protected LMTNode m_tree;
    
    /** use heuristic that determines the number of LogitBoost iterations only once in the beginning?*/
    protected boolean m_fastRegression;

    /** convert nominal attributes to binary ?*/
    protected boolean m_convertNominal;

    /** split on residuals?*/
    protected boolean m_splitOnResiduals;
    
    /**use error on probabilties instead of misclassification for stopping criterion of LogitBoost?*/
    protected boolean m_errorOnProbabilities;

    /**minimum number of instances at which a node is considered for splitting*/
    protected int m_minNumInstances;

    /**if non-zero, use fixed number of iterations for LogitBoost*/
    protected int  m_numBoostingIterations;
    
    /**
     * Creates an instance of LMT with standard options
     */
       
    public LMT() {
	m_fastRegression = true;
	m_numBoostingIterations = -1;
	m_minNumInstances = 15;
    }    

    /**
     * Builds the classifier.
     *
     * @exception Exception if classifier can't be built successfully
     */
    
    public void buildClassifier(Instances data) throws Exception{
	
     	Instances filteredData = new Instances(data);
	filteredData.deleteWithMissingClass();

	//replace missing values
	m_replaceMissing = new ReplaceMissingValues();
	m_replaceMissing.setInputFormat(filteredData);	
	filteredData = Filter.useFilter(filteredData, m_replaceMissing);	
	
	//possibly convert nominal attributes globally
	if (m_convertNominal) {	    
	    m_nominalToBinary = new NominalToBinary();
	    m_nominalToBinary.setInputFormat(filteredData);	
	    filteredData = Filter.useFilter(filteredData, m_nominalToBinary);
	}

	int minNumInstances = 2;
	
	//create ModelSelection object, either for splits on the residuals or for splits on the class value 
	ModelSelection modSelection;	
	if (m_splitOnResiduals) {
	    modSelection = new ResidualModelSelection(minNumInstances);
	} else {
	    modSelection = new C45ModelSelection(minNumInstances, filteredData);
	}
	
	//create tree root
	m_tree = new LMTNode(modSelection, m_numBoostingIterations, m_fastRegression, 
			      m_errorOnProbabilities, m_minNumInstances);
	//build tree
	m_tree.buildClassifier(filteredData);

	if (modSelection instanceof C45ModelSelection) ((C45ModelSelection)modSelection).cleanup();
    }

    /** 
     * Returns class probabilities for an instance.
     *
     * @exception Exception if distribution can't be computed successfully
     */
    
    public double [] distributionForInstance(Instance instance) throws Exception {
	
	//replace missing values
	m_replaceMissing.input(instance);
	instance = m_replaceMissing.output();	
	
	//possibly convert nominal attributes
	if (m_convertNominal) {
	    m_nominalToBinary.input(instance);
	    instance = m_nominalToBinary.output();
	}
	
	return m_tree.distributionForInstance(instance);
    }

    /**
     * Classifies an instance.
     *
     * @exception Exception if instance can't be classified successfully
     */
   public double classifyInstance(Instance instance) throws Exception {

       double maxProb = -1;
       int maxIndex = 0;
      
       //classify by maximum probability
       double[] probs = distributionForInstance(instance);       
       for (int j = 0; j < instance.numClasses(); j++) {
	   if (Utils.gr(probs[j], maxProb)) {
	       maxIndex = j;
	       maxProb = probs[j];
	   }
       }     
       return (double)maxIndex;      
   }    
     
    /**
     * Returns a description of the classifier.
     */
    public String toString() {
	if (m_tree!=null) {
	    return "Logistic model tree \n------------------\n" + m_tree.toString();
	} else {
	    return "No tree build";
	}
    }    

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(8);
  	
	newVector.addElement(new Option("\tBinary splits (convert nominal attributes to binary ones)\n", 
					"B", 0, "-B"));

	newVector.addElement(new Option("\tSplit on residuals instead of class values\n", 
					"R", 0, "-R"));

	newVector.addElement(new Option("\tUse cross-validation for boosting at all nodes (i.e., disable heuristic)\n", 
					"C", 0, "-C"));
		
	newVector.addElement(new Option("\tUse error on probabilities instead of misclassification error "+
					"for stopping criterion of LogitBoost.\n", 
					"P", 0, "-P"));

	newVector.addElement(new Option("\tSet fixed number of iterations for LogitBoost (instead of using "+
					"cross-validation)\n",
					"I",1,"-I <numIterations>"));
	
	newVector.addElement(new Option("\tSet minimum number of instances at which a node can be split (default 15)\n",
					"M",1,"-M <numInstances>"));
	return newVector.elements();
    } 
    
    /**
     * Parses a given list of options.
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {

	setConvertNominal(Utils.getFlag('B', options));
	setSplitOnResiduals(Utils.getFlag('R', options));
	setFastRegression(!Utils.getFlag('C', options));
	setErrorOnProbabilities(Utils.getFlag('P', options));

	String optionString = Utils.getOption('I', options);
	if (optionString.length() != 0) {
	    setNumBoostingIterations((new Integer(optionString)).intValue());
	}
	
	optionString = Utils.getOption('M', options);
	if (optionString.length() != 0) {
	    setMinNumInstances((new Integer(optionString)).intValue());
	}
	
	Utils.checkForRemainingOptions(options);
	
    } 
    
    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {
	String[] options = new String[6];
	int current = 0;

	if (getConvertNominal()) {
	    options[current++] = "-B";
	} 

	if (getSplitOnResiduals()) {
	    options[current++] = "-R";
	}

	if (!getFastRegression()) {
	    options[current++] = "-C";
	} 
	
	if (getErrorOnProbabilities()) {
	    options[current++] = "-P";
	} 
	
	options[current++] = "-I"; 
	options[current++] = ""+getNumBoostingIterations();

	options[current++] = "-M"; 
	options[current++] = ""+getMinNumInstances();
	
	while (current < options.length) {
	    options[current++] = "";
	} 
	return options;
    } 

    /**
     * Get the value of convertNominal.
     *
     * @return Value of convertNominal.
     */
    public boolean getConvertNominal(){
	return m_convertNominal;
    }

    /**
     * Get the value of splitOnResiduals.
     *
     * @return Value of splitOnResiduals.
     */
    public boolean getSplitOnResiduals(){
	return m_splitOnResiduals;
    }

    /**
     * Get the value of fastRegression.
     *
     * @return Value of fastRegression.
     */
    public boolean getFastRegression(){
	return m_fastRegression;
    }
    
    /**
     * Get the value of errorOnProbabilities.
     *
     * @return Value of errorOnProbabilities.
     */
    public boolean getErrorOnProbabilities(){
	return m_errorOnProbabilities;
    }

    /**
     * Get the value of numBoostingIterations.
     *
     * @return Value of numBoostingIterations.
     */
    public int getNumBoostingIterations(){
	return m_numBoostingIterations;
    }
    
    /**
     * Get the value of minNumInstances.
     *
     * @return Value of minNumInstances.
     */
    public int getMinNumInstances(){
	return m_minNumInstances;
    }
    
    /**
     * Set the value of convertNominal.
     *
     * @param c Value to assign to convertNominal.
     */
    public void setConvertNominal(boolean c){
	m_convertNominal = c;
    }

    /**
     * Set the value of splitOnResiduals.
     *
     * @param c Value to assign to splitOnResiduals.
     */
    public void setSplitOnResiduals(boolean c){
     	m_splitOnResiduals = c;
    }

    /**
     * Set the value of fastRegression.
     *
     * @param c Value to assign to fastRegression.
     */
    public void setFastRegression(boolean c){
	m_fastRegression = c;
    }

    /**
     * Set the value of errorOnProbabilities.
     *
     * @param c Value to assign to errorOnProbabilities.
     */
    public void setErrorOnProbabilities(boolean c){
	m_errorOnProbabilities = c;
    }

    /**
     * Set the value of numBoostingIterations.
     *
     * @param c Value to assign to numBoostingIterations.
     */
    public void setNumBoostingIterations(int c){
	m_numBoostingIterations = c;
    } 

    /**
     * Set the value of minNumInstances.
     *
     * @param c Value to assign to minNumInstances.
     */
    public void setMinNumInstances(int c){
	m_minNumInstances = c;
    }
    
    /**
     * Returns graph describing the tree.
     *
     * @exception Exception if graph can't be computed
     */
    public String graph() throws Exception {

	return m_tree.graph();
    }

    /**
     * Returns the size of the tree
     * @return the size of the tree
     */
    public int measureTreeSize(){
	return m_tree.numNodes();
    }
    
    /**
     * Returns the number of leaves in the tree
     * @return the number of leaves in the tree
     */
    public int measureNumLeaves(){
	return m_tree.numLeaves();
    }
     
    /**
     * Returns an enumeration of the additional measure names
     * @return an enumeration of the measure names
     */
    public Enumeration enumerateMeasures() {
	Vector newVector = new Vector(2);
	newVector.addElement("measureTreeSize");
	newVector.addElement("measureNumLeaves");
	
	return newVector.elements();
    }
    

    /**
     * Returns the value of the named measure
     * @param additionalMeasureName the name of the measure to query for its value
     * @return the value of the named measure
     * @exception IllegalArgumentException if the named measure is not supported
     */
    public double getMeasure(String additionalMeasureName) {
	if (additionalMeasureName.compareTo("measureTreeSize") == 0) {
	    return measureTreeSize();
	} else if (additionalMeasureName.compareTo("measureNumLeaves") == 0) {
	    return measureNumLeaves();
	} else {
	    throw new IllegalArgumentException(additionalMeasureName 
					       + " not supported (LMT)");
	}
    }    
    
    /**
     * Main method for testing this class
     *
     * @param String options 
     */
    public static void main (String [] argv) throws Exception{	
	    System.out.println(Evaluation.evaluateModel(new LMT(), argv));

    }  

}

