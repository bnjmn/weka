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
 *    JSONFlowUtils.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.EnumHelper;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.OptionHandler;
import weka.core.Settings;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.WekaPackageClassLoaderManager;
import weka.core.json.JSONNode;
import weka.gui.FilePropertyMetadata;
import weka.knowledgeflow.steps.ClassAssigner;
import weka.knowledgeflow.steps.NotPersistable;
import weka.knowledgeflow.steps.Saver;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.TrainingSetMaker;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utilities for building and saving flows from JSON data
 *
 * @author Mark Hall
 */
public class JSONFlowUtils {

  public static final String FLOW_NAME = "flow_name";
  public static final String STEPS = "steps";
  public static final String OPTIONHANDLER = "optionHandler";
  public static final String OPTIONS = "options";
  public static final String LOADER = "loader";
  public static final String SAVER = "saver";
  public static final String ENUM_HELPER = "enumHelper";
  public static final String CLASS = "class";
  public static final String PROPERTIES = "properties";
  public static final String CONNECTIONS = "connections";
  public static final String COORDINATES = "coordinates";

  /**
   * Add a name,value pair to the JSON
   *
   * @param b the StringBuilder to add the pair to
   * @param name the name
   * @param value the associated value
   * @param comma if true postfix a comma
   */
  protected static void addNameValue(StringBuilder b, String name,
    String value, boolean comma) {
    b.append(name).append(" : ").append(value);
    if (comma) {
      b.append(",");
    }
  }

  /**
   * Add an {@code OptionHandler} spec to the JSON
   *
   * @param propName the name to associate with the option handler
   * @param handler the option handler
   * @param json the current {@JSONNode} to add to
   */
  protected static void addOptionHandler(String propName,
    OptionHandler handler, JSONNode json) {
    JSONNode optionNode = json.addObject(propName);
    optionNode.addPrimitive("type", OPTIONHANDLER);
    optionNode.addPrimitive(CLASS, handler.getClass().getCanonicalName());
    optionNode.addPrimitive(OPTIONS, Utils.joinOptions(handler.getOptions()));
  }

  /**
   * Add an enum to the JSON
   *
   * @param propName the name to associate the enum with
   * @param ee the enum to add
   * @param json the current {@code JSONNode} to add to
   */
  protected static void addEnum(String propName, Enum ee, JSONNode json) {
    JSONNode enumNode = json.addObject(propName);
    enumNode.addPrimitive("type", ENUM_HELPER);
    EnumHelper helper = new EnumHelper(ee);
    enumNode.addPrimitive(CLASS, helper.getEnumClass());
    enumNode.addPrimitive("value", helper.getSelectedEnumValue());
  }

  /**
   * Add a {@code weka.core.converters.Saver} to the JSON
   * 
   * @param propName the name to associate the saver with
   * @param saver the saver to add
   * @param json the current {@code JSONNode} to add to
   */
  protected static void addSaver(String propName,
    weka.core.converters.Saver saver, JSONNode json) {
    JSONNode saverNode = json.addObject(propName);
    saverNode.addPrimitive("type", SAVER);
    saverNode.addPrimitive(CLASS, saver.getClass().getCanonicalName());

    String prefix = "";
    String dir = "";

    if (saver instanceof weka.core.converters.AbstractFileSaver) {
      ((weka.core.converters.AbstractFileSaver) saver).retrieveFile();
      prefix = ((weka.core.converters.AbstractFileSaver) saver).filePrefix();
      dir = ((weka.core.converters.AbstractFileSaver) saver).retrieveDir();
      // Replace any windows file separators with forward slashes (Java under
      // windows can
      // read paths with forward slashes (apparantly)
      dir = dir.replace('\\', '/');
    }

    Boolean relativeB = null;
    if (saver instanceof weka.core.converters.FileSourcedConverter) {
      relativeB =
        ((weka.core.converters.FileSourcedConverter) saver)
          .getUseRelativePath();
    }

    saverNode.addPrimitive("filePath", "");
    saverNode.addPrimitive("dir", dir);
    saverNode.addPrimitive("prefix", prefix);

    if (relativeB != null) {
      saverNode.addPrimitive("useRelativePath", relativeB);
    }

    if (saver instanceof OptionHandler) {
      String optsString =
        Utils.joinOptions(((OptionHandler) saver).getOptions());
      saverNode.addPrimitive(OPTIONS, optsString);
    }
  }

