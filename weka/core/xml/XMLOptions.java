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
 * XMLOptions.java
 * Copyright (C) 2004 FracPete
 *
 */

package weka.core.xml;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class for transforming options listed in XML to a regular WEKA command
 * line string.<p>
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.2 $
 */
public class XMLOptions {
   /** tag for a single option */
   public final static String TAG_OPTION = "option";

   /** tag for a list of options */
   public final static String TAG_OPTIONS = "options";
   
   /** the name attribute */
   public final static String ATT_NAME = "name";
   
   /** the type attribute */
   public final static String ATT_TYPE = "type";
   
   /** the value attribute */
   public final static String ATT_VALUE = "value";
   
   /** a value of the type attribute */
   public final static String VAL_TYPE_FLAG = "flag";
   
   /** a value of the type attribute */
   public final static String VAL_TYPE_SINGLE = "single";
   
   /** a value of the type attribute */
   public final static String VAL_TYPE_HYPHENS = "hyphens";
   
   /** a value of the type attribute */
   public final static String VAL_TYPE_QUOTES = "quotes";
   
   /** a value of the type attribute */
   public final static String VAL_TYPE_CLASSIFIER = "classifier";

   /** the root node */
   public final static String ROOT_NODE = TAG_OPTIONS;
   
   /** the DTD for the XML file */
   public final static String DOCTYPE = 
        "<!DOCTYPE " + ROOT_NODE + "\n"
      + "[\n"
      + "   <!ELEMENT " + TAG_OPTIONS + " (" + TAG_OPTION + ")*>\n"
      + "   <!ATTLIST " + TAG_OPTIONS + " " + ATT_TYPE + " CDATA \"classifier\">\n"
      + "   <!ATTLIST " + TAG_OPTIONS + " " + ATT_VALUE + " CDATA \"\">\n"
      + "   <!ELEMENT " + TAG_OPTION + " (#PCDATA | " + TAG_OPTIONS + ")*>\n"
      + "   <!ATTLIST " + TAG_OPTION + " " + ATT_NAME + " CDATA #REQUIRED>\n"
      + "   <!ATTLIST " + TAG_OPTION + " " + ATT_TYPE + " (flag | single | hyphens | quotes) \"single\">\n"
      + "]\n"
      + ">";

   /** the XML document */
   protected XMLDocument m_XMLDocument = null;
   
   /** 
    * Creates a new instance of XMLOptions 
    * @throws Exception if the construction of the DocumentBuilder fails
    * @see #setValidating(boolean)
    */
   public XMLOptions() throws Exception {
      m_XMLDocument = new XMLDocument(); 
      m_XMLDocument.setRootNode(ROOT_NODE);
      m_XMLDocument.setDocType(DOCTYPE);
      setValidating(true);
   }
   
   /** 
    * Creates a new instance of XMLOptions 
    * @param xml the xml to parse (if "<?xml" is not found then it is considered a file)
    * @throws Exception if the construction of the DocumentBuilder fails
    * @see #setValidating(boolean)
    */
   public XMLOptions(String xml) throws Exception {
      this();
      getXMLDocument().read(xml);
   }
   
   /** 
    * Creates a new instance of XMLOptions 
    * @param file the XML file to parse
    * @throws Exception if the construction of the DocumentBuilder fails
    * @see #setValidating(boolean)
    */
   public XMLOptions(File file) throws Exception {
      this();
      getXMLDocument().read(file);
   }
   
   /** 
    * Creates a new instance of XMLOptions 
    * @param stream the XML stream to parse
    * @throws Exception if the construction of the DocumentBuilder fails
    * @see #setValidating(boolean)
    */
   public XMLOptions(InputStream stream) throws Exception {
      this();
      getXMLDocument().read(stream);
   }
   
   /** 
    * Creates a new instance of XMLOptions 
    * @param reader the XML reader to parse
    * @throws Exception if the construction of the DocumentBuilder fails
    * @see #setValidating(boolean)
    */
   public XMLOptions(Reader reader) throws Exception {
      this();
      getXMLDocument().read(reader);
   }
   
