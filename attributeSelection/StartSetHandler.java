/*
 *    StartSetHandler.java
 *    Copyright (C) 1999 Mark Hall
 *
 */


package weka.attributeSelection;
import java.io.*;
import weka.core.*;

/** 
 * Interface for search methods capable of doing something sensible
 * given a starting set of attributes.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.1 $
 */
public interface StartSetHandler {

  /**
   * Sets a starting set of attributes for the search. It is the
   * search method's responsibility to report this start set (if any)
   * in its toString() method.
   * @param startSet a string containing a list of attributes (and or ranges),
   * eg. 1,2,6,10-15.
   * @exception Exception if start set can't be set.
   */
  public abstract void setStartSet (String startSet) throws Exception;

  /**
   * Returns a list of attributes (and or attribute ranges) as a String
   * @return a list of attributes (and or attribute ranges)
   */
  public abstract String getStartSet ();
}
