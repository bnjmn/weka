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
 *    SequentialClassifier.java
 *    Copyright (C) 2003 University Of Waikato
 *
 */

package weka.classifiers;

import weka.classifiers.*;
import weka.core.*;

/** 
 * Abstract sequential classifier.
 *
 * @author Saket Joshi (joshi@cs.orst.edu)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public abstract class SequentialClassifier extends Classifier {

  /**
   * Classifies a given sequence.
   *
   * @param instance the instances to be classified
   * @return the predicted sequence, as an array of class labels
   */  
  public abstract double[] classifySequence(Instances insts) throws Exception;

  /**
   * Classifies a given instance. Always returns a missing value, as this classifier
   * predicts a sequence instead.
   *
   * @param instance the instance to be classified
   * @return missing
   */  
  public double classifyInstance(Instance inst) { return Instance.missingValue(); }
}
