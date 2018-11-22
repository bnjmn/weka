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
 * SAXTransformer.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.TechnicalInformation;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * <!-- globalinfo-start -->
 * A filter to perform the Symbolic Aggregate Approximation transformation to time series.<br/>
 * <br/>
 * The filter can handle arbitrarily big alphabet sizes.<br/>
 * <br/>
 * For more information see:<br/>
 * Jessica Lin, Eamonn Keogh, Stefano Lonardi, Bill Chiu: A Symbolic Representation of Time Series, with Implications for Streaming Algorithms. In: Proceedings 8th ACM SIGMOD Workshop on Research issues in Data Mining and Knowledge Discovery , 2-11, 2003.<br/>
 * <br/>
 * It makes use of the PAATransformer filter
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;index1,index2-index3,...&gt;
 *  Specifies the attributes that should be transformed.
 *  The attributes must be relational attributes and must contain only
 *  numeric attributes which are each transformed separately.
 *  First and last are valid indices. (default Empty)</pre>
 * 
 * <pre> -V
 *  Inverts the specified attribute range (default don't invert)</pre>
 * 
 * <pre> -W &lt;CompressionFactor&gt;
 *  Specifies the compression factor w for the PAA transformation.
 *  A time series of length n will be compressed to one of length w.
 *  (default 100).</pre>
 * 
 * <pre> -A &lt;AlphabetSize&gt;
 *  Specifies the alphabet size a for the SAX transformation.
 *  The transformed data points will have discrete values.
 *  (default 10)</pre>
 * 
 * <!-- options-end -->
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class SAXTransformer extends PAATransformer {

  /** for serialization */
  private static final long serialVersionUID = 8349903480332417357L;
  
  private final static int DEFAULT_ALPHABET_SIZE = 10;

  /** the size of the alphabet for the SAX transformation */
  private int m_AlphabetSize = DEFAULT_ALPHABET_SIZE;
  
  /** the betas used for discretization */
  private double[] m_Betas = null;

  /**
   * Returns an enumeration describing the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    List<Option> options = Collections.list(super.listOptions());
    
    options.add(new Option(
        "\tSpecifies the alphabet size a for the SAX transformation.\n" +
        "\tThe transformed data points will have discrete values.\n" +
        "\t(default " + DEFAULT_ALPHABET_SIZE + ")"
        , "A", 1, "-A <AlphabetSize>"));

    return Collections.enumeration(options);
  }

  /**
   * Gets the current settings of the filter.
   */
  @Override
  public String[] getOptions() {
    
    if (m_AlphabetSize == DEFAULT_ALPHABET_SIZE)
      return super.getOptions();
    
    List<String> options = new ArrayList<String>(Arrays.asList(super.getOptions()));

    options.add("-A");
    options.add("" + m_AlphabetSize);
    
    return options.toArray(new String[0]);
    
  }

  /**
   * Parses a given list of options</p>
   * 
   * <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R &lt;index1,index2-index3,...&gt;
   *  Specifies the attributes that should be transformed.
   *  The attributes must be relational attributes and must contain only
   *  numeric attributes which are each transformed separately.
   *  First and last are valid indices. (default Empty)</pre>
   * 
   * <pre> -V
   *  Inverts the specified attribute range (default don't invert)</pre>
   * 
   * <pre> -W &lt;CompressionFactor&gt;
   *  Specifies the compression factor w for the PAA transformation.
   *  A time series of length n will be compressed to one of length w.
   *  (default 100).</pre>
   * 
   * <pre> -A &lt;AlphabetSize&gt;
   *  Specifies the alphabet size a for the SAX transformation.
   *  The transformed data points will have discrete values.
   *  (default 10)</pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String a = Utils.getOption('A', options);
    if (a.length() != 0) {
      m_AlphabetSize = Integer.parseInt(a);
    }
    if (m_AlphabetSize <= 0) {
      throw new Exception("Parameter M must be bigger than 0!");
    }
    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
    
  }
  
  /**
   * Returns the alphabet size
   * 
   * @return alphabet size
   */
  public int getAlphabetSize() {
    return m_AlphabetSize;
  }
  
  /**
   * Sets the alphabet size
   * 
   * @param alphabetSize the new alphabet size
   */
  public void setAlphabetSize(int alphabetSize) {
    m_AlphabetSize = alphabetSize;
  }
  
  /**
   * Returns the tip text for the alphabet size
   * 
   * @return the tip text
   */
  public String alphabetSizeTipText() {
    return "The alphabet size of the SAX representation";
  }

  /**
   * Returns a string describing this filter.
   */
  @Override
  public String globalInfo() {
    return
        "A filter to perform the Symbolic Aggregate Approximation " +
        "transformation to time series.\n\n" +
        "The filter can handle arbitrarily big alphabet sizes.\n\n" +
        "For more information see:\n" +
        getTechnicalInformation().toString() +
        "\n\n" +
        "It makes use of the PAATransformer filter"
    ;
  }

  /**
   * Returns the revision string.
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1000 $");
  }

  @Override
  public TechnicalInformation getTechnicalInformation() {

    TechnicalInformation result = new TechnicalInformation(TechnicalInformation.Type.INPROCEEDINGS);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Jessica Lin and Eamonn Keogh and Stefano Lonardi and Bill Chiu");
    result.setValue(TechnicalInformation.Field.TITLE, "A Symbolic Representation of Time Series, " +
            "with Implications for Streaming Algorithms");
    result.setValue(TechnicalInformation.Field.BOOKTITLE, "Proceedings 8th ACM SIGMOD Workshop on Research issues " +
                    "in Data Mining and Knowledge Discovery ");
    result.setValue(TechnicalInformation.Field.YEAR, "2003");
    result.setValue(TechnicalInformation.Field.PAGES, "2-11");
    result.setValue(TechnicalInformation.Field.PUBLISHER, "ACM");

    return result;
  }

  /**
   * Determines the output format based on the input format and returns this
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
    
    // checks whether the input format is valid
    super.determineOutputFormat(inputFormat);
    
    List<String> alphabet = generateAlphabet(m_AlphabetSize);
    m_Betas = generateBetas(m_AlphabetSize);
    
    ArrayList<Attribute> attributes = new ArrayList<Attribute>(inputFormat.numAttributes());
    for (int att = 0; att < inputFormat.numAttributes(); att++) {
      if (!m_TimeSeriesAttributes.isInRange(att)) {
        attributes.add((Attribute) inputFormat.attribute(att).copy());
        continue;
      }
      Instances timeSeries = inputFormat.attribute(att).relation().stringFreeStructure();
      ArrayList<Attribute> timeSeriesAttributes = new ArrayList<Attribute>(timeSeries.numAttributes());
      for (int i = 0; i < timeSeries.numAttributes(); i++) {
        timeSeriesAttributes.add(new Attribute(timeSeries.attribute(i).name(), alphabet));
      }
      Instances newTimeSeries = new Instances(timeSeries.relationName(), timeSeriesAttributes, 0);
      newTimeSeries.setClassIndex(timeSeries.classIndex());
      attributes.add(new Attribute(inputFormat.attribute(att).name(), newTimeSeries));
    }

    Instances outputFormat = new Instances(inputFormat.relationName(), attributes, 0);
    outputFormat.setClassIndex(inputFormat.classIndex());
    
    return outputFormat;
  }
  
  /**
   * Applies a SAX transformation to a time series.</p>
   * 
   * See: http://www.cs.ucr.edu/~eamonn/SAX.htm</p>
   * 
   * This implementation has been extended to support arbitrarily sized alphabet</p>
   * 
   * @param inputInstances the time series to be transformed
   * @param outputFormat the format of the output data
   * @return the transformed time series
   */
  @Override
  protected Instances transform(Instances inputInstances, Instances outputFormat) throws Exception {
    
    Instances outputInstances = new Instances(outputFormat, inputInstances.numInstances());
    
    // Z-normalize:
    Standardize standardize = new Standardize();
    standardize.setInputFormat(inputInstances);
    inputInstances = Filter.useFilter(inputInstances, standardize);
    
    // Apply PAA:
    inputInstances = super.transform(inputInstances, null);
    
    // discretize real values into SAX symbols
    for (int i = 0; i < inputInstances.numInstances(); i++) {
      Instance inputInstance = inputInstances.instance(i);
      double[] instance = inputInstance.toDoubleArray();
      for (int att = 0; att < inputInstances.numAttributes(); att++) {
        instance[att] = binarySearchIntoBetas(m_Betas, instance[att]);
      }
      outputInstances.add(inputInstance.copy(instance));
    }

    return outputInstances;
  }

  /**
   * Generates an alphabet of size <code>size</code>
   * 
   * @param size the size of the generated alphabet
   * @return the generated alphabet
   */
  private static final List<String> generateAlphabet(int size) {
    
    List<String> alphabet = new ArrayList<String>(size);

    int width = (int) Math.ceil(Math.log(size)/Math.log(26));
    
    for (int i = 0; i < size; i++) {
      int integer = i;
      String letter = "";
      for (int k = 1; k <= width; k++) {
        int digit = integer % 26;
        integer /= 26;
        // conversion to letter (a-z)
        letter += (char) ((int) ('a') + digit);
      }
      
      alphabet.add(new StringBuilder(letter).reverse().toString());
    }
    
    return alphabet;
  }
  
  /**
   * Generates the betas used for discretization with <code>num</code>
   * different values.
   * 
   * @param num number of different values
   * @return betas used for discretization
   */
  private static final double[] generateBetas(int num) {
    double[] betas = new double[num - 1];

    double area = 1/ (double) num;
    
    for (int i = 1; i < num; i++) {
      betas[i - 1] = Statistics.normalInverse(area*i);
    }
    
    return betas;
  }
  
  /**
   * Does a binary search in the betas to find the correct discretization
   * for <code>real</code>
   * 
   * @param betas the betas to use for the search
   * @param real the candidate to be discretized
   * @return the discretized value of <code>real</code>
   */
  private static final int binarySearchIntoBetas(double[] betas, double real) {
    
    /* Obviously a binary search will have O(log(k)) running time where k is the
     * alphabet size.
     * 
     * Since the betas have fixed boundaries it may be possible to create a
     * table for faster lookup, i.e. constant time lookup at the cost of a
     * higher memory overhead.
     */
    
    if (real <= betas[0])
      return 0;
    
    if (real > betas[betas.length - 1])
      return betas.length;
    
    int lower = 0;
    int upper = betas.length - 1;
    
    while (upper - lower > 1) {
      int mid = (lower + upper)/2;
      if (betas[mid] <= real) {
        lower = mid;
      } else {
        upper = mid;
      }
    }
    
    return upper;
  }
  
  /**
   * Main method for running this filter.
   * 
   * @param args arguments for the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new SAXTransformer(), args);
  }

}

