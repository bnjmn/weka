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
 *    BaseStep.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Defaults;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepInteractiveViewer;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.LoggingLevel;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Base class for implementations of Step to use. Provides handy functions that
 * automatically setup the step's name and "about" info, provide access to the
 * step's StepManager and for resolving environment variables.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class BaseStep implements Step, BaseStepExtender, Serializable {

  private static final long serialVersionUID = -1595753549991953141L;

  /** The name of this step component */
  protected String m_stepName = "";

  /** The step manager to use */
  protected transient StepManager m_stepManager;

  /** True if the step is resource (cpu/memory) intensive */
  protected boolean m_stepIsResourceIntensive;

  /**
   * Constructor
   */
  public BaseStep() {
    String clazzName = this.getClass().getCanonicalName();
    clazzName = clazzName.substring(clazzName.lastIndexOf(".") + 1);
    setName(clazzName);
    Annotation[] annotations = this.getClass().getAnnotations();
    for (Annotation a : annotations) {
      if (a instanceof KFStep) {
        String name = ((KFStep) a).name();
        if (name.length() > 0) {
          setName(name);
          break;
        }
      }
    }
  }

  /**
   * Attempt to get default "about" information for this step by grabbing the
   * toolTip from the KFStep annotation.
   * 
   * @return a default "about" info string if this step uses the KFStep
   *         annotation and null otherwise. Subclasses should override to
   *         provide more comprehensive about info
   */
  public String globalInfo() {
    Annotation[] annotations = this.getClass().getAnnotations();
    for (Annotation a : annotations) {
      if (a instanceof KFStep) {
        return ((KFStep) a).toolTipText();
      }
    }

    return null;
  }

  /**
   * Get the step manager for this step
   *
   * @return the step manager for this step
   */
  @NotPersistable
  @Override
  public StepManager getStepManager() {
    return m_stepManager;
  }

  /**
   * Set the step manager for this step
   *
   * @param manager the step manager to use
   */
  @Override
  public void setStepManager(StepManager manager) {
    m_stepManager = manager;
  }

  /**
   * Set whether this step is resource intensive (cpu/memory) or not. This
   * affects which executor service is used to execute the step's processing.
   *
   * @param isResourceIntensive true if this step is resource intensive.
   */
  @ProgrammaticProperty
  public void setStepIsResourceIntensive(boolean isResourceIntensive) {
    getStepManager().setStepIsResourceIntensive(isResourceIntensive);
  }

  /**
   * Get whether this step is resource intensive (cpu/memory) or not.
   *
   * @return true if this step is resource intensive
   */
  public boolean isResourceIntensive() {
    return getStepManager().stepIsResourceIntensive();
  }

  /**
   * Set whether this step must run single threaded. I.e. on an executor
   * service which has only one worker thread, thus effectively preventing
   * more than one copy of the step executing at any one time.
   *
   * @param mustRunSingleThreaded true if the step must run single threaded
   */
  @ProgrammaticProperty
  public void setStepMustRunSingleThreaded(boolean mustRunSingleThreaded) {
    getStepManager().setStepMustRunSingleThreaded(mustRunSingleThreaded);
  }

  /**
   * Get whether this step must run single threaded. I.e. on an executor
   * service which has only one worker thread, thus effectively preventing
   * more than one copy of the step executing at any one time.
   *
   * @return true if the step must run single threaded
   */
  public boolean stepMustRunSingleThreaded() {
    return getStepManager().getStepMustRunSingleThreaded();
  }

  /**
   * Get the name of this step
   *
   * @return the name of this step
   */
  @Override
  public String getName() {
    return m_stepName;
  }

  /**
   * Set the name of this step
   *
   * @param name the name for this step
   */
  @ProgrammaticProperty
  @Override
  public void setName(String name) {
    m_stepName = name;
  }

  /**
   * Start processing. Subclasses should override this method if they can act as
   * a start point in a flow.
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    // no-op. Subclass should override if it acts as a start point
  }

  /**
   * Request that processing be stopped. Subclasses should call
   * {@code isStopRequested()} periodically to see if they should stop
   * processing.
   */
  @Override
  public void stop() {

    if (!(this instanceof Note)) {
      // don't want any logging or status updates for Notes :-)
      getStepManager().statusMessage("INTERRUPTED");
      getStepManager().log("Interrupted", LoggingLevel.LOW);
    }
    // m_stopRequested = true;
    ((StepManagerImpl) getStepManager()).setStopRequested(true);

    // if this step is processing incrementally then this will ensure
    // that the busy flag gets set to false. This means that clients
    // processing incremental stuff *must* use the throughput update
    // mechanism
    getStepManager().throughputUpdateEnd();
  }

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
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    // no-op default
    return null;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    // no-op. Subclass should override if it accepts incoming data
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return null;
  }

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
  @Override
  public Map<String, String> getInteractiveViewers() {
    return null;
  }

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
  @Override
  public Map<String, StepInteractiveViewer> getInteractiveViewersImpls() {
    return null;
  }

  /**
   * Get default settings for the step (if any). Returning null indicates that
   * the step has no user-editable defaults.
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return null;
  }

  /**
   * Convenience method that calls {@code StepManager.isStopRequested()}
   *
   * @return true if the execution environment has requested processing to stop
   */
  public boolean isStopRequested() {
    return getStepManager().isStopRequested();
  }

  /**
   * Substitute the values of environment variables in the given string
   * 
   * @param source the source string to substitute in
   * @return the source string with all known environment variables resolved
   */
  public String environmentSubstitute(String source) {
    return getStepManager().environmentSubstitute(source);
  }
}
