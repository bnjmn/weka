/*
 *    SymmetricalUncertAttributeEval.java
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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.*;

/** 
 * Class for Evaluating attributes individually by measuring information gain 
 * with respect to the class.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */
public class SymmetricalUncertAttributeEval 
  extends AttributeEvaluator
  implements OptionHandler {


  private Instances trainInstances;

  private int classIndex;
  
  private int numAttribs;

  private int numInstances;

  private int numClasses;

  private boolean missing_merge;


  public SymmetricalUncertAttributeEval ()
  {
    resetOptions();
  }


  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   *
   **/
  public Enumeration listOptions() 
  {
    
    Vector newVector = new Vector(1);
    
    newVector.addElement(new Option("\ttreat missing values as a seperate value.", "M", 0,"-M"));

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
    resetOptions();
    missing_merge = !(Utils.getFlag('M',options));
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] options = new String [1];
    int current = 0;

    if (!missing_merge)
      {
	options[current++] = "-M";
      }

    while (current < options.length) 
      {
	options[current++] = "";
      }

    return options;

  }

  /**
   * Initializes a symmetrical uncertainty attribute evaluator. 
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception
  {
    if (data.checkForStringAttributes()) 
      {
	throw new Exception("Can't handle string attributes!");
      }

    trainInstances = data;
    classIndex = trainInstances.classIndex();
    numAttribs = trainInstances.numAttributes();
    numInstances = trainInstances.numInstances();

    if (trainInstances.attribute(classIndex).isNumeric())
      throw new Exception("Class must be nominal!");

    DiscretizeFilter disTransform = new DiscretizeFilter();
    disTransform.setUseBetterEncoding(true);
    disTransform.inputFormat(trainInstances);
    trainInstances = Filter.useFilter(trainInstances, disTransform);

    numClasses = trainInstances.attribute(classIndex).numValues();
  }


  protected void resetOptions()
  {
    trainInstances = null;
    missing_merge = true;
  }

  /**
   * evaluates an individual attribute by measuring the symmetrical
   * uncertainty between it and the class.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception
  {
    int i,j,ii,jj;
    int nnj,nni,ni,nj;
    double sum = 0.0;
    ni = trainInstances.attribute(attribute).numValues() + 1;
    nj = numClasses + 1;
    double [] sumi, sumj;
    Instance inst;
    double temp = 0.0;
    sumi = new double[ni];
    sumj = new double[nj];

    double [][] counts = new double[ni][nj];

    sumi = new double[ni];
    sumj = new double[nj];
 
    for (i=0;i<ni;i++)
      {
	sumi[i] = 0.0;
	for(j=0;j<nj;j++)
	  {
	    sumj[j] = 0.0;
	    counts[i][j] = 0.0;
	  }
      }
    
    // Fill the contingency table
    for (i=0;i<numInstances;i++)
      {
	inst = trainInstances.instance(i);

	if (inst.isMissing(attribute))
	  ii = ni-1;
	else
	  ii = (int) inst.value(attribute);

	if (inst.isMissing(classIndex))
	  jj = nj-1;
	else
	  jj = (int) inst.value(classIndex);

	counts[ii][jj]++;
      }

    // get the row totals
    for (i=0;i<ni;i++)
      {
	sumi[i] = 0.0;
	for(j=0;j<nj;j++)
	  {
	    sumi[i] += counts[i][j];
	    sum += counts[i][j];
	  }
      }

    // get the column totals
    for (j=0;j<nj;j++)
      {
	sumj[j] = 0.0;
	for(i=0;i<ni;i++)
	  {
	    sumj[j] += counts[i][j];
	  }
      }

    // distribute missing counts
    if (missing_merge)
      {
	double [] i_copy = new double [sumi.length], 
	  j_copy = new double [sumj.length];
	double [][] counts_copy = new double [sumi.length][sumj.length];
	for (i=0;i<ni;i++)
	  System.arraycopy(counts[i],0,counts_copy[i],0,sumj.length);

	System.arraycopy(sumi,0,i_copy,0,sumi.length);
	System.arraycopy(sumj,0,j_copy,0,sumj.length);
	double total_missing = (sumi[ni-1]+sumj[nj-1]-counts[ni-1][nj-1]);
     
	// do the missing i's
	if (sumi[ni-1] > 0.0)
	  for (j=0;j<nj-1;j++)
	    if (counts[ni-1][j] > 0.0)
	      {
		for (i=0;i<ni-1;i++)
		  {
		    temp = ((i_copy[i]/(sum-i_copy[ni-1]))*counts[ni-1][j]);
		    counts[i][j] += temp;
		    sumi[i] += temp;
		  }
		counts[ni-1][j] = 0.0;
	      }
	sumi[ni-1] = 0.0;
     
	// do the missing j's
	if (sumj[nj-1] > 0.0)
	  for (i=0;i<ni-1;i++)
	    if (counts[i][nj-1] > 0.0)
	      {
		for (j=0;j<nj-1;j++)
		  {
		    temp = ((j_copy[j]/(sum-j_copy[nj-1]))*counts[i][nj-1]);
		    counts[i][j] += temp;
		    sumj[j] += temp;
		  }
		counts[i][nj-1] = 0.0;
	      }
	sumj[nj-1] = 0.0;

	// do the both missing
	if (counts[ni-1][nj-1] > 0.0)
	  {
	    for (i=0;i<ni-1;i++)
	      for (j=0;j<nj-1;j++)
		{
		  temp =  (counts_copy[i][j] / (sum - total_missing)) * 
		    counts_copy[ni-1][nj-1];
		  counts[i][j] += temp;
		  sumi[i] += temp;
		  sumj[j] += temp;
		}
	    counts[ni-1][nj-1] = 0.0;
	  }
      }
    
    return ContingencyTables.symmetricalUncertainty(counts);
  }

  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    text.append("\tSymmetrical Uncertainty Ranking Filter");
    if (!missing_merge)
      text.append("\n\tMissing values treated as seperate");

    text.append("\n");

    return text.toString();
  }

  // ============
  // Test method.
  // ============
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file
   */
  
  public static void main(String [] argv)
  {
    
    try {
      System.out.println(AttributeSelection.SelectAttributes(new SymmetricalUncertAttributeEval(), argv));
    }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
  

}
