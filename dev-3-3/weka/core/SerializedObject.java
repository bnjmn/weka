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
 *    SerializedObject.java
 *    Copyright (C) 2000 Webmind Inc.
 *
 */

package weka.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class stores an object serialized in memory. It allows compression,
 * to be used to conserve memory (for example, when storing large strings
 * in memory), or can be used as a mechanism for deep copying objects.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.4 $
 */
public class SerializedObject implements Serializable {

  /** Stores the serialized object */
  private byte [] m_Serialized;

  /** True if the object has been compressed during storage */
  private boolean m_Compressed;

  /**
   * Serializes the supplied object into a byte array without compression.
   *
   * @param obj the Object to serialize.
   * @exception Exception if the object is not Serializable.
   */
  public SerializedObject(Object obj) throws Exception {

    this(obj, false);
  }

  /**
   * Serializes the supplied object into a byte array.
   *
   * @param obj the Object to serialize.
   * @param compress true if the object should be stored compressed.
   * @exception Exception if the object is not Serializable.
   */
  public SerializedObject(Object obj, boolean compress) throws Exception {

    //System.err.print("."); System.err.flush();
    m_Compressed = compress;
    m_Serialized = toByteArray(obj, m_Compressed);
  }

  /**
   * Serializes the supplied object to a byte array.
   *
   * @param obj the Object to serialize
   * @param compress true if the object should be compressed.
   * @return the byte array containing the serialized object.
   * @exception Exception if the object is not Serializable.
   */
  protected static byte [] toByteArray(Object obj, boolean compress) 
    throws Exception {

    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    OutputStream os = bo;
    if (compress) {
      os = new GZIPOutputStream(os);
    }
    os = new BufferedOutputStream(os);
    ObjectOutputStream oo = new ObjectOutputStream(os);
    oo.writeObject(obj);
    oo.close();
    return bo.toByteArray();
  }

  /**
   * Gets the object stored in this SerializedObject. The object returned
   * will be a deep copy of the original stored object.
   *
   * @return the deserialized Object.
   */
  public Object getObject() {
    try {
      InputStream is = new ByteArrayInputStream(m_Serialized);
      if (m_Compressed) {
        is = new GZIPInputStream(is);
      }
      is = new BufferedInputStream(is);
      ObjectInputStream oi = new ObjectInputStream(is);
      Object result = oi.readObject();
      oi.close();
      return result;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Compares this object with another for equality.
   *
   * @param other the other Object.
   * @return true if the objects are equal.
   */
  public final boolean equals(Object other) {

    // Check class type
    if ((other == null) || !(other.getClass().equals(this.getClass()))) {
      return false;
    }
    // Check serialized length
    byte [] os = ((SerializedObject)other).m_Serialized;
    if (os.length != m_Serialized.length) {
      return false;
    }
    // Check serialized contents
    for (int i = 0; i < m_Serialized.length; i++) {
      if (m_Serialized[i] != os[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a hashcode for this object.
   *
   * @return the hashcode for this object.
   */
  public final int hashCode() {
    
    return m_Serialized.length;
  }

  /**
   * Returns a text representation of the state of this object.
   *
   * @return a String representing this object.
   */
  public String toString() {
    
    return (m_Compressed ? "Compressed object: " : "Uncompressed object: ")
      + m_Serialized.length + " bytes";
  }

  /**
   * Test routine, reads text from stdin and measures memory usage
   */
  public static void main2(String []args) {

    try {
      Runtime r = Runtime.getRuntime();
      r.gc();
      java.io.LineNumberReader lnr = new java.io.LineNumberReader(new java.io.InputStreamReader(System.in));
      StringBuffer sb = new StringBuffer();
      String line;
      while ((line = lnr.readLine()) != null) {
        sb.append(line).append('\n');
      }
      String text = sb.toString();
      //System.err.println("TEXT:");
      //System.err.println(text);
      r.gc();

      System.err.print("TOTAL: " + r.totalMemory());
      System.err.println("\tFREE: " + r.freeMemory());
      long used = r.totalMemory() - r.freeMemory();
      
      // convert to a compressedobject
      SerializedObject co = new SerializedObject(text, true);

      System.err.print("TOTAL: " + r.totalMemory());
      System.err.println("\tFREE: " + r.freeMemory());
      r.gc();
      long used1 = r.totalMemory() - r.freeMemory();
      long csize = used1 - used;
      System.err.println("CompressedSize = " + csize);

      String newstr = (String)co.getObject();
      r.gc();
      System.err.print("TOTAL: " + r.totalMemory());
      System.err.println("\tFREE: " + r.freeMemory());
      long used2 = r.totalMemory() - r.freeMemory();
      long usize = used2 - used1;
      System.err.println("UncompressedSize = " + usize);

      // A couple of references to the original objects
      // so they don't get freed prematurely and muck up the
      // measurements.
      newstr = newstr.toLowerCase();
      text = text.toLowerCase();
      System.err.println("Byte array: " + co.toString());
      System.err.println("Length of text: " + text.length());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  /**
   * Test routine, reads an arff file from stdin and measures memory usage
   * (the arff file should have long string attribute values)
   */
  public static void main(String []args) {

    try {
      Runtime r = Runtime.getRuntime();
      r.gc();
      System.err.print("TOTAL: " + r.totalMemory());
      System.err.println("\tFREE: " + r.freeMemory());
      long used = r.totalMemory() - r.freeMemory();
      Instances inst = new Instances(new java.io.BufferedReader(new java.io.InputStreamReader(System.in)));
      r.gc();
      long used1 = r.totalMemory() - r.freeMemory();
      long csize = used1 - used;
      System.err.print("\nTOTAL: " + r.totalMemory());
      System.err.println("\tFREE: " + r.freeMemory());
      System.err.println("size = " + csize);
      int blah = inst.numAttributes();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
