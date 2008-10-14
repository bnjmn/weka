/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * RELAGGS.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.SimpleBatchFilter;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A propositionalization filter inspired by the RELAGGS algorithm.<br/>
 * It processes all relational attributes that fall into the user defined range (all others are skipped, i.e., not added to the output). Currently, the filter only processes one level of nesting.<br/>
 * The class attribute is not touched.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * M.-A. Krogel, S. Wrobel: Facets of Aggregation Approaches to Propositionalization. In: Work-in-Progress Track at the Thirteenth International Conference on Inductive Logic Programming (ILP), 2003.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
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
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
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
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.3 $
 */
public class RELAGGS
  extends SimpleBatchFilter
  implements TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -3333791375278589231L;
  
  /** the max. cardinality for nominal attributes */
  protected int m_MaxCardinality = 20;

  /** the range of attributes to process (only relational ones will be processed) */
  protected Range m_SelectedRange = new Range("first-last");
  
  /** stores the attribute statistics 
   * <code>att_index-att_index_in_rel_att &lt;-&gt; AttributeStats</code> */
  protected Hashtable<String,AttributeStats> m_AttStats = new Hashtable<String,AttributeStats>();
  
  /**
   * Returns a string describing this filter
   *
   * @return 		a description of the filter suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "A propositionalization filter inspired by the RELAGGS algorithm.\n"
      + "It processes all relational attributes that fall into the user defined "
      + "range (all others are skipped, i.e., not added to the output). "
      + "Currently, the filter only processes one level of nesting.\n"
      + "The class attribute is not touched.\n"
      + "\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return 		the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "M.-A. Krogel and S. Wrobel");
    result.setValue(Field.TITLE, "Facets of Aggregation Approaches to Propositionalization");
    result.setValue(Field.BOOKTITLE, "Work-in-Progress Track at the Thirteenth International Conference on Inductive Logic Programming (ILP)");
    result.setValue(Field.EDITOR, "T. Horvath and A. Yamamoto");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.PDF, "http://kd.cs.uni-magdeburg.de/~krogel/papers/aggs.pdf");
    
    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector        	result;
    Enumeration   	en;

    result = new Vector();

    en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    result.addElement(new Option(
	"\tSpecify list of string attributes to convert to words.\n"
	+ "\t(default: select all relational attributes)",
	"R", 1, "-R <index1,index2-index4,...>"));

    result.addElement(new Option(
	"\tInverts the matching sense of the selection.",
	"V", 0, "-V"));

    result.addElement(new Option(
	"\tMax. cardinality of nominal attributes. If a nominal attribute\n"
	+ "\thas more values than this upper limit, then it will be skipped.\n"
	+ "\t(default: 20)",
	"C", 1, "-C <num>"));

    return result.elements();
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turns on output of debugging information.</pre>
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
   <!-- options-end -->
   *
   * @param options	the options to use
   * @throws Exception	if setting of options fails
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0)
      setSelectedRange(tmpStr);
    else
      setSelectedRange("first-last");

    setInvertSelection(Utils.getFlag('V', options));
    
    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length() != 0)
      setMaxCardinality(Integer.parseInt(tmpStr));
    else
      setMaxCardinality(20);

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return 		an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    int			i;
    Vector<String>	result;
    String[]		options;

    result = new Vector<String>();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    result.add("-R"); 
    result.add(getSelectedRange().getRanges());

    if (getInvertSelection())
      result.add("-V");
    
    result.add("-C");
    result.add("" + getMaxCardinality());
    
    return result.toArray(new String[result.size()]);	  
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String maxCardinalityTipText() {
    return "The maximum number of values a nominal attribute can have before it's skipped.";
  }

  /**
   * Sets the maximum number of values allowed for nominal attributes, before
   * they're skipped.
   *
   * @param value 	the maximum value.
   */
  public void setMaxCardinality(int value) {
    m_MaxCardinality = value;
  }
  
  /**
   * Gets the maximum number of values allowed for nominal attributes, before
   * they're skipped.
   *
   * @return 		the maximum number.
   */
  public int getMaxCardinality() {
    return m_MaxCardinality;
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return 
        "Specify range of attributes to act on; "
      + "this is a comma separated list of attribute indices, with "
      + "\"first\" and \"last\" valid values; Specify an inclusive "
      + "range with \"-\"; eg: \"first-3,5,6-10,last\".";
  }
  
  /**
   * Set the range of attributes to process.
   *
   * @param value 	the new range.
   */
  public void setSelectedRange(String value) {
    m_SelectedRange = new Range(value);
  }

  /**
   * Gets the current range selection.
   *
   * @return 		current selection.
   */
  public Range getSelectedRange() {
    return m_SelectedRange;
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return 
        "Set attribute selection mode. If false, only selected "
      + "attributes in the range will be worked on; if "
      + "true, only non-selected attributes will be processed.";
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   *
   * @param value 	the new invert setting
   */
  public void setInvertSelection(boolean value) {
    m_SelectedRange.setInvert(value);
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped
   *
   * @return 		true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_SelectedRange.getInvert();
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
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
   * Determines the output format based on the input format and returns 
   * this. In case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called
   * from batchFinished().
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   * @throws Exception      in case the determination goes wrong
   * @see   #hasImmediateOutputFormat()
   * @see   #batchFinished()
   */
  protected Instances determineOutputFormat(Instances inputFormat)
      throws Exception {

    Instances 	result;
    Instances	relFormat;
    FastVector	atts;
    int		i;
    int		n;
    int		m;
    int		clsIndex;
    Attribute	att;
    String	prefix;

    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);
    
    atts     = new FastVector();
    clsIndex = -1;
    for (i = 0; i < inputFormat.numAttributes(); i++) {
      // we don't process the class
      if (i == inputFormat.classIndex()) {
	clsIndex = atts.size();
	atts.addElement(inputFormat.attribute(i).copy());
	continue;
      }
      
      if (!inputFormat.attribute(i).isRelationValued()) {
	atts.addElement(inputFormat.attribute(i).copy());
	continue;
      }
      
      if (!m_SelectedRange.isInRange(i)) {
	if (getDebug())
	  System.out.println(
	      "Attribute " + (i+1) + " (" + inputFormat.attribute(i).name() 
	      + ") skipped.");
	continue;
      }

      // process relational attribute
      prefix    = inputFormat.attribute(i).name() + "_";
      relFormat = inputFormat.attribute(i).relation();
      for (n = 0; n < relFormat.numAttributes(); n++) {
	att = relFormat.attribute(n);
	
	if (att.isNumeric()) {
	  atts.addElement(new Attribute(prefix + att.name() + "_MIN"));
	  atts.addElement(new Attribute(prefix + att.name() + "_MAX"));
	  atts.addElement(new Attribute(prefix + att.name() + "_AVG"));
	  atts.addElement(new Attribute(prefix + att.name() + "_STDEV"));
	  atts.addElement(new Attribute(prefix + att.name() + "_SUM"));
	}
	else if (att.isNominal()) {
	  if (att.numValues() <= m_MaxCardinality) {
	    for (m = 0; m < att.numValues(); m++)
	      atts.addElement(new Attribute(prefix + att.name() + "_" + att.value(m) + "_CNT"));
	  }
	  else {
	    if (getDebug())
	      System.out.println(
		  "Attribute " + (i+1) + "/" + (n+1) 
		  + " (" + inputFormat.attribute(i).name() + "/" + att.name()
		  + ") skipped, " + att.numValues() + " > " + m_MaxCardinality + ".");
	  }
	}
	else {
	  if (getDebug())
	    System.out.println(
		"Attribute " + (i+1) + "/" + (n+1) 
		+ " (" + inputFormat.attribute(i).name() + "/" + att.name()
		+ ") skipped.");
	}
      }
    }
    
    // generate new format
    result = new Instances(inputFormat.relationName(), atts, 0);
    result.setClassIndex(clsIndex);
    
    // neither string nor relational attributes need to be copied to the 
    // output:
    initOutputLocators(result, new int[0]);
    
    return result;
  }

  /**
   * Processes the given data (may change the provided dataset) and returns
   * the modified version. This method is called in batchFinished().
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   * @see               #batchFinished()
   */
  protected Instances process(Instances instances) throws Exception {
    Instances		result;
    Instance		inst;
    Instance		newInst;
    Instances		relInstances;
    int			k;
    int			l;
    int			i;
    int			n;
    int			m;
    AttributeStats	stats;
    Attribute		att;
    
    result = getOutputFormat();

    // initialize attribute statistics
    m_AttStats.clear();

    // collect data for all relational attributes
    for (i = 0; i < instances.numAttributes(); i++) {
      if (i == instances.classIndex())
	continue;

      if (!instances.attribute(i).isRelationValued())
	continue;

      if (!m_SelectedRange.isInRange(i))
	continue;

      // compute statistics
      for (k = 0; k < instances.numInstances(); k++) {
	relInstances = instances.instance(k).relationalValue(i);

	for (n = 0; n < relInstances.numAttributes(); n++) {
	  att   = relInstances.attribute(n);
	  stats = null;

	  if (    att.isNumeric() 
	      || (att.isNominal() && att.numValues() <= m_MaxCardinality) ) {
	    stats = relInstances.attributeStats(n);
	    m_AttStats.put(k + "-" + i + "-" + n, stats);
	  }
	}
      }
    }
    
    // convert data
    for (k = 0; k < instances.numInstances(); k++) {
      inst    = instances.instance(k);
      newInst = new Instance(result.numAttributes());
      newInst.setWeight(inst.weight());

      l = 0;
      for (i = 0; i < instances.numAttributes(); i++) {
	if (!instances.attribute(i).isRelationValued()) {
	  newInst.setValue(l, inst.value(i));
	  l++;
	}
	else {
	  if (!m_SelectedRange.isInRange(i))
	    continue;
	  
	  // replace relational data with statistics
	  relInstances = inst.relationalValue(i);
	  for (n = 0; n < relInstances.numAttributes(); n++) {
	    att   = relInstances.attribute(n);
	    stats = (AttributeStats) m_AttStats.get(k + "-" + i + "-" + n);
	    
	    if (att.isNumeric()) {
	      newInst.setValue(l, stats.numericStats.min); l++;
	      newInst.setValue(l, stats.numericStats.max); l++;
	      newInst.setValue(l, stats.numericStats.mean); l++;
	      newInst.setValue(l, stats.numericStats.stdDev); l++;
	      newInst.setValue(l, stats.numericStats.sum); l++;
	    }
	    else if (att.isNominal() && att.numValues() <= m_MaxCardinality) {
	      for (m = 0; m < att.numValues(); m++) {
		newInst.setValue(l, stats.nominalCounts[m]);
		l++;
	      }
	    }
	  }
	}
      }
      
      result.add(newInst);
    }
    
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.3 $");
  }

  /**
   * runs the filter with the given arguments
   *
   * @param args      the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new RELAGGS(), args);
  }
}
