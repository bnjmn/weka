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
 */


package weka.filters.unsupervised.attribute;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
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
 * @version $Revision: 1.4 $ 
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

  /**
   * The default number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   */
  private int m_WordsToKeep = 1000;

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
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [13];
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
    public int count;
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
  public boolean input(Instance instance) {

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
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // Determine the dictionary
    if (!m_FirstBatchDone) {
      determineDictionary();
    }

    // Convert pending input instances.
    for(int i = 0; i < getInputFormat().numInstances(); i++) {
      convertInstance(getInputFormat().instance(i));
    }
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
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
    
    // System.err.println("Selected Range: " + getSelectedRange().getRanges()); 
  }
  
  private void determineDictionary() {
    
    // System.err.println("Creating dictionary"); 
    
    int classInd = getInputFormat().classIndex();
    int values = 1;
    if (classInd != -1) {
      values = getInputFormat().attribute(classInd).numValues();
    }
    TreeMap dictionaryArr [] = new TreeMap[values];
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
      for (int j = 0; j < instance.numAttributes(); j++) { 
        if (m_SelectedRange.isInRange(j) && (instance.isMissing(j) == false)) {
	  //getInputFormat().attribute(j).type() == Attribute.STRING 
            
          StringTokenizer st = new StringTokenizer(instance.stringValue(j),
                                                   delimiters);
          while (st.hasMoreTokens()) {
            String word = st.nextToken().intern();
            int vInd = 0;
            if (classInd != -1) {
              vInd = (int)instance.classValue();
            }
            Count count = (Count)dictionaryArr[vInd].get(word);
            if (count == null) {
              dictionaryArr[vInd].put(word, new Count(1));
            } else {
              count.count ++;
            }
          }
        }
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

    attributes.trimToSize();
    m_Dictionary = newDictionary;

    //System.err.println("done: " + index + " words in total.");

    
    // Set the filter's output format
    Instances outputFormat = new Instances(getInputFormat().relationName(), 
                                           attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);
  }


  private void convertInstance(Instance instance) {

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
        StringTokenizer st = new StringTokenizer(instance.stringValue(j),
                                                 delimiters);
        while (st.hasMoreTokens()) {
          String word = st.nextToken();
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
}
