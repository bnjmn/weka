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
 *    EmptyAttributeFilter.java
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 */

package weka.filters;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.FastVector;
import weka.core.SparseInstance;
import weka.core.Utils;


/** 
 * Removes all attributes that do not contain more than one distinct
 * value.  This determination is made based on the first batch of
 * instances seen. If an attribute contains one distinct value and
 * missing values, this is still taken as being empty.
 *
 * @author Stuart Inglis (stuart@intelligenesis.net)
 * @version $Revision: 1.7 $ 
 */
public class EmptyAttributeFilter extends Filter {

  /** The minimum values for numeric attributes. */
  private double [] m_MinArray;
  
  /** The maximum values for numeric attributes. */
  private double [] m_MaxArray;

  /** An array of attribute indices that shall be kept. */
  private boolean [] m_Keep;

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
    m_MinArray = m_MaxArray = null;
    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    
    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_MinArray == null) {
   
      // Compute minimums and maximums
      Instances in = getInputFormat();
      m_MinArray = new double[in.numAttributes()];
      m_MaxArray = new double[in.numAttributes()];
      m_Keep = new boolean[in.numAttributes()];
      for (int i = 0; i < in.numAttributes(); i++) {
	m_MinArray[i] = Double.NaN;
      }
      for (int j = 0; j < in.numInstances(); j++) {
	double [] value = in.instance(j).toDoubleArray();
	for (int i = 0; i < in.numAttributes(); i++) {
          if (!in.attribute(i).isString() &&
              !Instance.isMissingValue(value[i])) {
            if (Double.isNaN(m_MinArray[i])) {
              m_MinArray[i] = m_MaxArray[i] = value[i];
            } else {
              if (value[i] < m_MinArray[i]) {
                m_MinArray[i] = value[i];
              }
              if (value[i] > m_MaxArray[i]) {
                m_MaxArray[i] = value[i];
              }
            }
	  }
	} 
      }

      FastVector attributes = new FastVector();
      for (int i = 0; i < in.numAttributes(); i++) {
        if (in.attribute(i).isString() || 
            (m_MinArray[i] < m_MaxArray[i])) {
          attributes.addElement(in.attribute(i).copy());
          m_Keep[i] = true;
        }
      }

      Instances outputFormat = new Instances(in.relationName(),
					     attributes, 0); 
      setOutputFormat(outputFormat);

      // Convert pending input instances
      for(int i = 0; i < in.numInstances(); i++) {
	convertInstance(in.instance(i));
      }

      // Free memory
      flushInput();
    } 

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    int index = 0;
    double [] vals = new double [outputFormatPeek().numAttributes()];

    for(int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (m_Keep[i]) {
        if (instance.isMissing(i)) {
          vals[index] = Instance.missingValue();
        } else {
          vals[index] = instance.value(i);
        }
        index++;
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
 	Filter.batchFilterFile(new EmptyAttributeFilter(), argv);
      } else {
	Filter.filterFile(new EmptyAttributeFilter(), argv);
      }
    } catch (Exception ex) {
	//	ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}








