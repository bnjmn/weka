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
 *    DistributionSequentialClassifier.java
 *    Copyright (C) 2003 University Of Waikato
 *
 */

package weka.classifiers;

import weka.classifiers.*;
import weka.core.*;

/** 
 * Abstract sequential classification model that produces (for each element in sequence)
 * an estimate of the membership in each class (ie. a probability distribution).
 *
 * @author Saket Joshi (joshi@cs.orst.edu)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public abstract class DistributionSequentialClassifier extends SequentialClassifier
{

  /**
   * Classifies a given sequence, with class membership probabilities for the output sequence.
   *
   * @param insts the instances to be classified
   * @return the probabilities of the predicted sequence, as a double array of memberships
   */  
  public abstract double[][] distributionForSequence(Instances insts) throws Exception;

  /**
   * Classifies a given sequence.
   *
   * @param insts the instances to be classified
   * @return the predicted sequence, as an array of class labels
   */    
  public double[] classifySequence(Instances insts) throws Exception {

    double [] result = new double[insts.numInstances()];
    double [][] dist = distributionForSequence(insts);
    for(int i = 0; i<result.length; i++) {
      result[i] = Utils.maxIndex(dist[i]);
    }
    return result;
  }

}
