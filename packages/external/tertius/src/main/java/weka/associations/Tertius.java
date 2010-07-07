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
 *    Tertius.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations;

import weka.associations.tertius.AttributeValueLiteral;
import weka.associations.tertius.IndividualInstances;
import weka.associations.tertius.IndividualLiteral;
import weka.associations.tertius.Literal;
import weka.associations.tertius.Predicate;
import weka.associations.tertius.Rule;
import weka.associations.tertius.SimpleLinkedList;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Finds rules according to confirmation measure (Tertius-type algorithm).<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * P. A. Flach, N. Lachiche (1999). Confirmation-Guided Discovery of first-order rules with Tertius. Machine Learning. 42:61-95.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Flach1999,
 *    author = {P. A. Flach and N. Lachiche},
 *    journal = {Machine Learning},
 *    pages = {61-95},
 *    title = {Confirmation-Guided Discovery of first-order rules with Tertius},
 *    volume = {42},
 *    year = {1999}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -K &lt;number of values in result&gt;
 *  Set maximum number of confirmation  values in the result. (default: 10)</pre>
 * 
 * <pre> -F &lt;frequency threshold&gt;
 *  Set frequency threshold for pruning. (default: 0)</pre>
 * 
 * <pre> -C &lt;confirmation threshold&gt;
 *  Set confirmation threshold. (default: 0)</pre>
 * 
 * <pre> -N &lt;noise threshold&gt;
 *  Set noise threshold : maximum frequency of counter-examples.
 *  0 gives only satisfied rules. (default: 1)</pre>
 * 
 * <pre> -R
 *  Allow attributes to be repeated in a same rule.</pre>
 * 
 * <pre> -L &lt;number of literals&gt;
 *  Set maximum number of literals in a rule. (default: 4)</pre>
 * 
 * <pre> -G &lt;0=no negation | 1=body | 2=head | 3=body and head&gt;
 *  Set the negations in the rule. (default: 0)</pre>
 * 
 * <pre> -S
 *  Consider only classification rules.</pre>
 * 
 * <pre> -c &lt;class index&gt;
 *  Set index of class attribute. (default: last).</pre>
 * 
 * <pre> -H
 *  Consider only horn clauses.</pre>
 * 
 * <pre> -E
 *  Keep equivalent rules.</pre>
 * 
 * <pre> -M
 *  Keep same clauses.</pre>
 * 
 * <pre> -T
 *  Keep subsumed rules.</pre>
 * 
 * <pre> -I &lt;0=always match | 1=never match | 2=significant&gt;
 *  Set the way to handle missing values. (default: 0)</pre>
 * 
 * <pre> -O
 *  Use ROC analysis. </pre>
 * 
 * <pre> -p &lt;name of file&gt;
 *  Set the file containing the parts of the individual for individual-based learning.</pre>
 * 
 * <pre> -P &lt;0=no output | 1=on stdout | 2=in separate window&gt;
 *  Set output of current values. (default: 0)</pre>
 * 
 <!-- options-end -->
 *
 * @author <a href="mailto:adeltour@netcourrier.com">Amelie Deltour</a>
 * @version $Revision$
 */

