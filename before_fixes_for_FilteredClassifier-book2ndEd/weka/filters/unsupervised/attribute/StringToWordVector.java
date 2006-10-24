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
 *    StringToWordVector.java
 *    Copyright (C) 2002 University of Waikato
 *
 *    Updated 12/Dec/2001 by Gordon Paynter (gordon.paynter@ucr.edu)
 *                        Added parameters for delimiter set,
 *                        number of words to add, and input range.
 *    updated 27/Nov/2003 by Asrhaf M. Kibriya (amk14@cs.waikato.ac.nz)
 *                        Added options for TF/IDF transforms, length 
 *                        normalization and down casing the tokens. Also 
 *                        added another onlyAlphabeticStringTokenizer and
 *                        support for using a list of stopwords.
 */


package weka.filters.unsupervised.attribute;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;


/** 
 * Converts String attributes into a set of attributes representing word
 * occurrence information from the text contained in the strings. The set of
 * words (attributes) is determined by the first batch filtered (typically
 * training data).
 *
 * @author Len Trigg (len@reeltwo.com)
 * @author Stuart Inglis (stuart@reeltwo.com)
 * @version $Revision: 1.8 $ 
 */
public class StringToWordVector extends Filter
  implements UnsupervisedFilter, OptionHandler {

  /** Delimiters used in tokenization */
  private String delimiters = " \n\t.,:'\"()?!";

  /** Range of columns to convert to word vectors */
  protected Range m_SelectedRange = null;

  /** Contains a mapping of valid words to attribute indexes */
  private TreeMap m_Dictionary = new TreeMap();
  
  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /** True if output instances should contain word frequency rather than boolean 0 or 1. */
  private boolean m_OutputCounts = false;

  /** A String prefix for the attribute names */
  private String m_Prefix = "";
  
  /** Contains the number of documents (instances) a particular word appears in.
      The counts are stored with the same indexing as given by m_Dictionary.  */
  private int [] docsCounts;
  
  /** Contains the number of documents (instances) in the input format from 
      which the dictionary is created. It is used in IDF transform. */
  private int numInstances = -1;

  /**
   * Contains the average length of documents (among the first batch of 
   * instances aka training data). This is used in length normalization of 
   * documents which will be normalized to average document length.
   */
  private double avgDocLength = -1;
  
  /**
   * The default number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   */
  private int m_WordsToKeep = 1000;

  /** True if word frequencies should be transformed into log(1+fi) 
      where fi is the frequency of word i
   */
  private boolean m_TFTransform;
  
  /** True if document's (instance's) word frequencies are to be normalized. 
      The are normalized to average length of documents specified as input 
      format. */
  private boolean m_normalizeDocLength;
  
  /** True if word frequencies should be transformed into 
     fij*log(numOfDocs/numOfDocsWithWordi) */
  private boolean m_IDFTransform;
  
  /** True if tokens are to be formed only from alphabetic sequences of 
      characters. (The delimiters string property is ignored if this
      is true).
   */
  private boolean m_onlyAlphabeticTokens;  
  
  /** True if all tokens should be downcased */
  private boolean m_lowerCaseTokens;
  
  /** True if tokens that are on a stoplist are to be ignored. */
  private boolean m_useStoplist;  
  
  
  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
				    "\tOutput word counts rather than boolean word presence.\n",
				    "C", 0, "-C"));
    newVector.addElement(new Option(
				    "\tString containing the set of delimiter characters\n"
				    + "\t(default: \" \\n\\t.,:'\\\"()?!\")",
				    "D", 1, "-D <delimiter set>"));
    newVector.addElement(new Option(
				    "\tSpecify list of string attributes to convert to words (as weka Range).\n"
				    + "\t(default: select all string attributes)",
				    "R", 1, "-R <index1,index2-index4,...>"));
    newVector.addElement(new Option(
				    "\tSpecify a prefix for the created attribute names.\n"
				    + "\t(default: \"\")",
				    "P", 1, "-P <attribute name prefix>"));
    newVector.addElement(new Option(
				    "\tSpecify approximate number of word fields to create.\n"
				    + "\tSurplus words will be discarded..\n"
				    + "\t(default: 1000)",
				    "W", 1, "-W <number of words to keep>"));
    newVector.addElement(new Option(
				    "\tTransform the word frequencies into log(1+fij)\n"+
                                    "\twhere fij is the frequency of word i in jth "+
                                    "document(instance).\n",
				    "T", 0, "-T"));
    newVector.addElement(new Option(
				    "\tTransform each word frequency into:\n"+
                                    "\tfij*log(num of Documents/num of "+
                                    " documents containing word i)\n"+
                                    "\t  where fij if frequency of word i in "+
                                    " jth document(instance)",
				    "I", 0, "-I"));
    newVector.addElement(new Option(
				    "\tNormalize word frequencies of each "+
                                    "document(instance) to average length of "+
                                    "documents.",
				    "N", 0, "-N"));
    newVector.addElement(new Option(
				    "\tOnly form tokens from contiguous "+
                                    "alphabetic sequences (The delimiter "+
                                    "string is ignored if this is set).",
				    "A", 0, "-A"));
    newVector.addElement(new Option(
				    "\tConvert all tokens to lowercase before "+
                                    "adding to the dictionary.",
				    "L", 0, "-L"));
    newVector.addElement(new Option(
				    "\tIgnore words that are in the stoplist.",
				    "S", 0, "-S"));
        

    return newVector.elements();
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -C<br>
   * Output word counts rather than boolean word presence.<p>
   * 
   * -D delimiter_charcters <br>
   * Specify set of delimiter characters
   * (default: " \n\t.,:'\\\"()?!\"<p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of string attributes to convert to words.
   * (default: all string attributes)<p>
   *
   * -P attribute_name_prefix <br>
   * Specify a prefix for the created attribute names.
   * (default: "")<p>
   *
   * -W number_of_words_to_keep <br>
   * Specify number of word fields to create.
   * Other, less useful words will be discarded.
   * (default: 1000)<p>
   *
   * -A <br>
   * Only tokenize contiguous alphabetic sequences. <p>
   *
   * -L <br>
   * Convert all tokens to lower case before adding to the dictionary. <p>
   *
   * -S <br>
   * Do not add words to the dictionary which are on the stop list. <p>
   *
   * -T <br>
   * Transform word frequencies to log(1+fij) where fij is frequency of word i 
   * in document j. <p>
   *
   * -I <br>
   * Transform word frequencies to fij*log(numOfDocs/numOfDocsWithWordi)
   * where fij is frequency of word i in document j. <p>
   *
   * -N <br>
   * Normalize word frequencies for each document(instance). The frequencies
   * are normalized to average length of the documents specified in input 
   * format. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String value = Utils.getOption('D', options);
    if (value.length() != 0) {
      setDelimiters(value);
    }

    value = Utils.getOption('R', options);
    if (value.length() != 0) {
      setSelectedRange(value);
    }

    value = Utils.getOption('P', options);
    if (value.length() != 0) {
      setAttributeNamePrefix(value);
    }

    value = Utils.getOption('W', options);
    if (value.length() != 0) {
      setWordsToKeep(Integer.valueOf(value).intValue());
    }
    
    setOutputWordCounts(Utils.getFlag('C', options));

    setTFTransform(Utils.getFlag('T',  options));
    
    setIDFTransform(Utils.getFlag('I', options));
    
    setNormalizeDocLength(Utils.getFlag('N', options));
    
    setLowerCaseTokens(Utils.getFlag('L', options));
    
    setOnlyAlphabeticTokens(Utils.getFlag('A', options));
    
    setUseStoplist(Utils.getFlag('S', options));
    
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [16];
    int current = 0;

    options[current++] = "-D"; 
    options[current++] = getDelimiters();

    if (getSelectedRange() != null) {
      options[current++] = "-R"; 
      m_SelectedRange.setUpper(getInputFormat().numAttributes() - 1);
      options[current++] = getSelectedRange().getRanges();
    }

    if (!"".equals(getAttributeNamePrefix())) {
      options[current++] = "-P"; 
      options[current++] = getAttributeNamePrefix();
    }

    options[current++] = "-W"; 
    options[current++] = String.valueOf(getWordsToKeep());

    if (getOutputWordCounts()) {
      options[current++] = "-C";
    }

    if(getTFTransform())
        options[current++] = "-T";
    
    if(getIDFTransform())
        options[current++] = "-I";
    
    if(getNormalizeDocLength())
        options[current++] = "-N";
    
    if(this.getLowerCaseTokens())
        options[current++] = "-L";
    
    if(this.getOnlyAlphabeticTokens())
        options[current++] = "-A";
    
    if(this.getUseStoplist())
        options[current++] = "-S";
    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Default constructor. Targets 1000 words in the output.
   */
  public StringToWordVector() {
  }

  /**
   * Constructor that allows specification of the target number of words
   * in the output.
   *
   * @param wordsToKeep the number of words in the output vector (per class
   * if assigned).
   */
  public StringToWordVector(int wordsToKeep) {
    m_WordsToKeep = wordsToKeep;
  }
  
  /** 
   * Used to store word counts for dictionary selection based on 
   * a threshold.
   */
  private class Count implements Serializable {
    public int count, docCount;
    public Count(int c) { count = c; }
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
    throws Exception {
    super.setInputFormat(instanceInfo);
    m_FirstBatchDone = false;
    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance.
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input structure has been defined.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_FirstBatchDone) {
      convertInstance(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output.
   * @exception IllegalStateException if no input structure has been defined.
   */
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // Determine the dictionary
    if (!m_FirstBatchDone) {
      determineDictionary();
    }

    // Convert pending input instances.
    if(this.m_normalizeDocLength==false || m_FirstBatchDone==true) {
      for(int i = 0; i < getInputFormat().numInstances(); i++) {
          convertInstance(getInputFormat().instance(i));
      }
      flushInput();
    }
    else {
      FastVector fv = new FastVector();
      int firstCopy=0;
      Instances inputFormat = getInputFormat();
      avgDocLength = 0;
      for(int i=0; i<inputFormat.numInstances(); i++)
          firstCopy = convertInstancewoDocNorm(inputFormat.instance(i), fv);
      
      //Now normalizing document length
      for(int i=0; i<fv.size(); i++) {
        
        Instance inst = (Instance) fv.elementAt(i);
        
        double docLength = 0;
        double val=0;
        for(int j=0; j<inst.numValues(); j++) {
          if(inst.index(j)>=firstCopy) {
            val = inst.valueSparse(j);
            docLength += val*val;
          }
        }        
        docLength = Math.sqrt(docLength);
        avgDocLength += docLength;
        for(int j=0; j<inst.numValues(); j++) {
          if(inst.index(j)>=firstCopy) {
            val = inst.valueSparse(j);
            val /= docLength;
//            if(i==0)
//              System.err.println("Instance "+i+
//              ": "+
//              "length: "+docLength+
//              " setting value "+inst.index(j)+
//              " from "+inst.valueSparse(j)+
//              " to "+val);
            inst.setValueSparse(j, val);
            if(val==0){
              System.err.println("setting value "+inst.index(j)+" to zero.");
              j--;
            }
          }
        }
        
      }
      avgDocLength /= inputFormat.numInstances();
      
      for(int i=0; i<fv.size(); i++) {
        Instance inst = (Instance) fv.elementAt(i);
        double val=0;
        for(int j=0; j<inst.numValues(); j++) {
          if(inst.index(j)>=firstCopy) {
            val = inst.valueSparse(j);
            val = val * avgDocLength;
//            if(i==0)
//              System.err.println("Instance "+i+
//              ": "+
//              "avgDocLength: "+avgDocLength+
//              " setting value "+inst.index(j)+
//              " from "+inst.valueSparse(j)+
//              " to "+val);            
            inst.setValueSparse(j, val);
            if(val==0) {
              System.err.println("setting value "+inst.index(j)+" to zero.");
              j--;
            }
          }
        }
        push(inst);
      }
      flushInput();
    }

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns a string describing this filter
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */  
  public String globalInfo() {
    return "Converts String attributes into a set of attributes representing "+
           "word occurrence information from the text contained in the "+
           "strings. The set of words (attributes) is determined by the first "+
           "batch filtered (typically training data).";
  }  
  
  /**
   * Gets whether output instances contain 0 or 1 indicating word
   * presence, or word counts.
   *
   * @return true if word counts should be output.
   */
  public boolean getOutputWordCounts() {
    return m_OutputCounts;
  }

  /**
   * Sets whether output instances contain 0 or 1 indicating word
   * presence, or word counts.
   *
   * @param outputWordCounts true if word counts should be output.
   */
  public void setOutputWordCounts(boolean outputWordCounts) {
    m_OutputCounts = outputWordCounts;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String outputWordCountsTipText() {
      return "Output word counts rather than boolean 0 or 1"+
             "(indicating presence or absence of a word).";
  }

  /**
   * Get the value of delimiters.
   *
   * @return Value of delimiters.
   */
  public String getDelimiters() {
    return delimiters;
  }
    
  /**
   * Set the value of delimiters.
   *
   * @param newdelimiters Value to assign to delimiters.
   */
  public void setDelimiters(String newDelimiters) {
    delimiters = newDelimiters;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String delimitersTipText() {
      return "Set of delimiter characters to use in tokenizing "+
             "(default: \" \\n\\t.,:'\\\"()?!\"). "+
             "This option is ignored if onlyAlphabeticTokens option is set to"+
             " true.";
  }

  /**
   * Get the value of m_SelectedRange.
   *
   * @return Value of m_SelectedRange.
   */
  public Range getSelectedRange() {
    return m_SelectedRange;
  }
    
  /**
   * Set the value of m_SelectedRange.
   *
   * @param newSelectedRange Value to assign to m_SelectedRange.
   */
  public void setSelectedRange(String newSelectedRange) {
    m_SelectedRange = new Range(newSelectedRange);
  }

  /**
   * Get the attribute name prefix.
   *
   * @return The current attribute name prefix.
   */
  public String getAttributeNamePrefix() {
    return m_Prefix;
  }
    
  /**
   * Set the attribute name prefix.
   *
   * @param newPrefix String to use as the attribute name prefix.
   */
  public void setAttributeNamePrefix(String newPrefix) {
    m_Prefix = newPrefix;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeNamePrefixTipText() {
      return "Prefix for the created attribute names. "+
             "(default: \"\")";
  }

  /**
   * Gets the number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   *
   * @return the target number of words in the output vector (per class if
   * assigned).
   */
  public int getWordsToKeep() {
    return m_WordsToKeep;
  }
  
  /**
   * Sets the number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   *
   * @param newWordsToKeep the target number of words in the output 
   * vector (per class if assigned).
   */
  public void setWordsToKeep(int newWordsToKeep) {
    m_WordsToKeep = newWordsToKeep;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String wordsToKeepTipText() {
      return "The number of words (per class if there is a class attribute "+
             "assigned) to attempt to keep.";
  }

  /** Gets whether if the word frequencies should be transformed into
   *  log(1+fij) where fij is the frequency of word i in document(instance) j.
   *
   * @return true if word frequencies are to be transformed.
   */
  public boolean getTFTransform() {
      return this.m_TFTransform;
  }
  
  /** Sets whether if the word frequencies should be transformed into
   *  log(1+fij) where fij is the frequency of word i in document(instance) j.
   *
   * @param true if word frequencies are to be transformed.
   */
  public void setTFTransform(boolean TFTransform) {
      this.m_TFTransform = TFTransform;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String TFTransformTipText() {
      return "Sets whether if the word frequencies should be transformed into:\n "+
             "   log(1+fij) \n"+
             "       where fij is the frequency of word i in document (instance) j.";
  }
  
  /** Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   *      where fij is the frequency of word i in document(instance) j.
   *
   * @return true if the word frequencies are to be transformed.
   */
  public boolean getIDFTransform() {
      return this.m_IDFTransform;
  }
  
  /** Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   *      where fij is the frequency of word i in document(instance) j.
   *
   * @param true if the word frequecies are to be transformed
   */
  public void setIDFTransform(boolean IDFTransform) {
      this.m_IDFTransform = IDFTransform;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String IDFTransformTipText() {
      return "Sets whether if the word frequencies in a document should be "+
             "transformed into: \n"+
             "   fij*log(num of Docs/num of Docs with word i) \n"+
             "      where fij is the frequency of word i in document (instance) j.";
  }


  /** Gets whether if the word frequencies for a document (instance) should
   *  be normalized or not.
   *
   * @return true if word frequencies are to be normalized.
   */
  public boolean getNormalizeDocLength() {
      return this.m_normalizeDocLength;
  }
  
  /** Sets whether if the word frequencies for a document (instance) should
   *  be normalized or not.
   *
   * @param true if word frequencies are to be normalized.
   */
  public void setNormalizeDocLength(boolean normalizeDocLength) {
      this.m_normalizeDocLength = normalizeDocLength;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeDocLengthTipText() {
      return "Sets whether if the word frequencies for a document (instance) "+
             "should be normalized or not.";
  }
  
  /** Gets whether if the tokens are to be formed only from contiguous 
   *  alphabetic sequences. The delimiter string is ignored if this is true.
   *
   * @return true if tokens are to be formed from contiguous alphabetic 
   * characters.
   */
  public boolean getOnlyAlphabeticTokens() {
      return m_onlyAlphabeticTokens;
  }  
  
  /** Sets whether if tokens are to be formed only from contiguous alphabetic
   * character sequences. The delimiter string is ignored if this option is 
   * set to true.
   *
   * @param onlyAlphabeticSequences should be set to true if only alphabetic 
   * tokens should be formed.
   */
  public void setOnlyAlphabeticTokens(boolean tokenizeOnlyAlphabeticSequences) {
      m_onlyAlphabeticTokens = tokenizeOnlyAlphabeticSequences;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String onlyAlphabeticTokensTipText() {
      return "Sets whether if the word tokens are to be formed only from "+
             "contiguous alphabetic sequences (The delimiter string is "+
             "ignored if this option is set to true).";
  }
  
  /** Gets whether if the tokens are to be downcased or not.
   *
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
      return this.m_lowerCaseTokens;
  }
  
  /** Sets whether if the tokens are to be downcased or not. (Doesn't affect
   * non-alphabetic characters in tokens).
   *
   * @param downCaseTokens should be true if only lower case tokens are 
   * to be formed.
   */
  public void setLowerCaseTokens(boolean downCaseTokens) {
      this.m_lowerCaseTokens = downCaseTokens;
  }
  
  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lowerCaseTokensTipText() {
      return "If set then all the word tokens are converted to lower case "+
             "before being added to the dictionary.";
  }

  /** Gets whether if the words on the stoplist are to be ignored (The stoplist
   *  is in weka.core.StopWords).
   *
   * @return true if the words on the stoplist are to be ignored.
   */
  public boolean getUseStoplist() {
      return m_useStoplist;
  }  
  
  /** Sets whether if the words that are on a stoplist are to be ignored (The
   * stop list is in weka.core.StopWords).
   *
   * @param useStoplist true if the tokens that are on a stoplist are to be 
   * ignored.
   */
  public void setUseStoplist(boolean useStoplist) {
      m_useStoplist = useStoplist;
  }  
  
  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useStoplistTipText() {
      return "Ignores all the words that are on the stoplist, if set to true.";
  } 
  
  private static void sortArray(int [] array) {
      
    int i, j, h, N = array.length - 1;
	
    for (h = 1; h <= N / 9; h = 3 * h + 1); 
	
    for (; h > 0; h /= 3) {
      for (i = h + 1; i <= N; i++) { 
        int v = array[i]; 
        j = i; 
        while (j > h && array[j - h] > v ) { 
          array[j] = array[j - h]; 
          j -= h; 
        } 
        array[j] = v; 
      } 
    }
  }

  private void determineSelectedRange() {
    
    Instances inputFormat = getInputFormat();
    
    // Calculate the default set of fields to convert
    if (m_SelectedRange == null) {
      StringBuffer fields = new StringBuffer();
      for (int j = 0; j < inputFormat.numAttributes(); j++) { 
	if (inputFormat.attribute(j).type() == Attribute.STRING)
	  fields.append((j + 1) + ",");
      }
      m_SelectedRange = new Range(fields.toString());
    }
    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);
    
    // Prevent the user from converting non-string fields
    StringBuffer fields = new StringBuffer();
    for (int j = 0; j < inputFormat.numAttributes(); j++) { 
      if (m_SelectedRange.isInRange(j) 
	  && inputFormat.attribute(j).type() == Attribute.STRING)
	fields.append((j + 1) + ",");
    }
    m_SelectedRange.setRanges(fields.toString());
    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);

    // System.err.println("Selected Range: " + getSelectedRange().getRanges()); 
  }
  
  private void determineDictionary() {
    
    // System.err.println("Creating dictionary"); 
    
    int classInd = getInputFormat().classIndex();
    int values = 1;
    if (classInd != -1) {
      values = getInputFormat().attribute(classInd).numValues();
    }

    //TreeMap dictionaryArr [] = new TreeMap[values];
    TreeMap [] dictionaryArr = new TreeMap[values];
    for (int i = 0; i < values; i++) {
      dictionaryArr[i] = new TreeMap();
    }

    // Make sure we know which fields to convert
    determineSelectedRange();

    // Tokenize all training text into an orderedMap of "words".
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      /*
	if (i % 10 == 0) {
        System.err.print( i + " " + getInputFormat().numInstances() + "\r"); 
        System.err.flush();
	}
      */
      Instance instance = getInputFormat().instance(i);
      int vInd = 0;
      if (classInd != -1) {
          vInd = (int)instance.classValue();
      }
      
      Hashtable h = new Hashtable();
      for (int j = 0; j < instance.numAttributes(); j++) { 
        if (m_SelectedRange.isInRange(j) && (instance.isMissing(j) == false)) {
	  //getInputFormat().attribute(j).type() == Attribute.STRING 
            
          Enumeration st;
          if(this.m_onlyAlphabeticTokens==false)
              st = new StringTokenizer(instance.stringValue(j),
                                                   delimiters);
          else
              st = new AlphabeticStringTokenizer(instance.stringValue(j));
          
          while (st.hasMoreElements()) {
            String word = ((String)st.nextElement()).intern();
            
            if(this.m_lowerCaseTokens==true)
                word = word.toLowerCase();
            
            if(this.m_useStoplist==true)
                if(weka.core.Stopwords.isStopword(word))
                    continue;
            
            if(!(h.contains(word)))
                h.put(word, new Integer(0));

            Count count = (Count)dictionaryArr[vInd].get(word);
            if (count == null) {
              dictionaryArr[vInd].put(word, new Count(1));
            } else {
                count.count ++;                
            }
          }          
        }
      }
      //updating the docCount for the words that have occurred in this
      //instance(document).
      Enumeration e = h.keys();
      while(e.hasMoreElements()) {
          String word = (String) e.nextElement();
          Count c = (Count)dictionaryArr[vInd].get(word);
      
          if(c!=null) {
              c.docCount++;
          }
          else 
              System.err.println("Warning: A word should definitely be in the "+
                                 "dictionary.Please check the code");
      }
      
    }

    int totalsize = 0;
    int prune[] = new int[values];
    for (int z = 0; z < values; z++) {
      totalsize += dictionaryArr[z].size();

      int array[] = new int[dictionaryArr[z].size()];
      int pos = 0;
      Iterator it = dictionaryArr[z].keySet().iterator();
      while (it.hasNext()) {
        String word = (String)it.next();
        Count count = (Count)dictionaryArr[z].get(word);
        array[pos] = count.count;
        pos++;
      }

      // sort the array
      sortArray(array);
      if (array.length < m_WordsToKeep) {
        // if there aren't enough words, set the threshold to 1
        prune[z] = 1;
      } else {
        // otherwise set it to be at least 1
        prune[z] = Math.max(1, array[array.length - m_WordsToKeep]);
      }

    }

    /*
      for (int z=0;z<values;z++) {
      System.err.println(dictionaryArr[z].size()+" "+totalsize);
      }
    */

    // Convert the dictionary into an attribute index
    // and create one attribute per word
    FastVector attributes = new FastVector(totalsize +
					   getInputFormat().numAttributes());

    // Add the non-converted attributes 
    int classIndex = -1;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (!m_SelectedRange.isInRange(i)) { 
        if (getInputFormat().classIndex() == i) {
          classIndex = attributes.size();
        }
	attributes.addElement(getInputFormat().attribute(i).copy());
      }     
    }

    
    // Add the word vector attributes
    TreeMap newDictionary = new TreeMap();

    int index = attributes.size();
    for(int z = 0; z < values; z++) {
      /*
	System.err.print("\nCreating word index...");
	if (values > 1) {
        System.err.print(" for class id=" + z); 
	}
	System.err.flush();
      */
      Iterator it = dictionaryArr[z].keySet().iterator();
      while (it.hasNext()) {
        String word = (String)it.next();
        Count count = (Count)dictionaryArr[z].get(word);
        if (count.count >= prune[z]) {
          //          System.err.println(word+" "+newDictionary.get(word));
          if(newDictionary.get(word) == null) {
            /*
	      if (values > 1) {
              System.err.print(getInputFormat().classAttribute().value(z) + " ");
	      }
	      System.err.println(word);
            */
            newDictionary.put(word, new Integer(index++));
            attributes.addElement(new Attribute(m_Prefix + word));
          }
        }
      }
    }
    
    
    
    docsCounts = new int[attributes.size()];
    Iterator it = newDictionary.keySet().iterator();
    while(it.hasNext()) {
        String word = (String) it.next();
        int idx = ((Integer)newDictionary.get(word)).intValue();
        int docsCount=0;
        for(int j=0; j<values; j++) {
            Count c = (Count) dictionaryArr[j].get(word);
            if(c!=null)
                docsCount += c.docCount;
        }
        docsCounts[idx]=docsCount;
    }
    
    attributes.trimToSize();
    m_Dictionary = newDictionary;

    //System.err.println("done: " + index + " words in total.");
    
    numInstances = getInputFormat().numInstances();
    
    // Set the filter's output format
    Instances outputFormat = new Instances(getInputFormat().relationName(), 
                                           attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);
  }


  private void convertInstance(Instance instance) throws Exception {

    // Convert the instance into a sorted set of indexes
    TreeMap contained = new TreeMap();

    // Copy all non-converted attributes from input to output
    int firstCopy = 0;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (!m_SelectedRange.isInRange(i)) { 
	if (getInputFormat().attribute(i).type() != Attribute.STRING) {
	  // Add simple nominal and numeric attributes directly
	  if (instance.value(i) != 0.0) {
	    contained.put(new Integer(firstCopy), 
			  new Double(instance.value(i)));
	  } 
	} else {
	  if (instance.isMissing(i)) {
	    contained.put(new Integer(firstCopy),
			  new Double(Instance.missingValue()));
	  } else {

	    // If this is a string attribute, we have to first add
	    // this value to the range of possible values, then add
	    // its new internal index.
	    if (outputFormatPeek().attribute(firstCopy).numValues() == 0) {
	      // Note that the first string value in a
	      // SparseInstance doesn't get printed.
	      outputFormatPeek().attribute(firstCopy)
		.addStringValue("Hack to defeat SparseInstance bug");
	    }
	    int newIndex = outputFormatPeek().attribute(firstCopy)
	      .addStringValue(instance.stringValue(i));
	    contained.put(new Integer(firstCopy), 
			  new Double(newIndex));
	  }
	}
	firstCopy++;
      }     
    }
    
    for (int j = 0; j < instance.numAttributes(); j++) { 
      //if ((getInputFormat().attribute(j).type() == Attribute.STRING) 
      if (m_SelectedRange.isInRange(j)
	  && (instance.isMissing(j) == false)) {          
        Enumeration st;
        
        if(this.m_onlyAlphabeticTokens==false)
            st = new StringTokenizer(instance.stringValue(j),
                                                 delimiters);
        else
            st = new AlphabeticStringTokenizer(instance.stringValue(j));
        
        while (st.hasMoreElements()) {
          String word = (String)st.nextElement(); 
          if(this.m_lowerCaseTokens==true)
              word = word.toLowerCase();
          Integer index = (Integer) m_Dictionary.get(word);
          if (index != null) {
            if (m_OutputCounts) { // Separate if here rather than two lines down to avoid hashtable lookup
              Double count = (Double)contained.get(index);
              if (count != null) {
                contained.put(index, new Double(count.doubleValue() + 1.0));
              } else {
                contained.put(index, new Double(1));
              }
            } else {
              contained.put(index, new Double(1));
            }                
          }
        }
      }
    }
    
    //Doing TFTransform
    if(m_TFTransform==true) {
        Iterator it = contained.keySet().iterator();
        for(int i=0; it.hasNext(); i++) {
            Integer index = (Integer)it.next();
            if( index.intValue() >= firstCopy ) { 
                double val = ((Double)contained.get(index)).doubleValue();
                val = Math.log(val+1);
//                if(printInstance==true)
//                  System.out.println("Instance 0"+ //instance.toString()+
//                                   ": setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);                
                contained.put(index, new Double(val));
            }
        }
    }
    
    //Doing IDFTransform
    if(m_IDFTransform==true) {
        Iterator it = contained.keySet().iterator();
        for(int i=0; it.hasNext(); i++) {
            Integer index = (Integer)it.next();
            //int num = getInputFormat().numInstances();
            if( index.intValue() >= firstCopy ) {
                double val = ((Double)contained.get(index)).doubleValue();
                val = val*Math.log( numInstances /    //num /
                                    (double)docsCounts[index.intValue()] );
//                if(printInstance==true)
//                  System.out.println("Instance 0"+ //instance.toString()+
//                                   ": "+
//                                   "num: "+numInstances+" index.intValue(): "+index.intValue()+
//                                   " docsCounts: "+this.docsCounts[index.intValue()]+ //"\n"+
//                                   "setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);                
                contained.put(index, new Double(val));
            }
        }        
    }
    
    //Doing length normalization
    if(m_normalizeDocLength==true) {
      if(avgDocLength<0)
        throw new Exception("Error. Average Doc Length not defined yet.");
      
      double sumSq = 0;
      Iterator it = contained.keySet().iterator();
      for(int i=0; it.hasNext(); i++) {
        Integer index = (Integer)it.next();
        if( index.intValue() >= firstCopy ) {
          double val = ((Double)contained.get(index)).doubleValue();
          sumSq += val*val;
        }
      }
      it = contained.keySet().iterator();
      for(int i=0; it.hasNext(); i++) {
        Integer index = (Integer)it.next();
        if( index.intValue() >= firstCopy ) {
          double val = ((Double)contained.get(index)).doubleValue();
          val = val/Math.sqrt(sumSq);
          val = val*avgDocLength;
//                System.out.println("Instance "+instance.toString()+
//                                   ": setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);
          contained.put(index, new Double(val));
        }
      }
    }
    
    
    // Convert the set to structures needed to create a sparse instance.
    double [] values = new double [contained.size()];
    int [] indices = new int [contained.size()];
    Iterator it = contained.keySet().iterator();
    for (int i = 0; it.hasNext(); i++) {
      Integer index = (Integer)it.next();
      Double value = (Double)contained.get(index);
      values[i] = value.doubleValue();
      indices[i] = index.intValue();
    }

    Instance inst = new SparseInstance(instance.weight(), values, indices, 
                                       outputFormatPeek().numAttributes());
    inst.setDataset(outputFormatPeek());
    push(inst);
    
    //System.err.print("#"); System.err.flush();
  }


  private int convertInstancewoDocNorm(Instance instance, FastVector v) {

    // Convert the instance into a sorted set of indexes
    TreeMap contained = new TreeMap();

    // Copy all non-converted attributes from input to output
    int firstCopy = 0;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (!m_SelectedRange.isInRange(i)) { 
	if (getInputFormat().attribute(i).type() != Attribute.STRING) {
	  // Add simple nominal and numeric attributes directly
	  if (instance.value(i) != 0.0) {
	    contained.put(new Integer(firstCopy), 
			  new Double(instance.value(i)));
	  } 
	} else {
	  if (instance.isMissing(i)) {
	    contained.put(new Integer(firstCopy),
			  new Double(Instance.missingValue()));
	  } else {

	    // If this is a string attribute, we have to first add
	    // this value to the range of possible values, then add
	    // its new internal index.
	    if (outputFormatPeek().attribute(firstCopy).numValues() == 0) {
	      // Note that the first string value in a
	      // SparseInstance doesn't get printed.
	      outputFormatPeek().attribute(firstCopy)
		.addStringValue("Hack to defeat SparseInstance bug");
	    }
	    int newIndex = outputFormatPeek().attribute(firstCopy)
	      .addStringValue(instance.stringValue(i));
	    contained.put(new Integer(firstCopy), 
			  new Double(newIndex));
	  }
	}
	firstCopy++;
      }     
    }
    
    for (int j = 0; j < instance.numAttributes(); j++) { 
      //if ((getInputFormat().attribute(j).type() == Attribute.STRING) 
      if (m_SelectedRange.isInRange(j)
	  && (instance.isMissing(j) == false)) {          
        Enumeration st;
        
        if(this.m_onlyAlphabeticTokens==false)
            st = new StringTokenizer(instance.stringValue(j),
                                                 delimiters);
        else
            st = new AlphabeticStringTokenizer(instance.stringValue(j));
        
        while (st.hasMoreElements()) {
          String word = (String)st.nextElement(); 
          if(this.m_lowerCaseTokens==true)
              word = word.toLowerCase();
          Integer index = (Integer) m_Dictionary.get(word);
          if (index != null) {
            if (m_OutputCounts) { // Separate if here rather than two lines down to avoid hashtable lookup
              Double count = (Double)contained.get(index);
              if (count != null) {
                contained.put(index, new Double(count.doubleValue() + 1.0));
              } else {
                contained.put(index, new Double(1));
              }
            } else {
              contained.put(index, new Double(1));
            }                
          }
        }
      }
    }
    
    //Doing TFTransform
    if(m_TFTransform==true) {
        Iterator it = contained.keySet().iterator();
        for(int i=0; it.hasNext(); i++) {
            Integer index = (Integer)it.next();
            if( index.intValue() >= firstCopy ) { 
                double val = ((Double)contained.get(index)).doubleValue();
                val = Math.log(val+1);
//                if(printInstance==true)
//                  System.out.println("Instance 0"+ //instance.toString()+
//                                   ": setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);                
                contained.put(index, new Double(val));
            }
        }
    }
    
    //Doing IDFTransform
    if(m_IDFTransform==true) {
        Iterator it = contained.keySet().iterator();
        for(int i=0; it.hasNext(); i++) {
            Integer index = (Integer)it.next();
            //int num = getInputFormat().numInstances();
            if( index.intValue() >= firstCopy ) {
                double val = ((Double)contained.get(index)).doubleValue();
                val = val*Math.log( numInstances /    //num /
                                    (double) docsCounts[index.intValue()] );
//                if(printInstance==true)
//                  System.out.println("Instance 0"+ //instance.toString()+
//                                   ": "+
//                                   "num: "+numInstances+" index.intValue(): "+index.intValue()+
//                                   " docsCounts: "+this.docsCounts[index.intValue()]+ //"\n"+
//                                   "setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);                
                contained.put(index, new Double(val));
            }
        }        
    }
    
    //Doing length normalization
    //if(m_normalizeDocLength==true) {
    //    double sumSq = 0;
    //    Iterator it = contained.keySet().iterator();
    //    for(int i=0; it.hasNext(); i++) {
    //        Integer index = (Integer)it.next();
    //        if( index.intValue() >= firstCopy ) { 
    //            double val = ((Double)contained.get(index)).doubleValue();
    //            sumSq += val*val;
    //        }
    //    }
    //    it = contained.keySet().iterator();
    //    for(int i=0; it.hasNext(); i++) {
    //        Integer index = (Integer)it.next();
    //        if( index.intValue() >= firstCopy ) { 
    //            double val = ((Double)contained.get(index)).doubleValue();
    //            val = val/Math.sqrt(sumSq);
//                System.out.println("Instance "+instance.toString()+
//                                   ": setting value "+index.intValue()+
//                                   " from "+((Double)contained.get(index)).doubleValue()+
//                                   " to "+val);                
    //            contained.put(index, new Double(val));
    //        }
    //    }
    //}
    
    
    // Convert the set to structures needed to create a sparse instance.
    double [] values = new double [contained.size()];
    int [] indices = new int [contained.size()];
    Iterator it = contained.keySet().iterator();
    for (int i = 0; it.hasNext(); i++) {
      Integer index = (Integer)it.next();
      Double value = (Double)contained.get(index);
      values[i] = value.doubleValue();
      indices[i] = index.intValue();
    }

    Instance inst = new SparseInstance(instance.weight(), values, indices, 
                                       outputFormatPeek().numAttributes());
    inst.setDataset(outputFormatPeek());
    //push(inst);
    v.addElement(inst);
    
    return firstCopy;    
    //System.err.print("#"); System.err.flush();
  }
  
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new StringToWordVector(), argv);
      } else {
	Filter.filterFile(new StringToWordVector(), argv);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
  
  
  
  private class AlphabeticStringTokenizer implements Enumeration {
      private char[] str;
      int currentPos=0;
      
      public AlphabeticStringTokenizer(String toTokenize) {
          str = new char[toTokenize.length()];
          toTokenize.getChars(0, toTokenize.length(), str, 0);
      }
      
      public boolean hasMoreElements() {
          int beginpos = currentPos;
          
          while( beginpos < str.length && 
                 (str[beginpos]<'a' || str[beginpos]>'z') &&
                 (str[beginpos]<'A' || str[beginpos]>'Z') ) {
                     beginpos++;    
          }
          currentPos = beginpos;
          //System.out.println("Currently looking at "+str[beginpos]);
          
          if( beginpos<str.length && 
              ((str[beginpos]>='a' && str[beginpos]<='z') ||
               (str[beginpos]>='A' && str[beginpos]<='Z')) ) {
                   return true;
          }
          else
              return false;
      }
      
      public Object nextElement() {
          int beginpos, endpos;
          beginpos = currentPos;
          
          while( beginpos < str.length && 
                 (str[beginpos]<'a' && str[beginpos]>'z') &&
                 (str[beginpos]<'A' && str[beginpos]>'Z') ) {
                     beginpos++;    
          }
          currentPos = endpos = beginpos;
          
          if(beginpos>=str.length)
              throw new NoSuchElementException("no more tokens present");
          
          while( endpos < str.length && 
                 ((str[endpos]>='a' && str[endpos]<='z') ||
                  (str[endpos]>='A' && str[endpos]<='Z')) ) {                     
                     endpos++;
          }
          
          String s = new String(str, beginpos, endpos-currentPos);
          currentPos = endpos;
          //System.out.println("found token >"+s+
          //                   "< beginpos: "+beginpos+
          //                   " endpos: "+endpos+
          //                   " str.length: "+str.length+
          //                   " str[beginpos]: "+str[beginpos]);
          return s;
      }      
  }
  
}
