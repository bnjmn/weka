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
 *    CSVToARFFHeaderMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.Utils;
import au.com.bytecode.opencsv.CSVParser;

/**
 * A map task that processes incoming lines in CSV format and builds up header
 * information. Can be configured with information on which columns to force to
 * be nominal, string, date etc. Nominal values can be determined automatically
 * or pre-supplied by the user. In addition to determining the format of the
 * columns in the data it also can compute meta data such as means, modes,
 * counts, standard deviations etc. These statistics get encoded in special
 * "summary" attributes in the header file - one for each numeric or nominal
 * attribute in the data.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHeaderMapTask implements OptionHandler, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -3949274571568175413L;

  /**
   * Enumerated type for specifying the type of each attribute in the data
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected enum TYPE {
    UNDETERMINED, NUMERIC, NOMINAL, STRING, DATE;
  }

  /**
   * An enumerated utility type for the various numeric summary metrics that are
   * computed.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static enum ArffSummaryNumericMetric {
    COUNT("count") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(COUNT.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    SUM("sum") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(SUM.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    SUMSQ("sumSq") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(SUMSQ.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    MIN("min") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(SUM.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    MAX("max") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(MAX.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    MISSING("missing") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(MISSING.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    MEAN("mean") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(MEAN.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    },
    STDDEV("stdDev") {
      @Override
      public double valueFromAttribute(Attribute att) {
        String value = att.value(STDDEV.ordinal());
        return toValue(value, toString());
      }

      @Override
      public String makeAttributeValue(double value) {
        return toString() + value;
      }
    };

    private final String m_name;

    ArffSummaryNumericMetric(String name) {
      m_name = name + "_";
    }

    /**
     * Extracts the value of this particular metric from the summary Attribute
     * 
     * @param att the summary attribute to extract the metric from
     * @return the value of this particular metric
     */
    public abstract double valueFromAttribute(Attribute att);

    /**
     * Makes the internal encoded version of this metric given it's value as a
     * double
     * 
     * @param value the value of the metric
     * @return the internal representation of this metric
     */
    public abstract String makeAttributeValue(double value);

    @Override
    public String toString() {
      return m_name;
    }

    /**
     * Extracts the value of the metric from the string representation
     * 
     * @param v the string representation
     * @param name the name of the attribute that the metric belongs to
     * @return the value of the metric
     */
    double toValue(String v, String name) {
      v = v.replace(name, "");

      return Double.parseDouble(v);
    }
  }

  /**
   * Stats base class for the numeric and nominal summary meta data
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public abstract static class Stats implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 3662688283840145572L;

    /** The name of the attribute that this Stats pertains to */
    protected String m_attributeName = "";

    /**
     * Construct a new Stats
     * 
     * @param attributeName the name of the attribute that this Stats pertains
     *          to
     */
    public Stats(String attributeName) {
      m_attributeName = attributeName;
    }

    /**
     * Get the name of the attribute that this Stats pertains to
     * 
     * @return the name of the attribute
     */
    public String getName() {
      return m_attributeName;
    }

    /**
     * Makes a Attribute that encapsulates the meta data
     * 
     * @return an Attribute that encapsulates the meta data
     */
    public abstract Attribute makeAttribute();

  }

  /**
   * Class for computing numeric stats
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public static class NumericStats extends Stats implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 5328158049841703129L;

    /** Holds the actual stats values */
    protected double[] m_stats = new double[ArffSummaryNumericMetric.values().length];

    /**
     * Constructs a new NumericStats
     * 
     * @param attributeName the name of the attribute that these statistics are
     *          for
     */
    public NumericStats(String attributeName) {
      super(attributeName);

      m_stats[ArffSummaryNumericMetric.MIN.ordinal()] = Double.MAX_VALUE;
      m_stats[ArffSummaryNumericMetric.MAX.ordinal()] = Double.MIN_VALUE;
    }

    /**
     * Return the array of statistics
     * 
     * @return the array of statistics
     */
    public double[] getStats() {
      return m_stats;
    }

    @Override
    public Attribute makeAttribute() {
      ArrayList<String> vals = new ArrayList<String>();

      for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
        String v = m.makeAttributeValue(m_stats[m.ordinal()]);
        vals.add(v);
      }

      Attribute a = new Attribute(ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + m_attributeName, vals);
      return a;
    }

    /**
     * Convert a summary meta attribute into a NumericStats object
     * 
     * @param a the summary meta attribute to convert
     * @return a NumericStats instance
     * @throws IllegalArgumentException if a problem occurs
     */
    public static NumericStats attributeToStats(Attribute a)
      throws IllegalArgumentException {
      if (!a.isNominal()) {
        throw new IllegalArgumentException("Stats attribute is not nominal!");
      }

      if (a.numValues() != ArffSummaryNumericMetric.values().length) {
        throw new IllegalArgumentException("Was expecting there to be "
          + ArffSummaryNumericMetric.values().length
          + " values in a summary attribute, but found " + a.numValues());
      }

      double[] stats = new double[ArffSummaryNumericMetric.values().length];

      for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
        String v = a.value(m.ordinal());

        double value = m.toValue(v, m.toString());
        stats[m.ordinal()] = value;
      }

      NumericStats s = new NumericStats(a.name());
      s.m_stats = stats;

      return s;
    }

    /**
     * Compute the derived statistics
     */
    public void computeDerived() {
      double count = m_stats[ArffSummaryNumericMetric.COUNT.ordinal()];
      double sum = m_stats[ArffSummaryNumericMetric.SUM.ordinal()];
      double sumSq = m_stats[ArffSummaryNumericMetric.SUMSQ.ordinal()];
      double mean = 0;

      double stdDev = 0;
      if (count > 0) {
        mean = sum / count;
        stdDev = Double.POSITIVE_INFINITY;
        if (count > 1) {
          stdDev = sumSq - (sum * sum) / count;
          stdDev /= (count - 1);
          if (stdDev < 0) {
            stdDev = 0;
          }
          stdDev = Math.sqrt(stdDev);
        }
      }

      m_stats[ArffSummaryNumericMetric.MEAN.ordinal()] = mean;
      m_stats[ArffSummaryNumericMetric.STDDEV.ordinal()] = stdDev;
    }
  }

  /**
   * Class for computing nominal statistics (primarily frequency counts)
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public static class NominalStats extends Stats implements Serializable {

    /** A map of values to counts */
    protected Map<String, Count> m_counts = new TreeMap<String, Count>();

    /** The number of missing values for this nominal attribute */
    protected double m_numMissing;

    /** A "label" to use when storing the number of missing values */
    public static final String MISSING_LABEL = "**missing**";

    /** For serialization */
    private static final long serialVersionUID = -6176046647546730423L;

    /**
     * Class that encapsulates a count for nominal value
     * 
     * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
     */
    public static class Count implements Serializable {

      /** For serialization */
      private static final long serialVersionUID = 4310467271632108735L;

      /** The value of the count */
      public double m_count;
    }

    /**
     * Constructs a new NominalStats
     * 
     * @param attributeName the name of the data attribute that these stats
     *          pertain to
     */
    public NominalStats(String attributeName) {
      super(attributeName);
    }

    /**
     * Convert a summary meta attribute to a NominalStats
     * 
     * @param a the attribute to convert
     * @return a NominalStats
     * @throws IllegalArgumentException if a problem occurs
     */
    public static NominalStats attributeToStats(Attribute a)
      throws IllegalArgumentException {

      if (!a.isNominal()) {
        throw new IllegalArgumentException("Stats attribute is not nominal!");
      }

      NominalStats ns = new NominalStats(a.name());
      for (int j = 0; j < a.numValues(); j++) {
        String v = a.value(j);
        String label = v.substring(0, v.lastIndexOf("_"));
        String freqCount = v.substring(v.lastIndexOf("_") + 1, v.length());
        try {
          double fC = Double.parseDouble(freqCount);
          if (label.equals(CSVToARFFHeaderMapTask.NominalStats.MISSING_LABEL)) {
            ns.add(null, fC);
          } else {
            ns.add(label, fC);
          }
        } catch (NumberFormatException n) {
          throw new IllegalArgumentException(n);
        }
      }

      return ns;
    }

    /**
     * Adds to the count for a given label. If the label is null then it adds to
     * the count for missing.
     * 
     * @param label the label to add the count to
     * @param value the count to add
     */
    public void add(String label, double value) {

      if (label == null) {
        m_numMissing += value;
      } else {

        Count c = m_counts.get(label);
        if (c == null) {
          c = new Count();
          m_counts.put(label, c);
        }

        c.m_count += value;
      }
    }

    /**
     * Get the set of labels seen by this NominalStats
     * 
     * @return the set of labels
     */
    public Set<String> getLabels() {
      return m_counts.keySet();
    }

    /**
     * Get the count for a given label
     * 
     * @param label the label to get the count for
     * @return the count
     */
    public double getCount(String label) {
      Count c = m_counts.get(label);

      if (c == null) {
        return Double.NaN;
      }

      return c.m_count;
    }

    /**
     * Get the number of missing values for this attribute
     * 
     * @return the number of missing values seen
     */
    public double getNumMissing() {
      return m_numMissing;
    }

    @Override
    public Attribute makeAttribute() {
      ArrayList<String> vals = new ArrayList<String>();

      for (Map.Entry<String, Count> e : m_counts.entrySet()) {
        vals.add(e.getKey() + "_" + e.getValue().m_count);
      }

      vals.add(MISSING_LABEL + "_" + m_numMissing);

      Attribute a = new Attribute(ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + m_attributeName, vals);

      return a;
    }
  }

  /** Attribute name prefix for a summary statistics attribute */
  public static final String ARFF_SUMMARY_ATTRIBUTE_PREFIX = "arff_summary_";

  /** Attribute types for the incoming CSV columns */
  protected TYPE[] m_attributeTypes;

  /** A range of columns to force to be of type String */
  protected Range m_forceString = new Range();

  /** A range of columns to force to be of type Nominal */
  protected Range m_forceNominal = new Range();

  /** A range of columns to force to be of type Date */
  protected Range m_forceDate = new Range();

  /**
   * Holds the names of the incoming columns/attributes. Names will be generated
   * if not supplied by the user
   */
  protected List<String> m_attributeNames = new ArrayList<String>();

  /** The formatting string to use to parse dates */
  protected String m_dateFormat = "yyyy-MM-dd'T'HH:mm:ss";

  /** The formatter to use on dates */
  protected SimpleDateFormat m_formatter;

  /** The user-supplied legal nominal values - each entry in the list is a spec */
  protected List<String> m_nominalLabelSpecs = new ArrayList<String>();

  /** Lookup for nominal values */
  protected Map<Integer, TreeSet<String>> m_nominalVals = new HashMap<Integer, TreeSet<String>>();

  /** The placeholder for missing values. */
  protected String m_MissingValue = "?";

  /** enclosure character to use for strings - opencsv only allows one */
  protected String m_Enclosures = "\'";

  /** the field separator. */
  protected String m_FieldSeparator = ",";

  /** The CSV parser */
  protected CSVParser m_parser;

  /** Whether to compute summary statistics or not */
  protected boolean m_computeSummaryStats = true;

  /** A map of attribute names to summary statistics */
  protected Map<String, Stats> m_summaryStats = new HashMap<String, Stats>();

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option(
      "\tThe range of attributes to force type to be NOMINAL.\n"
        + "\t'first' and 'last' are accepted as well.\n"
        + "\tExamples: \"first-last\", \"1,4,5-27,50-last\"\n"
        + "\t(default: -none-)", "N", 1, "-N <range>"));

    result.add(new Option(
      "\tOptional specification of legal labels for nominal\n"
        + "\tattributes. May be specified multiple times.\n"
        + "\tBatch mode can determine this\n"
        + "\tautomatically (and so can incremental mode if\n"
        + "\tthe first in memory buffer load of instances\n"
        + "\tcontains an example of each legal value). The\n"
        + "\tspec contains two parts separated by a \":\". The\n"
        + "\tfirst part can be a range of attribute indexes or\n"
        + "\ta comma-separated list off attruibute names; the\n"
        + "\tsecond part is a comma-separated list of labels. E.g\n"
        + "\t\"1,2,4-6:red,green,blue\" or \"att1,att2:red,green," + "blue\"",
      "L", 1, "-L <nominal label spec>"));

    result.add(new Option(
      "\tThe range of attribute to force type to be STRING.\n"
        + "\t'first' and 'last' are accepted as well.\n"
        + "\tExamples: \"first-last\", \"1,4,5-27,50-last\"\n"
        + "\t(default: -none-)", "S", 1, "-S <range>"));

    result.add(new Option(
      "\tThe range of attribute to force type to be DATE.\n"
        + "\t'first' and 'last' are accepted as well.\n"
        + "\tExamples: \"first-last\", \"1,4,5-27,50-last\"\n"
        + "\t(default: -none-)", "D", 1, "-D <range>"));

    result.add(new Option(
      "\tThe date formatting string to use to parse date values.\n"
        + "\t(default: \"yyyy-MM-dd'T'HH:mm:ss\")", "format", 1,
      "-format <date format>"));

    result.add(new Option("\tThe string representing a missing value.\n"
      + "\t(default: ?)", "M", 1, "-M <str>"));

    result.add(new Option("\tThe field separator to be used.\n"
      + "\t'\\t' can be used as well.\n" + "\t(default: ',')", "F", 1,
      "-F <separator>"));

    result.add(new Option("\tThe enclosure character(s) to use for strings.\n"
      + "\tSpecify as a comma separated list (e.g. \",'" + " (default: \",')",
      "E", 1, "-E <enclosures>"));

    result.add(new Option(
      "\tDon't compute summary statistics for numeric attributes",
      "no-summary-stats", 0, "-no-summary-stats"));

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0) {
      setNominalAttributes(tmpStr);
    } else {
      setNominalAttributes("");
    }

    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setStringAttributes(tmpStr);
    } else {
      setStringAttributes("");
    }

    tmpStr = Utils.getOption('D', options);
    if (tmpStr.length() > 0) {
      setDateAttributes(tmpStr);
    }
    tmpStr = Utils.getOption("format", options);
    if (tmpStr.length() > 0) {
      setDateFormat(tmpStr);
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMissingValue(tmpStr);
    } else {
      setMissingValue("?");
    }

    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0) {
      setFieldSeparator(tmpStr);
    } else {
      setFieldSeparator(",");
    }

    tmpStr = Utils.getOption("E", options);
    if (tmpStr.length() > 0) {
      setEnclosureCharacters(tmpStr);
    }

    setComputeSummaryStats(!Utils.getFlag("no-summary-stats", options)); //$NON-NLS-1$

    while (true) {
      tmpStr = Utils.getOption('L', options);
      if (tmpStr.length() == 0) {
        break;
      }

      m_nominalLabelSpecs.add(tmpStr);
    }
  }

  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (getNominalAttributes().length() > 0) {
      result.add("-N");
      result.add(getNominalAttributes());
    }

    if (getStringAttributes().length() > 0) {
      result.add("-S");
      result.add(getStringAttributes());
    }

    if (getDateAttributes().length() > 0) {
      result.add("-D");
      result.add(getDateAttributes());
      result.add("-format");
      result.add(getDateFormat());
    }

    result.add("-M");
    result.add(getMissingValue());

    result.add("-E");
    result.add(getEnclosureCharacters());

    result.add("-F");
    result.add(getFieldSeparator());

    if (!getComputeSummaryStats()) {
      result.add("-no-summary-stats");
    }

    for (String spec : m_nominalLabelSpecs) {
      result.add("-L");
      result.add(spec);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Whether to compute summary stats for numeric attributes
   * 
   * @param c true if summary stats are to be computed
   */
  public void setComputeSummaryStats(boolean c) {
    m_computeSummaryStats = c;
  }

  /**
   * Whether to compute summary stats for numeric attributes
   * 
   * @return true if summary stats are to be computed
   */
  public boolean getComputeSummaryStats() {
    return m_computeSummaryStats;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String computeSummaryStatsTipText() {
    return "Compute summary stats (count, sum, sum squared, "
      + "min, max mean and standard deviation) for numeric attributes.";
  }

  /**
   * Sets the placeholder for missing values.
   * 
   * @param value the placeholder
   */
  public void setMissingValue(String value) {
    m_MissingValue = value;
  }

  /**
   * Returns the current placeholder for missing values.
   * 
   * @return the placeholder
   */
  public String getMissingValue() {
    return m_MissingValue;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String missingValueTipText() {
    return "The placeholder for missing values, default is '?'.";
  }

  /**
   * Sets the attribute range to be forced to type string.
   * 
   * @param value the range
   */
  public void setStringAttributes(String value) {
    m_forceString.setRanges(value);
  }

  /**
   * Returns the current attribute range to be forced to type string.
   * 
   * @return the range
   */
  public String getStringAttributes() {
    return m_forceString.getRanges();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stringAttributesTipText() {
    return "The range of attributes to force to be of type STRING, example "
      + "ranges: 'first-last', '1,4,7-14,50-last'.";
  }

  /**
   * Sets the attribute range to be forced to type nominal.
   * 
   * @param value the range
   */
  public void setNominalAttributes(String value) {
    m_forceNominal.setRanges(value);
  }

  /**
   * Returns the current attribute range to be forced to type nominal.
   * 
   * @return the range
   */
  public String getNominalAttributes() {
    return m_forceNominal.getRanges();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nominalAttributesTipText() {
    return "The range of attributes to force to be of type NOMINAL, example "
      + "ranges: 'first-last', '1,4,7-14,50-last'.";
  }

  /**
   * Set the format to use for parsing date values.
   * 
   * @param value the format to use.
   */
  public void setDateFormat(String value) {
    m_dateFormat = value;
    m_formatter = null;
  }

  /**
   * Get the format to use for parsing date values.
   * 
   * @return the format to use for parsing date values.
   * 
   */
  public String getDateFormat() {
    return m_dateFormat;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dateFormatTipText() {
    return "The format to use for parsing date values.";
  }

  /**
   * Set the attribute range to be forced to type date.
   * 
   * @param value the range
   */
  public void setDateAttributes(String value) {
    m_forceDate.setRanges(value);
  }

  /**
   * Returns the current attribute range to be forced to type date.
   * 
   * @return the range.
   */
  public String getDateAttributes() {
    return m_forceDate.getRanges();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dateAttributesTipText() {
    return "The range of attributes to force to type DATE, example "
      + "ranges: 'first-last', '1,4,7-14, 50-last'.";
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String enclosureCharactersTipText() {
    return "The characters to use as enclosures for strings. E.g. \",'";
  }

  /**
   * Set the character(s) to use/recognize as string enclosures
   * 
   * @param enclosure the characters to use as string enclosures
   */
  public void setEnclosureCharacters(String enclosure) {
    m_Enclosures = enclosure;
  }

  /**
   * Get the character(s) to use/recognize as string enclosures
   * 
   * @return the characters to use as string enclosures
   */
  public String getEnclosureCharacters() {
    return m_Enclosures;
  }

  /**
   * Sets the character used as column separator.
   * 
   * @param value the character to use
   */
  public void setFieldSeparator(String value) {
    m_FieldSeparator = Utils.unbackQuoteChars(value);
    if (m_FieldSeparator.length() != 1) {
      m_FieldSeparator = ",";
      System.err
        .println("Field separator can only be a single character (exception being '\t'), "
          + "defaulting back to '" + m_FieldSeparator + "'!");
    }
  }

  /**
   * Returns the character used as column separator.
   * 
   * @return the character to use
   */
  public String getFieldSeparator() {
    return Utils.backQuoteChars(m_FieldSeparator);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fieldSeparatorTipText() {
    return "The character to use as separator for the columns/fields (use '\\t' for TAB).";
  }

  /**
   * Set label specifications for nominal attributes.
   * 
   * @param specs an array of label specifications
   */
  public void setNominalLabelSpecs(Object[] specs) {
    m_nominalLabelSpecs.clear();
    for (Object s : specs) {
      m_nominalLabelSpecs.add(s.toString());
    }
  }

  /**
   * Get label specifications for nominal attributes.
   * 
   * @return an array of label specifications
   */
  public Object[] getNominalLabelSpecs() {
    return m_nominalLabelSpecs.toArray(new String[0]);
  }

  /**
   * Generate attribute names. Attributes are named "attinitial",
   * "attinitial+1", ..., "attinitial+numAtts-1"
   * 
   * @param initial the number to use for the first attribute
   * @param numAtts the number of attributes to generate
   */
  public void generateNames(int initial, int numAtts) {
    for (int i = initial; i < initial + numAtts; i++) {
      m_attributeNames.add("att" + (i + 1));
    }
  }

  /**
   * Generate attribute names. Attributes are named "att0", "att1", ...
   * "attnumAtts-1"
   * 
   * @param numAtts the number of attribute names to generate
   */
  public void generateNames(int numAtts) {
    generateNames(0, numAtts);
    // for (int i = 0; i < numAtts; i++) {
    // m_attributeNames.add("att" + (i + 1));
    // }
  }

  public void initParserOnly() {
    m_parser = new CSVParser(m_FieldSeparator.charAt(0),
      m_Enclosures.charAt(0), '\\');
  }

  public String[] parseRowOnly(String row) throws IOException {
    return m_parser.parseLine(row);
  }

  /**
   * Process a row of data coming into the map. Split the row into fields and
   * initialize if this is the first row seen. attNames may be non-null for the
   * first row and is optional. If not supplied then names will be generated on
   * receiving the first row of data. An exception will be raised on subsequent
   * rows that don't have the same number of fields as seen in the first row
   * 
   * @param row the row to process
   * @param attNames the names of the attributes (fields)
   * @exception if the number of fields in the current row does not match the
   *              number of attribute names
   */
  public void processRow(String row, List<String> attNames)
    throws DistributedWekaException, IOException {

    String[] fields = null;

    // next check to see if m_attributeTypes is null (i.e. first row)
    // and if so then init array according to number of tokens and
    // set initial types based on ranges
    if (m_attributeTypes == null) {

      m_formatter = new SimpleDateFormat(m_dateFormat);

      // tokenize the first line
      m_parser = new CSVParser(m_FieldSeparator.charAt(0),
        m_Enclosures.charAt(0), '\\');

      fields = m_parser.parseLine(row);

      if (attNames != null && fields.length != attNames.size()) {
        throw new IllegalArgumentException(
          "Number of names given does not match number of fields in the first row!");
      }

      if (attNames == null) {
        generateNames(fields.length);
      } else {
        m_attributeNames = attNames;
      }

      // process ranges etc.
      processRanges(fields.length, TYPE.UNDETERMINED);
      processNominalSpecs(fields.length);
    }

    // process the row
    if (fields == null) {
      fields = m_parser.parseLine(row);

      if (fields.length != m_attributeNames.size()) {
        throw new IOException("Expected " + m_attributeNames.size()
          + " fields, but got " + fields.length + " for row: " + row);
      }
    }

    // should try to alert the user to all data issues in this phase (i.e.
    // before getting to the model building). E.g. unparseable dates,
    // numbers etc.
    for (int i = 0; i < fields.length; i++) {
      if (fields[i] != null && !fields[i].equals(m_MissingValue)
        && fields[i].trim().length() != 0) {
        if (m_attributeTypes[i] == TYPE.NUMERIC
          || m_attributeTypes[i] == TYPE.UNDETERMINED) {
          try {
            double value = Double.parseDouble(fields[i]);
            m_attributeTypes[i] = TYPE.NUMERIC;

            if (m_computeSummaryStats) {
              updateSummaryStats(m_summaryStats, m_attributeNames.get(i),
                value, null, false);
            }
          } catch (NumberFormatException ex) {

            if (m_attributeTypes[i] == TYPE.UNDETERMINED) {
              // assume its an enumerated value
              m_attributeTypes[i] = TYPE.NOMINAL;
              TreeSet<String> ts = new TreeSet<String>();
              ts.add(fields[i]);
              m_nominalVals.put(i, ts);

              if (m_computeSummaryStats) {
                updateSummaryStats(m_summaryStats, m_attributeNames.get(i), 1,
                  fields[i], true);
              }
            } else {
              m_attributeTypes[i] = TYPE.STRING;
            }
          }
        } else if (m_attributeTypes[i] == TYPE.DATE) {
          // check that date is parseable
          Date d = null;
          try {
            d = m_formatter.parse(fields[i]);
          } catch (ParseException e) {
            throw new DistributedWekaException(e);
          }
          if (m_computeSummaryStats) {
            updateSummaryStats(m_summaryStats, m_attributeNames.get(i),
              d.getTime(), null, false);
            ;
          }

        } else if (m_attributeTypes[i] == TYPE.NOMINAL) {
          m_nominalVals.get(i).add(fields[i]);
          if (m_computeSummaryStats) {
            updateSummaryStats(m_summaryStats, m_attributeNames.get(i), 1,
              fields[i], true);
          }
        }
      } else {
        // missing value

        if (m_computeSummaryStats) {
          updateSummaryStats(m_summaryStats, m_attributeNames.get(i),
            Utils.missingValue(), null, m_attributeTypes[i] == TYPE.NOMINAL);
        }
      }
    }
  }

  /**
   * Update the summary statistics for a given attribute with the given value
   * 
   * @param summaryStats the map of summary statistics
   * @param attName the name of the attribute being updated
   * @param value the value to update with (if the attribute is numeric)
   * @param nominalLabel holds the label for the attribute (if it is nominal)
   * @param isNominal true if the attribute is nominal
   */
  public static void updateSummaryStats(Map<String, Stats> summaryStats,
    String attName, double value, String nominalLabel, boolean isNominal) {
    Stats s = summaryStats.get(attName);

    if (!isNominal) {
      // numeric attribute
      if (s == null) {
        s = new NumericStats(attName);
        summaryStats.put(attName, s);
      }

      NumericStats ns = (NumericStats) s;
      if (Utils.isMissingValue(value)) {
        ns.m_stats[ArffSummaryNumericMetric.MISSING.ordinal()]++;
      } else {
        ns.m_stats[ArffSummaryNumericMetric.COUNT.ordinal()]++;
        ns.m_stats[ArffSummaryNumericMetric.SUM.ordinal()] += value;
        ns.m_stats[ArffSummaryNumericMetric.SUMSQ.ordinal()] += value * value;
        if (Double.isNaN(ns.m_stats[ArffSummaryNumericMetric.MIN.ordinal()])) {
          ns.m_stats[ArffSummaryNumericMetric.MIN.ordinal()] = ns.m_stats[ArffSummaryNumericMetric.MAX
            .ordinal()] = value;
        } else if (value < ns.m_stats[ArffSummaryNumericMetric.MIN.ordinal()]) {
          ns.m_stats[ArffSummaryNumericMetric.MIN.ordinal()] = value;
        } else if (value > ns.m_stats[ArffSummaryNumericMetric.MAX.ordinal()]) {
          ns.m_stats[ArffSummaryNumericMetric.MAX.ordinal()] = value;
        }
      }
    } else {
      // nominal attribute

      if (s == null) {
        s = new NominalStats(attName);
        summaryStats.put(attName, s);
      }

      // check to see if the type is correct - it
      // might not be if the first row(s) processed contain
      // missing values. In this case the TYPE would have
      // been undetermined (unless explicitly specified
      // by the user). The default is to assume the
      // attribute is numeric, so a NumericStats object
      // (initialized with only the missing count) would
      // have been created.

      if (s instanceof NumericStats) {
        double missing = ((NumericStats) s).m_stats[ArffSummaryNumericMetric.MISSING
          .ordinal()];

        // need to replace this with NominalStats and transfer over the missing
        // count
        s = new NominalStats(attName);
        ((NominalStats) s).add(null, missing);
        summaryStats.put(attName, s);
      }

      NominalStats ns = (NominalStats) s;
      if (Utils.isMissingValue(value) && nominalLabel == null) {
        ns.add(nominalLabel, 1.0);
      } else {

        NominalStats.Count c = ns.m_counts.get(nominalLabel);
        if (c == null) {
          c = new NominalStats.Count();
          ns.m_counts.put(nominalLabel, c);
        }
        c.m_count += value;
      }
    }
  }

  // called after map processing
  /**
   * get the header information (as an Instances object) from what has been seen
   * so far by this map task
   * 
   * @return the header information as an Instances object
   */
  public Instances getHeader() {

    return makeStructure();
  }

  /**
   * Check if the header can be produced immediately without having to do a
   * pre-processing pass to determine and unify nominal attribute values. All
   * types should be specified via the ranges and nominal label specs.
   * 
   * @param numFields number of fields in the data
   * @param attNames the names of the attributes (in order)
   * @param problems a StringBuffer to hold problem descriptions (if any)
   * @return true if the header can be generated immediately with out a
   *         pre-processing job
   */
  public boolean headerAvailableImmediately(int numFields,
    List<String> attNames, StringBuffer problems) {
    if (attNames == null) {
      generateNames(numFields);
    } else {
      m_attributeNames = attNames;
    }

    processRanges(numFields, TYPE.NUMERIC);
    processNominalSpecs(numFields);
    boolean ok = true;

    // check that all nominal atts have specs
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.NOMINAL) {
        if (m_nominalVals.get(i) == null || m_nominalVals.get(i).size() == 0) {
          ok = false;
          problems.append("Attribute number " + (i + 1) + " ("
            + m_attributeNames.get(i) + ") is specified as type nominal, "
            + "but no legal values have been supplied for this attribute!\n");
        }
      }
    }

    return ok;
  }

  /**
   * Get a header constructed using the supplied attribute names. This should
   * only be called in the situation where the data does not require a
   * pre-processing pass to determine and unify nominal attribute values. All
   * types should be specified via the ranges and nominal label specifications.
   * 
   * @param numFields the number of attributes in the data
   * @param attNames the attribute names to use. May be null, in which case
   *          names are generated
   * @return an Instances object encapsulating header information
   * @throws DistributedWekaException if nominal attributes have been specified
   *           but there are one or more tha have no user-supplied label
   *           specifications
   */
  public Instances getHeader(int numFields, List<String> attNames)
    throws DistributedWekaException {

    StringBuffer problems = new StringBuffer();
    if (!headerAvailableImmediately(numFields, attNames, problems)) {
      throw new DistributedWekaException(problems.toString());
    }

    // create header
    return makeStructure();
  }

  private void processRanges(int numFields, TYPE defaultType) {
    m_attributeTypes = new TYPE[numFields];

    m_forceString.setUpper(numFields - 1);
    m_forceNominal.setUpper(numFields - 1);
    m_forceDate.setUpper(numFields - 1);

    for (int i = 0; i < numFields; i++) {
      m_attributeTypes[i] = defaultType;

      if (m_forceNominal.isInRange(i)) {
        m_attributeTypes[i] = TYPE.NOMINAL;
        m_nominalVals.put(i, new TreeSet<String>());
      } else if (m_forceDate.isInRange(i)) {
        m_attributeTypes[i] = TYPE.DATE;
      } else if (m_forceString.isInRange(i)) {
        m_attributeTypes[i] = TYPE.STRING;
      }
    }

  }

  private void processNominalSpecs(int numFields) {
    if (m_nominalLabelSpecs.size() > 0) {
      for (String spec : m_nominalLabelSpecs) {
        String[] attsAndLabels = spec.split(":");
        if (attsAndLabels.length == 2) {
          String[] labels = attsAndLabels[1].split(",");
          try {
            // try as a range string first
            Range tempR = new Range();
            tempR.setRanges(attsAndLabels[0].trim());
            tempR.setUpper(numFields - 1);

            int[] rangeIndexes = tempR.getSelection();
            for (int i = 0; i < rangeIndexes.length; i++) {
              m_attributeTypes[rangeIndexes[i]] = TYPE.NOMINAL;
              TreeSet<String> ts = new TreeSet<String>();
              for (String lab : labels) {
                ts.add(lab);
              }
              m_nominalVals.put(rangeIndexes[i], ts);
            }
          } catch (IllegalArgumentException e) {
            // one or more named attributes?
            String[] attNames = attsAndLabels[0].split(",");
            for (String attN : attNames) {
              int attIndex = m_attributeNames.indexOf(attN);

              if (attIndex >= 0) {
                m_attributeTypes[attIndex] = TYPE.NOMINAL;
                TreeSet<String> ts = new TreeSet<String>();
                for (String lab : labels) {
                  ts.add(lab);
                }
                m_nominalVals.put(attIndex, ts);
              }
            }
          }
        }
      }
    }
  }

  protected Instances makeStructure() {
    // post-process for any undetermined - this means all missing values in
    // the data chunk that we processed
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.UNDETERMINED) {
        // type conflicts due to all missing values are handled
        // in the reducer by checking numeric types against nominal/string
        m_attributeTypes[i] = TYPE.NUMERIC;
      }
    }

    // make final structure
    ArrayList<Attribute> attribs = new ArrayList<Attribute>();
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.STRING
        || m_attributeTypes[i] == TYPE.UNDETERMINED) {
        attribs.add(new Attribute(m_attributeNames.get(i),
          (java.util.List<String>) null));
      } else if (m_attributeTypes[i] == TYPE.DATE) {
        attribs.add(new Attribute(m_attributeNames.get(i), m_dateFormat));
      } else if (m_attributeTypes[i] == TYPE.NUMERIC) {
        attribs.add(new Attribute(m_attributeNames.get(i)));
      } else if (m_attributeTypes[i] == TYPE.NOMINAL) {
        TreeSet<String> vals = m_nominalVals.get(i);
        ArrayList<String> theVals = new ArrayList<String>();
        if (vals.size() > 0) {
          for (String v : vals) {
            theVals.add(v);
          }
        } else {
          theVals.add("*unknown*");
        }
        attribs.add(new Attribute(m_attributeNames.get(i), theVals));
      } else {
        attribs.add(new Attribute(m_attributeNames.get(i), m_dateFormat));
      }
    }

    if (m_computeSummaryStats) {
      for (int i = 0; i < m_attributeTypes.length; i++) {
        if (m_attributeTypes[i] == TYPE.NUMERIC
          || m_attributeTypes[i] == TYPE.DATE) {
          NumericStats ns = (NumericStats) m_summaryStats.get(m_attributeNames
            .get(i));

          attribs.add(ns.makeAttribute());
        } else if (m_attributeTypes[i] == TYPE.NOMINAL) {
          NominalStats ns = (NominalStats) m_summaryStats.get(m_attributeNames
            .get(i));
          attribs.add(ns.makeAttribute());
        }
      }
    }

    Instances structure = new Instances("A relation name", attribs, 0);

    return structure;
  }

  /**
   * Utility method for Constructing a dense instance given an array of parsed
   * CSV values
   * 
   * @param trainingHeader the header to associate the instance with. Does not
   *          add the new instance to this data set; just gives the instance a
   *          reference to the header
   * @param setStringValues true if any string values should be set in the
   *          header as opposed to being added to the header (i.e. accumulating
   *          in the header).
   * @param parsed the array of parsed CSV values
   * @param sparse true if the new instance is to be a sparse instance
   * @return an Instance
   * @throws Exception if a problem occurs
   */
  public Instance makeInstance(Instances trainingHeader,
    boolean setStringValues, String[] parsed) throws Exception {
    return makeInstance(trainingHeader, setStringValues, parsed, false);
  }

  /**
   * Utility method for Constructing an instance given an array of parsed CSV
   * values
   * 
   * @param trainingHeader the header to associate the instance with. Does not
   *          add the new instance to this data set; just gives the instance a
   *          reference to the header
   * @param setStringValues true if any string values should be set in the
   *          header as opposed to being added to the header (i.e. accumulating
   *          in the header).
   * @param parsed the array of parsed CSV values
   * @param sparse true if the new instance is to be a sparse instance
   * @return an Instance
   * @throws Exception if a problem occurs
   */
  public Instance makeInstance(Instances trainingHeader,
    boolean setStringValues, String[] parsed, boolean sparse) throws Exception {
    double[] vals = new double[trainingHeader.numAttributes()];

    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      if (parsed[i] == null || parsed[i].equals(getMissingValue())
        || parsed[i].trim().length() == 0) {
        vals[i] = Utils.missingValue();
        continue;
      }
      Attribute current = trainingHeader.attribute(i);
      if (current.isString()) {
        if (setStringValues) {
          current.setStringValue(parsed[i]);
          vals[i] = 0;
        } else {
          vals[i] = current.addStringValue(parsed[i]);
        }
      } else if (current.isNominal()) {
        int index = current.indexOfValue(parsed[i]);

        if (index < 0) {
          throw new Exception("Can't find nominal value '" + parsed[i]
            + "' in list of values for " + "attribute '" + current.name() + "'");
        }
        vals[i] = index;
      } else if (current.isDate()) {
        try {
          double val = current.parseDate(parsed[i]);
          vals[i] = val;
        } catch (ParseException p) {
          throw new Exception(p);
        }
      } else if (current.isNumeric()) {
        try {
          double val = Double.parseDouble(parsed[i]);
          vals[i] = val;
        } catch (NumberFormatException n) {
          throw new Exception(n);
        }
      } else {
        throw new Exception("Unsupported attribute type: "
          + Attribute.typeToString(current));
      }
    }

    Instance result = null;

    if (sparse) {
      result = new SparseInstance(1.0, vals);
    } else {
      result = new DenseInstance(1.0, vals);
    }
    result.setDataset(trainingHeader);

    return result;
  }

  public static void main(String[] args) {
    try {
      CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
      // task.setOptions(args);
      task.setComputeSummaryStats(false);

      Instances i = task.getHeader(10, null);
      System.err.println(i);

      task = new CSVToARFFHeaderMapTask();
      task.setOptions(args);
      task.setComputeSummaryStats(true);

      BufferedReader br = new BufferedReader(new FileReader(args[0]));
      String line = br.readLine();
      String[] names = line.split(",");
      List<String> attNames = new ArrayList<String>();
      for (String s : names) {
        attNames.add(s);
      }

      while ((line = br.readLine()) != null) {
        task.processRow(line, attNames);
      }

      br.close();

      System.err.println(task.getHeader());

      CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
      List<Instances> instList = new ArrayList<Instances>();
      instList.add(task.getHeader());
      Instances withSummary = arffReduce.aggregate(instList);

      System.err.println(withSummary);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
