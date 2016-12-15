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
 *    SetPropertiesFromEnvironment.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.EnumHelper;
import weka.core.Environment;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.JobEnvironment;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Step that accesses property values stored in the flow environment and
 * attempts to set them on the algorithm-based step that it is connected to.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "SetPropertiesFromEnvironment",
  category = "Flow",
  toolTipText = "Set properties of the connected algorithm-based step (e.g. "
    + "Classifier, Clusterer etc.) using values stored in the flow environment. "
    + "If no property path for a particular setting value is specified, then it is "
    + "assumed that the value provided is scheme name + options in command-line "
    + "form, in which case the underlying scheme of the connected step will be "
    + "constructed and set; otherwise, the property path is used to set a value "
    + "on the existing underlying scheme.",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "SetPropertiesFromEnvironment.gif")
public class SetPropertiesFromEnvironment extends BaseStep {

  private static final long serialVersionUID = -8316084792512232973L;

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    // all the action happens here in the initialization
    Environment env =
      getStepManager().getExecutionEnvironment().getEnvironmentVariables();
    if (env instanceof JobEnvironment
      && getStepManager().numOutgoingConnections() == 1) {
      Map<String, List<StepManager>> outgoing =
        getStepManager().getOutgoingConnections();
      StepManagerImpl connectedManager = null;
      for (Map.Entry<String, List<StepManager>> e : outgoing.entrySet()) {
        connectedManager = (StepManagerImpl) e.getValue().get(0);
      }

      if (connectedManager != null) {
        Step connectedStep = connectedManager.getManagedStep();
        String stepName = connectedStep.getName();
        Map<String, String> propertiesToSet =
          ((JobEnvironment) env).getStepProperties(stepName);

        if (propertiesToSet != null && propertiesToSet.size() > 0) {
          if (connectedStep instanceof WekaAlgorithmWrapper) {
            // only handle algorithm wrappers (just about everything
            // else can handle variables
            setProperties((WekaAlgorithmWrapper) connectedStep, propertiesToSet);
          }
        }
      }
    }
  }

  /**
   * Get a list of acceptable incoming connection types
   *
   * @return a list of acceptable incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numOutgoingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INFO);
    }
    return null;
  }

  /**
   * Set properties on the specified target objet
   *
   * @param target the target to set properties on
   * @param propertiesToSet the properties to try and set
   */
  protected void setProperties(WekaAlgorithmWrapper target,
    Map<String, String> propertiesToSet) {
    for (Map.Entry<String, String> e : propertiesToSet.entrySet()) {
      String propName = e.getKey();
      String propVal = e.getValue().trim();
      if (propVal.length() == 0) {
        continue;
      }
      try {
        if (propName.length() == 0) {
          // assume the value is scheme + options for specifying the
          // wrapped algorithm

          String[] schemeAndOpts = Utils.splitOptions(propVal);
          if (schemeAndOpts.length > 0) {
            String schemeName = schemeAndOpts[0];
            schemeAndOpts[0] = "";
            Object valToSet = Utils.forName(null, schemeName, schemeAndOpts);
            setValue(target, target.getName(), "wrappedAlgorithm", valToSet);
          }
        } else {
          // single property on the wrapped algorithm
          String[] propPath = propName.split("\\.");
          Object propRoot = target.getWrappedAlgorithm();
          String propToSet = propPath[propPath.length - 1];
          List<String> remainingPath = new ArrayList<>();
          for (int i = 0; i < propPath.length - 1; i++) {
            remainingPath.add(propPath[i]);
          }
          if (remainingPath.size() > 0) {
            propRoot = drillToProperty(propRoot, remainingPath);
          }
          Object valToSet = stringToVal(propVal, propRoot, propToSet);
          setValue(propRoot, propRoot.getClass().getCanonicalName(), propToSet,
            valToSet);
        }
      } catch (Exception ex) {
        String pN = propName.length() == 0 ? "wrapped algorithm" : propName;
        getStepManager().logWarning(
          "Unable to set " + pN + " with value: " + propVal + " on step "
            + target.getName() + ". Reason: " + ex.getMessage());
      }
    }

    // re-initialize (just in case KF environment has called initStep() on
    // the target WekaAlgorithmWrapper before we get to set its properties
    try {
      target.stepInit();
    } catch (WekaException e) {
      getStepManager().logWarning(
        "Was unable to re-initialize step '" + target.getName()
          + "' after setting properties");
    }
  }

  /**
   * Attempts to convert a property value in string form to the object type that
   * the actual property accepts
   *
   * @param propVal the string representation of the property value
   * @param target the target object to receive the property value
   * @param propName the name of the property
   * @return the property value to set as an object
   * @throws WekaException if a problem occurs
   */
  protected Object stringToVal(String propVal, Object target, String propName)
    throws WekaException {

    Object resultVal = null;
    try {
      PropertyDescriptor prop = getPropDescriptor(target, propName);
      if (prop == null) {
        throw new WekaException("Unable to find method '" + propName + "'");
      }

      Method getMethod = prop.getReadMethod();
      Object current = getMethod.invoke(target);
      if (current.getClass().isArray()) {
        resultVal = Utils.forName(null, propVal, null);
      } else if (current instanceof SelectedTag) {
        Tag[] legalTags = ((SelectedTag) current).getTags();
        int tagIndex = Integer.MAX_VALUE;
        // first try and parse as an integer
        try {
          int specifiedID = Integer.parseInt(propVal);
          for (int z = 0; z < legalTags.length; z++) {
            if (legalTags[z].getID() == specifiedID) {
              tagIndex = z;
              break;
            }
          }
        } catch (NumberFormatException e) {
          // try to match tag strings
          for (int z = 0; z < legalTags.length; z++) {
            if (legalTags[z].getReadable().equals(propVal.trim())) {
              tagIndex = z;
              break;
            }
          }
        }
        if (tagIndex != Integer.MAX_VALUE) {
          resultVal = new SelectedTag(tagIndex, legalTags);
        } else {
          throw new WekaException(
            "Unable to set SelectedTag value for property " + "'" + propName
              + "'");
        }
      } else if (current instanceof Enum) {
        EnumHelper helper = new EnumHelper((Enum) current);
        resultVal = EnumHelper.valueFromString(helper.getEnumClass(), propVal);
      } else if (current instanceof OptionHandler) {
        String[] schemeAndOpts = Utils.splitOptions(propVal);
        if (schemeAndOpts.length > 0) {
          String schemeName = schemeAndOpts[0];
          schemeAndOpts[0] = "";
          resultVal = Utils.forName(null, schemeName, schemeAndOpts);
        }
      } else if (current instanceof Number) {
        try {
          if (current instanceof Integer) {
            resultVal = new Integer(propVal);
          } else if (current instanceof Long) {
            resultVal = new Long(propVal);
          } else if (current instanceof Double) {
            resultVal = new Double(propVal);
          } else if (current instanceof Float) {
            resultVal = new Float(propVal);
          }
        } catch (NumberFormatException ex) {
          throw new WekaException("Unable to parse '" + propVal
            + "' as a number");
        }
      } else if (current instanceof Boolean) {
        resultVal =
          propVal.equalsIgnoreCase("true") || propVal.equalsIgnoreCase("yes")
            || propVal.equalsIgnoreCase("Y");
      } else if (current instanceof String) {
        resultVal = propVal;
      } else if (current instanceof File) {
        resultVal = new File(propVal);
      }

      if (resultVal == null) {
        throw new WekaException("Was unable to determine the value to set for "
          + "property '" + propName + "'");
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    return resultVal;
  }

  /**
   * Sets the value of the property on the target object
   *
   * @param target the target object
   * @param targetName the name of the step owning the target object
   * @param propName the property name
   * @param valToSet the value to set
   * @throws WekaException if a problem occurs
   */
  protected void setValue(Object target, String targetName, String propName,
    Object valToSet) throws WekaException {

    try {
      getStepManager().logDebug(
        "Attempting to set property '" + propName + "' "
          + "with value of type '" + valToSet.getClass().getCanonicalName()
          + " '(" + valToSet + ") on '" + targetName + "'");
      PropertyDescriptor prop = getPropDescriptor(target, propName);

      if (prop == null) {
        throw new WekaException("Unable to find method '" + propName + "'");
      }
      Method setMethod = prop.getWriteMethod();
      setMethod.invoke(target, valToSet);
    } catch (Exception e) {
      throw new WekaException(e);
    }
  }

  /**
   * Gets the property descriptor for the supplied property name on the
   * supplied target object
   *
   * @param target the target object
   * @param propName the property name
   * @return the property descriptor, or null if no such property exists
   * @throws IntrospectionException if a problem occurs
   */
  protected PropertyDescriptor
    getPropDescriptor(Object target, String propName)
      throws IntrospectionException {
    PropertyDescriptor result = null;
    BeanInfo bi = Introspector.getBeanInfo(target.getClass());
    PropertyDescriptor[] properties = bi.getPropertyDescriptors();
    for (PropertyDescriptor p : properties) {
      if (p.getName().equals(propName)) {
        result = p;
        break;
      }
    }

    return result;
  }

  /**
   * Drill down to the last element in the supplied property path list on
   * the given base object
   *
   * @param baseObject the base object to drill down
   * @param propertyPath the property path to traverse
   * @return the Object corresponding to the path
   * @throws WekaException if a problem occurs
   */
  protected Object
    drillToProperty(Object baseObject, List<String> propertyPath)
      throws WekaException {
    Object objectBeingConfigured = baseObject;
    if (propertyPath != null) {
      for (String methodName : propertyPath) {
        try {
          boolean isArray = methodName.endsWith("]");
          int arrayIndex = -1;
          if (isArray) {
            String arrayPart =
              methodName.substring(methodName.indexOf('[') + 1,
                methodName.lastIndexOf(']'));
            arrayIndex = Integer.parseInt(arrayPart.trim());
            methodName = methodName.substring(0, methodName.indexOf('['));
          }

          BeanInfo bi =
            Introspector.getBeanInfo(objectBeingConfigured.getClass());
          PropertyDescriptor[] properties = bi.getPropertyDescriptors();
          PropertyDescriptor targetProperty = null;
          for (PropertyDescriptor p : properties) {
            if (p.getName().equals(methodName)) {
              targetProperty = p;
              break;
            }
          }
          if (targetProperty == null) {
            throw new WekaException(
              "Unable to find accessor method for property path part: "
                + methodName + " on object "
                + objectBeingConfigured.getClass().getName());
          }
          Method getMethod = targetProperty.getReadMethod();
          Object[] args = {};
          objectBeingConfigured = getMethod.invoke(objectBeingConfigured, args);
          if (isArray) {
            // get the indexed element
            if (!objectBeingConfigured.getClass().isArray()) {
              throw new WekaException("Property path element '" + methodName
                + "' was specified as an array type, but the "
                + "resulting object retrieved "
                + "from this property is not an array!");
            }
            objectBeingConfigured =
              ((Object[]) objectBeingConfigured)[arrayIndex];
          }
        } catch (IntrospectionException ex) {
          throw new WekaException("GOEManager: couldn't introspect", ex);
        } catch (InvocationTargetException e) {
          throw new WekaException("Invocation target exception when invoking "
            + methodName + " on " + objectBeingConfigured.getClass().getName(),
            e);
        } catch (IllegalAccessException e) {
          throw new WekaException(e);
        }
      }
    }
    return objectBeingConfigured;
  }
}
