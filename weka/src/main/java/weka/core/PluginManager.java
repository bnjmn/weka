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
 *    PluginManager.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that manages a global map of plugins. Provides static methods for
 * registering and instantiating plugins. The package manager looks for, and
 * processes, a PluginManager.props file in the top-level of a package.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12312 $
 */
public class PluginManager {

  /**
   * Global map of plugin classes that is keyed by plugin base class/interface
   * type. The inner Map then stores individual plugin instances of the
   * interface type, keyed by plugin name/short title with values the actual
   * fully qualified class name
   */
  protected static Map<String, Map<String, String>> PLUGINS =
    new HashMap<String, Map<String, String>>();

  /**
   * Set of concrete fully qualified class names or abstract/interface base
   * types to "disable". Entries in this list wont ever be returned by any of
   * the getPlugin() methods. Registering an abstract/interface base name will
   * disable all concrete implementations of that type
   */
  protected static Set<String> DISABLED = new HashSet<String>();

  /**
   * Global map of plugin resources (loadable from the classpath). Outer map is
   * keyed by group ID, i.e. an ID of a logical group of resources (e.g.
   * knowledge flow template files). The inner map then stores individual
   * resource paths keyed by their short description.
   */
  protected static Map<String, Map<String, String>> RESOURCES =
    new HashMap<String, Map<String, String>>();

  /**
   * Global lookup map to locate a package owner for resources. Keyed by group
   * ID:resource description. Used to see if a package owns a resource, in which
   * case the package classloader should be used to load the resource rather
   * than the application classloader
   */
  protected static Map<String, String> RESOURCE_OWNER_PACKAGE = new HashMap<>();

  /**
   * Add the supplied list of fully qualified class names to the disabled list
   * 
   * @param classnames a list of class names to add
   */
  public static synchronized void addToDisabledList(List<String> classnames) {
    for (String s : classnames) {
      addToDisabledList(s);
    }
  }

  /**
   * Add the supplied fully qualified class name to the list of disabled plugins
   * 
   * @param classname the fully qualified name of a class to add
   */
  public static synchronized void addToDisabledList(String classname) {
    DISABLED.add(classname);
  }

  /**
   * Remove the supplied list of fully qualified class names to the disabled
   * list
   * 
   * @param classnames a list of class names to remove
   */
  public static synchronized void
    removeFromDisabledList(List<String> classnames) {
    for (String s : classnames) {
      removeFromDisabledList(s);
    }
  }

  /**
   * Remove the supplied fully qualified class name from the list of disabled
   * plugins
   * 
   * @param classname the fully qualified name of a class to remove
   */
  public static synchronized void removeFromDisabledList(String classname) {
    DISABLED.remove(classname);
  }

