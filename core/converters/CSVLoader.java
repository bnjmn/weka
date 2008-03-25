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
 *    CSVLoader.java
 *    Copyright (C) 2000 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 <!-- globalinfo-start -->
 * Reads a source that is in comma separated or tab separated format. Assumes that the first row in the file determines the number of and names of the attributes.
 * <p/>
 <!-- globalinfo-end -->
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.13.2.4 $
 * @see Loader
 */
public class CSVLoader 
  extends AbstractFileLoader 
  implements BatchConverter {

  /** for serialization */
  static final long serialVersionUID = 5607529739745491340L;
  
  /** the file extension */
  public static String FILE_EXTENSION = ".csv";

  /**
   * A list of hash tables for accumulating nominal values during parsing.
   */
  private FastVector m_cumulativeStructure;

  /**
   * Holds instances accumulated so far
   */
  private FastVector m_cumulativeInstances;
  
  /** the data collected from an InputStream */
  private StringBuffer m_StreamBuffer;
  
  /**
   * default constructor
   */
  public CSVLoader() {
    // No instances retrieved yet
    setRetrieval(NONE);
  }

  /**
   * Get the file extension used for arff files
   *
   * @return the file extension
   */
  public String getFileExtension() {
    return FILE_EXTENSION;
  }

  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return "CSV data files";
  }

  /**
   * Gets all the file extensions used for this type of file
   *
   * @return the file extensions
   */
  public String[] getFileExtensions() {
    return new String[]{getFileExtension()};
  }

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads a source that is in comma separated or tab separated format. "
      +"Assumes that the first row in the file determines the number of "
      +"and names of the attributes.";
  }
  
  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied Stream object.
   *
   * @param input the input stream
   * @exception IOException if an error occurs
   */
  public void setSource(InputStream input) throws IOException {
    BufferedReader	reader;
    String		line;
    
    m_structure    = null;
    m_sourceFile   = null;
    m_File         = null;

    m_StreamBuffer = new StringBuffer();
    reader         = new BufferedReader(new InputStreamReader(input));
    while ((line = reader.readLine()) != null)
      m_StreamBuffer.append(line + "\n");
  }

  /**
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the source file.
   * @exception IOException if an error occurs
   */
  public void setSource(File file) throws IOException {
    super.setSource(file);
    
    m_StreamBuffer = null;
  }

  /**
   * Determines and returns (if possible) the structure (internally the 
   * header) of the data set as an empty set of instances.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if an error occurs
   */
  public Instances getStructure() throws IOException {
    if ((m_sourceFile == null) && (m_StreamBuffer == null)) {
      throw new IOException("No source has been specified");
    }

    if (m_structure == null) {
      try {
	BufferedReader br;
	if (m_StreamBuffer != null)
	  br = new BufferedReader(new StringReader(m_StreamBuffer.toString()));
	else
	  br = new BufferedReader(new FileReader(m_sourceFile));
	StreamTokenizer st = new StreamTokenizer(br);
	initTokenizer(st);
	readStructure(st);
      } catch (FileNotFoundException ex) {
      }
    }
    
    return m_structure;
  }

  /**
   * reads the structure
   * 
   * @param st the stream tokenizer to read from
   * @throws IOException if reading fails
   */
  private void readStructure(StreamTokenizer st) throws IOException {
    readHeader(st);
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
    if ((m_sourceFile == null) && (m_StreamBuffer == null)) {
      throw new IOException("No source has been specified");
    }
    BufferedReader br;
    if (m_sourceFile != null) {
      setSource(m_sourceFile);
      br = new BufferedReader(new FileReader(m_sourceFile));
    }
    else {
      br = new BufferedReader(new StringReader(m_StreamBuffer.toString()));
    }
    StreamTokenizer st = new StreamTokenizer(br);
    initTokenizer(st);
    readStructure(st);
    
    st.ordinaryChar(',');
    st.ordinaryChar('\t');
    
    m_cumulativeStructure = new FastVector(m_structure.numAttributes());
    for (int i = 0; i < m_structure.numAttributes(); i++) {
      m_cumulativeStructure.addElement(new Hashtable());
    }
    

    // Instances result = new Instances(m_structure);
    m_cumulativeInstances = new FastVector();
    FastVector current;
    while ((current = getInstance(st)) != null) {
      m_cumulativeInstances.addElement(current);
    }
    br.close();
    // now determine the true structure of the data set
    FastVector atts = new FastVector(m_structure.numAttributes());
    for (int i = 0; i < m_structure.numAttributes(); i++) {
      String attname = m_structure.attribute(i).name();
      Hashtable tempHash = ((Hashtable)m_cumulativeStructure.elementAt(i));
      if (tempHash.size() == 0) {
	atts.addElement(new Attribute(attname));
      } else {
	FastVector values = new FastVector(tempHash.size());
	// add dummy objects in order to make the FastVector's size == capacity
	for (int z = 0; z < tempHash.size(); z++) {
	  values.addElement("dummy");
	}
	Enumeration e = tempHash.keys();
	while (e.hasMoreElements()) {
	  Object ob = e.nextElement();
	  //	  if (ob instanceof Double) {
	  int index = ((Integer)tempHash.get(ob)).intValue();
	  values.setElementAt(new String(ob.toString()), index);
	  //	  }
	}
	atts.addElement(new Attribute(attname, values));
      }
    }

    // make the instances
    String relationName;
    if (m_sourceFile != null)
      relationName = (m_sourceFile.getName()).replaceAll("\\.[cC][sS][vV]$","");
    else
      relationName = "stream";
    Instances dataSet = new Instances(relationName, 
				      atts, 
				      m_cumulativeInstances.size());

    for (int i = 0; i < m_cumulativeInstances.size(); i++) {
      current = ((FastVector)m_cumulativeInstances.elementAt(i));
      double [] vals = new double[dataSet.numAttributes()];
      for (int j = 0; j < current.size(); j++) {
	Object cval = current.elementAt(j);
	if (cval instanceof String) {
	  if (((String)cval).compareTo("'?'") == 0) {
	    vals[j] = Instance.missingValue();
	  } else {
	    if (!dataSet.attribute(j).isNominal()) {
	      System.err.println("Wrong attribute type!!!");
	      System.exit(1);
	    }
	    // find correct index
	    Hashtable lookup = (Hashtable)m_cumulativeStructure.elementAt(j);
	    int index = ((Integer)lookup.get(cval)).intValue();
	    vals[j] = (double)index;
	  }
	} else if (dataSet.attribute(j).isNominal()) {
	  // find correct index
	  Hashtable lookup = (Hashtable)m_cumulativeStructure.elementAt(j);
	  int index = ((Integer)lookup.get(cval)).intValue();
	  vals[j] = (double)index;
	} else {
	  vals[j] = ((Double)cval).doubleValue();
	}
      }
      dataSet.add(new Instance(1.0, vals));
    }
    m_structure = new Instances(dataSet, 0);
    setRetrieval(BATCH);
    m_cumulativeStructure = null; // conserve memory
    return dataSet;
  }

  /**
   * CSVLoader is unable to process a data set incrementally.
   *
   * @param structure ignored
   * @return never returns without throwing an exception
   * @exception IOException always. CSVLoader is unable to process a data
   * set incrementally.
   */
  public Instance getNextInstance(Instances structure) throws IOException {
    throw new IOException("CSVLoader can't read data sets incrementally.");
  }

  /**
   * Attempts to parse a line of the data set.
   *
   * @param tokenizer the tokenizer
   * @return a FastVector containg String and Double objects representing
   * the values of the instance.
   * @exception IOException if an error occurs
   *
   * <pre><jml>
   *    private_normal_behavior
   *      requires: tokenizer != null;
   *      ensures: \result  != null;
   *  also
   *    private_exceptional_behavior
   *      requires: tokenizer == null
   *                || (* unsucessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  private FastVector getInstance(StreamTokenizer tokenizer) 
    throws IOException {

    FastVector current = new FastVector();

    // Check if end of file reached.
    ConverterUtils.getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return null;
    }
    boolean first = true;
    boolean wasSep;

    while (tokenizer.ttype != StreamTokenizer.TT_EOL &&
	   tokenizer.ttype != StreamTokenizer.TT_EOF) {
      
      // Get next token
      if (!first) {
	ConverterUtils.getToken(tokenizer);
      }

      if (tokenizer.ttype == ',' || tokenizer.ttype == '\t' || 
	  tokenizer.ttype == StreamTokenizer.TT_EOL) {
	current.addElement("'?'");
	wasSep = true;
      } else if (tokenizer.ttype == '?') {
        wasSep = false;
        current.addElement(new String("'?'"));
      } else {
	wasSep = false;
	// try to parse as a number
	try {
	  double val = Double.valueOf(tokenizer.sval).doubleValue();
	  current.addElement(new Double(val));
	} catch (NumberFormatException e) {
	  // otherwise assume its an enumerated value
	  current.addElement(new String(tokenizer.sval));
	}
      }
      
      if (!wasSep) {
	ConverterUtils.getToken(tokenizer);
      }
      first = false;
    }
    
    // check number of values read
    if (current.size() != m_structure.numAttributes()) {
      ConverterUtils.errms(tokenizer, 
			   "wrong number of values. Read "+current.size()
			   +", expected "+m_structure.numAttributes());
    }

    // check for structure update
    try {
      checkStructure(current);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return current;
  }

  /**
   * Checks the current instance against what is known about the structure
   * of the data set so far. If there is a nominal value for an attribute
   * that was beleived to be numeric then all previously seen values for this
   * attribute are stored in a Hashtable.
   *
   * @param current a <code>FastVector</code> value
   * @exception Exception if an error occurs
   *
   * <pre><jml>
   *    private_normal_behavior
   *      requires: current != null;
   *  also
   *    private_exceptional_behavior
   *      requires: current == null
   *                || (* unrecognized object type in current *);
   *      signals: (Exception);
   * </jml></pre>
   */
  private void checkStructure(FastVector current) throws Exception {
    if (current == null) {
      throw new Exception("current shouldn't be null in checkStructure");
    }
    for (int i = 0; i < current.size(); i++) {
      Object ob = current.elementAt(i);
      if (ob instanceof String) {
	if (((String)ob).compareTo("'?'") == 0) {
	} else {
	  Hashtable tempHash = (Hashtable)m_cumulativeStructure.elementAt(i);
	  if (!tempHash.containsKey(ob)) {
	    // may have found a nominal value in what was previously thought to
	    // be a numeric variable.
	    if (tempHash.size() == 0) {
	      for (int j = 0; j < m_cumulativeInstances.size(); j++) {
		FastVector tempUpdate = 
		  ((FastVector)m_cumulativeInstances.elementAt(j));
		Object tempO = tempUpdate.elementAt(i);
		if (tempO instanceof String) {
		  // must have been a missing value
		} else {
		  if (!tempHash.containsKey(tempO)) {
		    tempHash.put(new Double(((Double)tempO).doubleValue()), 
				 new Integer(tempHash.size()));
		  }
		}
	      }
	    }
	    int newIndex = tempHash.size();
	    tempHash.put(ob, new Integer(newIndex));
	  }
	}
      } else if (ob instanceof Double) {
	Hashtable tempHash = (Hashtable)m_cumulativeStructure.elementAt(i);
	if (tempHash.size() != 0) {
	  if (!tempHash.containsKey(ob)) {
	    int newIndex = tempHash.size();
	    tempHash.put(new Double(((Double)ob).doubleValue()), 
				    new Integer(newIndex));
	  }
	}
      } else {
	throw new Exception("Wrong object type in checkStructure!");
      }
    }
  }

  /**
   * Assumes the first line of the file contains the attribute names.
   * Assumes all attributes are real (Reading the full data set with
   * getDataSet will establish the true structure).
   *
   * @param tokenizer a <code>StreamTokenizer</code> value
   * @exception IOException if an error occurs
   *
   * <pre><jml>
   *    private_normal_behavior
   *      requires: tokenizer != null;
   *      modifiable: m_structure;
   *      ensures: m_structure != null;
   *  also
   *    private_exceptional_behavior
   *      requires: tokenizer == null
   *                || (* unsucessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  private void readHeader(StreamTokenizer tokenizer) throws IOException {
   
    FastVector attribNames = new FastVector();
    ConverterUtils.getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      ConverterUtils.errms(tokenizer,"premature end of file");
    }

    while (tokenizer.ttype != StreamTokenizer.TT_EOL) {
      attribNames.addElement(new Attribute(tokenizer.sval));
      ConverterUtils.getToken(tokenizer);
    }
    String relationName;
    if (m_sourceFile != null)
      relationName = (m_sourceFile.getName()).replaceAll("\\.[cC][sS][vV]$","");
    else
      relationName = "stream";
    m_structure = new Instances(relationName, attribNames, 0);
  }

  /**
   * Initializes the stream tokenizer
   *
   * @param tokenizer the tokenizer to initialize
   */
  private void initTokenizer(StreamTokenizer tokenizer) {
    tokenizer.resetSyntax();         
    tokenizer.whitespaceChars(0, (' '-1));    
    tokenizer.wordChars(' ','\u00FF');
    tokenizer.whitespaceChars(',',',');
    tokenizer.whitespaceChars('\t','\t');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.eolIsSignificant(true);
  }

  /**
   * Main method.
   *
   * @param args should contain the name of an input file.
   */
  public static void main(String [] args) {
    runFileLoader(new CSVLoader(), args);
  }
}
