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
 *    AbstractSaver.java
 *    Copyright (C) 2004 Richard Kirkby, Stefan Mutter
 *
 */

package weka.core.converters;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.OptionHandler;
import weka.core.FastVector;
import weka.core.Option;
import weka.core.Utils;

import java.util.Enumeration;
import java.io.*;

/**
 * Abstract class gives default implementation of setSource 
 * methods. All other methods must be overridden.
 *
 * Valid options:
 *
 * -i input arff file <br>
 * The input filw in arff format. <p>
 *
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient. If no output file is given, Saver tries to use standard out. <p>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public abstract class AbstractSaver implements Saver, OptionHandler, FileSourcedConverter{
  
  /** The retrieval modes */
  protected static final int NONE = 0;
  protected static final int BATCH = 1;
  protected static final int INCREMENTAL = 2;
  
  /** The write modes */
  protected static final int WRITE = 0;
  protected static final int WAIT = 1;
  protected static final int CANCEL = 2;
  protected static final int STRUCTURE_READY = 3;
  
  
  /** The destination file. */
  private File m_outputFile;
  
  /** The writer. */
  private BufferedWriter m_writer;
  
  /** The file extension of the destination file. */
  private String FILE_EXTENSION;
  
  /** The instances that should be stored */
  private Instances m_instances;

  /** The current retrieval mode */
  protected int m_retrieval;

   /** The current write mode */
  private int m_writeMode;
  
  /**
   * resets the options
   *
   */
  public void resetOptions(){
      
     m_instances = null;
     m_outputFile = null;
     m_writer = null;
     m_writeMode = WAIT;
  }
  
  
  /**
   * Sets the retrieval mode.
   *
   * @param mode the retrieval mode
   */
  protected void setRetrieval(int mode) {

    m_retrieval = mode;
  }

  /**
   * Gets the retrieval mode.
   *
   * @return the retrieval mode
   */
  protected int getRetrieval() {

    return m_retrieval;
  }
  
  
  /**
   * Sets the write mode.
   *
   * @param mode the write mode
   */
  protected void setWriteMode(int mode) {

    m_writeMode = mode;
  }

  /**
   * Gets the write mode.
   *
   * @return the write mode
   */
  protected int getWriteMode() {

    return m_writeMode;
  }
  
  
  /**
   * Sets instances that should be stored.
   *
   * @param instances the instances
   */
  public void setInstances(Instances instances){
   
      if(m_retrieval == INCREMENTAL){
          if(setStructure(instances) == CANCEL)
              cancel();
      }
      else  
        m_instances = instances;
  }
  
  /**
   * Gets instances that should be stored.
   *
   * @return the instances
   */
  public Instances getInstances(){
   
      return m_instances;
  }
 
  /**
   * Gets the writer
   *
   * @return the BufferedWriter
   */
  public BufferedWriter getWriter(){
   
      return m_writer;
  }
  
  /** Sets the writer to null. */  
  public void resetWriter(){
   
      m_writer = null;
  }
  
  /**
   * Gets ihe file extension.
   *
   * @return the file extension as a string.
   */
  public String getFileExtension(){
   
      return FILE_EXTENSION;
  }
  
  
  /**
   * Sets ihe file extension.
   *
   * @param ext the file extension as a string startin with '.'.
   */ 
  public void setFileExtension(String ext){
   
      FILE_EXTENSION = ext;
  }
  
  
  /**
   * Gets the destination file.
   *
   * @return the destination file.
   */
  public File getFile(){
  
      return m_outputFile;
  }
 
  /** Sets the destination file.
   * @param outputFile the destination file.
   * @throws IOException throws an IOException if file cannot be set
   */
  public void setFile(File outputFile) throws IOException  {
      
      m_outputFile = outputFile;
      
  }

  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    FastVector newVector = new FastVector(2);

    newVector.addElement(new Option("The input file", "i", 1, 
				    "-i <the input file>"));
    
    newVector.addElement(new Option("The output file", "o", 1, 
				    "-o <the output file>"));
    
    return newVector.elements();
  }

 
