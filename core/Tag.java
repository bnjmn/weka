/*
 *    Tag.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

/**
 * A <code>Tag</code> simply associates a numeric ID with a String description.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 */
public class Tag {

  /** The ID */
  protected int m_ID;

  /** The descriptive text */
  protected String m_Readable;
  
  /**
   * Creates a new <code>Tag</code> instance.
   *
   * @param ident the ID for the new Tag.
   * @param readable the description for the new Tag.
   */
  public Tag(int ident, String readable) {
    m_ID = ident;
    m_Readable = readable;
  }

  /**
   * Gets the numeric ID of the Tag.
   *
   * @return the ID of the Tag.
   */
  public int getID() {
    return m_ID;
  }

  /**
   * Gets the string description of the Tag.
   *
   * @return the description of the Tag.
   */
  public String getReadable() {
    return m_Readable;
  }
}
