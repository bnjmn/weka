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
 *    RSW.java
 *    Copyright (C) 2003 Saket Joshi
 *
 */

package weka.classifiers.meta;

import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.evaluation.*;
import weka.filters.*;
import java.io.*;
import java.util.*;

/**
 * Class for handling recurrent sliding window datasets with distribution
 * classifiers.<p>
 *
 * 1. An introduction to sequential data classification and recurrent sliding 
 * windows:<p>
 *
 * The standard supervised learning problem is to learn to map from
 * an input feature vector x to an output class variable y given N training
 * examples.  Many recent learning problems can be viewed as extensions of
 * standard supervised learning to the setting where each input object X is
 * a sequence of feature vectors X = (x1, ..., xT), and the corresponding
 * output object Y is a sequence of class labels Y = (y1, ..., yT).  The
 * sequential supervised learning (SSL) problem is to learn to map from X
 * to Y given a set of N training examples {(X1,Y1), ..., (XN, YN)}.<p>
 * 
 * Several recent learning systems involved solving SSL
 * problems. One example is the famous NETTalk problem of learning to
 * pronounce English words.  Each training example consists of a sequence
 * of letters (e.g., ``enough'') and a corresponding output sequence of
 * phonemes (e.g., In^-f-).  Another example is the problem of
 * part-of-speech tagging in which the input is a sequence of words (e.g.,
 * ``do you want fries with that?'') and the output is a sequence of parts
 * of speech (e.g., ``verb pron verb noun prep pron'').  A third example is
 * the problem of information extraction from web pages in which the input
 * sequence is a sequence of tokens from a web page and the output is a
 * sequence of field labels.<p>
 * 
 * In the literature, two general strategies for solving SSL
 * problems have been studied.  One strategy, which we might call the
 * "direct" approach, is to develop probabilistic models of sequential
 * data such as Hidden Markov Models (HMMs) and Conditional Random Fields
 * (CRFs).  These methods directly learn a model of the sequential data.<p>
 *
 * The other general strategy that has been explored might be
 * called the "indirect" approach (i.e., a "hack").  In this strategy,
 * the sequential supervised learning problem is solved indirectly by first
 * converting it into a standard supervised learning problem, solving that
 * problem, and then converting the results into a solution to the SSL
 * problem. Specifically, the indirect approach is to convert the input
 * sequence X and output sequence Y into a set of ``windows'' {w1, ..., w6}
 * as shown in the table below.  Each window wt consists of a central
 * element xt and some number of elements to the left and right of xt.  The
 * output of the window is the corresponding label yt.  We will denote the
 * number of elements to the left of xt as the Left Input Context (LIC),
 * and the number of elements to the right of xt as the Right Input Context
 * (RIC).  In the example, LIC=RIC=3.  Contextual positions before the
 * start of the sequence or after the end of the sequence are filled by a
 * designated null value (in this case "_").<p>
 * 
 * <code>
 * Simple Sliding Windows: <br>
 *  original SSL training example: (X, Y) where X = "enough" and Y = "In^-f-" <br>
 *  derived windows:       input elements:      output class label: <br>
 *  w1		 	   _ _ _ e n o u  	I <br>
 *  w2                     _ _ e n o u g	n <br>
 *  w3 			   _ e n o u g h	^ <br>
 *  w4 			   e n o u g h _	- <br>
 *  w5 			   n o u g h _ _	f <br>
 *  w6  		   o u g h _ _ _	- <br>
 * </code><p>
 *
 * In this example, each input element is a single character, but in
 * general, each input element can be a vector of features.<p>
 * 
 * The process of converting SSL training examples into windows is called
 * "windowizing".  The resulting windowized examples can be provided as
 * input to any standard supervised learning algorithm which will learn a
 * classifier that takes an input window and predicts an output class.  The
 * RSW package can then take this classifier and apply it to classify
 * additional windowized SSL examples.  RSW computes two measures of error
 * rate: (i) error rate on individual windows and (ii) error rate on entire
 * sequences.  An entire (X,Y) sequence is classified incorrectly if any of
 * its windows are misclassified.<p>
 * 
 * The RSW package also supports recurrent sliding windows.  In recurrent
 * sliding windows, the predicted output of the window classifier at
 * positions t-1, t-2, ..., are provided as additional input features to
 * make predictions at position t.   These additional input features are
 * called the Output Context.  Recurrent Sliding Windows require that the
 * X sequence be processed in one direction, either from left-to-right or
 * right-to-left.  If the sequence is processed left-to-right, the Left
 * Output Context (LOC) specifies the number of previous positions whose
 * predictions are fed back as additional inputs.  If the sequence is
 * processed right-to-left, the Right Output Context (ROC) parameter
 * specifies the number of positions whose predictions are fed back as
 * additional inputs.  No more than one of LOC and ROC can be nonzero.  If
 * LOC=ROC=0, then only simple sliding windows are produced.<p>
 * 
 * The table below shows the case for LOC=1 (and LIC=RIC=3 as before).
 * When the training data are windowized, the correct label for yt-1 is
 * provided as an input feature for predicting yt.<p>
 * 
 * <code>
 * Recurrent Sliding Windows <br>
 * (X, Y)			enough			In^-f- <br>
 * <br>
 *  w1		 	        _ _ _ e n o u - 	I <br>
 *  w2                          _ _ e n o u g I		n <br>
 *  w3 			        _ e n o u g h n		^ <br>
 *  w4 			        e n o u g h _ ^		- <br>
 *  w5 			        n o u g h _ _ -		f <br>
 *  w6  			o u g h _ _ _ f		- <br>
 * </code><p>
 *
 * These windowized examples can then be provided to a standard supervised
 * learning algorithm which learns to map the input windows to the output
 * labels.  The learned function can then be applied by RSW to classify new
 * (X,Y) sequences.  The new test sequences are windowized dynamically, by
 * using the predicted value yhat at position t-1 as the extra input
 * feature to predict yt.  As with simple sliding windows, two error rates
 * are computed:  (i) the fraction of individual elements incorrectly
 * predicted, and (ii) the fraction of entire (X,Y) sequences incorrectly
 * predicted.<p>
 * 
 * 2. Structure of the ARFF file for sequential data:<p>
 *
 * For RSW, the ARFF file contains one example for each element of each
 * training sequence.  The data file has the standard ARFF format with the
 * following extensions:<p>
 * 
 * The first attribute in each line must be the sequence number.  Each
 * training example (X,Y) is assigned a unique sequence number (in
 * ascending order counting from 1).  The second attribute in each line
 * must be the element number.  Each position (xt,yt) for t = 1, ..., T
 * (where T is the length of X and Y) must be assigned an element number
 * (in ascending order counting from 1).  The remaining attributes in each
 * line provide features that describe xt and the class label yt.  The
 * following example ARFF file shows two training sequences (bad, BAD) and
 * (feed, FEED).  Note that each attribute and the class variable must
 * specify an extra null value that is used to pad context that extends
 * beyond the end of the sequence.  In this case, we have specified the
 * null value "_".<p>
 *
 * <code> 
 * @relation SampleSequentialData <br>
 * <br>
 * @attribute sequence_number numeric <br>
 * @attribute element_number numeric <br>
 * @attribute feature1 {a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,_} <br>
 * @attribute class {A1, B1, C1, D1, E1, F1, _} <br>
 * <br>
 * @data <br>
 * 1, 1, b, B1 <br>
 * 1, 2, a, A1 <br>
 * 1, 3, d, D1 <br>
 * 2, 1, f, F1 <br>
 * 2, 2, e, E1 <br>
 * 2, 3, e, E1 <br>
 * 2, 4, d, D1 <br>
 * </code><p>
 *
 * The type for the sequence_number and element_number attributes
 * must be numeric. The sequence numbers must start with 1 and proceed
 * serially without any breaks or jumps in increasing order.  The element
 * numbers inside each sequence also must start at 1 and proceed serially
 * in increasing order until the end of the sequence.  The class attribute
 * must be nominal. RSW converts the data structured as above into the
 * windowized data as shown in the previous section. The non-nominal
 * attributes remain unmodified.<p>
 *
 * Valid options are:<p>
 *
 * -W classifier <br>
 * Sets the base classifier (required).<p>
 * 
 * -A lic <br>
 * Sets the left input context. <p>
 *
 * -B ric <br>
 * Sets the right input context <p>
 *
 * -Y loc <br>
 * Sets the left output context. <p>
 *
 * -Z roc <br>
 * Sets the right output context. <p>
 *
 * @author Saket Joshi (joshi@cs.orst.edu)
 * @version $Revision: 1.1 $
 */
 
