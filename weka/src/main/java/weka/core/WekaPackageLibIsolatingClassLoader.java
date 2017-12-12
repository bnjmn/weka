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
 *    WekaPackageLibIsolatingClassLoader.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import weka.core.packageManagement.Dependency;
import weka.core.packageManagement.Package;
import weka.core.packageManagement.PackageConstraint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static weka.core.WekaPackageManager.DO_NOT_LOAD_IF_CLASS_NOT_PRESENT_KEY;
import static weka.core.WekaPackageManager.DO_NOT_LOAD_IF_ENV_VAR_NOT_SET_KEY;
import static weka.core.WekaPackageManager.DO_NOT_LOAD_IF_ENV_VAR_NOT_SET_MESSAGE_KEY;
import static weka.core.WekaPackageManager.DO_NOT_LOAD_IF_FILE_NOT_PRESENT_KEY;
import static weka.core.WekaPackageManager.DO_NOT_LOAD_IF_FILE_NOT_PRESENT_MESSAGE_KEY;

/**
 * <p>
 * A ClassLoader that loads/finds classes from one Weka plugin package. This
 * includes the top-level jar file(s) and third-party libraries in the package's
 * lib directory. First checks the parent classloader (typically application
 * classloader) - covers general stuff and weka core classes. Next tries the
 * package jar files and third-party libs covered by this classloader/package.
 * Next tries packages this one depends on and their third-party libs - this is
 * transitive. Finally tries all top-level package jar files over all packages.
 * </p>
 * 
 * <p>
 * The basic assumption for Weka packages is that classes present in top-level
 * jar package jar files contain Weka-related code (schemes, filters, GUI
 * panels, tools, etc.), that is visible to all other packages via Weka's
 * dynamic class discovery mechanism. A top-level jar file should not contain
 * any third-party library code. If package A needs to compile against (and
 * explicitly reference) classes provided by package B (either top-level jar or
 * third-party library), then it should declare a dependency on package B.
 * </p>
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class WekaPackageLibIsolatingClassLoader extends URLClassLoader {

  /** The name of the package that this classloader loads classes from */
  protected String m_packageName = "";

  /** A reference to the classloader manager */
  protected WekaPackageClassLoaderManager m_classloaderRepo;

  /** Classes in the top-level jar file(s) */
  protected Set<String> m_packageJarEntries = new HashSet<>();

  /** Resources in the top-level jar file(s) */
  protected Set<String> m_packageJarResources = new HashSet<>();

  /** Classes in the lib jar file(s) */
  protected Set<String> m_libJarEntries = new HashSet<>();

  /** True to output debugging info */
  protected boolean m_debug;

  /** Which packages this one depends on */
  protected Set<String> m_packageDependencies = new HashSet<>();

  /**
   * Constructor
   * 
   * @param repo a reference to the classloader manager
   * @param packageDir the package directory for the package covered by this
   *          classloader
   * @throws Exception if a problem occurs
   */
  public WekaPackageLibIsolatingClassLoader(WekaPackageClassLoaderManager repo,
    File packageDir) throws Exception {
    // we don't allow any URLs prior to processing the package
    super(new URL[0]);
    String debug = System.getProperty("weka.core.classloader.debug", "false");
    m_debug = debug.equalsIgnoreCase("true");
    m_classloaderRepo = repo;
    init(packageDir);
  }

  /**
   * Initializes the classloader from the package directory. Checks for, and
   * processes, any native libraries and loaders specified in the
   * Description.props file; Assembles a list of packages that this one depends
   * on; processes top-level jar files and third party libraries in the lib
   * directory.
   * 
   * @param packageDir the home directory for the package
   * @throws Exception if a problem occurs
   */
  protected void init(File packageDir) throws Exception {
    m_packageName = packageDir.getName();
    Package toLoad = WekaPackageManager.getInstalledPackageInfo(m_packageName);

    // Process any native libs before anything else!
    List<String> jarsToBeIgnoredWhenLoadingClasses =
      checkForNativeLibs(toLoad, packageDir);

    List<Dependency> deps = toLoad.getDependencies();
    for (Dependency d : deps) {
      PackageConstraint target = d.getTarget();
      m_packageDependencies.add(target.getPackage().getName());
    }

    if (m_debug) {
      System.out.println("WekaPackageLibIsolatingClassLoader for: "
        + m_packageName);
      System.out.print("\tDependencies:");
      for (String dep : m_packageDependencies) {
        System.out.print(" " + dep);
      }
      System.out.println();
    }

    processDir(packageDir, jarsToBeIgnoredWhenLoadingClasses, true);

    if (m_debug) {
      System.out.println("\nPackage jar(s) classes:");
      for (String c : m_packageJarEntries) {
        System.out.println("\t" + c);
      }
      System.out.println("\nPackage jar(s) resources:");
      for (String r : m_packageJarResources) {
        System.out.println("\t" + r);
      }
      System.out.println("\nLib jar(s) classes:");
      for (String c : m_libJarEntries) {
        System.out.println("\t" + c);
      }
    }
  }

  /**
   * Add a package dependency to this classloader
   *
   * @param packageName the name of the package to add as a dependency
   */
  protected void addPackageDependency(String packageName) {
    m_packageDependencies.add(packageName);
  }

  /**
   * Gets a list of class loaders for the packages that this one depends on
   *
   * @return a list of class loaders for the packages that this one depends on
   */
  public List<WekaPackageLibIsolatingClassLoader> getPackageClassLoadersForDependencies() {
    List<WekaPackageLibIsolatingClassLoader> result = new ArrayList<>();
    for (String d : m_packageDependencies) {
      result.add(m_classloaderRepo.getPackageClassLoader(d));
    }

    return result;
  }

  /**
   * Checks for native libraries and any native library loader classes, as
   * specified by the presence of "NativeLibs" and "InjectLoader" entries in the
   * package's Description.props file respectively. Native libraries are copied
   * to $WEKA_HOME/native (if not already present therein). Classes specified by
   * "InjectLoader" are injected into the root classloader.
   *
   * @param toLoad the package object for the this package
   * @param packageDir the home directory of the package
   * @return a list of jar files that should be ignored by this classloader when
   *         loading classes (i.e. all jar files specified in the "InjectLoader"
   *         entry)
   */
  protected List<String> checkForNativeLibs(Package toLoad, File packageDir) {
    List<String> jarsForClassloaderToIgnore = new ArrayList<>();

    if (toLoad.getPackageMetaDataElement("NativeLibs") != null) {
      String nativeLibs =
        toLoad.getPackageMetaDataElement("NativeLibs").toString();
      if (nativeLibs.length() > 0) {
        String[] jarsWithLibs = nativeLibs.split(";");
        for (String entry : jarsWithLibs) {
          String[] jarAndEntries = entry.split(":");
          if (jarAndEntries.length != 2) {
            System.err
              .println("Was expecting two entries for native lib spec - "
                + "jar:comma-separated lib paths");
            continue;
          }
          String jarPath = jarAndEntries[0].trim();
          String[] libPathsInJar = jarAndEntries[1].split(",");
          List<String> libsToInstall = new ArrayList<>();
          // look at named libs and check if they are already in
          // $WEKA_HOME/native-libs - don't extract a second time, but DO
          // add entries to java.library.path
          for (String lib : libPathsInJar) {
            String libName = lib.trim().replace("\\", "/");
            if (!nativeLibInstalled(libName.substring(
              libName.lastIndexOf("/") + 1, libName.length()))) {
              libsToInstall.add(libName.substring(libName.lastIndexOf("/") + 1,
                libName.length()));
            }
          }

          if (libsToInstall.size() > 0) {
            try {
              installNativeLibs(packageDir, jarPath, libsToInstall);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          /*
           * if (libsToAddToPath.size() > 0) {
           * addNativeLibsToLibsProp(libsToAddToPath); }
           */
        }
      }
    }

    // now check to see if there is a native loader to inject into the
    // root class loader
    if (toLoad.getPackageMetaDataElement("InjectLoader") != null) {
      String injectDetails =
        toLoad.getPackageMetaDataElement("InjectLoader").toString();
      String[] entries = injectDetails.split(";");

      for (String entry : entries) {
        String jarPath = entry.trim();
        boolean rootClassLoader = false;
        if (jarPath.startsWith("root|")) {
          jarPath = jarPath.replace("root|", "");
          rootClassLoader = true;
        }
        String ignoreJar = jarPath.replace("\\", "/");
        ignoreJar = ignoreJar.substring(ignoreJar.lastIndexOf("/") + 1);

        jarsForClassloaderToIgnore.add(ignoreJar);
        try {
          WekaPackageClassLoaderManager.injectAllClassesInJar(new File(
            packageDir.toString() + File.separator + jarPath.trim()));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    return jarsForClassloaderToIgnore;
  }

  private static byte[] getByteCode(InputStream in) throws IOException {
    byte[] buf = new byte[1024];
    ByteArrayOutputStream byteCodeBuf = new ByteArrayOutputStream();

    for (int readLength; (readLength = in.read(buf)) != -1;) {
      byteCodeBuf.write(buf, 0, readLength);
    }
    in.close();

    return byteCodeBuf.toByteArray();
  }

  /**
   * Installs any native libraries in the specified jar to $WEKA_HOME/native, if
   * not already present therein.
   * 
   * @param packageDir the home directory of this package
   * @param libJar the path to the jar that contains the libs (relative to the
   *          package home directory)
   * @param libJarPaths paths to libraries to install within the jar
   * @throws IOException if a problem occurs
   */
  protected void installNativeLibs(File packageDir, String libJar,
    List<String> libJarPaths) throws IOException {
    File libJarFile =
      new File(packageDir.toString() + File.separator + libJar.trim());
    if (!libJarFile.exists()) {
      System.err.println("Native lib jar file '" + libJarFile.toString()
        + "' does " + "not seem to exist - skipping");
      return;
    }

    ZipFile libZip = new ZipFile(libJarFile);
    Enumeration enumeration = libZip.entries();
    List<String> libNames = new ArrayList<>();
    while (enumeration.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
      if (!zipEntry.isDirectory()) {
        String lastPart = zipEntry.getName().replace("\\", "/");
        lastPart = lastPart.substring(lastPart.lastIndexOf("/") + 1);
        if (libJarPaths.contains(lastPart)) {
          // check to see if it is already installed
          File installPath =
            new File(WekaPackageManager.NATIVE_LIBS_DIR, lastPart);

          if (!installPath.exists()) {
            InputStream inS =
              new BufferedInputStream(libZip.getInputStream(zipEntry));
            BufferedOutputStream bos =
              new BufferedOutputStream(new FileOutputStream(installPath));
            try {
              copyStreams(inS, bos);
            } finally {
              inS.close();
              bos.flush();
              bos.close();
            }
          }
          libNames.add(lastPart);
        }
      }
      if (libNames.size() == libJarPaths.size()) {
        break;
      }
    }
    libZip.close();
  }

  private static void copyStreams(InputStream input, OutputStream output)
    throws IOException {
    int count;
    byte data[] = new byte[1024];
    while ((count = input.read(data, 0, 1024)) != -1) {
      output.write(data, 0, count);
    }
  }

  /**
   * Returns true if a named native library is already present in
   * $WEKA_HOME/native
   *
   * @param libName the name of the library to check
   * @return true if the library is already present in $WEKA_HOME/native
   */
  protected boolean nativeLibInstalled(String libName) {
    boolean result = false;
    File[] contents = WekaPackageManager.NATIVE_LIBS_DIR.listFiles();
    if (contents != null) {
      for (File f : contents) {
        if (f.getName().equals(libName)) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Process jar files in a directory of this package. Jar files at the
   * top-level and those in the lib directory are added to this URLClassLoader's
   * list, unless specified in the list of jars to ignore.
   * 
   * @param dir the directory to process
   * @param jarsToIgnore a list of jar files to ignore
   * @param topLevel true if this is the top-level (home) directory of the
   *          package
   * @throws MalformedURLException if a problem occurs
   */
  protected void processDir(File dir, List<String> jarsToIgnore,
    boolean topLevel) throws MalformedURLException {
    File[] contents = dir.listFiles();
    if (contents != null) {
      for (File content : contents) {
        if (content.isFile()
          && content.getPath().toLowerCase().endsWith(".jar")) {
          if (jarsToIgnore.contains(content.getName())) {
            continue;
          }
          URL url = content.toURI().toURL();
          addURL(url);
          if (topLevel) {
            // m_packageJarURLs.add(url);
            storeJarContents(content, m_packageJarEntries, true);
            if (m_debug) {
              System.out.println("Package jar: " + content.getName());
            }
          } else {
            if (m_debug) {
              System.out.println("Lib jar: " + content.toString());
            }
            storeJarContents(content, m_libJarEntries, false);
          }
        } else if (content.isDirectory()
          && content.getName().equalsIgnoreCase("lib")) {
          processDir(content, jarsToIgnore, false);
        }
      }
    }
  }

  /**
   * Stores all the names of all classes in the supplied jar file in the
   * supplied set. Non-class, non-directory entries in jar files at the
   * top-level of the package are stored as resources in a separate lookup set.
   * 
   * @param jarFile jar file to process
   * @param repo a set to store class names in
   * @param isTopLevelPackageJar true if this jar file is at the top-level of
   *          the package.
   */
  protected void storeJarContents(File jarFile, Set<String> repo,
    boolean isTopLevelPackageJar) {
    if (jarFile.exists()) {
      try {
        JarFile jar = new JarFile(jarFile);
        Enumeration<JarEntry> enm = jar.entries();
        while (enm.hasMoreElements()) {
          JarEntry entry = enm.nextElement();
          if (entry.getName().endsWith(".class")) {
            String cleanedUp = ClassCache.cleanUp(entry.getName());
            repo.add(cleanedUp);
          } else if (!entry.isDirectory()
            && !entry.getName().contains("META-INF") && isTopLevelPackageJar) {
            String resource = entry.getName();
            resource = resource.replace("\\", "/");
            m_packageJarResources.add(entry.getName());
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Find a class using this package classloader. First checks the parent
   * classloader (typically application classloader) - covers general stuff and
   * weka core classes. Next tries the package jar files and third-party libs
   * covered by this classloader. Next tries packages this one depends on and
   * their third-party libs - this is transitive. Finally tries all top-level
   * package jar files over all packages.
   *
   * @param name the name of the class to find
   * @return the class
   * @throws ClassNotFoundException if the class cannot be found
   */
  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    // check to see if it has already been loaded/cached first
    Class<?> result = findLoadedClass(name);
    if (result == null) {
      try {
        result = super.findClass(name);
      } catch (ClassNotFoundException ex) {
        // try dependencies (NOTE that this also processes transitive
        // dependencies)
        for (String packageName : m_packageDependencies) {
          result = m_classloaderRepo.findClass(packageName, name);
          if (result != null) {
            break;
          }
        }
      }
    }

    if (result == null) {
      // try only top-level jars of all other known packages
      try {
        result = m_classloaderRepo.findClass(name);
      } catch (ClassNotFoundException ex) {
        //
      }
    }

    if (result == null) {
      throw new ClassNotFoundException("[" + toString()
        + "] Unable to find class: " + name);
    }
    return result;
  }

  /**
   * Find a named resource. First checks parent classloader and stuff covered by
   * this classloader. Next tries packages that this one depends on.
   * 
   * @param name the name of the resource to look for
   * @return the URL of the resource, or null if the resource is not found
   */
  @Override
  public URL getResource(String name) {
    URL result = super.getResource(name);

    if (result == null) {
      for (String packageName : m_packageDependencies) {
        result = m_classloaderRepo.findResource(packageName, name);
        if (result != null) {
          break;
        }
      }
    }

    if (result == null) {
      // Using SomeObject.getClass().getClassLoader is not the same as
      // SomeClass.class.getClassLoader()
      if (m_debug) {
        System.out.println("Trying parent classloader ("
          + m_classloaderRepo.getClass().getClassLoader() + ") for resource '"
          + name + "'");
      }
      result = m_classloaderRepo.getClass().getClassLoader().getResource(name);

      if (result == null && m_debug) {
        System.out.println("Failed...");
      }
    }

    if (m_debug) {
      System.out.println(m_packageName + " classloader searching for resource "
        + name + (result != null ? " - found" : " - not found"));
    }

    return result;
  }

  /**
   * Find an enumeration of resources matching the supplied name. First checks
   * parent classloader and stuff covered by this classloader. Next tries
   * packages that this one depends on.
   *
   * @param name the name to look for
   * @return an enumeration of URLs
   * @throws IOException if a problem occurs
   */
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> result = null;
    java.util.ServiceLoader l;
    result = super.getResources(name);

    if (result == null || !result.hasMoreElements()) {
      for (String packageName : m_packageDependencies) {
        result = m_classloaderRepo.findResources(packageName, name);
        if (result != null && result.hasMoreElements()) {
          break;
        }
      }
    }

    if (result == null || !result.hasMoreElements()) {
      if (m_debug) {
        System.out.println("Trying parent classloader ("
          + m_classloaderRepo.getClass().getClassLoader() + ") for resources '"
          + name + "'");
      }

      result = m_classloaderRepo.getClass().getClassLoader().getResources(name);
      if ((result == null || !result.hasMoreElements()) && m_debug) {
        System.out.println("Failed...");
      }
    }

    if (m_debug) {
      System.out.println(m_packageName
        + " classloader searching for resources " + name
        + (result != null ? " - found" : " - not found"));
    }

    return result;
  }

  /**
   * Returns true if this classloader is covering the named third-party class
   *
   * @param className the third-party classname to check for
   * @return true if this classloader is covering the named third-party class
   */
  public boolean hasThirdPartyClass(String className) {
    return m_libJarEntries.contains(className);
  }

  /**
   * Returns a Class object if this classloader covers the named globally
   * visible class (i.e. its top-level jar(s) contain the class), or null
   * otherwise.
   * 
   * @param name the name of the class to check for
   * @return the Class or null.
   */
  protected Class<?> findGloballyVisiblePackageClass(String name) {
    Class<?> result = null;

    // check only entries in top-level package jar files
    if (classExistsInPackageJarFiles(name)) {
      try {
        // check to see if it has already been loaded/cached first
        result = findLoadedClass(name);

        if (result == null) {
          result = super.findClass(name);
        }
      } catch (ClassNotFoundException e) {
        // ignore here
      }
    }
    return result;
  }

  /**
   * Returns a URL a resource if this classloader covers the named globally
   * visible resource (i.e. its top-level jar(s) contain the resource), or null
   * otherwise
   * 
   * @param name the path to the resource
   * @return a URL to the resource, or null if not covered by this package.
   */
  protected URL findGloballyVisiblePackageResource(String name) {
    URL result = null;

    if (resourceExistsInPackageJarFiles(name)) {
      result = super.findResource(name);
    }

    return result;
  }

  /**
   * return an enumeration of URLS if this classloader covers the named globally
   * visible resource, or null otherwise.
   * 
   * @param name the path of the resource to check for
   * @return an enumeration of URLs, or null
   * @throws IOException if a problem occurs
   */
  protected Enumeration<URL> findGloballyVisiblePackageResources(String name)
    throws IOException {
    Enumeration<URL> result = null;

    if (resourceExistsInPackageJarFiles(name)) {
      result = super.findResources(name);
    }

    return result;
  }

  private static Object getFieldObject(Class<?> clazz, String name, Object obj)
    throws Exception {
    Field field = clazz.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(obj);
  }

  /**
   * Try to clear classloader file locks under Windows
   */
  protected void closeClassLoader() {
    try {
      super.close();
    } catch (Exception ex) {
      System.err.println("Failed to close class loader.");
      ex.printStackTrace();
    }
    HashSet<String> closedFiles = new HashSet<String>();
    try {
      Object obj = getFieldObject(URLClassLoader.class, "ucp", this);
      ArrayList<?> loaders =
        (ArrayList<?>) getFieldObject(obj.getClass(), "loaders", obj);
      for (Object ldr : loaders) {
        try {
          JarFile file = (JarFile) getFieldObject(ldr.getClass(), "jar", ldr);
          closedFiles.add(file.getName());
          file.close();
        } catch (Exception e) {
          // skip
        }
      }
    } catch (Exception e) {
      // skip
    }

    try {
      Vector<?> nativeLibArr =
        (Vector<?>) getFieldObject(ClassLoader.class, "nativeLibraries", this);
      for (Object lib : nativeLibArr) {
        try {
          Method fMethod =
            lib.getClass().getDeclaredMethod("finalize", new Class<?>[0]);
          fMethod.setAccessible(true);
          fMethod.invoke(lib, new Object[0]);
        } catch (Exception e) {
          // skip
        }
      }
    } catch (Exception e) {
      // skip
    }

    HashMap<?, ?> uCache = null;
    HashMap<?, ?> fCache = null;

    try {
      Class<?> jarUrlConnClass = null;
      try {
        ClassLoader contextClassLoader =
          Thread.currentThread().getContextClassLoader();
        jarUrlConnClass =
          contextClassLoader
            .loadClass("sun.net.www.protocol.jar.JarURLConnection");
      } catch (Throwable skip) {
        // skip
      }
      if (jarUrlConnClass == null) {
        jarUrlConnClass =
          Class.forName("sun.net.www.protocol.jar.JarURLConnection");
      }
      Class<?> factory =
        getFieldObject(jarUrlConnClass, "factory", null).getClass();
      try {
        fCache = (HashMap<?, ?>) getFieldObject(factory, "fileCache", null);
      } catch (Exception e) {
        // skip
      }
      try {
        uCache = (HashMap<?, ?>) getFieldObject(factory, "urlCache", null);
      } catch (Exception e) {
        // skip
      }
      if (uCache != null) {
        Set<?> set = null;
        while (set == null) {
          try {
            set = ((HashMap<?, ?>) uCache.clone()).keySet();
          } catch (ConcurrentModificationException e) {
            // Fix for BACKLOG-2149 - Do nothing - while loop will try again.
          }
        }

        for (Object file : set) {
          if (file instanceof JarFile) {
            JarFile jar = (JarFile) file;
            if (!closedFiles.contains(jar.getName())) {
              continue;
            }
            try {
              jar.close();
            } catch (IOException e) {
              // skip
            }
            if (fCache != null) {
              fCache.remove(uCache.get(jar));
            }
            uCache.remove(jar);
          }
        }
      } else if (fCache != null) {
        for (Object key : ((HashMap<?, ?>) fCache.clone()).keySet()) {
          Object file = fCache.get(key);
          if (file instanceof JarFile) {
            JarFile jar = (JarFile) file;
            if (!closedFiles.contains(jar.getName())) {
              continue;
            }
            try {
              jar.close();
            } catch (IOException e) {
              // ignore
            }
            fCache.remove(key);
          }
        }
      }
    } catch (Exception e) {
      // skip
      e.printStackTrace();
    }
  }

  private boolean classExistsInPackageJarFiles(String name) {

    return m_packageJarEntries.contains(name);
  }

  private boolean resourceExistsInPackageJarFiles(String name) {

    return m_packageJarResources.contains(name);
  }

  /**
   * String representation of this classloader
   * 
   * @return the string representation of this classloader
   */
  public String toString() {
    return "" + getClass().getCanonicalName() + " (" + m_packageName + ")";
  }

  /**
   * Return the name of the package that this classloader loads classes for
   *
   * @return return the name of the package that this classloader loads classes
   *         for
   */
  public String getPackageName() {
    return m_packageName;
  }

  /**
   * Get a Set of the names of all classes contained within top-level jar files
   * in this package
   * 
   * @return a Set of the names of classes contained in top-level jar files in
   *         this package
   */
  public Set<String> getPackageJarEntries() {
    return m_packageJarEntries;
  }

  /**
   * Check the integrity of this classloader (typically done for each
   * classloader after all packages have been loaded by the package manager).
   * Checks to see that a classloader for each dependency is available
   *
   * @return true if a classloader for each dependency is available
   */
  protected boolean integrityCheck() throws Exception {

    for (String dep : m_packageDependencies) {
      if (m_classloaderRepo.getPackageClassLoader(dep) == null) {
        return false;
      }
    }

    // now check for missing classes, unset env vars etc.
    Package p = WekaPackageManager.getInstalledPackageInfo(m_packageName);
    if (!checkForMissingClasses(p, System.err)) {
      return false;
    }

    if (!checkForUnsetEnvVar(p)) {
      return false;
    }

    if (!checkForMissingFiles(p, new File(WekaPackageManager.getPackageHome()
      .toString() + File.separator + p.getName()), System.err)) {
      return false;
    }

    setSystemProperties(p, System.out);

    return true;
  }

  /**
   * Set any system properties specified in the metadata for this package
   *
   * @param toLoad the package in question
   * @param progress for printing progress/debug info
   */
  protected void setSystemProperties(Package toLoad, PrintStream... progress) {
    Object sysProps =
      toLoad
        .getPackageMetaDataElement(WekaPackageManager.SET_SYSTEM_PROPERTIES_KEY);
    if (sysProps != null && sysProps.toString().length() > 0) {
      // individual props separated by ;'s
      String[] propsToSet = sysProps.toString().split(";");
      for (String prop : propsToSet) {
        String[] keyVals = prop.split("=");
        if (keyVals.length == 2) {
          String key = keyVals[0].trim();
          String val = keyVals[1].trim();
          if (m_debug) {
            for (PrintStream p : progress) {
              p.println("[" + toString() + "] setting property: " + prop);
            }
          }
          System.setProperty(key, val);
        }
      }
    }
  }

  /**
   * Checks to see if there are any classes that we should try to instantiate
   * before allowing this package to be loaded. This is useful for checking to
   * see if third-party classes are accessible. An example would be Java3D,
   * which has an installer that installs into the JRE/JDK.
   * 
   * @param toCheck the package in question
   * @param progress for printing progress/error info
   *
   * @return true if good to go
   * @throws Exception if there is a problem
   */
  protected boolean checkForMissingClasses(Package toCheck,
    PrintStream... progress) throws Exception {

    boolean result = true;

    Object doNotLoadIfClassNotInstantiable =
      toCheck.getPackageMetaDataElement(DO_NOT_LOAD_IF_CLASS_NOT_PRESENT_KEY);

    if (doNotLoadIfClassNotInstantiable != null
      && doNotLoadIfClassNotInstantiable.toString().length() > 0) {

      StringTokenizer tok =
        new StringTokenizer(doNotLoadIfClassNotInstantiable.toString(), ",");
      while (tok.hasMoreTokens()) {
        String nextT = tok.nextToken().trim();
        try {
          findClass(nextT);
        } catch (Exception ex) {
          for (PrintStream p : progress) {
            p.println("[WekaPackageLibIsolatingClassLoader] "
              + toCheck.getName() + " can't be loaded because " + nextT
              + " can't be instantiated.");
          }
          result = false;
          break;
        }
      }
    }

    if (!result) {
      // grab the message to print to the log (if any)
      Object doNotLoadMessage =
        toCheck
          .getPackageMetaDataElement(DO_NOT_LOAD_IF_ENV_VAR_NOT_SET_MESSAGE_KEY);
      if (doNotLoadMessage != null && doNotLoadMessage.toString().length() > 0) {
        for (PrintStream p : progress) {
          String dnlM = doNotLoadMessage.toString();
          try {
            dnlM = Environment.getSystemWide().substitute(dnlM);
          } catch (Exception e) {
            // quietly ignore
          }
          p.println("[Weka] " + dnlM);
        }
      }
    }

    return result;
  }

  /**
   * Checks to see if there are any missing files/directories for a given
   * package. If there are missing files, then the package can't be loaded. An
   * example would be a connector package that, for whatever reason, can't
   * include a necessary third-party jar file in its lib folder, and requires
   * the user to download and install this jar file manually.
   *
   * @param toLoad the package to check
   * @param packageRoot the root directory of the package
   * @param progress for printing progress/error info
   * @return true if good to go
   */
  public static boolean checkForMissingFiles(Package toLoad, File packageRoot,
    PrintStream... progress) {
    boolean result = true;

    Object doNotLoadIfFileMissing =
      toLoad.getPackageMetaDataElement(DO_NOT_LOAD_IF_FILE_NOT_PRESENT_KEY);
    String packageRootPath = packageRoot.getPath() + File.separator;

    if (doNotLoadIfFileMissing != null
      && doNotLoadIfFileMissing.toString().length() > 0) {

      StringTokenizer tok =
        new StringTokenizer(doNotLoadIfFileMissing.toString(), ",");
      while (tok.hasMoreTokens()) {
        String nextT = tok.nextToken().trim();
        File toCheck = new File(packageRootPath + nextT);
        if (!toCheck.exists()) {
          for (PrintStream p : progress) {
            p.println("[Weka] " + toLoad.getName()
              + " can't be loaded because " + toCheck.getPath()
              + " appears to be missing.");
          }
          result = false;
          break;
        }
      }
    }

    if (!result) {
      // grab the message to print to the log (if any)
      Object doNotLoadMessage =
        toLoad
          .getPackageMetaDataElement(DO_NOT_LOAD_IF_FILE_NOT_PRESENT_MESSAGE_KEY);
      if (doNotLoadMessage != null && doNotLoadMessage.toString().length() > 0) {
        String dnlM = doNotLoadMessage.toString();
        try {
          dnlM = Environment.getSystemWide().substitute(dnlM);
        } catch (Exception ex) {
          // quietly ignore
        }
        for (PrintStream p : progress) {
          p.println("[Weka] " + dnlM);
        }
      }
    }

    return result;
  }

  /**
   * Checks to see if there are any environment variables or properties that
   * should be set at startup before allowing this package to be loaded. This is
   * useful for packages that might not be able to function correctly if certain
   * variables are not set correctly.
   *
   * @param toLoad the package to check
   * @return true if good to go
   */
  protected static boolean checkForUnsetEnvVar(Package toLoad) {
    Object doNotLoadIfUnsetVar =
      toLoad.getPackageMetaDataElement(DO_NOT_LOAD_IF_ENV_VAR_NOT_SET_KEY);

    boolean result = true;
    if (doNotLoadIfUnsetVar != null
      && doNotLoadIfUnsetVar.toString().length() > 0) {
      String[] elements = doNotLoadIfUnsetVar.toString().split(",");

      Environment env = Environment.getSystemWide();

      for (String var : elements) {
        if (env.getVariableValue(var.trim()) == null) {

            System.err.println("[Weka] " + toLoad.getName()
              + " can't be loaded because " + "the environment variable " + var
              + " is not set.");


          result = false;
          break;
        }
      }
    }

    if (!result) {
      // grab the message to print to the log (if any)
      Object doNotLoadMessage =
        toLoad
          .getPackageMetaDataElement(DO_NOT_LOAD_IF_ENV_VAR_NOT_SET_MESSAGE_KEY);
      if (doNotLoadMessage != null && doNotLoadMessage.toString().length() > 0) {

          String dnlM = doNotLoadMessage.toString();
          try {
            dnlM = Environment.getSystemWide().substitute(dnlM);
          } catch (Exception e) {
            // quietly ignore
          }
          System.err.println("[Weka] " + dnlM);
      }
    }

    return result;
  }
}
