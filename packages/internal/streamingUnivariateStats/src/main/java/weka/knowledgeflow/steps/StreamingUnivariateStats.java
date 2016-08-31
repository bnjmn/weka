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
 *    StreamingUnivariateStats
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.ChartUtils;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericAttributeBinData;
import weka.core.stats.NumericStats;
import weka.core.stats.Stats;
import weka.core.stats.StatsFormatter;
import weka.core.stats.StringStats;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Knowledge Flow step for computing summary statistics from an incoming
 * instance stream.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(
  name = "StreamingUnivariateStats",
  category = "Stats",
  toolTipText = "Compute various univariate statsitics on an incoming instance "
    + "stream", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "AttributeSummarizer.gif")
public class StreamingUnivariateStats extends BaseStep {
  private static final long serialVersionUID = -961940668737107184L;

  /** Holds the statistics estimators */
  protected Stats[] m_attributeStats;

  /** True to compute quantiles using TDigest streaming estimators */
  protected boolean m_computeQuantiles;

  /**
   * Compression level to use with the TDigest estimator - higher values are
   * less compression, more accurate, slower
   */
  protected double m_compressionLevel = 100.0;

  /** True if the step has been reset */
  protected boolean m_isReset;

  /** Header of the incoming data stream */
  protected Instances m_header;

  /**
   * How often to output the summary stats - 0 means only at the end of the
   * input stream
   */
  protected int m_outputEveryXRows;

  /** Count of the rows seen */
  protected int m_rowCount;

  /** Number of decimal places to output */
  protected int m_decimalPlaces = 2;

  protected String m_chartWidth = "500";
  protected String m_chartHeight = "400";

  @OptionMetadata(
    displayName = "Compute quartiles",
    description = "Compute median and quartiles (note quartile estimator is substantially slower "
      + "than the other stats)", displayOrder = 1)
  public
    void setComputeQuartiles(boolean computeQuartiles) {
    m_computeQuantiles = computeQuartiles;
  }

  public boolean getComputeQuartiles() {
    return m_computeQuantiles;
  }

  @OptionMetadata(displayName = "Quartile estimator compression",
    description = "The degree of compression for quartile estimation ("
      + "bigger = less compression/more accurate/slower", displayOrder = 2)
  public void setQuartileCompression(double compression) {
    m_compressionLevel = compression;
  }

  public double getQuartileCompression() {
    return m_compressionLevel;
  }

  @OptionMetadata(displayName = "Output stats every x rows",
    description = "How often (after every x input rows) to output the current "
      + "value of the stats (0 = only at the end of the stream)",
    displayOrder = 3)
  public void setOutputFrequency(int outputFrequency) {
    m_outputEveryXRows = outputFrequency;
  }

  public int getOutputFrequency() {
    return m_outputEveryXRows;
  }

  @OptionMetadata(displayName = "Number of decimal places",
    description = "Number of decimal places", displayOrder = 4)
  public void setDecimalPlaces(int decimalPlaces) {
    m_decimalPlaces = decimalPlaces;
  }

  public int getDecimalPlaces() {
    return m_decimalPlaces;
  }

  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_rowCount = 0;
  }

  @Override
  public void processIncoming(Data data) throws WekaException {

    Instance inst = (Instance) data.getPrimaryPayload();
    if (m_isReset) {
      m_isReset = false;
      m_header = inst.dataset();
      initStats();
    }

    if (getStepManager().isStreamFinished(data)) {
      outputStats();
      outputGraphs();
      getStepManager().throughputFinished(data);
    } else {
      if (!isStopRequested()) {
        getStepManager().throughputUpdateStart();
        updateStats(inst);
        m_rowCount++;
        if (m_outputEveryXRows > 0 && m_rowCount % m_outputEveryXRows == 0) {
          outputStats();
        }
        getStepManager().throughputUpdateEnd();
      } else {
        getStepManager().interrupted();
      }
    }
  }

  protected void outputStats() throws WekaException {
    String result =
      StatsFormatter.formatStats(m_header, m_attributeStats,
        m_computeQuantiles, m_decimalPlaces);
    Data textD = new Data(StepManager.CON_TEXT, result);
    textD.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
      "Summary stats (" + m_header.relationName() + ")");
    getStepManager().outputData(textD);
  }

  protected void outputGraphs() throws WekaException {
    if (!m_computeQuantiles
      && m_header.checkForAttributeType(Attribute.NUMERIC)) {
      return;
    }

    String width = environmentSubstitute(m_chartWidth);
    String height = environmentSubstitute(m_chartHeight);
    try {
      int chartWidth = Integer.parseInt(width);
      int chartHeight = Integer.parseInt(height);
      for (int i = 0; i < m_header.numAttributes(); i++) {
        BufferedImage img = null;
        if (m_header.attribute(i).isNumeric()
          && !m_header.attribute(i).isDate()) {
          Attribute summary =
            ((NumericStats) m_attributeStats[i]).makeAttribute();
          NumericAttributeBinData binData =
            new NumericAttributeBinData(m_header.attribute(i).name(), summary,
              -1);

          img =
            ChartUtils.createAttributeChartNumeric(binData, summary, null,
              chartWidth, chartHeight);
        } else if (m_header.attribute(i).isNominal()) {
          Attribute summary =
            ((NominalStats) m_attributeStats[i]).makeAttribute();
          img =
            ChartUtils.createAttributeChartNominal(summary,
              m_header.attribute(i).name(), null, chartWidth, chartHeight);
        }

        if (img != null) {
          Data imgData = new Data(StepManager.CON_IMAGE, img);
          imgData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
            m_header.attribute(i).name());
          getStepManager().outputData(imgData);
        }
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  protected void updateStats(Instance inst) {
    double weight = inst.weight();
    for (int i = 0; i < m_header.numAttributes(); i++) {
      if (m_header.attribute(i).isNumeric()) {
        ((NumericStats) m_attributeStats[i]).update(inst.value(i), weight,
          false, m_computeQuantiles);
      } else if (m_header.attribute(i).isNominal()) {
        ((NominalStats) m_attributeStats[i]).add(inst.isMissing(i) ? null
          : inst.stringValue(i), weight);
      } else if (m_header.attribute(i).isString()) {
        ((StringStats) m_attributeStats[i]).update(inst.isMissing(i) ? null
          : inst.stringValue(i), weight);
      }
    }
  }

  protected void initStats() {
    m_attributeStats = new Stats[m_header.numAttributes()];
    for (int i = 0; i < m_header.numAttributes(); i++) {
      Attribute a = m_header.attribute(i);

      if (a.isNumeric()) {
        m_attributeStats[i] = new NumericStats(a.name(), m_compressionLevel);
      } else if (a.isNominal()) {
        m_attributeStats[i] = new NominalStats(a.name());
      } else if (a.isString()) {
        m_attributeStats[i] = new StringStats(a.name());
      }
    }
  }

  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE);
    }

    return null;
  }

  @Override
  public List<String> getOutgoingConnectionTypes() {
    return Arrays.asList(StepManager.CON_TEXT, StepManager.CON_IMAGE);
  }
}
