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
 *    NominalToBinary.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters.unsupervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Converts all nominal attributes into binary numeric attributes. An
 * attribute with k values is transformed into k binary attributes
 * (using the one-attribute-per-value approach).
 * Binary attributes are left binary.
 *
 * Valid filter-specific options are: <p>
 *
 * -N <br>
 * If binary attributes are to be coded as nominal ones.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.1 $ 
 */
public class NominalToBinary extends Filter implements UnsupervisedFilter,
						       OptionHandler {

  /** The sorted indices of the attribute values. */
  private int[][] m_Indices = null;

  /** Are the new attributes going to be nominal or numeric ones? */
  private boolean m_Numeric = true;

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
    setOutputFormat();
    m_Indices = null;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    convertInstance(instance);
    return true;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tSets if binary attributes are to be coded as nominal ones.",
	      "N", 0, "-N"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -N <br>
   * If binary attributes are to be coded as nominal ones.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setBinaryAttributesNominal(Utils.getFlag('N', options));

    if (getInputFormat() != null)
      setInputFormat(getInputFormat());
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [1];
    int current = 0;

    if (getBinaryAttributesNominal()) {
      options[current++] = "-N";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets if binary attributes are to be treated as nominal ones.
   *
   * @return true if binary attributes are to be treated as nominal ones
   */
  public boolean getBinaryAttributesNominal() {

    return !m_Numeric;
  }

  /**
   * Sets if binary attributes are to be treates as nominal ones.
   *
   * @param bool true if binary attributes are to be treated as nominal ones
   */
  public void setBinaryAttributesNominal(boolean bool) {

    m_Numeric = !bool;
  }

  /**
   * Set the output format if the class is nominal.
   */
  private void setOutputFormat() {

    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = getInputFormat().classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (!att.isNominal()) {
	newAtts.addElement(att.copy());
      } else {
	if (att.numValues() <= 2) {
	  if (m_Numeric) {
	    newAtts.addElement(new Attribute(att.name()));
	  } else {
	    newAtts.addElement(att.copy());
	  }
	} else {

	  if (newClassIndex >= 0 && j < getInputFormat().classIndex()) {
	    newClassIndex += att.numValues() - 1;
	  }

	  // Compute values for new attributes
	  for (int k = 0; k < att.numValues(); k++) {
	    attributeName = 
	      new StringBuffer(att.name() + "=");
	    attributeName.append(att.value(k));
	    if (m_Numeric) {
	      newAtts.
		addElement(new Attribute(attributeName.toString()));
	    } else {
	      vals = new FastVector(2);
	      vals.addElement("f"); vals.addElement("t");
	      newAtts.
		addElement(new Attribute(attributeName.toString(), vals));
	    }
	  }
	}
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(),
				 newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over if the class is nominal. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    double [] vals = new double [outputFormatPeek().numAttributes()];
    int attSoFar = 0;

    for(int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (!att.isNominal()) {
	vals[attSoFar] = instance.value(j);
	attSoFar++;
      } else {
	if (att.numValues() <= 2) {
	  vals[attSoFar] = instance.value(j);
	  attSoFar++;
	} else {
	  if (instance.isMissing(j)) {
	    for (int k = 0; k < att.numValues(); k++) {
              vals[attSoFar + k] = instance.value(j);
	    }
	  } else {
	    for (int k = 0; k < att.numValues(); k++) {
	      if (k == (int)instance.value(j)) {
                vals[attSoFar + k] = 1;
	      } else {
                vals[attSoFar + k] = 0;
	      }
	    }
	  }
	  attSoFar += att.numValues();
	}
      }
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), getInputStringIndex(),
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
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
 	Filter.batchFilterFile(new NominalToBinary(), argv);
      } else {
	Filter.filterFile(new NominalToBinary(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








