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
 *    Windowise.java
 *    Copyright (C) Saket S Joshi
 *
 */

package weka.filters;

import weka.core.*;
import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * This filter takes encoded or unencoded files and 
 * windowises the data so as to encode the sequential nature 
 * of the data in the dataset. This filter cannot be used from 
 * the command line. 
 *
 * @author Saket S Joshi (joshi@cs.orst.edu) 
 * @version $Revision: 1.1 $ 
 */
public class Windowise extends Filter
{

  private int lic = 3;                          // The left input context
  private int ric = 3;				// Right input context
  private int roc = 3;				// Right output context
  private int loc = 0;				// Left output context
  private Instances m_Inst = null;


  public Windowise(int lic, int ric, int loc, int roc) throws Exception
  {

    this.lic = lic;
    this.ric = ric;
    this.loc = loc;
    this.roc = roc;
  }

  public Matrix getClassFeatureTable(int levels)
  {

    Matrix matrix = new Matrix(levels, levels);
    for(int j = 0; j < levels; j++)
      for(int k = 0; k < levels; k++)
	if(k == j){matrix.setElement(j, k, 1);}
	else{matrix.setElement(j, k, 0);}
    return (matrix);
  }

  /*
    inputFormat is a function which sets the formats of the input data and the output data. It also checks to see if the data is from a new batch. 
  */
  public boolean setInputFormat(Instances inst) throws Exception
  {
    super.setInputFormat(inst);
    setOutputFormat();
    m_Inst = new Instances(inst);
    return(true);
  }

  public void setOutputFormat()
  {
    FastVector newAtts = new FastVector(); // newAtts will later contain all the attributes in the output format
    FastVector binValues = new FastVector();

    String attName; // name of the attribute
    int start = 0;
	
    for(int i = 0; i < getInputFormat().numAttributes(); i++)
      {
	if(getInputFormat().attribute(i).isNominal())
	  {
	    attName = getInputFormat().attribute(i).name(); // This for loop is used for collecting all the attributes to be 
	    if (i == getInputFormat().classIndex()) 	    // used in the output format, in newAtts.
	      {
		start = loc + roc + 1;			
	      }	 									
	    else 
	      {
		start = lic + ric + 1;	
	      }	
	    FastVector values = new FastVector();
	    Enumeration e = getInputFormat().attribute(i).enumerateValues();
	    while(e.hasMoreElements())
	      {
		values.addElement(e.nextElement());
	      }			
	    for(int k = 0; k < start; k++) // For every attribute in the input format, the output will contain
	      {				   // (lic+ric+1)*inputLen[i] columns for that attribute. 
		newAtts.addElement(new Attribute(attName+k, values)); // No windowising is done for the numeric attributes because of the 
	      }							      // lack of the definition of an underscore or blank for a numeric	
	    // attribute.
				
	  }
	else
	  {
	    newAtts.addElement(getInputFormat().attribute(i).copy());
	  }	
      }


    Instances instances = new Instances(getInputFormat().relationName(), newAtts, 0); // New instances object is created from the array of attributes in newAtts
	
    int newClassIndex = (getInputFormat().classIndex()-2)*(lic+ric+1) + loc + 2; // The class index in the windowised data has to be set
    instances.setClassIndex(newClassIndex);
	
    setOutputFormat(instances);	// This function from the parent class sets the output format by copying 
  }

  /*
    Windowising is done by setting a the complete data sequentially in a "rawLine" and then moving the sliding window over it. The data here refers to the left contexts followed
    by the attribute values in that block (in sequence) followed by the right context. This indicates that a rawLine is required for every attribute in every block. Hence the
    following function which, given the block, returns the rawLines for all the attributes in an array, one array element representing one rawLine.
  */
  private double[][] getRawLines(Instances inst, int count)
  {
    double [][] rawLines = new double [getInputFormat().numAttributes()][];
    int left = 0, right = 0;
    int rawLineIndex;

    for(int i = 0; i < getInputFormat().numAttributes(); i++)
      {
	rawLineIndex = 0;
	if (inst.attribute(i).isNominal()) 
	  {
	    if (i != getInputFormat().classIndex()) 
	      {
		left = lic;
		right = ric;
	      }
	    else
	      {
		left = loc;
		right = roc;
	      }
			
				
	    rawLines[i] = new double[left + right + count];
	    double blankIndex = inst.attribute(i).numValues() - 1;
			
	    for(int j=0; j < left; j++)
	      {
		rawLines[i][rawLineIndex] = blankIndex;	// Setting the left context
		rawLineIndex++;
	      }
			
	    for(int j=0; j < count; j++)
	      {
		rawLines[i][rawLineIndex] = inst.instance(j).value(i); // Setting the values from the block in
		rawLineIndex++;	
	      }							       // proper sequence.
			
					
	    for(int j=0; j < right; j++)
	      {
		rawLines[i][rawLineIndex] = blankIndex;		       // Setting the right context.
		rawLineIndex++;
	      }
	  }	
	else
	  {
	    rawLines[i] = null;
	  }
      }
    return rawLines;
  }