  /**
   * Add a {@code weka.core.converters.Loader} to the JSON
   *
   * @param propName the name to associate the loader with
   * @param loader the loader to add
   * @param json the curren {@code JSONNode} to add to
   */
  protected static void addLoader(String propName,
    weka.core.converters.Loader loader, JSONNode json) {
    JSONNode loaderNode = json.addObject(propName);
    loaderNode.addPrimitive("type", LOADER);
    loaderNode.addPrimitive("class", loader.getClass().getCanonicalName());

    File file = null;
    if (loader instanceof weka.core.converters.AbstractFileLoader) {
      file = ((weka.core.converters.AbstractFileLoader) loader).retrieveFile();
    }

    Boolean relativeB = null;
    if (loader instanceof weka.core.converters.FileSourcedConverter) {
      relativeB =
        ((weka.core.converters.FileSourcedConverter) loader)
          .getUseRelativePath();
    }

    if (file != null && !file.isDirectory()) {
      String withResourceSeparators =
        file.getPath().replace(File.pathSeparatorChar, '/');
      boolean notAbsolute =
        (((weka.core.converters.AbstractFileLoader) loader)
          .getUseRelativePath()
          || (loader instanceof EnvironmentHandler && Environment
            .containsEnvVariables(file.getPath()))
          || JSONFlowUtils.class.getClassLoader().getResource(
            withResourceSeparators) != null || !file.exists());

      String path = (notAbsolute) ? file.getPath() : file.getAbsolutePath();
      // Replace any windows file separators with forward slashes (Java under
      // windows can
      // read paths with forward slashes (apparantly)
      path = path.replace('\\', '/');

      loaderNode.addPrimitive("filePath", path);
    } else {
      loaderNode.addPrimitive("filePath", "");
    }

    if (relativeB != null) {
      loaderNode.addPrimitive("useRelativePath", relativeB);
    }

    if (loader instanceof OptionHandler) {
      String optsString =
        Utils.joinOptions(((OptionHandler) loader).getOptions());
      loaderNode.addPrimitive(OPTIONS, optsString);
    }

  }

