/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Tag.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

/**
 * A <code>Tag</code> simply associates a numeric ID with a String description.
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.6 $
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
