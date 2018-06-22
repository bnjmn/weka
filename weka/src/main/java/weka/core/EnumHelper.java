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
 *    EnumHelper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.lang.reflect.Method;

/**
 * Helper/wrapper class for obtaining an arbitrary enum value from an arbitrary
 * enum type. An enum value wrapped in this class can be serialized by Weka's
 * XML serialization mechanism.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class EnumHelper {

  /**
   * The fully qualified name of the enum class to wrap
   */
  protected String m_enumClass;

  /**
   * The selected/wapped enum value (as obtained by calling toString() on the
   * value)
   */
  protected String m_selectedEnumValue;

  /**
   * Constructor
   *
   * @param e the enum value to wrap
   */
  public EnumHelper(Enum e) {
    m_selectedEnumValue = e.toString();
    m_enumClass = e.getClass().getName();
  }

  /**
   * No-op constructor (for beans conformity)
   */
  public EnumHelper() {
  }

  /**
   * Set the fully qualified enum class name
   *
   * @param enumClass the fully qualified name of the enum class
   */
  public void setEnumClass(String enumClass) {
    m_enumClass = enumClass;
  }

  /**
   * Get the fully qualified enum class name
   *
   * @return the fully qualified name of the enum class
   */
  public String getEnumClass() {
    return m_enumClass;
  }

  /**
   * Set the selected/wrapped enum value (as obtained by calling toString() on
   * the enum value)
   *
   * @param selectedEnumValue the enum value to wrap
   */
  public void setSelectedEnumValue(String selectedEnumValue) {
    m_selectedEnumValue = selectedEnumValue;
  }

  /**
   * Get the selected/wrapped enum value (as obtained by calling toString() on
   * the enum value)
   *
   * @return the enum value to wrap
   */
  public String getSelectedEnumValue() {
    return m_selectedEnumValue;
  }

  /**
   * Helper method to recover an enum value given the fully qualified name of
   * the enum and the value in question as strings
   *
   * @param enumClass a string containing the fully qualified name of the enum
   *          class
   * @param enumValue a string containing the value of the enum to find
   * @return the enum value as an Object
   * @throws Exception if a problem occurs
   */
  public static Object valueFromString(String enumClass, String enumValue)
    throws Exception {
    Class<?> eClazz = WekaPackageClassLoaderManager.forName(enumClass);

    return valueFromString(eClazz, enumValue);
  }

  /**
   * Helper method to recover an enum value given the fully qualified name of
   * the enum and the value in question as strings
   *
   * @param enumClass the class of the enum
   * @param enumValue a string containing the value of the enum to find
   * @return the enum value as an Object
   * @throws Exception if a problem occurs
   */
  public static Object valueFromString(Class<?> enumClass, String enumValue)
    throws Exception {
    Method valuesM = enumClass.getMethod("values");

    Enum[] values = (Enum[]) valuesM.invoke(null);
    for (Enum e : values) {
      if (e.toString().equals(enumValue)) {
        return e;
      }
    }

    return null;
  }

  /**
   * Main method for testing this class
   *
   * @param args arguments
   */
  public static void main(String[] args) {
    try {
      if (args.length != 2) {
        System.err
          .println("usage: weka.core.EnumHelper <enum class> <enum value>");
      }

      Object eVal = EnumHelper.valueFromString(args[0], args[1]);
      System.out.println("The enum's value is: " + eVal.toString());
      System.out.println("The enum's class is: " + eVal.getClass().toString());
      if (eVal instanceof Enum) {
        System.out.println("The value is an instance of Enum superclass");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