  /**
   * Add a step to the JSON
   *
   * @param stepArray the {@code JSONNode} to add the step to
   * @param stepManager the {@code StepManager} of the step to add
   * @throws WekaException if a problem occurs
   */
  protected static void addStepJSONtoFlowArray(JSONNode stepArray,
    StepManagerImpl stepManager) throws WekaException {

    JSONNode step = stepArray.addObjectArrayElement();
    step.addPrimitive("class", stepManager.getManagedStep().getClass()
      .getCanonicalName());
    // step.addPrimitive(STEP_NAME, stepManager.getManagedStep().getName());
    JSONNode properties = step.addObject(PROPERTIES);
    try {
      Step theStep = stepManager.getManagedStep();
      BeanInfo bi = Introspector.getBeanInfo(theStep.getClass());
      PropertyDescriptor[] stepProps = bi.getPropertyDescriptors();

      for (PropertyDescriptor p : stepProps) {
        if (p.isHidden() || p.isExpert()) {
          continue;
        }

        String name = p.getDisplayName();
        Method getter = p.getReadMethod();
        Method setter = p.getWriteMethod();
        if (getter == null || setter == null) {
          continue;
        }
        boolean skip = false;
        for (Annotation a : getter.getAnnotations()) {
          if (a instanceof NotPersistable) {
            skip = true;
            break;
          }
        }
        if (skip) {
          continue;
        }

        Object[] args = {};
        Object propValue = getter.invoke(theStep, args);
        if (propValue == null) {
          properties.addNull(name);
        } else if (propValue instanceof Boolean) {
          properties.addPrimitive(name, (Boolean) propValue);
        } else if (propValue instanceof Integer || propValue instanceof Long) {
          properties.addPrimitive(name,
            new Integer(((Number) propValue).intValue()));
        } else if (propValue instanceof Double) {
          properties.addPrimitive(name, (Double) propValue);
        } else if (propValue instanceof Number) {
          properties.addPrimitive(name,
            new Double(((Number) propValue).doubleValue()));
        } else if (propValue instanceof weka.core.converters.Loader) {
          addLoader(name, (weka.core.converters.Loader) propValue, properties);
        } else if (propValue instanceof weka.core.converters.Saver) {
          addSaver(name, (weka.core.converters.Saver) propValue, properties);
        } else if (propValue instanceof OptionHandler) {
          addOptionHandler(name, (OptionHandler) propValue, properties);
        } else if (propValue instanceof Enum) {
          addEnum(name, (Enum) propValue, properties);
        } else if (propValue instanceof File) {
          String fString = propValue.toString();
          fString = fString.replace('\\', '/');
          properties.addPrimitive(name, fString);
        } else {
          properties.addPrimitive(name, propValue.toString());
        }
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    JSONNode connections = step.addObject(CONNECTIONS);
    for (Map.Entry<String, List<StepManager>> e : stepManager.m_connectedByTypeOutgoing
      .entrySet()) {
      String connName = e.getKey();
      JSONNode connTypeArray = connections.addArray(connName);
      for (StepManager c : e.getValue()) {
        connTypeArray.addArrayElement(c.getName());
      }
    }

    if (stepManager.getStepVisual() != null) {
      String coords =
        "" + stepManager.getStepVisual().getX() + ","
          + stepManager.getStepVisual().getY();
      step.addPrimitive(COORDINATES, coords);
    }
  }

  /**
   * Read a {@code weka.core.converters.Loader} from the current JSON node
   *
   * @param loaderNode the {@code JSONNode} to read the loader from
   * @return the instantiated loader
   * @throws WekaException if a problem occurs
   */
  protected static weka.core.converters.Loader readStepPropertyLoader(
    JSONNode loaderNode) throws WekaException {

    String clazz = loaderNode.getChild(CLASS).getValue().toString();
    try {
      weka.core.converters.Loader loader =
        (weka.core.converters.Loader) WekaPackageClassLoaderManager
          .objectForName(clazz);
      /*
       * Beans.instantiate( JSONFlowUtils.class.getClassLoader(), clazz);
       */

      if (loader instanceof OptionHandler) {
        String optionString =
          loaderNode.getChild(OPTIONS).getValue().toString();
        if (optionString != null && optionString.length() > 0) {
          ((OptionHandler) loader).setOptions(Utils.splitOptions(optionString));
        }
      }

      if (loader instanceof weka.core.converters.AbstractFileLoader) {
        String filePath = loaderNode.getChild("filePath").getValue().toString();
        if (filePath.length() > 0) {

          ((weka.core.converters.AbstractFileLoader) loader)
            .setSource(new File(filePath));
        }
      }

      if (loader instanceof weka.core.converters.FileSourcedConverter) {
        Boolean relativePath =
          (Boolean) loaderNode.getChild("useRelativePath").getValue();
        ((weka.core.converters.FileSourcedConverter) loader)
          .setUseRelativePath(relativePath);

      }

      return loader;
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Read a {@code weka.core.converters.Saver} from the current JSON node
   *
   * @param saverNode the {@code JSONNode} to read from
   * @return an instantiated saver
   * @throws WekaException if a problem occurs
   */
  protected static weka.core.converters.Saver readStepPropertySaver(
    JSONNode saverNode) throws WekaException {
    String clazz = saverNode.getChild(CLASS).getValue().toString();
    try {
      weka.core.converters.Saver saver =
        (weka.core.converters.Saver) WekaPackageClassLoaderManager
          .objectForName(clazz);
      /*
       * Beans.instantiate( JSONFlowUtils.class.getClassLoader(), clazz);
       */

      if (saver instanceof OptionHandler) {
        String optionString = saverNode.getChild(OPTIONS).getValue().toString();
        if (optionString != null && optionString.length() > 0) {
          ((OptionHandler) saver).setOptions(Utils.splitOptions(optionString));
        }
      }

      if (saver instanceof weka.core.converters.AbstractFileSaver) {
        String dir = saverNode.getChild("dir").getValue().toString();
        String prefix = saverNode.getChild("prefix").getValue().toString();
        if (dir != null && prefix != null) {
          ((weka.core.converters.AbstractFileSaver) saver).setDir(dir);
          ((weka.core.converters.AbstractFileSaver) saver)
            .setFilePrefix(prefix);
        }
      }

      if (saver instanceof weka.core.converters.FileSourcedConverter) {
        Boolean relativePath =
          (Boolean) saverNode.getChild("useRelativePath").getValue();
        ((weka.core.converters.FileSourcedConverter) saver)
          .setUseRelativePath(relativePath);
      }
      return saver;
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Read an {@code OptionHandler} from the current JSON node
   *
   * @param optionHNode the {@code JSONNode} to read from
   * @return an instantiated and configured option handler
   * @throws WekaException if a problem occurs
   */
  protected static OptionHandler readStepPropertyOptionHandler(
    JSONNode optionHNode) throws WekaException {

    String clazz = optionHNode.getChild(CLASS).getValue().toString();
    try {
      OptionHandler oh =
        (OptionHandler) WekaPackageClassLoaderManager.objectForName(clazz);
      /*
       * Beans.instantiate(JSONFlowUtils.class.getClassLoader(), clazz);
       */
      String optionString = optionHNode.getChild(OPTIONS).getValue().toString();
      if (optionString != null && optionString.length() > 0) {
        String[] options = Utils.splitOptions(optionString);

        oh.setOptions(options);
      }

      return oh;
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Read an enum from the current JSON node
   *
   * @param enumNode the {@code JSONNode} to read from
   * @return the enum object
   * @throws WekaException if a problem occurs
   */
  protected static Object readStepPropertyEnum(JSONNode enumNode)
    throws WekaException {
    EnumHelper helper = new EnumHelper();
    String clazz = enumNode.getChild(CLASS).getValue().toString();
    String value = enumNode.getChild("value").getValue().toString();

    try {
      return EnumHelper.valueFromString(clazz, value);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Read a step property object from the current JSON node. Handles option
   * handlers, loaders, savers and enums.
   * 
   * @param propNode the {@code JSONNode} to read from
   * @return a step property object
   * @throws Exception if a problem occurs
   */
  protected static Object readStepObjectProperty(JSONNode propNode)
    throws Exception {

    String type = propNode.getChild("type").getValue().toString();
    if (type.equals(OPTIONHANDLER)) {
      return readStepPropertyOptionHandler(propNode);
    } else if (type.equals(LOADER)) {
      return readStepPropertyLoader(propNode);
    } else if (type.equals(SAVER)) {
      return readStepPropertySaver(propNode);
    } else if (type.equals(ENUM_HELPER)) {
      return readStepPropertyEnum(propNode);
    } else {
      throw new WekaException("Unknown object property type: " + type);
    }
  }

  /**
   * Checks a property to see if it is tagged with the FilePropertyMetadata
   * class. If so, it converts the string property into an actual file property
   * so that the Step can be restored correctly
   * 
   * @param theValue the value of the property
   * @param propD the PropertyDescriptor for the property
   * @return a File object or null if this property is not tagged as a file
   *         property
   */
  protected static File checkForFileProp(Object theValue,
    PropertyDescriptor propD) {
    Method writeMethod = propD.getWriteMethod();
    Method readMethod = propD.getReadMethod();
    if (writeMethod != null && readMethod != null) {
      FilePropertyMetadata fM =
        writeMethod.getAnnotation(FilePropertyMetadata.class);
      if (fM == null) {
        fM = readMethod.getAnnotation(FilePropertyMetadata.class);
      }

      if (fM != null) {
        return new File(theValue.toString());
      }
    }
    return null;
  }

  /**
   * Read a {@code Step} from the supplied JSON node
   *
   * @param stepNode the {@code JSONNode} to read from
   * @param flow the {@code Flow} to add the read step to
   * @throws WekaException if a problem occurs
   */
  protected static void readStep(JSONNode stepNode, Flow flow)
    throws WekaException {
    String clazz = stepNode.getChild(CLASS).getValue().toString();
    Object step = null;
    Step theStep = null;
    try {
      step = WekaPackageClassLoaderManager.objectForName(clazz);
      // Beans.instantiate(JSONFlowUtils.class.getClassLoader(), clazz);

      if (!(step instanceof Step)) {
        throw new WekaException(
          "Instantiated a knowledge flow step that does not implement StepComponent!");
      }
      theStep = (Step) step;

      JSONNode properties = stepNode.getChild(PROPERTIES);
      for (int i = 0; i < properties.getChildCount(); i++) {
        JSONNode aProp = (JSONNode) properties.getChildAt(i);

        Object valueToSet = null;
        if (aProp.isObject()) {
          valueToSet = readStepObjectProperty(aProp);
        } else if (aProp.isArray()) {
          // TODO
        } else {
          valueToSet = aProp.getValue();
        }

        try {
          if (valueToSet != null) {
            PropertyDescriptor propD =
              new PropertyDescriptor(aProp.getName(), theStep.getClass());
            File checkForFileProp = checkForFileProp(valueToSet, propD);
            if (checkForFileProp != null) {
              valueToSet = checkForFileProp;
            }
            Method writeMethod = propD.getWriteMethod();
            if (writeMethod == null) {
              System.err
                .println("Unable to obtain a setter method for property '"
                  + aProp.getName() + "' in step class '" + clazz);
              continue;
            }
            Object[] arguments = { valueToSet };
            writeMethod.invoke(theStep, arguments);
          }
        } catch (IntrospectionException ex) {
          System.err.println("WARNING: Unable to set property '" + aProp.getName()
            + "' in step class '" + clazz + " - skipping");
        }
      }

    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    StepManagerImpl manager = new StepManagerImpl(theStep);

    flow.addStep(manager);

    // do coordinates last
    JSONNode coords = stepNode.getChild(COORDINATES);
    if (coords != null) {
      String[] vals = coords.getValue().toString().split(",");
      int x = Integer.parseInt(vals[0]);
      int y = Integer.parseInt(vals[1]);
      manager.m_x = x;
      manager.m_y = y;
    }
  }

  /**
   * Read step connections from the supplied JSON node
   *
   * @param step the {@code JSONNode} to read from
   * @param flow the flow being constructed
   * @throws WekaException if a problem occurs
   */
  protected static void readConnectionsForStep(JSONNode step, Flow flow)
    throws WekaException {
    readConnectionsForStep(step, flow, false);
  }

  /**
   * Read step connections from the supplied JSON node
   *
   * @param step the {@code JSONNode} to read from
   * @param flow the flow being constructed
   * @param dontComplainAboutMissingConnections true if no exception should be
   *          raised if the step named in a connection is not present in the
   *          flow
   * @throws WekaException if a problem occurs
   */
  protected static void readConnectionsForStep(JSONNode step, Flow flow,
    boolean dontComplainAboutMissingConnections) throws WekaException {
    JSONNode properties = step.getChild(PROPERTIES);
    String stepName = properties.getChild("name").getValue().toString();
    StepManagerImpl manager = flow.findStep(stepName);

    JSONNode connections = step.getChild(CONNECTIONS);
    for (int i = 0; i < connections.getChildCount(); i++) {
      JSONNode conn = (JSONNode) connections.getChildAt(i);
      String conName = conn.getName();

      if (!conn.isArray()) {
        throw new WekaException(
          "Was expecting an array of connected step names "
            + "for a the connection '" + conName + "'");
      }

      for (int j = 0; j < conn.getChildCount(); j++) {
        JSONNode connectedStepName = (JSONNode) conn.getChildAt(j);
        StepManagerImpl targetManager =
          flow.findStep(connectedStepName.getValue().toString());
        if (targetManager == null && !dontComplainAboutMissingConnections) {
          throw new WekaException("Could not find the target step '"
            + connectedStepName.getValue().toString() + "' for connection "
            + "'" + connectedStepName.getValue().toString());
        }

        if (targetManager != null) {
          manager.addOutgoingConnection(conName, targetManager, true);
        }
      }
    }
  }

  /**
   * Serializes the supplied flow to JSON and writes out using the supplied
   * writer
   *
   * @param flow the {@code Flow} to serialize
   * @param writer the {@code Writer} to write to
   * @throws WekaException if a problem occurs
   */
  public static void writeFlow(Flow flow, Writer writer) throws WekaException {
    try {
      String flowJSON = flowToJSON(flow);
      writer.write(flowJSON);
    } catch (IOException ex) {
      throw new WekaException(ex);
    } finally {
      try {
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Serializes the supplied flow to JSON and writes out using the supplied
   * output stream
   *
   * @param flow the {@code Flow} to serialize
   * @param os the {@code OutputStream} to write to
   * @throws WekaException if a problem occurs
   */
  public static void writeFlow(Flow flow, OutputStream os) throws WekaException {
    OutputStreamWriter osw = new OutputStreamWriter(os);
    writeFlow(flow, osw);
  }

  /**
   * Serializes the supplied flow to JSON and writes it out to the supplied file
   *
   * @param flow the {@code Flow} to serialize
   * @param file the {@code File} to write to
   * @throws WekaException if a problem occurs
   */
  public static void writeFlow(Flow flow, File file) throws WekaException {
    try {
      Writer w = new BufferedWriter(new FileWriter(file));
      writeFlow(flow, w);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Read a flow from the supplied file
   *
   * @param file the file to read from
   * @return the deserialized {@code Flow}
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(File file) throws WekaException {
    return readFlow(file, false);
  }

  /**
   * Read a flow from the supplied file
   * 
   * @param file the file to read from
   * @param dontComplainAboutMissingConnections true if no exceptions should be
   *          raised if connections are missing in the flow
   * @return the deserialized Flow
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(File file,
    boolean dontComplainAboutMissingConnections) throws WekaException {
    try {
      Reader r = new BufferedReader(new FileReader(file));
      return readFlow(r, dontComplainAboutMissingConnections);
    } catch (FileNotFoundException e) {
      throw new WekaException(e);
    }
  }

  /**
   * Read a Flow from the supplied input stream
   *
   * @param is the {@code InputStream} to read from
   * @return the deserialized flow
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(InputStream is) throws WekaException {
    return readFlow(is, false);
  }

  /**
   * Read a Flow from the supplied input stream
   *
   * @param is the {@code InputStream} to read from
   * @param dontComplainAboutMissingConnections true if no exception should be
   *          raised if there are missing connections
   * @return the deserialized flow
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(InputStream is,
    boolean dontComplainAboutMissingConnections) throws WekaException {
    InputStreamReader isr = new InputStreamReader(is);

    return readFlow(isr, dontComplainAboutMissingConnections);
  }

  /**
   * Read a flow from the supplied reader
   * 
   * @param sr the reader to read from
   * @return the deserialized flow
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(Reader sr) throws WekaException {
    return readFlow(sr, false);
  }

  /**
   * Read a flow from the supplied reader
   * 
   * @param sr the reader to read from
   * @param dontComplainAboutMissingConnections true if no exception should be
   *          raised if there are missing connections
   * @return the deserialized flow
   * @throws WekaException if a problem occurs
   */
  public static Flow readFlow(Reader sr,
    boolean dontComplainAboutMissingConnections) throws WekaException {
    Flow flow = new Flow();

    try {
      JSONNode root = JSONNode.read(sr);
      flow.setFlowName(root.getChild(FLOW_NAME).getValue().toString());
      JSONNode stepsArray = root.getChild(STEPS);
      if (stepsArray == null) {
        throw new WekaException("Flow JSON does not contain a steps array!");
      }

      for (int i = 0; i < stepsArray.getChildCount(); i++) {
        JSONNode aStep = (JSONNode) stepsArray.getChildAt(i);
        readStep(aStep, flow);
      }

      // now fill in the connections
      for (int i = 0; i < stepsArray.getChildCount(); i++) {
        JSONNode aStep = (JSONNode) stepsArray.getChildAt(i);
        readConnectionsForStep(aStep, flow, dontComplainAboutMissingConnections);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    } finally {
      try {
        sr.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return flow;
  }

  /**
   * Utility routine to deserialize a flow from the supplied JSON string
   * 
   * @param flowJSON the string containing a flow in JSON form
   * @param dontComplainAboutMissingConnections to not raise any exceptions if
   *          there are missing connections in the flow
   * @return the deserialized flow
   * @throws WekaException if a problem occurs
   */
  public static Flow JSONToFlow(String flowJSON,
    boolean dontComplainAboutMissingConnections) throws WekaException {

    StringReader sr = new StringReader(flowJSON);
    return readFlow(sr, dontComplainAboutMissingConnections);
  }

  /**
   * Utility routine to serialize a supplied flow to a JSON string
   *
   * @param flow the flow to serialize
   * @return a string containing the flow in JSON form
   * @throws WekaException if a problem occurs
   */
  public static String flowToJSON(Flow flow) throws WekaException {

    JSONNode flowRoot = new JSONNode();
    flowRoot.addPrimitive(FLOW_NAME, flow.getFlowName());
    JSONNode flowArray = flowRoot.addArray(STEPS);
    Iterator<StepManagerImpl> iter = flow.iterator();
    if (iter.hasNext()) {
      while (iter.hasNext()) {
        StepManagerImpl next = iter.next();
        addStepJSONtoFlowArray(flowArray, next);
      }
    }

    StringBuffer b = new StringBuffer();
    flowRoot.toString(b);
    return b.toString();
  }

  /**
   * Main method for testing this class
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      weka.knowledgeflow.steps.Loader step =
        new weka.knowledgeflow.steps.Loader();
      weka.core.converters.ArffLoader arffL =
        new weka.core.converters.ArffLoader();
      arffL.setFile(new java.io.File("${user.home}/datasets/UCI/iris.arff"));
      step.setLoader(arffL);

      StepManagerImpl manager = new StepManagerImpl(step);

      Flow flow = new Flow();
      flow.addStep(manager);

      TrainingSetMaker step2 = new TrainingSetMaker();
      StepManagerImpl trainManager = new StepManagerImpl(step2);
      flow.addStep(trainManager);
      manager.addOutgoingConnection(StepManager.CON_DATASET, trainManager);

      ClassAssigner step3 = new ClassAssigner();
      StepManagerImpl assignerManager = new StepManagerImpl(step3);
      flow.addStep(assignerManager);
      trainManager.addOutgoingConnection(StepManager.CON_TRAININGSET,
        assignerManager);

      Saver step4 = new Saver();
      weka.core.converters.CSVSaver arffS = new weka.core.converters.CSVSaver();
      arffS.setDir(".");
      arffS.setFilePrefix("");

      step4.setSaver(arffS);
      StepManagerImpl saverManager = new StepManagerImpl(step4);
      // saverManager.setManagedStep(step3);
      flow.addStep(saverManager);
      assignerManager.addOutgoingConnection(StepManager.CON_TRAININGSET,
        saverManager);

      // Dummy steo2 = new Dummy();
      // StepManager manager2 = new StepManager();
      // manager2.setManagedStep(steo2);
      // flow.addStep(manager2);
      //
      // manager.addOutgoingConnection(StepManager.CON_DATASET,
      // manager2);

      // String json = flowToJSON(flow);
      // System.out.println(json);
      //
      // Flow newFlow = JSONToFlow(json);
      // json = flowToJSON(newFlow);
      // System.out.println(json);
      FlowRunner fr = new FlowRunner(new Settings("weka", KFDefaults.APP_ID));
      fr.setFlow(flow);
      fr.run();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
