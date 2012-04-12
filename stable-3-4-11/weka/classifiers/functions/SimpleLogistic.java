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
 *    SimpleLogistic.java
 *    Copyright (C) 2003 Niels Landwehr
 *
 */

package weka.classifiers.functions;

import weka.classifiers.*;
import weka.classifiers.trees.lmt.LogisticBase;
import weka.core.*;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;
import java.util.*;

/**
 * Class for building a logistic regression model using LogitBoost.
 * Incorporates attribute selection by fitting simple regression functions in LogitBoost.
 * For more information, see master thesis "Logistic Model Trees" (Niels Landwehr, 2003)<p>
 *
 * Valid options are: <p>
 *
 * -I iterations <br>
 * Set fixed number of iterations for LogitBoost (instead of using cross-validation). <p>
 * -S <br>
 * Select the number of LogitBoost iterations that gives minimal error on the training set 
 * (instead of using cross-validation). <p>
 * -P <br>
 * Minimize error on probabilities instead of misclassification error. <p>
 * -M iterations <br>
 * Set maximum number of iterations for LogitBoost. <p>
 * -H iter <br>
 * Set parameter for heuristic for early stopping of LogitBoost.
 * If enabled, the minimum is selected greedily, stopping if the current minimum has not changed 
 * for iter iterations. By default, heuristic is enabled with value 50. Set to zero to disable heuristic.
 *
 * @author Niels Landwehr 
 * @version $Revision: 1.5.2.1 $
 */

