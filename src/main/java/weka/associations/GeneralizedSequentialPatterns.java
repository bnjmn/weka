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
 * GeneralizedSequentialPatterns.java
 * Copyright (C) 2007 Sebastian Beer
 *
 */

package weka.associations;

import weka.associations.gsp.Element;
import weka.associations.gsp.Sequence;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class implementing a GSP algorithm for discovering sequential patterns in a sequential data set.<br/>
 * The attribute identifying the distinct data sequences contained in the set can be determined by the respective option. Furthermore, the set of output results can be restricted by specifying one or more attributes that have to be contained in each element/itemset of a sequence.<br/>
 * <br/>
 * For further information see:<br/>
 * <br/>
 * Ramakrishnan Srikant, Rakesh Agrawal (1996). Mining Sequential Patterns: Generalizations and Performance Improvements.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;proceedings{Srikant1996,
 *    author = {Ramakrishnan Srikant and Rakesh Agrawal},
 *    booktitle = {Advances in Database Technology EDBT '96},
 *    publisher = {Springer},
 *    title = {Mining Sequential Patterns: Generalizations and Performance Improvements},
 *    year = {1996}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, algorithm is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -S &lt;minimum support threshold&gt;
 *  The miminum support threshold.
 *  (default: 0.9)</pre>
 * 
 * <pre> -I &lt;attribute number representing the data sequence ID
 *  The attribute number representing the data sequence ID.
 *  (default: 0)</pre>
 * 
 * <pre> -F &lt;attribute numbers used for result filtering
 *  The attribute numbers used for result filtering.
 *  (default: -1)</pre>
 * 
 <!-- options-end -->
 *
 * @author  Sebastian Beer
 * @version $Revision$
 */
public class GeneralizedSequentialPatterns
  extends AbstractAssociator
  implements OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -4119691320812254676L;

  /** the minimum support threshold */
  protected double m_MinSupport; 

  /** number indicating the attribute holding the data sequence ID */
  protected int m_DataSeqID;

  /** original sequential data set to be used for sequential patterns extraction */
  protected Instances m_OriginalDataSet;
  
  /** all generated frequent sequences, i.e. sequential patterns */
  protected FastVector m_AllSequentialPatterns;
  
  /** number of cycles performed until termination */
  protected int m_Cycles;
  
  /** String indicating the starting time of an cycle. */
  protected String m_CycleStart;
  
  /** String indicating the ending time of an cycle. */
  protected String m_CycleEnd;
  
  /** String indicating the starting time of the algorithm. */
  protected String m_AlgorithmStart;
  
  /** String containing the attribute numbers that are used for result 
   * filtering; -1 means no filtering */
  protected String m_FilterAttributes;
  
  /** Vector containing the attribute numbers that are used for result 
   * filtering; -1 means no filtering */
  protected FastVector m_FilterAttrVector;
  
  /** Whether the classifier is run in debug mode. */
  protected boolean m_Debug = false;

  /**
   * Constructor.
   */
  public GeneralizedSequentialPatterns() {
    resetOptions();
  }

  /**
   * Returns global information about the algorithm.
   * 
   * @return 			the global information
   */
  public String globalInfo() {
    return 
        "Class implementing a GSP algorithm for discovering sequential "
      + "patterns in a sequential data set.\n" 
      + "The attribute identifying the distinct data sequences contained in "
      + "the set can be determined by the respective option. Furthermore, the "
      + "set of output results can be restricted by specifying one or more "
      + "attributes that have to be contained in each element/itemset of a "
      + "sequence.\n\n" 
      + "For further information see:\n\n" 
      + getTechnicalInformation().toString();
  }

  /**
   * Returns TechnicalInformation about the paper related to the algorithm.
   * 
   * @return 			the TechnicalInformation
   */
  public TechnicalInformation getTechnicalInformation() {	
    TechnicalInformation paper = new TechnicalInformation(Type.PROCEEDINGS);

    paper.setValue(Field.AUTHOR, "Ramakrishnan Srikant and Rakesh Agrawal");
    paper.setValue(Field.TITLE, "Mining Sequential Patterns: Generalizations and Performance Improvements");
    paper.setValue(Field.BOOKTITLE, "Advances in Database Technology EDBT '96");
    paper.setValue(Field.YEAR, "1996");
    paper.setValue(Field.PUBLISHER, "Springer");

    return paper;
  }

  /**
   * Returns an enumeration of the available options.
   * 
   * @return 			the available options
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
	"\tIf set, algorithm is run in debug mode and\n"
	+ "\tmay output additional info to the console",
	"D", 0, "-D"));
    
    result.addElement(new Option(
	"\tThe miminum support threshold.\n"
	+ "\t(default: 0.9)",
	"S", 1, "-S <minimum support threshold>"));
    
    result.addElement(new Option(
	"\tThe attribute number representing the data sequence ID.\n"
	+ "\t(default: 0)",
	"I", 1, "-I <attribute number representing the data sequence ID"));

    result.addElement(new Option(
	"\tThe attribute numbers used for result filtering.\n"
	+ "\t(default: -1)",
	"F", 1, "-F <attribute numbers used for result filtering"));

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, algorithm is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -S &lt;minimum support threshold&gt;
   *  The miminum support threshold.
   *  (default: 0.9)</pre>
   * 
   * <pre> -I &lt;attribute number representing the data sequence ID
   *  The attribute number representing the data sequence ID.
   *  (default: 0)</pre>
   * 
   * <pre> -F &lt;attribute numbers used for result filtering
   *  The attribute numbers used for result filtering.
   *  (default: -1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options 		the Array containing the options
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
  
    resetOptions();

    setDebug(Utils.getFlag('D', options));
    
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0)
      setMinSupport(Double.parseDouble(tmpStr));

    tmpStr = Utils.getOption('I', options);
    if (tmpStr.length() != 0)
      setDataSeqID(Integer.parseInt(tmpStr));

    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0)
      setFilterAttributes(tmpStr);
  }

  /**
   * Returns an Array containing the current options settings.
   * 
   * @return 			the Array containing the settings
   */
  public String[] getOptions() {
    Vector<String>	result;
    
    result = new Vector<String>();

    if (getDebug())
      result.add("-D");
    
    result.add("-S");
    result.add("" + getMinSupport());

    result.add("-I");
    result.add("" + getDataSeqID());
    
    result.add("-F");
    result.add(getFilterAttributes());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Resets the algorithm's options to the default values.
   */
  protected void resetOptions() {
    m_MinSupport       = 0.9;
    m_DataSeqID        = 0;
    m_FilterAttributes = "-1";
  }

  /**
   * Returns the Capabilities of the algorithm.
   * 
   * @return 			the Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Extracts all sequential patterns out of a given sequential data set and 
   * prints out the results.
   * 
   * @param data 	the original data set
   */
  public void buildAssociations(Instances data) throws Exception {
    // can associator handle the data?
    getCapabilities().testWithFail(data);

    m_AllSequentialPatterns = new FastVector();
    m_Cycles                = 0;
    m_FilterAttrVector      = new FastVector();
    m_AlgorithmStart        = getTimeAndDate();
    m_OriginalDataSet       = new Instances(data);
    
    extractFilterAttributes(m_FilterAttributes);
    findFrequentSequences();
  }

  /**
   * Calculates the total number of extracted frequent sequences.
   * 
   * @return 			the total number of frequent sequences
   */
  protected int calcFreqSequencesTotal() {
    int total = 0;
    Enumeration allSeqPatternsEnum = m_AllSequentialPatterns.elements();

    while (allSeqPatternsEnum.hasMoreElements()) {
      FastVector kSequences = (FastVector) allSeqPatternsEnum.nextElement();
      total += kSequences.size();
    }

    return total;
  }

  /**
   * Extracts the data sequences out of the original data set according to 
   * their sequence id attribute, which is removed after extraction.
   * 
   * @param originalDataSet 	the original data set
   * @param dataSeqID		the squence ID to use
   * @return 			set of distinct data sequences
   */
  protected FastVector extractDataSequences (Instances originalDataSet, int dataSeqID) {
    FastVector dataSequences = new FastVector();
    int firstInstance = 0;
    int lastInstance = 0;
    Attribute seqIDAttribute = originalDataSet.attribute(dataSeqID);

    for (int i = 0; i < seqIDAttribute.numValues(); i++) {
      double sequenceID = originalDataSet.instance(firstInstance).value(dataSeqID);
      while (lastInstance < originalDataSet.numInstances()
	  && sequenceID == originalDataSet.instance(lastInstance).value(dataSeqID)) {
	lastInstance++;
      }
      Instances dataSequence = new Instances(originalDataSet, firstInstance, (lastInstance)-firstInstance);
      dataSequence.deleteAttributeAt(dataSeqID);
      dataSequences.addElement(dataSequence);
      firstInstance = lastInstance;
    }
    return dataSequences;
  }

  /**
   * Parses a given String containing attribute numbers which are used for 
   * result filtering.
   * 
   * @param attrNumbers 	the String of attribute numbers
   */
  public void extractFilterAttributes(String attrNumbers) {
    String numbers = attrNumbers.trim();

    while (!numbers.equals("")) {
      int commaLoc = numbers.indexOf(',');

      if (commaLoc != -1) {
	String number = numbers.substring(0, commaLoc);
	numbers = numbers.substring(commaLoc + 1).trim();
	m_FilterAttrVector.addElement(Integer.decode(number));
      } else {
	m_FilterAttrVector.addElement(Integer.decode(numbers));
	break;
      }
    }
  }

  /**
   * The actual method for extracting frequent sequences.
   * 
   * @throws CloneNotSupportedException
   */
  protected void findFrequentSequences() throws CloneNotSupportedException {
    m_CycleStart = getTimeAndDate();
    Instances originalDataSet = m_OriginalDataSet;
    FastVector dataSequences = extractDataSequences(m_OriginalDataSet, m_DataSeqID);
    long minSupportCount = Math.round(m_MinSupport * dataSequences.size());
    FastVector kMinusOneSequences;
    FastVector kSequences;

    originalDataSet.deleteAttributeAt(0);
    FastVector oneElements = Element.getOneElements(originalDataSet);
    m_Cycles = 1;

    kSequences = Sequence.oneElementsToSequences(oneElements);
    Sequence.updateSupportCount(kSequences, dataSequences);
    kSequences = Sequence.deleteInfrequentSequences(kSequences, minSupportCount);

    m_CycleEnd = getTimeAndDate();

    if (kSequences.size() == 0) {
      return;
    }
    while (kSequences.size() > 0) {
      m_CycleStart = getTimeAndDate();

      m_AllSequentialPatterns.addElement(kSequences.copy());
      kMinusOneSequences = kSequences;
      kSequences = Sequence.aprioriGen(kMinusOneSequences);
      Sequence.updateSupportCount(kSequences, dataSequences);
      kSequences = Sequence.deleteInfrequentSequences(kSequences, minSupportCount);

      m_CycleEnd = getTimeAndDate();
      
      if (getDebug())
	System.out.println(
	    "Cycle " + m_Cycles + " from " + m_CycleStart + " to " + m_CycleEnd);
      
      m_Cycles++;
    }
  }

  /**
   * Returns the dataSeqID option tip text for the Weka GUI.
   * 
   * @return 			the option tip text
   */
  public String dataSeqIDTipText() {
    return "The attribute number representing the data sequence ID.";
  }

  /**
   * Returns the attribute representing the data sequence ID.
   * 
   * @return 			the data sequence ID
   */
  public int getDataSeqID() {
    return m_DataSeqID;
  }

  /**
   * Sets the attribute representing the data sequence ID.
   * 
   * @param value 		the data sequence ID to set
   */
  public void setDataSeqID(int value) {
    m_DataSeqID = value;
  }

  /**
   * Returns the filterAttributes option tip text for the Weka GUI.
   * 
   * @return 			the option tip text
   */
  public String filterAttributesTipText() {
    return 
        "The attribute numbers (eg \"0, 1\") used for result filtering; only "
      + "sequences containing the specified attributes in each of their "
      + "elements/itemsets will be output; -1 prints all.";
  }

  /**
   * Returns the String containing the attributes which are used for output 
   * filtering.
   * 
   * @return 			the String containing the attributes
   */
  public String getFilterAttributes() {
    return m_FilterAttributes;
  }

  /**
   * Sets the String containing the attributes which are used for output 
   * filtering.
   * 
   * @param value 		the String containing the attributes
   */
  public void setFilterAttributes(String value) {
    m_FilterAttributes = value;
  }

  /**
   * Returns the minimum support option tip text for the Weka GUI.
   * 
   * @return 			the option tip text
   */
  public String minSupportTipText() {
    return "Minimum support threshold.";
  }

  /**
   * Returns the minimum support threshold.
   * 
   * @return 			the minimum support threshold
   */
  public double getMinSupport() {
    return m_MinSupport;
  }

  /**
   * Sets the minimum support threshold.
   * 
   * @param value 		the minimum support threshold
   */
  public void setMinSupport(double value) {
    m_MinSupport = value;
  }

  /**
   * Set debugging mode.
   *
   * @param value 		true if debug output should be printed
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Get whether debugging is turned on.
   *
   * @return 			true if debugging output is on
   */
  public boolean getDebug() {
    return m_Debug;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return 			tip text for this property suitable for
   * 				displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, algorithm may output additional info to the console.";
  }

  /**
   * Returns the current time and date.
   * 
   * @return 			the time and date
   */
  protected String getTimeAndDate() {
    SimpleDateFormat	dateFormat;
    
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    return dateFormat.format(new Date());
  }

  /**
   * Returns the time/date string the algorithm was started
   * 
   * @return			the time and date the algorithm was started
   */
  public String getAlgorithmStart() {
    return m_AlgorithmStart;
  }

  /**
   * Returns the time/date string the cycle was started
   * 
   * @return			the time and date the cycle was started
   */
  public String getCycleStart() {
    return m_CycleStart;
  }

  /**
   * Returns the time/date string the cycle ended
   * 
   * @return			the time and date the cycle ended
   */
  public String getCycleEnd() {
    return m_CycleEnd;
  }
  
  /**
   * Returns a String containing the result information of the algorithm.
   * 
   * @return 			the String containing the result information
   */
  public String toString() {
    StringBuffer result = new StringBuffer();

    result.append("GeneralizedSequentialPatterns\n");
    result.append("=============================\n\n");
    result.append("Number of cycles performed: " + (m_Cycles-1) + "\n");
    result.append("Total number of frequent sequences: " + calcFreqSequencesTotal() + "\n\n");
    result.append("Frequent Sequences Details (filtered):\n\n");
    for (int i = 0; i < m_AllSequentialPatterns.size(); i++) {
      result.append("- " + (i+1) + "-sequences\n\n");
      FastVector kSequences = (FastVector) m_AllSequentialPatterns.elementAt(i);
      result.append(Sequence.setOfSequencesToString(kSequences, m_OriginalDataSet, m_FilterAttrVector) + "\n");
    }

    return result.toString();
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
   * @param args 	commandline options, use -h for help
   */
  public static void main(String[] args) {
    runAssociator(new GeneralizedSequentialPatterns(), args);
  }
}
