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
 *    Copyright (C) 2001 Mark Hall
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.rules.ZeroR;
import java.io.Serializable;
import weka.core.*;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.Filter;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Meta classifier for transforming an ordinal class problem to a series
 * of binary class problems. For more information see: <p>
 *
 * Frank, E. and Hall, M. (in press). <i>A simple approach to ordinal 
 * prediction.</i> 12th European Conference on Machine Learning. 
 * Freiburg, Germany. <p>
 *
 * Valid options are: <p>
 *
 * -W classname <br>
 * Specify the full class name of a learner as the basis for 
 * the ordinalclassclassifier (required).<p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision 1.0 $
 * @see DistributionClassifier
 * @see OptionHandler
 */
public class OrdinalClassClassifier extends DistributionClassifier 
implements OptionHandler {

  /** The classifiers. (One for each class.) */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicator[] m_ClassFilters;

  /** The class name of the base classifier. */
  private DistributionClassifier m_Classifier = new weka.classifiers.rules.ZeroR();

  /** Internal copy of the class attribute for output purposes */
  private Attribute m_ClassAttribute;

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return " Meta classifier that allows standard classification algorithms "
      +"to be applied to ordinal class problems.  For more information see: "
      +"Frank, E. and Hall, M. (in press). A simple approach to ordinal "
      +"prediction. 12th European Conference on Machine Learning. Freiburg, "
      +"Germany.";
  }

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    if (!insts.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("OrdinalClassClassifier: class should " +
					      "be declared nominal!");
    }
    
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
	m_ClassFilters[i].setAttributeIndex(insts.classIndex());
	m_ClassFilters[i].setValueIndices(""+(i+2)+"-last");
	m_ClassFilters[i].setNumeric(false);
	m_ClassFilters[i].setInputFormat(insts);
	newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
	m_Classifiers[i].buildClassifier(newInsts);
      }
    }
    m_ClassAttribute = insts.classAttribute();
  }
  
  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double [] distributionForInstance(Instance inst) throws Exception {
    
    if (m_Classifiers.length == 1) {
      return ((DistributionClassifier)m_Classifiers[0])
        .distributionForInstance(inst);
    }

    double [] probs = new double[inst.numClasses()];
    
    double [][] distributions = new double[m_ClassFilters.length][0];
    for(int i = 0; i < m_ClassFilters.length; i++) {
      m_ClassFilters[i].input(inst);
      m_ClassFilters[i].batchFinished();
      
      distributions[i] = ((DistributionClassifier)m_Classifiers[i])
	.distributionForInstance(m_ClassFilters[i].output());
      
    }

    for (int i = 0; i < inst.numClasses(); i++) {
      if (i == 0) {
	probs[i] = distributions[0][0];
      } else if (i == inst.numClasses() - 1) {
	probs[i] = distributions[i - 1][1];
      } else {
	probs[i] = distributions[i - 1][1] - distributions[i][1];
	if (!(probs[i] > 0)) {
	  //System.err.println("Warning: estimated probability " + probs[i] +
	  //		     ". Rounding to 0.");
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

    Vector vec = new Vector(1);
    Object c;
    
    vec.addElement(new Option(
       "\tSets the base classifier.",
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
   * the ordinalclassclassifier (required).<p>
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
    setDistributionClassifier((DistributionClassifier)
                              Classifier.forName(classifierName,
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String distributionClassifierTipText() {
    return "Sets the DistributionClassifier used as the basis for "
      + "the multi-class classifier.";
  }

  /**
   * Set the base classifier. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setDistributionClassifier(DistributionClassifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public DistributionClassifier getDistributionClassifier() {

    return m_Classifier;
  }

  /**
   * Prints the classifiers.
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

    DistributionClassifier scheme;

    try {
      scheme = new OrdinalClassClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
