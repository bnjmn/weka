/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Element.java
 * Copyright (C) 2007 Sebastian Beer
 *
 */

package weka.associations.gsp;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;

/**
 * Class representing an Element, i.e., a set of events/items.
 * 
 * @author  Sebastian Beer
 * @version $Revision$
 */
public class Element
  implements Cloneable, Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -7900701276019516371L;
  
  /** events/items stored as an array of ints */
  protected int[] m_Events;

  /**
   * Constructor
   */
  public Element() {
  }

  /**
   * Constructor accepting an initial size of the events Array as parameter.
   * 
   * @param size 	the size
   */
  public Element(int size) {
    m_Events = new int[size];
  }

  /**
   * Returns all events of the given data set as Elements containing a single 
   * event. The order of events is determined by the header information of 
   * the corresponding ARFF file.
   * 
   * @param instances 	the data set
   * @return 		the set of 1-Elements
   */
  public static FastVector getOneElements (Instances instances) {
    FastVector setOfOneElements = new FastVector();
    Element curElement;

    for (int i = 0; i < instances.numAttributes(); i++) {
      for (int j = 0; j < instances.attribute(i).numValues(); j++) {
	curElement = new Element();
	curElement.setEvents(new int [instances.numAttributes()]);
	for (int k = 0; k < instances.numAttributes(); k++) {
	  curElement.getEvents()[k] = -1;
	}
	curElement.getEvents()[i] = j;
	setOfOneElements.addElement(curElement);
      }
    }
    return setOfOneElements;
  }

  /**
   * Merges two Elements into one.
   * 
   * @param element1 	first Element
   * @param element2 	second Element
   * @return 		the merged Element
   */
  public static Element merge(Element element1, Element element2) {
    int[] element1Events = element1.getEvents();
    int[] element2Events = element2.getEvents();
    Element resultElement = new Element(element1Events.length);
    int[] resultEvents = resultElement.getEvents();

    for (int i = 0; i < element1Events.length; i++) {
      if (element2Events[i] > -1) {
	resultEvents[i] = element2Events[i];
      } else {
	resultEvents[i] = element1Events[i];
      }
    }
    resultElement.setEvents(resultEvents);

    return resultElement;
  }

  /**
   * Returns a deep clone of an Element.
   * 
   * @return 		the cloned Element
   */
  public Element clone() {
    try {
      Element clone = (Element) super.clone();
      int[] cloneEvents = new int[m_Events.length];

      for (int i = 0; i < m_Events.length; i++) {
	cloneEvents[i] = m_Events[i];
      }
      clone.setEvents(cloneEvents);

      return clone;
    } catch (CloneNotSupportedException exc) {
      exc.printStackTrace();
    }
    return null;
  }

  /**
   * Checks if an Element contains over one event.
   * 
   * @return 		true, if the Element contains over one event, else false
   */
  public boolean containsOverOneEvent() {
    int numEvents = 0;
    for (int i = 0; i < m_Events.length; i++) {
      if (m_Events[i] > -1) {
	numEvents++;
      }
      if (numEvents == 2) {
	return true;
      }
    }
    return false;
  }

  /**
   * Deletes the first or last event of an Element.
   * 
   * @param position 	the position of the event to be deleted (first or last)
   */
  public void deleteEvent(String position) {
    if (position.equals("first")) {
      //delete first event
      for (int i = 0; i < m_Events.length; i++) {
	if (m_Events[i] > -1) {
	  m_Events[i] = -1;
	  break;
	}
      }
    }
    if (position.equals("last")) {
      //delete last event
      for (int i = m_Events.length-1; i >= 0; i--) {
	if (m_Events[i] > -1) {
	  m_Events[i] = -1;
	  break;
	}
      }
    }
  }

  /**
   * Checks if two Elements are equal.
   * 
   * @return 		true, if the two Elements are equal, else false
   */
  public boolean equals(Object obj) {
    Element element2 = (Element) obj;

    for (int i=0; i < m_Events.length; i++) {
      if (!(m_Events[i] == element2.getEvents()[i])) {
	return false;
      }
    }
    return true;
  }

  /**
   * Returns the events Array of an Element.
   * 
   * @return 		the events Array
   */
  public int[] getEvents() {
    return m_Events;
  }

  /**
   * Checks if an Element is contained by a given Instance.
   * 
   * @param instance 	the given Instance
   * @return 		true, if the Instance contains the Element, else false
   */
  public boolean isContainedBy(Instance instance) {
    for (int i=0; i < instance.numAttributes(); i++) {
      if (m_Events[i] > -1) {
	if (instance.isMissing(i)) {
	  return false;
	}
	if (m_Events[i] != (int) instance.value(i)) {
	  return false;
	}
      }
    }
    return true;
  }

  /**
   * Checks if the Element contains any events.
   * 
   * @return 		true, if the Element contains no event, else false 
   */
  public boolean isEmpty() {
    for (int i=0; i < m_Events.length; i++) {
      if (m_Events[i] > -1) {
	return false;
      }
    }
    return true;
  }

  /**
   * Sets the events Array of an Element.
   * 
   * @param events 	the events Array to set
   */
  protected void setEvents(int[] events) {
    m_Events = events;
  }

  /**
   * Returns a String representation of an Element where the numeric value 
   * of each event/item is represented by its respective nominal value.
   * 
   * @param dataSet 	the corresponding data set containing the header information
   * @return 		the String representation
   */
  public String toNominalString(Instances dataSet) {
    StringBuffer result = new StringBuffer();
    int addedValues = 0;

    result.append("{");

    for (int i=0; i < m_Events.length; i++) {
      if (m_Events[i] > -1) {			
	result.append(dataSet.attribute(i).value(m_Events[i]) + ",");
	addedValues++;
      }
    }
    result.deleteCharAt(result.length()-1);
    result.append("}");

    return result.toString();
  }

  /**
   * Returns a String representation of an Element.
   * 
   * @return 		the String representation
   */
  public String toString() {
    String result = "";

    result += "{";

    for (int i=0; i < m_Events.length; i++) {
      result += m_Events[i];
      if (i+1 < m_Events.length) {
	result += ",";
      }
    }
    result += "}";

    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
