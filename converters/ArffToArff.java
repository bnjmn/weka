/*
 *    ArffToArff.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.converters;

import java.io.*;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Reads a text file that is in arff format.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 * @see Converter
 * @see Serializable
 */
public class ArffToArff implements Converter, Serializable {

  /**
   * Holds the determined structure (header) of the data set.
   */
  //@ protected depends: model_structureDetermined -> m_structure;
  //@ protected represents: model_structureDetermined <- (m_structure != null);
  protected Instances m_structure = null;

  /**
   * Used for reading instances incrementally.
   */
  private Instances m_tempHeader = null;

  /**
   * Holds the source of the data set.
   */
  //@ protected depends: model_sourceSupplied -> m_sourceFile;
  //@ protected represents: model_sourceSupplied <- (m_sourceFile != null);
  protected File m_sourceFile = null;

  /**
   * The reader for the source file.
   */
  private transient Reader m_sourceReader = null;

  /**
   * Resets the converter ready to read a new data set
   */
  public void reset() {
    m_structure = null;
  }

  /**
   * Resets the Converter object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the source file.
   * @exception IOException if an error occurs
   */
  public void setSource(File file) throws IOException {
    reset();

    if (file == null) {
      throw new IOException("Source file object is null!");
    }

    m_sourceFile = file;
    try {
      m_sourceReader = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException ex) {
      throw new IOException("File not found");
    }
  }

  /**
   * Determines and returns (if possible) the structure (internally the 
   * header) of the data set as an empty set of instances.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if an error occurs
   */
  public Instances getStructure() throws IOException {
    if (m_sourceReader == null) {
      throw new IOException("No source file specified");
    }

    if (m_structure == null) {
      try {
	m_tempHeader = new Instances(m_sourceReader, 1);
	m_structure = new Instances(m_tempHeader);
      } catch (Exception ex) {
	throw new IOException("Unable to determine structure as arff.");
      }
    }

    return m_structure;
  }

  /**
   * Return the full data set. If the structure hasn't yet been determined
   * by a call to getStructure then method should do so before processing
   * the rest of the data set.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if there is no source or parsing fails
   */
  public Instances getDataSet() throws IOException {
    if (m_sourceReader == null) {
      throw new IOException("No source file specified");
    }
    m_sourceReader.close();
    setSource(m_sourceFile);
    Instances result = null;

    try {
      result = new Instances(m_sourceReader);
      m_structure = new Instances(result, 0);
    } catch (Exception ex) {
      throw new IOException(ex.toString());
    }

    return result;
  }

  /**
   * Read the data set incrementally---get the next instance in the data 
   * set or returns null if there are no
   * more instances to get. If the structure hasn't yet been 
   * determined by a call to getStructure then method should do so before
   * returning the next instance in the data set.
   *
   * @return the next instance in the data set as an Instance object or null
   * if there are no more instances to be read
   * @exception IOException if there is an error during parsing
   */
  public Instance getNextInstance() throws IOException {

    if (m_structure == null) {
      getStructure();
    }

    if (!m_tempHeader.readInstance(m_sourceReader)) {
      return null;
    }
   
    Instance current = m_tempHeader.instance(0);
    m_tempHeader.delete(0);
    return current;
  }

  /**
   * Main method.
   *
   * @param args should contain the name of an input file.
   */
  public static void main(String [] args) {
    if (args.length > 0) {
      File inputfile;
      inputfile = new File(args[0]);
      try {
	ArffToArff atf = new ArffToArff();
	atf.setSource(inputfile);
	System.out.println(atf.getStructure());
	Instance temp;
	do {
	  temp = atf.getNextInstance();
	  if (temp != null) {
	    System.out.println(temp);
	  }
	} while (temp != null);
      } catch (Exception ex) {
	ex.printStackTrace();
	}
    } else {
      System.err.println("Usage:\n\tArffToArff <file.arff>\n");
    }
  }
}
