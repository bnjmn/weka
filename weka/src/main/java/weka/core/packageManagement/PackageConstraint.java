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
 *    PackageConstraint.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core.packageManagement;

/**
 * Abstract base class for package constraints. An example implementation
 * might be to encapsulate a constraint with respect to a version
 * number. Checking against a target in this case would
 * typically assume the same package for both this and the target.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 44030 $
 */
public abstract class PackageConstraint {
  
  protected Package m_thePackage;
  
  /**
   * Set the package that this constraint applies to.
   * 
   * @param p the package that this constraint applies to.
   */
  public void setPackage(Package p) {
    m_thePackage = p;
  }
  
  /**
   * Get the package that this constraint applies to.
   * 
   * @return the Package that this constraint applies to.
   */
  public Package getPackage() {
    return m_thePackage;
  }
  
  /**
   * Check the target package against the constraint embodied
   * in this PackageConstraint.
   * 
   * @param target a package to check with respect to the
   * encapsulated package and the constraint.
   * 
   * @return true if the constraint is met by the target package
   * with respect to the encapsulated package + constraint.
   * @throws Exception if the constraint can't be checked for some
   * reason.
   */
  public abstract boolean checkConstraint(Package target) throws Exception;
  
  /**
   * Check the target package constraint against the constraint embodied
   * in this package constraint. Returns either the package constraint that
   * covers both this and the target constraint, or null if this and the target
   * are incompatible.
   * 
   * @param target the package constraint to compare against
   * @return a package constraint that covers this and the supplied constraint,
   * or null if they are incompatible.
   */
  public abstract PackageConstraint checkConstraint(PackageConstraint target)
    throws Exception;
}
