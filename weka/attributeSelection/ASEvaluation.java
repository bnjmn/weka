/*
 *    ASEvaluation.java
 *    Copyright (C) 1999 Mark Hall
 *
 */


package weka.attributeSelection;
import java.io.*;
import weka.core.*;

/** 
 * Abstract attribute selection evaluation class
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */

public abstract class ASEvaluation implements Serializable {

  // ===============
  // Public methods.
  // ===============

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public abstract void buildEvaluator(Instances data) throws Exception;

  /**
   * Provides a chance for a attribute evaluator to do any special
   * post processing of the selected attribute set.
   *
   * @param attributeSet the set of attributes found by the search
   * @return a possibly ranked list of postprocessed attributes
   * @exception Exception if postprocessing fails for some reason
   */
  public int [] postProcess(int [] attributeSet) 
    throws Exception
  {
    return attributeSet;
  }

  /**
   * Creates a new instance of an attribute/subset evaluator 
   * given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * evaluator implements OptionHandler and the options parameter is
   * non-null, the evaluator will have it's options set.
   *
   * @param evaluatorName the fully qualified class name of the evaluator
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created evaluator, ready for use.
   * @exception Exception if the evaluator name is invalid, or the options
   * supplied are not acceptable to the evaluator
   */
  public static ASEvaluation forName(String evaluatorName,
				     String [] options) throws Exception
  {
    return (ASEvaluation)Utils.forName(ASEvaluation.class,
				       evaluatorName,
				       options);
  }

  /**
   * Creates copies of the current evaluator.
   *
   * @param model an example evaluator to copy
   * @param num the number of evaluator copies to create.
   * @return an array of evaluators.
   * @exception Exception if an error occurs
   */
  public static ASEvaluation [] makeCopies(ASEvaluation model,
					 int num) throws Exception {

    if (model == null) {
      throw new Exception("No model evaluator set");
    }
    ASEvaluation [] evaluators = new ASEvaluation [num];
    String [] options = null;
    if (model instanceof OptionHandler) {
      options = ((OptionHandler)model).getOptions();
    }
    for(int i = 0; i < evaluators.length; i++) {
      evaluators[i] = (ASEvaluation) model.getClass().newInstance();
      if (options != null) {
	String [] tempOptions = (String [])options.clone();
	((OptionHandler)evaluators[i]).setOptions(tempOptions);
	Utils.checkForRemainingOptions(tempOptions);
      }
    }
    return evaluators;
  }
}
