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
 * OneDimensionalTimeSeriesToString.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.RelationalLocator;
import weka.core.SparseInstance;
import weka.core.StringLocator;
import weka.core.Utils;
import weka.filters.SimpleStreamFilter;

/**
 * <!-- globalinfo-start -->
 * A filter to concatenate the string representation of each data point of a one dimensional time series.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;index1,index2-index3,...&gt;
 *  Specifies the attributes that should be transformed.
 *  The attributes must be relational attributes and must contain only
 *  one attribute.
 *  First and last are valid indices. (default Empty)</pre>
 * 
 * <pre> -V
 *  Inverts the specified attribute range (default don't invert)</pre>
 * 
 * <!-- options-end -->
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class OneDimensionalTimeSeriesToString extends SimpleStreamFilter {

  /** for serialization */
  private static final long serialVersionUID = -5844666358579212500L;

  /** the default attributes that should be transformed */
  private static final Range getDefaultAttributes() {
    return new Range();
  }
  
  /** which attributes to transform (must be time series) */
  private Range m_TimeSeriesAttributes = 
      new Range(getDefaultAttributes().getRanges());

  /** attribute locations of untouched strings */
  private StringLocator m_StringAttributes = null;
  
  /** attribute locations of untouched relations */
  private RelationalLocator m_RelationalAttributes = null;

  /**
   * Returns the Capabilities of this filter.    
   */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Returns an enumeration describing the available options. 
   */
  @Override
  public Enumeration<Option> listOptions() {
    List<Option> options = new ArrayList<Option>(2);

    options.add(new Option(
        "\tSpecifies the attributes that should be transformed.\n" +
        "\tThe attributes must be relational attributes and must contain only\n" +
        "\tone attribute.\n" +
        "\tFirst and last are valid indices. (default "
        + getDefaultAttributes() + ")"
        , "R", 1, "-R <index1,index2-index3,...>"));

    options.add(new Option(
        "\tInverts the specified attribute range (default don't invert)"
        , "V", 0, "-V"));

    return Collections.enumeration(options);
  }
  
  /**
   * Gets the current settings of the filter.
   */
  @Override
  public String[] getOptions() {
    
    List<String> options = new ArrayList<String>();
    
    if (!m_TimeSeriesAttributes.equals(getDefaultAttributes())) {
      options.add("-R");
      options.add(m_TimeSeriesAttributes.getRanges());
    }
    
    if (m_TimeSeriesAttributes.getInvert()) {
      options.add("-V");
    }
    
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
   *  one attribute.
   *  First and last are valid indices. (default Empty)</pre>
   * 
   * <pre> -V
   *  Inverts the specified attribute range (default don't invert)</pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    
    String timeSeriesAttributes = Utils.getOption('R', options);
    if (timeSeriesAttributes.length() != 0)
      m_TimeSeriesAttributes = new Range(timeSeriesAttributes);
    
    if (Utils.getFlag('V', options))
      m_TimeSeriesAttributes.setInvert(true);
    
    if (getInputFormat() != null)
      setInputFormat(getInputFormat());
    
    Utils.checkForRemainingOptions(options);
  }
 
  /**
   * Gets the attribute ranges to which the filter should be applied
   * 
   * @return the attribute ranges
   */
  public String getRange() {
    return m_TimeSeriesAttributes.getRanges();
  }
  
  /**
   * Sets the attribute ranges to which the filter should be applied
   * 
   * @param range the attribute ranges
   */
  public void setRange(String range) {
    m_TimeSeriesAttributes.setRanges(range);
  }
  
  /**
   * Returns the tip text for the <code>ranges</code> option
   * 
   * @return the tip text
   */
  public String rangeTipText() {
    return "The attribute ranges to which the filter should be applied to";
  }
  
  /**
   * Returns whether the attribute ranges should be inverted
   * 
   * @return ranges inversion
   */
  public boolean getInvertRange() {
    return m_TimeSeriesAttributes.getInvert();
  }
  
  /**
   * Sets whether the attribute ranges should be inverted
   * 
   * @param inversion inversion
   */
  public void setInvertRange(boolean inversion) {
    m_TimeSeriesAttributes.setInvert(inversion);
  }
  
  /**
   * Returns the tip text for the invert range option
   * 
   * @return the tip text
   */
  public String invertRangeTipText() {
    return "Whether the specified attribute range should be inverted";
  }
  
  @Override
  public String globalInfo() {
    return
        "A filter to concatenate the string representation of each data" +
        " point of a one dimensional time series."
    ;
  
  }

  /**
   * Returns true if the output format is immediately available after the input
   * format has been set and not only after all the data has been seen (see batchFinished()).
   * 
   * @return <code>true</code>
   */
  @Override
  protected boolean hasImmediateOutputFormat() {
    return true;
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format
   * @return the output format
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
      throws Exception {
    
    m_TimeSeriesAttributes.setUpper(inputFormat.numAttributes() - 1);
    
    ArrayList<Attribute> newAttributes =
        new ArrayList<Attribute>(inputFormat.numAttributes());

    for (int att = 0; att < inputFormat.numAttributes(); att++) {
      newAttributes.add((Attribute) inputFormat.attribute(att).copy());
    }

    Instances outputFormat = new Instances(inputFormat.relationName(),
        newAttributes, 0);
    
    for (int index : m_TimeSeriesAttributes.getSelection()) {
      if (!inputFormat.attribute(index).isRelationValued()) {
        throw new Exception(String.format(
            "Attribute '%s' isn't relational!",
            inputFormat.attribute(index).name()
            ));
      }
      
      if (inputFormat.attribute(index).relation().numAttributes() != 1)
        throw new Exception(String.format("More than one dimension!",
            inputFormat.attribute(index).relation().numAttributes() + "(%d)"
        ));
     
      outputFormat.replaceAttributeAt(
          new Attribute(inputFormat.attribute(index).name(), (List<String>) null),
          index);

    }
    
    // initialize untouched string and relational attribute locators:
    m_TimeSeriesAttributes.setInvert(!m_TimeSeriesAttributes.getInvert());
    m_StringAttributes =
        new StringLocator(inputFormat, m_TimeSeriesAttributes.getSelection());
    m_RelationalAttributes =
        new RelationalLocator(inputFormat, m_TimeSeriesAttributes.getSelection());
    m_TimeSeriesAttributes.setInvert(!m_TimeSeriesAttributes.getInvert());
    
    outputFormat.setClassIndex(inputFormat.classIndex());
    
    return outputFormat;
  }


  /**
   * Processes the provided instance
   * 
   * @param inputInstance the instance that should be processed
   * @return the processed instance
   */
  @Override
  protected Instance process(Instance inputInstance) throws Exception {
    
    StringLocator.copyStringValues(inputInstance, getOutputFormat(),
        m_StringAttributes);
    RelationalLocator.copyRelationalValues(inputInstance, getOutputFormat(),
        m_RelationalAttributes);

    Instance outputInstance = null;
    if (inputInstance instanceof DenseInstance) {
      outputInstance = new DenseInstance(inputInstance.weight(),
          Arrays.copyOf(inputInstance.toDoubleArray(), inputInstance.numAttributes()));
    } else if (inputInstance instanceof SparseInstance) {
      outputInstance = new SparseInstance(inputInstance.weight(),
          Arrays.copyOf(inputInstance.toDoubleArray(), inputInstance.numAttributes()));
    } else {
      throw new Exception("Input instance is neither sparse nor dense!");
    }

    outputInstance.setDataset(getOutputFormat());
    
    for (int index : m_TimeSeriesAttributes.getSelection()) {
      
      if (inputInstance.isMissing(index))
        continue;
      
      Instances timeSeries = inputInstance.relationalValue(index);
      StringBuilder str = new StringBuilder();
      
      for (int i = 0; i < timeSeries.numInstances(); i++) {
        str.append(timeSeries.get(i).toString(0));
      }
      
      int stringIndex = getOutputFormat().attribute(index).addStringValue(str.toString());
      
      outputInstance.setValue(index, stringIndex);
      
    }
      
    return outputInstance;
  }

}

