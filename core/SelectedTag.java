/*
 *    SelectedTag.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

public class SelectedTag {
  
  /** The index of the selected tag */
  protected int m_Selected;
  
  /** The set of tags to choose from */
  protected Tag [] m_Tags;
  
  public SelectedTag(int tagID, Tag [] tags) throws Exception {
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].getID() == tagID) {
	m_Selected = i;
	m_Tags = tags;
	return;
      }
    }
    throw new Exception("Selected tag is not valid");
  }
  
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
  
  public Tag getSelectedTag() {
    return m_Tags[m_Selected];
  }
  
  public Tag [] getTags() {
    return m_Tags;
  }
}
