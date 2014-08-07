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
 *    AttributeMetaInfo.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Properties;

public class AttributeMetaInfo implements Serializable, RevisionHandler {

  /** The attribute's metadata. */
  protected ProtectedProperties m_Metadata;

  /** The attribute's ordering. */
  protected int m_Ordering;

  /** Whether the attribute is regular. */
  protected boolean m_IsRegular;

  /** Whether the attribute is averagable. */
  protected boolean m_IsAveragable;

  /** Whether the attribute has a zeropoint. */
  protected boolean m_HasZeropoint;

  /** The attribute's lower numeric bound. */
  protected double m_LowerBound;

  /** Whether the lower bound is open. */
  protected boolean m_LowerBoundIsOpen;

  /** The attribute's upper numeric bound. */
  protected double m_UpperBound;

  /** Whether the upper bound is open */
  protected boolean m_UpperBoundIsOpen;

  /**
   * Creates the meta info object based on the given meta data.
   */
  public AttributeMetaInfo(ProtectedProperties metadata, Attribute att) {

    setMetadata(metadata, att);
  }


  /**
   * Sets the metadata for the attribute. Processes the strings stored in the
   * metadata of the attribute so that the properties can be set up for the
   * easy-access metadata methods. Any strings sought that are omitted will
   * cause default values to be set.
   * 
   * The following properties are recognised: ordering, averageable, zeropoint,
   * regular, weight, and range.
   * 
   * All other properties can be queried and handled appropriately by classes
   * calling the getMetadata() method.
   * 
   * @param metadata the metadata
   * @throws IllegalArgumentException if the properties are not consistent
   */
  // @ requires metadata != null;
  private void setMetadata(ProtectedProperties metadata, Attribute att) {

    m_Metadata = metadata;

    if (att.m_Type == Attribute.DATE) {
      m_Ordering = Attribute.ORDERING_ORDERED;
      m_IsRegular = true;
      m_IsAveragable = false;
      m_HasZeropoint = false;
    } else {

      // get ordering
      String orderString = m_Metadata.getProperty("ordering", "");

      // numeric ordered attributes are averagable and zeropoint by default
      String def;
      if (att.m_Type == Attribute.NUMERIC && orderString.compareTo("modulo") != 0
        && orderString.compareTo("symbolic") != 0) {
        def = "true";
      } else {
        def = "false";
      }

      // determine boolean states
      m_IsAveragable = (m_Metadata.getProperty("averageable", def).compareTo(
        "true") == 0);
      m_HasZeropoint = (m_Metadata.getProperty("zeropoint", def).compareTo(
        "true") == 0);
      // averagable or zeropoint implies regular
      if (m_IsAveragable || m_HasZeropoint) {
        def = "true";
      }
      m_IsRegular = (m_Metadata.getProperty("regular", def).compareTo("true") == 0);

      // determine ordering
      if (orderString.compareTo("symbolic") == 0) {
        m_Ordering = Attribute.ORDERING_SYMBOLIC;
      } else if (orderString.compareTo("ordered") == 0) {
        m_Ordering = Attribute.ORDERING_ORDERED;
      } else if (orderString.compareTo("modulo") == 0) {
        m_Ordering = Attribute.ORDERING_MODULO;
      } else {
        if (att.m_Type == Attribute.NUMERIC || m_IsAveragable || m_HasZeropoint) {
          m_Ordering = Attribute.ORDERING_ORDERED;
        } else {
          m_Ordering = Attribute.ORDERING_SYMBOLIC;
        }
      }
    }

    // consistency checks
    if (m_IsAveragable && !m_IsRegular) {
      throw new IllegalArgumentException("An averagable attribute must be"
        + " regular");
    }
    if (m_HasZeropoint && !m_IsRegular) {
      throw new IllegalArgumentException("A zeropoint attribute must be"
        + " regular");
    }
    if (m_IsRegular && m_Ordering == Attribute.ORDERING_SYMBOLIC) {
      throw new IllegalArgumentException("A symbolic attribute cannot be"
        + " regular");
    }
    if (m_IsAveragable && m_Ordering != Attribute.ORDERING_ORDERED) {
      throw new IllegalArgumentException("An averagable attribute must be"
        + " ordered");
    }
    if (m_HasZeropoint && m_Ordering != Attribute.ORDERING_ORDERED) {
      throw new IllegalArgumentException("A zeropoint attribute must be"
        + " ordered");
    }

    // determine weight
    att.m_Weight = 1.0;
    String weightString = m_Metadata.getProperty("weight");
    if (weightString != null) {
      try {
        att.m_Weight = Double.valueOf(weightString).doubleValue();
      } catch (NumberFormatException e) {
        // Check if value is really a number
        throw new IllegalArgumentException("Not a valid attribute weight: '"
          + weightString + "'");
      }
    }

    // determine numeric range
    if (att.m_Type == Attribute.NUMERIC) {
      setNumericRange(m_Metadata.getProperty("range"));
    }
  }

