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
 *    SelectedTag.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

/**
 * Represents a selected value from a finite set of values, where each
 * value is a Tag (i.e. has some string associated with it). Primarily
 * used in schemes to select between alternative behaviours,
 * associating names with the alternative behaviours.
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a> 
 * @version $Revision: 1.8 $
 */
public class SelectedTag {
  
  /** The index of the selected tag */
  protected int m_Selected;
  
  /** The set of tags to choose from */
  protected Tag [] m_Tags;
  
  /**
   * Creates a new <code>SelectedTag</code> instance.
   *
   * @param tagID the id of the selected tag.
   * @param tags an array containing the possible valid Tags.
   * @exception IllegalArgumentException if the selected tag isn't in the array
   * of valid values.
   */
  public SelectedTag(int tagID, Tag [] tags) {
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].getID() == tagID) {
	m_Selected = i;
	m_Tags = tags;
	return;
      }
    }
    throw new IllegalArgumentException("Selected tag is not valid");
  }
  
  /**
   * Creates a new <code>SelectedTag</code> instance.
   *
   * @param tagText the text of the selected tag (case-insensitive).
   * @param tags an array containing the possible valid Tags.
   * @exception IllegalArgumentException if the selected tag isn't in the array
   * of valid values.
   */
  public SelectedTag(String tagText, Tag [] tags) {
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].getReadable().equalsIgnoreCase(tagText)) {
        m_Selected = i;
        m_Tags = tags;
        return;
      }
    }
    throw new IllegalArgumentException("Selected tag is not valid");
  }
  
  /** Returns true if this SelectedTag equals another object */
  public boolean equals(Object o) {
    if ((o == null) || !(o.getClass().equals(this.getClass()))) {
      return false;
    }
    SelectedTag s = (SelectedTag)o;
    if ((s.getTags() == m_Tags)
	&& (s.getSelectedTag() == m_Tags[m_Selected])) {
      return true;
    } else {
      return false;
    }
  }
  
  
  /**
   * Gets the selected Tag.
   *
   * @return the selected Tag.
   */
  public Tag getSelectedTag() {
    return m_Tags[m_Selected];
  }
  
  /**
   * Gets the set of all valid Tags.
   *
   * @return an array containing the valid Tags.
   */
  public Tag [] getTags() {
    return m_Tags;
  }
}
