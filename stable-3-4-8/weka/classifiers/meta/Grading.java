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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4.2.2 $ 
 */
public class Grading extends Stacking {

  /** The meta classifiers, one for each base classifier. */
  protected Classifier [] m_MetaClassifiers = new Classifier[0];

  /** InstPerClass */
  protected double [] m_InstPerClass = null;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Implements Grading. The base classifiers are \"graded\". "
      + "For more information, see\n\n"
      + "Seewald A.K., Fuernkranz J. (2001): An Evaluation of Grading "
      + "Classifiers, in Hoffmann F. et al. (eds.), Advances in Intelligent "
      + "Data Analysis, 4th International Conference, IDA 2001, Proceedings, "
      + "Springer, Berlin/Heidelberg/New York/Tokyo, pp.115-124, 2001";
  }

  /**
   * Generates the meta data
   */
  protected void generateMetaLevel(Instances newData, Random random) 
    throws Exception {

    m_MetaFormat = metaFormat(newData);
    Instances [] metaData = new Instances[m_Classifiers.length];
    for (int i = 0; i < m_Classifiers.length; i++) {
      metaData[i] = metaFormat(newData);
    }
    for (int j = 0; j < m_NumFolds; j++) {

      Instances train = newData.trainCV(m_NumFolds, j, random);
      Instances test = newData.testCV(m_NumFolds, j);

      // Build base classifiers
      for (int i = 0; i < m_Classifiers.length; i++) {
	getClassifier(i).buildClassifier(train);
        for (int k = 0; k < test.numInstances(); k++) {
	  metaData[i].add(metaInstance(test.instance(k),i));
        }
      }
    }
        
    // calculate InstPerClass
    m_InstPerClass = new double[newData.numClasses()];
    for (int i=0; i < newData.numClasses(); i++) m_InstPerClass[i]=0.0;
    for (int i=0; i < newData.numInstances(); i++) {
      m_InstPerClass[(int)newData.instance(i).classValue()]++;
    }
    
    m_MetaClassifiers = Classifier.makeCopies(m_MetaClassifier,
					      m_Classifiers.length);

    for (int i = 0; i < m_Classifiers.length; i++) {
      m_MetaClassifiers[i].buildClassifier(metaData[i]);
    }
  }

  /**
   * Returns class probabilities for a given instance using the stacked classifier.
   * One class will always get all the probability mass (i.e. probability one).
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double maxConf, maxPreds;
    int numPreds=0;
    int numClassifiers=m_Classifiers.length;
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
      idxPreds=(int)(m_Classifiers[i].classifyInstance(instance));
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

    if (m_Classifiers.length == 0) {
      return "Grading: No base schemes entered.";
    }
    if (m_MetaClassifiers.length == 0) {
      return "Grading: No meta scheme selected.";
    }
    if (m_MetaFormat == null) {
      return "Grading: No model built yet.";
    }
    String result = "Grading\n\nBase classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += getClassifier(i).toString() +"\n\n";
    }
   
    result += "\n\nMeta classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += m_MetaClassifiers[i].toString() +"\n\n";
    }

    return result;
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
    
    for (int i = 0; i<instances.numAttributes(); i++) {
	if ( i != instances.classIndex() ) {
	    attributes.addElement(instances.attribute(i));
	}
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

    int idx = 0;
    for (i = 0; i < instance.numAttributes(); i++) {
	if (i != instance.classIndex()) {
	    values[idx] = instance.value(i);
	    idx++;
	}
    }

    Classifier classifier = getClassifier(k);

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
    
    values[idx]=predConf;
    metaInstance = new Instance(1, values);
    metaInstance.setDataset(m_MetaFormat);
    return metaInstance;
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
}
