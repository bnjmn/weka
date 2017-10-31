package weka.gui.knowledgeflow;

import weka.core.Instances;
import weka.core.JRILoader;
import weka.gui.AbstractPerspective;
import weka.gui.Logger;
import weka.gui.PerspectiveInfo;
import weka.gui.WorkbenchDefaults;
import weka.gui.beans.*;
import weka.gui.beans.KnowledgeFlowApp;
import weka.knowledgeflow.KFDefaults;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@PerspectiveInfo(ID = "r_console", title = "R Console",
  toolTipText = "Interactive R console, and visualizations using JavaGD",
  iconPath = "weka/gui/knowledgeflow/icons/Rlogo_small.png")
public class RConsolePerspective extends AbstractPerspective {

  /** For serialization */
  private static final long serialVersionUID = -6801483427965663754L;

  /** Try and load R */
  static {
    try {
      JRILoader.load();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /** The console panel */
  protected Object m_consoleP;

  /**
   * Constructor
   */
  public RConsolePerspective() {
    setLayout(new BorderLayout());
  }

  @Override
  public void instantiationComplete() {
    try {
      Class<?> p = Class.forName("weka.gui.beans.JavaGDConsolePanel");
      if (getMainApplication().getApplicationID()
        .equals(WorkbenchDefaults.APP_ID)) {
        m_consoleP = p.newInstance();
      } else {
        Constructor c = p.getConstructor(Boolean.TYPE);

        // must be running in the KnowledgeFlow
        m_consoleP = c.newInstance(true);
      }
      add((JComponent) m_consoleP, BorderLayout.CENTER);
    } catch (Exception ex) {
      getMainApplication().showErrorDialog(ex);
    }
  }

  /**
   * We accept instances, and push them to R as a data frame called "rdata"
   * 
   * @return true
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  @Override
  public boolean requiresLog() {
    return getMainApplication().getApplicationID()
      .equals(WorkbenchDefaults.APP_ID);
  }

  @Override
  public void setLog(Logger newLog) {
    if (newLog instanceof JComponent && m_consoleP != null) {
      try {
        Method m =
          m_consoleP.getClass().getDeclaredMethod("setLogger", Logger.class);

        m.invoke(m_consoleP, newLog);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Accept instances and push to R (as a data frame called "rdata")
   *
   * @param instances the instance to push to R
   */
  @Override
  public void setInstances(Instances instances) {
    try {
      if (instances != null) {
        Method m = m_consoleP.getClass().getDeclaredMethod("pushInstancesToR",
          Instances.class);

        m.invoke(m_consoleP, instances);

      }
    } catch (Exception ex) {
      getMainApplication().showErrorDialog(ex);
    }
  }
}
