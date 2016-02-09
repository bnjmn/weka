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
 * RELAGGS.java
 * Copyright (C) 2007-2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.SimpleBatchFilter;

/**
 * <!-- globalinfo-start -->
 * A propositionalization filter inspired by the RELAGGS algorithm.<br>
 * It processes all relational attributes that fall into the user defined range. Currently, the filter only processes one level of nesting.<br>
 * The class attribute is not touched.<br>
 * <br>
 * For more information see:<br>
 * <br>
 * M.-A. Krogel, S. Wrobel: Facets of Aggregation Approaches to Propositionalization. In: Work-in-Progress Track at the Thirteenth International Conference on Inductive Logic Programming (ILP), 2003.
 * <br><br>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Krogel2003,
 *    author = {M.-A. Krogel and S. Wrobel},
 *    booktitle = {Work-in-Progress Track at the Thirteenth International Conference on Inductive Logic Programming (ILP)},
 *    editor = {T. Horvath and A. Yamamoto},
 *    title = {Facets of Aggregation Approaches to Propositionalization},
 *    year = {2003},
 *    PDF = {http://kd.cs.uni-magdeburg.de/\~krogel/papers/aggs.pdf}
 * }
 * </pre>
 * <br><br>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <br><br>
 * 
 * <pre> -R &lt;index1,index2-index4,...&gt;
 *  Specify list of string attributes to convert to words.
 *  (default: select all relational attributes)</pre>
 * 
 * <pre> -V
 *  Inverts the matching sense of the selection.</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  Max. cardinality of nominal attributes. If a nominal attribute
 *  has more values than this upper limit, then it will be skipped.
 *  (default: 20)</pre>
 * 
 * <pre> -S
 *  Generate sparse instances.</pre>
 * 
 * <pre> -disable-min
 *  Disable out of MIN statistic.</pre>
 * 
 * <pre> -disable-max
 *  Disable out of MAX statistic.</pre>
 * 
 * <pre> -disable-avg
 *  Disable out of AVG statistic.</pre>
 * 
 * <pre> -disable-stdev
 *  Disable out of STDEV statistic.</pre>
 * 
 * <pre> -disable-sum
 *  Disable out of SUM statistic.</pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class RELAGGS extends SimpleBatchFilter implements
  TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -3333791375278589231L;

  /** the max. cardinality for nominal attributes */
  protected int m_MaxCardinality = 20;

  /**
   * the range of attributes to process (only relational ones will be processed)
   */
  protected Range m_SelectedRange = new Range("first-last");

  /**
   * stores the attribute statistics
   * <code>att_index-att_index_in_rel_att &lt;-&gt; AttributeStats</code>
   */
  protected Hashtable<String, AttributeStats> m_AttStats = null;

  /** whether to generate sparse instances */
  protected boolean m_GenerateSparseInstances = false;

  /** Boolean flags used to disable output of particular summary statistics */
  protected boolean m_DisableMIN = false;
  protected boolean m_DisableMAX = false;
  protected boolean m_DisableAVG = false;
  protected boolean m_DisableSTDEV = false;
  protected boolean m_DisableSUM = false;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "A propositionalization filter inspired by the RELAGGS algorithm.\n"
      + "It processes all relational attributes that fall into the user defined "
      + "range. Currently, the filter only processes one level of nesting.\n"
      + "The class attribute is not touched.\n" + "\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "M.-A. Krogel and S. Wrobel");
    result.setValue(Field.TITLE,
      "Facets of Aggregation Approaches to Propositionalization");
    result
      .setValue(
        Field.BOOKTITLE,
        "Work-in-Progress Track at the Thirteenth International Conference on Inductive Logic Programming (ILP)");
    result.setValue(Field.EDITOR, "T. Horvath and A. Yamamoto");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.PDF,
      "http://kd.cs.uni-magdeburg.de/~krogel/papers/aggs.pdf");

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>(3);

    result.addElement(new Option(
      "\tSpecify list of string attributes to convert to words.\n"
        + "\t(default: select all relational attributes)", "R", 1,
      "-R <index1,index2-index4,...>"));

    result.addElement(new Option(
      "\tInverts the matching sense of the selection.", "V", 0, "-V"));

    result.addElement(new Option(
      "\tMax. cardinality of nominal attributes. If a nominal attribute\n"
        + "\thas more values than this upper limit, then it will be skipped.\n"
        + "\t(default: 20)", "C", 1, "-C <num>"));

    result.addElement(new Option(
            "\tGenerate sparse instances.", "S", 0, "-S"));

    result.addElement(new Option(
            "\tDisable out of MIN statistic.", "disable-min", 0, "-disable-min"));

    result.addElement(new Option(
            "\tDisable out of MAX statistic.", "disable-max", 0, "-disable-max"));

    result.addElement(new Option(
            "\tDisable out of AVG statistic.", "disable-avg", 0, "-disable-avg"));

    result.addElement(new Option(
            "\tDisable out of STDEV statistic.", "disable-stdev", 0, "-disable-stdev"));

    result.addElement(new Option(
            "\tDisable out of SUM statistic.", "disable-sum", 0, "-disable-sum"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * <p>Parses the options for this object.
   * </p>
   * 
   * <!-- options-start -->
   * Valid options are: <br><br>
   * 
   * <pre> -R &lt;index1,index2-index4,...&gt;
   *  Specify list of string attributes to convert to words.
   *  (default: select all relational attributes)</pre>
   * 
   * <pre> -V
   *  Inverts the matching sense of the selection.</pre>
   * 
   * <pre> -C &lt;num&gt;
   *  Max. cardinality of nominal attributes. If a nominal attribute
   *  has more values than this upper limit, then it will be skipped.
   *  (default: 20)</pre>
   * 
   * <pre> -S
   *  Generate sparse instances.</pre>
   * 
   * <pre> -disable-min
   *  Disable out of MIN statistic.</pre>
   * 
   * <pre> -disable-max
   *  Disable out of MAX statistic.</pre>
   * 
   * <pre> -disable-avg
   *  Disable out of AVG statistic.</pre>
   * 
   * <pre> -disable-stdev
   *  Disable out of STDEV statistic.</pre>
   * 
   * <pre> -disable-sum
   *  Disable out of SUM statistic.</pre>
   * 
   * <pre> -output-debug-info
   *  If set, filter is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, filter capabilities are not checked before filter is built
   *  (use with caution).</pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options to use
   * @throws Exception if setting of options fails
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setSelectedRange(tmpStr);
    } else {
      setSelectedRange("first-last");
    }

    setInvertSelection(Utils.getFlag('V', options));

    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length() != 0) {
      setMaxCardinality(Integer.parseInt(tmpStr));
    } else {
      setMaxCardinality(20);
    }

    setGenerateSparseInstances(Utils.getFlag('S', options));

    setDisableMIN(Utils.getFlag("disable-min", options));

    setDisableMAX(Utils.getFlag("disable-max", options));

    setDisableAVG(Utils.getFlag("disable-avg", options));

    setDisableSTDEV(Utils.getFlag("disable-stdev", options));

    setDisableSUM(Utils.getFlag("disable-sum", options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-R");
    result.add(getSelectedRange().getRanges());

    if (getInvertSelection()) {
      result.add("-V");
    }

    result.add("-C");
    result.add("" + getMaxCardinality());

    if (getGenerateSparseInstances()) {
      result.add("-S");
    }

    if (getDisableMIN()) {
      result.add("-disable-min");
    }

    if (getDisableMAX()) {
      result.add("-disable-max");
    }

    if (getDisableAVG()) {
      result.add("-disable-avg");
    }

    if (getDisableSTDEV()) {
      result.add("-disable-stdev");
    }

    if (getDisableSUM()) {
      result.add("-disable-sum");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String disableMINTipText() {
    return "Do not include MAX attributes in output.";
  }

  /**
   * Get whether output of statistic is disabled
   * @return true if output is disabled
   */
  public boolean getDisableMIN() {
    return m_DisableMIN;
  }

  /**
   * Set whether output of statistic is disabled
   * @param m_DisableMIN true if output is disabled
   */
  public void setDisableMIN(boolean m_DisableMIN) {
    this.m_DisableMIN = m_DisableMIN;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String disableMAXTipText() {
    return "Do not include MAX attributes in output.";
  }

  /**
   * Get whether output of statistic is disabled
   * @return true if output is disabled
   */
  public boolean getDisableMAX() {
    return m_DisableMAX;
  }

  /**
   * Set whether output of statistic is disabled
   * @param m_DisableMAX true if output is disabled
   */
  public void setDisableMAX(boolean m_DisableMAX) {
    this.m_DisableMAX = m_DisableMAX;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String disableAVGTipText() {
    return "Do not include AVG attributes in output.";
  }

  /**
   * Get whether output of statistic is disabled
   * @return true if output is disabled
   */
  public boolean getDisableAVG() {
    return m_DisableAVG;
  }

  /**
   * Set whether output of statistic is disabled
   * @param m_DisableAVG true if output is disabled
   */
  public void setDisableAVG(boolean m_DisableAVG) {
    this.m_DisableAVG = m_DisableAVG;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String disableSTDEVTipText() {
    return "Do not include STDEV attributes in output.";
  }

  /**
   * Get whether output of statistic is disabled
   * @return true if output is disabled
   */
  public boolean getDisableSTDEV() {
    return m_DisableSTDEV;
  }

  /**
   * Set whether output of statistic is disabled
   * @param m_DisableSTDEV true if output is disabled
   */
  public void setDisableSTDEV(boolean m_DisableSTDEV) {
    this.m_DisableSTDEV = m_DisableSTDEV;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String disableSUMTipText() {
    return "Do not include SUM attributes in output.";
  }

  /**
   * Get whether output of statistic is disabled
   * @return true if output is disabled
   */
  public boolean getDisableSUM() {
    return m_DisableSUM;
  }


  /**
   * Set whether output of statistic is disabled
   * @param m_DisableSUM true if output is disabled
   */
  public void setDisableSUM(boolean m_DisableSUM) {
    this.m_DisableSUM = m_DisableSUM;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxCardinalityTipText() {
    return "The maximum number of values a nominal attribute can have before it's skipped.";
  }

  /**
   * Sets the maximum number of values allowed for nominal attributes, before
   * they're skipped.
   * 
   * @param value the maximum value.
   */
  public void setMaxCardinality(int value) {
    m_MaxCardinality = value;
  }

  /**
   * Gets the maximum number of values allowed for nominal attributes, before
   * they're skipped.
   * 
   * @return the maximum number.
   */
  public int getMaxCardinality() {
    return m_MaxCardinality;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on; "
      + "this is a comma separated list of attribute indices, with "
      + "\"first\" and \"last\" valid values; Specify an inclusive "
      + "range with \"-\"; eg: \"first-3,5,6-10,last\".";
  }

  /**
   * Set the range of attributes to process.
   * 
   * @param value the new range.
   */
  public void setSelectedRange(String value) {
    m_SelectedRange = new Range(value);
  }

  /**
   * Gets the current range selection.
   * 
   * @return current selection.
   */
  public Range getSelectedRange() {
    return m_SelectedRange;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected "
      + "attributes in the range will be worked on; if "
      + "true, only non-selected attributes will be processed.";
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   * 
   * @param value the new invert setting
   */
  public void setInvertSelection(boolean value) {
    m_SelectedRange.setInvert(value);
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped
   * 
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_SelectedRange.getInvert();
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String generateSparseInstancesTipText() {
    return "Generate sparse instances rather than dense ones.";
  }

  /**
   * Sets whether to generate sparse instances.
   *
   * @param value the new setting
   */
  public void setGenerateSparseInstances(boolean value) {
    m_GenerateSparseInstances = value;
  }

  /**
   * Gets whether to generate sparse instances.
   *
   * @return true if sparse instances are to be generated
   */
  public boolean getGenerateSparseInstances() {
    return m_GenerateSparseInstances;
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Determines the output format based on the input format and returns this. In
   * case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called from
   * batchFinished().
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   * @see #hasImmediateOutputFormat()
   * @see #batchFinished()
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    Instances result;
    Instances relFormat;
    ArrayList<Attribute> atts;
    int i;
    int n;
    int m;
    int clsIndex;
    Attribute att;
    String prefix;

    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);

    atts = new ArrayList<Attribute>();
    clsIndex = -1;
    for (i = 0; i < inputFormat.numAttributes(); i++) {
      // we don't process the class
      if (i == inputFormat.classIndex()) {
        clsIndex = atts.size();
        atts.add((Attribute) inputFormat.attribute(i).copy());
        continue;
      }

      if (!inputFormat.attribute(i).isRelationValued()) {
        atts.add((Attribute) inputFormat.attribute(i).copy());
        continue;
      }

      if (!m_SelectedRange.isInRange(i)) {
        Attribute relAtt = inputFormat.attribute(i);
        atts.add(new Attribute(relAtt.name(), new Instances(relAtt.relation(), 0), atts.size()));
        continue;
      }

      // process relational attribute
      prefix = inputFormat.attribute(i).name() + "_";
      relFormat = inputFormat.attribute(i).relation();
      for (n = 0; n < relFormat.numAttributes(); n++) {
        att = relFormat.attribute(n);

        if (att.isNumeric()) {
          if (!getDisableMIN()) {
            atts.add(new Attribute(prefix + att.name() + "_MIN"));
          }
          if (!getDisableMAX()) {
            atts.add(new Attribute(prefix + att.name() + "_MAX"));
          }
          if (!getDisableAVG()) {
            atts.add(new Attribute(prefix + att.name() + "_AVG"));
          }
          if (!getDisableSTDEV()) {
            atts.add(new Attribute(prefix + att.name() + "_STDEV"));
          }
          if (!getDisableSUM()) {
            atts.add(new Attribute(prefix + att.name() + "_SUM"));
          }
        } else if (att.isNominal()) {
          if (att.numValues() <= m_MaxCardinality) {
            for (m = 0; m < att.numValues(); m++) {
              atts.add(new Attribute(prefix + att.name() + "_" + att.value(m)
                + "_CNT"));
            }
          } else {
            if (getDebug()) {
              System.out.println("Attribute " + (i + 1) + "/" + (n + 1) + " ("
                + inputFormat.attribute(i).name() + "/" + att.name()
                + ") skipped, " + att.numValues() + " > " + m_MaxCardinality
                + ".");
            }
          }
        } else {
          if (getDebug()) {
            System.out.println("Attribute " + (i + 1) + "/" + (n + 1) + " ("
              + inputFormat.attribute(i).name() + "/" + att.name()
              + ") skipped.");
          }
        }
      }
    }

    // generate new format
    result = new Instances(inputFormat.relationName(), atts, 0);
    result.setClassIndex(clsIndex);

    return result;
  }

  /**
   * Processes the given data (may change the provided dataset) and returns the
   * modified version. This method is called in batchFinished().
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   * @see #batchFinished()
   */
  @Override
  protected Instances process(Instances instances) throws Exception {
    Instances result;
    Instance inst;
    Instance newInst;
    Instances relInstances;
    int k;
    int l;
    int i;
    int n;
    int m;
    AttributeStats stats;
    Attribute att;

    result = getOutputFormat();

    // initialize attribute statistics
    m_AttStats = new Hashtable<String, AttributeStats>();

    // collect data for all relational attributes
    for (i = 0; i < instances.numAttributes(); i++) {
      if (i == instances.classIndex()) {
        continue;
      }

      if (!instances.attribute(i).isRelationValued()) {
        continue;
      }

      if (!m_SelectedRange.isInRange(i)) {
        continue;
      }

      // compute statistics
      for (k = 0; k < instances.numInstances(); k++) {
        relInstances = instances.instance(k).relationalValue(i);

        for (n = 0; n < relInstances.numAttributes(); n++) {
          att = relInstances.attribute(n);
          if (att.isNumeric()
            || (att.isNominal() && att.numValues() <= m_MaxCardinality)) {
            stats = relInstances.attributeStats(n);
            m_AttStats.put(k + "-" + i + "-" + n, stats);
          }
        }
      }
    }

    // convert data
    for (k = 0; k < instances.numInstances(); k++) {
      inst = instances.instance(k);

      double[] values = new double[result.numAttributes()];
      l = 0;
      for (i = 0; i < instances.numAttributes(); i++) {
        if (!instances.attribute(i).isRelationValued() || !m_SelectedRange.isInRange(i)) {
          values[l++] = inst.value(i);
        } else {

          // replace relational data with statistics
          relInstances = inst.relationalValue(i);
          for (n = 0; n < relInstances.numAttributes(); n++) {
            att = relInstances.attribute(n);
            stats = m_AttStats.get(k + "-" + i + "-" + n);

            if (att.isNumeric()) {
              if (!getDisableMIN()) {
                values[l++] = stats.numericStats.min;
              }
              if (!getDisableMAX()) {
                values[l++] = stats.numericStats.max;
              }
              if (!getDisableAVG()) {
                values[l++] = stats.numericStats.mean;
              }
              if (!getDisableSTDEV()) {
                values[l++] = stats.numericStats.stdDev;
              }
              if (!getDisableSUM()) {
                values[l++] = stats.numericStats.sum;
              }
          } else if (att.isNominal() && att.numValues() <= m_MaxCardinality) {
              for (m = 0; m < att.numValues(); m++) {
                values[l++] =  stats.nominalCounts[m];
              }
            }
          }
        }
      }

      if (m_GenerateSparseInstances) {
        result.add(new SparseInstance(inst.weight(), values));
      } else {
        result.add(new DenseInstance(inst.weight(), values));
      }
    }

    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new RELAGGS(), args);
  }
}

