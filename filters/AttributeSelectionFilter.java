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
 * Filter for doing attribute selection.<p>
 *
 * Valid options are:<p>
 *
 * -C <br>
 * Set the class index for supervised attribute selection.<p>
 *
 * -S <"Name of search class [search options]"> <br>
 * Set search method for subset evaluators. <br>
 * eg. -S "weka.attributeSelection.BestFirst -S 8" <p>
 *
 * -E <"Name of attribute/subset evaluation class [evaluator options]"> <br>
 * Set the attribute/subset evaluator. <br>
 * eg. -E "weka.attributeSelection.CfsSubsetEval -L" <p>
 *
 * -P <range> <br>
 * Specify a (optional) set of attributes to start the search from. <br>
 * eg. -P 1,2,5-9 <p>
 *
 * -T <threshold> <br>
 * Specify a threshold by which to discard attributes for attribute evaluators
 * <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */
public class AttributeSelectionFilter
  extends Filter 
  implements OptionHandler
{
  /** the attribute evaluator to use */
  private ASEvaluation m_ASEvaluator;

  /** the search method if any */
  private ASSearch m_ASSearch;

  /** holds a copy of the full set of valid  options passed to the filter */
  private String [] m_filterOptions;

  /** hold the options that will be passed on to SelectAttributes in
   * AttributeSelection
   *
   private String [] m_optionsCopy; */

  /** holds the selected attributes  */
  private int [][] m_selectedAttributes;

  /** holds the class name of the attribute/subset evaluator */
  private String m_evaluatorString;

  /** holds the class name of the search method */
  private String m_searchString;

  /** the class index (-1) if not specified */
  private int m_classIndex;
  
  /** holds the starting set of attributes if specified */
  private String m_startSet;

  /** holds the threshold by which to discard attributes 
      (attribute evaluators only) */
  private double m_threshold;

  /**
   * Constructor
   **/
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

    newVector.addElement(new Option("\tSets class index for"
				    +"\n\tsupervised attribute selection."
				    +"\n\tDefault=last column.", "C", 1,
				    "-C <class index>"));
    newVector.addElement(new Option("\tSets search method for subset "
				    +"evaluators.", "S", 1,
				    "-S <\"Name of search class"
				    +" [search options]\">"));
    newVector.addElement(new Option("\tSets attribute/subset evaluator.",
				    "E", 1,
				    "-E <\"Name of attribute/subset "
				    +"evaluation class [evaluator "
				    +"options]\">"));
    newVector.addElement(new Option("\tSpecify a (optional) set of attributes"
				    +"\n\tto start the search from, eg "
				    +"1,2,5-9.",
				    "P", 1,"-P <range>"));
    newVector.addElement(new Option("\tThreshold by which to discard "
				    +"attributes"
				    +"\n\tfor attribute evaluators.",
				    "T",1,"-T <threshold>"));

    if ((m_ASEvaluator != null) && (m_ASEvaluator instanceof OptionHandler))
      {
	Enumeration enum = ((OptionHandler)m_ASEvaluator).listOptions();

	newVector.addElement(new Option("","",0,"\nOptions specific to "+
			     "evaluator "+m_ASEvaluator.getClass().getName()
					+":"));
	while (enum.hasMoreElements())
	  {
	    newVector.addElement((Option)enum.nextElement());
	  }
      }
  
  if ((m_ASSearch != null) && (m_ASSearch instanceof OptionHandler))
      {
	Enumeration enum = ((OptionHandler)m_ASSearch).listOptions();

	newVector.addElement(new Option("","",0,"\nOptions specific to "+
			     "search "+m_ASSearch.getClass().getName()+":"));
	while (enum.hasMoreElements())
	  {
	    newVector.addElement((Option)enum.nextElement());
	  }
      }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   * -C <br>
   * Set the class index for supervised attribute selection.<p>
   *
   * -S <"Name of search class [search options]"> <br>
   * Set search method for subset evaluators. <br>
   * eg. -S "weka.attributeSelection.BestFirst -S 8" <p>
   *
   * -E <"Name of attribute/subset evaluation class [evaluator options]"> <br>
   * Set the attribute/subset evaluator. <br>
   * eg. -E "weka.attributeSelection.CfsSubsetEval -L" <p>
   *
   * -P <range> <br>
   * Specify a (optional) set of attributes to start the search from. <br>
   * eg. -P 1,2,5-9 <p>
   *
   * -T <threshold> <br>
   * Specify a threshold by which to discard attributes for attribute 
   * evaluators. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception
  {
    String optionString;
    resetOptions();

    if (Utils.getFlag('X',options))
      {
	throw new Exception("Cross validation is not a valid option"
			    +" when using attribute selection as a Filter.");
      }

    optionString = Utils.getOption('C',options);
    if (optionString.length()!=0)
      setClassIndex(Integer.parseInt(optionString));

    optionString = Utils.getOption('P',options);
    if (optionString.length() != 0)
      setStartSet(optionString);

    optionString = Utils.getOption('T',options);
    if (optionString.length() != 0)
      {
	Double temp;
	temp = Double.valueOf(optionString);
	setThreshold(temp.doubleValue());
      }
    
    /* save the options of the filter
    if (options != null) 
      {
	m_optionsFilter = new String[options.length];
	System.arraycopy(options, 0, m_optionsFilter, 0, options.length);
	} */

    optionString = Utils.getOption('E',options);
    if (optionString.length() != 0)
      setEvaluator(optionString);

    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0)
      setSearch(optionString);

    makeOptions();
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Assembles a set of options from set member variables. Checks
   * that options for evaluators and search methods are valid.
   **/
  private void makeOptions() throws Exception
  {
    String optionString;
    String [] SearchOptions = null;
    String [] evalOptions = null;
    boolean noEvaluator = false;
    boolean noSearch = false;
    int breakLoc;
    int current=0;
    int current2=0;
  
    m_filterOptions = new String[14];

    if (m_classIndex != -1)
      {
	m_filterOptions[current++] = "-C";
	m_filterOptions[current++] = ""+m_classIndex;
      }
    
    if (m_startSet != null)
      {
	m_filterOptions[current++] = "-P";
	m_filterOptions[current++] = ""+m_startSet;
      }
    
    if (m_threshold != Double.MAX_VALUE)
      {
	m_filterOptions[current++] = "-T";
	m_filterOptions[current++] = ""+m_threshold;
      }

    if (m_evaluatorString==null)
      {
	noEvaluator = true;
	//	throw new Exception("No attribute/subset evaluator given.");
      }
    else
      {
	optionString = new String(m_evaluatorString);
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

	m_ASEvaluator = 
	  (ASEvaluation)Class.forName(evalClassName).newInstance();

	// see if any evaluator options are valid
	// Try setting options for m_ASEvaluator
	if (m_ASEvaluator instanceof OptionHandler)
	  {
	    if (evalOptions != null)
	      {
		String [] evalOptionsCopy = new String[evalOptions.length];
		System.arraycopy(evalOptions,0,evalOptionsCopy,0,
				 evalOptions.length);

		((OptionHandler)m_ASEvaluator).setOptions(evalOptionsCopy);
		Utils.checkForRemainingOptions(evalOptionsCopy);
		//m_filterOptions[current++]=""+evalOptionsString;
	      }
	  }
	 
	// append to the filter options
	// m_filterOptions[current++]="-E";
	// m_filterOptions[current++]=""+m_evaluatorString;
      }
  

    // set up a dummy search object (if necessary) so help can be printed
    
    if ((m_searchString == null) &&
	(!(m_ASEvaluator instanceof 
	   AttributeEvaluator)))
      {
	noSearch = true;
      }
  
    if ((m_searchString !=null)
      && (m_ASEvaluator instanceof AttributeEvaluator))
       {
	 throw new Exception("Can't specify search method for "
				 +"attribute evaluators.");
       }

     if (!(m_ASEvaluator instanceof AttributeEvaluator))
       {
	 optionString = new String(m_searchString);
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

	 m_ASSearch = (ASSearch)Class.forName(SearchClassName).newInstance();

	 // see if any search options are valid
	 if (m_ASSearch instanceof OptionHandler)
	   {
	     if (SearchOptions != null)
	       {
		 ((OptionHandler)m_ASSearch).setOptions(SearchOptions);
		 Utils.checkForRemainingOptions(SearchOptions);
	       }
	   }
	 
	 // append to the filter options
	 m_filterOptions[current++]="-S";
	 m_filterOptions[current++]=""+m_searchString;
       }

     if (noEvaluator)
       throw new Exception("No attribute/subset evaluator given.");
     
     if (noSearch)
       throw new Exception("No search method specified.");

     if (evalOptions != null)
       for (int i=0;i<evalOptions.length;i++)
	 m_filterOptions[current++] = ""+evalOptions[i];

     while (current < m_filterOptions.length) 
       {
	 m_filterOptions[current++] = "";
       }
  }


  /**
   * Gets the current settings for the attribute selection (search, evaluator)
   * etc.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] setOptions = new String [14];
    int current = 0;

     if (m_classIndex != -1)
      {
	setOptions[current++] = "-C";
	setOptions[current++] = ""+m_classIndex;
      }

     setOptions[current++]="-E";
     setOptions[current++]=""+m_evaluatorString;

     setOptions[current++]="-S";
     setOptions[current++]=""+m_searchString;

     setOptions[current++] = "-P";
     setOptions[current++] = ""+m_startSet;

     setOptions[current++] = "-T";
     setOptions[current++] = ""+m_threshold;

     while (current < setOptions.length) 
       {
	 setOptions[current++] = "";
       }

     return setOptions;
  }

  /**
   * Set the class index
   */
  public void setClassIndex(int c)
  {
    m_classIndex = c;
  }

  /**
   * Set the starting set
   */
  public void setStartSet(String startSet)
  {
    if (startSet.length() != 0)
      {
	m_startSet = new String(startSet);
      }
  }

  /**
   * Set a threshold by which to discard attributes
   */
  public void setThreshold(double t)
  {
    m_threshold = t;
  }

  /**
   * set a string holding the name of a attribute/subset evaluator
   */
  public void setEvaluator(String evString)
  {
    if (evString.length() != 0)
      {
	m_evaluatorString = new String(evString);
      }
  }

  /**
   * Set as string holding the name of a search class
   */
  public void setSearch(String searchString)
  {
    if (searchString.length() != 0)
      {
	m_searchString = new String(searchString);
      }
  }

  /**
   * Get the threshold
   * @return a threshold as a double
   */
  public double getThreshold()
  {
    return m_threshold;
  }

  /**
   * Get the start set
   * @return a starting set of features as a string
   */
  public String getStartSet()
  {
    return m_startSet;
  }

  /**
   * Get the class index
   * @return the class index as an int
   */
  public int getClassIndex()
  {
    return m_classIndex;
  }

  /**
   * Get the name of the attribute/subset evaluator
   * @return the name of the attribute/subset evaluator as a string
   */
  public String getEvaluator() throws Exception
  {
      return m_evaluatorString;
  }

  /**
   * Get the name of the search method
   * @return the name of the search method as a string
   */
  public String getSearch() throws Exception
  {
      return m_searchString;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct format 
   * or if there was a problem with the filtering.
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

    if (m_selectedAttributes[0] != null)
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

    if (m_selectedAttributes[0] == null)
      {
	makeOptions();
	// for (int i=0;i<m_filterOptions.length;i++)
	// System.out.println(m_filterOptions[i]);
	AttributeSelection.SelectAttributes(m_ASEvaluator, m_filterOptions,
					m_selectedAttributes, m_InputFormat);

	if (m_selectedAttributes[0] == null)
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
    
    if (m_selectedAttributes[0] == null)
    {
      setOutputFormat(null);
      return;
    }

    FastVector attributes = new FastVector(m_selectedAttributes[0].length);

    int i;
    for (i=0;i < m_selectedAttributes[0].length;i++)
      attributes.
	addElement(m_InputFormat.attribute(m_selectedAttributes[0][i]).copy());

    Instances outputFormat = 
    new Instances(m_InputFormat.relationName(), attributes, 0);

    if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) &&
	!(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator))
      try {
	outputFormat.setClassIndex(m_selectedAttributes[0].length-1);
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
    
    for (int i = 0; i < m_selectedAttributes[0].length; i++)
    {
      newInstance.setValue(index, instance.value(m_selectedAttributes[0][i]));
      index++;
    }

    // set the weight
    newInstance.setWeight(instance.weight());
    push(newInstance);
  }

  protected void resetOptions()
  {
    m_ASEvaluator = null;
    m_ASSearch = null;
    //m_optionsCopy = null;
    m_selectedAttributes = new int [1][0];
    m_selectedAttributes[0]=null;
    m_filterOptions = null;
    m_evaluatorString = null;
    m_searchString = null;
    m_classIndex=-1;
    m_startSet = null;
    m_threshold = Double.MAX_VALUE; // no threshold
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
