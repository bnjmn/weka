/*
 *    EM.java
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

package weka.clusterers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.estimators.*;

/**
 * Simple EM class
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 2 Feb 1999 (Mark)
 */
public class EM extends DistributionClusterer implements OptionHandler
{

  /** hold the discrete estimators for each cluster */
  private Estimator model [][];

  /** hold the normal estimators for each cluster */
  private double modelNormal [][][];

  /** hold the weights of each instance for each cluster */
  private double weights [][];

  /** hold default standard deviations for numeric attributes */
  private double defSds [];

  /** the prior probabilities for clusters */
  private double priors [];

  /** the loglikelihood of the data */
  private double loglikely;

  /** training instances */
  private Instances theInstances = null;

  /** number of clusters */
  private int num_clusters;

  /** number of attributes */
  private int num_attribs;

  /** number of training instances */
  private int num_instances;

  /** maximum iterations to perform */
  private int max_iterations;
  
  /** random numbers and seed */
  private Random rr;

  private int rseed;

  /** Constant for normal distribution. */
  private static double normConst = Math.sqrt(2 * Math.PI);

  private boolean verbose;


  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   *
   **/
  public Enumeration listOptions() 
  {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option("\tnumber of clusters. If omitted or\n\t-1 specified, then cross validation is used to\n\tselect the number of clusters.", "N", 1,"-N <num>"));

    newVector.addElement(new Option("\tmax iterations.", "I", 1,"-I <num>"));
    newVector.addElement(new Option("\trandom number seed.", "S", 1,"-S <num>"));
    newVector.addElement(new Option("\tverbose..", "V", 0,"-V"));

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

    verbose = Utils.getFlag('V',options);

    String optionString = Utils.getOption('I',options);
    if (optionString.length() != 0)
    {
      max_iterations = Integer.parseInt(optionString);
    }

    optionString = Utils.getOption('N',options);
    if (optionString.length() != 0)
    {
      num_clusters = Integer.parseInt(optionString);
      if (num_clusters <= 0 )
	num_clusters = -1;
    }