  /**
   * Returns true if the supplied fully qualified class name is in the disabled
   * list
   * 
   * @param classname the name of the class to check
   * @return true if the supplied class name is in the disabled list
   */
  public static boolean isInDisabledList(String classname) {
    return DISABLED.contains(classname);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param propsFile the properties file to add
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(File propsFile)
    throws Exception {
    addFromProperties(null, propsFile);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param propsFile the properties file to add
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    File propsFile) throws Exception {
    addFromProperties(packageName, propsFile, false);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param propsFile the properties file to add
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(File propsFile,
    boolean maintainInsertionOrder) throws Exception {
    addFromProperties(null, propsFile, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied property file
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param propsFile the properties file to add
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    File propsFile, boolean maintainInsertionOrder) throws Exception {
    BufferedInputStream bi =
      new BufferedInputStream(new FileInputStream(propsFile));
    addFromProperties(packageName, bi, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param propsStream an input stream to a properties file
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(InputStream propsStream)
    throws Exception {
    addFromProperties(null, propsStream);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param propsStream an input stream to a properties file
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    InputStream propsStream) throws Exception {
    addFromProperties(packageName, propsStream, false);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param propsStream an input stream to a properties file
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(InputStream propsStream,
    boolean maintainInsertionOrder) throws Exception {
    addFromProperties(null, propsStream, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param propsStream an input stream to a properties file
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    InputStream propsStream, boolean maintainInsertionOrder) throws Exception {

    Properties expProps = new Properties();

    expProps.load(propsStream);
    propsStream.close();
    propsStream = null;

    addFromProperties(packageName, expProps, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param props a Properties object
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(Properties props)
    throws Exception {
    addFromProperties(props, false);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param props a Properties object
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    Properties props) throws Exception {
    addFromProperties(packageName, props, false);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param props a Properties object
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public synchronized static void addFromProperties(Properties props,
    boolean maintainInsertionOrder) throws Exception {
    addFromProperties(null, props, maintainInsertionOrder);
  }

  /**
   * Add all key value pairs from the supplied properties object
   *
   * @param packageName the name of the Weka package that owns this properties
   *          object. Can be null if not owned by a Weka package
   * @param props a Properties object
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(String packageName,
    Properties props, boolean maintainInsertionOrder) throws Exception {
    java.util.Enumeration<?> keys = props.propertyNames();

    while (keys.hasMoreElements()) {
      String baseType = (String) keys.nextElement();
      String implementations = props.getProperty(baseType);
      if (baseType.equalsIgnoreCase("*resources*")) {
        addPluginResourcesFromProperty(packageName, implementations);
      } else {
        if (implementations != null && implementations.length() > 0) {
          String[] parts = implementations.split(",");
          for (String impl : parts) {
            impl = impl.trim();
            String name = impl;
            if (impl.charAt(0) == '[') {
              name = impl.substring(1, impl.indexOf(']'));
              impl = impl.substring(impl.indexOf(']') + 1);
            }
            PluginManager.addPlugin(baseType, name.trim(), impl,
              maintainInsertionOrder);
          }
        }
      }
    }
  }

  /**
   * Add resources from a list. String format for a list of resources (as might
   * be supplied from a *resources* entry in an property file:<br>
   * <br>
   *
   * <pre>
   * [groupID|description|path],[groupID|description|path],...
   * </pre>
   *
   * @param resourceList a list of resources to add
   */
  public static void addPluginResourcesFromProperty(String resourceList) {
    addPluginResourcesFromProperty(null, resourceList);
  }

  /**
   * Add resources from a list. String format for a list of resources (as might
   * be supplied from a *resources* entry in an property file:<br>
   * <br>
   *
   * <pre>
   * [groupID|description|path],[groupID|description|path],...
   * </pre>
   *
   * @param packageName the Weka package that owns these resources. Can be null
   *          if not owned by a Weka package
   * @param resourceList a list of resources to add
   */
  protected static synchronized void addPluginResourcesFromProperty(
    String packageName, String resourceList) {

    // Format: [groupID|description|path],[...],...
    String[] resources = resourceList.split(",");
    for (String r : resources) {
      r = r.trim();
      if (!r.startsWith("[") || !r.endsWith("]")) {
        System.err.println("[PluginManager] Malformed resource in: "
          + resourceList);
        continue;
      }

      r = r.replace("[", "").replace("]", "");
      String[] rParts = r.split("\\|");
      if (rParts.length != 3) {
        System.err
          .println("[PluginManager] Was expecting 3 pipe separated parts in "
            + "resource spec: " + r);
        continue;
      }

      String groupID = rParts[0].trim();
      String resourceDesc = rParts[1].trim();
      String resourcePath = rParts[2].trim();
      if (groupID.length() == 0 || resourceDesc.length() == 0
        || resourcePath.length() == 0) {
        System.err.println("[PluginManager] Empty part in resource spec: " + r);
        continue;
      }
      addPluginResource(packageName, groupID, resourceDesc, resourcePath);
    }
  }

  /**
   * Add a resource.
   *
   * @param resourceGroupID the ID of the group under which the resource should
   *          be stored
   * @param resourceDescription the description/ID of the resource
   * @param resourcePath the path to the resource
   */
  public static void addPluginResource(String resourceGroupID,
    String resourceDescription, String resourcePath) {
    addPluginResource(null, resourceGroupID, resourceDescription, resourcePath);
  }

  /**
   * Add a resource.
   * 
   * @param packageName the name of the package that owns this resource. Can be
   *          null if not owned by a package, in which case the current
   *          classloader will be used to load the resource.
   * @param resourceGroupID the ID of the group under which the resource should
   *          be stored
   * @param resourceDescription the description/ID of the resource
   * @param resourcePath the path to the resource
   */
  public static synchronized void addPluginResource(String packageName,
    String resourceGroupID, String resourceDescription, String resourcePath) {
    Map<String, String> groupMap = RESOURCES.get(resourceGroupID);
    if (groupMap == null) {
      groupMap = new LinkedHashMap<String, String>();
      RESOURCES.put(resourceGroupID, groupMap);
    }

    groupMap.put(resourceDescription, resourcePath);
    if (packageName != null && packageName.length() > 0) {
      RESOURCE_OWNER_PACKAGE.put(resourceGroupID + ":" + resourceDescription,
        packageName);
    }
  }

  /**
   * Get an input stream for a named resource under a given resource group ID.
   * 
   * @param resourceGroupID the group ID that the resource falls under
   * @param resourceDescription the description/ID of the resource
   * @return an InputStream for the resource
   * @throws IOException if the group ID or resource description/ID are not
   *           known to the PluginManager, or a problem occurs while trying to
   *           open an input stream
   */
  public static InputStream getPluginResourceAsStream(String resourceGroupID,
    String resourceDescription) throws IOException {
    Map<String, String> groupMap = RESOURCES.get(resourceGroupID);
    if (groupMap == null) {
      throw new IOException("Unknown resource group ID: " + resourceGroupID);
    }

    String resourcePath = groupMap.get(resourceDescription);
    if (resourcePath == null) {
      throw new IOException("Unknown resource: " + resourceDescription);
    }

    // owned by a package?
    String ownerPackage =
      RESOURCE_OWNER_PACKAGE.get(resourceGroupID + ":" + resourceDescription);

    if (ownerPackage == null) {
      return PluginManager.class.getClassLoader().getResourceAsStream(
        resourcePath);
    }

    return WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager()
      .getPackageClassLoader(ownerPackage).getResourceAsStream(resourcePath);
  }

  /**
   * Get the number of resources available under a given resource group ID.
   *
   * @param resourceGroupID the group ID of the resources
   * @return the number of resources registered under the supplied group ID
   */
  public static int numResourcesForWithGroupID(String resourceGroupID) {
    Map<String, String> groupMap = RESOURCES.get(resourceGroupID);
    return groupMap == null ? 0 : groupMap.size();
  }

  /**
   * Get a map of resources (description,path) registered under a given resource
   * group ID.
   *
   * @param resourceGroupID the group ID of the resources to get
   * @return a map of resources registered under the supplied group ID, or null
   *         if the resourceGroupID is not known to the plugin manager
   */
  public static Map<String, String> getResourcesWithGroupID(
    String resourceGroupID) {
    return RESOURCES.get(resourceGroupID);
  }

  /**
   * Get a set of names of plugins that implement the supplied interface.
   * 
   * @param interfaceName the fully qualified name of the interface to list
   *          plugins for
   * 
   * @return a set of names of plugins
   */
  public static Set<String> getPluginNamesOfType(String interfaceName) {
    if (PLUGINS.get(interfaceName) != null) {
      Set<String> match = PLUGINS.get(interfaceName).keySet();
      Set<String> result = new LinkedHashSet<String>();
      for (String s : match) {
        String impl = PLUGINS.get(interfaceName).get(s);
        if (!DISABLED.contains(impl)) {
          result.add(s);
        }
      }
      // return PLUGINS.get(interfaceName).keySet();
      return result;
    }

    return null;
  }

  /**
   * Get a sorted list of names of plugins that implement the supplied interface.
   *
   * @param interfaceName the fully qualified name of the interface to list
   *          plugins for
   *
   * @return a set of names of plugins
   */
  public static List<String> getPluginNamesOfTypeList(String interfaceName) {
    List<String> result;
    Set<String> r = getPluginNamesOfType(interfaceName);

    result = new ArrayList<String>();
    if (r != null) {
      result.addAll(r);
    }
    Collections.sort(result, new ClassDiscovery.StringCompare());

    return result;
  }

  /**
   * Add a plugin.
   *
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   *
   * @param name the name/short description of the plugin
   * @param concreteType the fully qualified class name of the actual concrete
   *          implementation
   */
  public static void addPlugin(String interfaceName, String name,
    String concreteType) {
    addPlugin(interfaceName, name, concreteType, false);
  }

  /**
   * Add a plugin.
   *
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   *
   * @param name the name/short description of the plugin
   * @param concreteType the fully qualified class name of the actual concrete
   *          implementation
   * @param maintainInsertionOrder true if the order of insertion of
   *          implementations is to be preserved (rather than sorted order)
   */
  public static void addPlugin(String interfaceName, String name,
    String concreteType, boolean maintainInsertionOrder) {

    if (PLUGINS.get(interfaceName) == null) {
      Map<String, String> pluginsOfInterfaceType =
        maintainInsertionOrder ? new LinkedHashMap<String, String>()
          : new TreeMap<String, String>();
      pluginsOfInterfaceType.put(name, concreteType);
      PLUGINS.put(interfaceName, pluginsOfInterfaceType);
    } else {
      PLUGINS.get(interfaceName).put(name, concreteType);
    }
  }

  /**
   * Remove plugins of a specific type.
   * 
   * @param interfaceName the fully qualified interface name that the plugins to
   *          be remove implement
   * @param names a list of named plugins to remove
   */
  public static void removePlugins(String interfaceName, List<String> names) {
    for (String name : names) {
      removePlugin(interfaceName, name);
    }
  }

  /**
   * Remove a plugin.
   * 
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   * 
   * @param name the name/short description of the plugin
   */
  public static void removePlugin(String interfaceName, String name) {
    if (PLUGINS.get(interfaceName) != null) {
      PLUGINS.get(interfaceName).remove(name);
    }
  }

  /**
   * Checks if a named plugin exists in the map of registered plugins
   *
   * @param interfaceType the fully qualified interface name of the plugin type
   * @param name the name/short description of the plugin to get
   * @return true if the named plugin exists
   */
  public static boolean pluginRegistered(String interfaceType, String name) {
    Map<String, String> pluginsOfInterfaceType = PLUGINS.get(interfaceType);
    return pluginsOfInterfaceType.get(name) != null;
  }

  /**
   * Get an instance of a concrete implementation of a plugin type
   * 
   * @param interfaceType the fully qualified interface name of the plugin type
   * @param name the name/short description of the plugin to get
   * @return the concrete plugin or null if the plugin is disabled
   * @throws Exception if the plugin can't be found or instantiated
   */
  public static Object getPluginInstance(String interfaceType, String name)
    throws Exception {
    if (PLUGINS.get(interfaceType) == null
      || PLUGINS.get(interfaceType).size() == 0) {
      throw new Exception("No plugins of interface type: " + interfaceType
        + " available!!");
    }

    Map<String, String> pluginsOfInterfaceType = PLUGINS.get(interfaceType);
    if (pluginsOfInterfaceType.get(name) == null) {
      throw new Exception("Can't find named plugin '" + name + "' of type '"
        + interfaceType + "'!");
    }

    String concreteImpl = pluginsOfInterfaceType.get(name);
    Object plugin = null;
    if (!DISABLED.contains(concreteImpl)) {
      plugin =
        WekaPackageClassLoaderManager.forName(concreteImpl).newInstance();
    }

    return plugin;
  }
}
