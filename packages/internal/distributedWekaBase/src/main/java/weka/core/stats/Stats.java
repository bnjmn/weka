package weka.core.stats;

import java.io.Serializable;

import weka.core.Attribute;

/**
 * Stats base class for the numeric and nominal summary meta data
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class Stats implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 3662688283840145572L;

  /** The name of the attribute that this Stats pertains to */
  protected String m_attributeName = "";

  /**
   * Construct a new Stats
   * 
   * @param attributeName the name of the attribute/field that this Stats
   *          pertains to
   */
  public Stats(String attributeName) {
    m_attributeName = attributeName;
  }

  /**
   * Get the name of the attribute that this Stats pertains to
   * 
   * @return the name of the attribute
   */
  public String getName() {
    return m_attributeName;
  }

  /**
   * Makes a Attribute that encapsulates the meta data
   * 
   * @return an Attribute that encapsulates the meta data
   */
  public abstract Attribute makeAttribute();

}