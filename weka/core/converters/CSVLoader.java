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
 *    CsvToArff.java
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
import java.util.Hashtable;
import java.util.Enumeration;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import java.io.StreamTokenizer;
import java.lang.String;

/**
 * Reads a text file that is comma or tab delimited..
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.9.2.5 $
 * @see Loader
 */
public class CSVLoader extends AbstractLoader 
implements FileSourcedConverter, BatchConverter {

  public static String FILE_EXTENSION = ".csv";

  protected String m_File = 
    (new File(System.getProperty("user.dir"))).getAbsolutePath();

  /**
   * Holds the determined structure (header) of the data set.
   */
  //@ protected depends: model_structureDetermined -> m_structure;
  //@ protected represents: model_structureDetermined <- (m_structure != null);
  protected Instances m_structure = null;

  /**
   * Holds the source of the data set.
   */
  //@ protected depends: model_sourceSupplied -> m_sourceFile;
  //@ protected represents: model_sourceSupplied <- (m_sourceFile != null);
  protected File m_sourceFile = null;

  /**
   * Describe variable <code>m_tokenizer</code> here.
   */
  //  private StreamTokenizer m_tokenizer = null;

  /**
   * A list of hash tables for accumulating nominal values during parsing.
   */
  private FastVector m_cumulativeStructure;

  /**
   * Holds instances accumulated so far
   */
  private FastVector m_cumulativeInstances;
  
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
   * get the File specified as the source
   *
   * @return the source file
   */
  public File retrieveFile() {
    return new File(m_File);
  }

  /**
   * sets the source File
   *
   * @param file the source file
   * @exception IOException if an error occurs
   */
  public void setFile(File file) throws IOException {
    m_File = file.getAbsolutePath();
    setSource(file);
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
   * Resets the loader ready to read a new data set
   */
  public void reset() {
    m_structure = null;
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

    m_sourceFile = file;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      br.close();
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
    if (m_sourceFile == null) {
      throw new IOException("No source has been specified");
    }

    if (m_structure == null) {
      try {
	BufferedReader br = new BufferedReader(new FileReader(m_sourceFile));
     
	// assumes that the first line of the file is the header
	/*m_tokenizer = new StreamTokenizer(br);
	initTokenizer(m_tokenizer);
	readHeader(m_tokenizer); */
	StreamTokenizer st = new StreamTokenizer(br);
	initTokenizer(st);
	readStructure(st);
        br.close();
      } catch (FileNotFoundException ex) {
      }
    }
    
    return m_structure;
  }

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
    if (m_sourceFile == null) {
      throw new IOException("No source has been specified");
    }
    //    m_sourceReader.close();
    setSource(m_sourceFile);
    BufferedReader br = new BufferedReader(new FileReader(m_sourceFile));
    //    getStructure();
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
    String relationName = (m_sourceFile.getName()).replaceAll("\\.[cC][sS][vV]$","");
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
	/* // Check if token is valid.
	if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
	  errms(tokenizer,"not a valid value");
	  }*/

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
    String relationName = (m_sourceFile.getName()).replaceAll("\\.[cC][sS][vV]$","");
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
    //    tokenizer.ordinaryChar(',');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    //    tokenizer.ordinaryChar('{');
    //    tokenizer.ordinaryChar('}');
    tokenizer.eolIsSignificant(true);
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
	CSVLoader atf = new CSVLoader();
	atf.setSource(inputfile);
	System.out.println(atf.getDataSet());
      } catch (Exception ex) {
	ex.printStackTrace();
	}
    } else {
      System.err.println("Usage:\n\tCSVLoader <file.csv>\n");
    }
  }
}

