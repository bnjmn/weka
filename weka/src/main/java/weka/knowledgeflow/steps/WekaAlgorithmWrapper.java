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
 *    WekaAlgorithmWrapper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Utils;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * A step that wraps a class of standard Weka algorithm (e.g. filter,
 * classifier, clusterer etc.)
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class WekaAlgorithmWrapper extends BaseStep implements
  Serializable {

  private static final long serialVersionUID = -1013404060247467085L;

  /** Icon path to the specific icon for the wrapped algorithim */
  protected String m_iconPath;

  /**
   * Icon path to the default icon at the package level - e.g.
   * weka.classifiers.rules
   */
  protected String m_defaultPackageIconPath;

  /**
   * Icon path to the default icon for the type of wrapped algorithm - e.g.
   * Classifier, Loader etc.
   */
  protected String m_defaultIconPath;

  /** The wrapped algorithm */
  protected Object m_wrappedAlgorithm;

  /**
   * Get global "help" info. Returns the global info of the wrapped algorithm
   *
   * @return global "help" info
   */
  @Override
  public String globalInfo() {
    if (getWrappedAlgorithm() != null) {
      return Utils.getGlobalInfo(getWrappedAlgorithm(), false);
    }
    return super.globalInfo();
  }

  /**
   * Get the wrapped algorithm
   *
   * @return the wrapped algorithm
   */
  @NotPersistable
  @ProgrammaticProperty
  public Object getWrappedAlgorithm() {
    return m_wrappedAlgorithm;
  }

  /**
   * Set the wrapped algorithm
   *
   * @param algo the algorithm to wrao
   */
  public void setWrappedAlgorithm(Object algo) {
    m_wrappedAlgorithm = algo;

    String className = algo.getClass().getCanonicalName();
    String name = className.substring(className.lastIndexOf(".") + 1);
    String packageName = className.substring(0, className.lastIndexOf("."));

    // preserve the existing name if already set (i.e. the name property might
    // get set first by the flow loading process before setWrappedAlgorithm()
    // is invoked
    Annotation stepA = this.getClass().getAnnotation(KFStep.class);
    if (getName() == null || getName().length() == 0
      || (stepA != null && getName().equals(((KFStep) stepA).name()))) {
      setName(name);
    }

    m_defaultPackageIconPath = StepVisual.BASE_ICON_PATH + packageName + ".gif";
    m_iconPath = StepVisual.BASE_ICON_PATH + name + ".gif";
  }

  /**
   * Get the path to the icon for this wrapped algorithm
   *
   * @return the path to the icon
   */
  public String getIconPath() {
    return m_iconPath;
  }

  /**
   * Get the default icon at the package level for this type of wrapped
   * algorithm - e.g. weka.classifiers.meta
   * 
   * @return the default icon at the package level
   */
  public String getDefaultPackageLevelIconPath() {
    return m_defaultPackageIconPath;
  }

  /**
   * Get the default icon for this type of wrapped algorithm (i.e. generic
   * Loader, Saver etc.
   * 
   * @return the default icon for this wrapped algorithm
   */
  public String getDefaultIconPath() {
    return m_defaultIconPath;
  }

  /**
   * Get the class of the algorithm being wrapped
   * 
   * @return the class of the algorithm being wrapped
   */
  public abstract Class getWrappedAlgorithmClass();
}
