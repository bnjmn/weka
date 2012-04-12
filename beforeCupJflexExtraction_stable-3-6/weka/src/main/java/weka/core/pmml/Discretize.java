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
 *    Discretize.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.core.pmml;

import java.io.Serializable;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

/**
 * Class encapsulating a Discretize Expression.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision 1.0 $
 */
public class Discretize extends Expression {
  
  /**
   * Inner class to encapsulate DiscretizeBin elements
   */
  protected class DiscretizeBin implements Serializable {
    
    /**
     * For serialization
     */
    private static final long serialVersionUID = 5810063243316808400L;

    /** The intervals for this DiscretizeBin */
    private ArrayList<FieldMetaInfo.Interval> m_intervals =
      new ArrayList<FieldMetaInfo.Interval>();
    
    /** The bin value for this DiscretizeBin */
    private String m_binValue;
    
    protected DiscretizeBin(Element bin) throws Exception {
      NodeList iL = bin.getElementsByTagName("Interval");
      for (int i = 0; i < iL.getLength(); i++) {
        Node iN = iL.item(i);
        if (iN.getNodeType() == Node.ELEMENT_NODE) {
          FieldMetaInfo.Interval tempInterval = new FieldMetaInfo.Interval((Element)iN);
          m_intervals.add(tempInterval);
        }
      }
      
      m_binValue = bin.getAttribute("binValue");
    }
    
    /**
     * Get the bin value for this DiscretizeBin
     * 
     * @return the bin value
     */
    protected String getBinValue() {
      return m_binValue;
    }
    
    /**
     * Returns true if there is an interval that contains the incoming
     * value.
     * 
     * @param value the value to check against
     * @return true if there is an interval that containst the supplied value
     */
    protected boolean containsValue(double value) {
      boolean result = false;
      
      for (FieldMetaInfo.Interval i : m_intervals) {
        if (i.containsValue(value)) {
          result = true;
          break;
        }
      }

      return result;
    }
    
    public String toString() {
      StringBuffer buff = new StringBuffer();
      
      buff.append("\"" + m_binValue + "\" if value in: ");
      boolean first = true;
      for (FieldMetaInfo.Interval i : m_intervals) {
        if (!first) {
          buff.append(", ");
        } else {
          first = false;
        }
        buff.append(i.toString());
      }
      
      return buff.toString();
    }
  }
  
  
  /** The name of the field to be discretized */
  protected String m_fieldName;
  
  /** The index of the field */
  protected int m_fieldIndex;
  
  /** True if a replacement for missing values has been specified */
  protected boolean m_mapMissingDefined = false;
  
  /** The value of the missing value replacement (if defined) */
  protected String m_mapMissingTo;
  
  /** True if a default value has been specified */
  protected boolean m_defaultValueDefined = false;
  
  /** The default value (if defined) */
  protected String m_defaultValue;
  
  /** The bins for this discretization */
  protected ArrayList<DiscretizeBin> m_bins = new ArrayList<DiscretizeBin>();
  
  /** The output structure of this discretization */
  protected Attribute m_outputDef;
  
  /**
   * Constructs a Discretize Expression
   * 
   * @param discretize the Element containing the discretize expression
   * @param opType the optype of this Discretize Expression
   * @param fieldDefs the structure of the incoming fields
   * @throws Exception if the optype is not categorical/ordinal or if there
   * is a problem parsing this element
   */
  public Discretize(Element discretize, FieldMetaInfo.Optype opType, ArrayList<Attribute> fieldDefs) 
    throws Exception {
    super(opType, fieldDefs);
  
    if (opType == FieldMetaInfo.Optype.CONTINUOUS) {
      throw new Exception("[Discretize] must have a categorical or ordinal optype");
    }
    
    m_fieldName = discretize.getAttribute("field");
    
    m_mapMissingTo = discretize.getAttribute("mapMissingTo");
    if (m_mapMissingTo != null && m_mapMissingTo.length() > 0) {
      m_mapMissingDefined = true;
    }
    
    m_defaultValue = discretize.getAttribute("defaultValue");
    if (m_defaultValue != null && m_defaultValue.length() > 0) {
      m_defaultValueDefined = true;
    }
    
    // get the DiscretizeBin Elements
    NodeList dbL = discretize.getElementsByTagName("DiscretizeBin");
    for (int i = 0; i < dbL.getLength(); i++) {
      Node dbN = dbL.item(i);
      if (dbN.getNodeType() == Node.ELEMENT_NODE) {
        Element dbE = (Element)dbN;
        DiscretizeBin db = new DiscretizeBin(dbE);
        m_bins.add(db);
      }
    }
   
    setUpField();
  }
  
