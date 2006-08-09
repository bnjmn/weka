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
 *    RELEASE INFORMATION (December 27, 2004)
 *    
 *    FCBF algorithm:
 *      Template obtained from Weka
 *      Developped for Weka by Zheng Alan Zhao   
 *      December 27, 2004
 *
 *    FCBF algorithm is a feature selection method based on Symmetrical Uncertainty Measurement for 
 *    relevance redundancy analysis. The details of FCBF algorithm are in:
 *
 <!-- technical-plaintext-start -->
 * Lei Yu, Huan Liu: Feature Selection for High-Dimensional Data: A Fast Correlation-Based Filter Solution. In: Proceedings of the Twentieth International Conference on Machine Learning, 856-863, 2003.
 <!-- technical-plaintext-end -->
 *    
 *    
 *    CONTACT INFORMATION
 *    
 *    For algorithm implementation:
 *    Zheng Zhao: zhaozheng at asu.edu
 *      
 *    For the algorithm:
 *    Lei Yu: leiyu at asu.edu
 *    Huan Liu: hliu at asu.edu
 *     
 *    Data Mining and Machine Learning Lab
 *    Computer Science and Engineering Department
 *    Fulton School of Engineering
 *    Arizona State University
 *    Tempe, AZ 85287
 *
 *    FCBFSearch.java
 *
 *    Copyright (C) 2004 Data Mining and Machine Learning Lab, 
 *                       Computer Science and Engineering Department, 
 *			 Fulton School of Engineering, 
 *                       Arizona State University
 *
 */


package weka.attributeSelection;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * FCBF : <br/>
 * <br/>
 * Feature selection method based on correlation measureand relevance&amp;redundancy analysis. Use in conjunction with an attribute set evaluator (SymmetricalUncertAttributeEval).<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Lei Yu, Huan Liu: Feature Selection for High-Dimensional Data: A Fast Correlation-Based Filter Solution. In: Proceedings of the Twentieth International Conference on Machine Learning, 856-863, 2003.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Yu2003,
 *    author = {Lei Yu and Huan Liu},
 *    booktitle = {Proceedings of the Twentieth International Conference on Machine Learning},
 *    pages = {856-863},
 *    publisher = {AAAI Press},
 *    title = {Feature Selection for High-Dimensional Data: A Fast Correlation-Based Filter Solution},
 *    year = {2003}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D &lt;create dataset&gt;
 *  Specify Whether the selector generate a new dataset.</pre>
 * 
 * <pre> -P &lt;start set&gt;
 *  Specify a starting set of attributes.
 *  Eg. 1,3,5-7. 
 * Any starting attributes specified are 
 * ignored during the ranking.</pre>
 * 
 * <pre> -T &lt;threshold&gt;
 *  Specify a theshold by which attributes may be discarded from the ranking.</pre>
 * 
 * <pre> -N &lt;num to select&gt;
 *  Specify number of attributes to select</pre>
 * 
 <!-- options-end -->
 *
 * @author Zheng Zhao: zhaozheng at asu.edu
 * @version $Revision: 1.5 $
 */
