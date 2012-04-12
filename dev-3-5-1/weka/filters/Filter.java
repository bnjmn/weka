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
 *    Filter.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.filters;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.util.Enumeration;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Queue;
import weka.core.Utils;

/** 
 * An abstract class for instance filters: objects that take instances
 * as input, carry out some transformation on the instance and then
 * output the instance. The method implementations in this class
 * assume that most of the work will be done in the methods overridden
 * by subclasses.<p>
 *
 * A simple example of filter use. This example doesn't remove
 * instances from the output queue until all instances have been
 * input, so has higher memory consumption than an approach that
 * uses output instances as they are made available:<p>
 *
 * <code> <pre>
 *  Filter filter = ..some type of filter..
 *  Instances instances = ..some instances..
 *  for (int i = 0; i < data.numInstances(); i++) {
 *    filter.input(data.instance(i));
 *  }
 *  filter.batchFinished();
 *  Instances newData = filter.outputFormat();
 *  Instance processed;
 *  while ((processed = filter.output()) != null) {
 *    newData.add(processed);
 *  }
 *  ..do something with newData..
 * </pre> </code>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.24 $
 */
public abstract class Filter implements Serializable {

  /*
   * Filter refactoring TODO:
   *
   * - Update all filters to use getOutputFormat and setInputFormat
   * instead of outputFormat, outputFormatPeek and inputFormat.
   * - Update users of filters to use getOutputFormat and setInputFormat
   * - remove outputFormat, outputFormatPeek and inputFormat
   *
   */

  /** Debugging mode */
  private boolean m_Debug = false;

  /** The output format for instances */
  private Instances m_OutputFormat = null;

  /** The output instance queue */
  private Queue m_OutputQueue = null;

  /** Indices of string attributes in the output format */
  private int [] m_OutputStringAtts = null;

  /** Indices of string attributes in the input format */
  private int [] m_InputStringAtts = null;

  /** The input format for instances */
  private Instances m_InputFormat = null;

  /** Record whether the filter is at the start of a batch */
  protected boolean m_NewBatch = true;

  /**
   * Sets the format of output instances. The derived class should use this
   * method once it has determined the outputformat. The 
   * output queue is cleared.
   *
   * @param outputFormat the new output format
   */
  protected void setOutputFormat(Instances outputFormat) {

    if (outputFormat != null) {
      m_OutputFormat = outputFormat.stringFreeStructure();
      m_OutputStringAtts = getStringIndices(m_OutputFormat);

      // Rename the attribute
      String relationName = outputFormat.relationName() 
        + "-" + this.getClass().getName();
      if (this instanceof OptionHandler) {
        String [] options = ((OptionHandler)this).getOptions();
        for (int i = 0; i < options.length; i++) {
          relationName += options[i].trim();
        }
      }
      m_OutputFormat.setRelationName(relationName);
    } else {
      m_OutputFormat = null;
    }
    m_OutputQueue = new Queue();
  }

  /**
   * Gets the currently set inputformat instances. This dataset may contain
   * buffered instances.
   *
   * @return the input Instances.
   */
  protected Instances getInputFormat() {

    return m_InputFormat;
  }

  /**
   * Returns a reference to the current input format without
   * copying it.
   *
   * @return a reference to the current input format
   */
  protected Instances inputFormatPeek() {

    return m_InputFormat;
  }

  /**
   * Returns a reference to the current output format without
   * copying it.
   *
   * @return a reference to the current output format
   */
  protected Instances outputFormatPeek() {

    return m_OutputFormat;
  }

  /**
   * Adds an output instance to the queue. The derived class should use this
   * method for each output instance it makes available. 
   *
   * @param instance the instance to be added to the queue.
   */
  protected void push(Instance instance) {

    if (instance != null) {
      copyStringValues(instance, m_OutputFormat, m_OutputStringAtts);
      instance.setDataset(m_OutputFormat);
      m_OutputQueue.push(instance);
    }
  }

  /**
   * Clears the output queue.
   */
  protected void resetQueue() {

    m_OutputQueue = new Queue();
  }

  /**
   * Adds the supplied input instance to the inputformat dataset for
   * later processing.  Use this method rather than
   * getInputFormat().add(instance). Or else. Note that the provided
   * instance gets copied when buffered. 
   *
   * @param instance the <code>Instance</code> to buffer.  
   */
  protected void bufferInput(Instance instance) {

    if (instance != null) {
      copyStringValues(instance, m_InputFormat, m_InputStringAtts);
      m_InputFormat.add(instance);
    }
  }

