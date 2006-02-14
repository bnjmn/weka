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
 *    ChangeDateFormat.java
 *    Copyright (C) 2004 Len Trigg
 *
 */


package weka.filters.unsupervised.attribute;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SingleIndex;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;


/**
 * Changes the date format used by a date attribute. This is most
 * useful for converting to a format with less precision, for example,
 * from an absolute date to day of year, etc. This changes the format
 * string, and changes the date values to those that would be parsed
 * by the new format.<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -C col <br>
 * The column containing the date attribute to be changed. (default last)<p>
 *
 * -F format <br>
 * The output date format (default corresponds to ISO-8601 format).<p>
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class ChangeDateFormat extends Filter 
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {


  /** The default output date format. Corresponds to ISO-8601 format. */
  private static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


  /** The attribute's index setting. */
  private SingleIndex m_AttIndex = new SingleIndex("last"); 

  /** The output date format. */
  private SimpleDateFormat m_DateFormat = DEFAULT_FORMAT;

  /** The output attribute. */
  private Attribute m_OutputAttribute;


  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Changes the format used by a date attribute.";
  }


  /** {@inheritDoc */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    super.setInputFormat(instanceInfo);
    m_AttIndex.setUpper(instanceInfo.numAttributes() - 1);
    if (!instanceInfo.attribute(m_AttIndex.getIndex()).isDate()) {
      throw new UnsupportedAttributeTypeException("Chosen attribute not date.");
    }

    setOutputFormat();
    return true;
  }


  /** {@inheritDoc */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    Instance newInstance = (Instance)instance.copy();
    int index = m_AttIndex.getIndex();
    if (!newInstance.isMissing(index)) {
      double value = instance.value(index);
      try {
        // Format and parse under the new format to force any required
        // loss in precision.
        value = m_OutputAttribute.parseDate(m_OutputAttribute.formatDate(value));
      } catch (ParseException pe) {
        throw new RuntimeException("Output date format couldn't parse its own output!!");
      }
      newInstance.setValue(index, value);
    }
    push(newInstance);
    return true;
  }


  /** {@inheritDoc */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
              "\tSets the attribute index (default last).",
              "C", 1, "-C <col>"));

    newVector.addElement(new Option(
              "\tSets the output date format string (default corresponds to ISO-8601).",
              "F", 1, "-F <value index>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -C col <br>
   * The column containing the date attribute to be changed. (default last)<p>
   *
   * -F index <br>
   * The output date format (default corresponds to ISO-8601 format).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String attIndex = Utils.getOption('C', options);
    if (attIndex.length() != 0) {
      setAttributeIndex(attIndex);
    } else {
      setAttributeIndex("last");
    }

    String formatString = Utils.getOption('F', options);
    if (formatString.length() != 0) {
      setDateFormat(formatString);
    } else {
      setDateFormat(DEFAULT_FORMAT);
    }

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }


  /** {@inheritDoc */
  public String [] getOptions() {

    String [] options = new String [4];
    int current = 0;

    options[current++] = "-C";
    options[current++] = "" + getAttributeIndex();
    options[current++] = "-F"; 
    options[current++] = "" + getDateFormat().toPattern();
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }


  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeIndexTipText() {

    return "Sets which attribute to process. This "
      + "attribute must be of type date (\"first\" and \"last\" are valid values)";
  }


  /**
   * Gets the index of the attribute converted.
   *
   * @return the index of the attribute
   */
  public String getAttributeIndex() {

    return m_AttIndex.getSingleIndex();
  }


  /**
   * Sets the index of the attribute used.
   *
   * @param index the index of the attribute
   */
  public void setAttributeIndex(String attIndex) {
    
    m_AttIndex.setSingleIndex(attIndex);
  }


  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String dateFormatTipText() {

    return "The date format to change to. This should be a "
      + "format understood by Java's SimpleDateFormat class.";
  }


  /**
   * Get the date format used in output.
   *
   * @return the output date format.
   */
  public SimpleDateFormat getDateFormat() {

    return m_DateFormat;
  }


  /**
   * Sets the output date format.
   *
   * @param index the output date format.
   */
  public void setDateFormat(String dateFormat) {

    setDateFormat(new SimpleDateFormat(dateFormat));
  }

  /**
   * Sets the output date format.
   *
   * @param index the output date format.
   */
  public void setDateFormat(SimpleDateFormat dateFormat) {
    if (dateFormat == null) {
      throw new NullPointerException();
    }
    m_DateFormat = dateFormat;
  }


  /**
   * Set the output format. Changes the format of the specified date
   * attribute.
   */
  private void setOutputFormat() {
    
    // Create new attributes
    FastVector newAtts = new FastVector(getInputFormat().numAttributes());
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (j == m_AttIndex.getIndex()) {
	newAtts.addElement(new Attribute(att.name(), getDateFormat().toPattern()));  
      } else {
	newAtts.addElement(att.copy()); 
      }
    }
      
    // Create new header
    Instances newData = new Instances(getInputFormat().relationName(), newAtts, 0);
    newData.setClassIndex(getInputFormat().classIndex());
    m_OutputAttribute = newData.attribute(m_AttIndex.getIndex());
    setOutputFormat(newData);
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
 	Filter.batchFilterFile(new ChangeDateFormat(), argv);
      } else {
	Filter.filterFile(new ChangeDateFormat(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








