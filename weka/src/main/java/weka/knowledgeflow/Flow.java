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
 *    Flow.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.WekaException;
import weka.gui.ExtensionFileFilter;
import weka.gui.Logger;
import weka.core.PluginManager;
import weka.knowledgeflow.steps.SetVariables;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that encapsulates the Steps involved in a Knowledge Flow process.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Flow {

  /** Holds available file extensions for flow files handled */
  public static final List<FileFilter> FLOW_FILE_EXTENSIONS =
    new ArrayList<FileFilter>();

  // register default loader for JSON flow files
  static {
    PluginManager.addPlugin(FlowLoader.class.getCanonicalName(),
      JSONFlowLoader.class.getCanonicalName(),
      JSONFlowLoader.class.getCanonicalName(), true);

    // TODO temporary (move to a package later)
    PluginManager.addPlugin(FlowLoader.class.getCanonicalName(),
      LegacyFlowLoader.class.getCanonicalName(),
      LegacyFlowLoader.class.getCanonicalName(), true);

    Set<String> flowLoaders =
      PluginManager.getPluginNamesOfType(FlowLoader.class.getCanonicalName());
    if (flowLoaders != null) {
      try {
        for (String f : flowLoaders) {
          FlowLoader fl =
            (FlowLoader) PluginManager.getPluginInstance(
              FlowLoader.class.getCanonicalName(), f);
          String extension = fl.getFlowFileExtension();
          String description = fl.getFlowFileExtensionDescription();
          FLOW_FILE_EXTENSIONS.add(new ExtensionFileFilter("." + extension,
            description + " (*." + extension + ")"));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /** Holds the Steps in this Flow, keyed by step name */
  protected Map<String, StepManagerImpl> m_flowSteps =
    new LinkedHashMap<String, StepManagerImpl>();

  /** The name of this flow */
  protected String m_flowName = "Untitled";

  /**
   * Utility method to get a FlowLoader implementation suitable for loading a
   * flow with the supplied file extension.
   *
   * @param flowFileExtension the file extension to get a FlowLoader for
   * @param log the log in use
   * @return a FlowLoader
   * @throws WekaException if a problem occurs
   */
  public static FlowLoader getFlowLoader(String flowFileExtension, Logger log)
    throws WekaException {
    Set<String> availableLoaders =
      PluginManager.getPluginNamesOfType(FlowLoader.class.getCanonicalName());
    FlowLoader result = null;
    if (availableLoaders != null) {
      try {
        for (String l : availableLoaders) {
          FlowLoader candidate =
            (FlowLoader) PluginManager.getPluginInstance(
              FlowLoader.class.getCanonicalName(), l);
          if (candidate.getFlowFileExtension().equalsIgnoreCase(
            flowFileExtension)) {
            result = candidate;
            break;
          }
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    }

    if (result != null) {
      result.setLog(log);
    }
    return result;
  }

  /**
   * Utility method to load a flow from a file
   *
   * @param flowFile the file to load from
   * @param log the log to use
   * @return the loaded Flow
   * @throws WekaException if a problem occurs
   */
  public static Flow loadFlow(File flowFile, Logger log) throws WekaException {
    String extension = "kf";
    if (flowFile.toString().lastIndexOf('.') > 0) {
      extension =
        flowFile.toString().substring(flowFile.toString().lastIndexOf('.') + 1,
          flowFile.toString().length());
    }
    FlowLoader toUse = getFlowLoader(extension, log);
    if (toUse == null) {
      throw new WekaException("Was unable to find a loader for flow file: "
        + flowFile.toString());
    }
    return toUse.readFlow(flowFile);
  }

  /**
   * Utility method to load a flow from the supplied input stream using the
   * supplied FlowLoader
   *
   * @param is the input stream to load from
   * @param loader the FlowLoader to use
   * @return the loaded Flow
   * @throws WekaException if a problem occurs
   */
  public static Flow loadFlow(InputStream is, FlowLoader loader)
    throws WekaException {
    return loader.readFlow(is);
  }

  /**
   * Utility method to load a flow from the supplied reader using the supplied
   * FlowLoader
   *
   * @param r the reader to load from
   * @param loader the FlowLoader to use
   * @return the loaded flow
   * @throws WekaException if a problem occurs
   */
  public static Flow loadFlow(Reader r, FlowLoader loader) throws WekaException {
    return loader.readFlow(r);
  }

  /**
   * Parse a Flow from the supplied JSON string
   * 
   * @param flowJSON the JSON string to parse
   * @return the Flow
   * @throws WekaException if a problem occurs
   */
  public static Flow JSONToFlow(String flowJSON) throws WekaException {
    return JSONToFlow(flowJSON, false);
  }

  /**
   * Parse a Flow from the supplied JSON string
   * 
   * @param flowJSON the JSON string to parse
   * @param dontComplainAboutMissingConnections true to not raise an exception
   *          if there are connections to non-existent Steps in the JSON flow
   * @return the Flow
   * @throws WekaException if a problem occurs
   */
  public static Flow JSONToFlow(String flowJSON,
    boolean dontComplainAboutMissingConnections) throws WekaException {
    return JSONFlowUtils.JSONToFlow(flowJSON,
      dontComplainAboutMissingConnections);
  }

  /**
   * Save this Flow to the supplied File
   *
   * @param file the File to save to
   * @throws WekaException if a problem occurs
   */
  public void saveFlow(File file) throws WekaException {
    JSONFlowUtils.writeFlow(this, file);
  }

  /**
   * Get the name of this Flow
   *
   * @return the name of this flow
   */
  public String getFlowName() {
    return m_flowName;
  }

  /**
   * Set the name of this Flow
   *
   * @param name the name to set
   */
  public void setFlowName(String name) {
    m_flowName = name;
  }

  /**
   * Get an ID string for this flow. The ID is made up of the FlowName + "_" +
   * the hashcode generated from the JSON representation of the flow.
   * 
   * @return
   */
  public String getFlowID() {
    String ID = getFlowName();
    try {
      ID += "_" + toJSON().hashCode();
    } catch (WekaException ex) {
      ex.printStackTrace();
    }

    return ID;
  }

  /**
   * All all steps in the supplied list to this Flow
   *
   * @param steps a list of StepManagers for the steps to add
   */
  public synchronized void addAll(List<StepManagerImpl> steps) {
    for (StepManagerImpl s : steps) {
      addStep(s);
    }
  }

  /**
   * Add the given Step to this flow
   *
   * @param manager the StepManager containing the Step to add
   */
  public synchronized void addStep(StepManagerImpl manager) {
    // int ID = manager.getManagedStep().hashCode();

    // scan for steps that already have the same name as the step being added
    String toAddName = manager.getManagedStep().getName();
    if (toAddName != null && toAddName.length() > 0) {

      boolean exactMatch = false;
      int maxCopyNum = 1;
      for (Map.Entry<String, StepManagerImpl> e : m_flowSteps.entrySet()) {
        String compName = e.getValue().getManagedStep().getName();
        if (toAddName.equals(compName)) {
          exactMatch = true;
        } else {
          if (compName.startsWith(toAddName)) {
            String num = compName.replace(toAddName, "");
            try {
              int compNum = Integer.parseInt(num);
              if (compNum > maxCopyNum) {
                maxCopyNum = compNum;
              }
            } catch (NumberFormatException ex) {
            }
          }
        }
      }

      if (exactMatch) {
        maxCopyNum++;
        toAddName += "" + maxCopyNum;
        manager.getManagedStep().setName(toAddName);
      }
    }

    m_flowSteps.put(toAddName, manager);
  }

  /**
   * Connect the supplied source and target steps using the given
   * connectionType. The connection will be successful only if both source and
   * target are actually part of this Flow, and the target is able to accept the
   * connection at this time.
   *
   * @param source the StepManager for the source step
   * @param target the StepManager for the target step
   * @param connectionType the connection type to use
   *          says it can accept the connection type at this time)
   * @return true if the connection was successful
   */
  public synchronized boolean connectSteps(StepManagerImpl source,
    StepManagerImpl target, String connectionType) {
    return connectSteps(source, target, connectionType, false);
  }

  /**
   * Connect the supplied source and target steps using the given
   * connectionType. The connection will be successful only if both source and
   * target are actually part of this Flow, and the target is able to accept the
   * connection at this time.
   *
   * @param source the StepManager for the source step
   * @param target the StepManager for the target step
   * @param connectionType the connection type to use
   * @param force true to force the connection (i.e. even if the target step
   *          says it can accept the connection type at this time)
   * @return true if the connection was successful
   */
  public synchronized boolean connectSteps(StepManagerImpl source,
    StepManagerImpl target, String connectionType, boolean force) {
    boolean connSuccessful = false;
    // make sure we contain both these steps!
    if (findStep(source.getName()) == source
      && findStep(target.getName()) == target) {

      // this takes care of ensuring that the target can accept
      // the connection at this time and the creation of the
      // incoming connection on the target
      connSuccessful = source.addOutgoingConnection(connectionType, target, force);
    }
    return connSuccessful;
  }

  /**
   * Rename the supplied step with the supplied name
   * 
   * @param step the StepManager of the Step to rename
   * @param newName the new name to give the step
   * @throws WekaException if the Step is not part of this Flow.
   */
  public synchronized void renameStep(StepManagerImpl step, String newName)
    throws WekaException {
    renameStep(step.getName(), newName);
  }

  /**
   * Rename a Step.
   *
   * @param oldName the name of the Step to rename
   * @param newName the new name to use
   * @throws WekaException if the named Step is not part of this flow
   */
  public synchronized void renameStep(String oldName, String newName)
    throws WekaException {

    if (!m_flowSteps.containsKey(oldName)) {
      throw new WekaException("Step " + oldName
        + " does not seem to be part of the flow!");
    }

    StepManagerImpl toRename = m_flowSteps.remove(oldName);
    toRename.getManagedStep().setName(newName);
    m_flowSteps.put(newName, toRename);
  }

  /**
   * Remove the supplied Step from this flow
   * 
   * @param manager the StepManager of the Step to remove
   * @throws WekaException if the step is not part of this flow
   */
  public synchronized void removeStep(StepManagerImpl manager)
    throws WekaException {
    // int ID = manager.getManagedStep().hashCode();

    if (!m_flowSteps.containsKey(manager.getManagedStep().getName())) {
      throw new WekaException("Step " + manager.getManagedStep().getName()
        + " does not seem to be part of the flow!");
    }

    m_flowSteps.remove(manager.getManagedStep().getName());
    manager.clearAllConnections();
    // remove from the map & disconnect from other steps!
    for (Map.Entry<String, StepManagerImpl> e : m_flowSteps.entrySet()) {
      e.getValue().disconnectStep(manager.getManagedStep());
    }
  }

  /**
   * Get a list of the Steps in this Flow
   * 
   * @return a list of StepManagers of the steps in this flow
   */
  public List<StepManagerImpl> getSteps() {
    return new ArrayList<StepManagerImpl>(m_flowSteps.values());
  }

  /**
   * Get an Iterator over the Steps in this flow
   * 
   * @return an Iterator over the StepManagers of the Steps in this flow
   */
  public Iterator<StepManagerImpl> iterator() {
    return m_flowSteps.values().iterator();
  }

  /**
   * Get the number of steps in this flow
   *
   * @return the number of steps in this flow
   */
  public int size() {
    return m_flowSteps.size();
  }

  /**
   * Find a Step by name
   * 
   * @param stepName the name of the Step to find
   * @return the StepManager of the named Step, or null if the Step is not part
   *         of this flow
   */
  public StepManagerImpl findStep(String stepName) {
    return m_flowSteps.get(stepName);
  }

  /**
   * Get a list of potential start points in this Flow. Potential start points
   * are those steps that have no incoming connections.
   *
   * @return a list of potential start points
   */
  public List<StepManagerImpl> findPotentialStartPoints() {
    List<StepManagerImpl> startPoints = new ArrayList<StepManagerImpl>();

    // potential start points will have no incoming connections...
    for (Map.Entry<String, StepManagerImpl> e : m_flowSteps.entrySet()) {
      StepManagerImpl candidate = e.getValue();
      if (candidate.getIncomingConnections().size() == 0) {
        startPoints.add(candidate);
      }
    }

    return startPoints;
  }

  /**
   * Initialize the flow by setting the execution environment and calling the
   * init() method of each step
   * 
   * @param executor the flow executor being used to execute the flow
   * @return false if initialization failed
   * @throws WekaException if a problem occurs during flow initialization
   */
  public boolean initFlow(FlowExecutor executor) throws WekaException {

    boolean initOK = true;
    // first set the execution environment
    for (Map.Entry<String, StepManagerImpl> s : m_flowSteps.entrySet()) {
      s.getValue().setExecutionEnvironment(executor.getExecutionEnvironment());
    }

    // scan for any SetVariables steps so that we can init() those first...
    for (Map.Entry<String, StepManagerImpl> s : m_flowSteps.entrySet()) {
      if (s.getValue().getManagedStep() instanceof SetVariables) {
        if (!s.getValue().initStep()) {
          initOK = false;
          break;
        }
      }
    }

    if (initOK) {
      // now call init() on each step
      for (Map.Entry<String, StepManagerImpl> s : m_flowSteps.entrySet()) {
        if (!s.getValue().initStep()) {
          initOK = false;
          break;
        }
      }
    }

    return initOK;
  }

  /**
   * Return the JSON encoded version of this Flow
   * 
   * @return the flow in JSON format
   * @throws WekaException if a problem occurs
   */
  public String toJSON() throws WekaException {
    return JSONFlowUtils.flowToJSON(this);
  }

  /**
   * Make a copy of this Flow
   * 
   * @return a copy of this Flow
   * @throws WekaException if a problem occurs
   */
  public Flow copyFlow() throws WekaException {
    return JSONToFlow(toJSON());
  }
}
