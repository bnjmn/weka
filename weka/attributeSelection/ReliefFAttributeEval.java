/*
 *    ReliefFAttributeEval.java
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


/** 
 * Class for Evaluating attributes individually using ReliefF.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 April 1999 (Mark)
 */
public class ReliefFAttributeEval 
  extends AttributeEvaluator 
  implements OptionHandler {


  private Instances trainInstances;

  private int classIndex;
  
  private int numAttribs;

  private int numInstances;

  private boolean numericClass;

  private int numClasses;

  // used to hold the probability of a different class val given nearest
  // instances (numeric class)
  private double r_ndc;

  // used to hold the prob of different value of an attribute given
  // nearest instances (numeric class case)
  private double [] r_nda;

  // used to hold the prob of a different class val and different att
  // val given nearest instances (numeric class case)
  private double [] r_ndcda;

  // holds the weights that relief assigns to attributes
  private double [] r_weights;

  // prior class probabilities (discrete class case)
  private double [] classProbs;

  // The number of instances to sample when estimating attributes
  // default == -1, use all instances
  private int r_sampleM;

  // The number of nearest hits/misses
  private int r_Knn;

  // k nearest scores + instance indexes for n classes
  private double [][][] r_karray;

  // upper bound for numeric attributes
  private double [] maxArray;

  // lower bound for numeric attributes
  private double [] minArray;

  // keep track of the farthest instance for each class
  private double [] worst;

  // index in the r_karray of the farthest instance for each class
  private int [] w_index;

  // number of nearest neighbours stored of each class
  private int [] stored;

  private int r_seed;

  /**
   *  used to (optionally) weight nearest neighbours by their distance
   *  from the instance in question. Each entry holds 
   *  exp(-((rank(r_i, i_j)/sigma)^2)) where rank(r_i,i_j) is the rank of
   *  instance i_j in a sequence of instances ordered by the distance
   *  from r_i. sigma is a user defined parameter, default=20
   **/
  private double [] weightsByRank;

  private int r_sigma;

  private boolean weightByDistance;

  public ReliefFAttributeEval ()
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
    
    Vector newVector = new Vector(4);
    
    newVector.addElement(new Option("\tSpecify the number of instances to\n"
				    +"\tsample when estimating attributes.\n"
				    +"\tIf not specified, then all instances\n"
				    +"\t will be used."
				    , "M", 1,"-M <num instances>"));
    newVector.addElement(new Option("\tSeed for randomly sampling instances."
				    ,"D",1,"-D <seed>"));
    newVector.addElement(new Option("\tNumber of nearest neighbours (k) used\n"
				    +"\tto estimate attribute relevances\n"
				    +"\t(Default = 10)."
				    ,"K",1,"-K <number of neighbours>"));
    newVector.addElement(new Option("\tWeight nearest neighbours by distance\n"
				    ,"W",0,"-W"));
    newVector.addElement(new Option("\tSpecify sigma value (used in an exp\n"
				    +"\tfunction to control how quickly\n"
				    +"\tweights for more distant instances\n"
				    +"\tdecrease. Use in conjunction with -W.\n"
				    +"\tSensible value=1/5 to 1/10 of the\n"
				    +"\tnumber of nearest neighbours.\n"
				    +"\t(Default = 2)"
				    ,"S",1,"-S <num>"));

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
    resetOptions();
    
    weightByDistance = Utils.getFlag('W',options);

    optionString = Utils.getOption('M',options);
    if (optionString.length() != 0)
      {
	r_sampleM = Integer.parseInt(optionString);
      }

    optionString = Utils.getOption('D',options);
    if (optionString.length() != 0)
      {
	r_seed = Integer.parseInt(optionString);
      }

    optionString = Utils.getOption('K',options);
    if (optionString.length() != 0)
      {
	r_Knn = Integer.parseInt(optionString);
	if (r_Knn <=0)
	  throw new Exception("number of nearest neighbours must be > 0!");
      }
    
    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0)
      {
	r_sigma = Integer.parseInt(optionString);
	if (r_sigma <=0)
	  throw new Exception("value of sigma must bee > 0!");
      }
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] options = new String [9];
    int current = 0;

    if (weightByDistance)
      {
	options[current++] = "-W";
      }
    options[current++] = "-M";options[current++] = ""+r_sampleM;
    options[current++] = "-D";options[current++] = ""+r_seed;
    options[current++] = "-K";options[current++] = ""+r_Knn;
    options[current++] = "-S";options[current++] = ""+r_sigma;

    while (current < options.length) 
      {
	options[current++] = "";
      }

    return options;
  }


  public String toString()
  {
    StringBuffer text = new StringBuffer();
    
    text.append("\tReliefF Ranking Filter");
    text.append("\n\tInstances sampled: ");
    if (r_sampleM == -1)
      text.append("all\n");
    else
      text.append(r_sampleM+"\n");

    text.append("\tNumber of nearest neighbours (k): "+r_Knn+"\n");
    if (weightByDistance)
      text.append("\tExponentially decreasing (with distance) influence for\n"
		  +"\tnearest neighbours. Sigma: "+r_sigma+"\n");
    else
      text.append("\tEqual influence nearest neighbours\n");
    return text.toString();
  }

  /**
   * Initializes a ReliefF attribute evaluator. 
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception
  {
    int z, totalInstances;
    Random r = new Random(r_seed);

    trainInstances = data;
    classIndex = trainInstances.classIndex();
    numAttribs = trainInstances.numAttributes();
    numInstances = trainInstances.numInstances();

    if (trainInstances.attribute(classIndex).isNumeric())
      numericClass = true;
    else
      numericClass = false;

    if (!numericClass)
      numClasses = trainInstances.attribute(classIndex).numValues();
    else
      {
	r_ndc = 0;
	numClasses = 1;
	r_nda = new double [numAttribs];
	r_ndcda = new double [numAttribs];
      }

    if (weightByDistance) // set up the rank based weights
      {
	weightsByRank = new double [r_Knn];
	for (int i=0;i<r_Knn;i++)
	  {
	    weightsByRank[i] = 
	      Math.exp(-((i/(double)r_sigma)*(i/(double)r_sigma)));
	  }
      }
    // the final attribute weights
    r_weights = new double [numAttribs];

    // num classes (1 for numeric class) knn neighbours, 
    // and 0 = distance, 1 = instance index
    r_karray = new double [numClasses][r_Knn][2];

    if (!numericClass)
      {
	classProbs = new double [numClasses];
	for (int i=0;i<numInstances;i++)
	  classProbs[(int)trainInstances.instance(i).value(classIndex)]++;
	for (int i=0;i<numClasses;i++)
	  classProbs[i] /= numInstances;
      }
	
    worst = new double [numClasses];
    w_index = new int [numClasses];
    stored = new int [numClasses];

    minArray = new double [numAttribs];
    maxArray = new double [numAttribs];
    for (int i = 0; i < numAttribs; i++) 
      {
	minArray[i] = maxArray[i] = Double.NaN;
      }

    for (int i=0;i<numInstances;i++)
      updateMinMax(trainInstances.instance(i));

    if ((r_sampleM > numInstances) || (r_sampleM == -1))
      totalInstances = numInstances;
    else
      totalInstances = r_sampleM;

    // process each instance, updating attribute weights
    for (int i=0;i<totalInstances;i++)
      {
	if (totalInstances == numInstances)
	  z = i;
	else
	  z = r.nextInt() % numInstances;
	if (z < 0)
	  z *= -1;

	if (!(trainInstances.instance(z).isMissing(classIndex)))
	  {
	    // first clear the knn and worst index stuff for the classes
	    for (int j=0;j<numClasses;j++)
	      {
		w_index[j] = stored[j] = 0;
		for (int k=0;k<r_Knn;k++)
		  r_karray[j][k][0] = r_karray[j][k][1] = 0;
	      }
	  
	    findKHitMiss(z);

	    if (numericClass)
	      updateWeightsNumericClass(z);
	    else
	      updateWeightsDiscreteClass(z);
	  }
      }

    // now scale weights by 1/numInstances (nominal class) or
    // calculate weights numeric class
    //    System.out.println("num inst:"+numInstances+" r_ndc:"+r_ndc);
    for (int i=0;i<numAttribs;i++)
      if (i != classIndex)
	{
	  if (numericClass)
	    r_weights[i] = r_ndcda[i]/r_ndc - 
	      ((r_nda[i] - r_ndcda[i])/((double)totalInstances-r_ndc));
	  else
	    r_weights[i] *= (1.0 / (double)totalInstances);

	  //	  System.out.println(r_weights[i]);
	}
  }

  /**
   * Evaluates an individual attribute using ReliefF's instance based approach.
   * The actual work is done by buildEvaluator which evaluates all features.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception
  {
    return r_weights[attribute];
  }

  protected void resetOptions()
  {
    trainInstances = null;
    r_sampleM = -1;
    r_Knn = 10;
    r_sigma = 2;
    weightByDistance = false;
    r_seed = 1;
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) 
  {
    if (Double.isNaN(minArray[i]) || Utils.eq(maxArray[i], minArray[i])) 
      {
	return 0;
      } 
    else 
      {
	return (x - minArray[i]) / (maxArray[i] - minArray[i]);
      }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {
    
    for (int j = 0;j < numAttribs; j++) 
      {
	if ((trainInstances.attribute(j).isNumeric()) && 
	    (!instance.isMissing(j))) 
	  {
	    if (Double.isNaN(minArray[j])) 
	      {
		minArray[j] = instance.value(j);
		maxArray[j] = instance.value(j);
	      } 
	    else 
	      {
		if (instance.value(j) < minArray[j]) 
		  {
		    minArray[j] = instance.value(j);
		  } 
		else 
		  {
		    if (instance.value(j) > maxArray[j]) 
		      {
			maxArray[j] = instance.value(j);
		      }
		  }
	      }
	  }
      }
  }
  
  /**
   * Calculate the difference between two attribute values
   *
   * @param attrib the attribute in question
   * @param first the index of the first instance
   * @param second the index of the second instance
   * @return the difference
   */
  private double attributeDiff(int attrib, int first, int second)
  {
    double temp,d;

    // Nominal attribute
    if (trainInstances.attribute(attrib).isNominal())
      {
	if (trainInstances.instance(first).isMissing(attrib) ||
	    trainInstances.instance(second).isMissing(attrib))
	  temp =  (1.0 - (1.0 / 
			 ((double)trainInstances.
			  attribute(attrib).numValues())));
	else
	  {
	    if (trainInstances.instance(first).value(attrib) !=
		trainInstances.instance(second).value(attrib))
	      temp = 1.0;
	    else
	      temp = 0.0;
	  }
      }
    else
      // Numeric attribute
      {
	if  (trainInstances.instance(first).isMissing(attrib) &&
	    trainInstances.instance(second).isMissing(attrib))
	  temp = 1.0; // maximally different
	else if (trainInstances.instance(first).isMissing(attrib))
	  {
	    d = norm(trainInstances.instance(second).value(attrib), attrib);
	    if (d < 0.5)
	      d = 1.0 - d;
	    temp = d;
	  }
	else if (trainInstances.instance(second).isMissing(attrib))
	  {
	    d = norm(trainInstances.instance(first).value(attrib), attrib);
	    if (d < 0.5)
	      d = 1.0 -d;
	    temp = d;
	  }
	else
	  {
	    d = norm(trainInstances.instance(first).value(attrib), 
		     attrib) - 
	      norm(trainInstances.instance(second).value(attrib), 
		   attrib);
	    if (d < 0.0)
	      d *= -1.0;
	    temp = d;
	  }
      }

    return temp;
  }

  /**
   * Calculate the difference between two instances as a sum of their
   * attribute differences
   *
   * @param first the index of the first instance
   * @param second the index of the second instance
   * @return the difference
   */
  private double diff(int first, int second)
  {
    int i,j;
    double temp = 0;

    for (i=0;i<numAttribs;i++)
      if (i != classIndex)
	{
	  temp += attributeDiff(i, first, second);
	}
    
    return temp;
  }

  /**
   * update attribute weights given an instance when the class is numeric
   *
   * @param instNum the index of the instance to use when updating weights
   */
  private void updateWeightsNumericClass(int instNum)
  {
    int i,j;
    double temp;
    int [] tempSorted = null;
    double [] tempDist = null;
    double distNorm = 1.0;

    // sort nearest neighbours and set up normalization variable
    if (weightByDistance)
      {
	tempDist = new double[stored[0]];
	for (j=0, distNorm = 0;j<stored[0];j++) 
	  {
	    // copy the distances
	    tempDist[j] = r_karray[0][j][0];
	    // sum normalizer
	    distNorm += weightsByRank[j];
	  }
	tempSorted = Utils.sort(tempDist);
      }

    for (i=0;i<stored[0];i++)
      {
	// P diff prediction (class) given nearest instances
	if (weightByDistance)
	  {
	    temp = attributeDiff(classIndex, instNum, 
				 (int)r_karray[0][tempSorted[i]][1]);
	    temp *= (weightsByRank[i] / distNorm);
	  }
	else
	  {
	    temp = attributeDiff(classIndex,instNum,
			     (int)r_karray[0][i][1]);
	    temp *= (1.0/(double)stored[0]); // equal influence
	  }
	r_ndc += temp;

	// now the attributes
	for (j=0;j<numAttribs;j++)
	  if (j != classIndex)
	    {
	      // P of different attribute val given nearest instances
	      if (weightByDistance)
		{
		  temp = attributeDiff(j,instNum,
				       (int)r_karray[0][tempSorted[i]][1]);
		  temp *= (weightsByRank[i] / distNorm);
		}
	      else
		{
		  temp = attributeDiff(j,instNum,
				       (int)r_karray[0][i][1]);
		  temp *= (1.0/(double)stored[0]); // equal influence
		}
	      r_nda[j] += temp;

	      // P of different prediction and different att value given
	      // nearest instances
	      if (weightByDistance)
		{
		  temp = attributeDiff(classIndex, instNum,
				       (int)r_karray[0][tempSorted[i]][1]) *
		    attributeDiff(j, instNum,
				  (int)r_karray[0][tempSorted[i]][1]);
		  temp *= (weightsByRank[i] / distNorm);
		}
	      else
		{
		  temp = attributeDiff(classIndex,instNum,
				       (int)r_karray[0][i][1]) *
		    attributeDiff(j,instNum,
				  (int)r_karray[0][i][1]);
		  temp *= (1.0/(double)stored[0]); // equal influence
		}
	      r_ndcda[j] += temp;
			      
	    }
      }
  }

  /**
   * update attribute weights given an instance when the class is discrete
   *
   * @param instNum the index of the instance to use when updating weights
   */
  private void updateWeightsDiscreteClass(int instNum)
  {
    int i,j,k;
    int cl;
    double cc = numInstances;
    double temp, temp_diff, w_norm=1.0;
    double [] tempDistClass;
    int [] tempSortedClass = null;
    double distNormClass = 1.0;
    double [] tempDistAtt;
    int [][] tempSortedAtt = null;
    double [] distNormAtt = null;

    // get the class of this instance
    cl = (int)trainInstances.instance(instNum).value(classIndex);

    // sort nearest neighbours and set up normalization variables
    if (weightByDistance)
      {
	// do class (hits) first
	// sort the distances
	tempDistClass = new double[stored[cl]];
	for (j=0, distNormClass = 0;j<stored[cl];j++) 
	  {
	    // copy the distances
	    tempDistClass[j] = r_karray[cl][j][0];
	    // sum normalizer
	    distNormClass += weightsByRank[j];
	  }
	tempSortedClass = Utils.sort(tempDistClass);

	// do misses (other classes)
	tempSortedAtt = new int[numClasses][1];
	distNormAtt = new double[numClasses];
	  for (k=0;k<numClasses;k++)
	    if (k != cl) // already done cl
	      {
		// sort the distances
		tempDistAtt = new double[stored[k]];
		for (j=0, distNormAtt[k] = 0;j<stored[k];j++) 
		  {
		    // copy the distances
		    tempDistAtt[j] = r_karray[k][j][0];
		    // sum normalizer
		    distNormAtt[k] += weightsByRank[j];
		  }
		tempSortedAtt[k] = Utils.sort(tempDistAtt);

	      }
      }

    if (numClasses > 2)
      {
	// the amount of probability space left after removing the
	// probability of this instances class value
	w_norm = (1.0 - classProbs[cl]);
      }

    for (i=0;i<numAttribs;i++)
      if (i != classIndex)
	{
	  // first do k nearest hits
	  for (j=0, temp_diff = 0.0;j < stored[cl]; j++)
	    {
	      if (weightByDistance)
		{
		  temp_diff += 
		    attributeDiff(i, instNum, 
				  (int)r_karray[cl][tempSortedClass[j]][1]) * 
		    (weightsByRank[j] / distNormClass);
		}
	      else
		temp_diff += 
		  attributeDiff(i, instNum, (int)r_karray[cl][j][1]);
	    }
	

	  // average
	  if ((!weightByDistance) && (stored[cl] > 0))
	    temp_diff /= (double)stored[cl];
    
	  r_weights[i] -= temp_diff;

	  // now do k nearest misses from each of the other classes
	  temp_diff = 0.0;
	  for (k=0;k<numClasses;k++)
	    if (k != cl) // already done cl
	      {
		for (j=0,temp=0.0;j<stored[k];j++)
		  {
		    if (weightByDistance)
		      {
			temp_diff += 
			  attributeDiff(i, instNum, 
					(int)r_karray[k][tempSortedAtt[k][j]][1]) * 
			  (weightsByRank[j] / distNormAtt[k]);
		      }
		    else
		      temp += attributeDiff(i, instNum, 
					    (int)r_karray[k][j][1]);
		  }

		if ((!weightByDistance) && (stored[k] > 0))
		  temp /= (double)stored[k];

		// now add temp to temp_diff weighted by the prob of this class
		if (numClasses > 2)
		  {
		    temp_diff += (classProbs[k] / w_norm) * temp;
		  }
		else
		  temp_diff += temp;
	      }
	  r_weights[i] += temp_diff;
	}
  }

  /**
   * Find the K nearest instances to supplied instance if the class is numeric,
   * or the K nearest Hits (same class) and Misses (K from each of the other
   * classes) if the class is discrete.
   *
   * @param instNum the index of the instance to find nearest neighbours of
   */
  private void findKHitMiss(int instNum)
  {
    int i,j;
    int cl;
    double ww;
    double temp_diff = 0.0;

    for (i=0;i<numInstances;i++)
      if (i != instNum)
	{
	  temp_diff = diff(i, instNum);

	  // class of this training instance or 0 if numeric
	  if (numericClass)
	    cl = 0;
	  else
	    cl = (int)trainInstances.instance(i).value(classIndex);

	  // add this diff to the list for the class of this instance
	  if (stored[cl] < r_Knn)
	    {
	      r_karray[cl][stored[cl]][0] = temp_diff;
	      r_karray[cl][stored[cl]][1] = i;
	      stored[cl]++;

	      // note the worst diff for this class
	      for (j=0,ww = -1.0;j<stored[cl];j++)
		{
		  if (r_karray[cl][j][0] > ww)
		    {
		      ww = r_karray[cl][j][0];
		      w_index[cl] = j;
		    }
		}

	      worst[cl] = ww;
	    }
	  else
	    /* if we already have stored knn for this class then check to
	       see if this instance is better than the worst */
	    {
	      if (temp_diff < r_karray[cl][w_index[cl]][0])
		{
		  r_karray[cl][w_index[cl]][0] = temp_diff;
		  r_karray[cl][w_index[cl]][1] = i;

		  for (j=0,ww = -1.0;j<stored[cl];j++)
		    {
		      if (r_karray[cl][j][0] > ww)
			{
			  ww = r_karray[cl][j][0];
			  w_index[cl] = j;
			}
		    }
		  worst[cl] = ww;
		}
	    }
	}
  }

  // ============
  // Test method.
  // ============
  
  /**
   * Main method for testing this class.
   *
   * @param argv holds general and specific feature selection options
   * plus search options
   */
  
  public static void main(String [] argv)
  {
    
    try {
      System.out.println(AttributeSelection.SelectAttributes(new ReliefFAttributeEval(), argv));
    }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
}