  /**
   * Set the field definitions for this Expression to use
   * 
   * @param fieldDefs the field definitions to use
   * @throws Exception if there is a problem setting the field definitions
   */
  public void setFieldDefs(ArrayList<Attribute> fieldDefs) throws Exception {
    super.setFieldDefs(fieldDefs);
    setUpField();
  }
  
  private void setUpField() throws Exception {
    m_fieldIndex = -1;
    
    if (m_fieldDefs != null) {
      m_fieldIndex = getFieldDefIndex(m_fieldName);
      if (m_fieldIndex < 0) {
        throw new Exception("[Discretize] Can't find field " + m_fieldName
            + " in the supplied field definitions.");
      }
      
      Attribute field = m_fieldDefs.get(m_fieldIndex);
      if (!field.isNumeric()) {
        throw new Exception("[Discretize] reference field " + m_fieldName
            +" must be continuous.");
      }
    }
    
    // set up the output structure
    Attribute tempAtt = new Attribute("temp", (FastVector)null);
    for (DiscretizeBin d : m_bins) {
      tempAtt.addStringValue(d.getBinValue());
    }
    
    // add the default value (just in case it is some other value than one
    // of the bins
    if (m_defaultValueDefined) {
      tempAtt.addStringValue(m_defaultValue);
    }
    
    // add the map missing to value (just in case it is some other value than one
    // of the bins
    if (m_mapMissingDefined) {
      tempAtt.addStringValue(m_mapMissingTo);
    }
    
    // now make this into a nominal attribute
    FastVector values = new FastVector();
    for (int i = 0; i < tempAtt.numValues(); i++) {
     values.addElement(tempAtt.value(i)); 
    }
    
    m_outputDef = new Attribute(m_fieldName + "_discretized", values);
  }

  /**
   * Return the structure of the result of applying this Expression
   * as an Attribute.
   * 
   * @return the structure of the result of applying this Expression as an
   * Attribute.
   */
  protected Attribute getOutputDef() {
    return m_outputDef;
  }

  /**
   * Get the result of evaluating the expression. In the case
   * of a continuous optype, a real number is returned; in
   * the case of a categorical/ordinal optype, the index of the nominal
   * value is returned as a double.
   * 
   * @param incoming the incoming parameter values
   * @return the result of evaluating the expression
   * @throws Exception if there is a problem computing the result
   */
  public double getResult(double[] incoming) throws Exception {
    
    // default of a missing value for the result if none of the following
    // logic applies
    double result = Instance.missingValue();
    
    double value = incoming[m_fieldIndex];
    
    if (Instance.isMissingValue(value)) {
      if (m_mapMissingDefined) {
        result = m_outputDef.indexOfValue(m_mapMissingTo);
      }
    } else {
      // look for a bin that has an interval that contains this value
      boolean found = false;
      for (DiscretizeBin b : m_bins) {
        if (b.containsValue(value)) {
          found = true;
          result = m_outputDef.indexOfValue(b.getBinValue());
          break;
        }
      }
      
      if (!found) {
        if (m_defaultValueDefined) {
          result = m_outputDef.indexOfValue(m_defaultValue);
        }
      }
    }
    
    return result;
  }

  /**
   * Gets the result of evaluating the expression when the
   * optype is categorical or ordinal as the actual String
   * value.
   * 
   * @param incoming the incoming parameter values 
   * @return the result of evaluating the expression
   * @throws Exception if the optype is continuous
   */
  public String getResultCategorical(double[] incoming) throws Exception {
    double index = getResult(incoming);
    if (Instance.isMissingValue(index)) {
      return "**Missing Value**";
    }
    
    return m_outputDef.value((int)index);
  }
  
  public String toString(String pad) {
    StringBuffer buff = new StringBuffer();
    
    buff.append(pad + "Discretize (" + m_fieldName + "):");
    for (DiscretizeBin d : m_bins) {
      buff.append("\n" + pad + d.toString());
    }
    
    if (m_mapMissingDefined) {
      buff.append("\n" + pad + "map missing values to: " + m_mapMissingTo);
    }
    
    if (m_defaultValueDefined) {
      buff.append("\n" + pad + "defautl value: " + m_defaultValue);
    }
    
    return buff.toString();
  }
}