public class SimpleLogistic extends Classifier 
  implements OptionHandler, AdditionalMeasureProducer, WeightedInstancesHandler {

  //format of serial: 1**date## (** = algorithm id, ##= version)
  //static final long serialVersionUID = 1110506200300L;
    
    /**The actual logistic regression model */
    protected LogisticBase m_boostedModel;
    
    /**Filter for converting nominal attributes to binary ones*/
    protected NominalToBinary m_NominalToBinary = null;

    /**Filter for replacing missing values*/
    protected ReplaceMissingValues m_ReplaceMissingValues = null;
    
    /**If non-negative, use this as fixed number of LogitBoost iterations*/ 
    protected int m_numBoostingIterations;
    
    /**Maximum number of iterations for LogitBoost*/
    protected int m_maxBoostingIterations = 500;
    
    /**Parameter for the heuristic for early stopping of LogitBoost*/
    protected int m_heuristicStop = 50;

    /**If true, cross-validate number of LogitBoost iterations*/
    protected boolean m_useCrossValidation;

    /**If true, use minimize error on probabilities instead of misclassification error*/
    protected boolean m_errorOnProbabilities;

    /**
     * Constructor for creating SimpleLogistic object with standard options.
     */
    public SimpleLogistic() {
	m_numBoostingIterations = 0;
	m_useCrossValidation = true;
	m_errorOnProbabilities = false;
    }

    /**
     * Constructor for creating SimpleLogistic object.
     * @param numBoostingIterations if non-negative, use this as fixed number of iterations for LogitBoost
     * @param useCrossValidation cross-validate number of LogitBoost iterations.
     * @param errorOnProbabilities minimize error on probabilities instead of misclassification error
     */
    public SimpleLogistic(int numBoostingIterations, boolean useCrossValidation, 
			      boolean errorOnProbabilities) { 
  	m_numBoostingIterations = numBoostingIterations;
	m_useCrossValidation = useCrossValidation;
	m_errorOnProbabilities = errorOnProbabilities;
    }

    /**
     * Builds the logistic regression using LogitBoost.
     * @param data the training data
     * @exception Exception if something goes wrong 
     */
    public void buildClassifier(Instances data) throws Exception {

	if (data.classAttribute().type() != Attribute.NOMINAL) {
	    throw new UnsupportedClassTypeException("Class attribute must be nominal.");
	}
	if (data.checkForStringAttributes()) {
	    throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
	}

	data = new Instances(data);
	data.deleteWithMissingClass();

	if (data.numInstances() == 0) {
	  throw new Exception("No instances without missing class values in training file!");
	}

	//replace missing values
	m_ReplaceMissingValues = new ReplaceMissingValues();
	m_ReplaceMissingValues.setInputFormat(data);
	data = Filter.useFilter(data, m_ReplaceMissingValues);
	
	//convert nominal attributes
	m_NominalToBinary = new NominalToBinary();
	m_NominalToBinary.setInputFormat(data);
	data = Filter.useFilter(data, m_NominalToBinary);
	
	//create actual logistic model
	m_boostedModel = new LogisticBase(m_numBoostingIterations, m_useCrossValidation, m_errorOnProbabilities);
	m_boostedModel.setMaxIterations(m_maxBoostingIterations);
	m_boostedModel.setHeuristicStop(m_heuristicStop);
	
	//build logistic model
	m_boostedModel.buildClassifier(data);
    }
    
    /** 
     * Returns class probabilities for an instance.
     *
     * @exception Exception if distribution can't be computed successfully
     */
    public double[] distributionForInstance(Instance inst) 
	throws Exception {
	
	//replace missing values / convert nominal atts
	m_ReplaceMissingValues.input(inst);
	inst = m_ReplaceMissingValues.output();
	m_NominalToBinary.input(inst);
	inst = m_NominalToBinary.output();	
	
	//obtain probs from logistic model
	return m_boostedModel.distributionForInstance(inst);	
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
	Vector newVector = new Vector(5);
	
	newVector.addElement(new Option("\tSet fixed number of iterations for LogitBoost\n",
					"I",1,"-I <iterations>"));
	
	newVector.addElement(new Option("\tUse stopping criterion on training set (instead of cross-validation)\n",
					"S",0,"-S"));
	
	newVector.addElement(new Option("\tUse error on probabilities (rmse) instead of misclassification error " +
					"for stopping criterion\n",
					"P",0,"-P"));

	newVector.addElement(new Option("\tSet maximum number of boosting iterations\n",
					"M",1,"-M <iterations>"));

	newVector.addElement(new Option("\tSet parameter for heuristic for early stopping of LogitBoost."+
					"If enabled, the minimum is selected greedily, stopping if the current minimum"+
					" has not changed for iter iterations. By default, heuristic is enabled with"+
					"value 50. Set to zero to disable heuristic."+
					"\n",
					"H",1,"-H <iterations>"));
	return newVector.elements();
    } 
    

    /**
     * Parses a given list of options.
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {

	String optionString = Utils.getOption('I', options);
	if (optionString.length() != 0) {
	    setNumBoostingIterations((new Integer(optionString)).intValue());
	}
		
	setUseCrossValidation(!Utils.getFlag('S', options));
	setErrorOnProbabilities(Utils.getFlag('P', options));
	
	optionString = Utils.getOption('M', options);
	if (optionString.length() != 0) {
	    setMaxBoostingIterations((new Integer(optionString)).intValue());
	}

	optionString = Utils.getOption('H', options);
	if (optionString.length() != 0) {
	    setHeuristicStop((new Integer(optionString)).intValue());
	}

	Utils.checkForRemainingOptions(options);
    } 

    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {
	String[] options = new String[9];
	int current = 0;
		
	options[current++] = "-I"; 
	options[current++] = ""+getNumBoostingIterations();
	
	if (!getUseCrossValidation()) {
	    options[current++] = "-S";
	} 

	if (getErrorOnProbabilities()) {
	    options[current++] = "-P";
	} 

	options[current++] = "-M"; 
	options[current++] = ""+getMaxBoostingIterations();
	
	options[current++] = "-H"; 
	options[current++] = ""+getHeuristicStop();

	while (current < options.length) {
	    options[current++] = "";
	} 
	return options;
    } 

    /**
     * Get the value of numBoostingIterations.
     */
    public int getNumBoostingIterations(){
	return m_numBoostingIterations;
    }
    /**
     * Get the value of useCrossValidation.
     */
    public boolean getUseCrossValidation(){
	return m_useCrossValidation;
    }

    /**
     * Get the value of errorOnProbabilities.
     */
    public boolean getErrorOnProbabilities(){
	return m_errorOnProbabilities;
    }
    
    /**
     * Get the value of maxBoostingIterations.
     */
    public int getMaxBoostingIterations(){
	return m_maxBoostingIterations;
    }

    /**
     * Get the value of heuristicStop.
     */
    public int getHeuristicStop(){
	return m_heuristicStop;
    }
    
    /**
     * Set the value of numBoostingIterations.
     */
    public void setNumBoostingIterations(int n){
	m_numBoostingIterations = n;
    }

    /**
     * Set the value of useCrossValidation.
     */
    public void setUseCrossValidation(boolean l){
	m_useCrossValidation = l;
    }

    /**
     * Set the value of errorOnProbabilities.
     */
    public void setErrorOnProbabilities(boolean l){
	m_errorOnProbabilities = l;
    }

    /**
     * Set the value of maxBoostingIterations.
     */
    public void setMaxBoostingIterations(int n){
	m_maxBoostingIterations = n;
    } 

    /**
     * Set the value of heuristicStop.
     */
    public void setHeuristicStop(int n){
	if (n == 0) m_heuristicStop = m_maxBoostingIterations; else m_heuristicStop = n;
    }

    /**
     * Get the number of LogitBoost iterations performed (= the number of regression functions fit by LogitBoost).
     */
    public int getNumRegressions(){
	return m_boostedModel.getNumRegressions();
    }

    /**
     * Returns a description of the logistic model (attributes/coefficients).
     */
    public String toString(){
	if (m_boostedModel == null) return "No model built";
	return "SimpleLogistic:\n" + m_boostedModel.toString();
    }

    /**
     * Returns the fraction of all attributes in the data that are used in the logistic model (in percent).
     * An attribute is used in the model if it is used in any of the models for the different classes.
     */
    public double measureAttributesUsed(){
	return m_boostedModel.percentAttributesUsed();
    }
       
     /**
     * Returns an enumeration of the additional measure names
     * @return an enumeration of the measure names
     */
    public Enumeration enumerateMeasures() {
	Vector newVector = new Vector(3);
	newVector.addElement("measureAttributesUsed");
	newVector.addElement("measureNumIterations");
	return newVector.elements();
    }
    
    /**
     * Returns the value of the named measure
     * @param additionalMeasureName the name of the measure to query for its value
     * @return the value of the named measure
     * @exception IllegalArgumentException if the named measure is not supported
     */
    public double getMeasure(String additionalMeasureName) {
	if (additionalMeasureName.compareToIgnoreCase("measureAttributesUsed") == 0) {
	    return measureAttributesUsed();
      	} else if(additionalMeasureName.compareToIgnoreCase("measureNumIterations") == 0){
	    return getNumRegressions();
	} else {
	    throw new IllegalArgumentException(additionalMeasureName 
					       + " not supported (SimpleLogistic)");
	}
    }    


    /**
     * Returns a string describing classifier
     * @return a description suitable for
     * displaying in the explorer/experimenter gui
     */
    public String globalInfo() {
	return "Classifier for building linear logistic regression models. LogitBoost with simple regression "
	    +"functions as base learners is used for fitting the logistic models. The optimal number of LogitBoost "
	    +"iterations to perform is cross-validated, which leads to automatic attribute selection. "
	    +"For more information see: N.Landwehr, M.Hall, E. Frank 'Logistic Model Trees' (ECML 2003).";	    
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String numBoostingIterationsTipText() {
	return "Set fixed number of iterations for LogitBoost. If >= 0, this sets the number of LogitBoost iterations "
	    +"to perform. If < 0, the number is cross-validated or a stopping criterion on the training set is used "
	    +"(depending on the value of useCrossValidation).";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String useCrossValidationTipText() {
	return "Sets whether the number of LogitBoost iterations is to be cross-validated or the stopping criterion "
	    +"on the training set should be used. If not set (and no fixed number of iterations was given), "
	    +"the number of LogitBoost iterations is used that minimizes the error on the training set "
	    +"(misclassification error or error on probabilities depending on errorOnProbabilities).";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String errorOnProbabilitiesTipText() {
	return "Use error on the probabilties as error measure when determining the best number of LogitBoost iterations. "
	    +"If set, the number of LogitBoost iterations is chosen that minimizes the root mean squared error "
	    +"(either on the training set or in the cross-validation, depending on useCrossValidation).";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String maxBoostingIterationsTipText() {
	return "Sets the maximum number of iterations for LogitBoost. Default value is 500, for very small/large "
	    +"datasets a lower/higher value might be preferable.";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String heuristicStopTipText() {
	return "If heuristicStop > 0, the heuristic for greedy stopping while cross-validating the number of "
	    +"LogitBoost iterations is enabled. This means LogitBoost is stopped if no new error minimum "
	    +"has been reached in the last heuristicStop iterations. It is recommended to use this heuristic, "
	    +"it gives a large speed-up especially on small datasets. The default value is 50.";
    }    

    /**
     * Main method for testing this class
     *
     * @param String options 
     */
    public static void main(String[] argv) {	
	try {
	    System.out.println(Evaluation.evaluateModel(new SimpleLogistic(), argv));
	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println(e.getMessage());
	}
    }

}






