/*
 *    Converter.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.converters;

import java.io.*;
import weka.core.Instances;
import weka.core.Instance;

/** 
 * Interface to something that can read and convert a text file (of some
 * format) to arff format (Instances)
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface Converter {
  
  /*@ public model instance boolean model_structureDetermined
    @   initially: model_structureDetermined == false;
    @*/

  /*@ public model instance boolean model_sourceSupplied
    @   initially: model_sourceSupplied == false;
    @*/

  /**
   * Resets the Converter object, ready to read a new data set.
   *
   * <pre><jml>
   *    public_normal_behavior
   *      modifiable: model_structureDetermined;
   *      ensures: model_structureDetermined == false;
   * </jml></pre>
   */
  public void reset();

  /**
   * Resets the Converter object and sets the source of the data set to be 
   * the supplied File object.
   *
   * @param file the File
   * @exception IOException if an error occurs
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: file != null
   *                && (* file exists *);
   *      modifiable: model_sourceSupplied, model_structureDetermined;
   *      ensures: model_sourceSupplied == true 
   *               && model_structureDetermined == false;
   *  also
   *    public_exceptional_behavior
   *      requires: file == null
   *                || (* file does not exist *);
   *    signals: (IOException);
   * </jml></pre>
   */
  public void setSource(File file) throws IOException;

  /**
   * Determines and returns (if possible) the structure (internally the 
   * header) of the data set as an empty set of instances.
   *
   * @return the structure of the data set as an empty set of Instances
   * @exception IOException if there is no source or parsing fails
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: model_sourceSupplied == true
   *                && model_structureDetermined == false
   *                && (* successful parse *);
   *      modifiable: model_structureDetermined;
   *      ensures: \result != null
   *               && \result.numInstances() == 0
   *               && model_structureDetermined == true;
   *  also
   *    public_exceptional_behavior
   *      requires: model_sourceSupplied == false
   *                || (* unsuccessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  public Instances getStructure() throws IOException;

  /**
   * Return the full data set. If the structure hasn't yet been determined
   * by a call to getStructure then method should do so before processing
   * the rest of the data set.
   *
   * @return the full data set as an Instances object
   * @exception IOException if there is an error during parsing
   *
   * <pre><jml>
   *    public_normal_behavior
   *      requires: model_sourceSupplied == true
   *                && (* successful parse *);
   *      modifiable: model_structureDetermined;
   *      ensures: \result != null
   *               && \result.numInstances() >= 0
   *               && model_structureDetermined == true;
   *  also
   *    public_exceptional_behavior
   *      requires: model_sourceSupplied == false
   *                || (* unsuccessful parse *);
   *      signals: (IOException);
   * </jml></pre>
   */
  public Instances getDataSet() throws IOException;

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
   *
   * <pre><jml>
   *    public_normal_behavior
   *    {|
   *       requires: model_sourceSupplied == true
   *                 && (* successful parse *);
   *       modifiable: model_structureDetermined;
   *       ensures: model_structureDetermined == true
   *                && \result != null;
   *     also
   *       requires: model_sourceSupplied == true
   *                 && (* no further input *);
   *       modifiable: model_structureDetermined;
   *       ensures: model_structureDetermined == true
   *                && \result == null;
   *    |}
   *  also
   *    public_exceptional_behavior
   *    {|
   *       requires: model_sourceSupplied == false
   *                 || (* unsuccessful parse *);
   *       signals: (IOException);
   *     also
   *       requires: (* unable to process data set incrementally *);
   *       signals: (IOException);
   *    |}
   * </jml></pre>
   */
  public Instance getNextInstance() throws IOException;
}





