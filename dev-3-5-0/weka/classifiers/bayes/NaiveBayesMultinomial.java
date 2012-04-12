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
 *    NaiveBayesMultinomial1.java
 *    Copyright (C) 2003 Andrew Golightly
 *                  -- last updated 30/06/2003
 */

package weka.classifiers.bayes;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.classifiers.Classifier;

/**
 * Class for building and using a multinomial Naive Bayes classifier.
 * For more information see,<p>
 *
 * Andrew Mccallum, Kamal Nigam (1998)<i>A Comparison of Event Models for Naive Bayes Text Classification </i>
 *
 * @author Andrew Golightly (acg4@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $ 
 */

/**
 * The core equation for this classifier:
 * 
 * P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)
 * 
 * where Ci is class i and D is a document
 */

public class NaiveBayesMultinomial extends Classifier 
  implements WeightedInstancesHandler {
    
  /*
    probability that a word (w) exists in a class (H) (i.e. Pr[w|H])
    The matrix is in the this format: probOfWordGivenClass[class][wordAttribute]
    NOTE: the values are actually the log of Pr[w|H]
  */
  private double[][] probOfWordGivenClass;
    
  //the probability of a class (i.e. Pr[H])
  private double[] probOfClass;
    
  //number of unique words
  private int numAttributes;
    
  //number of class values
  private int numClasses;
    
  //cache lnFactorial computations
  private double[] lnFactorialCache = new double[]{0.0,0.0};
    
  //copy of header information for use in toString method
  Instances headerInfo;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for building and using a multinomial Naive Bayes classifier. "
      +"For more information see,\n\n"
      +"Andrew Mccallum, Kamal Nigam (1998) A Comparison of Event Models for Naive "
      +"Bayes Text Classification";
  }
  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception 
  {
    headerInfo = new Instances(instances, 0);
    numClasses = instances.numClasses();
    numAttributes = instances.numAttributes();
    probOfWordGivenClass = new double[numClasses][];
	
    /*
      initialising the matrix of word counts
      NOTE: Laplace estimator introduced in case a word that does not appear for a class in the 
      training set does so for the test set
    */
    for(int c = 0; c<numClasses; c++)
      {
	probOfWordGivenClass[c] = new double[numAttributes];
	for(int att = 0; att<numAttributes; att++)
	  {
	    /*
	      check all attributes (except the class attribute) are numeric and 
	      that the class attribute in nominal
	    */
	    if(instances.classIndex() == att)
	      {
		if(!instances.attribute(att).isNominal())
		  throw new Exception("The class attribute is required to be nominal. This is currently not the case!");
	      }
	    else
	      if(!instances.attribute(att).isNumeric())
		throw new Exception(("Attribute " + instances.attribute(att).name() + " is not numeric! NaiveBayesMultinomial1 requires that all attributes (except the class attribute) are numeric."));
	    probOfWordGivenClass[c][att] = 1;
	  }
      }
	
    //enumerate through the instances 
    Instance instance;
    int classIndex;
    double numOccurences;
    double[] docsPerClass = new double[numClasses];
    double[] wordsPerClass = new double[numClasses];
	
    java.util.Enumeration enumInsts = instances.enumerateInstances();
    while (enumInsts.hasMoreElements()) 
      {
	instance = (Instance) enumInsts.nextElement();
	classIndex = (int)instance.value(instance.classIndex());
	docsPerClass[classIndex] += instance.weight();
		
	for(int a = 0; a<instance.numValues(); a++)
	  if(instance.index(a) != instance.classIndex())
	    {
	      if(!instance.isMissing(a))
		{
		  numOccurences = instance.valueSparse(a) * instance.weight();
		  if(numOccurences < 0)
		    throw new Exception("Numeric attribute values must all be greater or equal to zero.");
		  wordsPerClass[classIndex] += numOccurences;
		  probOfWordGivenClass[classIndex][instance.index(a)] += numOccurences;
		}
	    } 
      }
	
    /*
      normalising probOfWordGivenClass values
      and saving each value as the log of each value
    */
    for(int c = 0; c<numClasses; c++)
      for(int v = 0; v<numAttributes; v++) 
	probOfWordGivenClass[c][v] = Math.log(probOfWordGivenClass[c][v] / (wordsPerClass[c] + numAttributes - 1));
	
    /*
      calculating Pr(H)
      NOTE: Laplace estimator introduced in case a class does not get mentioned in the set of 
      training instances
    */
    final double numDocs = instances.sumOfWeights() + numClasses;
    probOfClass = new double[numClasses];
    for(int h=0; h<numClasses; h++)
      probOfClass[h] = (double)(docsPerClass[h] + 1)/numDocs; 
  }
    
  /**
   * Calculates the class membership probabilities for the given test 
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception 
  {
    double[] probOfClassGivenDoc = new double[numClasses];
	
    //calculate the array of log(Pr[D|C])
    double[] logDocGivenClass = new double[numClasses];
    for(int h = 0; h<numClasses; h++)
      logDocGivenClass[h] = probOfDocGivenClass(instance, h);
	
    double max = logDocGivenClass[Utils.maxIndex(logDocGivenClass)];
    double probOfDoc = 0.0;
	
    for(int i = 0; i<numClasses; i++) 
      {
	probOfClassGivenDoc[i] = Math.exp(logDocGivenClass[i] - max) * probOfClass[i];
	probOfDoc += probOfClassGivenDoc[i];
      }
	
    Utils.normalize(probOfClassGivenDoc,probOfDoc);
	
    return probOfClassGivenDoc;
  }
    
  /**
   * log(N!) + (for all the words)(log(Pi^ni) - log(ni!))
   *  
   *  where 
   *      N is the total number of words
   *      Pi is the probability of obtaining word i
   *      ni is the number of times the word at index i occurs in the document
   *
   * @param inst       The instance to be classified
   * @param classIndex The index of the class we are calculating the probability with respect to
   *
   * @return The log of the probability of the document occuring given the class
   */
    
  private double probOfDocGivenClass(Instance inst, int classIndex)
  {
    double answer = 0;
    //double totalWords = 0; //no need as we are not calculating the factorial at all.
	
    double freqOfWordInDoc;  //should be double
    for(int i = 0; i<inst.numValues(); i++)
      if(inst.index(i) != inst.classIndex())
	{
	  freqOfWordInDoc = inst.valueSparse(i);
	  //totalWords += freqOfWordInDoc;
	  answer += (freqOfWordInDoc * probOfWordGivenClass[classIndex][inst.index(i)] 
		     ); //- lnFactorial(freqOfWordInDoc));
	}
	
    //answer += lnFactorial(totalWords);//The factorial terms don't make 
    //any difference to the classifier's
    //accuracy, so not needed.
	
    return answer;
  }
    
  /**
   * Fast computation of ln(n!) for non-negative ints
   *
   * negative ints are passed on to the general gamma-function
   * based version in weka.core.SpecialFunctions
   *
   * if the current n value is higher than any previous one,
   * the cache is extended and filled to cover it
   *
   * the common case is reduced to a simple array lookup
   *
   * @param  n the integer 
   * @return ln(n!)
   */
    
  public double lnFactorial(int n) 
  {
    if (n < 0) return weka.core.SpecialFunctions.lnFactorial(n);
	
    if (lnFactorialCache.length <= n) {
      double[] tmp = new double[n+1];
      System.arraycopy(lnFactorialCache,0,tmp,0,lnFactorialCache.length);
      for(int i = lnFactorialCache.length; i < tmp.length; i++) 
	tmp[i] = tmp[i-1] + Math.log(i);
      lnFactorialCache = tmp;
    }
	
    return lnFactorialCache[n];
  }
    
  public String toString()
  {
    StringBuffer result = new StringBuffer("The independent probability of a class\n--------------------------------------\n");
	
    for(int c = 0; c<numClasses; c++)
      result.append(headerInfo.classAttribute().value(c)).append("\t").append(Double.toString(probOfClass[c])).append("\n");
	
    result.append("\nThe probability of a word given the class\n-----------------------------------------\n\t");

    for(int c = 0; c<numClasses; c++)
      result.append(headerInfo.classAttribute().value(c)).append("\t");
	
    result.append("\n");

    for(int w = 0; w<numAttributes; w++)
      {
	result.append(headerInfo.attribute(w).name()).append("\t");
	for(int c = 0; c<numClasses; c++)
	  result.append(Double.toString(Math.exp(probOfWordGivenClass[c][w]))).append("\t");
	result.append("\n");
      }

    return result.toString();
  }
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    try {
      System.out.println(weka.classifiers.Evaluation.evaluateModel(new NaiveBayesMultinomial(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
    

