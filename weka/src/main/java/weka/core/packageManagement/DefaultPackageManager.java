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
 *    DefaultPackageManager.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core.packageManagement;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * A concrete implementation of PackageManager that uses Java properties
 * files/class to manage package meta data. Assumes that meta data for
 * individual packages is stored on the central repository (accessible via http)
 * in properties files that live in a subdirectory with the same name as the
 * package. Furthermore, each property file is assumed to be named as the
 * version number of the package in question with a ".props" extension. A
 * "Latest.props" file should exist for each package and should always hold meta
 * data on the latest version of a package.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 52515 $
 */
public class DefaultPackageManager extends PackageManager {

  /** buffer size for copying files */
  protected static final int BUFF_SIZE = 100000;

  /** buffer used in copying files */
  protected static final byte[] m_buffer = new byte[BUFF_SIZE];

  protected static final String INSTALLED_PACKAGE_CACHE_FILE =
    "installedPackageCache.ser";

  protected static List<Package> s_installedPackageList;

  /** Timeout to use for comms */
  protected int m_timeout = DefaultPMDefaults.SOCKET_TIMEOUT;

  /** Key for the socket timeout property */
  protected static final String TIMEOUT_PROPERTY =
    "weka.packageManager.timeout";

  /**
   * Constructor
   */
  public DefaultPackageManager() {
    // set timeout based on property (if set)
    String timeout = System.getProperty(TIMEOUT_PROPERTY);
    if (timeout != null && timeout.length() > 0) {
      try {
        m_timeout = Integer.parseInt(timeout);
      } catch (NumberFormatException e) {
        // ignore quietly
      }
    }
  }

  /**
   * Get the default settings for the default package manager
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {

    return new DefaultPMDefaults();
  }

  /**
   * Apply settings.
   *
   * @param settings the settings to apply
   */
  @Override
  public void applySettings(Settings settings) {

    m_timeout =
      settings.getSetting(settings.getID(),
        DefaultPMDefaults.SOCKET_TIMEOUT_KEY, DefaultPMDefaults.SOCKET_TIMEOUT,
        Environment.getSystemWide());
  }

  /**
   * Default settings for the default package manager implementation
   */
  protected static final class DefaultPMDefaults extends Defaults {
    protected static final String APP_ID = "defaultpackagemanager";

    protected static final Settings.SettingKey SOCKET_TIMEOUT_KEY =
      new Settings.SettingKey(APP_ID + ".timeout",
        "Timeout (in ms) for socket " + "comms", "");
    /** Default timeout for socket connections (5 seconds) */
    protected static final int SOCKET_TIMEOUT = 5000;
    private static final long serialVersionUID = -1428588514991146709L;

    public DefaultPMDefaults() {
      super("defaultpackagemanager");
      m_defaults.put(SOCKET_TIMEOUT_KEY, SOCKET_TIMEOUT);
    }
  }

  /*
   * protected File downloadPackage2(URL packageURL, PrintStream... progress)
   * throws Exception { String packageArchiveName = packageURL.toString();
   * packageArchiveName =
   * packageArchiveName.substring(packageArchiveName.lastIndexOf('/'),
   * packageArchiveName.length()); packageArchiveName =
   * packageArchiveName.substring(0, packageArchiveName.lastIndexOf('.'));
   * 
   * // make a temp file to hold the downloaded archive File tmpDownload =
   * File.createTempFile(packageArchiveName, ".zip");
   * 
   * for (int i = 0; i < progress.length; i++) { progress[i].println(
   * "[Package Manager] Tmp file: " + tmpDownload.toString()); }
   * 
   * System.err.println("Here in downloadPackage..."); URLConnection conn =
   * null;
   * 
   * // setup the proxy (if we are using one) and open the connect if
   * (setProxyAuthentication()) { conn = packageURL.openConnection(m_httpProxy);
   * } else { conn = packageURL.openConnection(); }
   * 
   * if (conn instanceof HttpURLConnection) { System.err.println(
   * "We have a http url conn."); }
   * 
   * BufferedReader br = new BufferedReader(new
   * InputStreamReader(conn.getInputStream()));
   * 
   * String line = null;
   * 
   * while ((line = br.readLine()) != null) { System.err.println(line); }
   * 
   * br.close();
   * 
   * return null; }
   */

