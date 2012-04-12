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
 * PLSFilter.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.attribute;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.matrix.EigenvalueDecomposition;
import weka.core.matrix.Matrix;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.SupervisedFilter;
import weka.filters.unsupervised.attribute.Center;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * Runs Partial Least Square Regression over the given instances and computes the resulting beta matrix for prediction.<br/>
 * By default it replaces missing values and centers the data.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Tormod Naes, Tomas Isaksson, Tom Fearn, Tony Davies (2002). A User Friendly Guide to Multivariate Calibration and Classification. NIR Publications.<br/>
 * <br/>
 * StatSoft, Inc.. Partial Least Squares (PLS).<br/>
 * <br/>
 * Bent Jorgensen, Yuri Goegebeur. Module 7: Partial least squares regression I.<br/>
 * <br/>
 * S. de Jong (1993). SIMPLS: an alternative approach to partial least squares regression. Chemometrics and Intelligent Laboratory Systems. 18:251-263.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;book{Naes2002,
 *    author = {Tormod Naes and Tomas Isaksson and Tom Fearn and Tony Davies},
 *    publisher = {NIR Publications},
 *    title = {A User Friendly Guide to Multivariate Calibration and Classification},
 *    year = {2002},
 *    ISBN = {0-9528666-2-5}
 * }
 * 
 * &#64;misc{missing_id,
 *    author = {StatSoft, Inc.},
 *    booktitle = {Electronic Textbook StatSoft},
 *    title = {Partial Least Squares (PLS)},
 *    HTTP = {http://www.statsoft.com/textbook/stpls.html}
 * }
 * 
 * &#64;misc{missing_id,
 *    author = {Bent Jorgensen and Yuri Goegebeur},
 *    booktitle = {ST02: Multivariate Data Analysis and Chemometrics},
 *    title = {Module 7: Partial least squares regression I},
 *    HTTP = {http://statmaster.sdu.dk/courses/ST02/module07/}
 * }
 * 
 * &#64;article{Jong1993,
 *    author = {S. de Jong},
 *    journal = {Chemometrics and Intelligent Laboratory Systems},
 *    pages = {251-263},
 *    title = {SIMPLS: an alternative approach to partial least squares regression},
 *    volume = {18},
 *    year = {1993}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  The number of components to compute.
 *  (default: 20)</pre>
 * 
 * <pre> -U
 *  Updates the class attribute as well.
 *  (default: off)</pre>
 * 
 * <pre> -M
 *  Turns replacing of missing values on.
 *  (default: off)</pre>
 * 
 * <pre> -A &lt;SIMPLS|PLS1&gt;
 *  The algorithm to use.
 *  (default: PLS1)</pre>
 * 
 * <pre> -P &lt;none|center|standardize&gt;
 *  The type of preprocessing that is applied to the data.
 *  (default: center)</pre>
 * 
 <!-- options-end -->
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class PLSFilter
  extends SimpleBatchFilter 
  implements SupervisedFilter, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -3335106965521265631L;

  /** the type of algorithm: SIMPLS */
  public static final int ALGORITHM_SIMPLS = 1;
  /** the type of algorithm: PLS1 */
  public static final int ALGORITHM_PLS1 = 2;
  /** the types of algorithm */
  public static final Tag[] TAGS_ALGORITHM = {
    new Tag(ALGORITHM_SIMPLS, "SIMPLS"),
    new Tag(ALGORITHM_PLS1, "PLS1")
  };

  /** the type of preprocessing: None */
  public static final int PREPROCESSING_NONE = 0;
  /** the type of preprocessing: Center */
  public static final int PREPROCESSING_CENTER = 1;
  /** the type of preprocessing: Standardize */
  public static final int PREPROCESSING_STANDARDIZE = 2;
  /** the types of preprocessing */
  public static final Tag[] TAGS_PREPROCESSING = {
    new Tag(PREPROCESSING_NONE, "none"),
    new Tag(PREPROCESSING_CENTER, "center"),
    new Tag(PREPROCESSING_STANDARDIZE, "standardize")
  };

  /** the maximum number of components to generate */
  protected int m_NumComponents = 20;
  
  /** the type of algorithm */
  protected int m_Algorithm = ALGORITHM_PLS1;

  /** the regression vector "r-hat" for PLS1 */
  protected Matrix m_PLS1_RegVector = null;

  /** the P matrix for PLS1 */
  protected Matrix m_PLS1_P = null;

  /** the W matrix for PLS1 */
  protected Matrix m_PLS1_W = null;

  /** the b-hat vector for PLS1 */
  protected Matrix m_PLS1_b_hat = null;
  
  /** the W matrix for SIMPLS */
  protected Matrix m_SIMPLS_W = null;
  
  /** the B matrix for SIMPLS (used for prediction) */
  protected Matrix m_SIMPLS_B = null;
  
  /** whether to include the prediction, i.e., modifying the class attribute */
  protected boolean m_PerformPrediction = false;

  /** for replacing missing values */
  protected Filter m_Missing = null;
  
  /** whether to replace missing values */
  protected boolean m_ReplaceMissing = true;
  
  /** for centering the data */
  protected Filter m_Filter = null;
  
  /** the type of preprocessing */
  protected int m_Preprocessing = PREPROCESSING_CENTER;

  /** the mean of the class */
  protected double m_ClassMean = 0;

  /** the standard deviation of the class */
  protected double m_ClassStdDev = 0;
  
  /**
   * default constructor
   */
  public PLSFilter() {
    super();
    
    // setup pre-processing
    m_Missing = new ReplaceMissingValues();
    m_Filter  = new Center();
  }
  
  /**
   * Returns a string describing this classifier.
   *
   * @return      a description of the classifier suitable for
   *              displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Runs Partial Least Square Regression over the given instances "
      + "and computes the resulting beta matrix for prediction.\n"
      + "By default it replaces missing values and centers the data.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.BOOK);
    result.setValue(Field.AUTHOR, "Tormod Naes and Tomas Isaksson and Tom Fearn and Tony Davies");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.TITLE, "A User Friendly Guide to Multivariate Calibration and Classification");
    result.setValue(Field.PUBLISHER, "NIR Publications");
    result.setValue(Field.ISBN, "0-9528666-2-5");
    
    additional = result.add(Type.MISC);
    additional.setValue(Field.AUTHOR, "StatSoft, Inc.");
    additional.setValue(Field.TITLE, "Partial Least Squares (PLS)");
    additional.setValue(Field.BOOKTITLE, "Electronic Textbook StatSoft");
    additional.setValue(Field.HTTP, "http://www.statsoft.com/textbook/stpls.html");
    
    additional = result.add(Type.MISC);
    additional.setValue(Field.AUTHOR, "Bent Jorgensen and Yuri Goegebeur");
    additional.setValue(Field.TITLE, "Module 7: Partial least squares regression I");
    additional.setValue(Field.BOOKTITLE, "ST02: Multivariate Data Analysis and Chemometrics");
    additional.setValue(Field.HTTP, "http://statmaster.sdu.dk/courses/ST02/module07/");
    
    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "S. de Jong");
    additional.setValue(Field.YEAR, "1993");
    additional.setValue(Field.TITLE, "SIMPLS: an alternative approach to partial least squares regression");
    additional.setValue(Field.JOURNAL, "Chemometrics and Intelligent Laboratory Systems");
    additional.setValue(Field.VOLUME, "18");
    additional.setValue(Field.PAGES, "251-263");
    
    return result;
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector		result;
    Enumeration		enm;
    String		param;
    SelectedTag		tag;
    int			i;

    result = new Vector();

    enm = super.listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    result.addElement(new Option(
	"\tThe number of components to compute.\n"
	+ "\t(default: 20)",
	"C", 1, "-C <num>"));

    result.addElement(new Option(
	"\tUpdates the class attribute as well.\n"
	+ "\t(default: off)",
	"U", 0, "-U"));

    result.addElement(new Option(
	"\tTurns replacing of missing values on.\n"
	+ "\t(default: off)",
	"M", 0, "-M"));

    param = "";
    for (i = 0; i < TAGS_ALGORITHM.length; i++) {
      if (i > 0)
	param += "|";
      tag = new SelectedTag(TAGS_ALGORITHM[i].getID(), TAGS_ALGORITHM);
      param += tag.getSelectedTag().getReadable();
    }
    result.addElement(new Option(
	"\tThe algorithm to use.\n"
	+ "\t(default: PLS1)",
	"A", 1, "-A <" + param + ">"));

    param = "";
    for (i = 0; i < TAGS_PREPROCESSING.length; i++) {
      if (i > 0)
	param += "|";
      tag = new SelectedTag(TAGS_PREPROCESSING[i].getID(), TAGS_PREPROCESSING);
      param += tag.getSelectedTag().getReadable();
    }
    result.addElement(new Option(
	"\tThe type of preprocessing that is applied to the data.\n"
	+ "\t(default: center)",
	"P", 1, "-P <" + param + ">"));

    return result.elements();
  }

  /**
   * returns the options of the current setup
   *
   * @return      the current options
   */
  public String[] getOptions() {
    int       i;
    Vector    result;
    String[]  options;

    result = new Vector();
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    result.add("-C");
    result.add("" + getNumComponents());

    if (getPerformPrediction())
      result.add("-U");
    
    if (getReplaceMissing())
      result.add("-M");
    
    result.add("-A");
    result.add("" + getAlgorithm().getSelectedTag().getReadable());

    result.add("-P");
    result.add("" + getPreprocessing().getSelectedTag().getReadable());

    return (String[]) result.toArray(new String[result.size()]);	  
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turns on output of debugging information.</pre>
   * 
   * <pre> -C &lt;num&gt;
   *  The number of components to compute.
   *  (default: 20)</pre>
   * 
   * <pre> -U
   *  Updates the class attribute as well.
   *  (default: off)</pre>
   * 
   * <pre> -M
   *  Turns replacing of missing values on.
   *  (default: off)</pre>
   * 
   * <pre> -A &lt;SIMPLS|PLS1&gt;
   *  The algorithm to use.
   *  (default: PLS1)</pre>
   * 
   * <pre> -P &lt;none|center|standardize&gt;
   *  The type of preprocessing that is applied to the data.
   *  (default: center)</pre>
   * 
   <!-- options-end -->
   *
   * @param options	the options to use
   * @throws Exception	if the option setting fails
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption("C", options);
    if (tmpStr.length() != 0)
      setNumComponents(Integer.parseInt(tmpStr));
    else
      setNumComponents(20);

    setPerformPrediction(Utils.getFlag("U", options));
    
    setReplaceMissing(Utils.getFlag("M", options));
    
    tmpStr = Utils.getOption("A", options);
    if (tmpStr.length() != 0)
      setAlgorithm(new SelectedTag(tmpStr, TAGS_ALGORITHM));
    else
      setAlgorithm(new SelectedTag(ALGORITHM_PLS1, TAGS_ALGORITHM));
    
    tmpStr = Utils.getOption("P", options);
    if (tmpStr.length() != 0)
      setPreprocessing(new SelectedTag(tmpStr, TAGS_PREPROCESSING));
    else
      setPreprocessing(new SelectedTag(PREPROCESSING_CENTER, TAGS_PREPROCESSING));
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String numComponentsTipText() {
    return "The number of components to compute.";
  }

  /**
   * sets the maximum number of attributes to use.
   * 
   * @param value	the maximum number of attributes
   */
  public void setNumComponents(int value) {
    m_NumComponents = value;
  }

  /**
   * returns the maximum number of attributes to use.
   * 
   * @return		the current maximum number of attributes
   */
  public int getNumComponents() {
    return m_NumComponents;
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String performPredictionTipText() {
    return "Whether to update the class attribute with the predicted value.";
  }

  /**
   * Sets whether to update the class attribute with the predicted value.
   * 
   * @param value	if true the class value will be replaced by the 
   * 			predicted value.
   */
  public void setPerformPrediction(boolean value) {
    m_PerformPrediction = value;
  }

  /**
   * Gets whether the class attribute is updated with the predicted value.
   * 
   * @return		true if the class attribute is updated
   */
  public boolean getPerformPrediction() {
    return m_PerformPrediction;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String algorithmTipText() {
    return "Sets the type of algorithm to use.";
  }

  /**
   * Sets the type of algorithm to use 
   *
   * @param value 	the algorithm type
   */
  public void setAlgorithm(SelectedTag value) {
    if (value.getTags() == TAGS_ALGORITHM) {
      m_Algorithm = value.getSelectedTag().getID();
    }
  }

  /**
   * Gets the type of algorithm to use 
   *
   * @return 		the current algorithm type.
   */
  public SelectedTag getAlgorithm() {
    return new SelectedTag(m_Algorithm, TAGS_ALGORITHM);
  }

  /**
   * Returns the tip text for this property
   *
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String replaceMissingTipText() {
    return "Whether to replace missing values.";
  }

  /**
   * Sets whether to replace missing values.
   * 
   * @param value	if true missing values are replaced with the
   * 			ReplaceMissingValues filter.
   */
  public void setReplaceMissing(boolean value) {
    m_ReplaceMissing = value;
  }

  /**
   * Gets whether missing values are replace.
   * 
   * @return		true if missing values are replaced with the 
   * 			ReplaceMissingValues filter
   */
  public boolean getReplaceMissing() {
    return m_ReplaceMissing;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String preprocessingTipText() {
    return "Sets the type of preprocessing to use.";
  }

  /**
   * Sets the type of preprocessing to use 
   *
   * @param value 	the preprocessing type
   */
  public void setPreprocessing(SelectedTag value) {
    if (value.getTags() == TAGS_PREPROCESSING) {
      m_Preprocessing = value.getSelectedTag().getID();
    }
  }

  /**
   * Gets the type of preprocessing to use 
   *
   * @return 		the current preprocessing type.
   */
  public SelectedTag getPreprocessing() {
    return new SelectedTag(m_Preprocessing, TAGS_PREPROCESSING);
  }

  /**
   * Determines the output format based on the input format and returns 
   * this. In case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called
   * from batchFinished().
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   * @throws Exception      in case the determination goes wrong
   * @see   #hasImmediateOutputFormat()
   * @see   #batchFinished()
   */
  protected Instances determineOutputFormat(Instances inputFormat) 
    throws Exception {

    // generate header
    FastVector atts = new FastVector();
    String prefix = getAlgorithm().getSelectedTag().getReadable();
    for (int i = 0; i < getNumComponents(); i++)
      atts.addElement(new Attribute(prefix + "_" + (i+1)));
    atts.addElement(new Attribute("Class"));
    Instances result = new Instances(prefix, atts, 0);
    result.setClassIndex(result.numAttributes() - 1);
    
    return result;
  }
  
  /**
   * returns the data minus the class column as matrix
   * 
   * @param instances	the data to work on
   * @return		the data without class attribute
   */
  protected Matrix getX(Instances instances) {
    double[][]	x;
    double[]	values;
    Matrix	result;
    int		i;
    int		n;
    int		j;
    int		clsIndex;
    
    clsIndex = instances.classIndex();
    x        = new double[instances.numInstances()][];
    
    for (i = 0; i < instances.numInstances(); i++) {
      values = instances.instance(i).toDoubleArray();
      x[i]   = new double[values.length - 1];
      
      j = 0;
      for (n = 0; n < values.length; n++) {
	if (n != clsIndex) {
	  x[i][j] = values[n];
	  j++;
	}
      }
    }
    
    result = new Matrix(x);
    
    return result;
  }
  
  /**
   * returns the data minus the class column as matrix
   * 
   * @param instance	the instance to work on
   * @return		the data without the class attribute
   */
  protected Matrix getX(Instance instance) {
    double[][]	x;
    double[]	values;
    Matrix	result;
    
    x = new double[1][];
    values = instance.toDoubleArray();
    x[0] = new double[values.length - 1];
    System.arraycopy(values, 0, x[0], 0, values.length - 1);
    
    result = new Matrix(x);
    
    return result;
  }
  
  /**
   * returns the data class column as matrix
   * 
   * @param instances	the data to work on
   * @return		the class attribute
   */
  protected Matrix getY(Instances instances) {
    double[][]	y;
    Matrix	result;
    int		i;
    
    y = new double[instances.numInstances()][1];
    for (i = 0; i < instances.numInstances(); i++)
      y[i][0] = instances.instance(i).classValue();
    
    result = new Matrix(y);
    
    return result;
  }
  
  /**
   * returns the data class column as matrix
   * 
   * @param instance	the instance to work on
   * @return		the class attribute
   */
  protected Matrix getY(Instance instance) {
    double[][]	y;
    Matrix	result;
    
    y = new double[1][1];
    y[0][0] = instance.classValue();
    
    result = new Matrix(y);
    
    return result;
  }
  
  /**
   * returns the X and Y matrix again as Instances object, based on the given
   * header (must have a class attribute set).
   * 
   * @param header	the format of the instance object
   * @param x		the X matrix (data)
   * @param y		the Y matrix (class)
   * @return		the assembled data
   */
  protected Instances toInstances(Instances header, Matrix x, Matrix y) {
    double[]	values;
    int		i;
    int		n;
    Instances	result;
    int		rows;
    int		cols;
    int		offset;
    int		clsIdx;
    
    result = new Instances(header, 0);
    
    rows   = x.getRowDimension();
    cols   = x.getColumnDimension();
    clsIdx = header.classIndex();
    
    for (i = 0; i < rows; i++) {
      values = new double[cols + 1];
      offset = 0;

      for (n = 0; n < values.length; n++) {
	if (n == clsIdx) {
	  offset--;
	  values[n] = y.get(i, 0);
	}
	else {
	  values[n] = x.get(i, n + offset);
	}
      }
      
      result.add(new Instance(1.0, values));
    }
    
    return result;
  }
  
  /**
   * returns the given column as a vector (actually a n x 1 matrix)
   * 
   * @param m		the matrix to work on
   * @param columnIndex	the column to return
   * @return		the column as n x 1 matrix
   */
  protected Matrix columnAsVector(Matrix m, int columnIndex) {
    Matrix	result;
    int		i;
    
    result = new Matrix(m.getRowDimension(), 1);
    
    for (i = 0; i < m.getRowDimension(); i++)
      result.set(i, 0, m.get(i, columnIndex));
    
    return result;
  }
  
  /**
   * stores the data from the (column) vector in the matrix at the specified 
   * index
   * 
   * @param v		the vector to store in the matrix
   * @param m		the receiving matrix
   * @param columnIndex	the column to store the values in
   */
  protected void setVector(Matrix v, Matrix m, int columnIndex) {
    m.setMatrix(0, m.getRowDimension() - 1, columnIndex, columnIndex, v);
  }
  
  /**
   * returns the (column) vector of the matrix at the specified index
   * 
   * @param m		the matrix to work on
   * @param columnIndex	the column to get the values from
   * @return		the column vector
   */
  protected Matrix getVector(Matrix m, int columnIndex) {
    return m.getMatrix(0, m.getRowDimension() - 1, columnIndex, columnIndex);
  }

  /**
   * determines the dominant eigenvector for the given matrix and returns it
   * 
   * @param m		the matrix to determine the dominant eigenvector for
   * @return		the dominant eigenvector
   */
  protected Matrix getDominantEigenVector(Matrix m) {
    EigenvalueDecomposition	eigendecomp;
    double[]			eigenvalues;
    int				index;
    Matrix			result;
    
    eigendecomp = m.eig();
    eigenvalues = eigendecomp.getRealEigenvalues();
    index       = Utils.maxIndex(eigenvalues);
    result	= columnAsVector(eigendecomp.getV(), index);
    
    return result;
  }
  
  /**
   * normalizes the given vector (inplace) 
   * 
   * @param v		the vector to normalize
   */
  protected void normalizeVector(Matrix v) {
    double	sum;
    int		i;
    
    // determine length
    sum = 0;
    for (i = 0; i < v.getRowDimension(); i++)
      sum += v.get(i, 0) * v.get(i, 0);
    sum = StrictMath.sqrt(sum);
    
    // normalize content
    for (i = 0; i < v.getRowDimension(); i++)
      v.set(i, 0, v.get(i, 0) / sum);
  }

  /**
   * processes the instances using the PLS1 algorithm
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   */
  protected Instances processPLS1(Instances instances) throws Exception {
    Matrix	X, X_trans, x;
    Matrix	y;
    Matrix	W, w;
    Matrix	T, t, t_trans;
    Matrix	P, p, p_trans;
    double	b;
    Matrix	b_hat;
    int		i;
    int		j;
    Matrix	X_new;
    Matrix	tmp;
    Instances	result;
    Instances	tmpInst;

    // initialization
    if (!isFirstBatchDone()) {
      // split up data
      X       = getX(instances);
      y       = getY(instances);
      X_trans = X.transpose();
      
      // init
      W     = new Matrix(instances.numAttributes() - 1, getNumComponents());
      P     = new Matrix(instances.numAttributes() - 1, getNumComponents());
      T     = new Matrix(instances.numInstances(), getNumComponents());
      b_hat = new Matrix(getNumComponents(), 1);
      
      for (j = 0; j < getNumComponents(); j++) {
	// 1. step: wj
	w = X_trans.times(y);
	normalizeVector(w);
	setVector(w, W, j);
	
	// 2. step: tj
	t       = X.times(w);
	t_trans = t.transpose();
	setVector(t, T, j);
	
	// 3. step: ^bj
	b = t_trans.times(y).get(0, 0) / t_trans.times(t).get(0, 0);
	b_hat.set(j, 0, b);
	
	// 4. step: pj
	p       = X_trans.times(t).times((double) 1 / t_trans.times(t).get(0, 0));
	p_trans = p.transpose();
	setVector(p, P, j);
	
	// 5. step: Xj+1
	X = X.minus(t.times(p_trans));
	y = y.minus(t.times(b));
      }
      
      // W*(P^T*W)^-1
      tmp = W.times(((P.transpose()).times(W)).inverse());
      
      // X_new = X*W*(P^T*W)^-1
      X_new = getX(instances).times(tmp);
      
      // factor = W*(P^T*W)^-1 * b_hat
      m_PLS1_RegVector = tmp.times(b_hat);
   
      // save matrices
      m_PLS1_P     = P;
      m_PLS1_W     = W;
      m_PLS1_b_hat = b_hat;
      
      if (getPerformPrediction())
        result = toInstances(getOutputFormat(), X_new, y);
      else
        result = toInstances(getOutputFormat(), X_new, getY(instances));
    }
    // prediction
    else {
      result = new Instances(getOutputFormat());
      
      for (i = 0; i < instances.numInstances(); i++) {
	// work on each instance
	tmpInst = new Instances(instances, 0);
	tmpInst.add((Instance) instances.instance(i).copy());
	x = getX(tmpInst);
	X = new Matrix(1, getNumComponents());
	T = new Matrix(1, getNumComponents());
	
	for (j = 0; j < getNumComponents(); j++) {
	  setVector(x, X, j);
	  // 1. step: tj = xj * wj
	  t = x.times(getVector(m_PLS1_W, j));
	  setVector(t, T, j);
	  // 2. step: xj+1 = xj - tj*pj^T (tj is 1x1 matrix!)
	  x = x.minus(getVector(m_PLS1_P, j).transpose().times(t.get(0, 0)));
	}
	
	if (getPerformPrediction())
	  tmpInst = toInstances(getOutputFormat(), T, T.times(m_PLS1_b_hat));
	else
	  tmpInst = toInstances(getOutputFormat(), T, getY(tmpInst));
	
	result.add(tmpInst.instance(0));
      }
    }
    
    return result;
  }

  /**
   * processes the instances using the SIMPLS algorithm
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   */
  protected Instances processSIMPLS(Instances instances) throws Exception {
    Matrix	A, A_trans;
    Matrix	M;
    Matrix	X, X_trans;
    Matrix	X_new;
    Matrix	Y, y;
    Matrix	C, c;
    Matrix	Q, q;
    Matrix	W, w;
    Matrix	P, p, p_trans;
    Matrix	v, v_trans;
    Matrix	T;
    Instances	result;
    int		h;
    
    if (!isFirstBatchDone()) {
      // init
      X       = getX(instances);
      X_trans = X.transpose();
      Y       = getY(instances);
      A       = X_trans.times(Y);
      M       = X_trans.times(X);
      C       = Matrix.identity(instances.numAttributes() - 1, instances.numAttributes() - 1);
      W       = new Matrix(instances.numAttributes() - 1, getNumComponents());
      P       = new Matrix(instances.numAttributes() - 1, getNumComponents());
      Q       = new Matrix(1, getNumComponents());
      
      for (h = 0; h < getNumComponents(); h++) {
	// 1. qh as dominant EigenVector of Ah'*Ah
	A_trans = A.transpose();
	q       = getDominantEigenVector(A_trans.times(A));
	
	// 2. wh=Ah*qh, ch=wh'*Mh*wh, wh=wh/sqrt(ch), store wh in W as column
	w       = A.times(q);
	c       = w.transpose().times(M).times(w);
	w       = w.times(1.0 / StrictMath.sqrt(c.get(0, 0)));
	setVector(w, W, h);
	
	// 3. ph=Mh*wh, store ph in P as column
	p       = M.times(w);
	p_trans = p.transpose();
	setVector(p, P, h);
	
	// 4. qh=Ah'*wh, store qh in Q as column
	q = A_trans.times(w);
	setVector(q, Q, h);
	
	// 5. vh=Ch*ph, vh=vh/||vh||
	v       = C.times(p);
	normalizeVector(v);
	v_trans = v.transpose();
	
	// 6. Ch+1=Ch-vh*vh', Mh+1=Mh-ph*ph'
	C = C.minus(v.times(v_trans));
	M = M.minus(p.times(p_trans));
	
	// 7. Ah+1=ChAh (actually Ch+1)
	A = C.times(A);
      }
      
      // finish
      m_SIMPLS_W = W;
      T          = X.times(m_SIMPLS_W);
      X_new      = T;
      m_SIMPLS_B = W.times(Q.transpose());
      
      if (getPerformPrediction())
	y = T.times(P.transpose()).times(m_SIMPLS_B);
      else
	y = getY(instances);

      result = toInstances(getOutputFormat(), X_new, y);
    }
    else {
      result = new Instances(getOutputFormat());
      
      X     = getX(instances);
      X_new = X.times(m_SIMPLS_W);
      
      if (getPerformPrediction())
	y = X.times(m_SIMPLS_B);
      else
	y = getY(instances);
      
      result = toInstances(getOutputFormat(), X_new, y);
    }
    
    return result;
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);

    // other
    result.setMinimumNumberInstances(1);
    
    return result;
  }
  
  /**
   * Processes the given data (may change the provided dataset) and returns
   * the modified version. This method is called in batchFinished().
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   * @see               #batchFinished()
   */
  protected Instances process(Instances instances) throws Exception {
    Instances	result;
    int		i;
    double	clsValue;
    double[]	clsValues;
    
    result = null;

    // save original class values if no prediction is performed
    if (!getPerformPrediction())
      clsValues = instances.attributeToDoubleArray(instances.classIndex());
    else
      clsValues = null;
    
    if (!isFirstBatchDone()) {
      // init filters
      if (m_ReplaceMissing)
	m_Missing.setInputFormat(instances);
      
      switch (m_Preprocessing) {
	case PREPROCESSING_CENTER:
	  m_ClassMean   = instances.meanOrMode(instances.classIndex());
	  m_ClassStdDev = 1;
	  m_Filter      = new Center();
	  ((Center) m_Filter).setIgnoreClass(true);
      	  break;
	case PREPROCESSING_STANDARDIZE:
	  m_ClassMean   = instances.meanOrMode(instances.classIndex());
	  m_ClassStdDev = StrictMath.sqrt(instances.variance(instances.classIndex()));
	  m_Filter      = new Standardize();
	  ((Standardize) m_Filter).setIgnoreClass(true);
      	  break;
	default:
  	  m_ClassMean   = 0;
	  m_ClassStdDev = 1;
	  m_Filter      = null;
      }
      if (m_Filter != null)
	m_Filter.setInputFormat(instances);
    }
    
    // filter data
    if (m_ReplaceMissing)
      instances = Filter.useFilter(instances, m_Missing);
    if (m_Filter != null)
      instances = Filter.useFilter(instances, m_Filter);
    
    switch (m_Algorithm) {
      case ALGORITHM_SIMPLS:
	result = processSIMPLS(instances);
	break;
      case ALGORITHM_PLS1:
	result = processPLS1(instances);
	break;
      default:
	throw new IllegalStateException(
	    "Algorithm type '" + m_Algorithm + "' is not recognized!");
    }

    // add the mean to the class again if predictions are to be performed,
    // otherwise restore original class values
    for (i = 0; i < result.numInstances(); i++) {
      if (!getPerformPrediction()) {
	result.instance(i).setClassValue(clsValues[i]);
      }
      else {
	clsValue = result.instance(i).classValue();
	result.instance(i).setClassValue(clsValue*m_ClassStdDev + m_ClassMean);
      }
    }
    
    return result;
  }

  /**
   * runs the filter with the given arguments
   *
   * @param args      the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new PLSFilter(), args);
  }
}
