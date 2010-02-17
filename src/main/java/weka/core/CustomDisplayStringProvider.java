/**
 * CustomDisplayStringProvider.java
 * Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 */
package weka.core;

/**
 * For classes that do not implement the OptionHandler interface and want to 
 * provide a custom display string in the GenericObjectEditor, which is more 
 * descriptive than the class name.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public interface CustomDisplayStringProvider {

  /**
   * Returns the custom display string.
   * 
   * @return		the string
   */
  public String toDisplay();
}