  protected File downloadArchive(URL packageURL, String fileExtension,
    PrintStream... progress) throws Exception {
    String packageArchiveName = packageURL.toString();

    packageArchiveName =
      packageArchiveName.substring(0,
        packageArchiveName.lastIndexOf("." + fileExtension) + 3);

    packageArchiveName =
      packageArchiveName.substring(0, packageArchiveName.lastIndexOf('.'));

    packageArchiveName =
      packageArchiveName.substring(packageArchiveName.lastIndexOf('/'),
        packageArchiveName.length());

    // make a temp file to hold the downloaded archive
    File tmpDownload =
      File.createTempFile(packageArchiveName, "." + fileExtension);

    for (PrintStream progres : progress) {
      progres.println(packageURL.toString());
      progres.println("[DefaultPackageManager] Tmp file: "
        + tmpDownload.toString());
    }

    URLConnection conn = getConnection(packageURL);

    BufferedInputStream bi = new BufferedInputStream(conn.getInputStream());

    BufferedOutputStream bo =
      new BufferedOutputStream(new FileOutputStream(tmpDownload));

    // download the archive
    int totalBytesRead = 0;
    while (true) {
      synchronized (m_buffer) {
        int amountRead = bi.read(m_buffer);
        if (amountRead == -1) {
          for (PrintStream progres : progress) {
            progres.print("[DefaultPackageManager] downloaded "
              + (totalBytesRead / 1000) + " KB\r");
          }
          break;
        }
        bo.write(m_buffer, 0, amountRead);
        totalBytesRead += amountRead;
        for (PrintStream progres : progress) {
          progres.print("%%[DefaultPackageManager] downloaded "
            + (totalBytesRead / 1000) + " KB\r");
        }
      }
    }

    bi.close();
    bo.close();

    return tmpDownload;
  }

  /**
   * Get package information on the package at the given URL.
   * 
   * @param packageURL the URL to the package.
   * @return a Package object encapsulating the package meta data
   * @throws Exception if the package meta data can't be retrieved.
   */
  @Override
  public Package getURLPackageInfo(URL packageURL) throws Exception {
    File downloaded = downloadArchive(packageURL, "zip");

    // return the package info
    return getPackageArchiveInfo(downloaded);
  }

  /**
   * Get package information on the named package from the repository. If
   * multiple versions of the package are available, it assumes that the most
   * recent is required.
   * 
   * @param packageName the name of the package to get information about.
   * @return a Package object encapsulating the package meta data.
   * @throws Exception if the package meta data can't be retrieved.
   */
  @Override
  public Package getRepositoryPackageInfo(String packageName) throws Exception {

    return getRepositoryPackageInfo(packageName, "Latest");
  }

  /**
   * Get a list of available versions of the named package.
   * 
   * @param packageName the name of the package to get versions.
   * @return a list of available versions (or null if not applicable)
   * @throws Exception if something goes wrong while trying to retrieve the list
   *           of versions.
   */
  @Override
  public List<Object> getRepositoryPackageVersions(String packageName)
    throws Exception {

    if (getPackageRepositoryURL() == null) {
      throw new Exception("[DefaultPackageManager] No package repository set!!");
    }

    String versionsS =
      m_packageRepository.toString() + "/" + packageName + "/" + "versions.txt";

    URL packageURL = new URL(versionsS);
    URLConnection conn = getConnection(packageURL);

    BufferedReader bi =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

    ArrayList<Object> versions = new ArrayList<Object>();
    String versionNumber;
    while ((versionNumber = bi.readLine()) != null) {
      versions.add(versionNumber.trim());
    }

    bi.close();
    return versions;
  }

  /**
   * Get package information on the named package from the repository.
   * 
   * @param packageName the name of the package to get information about.
   * @param version the version of the package to retrieve (may be null if not
   *          applicable).
   * @return a Package object encapsulating the package meta data.
   * @throws Exception if the package meta data can't be retrieved.
   */
  @Override
  public Package getRepositoryPackageInfo(String packageName, Object version)
    throws Exception {
    if (getPackageRepositoryURL() == null) {
      throw new Exception("[DefaultPackageManager] No package repository set!!");
    }

    if (version == null) {
      version = "Latest";
    }

    String packageS =
      m_packageRepository.toString() + "/" + packageName + "/"
        + version.toString() + ".props";
    URL packageURL = new URL(packageS);
    URLConnection conn = getConnection(packageURL);

    BufferedInputStream bi = new BufferedInputStream(conn.getInputStream());
    Properties packageProperties = new Properties();
    packageProperties.load(bi);
    bi.close();

    return new DefaultPackage(m_packageHome, this, packageProperties);
  }

  private Package getPackageArchiveInfo(File packageArchive) throws Exception {
    return getPackageArchiveInfo(packageArchive.getAbsolutePath());
  }

  /**
   * Get package information from the supplied package archive file.
   * 
   * @param packageArchivePath the path to the package archive file
   * @return a Package object encapsulating the package meta data.
   * @throws Exception if the package meta data can't be retrieved.
   */
  @Override
  public Package getPackageArchiveInfo(String packageArchivePath)
    throws Exception {
    ZipFile zip = new ZipFile(new File(packageArchivePath));

    for (Enumeration e = zip.entries(); e.hasMoreElements();) {
      ZipEntry entry = (ZipEntry) e.nextElement();
      if (entry.getName().endsWith("Description.props")) {
        InputStream is = zip.getInputStream(entry);
        Properties packageProperties = new Properties();
        packageProperties.load(new BufferedInputStream(is));
        is.close();

        DefaultPackage pkg =
          new DefaultPackage(m_packageHome, this, packageProperties);

        return pkg;
      }
    }

    throw new Exception("Unable to find Description file in package archive!");
  }

