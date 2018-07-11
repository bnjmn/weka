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
 *    WekaPackageClassLoaderManager.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class that manages classloaders from individual Weka plugin packages.
 * Maintains a collection of {@code WekaPackageLibIsolatingClassLoader}s - one
 * for each package. {@code Utils.forName()} and {@code weka.Run} use this
 * classloader to find/instantiate schemes exposed in top-level package jar
 * files. Client code in a package should do the same, unless directly referring
 * to classes in other packages, in which case the other packages should be
 * explicit dependencies. This classloader will not find classes in third-party
 * libraries inside a package's lib directory.<br>
 * <br>
 * Classes are searched for first in the parent classloader and then in the
 * top-level package jar files.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see WekaPackageLibIsolatingClassLoader
 */
public class WekaPackageClassLoaderManager {

  protected static final WekaPackageClassLoaderManager s_singletonLoader =
    new WekaPackageClassLoaderManager();

  /** Map of package classloaders keyed by package name */
  protected Map<String, WekaPackageLibIsolatingClassLoader> m_packageJarClassLoaders =
    new HashMap<>();

  /**
   * Lookup for classloaders keyed by class names from top-level package jar
   * files
   */
  protected Map<String, WekaPackageLibIsolatingClassLoader> m_classBasedClassLoaderLookup =
    new HashMap<>();

  /** Path to the weka.jar file on the classpath */
  protected File m_pathToWekaJarFile;

  private WekaPackageClassLoaderManager() {
  }

