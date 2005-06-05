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
 * XMLBasicSerialization.java
 * Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.DefaultListModel;

import org.w3c.dom.Element;

/**
 * This serializer contains some read/write methods for common classes that
 * are not beans-conform. Currently supported are:
 * <ul>
 *    <li>java.util.HashMap</li>
 *    <li>java.util.HashSet</li>
 *    <li>java.util.Hashtable</li>
 *    <li>java.util.LinkedList</li>
 *    <li>java.util.Properties</li>
 *    <li>java.util.Stack</li>
 *    <li>java.util.TreeMap</li>
 *    <li>java.util.TreeSet</li>
 *    <li>java.util.Vector</li>
 *    <li>javax.swing.DefaultListModel</li>
 * </ul>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $ 
 */
public class XMLBasicSerialization extends XMLSerialization {

   /** the value for mapping, e.g., Maps */
   public final static String VAL_MAPPING = "mapping";

   /** the value for a mapping-key, e.g., Maps */
   public final static String VAL_KEY = "key";

   /** the value for mapping-value, e.g., Maps */
   public final static String VAL_VALUE = "value";

   /**
    * initializes the serialization
    * 
    * @throws Exception if initialization fails
    */
   public XMLBasicSerialization() throws Exception {
      super();
   }
   
   /**
    * generates internally a new XML document and clears also the IgnoreList
    * and the mappings for the Read/Write-Methods
    */
   public void clear() throws Exception {
      super.clear();
      
      // DefaultListModel
      m_CustomMethods.read().add(
            DefaultListModel.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readDefaultListModel"));
      m_CustomMethods.write().add(
            DefaultListModel.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeDefaultListModel"));

      // HashMap
      m_CustomMethods.read().add(
            HashMap.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readMap"));
      m_CustomMethods.write().add(
            HashMap.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeMap"));

      // HashSet
      m_CustomMethods.read().add(
            HashSet.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readCollection"));
      m_CustomMethods.write().add(
            HashSet.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeCollection"));

      // Hashtable
      m_CustomMethods.read().add(
            Hashtable.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readMap"));
      m_CustomMethods.write().add(
            Hashtable.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeMap"));

      // LinkedList
      m_CustomMethods.read().add(
            LinkedList.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readCollection"));
      m_CustomMethods.write().add(
            LinkedList.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeCollection"));

      // Properties
      m_CustomMethods.read().add(
            Properties.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readMap"));
      m_CustomMethods.write().add(
            Properties.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeMap"));

      // Stack
      m_CustomMethods.read().add(
            Stack.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readCollection"));
      m_CustomMethods.write().add(
            Stack.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeCollection"));

      // TreeMap
      m_CustomMethods.read().add(
            TreeMap.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readMap"));
      m_CustomMethods.write().add(
            TreeMap.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeMap"));

      // TreeSet
      m_CustomMethods.read().add(
            TreeSet.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readCollection"));
      m_CustomMethods.write().add(
            TreeSet.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeCollection"));

      // Vector
      m_CustomMethods.read().add(
            Vector.class, 
            XMLSerializationMethodHandler.findReadMethod(
               this, "readCollection"));
      m_CustomMethods.write().add(
            Vector.class, 
            XMLSerializationMethodHandler.findWriteMethod(
               this, "writeCollection"));
   }
   
   /**
    * adds the given DefaultListModel to a DOM structure. 
    * 
    * @param parent the parent of this object, e.g. the class this object is a
    * member of
    * @param o the Object to describe in XML
    * @param name the name of the object
    * @throws Exception if the DOM creation fails
    * @see javax.swing.DefaultListModel
    */
   public void writeDefaultListModel(Element parent, Object o, String name) 
      throws Exception {

      Element              node;
      int                  i;
      DefaultListModel     model;

      model = (DefaultListModel) o;
      node = addElement(parent, name, o.getClass().getName(), false, false);

      for (i = 0; i < model.getSize(); i++)
         invokeWriteToXML(node, model.get(i), Integer.toString(i));
   }

   /**
    * builds the DefaultListModel from the given DOM node. 
    * 
    * @param parent the parent object to get the properties for
    * @param node the associated XML node
    * @return the instance created from the XML description
    * @throws Exception if instantiation fails 
    * @see javax.swing.DefaultListModel
    */
   public Object readDefaultListModel(Element node) throws Exception {
      DefaultListModel     model;
      Vector               children;
      Element              child;
      int                  i;

      children = XMLDocument.getChildTags(node); 
      model    = new DefaultListModel();
      model.setSize(children.size());

      for (i = 0; i < children.size(); i++) {
         child = (Element) children.get(i);
         model.set(
             Integer.parseInt(child.getAttribute(ATT_NAME)), 
             invokeReadFromXML(child));
      }
      
      return model;
   }
   
   /**
    * adds the given Collection to a DOM structure. 
    * 
    * @param parent the parent of this object, e.g. the class this object is a
    * member of
    * @param o the Object to describe in XML
    * @param name the name of the object
    * @throws Exception if the DOM creation fails
    * @see javax.swing.DefaultListModel
    */
   public void writeCollection(Element parent, Object o, String name) 
      throws Exception {

      Element         node;
      Iterator        iter;
      int             i;

      iter = ((Collection) o).iterator();
      node = addElement(parent, name, o.getClass().getName(), false, false);

      i = 0;
      while (iter.hasNext()) {
         invokeWriteToXML(node, iter.next(), Integer.toString(i));
         i++;
      }
   }

   /**
    * builds the Collection from the given DOM node. 
    * 
    * @param parent the parent object to get the properties for
    * @param node the associated XML node
    * @return the instance created from the XML description
    * @throws Exception if instantiation fails 
    * @see javax.swing.DefaultListModel
    */
   public Object readCollection(Element node) throws Exception {
      Collection           coll;
      Vector               v;
      Vector               children;
      Element              child;
      int                  i;

      children = XMLDocument.getChildTags(node); 
      v        = new Vector();
      v.setSize(children.size());

      // put the children in the vector to sort them according their index
      for (i = 0; i < children.size(); i++) {
         child = (Element) children.get(i);
         v.set(
               Integer.parseInt(child.getAttribute(ATT_NAME)), 
               invokeReadFromXML(child));
      }
      
      // populate collection
      coll = (Collection) Class.forName(
                  node.getAttribute(ATT_CLASS)).newInstance();
      coll.addAll(v);
      
      return coll;
   }
   
   /**
    * adds the given Map to a DOM structure. 
    * 
    * @param parent the parent of this object, e.g. the class this object is a
    * member of
    * @param o the Object to describe in XML
    * @param name the name of the object
    * @throws Exception if the DOM creation fails
    * @see javax.swing.DefaultListModel
    */
   public void writeMap(Element parent, Object o, String name) 
      throws Exception {

      Map            map;
      Object         key;
      Element        node;
      Element        child;
      Iterator       iter;

      map  = (Map) o;
      iter = map.keySet().iterator();
      node = addElement(parent, name, o.getClass().getName(), false, false);

      while (iter.hasNext()) {
         key   = iter.next();
         child = addElement(
                     node, VAL_MAPPING, Object.class.getName(), false, false);
         invokeWriteToXML(child, key,          VAL_KEY);
         invokeWriteToXML(child, map.get(key), VAL_VALUE);
      }
   }

   /**
    * builds the Map from the given DOM node. 
    * 
    * @param parent the parent object to get the properties for
    * @param node the associated XML node
    * @return the instance created from the XML description
    * @throws Exception if instantiation fails 
    * @see javax.swing.DefaultListModel
    */
   public Object readMap(Element node) throws Exception {
      Map                  map;
      Object               key;
      Object               value;
      Vector               children;
      Vector               cchildren;
      Element              child;
      Element              cchild;
      int                  i;
      int                  n;
      String               name;

      map      = (Map) Class.forName(
                     node.getAttribute(ATT_CLASS)).newInstance();
      children = XMLDocument.getChildTags(node); 

      for (i = 0; i < children.size(); i++) {
         child     = (Element) children.get(i);
         cchildren = XMLDocument.getChildTags(child);
         key       = null;
         value     = null;
         
         for (n = 0; n < cchildren.size(); n++) {
            cchild = (Element) cchildren.get(n);
            name   = cchild.getAttribute(ATT_NAME);
            if (name.equals(VAL_KEY))
               key = invokeReadFromXML(cchild);
            else if (name.equals(VAL_VALUE))
               value = invokeReadFromXML(cchild);
            else
               System.out.println("WARNING: '" 
                     + name + "' is not a recognized name for maps!");
         }
         
         map.put(key, value);
      }
      
      return map;
   }
   
   /**
    * for testing only 
    */
   public static void main(String[] args) throws Exception {
   }
}
