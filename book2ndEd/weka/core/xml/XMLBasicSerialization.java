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
 * Copyright (C) 2004 FracPete
 *
 */

package weka.core.xml;

import java.util.Vector;

import javax.swing.DefaultListModel;

import org.w3c.dom.Element;

/**
 * This serializer contains some read/write methods for common classes that
 * are not beans-conform.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $ 
 */
public class XMLBasicSerialization extends XMLSerialization {

   /**
    * initializes the serialization
    * 
    * @throws Exception if initialization fails
    */
   public XMLBasicSerialization() throws Exception {
      super();
   }
   
   /**
    * generates internally a new XML document and clears also the IgnoreList and
    * the mappings for the Read/Write-Methods
    */
   public void clear() throws Exception {
      super.clear();
      
      m_CustomMethods.read().add(DefaultListModel.class, XMLSerializationMethodHandler.findReadMethod(this, "readDefaultListModel"));
      m_CustomMethods.write().add(DefaultListModel.class, XMLSerializationMethodHandler.findWriteMethod(this, "writeDefaultListModel"));
   }
   
   /**
    * adds the given DefaultListModel to a DOM structure. 
    * 
    * @param parent the parent of this object, e.g. the class this object is a member of
    * @param o the Object to describe in XML
    * @param name the name of the object
    * @throws Exception if the DOM creation fails
    * @see javax.swing.DefaultListModel
    */
   public void writeDefaultListModel(Element parent, Object o, String name) throws Exception {
      Element              node;
      Element              child;
      int                  i;
      DefaultListModel     model;

      model = (DefaultListModel) o;
      node  = (Element) parent.appendChild(m_Document.getDocument().createElement(TAG_OBJECT));

      node.setAttribute(ATT_NAME, name);
      node.setAttribute(ATT_CLASS, o.getClass().getName());
      node.setAttribute(ATT_PRIMITIVE, VAL_NO);
      node.setAttribute(ATT_ARRAY, VAL_NO);

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
      Object               result;
      DefaultListModel     model;
      Vector               children;
      Element              child;
      int                  i;

      result   = null;
      children = XMLDocument.getChildTags(node); 
      model    = new DefaultListModel();
      model.setSize(children.size());

      for (i = 0; i < children.size(); i++) {
         child = (Element) children.get(i);
         model.set(Integer.parseInt(child.getAttribute(ATT_NAME)), invokeReadFromXML(child));
      }
      
      return model;
   }
   
   /**
    * for testing only 
    */
   public static void main(String[] args) throws Exception {
   }
}
