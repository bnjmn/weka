/*
 *    Summarizable.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

/** 
 * Interface to something that provides a short textual summary (as opposed
 * to toString() which is usually a fairly complete description) of itself.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface Summarizable {

  /**
   * Returns a string that summarizes the object.
   *
   * @return the object summarized as a string
   */
  public String toSummaryString();
}








