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
 *    OrdinalClassClassifier.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.rules.ZeroR;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Meta classifier that allows standard classification algorithms to be applied to ordinal class problems.<br/>
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Eibe Frank, Mark Hall: A Simple Approach to Ordinal Classification. In: 12th European Conference on Machine Learning, 145-156, 2001.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Frank2001,
 *    author = {Eibe Frank and Mark Hall},
 *    booktitle = {12th European Conference on Machine Learning},
 *    pages = {145-156},
 *    publisher = {Springer},
 *    title = {A Simple Approach to Ordinal Classification},
 *    year = {2001}
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
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision 1.0 $
 * @see OptionHandler
 */
public class OrdinalClassClassifier 
  extends SingleClassifierEnhancer 
  implements OptionHandler, TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = -3461971774059603636L;

  /** The classifiers. (One for each class.) */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicator[] m_ClassFilters;

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Default constructor.
   */
  public OrdinalClassClassifier() {
    m_Classifier = new weka.classifiers.trees.J48();
  }

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Meta classifier that allows standard classification algorithms "
      +"to be applied to ordinal class problems.\n\n"
      + "For more information see: \n\n"
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
    result.setValue(Field.AUTHOR, "Eibe Frank and Mark Hall");
    result.setValue(Field.TITLE, "A Simple Approach to Ordinal Classification");
    result.setValue(Field.BOOKTITLE, "12th European Conference on Machine Learning");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "145-156");
    result.setValue(Field.PUBLISHER, "Springer");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @throws Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();
    
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);

    int numClassifiers = insts.numClasses() - 1;

    numClassifiers = (numClassifiers == 0) ? 1 : numClassifiers;

    if (numClassifiers == 1) {
      m_Classifiers = Classifier.makeCopies(m_Classifier, 1);
      m_Classifiers[0].buildClassifier(insts);
    } else {
      m_Classifiers = Classifier.makeCopies(m_Classifier, numClassifiers);
      m_ClassFilters = new MakeIndicator[numClassifiers];

      for (int i = 0; i < m_Classifiers.length; i++) {
	m_ClassFilters[i] = new MakeIndicator();
	m_ClassFilters[i].setAttributeIndex("" + (insts.classIndex() + 1));
	m_ClassFilters[i].setValueIndices(""+(i+2)+"-last");
	m_ClassFilters[i].setNumeric(false);
	m_ClassFilters[i].setInputFormat(insts);
	newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
	m_Classifiers[i].buildClassifier(newInsts);
      }
    }
  }
  
  /**
   * Returns the distribution for an instance.
   *
   * @param inst the instance to compute the distribution for
   * @return the class distribution for the given instance
   * @throws Exception if the distribution can't be computed successfully
   */
  public double [] distributionForInstance(Instance inst) throws Exception {
    
    if (m_Classifiers.length == 1) {
      return m_Classifiers[0].distributionForInstance(inst);
    }

    double [] probs = new double[inst.numClasses()];
    
    double [][] distributions = new double[m_ClassFilters.length][0];
    for(int i = 0; i < m_ClassFilters.length; i++) {
      m_ClassFilters[i].input(inst);
      m_ClassFilters[i].batchFinished();
      
      distributions[i] = m_Classifiers[i].
	distributionForInstance(m_ClassFilters[i].output());
      
    }

    for (int i = 0; i < inst.numClasses(); i++) {
      if (i == 0) {
	probs[i] = distributions[0][0];
      } else if (i == inst.numClasses() - 1) {
	probs[i] = distributions[i - 1][1];
      } else {
	probs[i] = distributions[i - 1][1] - distributions[i][1];
	if (!(probs[i] > 0)) {
	  System.err.println("Warning: estimated probability " + probs[i] +
	  		     ". Rounding to 0.");
	  probs[i] = 0;
	}
      }
    }

    if (Utils.gr(Utils.sum(probs), 0)) {
      Utils.normalize(probs);
      return probs;
    } else {
      return m_ZeroR.distributionForInstance(inst);
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector();

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      vec.addElement(enu.nextElement());
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.J48)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.trees.J48:
   * </pre>
   * 
   * <pre> -U
   *  Use unpruned tree.</pre>
   * 
   * <pre> -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)</pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)</pre>
   * 
   * <pre> -R
   *  Use reduced error pruning.</pre>
   * 
   * <pre> -N &lt;number of folds&gt;
   *  Set number of folds for reduced error
   *  pruning. One fold is used as pruning set.
   *  (default 3)</pre>
   * 
   * <pre> -B
   *  Use binary splits only.</pre>
   * 
   * <pre> -S
   *  Don't perform subtree raising.</pre>
   * 
   * <pre> -L
   *  Do not clean up after the tree has been built.</pre>
   * 
   * <pre> -A
   *  Laplace smoothing for predicted probabilities.</pre>
   * 
   * <pre> -Q &lt;seed&gt;
   *  Seed for random data shuffling (default 1).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    return super.getOptions();
  }
  
  /**
   * Prints the classifiers.
   * 
   * @return a string representation of this classifier
   */
  public String toString() {
    
    if (m_Classifiers == null) {
      return "OrdinalClassClassifier: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("OrdinalClassClassifier\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append("Classifier ").append(i + 1);
      if (m_Classifiers[i] != null) {
	 if ((m_ClassFilters != null) && (m_ClassFilters[i] != null)) {
          text.append(", using indicator values: ");
          text.append(m_ClassFilters[i].getValueRange());
        }
        text.append('\n');
        text.append(m_Classifiers[i].toString() + "\n");
      } else {
        text.append(" Skipped (no training examples)\n");
      }
    }

    return text.toString();
  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new OrdinalClassClassifier(), argv);
  }
}

