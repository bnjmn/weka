
package weka.experiment;

import java.io.Serializable;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.ClassNotFoundException;


/**
 * PropertyNode.java
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */

public class PropertyNode implements Serializable {
  
  public Object value;
  public Class parentClass;
  public PropertyDescriptor property;
  
  public PropertyNode(Object pValue) {
    
    this(pValue, null, null);
  }

  public PropertyNode(Object pValue, PropertyDescriptor prop, Class pClass) {
    
    value = pValue;
    property = prop;
    parentClass = pClass;
  }

  public String toString() {
    
    if (property == null) {
      return "Available properties";
    }
    return property.getDisplayName();
  }

  /*
   * Handle serialization ourselves since PropertyDescriptor isn't
   * serializable
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    try {
      out.writeObject(value);
    } catch (Exception ex) {
      throw new IOException("Can't serialize object: " + ex.getMessage());
    }
    out.writeObject(parentClass);
    out.writeObject(property.getDisplayName());
    out.writeObject(property.getReadMethod().getName());
    out.writeObject(property.getWriteMethod().getName());
  }
  private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    value = in.readObject();
    parentClass = (Class) in.readObject();
    String name = (String) in.readObject();
    String getter = (String) in.readObject();
    String setter = (String) in.readObject();
    /*
    System.err.println("Loading property descriptor:\n"
		       + "\tparentClass: " + parentClass.getName()
		       + "\tname: " + name
		       + "\tgetter: " + getter
		       + "\tsetter: " + setter);
    */
    try {
      property = new PropertyDescriptor(name, parentClass, getter, setter);
    } catch (IntrospectionException ex) {
      throw new ClassNotFoundException("Couldn't create property descriptor: "
				       + parentClass.getName() + "::"
				       + name);
    }
  }
} // PropertyNode
