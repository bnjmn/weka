/*
 *    UpdateableClassifier.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers;

import weka.core.*;

/**
 * Interface to incremental classification models that can learn using
 * one instance at a time.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public interface UpdateableClassifier {

  /**
   * Updates a classifier using the given instance.
   *
   * @param instance the instance to included
   * @exception Exception if instance could not be incorporated
   * successfully
   */
  void updateClassifier(Instance instance) throws Exception;

}
 
