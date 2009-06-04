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
 *    PairedTTester.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.experiment;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Range;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.FastVector;
import weka.core.Statistics;
import weka.core.OptionHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Option;

/**
 * Calculates T-Test statistics on data stored in a set of instances.<p>
 *
 * Valid options from the command-line are:<p>
 *
 * -D num,num2... <br>
 * The column numbers that uniquely specify a dataset.
 * (default last) <p>
 *
 * -R num <br>
 * The column number containing the run number.
 * (default last) <p>
 *
 * -F num <br>
 * The column number containing the fold number.
 * (default none) <p>
 *
 * -S num <br>
 * The significance level for T-Tests.
 * (default 0.05) <p>
 *
 * -G num,num2... <br>
 * The column numbers that uniquely specify one result generator (eg:
 * scheme name plus options).
 * (default last) <p>
 *
 * -L <br>
 * Produce comparison tables in Latex table format <p>
 *
 * -csv <br>
 * Produce comparison tables in csv format <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.22.2.2 $
 */
public class PairedTTester implements OptionHandler {

  /** The set of instances we will analyse */
  protected Instances m_Instances;

  /** The index of the column containing the run number */
  protected int m_RunColumn = 0;

  /** The option setting for the run number column (-1 means last) */
  protected int m_RunColumnSet = -1;

  /** The option setting for the fold number column (-1 means none) */
  protected int m_FoldColumn = -1;

  /** The significance level for comparisons */
  protected double m_SignificanceLevel = 0.05;

  /**
   * The range of columns that specify a unique "dataset"
   * (eg: scheme plus configuration)
   */
  protected Range m_DatasetKeyColumnsRange = new Range();

  /** An array containing the indexes of just the selected columns */ 
  protected int [] m_DatasetKeyColumns;

  /** The list of dataset specifiers */
  protected DatasetSpecifiers m_DatasetSpecifiers = 
    new DatasetSpecifiers();

  /**
   * The range of columns that specify a unique result set
   * (eg: scheme plus configuration)
   */
  protected Range m_ResultsetKeyColumnsRange = new Range();

  /** An array containing the indexes of just the selected columns */ 
  protected int [] m_ResultsetKeyColumns;

  /** An array containing the indexes of the datasets to display */
  protected int[] m_DisplayedResultsets = null;

  /** Stores a vector for each resultset holding all instances in each set */
  protected FastVector m_Resultsets = new FastVector();

  /** Indicates whether the instances have been partitioned */
  protected boolean m_ResultsetsValid;

  /** Indicates whether standard deviations should be displayed */
  protected boolean m_ShowStdDevs = false;

  /** Produce tables in latex format */
  protected boolean m_latexOutput = false;
  
  /** Produce tables in csv format */
  protected boolean m_csvOutput = false;
  
  /** the number of digits after the period (= precision) for printing the mean */
  protected int m_MeanPrec = 2;
  
  /** the number of digits after the period (= precision) for printing the std. deviation */
  protected int m_StdDevPrec = 2;
  
  /* A list of unique "dataset" specifiers that have been observed */
  protected class DatasetSpecifiers {

    FastVector m_Specifiers = new FastVector();

    /**
     * Removes all specifiers.
     */
    protected void removeAllSpecifiers() {

      m_Specifiers.removeAllElements();
    }

    /** 
     * Add an instance to the list of specifiers (if necessary)
     */
    protected void add(Instance inst) {
      
      for (int i = 0; i < m_Specifiers.size(); i++) {
	Instance specifier = (Instance)m_Specifiers.elementAt(i);
	boolean found = true;
	for (int j = 0; j < m_DatasetKeyColumns.length; j++) {
	  if (inst.value(m_DatasetKeyColumns[j]) !=
	      specifier.value(m_DatasetKeyColumns[j])) {
	    found = false;
	  }
	}
	if (found) {
	  return;
	}
      }
      m_Specifiers.addElement(inst);
    }

    /**
     * Get the template at the given position.
     */
    protected Instance specifier(int i) {

      return (Instance)m_Specifiers.elementAt(i);
    }

    /**
     * Gets the number of specifiers.
     */
    protected int numSpecifiers() {

      return m_Specifiers.size();
    }
  }

  /* Utility class to store the instances pertaining to a dataset */
  protected class Dataset {

    Instance m_Template;
    FastVector m_Dataset;

    public Dataset(Instance template) {

      m_Template = template;
      m_Dataset = new FastVector();
      add(template);
    }
    
    /**
     * Returns true if the two instances match on those attributes that have
     * been designated key columns (eg: scheme name and scheme options)
     *
     * @param first the first instance
     * @param second the second instance
     * @return true if first and second match on the currently set key columns
     */
    protected boolean matchesTemplate(Instance first) {
      
      for (int i = 0; i < m_DatasetKeyColumns.length; i++) {
	if (first.value(m_DatasetKeyColumns[i]) !=
	    m_Template.value(m_DatasetKeyColumns[i])) {
	  return false;
	}
      }
      return true;
    }

    /**
     * Adds the given instance to the dataset
     */
    protected void add(Instance inst) {
      
      m_Dataset.addElement(inst);
    }

    /**
     * Returns a vector containing the instances in the dataset
     */
    protected FastVector contents() {

      return m_Dataset;
    }

    /**
     * Sorts the instances in the dataset by the run number.
     *
     * @param runColumn a value of type 'int'
     */
    public void sort(int runColumn) {

      double [] runNums = new double [m_Dataset.size()];
      for (int j = 0; j < runNums.length; j++) {
	runNums[j] = ((Instance) m_Dataset.elementAt(j)).value(runColumn);
      }
      int [] index = Utils.stableSort(runNums);
      FastVector newDataset = new FastVector(runNums.length);
      for (int j = 0; j < index.length; j++) {
	newDataset.addElement(m_Dataset.elementAt(index[j]));
      }
      m_Dataset = newDataset;
    }
  }
 
  /* Utility class to store the instances in a resultset */
  protected class Resultset {

    Instance m_Template;
    FastVector m_Datasets;

    public Resultset(Instance template) {

      m_Template = template;
      m_Datasets = new FastVector();
      add(template);
    }
    
    /**
     * Returns true if the two instances match on those attributes that have
     * been designated key columns (eg: scheme name and scheme options)
     *
     * @param first the first instance
     * @param second the second instance
     * @return true if first and second match on the currently set key columns
     */
    protected boolean matchesTemplate(Instance first) {
      
      for (int i = 0; i < m_ResultsetKeyColumns.length; i++) {
	if (first.value(m_ResultsetKeyColumns[i]) !=
	    m_Template.value(m_ResultsetKeyColumns[i])) {
	  return false;
	}
      }
      return true;
    }

    /**
     * Returns a string descriptive of the resultset key column values
     * for this resultset
     *
     * @return a value of type 'String'
     */
    protected String templateString() {

      String result = "";
      String tempResult = "";
      for (int i = 0; i < m_ResultsetKeyColumns.length; i++) {
	tempResult = m_Template.toString(m_ResultsetKeyColumns[i]) + ' ';

	// compact the string
        tempResult = Utils.removeSubstring(tempResult, "weka.classifiers.");
        tempResult = Utils.removeSubstring(tempResult, "weka.filters.");
        tempResult = Utils.removeSubstring(tempResult, "weka.attributeSelection.");
	result += tempResult;
      }
      return result.trim();
    }
    
