/*
 *    NonSparseToSparseFilter.java
 *    Copyright (C) 2000 Eibe Frank
 *
 */


package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * A filter that converts all incoming instances into sparse format.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class NonSparseToSparseFilter extends Filter {

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that converts all incoming instances"
      + " into sparse format.";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   */
  public boolean inputFormat(Instances instanceInfo) {

    m_InputFormat = new Instances(instanceInfo, 0);
    setOutputFormat(m_InputFormat);
    m_NewBatch = true;
    return true;
  }


  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    push(new SparseInstance(instance));
    return true;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    
    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new NonSparseToSparseFilter(), argv);
      } else {
	Filter.filterFile(new NonSparseToSparseFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