/**
   * Parses a given list of options. Valid option is:<p>
   *   
   * -i input arff file <br>
   * The input filw in arff format. <p>
   *
   * -o the output file <br>
   * The output file. The prefix of the output file is sufficient. If no output file is given, Saver tries to use standard out. <p>
   *
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    String outputString = Utils.getOption('o', options);
    String inputString = Utils.getOption('i', options);
    
    ArffLoader loader = new ArffLoader();
    
    resetOptions();
    
    if(inputString.length() != 0){
        try{
            File input = new File(inputString);
            loader.setFile(input);
            m_instances = loader.getDataSet();
        } catch(Exception ex){
            throw new IOException("No data set loaded. Data set has to be arff format.");
        }
    }
    else
        throw new IOException("No data set to save.");
    if (outputString.length() != 0){ 
        //add appropriate file extension
        if(!outputString.endsWith(FILE_EXTENSION)){
            if(outputString.lastIndexOf('.') != -1)
                outputString = (outputString.substring(0,outputString.lastIndexOf('.'))) + FILE_EXTENSION;
            else
                outputString = outputString + FILE_EXTENSION;
        }
        try{
            File output = new File(outputString);
            setFile(output);
        } catch(Exception ex){
            throw new IOException("Cannot create output file. Standard out is used.");
        } finally{
            setDestination(m_outputFile);
        }
    }

  }

  /**
   * Gets the current settings of the PredictiveApriori object.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;
    if(m_outputFile != null){
        options[current++] = "-o"; options[current++] = "" + m_outputFile;
    }
    else{
        options[current++] = "-o"; options[current++] = "";
    }
    if(m_instances != null){
        options[current++] = "-i"; options[current++] = "" + m_instances.relationName();
    }
    else{
        options[current++] = "-i"; options[current++] = "";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }


  
  /**
   * Sets the destination file (and directories if necessary).
   *
   * @param file the File
   * @exception IOException always
   */
  public void setDestination(File file) throws IOException {

    boolean success = false;
    String out = file.getAbsolutePath();
    
    if(m_outputFile != null){
        try{
            if(file.exists())
                    throw new IOException("File already exists.");
            if(out.lastIndexOf(File.separatorChar) == -1){
                success = file.createNewFile();
            }
            else{
                String outPath = out.substring(0,out.lastIndexOf(File.separatorChar));
                File dir = new File(outPath);
                if(dir.exists())
                    success = file.createNewFile();
                else{
                    dir.mkdirs();
                    success = file.createNewFile();
                }
            }
            if(success){ 
                m_outputFile = file;
                setDestination(new FileOutputStream(m_outputFile));
            }
        } catch(Exception ex){
            throw new IOException("Cannot create a new output file. Standard out is used.");
        } finally{
            if(!success){
                System.err.println("Cannot create a new output file. Standard out is used.");
                m_outputFile = null; //use standard out
            }
        }
    }
  }
  
  
  /** Sets the destination output stream.
   * @param output the output stream.
   * @throws IOException throws an IOException if destination cannot be set
   */
  public void setDestination(OutputStream output) throws IOException {

    m_writer = new BufferedWriter(new OutputStreamWriter(output));
  }
  
  /** Sets the strcuture of the instances for the first step of incremental saving.
   * The instances only need to have a header.
   * @param headerInfo an instances object.
   * @return the appropriate write mode
   */  
  public int setStructure(Instances headerInfo){
  
      if(m_writeMode == WAIT && headerInfo != null){
        m_instances = headerInfo;
        m_writeMode = STRUCTURE_READY;
      }
      else{
        m_instances = null;
        if(m_writeMode != WAIT)
            System.err.println("A structure cannot be set up during an active incremental saving process. Structure rejected. Saving will be stopped.");
        m_writeMode = CANCEL;
      }
      return m_writeMode;
  }
  
  /** Cancels the incremental saving process. */  
  public void cancel(){
  
      if(m_writeMode == CANCEL)
        resetOptions();
  }
  
  /** Method for incremental saving.
   * Standard behaviour: no incremental saving is possible, therefore throw an
   * IOException.
   * @param i the instance to be saved
   * @throws IOException IOEXception if the instance acnnot be written to the specified destination
   */  
  public void writeIncremental(Instance i) throws IOException{
  
      throw new IOException("No Incremental saving possible.");
  }
  
  
  /*
   * To be overridden.
   *
   *
   * @throws IOException throws IOException if writing a file in bathc mode is not possible
   */  
  public abstract void writeBatch() throws IOException;

    /**
   * to be pverridden
   *
   * @return the file etype description.
   */
  public abstract String getFileDescription();
  
  
}