    /**
     * Returns a vector containing all instances belonging to one dataset.
     *
     * @param index a template instance
     * @return a value of type 'FastVector'
     */
    public FastVector dataset(Instance inst) {

      for (int i = 0; i < m_Datasets.size(); i++) {
	if (((Dataset)m_Datasets.elementAt(i)).matchesTemplate(inst)) {
	  return ((Dataset)m_Datasets.elementAt(i)).contents();
	} 
      }
      return null;
    }
    
    /**
     * Adds an instance to this resultset
     *
     * @param newInst a value of type 'Instance'
     */
    public void add(Instance newInst) {
      
      for (int i = 0; i < m_Datasets.size(); i++) {
	if (((Dataset)m_Datasets.elementAt(i)).matchesTemplate(newInst)) {
	  ((Dataset)m_Datasets.elementAt(i)).add(newInst);
	  return;
	}
      }
      Dataset newDataset = new Dataset(newInst);
      m_Datasets.addElement(newDataset);
    }

    /**
     * Sorts the instances in each dataset by the run number.
     *
     * @param runColumn a value of type 'int'
     */
    public void sort(int runColumn) {

      for (int i = 0; i < m_Datasets.size(); i++) {
	((Dataset)m_Datasets.elementAt(i)).sort(runColumn);
      }
    }
  } // Resultset


  /**
   * Returns a string descriptive of the key column values for
   * the "datasets
   *
   * @param template the template
   * @return a value of type 'String'
   */
  protected String templateString(Instance template) {
    
    String result = "";
    for (int i = 0; i < m_DatasetKeyColumns.length; i++) {
      result += template.toString(m_DatasetKeyColumns[i]) + ' ';
    }
    if (result.startsWith("weka.classifiers.")) {
      result = result.substring("weka.classifiers.".length());
    }
    return result.trim();
  }

  /**
   * Sets the precision of the mean output
   * @param precision the number of digits used in printing the mean
   */
  public void setMeanPrec(int precision) {
    m_MeanPrec = precision;
  }

  /**
   * Gets the precision used for printing the mean
   * @return the number of digits used in printing the mean
   */
  public int getMeanPrec() {
    return m_MeanPrec;
  }

  /**
   * Sets the precision of the std. deviation output
   * @param precision the number of digits used in printing the std. deviation
   */
  public void setStdDevPrec(int precision) {
    m_StdDevPrec = precision;
  }

  /**
   * Gets the precision used for printing the std. deviation
   * @return the number of digits used in printing the std. deviation
   */
  public int getStdDevPrec() {
    return m_StdDevPrec;
  }

  /**
   * Set whether latex is output
   * @param l true if tables are to be produced in Latex format
   */
  public void setProduceLatex(boolean l) {
    m_latexOutput = l;
    if (m_latexOutput)
      setProduceCSV(false);
  }

  /**
   * Get whether latex is output
   * @return true if Latex is to be output
   */
  public boolean getProduceLatex() {
    return m_latexOutput;
  }
  
  /**
   * Set whether csv is output
   * @param c true if tables are to be produced in csv format
   */
  public void setProduceCSV(boolean c) {
    m_csvOutput = c;
    if (m_csvOutput)
      setProduceLatex(false);
  }
  
  /**
   * Get whether csv is output
   * @return true if csv is to be output
   */
  public boolean getProduceCSV() {
    return m_csvOutput;
  }

  /**
   * Set whether standard deviations are displayed or not.
   * @param s true if standard deviations are to be displayed
   */
  public void setShowStdDevs(boolean s) {
    m_ShowStdDevs = s;
  }

  /**
   * Returns true if standard deviations have been requested.
   * @return true if standard deviations are to be displayed.
   */
  public boolean getShowStdDevs() {
    return m_ShowStdDevs;
  }
  
  /**
   * Separates the instances into resultsets and by dataset/run.
   *
   * @exception Exception if the TTest parameters have not been set.
   */
  protected void prepareData() throws Exception {

    if (m_Instances == null) {
      throw new Exception("No instances have been set");
    }
    if (m_RunColumnSet == -1) {
      m_RunColumn = m_Instances.numAttributes() - 1;
    } else {
      m_RunColumn = m_RunColumnSet;
    }

    if (m_ResultsetKeyColumnsRange == null) {
      throw new Exception("No result specifier columns have been set");
    }
    m_ResultsetKeyColumnsRange.setUpper(m_Instances.numAttributes() - 1);
    m_ResultsetKeyColumns = m_ResultsetKeyColumnsRange.getSelection();

    if (m_DatasetKeyColumnsRange == null) {
      throw new Exception("No dataset specifier columns have been set");
    }
    m_DatasetKeyColumnsRange.setUpper(m_Instances.numAttributes() - 1);
    m_DatasetKeyColumns = m_DatasetKeyColumnsRange.getSelection();
    
    //  Split the data up into result sets
    m_Resultsets.removeAllElements();  
    m_DatasetSpecifiers.removeAllSpecifiers();
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      Instance current = m_Instances.instance(i);
      if (current.isMissing(m_RunColumn)) {
	throw new Exception("Instance has missing value in run "
			    + "column!\n" + current);
      } 
      for (int j = 0; j < m_ResultsetKeyColumns.length; j++) {
	if (current.isMissing(m_ResultsetKeyColumns[j])) {
	  throw new Exception("Instance has missing value in resultset key "
			      + "column " + (m_ResultsetKeyColumns[j] + 1)
			      + "!\n" + current);
	}
      }
      for (int j = 0; j < m_DatasetKeyColumns.length; j++) {
	if (current.isMissing(m_DatasetKeyColumns[j])) {
	  throw new Exception("Instance has missing value in dataset key "
			      + "column " + (m_DatasetKeyColumns[j] + 1)
			      + "!\n" + current);
	}
      }
      boolean found = false;
      for (int j = 0; j < m_Resultsets.size(); j++) {
	Resultset resultset = (Resultset) m_Resultsets.elementAt(j);
	if (resultset.matchesTemplate(current)) {
	  resultset.add(current);
	  found = true;
	  break;
	}
      }
      if (!found) {
	Resultset resultset = new Resultset(current);
	m_Resultsets.addElement(resultset);
      }

      m_DatasetSpecifiers.add(current);
    }

