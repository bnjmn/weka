/*
 *    OptionHandler.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.core;

import java.util.*;

/** 
 * Interface to something that understands options.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface OptionHandler {

  /**
   * Returns an enumeration of all the available options.
   *
   * @return an enumeration of all available options
   */
  public Enumeration listOptions();

  /**
   * Sets the OptionHandler's options using the given list. All options
   * will be set (or reset) during this call (i.e. incremental setting
   * of options is not possible).
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception;

  /**
   * Gets the current option settings for the OptionHandler.
   *
   * @return the list of current option settings as an array of strings
   */
  public String[] getOptions();
}