  /**
   * Get package information on the named installed package.
   * 
   * @param packageName the name of the package to get information about.
   * @return a Package object encapsulating the package meta data or null if the
   *         package is not installed.
   * 
   * @throws Exception if the package meta data can't be retrieved.
   */
  @Override
  public Package getInstalledPackageInfo(String packageName) throws Exception {
    File packageDescription =
      new File(m_packageHome.getAbsoluteFile() + File.separator + packageName
        + File.separator + "Description.props");

    if (!packageDescription.exists()) {
      return null;
    }

    FileInputStream fis = new FileInputStream(packageDescription);

    Properties packageProperties = new Properties();
    packageProperties.load(fis);
    fis.close();

    DefaultPackage pkg =
      new DefaultPackage(m_packageHome, this, packageProperties);
    return pkg;
  }

  /**
   * Checks to see if the package home exists and creates it if necessary.
   * 
   * @return true if the package home exists/was created successfully.
   */
  protected boolean establishPackageHome() {
    if (m_packageHome == null) {
      return false;
    }

    if (!m_packageHome.exists()) {
      // create it for the user
      if (!m_packageHome.mkdir()) {
        System.err.println("Unable to create packages directory ("
          + m_packageHome.getAbsolutePath() + ")");
        return false;
      }
    }
    return true;
  }

  public static void deleteDir(File dir, PrintStream... progress)
    throws Exception {

    // get the contents
    File[] contents = dir.listFiles();

    if (contents.length != 0) {
      // process contents
      for (File f : contents) {
        if (f.isDirectory()) {
          deleteDir(f);
        } else {
          for (PrintStream progres : progress) {
            progres
              .println("[DefaultPackageManager] removing: " + f.toString());
          }
          if (!f.delete()) {
            System.err.println("[DefaultPackageManager] can't delete file "
              + f.toString());
            f.deleteOnExit();
          }
        }
      }
    }

    // delete this directory
    if (!dir.delete()) {
      System.err.println("[DefaultPackageManager] can't delete directory "
        + dir.toString());
      dir.deleteOnExit();
    }
    for (PrintStream progres : progress) {
      progres.println("[DefaultPackageManager] removing: " + dir.toString());
    }
  }

  /**
   * Uninstall a package.
   * 
   * @param packageName the package to uninstall.
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @throws Exception if the named package could not be removed for some
   *           reason.
   */
  @Override
  public void uninstallPackage(String packageName, PrintStream... progress)
    throws Exception {
    File packageToDel =
      new File(m_packageHome.toString() + File.separator + packageName);

    if (!packageToDel.exists()) {
      throw new Exception("[DefaultPackageManager] Can't remove " + packageName
        + " because it doesn't seem to be installed!");
    }

    deleteDir(packageToDel, progress);

    // invalidate cache
    s_installedPackageList = null;
    deleteInstalledPackageCacheFile();
  }

  /**
   * Install a package from an archive on the local file system.
   * 
   * @param packageArchivePath the path to the package archive file.
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @return the name of the package installed
   * @throws Exception if the package can't be installed for some reason.
   */
  @Override
  public String installPackageFromArchive(String packageArchivePath,
    PrintStream... progress) throws Exception {
    Properties packageProps =
      (Properties) getPackageArchiveInfo(packageArchivePath)
        .getPackageMetaData();
    String packageName = packageProps.getProperty("PackageName");
    String additionalLibs = packageProps.getProperty("AdditionalLibs");
    String[] additionalLibURLs = null;

    if (additionalLibs != null && additionalLibs.length() > 0) {
      additionalLibURLs = additionalLibs.split(",");
    }
    if (packageName == null) {
      throw new Exception("Unable to find the name of the package in"
        + " the Description file for " + packageArchivePath);
    }

    installPackage(packageArchivePath, packageName, progress);

    if (additionalLibURLs != null) {
      installAdditionalLibs(packageName, additionalLibURLs, progress);
    }

    return packageName;
  }

