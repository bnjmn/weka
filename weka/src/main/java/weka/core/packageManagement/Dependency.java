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
 *    Dependency.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core.packageManagement;

/**
 * Class that encapsulates a dependency between two packages
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 44030 $
 */
public class Dependency {
  
  /** The source package */
  protected Package m_sourcePackage;
  
  /** The target package (wrapped in a PackageConstraint) */
  protected PackageConstraint m_targetPackage;
  
  /**
   * Construct a new Dependency from a supplied source package and
   * PackageConstraint containing the target package.
   * 
   * @param source the source package.
   * @param target the target package (wrapped in a PackageConstraint).
   */
  public Dependency(Package source, PackageConstraint target) {
    m_sourcePackage = source;
    m_targetPackage = target;
  }
  
  /**
   * Set the source package.
   * 
   * @param source the source package.
   */
  public void setSource(Package source) {
    m_sourcePackage = source;
  }
  
  /**
   * Get the source package.
   * 
   * @return the source package.
   */
  public Package getSource() {
    return m_sourcePackage;
  }
  
  /**
   * Set the target package constraint.
   * 
   * @param target the target package (wrapped in a PackageConstraint).
   */
  public void setTarget(PackageConstraint target) {
    m_targetPackage = target;
  }
  
  /**
   * Get the target package constraint.
   * 
   * @return the target package (wrapped in a PackageConstraint).
   */
  public PackageConstraint getTarget() {
    return m_targetPackage;
  }
  
  public String toString() {
    return m_sourcePackage.toString() + " --> " + m_targetPackage.toString();
  }
}
