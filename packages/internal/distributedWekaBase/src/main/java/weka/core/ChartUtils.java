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
 *    ChartUtils
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.tc33.jheatchart.HeatChart;

import weka.core.matrix.Matrix;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericAttributeBinData;

/**
 * Utility routines for plotting various charts using the JFreeChart library.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ChartUtils {

  /**
   * Utility method for retrieving the value of an option from a simple list of
   * options. Returns null if the option to get isn't present in the list, an
   * empty string if it is but no value has been specified (i.e. option is a
   * flag) or the value of the option.
   * <p>
   * 
   * Format is:
   * <p>
   * <code>optionName=optionValue</code>
   * 
   * @param options a list of options
   * @param toGet the option to get the value of
   * @return
   */
  protected static String getOption(List<String> options, String toGet) {
    String value = null;

    if (options == null) {
      return null;
    }

    for (String option : options) {
      if (option.startsWith(toGet)) {
        String[] parts = option.split("=");
        if (parts.length != 2) {
          return ""; // indicates a flag
        }
        value = parts[1];
        break;
      }
    }

    return value;
  }

  /**
   * Generates a heat map from a matrix of correlations
   * 
   * @param matrix a Matrix (expected to hold correlation values between -1 and
   *          1)
   * @param rowAttNames a list of labels for the columns/rows
   * @return an Image holding the heat map
   */
  public static Image getHeatMapForMatrix(Matrix matrix,
    List<String> rowAttNames) {

    double[][] m = matrix.getArray();

    // generate the heat map
    // need to reverse the order of the rows
    double[][] mm = new double[m.length][];
    for (int i = 0; i < m.length; i++) {
      mm[m.length - 1 - i] = m[i];
    }
    String[] xLabels = new String[rowAttNames.size()];
    String[] yLabels = new String[rowAttNames.size()];
    for (int i = 0; i < rowAttNames.size(); i++) {
      xLabels[i] = rowAttNames.get(i);
      yLabels[rowAttNames.size() - 1 - i] = rowAttNames.get(i);
    }
    HeatChart map = new HeatChart(mm, true);
    map.setTitle("Correlation matrix heat map");
    map.setCellSize(new java.awt.Dimension(30, 30));
    map.setHighValueColour(java.awt.Color.RED);
    map.setLowValueColour(java.awt.Color.BLUE);
    map.setXValues(xLabels);
    map.setYValues(yLabels);

    return map.getChartImage();
  }

  /**
   * Write a BufferedImage to a destination output stream as a png
   * 
   * @param image the image to write
   * @param dest the destination output stream
   * @throws IOException if a problem occurs
   */
  public static void writeImage(BufferedImage image, OutputStream dest)
    throws IOException {
    ImageIO.write(image, "png", dest);

    dest.flush();
    dest.close();
  }

  /**
   * Create a histogram chart from summary data (i.e. a list of bin labels and
   * corresponding frequencies)
   * 
   * @param bins a list of bin labels
   * @param freqs the corresponding frequencies of the bins
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a histogram chart
   * @throws Exception if a problem occurs
   */
  protected static JFreeChart getHistogramFromSummaryDataChart(
    List<String> bins, List<Double> freqs, List<String> additionalArgs)
    throws Exception {

    if (bins.size() != freqs.size()) {
      throw new Exception(
        "Number of bins should be equal to number of frequencies!");
    }

    String plotTitle = "Histogram";
    String userTitle = getOption(additionalArgs, "-title");
    plotTitle = (userTitle != null) ? userTitle : plotTitle;
    String xLabel = getOption(additionalArgs, "-x-label");
    xLabel = xLabel == null ? "" : xLabel;
    String yLabel = getOption(additionalArgs, "-y-label");
    yLabel = yLabel == null ? "" : yLabel;

    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    String seriesTitle = "";

    for (int i = 0; i < bins.size(); i++) {
      String binLabel = bins.get(i);
      Number freq = freqs.get(i);

      dataset.addValue(freq, seriesTitle, binLabel);
    }

    JFreeChart chart = null;

    chart =
      ChartFactory.createBarChart(plotTitle, xLabel, yLabel, dataset,
        PlotOrientation.VERTICAL, false, false, false);

    chart.setBackgroundPaint(java.awt.Color.white);
    chart.setTitle(new TextTitle(plotTitle,
      new Font("SansSerif", Font.BOLD, 12)));

    return chart;
  }

  /**
   * Render a histogram chart from summary data (i.e. a list of bin labels and
   * corresponding frequencies) to a buffered image
   * 
   * @param width the width of the resulting image
   * @param height the height of the resulting image
   * @param bins the list of bin labels
   * @param freqs the corresponding bin frequencies
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a histogram as a buffered image
   * @throws Exception if a problem occurs
   */
  public static BufferedImage renderHistogramFromSummaryData(int width,
    int height, List<String> bins, List<Double> freqs,
    List<String> additionalArgs) throws Exception {

    JFreeChart chart =
      getHistogramFromSummaryDataChart(bins, freqs, additionalArgs);
    CategoryAxis axis = chart.getCategoryPlot().getDomainAxis();
    axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    Font newFont = new Font("SansSerif", Font.PLAIN, 11);
    axis.setTickLabelFont(newFont);
    BufferedImage image = chart.createBufferedImage(width, height);
    return image;
  }

  /**
   * Create a pie chart from summary data (i.e. a list of values and their
   * corresponding frequencies).
   * 
   * @param values a list of values for the chart
   * @param freqs a list of corresponding frequencies
   * @param showLabels true if the chart will show labels
   * @param showLegend true if the chart will show a legend
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a pie chart
   * @throws Exception if a problem occurs
   */
  protected static JFreeChart getPieChartFromSummaryData(List<String> values,
    List<Double> freqs, boolean showLabels, boolean showLegend,
    List<String> additionalArgs) throws Exception {

    if (values.size() != freqs.size()) {
      throw new Exception(
        "Number of bins should be equal to number of frequencies!");
    }

    String plotTitle = "Pie Chart";
    String userTitle = getOption(additionalArgs, "-title");
    plotTitle = (userTitle != null) ? userTitle : plotTitle;
    String xLabel = getOption(additionalArgs, "-x-label");
    xLabel = xLabel == null ? "" : xLabel;
    String yLabel = getOption(additionalArgs, "-y-label");
    yLabel = yLabel == null ? "" : yLabel;

    DefaultPieDataset dataset = new DefaultPieDataset();

    for (int i = 0; i < values.size(); i++) {
      String binLabel = values.get(i);
      Number freq = freqs.get(i);

      dataset.setValue(binLabel, freq);
    }

    JFreeChart chart = ChartFactory.createPieChart(plotTitle, // chart title
      dataset, // data
      showLegend, // include legend
      false, false);

    PiePlot plot = (PiePlot) chart.getPlot();
    if (!showLabels) {
      plot.setLabelGenerator(null);
    } else {
      plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
      plot.setNoDataMessage("No data available");
      // plot.setCircular(false);
      plot.setLabelGap(0.02);
    }

    chart.setBackgroundPaint(java.awt.Color.white);
    chart.setTitle(new TextTitle(plotTitle,
      new Font("SansSerif", Font.BOLD, 12)));

    return chart;
  }

  /**
   * Create a box plot from summary data (mean, median, q1, q3, min, max,
   * minOutlier, maxOutlier, list of outliers)
   * 
   * @param summary summary data
   * @param outliers list of outlier values
   * @param additionalArgs additional options to the renderer
   * @return a box plot chart
   * @throws Exception if a problem occurs
   */
  protected static JFreeChart getBoxPlotFromSummaryData(List<Double> summary,
    List<Double> outliers, List<String> additionalArgs) throws Exception {

    if (summary.size() != 8) {
      throw new Exception(
        "Expected 8 values in the summary argument: mean, median, "
          + "q1, q3, min, max, minOutlier, maxOutlier");
    }

    String plotTitle = "Box Plog";
    String userTitle = getOption(additionalArgs, "-title");
    plotTitle = (userTitle != null) ? userTitle : plotTitle;
    String xLabel = getOption(additionalArgs, "-x-label");
    xLabel = xLabel == null ? "" : xLabel;
    String yLabel = getOption(additionalArgs, "-y-label");
    yLabel = yLabel == null ? "" : yLabel;

    DefaultBoxAndWhiskerCategoryDataset dataset =
      new DefaultBoxAndWhiskerCategoryDataset();

    Double mean = summary.get(0);
    Double median = summary.get(1);
    Double q1 = summary.get(2);
    Double q3 = summary.get(3);
    Double min = summary.get(4);
    Double max = summary.get(5);
    Double minOutlier = summary.get(6);
    Double maxOutlier = summary.get(7);

    if (mean.isNaN() || median.isNaN() || min.isNaN() || max.isNaN()
      || q1.isNaN() || q3.isNaN()) {
      throw new Exception("NaN in summary data - can't generate box plot");
    }

    BoxAndWhiskerItem item =
      new BoxAndWhiskerItem(mean, median, q1, q3, min, max, minOutlier,
        maxOutlier, outliers);

    dataset.add(item, "", "");

    CategoryAxis xAxis = new CategoryAxis();
    NumberAxis yAxis = new NumberAxis();
    yAxis.setAutoRangeIncludesZero(false);
    BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
    renderer.setFillBox(false);
    renderer.setMaximumBarWidth(0.15);

    CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

    JFreeChart chart =
      new JFreeChart(plotTitle, new Font("SansSerif", Font.BOLD, 12), plot,
        false);

    return chart;
  }

  /**
   * Create a box plot buffered image from summary data (mean, median, q1, q3,
   * min, max, minOutlier, maxOutlier, list of outliers)
   * 
   * @param width the width of the resulting image
   * @param height the height of the resulting image
   * @param summary summary data
   * @param outliers list of outlier values
   * @param additionalArgs additional options to the renderer
   * @return a box plot chart
   * @throws Exception if a problem occurs
   */
  public static BufferedImage renderBoxPlotFromSummaryData(int width,
    int height, List<Double> summary, List<Double> outliers,
    List<String> additionalArgs) throws Exception {

    JFreeChart chart =
      getBoxPlotFromSummaryData(summary, outliers, additionalArgs);
    BufferedImage image = chart.createBufferedImage(width, height);
    return image;
  }

  /**
   * Render a pie chart from summary data (i.e. a list of values and their
   * corresponding frequencies) to a buffered image
   * 
   * @param width the width of the resulting image
   * @param height the height of the resulting image
   * @param values a list of values for the chart
   * @param freqs a list of corresponding frequencies
   * @param showLabels true if the chart will show labels
   * @param showLegend true if the chart will show a legend
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a buffered image
   * @throws Exception if a problem occurs
   */
  public static BufferedImage renderPieChartFromSummaryData(int width,
    int height, List<String> values, List<Double> freqs, boolean showLabels,
    boolean showLegend, List<String> additionalArgs) throws Exception {

    JFreeChart chart =
      getPieChartFromSummaryData(values, freqs, showLabels, showLegend,
        additionalArgs);
    BufferedImage image = chart.createBufferedImage(width, height);

    return image;
  }

  /**
   * Render a combined histogram and pie chart from summary data
   * 
   * @param width the width of the resulting image
   * @param height the height of the resulting image
   * @param values the values for the chart
   * @param freqs the corresponding frequencies
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a buffered image
   * @throws Exception if a problem occurs
   */
  public static BufferedImage renderCombinedPieAndHistogramFromSummaryData(
    int width, int height, List<String> values, List<Double> freqs,
    List<String> additionalArgs) throws Exception {

    String plotTitle = "Combined Chart";
    String userTitle = getOption(additionalArgs, "-title");
    plotTitle = (userTitle != null) ? userTitle : plotTitle;

    List<String> opts = new ArrayList<String>();
    opts.add("-title=distribution");
    BufferedImage pie =
      renderPieChartFromSummaryData(width / 2, height, values, freqs, false,
        true, opts);

    opts.clear();
    opts.add("-title=histogram");
    BufferedImage hist =
      renderHistogramFromSummaryData(width / 2, height, values, freqs, opts);

    BufferedImage img =
      new BufferedImage(width, height + 20, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
    g2d.setColor(Color.lightGray);
    g2d.fillRect(0, 0, width, height + 20);
    g2d.setColor(Color.black);
    FontMetrics fm = g2d.getFontMetrics();
    int fh = fm.getHeight();
    int sw = fm.stringWidth(plotTitle);

    g2d.drawImage(pie, 0, 20, null);
    g2d.drawImage(hist, width / 2 + 1, 20, null);
    g2d.drawString(plotTitle, width / 2 - sw / 2, fh + 2);
    g2d.dispose();

    return img;
  }

  /**
   * Render a combined histogram and box plot chart from summary data
   * 
   * @param width the width of the resulting image
   * @param height the height of the resulting image
   * @param bins the values for the chart
   * @param freqs the corresponding frequencies
   * @param summary the summary stats for the box plot
   * @param outliers an optional list of outliers for the box plot
   * @param additionalArgs optional arguments to the renderer (may be null)
   * @return a buffered image
   * @throws Exception if a problem occurs
   */
  public static BufferedImage renderCombinedBoxPlotAndHistogramFromSummaryData(
    int width, int height, List<String> bins, List<Double> freqs,
    List<Double> summary, List<Double> outliers, List<String> additionalArgs)
    throws Exception {

    String plotTitle = "Combined Chart";
    String userTitle = getOption(additionalArgs, "-title");
    plotTitle = (userTitle != null) ? userTitle : plotTitle;

    List<String> opts = new ArrayList<String>();
    opts.add("-title=histogram");
    BufferedImage hist =
      renderHistogramFromSummaryData(width / 2, height, bins, freqs, opts);
    opts.clear();
    opts.add("-title=box plot");
    BufferedImage box = null;
    try {
      box =
        renderBoxPlotFromSummaryData(width / 2, height, summary, outliers, opts);
    } catch (Exception ex) {
      // if we can't generate the box plot (i.e. probably because
      // data is 100% missing) then just return the histogram
    }

    if (box == null) {
      width /= 2;
    }
    BufferedImage img =
      new BufferedImage(width, height + 20, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
    g2d.setColor(Color.lightGray);
    g2d.fillRect(0, 0, width, height + 20);
    g2d.setColor(Color.black);
    FontMetrics fm = g2d.getFontMetrics();
    int fh = fm.getHeight();
    int sw = fm.stringWidth(plotTitle);

    if (box != null) {
      g2d.drawImage(box, 0, 20, null);
      g2d.drawImage(hist, width / 2 + 1, 20, null);
    } else {
      g2d.drawImage(hist, 0, 20, null);
    }
    g2d.drawString(plotTitle, width / 2 - sw / 2, fh + 2);
    g2d.dispose();

    return img;
  }

  public static BufferedImage createAttributeChartNumeric(
    NumericAttributeBinData binStats, Attribute summaryAtt,
    OutputStream dos, int width, int height) throws IOException {

    BufferedImage chart = null;
    double missingFreq = binStats.getMissingFreq();
    List<String> binLabs = binStats.getBinLabels();
    List<Double> freqs = binStats.getBinFreqs();

    if (missingFreq > 0) {
      binLabs.add("*missing*");
      freqs.add(missingFreq);
    }

    double mean = ArffSummaryNumericMetric.MEAN.valueFromAttribute(summaryAtt);
    double median =
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(summaryAtt);
    double q1 =
      ArffSummaryNumericMetric.FIRSTQUARTILE.valueFromAttribute(summaryAtt);
    double q3 =
      ArffSummaryNumericMetric.THIRDQUARTILE.valueFromAttribute(summaryAtt);
    double min = ArffSummaryNumericMetric.MIN.valueFromAttribute(summaryAtt);
    double max = ArffSummaryNumericMetric.MAX.valueFromAttribute(summaryAtt);
    double minOutlier = min;
    double maxOutlier = max;
    List<Double> summary = new ArrayList<Double>();
    summary.add(mean);
    summary.add(median);
    summary.add(q1);
    summary.add(q3);
    summary.add(min);
    summary.add(max);
    summary.add(minOutlier);
    summary.add(maxOutlier);

    List<String> opts = new ArrayList<String>();
    opts.add("-title=" + binStats.getAttributeName());

    try {
      chart =
        ChartUtils.renderCombinedBoxPlotAndHistogramFromSummaryData(width,
          height, binLabs, freqs, summary, null, opts);

      if (dos != null) {
        try {
          ChartUtils.writeImage(chart, dos);
        } finally {
          if (dos != null) {
            dos.close();
          }
        }
      }
    } catch (Exception e) {
      throw new IOException(e);
    }

    return chart;
  }

  /**
   * Creates and writes a combined chart for a nominal attribute given its
   * summary attribute information and an output stream to write to
   * 
   * @param summaryAtt the summary attribute for the attribute to process
   * @param attName the name of the attribute
   * @param dos the output stream to write to (can be null for no write)
   * @param width the width of the chart
   * @param height the height of the chart
   * @return a BufferedImage containing the chart
   * @throws IOException if a problem occurs
   */
  public static BufferedImage createAttributeChartNominal(Attribute summaryAtt,
    String attName, OutputStream dos, int width, int height) throws IOException {

    NominalStats stats = NominalStats.attributeToStats(summaryAtt);
    BufferedImage chart = null;

    Set<String> labels = stats.getLabels();
    String[] labs = new String[labels.size() + 1];
    double[] freqs = new double[labs.length];

    int count = 0;
    for (String l : labels) {
      labs[count] = l;
      freqs[count++] = stats.getCount(l);
    }
    freqs[count] = stats.getNumMissing();
    labs[count] = "*missing*";

    int[] sortedIndices = Utils.sort(freqs);

    // we want the top 7 values plus missing, and
    // any other values collapsed into an "other"
    // category
    String[] finalLabs;
    double[] finalFreqs;
    if (labs.length <= 8) {
      finalLabs = new String[labs.length];
      finalFreqs = new double[labs.length];

      count = 0;
      for (int i = sortedIndices.length - 1; i >= 0; i--) {
        finalLabs[count] = labs[sortedIndices[i]];
        finalFreqs[count++] = freqs[sortedIndices[i]];
      }
    } else {
      finalLabs = new String[9];
      finalFreqs = new double[9];

      finalLabs[8] = "*missing*";
      finalFreqs[8] = stats.getNumMissing();

      count = 0;
      for (int i = sortedIndices.length - 1; i >= 0; i--) {
        if (labs[sortedIndices[i]].equals("*missing*")) {
          continue;
        }

        if (count == 7) {
          if (labs.length > 9) {
            finalLabs[count] = "*other*";
          } else {
            finalLabs[count] = labs[sortedIndices[i]];
          }
          finalFreqs[count] += freqs[sortedIndices[i]];
          continue;
        }

        finalLabs[count] = labs[sortedIndices[i]];
        finalFreqs[count++] = freqs[sortedIndices[i]];
      }
    }

    List<String> values = Arrays.asList(finalLabs);
    List<Double> freqL = new ArrayList<Double>();
    for (double d : finalFreqs) {
      freqL.add(d);
    }
    List<String> opts = new ArrayList<String>();
    opts.add("-title=" + attName);

    try {
      chart =
        ChartUtils.renderCombinedPieAndHistogramFromSummaryData(width, height,
          values, freqL, opts);

      if (dos != null) {
        try {
          ChartUtils.writeImage(chart, dos);
        } finally {
          if (dos != null) {
            dos.close();
          }
        }
      }
    } catch (Exception e) {
      throw new IOException(e);
    }

    return chart;
  }

  public static void main(String[] args) {
    try {
      String outFile = args[0];
      List<String> chartArgs = new java.util.ArrayList<String>();
      chartArgs.add("-title=My plot");

      List<String> bins = new java.util.ArrayList<String>();
      bins.add("50");
      bins.add("100");
      bins.add("300");
      List<Double> freqs = new java.util.ArrayList<Double>();
      freqs.add(10.0);
      freqs.add(50.0);
      freqs.add(14.0);

      List<Double> boxSummary = new java.util.ArrayList<Double>();
      boxSummary.add(44.0);
      boxSummary.add(45.0);
      boxSummary.add(25.0);
      boxSummary.add(75.0);
      boxSummary.add(-10.0);
      boxSummary.add(90.0);
      boxSummary.add(10.0);
      boxSummary.add(90.0);

      // BufferedImage bi =
      // r.renderCombinedBoxPlotAndHistogramFromSummaryData(600, 400, bins,
      // freqs, boxSummary, null, chartArgs);

      BufferedImage bi =
        ChartUtils.renderCombinedPieAndHistogramFromSummaryData(600, 400, bins,
          freqs, chartArgs);

      javax.imageio.ImageIO.write(bi, "png", new java.io.File(outFile));

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
