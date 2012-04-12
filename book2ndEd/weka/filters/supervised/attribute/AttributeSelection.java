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
 *    AttributeSelection.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.filters.supervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.attributeSelection.*;

/** 
 * Filter for doing attribute selection.<p>
 *
 * Valid options are:<p>
 *
 * -S <"Name of search class [search options]"> <br>
 * Set search method for subset evaluators. <br>
 * eg. -S "weka.attributeSelection.BestFirst -S 8" <p>
 *
 * -E <"Name of attribute/subset evaluation class [evaluator options]"> <br>
 * Set the attribute/subset evaluator. <br>
 * eg. -E "weka.attributeSelection.CfsSubsetEval -L" <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class AttributeSelection extends Filter
  implements SupervisedFilter, OptionHandler {

  /** the attribute selection evaluation object */
  private weka.attributeSelection.AttributeSelection m_trainSelector;

  /** the attribute evaluator to use */
  private ASEvaluation m_ASEvaluator;

  /** the search method if any */
  private ASSearch m_ASSearch;

  /** holds a copy of the full set of valid  options passed to the filter */
  private String [] m_FilterOptions;

  /** holds the selected attributes  */
  private int [] m_SelectedAttributes;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A supervised attribute filter that can be used to select " 
      + "attributes. It is very flexible and allows various search " 
      + "and evaluation methods to be combined.";
  }

  /**
   * Constructor
   */
  public AttributeSelection () {
    
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(6);

    newVector.addElement(new Option("\tSets search method for subset "
				    + "evaluators.", "S", 1,
				    "-S <\"Name of search class"
				    + " [search options]\">"));
    newVector.addElement(new Option("\tSets attribute/subset evaluator.",
				    "E", 1,
				    "-E <\"Name of attribute/subset "
				    + "evaluation class [evaluator "
				    + "options]\">"));
    
    if ((m_ASEvaluator != null) && (m_ASEvaluator instanceof OptionHandler)) {
      Enumeration enu = ((OptionHandler)m_ASEvaluator).listOptions();
      
      newVector.addElement(new Option("", "", 0, "\nOptions specific to "
	   + "evaluator " + m_ASEvaluator.getClass().getName() + ":"));
      while (enu.hasMoreElements()) {
	newVector.addElement((Option)enu.nextElement());
      }
    }
  
    if ((m_ASSearch != null) && (m_ASSearch instanceof OptionHandler)) {
      Enumeration enu = ((OptionHandler)m_ASSearch).listOptions();
    
      newVector.addElement(new Option("", "", 0, "\nOptions specific to "
	      + "search " + m_ASSearch.getClass().getName() + ":"));
      while (enu.hasMoreElements()) {
	newVector.addElement((Option)enu.nextElement());
      }
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -S <"Name of search class [search options]"> <br>
   * Set search method for subset evaluators. <br>
   * eg. -S "weka.attributeSelection.BestFirst -S 8" <p>
   *
   * -E <"Name of attribute/subset evaluation class [evaluator options]"> <br>
   * Set the attribute/subset evaluator. <br>
   * eg. -E "weka.attributeSelection.CfsSubsetEval -L" <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String optionString;
    resetOptions();

    if (Utils.getFlag('X',options)) {
	throw new Exception("Cross validation is not a valid option"
			    + " when using attribute selection as a Filter.");
    }

    optionString = Utils.getOption('E',options);
    if (optionString.length() != 0) {
      optionString = optionString.trim();
      // split a quoted evaluator name from its options (if any)
      int breakLoc = optionString.indexOf(' ');
      String evalClassName = optionString;
      String evalOptionsString = "";
      String [] evalOptions=null;
      if (breakLoc != -1) {
	evalClassName = optionString.substring(0, breakLoc);
	evalOptionsString = optionString.substring(breakLoc).trim();
	evalOptions = Utils.splitOptions(evalOptionsString);
      }
      setEvaluator(ASEvaluation.forName(evalClassName, evalOptions));
    }

    if (m_ASEvaluator instanceof AttributeEvaluator) {
      setSearch(new Ranker());
    }

    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0) {
      optionString = optionString.trim();
      int breakLoc = optionString.indexOf(' ');
      String SearchClassName = optionString;
      String SearchOptionsString = "";
      String [] SearchOptions=null;
      if (breakLoc != -1) {
	SearchClassName = optionString.substring(0, breakLoc);
	SearchOptionsString = optionString.substring(breakLoc).trim();
	SearchOptions = Utils.splitOptions(SearchOptionsString);
      }
      setSearch(ASSearch.forName(SearchClassName, SearchOptions));
    }

    Utils.checkForRemainingOptions(options);
  }


  /**
   * Gets the current settings for the attribute selection (search, evaluator)
   * etc.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {
    String [] EvaluatorOptions = new String[0];
    String [] SearchOptions = new String[0];
    int current = 0;

    if (m_ASEvaluator instanceof OptionHandler) {
      EvaluatorOptions = ((OptionHandler)m_ASEvaluator).getOptions();
    }

    if (m_ASSearch instanceof OptionHandler) {
      SearchOptions = ((OptionHandler)m_ASSearch).getOptions();
    }

    String [] setOptions = new String [10];
    setOptions[current++]="-E";
    setOptions[current++]= getEvaluator().getClass().getName()
      +" "+Utils.joinOptions(EvaluatorOptions);

    setOptions[current++]="-S";
    setOptions[current++]=getSearch().getClass().getName() 
      + " "+Utils.joinOptions(SearchOptions);

    while (current < setOptions.length) {
      setOptions[current++] = "";
    }
    
    return setOptions;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluatorTipText() {

    return "Determines how attributes/attribute subsets are evaluated.";
  }

  /**
   * set a string holding the name of a attribute/subset evaluator
   */
  public void setEvaluator(ASEvaluation evaluator) {
    m_ASEvaluator = evaluator;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String searchTipText() {

    return "Determines the search method.";
  }

  /**
   * Set as string holding the name of a search class
   */
  public void setSearch(ASSearch search) {
    m_ASSearch = search;
  }

  /**
   * Get the name of the attribute/subset evaluator
   *
   * @return the name of the attribute/subset evaluator as a string
   */
  public ASEvaluation getEvaluator() {
    
      return m_ASEvaluator;
  }

  /**
   * Get the name of the search method
   *
   * @return the name of the search method as a string
   */
  public ASSearch getSearch() {
    
      return m_ASSearch;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been defined.
   * @exception Exception if the input instance was not of the correct format 
   * or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {
    
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (isOutputFormatDefined()) {
      convertInstance(instance);
      return true;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called
   * to retrieve the filtered instances.
   *
   * @return true if there are instances pending output.
   * @exception IllegalStateException if no input structure has been defined.
   * @exception Exception if there is a problem during the attribute selection.
   */
  public boolean batchFinished() throws Exception {
    
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (!isOutputFormatDefined()) {
      m_trainSelector.setEvaluator(m_ASEvaluator);
      m_trainSelector.setSearch(m_ASSearch);
      m_trainSelector.SelectAttributes(getInputFormat());
      //      System.out.println(m_trainSelector.toResultsString());

      m_SelectedAttributes = m_trainSelector.selectedAttributes();
      if (m_SelectedAttributes == null) {
	throw new Exception("No selected attributes\n");
      }
     
      setOutputFormat();
      
      // Convert pending input instances
      for (int i = 0; i < getInputFormat().numInstances(); i++) {
	convertInstance(getInputFormat().instance(i));
      }
      flushInput();
    }
    
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Set the output format. Takes the currently defined attribute set 
   * m_InputFormat and calls setOutputFormat(Instances) appropriately.
   */
  protected void setOutputFormat() throws Exception {
    Instances informat;

    if (m_SelectedAttributes == null) {
      setOutputFormat(null);
      return;
    }

    FastVector attributes = new FastVector(m_SelectedAttributes.length);

    int i;
    if (m_ASEvaluator instanceof AttributeTransformer) {
      informat = ((AttributeTransformer)m_ASEvaluator).transformedData();
    } else {
      informat = getInputFormat();
    }

    for (i=0;i < m_SelectedAttributes.length;i++) {
      attributes.
	addElement(informat.attribute(m_SelectedAttributes[i]).copy());
    }

    Instances outputFormat = 
      new Instances(getInputFormat().relationName(), attributes, 0);


    if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	!(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator)) {
      outputFormat.setClassIndex(m_SelectedAttributes.length - 1);
    }
    
    setOutputFormat(outputFormat);  
  }

  /**
   * Convert a single instance over. Selected attributes only are transfered.
   * The converted instance is added to the end of
   * the output queue.
   *
   * @param instance the instance to convert
   */
  protected void convertInstance(Instance instance) throws Exception {
    int index = 0;
    Instance newInstance;
    double[] newVals = new double[getOutputFormat().numAttributes()];

    if (m_ASEvaluator instanceof AttributeTransformer) {
      Instance tempInstance = ((AttributeTransformer)m_ASEvaluator).
	convertInstance(instance);
      for (int i = 0; i < m_SelectedAttributes.length; i++) {
	int current = m_SelectedAttributes[i];
	newVals[i] = tempInstance.value(current);
      }
    } else {
      for (int i = 0; i < m_SelectedAttributes.length; i++) {
	int current = m_SelectedAttributes[i];
	newVals[i] = instance.value(current);
      }
    }
    if (instance instanceof SparseInstance) {
      push(new SparseInstance(instance.weight(), newVals));
    } else {
      push(new Instance(instance.weight(), newVals));
    }
  }

  /**
   * set options to their default values
   */
  protected void resetOptions() {

    m_trainSelector = new weka.attributeSelection.AttributeSelection();
    setEvaluator(new CfsSubsetEval());
    setSearch(new BestFirst());
    m_SelectedAttributes = null;
    m_FilterOptions = null;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new AttributeSelection(), argv);
      } else {
	Filter.filterFile(new AttributeSelection(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