    // Tell each resultset to sort on the run column
    for (int j = 0; j < m_Resultsets.size(); j++) {
      Resultset resultset = (Resultset) m_Resultsets.elementAt(j);
      if (m_FoldColumn >= 0) {
	// sort on folds first in case they are out of order
	resultset.sort(m_FoldColumn);
      }
      resultset.sort(m_RunColumn);
    }
    m_ResultsetsValid = true;
  }

  /**
   * Gets the number of datasets in the resultsets
   *
   * @return the number of datasets in the resultsets
   */
  public int getNumDatasets() {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	ex.printStackTrace();
	return 0;
      }
    }
    return m_DatasetSpecifiers.numSpecifiers();
  }

  /**
   * Gets the number of resultsets in the data.
   *
   * @return the number of resultsets in the data
   */
  public int getNumResultsets() {

    if (!m_ResultsetsValid) {
      try {
  prepareData();
      } catch (Exception ex) {
  ex.printStackTrace();
  return 0;
      }
    }
    return m_Resultsets.size();
  }

  /**
   * Gets the number of displayed resultsets in the data.
   *
   * @return the number of displayed resultsets in the data
   */
  public int getNumDisplayedResultsets() {

    if (!m_ResultsetsValid) {
      try {
        prepareData();
      } catch (Exception ex) {
        ex.printStackTrace();
        return 0;
      }
    }
    
    int result = 0;
    for (int i = 0; i < getNumResultsets(); i++) {
      if (displayResultset(i))
        result++;
    }
    return result;
  }

  /**
   * Gets a string descriptive of the specified resultset.
   *
   * @param index the index of the resultset
   * @return a descriptive string for the resultset
   */
  public String getResultsetName(int index) {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	ex.printStackTrace();
	return null;
      }
    }
    return ((Resultset) m_Resultsets.elementAt(index)).templateString();
  }
  
  /**
   * Checks whether the resultset with the given index shall be displayed.
   * 
   * @param index the index of the resultset to check whether it shall be displayed 
   * @return whether the specified resultset is displayed 
   */
  public boolean displayResultset(int index) {
    boolean       result;
    int           i;
    
    result = true;

    if (m_DisplayedResultsets != null) {
      result = false;
      for (i = 0; i < m_DisplayedResultsets.length; i++) {
        if (m_DisplayedResultsets[i] == index) {
          result = true;
          break;
        }
      }
    }
      
    return result;
  }
  
  /**
   * Computes a paired t-test comparison for a specified dataset between
   * two resultsets.
   *
   * @param datasetSpecifier the dataset specifier
   * @param resultset1Index the index of the first resultset
   * @param resultset2Index the index of the second resultset
   * @param comparisonColumn the column containing values to compare
   * @return the results of the paired comparison
   * @exception Exception if an error occurs
   */
  public PairedStats calculateStatistics(Instance datasetSpecifier,
					 int resultset1Index,
					 int resultset2Index,
					 int comparisonColumn) throws Exception {

    if (m_Instances.attribute(comparisonColumn).type()
	!= Attribute.NUMERIC) {
      throw new Exception("Comparison column " + (comparisonColumn + 1)
			  + " ("
			  + m_Instances.attribute(comparisonColumn).name()
			  + ") is not numeric");
    }
    if (!m_ResultsetsValid) {
      prepareData();
    }

    Resultset resultset1 = (Resultset) m_Resultsets.elementAt(resultset1Index);
    Resultset resultset2 = (Resultset) m_Resultsets.elementAt(resultset2Index);
    FastVector dataset1 = resultset1.dataset(datasetSpecifier);
    FastVector dataset2 = resultset2.dataset(datasetSpecifier);
    String datasetName = templateString(datasetSpecifier);
    if (dataset1 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset1.templateString());
    } else if (dataset2 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset2.templateString());
    } else if (dataset1.size() != dataset2.size()) {
      throw new Exception("Results for dataset=" + datasetName
			  + " differ in size for resultset="
			  + resultset1.templateString()
			  + " and resultset="
			  + resultset2.templateString()
			  );
    }
    
    PairedStats pairedStats = new PairedStats(m_SignificanceLevel);

    for (int k = 0; k < dataset1.size(); k ++) {
      Instance current1 = (Instance) dataset1.elementAt(k);
      Instance current2 = (Instance) dataset2.elementAt(k);
      if (current1.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current1);
      }
      if (current2.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current2);
      }
      if (current1.value(m_RunColumn) != current2.value(m_RunColumn)) {
	System.err.println("Run numbers do not match!\n"
			    + current1 + current2);
      }
      if (m_FoldColumn != -1) {
	if (current1.value(m_FoldColumn) != current2.value(m_FoldColumn)) {
	  System.err.println("Fold numbers do not match!\n"
			     + current1 + current2);
	}
      }
      double value1 = current1.value(comparisonColumn);
      double value2 = current2.value(comparisonColumn);
      pairedStats.add(value1, value2);
    }
    pairedStats.calculateDerived();
    System.err.println("Differences stats:\n" + pairedStats.differencesStats);
    return pairedStats;

  }
  
  /**
   * Creates a key that maps resultset numbers to their descriptions.
   *
   * @return a value of type 'String'
   */
  public String resultsetKey() {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	ex.printStackTrace();
	return ex.getMessage();
      }
    }
    String result = "";
    for (int j = 0; j < getNumResultsets(); j++) {
      result += "(" + (j + 1) + ") " + getResultsetName(j) + '\n';
    }
    return result + '\n';
  }
  
  /**
   * Creates a "header" string describing the current resultsets.
   *
   * @param comparisonColumn a value of type 'int'
   * @return a value of type 'String'
   */
  public String header(int comparisonColumn) {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	ex.printStackTrace();
	return ex.getMessage();
      }
    }
    return "Analysing:  "
      + m_Instances.attribute(comparisonColumn).name() + '\n'
      + "Datasets:   " + getNumDatasets() + '\n'
      + "Resultsets: " + getNumResultsets() + '\n'
      + "Confidence: " + getSignificanceLevel() + " (two tailed)\n"
      + "Date:       " + (new SimpleDateFormat()).format(new Date()) + "\n\n";
  }

  /**
   * Carries out a comparison between all resultsets, counting the number
   * of datsets where one resultset outperforms the other.
   *
   * @param comparisonColumn the index of the comparison column
   * @return a 2d array where element [i][j] is the number of times resultset
   * j performed significantly better than resultset i.
   * @exception Exception if an error occurs
   */
  public int [][] multiResultsetWins(int comparisonColumn, int [][] nonSigWin)
    throws Exception {

    int numResultsets = getNumResultsets();
    int [][] win = new int [numResultsets][numResultsets];
    //    int [][] nonSigWin = new int [numResultsets][numResultsets];
    for (int i = 0; i < numResultsets; i++) {
      for (int j = i + 1; j < numResultsets; j++) {
	System.err.print("Comparing (" + (i + 1) + ") with ("
			 + (j + 1) + ")\r");
	System.err.flush();
	for (int k = 0; k < getNumDatasets(); k++) {
	  try {
	    PairedStats pairedStats = 
	      calculateStatistics(m_DatasetSpecifiers.specifier(k), i, j,
				  comparisonColumn);
	    if (pairedStats.differencesSignificance < 0) {
	      win[i][j]++;
	    } else if (pairedStats.differencesSignificance > 0) {
	      win[j][i]++;
	    }

	    if (pairedStats.differencesStats.mean < 0) {
	      nonSigWin[i][j]++;
	    } else if (pairedStats.differencesStats.mean > 0) {
	      nonSigWin[j][i]++;
	    }
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    System.err.println(ex.getMessage());
	  }
	}
      }
    }
    return win;
  }
  
  /**
   * Carries out a comparison between all resultsets, counting the number
   * of datsets where one resultset outperforms the other. The results
   * are summarized in a table.
   *
   * @param comparisonColumn the index of the comparison column
   * @return the results in a string
   * @exception Exception if an error occurs
   */
  public String multiResultsetSummary(int comparisonColumn)
    throws Exception {
    
    int numResultsets = getNumResultsets();
    int [][] nonSigWin = new int [numResultsets][numResultsets];

    int [][] win = multiResultsetWins(comparisonColumn, nonSigWin);
    int resultsetLength = 1 + Math.max((int)(Math.log(numResultsets)
					     / Math.log(10)),
				       (int)(Math.log(getNumDatasets()) / 
					     Math.log(10)));
    String result = "";
    String titles = "";

    if (m_latexOutput) {
      result += "{\\centering\n";
      result += "\\begin{table}[thb]\n\\caption{\\label{labelname}"
		  +"Table Caption}\n";
      result += "\\footnotesize\n";
      result += "\\begin{tabular}{l";
    }

    for (int i = 0; i < numResultsets; i++) {
      if (!displayResultset(i))
        continue;
      if (m_latexOutput) {
	titles += " &";
	result += "c";
      }
      titles += ' ' + Utils.padLeft("" + (char)((int)'a' + i % 26),
				    resultsetLength * 2 + 3);
    }
    if (m_latexOutput) {
      result += "}\\\\\n\\hline\n";
      result += titles + " \\\\\n\\hline\n";
    } else {
      result += titles + "  (No. of datasets where [col] >> [row])\n";
    }
    for (int i = 0; i < numResultsets; i++) {
      if (!displayResultset(i))
        continue;
      for (int j = 0; j < numResultsets; j++) {
        if (!displayResultset(j))
          continue;
	if (m_latexOutput && j == 0) {
	  result +=  (char)((int)'a' + i % 26);
	}
	if (j == i) {
	  if (m_latexOutput) {
	    result += " & - ";
	  } else {
	    result += ' ' + Utils.padLeft("-", resultsetLength * 2 + 3);
	  }
	} else {
	  if (m_latexOutput) {
	    result += "& " + nonSigWin[i][j] + " (" + win[i][j] + ") ";
	  } else {
	    result += ' ' + Utils.padLeft("" + nonSigWin[i][j] +  " (" + win[i][j] + ")"
					  , resultsetLength * 2 + 3);
	  }
	}
      }
      if (!m_latexOutput) {
	result += " | " + (char)((int)'a' + i % 26)
	  + " = " + getResultsetName(i) + '\n';
      } else {
	result += "\\\\\n";
      }
    }

    if (m_latexOutput) {
      result += "\\hline\n\\end{tabular} \\footnotesize \\par\n\\end{table}}";
    }
    return result;
  }

  public String multiResultsetRanking(int comparisonColumn)
    throws Exception {
    
    int numResultsets = getNumResultsets();
    int [][] nonSigWin = new int [numResultsets][numResultsets];

    int [][] win = multiResultsetWins(comparisonColumn, nonSigWin);

    int [] wins = new int [numResultsets];
    int [] losses = new int [numResultsets];
    int [] diff = new int [numResultsets];
    for (int i = 0; i < win.length; i++) {
      for (int j = 0; j < win[i].length; j++) {
	wins[j] += win[i][j];
	diff[j] += win[i][j];
	losses[i] += win[i][j];
	diff[i] -= win[i][j];
      }
    }
    int biggest = Math.max(wins[Utils.maxIndex(wins)],
			   losses[Utils.maxIndex(losses)]);
    int width = Math.max(2 + (int)(Math.log(biggest) / Math.log(10)),
			 ">-<".length());
    String result;
    if (m_latexOutput) {
      result = "\\begin{table}[thb]\n\\caption{\\label{labelname}Table Caption"
	+"}\n\\footnotesize\n{\\centering \\begin{tabular}{rlll}\\\\\n\\hline\n";
      result += "Resultset & Wins$-$ & Wins & Losses \\\\\n& Losses & & "
	+"\\\\\n\\hline\n";
    } else {
      result = Utils.padLeft(">-<", width) + ' '
	+ Utils.padLeft(">", width) + ' '
	+ Utils.padLeft("<", width) + " Resultset\n";
    }
    int [] ranking = Utils.sort(diff);
    for (int i = numResultsets - 1; i >= 0; i--) {
      int curr = ranking[i];
      if (!displayResultset(curr))
        continue;
      if (m_latexOutput) {
	result += "(" + (curr+1) + ") & " 
	  + Utils.padLeft("" + diff[curr], width) 
	  +" & " + Utils.padLeft("" + wins[curr], width)
	  +" & " + Utils.padLeft("" + losses[curr], width)
	  +"\\\\\n";
      } else {
	result += Utils.padLeft("" + diff[curr], width) + ' '
	  + Utils.padLeft("" + wins[curr], width) + ' '
	  + Utils.padLeft("" + losses[curr], width) + ' '
	  + getResultsetName(curr) + '\n';
      }
    }

    if (m_latexOutput) {
      result += "\\hline\n\\end{tabular} \\footnotesize \\par}\n\\end{table}";
    }
    return result;
  }

  /**
   * Generates a comparison table in latex table format
   *
   * @param baseResultset the index of the base resultset
   * @param comparisonColumn the index of the column to compare over
   * @param maxWidthMean width for the mean
   * @param maxWidthStdDev width for the standard deviation
   * @return the comparison table string
   */
  private String multiResultsetFullLatex(int baseResultset,
					 int comparisonColumn,
					 int maxWidthMean,
					 int maxWidthStdDev) {

    StringBuffer result = new StringBuffer(1000);
    String tmpStr = "";
    int numcols = getNumDisplayedResultsets() * 2;
    if (m_ShowStdDevs) {
      numcols += getNumDisplayedResultsets();
    }

    result.append("\\begin{table}[thb]\n\\caption{\\label{labelname}"
		  +"Table Caption}\n");
    if (!m_ShowStdDevs) {
      result.append("\\footnotesize\n");
    } else {
      result.append("\\scriptsize\n");
    }

    // output the column alignment characters
    // one for the dataset name and one for the comparison column
    if (!m_ShowStdDevs) {
      result.append("{\\centering \\begin{tabular}{ll");
    } else {
      // dataset, mean, std dev
      result.append("{\\centering \\begin{tabular}{lr@{\\hspace{0cm}}l");
    }

    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
	if (!m_ShowStdDevs) {
	  result.append("l@{\\hspace{0.1cm}}l");
	} else {
	  result.append("r@{\\hspace{0cm}}l@{\\hspace{0cm}}r");
	}
      }
    }
    result.append("}\n\\\\\n\\hline\n");
    if (!m_ShowStdDevs) {
      result.append("Data Set & ("+(baseResultset+1)+")");
    } else {
      result.append("Data Set & \\multicolumn{2}{c}{("+(baseResultset+1)+")}");
    }

    // now do the column names (numbers)
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
	if (!m_ShowStdDevs) {
	  result.append("& (" + (j + 1) + ") & ");
	} else {
	  result.append("& \\multicolumn{3}{c}{(" + (j + 1) + ")} ");
	}
      }
    }
    result.append("\\\\\n\\hline\n");
    
    int datasetLength = 25;
    int resultsetLength = maxWidthMean + 5 + m_MeanPrec;
    if (m_ShowStdDevs) {
      resultsetLength += (maxWidthStdDev + 8 + m_StdDevPrec);
    }

    for (int i = 0; i < getNumDatasets(); i++) {
      // Print the name of the dataset
      String datasetName = 
	templateString(m_DatasetSpecifiers.specifier(i)).replace('_','-');
      try {
	PairedStats pairedStats = 
	  calculateStatistics(m_DatasetSpecifiers.specifier(i), 
			      baseResultset, baseResultset,
			      comparisonColumn);
	datasetName = Utils.padRight(datasetName, datasetLength);
	result.append(datasetName);

	
	if (!m_ShowStdDevs) {
	  tmpStr = padIt(pairedStats.xStats.mean, maxWidthMean + 5, m_MeanPrec);
	} else {
	  tmpStr = padIt(pairedStats.xStats.mean, maxWidthMean + 5, m_MeanPrec) + "$\\pm$";
	  if (Double.isNaN(pairedStats.xStats.stdDev)) {
	    tmpStr += "&" + Utils.doubleToString(0.0, maxWidthStdDev + 3, m_StdDevPrec) + " ";
	  } else {
	    tmpStr += "&" + padIt(pairedStats.xStats.stdDev, maxWidthStdDev + 3, m_StdDevPrec) + " ";
	  }
	}
	if (tmpStr.length() < resultsetLength - 2)
	  tmpStr = Utils.padLeft(tmpStr, resultsetLength - 2);
	result.append("& ")
	      .append(tmpStr);
	// Iterate over the resultsets
	for (int j = 0; j < getNumResultsets(); j++) {
          if (!displayResultset(j))
            continue;
	  if (j != baseResultset) {
	    try {
	      pairedStats = 
		calculateStatistics(m_DatasetSpecifiers.specifier(i), 
				    baseResultset, j, comparisonColumn);
	      String sigString = "         ";
	      if (pairedStats.differencesSignificance < 0) {
		sigString = "$\\circ$  ";
	      } else if (pairedStats.differencesSignificance > 0) {
		sigString = "$\\bullet$";
	      } 
	      if (!m_ShowStdDevs) {
		tmpStr = padIt(pairedStats.yStats.mean, maxWidthMean + 5, m_MeanPrec);
	      } else {
		tmpStr = padIt(pairedStats.yStats.mean, maxWidthMean + 5, m_MeanPrec) + "$\\pm$";
		if (Double.isNaN(pairedStats.yStats.stdDev)) {
		  tmpStr += "&" + Utils.doubleToString(0.0, maxWidthStdDev + 3, m_StdDevPrec) + " ";
		} else {
		  tmpStr += "&" + padIt(pairedStats.yStats.stdDev, maxWidthStdDev + 3, m_StdDevPrec) + " ";
		}
	      }
	      if (tmpStr.length() < resultsetLength - 2)
	        tmpStr = Utils.padLeft(tmpStr, resultsetLength - 2);
	      result.append(" & ")
	            .append(tmpStr)
	            .append(" & ")
	            .append(sigString);
	    } catch (Exception ex) {
	      ex.printStackTrace();
	      result.append(Utils.padLeft("", resultsetLength + 1));
	    }
	  }
	}
	result.append("\\\\\n");
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }

    result.append("\\hline\n\\multicolumn{"+numcols+"}{c}{$\\circ$, $\\bullet$"
		  +" statistically significant improvement or degradation}"
		  +"\\\\\n\\end{tabular} ");
    if (!m_ShowStdDevs) {
      result.append("\\footnotesize ");
      } else {
	result.append("\\scriptsize ");
      }
    
    result.append("\\par}\n\\end{table}"
		  +"\n");
    return result.toString();
  }

  /**
   * pads the given double on the left side with blanks and returns the string
   * 
   * @param value the value to print as string
   * @param a the width of the double
   * @param b the precision of the double (digits after period)
   * @return the double as left-padded string
   */
  private String padIt(double value, int a, int b) {
    String res = Utils.doubleToString(value,
				      a, b);
    int precision = 0;  
    int width = res.length();

    res = res.trim();
    
    // how many "0" to add? -> determine current precision digits
    if (res.indexOf(".") == -1) {
      if (b > 0)
        res += ".";
      precision = 0;
    }
    else {
      precision = res.substring(res.indexOf(".") + 1).length();
    }
    for (int i = precision; i < b; i++)
      res += "0";

    while (res.length() < width) {
      res = " " + res;
    }
    return res;
  }
  
  /**
   * Generates a comparison table in latex table format
   *
   * @param baseResultset the index of the base resultset
   * @param comparisonColumn the index of the column to compare over
   * @param maxWidthMean width for the mean
   * @param maxWidthStdDev width for the standard deviation
   * @return the comparison table string
   */
  private String multiResultsetFullPlainText(int baseResultset,
                                             int comparisonColumn,
                                             int maxWidthMean,
                                             int maxWidthStdDev) {

    StringBuffer result = new StringBuffer(1000);
    String tmpStr = "";
    int datasetLength = 25;
    //    int resultsetLength = 9;
    //    int resultsetLength = 16;
    int resultsetLength = maxWidthMean + 5 + m_MeanPrec;
    if (m_ShowStdDevs) {
      resultsetLength += (maxWidthStdDev + 3 + m_StdDevPrec);
    }

    // Set up the titles
    StringBuffer titles = new StringBuffer(Utils.padRight("Dataset",
                                                          datasetLength));
    titles.append(' ');
    StringBuffer label 
      = new StringBuffer(Utils.padLeft("(" + (baseResultset + 1)
                                       + ") "
                                       + getResultsetName(baseResultset),
                                       resultsetLength + 3));

    titles.append(label);
    StringBuffer separator = new StringBuffer(Utils.padRight("",
                                                             datasetLength));
    while (separator.length() < titles.length()) {
      separator.append('-');
    }
    separator.append("---");
    titles.append(" | ");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
        label = new StringBuffer(Utils.padLeft("(" + (j + 1) + ") "
                                               + getResultsetName(j), resultsetLength));
        titles.append(label).append(' ');
        for (int i = 0; i < label.length(); i++) {
          separator.append('-');
        }
        separator.append('-');
      }
    }
    result.append(titles).append('\n').append(separator).append('\n');
    
    // Iterate over datasets
    int [] win = new int [getNumResultsets()];
    int [] loss = new int [getNumResultsets()];
    int [] tie = new int [getNumResultsets()];
    StringBuffer skipped = new StringBuffer("");
    for (int i = 0; i < getNumDatasets(); i++) {
      // Print the name of the dataset
      String datasetName = 
        templateString(m_DatasetSpecifiers.specifier(i));
      try {
        PairedStats pairedStats = 
          calculateStatistics(m_DatasetSpecifiers.specifier(i), 
                              baseResultset, baseResultset,
                              comparisonColumn);
        datasetName = Utils.padRight(datasetName, datasetLength);
        result.append(datasetName);
        result.append(Utils.padLeft('(' + Utils.doubleToString(pairedStats.count, 0) + ')', 5))
              .append(' ');
        if (!m_ShowStdDevs) {
	  tmpStr = padIt(pairedStats.xStats.mean, maxWidthMean + 1 + m_MeanPrec, m_MeanPrec);
        } else {
          tmpStr = padIt(pairedStats.xStats.mean, maxWidthMean + 1 + m_MeanPrec, m_MeanPrec);
          if (Double.isInfinite(pairedStats.xStats.stdDev)) {
            tmpStr += '(' + Utils.padRight("Inf", maxWidthStdDev + 1 + m_StdDevPrec) +')';
          } else {
            tmpStr += '(' + padIt(pairedStats.xStats.stdDev, maxWidthStdDev + 1 + m_StdDevPrec, m_StdDevPrec) + ')';
          }
        }
        result.append(Utils.padLeft(tmpStr, resultsetLength - 2)).append(" | ");
        // Iterate over the resultsets
        for (int j = 0; j < getNumResultsets(); j++) {
          if (!displayResultset(j))
            continue;
          if (j != baseResultset) {
            try {
              pairedStats = 
                calculateStatistics(m_DatasetSpecifiers.specifier(i), 
                                    baseResultset, j, comparisonColumn);
              char sigChar = ' ';
              if (pairedStats.differencesSignificance < 0) {
                sigChar = 'v';
                win[j]++;
              } else if (pairedStats.differencesSignificance > 0) {
                sigChar = '*';
                loss[j]++;
              } else {
                tie[j]++;
              }
              if (!m_ShowStdDevs) {
                tmpStr = padIt(pairedStats.yStats.mean, resultsetLength - 2, m_MeanPrec);
              } else {
                tmpStr = padIt(pairedStats.yStats.mean, maxWidthMean + 5, m_MeanPrec);
                if (Double.isInfinite(pairedStats.yStats.stdDev)) {
                  tmpStr += '(' + Utils.padRight("Inf", maxWidthStdDev + 3) + ')';
                } else {
                  tmpStr += '(' + padIt(pairedStats.yStats.stdDev, maxWidthStdDev + 3, m_StdDevPrec) + ')';
                }
              }
              result.append(Utils.padLeft(tmpStr, resultsetLength - 2))
                    .append(' ')
                    .append(sigChar)
                    .append(' ');
            } catch (Exception ex) {
              ex.printStackTrace();
              result.append(Utils.padLeft("", resultsetLength + 1));
            }
          }
        }
        result.append('\n');
      } catch (Exception ex) {
        ex.printStackTrace();
        skipped.append(datasetName).append(' ');
      }
    }
    result.append(separator).append('\n');
    result.append(Utils.padLeft("(v/ /*)", datasetLength + 4 +
                                resultsetLength)).append(" | ");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
        result.append(Utils.padLeft("(" + win[j] + '/' + tie[j]
                                    + '/' + loss[j] + ')',
                                    resultsetLength)).append(' ');
      }
    }
    result.append('\n');
    if (!skipped.equals("")) {
      result.append("Skipped: ").append(skipped).append('\n');
    }
    return result.toString();
  }
  
  /**
   * Generates a comparison table in CSV table format
   *
   * @param baseResultset the index of the base resultset
   * @param comparisonColumn the index of the column to compare over
   * @param maxWidthMean width for the mean
   * @param maxWidthStdDev width for the standard deviation
   * @return the comparison table string
   */
  private String multiResultsetFullCSV(int baseResultset,
                                       int comparisonColumn,
                                       int maxWidthMean,
                                       int maxWidthStdDev) {
    StringBuffer result = new StringBuffer(1000);
    
    int resultsetLength = maxWidthMean + 7;
    if (m_ShowStdDevs) {
      resultsetLength += (maxWidthStdDev + 5);
    }
    
    // Set up the titles
    StringBuffer titles = new StringBuffer("Dataset");
    titles.append(",");
    StringBuffer label  = new StringBuffer(Utils.quote("(" + (baseResultset + 1)
                                           + ") "
                                           + getResultsetName(baseResultset)));
    
    titles.append(label);
    if (m_ShowStdDevs)
      titles.append(",")
            .append("StdDev");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
        label = new StringBuffer(Utils.quote("(" + (j + 1) + ") " + getResultsetName(j)));
        titles.append(",")
              .append(label)
              .append(",");
        if (m_ShowStdDevs)
          titles.append("StdDev")
                .append(",");
      }
    }
    result.append(titles).append('\n');
    
    // Iterate over datasets
    int [] win = new int [getNumResultsets()];
    int [] loss = new int [getNumResultsets()];
    int [] tie = new int [getNumResultsets()];
    StringBuffer skipped = new StringBuffer("");
    for (int i = 0; i < getNumDatasets(); i++) {
      // Print the name of the dataset
      String datasetName = 
        templateString(m_DatasetSpecifiers.specifier(i));
      try {
        PairedStats pairedStats = 
          calculateStatistics(m_DatasetSpecifiers.specifier(i), 
              baseResultset, baseResultset,
              comparisonColumn);
        result.append(Utils.quote(datasetName + Utils.doubleToString(pairedStats.count, 0)));
        if (!m_ShowStdDevs) {
          result.append(',')
                .append(padIt(pairedStats.xStats.mean, resultsetLength - 2, m_MeanPrec).trim());
        } 
        else {
          result.append(',')
                .append(padIt(pairedStats.xStats.mean, (maxWidthMean+5), m_MeanPrec).trim());
          if (Double.isInfinite(pairedStats.xStats.stdDev)) {
            result.append(',')
                  .append("Inf");
          } 
          else {
            result.append(',')
                  .append(padIt(pairedStats.xStats.stdDev, (maxWidthStdDev+3), m_StdDevPrec));
          }
        }
        // Iterate over the resultsets
        for (int j = 0; j < getNumResultsets(); j++) {
          if (!displayResultset(j))
            continue;
          if (j != baseResultset) {
            try {
              pairedStats = 
                calculateStatistics(m_DatasetSpecifiers.specifier(i), 
                    baseResultset, j, comparisonColumn);
              char sigChar = ' ';
              if (pairedStats.differencesSignificance < 0) {
                sigChar = 'v';
                win[j]++;
              } 
              else 
                if (pairedStats.differencesSignificance > 0) {
                  sigChar = '*';
                  loss[j]++;
                } 
                else {
                  tie[j]++;
                }
              if (!m_ShowStdDevs) {
                result.append(',')
                      .append(padIt(pairedStats.yStats.mean, resultsetLength - 2, m_MeanPrec).trim())
                      .append(',')
                      .append(sigChar);
              } 
              else {
                result.append(',')
                      .append(padIt(pairedStats.yStats.mean, (maxWidthMean+5), m_MeanPrec).trim());
                if (Double.isInfinite(pairedStats.yStats.stdDev)) {
                  result.append(',')
                        .append("Inf");
                } 
                else {
                  result.append(',')
                        .append(padIt(pairedStats.yStats.stdDev, (maxWidthStdDev+3), m_StdDevPrec).trim());
                }
                result.append(',')
                      .append(sigChar);
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }
        result.append('\n');
      } 
      catch (Exception ex) {
        ex.printStackTrace();
        if (skipped.length() > 0)
          skipped.append(',');
        skipped.append(datasetName);
      }
    }
    result.append(',')
          .append(Utils.quote("(v/ /*)"));
    if (m_ShowStdDevs)
      result.append(',');
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      if (j != baseResultset) {
        result.append(',');
        if (m_ShowStdDevs)
          result.append(',');
        result.append(',')
              .append(Utils.quote("(" + win[j] + '/' + tie[j]
                                      + '/' + loss[j] + ')'));
      }
    }
    result.append('\n');
    if (!skipped.equals("")) {
      result.append("Skipped: ").append(skipped).append('\n');
    }
    return result.toString();
  }
				    
  /**
   * Creates a comparison table where a base resultset is compared to the
   * other resultsets. Results are presented for every dataset.
   *
   * @param baseResultset the index of the base resultset
   * @param comparisonColumn the index of the column to compare over
   * @return the comparison table string
   * @exception Exception if an error occurs
   */
  public String multiResultsetFull(int baseResultset,
				   int comparisonColumn) throws Exception {

    int maxWidthMean = 2;
    int maxWidthStdDev = 2;
     // determine max field width
    for (int i = 0; i < getNumDatasets(); i++) {
      for (int j = 0; j < getNumResultsets(); j++) {
        if (!displayResultset(j))
          continue;
	try {
	  PairedStats pairedStats = 
	    calculateStatistics(m_DatasetSpecifiers.specifier(i), 
				baseResultset, j, comparisonColumn);
          if (!Double.isInfinite(pairedStats.yStats.mean) &&
              !Double.isNaN(pairedStats.yStats.mean)) {
            double width = ((Math.log(Math.abs(pairedStats.yStats.mean)) / 
                             Math.log(10))+1);
            if (width > maxWidthMean) {
              maxWidthMean = (int)width;
            }
          }
	  
	  if (m_ShowStdDevs &&
              !Double.isInfinite(pairedStats.yStats.stdDev) &&
              !Double.isNaN(pairedStats.yStats.stdDev)) {
	    double width = ((Math.log(Math.abs(pairedStats.yStats.stdDev)) / 
                             Math.log(10))+1);
	    if (width > maxWidthStdDev) {
	      maxWidthStdDev = (int)width;
	    }
	  }
	}  catch (Exception ex) {
	  ex.printStackTrace();
	}
      }
    }

    StringBuffer result = new StringBuffer(1000);

    if (m_latexOutput) {
      result = new StringBuffer(multiResultsetFullLatex(baseResultset, 
							comparisonColumn, 
							maxWidthMean,
							maxWidthStdDev));

    } else if (m_csvOutput) {
      result = new StringBuffer(multiResultsetFullCSV(baseResultset, 
                     comparisonColumn, 
                     maxWidthMean,
                     maxWidthStdDev));
        
     } else {
      result = new StringBuffer(multiResultsetFullPlainText(baseResultset, 
                                                            comparisonColumn, 
                                                            maxWidthMean,
                                                            maxWidthStdDev));
    }
    // append a key so that we can tell the difference between long
    // scheme+option names
    result.append("\nKey:\n\n");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (!displayResultset(j))
        continue;
      result.append("("+(j+1)+") ");
      result.append(getResultsetName(j)+"\n");
    }
    return result.toString();
  }

  /**
   * Lists options understood by this object.
   *
   * @return an enumeration of Options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
             "\tSpecify list of columns that specify a unique\n"
	      + "\tdataset.\n"
	      + "\tFirst and last are valid indexes. (default none)",
              "D", 1, "-D <index,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tSet the index of the column containing the run number",
              "R", 1, "-R <index>"));
    newVector.addElement(new Option(
	      "\tSet the index of the column containing the fold number",
              "F", 1, "-F <index>"));
    newVector.addElement(new Option(
              "\tSpecify list of columns that specify a unique\n"
	      + "\t'result generator' (eg: classifier name and options).\n"
	      + "\tFirst and last are valid indexes. (default none)",
              "G", 1, "-G <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tSet the significance level for comparisons (default 0.05)",
              "S", 1, "-S <significance level>"));
    newVector.addElement(new Option(
	      "\tShow standard deviations",
              "V", 0, "-V"));
    newVector.addElement(new Option(
	      "\tProduce table comparisons in Latex table format",
              "L", 0, "-L"));
    newVector.addElement(new Option(
         "\tProduce table comparisons in CSV table format",
         "csv", 0, "-csv"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D num,num2... <br>
   * The column numbers that uniquely specify a dataset.
   * (default last) <p>
   *
   * -R num <br>
   * The column number containing the run number.
   * (default last) <p>
   *
   * -F num <br>
   * The column number containing the fold number.
   * (default none) <p>
   * 
   * -S num <br>
   * The significance level for T-Tests.
   * (default 0.05) <p>
   *
   * -G num,num2... <br>
   * The column numbers that uniquely specify one result generator (eg:
   * scheme name plus options).
   * (default last) <p>
   *
   * -V <br>
   * Show standard deviations <p>
   *
   * -L <br>
   * Produce comparison tables in Latex table format <p>
   *
   * -csv <br>
   * Produce comparison tables in csv format <p>
   *
   * @param options an array containing options to set.
   * @exception Exception if invalid options are given
   */
  public void setOptions(String[] options) throws Exception {

    setShowStdDevs(Utils.getFlag('V', options));
    setProduceLatex(Utils.getFlag('L', options));
    setProduceCSV(Utils.getFlag("csv", options));

    String datasetList = Utils.getOption('D', options);
    Range datasetRange = new Range();
    if (datasetList.length() != 0) {
      datasetRange.setRanges(datasetList);
    }
    setDatasetKeyColumns(datasetRange);

    String indexStr = Utils.getOption('R', options);
    if (indexStr.length() != 0) {
      if (indexStr.equals("first")) {
	setRunColumn(0);
      } else if (indexStr.equals("last")) {
	setRunColumn(-1);
      } else {
	setRunColumn(Integer.parseInt(indexStr) - 1);
      }    
    } else {
      setRunColumn(-1);
    }

    String foldStr = Utils.getOption('F', options);
    if (foldStr.length() != 0) {
      setFoldColumn(Integer.parseInt(foldStr) - 1);
    } else {
      setFoldColumn(-1);
    }

    String sigStr = Utils.getOption('S', options);
    if (sigStr.length() != 0) {
      setSignificanceLevel((new Double(sigStr)).doubleValue());
    } else {
      setSignificanceLevel(0.05);
    }
    
    String resultsetList = Utils.getOption('G', options);
    Range generatorRange = new Range();
    if (resultsetList.length() != 0) {
      generatorRange.setRanges(resultsetList);
    }
    setResultsetKeyColumns(generatorRange);
  }
  
  /**
   * Gets current settings of the PairedTTester.
   *
   * @return an array of strings containing current options.
   */
  public String[] getOptions() {

    String [] options = new String [10];
    int current = 0;

    if (!getResultsetKeyColumns().getRanges().equals("")) {
      options[current++] = "-G";
      options[current++] = getResultsetKeyColumns().getRanges();
    }
    if (!getDatasetKeyColumns().getRanges().equals("")) {
      options[current++] = "-D";
      options[current++] = getDatasetKeyColumns().getRanges();
    }
    options[current++] = "-R";
    options[current++] = "" + (getRunColumn() + 1);
    options[current++] = "-S";
    options[current++] = "" + getSignificanceLevel();
    
    if (getShowStdDevs()) {
      options[current++] = "-V";
    }

    if (getProduceLatex()) {
      options[current++] = "-L";
    }

    if (getProduceCSV()) {
      options[current++] = "-csv";
    }
   
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the value of ResultsetKeyColumns.
   *
   * @return Value of ResultsetKeyColumns.
   */
  public Range getResultsetKeyColumns() {
    
    return m_ResultsetKeyColumnsRange;
  }
  
  /**
   * Set the value of ResultsetKeyColumns.
   *
   * @param newResultsetKeyColumns Value to assign to ResultsetKeyColumns.
   */
  public void setResultsetKeyColumns(Range newResultsetKeyColumns) {
    
    m_ResultsetKeyColumnsRange = newResultsetKeyColumns;
    m_ResultsetsValid = false;
  }
  
  /**
   * Gets the indices of the the datasets that are displayed (if <code>null</code>
   * then all are displayed). The base is always displayed.
   * 
   * @return the indices of the datasets to display
   */
  public int[] getDisplayedResultsets() {
    return m_DisplayedResultsets;
  }
  
  /**
   * Sets the indicies of the datasets to display (<code>null</code> means all).
   * The base is always displayed.
   * 
   * @param cols the indices of the datasets to display
   */
  public void setDisplayedResultsets(int[] cols) {
    m_DisplayedResultsets = cols;
  }
  
  /**
   * Get the value of SignificanceLevel.
   *
   * @return Value of SignificanceLevel.
   */
  public double getSignificanceLevel() {
    
    return m_SignificanceLevel;
  }
  
  /**
   * Set the value of SignificanceLevel.
   *
   * @param newSignificanceLevel Value to assign to SignificanceLevel.
   */
  public void setSignificanceLevel(double newSignificanceLevel) {
    
    m_SignificanceLevel = newSignificanceLevel;
  }

  /**
   * Get the value of DatasetKeyColumns.
   *
   * @return Value of DatasetKeyColumns.
   */
  public Range getDatasetKeyColumns() {
    
    return m_DatasetKeyColumnsRange;
  }
  
  /**
   * Set the value of DatasetKeyColumns.
   *
   * @param newDatasetKeyColumns Value to assign to DatasetKeyColumns.
   */
  public void setDatasetKeyColumns(Range newDatasetKeyColumns) {
    
    m_DatasetKeyColumnsRange = newDatasetKeyColumns;
    m_ResultsetsValid = false;
  }
  
  /**
   * Get the value of RunColumn.
   *
   * @return Value of RunColumn.
   */
  public int getRunColumn() {
    
    return m_RunColumnSet;
  }
  
  /**
   * Set the value of RunColumn.
   *
   * @param newRunColumn Value to assign to RunColumn.
   */
  public void setRunColumn(int newRunColumn) {
    
    m_RunColumnSet = newRunColumn;
    m_ResultsetsValid = false;
  }

  /**
   * Get the value of FoldColumn.
   *
   * @return Value of FoldColumn.
   */
  public int getFoldColumn() {
    
    return m_FoldColumn;
  }
  
  /**
   * Set the value of FoldColumn.
   *
   * @param newFoldColumn Value to assign to FoldColumn.
   */
  public void setFoldColumn(int newFoldColumn) {
    
    m_FoldColumn = newFoldColumn;
    m_ResultsetsValid = false;
  }
  
  /**
   * Get the value of Instances.
   *
   * @return Value of Instances.
   */
  public Instances getInstances() {
    
    return m_Instances;
  }
  
  /**
   * Set the value of Instances.
   *
   * @param newInstances Value to assign to Instances.
   */
  public void setInstances(Instances newInstances) {
    
    m_Instances = newInstances;
    m_ResultsetsValid = false;
  }
  
  /**
   * Test the class from the command line.
   *
   * @param args contains options for the instance ttests
   */
  public static void main(String args[]) {

    try {
      PairedTTester tt = new PairedTTester();
      String datasetName = Utils.getOption('t', args);
      String compareColStr = Utils.getOption('c', args);
      String baseColStr = Utils.getOption('b', args);
      boolean summaryOnly = Utils.getFlag('s', args);
      boolean rankingOnly = Utils.getFlag('r', args);
      try {
	if ((datasetName.length() == 0)
	    || (compareColStr.length() == 0)) {
	  throw new Exception("-t and -c options are required");
	}
	tt.setOptions(args);
	Utils.checkForRemainingOptions(args);
      } catch (Exception ex) {
	String result = "";
	Enumeration enu = tt.listOptions();
	while (enu.hasMoreElements()) {
	  Option option = (Option) enu.nextElement();
	  result += option.synopsis() + '\n'
	    + option.description() + '\n';
	}
	throw new Exception(
	      "Usage:\n\n"
	      + "-t <file>\n"
	      + "\tSet the dataset containing data to evaluate\n"
	      + "-b <index>\n"
	      + "\tSet the resultset to base comparisons against (optional)\n"
	      + "-c <index>\n"
	      + "\tSet the column to perform a comparison on\n"
	      + "-s\n"
	      + "\tSummarize wins over all resultset pairs\n\n"
	      + "-r\n"
	      + "\tGenerate a resultset ranking\n\n"
	      + result);
      }
      Instances data = new Instances(new BufferedReader(
				  new FileReader(datasetName)));
      tt.setInstances(data);
      //      tt.prepareData();
      int compareCol = Integer.parseInt(compareColStr) - 1;
      System.out.println(tt.header(compareCol));
      if (rankingOnly) {
	System.out.println(tt.multiResultsetRanking(compareCol));
      } else if (summaryOnly) {
	System.out.println(tt.multiResultsetSummary(compareCol));
      } else {
	System.out.println(tt.resultsetKey());
	if (baseColStr.length() == 0) {
	  for (int i = 0; i < tt.getNumResultsets(); i++) {
            if (!tt.displayResultset(i))
              continue;
	    System.out.println(tt.multiResultsetFull(i, compareCol));
	  }
	} else {
	  int baseCol = Integer.parseInt(baseColStr) - 1;
	  System.out.println(tt.multiResultsetFull(baseCol, compareCol));
	}
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
