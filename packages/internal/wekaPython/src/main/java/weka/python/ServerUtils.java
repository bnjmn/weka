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
 *    ServerUtils.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.python;

import static org.boon.Boon.puts;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.converters.CSVSaver;
import weka.gui.Logger;

/**
 * Contains routines for getting data in and out of python.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ServerUtils {

  /**
   * Create a simple header definition to transfer as json to the server
   *
   * @param header the Instances header to convert
   * @param frameName the name of the pandas data frame that this header will
   *          refer to
   * @return A map with key values that define the header
   */
  protected static Map<String, Object> createSimpleHeader(Instances header,
    String frameName) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("relation_name", header.relationName());
    result.put("frame_name", frameName);
    result.put("class_index", header.classIndex());
    if (header.classIndex() >= 0) {
      result.put("class_name", header.classAttribute().name());
      if (header.classAttribute().isNominal()) {
        List<String> classVals = new ArrayList<String>();
        for (int i = 0; i < header.classAttribute().numValues(); i++) {
          classVals.add(header.classAttribute().value(i));
        }
        result.put("class_values", classVals);
      }
    }

    if (header.checkForAttributeType(Attribute.DATE)) {
      List<Integer> dateAtts = new ArrayList<Integer>();
      for (int i = 0; i < header.numAttributes(); i++) {
        if (header.attribute(i).isDate()) {
          dateAtts.add(i);
        }
      }
      result.put("date_atts", dateAtts);
    }

    return result;
  }

  /**
   * Converts a header definition read and decoded from json into a structure
   * only set of Instances
   *
   * @param header the map containing the definition of the header
   * @return an Instances object
   */
  @SuppressWarnings("unchecked")
  protected static Instances jsonToInstancesHeader(Map<String, Object> header) {
    String relationName = header.get("relation_name").toString();
    List<Map<String, Object>> attributes =
      (List<Map<String, Object>>) header.get("attributes");
    if (attributes == null) {
      throw new IllegalStateException("No attributes in header map!");
    }

    ArrayList<weka.core.Attribute> atts = new ArrayList<Attribute>();
    for (Map<String, Object> a : attributes) {
      String attName = a.get("name").toString();
      String type = a.get("type").toString();
      String format = null;
      List<String> values = null;
      if (a.get("format") != null) {
        format = a.get("format").toString();
      }
      if (a.get("values") != null) {
        values = (List<String>) a.get("values");
      }

      if (type.equals("NUMERIC")) {
        atts.add(new Attribute(attName));
      } else if (type.equals("DATE")) {
        atts.add(new Attribute(attName, format));
      } else if (type.equals("NOMINAL")) {
        atts.add(new Attribute(attName, values));
      } else if (type.equals("STRING")) {
        atts.add(new Attribute(attName, (List<String>) null));
      }
    }

    return new Instances(relationName, atts, 0);
  }

  /**
   * Send a shutdown command to the micro server
   *
   * @param outputStream the output stream to write the command to
   * @throws WekaException if a problem occurs
   */
  protected static void sendServerShutdown(OutputStream outputStream)
    throws WekaException {
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "shutdown");
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectMapper mapper = JsonFactory.create();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();
    try {
      // write the command
      writeDelimitedToOutputStream(bytes, outputStream);
    } catch (IOException ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Execute a script on the server
   * 
   * @param script the script to execute
   * @param outputStream the output stream to write data to the server
   * @param inputStream the input stream to read responses from
   * @param log optional log to write to
   * @param debug true to output debugging info from both java and the server
   * @return a two element list that contains the sys out and sys error from the
   *         script execution
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static List<String> executeUserScript(String script,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {
    if (!script.endsWith("\n")) {
      script += "\n";
    }
    List<String> outAndErr = new ArrayList<String>();

    ObjectMapper objectMapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "execute_script");
    command.put("script", script);
    command.put("debug", debug);
    if (inputStream != null && outputStream != null) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      objectMapper.writeValue(bos, command);
      byte[] bytes = bos.toByteArray();
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        writeDelimitedToOutputStream(bytes, outputStream);

        // get the result of execution
        bytes = readDelimitedFromInputStream(inputStream);
        ObjectMapper mapper = JsonFactory.create();
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }
        // get the script out and err
        outAndErr.add(ack.get("script_out").toString());
        outAndErr.add(ack.get("script_error").toString());
        if (debug) {
          if (log != null) {
            log.logMessage("Script output:\n" + outAndErr.get(0));
            log.logMessage("\nScript error:\n" + outAndErr.get(1));
          } else {
            System.err.println("Script output:\n" + outAndErr.get(0));
            System.err.println("\nScript error:\n" + outAndErr.get(1));
          }
        }

        if (outAndErr.get(1).contains("Warning:")) {
          // clear warnings - we really just want to know if there
          // are major errors
          outAndErr.set(1, "");
        }
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    return outAndErr;
  }

  /**
   * Sends instances to a pandas dataframe in python. Assumes data has been
   * binarized (as scikit-learn algorithms take only numeric data) and have had
   * missing values replaced. Creates up to two numpy arrays in python called X
   * and Y: input columns and target (if class is set) respectively
   *
   * @param instances the instances to transfer
   * @param frameName the name of the pandas dataframe
   * @param outputStream the output stream to write to
   * @param inputStream the input stream to listen for server response on
   * @param log the log (if any) to use
   * @param debug true if debugging info is to be output
   * @throws WekaException if a problem occurs
   */
  protected static void sendInstancesScikitLearn(Instances instances,
    String frameName, OutputStream outputStream, InputStream inputStream,
    Logger log, boolean debug) throws WekaException {
    // iris.iloc[:,[0,2,4]] (slice columns to array)
    // pd.get_dummies(iris) (binarize/one hot)

    // Assumes that data has had nominals (except the class) converted
    // to binary indicators and all missing values replaced
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> simpleHeader = createSimpleHeader(instances, frameName);
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "put_instances");
    command.put("num_instances", instances.numInstances());
    command.put("header", simpleHeader);
    command.put("debug", debug);
    if (inputStream != null && outputStream != null) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      mapper.writeValue(bos, command);
      byte[] bytes = bos.toByteArray();
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);
        if (instances.numInstances() > 0) {
          StringBuilder builder = new StringBuilder();

          // header row
          for (int i = 0; i < instances.numAttributes(); i++) {
            builder.append(Utils.quote(instances.attribute(i).name()));
            if (i < instances.numAttributes() - 1) {
              builder.append(",");
            } else {
              builder.append("\n");
            }
          }

          // instances
          for (int i = 0; i < instances.numInstances(); i++) {
            Instance current = instances.instance(i);
            for (int j = 0; j < instances.numAttributes(); j++) {
              builder.append(current.value(j));
              if (j < instances.numAttributes() - 1) {
                builder.append(",");
              } else {
                builder.append("\n");
              }
            }
          }
          if (debug) {
            System.err.println(builder.toString());
          }
          bos = new ByteArrayOutputStream();
          BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos));
          bw.write(builder.toString());
          bw.flush();
          bytes = bos.toByteArray();
          writeDelimitedToOutputStream(bytes, outputStream);

          String serverAck = receiveServerAck(inputStream);
          if (serverAck != null) {
            throw new WekaException("Transfer of instances failed: "
              + serverAck);
          }

          // execute script to create X (and Y) arrays
          int classIndex = instances.classIndex();
          builder = new StringBuilder();
          for (int i = 0; i < instances.numAttributes(); i++) {
            if (i != classIndex) {
              builder.append(i).append(",");
            }
          }
          String xList = builder.substring(0, builder.length() - 1);
          builder = new StringBuilder();
          builder.append("X = " + frameName + ".iloc[:,[" + xList
            + "]].values\n");
          if (classIndex >= 0) {
            builder.append("Y = " + frameName + ".iloc[:,[" + classIndex
              + "]].values\n");
          }

          if (debug) {
            if (log != null) {
              log.logMessage("Executing python script:\n\n"
                + builder.toString());
            } else {
              System.err.println("Executing python script:\n\n"
                + builder.toString());
            }
          }

          List<String> outErr =
            executeUserScript(builder.toString(), outputStream, inputStream,
              log, debug);

          if (outErr.size() == 2 && outErr.get(1).length() > 0) {
            throw new WekaException(outErr.get(1));
          }
        }
      } catch (IOException e) {
        throw new WekaException(e);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }
  }

  /**
   * Sends instances to a pandas dataframe in python.
   *
   * @param instances the instances to transfer
   * @param frameName the name of the data frame to create in python
   * @param outputStream the output stream to write to the server
   * @param inputStream the input stream to get server responses from
   * @param log optional log
   * @param debug true if debugging info is to be output
   * @throws WekaException if a problem occurs
   */
  protected static void sendInstances(Instances instances, String frameName,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> simpleHeader = createSimpleHeader(instances, frameName);
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "put_instances");
    command.put("num_instances", instances.numInstances());
    command.put("header", simpleHeader);
    command.put("debug", debug);

    if (instances.checkForAttributeType(Attribute.DATE)) {
      // ensure a single, consistent date format
      ArrayList<Attribute> newAtts = new ArrayList<Attribute>();
      for (int i = 0; i < instances.numAttributes(); i++) {
        if (!instances.attribute(i).isDate()) {
          newAtts.add((Attribute) instances.attribute(i).copy());
        } else {
          Attribute newDate =
            new Attribute(instances.attribute(i).name(), "yyyy-MM-dd HH:mm:ss");
          newAtts.add(newDate);
        }
      }
      Instances newInsts =
        new Instances(instances.relationName(), newAtts,
          instances.numInstances());
      for (int i = 0; i < instances.numInstances(); i++) {
        newInsts.add(instances.instance(i));
      }
      newInsts.setClassIndex(instances.classIndex());
      instances = newInsts;
    }

    if (inputStream != null && outputStream != null) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      mapper.writeValue(bos, command);
      byte[] bytes = bos.toByteArray();
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        // write instances as CSV
        if (instances.numInstances() > 0) {
          CSVSaver saver = new CSVSaver();
          saver.setInstances(instances);
          bos = new ByteArrayOutputStream();
          saver.setDestination(bos);
          saver.writeBatch();
          bytes = bos.toByteArray();
          writeDelimitedToOutputStream(bytes, outputStream);
        }

        String serverAck = receiveServerAck(inputStream);
        if (serverAck != null) {
          throw new WekaException("Transfer of instances failed: " + serverAck);
        }
      } catch (IOException e) {
        throw new WekaException(e);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }
  }

  /**
   * Recieve a simple ack from the server. Returns a non-null string if the ack
   * received contains an error message
   *
   * @param inputStream the input stream to read the ack from
   * @return a non-null string if there was an error returned by the server
   * @throws IOException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static String receiveServerAck(InputStream inputStream)
    throws IOException {
    byte[] bytes = readDelimitedFromInputStream(inputStream);
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> ack = mapper.readValue(bytes, Map.class);

    String response = ack.get("response").toString();
    if (response.equals("ok")) {
      return null;
    }

    return ack.get("error_message").toString();
  }

  /**
   * Receives a PID ack from the server
   *
   * @param inputStream the input stream to read from
   * @return the process ID of the server
   * @throws IOException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static int receiveServerPIDAck(InputStream inputStream)
    throws IOException {
    byte[] bytes = readDelimitedFromInputStream(inputStream);
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> ack = mapper.readValue(bytes, Map.class);

    String response = ack.get("response").toString();
    if (response.equals("pid_response")) {
      return (Integer) ack.get("pid");
    } else {
      throw new IOException("Server did not send a pid_response");
    }
  }

  /**
   * Receives the current list of variables from Python
   *
   * @param outputStream the output stream to
   * @param inputStream the input stream to read from
   * @param log the log to use
   * @param debug true if debugging info is to be output
   * @return a list of variables set in python. Each entry in the list is a two
   *         element array, where the first element holds the variable name and
   *         the second the python type
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static List<String[]> receiveVariableList(
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    List<String[]> results = new ArrayList<String[]>();
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_variable_list");
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();
    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }

        Object l = ack.get("variable_list");
        if (!(l instanceof List)) {
          throw new WekaException(
            "Was expecting the variable list to be a List " + "object!");
        }
        List<Map<String, String>> vList = (List<Map<String, String>>) l;
        for (Map<String, String> v : vList) {
          String[] vEntry = new String[2];
          vEntry[0] = v.get("name");
          vEntry[1] = v.get("type");
          results.add(vEntry);
        }
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    return results;
  }

  /**
   * Receive the value of a variable in python in json form
   *
   * @param varName the name of the variable to get from python
   * @param outputStream the output stream to write to
   * @param inputStream the input stream to get server responses from
   * @param log optional log
   * @param debug true if debugging info is to be output
   * @return the value of the variable in json form
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static Object receiveJsonVariableValue(String varName,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    Object variableValue = "";
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_variable_value");
    command.put("variable_name", varName);
    command.put("variable_encoding", "json");
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();
    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }
        if (!ack.get("variable_name").toString().equals(varName)) {
          throw new WekaException("Server sent back a value for a different "
            + "variable!");
        }
        if (!ack.get("variable_encoding").toString().equals("json")) {
          throw new WekaException("Encoding of variable value received from "
            + "server is not Json!");
        }
        variableValue = ack.get("variable_value");
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    return variableValue;
  }

  /**
   * Receive the value of a variable in pickled or plain string form. If getting
   * a pickled variable, then in python 2 this is the pickled string; in python
   * 3 pickle.dumps returns a byte object, so the value is converted to base64
   * before leaving the server.
   *
   * @param varName the name of the variable to get from the server
   * @param outputStream the output stream to write to
   * @param inputStream the input stream to get server responses from
   * @param plainString true if the plain string form of the variable is to be
   *          returned; otherwise the variable value is pickled (and further
   *          encoded to base64 in the case of python 3)
   * @param log optional log
   * @param debug true if debugging info is to be output
   * @return the variable value
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static String receivePickledVariableValue(String varName,
    OutputStream outputStream, InputStream inputStream, boolean plainString,
    Logger log, boolean debug) throws WekaException {

    String objectValue = "";
    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_variable_value");
    command.put("variable_name", varName);
    command.put("variable_encoding", plainString ? "string" : "pickled");
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }
        if (!ack.get("variable_name").toString().equals(varName)) {
          throw new WekaException("Server sent back a value for a different "
            + "variable!");
        }
        objectValue = ack.get("variable_value").toString();
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    return objectValue;
  }

  /**
   * Std out and err are redirected to StringIO objects in the server. This
   * method retrieves the values of those buffers.
   *
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive server responses from
   * @param log optional log
   * @param debug true to output debugging info
   * @return the std out and err strings as a two element list
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static List<String> receiveDebugBuffer(OutputStream outputStream,
    InputStream inputStream, Logger log, boolean debug) throws WekaException {
    List<String> stdOutStdErr = new ArrayList<String>();

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_debug_buffer");
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }
        Object stOut = ack.get("std_out");
        stdOutStdErr.add(stOut != null ? stOut.toString() : "");
        Object stdErr = ack.get("std_err");
        stdOutStdErr.add(stdErr != null ? stdErr.toString() : "");
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    return stdOutStdErr;
  }

  /**
   * Send the pickled (and possibly base64 encoded) value of a variable to the
   * server. Sets the decoded value in python.
   *
   * @param varName the name of the variable to be set in python
   * @param varValue the value of the variable
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive responses on
   * @param log optional log
   * @param debug true if debugging info is to be output
   * @throws WekaException if a problem occurs
   */
  protected static void sendPickledVariableValue(String varName,
    String varValue, OutputStream outputStream, InputStream inputStream,
    Logger log, boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "set_variable_value");
    command.put("variable_name", varName);
    command.put("variable_encoding", "pickled");
    command.put("variable_value", varValue);
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();
    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        String serverAck = receiveServerAck(inputStream);
        if (serverAck != null) {
          throw new WekaException(serverAck);
        }
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }
  }

  /**
   * Get an image from python. Assumes that the image is a
   * matplotlib.figure.Figure object. Retrieves this as png data and returns a
   * BufferedImage.
   *
   * @param varName the name of the variable containing the image in python
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive server responses from
   * @param log an optional log
   * @param debug true to output debug info
   * @return a BufferedImage
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static BufferedImage getPNGImageFromPython(String varName,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_image");
    command.put("variable_name", varName);
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        mapper = JsonFactory.create();
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }

        if (!ack.get("variable_name").toString().equals(varName)) {
          throw new WekaException(
            "Server sent back a response for a different " + "variable!");
        }

        String encoding = ack.get("encoding").toString();
        String imageData = ack.get("image_data").toString();
        byte[] imageBytes;
        if (encoding.equals("base64")) {
          imageBytes = Base64.decodeBase64(imageData.getBytes());
        } else {
          imageBytes = imageData.getBytes();
        }
        return ImageIO.read(new BufferedInputStream(new ByteArrayInputStream(
          imageBytes)));
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else {
      outputCommandDebug(command, log);
    }
    return null;
  }

  /**
   * Get the type of a variable in python
   *
   * @param varName the name of the variable to check
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive server responses from
   * @param log an optional log
   * @param debug true to output debug info
   * @return the type of the variable in python
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static PythonSession.PythonVariableType getPythonVariableType(
    String varName, OutputStream outputStream, InputStream inputStream,
    Logger log, boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_variable_type");
    command.put("variable_name", varName);
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        mapper = JsonFactory.create();
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }

        if (!ack.get("variable_name").toString().equals(varName)) {
          throw new WekaException(
            "Server sent back a response for a different " + "variable!");
        }

        String varType = ack.get("type").toString();
        PythonSession.PythonVariableType pvt =
          PythonSession.PythonVariableType.Unknown;
        for (PythonSession.PythonVariableType t : PythonSession.PythonVariableType
          .values()) {
          if (t.toString().toLowerCase().equals(varType)) {
            pvt = t;
            break;
          }
        }
        return pvt;
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else {
      outputCommandDebug(command, log);
    }

    return PythonSession.PythonVariableType.Unknown;
  }

  /**
   * Check if a named variable has a value in python
   *
   * @param varName the name of the variable to check
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive server responses from
   * @param log an optional log
   * @param debug true to output debug info
   * @return true if the named variable is set in python
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static boolean checkIfPythonVariableIsSet(String varName,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "variable_is_set");
    command.put("variable_name", varName);
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }
        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);

        bytes = readDelimitedFromInputStream(inputStream);
        mapper = JsonFactory.create();
        Map<String, Object> ack = mapper.readValue(bytes, Map.class);
        if (!ack.get("response").toString().equals("ok")) {
          // fatal error
          throw new WekaException(ack.get("error_message").toString());
        }

        if (!ack.get("variable_name").toString().equals(varName)) {
          throw new WekaException(
            "Server sent back a response for a different " + "variable!");
        }

        return (Boolean) ack.get("variable_exists");
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }

    // TODO
    return true;
  }

  /**
   * Retrieve a pandas data frame from python. The server sends the header
   * information in json form followed by CSV data (without a header row).
   *
   * @param frameName the name of the pandas data frame to get from the server
   * @param outputStream the output stream to talk to the server on
   * @param inputStream the input stream to receive responses on
   * @param log optional log
   * @param debug true if debugging info is to be output
   * @return the panadas data frame as a set of Instances
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static Instances receiveInstances(String frameName,
    OutputStream outputStream, InputStream inputStream, Logger log,
    boolean debug) throws WekaException {

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> command = new HashMap<String, Object>();
    command.put("command", "get_instances");
    command.put("frame_name", frameName);
    command.put("debug", debug);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, command);
    byte[] bytes = bos.toByteArray();

    if (inputStream != null && outputStream != null) {
      try {
        if (debug) {
          outputCommandDebug(command, log);
        }

        // write the command
        writeDelimitedToOutputStream(bytes, outputStream);
        String serverAck = receiveServerAck(inputStream);
        if (serverAck != null) {
          throw new WekaException(serverAck);
        }

        // read the header
        bytes = readDelimitedFromInputStream(inputStream);
        Map<String, Object> headerResponse = mapper.readValue(bytes, Map.class);
        if (headerResponse == null) {
          throw new WekaException("Map is null!");
        }
        if (headerResponse.get("response").toString()
          .equals("instances_header")) {
          if (debug) {
            if (log != null) {
              log.logMessage("Received header response command with "
                + headerResponse.get("num_instances") + " instances");
            } else {
              System.err.println("Received header response command with "
                + headerResponse.get("num_instances") + " instances");
            }
          }
        } else {
          throw new WekaException("Unknown response type from server");
        }

        Instances header =
          jsonToInstancesHeader((Map<String, Object>) headerResponse
            .get("header"));

        // receive the CSV data, append with header, and then create
        // instances
        bytes = readDelimitedFromInputStream(inputStream);
        String CSV = new String(bytes);
        StringBuilder b = new StringBuilder();
        b.append(header.toString()).append("\n");
        b.append(CSV).append("\n");

        return new Instances(new StringReader(b.toString()));
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else if (debug) {
      outputCommandDebug(command, log);
    }
    return null;
  }

  /**
   * Write length delimited data to the output stream
   *
   * @param bytes the bytes to write
   * @param outputStream the output stream to write to
   * @throws IOException if a problem occurs
   */
  protected static void writeDelimitedToOutputStream(byte[] bytes,
    OutputStream outputStream) throws IOException {

    // write the message length as a fixed size integer
    outputStream.write(ByteBuffer.allocate(4).putInt(bytes.length).array());

    // write the message itself
    outputStream.write(bytes);
  }

  /**
   * Read length delimited data from the input stream
   *
   * @param inputStream the input stream to read from
   * @return the bytes read
   * @throws IOException if a problem occurs
   */
  protected static byte[] readDelimitedFromInputStream(InputStream inputStream)
    throws IOException {
    byte[] sizeBytes = new byte[4];
    int numRead = inputStream.read(sizeBytes, 0, 4);
    if (numRead < 4) {
      throw new IOException(
        "Failed to read the message size from the input stream!");
    }

    int messageLength = ByteBuffer.wrap(sizeBytes).getInt();
    byte[] messageData = new byte[messageLength];
    // for (numRead = 0; numRead < messageLength; numRead +=
    // inputStream.read(messageData, numRead, messageLength - numRead));
    for (numRead = 0; numRead < messageLength;) {
      int currentNumRead =
        inputStream.read(messageData, numRead, messageLength - numRead);
      if (currentNumRead < 0) {
        throw new IOException("Unexpected end of stream!");
      }
      numRead += currentNumRead;
    }

    return messageData;
  }

  /**
   * Prints out a json command for debugging purposes
   *
   * @param command the command to print out
   * @param log optional log
   */
  protected static void outputCommandDebug(Map<String, Object> command,
    Logger log) {
    ObjectMapper mapper = JsonFactory.create();
    String serialized = mapper.writeValueAsString(command);
    if (log != null) {
      log.logMessage("Sending command:\n" + serialized);
    } else {
      System.err.println("Sending command: ");
      puts(serialized);
    }
  }

  public static void main(String[] args) {
    try {
      Instances insts = new Instances(new FileReader(args[0]));
      insts.setClassIndex(insts.numAttributes() - 1);
      sendInstances(insts, "test", null, null, null, false);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
