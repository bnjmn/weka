/*
 *    SelectedTag.java
 *    Copyright (C) 1999 Len Trigg
 *
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
