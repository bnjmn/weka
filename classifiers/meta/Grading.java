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
 *    Copyright (C) 2000 University of Waikato
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Random;

/**
 <!-- globalinfo-start -->
 * Implements Grading. The base classifiers are "graded".<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * A.K. Seewald, J. Fuernkranz: An Evaluation of Grading Classifiers. In: Advances in Intelligent Data Analysis: 4th International Conference, Berlin/Heidelberg/New York/Tokyo, 115-124, 2001.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Seewald2001,
 *    address = {Berlin/Heidelberg/New York/Tokyo},
 *    author = {A.K. Seewald and J. Fuernkranz},
 *    booktitle = {Advances in Intelligent Data Analysis: 4th International Conference},
 *    editor = {F. Hoffmann et al.},
 *    pages = {115-124},
 *    publisher = {Springer},
 *    title = {An Evaluation of Grading Classifiers},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -M &lt;scheme specification&gt;
 *  Full name of meta classifier, followed by options.
 *  (default: "weka.classifiers.rules.Zero")</pre>
 * 
 * <pre> -X &lt;number of folds&gt;
 *  Sets the number of cross-validation folds.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -B &lt;classifier specification&gt;
 *  Full class name of classifier to include, followed
 *  by scheme options. May be specified multiple times.
 *  (default: "weka.classifiers.rules.ZeroR")</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Alexander K. Seewald (alex@seewald.at)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $ 
 */
public class Grading 
  extends Stacking
  implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5207837947890081170L;
  
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

    return 
        "Implements Grading. The base classifiers are \"graded\".\n\n"
      + "For more information, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "A.K. Seewald and J. Fuernkranz");
    result.setValue(Field.TITLE, "An Evaluation of Grading Classifiers");
    result.setValue(Field.BOOKTITLE, "Advances in Intelligent Data Analysis: 4th International Conference");
    result.setValue(Field.EDITOR, "F. Hoffmann et al.");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "115-124");
    result.setValue(Field.PUBLISHER, "Springer");
    result.setValue(Field.ADDRESS, "Berlin/Heidelberg/New York/Tokyo");
    
    return result;
  }

  /**
   * Generates the meta data
   * 
   * @param newData the data to work on
   * @param random the random number generator used in the generation
   * @throws Exception if generation fails
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
   * @throws Exception if instance could not be classified
   * successfully
   * @return the class distribution for the given instance
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    double maxPreds;
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
   * 
   * @return a string representation of the classifier
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
   * @throws Exception if an error occurs
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
   * @param k index of the classifier
   * @return the level-1 instance
   * @throws Exception if an error occurs
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
    runClassifier(new Grading(), argv);
  }
}

