/*
 *    ModelSelection.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import java.io.*;
import weka.core.*;

/**
 * Abstract class for model selection criteria.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public abstract class ModelSelection implements Serializable {

  /**
   * Selects a model for the given dataset.
   *
   * @exception Exception if model can't be selected
   */
  public abstract ClassifierSplitModel selectModel(Instances data) throws Exception;

  /**
   * Selects a model for the given train data using the given test data
   *
   * @exception Exception if model can't be selected
   */
  public ClassifierSplitModel selectModel(Instances train, Instances test) 
       throws Exception {

    throw new Exception("Model selection method not implemented");
  }
}