  /**
   * Returns an array containing the indices of all string attributes in the
   * input format. This index is created during setInputFormat()
   *
   * @return an array containing the indices of string attributes in the 
   * input dataset.
   */
  protected int [] getInputStringIndex() {

    return m_InputStringAtts;
  }

  /**
   * Returns an array containing the indices of all string attributes in the
   * output format. This index is created during setOutputFormat()
   *
   * @return an array containing the indices of string attributes in the 
   * output dataset.
   */
  protected int [] getOutputStringIndex() {

    return m_OutputStringAtts;
  }

  /**
   * Copies string values contained in the instance copied to a new
   * dataset. The Instance must already be assigned to a dataset. This
   * dataset and the destination dataset must have the same structure.
   *
   * @param instance the Instance containing the string values to copy.
   * @param destDataset the destination set of Instances
   * @param strAtts an array containing the indices of any string attributes
   * in the dataset.  
   */
  private void copyStringValues(Instance inst, Instances destDataset, 
                                int []strAtts) {

    if (strAtts.length == 0) {
      return;
    }
    if (inst.dataset() == null) {
      throw new IllegalArgumentException("Instance has no dataset assigned!!");
    } else if (inst.dataset().numAttributes() != destDataset.numAttributes()) {
      throw new IllegalArgumentException("Src and Dest differ in # of attributes!!");
    } 
    copyStringValues(inst, true, inst.dataset(), strAtts,
                     destDataset, strAtts);
  }

  /**
   * Takes string values referenced by an Instance and copies them from a
   * source dataset to a destination dataset. The instance references are
   * updated to be valid for the destination dataset. The instance may have the 
   * structure (i.e. number and attribute position) of either dataset (this
   * affects where references are obtained from). The source dataset must
   * have the same structure as the filter input format and the destination
   * must have the same structure as the filter output format.
   *
   * @param instance the instance containing references to strings in the source
   * dataset that will have references updated to be valid for the destination
   * dataset.
   * @param instSrcCompat true if the instance structure is the same as the
   * source, or false if it is the same as the destination
   * @param srcDataset the dataset for which the current instance string
   * references are valid (after any position mapping if needed)
   * @param destDataset the dataset for which the current instance string
   * references need to be inserted (after any position mapping if needed)
   */
  protected void copyStringValues(Instance instance, boolean instSrcCompat,
                                  Instances srcDataset, Instances destDataset) {

    copyStringValues(instance, instSrcCompat, srcDataset, m_InputStringAtts,
                     destDataset, m_OutputStringAtts);
  }

  /**
   * Takes string values referenced by an Instance and copies them from a
   * source dataset to a destination dataset. The instance references are
   * updated to be valid for the destination dataset. The instance may have the 
   * structure (i.e. number and attribute position) of either dataset (this
   * affects where references are obtained from). Only works if the number
   * of string attributes is the same in both indices (implicitly these string
   * attributes should be semantically same but just with shifted positions).
   *
   * @param instance the instance containing references to strings in the source
   * dataset that will have references updated to be valid for the destination
   * dataset.
   * @param instSrcCompat true if the instance structure is the same as the
   * source, or false if it is the same as the destination (i.e. which of the
   * string attribute indices contains the correct locations for this instance).
   * @param srcDataset the dataset for which the current instance string
   * references are valid (after any position mapping if needed)
   * @param srcStrAtts an array containing the indices of string attributes
   * in the source datset.
   * @param destDataset the dataset for which the current instance string
   * references need to be inserted (after any position mapping if needed)
   * @param destStrAtts an array containing the indices of string attributes
   * in the destination datset.
   */
  protected void copyStringValues(Instance instance, boolean instSrcCompat,
                                  Instances srcDataset, int []srcStrAtts,
                                  Instances destDataset, int []destStrAtts) {
    if (srcDataset == destDataset) {
      return;
    }
    if (srcStrAtts.length != destStrAtts.length) {
      throw new IllegalArgumentException("Src and Dest string indices differ in length!!");
    }
    for (int i = 0; i < srcStrAtts.length; i++) {
      int instIndex = instSrcCompat ? srcStrAtts[i] : destStrAtts[i];
      Attribute src = srcDataset.attribute(srcStrAtts[i]);
      Attribute dest = destDataset.attribute(destStrAtts[i]);
      if (!instance.isMissing(instIndex)) {
        //System.err.println(instance.value(srcIndex) 
        //                   + " " + src.numValues()
        //                   + " " + dest.numValues());
        int valIndex = dest.addStringValue(src, (int)instance.value(instIndex));
        // setValue here shouldn't be too slow here unless your dataset has
        // squillions of string attributes
        instance.setValue(instIndex, (double)valIndex);
      }
    }
  }

