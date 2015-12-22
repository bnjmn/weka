package weka.gui.knowledgeflow;

import java.beans.Beans;
import java.io.Serializable;
import java.lang.annotation.Annotation;

import javax.swing.Icon;

import weka.core.Utils;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.KFStep;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

public class StepTreeLeafDetails implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6347861816716877761L;

  /** Class of the step stored at this leaf */
  protected Class m_stepClazz;

  /** The name of the algorithm wrapped by a WekaAlgorithmWrapper step */
  protected String m_wrappedWekaAlgoName = "";

  /**
   * the label (usually derived from the qualified name or wrapped algorithm)
   * for the leaf
   */
  protected String m_leafLabel = "";

  /** icon to display at the leaf (scaled appropriately) */
  protected transient Icon m_scaledIcon = null;

  /** tool tip text to display */
  protected String m_toolTipText = null;

  /** If a tool tip text is set, whether to show it or not */
  protected boolean m_showTipText = true;

  public StepTreeLeafDetails(Object step) {
    this(step, true);
  }

  public StepTreeLeafDetails(Object step, boolean showTipText) {
    m_stepClazz = step.getClass();

    Annotation[] annotations = m_stepClazz.getAnnotations();
    for (Annotation a : annotations) {
      if (a instanceof KFStep) {
        m_leafLabel = ((KFStep) a).name();
        if (showTipText) {
          m_toolTipText = ((KFStep) a).toolTipText();
        }
        break;
      }
    }

    if (step instanceof Step) {
      m_leafLabel = ((Step) step).getName();
    }

    if (step instanceof WekaAlgorithmWrapper) {
      m_wrappedWekaAlgoName =
        ((WekaAlgorithmWrapper) step).getWrappedAlgorithm().getClass()
          .getCanonicalName();
    }

    if (showTipText) {
      String globalInfo = Utils.getGlobalInfo(step, false);
      if (globalInfo != null) {
        m_toolTipText = globalInfo;
      }
    }

    m_scaledIcon =
      StepVisual.scaleIcon(StepVisual.iconForStep((Step) step), 0.33);
  }

  /**
   * Set whether to show tip text or not
   *
   * @param show true to show tip text
   */
  public void setShowTipTexts(boolean show) {
    m_showTipText = show;
  }

  /**
   * Get the tool tip for this leaf
   * 
   * @return the tool tip
   */
  public String getToolTipText() {
    return m_showTipText ? m_toolTipText : null;
  }

  /**
   * Returns the leaf label
   * 
   * @return the leaf label
   */
  @Override
  public String toString() {
    return m_leafLabel;
  }

  /**
   * Gets the icon for this bean
   * 
   * @return the icon for this bean
   */
  protected Icon getIcon() {
    return m_scaledIcon;
  }

  /**
   * Returns true if this leaf represents a wrapped Weka algorithm (i.e. filter,
   * classifier, clusterer etc.).
   * 
   * @return true if this leaf represents a wrapped algorithm
   */
  public boolean isWrappedAlgorithm() {
    return m_wrappedWekaAlgoName != null && m_wrappedWekaAlgoName.length() > 0;
  }

  /**
   * Instantiate the step at this leaf and return it wrapped in a StepVisual
   * 
   * @return a StepVisual instance wrapping a copy of the step at this leaf
   * @throws Exception if a problem occurs
   */
  public StepVisual instantiateStep() throws Exception {
    Step step = null;

    step =
      (Step) Beans.instantiate(this.getClass().getClassLoader(),
        m_stepClazz.getCanonicalName());
    StepManagerImpl manager = new StepManagerImpl(step);

    if (step instanceof WekaAlgorithmWrapper) {
      Object algo =
        Beans.instantiate(this.getClass().getClassLoader(),
          m_wrappedWekaAlgoName);
      ((WekaAlgorithmWrapper) step).setWrappedAlgorithm(algo);
    }

    StepVisual visual = StepVisual.createVisual(manager);

    return visual;
  }
}
