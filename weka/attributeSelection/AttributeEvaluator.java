/*
 *    AttributeEvaluator.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.attributeSelection;

import java.io.*;
import weka.core.*;

/** 
 * Abstract attribute evaluator. Evaluate attributes individually.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public abstract class AttributeEvaluator extends ASEvaluation {
    // ===============
    // Public methods.
    // ===============

    /**
     * evaluates an individual attribute
     *
     * @param attribute the index of the attribute to be evaluated
     * @return the "merit" of the attribute
     * @exception Exception if the attribute could not be evaluated
     */
    public abstract double evaluateAttribute(int attribute) throws Exception;
}