  /**
   * Installs additional library jar files (as specified in the AdditionalLibs
   * entry in the Description.props file).
   * 
   * @param packageName the name of the package that will receive the downloaded
   *          libraries into its lib directory
   * @param additionalLibURLs an array of urls to the libraries to download
   * @param progress for progress reporting
   * @throws Exception if a problem occurs
   */
  protected void installAdditionalLibs(String packageName,
    String[] additionalLibURLs, PrintStream... progress) throws Exception {

    if (!establishPackageHome()) {
      throw new Exception("Unable to install additional libraries"
        + " because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    for (String libU : additionalLibURLs) {
      libU = libU.trim();

      if (libU.trim().length() > 0) {
        URL libURL = new URL(libU.trim());

        File libPath = downloadArchive(libURL, "jar", progress);

        String destName = libU.substring(0, libU.lastIndexOf("." + "jar") + 3);
        destName = destName.substring(0, destName.lastIndexOf('.'));
        destName =
          destName.substring(destName.lastIndexOf('/'), destName.length());
        destName += ".jar";

        File destDir =
          new File(m_packageHome, packageName + File.separator + "lib");
        if (!destDir.mkdir()) {
          // hopefully failure is because the directory already exists
        }
        File destPath = new File(destDir, destName);

        InputStream input =
          new BufferedInputStream(new FileInputStream(libPath));
        OutputStream output =
          new BufferedOutputStream(new FileOutputStream(destPath));
        copyStreams(input, output);

        input.close();
        output.flush();
        output.close();
      }
    }
  }

  /**
   * Installs all the packages in the supplied list.
   * 
   * @param toInstall a list of Packages to install.
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @throws Exception if something goes wrong during the installation process.
   */
  @Override
  public void installPackages(List<Package> toInstall, PrintStream... progress)
    throws Exception {
    File[] archivePaths = new File[toInstall.size()];

    for (int i = 0; i < toInstall.size(); i++) {
      Package toDownload = toInstall.get(i);
      if (toDownload.isInstalled()) {
        for (PrintStream p : progress) {
          p.println("[DefaultPackageManager] cleanining installed package: "
            + toDownload.getName());
          uninstallPackage(toDownload.getName(), progress);
        }
      }
      archivePaths[i] =
        downloadArchive(toDownload.getPackageURL(), "zip", progress);
    }

    // OK, all downloaded successfully, now install them
    for (File archivePath : archivePaths) {
      installPackageFromArchive(archivePath.getAbsolutePath(), progress);
    }
  }

  /**
   * Checks a given package's list of dependencies for any conflicts with the
   * packages in the supplied Map. Any packages from this packages dependency
   * list that are not in the Map are simply added and checkDependencies is
   * called recursively for each.
   * 
   * @param toCheck the package to check.
   * @param lookup a Map of package name, Dependency pairs to check against.
   * @param conflicts a list of Dependency objects for any conflicts that are
   *          detected.
   * @return true if no conflicts are found.
   * @throws Exception if a problem occurs when checking for conflicts.
   */
  protected static boolean checkDependencies(PackageConstraint toCheck,
    Map<String, Dependency> lookup, Map<String, List<Dependency>> conflicts)
    throws Exception {
    boolean ok = true;

    // get the dependencies for the package to check
    List<Dependency> deps = toCheck.getPackage().getDependencies();

    for (Dependency p : deps) {
      String depName =
        p.getTarget().getPackage().getPackageMetaDataElement("PackageName")
          .toString();
      if (!lookup.containsKey(depName)) {
        // just add this package to the lookup
        lookup.put(depName, p);

        // check its dependencies
        ok = checkDependencies(p.getTarget(), lookup, conflicts);
      } else {
        // we have to see if the version number for this package is compatible
        // with the one already in the lookup
        Dependency checkAgainst = lookup.get(depName);
        PackageConstraint result =
          checkAgainst.getTarget().checkConstraint(p.getTarget());
        if (result != null) {
          checkAgainst.setTarget(result);
          lookup.put(depName, checkAgainst);
        } else {
          // there is a conflict here
          List<Dependency> conflictList = conflicts.get(depName);
          conflictList.add(p);
          ok = false;
        }
      }
    }

    return ok;
  }

  /**
   * Gets a full list of packages (encapsulated in Dependency objects) that are
   * required by directly and indirectly by the named target package. Also
   * builds a Map of any packages that are required by more than one package and
   * where there is a conflict of some sort (e.g. multiple conflicting
   * versions). The keys of this map are package names (strings), and each
   * associated value is a list of Dependency objects.
   * 
   * @param target the package for which a list of dependencies is required.
   * @param conflicts will hold any conflicts that are discovered while building
   *          the full dependency list.
   * @return a list of packages that are directly and indirectly required by the
   *         named target package.
   * @throws Exception if a problem occurs while building the dependency list.
   */
  @Override
  public List<Dependency> getAllDependenciesForPackage(Package target,
    Map<String, List<Dependency>> conflicts) throws Exception {

    // start with the target package's list of dependencies
    List<Dependency> initialList = target.getDependencies();

    // load them into a map for quick lookup
    Map<String, Dependency> lookup = new HashMap<String, Dependency>();

    for (Dependency d : initialList) {
      lookup.put(
        d.getTarget().getPackage().getPackageMetaDataElement("PackageName")
          .toString(), d);
      ArrayList<Dependency> deps = new ArrayList<Dependency>();
      deps.add(d);

      // Pre-load a conficts Map
      conflicts.put(
        d.getTarget().getPackage().getPackageMetaDataElement("PackageName")
          .toString(), deps);
    }

    // now process each of these to build the full list
    for (Dependency d : initialList) {
      checkDependencies(d.getTarget(), lookup, conflicts);
    }

    List<Dependency> fullList = new ArrayList<Dependency>(lookup.values());

    // Prune packages from conflicts Map that only have one
    // item in their list (i.e. these ones have no conflicts)
    ArrayList<String> removeList = new ArrayList<String>();
    Iterator<String> keyIt = conflicts.keySet().iterator();

    while (keyIt.hasNext()) {
      String key = keyIt.next();
      List<Dependency> tempD = conflicts.get(key);
      if (tempD.size() == 1) {
        // remove this one
        removeList.add(key);
      }
    }

    for (String s : removeList) {
      conflicts.remove(s);
    }

    return fullList;
  }

  /**
   * Install a package sourced from the repository.
   * 
   * @param packageName the name of the package to install
   * @param version the version of the package to install (may be null if not
   *          applicable).
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @throws Exception if the package can't be installed for some reason.
   */
  @Override
  public void installPackageFromRepository(String packageName, Object version,
    PrintStream... progress) throws Exception {
    Package toInstall = getRepositoryPackageInfo(packageName, version);
    if (toInstall.isInstalled()) {
      for (PrintStream p : progress) {
        p.println("[DefaultPackageManager] cleanining installed package: "
          + toInstall.getName());
        uninstallPackage(toInstall.getName(), progress);
      }
    }

    String urlString =
      toInstall.getPackageMetaDataElement("PackageURL").toString();
    URL packageURL = new URL(urlString);

    installPackageFromURL(packageURL, progress);
  }

  /**
   * Install a package sourced from a given URL.
   * 
   * @param packageURL the URL to the package.
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @return the name of the package installed
   * @throws Exception if the package can't be installed for some reason.
   */
  @Override
  public String installPackageFromURL(URL packageURL, PrintStream... progress)
    throws Exception {
    File downloaded = downloadArchive(packageURL, "zip", progress);
    return installPackageFromArchive(downloaded.getAbsolutePath(), progress);
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
   * Installs a package from a zip/jar archive.
   * 
   * @param packageArchivePath the full path to the archived package to install.
   * @param packageName the name of the package to install.
   * @param progress optional varargs parameter, that, if supplied, is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @throws Exception if the package can't be installed for some reason.
   */
  protected void installPackage(String packageArchivePath, String packageName,
    PrintStream... progress) throws Exception {

    if (!establishPackageHome()) {
      throw new Exception("Unable to install " + packageArchivePath
        + " because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    File destDir = new File(m_packageHome, packageName);
    if (!destDir.mkdir()) {
      /*
       * throw new Exception("Unable to create package directory " +
       * destDir.getAbsolutePath());
       */

      // hopefully failure is because the directory already exists
    }

    InputStream input = null;
    OutputStream output = null;

    ZipFile zipFile = new ZipFile(packageArchivePath);
    Enumeration enumeration = zipFile.entries();
    while (enumeration.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
      if (zipEntry.isDirectory()) {
        new File(destDir, zipEntry.getName()).mkdirs();
        continue;
      } else {
        File temp = new File(destDir, zipEntry.getName()).getParentFile();
        if (temp != null && !temp.exists()) {
          temp.mkdirs();
        }
      }

      for (PrintStream progres : progress) {
        progres.println("[DefaultPackageManager] Installing: "
          + zipEntry.getName());
      }

      input = new BufferedInputStream(zipFile.getInputStream(zipEntry));
      File destFile = new File(destDir, zipEntry.getName());
      FileOutputStream fos = new FileOutputStream(destFile);
      output = new BufferedOutputStream(fos);
      copyStreams(input, output);
      input.close();
      output.flush();
      output.close();
    }

    // invalidate the cache
    s_installedPackageList = null;
    deleteInstalledPackageCacheFile();
  }

  private URLConnection getConnection(String urlString) throws IOException {
    URL connURL = new URL(urlString);

    return getConnection(connURL);
  }

  private URLConnection getConnection(URL connURL) throws IOException {
    URLConnection conn = null;

    // setup the proxy (if we are using one) and open the connect
    if (setProxyAuthentication(connURL)) {
      conn = connURL.openConnection(m_httpProxy);
    } else {
      conn = connURL.openConnection();
    }

    // Set a timeout for establishing the connection
    conn.setConnectTimeout(m_timeout);

    return conn;
  }

  private void transToBAOS(BufferedInputStream bi, ByteArrayOutputStream bos)
    throws Exception {
    while (true) {
      synchronized (m_buffer) {
        int amountRead = bi.read(m_buffer);
        if (amountRead == -1) {
          break;
        }
        bos.write(m_buffer, 0, amountRead);
      }
    }

    bi.close();
  }

  private void writeZipEntryForPackage(String packageName, ZipOutputStream zos)
    throws Exception {

    ZipEntry packageDir = new ZipEntry(packageName + "/");
    zos.putNextEntry(packageDir);

    ZipEntry z = new ZipEntry(packageName + "/Latest.props");
    ZipEntry z2 = new ZipEntry(packageName + "/Latest.html");
    URLConnection conn =
      getConnection(m_packageRepository.toString() + "/" + packageName
        + "/Latest.props");

    BufferedInputStream bi = new BufferedInputStream(conn.getInputStream());
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    transToBAOS(bi, bos);
    zos.putNextEntry(z);
    zos.write(bos.toByteArray());

    conn =
      getConnection(m_packageRepository.toString() + "/" + packageName
        + "/Latest.html");
    bi = new BufferedInputStream(conn.getInputStream());
    bos = new ByteArrayOutputStream();
    transToBAOS(bi, bos);
    zos.putNextEntry(z2);
    zos.write(bos.toByteArray());

    // write the versions.txt file to the zip
    z = new ZipEntry(packageName + "/versions.txt");
    conn =
      getConnection(m_packageRepository.toString() + "/" + packageName
        + "/versions.txt");
    bi = new BufferedInputStream(conn.getInputStream());
    bos = new ByteArrayOutputStream();
    transToBAOS(bi, bos);
    zos.putNextEntry(z);
    zos.write(bos.toByteArray());

    // write the index.html to the zip
    z = new ZipEntry(packageName + "/index.html");
    conn =
      getConnection(m_packageRepository.toString() + "/" + packageName
        + "/index.html");
    bi = new BufferedInputStream(conn.getInputStream());
    bos = new ByteArrayOutputStream();
    transToBAOS(bi, bos);
    zos.putNextEntry(z);
    zos.write(bos.toByteArray());

    // Now process the available versions
    List<Object> versions = getRepositoryPackageVersions(packageName);
    for (Object o : versions) {
      conn =
        getConnection(m_packageRepository.toString() + "/" + packageName + "/"
          + o.toString() + ".props");
      z = new ZipEntry(packageName + "/" + o.toString() + ".props");
      bi = new BufferedInputStream(conn.getInputStream());
      bos = new ByteArrayOutputStream();
      transToBAOS(bi, bos);
      zos.putNextEntry(z);
      zos.write(bos.toByteArray());

      conn =
        getConnection(m_packageRepository.toString() + "/" + packageName + "/"
          + o.toString() + ".html");
      z = new ZipEntry(packageName + "/" + o.toString() + ".html");
      bi = new BufferedInputStream(conn.getInputStream());
      bos = new ByteArrayOutputStream();
      transToBAOS(bi, bos);
      zos.putNextEntry(z);
      zos.write(bos.toByteArray());
    }
  }

  /**
   * Gets an array of bytes containing a zip of all the repository meta data and
   * supporting files. Does *not* contain any package archives etc., only a
   * snapshot of the meta data. Could be used by clients to establish a cache of
   * meta data.
   * 
   * @return a zip compressed array of bytes.
   */
  @Override
  public byte[] getRepositoryPackageMetaDataOnlyAsZip(PrintStream... progress)
    throws Exception {

    if (getPackageRepositoryURL() == null) {
      throw new Exception("[DefaultPackageManager] No package repository set!!");
    }

    try {
      String repoZip = m_packageRepository.toString() + "/repo.zip";
      URLConnection conn = null;
      conn = getConnection(repoZip);

      BufferedInputStream bi = new BufferedInputStream(conn.getInputStream());

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      // ZipOutputStream zos = new ZipOutputStream(bos);

      int totalBytesRead = 0;
      while (true) {
        synchronized (m_buffer) {
          int amountRead = bi.read(m_buffer);
          if (amountRead == -1) {
            for (PrintStream progres : progress) {
              progres.println("[DefaultPackageManager] downloaded "
                + (totalBytesRead / 1000) + " KB\r");
            }
            break;
          }
          bos.write(m_buffer, 0, amountRead);
          totalBytesRead += amountRead;
          for (PrintStream progres : progress) {
            progres.print("[DefaultPackageManager] downloaded "
              + (totalBytesRead / 1000) + " KB\r");
          }
        }
      }

      bi.close();

      return bos.toByteArray();
    } catch (Exception ex) {
      System.err.println("Unable to download repository zip archve " + "("
        + ex.getMessage() + ") - trying legacy routine...");
      return getRepositoryPackageMetaDataOnlyAsZipLegacy(progress);
    }
  }

  /**
   * Gets an array of bytes containing a zip of all the repository meta data and
   * supporting files using the legacy approach. Does *not* contain any package
   * archives etc., only a snapshot of the meta data. Could be used by clients
   * to establish a cache of meta data.
   * 
   * @return a zip compressed array of bytes.
   */
  public byte[] getRepositoryPackageMetaDataOnlyAsZipLegacy(
    PrintStream... progress) throws Exception {

    if (getPackageRepositoryURL() == null) {
      throw new Exception("[DefaultPackageManager] No package repository set!!");
    }

    String packageList = m_packageRepository.toString() + "/packageList.txt";
    String packageListWithVersion =
      m_packageRepository.toString() + "/packageListWithVersion.txt";

    URLConnection conn = null;
    conn = getConnection(packageList);

    BufferedReader bi =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(bos);

    // Process the packages
    String packageName;
    while ((packageName = bi.readLine()) != null) {
      for (PrintStream p : progress) {
        p.println("Fetching meta data for " + packageName);
      }
      writeZipEntryForPackage(packageName, zos);
    }
    bi.close();

    // include the package list (legacy) in the zip
    conn = getConnection(packageList);
    ZipEntry z = new ZipEntry("packageList.txt");
    BufferedInputStream bi2 = new BufferedInputStream(conn.getInputStream());
    ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
    transToBAOS(bi2, bos2);
    zos.putNextEntry(z);
    zos.write(bos2.toByteArray());
    bi2.close();

    // include the package list with latest version numbers
    conn = getConnection(packageListWithVersion);
    z = new ZipEntry("packageListWithVersion.txt");
    bi2 = new BufferedInputStream(conn.getInputStream());
    bos2 = new ByteArrayOutputStream();
    transToBAOS(bi2, bos2);
    zos.putNextEntry(z);
    zos.write(bos2.toByteArray());
    bi2.close();

    // Include the top level images
    String imageList = m_packageRepository.toString() + "/images.txt";
    conn = getConnection(imageList);
    bi = new BufferedReader(new InputStreamReader(conn.getInputStream()));

    String imageName;
    while ((imageName = bi.readLine()) != null) {
      // System.err.println("Processing " + imageName);
      z = new ZipEntry(imageName);
      URLConnection conn2 =
        getConnection(m_packageRepository.toString() + "/" + imageName);
      bi2 = new BufferedInputStream(conn2.getInputStream());
      bos2 = new ByteArrayOutputStream();
      transToBAOS(bi2, bos2);
      zos.putNextEntry(z);
      zos.write(bos2.toByteArray());
      bi2.close();
    }

    // include the image list in the zip
    conn = getConnection(imageList);
    z = new ZipEntry("images.txt");
    bi2 = new BufferedInputStream(conn.getInputStream());
    bos2 = new ByteArrayOutputStream();
    transToBAOS(bi2, bos2);
    zos.putNextEntry(z);
    zos.write(bos2.toByteArray());
    bi2.close();

    zos.close();

    return bos.toByteArray();
  }

  /**
   * Get all packages that the system knows about (i.e. all packages contained
   * in the repository).
   * 
   * @param progress optional varargs parameter, that, if supplied is expected
   *          to contain one or more PrintStream objects to write progress to.
   * @return a list of all packages.
   * @throws Exception if a list of packages can't be determined.
   */
  @Override
  public List<Package> getAllPackages(PrintStream... progress) throws Exception {
    ArrayList<Package> allPackages = new ArrayList<Package>();

    if (getPackageRepositoryURL() == null) {
      throw new Exception("[DefaultPackageManager] No package repository set!!");
    }

    String packageList = m_packageRepository.toString() + "/packageList.txt";

    URL packageListURL = new URL(packageList);
    URLConnection conn = getConnection(packageListURL);

    BufferedReader bi =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

    String packageName;
    while ((packageName = bi.readLine()) != null) {
      Package temp = getRepositoryPackageInfo(packageName);
      allPackages.add(temp);
    }

    return allPackages;
  }

  /**
   * Get a list of packages that are not currently installed.
   * 
   * @return a list of packages that are not currently installed.
   * @throws Exception if a list of packages can't be determined.
   */
  @Override
  public List<Package> getAvailablePackages() throws Exception {
    List<Package> allP = getAllPackages();
    List<Package> available = new ArrayList<Package>();

    for (int i = 0; i < allP.size(); i++) {
      if (!allP.get(i).isInstalled()) {
        available.add(allP.get(i));
      }
    }

    return available;
  }

  /**
   * Get a list of installed packages.
   * 
   * @return a list of installed packages.
   * @throws Exception if a list of packages can't be determined.
   */
  @Override
  public List<Package> getInstalledPackages() throws Exception {
    if (!establishPackageHome()) {
      throw new Exception("Unable to get list of installed packages "
        + "because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    if (s_installedPackageList != null) {
      return s_installedPackageList;
    }

    s_installedPackageList = loadInstalledPackageCache();
    if (s_installedPackageList != null) {
      return s_installedPackageList;
    }

    List<Package> installedP = new ArrayList<Package>();

    File[] contents = m_packageHome.listFiles();

    for (File content : contents) {
      if (content.isDirectory()) {
        File description =
          new File(content.getAbsolutePath() + File.separator
            + "Description.props");

        if (description.exists()) {
          try {
            Properties packageProperties = new Properties();
            BufferedInputStream bi =
              new BufferedInputStream(new FileInputStream(description));
            packageProperties.load(bi);
            bi.close();
            bi = null;
            DefaultPackage pkg =
              new DefaultPackage(m_packageHome, this, packageProperties);
            installedP.add(pkg);
          } catch (Exception ex) {
            // ignore if we can't load the description file for some reason
          }
        }
      }
    }

    s_installedPackageList = installedP;
    saveInstalledPackageCache(installedP);

    return installedP;
  }

  protected void deleteInstalledPackageCacheFile() throws Exception {
    if (!establishPackageHome()) {
      throw new Exception("Unable to delete installed package cache file "
        + "because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    File cache = new File(m_packageHome, INSTALLED_PACKAGE_CACHE_FILE);
    if (cache.exists()) {
      if (!cache.delete()) {
        System.err.println("Unable to delete installed package cache file '"
          + cache.toString() + "'");
        cache.deleteOnExit();
      }
    }
  }

  /**
   * Save the supplied list of Packages to the installed package cache file
   * 
   * @param cacheToSave the list of packages to save
   * @throws Exception if a problem occurs
   */
  protected void saveInstalledPackageCache(List<Package> cacheToSave)
    throws Exception {

    if (!establishPackageHome()) {
      throw new Exception("Unable to save installed package cache file "
        + "because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    ObjectOutputStream oos = null;
    try {
      oos =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
          new File(m_packageHome, INSTALLED_PACKAGE_CACHE_FILE))));
      oos.writeObject(cacheToSave);
      oos.flush();
    } finally {
      if (oos != null) {
        oos.close();
      }
    }
  }

  /**
   * Load the serialized installed package cache file
   * 
   * @return a list of installed packages (loaded from the cache file)
   * @throws Exception if a problem occurs
   */
  protected List<Package> loadInstalledPackageCache() throws Exception {
    if (!establishPackageHome()) {
      throw new Exception("Unable to load installed package cache file "
        + "because package home (" + m_packageHome.getAbsolutePath()
        + ") can't be established.");
    }

    List<Package> installedP = null;
    if (new File(m_packageHome, INSTALLED_PACKAGE_CACHE_FILE).exists()) {
      ObjectInputStream ois = null;
      try {
        ois =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(
            new File(m_packageHome.toString(), INSTALLED_PACKAGE_CACHE_FILE))));

        installedP = (List) ois.readObject();
      } catch (Exception ex) {
        deleteInstalledPackageCacheFile();
      } finally {
        if (ois != null) {
          ois.close();
        }
      }
    }

    if (installedP != null) {
      for (Package p : installedP) {
        if (p instanceof DefaultPackage) {
          ((DefaultPackage) p).setPackageManager(this);
        }
      }
    }

    return installedP;
  }

  /**
   * Pads a string to a specified length, inserting spaces on the left as
   * required. If the string is too long, characters are removed (from the
   * right).
   * 
   * @param inString the input string
   * @param length the desired length of the output string
   * @return the output string
   */
  protected static String padLeft(String inString, int length) {

    return fixStringLength(inString, length, false);
  }

  /**
   * Pads a string to a specified length, inserting spaces on the right as
   * required. If the string is too long, characters are removed (from the
   * right).
   * 
   * @param inString the input string
   * @param length the desired length of the output string
   * @return the output string
   */
  protected static String padRight(String inString, int length) {

    return fixStringLength(inString, length, true);
  }

  /**
   * Pads a string to a specified length, inserting spaces as required. If the
   * string is too long, characters are removed (from the right).
   * 
   * @param inString the input string
   * @param length the desired length of the output string
   * @param right true if inserted spaces should be added to the right
   * @return the output string
   */
  private static/* @pure@ */String fixStringLength(String inString, int length,
    boolean right) {

    if (inString.length() < length) {
      while (inString.length() < length) {
        inString = (right ? inString.concat(" ") : " ".concat(inString));
      }
    } else if (inString.length() > length) {
      inString = inString.substring(0, length);
    }
    return inString;
  }

  /*
   * public void printPackageInfo(String packagePath) throws Exception {
   * Properties packageProps = (Properties)getPackageArchiveInfo(packagePath);
   * Enumeration<?> e = packageProps.propertyNames(); while
   * (e.hasMoreElements()) { String key = (String) e.nextElement(); String value
   * = packageProps.getProperty(key); System.out.println(padLeft(key, 11) +
   * ":\t" + value); } }
   */

  public static void main(String[] args) {
    try {
      URL url = new URL(args[0]);
      DefaultPackageManager pm = new DefaultPackageManager();
      pm.downloadArchive(url, args[1], System.out);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
