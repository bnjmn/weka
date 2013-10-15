package weka.core;

/**
 * Marker interface for something that has been constructed/learned earlier
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface Preconstructed {

  /**
   * Returns true if this Preconstructed instance is initialized and ready to be
   * used
   * 
   * @return true if this instance is initialized and ready to be used
   */
  boolean isConstructed();

  /**
   * Reset. After calling reset() a call to isConstructed() should return false.
   */
  void reset();

}