public class RSW extends DistributionSequentialClassifier implements OptionHandler  {

  /** The left input context for windowising the data. */
  private int lic;
  
  /** The right input context for windowising the data. */
  private int ric;

  /** The left output context for windowising the data. */
  private int loc;
  
  /** The right output context for windowising the data. */
  private int roc;

  /** The base distribution classifier used. */
  private DistributionClassifier m_Classifier = new weka.classifiers.rules.ZeroR();

  /** The windowised dataset. */
  private Instances m_Data = null;

  /** The class index of the unwindowised data. */
  private int originalClassIndex;

  /** The class index of the windowised data . */
  private int classIndexStart;

  /**
   * Builds the classifier.
   */  
  public void buildClassifier(Instances raw) throws Exception {

    Windowise window = new Windowise(lic, ric, loc, roc);
    window.setInputFormat(raw);
    m_Data = Filter.useFilter(raw, window);
	
    m_Data.deleteAttributeAt(0);
    m_Data.deleteAttributeAt(0);
	
    classIndexStart = m_Data.classIndex();
    m_Classifier.buildClassifier(m_Data);

    // free memory
    m_Data = new Instances(m_Data, 0);
  }


  public String toString() {

    return ("DistributionClassifier trained on recurrent sliding window transformed dataset:\n\n" +
	    m_Classifier);
      
  }

