
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
 * @version $Revision: 1.12 $
 */
public abstract class Filter implements Serializable {

  /** Debugging mode */
  private boolean m_Debug = false;

  /** The output format for instances */
  private Instances m_OutputFormat = null;

  /** The output instance queue */
  private Queue m_OutputQueue = null;

  /** Indices of string attributes in the output format */
  private int [] m_OutputStringAtts;

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

      // Scan through getting the indices of String attributes
      int [] index = new int [m_OutputFormat.numAttributes()];
      int indexSize = 0;
      for (int i = 0; i < m_OutputFormat.numAttributes(); i++) {
        if (m_OutputFormat.attribute(i).type() == Attribute.STRING) {
          index[indexSize++] = i;
        }
      }
      m_OutputStringAtts = new int [indexSize];
      System.arraycopy(index, 0, m_OutputStringAtts, 0, indexSize);

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
   * @param instance the instance to be added to the queue
   */
  protected void push(Instance instance) {

    if (instance != null) {
      // Copy any string values across
      Instances origDataset = instance.dataset();
      for (int i = 0; i < m_OutputStringAtts.length; i++) {
        int attIndex = m_OutputStringAtts[i];
        Attribute dest = m_OutputFormat.attribute(attIndex);
        Attribute src = origDataset.attribute(attIndex);
        int valIndex = dest.addStringValue(src, (int)instance.value(attIndex));
        instance.setValue(attIndex, (double)valIndex);
      }
      // Point the instance at the outputformat dataset
      instance.setDataset(m_OutputFormat);
      // Queue up for later retrieval
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
   * Sets the format of the input instances. If the filter is able to
   * determine the output format before seeing any input instances, it
   * does so here. This default implementation clears the output format
   * and output queue, and the new batch flag is set. Overriders should
   * call super.inputFormat()
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the inputFormat can't be set successfully 
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    // TODO. Make m_InputFormat safe for string attributes 

    // The current implementation will break if we had a filter that
    // couldn't process instances in non-first batches incrementally,
    // because of the reassignment of dataset that happens when
    // buffering the data.  For the first batch this is OK, since
    // strings aren't removed when we copy the inputformat structure.

    // inputFormat() would use stringFreeStructure
    // inputFormat() would create an index of string atts
    // all overriders of inputFormat must call super.inputFormat() or
    // otherwise ensure inputFormat was created stringFree
    // all buffering should be done by bufferInput()
    // bufferInput() would copy string values between the headers

    m_InputFormat = new Instances(instanceInfo, 0);
    m_OutputFormat = null;
    m_OutputQueue = new Queue();
    m_NewBatch = true;
    return false;
  }

  /**
   * Gets the format of the output instances. This should only be called
   * after input() or batchFinished() has returned true. The relation
   * name of the output instances should be changed to reflect the
   * action of the filter (eg: add the filter name and options).
   *
   * @return an Instances object containing the output instance
   * structure only.
   * @exception Exception if no input structure has been defined (or the 
   * output format hasn't been determined yet)
   */
  public final Instances outputFormat() throws Exception {

    if (m_OutputFormat == null) {
      throw new Exception("No output format defined.");
    }
    return m_OutputFormat;
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
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.  
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      m_OutputQueue = new Queue();
      m_NewBatch = false;
    }
    bufferInput(instance);
    return false;
  }

  /**
   * Adds the supplied input to the inputformat dataset for later processing.
   * Use this method rather than getInputFormat().add(instance). Or else.
   *
   * @param instance the <code>Instance</code> to buffer.
   */
  protected void bufferInput(Instance instance) {

    // This will most likely change to do special string attribute 
    // manipulation stuff. See the comments in inputFormat()
    m_InputFormat.add(instance);
  }

  /**
   * This will remove all buffered instances from the inputformat dataset.
   * Use this method rather than getInputFormat().delete();
   */
  protected void flushInput() {

    m_InputFormat = m_InputFormat.stringFreeStructure();
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
   * @exception Exception if no input structure has been defined 
   */
  public boolean batchFinished() throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
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
   * @exception Exception if no input structure has been defined
   */
  public Instance output() throws Exception {

    if (m_OutputFormat == null) {
      throw new Exception("No output instance format defined");
    }
    if (m_OutputQueue.empty()) {
      return null;
    }
    Instance result = (Instance)m_OutputQueue.pop();
    if (m_OutputQueue.empty() && m_NewBatch) {
      //m_OutputFormat = m_OutputFormat.stringFreeStructure();
    }
    return result;
  }
  
  /**
   * Output an instance after filtering but do not remove from the
   * output queue.
   *
   * @return the instance that has most recently been filtered (or null if
   * the queue is empty).
   * @exception Exception if no input structure has been defined 
   */
  public Instance outputPeek() throws Exception {

    if (m_OutputFormat == null) {
      throw new Exception("No output instance format defined");
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
   * @exception Exception if no input structure has been defined
   */
  public int numPendingOutput() throws Exception {

    if (m_OutputFormat == null) {
      throw new Exception("No output instance format defined");
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

    System.err.println(filter.getClass().getName() 
                       + " in:" + data.numInstances());
    for (int i = 0; i < data.numInstances(); i++) {
      filter.input(data.instance(i));
    }
    filter.batchFinished();
    Instances newData = filter.outputFormat();
    Instance processed;
    while ((processed = filter.output()) != null) {
      newData.add(processed);
    }
    System.err.println(filter.getClass().getName() 
                       + " out:" + newData.numInstances());
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
	Enumeration enum = ((OptionHandler)filter).listOptions();
	while (enum.hasMoreElements()) {
	  Option option = (Option) enum.nextElement();
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
    if (filter.inputFormat(data)) {
      if (debug) {
	System.err.println("Getting output format");
      }
      output.println(filter.outputFormat().toString());
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
	  throw new Error("Filter didn't return true from inputFormat() "
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
	output.println(filter.outputFormat().toString());
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
	Enumeration enum = ((OptionHandler)filter).listOptions();
	while (enum.hasMoreElements()) {
	  Option option = (Option) enum.nextElement();
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
    if (filter.inputFormat(firstData)) {
      firstOutput.println(filter.outputFormat().toString());
      printedHeader = true;
    }
    
    // Pass all the instances to the filter
    while (firstData.readInstance(firstInput)) {
      if (filter.input(firstData.instance(0))) {
	if (!printedHeader) {
	  throw new Error("Filter didn't return true from inputFormat() "
			  + "earlier!");
	}
	firstOutput.println(filter.output().toString());
      }
      firstData.delete(0);
    }
    
    // Say that input has finished, and print any pending output instances
    if (filter.batchFinished()) {
      if (!printedHeader) {
	firstOutput.println(filter.outputFormat().toString());
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
      secondOutput.println(filter.outputFormat().toString());
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
	secondOutput.println(filter.outputFormat().toString());
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








