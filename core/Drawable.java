/*
 *    Drawable.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.core;

/** 
 * Interface to something that can be drawn as a graph.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface Drawable {

  /**
   * Returns a string that describes a graph representing
   * the object. The string should be in dotty format.
   *
   * @return the graph described by a string
   * @exception Exception if the graph can't be computed
   */
  public String graph() throws Exception;
}