  /**
   * Injects the MTJ core classes into the root classloader. This is so that
   * they are visible to the MTJ native library loader (if a MTJ native package
   * is installed).
   */
  protected void injectMTJCoreClasses() {
    if (!WekaPackageClassLoaderManager
      .classExists("com.github.fommil.netlib.ARPACK")) {
      // inject core MTJ classes into the root classloader

      String debugS =
        System.getProperty("weka.core.classloader.debug", "false");
      boolean debug = debugS.equalsIgnoreCase("true");

      InputStream mtjCoreInputStream =
        getClass().getClassLoader().getResourceAsStream("core.jar");
      InputStream arpackAllInputStream =
        getClass().getClassLoader().getResourceAsStream(
          "arpack_combined.jar");
      InputStream mtjInputStream =
        getClass().getClassLoader().getResourceAsStream("mtj.jar");
      if (mtjCoreInputStream != null && arpackAllInputStream != null
        && mtjInputStream != null) {
        if (debug) {
          System.out.println("[WekaPackageClassLoaderManager] injecting "
            + "mtj-related core classes into root classloader");
        }
        try {
          if (debug) {
            System.out
              .println("[WekaPackageClassLoaderManager] Injecting arpack");
          }
          injectAllClassesInFromStream(arpackAllInputStream);
          if (debug) {
            System.out.println("[WekaPackageClassLoaderManager] Injecting mtj "
              + "core");
          }
          injectAllClassesInFromStream(mtjCoreInputStream);
          if (debug) {
            System.out.println("[WekaPackageClassLoaderManager] Injecting mtj");
          }
          injectAllClassesInFromStream(mtjInputStream);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      } else {
        System.out.println("WARNING: core mtj jar files are not available as "
          + "resources to this classloader ("
          + WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager()
            .getClass().getClassLoader() + ")");
      }
    }
  }

  /**
   * Gets the singleton instance of the WekaPackageClassLoaderManager
   * 
   * @return the singleton instance of hte WekaPackageClassLoaderManager
   */
  public static WekaPackageClassLoaderManager
    getWekaPackageClassLoaderManager() {
    return s_singletonLoader;
  }

  /**
   * Return an instantiated instance of the supplied class name. This method
   * will attempt to find a package that owns the named class first, before
   * falling back on the current and then parent class loader. Use this method
   * instead of Class.forName().newInstance().
   * 
   * @param className the name of the class to get an instance of
   * @return an instantiated object
   * @throws Exception if the class cannot be found, or a problem occurs during
   *           instantiation
   */
  public static Object objectForName(String className) throws Exception {
    return forName(className).newInstance();
  }

  /**
   * Return the class object for the supplied class name. This method will
   * attempt to find a package that owns the named class first, before falling
   * back on the current, and then parent, class loader. Use this method instead
   * of Class.forName().
   * 
   * @param className the name of hte class to get an instance of
   * @return a class object
   * @throws ClassNotFoundException if the named class cannot be found.
   */
  public static Class<?> forName(String className)
    throws ClassNotFoundException {

    return forName(className, true);
  }

  /**
   * Return the class object for the supplied class name. This method will
   * attempt to find a package that owns the named class first, before falling
   * back on the current, and then parent, class loader. Use this method instead
   * of Class.forName().
   *
   * @param className the name of hte class to get an instance of
   * @param initialize true if the class should be initialized
   * @return a class object
   * @throws ClassNotFoundException if the named class cannot be found.
   */
  public static Class<?> forName(String className, boolean initialize)
    throws ClassNotFoundException {
    WekaPackageClassLoaderManager cl =
      WekaPackageClassLoaderManager.getWekaPackageClassLoaderManager();
    ClassLoader toUse = cl.getLoaderForClass(className);

    return Class.forName(className, initialize, toUse);
  }

  /**
   * Return the path to the weka.jar file (if found) on the classpath.
   *
   * @return the path to the weka.jar file on the classpath, or null if no
   *         weka.jar file was found.
   */
  public File getPathToWekaJarFile() {
    return m_pathToWekaJarFile;
  }

  /**
   * Get the entries in the Weka class loader (i.e. the class loader that loads
   * the core weka classes) as an array of URLs. This is primarily used by
   * Weka's dynamic class discovery mechanism, so that all Weka schemes on the
   * classpath can be discovered. If the Weka class loader is an instance of
   * URLClassLoader then the URLs encapsulated within are returned. Otherwise,
   * if the system class loader is the classloader that loads Weka then the
   * entries from java.class.path are returned. Failing that, we assume that
   * Weka and any other supporting classes (not including packages) can be found
   * in the WEKA_CLASSPATH environment variable. This latter case can be used to
   * handle the situation where the weka.jar is loaded by a custom
   * (non-URLClassLoader) withn an app server (for example).
   * 
   * @return an array of URLs containing the entries available to the
   *         classloader that loads the core weka classes
   */
  public URL[] getWekaClassloaderClasspathEntries() {
    ClassLoader parent = getClass().getClassLoader();

    // easy case
    if (parent instanceof URLClassLoader) {
      URL[] result = ((URLClassLoader) parent).getURLs();
      // scan for weka.jar
      for (URL u : result) {
        if (u.toString().endsWith("weka.jar")) {
          try {
            m_pathToWekaJarFile = new File(u.toURI());
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }
        }
      }
      return result;
    }

    // otherwise, see if we've been loaded by the system classloader
    if (ClassLoader.getSystemClassLoader().equals(getClass().getClassLoader())) {
      // we can process the java.class.path property for weka core stuff
      return getSystemClasspathEntries();
    } else {
      // have to have the WEKA_CLASSPATH property set
      return getWekaClasspathEntries();
    }
  }

  /**
   * Get a set of all classes contained in all top-level jar files from Weka
   * packages. These classes are globally visible across all packages.
   *
   * @return a set of all classes in all top-level package jar files
   */
  public Set<String> getPackageJarFileClasses() {
    return m_classBasedClassLoaderLookup.keySet();
  }

  /**
   * Returns a list of URLs made up from the entries available in the
   * java.class.path property. This will contain the weka.jar (or directory
   * containing weka classes) if Weka is launched as an application. It won't
   * contain Weka if weka is loaded by a child classloader (in an app server or
   * similar). In this case, if the child classloader is a URLClassLoader then
   * the original class discovery mechanism will work; if not, then the
   * WEKA_CLASSPATH environment variable will need to be set to point to the
   * location of the weka.jar file on the file system.
   *
   * @return an array of URLs containing the entries in the java.class.path
   *         property
   */
  private URL[] getSystemClasspathEntries() {
    String cp = System.getProperty("java.class.path", "");
    String sep = System.getProperty("path.separator", ":");
    return getParts(cp, sep);
  }

  /**
   * Returns entries from the WEKA_CLASSPATH property (if set).
   *
   * @return an array of URLs, one for each part of the WEKA_CLASSPATH
   */
  private URL[] getWekaClasspathEntries() {
    String wekaCp =
      Environment.getSystemWide().getVariableValue("WEKA_CLASSPATH");

    // assume the system separator is being used
    String sep = System.getProperty("path.separator", ":");
    if (wekaCp != null) {
      return getParts(wekaCp, sep);
    }

    return new URL[0];
  }

  /**
   * Splits a supplied string classpath and returns an array of URLs
   * representing the entries.
   *
   * @param cp the classpath to process
   * @param sep the path separator
   * @return an array of URLs
   */
  private URL[] getParts(String cp, String sep) {
    String[] cpParts = cp.split(sep);

    List<URL> uList = new ArrayList<>();
    for (String part : cpParts) {
      try {
        URL url;
        if (part.startsWith("file:")) {
          part = part.replace(" ", "%20");
          url = new URI(part).toURL();
        } else {
          url = new File(part).toURI().toURL();
          uList.add(url);
        }
        if (part.endsWith("weka.jar")) {
          m_pathToWekaJarFile = new File(url.toURI());
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    }
    return uList.toArray(new URL[uList.size()]);
  }

  /**
   * Removes the named package classloader from those managed by this class.
   * Attempts to close the classloader and remove any file locks (under Windows)
   * that it might be holding
   *
   * @param packageName the name of the package to remove the classloader for
   */
  public synchronized void removeClassLoaderForPackage(String packageName) {
    WekaPackageLibIsolatingClassLoader loader =
      m_packageJarClassLoaders.get(packageName);
    if (loader != null) {
      loader.closeClassLoader();

      m_packageJarClassLoaders.remove(packageName);
    }
  }

  /**
   * Create a class loader for the given package directory
   *
   * @param packageDir the directory of a Weka package to create a class loader
   *          for
   * @preturn the newly created class loader
   * @throws Exception if a problem occurs
   */
  public synchronized ClassLoader addPackageToClassLoader(File packageDir)
    throws Exception {

    if (m_packageJarClassLoaders.containsKey(packageDir.getName())) {
      m_packageJarClassLoaders.get(packageDir.getName()).closeClassLoader();
    }

    WekaPackageLibIsolatingClassLoader packageLoader =
      new WekaPackageLibIsolatingClassLoader(this, packageDir);
    m_packageJarClassLoaders.put(packageDir.getName(), packageLoader);
    Set<String> classes = packageLoader.getPackageJarEntries();
    for (String c : classes) {
      m_classBasedClassLoaderLookup.put(c, packageLoader);
    }
    return packageLoader;
  }

  /**
   * Attempts to locate a classloader for the named class. Tries the Weka
   * classloader and globally visible package classes first. Note that this also
   * finds classes that are contained in a package's lib directory. General code
   * should not be looking directly for such third-party classes. Instead, if
   * they require third-party classes they should reference them directly (and
   * compile against the libraries in question), and then either include the
   * third party libraries in their own lib directory or declare a dependency on
   * the package that contains them.
   * 
   * This method is used by Weka's deserialization routines (in
   * SerializationHelper and SerializedObject) that need to locate classes
   * (including third-party library ones) that might have been serialized when
   * saving a learning scheme.
   *
   * @param className the name of the class to locate a classloader for
   * @return a classloader
   */
  public ClassLoader getLoaderForClass(String className) {
    className = className.replace("[L", "").replace("[", "").replace(";", "");

    // try the Weka classloader and globally visible package classes first
    ClassLoader result = getClass().getClassLoader();
    try {
      Class<?> cl = findClass(className);
      return cl.getClassLoader();
    } catch (Exception ex) {
    }

    result = m_classBasedClassLoaderLookup.get(className);
    if (result == null) {
      result = getClass().getClassLoader();
    }
    return result;
  }

  /**
   * Get the classloader for the named package
   * 
   * @param packageName the name of the package to get the classloader for
   * @return the package's classloader, or null if the package is not known (or
   *         perhaps was not loaded for some reason)
   */
  public WekaPackageLibIsolatingClassLoader getPackageClassLoader(
    String packageName) {
    return m_packageJarClassLoaders.get(packageName);
  }

  /**
   * Attempts to find the named class. Tries the Weka classloader first (for
   * core Weka classes and general Java classes) and then tries package
   * classloaders (with respect to the globally visible classes contained in
   * their top-level jar files). Note that this method will not find classes
   * contained in third-party libraries that reside in lib directories).
   * 
   * @param name the name of the class to find
   * @return the class object
   * @throws ClassNotFoundException if the named class cannot be found
   */
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    Class<?> result = null;

    // try the Weka classloader first (for general java stuff and core Weka
    // stuff)
    try {
      // result = super.findClass(name);
      result = getClass().getClassLoader().loadClass(name);
    } catch (ClassNotFoundException e) {
      // ignore
    }

    if (result == null) {
      // now ask the package top-level classloaders
      for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_packageJarClassLoaders
        .entrySet()) {
        result = e.getValue().findGloballyVisiblePackageClass(name);
        if (result != null) {
          break;
        }
      }
    }

    if (result == null) {
      throw new ClassNotFoundException("Unable to find class '" + name + "'");
    }

    return result;
  }

  /**
   * Find a named resource. This searches the Weka classloader and all package
   * classloaders. Note that it will only find resources contained in a
   * package's top-level jar file(s).
   * 
   * @param name the name of the resource to find
   * @return a URL to the resource, or null if the resource could not be located
   */
  public URL findResource(String name) {
    URL result = null;

    // TODO might want to allow package resources to override parent classpath
    // ones?
    // try the parent classloader first (for general java stuff and core Weka)
    // result = super.findResource(name);
    result = getClass().getClassLoader().getResource(name);

    if (result == null) {
      // now ask the package top-level classloaders
      for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_packageJarClassLoaders
        .entrySet()) {
        result = e.getValue().findGloballyVisiblePackageResource(name);

        if (result != null) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get the classloader that covers the jar that contains the named resource.
   * Note that this considers the Weka classloader and package classloaders
   * (with respect to resources contained in their top-level jar file(s))
   * 
   * @param name the name of the resource to get the owning classloader for
   * @return the classloader that "owns" the resource
   */
  public ClassLoader findClassloaderForResource(String name) {
    ClassLoader result = null;

    if (getClass().getClassLoader().getResource(name) != null) {
      result = getClass().getClassLoader();
    } else {
      // now ask the package top-level classloaders
      for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_packageJarClassLoaders
        .entrySet()) {
        if (e.getValue().findGloballyVisiblePackageResource(name) != null) {
          result = e.getValue();
        }
      }
    }

    return result;
  }

  /**
   * Find a named resource. This searches the Weka classloader and all package
   * classloaders. Note that it will only find resources contained in a
   * package's top-level jar file(s).
   *
   * @param name the name of the resource to find
   * @return an enumeration of URLs to the resource, or null if the resource
   *         could not be located
   */
  public Enumeration<URL> findResources(String name) throws IOException {
    Enumeration<URL> result = null;

    // TODO might want to allow package resources to override parent classpath
    // ones?
    try {
      // result = super.findResources(name);
      result = getClass().getClassLoader().getResources(name);
    } catch (IOException ex) {
      // just ignore
    }

    if (result == null) {
      for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_packageJarClassLoaders
        .entrySet()) {
        try {
          result = e.getValue().findGloballyVisiblePackageResources(name);
          if (result != null) {
            break;
          }
        } catch (IOException ex) {
          // ignore
        }
      }
    }

    return result;
  }

  /**
   * Try to find a class from the classloader for the named package. Note that
   * this will traverse transitive package dependencies
   *
   * @param packageName the name of the package to check for the class
   * @param className the name of the class to search for
   * @return the named class or null if the class could not be found
   */
  protected Class<?> findClass(String packageName, String className) {
    Class<?> result = null;

    WekaPackageLibIsolatingClassLoader toTry =
      getPackageClassLoader(packageName);

    if (toTry != null) {
      try {
        // System.err.println("Looking for " + className + " in classloader: " +
        // toTry.toString());
        result = toTry.findClass(className);
      } catch (ClassNotFoundException ex) {
        // ignore here
      }
    }

    return result;
  }

  /**
   * Try to find a resource from the classloader for the named package. Note
   * that this will traverse transitive package dependencies
   * 
   * @param packageName the name of the package to check for the resource
   * @param name the name of the resource to search for
   * @return a URL to the resource, or null if the resource could not be found
   */
  protected URL findResource(String packageName, String name) {
    URL result = null;
    WekaPackageLibIsolatingClassLoader toTry =
      getPackageClassLoader(packageName);

    if (toTry != null) {
      result = toTry.getResource(name);
    }

    return result;
  }

  /**
   * Try to find a resource from the classloader for the named package. Note
   * that this will traverse transitive package dependencies
   *
   * @param packageName the name of the package to check for the resource
   * @param name the name of the resource to search for
   * @return an enumeration of URLs to the resource, or null if the resource
   *         could not be found
   */
  protected Enumeration<URL> findResources(String packageName, String name) {
    Enumeration<URL> result = null;
    WekaPackageLibIsolatingClassLoader toTry =
      getPackageClassLoader(packageName);

    if (toTry != null) {
      try {
        result = toTry.getResources(name);
      } catch (IOException ex) {
        // ignore here
      }
    }

    return result;
  }

  /**
   * Check to see if the named class exists in this classloader
   *
   * @param className the name of the class to check for
   * @return true if the class exists in this classloader
   */
  protected static boolean classExists(String className) {
    boolean result = false;

    try {
      Class<?> cls = Class.forName(className);
      result = true;
    } catch (ClassNotFoundException e) {
      // ignore - means class is not visible/available here
    } catch (NoClassDefFoundError e2) {
      // ignore - means class is not visible/available here
    }

    return result;
  }

  /**
   * Inject all classes in the supplied jar file into the parent or root
   * classloader
   * 
   * @param jarPath the path to the jar in question
   * @param injectToRootClassLoader true to inject right up to the root
   * @throws Exception if a problem occurs
   */
  protected static void injectAllClassesInJar(File jarPath,
    boolean injectToRootClassLoader) throws Exception {
    injectClasses(jarPath, null, null, injectToRootClassLoader);
  }

  /**
   * Inject all classes in the supplied jar file into the root classloader
   * 
   * @param jarPath the path to the jar in question
   * @throws Exception if a problem occurs
   */
  protected static void injectAllClassesInJar(File jarPath) throws Exception {
    injectAllClassesInJar(jarPath, true);
  }

  /**
   * Inject all classes from the supplied input stream into the root
   * classloader. Assumes that the zip entries can be read from the input stream
   * (i.e. a jar/zip file is the target)
   * 
   * @param inStream the input stream to process
   * @throws Exception if a problem occurs
   */
  protected static void injectAllClassesInFromStream(InputStream inStream)
    throws Exception {
    injectClasses(new BufferedInputStream(inStream), null, null, true);
  }

  /**
   * Inject classes from the supplied jar file into the Weka or root
   * classloader.
   * 
   * @param jarPath the path to the jar file to process
   * @param classJarPaths an optional list of paths to classes in the jar file
   *          to inject (if null then all classes in the jar file are injected)
   * @param classes an optional list of fully qualified class names. This should
   *          correspond to the paths in classJarPaths, but contain just the
   *          class name along with slashes replaced by "." and the .class
   *          extension removed.
   * @param injectToRootClassLoader true if the classes are to be injected into
   *          the root classloader rather than the Weka classloader
   * @throws Exception if a problem occurs
   */
  protected static void injectClasses(File jarPath, List<String> classJarPaths,
    List<String> classes, boolean injectToRootClassLoader) throws Exception {

    if (!jarPath.exists()) {
      System.err.println("Path for jar file to inject '" + jarPath.toString()
        + "' does not seem to exist - skipping");
      return;
    }

    InputStream inStream = new FileInputStream(jarPath);
    injectClasses(inStream, classJarPaths, classes, injectToRootClassLoader);
  }

  /**
   * Inject classes from the supplied stream into the Weka or root classloader.
   *
   * @param jarStream input stream to process. Is expected that jar/zip entries
   *          can be read from the stream
   * @param classJarPaths an optional list of paths to classes in the jar file
   *          to inject (if null then all classes in the jar file are injected)
   * @param classes an optional list of fully qualified class names. This should
   *          correspond to the paths in classJarPaths, but contain just the
   *          class name along with slashes replaced by "." and the .class
   *          extension removed.
   * @param injectToRootClassLoader true if the classes are to be injected into
   *          the root classloader rather than the Weka classloader
   * @throws Exception if a problem occurs
   */
  protected static void injectClasses(InputStream jarStream,
    List<String> classJarPaths, List<String> classes,
    boolean injectToRootClassLoader) throws Exception {
    String debugS = System.getProperty("weka.core.classloader.debug", "false");
    boolean debug = debugS.equalsIgnoreCase("true");

    boolean processAllClasses = classes == null || classJarPaths == null;
    if (processAllClasses) {
      classes = new ArrayList<>();
      classJarPaths = new ArrayList<>();
    }
    List<byte[]> preloadClassByteCode = new ArrayList<>();

    ZipInputStream zi = new ZipInputStream(jarStream);
    ZipEntry zipEntry = null;
    while ((zipEntry = zi.getNextEntry()) != null) {
      if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".class")) {
        String zipPart = zipEntry.getName().replace("\\", "/");
        if (classJarPaths.contains(zipPart) || processAllClasses) {
          preloadClassByteCode.add(getByteCode(zi, false));
          zi.closeEntry(); // move to next entry in zip
          if (processAllClasses) {
            classes.add(zipEntry.getName().replace(".class", "")
              .replace("\\", "/").replace("/", "."));
          }
        }
      }
    }

