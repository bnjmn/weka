/*
 *    Tag.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

public class Tag {
  protected int m_ID;
  protected String m_Readable;
  public Tag(int ident, String readable) {
    m_ID = ident;
    m_Readable = readable;
  }
  public int getID() {
    return m_ID;
  }
  public String getReadable() {
    return m_Readable;
  }
}
