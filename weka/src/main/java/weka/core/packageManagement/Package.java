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
 *    Package.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core.packageManagement;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for Packages.
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 52462 $
 * 
 */
public abstract class Package implements Cloneable, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -8193697646938632764L;

  /**
   * The meta data for the package
   */
  protected Map<?, ?> m_packageMetaData;

  /**
   * Set the meta data for this package.
   * 
   * @param metaData the meta data for this package.
   */
  public void setPackageMetaData(Map<?, ?> metaData) {
    m_packageMetaData = metaData;
  }

  /**
   * Get the meta data for this package.
   * 
   * @return the meta data for this package
   */
  public Map<?, ?> getPackageMetaData() {
    return m_packageMetaData;
  }

  /**
   * Convenience method to return the name of this package.
   * 
   * @return the name of this package.
   */
  public abstract String getName();

  /**
   * Convenience method that returns the URL to the package (i.e the provider's
   * URL). This information is assumed to be stored in the package meta data.
   * 
   * @return the URL to the package or null if the URL is not available for some
   *         reason
   * @throws Exception if the URL can't be retrieved for some reason
   */
  public abstract URL getPackageURL() throws Exception;

  /**
   * Compare the supplied package to this package. Simply does an equals()
   * comparison between the two. Concrete subclasses should override if they
   * wan't to do comparison based on specific package meta data elements.
   * 
   * @param toCompare the package to compare against.
   * @return true if the supplied package is equal to this one.
   */
  public boolean equals(Package toCompare) {
    return m_packageMetaData.equals(toCompare.getPackageMetaData());
  }

  /**
   * Get the list of packages that this package depends on.
   * 
   * @return the list of packages that this package depends on.
   * @throws Exception if a problem occurs while getting the list of
   *           dependencies.
   */
  public abstract List<Dependency> getDependencies() throws Exception;

  /**
   * Returns true if this package is already installed
   * 
   * @return true if this package is installed
   */
  public abstract boolean isInstalled();

  /**
   * Install this package.
   * 
   * @throws Exception if something goes wrong during installation.
   */
  public abstract void install() throws Exception;

  /**
   * Returns true if this package is compatible with the currently installed
   * version of the base system.
   * 
   * @return true if this package is compatible with the main software system.
   * @throws Exception if a problem occurs while checking compatibility.
   */
  public abstract boolean isCompatibleBaseSystem() throws Exception;

  /**
   * Returns true if this package is good to go with the current version of the
   * software and versions of any installed packages that it depends on.
   * 
   * @return true if this package is good to go.
   * @throws Exception if a problem occurs while checking compatibility.
   */
  /*
   * public abstract boolean isCompatible() throws Exception;
   */

  /**
   * Gets the dependency on the base system that this package requires.
   * 
   * @return the base system dependency(s) for this package
   * @throws Exception if the base system dependency can't be determined for
   *           some reason.
   */
  public abstract List<Dependency> getBaseSystemDependency() throws Exception;

  /**
   * Gets a list of packages that this package depends on that are not currently
   * installed.
   * 
   * @return a list of missing packages that this package depends on.
   */
  public abstract List<Dependency> getMissingDependencies() throws Exception;

  /**
   * Gets a list of packages that this package depends on that are not in the
   * supplied list of packages.
   * 
   * @param packages a list of packages to compare this package's dependencies
   *          against.
   * @return those packages that this package depends on that aren't in the
   *         supplied list.
   * @throws Exception if the list of missing depenencies can't be determined
   *           for some reason.
   */
  public abstract List<Dependency>
    getMissingDependencies(List<Package> packages) throws Exception;

  /**
   * Gets a list of installed packages that this package depends on that are
   * currently incompatible with this package.
   * 
   * @return a list of incompatible installed packages that this package depends
   *         on.
   */
  public abstract List<Dependency> getIncompatibleDependencies()
    throws Exception;

  /**
   * Compares this package's precluded list (if any) against the list of
   * supplied packages. Any packages in the supplied list that match (by name
   * and version constraints) any in the precluded list are returned
   *
   * @param packages a list of packages to compare against those in this
   *          package's precluded list
   * @return a list of packages that are covered by those in the precluded list
   * @throws Exception if a problem occurs
   */
  public abstract List<Package> getPrecludedPackages(List<Package> packages)
    throws Exception;

  /**
   * Gets those packages from the supplied list that this package depends on and
   * are currently incompatible with this package.
   * 
   * @param packages a list of packages to compare this package's dependencies
   *          against
   * @return those packages from the supplied list that are incompatible with
   *         respect to this package's dependencies
   * @throws Exception if the list of incompatible dependencies can't be
   *           generated for some reason.
   */
  public abstract List<Dependency> getIncompatibleDependencies(
    List<Package> packages) throws Exception;

  /**
   * Gets the package meta data element associated with the supplied key.
   * 
   * @param key the key to use to look up a value in the meta data
   * @return the meta data value or null if the key does not exist.
   */
  public Object getPackageMetaDataElement(Object key) {
    if (m_packageMetaData == null) {
      return null;
    }

    return m_packageMetaData.get(key);
  }

  /**
   * Adds a key, value pair to the meta data map.
   * 
   * @param key the key
   * @param value the value to add
   * @throws Exception if there is no meta data map to add to.
   */
  public abstract void setPackageMetaDataElement(Object key, Object value)
    throws Exception;

  @Override
  public abstract Object clone();
}