  /**
   * Sets the numeric range based on a string. If the string is null the range
   * will default to [-inf,+inf]. A square brace represents a closed interval, a
   * curved brace represents an open interval, and 'inf' represents infinity.
   * Examples of valid range strings: "[-inf,20)","(-13.5,-5.2)","(5,inf]"
   * 
   * @param rangeString the string to parse as the attribute's numeric range
   * @throws IllegalArgumentException if the range is not valid
   */
  // @ requires rangeString != null;
  private void setNumericRange(String rangeString) {
    // set defaults
    m_LowerBound = Double.NEGATIVE_INFINITY;
    m_LowerBoundIsOpen = false;
    m_UpperBound = Double.POSITIVE_INFINITY;
    m_UpperBoundIsOpen = false;

    if (rangeString == null) {
      return;
    }

    // set up a tokenzier to parse the string
    StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(
      rangeString));
    tokenizer.resetSyntax();
    tokenizer.whitespaceChars(0, ' ');
    tokenizer.wordChars(' ' + 1, '\u00FF');
    tokenizer.ordinaryChar('[');
    tokenizer.ordinaryChar('(');
    tokenizer.ordinaryChar(',');
    tokenizer.ordinaryChar(']');
    tokenizer.ordinaryChar(')');

    try {

      // get opening brace
      tokenizer.nextToken();

      if (tokenizer.ttype == '[') {
        m_LowerBoundIsOpen = false;
      } else if (tokenizer.ttype == '(') {
        m_LowerBoundIsOpen = true;
      } else {
        throw new IllegalArgumentException("Expected opening brace on range,"
          + " found: " + tokenizer.toString());
      }

      // get lower bound
      tokenizer.nextToken();
      if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
        throw new IllegalArgumentException("Expected lower bound in range,"
          + " found: " + tokenizer.toString());
      }
      if (tokenizer.sval.compareToIgnoreCase("-inf") == 0) {
        m_LowerBound = Double.NEGATIVE_INFINITY;
      } else if (tokenizer.sval.compareToIgnoreCase("+inf") == 0) {
        m_LowerBound = Double.POSITIVE_INFINITY;
      } else if (tokenizer.sval.compareToIgnoreCase("inf") == 0) {
        m_LowerBound = Double.NEGATIVE_INFINITY;
      } else {
        try {
          m_LowerBound = Double.valueOf(tokenizer.sval).doubleValue();
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Expected lower bound in range,"
            + " found: '" + tokenizer.sval + "'");
        }
      }

      // get separating comma
      if (tokenizer.nextToken() != ',') {
        throw new IllegalArgumentException("Expected comma in range,"
          + " found: " + tokenizer.toString());
      }

      // get upper bound
      tokenizer.nextToken();
      if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
        throw new IllegalArgumentException("Expected upper bound in range,"
          + " found: " + tokenizer.toString());
      }
      if (tokenizer.sval.compareToIgnoreCase("-inf") == 0) {
        m_UpperBound = Double.NEGATIVE_INFINITY;
      } else if (tokenizer.sval.compareToIgnoreCase("+inf") == 0) {
        m_UpperBound = Double.POSITIVE_INFINITY;
      } else if (tokenizer.sval.compareToIgnoreCase("inf") == 0) {
        m_UpperBound = Double.POSITIVE_INFINITY;
      } else {
        try {
          m_UpperBound = Double.valueOf(tokenizer.sval).doubleValue();
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Expected upper bound in range,"
            + " found: '" + tokenizer.sval + "'");
        }
      }

      // get closing brace
      tokenizer.nextToken();

      if (tokenizer.ttype == ']') {
        m_UpperBoundIsOpen = false;
      } else if (tokenizer.ttype == ')') {
        m_UpperBoundIsOpen = true;
      } else {
        throw new IllegalArgumentException("Expected closing brace on range,"
          + " found: " + tokenizer.toString());
      }

      // check for rubbish on end
      if (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
        throw new IllegalArgumentException("Expected end of range string,"
          + " found: " + tokenizer.toString());
      }

    } catch (IOException e) {
      throw new IllegalArgumentException("IOException reading attribute range"
        + " string: " + e.getMessage());
    }

    if (m_UpperBound < m_LowerBound) {
      throw new IllegalArgumentException("Upper bound (" + m_UpperBound
        + ") on numeric range is" + " less than lower bound (" + m_LowerBound
        + ")!");
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10203 $");
  }
}