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
 *    Grading.java
 *    Copyright (C) 2000 Alexander K. Seewald
 *
 */

package weka.classifiers.meta;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;

/**
 * Implements Grading. For more information, see<p>
 *
 *  Seewald A.K., Fuernkranz J. (2001): An Evaluation of Grading
 *    Classifiers, in Hoffmann F.\ et al.\ (eds.), Advances in Intelligent
 *    Data Analysis, 4th International Conference, IDA 2001, Proceedings,
 *    Springer, Berlin/Heidelberg/New York/Tokyo, pp.115-124, 2001
 *
 * Valid options are:<p>
 *
 * -X num_folds <br>
 * The number of folds for the cross-validation (default 10).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a base scheme
 * followed by options to the classifier.
 * (required, option should be used once for each classifier).<p>
 *
 * -M classifierstring <br>
 * Classifierstring for the meta classifier. Same format as for base
 * classifiers. This classifier estimates confidence in prediction of
 * base classifiers. (required) <p>
 *
 * @author Alexander K. Seewald (alex@seewald.at)
 * @version $Revision: 1.2 $ 
 */
public class Grading extends Classifier implements OptionHandler {

  /** The meta classifiers, one for each base classifier. */
  protected Classifier [] m_MetaClassifiers = {
     new weka.classifiers.rules.ZeroR()
  };

  /** The base classifiers. */
  protected Classifier [] m_BaseClassifiers = {
     new weka.classifiers.rules.ZeroR()
  };
 
  /** Format for meta data */
  protected Instances m_MetaFormat = null;

  /** Format for base data */
  protected Instances m_BaseFormat = null;

  /** Set the number of folds for the cross-validation */
  protected int m_NumFolds = 10;

  /** Random number seed */
  protected int m_Seed = 1;

