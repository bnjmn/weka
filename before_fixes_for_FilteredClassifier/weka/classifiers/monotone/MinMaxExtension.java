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
 *    MinMaxExtension.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.monotone;

import weka.classifiers.Classifier;
import weka.classifiers.monotone.util.InstancesUtil;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * This class is an implementation of the minimal and maximal extension.<br/>
 * All attributes and the class are assumed to be ordinal. The order of the ordinal attributes is determined by the internal codes used by WEKA.<br/>
 * <br/>
 * Further information regarding these algorithms can be found in:<br/>
 * <br/>
 * S. Lievens, B. De Baets, K. Cao-Van (2006). A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting. Annals of Operations Research..<br/>
 * <br/>
 * Kim Cao-Van (2003). Supervised ranking: from semantics to algorithms.<br/>
 * <br/>
 * Stijn Lievens (2004). Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken.<br/>
 * <br/>
 * For more information about supervised ranking, see<br/>
 * <br/>
 * http://users.ugent.be/~slievens/supervised_ranking.php
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Lievens2006,
 *    author = {S. Lievens and B. De Baets and K. Cao-Van},
 *    journal = {Annals of Operations Research},
 *    title = {A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting},
 *    year = {2006}
 * }
 * 
 * &#64;phdthesis{Cao-Van2003,
 *    author = {Kim Cao-Van},
 *    school = {Ghent University},
 *    title = {Supervised ranking: from semantics to algorithms},
 *    year = {2003}
 * }
 * 
 * &#64;mastersthesis{Lievens2004,
 *    author = {Stijn Lievens},
 *    school = {Ghent University},
 *    title = {Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -M
 *  Use maximal extension (default: minimal extension)</pre>
 * 
 <!-- options-end -->
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.1 $
 */
public class MinMaxExtension
  extends Classifier 
  implements TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = 8505830465540027104L;

  /**
   * The training instances.
   */
  private Instances m_data;

  /**
   * parameter for choice between min extension and max extension
   */
  private boolean m_min = true;


  /**
   * Returns a string describing the classifier.
   * @return a description suitable for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "This class is an implementation of the "
      + "minimal and maximal extension.\n" 
      + "All attributes and the class are assumed to be ordinal. The order of "
      + "the ordinal attributes is determined by the internal codes used by "
      + "WEKA.\n\n"
      + "Further information regarding these algorithms can be found in:\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "For more information about supervised ranking, see\n\n"
      + "http://users.ugent.be/~slievens/supervised_ranking.php";
  }
  
  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "S. Lievens and B. De Baets and K. Cao-Van");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE, "A Probabilistic Framework for the Design of Instance-Based Supervised Ranking Algorithms in an Ordinal Setting");
    result.setValue(Field.JOURNAL, "Annals of Operations Research");

    additional = result.add(Type.PHDTHESIS);
    additional.setValue(Field.AUTHOR, "Kim Cao-Van");
    additional.setValue(Field.YEAR, "2003");
    additional.setValue(Field.TITLE, "Supervised ranking: from semantics to algorithms");
    additional.setValue(Field.SCHOOL, "Ghent University");

    additional = result.add(Type.MASTERSTHESIS);
    additional.setValue(Field.AUTHOR, "Stijn Lievens");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.TITLE, "Studie en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd rangschikken");
    additional.setValue(Field.SCHOOL, "Ghent University");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }


  /**
   * Builds the classifier. This is in fact nothing else than copying
   * the given instances.
   *
   * @param instances the training examples
   * @throws Exception if the classifier is not able to handle the 
   *  <code> instances </code>.
   */
  public void buildClassifier(Instances instances) throws Exception {

    getCapabilities().testWithFail(instances);

    // copy the dataset 
    m_data = new Instances(instances);

    // new dataset in which examples with missing class value are removed
    m_data.deleteWithMissingClass();
  }

  /**
   * Classifies the given instance. 
   *
   * @param instance the instance to be classified
   * @return a  double representing the internal value
   * of the label that is assigned to the given instance
   */
  public double classifyInstance(Instance instance) {
    double value;
    if (m_min == true) {
      value = 0;
      for (int i = 0; i < m_data.numInstances(); i++) {
	Instance i2 = m_data.instance(i);
	if (InstancesUtil.smallerOrEqual(i2, instance) == true) {
	  value = Math.max(value, i2.classValue());
	}
      }
    }
    else {
      value = m_data.classAttribute().numValues() - 1;
      for (int i = 0; i < m_data.numInstances(); i++) {
	Instance i2 = m_data.instance(i);
	if (InstancesUtil.smallerOrEqual(instance, i2) == true) {
	  value = Math.min(value, i2.classValue());
	}
      }
    }
    return value;
  }

  /**
   * After calling this method, the next classification will use the minimal
   * extension.
   */
  public void setMinExtension() {
    m_min = true;
  }

  /**
   * After calling this method, the next classification will use the maximal
   * extension.
   */
  public void setMaxExtension() {
    m_min = false;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for 
   * displaying in the explorer/experimenter gui
   */
  public String minMaxExtensionTipText() {
    return "If true, the minimal extension of the algorithm is chosen, "
    + "otherwise, it is the maximal extension";
  }

  /**
   * Return if the minimal extension is in effect.  
   *
   * @return <code> true </code> if the minimal is in effect, 
   * <code> false </code> otherwise
   */
  public boolean getMinMaxExtension() {
    return m_min;
  }
  /**
   * Chooses between the minimal and maximal extension of the algorithm.
   * If <code> min </code> is <code> true </code> then the minimal extension
   * wil be in effect, otherwise it will the maximal extension.
   *
   * @param min do we choose the minimal extension
   */
  public void setMinMaxExtension(boolean min) {
    m_min = min;
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -M
   *  Use maximal extension (default: minimal extension)</pre>
   * 
   <!-- options-end -->
   *
   * @param options an array of strings containing the options for the classifier
   * @throws Exception if 
   */
  public void setOptions(String[] options) throws Exception {
    m_min = !Utils.getFlag('M',options); // check if -M option is present
    
    super.setOptions(options);
  }

  /**
   * Gets the current settings of this classifier.
   *
   * @return an array of strings suitable for passing to 
   * <code> setOptions </code>
   */
  public String[] getOptions() {
    int       	i;
    Vector    	result;
    String[]  	options;

    result = new Vector();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (!m_min)
      result.add("-M");

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Produces an enumeration describing the available options for 
   * this classifier.
   *
   * @return an enumeration with the available options.
   */
  public Enumeration listOptions() {
    Vector options = new Vector();

    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      options.addElement(enm.nextElement());

    String s = "\tUse maximal extension (default: minimal extension)";
    options.add(new Option(s, "M", 0, "-M"));
    
    return options.elements();
  }
  
  /**
   * returns a string representation of this classifier
   * 
   * @return the classname
   */
  public String toString() {
    return this.getClass().getName();
  }

  /**
   * Main method for testing this class and for using it from the
   * command line.
   *
   * @param args array of options for both the classifier <code>
   * MinMaxExtension </code> and for <code> evaluateModel </code>
   */
  public static void main(String[] args) {
    runClassifier(new MinMaxExtension(), args);
  }
}
