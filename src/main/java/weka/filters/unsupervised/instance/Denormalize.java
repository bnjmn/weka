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
 *    Denormalize.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.instance;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 <!-- globalinfo-start -->
 * An instance filter that collapses instances with a common grouping ID value into a single instance. Useful for converting transactional data into a format that Weka's association rule learners can handle. IMPORTANT: assumes that the incoming batch of instances has been sorted on the grouping attribute. The values of nominal attributes are converted to indicator attributes. These can be either binary (with f and t values) or unary with missing values used to indicate absence. The later is Weka's old market basket format, which is useful for Apriori. Numeric attributes can be aggregated within groups by computing the average, sum, minimum or maximum.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -G &lt;index | name | first | last&gt;
 *  Index or name of attribute to group by. e.g. transaction ID
 *  (default: first)</pre>
 * 
 * <pre> -B
 *  Output instances in Weka's old market basket format (i.e. unary attributes with absence indicated
 *   by missing values.</pre>
 * 
 * <pre> -S
 *  Output sparse instances (can't be used in conjunction with -B)</pre>
 * 
 * <pre> -A &lt;Average | Sum | Maximum | Minimum&gt;
 *  Aggregation function for numeric attributes.
 *  (default: sum).</pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class Denormalize extends Filter 
  implements UnsupervisedFilter, OptionHandler, StreamableFilter {
  
  /** For serialization */
  private static final long serialVersionUID = -6334763153733741054L;

  /** Enumeration of the aggregation methods for numeric attributes */
  public static enum NumericAggregation {
    AVG ("Average"),
    SUM ("Sum"),
    MIN ("Minimum"),
    MAX ("Maximum");
    
    private final String m_stringVal;
    NumericAggregation(String name) {
      m_stringVal = name;
    }
    
    public String toString() {
      return m_stringVal;
    }
  }
  
  /** tags */
  public static final Tag[] TAGS_SELECTION = {
    new Tag(NumericAggregation.AVG.ordinal(), "Average"),
    new Tag(NumericAggregation.SUM.ordinal(), "Sum"),
    new Tag(NumericAggregation.MIN.ordinal(), "Minimum"),
    new Tag(NumericAggregation.MAX.ordinal(), "Maximum")
  };
  
  /**
   * The index (1-based) or name of the attribute that contains the
   * key field to group by (can use first and last too).
   */
  protected String m_groupingAttribute = "first";
  
  /** The 0-based index of the grouping attribute */
  protected int m_groupingIndex = -1;
  
  /** 
   * Denormalized nominal values in Weka's market basket format
   * (i.e. unary attributes with missing values for absent)?
   */
  protected boolean m_nonSparseMarketBasketFormat = false;
  
  /**
   * Sparse format with denormalized nominal values as binary 
   * (absence as zero).
   */
  protected boolean m_sparseFormat = true;
  
  /** How to aggregate numeric values within a group */
  protected NumericAggregation m_numericAggregation = NumericAggregation.SUM;
  
  /**
   * Returns a string describing this associator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that collapses instances with " +
    		"a common grouping ID value into a single instance. " +
    		"Useful for converting transactional data into a " +
    		"format that Weka's association rule learners can " +
    		"handle. IMPORTANT: assumes that the incoming batch " +
    		"of instances has been sorted on the grouping attribute. " +
    		"The values of nominal attributes are " +
    		"converted to indicator attributes. These " +
    		"can be either binary (with f and t values) or " +
    		"unary with missing values used to indicate " +
    		"absence. The later is Weka's old market basket " +
    		"format, which is useful for Apriori. Numeric " +
    		"attributes can be aggregated within groups by " +
    		"computing the average, sum, minimum or maximum."; 
  }
  
  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();
    result.enable(Capability.NO_CLASS);
    result.enableAllClasses();
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    return result;
  }

  private Map<String, Integer> m_newFormatIndexes = null;
  
  /**
   * Sets up the structure of the output instances.
   * 
   * @param instanceInfo the input structure.
   * @throws Exception if something goes wrong.
   */
  protected void createOutputFormat(Instances instanceInfo) throws Exception {
    m_newFormatIndexes = new HashMap<String,Integer>();
    ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
    // determine output format
    int count = 0;
    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (i == m_groupingIndex) {
        attInfo.add(instanceInfo.attribute(m_groupingIndex));
        m_newFormatIndexes.put(instanceInfo.attribute(m_groupingIndex).name(), new Integer(count));
        count++;
      } else if (instanceInfo.attribute(i).isNumeric()) {
        String newName = m_numericAggregation.toString() + "_"
        + instanceInfo.attribute(i).name();
//        System.err.println("-- " + newName);
        attInfo.add(new Attribute(newName));
        m_newFormatIndexes.put(newName, new Integer(count));
        count++;
      } else if (instanceInfo.attribute(i).isNominal()) {
        // nominal
        
        // create a new attribute for each nominal value
        for (int j = 0; j < instanceInfo.attribute(i).numValues(); j++) {
          ArrayList<String> vals = new ArrayList<String>();
          if (m_nonSparseMarketBasketFormat) {
            vals.add("t");
          } else {
            vals.add("f"); vals.add("t");
          }
          
          String newName = instanceInfo.attribute(i).name() + "_"
            + instanceInfo.attribute(i).value(j);
//          System.err.println("-- " + newName);
          attInfo.add(new Attribute(newName, vals));
          m_newFormatIndexes.put(newName, new Integer(count));
          count++;
        }        
      }
    }
    
    Instances outputFormat = new Instances(instanceInfo.relationName() 
        + "_denormalized", attInfo, 0);
//    System.err.println(outputFormat);
    setOutputFormat(outputFormat);
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the inputFormat can't be set successfully 
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);
    
    m_groupingIndex = -1;
    m_currentGroup = -1;
    // first try and parse the grouping attribute as a number
    try {
      m_groupingIndex = Integer.parseInt(m_groupingAttribute);
      m_groupingIndex--;
    } catch (NumberFormatException e) { 
      if (m_groupingAttribute.equalsIgnoreCase("first")) {
        m_groupingIndex = 0;
      } else if (m_groupingAttribute.equalsIgnoreCase("last")) {
        m_groupingIndex = instanceInfo.numAttributes() - 1;
      } else {
        // look for the attribute
        for (int i = 0; i < instanceInfo.numAttributes(); i++) {
          if (instanceInfo.attribute(i).name().
                equalsIgnoreCase(m_groupingAttribute)) {
            m_groupingIndex = i;
            break;
          }
        }
      }
    }
    
    if (m_groupingIndex == -1) {
      throw new Exception("Unable to determine which attribute should " +
      		"be used for grouping!");
    }
    
    if (instanceInfo.attribute(m_groupingIndex).isString()) {
      throw new Exception("The grouping attribute must be either numeric" +
      		" or nominal!");
    }
    
    createOutputFormat(instanceInfo);
    System.err.println("[Denormalize] WARNING: this filter expects the incoming batch of " +
    		"instances to be sorted in order by the grouping attribute.");
    
    return true;
  }
  
  private double m_currentGroup = -1;
  private double[] m_tempVals = null;
  private double[] m_counts = null;
  
  /**
   * Convert an instance to the output format. An instance
   * is only pushed onto the output when the current group
   * (as defined by the grouping ID attribute) finishes.
   * 
   * @param instance an instance to process
   * @return true if an output instance has been pushed onto
   * the output queue.
   * @throws Exception if something goes wrong.
   */
  protected boolean convertInstance(Instance instance) throws Exception {
    Instances outputFormat = outputFormatPeek();
    boolean instanceOutputted = false;
    
    if (m_currentGroup == -1 || instance.value(m_groupingIndex) != m_currentGroup) {      
      if (m_tempVals != null) {
        // make the new instance
        for (int i = 0; i < outputFormat.numAttributes(); i++) {
          if (outputFormat.attribute(i).isNominal()) {
            if (m_nonSparseMarketBasketFormat) {
              // set any -1's to missing
              if (m_tempVals[i] == -1) {
                m_tempVals[i] = Utils.missingValue();
              }
            } else if (m_tempVals[i] == -1) {
              // set the -1's to zero (for non-market basket/sparse format)
              m_tempVals[i] = 0;
            }
          } else if (outputFormat.attribute(i).isNumeric()) {
            if (m_numericAggregation == NumericAggregation.AVG && m_counts[i] > 0) {
              m_tempVals[i] /= m_counts[i];
            }
          }
        }
        
        Instance tempI = null;
        tempI = new DenseInstance(1.0, m_tempVals);
        if (m_sparseFormat && !m_nonSparseMarketBasketFormat) {
          tempI = new SparseInstance(tempI);
        }
        
//        System.err.println("pushing: " + tempI);
        push(tempI); // make available for collection
        instanceOutputted = true;
        
        // current "batch" (i.e. group) is done
   /*     flushInput();
        m_NewBatch = true; */
      }

      if (instance != null) {
        if (m_currentGroup == -1 || instance.value(m_groupingIndex) != m_currentGroup) {
          m_currentGroup = instance.value(m_groupingIndex);
          m_tempVals = new double[outputFormat.numAttributes()];

          for (int i = 0; i < outputFormat.numAttributes(); i++) {
            if (outputFormat.attribute(i).isNominal()) {
              m_tempVals[i] = -1; // mark as not set
            } else if (outputFormat.attribute(i).isNumeric()) {
              if (m_numericAggregation == NumericAggregation.MAX) {
                m_tempVals[i] = Double.MIN_VALUE;
              } else if (m_numericAggregation == NumericAggregation.MIN) {
                m_tempVals[i] = Double.MAX_VALUE;
              }
            }
          }
//          m_count = 0;
          m_counts = new double[outputFormat.numAttributes()];
        }
      }
    }
    
    if (instance != null) {
      //m_count++;
      for (int i = 0; i < instance.numAttributes(); i++) {
        
        if (!Utils.isMissingValue(instance.value(i))) {
          if (i == m_groupingIndex) {
            int newIndex = m_newFormatIndexes.get(instance.attribute(m_groupingIndex).name());
            m_tempVals[newIndex] = instance.value(m_groupingIndex);
        //    System.err.println("-> " + m_tempVals[newIndex]);
          } else if (instance.attribute(i).isNominal()) {
            String newName = instance.attribute(i).name() + "_"
            + instance.attribute(i).value((int)instance.value(i));
            Integer nn = m_newFormatIndexes.get(newName);
            int newIndex = nn.intValue();

            if (m_nonSparseMarketBasketFormat) {
              m_tempVals[newIndex] = 0; // unary attributes
            } else {
//              System.err.println("Here");
              m_tempVals[newIndex] = 1; // binary attributes
            }
          } else if (instance.attribute(i).isNumeric()) {
            String newName = m_numericAggregation.toString() + "_"
            + instance.attribute(i).name();
            int newIndex = m_newFormatIndexes.get(newName);
            m_counts[newIndex]++;
            switch (m_numericAggregation) {
            
            case  AVG:              
            case SUM:
              m_tempVals[newIndex] += instance.value(i);
              break;
            case MIN:
              if (instance.value(i) < m_tempVals[newIndex]) {
                m_tempVals[newIndex] = instance.value(i);
              }
              break;
            case MAX:
              if (instance.value(i) > m_tempVals[newIndex]) {
                m_tempVals[newIndex] = instance.value(i);
              }
              break;
            }
          }
        }
      }
    }
    
    return instanceOutputted;
  }
  
  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    
    return convertInstance(instance);
  }
  
  /**
   * Signify that this batch of input to the filter is finished.
   *
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined 
   */
  public boolean batchFinished() throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    
    if (outputFormatPeek() == null) {
      createOutputFormat(getInputFormat());
    }
        
    if (m_tempVals != null) {
      // process the last group
      m_currentGroup = -1;
      convertInstance(null);
    }
    
    flushInput();
    m_NewBatch = true;
    m_currentGroup = -1;
    m_tempVals = null;
    m_counts = null;
    return (numPendingOutput() != 0);
  }
  
  /**
   * Returns a description of this option suitable for display
   * as a tip text in the gui.
   *
   * @return description of this option
   */
  public String groupingAttributeTipText() {
    return "Set the attribute that defines the groups (e.g. transaction ID).";
  }
  
  /**
   * Set the name or index of the attribute to use for grouping
   * rows (transactions). "first" and "last" may also be used.
   * 
   * @param groupAtt the name/index of the attribute to use for
   * grouping
   */
  public void setGroupingAttribute(String groupAtt) {
    m_groupingAttribute = groupAtt;
  }
  
  /**
   * Get the name/index of the attribute to be used for
   * grouping rows (tranasactions).
   * 
   * @return the name/index of the attribute to use for
   * grouping.
   */
  public String getGroupingAttribute() {
    return m_groupingAttribute;
  }
  
  /**
   * Set whether to output data in Weka's old market basket format.
   * This format uses unary attributes and missing values to indicate
   * absence. Apriori works best on market basket type data in this format.
   * 
   * @param m true if data is to be output in Weka's old market basket format.
   */
  public void setUseOldMarketBasketFormat(boolean m) {
    m_nonSparseMarketBasketFormat = m;
  }
  
  /**
   * Gets whether data is to be output in Weka's old market basket
   * format.
   * 
   * @return true if data is to be output in Weka's old market basket
   * format.
   */
  public boolean getUseOldMarketBasketFormat() {
    return m_nonSparseMarketBasketFormat;
  }
  
  /**
   * Returns a description of this option suitable for display
   * as a tip text in the gui.
   *
   * @return description of this option
   */
  public String useOldMarketBasketFormatTipText() {
    return "Output instances that contain unary attributes with" +
    		" absence indicated by missing values. Apriori operates " +
    		"faster with this format.";
  }
  
  /**
   * Set whether to output sparse data. Only one or the other
   * of this option or oldMarketBasketFormat can be used.
   * 
   * @param s true if sparse data is to be output.
   */
  public void setUseSparseFormat(boolean s) {
    m_sparseFormat = s;
  }
  
  /**
   * Get whether sparse data is to be output.
   * 
   * @return true if sparse data is to be output.
   */
  public boolean getUseSparseFormat() {
    return m_sparseFormat;
  }
  
  /**
   * Returns a description of this option suitable for display
   * as a tip text in the gui.
   *
   * @return description of this option
   */
  public String useSparseFormatTipText() {
    return "Output sparse instances (can't be used in conjunction " +
    		"with useOldMarketBasketFormat).";
  }
  
  /**
   * Set the type of aggregation to use on numeric values within
   * a group. Available choices are: Sum, Average, Min and Max.
   * 
   * @param d the type of aggregation to use for numeric values.
   */
  public void setAggregationType(SelectedTag d) {
    int ordinal = d.getSelectedTag().getID();
    
    for (NumericAggregation n : NumericAggregation.values()) {
      if (n.ordinal() == ordinal) {
        m_numericAggregation = n;
        break;
      }
    }
  }
  
  /**
   * Get the type of aggregation to use on numeric values withn
   * a group. Available choices are: Sum, Average, Min and Max.
   * 
   * @return the type of aggregation to use for numeric values.
   */
  public SelectedTag getAggregationType() {
    return new SelectedTag(m_numericAggregation.ordinal(), TAGS_SELECTION);
  }
  
  /**
   * Returns a description of this option suitable for display
   * as a tip text in the gui.
   *
   * @return description of this option
   */
  public String aggregationTypeTipText() {
    return "The type of aggregation to apply to numeric attributes.";
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector<Option> newVector = new Vector<Option>();
    
    newVector.add(new Option("\tIndex or name of attribute to group by. e.g. " +
    		"transaction ID\n\t(default: first)", "G", 1, "-G <index | " +
    				"name | first | last>"));
    newVector.add(new Option("\tOutput instances in Weka's old market basket " +
    		"format (i.e. unary attributes with absence indicated\n\t " +
    		"by missing values.", "B", 0, "-B"));
    newVector.add(new Option("\tOutput sparse instances (can't be " +
    		"used in conjunction with -B)", "S", 0, "-S"));
    newVector.add(new Option("\tAggregation function for numeric attributes." +
    		"\n\t(default: sum).", "A", 1, "-A <Average | Sum | Maximum | Minimum>"));
    
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -G &lt;index | name | first | last&gt;
   *  Index or name of attribute to group by. e.g. transaction ID
   *  (default: first)</pre>
   * 
   * <pre> -B
   *  Output instances in Weka's old market basket format (i.e. unary attributes with absence indicated
   *   by missing values.</pre>
   * 
   * <pre> -S
   *  Output sparse instances (can't be used in conjunction with -B)</pre>
   * 
   * <pre> -A &lt;Average | Sum | Maximum | Minimum&gt;
   *  Aggregation function for numeric attributes.
   *  (default: sum).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String groupName = Utils.getOption('G', options);
    if (groupName.length() != 0) {
      setGroupingAttribute(groupName);
    }
    
    setUseOldMarketBasketFormat(Utils.getFlag('B', options));
    setUseSparseFormat(Utils.getFlag('S', options));
    
    String aggregation = Utils.getOption('A', options);
    if (aggregation.length() != 0) {
      NumericAggregation selected = null;
      for (NumericAggregation n : NumericAggregation.values()) {
        if (n.toString().equalsIgnoreCase(aggregation)) {
          selected = n;
          break;
        }
      }
      if (selected == null) {
        throw new Exception("Unknown aggregation type: " + aggregation + "!");
      }
      
      setAggregationType(new SelectedTag(selected.ordinal(), TAGS_SELECTION));
    }
    
    Utils.checkForRemainingOptions(options);
  }
  
  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();
    
    options.add("-G"); options.add(getGroupingAttribute());
    options.add("-A"); options.add(m_numericAggregation.toString());
    if (getUseOldMarketBasketFormat()) {
      options.add("-B");
    } else if (getUseSparseFormat()) {
      options.add("-S");
    }
    
    return options.toArray(new String[1]);   
  }
  
  /**
   * Returns the revision string.
   * 
   * @return            the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
  
  /**
   * Main method for testing this class.
   *
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new Denormalize(), args);
  }
}

