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
 *    SchedulePanel.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import com.toedter.calendar.JDateChooser;
import weka.server.Schedule;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

/**
 * Class providing a panel for configuring task scheduling details
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SchedulePanel extends JPanel {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1319029770018027370L;

  /**
   * Inner class to the SchedulePanel providing a date time configuration
   * panel
   */
  protected static class DateTimePanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 5343421783518237483L;

    /**
     * A widget for selecting a date
     */
    protected JDateChooser m_dateChooser;
    /**
     * Combo for choosing the hour of the day
     */
    protected JComboBox m_hourCombo = new JComboBox();
    /**
     * Combo for choosing the minute of the hour
     */
    protected JComboBox m_minutesCombo = new JComboBox();
    /**
     * Radio button for selecting execution now (or no end date)
     */
    protected JRadioButton m_nowNoEndBut;
    /**
     * Radio button for selecting a specific date
     */
    protected JRadioButton m_dateBut = new JRadioButton("Date", false);

    /**
     * Construct a new DateTimePanel
     *
     * @param title title to display in the border of the panel
     * @param now   true if the now/no-end radio button should be labeled "now"
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
        @Override public void actionPerformed(ActionEvent e) {
          boolean enable = !m_nowNoEndBut.isSelected();

          m_dateChooser.setEnabled(enable);
          m_hourCombo.setEnabled(enable);
          m_minutesCombo.setEnabled(enable);
        }
      });

      m_dateBut.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
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
  protected static class RepeatPanel extends JPanel {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 749883190997695194L;

    /**
     * The enabled status of the entire panel
     */
    protected boolean m_globalEnabled = true;

    /**
     * Combo box for the repeat type
     */
    protected JComboBox m_repeatCombo = new JComboBox();

    /**
     * Panel to hold the specific configuration widgets for each repeat type
     */
    protected JPanel m_detailPanel = new JPanel();

    /**
     * Label for the interval
     */
    protected JLabel m_intervalLab = new JLabel("", SwingConstants.LEFT);

    /**
     * Text field for the interval
     */
    protected JTextField m_intervalField = new JTextField(5);

    /**
     * Labels for monthly details
     */
    protected JLabel m_ofEveryMonthLab = new JLabel("of every month");
    protected JLabel m_ofEveryMonthLab2 = new JLabel("of every month");

    /**
     * Weekly stuff
     */
    protected JCheckBox m_sunCheck = new JCheckBox("Sunday");
    protected JCheckBox m_monCheck = new JCheckBox("Monday");
    protected JCheckBox m_tueCheck = new JCheckBox("Tuesday");
    protected JCheckBox m_wedCheck = new JCheckBox("Wednesday");
    protected JCheckBox m_thuCheck = new JCheckBox("Thursday");
    protected JCheckBox m_friCheck = new JCheckBox("Friday");
    protected JCheckBox m_satCheck = new JCheckBox("Saturday");

    /**
     * Monthly stuff
     */
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
        @Override public void actionPerformed(ActionEvent e) {
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
        @Override public void actionPerformed(ActionEvent e) {
          checkEnabled();
        }
      });
      m_specificDayYearBut.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
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

      m_dayYearNumField.setEnabled(m_dayYearNumBut.isSelected() && m_globalEnabled);
      m_occurrenceInMonth.setEnabled(m_specificDayYearBut.isSelected() && m_globalEnabled);
      m_dayOfWeek.setEnabled(m_specificDayYearBut.isSelected() && m_globalEnabled);
      m_month
        .setEnabled(m_specificDayYearBut.isSelected() && m_globalEnabled);
      m_ofEveryMonthLab.setEnabled(m_dayYearNumBut.isSelected() && m_globalEnabled);
      m_ofEveryMonthLab2.setEnabled(m_specificDayYearBut.isSelected() && m_globalEnabled);

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
    @Override public void setEnabled(boolean enabled) {
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
            sched.setDayOfTheMonth(Integer.parseInt(m_dayYearNumField.getText()));
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

  /** A panel for configuring the start date/time */
  protected DateTimePanel m_startDate;

  /** A panel for configuring the end date/time */
  protected DateTimePanel m_endDate;

  /** A panel for configuring the repeat */
  protected RepeatPanel m_repeat = new RepeatPanel();

  /** A panel for specifying flow variables and their values */
  protected ParameterPanel m_parameters = new ParameterPanel();

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
    // add(m_launcher, BorderLayout.SOUTH);

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
   * Get the parameters panel
   *
   * @return the ParametersPanel
   */
  public ParameterPanel getParameterPanel() {
    return m_parameters;
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
