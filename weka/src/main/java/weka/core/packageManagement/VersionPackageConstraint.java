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
 *    VersionPackageConstraint.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core.packageManagement;

/**
 * Concrete implementation of PackageConstraint that encapsulates constraints
 * related to version numbers. Handles equality = and open-ended inequalities
 * (e.g. > x, < x, >= x, <= x).
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 51469 $
 */
public class VersionPackageConstraint extends PackageConstraint {

  /** The meta data key for the version number */
  public static String VERSION_KEY = "Version";

  /** Enumeration encapsulating version comparison operations */
  public enum VersionComparison {
    EQUAL("=") {
      @Override
      boolean compatibleWith(VersionComparison v) {
        if (v == VersionComparison.EQUAL) {
          return true;
        }
        return false;
      }
    },
    GREATERTHAN(">") {
      @Override
      boolean compatibleWith(VersionComparison v) {
        if (v == VersionComparison.GREATERTHAN) {
          return true;
        }
        return false;
      }
    },
    GREATERTHANOREQUAL(">=") {
      @Override
      boolean compatibleWith(VersionComparison v) {
        if (v == VersionComparison.LESSTHAN
            || v == VersionComparison.LESSTHANOREQUAL) {
          return false;
        }
        return true;
      }
    },
    LESSTHAN("<") {
      @Override
      boolean compatibleWith(VersionComparison v) {
        if (v == VersionComparison.LESSTHAN) {
          return true;
        }
        return false;
      }
    },
    LESSTHANOREQUAL("<=") {
      @Override
      boolean compatibleWith(VersionComparison v) {
        if (v == VersionComparison.GREATERTHAN
            || v == VersionComparison.GREATERTHANOREQUAL) {
          return false;
        }
        return true;
      }
    };

    private final String m_stringVal;

    VersionComparison(String name) {
      m_stringVal = name;
    }

    abstract boolean compatibleWith(VersionComparison v);

    @Override
    public String toString() {
      return m_stringVal;
    }
  }

  /** The comparison operator for this constraint */
  protected VersionComparison m_constraint = null;

  /**
   * Returns a VersionComparison equivalent to the supplied String operator.
   * 
   * @param compOpp the comparison operator as a string.
   * @return a VersionComparison object.
   */
  protected static VersionComparison getVersionComparison(String compOpp) {
    for (VersionComparison v : VersionComparison.values()) {
      if (v.toString().equalsIgnoreCase(compOpp)) {
        return v;
      }
    }
    return null;
  }

