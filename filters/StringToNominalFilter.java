/*
 *    StringToNominalFilter.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
 */


package weka.filters;


import java.util.Enumeration;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.UnsupportedAttributeTypeException;


/** 
 * Converts a string attribute (i.e. unspecified number of values) to nominal
 * (i.e. set number of values). You should ensure that all string values that
 * will appear are represented in the dataset.<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -C col <br>
 * Index of the attribute to be changed. (default last)<p>
 *
 * @author Len Trigg (len@intelligenesis.net) 
 * @version $Revision: 1.4 $
 */
public class StringToNominalFilter extends Filter 
  implements OptionHandler {

  /** The attribute index setting (allows -1 = last). */
  private int m_AttIndexSet = -1; 

  /** The attribute index. */
  private int m_AttIndex; 

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately.
   * @exception UnsupportedAttributeTypeException if the selected attribute
   * a string attribute.
   * @exception Exception if the input format can't be set 
   * successfully.
   */
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    super.inputFormat(instanceInfo);
    m_AttIndex = m_AttIndexSet;
    if (m_AttIndex < 0) {
      m_AttIndex = instanceInfo.numAttributes() - 1;
    }
    if (!instanceInfo.attribute(m_AttIndex).isString()) {
      throw new UnsupportedAttributeTypeException("Chosen attribute is not of type string.");
    }
    return false;
  }

  /**
   * Input an instance for filtering. The instance is processed
   * and made available for output immediately.
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

    if (isOutputFormatDefined()) {
      Instance newInstance = (Instance)instance.copy();
      push(newInstance);
      return true;
    }

    bufferInput(instance);
    return false;
  }


  /**
   * Signifies that this batch of input to the filter is finished. If the 
   * filter requires all instances prior to filtering, output() may now 
   * be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output.
   * @exception IllegalStateException if no input structure has been defined.
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (!isOutputFormatDefined()) {

      setOutputFormat();

      // Convert pending input instances
      for(int i = 0; i < getInputFormat().numInstances(); i++) {
	push((Instance) getInputFormat().instance(i).copy());
      }
    } 

    flushInput();
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }


  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSets the attribute index (default last).",
              "C", 1, "-C <col>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -C col <br>
   * The column containing the values to be merged. (default last)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String attributeIndex = Utils.getOption('C', options);
    if (attributeIndex.length() != 0) {
      if (attributeIndex.toLowerCase().equals("last")) {
	setAttributeIndex(-1);
      } else if (attributeIndex.toLowerCase().equals("first")) {
	setAttributeIndex(0);
      } else {
	setAttributeIndex(Integer.parseInt(attributeIndex) - 1);
      }
    } else {
      setAttributeIndex(-1);
    }
       
    if (getInputFormat() != null) {
      inputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-C";
    options[current++] = "" + (getAttributeIndex() + 1);

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the index of the attribute used.
   *
   * @return the index of the attribute
   */
  public int getAttributeIndex() {

    return m_AttIndexSet;
  }

  /**
   * Sets index of the attribute used.
   *
   * @param index the index of the attribute
   */
  public void setAttributeIndex(int attIndex) {
    
    m_AttIndexSet = attIndex;
  }

  /**
   * Set the output format. Takes the current average class values
   * and m_InputFormat and calls setOutputFormat(Instances) 
   * appropriately.
   */
  private void setOutputFormat() {
    
    Instances newData;
    FastVector newAtts, newVals;
      
    // Compute new attributes
      
    newAtts = new FastVector(getInputFormat().numAttributes());
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (j != m_AttIndex) {
	newAtts.addElement(att.copy()); 
      } else {
	  
	// Compute list of attribute values
	newVals = new FastVector(att.numValues());
	for (int i = 0; i < att.numValues(); i++) {
          newVals.addElement(att.value(i)); 
	}
	newAtts.addElement(new Attribute(att.name(), newVals));
      }
    }
      
    // Construct new header
    newData = new Instances(getInputFormat().relationName(), newAtts, 0);
    newData.setClassIndex(getInputFormat().classIndex());
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
 	Filter.batchFilterFile(new StringToNominalFilter(), argv);
      } else {
	Filter.filterFile(new StringToNominalFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








