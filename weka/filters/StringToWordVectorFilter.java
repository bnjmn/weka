/*
 *    StringToWordVectorFilter.java
 *    Copyright (C) 2000 Webmind Inc.
 *
 */


package weka.filters;


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
import weka.core.SparseInstance;
import weka.core.Utils;

/** 
 * Converts String attributes into a set of attributes representing word
 * occurrence information from the text contained in the strings. The set of
 * words (attributes) is determined by the first batch filtered (typically
 * training data).
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @author Stuart Inglis (stuart@intelligenesis.net)
 * @version $Revision: 1.6 $ 
 **/
public class StringToWordVectorFilter extends Filter {

  /** Delimiters used in tokenization */
  private static final String DELIMS = " \n\t.,:'\"()?!";

  /** Contains a mapping of valid words to attribute indexes */
  private TreeMap m_Dictionary = new TreeMap();

  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /**
   * The default number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   */
  private int m_WordsToKeep = 1000;

  
  /**
   * Default constructor. Targets 1000 words in the output.
   */
  public StringToWordVectorFilter() {
  }

  /**
   * Constructor that allows specification of the target number of words
   * in the output.
   *
   * @param wordsToKeep the number of words in the output vector (per class
   * if assigned).
   */
  public StringToWordVectorFilter(int wordsToKeep) {
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
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    super.inputFormat(instanceInfo);
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


  private void determineDictionary() {
    
    int classInd = getInputFormat().classIndex();
    int values = 1;
    if (classInd != -1) {
      values = getInputFormat().attribute(classInd).numValues();
    }
    TreeMap dictionaryArr [] = new TreeMap[values];
    for (int i = 0; i < values; i++) {
      dictionaryArr[i] = new TreeMap();
    }

    //System.err.print("Creating dictionary\n"); System.err.flush();

    // Tokenize all training text into an orderedMap.
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      /*
      if (i % 10 == 0) {
        System.err.print( i + " " + getInputFormat().numInstances() + "\r"); 
        System.err.flush();
      }
      */
      Instance instance = getInputFormat().instance(i);
      for (int j = 0; j < instance.numAttributes(); j++) { 
        if ((getInputFormat().attribute(j).type() == Attribute.STRING) 
            && (instance.isMissing(j) == false)) {
          StringTokenizer st = new StringTokenizer(instance.stringValue(j),
                                                   DELIMS);
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
    FastVector attributes = new FastVector(totalsize);
    TreeMap newDictionary = new TreeMap();

    int index = 0;
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
            attributes.addElement(new Attribute(word));
          }
        }
      }
    }

    attributes.trimToSize();
    m_Dictionary = newDictionary;

    int classIndex = -1;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (getInputFormat().attribute(i).type() != Attribute.STRING) {
        if (getInputFormat().classIndex() == i) {
          classIndex = attributes.size();
        }
        attributes.addElement(getInputFormat().attribute(i).copy());
      }     
    }
    /*
    System.err.println("done. " + index + " words in total.");
    */
    
    Instances outputFormat = new Instances(getInputFormat().relationName(), 
                                           attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);
  }


  private void convertInstance(Instance instance) {

    // Convert the instance into a sorted set of indexes
    TreeMap contained = new TreeMap();
    for (int j = 0; j < instance.numAttributes(); j++) { 
      if ((getInputFormat().attribute(j).type() == Attribute.STRING) 
          && (instance.isMissing(j) == false)) {
        StringTokenizer st = new StringTokenizer(instance.stringValue(j),
                                                 DELIMS);
        while (st.hasMoreTokens()) {
          String word = st.nextToken();
          Integer index = (Integer) m_Dictionary.get(word);
          if (index != null) {
            contained.put(index, new Double(1));
          }
        }
      }
    }

    int firstCopy = m_Dictionary.size();
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (getInputFormat().attribute(i).type() != Attribute.STRING) {
        if (instance.value(i) != 0.0) {
          contained.put(new Integer(firstCopy), 
                        new Double(instance.value(i)));
        }
        firstCopy++;
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
 	Filter.batchFilterFile(new StringToWordVectorFilter(), argv);
      } else {
	Filter.filterFile(new StringToWordVectorFilter(), argv);
      }
    } catch (Exception ex) {
      	ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}








