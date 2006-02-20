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
 * Capabilities.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;

/**
 * a class that describes the capabilites (e.g., handling certain types of
 * attributes, missing values, types of classes, etc.) of a specific
 * classifier. By default, the classifier is capable of nothing. This
 * ensures that new features have to be enabled explicitly.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class Capabilities 
  implements Cloneable, Serializable {

  /** enumeration of all capabilities */
  public enum Capability {
    // attributes
    NOMINAL_ATTRIBUTES,
    BINARY_ATTRIBUTES,
    NUMERIC_ATTRIBUTES,
    DATE_ATTRIBUTES,
    STRING_ATTRIBUTES,
    RELATIONAL_ATTRIBUTES,
    MISSING_VALUES,
    // class
    NO_CLASS,
    NOMINAL_CLASS,
    BINARY_CLASS,
    NUMERIC_CLASS,
    DATE_CLASS,
    STRING_CLASS,
    RELATIONAL_CLASS,
    MISSING_CLASS_VALUES,
    // other
    ONLY_MULTIINSTANCE
  };

  /** the object that owns this capabilities instance */
  protected CapabilitiesHandler m_Owner;
  
  /** the hashset for storing the active capabilities */
  protected HashSet m_Capabilities;
  
  /** the reason why the test failed, used to throw an exception */
  protected Exception m_FailReason = null;

  /** the minimum number of instances in a dataset */
  protected int m_MinimumNumberInstances = 1;
  
  /**
   * initializes the capabilities for the given owner
   * 
   * @param owner       the object that produced this Capabilities instance
   */
  public Capabilities(CapabilitiesHandler owner) {
    super();

    setOwner(owner);
    m_Capabilities = new HashSet();
  }
  
  /**
   * Creates and returns a copy of this object.
   */
  public Object clone() {
    Capabilities    result;

    result = new Capabilities(m_Owner);
    result.assign(this);

    return result;
  }
  
  /**
   * retrieves the data from the given Capabilities object
   * 
   * @param c	  the capabilities object to initialize with
   */
  public void assign(Capabilities c) {
    for (Capability cap: Capability.values()) {
      if (c.handles(cap))
        enable(cap);
      else
	disable(cap);
    }

    setMinimumNumberInstances(c.getMinimumNumberInstances());
  }

  /**
   * performs an AND conjunction with the capabilities of the given 
   * Capabilities object and updates itself
   *
   * @param c     the capabilities to AND with
   */
  public void and(Capabilities c) {
    for (Capability cap: Capability.values()) {
      if (handles(cap) && c.handles(cap))
        m_Capabilities.add(cap);
      else
        m_Capabilities.remove(cap);
    }
    
    // minimum number of instances that both handlers need at least to work
    if (c.getMinimumNumberInstances() > getMinimumNumberInstances())
      setMinimumNumberInstances(c.getMinimumNumberInstances());
  }

  /**
   * performs an OR conjunction with the capabilities of the given 
   * Capabilities object and updates itself
   *
   * @param c     the capabilities to OR with
   */
  public void or(Capabilities c) {
    for (Capability cap: Capability.values()) {
      if (handles(cap) || c.handles(cap))
        m_Capabilities.add(cap);
      else
        m_Capabilities.remove(cap);
    }
    
    if (c.getMinimumNumberInstances() < getMinimumNumberInstances())
      setMinimumNumberInstances(c.getMinimumNumberInstances());
  }
  
  /**
   * Returns true if the currently set capabilities support at least all of
   * the capabiliites of the given Capabilities object (checks only the enum!)
   * 
   * @param c	the capabilities to support at least
   * @return	true if all the requested capabilities are supported
   */
  public boolean supports(Capabilities c) {
    boolean	result;
    
    result = true;
    
    for (Capability cap: Capability.values()) {
      if (c.handles(cap) && !handles(cap)) {
	result = false;
	break;
      }
    }

    return result;
  }

  /**
   * sets the owner of this capabilities object
   * 
   * @param value       the new owner
   */
  public void setOwner(CapabilitiesHandler value) {
    m_Owner = value;
  }
  
  /**
   * returns the owner of this capabilities object
   * 
   * @return            the current owner of this capabilites object
   */
  public CapabilitiesHandler getOwner() {
    return m_Owner;
  }

  /**
   * sets the minimum number of instances that have to be in the dataset
   * 
   * @param value       the minimum number of instances
   */
  public void setMinimumNumberInstances(int value) {
    if (value >= 0)
      m_MinimumNumberInstances = value;
  }
  
  /**
   * returns the minimum number of instances that have to be in the dataset
   * 
   * @return            the minimum number of instances
   */
  public int getMinimumNumberInstances() {
    return m_MinimumNumberInstances;
  }
  
  /**
   * enables the given capability
   *
   * @param c     the capability to enable
   */
  public void enable(Capability c) {
    if (c == Capability.NOMINAL_ATTRIBUTES) {
      enable(Capability.BINARY_ATTRIBUTES);
    }
    else if (c == Capability.NOMINAL_CLASS) {
      enable(Capability.BINARY_CLASS);
    }

    m_Capabilities.add(c);
  }
  
  /**
   * enables all class types
   * 
   * @see #disableAllClasses()
   * @see #getClassCapabilities()
   */
  public void enableAllClasses() {
    enable(Capability.NOMINAL_CLASS);
    enable(Capability.NUMERIC_CLASS);
    enable(Capability.DATE_CLASS);
    enable(Capability.STRING_CLASS);
    enable(Capability.RELATIONAL_CLASS);
  }
  
  /**
   * enables all attribute types
   * 
   * @see #disableAllAttributes()
   * @see #getAttributeCapabilities()
   */
  public void enableAllAttributes() {
    enable(Capability.NOMINAL_ATTRIBUTES);
    enable(Capability.NUMERIC_ATTRIBUTES);
    enable(Capability.DATE_ATTRIBUTES);
    enable(Capability.STRING_ATTRIBUTES);
    enable(Capability.RELATIONAL_ATTRIBUTES);
  }

  /**
   * disables the given capability
   *
   * @param c     the capability to disable
   */
  public void disable(Capability c) {
    if (c == Capability.NOMINAL_ATTRIBUTES) {
      disable(Capability.BINARY_ATTRIBUTES);
    }
    else if (c == Capability.NOMINAL_CLASS) {
      disable(Capability.BINARY_CLASS);
    }

    m_Capabilities.remove(c);
  }
  
  /**
   * disables all class types
   * 
   * @see #enableAllClasses()
   * @see #getClassCapabilities()
   */
  public void disableAllClasses() {
    disable(Capability.NOMINAL_CLASS);
    disable(Capability.NUMERIC_CLASS);
    disable(Capability.DATE_CLASS);
    disable(Capability.STRING_CLASS);
    disable(Capability.RELATIONAL_CLASS);
  }
  
  /**
   * disables all attribute types
   * 
   * @see #enableAllAttributes()
   * @see #getAttributeCapabilities()
   */
  public void disableAllAttributes() {
    disable(Capability.NOMINAL_ATTRIBUTES);
    disable(Capability.NUMERIC_ATTRIBUTES);
    disable(Capability.DATE_ATTRIBUTES);
    disable(Capability.STRING_ATTRIBUTES);
    disable(Capability.RELATIONAL_ATTRIBUTES);
  }
  
  /**
   * returns all class capabilities
   * 
   * @return		all capabilities regarding the class
   * @see #enableAllClasses()
   * @see #disableAllClasses()
   */
  public Capabilities getClassCapabilities() {
    Capabilities	result;
    
    result = new Capabilities(getOwner());
    
    if (handles(Capability.NOMINAL_CLASS))
      result.enable(Capability.NOMINAL_CLASS);
    if (handles(Capability.BINARY_CLASS))
      result.enable(Capability.BINARY_CLASS);
    if (handles(Capability.NUMERIC_CLASS))
      result.enable(Capability.NUMERIC_CLASS);
    if (handles(Capability.DATE_CLASS))
      result.enable(Capability.DATE_CLASS);
    if (handles(Capability.STRING_CLASS))
      result.enable(Capability.STRING_CLASS);
    if (handles(Capability.RELATIONAL_CLASS))
      result.enable(Capability.RELATIONAL_CLASS);
    if (handles(Capability.NO_CLASS))
      result.enable(Capability.NO_CLASS);
    if (handles(Capability.MISSING_CLASS_VALUES))
      result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }
  
  /**
   * returns all attribute capabilities
   * 
   * @return		all capabilities regarding attributes
   * @see #enableAllAttributes()
   * @see #disableAllAttributes()
   */
  public Capabilities getAttributeCapabilities() {
    Capabilities	result;
    
    result = new Capabilities(getOwner());
    
    if (handles(Capability.NOMINAL_ATTRIBUTES))
      result.enable(Capability.NOMINAL_ATTRIBUTES);
    if (handles(Capability.BINARY_ATTRIBUTES))
      result.enable(Capability.BINARY_ATTRIBUTES);
    if (handles(Capability.NUMERIC_ATTRIBUTES))
      result.enable(Capability.NUMERIC_ATTRIBUTES);
    if (handles(Capability.DATE_ATTRIBUTES))
      result.enable(Capability.DATE_ATTRIBUTES);
    if (handles(Capability.STRING_ATTRIBUTES))
      result.enable(Capability.STRING_ATTRIBUTES);
    if (handles(Capability.RELATIONAL_ATTRIBUTES))
      result.enable(Capability.RELATIONAL_ATTRIBUTES);
    if (handles(Capability.MISSING_VALUES))
      result.enable(Capability.MISSING_VALUES);
    
    return result;
  }
  
  /**
   * returns all other capabilities, besides class and attribute related ones
   * 
   * @return		all other capabilities, besides class and attribute 
   * 			related ones
   */
  public Capabilities getOtherCapabilities() {
    Capabilities	result;
    
    result = new Capabilities(getOwner());
    
    if (handles(Capability.ONLY_MULTIINSTANCE))
      result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * returns true if the classifier has the specified capability
   *
   * @param c     the capability to test
   * @return      true if the classifier has the capability
   */
  public boolean handles(Capability c) {
    return m_Capabilities.contains(c);
  }

  /**
   * Generates the message for, e.g., an exception. Adds the classname before the
   * actual message and returns that string.
   * 
   * @param msg       the actual content of the message, e.g., exception
   * @return          the new message
   */
  protected String createMessage(String msg) {
    String	result;
    
    result = "";
    
    if (getOwner() != null)
      result = getOwner().getClass().getName();
    else
      result = "<anonymous>";
      
    result += ": " + msg;
    
    return result;
  }
  
  /**
   * Tests the given data, whether it can be processed by the classifier,
   * given its capabilities. Classifiers implementing the 
   * <code>MultiInstanceCapabilitiesHandler</code> interface are checked 
   * automatically for their multi-instance Capabilities (if no bags, then
   * only the bag-structure, otherwise only the first bag).
   *
   * @param data 	the data to test
   * @return		true if all the tests succeeded
   * @see MultiInstanceCapabilitiesHandler
   */
  public boolean test(Instances data) {
    int         i;
    int         n;
    Attribute   att;
    Instance    inst;
    boolean     noClass;
    boolean     tmpResult;
    
    // no Capabilities? -> warning
    if (    (m_Capabilities.size() == 0) 
	 || ((m_Capabilities.size() == 1) && handles(Capability.NO_CLASS)) )
      System.err.println(createMessage("No capabilities set!"));
    
    // attributes
    if (data.numAttributes() == 0) {
      m_FailReason = new WekaException(
                          createMessage("No attributes!"));
      return false;
    }

    for (i = 0; i < data.numAttributes(); i++) {
      att = data.attribute(i);
      
      // class is handled separately
      if (i == data.classIndex())
        continue;
      
      // check attribute types
      switch (att.type()) {
        case Attribute.NOMINAL:
          // all types
          if (handles(Capability.NOMINAL_ATTRIBUTES))
            continue;
          
          // none
          if (    !handles(Capability.NOMINAL_ATTRIBUTES) 
               && !handles(Capability.BINARY_ATTRIBUTES) ) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle nominal attributes!"));
            return false;
          }
          
          // binary
          if (    handles(Capability.BINARY_ATTRIBUTES)
               && !handles(Capability.NOMINAL_ATTRIBUTES)
               && (att.numValues() != 2) ) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle non-binary attributes!"));
            return false;
          }
          break;

        case Attribute.NUMERIC:
          if (!handles(Capability.NUMERIC_ATTRIBUTES)) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle numeric attributes!"));
            return false;
          }
          break;

        case Attribute.DATE:
          if (!handles(Capability.DATE_ATTRIBUTES)) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle date attributes!"));
            return false;
          }
          break;

        case Attribute.STRING:
          if (!handles(Capability.STRING_ATTRIBUTES)) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle string attributes!"));
            return false;
          }
          break;

        case Attribute.RELATIONAL:
          if (!handles(Capability.RELATIONAL_ATTRIBUTES)) {
            m_FailReason = new UnsupportedAttributeTypeException(
                                createMessage("Cannot handle relational attributes!"));
            return false;
          }
          // attributes in the relation of this attribute must be tested
          // separately with a different Capabilites object
          break;

        default:
          m_FailReason = new UnsupportedAttributeTypeException(
                              createMessage("Cannot handle unknown attribute type '" 
                                          + att.type() + "'!"));
          return false;
      }
    }
    
    // class
    if (!handles(Capability.NO_CLASS)) {
      if (data.classIndex() == -1) {
        m_FailReason = new UnassignedClassException(
                            createMessage("Class attribute not set!"));
        return false;
      }
      
      att = data.classAttribute();

      // type
      switch (att.type()) {
        case Attribute.NOMINAL:
          // all types
          if (handles(Capability.NOMINAL_CLASS))
            break;
          
          // none
          if (    !handles(Capability.NOMINAL_CLASS) 
               && !handles(Capability.BINARY_CLASS) ) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle nominal class!"));
            return false;
          }
          
          // binary
          if (    handles(Capability.BINARY_CLASS) 
               && !handles(Capability.NOMINAL_CLASS)
               && (att.numValues() != 2) ) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle non-binary class!"));
            return false;
          }
          break;

        case Attribute.NUMERIC:
          if (!handles(Capability.NUMERIC_CLASS)) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle numeric class!"));
            return false;
          }
          break;

        case Attribute.DATE:
          if (!handles(Capability.DATE_CLASS)) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle date class!"));
            return false;
          }
          break;

        case Attribute.STRING:
          if (!handles(Capability.STRING_CLASS)) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle string class!"));
            return false;
          }
          break;

        case Attribute.RELATIONAL:
          if (!handles(Capability.RELATIONAL_CLASS)) {
            m_FailReason = new UnsupportedClassTypeException(
                                createMessage("Cannot handle relational class!"));
            return false;
          }
          
          // test recursively (but add ability to handle NO_CLASS)
          noClass = handles(Capability.NO_CLASS);
          if (!noClass)
            enable(Capability.NO_CLASS);
          tmpResult = test(att.relation());
          if (!noClass) 
            disable(Capability.NO_CLASS);
          if (!tmpResult)
            return false;
          
          break;

        default:
          m_FailReason = new UnsupportedClassTypeException(
                              createMessage("Cannot handle unknown attribute type '" + att.type() + "'!"));
          return false;
      }
      
      // missing class labels
      if (!handles(Capability.MISSING_CLASS_VALUES)) {
        for (i = 0; i < data.numInstances(); i++) {
          if (data.instance(i).classIsMissing()) {
            m_FailReason = new WekaException(
                createMessage("No training data without missing class values!"));
            return false;
          }
        }
      }
      else {
        int hasClass = 0;

        for (i = 0; i < data.numInstances(); i++) {
          if (!data.instance(i).classIsMissing())
            hasClass++;
        }
        
        // not enough instances with class labels?
        if (hasClass < getMinimumNumberInstances()) {
          m_FailReason = new WekaException(
                              createMessage("Not enough training instances with class labels (required: " 
                                          + getMinimumNumberInstances() 
                                          + ", provided: " 
                                          + hasClass + ")!"));
          return false;
        }
      }
    }

    // missing values
    if (!handles(Capability.MISSING_VALUES)) {
      for (i = 0; i < data.numInstances(); i++) {
        inst = data.instance(i);
        for (n = 0; n < inst.numAttributes(); n++) {
          // skip class
          if (n == inst.classIndex())
            continue;

          if (inst.isMissing(n)) {
            m_FailReason = new NoSupportForMissingValuesException(
                                createMessage("Cannot handle missing values!"));
            return false;
          }
        }
      }
    }
    
    // instances
    if (data.numInstances() < getMinimumNumberInstances()) {
      m_FailReason = new WekaException(
                          createMessage("Not enough training instances (required: " 
                                      + getMinimumNumberInstances() 
                                      + ", provided: " 
                                      + data.numInstances() + ")!"));
      return false;
    }

    // Multi-Instance? -> check structure
    if (handles(Capability.ONLY_MULTIINSTANCE)) {
      // number of attributes?
      if (data.numAttributes() != 3) {
        m_FailReason = new WekaException(
                            createMessage("Incorrect Multi-Instance format, must be 'bag-id, bag, class'!"));
        return false;
      }
      
      // type of attributes and position of class?
      if (    !data.attribute(0).isNominal() 
           || !data.attribute(1).isRelationValued() 
           || (data.classIndex() != data.numAttributes() - 1) ) {
        m_FailReason = new WekaException(
            createMessage("Incorrect Multi-Instance format, must be 'NOMINAL att, RELATIONAL att, CLASS att'!"));
        return false;
      }

      // check data immediately
      if (getOwner() instanceof MultiInstanceCapabilitiesHandler) {
	MultiInstanceCapabilitiesHandler handler = (MultiInstanceCapabilitiesHandler) getOwner();
	Capabilities cap = handler.getMultiInstanceCapabilities();
	boolean result;
	if (data.numInstances() > 0)
	  result = cap.test(data.attribute(1).relation(1));
	else
	  result = cap.test(data.attribute(1).relation());
	
	if (!result) {
	  m_FailReason = cap.m_FailReason;
	  return false;
	}
      }
    }
    
    // passed all tests!
    return true;
  }

  /**
   * tests the given data by calling the test(Instances) method and throws 
   * an exception if the test fails.
   *
   * @param data        the data to test
   * @throws Exception  in case the data doesn't pass the tests
   * @see #test(Instances)
   */
  public void testWithFail(Instances data) throws Exception {
    if (!test(data))
      throw m_FailReason;
  }
  
  /**
   * returns a string representation of the capabilities
   */
  public String toString() {
    Vector		sorted;
    StringBuffer	result;
    
    result = new StringBuffer();

    // capabilities
    sorted = new Vector(m_Capabilities);
    Collections.sort(sorted);
    result.append("Capabilities: " + sorted.toString() + "\n");
    
    // other stuff
    result.append("min # Instance: " + getMinimumNumberInstances() + "\n");
    
    return result.toString();
  }
  
  /**
   * returns a Capabilities object specific for this data. The multi-instance
   * capability is not checked as well as the minimum number of instances
   * is not set.
   * 
   * @param data	the data to base the capabilities on
   * @throws Exception	in case an error occurrs, e.g., an unknown attribute 
   * 			type
   */
  public static Capabilities forInstances(Instances data) throws Exception {
    Capabilities	result;
    int			i;
    int			n;
    
    result = new Capabilities(null);
    
    // class
    if (data.classIndex() == -1) {
      result.enable(Capability.NO_CLASS);
    }
    else {
      switch (data.classAttribute().type()) {
	case Attribute.NOMINAL:
	  if (data.classAttribute().numValues() == 2)
	    result.enable(Capability.BINARY_CLASS);
	  else
	    result.enable(Capability.NOMINAL_CLASS);
	  break;
	  
	case Attribute.NUMERIC:
	  result.enable(Capability.NUMERIC_CLASS);
	  break;
	  
	case Attribute.STRING:
	  result.enable(Capability.STRING_CLASS);
	  break;
	  
	case Attribute.DATE:
	  result.enable(Capability.DATE_CLASS);
	  break;
	  
	case Attribute.RELATIONAL:
	  result.enable(Capability.RELATIONAL_CLASS);
	  break;
	  
	default:
	  throw new UnsupportedAttributeTypeException("Unknown class attribute type '" + data.classAttribute() + "'!");
      }
      
      // missing class values
      for (i = 0; i < data.numInstances(); i++) {
	if (data.instance(i).classIsMissing()) {
	  result.enable(Capability.MISSING_CLASS_VALUES);
	  break;
	}
      }
    }
    
    // attributes
    if (data.checkForAttributeType(Attribute.NOMINAL)) {
      result.enable(Capability.BINARY_ATTRIBUTES);
      
      for (i = 0; i < data.numAttributes(); i++) {
	// skip class
	if (i == data.classIndex())
	  continue;

	// non-binary attributes?
	if (data.attribute(i).isNominal()) {
	  if (data.attribute(i).numValues() != 2) {
	    result.enable(Capability.NOMINAL_ATTRIBUTES);
	    break;
	  }
	}
      }
    }

    if (data.checkForAttributeType(Attribute.NUMERIC))
      result.enable(Capability.NUMERIC_ATTRIBUTES);
    
    if (data.checkForAttributeType(Attribute.STRING))
      result.enable(Capability.STRING_ATTRIBUTES);

    if (data.checkForAttributeType(Attribute.DATE))
      result.enable(Capability.DATE_ATTRIBUTES);

    if (data.checkForAttributeType(Attribute.RELATIONAL))
      result.enable(Capability.RELATIONAL_ATTRIBUTES);
    
    // missing values
    for (n = 0; n < data.numAttributes(); n++) {
      if (n == data.classIndex())
	continue;
      
      for (i = 0; i < data.numInstances(); i++) {
	if (data.instance(i).classIsMissing()) {
	  result.enable(Capability.MISSING_VALUES);
	  break;
	}
      }
    }

    return result;
  }
}