    zi.close();

    List<byte[]> okBytes = new ArrayList<>();
    List<String> okClasses = new ArrayList<>();
    for (int i = 0; i < classes.size(); i++) {
      if (!classExists(classes.get(i))) {
        okClasses.add(classes.get(i));
        okBytes.add(preloadClassByteCode.get(i));
      }
    }
    preloadClassByteCode = okBytes;
    classes = okClasses;

    if (preloadClassByteCode.size() > 0) {
      ClassLoader rootClassloader =
        injectToRootClassLoader ? getRootClassLoader()
          : getWekaLevelClassloader();

      Class<?> classLoader = Class.forName("java.lang.ClassLoader");
      Method defineClass =
        classLoader.getDeclaredMethod("defineClass", String.class,
          byte[].class, int.class, int.class, ProtectionDomain.class);

      ProtectionDomain pd = System.class.getProtectionDomain();

      // ClassLoader.defineClass is a protected method, so we have to make it
      // accessible
      defineClass.setAccessible(true);
      List<byte[]> failedToInject = new ArrayList<>();
      List<String> classesF = new ArrayList<>();
      boolean cont = true;
      int numLeft = classes.size();
      try {
        do {
          if (debug) {
            System.out
              .println("[WekaPackageClassLoaderManager] Injecting classes "
                + "into the "
                + (injectToRootClassLoader ? "root classloader..."
                  : "weka-level classloader..."));
          }
          for (int i = 0; i < classes.size(); i++) {
            if (debug) {
              System.out.println("** Injecting " + classes.get(i));
            }
            byte[] b = preloadClassByteCode.get(i);
            try {
              defineClass.invoke(rootClassloader, classes.get(i), b, 0,
                b.length, pd);
            } catch (Exception ex) {
              failedToInject.add(b);
              classesF.add(classes.get(i));
            }
          }

          cont = failedToInject.size() < numLeft;
          preloadClassByteCode = failedToInject;
          classes = classesF;
          numLeft = failedToInject.size();
          failedToInject = new ArrayList<>();
          classesF = new ArrayList<>();
        } while (classes.size() > 0 && cont);
      } finally {
        defineClass.setAccessible(false);
      }
    }
  }

  private static byte[] getByteCode(InputStream in) throws IOException {
    return getByteCode(in, true);
  }

  private static byte[] getByteCode(InputStream in, boolean closeInput)
    throws IOException {
    byte[] buf = new byte[1024];
    ByteArrayOutputStream byteCodeBuf = new ByteArrayOutputStream();

    for (int readLength; (readLength = in.read(buf)) != -1;) {
      byteCodeBuf.write(buf, 0, readLength);
    }

    if (closeInput) {
      in.close();
    }

    return byteCodeBuf.toByteArray();
  }

  private static ClassLoader getRootClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    while (cl.getParent() != null) {
      // System.err.println("Getting parent classloader....");
      cl = cl.getParent();
    }
    return cl;
  }

  private static ClassLoader getWekaLevelClassloader() {
    return weka.core.Version.class.getClassLoader();
  }

  /**
   * Checks each classloader to make sure that their dependencies are available
   * (i.e. a classloader for each dependency is present). This method is called
   * by WekaPackageManager after loading all packages, but before dynamic class
   * discovery is invoked. This takes care of any issues that might arise from
   * the user toggling the load status of a package, or perhaps manually
   * installing packages that preclude one another, which might make one or more
   * dependencies unavailable
   */
  protected void performIntegrityCheck() {

    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_packageJarClassLoaders
      .entrySet()) {
      String packageName = e.getKey();
      WekaPackageLibIsolatingClassLoader child = e.getValue();

      try {
        if (!child.integrityCheck()) {
          problems.add(packageName);
        }
      } catch (Exception ex) {
        problems.add(packageName);
      }
    }

    List<String> classKeys = new ArrayList<>();
    for (String p : problems) {
      System.err.println("[Weka] Integrity: removing classloader for: " + p);
      // remove from lookups
      m_packageJarClassLoaders.remove(p);

      for (Map.Entry<String, WekaPackageLibIsolatingClassLoader> e : m_classBasedClassLoaderLookup
        .entrySet()) {
        if (e.getValue().getPackageName().equals(p)) {
          classKeys.add(e.getKey());
        }
      }
      for (String k : classKeys) {
        m_classBasedClassLoaderLookup.remove(k);
      }
    }
  }
}