  /**
   * Over writes a test instance with the recurrent value plugged in
   * @param inst the test instance.
   * @param line the data to be written on the instance.
   * @param start the starting point for over-writing.
   */  
  private Instance overWrite(Instance inst, double [] line, int start) {

    Instance instance = new Instance(inst);
    int end = start+line.length;
    for(int i = start; i<end; i++)	{
      instance.setValue(i, line[i-start]);
    }
    return (instance);
  }

  /**
   * Reads a test instance after it is classified
   * @param inst the test instance.
   * @param start the starting point for reading.
   * @param end the ending point for reading.
   */  
  private double [] overRead(Instance inst, int start, int end) {

    double [] line = new double[end-start];
    for(int i = start; i<end; i++)	{
      line[i-start] = inst.value(i);
    }
    return(line);
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   *
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector(5);
    
    vec.addElement(new Option(
			      "\tThe base classifier.\n",
			      "W", 1, "-W <base classifier>"));
    vec.addElement(new Option(
			      "\tLeft input context\n"
			      ,"A", 1, "-A <lic>")); 
    vec.addElement(new Option(
			      "\tRight input context\n"
			      ,"B", 1, "-B <ric>"));
    vec.addElement(new Option(
			      "\tLeft output context\n"
			      ,"Y", 1, "-Y <loc>"));
    vec.addElement(new Option(
			      "\tRight output context\n"
			      ,"Z", 1, "-Z <roc>"));  

    if (m_Classifier != null) {
      try {
	vec.addElement(new Option("",
				  "", 0, "\nOptions specific to classifier "
				  + m_Classifier.getClass().getName() + ":"));
	Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
	while (enum.hasMoreElements()) {
	  vec.addElement(enum.nextElement());
	}
      } catch (Exception e) {
      }
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   *
   * -W classifier <br>
   * Sets the base classifier (required).<p>
   * 
   * -A lic <br>
   * Sets the left input context. <p>
   *
   * -B ric <br>
   * Sets the right input context <p>
   *
   * -Y loc <br>
   * Sets the left output context. <p>
   *
   * -Z roc <br>
   * Sets the right output context. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   */
  public void setOptions(String[] options) throws Exception {
  
    String licString = Utils.getOption('A', options);
    if (licString.length() != 0) {
      setLic(Integer.parseInt(licString));
    } else {
      setLic(3);
    }
    String ricString = Utils.getOption('B', options);
    if (ricString.length() != 0) {
      setRic(Integer.parseInt(ricString));
    } else {
      setRic(3);
    }
    String locString = Utils.getOption('Y', options);
    if (locString.length() != 0) {
      setLoc(Integer.parseInt(locString));
    } else {
      setLoc(0);
    }
    String rocString = Utils.getOption('Z', options);
    if (rocString.length() != 0) {
      setRoc(Integer.parseInt(rocString));
    } else {
      setRoc(3);
    }

    if((loc*roc) != 0) {
      throw new Exception("One of the output contexts has to be zero");
    }	
    if((lic < 0) || (ric < 0) || (loc < 0) || (roc < 0)) {
      throw new Exception("Windowizing contexts cannot be negative");
    }
    

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setDistributionClassifier((DistributionClassifier)
                              Classifier.forName(classifierName,
                                                 Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   *
   */
  public String [] getOptions() {

    String [] classifierOptions = new String[0];
    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String[classifierOptions.length + 20];
    int current = 0;

    options[current++] = "-A";
    options[current++] = "" + lic;
    
    options[current++] = "-B";
    options[current++] = "" + ric;
    
    options[current++] = "-Y";
    options[current++] = "" + loc;

    options[current++] = "-Z";
    options[current++] = "" + roc;
    
    if (getDistributionClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getDistributionClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * returns the value of the distribution classifier.
   */
  public DistributionClassifier getDistributionClassifier() {

    return (m_Classifier);
  }


  /**
   * returns the value of the left input context.
   */
  public int getLic() {

    return (lic);
  }

  /**
   * returns the value of the right input context.
   */
  public int getRic() {

    return (ric);
  }

  /**
   * returns the value of the left output context.
   */
  public int getLoc() {

    return (loc);
  }

  /**
   * returns the value of the right output context.
   */
  public int getRoc() {

    return (roc);
  }

  /**
   * sets the value of the distribution classifier.
   */
  public void setDistributionClassifier(DistributionClassifier cf) {

    m_Classifier = cf;
  }


  /**
   * sets the value of the left input context.
   */
  public void setLic(int num) {

    lic = num;
  }

  /**
   * sets the value of the right input context.
   */
  public void setRic(int num) {

    ric = num;
  }

  /**
   * sets the value of the left output context.
   */
  public void setLoc(int num) {

    loc = num;
  }

  /**
   * sets the value of the right output context.
   */
  public void setRoc(int num) {

    roc = num;
  }

  /**
   * Classifies all instances
   * @param instances the test instances
   */
  public double [][] distributionForSequence(Instances instances) throws
  Exception {	

    Filter window = new Windowise(lic, ric, loc, roc);
    window.setInputFormat(instances);
    Instances windowed = Filter.useFilter(instances, window);
    windowed.sort(1);
    windowed.deleteAttributeAt(0);
    windowed.deleteAttributeAt(0);
	
    double [] line;
    double temp;
    int startWrite = 0, startRead, tempDisp = 0, lineLen = 0;

    int winNumClasses = m_Data.numClasses();

    double [][] result = new double [windowed.numInstances()][winNumClasses];
    int classifiedSoFar = 0;
	
    if(loc != 0)  {
      startWrite = classIndexStart-loc;
      tempDisp = -1;
      lineLen = loc - 1;
    }
    if (roc != 0){
      startWrite = classIndexStart+2;
      tempDisp = 1;
      lineLen = roc-1;
    }
	
    line = new double[lineLen];
    startRead = classIndexStart - loc + 1;
	
    double [] classes = new double[windowed.numInstances()];
    temp = winNumClasses - 1;
    for(int i = 0; i<lineLen; i++)
      {
	line[i] = winNumClasses - 1;
      }
	
    if(loc != 0)
      {
	for (int k = windowed.numInstances() - 1; k >= 0; k--)
	  {
	    Instance inst = (Instance)windowed.instance(k);
	    inst.setValue(classIndexStart+tempDisp, temp);
	    inst = overWrite(inst, line, startWrite);
	    inst.setDataset(windowed);
						
	    double [] probDist = ((DistributionClassifier)m_Classifier).distributionForInstance(inst);
	    temp = Utils.maxIndex(probDist);
	    line = overRead(inst, startRead, startRead+lineLen);
	    result[windowed.numInstances()-k] = probDist;
	  }
      }
    else
      {
	for (int k = 0; k < windowed.numInstances(); k++)
	  {
	    Instance inst = (Instance)windowed.instance(k);
	    inst.setValue(classIndexStart+tempDisp, temp);
	    inst = overWrite(inst, line, startWrite);
	    inst.setDataset(m_Data/*windowed*/);
			
	    double [] probDist = ((DistributionClassifier)m_Classifier).distributionForInstance(inst);
			
	    temp = Utils.maxIndex(probDist);
	    line = overRead(inst, startRead, startRead+lineLen);
	    result[windowed.numInstances()-k-1] = probDist;
	  }
      }
    return result;
  }

  /**
   * The main function for testing this class
   */
  public static void main(String args[]) {

    try {
      System.out.println(SequentialEvaluation.
			 evaluateModel(new RSW(), args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

  }

}
