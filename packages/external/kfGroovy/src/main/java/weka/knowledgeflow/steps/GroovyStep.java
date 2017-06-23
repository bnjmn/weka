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
 *    GroovyStep.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import groovy.lang.GroovyClassLoader;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.knowledgeflow.StepInteractiveViewer;
import weka.knowledgeflow.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A KnowledgeFlow step that allows the user to write an implementation of a
 * Step in the Groovy scripting language. The Groovy script gets compiled
 * dynamically at runtime. This makes for a fast way to prototype new processing
 * steps for the Knowledge Flow
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "GroovyStep", category = "Scripting",
  toolTipText = "Implement a Knowledge Flow step using Groovy",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "GroovyStep.gif")
public class GroovyStep extends BaseStep {

  private static final long serialVersionUID = -8984243132321480487L;

  /** The object constructed from the script */
  protected transient Step m_groovyObject;

  /** The script to compile and execute */
  protected String m_groovyScript = "";

  /**
   * Set the script to execute
   *
   * @param script the script to execute
   */
  @ProgrammaticProperty
  public void setScript(String script) {
    m_groovyScript = script;
    m_groovyObject = null;
  }

  /**
   * Get the script to execute
   *
   * @return the script to execute
   */
  public String getScript() {
    return m_groovyScript;
  }

  /**
   * Get the compiled and instantiated Groovy step implementation
   *
   * @return the object compiled and instantiated from the script
   */
  public Step getWrappedGroovyStep() {
    return m_groovyObject;
  }

  /**
   * Unitialize this step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (m_groovyScript.length() > 0) {
      try {
        if (m_groovyObject == null) {
          m_groovyObject = (Step) compileScript(m_groovyScript);
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else {
      throw new WekaException("No Groovy script to execute!");
    }

    // Use our step manager with the script
    m_groovyObject.setStepManager(getStepManager());

    // Init the script
    m_groovyObject.stepInit();
  }

  /**
   * Get a list of incoming connection types we can accept at this time
   *
   * @return a list of incoming connection types that we can accept
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    if (m_groovyScript.length() > 0) {
      // compile if necessary
      if (m_groovyObject == null) {
        try {
          m_groovyObject = compileScript(m_groovyScript);
        } catch (Exception ex) {
          return new ArrayList<String>();
        }
      }
      m_groovyObject.setStepManager(getStepManager());
      // delegate to the script
      return m_groovyObject.getIncomingConnectionTypes();
    } else {
      return new ArrayList<String>();
    }
  }

  /**
   * Get a list of outgoing connection types we can produce at this time
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (m_groovyScript.length() > 0) {
      // compile if necessary
      if (m_groovyObject == null) {
        try {
          m_groovyObject = compileScript(m_groovyScript);
        } catch (Exception ex) {
          return new ArrayList<String>();
        }
      }
      m_groovyObject.setStepManager(getStepManager());
      // delegate to the script
      return m_groovyObject.getOutgoingConnectionTypes();
    } else {
      return new ArrayList<String>();
    }
  }

  /**
   * Start processing as a start point - just gets passed to the Groovy script
   * to handle
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void start() throws WekaException {
    ((Step) m_groovyObject).start();
  }

  /**
   * Process incoming data objects - just get passed to the Groovy script to
   * handle
   *
   * @param data the Data object to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {

    // delegate
    ((Step) m_groovyObject).processIncoming(data);
  }

  /**
   * Get the name of the script editor GUI component
   *
   * @return the fully qualified name of the script editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.GroovyStepEditorDialog";
  }

  /**
   * Gets a map of named implementations of StepInteractiveViewer that the
   * Groovy script Step implementation produces. We implement this method rather
   * than getInteractiveViewers() (which returns fully qualified class names as
   * strings) due to issues with the Groovy classloader. If groovy scripts wish
   * to provided interactive GUI "viewers" then they will need to implement this
   * method and return instantiated instances of their viewers in the map.
   *
   * @return a map of named instantiated viewer objects
   */
  @Override
  public Map<String, StepInteractiveViewer> getInteractiveViewersImpls() {
    if (m_groovyScript.length() > 0) {
      // compile if necessary
      if (m_groovyObject == null) {
        try {
          m_groovyObject = compileScript(m_groovyScript);
        } catch (Exception ex) {
          return null;
        }
      }
      // delegate to the script
      return ((Step) m_groovyObject).getInteractiveViewersImpls();
    } else {
      return null;
    }
  }

  /**
   * Compile a groovy script
   *
   * @param script the script to compile.
   * @return an new object from the compiled groovy script
   * @throws Exception if there is a problem during compilation
   */
  public static Step compileScript(String script) throws Exception {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        new GroovyStep().getClass().getClassLoader());
      GroovyClassLoader gcl = new GroovyClassLoader();
      Class scriptClass = gcl.parseClass(script);
      return (Step) scriptClass.newInstance();
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }
}
