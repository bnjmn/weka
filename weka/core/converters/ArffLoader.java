/*
 *    ArffLoader.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.core.converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import weka.core.Instance;
import weka.core.Instances;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Reads a source that is in arff text format.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 * @see Loader
 */
public class ArffLoader extends AbstractLoader {

  /**
   * Holds the determined structure (header) of the data set.
   */
  //@ protected depends: model_structureDetermined -> m_structure;
  //@ protected represents: model_structureDetermined <- (m_structure != null);
  protected Instances m_structure = null;

  /**
   * The reader for the source file.
   */
  private transient Reader m_sourceReader = null;

  /**
   * Resets the Loader ready to read a new data set
   */
  public void reset() {

    m_structure = null;
    m_sourceReader = null;
    setRetrieval(NONE);
  }

  /**
   * Resets the Loader object and sets the source of the data set to be 
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

    try {
      setSource(new FileInputStream(file));
    } catch (FileNotFoundException ex) {
      throw new IOException("File not found");
    }
  }

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied InputStream.
   *
   * @param in the source InputStream.
   * @exception IOException always thrown.
   */
  public void setSource(InputStream in) throws IOException {

    m_sourceReader = new BufferedReader(new InputStreamReader(in));
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
      throw new IOException("No source has been specified");
    }

    if (m_structure == null) {
      try {
	m_structure = new Instances(m_sourceReader, 1);
      } catch (Exception ex) {
	throw new IOException("Unable to determine structure as arff.");
      }
    }

    return new Instances(m_structure, 0);
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
      throw new IOException("No source has been specified");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(BATCH);
    

    // Read all instances
    // XXX This is inefficient because readInstance creates a new 
    // StringTokenizer each time: This will be fixed once all arff reading
    // is moved out of Instances and into this Loader.
    while (m_structure.readInstance(m_sourceReader));
    
    return m_structure;
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
    if (getRetrieval() == BATCH) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(INCREMENTAL);

    if (!m_structure.readInstance(m_sourceReader)) {
      return null;
    }
   
    Instance current = m_structure.instance(0);
    m_structure.delete(0);
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
	ArffLoader atf = new ArffLoader();
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
      System.err.println("Usage:\n\tArffLoader <file.arff>\n");
    }
  }
}