  /** InstPerClass */
  protected double [] m_InstPerClass = null;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);
    newVector.addElement(new Option(
	      "\tFull class name of base classifiers to include, followed "
	      + "by scheme options\n"
	      + "\t(may be specified multiple times).\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -K\"",
	      "B", 1, "-B <scheme specification>"));
    newVector.addElement(new Option(
	      "\tFull name of meta classifier, followed by options.",
	      "M", 0, "-M <scheme specification>"));
    newVector.addElement(new Option(
	      "\tSets the number of cross-validation folds.",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed.",
	      "S", 1, "-S <random number seed>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -X num_folds <br>
   * The number of folds for the cross-validation (default 10).<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a base scheme
   * followed by options to the classifier.
   * (required, option should be used once for each classifier).<p>
   *
   * -M classifierstring <br>
   * Classifierstring for the meta classifier. Same format as for base
   * classifiers. (required) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String numFoldsString = Utils.getOption('X', options);
    if (numFoldsString.length() != 0) {
      setNumFolds(Integer.parseInt(numFoldsString));
    } else {
      setNumFolds(10);
    }
    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    // Iterate through the schemes
    FastVector classifiers = new FastVector();
    while (true) {
      String classifierString = Utils.getOption('B', options);
      if (classifierString.length() == 0) {
	break;
      }
      String [] classifierSpec = Utils.splitOptions(classifierString);
      if (classifierSpec.length == 0) {
	throw new Exception("Invalid classifier specification string");
      }
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      classifiers.addElement(Classifier.forName(classifierName,
						classifierSpec));
    }
    if (classifiers.size() == 0) {
      throw new Exception("At least one base classifier must be specified"
			  + " with the -B option.");
    } else {
      Classifier [] classifiersArray = new Classifier [classifiers.size()];
      for (int i = 0; i < classifiersArray.length; i++) {
	classifiersArray[i] = (Classifier) classifiers.elementAt(i);
      }
      setBaseClassifiers(classifiersArray);
    }

    String classifierString = Utils.getOption('M', options);
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Meta classifier has to be provided.");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String[6];
    int current = 0;

    if (m_BaseClassifiers.length != 0) {
      options = new String [m_BaseClassifiers.length * 2 + 6];
      for (int i = 0; i < m_BaseClassifiers.length; i++) {
	options[current++] = "-B";
	options[current++] = "" + getBaseClassifierSpec(i);
      }
    }
    options[current++] = "-X"; options[current++] = "" + getNumFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (m_MetaClassifiers.length != 0) {
      options[current++] = "-M";
      options[current++] = getClassifierSpec(m_MetaClassifiers[0]);
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets the seed for random number generation.
   *
   * @param seed the random number seed
   */
  public void setSeed(int seed) {
    
    m_Seed = seed;;
  }

  /**
   * Gets the random number seed.
   * 
   * @return the random number seed
   */
  public int getSeed() {

    return m_Seed;
  }

  /** 
   * Gets the number of folds for the cross-validation.
   *
   * @return the number of folds for the cross-validation
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Sets the number of folds for the cross-validation.
   *
   * @param numFolds the number of folds for the cross-validation
   * @exception Exception if parameter illegal
   */
  public void setNumFolds(int numFolds) throws Exception {
    
    if (numFolds < 0) {
      throw new Exception("Grading: Number of cross-validation " +
			  "folds must be positive.");
    }
    m_NumFolds = numFolds;
  }

  /**
   * Sets the list of possible classifers to choose from.
   *
   * @param classifiers an array of classifiers with all options set.
   */
  public void setBaseClassifiers(Classifier [] classifiers) throws Exception {

    m_BaseClassifiers = classifiers;
    setMetaClassifier(new weka.classifiers.rules.ZeroR());
  }

  /**
   * Gets the list of possible classifers to choose from.
   *
   * @return the array of Classifiers
   */
  public Classifier [] getBaseClassifiers() {

    return m_BaseClassifiers;
  }

  /**
   * Gets the specific classifier from the set of base classifiers.
   *
   * @param index the index of the classifier to retrieve
   * @return the classifier
   */
  public Classifier getBaseClassifier(int index) {
    
    return m_BaseClassifiers[index];
  }

  /**
   * Adds meta classifier
   *
   * @param classifier the classifier with all options set.
   */
  public void setMetaClassifier(Classifier classifier) throws Exception {
    m_MetaClassifiers = Classifier.makeCopies(classifier,m_BaseClassifiers.length);
    /*m_MetaClassifiers = new Classifier [m_BaseClassifiers.length];
      for (int i = 0; i < m_MetaClassifiers.length; i++) {
	m_MetaClassifiers[i] = classifier;
      }*/
  }
  
  /**
   * Gets the meta classifiers.
   *
   * @return the array of meta classifiers
   */
  public Classifier [] getMetaClassifiersX() {
    
    return m_MetaClassifiers;
  }


  /**
   * Gets the meta classifiers.
   *
   * @return the array of meta classifiers
   */
  public Classifier getMetaClassifier() {
    
    return m_MetaClassifiers[0];
  }

  /**
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_BaseClassifiers.length == 0) {
      throw new Exception("No base classifiers have been set");
    }
    if (m_MetaClassifiers.length == 0) {
      throw new Exception("No meta classifier has been set");
    }
    if (!(data.classAttribute().isNominal() ||
	  data.classAttribute().isNumeric())) {
      throw new Exception("Class attribute has to be nominal or numeric!");
    }
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();
    if (newData.numInstances() == 0) {
      throw new Exception("No training instances without missing class!");
    }
    newData.randomize(new Random(m_Seed));
    if (newData.classAttribute().isNominal())
      newData.stratify(m_NumFolds);
    int numClassifiers = m_BaseClassifiers.length;
    // Create meta data
    m_MetaFormat=metaFormat(newData);
    Instances [] metaData = new Instances[numClassifiers];
    for (int i = 0; i < m_BaseClassifiers.length; i++) {
      metaData[i] = metaFormat(newData);
    }
    for (int j = 0; j < m_NumFolds; j++) {

      Instances train = newData.trainCV(m_NumFolds, j);
      Instances test = newData.testCV(m_NumFolds, j);

      // Build base classifiers
      for (int i = 0; i < m_BaseClassifiers.length; i++) {
	getBaseClassifier(i).buildClassifier(train);
        for (int k = 0; k < test.numInstances(); k++) {
	  metaData[i].add(metaInstance(test.instance(k),i));
        }
      }
    }

        
    // calculate InstPerClass
    m_InstPerClass = new double[newData.numClasses()];
    for (int i=0; i < newData.numClasses(); i++) m_InstPerClass[i]=0.0;
    for (int i=0; i < newData.numInstances(); i++)
      m_InstPerClass[(int)newData.instance(i).classValue()]++;

    // Rebuilt all the base classifiers on the full training data
    // and confidence prediction classifiers on meta data.
    for (int i = 0; i < numClassifiers; i++) {
      getBaseClassifier(i).buildClassifier(newData);
      //System.out.println(metaData[i].toString());
      m_MetaClassifiers[i].buildClassifier(metaData[i]);
    }
  }

  /**
   * Returns class probabilities for a given instance using the stacked classifier.
   * One class will always get all the probability mass (i.e. probabilit one).
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double maxConf, maxPreds;
    int numPreds=0;
    int numClassifiers=m_BaseClassifiers.length;
    int idxPreds;
    double [] predConfs = new double[numClassifiers];
    double [] preds;

    for (int i=0; i<numClassifiers; i++) {
      preds = m_MetaClassifiers[i].distributionForInstance(metaInstance(instance,i));
      if (m_MetaClassifiers[i].classifyInstance(metaInstance(instance,i))==1)
        predConfs[i]=preds[1];
      else
        predConfs[i]=-preds[0];
    }
    if (predConfs[Utils.maxIndex(predConfs)]<0.0) { // no correct classifiers
      for (int i=0; i<numClassifiers; i++)   // use neg. confidences instead
        predConfs[i]=1.0+predConfs[i];
    } else {
      for (int i=0; i<numClassifiers; i++)   // otherwise ignore neg. conf
        if (predConfs[i]<0) predConfs[i]=0.0;
    }

    /*System.out.print(preds[0]);
    System.out.print(":");
    System.out.print(preds[1]);
    System.out.println("#");*/

    preds=new double[instance.numClasses()];
    for (int i=0; i<instance.numClasses(); i++) preds[i]=0.0;
    for (int i=0; i<numClassifiers; i++) {
      idxPreds=(int)(m_BaseClassifiers[i].classifyInstance(instance));
      preds[idxPreds]+=predConfs[i];
    }

    maxPreds=preds[Utils.maxIndex(preds)];
    int MaxInstPerClass=-100;
    int MaxClass=-1;
    for (int i=0; i<instance.numClasses(); i++) {
      if (preds[i]==maxPreds) {
        numPreds++;
        if (m_InstPerClass[i]>MaxInstPerClass) {
          MaxInstPerClass=(int)m_InstPerClass[i];
          MaxClass=i;
        }
      }
    }

    int predictedIndex;
    if (numPreds==1)
      predictedIndex = Utils.maxIndex(preds);
    else
    {
      // System.out.print("?");
      // System.out.print(instance.toString());
      // for (int i=0; i<instance.numClasses(); i++) {
      //   System.out.print("/");
      //   System.out.print(preds[i]);
      // }
      // System.out.println(MaxClass);
      predictedIndex = MaxClass;
    }
    double[] classProbs = new double[instance.numClasses()];
    classProbs[predictedIndex] = 1.0;
    return classProbs;
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_BaseClassifiers.length == 0) {
      return "Grading: No base schemes entered.";
    }
    if (m_MetaClassifiers.length == 0) {
      return "Grading: No meta scheme selected.";
    }
    if (m_MetaFormat == null) {
      return "Grading: No model built yet.";
    }
    String result = "Grading\n\nBase classifiers\n\n";
    for (int i = 0; i < m_BaseClassifiers.length; i++) {
      result += getBaseClassifier(i).toString() +"\n\n";
    }
   
    result += "\n\nMeta classifiers\n\n";
    for (int i = 0; i < m_BaseClassifiers.length; i++) {
      result += m_MetaClassifiers[i].toString() +"\n\n";
    }

    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new Grading(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Makes the format for the level-1 data.
   *
   * @param instances the level-0 format
   * @return the format for the meta data
   */
  protected Instances metaFormat(Instances instances) throws Exception {

    FastVector attributes = new FastVector();
    Instances metaFormat;
    
    for (int i = 0; i<instances.numAttributes()-1; i++) {
      attributes.addElement(instances.attribute(i));
    }

    FastVector nomElements = new FastVector(2);
    nomElements.addElement("0");
    nomElements.addElement("1");
    attributes.addElement(new Attribute("PredConf",nomElements));

    metaFormat = new Instances("Meta format", attributes, 0);
    metaFormat.setClassIndex(metaFormat.numAttributes()-1);
    return metaFormat;
  }

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param index the index of the classifier string to retrieve, starting from
   * 0.
   * @return the classifier string, or the empty string if no classifier
   * has been assigned (or the index given is out of range).
   */
  protected String getBaseClassifierSpec(int index) {
    
    if (m_BaseClassifiers.length < index) {
      return "";
    }
    return getClassifierSpec(getBaseClassifier(index));
  }
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @param c the classifier
   * @return the classifier specification string.
   */
  protected String getClassifierSpec(Classifier c) {
    
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Makes a level-1 instance from the given instance.
   * 
   * @param instance the instance to be transformed
   * @return the level-1 instance
   */
  protected Instance metaInstance(Instance instance, int k) throws Exception {

    double[] values = new double[m_MetaFormat.numAttributes()];
    Instance metaInstance;
    double predConf;
    int i;
    int maxIdx;
    double maxVal;
    for (i = 0;i<instance.numAttributes()-1;i++) {
      values[i] = instance.value(i);
    }

    Classifier classifier = getBaseClassifier(k);

    if (m_BaseFormat.classAttribute().isNumeric()) {
      throw new Exception("Class Attribute must not be numeric!");
    } else {
      double[] dist = classifier.distributionForInstance(instance);
      
      maxIdx=0;
      maxVal=dist[0];
      for (int j = 1; j < dist.length; j++) {
	if (dist[j]>maxVal) {
	  maxVal=dist[j];
	  maxIdx=j;
	}
      }
      predConf= (instance.classValue()==maxIdx) ? 1:0;
    }
    
    values[i]=predConf;
    metaInstance = new Instance(1, values);
    metaInstance.setDataset(m_MetaFormat);
    return metaInstance;
  }
}
