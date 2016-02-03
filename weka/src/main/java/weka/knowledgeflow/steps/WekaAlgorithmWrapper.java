package weka.knowledgeflow.steps;

import weka.core.Utils;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;

import java.io.Serializable;

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

  protected Object m_wrappedAlgorithm;

  @Override
  public String globalInfo() {
    if (getWrappedAlgorithm() != null) {
      return Utils.getGlobalInfo(getWrappedAlgorithm(), false);
    }
    return super.globalInfo();
  }

  @NotPersistable
  @ProgrammaticProperty
  public Object getWrappedAlgorithm() {
    return m_wrappedAlgorithm;
  }

  public void setWrappedAlgorithm(Object algo) {
    m_wrappedAlgorithm = algo;

    String className = algo.getClass().getCanonicalName();
    String name = className.substring(className.lastIndexOf(".") + 1);
    String packageName = className.substring(0, className.lastIndexOf("."));
    setName(name);

    m_defaultPackageIconPath = StepVisual.BASE_ICON_PATH + packageName + ".gif";
    m_iconPath = StepVisual.BASE_ICON_PATH + name + ".gif";
  }

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