public class Tertius 
  extends AbstractAssociator 
  implements OptionHandler, Runnable, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5556726848380738179L;
  
  /** The results. */
  private SimpleLinkedList m_results;

  /** Number of hypotheses considered. */
  private int m_hypotheses;

  /** Number of hypotheses explored. */
  private int m_explored;

  /** Time needed for the search. */
  private Date m_time;

  /** Field to output the current values. */ 
  private TextField m_valuesText;

  /** Instances used for the search. */
  private Instances m_instances;

  /** Predicates used in the rules. */
  private ArrayList m_predicates;

  /** Status of the search. */
  private int m_status;
  /** Status of the search: normal */
  private static final int NORMAL = 0;
  /** Status of the search: memory problem */
  private static final int MEMORY = 1;
  /** Status of the search: user interruption */
  private static final int STOP = 2;
  
  /* Pruning options. */

  /** Number of best confirmation values to search. */
  private int m_best;

  /** Frequency threshold for the body and the negation of the head. */
  private double m_frequencyThreshold;

  /** Confirmation threshold for the rules. */
  private double m_confirmationThreshold;

  /** Maximal number of counter-instances. */
  private double m_noiseThreshold;

  /* Search space & language bias options. */

  /** Repeat attributes ? */
  private boolean m_repeat;

  /** Number of literals in a rule. */
  private int m_numLiterals;

  /** Type of negation: none */
  private static final int NONE = 0;
  /** Type of negation: body */
  private static final int BODY = 1;
  /** Type of negation: head */
  private static final int HEAD = 2;
  /** Type of negation: all */
  private static final int ALL = 3;
  /** Types of negation. */
  private static final Tag [] TAGS_NEGATION = {
    new Tag(NONE, "None"),
    new Tag(BODY, "Body"),
    new Tag(HEAD, "Head"),
    new Tag(ALL, "Both")
      };

  /** Type of negation used in the rules. */
  private int m_negation;

  /** Classification bias. */
  private boolean m_classification;

  /** Index of class attribute. */
  private int m_classIndex;

  /** Horn clauses bias. */
  private boolean m_horn;

  /* Subsumption tests options. */

  /** Perform test on equivalent rules ? */
  private boolean m_equivalent;

  /** Perform test on same clauses ? */
  private boolean m_sameClause;
  
  /** Perform subsumption test ? */
  private boolean m_subsumption;

  /** Way of handling missing values: min counterinstances */
  public static final int EXPLICIT = 0;
  /** Way of handling missing values: max counterinstances */
  public static final int IMPLICIT = 1;
  /** Way of handling missing values: missing as a particular value */
  public static final int SIGNIFICANT = 2;
  /** Ways of handling missing values.  */
  private static final Tag [] TAGS_MISSING = {
    new Tag(EXPLICIT, "Matches all"),
    new Tag(IMPLICIT, "Matches none"),
    new Tag(SIGNIFICANT, "Significant")
      };

  /** Way of handling missing values in the search. */
  private int m_missing;

  /** Perform ROC analysis ? */
  private boolean m_roc;

  /** Name of the file containing the parts for individual-based learning. */
  private String m_partsString;
  
  /** Part instances for individual-based learning. */
  private Instances m_parts;

  /** Type of values output: No */ 
  private static final int NO = 0;
  /** Type of values output: stdout */ 
  private static final int OUT = 1;
  /** Type of values output: Window */ 
  private static final int WINDOW = 2;
  /** Types of values output. */ 
  private static final Tag [] TAGS_VALUES = {
    new Tag(NO, "No"),
    new Tag(OUT, "stdout"),
    new Tag(WINDOW, "Window")
      };

  /** Type of values output. */
  private int m_printValues;

  /**
   * Constructor that sets the options to the default values.
   */
  public Tertius() {

    resetOptions();
  }

  /**
   * Returns a string describing this associator.
   *
   * @return A description of the evaluator suitable for
   * displaying in the explorer/experimenter gui.
   */
  public String globalInfo() {
    return 
        "Finds rules according to confirmation measure (Tertius-type "
      + "algorithm).\n\n"
      + "For more information see:\n\n"
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
    
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "P. A. Flach and N. Lachiche");
    result.setValue(Field.YEAR, "1999");
    result.setValue(Field.TITLE, "Confirmation-Guided Discovery of first-order rules with Tertius");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "42");
    result.setValue(Field.PAGES, "61-95");
    
    return result;
  }

  /**
   * Resets the options to the default values.
   */
  public void resetOptions() {

    /* Pruning options. */
    m_best = 10;
    m_frequencyThreshold = 0;
    m_confirmationThreshold = 0;
    m_noiseThreshold = 1;

    /* Search space & language bias options. */
    m_repeat = false;
    m_numLiterals = 4;
    m_negation = NONE;
    m_classification = false;
    m_classIndex = 0;
    m_horn = false;

    /* Subsumption tests options. */
    m_equivalent = true;
    m_sameClause = true;
    m_subsumption = true;

    /* Missing values. */
    m_missing = EXPLICIT;

    /* ROC analysis. */
    m_roc = false;

    /* Individual-based learning. */
    m_partsString = "";
    m_parts = null;

    /* Values output. */
    m_printValues = NO;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return An enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(17);

    /* Pruning options. */
    newVector.addElement(new Option("\tSet maximum number of confirmation  "
				    + "values in the result. (default: 10)",
				    "K", 1, "-K <number of values in result>"));
    newVector.addElement(new Option("\tSet frequency threshold for pruning. "
				    + "(default: 0)",
				    "F", 1, "-F <frequency threshold>"));
    newVector.addElement(new Option("\tSet confirmation threshold. "
				    + "(default: 0)",
				    "C", 1, "-C <confirmation threshold>"));
    newVector.addElement(new Option("\tSet noise threshold : maximum frequency "
				    + "of counter-examples.\n\t0 gives only "
				    + "satisfied rules. (default: 1)",
				    "N", 1, "-N <noise threshold>"));

    /* Search space & language bias options. */
    newVector.addElement(new Option("\tAllow attributes to be repeated in a "
				    + "same rule.",
				    "R", 0, "-R"));
    newVector.addElement(new Option("\tSet maximum number of literals in a "
				    + "rule. (default: 4)",
				    "L", 1, "-L <number of literals>"));
    newVector.addElement(new Option("\tSet the negations in the rule. "
				    + "(default: 0)",
				    "G", 1, "-G <0=no negation | "
				    + "1=body | "
				    + "2=head | "
				    + "3=body and head>"));
    newVector.addElement(new Option("\tConsider only classification rules.",
				    "S", 0, "-S"));
    newVector.addElement(new Option("\tSet index of class attribute. "
				    + "(default: last).",
				    "c", 1, "-c <class index>"));
    newVector.addElement(new Option("\tConsider only horn clauses.",
				    "H", 0, "-H"));

    /* Subsumption tests options. */
    newVector.addElement(new Option("\tKeep equivalent rules.",
				    "E", 0, "-E"));
    newVector.addElement(new Option("\tKeep same clauses.",
				    "M", 0, "-M"));
    newVector.addElement(new Option("\tKeep subsumed rules.",
				    "T", 0, "-T"));

    /* Missing values options. */
    newVector.addElement(new Option("\tSet the way to handle missing values. " 
				    + "(default: 0)",
				    "I", 1, "-I <0=always match | "
				    + "1=never match | "
				    + "2=significant>"));

    /* ROC analysis. */
    newVector.addElement(new Option("\tUse ROC analysis. ",
				    "O", 0, "-O"));

    /* Individual-based learning. */
    newVector.addElement(new Option("\tSet the file containing the parts of "
				    + "the individual for individual-based "
				    + "learning.",
				    "p", 1, "-p <name of file>"));

    /* Values output. */
    newVector.addElement(new Option("\tSet output of current values. "
				    + "(default: 0)",
				    "P", 1, "-P <0=no output | "
				    + "1=on stdout | "
				    + "2=in separate window>"));
    
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -K &lt;number of values in result&gt;
   *  Set maximum number of confirmation  values in the result. (default: 10)</pre>
   * 
   * <pre> -F &lt;frequency threshold&gt;
   *  Set frequency threshold for pruning. (default: 0)</pre>
   * 
   * <pre> -C &lt;confirmation threshold&gt;
   *  Set confirmation threshold. (default: 0)</pre>
   * 
   * <pre> -N &lt;noise threshold&gt;
   *  Set noise threshold : maximum frequency of counter-examples.
   *  0 gives only satisfied rules. (default: 1)</pre>
   * 
   * <pre> -R
   *  Allow attributes to be repeated in a same rule.</pre>
   * 
   * <pre> -L &lt;number of literals&gt;
   *  Set maximum number of literals in a rule. (default: 4)</pre>
   * 
   * <pre> -G &lt;0=no negation | 1=body | 2=head | 3=body and head&gt;
   *  Set the negations in the rule. (default: 0)</pre>
   * 
   * <pre> -S
   *  Consider only classification rules.</pre>
   * 
   * <pre> -c &lt;class index&gt;
   *  Set index of class attribute. (default: last).</pre>
   * 
   * <pre> -H
   *  Consider only horn clauses.</pre>
   * 
   * <pre> -E
   *  Keep equivalent rules.</pre>
   * 
   * <pre> -M
   *  Keep same clauses.</pre>
   * 
   * <pre> -T
   *  Keep subsumed rules.</pre>
   * 
   * <pre> -I &lt;0=always match | 1=never match | 2=significant&gt;
   *  Set the way to handle missing values. (default: 0)</pre>
   * 
   * <pre> -O
   *  Use ROC analysis. </pre>
   * 
   * <pre> -p &lt;name of file&gt;
   *  Set the file containing the parts of the individual for individual-based learning.</pre>
   * 
   * <pre> -P &lt;0=no output | 1=on stdout | 2=in separate window&gt;
   *  Set output of current values. (default: 0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options The list of options as an array of strings.
   * @throws Exception if an option is not supported.
   */
  public void setOptions(String [] options) throws Exception {
    
    resetOptions();
    
    /* Pruning options. */
    String bestString = Utils.getOption('K', options);
    if (bestString.length() != 0) {
      try {
	m_best = Integer.parseInt(bestString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -K option: "
			    + e.getMessage() + ".");
      }
      if (m_best < 1) {
	throw new Exception("Number of confirmation values has to be "
			    + "greater than one!");
      }
    }
    String frequencyThresholdString = Utils.getOption('F', options);
    if (frequencyThresholdString.length() != 0) {
      try {	
	m_frequencyThreshold 
	  = (new Double(frequencyThresholdString)).doubleValue();
      } catch (Exception e) {
	throw new Exception("Invalid value for -F option: "
			    + e.getMessage() + ".");
      }
      if (m_frequencyThreshold < 0 || m_frequencyThreshold > 1) {
	throw new Exception("Frequency threshold has to be between "
			    + "zero and one!");
      }
    }
    String confirmationThresholdString = Utils.getOption('C', options);
    if (confirmationThresholdString.length() != 0) {
      try {
	m_confirmationThreshold 
	  = (new Double(confirmationThresholdString)).doubleValue();
      } catch (Exception e) {
	throw new Exception("Invalid value for -C option: "
			    + e.getMessage() + ".");
      }
      if (m_confirmationThreshold < 0 || m_confirmationThreshold > 1) {
	throw new Exception("Confirmation threshold has to be between "
			    + "zero and one!");
      }
      if (bestString.length() != 0) {
	throw new Exception("Specifying both a number of confirmation "
			    + "values and a confirmation threshold "
			    + "doesn't make sense!");
      }
      if (m_confirmationThreshold != 0) {
	m_best = 0;
      }
    }
    String noiseThresholdString = Utils.getOption('N', options);
    if (noiseThresholdString.length() != 0) {
      try {
	m_noiseThreshold = (new Double(noiseThresholdString)).doubleValue();
      } catch (Exception e) {
	throw new Exception("Invalid value for -N option: "
			    + e.getMessage() + ".");
      }
      if (m_noiseThreshold < 0 || m_noiseThreshold > 1) {
	throw new Exception("Noise threshold has to be between "
			    + "zero and one!");
      }
    }

    /* Search space and language bias options. */
    m_repeat = Utils.getFlag('R', options);
    String numLiteralsString = Utils.getOption('L', options);
    if (numLiteralsString.length() != 0) {
      try {
	m_numLiterals = Integer.parseInt(numLiteralsString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -L option: "
			    + e.getMessage() + ".");
      }
      if (m_numLiterals < 1) {
	throw new Exception("Number of literals has to be "
			    + "greater than one!");
      }
    }
    String negationString = Utils.getOption('G', options);
    if (negationString.length() != 0) {
      SelectedTag selected;
      int tag;
      try {
	tag = Integer.parseInt(negationString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -G option: "
			    + e.getMessage() + ".");
      }
      try {
	selected = new SelectedTag(tag, TAGS_NEGATION);
      } catch (Exception e) {
	throw new Exception("Value for -G option has to be "
			    + "between zero and three!");
      }
      setNegation(selected);
    }
    m_classification = Utils.getFlag('S', options);
    String classIndexString = Utils.getOption('c', options);
    if (classIndexString.length() != 0) {
      try {
	m_classIndex = Integer.parseInt(classIndexString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -c option: "
			    + e.getMessage() + ".");
      }
    }
    m_horn = Utils.getFlag('H', options);
    if (m_horn && (m_negation != NONE)) {
      throw new Exception("Considering horn clauses doesn't make sense "
			  + "if negation allowed!");
    }
    
    /* Subsumption tests options. */
    m_equivalent = !(Utils.getFlag('E', options));
    m_sameClause = !(Utils.getFlag('M', options));
    m_subsumption = !(Utils.getFlag('T', options));

    /* Missing values options. */
    String missingString = Utils.getOption('I', options);
    if (missingString.length() != 0) {
      SelectedTag selected;
      int tag;
      try {
	tag = Integer.parseInt(missingString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -I option: "
			    + e.getMessage() + ".");
      }
      try {
	selected = new SelectedTag(tag, TAGS_MISSING);
      } catch (Exception e) {
	throw new Exception("Value for -I option has to be "
			    + "between zero and two!");
      }
      setMissingValues(selected);
    }

    /* ROC analysis. */
    m_roc = Utils.getFlag('O', options);


    /* Individual-based learning. */
    m_partsString = Utils.getOption('p', options);
    if (m_partsString.length() != 0) {
      Reader reader;
      try {
	reader = new BufferedReader(new FileReader(m_partsString));
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + ".");
      }
      m_parts = new Instances(reader);	
    }

    /* Values output. */
    String printValuesString = Utils.getOption('P', options);
    if (printValuesString.length() != 0) {
      SelectedTag selected;
      int tag;
      try {
	tag = Integer.parseInt(printValuesString);
      } catch (Exception e) {
	throw new Exception("Invalid value for -P option: "
			    + e.getMessage() + ".");
      }
      try {
	selected = new SelectedTag(tag, TAGS_VALUES);
      } catch (Exception e) {
	throw new Exception("Value for -P option has to be "
			    + "between zero and two!");
      }
      setValuesOutput(selected);
    }
  }

  /**
   * Gets the current settings of the Tertius object.
   *
   * @return An array of strings suitable for passing to setOptions.
   */
  public String [] getOptions() {
    Vector    	result;

    result = new Vector();

    /* Pruning options. */
    if (m_best > 0) {
      result.add("-K");
      result.add("" + m_best);
    }
    
    result.add("-F");
    result.add("" + m_frequencyThreshold);
    
    if (m_confirmationThreshold > 0) {
      result.add("-C");
      result.add("" + m_confirmationThreshold);
    }
    
    result.add("-N");
    result.add("" + m_noiseThreshold);
    
    /* Search space and language bias options. */
    if (m_repeat)
      result.add("-R");
    
    result.add("-L");
    result.add("" + m_numLiterals);
    
    result.add("-G");
    result.add("" + m_negation);
    
    if (m_classification)
      result.add("-S");
      
    result.add("-c");
    result.add("" + m_classIndex);
    
    if (m_horn)
      result.add("-H");

    /* Subsumption tests options. */
    if (!m_equivalent)
      result.add("-E");
    
    if (!m_sameClause)
      result.add("-M");
    
    if (!m_subsumption)
      result.add("-T");

    /* Missing values options. */
    result.add("-I");
    result.add("" + m_missing);

    /* ROC analysis. */
    if (m_roc)
      result.add("-O");

    /* Individual-based learning. */
    if (m_partsString.length() > 0) {
      result.add("-p");
      result.add("" + m_partsString);
    }

    /* Values output. */
    result.add("-P");
    result.add("" + m_printValues);

    return (String[]) result.toArray(new String[result.size()]);	  
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String confirmationValuesTipText() {

    return "Number of best confirmation values to find.";
  }

  /**
   * Get the value of confirmationValues.
   *
   * @return Value of confirmationValues.
   */
  public int getConfirmationValues() {

    return m_best;
  }

  /**
   * Set the value of confirmationValues.
   *
   * @param v  Value to assign to confirmationValues.
   */
  public void setConfirmationValues(int v) {

    m_best = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String frequencyThresholdTipText() {
    
    return "Minimum proportion of instances satisfying head and body of rules";
  }

  /**
   * Get the value of frequencyThreshold.
   *
   * @return Value of frequencyThreshold.
   */
  public double getFrequencyThreshold() {
    
    return m_frequencyThreshold;
  }

  /**
   * Set the value of frequencyThreshold.
   *
   * @param v  Value to assign to frequencyThreshold.
   */
  public void setFrequencyThreshold(double v) {
    
    m_frequencyThreshold = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String confirmationThresholdTipText() {
    
    return "Minimum confirmation of the rules.";
  }

  /**
   * Get the value of confirmationThreshold.
   *
   * @return Value of confirmationThreshold.
   */
  public double getConfirmationThreshold() {
    
    return m_confirmationThreshold;
  }

  /**
   * Set the value of confirmationThreshold.
   *
   * @param v  Value to assign to confirmationThreshold.
   */
  public void setConfirmationThreshold(double v) {
    
    m_confirmationThreshold = v;
    if (v != 0) {
      m_best = 0;
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String noiseThresholdTipText() {
    
    return "Maximum proportion of counter-instances of rules. "
      + "If set to 0, only satisfied rules will be given.";
  }

  /**
   * Get the value of noiseThreshold.
   *
   * @return Value of noiseThreshold.
   */
  public double getNoiseThreshold() {
    
    return m_noiseThreshold;
  }

  /**
   * Set the value of noiseThreshold.
   *
   * @param v  Value to assign to noiseThreshold.
   */
  public void setNoiseThreshold(double v) {
    
    m_noiseThreshold = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String repeatLiteralsTipText() {

    return "Repeated attributes allowed.";
  }

  /**
   * Get the value of repeatLiterals.
   *
   * @return Value of repeatLiterals.
   */
  public boolean getRepeatLiterals() {
    
    return m_repeat;
  }

  /**
   * Set the value of repeatLiterals.
   *
   * @param v  Value to assign to repeatLiterals.
   */
  public void setRepeatLiterals(boolean v) {
    
    m_repeat = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String numberLiteralsTipText() {
    
    return "Maximum number of literals in a rule.";
  }

  /**
   * Get the value of numberLiterals.
   *
   * @return Value of numberLiterals.
   */
  public int getNumberLiterals() {

    return m_numLiterals;
  }

  /**
   * Set the value of numberLiterals.
   *
   * @param v  Value to assign to numberLiterals.
   */
  public void setNumberLiterals(int v) {
    
    m_numLiterals = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String negationTipText() {
    
    return "Set the type of negation allowed in the rule. "
      + "Negation can be allowed in the body, in the head, in both "
      + "or in none.";
  }

  /**
   * Get the value of negation.
   *
   * @return Value of negation.
   */
  public SelectedTag getNegation() {
    
    return new SelectedTag(m_negation, TAGS_NEGATION);
  }

  /**
   * Set the value of negation.
   *
   * @param v  Value to assign to negation.
   */
  public void setNegation(SelectedTag v) {
    
    if (v.getTags() == TAGS_NEGATION) {
      m_negation = v.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String classificationTipText() {
    
    return "Find only rules with the class in the head.";
  }

  /**
   * Get the value of classification.
   *
   * @return Value of classification.
   */
  public boolean getClassification() {

    return m_classification;
  }

  /**
   * Set the value of classification.
   *
   * @param v  Value to assign to classification.
   */
  public void setClassification(boolean v) {

    m_classification = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String classIndexTipText() {

    return "Index of the class attribute. If set to 0, the class will be the last attribute.";
  }

  /**
   * Get the value of classIndex.
   *
   * @return Value of classIndex.
   */
  public int getClassIndex() {

    return m_classIndex;
  }

  /**
   * Set the value of classIndex.
   *
   * @param v  Value to assign to classIndex.
   */
  public void setClassIndex(int v) {

    m_classIndex = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String hornClausesTipText() {
    
    return "Find rules with a single conclusion literal only.";
  }

  /**
   * Get the value of hornClauses.
   *
   * @return Value of hornClauses.
   */
  public boolean getHornClauses() {

    return m_horn;
  }

  /**
   * Set the value of hornClauses.
   *
   * @param v  Value to assign to hornClauses.
   */
  public void setHornClauses(boolean v) {

    m_horn = v;
  }
  
  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String equivalentTipText() {
    
    return "Keep equivalent rules. "
      + "A rule r2 is equivalent to a rule r1 if the body of r2 is the "
      + "negation of the head of r1, and the head of r2 is the "
      + "negation of the body of r1.";
  }

  /**
   * Get the value of equivalent.
   *
   * @return Value of equivalent.
   */
  public boolean disabled_getEquivalent() {
    
    return !m_equivalent;
  }

  /**
   * Set the value of equivalent.
   *
   * @param v  Value to assign to equivalent.
   */
  public void disabled_setEquivalent(boolean v) {
    
    m_equivalent = !v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String sameClauseTipText() {

    return "Keep rules corresponding to the same clauses. "
      + "If set to false, only the rule with the best confirmation "
      + "value and rules with a lower number of counter-instances "
      + "will be kept.";
  }

  /**
   * Get the value of sameClause.
   *
   * @return Value of sameClause.
   */
  public boolean disabled_getSameClause() {

    return !m_sameClause;
  }

  /**
   * Set the value of sameClause.
   *
   * @param v  Value to assign to sameClause.
   */
  public void disabled_setSameClause(boolean v) {

    m_sameClause = !v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String subsumptionTipText() {

    return "Keep subsumed rules. "
      + "If set to false, subsumed rules will only be kept if they "
      + "have a better confirmation or a lower number of counter-instances.";
  }

  /**
   * Get the value of subsumption.
   *
   * @return Value of subsumption.
   */
  public boolean disabled_getSubsumption() {

    return !m_subsumption;
  }

  /**
   * Set the value of subsumption.
   *
   * @param v  Value to assign to subsumption.
   */
  public void disabled_setSubsumption(boolean v) {

    m_subsumption = !v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String missingValuesTipText() {
    
    return "Set the way to handle missing values. "
      + "Missing values can be set to match any value, or never match values "
      + "or to be significant and possibly appear in rules.";
  }

  /**
   * Get the value of missingValues.
   *
   * @return Value of missingValues.
   */
  public SelectedTag getMissingValues() {
    
    return new SelectedTag(m_missing, TAGS_MISSING);
  }

  /**
   * Set the value of missingValues.
   *
   * @param v  Value to assign to missingValues.
   */
  public void setMissingValues(SelectedTag v) {
    
    if (v.getTags() == TAGS_MISSING) {
      m_missing = v.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String rocAnalysisTipText() {
    
    return "Return TP-rate and FP-rate for each rule found.";
  }

  /**
   * Get the value of rocAnalysis.
   *
   * @return Value of rocAnalysis.
   */
  public boolean getRocAnalysis() {

    return m_roc;
  }

  /**
   * Set the value of rocAnalysis.
   *
   * @param v  Value to assign to rocAnalysis.
   */
  public void setRocAnalysis(boolean v) {

    m_roc = v;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String partFileTipText() {
    
    return "Set file containing the parts of the individual "
      + "for individual-based learning.";
  }

  /**
   * Get the value of partFile.
   *
   * @return Value of partFile.
   */
  public File disabled_getPartFile() {

    return new File(m_partsString);
  }

  /**
   * Set the value of partFile.
   *
   * @param v  Value to assign to partFile.
   * @throws Exception if file cannot be opened
   */
  public void disabled_setPartFile(File v) throws Exception {

    m_partsString = v.getAbsolutePath();
    if (m_partsString.length() != 0) {
      Reader reader;
      try {
	reader = new BufferedReader(new FileReader(m_partsString));
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + ".");
      }
      m_parts = new Instances(reader);	
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return Tip text for this property suitable for
   * displaying in the explorer/experimenter GUI.
   */
  public String valuesOutputTipText() {
    
    return "Give visual feedback during the search. "
      + "The current best and worst values can be output either to stdout or to a separate window.";
  }

  /**
   * Get the value of valuesOutput.
   *
   * @return Value of valuesOutput.
   */
  public SelectedTag getValuesOutput() {
    
    return new SelectedTag(m_printValues, TAGS_VALUES);
  }

  /**
   * Set the value of valuesOutput.
   *
   * @param v  Value to assign to valuesOutput.
   */
  public void setValuesOutput(SelectedTag v) {
    
    if (v.getTags() == TAGS_VALUES) {
      m_printValues = v.getSelectedTag().getID();
    }
  }

  /**
   * Build the predicate corresponding to an attribute.
   *
   * @param instances The instances this predicates comes from.
   * @param attr The attribute this predicate corresponds to.
   * @param isClass Saying if the attribute is the class attribute.
   * @return The corresponding Predicate.
   * @throws Exception if the predicate could not be build 
   * (the attribute is numeric).
   */
  private Predicate buildPredicate(Instances instances,
				   Attribute attr, boolean isClass) 
    throws Exception {

    Predicate predicate; /* The result. */
    Literal lit;
    Literal negation;
    boolean missingValues; /* Missing values for this attribute ? */
    boolean individual = (m_parts != null); /* Individual-based learning ? */
    int type = (instances == m_parts)
      ? IndividualLiteral.PART_PROPERTY
      : IndividualLiteral.INDIVIDUAL_PROPERTY; /* Type of property. */

    if (attr.isNumeric()) {
      throw new Exception("Can't handle numeric attributes!");
    }
	
    missingValues = instances.attributeStats(attr.index()).missingCount > 0;

    /* Build predicate. */
    if (individual) {
      predicate = new Predicate(instances.relationName() + "." + attr.name(), 
				attr.index(), isClass);
    } else {
      predicate = new Predicate(attr.name(), attr.index(), isClass);
    }
	
    if (attr.numValues() == 2
	&& (!missingValues || m_missing == EXPLICIT)) {
      /* Case of two values.
       * If there are missing values, this case is treated like other cases.
       */
      if (individual) {
	lit = new IndividualLiteral(predicate, attr.value(0), 0, 
				    Literal.POS, m_missing, type);
	negation = new IndividualLiteral(predicate, attr.value(1), 1, 
					 Literal.POS, m_missing, type);
      } else {
	lit = new AttributeValueLiteral(predicate, attr.value(0), 0, 
					Literal.POS, m_missing);
	negation = new AttributeValueLiteral(predicate, attr.value(1), 1, 
					     Literal.POS, m_missing);
      }
      lit.setNegation(negation);
      negation.setNegation(lit);
      predicate.addLiteral(lit);      
    } else {
      /* Case of several values. */
      for (int i = 0; i < attr.numValues(); i++) {
	if (individual) {
	  lit = new IndividualLiteral(predicate, attr.value(i), i,
				      Literal.POS, m_missing, type);
	} else {
	  lit = new AttributeValueLiteral(predicate, attr.value(i), i, 
					  Literal.POS, m_missing);
	}
	if (m_negation != NONE) {
	  if (individual) {
	    negation = new IndividualLiteral(predicate, attr.value(i), i, 
					     Literal.NEG, m_missing, type);
	  } else {
	    negation = new AttributeValueLiteral(predicate, attr.value(i), i, 
						 Literal.NEG, m_missing);
	  }
	  lit.setNegation(negation);
	  negation.setNegation(lit);
	}
	predicate.addLiteral(lit);
      }

      /* One more value if missing is significant. */
      if (missingValues && m_missing == SIGNIFICANT) {
	if (individual) {
	  lit = new IndividualLiteral(predicate, "?", -1, 
				      Literal.POS, m_missing, type);
	} else {
	  lit = new AttributeValueLiteral(predicate, "?", -1, 
					  Literal.POS, m_missing);
	}
	if (m_negation != NONE) {
	  if (individual) {
	    negation = new IndividualLiteral(predicate, "?", -1, 
					     Literal.NEG, m_missing, type);
	  } else {
	    negation = new AttributeValueLiteral(predicate, "?", -1, 
						 Literal.NEG, m_missing);
	  }
	  lit.setNegation(negation);
	  negation.setNegation(lit);
	}
	predicate.addLiteral(lit);
      }
    }
    return predicate;
  }
   
  /**
   * Build the predicates to use in the rules.
   *
   * @return the predicates
   * @throws Exception If the predicates could not be built 
   * (numeric attribute).
   */
  private ArrayList buildPredicates() throws Exception {

    ArrayList predicates = new ArrayList(); /* The result. */
    Predicate predicate;
    Attribute attr;
    Enumeration attributes = m_instances.enumerateAttributes();
    boolean individual = (m_parts != null); /* Individual-based learning ? */

    /* Attributes. */
    while (attributes.hasMoreElements()) {
      attr = (Attribute) attributes.nextElement();
      /* Identifiers do not appear in rules in individual-based learning. */
      if (!(individual && attr.name().equals("id"))) {
	predicate = buildPredicate(m_instances, attr, false);
	predicates.add(predicate);
      }
    }
    /* Class attribute. */
    attr = m_instances.classAttribute();
    /* Identifiers do not appear in rules. */
    if (!(individual && attr.name().equals("id"))) {
      predicate = buildPredicate(m_instances, attr, true);
      predicates.add(predicate);
    }

    /* Attributes of the parts in individual-based learning. */
    if (individual) {
      attributes = m_parts.enumerateAttributes();
      while (attributes.hasMoreElements()) {
	attr = (Attribute) attributes.nextElement();
	/* Identifiers do not appear in rules. */
	if (!attr.name().equals("id")) {
	  predicate = buildPredicate(m_parts, attr, false);
	  predicates.add(predicate);
	}
      }
    }
	
    return predicates;
  }

  /**
   * Count the number of distinct confirmation values in the results.
   *
   * @return Number of confirmation values in the results.
   */
  private int numValuesInResult() {

    int result = 0;
    SimpleLinkedList.LinkedListIterator iter = m_results.iterator();
    Rule current;
    Rule next;
    if (!iter.hasNext()) {
      return result;
    } else {
      current = (Rule) iter.next();
      while (iter.hasNext()) {
	next = (Rule) iter.next();
	if (current.getConfirmation() > next.getConfirmation()) {
	  result++;
	}
	current = next;
      }
      return result + 1;
    }
  }

  /**
   * Test if it is worth refining a rule.
   *
   * @param rule The rule to consider.
   * @return True if the rule can be refined, false otherwise.
   */
  private boolean canRefine(Rule rule) {
    if (rule.isEmpty()) {
      return true;
    }
    if (m_best != 0) {
      if (numValuesInResult() < m_best) {
	return true;
      }
      Rule worstResult = (Rule) m_results.getLast();
      if (rule.getOptimistic() >= worstResult.getConfirmation()) {
	return true;
      }
      return false;
    } else {
      return true;
    }
  }

  /**
   * Test if it is worth calculating the optimistic estimate of a rule.
   *
   * @param rule The rule to consider.
   * @return True if the optimistic estimate can be calculated, false otherwise.
   */
  private boolean canCalculateOptimistic(Rule rule) {
    if (rule.hasTrueBody() || rule.hasFalseHead()) {
      return false;
    }
    if (!rule.overFrequencyThreshold(m_frequencyThreshold)) {
      return false;
    }
    return true;
  }

  /**
   * Test if a rule can be explored (if it is interesting for the results 
   * or for refining).
   *
   * @param rule The rule to consider.
   * @return True if the rule can be explored, false otherwise.
   */
  private boolean canExplore(Rule rule) {
    if (rule.getOptimistic() < m_confirmationThreshold) {
      return false;
    }
    if (m_best != 0) {
      if (numValuesInResult() < m_best) {
	return true;
      }	
      Rule worstResult = (Rule) m_results.getLast();
      if (rule.getOptimistic() >= worstResult.getConfirmation()) {
	return true;
      }
      return false;      
    } else {
      return true;
    }
  }    

  /**
   * Test if a rule can be stored in the agenda.
   *
   * @param rule The rule to consider.
   * @return True if the rule can be stored, false otherwise.
   */
  private boolean canStoreInNodes(Rule rule) {
    if (rule.getObservedNumber() == 0) {
      return false;
    }
    return true;
  }

  /**
   * Test if it is worth calculating the confirmation of a rule.
   *
   * @param rule The rule to consider.
   * @return True if the confirmation can be calculated, false otherwise.
   */
  private boolean canCalculateConfirmation(Rule rule) {
    if (rule.getObservedFrequency() > m_noiseThreshold) {
      return false;
    }
    return true;
  }

  /**
   * Test if a rule can be added to the results.
   *
   * @param rule The rule to consider.
   * @return True if the rule can be stored, false otherwise.
   */
  private boolean canStoreInResults(Rule rule) {
    if (rule.getConfirmation() < m_confirmationThreshold) {
      return false;
    }
    if (m_best != 0) {
      if (numValuesInResult() < m_best) {
	return true;
      }
      Rule worstResult = (Rule) m_results.getLast();
      if (rule.getConfirmation() >= worstResult.getConfirmation()) {
	return true;
      }
      return false;    
    } else {
      return true;
    }
  }

  /**
   * Add a rule in the appropriate place in the list of the results, 
   * according to the confirmation and 
   * number of counter-instances of the rule. <p>
   * Subsumption tests are performed and the rule may not be added. <p>
   * Previous results may also be removed because of sumption.
   */
  private void addResult(Rule rule) {

    Rule current;
    boolean added = false;

    /* Iterate the list until we find the right place. */
    SimpleLinkedList.LinkedListIterator iter = m_results.iterator();
    while (iter.hasNext()) {
      current = (Rule) iter.next();
      if (Rule.confirmationThenObservedComparator.compare(current, rule) > 0) {
	iter.addBefore(rule);
	added = true;
	break;
      }
      /* Subsumption tests to see if the rule can be added. */
      if ((m_subsumption || m_sameClause || m_equivalent)
	  && current.subsumes(rule)) {
	if (current.numLiterals() == rule.numLiterals()) {
	  if (current.equivalentTo(rule)) {
	    /* Equivalent rules. */
	    if (m_equivalent) {
	      return;
	    }
	  } else {
	    /* Same clauses. */
	    if (m_sameClause
		&& Rule.confirmationComparator.compare(current, rule) < 0) {
	      return;
	    }
	  }
	} else {
	  /* Subsumption. */
	  if (m_subsumption
	      && Rule.observedComparator.compare(current, rule) <= 0) {	
	    return;
	  }
	}
      }
    }

    if (added == false) {
      /* The rule must be added in the end of the results. */
      m_results.add(rule);
    }

    /* Iterate the results with a lower confirmation 
     *  to see if some of them must be removed. */
    SimpleLinkedList.LinkedListInverseIterator inverse 
      = m_results.inverseIterator();
    while (inverse.hasPrevious()) {
      current = (Rule) inverse.previous();
      if (Rule.confirmationThenObservedComparator.compare(current, rule) < 0) {
	break;
      }
      if (current != rule && rule.subsumes(current)) {
	if (current.numLiterals() == rule.numLiterals()) {
	  if (!current.equivalentTo(rule)) {
	    /* Same clauses. */
	    if (m_sameClause
		&& Rule.confirmationComparator.compare(current, rule) > 0) {
	      inverse.remove();
	    }
	  }
	} else {
	  /* Subsumption. */
	  if (m_subsumption
	      && Rule.observedComparator.compare(rule, current) <= 0) {	
	    inverse.remove();
	  }
	}	
      }
    }

    /* Remove the rules with the worst confirmation value 
     * if there are too many results. */
    if (m_best != 0 && numValuesInResult() > m_best) {
      Rule worstRule = (Rule) m_results.getLast();
      inverse = m_results.inverseIterator();
      while (inverse.hasPrevious()) {
	current = (Rule) inverse.previous();
	if (Rule.confirmationComparator.compare(current, worstRule) < 0) {
	  break;
	}
	inverse.remove();
      }
    }

    /* Print the new current values. */
    printValues();
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NO_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
  
  /**
   * Method that launches the search to find the rules with the highest 
   * confirmation.
   *
   * @param instances The instances to be used for generating the rules.
   * @throws Exception if rules can't be built successfully.
   */
  public void buildAssociations(Instances instances) throws Exception {

    Frame valuesFrame = null; /* Frame to display the current values. */

    /* Initialization of the search. */
    if (m_parts == null) {
      m_instances = new Instances(instances);
    } else {
      m_instances = new IndividualInstances(new Instances(instances), m_parts);
    }    
    m_results = new SimpleLinkedList();
    m_hypotheses = 0;
    m_explored = 0;
    m_status = NORMAL;

    if (m_classIndex == -1)
      m_instances.setClassIndex(m_instances.numAttributes()-1);     
    else if (m_classIndex < m_instances.numAttributes() && m_classIndex >= 0)
      m_instances.setClassIndex(m_classIndex);
    else
      throw new Exception("Invalid class index.");
    
    // can associator handle the data?
    getCapabilities().testWithFail(m_instances);
    
    /* Initialization of the window for current values. */
    if (m_printValues == WINDOW) {
      m_valuesText = new TextField(37);
      m_valuesText.setEditable(false);
      m_valuesText.setFont(new Font("Monospaced", Font.PLAIN, 12));    
      Label valuesLabel = new Label("Best and worst current values:");
      Button stop = new Button("Stop search");
      stop.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    /* Signal the interruption to the search. */
	    m_status = STOP;
	  }
	});
      valuesFrame = new Frame("Tertius status");
      valuesFrame.setResizable(false);
      valuesFrame.add(m_valuesText, BorderLayout.CENTER);
      valuesFrame.add(stop, BorderLayout.SOUTH);
      valuesFrame.add(valuesLabel, BorderLayout.NORTH);
      valuesFrame.pack();
      valuesFrame.setVisible(true);
    } else if (m_printValues == OUT) {
      System.out.println("Best and worst current values:");
    }
      
    Date start = new Date();

    /* Build the predicates and launch the search. */
    m_predicates = buildPredicates();
    beginSearch();    

    Date end = new Date();

    if (m_printValues == WINDOW) {
      valuesFrame.dispose();
    }

    m_time = new Date(end.getTime() - start.getTime());
  }

  /**
   * Run the search.
   */
  public void run() {
    try {
      search();
    } catch (OutOfMemoryError e) {
      /* Garbage collect what can be collected to be able to continue. */
      System.gc();
      m_status = MEMORY;
    }
    endSearch();
  }

  /**
   * Begin the search by starting a new thread.
   */
  private synchronized void beginSearch() throws Exception {
    /* This method must be synchronized to be able to 
     * call the wait() method. */
    Thread search = new Thread(this);
    search.start();
    try {
      /* Wait for the end of the thread. */
      wait();
    } catch (InterruptedException e) {
      /* Signal the interruption to the search. */
      m_status = STOP;
    }
  }

  /**
   * End the search by notifying to the waiting thread that it is finished.
   */
  private synchronized void endSearch() {
    /* This method must be synchronized to be able to
     * call the notify() method. */
    notify();
  }

  /**
   * Search in the space of hypotheses the rules that have the highest 
   * confirmation.
   * The search starts with the empty rule, other rules are generated by 
   * refinement.
   */
  public void search() {

    SimpleLinkedList nodes = new SimpleLinkedList(); /* The agenda. */
    Rule currentNode;
    SimpleLinkedList children;
    SimpleLinkedList.LinkedListIterator iter;
    Rule child;
    boolean negBody = (m_negation == BODY || m_negation == ALL);
    boolean negHead = (m_negation == HEAD || m_negation == ALL);

    /* Start with the empty rule. */
      nodes.add(new Rule(m_repeat, m_numLiterals, negBody, negHead,
			 m_classification, m_horn));
    
    /* Print the current values. */
    printValues();

    /* Explore the rules in the agenda. */
    while (m_status != STOP && !nodes.isEmpty()) {
      currentNode = (Rule) nodes.removeFirst();
      if (canRefine(currentNode)) {
	children = currentNode.refine(m_predicates);
	iter = children.iterator();
	/* Calculate the optimistic estimate of the children and 
	 * consider them for adding to the agenda and to the results. */
	while (iter.hasNext()) {
	  m_hypotheses++;
	  child = (Rule) iter.next();
	  child.upDate(m_instances);
	  if (canCalculateOptimistic(child)) {
	    child.calculateOptimistic();
	    if (canExplore(child)) {
	      m_explored++;
	      if (canStoreInNodes(child)) {
	      } else {
		iter.remove();
	      }
	      if (canCalculateConfirmation(child)) {
		child.calculateConfirmation();
		if (canStoreInResults(child)) {
		  addResult(child);
		}	  
	      }
	    } else {
	      iter.remove();
	    }
	  } else {
	    iter.remove();
	  }
	}
	/* Since the agenda is already sorted it is more efficient
	 * to sort the children only and then merge. */
	children.sort(Rule.optimisticThenObservedComparator);
	nodes.merge(children, Rule.optimisticThenObservedComparator);
      } else {
	/* The agenda being sorted, it is not worth considering the following 
	 * nodes. */
	break;
      }
    }
  }

  /**
   * returns the results
   * 
   * @return		the results
   */
  public SimpleLinkedList getResults() {
    return m_results;
  }

  /**
   * Print the current best and worst values. 
   */
  private void printValues() {

    if (m_printValues == NO) {
      return;
    } else {
      if (m_results.isEmpty()) {
	if (m_printValues == OUT) {
	  System.out.print("0.000000 0.000000 - 0.000000 0.000000");
	} else { //m_printValues == WINDOW
	  m_valuesText.setText("0.000000 0.000000 - 0.000000 0.000000");
	}
      } else {
	Rule best = (Rule) m_results.getFirst();
	Rule worst = (Rule) m_results.getLast();
	String values = best.valuesToString() + " - " + worst.valuesToString();
	if (m_printValues == OUT) {
	  System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b"
			   + "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
	  System.out.print(values);
	} else { //m_printValues == WINDOW
	  m_valuesText.setText(values);
	}
      }
    }
  }

  /**
   * Outputs the best rules found with their confirmation value and number 
   * of counter-instances.
   * Also gives the number of hypotheses considered and explored, and the 
   * time needed. 
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    SimpleLinkedList.LinkedListIterator iter = m_results.iterator();
    int size = m_results.size();
    int i = 0;

    text.append("\nTertius\n=======\n\n");

    while (iter.hasNext()) {
      Rule current = (Rule) iter.next();
      text.append(Utils.doubleToString((double) i + 1,
				       (int) (Math.log(size) 
					      / Math.log(10) + 1),
				       0)
		  + ". ");
      text.append("/* ");
      if (m_roc) {
	text.append(current.rocToString());
      } else {
	text.append(current.valuesToString());
      }
      text.append(" */ ");
      text.append(current.toString());
      text.append("\n");
      i++;
    }
 
    text.append("\nNumber of hypotheses considered: " + m_hypotheses);
    text.append("\nNumber of hypotheses explored: " + m_explored);

    if (m_status == MEMORY) {
      text.append("\n\nNot enough memory to continue the search");
    } else if (m_status == STOP) {
      text.append("\n\nSearch interrupted");
    }

    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method.
   * 
   * @param args the commandline parameters
   */
  public static void main(String [] args) {
    runAssociator(new Tertius(), args);
  }
}
