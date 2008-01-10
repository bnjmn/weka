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
 * XStream.java
 * Copyright (C) 2008 Pentaho Corporation
 */

package weka.core.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.lang.StringBuffer;

/**
 * This class is a helper class for XML serialization using 
 * <a href="http://xstream.codehaus.org" target="_blank">XStream</a> .
 * XStream does not need to be present, since the class-calls are done generically via Reflection.
 *
 * @author Mark Hall (mhall[{at}]pentao[{dot}]org
 * @version $Revision: 1.1 $
 */
public class XStream {

  /**
   * indicates whether <a href="http://xstream.codehaus.org" target="_blank">XStream</a> 
   * is present
   */
  protected static boolean m_Present = false;

  /** the extension for XStream files (including '.') */
  public final static String FILE_EXTENSION = ".xstream";
   
  /** check for XStream statically (needs only to be done once) */
  static {
    checkForXStream();
  }

  /**
   * checks whether the KOML is present in the class path
   */
  private static void checkForXStream() {
    try {
      Class.forName("com.thoughtworks.xstream.XStream");
      m_Present = true;
    }
    catch (Exception e) {
      m_Present = false;
    }
  }
  
  /**
   * returns whether KOML is present or not, i.e. whether the classes are in the
   * classpath or not
   *
   * @return whether KOML is available
   */
  public static boolean isPresent() {
    return m_Present;
  }

  /**
   * reads the XML-serialized object from the given file
   * @param filename the file to deserialize the object from
   * @return the deserialized object
   * @throws Exception if something goes wrong while reading from the file
   */
  public static Object read(String filename) throws Exception {
    return read(new FileInputStream(filename));
  }
  
  /**
   * reads the XML-serialized object from the given file
   * @param file the file to deserialize the object from
   * @return the deserialized object
   * @throws Exception if something goes wrong while reading from the file
   */
  public static Object read(File file) throws Exception {
    return read(new FileInputStream(file));
  }

  /**
   * reads the XML-serialized object from the given input stream
   *
   * @param stream the input stream
   * @return the deserialized object
   * @throws Exception if something goes wrong while reading from stream
   */
  public static Object read(InputStream stream) throws Exception {
    InputStreamReader isr = new InputStreamReader(stream);
    BufferedReader br = new BufferedReader(isr);
    String line;
    StringBuffer buff = new StringBuffer();
    
    while ((line = br.readLine()) != null) {
      buff.append(line+"\n");
    }
    br.close();
    
    String xml = buff.toString();

    Object result = deSerialize(xml);
    
    return result;
  }

  /**
   * writes the XML-serialized object to the given file
   * @param filename the file to serialize the object to
   * @param o the object to write to the file
   * @return whether writing was successful or not
   * @throws Exception if something goes wrong while writing to the file
   */
  public static boolean write(String filename, Object o) throws Exception {
    return write(new FileOutputStream(filename), o);
  }
  
  /**
   * write the XML-serialized object to the given file
   * @param file the file to serialize the object to
   * @param o the object to write to the file
   * @return whether writing was successful or not
   * @throws Exception if something goes wrong while writing to the file
   */
  public static boolean write(File file, Object o) throws Exception {
    return write(new FileOutputStream(file), o);
  }

  /**
   * writes the XML-serialized object to the given output stream
   *
   * @param stream the output stream
   * @param o the object to write
   * @return true if everything goes ok
   */
  public static boolean write(OutputStream stream, Object o) {
    BufferedOutputStream bos = new BufferedOutputStream(stream);
    PrintWriter pw = new PrintWriter(bos, true);
    boolean result = false;
    
    try {
      String xmlOut = serialize(o);
      
      // write that sucker...
      pw.println(xmlOut);
      pw.close();
      result = true;
    } catch (Exception ex) {
      result = false;
      pw.close();
    }

    return result;
  }

  /**
   * Serializes the supplied object xml
   *
   * @param toSerialize the object to serialize
   * @return the serialized object as an XML string
   * @throws Exception if something goes wrong
   */
  public static String serialize(Object toSerialize) throws Exception {
    Class xstreamClass;
    java.lang.reflect.Constructor constructor;
    Object xstream;
    Class [] serializeArgsClasses = new Class[1];
    Object [] serializeArgs = new Object[1];
    java.lang.reflect.Method methodSerialize;
    String result;
    
    xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
    constructor = xstreamClass.getConstructor();
    xstream = constructor.newInstance();

    serializeArgsClasses[0] = Object.class;
    serializeArgs[0] = toSerialize;
    methodSerialize = xstreamClass.getMethod("toXML", serializeArgsClasses);
    
    // execute it
    try {
      result = (String)methodSerialize.invoke(xstream, serializeArgs);
    } catch (Exception ex) {
      result = null;
    }

    return result;
  }

  /**
   * Deserializes an object from the supplied XML string
   * 
   * @param xmlString the XML to deserialize from
   * @return the deserialized object
   * @throws Exception if something goes wrong
   */
  public static Object deSerialize(String xmlString) throws Exception {
    Class xstreamClass;
    java.lang.reflect.Constructor constructor;
    Object xstream;
    Class [] deSerializeArgsClasses = new Class[1];
    Object [] deSerializeArgs = new Object[1];
    java.lang.reflect.Method methodDeSerialize;
    Object result;

    xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
    constructor = xstreamClass.getConstructor();
    xstream = constructor.newInstance();

    deSerializeArgsClasses[0] = String.class;
    deSerializeArgs[0] = xmlString;
    methodDeSerialize = xstreamClass.getMethod("fromXML", deSerializeArgsClasses);

    // execute it
    try {
      result = methodDeSerialize.invoke(xstream, deSerializeArgs);
    } catch (Exception ex) {
      ex.printStackTrace();
      result = null;
    }

    return result;
  }
}