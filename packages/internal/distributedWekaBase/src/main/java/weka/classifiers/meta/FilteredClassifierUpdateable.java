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
 * FilteredClassifierUpdateable.java
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.RevisionUtils;

/**
 * <!-- globalinfo-start --> Class for running an arbitrary classifier on data
 * that has been passed through an arbitrary filter. Like the classifier, the
 * structure of the filter is based exclusively on the training data and test
 * instances will be processed by the filter without changing their structure.<br/>
 * Incremental version: only takes incremental classifiers as base classifiers
 * (i.e., they have to implement the weka.classifiers.UpdateableClassifier
 * interface).
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  eg: "weka.filters.unsupervised.attribute.Remove -V -R 1,2"
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.bayes.NaiveBayesUpdateable)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.bayes.NaiveBayesUpdateable:
 * </pre>
 * 
 * <pre>
 * -K
 *  Use kernel density estimator rather than normal
 *  distribution for numeric attributes
 * </pre>
 * 
 * <pre>
 * -D
 *  Use supervised discretization to process numeric attributes
 * </pre>
 * 
 * <pre>
 * -O
 *  Display model in old format (good when there are many classes)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see UpdateableClassifier
 */
public class FilteredClassifierUpdateable extends FilteredClassifier implements
  UpdateableClassifier, UpdateableBatchProcessor {

  /** for serialization. */
  private static final long serialVersionUID = -423438402311145048L;

  /**
   * Default constructor.
   */
  public FilteredClassifierUpdateable() {
    super();

    m_Classifier = new weka.classifiers.bayes.NaiveBayesUpdateable();
    m_Filter = new weka.filters.AllFilter();
  }

  /**
   * Returns a string describing this classifier.
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return super.globalInfo() + "\n"
      + "Incremental version: only takes incremental classifiers as base "
      + "classifiers (i.e., they have to implement the "
      + UpdateableClassifier.class.getName() + " interface).";
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {
    return weka.classifiers.bayes.NaiveBayesUpdateable.class.getName();
  }

  /**
   * Set the base learner.
   * 
   * @param value the classifier to use.
   */
  @Override
  public void setClassifier(Classifier value) {
    if (!(value instanceof UpdateableClassifier)) {
      throw new IllegalArgumentException("Classifier must be derived from "
        + UpdateableClassifier.class.getName() + "!");
    } else {
      super.setClassifier(value);
    }
  }

  /**
   * Updates a classifier using the given instance.
   * 
   * @param instance the instance to included
   * @throws Exception if instance could not be incorporated successfully or not
   *           successfully filtered
   */
  @Override
  public void updateClassifier(Instance instance) throws Exception {
    if (m_Filter.numPendingOutput() > 0) {
      throw new Exception("Filter output queue not empty!");
    }
    if (!m_Filter.input(instance)) {
      if (m_Filter.numPendingOutput() > 0) {
        throw new Exception("Filter output queue not empty!");
      }

      // nothing to train on if the filter does not make an instance available
      return;
      // throw new
      // Exception("Filter didn't make the train instance immediately available!");
    }

    m_Filter.batchFinished();
    Instance newInstance = m_Filter.output();

    ((UpdateableClassifier) m_Classifier).updateClassifier(newInstance);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for running this classifier.
   * 
   * @param args use -h for overview on parameters
   */
  public static void main(String[] args) {
    runClassifier(new FilteredClassifierUpdateable(), args);
  }

  @Override
  public void batchFinished() throws Exception {
    if (getClassifier() instanceof UpdateableBatchProcessor) {
      ((UpdateableBatchProcessor) getClassifier()).batchFinished();
    }
  }
}
