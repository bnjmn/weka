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
 * SQLViewerPerspective.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.knowledgeflow;

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Perspective that wraps the {@code SQLViewer) component
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = SQLViewerPerspective.SQLDefaults.ID,
  title = "SQL Viewer", toolTipText = "Explore database tables with SQL",
  iconPath = "weka/gui/knowledgeflow/icons/database.png")
public class SQLViewerPerspective extends AbstractPerspective {

  private static final long serialVersionUID = -4771310190331379801L;

  /** The wrapped SQLViewer */
  protected SqlViewer m_viewer;

  /** Button for creating a new flow layout with a configured DBLoader step */
  protected JButton m_newFlowBut;

  /** Reference to tne main knowledge flow perspective */
  protected MainKFPerspective m_mainKFPerspective;

  /** Panel for holding buttons */
  protected JPanel m_buttonHolder;

  /**
   * Constructor
   */
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

  /**
   * Set the main application. Gives other perspectives access to information
   * provided by the main application
   *
   * @param application the main application
   */
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

  /**
   * Create a new flow in the main knowledge flow perspective with a configured
   * database loader step
   */
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

  /**
   * Default settings for the SQLViewer perspective
   */
  protected static class SQLDefaults extends Defaults {
    public static final String ID = "sqlviewer";

    private static final long serialVersionUID = 5907476861935295960L;

    public SQLDefaults() {
      super(ID);
    }
  }
}
