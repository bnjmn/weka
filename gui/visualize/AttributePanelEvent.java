/*
 *    AttributePanelEvent.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.gui.visualize;

/**
 * Class encapsulating a change in the AttributePanel's selected x and y
 * attributes.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class AttributePanelEvent {

  /** True if the x selection changed */
  public boolean m_xChange;

  /** True if the y selection changed */
  public boolean m_yChange;

  /** The index for the new attribute */
  public int m_indexVal;

  /**
   * Constructor
   * @param xChange true if a change occured to the x selection
   * @param yChange true if a change occured to the y selection
   * @param indexVal the index of the new attribute
   */
  public AttributePanelEvent(boolean xChange, boolean yChange, int indexVal) {
    m_xChange = xChange;
    m_yChange = yChange;
    m_indexVal = indexVal;
  }
}