  /**
   * Parses a version number and returns major, minor and revision numbers in an
   * array of integers.
   * 
   * @param version the version number as a string.
   * @return an array of integers containing the major, minor and revision
   *         numbers.
   */
  protected static int[] parseVersion(String version) {
    int major = 0;
    int minor = 0;
    int revision = 0;
    int point = 0;
    int[] majMinRev = new int[4];

    try {
      String tmpStr = version;
      tmpStr = tmpStr.toLowerCase().replace("-snapshot", "");
      tmpStr = tmpStr.replace('-', '.');
      if (tmpStr.indexOf(".") > -1) {
        major = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
        tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);
        if (tmpStr.indexOf(".") > -1) {
          minor = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
          tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);

          if (tmpStr.indexOf(".") > 0) {
            revision = Integer
                .parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
            tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);

            if (!tmpStr.equals("")) {
              point = Integer.parseInt(tmpStr);
            } else {
              point = 0;
            }
          } else {
            if (!tmpStr.equals("")) {
              revision = Integer.parseInt(tmpStr);
            } else {
              revision = 0;
            }
          }
        } else {
          if (!tmpStr.equals("")) {
            minor = Integer.parseInt(tmpStr);
          } else {
            minor = 0;
          }
        }
      } else {
        if (!tmpStr.equals("")) {
          major = Integer.parseInt(tmpStr);
        } else {
          major = 0;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      major = -1;
      minor = -1;
      revision = -1;
    } finally {
      majMinRev[0] = major;
      majMinRev[1] = minor;
      majMinRev[2] = revision;
    }

    return majMinRev;
  }

  /**
   * Evaluates the supplied constraint with respect to two supplied version
   * numbers as strings.
   * 
   * @param version1 String containing version number 1
   * @param constraint the constraint comparison to use
   * @param version2 String containing version number 2
   * @return true if version 1 is compatible with version two with respect to
   *         the constraint.
   */
  protected static boolean checkConstraint(String version1,
      VersionComparison constraint, String version2) {

    VersionComparison c = compare(version1, version2);

    return constraint.compatibleWith(c);
  }

  /**
   * Returns a VersionComparison that represents the comparison between the
   * supplied version 1 and version 2.
   * 
   * @param version1 String containing version number 1.
   * @param version2 String containing version number 2.
   * @return a VersionComparison object.
   */
  public static VersionComparison compare(String version1, String version2) {

    // parse both of the versions
    int[] majMinRev1 = VersionPackageConstraint.parseVersion(version1);
    int[] majMinRev2 = VersionPackageConstraint.parseVersion(version2);

    VersionComparison result;

    if (majMinRev1[0] < majMinRev2[0]) {
      result = VersionComparison.LESSTHAN;
    } else if (majMinRev1[0] == majMinRev2[0]) {
      if (majMinRev1[1] < majMinRev2[1]) {
        result = VersionComparison.LESSTHAN;
      } else if (majMinRev1[1] == majMinRev2[1]) {
        if (majMinRev1[2] < majMinRev2[2]) {
          result = VersionComparison.LESSTHAN;
        } else if (majMinRev1[2] == majMinRev2[2]) {
          if (majMinRev1[3] == majMinRev2[3]) {
            result = VersionComparison.EQUAL;
          } else {
            result = VersionComparison.GREATERTHAN;
          }
        } else {
          result = VersionComparison.GREATERTHAN;
        }
      } else {
        result = VersionComparison.GREATERTHAN;
      }
    } else {
      result = VersionComparison.GREATERTHAN;
    }

    return result;
  }

  public VersionPackageConstraint(Package p) {
    setPackage(p);
  }

  public void setVersionConstraint(VersionComparison c) {
    m_constraint = c;
  }

  public VersionComparison getVersionComparison() {
    return m_constraint;
  }

  public void setVersionConstraint(String constraint) {
    for (VersionComparison v : VersionComparison.values()) {
      if (v.toString().equalsIgnoreCase(constraint)) {
        m_constraint = v;
        break;
      }
    }
  }

  /**
   * Check the target package constraint against the constraint embodied in this
   * package constraint. Returns either the package constraint that covers both
   * this and the target constraint, or null if this and the target are
   * incompatible.
   * 
   * Try and return the *least* restrictive constraint that satisfies this and
   * the target.
   * 
   * @param target the package constraint to compare against
   * @return a package constraint that covers this and the supplied constraint,
   *         or null if they are incompatible.
   */
  @Override
  public PackageConstraint checkConstraint(PackageConstraint target)
      throws Exception {
    if (m_constraint == null) {
      throw new Exception(
          "[VersionPackageConstraint] No constraint has been set!");
    }

    // delegate to VersionRangePackageConstraint if necessary
    if (target instanceof VersionRangePackageConstraint) {
      return target.checkConstraint(this);
    }

    String targetVersion = target.getPackage()
        .getPackageMetaDataElement(VERSION_KEY).toString();
    String thisVersion = m_thePackage.getPackageMetaDataElement(VERSION_KEY)
        .toString();

    VersionComparison comp = compare(thisVersion, targetVersion);
    if (comp == VersionComparison.EQUAL) { // equal version numbers
      // return this if the constraints are the same for both this and target
      if (m_constraint == ((VersionPackageConstraint) target)
          .getVersionComparison()) {
        return this;
      } else if (m_constraint == VersionComparison.GREATERTHAN
          && (((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.GREATERTHAN || ((VersionPackageConstraint) target)
              .getVersionComparison() == VersionComparison.GREATERTHANOREQUAL)) {
        return this; // satisfies both
      } else if ((m_constraint == VersionComparison.GREATERTHANOREQUAL || m_constraint == VersionComparison.GREATERTHAN)
          && ((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.GREATERTHAN) {
        return target; // satisfies both
      }

      return null; // can't satisfy
    } else {

      // target constraint >/>=
      if (((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.GREATERTHAN
          || ((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.GREATERTHANOREQUAL) {
        if (m_constraint == VersionComparison.EQUAL
            || m_constraint == VersionComparison.GREATERTHAN
            || m_constraint == VersionComparison.GREATERTHANOREQUAL) {

          // return the higher of the two versions
          if (comp == VersionComparison.GREATERTHAN) {
            return this;
          } else {
            return target;
          }
        }

        return null; // can't satisfy

        // target constraint </<=
      } else if (((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.LESSTHAN
          || ((VersionPackageConstraint) target).getVersionComparison() == VersionComparison.LESSTHANOREQUAL) {
        if (m_constraint == VersionComparison.EQUAL
            || m_constraint == VersionComparison.LESSTHAN
            || m_constraint == VersionComparison.LESSTHANOREQUAL) {

          // return the lower of the two versions
          if (comp == VersionComparison.GREATERTHAN) {
            return target; // satisfies both
          } else {
            return this;
          }
        }

        return null; // can't satisfy
      }

      return null; // can't satisfy

      // Could also be compatible in the case where
      // our constraint is < and target is > (but would need to implement a
      // y < x < z type of version constraint
    }
  }

  /**
   * Check the target package against the constraint embodied in this
   * PackageConstraint.
   * 
   * @param target a package to check with respect to the encapsulated package
   *          and the constraint.
   * 
   * @return true if the constraint is met by the target package with respect to
   *         the encapsulated package + constraint.
   * @throws Exception if the constraint can't be checked for some reason.
   */
  @Override
  public boolean checkConstraint(Package target) throws Exception {
    if (m_constraint == null) {
      throw new Exception(
          "[VersionPackageConstraint] No constraint has been set!");
    }

    String targetVersion = target.getPackageMetaDataElement(VERSION_KEY)
        .toString();
    String thisVersion = m_thePackage.getPackageMetaDataElement(VERSION_KEY)
        .toString();

    return checkConstraint(targetVersion, m_constraint, thisVersion);
  }

  @Override
  public String toString() {
    String result = m_thePackage.getPackageMetaDataElement("PackageName")
        .toString()
        + " ("
        + m_constraint
        + m_thePackage.getPackageMetaDataElement("Version").toString() + ")";
    return result;
  }
}
