/*
 *    AdditionalMeasureProducer.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.core;

import java.util.*;

/** 
 * Interface to something that can produce measures other than those
 * calculated by evaluation modules. 
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface AdditionalMeasureProducer {

  /**
   * Returns an enumeration of the measure names. Additional measures
   * must follow the naming convention of starting with "measure", eg.
   * double measureBlah()
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures();

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception Exception if the named measure is not supported
   */
  public double getMeasure(String measureName) throws Exception;
}