  /**
   * This will remove all buffered instances from the inputformat dataset.
   * Use this method rather than getInputFormat().delete();
   */
  protected void flushInput() {

    if (m_InputStringAtts.length > 0) {
      m_InputFormat = m_InputFormat.stringFreeStructure();
    } else {
      // This more efficient than new Instances(m_InputFormat, 0);
      m_InputFormat.delete();
    }
  }

  /**
   * @deprecated use <code>setInputFormat(Instances)</code> instead.
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    return setInputFormat(instanceInfo);
  }

  /**
   * Sets the format of the input instances. If the filter is able to
   * determine the output format before seeing any input instances, it
   * does so here. This default implementation clears the output format
   * and output queue, and the new batch flag is set. Overriders should
   * call <code>super.setInputFormat(Instances)</code>
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the inputFormat can't be set successfully 
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    m_InputFormat = instanceInfo.stringFreeStructure();
    m_InputStringAtts = getStringIndices(instanceInfo);
    m_OutputFormat = null;
    m_OutputQueue = new Queue();
    m_NewBatch = true;
    return false;
  }

  /**
   * @deprecated use <code>getOutputFormat()</code> instead.
   */
  public Instances outputFormat() {

    return getOutputFormat();
  }

  /**
   * Gets the format of the output instances. This should only be called
   * after input() or batchFinished() has returned true. The relation
   * name of the output instances should be changed to reflect the
   * action of the filter (eg: add the filter name and options).
   *
   * @return an Instances object containing the output instance
   * structure only.
   * @exception NullPointerException if no input structure has been
   * defined (or the output format hasn't been determined yet) 
   */
  public Instances getOutputFormat() {

    if (m_OutputFormat == null) {
      throw new NullPointerException("No output format defined.");
    }
    return new Instances(m_OutputFormat, 0);
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is
   * processed and made available for output immediately. Some filters
   * require all instances be read before producing output, in which
   * case output instances should be collected after calling
   * batchFinished(). If the input marks the start of a new batch, the
   * output queue is cleared. This default implementation assumes all
   * instance conversion will occur when batchFinished() is called.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception NullPointerException if the input format has not been
   * defined.
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.  
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new NullPointerException("No input instance format defined");
    }
    if (m_NewBatch) {
      m_OutputQueue = new Queue();
      m_NewBatch = false;
    }
    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. If
   * the filter requires all instances prior to filtering, output()
   * may now be called to retrieve the filtered instances. Any
   * subsequent instances filtered should be filtered based on setting
   * obtained from the first batch (unless the inputFormat has been
   * re-assigned or new options have been set). This default
   * implementation assumes all instance processing occurs during
   * inputFormat() and input().
   *
   * @return true if there are instances pending output
   * @exception NullPointerException if no input structure has been defined,
   * @exception Exception if there was a problem finishing the batch.
   */
  public boolean batchFinished() throws Exception {

    if (m_InputFormat == null) {
      throw new NullPointerException("No input instance format defined");
    }
    flushInput();
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }


  /**
   * Output an instance after filtering and remove from the output queue.
   *
   * @return the instance that has most recently been filtered (or null if
   * the queue is empty).
   * @exception NullPointerException if no output structure has been defined
   */
  public Instance output() {

    if (m_OutputFormat == null) {
      throw new NullPointerException("No output instance format defined");
    }
    if (m_OutputQueue.empty()) {
      return null;
    }
    Instance result = (Instance)m_OutputQueue.pop();
    // Clear out references to old strings occasionally
    if (m_OutputQueue.empty() && m_NewBatch) {
      if (m_OutputStringAtts.length > 0) {
        m_OutputFormat = m_OutputFormat.stringFreeStructure();
      }
    }
    return result;
  }
  