    optionString = Utils.getOption('S',options);
    if (optionString.length() != 0)
    {
      rseed = Integer.parseInt(optionString);
    }
  }

  /**
   * Gets the current settings of EM.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    String [] options = new String [7];
    int current = 0;
    if (verbose) {
      options[current++] = "-V";
    }
    options[current++] = "-I"; options[current++] = "" + max_iterations;
    options[current++] = "-N"; options[current++] = "" + num_clusters;
    options[current++] = "-S"; options[current++] = "" + rseed;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets default standard devs for numeric attributes based on the 
   * differences between their sorted values.
   * @param inst the instances
   **/
  private void setDefaultStdDevs(Instances inst)
  {
    int i;

    Instances copyI = new Instances(inst);
    inst = copyI;

    defSds = new double [num_attribs];
    for (i=0;i<num_attribs;i++)
      defSds[i] = 0.01;
    
    for (i=0;i<num_attribs;i++)
      if (inst.attribute(i).isNumeric())
      {
	inst.sort(i);
	
	if ((inst.numInstances() > 0)
            && !inst.instance(0).isMissing(i)) 
	{
          double lastVal = inst.instance(0).value(i);
          double currentVal, deltaSum = 0;
          int distinct = 0;
          for (int j = 1; j < inst.numInstances(); j++) 
	  {
            Instance currentInst = inst.instance(j);
            if (currentInst.isMissing(i)) 
	    {
              break;
            }
            currentVal = currentInst.value(i);
            if (currentVal != lastVal) 
	    {
              deltaSum += currentVal - lastVal;
              lastVal = currentVal;
              distinct++;
            }
          }

	   if (distinct > 0) 
	   {
	     defSds[i] = deltaSum / distinct;
	   }
	}
      }
  }


  /**
   * Initialised estimators and storage.
   **/
  private void EM_Init(Instances inst, int num_cl) throws Exception
  {
    weights = new double [inst.numInstances()][num_cl];
    int z;

    model = new Estimator [num_cl][num_attribs];

    modelNormal = new double [num_cl][num_attribs][3];

    priors = new double [num_cl];

    for (int i=0;i<inst.numInstances();i++)
    {
      for (int j=0;j<num_cl;j++)
	weights[i][j] = rr.nextDouble();

      Utils.normalize(weights[i]);
    }

    // initial priors
    estimate_priors(inst, num_cl);
  }


  /**
   * calculate prior probabilites for the clusters
   * @exception Exception if priors can't be calculated
   **/
  private void estimate_priors(Instances inst, int num_cl) throws Exception
  {
    for (int i=0;i<num_clusters;i++)
      priors[i] = 0.0;

    for (int i=0;i<inst.numInstances();i++)
      for (int j=0;j<num_cl;j++)
	priors[j] += weights[i][j];

    Utils.normalize(priors);
  }

  /**
   * Density function of normal distribution.
   * @param x input value
   * @param mean mean of distribution
   * @param stdDev standard deviation of distribution
   */
  private double normalDens(double x, double mean, double stdDev) {
    
    double diff = x - mean;
    
    return (1 / (normConst * stdDev)) 
      * Math.exp(-(diff * diff / (2 * stdDev * stdDev)));
  }
  
  /**
   * New probability estimators for an iteration
   */
  private void new_estimators(int num_cl)
  {
    for (int i=0;i<num_cl;i++)
      for (int j=0;j<num_attribs;j++)
      {
	if (theInstances.attribute(j).isNominal())
	  model[i][j] = new DiscreteEstimator(theInstances.attribute(j).numValues(), true);
	else
	{
	  modelNormal[i][j][0] = modelNormal[i][j][1] = modelNormal[i][j][2] = 0.0;
	}
      }
  }

  /**
   * The M step of the EM algorithm.
   * @param inst the training instances
   * @param num_cl the number of clusters
   */
  private void M(Instances inst, int num_cl) throws Exception
  {
    int i,j,l;

    new_estimators(num_cl);

    for (i=0;i<num_cl;i++)
      for (j=0;j<num_attribs;j++)
	for (l=0;l<inst.numInstances();l++)
	{
	  if (!inst.instance(l).isMissing(j))
	  {
	    if (inst.attribute(j).isNominal())
	      model[i][j].addValue(inst.instance(l).value(j),
				   weights[l][i]);
	    else
	    {
	    
	      modelNormal[i][j][0] += (inst.instance(l).value(j) * 
				       weights[l][i]);
	    
	      modelNormal[i][j][2] += weights[l][i];

	      modelNormal[i][j][1] += (inst.instance(l).value(j) * 
				       inst.instance(l).value(j) * 
				       weights[l][i]);
	   
	    }
	  }
	}

    // calcualte mean and std deviation for numeric attributes
    for (j=0;j<num_attribs;j++)
      if (!inst.attribute(j).isNominal())
	for (i=0;i<num_cl;i++)
	{

	  if (Utils.smOrEq(modelNormal[i][j][2],1))
	  {
	    modelNormal[i][j][1] = /* defSds[j] / (2 * 3);*/ 1e-6;
	  }
	  else
	    // variance
	    modelNormal[i][j][1] = (modelNormal[i][j][1] - 
				  (modelNormal[i][j][0] * modelNormal[i][j][0] / 
				   modelNormal[i][j][2])) / (modelNormal[i][j][2] - 1);

	  // std dev
	  if (modelNormal[i][j][1] <= 0.0)
	  {
	    modelNormal[i][j][1] = /* defSds[j] / (2 * 3);*/ 1e-6;
	  }

	  modelNormal[i][j][1] = Math.sqrt(modelNormal[i][j][1]);

	  // mean
	  if (modelNormal[i][j][2] > 0.0)
	    modelNormal[i][j][0] /= modelNormal[i][j][2]; 
	}

  }

  /**
   * The E step of the EM algorithm. Estimate cluster membership 
   * probabilities.
   * @param inst the training instances
   * @param num_cl the number of clusters
   */
  private double E(Instances inst, int num_cl) throws Exception
  {
    int i,j,l;
    double prob;

    double loglk = 0.0;

    for (l=0;l<inst.numInstances();l++)
    {
      for (i=0;i<num_cl;i++)
      {
	prob = 1.0;
	for (j=0;j<num_attribs;j++)
	{
	  if (!inst.instance(l).isMissing(j))
	  {
	    if (inst.attribute(j).isNominal())
	      prob *= 
	      model[i][j].getProbability(inst.instance(l).value(j));
	    else // numeric attribute
	      prob *= 
	      normalDens(inst.instance(l).value(j), 
				 modelNormal[i][j][0], modelNormal[i][j][1]);
	  }
	}
	weights[l][i] = (prob * priors[i]);
      }

      double temp1=0;

       for (i=0;i<num_cl;i++)
	 temp1 += weights[l][i];

       if (temp1 > 0)
	    loglk += Math.log(temp1);

      // normalise the weights for this instance
      Utils.normalize(weights[l]);
    }

    // reestimate priors
    estimate_priors(inst, num_cl);

    return loglk / inst.numInstances();
  }

  /**
   * Constructor.
   *
   **/
  public EM()
  {
    resetOptions();
  }

  protected void resetOptions() {
    
    max_iterations = 100;
    rseed = 100;
    num_clusters = -1;
    verbose = false;
  }

  /**
   * Outputs the generated clusters into a string.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    text.append("\nEM\n==\n");
    text.append("\nNumber of clusters: "+num_clusters+"\n");
    for (int j = 0; j < num_clusters; j++) {
      text.append("\nCluster: "+j+" Prior probability: "+
		  Utils.doubleToString(priors[j], 6, 4)+"\n\n");
      for (int i = 0; i < num_attribs; i++) {
	text.append("Attribute: "+theInstances.attribute(i).name()+"\n");
	if (theInstances.attribute(i).isNominal()) {
	  if (model[j][i] != null) {
	    text.append(model[j][i].toString());
	  }
	} else {
	  text.append("Normal Distribution. Mean = "+
		      Utils.doubleToString(modelNormal[j][i][0],8,4)+
		      " StdDev = "+
		      Utils.doubleToString(modelNormal[j][i][1],8,4)+
		      "\n");
	}
      }
    }
    return text.toString();
  }


  /**
   * verbose output for debugging
   * @param inst the training instances
   */
  private void EM_Report(Instances inst)
  {
    int i,j,l,m;

    System.out.println("======================================");
    for (j=0;j<num_clusters;j++)
      for (i=0;i<num_attribs;i++)
      {
	System.out.println("Clust: "+j+" att: "+i+"\n");
	if (theInstances.attribute(i).isNominal())
	{
	  if (model[j][i] != null)
	    System.out.println(model[j][i].toString());
	}
	else 
	{
	  System.out.println("Normal Distribution. Mean = "+
			     Utils.doubleToString(modelNormal[j][i][0],8,4)+
			     " StandardDev = "+
			     Utils.doubleToString(modelNormal[j][i][1],8,4)+
			     " WeightSum = "+
			     Utils.doubleToString(modelNormal[j][i][2],8,4));
	}
      }

    for (l=0;l<inst.numInstances();l++)
    {
      m = Utils.maxIndex(weights[l]);
      System.out.print("Inst "+Utils.doubleToString((double)l,5,0)+" Class "+m+"\t");
      for (j=0;j<num_clusters;j++)
	System.out.print(Utils.doubleToString(weights[l][j],7,5)+"  ");
      System.out.println();
    }
  }

  /**
   * estimate the number of clusters by cross validation on the training
   * data.
   */
  private int CVClusters() throws Exception
  {
    double CVLogLikely = -Double.MAX_VALUE;
    double templl, tll;
    boolean CVdecreased = true;
    int num_cl = 1;
    int i;
    Random cvr;
    Instances trainCopy;
    
    while (CVdecreased)
    {
      CVdecreased = false;
      cvr = new Random(rseed);
      trainCopy = new Instances(theInstances);
      trainCopy.randomize(cvr);
      // theInstances.stratify(10);
      templl = 0.0;
      for (i=0;i<10;i++)
      {
	Instances cvTrain = trainCopy.trainCV(10,i);
	Instances cvTest = trainCopy.testCV(10,i);
	
	EM_Init(cvTrain, num_cl);

	iterate(cvTrain, num_cl, false);
	tll = E(cvTest, num_cl);
	if (verbose)
	  System.out.println("# clust: "+num_cl+" Fold: "+i+" Loglikely: "+tll);
	templl += tll;
      }
      templl /= 10.0;
      if (verbose)
	System.out.println("=================================================\n# clust: "
			   +num_cl+" Mean Loglikely: "
			   +templl
			   +"\n=================================================");
      
      if (templl > CVLogLikely)
      {
	CVLogLikely = templl;
	CVdecreased = true;
	num_cl++;
      }
    }

    if (verbose)
      System.out.println("Number of clusters: "+(num_cl-1));

    return num_cl-1;
  }


  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned
   * successfully
   */
  public int numberOfClusters() throws Exception
  {
    if (num_clusters == -1)
      throw new Exception("Haven't generated any clusters!");

    return num_clusters;
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be assigned to a cluster
   * @return the number of the assigned cluster as an interger
   * if the class is enumerated, otherwise the predicted value
   * @exception Exception if instance could not be classified
   * successfully
   */
  public void buildClusterer(Instances data) throws Exception
  {
    theInstances = data;
    
    doEM();
  }


  /**
   * Predicts the cluster memberships for a given instance.
   * @param data set of test instances
   * @param instance the instance to be assigned a cluster.
   * @return an array containing the estimated membership 
   * probabilities of the test instance in each cluster (this 
   * should sum to at most 1)
   * @exception Exception if distribution could not be 
   * computed successfully
   */
  public double [] distributionForInstance(Instance inst) throws Exception
  {
    int i,j;
    double prob;

    double [] wghts = new double [num_clusters];
    
    for (i=0;i<num_clusters;i++)
    {
      prob = 1.0;
      for (j=0;j<num_attribs;j++)
      {
	if (!inst.isMissing(j))
	{
	  if (inst.attribute(j).isNominal())
	    prob *= 
	    model[i][j].getProbability(inst.value(j));
	  else // numeric attribute
	    prob *= 
	    normalDens(inst.value(j), 
		       modelNormal[i][j][0], modelNormal[i][j][1]);
	}
      }
      wghts[i] = (prob * priors[i]);
    }

    return wghts;
  }


  /**
   * Perform the EM algorithm
   */
  private void doEM() throws Exception
  {
    if (verbose)
      System.out.println("Seed: "+rseed);

    rr = new Random(rseed);

    num_instances = theInstances.numInstances();
    num_attribs = theInstances.numAttributes();

    if (verbose)
      System.out.println("Number of instances: "+num_instances+"\nNumber of atts: "+num_attribs+"\n");

    // setDefaultStdDevs(theInstances);
    
    // cross validate to determine number of clusters?
    if (num_clusters == -1)
    {
      num_clusters = CVClusters();
    }

    // fit full training set
    EM_Init(theInstances,num_clusters);
    loglikely = iterate(theInstances, num_clusters, verbose);
  }


  /**
   * iterates the M and E steps until the log likelihood of the data
   * converges.
   *
   * @param inst the training instances.
   * @param num_cl the number of clusters.
   * @param report be verbose.
   * @return the log likelihood of the data
   */
  private double iterate(Instances inst, 
			int num_cl,
			boolean report) throws Exception
  {
    int i;
    double llkold = 0.0;
    double llk = 0.0;

    if (report)
      EM_Report(inst);

    for (i=0;i<max_iterations;i++)
    {
      M(inst,num_cl);
      llkold = llk;
      llk = E(inst,num_cl);
      
      if (report)
	System.out.println("Loglikely: "+llk);

      if (i > 0)
      {
	if ((llk - llkold) < 1e-6)
	{
	  break;
	}
      }
    }

    if (report)
      EM_Report(inst);

    return llk;
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-N number of clusters] [-S random seed]
   */

  public static void main(String [] argv)
  {

    try {
      System.out.println(ClusterEvaluation.evaluateClusterer(new EM(), argv));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}


