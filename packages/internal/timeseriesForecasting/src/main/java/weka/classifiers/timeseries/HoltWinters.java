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
 *    HoltWinters.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Class implementing the Holt-Winters triple exponential smoothing method for
 * time series forecasting. Designed to be used in the Weka forecasting
 * environment. Although lagged variables are not used by this method, for
 * evaluation purposes within the framework the user is required to set a lag
 * length that is at least 3 times that of the seasonal cycle length. This is
 * because priming data (which is set by the framework to have as many instances
 * as the lag length) is used to train the Holt-Winters forecaster. Two seasonal
 * cycles worth of data are used to set the initial parameters of the method
 * (primarily the seasonal components) and the remaining training data is used
 * to smooth over.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class HoltWinters extends AbstractClassifier implements
  PrimingDataLearner, OptionHandler, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4478856847945888899L;

  /** Value smoothing parameter */
  protected double m_alpha = 0.2;

  /** Trend smoothing parameter */
  protected double m_beta = 0.2;

  /** Seasonal smoothing parameter */
  protected double m_gamma = 0.2;

  /** The length of the seasonal cycle */
  protected int m_seasonCycleLength = 12; // monthly data (1 year)

  protected boolean m_includeSeason = true;

  protected boolean m_includeTrend = true;

  /** The seasonal correction factors */
  protected double[] m_c;

  /** Used for setting initial estimates */
  protected double[] m_twoSeasonsOfY;

  /** % m_seasonCycleLength gives the position in the seasonal cycle */
  protected int m_counter;

  /**
   * Holds the number of training points that were used to estimate the starting
   * parameters of the model (i.e. initial smoothed value, trend estimate and
   * seasonal correction factors)
   */
  // protected int m_numTrainingPoints;

  /** Used in computing the smoothed value */
  protected double m_sPrev;

  /** Used in computing the smoothed trend */
  protected double m_bPrev;

  /** Smoothed value estimate */
  protected double m_s;

  /** Trend */
  protected double m_b;

  /**
   * Description of this forecaster.
   * 
   * @return a description of this forecaster
   */
  public String globalInfo() {
    return "Class implementing the Holt-Winters triple exponential smoothing method. "
      + "Use the max lag length option in the time series forecasting "
      + "environment to control how many of the most recent training "
      + "data instances are used for estimating the initial parameters "
      + "and smoothing. If not specified manually, the max lag will be set "
      + "to 3 * seasonal cycle length";
  }

  @Override
  public void reset() {
    m_sPrev = Utils.missingValue();
    m_bPrev = Utils.missingValue();
    m_s = Utils.missingValue();
    m_b = Utils.missingValue();

    m_c = new double[m_seasonCycleLength];
    m_twoSeasonsOfY = new double[2 * m_seasonCycleLength];
    m_counter = 0;
    // m_numTrainingPoints = 0;
  }

  /**
   * Return the minimum number of training/priming data points required before a
   * forecast can be made
   * 
   * @return the minimum number of training/priming data points required
   */
  @Override
  public int getMinRequiredTrainingPoints() {

    // two full seasons for setting the seasonal correction factors and
    // one season for smoothing over.
    return 3 * m_seasonCycleLength;
  }

  /**
   * Update the smoothed estimates of value and trend
   * 
   * @param value the point to update with
   */
  protected void update(double value) {
    // value
    double seasonAdjust = m_includeSeason ? m_c[m_counter % m_seasonCycleLength]
      : 1.0;
    double trendAdjust = m_includeTrend ? m_bPrev : 0.0;

    m_s = (m_alpha * value / seasonAdjust)
      + ((1 - m_alpha) * (m_sPrev + trendAdjust));

    // trend
    if (m_includeTrend) {
      m_b = (m_beta * (m_s - m_sPrev)) + ((1.0 - m_beta) * m_bPrev);
    }

    // season
    if (m_includeSeason) {
      m_c[m_counter % m_seasonCycleLength] = (m_gamma * value / m_s)
        + ((1.0 - m_gamma) * m_c[m_counter % m_seasonCycleLength]);
    }

    m_counter++; // increment to the next time step

    // System.out.println("" + (m_counter + 0) + " " + (m_s + m_b)
    // * m_c[(m_counter + 0) % m_seasonCycleLength]);

    m_sPrev = m_s;
    m_bPrev = m_b;
  }

  /**
   * Initializes the value, trend and seasonal correction factors estimations
   * once we have seen enough data for the seasonal correction factors
   */
  protected void initialize() {
    double[] obsFirstYear = new double[m_seasonCycleLength];
    System.arraycopy(m_twoSeasonsOfY, 0, obsFirstYear, 0, m_seasonCycleLength);
    double sum = Utils.sum(obsFirstYear);
    m_sPrev = sum / m_seasonCycleLength;

    double[] obsSecondYear = new double[m_seasonCycleLength];
    System.arraycopy(m_twoSeasonsOfY, m_seasonCycleLength, obsSecondYear, 0,
      m_seasonCycleLength);
    sum = Utils.sum(obsSecondYear);
    double avgSecondYear = sum / m_seasonCycleLength;

    m_bPrev = (m_sPrev - avgSecondYear) / m_seasonCycleLength;

    // initialize the seasonal correction factors
    for (int i = 1; i <= m_seasonCycleLength; i++) {
      m_c[i - 1] = (m_twoSeasonsOfY[i - 1] - (i - 1) * m_bPrev / 2) / m_sPrev;
    }
  }

  /**
   * Update the smoothed estimates using the supplied value
   * 
   * @param primingOrPredictedTargetValue the value to update with
   */
  @Override
  public void updateForecaster(double primingOrPredictedTargetValue) {
    if (m_counter < 2 * m_seasonCycleLength) {
      if (!Utils.isMissingValue(primingOrPredictedTargetValue)) {
        m_twoSeasonsOfY[m_counter++] = primingOrPredictedTargetValue;
      }
      return;
    }

    if (m_counter == 2 * m_seasonCycleLength) {
      // initialize
      initialize();
    }

    if (m_counter >= 2 * m_seasonCycleLength) {
      if (!Utils.isMissingValue(primingOrPredictedTargetValue)) {
        // update
        update(primingOrPredictedTargetValue);
        // m_counter++; // increment to the next time step
      }
    }
  }

  /**
   * Generates a one-step ahead forecast. Clients should follow this with a call
   * to updateForecaster() and pass in the forecasted value if they want to
   * generate further projected values (i.e. beyond one-step ahead).
   * 
   * @throws Exception if forecast can't be produced
   * @return a one-step ahead forecast
   */
  public double forecast() throws Exception {

    if (m_counter < m_seasonCycleLength * 2) {
      throw new Exception(
        "Haven't seen enough training data to make a forecast yet");
    }

    // we use counter % season here because the last training update has
    // already incremented counter to the next time step
    double result = (m_s + (m_includeTrend ? m_b : 0.0))
      * (m_includeSeason ? m_c[m_counter % m_seasonCycleLength] : 1.0);

    return result;
  }

  @Override
  public void buildClassifier(Instances data) throws Exception {
    reset();

    for (int i = 0; i < data.numInstances(); i++) {
      updateForecaster(data.instance(i).classValue());
    }
  }

  @Override
  public double classifyInstance(Instance inst) throws Exception {
    return forecast();
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> opts = new Vector<Option>();

    opts.add(new Option("\tExclude trend correction.", "no-trend", 0,
      "no-trend"));
    opts.add(new Option("\tExclude seasonal correction.", "no-season", 0,
      "no-season"));
    opts.add(new Option("\tSet the value smoothing factor (default = 0.2)",
      "alpha", 1, "-alpha <number>"));
    opts.add(new Option("\tSet the trend smoothing factor (default = 0.2)",
      "beta", 1, "-beta <number>"));
    opts.add(new Option("\tSet the seasonal smoothing factor (default = 0.2)",
      "gamma", 1, "-gamma <number>"));
    opts.add(new Option(
      "\tSet the season cycle length (default = 12 (for monthly data))",
      "cycle-length", 1, "-cycle-length <integer>"));

    return opts.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    setExcludeTrendCorrection(Utils.getFlag("no-trend", options));
    setExcludeSeasonalCorrection(Utils.getFlag("no-season", options));

    String val = Utils.getOption("alpha", options);
    if (val.length() > 0) {
      setValueSmoothingFactor(Double.parseDouble(val));
    }

    val = Utils.getOption("beta", options);
    if (val.length() > 0) {
      setTrendSmoothingFactor(Double.parseDouble(val));
    }

    val = Utils.getOption("gamma", options);
    if (val.length() > 0) {
      setSeasonalSmoothingFactor(Double.parseDouble(val));
    }

    val = Utils.getOption("cycle-length", options);
    if (val.length() > 0) {
      setSeasonCycleLength(Integer.parseInt(val));
    }
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (getExcludeTrendCorrection()) {
      options.add("-no-trend");
    }
    if (getExcludeSeasonalCorrection()) {
      options.add("-no-season");
    }
    options.add("-alpha");
    options.add("" + getValueSmoothingFactor());
    options.add("-beta");
    options.add("" + getTrendSmoothingFactor());
    options.add("-gamma");
    options.add("" + getSeasonalSmoothingFactor());
    options.add("cycle-length");
    options.add("" + getSeasonCycleLength());

    return options.toArray(new String[options.size()]);
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String seasonCycleLengthTipText() {
    return "The length of the seasonal cycle (e.g. 12 for monthly data; 4 for quarterly data)";
  }

  /**
   * Set the length of a "year", i.e. the number of seasons for the seasonal
   * cycle
   * 
   * @param length the number of seasons in a cycle
   */
  public void setSeasonCycleLength(int length) {
    m_seasonCycleLength = length;
  }

  /**
   * Get the length of a "year", i.e. the number of seasons for the seasonal
   * cycle
   * 
   * @param length the number of seasons in a cycle
   */
  public int getSeasonCycleLength() {
    return m_seasonCycleLength;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String excludeSeasonalCorrectionTipText() {
    return "Don't include the seasonal correction";
  }

  /**
   * Set whether to exclude the seasonal correction
   * 
   * @param s true if the seasonal correction is to be excluded
   */
  public void setExcludeSeasonalCorrection(boolean s) {
    m_includeSeason = !s;
  }

  /**
   * Get whether to exclude the seasonal correction
   * 
   * @return true if the seasonal correction is to be excluded
   */
  public boolean getExcludeSeasonalCorrection() {
    return !m_includeSeason;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String excludeTrendCorrection() {
    return "Don't include the trend correction";
  }

  /**
   * Set whether to exclude the trend correction
   * 
   * @param s true if the trend correction is to be excluded
   */
  public void setExcludeTrendCorrection(boolean t) {
    m_includeTrend = !t;
  }

  /**
   * Get whether to exclude the trend correction
   * 
   * @return true if the trend correction is to be excluded
   */
  public boolean getExcludeTrendCorrection() {
    return !m_includeTrend;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String valueSmoothingFactorTipText() {
    return "The smoothing factor, between 0 and 1, for the series values";
  }

  /**
   * Set the value smoothing factor
   * 
   * @param s the value smoothing factor
   */
  public void setValueSmoothingFactor(double s) {
    m_alpha = s;
  }

  /**
   * Get the value smoothing factor
   * 
   * @return the value smoothing factor
   */
  public double getValueSmoothingFactor() {
    return m_alpha;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String trendSmoothingFactorTipText() {
    return "The smoothing factor, between 0 and 1, for the trend";
  }

  /**
   * Set the trend smoothing factor
   * 
   * @param t the trend smoothing factor
   */
  public void setTrendSmoothingFactor(double t) {
    m_beta = t;
  }

  /**
   * Get the trend smoothing factor
   * 
   * @param t the trend smoothing factor
   */
  public double getTrendSmoothingFactor() {
    return m_beta;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String seasonalSmoothingFactorTipText() {
    return "The smoothing factor, between 0 and 1, for the seasonal component";
  }

  /**
   * Set the seasonal smoothing factor
   * 
   * @param s the seasonal smoothing factor
   */
  public void setSeasonalSmoothingFactor(double s) {
    m_gamma = s;
  }

  /**
   * Get the seasonal smoothing factor
   * 
   * @return the seasonal smoothing factor
   */
  public double getSeasonalSmoothingFactor() {
    return m_gamma;
  }

  /**
   * Print usage information
   */
  public static void printUsage() {

    HoltWinters h = new HoltWinters();
    StringBuffer result = new StringBuffer();
    result
      .append("General options:\n\n-t <training data>\n\tThe training data ARFF file to use\n");
    result.append("-c <class index>\n\tThe index of the series to model\n");
    result
      .append("-f <integer>\n\tThe number of time steps to forecast beyond the end of the series\n");
    Option option;

    // build option string
    result.append("\n");
    result.append(h.getClass().getName().replaceAll(".*\\.", ""));
    result.append(" options:\n\n");

    Enumeration enm = ((OptionHandler) h).listOptions();
    while (enm.hasMoreElements()) {
      option = (Option) enm.nextElement();

      result.append(option.synopsis() + "\n");
      result.append(option.description() + "\n");
    }

    System.err.println(result.toString());
  }

  @Override
  public String toString() {
    return "Holt-Winters triple exponential smoothing.\n\tValue smoothing factor: "
      + m_alpha
      + "\n\tTrend smoothing factor: "
      + m_beta
      + "\n\tSeasonal "
      + "smoothing factor: "
      + m_gamma
      + "\n\tSeason cycle length: "
      + m_seasonCycleLength + "\n\n";
  }

  /**
   * Main method for running this class
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {

      HoltWinters h = new HoltWinters();
      if (Utils.getFlag('h', args) || Utils.getFlag("help", args)) {
        System.err.println("Help requested\n\n");
        printUsage();
        System.exit(1);
      }

      String trainingData = Utils.getOption('t', args);
      if (trainingData.length() == 0) {
        System.err.println("No training set specified!\n\n");
        printUsage();
        System.exit(1);
      }
      weka.core.Instances train = new weka.core.Instances(
        new java.io.BufferedReader(new java.io.FileReader(trainingData)));

      String classIndex = Utils.getOption('c', args);
      int classToUse = 0;
      if (classIndex.length() > 0) {
        if (classIndex.toLowerCase().equals("first")) {
          classToUse = 0;
        } else if (classIndex.toLowerCase().equals("last")) {
          classToUse = train.numAttributes() - 1;
        } else {
          classToUse = Integer.parseInt(classIndex);
          classToUse--;
        }
      }

      int horizon = 1;
      String horizS = Utils.getOption('f', args);
      if (horizS.length() > 0) {
        horizon = Integer.parseInt(horizS);
      }

      train.setClassIndex(classToUse);
      h.buildClassifier(train);

      for (int i = 0; i < horizon; i++) {
        double v = h.forecast();
        System.out.println("" + Utils.doubleToString(v, 4));
        h.update(v);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
