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
 *    KnowledgeFlowRemoteSchedulerPerspective.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

import com.toedter.calendar.JDateChooser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.mortbay.jetty.security.Password;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.WekaPackageManager;
import weka.experiment.TaskStatusInfo;
import weka.gui.ListSelectorDialog;
import weka.gui.beans.KnowledgeFlowApp.KFPerspective;
import weka.gui.beans.KnowledgeFlowApp.MainKFPerspective;
import weka.gui.beans.xml.XMLBeans;
import weka.server.ExecuteTaskServlet;
import weka.server.GetScheduleServlet;
import weka.server.GetTaskListServlet;
import weka.server.GetTaskResultServlet;
import weka.server.GetTaskStatusServlet;
import weka.server.NamedTask;
import weka.server.RootServlet;
import weka.server.Schedule;
import weka.server.WekaServer;
import weka.server.WekaServlet;
import weka.server.WekaTaskMap;
import weka.server.knowledgeFlow.legacy.LegacyScheduledNamedKFTask;
import weka.server.knowledgeFlow.legacy.LegacyUnscheduledNamedKFTask;
import weka.server.knowledgeFlow.legacy.FlowRunnerRemote;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.beancontext.BeanContextSupport;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/**
 * Class that provides a Knowledge Flow perspective for executing, scheduling
 * and monitoring tasks on remote Weka Server instances
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class KnowledgeFlowRemoteSchedulerPerspective extends JPanel implements
  KFPerspective {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -8233265107208192698L;

  /**
   * Inner class providing a panel for configuring and testing a connection to a
   * server
   */
  protected class HostPanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 7893017818866180239L;

    /** Text field for entering the host name */
    protected JTextField m_hostField = new JTextField(25);

    /** Text field for entering the port number */
    protected JTextField m_portField = new JTextField(8);

    /** Button for testing the configured connection details */
    protected JButton m_testBut = new JButton("Test connection");

    /** Label for displaying server status (and load level) */
    protected JLabel m_serverStatus = new JLabel();

    /**
     * Constructor
     */
    public HostPanel() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Remote host"));

      JPanel temp = new JPanel();
      temp.setLayout(new BorderLayout());

      JPanel holder1 = new JPanel();
      holder1.setLayout(new BorderLayout());
      holder1
        .add(new JLabel("Host: ", SwingConstants.RIGHT), BorderLayout.WEST);
      holder1.add(m_hostField, BorderLayout.CENTER);

      JPanel holder2 = new JPanel();
      holder2.setLayout(new BorderLayout());
      holder2
        .add(new JLabel("Port: ", SwingConstants.RIGHT), BorderLayout.WEST);
      holder2.add(m_portField, BorderLayout.CENTER);

      JPanel holder3 = new JPanel();
      holder3.setLayout(new BorderLayout());
      holder3.add(holder1, BorderLayout.CENTER);
      holder3.add(holder2, BorderLayout.EAST);

      temp.add(holder3, BorderLayout.NORTH);

      m_hostField.setText("localhost");
      m_portField.setText("" + WekaServer.PORT);

      JPanel holder4 = new JPanel();
      holder4.setLayout(new BorderLayout());
      holder4.add(m_testBut, BorderLayout.WEST);

      JPanel holder5 = new JPanel();
      holder5.setLayout(new BorderLayout());
      holder5.setBorder(BorderFactory.createTitledBorder("Server status"));
      holder5.add(m_serverStatus, BorderLayout.CENTER);
      holder5.add(holder4, BorderLayout.WEST);

      temp.add(holder5, BorderLayout.SOUTH);

      add(temp, BorderLayout.NORTH);

      m_testBut.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          hostOK();
        }
      });
    }

    /**
     * Get the host name
     * 
     * @return the host name
     */
    public String getHostName() {
      return m_hostField.getText();
    }

    /**
     * Get the port
     * 
     * @return the port
     */
    public String getPort() {
      return m_portField.getText();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#toString()
     */
    @Override
    public String toString() {
      String hostPort = m_hostField.getText() + ":" + m_portField.getText();
      return hostPort;
    }

    /**
     * Test the connection details
     * 
     * @return true if a connection can be made to the server
     */
    public boolean hostOK() {
      if (m_hostField.getText() != null && m_hostField.getText().length() > 0) {
        String host = m_hostField.getText();
        String port = "" + WekaServer.PORT;
        if (m_portField.getText() != null && m_portField.getText().length() > 0) {
          port = m_portField.getText();
        }

        try {
          host = Environment.getSystemWide().substitute(host);
        } catch (Exception ex) {
        }
        try {
          port = Environment.getSystemWide().substitute(port);
        } catch (Exception ex) {
        }

        String server = host + ":" + port;
        double serverLoad = RootServlet.getSlaveLoad(server, m_username,
          m_password);
        if (serverLoad < 0) {
          m_serverStatus.setText("Unable to connect to server.");
          return false;
        } else {
          m_serverStatus.setText("OK. Server load: " + serverLoad);
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Inner class providing a panel for configuring task scheduling details
   */
  protected class SchedulePanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -1319029770018027370L;

    /**
     * Inner class to the SchedulePanel providing a date time configuration
     * panel
     */
    protected class DateTimePanel extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = 5343421783518237483L;

      /** A widget for selecting a date */
      protected JDateChooser m_dateChooser;
      /** Combo for choosing the hour of the day */
      protected JComboBox m_hourCombo = new JComboBox();
      /** Combo for choosing the minute of the hour */
      protected JComboBox m_minutesCombo = new JComboBox();
      /** Radio button for selecting execution now (or no end date) */
      protected JRadioButton m_nowNoEndBut;
      /** Radio button for selecting a specific date */
      protected JRadioButton m_dateBut = new JRadioButton("Date", false);

      /**
       * Construct a new DateTimePanel
       * 
       * @param title title to display in the border of the panel
       * @param now true if the now/no-end radio button should be labeled "now"
       */
      public DateTimePanel(String title, boolean now) {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        m_dateChooser = new JDateChooser(new Date(), "yyyy-MM-dd");
        if (now) {
          m_nowNoEndBut = new JRadioButton("Now (unscheduled)", true);
        } else {
          m_nowNoEndBut = new JRadioButton("No end", true);
        }
        ButtonGroup b = new ButtonGroup();
        b.add(m_nowNoEndBut);
        b.add(m_dateBut);

        Vector<String> hours = new Vector<String>();
        for (int i = 0; i < 24; i++) {
          String num = (i < 10) ? "0" + i : "" + i;
          hours.add(num);
        }
        m_hourCombo.setModel(new DefaultComboBoxModel(hours));

        Vector<String> minutes = new Vector<String>();
        for (int i = 0; i < 60; i++) {
          String num = (i < 10) ? "0" + i : "" + i;
          minutes.add(num);
        }
        m_minutesCombo.setModel(new DefaultComboBoxModel(minutes));

        JPanel topHolder = new JPanel();
        topHolder.setLayout(new BorderLayout());
        JPanel temp1 = new JPanel();
        temp1.setLayout(new FlowLayout(FlowLayout.LEFT));
        temp1.add(m_nowNoEndBut);
        JPanel temp2 = new JPanel();
        temp2.setLayout(new FlowLayout(FlowLayout.LEFT));
        temp2.add(m_dateBut);
        temp2.add(m_dateChooser);
        topHolder.add(temp1, BorderLayout.NORTH);
        topHolder.add(temp2, BorderLayout.SOUTH);

        JPanel temp3 = new JPanel();
        temp3.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel padder = new JPanel();
        padder.setMinimumSize(m_dateBut.getMinimumSize());
        temp3.add(padder);
        temp3.add(new JLabel("Time"));
        temp3.add(m_hourCombo);
        temp3.add(m_minutesCombo);

        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.add(topHolder, BorderLayout.NORTH);
        holder.add(temp3, BorderLayout.SOUTH);

        add(holder, BorderLayout.NORTH);

        m_nowNoEndBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            boolean enable = !m_nowNoEndBut.isSelected();

            m_dateChooser.setEnabled(enable);
            m_hourCombo.setEnabled(enable);
            m_minutesCombo.setEnabled(enable);
          }
        });

        m_dateBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            boolean enable = !m_nowNoEndBut.isSelected();

            m_dateChooser.setEnabled(enable);
            m_hourCombo.setEnabled(enable);
            m_minutesCombo.setEnabled(enable);
          }
        });

        m_dateChooser.setEnabled(false);
        m_hourCombo.setEnabled(false);
        m_minutesCombo.setEnabled(false);
      }

      /**
       * Get a Date object for the date configured in this panel
       * 
       * @return the configured date
       */
      public Date getDate() {
        if (m_nowNoEndBut.isSelected()) {
          return null;
        }

        Date d = new Date(m_dateChooser.getDate().getTime());
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY,
          Integer.parseInt(m_hourCombo.getSelectedItem().toString()));
        cal.set(Calendar.MINUTE,
          Integer.parseInt(m_minutesCombo.getSelectedItem().toString()));

        // resolution to the nearest minute
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
      }

      /**
       * Add a listener to radio button state changes
       * 
       * @param listener the listener to add
       */
      public void addRadioActionListener(ActionListener listener) {
        m_nowNoEndBut.addActionListener(listener);
        m_dateBut.addActionListener(listener);
      }
    }

    /**
     * Inner class to the SchedulePanel providing a panel for configuring repeat
     * details
     */
    protected class RepeatPanel extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = 749883190997695194L;

      /** The enabled status of the entire panel */
      protected boolean m_globalEnabled = true;

      /** Combo box for the repeat type */
      protected JComboBox m_repeatCombo = new JComboBox();

      /** Panel to hold the specific configuration widgets for each repeat type */
      protected JPanel m_detailPanel = new JPanel();

      /** Label for the interval */
      protected JLabel m_intervalLab = new JLabel("", SwingConstants.LEFT);

      /** Text field for the interval */
      protected JTextField m_intervalField = new JTextField(5);

      /** Labels for monthly details */
      protected JLabel m_ofEveryMonthLab = new JLabel("of every month");
      protected JLabel m_ofEveryMonthLab2 = new JLabel("of every month");

      /** Weekly stuff */
      protected JCheckBox m_sunCheck = new JCheckBox("Sunday");
      protected JCheckBox m_monCheck = new JCheckBox("Monday");
      protected JCheckBox m_tueCheck = new JCheckBox("Tuesday");
      protected JCheckBox m_wedCheck = new JCheckBox("Wednesday");
      protected JCheckBox m_thuCheck = new JCheckBox("Thursday");
      protected JCheckBox m_friCheck = new JCheckBox("Friday");
      protected JCheckBox m_satCheck = new JCheckBox("Saturday");

      /** Monthly stuff */
      protected JRadioButton m_dayYearNumBut = new JRadioButton("");
      protected JRadioButton m_specificDayYearBut = new JRadioButton("The");
      protected JTextField m_dayYearNumField = new JTextField(5);
      protected JComboBox m_occurrenceInMonth = new JComboBox();
      protected JComboBox m_dayOfWeek = new JComboBox();
      protected JComboBox m_month = new JComboBox();

      /**
       * Construct a new RepeatPanel
       */
      public RepeatPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Repeat"));

        Vector<String> repeats = new Vector<String>();
        for (Schedule.Repeat r : Schedule.Repeat.values()) {
          repeats.add(r.toString());
        }
        m_repeatCombo.setModel(new DefaultComboBoxModel(repeats));

        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.add(m_repeatCombo, BorderLayout.NORTH);

        m_repeatCombo.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String selected = m_repeatCombo.getSelectedItem().toString();
            if (selected.equals("once")) {
              setupOnce();
              return;
            }
            if (selected.equals("minutes")) {
              setupInterval("minute(s)");
              return;
            }
            if (selected.equals("hours")) {
              setupInterval("hour(s)");
              return;
            }
            if (selected.equals("days")) {
              setupInterval("day(s)");
              return;
            }
            if (selected.equals("weekly")) {
              setupWeekly();
              return;
            }
            if (selected.equals("monthly")) {
              setupMonthly();
              return;
            }
            if (selected.equals("yearly")) {
              setupYearly();
            }
          }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(m_dayYearNumBut);
        group.add(m_specificDayYearBut);
        m_dayYearNumBut.setSelected(true);
        m_dayYearNumField.setEnabled(m_dayYearNumBut.isSelected());
        m_occurrenceInMonth.setEnabled(m_specificDayYearBut.isSelected());
        m_dayOfWeek.setEnabled(m_specificDayYearBut.isSelected());
        m_month.setEnabled(m_specificDayYearBut.isSelected());
        m_dayYearNumBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            checkEnabled();
          }
        });
        m_specificDayYearBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            checkEnabled();
          }
        });

        Vector<String> occur = new Vector<String>();
        for (Schedule.OccurrenceWithinMonth o : Schedule.OccurrenceWithinMonth
          .values()) {
          occur.add(o.toString());
        }
        m_occurrenceInMonth.setModel(new DefaultComboBoxModel(occur));
        Vector<String> days = new Vector<String>();
        days.add("Sunday");
        days.add("Monday");
        days.add("Tuesday");
        days.add("Wednesday");
        days.add("Thursday");
        days.add("Friday");
        days.add("Saturday");
        m_dayOfWeek.setModel(new DefaultComboBoxModel(days));

        Vector<String> months = new Vector<String>();
        months.add("January");
        months.add("February");
        months.add("March");
        months.add("April");
        months.add("May");
        months.add("June");
        months.add("July");
        months.add("August");
        months.add("September");
        months.add("October");
        months.add("November");
        months.add("December");
        m_month.setModel(new DefaultComboBoxModel(months));

        m_detailPanel.setLayout(new BorderLayout());
        holder.add(m_detailPanel, BorderLayout.SOUTH);
        add(holder, BorderLayout.NORTH);
      }

      private void checkEnabled() {

        m_dayYearNumField.setEnabled(m_dayYearNumBut.isSelected()
          && m_globalEnabled);
        m_occurrenceInMonth.setEnabled(m_specificDayYearBut.isSelected()
          && m_globalEnabled);
        m_dayOfWeek.setEnabled(m_specificDayYearBut.isSelected()
          && m_globalEnabled);
        m_month
          .setEnabled(m_specificDayYearBut.isSelected() && m_globalEnabled);
        m_ofEveryMonthLab.setEnabled(m_dayYearNumBut.isSelected()
          && m_globalEnabled);
        m_ofEveryMonthLab2.setEnabled(m_specificDayYearBut.isSelected()
          && m_globalEnabled);

        m_repeatCombo.setEnabled(m_globalEnabled);
        m_intervalLab.setEnabled(m_globalEnabled);
        m_intervalField.setEnabled(m_globalEnabled);
        m_sunCheck.setEnabled(m_globalEnabled);
        m_monCheck.setEnabled(m_globalEnabled);
        m_tueCheck.setEnabled(m_globalEnabled);
        m_wedCheck.setEnabled(m_globalEnabled);
        m_thuCheck.setEnabled(m_globalEnabled);
        m_friCheck.setEnabled(m_globalEnabled);
        m_satCheck.setEnabled(m_globalEnabled);
        m_dayYearNumBut.setEnabled(m_globalEnabled);
        m_specificDayYearBut.setEnabled(m_globalEnabled);

        // m_dayYearNumField.setEnabled(m_globalEnabled);
        // m_occurrenceInMonth.setEnabled(m_globalEnabled);
        // m_dayOfWeek.setEnabled(m_globalEnabled);
        // m_month.setEnabled(m_globalEnabled);
      }

      private void setupOnce() {
        m_detailPanel.removeAll();
        revalidate();
        repaint();
      }

      private void setupInterval(String label) {
        m_detailPanel.removeAll();
        JPanel temp = new JPanel();
        temp.setLayout(new FlowLayout(FlowLayout.LEFT));
        temp.add(new JLabel("Every", SwingConstants.RIGHT));
        m_intervalLab.setText(label);
        temp.add(m_intervalField);
        temp.add(m_intervalLab);
        m_detailPanel.add(temp, BorderLayout.NORTH);
        revalidate();
        repaint();
      }

      private void setupWeekly() {
        m_detailPanel.removeAll();
        JLabel lab = new JLabel("Recur every week on:");
        JPanel temp = new JPanel();
        temp.setLayout(new FlowLayout(FlowLayout.LEFT));
        temp.add(lab);
        JPanel temp2 = new JPanel();
        temp2.setLayout(new GridLayout(2, 4));
        temp2.add(m_sunCheck);
        temp2.add(m_monCheck);
        temp2.add(m_tueCheck);
        temp2.add(m_wedCheck);
        temp2.add(m_thuCheck);
        temp2.add(m_friCheck);
        temp2.add(m_satCheck);
        m_detailPanel.add(temp, BorderLayout.NORTH);
        m_detailPanel.add(temp2, BorderLayout.CENTER);
        revalidate();
        repaint();
      }

      private void setupMonthly() {
        m_detailPanel.removeAll();

        JPanel topP = new JPanel();
        m_dayYearNumBut.setText("Day");
        topP.setLayout(new FlowLayout(FlowLayout.LEFT));
        topP.add(m_dayYearNumBut);
        topP.add(m_dayYearNumField);
        topP.add(m_ofEveryMonthLab);

        JPanel botP = new JPanel();
        botP.setLayout(new FlowLayout(FlowLayout.LEFT));
        botP.add(m_specificDayYearBut);
        botP.add(m_occurrenceInMonth);
        botP.add(m_dayOfWeek);
        botP.add(m_ofEveryMonthLab2);
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.add(topP, BorderLayout.NORTH);
        holder.add(botP, BorderLayout.SOUTH);
        m_detailPanel.add(holder, BorderLayout.NORTH);
        revalidate();
        repaint();
      }

      private void setupYearly() {
        m_detailPanel.removeAll();

        m_dayYearNumBut.setText("Every");

        JPanel topP = new JPanel();
        topP.setLayout(new FlowLayout(FlowLayout.LEFT));
        topP.add(m_dayYearNumBut);
        topP.add(m_dayYearNumField);
        topP.add(new JLabel("years"));

        JPanel botP = new JPanel();
        botP.setLayout(new FlowLayout(FlowLayout.LEFT));
        botP.add(m_specificDayYearBut);
        botP.add(m_occurrenceInMonth);
        botP.add(m_dayOfWeek);
        botP.add(new JLabel("of"));
        botP.add(m_month);
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.add(topP, BorderLayout.NORTH);
        holder.add(botP, BorderLayout.SOUTH);
        m_detailPanel.add(holder, BorderLayout.NORTH);
        revalidate();
        repaint();
      }

      /**
       * Set the global enabled status
       * 
       * @param enabled true if panel is enabled globally
       */
      @Override
      public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        m_globalEnabled = enabled;
        checkEnabled();
      }

      /**
       * Apply the panel's settings to a Schedule object
       * 
       * @param sched the schedule to configure
       */
      public void applyToSchedule(Schedule sched) {
        String repeat = m_repeatCombo.getSelectedItem().toString();
        for (Schedule.Repeat r : Schedule.Repeat.values()) {
          if (r.toString().equals(repeat)) {
            sched.setRepeatUnit(r);
            break;
          }
        }
        if (m_intervalField.getText() != null
          && m_intervalField.getText().length() > 0) {
          sched.setRepeatValue(Integer.parseInt(m_intervalField.getText()));
        } else if (sched.getRepeatUnit() == Schedule.Repeat.MINUTES
          || sched.getRepeatUnit() == Schedule.Repeat.HOURS
          || sched.getRepeatUnit() == Schedule.Repeat.DAYS) {
          // set a default value of 5
          sched.setRepeatValue(5);
          m_intervalField.setText("5");
        }

        java.util.List<Integer> dow = new java.util.ArrayList<Integer>();
        if (m_sunCheck.isSelected()) {
          dow.add(Calendar.SUNDAY);
        }
        if (m_monCheck.isSelected()) {
          dow.add(Calendar.MONDAY);
        }
        if (m_tueCheck.isSelected()) {
          dow.add(Calendar.TUESDAY);
        }
        if (m_wedCheck.isSelected()) {
          dow.add(Calendar.WEDNESDAY);
        }
        if (m_thuCheck.isSelected()) {
          dow.add(Calendar.THURSDAY);
        }
        if (m_friCheck.isSelected()) {
          dow.add(Calendar.FRIDAY);
        }
        if (m_satCheck.isSelected()) {
          dow.add(Calendar.SATURDAY);
        }
        sched.setDayOfTheWeek(dow);

        if (sched.getRepeatUnit() == Schedule.Repeat.MONTHLY) {
          if (m_dayYearNumBut.isSelected()) {
            if (m_dayYearNumField.getText() != null
              && m_dayYearNumField.getText().length() > 0) {
              sched.setDayOfTheMonth(Integer.parseInt(m_dayYearNumField
                .getText()));
            }
          } else {
            for (Schedule.OccurrenceWithinMonth o : Schedule.OccurrenceWithinMonth
              .values()) {
              if (o.equals(m_occurrenceInMonth.getSelectedItem().toString())) {
                sched.setOccurrenceWithinMonth(o);
                break;
              }
            }
          }
        }

        if (sched.getRepeatUnit() == Schedule.Repeat.YEARLY) {
          if (m_dayYearNumBut.isSelected()) {
            if (m_dayYearNumField.getText() != null
              && m_dayYearNumField.getText().length() > 0) {
              sched
                .setRepeatValue(Integer.parseInt(m_dayYearNumField.getText()));
            }
          } else {
            for (Schedule.OccurrenceWithinMonth o : Schedule.OccurrenceWithinMonth
              .values()) {
              if (o.equals(m_occurrenceInMonth.getSelectedItem().toString())) {
                sched.setOccurrenceWithinMonth(o);
                break;
              }
            }
            // day of the week is already set

            sched.setMonthOfTheYear(m_month.getSelectedIndex());
          }
        }
      }
    }

    /**
     * Inner class providing a panel for configuring flow variables
     */
    protected class ParameterPanel extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = -491659781619944544L;

      /** The JTable for configuring variables */
      protected InteractiveTablePanel m_table = new InteractiveTablePanel(
        new String[] { "Variable", "Value", "" });

      /**
       * Construct a new ParameterPanel
       */
      public ParameterPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Flow variables"));
        add(m_table, BorderLayout.CENTER);
      }

      /**
       * Get the configured variables
       * 
       * @return a Map of variable names and values
       */
      public Map<String, String> getParameters() {

        Map<String, String> result = new HashMap<String, String>();
        JTable table = m_table.getTable();
        int numRows = table.getModel().getRowCount();

        for (int i = 0; i < numRows; i++) {
          String paramName = table.getValueAt(i, 0).toString();
          String paramValue = table.getValueAt(i, 1).toString();
          if (paramName.length() > 0 && paramValue.length() > 0) {
            result.put(paramName, paramValue);
          }
        }

        return result;
      }
    }

    /**
     * Inner class providing a JList dialog of flows from the main KF
     * perspective. The user can select one or more flows from the list to
     * launch remotely
     */
    protected class FlowLauncher extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = 5635324388653056125L;
      protected JButton m_popupFlowSelector = new JButton(
        "Launch flows remotely");
      protected JCheckBox m_sequentialCheck = new JCheckBox(
        "Launch flow start points sequentially");
      protected DefaultListModel m_flowModel = new DefaultListModel();
      protected JList m_flowList = new JList(m_flowModel);

      public FlowLauncher() {
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
          JOptionPane.showMessageDialog(
            KnowledgeFlowRemoteSchedulerPerspective.this,
            "Can't connect to host, so can't launch flows.",
            "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          System.err.println("Can't connect to host, so can't launch flows...");
          return;
        }
        m_flowModel.removeAllElements();
        // int unamedCount = 1;
        for (int i = 0; i < m_mainPerspective.getNumTabs(); i++) {
          String tabTitle = m_mainPerspective.getTabTitle(i);

          /*
           * File flowFile = m_mainPerspective.getFlowFile(i); if
           * (flowFile.getName().equals("-NONE-")) {
           * m_flowModel.addElement("unnamedFlow" + unamedCount++); } else {
           * String flowName = flowFile.getName(); if (flowName.indexOf('.') >
           * 0) { flowName = flowName.substring(0, flowName.lastIndexOf('.')); }
           * m_flowModel.addElement(flowName); }
           */
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
              if (!m_mainPerspective.getExecuting(flowIndex)) {
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

        StringBuffer flowXML = null;
        try {
          flowXML = copyFlow(flowIndex);
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
            KnowledgeFlowRemoteSchedulerPerspective.this,
            "A problem occurred while copying the flow to execute:\n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
          ex.printStackTrace();
          return false;
        }

        NamedTask taskToRun = null;

        Map<String, String> params = m_parameters.getParameters();
        if (params.size() == 0) {
          params = null;
        }
        if (sched == null) {
          taskToRun = new LegacyUnscheduledNamedKFTask(FlowRunnerRemote.NAME_PREFIX
            + flowName, flowXML, m_sequentialCheck.isSelected(), params);
        } else {
          taskToRun = new LegacyScheduledNamedKFTask(FlowRunnerRemote.NAME_PREFIX
            + flowName, flowXML, m_sequentialCheck.isSelected(), params, sched);
        }

        InputStream is = null;
        PostMethod post = null;
        boolean errorOccurred = false;

        try {
          byte[] serializedTask = WekaServer.serializeTask(taskToRun);
          System.out.println("Sending " + serializedTask.length + " bytes...");

          String service = ExecuteTaskServlet.CONTEXT_PATH + "/?client=Y";
          post = new PostMethod(constructURL(service));
          RequestEntity entity = new ByteArrayRequestEntity(serializedTask);
          post.setRequestEntity(entity);

          post.setDoAuthentication(true);
          post.addRequestHeader(new Header("Content-Type",
            "application/octet-stream"));

          // Get HTTP client
          HttpClient client = WekaServer.ConnectionManager.getSingleton()
            .createHttpClient();
          WekaServer.ConnectionManager.addCredentials(client, m_username,
            m_password);

          // Execute request
          int result = client.executeMethod(post);
          if (result == 401) {
            JOptionPane.showMessageDialog(
              KnowledgeFlowRemoteSchedulerPerspective.this,
              "Unable to send task to server - authentication required",
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
            System.err
              .println("Unable to send task to server - authentication required.\n");
            errorOccurred = true;
          } else {
            is = post.getResponseBodyAsStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Object response = ois.readObject();
            if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
              System.err.println("A problem occurred at the sever : \n" + "\t"
                + response.toString());
              JOptionPane
                .showMessageDialog(
                  KnowledgeFlowRemoteSchedulerPerspective.this,
                  "A problem occurred at the server : \n\n"
                    + response.toString(), "SchedulePerspective",
                  JOptionPane.ERROR_MESSAGE);
              errorOccurred = true;
            } else {
              String taskID = response.toString();
              System.out.println("Task ID from server : " + taskID);
              m_monitorPanel.monitorTask(taskID);
            }
          }
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
            KnowledgeFlowRemoteSchedulerPerspective.this,
            "A problem occurred while sending task to the server : \n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
          ex.printStackTrace();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (Exception e) {
            }
          }

          if (post != null) {
            // Release current connection to the connection pool
            post.releaseConnection();
          }
        }
        return errorOccurred;
      }

      private StringBuffer copyFlow(int flowIndex) throws Exception {
        StringBuffer copy = new StringBuffer();
        Vector beans = BeanInstance.getBeanInstances(flowIndex);
        Vector associatedConnections = BeanConnection.getConnections(flowIndex);

        Vector v = new Vector();
        v.setSize(2);
        v.set(XMLBeans.INDEX_BEANINSTANCES, beans);
        v.set(XMLBeans.INDEX_BEANCONNECTIONS, associatedConnections);
        KnowledgeFlowApp.BeanLayout layout = m_mainPerspective
          .getBeanLayout(flowIndex);
        BeanContextSupport bcs = new BeanContextSupport();
        bcs.setDesignTime(true);

        XMLBeans xml = new XMLBeans(layout, bcs, flowIndex);
        java.io.StringWriter sw = new java.io.StringWriter();
        xml.write(sw, v);

        return sw.getBuffer();
      }
    }

    /** A panel for configuring the start date/time */
    protected DateTimePanel m_startDate;

    /** A panel for configuring the end date/time */
    protected DateTimePanel m_endDate;

    /** A panel for configuring the repeat */
    protected RepeatPanel m_repeat = new RepeatPanel();

    /** A panel for specifying flow variables and their values */
    protected ParameterPanel m_parameters = new ParameterPanel();

    /** Popup that lists the available flows from the main perspective */
    protected FlowLauncher m_launcher = new FlowLauncher();

    /**
     * Construct a new SchedulePanel
     */
    public SchedulePanel() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("Schedule"));
      JPanel holderPanel = new JPanel();
      holderPanel.setLayout(new BorderLayout());

      m_startDate = new DateTimePanel("Start", true);
      m_endDate = new DateTimePanel("End", false);
      JPanel temp1 = new JPanel();
      temp1.setLayout(new GridLayout(1, 2));
      temp1.add(m_startDate);
      temp1.add(m_endDate);
      holderPanel.add(temp1, BorderLayout.NORTH);

      JPanel temp2 = new JPanel();
      temp2.setLayout(new GridLayout(1, 2));
      temp2.add(m_repeat);
      temp2.add(m_parameters);
      holderPanel.add(temp2, BorderLayout.SOUTH);
      m_repeat.setEnabled(false);

      add(holderPanel, BorderLayout.NORTH);
      add(m_launcher, BorderLayout.SOUTH);

      // action listener to listen for changes in the start date panel
      // so that we can enable/disable the repeat panel
      m_startDate.addRadioActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (m_startDate.getDate() == null) {
            m_repeat.setEnabled(false);
          } else {
            m_repeat.setEnabled(true);
          }
        }
      });
    }

    /**
     * Get the schedule configured by this panel
     * 
     * @return the configured schedule
     */
    public Schedule getSchedule() {

      Schedule sched = new Schedule();

      if (m_startDate.getDate() != null) {
        sched.setStartDate(m_startDate.getDate());
      } else {
        return null; // execute unscheduled (right now)
      }
      if (m_endDate.getDate() != null) {
        sched.setEndDate(m_endDate.getDate());
      }

      m_repeat.applyToSchedule(sched);

      return sched;
    }
  }

  /**
   * Inner class providing a panel for monitoring flows running on the server.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected class MonitorPanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 6556408319133214173L;

    private class CloseableTabTitle extends JPanel {

      /**
       * For serialization
       */
      private static final long serialVersionUID = -871710265588121352L;
      private final JTabbedPane m_enclosingPane;
      private Thread m_monitorThread;

      private JLabel m_tabLabel;
      private TabButton m_tabButton;

      public CloseableTabTitle(final JTabbedPane pane) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        m_enclosingPane = pane;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // read the title from the JTabbedPane
        m_tabLabel = new JLabel() {

          @Override
          public String getText() {
            int index = m_enclosingPane
              .indexOfTabComponent(CloseableTabTitle.this);
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

      public void setBold(boolean bold) {
        m_tabLabel.setEnabled(bold);
      }

      public void setButtonEnabled(boolean enabled) {
        m_tabButton.setEnabled(enabled);
      }

      private class TabButton extends JButton implements ActionListener {

        /**
         * For serialization
         */
        private static final long serialVersionUID = 8050674024546615663L;

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

    /**
     * Construct a new MontitorPanel
     */
    public MonitorPanel() {
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
              interval = Integer.parseInt(m_monitorIntervalField.getText()
                .trim());
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
     * Retrieve a result from the server
     * 
     * @param taskID the ID of the task to get the result for
     * @return the TaskStatusInfo object encapsulating the result
     */
    protected TaskStatusInfo getResultFromServer(String taskID) {
      InputStream is = null;
      PostMethod post = null;
      TaskStatusInfo resultInfo = null;

      try {
        String service = GetTaskResultServlet.CONTEXT_PATH + "/?name=" + taskID
          + "&client=Y";
        post = new PostMethod(constructURL(service));
        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client = WekaServer.ConnectionManager.getSingleton()
          .createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        int result = client.executeMethod(post);
        if (result == 401) {
          System.err
            .println("Unable to retrieve task from server - authentication "
              + "required");
        } else {
          is = post.getResponseBodyAsStream();
          ObjectInputStream ois = new ObjectInputStream(
            new BufferedInputStream(new GZIPInputStream(is)));
          Object response = ois.readObject();
          if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {

            System.err.println("Server returned an error: "
              + "while trying to retrieve task result for task: '" + taskID
              + "'. (" + response.toString() + ")." + " Check logs on server.");
          } else {
            resultInfo = ((TaskStatusInfo) response);
          }
        }
      } catch (Exception ex) {
        System.err
          .println("An error occurred while trying to retrieve task result from "
            + "server: " + ex.getMessage());
        ex.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (Exception e) {
          }
        }

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
    protected Schedule getScheduleForTask(String taskName) {

      InputStream is = null;
      PostMethod post = null;
      TaskStatusInfo resultInfo = null;
      Schedule sched = null;

      if (!m_hostPanel.hostOK()) {
        return null;
      }

      try {
        String service = GetScheduleServlet.CONTEXT_PATH + "/?name=" + taskName
          + "&client=Y";
        post = new PostMethod(constructURL(service));

        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client = WekaServer.ConnectionManager.getSingleton()
          .createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        int result = client.executeMethod(post);
        if (result == 401) {
          JOptionPane
            .showMessageDialog(
              KnowledgeFlowRemoteSchedulerPerspective.this,
              "Unable to get schedule information from server - authentication required",
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          System.err
            .println("Unable to send task to get schedule info from server"
              + " - authentication required.\n");
        } else {
          is = post.getResponseBodyAsStream();
          ObjectInputStream ois = new ObjectInputStream(is);
          Object response = ois.readObject();

          if (response.toString().indexOf("is not a scheduled task") > 0) {
            // just return null
          } else if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            System.err.println("A problem occurred at the sever : \n" + "\t"
              + response.toString());
            JOptionPane.showMessageDialog(
              KnowledgeFlowRemoteSchedulerPerspective.this,
              "A problem occurred at the server : \n\n" + response.toString(),
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          } else {
            sched = (Schedule) response;
          }
        }
      } catch (Exception ex) {
        JOptionPane
          .showMessageDialog(KnowledgeFlowRemoteSchedulerPerspective.this,
            "A problem occurred while trying to get schedule info from server : \n\n"
              + ex.getMessage(), "SchedulePerspective",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (Exception e) {
          }
        }

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
    protected boolean refreshTaskList() {
      m_tasksOnServer.removeAllItems();

      if (!m_hostPanel.hostOK()) {
        return false;
      }

      InputStream is = null;
      PostMethod post = null;
      boolean tasksOnServer = false;

      try {
        String service = GetTaskListServlet.CONTEXT_PATH + "/?client=Y";
        post = new PostMethod(constructURL(service));

        post.setDoAuthentication(true);
        post.addRequestHeader(new Header("Content-Type", "text/plain"));

        // Get HTTP client
        HttpClient client = WekaServer.ConnectionManager.getSingleton()
          .createHttpClient();
        WekaServer.ConnectionManager.addCredentials(client, m_username,
          m_password);

        // Execute request
        int result = client.executeMethod(post);
        if (result == 401) {
          System.err.println("Unable to send task to get task list from server"
            + " - authentication required.\n");
          JOptionPane.showMessageDialog(
            KnowledgeFlowRemoteSchedulerPerspective.this,
            "Unable to get task list from server - authentication required",
            "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
        } else {
          is = post.getResponseBodyAsStream();
          ObjectInputStream ois = new ObjectInputStream(is);
          Object response = ois.readObject();
          if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
            System.err.println("A problem occurred at the sever : \n" + "\t"
              + response.toString());
            JOptionPane.showMessageDialog(
              KnowledgeFlowRemoteSchedulerPerspective.this,
              "A problem occurred at the server : \n\n" + response.toString(),
              "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
          } else {
            List<String> taskList = (List<String>) response;
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
        JOptionPane.showMessageDialog(
          KnowledgeFlowRemoteSchedulerPerspective.this,
          "A problem occurred at the server : \n\n" + ex.getMessage(),
          "SchedulePerspective", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (Exception e) {
          }
        }

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
    public synchronized void monitorTask(final String taskName) {

      final LogPanel theLog = addTab(taskName);

      Thread monitorThread = new Thread() {
        @Override
        public void run() {
          Schedule sched = getScheduleForTask(taskName);

          InputStream is = null;
          PostMethod post = null;
          TaskStatusInfo resultInfo = null;
          // Get HTTP client
          HttpClient client = WekaServer.ConnectionManager.getSingleton()
            .createHttpClient();
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

              String service = GetTaskStatusServlet.CONTEXT_PATH + "/?name="
                + taskName + "&client=Y";
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
                is = post.getResponseBodyAsStream();
                ObjectInputStream ois = new ObjectInputStream(
                  new BufferedInputStream(new GZIPInputStream(is)));
                Object response = ois.readObject();
                if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
                  theLog.statusMessage("[Remote Excecution]|ERROR: server "
                    + "returned an error - see log (and/or log on server)");
                  theLog.logMessage("Server returned an error: "
                    + "for task : '" + taskName + "'. (" + response.toString()
                    + ")." + " Check logs on server.");
                  ok = false;
                } else {
                  if (response instanceof TaskStatusInfo) {
                    resultInfo = ((TaskStatusInfo) response);
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
                        BufferedReader br = new BufferedReader(
                          new StringReader(message));
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
                        TaskStatusInfo taskResult = getResultFromServer(taskName);

                        // see if there are any results...
                        if (taskResult != null
                          && taskResult.getTaskResult() != null) {

                          if (taskResult.getTaskResult() != null
                            && taskResult.getTaskResult() instanceof Map) {
                            Map<String, List<EventObject>> results = (Map<String, List<EventObject>>) taskResult
                              .getTaskResult();

                            String justName = taskName.replace(
                              FlowRunnerRemote.NAME_PREFIX, "");
                            justName = justName.substring(0, justName
                              .indexOf(WekaTaskMap.s_taskNameIDSeparator));
                            // System.out.println("Looking for tab: " +
                            // justName);
                            int flowIndex = -1;
                            for (int i = 0; i < m_mainPerspective.getNumTabs(); i++) {
                              if (m_mainPerspective.getTabTitle(i).equals(
                                justName)) {
                                flowIndex = i;
                                break;
                              }
                            }
                            if (flowIndex >= 0) {
                              Vector beans = BeanInstance
                                .getBeanInstances(flowIndex);
                              if (beans != null) {
                                for (int i = 0; i < beans.size(); i++) {
                                  BeanInstance temp = (BeanInstance) beans
                                    .get(i);
                                  if (temp.getBean() instanceof HeadlessEventCollector
                                    && temp.getBean() instanceof BeanCommon) {
                                    String beanName = ((BeanCommon) temp
                                      .getBean()).getCustomName();
                                    // System.out.println("Looking for " +
                                    // beanName + " in result Map");
                                    if (results.containsKey(beanName)) {
                                      // System.out.println("Processing headless events......");
                                      List<EventObject> headlessEvents = results
                                        .get(beanName);
                                      ((HeadlessEventCollector) temp.getBean())
                                        .processHeadlessEvents(headlessEvents);
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  } else {
                    theLog.logMessage("A problem occurred while "
                      + "trying to retrieve remote status of task : '"
                      + taskName + "'. Check logs on server.");
                    theLog.statusMessage("[Remote Excecution]|ERROR: task "
                      + "failed to return the correct task status object type"
                      + " - see log (and/or log on server)");
                    ok = false;
                  }
                }
              }

              if (is != null) {
                try {
                  is.close();
                  is = null;
                } catch (IOException e) {
                  e.printStackTrace();
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
            if (is != null) {
              try {
                is.close();
                is = null;
              } catch (IOException e) {
                e.printStackTrace();
              }
            }

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

  /** A reference to the main KF perspective */
  protected KnowledgeFlowApp.MainKFPerspective m_mainPerspective;

  /** A panel for configuring hostname and port */
  protected HostPanel m_hostPanel = new HostPanel();

  /** A panel for configuring a schedule */
  protected SchedulePanel m_schedulePanel = new SchedulePanel();

  /** A panel for initiating monitoring of task(s) */
  protected MonitorPanel m_monitorPanel = new MonitorPanel();

  /** Username for authenticating with the server (if necessary) */
  protected String m_username;

  /** Password for authenticating with the server (if necessary) */
  protected String m_password;

  /**
   * Construct the perspective
   */
  public KnowledgeFlowRemoteSchedulerPerspective() {
    setLayout(new BorderLayout());

    JPanel holderPanel = new JPanel();
    holderPanel.setLayout(new BorderLayout());
    holderPanel.add(m_hostPanel, BorderLayout.NORTH);
    holderPanel.add(m_schedulePanel, BorderLayout.SOUTH);

    add(holderPanel, BorderLayout.NORTH);
    add(m_monitorPanel, BorderLayout.CENTER);

    String wekaServerPasswordPath = WekaPackageManager.WEKA_HOME.toString()
      + File.separator + "server" + File.separator + "weka.pwd";
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
  }

  protected String constructURL(String serviceAndArguments) {
    String realHostname = m_hostPanel.getHostName();
    String realPort = m_hostPanel.getPort();
    try {
      realHostname = Environment.getSystemWide().substitute(
        m_hostPanel.getHostName());
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
   * Returns true if this perspective can accept instances from the main
   * perspective
   * 
   * @return false - this perspective does not use/require instances
   */
  @Override
  public boolean acceptsInstances() {
    return false;
  }

  /**
   * Get the Icon for this perspective
   * 
   * @return the icon for this perspective
   */
  @Override
  public Icon getPerspectiveIcon() {
    java.awt.Image pic = null;
    java.net.URL imageURL = this.getClass().getClassLoader()
      .getResource("weka/gui/beans/icons/calendar.png");

    if (imageURL == null) {
      return null;
    } else {
      pic = java.awt.Toolkit.getDefaultToolkit().getImage(imageURL);
    }
    return new javax.swing.ImageIcon(pic);
  }

  /**
   * Get the tool tip text for this perspective.
   * 
   * @return the tool tip text for this perspective
   */
  @Override
  public String getPerspectiveTipText() {
    return "Schedule knowledge flows to run on remote hosts";
  }

  /**
   * Get the title of this perspective
   * 
   * @return the title of this perspective
   */
  @Override
  public String getPerspectiveTitle() {
    return "Remote host scheduler";
  }

  /**
   * Set active status of this perspective. True indicates that this perspective
   * is the visible active perspective in the KnowledgeFlow
   * 
   * @param active true if this perspective is the active one
   */
  @Override
  public void setActive(boolean active) {
  }

  /**
   * Set instances (if the perspective accepts them)
   * 
   * @param insts the instances
   */
  @Override
  public void setInstances(Instances insts) throws Exception {
    // Nothing to do
  }

  /**
   * Set whether this perspective is "loaded" - i.e. whether or not the user has
   * opted to have it available in the perspective toolbar. The perspective can
   * make the decision as to allocating or freeing resources on the basis of
   * this.
   * 
   * @param loaded true if the perspective is available in the perspective
   *          toolbar of the KnowledgeFlow
   */
  @Override
  public void setLoaded(boolean loaded) {
  }

  /**
   * Set a reference to the main KnowledgeFlow perspective - i.e. the
   * perspective that manages flow layouts.
   * 
   * @param mainPerspective the main KnowledgeFlow perspective.
   */
  @Override
  public void setMainKFPerspective(MainKFPerspective mainPerspective) {
    m_mainPerspective = mainPerspective;
  }

  /**
   * Main method for testing this class
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    final javax.swing.JFrame jf = new javax.swing.JFrame();
    jf.getContentPane().setLayout(new java.awt.BorderLayout());
    KnowledgeFlowRemoteSchedulerPerspective p = new KnowledgeFlowRemoteSchedulerPerspective();

    jf.getContentPane().add(p, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        jf.dispose();
        System.exit(0);
      }
    });
    jf.setSize(800, 600);
    jf.setVisible(true);
  }
}
