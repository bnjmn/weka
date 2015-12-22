package weka.gui.knowledgeflow;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import weka.core.Defaults;
import weka.core.converters.DatabaseLoader;
import weka.gui.AbstractPerspective;
import weka.gui.GUIApplication;
import weka.gui.PerspectiveInfo;
import weka.gui.sql.SqlViewer;
import weka.gui.sql.event.ConnectionEvent;
import weka.gui.sql.event.ConnectionListener;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Loader;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = SQLViewerPerspective.SQLDefaults.ID,
  title = "SQL Viewer", toolTipText = "Explore database tables with SQL",
  iconPath = "weka/gui/knowledgeflow/icons/database.png")
public class SQLViewerPerspective extends AbstractPerspective {

  protected SqlViewer m_viewer;
  protected JButton m_newFlowBut;
  protected MainKFPerspective m_mainKFPerspective;
  protected JPanel m_buttonHolder;

  public SQLViewerPerspective() {
    setLayout(new BorderLayout());
    m_viewer = new SqlViewer(null);
    add(m_viewer, BorderLayout.CENTER);

    m_newFlowBut = new JButton("New Flow");
    m_newFlowBut.setToolTipText("Set up a new Knowledge Flow with the "
      + "current connection and query");
    m_buttonHolder = new JPanel();
    m_buttonHolder.add(m_newFlowBut);
    add(m_buttonHolder, BorderLayout.SOUTH);

    m_newFlowBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainKFPerspective != null) {
          newFlow();
        }
      }
    });
    m_newFlowBut.setEnabled(false);

    m_viewer.addConnectionListener(new ConnectionListener() {

      @Override
      public void connectionChange(ConnectionEvent evt) {
        if (evt.getType() == ConnectionEvent.DISCONNECT) {
          m_newFlowBut.setEnabled(false);
        } else {
          m_newFlowBut.setEnabled(true);
        }
      }
    });
  }

  @Override
  public void setMainApplication(GUIApplication application) {
    super.setMainApplication(application);

    m_mainKFPerspective =
      (MainKFPerspective) m_mainApplication.getPerspectiveManager()
        .getPerspective(KFDefaults.APP_ID);
    if (m_mainKFPerspective == null) {
      remove(m_buttonHolder);
    }
  }

  protected void newFlow() {
    m_newFlowBut.setEnabled(false);

    String user = m_viewer.getUser();
    String password = m_viewer.getPassword();
    String uRL = m_viewer.getURL();
    String query = m_viewer.getQuery();

    if (query == null) {
      query = "";
    }

    try {
      DatabaseLoader dbl = new DatabaseLoader();
      dbl.setUser(user);
      dbl.setPassword(password);
      dbl.setUrl(uRL);
      dbl.setQuery(query);

      Loader loaderStep = new Loader();
      loaderStep.setLoader(dbl);

      StepManagerImpl manager = new StepManagerImpl(loaderStep);
      m_mainKFPerspective.addTab("DBSource");
      m_mainKFPerspective.getCurrentLayout().addStep(manager, 50, 50);
      m_mainApplication.getPerspectiveManager().setActivePerspective(
        KFDefaults.APP_ID);

      m_newFlowBut.setEnabled(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      m_mainApplication.showErrorDialog(ex);
    }
  }

  protected static class SQLDefaults extends Defaults {
    public static final String ID = "sqlviewer";

    public SQLDefaults() {
      super(ID);
    }
  }
}
