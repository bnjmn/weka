/*
 *    ResultListener.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.experiment;
import java.io.Serializable;

/**
 * Interface for objects able to listen for results obtained
 * by a ResultProducer
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */

public interface ResultListener extends Serializable {

  /**
   * Determines if there are any constraints (imposed by the
   * destination) on additional result columns to be produced by
   * resultProducers. Null should be returned if there are NO
   * constraints, otherwise a list of column names should be
   * returned as an array of Strings.
   * @param rp the ResultProducer to which the constraints will apply
   * @return an array of column names to which resutltProducer's
   * additional results will be restricted.
   * @exception Exception if an error occurs
   */
  public String [] determineColumnConstraints(ResultProducer rp) 
    throws Exception;

  /**
   * Prepare for the results to be received.
   *
   * @param rp the ResultProducer that will generate the results
   * @exception Exception if an error occurs during preprocessing.
   */
  public void preProcess(ResultProducer rp) throws Exception;
  
  /**
   * Perform any postprocessing. When this method is called, it indicates
   * that no more results will be sent that need to be grouped together
   * in any way.
   *
   * @param rp the ResultProducer that generated the results
   * @exception Exception if an error occurs
   */
  public void postProcess(ResultProducer rp) throws Exception;
  
  /**
   * Accepts results from a ResultProducer.
   *
   * @param rp the ResultProducer that generated the results
   * @param key an array of Objects (Strings or Doubles) that uniquely
   * identify a result for a given ResultProducer with given compatibilityState
   * @param result the results stored in an array. The objects stored in
   * the array may be Strings, Doubles, or null (for the missing value).
   * @exception Exception if the result could not be accepted.
   */
  public void acceptResult(ResultProducer rp, Object [] key, Object [] result)
    throws Exception;

  /**
   * Determines whether the results for a specified key must be
   * generated.
   *
   * @param rp the ResultProducer wanting to generate the results
   * @param key an array of Objects (Strings or Doubles) that uniquely
   * identify a result for a given ResultProducer with given compatibilityState
   * @return true if the result should be generated
   * @exception Exception if it could not be determined if the result 
   * is needed.
   */
  public boolean isResultRequired(ResultProducer rp, Object [] key) 
    throws Exception;

}
