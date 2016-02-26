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
 *    Schedule.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

/**
 * Class for configuring a recurring or one-off schedule.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class Schedule implements Serializable, OptionHandler {
  /**
   * For serialization
   */
  private static final long serialVersionUID = 5509019709958908934L;

  /** Start date */
  protected Date m_start;

  /** End date */
  protected Date m_end;

  /** Repeat on this schedule */
  protected int m_repeatValue;

  /** Day of the week */
  protected List<Integer> m_dayOfTheWeek = new ArrayList<Integer>();

  /** Day of the month */
  protected int m_dayOfTheMonth;

  /** Month of the year */
  protected int m_monthOfTheYear;

  /** Default formatting string for dates */
  protected String m_dateFormat = "yyyy-MM-dd'T'HH:mm";

  public static enum Repeat {
    ONCE("once"), MINUTES("minutes"), HOURS("hours"), DAYS("days"), WEEKLY(
      "weekly"), MONTHLY("monthly"), YEARLY("yearly");

    private final String m_stringVal;

    Repeat(String name) {
      m_stringVal = name;
    }

    @Override
    public String toString() {
      return m_stringVal;
    }

    public static Repeat stringToValue(String r) {
      for (Repeat rp : Repeat.values()) {
        if (rp.toString().equalsIgnoreCase(r)) {
          return rp;
        }
      }
      return null;
    }
  }

  public static enum OccurrenceWithinMonth {
    FIRST("first"), SECOND("second"), THIRD("third"), FOURTH("fourth"), FIFTH(
      "fifth"), LAST("last");

    private final String m_stringVal;

    OccurrenceWithinMonth(String name) {
      m_stringVal = name;
    }

    @Override
    public String toString() {
      return m_stringVal;
    }

    public static OccurrenceWithinMonth stringToValue(String o) {
      for (OccurrenceWithinMonth m : OccurrenceWithinMonth.values()) {
        if (m.toString().equalsIgnoreCase(o)) {
          return m;
        }
      }
      return null;
    }
  }

  protected Repeat m_repeat = Repeat.ONCE;
  protected OccurrenceWithinMonth m_occurrence = null;

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> newVector = new Vector<Option>();

    newVector.add(new Option(
      "\tSet the start date (e.g. -start 2011-08-28T13:45)", "start", 1,
      "-start <date>"));
    newVector.add(new Option(
      "\tSet the end date (e.g. -start 2011-08-31T9:30)", "end", 1,
      "-end <date>"));
    newVector.add(new Option(
      "\tSet the repeat unit (e.g. once, minutes, hours, "
        + "days, weekly, monthly or yearly)", "repeat-unit", 1,
      "-repeat-unit <unit>"));
    newVector.add(new Option("\tSet the repeat value", "repeat-value", 1,
      "-repeat-value <value>"));
    newVector.add(new Option("\tSet the day of the week (weekly and monthly "
      + "repeat units only. E.g mon, tue etc.)", "day-of-week", 1,
      "-day-of-week <day>"));
    newVector.add(new Option("\tSet the month of the year (yearly "
      + "repeat units only. E.g. jan, feb, etc.)", "month-of-year", 1,
      "-month-of-year <month>"));
    newVector.add(new Option(
      "\tSet the day of the month (monthly repeat units "
        + "only. Execute on this day of every month. Range 1 - 31)",
      "day-of-month", 1, "-day-of-month <day number>"));
    newVector.add(new Option(
      "\tSet occurrence within month. Use in conjunction "
        + "with -day-of-week. Overrides -day-of-month. (first, second, "
        + "third, fourth, fifth, or last. E.g. to execute on the third "
        + "sunday of every month: -day-of-week sun -occurrence third",
      "occurrence", 1, "-occurrence <occurrence>"));

    return newVector.elements();
  }

  @Override
  public String[] getOptions() {
    // TODO Auto-generated method stub
    ArrayList<String> options = new ArrayList<String>();

    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.applyPattern(m_dateFormat);

    if (m_start != null) {
      options.add("-start");
      options.add(sdf.format(m_start));
    }

    if (m_end != null) {
      options.add("-end");
      options.add(sdf.format(m_end));
    }

    options.add("-repeat-unit");
    options.add(m_repeat.toString());

    if (m_repeatValue > 0) {
      options.add("-repeat-value");
      options.add("" + m_repeatValue);
    }

    if (m_dayOfTheWeek.size() > 0) {

      for (Integer dayOfTheWeek : m_dayOfTheWeek) {
        if (dayOfTheWeek >= Calendar.SUNDAY
          && dayOfTheWeek <= Calendar.SATURDAY
          && (m_repeat == Repeat.WEEKLY || m_repeat == Repeat.MONTHLY || m_repeat == Repeat.YEARLY)) {
          options.add("-day-of-week");
          switch (dayOfTheWeek) {
          case Calendar.SUNDAY:
            options.add("sun");
            break;
          case Calendar.MONDAY:
            options.add("mon");
            break;
          case Calendar.TUESDAY:
            options.add("tue");
            break;
          case Calendar.WEDNESDAY:
            options.add("wed");
            break;
          case Calendar.THURSDAY:
            options.add("thu");
            break;
          case Calendar.FRIDAY:
            options.add("fri");
            break;
          case Calendar.SATURDAY:
            options.add("sat");
            break;
          }
        }
      }
    }

    if (m_monthOfTheYear >= Calendar.JANUARY
      && m_monthOfTheYear <= Calendar.DECEMBER && m_repeat == Repeat.YEARLY) {
      options.add("-month-of-year");
      switch (m_monthOfTheYear) {
      case Calendar.JANUARY:
        options.add("jan");
        break;
      case Calendar.FEBRUARY:
        options.add("feb");
        break;
      case Calendar.MARCH:
        options.add("mar");
        break;
      case Calendar.APRIL:
        options.add("apr");
        break;
      case Calendar.MAY:
        options.add("may");
        break;
      case Calendar.JUNE:
        options.add("jun");
        break;
      case Calendar.JULY:
        options.add("jul");
        break;
      case Calendar.AUGUST:
        options.add("aug");
        break;
      case Calendar.SEPTEMBER:
        options.add("sep");
        break;
      case Calendar.OCTOBER:
        options.add("oct");
        break;
      case Calendar.NOVEMBER:
        options.add("nov");
        break;
      case Calendar.DECEMBER:
        options.add("dec");
        break;
      }
    }

    if (m_occurrence != null) {
      options.add("-occurrence");
      options.add(m_occurrence.toString());
    }

    if (m_dayOfTheMonth > 0 && m_dayOfTheMonth <= 31 && m_occurrence == null
      && m_repeat == Repeat.MONTHLY) {
      options.add("day-of-month");
      options.add("" + m_dayOfTheMonth);
    }

    return options.toArray(new String[1]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.applyPattern(m_dateFormat);
    String startD = Utils.getOption("start", options);

    m_start = null;
    if (startD.length() > 0) {
      m_start = sdf.parse(startD);
    }

    m_end = null;
    String endD = Utils.getOption("end", options);
    if (endD.length() > 0) {
      m_end = sdf.parse(endD);
    }

    String repeatU = Utils.getOption("repeat-unit", options);
    repeatU = repeatU.toLowerCase();
    if (repeatU.length() > 0) {
      for (Repeat r : Repeat.values()) {
        if (r.toString().equals(repeatU)) {
          m_repeat = r;
          break;
        }
      }
    }

    String repeatV = Utils.getOption("repeat-value", options);
    if (repeatV.length() > 0) {
      m_repeatValue = Integer.parseInt(repeatV);
    }

    String dow = null;
    while ((dow = Utils.getOption("day-of-week", options)).length() > 0) {
      dow = dow.toLowerCase();
      if (dow.length() > 0) {
        m_dayOfTheWeek.add(stringDOWToCalendarDOW(dow));
      }
    }

    String moy = Utils.getOption("month-of-year", options);
    moy = moy.toLowerCase();
    if (moy.length() > 0) {
      m_monthOfTheYear = stringMonthToCalendarMonth(moy);
    }

    String dom = Utils.getOption("day-of-month", options);
    dom = dom.toLowerCase();
    if (dom.length() > 0) {
      m_dayOfTheMonth = Integer.parseInt(dom);
    }

    String occur = Utils.getOption("occurrence", options);
    occur = occur.toLowerCase();
    if (occur.length() > 0) {
      for (OccurrenceWithinMonth o : OccurrenceWithinMonth.values()) {
        if (o.toString().equals(occur)) {
          m_occurrence = o;
          break;
        }
      }
    }

    Utils.checkForRemainingOptions(options);

    String configCheck = checkConfig();
    if (configCheck.length() > 0) {
      throw new Exception("Problem with configuration of Schedule: "
        + configCheck);
    }
  }

  public static int stringMonthToCalendarMonth(String moy) {
    moy = moy.toLowerCase().trim();
    if (moy.equals("jan") || moy.equals("january")) {
      return Calendar.JANUARY;
    } else if (moy.equals("feb") || moy.equals("february")) {
      return Calendar.FEBRUARY;
    } else if (moy.equals("mar") || moy.equals("march")) {
      return Calendar.MARCH;
    } else if (moy.equals("apr") || moy.equals("april")) {
      return Calendar.APRIL;
    } else if (moy.equals("may")) {
      return Calendar.MAY;
    } else if (moy.equals("jun") || moy.equals("june")) {
      return Calendar.JUNE;
    } else if (moy.equals("jul") || moy.equals("july")) {
      return Calendar.JULY;
    } else if (moy.equals("aug") || moy.equals("august")) {
      return Calendar.AUGUST;
    } else if (moy.equals("sep") || moy.equals("september")) {
      return Calendar.SEPTEMBER;
    } else if (moy.equals("oct") || moy.equals("october")) {
      return Calendar.OCTOBER;
    } else if (moy.equals("nov") || moy.equals("november")) {
      return Calendar.NOVEMBER;
    } else if (moy.equals("dec") || moy.equals("december")) {
      return Calendar.DECEMBER;
    }

    throw new IllegalArgumentException("Unrecognised month of the year");
  }

  public static String calendarMonthToString(int moy) {
    String result = null;
    if (moy == Calendar.JANUARY) {
      result = "jan";
    } else if (moy == Calendar.FEBRUARY) {
      result = "feb";
    } else if (moy == Calendar.MARCH) {
      result = "mar";
    } else if (moy == Calendar.APRIL) {
      result = "apr";
    } else if (moy == Calendar.MAY) {
      result = "may";
    } else if (moy == Calendar.JUNE) {
      result = "jun";
    } else if (moy == Calendar.JULY) {
      result = "jul";
    } else if (moy == Calendar.AUGUST) {
      result = "aug";
    } else if (moy == Calendar.SEPTEMBER) {
      result = "sep";
    } else if (moy == Calendar.OCTOBER) {
      result = "oct";
    } else if (moy == Calendar.NOVEMBER) {
      result = "nov";
    } else if (moy == Calendar.DECEMBER) {
      result = "dec";
    }

    return result;
  }

  public static int stringDOWToCalendarDOW(String dow) {
    dow = dow.toLowerCase().trim();
    if (dow.startsWith("mon")) {
      return Calendar.MONDAY;
    } else if (dow.startsWith("tue")) {
      return Calendar.TUESDAY;
    } else if (dow.startsWith("wed")) {
      return Calendar.WEDNESDAY;
    } else if (dow.startsWith("thu")) {
      return Calendar.THURSDAY;
    } else if (dow.startsWith("fri")) {
      return Calendar.FRIDAY;
    } else if (dow.startsWith("sat")) {
      return Calendar.SATURDAY;
    } else if (dow.startsWith("sun")) {
      return Calendar.SUNDAY;
    }
    throw new IllegalArgumentException("Unrecogised day of the week");
  }

  public static String calendarDOWToString(int dow) {
    String result = null;
    if (dow == Calendar.MONDAY) {
      result = "mon";
    } else if (dow == Calendar.TUESDAY) {
      result = "tue";
    } else if (dow == Calendar.WEDNESDAY) {
      result = "wed";
    } else if (dow == Calendar.THURSDAY) {
      result = "thu";
    } else if (dow == Calendar.FRIDAY) {
      result = "fri";
    } else if (dow == Calendar.SATURDAY) {
      result = "sat";
    } else if (dow == Calendar.SUNDAY) {
      result = "sun";
    }
    return result;
  }

  public void setStartDate(Date d) {
    m_start = d;
  }

  public Date getStartDate() {
    return m_start;
  }

  public void setEndDate(Date end) {
    m_end = end;
  }

  public Date getEndDate() {
    return m_end;
  }

  public void setRepeatUnit(Repeat p) {
    m_repeat = p;
  }

  public Repeat getRepeatUnit() {
    return m_repeat;
  }

  public void setRepeatValue(int r) {
    m_repeatValue = r;
  }

  public int getRepeatValue() {
    return m_repeatValue;
  }

  public void setDayOfTheWeek(List<Integer> dow) {
    m_dayOfTheWeek = dow;
  }

  public List<Integer> getDayOfTheWeek() {
    return m_dayOfTheWeek;
  }

  public void setDayOfTheMonth(int dom) {
    m_dayOfTheMonth = dom;
    m_occurrence = null;
  }

  public int getDayOfTheMonth() {
    return m_dayOfTheMonth;
  }

  public void setOccurrenceWithinMonth(OccurrenceWithinMonth o) {
    m_occurrence = o;
  }

  public OccurrenceWithinMonth getOccurrenceWithinMonth() {
    return m_occurrence;
  }

  public void setMonthOfTheYear(int m) {
    m_monthOfTheYear = m;
  }

  public int getMonthOfTheYear() {
    return m_monthOfTheYear;
  }

  public String checkConfig() {

    if (m_start != null && m_end != null) {
      if (m_end.before(m_start)) {
        return "Can't have an end time that occurs before a start time!";
      }
    }

    switch (m_repeat) {
    case ONCE:
      if (m_start == null) {
        return "No start date set!";
      }
      return "";
    case MINUTES:
      if (m_repeatValue <= 0 || m_repeatValue > 60) {
        return "Repeat of " + m_repeatValue
          + " is out of range for MINUTES repeat unit!";
      }
      return "";
    case HOURS:
      if (m_repeatValue <= 0 || m_repeatValue > 60) {
        return "Repeat of " + m_repeatValue
          + " is out of range for HOURS repeat unit!";
      }
      return "";
    case DAYS:
      if (m_repeatValue <= 0) {
        return "Daily repeat value must be greater than 0!";
      }
      return "";
    case WEEKLY:
      for (Integer dayOfTheWeek : m_dayOfTheWeek) {
        if (dayOfTheWeek < Calendar.SUNDAY || dayOfTheWeek > Calendar.SATURDAY) {
          return "Day of the week is out of range!";
        }
      }
      return "";
    case MONTHLY:
      if (m_occurrence != null) {
        if (m_dayOfTheWeek.size() > 1) {
          return "Can only specify one day of the week for MONTHLY occurrence "
            + "of a day!";
        }
        if (m_dayOfTheWeek.size() == 0) {
          return "Must specify a day of the week for MONTHLY occurrence "
            + "of a day!";
        }
        if (m_dayOfTheWeek.get(0) < Calendar.SUNDAY
          || m_dayOfTheWeek.get(0) > Calendar.SATURDAY) {
          return "Day of the week is out of range for MONTHLY occurrence of a day!";
        }
      } else {
        if (m_dayOfTheMonth < 1 || m_dayOfTheMonth > 31) {
          return "Day of the month is out of range (1 - 31) for MONTHLY repeat unit!";
        }
      }
      return "";
    case YEARLY:
      if (m_occurrence == null) {
        return "Must specify a occurrence value for YEARLY repeat unit!";
      }
      if (m_monthOfTheYear < Calendar.JANUARY
        || m_monthOfTheYear > Calendar.DECEMBER) {
        return "Month of the year is out of range for YEARLY repeat unit!";
      }
      if (m_dayOfTheWeek.size() > 1) {
        return "Can only specify one day of the week for YEARLY repeat unit!";
      }
      if (m_dayOfTheWeek.size() == 0) {
        return "Must specify a day of the week for YEARLY repeat unit! ";
      }

      if (m_dayOfTheWeek.get(0) < Calendar.SUNDAY
        || m_dayOfTheWeek.get(0) > Calendar.SATURDAY) {
        return "Day of the week is out of range for YEARLY repeat unit!";
      }
    }

    return "";
  }

  private boolean dayOfWeekInList(int dow) {

    for (Integer d : m_dayOfTheWeek) {
      if (d.intValue() == dow) {
        return true;
      }
    }

    return false;
  }

  /**
   * Get the next execution with respect to this schedule and the supplied date
   * of the last execution (may be null if there has been no previous
   * execution).
   * 
   * @param lastExecution date of the last execution or null (if no previous
   *          execution)
   * @return the date of the next execution
   */
  public Date nextExecution(Date lastExecution) {

    if (m_repeat == Repeat.ONCE) {
      if (lastExecution == null) {
        Date nextE = new Date();
        if (m_start != null && nextE.after(m_start)) {
          return nextE;
        }

        if (m_start != null && nextE.before(m_start)) {
          return m_start;
        }

        return null;
      }
    }

    if (m_repeat == Repeat.MINUTES || m_repeat == Repeat.HOURS
      || m_repeat == Repeat.DAYS) {
      if (lastExecution == null) {
        if (m_start == null) {
          return null;
        }

        Date nextE = new Date();
        if (nextE.after(m_start)) {
          return nextE;
        }
        return m_start;
      }

      long offset = getOffsetInMilliseconds();
      long last = lastExecution.getTime();
      Date nextE = new Date(last + offset);
      if (m_start != null && nextE.before(m_start)) {
        // this can't really happen if we've already executed previously...
        return null;
      }

      if (m_end != null && nextE.after(m_end)) {
        return null;
      }

      Date now = new Date();
      if (m_end != null && now.after(m_end)) {
        return null;
      }

      return nextE;
    }

    if (m_repeat == Repeat.WEEKLY) {
      if (lastExecution == null) {
        // need to find the specified day of the week
        Date now = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(now);
        if (m_start != null) {
          // set the hour of the day and minute
          GregorianCalendar temp = new GregorianCalendar();
          temp.setTime(m_start);
          cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
          cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
        }

        // while (cal.get(Calendar.DAY_OF_WEEK) != m_dayOfTheWeek) {
        while (!dayOfWeekInList(cal.get(Calendar.DAY_OF_WEEK))) {
          cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (m_start != null && cal.getTime().before(m_start)) {
          while (cal.getTime().before(m_start)) {
            cal.add(Calendar.DAY_OF_YEAR, 7);
          }
          return cal.getTime();
        }

        if (m_end != null && cal.getTime().after(m_end)) {
          return null;
        }

        return cal.getTime();
      } else {
        /*
         * GregorianCalendar lastCal = new GregorianCalendar();
         * lastCal.setTime(new Date(lastExecution.getTime()));
         */

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(lastExecution.getTime()));

        // advance 1 day beyond last execution
        cal.add(Calendar.DAY_OF_YEAR, 1);

        // advance until the day of the week is in the list
        while (!dayOfWeekInList(cal.get(Calendar.DAY_OF_WEEK))) {
          cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // cal.add(Calendar.DAY_OF_YEAR, 7);

        Date now = new Date();
        if (now.after(cal.getTime())) {
          // we're already past the next execution time -
          // set to now and then check against the day of the
          // week. Add one week if we're already past the
          // specified day
          int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
          int minOfHour = cal.get(Calendar.MINUTE);
          cal.setTime(now);

          // make sure we get the right hour and minute
          cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
          cal.set(Calendar.MINUTE, minOfHour);

          while (!dayOfWeekInList(cal.get(Calendar.DAY_OF_WEEK))) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
          }
          /*
           * if (cal.get(Calendar.DAY_OF_WEEK) > m_dayOfTheWeek) {
           * cal.add(Calendar.DAY_OF_YEAR, 7); } cal.set(Calendar.DAY_OF_WEEK,
           * m_dayOfTheWeek);
           */
        }

        if (m_end != null && cal.getTime().after(m_end)) {
          return null;
        }

        return cal.getTime();
      }
    }

    if (m_repeat == Repeat.MONTHLY) {
      if (m_occurrence == null) {
        if (lastExecution == null) {
          Date now = new Date();
          GregorianCalendar cal = new GregorianCalendar();
          cal.setTime(now);
          cal.set(Calendar.DAY_OF_MONTH, m_dayOfTheMonth);
          if (m_start != null) {
            // make sure we set the right hour and minute
            GregorianCalendar temp = new GregorianCalendar();
            temp.setTime(m_start);
            cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
          }

          // if now is after the specified day of the month
          // for this month then next execution will be next
          // month
          now = new Date();
          if (cal.getTime().before(now)) {
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, m_dayOfTheMonth);
          }

          if (m_start != null && cal.getTime().before(m_start)) {
            return null;
          }
          if (m_end != null && cal.getTime().after(m_end)) {
            return null;
          }
          return cal.getTime();
        } else {
          GregorianCalendar cal = new GregorianCalendar();
          cal.setTime(new Date(lastExecution.getTime()));
          cal.add(Calendar.MONTH, 1);
          cal.set(Calendar.DAY_OF_MONTH, m_dayOfTheMonth);

          Date now = new Date();
          if (cal.getTime().before(now)) {
            // if now is after next execution then add 1 to month
            cal.setTime(now);
            if (m_dayOfTheMonth < cal.get(Calendar.DAY_OF_MONTH)) {
              cal.add(Calendar.MONTH, 1);
            }
            cal.set(Calendar.DAY_OF_MONTH, m_dayOfTheMonth);

            // set the right hour and minute
            GregorianCalendar temp = new GregorianCalendar();
            temp.setTime(new Date(lastExecution.getTime()));
            cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
          }

          if (m_start != null && cal.getTime().before(m_start)) {
            // can't really happen if we've already executed at least once
            // previously...
            return null;
          }
          if (m_end != null && cal.getTime().after(m_end)) {
            return null;
          }

          return cal.getTime();
        }
      } else {
        Date d = null;
        if (lastExecution == null) {
          d = new Date();

          // get the right hour and minute
          if (m_start != null) {
            GregorianCalendar temp = new GregorianCalendar();
            temp.setTime(new Date(m_start.getTime()));
            GregorianCalendar temp2 = new GregorianCalendar();
            temp2.setTime(d);
            temp2.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
            temp2.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
            d = new Date(temp2.getTime().getTime());
          }
        } else {
          d = new Date(lastExecution.getTime()); // should have the correct hour
                                                 // and min
          GregorianCalendar temp = new GregorianCalendar();
          temp.setTime(d);

          // next month, set to first of the month
          temp.add(Calendar.MONTH, 1);
          temp.set(Calendar.DAY_OF_MONTH, 1);
          d = new Date(temp.getTime().getTime());
        }

        if (m_end != null && d.after(m_end)) {
          return null;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(d);
        if (m_start != null && d.before(m_start)) {

          // still before the start? Set to first day of the start month
          cal.setTime(m_start);
          cal.set(Calendar.DAY_OF_MONTH, 1);
        }

        cal = setRequestedDayInMonth(cal);

        Date now = new Date();
        if (now.after(cal.getTime())) {
          // next execution is in the past - move to this month
          cal.setTime(new Date());
          cal.set(Calendar.DAY_OF_MONTH, 1);
          cal = setRequestedDayInMonth(cal);

          if (now.after(cal.getTime())) {
            // the present is still after the next execution -
            // move to next month
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal = setRequestedDayInMonth(cal);
          }
        }

        // check that we're still in range
        if (m_end != null && cal.getTime().after(m_end)) {
          return null;
        }
        return cal.getTime();
      }
    }

    if (m_repeat == Repeat.YEARLY) {
      if (m_occurrence == null) {
        return null;
      }

      if (lastExecution == null) {
        Date d = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.set(Calendar.MONTH, m_monthOfTheYear);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        cal = setRequestedDayInMonth(cal);
        if (m_start != null) {
          // make sure we get the correct hour and minute
          GregorianCalendar temp = new GregorianCalendar();
          temp.setTime(new Date(m_start.getTime()));
          cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
          cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
        }

        if (cal.getTime().before(new Date())) {
          // requested time is before now, so add one year
          cal.add(Calendar.YEAR, 1);
        }

        if (m_start != null && cal.getTime().before(m_start)) {
          // next execution will be this time in the following year
          cal.add(Calendar.YEAR, 1);
          cal.set(Calendar.MONTH, m_monthOfTheYear);
          cal.set(Calendar.DAY_OF_MONTH, 1);
          cal = setRequestedDayInMonth(cal);
        }

        if (m_end != null && cal.getTime().after(m_end)) {
          return null;
        }
        return cal.getTime();
      }

      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime(new Date(lastExecution.getTime())); // should have correct
                                                      // hour and min
      cal.add(Calendar.YEAR, 1);
      cal.set(Calendar.MONTH, m_monthOfTheYear);
      cal.set(Calendar.DAY_OF_MONTH, 1);
      cal = setRequestedDayInMonth(cal);

      Date now = new Date();
      if (now.after(cal.getTime())) {
        // now is after next execution time
        cal.setTime(new Date());
        cal.set(Calendar.MONTH, m_monthOfTheYear);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal = setRequestedDayInMonth(cal);

        // make sure we get the correct hour and minute
        GregorianCalendar temp = new GregorianCalendar();
        temp.setTime(new Date(lastExecution.getTime()));
        cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));

        if (now.after(cal.getTime())) {
          // need to add 1 year
          cal.add(Calendar.YEAR, 1);
          cal.set(Calendar.MONTH, m_monthOfTheYear);
          cal.set(Calendar.DAY_OF_MONTH, 1);
          cal = setRequestedDayInMonth(cal);
        }
      }

      if (m_end != null && cal.getTime().after(m_end)) {
        return null;
      }
      return cal.getTime();
    }

    return null;
  }

  private GregorianCalendar setRequestedDayInMonth(GregorianCalendar cal) {
    int numOccurrences = 0;
    int month = cal.get(Calendar.MONTH);
    boolean done = false;
    while (cal.get(Calendar.DAY_OF_MONTH) <= 31
      && cal.get(Calendar.MONTH) == month) {
      if (cal.get(Calendar.DAY_OF_WEEK) == m_dayOfTheWeek.get(0)) {
        numOccurrences++;

        switch (m_occurrence) {
        case FIRST:
          done = (numOccurrences == 1);
          break;
        case SECOND:
          done = (numOccurrences == 2);
          break;
        case THIRD:
          done = (numOccurrences == 3);
          break;
        case FOURTH:
          done = (numOccurrences == 4);
          break;
        case FIFTH:
          done = (numOccurrences == 5);
          break;
        case LAST:
          GregorianCalendar temp = new GregorianCalendar();
          temp.setTime(new Date(cal.getTimeInMillis()));
          temp.add(Calendar.WEEK_OF_YEAR, 1);
          if (temp.get(Calendar.MONTH) != month) {
            // must have been the last because the month has changed
            done = true;
          }
          break;
        }
      }

      if (done) {
        break;
      }

      cal.add(Calendar.DAY_OF_YEAR, 1);
    }

    return cal;
  }

  public boolean execute(Date lastExecution) throws Exception {
    // check our configuration first
    String configCheck = checkConfig();
    if (configCheck.length() > 0) {
      throw new Exception("Problem with configuration of Schedule: "
        + configCheck);
    }

    Date now = new Date();

    boolean result = false;

    switch (m_repeat) {
    case ONCE:
      return checkOnce(now, lastExecution);
    case MINUTES:
    case HOURS:
    case DAYS:
      return checkElapsedBased(now, lastExecution);
    case WEEKLY:
      return checkWeekly(now, lastExecution);
    case MONTHLY:
      return checkMonthly(now, lastExecution);
    case YEARLY:
      return checkYearly(now, lastExecution);
    }

    return result;
  }

  private boolean checkOnce(Date now, Date lastExecution) {
    if (lastExecution == null) {
      if (m_start == null || now.after(m_start)) {
        return true;
      }
    }

    return false;
  }

  private boolean checkYearly(Date now, Date lastExecution) {

    if (m_occurrence == null) {
      return false;
    }

    // check to see if the month matches first
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(now);
    int nowMonth = cal.get(Calendar.MONTH);
    if (nowMonth == m_monthOfTheYear) {

      // now does the particular day in the month (i.e. last wednesday of month)
      // match
      return checkMonthly(now, lastExecution);
    }

    return false;
  }

  private boolean checkMonthly(Date now, Date lastExecution) {
    if (m_start != null) {
      if (now.before(m_start)) {
        return false;
      }
    }

    if (m_end != null) {
      if (now.after(m_end)) {
        return false;
      }
    }

    GregorianCalendar cal = new GregorianCalendar();
    if (m_occurrence == null) {
      // specific day within the month

      if (lastExecution == null) {
        cal.setTime(now);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        if (dayOfMonth == m_dayOfTheMonth) {
          return true;
        }
        return false;
      } else {
        cal.setTime(now);
        int nowDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        if (nowDayOfMonth == m_dayOfTheMonth) {
          cal.setTime(lastExecution);
          int lastExecutionDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
          if (nowDayOfMonth != lastExecutionDayOfMonth) {
            return true;
          }
        }
        return false;
      }
    } else {
      // specific occurrence of a particular day in the month (e.g. second
      // tuesday)
      boolean ok = occurrenceInMonth(now);

      if (ok) {
        if (lastExecution == null) {
          return true;
        }
        cal.setTime(now);
        int nowDayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        cal.setTime(lastExecution);
        int lastDayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        if (nowDayOfYear != lastDayOfYear) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean occurrenceInMonth(Date d) {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(d);
    int dow = cal.get(Calendar.DAY_OF_WEEK);
    int nowDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

    if (dow != m_dayOfTheWeek.get(0)) {
      return false;
    }

    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH);
    int day = 1;
    GregorianCalendar cal2 = new GregorianCalendar();
    cal2.set(Calendar.YEAR, year);
    cal2.set(Calendar.MONTH, month);
    cal2.set(Calendar.DAY_OF_MONTH, day);

    int numOccurrences = 0;
    boolean last = false;
    while (cal2.get(Calendar.DAY_OF_MONTH) <= nowDayOfMonth) {
      if (cal2.get(Calendar.DAY_OF_WEEK) == m_dayOfTheWeek.get(0)) {
        numOccurrences++;
      }
      if (cal2.get(Calendar.DAY_OF_MONTH) == nowDayOfMonth) {
        cal2.add(Calendar.WEEK_OF_YEAR, 1);
        if (cal2.get(Calendar.MONTH) != cal.get(Calendar.MONTH)) {
          // we've advanced by 1 week and the month has changed, so that
          // must have been the last in the month
          last = true;
        }
        break;
      }

      cal.add(Calendar.DAY_OF_MONTH, 1);
    }

    switch (m_occurrence) {
    case FIRST:
      if (numOccurrences == 1) {
        return true;
      }
      break;
    case SECOND:
      if (numOccurrences == 2) {
        return true;
      }
      break;
    case THIRD:
      if (numOccurrences == 3) {
        return true;
      }
      break;
    case FOURTH:
      if (numOccurrences == 4) {
        return true;
      }
      break;
    case FIFTH:
      if (numOccurrences == 5) {
        return true;
      }
      break;
    case LAST:
      if (last) {
        return true;
      }
      break;
    }

    return false;
  }

  private boolean checkWeekly(Date now, Date lastExecution) {
    if (m_start != null) {
      if (now.before(m_start)) {
        return false;
      }
    }

    if (m_end != null) {
      if (now.after(m_end)) {
        return false;
      }
    }

    GregorianCalendar cal = new GregorianCalendar();
    if (lastExecution == null) {
      cal.setTime(now);
      int nowDay = cal.get(Calendar.DAY_OF_WEEK);
      // if (nowDay == m_dayOfTheWeek) {
      if (dayOfWeekInList(nowDay)) {
        return true;
      }
      return false;
    }
    cal.setTime(now);
    int nowDay = cal.get(Calendar.DAY_OF_WEEK);
    // if (nowDay == m_dayOfTheWeek) {
    if (dayOfWeekInList(nowDay)) {
      cal.setTime(lastExecution);
      int lastDay = cal.get(Calendar.DAY_OF_WEEK);

      // if the day of the last execution is not the same as today
      // then ok to run
      if (nowDay != lastDay) {
        return true;
      }
    }

    return false;
  }

  private boolean checkElapsedBased(Date now, Date lastExecution) {
    if (m_start != null) {
      if (now.before(m_start)) {
        return false;
      }
    }

    if (m_end != null) {
      if (now.after(m_end)) {
        return false;
      }
    }

    if (lastExecution == null) {
      return true;
    }

    long delta = getOffsetInMilliseconds();
    long lastMilli = lastExecution.getTime();
    lastMilli += delta;

    // subtract a few milliseconds off the delta. Since the schedule thread
    // runs every minute we want to be sure that the call to after() below
    // will return true for the actual minute that execution is scheduled for
    // (otherwise we'll be late by 1 minute)
    Date lastPlusDelta = new Date(lastMilli - 10L);
    if (now.after(lastPlusDelta)) {
      return true;
    }

    return false;
  }

  private long getOffsetInMilliseconds() {

    switch (m_repeat) {
    case MINUTES:
      return 60000L * m_repeatValue;
    case HOURS:
      return (60000L * 60L * m_repeatValue);
    case DAYS:
      return (60000L * 60L * 24L * m_repeatValue);
    }

    return 0L;
  }

  protected Date parseDate(Object date) throws ParseException {
    if (date == null) {
      return null;
    }

    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.applyPattern(m_dateFormat);

    return sdf.parse(date.toString());
  }
}
