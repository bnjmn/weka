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
 *    C45Loader.java
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
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.Utils;
import java.io.StreamTokenizer;

/**
 * Reads C4.5 input files. Takes a filestem or filestem with .names or .data
 * appended. Assumes that both <filestem>.names and <filestem>.data exist
 * in the directory of the supplied filestem.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 * @see Loader
 */
public class C45Loader extends AbstractLoader 
implements FileSourcedConverter, BatchConverter, IncrementalConverter {

  public static String FILE_EXTENSION = ".names";

  protected String m_File = 
    (new File(System.getProperty("user.dir"))).getAbsolutePath();
  
  /**
   * Holds the determined structure (header) of the data set.
   */
  //@ protected depends: model_structureDetermined -> m_structure;
  //@ protected represents: model_structureDetermined <- (m_structure != null);
  protected Instances m_structure = null;

  /**
   * Holds the source of the data set. In this case the names file of the
   * data set. m_sourceFileData is the data file.
   */
  //@ protected depends: model_sourceSupplied -> m_sourceFile;
  //@ protected represents: model_sourceSupplied <- (m_sourceFile != null);
  protected File m_sourceFile = null;

  /**
   * Describe variable <code>m_sourceFileData</code> here.
   */
  private File m_sourceFileData = null;

  /**
   * Reader for names file
   */
  private transient Reader m_namesReader = null;

  /**
   * Reader for data file
   */
  private transient Reader m_dataReader = null;

  /**
   * Holds the filestem.
   */
  private String m_fileStem;

  /**
   * Number of attributes in the data (including ignore and label attributes).
   */
  private int m_numAttribs;

  /**
   * Which attributes are ignore or label. These are *not* included in the
   * arff representation.
   */
  private boolean [] m_ignore;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads a file that is C45 format. Can take a filestem or filestem "
      +"with .names or .data appended. Assumes that path/<filestem>.names and "
      +"path/<filestem>.data exist and contain the names and data "
      +"respectively.";
  }
  
  /**
   * Resets the Loader ready to read a new data set
   */
  public void reset() throws Exception {
    m_structure = null;
    setRetrieval(NONE);
    if (m_File != null) {
      setFile(new File(m_File));
    }
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
    return "C4.5 data files";
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
   * Resets the Loader object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the source file.
   * @exception IOException if an error occurs
   */
  public void setSource(File file) throws IOException {
    
    m_structure = null;
    setRetrieval(NONE);

    if (file == null) {
      throw new IOException("Source file object is null!");
    }

    String fname = file.getName();
    String fileStem;
    String path = file.getParent();
    if (path != null) {
      path += File.separator;
    } else {
      path = "";
    }
    if (fname.indexOf('.') < 0) {
      fileStem = fname;
      fname += ".names";
    } else {
      fileStem = fname.substring(0, fname.lastIndexOf('.'));
      fname = fileStem + ".names";
    }
    m_fileStem = fileStem;
    file = new File(path+fname);

    m_sourceFile = file;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      m_namesReader = br;
    } catch (FileNotFoundException ex) {
      throw new IOException("File not found : "+(path+fname));
    }

    m_sourceFileData = new File(path+fileStem+".data");
    try {
      BufferedReader br = new BufferedReader(new FileReader(m_sourceFileData));
      m_dataReader = br;
    } catch (FileNotFoundException ex) {
      throw new IOException("File not found : "+(path+fname));
    }
    m_File = file.getAbsolutePath();
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
      throw new IOException("No source has beenspecified");
    }

    if (m_structure == null) {
      setSource(m_sourceFile);
      StreamTokenizer st = new StreamTokenizer(m_namesReader);
      initTokenizer(st);
      readHeader(st);
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
    if (m_sourceFile == null) {
      throw new IOException("No source has been specified");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(BATCH);
    if (m_structure == null) {
      getStructure();
    }
    StreamTokenizer st = new StreamTokenizer(m_dataReader);
    initTokenizer(st);
    //    st.ordinaryChar('.');
    Instances result = new Instances(m_structure);
    Instance current = getInstance(st);

    while (current != null) {
      result.add(current);
      current = getInstance(st);
    }
    try {
      reset();
    } catch (Exception ex) {
      ex.printStackTrace();
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
   * If it is not possible to read the data set incrementally (ie. in cases
   * where the data set structure cannot be fully established before all
   * instances have been seen) then an exception should be thrown.
   *
   * @return the next instance in the data set as an Instance object or null
   * if there are no more instances to be read
   * @exception IOException if there is an error during parsing
   */
  public Instance getNextInstance() throws IOException {
    if (m_sourceFile == null) {
      throw new IOException("No source has been specified");
    }
    
    if (getRetrieval() == BATCH) {
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");
    }
    setRetrieval(INCREMENTAL);

    if (m_structure == null) {
      getStructure();
    }

    StreamTokenizer st = new StreamTokenizer(m_dataReader);
    initTokenizer(st);
    //    st.ordinaryChar('.');
    Instance nextI = getInstance(st);
    if (nextI != null) {
      nextI.setDataset(m_structure);
    }
    else{
      try {
        reset();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return nextI;
  }

  /**
   * Reads an instance using the supplied tokenizer.
   *
   * @param tokenizer the tokenizer to use
   * @return an Instance or null if there are no more instances to read
   * @exception IOException if an error occurs
   */
  private Instance getInstance(StreamTokenizer tokenizer) 
    throws IOException {
    double [] instance = new double[m_structure.numAttributes()];
    
    ConverterUtils.getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return null;
    }
    
    int counter = 0;
    for (int i = 0; i < m_numAttribs; i++) {
      if (i > 0) {
	ConverterUtils.getToken(tokenizer);
      }

      if (!m_ignore[i]) {
	// Check if value is missing.
	if  (tokenizer.ttype == '?') {
	  instance[counter++] = Instance.missingValue();
	} else {
	  String val = tokenizer.sval;

	  if (i == m_numAttribs - 1) {
	    // remove trailing period	    
	    if (val.charAt(val.length()-1) == '.') {
	      val = val.substring(0,val.length()-1);
	    }
	  }
	  if (m_structure.attribute(counter).isNominal()) {
	    int index = m_structure.attribute(counter).indexOfValue(val);
	    if (index == -1) {
	      ConverterUtils.errms(tokenizer, "nominal value not declared in "
				   +"header :"+val+" column "+i);
	    }
	    instance[counter++] = (double)index;
	  } else if (m_structure.attribute(counter).isNumeric()) {
	    try {
	      instance[counter++] = Double.valueOf(val).doubleValue();
	    } catch (NumberFormatException e) {
	      ConverterUtils.errms(tokenizer, "number expected");
	    }
	  } else {
	    System.err.println("Shouldn't get here");
	    System.exit(1);
	  }
	}
      }
    }

    return new Instance(1.0, instance);
  }

  private String removeTrailingPeriod(String val) {
    // remove trailing period
    if (val.charAt(val.length()-1) == '.') {
      val = val.substring(0,val.length()-1);
    }
    return val;
  }

  /**
   * Reads header (from the names file) using the supplied tokenizer
   *
   * @param tokenizer the tokenizer to use
   * @exception IOException if an error occurs
   */
  private void readHeader(StreamTokenizer tokenizer) throws IOException {

    FastVector attribDefs = new FastVector();
    FastVector ignores = new FastVector();
    ConverterUtils.getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      ConverterUtils.errms(tokenizer,"premature end of file");
    }

    m_numAttribs = 1;
    // Read the class values
    FastVector classVals = new FastVector();
    while (tokenizer.ttype != StreamTokenizer.TT_EOL) {
      String val = tokenizer.sval.trim();
      
      if (val.length() > 0) {
	val = removeTrailingPeriod(val);
	classVals.addElement(val);
      }
      ConverterUtils.getToken(tokenizer);
    }

    // read the attribute names and types
    int counter = 0;
    while (tokenizer.ttype != StreamTokenizer.TT_EOF) {
      ConverterUtils.getFirstToken(tokenizer);
      if (tokenizer.ttype != StreamTokenizer.TT_EOF) {

	String attribName = tokenizer.sval;

	ConverterUtils.getToken(tokenizer);
	if (tokenizer.ttype == StreamTokenizer.TT_EOL) {
	  ConverterUtils.errms(tokenizer, "premature end of line. Expected "
			       +"attribute type.");
	}
	String temp = tokenizer.sval.toLowerCase().trim();
	if (temp.startsWith("ignore") || temp.startsWith("label")) {
	  ignores.addElement(new Integer(counter));
	  counter++;
	} else if (temp.startsWith("continuous")) {
	  attribDefs.addElement(new Attribute(attribName));
	  counter++;
	} else {
	  counter++;
	  // read the values of the attribute
	  FastVector attribVals = new FastVector();
	  while (tokenizer.ttype != StreamTokenizer.TT_EOL &&
		 tokenizer.ttype != StreamTokenizer.TT_EOF) {
	    String val = tokenizer.sval.trim();

	    if (val.length() > 0) {
	      val = removeTrailingPeriod(val);
	      attribVals.addElement(val);
	    }
	    ConverterUtils.getToken(tokenizer);
	  }
	  attribDefs.addElement(new Attribute(attribName, attribVals));
	}
      }
    }

    boolean ok = true;
    int i = -1;
    if (classVals.size() == 1) {
      // look to see if this is an attribute name (ala c5 names file style)
      for (i = 0; i < attribDefs.size(); i++) {
	if (((Attribute)attribDefs.elementAt(i))
	    .name().compareTo((String)classVals.elementAt(0)) == 0) {
	  ok = false;
	  m_numAttribs--;
	  break;
	}
      }
    }

    if (ok) {
      attribDefs.addElement(new Attribute("Class", classVals));
    }

    m_structure = new Instances(m_fileStem, attribDefs, 0);

    try {
      if (ok) {
	m_structure.setClassIndex(m_structure.numAttributes()-1);
      } else {
	m_structure.setClassIndex(i);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    m_numAttribs = m_structure.numAttributes() + ignores.size();
    m_ignore = new boolean[m_numAttribs];
    for (i = 0; i < ignores.size(); i++) {
      m_ignore[((Integer)ignores.elementAt(i)).intValue()] = true;
    }
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
    tokenizer.whitespaceChars(':',':');
    //    tokenizer.whitespaceChars('.','.');
    tokenizer.commentChar('|');
    tokenizer.whitespaceChars('\t','\t');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.eolIsSignificant(true);
  }

  /**
   * Main method for testing this class.
   *
   * @param args should contain <filestem>[.names | data]
   */
  public static void main (String [] args) {
    if (args.length > 0) {
      File inputfile;
      inputfile = new File(args[0]);
      try {
	C45Loader cta = new C45Loader();
	cta.setSource(inputfile);
	System.out.println(cta.getStructure());
	Instance temp = cta.getNextInstance();
	while (temp != null) {
	  System.out.println(temp);
	  temp = cta.getNextInstance();
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    } else {
      System.err.println("Usage:\n\tC45Loader <filestem>[.names | data]\n");
    }
  }
}
