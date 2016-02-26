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
 *    KnowledgeFlowRemotePerspective.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.mortbay.jetty.security.Password;
import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.WekaPackageManager;
import weka.experiment.TaskStatusInfo;
import weka.gui.AbstractPerspective;
import weka.gui.HostPanel;
import weka.gui.ListSelectorDialog;
import weka.gui.Perspective;
import weka.gui.PerspectiveInfo;
import weka.gui.SchedulePanel;
import weka.gui.beans.LogPanel;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.DataCollector;
import weka.knowledgeflow.steps.Step;
import weka.server.ExecuteTaskServlet;
import weka.server.GetScheduleServlet;
import weka.server.GetTaskListServlet;
import weka.server.GetTaskResultServlet;
import weka.server.GetTaskStatusServlet;
import weka.server.JSONProtocol;
import weka.server.NamedTask;
import weka.server.Schedule;
import weka.server.WekaServer;
import weka.server.WekaServlet;
import weka.server.WekaTaskMap;
import weka.server.knowledgeFlow.FlowRunnerRemote;
import weka.server.knowledgeFlow.ScheduledNamedKnowledgeFlowTask;
import weka.server.knowledgeFlow.UnscheduledNamedKnowledgeFlowTask;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Perspective for configuring remote execution of Knowledge Flow processes
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = "weka.gui.knowledgeflow.knowledgeflowremoteperspective",
  title = "Remote host scheduler",
  toolTipText = "Schedule knowledge flows to run on remote hosts",
  iconPath = "weka/gui/knowledgeflow/icons/calendar.png")
public class KnowledgeFlowRemotePerspective extends AbstractPerspective {

  private static final long serialVersionUID = -8715053418611823815L;

  /** Panel for host/port details */
  protected HostPanel m_hostPanel;

  /** Panel containing scheduling stuff */
  protected SchedulePanel m_schedulePanel;

  /** Reference to the main kf perspective */
  protected MainKFPerspective m_mainPerspective;

  /** Panel for monitoring remote tasks */
  protected MonitorPanel m_monitorPanel;

  /** Panel for launching tasks remotely */
  protected FlowLauncher m_launcher;

  /** Username for authenticating with the server (if necessary) */
  protected String m_username;

  /** Password for authenticating with the server (if necessary) */
  protected String m_password;