  /**
   * Output an instance after filtering but do not remove from the
   * output queue.
   *
   * @return the instance that has most recently been filtered (or null if
   * the queue is empty).
   * @exception NullPointerException if no input structure has been defined 
   */
  public Instance outputPeek() {

    if (m_OutputFormat == null) {
      throw new NullPointerException("No output instance format defined");
    }
    if (m_OutputQueue.empty()) {
      return null;
    }
    Instance result = (Instance)m_OutputQueue.peek();
    return result;
  }

  /**
   * Returns the number of instances pending output
   *
   * @return the number of instances  pending output
   * @exception NullPointerException if no input structure has been defined
   */
  public int numPendingOutput() {

    if (m_OutputFormat == null) {
      throw new NullPointerException("No output instance format defined");
    }
    return m_OutputQueue.size();
  }

  /**
   * Returns whether the output format is ready to be collected
   *
   * @return true if the output format is set
   */
  public boolean isOutputFormatDefined() {

    return (m_OutputFormat != null);
  }

  /**
   * Gets an array containing the indices of all string attributes.
   *
   * @param insts the Instances to scan for string attributes. 
   * @return an array containing the indices of string attributes in
   * the input structure. Will be zero-length if there are no
   * string attributes
   */
  protected int [] getStringIndices(Instances insts) {
    
    // Scan through getting the indices of String attributes
    int [] index = new int [insts.numAttributes()];
    int indexSize = 0;
    for (int i = 0; i < insts.numAttributes(); i++) {
      if (insts.attribute(i).type() == Attribute.STRING) {
        index[indexSize++] = i;
      }
    }
    int [] result = new int [indexSize];
    System.arraycopy(index, 0, result, 0, indexSize);
    return result;
  }
  
  /**
   * Filters an entire set of instances through a filter and returns
   * the new set. 
   *
   * @param data the data to be filtered
   * @param filter the filter to be used
   * @return the filtered set of data
   * @exception Exception if the filter can't be used successfully
   */
  public static Instances useFilter(Instances data,
				    Filter filter) throws Exception {
    /*
    System.err.println(filter.getClass().getName() 
                       + " in:" + data.numInstances());
    */
    for (int i = 0; i < data.numInstances(); i++) {
      filter.input(data.instance(i));
    }
    filter.batchFinished();
    Instances newData = filter.getOutputFormat();
    Instance processed;
    while ((processed = filter.output()) != null) {
      newData.add(processed);
    }

    /*
    System.err.println(filter.getClass().getName() 
                       + " out:" + newData.numInstances());
    */
    return newData;
  }

