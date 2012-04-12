/*
 *    CfsSubsetEval.java
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
import weka.classifiers.*;
import weka.filters.*;

/** 
 * CFS attribute subset evaluator
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */

public class CfsSubsetEval
  extends SubsetEvaluator
  implements OptionHandler {

   /** training instances */
  private Instances trainInstances;

  /** discretise attributes when class in nominal */
  private DiscretizeFilter disTransform;

  /** class index */
  private int classIndex;

  /** is the class numeric */
  private boolean isNumeric;

  /** number of attributes in the training data */
  private int numAttribs;

  /** number of instances in the training data */
  private int numInstances;

  /** treat missing values as seperate values */
  private boolean missingSeperate;

  /** include locally predicitive attributes */
  private boolean locallyPredictive;

  /** holds the matrix of attribute correlations */
  private Matrix corr_matrix;

  /** standard deviations of attributes (when using pearsons correlation) */
  private double [] std_devs;

  private double c_Threshold;

  public CfsSubsetEval()
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
    
    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
				    "\tTreat missing values as a seperate"
				    +"\n\tvalue.",
				    "M", 0,"-M"));
     newVector.addElement(new Option(
				    "\tInclude locally predictive attributes"
				    +".",
				    "L", 0,"-L"));

     return newVector.elements();
  }

   /**
   * Parses and sets a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception
  {
    String optionString;

    resetOptions();
    missingSeperate = Utils.getFlag('M', options);
    locallyPredictive = Utils.getFlag('L',options);
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] options = new String [2];
    int current = 0;

    if (missingSeperate)
      {
	options[current++] = "-M";
      }

     if (locallyPredictive)
      {
	options[current++] = "-L";
      }

    while (current < options.length) 
      {
	options[current++] = "";
      }

    return options;
  }


  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * For CFS also discretises attributes (if necessary) and initializes
   * the correlation matrix.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data)  throws Exception
  { 
    if (data.checkForStringAttributes()) 
      {
	throw new Exception("Can't handle string attributes!");
      }

    trainInstances = data;
    classIndex = trainInstances.classIndex();
    numAttribs = trainInstances.numAttributes();
    numInstances = trainInstances.numInstances();
    isNumeric = trainInstances.attribute(classIndex).isNumeric();

    if (!isNumeric)
      {
	disTransform = new DiscretizeFilter();
	disTransform.setUseBetterEncoding(true);
	disTransform.inputFormat(trainInstances);
	trainInstances = Filter.useFilter(trainInstances, disTransform);
      }

    std_devs = new double [numAttribs];
    corr_matrix = new Matrix(numAttribs,numAttribs);

    for(int i = 0; i < corr_matrix.numRows(); i++)
      {
	corr_matrix.setElement(i,i,1.0);
	std_devs[i] = 1.0;
      }

    for (int i = 0; i<numAttribs;i++)
      for (int j=i+1;j<numAttribs;j++)
	{
	  corr_matrix.setElement(i,j,-999);
	  corr_matrix.setElement(j,i,-999);
	}
  }

  /**
   * evaluates a subset of attributes
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @exception Exception if the subset could not be evaluated
   */
  public double evaluateSubset(BitSet subset) throws Exception
  {
    double num = 0.0;
    double denom = 0.0;
    double corr;

    // do numerator
    for (int i=0;i < numAttribs; i++)
      if (i != classIndex)
	if (subset.get(i))
	  {
	    if (corr_matrix.getElement(i,classIndex) == -999)
	      {
		corr = correlate(i,classIndex);
		corr_matrix.setElement(i,classIndex,corr);
		corr_matrix.setElement(classIndex,i,corr);
		num+=(std_devs[i] * corr);
	      }
	    else
	      num += (std_devs[i] * corr_matrix.getElement(i,classIndex));
	  }

    // do denominator
    for (int i=0; i<numAttribs;i++)
      if (i != classIndex)
	if (subset.get(i))
	  {
	    denom += (1.0 * std_devs[i] * std_devs[i]);
	    for (int j=i+1;j<numAttribs;j++)
	      if (subset.get(j))
		{
		  if (corr_matrix.getElement(i,j) == -999)
		    {
		      corr = correlate(i,j);
		      corr_matrix.setElement(i,j,corr);
		      corr_matrix.setElement(j,i,corr);
		      denom += (2.0 * std_devs[i] * std_devs[j] * corr);
		    }
		  else
		    denom += (2.0 * std_devs[i] * std_devs[j] * 
			      corr_matrix.getElement(i,j));
		}
	  }
    
    if (denom < 0.0)
      denom *= -1.0;
    if (denom == 0.0)
      return (0.0);
    
    double merit = (num / Math.sqrt(denom));
    if (merit < 0.0)
      merit *= -1.0;

    return merit;
  }


  private double correlate(int att1, int att2)
  {
    if (!isNumeric)
      return symmUncertCorr(att1,att2);

    boolean att1_is_num = (trainInstances.attribute(att1).isNumeric());
    boolean att2_is_num = (trainInstances.attribute(att2).isNumeric());
    
    if (att1_is_num && att2_is_num)
      return num_num(att1,att2);
    else if (att2_is_num)
      return num_nom2(att1,att2);
    else if (att1_is_num)
      return num_nom2(att2,att1);
    
    return nom_nom(att1,att2);
  }

  private double symmUncertCorr(int att1, int att2)
  {
    int i,j,k,ii,jj;
    int nnj,nni,ni,nj;
    double sum = 0.0;
    double  sumi[],sumj[];
    double counts[][];
    Instance inst;
    double corr_measure;
    boolean flag = false;
    double temp = 0.0;

    if (att1 == classIndex || att2 == classIndex)
      flag = true;

    ni = trainInstances.attribute(att1).numValues() + 1;
    nj = trainInstances.attribute(att2).numValues() + 1;
    counts = new double[ni][nj];
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

       if (inst.isMissing(att1))
	 ii = ni-1;
       else
	 ii = (int) inst.value(att1);

       if (inst.isMissing(att2))
	 jj = nj-1;
       else
	 jj = (int) inst.value(att2);

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
    if (!missingSeperate)
    {
      double [] i_copy = new double [sumi.length], j_copy = new double [sumj.length];
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
		temp =  (counts_copy[i][j] / (sum - total_missing)) * counts_copy[ni-1][nj-1];
	      counts[i][j] += temp;
	      sumi[i] += temp;
	      sumj[j] += temp;
	      }
	  counts[ni-1][nj-1] = 0.0;
	}
    }
   
    // corr_measure = Correlate.symm_uncert(counts,sumi,sumj,sum,ni,nj,flag);
    corr_measure = ContingencyTables.symmetricalUncertainty(counts);
    // corr_measure = ContingencyTables.gainRatio(counts);

    if (Utils.eq(corr_measure,0.0))
      {
	if (flag == true)
	  return (0.0);
	else
	  return (1.0);
      }
      else
	return (corr_measure);

  }

  private double num_num(int att1, int att2)
  {
    int i;
    Instance inst;
    double r, diff1, diff2, num = 0.0, sx = 0.0, sy = 0.0;
    double mx = trainInstances.meanOrMode(trainInstances.attribute(att1)), my = trainInstances.meanOrMode(trainInstances.attribute(att2));

    for (i=0;i<numInstances;i++)
      {
	inst = trainInstances.instance(i);
      
	diff1 = (inst.isMissing(att1)) ? 0.0 : (inst.value(att1) - mx);
	diff2 = (inst.isMissing(att2)) ? 0.0 : (inst.value(att2) - my);

	num += (diff1 * diff2);
	sx += (diff1 * diff1);
	sy += (diff2 * diff2);
      }

    if (sx != 0.0)
      if (std_devs[att1] == 1.0)
	std_devs[att1] = Math.sqrt((sx / numInstances));
    if (sy != 0.0)
      if (std_devs[att2] == 1.0)
	std_devs[att2] = Math.sqrt((sy / numInstances));

    if ((sx * sy) > 0.0)
      {
	r = (num / (Math.sqrt(sx*sy)));

	return ((r < 0.0) ? -r : r);
      }
    else
      {
	if (att1 != classIndex && att2 != classIndex)
	  return 1.0;
	else
	  return 0.0;
      }
  }

  private double num_nom2(int att1, int att2)
  {
    int i,ii,k;
    double temp;
    Instance inst;
    int mx = (int)trainInstances.meanOrMode(trainInstances.attribute(att1));
    double my = trainInstances.meanOrMode(trainInstances.attribute(att2));
    double stdv_num = 0.0;
    double diff1,diff2;
    double r = 0.0,rr,max_corr = 0.0;

    int nx = (!missingSeperate) ? 
      trainInstances.attribute(att1).numValues() : 
      trainInstances.attribute(att1).numValues()+1;

    double [] prior_nom = new double [nx];
    double [] stdvs_nom = new double [nx];
    double [] covs =  new double [nx];
    for (i=0;i<nx;i++)
      stdvs_nom[i] = covs[i] = prior_nom[i] = 0.0;

    // calculate frequencies (and means) of the values of the nominal 
    // attribute
    for (i=0;i<numInstances;i++)
      {
	inst = trainInstances.instance(i);
	
	if (inst.isMissing(att1))
	  {
	    if (!missingSeperate)
	      ii = mx;
	    else
	      ii = nx - 1;
	  }
	else
	  ii = (int) inst.value(att1);

	// increment freq for nominal
	prior_nom[ii]++;
      }

    for (k=0;k<numInstances;k++)
      {
	inst = trainInstances.instance(k);

	// std dev of numeric attribute
	diff2 = (inst.isMissing(att2)) ? 0.0 : (inst.value(att2) - my);
	stdv_num += (diff2 * diff2);

	// 
	for (i=0;i<nx;i++)
	  {
	    if (inst.isMissing(att1))
	      {
		if (!missingSeperate)
		  temp = (i == mx) ? 1.0 : 0.0;
		else
		  temp = (i == (nx-1)) ? 1.0 : 0.0;
	      }
	    else
	      temp = (i == inst.value(att1)) ? 1.0 : 0.0;

	    diff1 = (temp - (prior_nom[i] / numInstances));
	     
	    stdvs_nom[i] += (diff1 * diff1);
	    covs[i] += (diff1 * diff2);
	  }
      }

    // calculate weighted correlation
    for (i=0,temp=0.0;i<nx;i++)
      {
	// calculate the weighted variance of the nominal
	temp += ((prior_nom[i] / numInstances) * (stdvs_nom[i] / numInstances));

	if ((stdvs_nom[i] * stdv_num) > 0.0)
	  {
	    //System.out.println("Stdv :"+stdvs_nom[i]);
	    rr = (covs[i] / (Math.sqrt(stdvs_nom[i] * stdv_num)));
	    if (rr < 0.0)
	      rr = -rr;

	    r += ((prior_nom[i] / numInstances) * rr);
	  }
	/* if there is zero variance for the numeric att at a specific 
	   level of the catergorical att then if neither is the class then 
	   make this correlation at this level maximally bad i.e. 1.0. 
	   If either is the class then maximally bad correlation is 0.0 */
	else if (att1 != classIndex && att2 != classIndex)
	  r += ((prior_nom[i] / numInstances) * 1.0);
      }

    // set the standard deviations for these attributes if necessary
    // if ((att1 != classIndex) && (att2 != classIndex)) // =============
    if (temp != 0.0)
      if (std_devs[att1] == 1.0)
	std_devs[att1] = Math.sqrt(temp);

    if (stdv_num != 0.0)
      if (std_devs[att2] == 1.0)
	std_devs[att2] = Math.sqrt((stdv_num / numInstances));

    if (r == 0.0)
      if (att1 != classIndex && att2 != classIndex)
	r = 1.0;

    return r;
  }
  
  private double nom_nom(int att1, int att2)
  {
    int i,j,ii,jj,z;
    double temp1,temp2;
    Instance inst;
    int mx = (int)trainInstances.meanOrMode(trainInstances.attribute(att1));
    int my = (int)trainInstances.meanOrMode(trainInstances.attribute(att2));
    double diff1,diff2;
    double r = 0.0,rr, max_corr = 0.0;

    int nx = (!missingSeperate) ? trainInstances.attribute(att1).numValues() : trainInstances.attribute(att1).numValues()+1;
    int ny = (!missingSeperate) ? trainInstances.attribute(att2).numValues() : trainInstances.attribute(att2).numValues()+1;

    double [][] prior_nom = new double [nx][ny];
    double [] sumx = new double [nx];
    double [] sumy = new double [ny];
    double [] stdvsx = new double [nx];
    double [] stdvsy = new double [ny];
    double [][] covs =  new double [nx][ny];
    for(i=0;i<nx;i++)
      sumx[i] = stdvsx[i] = 0.0;
    for (j=0;j<ny;j++)
      sumy[j] = stdvsy[j] = 0.0;
    for (i=0;i<nx;i++)
      for (j=0;j<ny;j++)
	covs[i][j] = prior_nom[i][j] = 0.0;

    // calculate frequencies (and means) of the values of the nominal 
    // attribute
    for (i=0;i<numInstances;i++)
      {
	inst = trainInstances.instance(i);
	
	if (inst.isMissing(att1))
	  {
	    if (!missingSeperate)
	      ii = mx;
	    else
	      ii = nx - 1;
	  }
	else
	  ii = (int) inst.value(att1);

	if (inst.isMissing(att2))
	  {
	    if (!missingSeperate)
	      jj = my;
	    else
	      jj = ny - 1;
	  }
	else
	  jj = (int) inst.value(att2);

	// increment freq for nominal
	prior_nom[ii][jj]++;
	sumx[ii]++;
	sumy[jj]++;
      }

    for (z=0;z<numInstances;z++)
      {
	inst = trainInstances.instance(z);
   
	for (j=0;j<ny;j++)
	  {
	    if (inst.isMissing(att2))
	      {
		if (!missingSeperate)
		  temp2 = (j == my) ? 1.0 : 0.0;
		else
		  temp2 = (j == (ny-1)) ? 1.0 : 0.0;
	      }
	    else
	      temp2 = (j == inst.value(att2)) ? 1.0 : 0.0;

	    diff2 = (temp2 - (sumy[j] / numInstances));
	    stdvsy[j] += (diff2 * diff2);
	  }

	// 
	for (i=0;i<nx;i++)
	  {
	    if (inst.isMissing(att1))
	      {
		if (!missingSeperate)
		  temp1 = (i == mx) ? 1.0 : 0.0;
		else
		  temp1 = (i == (nx-1)) ? 1.0 : 0.0;
	      }
	    else
	      temp1 = (i == inst.value(att1)) ? 1.0 : 0.0;

	    diff1 = (temp1 - (sumx[i] / numInstances));
	    stdvsx[i] += (diff1 * diff1);

	    for (j=0;j<ny;j++)
	      {
		if (inst.isMissing(att2))
		  {
		    if (!missingSeperate)
		      temp2 = (j == my) ? 1.0 : 0.0;
		    else
		      temp2 = (j == (ny-1)) ? 1.0 : 0.0;
		  }
		else
		  temp2 = (j == inst.value(att2)) ? 1.0 : 0.0;

		diff2 = (temp2 - (sumy[j] / numInstances));
		
		covs[i][j] += (diff1 * diff2);
	      }
	  }
      }

    // calculate weighted correlation
    for (i=0;i<nx;i++)
      for (j=0;j<ny;j++)
	if ((stdvsx[i] * stdvsy[j]) > 0.0)
	  {
	    //System.out.println("Stdv :"+stdvs_nom[i]);
	    rr = (covs[i][j] / (Math.sqrt(stdvsx[i] * stdvsy[j])));
	    if (rr < 0.0)
	      rr = -rr;
	    
	    r += ((prior_nom[i][j] / numInstances) * rr);
	  }
    // if there is zero variance for either of the categorical atts then if
    // neither is the class then make this
    // correlation at this level maximally bad i.e. 1.0. If either is 
    // the class then maximally bad correlation is 0.0
	else if (att1 != classIndex && att2 != classIndex)
	  r += ((prior_nom[i][j] / numInstances) * 1.0);

    // calculate weighted standard deviations for these attributes
    // (if necessary)
   
    for (i=0,temp1=0.0;i<nx;i++)
      temp1 += ((sumx[i] / numInstances) * (stdvsx[i] / numInstances));
    if (temp1 != 0.0)
      if (std_devs[att1] == 1.0)
	std_devs[att1] = Math.sqrt(temp1);

    for (j=0,temp2=0.0;j<ny;j++)
      temp2 += ((sumy[j] / numInstances) * (stdvsy[j] / numInstances));
    if (temp2 != 0.0)
      if (std_devs[att2] == 1.0)
	std_devs[att2] = Math.sqrt(temp2);

    if (r == 0.0)
      if (att1 != classIndex && att2 != classIndex)
	r = 1.0;

    return r;
  }
  
   /**
   * returns a string describing CFS
   *
   * @return the description as a string
   */
  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    text.append("\tCFS Subset Evaluator\n");
    if (missingSeperate)
      text.append("\tTreating missing values as a seperate value\n");
    if (locallyPredictive)
      text.append("\tIncluding locally predictive attributes\n");

    return text.toString();
  }

  private void addLocallyPredictive(BitSet best_group)
  {
    int i,j;
    boolean done = false;
    boolean ok = true;
    double temp_best = -1.0;
    double corr;
    j = 0;
    BitSet temp_group = (BitSet)best_group.clone();

    while (!done)
      {
	temp_best = -1.0;
	// find best not already in group
	for (i=0;i<numAttribs;i++)
	  if ((!temp_group.get(i)) && (i != classIndex))
	    {
	      if (corr_matrix.getElement(i,classIndex) == -999)
		{
		  corr = correlate(i,classIndex);
		  corr_matrix.setElement(i,classIndex,corr);
		  corr_matrix.setElement(classIndex,i,corr);
		}
		  
	      if (corr_matrix.getElement(i,classIndex) > temp_best)
		{
		  temp_best = corr_matrix.getElement(i,classIndex);
		  j = i;
		}
	    }

	if (temp_best == -1.0)
	  done = true;
	else
	  {
	    ok = true;
	    temp_group.set(j);
	    // check the best against correlations with others already
	    // in group 
	    for (i = 0;i<numAttribs;i++)
	      if (best_group.get(i))
		{
		  if (corr_matrix.getElement(i,j) == -999)
		    {
		      corr = correlate(i,j);
		      corr_matrix.setElement(i,j,corr);
		      corr_matrix.setElement(j,i,corr);
		    }

		  if (corr_matrix.getElement(i,j) > temp_best - c_Threshold)
		    {
		      ok = false;
		      break;
		    }
		}
	    // if ok then add to best_group
	    if (ok)
	      {
		best_group.set(j);
	      }
	  }
      }
  }


  /**
   * Calls locallyPredictive in order to include locally predictive
   * attributes (if requested).
   *
   * @param attributeSet the set of attributes found by the search
   * @return a possibly ranked list of postprocessed attributes
   * @exception Exception if postprocessing fails for some reason
   */
  public int [] postProcess(int [] attributeSet) throws Exception
  {
    int j = 0;
    if (!locallyPredictive)
      return attributeSet;

    BitSet bestGroup = new BitSet(numAttribs);
    for (int i=0;i<attributeSet.length;i++)
      bestGroup.set(attributeSet[i]);
    
    addLocallyPredictive(bestGroup);

    // count how many are set
    for (int i=0;i<numAttribs;i++)
      if (bestGroup.get(i))
	j++;
    int [] newSet = new int [j];
    j=0;
    for (int i=0; i<numAttribs;i++)
      if (bestGroup.get(i))
	newSet[j++] = i;

    return newSet;
  }

  protected void resetOptions()
  {
    missingSeperate = false;
    locallyPredictive = false;
    c_Threshold = 0.0;
  }

    /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file
   */
  
  public static void main(String [] argv)
  {
    
    try {
      System.out.println(AttributeSelection.SelectAttributes(new CfsSubsetEval(), argv));
    }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
}
