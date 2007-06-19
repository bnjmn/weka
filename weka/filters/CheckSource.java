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
 * CheckSource.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.filters;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A simple class for checking the source generated from Filters
 * implementing the <code>weka.filters.Sourcable</code> interface.
 * It takes a filter, the classname of the generated source
 * and the dataset the source was generated with as parameters and tests
 * the output of the built filter against the output of the generated
 * source. Use option '-h' to display all available commandline options.
 * 
 <!-- options-start -->
 <!-- options-end -->
 *
 * Options after -- are passed to the designated filter.<p>
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see     weka.filters.Sourcable
 */
public class CheckSource
  implements OptionHandler {

  /** the classifier used for generating the source code */
  protected Filter m_Filter = null;
  
  /** the generated source code */
  protected Object m_SourceCode = null;
  
  /** the method name in the generated source code used for filtering a 
   * single row */
  protected String m_MethodNameSingle = "filter";
  
  /** the method name in the generated source code used for filtering
   * multiple rows */
  protected String m_MethodNameMultiple = "filter";
  
  /** the dataset to use for testing */
  protected File m_Dataset = null;
  
  /** the class index */
  protected int m_ClassIndex = -1;
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    
    result.addElement(new Option(
        "\tThe filter (incl. options) that was used to generate\n"
        + "\tthe source code.",
        "W", 1, "-W <classname and options>"));
    
    result.addElement(new Option(
        "\tThe classname of the generated source code.",
        "S", 1, "-S <classname>"));
    
    result.addElement(new Option(
        "\tThe name of the method in the generated source used for \n"
        + "\tfiltering a single row.\n"
        + "\t(default: filter)",
        "single", 1, "-single <methodname>"));
    
    result.addElement(new Option(
        "\tThe name of the method in the generated source used for \n"
        + "\tfiltering multiple rows.\n"
        + "\t(default: filter)",
        "multiple", 1, "-multiple <methodname>"));
    
    result.addElement(new Option(
        "\tThe training set with which the source code was generated.",
        "t", 1, "-t <file>"));
    
    result.addElement(new Option(
        "\tThe class index of the training set. 'first' and 'last' are\n"
        + "\tvalid indices.\n"
        + "\t(default: last)",
        "c", 1, "-c <index>"));
    
    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   <!-- options-end -->
   *
   * Options after -- are passed to the designated filter.<p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;
    String[]    spec;
    String      classname;

    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() > 0) {
      spec = Utils.splitOptions(tmpStr);
      if (spec.length == 0)
        throw new IllegalArgumentException("Invalid filter specification string");
      classname = spec[0];
      spec[0]   = "";
      setFilter((Filter) Utils.forName(Filter.class, classname, spec));
    }
    else {
      throw new Exception("No filter (classname + options) provided!");
    }
    
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() > 0) {
      spec = Utils.splitOptions(tmpStr);
      if (spec.length != 1)
        throw new IllegalArgumentException("Invalid source code specification string");
      setSourceCode(Class.forName(spec[0]).newInstance());
    }
    else {
      throw new Exception("No source code (classname) provided!");
    }
    
    tmpStr = Utils.getOption("single", options);
    if (tmpStr.length() != 0)
      setMethodNameSingle(tmpStr);
    else
      setMethodNameSingle("filter");

    tmpStr = Utils.getOption("multiple", options);
    if (tmpStr.length() != 0)
      setMethodNameMultiple(tmpStr);
    else
      setMethodNameMultiple("filter");

    tmpStr = Utils.getOption('t', options);
    if (tmpStr.length() != 0)
      setDataset(new File(tmpStr));
    else
      throw new Exception("No dataset provided!");

    tmpStr = Utils.getOption('c', options);
    if (tmpStr.length() != 0) {
      if (tmpStr.equals("first"))
        setClassIndex(0);
      else if (tmpStr.equals("last"))
        setClassIndex(-1);
      else 
        setClassIndex(Integer.parseInt(tmpStr) - 1);
    }
    else {
      setClassIndex(-1);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>      result;
    
    result  = new Vector<String>();

    if (getFilter() != null) {
      result.add("-W");
      result.add(getFilter().getClass().getName() + " " 
          + Utils.joinOptions(((OptionHandler) getFilter()).getOptions()));
    }

    if (getSourceCode() != null) {
      result.add("-S");
      result.add(getSourceCode().getClass().getName());
    }

    result.add("-single");
    result.add(getMethodNameSingle());

    result.add("-multiple");
    result.add(getMethodNameMultiple());

    if (getDataset() != null) {
      result.add("-t");
      result.add(m_Dataset.getAbsolutePath());
    }

    result.add("-c");
    if (getClassIndex() == -1)
      result.add("last");
    else if (getClassIndex() == 0)
      result.add("first");
    else 
      result.add("" + (getClassIndex() + 1));
    
    return result.toArray(new String[result.size()]);
  }

  /**
   * Sets the filter to use for the comparison.
   * 
   * @param value       the filter to use
   */
  public void setFilter(Filter value) {
    m_Filter = value;
  }
  
  /**
   * Gets the filter being used for the tests, can be null.
   * 
   * @return            the currently set filter
   */
  public Filter getFilter() {
    return m_Filter;
  }
  
  /**
   * Sets the class to test.
   * 
   * @param value       the class to test
   */
  public void setSourceCode(Object value) {
    m_SourceCode = value;
  }
  
  /**
   * Gets the class to test.
   * 
   * @return            the currently set class, can be null.
   */
  public Object getSourceCode() {
    return m_SourceCode;
  }

  /**
   * Sets the name of the method used in the generated source to filter a 
   * single row.
   * 
   * @param value       the method name
   */
  public void setMethodNameSingle(String value) {
    m_MethodNameSingle = value;
  }
  
  /**
   * Gets the name of the method used in the generated source for filtering
   * a single row.
   * 
   * @return            the method name
   */
  public String getMethodNameSingle() {
    return m_MethodNameSingle;
  }

  /**
   * Sets the name of the method used in the generated source to filter 
   * multiple rows.
   * 
   * @param value       the method name
   */
  public void setMethodNameMultiple(String value) {
    m_MethodNameMultiple = value;
  }
  
  /**
   * Gets the name of the method used in the generated source for filtering
   * multiple rows.
   * 
   * @return            the method name
   */
  public String getMethodNameMultiple() {
    return m_MethodNameMultiple;
  }
  
  /**
   * Sets the dataset to use for testing.
   * 
   * @param value       the dataset to use.
   */
  public void setDataset(File value) {
    if (!value.exists())
      throw new IllegalArgumentException(
          "Dataset '" + value.getAbsolutePath() + "' does not exist!");
    else
      m_Dataset = value;
  }
  
  /**
   * Gets the dataset to use for testing, can be null.
   * 
   * @return            the dataset to use.
   */
  public File getDataset() {
    return m_Dataset;
  }

  /**
   * Sets the class index of the dataset.
   * 
   * @param value       the class index of the dataset.
   */
  public void setClassIndex(int value) {
    m_ClassIndex = value;
  }
  
  /**
   * Gets the class index of the dataset.
   * 
   * @return            the current class index.
   */
  public int getClassIndex() {
    return m_ClassIndex;
  }

  /**
   * turns the Instance object into an array of Objects
   * 
   * @param inst	the instance to turn into an array
   * @return		the Object array representing the instance
   */
  protected Object[] instanceToObjects(Instance inst) {
    Object[]	result;
    int		i;
    
    result = new Object[inst.numAttributes()];

    for (i = 0 ; i < inst.numAttributes(); i++) {
      if (inst.isMissing(i))
	result[i] = null;
      else if (inst.attribute(i).isNumeric())
	result[i] = inst.value(i);
      else
	result[i] = inst.stringValue(i);
    }
    
    return result;
  }

  /**
   * turns the Instances object into an array of Objects
   * 
   * @param data	the instances to turn into an array
   * @return		the Object array representing the instances
   */
  protected Object[][] instancesToObjects(Instances data) {
    Object[][]	result;
    int		i;
    
    result = new Object[data.numInstances()][];
    
    for (i = 0; i < data.numInstances(); i++)
      result[i] = instanceToObjects(data.instance(i));
    
    return result;
  }

  /**
   * compares the Instance object with the array of objects
   * 
   * @param inst	the Instance object to compare
   * @param obj		the Object array to compare with
   * @return		true if both are the same
   */
  protected boolean compare(Instance inst, Object[] obj) {
    boolean	result;
    int		i;
    Object[]	instObj;
    
    // check dimension
    result = (inst.numAttributes() == obj.length);
    
    // check content
    if (result) {
      instObj = instanceToObjects(inst);
      for (i = 0; i < instObj.length; i++) {
	if (!instObj[i].equals(obj[i])) {
	  result = false;
	  System.out.println(
	      "Values at position " + (i+1) + " differ (Filter/Source code): " 
	      + instObj[i] + " != " + obj[i]);
	  break;
	}
      }
    }
    
    return result;
  }

  /**
   * compares the Instances object with the 2-dimensional array of objects
   * 
   * @param inst	the Instances object to compare
   * @param obj		the Object array to compare with
   * @return		true if both are the same
   */
  protected boolean compare(Instances inst, Object[][] obj) {
    boolean	result;
    int		i;
    
    // check dimensions
    result = (inst.numInstances() == obj.length);

    // check content
    if (result) {
      for (i = 0; i < inst.numInstances(); i++) {
	result = compare(inst.instance(i), obj[i]);
	if (!result) {
	  System.out.println(
	      "Values in line " + (i+1) + " differ!");
	  break;
	}
      }
    }
    
    return result;
  }
  
  /**
   * performs the comparison test
   * 
   * @return            true if tests were successful
   * @throws Exception  if tests fail
   */
  public boolean execute() throws Exception {
    boolean     result;
    Instances   data;
    Instance	filteredInstance;
    Instances	filteredInstances;
    Object[]	filteredRow;
    Object[][]	filteredRows;
    DataSource  source;
    Filter	filter;
    Method	multiple;
    Method	single;
    int		i;
    
    result = true;
    
    // a few checks
    if (getFilter() == null)
      throw new Exception("No filter set!");
    if (getSourceCode() == null)
      throw new Exception("No source code set!");
    if (getDataset() == null)
      throw new Exception("No dataset set!");
    if (!getDataset().exists())
      throw new Exception(
          "Dataset '" + getDataset().getAbsolutePath() + "' does not exist!");
    
    // load data
    source = new DataSource(getDataset().getAbsolutePath());
    data   = source.getDataSet();
    if (getClassIndex() == -1)
      data.setClassIndex(data.numAttributes() - 1);
    else
      data.setClassIndex(getClassIndex());
    
    // determine the method to calls
    single = getSourceCode().getClass().getMethod(
        getMethodNameSingle(), new Class[]{Object[].class});
    multiple = getSourceCode().getClass().getMethod(
        getMethodNameMultiple(), new Class[]{Object[][].class});
    
    // compare output
    // 1. batch filtering
    filter = Filter.makeCopy(getFilter());
    filter.setInputFormat(data);
    filteredInstances = Filter.useFilter(data, filter);

    filteredRows = (Object[][]) multiple.invoke(
	getSourceCode(), new Object[]{instancesToObjects(data)});
    
    result = compare(filteredInstances, filteredRows);
    
    // 2. instance by instance
    if (result) {
      filter = Filter.makeCopy(getFilter());
      filter.setInputFormat(data);
      Filter.useFilter(data, filter);
      
      for (i = 0; i < data.numInstances(); i++) {
	filter.input(data.instance(i));
	filter.batchFinished();
	filteredInstance = filter.output();
	
	filteredRow = (Object[]) single.invoke(
	    getSourceCode(), new Object[]{instanceToObjects(data.instance(i))});

	if (!compare(filteredInstance, filteredRow))
	  System.out.println(
	      (i+1) + ". instance (Filter/Source code): " 
	      + filteredInstance + " != " + Utils.arrayToString(filteredRow));
      }
    }
    
    return result;
  }
  
  /**
   * Executes the tests, use "-h" to list the commandline options.
   * 
   * @param args        the commandline parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception{
    CheckSource         check;
    StringBuffer        text;
    Enumeration         enm;
    
    check = new CheckSource();
    if (Utils.getFlag('h', args)) {
      text = new StringBuffer();   
      text.append("\nHelp requested:\n\n");
      enm = check.listOptions();
      while (enm.hasMoreElements()) {
        Option option = (Option) enm.nextElement();
        text.append(option.synopsis() + "\n");
        text.append(option.description() + "\n");
      }
      System.out.println("\n" + text + "\n");
    }
    else {
      check.setOptions(args);
      if (check.execute())
        System.out.println("Tests OK!");
      else
        System.out.println("Tests failed!");
    }
  }
}
