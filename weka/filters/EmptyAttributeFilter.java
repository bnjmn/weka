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
 * Removes all attributes that do not contain more than one value. This
 * determination is made based on the first batch of instances seen.
 *
 * @author Stuart Inglis (stuart@intelligenesis.net)
 * @version $Revision: 1.2 $ 
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
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    m_NewBatch = true;
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
   * @exception Exception if the input instance was not of the 
   * correct format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    
    m_InputFormat.add(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    Instance current;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_MinArray == null) {
   
      // Compute minimums and maximums
      m_MinArray = new double[m_InputFormat.numAttributes()];
      m_MaxArray = new double[m_InputFormat.numAttributes()];
      m_Keep = new boolean[m_InputFormat.numAttributes()];
      for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	m_MinArray[i] = Double.NaN;
      }
      for (int j = 0; j < m_InputFormat.numInstances(); j++) {
	double[] value = m_InputFormat.instance(j).toDoubleArray();
	for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
	  if (m_InputFormat.attribute(i).isNumeric()) {
	    if (!Instance.isMissingValue(value[i])) {
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
      }

      FastVector attributes = new FastVector();
      for (int i = 0; i< m_InputFormat.numAttributes() ; i++) {
        if ((!m_InputFormat.attribute(i).isNumeric()) || 
            (m_MinArray[i] < m_MaxArray[i]) || (m_MaxArray[i] > 0) ) {
          attributes.addElement(m_InputFormat.attribute(i).copy());

          m_Keep[i] = true;
        }
      }

      Instances outputFormat = new Instances(m_InputFormat.relationName(),
					     attributes, 0); 
      setOutputFormat(outputFormat);

      // Convert pending input instances
      for(int i = 0; i < m_InputFormat.numInstances(); i++) {
	current = m_InputFormat.instance(i);
	convertInstance(current);
      }

      // Free memory
      m_InputFormat = new Instances(m_InputFormat, 0);
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
  private void convertInstance(Instance instance) throws Exception {
  
    int index = 0;
    double [] newVals = new double [outputFormatPeek().numAttributes()];

    for(int i = 0; i < m_InputFormat.numAttributes(); i++) {
      if (m_Keep[i]) {
        if (instance.isMissing(i)) {
          newVals[index] = Instance.missingValue();
        } else {
          newVals[index] = instance.value(i);
        }
        index++;
      }
    }

    if (instance instanceof SparseInstance) {
      push(new SparseInstance(instance.weight(), newVals));
    } else {
      push(new Instance(instance.weight(), newVals));
    }

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








