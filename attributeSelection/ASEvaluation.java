/*
 *    ASEvaluation.java
 *    Copyright (C) 1999 Mark Hall
 *
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

package weka.attributeSelection;
import java.io.*;
import weka.core.*;

/** 
 * Abstract attribute selection evaluation class
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */

public abstract class ASEvaluation implements Serializable 
{
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
  public int [] postProcess(int [] attributeSet) throws Exception
  {
    return attributeSet;
  }
}
