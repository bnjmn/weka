/*
 *    AttributeSelectionFilter.java
 *    Copyright (C) 1999 Mark Hall
 *
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
package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.attributeSelection.*;

/** 
 * Filter for doing attribute selection.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */
public class AttributeSelectionFilter
  extends Filter 
  implements OptionHandler
{
  // the attribute evaluator to use
  private ASEvaluation ASEvaluator;

  // the search method if any
  private ASSearch ASSearch;

  // holds a copy of the full set of options passed to the filter
  private String [] optionsFilter;

  // hold the options that will be passed on to SelectAttributes in
  // AttributeSelection
  private String [] optionsCopy;

  // holds the selected attributes 
  private int [][] selectedAttributes;

  public AttributeSelectionFilter ()
  {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions() 
  {
    
    Vector newVector = new Vector(7);

    newVector.addElement(new Option("<class index>\n"
				    +"\tSets class index for"
				    +"\n\tsupervised attribute selection."
				    +"\n\tDefault=last column.", "C", 1,"-C"));
    newVector.addElement(new Option("<\"Name of search Class "
				    +"[search options]\">\n"
				    +"\tSets search method for subset "
				    +"evaluators.", "S", 1,"-S"));
    newVector.addElement(new Option("<\"Name of attribute/subset evaluation "
				    +"Class [evaluation options]\">\n"
				    +"\tSets attribute/subset evaluator.",
				    "E", 1,"-E"));
    newVector.addElement(new Option("<range>\n"
				    +"\tSpecify a (optional) set of attributes"
				    +"\n\tto start the search from, eg 1,2,5-9.",
				    "P", 1,"-P"));
    newVector.addElement(new Option("Produce a attribute ranking if the"
				    +"\n\tspecified search method is capable of"
				    +"\n\tdoing so.", "R", 0,"-R"));
    newVector.addElement(new Option("Threshold by which to discard attributes"
				    +"\n\tfor attribute evaluators.",
				    "T",1,"-T"));

    if ((ASEvaluator != null) && (ASEvaluator instanceof OptionHandler))
      {
	Enumeration enum = ((OptionHandler)ASEvaluator).listOptions();

	newVector.addElement(new Option("","",0,"\nOptions specific to "+
			     "evaluator "+ASEvaluator.getClass().getName()
					+":"));
	while (enum.hasMoreElements())
	  {
	    newVector.addElement((Option)enum.nextElement());
	  }
      }
  
  if ((ASSearch != null) && (ASSearch instanceof OptionHandler))
      {
	Enumeration enum = ((OptionHandler)ASSearch).listOptions();

	newVector.addElement(new Option("","",0,"\nOptions specific to "+
			     "search "+ASSearch.getClass().getName()+":"));
	while (enum.hasMoreElements())
	  {
	    newVector.addElement((Option)enum.nextElement());
	  }
      }
    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception
  {
    String optionString;
    String [] SearchOptions = null;
    String [] evalOptions = null;
    boolean noEvaluator = false;
    boolean noSearch = false;
    int breakLoc;
    resetOptions();

    if (Utils.getFlag('X',options))
      {
	throw new Exception("Cross validation is not a valid option"
			    +" when using attribute selection as a Filter.");
      }

    // save the options of the filter
    if (options != null) 
      {
	optionsFilter = new String[options.length];
	System.arraycopy(options, 0, optionsFilter, 0, options.length);
      }

    optionString = Utils.getOption('E',options);
    if (optionString.length()==0)
      {
	noEvaluator = true;
	//	throw new Exception("No attribute/subset evaluator given.");
      }
    else
      {
	optionString = optionString.trim();
	// split a quoted evaluator name from its options (if any)
	breakLoc = optionString.indexOf(' ');
	String evalClassName = optionString;
	String evalOptionsString = "";
	if (breakLoc != -1) {
	  evalClassName = optionString.substring(0, breakLoc);
	  evalOptionsString = optionString.substring(breakLoc).trim();
	  evalOptions = Utils.splitOptions(evalOptionsString);
	}

	ASEvaluator = (ASEvaluation)Class.forName(evalClassName).newInstance();

	// save the options to be passed on
	if (options != null) 
	  {
	    if (evalOptions != null)
	      optionsCopy = new String[options.length+evalOptions.length];
	    else
	      optionsCopy = new String[options.length];
	    System.arraycopy(options, 0, optionsCopy, 0, options.length);
	    
	    // append any evaluator options to the array
	    if (evalOptions != null)
	      System.arraycopy(evalOptions, 0, 
			       optionsCopy, options.length, 
			       evalOptions.length); 
	  }
      }

    // set up a dummy search object (if necessary) so help can be printed
    optionString = Utils.getOption('S',options);
     if ((optionString.length()==0) &&
	 (!(ASEvaluator instanceof AttributeEvaluator)))
      {
	noSearch = true;
      }

     if ((optionString.length() !=0)
	 && (ASEvaluator instanceof AttributeEvaluator))
       {
	 throw new Exception("Can't specify search method for "
				 +"attribute evaluators.");
       }

     if (!(ASEvaluator instanceof AttributeEvaluator))
       {
	 // split a quoted evaluator name from its options (if any)
	 optionString = optionString.trim();
	 breakLoc = optionString.indexOf(' ');
	 String SearchClassName = optionString;
	 String SearchOptionsString = "";
	 if (breakLoc != -1) 
	   {
	     SearchClassName = optionString.substring(0, breakLoc);
	     SearchOptionsString = optionString.substring(breakLoc).trim();
	     SearchOptions = Utils.splitOptions(SearchOptionsString);
	   }

	 ASSearch = (ASSearch)Class.forName(SearchClassName).newInstance();
       }

     if (!noEvaluator)
       {
	 // Try setting options for ASEvaluator
	 if (ASEvaluator instanceof OptionHandler)
	   {
	     ((OptionHandler)ASEvaluator).setOptions(evalOptions);
	   }
	 Utils.checkForRemainingOptions(evalOptions);
       }
    
     if (!noSearch)
       {
	 // Try setting options for search method
	 if (ASSearch instanceof OptionHandler)
	   {
	     if (SearchOptions != null)
	       ((OptionHandler)ASSearch).setOptions(SearchOptions);
	   }
	 Utils.checkForRemainingOptions(SearchOptions);
       }

     if (noEvaluator)
       throw new Exception("No attribute/subset evaluator given.");

     if (noSearch)
       throw new Exception("No search method specified.");

    for (int i=0;i<options.length;i++)
      options[i] = "";
  }

  /**
   * Gets the current settings for the attribute selection (search, evaluator)
   * etc.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    return optionsCopy;
  }


  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct format or
   * if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception
  {
    if (m_InputFormat == null)
      throw new Exception("No input instance format defined");

    if (b_NewBatch)
    {
      resetQueue();
      b_NewBatch = false;
    }

    if (selectedAttributes[0] != null)
    {
      convertInstance(instance);
      return true;
    }

    m_InputFormat.add(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called
   * to retrieve the filtered instances.
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception
  {
    if (m_InputFormat == null)
      throw new Exception("No input instance format defined");

    if (selectedAttributes[0] == null)
      {
	AttributeSelection.SelectAttributes(ASEvaluator, optionsCopy,
					selectedAttributes, m_InputFormat);

	if (selectedAttributes[0] == null)
	  throw new Exception("No selected attributes\n");

	setOutputFormat();

	// Convert pending input instances
	Instance current;
	for(int i = 0; i < m_InputFormat.numInstances(); i++)
	  {
	    current = m_InputFormat.instance(i);
	    convertInstance(current);
	  }
	m_InputFormat = new Instances(m_InputFormat, 0);
      }

    b_NewBatch = true;
    return (numPendingOutput() != 0);
  }

   /**
   * Set the output format. Takes the currently defined attribute set 
   * m_InputFormat and calls setOutputFormat(Instances) appropriately.
   */
  protected void setOutputFormat()
  {
    
    if (selectedAttributes[0] == null)
    {
      setOutputFormat(null);
      return;
    }

    FastVector attributes = new FastVector(selectedAttributes[0].length);

    int i;
    for (i=0;i < selectedAttributes[0].length;i++)
      attributes.
	addElement(m_InputFormat.attribute(selectedAttributes[0][i]).copy());

    Instances outputFormat = 
    new Instances(m_InputFormat.relationName(), attributes, 0);

    if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	!(ASEvaluator instanceof UnsupervisedAttributeEvaluator))
      try {
	outputFormat.setClassIndex(selectedAttributes[0].length-1);
      }
    catch (Exception e)
      {
	System.err.println(e.toString()+"\nProblem setting new output format");
	System.exit(0);
      }
    
    setOutputFormat(outputFormat);  
  }

  /**
   * Convert a single instance over. Selected attributes only are transfered.
   * The converted instance is added to the end of
   * the output queue. 
   * @param instance the instance to convert
   */
  protected void convertInstance(Instance instance) throws Exception
  {
    int index = 0;
    Instance newInstance = new Instance(outputFormatPeek().numAttributes());
    
    for (int i = 0; i < selectedAttributes[0].length; i++)
    {
      newInstance.setValue(index, instance.value(selectedAttributes[0][i]));
      index++;
    }

    // set the weight
    newInstance.setWeight(instance.weight());
    push(newInstance);
  }

  protected void resetOptions()
  {
    ASEvaluator = null;
    ASSearch = null;
    optionsCopy = null;
    selectedAttributes = new int [1][0];
    selectedAttributes[0]=null;
  }

  // ============
  // Test method.
  // ============
  
  /**
   * Main method for testing this class.
   * @param argv should contain arguments to the filter: use -h for help
   */

  public static void main(String [] argv)
  {
    try
    {
      if (Utils.getFlag('b', argv))
 	Filter.batchFilterFile(new AttributeSelectionFilter(),argv);
      else
	Filter.filterFile(new AttributeSelectionFilter(),argv);
    }

    catch (Exception ex)
    {
      System.out.println(ex.getMessage());
    }
  }
}
