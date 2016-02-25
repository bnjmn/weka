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
 *    TemplateManager.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Utils;
import weka.core.WekaException;
import weka.core.PluginManager;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.JSONFlowLoader;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Manages all things template-related
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class TemplateManager {

  // statically register templates that come with Weka
  static {
    try {
      Properties templateProps =
        Utils.readProperties(KFGUIConsts.TEMPLATE_PROPERTY_FILE);
      PluginManager.addFromProperties(templateProps, true);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "KnowledgeFlow",
        JOptionPane.ERROR_MESSAGE);
      ex.printStackTrace();
    }
  }

  /**
   * Get the total number of KF templates available
   *
   * @return the total number (both builtin and plugin) KF templates available
   */
  public int numTemplates() {
    return numBuiltinTemplates() + numPluginTemplates();
  }

  /**
   * Get the number of builtin KF templates available
   *
   * @return the number of builtin KF templates available
   */
  public int numBuiltinTemplates() {
    return PluginManager
      .numResourcesForWithGroupID(KFGUIConsts.KF_BUILTIN_TEMPLATE_KEY);
  }

  /**
   * Get the number of plugin KF templates available
   *
   * @return the number of plugin KF templates available
   */
  public int numPluginTemplates() {
    return PluginManager
      .numResourcesForWithGroupID(KFGUIConsts.KF_PLUGIN_TEMPLATE_KEY);
  }

  /**
   * Get descriptions for the built-in knowledge flow templates
   *
   * @return descriptions for the built-in templates
   */
  public List<String> getBuiltinTemplateDescriptions() {
    List<String> result = new ArrayList<String>();

    Map<String, String> builtin = PluginManager
      .getResourcesWithGroupID(KFGUIConsts.KF_BUILTIN_TEMPLATE_KEY);
    if (builtin != null) {
      for (String desc : builtin.keySet()) {
        result.add(desc);
      }
    }

    return result;
  }

  /**
   * Get descriptions for plugin knowledge flow templates
   *
   * @return descriptions for plugin templates
   */
  public List<String> getPluginTemplateDescriptions() {
    List<String> result = new ArrayList<String>();

    Map<String, String> plugin =
      PluginManager.getResourcesWithGroupID(KFGUIConsts.KF_PLUGIN_TEMPLATE_KEY);
    if (plugin != null) {
      for (String desc : plugin.keySet()) {
        result.add(desc);
      }
    }

    return result;
  }

  /**
   * Get the flow for the supplied description
   *
   * @param flowDescription the description of the template flow to get
   * @return the template flow
   * @throws WekaException if the template does not exist
   */
  public Flow getTemplateFlow(String flowDescription) throws WekaException {
    Flow result = null;
    try {
      // try builtin first...
      result = getBuiltinTemplateFlow(flowDescription);
    } catch (IOException ex) {
      // ignore
    }

    if (result == null) {
      // now try as a plugin...
      try {
        result = getPluginTemplateFlow(flowDescription);
      } catch (IOException ex) {
        throw new WekaException("The template flow '" + flowDescription + "' "
          + "does not seem to exist as a builtin or plugin template");
      }
    }

    return result;
  }

  /**
   * Get the built-in template flow corresponding to the description
   *
   * @param flowDescription the description of the template flow to get
   * @return the flow
   * @throws IOException if an IO error occurs
   * @throws WekaException if a problem occurs
   */
  public Flow getBuiltinTemplateFlow(String flowDescription)
    throws IOException, WekaException {
    InputStream flowStream = PluginManager.getPluginResourceAsStream(
      KFGUIConsts.KF_BUILTIN_TEMPLATE_KEY, flowDescription);

    JSONFlowLoader loader = new JSONFlowLoader();
    return loader.readFlow(flowStream);
  }

  /**
   * Get the plugin template flow corresponding to the description
   *
   * @param flowDescription the description of the template flow to get
   * @return the flow
   * @throws IOException if an IO error occurs
   * @throws WekaException if a problem occurs
   */
  public Flow getPluginTemplateFlow(String flowDescription)
    throws IOException, WekaException {
    InputStream flowStream = PluginManager.getPluginResourceAsStream(
      KFGUIConsts.KF_PLUGIN_TEMPLATE_KEY, flowDescription);

    JSONFlowLoader loader = new JSONFlowLoader();
    return loader.readFlow(flowStream);
  }
}
