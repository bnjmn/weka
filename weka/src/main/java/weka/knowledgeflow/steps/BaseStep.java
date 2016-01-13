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

  /** True if execution is to be stopped as soon as possible */
  protected boolean m_stopRequested;

  /** True if the step is busy processing */
  protected boolean m_busy;

  /** The name of this step component */
  protected String m_stepName = "";

  /** The step manager to use */
  protected transient StepManager m_stepManager;

  /** True if the step is resource (cpu/memory) intensive */
  protected boolean m_stepIsResourceIntensive;

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

  @NotPersistable
  @Override
  public StepManager getStepManager() {
    return m_stepManager;
  }

  @Override
  public void setStepManager(StepManager manager) {
    m_stepManager = manager;
  }

  @Override
  public void init() throws WekaException {
    m_stopRequested = false;
    m_busy = false;
    stepInit();
  }

  @ProgrammaticProperty
  public void setStepIsResourceIntensive(boolean isResourceIntensive) {
    getStepManager().setStepIsResourceIntensive(isResourceIntensive);
  }

  public boolean isResourceIntensive() {
    return getStepManager().stepIsResourceIntensive();
  }

  @Override
  public String getName() {
    return m_stepName;
  }

  @ProgrammaticProperty
  @Override
  public void setName(String name) {
    m_stepName = name;
  }

  @Override
  public void start() throws WekaException {
    // no-op. Subclass should override if it acts as a start point
  }

  @Override
  public void stop() {
    getStepManager().statusMessage("INTERRUPTED");
    getStepManager().log("Interrupted", LoggingLevel.LOW);
    m_stopRequested = true;

    // if this step is processing incrementally then this will ensure
    // that the busy flag gets set to false. This means that clients
    // processing incremental stuff *must* use the throughput update
    // mechanism
    getStepManager().throughputUpdateEnd();
  }

  @NotPersistable
  @Override
  public boolean isBusy() {
    return m_busy;
  }

  @ProgrammaticProperty
  @Override
  public void setBusy(boolean busy) {
    m_busy = busy;
  }

  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {
    // no-op default
    return null;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {
    // no-op. Subclass should override if it accepts incoming data
  }

  @Override
  public String getCustomEditorForStep() {
    return null;
  }

  @Override
  public Map<String, String> getInteractiveViewers() {
    return null;
  }

  @Override
  public Map<String, StepInteractiveViewer> getInteractiveViewersImpls() {
    return null;
  }

  @Override
  public Defaults getDefaultSettings() {
    return null;
  }

  @Override
  public boolean isStopRequested() {
    return m_stopRequested;
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
