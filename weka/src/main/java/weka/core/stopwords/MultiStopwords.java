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
 * MultiStopwords.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;

/**
 <!-- globalinfo-start -->
 * Applies the specified stopwords algorithms one after other.<br/>
 * As soon as a word has been identified as stopword, the loop is exited.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, stopword scheme is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -stopwords &lt;classname + options&gt;
 *  The stopwords algorithms to apply sequentially.
 *  (default: none)</pre>
 * 
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10978 $
 */
public class MultiStopwords
  extends AbstractStopwords {

  /** for serialization. */
  private static final long serialVersionUID = -8568762652879773063L;

  /** the stopwords algorithms to use. */
  protected StopwordsHandler[] m_Stopwords = new StopwordsHandler[0];

  /* (non-Javadoc)
   * @see weka.core.stopwords.AbstractStopwords#globalInfo()
   */
  @Override
  public String globalInfo() {
    return 
	"Applies the specified stopwords algorithms one after other.\n"
	+ "As soon as a word has been identified as stopword, the loop is "
	+ "exited.";
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    Enumeration<Option> enm = super.listOptions();
    while (enm.hasMoreElements())
      result.add(enm.nextElement());

    result.addElement(new Option(
      "\t" + stopwordsTipText() + "\n"
      + "\t(default: none)",
      "stopwords", 1, "-stopwords <classname + options>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String			tmpStr;
    String[]			tmpOptions;
    List<StopwordsHandler>	handlers;

    handlers = new ArrayList<StopwordsHandler>();
    do {
      tmpStr = Utils.getOption("stopwords", options);
      if (!tmpStr.isEmpty()) {
	tmpOptions    = Utils.splitOptions(tmpStr);
	tmpStr        = tmpOptions[0];
	tmpOptions[0] = "";
	handlers.add((StopwordsHandler) Utils.forName(StopwordsHandler.class, tmpStr, tmpOptions));
      }
    }
    while (!tmpStr.isEmpty());
    
    setStopwords(handlers.toArray(new StopwordsHandler[handlers.size()]));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>(Arrays.asList(super.getOptions()));

    for (StopwordsHandler handler: m_Stopwords) {
      options.add("-stopwords");
      options.add(Utils.toCommandLine(handler));
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Sets the stopwords algorithms.
   *
   * @param value 	the algorithms
   */
  public void setStopwords(StopwordsHandler[] value) {
    m_Stopwords = value;
    reset();
  }

  /**
   * Returns the stopwords algorithms.
   *
   * @return 		the algorithms
   */
  public StopwordsHandler[] getStopwords() {
    return m_Stopwords;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stopwordsTipText() {
    return "The stopwords algorithms to apply sequentially.";
  }
  
  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return true if the word is a stopword
   */
  @Override
  protected boolean is(String word) {
    boolean	result;
    
    result = false;
    
    for (StopwordsHandler handler: m_Stopwords) {
      if (handler.isStopword(word)) {
	result = true;
	break;
      }
    }
    
    return result;
  }
}
