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
 * @author <a href="mailto:len@webmind.com">Len Trigg</a> 
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
