/*
 *    ASSearch.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.attributeSelection;

import java.io.*;
import weka.core.*;

/** 
 * Abstract attribute selection search class.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public abstract class ASSearch implements Serializable {

  // ===============
  // Public methods.
  // ===============
  
  /**
   * Searches the attribute subset/ranking space.
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public abstract int [] search(ASEvaluation ASEvaluator,
				Instances data) throws Exception;

  /**
   * Creates a new instance of a search class given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * search method implements OptionHandler and the options parameter is
   * non-null, the search method will have it's options set.
   *
   * @param searchName the fully qualified class name of the search class
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created search object, ready for use.
   * @exception Exception if the search class name is invalid, or the options
   * supplied are not acceptable to the search class.
   */
  public static ASSearch forName(String searchName,
				 String [] options) throws Exception
  {
    return (ASSearch)Utils.forName(ASSearch.class,
				   searchName,
				   options);
  }
}