  /**
   * Method for testing filters.
   *
   * @param argv should contain the following arguments: <br>
   * -i input_file <br>
   * -o output_file <br>
   * -c class_index <br>
   * or -h for help on options
   * @exception Exception if something goes wrong or the user requests help on
   * command options
   */
  public static void filterFile(Filter filter, String [] options) 
    throws Exception {

    boolean debug = false;
    Instances data = null;
    Reader input = null;
    PrintWriter output = null;
    boolean helpRequest;

    try {
       helpRequest = Utils.getFlag('h', options);

      if (Utils.getFlag('d', options)) {
	debug = true;
      }
      String infileName = Utils.getOption('i', options);
      String outfileName = Utils.getOption('o', options); 
      String classIndex = Utils.getOption('c', options);
      
      if (filter instanceof OptionHandler) {
	((OptionHandler)filter).setOptions(options);
      }

      Utils.checkForRemainingOptions(options);
      if (helpRequest) {
	throw new Exception("Help requested.\n");
      }
      if (infileName.length() != 0) {
	input = new BufferedReader(new FileReader(infileName));
      } else {
	input = new BufferedReader(new InputStreamReader(System.in));
      }
      if (outfileName.length() != 0) {
	output = new PrintWriter(new FileOutputStream(outfileName));
      } else { 
	output = new PrintWriter(System.out);
      }

      data = new Instances(input, 1);
      if (classIndex.length() != 0) {
	if (classIndex.equals("first")) {
	  data.setClassIndex(0);
	} else if (classIndex.equals("last")) {
	  data.setClassIndex(data.numAttributes() - 1);
	} else {
	  data.setClassIndex(Integer.parseInt(classIndex) - 1);
	}
      }
    } catch (Exception ex) {
      String filterOptions = "";
      // Output the error and also the valid options
      if (filter instanceof OptionHandler) {
	filterOptions += "\nFilter options:\n\n";
	Enumeration enu = ((OptionHandler)filter).listOptions();
	while (enu.hasMoreElements()) {
	  Option option = (Option) enu.nextElement();
	  filterOptions += option.synopsis() + '\n'
	    + option.description() + "\n";
	}
      }

      String genericOptions = "\nGeneral options:\n\n"
	+ "-h\n"
	+ "\tGet help on available options.\n"
	+ "\t(use -b -h for help on batch mode.)\n"
	+ "-i <file>\n"
	+ "\tThe name of the file containing input instances.\n"
	+ "\tIf not supplied then instances will be read from stdin.\n"
	+ "-o <file>\n"
	+ "\tThe name of the file output instances will be written to.\n"
	+ "\tIf not supplied then instances will be written to stdout.\n"
	+ "-c <class index>\n"
	+ "\tThe number of the attribute to use as the class.\n"
	+ "\t\"first\" and \"last\" are also valid entries.\n"
	+ "\tIf not supplied then no class is assigned.\n";

      throw new Exception('\n' + ex.getMessage()
			  + filterOptions+genericOptions);
    }
    
    if (debug) {
      System.err.println("Setting input format");
    }
    boolean printedHeader = false;
    if (filter.setInputFormat(data)) {
      if (debug) {
	System.err.println("Getting output format");
      }
      output.println(filter.getOutputFormat().toString());
      printedHeader = true;
    }
    
    // Pass all the instances to the filter
    while (data.readInstance(input)) {
      if (debug) {
	System.err.println("Input instance to filter");
      }
      if (filter.input(data.instance(0))) {
	if (debug) {
	  System.err.println("Filter said collect immediately");
	}
	if (!printedHeader) {
	  throw new Error("Filter didn't return true from setInputFormat() "
			  + "earlier!");
	}
	if (debug) {
	  System.err.println("Getting output instance");
	}
	output.println(filter.output().toString());
      }
      data.delete(0);
    }
    
    // Say that input has finished, and print any pending output instances
    if (debug) {
      System.err.println("Setting end of batch");
    }
    if (filter.batchFinished()) {
      if (debug) {
	System.err.println("Filter said collect output");
      }
      if (!printedHeader) {
	if (debug) {
	  System.err.println("Getting output format");
	}
	output.println(filter.getOutputFormat().toString());
      }
      if (debug) {
	System.err.println("Getting output instance");
      }
      while (filter.numPendingOutput() > 0) {
	output.println(filter.output().toString());
	if (debug){
	  System.err.println("Getting output instance");
	}
      }
    }
    if (debug) {
      System.err.println("Done");
    }
    
    if (output != null) {
      output.close();
    }
  }