   /**
    * returns whether a validating parser is used
    * @return whether a validating parser is used
    */
   public boolean getValidating() {
      return m_XMLDocument.getValidating();
   }
   
   /**
    * sets whether to use a validating parser or not. <br>
    * Note: this does clear the current DOM document! 
    * @param validating whether to use a validating parser
    * @throws Exception if the instantiating of the DocumentBuilder fails
    */
   public void setValidating(boolean validating) throws Exception {
       m_XMLDocument.setValidating(validating);
   }
   
   /**
    * returns the parsed DOM document
    * @return the parsed DOM document
    */
   public Document getDocument() {
      return fixHyphens(m_XMLDocument.getDocument());
   }
   
   /**
    * returns the handler of the XML document. the internal DOM document can 
    * be accessed via the <code>getDocument()</code> method.
    * @return the object handling the XML document
    * @see #getDocument()
    */
   public XMLDocument getXMLDocument() {
       return m_XMLDocument;
   }
   
   /**
    * pushes any options with type ATT_HYPHENS to the end, s.t. the "--" are
    * really added at the end
    * @param document the DOM document to work on
    * @return the fixed DOM document
    */
   protected Document fixHyphens(Document document) {
      NodeList          list;
      Vector            hyphens;
      int               i;
      Node              node;
      Node              tmpNode;
      boolean           isLast;
      
      // get all option tags
      list = document.getDocumentElement().getElementsByTagName(TAG_OPTION);
      
      // get all hyphen tags
      hyphens = new Vector();
      for (i = 0; i < list.getLength(); i++) {
         if (((Element) list.item(i)).getAttribute(ATT_TYPE).equals(VAL_TYPE_HYPHENS))
            hyphens.add(list.item(i));
      }
      
      // check all hyphen tags whether they are the end, if not fix it
      for (i = 0; i < hyphens.size(); i++) {
         node = (Node) hyphens.get(i);
         
         // at the end?
         isLast  = true;
         tmpNode = node;
         while (tmpNode.getNextSibling() != null) {
            // normal tag?
            if (tmpNode.getNextSibling().getNodeType() == Node.ELEMENT_NODE) {
               isLast = false;
               break;
            }
            tmpNode = tmpNode.getNextSibling();
         }
         
         // move
         if (!isLast) {
            tmpNode = node.getParentNode();
            tmpNode.removeChild(node);
            tmpNode.appendChild(node);
         }
      }
      
      return document;
   }
   
   /**
    * returns the quotes level for the given node, i.e. it returns the number 
    * of option's of the type "quotes" are in the path
    */
   protected int getQuotesLevel(Node node) {
      int         result;
      
      result = 0;
      while (node.getParentNode() != null) {
         if (!(node instanceof Element))
            continue;
         
         // option-tag?
         if (node.getNodeName().equals(TAG_OPTION)) {
            // types = quotes?
            if (((Element) node).getAttribute(ATT_TYPE).equals(VAL_TYPE_QUOTES))
               result++;
         }

         node = node.getParentNode();
      }
      
      return result;
   }
   
