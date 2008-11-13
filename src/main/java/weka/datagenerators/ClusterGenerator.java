/*
 *    Generator.java
 *    Copyright (C) 2000 Gabi Schmidberger
 *
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

package weka.datagenerators;

import java.lang.Exception;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Enumeration;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Attribute;
import weka.core.FastVector;

/** 
 * Abstract class for cluster data generators.
 *
 * ------------------------------------------------------------------- <p>
 *
 * General options are: <p>
 *
 * -r string <br>
 * Name of the relation of the generated dataset. <br>
 * (default = name built using name of used generator and options) <p>
 *
 * -a num <br>
 * Number of attributes. (default = 2) <p>
 *
 * -k num <br>
 * Number of clusters. (default = 4) <p>
 *
 * -c <br>
 * Class Flag. If set, cluster is listed in extra class attribute.<p>
 *
 * -o filename<br>
 * writes the generated dataset to the given file using ARFF-Format.
 * (default = stdout).
 * 
 * ------------------------------------------------------------------- <p>
 *
 * Example usage as the main of a datagenerator called RandomGenerator:
 * <code> <pre>
 * public static void main(String [] args) {
 *   try {
 *     DataGenerator.makeData(new RandomGenerator(), argv);
 *   } catch (Exception e) {
 *     System.err.println(e.getMessage());
 *   }
 * }
 * </pre> </code> 
 * <p>
 *
 * ------------------------------------------------------------------ <p>
 *
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public abstract class ClusterGenerator implements Serializable {

  /** @serial Debugging mode */
  private boolean m_Debug = false;

  /** @serial The format for the generated dataset */
  private Instances m_Format = null;

  /** @serial Relation name the dataset should have */
  private String m_RelationName = "";

  /** @serial Number of attribute the dataset should have */
  protected int m_NumAttributes = 2;

  /** @serial Number of Clusters the dataset should have */
  protected int m_NumClusters = 4;

  /** @serial class flag  */
  private boolean m_ClassFlag = false;

  /** @serial Number of instances that should be produced into the dataset 
    * this number is by default m_NumExamples,
    * but can be reset by the generator 
    */
   private int m_NumExamplesAct = 0;

  /** @serial PrintWriter */
  private PrintWriter m_Output = null;

  /**
   * Initializes the format for the dataset produced. 
   * Must be called before the generateExample or generateExamples
   * methods are used.
   *
   * @return the format for the dataset 
   * @exception Exception if the generating of the format failed
   */

  abstract Instances defineDataFormat() throws Exception; 

  /**
   * Generates one example of the dataset. 
   *
   * @return the generated example
   * @exception Exception if the format of the dataset is not yet defined
   * @exception Exception if the generator only works with generateExamples
   * which means in non single mode
   */

  abstract Instance generateExample() throws Exception;

  /**
   * Generates all examples of the dataset. 
   *
   * @return the generated dataset
   * @exception Exception if the format of the dataset is not yet defined
   * @exception Exception if the generator only works with generateExample,
   * which means in single mode
   */

  abstract Instances generateExamples() throws Exception;

  /**
   * Generates a comment string that documentates the data generator.
   * By default this string is added at the beginning of the produced output
   * as ARFF file type, next after the options.
   * 
   * @return string contains info about the generated rules
   * @exception Exception if the generating of the documentation fails
   */
  abstract String generateStart () throws Exception;

  /**
   * Generates a comment string that documentates the data generator.
   * By default this string is added at the end of the produced output
   * as ARFF file type.
   * 
   * @return string contains info about the generated rules
   * @exception Exception if the generating of the documentation fails
   */
  abstract String generateFinished () throws Exception;


  /**
   * Return if single mode is set for the given data generator
   * mode depends on option setting and or generator type.
   * 
   * @return single mode flag
   * @exception Exception if mode is not set yet
   */
  abstract boolean getSingleModeFlag () throws Exception;


  /**
   * Sets the class flag, if class flag is set, 
   * the cluster is listed as class atrribute in an extra attribute.
   * @param classFlag the new class flag
   */
  public void setClassFlag(boolean classFlag) { m_ClassFlag = classFlag; }

  /**
   * Gets the class flag.
   * @return the class flag 
   */
  public boolean getClassFlag() {
    boolean b = m_ClassFlag;
    return m_ClassFlag; }

  /**
   * Sets the debug flag.
   * @param debug the new debug flag
   */
  public void setDebug(boolean debug) { 
    m_Debug = debug;
  }

  /**
   * Gets the debug flag.
   * @return the debug flag 
   */
  public boolean getDebug() { return m_Debug; }

  /**
   * Sets the relation name the dataset should have.
   * @param relationName the new relation name
   */
  public void setRelationName(String relationName) {
    if (relationName.length() == 0) {
      // build relationname 
      StringBuffer name = new StringBuffer(this.getClass().getName());
      String [] options = getGenericOptions();
      for (int i = 0; i < options.length; i++) {
	name = name.append(options[i].trim());
      }

      if (this instanceof OptionHandler) {
        options = ((OptionHandler)this).getOptions();
        for (int i = 0; i < options.length; i++) {
	  name = name.append(options[i].trim());
        }
      }
      m_RelationName = name.toString();
    } 
    else
      m_RelationName = relationName;
  }

  /**
   * Gets the relation name the dataset should have.
   * @return the relation name the dataset should have
   */
  public String getRelationName() { return m_RelationName; }

  /**
   * Sets the number of clusters the dataset should have.
   * @param numClusters the new number of clusters
   */
  public void setNumClusters(int numClusters) { m_NumClusters = numClusters; }

  /**
   * Gets the number of clusters the dataset should have.
   * @return the number of clusters the dataset should have
   */
  public int getNumClusters() { return m_NumClusters; }

  /**
   * Sets the number of attributes the dataset should have.
   * @param numAttributes the new number of attributes
   */
  public void setNumAttributes(int numAttributes) {
    m_NumAttributes = numAttributes;
  }

  /**
   * Gets the number of attributes that should be produced.
   * @return the number of attributes that should be produced
   */
  public int getNumAttributes() { return m_NumAttributes; }

  /**
   * Sets the number of examples the dataset should have.
   * @param numExamplesAct the new number of examples
   */
  public void setNumExamplesAct(int numExamplesAct) { 
    m_NumExamplesAct = numExamplesAct;
  }

  /**
   * Gets the number of examples the dataset should have.
   * @return the number of examples the dataset should have
   */
  public int getNumExamplesAct() { return m_NumExamplesAct; }

  /**
   * Sets the print writer.
   * @param newOutput the new print writer
   */
  public void setOutput(PrintWriter newOutput) {
    m_Output = newOutput;
  }

  /**
   * Gets the print writer.
   * @return print writer object
   */
  public PrintWriter getOutput() { return m_Output; }

  /**
   * Sets the format of the dataset that is to be generated. 
   * @param the new dataset format of the dataset 
   */
  protected void setFormat(Instances newFormat) {

    m_Format = new Instances(newFormat, 0);
  }

  /**
   * Gets the format of the dataset that is to be generated. 
   * @return the dataset format of the dataset
   */
  protected Instances getFormat() {

    Instances format = new Instances(m_Format, 0);
    return format;
  }

  /**
   * Returns a string representing the dataset in the instance queue.
   * @return the string representing the output data format
   */
  protected String toStringFormat() {
  
   if (m_Format == null)
     return "";
   return m_Format.toString();
  }

  /**
   * Calls the data generator.
   *
   * @param dataGenerator one of the data generators 
   * @param options options of the data generator
   * @exception Exception if there was an error in the option list
   */
  public static void makeData(ClusterGenerator generator, String [] options) 
    throws Exception {

    PrintWriter output = null;

    // read options /////////////////////////////////////////////////
    try {
      setOptions(generator, options);
    } catch (Exception ex) {
      String specificOptions = "";
      if (generator instanceof OptionHandler) {
        specificOptions = generator.listSpecificOptions(generator);
        }
      String genericOptions = listGenericOptions(generator);
      throw new Exception('\n' + ex.getMessage()
			  + specificOptions + genericOptions);
    }
    
    // define dataset format  ///////////////////////////////////////    
    // computes actual number of examples to be produced
    generator.setFormat(generator.defineDataFormat());

    // get print writer /////////////////////////////////////////////
    output = generator.getOutput();

    // output of options ////////////////////////////////////////////
    output.println("% ");
    output.print("% " + generator.getClass().getName() + " ");
    String [] outOptions = generator.getGenericOptions();
    for (int i = 0; i < outOptions.length; i++) {
      output.print(outOptions[i] + " ");
    }
    outOptions = ((OptionHandler) generator).getOptions();
    for (int i = 0; i < outOptions.length; i++) {
      output.print(outOptions[i] + " ");
    }
    output.println("\n%");

    // comment at beginning of ARFF File ////////////////////////////    
    String commentAtStart = generator.generateStart();
  
    if (commentAtStart.length() > 0) {
      output.println(commentAtStart);
    }

    // ask data generator which mode ////////////////////////////////
    boolean singleMode = generator.getSingleModeFlag();

    // start data producer   //////////////////////////////////////// 
    if (singleMode) {
      // output of  dataset header //////////////////////////////////
      output.println(generator.toStringFormat());
      for (int i = 0; i < generator.getNumExamplesAct(); i++)  {
        // over all examples to be produced
        Instance inst = generator.generateExample();
        output.println(inst);
        }
    } else { // generator produces all instances at once
      Instances dataset = generator.generateExamples();
      // output of  dataset ////////////////////////////////////////////
      output.println(dataset);      
    }
    // comment at end of ARFF File /////////////////////////////////////    
    String commentAtEnd = generator.generateFinished();
  
    if (commentAtEnd.length() > 0) {
      output.println(commentAtEnd);
    }
    
    if (output != null) {
      output.close();
    }
  }

  /**
   * Makes a string with the options of the specific data generator.
   *
   * @param generator the datagenerator that is used
   * @return string with the options of the data generator used
   */
  private String listSpecificOptions(ClusterGenerator generator) { 

    String optionString = "";
    if (generator instanceof OptionHandler) {
      optionString += "\nData Generator options:\n\n";
      Enumeration enu = ((OptionHandler)generator).listOptions();
      while (enu.hasMoreElements()) {
        Option option = (Option) enu.nextElement();
	optionString += option.synopsis() + '\n'
	    + option.description() + "\n";
	}
    }
    return optionString;
  }

  /**
   * Sets the generic options and specific options.
   *
   * @param generator the data generator used 
   * @param options the generic options and the specific options
   * @exception Exception if help request or any invalid option
   */
  private static void setOptions(ClusterGenerator generator,
                                 String[] options) throws Exception { 

    boolean helpRequest = false;
    String outfileName = new String(""); 
    PrintWriter output;

    // get help 
    helpRequest = Utils.getFlag('h', options);

    if (Utils.getFlag('d', options)) { generator.setDebug(true); }

    // get relationname
    String relationName = Utils.getOption('r', options); 
    // set relation name at end of method after all options are set

    // get outputfilename
    outfileName = Utils.getOption('o', options); 

    // get num of clusters
    String num = Utils.getOption('k', options);
    if (num.length() != 0)
      generator.setNumClusters(Integer.parseInt(num));

    // get class flag
    if (Utils.getFlag('c', options))
      generator.setClassFlag(true);

    // get num of attributes
    String numAttributes = Utils.getOption('a', options);
    if (numAttributes.length() != 0)
      generator.setNumAttributes(Integer.parseInt(numAttributes));
      
    if (generator instanceof OptionHandler) {
      ((OptionHandler)generator).setOptions(options);
      }

    // all options are set, now set relation name
    generator.setRelationName(relationName);

    // End read options
    Utils.checkForRemainingOptions(options);

    if (helpRequest) {
      throw new Exception("Help requested.\n");
    }

    if (outfileName.length() != 0) {
      output = new PrintWriter(new FileOutputStream(outfileName));
    } else { 
      output = new PrintWriter(System.out);
    }
    generator.setOutput(output);
  }

  /**
   * Method for listing generic options.
   *
   * @param generator the data generator
   * @return string with the generic data generator options
   */
  private static String listGenericOptions (ClusterGenerator generator) {

    String genericOptions = "\nGeneral options:\n\n"
	+ "-h\n"
	+ "\tGet help on available options.\n"
	+ "-r <relation name>\n"
	+ "\tThe name of the relation for the produced dataset.\n"
	+ "-a <number of attributes>\n"
	+ "\tThe number of attributes for the produced dataset.\n"
	+ "-k <number of clusters>\n"
	+ "\tThe number of clusters the dataset is produced in.\n"
	+ "-c \n"
	+ "\tThe class flag, if set, the cluster is listed in the class " 
        + "attribute.\n"
	+ "-o <file>\n"
	+ "\tThe name of the file output instances will be written to.\n"
	+ "\tIf not supplied the instances will be written to stdout.\n";
    return genericOptions;
  }

  /**
   * Gets the current generic settings of the datagenerator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  private String [] getGenericOptions() {
    
    String [] options = new String [10];
    int i = 0;
    String name = getRelationName();
    if (name.length() > 0) {
      options[i++] = "-r";
      options[i++] = "" + getRelationName();
    }
    options[i++] = "-a"; options[i++] = "" + getNumAttributes();
    options[i++] = "-k"; options[i++] = "" + getNumClusters();
    if (getClassFlag()) {
      options[i++] = "-c"; options[i++] = "";
    }
    while (i < options.length) {
      options[i++] = "";
    }
    return options;
  }
}