  /** This is the primary function that is called to do the windowising from the 'filterFile' function in the parent class. */
  public boolean input(Instance instance) {return false;}


  public boolean batchFinished() throws Exception
  {
    if (getInputFormat() == null) {
      throw new Exception("No input instance format defined");	  // Cant go on as long as the input format is undefined.
    }

    m_Inst.sort(0);	
    int max  = (int)m_Inst.lastInstance().value(0);
    for(int i=1; (m_Inst.numInstances() > 0) && (i<= max); i++)		
      {
	Instances instances = new Instances(m_Inst, 0);					  // A new instances object is made to hold a block. Instances 
	while((m_Inst.numInstances() > 0) && ((int)m_Inst.firstInstance().value(0) == i)) // are added one by one until the block is fully added.
	  {												
	    instances.add(m_Inst.firstInstance()); // The first instance in the input copy is deleted after it
	    m_Inst.delete(0);	
	  }					   // has been added to the block. This is why a copy of the 
	                                           // input was required.
	instances.sort(1);
									
	convertInstances(instances); // This function converts an entire block and adds it to output
      }

    m_NewBatch = true; // Next batch will be new again
    return(numPendingOutput() != 0); // returns true if instances from input remain to be converted
  }

  /*
    This function takes every block in instances and adds the corresponding windowised block to output.
  */
  private void convertInstances(Instances instances) throws Exception
  {
    int count = instances.numInstances();// count is the number of instances in the block. 
    double [][] rawLines = getRawLines(instances, count); // The array rawLines will contain the raw lines for all attributes	
	
    for(int i = 0; i < count; i++)
      {
	Instance newInstance = new Instance(outputFormatPeek().numAttributes());		
	newInstance.setValue(0, instances.instance(i).value(0)); // The first 2 attributes of the output instance are set according
	newInstance.setValue(1, instances.instance(i).value(1)); // to the block number and the sequence number.
		
	int attSoFar = 0; // attSoFar is the index of the output instance 
	int correctionOffset = 0;
													
	for(int j = 0; j < getInputFormat().numAttributes(); j++)
	  {
	    int end = 0, begin = 0, start = 0; // The left context and the right context
													
	    if (rawLines[j] != null) 
	      {
		if (j != getInputFormat().classIndex()) 
		  {
		    start = lic + ric;
		  }	
		else
		  {
		    start = loc + roc;
		  }
					
		end = rawLines[j].length - 1 - i;
		begin = end - start;
	
		for(int k = begin; k<= end; k++)
		  {
		    newInstance.setValue(attSoFar, rawLines[j][k]);
		    attSoFar++;
		  }
	      }
	    else
	      {
		newInstance.setValue(attSoFar, instances.instance(i).value(j));
		attSoFar++;
	      }
	  }

	push(newInstance); // The newly created output instance is pushed into the output
      }
  }	

  public static void main(String args[]) 
  {
    try{
      int lic = Integer.parseInt(Utils.getOption('A', args));
      int ric = Integer.parseInt(Utils.getOption('B', args));
      int loc = Integer.parseInt(Utils.getOption('Y', args));
      int roc = Integer.parseInt(Utils.getOption('Z', args));
		
      if(Utils.getFlag('b', args)){
	Filter.batchFilterFile(new Windowise(lic, ric, loc, roc), args);
      }
      else{
	Filter.filterFile(new Windowise(lic, ric, loc, roc), args);
      }
    }
    catch(Exception ex){	
      System.out.println("Windowise cannot be used from the command line");
    }
  }

}
