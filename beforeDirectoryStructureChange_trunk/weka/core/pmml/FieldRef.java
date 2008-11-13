package weka.core.pmml;

import java.util.ArrayList;
import org.w3c.dom.Element;

import weka.core.Attribute;

/**
 * Class encapsulating a FieldRef Expression. Is simply a
 * pass-through to an existing field.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision 1.0 $
 */
public class FieldRef extends Expression {
  
  /** The name of the field to reference */
  protected String m_fieldName = null;
  
  public FieldRef(Element fieldRef, FieldMetaInfo.Optype opType, ArrayList<Attribute> fieldDefs) 
    throws Exception {
    super(opType, fieldDefs);
    
    m_fieldName = fieldRef.getAttribute("field");
    validateField();
  }
  
  public void setFieldDefs(ArrayList<Attribute> fieldDefs) throws Exception {
    super.setFieldDefs(fieldDefs);
    validateField();    
  }
  
  protected void validateField() throws Exception {
    // do some type checking here
    if (m_fieldDefs != null) {
      Attribute a = getFieldDef(m_fieldName);
      if (a == null) {
        throw new Exception("[FieldRef] Can't find field " + m_fieldName
            + " in the supplied field definitions");
      }
      if ((m_opType == FieldMetaInfo.Optype.CATEGORICAL ||
          m_opType == FieldMetaInfo.Optype.ORDINAL) && a.isNumeric()) {
        throw new IllegalArgumentException("[FieldRef] Optype is categorical/ordinal but matching "
            + "parameter in the field definitions is not!");
      }
      
      if (m_opType == FieldMetaInfo.Optype.CONTINUOUS && a.isNominal()) {
        throw new IllegalArgumentException("[FieldRef] Optype is continuous but matching "
            + "parameter in the field definitions is not!");
      }
    }
  }

  @Override
  public double getResult(double[] incoming) throws Exception {

    double result = Double.NaN;
    boolean found = false;
    
    for (int i = 0; i < m_fieldDefs.size(); i++) {
      Attribute a = m_fieldDefs.get(i);
      if (a.name().equals(m_fieldName)) {
        if (a.isNumeric()) {
          if (m_opType == FieldMetaInfo.Optype.CATEGORICAL ||
              m_opType == FieldMetaInfo.Optype.ORDINAL) {
            throw new IllegalArgumentException("[FieldRef] Optype is categorical/ordinal but matching "
                + "parameter is not!");         
          }          
        } else if (a.isNominal()) {
          if (m_opType == FieldMetaInfo.Optype.CONTINUOUS) {
            throw new IllegalArgumentException("[FieldRef] Optype is continuous but matching "
                + "parameter is not!");
          }
        } else {
          throw new IllegalArgumentException("[FieldRef] Unhandled attribute type");
        }
        result = incoming[i];
        found = true;
        break;
      }
    }
    
    if (!found) {
      throw new Exception("[FieldRef] this field: " + m_fieldName + " is not in the supplied "
          + "list of parameters!");
    }
    return result;
  }

  @Override
  public String getResultCategorical(double[] incoming)
      throws Exception {
    
    if (m_opType == FieldMetaInfo.Optype.CONTINUOUS) {
      throw new IllegalArgumentException("[FieldRef] Can't return result as "
          +"categorical/ordinal because optype is continuous!");
    }
    
    boolean found = false;
    String result = null;
    
    for (int i = 0; i < m_fieldDefs.size(); i++) {
      Attribute a = m_fieldDefs.get(i);
      if (a.name().equals(m_fieldName)) {
        found = true;
        result = a.value((int)incoming[i]);
        break;
      }
    }
    
    if (!found) {
      throw new Exception("[FieldRef] this field: " + m_fieldName + " is not in the supplied "
          + "list of parameters!");
    }
    return result;
  }
  
  /**
   * Return the structure of the result of applying this Expression
   * as an Attribute.
   * 
   * @return the structure of the result of applying this Expression as an
   * Attribute.
   */
  public Attribute getOutputDef() {
    
    Attribute a = getFieldDef(m_fieldName);
    if (a != null) {
      return a;
      /* Attribute result = a.copy(attName);
      return result; */
    }
    
    // If we can't find the reference field in the field definitions then
    // we can't return a definition for the result
    return null;
  }
  
  public String toString(String pad) {
    return pad + "FieldRef: " + m_fieldName;
  }
}