  /**
   * Method for testing filters ability to process multiple batches.
   *
   * @param argv should contain the following arguments:<br>
   * -i (first) input file <br>
   * -o (first) output file <br>
   * -r (second) input file <br>
   * -s (second) output file <br>
   * -c class_index <br>
   * or -h for help on options
   * @exception Exception if something goes wrong or the user requests help on
   * command options
   */
  public static void batchFilterFile(Filter filter, String [] options) 
    throws Exception {

    Instances firstData = null;
    Instances secondData = null;
    Reader firstInput = null;
    Reader secondInput = null;
    PrintWriter firstOutput = null;
    PrintWriter secondOutput = null;
    boolean helpRequest;
    try {
      helpRequest = Utils.getFlag('h', options);

      String fileName = Utils.getOption('i', options); 
      if (fileName.length() != 0) {
	firstInput = new BufferedReader(new FileReader(fileName));
      } else {
	throw new Exception("No first input file given.\n");
      }

      fileName = Utils.getOption('r', options); 
      if (fileName.length() != 0) {
	secondInput = new BufferedReader(new FileReader(fileName));
      } else {
	throw new Exception("No second input file given.\n");
      }

      fileName = Utils.getOption('o', options); 
      if (fileName.length() != 0) {
	firstOutput = new PrintWriter(new FileOutputStream(fileName));
      } else {
	firstOutput = new PrintWriter(System.out);
      }
      
      fileName = Utils.getOption('s', options); 
      if (fileName.length() != 0) {
	secondOutput = new PrintWriter(new FileOutputStream(fileName));
      } else {
	secondOutput = new PrintWriter(System.out);
      }
      String classIndex = Utils.getOption('c', options);

      if (filter instanceof OptionHandler) {
	((OptionHandler)filter).setOptions(options);
      }
      Utils.checkForRemainingOptions(options);
      
      if (helpRequest) {
	throw new Exception("Help requested.\n");
      }
      firstData = new Instances(firstInput, 1);
      secondData = new Instances(secondInput, 1);
      if (!secondData.equalHeaders(firstData)) {
	throw new Exception("Input file formats differ.\n");
      }
      if (classIndex.length() != 0) {
	if (classIndex.equals("first")) {
	  firstData.setClassIndex(0);
	  secondData.setClassIndex(0);
	} else if (classIndex.equals("last")) {
	  firstData.setClassIndex(firstData.numAttributes() - 1);
	  secondData.setClassIndex(secondData.numAttributes() - 1);
	} else {
	  firstData.setClassIndex(Integer.parseInt(classIndex) - 1);
	  secondData.setClassIndex(Integer.parseInt(classIndex) - 1);
	}
      }
    } catch (Exception ex) {
      String filterOptions = "";
      // Output the error and also the valid options
      if (filter instanceof OptionHandler) {
	filterOptions += "\nFilter options:\n\n";
	Enumeration enu = ((OptionHandler)filter).listOptions();
	while (enu.hasMoreElements()) {
	  Option option = (Option) enu.nextElement();
	  filterOptions += option.synopsis() + '\n'
	    + option.description() + "\n";
	}
      }

      String genericOptions = "\nGeneral options:\n\n"
	+ "-h\n"
	+ "\tGet help on available options.\n"
	+ "-i <filename>\n"
	+ "\tThe file containing first input instances.\n"
	+ "-o <filename>\n"
	+ "\tThe file first output instances will be written to.\n"
	+ "-r <filename>\n"
	+ "\tThe file containing second input instances.\n"
	+ "-s <filename>\n"
	+ "\tThe file second output instances will be written to.\n"
	+ "-c <class index>\n"
	+ "\tThe number of the attribute to use as the class.\n"
	+ "\t\"first\" and \"last\" are also valid entries.\n"
	+ "\tIf not supplied then no class is assigned.\n";

      throw new Exception('\n' + ex.getMessage()
			  + filterOptions+genericOptions);
    }
    boolean printedHeader = false;
    if (filter.setInputFormat(firstData)) {
      firstOutput.println(filter.getOutputFormat().toString());
      printedHeader = true;
    }
    
    // Pass all the instances to the filter
    while (firstData.readInstance(firstInput)) {
      if (filter.input(firstData.instance(0))) {
	if (!printedHeader) {
	  throw new Error("Filter didn't return true from setInputFormat() "
			  + "earlier!");
	}
	firstOutput.println(filter.output().toString());
      }
      firstData.delete(0);
    }
    
    // Say that input has finished, and print any pending output instances
    if (filter.batchFinished()) {
      if (!printedHeader) {
	firstOutput.println(filter.getOutputFormat().toString());
      }
      while (filter.numPendingOutput() > 0) {
	firstOutput.println(filter.output().toString());
      }
    }
    
    if (firstOutput != null) {
      firstOutput.close();
    }    
    printedHeader = false;
    if (filter.isOutputFormatDefined()) {
      secondOutput.println(filter.getOutputFormat().toString());
      printedHeader = true;
    }
    // Pass all the second instances to the filter
    while (secondData.readInstance(secondInput)) {
      if (filter.input(secondData.instance(0))) {
	if (!printedHeader) {
	  throw new Error("Filter didn't return true from"
			  + " isOutputFormatDefined() earlier!");
	}
	secondOutput.println(filter.output().toString());
      }
      secondData.delete(0);
    }
    
    // Say that input has finished, and print any pending output instances
    if (filter.batchFinished()) {
      if (!printedHeader) {
	secondOutput.println(filter.getOutputFormat().toString());
      }
      while (filter.numPendingOutput() > 0) {
	secondOutput.println(filter.output().toString());
      }
    }
    if (secondOutput != null) {
      secondOutput.close();
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] args) {
    
    try {
      if (args.length == 0) {
        throw new Exception("First argument must be the class name of a Filter");
      }
      String fname = args[0];
      Filter f = (Filter)Class.forName(fname).newInstance();
      args[0] = "";
      if (Utils.getFlag('b', args)) {
	Filter.batchFilterFile(f, args);
      } else {
	Filter.filterFile(f, args);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}








