/*
 *    Classifier.java
 *    Copyright (C) 1999 Eibe Frank, Len Trigg
 *
 */

package weka.classifiers;

import java.io.*;
import weka.core.*;

/** 
 * Abstract classifier. All schemes for numeric or nominal prediction in
 * Weka extend this class.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public abstract class Classifier implements Cloneable, Serializable {
 
  /**
   * Generates a classifier. Must initialize all fields of the classifier
   * that are not being set via options (ie. multiple calls of buildClassifier
   * must always lead to the same result). Must not change the dataset
   * in any way.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the classifier has not been 
   * generated successfully
   */
  public abstract void buildClassifier(Instances data) throws Exception;

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class as a double
   * if the class is nominal, otherwise the predicted value
   * @exception Exception if instance could not be classified
   * successfully
   */
  public abstract double classifyInstance(Instance instance) throws Exception; 


  
  /**
   * Creates a new instance of a classifier given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * classifier implements OptionHandler and the options parameter is
   * non-null, the classifier will have it's options set.
   *
   * @param classifierName the fully qualified class name of the classifier
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created classifier, ready for use.
   * @exception Exception if the classifier name is invalid, or the options
   * supplied are not acceptable to the classifier
   */
  public static Classifier forName(String classifierName,
				   String [] options) throws Exception {

    return (Classifier)Utils.forName(Classifier.class,
				     classifierName,
				     options);
  }

  /**
   * Creates copies of the current classifier, which can then
   * be used for boosting etc. Note that this is not
   * designed to copy any currently built <i>model</i>, just the
   * option settings.
   *
   * @param model an example classifier to copy
   * @param num the number of classifiers copies to create.
   * @return an array of classifiers.
   * @exception Exception if an error occurs
   */
  public static Classifier [] makeCopies(Classifier model,
					 int num) throws Exception {

    if (model == null) {
      throw new Exception("No model classifier set");
    }
    Classifier [] classifiers = new Classifier [num];
    String [] options = null;
    if (model instanceof OptionHandler) {
      options = ((OptionHandler)model).getOptions();
    }
    for(int i = 0; i < classifiers.length; i++) {
      classifiers[i] = (Classifier) model.getClass().newInstance();
      if (options != null) {
	String [] tempOptions = (String [])options.clone();
	((OptionHandler)classifiers[i]).setOptions(tempOptions);
	Utils.checkForRemainingOptions(tempOptions);
      }
    }
    return classifiers;
  }

}

