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
 *    StackingC.java
 *    Copyright (C) 1999 Eibe Frank
 *    Copyright (C) 2002 Alexander K. Seewald
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.attribute.Remove;

import java.util.Random;

/**
 <!-- globalinfo-start -->
 * Implements StackingC (more efficient version of stacking).<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * A.K. Seewald: How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness. In: Nineteenth International Conference on Machine Learning, 554-561, 2002.<br/>
 * <br/>
 * Note: requires meta classifier to be a numeric prediction scheme.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Seewald2002,
 *    author = {A.K. Seewald},
 *    booktitle = {Nineteenth International Conference on Machine Learning},
 *    editor = {C. Sammut and A. Hoffmann},
 *    pages = {554-561},
 *    publisher = {Morgan Kaufmann Publishers},
 *    title = {How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness},
 *    year = {2002}
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
 *  Must be a numeric prediction scheme. Default: Linear Regression.</pre>
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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Alexander K. Seewald (alex@seewald.at)
 * @version $Revision: 1.12 $ 
 */
public class StackingC 
  extends Stacking 
  implements OptionHandler, TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = -6717545616603725198L;
  
  /** The meta classifiers (one for each class, like in ClassificationViaRegression) */
  protected Classifier [] m_MetaClassifiers = null;
  
  /** Filter to transform metaData - Remove */
  protected Remove m_attrFilter = null;
  /** Filter to transform metaData - MakeIndicator */
  protected MakeIndicator m_makeIndicatorFilter = null;

  /**
   * The constructor.
   */
  public StackingC() {
    m_MetaClassifier = new weka.classifiers.functions.LinearRegression();
    ((LinearRegression)(getMetaClassifier())).
      setAttributeSelectionMethod(new 
	weka.core.SelectedTag(1, LinearRegression.TAGS_SELECTION));
  }  
      
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements StackingC (more efficient version of stacking).\n\n"
      + "For more information, see\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "Note: requires meta classifier to be a numeric prediction scheme.";
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
    result.setValue(Field.AUTHOR, "A.K. Seewald");
    result.setValue(Field.TITLE, "How to Make Stacking Better and Faster While Also Taking Care of an Unknown Weakness");
    result.setValue(Field.BOOKTITLE, "Nineteenth International Conference on Machine Learning");
    result.setValue(Field.EDITOR, "C. Sammut and A. Hoffmann");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.PAGES, "554-561");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann Publishers");
    
    return result;
  }

  /**
   * String describing option for setting meta classifier
   * 
   * @return string describing the option
   */
  protected String metaOption() {

    return "\tFull name of meta classifier, followed by options.\n"
      + "\tMust be a numeric prediction scheme. Default: Linear Regression.";
  }

  /**
   * Process options setting meta classifier.
   * 
   * @param options the meta options to parse
   * @throws Exception if parsing fails
   */
  protected void processMetaOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('M', options);
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length != 0) {
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
    } else {
        ((LinearRegression)(getMetaClassifier())).
	  setAttributeSelectionMethod(new 
	    weka.core.SelectedTag(1,LinearRegression.TAGS_SELECTION));
    }
  }

  /**
   * Method that builds meta level.
   * 
   * @param newData the data to work with
   * @param random the random number generator to use for cross-validation
   * @throws Exception if generation fails
   */
  protected void generateMetaLevel(Instances newData, Random random) 
    throws Exception {

    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      Instances train = newData.trainCV(m_NumFolds, j, random);

      // Build base classifiers
      for (int i = 0; i < m_Classifiers.length; i++) {
	getClassifier(i).buildClassifier(train);
      }

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
	metaData.add(metaInstance(test.instance(i)));
      }
    }
    
    m_MetaClassifiers = Classifier.makeCopies(m_MetaClassifier,
					      m_BaseFormat.numClasses());
    
    int [] arrIdc = new int[m_Classifiers.length + 1];
    arrIdc[m_Classifiers.length] = metaData.numAttributes() - 1;
    Instances newInsts;
    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
	arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter = new weka.filters.unsupervised.attribute.MakeIndicator();
      m_makeIndicatorFilter.setAttributeIndex("" + (metaData.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(metaData);
      newInsts = Filter.useFilter(metaData,m_makeIndicatorFilter);
      
      m_attrFilter = new weka.filters.unsupervised.attribute.Remove();
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      newInsts = Filter.useFilter(newInsts,m_attrFilter);
      
      newInsts.setClassIndex(newInsts.numAttributes()-1);
      
      m_MetaClassifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Classifies a given instance using the stacked classifier.
   *
   * @param instance the instance to be classified
   * @return the distribution
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    int [] arrIdc = new int[m_Classifiers.length+1];
    arrIdc[m_Classifiers.length] = m_MetaFormat.numAttributes() - 1;
    double [] classProbs = new double[m_BaseFormat.numClasses()];
    Instance newInst;
    double sum = 0;

    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
          arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter.setAttributeIndex("" + (m_MetaFormat.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(m_MetaFormat);
      m_makeIndicatorFilter.input(metaInstance(instance));
      m_makeIndicatorFilter.batchFinished();
      newInst = m_makeIndicatorFilter.output();

      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      m_attrFilter.input(newInst);
      m_attrFilter.batchFinished();
      newInst = m_attrFilter.output();

      classProbs[i]=m_MetaClassifiers[i].classifyInstance(newInst);
      if (classProbs[i] > 1) { classProbs[i] = 1; }
      if (classProbs[i] < 0) { classProbs[i] = 0; }
      sum += classProbs[i];
    }

    if (sum!=0) Utils.normalize(classProbs,sum);

    return classProbs;
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a string representation of the classifier
   */
  public String toString() {

    if (m_MetaFormat == null) {
      return "StackingC: No model built yet.";
    }
    String result = "StackingC\n\nBase classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += getClassifier(i).toString() +"\n\n";
    }
   
    result += "\n\nMeta classifiers (one for each class)\n\n";
    for (int i = 0; i< m_MetaClassifiers.length; i++) {
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
    runClassifier(new StackingC(), argv);
  }
}

