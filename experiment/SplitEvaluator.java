/*
 *    SplitEvaluator.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.experiment;

import weka.core.Instances;
import java.io.Serializable;

/**
 * Interface to objects able to generate a fixed set of results for
 * a particular split of a dataset. The set of results should contain
 * fields related to any settings of the SplitEvaluator (not including
 * the dataset name. For example, one field for the classifier used to
 * get the results, another for the classifier options, etc). <p>
 *
 * Possible implementations of SplitEvaluator: <br>
 * <ul>
 *   <li>StdClassification results
 *   <li>StdRegression results
 * </ul>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public interface SplitEvaluator extends Serializable {
  
  /**
   * Sets a list of method names for additional measures to look for
   * in SplitEvaluators.
   * @param additionalMeasures a list of method names
   */
  public void setAdditionalMeasures(String [] additionalMeasures);

  /**
   * Gets the names of each of the key columns produced for a single run.
   * The names should not contain spaces (use '_' instead for easy 
   * translation.) The number of key fields must be constant for a given 
   * SplitEvaluator.
   *
   * @return an array containing the name of each key column
   */
  public String [] getKeyNames();

  /**
   * Gets the data types of each of the key columns produced for a single run.
   * The number of key fields must be constant
   * for a given SplitEvaluator.
   *
   * @return an array containing objects of the type of each key column. The 
   * objects should be Strings, or Doubles.
   */
  public Object [] getKeyTypes();

  /**
   * Gets the names of each of the result columns produced for a single run.
   * The names should not contain spaces (use '_' instead for easy 
   * translation.) The number of result fields must be constant
   * for a given SplitEvaluator.
   *
   * @return an array containing the name of each result column
   */
  public String [] getResultNames();

  /**
   * Gets the data types of each of the result columns produced for a 
   * single run. The number of result fields must be constant
   * for a given SplitEvaluator.
   *
   * @return an array containing objects of the type of each result column. 
   * The objects should be Strings, or Doubles.
   */
  public Object [] getResultTypes();

  /**
   * Gets the key describing the current SplitEvaluator. For example
   * This may contain the name of the classifier used for classifier
   * predictive evaluation. The number of key fields must be constant
   * for a given SplitEvaluator.
   *
   * @return a value of type 'Object'
   */
  public Object [] getKey();

  /**
   * Gets the results for the supplied train and test datasets.
   *
   * @param train the training Instances.
   * @param test the testing Instances.
   * @return the results stored in an array. The objects stored in
   * the array may be Strings, Doubles, or null (for the missing value).
   * @exception Exception if a problem occurs while getting the results
   */
  public Object [] getResult(Instances train, Instances test) throws Exception;

  /**
   * Returns the raw output for the most recent call to getResult. Useful
   * for debugging splitEvaluators.
   * 
   * @return the raw output corresponding to the most recent call
   * to getResut
   */
  public String getRawResultOutput();

} // SplitEvaluator