public class FCBFSearch 
  extends ASSearch
  implements RankedOutputSearch, StartSetHandler, OptionHandler,
             TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 8209699587428369942L;
  
  /** Holds the starting set as an array of attributes */
  private int[] m_starting;

  /** Holds the start set for the search as a range */
  private Range m_startRange;

  /** Holds the ordered list of attributes */
  private int[] m_attributeList;

  /** Holds the list of attribute merit scores */
  private double[] m_attributeMerit;

  /** Data has class attribute---if unsupervised evaluator then no class */
  private boolean m_hasClass;

  /** Class index of the data if supervised evaluator */
  private int m_classIndex;

  /** The number of attribtes */
  private int m_numAttribs;

  /**
   * A threshold by which to discard attributes---used by the
   * AttributeSelection module
   */
  private double m_threshold;

  /** The number of attributes to select. -1 indicates that all attributes
      are to be retained. Has precedence over m_threshold */
  private int m_numToSelect = -1;

  /** Used to compute the number to select */
  private int m_calculatedNumToSelect = -1;

  /*-----------------add begin 2004-11-15 by alan-----------------*/
  /** Used to determine whether we create a new dataset according to the selected features */
  private boolean m_generateOutput = false;

  /** Used to store the ref of the Evaluator we use*/
  private ASEvaluation m_asEval;

  /** Holds the list of attribute merit scores generated by FCBF */
  private double[][] m_rankedFCBF;

  /** Hold the list of selected features*/
  private double[][] m_selectedFeatures;
  /*-----------------add end 2004-11-15 by alan-----------------*/

   /**
   * Returns a string describing this search method
   * @return a description of the search suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "FCBF : \n\nFeature selection method based on correlation measure"
      + "and relevance&redundancy analysis. "
      + "Use in conjunction with an attribute set evaluator (SymmetricalUncertAttributeEval).\n\n"
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
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Lei Yu and Huan Liu");
    result.setValue(Field.TITLE, "Feature Selection for High-Dimensional Data: A Fast Correlation-Based Filter Solution");
    result.setValue(Field.BOOKTITLE, "Proceedings of the Twentieth International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.PAGES, "856-863");
    result.setValue(Field.PUBLISHER, "AAAI Press");
    
    return result;
  }

  /**
   * Constructor
   */
  public FCBFSearch () {
    resetOptions();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numToSelectTipText() {
    return "Specify the number of attributes to retain. The default value "
      +"(-1) indicates that all attributes are to be retained. Use either "
      +"this option or a threshold to reduce the attribute set.";
  }

  /**
   * Specify the number of attributes to select from the ranked list. -1
   * indicates that all attributes are to be retained.
   * @param n the number of attributes to retain
   */
  public void setNumToSelect(int n) {
    m_numToSelect = n;
  }

  /**
   * Gets the number of attributes to be retained.
   * @return the number of attributes to retain
   */
  public int getNumToSelect() {
    return m_numToSelect;
  }

  /**
   * Gets the calculated number to select. This might be computed
   * from a threshold, or if < 0 is set as the number to select then
   * it is set to the number of attributes in the (transformed) data.
   * @return the calculated number of attributes to select
   */
  public int getCalculatedNumToSelect() {
    if (m_numToSelect >= 0) {
      m_calculatedNumToSelect = m_numToSelect;
    }
    if (m_selectedFeatures.length>0
        && m_selectedFeatures.length<m_calculatedNumToSelect)
    {
      m_calculatedNumToSelect = m_selectedFeatures.length;
    }

    return m_calculatedNumToSelect;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Set threshold by which attributes can be discarded. Default value "
      + "results in no attributes being discarded. Use either this option or "
      +"numToSelect to reduce the attribute set.";
  }

  /**
   * Set the threshold by which the AttributeSelection module can discard
   * attributes.
   * @param threshold the threshold.
   */
  public void setThreshold(double threshold) {
    m_threshold = threshold;
  }

  /**
   * Returns the threshold so that the AttributeSelection module can
   * discard attributes from the ranking.
   * @return the threshold
   */
  public double getThreshold() {
    return m_threshold;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String generateRankingTipText() {
    return "A constant option. FCBF is capable of generating"
      +" attribute rankings.";
  }

  /**
   * This is a dummy set method---Ranker is ONLY capable of producing
   * a ranked list of attributes for attribute evaluators.
   * @param doRank this parameter is N/A and is ignored
   */
  public void setGenerateRanking(boolean doRank) {
  }

  /**
   * This is a dummy method. Ranker can ONLY be used with attribute
   * evaluators and as such can only produce a ranked list of attributes
   * @return true all the time.
   */
  public boolean getGenerateRanking() {
    return true;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */

  public String generateDataOutputTipText() {
    return "Generating new dataset according to the selected features."
      +" ";
  }

  /**
   * Sets the flag, by which the AttributeSelection module decide
   * whether create a new dataset according to the selected features.
   * @param doGenerate the flag, by which the AttributeSelection module
   * decide whether create a new dataset according to the selected
   * features
   */
  public void setGenerateDataOutput(boolean doGenerate) {
    this.m_generateOutput = doGenerate;

  }

  /**
   * Returns the flag, by which the AttributeSelection module decide
   * whether create a new dataset according to the selected features.
   * @return the flag, by which the AttributeSelection module decide
   * whether create a new dataset according to the selected features.
   */
  public boolean getGenerateDataOutput() {
    return this.m_generateOutput;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String startSetTipText() {
    return "Specify a set of attributes to ignore. "
      +" When generating the ranking, FCBF will not evaluate the attributes "
      +" in this list. "
      +"This is specified as a comma "
      +"seperated list off attribute indexes starting at 1. It can include "
      +"ranges. Eg. 1,2,5-9,17.";
  }

  /**
   * Sets a starting set of attributes for the search. It is the
   * search method's responsibility to report this start set (if any)
   * in its toString() method.
   * @param startSet a string containing a list of attributes (and or ranges),
   * eg. 1,2,6,10-15.
   * @throws Exception if start set can't be set.
   */
  public void setStartSet (String startSet) throws Exception {
    m_startRange.setRanges(startSet);
  }

  /**
   * Returns a list of attributes (and or attribute ranges) as a String
   * @return a list of attributes (and or attribute ranges)
   */
  public String getStartSet () {
    return m_startRange.getRanges();
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(4);

    newVector
      .addElement(new Option("\tSpecify Whether the selector generate a new dataset."
                             ,"D",1
                             , "-D <create dataset>"));

    newVector
      .addElement(new Option("\tSpecify a starting set of attributes."
                             + "\n\tEg. 1,3,5-7."
                             +"\t\nAny starting attributes specified are"
                             +"\t\nignored during the ranking."
                             ,"P",1
                             , "-P <start set>"));
    newVector
      .addElement(new Option("\tSpecify a theshold by which attributes"
                             + "\tmay be discarded from the ranking.","T",1
                             , "-T <threshold>"));

    newVector
      .addElement(new Option("\tSpecify number of attributes to select"
                             ,"N",1
                             , "-N <num to select>"));

    return newVector.elements();

  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D &lt;create dataset&gt;
   *  Specify Whether the selector generate a new dataset.</pre>
   * 
   * <pre> -P &lt;start set&gt;
   *  Specify a starting set of attributes.
   *  Eg. 1,3,5-7. 
   * Any starting attributes specified are 
   * ignored during the ranking.</pre>
   * 
   * <pre> -T &lt;threshold&gt;
   *  Specify a theshold by which attributes may be discarded from the ranking.</pre>
   * 
   * <pre> -N &lt;num to select&gt;
   *  Specify number of attributes to select</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('D', options);
    if (optionString.length() != 0) {
      setGenerateDataOutput(Boolean.getBoolean(optionString));
    }

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setStartSet(optionString);
    }

    optionString = Utils.getOption('T', options);
    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setThreshold(temp.doubleValue());
    }

    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setNumToSelect(Integer.parseInt(optionString));
    }
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[8];
    int current = 0;

      options[current++] = "-D";
      options[current++] = ""+getGenerateDataOutput();

    if (!(getStartSet().equals(""))) {
      options[current++] = "-P";
      options[current++] = ""+startSetToString();
    }

    options[current++] = "-T";
    options[current++] = "" + getThreshold();

    options[current++] = "-N";
    options[current++] = ""+getNumToSelect();

    while (current < options.length) {
      options[current++] = "";
    }
    return  options;
  }

  /**
   * converts the array of starting attributes to a string. This is
   * used by getOptions to return the actual attributes specified
   * as the starting set. This is better than using m_startRanges.getRanges()
   * as the same start set can be specified in different ways from the
   * command line---eg 1,2,3 == 1-3. This is to ensure that stuff that
   * is stored in a database is comparable.
   * @return a comma seperated list of individual attribute numbers as a String
   */
  private String startSetToString() {
    StringBuffer FString = new StringBuffer();
    boolean didPrint;

    if (m_starting == null) {
      return getStartSet();
    }

    for (int i = 0; i < m_starting.length; i++) {
      didPrint = false;

      if ((m_hasClass == false) ||
          (m_hasClass == true && i != m_classIndex)) {
        FString.append((m_starting[i] + 1));
        didPrint = true;
      }

      if (i == (m_starting.length - 1)) {
        FString.append("");
      }
      else {
        if (didPrint) {
          FString.append(",");
        }
      }
    }

    return FString.toString();
  }

  /**
   * Kind of a dummy search algorithm. Calls a Attribute evaluator to
   * evaluate each attribute not included in the startSet and then sorts
   * them to produce a ranked list of attributes.
   *
   * @param ASEval the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @throws Exception if the search can't be completed
   */
  public int[] search (ASEvaluation ASEval, Instances data)
    throws Exception {
    int i, j;

    if (!(ASEval instanceof AttributeSetEvaluator)) {
      throw  new Exception(ASEval.getClass().getName()
                           + " is not an "
                           + "Attribute Set evaluator!");
    }

    m_numAttribs = data.numAttributes();

    if (ASEval instanceof UnsupervisedAttributeEvaluator) {
      m_hasClass = false;
    }
    else {
      m_classIndex = data.classIndex();
      if (m_classIndex >= 0) {
        m_hasClass = true;
      } else {
        m_hasClass = false;
      }
    }

    // get the transformed data and check to see if the transformer
    // preserves a class index
    if (ASEval instanceof AttributeTransformer) {
      data = ((AttributeTransformer)ASEval).transformedHeader();
      if (m_classIndex >= 0 && data.classIndex() >= 0) {
        m_classIndex = data.classIndex();
        m_hasClass = true;
      }
    }


    m_startRange.setUpper(m_numAttribs - 1);
    if (!(getStartSet().equals(""))) {
      m_starting = m_startRange.getSelection();
    }

    int sl=0;
    if (m_starting != null) {
      sl = m_starting.length;
    }
    if ((m_starting != null) && (m_hasClass == true)) {
      // see if the supplied list contains the class index
      boolean ok = false;
      for (i = 0; i < sl; i++) {
        if (m_starting[i] == m_classIndex) {
          ok = true;
          break;
        }
      }

      if (ok == false) {
        sl++;
      }
    }
    else {
      if (m_hasClass == true) {
        sl++;
      }
    }


    m_attributeList = new int[m_numAttribs - sl];
    m_attributeMerit = new double[m_numAttribs - sl];

    // add in those attributes not in the starting (omit list)
    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (!inStarting(i)) {
        m_attributeList[j++] = i;
      }
    }

    this.m_asEval = ASEval;
    AttributeSetEvaluator ASEvaluator = (AttributeSetEvaluator)ASEval;

    for (i = 0; i < m_attributeList.length; i++) {
      m_attributeMerit[i] = ASEvaluator.evaluateAttribute(m_attributeList[i]);
    }

    double[][] tempRanked = rankedAttributes();
    int[] rankedAttributes = new int[m_selectedFeatures.length];

    for (i = 0; i < m_selectedFeatures.length; i++) {
      rankedAttributes[i] = (int)tempRanked[i][0];
    }
    return  rankedAttributes;
  }



  /**
   * Sorts the evaluated attribute list
   *
   * @return an array of sorted (highest eval to lowest) attribute indexes
   * @throws Exception of sorting can't be done.
   */
  public double[][] rankedAttributes ()
    throws Exception {
    int i, j;

    if (m_attributeList == null || m_attributeMerit == null) {
      throw  new Exception("Search must be performed before a ranked "
                           + "attribute list can be obtained");
    }

    int[] ranked = Utils.sort(m_attributeMerit);
    // reverse the order of the ranked indexes
    double[][] bestToWorst = new double[ranked.length][2];

    for (i = ranked.length - 1, j = 0; i >= 0; i--) {
      bestToWorst[j++][0] = ranked[i];
    //alan: means in the arrary ranked, varialbe is from ranked as from small to large
    }

    // convert the indexes to attribute indexes
    for (i = 0; i < bestToWorst.length; i++) {
      int temp = ((int)bestToWorst[i][0]);
      bestToWorst[i][0] = m_attributeList[temp];     //for the index
      bestToWorst[i][1] = m_attributeMerit[temp];    //for the value of the index
    }

    if (m_numToSelect > bestToWorst.length) {
      throw new Exception("More attributes requested than exist in the data");
    }

    this.FCBFElimination(bestToWorst);

    if (m_numToSelect <= 0) {
      if (m_threshold == -Double.MAX_VALUE) {
        m_calculatedNumToSelect = m_selectedFeatures.length;
      } else {
        determineNumToSelectFromThreshold(m_selectedFeatures);
      }
    }
    /*    if (m_numToSelect > 0) {
      determineThreshFromNumToSelect(bestToWorst);
      } */

    return  m_selectedFeatures;
  }

  private void determineNumToSelectFromThreshold(double [][] ranking) {
    int count = 0;
    for (int i = 0; i < ranking.length; i++) {
      if (ranking[i][1] > m_threshold) {
        count++;
      }
    }
    m_calculatedNumToSelect = count;
  }

  private void determineThreshFromNumToSelect(double [][] ranking)
    throws Exception {
    if (m_numToSelect > ranking.length) {
      throw new Exception("More attributes requested than exist in the data");
    }

    if (m_numToSelect == ranking.length) {
      return;
    }

    m_threshold = (ranking[m_numToSelect-1][1] +
                   ranking[m_numToSelect][1]) / 2.0;
  }

  /**
   * returns a description of the search as a String
   * @return a description of the search
   */
  public String toString () {
    StringBuffer BfString = new StringBuffer();
    BfString.append("\tAttribute ranking.\n");

    if (m_starting != null) {
      BfString.append("\tIgnored attributes: ");

      BfString.append(startSetToString());
      BfString.append("\n");
    }

    if (m_threshold != -Double.MAX_VALUE) {
      BfString.append("\tThreshold for discarding attributes: "
                      + Utils.doubleToString(m_threshold,8,4)+"\n");
    }

    BfString.append("\n\n");

    BfString.append("     J || SU(j,Class) ||    I || SU(i,j). \n");

    for (int i=0; i<m_rankedFCBF.length; i++)
    {
      BfString.append(Utils.doubleToString(m_rankedFCBF[i][0]+1,6,0)+" ; "
                      +Utils.doubleToString(m_rankedFCBF[i][1],12,7)+" ; ");
      if (m_rankedFCBF[i][2] == m_rankedFCBF[i][0])
      {
        BfString.append("    *\n");
      }
      else
      {
        BfString.append(Utils.doubleToString(m_rankedFCBF[i][2] + 1,5,0) + " ; "
                     + m_rankedFCBF[i][3] + "\n");
      }
    }

    return BfString.toString();
  }


  /**
   * Resets stuff to default values
   */
  protected void resetOptions () {
    m_starting = null;
    m_startRange = new Range();
    m_attributeList = null;
    m_attributeMerit = null;
    m_threshold = -Double.MAX_VALUE;
  }


  private boolean inStarting (int feat) {
    // omit the class from the evaluation
    if ((m_hasClass == true) && (feat == m_classIndex)) {
      return  true;
    }

    if (m_starting == null) {
      return  false;
    }

    for (int i = 0; i < m_starting.length; i++) {
      if (m_starting[i] == feat) {
        return  true;
      }
    }

    return  false;
  }

  private void FCBFElimination(double[][]rankedFeatures)
  throws Exception {

    int i,j;

    m_rankedFCBF = new double[m_attributeList.length][4];
    int[] attributes = new int[1];
    int[] classAtrributes = new int[1];

    int numSelectedAttributes = 0;

    int startPoint = 0;
    double tempSUIJ = 0;

    AttributeSetEvaluator ASEvaluator = (AttributeSetEvaluator)m_asEval;

    for (i = 0; i < rankedFeatures.length; i++) {
      m_rankedFCBF[i][0] = rankedFeatures[i][0];
      m_rankedFCBF[i][1] = rankedFeatures[i][1];
      m_rankedFCBF[i][2] = -1;
    }

    while (startPoint < rankedFeatures.length)
    {
      if (m_rankedFCBF[startPoint][2] != -1)
      {
        startPoint++;
        continue;
      }

      m_rankedFCBF[startPoint][2] = m_rankedFCBF[startPoint][0];
      numSelectedAttributes++;

      for (i = startPoint + 1; i < m_attributeList.length; i++)
      {
        if (m_rankedFCBF[i][2] != -1)
        {
          continue;
        }
        attributes[0] = (int) m_rankedFCBF[startPoint][0];
        classAtrributes[0] = (int) m_rankedFCBF[i][0];
        tempSUIJ = ASEvaluator.evaluateAttribute(attributes, classAtrributes);
        if (m_rankedFCBF[i][1] < tempSUIJ || Math.abs(tempSUIJ-m_rankedFCBF[i][1])<1E-8)
        {
          m_rankedFCBF[i][2] = m_rankedFCBF[startPoint][0];
          m_rankedFCBF[i][3] = tempSUIJ;
        }
      }
      startPoint++;
    }

    m_selectedFeatures = new double[numSelectedAttributes][2];

    for (i = 0, j = 0; i < m_attributeList.length; i++)
    {
      if (m_rankedFCBF[i][2] == m_rankedFCBF[i][0])
      {
        m_selectedFeatures[j][0] = m_rankedFCBF[i][0];
        m_selectedFeatures[j][1] = m_rankedFCBF[i][1];
        j++;
      }
    }
  }
}

