/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    RankSearch.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> RankSearch : <br/>
 * <br/>
 * Uses an attribute/subset evaluator to rank all attributes. If a subset
 * evaluator is specified, then a forward selection search is used to generate a
 * ranked list. From the ranked list of attributes, subsets of increasing size
 * are evaluated, ie. The best attribute, the best attribute plus the next best
 * attribute, etc.... The best attribute set is reported. RankSearch is linear
 * in the number of attributes if a simple attribute evaluator is used such as
 * GainRatioAttributeEval. For more information see:<br/>
 * <br/>
 * Mark Hall, Geoffrey Holmes (2003). Benchmarking attribute selection
 * techniques for discrete class data mining. IEEE Transactions on Knowledge and
 * Data Engineering. 15(6):1437-1447.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -A &lt;attribute evaluator&gt;
 *  class name of attribute evaluator to use for ranking. Place any
 *  evaluator options LAST on the command line following a "--".
 *  eg.:
 *   -A weka.attributeSelection.GainRatioAttributeEval ... -- -M
 *  (default: weka.attributeSelection.GainRatioAttributeEval)
 * </pre>
 * 
 * <pre>
 * -S &lt;step size&gt;
 *  number of attributes to be added from the
 *  ranking in each iteration (default = 1).
 * </pre>
 * 
 * <pre>
 * -R &lt;start point&gt;
 *  point in the ranking to start evaluating from. 
 *  (default = 0, ie. the head of the ranking).
 * </pre>
 * 
 * <pre>
 * Options specific to evaluator weka.attributeSelection.GainRatioAttributeEval:
 * </pre>
 * 
 * <pre>
 * -M
 *  treat missing values as a separate value.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RankSearch extends ASSearch implements OptionHandler,
  TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -7992268736874353755L;

  /** does the data have a class */
  // private boolean m_hasClass; NOT USED

  /** holds the class index */
  // private int m_classIndex; NOT USED

  /** number of attributes in the data */
  private int m_numAttribs;

  /** the best subset found */
  // private BitSet m_best_group; NOT USED

  /** the attribute evaluator to use for generating the ranking */
  private ASEvaluation m_ASEval;

  /** the subset evaluator with which to evaluate the ranking */
  private ASEvaluation m_SubsetEval;

  /** the training instances */
  private Instances m_Instances;

  /** the merit of the best subset found */
  private double m_bestMerit;

  /** will hold the attribute ranking */
  private int[] m_Ranking;

  /**
   * Threshold on improvement in merit by which to accept additional attributes
   * from the ranked list
   */
  protected double m_improvementThreshold = 0;

  /**
   * Don't include non-improving attributes when evaluating more attributes from
   * the ranked list
   */
  protected boolean m_excludeNonImproving = false;

  /** add this many attributes in each iteration from the ranking */
  protected int m_add = 1;

  /** start from this point in the ranking */
  protected int m_startPoint = 0;

  /** Output debugging (progress) info to the console */
  protected boolean m_debug;

  /**
   * Terminate evaluation of the ranking after this many non improving
   * additions. 0 means don't terminate.
   */
  protected int m_nonImprovingAdditions = 0;

  /**
   * Returns a string describing this search method
   * 
   * @return a description of the search method suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "RankSearch : \n\n"
      + "Uses an attribute/subset evaluator to rank all attributes. "
      + "If a subset evaluator is specified, then a forward selection "
      + "search is used to generate a ranked list. From the ranked "
      + "list of attributes, subsets of increasing size are evaluated, ie. "
      + "The best attribute, the best attribute plus the next best attribute, "
      + "etc.... The best attribute set is reported. RankSearch is linear in "
      + "the number of attributes if a simple attribute evaluator is used "
      + "such as GainRatioAttributeEval. For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Mark Hall and Geoffrey Holmes");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.TITLE,
      "Benchmarking attribute selection techniques for "
        + "discrete class data mining");
    result.setValue(Field.JOURNAL,
      "IEEE Transactions on Knowledge and Data Engineering");
    result.setValue(Field.VOLUME, "15");
    result.setValue(Field.NUMBER, "6");
    result.setValue(Field.PAGES, "1437-1447");
    result.setValue(Field.PUBLISHER, "IEEE Computer Society");

    return result;
  }

  /**
   * Constructor
   */
  public RankSearch() {
    resetOptions();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeEvaluatorTipText() {
    return "Attribute evaluator to use for generating a ranking.";
  }

  /**
   * Set the attribute evaluator to use for generating the ranking.
   * 
   * @param newEvaluator the attribute evaluator to use.
   */
  public void setAttributeEvaluator(ASEvaluation newEvaluator) {
    m_ASEval = newEvaluator;
  }

  /**
   * Get the attribute evaluator used to generate the ranking.
   * 
   * @return the evaluator used to generate the ranking.
   */
  public ASEvaluation getAttributeEvaluator() {
    return m_ASEval;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stepSizeTipText() {
    return "Add this many attributes from the ranking in each iteration.";
  }

  /**
   * Set the number of attributes to add from the rankining in each iteration
   * 
   * @param ss the number of attribes to add.
   */
  public void setStepSize(int ss) {
    if (ss > 0) {
      m_add = ss;
    }
  }

  /**
   * Get the number of attributes to add from the rankining in each iteration
   * 
   * @return the number of attributes to add.
   */
  public int getStepSize() {
    return m_add;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String startPointTipText() {
    return "Start evaluating from this point in the ranking.";
  }

  /**
   * Set the point at which to start evaluating the ranking
   * 
   * @param sp the position in the ranking to start at
   */
  public void setStartPoint(int sp) {
    if (sp >= 0) {
      m_startPoint = sp;
    }
  }

  /**
   * Get the point at which to start evaluating the ranking
   * 
   * @return the position in the ranking to start at
   */
  public int getStartPoint() {
    return m_startPoint;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debuggingOutputTipText() {
    return "Output debugging information to the console";
  }

  /**
   * Set whether to output debugging info to the console
   * 
   * @param d true if dubugging info is to be output
   */
  public void setDebuggingOutput(boolean d) {
    m_debug = d;
  }

  /**
   * Get whether to output debugging info to the console
   * 
   * @return true if dubugging info is to be output
   */
  public boolean getDebuggingOutput() {
    return m_debug;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String improvementThresholdTipText() {
    return "Threshold on improvement in merit by which to "
      + "accept additional attributes from the ranked list";
  }

  /**
   * Set merit improvement threshold
   * 
   * @param t improvement threshold
   */
  public void setImprovementThreshold(double t) {
    m_improvementThreshold = t;
  }

  /**
   * Get merit improvement threshold
   * 
   * @return improvement threshold
   */
  public double getImprovementThreshold() {
    return m_improvementThreshold;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String excludeNonImprovingAttributesTipText() {
    return "As more attributes are considered from the ranked list, "
      + "don't include any prior ones that did not improve " + "merit";
  }

  /**
   * Set whether or not to add prior non-improving attributes when considering
   * more attributes from the ranked list
   * 
   * @param b true if prior non-improving attributes should be omitted
   */
  public void setExcludeNonImprovingAttributes(boolean b) {
    m_excludeNonImproving = b;
  }

  /**
   * Get whether or not to add prior non-improving attributes when considering
   * more attributes from the ranked list
   * 
   * @return true if prior non-improving attributes should be omitted
   */
  public boolean getExcludeNonImprovingAttributes() {
    return m_excludeNonImproving;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nonImprovingAdditionsTipText() {
    return "Terminate the evaluation of the ranking after this many "
      + "non-improving additions to the best subset seen (0 = don't "
      + "terminate early)";
  }

  /**
   * Set the number of consecutive non-improving additions to tolerate before
   * terminating the search
   * 
   * @param t the number of non-improving additions to allow
   */
  public void setNonImprovingAdditions(int t) {
    m_nonImprovingAdditions = t;
  }

  /**
   * Get the number of consecutive non-improving additions to tolerate before
   * terminating the search
   * 
   * @return the number of non-improving additions to allow
   */
  public int getNonImprovingAdditions() {
    return m_nonImprovingAdditions;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   **/
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>(7);

    newVector.addElement(new Option(
      "\tclass name of attribute evaluator to use for ranking. Place any\n"
        + "\tevaluator options LAST on the command line following a \"--\".\n"
        + "\teg.:\n"
        + "\t\t-A weka.attributeSelection.GainRatioAttributeEval ... -- -M\n"
        + "\t(default: weka.attributeSelection.GainRatioAttributeEval)", "A",
      1, "-A <attribute evaluator>"));

    newVector.addElement(new Option(
      "\tnumber of attributes to be added from the"
        + "\n\tranking in each iteration (default = 1).", "S", 1,
      "-S <step size>"));

    newVector.addElement(new Option(
      "\tpoint in the ranking to start evaluating from. "
        + "\n\t(default = 0, ie. the head of the ranking).", "R", 1,
      "-R <start point>"));

    newVector.addElement(new Option(
      "\tThreshold on improvement in merit by which to "
        + "accept\n\tadditional attributes from the ranked list "
        + "\n\t(default = 0).", "I", 1, "-I <threshold>"));

    newVector.addElement(new Option(
      "\tNumber of non-improving additions to the best subset seen"
        + "\n\tto tolerate before terminating the search (default = 0, i.e."
        + "\n\tdon't terminate early).", "N", 1,
      "-N <number of non-improving additions>"));

    newVector.addElement(new Option("\tExclude non improving "
      + "attributes when\n\t"
      + "considering more attributes from the ranked list", "X", 0, "-X"));

    newVector.addElement(new Option("\tPrint debugging output", "D", 0, "-D"));

    if ((m_ASEval != null) && (m_ASEval instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to "
        + "evaluator " + m_ASEval.getClass().getName() + ":"));

      newVector.addAll(Collections.list(((OptionHandler) m_ASEval)
        .listOptions()));
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -A &lt;attribute evaluator&gt;
   *  class name of attribute evaluator to use for ranking. Place any
   *  evaluator options LAST on the command line following a "--".
   *  eg.:
   *   -A weka.attributeSelection.GainRatioAttributeEval ... -- -M
   *  (default: weka.attributeSelection.GainRatioAttributeEval)
   * </pre>
   * 
   * <pre>
   * -S &lt;step size&gt;
   *  number of attributes to be added from the
   *  ranking in each iteration (default = 1).
   * </pre>
   * 
   * <pre>
   * -R &lt;start point&gt;
   *  point in the ranking to start evaluating from. 
   *  (default = 0, ie. the head of the ranking).
   * </pre>
   * 
   * <pre>
   * Options specific to evaluator weka.attributeSelection.GainRatioAttributeEval:
   * </pre>
   * 
   * <pre>
   * -M
   *  treat missing values as a seperate value.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setStepSize(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setStartPoint(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('I', options);
    if (optionString.length() > 0) {
      setImprovementThreshold(Double.parseDouble(optionString));
    }

    optionString = Utils.getOption('N', options);
    if (optionString.length() > 0) {
      setNonImprovingAdditions(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('A', options);
    if (optionString.length() == 0) {
      optionString = GainRatioAttributeEval.class.getName();
    }
    setAttributeEvaluator(ASEvaluation.forName(optionString,
      Utils.partitionOptions(options)));

    setExcludeNonImprovingAttributes(Utils.getFlag('X', options));

    setDebuggingOutput(Utils.getFlag('D', options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-S");
    options.add("" + getStepSize());

    options.add("-R");
    options.add("" + getStartPoint());

    options.add("-N");
    options.add("" + getNonImprovingAdditions());

    options.add("-I");
    options.add("" + getImprovementThreshold());

    if (getExcludeNonImprovingAttributes()) {
      options.add("-X");
    }

    if (getDebuggingOutput()) {
      options.add("-D");
    }

    if (getAttributeEvaluator() != null) {
      options.add("-A");
      options.add(getAttributeEvaluator().getClass().getName());
    }

    if ((m_ASEval != null) && (m_ASEval instanceof OptionHandler)) {
      String[] evaluatorOptions = ((OptionHandler) m_ASEval).getOptions();

      if (evaluatorOptions.length > 0) {
        options.add("--");
        Collections.addAll(options, evaluatorOptions);
      }
    }

    return options.toArray(new String[0]);
  }

  /**
   * Reset the search method.
   */
  protected void resetOptions() {
    m_ASEval = new GainRatioAttributeEval();
    m_Ranking = null;
  }

  /**
   * Ranks attributes using the specified attribute evaluator and then searches
   * the ranking using the supplied subset evaluator.
   * 
   * @param ASEval the subset evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @throws Exception if the search can't be completed
   */
  @Override
  public int[] search(ASEvaluation ASEval, Instances data) throws Exception {

    double best_merit = -Double.MAX_VALUE;
    double temp_merit;
    BitSet temp_group, best_group = null;

    if (!(ASEval instanceof SubsetEvaluator)) {
      throw new Exception(ASEval.getClass().getName() + " is not a "
        + "Subset evaluator!");
    }

    m_SubsetEval = ASEval;
    m_Instances = data;
    m_numAttribs = m_Instances.numAttributes();

    /*
     * if (m_ASEval instanceof AttributeTransformer) { throw new
     * Exception("Can't use an attribute transformer " +"with RankSearch"); }
     */
    /*
     * if (m_ASEval instanceof UnsupervisedAttributeEvaluator || NOT USED
     * m_ASEval instanceof UnsupervisedSubsetEvaluator) { m_hasClass = false;
     */
    /*
     * if (!(m_SubsetEval instanceof UnsupervisedSubsetEvaluator)) { throw new
     * Exception("Must use an unsupervised subset evaluator."); }
     */
    /*
     * } NOT USED else { m_hasClass = true; m_classIndex =
     * m_Instances.classIndex(); }
     */

    if (m_debug) {
      System.err.println("Ranking...");
    }
    if (m_ASEval instanceof AttributeEvaluator) {
      // generate the attribute ranking first
      Ranker ranker = new Ranker();
      m_ASEval.buildEvaluator(m_Instances);
      if (m_ASEval instanceof AttributeTransformer) {
        // get the transformed data a rebuild the subset evaluator
        m_Instances = ((AttributeTransformer) m_ASEval)
          .transformedData(m_Instances);
        m_SubsetEval.buildEvaluator(m_Instances);
      }
      m_Ranking = ranker.search(m_ASEval, m_Instances);
    } else {
      GreedyStepwise fs = new GreedyStepwise();
      double[][] rankres;
      fs.setGenerateRanking(true);
      m_ASEval.buildEvaluator(m_Instances);
      fs.search(m_ASEval, m_Instances);
      rankres = fs.rankedAttributes();
      m_Ranking = new int[rankres.length];
      for (int i = 0; i < rankres.length; i++) {
        m_Ranking[i] = (int) rankres[i][0];
      }
    }

    boolean[] dontAdd = null;
    if (m_excludeNonImproving) {
      dontAdd = new boolean[m_Ranking.length];
    }

    if (m_debug) {
      System.err.println("Done ranking. Evaluating ranking...");
    }

    int additions = 0;
    // now evaluate the attribute ranking
    int tenPercent = (m_Ranking.length - m_startPoint) / 10;
    int count = 0;
    for (int i = m_startPoint; i < m_Ranking.length;) {
      i += m_add;
      if (i > m_Ranking.length) {
        i = m_Ranking.length;
      }
      temp_group = new BitSet(m_numAttribs);
      for (int j = 0; j < i; j++) {
        if (m_excludeNonImproving) {
          if (!dontAdd[j]) {
            temp_group.set(m_Ranking[j]);
          }
        } else {
          temp_group.set(m_Ranking[j]);
        }
      }

      additions++;
      count += m_add;
      temp_merit = ((SubsetEvaluator) m_SubsetEval).evaluateSubset(temp_group);

      if (m_debug && tenPercent > 0 && count >= tenPercent) {
        System.err
          .println(""
            + ((double) (i - m_startPoint) / (double) (m_Ranking.length - m_startPoint))
            * 100.0 + " percent complete");
        count = 0;
      }

      if (temp_merit - best_merit > m_improvementThreshold) {
        best_merit = temp_merit;
        ;
        best_group = temp_group;
        additions = 0;

        if (m_debug) {
          int[] atts = attributeList(best_group);
          System.err.print("Best subset found so far (" + atts.length
            + " features): ");
          for (int a : atts) {
            System.err.print("" + (a + 1) + " ");
          }
          System.err.println("\nMerit: " + best_merit);
        }
      } else if (m_excludeNonImproving && i > 0) {
        if (m_debug) {
          System.err.println("Skipping atts ranked " + (i - m_add) + " to " + i
            + " as there is no improvement");
        }
        for (int j = (i - m_add); j < i; j++) {
          dontAdd[j] = true;
        }
      }

      if (m_nonImprovingAdditions > 0 && additions > m_nonImprovingAdditions) {
        if (m_debug) {
          System.err.println("Terminating the search after "
            + m_nonImprovingAdditions + " non-improving additions");
        }
        break;
      }
    }

    m_bestMerit = best_merit;
    return attributeList(best_group);
  }

  /**
   * converts a BitSet into a list of attribute indexes
   * 
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  private int[] attributeList(BitSet group) {
    int count = 0;

    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
        count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
        list[count++] = i;
      }
    }

    return list;
  }

  /**
   * returns a description of the search as a String
   * 
   * @return a description of the search
   */
  @Override
  public String toString() {
    StringBuffer text = new StringBuffer();
    text.append("\tRankSearch :\n");
    text.append("\tAttribute evaluator : "
      + getAttributeEvaluator().getClass().getName() + " ");
    if (m_ASEval instanceof OptionHandler) {
      String[] evaluatorOptions = new String[0];
      evaluatorOptions = ((OptionHandler) m_ASEval).getOptions();
      for (String evaluatorOption : evaluatorOptions) {
        text.append(evaluatorOption + ' ');
      }
    }
    text.append("\n");
    text.append("\tAttribute ranking : \n");
    int rlength = (int) (Math.log(m_Ranking.length) / Math.log(10) + 1);
    for (int element : m_Ranking) {
      text.append("\t " + Utils.doubleToString(element + 1, rlength, 0) + " "
        + m_Instances.attribute(element).name() + '\n');
    }
    text.append("\tMerit of best subset found : ");
    int fieldwidth = 3;
    double precision = (m_bestMerit - (int) m_bestMerit);
    if (Math.abs(m_bestMerit) > 0) {
      fieldwidth = (int) Math.abs((Math.log(Math.abs(m_bestMerit)) / Math
        .log(10))) + 2;
    }
    if (Math.abs(precision) > 0) {
      precision = Math.abs((Math.log(Math.abs(precision)) / Math.log(10))) + 3;
    } else {
      precision = 2;
    }

    text.append(Utils.doubleToString(Math.abs(m_bestMerit), fieldwidth
      + (int) precision, (int) precision)
      + "\n");
    return text.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
