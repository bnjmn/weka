/*
 *    Classifier.java
 *    Copyright (C) 1999 Eibe Frank
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

package weka.classifiers;

import java.io.*;
import weka.core.*;

/** 
 * Abstract classifier.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.1 September 1998 (Eibe)
 */
public abstract class Classifier implements Cloneable, Serializable {

  // ===============
  // Public methods.
  // ===============
 
  /**
   * Generates a classifier. Has to initialize all fields of the classifier
   * that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the classifier has not been 
   * generated successfully
   */
  public abstract void buildClassifier(Instances data) throws Exception;

  /**
   * Classifies a given instance.
   *
   * @param data set of instances 
   * @param instance the instance to be classified
   * @return index of the predicted class as a double
   * if the class is enumerated, otherwise the predicted value
   * @exception Exception if instance could not be classified
   * successfully
   */
  public abstract double classifyInstance(Instance instance) throws Exception; 
}

