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

import au.com.bytecode.opencsv.CSVParser;
import weka.core.stats.TDigest;
import distributed.core.DistributedJobConfig;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;
import weka.core.stats.Stats;
import weka.core.stats.StringStats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

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

  /** Attribute name prefix for a summary statistics attribute */
  public static final String ARFF_SUMMARY_ATTRIBUTE_PREFIX = "arff_summary_";
  public static final int MAX_PARSING_ERRORS = 50;
  /**
   * For serialization
   */
  private static final long serialVersionUID = -3949274571568175413L;
  /** Attribute types for the incoming CSV columns */
  protected TYPE[] m_attributeTypes;

  /** A range of columns to force to be of type String */
  protected Range m_forceString = new Range();

  /** A range of columns to force to be of type Nominal */
  protected Range m_forceNominal = new Range();

  /** A range of columns to force to be of type Date */
  protected Range m_forceDate = new Range();

  /**
   * User supplied ranges to force to be string (passed to Range objects at init
   * time)
   */
  protected String m_stringRange = "";

  /**
   * User supplied ranges to force to be nominal (passed to Range objects at
   * init time)
   */
  protected String m_nominalRange = "";

  /**
   * User supplied ranges to force to be date (passed to Range objects at init
   * time)
   */
  protected String m_dateRange = "";

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
  /**
   * The user-supplied default nominal values - each entry in the list is a spec
   */
  protected List<String> m_nominalDefaultLabelSpecs = new ArrayList<String>();
  /** Lookup for nominal values */
  protected Map<Integer, TreeSet<String>> m_nominalVals =
    new HashMap<Integer, TreeSet<String>>();
  /**
   * Default labels (if any) to use with nominal attributes. These are like a
   * "catch-all" and can be used when you are are explicitly specifying labels
   * but don't want to specify all labels. One use-case if to convert a
   * multi-class problem into a binary one, by simply specifying the positive
   * class label.
   */
  protected Map<Integer, String> m_nominalDefaultVals =
    new HashMap<Integer, String>();
  /** The placeholder for missing values. */
  protected String m_MissingValue = "?";
  /** enclosure character to use for strings - opencsv only allows one */
  protected String m_Enclosures = "\'";
  /** the field separator. */
  protected String m_FieldSeparator = ",";
  /** The CSV parser (unfortunately, the parser does not implement Serializable) */
  protected transient CSVParser m_parser;
  /** Whether to compute summary statistics or not */
  protected boolean m_computeSummaryStats = true;
  /** A map of attribute names to summary statistics */
  protected Map<String, Stats> m_summaryStats = new HashMap<String, Stats>();
  /**
   * We keep (potentially) temporary string stats for numeric atts too - just in
   * case we hit an unparsable number and switch to string for that column
   */
  protected Map<String, StringStats> m_stringBackupStats = new HashMap<>();
  /** Decimal places for summary stats */
  protected int m_decimalPlaces = 2;

  /**
   * Whether to treat zeros as missing values when computing summary stats for
   * numeric attributes
   */
  protected boolean m_treatZeroAsMissing;

  /** Whether to suppress command line options relating to quantile estimation */
  protected boolean m_suppressQuantileOptions;

  /* Whether to suppress command line options relating to CSV parsing */
  protected boolean m_suppressCSVParsingOptions;

  /** Whether to perform quantile estimation too */
  protected boolean m_estimateQuantiles = false;
  /** The compression level for the TDigest quantile estimator */
  protected double m_quantileCompression = NumericStats.Q_COMPRESSION;
  protected int m_parsingErrors;

  /**
   * Whether to treat values not parsable as numbers as missing value (instead
   * of this forcing a previously thought numeric field to type string
   */
  protected boolean m_treatUnparsableNumericValuesAsMissing;

  /**
   * Constructor
   */
  public CSVToARFFHeaderMapTask() {
    this(false);
  }

  /**
   * Constructor
   * 
   * @param suppressQuantileOptions true if commandline options relating to
   *          quantile estimation are to be suppressed
   */
  public CSVToARFFHeaderMapTask(boolean suppressQuantileOptions) {
    m_suppressQuantileOptions = suppressQuantileOptions;
  }

  /**
   * Constructor
   * 
   * @param suppressQuantileOptions true if command line options relating to
   *          quantile estimation are to be suppressed
   * @param suppressCSVParsingOptions true if command line options relating to
   *          CSV parsing are to be suppressed
   */
  public CSVToARFFHeaderMapTask(boolean suppressQuantileOptions,
    boolean suppressCSVParsingOptions) {
    m_suppressQuantileOptions = suppressQuantileOptions;
    m_suppressCSVParsingOptions = suppressCSVParsingOptions;
  }

  /**
   * Update the summary statistics for a given attribute with the given value
   *
   * @param summaryStats the map of summary statistics
   * @param backupStringStats the temporary map of backup string stats kept for
   *          numeric fields (this can be null in cases where we are sure that
   *          there is no chance of unparsable numeric values occuring)
   * @param attName the name of the attribute being updated
   * @param value the value to update with (if the attribute is numeric)
   * @param nominalLabel holds the label/string for the attribute (if it is
   *          nominal or string)
   * @param isNominal true if the attribute is nominal
   * @param isString true if the attribute is a string attribute
   * @param treatZeroAsMissing treats zero as missing value for numeric
   *          attributes
   * @param estimateQuantiles true if we should estimate quantiles too
   * @param quantileCompression the compression level to use in the TDigest
   *          estimators
   */
  public static void updateSummaryStats(Map<String, Stats> summaryStats,
    Map<String, StringStats> backupStringStats, String attName, double value,
    String nominalLabel, boolean isNominal, boolean isString,
    boolean treatZeroAsMissing, boolean estimateQuantiles,
    double quantileCompression) {
    Stats s = summaryStats.get(attName);
    StringStats backup =
      backupStringStats != null ? backupStringStats.get(attName) : null;

    if (!isNominal && !isString) {
      // numeric attribute
      if (s == null) {
        s = new NumericStats(attName, quantileCompression);
        summaryStats.put(attName, s);
        if (backupStringStats != null) {
          backup = new StringStats(attName);
          backupStringStats.put(attName, backup);
        }
      }

      NumericStats ns = (NumericStats) s;
      ns.update(value, 1.0, treatZeroAsMissing, estimateQuantiles);
      if (backup != null) {
        backup.update("" + value, 1.0);
      }
    } else if (isNominal) {
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
        double missing =
          ((NumericStats) s).getStats()[ArffSummaryNumericMetric.MISSING
            .ordinal()];

        // need to replace this with NominalStats and transfer over the missing
        // count
        s = new NominalStats(attName);
        ((NominalStats) s).add(null, missing);
        summaryStats.put(attName, s);
      }

      NominalStats ns = (NominalStats) s;
      ns.add(nominalLabel, 1.0);
    } else if (isString) {
      if (s == null) {
        s = new StringStats(attName);
        summaryStats.put(attName, s);
      }

      if (s instanceof NumericStats) {
        if (backup != null) {
          s = backup;
          summaryStats.put(attName, s);
          // save memory
          backupStringStats.put(attName, null);
          System.err.println("[CSVToARFFHeaderMapTask] Attribute '" + attName
            + "' was numeric - now being treated as string.");
        } else {
          throw new IllegalStateException("Attribute '" + attName
            + "' has been marked "
            + "as type string, but the associated stats object is of "
            + "type numeric and there is no backup string stats object");
        }
      }

      StringStats ss = (StringStats) s;
      ss.update(nominalLabel, 1.0);
    }
  }

  public static List<String>
    instanceHeaderToAttributeNameList(Instances header) {
    List<String> attNames = new ArrayList<String>();

    for (int i = 0; i < header.numAttributes(); i++) {
      attNames.add(header.attribute(i).name());
    }

    return attNames;
  }

  public static void main(String[] args) {
    try {
      CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

      task = new CSVToARFFHeaderMapTask();
      task.setOptions(args);
      task.setComputeQuartilesAsPartOfSummaryStats(true);
      // task.setComputeSummaryStats(true);

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

  /**
   * Performs a "combine" operation using the supplied partial
   * CSVToARFFHeaderMapTask tasks. This is essentially a reduce operation, but
   * returns a single CSVToARFFHeaderMapTask object (rather than the final
   * header that is produced by CSVToARFFHeaderReduceTask). This allows several
   * reduce stages to be implemented (if desired) or partial reduces to occur in
   * parallel.
   *
   * @param tasks a list of CSVToARFFHeaderMapTasks to "combine"
   * @return a CSVToARFFHeaderMapTask with the merged state
   * @throws DistributedWekaException if a problem occurs
   */
  public static CSVToARFFHeaderMapTask combine(
    List<CSVToARFFHeaderMapTask> tasks) throws DistributedWekaException {
    if (tasks == null || tasks.size() == 0) {
      throw new DistributedWekaException(
        "[CSVToARFFHeaderMapTask:combine] no tasks to combine!");
    }
    if (tasks.size() == 1) {
      return tasks.get(0);
    }

    Instances combinedHeaders = null;
    CSVToARFFHeaderMapTask master = tasks.get(0);
    List<Instances> toCombine = new ArrayList<Instances>();
    for (int i = 0; i < tasks.size(); i++) {
      toCombine.add(tasks.get(i).getHeader());
    }
    combinedHeaders = CSVToARFFHeaderReduceTask.aggregate(toCombine);

    Map<String, TDigest> mergedDigests = new HashMap<String, TDigest>();
    if (master.getComputeQuartilesAsPartOfSummaryStats()) {
      Instances headerNoSummary =
        CSVToARFFHeaderReduceTask.stripSummaryAtts(combinedHeaders);

      for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
        List<TDigest> digestsToMerge = new ArrayList<TDigest>();
        String attName = headerNoSummary.attribute(i).name();

        for (CSVToARFFHeaderMapTask t : tasks) {
          Stats ns = t.m_summaryStats.get(attName);
          if (ns instanceof NumericStats) {
            TDigest partialEstimator =
              ((NumericStats) ns).getQuantileEstimator();
            if (partialEstimator != null) {
              digestsToMerge.add(partialEstimator);
            }
          }

          // HeaderAndQuantileDataHolder h =
          // t.getHeaderAndQuantileEstimators();
          // TDigest partialEstimator =
          // h.getQuantileEstimator(attName);
          // if (partialEstimator != null) {
          // digestsToMerge.add(partialEstimator);
          // }
        }

        if (digestsToMerge.size() > 0) {
          TDigest mergedForAtt =
            TDigest.merge(digestsToMerge.get(0).compression(), digestsToMerge);
          mergedDigests.put(attName, mergedForAtt);
        }
      }
    }

    // need to re-construct master now that we've (potentially) resolved
    // type conflicts within this combine operation
    master.fromHeader(combinedHeaders, mergedDigests);

    return master;
  }

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
        + "\tattributes. May be specified multiple times.\n" + "\tThe "
        + "spec contains two parts separated by a \":\". The\n"
        + "\tfirst part can be a range of attribute indexes or\n"
        + "\ta comma-separated list off attruibute names; the\n"
        + "\tsecond part is a comma-separated list of labels. E.g\n"
        + "\t\"1,2,4-6:red,green,blue\" or \"att1,att2:red,green," + "blue\"",
      "L", 1, "-L <nominal label spec>"));

    result.add(new Option("\tDefault label specs. Use in conjunction with\n"
      + "\t-L to specify a default label to use in the case\n"
      + "\twhere a label is encountered, for a given attribute,\n"
      + "\t that is not in the set supplied via the -L option.\n"
      + "\tUse the same format [index range | name list]:<default label>.",
      "default-label", 1, "-default-label <spec>"));

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
      "\tThe date formatting string to use to parse/format date values.\n"
        + "\t(default: \"yyyy-MM-dd'T'HH:mm:ss\")", "format", 1,
      "-format <date format>"));

    result.add(new Option("\tFor numeric columns, treat any "
      + "unparsable values as missing.", "unparsable-numeric", 0,
      "-unparsable-numeric"));

    if (!m_suppressCSVParsingOptions) {
      result.add(new Option("\tThe string representing a missing value.\n"
        + "\t(default: ?)", "M", 1, "-M <str>"));

      result.add(new Option("\tThe field separator to be used.\n"
        + "\t'\\t' can be used as well.\n" + "\t(default: ',')", "F", 1,
        "-F <separator>"));

      result.add(new Option(
        "\tThe enclosure character(s) to use for strings.\n"
          + "\tSpecify as a comma separated list (e.g. \",'"
          + " (default: \",')", "E", 1, "-E <enclosures>"));
    }

    if (!m_suppressQuantileOptions) {
      result.add(new Option(
        "\tInclude quartile estimates (and histograms) in summary attributes.\n\t"
          + "Note that this adds quite a bit to computation time",
        "compute-quartiles", 0, "-compute-quartiles"));

      result
        .add(new Option(
          "\tThe compression level to use when computing estimated quantiles.\n\t"
            + "Higher values result in less compression and more accurate estimates\n\t"
            + "at the expense of time and space (default="
            + NumericStats.Q_COMPRESSION + ").", "compression", 1,
          "-compression <number>"));

      result.add(new Option("\tNumber of decimal places for summary stats.\n\t"
        + "(default = 2)", "decimal-places", 1, "-decimal-places <num>"));
    }

    return result.elements();
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

    if (getTreatUnparsableNumericValuesAsMissing()) {
      result.add("-unparsable-numeric");
    }

    if (!m_suppressCSVParsingOptions) {
      result.add("-M");
      result.add(getMissingValue());

      result.add("-E");
      String encl = getEnclosureCharacters();
      if (encl.charAt(0) == '"') {
        encl = "\\\"";
      }
      result.add(encl);

      result.add("-F");
      result.add(getFieldSeparator());
    }

    if (!m_suppressQuantileOptions) {
      if (getComputeQuartilesAsPartOfSummaryStats()) {
        result.add("-compute-quartiles");
      }

      result.add("-compression");
      result.add("" + getCompressionLevelForQuartileEstimation());

      result.add("-decimal-places");
      result.add("" + getNumDecimalPlaces());
    }

    if (getTreatZerosAsMissing()) {
      result.add("-treat-zeros-as-missing");
    }

    for (String spec : m_nominalLabelSpecs) {
      result.add("-L");
      result.add(spec);
    }

    for (String spec : m_nominalDefaultLabelSpecs) {
      result.add("-default-label");
      result.add(spec);
    }

    return result.toArray(new String[result.size()]);
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

    setTreatUnparsableNumericValuesAsMissing(Utils.getFlag(
      "unparsable-numeric", options));

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMissingValue(tmpStr);
    } else {
      setMissingValue("?");
    }

    if (!m_suppressCSVParsingOptions) {
      tmpStr = Utils.getOption('F', options);
      if (tmpStr.length() != 0) {
        setFieldSeparator(tmpStr);
      } else {
        setFieldSeparator(",");
      }

      tmpStr = Utils.getOption("E", options);
      if (tmpStr.length() > 0) {
        if (tmpStr.charAt(0) == '\\' && tmpStr.length() > 1) {
          tmpStr = "" + tmpStr.charAt(1);
        }
        setEnclosureCharacters(tmpStr);
      }
    }

    setTreatZerosAsMissing(Utils.getFlag("treat-zeros-as-missing", options));

    if (!m_suppressQuantileOptions) {
      setComputeQuartilesAsPartOfSummaryStats(Utils.getFlag(
        "compute-quartiles", options)); //$NON-NLS-1$

      tmpStr = Utils.getOption("compression", options);
      if (tmpStr.length() > 0) {
        setCompressionLevelForQuartileEstimation(Double.parseDouble(tmpStr));
      }

      tmpStr = Utils.getOption("decimal-places", options);
      if (tmpStr.length() > 0) {
        setNumDecimalPlaces(Integer.parseInt(tmpStr));
      }
    }

    while (true) {
      tmpStr = Utils.getOption('L', options);
      if (tmpStr.length() == 0) {
        break;
      }

      m_nominalLabelSpecs.add(tmpStr);
    }

    while (true) {
      tmpStr = Utils.getOption("default-label", options);
      if (tmpStr.length() == 0) {
        break;
      }

      m_nominalDefaultLabelSpecs.add(tmpStr);
    }
  }

  /**
   * Set the number of decimal places for outputting summary stats
   *
   * @param numDecimalPlaces number of decimal places to use
   */
  public void setNumDecimalPlaces(int numDecimalPlaces) {
    m_decimalPlaces = numDecimalPlaces;
  }

  /**
   * Get the number of decimal places for outputting summary stats
   *
   * @return number of decimal places to use
   */
  public int getNumDecimalPlaces() {
    return m_decimalPlaces;
  }

  /**
   * Set whether, for hitherto thought to be numeric columns, to treat any
   * unparsable values as missing value.
   *
   * @param unparsableNumericValuesToMissing
   */
  public void setTreatUnparsableNumericValuesAsMissing(
    boolean unparsableNumericValuesToMissing) {
    m_treatUnparsableNumericValuesAsMissing = unparsableNumericValuesToMissing;
  }

  /**
   * Get whether, for hitherto thought to be numeric columns, to treat any
   * unparsable values as missing value.
   *
   * @return true if unparsable numeric values are to be treated as missing
   */
  public boolean getTreatUnparsableNumericValuesAsMissing() {
    return m_treatUnparsableNumericValuesAsMissing;
  }

  /**
   * Get whether to treat zeros as missing values for numeric attributes when
   * computing summary statistics.
   *
   * @return true if zeros are to be treated as missing values for the purposes
   *         of computing summary stats.
   */
  public boolean getTreatZerosAsMissing() {
    return m_treatZeroAsMissing;
  }

  /**
   * Set whether to treat zeros as missing values for numeric attributes when
   * computing summary statistics.
   *
   * @param t true if zeros are to be treated as missing values for the purposes
   *          of computing summary stats.
   */
  public void setTreatZerosAsMissing(boolean t) {
    m_treatZeroAsMissing = t;
  }

  /**
   * Get the compression level to use in the TDigest quantile estimators
   *
   * @return the compression level (smaller values give higher compression and
   *         less accurate estimates).
   */
  public double getCompressionLevelForQuartileEstimation() {
    return m_quantileCompression;
  }

  /**
   * Set the compression level to use in the TDigest quantile estimators
   *
   * @param compression the compression level (smaller values give higher
   *          compression and less accurate estimates).
   */
  public void setCompressionLevelForQuartileEstimation(double compression) {
    m_quantileCompression = compression;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String compressionLevelForQuartileEstimationTipText() {
    return "Level of compression to use when computing estimated quantiles "
      + "(smaller is more compression). Less compression gives more accurate "
      + "estimates at the expense of time and space.";
  }

  /**
   * Get whether to include estimated quartiles in the profiling stats
   *
   * @return true if quartiles are to be estimated
   */
  public boolean getComputeQuartilesAsPartOfSummaryStats() {
    return m_estimateQuantiles;
  }

  /**
   * Set whether to include estimated quartiles in the profiling stats
   *
   * @param c true if quartiles are to be estimated
   */
  public void setComputeQuartilesAsPartOfSummaryStats(boolean c) {
    m_estimateQuantiles = c;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String computeQuartilesAsPartOfSummaryStatsTipText() {
    return "Include estimated quartiles and histograms in summary statistics (note "
      + "that this increases run time).";
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
   * Sets the placeholder for missing values.
   *
   * @param value the placeholder
   */
  public void setMissingValue(String value) {
    m_MissingValue = value;
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
   * Returns the current attribute range to be forced to type string.
   *
   * @return the range
   */
  public String getStringAttributes() {
    return m_stringRange;
    // return m_forceString.getRanges();
  }

  /**
   * Sets the attribute range to be forced to type string.
   *
   * @param value the range
   */
  public void setStringAttributes(String value) {
    m_stringRange = value;
    // m_forceString.setRanges(value);
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
   * Returns the current attribute range to be forced to type nominal.
   *
   * @return the range
   */
  public String getNominalAttributes() {
    return m_nominalRange;
    // return m_forceNominal.getRanges();
  }

  /**
   * Sets the attribute range to be forced to type nominal.
   *
   * @param value the range
   */
  public void setNominalAttributes(String value) {
    m_nominalRange = value;
    // m_forceNominal.setRanges(value);
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
   * Get the format to use for parsing date values.
   *
   * @return the format to use for parsing date values.
   *
   */
  public String getDateFormat() {
    return m_dateFormat;
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
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dateFormatTipText() {
    return "The format to use for parsing date values.";
  }

  /**
   * Returns the current attribute range to be forced to type date.
   *
   * @return the range.
   */
  public String getDateAttributes() {
    return m_dateRange;
    // return m_forceDate.getRanges();
  }

  /**
   * Set the attribute range to be forced to type date.
   *
   * @param value the range
   */
  public void setDateAttributes(String value) {
    m_dateRange = value;
    // m_forceDate.setRanges(value);
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
   * Get the character(s) to use/recognize as string enclosures
   *
   * @return the characters to use as string enclosures
   */
  public String getEnclosureCharacters() {
    return m_Enclosures;
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
   * Returns the character used as column separator.
   *
   * @return the character to use
   */
  public String getFieldSeparator() {
    return Utils.backQuoteChars(m_FieldSeparator);
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
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fieldSeparatorTipText() {
    return "The character to use as separator for the columns/fields (use '\\t' for TAB).";
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nominalDefaultLabelSpecsTipText() {
    return "Specificaton of an optional 'default' label for nominal attributes. "
      + "To be used in conjuction with nominalLabelSpecs in the case where "
      + "you only want to specify some of the legal values that "
      + "a given attribute can take on. Any remaining values are then "
      + "assigned to this 'default' category. One use-case is to "
      + "easily convert a multi-class problem into a binary one - "
      + "in this case, only the positive class label need be specified "
      + "via nominalLabelSpecs and then the default label acts as a "
      + "catch-all for the rest. The specification format is the "
      + "same as for nominalLabelSpecs, namely "
      + "[index range | attribute name list]:<default label>";
  }

  /**
   * Get the default label specifications for nominal attributes
   *
   * @return an array of default label specifications
   */
  public Object[] getNominalDefaultLabelSpecs() {
    return m_nominalDefaultLabelSpecs.toArray(new String[0]);
  }

  /**
   * Set the default label specifications for nominal attributes
   *
   * @param specs an array of default label specifications
   */
  public void setNominalDefaultLabelSpecs(Object[] specs) {
    m_nominalDefaultLabelSpecs.clear();
    for (Object s : specs) {
      m_nominalDefaultLabelSpecs.add(s.toString());
    }
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nominalLabelSpecsTipText() {
    return "Optional specification of legal labels for nominal "
      + "attributes. May be specified multiple times. " + "The "
      + "spec contains two parts separated by a \":\". The "
      + "first part can be a range of attribute indexes or "
      + "a comma-separated list off attruibute names; the "
      + "second part is a comma-separated list of labels. E.g "
      + "\"1,2,4-6:red,green,blue\" or \"att1,att2:red,green,blue\"";
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

  /**
   * Only initialize enough stuff in order to parse rows and construct instances
   *
   * @param attNames the names of the attributes to use
   */
  public void initParserOnly(List<String> attNames) {
    char encl = m_Enclosures.charAt(0);
    if (encl == '\\' && m_Enclosures.length() == 2) {
      encl = m_Enclosures.charAt(1);
    }
    m_parser = new CSVParser(m_FieldSeparator.charAt(0), encl, '\\');

    m_attributeNames = attNames;
    if (attNames != null) {
      processRanges(attNames.size(), TYPE.UNDETERMINED);
      processNominalSpecs(attNames.size());
    }
  }

  // called after map processing

  /**
   * Just parse a row.
   *
   * @param row the row to parse
   * @return the values of the row in an array
   * @throws IOException if a problem occurs
   */
  public String[] parseRowOnly(String row) throws IOException {
    return m_parser.parseLine(row);
  }

  /**
   * Process a tokenized row of values. attNames may be non-null for the first
   * row and is optional. If not supplied then names will be generated on
   * receiving the first row of data. An exception will be raised on subsequent
   * rows that don't have the same number of fields as seen in the first row
   *
   * @param fieldVals the row values to process
   * @param attNames the names of the attributes (fields)
   * @exception if the number of fields in the current row does not match the
   *              number of attribute names
   */
  public void processRowValues(Object[] fieldVals, List<String> attNames)
    throws DistributedWekaException, IOException {

    if (m_attributeTypes == null) {
      if (attNames != null && fieldVals.length != attNames.size()) {
        throw new IOException("Expected " + attNames.size()
          + " fields, but got " + fieldVals.length + " for row");
      }

      if (attNames == null) {
        generateNames(fieldVals.length);
      } else {
        m_attributeNames = attNames;
      }

      // process ranges etc.
      processRanges(fieldVals.length, TYPE.UNDETERMINED);
      processNominalSpecs(fieldVals.length);
    }

    if (fieldVals.length != m_attributeNames.size()) {
      throw new IOException("Expected " + m_attributeNames.size()
        + " fields, but got " + fieldVals.length + " for row");
    }

    // should try to alert the user to all data issues in this phase (i.e.
    // before getting to the model building). E.g. unparseable dates,
    // numbers etc.
    for (int i = 0; i < fieldVals.length; i++) {
      if (fieldVals[i] != null
        && !fieldVals[i].toString().equals(m_MissingValue)
        && fieldVals[i].toString().trim().length() != 0) {
        if (m_attributeTypes[i] == TYPE.NUMERIC
          || m_attributeTypes[i] == TYPE.UNDETERMINED) {
          try {
            double value = Double.parseDouble(fieldVals[i].toString());
            m_attributeTypes[i] = TYPE.NUMERIC;

            if (m_computeSummaryStats) {
              updateSummaryStats(m_summaryStats, m_stringBackupStats,
                m_attributeNames.get(i), value, null, false, false,
                m_treatZeroAsMissing, m_estimateQuantiles,
                m_quantileCompression);
            }
          } catch (NumberFormatException ex) {

            if (m_attributeTypes[i] == TYPE.UNDETERMINED) {
              // assume its an enumerated value
              m_attributeTypes[i] = TYPE.NOMINAL;
              TreeSet<String> ts = new TreeSet<String>();

              String defaultLabel = m_nominalDefaultVals.get(i);
              String toAdd = defaultLabel;
              if (defaultLabel != null && fieldVals[i].equals(defaultLabel)) {
                // don't add it if it's the default label
              } else {
                ts.add(fieldVals[i].toString());
                toAdd = fieldVals[i].toString();
              }
              m_nominalVals.put(i, ts);

              if (m_computeSummaryStats) {
                updateSummaryStats(m_summaryStats, m_stringBackupStats,
                  m_attributeNames.get(i), 1, toAdd, true, false,
                  m_treatZeroAsMissing, m_estimateQuantiles,
                  m_quantileCompression);
              }
            } else {
              if (!m_treatUnparsableNumericValuesAsMissing) {
                m_attributeTypes[i] = TYPE.STRING;
                if (m_computeSummaryStats) {
                  updateSummaryStats(m_summaryStats, m_stringBackupStats,
                    m_attributeNames.get(i), 1, fieldVals[i].toString(), false,
                    true, m_treatZeroAsMissing, m_estimateQuantiles,
                    m_quantileCompression);
                }
              } else {
                // missing value
                updateSummaryStats(m_summaryStats, m_stringBackupStats,
                  m_attributeNames.get(i), Utils.missingValue(), null,
                  m_attributeTypes[i] == TYPE.NOMINAL,
                  m_attributeTypes[i] == TYPE.STRING, m_treatZeroAsMissing,
                  m_estimateQuantiles, m_quantileCompression);
              }
            }
          }
        } else if (m_attributeTypes[i] == TYPE.DATE) {
          // check that date is parseable
          Date d = fieldVals[i] instanceof Date ? (Date) fieldVals[i] : null;
          if (d == null) {
            try {
              d = m_formatter.parse(fieldVals[i].toString());
            } catch (ParseException e) {
              throw new DistributedWekaException(e);
            }
          }
          if (m_computeSummaryStats) {
            updateSummaryStats(m_summaryStats, m_stringBackupStats,
              m_attributeNames.get(i), d.getTime(), null, false, false,
              m_treatZeroAsMissing, m_estimateQuantiles, m_quantileCompression);
          }

        } else if (m_attributeTypes[i] == TYPE.NOMINAL) {
          String defaultLabel = m_nominalDefaultVals.get(i);
          if (defaultLabel != null) {
            String toUpdate = defaultLabel;
            if (m_nominalVals.get(i).contains(fieldVals[i])) {
              toUpdate = fieldVals[i].toString();
            }

            if (m_computeSummaryStats) {
              updateSummaryStats(m_summaryStats, m_stringBackupStats,
                m_attributeNames.get(i), 1, toUpdate, true, false,
                m_treatZeroAsMissing, m_estimateQuantiles,
                m_quantileCompression);
            }
          } else {
            m_nominalVals.get(i).add(fieldVals[i].toString());
            if (m_computeSummaryStats) {
              updateSummaryStats(m_summaryStats, m_stringBackupStats,
                m_attributeNames.get(i), 1, fieldVals[i].toString(), true,
                false, m_treatZeroAsMissing, m_estimateQuantiles,
                m_quantileCompression);
            }
          }
        } else if (m_attributeTypes[i] == TYPE.STRING) {
          if (m_computeSummaryStats) {
            updateSummaryStats(m_summaryStats, m_stringBackupStats,
              m_attributeNames.get(i), 1, fieldVals[i].toString(), false, true,
              m_treatZeroAsMissing, m_estimateQuantiles, m_quantileCompression);
          }
        }
      } else {
        // missing value
        if (m_computeSummaryStats) {
          updateSummaryStats(m_summaryStats, m_stringBackupStats,
            m_attributeNames.get(i), Utils.missingValue(), null,
            m_attributeTypes[i] == TYPE.NOMINAL,
            m_attributeTypes[i] == TYPE.STRING, m_treatZeroAsMissing,
            m_estimateQuantiles, m_quantileCompression);
        }
      }
    }
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

      char encl = m_Enclosures.charAt(0);
      if (encl == '\\' && m_Enclosures.length() == 2) {
        encl = m_Enclosures.charAt(1);
      }

      // tokenize the first line
      m_parser = new CSVParser(m_FieldSeparator.charAt(0), encl, '\\');

      fields = m_parser.parseLine(row);
    }

    // process the row
    if (fields == null) {
      try {
        fields = m_parser.parseLine(row);
      } catch (IOException e) {
        m_parsingErrors++;
        if (m_parsingErrors > MAX_PARSING_ERRORS) {
          throw e;
        }
        System.err.println("CSV parsing error: " + e.getMessage()
          + "\n\nFor line:\n" + row);
        return;
      }
    }

    processRowValues(fields, attNames);
  }

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
   * Get the header information and the encoded quantile estimators
   *
   * @return a holder instance containing both the header information and
   *         encoded quantile estimators
   * @throws DistributedWekaException if we are not computing summary statistics
   *           or we are computing statistics but not quantiles
   */
  public HeaderAndQuantileDataHolder getHeaderAndQuantileEstimators()
    throws DistributedWekaException {
    if (!m_computeSummaryStats) {
      throw new DistributedWekaException("No summary stats computed!");
    }

    if (!m_estimateQuantiles) {
      throw new DistributedWekaException("No quantile information computed!");
    }

    Map<String, TDigest> quantileMap = new HashMap<String, TDigest>();
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.NUMERIC
        || m_attributeTypes[i] == TYPE.DATE) {
        NumericStats ns =
          (NumericStats) m_summaryStats.get(m_attributeNames.get(i));

        if (ns.getQuantileEstimator() != null) {
          quantileMap.put(m_attributeNames.get(i), ns.getQuantileEstimator());
        }
      }
    }

    HeaderAndQuantileDataHolder holder =
      new HeaderAndQuantileDataHolder(getHeader(), quantileMap);
    return holder;
  }

  /**
   * Serialize all TDigest quantile estimators in use
   */
  public void serializeAllQuantileEstimators() {
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.NUMERIC
        || m_attributeTypes[i] == TYPE.DATE) {
        NumericStats ns =
          (NumericStats) m_summaryStats.get(m_attributeNames.get(i));
        ns.serializeCurrentQuantileEstimator();
      }
    }
  }

  /**
   * Deserialize all TDigest quantile estimators in use
   */
  public void deSerializeAllQuantileEstimators() {
    for (int i = 0; i < m_attributeTypes.length; i++) {
      if (m_attributeTypes[i] == TYPE.NUMERIC
        || m_attributeTypes[i] == TYPE.DATE) {
        NumericStats ns =
          (NumericStats) m_summaryStats.get(m_attributeNames.get(i));
        ns.deSerializeCurrentQuantileEstimator();
      }
    }
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

    if (!DistributedJobConfig.isEmpty(getStringAttributes())) {
      m_forceString.setRanges(getStringAttributes());
    }

    if (!DistributedJobConfig.isEmpty(getNominalAttributes())) {
      m_forceNominal.setRanges(getNominalAttributes());
    }

    if (!DistributedJobConfig.isEmpty(getDateAttributes())) {
      m_forceDate.setRanges(getDateAttributes());
    }

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

    if (m_nominalDefaultLabelSpecs.size() > 0) {
      for (String spec : m_nominalDefaultLabelSpecs) {
        String[] attsAndLabel = spec.split(":");
        if (attsAndLabel.length == 2) {
          String label = attsAndLabel[1];

          try {
            // try as a range string first
            Range tempR = new Range();
            tempR.setRanges(attsAndLabel[0].trim());
            tempR.setUpper(numFields - 1);

            int[] rangeIndexes = tempR.getSelection();
            for (int rangeIndexe : rangeIndexes) {
              // these specs should correspond with nominal attribute specs
              // above -
              // so the type should already be set for this
              if (m_attributeTypes[rangeIndexe] == TYPE.NOMINAL) {
                m_nominalDefaultVals.put(rangeIndexe, label);
              }
            }
          } catch (IllegalArgumentException e) {
            // one or more named attributes?
            String[] attNames = attsAndLabel[0].split(",");
            for (String attN : attNames) {
              int attIndex = m_attributeNames.indexOf(attN);
              if (attIndex >= 0) {
                if (m_attributeTypes[attIndex] == TYPE.NOMINAL) {
                  m_nominalDefaultVals.put(attIndex, label);
                }
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
        TreeSet<String> treeVals = new TreeSet<String>();
        treeVals.addAll(m_nominalVals.get(i));
        // TreeSet<String> vals = m_nominalVals.get(i);

        // Add the default label into the spec
        if (m_nominalDefaultVals.get(i) != null) {
          treeVals.add(m_nominalDefaultVals.get(i));
        }

        ArrayList<String> theVals = new ArrayList<String>();
        if (treeVals.size() > 0) {
          for (String v : treeVals) {
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

    if (m_computeSummaryStats && m_summaryStats.size() > 0) {
      for (int i = 0; i < m_attributeTypes.length; i++) {
        if (m_attributeTypes[i] == TYPE.NUMERIC
          || m_attributeTypes[i] == TYPE.DATE) {
          NumericStats ns =
            (NumericStats) m_summaryStats.get(m_attributeNames.get(i));

          attribs.add(ns.makeAttribute());
        } else if (m_attributeTypes[i] == TYPE.NOMINAL) {
          NominalStats ns =
            (NominalStats) m_summaryStats.get(m_attributeNames.get(i));
          attribs.add(ns.makeAttribute());
        } else if (m_attributeTypes[i] == TYPE.STRING) {
          StringStats ss =
            (StringStats) m_summaryStats.get(m_attributeNames.get(i));
          attribs.add(ss.makeAttribute());
        }
      }
    }

    Instances structure = new Instances("A relation name", attribs, 0);

    return structure;
  }

  /**
   * Initialize internal state using the supplied ARFF header with summary
   * attributes. Assumes that setOptions() has already been called on this
   * instance of CSVToARFFHeaderMapTask.
   *
   * @param headerWithSummary the ARFF header (with summary attributes) to
   *          initialize with
   * @param quantileEstimators a map (keyed by attribute name) of TDigest
   *          estimators for numeric attributes (can be null if quantiles are
   *          not being estimated)
   * @throws DistributedWekaException if a problem occurs
   */
  public void fromHeader(Instances headerWithSummary,
    Map<String, TDigest> quantileEstimators) throws DistributedWekaException {
    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    m_attributeTypes = new TYPE[headerNoSummary.numAttributes()];
    m_attributeNames = new ArrayList<String>();
    m_nominalVals = new HashMap<Integer, TreeSet<String>>();

    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      String attName = headerNoSummary.attribute(i).name();
      if (headerNoSummary.attribute(i).isNominal()) {
        m_attributeTypes[i] = TYPE.NOMINAL;
        TreeSet<String> vals = new TreeSet<String>();
        for (int j = 0; j < headerNoSummary.attribute(i).numValues(); j++) {
          vals.add(headerNoSummary.attribute(i).value(j));
        }
        m_nominalVals.put(i, vals);
      } else if (headerNoSummary.attribute(i).isString()) {
        m_attributeTypes[i] = TYPE.STRING;
      } else if (headerNoSummary.attribute(i).isDate()) {
        m_attributeTypes[i] = TYPE.DATE;
      } else if (headerNoSummary.attribute(i).isNumeric()) {
        m_attributeTypes[i] = TYPE.NUMERIC;
      } else {
        m_attributeTypes[i] = TYPE.UNDETERMINED;
      }

      m_attributeNames.add(attName);
    }

    m_summaryStats = new HashMap<String, Stats>();
    // re-construct summary Stats
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      String attName = headerNoSummary.attribute(i).name();
      Attribute origAtt = headerNoSummary.attribute(i);
      Attribute summaryAtt =
        headerWithSummary.attribute(ARFF_SUMMARY_ATTRIBUTE_PREFIX + attName);
      if (summaryAtt != null) {
        Stats s = null;
        if (origAtt.isNominal()) {
          s = NominalStats.attributeToStats(summaryAtt);
        } else if (origAtt.isString()) {
          s = StringStats.attributeToStats(summaryAtt);
        } else if (origAtt.isNumeric()) {
          s = NumericStats.attributeToStats(summaryAtt);
        }

        m_summaryStats.put(attName, s);
      }
    }

    // estimators
    if (quantileEstimators != null && quantileEstimators.size() > 0) {
      for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
        if (headerNoSummary.attribute(i).isNumeric()) {
          TDigest estimator =
            quantileEstimators.get(headerNoSummary.attribute(i).name());
          if (estimator != null) {
            NumericStats numStats =
              (NumericStats) m_summaryStats.get(headerNoSummary.attribute(i)
                .name());
            numStats.setQuantileEstimator(estimator);
          }
        }
      }
    }
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
    return makeInstanceFromObjectRow(trainingHeader, setStringValues, parsed,
      sparse);
  }

  /**
   * Utility method for Constructing an instance given an array of Objects
   *
   * @param trainingHeader the header to associate the instance with. Does not
   *          add the new instance to this data set; just gives the instance a
   *          reference to the header
   * @param setStringValues true if any string values should be set in the
   *          header as opposed to being added to the header (i.e. accumulating
   *          in the header).
   * @param row the array of Object values
   * @param sparse true if the new instance is to be a sparse instance
   * @return an Instance
   * @throws Exception if a problem occurs
   */
  public Instance makeInstanceFromObjectRow(Instances trainingHeader,
    boolean setStringValues, Object[] row, boolean sparse) throws Exception {

    double[] vals = new double[trainingHeader.numAttributes()];

    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      if (row[i] == null || row[i].toString().equals(getMissingValue())
        || row[i].toString().trim().length() == 0) {
        vals[i] = Utils.missingValue();
        continue;
      }

      Attribute current = trainingHeader.attribute(i);
      if (current.isString()) {
        if (setStringValues) {
          current.setStringValue(row[i].toString());
          vals[i] = 0;
        } else {
          vals[i] = current.addStringValue(row[i].toString());
        }
      } else if (current.isNominal()) {
        int index = current.indexOfValue(row[i].toString());

        if (index < 0) {
          if (m_nominalDefaultVals.get(i) != null) {
            index = current.indexOfValue(m_nominalDefaultVals.get(i));
          }

          if (index < 0) {
            throw new Exception("Can't find nominal value '"
              + row[i].toString() + "' in list of values for " + "attribute '"
              + current.name() + "'");
          }
        }
        vals[i] = index;
      } else if (current.isDate()) {
        double val = 0;
        if (row[i] instanceof Date) {
          val = ((Date) row[i]).getTime();
        } else {
          try {
            val = current.parseDate(row[i].toString());
          } catch (ParseException p) {
            throw new Exception(p);
          }
        }
        vals[i] = val;
      } else if (current.isNumeric()) {
        if (row[i] instanceof Number) {
          vals[i] = ((Number) row[i]).doubleValue();
        } else {
          try {
            vals[i] = Double.parseDouble(row[i].toString());
          } catch (NumberFormatException n) {
            throw new Exception(n);
          }
        }
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

  /**
   * Get the default label for a given attribute. May be null if a default value
   * hasn't been specified
   *
   * @param attIndex the index (0-based) of the attribute to get the default
   *          value for
   * @return the default value or null (if a default has not been specified)
   */
  public String getDefaultValue(int attIndex) {
    return m_nominalDefaultVals.get(attIndex);
  }

  /**
   * Enumerated type for specifying the type of each attribute in the data
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected enum TYPE {
    UNDETERMINED, NUMERIC, NOMINAL, STRING, DATE;
  }

  /**
   * Container class for a Instances header with basic summary stats and a map
   * of TDigest quantile estimators for numeric attributes
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public static class HeaderAndQuantileDataHolder implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -5741832014478935587L;

    protected Instances m_header;
    protected Map<String, byte[]> m_encodedQuantileEstimators;

    /**
     * Constructor
     * 
     * @param header the header with summary attributes
     * @param quantileEstimators a map of TDigest quantile estimators keyed by
     *          attribute name
     */
    public HeaderAndQuantileDataHolder(Instances header,
      Map<String, TDigest> quantileEstimators) {

      m_header = header;

      if (quantileEstimators != null && quantileEstimators.size() > 0) {
        m_encodedQuantileEstimators =
          new HashMap<String, byte[]>(quantileEstimators.size());
        for (Map.Entry<String, TDigest> q : quantileEstimators.entrySet()) {
          ByteBuffer buff = ByteBuffer.allocate(q.getValue().byteSize());
          q.getValue().asSmallBytes(buff);
          m_encodedQuantileEstimators.put(q.getKey(), buff.array());
        }
      }
    }

    /**
     * Get the header
     * 
     * @return the header
     */
    public Instances getHeader() {
      return m_header;
    }

    /**
     * Return a decoded TDigest quantile estimator
     * 
     * @param attributeName the name of the attribute to get the estimator for
     * @return the decoded estimator
     * @throws DistributedWekaException if there are no quantile estimators or
     *           the named one is not in the map
     */
    public TDigest getQuantileEstimator(String attributeName)
      throws DistributedWekaException {
      if (m_encodedQuantileEstimators == null
        || m_encodedQuantileEstimators.size() == 0) {
        throw new DistributedWekaException("No quantile estimators!");
      }

      byte[] encoded = m_encodedQuantileEstimators.get(attributeName);

      if (encoded == null) {
        throw new DistributedWekaException(
          "Can't find a quantile estimator for attribute '" + attributeName
            + "'");
      }

      ByteBuffer buff = ByteBuffer.wrap(encoded);
      TDigest returnVal = TDigest.fromBytes(buff);

      return returnVal;
    }
  }
}
