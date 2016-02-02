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
 *    Step.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Defaults;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.StepInteractiveViewer;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.List;
import java.util.Map;

/**
 * Client API for Knowledge Flow steps. Typically, an implementation would
 * extend BaseStep. A minimal subset of the methods in this class that a simple
 * implementation extending BaseStep would need to address is also specified in
 * the BaseStepExtender interface.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see BaseStep
 * @see BaseStepExtender
 */
public interface Step {

  /**
   * Get the step manager in use with this step
   *
   * @return the step manager
   */
  StepManager getStepManager();

  /**
   * Set the step manager to use with this step. The execution environment will
   * call this method to provide a StepManager.
   *
   * @param manager the step manager to use
   */
  void setStepManager(StepManager manager);

  /**
   * Get the name of this step
   *
   * @return the name of this step
   */
  String getName();

  /**
   * Set the name for this step
   *
   * @param name the name for this step
   */
  void setName(String name);

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  void stepInit() throws WekaException;

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   * 
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  List<String> getIncomingConnectionTypes();

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   * 
   * @return a list of outgoing connections that this step can produce
   */
  List<String> getOutgoingConnectionTypes();

  /**
   * Start executing (if this component is a start point)
   * 
   * @throws WekaException if a problem occurs
   */
  void start() throws WekaException;

  /**
   * Request a stop to all processing by this step (as soon as possible)
   */
  void stop();

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   * 
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  void processIncoming(Data data) throws WekaException;

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   * 
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  Instances outputStructureForConnectionType(String connectionName)
    throws WekaException;

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   * 
   * @return the fully qualified name of a step editor component
   */
  String getCustomEditorForStep();

  /**
   * When running in a graphical execution environment a step can make one or
   * more popup Viewer components available. These might be used to display
   * results, graphics etc. Returning null indicates that the step has no such
   * additional graphical views. The map returned by this method should be keyed
   * by action name (e.g. "View results"), and values should be fully qualified
   * names of the corresponding StepInteractiveView implementation. Furthermore,
   * the contents of this map can (and should) be dependent on whether a
   * particular viewer should be made available - i.e. if execution hasn't
   * occurred yet, or if a particular incoming connection type is not present,
   * then it might not be possible to view certain results.
   *
   * Viewers can implement StepInteractiveView directly (in which case they need
   * to extends JPanel), or extends the AbstractInteractiveViewer class. The
   * later extends JPanel, uses a BorderLayout, provides a "Close" button and a
   * method to add additional buttons.
   * 
   * @return a map of viewer component names, or null if this step has no
   *         graphical views
   */
  Map<String, String> getInteractiveViewers();

  /**
   * An alternative to getStepInteractiveViewers that returns a Map of
   * instantiated StepInteractiveViewer objects. Generally,
   * getInteractiveViewers() is the preferred mechanism to specify any
   * interactive viewers, as it does not require Steps to import and instantiate
   * GUI classes. However, in some cases it might be unavoidable (e.g. Groovy
   * script compilation involves custom classloaders), in these cases this
   * method can be used instead.
   *
   * @return a map of instantiated instances of StepInteractiveViewers
   */
  Map<String, StepInteractiveViewer> getInteractiveViewersImpls();

  /**
   * Get default settings for the step (if any). Returning null indicates that
   * the step has no user-editable defaults.
   *
   * @return the default settings
   */
  Defaults getDefaultSettings();
}
