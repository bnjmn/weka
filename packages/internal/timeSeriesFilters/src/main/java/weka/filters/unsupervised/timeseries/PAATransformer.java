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
 * PAATransformer.java
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
import weka.core.SparseInstance;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.RelationalLocator;
import weka.core.RevisionUtils;
import weka.core.StringLocator;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.SimpleStreamFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start -->
 * A filter to perform the Piecewise Aggregate Approximation transformation to time series.<br/>
 * <br/>
 * It uses the "N/n not equal an integer" generalization. Furthermore, it is extended to handle weights for data points in time series.<br/>
 * <br/>
 * Warning: The lower bounding property may not hold for the distance measure when weights are used or N/n is not equal to an integer!<br/>
 * <br/>
 * For more information see:<br/>
 * Byoung-Kee Yi, Christos Faloutsos: Fast Time Sequence Indexing for Arbitrary L_p Norms. In: Proceedings of the 26th VLDB Conference, 385-394, 2000.<br/>
 * <br/>
 * Eamonn Keogh, Kaushik Chakrabarti, Michael Pazzani, Sharad Mehrotra (2001). Dimensionality Reduction for Fast Similarity Search in Large Time Series Databases. Knowledge and information Systems. 3(3):263-286.<br/>
 * <br/>
 * Li Wei. Code for "N/n not equal an integer'. URL http://www.cs.ucr.edu/~eamonn/SAX_2006_ver.zip.
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
 * <!-- options-end -->
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class PAATransformer extends SimpleStreamFilter
  implements UnsupervisedFilter, TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = 3384394202360169084L;
  
  /** the default compression factor w to be used for the PAA transformation */
  protected static final int DEFAULT_W = 100;
  
  /** the default attributes that should be transformed */
  protected static final Range getDefaultAttributes() {
    return new Range();
  }

  /** which attributes to transform (must be time series) */
  protected Range m_TimeSeriesAttributes = new Range(getDefaultAttributes().getRanges());

  /** the compression factor w for the PAA transformation */
  protected int m_W = DEFAULT_W; // invariant: m_W >= 1

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

    // currently can't handle missing values inside time series
    // if a time series is missing, it won't be touched

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance filter for the relational
   * data (i.e., the bags).
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = new Capabilities(this);

    // attributes
    result.disableAll();
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

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
        "\tnumeric attributes which are each transformed separately.\n" +
        "\tFirst and last are valid indices. (default "
        + getDefaultAttributes() + ")"
        , "R", 1, "-R <index1,index2-index3,...>"));

    options.add(new Option(
        "\tInverts the specified attribute range (default don't invert)"
        , "V", 0, "-V"));

    options.add(new Option(
        "\tSpecifies the compression factor w for the PAA transformation.\n" +
        "\tA time series of length n will be compressed to one of length w.\n" +
        "\t(default " + DEFAULT_W + ")."
        , "W", 1, "-W <CompressionFactor>"));
    
    return Collections.enumeration(options);
  }
  
  /**
   * Gets the current settings of the filter.
   */
  @Override
  public String[] getOptions() {
    
    List<String> options = new ArrayList<String>();
    
    if (m_W != DEFAULT_W) {
      options.add("-W");
      options.add("" + m_W);
    }
    
    if (!m_TimeSeriesAttributes.getRanges().equals("")) {
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
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    
    String w = Utils.getOption('W', options);
    if (w.length() != 0)
      m_W = Integer.parseInt(w);
    
    if (m_W <= 0)
      throw new Exception("Parameter W must be bigger than 0!");
    
    String timeSeriesAttributes = Utils.getOption('R', options);
    if (timeSeriesAttributes.length() != 0) {
      m_TimeSeriesAttributes = new Range(timeSeriesAttributes);
    }
    m_TimeSeriesAttributes.setInvert(Utils.getFlag('V', options));

    Utils.checkForRemainingOptions(options);
  }
  
  /**
   * Gets the <code>W</code> factor for the PAA transformation
   * 
   * @return the <code>W</code> factor
   */
  public int getW() {
    return m_W;
  }
  
  /**
   * Sets the <code>W</code> factor for the PAA transformation
   * 
   * @param W the <code>W</code> factor
   */
  public void setW(int W) {
    m_W = W;
  }
  
  /**
   * Returns the tip text for the <code>W</code> parameter
   * 
   * @return the tip text
   */
  public String wTipText() {
    return "The compression factor w";
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
  
  /**
   * Returns a string describing this filter.
   */
  @Override
  public String globalInfo() {
    return
        "A filter to perform the Piecewise Aggregate Approximation " +
        "transformation to time series.\n\n" +
        "It uses the \"N/n not equal an integer\" generalization. " +
        "Furthermore, it is extended to handle weights for data points in a time series.\n\n" +
        "Warning: The lower bounding property may not hold for the distance measure " +
        "when weights are used or N/n is not equal to an integer!\n\n" +
        "For more information see:\n" +
        getTechnicalInformation().toString()
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

    TechnicalInformation result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Byoung-Kee Yi and Christos Faloutsos");
    result.setValue(Field.TITLE, "Fast Time Sequence Indexing for Arbitrary L_p Norms");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 26th VLDB Conference");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.PAGES, "385-394");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann Publishers");

    TechnicalInformation additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "Eamonn Keogh and Kaushik Chakrabarti and Michael Pazzani " +
                      "and Sharad Mehrotra");
    additional.setValue(Field.TITLE,
            "Dimensionality Reduction for Fast Similarity Search in Large Time Series Databases");
    additional.setValue(Field.JOURNAL, "Knowledge and information Systems");
    additional.setValue(Field.YEAR, "2001");
    additional.setValue(Field.PAGES, "263-286");
    additional.setValue(Field.NUMBER, "3");
    additional.setValue(Field.VOLUME, "3");

    TechnicalInformation nNnonEqual = result.add(Type.MISC);
    nNnonEqual.setValue(Field.AUTHOR, "Li Wei");
    nNnonEqual.setValue(Field.TITLE, "Code for \"N/n not equal an integer\'");
    nNnonEqual.setValue(Field.URL, "http://www.cs.ucr.edu/~eamonn/SAX_2006_ver.zip");

    return result;
  }

  /**
   * This method needs to be called before the filter is used to transform instances. Will return true
   * for this filter because the output format can be collected immediately.
   *
   * This specialised version of setInputFormat() calls the setInputFormat() method in SimpleFilter and then sets the
   * output locators for relational and string attributes correctly. We also set the input locators correctly here.
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);

    // Find attributes that have not been selected
    m_TimeSeriesAttributes.setInvert(!m_TimeSeriesAttributes.getInvert());

    // Make sure only string and relational attributes that are not in the selected range for the time
    // series are covered by the output locators.
    initOutputLocators(outputFormatPeek(), m_TimeSeriesAttributes.getSelection());

    // Make sure only string and relational attributes that are not in the selected range for the time
    // series are covered by the input locators.
    initInputLocators(inputFormatPeek(), m_TimeSeriesAttributes.getSelection());

    // Restore selected attributes
    m_TimeSeriesAttributes.setInvert(!m_TimeSeriesAttributes.getInvert());

    return hasImmediateOutputFormat();
  }

  /**
   * Determines the output format based on the input format and returns this.
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

    m_TimeSeriesAttributes.setUpper(inputFormat.numAttributes() - 1);
    
    for (int index : m_TimeSeriesAttributes.getSelection()) {
      Attribute attribute = getInputFormat().attribute(index);

      // time series must be relational
      if (!attribute.isRelationValued()) {
        throw new Exception(String.format("Attribute '%s' isn't relational!", attribute.name()));
      }
      
      // time series must contain only numeric attributes
      Instances timeSeries = attribute.relation();
      for (int i = 0; i < timeSeries.numAttributes(); i++) {
        if (!timeSeries.attribute(i).isNumeric()) {
          throw new Exception(String.format("Attribute '%s' inside relational attribute '%s' isn't numeric!",
                  timeSeries.attribute(i).name(), attribute.name()));
        }
      }
    }

    return new Instances(inputFormat, 0);
    
  }

  /**
   * Processes the given instance (may change the provided instance) and
   * returns the modified version.</p>
   * 
   * Will delegate time series processing to the {@link #transform(Instances, Instances)}
   * method
   */
  @Override
  protected Instance process(Instance inputInstance) throws Exception {
    
    assert inputInstance.dataset().equalHeaders(getInputFormat());

    double[] outputInstance = inputInstance.toDoubleArray();
    for (int index : m_TimeSeriesAttributes.getSelection()) {
      if (inputInstance.isMissing(index)) {
        continue;
      }
      Instances timeSeries = inputInstance.relationalValue(inputInstance.attribute(index));
      Attribute outputAttribute = outputFormatPeek().attribute(index);
      outputInstance[index] = (double) outputAttribute.addRelation(transform(timeSeries, outputAttribute.relation()));
    }

    // Create new instance and make sure string and relational values are copied from input to output
    Instance outInstance = inputInstance.copy(outputInstance);; // Note that this will copy the reference to the input dataset!
    outInstance.setDataset(null); // Make sure we do not class the copyValues() method in push()
    copyValues(outInstance, false, inputInstance.dataset(), outputFormatPeek()); // Copy string and relational values

    return outInstance;
  }
  
  /**
   * PAA transforms a time series</p>
   * 
   * This uses the "N/n not equal an integer" specialization.
   * See: http://www.cs.ucr.edu/~eamonn/SAX_2006_ver.zip</p>
   * 
   * Furthermore it's extended to handle time series where data points have
   * different weights.</p>
   * 
   * However, it's unknown whether the lower bounding property of the distance
   * measure applies to the weighted case or the "N/n not equal an integer"
   * case!</p>
   * 
   * @param input the time series to be transformed
   * @param outputFormat the format of the output data (not used in this super class method)
   *
   * @return the transformed time series
   * @throws Exception
   */
  protected Instances transform(Instances input, Instances outputFormat) throws Exception {
    
    /* A word on this implementation:
     * 
     * There are different ways to implement this algorithm.
     * This implementation puts an emphasis on numerical stability.
     * 
     * I.e. not so much on numerical stability when calculating the average 
     * (average calculation usually doesn't suffer from serious numerical
     * problems and stability usually only becomes a concern in very extreme
     * cases) but more on numerical stability when it comes to distributing
     * the input instances onto the output instances.
     *
     *
     * This can be seen in how the segment boundaries are calculated:
     * 
     * segmentUpperBoundary = ++segmentIndex*segmentSize;
     * 
     * will only suffer from rounding errors in the multiplication and rounding
     * errors in segmentSize. I.e. there is no error accumulating.
     * 
     * In contrast to calculating it like:
     * 
     * segmentUpperBoundary += segmentSize
     * 
     * Which will accumulate rounding errors over the whole run!
     * 
     * 
     * However, the instance boundaries will suffer from accumulated errors:
     * 
     * instanceUpperBoundary = instanceLowerBoundary + input.get(i).weight();
     * 
     * But summation will always suffer from accumulated rounding errors and
     * this is still better than e.g. chopping up the instance weights into
     * the segments and then adding them up again where we would introduce
     * artificial error accumulation again.
     * 
     * If accumulated errors become a problem when summing the instance weights
     * up, there is always the option of using kahan summation or another
     * numerically stable summation algorithm.
     * 
     * 
     * Unfortunate is that some weight calculation may suffer from cancellation
     * problems, because the absolute boundaries are subtracted. However, this
     * seems to be the price of having numerically stable boundaries and
     * avoiding the accumulated error for the boundaries.
     * 
     * Noteworthy is that exactly this accumulated error was seen during testing
     * an early implementation that suffered from this problem.
     */
    
    Instances output = new Instances(input, m_W);
    
    assert m_W >= 1; // enforced in setOptions();
    
    final double segmentSize = input.sumOfWeights()/(double) m_W;
    int segmentIndex = 1;
    double segmentLowerBoundary = 0;
    double segmentUpperBoundary = segmentSize;
    
    double instanceLowerBoundary = 0.0;
    double instanceUpperBoundary = 0.0;

    double[] sumValues = new double[input.numAttributes()];
    
    for (int i = 0; i < input.numInstances(); i++) {

      // loop invariant
      assert instanceLowerBoundary < segmentUpperBoundary;
      assert segmentLowerBoundary <= instanceLowerBoundary;
      // variant: input.numInstances() - i;
      
      instanceUpperBoundary = instanceLowerBoundary + input.get(i).weight();
      
      if (instanceUpperBoundary >= segmentUpperBoundary && segmentUpperBoundary - instanceLowerBoundary < segmentSize) {

        // yield new segment
        final double weight = segmentUpperBoundary - instanceLowerBoundary;
        for (int att = 0; att < input.numAttributes(); att++) {
          sumValues[att] += input.get(i).value(att)*weight;
          sumValues[att] /= segmentSize;
        }
        
        output.add(new DenseInstance(segmentSize, sumValues));
        segmentLowerBoundary = segmentUpperBoundary;
        segmentUpperBoundary = ++segmentIndex*segmentSize;
        sumValues = new double[input.numAttributes()];
        
      }
      
      while (instanceUpperBoundary >= segmentUpperBoundary) {

        // yield new segment
        output.add(new DenseInstance(segmentSize, input.get(i).toDoubleArray()));
        segmentLowerBoundary = segmentUpperBoundary;
        segmentUpperBoundary = ++segmentIndex*segmentSize;
        sumValues = new double[input.numAttributes()];
        
      }
      
      assert instanceUpperBoundary < segmentUpperBoundary;
      
      // add remaining to current segment
      final double weight = Math.min(input.get(i).weight(), instanceUpperBoundary - segmentLowerBoundary);

      for (int att = 0; att < input.numAttributes(); att++) {
        sumValues[att] += input.get(i).value(att) * weight;
      }

      instanceLowerBoundary = instanceUpperBoundary;
    }
    
    return output;
  }
  
  /**
   * Main method for running this filter.
   * 
   * @param args arguments for the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new PAATransformer(), args);
  }

}

