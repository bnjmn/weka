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
 *    DistributionMetaClassifier.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.rules.ZeroR;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Class that wraps up a Classifier and presents it as a DistributionClassifier
 * for ease of programmatically handling Classifiers in general -- only the
 * one predict method (distributionForInstance) need be worried about. The
 * distributions produced by this classifier place a probability of 1 on the
 * class value predicted by the sub-classifier.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a sub-classifier (required).<p>
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.7 $
 */
public class DistributionMetaClassifier extends DistributionClassifier 
  implements OptionHandler {

  /** The classifier. */
  private Classifier m_Classifier = new weka.classifiers.rules.ZeroR();

  /** Default constructor */
  public DistributionMetaClassifier() { }

  /**
   * Creates a new <code>DistributionMetaClassifier</code> instance,
   * specifying the Classifier to wrap around.
   *
   * @param subClassifier a <code>Classifier</code>.
   */
  public DistributionMetaClassifier(Classifier subClassifier) {

    setClassifier(subClassifier);
  }


  /**
   * Builds the classifier.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    m_Classifier.buildClassifier(insts);
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    double[] result = new double[inst.numClasses()];
    double predictedClass = m_Classifier.classifyInstance(inst);
    if (Instance.isMissingValue(predictedClass)) {
      return result;
    }
    if (inst.classAttribute().type() == Attribute.NOMINAL) {
      result[(int)predictedClass] = 1.0;
    } else {
      result[0] = predictedClass;
    }
    return result;
  }

  /**
   * Prints the classifiers.
   */
  public String toString() {

    return "DistributionMetaClassifier: " + m_Classifier.toString() + "\n";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector(1);
    Object c;
    
    vec.addElement(new Option("\tSets the base classifier.",
			      "W", 1, "-W <base classifier>"));
    
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
   * -W classname <br>
   * Specify the full class name of a learner as the basis for 
   * the multiclassclassifier (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 3];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
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
   * Set the base classifier. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    DistributionClassifier scheme;

    try {
      scheme = new DistributionMetaClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