  /**
   * Construct the perspective
   */
  public KnowledgeFlowRemotePerspective() {
    String wekaServerPasswordPath =
      WekaPackageManager.WEKA_HOME.toString() + File.separator + "server"
        + File.separator + "weka.pwd";
    File wekaServerPasswordFile = new File(wekaServerPasswordPath);
    boolean useAuth = wekaServerPasswordFile.exists();

    if (useAuth) {
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(wekaServerPasswordFile));
        String line = null;
        while ((line = br.readLine()) != null) {
          // not a comment character, then assume its the data
          if (!line.startsWith("#")) {

            String[] parts = line.split(":");
            if (parts.length > 3 || parts.length < 2) {
              continue;
            }
            m_username = parts[0].trim();
            m_password = parts[1].trim();
            if (parts.length == 3 && parts[1].trim().startsWith("OBF")) {
              m_password = m_password + ":" + parts[2];
              String deObbs = Password.deobfuscate(m_password);
              m_password = deObbs;
            }
            break;
          }
        }
      } catch (Exception ex) {
        System.err.println("[SchedulePerspective] Error reading "
          + "password file: " + ex.getMessage());
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (Exception e) {
          }
        }
      }
    }

    setLayout(new BorderLayout());
    m_hostPanel = new HostPanel(m_username, m_password);
    m_schedulePanel = new SchedulePanel();
    m_monitorPanel = new MonitorPanel(m_username, m_password);

    JPanel holderPanel = new JPanel();
    holderPanel.setLayout(new BorderLayout());
    holderPanel.add(m_hostPanel, BorderLayout.NORTH);

    JPanel scheduleHolder = new JPanel(new BorderLayout());
    scheduleHolder.add(m_schedulePanel, BorderLayout.NORTH);
    m_launcher = new FlowLauncher(m_username, m_password, m_monitorPanel);
    scheduleHolder.add(m_launcher, BorderLayout.SOUTH);

    holderPanel.add(scheduleHolder, BorderLayout.SOUTH);

    add(holderPanel, BorderLayout.NORTH);
    add(m_monitorPanel, BorderLayout.CENTER);
  }

  /**
   * Called when the perspective and application have completed initialization
   */
  @Override
  public void instantiationComplete() {
    Perspective mainP =
      getMainApplication().getPerspectiveManager().getPerspective(
        KFDefaults.MAIN_PERSPECTIVE_ID);

    if (mainP == null) {
      getMainApplication()
        .showErrorDialog(
          new Exception(
            "The main perspective for "
              + "the Knowledge Flow does not seem to be available in this application!"));
    } else {
      m_mainPerspective = (MainKFPerspective) mainP;
    }

    Settings s = getMainApplication().getApplicationSettings();
    String defHost =
      s.getSetting(RemotePerspectiveDefaults.ID,
        RemotePerspectiveDefaults.HOST_KEY, RemotePerspectiveDefaults.HOST,
        Environment.getSystemWide());
    String defPort =
      s.getSetting(RemotePerspectiveDefaults.ID,
        RemotePerspectiveDefaults.PORT_KEY, RemotePerspectiveDefaults.PORT,
        Environment.getSystemWide());
    m_hostPanel.setHostName(defHost);
    m_hostPanel.setPort(defPort);

    String defMonitor =
      s.getSetting(RemotePerspectiveDefaults.ID,
        RemotePerspectiveDefaults.MONITORING_INTERVAL_KEY,
        RemotePerspectiveDefaults.MONITORING_INTERVAL,
        Environment.getSystemWide());
    m_monitorPanel.setMonitorInterval(defMonitor);
  }

  /**
   * Inner class providing a panel for monitoring flows running on the server.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision: 10248 $
   */
  protected class MonitorPanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 6556408319133214173L;

    /**
     * A Closeable tab widget
     */
    private class CloseableTabTitle extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = -871710265588121352L;

      private final JTabbedPane m_enclosingPane;

      private Thread m_monitorThread;

      private JLabel m_tabLabel;
      private TabButton m_tabButton;

      /**
       * Constructor
       *
       * @param pane the enclosing JTabbedPane
       */
      public CloseableTabTitle(final JTabbedPane pane) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        m_enclosingPane = pane;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // read the title from the JTabbedPane
        m_tabLabel = new JLabel() {

          @Override
          public String getText() {
            int index =
              m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
            if (index >= 0) {
              return m_enclosingPane.getTitleAt(index);
            }
            return null;
          }
        };

        add(m_tabLabel);
        m_tabLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        m_tabButton = new TabButton();
        add(m_tabButton);

      }

      /**
       * Set the thread that is monitoring this tab's flow
       *
       * @param monitorThread the monitoring thread
       */
      public void setMonitorThread(Thread monitorThread) {
        m_monitorThread = monitorThread;
      }

      /**
       * Make the tab title bold or not
       *
       * @param bold true if the title should be bold
       */
      public void setBold(boolean bold) {
        m_tabLabel.setEnabled(bold);
      }

      /**
       * Set the enabled status of the close part of the tab title
       * 
       * @param enabled true if the clickable close part of the title should be
       *          enabled
       */
      public void setButtonEnabled(boolean enabled) {
        m_tabButton.setEnabled(enabled);
      }

      /**
       * Exension of {@code JButton} for making a close cross in the tab title
       */
      private class TabButton extends JButton implements ActionListener {

        /**
         * For serialization
         */
        private static final long serialVersionUID = 8050674024546615663L;

        /**
         * Constructor
         */
        public TabButton() {
          int size = 17;
          setPreferredSize(new Dimension(size, size));
          setToolTipText("close this tab");
          // Make the button looks the same for all Laf's
          setUI(new BasicButtonUI());
          // Make it transparent
          setContentAreaFilled(false);
          // No need to be focusable
          setFocusable(false);
          setBorder(BorderFactory.createEtchedBorder());
          setBorderPainted(false);
          // Making nice rollover effect
          // we use the same listener for all buttons
          addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
              Component component = e.getComponent();

              if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
              }
            }

            @Override
            public void mouseExited(MouseEvent e) {
              Component component = e.getComponent();
              if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
              }
            }
          });
          setRolloverEnabled(true);
          // Close the proper tab by clicking the button
          addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          int i = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
          m_enclosingPane.remove(i);
          if (m_monitorThread != null) {
            m_monitorThread.interrupt();
            m_monitorThread = null;
          }
        }

        // we don't want to update UI for this button
        @Override
        public void updateUI() {
        }

        // paint the cross
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          Graphics2D g2 = (Graphics2D) g.create();
          // shift the image for pressed buttons
          if (getModel().isPressed()) {
            g2.translate(1, 1);
          }
          g2.setStroke(new BasicStroke(2));
          g2.setColor(Color.BLACK);
          if (!isEnabled()) {
            g2.setColor(Color.GRAY);
          }
          if (getModel().isRollover()) {
            g2.setColor(Color.MAGENTA);
          }
          int delta = 6;
          g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta
            - 1);
          g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta
            - 1);
          g2.dispose();
        }
      }
    }

    /** Provides tabs - one for each flow being monitored */
    protected JTabbedPane m_monitorTabs = new JTabbedPane();

    /** Combo box listing tasks on the server */
    protected JComboBox m_tasksOnServer = new JComboBox();

    /** Button to refresh the task combo */
    protected JButton m_refreshTaskBut = new JButton("Refresh task list");

    /** Button to start monitoring a task (opens a new tab) */
    protected JButton m_monitorBut = new JButton("Monitor");

    /** Field for entering an interal at which to poll the server */
    protected JTextField m_monitorIntervalField = new JTextField(5);

    /** Current monitoring interval */
    protected int m_monitorInterval = 15; // 15 seconds

    protected String m_username = "";
    protected String m_password = "";

    /**
     * Construct a new MontitorPanel
     * 
     * @param username the username for authentication
     * @param password the password for authentication
     */
    public MonitorPanel(String username, String password) {
      m_username = username;
      m_password = password;
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Monitor tasks"));
      m_monitorIntervalField
        .setToolTipText("Task status monitoring interval (seconds)");
      m_monitorIntervalField.setText("" + m_monitorInterval);
      m_refreshTaskBut.setToolTipText("Fetch task list from server");
      m_monitorBut.setToolTipText("Start monitoring selected task");

      JPanel holder = new JPanel();
      holder.setLayout(new FlowLayout(FlowLayout.LEFT));
      holder.add(m_tasksOnServer);
      holder.add(m_refreshTaskBut);
      holder.add(m_monitorBut);
      holder.add(m_monitorIntervalField);
      m_monitorBut.setEnabled(false);
      m_monitorBut.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String toMonitor = m_tasksOnServer.getSelectedItem().toString();
          if (toMonitor != null && toMonitor.length() > 0) {
            int interval = m_monitorInterval;
            try {
              interval =
                Integer.parseInt(m_monitorIntervalField.getText().trim());
            } catch (NumberFormatException ex) {
            }
            m_monitorInterval = interval;
            monitorTask(toMonitor);
          }
        }
      });

      m_refreshTaskBut.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          boolean tasksOnServer = refreshTaskList();
          m_monitorBut.setEnabled(tasksOnServer);
        }
      });

      add(holder, BorderLayout.NORTH);
      add(m_monitorTabs, BorderLayout.CENTER);

      Dimension d = m_refreshTaskBut.getPreferredSize();
      m_tasksOnServer.setMinimumSize(d);
      m_tasksOnServer.setPreferredSize(d);
    }

    /**
     * Set the monitoring interval
     *
     * @param monitorInterval the interval to use
     */
    public void setMonitorInterval(String monitorInterval) {
      m_monitorIntervalField.setText(monitorInterval);
    }

    /**
     * Retrieve a result from the server
     *
     * @param taskID the ID of the task to get the result for
     * @return the TaskStatusInfo object encapsulating the result
     */
    @SuppressWarnings("unchecked")
    protected TaskStatusInfo getResultFromServer(String taskID) {
      PostMethod post = null;
      TaskStatusInfo resultInfo = null;

      try {
        String service =
          GetTaskResultServlet.CONTEXT_PATH + "/?name=" + taskID + "&"
            + JSONProtocol.JSON_CLIENT_KEY + "=Y";
        post = new PostMethod(constructURL(service));
        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client =
          WekaServer.ConnectionManager.getSingleton().createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        int result = client.executeMethod(post);
        if (result == 401) {
          System.err
            .println("Unable to retrieve task from server - authentication "
              + "required");
        } else {
          String responseS = post.getResponseBodyAsString();
          ObjectMapper mapper = JsonFactory.create();
          Map<String, Object> responseMap =
            mapper.readValue(responseS, Map.class);
          Object responseType = responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);
          if (responseType == null
            || responseType.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            System.err.println("Server returned an error: "
              + "while trying to retrieve task result for task: '"
              + taskID
              + "'. ("
              + (responseType == null ? "response was null" : responseMap
                .get(JSONProtocol.RESPONSE_MESSAGE_KEY)) + ")."
              + " Check logs on server.");
          } else {
            Map<String, Object> statusMap =
              (Map<String, Object>) responseMap
                .get(JSONProtocol.RESPONSE_PAYLOAD_KEY);
            resultInfo = JSONProtocol.jsonMapToStatusInfo(statusMap);
          }
        }
      } catch (Exception ex) {
        System.err
          .println("An error occurred while trying to retrieve task result from "
            + "server: " + ex.getMessage());
        ex.printStackTrace();
      } finally {
        if (post != null) {
          // Release current connection to the connection pool
          post.releaseConnection();
        }
      }

      return resultInfo;
    }

    /**
     * Retrieve scheduling information for the supplied task ID
     *
     * @param taskName the ID of the task to get the schedule for from the
     *          server
     *
     * @return the Schedule of the task
     */
    @SuppressWarnings("unchecked")
    protected Schedule getScheduleForTask(String taskName) {

      PostMethod post = null;
      Schedule sched = null;

      if (!m_hostPanel.hostOK()) {
        return null;
      }

      try {
        String service =
          GetScheduleServlet.CONTEXT_PATH + "/?name=" + taskName + "&"
            + JSONProtocol.JSON_CLIENT_KEY + "=Y";
        post = new PostMethod(constructURL(service));

        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client =
          WekaServer.ConnectionManager.getSingleton().createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        int result = client.executeMethod(post);
        if (result == 401) {
          JOptionPane
            .showMessageDialog(
              KnowledgeFlowRemotePerspective.this,
              "Unable to get schedule information from server - authentication required",
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          System.err
            .println("Unable to send task to get schedule info from server"
              + " - authentication required.\n");
        } else {
          String responseS = post.getResponseBodyAsString();
          ObjectMapper mapper = JsonFactory.create();
          Map<String, Object> responseMap =
            mapper.readValue(responseS, Map.class);
          Object responseType = responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);

          if (responseType == null
            || responseType.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            if (responseType != null
              && responseMap.get(JSONProtocol.RESPONSE_MESSAGE_KEY).toString()
                .contains("is not a scheduled task")) {
              // just return null
              return null;
            }
            System.err.println("Server returned an error: "
              + "while trying to retrieve schedule for task '"
              + taskName
              + "'. ("
              + (responseType == null ? "response was null" : responseMap
                .get(JSONProtocol.RESPONSE_MESSAGE_KEY)) + ")."
              + " Check logs on server.");
          } else {
            Map<String, Object> schedMap =
              (Map<String, Object>) responseMap
                .get(JSONProtocol.RESPONSE_PAYLOAD_KEY);
            sched = JSONProtocol.jsonMapToSchedule(schedMap);
          }
        }
      } catch (Exception ex) {
        JOptionPane
          .showMessageDialog(KnowledgeFlowRemotePerspective.this,
            "A problem occurred while trying to get schedule info from server : \n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
      } finally {
        if (post != null) {
          // Release current connection to the connection pool
          post.releaseConnection();
        }
      }

      return sched;
    }

    /**
     * Refresh the task combo box
     *
     * @return true if there are tasks available on the server
     */
    @SuppressWarnings("unchecked")
    protected boolean refreshTaskList() {
      m_tasksOnServer.removeAllItems();

      if (!m_hostPanel.hostOK()) {
        return false;
      }

      PostMethod post = null;
      boolean tasksOnServer = false;

      try {
        String service =
          GetTaskListServlet.CONTEXT_PATH + "/?" + JSONProtocol.JSON_CLIENT_KEY
            + "=Y";
        post = new PostMethod(constructURL(service));

        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client =
          WekaServer.ConnectionManager.getSingleton().createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        // Execute request
        int result = client.executeMethod(post);
        if (result == 401) {
          System.err.println("Unable to send task to get task list from server"
            + " - authentication required.\n");
          JOptionPane.showMessageDialog(KnowledgeFlowRemotePerspective.this,
            "Unable to get task list from server - authentication required",
            "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
        } else {
          String responseS = post.getResponseBodyAsString();
          ObjectMapper mapper = JsonFactory.create();
          Map<String, Object> responseMap =
            mapper.readValue(responseS, Map.class);
          Object responseType = responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);
          if (responseType == null
            || responseType.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            System.err.println("A problem occurred at the sever : \n"
              + "\t"
              + (responseType == null ? "response was null!" : responseMap
                .get(JSONProtocol.RESPONSE_MESSAGE_KEY)));
            JOptionPane.showMessageDialog(
              KnowledgeFlowRemotePerspective.this,
              "A problem occurred at the server : \n\n"
                + (responseType == null ? "response was null!" : responseMap
                  .get(JSONProtocol.RESPONSE_MESSAGE_KEY)),
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          } else {
            List<String> taskList =
              (List<String>) responseMap.get(JSONProtocol.RESPONSE_PAYLOAD_KEY);
            if (taskList.size() > 0) {
              Vector<String> newList = new Vector<String>();
              for (String task : taskList) {
                newList.add(task);
              }
              DefaultComboBoxModel model = new DefaultComboBoxModel(newList);
              m_tasksOnServer.setModel(model);
              tasksOnServer = true;
            }
          }
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(KnowledgeFlowRemotePerspective.this,
          "A problem occurred at the server : \n\n" + ex.getMessage(),
          "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
      } finally {
        if (post != null) {
          // Release current connection to the connection pool
          post.releaseConnection();
        }
      }

      return tasksOnServer;
    }

    /**
     * Add a new logging tab
     *
     * @param tabTitle the title for the tab
     * @return return the LogPanel for the tab
     */
    protected LogPanel addTab(String tabTitle) {
      String name = tabTitle;
      if (tabTitle.indexOf(WekaTaskMap.s_taskNameIDSeparator) > 0) {
        String[] parts = name.split(WekaTaskMap.s_taskNameIDSeparator);
        name = parts[0];
      }

      LogPanel logP = new LogPanel();
      Dimension d2 = new Dimension(100, 170);
      logP.setPreferredSize(d2);
      logP.setMinimumSize(d2);

      m_monitorTabs.addTab(name, logP);
      int numTabs = m_monitorTabs.getTabCount();
      m_monitorTabs.setTabComponentAt(numTabs - 1, new CloseableTabTitle(
        m_monitorTabs));
      m_monitorTabs.setToolTipTextAt(numTabs - 1, tabTitle);

      return logP;
    }

    /**
     * Start monitoring a task
     *
     * @param taskName the ID of the task to start monitoring
     */
    @SuppressWarnings("unchecked")
    public synchronized void monitorTask(final String taskName) {

      final LogPanel theLog = addTab(taskName);

      Thread monitorThread = new Thread() {
        @Override
        public void run() {
          Schedule sched = getScheduleForTask(taskName);

          PostMethod post = null;
          TaskStatusInfo resultInfo = null;
          // Get HTTP client
          HttpClient client =
            WekaServer.ConnectionManager.getSingleton().createHttpClient();
          WekaServer.ConnectionManager.addCredentials(client, m_username,
            m_password);
          boolean ok = true;
          try {
            while (ok) {

              if (this.isInterrupted()) {
                break;
              }

              if (sched != null) {
                Date now = new Date();
                if (now.before(sched.getStartDate())) {
                  // sleep until this task's start time
                  sleep(sched.getStartDate().getTime() - now.getTime());
                } else {
                  sleep(m_monitorInterval * 1000);
                }
              } else {
                sleep(m_monitorInterval * 1000);
              }

              String service =
                GetTaskStatusServlet.CONTEXT_PATH + "/?name=" + taskName + "&"
                  + JSONProtocol.JSON_CLIENT_KEY + "=Y";
              post = new PostMethod(constructURL(service));
              post.setDoAuthentication(true);
              post.addRequestHeader(new Header("Content-Type", "text/plain"));
              int result = client.executeMethod(post);
              // System.out.println("[WekaServer] Response from master server : "
              // + result);
              if (result == 401) {
                theLog
                  .statusMessage("[Remote Execution]|ERROR: authentication "
                    + "required on server.");
                theLog.logMessage("Unable to get remote status of task'"
                  + taskName + "' - authentication required.\n");
                ok = false;
              } else {
                // the response
                String responseS = post.getResponseBodyAsString();
                ObjectMapper mapper = JsonFactory.create();
                Map<String, Object> responseMap =
                  mapper.readValue(responseS, Map.class);
                Object responseType =
                  responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);
                if (responseType == null
                  || responseType.toString().startsWith(
                    WekaServlet.RESPONSE_ERROR)) {
                  theLog.statusMessage("[Remote Excecution]|ERROR: server "
                    + "returned an error - see log (and/or log on server)");
                  theLog.logMessage("Server returned an error: "
                    + "for task : '"
                    + taskName
                    + "'. ("
                    + (responseType == null ? "response was null" : responseMap
                      .get(JSONProtocol.RESPONSE_MESSAGE_KEY)) + ")."
                    + " Check logs on server.");
                  ok = false;
                } else {
                  Map<String, Object> taskStatusMap =
                    (Map<String, Object>) responseMap
                      .get(JSONProtocol.RESPONSE_PAYLOAD_KEY);
                  if (taskStatusMap != null) {
                    resultInfo =
                      JSONProtocol.jsonMapToStatusInfo(taskStatusMap);
                    if (resultInfo.getExecutionStatus() == TaskStatusInfo.FAILED) {
                      ok = false;
                      String message = resultInfo.getStatusMessage();
                      theLog.statusMessage("[Remote Excecution]|ERROR: task "
                        + "execution failed - see log (and/or log on server)");
                      if (message != null && message.length() > 0) {
                        theLog.logMessage(message);
                      }
                    } else {
                      String message = resultInfo.getStatusMessage();
                      if (message != null && message.length() > 0
                        && !message.equals("New Task")) {
                        BufferedReader br =
                          new BufferedReader(new StringReader(message));
                        String line = null;
                        boolean statusMessages = true;
                        while ((line = br.readLine()) != null) {
                          if (line.startsWith("@@@ Status messages")) {
                            statusMessages = true;
                          } else if (line.startsWith("@@@ Log messages")) {
                            statusMessages = false;
                          } else {
                            if (line.length() > 2) {
                              if (statusMessages) {
                                theLog.statusMessage(line);
                              } else {
                                theLog.logMessage(line);
                              }
                            }
                          }
                        }
                        br.close();

                        // if the task is scheduled, and has an end time, only
                        // continue to monitor if we haven't passed the end time
                        if (sched != null && sched.getEndDate() != null) {
                          Date now = new Date();
                          if (now.after(sched.getEndDate())) {
                            ok = false;
                          }
                        }
                      }
                      if (resultInfo.getExecutionStatus() == TaskStatusInfo.FINISHED
                        && sched == null) {
                        // It's finished - no need to grab any more
                        // logging/status
                        // info, unless this is a scheduled task...
                        ok = false;
                        TaskStatusInfo taskResult =
                          getResultFromServer(taskName);

                        // see if there are any results...
                        if (taskResult != null
                          && taskResult.getTaskResult() != null) {

                          if (taskResult.getTaskResult() != null) {
                            String base64 =
                              taskResult.getTaskResult().toString();
                            Object decoded = JSONProtocol.decodeBase64(base64);

                            Map<String, Object> results =
                              (Map<String, Object>) decoded;
                            String justName =
                              taskName
                                .replace(FlowRunnerRemote.NAME_PREFIX, "");
                            justName =
                              justName.substring(0, justName
                                .indexOf(WekaTaskMap.s_taskNameIDSeparator));

                            int flowIndex = -1;
                            for (int i = 0; i < m_mainPerspective.getNumTabs(); i++) {
                              if (m_mainPerspective.getTabTitle(i).equals(
                                justName)) {
                                flowIndex = i;
                                break;
                              }
                            }
                            if (flowIndex >= 0) {
                              Flow theFlow =
                                m_mainPerspective.getLayoutAt(flowIndex)
                                  .getFlow();
                              for (StepManagerImpl manager : theFlow.getSteps()) {
                                Step step = manager.getManagedStep();
                                if (step instanceof DataCollector) {
                                  Object data = results.get(step.getName());
                                  if (data != null) {
                                    ((DataCollector) step).restoreData(data);
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  } else {
                    theLog.logMessage("A problem occurred while trying to "
                      + "retrieve remote status of task : '" + taskName + "'. "
                      + "Check logs on server");
                    theLog
                      .statusMessage("[Remote Execution]|ERROR: task failed "
                        + "to return a task status - see log (and/or log on server)");
                    ok = false;
                  }
                }
              }

              if (post != null) {
                post.releaseConnection();
                post = null;
              }
            }
          } catch (Exception ex) {
            ex.printStackTrace();
            theLog.statusMessage("[Remote Excecution]|ERROR: a error "
              + "occurred while trying to retrieve remote status of task"
              + " - see log (and/or log on server)");
            theLog.logMessage("A problem occurred while "
              + "trying to retrieve remote status of task : '" + taskName
              + "' (" + ex.getMessage() + ")");

          } finally {
            if (post != null) {
              post.releaseConnection();
              post = null;
            }
          }
        }
      };

      ((CloseableTabTitle) m_monitorTabs.getTabComponentAt(m_monitorTabs
        .getTabCount() - 1)).setMonitorThread(monitorThread);
      monitorThread.setPriority(Thread.MIN_PRIORITY);
      monitorThread.start();
    }
  }

  /**
   * Inner class providing a JList dialog of flows from the main KF perspective.
   * The user can select one or more flows from the list to launch remotely
   */
  protected class FlowLauncher extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 5635324388653056125L;

    /** Button for popping up a panel to select the flow to execute */
    protected JButton m_popupFlowSelector =
      new JButton("Launch flows remotely");

    /** Checkbox for sequential execution */
    protected JCheckBox m_sequentialCheck = new JCheckBox(
      "Launch flow start points sequentially");

    /** Model for the JList */
    protected DefaultListModel<String> m_flowModel =
      new DefaultListModel<String>();

    /**
     * List of flows to select from - extracted from the tabs of the main KF
     * perspective
     */
    protected JList<String> m_flowList = new JList<String>(m_flowModel);

    /** Username for authentication */
    protected String m_username = "";

    /** Password for authentication */
    protected String m_password = "";

    /** Reference to the monitor panel */
    protected MonitorPanel m_monitorPanel;

    /**
     * Constructor
     *
     * @param username username for authentication
     * @param password password for authentication
     * @param monitorPanel reference to the monitor panel
     */
    public FlowLauncher(String username, String password,
      MonitorPanel monitorPanel) {
      m_username = username;
      m_password = password;
      m_monitorPanel = monitorPanel;

      setLayout(new BorderLayout());
      m_popupFlowSelector
        .setToolTipText("Launch flow(s) using the specified schedule");
      JPanel holder = new JPanel();
      holder.setLayout(new BorderLayout());

      holder.add(m_popupFlowSelector, BorderLayout.WEST);
      holder.add(m_sequentialCheck, BorderLayout.EAST);
      add(holder, BorderLayout.WEST);

      m_popupFlowSelector.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupList();
        }
      });
    }

    protected void popupList() {
      if (m_mainPerspective == null || m_mainPerspective.getNumTabs() == 0) {
        return;
      }
      if (!m_hostPanel.hostOK()) {
        JOptionPane.showMessageDialog(KnowledgeFlowRemotePerspective.this,
          "Can't connect to host, so can't launch flows.",
          "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
        System.err.println("Can't connect to host, so can't launch flows...");
        return;
      }
      m_flowModel.removeAllElements();
      // int unamedCount = 1;
      for (int i = 0; i < m_mainPerspective.getNumTabs(); i++) {
        String tabTitle = m_mainPerspective.getTabTitle(i);

        m_flowModel.addElement(tabTitle);
      }

      ListSelectorDialog jd = new ListSelectorDialog(null, m_flowList);

      // Open the dialog
      int result = jd.showDialog();

      if (result != ListSelectorDialog.APPROVE_OPTION) {
        m_flowList.clearSelection();
      } else {
        if (!m_flowList.isSelectionEmpty()) {
          int selected[] = m_flowList.getSelectedIndices();
          for (int element : selected) {
            int flowIndex = element;

            // only send to server if its not executing locally!
            if (!m_mainPerspective.getLayoutAt(flowIndex).isExecuting()) {
              String flowName = m_flowModel.get(flowIndex).toString();
              if (launchFlow(flowIndex, flowName)) {
                // abort sending any other flows...
                break;
              }
            }
          }
        }
      }
    }

    /**
     * Launch a knowledge flow on the remote server
     *
     * @param flowIndex the index, in the main perspective, of the flow to
     *          launch
     * @param flowName the name of the flow
     * @return true if the flow was launched without error.
     */
    protected boolean launchFlow(int flowIndex, String flowName) {
      Schedule sched = m_schedulePanel.getSchedule();

      String flowCopy = "";
      try {
        flowCopy = copyFlow(flowIndex);
      } catch (Exception ex) {
        JOptionPane
          .showMessageDialog(
            KnowledgeFlowRemotePerspective.this,
            "A problem occurred while copying the flow to execute:\n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
        return false;
      }

      NamedTask taskToRun = null;

      Map<String, String> params =
        m_schedulePanel.getParameterPanel().getParameters();

      taskToRun =
        new UnscheduledNamedKnowledgeFlowTask(FlowRunnerRemote.NAME_PREFIX
          + flowName, flowCopy, m_sequentialCheck.isSelected(), params);
      if (sched != null) {
        taskToRun =
          new ScheduledNamedKnowledgeFlowTask(
            (UnscheduledNamedKnowledgeFlowTask) taskToRun, sched);
      }

      PostMethod post = null;
      boolean errorOccurred = false;
      Map<String, Object> taskMap = JSONProtocol.kFTaskToJsonMap(taskToRun);
      String json = JSONProtocol.encodeToJSONString(taskMap);
      try {
        String service =
          ExecuteTaskServlet.CONTEXT_PATH + "/?" + JSONProtocol.JSON_CLIENT_KEY
            + "=Y";
        post = new PostMethod(constructURL(service));
        RequestEntity jsonRequest =
          new StringRequestEntity(json, JSONProtocol.JSON_MIME_TYPE,
            JSONProtocol.CHARACTER_ENCODING);

        post.setRequestEntity(jsonRequest);
        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type",
          JSONProtocol.JSON_MIME_TYPE));

        // Get HTTP client
        HttpClient client =
          WekaServer.ConnectionManager.getSingleton().createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        // Execute request
        int result = client.executeMethod(post);
        if (result == 401) {
          JOptionPane.showMessageDialog(KnowledgeFlowRemotePerspective.this,
            "Unable to send task to server - authentication required",
            "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          System.err
            .println("Unable to send task to server - authentication required.\n");
          errorOccurred = true;
        } else {
          String responseS = post.getResponseBodyAsString();
          ObjectMapper mapper = JsonFactory.create();
          Map<String, Object> responseMap =
            mapper.readValue(responseS, Map.class);
          Object responseType = responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);
          if (responseType == null
            || responseType.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            System.err.println("A problem occurred at the sever : \n");
            if (responseType == null) {
              System.err.println("Response was null!");
            } else {
              System.err.println(responseMap
                .get(JSONProtocol.RESPONSE_MESSAGE_KEY));
            }
            JOptionPane.showMessageDialog(
              KnowledgeFlowRemotePerspective.this,
              "A problem occurred at the server : \n\n"
                + (responseType == null ? "Response was null!" : responseMap
                  .get(JSONProtocol.RESPONSE_MESSAGE_KEY)),
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
            errorOccurred = true;
          } else {
            String taskID =
              responseMap.get(JSONProtocol.RESPONSE_MESSAGE_KEY).toString();
            System.out.println("Task ID from server : " + taskID);
            m_monitorPanel.monitorTask(taskID);
          }
        }
      } catch (Exception ex) {
        JOptionPane
          .showMessageDialog(
            KnowledgeFlowRemotePerspective.this,
            "A problem occurred while sending task to the server : \n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
      } finally {
        if (post != null) {
          // Release current connection to the connection pool
          post.releaseConnection();
        }
      }

      return errorOccurred;
    }

    private String copyFlow(int flowIndex) throws Exception {
      String jsonFlow =
        m_mainPerspective.getLayoutAt(flowIndex).getFlow().toJSON();
      return jsonFlow;
    }
  }

  /**
   * Construct a URL from the settings int the host panel
   *
   * @param serviceAndArguments the servlet to contact plus arguments
   * @return a URL
   */
  protected String constructURL(String serviceAndArguments) {
    String realHostname = m_hostPanel.getHostName();
    String realPort = m_hostPanel.getPort();
    try {
      realHostname =
        Environment.getSystemWide().substitute(m_hostPanel.getHostName());
      realPort = Environment.getSystemWide().substitute(m_hostPanel.getPort());
    } catch (Exception ex) {
    }

    if (realPort.equals("80")) {
      realPort = "";
    } else {
      realPort = ":" + realPort;
    }

    String retVal = "http://" + realHostname + realPort + serviceAndArguments;

    retVal = retVal.replace(" ", "%20");

    return retVal;
  }

  /**
   * Get default settings for this perspective
   *
   * @return default settings for this perspective
   */
  @Override
  public Defaults getDefaultSettings() {
    return new RemotePerspectiveDefaults();
  }

  /**
   * Default settings for the {@code KnowledgeFlowRemotePerspective}
   */
  public static class RemotePerspectiveDefaults extends Defaults {
    public static final String ID =
      "weka.gui.knowledgeflow.knowledgeflowremoteperspective";

    public static final Settings.SettingKey HOST_KEY = new Settings.SettingKey(
      ID + ".host", "Default remote host", "");
    public static final String HOST = "localhost";
    public static final Settings.SettingKey PORT_KEY = new Settings.SettingKey(
      ID + ".port", "Default remote port", "");
    public static final String PORT = "" + WekaServer.PORT;
    public static final Settings.SettingKey MONITORING_INTERVAL_KEY =
      new Settings.SettingKey(ID + ".monitoringInterval", "Default monitoring "
        + "interval (seconds)", "");
    public static final String MONITORING_INTERVAL = "15";
    private static final long serialVersionUID = -4216395745292129810L;

    public RemotePerspectiveDefaults() {
      super(ID);
      m_defaults.put(HOST_KEY, HOST);
      m_defaults.put(PORT_KEY, PORT);
      m_defaults.put(MONITORING_INTERVAL_KEY, MONITORING_INTERVAL);
    }

  }
}
