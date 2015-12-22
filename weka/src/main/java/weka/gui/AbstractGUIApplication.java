package weka.gui;

import weka.knowledgeflow.LogHandler;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/**
 * Base class for GUI applications in Weka
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractGUIApplication extends JPanel
  implements GUIApplication {

  private static final long serialVersionUID = -2116770422043462730L;

  /** Manages perspectives and provides the perspectives toolbar */
  protected PerspectiveManager m_perspectiveManager;

  public AbstractGUIApplication() {
    this(true);
  }

  public AbstractGUIApplication(boolean layoutComponent,
    String[] allowedPerspectiveClassPrefixes,
    String[] disallowedPerspectiveClassPrefixes) {

    m_perspectiveManager = new PerspectiveManager(this,
      allowedPerspectiveClassPrefixes, disallowedPerspectiveClassPrefixes);
    m_perspectiveManager.setMainApplicationForAllPerspectives();
    if (layoutComponent) {
      setLayout(new BorderLayout());
      add(m_perspectiveManager, BorderLayout.CENTER);
      if (m_perspectiveManager.perspectiveToolBarIsVisible()) {
        add(m_perspectiveManager.getPerspectiveToolBar(), BorderLayout.NORTH);
      }
    }
  }

  public AbstractGUIApplication(boolean layoutComponent,
    String... allowedPerspectiveClassPrefixes) {

    this(layoutComponent, allowedPerspectiveClassPrefixes, new String[0]);
  }

  @Override
  public PerspectiveManager getPerspectiveManager() {
    return m_perspectiveManager;
  }

  @Override
  public boolean isPerspectivesToolBarVisible() {
    return m_perspectiveManager.perspectiveToolBarIsVisible();
  }

  @Override
  public void hidePerspectivesToolBar() {
    if (isPerspectivesToolBarVisible()) {
      m_perspectiveManager.setPerspectiveToolBarIsVisible(false);
      remove(m_perspectiveManager.getPerspectiveToolBar());
    }
  }

  @Override
  public void showPerspectivesToolBar() {
    if (!isPerspectivesToolBarVisible()) {
      m_perspectiveManager.setPerspectiveToolBarIsVisible(true);
      add(m_perspectiveManager.getPerspectiveToolBar(), BorderLayout.NORTH);
    }
  }

  @Override
  public void settingsChanged() {
    // no-op. Subclasses to override if necessary
  }

  @Override
  public void notifyIsDirty() {
    firePropertyChange("PROP_DIRTY", null, null);
  }

  @Override
  public void showMenuBar(JFrame topLevelAncestor) {
    m_perspectiveManager.showMenuBar(topLevelAncestor);
  }

  @Override
  public void showErrorDialog(Exception cause) {
    String stackTrace = LogHandler.stackTraceToString(cause);

    Object[] options = null;

    if (stackTrace != null && stackTrace.length() > 0) {
      options = new Object[2];
      options[0] = "OK";
      options[1] = "Show error";
    } else {
      options = new Object[1];
      options[0] = "OK";
    }
    int result = JOptionPane.showOptionDialog(this,
      "An error has occurred: " + cause.getMessage(), getApplicationName(),
      JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options,
      options[0]);

    if (result == 1) {
      JTextArea jt = new JTextArea(stackTrace, 10, 40);
      JOptionPane.showMessageDialog(this, new JScrollPane(jt),
        getApplicationName(), JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public void showInfoDialog(Object information, String title,
    boolean isWarning) {
    JOptionPane.showMessageDialog(this, information, title, isWarning
      ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void revalidate() {
    if (getTopLevelAncestor() != null) {
      getTopLevelAncestor().revalidate();
      getTopLevelAncestor().repaint();
    } else {
      super.revalidate();
    }
    repaint();
  }
}
