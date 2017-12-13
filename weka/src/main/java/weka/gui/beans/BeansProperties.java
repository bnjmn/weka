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
 *    BeansProperties.java
 *    Copyright (C) 2009-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

import weka.core.PluginManager;
import weka.core.Utils;
import weka.core.WekaPackageClassLoaderManager;
import weka.core.metastore.MetaStore;
import weka.core.metastore.XMLFileBasedMetaStore;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Utility class encapsulating various properties for the KnowledgeFlow and
 * providing methods to register and deregister plugin Bean components
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class BeansProperties implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 7183413615090695785L;

  /**
   * Location of the property file for the KnowledgeFlowApp
   */
  protected static final String PROPERTY_FILE = "weka/gui/beans/Beans.props";

  /** Location of the property file listing available templates */
  protected static final String TEMPLATE_PROPERTY_FILE =
    "weka/gui/beans/templates/templates.props";

  /** The paths to template resources */
  protected static List<String> TEMPLATE_PATHS;
  /** Short descriptions for templates suitable for displaying in a menu */
  protected static List<String> TEMPLATE_DESCRIPTIONS;

  /** Contains the editor properties */
  protected static Properties BEAN_PROPERTIES;

  /** Contains the plugin components properties */
  protected static ArrayList<Properties> BEAN_PLUGINS_PROPERTIES =
    new ArrayList<Properties>();

  /**
   * Contains the user's selection of available perspectives to be visible in
   * the perspectives toolbar
   */
  protected static String VISIBLE_PERSPECTIVES_PROPERTIES_FILE =
    "weka/gui/beans/VisiblePerspectives.props";

  /** Those perspectives that the user has opted to have visible in the toolbar */
  protected static SortedSet<String> VISIBLE_PERSPECTIVES;

  private static boolean s_pluginManagerIntialized = false;

  /** Default metastore for configurations etc. */
  protected static MetaStore s_kfMetaStore = new XMLFileBasedMetaStore();

  /**
   * Get the metastore
   * 
   * @return the metastore
   */
  public static MetaStore getMetaStore() {
    return s_kfMetaStore;
  }

  public static void addToPluginBeanProps(File beanPropsFile) throws Exception {
    Properties tempP = new Properties();

    tempP.load(new FileInputStream(beanPropsFile));
    if (!BEAN_PLUGINS_PROPERTIES.contains(tempP)) {
      BEAN_PLUGINS_PROPERTIES.add(tempP);
    }
  }

  public static void removeFromPluginBeanProps(File beanPropsFile)
    throws Exception {
    Properties tempP = new Properties();

    FileInputStream fis = new FileInputStream(beanPropsFile);
    try {
      tempP.load(fis);
      if (BEAN_PLUGINS_PROPERTIES.contains(tempP)) {
        BEAN_PLUGINS_PROPERTIES.remove(tempP);
      }
    } finally {
      fis.close();
    }
  }

  /**
   * Loads KnowledgeFlow properties and any plugins (adds jars to the classpath)
   */
  public static synchronized void loadProperties() {
    if (BEAN_PROPERTIES == null) {
      weka.core.WekaPackageManager.loadPackages(false);
      weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
        "[KnowledgeFlow] Loading properties and plugins...");
      /** Loads the configuration property file */
      // static {
      // Allow a properties file in the current directory to override
      try {
        BEAN_PROPERTIES = Utils.readProperties(PROPERTY_FILE);
        java.util.Enumeration<?> keys = BEAN_PROPERTIES.propertyNames();
        if (!keys.hasMoreElements()) {
          throw new Exception(
            "Could not read a configuration file for the bean\n"
              + "panel. An example file is included with the Weka distribution.\n"
              + "This file should be named \"" + PROPERTY_FILE + "\" and\n"
              + "should be placed either in your user home (which is set\n"
              + "to \"" + System.getProperties().getProperty("user.home")
              + "\")\n" + "or the directory that java was started from\n");
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "KnowledgeFlow",
          JOptionPane.ERROR_MESSAGE);
      }

      if (VISIBLE_PERSPECTIVES == null) {
        // set up built-in perspectives
        Properties pp = new Properties();
        pp.setProperty("weka.gui.beans.KnowledgeFlow.Perspectives",
          "weka.gui.beans.ScatterPlotMatrix,weka.gui.beans.AttributeSummarizer,"
            + "weka.gui.beans.SQLViewerPerspective");
        BEAN_PLUGINS_PROPERTIES.add(pp);

        VISIBLE_PERSPECTIVES = new TreeSet<String>();
        try {

          Properties visible =
            Utils.readProperties(VISIBLE_PERSPECTIVES_PROPERTIES_FILE);
          Enumeration<?> keys = visible.propertyNames();
          if (keys.hasMoreElements()) {
            String listedPerspectives =
              visible
                .getProperty("weka.gui.beans.KnowledgeFlow.SelectedPerspectives");
            if (listedPerspectives != null && listedPerspectives.length() > 0) {
              // split up the list of user selected perspectives and populate
              // VISIBLE_PERSPECTIVES
              StringTokenizer st =
                new StringTokenizer(listedPerspectives, ", ");

              while (st.hasMoreTokens()) {
                String perspectiveName = st.nextToken().trim();
                weka.core.logging.Logger.log(
                  weka.core.logging.Logger.Level.INFO, "Adding perspective "
                    + perspectiveName + " to visible list");
                VISIBLE_PERSPECTIVES.add(perspectiveName);
              }
            }
          }
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(null, ex.getMessage(), "KnowledgeFlow",
            JOptionPane.ERROR_MESSAGE);
        }
      }

    }

    // System templates
    if (TEMPLATE_PATHS == null) {
      TEMPLATE_PATHS = new ArrayList<String>();
      TEMPLATE_DESCRIPTIONS = new ArrayList<String>();

      try {
        Properties templateProps = Utils.readProperties(TEMPLATE_PROPERTY_FILE);
        String paths =
          templateProps.getProperty("weka.gui.beans.KnowledgeFlow.templates");
        String descriptions =
          templateProps
            .getProperty("weka.gui.beans.KnowledgeFlow.templates.desc");
        if (paths == null || paths.length() == 0) {
          weka.core.logging.Logger.log(weka.core.logging.Logger.Level.WARNING,
            "[KnowledgeFlow] WARNING: no templates found in classpath");
        } else {
          String[] templates = paths.split(",");
          String[] desc = descriptions.split(",");
          if (templates.length != desc.length) {
            throw new Exception("Number of template descriptions does "
              + "not match number of templates.");
          }
          for (String template : templates) {
            TEMPLATE_PATHS.add(template.trim());
          }
          for (String d : desc) {
            TEMPLATE_DESCRIPTIONS.add(d.trim());
          }
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "KnowledgeFlow",
          JOptionPane.ERROR_MESSAGE);
      }
    }

    if (!s_pluginManagerIntialized && BEAN_PLUGINS_PROPERTIES != null
      && BEAN_PLUGINS_PROPERTIES.size() > 0) {
      for (int i = 0; i < BEAN_PLUGINS_PROPERTIES.size(); i++) {
        Properties tempP = BEAN_PLUGINS_PROPERTIES.get(i);
        // Check for OffScreenChartRenderers
        String offscreenRenderers =
          tempP.getProperty("weka.gui.beans.OffscreenChartRenderer");
        if (offscreenRenderers != null && offscreenRenderers.length() > 0) {
          String[] parts = offscreenRenderers.split(",");
          for (String renderer : parts) {
            renderer = renderer.trim();
            // Check that we can instantiate it successfully
            try {
              Object p = WekaPackageClassLoaderManager.objectForName(renderer);
              // Class.forName(renderer).newInstance();
              if (p instanceof OffscreenChartRenderer) {
                String name = ((OffscreenChartRenderer) p).rendererName();
                PluginManager.addPlugin(
                  "weka.gui.beans.OffscreenChartRenderer", name, renderer);
                weka.core.logging.Logger.log(
                  weka.core.logging.Logger.Level.INFO,
                  "[KnowledgeFlow] registering chart rendering " + "plugin: "
                    + renderer);
              }
            } catch (Exception ex) {

              weka.core.logging.Logger
                .log(weka.core.logging.Logger.Level.WARNING,
                  "[KnowledgeFlow] WARNING: "
                    + "unable to instantiate chart renderer \"" + renderer
                    + "\"");
              ex.printStackTrace();

            }
          }
        }

        // Check for user templates
        String templatePaths =
          tempP.getProperty("weka.gui.beans.KnowledgeFlow.templates");
        String templateDesc =
          tempP.getProperty("weka.gui.beans.KnowledgeFlow.templates.desc");
        if (templatePaths != null && templatePaths.length() > 0
          && templateDesc != null && templateDesc.length() > 0) {
          String[] templates = templatePaths.split(",");
          String[] desc = templateDesc.split(",");
          // quietly ignore any user-templates that are not consistent
          if (templates.length == desc.length) {

            for (int kk = 0; kk < templates.length; kk++) {
              String pth = templates[kk];
              String d = desc[kk];
              if (!TEMPLATE_PATHS.contains(pth)) {
                TEMPLATE_PATHS.add(pth.trim());
                TEMPLATE_DESCRIPTIONS.add(d.trim());
              }
            }
          }
        }
      }
      s_pluginManagerIntialized = true;
    }
  }
}