   /**
    * converts the given node into a command line representation and adds it
    * to the existing command line
    * @param cl the command line so far
    * @param parent the node to convert to command line
    * @param depth the current depth
    * @return the new command line
    */
   protected String toCommandLine(String cl, Element parent, int depth) {
      String            newCl;
      String            tmpCl;
      int               i;
      Vector            list;
      Vector            subList;
      NodeList          subNodeList;
      Element           node;
      
      newCl = "";

      // options
      if (parent.getNodeName().equals(TAG_OPTIONS)) {
         // classifier? -> add
         if (parent.getAttribute(ATT_TYPE).equals(VAL_TYPE_CLASSIFIER)) {
            newCl += parent.getAttribute(ATT_VALUE);
         }
         
         // process children
         list = XMLDocument.getChildTags(parent);
         for (i = 0; i < list.size(); i++)
            newCl = toCommandLine(newCl, (Element) list.get(i), depth + 1);
      }
      else
      // option
      if (parent.getNodeName().equals(TAG_OPTION)) {
         newCl       += " -" +  parent.getAttribute(ATT_NAME);
         subList      = XMLDocument.getChildTags(parent);
         subNodeList  = parent.getChildNodes();

         if (parent.getAttribute(ATT_TYPE).equals(VAL_TYPE_SINGLE)) {
            if ( (subNodeList.getLength() > 0) && (!subNodeList.item(0).getNodeValue().trim().equals("")) )
               newCl += " " + subNodeList.item(0).getNodeValue().trim();
         }
         else
         if (parent.getAttribute(ATT_TYPE).equals(VAL_TYPE_HYPHENS)) {
            newCl   += " " + ((Element) subList.get(0)).getAttribute(ATT_VALUE);  // expects classifier
            // get single options in this node
            subList  = XMLDocument.getChildTags((Element) subList.get(0));
            // get options after --
            tmpCl  = "";
            for (i = 0; i < subList.size(); i++)
               tmpCl = toCommandLine(tmpCl, (Element) subList.get(i), depth + 1);
            // add options
            tmpCl = tmpCl.trim();
            if (!tmpCl.equals(""))
               newCl += " -- " + tmpCl;
         }
         else
         if (parent.getAttribute(ATT_TYPE).equals(VAL_TYPE_QUOTES)) {
            newCl += " ";
            // opening quote
            for (i = 1; i < getQuotesLevel(parent); i++)
               newCl += "\\";
            newCl += "\"";
            // options
            tmpCl = "";
            for (i = 0; i < subList.size(); i++)
               tmpCl = toCommandLine(tmpCl, (Element) subList.get(i), depth + 1);
            newCl += tmpCl.trim();
            // closing quote
            for (i = 1; i < getQuotesLevel(parent); i++)
               newCl += "\\";
            newCl += "\"";
         }
      }
      
      // add to existing command line
      cl += " " + newCl.trim();
      
      return cl.trim();
   }
   
   /**
    * returns the given DOM document as command line
    * @return the document as command line
    * @throws Exception if anything goes wrong initializing the parsing
    */
   public String toCommandLine() throws Exception {
      return toCommandLine(new String(), getDocument().getDocumentElement(), 0);
   }
   
   /**
    * returns the current DOM document as string array (takes care of quotes!)
    * @return the document as string array
    * @throws Exception if anything goes wrong initializing the parsing
    */
   public String[] toArray() throws Exception {
       String         cl;
       Vector         result;
       boolean        quotes;
       boolean        backslash;
       boolean        add;
       int            i;
       String         tmpStr;
       
       cl     = toCommandLine();
       result = new Vector();
       
       // break up string
       quotes    = false;
       backslash = false;
       tmpStr    = "";
       for (i = 0; i < cl.length(); i++) {
          add = true;
          
          switch (cl.charAt(i)) {
             case '\\' :
                backslash = true;
                break;
                
             case '"' :
                // can we toggle quotes? (ignore nested quotes)
                if (!backslash) {
                   quotes = !quotes;
                   add    = false;
                }
                backslash = false;
                break;
               
             case ' ' :
                // if not quoted then break!
                if (!quotes) {
                   result.add(tmpStr.replaceAll("\\\\\"", "\""));
                   add    = false;
                   tmpStr = "";
                }
                break;
          }

          if (add)
             tmpStr += "" + cl.charAt(i);
       }
       
       // add last part
       if (!tmpStr.equals(""))
          result.add(tmpStr);
       
       return (String[]) result.toArray(new String[1]);
   }
   
   /**
    * returns the object in a string representation (as indented XML output)
    * 
    * @return the object in a string representation
    */
   public String toString() {
      return getXMLDocument().toString();
   }
   
   /**
    * for testing only. prints the given XML, the resulting commandline and
    * the string array.
    */
   public static void main(String[] args) throws Exception {
      if (args.length > 0) {
         System.out.println("\nXML:\n\n" + new XMLOptions(args[0]).toString()); 
          
         System.out.println("\nCommandline:\n\n" + new XMLOptions(args[0]).toCommandLine());
         
         System.out.println("\nString array:\n");
         String[] options = new XMLOptions(args[0]).toArray();
         for (int i = 0; i < options.length; i++)
            System.out.println(options[i]);
      }
   }
}
